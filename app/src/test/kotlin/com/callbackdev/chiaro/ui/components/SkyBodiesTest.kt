package com.callbackdev.chiaro.ui.components

import androidx.compose.ui.geometry.Offset
import com.callbackdev.chiaro.domain.model.CloudLayers
import com.callbackdev.chiaro.domain.model.Coordinates
import com.callbackdev.chiaro.domain.sky.AstronomyEngine
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * The sun and the moon on the canvas (23 set 2026): where they are drawn is arithmetic,
 * and the arithmetic is what makes them the sky rather than a decoration. The shader draws
 * them since 19 ott 2026 (`SkyShaderPixelTest`); these are the rules it reads.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SkyBodiesTest {

    @Test
    fun `in the north the south is the middle, east the left and west the right`() {
        assertEquals(0.5f, bodyAcross(180.0, southern = false), 1e-5f)
        assertEquals(0.2f, bodyAcross(90.0, southern = false), 1e-5f)
        assertEquals(0.8f, bodyAcross(270.0, southern = false), 1e-5f)
    }

    @Test
    fun `in the south the reader faces north and east is on the right`() {
        assertEquals(0.5f, bodyAcross(0.0, southern = true), 1e-5f)
        assertEquals(0.8f, bodyAcross(90.0, southern = true), 1e-5f)
        assertEquals(0.2f, bodyAcross(270.0, southern = true), 1e-5f)
    }

    @Test
    fun `a bearing behind the reader stays on the screen`() {
        assertEquals(0f, bodyAcross(10.0, southern = false), 0f)
        assertEquals(1f, bodyAcross(350.0, southern = false), 0f)
    }

    @Test
    fun `altitude climbs to the top at sixty degrees and no further`() {
        assertEquals(0f, bodyUp(0.0), 0f)
        assertEquals(0.5f, bodyUp(30.0), 1e-5f)
        assertEquals(1f, bodyUp(75.0), 0f)
        assertEquals(0f, bodyUp(-10.0), 0f)
    }

    @Test
    fun `a full moon is the whole disc and a crescent stays on its lit side`() {
        val center = Offset(100f, 100f)
        val full = moonLitPath(center, 10f, 1f, litRight = true).getBounds()
        assertEquals(90f, full.left, 0.5f)
        assertEquals(110f, full.right, 0.5f)
        val crescent = moonLitPath(center, 10f, 0.2f, litRight = true).getBounds()
        assertTrue("a right-lit crescent starts right of the middle", crescent.left >= 100f - 0.5f)
        val left = moonLitPath(center, 10f, 0.2f, litRight = false).getBounds()
        assertTrue("a left-lit crescent ends left of the middle", left.right <= 100f + 0.5f)
    }

    /** 24 set 2026: an overcast sun is a soft patch of light, not a pale disc (it read as the moon). */
    @Test
    fun `under a full cover the sun loses its edge, not its light`() {
        val clear = sunVeil(0f)
        assertEquals(SunVeil(disc = 1f, glowCore = 0.55f, glowMid = 0.18f, spread = 1f), clear)
        val overcast = sunVeil(1f)
        assertEquals(0f, overcast.disc, 1e-6f)
        // The glow stays, dimmer, and spreads: a brighter part of the sky, never a disc.
        assertTrue(overcast.glowCore >= 0.25f)
        assertTrue(overcast.glowMid > clear.glowMid)
        assertTrue(overcast.spread > 1.2f)
        // In between the disc fades steadily and never jumps.
        val half = sunVeil(0.5f)
        assertTrue(half.disc in 0.4f..0.6f)
        assertTrue(sunVeil(0.8f).disc < half.disc)
        // Out of range clamps.
        assertEquals(overcast, sunVeil(1.7f))
        assertEquals(clear, sunVeil(-0.2f))
    }

    /**
     * 16 ott 2026: what veils the sun is how much light the cloud takes, not how much sky it
     * covers. A sky closed by a high veil keeps the sun's disc in its halo; one closed by a low
     * deck keeps only a dim, wide brightening.
     */
    @Test
    fun `a high veil keeps the sun's disc, a low deck takes it and dims the glow`() {
        val veiled = sunVeil(cloudOpacity(100, CloudLayers(0, 0, 100), SkyFall.NONE))
        val decked = sunVeil(cloudOpacity(100, CloudLayers(100, 0, 0), SkyFall.NONE))
        assertTrue("a cirrostratus sun has its outline", veiled.disc > 0.6f)
        assertEquals(0f, decked.disc, 1e-6f)
        assertTrue(decked.glowCore < veiled.glowCore)
        assertTrue(decked.spread > veiled.spread)
    }

    /** 16 ott 2026: a day moon behind a stratus is not there; at night its place still glows.
     * 24 nov 2026: a day moon has no glow at all, and the disc pales with the daylight. */
    @Test
    fun `the moon goes behind a low deck, and keeps a glow there only at night`() {
        val clearDay = moonVeil(0f, day = 1f)
        assertEquals(0.55f, clearDay.disc, 1e-6f)
        assertEquals("a day moon has no glow", 0f, clearDay.glow, 0f)
        assertEquals(1f, clearDay.spread, 0f)
        assertEquals(MoonVeil(disc = 0.95f, glow = 0.95f, spread = 1f), moonVeil(0f, day = 0f))
        val deckDay = moonVeil(1f, day = 1f)
        assertEquals(0f, deckDay.disc, 1e-6f)
        assertEquals(0f, deckDay.glow, 1e-6f)
        val deckNight = moonVeil(1f, day = 0f)
        assertEquals(0f, deckNight.disc, 1e-6f)
        assertTrue(deckNight.glow > 0.3f)
        assertTrue(deckNight.spread > 1.2f)
        // Behind a high veil the day moon is paler, not gone.
        val veilDay = moonVeil(cloudOpacity(100, CloudLayers(0, 0, 100), SkyFall.NONE), day = 1f)
        assertTrue(veilDay.disc in 0.3f..0.45f)
        // Steady in between, and clamped outside.
        assertTrue(moonVeil(0.8f, day = 1f).disc < moonVeil(0.5f, day = 1f).disc)
        assertEquals(deckDay, moonVeil(1.4f, day = 1f))
        assertEquals(deckDay, moonVeil(1f, day = 1.6f))
    }

    /** 24 nov 2026: the moon pales as the sky brightens, not in the minute of sunrise. Through
     * the twilight every step of the daylight is a small step of the disc. */
    @Test
    fun `the moon pales with the daylight, without a step at the horizon`() {
        var last = moonVeil(0f, day = 0f)
        for (i in 1..20) {
            val next = moonVeil(0f, day = i / 20f)
            assertTrue("paler at ${i / 20f}", next.disc < last.disc)
            assertTrue("a step of ${last.disc - next.disc}", last.disc - next.disc < 0.03f)
            assertTrue(next.glow <= last.glow)
            last = next
        }
        assertEquals(0.75f, moonVeil(0f, day = 0.5f).disc, 1e-6f)
    }

    /** 24 nov 2026: the dark part of the moon shows on a thin crescent in the dark, the Sky's
     * own «earthshine» (a crescent of 1% to 18%, at dusk or before dawn), and nowhere else. */
    @Test
    fun `the earth's light is a thin crescent's, and the night's`() {
        assertEquals(1f, earthshineShare(0.03f, -30.0), 0f)
        assertEquals(1f, earthshineShare(0.08f, -8.0), 0f)
        assertTrue("still there at the event's 18%", earthshineShare(0.18f, -30.0) in 0.5f..0.85f)
        assertEquals("a half moon's dark part is the sky", 0f, earthshineShare(0.5f, -30.0), 0f)
        assertEquals(0f, earthshineShare(0.35f, -30.0), 0f)
        assertEquals(0f, earthshineShare(0.9f, -30.0), 0f)
        // By day there is none, whatever the phase: a new moon beside the sun is not drawn.
        assertEquals(0f, earthshineShare(0.03f, 0.0), 0f)
        assertEquals(0f, earthshineShare(0.01f, 35.0), 0f)
        assertEquals(0.5f, earthshineShare(0.03f, -3.0), 1e-5f)
        // Never growing with the phase.
        var last = 1f
        for (i in 0..40) {
            val share = earthshineShare(i / 40f, -30.0)
            assertTrue(share <= last)
            last = share
        }
    }

    /** One air for the two bodies: what reddens the low sun warms the low moon. */
    @Test
    fun `the horizon's colour is worn in the last twelve degrees`() {
        assertEquals(1f, horizonWarmth(0.0), 0f)
        assertEquals(1f, horizonWarmth(-0.4), 0f)
        assertEquals(0.5f, horizonWarmth(6.0), 1e-6f)
        assertEquals(0f, horizonWarmth(12.0), 0f)
        assertEquals(0f, horizonWarmth(55.0), 0f)
    }

    private val huaraz = Coordinates(-9.53, -77.53)
    private val singapore = Coordinates(1.35, 103.82)
    private val mumbai = Coordinates(19.08, 72.88)
    private val milan = Coordinates(45.4642, 9.19)
    private val sydney = Coordinates(-33.87, 151.21)

    private fun noon(date: String, zone: String): Instant =
        LocalDate.parse(date).atTime(12, 0).atZone(ZoneId.of(zone)).toInstant()

    private fun facing(coords: Coordinates, date: String, zone: String): Boolean =
        facesNorth(coords.lat, AstronomyEngine.sunDeclination(noon(date, zone)))

    /** 24 nov 2026: the reader faces where the sun culminates. Outside the tropics that is
     * the hemisphere, all year; inside them it turns with the season. */
    @Test
    fun `the reader faces the noon sun, which in the tropics changes side with the season`() {
        listOf("2026-06-21", "2026-12-21", "2026-03-20").forEach { date ->
            assertFalse("Milan faces south on $date", facing(milan, date, "Europe/Rome"))
            assertTrue("Sydney faces north on $date", facing(sydney, date, "Australia/Sydney"))
        }
        // Huaraz is south of the equator and in November the sun is south of Huaraz.
        assertFalse(facing(huaraz, "2026-11-24", "America/Lima"))
        assertTrue(facing(huaraz, "2026-06-21", "America/Lima"))
        // Singapore and Mumbai are north of it, and in June the sun is north of both.
        assertTrue(facing(singapore, "2026-06-21", "Asia/Singapore"))
        assertFalse(facing(singapore, "2026-12-21", "Asia/Singapore"))
        assertTrue(facing(mumbai, "2026-06-10", "Asia/Kolkata"))
    }

    /** The sun's place across the canvas, minute by minute while it is up: its two ends, its
     * place at the day's noon and the largest step between two minutes. */
    private class Track(val first: Float, val last: Float, val atNoon: Float, val largestStep: Float)

    private fun track(coords: Coordinates, date: String, zone: String, facingNorth: Boolean): Track {
        val start = LocalDate.parse(date).atStartOfDay(ZoneId.of(zone)).toInstant()
        val places = (0 until 24 * 60).mapNotNull { minute ->
            val at = start.plusSeconds(minute * 60L)
            if (AstronomyEngine.sunAltitude(at, coords) <= 0.0) null else bodyAcross(AstronomyEngine.sunAzimuth(at, coords), facingNorth)
        }
        val solarNoon = AstronomyEngine.solarDay(LocalDate.parse(date), ZoneId.of(zone), coords).solarNoon
        return Track(
            first = places.first(),
            last = places.last(),
            atNoon = bodyAcross(AstronomyEngine.sunAzimuth(solarNoon, coords), facingNorth),
            largestStep = places.zipWithNext { a, b -> abs(b - a) }.max()
        )
    }

    /**
     * 24 nov 2026, measured for Huaraz that day: facing the equator the sun stood at 1.00 of
     * the canvas at 11:40 and at 0.00 at 12:00. Facing the noon sun it crosses the canvas once,
     * from one side through the middle to the other, with no minute that moves it by more than
     * a twentieth of the width; and facing the equator it did leap, which is what this guards.
     */
    @Test
    fun `in the tropics the sun crosses the canvas once, through the middle at noon`() {
        listOf(
            Triple(huaraz, "2026-11-24", "America/Lima"),
            Triple(singapore, "2026-06-21", "Asia/Singapore"),
            Triple(mumbai, "2026-06-10", "Asia/Kolkata")
        ).forEach { (coords, date, zone) ->
            val sun = track(coords, date, zone, facing(coords, date, zone))
            assertTrue("$date $zone: the largest step is ${sun.largestStep}", sun.largestStep < 0.05f)
            assertEquals("$date $zone: noon in the middle", 0.5f, sun.atNoon, 0.02f)
            assertTrue("$date $zone: from one side to the other", abs(sun.first - sun.last) > 0.3f)
            assertTrue("$date $zone: never on an edge", minOf(sun.first, sun.last) > 0.05f && maxOf(sun.first, sun.last) < 0.95f)
            val equator = track(coords, date, zone, coords.lat < 0.0)
            assertTrue("$date $zone: facing the equator it leapt by ${equator.largestStep}", equator.largestStep > 0.8f)
        }
        // And where the sun never passes overhead nothing moved: Milan's summer and winter.
        listOf("2026-06-21", "2026-11-24").forEach { date ->
            val sun = track(milan, date, "Europe/Rome", facing(milan, date, "Europe/Rome"))
            assertEquals(0.5f, sun.atNoon, 0.01f)
            assertTrue(sun.first < 0.35f && sun.last > 0.65f && sun.largestStep < 0.01f)
        }
    }
}
