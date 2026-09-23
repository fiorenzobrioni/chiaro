package com.callbackdev.chiaro.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.callbackdev.chiaro.ui.theme.ChiaroColors
import com.callbackdev.chiaro.ui.theme.ChiaroTheme
import com.callbackdev.chiaro.ui.theme.reducedMotion
import com.callbackdev.chiaro.ui.theme.tabular

/**
 * One hour of [RainChart]: the label the axis prints where this hour is one of the
 * labelled ones, and the chance itself — `null` for an hour the provider gave no
 * probability for (§1.1). A gap is drawn as a gap.
 */
@Immutable
data class RainHour(val label: String, val pct: Int?)

/**
 * DESIGN.md §8.3 and §9. Rain probability over the strip's own 24 hours: one series,
 * one hue, and an axis anchored to the world at 0–100% whatever the day holds, so a wet
 * day is a wall and a quiet one is a line near the floor.
 *
 * It was a bare sparkline until the second device review (6 set 2026), and a bare
 * sparkline is a shape with nowhere to stand: over a day pinned at 100% it drew a
 * near-straight line across an empty box, which says "something is happening" and
 * nothing else. What it was missing was not decoration, it was the two questions a
 * reader actually asks — *how high is that* and *when* — so the chart now states both:
 *
 * - three recessive gridlines (§9.2) at 0, 50 and 100%, the two ends labelled, so the
 *   height of the line is readable without moving the eye off it;
 * - a dot on every hour, which is what makes a flat line read as 24 measurements
 *   rather than a rule;
 * - the hour itself under the axis every [labelEvery] hours, first and last included,
 *   so the span is a time of day and not "the next while";
 * - the area under the line, tinted, because at a glance a filled shape carries a
 *   level and a stroke carries a direction.
 *
 * [description] is required (§9.3), and it speaks for the whole block: the caption is
 * part of the picture, not a second sentence.
 *
 * **The chart is the strip's map** (design review, 23 set 2026). The strip above shows
 * six hours at a time and scrolls; the chart shows all 24 and does not, so the two could
 * never line up hour over hour — and stretching the chart to the strip's 24 cells would
 * have bought the alignment with the one thing the chart is for, the whole day at a
 * glance. So it keeps the overview and marks on it the stretch in view: [window] is read
 * at draw time (a scroll redraws, it never recomposes), and a tap or a drag on the chart
 * calls [onSeek] with the hour under the finger, which scrolls the strip there.
 *
 * **It draws itself in** the first time it is shown: the line is revealed left to right
 * over [RevealMillis], along time, the way the day will go. Never grown up from the
 * floor — for half a second that would draw a dry day nobody forecast (§1.1). Once per
 * page (the flag is saveable, so scrolling it away and back does not replay it), and not
 * at all under reduced motion.
 */
