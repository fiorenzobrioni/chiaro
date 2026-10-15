package com.callbackdev.chiaro.ui.today

import androidx.compose.ui.graphics.compositeOver
import com.callbackdev.chiaro.data.AppPalette
import com.callbackdev.chiaro.ui.theme.SkyPalette
import com.callbackdev.chiaro.ui.theme.paletteFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The place row that fades with the scroll (DESIGN §8.1b, committente, 15 ott 2026). The
 * fade is a sweep from the sky to the paper, and the one thing it may never do is set the
 * name on a ground it does not read on — the grey-on-grey a plain crossfade has halfway.
 * Swept over every sky the canvas can have at its top, both palettes, both themes, and the
 * whole scroll.
 */
class PlaceBarColorsTest {

    private data class Case(val palette: AppPalette, val dark: Boolean, val altitude: Double, val f: Float)

    private fun sweep(): List<Pair<Case, Double>> = buildList {
        AppPalette.entries.forEach { palette ->
            listOf(false, true).forEach { dark ->
                val scheme = paletteFor(palette).scheme(dark)
                SkyPalette.entries.forEach { sky ->
                    var altitude = -90.0
                    while (altitude <= 90.0) {
                        val top = SkyPalette.ScrimColor.copy(alpha = SkyPalette.ScrimAlpha)
                            .compositeOver(sky.gradient(altitude).top)
                        (0..100).forEach { step ->
                            val f = step / 100f
                            val bar = placeBarColors(top, scheme.surface, scheme.onSurface, scheme.outlineVariant, f, 1f)
                            add(Case(palette, dark, altitude, f) to contrast(bar.ink, bar.ground))
                        }
                        altitude += 2.0
                    }
                }
            }
        }
    }

    @Test
    fun `the name reads on the bar at every point of the fade`() {
        val (worst, ratio) = sweep().minBy { it.second }
        // The flip point, where the sky's white and the theme's ink are as good as each
        // other: 4.14:1 measured (light theme, sun at −10°, 47% of the way), past the 3:1 a
        // 22sp name needs, and the figure DESIGN §8.1b quotes.
        assertTrue("worst is %.2f:1 at %s".format(ratio, worst), ratio >= MeasuredWorst)
    }

    @Test
    fun `the two ends are the canvas' and the other tabs' own`() {
        val scheme = paletteFor(AppPalette.PAPER).scheme(false)
        val top = SkyPalette.ScrimColor.copy(alpha = SkyPalette.ScrimAlpha)
            .compositeOver(SkyPalette.entries.first().gradient(45.0).top)
        val sky = placeBarColors(top, scheme.surface, scheme.onSurface, scheme.outlineVariant, 0f, 1f)
        assertEquals(SkyPalette.Ink, sky.ink)
        assertTrue(sky.inkIsLight)
        val paper = placeBarColors(top, scheme.surface, scheme.onSurface, scheme.outlineVariant, 1f, 1f)
        assertEquals(scheme.surface, paper.ground)
        assertEquals(scheme.onSurface, paper.ink)
        assertEquals(scheme.outlineVariant, paper.dots)
    }

    @Test
    fun `in the light theme the ink flips once, never back`() {
        val scheme = paletteFor(AppPalette.PAPER).scheme(false)
        SkyPalette.entries.forEach { sky ->
            val top = SkyPalette.ScrimColor.copy(alpha = SkyPalette.ScrimAlpha)
                .compositeOver(sky.gradient(30.0).top)
            val lights = (0..100).map {
                placeBarColors(top, scheme.surface, scheme.onSurface, scheme.outlineVariant, it / 100f, 1f).inkIsLight
            }
            val flips = lights.zipWithNext().count { (a, b) -> a != b }
            assertEquals("light→dark exactly once", 1, flips)
        }
    }

    private companion object {
        const val MeasuredWorst = 4.1
    }
}
