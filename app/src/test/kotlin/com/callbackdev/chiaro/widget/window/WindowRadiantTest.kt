package com.callbackdev.chiaro.widget.window

import com.callbackdev.chiaro.domain.model.Coordinates
import com.callbackdev.chiaro.domain.sky.AstronomyEngine
import com.callbackdev.chiaro.domain.sky.MeteorShowerTable
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The radiant the window turns to on a peak night, from the catalogue's own resolution. */
class WindowRadiantTest {

    private val milan = Coordinates(45.4642, 9.19)
    private val rome: ZoneId = ZoneId.of("Europe/Rome")

    @Test
    fun `on the Geminids' peak night the radiant is theirs, up, in the east after dusk`() {
        val geminids = requireNotNull(MeteorShowerTable.byId("geminids"))
        val peak = AstronomyEngine.solarLongitudeInstant(2026, geminids.solarLongitudeDeg)
        // The evening of the peak's night, three hours after sunset at the latest.
        val evening = peak.atZone(rome).toLocalDate().atTime(21, 0).atZone(rome).toInstant()
            .let { if (peak.atZone(rome).hour < 12) it.minus(Duration.ofDays(1)) else it }
        val radiant = checkNotNull(WindowPainter.peakRadiant(evening, rome, milan))
        assertEquals("meteor.geminids.peak", radiant.jobId)
        val expected = AstronomyEngine.starHorizontal(geminids.radiantRaDeg, geminids.radiantDecDeg, evening, milan)
        assertEquals(expected.altitudeDeg, radiant.altitudeDeg, 1e-9)
        assertEquals(expected.azimuthDeg, radiant.azimuthDeg, 1e-9)
        assertTrue("Gemini is up in the evening of mid December: ${radiant.altitudeDeg}", radiant.altitudeDeg > 10.0)
        assertTrue("and in the east: ${radiant.azimuthDeg}", radiant.azimuthDeg in 40.0..140.0)
    }

    @Test
    fun `on a night with no peak there is no radiant`() {
        assertNull(WindowPainter.peakRadiant(Instant.parse("2026-09-25T19:00:00Z"), rome, milan))
        assertNull(WindowPainter.peakRadiant(Instant.parse("2026-06-01T22:00:00Z"), rome, milan))
    }

    @Test
    fun `the peak's night is known from the evening before a peak in the small hours`() {
        val perseids = requireNotNull(MeteorShowerTable.byId("perseids"))
        val peak = AstronomyEngine.solarLongitudeInstant(2026, perseids.solarLongitudeDeg)
        val dusk = peak.minus(Duration.ofHours(6))
        val radiant = WindowPainter.peakRadiant(dusk, rome, milan)
        // Six hours before the peak the window is the same night's, or the peak is by day
        // and the night either side of it carries it: either way a radiant, the Perseids'.
        assertNotNull(radiant)
        assertEquals("meteor.perseids.peak", radiant!!.jobId)
        // A week before, nothing.
        assertNull(WindowPainter.peakRadiant(peak.minus(Duration.ofDays(7)), rome, milan))
    }
}