@Composable
fun RainChart(
    hours: List<RainHour>,
    caption: String,
    description: String,
    modifier: Modifier = Modifier,
    plotHeight: Dp = 56.dp,
    labelEvery: Int = 6,
    window: (() -> HourWindow?)? = null,
    onSeek: ((hour: Float, animate: Boolean) -> Unit)? = null
) {
    if (hours.isEmpty()) return
    val colors = ChiaroTheme.colors
    val grid = MaterialTheme.colorScheme.outlineVariant
    val windowFill = MaterialTheme.colorScheme.surfaceContainerHigh

    val seek by rememberUpdatedState(onSeek)
    val reduced = reducedMotion()
    var played by rememberSaveable { mutableStateOf(false) }
    val reveal = remember { Animatable(if (played || reduced) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (reveal.value < 1f) {
            reveal.animateTo(1f, tween(RevealMillis, easing = FastOutSlowInEasing))
        }
        played = true
    }
    // The line is on the INK ramp (§2.3): a mark has a 3:1 floor of its own, and the
    // fill ramp's light end cannot clear it either — a day peaking at 10% used to draw
    // its line in #D0E8FA, which is not a line. The tint under it is the fill ramp,
    // which is where that ramp belongs.
    val peak = hours.mapNotNull { it.pct }.maxOrNull() ?: 0
    val ink = colors.rainInkAt(peak)
    val tint = colors.rainAt(peak)

    val measurer = rememberTextMeasurer()
    val axisStyle = MaterialTheme.typography.labelSmall.tabular()
        .copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val ceiling = remember(axisStyle) { measurer.measure(AnnotatedString("100%"), axisStyle) }
    val floor = remember(axisStyle) { measurer.measure(AnnotatedString("0%"), axisStyle) }
    val hourLabels = remember(hours, axisStyle) {
        hours.map { measurer.measure(AnnotatedString(it.label), axisStyle) }
    }
    val density = LocalDensity.current
    val axisHeight = with(density) { ceiling.size.height.toDp() }
    val hourHeight = with(density) { hourLabels.maxOf { it.size.height }.toDp() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            // One announcement for the block: the caption is already inside [description].
            .clearAndSetSemantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = caption,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                // Half the axis label above the ceiling line and the hour labels below
                // the floor: the box is exactly what it draws, no magic padding — which
                // is also how it survives 200% type (§10) without clipping a label.
                .height(axisHeight / 2 + plotHeight + TickLength + TickGap + hourHeight)
                .then(
                    // Keyed on the geometry, never on the lambda: a caller's lambda is new
                    // at every recomposition, and restarting the detector mid-drag drops it.
                    if (onSeek == null) Modifier else Modifier
                        .pointerInput(hours.size, ceiling, floor) {
                            detectTapGestures {
                                seek?.invoke(hourAt(it.x, hours.size, gutterOf(ceiling, floor)), true)
                            }
                        }
                        .pointerInput(hours.size, ceiling, floor) {
                            detectHorizontalDragGestures { change, _ ->
                                change.consume()
                                seek?.invoke(
                                    hourAt(change.position.x, hours.size, gutterOf(ceiling, floor)),
                                    false
                                )
                            }
                        }
                )
        ) {
            val gutter = gutterOf(ceiling, floor)
            val left = 0f
            val right = (size.width - gutter).coerceAtLeast(1f)
            val top = ceiling.size.height / 2f
            val bottom = top + plotHeight.toPx()

            fun yOf(pct: Int) = bottom - (pct.coerceIn(0, 100) / 100f) * (bottom - top)
            val step = if (hours.size > 1) (right - left) / (hours.size - 1) else 0f
            fun xOf(index: Int) = left + step * index

            // The hours in view in the strip, behind everything else: the map's "you are
            // here". Half an hour of margin either side, because a cell is an hour wide
            // and the point is its middle.
            window?.invoke()?.let { w ->
                val x0 = (left + step * (w.start - 0.5f)).coerceIn(left, right)
                val x1 = (left + step * (w.end - 0.5f)).coerceIn(x0, right)
                if (x1 > x0) {
                    drawRoundRect(
                        color = windowFill,
                        topLeft = Offset(x0, 0f),
                        size = Size(x1 - x0, bottom + TickLength.toPx()),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(WindowCorner.toPx())
                    )
                }
            }

            listOf(0, 50, 100).forEach { level ->
                val y = yOf(level)
                drawLine(grid, Offset(left, y), Offset(right, y), strokeWidth = 1.dp.toPx())
            }
            // The two ends of the scale, printed: a picture of a number is not a number
            // (§9.3), and this is the number the line is standing on.
            listOf(ceiling to 100, floor to 0).forEach { (label, level) ->
                drawText(
                    label,
                    topLeft = Offset(
                        size.width - label.size.width,
                        yOf(level) - label.size.height / 2f
                    )
                )
            }

            // One run per stretch the provider actually forecast; a gap stays a gap. The
            // reveal clips along time and nothing else: every point it shows is at its
            // real height from the first frame.
            val dotRadius = 1.5.dp.toPx()
            clipRect(right = left + (right - left + dotRadius * 2) * reveal.value) {
            rainRuns(hours).forEach { run ->
                val points = run.map { Offset(xOf(it), yOf(hours[it].pct!!)) }
                if (points.size > 1) {
                    val line = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        points.drop(1).forEach { lineTo(it.x, it.y) }
                    }
                    drawPath(
                        Path().apply {
                            addPath(line)
                            lineTo(points.last().x, bottom)
                            lineTo(points.first().x, bottom)
                            close()
                        },
                        brush = Brush.verticalGradient(
                            listOf(tint.copy(alpha = 0.30f), tint.copy(alpha = 0.06f)),
                            startY = top,
                            endY = bottom
                        )
                    )
                    drawPath(line, ink, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
                }
                // The hour marks. Below about 6dp of pitch they stop being separate
                // hours and start being a thicker line, so they are simply not drawn.
                if (step >= MinDotPitch.toPx() || points.size == 1) {
                    points.forEach { drawCircle(ink, radius = dotRadius, center = it) }
                }
            }
            }

            val last = hours.lastIndex
            val ticks = axisTicks(
                count = hours.size,
                every = labelEvery,
                stepPx = step,
                labelWidthPx = { hourLabels[it].size.width.toFloat() },
                gapPx = AxisGap.toPx()
            )
            ticks.forEach { index ->
                val x = xOf(index)
                drawLine(
                    grid,
                    Offset(x, bottom),
                    Offset(x, bottom + TickLength.toPx()),
                    strokeWidth = 1.dp.toPx()
                )
                val label: TextLayoutResult = hourLabels[index]
                // The ends align to the plot instead of centring on their tick, which is
                // what keeps the first hour off the margin and the last out of the gutter.
                val x0 = when (index) {
                    0 -> left
                    last -> right - label.size.width
                    else -> x - label.size.width / 2f
                }.coerceIn(left, (right - label.size.width).coerceAtLeast(left))
                drawText(
                    label,
                    topLeft = Offset(x0, bottom + TickLength.toPx() + TickGap.toPx())
                )
            }
        }
    }
}

