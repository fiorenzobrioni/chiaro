package com.callbackdev.chiaro.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

/**
 * «In immagini»'s inks, measured (PLANNING, «Fase 17», «La lastra»: «con il loro test di
 * contrasto a 3:1 come segni»): every rain tone, the dial's dots and the strip's ring at 3:1
 * as marks over the plate's ground, the labels and the hours at 4.5:1 as text — on the
 * brightest scrimmed sky either table paints, on the six card colours, on paper and on the
 * dark card, in both dresses.
 */
class PicturesInksTest {

    private fun channel(c: Float) =
        if (c <= 0.03928f) c / 12.92 else ((c + 0.055) / 1.055).toDouble().pow(2.4)

    private fun luminance(color: Color) =
        0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)

    private fun contrast(a: Color, b: Color): Double {
        val (hi, lo) = listOf(luminance(a), luminance(b)).sortedDescending()
        return (hi + 0.05) / (lo + 0.05)
    }

    /** A colour with alpha over an opaque one, per channel, as SRC_OVER does it. */
    private fun over(top: Color, ground: Color): Color = top.compositeOver(ground).copy(alpha = 1f)

    /** The brightest the widget's sky dress gets under its scrim (`skyGradientBitmap`: the
     * flat §3.6 scrim at its alpha), over every altitude, cover and chance of either table. */
    private fun brightestSkies(): List<Color> = SkyPalette.entries.map { sky ->
        var best = Color.Black
        var altitude = -90.0
        while (altitude <= 90.0) {
            for (cloud in listOf(0, 25, 50, 75, 100)) for (chance in listOf(0, 50, 100)) {
                sky.gradient(altitude, cloud, chance).stops().forEach { stop ->
                    val g = over(SkyPalette.ScrimColor.copy(alpha = SkyPalette.ScrimAlpha), stop)
                    if (luminance(g) > luminance(best)) best = g
                }
            }
            altitude += 1.0
        }
        best
    }

    /** Every ground with the inks that go on it. */
    private fun grounds(): List<Triple<String, Color, PicturesInks>> {
        val skies = brightestSkies().mapIndexed { i, g -> Triple("sky $i", g, PicturesInks.Sky) }
        val cards = WidgetCardColor.entries.map { Triple("card $it", widgetCardContainer(it), PicturesInks.Card) }
        val schemes = ChiaroPalettes.values.flatMap { dress ->
            val l = dress.lightScheme
            val d = dress.darkScheme
            listOf(
                Triple("paper", l.surface, PicturesInks.light(l.onSurface, l.onSurfaceVariant, l.surface)),
                Triple("dark", d.surface, PicturesInks.dark(d.onSurface, d.onSurfaceVariant, d.surface))
            )
        }
        return skies + cards + schemes
    }

    @Test
    fun `every rain tone clears 3 to 1 as a mark on the plate, on every ground`() {
        grounds().forEach { (name, ground, inks) ->
            val plate = over(inks.veil, ground)
            inks.rain.forEachIndexed { i, tone ->
                val c = contrast(tone, plate)
                assertTrue("$name, tone $i: %.2f:1".format(c), c >= 3.0)
            }
            val dot = contrast(inks.dot, ground)
            assertTrue("$name, the dial's dot: %.2f:1".format(dot), dot >= 3.0)
        }
    }

    @Test
    fun `the four tones stay four, in order of strength`() {
        grounds().forEach { (name, ground, inks) ->
            assertEquals(4, inks.rain.size)
            val plate = over(inks.veil, ground)
            val ratios = inks.rain.map { contrast(it, plate) }
            assertEquals("$name: the stronger, the further from the ground", ratios.sorted(), ratios)
            ratios.zipWithNext().forEach { (a, b) ->
                assertTrue("$name: two tones a step apart, $a and $b", b / a >= 1.1)
            }
        }
    }

    @Test
    fun `the labels and the hours are text, at 4 and a half to 1`() {
        grounds().forEach { (name, ground, inks) ->
            val plate = over(inks.veil, ground)
            val label = contrast(over(inks.ink, plate), plate)
            val hour = contrast(over(inks.quiet, plate), plate)
            assertTrue("$name, the high and the low: %.2f:1".format(label), label >= 4.5)
            assertTrue("$name, the hours: %.2f:1".format(hour), hour >= 4.5)
        }
    }

    @Test
    fun `the chance under the hours and the days is text on the card, at 4 and a half to 1`() {
        grounds().forEach { (name, ground, inks) ->
            val c = contrast(inks.dot, ground)
            assertTrue("$name, the chance in words: %.2f:1".format(c), c >= 4.5)
        }
    }

    @Test
    fun `the canvas's own tones were the ones that failed`() {
        // So the change is a measurement and not a taste: the canvas's darkest sky tone is under
        // the floor on the brightest sky, its lightest paper tone on paper.
        val sky = over(PicturesInks.Sky.veil, brightestSkies().maxBy { luminance(it) })
        assertTrue(contrast(Color(0xFF1E97CF), sky) < 3.0)
        val paper = ChiaroPalettes.values.first().lightScheme
        val onPaper = over(paper.onSurface.copy(alpha = 0.05f), paper.surface)
        assertTrue(contrast(Color(0xFF76BCEC), onPaper) < 3.0)
    }
}
