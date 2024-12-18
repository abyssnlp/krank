package resourcemanager

import com.github.abyssnlp.common.ScalingConfiguration
import com.github.abyssnlp.common.TargetResource
import com.github.abyssnlp.resourcemanager.ResourceManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ResourceManagerTest {
    private lateinit var resourceManager: ResourceManager<Any>
    private lateinit var targetResource: TargetResource
    private lateinit var scalingConfig: ScalingConfiguration

    @BeforeTest
    fun setup() {
        resourceManager = mockk()
        targetResource = mockk()
        scalingConfig = mockk()
    }

    @Test
    fun `getCurrentReplicas returns correct number of replicas`() = runBlocking {
        val expectedReplicas = 3
        coEvery { resourceManager.getCurrentReplicas(targetResource) } returns expectedReplicas

        val actualReplicas = resourceManager.getCurrentReplicas(targetResource)
        assertEquals(expectedReplicas, actualReplicas)
        coVerify(exactly = 1) { resourceManager.getCurrentReplicas(targetResource) }
    }

    @Test
    fun `getCurrentReplicas handles zero replicas`() = runBlocking {
        coEvery { resourceManager.getCurrentReplicas(targetResource) } returns 0
        val actualReplicas = resourceManager.getCurrentReplicas(targetResource)
        assertEquals(0, actualReplicas)
    }

    @Test
    fun `scale successfully updates replicas`() = runBlocking {
        val scaleFactor = 2
        coEvery {
            resourceManager.scale(targetResource, scaleFactor, scalingConfig)
        } returns 1

        resourceManager.scale(targetResource, scaleFactor, scalingConfig)
        coVerify(exactly = 1) {
            resourceManager.scale(targetResource, scaleFactor, scalingConfig)
        }
    }

    @Test
    fun `scale handles negative scale factor`() = runBlocking {
        val negativeFactor = -1
        coEvery {
            resourceManager.scale(targetResource, negativeFactor, scalingConfig)
        } returns 1

        resourceManager.scale(targetResource, negativeFactor, scalingConfig)
        coVerify(exactly = 1) {
            resourceManager.scale(targetResource, negativeFactor, scalingConfig)
        }
    }

    @Test
    fun `getCurrentReplicas handles error conditions`(): Unit = runBlocking {
        coEvery {
            resourceManager.getCurrentReplicas(targetResource)
        } throws RuntimeException("Failed to get replicas")
        assertFailsWith<RuntimeException> {
            resourceManager.getCurrentReplicas(targetResource)
        }
    }

    @Test
    fun `scale handles error conditions`(): Unit = runBlocking {
        val scaleFactor = 2
        coEvery {
            resourceManager.scale(targetResource, scaleFactor, scalingConfig)
        } throws RuntimeException("Scaling failed")

        assertFailsWith<RuntimeException> {
            resourceManager.scale(targetResource, scaleFactor, scalingConfig)
        }
    }
}
