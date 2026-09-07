package com.callbackdev.chiaro.ui.theme

import androidx.compose.material3.ColorScheme
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
 *
 * Since the second dress arrived (§2.5, §3.7) every check is **scoped to a section**.
 * The document now prints two verdict tables, two sets of three ramps and two band
 * tables of exactly the same shape, so a sweep of the whole file would happily measure
 * the vivid ramp against the paper Kotlin and pass. The section a claim lives in is part
 * of the claim.
 */
class PaletteDocTest {

    private val design: String by lazy {
        val file = File("../DESIGN.md")
        assertTrue("no DESIGN.md at ${file.absolutePath}", file.isFile)
        file.readText()
    }

    /** One numbered subsection, heading included, up to the next heading of any depth. */
    private fun section(number: String): String {
        val start = design.indexOf("\n### $number ")
        assertTrue("DESIGN.md no longer has a §$number", start >= 0)
        val rest = design.substring(start + 1)
        val end = Regex("""\n#{2,3} """).find(rest, startIndex = 1)?.range?.first ?: rest.length
        return rest.substring(0, end)
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

    // The two dresses, each with the section that documents its color and the section
    // that documents its sky. Adding a third palette means adding a row here and a
    // section there, and nothing else in this file.
    private data class Documented(
        val name: String,
        val colorSection: String,
        val skySection: String,
        val light: ColorScheme,
        val dark: ColorScheme,
        val lightColors: ChiaroColors,
        val darkColors: ChiaroColors,
        val sky: SkyPalette
    )

    private val documented = listOf(
        Documented(
            "paper", "2.2", "3.2", ChiaroLightScheme, ChiaroDarkScheme,
            ChiaroLightColors, ChiaroDarkColors, SkyPalette.Paper
        ),
        Documented(
            "vivid", "2.5", "3.7", VividLightScheme, VividDarkScheme,
            VividLightColors, VividDarkColors, SkyPalette.Vivid
        )
    )

    /** §2.3 holds the paper ramps and verdicts; §2.5 holds the vivid ones next to its
     * own roles. So the semantic tokens are looked up one section over for paper. */
    private fun semanticSection(dress: Documented) =
        if (dress.name == "paper") section("2.3") else section(dress.colorSection)

    @Test
    fun `the two roles each color section names are the generated ones`() {
        documented.forEach { dress ->
            val text = section(dress.colorSection)
            val surface = Regex("""^\| `surface` \| `(#[0-9A-Fa-f]{6})` \| `(#[0-9A-Fa-f]{6})` \|""", RegexOption.MULTILINE)
                .find(text)
            val primary = Regex("""^\| `primary` \| `(#[0-9A-Fa-f]{6})` \| `(#[0-9A-Fa-f]{6})` \|""", RegexOption.MULTILINE)
                .find(text)
            requireNotNull(surface) { "§${dress.colorSection} no longer names the two surfaces" }
            requireNotNull(primary) { "§${dress.colorSection} no longer names the two primaries" }
            assertEquals("${dress.name} light surface", surface.groupValues[1].uppercase(), hex(dress.light.surface))
            assertEquals("${dress.name} dark surface", surface.groupValues[2].uppercase(), hex(dress.dark.surface))
            assertEquals("${dress.name} light primary", primary.groupValues[1].uppercase(), hex(dress.light.primary))
            assertEquals("${dress.name} dark primary", primary.groupValues[2].uppercase(), hex(dress.dark.primary))

            val span = Regex("""surfaces sit ([\d.]+):1 apart""").find(text)
            requireNotNull(span) { "§${dress.colorSection} no longer prints the span between the surfaces" }
            assertEquals(
                "the span §${dress.colorSection} prints", span.groupValues[1],
                printed(contrast(dress.light.surface, dress.dark.surface))
            )
        }
    }

    @Test
    fun `each verdict table is that dress's verdict palette`() {
        documented.forEach { dress ->
            val row = Regex(
                """^\| (pass|unstable|fail|unknown) \| `(#[0-9A-Fa-f]{6})` ([\d.]+):1 \| `(#[0-9A-Fa-f]{6})` """ +
                    """\| `(#[0-9A-Fa-f]{6})` ([\d.]+):1 \| `(#[0-9A-Fa-f]{6})` \|""",
                RegexOption.MULTILINE
            )
            val rows = row.findAll(semanticSection(dress)).toList()
            assertEquals("${dress.name} should print four verdicts", 4, rows.size)

            rows.forEach { match ->
                val g = match.groupValues
                val name = g[1]
                val light = verdict(dress.lightColors, name)
                val dark = verdict(dress.darkColors, name)
                assertEquals("${dress.name} $name light ink", g[2].uppercase(), hex(light.ink))
                assertEquals("${dress.name} $name light container", g[4].uppercase(), hex(light.container))
                assertEquals("${dress.name} $name dark ink", g[5].uppercase(), hex(dark.ink))
                assertEquals("${dress.name} $name dark container", g[7].uppercase(), hex(dark.container))
                assertEquals("${dress.name} $name light ratio", g[3], printed(contrast(light.ink, dress.light.surface)))
                assertEquals("${dress.name} $name dark ratio", g[6], printed(contrast(dark.ink, dress.dark.surface)))
            }
        }
    }

    private fun verdict(colors: ChiaroColors, name: String) = when (name) {
        "pass" -> colors.pass
        "unstable" -> colors.unstable
        "fail" -> colors.fail
        else -> colors.unknown
    }

    @Test
    fun `the three ramps each dress prints are that dress's three ramps`() {
        documented.forEach { dress ->
            val text = semanticSection(dress)
            val fill = Regex(
                """```\nlight  ((?:#[0-9A-Fa-f]{6} +){5})Y[^\n]*\ndark   ((?:#[0-9A-Fa-f]{6} +){5})Y"""
            ).find(text)
            requireNotNull(fill) { "${dress.name} no longer prints the rain FILL ramp" }
            assertEquals("${dress.name} rain fill, light", fill.groupValues[1].hexes(), dress.lightColors.rainRamp.map(::hex))
            assertEquals("${dress.name} rain fill, dark", fill.groupValues[2].hexes(), dress.darkColors.rainRamp.map(::hex))

            val ink = Regex(
                """```\nlight  ((?:#[0-9A-Fa-f]{6} +){5})([\d. ]+): 1\ndark   ((?:#[0-9A-Fa-f]{6} +){5})([\d. ]+): 1"""
            ).find(text)
            requireNotNull(ink) { "${dress.name} no longer prints the rain INK ramp" }
            assertEquals("${dress.name} rain ink, light", ink.groupValues[1].hexes(), dress.lightColors.rainInkRamp.map(::hex))
            assertEquals("${dress.name} rain ink, dark", ink.groupValues[3].hexes(), dress.darkColors.rainInkRamp.map(::hex))
            // This is the ramp whose NUMBERS are the point of it — the document prints a
            // ratio per step, because the bug it exists for was a step nobody had
            // measured. So every step's ratio is asserted, not just the ends.
            assertEquals(
                "${dress.name} rain ink ratios, light", ink.groupValues[2].trim().split(Regex(" +")),
                dress.lightColors.rainInkRamp.map { printed(contrast(it, dress.light.surface)) }
            )
            assertEquals(
                "${dress.name} rain ink ratios, dark", ink.groupValues[4].trim().split(Regex(" +")),
                dress.darkColors.rainInkRamp.map { printed(contrast(it, dress.dark.surface)) }
            )

            val temperature = Regex(
                """```\nlight  ((?:#[0-9A-Fa-f]{6} +){6}#[0-9A-Fa-f]{6})\ndark   ((?:#[0-9A-Fa-f]{6} +){6}#[0-9A-Fa-f]{6})\n```"""
            ).find(text)
            requireNotNull(temperature) { "${dress.name} no longer prints the temperature ramp" }
            assertEquals(
                "${dress.name} temperature, light",
                temperature.groupValues[1].hexes(), dress.lightColors.temperatureRamp.map(::hex)
            )
            assertEquals(
                "${dress.name} temperature, dark",
                temperature.groupValues[2].hexes(), dress.darkColors.temperatureRamp.map(::hex)
            )
        }
    }

    /** Each dress says the fill ramp cannot carry text, and prints the two ratios that
     * say so — measured against ITS surface, which is the point of printing them twice. */
    @Test
    fun `the reason the fill ramp never paints a figure is still the reason`() {
        documented.forEach { dress ->
            val claim = Regex("""its light end is ([\d.]+):1 and its middle ([\d.]+):1""")
                .find(semanticSection(dress))
            requireNotNull(claim) { "${dress.name} no longer prints why the fill ramp cannot carry text" }
            val paper = dress.light.surface
            assertEquals(
                "${dress.name}: the light end", claim.groupValues[1],
                printed2(contrast(dress.lightColors.rainRamp[0], paper))
            )
            assertEquals(
                "${dress.name}: the middle", claim.groupValues[2],
                printed2(contrast(dress.lightColors.rainRamp[2], paper))
            )
        }
    }

    @Test
    fun `each band table is that dress's canvas`() {
        documented.forEach { dress ->
            val row = Regex(
                """^\| [^|]+ \| [^|]+ \| `(#[0-9A-Fa-f]{6})` \| `(#[0-9A-Fa-f]{6})` \| `(#[0-9A-Fa-f]{6})` \|""",
                RegexOption.MULTILINE
            )
            val printedBands = row.findAll(section(dress.skySection))
                .map { it.groupValues.drop(1).map(String::uppercase) }
                .toList()
            assertEquals("§${dress.skySection} should print eight bands", 8, printedBands.size)

            // Eight rows in the document, nine anchors in the canvas: Day is pinned twice
            // (90° and 12°) so everything above 12° is flat daylight rather than a slow
            // climb to a brighter blue nobody has ever seen.
            val drawn = dress.sky.anchors.map { (_, g) -> listOf(hex(g.top), hex(g.mid), hex(g.bottom)) }
            printedBands.forEach { band ->
                assertTrue("§${dress.skySection} prints $band, which the canvas does not draw", band in drawn)
            }
            drawn.toSet().forEach { band ->
                assertTrue("the ${dress.name} canvas draws $band, which §${dress.skySection} does not print", band in printedBands)
            }
        }
    }

    @Test
    fun `the moon lift target of section 3 4 is the one the canvas mixes`() {
        val printedTarget = Regex("""lifted toward `(#[0-9A-Fa-f]{6})`""").find(section("3.4"))
        requireNotNull(printedTarget) { "§3.4 no longer prints the moon's lift target" }
        assertEquals(
            "the value §3.4 prints",
            printedTarget.groupValues[1].uppercase(), hex(SkyPalette.Moonlight)
        )
    }

    @Test
    fun `the scrim contract of section 3 6 is the scrim the canvas paints`() {
        val text = section("3.6")
        val rgba = Regex("""`rgba\((\d+),\s*(\d+),\s*(\d+),\s*([\d.]+)\)`""").find(text)
        requireNotNull(rgba) { "§3.6 no longer prints the scrim" }
        val g = rgba.groupValues
        assertEquals(
            "the scrim color §3.6 prints",
            "#%02X%02X%02X".format(g[1].toInt(), g[2].toInt(), g[3].toInt()), hex(SkyPalette.ScrimColor)
        )
        assertEquals("the scrim alpha §3.6 prints", g[4].toFloat(), SkyPalette.ScrimAlpha, 1e-6f)

        val brightest = Regex("""brightest stop `(#[0-9A-Fa-f]{6})`""").find(text)
        requireNotNull(brightest) { "§3.6 no longer names the brightest stop" }
        assertEquals(
            "the stop §3.6 measures against",
            brightest.groupValues[1].uppercase(), hex(SkyPalette.Paper.brightestBottomStop())
        )

        val claim = Regex("""white on the scrimmed band is ([\d.]+):1""").find(text)
        requireNotNull(claim) { "§3.6 no longer prints the measured ratio" }
        val measured = contrast(Color.White, over(SkyPalette.Paper.brightestBottomStop()))
        assertEquals(
            "§3.6 claims ${claim.groupValues[1]}:1, the palette measures %.2f:1".format(measured),
            claim.groupValues[1].toDouble(), measured, 0.005
        )
    }

    /** §3.7's whole reason for printing numbers of its own: the scrim composites per
     * channel, so the vivid sky's ratio is not paper's ratio and had to be measured. */
    @Test
    fun `the vivid scrim numbers of section 3 7 are the vivid canvas`() {
        val text = section("3.7")
        val brightest = Regex("""brightest stop `(#[0-9A-Fa-f]{6})` is \*\*([\d.]+):1\*\*""").find(text)
        requireNotNull(brightest) { "§3.7 no longer prints the vivid scrim measurement" }
        assertEquals(
            "the stop §3.7 measures against",
            brightest.groupValues[1].uppercase(), hex(SkyPalette.Vivid.brightestBottomStop())
        )
        assertEquals(
            "§3.7's measured ratio", brightest.groupValues[2].toDouble(),
            contrast(Color.White, over(SkyPalette.Vivid.brightestBottomStop())), 0.005
        )
        assertRejectedAlphas(text, SkyPalette.Vivid.brightestBottomStop(), "§3.7")
    }

    /** The two alphas §3.6 rejects are rejected for the numbers it prints. */
    @Test
    fun `the alphas section 3 6 turns down are turned down for measured reasons`() {
        assertRejectedAlphas(section("3.6"), SkyPalette.Paper.brightestBottomStop(), "§3.6")
    }

    private fun assertRejectedAlphas(text: String, stop: Color, where: String) {
        val rejected = Regex("""([\d.]+) gives ([\d.]+):1""").findAll(text).toList()
        assertEquals("$where should turn down two alphas", 2, rejected.size)
        rejected.forEach { match ->
            val alpha = match.groupValues[1].toFloat()
            val ratio = contrast(Color.White, over(stop, alpha))
            assertTrue(
                "$where says alpha $alpha gives ${match.groupValues[2]}:1; it gives %.2f:1".format(ratio),
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
