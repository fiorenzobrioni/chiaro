package com.callbackdev.chiaro.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
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
import com.callbackdev.chiaro.ui.icons.ReportScrolling
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
    val dayStart: Boolean = false,
    /** The strip's first cell on Today: the hour we are in, labelled «Ora» (15 ott 2026). */
    val now: Boolean = false
)

/**
 * DESIGN.md §8.3. Horizontal, one cell an hour, **42dp wide and touching** (second review of
 * Today, 15 ott 2026): hour, icon, the temperature riding its curve, and — on a strip where
 * any hour reaches the chance the caller prints from — **the rain lane**, a bar per hour on a
 * 0–100% scale anchored to the world, in the rain's ink ramp, with the figure under it from
 * 20% up. The bars of neighbouring hours touch, so a wet afternoon is one shape across the
 * strip, the way the ribbon's wet marks are; the lane replaced a separate 24-hour chart under
 * the strip, and with it some 100dp of card. At 42dp a 360dp phone shows seven hours where it
 * showed five.
 *
 * A **null** [HourCell.rainPct] is "no data" (Fase 26): the provider forecast no chance for
 * that hour. The cell keeps its width and its lane is empty — never a zero bar, which is a
 * forecast, and never a dash, which §1.1 forbids. A strip in which **no** cell prints a figure
 * has no lane at all.
 *
 * The strip scrolls **edge to edge** of what it stands on: the caller hands the margin in as
 * [contentPadding], so the first cell starts on the column and the others slide under the edge.
 *
 * One cell is **132dp** tall at 100% type with the lane (16 hour + 4 + 32 icon + 4 + 42 curve
 * band + 4 + 14 lane + 16 figure) and 102 without; the skeleton quotes the tall number.
 */
@Composable
fun HourStrip(
    hours: List<HourCell>,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    rowState: LazyListState = rememberLazyListState(),
    /** What the strip stands on: the curve's dots are cut from it (a card, a segment). */
    ground: Color = MaterialTheme.colorScheme.surface
) {
    // The strip's own scroll holds the weather still while it runs (DESIGN §7.1, 9 set
    // 2026), on top of whatever the page around it is already saying.
    val paused = LocalMotionPaused.current || rowState.isScrollInProgress
    // And the page around it, whose other icons would otherwise keep moving while this strip
    // is dragged (committente, 15 ott 2026).
    ReportScrolling(rowState.isScrollInProgress)
    val lane = hours.any { it.rainLabel != null }
    val curve = remember(hours) {
        StripCurve.of(hours.map { it.tempC }, CurveTravel.value, CurveDpPerDegree)
    }
    CompositionLocalProvider(LocalMotionPaused provides paused) {
        LazyRow(
            state = rowState,
            modifier = modifier.fillMaxWidth(),
            contentPadding = contentPadding
        ) {
            itemsIndexed(hours, key = { _, cell -> cell.key }) { index, cell ->
                Column(
                    modifier = Modifier
                        // §10: a cell measured in dp holding text measured in sp came apart at
                        // 200% — «11 PM» and «-10°» outgrow it — and the strip scrolls sideways
                        // anyway, so the cell is as wide as the reader's type needs.
                        .width(CellWidth.forText())
                        .semantics { contentDescription = cell.description },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = cell.hourLabel,
                        style = when {
                            cell.now || cell.dayStart ->
                                MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                            else -> MaterialTheme.typography.labelSmall.tabular()
                        },
                        color = when {
                            cell.now -> MaterialTheme.colorScheme.onSurface
                            cell.dayStart -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    ConditionIcon(
                        glyph = cell.condition,
                        modifier = Modifier.size(WeatherIconSize.Strip)
                    )
                    if (curve != null && cell.tempC != null) {
                        CurveCell(
                            curve = curve,
                            index = index,
                            temperature = cell.temperature,
                            ground = ground
                        )
                    } else {
                        Text(
                            text = cell.temperature,
                            style = MaterialTheme.typography.labelLarge.tabular()
                        )
                    }
                    if (lane) RainLaneCell(pct = cell.rainPct, label = cell.rainLabel)
                }
            }
        }
    }
}

/**
 * One hour of the rain lane: its bar, from the lane's floor, as tall as its chance on a
 * 0–100% scale and in the rain's fill ramp at that chance (§2.3, the ramp of the marks), so an
 * unlikely hour is short and pale and a likely one tall and full — the figure under it carries
 * the number in the ink ramp, the one that clears 4.5:1; a hairline floor under every hour, so
 * an hour of 0% still stands on the lane and the lane reads as a scale.
 */
@Composable
private fun RainLaneCell(pct: Int?, label: String?) {
    val floor = MaterialTheme.colorScheme.outlineVariant
    val ink = pct?.let { ChiaroTheme.colors.rainInkAt(it) }
    val fill = pct?.let { ChiaroTheme.colors.rainAt(it) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(modifier = Modifier.fillMaxWidth().height(LaneHeight)) {
            val hairline = 1.dp.toPx()
            drawRect(floor, topLeft = Offset(0f, size.height - hairline), size = androidx.compose.ui.geometry.Size(size.width, hairline))
            if (pct != null && fill != null && pct > 0) {
                val h = size.height * (pct.coerceIn(0, 100) / 100f)
                val inset = BarInset.toPx()
                drawRoundRect(
                    color = fill,
                    topLeft = Offset(inset, size.height - h),
                    size = androidx.compose.ui.geometry.Size(size.width - inset * 2, h),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(BarCorner.toPx())
                )
            }
        }
        Text(
            text = label.orEmpty(),
            style = MaterialTheme.typography.labelSmall.tabular(),
            // Zero is the ramp's quiet end, not another role (DESIGN.md §2.3).
            color = ink ?: MaterialTheme.colorScheme.onSurfaceVariant
        )
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
private fun CurveCell(curve: StripCurve, index: Int, temperature: String, ground: Color) {
    val level = curve.level(index) ?: 0.5f
    val before = curve.level(index - 1)
    val after = curve.level(index + 1)
    val line = MaterialTheme.colorScheme.outlineVariant
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
            val half = 0f
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

/** How far a dot may travel in its band, and the rate it prefers: 1.5dp a degree puts an
 * ordinary day's swing of 8–12° across most of the band (24dp and 2dp a degree until the strip
 * was made compact, 15 ott 2026). */
private val CurveTravel = 16.dp
private const val CurveDpPerDegree = 1.5f
private val DotRadius = 4.dp
private val LabelGap = 2.dp

/** A cell's width: seven hours at 360dp, the cells touching so the lane's bars do too. */
private val CellWidth = 42.dp

/** The rain lane: its height (the 100% bar), the air each side of a bar, its corner. */
private val LaneHeight = 14.dp
private val BarInset = 1.dp
private val BarCorner = 2.dp

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
