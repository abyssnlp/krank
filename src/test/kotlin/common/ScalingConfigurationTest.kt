package common

import com.github.abyssnlp.common.ScalingConfiguration
import io.mockk.MockKAnnotations
import kotlin.test.*

internal class ScalingConfigurationTest {
    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
    }

    @Test
    fun `test default configuration values`() {
        val config = ScalingConfiguration()

        assertEquals(100L, config.normalThreshold)
        assertEquals(1000L, config.highThreshold)
        assertEquals(5000L, config.criticalThreshold)
        assertEquals(1, config.minReplicas)
        assertEquals(10, config.maxReplicas)
        assertEquals(1, config.minScaleStep)
        assertEquals(2, config.mediumScaleStep)
        assertEquals(3, config.maxScaleStep)
        assertEquals(30000L, config.cooldownPeriodMs)
    }

    @Test
    fun `test custom configuration values`() {
        val config = ScalingConfiguration(
            normalThreshold = 200L,
            highThreshold = 2000L,
            criticalThreshold = 10000L,
            minReplicas = 2,
            maxReplicas = 20,
            minScaleStep = 2,
            mediumScaleStep = 4,
            maxScaleStep = 6,
            cooldownPeriodMs = 60000L
        )

        assertEquals(200L, config.normalThreshold)
        assertEquals(2000L, config.highThreshold)
        assertEquals(10000L, config.criticalThreshold)
        assertEquals(2, config.minReplicas)
        assertEquals(20, config.maxReplicas)
        assertEquals(2, config.minScaleStep)
        assertEquals(4, config.mediumScaleStep)
        assertEquals(6, config.maxScaleStep)
        assertEquals(60000L, config.cooldownPeriodMs)
    }

    @Test
    fun `test threshold ordering invariant`() {
        val config = ScalingConfiguration()

        assertTrue(config.normalThreshold < config.highThreshold)
        assertTrue(config.highThreshold < config.criticalThreshold)
    }

    @Test
    fun `test replica range invariant`() {
        val config = ScalingConfiguration()

        assertTrue(config.minReplicas > 0)
        assertTrue(config.maxReplicas > config.minReplicas)
    }

    @Test
    fun `test scale step ordering invariant`() {
        val config = ScalingConfiguration()

        assertTrue(config.minScaleStep > 0)
        assertTrue(config.mediumScaleStep > config.minScaleStep)
        assertTrue(config.maxScaleStep > config.mediumScaleStep)
    }

    @Test
    fun `test cooldown period is positive`() {
        val config = ScalingConfiguration()

        assertTrue(config.cooldownPeriodMs > 0)
    }
}