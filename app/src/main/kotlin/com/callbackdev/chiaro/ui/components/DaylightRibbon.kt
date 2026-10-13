package com.callbackdev.chiaro.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.callbackdev.chiaro.ui.theme.ChiaroTheme
import com.callbackdev.chiaro.ui.theme.SkyPalette
import kotlin.math.roundToInt

/**
 * One phase of light, as a fraction of the day. [start] and [end] are 0..1 from local
 * midnight, and [sunAltitudeDeg] is what the segment is painted with, so the ribbon and
 * the canvas cannot disagree about what nautical twilight looks like.
 */
data class LightPhase(val start: Float, val end: Float, val sunAltitudeDeg: Double)

/**
 * One hour of the forecast sky, for the ribbon (DESIGN.md §4, 12 ott 2026). [at] is where
 * the hour's timestamp falls in its day, 0..1 from local midnight like [LightPhase];
 * [cloudPct] and [precipPct] tint the light exactly as they tint the canvas; [wet] is the
 * app's state of the hour — something falls, a possibility does not count — which the
 * compact rows mark under the band, so the rain is a shape and not only a shade.
 */
@Immutable
data class RibbonHour(val at: Float, val cloudPct: Int, val precipPct: Int, val wet: Boolean)

/**
 * DESIGN.md §4. The signature element: one day of light, as color — and since 12 ott 2026
 * of the sky the forecast gives it.
 *
 * It is a **depiction, not an encoding** — nobody has to decode a color into a phase,
 * because every phase is named in text on the Sky screen and [description] says them
 * here. That is why it is allowed a natural sky gradient where §9.1 forbids a rainbow
 * for data.
 *
 * Since the design review of 23 set 2026 it is drawn as light rather than as a table of
 * it: one continuous gradient with rounded ends instead of a row of hard rectangles —
 * which read, on the device, as a barcode — because the sky does not change color on a
 * line and a depiction is allowed to say so. The phases still hold their color across
 * their own middle ([ribbonStops]); only the edges between them blend.
 *
 * **The forecast sky** ([hours], 12 ott 2026): every stretch of the day the forecast
 * covers is painted with `SkyPalette.gradient(phase, cloud, chance)` — the same call that
 * paints the canvas, so the ribbon is the canvas carried through the day and the two
 * cannot tell different stories. Light alone, the week's seven ribbons were seven copies:
 * a week moves sunset by a quarter of an hour, which 4dp does not show. With the sky in
 * them each day says *when* it is clear, grey or wet, which neither the day's icon nor its
 * chance can. The hours already gone are not in the report, and before [skyFrom] the
 * ribbon draws no sky: it does not invent one it was not told about. The canvas keeps the
 * spent light at [PastAlpha]; today's row fades it toward its track ([ribbonColorStops]).
 *
 * On the canvas ([nowFraction] set) "now" is a disc, as §4 always said it was, and the
 * part of the day already spent is drawn at [PastAlpha]: the ribbon reads as "the day so
 * far, and what is left of it" before any word is read.
 *
 * [nightFade] is for the compact rows of the week: on a light page seven navy bars were
 * the heaviest ink in the section while saying the least (the night is the part of the
 * day that is the same every day). Given the track color, the dark phases lean toward it
 * and the row shows the day's pill of light instead.
 *
 * [wetLane] reserves a lane under the band and marks there every [RibbonHour.wet] hour,
 * in the rain's ink at the hour's chance. Color alone must not carry the rain (§2.3), and
 * the darkening the canvas gives a likely hour is a shade, not a mark.
 */
