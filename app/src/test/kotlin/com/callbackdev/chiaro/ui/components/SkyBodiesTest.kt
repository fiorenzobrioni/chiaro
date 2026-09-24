package com.callbackdev.chiaro.ui.components

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * The sun and the moon on the canvas (23 set 2026): where they are drawn is arithmetic,
 * and the arithmetic is what makes them the sky rather than a decoration.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SkyBodiesTest {

    private fun bodies(elongation: Double, southern: Boolean) = SkyBodies(
        sunAltitudeDeg = -20.0, sunAzimuthDeg = 300.0,
        moonAltitudeDeg = 30.0, moonAzimuthDeg = 150.0,
        moonIllumination = 0.5, moonElongationDeg = elongation,
        cloudPct = 0, southern = southern
    )

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
    fun `a waxing moon is lit toward the evening sun, mirrored south of the equator`() {
        assertTrue(litOnRight(bodies(elongation = 90.0, southern = false)))
        assertFalse(litOnRight(bodies(elongation = 270.0, southern = false)))
        assertFalse(litOnRight(bodies(elongation = 90.0, southern = true)))
        assertTrue(litOnRight(bodies(elongation = 270.0, southern = true)))
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
        // The glow stays the brightest thing there is, and spreads.
        assertTrue(overcast.glowCore >= 0.35f)
        assertTrue(overcast.spread > 1.2f)
        // In between the disc fades steadily and never jumps.
        val half = sunVeil(0.5f)
        assertTrue(half.disc in 0.4f..0.6f)
        assertTrue(sunVeil(0.8f).disc < half.disc)
        // Out of range clamps.
        assertEquals(overcast, sunVeil(1.7f))
        assertEquals(clear, sunVeil(-0.2f))
    }
}
