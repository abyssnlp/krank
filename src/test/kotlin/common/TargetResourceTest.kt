package common

import com.github.abyssnlp.common.ResourceType
import com.github.abyssnlp.common.TargetResource
import io.mockk.MockKAnnotations

import kotlin.test.*

internal class TargetResourceTest {
    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
    }

    @Test
    fun `test ResourceType enum values`() {
        assertEquals(2, ResourceType.entries.size)
        assertTrue(ResourceType.entries.contains(ResourceType.DEPLOYMENT))
        assertTrue(ResourceType.entries.contains(ResourceType.STATEFUL_SET))
    }

    @Test
    fun `test ResourceType enum names`() {
        assertEquals("DEPLOYMENT", ResourceType.DEPLOYMENT.name)
        assertEquals("STATEFUL_SET", ResourceType.STATEFUL_SET.name)
    }

    @Test
    fun `test TargetResource creation with Deployment type`() {
        val resource = TargetResource(
            namespace = "default",
            deploymentName = "test-deployment",
            resourceType = ResourceType.DEPLOYMENT,
            consumerGroup = "test-group"
        )

        assertEquals("default", resource.namespace)
        assertEquals("test-deployment", resource.deploymentName)
        assertEquals(ResourceType.DEPLOYMENT, resource.resourceType)
        assertEquals("test-group", resource.consumerGroup)
    }

    @Test
    fun `test TargetResource creation with StatefulSet type`() {
        val resource = TargetResource(
            namespace = "prod",
            deploymentName = "test-statefulset",
            resourceType = ResourceType.STATEFUL_SET,
            consumerGroup = "prod-group"
        )

        assertEquals("prod", resource.namespace)
        assertEquals("test-statefulset", resource.deploymentName)
        assertEquals(ResourceType.STATEFUL_SET, resource.resourceType)
        assertEquals("prod-group", resource.consumerGroup)
    }

    @Test
    fun `test data class copy functionality`() {
        val original = TargetResource(
            namespace = "default",
            deploymentName = "original-deployment",
            resourceType = ResourceType.DEPLOYMENT,
            consumerGroup = "original-group"
        )

        val modified = original.copy(
            namespace = "new-namespace",
            deploymentName = "new-deployment"
        )

        // Check modified properties
        assertEquals("new-namespace", modified.namespace)
        assertEquals("new-deployment", modified.deploymentName)

        // Verify unmodified properties remain the same
        assertEquals(original.resourceType, modified.resourceType)
        assertEquals(original.consumerGroup, modified.consumerGroup)
    }

    @Test
    fun `test equals and hashCode contract`() {
        val resource1 = TargetResource(
            namespace = "test",
            deploymentName = "app",
            resourceType = ResourceType.DEPLOYMENT,
            consumerGroup = "group1"
        )

        val resource2 = TargetResource(
            namespace = "test",
            deploymentName = "app",
            resourceType = ResourceType.DEPLOYMENT,
            consumerGroup = "group1"
        )

        val resource3 = TargetResource(
            namespace = "different",
            deploymentName = "app",
            resourceType = ResourceType.DEPLOYMENT,
            consumerGroup = "group1"
        )

        assertEquals(resource1, resource2)
        assertEquals(resource1.hashCode(), resource2.hashCode())
        assertNotEquals(resource1, resource3)
        assertNotEquals(resource1.hashCode(), resource3.hashCode())
    }

}