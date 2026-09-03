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
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.data.WeatherIcons
import com.callbackdev.chiaro.domain.model.MoonPhase
import com.callbackdev.chiaro.domain.sky.SkyVerdict
import com.callbackdev.chiaro.domain.sky.SkyVerdictKind
import com.callbackdev.chiaro.ui.format.Formats
import com.callbackdev.chiaro.ui.icons.ChiaroIcons
import com.callbackdev.chiaro.ui.sky.SkyText
import java.util.Locale

/**
 * The Sky widget (VISION §5.9): the next subscribed moment and its verdict — the
 * widget nobody else ships. The verdict is a word with its number, in the same fixed
 * colors the app uses; when the forecast cannot judge yet, the widget says that
 * instead of guessing.
 */
class SkyWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val model = WidgetData.load(context)
        val schemes = widgetSchemes(context, model.settings.dynamicColor)
        provideContent {
            WidgetCard(schemes, model.settings) {
                when {
                    model.city == null -> NoPlaceContent()
                    model.nextMoment == null -> NoMomentContent()
                    else -> SkyContent(model, model.nextMoment)
                }
            }
        }
    }
}

@Composable
private fun SkyContent(model: WidgetModel, moment: NextMoment) {
    val context = LocalContext.current
    val locale = Locale.getDefault()
    val is24h = android.text.format.DateFormat.is24HourFormat(context)
    val timeFmt = Formats.timeFormatter(is24h, locale)

    val start = moment.start.atZone(model.zone).format(timeFmt)
    val timeLine = moment.end
        ?.let { "$start – ${it.atZone(model.zone).format(timeFmt)}" }
        ?: start

    Column(modifier = GlanceModifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                provider = ImageProvider(skyJobIconRes(moment, model.settings.weatherIcons)),
                contentDescription = null,
                modifier = GlanceModifier.size(36.dp)
            )
            Column(modifier = GlanceModifier.padding(start = 10.dp)) {
                Text(
                    text = context.getString(SkyText.nameRes(moment.job.id)),
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1
                )
                Text(text = timeLine, style = secondaryStyle(12.sp))
            }
        }
        Spacer(modifier = GlanceModifier.defaultWeight())
        moment.verdict?.let { verdict -> VerdictPill(verdict) }
    }
}

/** DESIGN §8.7 on the launcher: the word first, the number beside it, the color
 * third — a bare colored dot on a home screen would tell some readers nothing. */
@Composable
private fun VerdictPill(verdict: SkyVerdict) {
    val context = LocalContext.current
    val word = context.getString(SkyText.verdictWordRes(verdict.kind))
    val detail = if (verdict.kind == SkyVerdictKind.UNKNOWN) {
        SkyText.unknownReason(context.resources, verdict)
    } else {
        SkyText.chipEvidence(context.resources, verdict)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = GlanceModifier
            .background(verdictContainer(verdict.kind))
            .cornerRadius(14.dp)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = if (detail != null) "$word · $detail" else word,
            style = TextStyle(color = verdictInk(verdict.kind), fontSize = 12.sp),
            maxLines = 1
        )
    }
}

/** The moon's day-moment gets its real phase; everything else its family glyph. */
private fun skyJobIconRes(moment: NextMoment, style: WeatherIcons): Int = when (moment.job.id) {
    "sun.rise", "twilight.civil.am" -> ChiaroIcons.styledRes(R.drawable.mc_sunrise, style)
    "sun.set", "twilight.civil.pm" -> ChiaroIcons.styledRes(R.drawable.mc_sunset, style)
    "solar.noon" -> ChiaroIcons.conditionRes(0, night = false, style = style)
    "golden_hour.am", "golden_hour.pm" -> ChiaroIcons.styledRes(R.drawable.mc_horizon, style)
    "blue_hour.am", "blue_hour.pm",
    "twilight.nautical.am", "twilight.nautical.pm" ->
        ChiaroIcons.styledRes(R.drawable.mc_star, style)
    "twilight.astronomical.am", "twilight.astronomical.pm", "darkness.window" ->
        ChiaroIcons.styledRes(R.drawable.mc_starry_night, style)
    "moon.rise" -> ChiaroIcons.styledRes(R.drawable.mc_moonrise, style)
    "moon.set" -> ChiaroIcons.styledRes(R.drawable.mc_moonset, style)
    "moon.today", "moon.phase" -> ChiaroIcons.moonPhaseRes(MoonPhase.FULL_MOON, style)
    "equinox.spring", "solstice.summer", "equinox.autumn", "solstice.winter" ->
        ChiaroIcons.styledRes(R.drawable.mc_horizon, style)
    else -> ChiaroIcons.styledRes(R.drawable.mc_falling_stars, style)
}

/** Subscriptions emptied by hand: the widget says why it is quiet, never blanks. */
@Composable
private fun NoMomentContent() {
    val context = LocalContext.current
    Column {
        Text(
            text = context.getString(R.string.widget_sky_empty),
            style = secondaryStyle(12.sp)
        )
    }
}
