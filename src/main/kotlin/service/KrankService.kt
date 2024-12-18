package com.github.abyssnlp.service

import com.github.abyssnlp.service.metrics.PrometheusMetricsServer
import io.javalin.Javalin
import io.prometheus.client.exporter.common.TextFormat


class KrankService {
    // TODO: Move metrics port to config
    private val app = Javalin.create().start(8000)
    private val metricsServer = PrometheusMetricsServer.instance

    init {
        app.get("/") { ctx -> ctx.result("Hello World") }

        app.get("/healthz") { ctx -> ctx.status(200).json(mapOf("status" to "ok")) }
        app.get("/live") { ctx -> ctx.status(200).json(mapOf("status" to "ok")) }
        app.get("/metrics") { ctx ->
            ctx.contentType(TextFormat.CONTENT_TYPE_004)
            ctx.result(metricsServer.getMetrics())
        }
    }
}