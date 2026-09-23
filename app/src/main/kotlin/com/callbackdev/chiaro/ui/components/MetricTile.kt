package com.callbackdev.chiaro.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.callbackdev.chiaro.ui.icons.ChiaroIcons
import com.callbackdev.chiaro.ui.icons.WeatherIconSize
import com.callbackdev.chiaro.ui.theme.LocalChiaroType

/**
 * DESIGN.md §8.6 and §1.2: a number plus what to do about it.
 *
 * [meaning] is a required parameter, and that is the rule being enforced by the type
 * system rather than by a review: a metric with no honest second line does not belong on
 * the home screen, it belongs in the details sheet. UV 7 is not information; "burns in
 * about 25 minutes" is.
 *
 * The tile's lines, top to bottom, since the card review of 8 set 2026: the icon and
 * the label; the value as a **reading** (`ChiaroType.readingValue`, 24sp light tabular — the hero's
 * voice at a tile's scale, because at 16sp the value barely outranked its own label); an
 * optional [track], the quantity's own scale anchored to the world (UV 0–11, humidity
 * 0–100, air 0–300, pressure around 1013, pollen's four levels), in its own hue with a
 * disc on the value since 23 set 2026 ([QuantityTrack]), so the eye gets "where on the
 * scale" before the number is read; an optional [detail] and [note], the facts behind the
 * value (where the wind comes from, its gusts, which pollen, the dew point) in the ink
 * of a fact; and the [meaning], the consequence, quiet and last.
 */
