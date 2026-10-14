package com.callbackdev.chiaro.ui.journal

import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The strip read with a finger (review of the Journal, C.1): which square, and what it says. */
class DriftScrubTest {

    private val saturday = LocalDate.of(2026, 10, 17)
    private val t0 = Instant.parse("2026-10-14T00:00:00Z")
    private val drift = DriftModel(
        dates = listOf(saturday, saturday.plusDays(1)),
        columns = listOf(t0, null, t0.plusSeconds(12 * 3600L), t0.plusSeconds(18 * 3600L)),
        rain = listOf(listOf(52, null, 40, 30), listOf(null, null, null, 10)),
        highC = listOf(listOf(18.0, null, 19.0, 21.0), listOf(null, null, null, 20.0)),
        frostC = listOf(null, null),
        columnHours = 6
    )

    @Test
    fun `the finger's square, rows sharing the height and columns what the names leave`() {
        // 40 px of names, 4 columns of 40 px, 2 rows of 20 px.
        assertEquals(DriftCell(0, 0), driftCellAt(45f, 5f, 200f, 40f, 40f, 2, 4))
        assertEquals(DriftCell(1, 2), driftCellAt(125f, 30f, 200f, 40f, 40f, 2, 4))
        assertEquals(DriftCell(1, 3), driftCellAt(199f, 39f, 200f, 40f, 40f, 2, 4))
    }

    @Test
    fun `past an edge the edge's square holds`() {
        assertEquals(DriftCell(0, 0), driftCellAt(10f, -8f, 200f, 40f, 40f, 2, 4))
        assertEquals(DriftCell(1, 3), driftCellAt(260f, 90f, 200f, 40f, 40f, 2, 4))
    }

    @Test
    fun `a square reads its update's two numbers`() {
        val reading = driftReading(drift, DriftCell(0, 2))
        assertEquals(DriftReading(saturday, t0.plusSeconds(12 * 3600L), true, 40, 19.0), reading)
    }

    @Test
    fun `an empty slot says where it falls and that no update landed`() {
        val reading = driftReading(drift, DriftCell(0, 1))
        assertFalse(reading.fetched)
        assertEquals(t0.plusSeconds(6 * 3600L), reading.at)
        assertNull(reading.rainPct)
    }

    @Test
    fun `an update that did not reach the day has no numbers for it`() {
        val reading = driftReading(drift, DriftCell(1, 0))
        assertTrue(reading.fetched)
        assertNull(reading.rainPct)
        assertNull(reading.highC)
    }
}
