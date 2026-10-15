package com.callbackdev.chiaro.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
 * Since the review of Today (15 ott 2026) the row speaks the Sky row's grammar: the name
 * at `bodyLarge`, what follows it (until when, how soon) on a line under it at
 * `bodyMedium` in `onSurfaceVariant`, and the next moment of the day marked by a label
 * over its name in the accent, «Il prossimo, oggi», as the Sky's and Alerts' next cards
 * are. A row with a page ([onClick]) opens it, as a row of the Sky does.
 */
@Composable
fun TimelineRow(
    time: String,
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    /** The accent label over the name: «Il prossimo, oggi» on the day's first row. */
    label: String? = null,
    /** The line under the name: until when the span lasts, how soon it comes. */
    supporting: String? = null,
    /** The thread through the icons (design review, 23 set 2026): a line from this
     * row's glyph to its neighbours', so the rows read as one day in order. [reach] is
     * half the gap to the next row, which the line crosses to meet the next one's. */
    connectAbove: Boolean = false,
    connectBelow: Boolean = false,
    reach: Dp = 6.dp,
    /** What a tap opens, or null for a row that opens nothing (a weather turn). */
    onClick: (() -> Unit)? = null,
    /** The air between the row's edge and its content, which the press's ripple reaches
     * into. The thread is drawn outside the press's clip, so it still meets its neighbours. */
    inset: Dp = 0.dp,
    trailing: (@Composable () -> Unit)? = null
) {
    val timeWidth = 48.dp.forText()
    val line = MaterialTheme.colorScheme.outlineVariant
    Row(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                if (!connectAbove && !connectBelow) return@drawBehind
                val x = (inset + timeWidth + ColumnGap + WeatherIconSize.Timeline / 2).toPx()
                val half = WeatherIconSize.Timeline.toPx() / 2f + IconClearance.toPx()
                val mid = size.height / 2f
                val stroke = 2.dp.toPx()
                if (connectAbove) {
                    drawLine(line, Offset(x, -reach.toPx()), Offset(x, mid - half), stroke, StrokeCap.Round)
                }
                if (connectBelow) {
                    drawLine(line, Offset(x, mid + half), Offset(x, size.height + reach.toPx()), stroke, StrokeCap.Round)
                }
            }
            .let {
                if (onClick != null) it.clip(PressShape).clickable(onClick = onClick) else it
            }
            .padding(horizontal = inset),
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
            label?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            // `bodyLarge`, the Sky row's name and the Alerts row's (review of Today, 15 ott
            // 2026): the same golden hour and the same sunset were set two sizes smaller on
            // Today than on the tab that names them.
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge
            )
            supporting?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        trailing?.invoke()
    }
}

/** The press's shape, the week row's own: the ripple reads as one row, not a slab. */
private val PressShape = RoundedCornerShape(12.dp)

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
                icon = ImageVector.vectorResource(R.drawable.mc3_rain),
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
