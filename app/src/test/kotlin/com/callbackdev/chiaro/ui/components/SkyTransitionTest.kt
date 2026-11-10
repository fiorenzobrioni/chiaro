package com.callbackdev.chiaro.ui.components

import androidx.compose.ui.geometry.Offset
import com.callbackdev.chiaro.domain.model.CloudLayers
import com.callbackdev.chiaro.ui.theme.SkyPalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The sky between two skies (DESIGN §3.9, 9 nov 2026): what the canvas draws while the hour,
 * the place or the forecast changes under it.
 */
class SkyTransitionTest {

    private fun sky(
        alt: Double,
        cloud: Int = 0,
        moonAlt: Double = -20.0,
        moonIllum: Double = 0.0,
        fog: Float = 0f,
        sunAz: Double = 180.0
    ): Sky {
        val gradient = SkyPalette.Paper.gradient(alt, cloud, 0, moonIllum, moonAlt)
        return gradient to skyScene(
            SkyPalette.Paper, gradient, alt, sunAz, moonAlt, 150.0, moonIllum, 0.0, false, cloud,
            CloudLayers(cloud, 0, 0), 0, SkyFall.NONE, 0.0, 10.0, 270, fog = fog
        )
    }

    @Test
    fun `the ends are the two skies themselves`() {
        val a = sky(40.0, 20)
        val b = sky(10.0, 80)
        assertSame(a, lerpSky(a, b, 0f))
        assertSame(b, lerpSky(a, b, 1f))
    }

    @Test
    fun `halfway, every amount is halfway`() {
        val a = sky(40.0, 20)
        val b = sky(40.0, 80)
        val (gradient, scene) = lerpSky(a, b, 0.5f)
        assertEquals((a.second.deck + b.second.deck) / 2f, scene.deck, 1e-6f)
        assertEquals((a.second.cover + b.second.cover) / 2f, scene.cover, 1e-6f)
        assertEquals((a.second.opacity + b.second.opacity) / 2f, scene.opacity, 1e-6f)
        // The gradient is between the two: darker than the open sky, lighter than the closed one.
        assertTrue(gradient.mid != a.first.mid && gradient.mid != b.first.mid)
    }

    /** The sun sets by dimming where it stood: no leap to a place it never had. */
    @Test
    fun `a body that is in one sky only keeps its place and fades`() {
        val up = sky(2.0, sunAz = 260.0)
        val down = sky(-3.0, sunAz = 264.0)
        assertNotNull(up.second.sun)
        assertNull(down.second.sun)
        val mid = lerpSky(up, down, 0.5f).second.sun!!
        assertEquals(up.second.sun!!.across, mid.across, 1e-6f)
        assertEquals(up.second.sun!!.up, mid.up, 1e-6f)
        assertEquals(up.second.sun!!.disc / 2f, mid.disc, 1e-6f)
        // And the other way: the moon rises where it will stand.
        val dark = sky(-30.0)
        val moonlit = sky(-30.0, moonAlt = 30.0, moonIllum = 0.8)
        val rising = lerpSky(dark, moonlit, 0.25f).second.moon!!
        assertEquals(moonlit.second.moon!!.across, rising.across, 1e-6f)
        assertEquals(moonlit.second.moon!!.disc * 0.25f, rising.disc, 1e-6f)
        assertNull(lerpSky(dark, dark.first to dark.second.copy(deck = 0.5f), 0.5f).second.sun)
    }

    @Test
    fun `an angle turns the short way round`() {
        // As a direction: 365° is 5°, and the shader reads the angle through its sine and cosine.
        fun direction(deg: Float) = ((deg % 360f) + 360f) % 360f
        assertEquals(180f, direction(turnTo(170f, -170f, 0.5f)), 1e-4f)
        assertEquals(5f, direction(turnTo(350f, 20f, 0.5f)), 1e-4f)
        assertEquals(45f, direction(turnTo(0f, 90f, 0.5f)), 1e-4f)
        assertEquals(350f, direction(turnTo(350f, 20f, 0f)), 1e-4f)
        assertEquals(20f, direction(turnTo(350f, 20f, 1f)), 1e-4f)
    }

