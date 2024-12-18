package collectors

import com.github.abyssnlp.common.KafkaSecurityConfig
import kotlin.test.Test
import kotlin.test.assertEquals

class KafkaSecurityConfigTest {

    @Test
    fun `test KafkaSecurityConfig with default values`() {
        val config = KafkaSecurityConfig(
            bootstrapServers = "localhost:9092",
            saslUsername = "user",
            saslPassword = "password",
            caCertPath = "/path/to/ca.pem",
            trustStorePassword = "changeit"
        )

        assertEquals("localhost:9092", config.bootstrapServers)
        assertEquals("user", config.saslUsername)
        assertEquals("password", config.saslPassword)
        assertEquals("SCRAM-SHA-256", config.saslMechanism)
        assertEquals("SASL_SSL", config.securityProtocol)
        assertEquals("/path/to/ca.pem", config.caCertPath)
        assertEquals("changeit", config.trustStorePassword)
    }

    @Test
    fun `test KafkaSecurityConfig with custom values`() {
        val config = KafkaSecurityConfig(
            bootstrapServers = "localhost:9092",
            saslUsername = "user",
            saslPassword = "password",
            saslMechanism = "PLAIN",
            securityProtocol = "PLAINTEXT",
            caCertPath = "/path/to/ca.pem",
            trustStorePassword = "changeit"
        )

        assertEquals("localhost:9092", config.bootstrapServers)
        assertEquals("user", config.saslUsername)
        assertEquals("password", config.saslPassword)
        assertEquals("PLAIN", config.saslMechanism)
        assertEquals("PLAINTEXT", config.securityProtocol)
        assertEquals("/path/to/ca.pem", config.caCertPath)
        assertEquals("changeit", config.trustStorePassword)
    }
}