package com.callbackdev.chiaro.ui.sky

import com.callbackdev.chiaro.domain.model.HourlyForecast
import com.callbackdev.chiaro.domain.model.WeatherCondition
import com.callbackdev.chiaro.domain.sky.SkyJobCatalog
import com.callbackdev.chiaro.domain.sky.SkyNotScheduled
import com.callbackdev.chiaro.domain.sky.SkyOccurrence
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
                condition = WeatherCondition(0), precipChancePct = 0, cloudCoverPct = h.toInt()
            )
        }
        val hours = SkyStateBuilder.nightHours(night, hourly)
        assertEquals(dusk.minusSeconds(1800), hours.first().at)
        assertTrue(hours.all { it.at.isBefore(dawn) })
        assertEquals(9, hours.size)
    }

    // ----------------------------------------------------------- the strip under a finger

    @Test
    fun `a reading takes the cloud and the rain of the hour that holds it`() {
        val tonight = Tonight(
            window = night, verdict = null, night = night,
            hours = listOf(NightHour(at(0), 10, 5), NightHour(at(1), 80, 45))
        )
        val reading = nightReading(tonight, at(1).plusSeconds(600))
        assertEquals(80, reading.cloudPct)
        assertEquals(45, reading.precipPct)
        assertTrue(reading.inWindow)
        assertFalse(reading.moonUp)
    }

    @Test
    fun `an hour the forecast does not cover reads with no cloud rather than a zero`() {
        val tonight = Tonight(window = night, verdict = null, night = night, hours = listOf(NightHour(at(0), 10)))
        val reading = nightReading(tonight, at(5))
        assertNull(reading.cloudPct)
        assertNull(reading.precipPct)
    }

    @Test
    fun `the moon a reading names is the moon the strip washes, and the window's edge is dark`() {
        val tonight = Tonight(window = SkyOccurrence.At(job, at(3), dawn), verdict = null, night = night)
        assertTrue(nightReading(tonight, at(2)).moonUp)
        val edge = nightReading(tonight, at(3))
        assertFalse(edge.moonUp)
        assertTrue(edge.inWindow)
        val allNight = Tonight(window = null, verdict = null, reason = SkyNotScheduled.MOON_ALL_NIGHT, night = night)
        assertTrue(nightReading(allNight, at(4)).moonUp)
        assertFalse(nightReading(allNight, at(4)).inWindow)
    }

    @Test
    fun `a point on the strip settles on the clock's five minutes and never leaves the night`() {
        val oddDusk = Instant.parse("2026-09-23T18:54:00Z")
        val oddDawn = Instant.parse("2026-09-24T03:35:00Z")
        assertEquals(oddDusk, scrubInstant(0f, oddDusk, oddDawn))
        assertEquals(oddDawn, scrubInstant(1f, oddDusk, oddDawn))
        assertEquals(oddDawn, scrubInstant(1.4f, oddDusk, oddDawn))
        val middle = scrubInstant(0.5f, oddDusk, oddDawn)
        assertEquals(0L, middle.epochSecond % 300)
        assertTrue(middle > oddDusk && middle < oddDawn)
    }

    @Test
    fun `the ticks are the whole hours strictly inside the night`() {
        val ticks = hourTicks(Instant.parse("2026-09-23T18:54:00Z"), Instant.parse("2026-09-24T03:00:00Z"))
        assertEquals(Instant.parse("2026-09-23T19:00:00Z"), ticks.first())
        assertEquals(Instant.parse("2026-09-24T02:00:00Z"), ticks.last())
        assertEquals(8, ticks.size)
    }

    @Test
    fun `the sky between two samples is read between them, and beyond them from the nearest`() {
        val samples = listOf(NightSample(at(0), -20.0, 10.0), NightSample(at(1), -30.0, 20.0))
        val half = sampleAt(samples, at(0).plusSeconds(1800))!!
        assertEquals(-25.0, half.sunAltitudeDeg, 1e-9)
        assertEquals(15.0, half.moonAltitudeDeg, 1e-9)
        assertEquals(20.0, sampleAt(samples, at(3))!!.moonAltitudeDeg, 1e-9)
        assertEquals(-20.0, sampleAt(samples, dusk.minusSeconds(60))!!.sunAltitudeDeg, 1e-9)
        assertNull(sampleAt(emptyList(), at(1)))
    }

    @Test
    fun `the night is sampled from dusk to dawn, dawn included, with the moon and the sun in it`() {
        val milan = com.callbackdev.chiaro.domain.model.Coordinates(45.46, 9.19)
        val samples = SkyStateBuilder.nightSamples(night, milan)
        assertEquals(dusk, samples.first().at)
        assertEquals(dawn, samples.last().at)
        assertEquals(8 * 6 + 1, samples.size)
        assertTrue(samples.all { it.sunAltitudeDeg < 0.0 })
    }
}
