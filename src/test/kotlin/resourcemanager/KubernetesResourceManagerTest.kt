package resourcemanager

import com.github.abyssnlp.common.KrankExceptions
import com.github.abyssnlp.common.ResourceType
import com.github.abyssnlp.common.ScalingConfiguration
import com.github.abyssnlp.common.TargetResource
import com.github.abyssnlp.resourcemanager.KubernetesResourceManager
import io.fabric8.kubernetes.api.model.KubernetesResource
import io.fabric8.kubernetes.api.model.apps.Deployment
import io.fabric8.kubernetes.api.model.apps.DeploymentSpec
import io.fabric8.kubernetes.api.model.apps.StatefulSet
import io.fabric8.kubernetes.api.model.apps.StatefulSetSpec
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.dsl.AppsAPIGroupDSL
import io.fabric8.kubernetes.client.dsl.RollableScalableResource
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class KubernetesResourceManagerTest {

    private lateinit var k8sClient: KubernetesClient
    private lateinit var resourceManager: KubernetesResourceManager
    private lateinit var appsAPIGroup: AppsAPIGroupDSL
    private lateinit var mockDeployment: RollableScalableResource<Deployment>
    private lateinit var mockStatefulSet: RollableScalableResource<StatefulSet>

    @BeforeEach
    fun setup() {
        k8sClient = mockk(relaxed = true)
        appsAPIGroup = mockk(relaxed = true)
        mockDeployment = mockk(relaxed = true)
        mockStatefulSet = mockk(relaxed = true)

        // Mock chain of calls to k8sClient
        every { k8sClient.apps() } returns appsAPIGroup
        every { appsAPIGroup.deployments() } returns mockk {
            every { inNamespace(any()) } returns mockk {
                every { withName(any()) } returns mockDeployment
            }
        }
        every { appsAPIGroup.statefulSets() } returns mockk {
            every { inNamespace(any()) } returns mockk {
                every { withName(any()) } returns mockStatefulSet
            }
        }

        resourceManager = KubernetesResourceManager()
        val field = KubernetesResourceManager::class.java.getDeclaredField("k8sClient")
        field.isAccessible = true
        field.set(resourceManager, k8sClient)
    }

    @Test
    fun `getCurrentReplicas gets correct replicas for Deployment`(): Unit = runBlocking {
        val deployment = mockk<Deployment>()
        val spec = mockk<DeploymentSpec>()
        every { deployment.spec } returns spec
        every { spec.replicas } returns 3

        val replicas = resourceManager.getCurrentReplicas(deployment)
        assertEquals(3, replicas)
    }

    @Test
    fun `getCurrentReplicas gets correct replicas for StatefulSet`(): Unit = runBlocking {
        val statefulSet = mockk<StatefulSet>()
        val spec = mockk<StatefulSetSpec>()
        every { statefulSet.spec } returns spec
        every { spec.replicas } returns 5

        val replicas = resourceManager.getCurrentReplicas(statefulSet)
        assertEquals(5, replicas)
    }

    @Test
    fun `getCurrentReplicas throws exception for unsupported resource type`(): Unit = runBlocking {
        val unsupportedResource = mockk<KubernetesResource>()

        assertFailsWith<KrankExceptions.ResourceTypeNotSupportedException> {
            runBlocking {
                resourceManager.getCurrentReplicas(unsupportedResource)
            }
        }
    }

    @Test
    fun `scale deployment within bounds`() = runBlocking {
        val targetResource = TargetResource(
            namespace = "default",
            deploymentName = "test-deployment",
            resourceType = ResourceType.DEPLOYMENT,
            consumerGroup = "test-consumer-group"
        )
        val scalingConfig = ScalingConfiguration(minReplicas = 1, maxReplicas = 10)
        val deployment = mockk<Deployment>()
        val spec = mockk<DeploymentSpec>()

        every { deployment.spec } returns spec
        every { spec.replicas } returns 3
        every { mockDeployment.get() } returns deployment
        every { mockDeployment.scale(mockk<Int>()) } returns mockk()

        resourceManager.scale(targetResource, 2, scalingConfig)
        verify { mockDeployment.scale(5) }
    }

    @Test
    fun `scale deployment respects maximum replicas`() = runBlocking {
        val targetResource = TargetResource(
            namespace = "default",
            deploymentName = "test-deployment",
            resourceType = ResourceType.DEPLOYMENT,
            consumerGroup = "test-consumer-group"
        )
        val scalingConfig = ScalingConfiguration(minReplicas = 1, maxReplicas = 5)
        val deployment = mockk<Deployment>()
        val spec = mockk<DeploymentSpec>()

        every { deployment.spec } returns spec
        every { spec.replicas } returns 4
        every { mockDeployment.get() } returns deployment
        every { mockDeployment.scale(mockk<Int>()) } returns mockk()

        resourceManager.scale(targetResource, 3, scalingConfig)
        verify { mockDeployment.scale(5) }
    }

    @Test
    fun `scale deployment respects minimum replicas`() = runBlocking {
        val targetResource = TargetResource(
            namespace = "default",
            deploymentName = "test-deployment",
            resourceType = ResourceType.DEPLOYMENT,
            consumerGroup = "test-consumer-group"
        )
        val scalingConfig = ScalingConfiguration(minReplicas = 2, maxReplicas = 10)
        val deployment = mockk<Deployment>()
        val spec = mockk<DeploymentSpec>()

        every { deployment.spec } returns spec
        every { spec.replicas } returns 3
        every { mockDeployment.get() } returns deployment
        every { mockDeployment.scale(mockk<Int>()) } returns mockk()

        resourceManager.scale(targetResource, -2, scalingConfig)
        verify { mockDeployment.scale(2) }
    }

    @Test
    fun `scale statefulset within bounds`() = runBlocking {
        val targetResource = TargetResource(
            namespace = "default",
            deploymentName = "test-statefulset",
            resourceType = ResourceType.STATEFUL_SET,
            consumerGroup = "test-consumer-group"
        )
        val scalingConfig = ScalingConfiguration(minReplicas = 1, maxReplicas = 10)
        val statefulSet = mockk<StatefulSet>()
        val spec = mockk<StatefulSetSpec>()

        every { statefulSet.spec } returns spec
        every { spec.replicas } returns 3
        every { mockStatefulSet.get() } returns statefulSet
        every { mockStatefulSet.scale(mockk<Int>()) } returns mockk()

        resourceManager.scale(targetResource, 2, scalingConfig)
        verify { mockStatefulSet.scale(5) }
    }

    @Test
    fun `no scaling when new replica count equals current`() = runBlocking {
        val targetResource = TargetResource(
            namespace = "default",
            deploymentName = "test-deployment",
            resourceType = ResourceType.DEPLOYMENT,
            consumerGroup = "test-consumer-group"
        )
        val scalingConfig = ScalingConfiguration(minReplicas = 1, maxReplicas = 10)
        val deployment = mockk<Deployment>()
        val spec = mockk<DeploymentSpec>()

        every { deployment.spec } returns spec
        every { spec.replicas } returns 5
        every { mockDeployment.get() } returns deployment

        resourceManager.scale(targetResource, 0, scalingConfig)
        verify(exactly = 0) { mockDeployment.scale(mockk<Int>()) }
    }

}