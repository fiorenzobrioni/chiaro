package com.callbackdev.chiaro.widget

import android.content.Context
import androidx.compose.runtime.Composable
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
import com.callbackdev.chiaro.ui.today.HeadlineText
import com.callbackdev.chiaro.ui.today.TodayUiState
import java.time.Instant
import java.util.Locale

/**
 * The Today widget (VISION §5.9): the Now block, the headline sentence when there is
 * one, and the next hours — all straight out of [com.callbackdev.chiaro.ui.today
 * .TodayStateBuilder], so the widget and the app can never tell two stories about
 * the same afternoon.
 */
class TodayWidget : GlanceAppWidget() {

    /** Exact sizing so [LocalSize] is the width the launcher really granted — the
     * strip reads it to decide how many hours honestly fit. */
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = runCatching {
            GlanceAppWidgetManager(context).getAppWidgetId(id)
        }.getOrDefault(0)
        val model = WidgetData.load(context, appWidgetId)
        val schemes = widgetSchemes(context, model.settings.dynamicColor)
        val skyBitmap = model.content
            ?.takeIf { model.look.background == WidgetBackground.SKY }
            ?.let { skyGradientBitmap(it.sky, model.look.opacityPct) }
        provideContent {
            WidgetCard(model, schemes, skyBitmap) { palette ->
                when (val content = model.content) {
                    null -> if (model.city == null) {
                        NoPlaceContent(palette)
                    } else {
                        NoDataContent(palette)
                    }
                    else -> TodayContent(content, model, palette)
                }
            }
        }
    }

    private companion object {
        /** The hero number's size, named because the ink balance beside it is
         * measured off the leading this size carries. */
        const val TemperatureSp = 36f

        /** What the sentence and the hour strip need under the hero row, so the
         * icon can take the rest of the granted height (device review, 3 set). */
        val HeroReserve = 116.dp

        /** The strip sizes itself to the width the launcher actually granted: a
         * cell under this width squeezes its numbers, and fewer than four hours is
         * no longer an afternoon. At the 4-cell minimum this lands on the same five
         * cells the strip always had; wider widgets get their sixth and seventh. */
        val StripCellMin = 38.dp
        val StripCellSpacing = 6.dp
        const val StripCellsFloor = 4
        const val StripCellsCeiling = 7
    }

    @Composable
    private fun TodayContent(
        content: TodayUiState.Content,
        model: WidgetModel,
        palette: WidgetPalette
    ) {
        val context = LocalContext.current
        val locale = Locale.getDefault()
        val is24h = android.text.format.DateFormat.is24HourFormat(context)
        val timeFmt = Formats.timeFormatter(is24h, locale)
        val current = content.report.current

        Column(modifier = GlanceModifier.fillMaxSize()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = GlanceModifier.fillMaxWidth()
            ) {
                Image(
                    provider = ImageProvider(
                        ChiaroIcons.conditionRes(
                            current.condition.wmoCode, content.night,
                            model.settings.weatherIcons, palette.darkGround
                        )
                    ),
                    contentDescription = null,
                    modifier = GlanceModifier.size(
                        heroIconSize(
                            LocalSize.current.height - WidgetCardPadding * 2 - HeroReserve
                        )
                    )
                )
                // The bottom padding balances the leading above the temperature, so
                // the words' ink lines up with the glyph's rather than the two boxes
                // lining up (the Now widget's finding, 5th device pass).
                Column(
                    modifier = GlanceModifier
                        .padding(start = 12.dp, bottom = textInkBalance(context, TemperatureSp))
                ) {
                    // The temperature and the place, and nothing between them: the
                    // day's range left the hero with the Now widget's (committente,
                    // 3 set) — the week's rows are where a range belongs.
                    Text(
                        text = Formats.temperature(
                            current.tempC, model.settings.units.temperature, locale
                        ),
                        style = TextStyle(
                            color = palette.primary,
                            fontSize = TemperatureSp.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Text(
                        text = content.city.name,
                        style = secondaryStyle(palette, 16.sp),
                        maxLines = 1
                    )
                }
                Spacer(modifier = GlanceModifier.defaultWeight())
                if (content.isStale) {
                    Text(
                        text = staleText(context, content.lastSync, Instant.now()),
                        style = TextStyle(color = palette.stale, fontSize = 11.sp)
                    )
                }
            }

            HeadlineText.of(context, content.headline, timeFmt)?.let { sentence ->
                Text(
                    text = sentence,
                    style = TextStyle(
                        color = palette.primary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 2,
                    modifier = GlanceModifier.padding(top = 6.dp)
                )
            }

            Spacer(modifier = GlanceModifier.defaultWeight())

            val cells = (
                (LocalSize.current.width - WidgetCardPadding * 2 + StripCellSpacing) /
                    (StripCellMin + StripCellSpacing)
                ).toInt().coerceIn(StripCellsFloor, StripCellsCeiling)
            val shown = content.strip.take(cells)
            // The rain row appears when any visible hour has something to report:
            // then EVERY cell prints its figure (a 0% next to an 80% is information),
            // and on a dry stretch the whole row stays home — the app strip's rule,
            // sized for a launcher.
            val showRain = shown.any { it.hour.precipChancePct > 0 }
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                shown.forEachIndexed { index, strip ->
                    if (index > 0) Spacer(modifier = GlanceModifier.width(StripCellSpacing))
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = GlanceModifier.defaultWeight()
                    ) {
                        Text(
                            text = Formats.hourLabel(strip.hour.time, is24h, locale),
                            style = secondaryStyle(palette, 12.sp)
                        )
                        Spacer(modifier = GlanceModifier.height(2.dp))
                        Image(
                            provider = ImageProvider(
                                ChiaroIcons.conditionRes(
                                    strip.hour.condition.wmoCode,
                                    strip.night,
                                    model.settings.weatherIcons,
                                    palette.darkGround
                                )
                            ),
                            contentDescription = null,
                            modifier = GlanceModifier.size(32.dp)
                        )
                        Spacer(modifier = GlanceModifier.height(2.dp))
                        Text(
                            text = Formats.temperature(
                                strip.hour.tempC, model.settings.units.temperature, locale
                            ),
                            style = TextStyle(color = palette.primary, fontSize = 14.sp)
                        )
                        if (showRain) {
                            Text(
                                text = "${strip.hour.precipChancePct}%",
                                style = TextStyle(
                                    color = rainInk(strip.hour.precipChancePct, palette),
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
