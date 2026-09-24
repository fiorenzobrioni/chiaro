package com.callbackdev.chiaro.domain

import com.callbackdev.chiaro.domain.model.CloudLayers
import com.callbackdev.chiaro.domain.model.HourlyForecast
import com.callbackdev.chiaro.domain.model.WeatherCondition
import com.callbackdev.chiaro.domain.sample.sampleWeatherReport
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** Suggestion 3 of the engine review (24 set 2026): the forecast for now, when now is stale. */
class CurrentEstimateTest {

    private val zone = ZoneId.of("Europe/Rome")
    private val base = sampleWeatherReport()
    private val fetched: Instant = LocalDateTime.of(2026, 9, 24, 8, 10).atZone(zone).toInstant()

    private fun row(hour: Int, temp: Double, code: Int = 0, gust: Double? = 30.0, dir: Int = 350 + hour) = HourlyForecast(
        time = LocalDateTime.of(2026, 9, 24, hour, 0),
        at = LocalDateTime.of(2026, 9, 24, hour, 0).atZone(zone).toInstant(),
        tempC = temp,
        condition = WeatherCondition(code),
        precipChancePct = hour * 5,
        cloudCoverPct = hour * 5,
        feelsLikeC = temp - 1,
        humidityPct = 60 + hour,
        dewPointC = 10.0,
        pressureMb = 1010.0 + hour,
        windKph = 10.0 + hour,
        windDirectionDeg = dir % 360,
        gustKph = gust,
        uvIndex = hour / 2.0,
        visibilityKm = 20.0,
        cloudLayers = CloudLayers(hour, 0, 80)
    )

    private val report = base.copy(
        systemInfo = base.systemInfo.copy(lastSync = fetched),
        hourly = listOf(row(12, 20.0), row(13, 22.0, code = 63), row(14, 24.0))
    )

    private fun at(hour: Int, minute: Int): Instant =
        LocalDateTime.of(2026, 9, 24, hour, minute).atZone(zone).toInstant()

    @Test
    fun `under an hour old, the fetched block stands`() {
        val fresh = report.copy(systemInfo = report.systemInfo.copy(lastSync = at(13, 0)))
        assertSame(fresh, CurrentEstimate.apply(fresh, at(13, 45)))
        assertFalse(CurrentEstimate.apply(fresh, at(13, 45)).current.estimated)
    }

    @Test
    fun `past an hour, now is the forecast for now, and says so`() {
        val now = CurrentEstimate.apply(report, at(13, 30)).current
        assertTrue(now.estimated)
        // Instants halfway between 13:00 and 14:00.
        assertEquals(23.0, now.tempC, 1e-9)
        assertEquals(22.0, now.feelsLikeC, 1e-9)
        assertEquals(74, now.humidityPct) // 73.5 rounds half up
        assertEquals(1023.5, now.pressureMb, 1e-9)
        assertEquals(23.5, now.wind.speedKph, 1e-9)
        assertEquals(68, now.cloudCoverPct) // 65 and 70: 67.5, rounded half up
        // The hour under way is 13-14: its sky, its gust, its chance.
        assertEquals(63, now.condition.wmoCode)
        assertEquals(65, now.precipitation.chancePct)
        assertEquals(30.0, now.wind.gustKph, 1e-9)
        // The direction and the layers from the nearer row (14:00 at half past: f = 0.5).
        assertEquals(4, now.wind.degree)
        assertEquals(CloudLayers(14, 0, 80), now.cloudLayers)
    }

    @Test
    fun `a direction is never averaged across north`() {
        // 350° at 13:00, 10° at 14:00: the mean of the two numbers is 180°, a south wind
        // nobody forecast. The nearer row's direction is taken instead.
        val across = report.copy(hourly = listOf(row(13, 22.0, dir = 350), row(14, 24.0, dir = 10)))
        assertEquals(350, CurrentEstimate.apply(across, at(13, 10)).current.wind.degree)
        assertEquals(10, CurrentEstimate.apply(across, at(13, 40)).current.wind.degree)
    }

    @Test
    fun `a gust nobody forecast is not invented`() {
        val noGust = report.copy(hourly = listOf(row(13, 22.0, gust = null), row(14, 24.0, gust = null)))
        val now = CurrentEstimate.apply(noGust, at(13, 30)).current
        assertEquals(now.wind.speedKph, now.wind.gustKph, 1e-9)
    }

    @Test
    fun `rows without the rest of an hour are not estimated at all`() {
        val bare = report.copy(
            hourly = report.hourly.map { it.copy(feelsLikeC = null) }
        )
        val out = CurrentEstimate.apply(bare, at(13, 30))
        assertSame(bare, out)
        assertNull(CurrentEstimate.estimate(bare, at(13, 30)))
    }

    @Test
    fun `no row for now, no estimate`() {
        // Past the last row by more than its hour, and before the first.
        assertNull(CurrentEstimate.estimate(report, at(16, 0)))
        assertNull(CurrentEstimate.estimate(report, at(11, 0)))
    }

    @Test
    fun `the air and the pollen stay what was measured`() {
        val out = CurrentEstimate.apply(report, at(13, 30))
        assertEquals(report.airQuality, out.airQuality)
        assertEquals(report.pollen, out.pollen)
        assertEquals(report.systemInfo.lastSync, out.systemInfo.lastSync)
        assertTrue(Duration.between(out.systemInfo.lastSync, at(13, 30)) >= CurrentEstimate.MAX_AGE)
    }
}
