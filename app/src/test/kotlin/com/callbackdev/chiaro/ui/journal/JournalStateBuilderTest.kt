package com.callbackdev.chiaro.ui.journal

import com.callbackdev.chiaro.data.FetchFailure
import com.callbackdev.chiaro.data.FetchFailureReason
import com.callbackdev.chiaro.domain.model.City
import com.callbackdev.chiaro.domain.model.Coordinates
import com.callbackdev.chiaro.domain.sky.SkyRun
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JournalStateBuilderTest {

    private val zone = ZoneId.of("Europe/Rome")
    private val milan = City(
        id = 1L, name = "Milano", region = "Lombardia", country = "Italia",
        coordinates = Coordinates(45.4643, 9.1895), timezone = "Europe/Rome"
    )
    private val saturday = "2026-09-05"

    private fun at(day: Int, hour: Int): Instant =
        LocalDate.of(2026, 9, day).atTime(hour, 0).atZone(zone).toInstant()

    private fun forecast(precip: Int, high: Double) = mapOf(
        "$saturday.status" to "Rain",
        "$saturday.high_c" to high.toString(),
        "$saturday.low_c" to "14.0",
        "$saturday.precip_pct" to precip.toString()
    )

    private fun row(
        at: Instant,
        forecast: Map<String, String> = emptyMap(),
        fired: List<String> = emptyList(),
        runs: List<SkyRun> = emptyList()
    ) = JournalRow(at, forecast, fired, runs)

    @Test
    fun `a rain drop reads as Saturday improved, with the numbers`() {
        val rows = listOf(
            row(at(3, 7), forecast(precip = 30, high = 27.0)),
            row(at(2, 7), forecast(precip = 70, high = 24.0))
        )
        val content = JournalStateBuilder.build(milan, rows, emptyList())

        val shift = content.days.flatMap { it.entries }
            .filterIsInstance<JournalEntry.ForecastShift>()
            .single()
        assertEquals(LocalDate.parse(saturday), shift.date)
        assertEquals(true, shift.better)
        val rain = shift.shifts.first { it.field == "precip_pct" }
        assertEquals("70", rain.old)
        assertEquals("30", rain.new)
    }

    @Test
    fun `a day entering the horizon is not a revision worth a sentence`() {
        val rows = listOf(row(at(2, 7), forecast(precip = 70, high = 24.0)))
        val content = JournalStateBuilder.build(milan, rows, emptyList())

        assertTrue(
            content.days.flatMap { it.entries }
                .filterIsInstance<JournalEntry.ForecastShift>()
                .isEmpty()
        )
    }

    @Test
    fun `rules, sky runs and failures land in their days, newest first`() {
        val rows = listOf(
            row(
                at(3, 7),
                fired = listOf("bici"),
                runs = listOf(SkyRun("sun.rise", at(3, 6).epochSecond, "PASS", cloudPct = 8))
            )
        )
        val failures = listOf(FetchFailure("k", at(2, 9).epochSecond, FetchFailureReason.OFFLINE))
        val content = JournalStateBuilder.build(milan, rows, failures)

        assertEquals(listOf(LocalDate.of(2026, 9, 3), LocalDate.of(2026, 9, 2)),
            content.days.map { it.date })
        val day3 = content.days.first().entries
        assertTrue(day3.first() is JournalEntry.RuleFired) // 07:00 before the 06:00 run
        val run = day3.filterIsInstance<JournalEntry.SkyObserved>().single()
        assertEquals(8, run.cloudPct)
        assertNotNull(run.verdict)
        val failed = content.days.last().entries.single() as JournalEntry.FetchFailed
        assertEquals(FetchFailureReason.OFFLINE, failed.reason)
    }

    @Test
    fun `a skipped sky run keeps its row but carries no verdict`() {
        val rows = listOf(
            row(at(3, 7), runs = listOf(SkyRun("sun.set", at(3, 5).epochSecond, kind = null)))
        )
        val content = JournalStateBuilder.build(milan, rows, emptyList())
        val run = content.days.single().entries
            .filterIsInstance<JournalEntry.SkyObserved>().single()
        assertNull(run.verdict)
    }

    @Test
    fun `one fetch is not a drift, two are`() {
        val one = JournalStateBuilder.build(
            milan, listOf(row(at(2, 7), forecast(70, 24.0))), emptyList()
        )
        assertNull(one.drift)

        val two = JournalStateBuilder.build(
            milan,
            listOf(row(at(3, 7), forecast(30, 27.0)), row(at(2, 7), forecast(70, 24.0))),
            emptyList()
        )
        val drift = two.drift
        assertNotNull(drift)
        assertEquals(listOf(LocalDate.parse(saturday)), drift!!.dates)
        // Columns oldest → newest, values in fetch order.
        assertEquals(listOf(70, 30), drift.rain.single())
        assertEquals(listOf(24.0, 27.0), drift.highC.single())
    }

    @Test
    fun `a fetch whose horizon missed a day leaves an absent cell, not a zero`() {
        val rows = listOf(
            row(at(3, 7), forecast(30, 27.0)),
            row(at(2, 7), emptyMap()),          // no forecast stored on this one
            row(at(1, 7), forecast(70, 24.0))
        )
        val drift = JournalStateBuilder.build(milan, rows, emptyList()).drift
        assertNotNull(drift)
        // The empty fetch is not a column at all: it said nothing about any day.
        assertEquals(2, drift!!.fetches.size)
        assertEquals(listOf(70, 30), drift.rain.single())
    }
}
