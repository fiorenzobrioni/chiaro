package com.callbackdev.chiaro.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

/**
 * The canvas is computed, so it can be wrong in ways a color chosen by hand cannot be.
 * These are the claims DESIGN.md §3 makes, as assertions.
 *
 * Every one of them runs over EVERY band table (§3.7). The vivid table was derived from
 * the paper one at held luminance, so all of this should hold for it by construction —
 * and "by construction" is a thing you say after the test passes, not instead of it.
 */
class SkyPaletteTest {

    private fun channel(c: Float) =
        if (c <= 0.03928f) c / 12.92 else ((c + 0.055) / 1.055).toDouble().pow(2.4)

    private fun luminance(c: androidx.compose.ui.graphics.Color) =
        0.2126 * channel(c.red) + 0.7152 * channel(c.green) + 0.0722 * channel(c.blue)

    /** Runs one claim against both tables, naming the one that broke. */
    private fun eachPalette(block: (SkyPalette, String) -> Unit) =
        SkyPalette.entries.forEachIndexed { i, palette ->
            block(palette, if (i == 0) "paper" else "vivid")
        }

    private fun SkyPalette.brightness(altitude: Double, cloud: Int = 0, precip: Int = 0,
                                      illum: Double = 0.0, moonAlt: Double = -90.0) =
        gradient(altitude, cloud, precip, illum, moonAlt).stops().sumOf { c ->
            0.2126 * channel(c.red) + 0.7152 * channel(c.green) + 0.0722 * channel(c.blue)
        }

    @Test
    fun `once the sun is down the sky only gets darker`() = eachPalette { sky, name ->
        // Deliberately NOT asserted above the horizon: golden hour genuinely brightens
        // the bottom of the sky while the top darkens, which the first version of this
        // test called a bug at altitude 5.5 and which is the whole reason the band
        // exists. Below the horizon there is no such excuse.
        var previous = sky.brightness(0.0)
        var altitude = -0.5
        while (altitude >= -40.0) {
            val here = sky.brightness(altitude)
            assertTrue("the $name sky brightened after sunset, at $altitude", here <= previous + 1e-6)
            altitude -= 0.5
            previous = here
        }
    }

    @Test
    fun `the canvas never snaps, at any altitude`() = eachPalette { sky, name ->
        // Continuity is what makes the canvas readable as a sky rather than as seven
        // states: no half-degree of the sun's travel may visibly jump.
        var worst = 0.0
        var worstAt = 0.0
        var altitude = 90.0
        while (altitude > -90.0) {
            val delta = kotlin.math.abs(sky.brightness(altitude) - sky.brightness(altitude - 0.5))
            if (delta > worst) {
                worst = delta
                worstAt = altitude
            }
            altitude -= 0.5
        }
        // The bound is calibrated, not guessed: the largest legitimate half-degree step
        // is 0.104, at the horizon, where the anchors are 6° apart and the sky is
        // genuinely doing the fastest thing it does all day. What this catches is the
        // regression that matters — an implementation that buckets instead of
        // interpolating jumps by a whole band difference (several tenths) at each edge.
        assertTrue("a half-degree step changed the $name sky by %.4f at %.1f".format(worst, worstAt),
            worst < 0.15)
    }

    @Test
    fun `an anchor renders as itself, and between two anchors is a blend of both`() =
        eachPalette { sky, name ->
            // Together with the step bound above, this is what says "interpolated"
            // rather than "bucketed": the midpoint between two anchors must be neither.
            val golden = sky.gradient(0.0)
            val civil = sky.gradient(-6.0)
            val between = sky.gradient(-3.0)
            assertTrue("the $name midpoint must not be the anchor above it", between != golden)
            assertTrue("the $name midpoint must not be the anchor below it", between != civil)
            val mid = sky.brightness(-3.0)
            assertTrue("the $name midpoint must sit between its anchors",
                mid < sky.brightness(0.0) && mid > sky.brightness(-6.0))
        }

    @Test
    fun `the day is far brighter than the night, which is the only absolute claim here`() =
        eachPalette { sky, name ->
            assertTrue(name, sky.brightness(60.0) > sky.brightness(-30.0) * 5)
        }

