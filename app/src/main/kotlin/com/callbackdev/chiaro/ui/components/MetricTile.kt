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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.callbackdev.chiaro.ui.icons.ChiaroIcons
import com.callbackdev.chiaro.ui.icons.WeatherIconSize
import com.callbackdev.chiaro.ui.theme.ReadingValue

/**
 * DESIGN.md §8.6 and §1.2: a number plus what to do about it.
 *
 * [meaning] is a required parameter, and that is the rule being enforced by the type
 * system rather than by a review: a metric with no honest second line does not belong on
 * the home screen, it belongs in the details sheet. UV 7 is not information; "burns in
 * about 25 minutes" is.
 *
 * The tile's lines, top to bottom, since the card review of 8 set 2026: the icon and
 * the label; the value as a **reading** (`ReadingValue`, 24sp light tabular — the hero's
 * voice at a tile's scale, because at 16sp the value barely outranked its own label); an
 * optional [scale], a 4dp track on a scale anchored to the world (UV 0–11, humidity
 * 0–100, air 0–300) so the eye gets "how much" before the number is read — one hue,
 * printed value beside it, §9; an optional [detail] and [note], the facts behind the
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
    /** 0..1 on a world-anchored scale, or null for a metric that has no such scale
     * (pressure, visibility) — a track under those would be a shape with nothing to say. */
    scale: Float? = null,
    /** A composed fact about the value: the wind's arrow and its source. */
    detail: (@Composable () -> Unit)? = null,
    /** A printed fact about the value: the gusts, the dew point, which pollen. */
    note: String? = null,
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
            // keeps its words, which is the honest way to fail.
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
                    modifier = Modifier.size(WeatherIconSize.Tile)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(text = value, style = ReadingValue)
            scale?.let { QuantityTrack(fraction = it, modifier = Modifier.padding(vertical = 2.dp)) }
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
 * A quantity on a scale anchored to the world (DESIGN §9.1): a 4dp track in
 * `outlineVariant`, the part up to the value in `primary`. One hue, because it depicts
 * one quantity; no marker, because "how much of the scale" is the question, not "where";
 * no semantics, because the number it depicts is printed right above it (§9.3).
 */
@Composable
fun QuantityTrack(fraction: Float, modifier: Modifier = Modifier) {
    val track = MaterialTheme.colorScheme.outlineVariant
    val fill = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier.fillMaxWidth().height(4.dp)) {
        val radius = CornerRadius(size.height / 2)
        drawRoundRect(color = track, cornerRadius = radius)
        val filled = size.width * fraction.coerceIn(0f, 1f)
        if (filled > 0f) {
            // Never thinner than it is tall: a 1% fill is still a dot, not a smear.
            drawRoundRect(
                color = fill,
                size = Size(maxOf(filled, size.height), size.height),
                cornerRadius = radius
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MetricTilePreview() {
    com.callbackdev.chiaro.ui.theme.ChiaroTheme(dynamicColor = false) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricTile(
                icon = ChiaroIcons.uv,
                label = "UV", value = "7", meaning = "Scotta in circa 25 minuti",
                scale = 7f / 11f
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
                scale = 0.44f,
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