    /** A fog that comes wears its own colour from the first frame, not a mix with no colour. */
    @Test
    fun `a fog that comes or goes wears the colour of the sky that has it`() {
        val clear = sky(30.0, 10)
        val foggy = sky(30.0, 10, fog = 1f)
        val coming = lerpSky(clear, foggy, 0.2f).second
        assertEquals(0.2f, coming.fog, 1e-6f)
        assertEquals(foggy.second.fogColor, coming.fogColor)
        val going = lerpSky(foggy, clear, 0.8f).second
        assertEquals(foggy.second.fogColor, going.fogColor)
    }

    @Test
    fun `the stars' list changes at the middle, their amount all the way`() {
        val day = sky(-8.0)
        val star = listOf(StarMark(0f, 30f, 1f, 0.2f, 0.4f))
        val night = sky(-30.0).let { it.first to it.second.copy(starField = star, radiant = Offset(10f, 30f)) }
        assertTrue(lerpSky(day, night, 0.4f).second.starField.isEmpty())
        assertEquals(star, lerpSky(day, night, 0.6f).second.starField)
        assertEquals(night.second.stars * 0.4f, lerpSky(day, night, 0.4f).second.stars, 1e-6f)
    }

    // ------------------------------------------------ what is a glide and what is not, 10 nov 2026

    /** The page hands over a new sky every minute, and a minute of the sun is not weather. */
    @Test
    fun `a minute of the sun is under the glide, weather is over it`() {
        assertEquals(0f, skyDistance(sky(12.0, 50), sky(12.0, 50)), 0f)
        // A quarter of a degree, the most the sun moves in a minute, by day and in the night.
        assertTrue(skyDistance(sky(40.0, 30, sunAz = 180.0), sky(39.75, 30, sunAz = 180.5)) < GlideFrom)
        assertTrue(
            skyDistance(
                sky(-30.0, moonAlt = 40.0, moonIllum = 0.6),
                sky(-30.25, moonAlt = 40.2, moonIllum = 0.6)
            ) < GlideFrom
        )
        // Ten points of cloud at the turn of an hour, a fog that comes, a sun that sets.
        assertTrue(skyDistance(sky(40.0, 30), sky(40.0, 40)) >= GlideFrom)
        assertTrue(skyDistance(sky(40.0, 30), sky(40.0, 30, fog = 0.8f)) >= GlideFrom)
        assertTrue(skyDistance(sky(-0.4, sunAz = 260.0), sky(-0.6, sunAz = 260.2)) >= GlideFrom)
    }

    /** The sun from 60° down to −40° a quarter of a degree at a time, under three skies: which
     * of those minutes are far enough from the one before to glide. Measured on 10 nov 2026:
     * 13 of 400 under a clear sky, 5 under a broken one, 1 under a closed one, all where the
     * light turns golden, the sun sets and the planets come out. Until then, all 400. */
    @Test
    fun `of a whole day's minutes few glide`() {
        val steps = generateSequence(60.0) { it - 0.25 }.takeWhile { it >= -40.0 }.toList()
        listOf(0, 40, 100).forEach { cloud ->
            val far = steps.zipWithNext().filter { (a, b) ->
                skyDistance(sky(a, cloud, sunAz = 260.0 - a), sky(b, cloud, sunAz = 260.0 - b)) >= GlideFrom
            }.map { it.first }
            println("glide measure, cloud $cloud: ${far.size} of ${steps.size - 1} minutes, at $far")
            assertTrue("cloud $cloud: ${far.size} glides", far.size <= 20)
            assertTrue("cloud $cloud: only round the horizon, $far", far.all { it < 6.0 && it > -5.0 })
        }
    }
}
