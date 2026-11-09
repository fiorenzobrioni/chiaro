package com.callbackdev.chiaro.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.ui.icons.ConditionGlyph
import com.callbackdev.chiaro.ui.icons.ConditionIcon
import com.callbackdev.chiaro.ui.icons.WeatherIconSize
import com.callbackdev.chiaro.ui.theme.ChiaroMotion
import com.callbackdev.chiaro.ui.theme.ChiaroTheme
import com.callbackdev.chiaro.ui.theme.reducedMotion
import com.callbackdev.chiaro.ui.theme.forText
import com.callbackdev.chiaro.ui.theme.tabular
import com.callbackdev.chiaro.ui.theme.textColumnScale

/**
 * DESIGN.md §8.5. One day of the week: the weekday, the sky's icon with the chance of rain
 * under it, and **the day's two tracks as one block** between its printed low and high (a
 * coloured bar is not a number): the range bar on the scale the caller computed **across the
 * whole week**, and under it, on exactly the same width, the day's ribbon — its light and,
 * since 12 ott 2026, its forecast sky, with the wet hours marked under it.
 *
 * **One block** (second review of Today, 15 ott 2026). The ribbon used to run from under the
 * icon to the row's end while the bar stood between the low and the high, two bars of one
 * day with two different starts and two different ends; now they share both, so the hour of a
 * sunset under the bar is the same place as the evening on it, and the row reads as one day.
 * The chance moved under the icon to give the block its width, and the chevron went (a row
 * that opens is said once, by the section's title, and to TalkBack by every row).
 *
 * Everything arrives formatted; [scaleLowC]/[scaleHighC] are the week's own extremes, shared
 * by all seven rows so the week has a shape.
 *
 * **Closed, the row is its range bar alone; open, it shows its ribbon** (review of Today, 9 nov
 * 2026, committente; it changes «One block» above and the chevron's leaving, both of 15 ott
 * 2026). Seven rows of two bars 4dp apart read as seven double bars, and the ribbon, the
 * finer of the two, was the one nobody could read at a glance: the closed week now says each
 * day's range and its chance, and the day's sky, hour by hour with its wet hours under it,
 * comes with the day opened ([ribbon]), where the hours it is about are. And a row that
 * opens says so itself, with a chevron at its end that turns when it is open ([chevron]):
 * the title's «Tap a day for its hours» said it once for seven rows and beside none of them.
 *
 * **Where the row turns into two is measured** (`splitsRow`): when the block would get less
 * than [TracksMin] beside the three text columns (grown with the type), the icon and the
 * gaps, the day and its sky go on a line of their own and the block takes the whole width
 * under them.
 */
