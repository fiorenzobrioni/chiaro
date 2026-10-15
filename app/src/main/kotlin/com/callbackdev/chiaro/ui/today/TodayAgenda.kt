package com.callbackdev.chiaro.ui.today

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.domain.rules.RuleMessages
import com.callbackdev.chiaro.domain.settings.UnitSettings
import com.callbackdev.chiaro.domain.sky.SkyJob
import com.callbackdev.chiaro.domain.sky.SkyVerdictKind
import com.callbackdev.chiaro.ui.alerts.RuleText
import com.callbackdev.chiaro.ui.components.SegmentedGroup
import com.callbackdev.chiaro.ui.components.TimelineRow
import com.callbackdev.chiaro.ui.components.VerdictChip
import com.callbackdev.chiaro.ui.format.currentLocale
import com.callbackdev.chiaro.ui.icons.ChiaroIcons
import com.callbackdev.chiaro.ui.sky.SkyText
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * The rest of the day (VISION §5.2.4), as a group of the Sky's segments (15 ott 2026): the
 * Sky's names for its moments («Ora d'oro, sera»), until when a span lasts on the line under
 * the name, the first moment ahead marked «Il prossimo, oggi» with how soon, and — the
 * product's first promise, which until this day only the Sky tab kept — **will the sky allow
 * it**, the Sky's verdict with the number that decided it. The reader's own rules that hold
 * today stand among the moments, in their own words.
 *
 * A row of the sky opens the Sky's page for its moment, with «Dove guardare» when it has a
 * direction; the rainbow opens its own, with the same compass ([RainbowSheet]); a weather turn
 * and a rule have no page and open nothing.
 */
@Composable
internal fun RestOfDay(
    content: TodayUiState.Content,
    timeFmt: DateTimeFormatter,
    units: UnitSettings,
    /** The rainbow's row opens its own page (15 ott 2026); null leaves it a plain row. */
    onOpenRainbow: ((TimelineItem) -> Unit)? = null,
    onOpen: (TimelineItem, SkyJob) -> Unit
) {
    val locale = currentLocale()
    val res = LocalContext.current.resources
    val next = stringResource(
        R.string.sky_next_label,
        stringResource(R.string.sky_day_today).lowercase(locale)
    )
    val nowWord = stringResource(R.string.sky_moment_now)
    val writer = ruleWriter(units)
    // «The next» is the first moment of the day still ahead, in the Sky's sense: a rule is the
    // reader's reading of the weather, not a moment, and is not the next of anything.
    val firstAhead = content.timeline.indexOfFirst { it.kind != TimelineKind.ALERT }
    SegmentedGroup(
        segments = content.timeline.mapIndexed { index, item ->
            {
                val job = item.kind.skyJob
                if (item.kind == TimelineKind.ALERT && item.rule != null) {
                    val rule = item.rule
                    TimelineRow(
                        time = if (item.now) nowWord else item.at.format(timeFmt),
                        icon = Icons.Outlined.Notifications,
                        iconTint = MaterialTheme.colorScheme.primary,
                        // The reader's name and the reader's words: user content, never
                        // translated (CLAUDE.md), written out as it would arrive.
                        text = rule.rule.name,
                        supporting = RuleMessages.interpolate(
                            rule.rule.message, rule.rule, rule.value, rule.at,
                            content.report, content.now, units, writer
                        ).ifBlank { null }
                    )
                } else {
                    val until = item.until?.let {
                        stringResource(
                            if (item.kind == TimelineKind.GOLDEN_EVENING) R.string.tl_until_sunset
                            else R.string.sky_until,
                            it.format(timeFmt)
                        )
                    }
                    val soon = if (index == firstAhead) soonText(content.now, item.at) else null
                    val skipped = item.skipped?.let { stringResource(SkyText.notScheduledRes(it)) }
                    TimelineRow(
                        time = item.at.format(timeFmt),
                        icon = timelineIcon(item.kind),
                        text = timelineText(item),
                        // How soon rides on the label, as the Sky's next card says it beside «the
                        // next»: on the line under the name it ran to three lines at 384dp.
                        label = if (index == firstAhead) listOfNotNull(next, soon).joinToString(" · ") else null,
                        supporting = listOfNotNull(until, skipped).joinToString(" · ").ifEmpty { null },
                        onClick = if (item.kind == TimelineKind.RAINBOW) {
                            onOpenRainbow?.let { open -> { open(item) } }
                        } else {
                            job?.let { { onOpen(item, it) } }
                        },
                        // Not knowing is not drawn here: the Sky's page for the moment says why
                        // it does not know, and a row of «?» chips on a stale day would repeat
                        // what the freshness chip already said once.
                        verdict = item.verdict
                            ?.takeIf { it.kind != SkyVerdictKind.UNKNOWN }
                            ?.let { verdict ->
                                {
                                    VerdictChip(
                                        kind = SkyText.chipKind(verdict.kind),
                                        label = stringResource(SkyText.verdictWordRes(verdict.kind)),
                                        evidence = SkyText.chipEvidence(res, verdict)
                                    )
                                }
                            }
                    )
                }
            }
        }
    )
}

