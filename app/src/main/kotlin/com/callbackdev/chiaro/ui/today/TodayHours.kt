package com.callbackdev.chiaro.ui.today

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.draw.clip
import com.callbackdev.chiaro.ui.theme.CardCorner
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.domain.WmoCode
import com.callbackdev.chiaro.domain.settings.UnitSettings
import com.callbackdev.chiaro.ui.components.HourCell
import com.callbackdev.chiaro.ui.components.HourStrip
import com.callbackdev.chiaro.ui.components.RainChart
import com.callbackdev.chiaro.ui.components.RainHour
import com.callbackdev.chiaro.ui.components.VisibleCell
import com.callbackdev.chiaro.ui.components.hourWindow
import com.callbackdev.chiaro.ui.format.Formats
import com.callbackdev.chiaro.ui.icons.ConditionGlyph
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
internal fun NextHours(
    content: TodayUiState.Content,
    units: UnitSettings,
    is24h: Boolean,
    timeFmt: DateTimeFormatter,
    locale: Locale
) {
    // The page margin travels differently for the two children: the strip takes it as
    // `contentPadding` so it scrolls edge to edge (DESIGN §8.3), the chart wears it.
    // The strip's scroll is hoisted so the chart can follow it: the chart marks the
    // hours in view and scrolls the strip to the hour it is tapped on (DESIGN §8.3b).
    val rowState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val window = {
        val info = rowState.layoutInfo
        hourWindow(
            info.visibleItemsInfo.map { VisibleCell(it.index, it.offset, it.size) },
            info.viewportStartOffset,
            info.viewportEndOffset
        )
    }
    val cellGap = with(LocalDensity.current) { StripCellGap.roundToPx() }
    val seek: (Float, Boolean) -> Unit = { hour, animate ->
        val info = rowState.layoutInfo
        val cell = info.visibleItemsInfo.firstOrNull()
        val inView = window()
        if (cell != null && inView != null) {
            // Center the hour under the finger, as far as the strip's ends allow.
            val span = inView.end - inView.start
            val first = (hour + 0.5f - span / 2f)
                .coerceIn(0f, (info.totalItemsCount - span).coerceAtLeast(0f))
            val index = first.toInt()
            val offset = ((first - index) * (cell.size + cellGap)).roundToInt()
            scope.launch {
                if (animate) rowState.animateScrollToItem(index, offset)
                else rowState.scrollToItem(index, offset)
            }
        }
    }
    // On a card of its own (15 ott 2026, DESIGN §8.0): the hours were the one section of the
    // page standing bare on the paper, between a sky above and segments below. The strip
    // scrolls inside the card, under its rounded edge, and its curve's dots are cut from the
    // card's ground.
    val ground = MaterialTheme.colorScheme.surfaceContainer
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(CardCorner))
            .background(ground)
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HourStrip(
            hours = content.strip.map { it.toCell(units, is24h, locale) },
            contentPadding = CardPadding,
            rowState = rowState,
            ground = ground
        )
        // A dry run draws NO chart (device review, 4 set). Every value at zero put
        // a flat line along the bottom of a 28dp box, which read on the screen as a
        // stray divider with a hole above it — and said nothing the row of "0%" right
        // over it had not already said. §1.1: a section with no data is not drawn, and
        // a chart of nothing but zeros is one. Since 12 ott 2026 "dry" is the strip's own
        // floor: a run that never reaches [ChanceFloorPct] prints no figure, and a chart
        // of 8% bumps under a row of empty cells would be a shape with nothing to plan by.
        val peak = content.strip.maxByOrNull { it.hour.precipChancePct ?: -1 }
        val peakPct = peak?.hour?.precipChancePct
        if (peakPct != null && peakPct >= ChanceFloorPct) {
            val caption = stringResource(chanceCaption(content.strip.map { it.hour.condition.wmoCode }))
            RainChart(
                hours = content.strip.map {
                    RainHour(
                        label = Formats.hourLabel(it.hour.time, is24h, locale),
                        pct = it.hour.precipChancePct
                    )
                },
                caption = caption,
                description = stringResource(
                    R.string.rain_chart_desc,
                    caption,
                    content.strip.first().hour.time.format(timeFmt),
                    content.strip.last().hour.time.format(timeFmt),
                    peakPct,
                    peak.hour.time.format(timeFmt)
                ),
                window = window,
                onSeek = seek,
                modifier = Modifier.padding(CardPadding)
            )
        }
    }
}

