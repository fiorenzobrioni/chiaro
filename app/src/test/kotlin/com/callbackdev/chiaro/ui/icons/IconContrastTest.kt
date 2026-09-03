package com.callbackdev.chiaro.ui.icons

import androidx.compose.ui.graphics.Color
import com.callbackdev.chiaro.ui.theme.ChiaroDarkScheme
import com.callbackdev.chiaro.ui.theme.ChiaroLightScheme
import java.io.File
import kotlin.math.pow
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The icon strokes are the only carrier of "what kind of weather" in the hour strip, so
 * they are non-text marks under DESIGN.md §10 and owe 3:1 against BOTH surfaces — which
 * Meteocons' own palette does not clear (its cloud gray is 1.18:1 on the light surface;
 * the recoloring and its table live in `tools/import_meteocons.py`).
 *
 * This sweeps the emitted XML rather than the tool's table, for the same reason
 * `PaletteContrastTest` asserts the outcome instead of trusting the method: what ships
 * is the file, and a hand edit to a drawable would slip past a table nobody re-runs.
 */
class IconContrastTest {

    private fun channel(c: Float) =
        if (c <= 0.03928f) c / 12.92 else ((c + 0.055) / 1.055).toDouble().pow(2.4)

    private fun luminance(color: Color) =
        0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)

    private fun contrast(a: Color, b: Color): Double {
        val (hi, lo) = listOf(luminance(a), luminance(b)).sortedDescending()
        return (hi + 0.05) / (lo + 0.05)
    }

    private val colorAttr = Regex("""android:(?:strokeColor|fillColor)="#([0-9A-Fa-f]{6})"""")

    @Test
    fun `every icon color clears 3 to 1 on both surfaces`() {
        val drawables = File("src/main/res/drawable")
            .listFiles { f ->
                (f.name.startsWith("mc_") || f.name.startsWith("mcf_")) && f.extension == "xml"
            }
            .orEmpty()
        assertTrue("no mc_*.xml drawables found — did the import move?", drawables.isNotEmpty())
        assertTrue(
            "the fill set is missing — run tools/import_meteocons.py",
            drawables.any { it.name.startsWith("mcf_") }
        )

        val offenders = drawables.flatMap { file ->
            colorAttr.findAll(file.readText()).map { it.groupValues[1] }.distinct().mapNotNull {
                val color = Color(0xFF000000 or it.toLong(16))
                val vsLight = contrast(color, ChiaroLightScheme.surface)
                val vsDark = contrast(color, ChiaroDarkScheme.surface)
                if (vsLight < 3.0 || vsDark < 3.0) {
                    "${file.name} #$it — %.2f:1 light, %.2f:1 dark".format(vsLight, vsDark)
                } else {
                    null
                }
            }
        }
        assertTrue(
            "an icon color below the 3:1 floor (re-anchor it in tools/import_meteocons.py):\n" +
                offenders.joinToString("\n"),
            offenders.isEmpty()
        )
    }
}
