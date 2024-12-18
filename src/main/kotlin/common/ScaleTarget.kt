package com.github.abyssnlp.common

import com.github.abyssnlp.collectors.MetricsCollector

data class ScaleTarget(
    val resourceName: String,
    val collector: MetricsCollector,
    val scalingConfiguration: ScalingConfiguration,
    val targetResources: List<TargetResource>,
)
