package com.github.abyssnlp.controller

import com.github.abyssnlp.common.QueueMetrics
import com.github.abyssnlp.common.ScaleTarget
import com.github.abyssnlp.common.ScalingConfiguration
import com.github.abyssnlp.common.TargetResource
import com.github.abyssnlp.resourcemanager.ResourceManager
import com.github.abyssnlp.service.metrics.PrometheusMetricsServer
import com.github.abyssnlp.strategy.ScalingStrategy
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory

class AdaptiveScalingController<T>(
    private val resourceManager: ResourceManager<T>,
    private val scaleTargets: List<ScaleTarget>,
    private val scalingStrategy: ScalingStrategy,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) : Controller {

    private val stateManager = ScalingStateManager()
    private val metricsServer = PrometheusMetricsServer.instance

    private suspend fun collectMetricsForTarget(scaleTarget: ScaleTarget): Map<String, QueueMetrics> {
        val metrics = scaleTarget.collector.collectMetrics(scaleTarget.resourceName)
        logger.debug("Metrics for {}: {}", scaleTarget.resourceName, metrics)
        return metrics
    }

    private suspend fun processScaleTarget(scaleTarget: ScaleTarget) {
        logger.info("Monitoring ${scaleTarget.resourceName}")

        val metrics = collectMetricsForTarget(scaleTarget)
        if (metrics.isEmpty()) return

        scaleTarget.targetResources.forEach { targetResource ->
            supervisorScope {
                launch {
                    processTargetResource(targetResource, scaleTarget, metrics)
                }
            }
        }
    }

    private suspend fun processTargetResource(
        targetResource: TargetResource,
        scaleTarget: ScaleTarget,
        metrics: Map<String, QueueMetrics>
    ) {
        val currentTime = System.currentTimeMillis()
        val state = stateManager.getState(scaleTarget.resourceName, targetResource.consumerGroup)
        val consumerGroupMetrics = metrics[targetResource.consumerGroup]

        if (shouldScale(consumerGroupMetrics, currentTime, state, scaleTarget.scalingConfiguration)) {
            performScaling(
                targetResource,
                consumerGroupMetrics!!,
                state.previousMetrics,
                scaleTarget,
                currentTime
            )
        }
    }

    private fun shouldScale(
        metrics: QueueMetrics?,
        currentTime: Long,
        state: PreviousScaleState,
        config: ScalingConfiguration
    ): Boolean {
        return metrics != null &&
                metrics.isHealthy() &&
                currentTime - state.lastScalingEventMs > config.cooldownPeriodMs
    }

    private suspend fun performScaling(
        targetResource: TargetResource,
        currentMetrics: QueueMetrics,
        previousMetrics: QueueMetrics?,
        scaleTarget: ScaleTarget,
        currentTime: Long
    ) {
        val scaleFactor = scalingStrategy.getScalingFactor(
            currentMetrics,
            previousMetrics,
            scaleTarget.scalingConfiguration
        )
        logScalingAttempt(targetResource, scaleFactor)
        scaleDeployment(targetResource, scaleFactor, scaleTarget.scalingConfiguration, scaleTarget.resourceName)

        stateManager.updateState(
            scaleTarget.resourceName,
            targetResource.consumerGroup,
            PreviousScaleState(currentMetrics, currentTime)
        )
    }

    private fun logScalingAttempt(targetResource: TargetResource, scaleFactor: Int) {
        logger.info(
            "Trying to scale {}, {}.{} by {}",
            targetResource.resourceType,
            targetResource.namespace,
            targetResource.deploymentName,
            scaleFactor
        )
    }

    override suspend fun startMonitoring() = supervisorScope {
        while (isActive) {
            try {
                scaleTargets.forEach { scaleTarget ->
                    logger.debug("State Manager: {}", stateManager)
                    processScaleTarget(scaleTarget)
                }
            } catch (e: Exception) {
                logger.error("Error: {} in monitoring loop, for {}", e, scaleTargets)
            }
            delay(MONITORING_INTERVAL_MS)
        }

    }

    override suspend fun scaleDeployment(
        targetResource: TargetResource, scaleFactor: Int,
        scalingConfig: ScalingConfiguration,
        resourceName: String
    ) {
        try {
            val finalReplicaCount = resourceManager.scale(targetResource, scaleFactor, scalingConfig)
            metricsServer.recordScalingEvent(resourceName, targetResource.consumerGroup)
            metricsServer.setReplicas(resourceName, targetResource.deploymentName, finalReplicaCount.toDouble())
        } catch (e: Exception) {
            metricsServer.recordScalingFailure(resourceName, targetResource.consumerGroup)
            logger.error(
                "Error scaling {}.{} by {}",
                targetResource.namespace,
                targetResource.deploymentName,
                scaleFactor,
                e
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AdaptiveScalingController::class.java)
        private const val MONITORING_INTERVAL_MS = 10_000L
    }
}