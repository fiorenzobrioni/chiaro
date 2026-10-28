package com.callbackdev.chiaro.ui.sky

import com.callbackdev.chiaro.domain.model.CloudLayers
import com.callbackdev.chiaro.domain.model.HourlyForecast
import com.callbackdev.chiaro.domain.model.WeatherCondition
import com.callbackdev.chiaro.domain.sky.SkyJobCatalog
import com.callbackdev.chiaro.domain.sky.SkyNotScheduled
import com.callbackdev.chiaro.domain.sky.SkyOccurrence
import com.callbackdev.chiaro.ui.components.cloudDecks
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

    /** The card's picture (22 ott 2026): of now while the night is in progress, of the
     * night's middle otherwise, the hour its moon is read at. */
    @Test
    fun `the picture is of now inside the night and of the night's middle outside it`() {
        assertEquals(at(2), tonightInstant(at(2), night))
        assertEquals(dusk, tonightInstant(dusk, night))
        assertEquals(at(4), tonightInstant(dusk.minusSeconds(6 * 3600), night)) // the afternoon before
        assertEquals(at(4), tonightInstant(dawn, night)) // dawn itself is over
        assertEquals(dusk, tonightInstant(at(2), SkyOccurrence.At(job, dusk, null)))
    }

    /** The picture's cover slides between the hours (22 ott 2026); a printed number stays the hour's. */
    @Test
    fun `the cover is read between the hour and the next, and from the hour alone at the end`() {
        val hours = listOf(NightHour(at(0), 10), NightHour(at(1), 50), NightHour(at(2), 50))
        assertEquals(10f, betweenHours(hours, at(0)) { it.cloudPct.toFloat() })
        assertEquals(30f, betweenHours(hours, at(0).plusSeconds(30 * 60)) { it.cloudPct.toFloat() })
        assertEquals(40f, betweenHours(hours, at(0).plusSeconds(45 * 60)) { it.cloudPct.toFloat() })
        assertEquals(50f, betweenHours(hours, at(2).plusSeconds(30 * 60)) { it.cloudPct.toFloat() }) // no next hour
        assertNull(betweenHours(hours, at(5)) { it.cloudPct.toFloat() })
        assertNull(betweenHours(emptyList(), at(0)) { it.cloudPct.toFloat() })
        // The hour's own number is still what the reading prints.
        assertEquals(10, cloudAt(hours, at(0).plusSeconds(45 * 60)))
    }

    /** The picture's layers (28 ott 2026): the model's three where it split the hour, else
     * the whole cover as deck, which is what the shader makes of no layers at all. */
    @Test
    fun `an hour's layers are the model's, and an unsplit hour is all deck`() {
        val split = NightHour(at(0), 70, layers = CloudLayers(lowPct = 10, midPct = 0, highPct = 65))
        assertEquals(LayerShares(10f, 0f, 65f), layerShares(split))
        assertEquals(LayerShares(70f, 0f, 0f), layerShares(NightHour(at(0), 70)))
        assertEquals(LayerShares(70f, 0f, 0f), layerShares(NightHour(at(0), 70, layers = CloudLayers(0, 0, 0))))
        assertEquals(LayerShares(70f, 0f, 0f), layerShares(NightHour(at(0), 70, layers = CloudLayers(null, null, null))))
        // The shader's reading of an unsplit hour through its shares is the reading with no
        // layers: the deck is the cover, the veil nothing, the light taken the cover.
        val unsplit = layerShares(NightHour(at(0), 70))
        val asLayers = CloudLayers(unsplit.low.toInt(), unsplit.mid.toInt(), unsplit.high.toInt())
        assertEquals(cloudDecks(70, null), cloudDecks(70, asLayers))
        assertEquals(CloudLayers.lightTaken(70, null), CloudLayers.lightTaken(70, asLayers), 1e-6f)
    }

    /** Between a split hour and an unsplit one of the same cover the deck slides, it does not
     * dip: a veil of 70% at 23:00 and an unsplit 70% at midnight are a sky that lowers. */
    @Test
    fun `the layers slide between the hours and the deck never dips on the way`() {
        val hours = listOf(
            NightHour(at(0), 70, layers = CloudLayers(lowPct = 0, midPct = 0, highPct = 70)),
            NightHour(at(1), 70)
        )
        val half = at(0).plusSeconds(30 * 60)
        val low = betweenHours(hours, half) { layerShares(it).low }!!
        val high = betweenHours(hours, half) { layerShares(it).high }!!
        assertEquals(35f, low)
        assertEquals(35f, high)
        val (deck, veil) = cloudDecks(70, CloudLayers(low.toInt(), 0, high.toInt()))
        assertEquals(0.35f, deck, 1e-6f)
        assertEquals(0.35f, veil, 1e-6f)
        // At either end, the hour's own reading.
        assertEquals(0f to 0.7f, cloudDecks(70, CloudLayers(0, 0, 70)))
        assertEquals(0.7f to 0f, cloudDecks(70, null))
    }

    /** A drag to the right brings the sky on the left into view: the bearing falls by the
     * stars' camera's focal length, and wraps at north. */
    @Test
    fun `a drag turns the window by the camera's focal length and wraps at north`() {
        val skyH = 550 // px: 200 dp at 2.75
        val perPx = com.callbackdev.chiaro.widget.window.WindowScene.degreesPerPx(skyH)
        assertTrue("about a tenth of a degree a px: $perPx", perPx > 0.08f && perPx < 0.12f)
        assertEquals(180.0 - 100 * perPx, turnedFacing(180.0, turnDegrees(100f, skyH)), 1e-6)
        assertEquals(180.0 + 100 * perPx, turnedFacing(180.0, turnDegrees(-100f, skyH)), 1e-6)
        val wrapped = turnedFacing(5.0, turnDegrees(100f, skyH))
        assertTrue("wraps under north: $wrapped", wrapped > 350.0 && wrapped < 360.0)
        // The turn itself is not wrapped: a circle and a half is what the clouds move by.
        assertEquals(540.0, turnDegrees(-(540f / perPx), skyH), 1e-3)
        assertEquals(180.0, turnedFacing(0.0, 540.0), 1e-9)
    }

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
                condition = WeatherCondition(0), precipChancePct = 0, cloudCoverPct = h.toInt(),
                cloudLayers = CloudLayers(lowPct = h.toInt(), midPct = 0, highPct = 0)
            )
        }
        val hours = SkyStateBuilder.nightHours(night, hourly)
        assertEquals(dusk.minusSeconds(1800), hours.first().at)
        assertTrue(hours.all { it.at.isBefore(dawn) })
        assertEquals(9, hours.size)
        // The hour's layers travel with it (28 ott 2026): the picture reads them.
        assertTrue(hours.all { it.layers == CloudLayers(it.cloudPct, 0, 0) })
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

    @Test
    fun `a whole night's cloud is the mean of its hours, and no hours is no number`() {
        assertEquals(45, meanCloud(listOf(NightHour(at(0), 10), NightHour(at(1), 80))))
        assertEquals(33, meanCloud(listOf(NightHour(at(0), 0), NightHour(at(1), 50), NightHour(at(2), 50))))
        assertNull(meanCloud(emptyList()))
    }
}
