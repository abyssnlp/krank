package com.github.abyssnlp.config

import com.github.abyssnlp.common.KafkaSecurityConfig
import com.github.abyssnlp.common.ScalingConfiguration
import com.github.abyssnlp.common.TargetResource
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import java.io.File

@Serializable
data class KrankConfig(
    val targets: List<KrankTarget>
)

@Serializable
data class KrankTarget(
    val resourceName: String,
    val messageBroker: BrokerConfig,
    val services: List<TargetResource>,
    val scalingConfiguration: ScalingConfiguration
)

@Serializable
data class BrokerConfig(
    val brokerType: String,
    val config: KafkaSecurityConfig
)


class ApplicationConfig(private val configPath: String) {
    private val yaml = Yaml(
        configuration = YamlConfiguration(
            strictMode = true
        )
    )

    fun loadConfig(): KrankConfig {
        return try {
            val path = configPath

            yaml.decodeFromString<KrankConfig>(File(path).readText())
        } catch (e: Exception) {
            logger.error("Error loading configuration", e)
            throw e
        }
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(ApplicationConfig::class.java)
    }
}