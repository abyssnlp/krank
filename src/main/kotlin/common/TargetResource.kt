package com.github.abyssnlp.common

import kotlinx.serialization.Serializable

enum class ResourceType {
    DEPLOYMENT,
    STATEFUL_SET
}

@Serializable
data class TargetResource(
    val namespace: String,
    val deploymentName: String,
    val resourceType: ResourceType,
    val consumerGroup: String
)
