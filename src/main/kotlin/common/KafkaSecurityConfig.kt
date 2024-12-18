package com.github.abyssnlp.common

import kotlinx.serialization.Serializable

@Serializable
data class KafkaSecurityConfig(
    val bootstrapServers: String,
    val saslUsername: String,
    val saslPassword: String,
    val saslMechanism: String = "SCRAM-SHA-256",
    val securityProtocol: String = "SASL_SSL",
    val caCertPath: String,
    val trustStorePassword: String,
)
