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
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.callbackdev.chiaro.ui.format.Formats
import com.callbackdev.chiaro.ui.icons.ChiaroIcons
import com.callbackdev.chiaro.ui.today.TodayUiState
import java.time.Instant
import java.util.Locale

/**
 * The Now widget (VISION §5.9): icon, temperature, place — the glance in the word's
 * old sense, drawn big enough to be one (device review, 3 set). Stale data states
 * its age; no place says so; nothing here is ever a guess.
 */
class NowWidget : GlanceAppWidget() {

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
            WidgetCard(
                model, schemes, skyBitmap,
                contentPadding = WidgetCardPaddingTight
            ) { palette ->
                when (val content = model.content) {
                    null -> if (model.city == null) {
                        NoPlaceContent(palette)
                    } else {
                        NoDataContent(palette)
                    }
                    else -> NowContent(content, model, palette)
                }
            }
        }
    }
}

@Composable
private fun NowContent(
    content: TodayUiState.Content,
    model: WidgetModel,
    palette: WidgetPalette
) {
    val context = LocalContext.current
    val locale = Locale.getDefault()
    val current = content.report.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = GlanceModifier.fillMaxSize()
    ) {
        Image(
            provider = ImageProvider(
                ChiaroIcons.conditionRes(
                    current.condition.wmoCode, content.night, model.settings.weatherIcons
                )
            ),
            contentDescription = null, // the temperature and place say it in words
            modifier = GlanceModifier.size(68.dp)
        )
        Column(modifier = GlanceModifier.padding(start = 12.dp).fillMaxWidth()) {
            // The day's range sits next to the number that moves inside it.
            val range = dayRangeText(content, model.settings.units.temperature, locale)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = Formats.temperature(
                        current.tempC, model.settings.units.temperature, locale
                    ),
                    style = TextStyle(
                        color = palette.primary,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
                if (range != null) {
                    Text(
                        text = range,
                        style = secondaryStyle(palette, 13.sp),
                        maxLines = 1,
                        // 4dp of bottom inset lands the small line on the big
                        // number's baseline — two sizes top-aligned read as a
                        // mistake (the hero's own rule, device check, 2 set).
                        modifier = GlanceModifier.padding(start = 8.dp, bottom = 4.dp)
                    )
                }
            }
            Text(
                text = content.city.name,
                style = secondaryStyle(palette, 15.sp),
                maxLines = 1
            )
            if (content.isStale) {
                Text(
                    text = staleText(context, content.lastSync, Instant.now()),
                    style = TextStyle(color = palette.stale, fontSize = 11.sp)
                )
            }
        }
    }
}
