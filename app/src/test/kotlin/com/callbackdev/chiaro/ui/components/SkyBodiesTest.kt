package com.callbackdev.chiaro.ui.components

import androidx.compose.ui.geometry.Offset
import com.callbackdev.chiaro.domain.model.CloudLayers
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
    fun `a waxing moon is lit toward the evening sun, mirrored south of the equator`() {
        assertTrue(litOnRight(90.0, southern = false))
        assertFalse(litOnRight(270.0, southern = false))
        assertFalse(litOnRight(90.0, southern = true))
        assertTrue(litOnRight(270.0, southern = true))
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

    /** 16 ott 2026: a day moon behind a stratus is not there; at night its place still glows. */
    @Test
    fun `the moon goes behind a low deck, and keeps a glow there only at night`() {
        val clearDay = moonVeil(0f, day = true)
        assertEquals(MoonVeil(disc = 0.55f, glow = 0.55f, spread = 1f), clearDay)
        assertEquals(MoonVeil(disc = 0.95f, glow = 0.95f, spread = 1f), moonVeil(0f, day = false))
        val deckDay = moonVeil(1f, day = true)
        assertEquals(0f, deckDay.disc, 1e-6f)
        assertEquals(0f, deckDay.glow, 1e-6f)
        val deckNight = moonVeil(1f, day = false)
        assertEquals(0f, deckNight.disc, 1e-6f)
        assertTrue(deckNight.glow > 0.3f)
        assertTrue(deckNight.spread > 1.2f)
        // Behind a high veil the day moon is paler, not gone.
        val veilDay = moonVeil(cloudOpacity(100, CloudLayers(0, 0, 100), SkyFall.NONE), day = true)
        assertTrue(veilDay.disc in 0.3f..0.45f)
        // Steady in between, and clamped outside.
        assertTrue(moonVeil(0.8f, day = true).disc < moonVeil(0.5f, day = true).disc)
        assertEquals(deckDay, moonVeil(1.4f, day = true))
    }
}