@Composable
fun DayRow(
    dayLabel: String,
    condition: ConditionGlyph,
    /** The quantity, which the ink ramp needs; **null when the model carried no
     * probability** (§1.1) — nothing is printed and the space stays, the way the hour cell's
     * does. A dash would read as a value. */
    rainPct: Int?,
    /** The same quantity printed, which the locale owns (§11); null with [rainPct], and
     * null under the floor the caller prints from (20%, 12 ott 2026). */
    rainLabel: String?,
    lowC: Double,
    highC: Double,
    lowLabel: String,
    highLabel: String,
    scaleLowC: Double,
    scaleHighC: Double,
    phases: List<LightPhase>,
    description: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    /** Whether the day is open, for a row that opens one: said to TalkBack, drawn by the
     * caller (the open day's segment). Null for a row that opens nothing. */
    expanded: Boolean? = null,
    /** The air between the row's touch target and its content. */
    contentPadding: PaddingValues = PaddingValues(0.dp),
    /** Today's row: the temperature now, drawn on the range bar. */
    nowC: Double? = null,
    /** The day's forecast sky for its ribbon (DESIGN §4, 12 ott 2026). */
    sky: List<RibbonHour> = emptyList(),
    /** Whether the ribbon keeps a lane for the wet hours. The caller decides it for the
     * whole week, so seven rows stay one height whichever of them has rain in it. */
    wetLane: Boolean = false,
    /** Whether the icon keeps a line for the chance under it: the caller decides it for the
     * whole week, as the lane, so a dry day's row is as tall as a wet one's. */
    rainLine: Boolean = rainLabel != null,
    /** Today's row: where now is in the day. What comes before it is spent, and the
     * ribbon fades it toward the track like the night (DESIGN §4, 12 ott 2026). */
    skyFrom: Float = 0f,
    /** How far the ribbon's sky leans toward the track ([DaylightRibbon]'s quiet voice):
     * the week's closed rows draw it lower, an open day in full. */
    ribbonQuiet: Float = 0f,
    /** The range bar's empty track, and the colour the ribbon's night leans toward: one step
     * above the [ground] the row stands on. */
    track: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ground: Color = MaterialTheme.colorScheme.surface,
    /** The press's shape: the segment's own on Today's week, the row's elsewhere. */
    targetShape: Shape = TargetShape,
    /** Whether the day's ribbon is drawn under its bar: the open day's on Today's week. */
    ribbon: Boolean = true,
    /** Whether the row ends with the chevron of a row that opens, turned when [expanded]. */
    chevron: Boolean = false
) {
    val openLabel = stringResource(if (expanded == true) R.string.week_day_close_action else R.string.week_day_open_action)
    val state = expanded?.let {
        stringResource(if (it) R.string.week_day_state_open else R.string.week_day_state_closed)
    }
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            // The press is clipped to the target's shape; a row that opens nothing (the guide's
            // picture) is not clipped at all.
            .let {
                if (onClick != null) {
                    it.clip(targetShape).clickable(onClickLabel = openLabel, onClick = onClick)
                } else {
                    it
                }
            }
            .semantics {
                contentDescription = description
                // Only on a row a finger can open.
                if (onClick != null) state?.let { stateDescription = it }
            }
            .padding(contentPadding)
    ) {
        val reduced = reducedMotion()
        val reflow = splitsRow(maxWidth, LocalDensity.current.fontScale, if (chevron) ChevronSize + RowGap else 0.dp)
        val turn by animateFloatAsState(
            targetValue = if (expanded == true) 180f else 0f,
            animationSpec = ChiaroMotion.spatialFast(reduced),
            label = "day chevron"
        )
        val mark: @Composable () -> Unit = {
            Icon(
                imageVector = Icons.Outlined.KeyboardArrowDown,
                contentDescription = null, // the row says open or closed, and what a tap does
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(ChevronSize).rotate(turn)
            )
        }
        val tracks: @Composable RowScope.() -> Unit = {
            Text(
                text = lowLabel,
                style = MaterialTheme.typography.labelLarge.tabular(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                modifier = Modifier.width(TempColumn.forText())
            )
            Column(modifier = Modifier.weight(1f)) {
                TemperatureRangeBar(
                    lowC = lowC,
                    highC = highC,
                    scaleLowC = scaleLowC,
                    scaleHighC = scaleHighC,
                    description = "", // the row's own description covers it
                    nowC = nowC,
                    track = track,
                    ground = ground
                )
                // The gap is the ribbon's own, inside what opens: a closed row is its bar alone,
                // centred on the row as a bar on its own is.
                AnimatedVisibility(
                    visible = ribbon,
                    enter = ChiaroMotion.enter(reduced),
                    exit = ChiaroMotion.exit(reduced)
                ) {
                    DaylightRibbon(
                        phases = phases,
                        nowFraction = null,
                        description = "", // idem: one announcement per row
                        // 6dp since the ribbon carries the sky (committente, 12 ott 2026): it reads
                        // at a glance and stays second to the 8dp range bar above it.
                        height = 6.dp,
                        // The range bar's own track: the night leans toward it, so the row shows
                        // its pill of light rather than a navy bar (DESIGN §4, 23 set 2026).
                        nightFade = track,
                        hours = sky,
                        wetLane = wetLane,
                        skyFrom = skyFrom,
                        quiet = ribbonQuiet,
                        modifier = Modifier.padding(top = TrackGap)
                    )
                }
            }
            // The low ends on the block's left and the high starts on its right: both figures
            // hug the bars they are the ends of, at the same distance.
            Text(
                text = highLabel,
                style = MaterialTheme.typography.labelLarge.tabular(),
                textAlign = TextAlign.Start,
                modifier = Modifier.width(TempColumn.forText())
            )
        }
        if (reflow) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(RowGap),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DayAndSky(dayLabel, condition, rainPct, rainLabel, rainLine)
                    if (chevron) {
                        androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                        mark()
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(RowGap),
                    verticalAlignment = Alignment.CenterVertically,
                    content = tracks
                )
            }
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(RowGap),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DayAndSky(dayLabel, condition, rainPct, rainLabel, rainLine)
                tracks()
                if (chevron) mark()
            }
        }
    }
}

