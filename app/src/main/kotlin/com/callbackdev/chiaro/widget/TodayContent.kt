package com.callbackdev.chiaro.widget

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.callbackdev.chiaro.ui.format.Formats
import com.callbackdev.chiaro.ui.icons.ChiaroIcons
import com.callbackdev.chiaro.ui.today.TodayUiState

/**
 * The Today widget (VISION §5.9): now plus the next hours — the Now widget's one-row
 * form as the head of the card, and under it the hour strip, all straight out of
 * [com.callbackdev.chiaro.ui.today.TodayStateBuilder], so the widget and the app can
 * never tell two stories about the same afternoon. Laid out again on 8 set 2026 on the
 * Now widget's grammar ([TodayWidgetLayout.kt] carries the budget and the reasons).
 */
@Composable
private fun TodayContent(
    content: TodayUiState.Content,
    model: WidgetModel,
    palette: WidgetPalette,
    size: DpSize
) {
    val context = LocalContext.current
    val locale = glanceLocale()
    val is24h = android.text.format.DateFormat.is24HourFormat(context)
    val fontScale = fontScale(context)
    val cells = todayStripCells(size.width, stripCellFor(widestStripLabel(context, is24h, locale)))
    val shown = content.strip.take(cells)
    val today = content.week.firstOrNull()?.forecast
    val range = today?.takeIf { model.look.showDayRange }
    val sentenceOn = model.look.showSentence
    val anyRain = shown.any { (it.hour.precipChancePct ?: 0) > 0 }
    // The warning's chip (Fase 11) is settled on the card the reader would have WITHOUT
    // it: the layout depends on the budget and the budget on the layout, and one of the
    // two has to go first — the same knot the rain row unties by asking the switch
    // rather than the resolved sentence. The trailing column is the only place a chip
    // can stand on this card, so a card too narrow for one carries no warning either.
    val wide = todayIsWide(size.width)
    val warning = warningSlot(
        level = model.warning?.maxLevel,
        enabled = model.look.showWarning,
        headlineShown = sentenceOn && wide,
        sentenceSlot = wide,
        ownRow = wide &&
            todayHasWarningRow(size, fontScale, content.marked, sentenceOn, range != null)
    )
    // The rain row appears when any visible hour has something to report — then EVERY
    // cell prints its figure, because a 0% next to an 80% is information (the app
    // strip's own rule) — and when it fits under the hero's words (the budget's).
    val showRain = anyRain &&
        todayShowRain(
            size, fontScale, content.marked,
            sentence = sentenceOn, range = range != null, warning = warning.drawn
        )
    // The same count as the hours, so the two rows are one grid.
    val days = content.week.take(cells)
    val showDays = model.look.showLater && days.size >= StripCellsFloor &&
        todayShowDays(
            size, fontScale, content.marked,
            sentence = sentenceOn, range = range != null, warning = warning.drawn,
            rain = showRain
        )
    // The hours after the first row's, on a card tall enough for them under the days.
    val more = if (
        showDays && todayShowMoreHours(
            size, fontScale, content.marked,
            sentence = sentenceOn, range = range != null, warning = warning.drawn, rain = showRain
        )
    ) {
        content.strip.drop(cells).take(cells).takeIf { it.size == cells }.orEmpty()
    } else {
        emptyList()
    }

    Column(modifier = GlanceModifier.fillMaxSize()) {
        // The four-by-one's own row (15 nov 2026), laid out for the band the strip leaves:
        // the place over the glyph and the number, the sentence, the chip and the day's
        // range against the far edge. Its height is its content's, never a fixed box, so a
        // font that boxes a little taller than the estimate pushes into the air above the
        // strip rather than losing a line.
        NowHeroRow(
            content, model, palette,
            size = todayHeroSize(size, fontScale, showRain),
            modifier = GlanceModifier.fillMaxWidth(),
            wide = wide,
            warning = warning
        )

        // The strip hangs from the bottom; whatever the grant leaves beyond the budget
        // is air between the two, never padding inside either.
        Spacer(modifier = GlanceModifier.defaultWeight())
        Spacer(modifier = GlanceModifier.height(StripGap))

        // The gaps between cells are PADDING, not spacers (Fase 11, while counting this
        // card's containers): Glance draws at most ten children per container and drops
        // the rest without a word, and seven cells with six spacers between them is
        // thirteen — the last two hours of the strip were being dropped in silence on
        // exactly the four-cell card the widget is designed around. Seven children now.
        //
        // Half the gap on each side of every cell rather than the whole of it between
        // them, so the cells stay one grid: the strip's ink gives up 3 dp at each end,
        // which is the whole visible difference on a 340 dp card.
        HoursRow(shown, showRain, model, palette, is24h, locale)
        if (more.isNotEmpty()) {
            Spacer(modifier = GlanceModifier.height(MoreHoursGap))
            HoursRow(more, showRain || more.any { (it.hour.precipChancePct ?: 0) > 0 }, model, palette, is24h, locale)
        }
        if (showDays) {
            Spacer(modifier = GlanceModifier.height(DaysGap))
            DaysRow(days, content.now.toLocalDate(), model, palette)
        }
    }
}

/**
 * The whole card for [model] at the launcher's [LocalSize], split off the receiver's
 * `provideGlance` (23 set 2026) so the configuration screen's preview and the render tests
 * draw the SAME composition the launcher does rather than a lookalike.
 */
