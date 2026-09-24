package com.callbackdev.chiaro.data.local

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ForecastOutcomeTest {

    private val zone: ZoneId = ZoneId.of("Europe/Rome")
    private val saturday: LocalDate = LocalDate.of(2026, 9, 5)
    private val sunday: LocalDate = saturday.plusDays(1)

    private fun at(date: LocalDate, hour: Int, minute: Int = 0): Long =
        LocalDateTime.of(date, java.time.LocalTime.of(hour, minute))
            .atZone(zone).toInstant().epochSecond

    /** What the app was carrying the evening before: one commit with Saturday in it. */
    private fun forecastOf(precipPct: Int?, highC: Double = 27.0): Map<String, String> = buildMap {
        put("$saturday.status", "Rainy 🌧️")
        put("$saturday.high_c", highC.toString())
        put("$saturday.low_c", "14.0")
        precipPct?.let { put("$saturday.precip_pct", it.toString()) }
    }

    /**
     * A commit written at [hour]:[minute] of [date], in the shape since 24 set 2026: the
     * three hours closed by then (ending on the hour, newest first) with [hourMm] in them,
     * and the quarter of an hour behind the reading with [quarterMm].
     */
    private fun observation(
        date: LocalDate,
        hour: Int,
        minute: Int = 0,
        wmoCode: Int = 3,
        hourMm: List<Double> = listOf(0.0, 0.0, 0.0),
        quarterMm: Double = 0.0,
        tempC: (Int) -> Double = { 20.0 }
    ): Map<String, String> {
        val lastEnd = LocalDateTime.of(date, java.time.LocalTime.of(hour, 0))
        val ends = hourMm.indices.map { lastEnd.minusHours(it.toLong()) }
        return mapOf(
            "current.wmo_code" to wmoCode.toString(),
            "current.precip_quarter_mm" to quarterMm.toString(),
            "current.temp_c" to tempC(hour).toString(),
            WeatherSnapshots.PAST_HOURS_END to ends.joinToString(",") { it.atZone(zone).toInstant().toString() },
            WeatherSnapshots.PAST_HOURS_MM to hourMm.joinToString(","),
            WeatherSnapshots.PAST_HOURS_TEMP_C to ends.joinToString(",") { tempC(it.hour).toString() }
        )
    }

    /** The shape every commit had before 24 set 2026: one amount, which was a quarter. */
    private fun legacyObservation(wmoCode: Int, mm: Double, tempC: Double = 20.0) = mapOf(
        "current.wmo_code" to wmoCode.toString(),
        "current.precip_last_hour_mm" to mm.toString(),
        "current.temp_c" to tempC.toString()
    )

    private fun baseline(precipPct: Int? = 70, highC: Double = 27.0) =
        ForecastOutcome.Fetch(at(saturday.minusDays(1), 22), forecastOf(precipPct, highC), emptyMap())

    /** Commits on the hour through Saturday, each carrying the three hours behind it. */
    private fun watched(
        hours: Iterable<Int>,
        wmoCode: Int = 3,
        tempC: (Int) -> Double = { 20.0 }
    ) = hours.map {
        ForecastOutcome.Fetch(
            at(saturday, it), emptyMap(), observation(saturday, it, wmoCode = wmoCode, tempC = tempC)
        )
    }

    private fun compute(fetches: List<ForecastOutcome.Fetch>) =
        ForecastOutcome.compute(fetches, zone, today = sunday)

    @Test
    fun `a dry day the app watched all through says so, with its coverage`() {
        val outcome = compute(listOf(baseline()) + watched(0..23)).single()

        assertEquals(saturday, outcome.date)
        assertEquals(70, outcome.forecastPrecipPct)
        assertEquals(false, outcome.rained)
        // 00:00 through 23:00: the last hour of the day closes at midnight, and only a
        // commit written after it can carry its amount — Sunday's first one.
        assertEquals(23, outcome.coveredHours)
    }

    @Test
    fun `one wet hour is proof, whatever the coverage`() {
        // Three commits only — nowhere near the floor — but one of them carries rain.
        val fetches = listOf(baseline()) +
            watched(listOf(9, 10)) +
            ForecastOutcome.Fetch(
                at(saturday, 16), emptyMap(),
                observation(saturday, 16, wmoCode = 63, hourMm = listOf(2.4, 0.0, 0.0))
            )
        val outcome = compute(fetches).single()

        assertTrue(outcome.rained)
        // Exactly the hours with an amount: 06-10 from the first two, 13-16 from the
        // third. Nothing between 10 and 13 is vouched for, because nothing says it.
        assertEquals(7, outcome.coveredHours)
    }

    @Test
    fun `millimetres alone are enough, even under a dry code`() {
        // The shower ended before the reading: the code says overcast, the hour behind
        // it carries 0.6 mm. The hour is the honest evidence.
        val fetches = listOf(baseline()) +
            watched(0..12) +
            ForecastOutcome.Fetch(
                at(saturday, 13), emptyMap(),
                observation(saturday, 13, wmoCode = 3, hourMm = listOf(0.6, 0.0, 0.0))
            ) +
            watched(14..23)
        assertTrue(compute(fetches).single().rained)
    }

    /**
     * The defect of 24 set 2026 itself: a reading every hour saw a dry quarter each time
     * and the day was called dry, while the hour series behind the same readings had
     * the rain in it. Here the rain fell between the quarters and only the hours know.
     */
    @Test
    fun `rain between two readings is in the hours, not in the quarters`() {
        val fetches = listOf(baseline()) + watched(0..13) +
            ForecastOutcome.Fetch(
                at(saturday, 14, 20), emptyMap(),
                observation(saturday, 14, minute = 20, wmoCode = 3, hourMm = listOf(1.8, 0.0, 0.0), quarterMm = 0.0)
            ) + watched(15..23)
        assertTrue(compute(fetches).single().rained)
    }

    @Test
    fun `a day the app barely watched gets no verdict at all`() {
        // Six dry readings, eight hours of amounts: not rain, and not enough of the day
        // to claim it stayed dry.
        assertTrue(compute(listOf(baseline()) + watched(8..13)).isEmpty())
    }

    @Test
    fun `a refresh spree does not buy coverage`() {
        // Twenty commits inside one hour carry the same three closed hours and twenty
        // overlapping quarters: four hours of day between them, not twenty.
        val spree = (0..19).map {
            ForecastOutcome.Fetch(at(saturday, 15, it * 3), emptyMap(), observation(saturday, 15, minute = it * 3))
        }
        assertTrue(compute(listOf(baseline()) + spree).isEmpty())
    }

    /**
     * A day watched every two hours — the top of the update range — is a day watched,
     * and now for a reason that is true: each commit carries three closed hours, so two
     * readings two hours apart leave no hour between them without an amount. The rule
     * it replaces let each reading vouch for up to two hours it had no amount for.
     */
    @Test
    fun `a day watched every two hours is covered, not half covered`() {
        val outcome = compute(listOf(baseline()) + watched(0..22 step 2)).single()
        assertEquals(false, outcome.rained)
        assertEquals(22, outcome.coveredHours)
    }

    @Test
    fun `a night the phone slept through stays uncovered`() {
        // Readings at 00:00 and 06:00, then hourly: the 06:00 one reaches back to 03:00,
        // and 00:00-03:00 stays what it was — unseen. 3-23 is twenty hours.
        val outcome = compute(listOf(baseline()) + watched(listOf(0, 6) + (7..23))).single()
        assertEquals(20, outcome.coveredHours)
    }

    @Test
    fun `the rain stays on the hour the millimetres describe, whichever day reads it`() {
        // A wet reading at 01:30 on Sunday whose newest closed hour, 00-01, holds the
        // rain: Sunday's, not Saturday's, although the commit also carries 23-24.
        val sundayRain = listOf(baseline()) + watched(0..23) + listOf(
            ForecastOutcome.Fetch(
                at(sunday, 1, 30), emptyMap(),
                observation(sunday, 1, minute = 30, wmoCode = 63, hourMm = listOf(2.0, 0.0, 0.0))
            )
        )
        val dry = ForecastOutcome.compute(sundayRain, zone, today = sunday.plusDays(1))
            .single { it.date == saturday }
        assertEquals(false, dry.rained)
        // …and the last hour of Saturday, read only on Sunday, is Saturday's rain.
        val lateRain = listOf(baseline()) + watched(0..23) + listOf(
            ForecastOutcome.Fetch(
                at(sunday, 0, 10), emptyMap(),
                observation(sunday, 0, minute = 10, wmoCode = 3, hourMm = listOf(0.8, 0.0, 0.0))
            )
        )
        val wet = ForecastOutcome.compute(lateRain, zone, today = sunday.plusDays(1))
            .single { it.date == saturday }
        assertTrue(wet.rained)
        assertEquals(24, wet.coveredHours)
    }

    @Test
    fun `the high is the warmest reading seen, and only when the day was covered`() {
        val warm = compute(
            listOf(baseline(highC = 27.0)) + watched(0..23, tempC = { hour -> 10.0 + hour })
        ).single()
        assertEquals(27.0, warm.forecastHighC!!, 0.001)
        assertEquals(33.0, warm.observedHighC!!, 0.001)

        // Same day, seen once in the rain: the verdict stands, the maximum does not.
        val glimpsed = compute(
            listOf(baseline()) +
                ForecastOutcome.Fetch(
                    at(saturday, 16), emptyMap(),
                    observation(saturday, 16, wmoCode = 63, hourMm = listOf(1.0, 0.0, 0.0), tempC = { 24.0 })
                )
        ).single()
        assertTrue(glimpsed.rained)
        assertNull(glimpsed.observedHighC)
        assertNull(glimpsed.forecastHighC)
    }

    @Test
    fun `the high counts the closed hours too, not only the instants of the readings`() {
        // Readings every two hours at a steady 20°, while the hour between two of them
        // (15:00) was the day's warmest: the commit at 16:00 carries it.
        val fetches = listOf(baseline()) + (0..22 step 2).map { h ->
            ForecastOutcome.Fetch(
                at(saturday, h), emptyMap(),
                observation(saturday, h, tempC = { hour -> if (hour == 15) 29.0 else 20.0 })
            )
        }
        assertEquals(29.0, compute(fetches).single().observedHighC!!, 0.001)
    }

    /**
     * Commits written before 24 set 2026 keep what they really were. Their one amount,
     * under `precip_last_hour_mm`, was Open-Meteo's quarter of an hour: a day watched
     * every hour in that shape has six hours of evidence, not twenty-four, and gets no
     * "stayed dry" — while a wet one is still proof.
     */
    @Test
    fun `a commit from before the fix is a quarter of an hour of evidence`() {
        val legacyDay = (0..23).map {
            ForecastOutcome.Fetch(at(saturday, it), emptyMap(), legacyObservation(3, 0.0))
        }
        assertTrue(compute(listOf(baseline()) + legacyDay).isEmpty())

        val legacyWet = legacyDay.take(10) +
            ForecastOutcome.Fetch(at(saturday, 10), emptyMap(), legacyObservation(3, 0.2))
        val outcome = compute(listOf(baseline()) + legacyWet).single()
        assertTrue(outcome.rained)
        assertEquals(2, outcome.coveredHours)
    }

    /** The quarter is the provider's, ending at its `current.time`, not at the commit. */
    @Test
    fun `a wet quarter counts for the day it fell in, not the day it was saved`() {
        // Saved at 00:20 on Sunday; the quarter it carries is Saturday's 23:30-23:45.
        // Read off the commit's own time it would have been Sunday's 00:05-00:20.
        val snapshot = legacyObservation(3, 0.3) + (
            WeatherSnapshots.QUARTER_END to LocalDateTime.of(saturday, java.time.LocalTime.of(23, 45))
                .atZone(zone).toInstant().toString()
            )
        val outcome = ForecastOutcome.compute(
            listOf(baseline()) + watched(0..23) +
                ForecastOutcome.Fetch(at(sunday, 0, 20), emptyMap(), snapshot),
            zone, today = sunday.plusDays(1)
        ).single { it.date == saturday }
        assertTrue(outcome.rained)
    }

    @Test
    fun `hour lists that do not line up are dropped whole`() {
        val broken = observation(saturday, 12, hourMm = listOf(1.0, 0.0, 0.0)) +
            (WeatherSnapshots.PAST_HOURS_MM to "1.0,0.0")
        val outcome = compute(
            listOf(baseline()) + ForecastOutcome.Fetch(at(saturday, 12), emptyMap(), broken)
        )
        // The rain in the misaligned list is not claimed, and there is not enough else.
        assertTrue(outcome.isEmpty())
    }

    @Test
    fun `a day nobody predicted is not a miss`() {
        assertTrue(compute(watched(0..23)).isEmpty())
    }

    @Test
    fun `a day whose forecast carried no probability has nothing to verify`() {
        assertTrue(compute(listOf(baseline(precipPct = null)) + watched(0..23)).isEmpty())
    }

    @Test
    fun `commits written before the two readings existed are silence, not dry hours`() {
        val legacy = (0..23).map {
            ForecastOutcome.Fetch(
                at(saturday, it),
                emptyMap(),
                // The oldest snapshot shape: a status label and a temperature, no code.
                mapOf("current.status" to "Overcast ☁️", "current.temp_c" to "20.0")
            )
        }
        assertTrue(compute(listOf(baseline()) + legacy).isEmpty())
    }

    @Test
    fun `the day still running is not judged`() {
        val fetches = listOf(baseline()) + watched(0..23)
        assertTrue(ForecastOutcome.compute(fetches, zone, today = saturday).isEmpty())
    }

    @Test
    fun `the baseline is the last word before the day, not the first`() {
        val stale = ForecastOutcome.Fetch(
            at(saturday.minusDays(3), 12), forecastOf(90), emptyMap()
        )
        val outcome = compute(listOf(stale, baseline(precipPct = 20)) + watched(0..23)).single()
        assertEquals(20, outcome.forecastPrecipPct)
    }
}
