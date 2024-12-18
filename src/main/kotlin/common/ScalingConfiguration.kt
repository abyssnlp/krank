package com.github.abyssnlp.common

import kotlinx.serialization.Serializable

@Serializable
data class ScalingConfiguration(
    val normalThreshold: Long = 100,
    val highThreshold: Long = 1000,
    val criticalThreshold: Long = 5000,
    val minReplicas: Int = 1,
    val maxReplicas: Int = 10,
    val minScaleStep: Int = 1,
    val mediumScaleStep: Int = 2,
    val maxScaleStep: Int = 3,
    val cooldownPeriodMs: Long = 30000 // 30 seconds cooldown between scaling operations
)
