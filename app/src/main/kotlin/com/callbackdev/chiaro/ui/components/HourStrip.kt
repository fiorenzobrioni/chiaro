package com.callbackdev.chiaro.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.callbackdev.chiaro.ui.icons.ConditionGlyph
import com.callbackdev.chiaro.ui.icons.ConditionIcon
import com.callbackdev.chiaro.ui.icons.WeatherIconSize
import com.callbackdev.chiaro.ui.theme.ChiaroTheme
import com.callbackdev.chiaro.ui.theme.forText
import com.callbackdev.chiaro.ui.theme.tabular

/**
 * One cell of the hour strip: everything already formatted, because prose and formats
 * are the caller's job (they carry locale and unit settings; this carries layout).
 * [description] is the spoken form of the whole cell — one announcement, not four.
 *
 * [key] is the hour's identity for the `LazyRow` (8 set 2026): without one the cells
 * were keyed by position, so at the top of every hour, when the first hour drops off,
 * all the visible cells changed content at once and each re-inflated its moving icon in
 * the same frame. With the hour as the key only the first cell leaves. A `String`,
 * because lazy keys are saved in a Bundle.
 */
data class HourCell(
    val key: String,
    val hourLabel: String,
    val condition: ConditionGlyph,
    val temperature: String,
    /** Null when the provider forecast no chance for that hour — never a stand-in 0.
     * The quantity, for the ink ramp; [rainLabel] is what gets printed (§11). */
    val rainPct: Int?,
    val rainLabel: String?,
    val description: String
)

/**
 * DESIGN.md §8.3. Horizontal, 24 cells from the next full hour, each 56dp: hour, icon,
 * temperature, rain probability. The rain column prints even a 0 — an absent number
 * under one hour would read as "no data", and it is data.
 *
 * A **null** [HourCell.rainPct] is the other case and it really is "no data" (Fase
 * 26): the provider forecast no chance for that hour. The cell keeps its 56dp so the
 * strip does not shift, and prints nothing where the figure would be — never a 0,
 * which is a forecast, and never a dash, which §1.1 forbids.
 *
 * The strip scrolls **edge to edge** (8 set 2026): the caller hands the page margin in
 * as [contentPadding] rather than padding the row, so the first cell starts on the
 * 16dp line like everything else and the others slide under the screen's edge instead
 * of being cut on a line 16dp inside it, which made the strip read as a box.
 *
 * One cell is 112dp tall at 100% type: 16 (hour) + 6 + 42 (icon) + 6 + 20 (temperature)
 * + 6 + 16 (rain). The skeleton quotes that number.
 */
@Composable
fun HourStrip(
    hours: List<HourCell>,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(hours, key = { it.key }) { cell ->
            Column(
                modifier = Modifier
                    // §10: a cell measured in dp holding text measured in sp came
                    // apart at 200% — «11 PM» and «-10°» both outgrow 56dp, and the
                    // strip scrolls sideways anyway, so the cell can simply be as
                    // wide as the reader's type needs.
                    .width(56.dp.forText())
                    .semantics { contentDescription = cell.description },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = cell.hourLabel,
                    style = MaterialTheme.typography.labelSmall.tabular(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ConditionIcon(
                    glyph = cell.condition,
                    // The top rung of the family's ladder, in a 56dp cell. The third
                    // step (8 set 2026) took over the 2dp of vertical padding the icon
                    // used to sit in, so the cell is as tall as it was at 38dp.
                    modifier = Modifier.size(WeatherIconSize.Strip)
                )
                Text(
                    text = cell.temperature,
                    style = MaterialTheme.typography.labelLarge.tabular()
                )
                Text(
                    text = cell.rainLabel.orEmpty(),
                    style = MaterialTheme.typography.labelSmall.tabular(),
                    // Zero is the ramp's quiet end, not another role (DESIGN.md §2.3);
                    // an hour with no forecast at all prints nothing, so its ink is moot.
                    color = cell.rainPct
                        ?.let { ChiaroTheme.colors.rainInkAt(it) }
                        ?: MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun HourStripPreview() {
    ChiaroTheme(dynamicColor = false) {
        val rain = listOf(0, 0, 10, 40, 70, 80, 30, 5)
        HourStrip(
            hours = (0 until 8).map { i ->
                HourCell(
                    key = "${14 + i}",
                    hourLabel = "${14 + i}",
                    condition = ConditionGlyph(if (rain[i] >= 40) 63 else 2),
                    temperature = "${22 - i}°",
                    rainPct = rain[i],
                    rainLabel = "${rain[i]}%",
                    description = "Alle ${14 + i}, ${22 - i} gradi, pioggia ${rain[i]}%"
                )
            },
            modifier = Modifier.padding(vertical = 16.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        )
    }
}

/** The strip at 200%: the cells grow, the columns stay columns, the row scrolls. */
@Preview(showBackground = true, widthDp = 360, fontScale = 2f)
@Composable
private fun HourStripLargeTextPreview() = HourStripPreview()
