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
 * they are non-text marks under DESIGN.md §10 and owe 3:1 — against the ground their
 * set is picked for (§13.1): the line set serves both themes and owes both surfaces;
 * the fill set ships twice, mcf_* for light grounds and mcfn_* for dark ones, and each
 * owes only the surface `ChiaroIcons` will ever put it on. Meteocons' own palette does
 * not clear the light surface (its cloud gray is 1.18:1 there), which is why mcf_* is
 * re-anchored and mcfn_* is the original palette on the backdrop it was drawn for.
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
    fun `every icon color clears 3 to 1 on the ground its set is picked for`() {
        val drawables = File("src/main/res/drawable")
            .listFiles { f ->
                (f.name.startsWith("mc_") || f.name.startsWith("mcf_") ||
                    f.name.startsWith("mcfn_")) && f.extension == "xml"
            }
            .orEmpty()
        assertTrue("no mc_*.xml drawables found — did the import move?", drawables.isNotEmpty())
        assertTrue(
            "the fill set is missing — run tools/import_meteocons.py",
            drawables.any { it.name.startsWith("mcf_") && !it.name.startsWith("mcfn_") }
        )
        assertTrue(
            "the fill-night set is missing — run tools/import_meteocons.py",
            drawables.any { it.name.startsWith("mcfn_") }
        )

        val offenders = drawables.flatMap { file ->
            val night = file.name.startsWith("mcfn_")
            val fillLight = !night && file.name.startsWith("mcf_")
            colorAttr.findAll(file.readText()).map { it.groupValues[1] }.distinct().mapNotNull {
                val color = Color(0xFF000000 or it.toLong(16))
                val vsLight = contrast(color, ChiaroLightScheme.surface)
                val vsDark = contrast(color, ChiaroDarkScheme.surface)
                val fails = when {
                    night -> vsDark < 3.0
                    fillLight -> vsLight < 3.0
                    else -> vsLight < 3.0 || vsDark < 3.0
                }
                if (fails) {
                    "${file.name} #$it — %.2f:1 light, %.2f:1 dark".format(vsLight, vsDark)
                } else {
                    null
                }
            }
        }
        assertTrue(
            "an icon color below the 3:1 floor for its ground (tools/import_meteocons.py):\n" +
                offenders.joinToString("\n"),
            offenders.isEmpty()
        )
    }
}