@Composable
fun DaylightRibbon(
    phases: List<LightPhase>,
    nowFraction: Float?,
    description: String,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
    markerColor: Color = Color.White,
    nightFade: Color? = null,
    hours: List<RibbonHour> = emptyList(),
    skyFrom: Float = 0f,
    wetLane: Boolean = false
) {
    // Read outside the Canvas: the draw lambda is a DrawScope, not a composition.
    val sky = ChiaroTheme.sky
    val colors = ChiaroTheme.colors
    // A ribbon is up to 145 sky mixes: computed once per input, not once per frame.
    val stops = remember(phases, hours, skyFrom, nightFade, sky) {
        ribbonColorStops(phases, hours, nightFade, skyFrom) { altitude, cloud, chance ->
            // The middle stop of the canvas' own gradient: the ribbon is a thin slice of
            // the same sky, not a second palette to keep in sync.
            sky.gradient(altitude, cloud, chance).mid
        }
    }
    val wet = if (wetLane) hours.filter { it.wet && it.at < 1f } else emptyList()
    // The disc stands out of the band on both sides, so the box is as tall as the disc.
    val boxHeight = when {
        nowFraction != null -> maxOf(height, MarkerDiameter)
        wetLane -> height + WetGap + WetHeight
        else -> height
    }
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(boxHeight)
            .semantics { contentDescription = description }
    ) {
        val bandHeight = height.toPx()
        // With a lane the band sits at the top and the marks under it; otherwise it is
        // centred, which is what the canvas' disc needs.
        val top = if (wetLane) 0f else (size.height - bandHeight) / 2f
        val brush = ribbonBrush(stops, size.width)
        val corner = CornerRadius(bandHeight / 2f)
        fun band(alpha: Float) = drawRoundRect(
            brush = brush,
            topLeft = Offset(0f, top),
            size = Size(size.width, bandHeight),
            cornerRadius = corner,
            alpha = alpha
        )
        wet.forEach { hour ->
            val (from, to) = wetMarkSpan(hour.at)
            val inset = WetInset.toPx()
            val left = from * size.width + inset
            val right = to * size.width - inset
            if (right > left) {
                val markHeight = WetHeight.toPx()
                drawRoundRect(
                    color = colors.rainInkAt(hour.precipPct),
                    topLeft = Offset(left, bandHeight + WetGap.toPx()),
                    size = Size(right - left, markHeight),
                    cornerRadius = CornerRadius(markHeight / 2f)
                )
            }
        }
        if (nowFraction == null) {
            band(1f)
            return@Canvas
        }
        val x = size.width * nowFraction.coerceIn(0f, 1f)
        clipRect(right = x) { band(PastAlpha) }
        clipRect(left = x) { band(1f) }
        nowDisc(Offset(x, size.height / 2f), markerColor)
    }
}

/** The disc that says "now": the marker color with a thin ring of the scrim's ink, so it
 * separates from a white-hot noon band as well as from a night one. */
private fun DrawScope.nowDisc(center: Offset, color: Color) {
    val radius = MarkerDiameter.toPx() / 2f
    drawCircle(SkyPalette.ScrimColor.copy(alpha = 0.35f), radius = radius, center = center)
    drawCircle(color, radius = radius - 1.5.dp.toPx(), center = center)
}

/** The gradient for a day: the stops [ribbonColorStops] computed, across the width. */
private fun ribbonBrush(stops: List<Pair<Float, Color>>, width: Float): Brush {
    if (stops.size < 2) {
        val color = stops.firstOrNull()?.second ?: Color.Transparent
        return Brush.horizontalGradient(listOf(color, color), startX = 0f, endX = width)
    }
    return Brush.horizontalGradient(
        colorStops = stops.toTypedArray(),
        startX = 0f,
        endX = width.coerceAtLeast(1f)
    )
}

