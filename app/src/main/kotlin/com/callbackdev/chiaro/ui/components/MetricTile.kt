package com.callbackdev.chiaro.ui.components

import androidx.compose.foundation.Canvas
import com.callbackdev.chiaro.ui.icons.MetricIcons
import com.callbackdev.chiaro.ui.theme.forText
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.callbackdev.chiaro.ui.theme.GroupCorner
import com.callbackdev.chiaro.ui.theme.LocalChiaroType

/**
 * DESIGN.md §8.6 and §1.2: a number plus what to do about it.
 *
 * [meaning] is a required parameter, and that is the rule being enforced by the type
 * system rather than by a review: a metric with no honest second line does not belong on
 * the home screen, it belongs in the details sheet. UV 7 is not information; "burns in
 * about 25 minutes" is.
 *
 * Two groups, since the second review of Today (15 ott 2026): at the **top** the eyebrow —
 * the quantity's glyph and its label in one quiet ink ([MetricIcons]) — the value as a
 * **reading** (`ChiaroType.readingValue`, 24sp light tabular, the hero's voice at a tile's
 * scale) and its [track], the quantity's own scale anchored to the world; at the **foot** the
 * facts behind the value ([detail], [note]) and the [meaning], the consequence, quiet and
 * last. The foot is pushed to the tile's bottom, so the two tiles of a row, which share a
 * height, put their values on one line and their meanings on another, whatever each holds.
 *
 * [notable] is the one tile of the grid that says something today (`DetailsNotable`): it
 * stands on `secondaryContainer` and its meaning is set a step up, `bodyMedium` in the
 * container's ink. The emphasis carries no fact of its own — the meaning line already says,
 * in words, why the tile matters — so nothing is said by colour alone (§10); TalkBack hears
 * «worth noting» with it.
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
    /** A composed fact about the value: where the wind comes from. */
    detail: (@Composable () -> Unit)? = null,
    /** A printed fact about the value: the gusts, the dew point, which pollen. */
    note: String? = null,
    /** The tile's corners: alone, the groups' 20dp; in the details grid, its segment's
     * ([gridSegmentShape]). */
    shape: Shape = RoundedCornerShape(GroupCorner),
    /** The grid's one tile that matters today. */
    notable: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val ground = if (notable) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer
    val ink = if (notable) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
    val quiet = if (notable) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val worthNoting = androidx.compose.ui.res.stringResource(com.callbackdev.chiaro.R.string.metric_notable_state)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .let { if (notable) it.semantics { stateDescription = worthNoting } else it },
        color = ground,
        contentColor = ink,
        // The groups' corners since the review of Today (15 ott 2026, DESIGN §6).
        shape = shape
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // The eyebrow holds one line by construction, never by truncation: the glyph is
            // the label's own size now (20dp, it was a 38dp drawing), which leaves the label
            // some 100dp on a 360dp screen where the widest, «Qualità aria», wants 77. A label
            // that outgrows it (a huge font scale) wraps and keeps its words.
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null, // the label right beside it says the word
                    tint = quiet,
                    modifier = Modifier.size(EyebrowIcon.forText())
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = quiet
                )
            }
            Text(
                text = value,
                style = LocalChiaroType.current.readingValue,
                modifier = Modifier.padding(top = 8.dp)
            )
            track?.let {
                QuantityTrack(scale = it, ground = ground, modifier = Modifier.padding(top = 6.dp, bottom = 2.dp))
            }
            Spacer(modifier = Modifier.weight(1f).heightIn(min = 8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                detail?.invoke()
                note?.let { Text(text = it, style = MaterialTheme.typography.bodyMedium) }
                Text(
                    text = meaning,
                    style = if (notable) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
                    color = quiet
                )
            }
        }
    }
}

/** The eyebrow's glyph: the label's own size. */
private val EyebrowIcon = 20.dp

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
fun QuantityTrack(
    scale: TrackScale,
    modifier: Modifier = Modifier,
    /** What the track stands on: its band gaps and the marker's ring are cut from it. */
    ground: Color = MaterialTheme.colorScheme.surfaceContainer
) {
    val gap = ground
    // At rest every scale is one track (second review of Today, 15 ott 2026): the quantity's
    // hue only where the value has reached, so a grid of six scales is six lengths of colour
    // on one grey, not six rainbows at 40%.
    // The ink at 12%, Material's own rest for a track, so it reads on the plain tile and on
    // the notable one alike: `surfaceContainerHighest` stood 1.11:1 off the tile and vanished.
    val rest = MaterialTheme.colorScheme.onSurface.copy(alpha = RestAlpha)
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
                    color = if (i <= lit) colors.rampAt(scale.ramp, (i + 0.5f) / segments) else rest,
                    topLeft = Offset(left, top),
                    size = Size(step, trackHeight),
                    cornerRadius = radius
                )
            }
            val center = Offset(lit * (step + gapPx) + step / 2f, size.height / 2f)
            marker(center, markerRadius, at, gap, ring)
            return@Canvas
        }
        drawRoundRect(
            color = rest,
            topLeft = Offset(0f, top),
            size = Size(size.width, trackHeight),
            cornerRadius = radius
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
private const val RestAlpha = 0.12f

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

/** The European index on 0–100 ("extremely poor" beyond), bands at 20, 40, 60 and 80
 * (EEA, revised 2024) — the same air ramp as the US scale, since it is the same quantity. */
@Composable
@ReadOnlyComposable
fun euAirTrack(aqi: Int): TrackScale = TrackScale(
    fraction = aqi / EuAqiTop,
    ramp = com.callbackdev.chiaro.ui.theme.ChiaroTheme.colors.airRamp,
    ticks = listOf(20f, 40f, 60f, 80f).map { it / EuAqiTop }
)

/**
 * The day's rain (24 set 2026) as its five bands, in water's ramp: steps and not a line,
 * because millimetres are not linear to a reader — 2 and 10 are further apart in what
 * they ask of a day than 30 and 50 — and a linear track would put nine days out of ten
 * in its first twentieth. The bands are `WeatherText.rainBand`'s.
 */
@Composable
@ReadOnlyComposable
fun rainTrack(band: Int, bands: Int): TrackScale = TrackScale(
    fraction = (band + 0.5f) / bands,
    ramp = com.callbackdev.chiaro.ui.theme.ChiaroTheme.colors.rainRamp,
    segments = bands
)

/** Pollen's levels as steps, the level and those under it lit. */
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

/** The European index's «extremely poor» threshold: past 100 the track is full. */
private const val EuAqiTop = 100f

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
                icon = MetricIcons.uv,
                label = "UV", value = "7", meaning = "Scotta in circa 25 minuti",
                track = uvTrack(7)
            )
            MetricTile(
                icon = MetricIcons.wind,
                label = "Vento", value = "12 km/h", meaning = "Tieni il cappello",
                detail = {
                    Text(text = "da nord-est", style = MaterialTheme.typography.bodyMedium)
                },
                note = "Raffiche fino a 45 km/h"
            )
            MetricTile(
                icon = MetricIcons.humidity,
                label = "Umidità", value = "44%", meaning = "Gradevole",
                track = humidityTrack(44),
                note = "Punto di rugiada 12°"
            )
            MetricTile(
                icon = MetricIcons.pollen,
                label = "Pollini", notable = true, value = "Alti", meaning = "Giornata dura per chi è allergico",
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
