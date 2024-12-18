package com.github.abyssnlp.collectors

import com.github.abyssnlp.common.KafkaSecurityConfig
import com.github.abyssnlp.common.MessageBrokerType
import com.github.abyssnlp.common.QueueMetrics
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.KafkaAdminClient
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.config.SaslConfigs
import org.slf4j.LoggerFactory
import java.io.File
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.util.Properties

data class SizeAndLag(val size: Long, val lag: Long)


class KafkaMetricsCollector(
    private val securityConfig: KafkaSecurityConfig,
    private val consumerGroupIds: List<String>,
) : MetricsCollector {

    private val adminClient: AdminClient by lazy {
        KafkaAdminClient.create(createKafkaProperties())
    }

    private fun createKafkaProperties(): Properties {
        val trustStore = createTrustStore(
            securityConfig.caCertPath,
            securityConfig.trustStorePassword
        )

        return Properties().apply {
            put("bootstrap.servers", securityConfig.bootstrapServers)
            put("security.protocol", securityConfig.securityProtocol)
            put("ssl.truststore.type", "JKS")
            put(SaslConfigs.SASL_MECHANISM, securityConfig.saslMechanism)
            put(SaslConfigs.SASL_JAAS_CONFIG, createJaasConfig())
            put("ssl.truststore.location", trustStore.absolutePath)
            put("ssl.truststore.password", securityConfig.trustStorePassword)
            put("key.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer")
            put("value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer")
        }
    }

    private fun createJaasConfig(): String {
        return when {
            securityConfig.saslMechanism == "PLAIN" -> {
                """
            org.apache.kafka.common.security.plain.PlainLoginModule required
            username="${securityConfig.saslUsername}"
            password="${securityConfig.saslPassword}";
            """.trimIndent()
            }

            else -> {
                """
            org.apache.kafka.common.security.scram.ScramLoginModule required
            username="${securityConfig.saslUsername}"
            password="${securityConfig.saslPassword}";
            """.trimIndent()
            }
        }
    }


    private fun createTrustStore(caCertPath: String, trustStorePassword: String): File {
        val trustStore = KeyStore.getInstance(KeyStore.getDefaultType())
        trustStore.load(null, trustStorePassword.toCharArray())

        val certificateFactory: CertificateFactory = CertificateFactory.getInstance("X.509")
        val certificate = File(caCertPath).inputStream().use { input ->
            certificateFactory.generateCertificate(input)
        }
        trustStore.setCertificateEntry("ca-cert", certificate)

        val tempTrustStore = File.createTempFile("kafka-truststore", ".jks")
        tempTrustStore.outputStream().use { output ->
            trustStore.store(output, trustStorePassword.toCharArray())
        }

        tempTrustStore.deleteOnExit()
        return tempTrustStore
    }


    override suspend fun collectMetrics(resourceName: String): Map<String, QueueMetrics> {
        val consumerGroupDescription = adminClient
            .describeConsumerGroups(consumerGroupIds)
            .all()
            .get()

        val consumerGroupPartitionInfo = getPartitionOffsets(resourceName)

        return consumerGroupPartitionInfo.mapValues { (consumerGroupId, partitionInfo) ->
            try {

                val totalSize = partitionInfo.values.sumOf { it.size }
                val totalLag = partitionInfo.values.sumOf { it.lag }

                QueueMetrics(
                    consumerGroup = consumerGroupId,
                    queueSize = totalSize,
                    consumerLag = totalLag,
                    brokerType = MessageBrokerType.KAFKA,
                    additionalMetrics = mapOf(
                        "activeConsumers" to
                                (consumerGroupDescription[consumerGroupId]?.members()?.size ?: 0),
                        "partitionCount" to partitionInfo.size
                    )
                )
            } catch (e: Exception) {
                logger.error(
                    "Failed to collect metrics for " +
                            "consumer: $consumerGroupId resource $resourceName, error: ${e.message}", e
                )
                QueueMetrics(
                    consumerGroup = consumerGroupId,
                    -1,
                    -1,
                    MessageBrokerType.KAFKA,
                    additionalMetrics = mapOf("error" to e.message.toString())
                )
            }
        }
    }

    private fun getPartitionOffsets(topic: String): Map<String, Map<TopicPartition, SizeAndLag>> {
        val partitions = adminClient
            .describeTopics(listOf(topic))
            .allTopicNames()
            .get()
            ?.get(topic)
            ?.partitions()
            ?.map { TopicPartition(topic, it.partition()) }
            ?: emptyList()


        val consumerGroupMap = consumerGroupIds.associateWith { consumerGroupId ->
            val consumer = KafkaConsumer<ByteArray, ByteArray>(createKafkaProperties().apply {
                put("group.id", consumerGroupId)
            })

            val partitionLags = consumer.use { consumer ->
                val latestOffsets = consumer.endOffsets(partitions)
                val committedOffsets = consumer.committed(partitions.toSet())

                partitions.associateWith { partition ->
                    val latestOffset = latestOffsets[partition] ?: 0
                    val committedOffset = committedOffsets[partition]?.offset() ?: 0
                    SizeAndLag(
                        size = latestOffset,
                        lag = maxOf(latestOffset - committedOffset, 0L)
                    )
                }
            }

            partitionLags.forEach { (partition, lag) ->
                logger.info(
                    " Consumer: {}, Topic: {}, Partition: {}, Lag: {}",
                    consumerGroupId, topic, partition, lag
                )
            }
            partitionLags
        }
        return consumerGroupMap
    }

    companion object {
        private val logger = LoggerFactory.getLogger(KafkaMetricsCollector::class.java)
    }
}