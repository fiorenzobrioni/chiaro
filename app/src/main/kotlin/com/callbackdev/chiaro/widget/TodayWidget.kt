package com.callbackdev.chiaro.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
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
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.callbackdev.chiaro.ui.format.Formats
import com.callbackdev.chiaro.ui.icons.ChiaroIcons
import com.callbackdev.chiaro.ui.today.TodayUiState
import java.time.Instant
import java.util.Locale

/**
 * The Today widget (VISION §5.9): now plus the next hours — the Now widget's one-row
 * form as the head of the card, and under it the hour strip, all straight out of
 * [com.callbackdev.chiaro.ui.today.TodayStateBuilder], so the widget and the app can
 * never tell two stories about the same afternoon. Laid out again on 8 set 2026 on the
 * Now widget's grammar ([TodayWidgetLayout.kt] carries the budget and the reasons).
 */
class TodayWidget : GlanceAppWidget() {

    /** Exact sizing so [LocalSize] is the size the launcher really granted: the strip
     * reads the width for its hour count, the hero band the height for its glyph. */
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = runCatching {
            GlanceAppWidgetManager(context).getAppWidgetId(id)
        }.getOrDefault(0)
        // The revision is read BEFORE the load, so the model composed below is only
        // re-read when something really changed after it (see [WidgetRefresh]: Glance
        // does not run this function again while the session is alive, which is why
        // everything the widget draws is observed from inside the composition).
        val loadedAt = WidgetRefresh.revision.value
        val initial = WidgetData.load(context, appWidgetId)
        provideContent {
            val model = rememberWidgetModel(context, appWidgetId, initial, loadedAt)
            val schemes = rememberWidgetSchemes(
                context, model.settings.dynamicColor, model.settings.palette
            )
            val skyBitmap = rememberSkyBitmap(model)
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
    }
}

@Composable
private fun TodayContent(
    content: TodayUiState.Content,
    model: WidgetModel,
    palette: WidgetPalette,
    size: DpSize
) {
    val context = LocalContext.current
    val locale = Locale.getDefault()
    val is24h = android.text.format.DateFormat.is24HourFormat(context)
    val fontScale = fontScale(context)
    val cells = todayStripCells(size.width)
    val shown = content.strip.take(cells)
    val today = content.week.firstOrNull()?.forecast
    val range = today?.takeIf { model.look.showDayRange }
    // The rain row appears when any visible hour has something to report — then EVERY
    // cell prints its figure, because a 0% next to an 80% is information (the app
    // strip's own rule) — and when it fits under the hero's words (the budget's).
    val showRain = shown.any { (it.hour.precipChancePct ?: 0) > 0 } &&
        todayShowRain(
            size, fontScale, content.isStale,
            sentence = model.look.showSentence, range = range != null
        )
    val icon = todayHeroIconSize(size, fontScale, showRain)
    val withSentence = model.look.showSentence && todayIsWide(size, icon)
    // The range wants the far edge too (committente, 4 set: «at the far edge, level with
    // the temperature»); with the sentence there it sits under it, and without a sentence
    // column to hold it — a card too narrow for one — it stays home rather than crowding
    // the number, which is the reason it left that spot on the third device pass.
    val trailing = withSentence || (range != null && todayIsWide(size, icon))

    Column(modifier = GlanceModifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = GlanceModifier.fillMaxWidth()
        ) {
            Image(
                provider = ImageProvider(
                    ChiaroIcons.conditionRes(
                        content.report.current.condition.wmoCode, content.night,
                        model.iconStyle, palette.darkGround,
                        model.settings.palette
                    )
                ),
                contentDescription = null, // the temperature and the sentence say it
                modifier = GlanceModifier.size(icon)
            )
            // The bottom padding balances the leading above the temperature, so the
            // words' ink lines up with the glyph's rather than the two boxes lining up
            // (the Now widget's finding, 5th device pass).
            Column(
                modifier = GlanceModifier
                    .padding(start = IconTextGap, bottom = textInkBalance(context, TemperatureSp))
                    .defaultWeight()
            ) {
                Text(
                    text = Formats.temperature(
                        content.report.current.tempC, model.settings.units.temperature, locale
                    ),
                    style = TextStyle(
                        color = palette.primary,
                        fontSize = TemperatureSp.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1
                )
                PlaceLine(
                    name = content.city.name,
                    fromGps = model.fromGps,
                    palette = palette,
                    size = PlaceSp.sp
                )
                if (content.isStale) {
                    Text(
                        text = staleText(context, content.lastSync, Instant.now()),
                        style = TextStyle(color = palette.stale, fontSize = StaleSp.sp),
                        maxLines = 1
                    )
                }
            }
            if (trailing) {
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = GlanceModifier
                        .padding(start = SentenceGap)
                        .defaultWeight()
                ) {
                    if (withSentence) {
                        Text(
                            text = sentence(context, content),
                            style = sentenceStyle(palette, TextAlign.End),
                            maxLines = TallSentenceMaxLines,
                            modifier = GlanceModifier.fillMaxWidth()
                        )
                    }
                    range?.let { day ->
                        DayRange(day.highC, day.lowC, model.settings.units, palette)
                    }
                }
            }
        }

        // The strip hangs from the bottom; whatever the grant leaves beyond the budget
        // is air between the two, never padding inside either.
        Spacer(modifier = GlanceModifier.defaultWeight())
        Spacer(modifier = GlanceModifier.height(StripGap))

        Row(modifier = GlanceModifier.fillMaxWidth().padding(start = StripStartInset)) {
            shown.forEachIndexed { index, strip ->
                if (index > 0) Spacer(modifier = GlanceModifier.width(StripCellSpacing))
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = GlanceModifier.defaultWeight()
                ) {
                    Text(
                        text = Formats.hourLabel(strip.hour.time, is24h, locale),
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
                                palette.darkGround,
                                model.settings.palette
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
                        style = TextStyle(color = palette.primary, fontSize = StripTempSp.sp),
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
}
