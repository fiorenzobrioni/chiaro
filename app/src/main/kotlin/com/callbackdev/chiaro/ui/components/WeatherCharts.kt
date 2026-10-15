package com.callbackdev.chiaro.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.callbackdev.chiaro.ui.theme.ChiaroColors
import com.callbackdev.chiaro.ui.theme.ChiaroTheme

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
    height: Dp = 8.dp,
    /**
     * Today's row only: the temperature right now, as a disc on the bar (design review,
     * 23 set 2026) — where in its day the day is. On the week's shared scale like the
     * bar itself, so a morning below the forecast low sits honestly left of the fill.
     */
    nowC: Double? = null,
    /** The empty bar, the scale at rest: one step above the ground it stands on. The page's
     * `surfaceContainerHighest`; on the open day's card, a step stronger (see `DayRow`). */
    track: Color = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHighest,
    /** What the bar stands on, which the now-disc's ring is cut from. */
    ground: Color = androidx.compose.material3.MaterialTheme.colorScheme.surface
) {
    val colors: ChiaroColors = ChiaroTheme.colors
    val ring = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
    Canvas(
        modifier = modifier.fillMaxWidth().height(if (nowC != null) NowDisc else height)
            .semantics { contentDescription = description }
    ) {
        val span = (scaleHighC - scaleLowC).takeIf { it > 0.0 } ?: 1.0
        fun fraction(value: Double) = ((value - scaleLowC) / span).coerceIn(0.0, 1.0).toFloat()
        val barHeight = height.toPx()
        val top = (size.height - barHeight) / 2f
        val radius = barHeight / 2f
        drawRoundRect(
            color = track,
            topLeft = Offset(0f, top),
            size = Size(size.width, barHeight),
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
            topLeft = Offset(left, top),
            size = Size((right - left).coerceAtLeast(barHeight), barHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius)
        )
        nowC?.let { now ->
            val r = size.height / 2f
            val center = Offset((size.width * fraction(now)).coerceIn(r, size.width - r), size.height / 2f)
            drawCircle(ring, radius = r, center = center)
            drawCircle(ground, radius = r - 1.5.dp.toPx(), center = center)
            drawCircle(colors.temperatureAt(now), radius = r - 3.dp.toPx(), center = center)
        }
    }
}

/** Today's disc on the range bar: past §9.2's 8dp marker floor, and taller than the bar
 * so it reads as a mark on it rather than a bead in it. */
private val NowDisc = 12.dp

@Preview(showBackground = true, widthDp = 320)
@Composable
private fun ChartsPreview() {
    com.callbackdev.chiaro.ui.theme.ChiaroTheme(dynamicColor = false) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
        ) {
            listOf(4.0 to 11.0, 8.0 to 19.0, 14.0 to 27.0).forEach { (low, high) ->
                TemperatureRangeBar(
                    lowC = low, highC = high, scaleLowC = 2.0, scaleHighC = 30.0,
                    description = "Da ${low.toInt()} a ${high.toInt()} gradi"
                )
            }
        }
    }
}
