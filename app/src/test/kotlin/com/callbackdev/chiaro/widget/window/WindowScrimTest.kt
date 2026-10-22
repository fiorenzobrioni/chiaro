package com.callbackdev.chiaro.widget.window

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import com.callbackdev.chiaro.ui.theme.SkyPalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The window's caption veil in the sky's own colour (committente, 22 ott 2026): the band of
 * before, as dark as the black veil at 50% was over the same ground, in a deeper shade of
 * the sky's zenith instead of a neutral black.
 */
class WindowScrimTest {

    private val clear = SkyPalette.Paper.gradient(40.0, 0, 0, 0.0, -20.0)
    private val night = SkyPalette.Paper.gradient(-30.0, 0, 0, 0.0, -20.0)
    private val blackAtHalf = Color.Black.copy(alpha = 0.5f)

    @Test
    fun `under a clear sky the veil is a blue, not a grey`() {
        val veil = WindowPainter.captionVeil(clear)
        assertTrue("blue over red: $veil", veil.blue > veil.red + 0.15f)
    }

    /** Only the hue moves: over the pale horizon and a sunlit cloud the coloured veil is as
     * dark as the black one was, to the step of the search. */
    @Test
    fun `by day the coloured veil keeps the darkness of the black one`() {
        listOf(SkyPalette.Paper.brightestBottomStop(), Color(0xFFF4F6F8)).forEach { ground ->
            val veil = WindowPainter.captionVeil(clear)
            val alpha = WindowPainter.todaysDarknessIn(ground, veil)
            assertTrue("$alpha", alpha > 0f && alpha < WindowPainter.CaptionScrimMax)
            val was = blackAtHalf.compositeOver(ground).luminance()
            val now = veil.copy(alpha = alpha).compositeOver(ground).luminance()
            assertTrue("not lighter than before over $ground: $now against $was", now <= was + 1e-4f)
            val lighter = veil.copy(alpha = alpha - 0.025f).compositeOver(ground).luminance()
            assertTrue("the least that does it over $ground", lighter > was)
        }
    }

    /** At night the navy veil cannot be as dark as black at half: it stops at its cap, a
     * touch lighter than before, and white over it stays far above any floor. */
    @Test
    fun `at night the veil stops at its cap and white stays far above the floor`() {
        val ground = night.bottom
        val veil = WindowPainter.captionVeil(night)
        assertEquals(WindowPainter.CaptionScrimMax, WindowPainter.todaysDarknessIn(ground, veil), 1e-6f)
        val l = veil.copy(alpha = WindowPainter.CaptionScrimMax).compositeOver(ground).luminance()
        val contrast = 1.05f / (l + 0.05f)
        assertTrue("white over the night's veiled foot: $contrast", contrast > 9f)
    }

    @Test
    fun `a ground the black veil could not darken asks for nothing`() {
        assertEquals(0f, WindowPainter.todaysDarknessIn(Color.Black, WindowPainter.captionVeil(night)))
    }
}
