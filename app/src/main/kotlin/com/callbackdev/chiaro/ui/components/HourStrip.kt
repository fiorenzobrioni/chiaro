package com.callbackdev.chiaro.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.callbackdev.chiaro.ui.icons.ConditionGlyph
import com.callbackdev.chiaro.ui.icons.ConditionIcon
import com.callbackdev.chiaro.ui.icons.LocalMotionPaused
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
    val description: String,
    /** The temperature as a quantity, for the curve the strip draws through its cells
     * (§8.3, 23 set 2026); null draws the figure without a curve. */
    val tempC: Double? = null,
    /** The hour a new day starts at: its label is the day's name, in the accent. */
    val dayStart: Boolean = false
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
 * One cell is 144dp tall at 100% type: 16 (hour) + 6 + 42 (icon) + 6 + 52 (the curve's
 * band: the 20dp figure, 2 of gap, 24 of travel and the 8dp dot) + 6 + 16 (rain). The
 * skeleton quotes that number. A strip with no temperatures (none, today) keeps the old
 * 20dp figure and is 112.
 */
@Composable
fun HourStrip(
    hours: List<HourCell>,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    /** Hoisted by a caller that follows the strip's scroll: the rain chart under it
     * draws the hours in view and scrolls the strip from a tap (§8.3b). */
    rowState: LazyListState = rememberLazyListState()
) {
    // The strip's own scroll holds the weather still while it runs (DESIGN §7.1, 9 set
    // 2026), on top of whatever the page around it is already saying.
    val paused = LocalMotionPaused.current || rowState.isScrollInProgress
    val curve = remember(hours) {
        StripCurve.of(hours.map { it.tempC }, CurveTravel.value, CurveDpPerDegree)
    }
    CompositionLocalProvider(LocalMotionPaused provides paused) {
        LazyRow(
            state = rowState,
            modifier = modifier.fillMaxWidth(),
            contentPadding = contentPadding,
            horizontalArrangement = Arrangement.spacedBy(CellGap)
        ) {
            itemsIndexed(hours, key = { _, cell -> cell.key }) { index, cell ->
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
                    style = if (cell.dayStart) {
                        MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                    } else {
                        MaterialTheme.typography.labelSmall.tabular()
                    },
                    color = if (cell.dayStart) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                ConditionIcon(
                    glyph = cell.condition,
                    // The top rung of the family's ladder, in a 56dp cell. The third
                    // step (8 set 2026) took over the 2dp of vertical padding the icon
                    // used to sit in, so the cell is as tall as it was at 38dp.
                    modifier = Modifier.size(WeatherIconSize.Strip)
                )
                if (curve != null && cell.tempC != null) {
                    CurveCell(
                        curve = curve,
                        index = index,
                        temperature = cell.temperature,
                        gap = CellGap
                    )
                } else {
                    Text(
                        text = cell.temperature,
                        style = MaterialTheme.typography.labelLarge.tabular()
                    )
                }
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
}

/**
 * The strip's temperatures as heights (design review, 23 set 2026): where each cell's dot
 * sits in its band, 0 at the top and 1 at the bottom, and the temperatures themselves.
 *
 * The scale is **degrees per dp, fixed** ([CurveDpPerDegree]), centred on the strip's own
 * mean — so a 10° evening drop is the same slope on any day, which is §9.1's rule for a
 * quantity (a line that re-scaled to its own min and max would draw a 2° wobble as a
 * cliff). Only a strip whose range will not fit the band at that rate is compressed to
 * fit, and then the figures printed on every dot still say the truth.
 */
@Immutable
internal class StripCurve(val temps: List<Double?>, private val mean: Double, private val perDegree: Float) {
    /** The dot's height for cell [i] as 0..1 of the travel, 0 the warmest end. */
    fun level(i: Int): Float? {
        val t = temps.getOrNull(i) ?: return null
        return (0.5f - ((t - mean) * perDegree).toFloat()).coerceIn(0f, 1f)
    }

    companion object {
        /** [travelDp] is how far a dot may move; [dpPerDegree] the preferred rate. */
        fun of(temps: List<Double?>, travelDp: Float, dpPerDegree: Float): StripCurve? {
            val known = temps.filterNotNull()
            if (known.size < 2) return null
            val mean = (known.max() + known.min()) / 2.0
            val range = (known.max() - known.min()).coerceAtLeast(1e-6)
            val rate = minOf(dpPerDegree.toDouble(), travelDp / range)
            return StripCurve(temps, mean, (rate / travelDp).toFloat())
        }
    }
}

/**
 * One cell's share of the curve: the figure riding over its dot, and the line drawn
 * from the midpoint with the cell before to the midpoint with the cell after, as a
 * quadratic through this cell's point. Consecutive cells share their midpoints and their
 * tangents there, so the strip reads as one smooth line although every cell draws its own
 * piece — which is what lets it stay a lazy row. The piece reaches half a gap past the
 * cell on each side; a lazy item is not clipped to itself.
 */
@Composable
private fun CurveCell(curve: StripCurve, index: Int, temperature: String, gap: Dp) {
    val level = curve.level(index) ?: 0.5f
    val before = curve.level(index - 1)
    val after = curve.level(index + 1)
    val line = MaterialTheme.colorScheme.outlineVariant
    val ground = MaterialTheme.colorScheme.surface
    val dotColor = ChiaroTheme.colors.temperatureAt(curve.temps[index] ?: 15.0)
    val dotRing = MaterialTheme.colorScheme.outline
    val labelStyle = MaterialTheme.typography.labelLarge.tabular()
    val density = LocalDensity.current
    val labelHeight = with(density) { labelStyle.lineHeight.toDp() }
    val travel = CurveTravel
    Box(modifier = Modifier.fillMaxWidth().height(labelHeight + LabelGap + travel + DotRadius * 2)) {
        val dotY = curveDotY(before, level, after)
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(travel + DotRadius * 2)
                .align(Alignment.BottomCenter)
        ) {
            val r = DotRadius.toPx()
            fun y(l: Float) = r + l * (size.height - r * 2)
            val w = size.width
            val half = gap.toPx() / 2f
            val p = Offset(w / 2f, y(level))
            val start = before?.let { Offset(-half, y((it + level) / 2f)) } ?: p
            val end = after?.let { Offset(w + half, y((level + it) / 2f)) } ?: p
            val path = Path().apply {
                moveTo(start.x, start.y)
                quadraticTo(p.x, p.y, end.x, end.y)
            }
            drawPath(path, line, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
            val dot = Offset(w / 2f, y(dotY))
            drawCircle(dotRing.copy(alpha = 0.6f), radius = r, center = dot)
            drawCircle(ground, radius = r - 1.dp.toPx(), center = dot)
            drawCircle(dotColor, radius = r - 2.dp.toPx(), center = dot)
        }
        Text(
            text = temperature,
            style = labelStyle,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = travel * dotY)
        )
    }
}

/** Where the curve actually passes at the cell's middle: the quadratic's midpoint, a
 * quarter of each neighbour's midpoint and half the cell's own point. The dot and its
 * figure sit ON the line rather than on the point the line bends toward. */
internal fun curveDotY(before: Float?, level: Float, after: Float?): Float {
    val a = before?.let { (it + level) / 2f } ?: level
    val b = after?.let { (level + it) / 2f } ?: level
    return 0.25f * a + 0.5f * level + 0.25f * b
}

/** How far a dot may travel in its band, and the rate it prefers: 2dp a degree puts an
 * ordinary day's swing of 8–12° across most of the band. */
private val CurveTravel = 24.dp
private const val CurveDpPerDegree = 2f
private val DotRadius = 4.dp
private val LabelGap = 2.dp

/** The strip's gap between cells: `spacedBy` in the row, and the curve's reach. */
private val CellGap = 4.dp

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
                    description = "Alle ${14 + i}, ${22 - i} gradi, pioggia ${rain[i]}%",
                    tempC = 22.0 - i
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
