package com.callbackdev.chiaro.ui.theme

import androidx.compose.ui.graphics.Color
import com.callbackdev.chiaro.data.AppPalette
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

/**
 * DESIGN.md §2.3 and §10 print numbers; this is what stops them from becoming
 * decoration. Every ratio in the document is asserted here, so re-picking a token
 * without re-measuring it fails the build instead of the reader's eyes.
 *
 * Every assertion runs over EVERY dress (§2.5). The vivid palette was derived from the
 * paper one at held luminance, so all of this should hold for it by construction; a
 * palette that is only correct by construction is a palette nobody has measured, which
 * is exactly the failure mode this file exists for.
 */
class PaletteContrastTest {

    /** The two dresses, named, so a failure says which one broke. */
    private val dresses = ChiaroPalettes.map { (choice, dress) -> choice.name.lowercase() to dress }

    private fun channel(c: Float) =
        if (c <= 0.03928f) c / 12.92 else ((c + 0.055) / 1.055).toDouble().pow(2.4)

    private fun luminance(color: Color) =
        0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)

    private fun contrast(a: Color, b: Color): Double {
        val (hi, lo) = listOf(luminance(a), luminance(b)).sortedDescending()
        return (hi + 0.05) / (lo + 0.05)
    }

    private fun assertAtLeast(expected: Double, a: Color, b: Color, what: String) {
        val actual = contrast(a, b)
        assertTrue("$what is %.2f:1, below $expected:1".format(actual), actual >= expected)
    }

    private fun ChiaroColors.verdicts() = listOf(
        "pass" to pass, "unstable" to unstable, "fail" to fail, "unknown" to unknown
    )

    /** The three official-warning levels of §2.3, in severity order (Fase 11). */
    private fun ChiaroColors.levels() = listOf(
        "yellow" to warningYellow, "orange" to warningOrange, "red" to warningRed
    )

    /** Every ink/container pair the palette holds — the four verdicts and the three
     * levels are the same shape and must clear the same floors. */
    private fun ChiaroColors.pairs() = verdicts() + levels()

    /** `paletteFor` looks a dress up by enum value and throws if there is not one. A
     * palette added to the store without one is a crash on the first frame, so the map
     * is checked against the enum rather than against the two entries it happens to
     * have. */
    @Test
    fun `every palette the reader can choose has a dress`() {
        AppPalette.entries.forEach { choice ->
            assertTrue("no dress for $choice", ChiaroPalettes.containsKey(choice))
            val dress = paletteFor(choice)
            assertTrue("$choice: light and dark must not be the same scheme",
                dress.lightScheme.surface != dress.darkScheme.surface)
            assertTrue("$choice: scheme(dark) must pick the dark one",
                dress.scheme(true) == dress.darkScheme && dress.scheme(false) == dress.lightScheme)
            assertTrue("$choice: colors(dark) must pick the dark set",
                dress.colors(true) == dress.darkColors && dress.colors(false) == dress.lightColors)
        }
        assertTrue(
            "the two dresses must not share a sky",
            paletteFor(AppPalette.PAPER).sky !== paletteFor(AppPalette.VIVID).sky
        )
    }

    @Test
    fun `verdict ink reads on its surface in every scheme`() {
        dresses.forEach { (dress, palette) ->
            palette.lightColors.pairs().forEach { (name, v) ->
                assertAtLeast(4.5, v.ink, palette.lightScheme.surface, "$dress light $name ink")
            }
            palette.darkColors.pairs().forEach { (name, v) ->
                assertAtLeast(4.5, v.ink, palette.darkScheme.surface, "$dress dark $name ink")
            }
        }
    }

    @Test
    fun `verdict ink reads on its own container`() {
        dresses.forEach { (dress, palette) ->
            listOf(palette.lightColors, palette.darkColors).forEach { colors ->
                colors.pairs().forEach { (name, v) ->
                    assertAtLeast(4.5, v.ink, v.container, "$dress $name ink on container")
                }
            }
        }
    }

    /**
     * §2.3's claim about the three levels, and the reason a banner never says a level in
     * colour alone: under a deuteranope simulation ONE of the two carriers collapses in
     * each scheme — the ink on paper, the container in the dark — keeping under a tenth
     * of the distance a full-colour reader gets from the same pair.
     *
     * A fraction and not an absolute floor, because the absolute number is not the point:
     * `#5C4700` and `#763900` are 21.2 apart in full colour and 1.6 apart to a
     * deuteranope, and it is the ratio that says "this carrier stopped carrying".
     */
    @Test
    fun `in every scheme one of a level's two carriers collapses under deuteranopia`() {
        dresses.forEach { (dress, palette) ->
            listOf(
                "$dress light ink" to palette.lightColors.levels().map { it.second.ink },
                "$dress dark container" to palette.darkColors.levels().map { it.second.container }
            ).forEach { (what, colors) ->
                colors.indices.forEach { i ->
                    colors.indices.drop(i + 1).forEach { j ->
                        val full = Deuteranopia.deltaE(colors[i], colors[j])
                        val left = Deuteranopia.separation(colors[i], colors[j])
                        assertTrue(
                            "$what keeps %.0f%% of %.1f between two levels: colour would be a carrier"
                                .format(100 * left / full, full),
                            left / full < 0.12
                        )
                    }
                }
            }
        }
    }

    /** The other half of the same measurement: with normal colour vision the three ARE
     * three colours, so the palette is not simply giving up on colour. */
    @Test
    fun `the three levels are three colours to a reader who sees all of them`() {
        sets().forEach { (name, colors, _) ->
            colors.levels().map { it.second }.zipWithNext().forEach { (a, b) ->
                assertTrue(
                    "$name: two level containers are indistinguishable even in full colour",
                    Deuteranopia.deltaE(a.container, b.container) > 10.0
                )
            }
        }
    }

    @Test
    fun `the two surfaces are as far apart as the document says`() {
        dresses.forEach { (dress, palette) ->
            assertAtLeast(
                17.0, palette.lightScheme.surface, palette.darkScheme.surface,
                "$dress surface span"
            )
        }
    }

    @Test
    fun `body text reads on every surface container`() {
        schemes().forEach { (name, s) ->
            listOf(
                s.surface, s.surfaceContainerLowest, s.surfaceContainerLow,
                s.surfaceContainer, s.surfaceContainerHigh, s.surfaceContainerHighest
            ).forEach { assertAtLeast(4.5, s.onSurface, it, "$name onSurface over a container") }
        }
    }

    @Test
    fun `the secondary text role still reads, which is where a generated scheme usually fails`() {
        schemes().forEach { (name, s) ->
            assertAtLeast(4.5, s.onSurfaceVariant, s.surface, "$name onSurfaceVariant")
        }
    }

    /** Every generated scheme there is, named. */
    private fun schemes() = dresses.flatMap { (dress, palette) ->
        listOf("$dress light" to palette.lightScheme, "$dress dark" to palette.darkScheme)
    }

    @Test
    fun `on-color roles read on the color they are named for`() {
        schemes().forEach { (_, s) ->
            assertAtLeast(4.5, s.onPrimary, s.primary, "onPrimary")
            assertAtLeast(4.5, s.onSecondary, s.secondary, "onSecondary")
            assertAtLeast(4.5, s.onTertiary, s.tertiary, "onTertiary")
            assertAtLeast(4.5, s.onError, s.error, "onError")
            assertAtLeast(4.5, s.onPrimaryContainer, s.primaryContainer, "onPrimaryContainer")
            assertAtLeast(4.5, s.onSecondaryContainer, s.secondaryContainer, "onSecondaryContainer")
            assertAtLeast(4.5, s.onTertiaryContainer, s.tertiaryContainer, "onTertiaryContainer")
            assertAtLeast(4.5, s.onErrorContainer, s.errorContainer, "onErrorContainer")
        }
    }

    /** Every (semantic set, its surface) pair there is, named. */
    private fun sets() = dresses.flatMap { (dress, palette) ->
        listOf(
            Triple("$dress light", palette.lightColors, palette.lightScheme.surface),
            Triple("$dress dark", palette.darkColors, palette.darkScheme.surface)
        )
    }

    @Test
    fun `the rain ramp is one hue, light to dark, with no step that repeats`() {
        sets().forEach { (name, colors, _) ->
            val ys = colors.rainRamp.map(::luminance)
            val descending = ys.zipWithNext().all { (a, b) -> a > b }
            val ascending = ys.zipWithNext().all { (a, b) -> a < b }
            assertTrue("the $name rain ramp is not monotonic: $ys", descending || ascending)
        }
    }

    @Test
    fun `the rain INK ramp is one hue, monotonic, and reads at every step`() {
        sets().map { (_, colors, surface) -> colors.rainInkRamp to surface }.forEach { (ramp, surface) ->
            val ys = ramp.map(::luminance)
            val descending = ys.zipWithNext().all { (a, b) -> a > b }
            val ascending = ys.zipWithNext().all { (a, b) -> a < b }
            assertTrue("the rain ink ramp is not monotonic: $ys", descending || ascending)
            ramp.forEachIndexed { i, ink -> assertAtLeast(4.5, ink, surface, "rain ink step $i") }
        }
    }

    @Test
    fun `a printed probability reads at every value, zero included`() {
        // The bug this ramp exists for: the FILL ramp is a fill, and painting a figure
        // with its light end put 15% on paper at 1.3:1 while 0% fell back to the
        // secondary text role and became the heaviest number in the row.
        sets().forEach { (_, colors, surface) ->
            (0..100 step 5).forEach { pct ->
                assertAtLeast(4.5, colors.rainInkAt(pct), surface, "the ink of a probability of $pct")
            }
            // Quiet at the bottom, loud at the top: the scale still carries the quantity.
            assertTrue(
                "0% must not out-shout 100%",
                contrast(colors.rainInkAt(0), surface) < contrast(colors.rainInkAt(100), surface)
            )
            // And it is the same ink at 0% as just above it: no step, no second color.
            assertTrue(
                "0% must sit on the ramp its neighbours are on",
                colors.rainInkAt(0) == colors.rainInkRamp.first()
            )
        }
    }

    @Test
    fun `the temperature ramp peaks at its neutral middle, and troughs at it in dark`() {
        dresses.forEach { (dress, palette) ->
            val light = palette.lightColors.temperatureRamp.map(::luminance)
            assertTrue("the $dress light ramp should be lightest in the middle: $light",
                light.indexOf(light.max()) == 3)
            val dark = palette.darkColors.temperatureRamp.map(::luminance)
            assertTrue("the $dress dark ramp should be darkest in the middle: $dark",
                dark.indexOf(dark.min()) == 3)
        }
    }

    /**
     * The diverging ramp's midpoint is the one token that must NOT gain color: two hues
     * and a neutral is the design, and a saturated middle makes it a rainbow (§9.1).
     * This is what the vivid palette's chroma ceiling is for, so it is measured.
     */
    @Test
    fun `the temperature ramp's middle stays a neutral in every dress`() {
        sets().forEach { (name, colors, _) ->
            val middle = colors.temperatureRamp[3]
            val spread = listOf(middle.red, middle.green, middle.blue).let { it.max() - it.min() }
            assertTrue("the $name midpoint has a hue: $middle (spread %.3f)".format(spread),
                spread < 0.16f)
        }
    }

    @Test
    fun `the temperature ramp is anchored to the world`() {
        sets().forEach { (name, colors, _) ->
            // 15 C is the middle step, and it stays the middle step whatever is on screen.
            assertTrue("$name: 15 C must sample the neutral step",
                colors.temperatureAt(ChiaroColors.ANCHOR_MID) == colors.temperatureRamp[3])
            assertTrue("$name: below the floor clamps",
                colors.temperatureAt(-40.0) == colors.temperatureRamp.first())
            assertTrue("$name: above the ceiling clamps",
                colors.temperatureAt(60.0) == colors.temperatureRamp.last())
        }
    }

    /**
     * §2.5's actual claim: the vivid palette is the paper one re-picked at the gamut
     * edge with the luminance HELD. Holding luminance is what carries every ratio above
     * from one dress to the other, so it is asserted directly rather than inferred.
     */
    @Test
    fun `the vivid palette holds the paper palette's luminances`() {
        listOf(
            "light" to (ChiaroLightColors to VividLightColors),
            "dark" to (ChiaroDarkColors to VividDarkColors)
        ).forEach { (mode, pair) ->
            val (paper, vivid) = pair
            val tokens = { c: ChiaroColors ->
                c.pairs().flatMap { (_, v) -> listOf(v.ink, v.container) } +
                    c.rainRamp + c.rainInkRamp + c.temperatureRamp
            }
            val before = tokens(paper)
            val after = tokens(vivid)
            assertTrue("$mode: the two sets must have the same tokens", before.size == after.size)
            before.zip(after).forEachIndexed { i, (a, b) ->
                val drift = kotlin.math.abs(luminance(a) - luminance(b))
                assertTrue(
                    "$mode token $i moved its luminance by %.4f ($a -> $b)".format(drift),
                    drift < 0.005
                )
            }
            assertTrue("$mode: the vivid set must not BE the paper set", before != after)
        }
    }
}
