package com.github.abyssnlp.strategy

import com.github.abyssnlp.common.QueueMetrics
import com.github.abyssnlp.common.ScalingConfiguration

class SimpleScalingStrategy : ScalingStrategy {

    /**
     * This method calculates the scaling factor based on the current and previous [QueueMetrics].
     *
     * @param currentMetrics The current queue metrics.
     * @param previousMetrics The previous queue metrics.
     * @param config The scaling configuration.
     * @return The scaling factor.
     */
    override fun getScalingFactor(
        currentMetrics: QueueMetrics,
        previousMetrics: QueueMetrics?,
        config: ScalingConfiguration
    ): Int {
        if (!currentMetrics.isHealthy()) {
            return 0
        }

        if (previousMetrics == null) {
            return when {
                currentMetrics.consumerLag > config.criticalThreshold -> config.maxScaleStep
                currentMetrics.consumerLag > config.highThreshold -> config.mediumScaleStep
                currentMetrics.consumerLag > config.normalThreshold -> config.minScaleStep
                else -> 0
            }
        }

        val lagDelta = currentMetrics.consumerLag - previousMetrics.consumerLag
        val lagTrend = when {
            lagDelta > 0 -> LagTrend.INCREASING
            lagDelta < 0 -> LagTrend.DECREASING
            else -> LagTrend.STABLE
        }

        val lagChangeRate = if (previousMetrics.consumerLag != 0L) {
            (lagDelta.toDouble() / previousMetrics.consumerLag.toDouble()) * 100
        } else {
            0.0
        }

        return when (lagTrend) {
            LagTrend.INCREASING -> getScaleUpFactor(currentMetrics.consumerLag, lagChangeRate, config)
            LagTrend.DECREASING -> getScaleDownFactor(currentMetrics.consumerLag, lagChangeRate, config)
            LagTrend.STABLE -> if (currentMetrics.consumerLag > config.normalThreshold) 1 else 0
        }
    }

    private fun getScaleUpFactor(
        currentLag: Long,
        lagChangeRate: Double,
        config: ScalingConfiguration
    ): Int {
        if (lagChangeRate > 50) {
            return when {
                currentLag > config.criticalThreshold -> config.maxScaleStep
                currentLag > config.highThreshold -> config.maxScaleStep
                currentLag > config.normalThreshold -> config.mediumScaleStep
                else -> config.minScaleStep
            }
        }

        return when {
            currentLag > config.criticalThreshold -> config.mediumScaleStep
            currentLag > config.highThreshold -> config.minScaleStep
            currentLag > config.normalThreshold -> 1
            else -> 0
        }
    }

    private fun getScaleDownFactor(
        currentLag: Long,
        lagChangeRate: Double,
        config: ScalingConfiguration
    ): Int {
        if (currentLag > config.normalThreshold) {
            return 0
        }

        return when {
            lagChangeRate < -50 -> -config.maxScaleStep
            lagChangeRate < -25 -> -config.mediumScaleStep
            lagChangeRate < -10 -> -config.minScaleStep
            else -> 0
        }.coerceIn(-config.maxScaleStep, 0)
    }

}