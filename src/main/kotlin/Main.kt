package com.github.abyssnlp

import com.github.abyssnlp.collectors.KafkaMetricsCollector
import com.github.abyssnlp.common.*
import com.github.abyssnlp.config.ApplicationConfig
import com.github.abyssnlp.controller.AdaptiveScalingController
import com.github.abyssnlp.resourcemanager.KubernetesResourceManager
import com.github.abyssnlp.service.KrankService
import com.github.abyssnlp.strategy.SimpleScalingStrategy
import kotlinx.coroutines.*

suspend fun main() {

    val config = ApplicationConfig("/etc/config/scale-config.yaml").loadConfig()
    val scaleTargets = config.targets.map { target ->
        val metricsCollector = when (target.messageBroker.brokerType) {
            "kafka" -> KafkaMetricsCollector(
                securityConfig = target.messageBroker.config,
                consumerGroupIds = target.services.map { it.consumerGroup }
            )

            else ->
                throw KrankExceptions.MetricsCollectorNotImplementedException("Unsupported broker type")
        }
        ScaleTarget(
            resourceName = target.resourceName,
            collector = metricsCollector,
            scalingConfiguration = target.scalingConfiguration,
            targetResources = target.services
        )
    }
    val kubernetesResourceManager = KubernetesResourceManager()


    val controller = AdaptiveScalingController(
        resourceManager = kubernetesResourceManager,
        scaleTargets = scaleTargets,
        scalingStrategy = SimpleScalingStrategy()
    )

    val service = KrankService()

    runBlocking {
        controller.startMonitoring()
    }
}