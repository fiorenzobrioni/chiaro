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
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
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
        /** Cells in the strip: enough shape to plan the afternoon, few enough that
         * every number stays readable at 4 launcher cells. */
        const val STRIP_CELLS = 5
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
                            current.condition.wmoCode, content.night, model.settings.weatherIcons
                        )
                    ),
                    contentDescription = null,
                    modifier = GlanceModifier.size(52.dp)
                )
                Column(modifier = GlanceModifier.padding(start = 12.dp)) {
                    Text(
                        text = Formats.temperature(
                            current.tempC, model.settings.units.temperature, locale
                        ),
                        style = TextStyle(
                            color = palette.primary,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Text(
                        text = content.city.name,
                        style = secondaryStyle(palette, 13.sp),
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

            Row(modifier = GlanceModifier.fillMaxWidth()) {
                content.strip.take(STRIP_CELLS).forEachIndexed { index, strip ->
                    if (index > 0) Spacer(modifier = GlanceModifier.width(6.dp))
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = GlanceModifier.defaultWeight()
                    ) {
                        Text(
                            text = Formats.hourLabel(strip.hour.time, is24h, locale),
                            style = secondaryStyle(palette, 11.sp)
                        )
                        Spacer(modifier = GlanceModifier.height(2.dp))
                        Image(
                            provider = ImageProvider(
                                ChiaroIcons.conditionRes(
                                    strip.hour.condition.wmoCode,
                                    strip.night,
                                    model.settings.weatherIcons
                                )
                            ),
                            contentDescription = null,
                            modifier = GlanceModifier.size(28.dp)
                        )
                        Spacer(modifier = GlanceModifier.height(2.dp))
                        Text(
                            text = Formats.temperature(
                                strip.hour.tempC, model.settings.units.temperature, locale
                            ),
                            style = TextStyle(color = palette.primary, fontSize = 12.sp)
                        )
                    }
                }
            }
        }
    }
}
