package com.github.abyssnlp.service.metrics

import io.prometheus.client.CollectorRegistry
import io.prometheus.client.Counter
import io.prometheus.client.Gauge
import io.prometheus.client.exporter.common.TextFormat
import io.prometheus.client.hotspot.DefaultExports
import java.io.StringWriter

class PrometheusMetricsServer {

    private val registry = CollectorRegistry.defaultRegistry

    // Application metrics
    private val scalingEvents = Counter.build()
        .name("scaling_events_total")
        .labelNames("resource_name", "consumer_group")
        .help("Total number of scaling events")
        .register()

    private val scalingFailures = Counter.build()
        .name("scaling_failures_total")
        .labelNames("resource_name", "consumer_group")
        .help("Total number of scaling failures")
        .register()

    private val currentReplicas = Gauge.build()
        .name("current_replicas")
        .labelNames("resource_name", "deployment_name")
        .help("Current number of replicas")
        .register()

    init {
        DefaultExports.initialize()
    }

    fun recordScalingEvent(resourceName: String, consumerGroup: String) {
        scalingEvents.labels(resourceName, consumerGroup).inc()
    }

    fun recordScalingFailure(resourceName: String, consumerGroup: String) {
        scalingFailures.labels(resourceName, consumerGroup).inc()
    }

    fun setReplicas(resourceName: String, deploymentName: String, replicas: Double) {
        currentReplicas.labels(resourceName, deploymentName).set(replicas)
    }

    fun getMetrics(): String {
        val writer = StringWriter()
        TextFormat.write004(writer, registry.metricFamilySamples())
        return writer.toString()
    }

    companion object {
        val instance by lazy { PrometheusMetricsServer() }
    }
}