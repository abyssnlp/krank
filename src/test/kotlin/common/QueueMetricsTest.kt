package common

import com.github.abyssnlp.common.MessageBrokerType
import com.github.abyssnlp.common.QueueMetrics
import com.github.abyssnlp.common.ScalingConfiguration
import com.github.abyssnlp.strategy.ScalingStrategy
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.*

internal class QueueMetricsTest {

    private lateinit var mockStrategy: ScalingStrategy
    private lateinit var config: ScalingConfiguration

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
        mockStrategy = mockk()
        config = ScalingConfiguration()
    }

    @Test
    fun `test MessageBrokerType enum values`() {
        assertEquals(2, MessageBrokerType.entries.size)
        assertTrue(MessageBrokerType.entries.contains(MessageBrokerType.KAFKA))
        assertTrue(MessageBrokerType.entries.contains(MessageBrokerType.RABBITMQ))
    }

    @Test
    fun `test QueueMetrics creation with minimal parameters`() {
        val metrics = QueueMetrics(
            consumerGroup = "test-group",
            queueSize = 100L,
            consumerLag = 50L,
            brokerType = MessageBrokerType.KAFKA
        )

        assertEquals("test-group", metrics.consumerGroup)
        assertEquals(100L, metrics.queueSize)
        assertEquals(50L, metrics.consumerLag)
        assertEquals(MessageBrokerType.KAFKA, metrics.brokerType)
        assertTrue(metrics.additionalMetrics.isEmpty())
        assertTrue(metrics.timestamp > 0)
    }

    @Test
    fun `test QueueMetrics creation with all parameters`() {
        val additionalMetrics = mapOf("key1" to "value1", "key2" to 123)
        val timestamp = System.currentTimeMillis()

        val metrics = QueueMetrics(
            consumerGroup = "test-group",
            queueSize = 100L,
            consumerLag = 50L,
            brokerType = MessageBrokerType.RABBITMQ,
            additionalMetrics = additionalMetrics,
            timestamp = timestamp
        )

        assertEquals("test-group", metrics.consumerGroup)
        assertEquals(100L, metrics.queueSize)
        assertEquals(50L, metrics.consumerLag)
        assertEquals(MessageBrokerType.RABBITMQ, metrics.brokerType)
        assertEquals(additionalMetrics, metrics.additionalMetrics)
        assertEquals(timestamp, metrics.timestamp)
    }

    @Test
    fun `test isHealthy returns true for valid metrics`() {
        val metrics = QueueMetrics(
            consumerGroup = "test-group",
            queueSize = 0L,
            consumerLag = 0L,
            brokerType = MessageBrokerType.KAFKA
        )

        assertTrue(metrics.isHealthy())
    }

    @Test
    fun `test isHealthy returns true for positive metrics`() {
        val metrics = QueueMetrics(
            consumerGroup = "test-group",
            queueSize = 100L,
            consumerLag = 50L,
            brokerType = MessageBrokerType.KAFKA
        )

        assertTrue(metrics.isHealthy())
    }

    @Test
    fun `test isHealthy returns false for negative queue size`() {
        val metrics = QueueMetrics(
            consumerGroup = "test-group",
            queueSize = -1L,
            consumerLag = 0L,
            brokerType = MessageBrokerType.KAFKA
        )

        assertFalse(metrics.isHealthy())
    }

    @Test
    fun `test isHealthy returns false for negative consumer lag`() {
        val metrics = QueueMetrics(
            consumerGroup = "test-group",
            queueSize = 0L,
            consumerLag = -1L,
            brokerType = MessageBrokerType.KAFKA
        )

        assertFalse(metrics.isHealthy())
    }

    @Test
    fun `test getScalingFactor delegates to strategy`() {
        val currentMetrics = QueueMetrics(
            consumerGroup = "test-group",
            queueSize = 100L,
            consumerLag = 50L,
            brokerType = MessageBrokerType.KAFKA
        )

        val previousMetrics = QueueMetrics(
            consumerGroup = "test-group",
            queueSize = 80L,
            consumerLag = 30L,
            brokerType = MessageBrokerType.KAFKA
        )

        every {
            mockStrategy.getScalingFactor(currentMetrics, previousMetrics, config)
        } returns 2

        val scalingFactor = currentMetrics.getScalingFactor(config, previousMetrics, mockStrategy)

        assertEquals(2, scalingFactor)
        verify(exactly = 1) {
            mockStrategy.getScalingFactor(currentMetrics, previousMetrics, config)
        }
    }

    @Test
    fun `test getScalingFactor works with null previous metrics`() {
        val currentMetrics = QueueMetrics(
            consumerGroup = "test-group",
            queueSize = 100L,
            consumerLag = 50L,
            brokerType = MessageBrokerType.KAFKA
        )

        every {
            mockStrategy.getScalingFactor(currentMetrics, null, config)
        } returns 1

        val scalingFactor = currentMetrics.getScalingFactor(config, null, mockStrategy)

        assertEquals(1, scalingFactor)
        verify(exactly = 1) {
            mockStrategy.getScalingFactor(currentMetrics, null, config)
        }
    }
}