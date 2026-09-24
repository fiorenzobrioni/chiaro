package com.callbackdev.chiaro.ui.journal

import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * "How the forecast did" (design review, 23 set 2026) is averages, never a score: the
 * high's mean distance from what was seen, and the rain it was given on the days it
 * rained against the days it did not.
 */
class OutcomeStatsTest {

    private fun day(pct: Int, rained: Boolean, forecast: Double?, seen: Double?) = JournalEntry.DayOutcome(
        at = Instant.EPOCH, date = LocalDate.of(2026, 9, 20), forecastPrecipPct = pct, rained = rained,
        forecastHighC = forecast, observedHighC = seen, coveredHours = 20
    )

    @Test
    fun `the high's error is the mean distance, whichever side it missed on`() {
        val stats = outcomeStats(listOf(day(10, false, 20.0, 21.0), day(10, false, 20.0, 18.0), day(10, false, null, 20.0)))
        assertEquals(1.5, stats.highErrorC!!, 1e-9)
        assertEquals(2, stats.highDays)
    }

    @Test
    fun `rain is averaged apart for the wet days and the dry ones`() {
        val stats = outcomeStats(listOf(day(80, true, null, null), day(61, true, null, null), day(10, false, null, null)))
        assertEquals(71, stats.wetAvgPct)
        assertEquals(10, stats.dryAvgPct)
        assertNull(stats.highErrorC)
    }

    @Test
    fun `a week with no rain has no wet average`() {
        val stats = outcomeStats(listOf(day(20, false, null, null), day(30, false, null, null)))
        assertNull(stats.wetAvgPct)
        assertEquals(25, stats.dryAvgPct)
    }
}
