package com.callbackdev.chiaro.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.ui.format.Formats
import com.callbackdev.chiaro.ui.icons.ChiaroIcons
import com.callbackdev.chiaro.ui.today.TodayUiState
import java.time.Instant
import java.util.Locale

/**
 * The Now widget (VISION §5.9): icon, temperature, place — the glance in the word's
 * old sense. Stale data states its age in the freshness amber; no place says so and
 * the tap opens the app; nothing here is ever a guess.
 */
class NowWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val model = WidgetData.load(context)
        val schemes = widgetSchemes(context, model.settings.dynamicColor)
        provideContent {
            WidgetCard(schemes, model.settings) {
                when (val content = model.content) {
                    null -> if (model.city == null) NoPlaceContent() else NoDataContent()
                    else -> NowContent(content, model)
                }
            }
        }
    }
}

@Composable
private fun NowContent(content: TodayUiState.Content, model: WidgetModel) {
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
            modifier = GlanceModifier.size(44.dp)
        )
        Column(modifier = GlanceModifier.padding(start = 10.dp)) {
            Text(
                text = Formats.temperature(
                    current.tempC, model.settings.units.temperature, locale
                ),
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Medium
                )
            )
            Text(text = content.city.name, style = secondaryStyle(12.sp))
            if (content.isStale) {
                Text(
                    text = staleText(context, content.lastSync, Instant.now()),
                    style = TextStyle(color = FreshnessInk, fontSize = 11.sp)
                )
            }
        }
    }
}

/** A place with no report yet: said plainly, never a grey zero (DESIGN §1.1). */
@Composable
fun NoDataContent() {
    val context = LocalContext.current
    Column {
        Text(
            text = context.getString(R.string.widget_no_data),
            style = secondaryStyle(12.sp)
        )
    }
}
