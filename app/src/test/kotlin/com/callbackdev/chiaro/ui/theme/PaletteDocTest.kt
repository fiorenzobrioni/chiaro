package com.callbackdev.chiaro.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import java.io.File
import java.util.Locale
import kotlin.math.abs
import kotlin.math.pow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `PaletteContrastTest` proves the palette is legible. This proves it is the palette
 * **DESIGN.md says it is** — a different claim, and the one that was quietly false.
 *
 * The Fase 9 audit read §2.2, §2.3, §3.2, §3.4 and §3.6 against `Scheme`, `ChiaroColors`
 * and `SkyPalette` and found the moon's lift target still printing its pre-color-pass
 * value: `#2A3550` in the document, `#273458` in the canvas since 3 set 2026. Nothing
 * failed, because nothing was looking. Every other hex and every printed ratio matched,
 * so the color pass really was done — but "was done" is something you can only say after
 * looking, and this file is what looks from now on.
 *
 * It reads the document rather than a copy of it, for the same reason
 * `tools/palette_sheet.py` reads the Kotlin rather than a copy of it: two lists of the
 * same colors is one list and one lie waiting.
 */
class PaletteDocTest {

    private val design: String by lazy {
        val file = File("../DESIGN.md")
        assertTrue("no DESIGN.md at ${file.absolutePath}", file.isFile)
        file.readText()
    }

    private fun hex(color: Color): String = "#%06X".format(color.toArgb() and 0xFFFFFF)

    private fun channel(c: Float) =
        if (c <= 0.03928f) c / 12.92 else ((c + 0.055) / 1.055).toDouble().pow(2.4)

