package com.callbackdev.chiaro.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import kotlin.math.abs

/**
 * The sky canvas (DESIGN.md §3): a gradient computed from the sky above the active city
 * rather than chosen to look nice.
 *
 * It does not follow the reader's theme, and that is the one deliberate exception to
 * "roles, never hexes": at 23:00 it is dark outside whatever the phone is set to. What
 * makes the exception safe is the scrim contract of §3.6, which `ScrimContractTest`
 * holds — for every band of every table below, not just for the one we think is
 * brightest.
 */
@Immutable
data class SkyGradient(val top: Color, val mid: Color, val bottom: Color) {
    fun stops(): List<Color> = listOf(top, mid, bottom)
}

/**
 * One band table and the rules that mix it. There are two of them ([Paper] and [Vivid],
 * §3.7), because the reader picks a palette and the sky is the loudest thing in it; the
 * mixing is shared, so the two skies can differ in saturation and in nothing else.
 */
@Immutable
class SkyPalette internal constructor(
    /**
     * The rows of DESIGN.md §3.2 as ANCHORS on solar altitude, not as buckets: a value
     * between two anchors is the blend of the two, which is what makes a sunset move
     * instead of snapping through seven states.
     */
    internal val anchors: List<Pair<Double, SkyGradient>>
) {

    /**
     * The canvas for one moment.
     *
     * Order matters and is part of the spec: band, then cloud, then rain, then the moon
     * — with the moon's lift scaled DOWN by the cloud cover, because clouds hide the
     * moon. (Applying the lift after the cloud mix without that scaling would have made
     * an overcast full-moon night brighter than a clear one, which is how the
     * implementation found the hole in the first draft of §3.4.)
     *
     * @param sunAltitudeDeg the sun's altitude, from AstronomyEngine
     * @param cloudPct 0..100
     * @param precipPct 0..100
     * @param moonIllumination 0..1
     * @param moonAltitudeDeg the moon's altitude; below the horizon it contributes nothing
     */
    fun gradient(
        sunAltitudeDeg: Double,
        cloudPct: Int = 0,
        precipPct: Int = 0,
        moonIllumination: Double = 0.0,
        moonAltitudeDeg: Double = -90.0
    ): SkyGradient {
        val base = interpolate(sunAltitudeDeg)
        val cloud = (cloudPct.coerceIn(0, 100) / 100f)
        val clouded = base.map { stop ->
            val grey = stop.red * 0.2126f + stop.green * 0.7152f + stop.blue * 0.0722f
            val flat = lerp(stop, Color(grey, grey, grey, stop.alpha), CloudDesaturation * cloud)
            val dim = 1f - CloudDarkening * cloud
            Color(flat.red * dim, flat.green * dim, flat.blue * dim, flat.alpha)
        }
        val rain = if (precipPct > 50) 1f - 0.25f * ((precipPct.coerceAtMost(100) - 50) / 50f) else 1f
        val rained = clouded.map { Color(it.red * rain, it.green * rain, it.blue * rain, it.alpha) }
        val moonlit = if (sunAltitudeDeg < -6.0 && moonAltitudeDeg > 0.0) {
            val altitude = (moonAltitudeDeg / 40.0).coerceIn(0.0, 1.0)
            val lift = (moonIllumination.coerceIn(0.0, 1.0) * altitude * (1.0 - cloud)).toFloat()
            rained.map { lerp(it, Moonlight, lift) }
        } else {
            rained
        }
        return SkyGradient(moonlit[0], moonlit[1], moonlit[2])
    }

    /** The brightest canvas this palette can produce — what the scrim has to survive. */
    fun brightestBottomStop(): Color = anchors.first().second.bottom

    private fun interpolate(altitude: Double): List<Color> {
        val clamped = altitude.coerceIn(-90.0, 90.0)
        val upper = anchors.last { it.first >= clamped }
        val lower = anchors.first { it.first <= clamped }
        if (upper.first == lower.first) return upper.second.stops()
        val span = upper.first - lower.first
        val t = (if (abs(span) < 1e-9) 0.0 else (upper.first - clamped) / span).toFloat()
        return upper.second.stops().zip(lower.second.stops()) { a, b -> lerp(a, b, t) }
    }

    companion object {

        /**
         * The bands of DESIGN.md §3.2, and the app's default sky.
         *
         * Retuned on the color pass (3 set 2026, raised once more after the device look
         * the same evening): chroma up in OKLCh — ×1.65 for the day sky, tapering to
         * ×1.22 at night — at HELD WCAG luminance, so the scrim contract
         * and every brightness ordering the tests assert survived the retune by
         * construction. The day sky was the whole point: it was the most washed-out
         * thing on the screen it was supposed to be the hero of.
         */
        val Paper = SkyPalette(
            listOf(
                90.0 to SkyGradient(Color(0xFF0090DA), Color(0xFF55B7F0), Color(0xFFBADFF6)),
                12.0 to SkyGradient(Color(0xFF0090DA), Color(0xFF55B7F0), Color(0xFFBADFF6)),
                8.0 to SkyGradient(Color(0xFF3483CA), Color(0xFF80B7DE), Color(0xFFE8CEA3)),
                // TWO golden anchors, not one. With a single anchor at the horizon the
                // golden hour was only golden in its last minutes: at 3° the canvas
                // rendered as the midpoint between a cool low sun and the amber, which
                // is a washed-out tan and is not what anybody means by the golden hour.
                // Rendering the sheet and looking at it is what found this; no test
                // would have.
                4.0 to SkyGradient(Color(0xFF4B7FBB), Color(0xFFF49C04), Color(0xFFFFD083)),
                0.0 to SkyGradient(Color(0xFF4573AF), Color(0xFFE58800), Color(0xFFFFC268)),
                -6.0 to SkyGradient(Color(0xFF203D73), Color(0xFF425DA4), Color(0xFF8D7CB7)),
                -12.0 to SkyGradient(Color(0xFF172449), Color(0xFF27396A), Color(0xFF425187)),
                -18.0 to SkyGradient(Color(0xFF101934), Color(0xFF152143), Color(0xFF202D53)),
                -90.0 to SkyGradient(Color(0xFF0D1323), Color(0xFF111A2D), Color(0xFF182237))
            )
        )

        /**
         * The same nine anchors of §3.7, GENERATED from [Paper] by `tools/gen_vivid.py`:
         * same hue, same WCAG luminance, chroma to the sRGB gamut edge or ×1.8, whichever
         * comes first — and never below 0.65 of what the gamut holds at that band's own
         * luminance, which is the clause the sky has and the semantic tokens do not.
         *
         * The floor was added on 8 set 2026 and it moves the last three rows and nothing
         * else. ×1.8 is a multiple of paper's chroma, and paper drew the night with the
         * least of it, so the multiplier was handing the least to the bands where the
         * gamut has the most left: measured as a fraction of that gamut, the day sky ran
         * at 1.00 and midnight at 0.44. The dress was loudest on the one sky that is
         * already bright. Everything from the blue hour up was above the floor already
         * and comes out unchanged, so §3.7's scrim measurement did not move.
         *
         * Holding the luminance is what makes a second sky cheap rather than frightening.
         * Every claim §3 makes is a claim about brightness — darker after sunset, an
         * overcast midnight is not a dusk, white survives the scrim — and brightness is
         * exactly what this table does not change. `SkyPaletteTest` and
         * `ScrimContractTest` run over both tables anyway, because "by construction" is
         * a thing you say after the test passes.
         *
         * Do not hand-edit: change [Paper], regenerate, and re-measure.
         */
        val Vivid = SkyPalette(
            listOf(
                90.0 to SkyGradient(Color(0xFF0090DA), Color(0xFF2BB8FF), Color(0xFFAFE0FF)),
                12.0 to SkyGradient(Color(0xFF0090DA), Color(0xFF2BB8FF), Color(0xFFAFE0FF)),
                8.0 to SkyGradient(Color(0xFF0082DC), Color(0xFF4BBAFF), Color(0xFFFACA78)),
                4.0 to SkyGradient(Color(0xFF007DE5), Color(0xFFF49C00), Color(0xFFFFD083)),
                0.0 to SkyGradient(Color(0xFF006FDC), Color(0xFFE58800), Color(0xFFFFC268)),
                -6.0 to SkyGradient(Color(0xFF003698), Color(0xFF2C52DB), Color(0xFF9571DE)),
                -12.0 to SkyGradient(Color(0xFF0D1D65), Color(0xFF172F95), Color(0xFF3445C0)),
                -18.0 to SkyGradient(Color(0xFF09144B), Color(0xFF0C1B5E), Color(0xFF142479)),
                -90.0 to SkyGradient(Color(0xFF050E3C), Color(0xFF051645), Color(0xFF041B5F))
            )
        )

        /** Both tables, so a sweep test cannot forget the one that was added second. */
        val entries: List<SkyPalette> = listOf(Paper, Vivid)

        /**
         * How much of a stop's color a fully overcast sky removes, and how much of its
         * brightness.
         *
         * Clouds take the COLOR out of a sky, not a fixed amount of light into it: the
         * first draft of §3.3 mixed every stop toward one grey, which made an overcast
         * midnight brighter than a clear dusk. Desaturating each stop toward its own
         * brightness keeps an overcast noon grey and an overcast midnight dark, which is
         * the thing anyone looking out of a window already knows.
         */
        private const val CloudDesaturation = 0.7f
        private const val CloudDarkening = 0.15f

        /** What moonlight lifts a night sky toward. Shared: the moon is not part of the
         * palette the reader picked, it is the same moon. */
        internal val Moonlight = Color(0xFF273458)

        /**
         * The scrim of DESIGN.md §3.6, living here rather than in the component that
         * paints it — which is where the first draft put it, until `NoRawColorTest`
         * pointed out that a hex outside `ui/theme/` is a hex outside `ui/theme/`
         * whatever its excuse. It belongs here anyway: the scrim is part of the sky's
         * contract, and now `ScrimContractTest` guards the value the canvas actually
         * uses instead of a copy of it.
         */
        val ScrimColor = Color(0xFF101216)

        /**
         * 0.55, and the number has a reason: against the brightest stop either palette
         * can produce, white lands at 5.27:1 (paper) and 5.28:1 (vivid). 0.50 gives
         * 4.57:1 and leaves no headroom for a band added later; 0.45 gives 3.97:1 and
         * fails outright. Measured as the brush composites — `scrim × α + sky × (1 − α)`
         * in sRGB values — which is not what `Color.lerp` does (DESIGN.md §3.6).
         */
        const val ScrimAlpha = 0.55f
    }
}

/** The sky table the app is currently painting with, so a composable deep in the tree
 * does not have to be handed the reader's palette to draw one gradient. */
val LocalSkyPalette = staticCompositionLocalOf { SkyPalette.Paper }
