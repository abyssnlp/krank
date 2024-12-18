package com.github.abyssnlp.resourcemanager

import com.github.abyssnlp.common.ScalingConfiguration
import com.github.abyssnlp.common.TargetResource

interface ResourceManager<T> {
    suspend fun getCurrentReplicas(targetResource: T): Int
    suspend fun scale(targetResource: TargetResource, scaleFactor: Int, scalingConfig: ScalingConfiguration): Int
}