/**
 * What the chance is a chance OF (24 set 2026). `precipitation_probability` is any
 * precipitation, snow included, and the chart said «pioggia» over a night of snow at
 * Longyearbyen. The strip's own codes name it, the way the headline names rain or snow
 * from its hour's code: snow when every hour that falls is snow, «rain or snow» when
 * both fall, rain otherwise — and rain too when no hour draws a code, the chart's word
 * before this.
 */
internal fun chanceCaption(codes: List<Int>): Int {
    // By phase, not by `isPrecipitation` (25 set 2026): an hour of likely snow names the
    // chance as much as a snowing one, and rain and snow together name both.
    val phases = codes.mapNotNull { WmoCode.of(it)?.phase }.toSet()
    val snow = WmoCode.Phase.FROZEN in phases || WmoCode.Phase.MIXED in phases
    val rain = WmoCode.Phase.LIQUID in phases || WmoCode.Phase.FREEZING in phases ||
        WmoCode.Phase.MIXED in phases
    return when {
        snow && rain -> R.string.rain_chart_caption_mixed
        snow -> R.string.rain_chart_caption_snow
        else -> R.string.rain_chart_caption
    }
}

/**
 * A chance is printed from [ChanceFloorPct] up (review of 12 ott 2026). Under it the
 * figure has nothing to plan by — a row of six «0%» under the strip and five in the week
 * was the loudest repetition on the page, saying the one thing every other mark already
 * said. The floor is the state engine's own for a wet hour: below it the app never calls
 * an hour wet, so it has no business printing the figure as if it mattered. The cell and
 * the row keep their width (an empty column, never a dash), and the spoken description
 * still says the figure, zero included: TalkBack reads one cell at a time and has no
 * column of repetitions to be spared.
 */
internal fun chanceLabel(pct: Int?, locale: Locale): String? =
    pct?.takeIf { it >= ChanceFloorPct }?.let { Formats.percent(it, locale) }

/** The floor of [chanceLabel] and of the rain chart: the state engine's «probable». */
internal const val ChanceFloorPct = com.callbackdev.chiaro.domain.WeatherStateEngine.PROBABLE_PCT

/** The card's inner margin, as the strip's `contentPadding` and the chart's padding. */
internal val CardPadding = PaddingValues(horizontal = 16.dp)

/** `HourStrip`'s gap between cells, which the seek needs to turn hours into pixels. */
internal val StripCellGap = 4.dp

@Composable
internal fun StripHour.toCell(
    units: UnitSettings,
    is24h: Boolean,
    locale: Locale,
    markDayStart: Boolean = true
): HourCell {
    val hourLabel = Formats.hourLabel(hour.time, is24h, locale)
    // The strip crosses midnight and never said so (design review, 23 set 2026): «22 23 00
    // 01» reads as one evening. The first hour of a new day is labelled with the day's
    // name, in the accent; the hour itself is the one hour a reader can infer, and the
    // cell still says it aloud. Not in a week row's own strip, which is one day already.
    val dayStart = markDayStart && hour.time.hour == 0
    val shownLabel = if (dayStart) Formats.dayLabel(hour.time.toLocalDate(), locale) else hourLabel
    val temp = Formats.temperature(hour.tempC, units.temperature, locale)
    val word = stringResource(WeatherText.condition(hour.condition.wmoCode))
    return HourCell(
        // The MOMENT, not its label (20 set 2026). The label was unique across the
        // strip's 24 and a day's 24 only while the app read the provider's fixed-offset
        // timestamps as local times; now that they are re-expressed on the city's clock,
        // the day a zone falls back really does hold 02:00 twice — and a `LazyRow` with
        // two equal keys does not draw a duplicate row, it throws.
        key = hour.at.toString(),
        hourLabel = shownLabel,
        tempC = hour.tempC,
        dayStart = dayStart,
        condition = ConditionGlyph(hour.condition.wmoCode, night),
        temperature = temp,
        rainPct = hour.precipChancePct,
        rainLabel = chanceLabel(hour.precipChancePct, locale),
        // Two forms, like the week row below (§1.1). The single form read "rain 0%"
        // through a `?: 0` for an hour the provider forecast no chance for — the cell
        // printed nothing and the announcement invented a forecast, which is the one
        // value a probability must never be given. Spoken as 0 only when 0 is what
        // was forecast.
        description = hour.precipChancePct?.let { pct ->
            stringResource(R.string.hour_cell_desc, hourLabel, word, temp, pct)
        } ?: stringResource(R.string.hour_cell_desc_no_rain, hourLabel, word, temp)
    )
}
