package com.callbackdev.chiaro.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.domain.sky.SkyJob
import com.callbackdev.chiaro.domain.sky.SkyJobCatalog
import com.callbackdev.chiaro.ui.format.currentLocale
import com.callbackdev.chiaro.ui.icons.ChiaroIcons
import com.callbackdev.chiaro.ui.sky.SkyText
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * The rest of the day, in the Sky row's grammar (review of Today, 15 ott 2026): the Sky's
 * names for its moments («Ora d'oro, sera»), until when a span lasts on the line under the
 * name, and the first row marked «Il prossimo, oggi» with how soon on that same line, as
 * the Sky's next-moment card says it. A row of the sky opens the Sky's page for its moment,
 * with «Dove guardare» when it has a direction; a weather turn and the rainbow have no page
 * and open nothing.
 */
@Composable
internal fun RestOfDay(
    content: TodayUiState.Content,
    timeFmt: DateTimeFormatter,
    onOpen: (TimelineItem, SkyJob) -> Unit
) {
    val gap = 12.dp
    val locale = currentLocale()
    val next = stringResource(
        R.string.sky_next_label,
        stringResource(R.string.sky_day_today).lowercase(locale)
    )
    Column(
        // The page's 16, less the rows' own inset, which their press reaches into.
        modifier = Modifier.padding(horizontal = 16.dp - AgendaRowInset),
        verticalArrangement = Arrangement.spacedBy(gap)
    ) {
        content.timeline.forEachIndexed { index, item ->
            val job = timelineJob(item.kind)
            val until = item.until?.let { stringResource(R.string.sky_until, it.format(timeFmt)) }
            val soon = if (index == 0) soonText(content.now, item.at) else null
            com.callbackdev.chiaro.ui.components.TimelineRow(
                time = item.at.format(timeFmt),
                icon = timelineIcon(item.kind),
                text = timelineText(item),
                label = if (index == 0) next else null,
                supporting = listOfNotNull(until, soon).joinToString(" · ").ifEmpty { null },
                connectAbove = index > 0,
                connectBelow = index < content.timeline.lastIndex,
                reach = gap / 2,
                onClick = job?.let { { onOpen(item, it) } },
                inset = AgendaRowInset
            )
        }
    }
}

/** The agenda rows' inset, the room their press has around the text. */
internal val AgendaRowInset = 8.dp

/**
 * The Sky's moment a row of the day is (15 ott 2026), whose page the row opens and whose
 * name it prints. The morning's golden hour ends here rather than starts, so its row keeps
 * its own words and opens the morning's page. Null for what the Sky has no page for: the
 * weather's turns and the rainbow.
 */
internal fun timelineJob(kind: TimelineKind): SkyJob? = when (kind) {
    TimelineKind.SUNRISE -> SkyJobCatalog.SunRise
    TimelineKind.GOLDEN_MORNING_END -> SkyJobCatalog.GoldenAm
    TimelineKind.GOLDEN_EVENING -> SkyJobCatalog.GoldenPm
    TimelineKind.SUNSET -> SkyJobCatalog.SunSet
    TimelineKind.BLUE_EVENING -> SkyJobCatalog.BluePm
    // From the sun at −18° the sky is as dark as the sun leaves it: the Sky's «Buio pieno».
    TimelineKind.DARK -> SkyJobCatalog.DarknessWindow
    TimelineKind.MOONRISE -> SkyJobCatalog.MoonRise
    TimelineKind.MOONSET -> SkyJobCatalog.MoonSet
    TimelineKind.RAINBOW, TimelineKind.RAIN_START, TimelineKind.RAIN_STOP -> null
}

/** «tra 18 min», «tra 2 h», «tra 1 h 20 min» — or nothing for a moment already here. */
@Composable
internal fun soonText(now: LocalDateTime, at: LocalDateTime): String? {
    val minutes = Duration.between(now, at).toMinutes()
    return when {
        minutes < 1 -> null
        minutes < 60 -> stringResource(R.string.tl_in_minutes, minutes.toInt())
        minutes % 60 == 0L -> stringResource(R.string.tl_in_hours, (minutes / 60).toInt())
        else -> stringResource(R.string.tl_in_hours_minutes, (minutes / 60).toInt(), (minutes % 60).toInt())
    }
}

@Composable
internal fun timelineIcon(kind: TimelineKind) = when (kind) {
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
}

/** A row's name: the Sky's own for its moments, so the same golden hour has one name on
 * both tabs; until when a span lasts is the row's supporting line, not the name's. */
@Composable
internal fun timelineText(item: TimelineItem): String = when (item.kind) {
    TimelineKind.GOLDEN_MORNING_END -> stringResource(R.string.tl_golden_morning_end)
    TimelineKind.RAINBOW -> stringResource(
        R.string.timeline_rainbow,
        item.pct ?: 0,
        stringResource(SkyText.bearingRes(item.bearingDeg ?: 0.0))
    )
    TimelineKind.RAIN_START -> stringResource(R.string.tl_rain_start, item.pct ?: 0)
    TimelineKind.RAIN_STOP -> stringResource(R.string.tl_rain_stop)
    else -> stringResource(SkyText.nameRes(timelineJob(item.kind)!!.id))
}
