package collectors

import com.github.abyssnlp.collectors.MetricsCollector
import com.github.abyssnlp.common.MessageBrokerType
import com.github.abyssnlp.common.QueueMetrics
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

internal class MetricsCollectorTest {

    private val metricsCollector = mockk<MetricsCollector>()

    @Test
    fun `test collectMetrics returns correct metrics`() = runBlocking {
        val resourceName = "testResource"
        val expectedMetrics = mapOf(
            "metric1" to QueueMetrics(
                "test-consumer-group",
                100,
                200,
                MessageBrokerType.KAFKA,
                emptyMap(),
                10000L
            ),
            "metric2" to QueueMetrics(
                "test-consumer-group",
                50,
                150,
                MessageBrokerType.KAFKA,
                emptyMap(),
                10000L
            )
        )

        coEvery { metricsCollector.collectMetrics(resourceName) } returns expectedMetrics

        val result = metricsCollector.collectMetrics(resourceName)
        assertEquals(expectedMetrics, result)
    }

    @Test
    fun `test collectMetrics with empty metrics`() = runBlocking {
        val resourceName = "testResource"
        val expectedMetrics = emptyMap<String, QueueMetrics>()

        coEvery { metricsCollector.collectMetrics(resourceName) } returns expectedMetrics

        val result = metricsCollector.collectMetrics(resourceName)
        assertEquals(expectedMetrics, result)
    }
}