/**
 * The ribbon's color stops, as (fraction of the day, color), in order.
 *
 * With no [hours] they are exactly the light's: each phase holds its color over its core
 * ([ribbonStops]) and the platform blends between cores. With hours, the day is also
 * sampled every ten minutes ([SkySamples]) and every stop is the phase's light tinted by
 * the cloud and the chance at that moment ([skyAt]); between two cores the two tinted
 * colors are mixed in sRGB, which is how the platform's gradient mixes them anyway. The
 * cores' own edges stay among the stops, so a six-minute twilight is as sharp as before.
 *
 * Before [skyFrom] the day is spent: no sky, and in a compact row ([nightFade] set) the
 * light leans toward the track as far as the night does, so today's row reads as what is
 * left of the day. The mapper keeps no hour before the one we are in, and a spent morning
 * drawn as bare light reads, on a row with no dimming, as a clear morning that may have
 * been grey (found on the README's Milan, 12 ott 2026). On the canvas ([nightFade] null)
 * the spent part keeps its light, and the alpha the caller draws it at says "spent".
 *
 * [sky] is `SkyPalette.gradient(…).mid`, passed in so the arithmetic is testable without a
 * theme. Pure; the positions never go backwards.
 */
internal fun ribbonColorStops(
    phases: List<LightPhase>,
    hours: List<RibbonHour>,
    nightFade: Color?,
    skyFrom: Float = 0f,
    sky: (altitudeDeg: Double, cloudPct: Int, precipPct: Int) -> Color
): List<Pair<Float, Color>> {
    val sorted = phases.sortedBy { it.start }
    if (sorted.isEmpty()) return emptyList()
    val cores = ribbonStops(sorted)
    fun light(index: Int, cloud: Int, chance: Int, spent: Boolean = false): Color {
        val altitude = sorted[index].sunAltitudeDeg
        val color = sky(altitude, cloud, chance)
        val fade = if (spent) maxOf(nightFadeAmount(altitude), NightFadeMax) else nightFadeAmount(altitude)
        return nightFade?.let { lerp(color, it, fade) } ?: color
    }
    if (hours.isEmpty() && skyFrom <= 0f) {
        return cores.flatMapIndexed { i, (a, b) ->
            val color = light(i, 0, 0)
            listOf(a to color, b to color)
        }
    }
    val ordered = hours.sortedBy { it.at }
    val samples = (0..SkySamples).map { it / SkySamples.toFloat() }
    val positions = (samples + cores.flatMap { listOf(it.first, it.second) } + skyFrom.coerceIn(0f, 1f))
        .distinct()
        .sorted()
    return positions.map { t ->
        val spent = t < skyFrom
        val (cloud, chance) = if (!spent) skyAt(ordered, t) ?: (0 to 0) else (0 to 0)
        val inside = cores.indexOfFirst { (a, b) -> t >= a && t <= b }
        val color = if (inside >= 0) {
            light(inside, cloud, chance, spent)
        } else {
            // Between core i and core i + 1: the blend the light-only ribbon leaves to the
            // platform, done here because each end now has its own sky.
            val i = cores.indices.lastOrNull { cores[it].second < t } ?: 0
            val j = (i + 1).coerceAtMost(cores.lastIndex)
            val span = cores[j].first - cores[i].second
            val u = if (span <= 0f) 0f else ((t - cores[i].second) / span).coerceIn(0f, 1f)
            mixSrgb(light(i, cloud, chance, spent), light(j, cloud, chance, spent), u)
        }
        t to color
    }
}

/**
 * The forecast's cloud cover and chance at [t], as whole percents, or null where the
 * forecast says nothing: before its first hour, and more than an hour past its last.
 * Between two hours both are interpolated in a straight line — the cover is the model's
 * value at the hour's instant, and the sky does not change on the hour.
 */
internal fun skyAt(hours: List<RibbonHour>, t: Float): Pair<Int, Int>? {
    if (hours.isEmpty()) return null
    val first = hours.first()
    val last = hours.last()
    if (t < first.at) return null
    if (t >= last.at) return if (t - last.at <= HourFraction + 1e-4f) last.cloudPct to last.precipPct else null
    val next = hours.indexOfFirst { it.at > t }
    val b = hours[next]
    val a = hours[next - 1]
    val span = b.at - a.at
    val u = if (span <= 0f) 0f else (t - a.at) / span
    fun mix(x: Int, y: Int) = (x + (y - x) * u).roundToInt()
    return mix(a.cloudPct, b.cloudPct) to mix(a.precipPct, b.precipPct)
}

