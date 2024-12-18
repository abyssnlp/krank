package controller

import com.github.abyssnlp.common.*
import com.github.abyssnlp.collectors.MetricsCollector
import com.github.abyssnlp.controller.AdaptiveScalingController
import com.github.abyssnlp.resourcemanager.ResourceManager
import com.github.abyssnlp.strategy.ScalingStrategy
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.BeforeEach
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class AdaptiveScalingControllerTest {
    private lateinit var resourceManager: ResourceManager<Any>
    private lateinit var scaleTargets: List<ScaleTarget>
    private lateinit var scalingStrategy: ScalingStrategy
    private lateinit var controller: AdaptiveScalingController<Any>
    private lateinit var metricsCollector: MetricsCollector
    private lateinit var testScope: TestScope

    @BeforeEach
    fun setup() {
        resourceManager = mockk(relaxed = true)
        scalingStrategy = mockk()
        metricsCollector = mockk()
        testScope = TestScope()

        val targetResource = TargetResource(
            namespace = "test-namespace",
            deploymentName = "test-deployment",
            resourceType = ResourceType.DEPLOYMENT,
            consumerGroup = "test-consumer-group"
        )

        val scaleTarget = ScaleTarget(
            resourceName = "test-resource",
            collector = metricsCollector,
            scalingConfiguration = ScalingConfiguration(),
            targetResources = listOf(targetResource)
        )

        scaleTargets = listOf(scaleTarget)

        controller = AdaptiveScalingController(
            resourceManager = resourceManager,
            scaleTargets = scaleTargets,
            scalingStrategy = scalingStrategy,
            coroutineScope = testScope
        )
    }

    @Test
    fun `test successful scaling operation`() = runTest {
        val metrics = QueueMetrics(
            consumerGroup = "test-consumer-group",
            queueSize = 1000L,
            consumerLag = 500L,
            brokerType = MessageBrokerType.KAFKA
        )

        coEvery { metricsCollector.collectMetrics(any()) } returns mapOf(
            "test-consumer-group" to metrics
        )
        coEvery { resourceManager.getCurrentReplicas(any()) } returns 1
        every { scalingStrategy.getScalingFactor(any(), any(), any()) } returns 2

        // Launch controller with timeout
        val job = launch {
            controller.startMonitoring()
        }

        // Allow some time for the controller to process
        advanceTimeBy(3000)

        // Verify interactions
        coVerify {
            metricsCollector.collectMetrics(any())
            scalingStrategy.getScalingFactor(any(), any(), any())
            resourceManager.scale(any(), any(), any())
        }

        // Cancel the monitoring job
        job.cancel()
    }

    @Test
    fun `test cooldown period prevents scaling`() = runTest {
        val metrics = QueueMetrics(
            consumerGroup = "test-consumer-group",
            queueSize = 1000L,
            consumerLag = 500L,
            brokerType = MessageBrokerType.KAFKA
        )

        coEvery {
            metricsCollector.collectMetrics(any())
        } returns mapOf("test-consumer-group" to metrics)

        val job = launch {
            controller.startMonitoring()
        }
        testScope.advanceTimeBy(5_000) // Within cooldown period

        coVerify(exactly = 0) {
            resourceManager.scale(any(), any(), any())
        }

        job.cancel()
    }

    @Test
    fun `test error handling during scaling`() = runTest {
        // Setup error condition
        val metrics = QueueMetrics(
            consumerGroup = "test-consumer-group",
            queueSize = 1000L,
            consumerLag = 500L,
            brokerType = MessageBrokerType.KAFKA
        )

        // Setup mocks with correct matchers
        coEvery { metricsCollector.collectMetrics("test-resource") } returns mapOf(
            "test-consumer-group" to metrics
        )
        coEvery { resourceManager.getCurrentReplicas(any<TargetResource>()) } returns 1
        every { scalingStrategy.getScalingFactor(any(), any(), any()) } returns 2
        coEvery {
            resourceManager.scale(
                any<TargetResource>(),
                any<Int>(),
                any<ScalingConfiguration>()
            )
        } throws RuntimeException("Scaling failed")

        // Launch controller
        val job = launch {
            controller.startMonitoring()
        }

        // Allow time for processing
        advanceTimeBy(3000)

        // Verify interactions
        coVerify {
            metricsCollector.collectMetrics("test-resource")
            resourceManager.scale(
                any(),
                any<Int>(),
                any()
            )
        }

        job.cancel()
    }

    @Test
    fun `test empty metrics handling`() = runTest {
        coEvery {
            metricsCollector.collectMetrics(any())
        } returns emptyMap()

        val job = launch {
            controller.startMonitoring()
        }
        testScope.advanceTimeBy(11_000)

        coVerify(exactly = 0) {
            resourceManager.scale(any(), any(), any())
        }
        job.cancel()
    }

    @Test
    fun `test unhealthy metrics prevent scaling`() = runTest {
        val metrics = QueueMetrics(
            consumerGroup = "test-consumer-group",
            queueSize = -1L, // Unhealthy value
            consumerLag = 500L,
            brokerType = MessageBrokerType.KAFKA
        )

        coEvery {
            metricsCollector.collectMetrics(any())
        } returns mapOf("test-consumer-group" to metrics)

        val job = launch {
            controller.startMonitoring()
        }
        testScope.advanceTimeBy(11_000)

        coVerify(exactly = 0) {
            resourceManager.scale(any(), any(), any())
        }

        job.cancel()
    }
}