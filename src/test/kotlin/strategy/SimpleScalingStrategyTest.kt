package strategy

import com.github.abyssnlp.common.QueueMetrics
import com.github.abyssnlp.common.ScalingConfiguration
import com.github.abyssnlp.strategy.SimpleScalingStrategy
import io.mockk.every
import io.mockk.mockk
import kotlin.test.*

internal class SimpleScalingStrategyTest {

    private val scalingStrategy = SimpleScalingStrategy()

    @Test
    fun `test getScalingFactor with unhealthy metrics`() {
        val currentMetrics = mockk<QueueMetrics>()
        val config = mockk<ScalingConfiguration>()

        every { currentMetrics.isHealthy() } returns false

        val result = scalingStrategy.getScalingFactor(currentMetrics, null, config)
        assertEquals(0, result)
    }

    @Test
    fun `test getScalingFactor with null previous metrics`() {
        val currentMetrics = mockk<QueueMetrics>()
        val config = ScalingConfiguration(
            criticalThreshold = 1000,
            highThreshold = 500,
            normalThreshold = 100,
            minReplicas = 1,
            maxReplicas = 4,
            minScaleStep = 1,
            mediumScaleStep = 2,
            maxScaleStep = 3,
            cooldownPeriodMs = 20000
        )

        every { currentMetrics.isHealthy() } returns true
        every { currentMetrics.consumerLag } returns 1200

        val result = scalingStrategy.getScalingFactor(currentMetrics, null, config)
        assertEquals(3, result)
    }

    @Test
    fun `test getScalingFactor with increasing lag trend`() {
        val currentMetrics = mockk<QueueMetrics>()
        val previousMetrics = mockk<QueueMetrics>()
        val config = ScalingConfiguration(
            criticalThreshold = 1000,
            highThreshold = 500,
            normalThreshold = 100,
            minReplicas = 1,
            maxReplicas = 4,
            minScaleStep = 1,
            mediumScaleStep = 2,
            maxScaleStep = 3,
            cooldownPeriodMs = 20000
        )

        every { currentMetrics.isHealthy() } returns true
        every { currentMetrics.consumerLag } returns 1200
        every { previousMetrics.consumerLag } returns 800

        val result = scalingStrategy.getScalingFactor(currentMetrics, previousMetrics, config)
        assertEquals(2, result)
    }

    @Test
    fun `test getScalingFactor with decreasing lag trend`() {
        val currentMetrics = mockk<QueueMetrics>()
        val previousMetrics = mockk<QueueMetrics>()
        val config = ScalingConfiguration(
            criticalThreshold = 1000,
            highThreshold = 500,
            normalThreshold = 100,
            minReplicas = 1,
            maxReplicas = 4,
            minScaleStep = 1,
            mediumScaleStep = 2,
            maxScaleStep = 3,
            cooldownPeriodMs = 20000
        )

        every { currentMetrics.isHealthy() } returns true
        every { currentMetrics.consumerLag } returns 400
        every { previousMetrics.consumerLag } returns 800

        val result = scalingStrategy.getScalingFactor(currentMetrics, previousMetrics, config)
        assertEquals(0, result)
    }

    @Test
    fun `test getScalingFactor with stable lag trend`() {
        val currentMetrics = mockk<QueueMetrics>()
        val previousMetrics = mockk<QueueMetrics>()
        val config = ScalingConfiguration(
            criticalThreshold = 1000,
            highThreshold = 500,
            normalThreshold = 100,
            minReplicas = 1,
            maxReplicas = 4,
            minScaleStep = 1,
            mediumScaleStep = 2,
            maxScaleStep = 3,
            cooldownPeriodMs = 20000
        )

        every { currentMetrics.isHealthy() } returns true
        every { currentMetrics.consumerLag } returns 150
        every { previousMetrics.consumerLag } returns 150

        val result = scalingStrategy.getScalingFactor(currentMetrics, previousMetrics, config)
        assertEquals(1, result)
    }
}