@Composable
fun MetricTile(
    icon: ImageVector,
    label: String,
    value: String,
    meaning: String,
    modifier: Modifier = Modifier,
    /** The metric's world scale, or null for one that has none (visibility, a logarithmic
     * quantity, and the wind) — a track under those would be a shape with nothing to say. */
    track: TrackScale? = null,
    /** A composed fact about the value: the wind's arrow and its source. */
    detail: (@Composable () -> Unit)? = null,
    /** A printed fact about the value: the gusts, the dew point, which pollen. */
    note: String? = null,
    /**
     * The glyph's box. [WeatherIconSize.Tile] for every tile but UV, which draws its own
     * value inside the drawing and needs [WeatherIconSize.TileUv] for that value to read
     * at the size the pollen tile's does — the arithmetic is there, not here.
     */
    iconSize: Dp = WeatherIconSize.Tile,
    onClick: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth().let { if (onClick != null) it.clickable(onClick = onClick) else it },
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // The header holds one line by construction, never by truncation: on a
            // 360dp screen the two columns leave the label 80dp beside the 38dp icon
            // (94 before the ladder's first step on 6 set, 88 before its third on 8
            // set), so the eight labels are written to fit that budget rather than
            // trimmed with an ellipsis — the widest of them, «Qualità aria», measures
            // 76.7dp. A label that outgrows the budget (a huge font scale) wraps and
            // keeps its words, which is the honest way to fail. UV spends 17dp more of
            // that budget than the rest ([WeatherIconSize.TileUv], 12 set 2026) and can
            // afford to: its label is the two letters «UV».
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null, // the label right beside it says the word
                    // DESIGN.md §13.1: the icons keep their own colors, here as
                    // everywhere else (HourStrip, DayRow, TimelineRow). A flat tint
                    // collapses a Meteocon into its silhouette, and the details grid is
                    // where that costs the most: the humidity drop loses the white %
                    // that makes it humidity and becomes any other drop, the barometer
                    // loses its needle and becomes a disc, the three particles of the
                    // air-quality mark merge into one blob. Reported from a device
                    // (4 set 2026): «due sembrano uguali».
                    tint = Color.Unspecified,
                    modifier = Modifier.size(iconSize)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(text = value, style = LocalChiaroType.current.readingValue)
            track?.let { QuantityTrack(scale = it, modifier = Modifier.padding(vertical = 2.dp)) }
            detail?.invoke()
            note?.let { Text(text = it, style = MaterialTheme.typography.bodyMedium) }
            Text(
                text = meaning,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * A world scale for [QuantityTrack]: where the value sits on it, the ramp that paints
 * it, and where its bands change.
 *
 * [fraction] is 0..1 on the scale the world uses (UV 0–11, humidity 0–100, air 0–300,
 * pressure 980–1046 hPa). [ticks] are the thresholds the meaning line switches at, as
 * fractions, drawn as gaps in the track. [diverging] fills from the middle out, for a
 * quantity whose middle means something (pressure around 1013). [segments], when set,
 * draws the scale as that many discrete steps (pollen's four levels) instead of a line.
 */
@Immutable
data class TrackScale(
    val fraction: Float,
    val ramp: List<Color>,
    val ticks: List<Float> = emptyList(),
    val diverging: Boolean = false,
    val segments: Int? = null
)

/**
 * A quantity on a scale anchored to the world (DESIGN §8.6, §9.1), drawn since the design
 * review of 23 set 2026 as the quantity's OWN scale rather than a progress bar: the whole
 * ramp is the track, recessive, so the reader sees what the scale runs from and to; the
 * part up to the value is the ramp at full strength; the bands the meaning line switches
 * at are gaps in it; and a disc marks the value, in the ramp's color at that point, so
 * "where on the scale" arrives before the number is read.
 *
 * Until then it was one hue for every metric — `primary` over `outlineVariant`, the same
 * bar under UV, humidity and air — and said "how much" without saying "of what". Each
 * quantity now has its own hue ([ChiaroColors.uvRamp] and siblings), still one hue per
 * quantity (§9.1): never the green-to-violet UV chart, which is a rainbow, and whose bands
 * collapse under deuteranopia — the word under the number carries the band.
 *
 * No semantics: the number it depicts is printed right above it (§9.3).
 */
@Composable
fun QuantityTrack(scale: TrackScale, modifier: Modifier = Modifier) {
    val gap = MaterialTheme.colorScheme.surfaceContainer
    val ring = MaterialTheme.colorScheme.outline
    val colors = com.callbackdev.chiaro.ui.theme.ChiaroTheme.colors
    val at = colors.rampAt(scale.ramp, scale.fraction)
    Canvas(modifier = modifier.fillMaxWidth().height(MarkerSize)) {
        val trackHeight = TrackHeight.toPx()
        val top = (size.height - trackHeight) / 2f
        val radius = CornerRadius(trackHeight / 2f)
        val brush = Brush.horizontalGradient(scale.ramp, startX = 0f, endX = size.width)
        val markerRadius = MarkerSize.toPx() / 2f
        val x = (size.width * scale.fraction.coerceIn(0f, 1f))
        val segments = scale.segments
        if (segments != null && segments > 0) {
            // Discrete steps: the steps up to the value lit, the rest the scale's own
            // color at rest. The gap is §9.2's surface gap between adjacent fills.
            val gapPx = TickGap.toPx()
            val step = (size.width - gapPx * (segments - 1)) / segments
            val lit = (scale.fraction * segments).toInt().coerceIn(0, segments - 1)
            repeat(segments) { i ->
                val left = i * (step + gapPx)
                drawRoundRect(
                    color = colors.rampAt(scale.ramp, (i + 0.5f) / segments),
                    topLeft = Offset(left, top),
                    size = Size(step, trackHeight),
                    cornerRadius = radius,
                    alpha = if (i <= lit) 1f else RestAlpha
                )
            }
            val center = Offset(lit * (step + gapPx) + step / 2f, size.height / 2f)
            marker(center, markerRadius, at, gap, ring)
            return@Canvas
        }
        drawRoundRect(
            brush = brush,
            topLeft = Offset(0f, top),
            size = Size(size.width, trackHeight),
            cornerRadius = radius,
            alpha = RestAlpha
        )
        val from = if (scale.diverging) size.width / 2f else 0f
        val left = minOf(from, x)
        val right = maxOf(from, x)
        if (right - left > 0.5f) {
            clipRect(left = left, right = right) {
                drawRoundRect(
                    brush = brush,
                    topLeft = Offset(0f, top),
                    size = Size(size.width, trackHeight),
                    cornerRadius = radius
                )
            }
        }
        (scale.ticks + if (scale.diverging) listOf(0.5f) else emptyList()).forEach { t ->
            val tx = size.width * t.coerceIn(0f, 1f)
            drawRect(
                color = gap,
                topLeft = Offset(tx - TickGap.toPx() / 2f, top),
                size = Size(TickGap.toPx(), trackHeight)
            )
        }
        marker(
            Offset(x.coerceIn(markerRadius, size.width - markerRadius), size.height / 2f),
            markerRadius, at, gap, ring
        )
    }
}

/** The value: a disc in the ramp's color there, a ring of the tile's own ground that
 * cuts it out of the track, and a hairline of `outline` so the pale end of a ramp
 * still has an edge on a pale tile (§9.2's markers are ≥ 8dp; this one is 12). */
private fun DrawScope.marker(center: Offset, radius: Float, fill: Color, ground: Color, ring: Color) {
    drawCircle(ring.copy(alpha = 0.55f), radius = radius, center = center)
    drawCircle(ground, radius = radius - 1.dp.toPx(), center = center)
    drawCircle(fill, radius = radius - 3.dp.toPx(), center = center)
}

private val TrackHeight = 6.dp
private val MarkerSize = 14.dp
private val TickGap = 2.dp
private const val RestAlpha = 0.4f

// The five scales of the details grid (DESIGN §8.6), each anchored to the world and
// written once, so the grid and the guide's sample cannot draw two different UV tracks.

/** UV on 0–11, the WHO's bands (moderate from 3, high from 6, very high from 8,
 * extreme from 11) as gaps half an index before each. */
@Composable
@ReadOnlyComposable
fun uvTrack(index: Int): TrackScale = TrackScale(
    fraction = index / UvTop,
    ramp = com.callbackdev.chiaro.ui.theme.ChiaroTheme.colors.uvRamp,
    ticks = listOf(2.5f, 5.5f, 7.5f, 10.5f).map { it / UvTop }
)

/** Relative humidity on 0–100, in water's own ramp. No bands: how the air feels is the
 * dew point's to say, and it says it in the meaning line. */
@Composable
@ReadOnlyComposable
fun humidityTrack(pct: Int): TrackScale = TrackScale(
    fraction = pct / 100f,
    ramp = com.callbackdev.chiaro.ui.theme.ChiaroTheme.colors.rainRamp
)

/** The US AQI on 0–300 ("hazardous" beyond), with its bands at 50, 100, 150 and 200. */
@Composable
@ReadOnlyComposable
fun airTrack(aqi: Int): TrackScale = TrackScale(
    fraction = aqi / AqiTop,
    ramp = com.callbackdev.chiaro.ui.theme.ChiaroTheme.colors.airRamp,
    ticks = listOf(50f, 100f, 150f, 200f).map { it / AqiTop }
)

/** Pollen's four levels as four steps, the level and those under it lit. */
@Composable
@ReadOnlyComposable
fun pollenTrack(level: Int, levels: Int): TrackScale = TrackScale(
    fraction = (level + 0.5f) / levels,
    ramp = com.callbackdev.chiaro.ui.theme.ChiaroTheme.colors.pollenRamp,
    segments = levels
)

/** Sea-level pressure on 980–1046 hPa, diverging from 1013 — the standard atmosphere,
 * which is the middle a barometer's dial is printed around. */
@Composable
@ReadOnlyComposable
fun pressureTrack(mb: Double): TrackScale = TrackScale(
    fraction = ((mb - PressureLow) / (PressureHigh - PressureLow)).toFloat(),
    ramp = com.callbackdev.chiaro.ui.theme.ChiaroTheme.colors.pressureRamp,
    diverging = true
)

/** The UV index's practical top: 11 is "extreme" and the WHO's scale is open-ended
 * above it, so a rarer 12 or 13 fills the track and the meaning line does the talking. */
private const val UvTop = 11f

/** The US AQI's "hazardous" threshold: above 300 the track is full and the meaning line
 * says to stay indoors, which is all a reader needs from a number past that. */
private const val AqiTop = 300f

/** 1013 ± 33 hPa: a deep low and a strong high sit at the two ends, and the everyday
 * swing of a few hPa moves the disc visibly. */
private const val PressureLow = 980.0
private const val PressureHigh = 1046.0

@Preview(showBackground = true)
@Composable
private fun MetricTilePreview() {
    com.callbackdev.chiaro.ui.theme.ChiaroTheme(dynamicColor = false) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricTile(
                icon = ChiaroIcons.uv,
                label = "UV", value = "7", meaning = "Scotta in circa 25 minuti",
                track = uvTrack(7)
            )
            MetricTile(
                icon = ChiaroIcons.wind,
                label = "Vento", value = "12 km/h", meaning = "Tieni il cappello",
                detail = {
                    Text(text = "da nord-est", style = MaterialTheme.typography.bodyMedium)
                },
                note = "Raffiche fino a 45 km/h"
            )
            MetricTile(
                icon = ChiaroIcons.humidity,
                label = "Umidità", value = "44%", meaning = "Gradevole",
                track = humidityTrack(44),
                note = "Punto di rugiada 12°"
            )
            MetricTile(
                icon = ChiaroIcons.pollen,
                label = "Pollini", value = "Alti", meaning = "Giornata dura per chi è allergico",
                note = "Graminacee e alberi"
            )
        }
    }
}

/** The other of §10's two places. The grid that holds these reflows to one column at
 * this scale (`TodayScreen.Details`); the tile itself only has to keep its words. */
@Preview(showBackground = true, fontScale = 2f)
@Composable
private fun MetricTileLargeTextPreview() = MetricTilePreview()
