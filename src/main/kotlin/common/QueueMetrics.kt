package com.github.abyssnlp.common

import com.github.abyssnlp.strategy.ScalingStrategy

enum class MessageBrokerType {
    KAFKA,
    RABBITMQ
}

data class QueueMetrics(
    val consumerGroup: String,
    val queueSize: Long,
    val consumerLag: Long,
    val brokerType: MessageBrokerType,
    val additionalMetrics: Map<String, Any> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis()
) {
    fun isHealthy(): Boolean {
        return queueSize >= 0 && consumerLag >= 0
    }

    fun getScalingFactor(
        config: ScalingConfiguration,
        previousMetrics: QueueMetrics?,
        strategy: ScalingStrategy
    ): Int {
        return strategy.getScalingFactor(this, previousMetrics, config)
    }
}
