package strategy

import com.github.abyssnlp.strategy.LagTrend
import kotlin.test.*

internal class LagTrendTest {

    @Test
    fun `verify all expected enum values exist`() {
        // Verify all values are present
        val expectedValues = setOf("INCREASING", "DECREASING", "STABLE")
        val actualValues = LagTrend.entries.map { it.name }.toSet()
        assertEquals(expectedValues, actualValues)
    }

    @Test
    fun `verify enum has exactly three values`() {
        assertEquals(3, LagTrend.entries.size)
    }

    @Test
    fun `verify valueOf returns correct enum value`() {
        assertEquals(LagTrend.INCREASING, LagTrend.valueOf("INCREASING"))
        assertEquals(LagTrend.DECREASING, LagTrend.valueOf("DECREASING"))
        assertEquals(LagTrend.STABLE, LagTrend.valueOf("STABLE"))
    }

    @Test
    fun `verify enum values are not null`() {
        LagTrend.entries.forEach {
            assertNotNull(it)
        }
    }

    @Test
    fun `verify correct ordinal values`() {
        assertEquals(0, LagTrend.INCREASING.ordinal)
        assertEquals(1, LagTrend.DECREASING.ordinal)
        assertEquals(2, LagTrend.STABLE.ordinal)
    }

    @Test
    fun `verify enum value name matches declaration`() {
        assertEquals("INCREASING", LagTrend.INCREASING.name)
        assertEquals("DECREASING", LagTrend.DECREASING.name)
        assertEquals("STABLE", LagTrend.STABLE.name)
    }
}