package com.callbackdev.chiaro.ui.components

import com.callbackdev.chiaro.domain.model.Coordinates
import com.callbackdev.chiaro.domain.sky.AstronomyEngine
import com.callbackdev.chiaro.domain.sky.BrightStars
import com.callbackdev.chiaro.domain.sky.Planet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import kotlin.math.abs

/**
 * The stars of the canvas (step 6 of the sky review, 19 ott 2026): the catalogue's stars in
 * the canvas' window, placed by the same arithmetic the engine places the moon with, and drawn
 * by their magnitude and colour.
 */
class StarFieldTest {

    private val milan = Coordinates(45.4642, 9.19)
    private val sydney = Coordinates(-33.8688, 151.2093)

    @Test
    fun `the window holds the stars in front of a reader facing the equator, within the margin`() {
        val at = Instant.parse("2026-01-15T21:00:00Z")
        val stars = StarField.visible(at, milan, southern = false)
        assertTrue("some stars on a January evening: ${stars.size}", stars.size in 200..900)
        stars.forEach {
            assertTrue(abs(it.acrossDeg) <= StarField.WINDOW_ACROSS_DEG)
            assertTrue(it.altitudeDeg in StarField.WINDOW_ALT_LOW_DEG..StarField.WINDOW_ALT_HIGH_DEG)
        }
        // Every mark is where the engine puts its star, measured from due south.
        stars.forEach { mark ->
            val found = (0 until BrightStars.count).any { i ->
                val h = AstronomyEngine.starHorizontal(BrightStars.raDeg[i].toDouble(), BrightStars.decDeg[i].toDouble(), at, milan)
                abs(h.altitudeDeg - mark.altitudeDeg) < 0.01 && abs(((h.azimuthDeg - 180.0 + 540.0) % 360.0) - 180.0 - mark.acrossDeg) < 0.01
            }
            assertTrue("a mark with no star under it", found)
        }
        // Polaris, due north, is behind the reader and never in it.
        assertTrue(stars.none { it.altitudeDeg > 44f && it.altitudeDeg < 47f && abs(it.acrossDeg) > 35f && it.brightness > 0.3f })
    }

    @Test
    fun `south of the equator the window faces north`() {
        val at = Instant.parse("2026-01-15T12:00:00Z")
        val north = StarField.visible(at, sydney, southern = true)
        val south = StarField.visible(at, sydney, southern = false)
        assertTrue(north.isNotEmpty())
        // The same stars measured from the two facings are 180° apart: none in both windows.
        val northKeys = north.map { it.altitudeDeg to it.brightness }.toSet()
        assertTrue(south.none { (it.altitudeDeg to it.brightness) in northKeys })
    }

    @Test
    fun `brightness follows the magnitude, warmth the colour`() {
        assertEquals(1f, StarField.brightness(-1.46f), 0.02f)
        assertTrue(StarField.brightness(0f) > StarField.brightness(2f))
        assertTrue(StarField.brightness(2f) > StarField.brightness(4.5f))
        assertTrue(StarField.brightness(4.5f) > StarField.brightness(6f))
        // The dust: a star at the limit is a tenth of Sirius, one at 4.5 a fifth.
        assertEquals(0.09f, StarField.brightness(6f), 0.01f)
        assertEquals(0.2f, StarField.brightness(4.5f), 0.02f)
        assertEquals(0.08f, StarField.brightness(BrightStars.LIMIT_MAG + 0.5f), 1e-6f)
        assertTrue(StarField.warmth(-0.03f) < 0.15f)
        assertEquals(0.53f, StarField.warmth(0.65f), 0.01f)
        assertEquals(1f, StarField.warmth(1.85f), 1e-6f)
        // Two neighbours in the catalogue twinkle out of step.
        assertTrue(abs(StarField.phase(10) - StarField.phase(11)) > 0.2f)
    }

    @Test
    fun `no star is ever read as a planet`() {
        assertTrue(StarField.STEADY_PHASE > StarField.STAR_PHASE_CEILING)
        (0 until BrightStars.count).forEach { i ->
            assertTrue(StarField.phase(i) <= StarField.STAR_PHASE_CEILING)
        }
        // The texture rounds a phase to a byte: the ceiling and the steady mark are bytes apart.
        assertTrue((StarField.STAR_PHASE_CEILING * 255f) < (StarField.STEADY_PHASE * 255f) - 3f)
    }

