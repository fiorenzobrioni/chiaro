package com.callbackdev.chiaro.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.callbackdev.chiaro.ui.theme.ChiaroColors
import com.callbackdev.chiaro.ui.theme.ChiaroTheme
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
 */
@Composable
fun RainChart(
    hours: List<RainHour>,
    caption: String,
    description: String,
    modifier: Modifier = Modifier,
    plotHeight: Dp = 56.dp,
    labelEvery: Int = 6
) {
    if (hours.isEmpty()) return
    val colors = ChiaroTheme.colors
    val grid = MaterialTheme.colorScheme.outlineVariant
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
        ) {
            val gutter = maxOf(ceiling.size.width, floor.size.width) + AxisGap.toPx()
            val left = 0f
            val right = (size.width - gutter).coerceAtLeast(1f)
            val top = ceiling.size.height / 2f
            val bottom = top + plotHeight.toPx()

            fun yOf(pct: Int) = bottom - (pct.coerceIn(0, 100) / 100f) * (bottom - top)
            val step = if (hours.size > 1) (right - left) / (hours.size - 1) else 0f
            fun xOf(index: Int) = left + step * index

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

            // One run per stretch the provider actually forecast; a gap stays a gap.
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
                    points.forEach { drawCircle(ink, radius = 1.5.dp.toPx(), center = it) }
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
