package com.callbackdev.chiaro.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

/**
 * DESIGN.md §3.6. The canvas is the one place in the app that does not follow the
 * reader's theme, so it is also the one place where text could silently become
 * unreadable. The contract: white over the scrim, over the brightest sky this palette
 * can produce, is at least 4.5:1.
 *
 * The alpha is 0.55 because of the numbers below, not because it looked right.
 */
class ScrimContractTest {

    // The values the canvas actually paints with, not a copy of them: a test that
    // restates the constant it is guarding guards nothing.
    private val scrim = SkyPalette.ScrimColor
    private val alpha = SkyPalette.ScrimAlpha

    private fun channel(c: Float) =
        if (c <= 0.03928f) c / 12.92 else ((c + 0.055) / 1.055).toDouble().pow(2.4)

    private fun luminance(color: Color) =
        0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)

    private fun contrast(a: Color, b: Color): Double {
        val (hi, lo) = listOf(luminance(a), luminance(b)).sortedDescending()
        return (hi + 0.05) / (lo + 0.05)
    }

    /**
     * The scrim over a sky, the way the framebuffer does it (found in the Fase 9
     * accessibility pass): the canvas paints `ScrimColor.copy(alpha = …)` in a brush,
     * so SRC_OVER mixes the two colors' sRGB VALUES and the pixel the reader sees is
     * 8 bits per channel.
     *
     * This used `Color.lerp`, which interpolates in Oklab: a perceptual blend, not a
     * composite. Close enough to keep passing, wrong enough that the document and the
     * test disagreed about the number they were both quoting, and §3.6's rejected
     * alphas turned out to be quoting a third arithmetic again. `PaletteDocTest`
     * measures with this same function now, which is the point of writing it once.
     */
    private fun scrimmed(background: Color, a: Float = alpha): Color {
        fun mix(f: Float, b: Float) = f * a + b * (1 - a)
        return Color(
            mix(scrim.red, background.red),
            mix(scrim.green, background.green),
            mix(scrim.blue, background.blue)
        )
    }

    @Test
    fun `white text survives the brightest sky the palette can produce`() {
        val brightest = SkyPalette.brightestBottomStop()
        val actual = contrast(Color.White, scrimmed(brightest))
        assertTrue("white over the scrimmed canvas is %.2f:1".format(actual), actual >= 4.5)
    }

    @Test
    fun `every stop of every band survives it, not just the one we thought was brightest`() {
        // The band table is data; a future row could be brighter than the day sky and
        // nobody would notice until a screenshot. So: sweep the whole altitude range.
        var worst = Double.MAX_VALUE
        var worstAt = 0.0
        var altitude = -90.0
        while (altitude <= 90.0) {
            SkyPalette.gradient(altitude).stops().forEach { stop ->
                val c = contrast(Color.White, scrimmed(stop))
                if (c < worst) {
                    worst = c
                    worstAt = altitude
                }
            }
            altitude += 0.5
        }
        assertTrue(
            "the worst case is %.2f:1 at altitude %.1f".format(worst, worstAt),
            worst >= 4.5
        )
    }

    @Test
    fun `the chosen alpha is the smallest one that clears the floor with headroom`() {
        val brightest = SkyPalette.brightestBottomStop()
        assertTrue("0.45 should NOT clear the floor, or the spec is stale",
            contrast(Color.White, scrimmed(brightest, 0.45f)) < 4.5)
        assertTrue("0.55 should clear it",
            contrast(Color.White, scrimmed(brightest, 0.55f)) >= 4.5)
    }
}
