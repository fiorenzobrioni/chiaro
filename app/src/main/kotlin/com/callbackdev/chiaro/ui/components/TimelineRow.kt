package com.callbackdev.chiaro.ui.components

import kotlin.math.roundToInt
import kotlin.math.ceil
import androidx.compose.ui.layout.Layout
import androidx.compose.runtime.remember
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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.ui.icons.WeatherIconSize
import com.callbackdev.chiaro.ui.theme.ChiaroTheme
import com.callbackdev.chiaro.ui.theme.forText
import com.callbackdev.chiaro.ui.theme.reflowForText
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
 * **The Sky's rows are this row too** (Fase 16.2, 10 nov 2026). Until then the Sky drew the
 * same golden hour with its time at the row's end, or large in a card of its own, and the
 * moment a reader had just seen on Today was a different drawing one tab over. What the
 * Sky's row has and the day's does not came with it: the bell at the row's end ([trailing]),
 * the camera after the name ([mark]), what a tap is said to do ([onClickLabel]), and a row
 * of the calendar ahead, whose «when» is a date in prose and not a clock time, draws no
 * clock column at all ([time] null).
 *
 * **The clock column is 52dp** (48 until 16.2): «Adesso», the word a moment in progress
 * has where its hour would be, is about 50 in the app's type, and at 48 it broke as «Adess»
 * over «o», on Today's rules that hold now as on the Sky.
 *
 * **Past 150% of type the clock is a line above the row** (§10, the rule every row of
 * columns follows; here since 16.2). The column grows with the type, and at 200% a 104dp
 * clock, the glyph and a bell left the name 90dp of a 360dp screen: «La luna di oggi» and
 * its line stood eight lines tall, three or four letters to a line. Above the row the span
 * is «18:36 – 19:15» with the bell at its end, and the name has the row's width.
 *
 * **The verdict line starts at the row's start**, under the clock, not under the name: under
 * the name the chip has `360 − 32 − 12 − 52 − 12 − 34 − 12 − 16` = 190dp at a 360dp screen
 * (142 beside a bell), and the widest pair the app prints («Niente da fare», «nuvole 86%,
 * basse») wants about 262. From the row's start it has 300, the bell's column included,
 * and the word is never parted from its number.
 */
@Composable
fun TimelineRow(
    /** The clock time; empty keeps the column and leaves it blank (a moment with no hour of
     * its own among moments that have one: the glyphs of a group stay in one column); null
     * draws no column (the calendar ahead). */
    time: String?,
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    /** The accent label over the name: «Il prossimo, oggi» on the day's first moment. */
    label: String? = null,
    /** The line under the name: until when the span lasts, how soon it comes. */
    supporting: String? = null,
    /** What a tap opens, or null for a row that opens nothing (a weather turn). */
    onClick: (() -> Unit)? = null,
    /** What the tap does, for a screen reader: a ripple says it only to the eye. */
    onClickLabel: String? = null,
    /** The glyph's tint: [Color.Unspecified] keeps a weather drawing's own colours; a
     * Material symbol (the reader's rule) takes one. */
    iconTint: Color = Color.Unspecified,
    /** The verdict chip, on its own line under the row. */
    verdict: (@Composable () -> Unit)? = null,
    /** Where a span ends, under the time it starts at (9 nov 2026), and how that is said to
     * a screen reader («until 19:44»): two clock times in a column are a span to the eye, and
     * two numbers in a row to the ear. */
    until: String? = null,
    untilDescription: String? = null,
    /** After the name's last word, on its line and never parted from it: the Sky's camera
     * mark ([NameWithMark]). */
    mark: (@Composable () -> Unit)? = null,
    /** At the row's end, a target of its own: the Sky's bell. Its 48dp box carries its own
     * air, so the row keeps 4dp after it where it keeps 16 after its text. */
    trailing: (@Composable () -> Unit)? = null
) {
    val reflow = reflowForText()
    val clockAbove = reflow && !time.isNullOrEmpty()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .let { if (onClick != null) it.clickable(onClickLabel = onClickLabel, onClick = onClick) else it }
            .semantics(mergeDescendants = true) {}
            .heightIn(min = RowMinHeight)
            .padding(vertical = ListRow.Vertical),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (clockAbove) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = ListRow.Start, end = if (trailing != null) TrailingEnd else RowEnd)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                    ClockTime(time.orEmpty())
                    until?.let {
                        // The dash is the eye's «until»; the ear has the words.
                        Text(
                            text = "–",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.clearAndSetSemantics {}
                        )
                        ClockUntil(it, untilDescription, large = true)
                    }
                }
                trailing?.invoke()
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(ListRow.Gap),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(
                start = ListRow.Start,
                end = if (trailing != null && !clockAbove) TrailingEnd else RowEnd
            )
        ) {
            // §10: the clock column grows with the reader's type, or «12:30 PM» wraps in
            // half while the prose beside it has room to spare; past 150% it is the line
            // above, and a row with no hour keeps no empty column beside rows that have none.
            if (time != null && !reflow) {
                Column(modifier = Modifier.width(ClockWidth.forText())) {
                    if (time.isNotEmpty()) ClockTime(time)
                    // The span's end under its start, a size down: the column is the row's
                    // «when», and «until 19:44» on a line of its own under the name was a line
                    // of the row spent on half a time.
                    until?.let { ClockUntil(it, untilDescription, large = false) }
                }
            }
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
                // `bodyLarge`, the Alerts row's name too: the same golden hour is one size
                // on every tab that names it.
                if (mark == null) {
                    Text(text = text, style = MaterialTheme.typography.bodyLarge)
                } else {
                    NameWithMark(text, mark)
                }
                supporting?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (!clockAbove) trailing?.invoke()
        }
        verdict?.let {
            Box(modifier = Modifier.padding(start = ListRow.Start, end = RowEnd)) { it() }
        }
    }
}

