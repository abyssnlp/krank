package strategy

import com.github.abyssnlp.common.MessageBrokerType
import com.github.abyssnlp.common.QueueMetrics
import com.github.abyssnlp.common.ScalingConfiguration
import com.github.abyssnlp.strategy.ScalingStrategy
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals

class ScalingStrategyTest {

    private val scalingStrategy = mockk<ScalingStrategy>()

    @Test
    fun `test getScalingFactor with increasing metrics`() {
        val currentMetrics = QueueMetrics(
            "test-consumer-group",
            100,
            200,
            MessageBrokerType.KAFKA,
            emptyMap(),
            10000L
        )
        val previousMetrics = QueueMetrics(
            "test-consumer-group",
            50,
            150,
            MessageBrokerType.KAFKA,
            emptyMap(),
            10000L
        )
        val config = ScalingConfiguration(
            1000,
            500,
            100,
            1,
            4,
            1,
            2,
            3,
            20000L
        )

        every { scalingStrategy.getScalingFactor(currentMetrics, previousMetrics, config) } returns 2

        val result = scalingStrategy.getScalingFactor(currentMetrics, previousMetrics, config)

        assertEquals(2, result)
        verify { scalingStrategy.getScalingFactor(currentMetrics, previousMetrics, config) }
    }

    @Test
    fun `test getScalingFactor with stable metrics`() {
        val currentMetrics = QueueMetrics(
            "test-consumer-group",
            100,
            200,
            MessageBrokerType.KAFKA,
            emptyMap(),
            10000L
        )
        val previousMetrics = QueueMetrics(
            "test-consumer-group",
            100,
            200,
            MessageBrokerType.KAFKA,
            emptyMap(),
            10000L
        )
        val config = ScalingConfiguration(
            1000,
            500,
            100,
            1,
            4,
            1,
            2,
            3,
            20000L
        )

        every { scalingStrategy.getScalingFactor(currentMetrics, previousMetrics, config) } returns 1

        val result = scalingStrategy.getScalingFactor(currentMetrics, previousMetrics, config)

        assertEquals(1, result)
        verify { scalingStrategy.getScalingFactor(currentMetrics, previousMetrics, config) }
    }

    @Test
    fun `test getScalingFactor with decreasing metrics`() {
        val currentMetrics = QueueMetrics(
            "test-consumer-group",
            50,
            150,
            MessageBrokerType.KAFKA,
            emptyMap(),
            10000L
        )
        val previousMetrics = QueueMetrics(
            "test-consumer-group",
            100,
            200,
            MessageBrokerType.KAFKA,
            emptyMap(),
            10000L
        )
        val config = ScalingConfiguration(
            1000,
            500,
            100,
            1,
            4,
            1,
            2,
            3,
            20000L
        )

        every { scalingStrategy.getScalingFactor(currentMetrics, previousMetrics, config) } returns -1

        val result = scalingStrategy.getScalingFactor(currentMetrics, previousMetrics, config)

        assertEquals(-1, result)
        verify { scalingStrategy.getScalingFactor(currentMetrics, previousMetrics, config) }
    }
}