    @Test
    fun `an overcast sky keeps a third of its band, so morning still looks like morning`() =
        eachPalette { sky, name ->
            val clearNoon = sky.gradient(50.0)
            val cloudyNoon = sky.gradient(50.0, cloudPct = 100)
            assertTrue("$name: full cloud should not erase the band", cloudyNoon != clearNoon)
            assertTrue("$name: full cloud should still be recognisably day",
                sky.brightness(50.0, cloud = 100) > sky.brightness(-20.0))
            assertTrue("$name: an overcast midnight must not read as dusk",
                sky.brightness(-30.0, cloud = 100) < sky.brightness(-3.0))
        }

    @Test
    fun `clouds hide the moon, and not the other way round`() = eachPalette { sky, name ->
        // The hole in the first draft of §3.4: applying the moon lift after the cloud mix
        // without scaling it by the cloud made an overcast full-moon night BRIGHTER than
        // a clear one.
        val clearFullMoon = sky.brightness(-30.0, illum = 1.0, moonAlt = 60.0)
        val overcastFullMoon = sky.brightness(-30.0, cloud = 100, illum = 1.0, moonAlt = 60.0)
        assertTrue("$name: an overcast full moon must not out-shine a clear one",
            overcastFullMoon < clearFullMoon)
    }

    @Test
    fun `a moon below the horizon contributes nothing`() = eachPalette { sky, name ->
        assertEquals(
            name,
            sky.gradient(-30.0),
            sky.gradient(-30.0, moonIllumination = 1.0, moonAltitudeDeg = -5.0)
        )
    }

    @Test
    fun `the moon only lifts a sky that is actually dark`() = eachPalette { sky, name ->
        assertEquals(
            name,
            sky.gradient(10.0),
            sky.gradient(10.0, moonIllumination = 1.0, moonAltitudeDeg = 45.0)
        )
    }

    @Test
    fun `rain darkens the sky only once it is likely`() = eachPalette { sky, name ->
        assertEquals(name, sky.gradient(30.0), sky.gradient(30.0, precipPct = 50))
        assertTrue("$name: 90% rain should darken",
            sky.brightness(30.0, precip = 90) < sky.brightness(30.0))
    }

    @Test
    fun `the altitude is clamped, not wrapped`() = eachPalette { sky, name ->
        assertEquals(name, sky.gradient(90.0), sky.gradient(200.0))
        assertEquals(name, sky.gradient(-90.0), sky.gradient(-200.0))
    }

    @Test
    fun `the two tables are the same sky at two saturations`() {
        val paper = SkyPalette.Paper.anchors
        val vivid = SkyPalette.Vivid.anchors
        assertEquals("the vivid table must have the paper table's bands", paper.size, vivid.size)
        paper.zip(vivid).forEach { (a, b) ->
            assertEquals("the two tables must anchor at the same altitudes", a.first, b.first, 0.0)
            // The claim of §3.7, at the only places it is exactly true: an ANCHOR holds
            // its luminance, which is what carries every brightness ordering across.
            a.second.stops().zip(b.second.stops()).forEachIndexed { stop, (before, after) ->
                val drift = kotlin.math.abs(luminance(before) - luminance(after))
                assertTrue(
                    "the ${a.first}° anchor's stop $stop moved its luminance by %.4f".format(drift),
                    drift < 0.003
                )
            }
        }
        // BETWEEN anchors the two tables drift, and the reason is worth stating: the
        // blend is `Color.lerp`, which interpolates in Oklab. A perceptual midpoint
        // between two saturated colors is not the midpoint between two dull ones, even
        // when the ends agree — so the bound here is a measured ceiling (0.0112, at −4°,
        // over a sum of three stops), not an equality.
        var altitude = -90.0
        var worst = 0.0
        var worstAt = 0.0
        while (altitude <= 90.0) {
            val delta = kotlin.math.abs(
                SkyPalette.Paper.brightness(altitude) - SkyPalette.Vivid.brightness(altitude)
            )
            if (delta > worst) {
                worst = delta
                worstAt = altitude
            }
            altitude += 0.5
        }
        assertTrue(
            "the two skies part company by %.4f at %.1f".format(worst, worstAt),
            worst < 0.02
        )
        assertTrue(
            "the vivid table must not BE the paper table",
            SkyPalette.Paper.gradient(-6.0) != SkyPalette.Vivid.gradient(-6.0)
        )
    }
}