/**
 * A name with its [mark] after its last word, on the last word's line.
 *
 * Beside a name that wraps, a mark in a column of its own stood half a line away from both
 * of its lines; set in the text as an inline placeholder it broke onto a line of its own
 * whenever the name alone filled the line (the end of a placeholder is a place to break, a
 * no-break space before it notwithstanding: seen on the render, «Golden hour, evening» at
 * 384dp). So the name is measured in the width less the mark's, which leaves the mark its
 * place after whichever line turns out to be the last, and the mark is placed there.
 */
@Composable
private fun NameWithMark(text: String, mark: @Composable () -> Unit) {
    val lastLine = remember { FloatArray(3) }
    Layout(
        content = {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                onTextLayout = { result ->
                    val line = result.lineCount - 1
                    lastLine[0] = result.getLineRight(line)
                    lastLine[1] = result.getLineTop(line)
                    lastLine[2] = result.getLineBottom(line)
                }
            )
            Box { mark() }
        }
    ) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val markPlaceable = measurables[1].measure(loose)
        val name = measurables[0].measure(
            loose.copy(maxWidth = (constraints.maxWidth - markPlaceable.width).coerceAtLeast(0))
        )
        val markX = ceil(lastLine[0]).toInt()
        layout(maxOf(name.width, markX + markPlaceable.width), name.height) {
            name.place(0, 0)
            markPlaceable.place(markX, ((lastLine[1] + lastLine[2] - markPlaceable.height) / 2f).roundToInt())
        }
    }
}

@Composable
private fun ClockTime(time: String) {
    Text(
        text = time,
        style = MaterialTheme.typography.labelLarge.tabular(),
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** A span's end: a size down under its start, the start's size beside it. */
@Composable
private fun ClockUntil(until: String, description: String?, large: Boolean) {
    Text(
        text = until,
        style = (if (large) MaterialTheme.typography.labelLarge else MaterialTheme.typography.labelMedium).tabular(),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.semantics { description?.let { said -> contentDescription = said } }
    )
}

// The start, the gap and the air above and below are [ListRow]'s, the ones Alerts' rows
// spend: one density for the three tabs' lists.
private val RowMinHeight = 64.dp
private val RowEnd = 16.dp
private val TrailingEnd = 4.dp
private val ClockWidth = 52.dp

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
                        until = "19:41",
                        supporting = "fino al tramonto",
                        verdict = { VerdictChip(VerdictKind.PASS, "Ottimo", "nuvole 12%") }
                    )
                }
            ),
            modifier = Modifier.padding(vertical = 16.dp)
        )
    }
}