@Composable
internal fun TodayWidgetContent(model: WidgetModel, schemes: WidgetSchemes, skyBitmap: Bitmap?) {
    val content = model.content
    // The Now widget's edge rule: the glyph leads the hero row and owns the top,
    // so those two edges take the glyph's insets; the far edge carries words and
    // the bottom the strip's, so those take the words'. Empty states: words only.
    WidgetCard(
        model, schemes, skyBitmap,
        contentPaddingStart =
            if (content != null) WidgetCardPaddingLeading else WidgetCardPadding,
        contentPaddingEnd = WidgetCardPaddingTrailing,
        contentPaddingTop = if (content != null) WidgetCardPaddingSnug else WidgetCardPadding,
        contentPaddingBottom = WidgetCardPadding
    ) { palette ->
        when {
            content == null && model.city == null -> NoPlaceContent(palette)
            content == null -> NoDataContent(palette)
            else -> TodayContent(content, model, palette, LocalSize.current)
        }
    }
}

/**
 * The days on a tall card ([todayShowDays]), today first: the hours' own grid and type, one
 * cell per day and as many as the hours above have, so the two rows are one grid — the day's name in the quiet ink, its drawing, the high in the strong ink
 * and the low in the quiet one under it, which is the week rows' emphasis in the app.
 */
@Composable
private fun DaysRow(
    days: List<com.callbackdev.chiaro.ui.today.WeekDay>,
    today: java.time.LocalDate,
    model: WidgetModel,
    palette: WidgetPalette
) {
    val context = LocalContext.current
    val locale = glanceLocale()
    Row(modifier = GlanceModifier.fillMaxWidth().padding(start = StripStartInset)) {
        days.forEach { day ->
            val forecast = day.forecast
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = GlanceModifier
                    .padding(horizontal = StripCellSpacing / 2)
                    .defaultWeight()
            ) {
                Text(
                    text = if (forecast.date == today) {
                        context.getString(com.callbackdev.chiaro.R.string.week_today)
                    } else {
                        Formats.dayLabel(forecast.date, locale)
                    },
                    style = secondaryStyle(palette, StripHourSp.sp),
                    maxLines = 1
                )
                Spacer(modifier = GlanceModifier.height(StripInnerGap))
                Image(
                    provider = ImageProvider(
                        ChiaroIcons.conditionRes(
                            forecast.condition.wmoCode, false, model.iconStyle, palette.darkGround
                        )
                    ),
                    contentDescription = null,
                    modifier = GlanceModifier.size(DaysIconSize)
                )
                Spacer(modifier = GlanceModifier.height(StripInnerGap))
                Text(
                    text = Formats.temperature(forecast.highC, model.settings.units.temperature, locale),
                    style = TextStyle(color = palette.primary, fontSize = StripTempSp.sp, fontWeight = FontWeight.Medium),
                    maxLines = 1
                )
                Text(
                    text = Formats.temperature(forecast.lowC, model.settings.units.temperature, locale),
                    style = secondaryStyle(palette, StripRainSp.sp),
                    maxLines = 1
                )
            }
        }
    }
}

/** One row of hours, the strip's grid: the hour, its drawing, its temperature and, when
 * [showRain], its chance of rain. */
@Composable
private fun HoursRow(
    shown: List<com.callbackdev.chiaro.ui.today.StripHour>,
    showRain: Boolean,
    model: WidgetModel,
    palette: WidgetPalette,
    is24h: Boolean,
    locale: java.util.Locale
) {
    Row(modifier = GlanceModifier.fillMaxWidth().padding(start = StripStartInset)) {
        shown.forEach { strip ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = GlanceModifier
                    .padding(horizontal = StripCellSpacing / 2)
                    .defaultWeight()
            ) {
                Text(
                    text = Formats.hourClockLabel(strip.hour.time, is24h, locale),
                    style = secondaryStyle(palette, StripHourSp.sp),
                    maxLines = 1
                )
                Spacer(modifier = GlanceModifier.height(StripInnerGap))
                Image(
                    provider = ImageProvider(
                        ChiaroIcons.conditionRes(
                            strip.hour.condition.wmoCode,
                            strip.night,
                            model.iconStyle,
                            palette.darkGround
                        )
                    ),
                    contentDescription = null,
                    modifier = GlanceModifier.size(StripIconSize)
                )
                Spacer(modifier = GlanceModifier.height(StripInnerGap))
                Text(
                    text = Formats.temperature(
                        strip.hour.tempC, model.settings.units.temperature, locale
                    ),
                    style = TextStyle(color = palette.primary, fontSize = StripTempSp.sp, fontWeight = FontWeight.Medium),
                    maxLines = 1
                )
                // An hour with no forecast chance prints nothing under it rather
                // than a 0% it was never told (Fase 26).
                val pct = strip.hour.precipChancePct
                if (showRain && pct != null) {
                    Text(
                        text = Formats.percent(pct, locale),
                        style = TextStyle(
                            color = rainInk(pct, palette),
                            fontSize = StripRainSp.sp
                        ),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/** The widest label the strip can print in this locale and clock: «22:00» or «12 PM». */
private fun widestStripLabel(context: Context, is24h: Boolean, locale: java.util.Locale): Dp =
    listOf(10, 12, 22).maxOf { hour ->
        measureWidgetText(
            context,
            Formats.hourClockLabel(java.time.LocalDateTime.of(2026, 1, 1, hour, 0), is24h, locale),
            StripHourSp,
            medium = false
        )
    }
