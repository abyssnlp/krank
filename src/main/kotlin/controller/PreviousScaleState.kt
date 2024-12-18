package com.github.abyssnlp.controller

import com.github.abyssnlp.common.QueueMetrics

data class PreviousScaleState(
    var previousMetrics: QueueMetrics?,
    var lastScalingEventMs: Long
)
