package com.callbackdev.chiaro.domain

import com.callbackdev.chiaro.domain.model.DailyForecast
import com.callbackdev.chiaro.domain.model.HourlyForecast
import com.callbackdev.chiaro.domain.model.WeatherCondition
import com.callbackdev.chiaro.domain.rules.RuleVariables
import com.callbackdev.chiaro.domain.sample.sampleWeatherReport
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The report as a screen reads it for now (15.1, 28 Oct 2026): what Today prepared and the
 * Alerts cards did not, on the case that showed it — a cache fetched the day before.
 */
class WeatherNowTest {

    private val zone = ZoneId.of("Europe/Rome")
    private val clear = WeatherCondition(0)
    private val yesterday = LocalDate.of(2026, 10, 27)
    private val today = yesterday.plusDays(1)

    /** Fetched yesterday at 10:00; the rows carry what the estimate needs. */
    private val fetchedAt: Instant = yesterday.atTime(10, 0).atZone(zone).toInstant()

    private fun row(start: LocalDateTime, temp: Double) = HourlyForecast(
        time = start,
        at = start.atZone(zone).toInstant(),
        tempC = temp,
        condition = clear,
        precipChancePct = 10,
        cloudCoverPct = 20,
        feelsLikeC = temp - 1,
        humidityPct = 60,
        dewPointC = 8.0,
        pressureMb = 1015.0,
        windKph = 10.0,
        windDirectionDeg = 90,
        gustKph = 20.0
    )

    private val report = sampleWeatherReport().let { sample ->
        sample.copy(
            location = sample.location.copy(timezone = "Europe/Rome", localTime = yesterday.atTime(10, 0)),
            // 25° at the fetch; the rows for today's late morning say 12°.
            current = sample.current.copy(tempC = 25.0),
            hourly = (0 until 48).map { h ->
                val start = yesterday.atTime(10, 0).plusHours(h.toLong())
                row(start, if (start.toLocalDate() == today) 12.0 else 25.0)
            },
            daily = listOf(
                DailyForecast(yesterday, 30.0, 18.0, clear, 0, 5),
                DailyForecast(today, 20.0, 9.0, clear, 10, 3),
                DailyForecast(today.plusDays(1), 18.0, 8.0, clear, 20, 3)
            ),
            systemInfo = sample.systemInfo.copy(lastSync = fetchedAt)
        )
    }

    private val now: Instant = today.atTime(10, 30).atZone(zone).toInstant()

    @Test
    fun `a cache from yesterday leads with today, and its now is the estimate`() {
        val read = WeatherNow.prepare(report, now)!!
        assertEquals(today, read.daily.first().date)
        assertEquals(today.atTime(10, 0), read.hourly.first().time)
        assertTrue(read.current.estimated)
        assertEquals(12.0, read.current.tempC, 1e-9)
        // What a rule reads off it: today's high, not the row the cache leads with.
        assertEquals(20.0, RuleVariables.byId("today.high_c")!!.resolve(read, LocalDateTime.of(today, java.time.LocalTime.of(10, 30)))!!.value, 1e-9)
        assertEquals(12.0, RuleVariables.byId("current.temp_c")!!.resolve(read, LocalDateTime.of(today, java.time.LocalTime.of(10, 30)))!!.value, 1e-9)
    }

    @Test
    fun `a report fetched this hour is returned as it is`() {
        val fresh = report.copy(systemInfo = report.systemInfo.copy(lastSync = fetchedAt))
        assertSame(fresh, WeatherNow.prepare(fresh, fetchedAt.plusSeconds(300)))
    }

    @Test
    fun `a report past its horizon has no now to read`() {
        assertNull(WeatherNow.prepare(report, today.plusDays(3).atTime(10, 0).atZone(zone).toInstant()))
    }
}
