package com.callbackdev.chiaro.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.ui.icons.WeatherIconSize
import com.callbackdev.chiaro.ui.theme.ChiaroTheme
import com.callbackdev.chiaro.ui.theme.forText
import com.callbackdev.chiaro.ui.theme.tabular

/**
 * DESIGN.md §8.4. One entry of the merged day, drawn as a segment of its group: the clock
 * time in a column at the start, the glyph, the name and what follows it, and under them,
 * when the row is a sight, the Sky's verdict with its number. Sun moments, weather turns and
 * the reader's own rules all use this same row — only the leading glyph differs, which is
 * what makes the list read as one day instead of three.
 *
 * **A segment, not a line on the paper** (15 ott 2026). The rows used to stand on the page,
 * strung on a thread drawn through their glyphs; the Sky tab draws the same moments as
 * segments of a group, and the two tabs looked cut from different paper. In a group the
 * order is the clock column's to say, and the thread went: its job is done by the times.
 *
 * **The verdict line starts at the row's start**, under the clock, not under the name, for
 * the Sky row's reason ([com.callbackdev.chiaro.ui.sky] `SkyVerdictLine`): under the name the
 * chip has `360 − 32 − 12 − 48 − 12 − 34 − 12 − 16` ≈ 194dp at a 360dp screen, and the widest
 * pair the app prints («Nothing doing», «rain 100%») wants about 236. From the row's start
 * it has 300, and the word is never parted from its number.
 */
@Composable
fun TimelineRow(
    time: String,
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    /** The accent label over the name: «Il prossimo, oggi» on the day's first moment. */
    label: String? = null,
    /** The line under the name: until when the span lasts, how soon it comes. */
    supporting: String? = null,
    /** What a tap opens, or null for a row that opens nothing (a weather turn). */
    onClick: (() -> Unit)? = null,
    /** The glyph's tint: [Color.Unspecified] keeps a weather drawing's own colours; a
     * Material symbol (the reader's rule) takes one. */
    iconTint: Color = Color.Unspecified,
    /** The verdict chip, on its own line under the row. */
    verdict: (@Composable () -> Unit)? = null
) {
    val timeWidth = 48.dp.forText()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .semantics(mergeDescendants = true) {}
            .heightIn(min = RowMinHeight)
            .padding(start = RowStart, top = 12.dp, end = 16.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
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
                tint = iconTint,
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
                // `bodyLarge`, the Sky row's name and the Alerts row's: the same golden hour
                // is one size on every tab that names it.
                Text(text = text, style = MaterialTheme.typography.bodyLarge)
                supporting?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        verdict?.let { Box { it() } }
    }
}

private val RowStart = 12.dp
private val RowMinHeight = 64.dp
private val ColumnGap = 12.dp

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun TimelineRowPreview() {
    ChiaroTheme(dynamicColor = false) {
        SegmentedGroup(
            segments = listOf(
                {
                    TimelineRow(
                        time = "17:04",
                        icon = ImageVector.vectorResource(R.drawable.mc3_rain),
                        text = "Pioggia probabile (72%)"
                    )
                },
                {
                    TimelineRow(
                        time = "19:12",
                        icon = ImageVector.vectorResource(R.drawable.mc3_clear_day),
                        text = "Ora d'oro, sera",
                        supporting = "fino al tramonto, alle 19:41",
                        verdict = { VerdictChip(VerdictKind.PASS, "Ottimo", "nuvole 12%") }
                    )
                }
            ),
            modifier = Modifier.padding(vertical = 16.dp)
        )
    }
}
