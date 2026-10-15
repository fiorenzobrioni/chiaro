package com.callbackdev.chiaro.ui.today

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.domain.WmoCode
import com.callbackdev.chiaro.domain.model.DailyForecast
import com.callbackdev.chiaro.domain.model.HourlyForecast
import com.callbackdev.chiaro.domain.settings.UnitSettings
import com.callbackdev.chiaro.ui.components.DayRow
import com.callbackdev.chiaro.ui.components.HourStrip
import com.callbackdev.chiaro.ui.format.Formats
import com.callbackdev.chiaro.ui.icons.ChiaroIcons
import com.callbackdev.chiaro.ui.icons.ConditionGlyph
import com.callbackdev.chiaro.ui.icons.WeatherIconSize
import com.callbackdev.chiaro.ui.theme.ChiaroMotion
import com.callbackdev.chiaro.ui.theme.GroupCorner
import com.callbackdev.chiaro.ui.theme.reducedMotion
import java.util.Locale
import kotlin.math.roundToInt

/**
 * The week, one lazy item per day (8 set 2026). It was one item holding a column of
 * seven rows, which composed seven moving icons in the frame the section scrolled into
 * view; as seven items the list's prefetcher takes them one row ahead of the scroll. The
 * rhythm is unchanged: the column spaced its rows by 12dp and the list spaces its items
 * by the same 12dp.
 *
 * The scale is the week's own extremes, computed once here and shared by all seven rows
 * so the week has a shape (DESIGN §8.5).
 */
internal fun LazyListScope.weekRows(
    content: TodayUiState.Content,
    units: UnitSettings,
    is24h: Boolean,
    locale: Locale,
    expandedDay: java.time.LocalDate?,
    onToggle: (java.time.LocalDate) -> Unit
) {
    val scaleLow = content.week.minOf { it.forecast.lowC }
    val scaleHigh = content.week.maxOf { it.forecast.highC }
    val today = content.now.toLocalDate()
    // One lane for the whole week or none: a row that grew by its wet hours would make the
    // week uneven, and a dry week pays nothing for a lane it would leave empty.
    val wetLane = content.week.any { day -> day.sky.any { it.wet && it.at < 1f } }
    items(content.week, key = { "week-${it.forecast.date}" }) { day ->
        WeekRow(
            day = day,
            isToday = day.forecast.date == today,
            scaleLowC = scaleLow,
            scaleHighC = scaleHigh,
            units = units,
            is24h = is24h,
            locale = locale,
            expanded = expandedDay == day.forecast.date,
            onToggle = { onToggle(day.forecast.date) },
            nowC = content.report.current.tempC,
            wetLane = wetLane,
            skyFrom = if (day.forecast.date == today) content.sky.nowFraction else 0f
        )
    }
}

/**
 * One day of the week and, when it is open, its hours.
 *
 * **The open day is a card** (review of Today, 15 ott 2026). Its facts and its strip used to
 * open straight onto the paper between two rows, with the strip's curve running edge to
 * edge, and nothing said which day they belonged to. Open, the row, its facts and its hours
 * now stand on one `surfaceContainer` ground with the groups' 20dp corner ([GroupCorner]),
 * drawn [OpenDayInset] inside the page margin so the row's text keeps its column (the seven
 * bars still line up) and the strip scrolls inside the card rather than off the screen. The
 * ground fades in with the effects spring and the card's top air grows with the spatial one,
 * so opening a day is one movement; closing it reverses it. A closed row stands on the paper
 * as before: seven cards would be a heavier week to read, and the chevron already says
 * which rows open.
 */