    @Test
    fun `Venus and Jupiter stand where the engine puts them, steady and larger than a star`() {
        // A year of evenings: the two are in front of a reader facing south on some of them.
        var seen = 0
        var at = Instant.parse("2026-01-01T21:00:00Z")
        repeat(73) {
            val marks = StarField.planets(at, milan, southern = false)
            marks.forEach { mark ->
                val planet = Planet.values().first { p ->
                    abs(AstronomyEngine.planetAltitude(p, at, milan) - mark.altitudeDeg) < 0.01
                }
                val across = ((AstronomyEngine.planetAzimuth(planet, at, milan) - 180.0 + 540.0) % 360.0) - 180.0
                assertEquals(across, mark.acrossDeg.toDouble(), 0.01)
                assertTrue(mark.altitudeDeg in StarField.WINDOW_ALT_LOW_DEG..StarField.WINDOW_ALT_HIGH_DEG)
                assertTrue(abs(mark.acrossDeg) <= StarField.WINDOW_ACROSS_DEG)
                assertEquals(StarField.STEADY_PHASE, mark.phase, 0f)
                assertEquals(1f, mark.brightness, 0f)
                assertTrue(mark.scale > 1f)
                assertTrue(markRadiusDp(mark) > starRadiusDp(1f))
                seen++
            }
            at = at.plusSeconds(5L * 86_400)
        }
        assertTrue("a planet on some evening of the year: $seen", seen > 0)
        // Venus is the larger disc and the whiter light.
        assertTrue(StarField.planetScale(Planet.VENUS) > StarField.planetScale(Planet.JUPITER))
        assertTrue(StarField.planetWarmth(Planet.VENUS) < StarField.planetWarmth(Planet.JUPITER))
    }

    @Test
    fun `a window of its own, the direction and the width a card asks for`() {
        val at = Instant.parse("2026-01-15T21:00:00Z")
        val south = StarField.visible(at, milan, 180.0, 36f)
        assertEquals(StarField.visible(at, milan, southern = false), south)
        val wide = StarField.visible(at, milan, 180.0, 48f)
        assertTrue(wide.size > south.size)
        assertTrue(wide.containsAll(south))
        // Turned toward the south-east, the stars are measured from there.
        val southEast = StarField.visible(at, milan, 135.0, 36f)
        assertTrue(southEast.isNotEmpty())
        assertTrue(southEast.none { it in south && abs(it.acrossDeg) < 9f })
    }

    @Test
    fun `a star lands on the canvas by the deck's camera, and off it when outside`() {
        val w = 800
        val h = 704
        val centre = starPlace(StarMark(0f, 37.5f, 1f, 0f, 0f), w, h)!!
        assertEquals(400f, centre.first, 0.01f)
        assertEquals(352f, centre.second, 0.01f)
        val top = starPlace(StarMark(0f, 65f, 1f, 0f, 0f), w, h)!!
        assertEquals(0f, top.second, 0.01f)
        val right = starPlace(StarMark(10f, 30f, 1f, 0f, 0f), w, h)!!
        assertTrue(right.first > centre.first)
        // The horizontal scale is the vertical one: a degree is the same number of px either way.
        val focal = h / Math.toRadians((StarCameraTopDeg - StarCameraBottomDeg).toDouble())
        assertEquals(400 + Math.tan(Math.toRadians(10.0)) * focal, right.first.toDouble(), 0.01)
        // About ±29° across a 400 dp canvas; beyond it, and over the top, a star is off.
        assertTrue(starPlace(StarMark(28f, 30f, 1f, 0f, 0f), w, h) != null)
        assertNull(starPlace(StarMark(31f, 30f, 1f, 0f, 0f), w, h))
        assertNull(starPlace(StarMark(0f, 70f, 1f, 0f, 0f), w, h))
        assertTrue(starRadiusDp(1f) > starRadiusDp(0.09f) * 2)
    }

    @Test
    fun `the galactic frame is two unit vectors at right angles, where the engine puts the centre`() {
        val at = Instant.parse("2026-07-15T23:00:00Z")
        val frame = StarField.galacticFrame(at, milan, 180.0)
        val pole = doubleArrayOf(frame.poleX.toDouble(), frame.poleY.toDouble(), frame.poleZ.toDouble())
        val centre = doubleArrayOf(frame.centreX.toDouble(), frame.centreY.toDouble(), frame.centreZ.toDouble())
        fun norm(v: DoubleArray) = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2])
        assertEquals(1.0, norm(pole), 1e-5)
        assertEquals(1.0, norm(centre), 1e-5)
        assertEquals(0.0, pole[0] * centre[0] + pole[1] * centre[1] + pole[2] * centre[2], 2e-3)
        // The centre's height is the engine's own galactic core altitude (its constant sits
        // a tenth of a degree from the J2000 centre).
        val altitude = Math.toDegrees(Math.asin(centre[1]))
        assertEquals(AstronomyEngine.galacticCoreAltitude(at, milan), altitude, 0.15)
        // Turning the camera turns x and z and leaves the height alone.
        val turned = StarField.galacticFrame(at, milan, 135.0)
        assertEquals(frame.centreY, turned.centreY, 1e-6f)
        assertTrue(abs(frame.centreX - turned.centreX) > 0.05f)
    }
}
