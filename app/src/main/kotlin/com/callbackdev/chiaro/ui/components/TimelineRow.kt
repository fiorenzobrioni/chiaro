package com.callbackdev.chiaro.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.ui.icons.WeatherIconSize
import com.callbackdev.chiaro.ui.theme.ChiaroTheme
import com.callbackdev.chiaro.ui.theme.forText
import com.callbackdev.chiaro.ui.theme.tabular

/**
 * DESIGN.md §8.4. One entry of the merged day: time, glyph, one line of prose. Sun
 * events, weather turns and (from Fase 6) the reader's own alerts all use this same
 * row — only the leading glyph differs, which is what makes the list read as one
 * timeline instead of three.
 *
 * The verdict chip slot of §8.4 arrives with the Sky engine wiring (Fase 5); the row
 * takes an optional trailing composable so that lands here without a reshape.
 */
@Composable
fun TimelineRow(
    time: String,
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    /**
     * How soon, under the prose — «tra 18 min» — for the next moment of the day only
     * (design review, 23 set 2026): the clock time is what you check against a watch,
     * the countdown is what you plan with, and the first row is the one being planned.
     */
    soon: String? = null,
    /** The thread through the icons (design review, 23 set 2026): a line from this
     * row's glyph to its neighbours', so the rows read as one day in order. [reach] is
     * half the gap to the next row, which the line crosses to meet the next one's. */
    connectAbove: Boolean = false,
    connectBelow: Boolean = false,
    reach: Dp = 6.dp,
    trailing: (@Composable () -> Unit)? = null
) {
    val timeWidth = 48.dp.forText()
    val line = MaterialTheme.colorScheme.outlineVariant
    Row(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                if (!connectAbove && !connectBelow) return@drawBehind
                val x = (timeWidth + ColumnGap + WeatherIconSize.Timeline / 2).toPx()
                val half = WeatherIconSize.Timeline.toPx() / 2f + IconClearance.toPx()
                val mid = size.height / 2f
                val stroke = 2.dp.toPx()
                if (connectAbove) {
                    drawLine(line, Offset(x, -reach.toPx()), Offset(x, mid - half), stroke, StrokeCap.Round)
                }
                if (connectBelow) {
                    drawLine(line, Offset(x, mid + half), Offset(x, size.height + reach.toPx()), stroke, StrokeCap.Round)
                }
            },
        horizontalArrangement = Arrangement.spacedBy(ColumnGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = time,
            style = MaterialTheme.typography.labelLarge.tabular(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            // §10: the clock column grows with the reader's type, or «12:30 PM»
            // wraps in half at 200% while the prose beside it has room to spare.
            modifier = Modifier.width(timeWidth)
        )
        Icon(
            imageVector = icon,
            contentDescription = null, // the prose beside it says the word
            tint = Color.Unspecified,
            modifier = Modifier.size(WeatherIconSize.Timeline)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium
            )
            soon?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        trailing?.invoke()
    }
}

private val ColumnGap = 12.dp
/** The line stops short of the glyph instead of running into it. */
private val IconClearance = 3.dp

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun TimelineRowPreview() {
    ChiaroTheme(dynamicColor = false) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TimelineRow(
                time = "17:04",
                icon = ImageVector.vectorResource(R.drawable.mc3_overcast_rain),
                text = "Pioggia probabile (72%)"
            )
            TimelineRow(
                time = "19:12",
                icon = ImageVector.vectorResource(R.drawable.mc3_sunset),
                text = "Ora d'oro"
            )
            TimelineRow(
                time = "20:06",
                icon = ImageVector.vectorResource(R.drawable.mc3_horizon),
                text = "Tramonto"
            )
            TimelineRow(
                time = "22:41",
                icon = ImageVector.vectorResource(R.drawable.mc3_moonrise),
                text = "Sorge la luna"
            )
        }
    }
}