@Composable
internal fun WeekRow(
    day: WeekDay,
    isToday: Boolean,
    scaleLowC: Double,
    scaleHighC: Double,
    units: UnitSettings,
    is24h: Boolean,
    locale: Locale,
    expanded: Boolean,
    onToggle: () -> Unit,
    nowC: Double? = null,
    wetLane: Boolean = false,
    /** Today's row: where now is in the day, so what is spent of it reads as spent. */
    skyFrom: Float = 0f
) {
    val f = day.forecast
    val label = if (isToday) stringResource(R.string.week_today) else Formats.dayLabel(f.date, locale)
    val low = Formats.temperature(f.lowC, units.temperature, locale)
    val high = Formats.temperature(f.highC, units.temperature, locale)
    val reduced = reducedMotion()
    val opens = day.hours.isNotEmpty()
    val card = MaterialTheme.colorScheme.surfaceContainer
    val ground by animateColorAsState(
        targetValue = if (expanded) card else card.copy(alpha = 0f),
        animationSpec = ChiaroMotion.effects(reduced),
        label = "open day ground"
    )
    val top by animateDpAsState(
        targetValue = if (expanded) OpenDayInset else 0.dp,
        animationSpec = ChiaroMotion.spatial(reduced),
        label = "open day top"
    )
    // On the card the bar's empty track goes one step stronger, `outlineVariant`: the page's
    // `surfaceContainerHighest` stands 1.11:1 off the card (1.23 off the page), and an open day
    // showed a range bar with no track (committente, device, 15 ott 2026). The ring of today's
    // disc is cut from the ground it stands on, the card's while open.
    val track by animateColorAsState(
        targetValue = if (expanded) MaterialTheme.colorScheme.outlineVariant
        else MaterialTheme.colorScheme.surfaceContainerHighest,
        animationSpec = ChiaroMotion.effects(reduced),
        label = "open day track"
    )
    val rowGround by animateColorAsState(
        targetValue = if (expanded) card else MaterialTheme.colorScheme.surface,
        animationSpec = ChiaroMotion.effects(reduced),
        label = "open day row ground"
    )
    // The closed week speaks its skies lower, so the bars are read first; the open day says
    // its own in full (review of Today, 15 ott 2026, DESIGN §4).
    val ribbonQuiet by animateFloatAsState(
        targetValue = if (expanded) 0f else WeekRibbonQuiet,
        animationSpec = ChiaroMotion.effects(reduced),
        label = "open day ribbon"
    )
    Column(
        modifier = Modifier
            .padding(horizontal = OpenDayInset)
            .drawBehind {
                if (ground.alpha > 0f) {
                    drawRoundRect(color = ground, cornerRadius = CornerRadius(GroupCorner.toPx()))
                }
            }
            // Never under zero: the spatial spring overshoots, and on its way back from 8dp it
            // went below 0 — a negative padding, which Compose refuses with an exception, and
            // the app crashed on closing a day (committente, 15 ott 2026; `TodayAgendaTest`).
            .padding(top = top.coerceAtLeast(0.dp))
    ) {
        DayRow(
            dayLabel = label,
            condition = ConditionGlyph(f.condition.wmoCode, night = false),
            rainPct = f.precipPct,
            rainLabel = chanceLabel(f.precipPct, locale),
            lowC = f.lowC,
            highC = f.highC,
            lowLabel = low,
            highLabel = high,
            scaleLowC = scaleLowC,
            scaleHighC = scaleHighC,
            phases = day.phases,
            sky = day.sky,
            wetLane = wetLane,
            skyFrom = skyFrom,
            // Two forms, because a day with no probability must not announce one:
            // the row that prints nothing says nothing (§1.1). The ribbon's wet marks
            // are spoken after them (§4.6: no mark without its word).
            description = listOfNotNull(f.precipPct?.let { pct ->
                stringResource(
                    R.string.week_day_desc,
                    label,
                    stringResource(WeatherText.condition(f.condition.wmoCode)),
                    low, high, pct
                )
            } ?: stringResource(
                R.string.week_day_desc_no_rain,
                label,
                stringResource(WeatherText.condition(f.condition.wmoCode)),
                low, high
            ), wetDescription(day.hours.map { it.hour }, is24h, locale)).joinToString(". "),
            onClick = if (opens) onToggle else null,
            expanded = if (opens) expanded else null,
            ribbonQuiet = ribbonQuiet.coerceIn(0f, 1f),
            track = track,
            ground = rowGround,
            nowC = if (isToday) nowC else null,
            contentPadding = PaddingValues(horizontal = OpenDayInset)
        )
        AnimatedVisibility(
            visible = expanded,
            enter = ChiaroMotion.enter(reduced),
            exit = ChiaroMotion.exit(reduced)
        ) {
            Column(modifier = Modifier.padding(bottom = OpenDayBottom)) {
                DayFacts(forecast = f, units = units, locale = locale)
                HourStrip(
                    hours = day.hours.map { it.toCell(units, is24h, locale, markDayStart = false) },
                    modifier = Modifier.padding(top = 4.dp),
                    // The card's own inset, so the first hour stands on the page's column and
                    // the strip scrolls under the card's edge, not the screen's.
                    contentPadding = PaddingValues(horizontal = OpenDayInset),
                    // Opened on the morning, not on midnight (review of 12 ott 2026): the
                    // first six cells of a later day were 00–05, all night, and the hours
                    // a reader opens a day for were off the screen. Remembered inside the
                    // expansion, so every opening starts there again.
                    rowState = rememberLazyListState(
                        initialFirstVisibleItemIndex = openingHour(day.hours)
                    )
                )
            }
        }
    }
}

/** The open day's card stands this far inside the page margin, and its content this far
 * inside the card: 8 + 8 is the page's 16, so the row's columns never move. */
internal val OpenDayInset = 8.dp

/**
 * How far a closed week row's ribbon leans toward its track (review of Today, 15 ott 2026).
 * Seven skies at full strength were the loudest thing in the section, louder than the
 * seven range bars that are its figures. Looked at on the README's Milan: at 0.4 a clear
 * afternoon still reads blue, a grey one grey and an evening amber, and the bars come first.
 * The wet marks keep their full ink whatever this is.
 */
internal const val WeekRibbonQuiet = 0.4f

/** The air under the open day's strip, before the card's edge. */
internal val OpenDayBottom = 12.dp

/**
 * Where an open day's strip starts: the hour before the first daylight hour, so the
 * sunrise is the second cell and the dawn before it is in view. Today's row starts at the
 * hour we are in already (the report holds nothing earlier), and opens there, as does a
 * day the sun never rises on.
 */
