package com.github.abyssnlp.collectors

import com.github.abyssnlp.common.QueueMetrics

interface MetricsCollector {
    suspend fun collectMetrics(resourceName: String): Map<String, QueueMetrics>
}