/**
 * Whether the row splits in two at this width and this type scale: when the block of the two
 * tracks would get less than [TracksMin] beside the three text columns (which grow with the
 * type), the icon and the four gaps between them, and [trailing], what stands after the high
 * on a row that has something there (the chevron and its gap). Pure, so the arithmetic is
 * testable.
 */
internal fun splitsRow(width: Dp, fontScale: Float, trailing: Dp = 0.dp): Boolean {
    val text = TextColumns * textColumnScale(fontScale)
    val fixed = text + WeatherIconSize.Week + RowGap * 4 + trailing
    return width - fixed < TracksMin
}

/** The chevron of a row that opens: a mark at the row's end, not a button of its own. */
internal val ChevronSize = 16.dp

/** The day and the two temperature columns, in dp at 100% type. */
private val DayColumn = 44.dp
private val TempColumn = 34.dp
private val TextColumns = DayColumn + TempColumn * 2

/** The gap between the row's columns, and between its two tracks. */
private val RowGap = 8.dp
private val TrackGap = 4.dp

/** Under this the two tracks are smudges, and the ribbon's day too short to read (§10). */
internal val TracksMin = 72.dp

/** The ripple's shape where the caller hands none: the row's own. */
private val TargetShape = RoundedCornerShape(12.dp)

/** Which day, and what kind of day: the weekday, and the sky's icon with the chance under it. */
@Composable
private fun RowScope.DayAndSky(
    dayLabel: String,
    condition: ConditionGlyph,
    rainPct: Int?,
    rainLabel: String?,
    rainLine: Boolean
) {
    Text(
        text = dayLabel,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.width(DayColumn.forText())
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        ConditionIcon(
            glyph = condition,
            // Sized to the row it sits in: the row is as tall as the icon and its chance.
            modifier = Modifier.size(WeatherIconSize.Week)
        )
        if (rainLine) {
            Text(
                // Empty, not a dash: the line holds its height so the week stays one rhythm,
                // and an absent probability leaves no mark that looks like a reading.
                text = rainLabel.orEmpty(),
                style = MaterialTheme.typography.labelSmall.tabular(),
                // The ink ramp, zero included: 0% is the quiet end of the same scale, not the
                // secondary text role (DESIGN.md §2.3).
                color = ChiaroTheme.colors.rainInkAt(rainPct ?: 0)
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun DayRowPreview() {
    ChiaroTheme(dynamicColor = false) {
        val phases = listOf(
            LightPhase(0f, 0.22f, -30.0),
            LightPhase(0.22f, 0.28f, -9.0),
            LightPhase(0.28f, 0.33f, 2.0),
            LightPhase(0.33f, 0.74f, 45.0),
            LightPhase(0.74f, 0.79f, 2.0),
            LightPhase(0.79f, 0.85f, -9.0),
            LightPhase(0.85f, 1f, -30.0)
        )
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(
                Triple("Oggi", 2, Triple(12.0, 22.0, 10 as Int?)),
                Triple("Gio", 63, Triple(14.0, 19.0, 80 as Int?)),
                Triple("Ven", 0, Triple(9.0, 24.0, 0 as Int?)),
                // The row whose model carried no probability: the column is empty and
                // the grid still lines up (§1.1).
                Triple("Sab", 3, Triple(11.0, 20.0, null))
            ).forEach { (day, wmoCode, data) ->
                val (low, high, rain) = data
                DayRow(
                    dayLabel = day,
                    condition = ConditionGlyph(wmoCode),
                    rainPct = rain,
                    rainLabel = rain?.let { "$it%" },
                    lowC = low,
                    highC = high,
                    lowLabel = "${low.toInt()}°",
                    highLabel = "${high.toInt()}°",
                    scaleLowC = 9.0,
                    scaleHighC = 24.0,
                    phases = phases,
                    description = "$day, da ${low.toInt()} a ${high.toInt()} gradi" +
                        (rain?.let { ", pioggia $it%" } ?: "")
                )
            }
        }
    }
}

/**
 * §10 names the week row as one of the two places 200% type is checked, so the check is
 * a preview and not a promise: this is the row at the top of the system's own slider.
 */
@Preview(showBackground = true, widthDp = 360, fontScale = 2f)
@Composable
private fun DayRowLargeTextPreview() = DayRowPreview()