/**
 * The stretches the provider actually forecast, as index runs. A `null` hour ends the
 * run it is in: the line breaks there rather than dipping to a zero nobody predicted
 * (§1.1), and a lone hour between two gaps is a run of one — a dot, no line.
 */
internal fun rainRuns(hours: List<RainHour>): List<List<Int>> {
    val runs = mutableListOf<List<Int>>()
    var run = mutableListOf<Int>()
    hours.forEachIndexed { index, hour ->
        if (hour.pct == null) {
            if (run.isNotEmpty()) runs.add(run)
            run = mutableListOf()
        } else {
            run.add(index)
        }
    }
    if (run.isNotEmpty()) runs.add(run)
    return runs
}

/**
 * The stretch of the strip in view, in hours from its first cell: cell `i` spans
 * `[i, i + 1)`, so a strip resting at its start with six whole cells in view is `0..6`.
 */
@Immutable
data class HourWindow(val start: Float, val end: Float)

/** One cell of a lazy row as its layout reports it: [offset] from the start of the
 * content, after the before-padding, as `LazyListItemInfo` has it. */
data class VisibleCell(val index: Int, val offset: Int, val size: Int)

/**
 * Which hours a lazy row has in view, counting the fractions of the cells cut at either
 * edge. [viewportStart] and [viewportEnd] are `LazyListLayoutInfo`'s own, so the page
 * margin the strip scrolls under counts as in view — it is. Pure, for the test.
 */
internal fun hourWindow(cells: List<VisibleCell>, viewportStart: Int, viewportEnd: Int): HourWindow? {
    val first = cells.firstOrNull() ?: return null
    val last = cells.last()
    if (first.size <= 0 || last.size <= 0) return null
    val start = first.index + ((viewportStart - first.offset).toFloat() / first.size).coerceIn(0f, 1f)
    val end = last.index + ((viewportEnd - last.offset).toFloat() / last.size).coerceIn(0f, 1f)
    return HourWindow(start, end.coerceAtLeast(start))
}

/** The hour under a point of the plot, as [RainChart] lays its x axis out: hour `i` at
 * `step × i`, so the answer is fractional and the caller decides where to land. */
