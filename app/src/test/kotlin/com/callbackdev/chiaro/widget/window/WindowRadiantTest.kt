package com.callbackdev.chiaro.widget.window

import com.callbackdev.chiaro.domain.model.Coordinates
import com.callbackdev.chiaro.domain.sky.AstronomyEngine
import com.callbackdev.chiaro.domain.sky.MeteorShowerTable
import com.callbackdev.chiaro.domain.sky.SkyJobCatalog
import com.callbackdev.chiaro.domain.sky.SkyOccurrence
import com.callbackdev.chiaro.domain.sky.SkyScheduler
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
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
        val radiant = checkNotNull(WindowPainter.peakRadiant(evening, rome, milan, emptySet()))
        assertEquals("meteor.geminids.peak", radiant.jobId)
        val expected = AstronomyEngine.starHorizontal(geminids.radiantRaDeg, geminids.radiantDecDeg, evening, milan)
        assertEquals(expected.altitudeDeg, radiant.altitudeDeg, 1e-9)
        assertEquals(expected.azimuthDeg, radiant.azimuthDeg, 1e-9)
        assertTrue("Gemini is up in the evening of mid December: ${radiant.altitudeDeg}", radiant.altitudeDeg > 10.0)
        assertTrue("and in the east: ${radiant.azimuthDeg}", radiant.azimuthDeg in 40.0..140.0)
    }

    @Test
    fun `on a night with no peak there is no radiant`() {
        assertNull(WindowPainter.peakRadiant(Instant.parse("2026-09-25T19:00:00Z"), rome, milan, emptySet()))
        assertNull(WindowPainter.peakRadiant(Instant.parse("2026-06-01T22:00:00Z"), rome, milan, emptySet()))
    }

    @Test
    fun `a peak by day is carried by the night nearest to it, from its first hour`() {
        val perseids = requireNotNull(MeteorShowerTable.byId("perseids"))
        val peak = AstronomyEngine.solarLongitudeInstant(2026, perseids.solarLongitudeDeg)
        // The Perseids of 2026 peak with the sun up over Milan: the catalogue gives them
        // the night beside the peak, whole, and that night's sky names the radiant.
        val night = SkyScheduler.resolve(
            SkyJobCatalog.byId("meteor.perseids.peak")!!, LocalDate.of(2026, 6, 1), rome, milan
        ) as SkyOccurrence.At
        assertTrue("the peak is by day", peak.isBefore(night.start) || peak.isAfter(night.end))
        val radiant = WindowPainter.peakRadiant(night.start.plus(Duration.ofHours(1)), rome, milan, emptySet())
        assertNotNull(radiant)
        assertEquals("meteor.perseids.peak", radiant!!.jobId)
        // In the middle of the day before it, no sky of this app is drawn dark and none asks.
        assertNull(WindowPainter.peakRadiant(night.start.minus(Duration.ofHours(10)), rome, milan, emptySet()))
        // A week before, nothing.
        assertNull(WindowPainter.peakRadiant(peak.minus(Duration.ofDays(7)), rome, milan, emptySet()))
    }

    /** The peak's night is one night (10 nov 2026): from its twilight to its dawn, and not
     * the end of the night before. The Geminids of 2026 peak at 05:51 on 14 December. */
    @Test
    fun `the radiant is named from the twilight of the peak's night to its dawn, and not the morning before`() {
        fun at(day: Int, hour: Int, minute: Int = 0) =
            java.time.LocalDateTime.of(2026, 12, day, hour, minute).atZone(rome).toInstant()
        // The morning of the 13th is the end of another night.
        assertNull(WindowPainter.peakRadiant(at(13, 5), rome, milan, emptySet()))
        // Twenty past five in the afternoon: the sun is six degrees down, the stars are an hour away.
        assertEquals("meteor.geminids.peak", WindowPainter.peakRadiant(at(13, 17, 20), rome, milan, emptySet())?.jobId)
        assertEquals("meteor.geminids.peak", WindowPainter.peakRadiant(at(13, 22), rome, milan, emptySet())?.jobId)
        // Two in the morning, four hours before the peak: the radiant is at its highest.
        val high = checkNotNull(WindowPainter.peakRadiant(at(14, 2), rome, milan, emptySet()))
        assertTrue("the radiant at two is at ${high.altitudeDeg}", high.altitudeDeg > 70.0)
        // And the evening after is not the peak's night any more.
        assertNull(WindowPainter.peakRadiant(at(14, 22), rome, milan, emptySet()))
    }

    /** A weak shower's peak night names its radiant only for a reader who follows it. */
    @Test
    fun `a weak shower is named only when followed, a strong one always`() {
        val taurids = requireNotNull(MeteorShowerTable.byId("southern_taurids"))
        assertTrue(!taurids.strong)
        val peak = AstronomyEngine.solarLongitudeInstant(2026, taurids.solarLongitudeDeg)
        val evening = peak.atZone(rome).toLocalDate().atTime(22, 0).atZone(rome).toInstant()
            .let { if (peak.atZone(rome).hour < 12) it.minus(Duration.ofDays(1)) else it }
        assertNull(WindowPainter.peakRadiant(evening, rome, milan, emptySet()))
        val followed = checkNotNull(WindowPainter.peakRadiant(evening, rome, milan, setOf("meteor.southern_taurids.peak")))
        assertEquals("meteor.southern_taurids.peak", followed.jobId)
        assertTrue(requireNotNull(MeteorShowerTable.byId("geminids")).strong)
    }
}
