package com.callbackdev.chiaro.ui.sky

import com.callbackdev.chiaro.domain.model.HourlyForecast
import com.callbackdev.chiaro.domain.model.WeatherCondition
import com.callbackdev.chiaro.domain.sky.SkyJobCatalog
import com.callbackdev.chiaro.domain.sky.SkyNotScheduled
import com.callbackdev.chiaro.domain.sky.SkyOccurrence
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The tonight card's night strip (design review, 23 set 2026) draws only what the card
 * already says: where the moon is up is read off the window against the night, and the
 * clouds are the forecast's own hours. These are the arithmetic halves of that picture.
 */
class TonightNightTest {

    private val job = SkyJobCatalog.DarknessWindow
    private val dusk = Instant.parse("2026-09-23T19:00:00Z")
    private val dawn = Instant.parse("2026-09-24T03:00:00Z")
    private val night = SkyOccurrence.At(job, dusk, dawn)
    private fun at(h: Long) = dusk.plusSeconds(h * 3600)

    @Test
    fun `a window that opens late was held by the moon from dusk`() {
        val tonight = Tonight(window = SkyOccurrence.At(job, at(3), dawn), verdict = null, night = night)
        assertEquals(listOf(dusk..at(3)), moonUpRanges(tonight))
    }

    @Test
    fun `a window that closes early lost its end to a rising moon`() {
        val tonight = Tonight(window = SkyOccurrence.At(job, dusk, at(5)), verdict = null, night = night)
        assertEquals(listOf(at(5)..dawn), moonUpRanges(tonight))
    }

    @Test
    fun `a moon up all night silvers all of it, and a moonless night none of it`() {
        val allNight = Tonight(window = null, verdict = null, reason = SkyNotScheduled.MOON_ALL_NIGHT, night = night)
        assertEquals(listOf(dusk..dawn), moonUpRanges(allNight))
        val dark = Tonight(window = night, verdict = null, night = night)
        assertTrue(moonUpRanges(dark).isEmpty())
    }

    @Test
    fun `a moment's place on the strip is its share of the night, never off the ends`() {
        assertEquals(0f, nightFraction(dusk, dusk, dawn), 0f)
        assertEquals(0.5f, nightFraction(at(4), dusk, dawn), 1e-5f)
        assertEquals(1f, nightFraction(at(12), dusk, dawn), 0f)
    }

    @Test
    fun `the cloud of an instant is the cloud of the hour that holds it`() {
        val hours = listOf(NightHour(at(0), 10), NightHour(at(1), 80))
        assertEquals(10, cloudAt(hours, at(0).plusSeconds(1800)))
        assertEquals(80, cloudAt(hours, at(1)))
        assertNull(cloudAt(hours, at(2).plusSeconds(1)))
    }

    @Test
    fun `the night's hours include the one that straddles dusk and stop at dawn`() {
        val zone = ZoneId.of("UTC")
        val hourly = (-2L..10L).map { h ->
            val t = dusk.plusSeconds(h * 3600 - 1800)
            HourlyForecast(
                time = t.atZone(zone).toLocalDateTime(), at = t, tempC = 10.0,
                condition = WeatherCondition(0, "", ""), precipChancePct = 0, cloudCoverPct = h.toInt()
            )
        }
        val hours = SkyStateBuilder.nightHours(night, hourly)
        assertEquals(dusk.minusSeconds(1800), hours.first().at)
        assertTrue(hours.all { it.at.isBefore(dawn) })
        assertEquals(9, hours.size)
    }
}
