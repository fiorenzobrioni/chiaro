package com.callbackdev.chiaro.ui.journal

import com.callbackdev.chiaro.data.FetchFailure
import com.callbackdev.chiaro.data.FetchFailureReason
import com.callbackdev.chiaro.domain.model.City
import com.callbackdev.chiaro.domain.model.Coordinates
import com.callbackdev.chiaro.data.local.WarningRecordKind
import com.callbackdev.chiaro.domain.sky.SkyRun
import com.callbackdev.chiaro.domain.warnings.WarningHazard
import com.callbackdev.chiaro.domain.warnings.WarningLevel
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

    /** Somewhere on the 3rd, with Saturday still ahead: the drift's rows are the days
     * that have not happened yet. */
    private val now = at(3, 12)

    private fun forecast(precip: Int, high: Double, low: Double = 14.0) = mapOf(
        "$saturday.status" to "Rain",
        "$saturday.high_c" to high.toString(),
        "$saturday.low_c" to low.toString(),
        "$saturday.precip_pct" to precip.toString()
    )

    private fun row(
        at: Instant,
        forecast: Map<String, String> = emptyMap(),
        fired: List<String> = emptyList(),
        runs: List<SkyRun> = emptyList(),
        snapshot: Map<String, String> = emptyMap()
    ) = JournalRow(at, forecast, fired, runs, snapshot)

    private fun build(
        rows: List<JournalRow>,
        failures: List<FetchFailure> = emptyList(),
        now: Instant = this.now,
        warnings: List<WarningRecordRow> = emptyList()
    ) = JournalStateBuilder.build(milan, rows, failures, now, warnings)

    // ------------------------------------------------- the official warnings (Fase 11)

    private fun warningRow(
        at: Instant = this.at(3, 16),
        kind: WarningRecordKind = WarningRecordKind.LEVEL_CHANGE,
        day: LocalDate? = LocalDate.of(2026, 9, 4),
        hazard: WarningHazard? = WarningHazard.THUNDERSTORM,
        from: WarningLevel? = WarningLevel.NONE,
        to: WarningLevel? = WarningLevel.ORANGE,
        issuedAt: java.time.LocalDateTime? = LocalDate.of(2026, 9, 3).atTime(15, 46)
    ) = WarningRecordRow(at, kind, issuedAt, "Nodo Idraulico di Milano", day, hazard, from, to)

    private fun entries(warnings: List<WarningRecordRow>) =
        build(rows = emptyList(), warnings = warnings).days.flatMap { it.entries }

    @Test
    fun `a level that rose becomes a line of its own, on its own day`() {
        val entry = entries(listOf(warningRow())).single() as JournalEntry.WarningChanged
        assertEquals(WarningLevel.ORANGE, entry.to)
        assertEquals(WarningLevel.NONE, entry.from)
        assertEquals(LocalDate.of(2026, 9, 4), entry.day)
        assertEquals(LocalDate.of(2026, 9, 3), entry.at.atZone(zone).toLocalDate())
    }

    /** A level coming down is the Journal's business precisely because it is not the
     * notifier's: the engine never fires for it, so this is where it is recorded. */
    @Test
    fun `a level that fell to NONE is still a line`() {
        val entry = entries(
            listOf(warningRow(from = WarningLevel.ORANGE, to = WarningLevel.NONE))
        ).single() as JournalEntry.WarningChanged
        assertEquals(WarningLevel.NONE, entry.to)
    }

    @Test
    fun `a bulletin that was not reached is its own kind, with what still stands`() {
        val entry = entries(
            listOf(
                warningRow(
                    kind = WarningRecordKind.BULLETIN_MISSED,
                    day = null, hazard = null, from = null, to = null
                )
            )
        ).single() as JournalEntry.BulletinMissed
        assertEquals(LocalDate.of(2026, 9, 3).atTime(15, 46), entry.heldFrom)
    }

    /** A row a later build wrote with a hazard this one cannot read is dropped, not
     * guessed at: half a sentence is worse than no line. */
    @Test
    fun `a level change missing what it is about is not drawn`() {
        assertTrue(entries(listOf(warningRow(hazard = null))).isEmpty())
        assertTrue(entries(listOf(warningRow(day = null))).isEmpty())
        assertTrue(entries(listOf(warningRow(to = null))).isEmpty())
    }

    @Test
    fun `warning lines share the day with everything else that happened`() {
        val content = build(
            rows = listOf(row(at(3, 9), forecast(70, 24.0)), row(at(3, 15), forecast(30, 24.0))),
            warnings = listOf(warningRow())
        )
        val day = content.days.single { it.date == LocalDate.of(2026, 9, 3) }
        assertTrue(day.entries.any { it is JournalEntry.WarningChanged })
        assertTrue(day.entries.any { it is JournalEntry.ForecastShift })
        // Newest first inside a day: 16:00 leads 15:00.
        assertTrue(day.entries.first() is JournalEntry.WarningChanged)
    }

    @Test
    fun `a rain drop reads as Saturday improved, with the numbers`() {
        val rows = listOf(
            row(at(3, 7), forecast(precip = 30, high = 27.0)),
            row(at(2, 7), forecast(precip = 70, high = 24.0))
        )
        val content = build(rows)

        val shift = content.days.flatMap { it.entries }
            .filterIsInstance<JournalEntry.ForecastShift>()
            .single()
        assertEquals(LocalDate.parse(saturday), shift.date)
        assertEquals(true, shift.better)
        val rain = shift.shifts.first { it.field == "precip_pct" }
        assertEquals("70", rain.old)
        assertEquals("30", rain.new)
    }

    /**
     * The Journal folds a day's revisions of one target day into one line (8 set 2026):
     * three hourly fetches that walked Saturday's high 24 → 25 → 27 and its rain
     * 70 → 60 → 30 are one sentence, first value to last, that says it took three
     * updates. Today's "what changed" still reads the newest fetch alone.
     */
    @Test
    fun `a day's revisions of one target day fold into one line`() {
        val rows = listOf(
            row(at(3, 9), forecast(precip = 30, high = 27.0)),
            row(at(3, 8), forecast(precip = 60, high = 25.0)),
            row(at(3, 7), forecast(precip = 70, high = 24.0)),
            row(at(2, 7), forecast(precip = 70, high = 24.0))
        )
        val content = build(rows)

        val shift = content.days.flatMap { it.entries }
            .filterIsInstance<JournalEntry.ForecastShift>()
            .single()
        assertEquals(at(3, 9), shift.at)
        assertEquals(2, shift.revisions) // the 07:00 fetch changed nothing
        assertEquals("70", shift.shifts.first { it.field == "precip_pct" }.old)
        assertEquals("30", shift.shifts.first { it.field == "precip_pct" }.new)
        assertEquals("24.0", shift.shifts.first { it.field == "high_c" }.old)
        assertEquals("27.0", shift.shifts.first { it.field == "high_c" }.new)
        assertEquals(true, shift.better)

        // Today's list is per fetch: the newest one moved rain by 30 and the high by 2.
        val latest = JournalStateBuilder.latestShifts(rows).single()
        assertEquals(1, latest.revisions)
        assertEquals("60", latest.shifts.first { it.field == "precip_pct" }.old)
    }

    @Test
    fun `a value that came back where it started is not a change`() {
        // Rain 70 → 30 → 70 within the day: nothing to say about rain. The high moved
        // 24 → 26 and stayed, so the line survives with the high alone and no verdict.
        val rows = listOf(
            row(at(3, 9), forecast(precip = 70, high = 26.0)),
            row(at(3, 8), forecast(precip = 30, high = 26.0)),
            row(at(2, 7), forecast(precip = 70, high = 24.0))
        )
        val shift = build(rows).days.flatMap { it.entries }
            .filterIsInstance<JournalEntry.ForecastShift>()
            .single()
        assertEquals(listOf("high_c"), shift.shifts.map { it.field })
        assertNull(shift.better)

        // And when every field comes back, the day has no line at all.
        val quiet = listOf(
            row(at(3, 9), forecast(precip = 70, high = 24.0)),
            row(at(3, 8), forecast(precip = 30, high = 26.0)),
            row(at(2, 7), forecast(precip = 70, high = 24.0))
        )
        assertTrue(
            build(quiet).days.flatMap { it.entries }
                .filterIsInstance<JournalEntry.ForecastShift>()
                .isEmpty()
        )
    }

    @Test
    fun `a day entering the horizon is not a revision worth a sentence`() {
        val content = build(listOf(row(at(2, 7), forecast(precip = 70, high = 24.0))))

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
        val content = build(rows, failures)

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
        val content = build(rows)
        val run = content.days.single().entries
            .filterIsInstance<JournalEntry.SkyObserved>().single()
        assertNull(run.verdict)
    }

    @Test
    fun `one fetch is not a drift, two are`() {
        assertNull(build(listOf(row(at(2, 7), forecast(70, 24.0)))).drift)

        val two = build(
            listOf(row(at(3, 7), forecast(30, 27.0)), row(at(2, 7), forecast(70, 24.0)))
        )
        val drift = two.drift
        assertNotNull(drift)
        assertEquals(listOf(LocalDate.parse(saturday)), drift!!.dates)
        // Columns oldest → newest, values in column order.
        assertEquals(listOf(70, 30), drift.rain.single().filterNotNull())
        assertEquals(listOf(24.0, 27.0), drift.highC.single().filterNotNull())
        assertEquals(at(2, 7), drift.oldest)
        assertEquals(at(3, 7), drift.newest)
    }

    @Test
    fun `the axis is time, so a day between two fetches is drawn as a gap`() {
        // Twenty-four hours apart at a six-hour column: four slots, three of them empty.
        val drift = build(
            listOf(row(at(3, 7), forecast(30, 27.0)), row(at(2, 7), forecast(70, 24.0)))
        ).drift!!
        assertEquals(5, drift.columns.size)
        assertEquals(2, drift.columns.count { it != null })
        assertEquals(listOf(70, null, null, null, 30), drift.rain.single())
    }

    @Test
    fun `two fetches inside the same slot are one column, and one column is no drift`() {
        val drift = build(
            listOf(row(at(3, 7), forecast(30, 27.0)), row(at(3, 10), forecast(20, 27.0)))
        ).drift
        assertNull(drift)
    }

    @Test
    fun `a fetch whose horizon missed a day leaves an absent cell, not a zero`() {
        val rows = listOf(
            row(at(3, 7), forecast(30, 27.0)),
            row(at(2, 7), emptyMap()),          // no forecast stored on this one
            row(at(1, 7), forecast(70, 24.0))
        )
        val drift = build(rows).drift
        assertNotNull(drift)
        // The empty fetch said nothing about any day, so its slot stays empty; the two
        // that spoke are forty-eight hours and eight columns apart.
        assertEquals(9, drift!!.columns.size)
        assertEquals(2, drift.columns.count { it != null })
        assertEquals(listOf(70, 30), drift.rain.single().filterNotNull())
    }

    @Test
    fun `a horizon gone stale offline loses its rows instead of showing a past week`() {
        // The newest fetch is four days old: everything it called "tomorrow" has
        // already happened, and a strip of days gone by is not the week ahead.
        val rows = listOf(
            row(at(3, 7), forecast(30, 27.0)), row(at(2, 7), forecast(70, 24.0))
        )
        assertNull(build(rows, now = at(9, 12)).drift)
    }

    @Test
    fun `a day forecast to freeze is marked, and a day above zero is not`() {
        val mild = build(
            listOf(row(at(3, 7), forecast(30, 8.0)), row(at(2, 7), forecast(70, 6.0)))
        ).drift!!
        assertNull(mild.frostC.single())

        val freezing = build(
            listOf(
                row(at(3, 7), forecast(30, 8.0, low = -1.5)),
                row(at(2, 7), forecast(70, 6.0, low = 2.0))
            )
        ).drift!!
        assertEquals(-1.5, freezing.frostC.single()!!, 0.001)
    }

    @Test
    fun `the mark follows the latest word, not the coldest one ever said`() {
        // Yesterday's fetch said −3°, this morning's says +2°: nobody is standing by
        // the frost any more, so the day loses its mark.
        val warmed = build(
            listOf(
                row(at(3, 7), forecast(30, 8.0, low = 2.0)),
                row(at(2, 7), forecast(70, 6.0, low = -3.0))
            )
        ).drift!!
        assertNull(warmed.frostC.single())
    }

    @Test
    fun `zero itself is freezing`() {
        val drift = build(
            listOf(
                row(at(3, 7), forecast(30, 8.0, low = 0.0)),
                row(at(2, 7), forecast(70, 6.0, low = 1.0))
            )
        ).drift!!
        assertEquals(0.0, drift.frostC.single()!!, 0.001)
    }

    // -----------------------------------------------------------------------------
    // The loop closed: what the app said, against what it then saw.
    // -----------------------------------------------------------------------------

    private fun observed(mm: Double) = mapOf(
        "current.wmo_code" to (if (mm > 0) "63" else "3"),
        "current.precip_last_hour_mm" to mm.toString(),
        "current.temp_c" to "21.0"
    )

    @Test
    fun `a finished day is judged, and its verdict opens the day it closes`() {
        val target = LocalDate.of(2026, 9, 3)
        val rows = listOf(row(at(2, 22), mapOf("$target.precip_pct" to "70", "$target.high_c" to "24.0"))) +
            (0..23).map { row(at(3, it), snapshot = observed(mm = 0.0)) }
        val content = build(rows, now = at(4, 12))

        val day = content.days.single { it.date == target }
        val outcome = day.entries.first() as JournalEntry.DayOutcome
        assertEquals(target, outcome.date)
        assertEquals(70, outcome.forecastPrecipPct)
        assertEquals(false, outcome.rained)
        assertEquals(24.0, outcome.forecastHighC!!, 0.001)
        assertEquals(21.0, outcome.observedHighC!!, 0.001)
    }

    @Test
    fun `a day the app could not watch enough gets no line at all`() {
        val target = LocalDate.of(2026, 9, 3)
        val rows = listOf(row(at(2, 22), mapOf("$target.precip_pct" to "70"))) +
            (8..12).map { row(at(3, it), snapshot = observed(mm = 0.0)) }

        assertTrue(
            build(rows, now = at(4, 12)).days
                .flatMap { it.entries }
                .filterIsInstance<JournalEntry.DayOutcome>()
                .isEmpty()
        )
    }
}
