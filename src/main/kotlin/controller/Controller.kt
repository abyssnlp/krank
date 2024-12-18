package com.github.abyssnlp.controller

import com.github.abyssnlp.common.ScalingConfiguration
import com.github.abyssnlp.common.TargetResource

interface Controller {
    suspend fun startMonitoring(): Unit
    suspend fun scaleDeployment(
        targetResource: TargetResource,
        scaleFactor: Int,
        scalingConfig: ScalingConfiguration,
        resourceName: String
    ): Unit
}