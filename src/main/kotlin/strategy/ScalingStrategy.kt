package com.github.abyssnlp.strategy

import com.github.abyssnlp.common.QueueMetrics
import com.github.abyssnlp.common.ScalingConfiguration

interface ScalingStrategy {
    fun getScalingFactor(
        currentMetrics: QueueMetrics,
        previousMetrics: QueueMetrics?,
        config: ScalingConfiguration
    ): Int
}