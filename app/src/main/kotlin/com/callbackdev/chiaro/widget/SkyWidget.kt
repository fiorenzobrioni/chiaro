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
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The Sky widget (VISION §5.9): the next subscribed moment and its verdict — the
 * widget nobody else ships. The verdict is a word with its number, in the same fixed
 * colors the app uses; when the forecast cannot judge yet, the widget says that
 * instead of guessing.
 */
class SkyWidget : GlanceAppWidget() {

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
                when {
                    model.city == null -> NoPlaceContent(palette)
                    model.nextMoment == null -> NoMomentContent(palette)
                    else -> SkyContent(model, model.nextMoment, palette)
                }
            }
        }
    }
}

@Composable
private fun SkyContent(model: WidgetModel, moment: NextMoment, palette: WidgetPalette) {
    val context = LocalContext.current
    val locale = Locale.getDefault()
    val is24h = android.text.format.DateFormat.is24HourFormat(context)
    val timeFmt = Formats.timeFormatter(is24h, locale)

    val start = moment.start.atZone(model.zone).format(timeFmt)
    val clock = moment.end
        ?.let { "$start – ${it.atZone(model.zone).format(timeFmt)}" }
        ?: start
    // Which day that clock belongs to, in the Sky screen's own words: at nine in the
    // evening the next sunrise is TOMORROW's, and a bare "06:47" on a home screen
    // reads as this morning's (committente, 3 set). Today's moments carry no marker;
    // anything past tomorrow carries its date, because a word for it would be a
    // guess at how the reader counts days.
    val date = moment.start.atZone(model.zone).toLocalDate()
    val today = LocalDate.now(model.zone)
    val dayMark = when {
        moment.inProgress -> context.getString(R.string.sky_moment_now)
        date == today -> null
        date == today.plusDays(1) -> context.getString(R.string.sky_day_tomorrow)
        else -> date.format(DateTimeFormatter.ofPattern("d MMM", locale))
    }
    val timeLine = listOfNotNull(dayMark, clock).joinToString(" · ")

    Column(modifier = GlanceModifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                provider = ImageProvider(
                    skyJobIconRes(moment, model.settings.weatherIcons, palette.darkGround)
                ),
                contentDescription = null,
                modifier = GlanceModifier.size(48.dp)
            )
            Column(modifier = GlanceModifier.padding(start = 12.dp)) {
                Text(
                    text = context.getString(SkyText.nameRes(moment.job.id)),
                    style = TextStyle(
                        color = palette.primary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1
                )
                Text(text = timeLine, style = secondaryStyle(palette, 12.sp))
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
    val night = isNight(context)
    val word = context.getString(SkyText.verdictWordRes(verdict.kind))
    val detail = if (verdict.kind == SkyVerdictKind.UNKNOWN) {
        SkyText.unknownReason(context.resources, verdict)
    } else {
        SkyText.chipEvidence(context.resources, verdict)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = GlanceModifier
            .background(verdictContainer(verdict.kind, night))
            // 48dp icon and a 4dp-tall pill: on a one-cell grant the first cut
            // (54dp, 5dp) left the pill's bottom outside the card (screenshot, 3 set).
            .cornerRadius(12.dp)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = if (detail != null) "$word · $detail" else word,
            style = TextStyle(color = verdictInk(verdict.kind, night), fontSize = 12.sp),
            maxLines = 1
        )
    }
}

/** The moon's day-moment gets its real phase; everything else its family glyph. */
private fun skyJobIconRes(
    moment: NextMoment,
    style: WeatherIcons,
    darkGround: Boolean
): Int = when (moment.job.id) {
    "sun.rise", "twilight.civil.am" -> ChiaroIcons.styledRes(R.drawable.mc_sunrise, style, darkGround)
    "sun.set", "twilight.civil.pm" -> ChiaroIcons.styledRes(R.drawable.mc_sunset, style, darkGround)
    "solar.noon" ->
        ChiaroIcons.conditionRes(0, night = false, style = style, darkGround = darkGround)
    "golden_hour.am", "golden_hour.pm" -> ChiaroIcons.styledRes(R.drawable.mc_horizon, style, darkGround)
    "blue_hour.am", "blue_hour.pm",
    "twilight.nautical.am", "twilight.nautical.pm" ->
        ChiaroIcons.styledRes(R.drawable.mc_star, style, darkGround)
    "twilight.astronomical.am", "twilight.astronomical.pm", "darkness.window" ->
        ChiaroIcons.styledRes(R.drawable.mc_starry_night, style, darkGround)
    "moon.rise" -> ChiaroIcons.styledRes(R.drawable.mc_moonrise, style, darkGround)
    "moon.set" -> ChiaroIcons.styledRes(R.drawable.mc_moonset, style, darkGround)
    "moon.today", "moon.phase" -> ChiaroIcons.moonPhaseRes(MoonPhase.FULL_MOON, style, darkGround)
    "equinox.spring", "solstice.summer", "equinox.autumn", "solstice.winter" ->
        ChiaroIcons.styledRes(R.drawable.mc_horizon, style, darkGround)
    else -> ChiaroIcons.styledRes(R.drawable.mc_falling_stars, style, darkGround)
}

/** Subscriptions emptied by hand: the widget says why it is quiet, never blanks. */
@Composable
private fun NoMomentContent(palette: WidgetPalette) {
    val context = LocalContext.current
    Column {
        Text(
            text = context.getString(R.string.widget_sky_empty),
            style = secondaryStyle(palette, 12.sp)
        )
    }
}
