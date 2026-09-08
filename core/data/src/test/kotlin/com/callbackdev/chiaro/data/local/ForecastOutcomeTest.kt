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

    private fun observation(wmoCode: Int, mm: Double, tempC: Double = 20.0) = mapOf(
        "current.wmo_code" to wmoCode.toString(),
        "current.precip_last_hour_mm" to mm.toString(),
        "current.temp_c" to tempC.toString()
    )

    private fun baseline(precipPct: Int? = 70, highC: Double = 27.0) =
        ForecastOutcome.Fetch(at(saturday.minusDays(1), 22), forecastOf(precipPct, highC), emptyMap())

    /** [hours] hourly commits through Saturday, each covering the hour behind it. */
    private fun watched(
        hours: Iterable<Int>,
        wmoCode: Int = 3,
        mm: Double = 0.0,
        tempC: (Int) -> Double = { 20.0 }
    ) = hours.map {
        ForecastOutcome.Fetch(at(saturday, it), emptyMap(), observation(wmoCode, mm, tempC(it)))
    }

    private fun compute(fetches: List<ForecastOutcome.Fetch>) =
        ForecastOutcome.compute(fetches, zone, today = sunday)

    @Test
    fun `a dry day the app watched all through says so, with its coverage`() {
        val outcome = compute(listOf(baseline()) + watched(0..23)).single()

        assertEquals(saturday, outcome.date)
        assertEquals(70, outcome.forecastPrecipPct)
        assertEquals(false, outcome.rained)
        // 00:00 through 23:00: the midnight reading's hour belongs to the day before,
        // so a day watched every hour still covers twenty-three of its own.
        assertEquals(23, outcome.coveredHours)
    }

    @Test
    fun `one wet hour is proof, whatever the coverage`() {
        // Three commits only — nowhere near the floor — but one of them saw rain.
        val fetches = listOf(baseline()) +
            watched(listOf(9, 10)) +
            ForecastOutcome.Fetch(at(saturday, 16), emptyMap(), observation(63, 2.4))
        val outcome = compute(fetches).single()

        assertTrue(outcome.rained)
        assertEquals(3, outcome.coveredHours)
    }

    @Test
    fun `millimetres alone are enough, even under a dry code`() {
        // The shower ended before the reading: the code says overcast, the hour behind
        // it carries 0.6 mm. The hour is the honest evidence.
        val fetches = listOf(baseline()) +
            watched(0..12) +
            ForecastOutcome.Fetch(at(saturday, 13), emptyMap(), observation(3, 0.6)) +
            watched(14..23)
        assertTrue(compute(fetches).single().rained)
    }

    @Test
    fun `a day the app barely watched gets no verdict at all`() {
        // Six dry hours: not rain, and not enough of the day to claim it stayed dry.
        assertTrue(compute(listOf(baseline()) + watched(8..13)).isEmpty())
    }

    @Test
    fun `a refresh spree does not buy coverage`() {
        // Twenty commits inside one hour: their windows overlap into two hours of
        // covered day, not twenty.
        val spree = (0..19).map {
            ForecastOutcome.Fetch(at(saturday, 15, it * 3), emptyMap(), observation(3, 0.0))
        }
        assertTrue(compute(listOf(baseline()) + spree).isEmpty())
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
                ForecastOutcome.Fetch(at(saturday, 16), emptyMap(), observation(63, 1.0, 24.0))
        ).single()
        assertTrue(glimpsed.rained)
        assertNull(glimpsed.observedHighC)
        assertNull(glimpsed.forecastHighC)
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
                // The old snapshot shape: a status label and a temperature, no code.
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