/** How a rule's message writes its placeholders on this screen: as its notification would. */
@Composable
private fun ruleWriter(units: UnitSettings): RuleMessages.Writer {
    val context = LocalContext.current
    return RuleText.MessageWriter(
        context.resources, units, currentLocale(),
        android.text.format.DateFormat.is24HourFormat(context)
    )
}

/** «tra 18 min», «tra 2 h», «tra 1 h 20 min» — or nothing for a moment already here. */
@Composable
private fun soonText(now: LocalDateTime, at: LocalDateTime): String? {
    val minutes = Duration.between(now, at).toMinutes()
    return when {
        minutes < 1 -> null
        minutes < 60 -> stringResource(R.string.tl_in_minutes, minutes.toInt())
        minutes % 60 == 0L -> stringResource(R.string.tl_in_hours, (minutes / 60).toInt())
        else -> stringResource(R.string.tl_in_hours_minutes, (minutes / 60).toInt(), (minutes % 60).toInt())
    }
}

@Composable
private fun timelineIcon(kind: TimelineKind) = when (kind) {
    TimelineKind.SUNRISE -> ChiaroIcons.sunrise
    TimelineKind.GOLDEN_MORNING_END -> ChiaroIcons.goldenHour
    TimelineKind.GOLDEN_EVENING -> ChiaroIcons.goldenHour
    TimelineKind.SUNSET -> ChiaroIcons.sunset
    TimelineKind.BLUE_EVENING -> ChiaroIcons.star
    TimelineKind.DARK -> ChiaroIcons.starryNight
    TimelineKind.MOONRISE -> ChiaroIcons.moonrise
    TimelineKind.MOONSET -> ChiaroIcons.moonset
    TimelineKind.RAINBOW -> ChiaroIcons.rainbow
    TimelineKind.RAIN_START -> ChiaroIcons.precipitation
    TimelineKind.RAIN_STOP -> ChiaroIcons.cloud
    TimelineKind.ALERT -> ChiaroIcons.cloud // never drawn: an alert row takes the bell
}

/** A row's name: the Sky's own for its moments, so the same golden hour has one name on
 * both tabs; until when a span lasts is the row's supporting line, not the name's. */
@Composable
private fun timelineText(item: TimelineItem): String = when (item.kind) {
    TimelineKind.GOLDEN_MORNING_END -> stringResource(R.string.tl_golden_morning_end)
    TimelineKind.RAINBOW -> stringResource(
        R.string.timeline_rainbow,
        item.pct ?: 0,
        stringResource(SkyText.bearingRes(item.bearingDeg ?: 0.0))
    )
    TimelineKind.RAIN_START -> stringResource(R.string.tl_rain_start, item.pct ?: 0)
    TimelineKind.RAIN_STOP -> stringResource(R.string.tl_rain_stop)
    TimelineKind.ALERT -> item.rule?.rule?.name.orEmpty()
    else -> stringResource(SkyText.nameRes(item.kind.skyJob!!.id))
}