/** Where a wet hour's mark runs: the hour its label names, centred on its timestamp the
 * way the strip's cell is, clipped to the day. */
internal fun wetMarkSpan(at: Float): Pair<Float, Float> =
    (at - HourFraction / 2f).coerceIn(0f, 1f) to (at + HourFraction / 2f).coerceIn(0f, 1f)

/** Two colors mixed on their sRGB values, as a platform gradient mixes its stops (and
 * not in Oklab, as `lerp` does): a sampled ribbon must blend like the light-only one. */
private fun mixSrgb(a: Color, b: Color, u: Float): Color = Color(
    red = a.red + (b.red - a.red) * u,
    green = a.green + (b.green - a.green) * u,
    blue = a.blue + (b.blue - a.blue) * u,
    alpha = a.alpha + (b.alpha - a.alpha) * u
)

/**
 * Where each phase's solid core starts and ends, as fractions of the day, in the order
 * the phases were given (sorted by start). Monotonic by construction — a gradient's stops
 * must never go backwards — and pure, so the rule is testable without a canvas.
 */
internal fun ribbonStops(phases: List<LightPhase>): List<Pair<Float, Float>> {
    var floor = 0f
    return phases.map { phase ->
        val start = phase.start.coerceIn(0f, 1f)
        val end = phase.end.coerceIn(start, 1f)
        val inset = minOf((end - start) * BlendShare, MaxBlend)
        // A phase that touches midnight has no neighbour on that side to blend into.
        val a = (if (start <= 0f) 0f else start + inset).coerceAtLeast(floor)
        val b = (if (end >= 1f) 1f else end - inset).coerceAtLeast(a)
        floor = b
        a to b
    }
}

/** How far a phase leans toward the page in the compact rows: nothing from the civil
 * twilight up, most of the way by astronomical night. */
internal fun nightFadeAmount(sunAltitudeDeg: Double): Float =
    (((-6.0 - sunAltitudeDeg) / 12.0).coerceIn(0.0, 1.0) * NightFadeMax).toFloat()

private const val BlendShare = 0.3f
private const val MaxBlend = 0.015f
private const val NightFadeMax = 0.75f

/** The day already spent, on the canvas: present, and quieter than what is still ahead. */
private const val PastAlpha = 0.6f
private val MarkerDiameter = 14.dp

/** How finely the forecast sky is sampled: every ten minutes, 145 stops a day. The cloud
 * changes by the hour, so ten minutes draws its turns smooth without a stop to spare. */
private const val SkySamples = 144

/** One hour, as a fraction of the day. */
private const val HourFraction = 1f / 24f

/** The wet lane: 2dp under the band, marks 3dp tall, a hairline between two wet hours so
 * a run of them still counts as hours. */
private val WetGap = 2.dp
private val WetHeight = 3.dp
private val WetInset = 0.5.dp

@Preview(showBackground = true, widthDp = 320, heightDp = 60)
@Composable
private fun DaylightRibbonPreview() {
    ChiaroTheme(dynamicColor = false) {
        val phases = listOf(
            LightPhase(0f, 0.20f, -30.0),
            LightPhase(0.20f, 0.24f, -12.0),
            LightPhase(0.24f, 0.27f, -6.0),
            LightPhase(0.27f, 0.31f, 2.0),
            LightPhase(0.31f, 0.76f, 45.0),
            LightPhase(0.76f, 0.80f, 2.0),
            LightPhase(0.80f, 0.84f, -6.0),
            LightPhase(0.84f, 1f, -30.0)
        )
        androidx.compose.foundation.layout.Column {
            DaylightRibbon(
                phases = phases,
                nowFraction = 0.62f,
                description = "Giorno fino alle 18:14, ora d'oro fino alle 19:12, notte dalle 20:06"
            )
            DaylightRibbon(
                phases = phases,
                nowFraction = null,
                description = "",
                height = 4.dp,
                nightFade = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHighest
            )
        }
    }
}