internal fun hourAtX(x: Float, count: Int, plotWidth: Float): Float {
    if (count <= 1 || plotWidth <= 0f) return 0f
    return (x / (plotWidth / (count - 1))).coerceIn(0f, (count - 1).toFloat())
}

private fun Density.gutterOf(ceiling: TextLayoutResult, floor: TextLayoutResult): Float =
    maxOf(ceiling.size.width, floor.size.width) + AxisGap.toPx()

private fun PointerInputScope.hourAt(x: Float, count: Int, gutter: Float): Float =
    hourAtX(x, count, (size.width - gutter).coerceAtLeast(1f))

/**
 * Which hours the axis names: every [every]-th from the first, plus the LAST one —
 * the hour that says where the span ends — unless its label would collide with the
 * one before it. Pure, because "the last hour is named" is a rule and not a pixel.
 */
internal fun axisTicks(
    count: Int,
    every: Int,
    stepPx: Float,
    labelWidthPx: (Int) -> Float,
    gapPx: Float
): List<Int> {
    if (count <= 0) return emptyList()
    val ticks = (0 until count step every.coerceAtLeast(1)).toMutableList()
    val last = count - 1
    val previous = ticks.last()
    if (last != previous &&
        (last - previous) * stepPx >=
        (labelWidthPx(last) + labelWidthPx(previous)) / 2f + gapPx
    ) {
        ticks.add(last)
    }
    return ticks
}

/** The axis' own measurements (§9.2: marks are small, gridlines are recessive). */
private val TickLength = 3.dp
private val TickGap = 3.dp
private val AxisGap = 6.dp
private val MinDotPitch = 6.dp
private val WindowCorner = 6.dp

/** How long the line takes to draw itself in, the first time. */
internal const val RevealMillis = 900

/**
 * DESIGN.md §8.5. One day's low and high on a scale **shared by the whole week**, so the
 * week has a shape: a bar per row scaled to its own day would make every day look
 * identical, which is the exact opposite of what a week view is for.
 *
 * The ends are printed by the caller as numbers (§9.3). A colored bar is not a number.
 */
@Composable
fun TemperatureRangeBar(
    lowC: Double,
    highC: Double,
    scaleLowC: Double,
    scaleHighC: Double,
    description: String,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp
) {
    val colors: ChiaroColors = ChiaroTheme.colors
    val track = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHighest
    Canvas(
        modifier = modifier.fillMaxWidth().height(height)
            .semantics { contentDescription = description }
    ) {
        val span = (scaleHighC - scaleLowC).takeIf { it > 0.0 } ?: 1.0
        fun fraction(value: Double) = ((value - scaleLowC) / span).coerceIn(0.0, 1.0).toFloat()
        val radius = size.height / 2f
        drawRoundRect(
            color = track,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius)
        )
        val left = size.width * fraction(lowC)
        val right = size.width * fraction(highC)
        drawRoundRect(
            brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                listOf(colors.temperatureAt(lowC), colors.temperatureAt(highC)),
                startX = left,
                endX = right.coerceAtLeast(left + 1f)
            ),
            topLeft = Offset(left, 0f),
            size = Size((right - left).coerceAtLeast(size.height), size.height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius)
        )
    }
}

@Preview(showBackground = true, widthDp = 320)
@Composable
private fun ChartsPreview() {
    com.callbackdev.chiaro.ui.theme.ChiaroTheme(dynamicColor = false) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
        ) {
            RainChart(
                hours = listOf(0, 5, 10, 40, 70, 80, 60, 20, 10, 5, 0, null)
                    .mapIndexed { i, pct -> RainHour(label = "%02d".format((14 + i) % 24), pct = pct) },
                caption = "Probabilità di pioggia",
                description = "Probabilità di pioggia dalle 14 alle 1, picco 80% verso le 19"
            )
            listOf(4.0 to 11.0, 8.0 to 19.0, 14.0 to 27.0).forEach { (low, high) ->
                TemperatureRangeBar(
                    lowC = low, highC = high, scaleLowC = 2.0, scaleHighC = 30.0,
                    description = "Da ${low.toInt()} a ${high.toInt()} gradi"
                )
            }
        }
    }
}