internal fun openingHour(hours: List<StripHour>): Int =
    (hours.indexOfFirst { !it.night } - 1).coerceAtLeast(0)

/**
 * The words for a row's wet marks (12 ott 2026): what falls, named from the hours' own
 * states the way the rain chart's caption names it, and when — «Pioggia dalle 15:00 alle
 * 17:00, alle 20:00». Null on a dry day, which says nothing about it.
 */
@Composable
internal fun wetDescription(hours: List<HourlyForecast>, is24h: Boolean, locale: Locale): String? {
    val timeFmt = remember(locale, is24h) { Formats.timeFormatter(is24h, locale) }
    val runs = DaylightPhases.wetRuns(hours)
    if (runs.isEmpty()) return null
    val phases = hours.filter { WmoCode.isPrecipitation(it.condition.wmoCode) }
        .mapNotNull { WmoCode.of(it.condition.wmoCode)?.phase }
        .toSet()
    val snow = WmoCode.Phase.FROZEN in phases || WmoCode.Phase.MIXED in phases
    val rain = WmoCode.Phase.LIQUID in phases || WmoCode.Phase.FREEZING in phases ||
        WmoCode.Phase.MIXED in phases
    val spans = runs.map { (from, to) ->
        if (from == to) stringResource(R.string.ribbon_wet_at, from.format(timeFmt))
        else stringResource(R.string.ribbon_wet_span, from.format(timeFmt), to.format(timeFmt))
    }.joinToString(", ")
    return stringResource(
        when {
            snow && rain -> R.string.ribbon_wet_mixed
            snow -> R.string.ribbon_wet_snow
            else -> R.string.ribbon_wet_rain
        },
        spans
    )
}

/**
 * The open day's facts (24 set 2026): how much rain and over how many hours, the snow,
 * the strongest gust, the highest UV — one line each with its consequence (§1.2), and
 * only the lines that say something that day. The closed row stays what it was, weekday,
 * sky, chance and range: §1.3 puts the numbers one level down, and a row that grew four
 * figures would stop being a week you read at a glance. A day with nothing to say draws
 * nothing, not an empty block (§1.1).
 */
@Composable
internal fun DayFacts(forecast: DailyForecast, units: UnitSettings, locale: Locale) {
    // The rain is the rain alone (24 set 2026): the total also carries the snow's water,
    // and printed «0,6 mm di pioggia in 6 ore» over a day whose 0.6 mm were 0.4 cm of snow.
    // `precipitation_hours` counts every kind of hour, so it rides on whichever line is
    // the day's only one, and on neither when rain and snow share the day.
    val rainMm = forecast.rainMm?.takeIf { it >= 0.2 }
    val snowCm = forecast.snowCm?.takeIf { it >= 0.1 }
    val hours = forecast.precipHours?.takeIf { it >= 1.0 && (rainMm == null || snowCm == null) }?.let {
        pluralStringResource(R.plurals.rain_hours_note, it.roundToInt(), Formats.hours(it, locale))
    }
    val facts = buildList {
        rainMm?.let { mm ->
            val amount = stringResource(R.string.day_fact_rain, Formats.millimetres(mm, locale))
            add(DayFact({ ChiaroIcons.precipitation }, listOfNotNull(amount, hours).joinToString(" "), WeatherText.rainMeaning(mm)))
        }
        snowCm?.let { cm ->
            val amount = stringResource(R.string.day_fact_snow, Formats.centimetres(cm, locale))
            add(
                DayFact(
                    { ChiaroIcons.snow },
                    listOfNotNull(amount, hours).joinToString(" "),
                    WeatherText.snowMeaning(cm)
                )
            )
        }
        forecast.gustMaxKph?.takeIf { it >= DayGustNewsKph }?.let { gust ->
            add(
                DayFact(
                    { ChiaroIcons.wind },
                    stringResource(R.string.day_fact_gust, Formats.wind(gust, units.windSpeed, locale)),
                    WeatherText.windMeaning(gust)
                )
            )
        }
        // From «moderate» up (3): a low UV asks nothing of the day, and a line saying so
        // on every winter day would be the block talking to fill itself.
        forecast.uvIndexMax?.takeIf { it >= 3 }?.let { uv ->
            add(DayFact({ ChiaroIcons.uv(uv) }, stringResource(R.string.day_fact_uv, uv), WeatherText.uvPeakMeaning(uv, forecast.condition)))
        }
    }
    if (facts.isEmpty()) return
    Column(
        // Inside the open day's card, whose own inset makes up the page margin.
        modifier = Modifier.padding(horizontal = OpenDayInset).padding(top = 4.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        facts.forEach { fact ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = fact.icon(),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(DayFactIconSize)
                )
                Column {
                    Text(text = fact.value, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = stringResource(fact.meaning),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

internal class DayFact(
    val icon: @Composable () -> androidx.compose.ui.graphics.vector.ImageVector,
    val value: String,
    @androidx.annotation.StringRes val meaning: Int
)

/** The open day's glyphs: the timeline's rung, a leading glyph for a line or two of
 * prose — which is what each fact is. */
internal val DayFactIconSize = WeatherIconSize.Timeline
