package com.callbackdev.chiaro.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
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

/**
 * One phase of light, as a fraction of the day. [start] and [end] are 0..1 from local
 * midnight, and [sunAltitudeDeg] is what the segment is painted with, so the ribbon and
 * the canvas cannot disagree about what nautical twilight looks like.
 */
data class LightPhase(val start: Float, val end: Float, val sunAltitudeDeg: Double)

/**
 * DESIGN.md §4. The signature element: one day of light, as color.
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
 * On the canvas ([nowFraction] set) "now" is a disc, as §4 always said it was, and the
 * part of the day already spent is drawn at [PastAlpha]: the ribbon reads as "the day so
 * far, and what is left of it" before any word is read.
 *
 * [nightFade] is for the compact rows of the week: on a light page seven navy bars were
 * the heaviest ink in the section while saying the least (the night is the part of the
 * day that is the same every day). Given the track color, the dark phases lean toward it
 * and the row shows the day's pill of light instead.
 */
@Composable
fun DaylightRibbon(
    phases: List<LightPhase>,
    nowFraction: Float?,
    description: String,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
    markerColor: Color = Color.White,
    nightFade: Color? = null
) {
    // Read outside the Canvas: the draw lambda is a DrawScope, not a composition.
    val sky = ChiaroTheme.sky
    val colored = phases.sortedBy { it.start }.map { phase ->
        // The middle stop of the canvas' own gradient: the ribbon is a thin slice of the
        // same sky, not a second palette to keep in sync.
        val color = sky.gradient(phase.sunAltitudeDeg).mid
        phase to (nightFade?.let { lerp(color, it, nightFadeAmount(phase.sunAltitudeDeg)) } ?: color)
    }
    // The disc stands out of the band on both sides, so the box is as tall as the disc.
    val boxHeight = if (nowFraction != null) maxOf(height, MarkerDiameter) else height
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(boxHeight)
            .semantics { contentDescription = description }
    ) {
        val bandHeight = height.toPx()
        val top = (size.height - bandHeight) / 2f
        val brush = ribbonBrush(colored, size.width)
        val corner = CornerRadius(bandHeight / 2f)
        fun band(alpha: Float) = drawRoundRect(
            brush = brush,
            topLeft = Offset(0f, top),
            size = Size(size.width, bandHeight),
            cornerRadius = corner,
            alpha = alpha
        )
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

/**
 * The gradient for a day of phases: each phase holds its color over its middle and blends
 * into its neighbour across the edge they share. A phase keeps a solid core of all but
 * [BlendShare] of its width on each side, capped at [MaxBlend] of the day, so a long
 * night stays night and a six-minute twilight is mostly transition — which is what a
 * twilight is.
 */
private fun ribbonBrush(colored: List<Pair<LightPhase, Color>>, width: Float): Brush {
    val stops = ribbonStops(colored.map { it.first }).zip(colored) { (a, b), (_, color) ->
        listOf(a to color, b to color)
    }.flatten()
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
