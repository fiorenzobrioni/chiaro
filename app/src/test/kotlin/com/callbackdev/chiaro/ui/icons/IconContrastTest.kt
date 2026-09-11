package com.callbackdev.chiaro.ui.icons

import androidx.compose.ui.graphics.Color
import com.callbackdev.chiaro.ui.theme.ChiaroDarkScheme
import com.callbackdev.chiaro.ui.theme.ChiaroLightScheme
import com.callbackdev.chiaro.ui.theme.VividDarkScheme
import com.callbackdev.chiaro.ui.theme.VividLightScheme
import java.io.File
import kotlin.math.pow
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The icon strokes are the only carrier of "what kind of weather" in the hour strip, so
 * they are non-text marks under DESIGN.md §10 and owe 3:1 — against the ground their
 * set is picked for (§13.1).
 *
 * **Since Fase 13 no set owes both surfaces.** Until then the line set served every
 * ground, which pinned its colors into Y ∈ [0.120, 0.283] and is why its sun was a
 * bronze; a second line set existed only so the vivid dress could escape that ceiling on
 * dark grounds. v3 ships four sets picked by style and ground — `mc3_`/`mc3n_` for line,
 * `mc3f_`/`mc3fn_` for flat — so each one only ever meets the surface `tools/reanchor.py`
 * measured it against, and the dress no longer chooses an icon at all.
 *
 * Each of the four has an animated twin (§7.1) painted from the same table, and they are
 * swept here too — `AnimatedIconTest` proves twin and original are the same colors, but
 * what ships is the file, and a hand edit to a drawable is exactly what neither of those
 * two guards would catch alone.
 *
 * A light ground is still two surfaces, because there are two dresses (§2.5), and the
 * binding one is whichever is darker: a set measured against one dress's paper and
 * shipped over the other's is exactly the kind of thing that passes review and fails on
 * a device.
 *
 * This sweeps the emitted XML rather than the tool's table, for the same reason
 * `PaletteContrastTest` asserts the outcome instead of trusting the method: what ships
 * is the file, and a hand edit to a drawable would slip past a table nobody re-runs.
 *
 * **What this does NOT measure, and it is not an oversight**: the widget's Cielo card.
 * That ground is the scrimmed sky, `#5C6E7B` at its brightest — a mid-tone, Y 0.149,
 * where an ink needs Y ≥ 0.546 or Y ≤ 0.016 to clear 3:1 and neither set qualifies
 * (8 of 8 line colors short, 25 of 37 fill-night ones; measured 7 set 2026). It is a
 * declared exception, argued in DESIGN §13.1 with the three fixes that were weighed
 * and why the committente turned them down — the colored icons are much of what makes
 * that widget worth looking at. A test cannot assert an accepted shortfall, so it says
 * so here instead of silently implying a coverage it does not have.
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
        // Every surface a set can meet, both dresses (§2.5).
        val lightGrounds = listOf(ChiaroLightScheme.surface, VividLightScheme.surface)
        val darkGrounds = listOf(ChiaroDarkScheme.surface, VividDarkScheme.surface)

        // Every set the app ships, and the grounds `ChiaroIcons` can put it on. Longest
        // prefix first, because "mcafn_" is also a "mca…" and only one of them is right.
        val sets = listOf(
            "mc3fan_" to darkGrounds,
            "mc3fa_" to lightGrounds,
            "mc3an_" to darkGrounds,
            "mc3a_" to lightGrounds,
            "mc3fn_" to darkGrounds,
            "mc3f_" to lightGrounds,
            "mc3n_" to darkGrounds,
            "mc3_" to lightGrounds
        )

        val drawables = File("src/main/res/drawable")
            .listFiles { f -> f.extension == "xml" && sets.any { (p, _) -> f.name.startsWith(p) } }
            .orEmpty()
        assertTrue("no mc3_*.xml drawables found — did the import move?", drawables.isNotEmpty())
        sets.forEach { (prefix, _) ->
            assertTrue(
                "the $prefix* set is missing — run tools/import_meteocons_v3.py",
                drawables.any { it.name.startsWith(prefix) }
            )
        }

        val offenders = drawables.flatMap { file ->
            val grounds = sets.first { (prefix, _) -> file.name.startsWith(prefix) }.second
            colorAttr.findAll(file.readText()).map { it.groupValues[1] }.distinct().mapNotNull {
                val color = Color(0xFF000000 or it.toLong(16))
                val worst = grounds.minOf { ground -> contrast(color, ground) }
                if (worst < 3.0) {
                    "${file.name} #$it — %.2f:1 on the worst ground its set is picked for"
                        .format(worst)
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
