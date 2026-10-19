package com.callbackdev.chiaro.ui.components

import com.callbackdev.chiaro.domain.model.Coordinates
import com.callbackdev.chiaro.domain.sky.AstronomyEngine
import com.callbackdev.chiaro.domain.sky.BrightStars
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
        assertTrue("some stars on a January evening: ${stars.size}", stars.size in 8..120)
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
        assertTrue(stars.none { it.altitudeDeg > 44f && abs(it.acrossDeg) > 29f })
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
        assertEquals(0.2f, StarField.brightness(BrightStars.LIMIT_MAG + 0.5f), 1e-6f)
        assertTrue(StarField.warmth(-0.03f) < 0.15f)
        assertEquals(0.53f, StarField.warmth(0.65f), 0.01f)
        assertEquals(1f, StarField.warmth(1.85f), 1e-6f)
        // Two neighbours in the catalogue twinkle out of step.
        assertTrue(abs(StarField.phase(10) - StarField.phase(11)) > 0.2f)
    }

    @Test
    fun `a star lands on the canvas by the deck's camera, and off it when outside`() {
        val w = 800
        val h = 704
        val centre = starPlace(StarMark(0f, 30f, 1f, 0f, 0f), w, h)!!
        assertEquals(400f, centre.first, 0.01f)
        assertEquals(352f, centre.second, 0.01f)
        val top = starPlace(StarMark(0f, 45f, 1f, 0f, 0f), w, h)!!
        assertEquals(0f, top.second, 0.01f)
        val right = starPlace(StarMark(10f, 30f, 1f, 0f, 0f), w, h)!!
        assertTrue(right.first > centre.first)
        // The horizontal scale is the plane's: a degree is the same number of px either way.
        val focal = h / Math.toRadians((SkyCameraTopDeg - SkyCameraBottomDeg).toDouble())
        assertEquals(400 + Math.tan(Math.toRadians(10.0)) * focal, right.first.toDouble(), 0.01)
        assertNull(starPlace(StarMark(29f, 30f, 1f, 0f, 0f), w, h))
        assertNull(starPlace(StarMark(0f, 60f, 1f, 0f, 0f), w, h))
        assertTrue(starRadiusDp(1f) > starRadiusDp(0.2f) * 2)
    }
}