    private fun luminance(color: Color) =
        0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)

    private fun contrast(a: Color, b: Color): Double {
        val (hi, lo) = listOf(luminance(a), luminance(b)).sortedDescending()
        return (hi + 0.05) / (lo + 0.05)
    }

    /** A ratio rounded the way the document prints it. */
    private fun printed(ratio: Double) = String.format(Locale.ROOT, "%.1f", ratio)

    private fun printed2(ratio: Double) = String.format(Locale.ROOT, "%.2f", ratio)

    private fun String.hexes(): List<String> =
        Regex("#[0-9A-Fa-f]{6}").findAll(this).map { it.value.uppercase() }.toList()

    @Test
    fun `the two roles section 2 2 names are the generated ones`() {
        val surface = Regex("""^\| `surface` \| `(#[0-9A-Fa-f]{6})` \| `(#[0-9A-Fa-f]{6})` \|""", RegexOption.MULTILINE)
            .find(design)
        val primary = Regex("""^\| `primary` \| `(#[0-9A-Fa-f]{6})` \| `(#[0-9A-Fa-f]{6})` \|""", RegexOption.MULTILINE)
            .find(design)
        requireNotNull(surface) { "§2.2 no longer names the two surfaces" }
        requireNotNull(primary) { "§2.2 no longer names the two primaries" }
        assertEquals("light surface", surface.groupValues[1].uppercase(), hex(ChiaroLightScheme.surface))
        assertEquals("dark surface", surface.groupValues[2].uppercase(), hex(ChiaroDarkScheme.surface))
        assertEquals("light primary", primary.groupValues[1].uppercase(), hex(ChiaroLightScheme.primary))
        assertEquals("dark primary", primary.groupValues[2].uppercase(), hex(ChiaroDarkScheme.primary))

        val span = Regex("""two surfaces sit ([\d.]+):1 apart""").find(design)
        requireNotNull(span) { "§2.2 no longer prints the span between the surfaces" }
        assertEquals(
            "the span §2.2 prints", span.groupValues[1],
            printed(contrast(ChiaroLightScheme.surface, ChiaroDarkScheme.surface))
        )
    }

    @Test
    fun `the verdict table of section 2 3 is the verdict palette`() {
        val row = Regex(
            """^\| (pass|unstable|fail|unknown) \| `(#[0-9A-Fa-f]{6})` ([\d.]+):1 \| `(#[0-9A-Fa-f]{6})` """ +
                """\| `(#[0-9A-Fa-f]{6})` ([\d.]+):1 \| `(#[0-9A-Fa-f]{6})` \|""",
            RegexOption.MULTILINE
        )
        val rows = row.findAll(design).toList()
        assertEquals("§2.3 should print four verdicts", 4, rows.size)

        rows.forEach { match ->
            val g = match.groupValues
            val name = g[1]
            val light = verdict(ChiaroLightColors, name)
            val dark = verdict(ChiaroDarkColors, name)
            assertEquals("$name light ink", g[2].uppercase(), hex(light.ink))
            assertEquals("$name light container", g[4].uppercase(), hex(light.container))
            assertEquals("$name dark ink", g[5].uppercase(), hex(dark.ink))
            assertEquals("$name dark container", g[7].uppercase(), hex(dark.container))
            assertEquals("$name light ratio", g[3], printed(contrast(light.ink, ChiaroLightScheme.surface)))
            assertEquals("$name dark ratio", g[6], printed(contrast(dark.ink, ChiaroDarkScheme.surface)))
        }
    }

    private fun verdict(colors: ChiaroColors, name: String) = when (name) {
        "pass" -> colors.pass
        "unstable" -> colors.unstable
        "fail" -> colors.fail
        else -> colors.unknown
    }

    @Test
    fun `the three ramps of section 2 3 are the three ramps`() {
        val fill = Regex(
            """```\nlight  ((?:#[0-9A-Fa-f]{6} +){5})Y[^\n]*\ndark   ((?:#[0-9A-Fa-f]{6} +){5})Y"""
        ).find(design)
        requireNotNull(fill) { "§2.3 no longer prints the rain FILL ramp" }
        assertEquals("rain fill, light", fill.groupValues[1].hexes(), ChiaroLightColors.rainRamp.map(::hex))
        assertEquals("rain fill, dark", fill.groupValues[2].hexes(), ChiaroDarkColors.rainRamp.map(::hex))

        val ink = Regex(
            """```\nlight  ((?:#[0-9A-Fa-f]{6} +){5})([\d. ]+): 1\ndark   ((?:#[0-9A-Fa-f]{6} +){5})([\d. ]+): 1"""
        ).find(design)
        requireNotNull(ink) { "§2.3 no longer prints the rain INK ramp" }
        assertEquals("rain ink, light", ink.groupValues[1].hexes(), ChiaroLightColors.rainInkRamp.map(::hex))
        assertEquals("rain ink, dark", ink.groupValues[3].hexes(), ChiaroDarkColors.rainInkRamp.map(::hex))
        // This is the ramp whose NUMBERS are the point of it — §2.3 prints a ratio per
        // step, because the bug it exists for was a step nobody had measured. So every
        // step's ratio is asserted, not just the ends.
        assertEquals(
            "rain ink ratios, light", ink.groupValues[2].trim().split(Regex(" +")),
            ChiaroLightColors.rainInkRamp.map { printed(contrast(it, ChiaroLightScheme.surface)) }
        )
        assertEquals(
            "rain ink ratios, dark", ink.groupValues[4].trim().split(Regex(" +")),
            ChiaroDarkColors.rainInkRamp.map { printed(contrast(it, ChiaroDarkScheme.surface)) }
        )

        val temperature = Regex(
            """```\nlight  ((?:#[0-9A-Fa-f]{6} +){6}#[0-9A-Fa-f]{6})\ndark   ((?:#[0-9A-Fa-f]{6} +){6}#[0-9A-Fa-f]{6})\n```"""
        ).find(design)
        requireNotNull(temperature) { "§2.3 no longer prints the temperature ramp" }
        assertEquals(
            "temperature, light",
            temperature.groupValues[1].hexes(), ChiaroLightColors.temperatureRamp.map(::hex)
        )
        assertEquals(
            "temperature, dark",
            temperature.groupValues[2].hexes(), ChiaroDarkColors.temperatureRamp.map(::hex)
        )
    }

    /** §2.3 says the fill ramp cannot carry text, and prints the two ratios that say so. */
    @Test
    fun `the reason the fill ramp never paints a figure is still the reason`() {
        val claim = Regex("""its light end is ([\d.]+):1 and its middle ([\d.]+):1""").find(design)
        requireNotNull(claim) { "§2.3 no longer prints why the fill ramp cannot carry text" }
        val paper = ChiaroLightScheme.surface
        assertEquals("the light end", claim.groupValues[1], printed2(contrast(ChiaroLightColors.rainRamp[0], paper)))
        assertEquals("the middle", claim.groupValues[2], printed2(contrast(ChiaroLightColors.rainRamp[2], paper)))
    }

    @Test
    fun `the band table of section 3 2 is the canvas`() {
        val row = Regex(
            """^\| [^|]+ \| [^|]+ \| `(#[0-9A-Fa-f]{6})` \| `(#[0-9A-Fa-f]{6})` \| `(#[0-9A-Fa-f]{6})` \|""",
            RegexOption.MULTILINE
        )
        val printedBands = row.findAll(design)
            .map { it.groupValues.drop(1).map(String::uppercase) }
            .toList()
        assertEquals("§3.2 should print eight bands", 8, printedBands.size)

        // Eight rows in the document, nine anchors in the canvas: Day is pinned twice
        // (90° and 12°) so everything above 12° is flat daylight rather than a slow
        // climb to a brighter blue nobody has ever seen.
        val drawn = SkyPalette.anchors.map { (_, g) -> listOf(hex(g.top), hex(g.mid), hex(g.bottom)) }
        printedBands.forEach { band ->
            assertTrue("§3.2 prints $band, which the canvas does not draw", band in drawn)
        }
        drawn.toSet().forEach { band ->
            assertTrue("the canvas draws $band, which §3.2 does not print", band in printedBands)
        }
    }

    @Test
    fun `the moon lift target of section 3 4 is the one the canvas mixes`() {
        val printedTarget = Regex("""lifted toward `(#[0-9A-Fa-f]{6})`""").find(design)
        requireNotNull(printedTarget) { "§3.4 no longer prints the moon's lift target" }
        assertEquals(
            "the value §3.4 prints",
            printedTarget.groupValues[1].uppercase(), hex(SkyPalette.Moonlight)
        )
    }

    @Test
    fun `the scrim contract of section 3 6 is the scrim the canvas paints`() {
        val rgba = Regex("""`rgba\((\d+),\s*(\d+),\s*(\d+),\s*([\d.]+)\)`""").find(design)
        requireNotNull(rgba) { "§3.6 no longer prints the scrim" }
        val g = rgba.groupValues
        assertEquals(
            "the scrim color §3.6 prints",
            "#%02X%02X%02X".format(g[1].toInt(), g[2].toInt(), g[3].toInt()), hex(SkyPalette.ScrimColor)
        )
        assertEquals("the scrim alpha §3.6 prints", g[4].toFloat(), SkyPalette.ScrimAlpha, 1e-6f)

        val brightest = Regex("""brightest stop `(#[0-9A-Fa-f]{6})`""").find(design)
        requireNotNull(brightest) { "§3.6 no longer names the brightest stop" }
        assertEquals(
            "the stop §3.6 measures against",
            brightest.groupValues[1].uppercase(), hex(SkyPalette.brightestBottomStop())
        )

        val claim = Regex("""white on the scrimmed band is ([\d.]+):1""").find(design)
        requireNotNull(claim) { "§3.6 no longer prints the measured ratio" }
        val measured = contrast(Color.White, over(SkyPalette.brightestBottomStop()))
        assertEquals(
            "§3.6 claims ${claim.groupValues[1]}:1, the palette measures %.2f:1".format(measured),
            claim.groupValues[1].toDouble(), measured, 0.005
        )
    }

    /** The two alphas §3.6 rejects are rejected for the numbers it prints. */
    @Test
    fun `the alphas section 3 6 turns down are turned down for measured reasons`() {
        val rejected = Regex("""([\d.]+) gives ([\d.]+):1""").findAll(design).toList()
        assertEquals("§3.6 should turn down two alphas", 2, rejected.size)
        rejected.forEach { match ->
            val alpha = match.groupValues[1].toFloat()
            val ratio = contrast(Color.White, over(SkyPalette.brightestBottomStop(), alpha))
            assertTrue(
                "§3.6 says alpha $alpha gives ${match.groupValues[2]}:1; it gives %.2f:1".format(ratio),
                abs(ratio - match.groupValues[2].toDouble()) < 0.005
            )
        }
    }

    /**
     * The scrim over a sky, the way the brush composites it: SRC_OVER is a mix of the
     * two colors' sRGB VALUES. Not `Color.lerp`, which interpolates in Oklab — a
     * perceptual blend, and the wrong model to measure a compositing rule with. The
     * same function as `ScrimContractTest.scrimmed`, deliberately: §3.6 quoting one
     * number and its guard measuring another is the failure this file exists for.
     */
    private fun over(background: Color, alpha: Float = SkyPalette.ScrimAlpha): Color {
        val scrim = SkyPalette.ScrimColor
        fun mix(a: Float, b: Float) = a * alpha + b * (1 - alpha)
        return Color(
            mix(scrim.red, background.red),
            mix(scrim.green, background.green),
            mix(scrim.blue, background.blue)
        )
    }
}
