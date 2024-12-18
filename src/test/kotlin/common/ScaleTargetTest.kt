package common

import com.github.abyssnlp.collectors.MetricsCollector
import com.github.abyssnlp.common.ResourceType
import com.github.abyssnlp.common.ScaleTarget
import com.github.abyssnlp.common.ScalingConfiguration
import com.github.abyssnlp.common.TargetResource
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

internal class ScaleTargetTest {
    private val mockCollector = mockk<MetricsCollector>()
    private val sampleScalingConfig = ScalingConfiguration(
        minReplicas = 1,
        maxReplicas = 4,
        cooldownPeriodMs = 10_000
    )

    private val sampleTargetResource = TargetResource(
        resourceType = ResourceType.DEPLOYMENT,
        namespace = "default",
        deploymentName = "test-deployment",
        consumerGroup = "test-consumer-group"
    )

    @Test
    fun `should create ScaleTarget with valid parameters`() {
        val scaleTarget = ScaleTarget(
            resourceName = "test-resource",
            collector = mockCollector,
            scalingConfiguration = sampleScalingConfig,
            targetResources = listOf(sampleTargetResource)
        )

        assertEquals("test-resource", scaleTarget.resourceName)
        assertEquals(mockCollector, scaleTarget.collector)
        assertEquals(sampleScalingConfig, scaleTarget.scalingConfiguration)
        assertEquals(listOf(sampleTargetResource), scaleTarget.targetResources)
    }

    @Test
    fun `should maintain immutability of target resources list`() {
        val mutableList = mutableListOf(sampleTargetResource)
        val scaleTarget = ScaleTarget(
            resourceName = "test-resource",
            collector = mockCollector,
            scalingConfiguration = sampleScalingConfig,
            targetResources = mutableList.toList()
        )

        mutableList.add(sampleTargetResource.copy(deploymentName = "new-deployment"))

        assertEquals(1, scaleTarget.targetResources.size)
        assertEquals(sampleTargetResource, scaleTarget.targetResources[0])
    }

    @Test
    fun `should correctly implement equals and hashCode`() {
        val scaleTarget1 = ScaleTarget(
            resourceName = "test-resource",
            collector = mockCollector,
            scalingConfiguration = sampleScalingConfig,
            targetResources = listOf(sampleTargetResource)
        )

        val scaleTarget2 = ScaleTarget(
            resourceName = "test-resource",
            collector = mockCollector,
            scalingConfiguration = sampleScalingConfig,
            targetResources = listOf(sampleTargetResource)
        )

        val scaleTarget3 = ScaleTarget(
            resourceName = "different-resource",
            collector = mockCollector,
            scalingConfiguration = sampleScalingConfig,
            targetResources = listOf(sampleTargetResource)
        )

        assertEquals(scaleTarget1, scaleTarget2)
        assertEquals(scaleTarget1.hashCode(), scaleTarget2.hashCode())
        assertNotEquals(scaleTarget1, scaleTarget3)
        assertNotEquals(scaleTarget1.hashCode(), scaleTarget3.hashCode())
    }

    @Test
    fun `should support destructuring`() {
        val scaleTarget = ScaleTarget(
            resourceName = "test-resource",
            collector = mockCollector,
            scalingConfiguration = sampleScalingConfig,
            targetResources = listOf(sampleTargetResource)
        )

        val (resourceName, collector, scalingConfig, targetResources) = scaleTarget

        assertEquals("test-resource", resourceName)
        assertEquals(mockCollector, collector)
        assertEquals(sampleScalingConfig, scalingConfig)
        assertEquals(listOf(sampleTargetResource), targetResources)
    }

    @Test
    fun `should handle empty target resources list`() {
        val scaleTarget = ScaleTarget(
            resourceName = "test-resource",
            collector = mockCollector,
            scalingConfiguration = sampleScalingConfig,
            targetResources = emptyList()
        )

        assertEquals(0, scaleTarget.targetResources.size)
        assert(scaleTarget.targetResources.isEmpty())
    }
}