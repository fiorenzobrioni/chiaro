package com.callbackdev.chiaro.widget.window

import android.content.Context
import android.graphics.Bitmap
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider as FixedColorProvider
import com.callbackdev.chiaro.MainActivity
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.ui.format.LocalClock
import com.callbackdev.chiaro.ui.shell.ShellDestination
import com.callbackdev.chiaro.widget.ChiaroWidgetReceiver
import com.callbackdev.chiaro.widget.CompactRow
import com.callbackdev.chiaro.widget.LaterTable
import com.callbackdev.chiaro.widget.TextLaterMinRows
import com.callbackdev.chiaro.widget.laterHours
import com.callbackdev.chiaro.widget.textLaterHeight
import com.callbackdev.chiaro.widget.NoDataContent
import com.callbackdev.chiaro.widget.NoPlaceContent
import com.callbackdev.chiaro.widget.SkyRowGap
import com.callbackdev.chiaro.widget.WarningChipRow
import com.callbackdev.chiaro.widget.WidgetBackground
import com.callbackdev.chiaro.widget.WidgetCard
import com.callbackdev.chiaro.widget.WidgetCardPadding
import com.callbackdev.chiaro.widget.WidgetData
import com.callbackdev.chiaro.widget.WidgetModel
import com.callbackdev.chiaro.widget.WidgetRefresh
import com.callbackdev.chiaro.widget.WidgetSchemes
import com.callbackdev.chiaro.widget.fontScale
import com.callbackdev.chiaro.widget.isNight
import com.callbackdev.chiaro.widget.rememberSkyBitmap
import com.callbackdev.chiaro.widget.rememberWidgetModel
import com.callbackdev.chiaro.widget.rememberWidgetSchemes
import com.callbackdev.chiaro.widget.resolveWidgetPalette
import com.callbackdev.chiaro.widget.secondaryStyle
import com.callbackdev.chiaro.widget.marked
import com.callbackdev.chiaro.widget.markerText
import com.callbackdev.chiaro.widget.widgetCardFill
import java.time.Instant
import java.time.ZoneId

class WindowWidgetReceiver : ChiaroWidgetReceiver() {
    override val glanceAppWidget = WindowWidget()
}

/**
 * «The window» (20 ott 2026, VISION §5.9): the sky of this hour over the reader's place on
 * the home screen, drawn by the hero's own scene — the gradient, the clouds, the sun, the
 * moon as it looks, the stars that are there, Venus and Jupiter among them — into the
 * card's box, with the place and one sentence at its foot: tonight's verdict with the
 * numbers that decided it, or the next moment of light with its verdict. The brightest
 * things in the window are named where they stand, and the caption says where the window
 * looks. Taller, it carries the moments the reader follows with their verdicts, the Sky
 * card's own rows. It replaced the day's arc, a chart of the sky, with the sky. A tap opens
 * the app where the reader left it, as the other cards do: the picture is Today's hero.
 *
 * It resizes from two cells by one to four by four and reshapes itself at every step
 * ([WindowLayout.kt]). Like every card of the family it never invents: no place and no
 * data are said, old data states its age, and what is drawn is the cached report's hour.
 */
class WindowWidget : GlanceAppWidget() {

    /** Exact sizing: the picture's box, the rows and the caption are all read off the
     * size the launcher really granted. */
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = runCatching {
            GlanceAppWidgetManager(context).getAppWidgetId(id)
        }.getOrDefault(0)
        val loadedAt = WidgetRefresh.revision.value
        val initial = WidgetData.load(context, appWidgetId)
        provideContent {
            val model = rememberWidgetModel(context, appWidgetId, initial, loadedAt)
            val schemes = rememberWidgetSchemes(
                context, model.settings.dynamicColor, model.settings.palette
            )
            WindowWidgetContent(model, schemes, rememberSkyBitmap(model))
        }
    }
}

/**
 * The whole card for [model] at the launcher's [LocalSize], split off `provideGlance` so
 * the configuration screen's preview and the render tests draw the SAME composition the
 * launcher does. [skyBitmap] is the family's gradient dress, used only by the plain card
 * the window falls back to when there is nothing to draw: the window's own ground is
 * its picture.
 */
@Composable
internal fun WindowWidgetContent(model: WidgetModel, schemes: WidgetSchemes, skyBitmap: Bitmap?) {
    val context = LocalContext.current
    val size = LocalSize.current
    val content = model.content
    val city = model.city
    if (content == null || city == null) {
        WidgetCard(model, schemes, skyBitmap) { palette ->
            if (city == null) NoPlaceContent(palette) else NoDataContent(palette)
        }
        return
    }
    val warning = model.warning?.takeIf { model.look.showWarning }
    val scale = fontScale(context)
    val plan = windowPlan(size, scale, model.moments.size, warning != null, weather = model.look.windowWeather)
    val clock = LocalClock.current
    val at = Instant.now(clock)
    val picture = remember(model, size, plan) { WindowPainter.paint(context, model, size, plan, at) }
    // The ink of a card whose ground is the sky, at full opacity: the scrim in the picture
    // is what keeps the words legible, and the family's own resolution picks the ink.
    val palette = resolveWidgetPalette(
        context, model.look.copy(background = WidgetBackground.SKY, opacityPct = 100), schemes, hasSky = true
    )
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .cornerRadius(android.R.dimen.system_app_widget_background_radius)
            // The app, wherever the reader left it, as the other cards open it (committente,
            // 20 ott 2026, from the device): the picture is Today's own hero, and a tap on
            // the sky lands on the sky, bigger, with its numbers under it.
            .clickable(
                androidx.glance.appwidget.action.actionStartActivity(
                    ShellDestination.intent(context, MainActivity::class.java, null)
                )
            )
    ) {
        if (picture != null) {
            Image(
                provider = ImageProvider(picture.bitmap),
                contentDescription = null, // the caption says what the picture shows
                contentScale = ContentScale.FillBounds,
                modifier = GlanceModifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = GlanceModifier.fillMaxSize().background(
                    FixedColorProvider(widgetCardFill(WidgetBackground.COLOR, schemes, isNight(context), 1f, model.look.cardColor))
                )
            ) {}
        }
        Column(modifier = GlanceModifier.fillMaxSize()) {
            Box(modifier = GlanceModifier.fillMaxWidth().height(plan.skyHeight)) {
                if (plan.weather || (warning != null && plan.warning)) {
                    Column(modifier = GlanceModifier.padding(WindowCaptionPadding)) {
                        if (plan.weather) {
                            // The hour's weather, when asked for (committente, 20 ott 2026): the
                            // temperature and the sky's word, and nothing else, so the picture
                            // stays the subject; the painter lays a scrim under them.
                            Text(
                                text = WindowText.weatherLine(context, content, model),
                                style = TextStyle(
                                    color = palette.primary, fontSize = WindowPlaceSp.sp, fontWeight = FontWeight.Medium
                                ),
                                maxLines = 1
                            )
                        }
                        if (warning != null && plan.warning) {
                            WarningChipRow(warning.maxLevel, palette, topGap = if (plan.weather) 6.dp else 0.dp)
                        }
                    }
                }
                Column(
                    modifier = GlanceModifier.fillMaxSize().padding(
                        start = WindowCaptionPadding, end = WindowCaptionPadding, bottom = WindowCaptionPadding
                    ),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (clockDiffers(model.zone, at)) {
                            // The place's own time, live, when its clock is not the phone's
                            // (committente, 20 ott 2026: a night sky at three in the afternoon asks
                            // what time it is there). The platform's TextClock, kept by the
                            // launcher minute by minute in the place's zone: a time painted into
                            // the picture would be a minute stale, then an hour. The name and the
                            // clock are one platform layout aligned by baseline (the layout says
                            // why), so the clock stands on the place's line in any face.
                            AndroidRemoteViews(
                                remoteViews = RemoteViews(context.packageName, R.layout.widget_window_name).apply {
                                    setTextViewText(R.id.window_name, city.name)
                                    setTextColor(R.id.window_name, palette.primary.getColor(context).toArgb())
                                    setString(R.id.window_clock, "setTimeZone", model.zone.id)
                                    setTextColor(R.id.window_clock, palette.secondary.getColor(context).toArgb())
                                },
                                modifier = GlanceModifier.defaultWeight()
                            )
                        } else {
                            Text(
                                text = city.name,
                                style = TextStyle(
                                    color = palette.primary, fontSize = WindowPlaceSp.sp, fontWeight = FontWeight.Medium
                                ),
                                maxLines = 1,
                                modifier = GlanceModifier.defaultWeight()
                            )
                        }
                        // Old data says its age where the compass would be: the age is the
                        // one thing a reader must not miss, and the two are read last alike.
                        val aside = when {
                            content.marked -> markerText(context, content, at)
                            plan.compass && picture?.night == true ->
                                WindowText.looking(context, picture.facingDeg, picture.drawnAt, model.zone)
                            else -> null
                        }
                        if (aside != null) {
                            Text(
                                text = aside,
                                style = secondaryStyle(palette, WindowCaptionSp.sp),
                                maxLines = 1,
                                modifier = GlanceModifier.padding(start = 8.dp)
                            )
                        }
                    }
                    Text(
                        // One moon number per card: the disc's when the moon is named in the picture.
                        text = WindowText.caption(
                            context, model, picture?.night ?: false, at, withMoon = picture?.moonNamed != true
                        ),
                        style = TextStyle(color = palette.primary, fontSize = WindowSentenceSp.sp),
                        maxLines = plan.captionLines
                    )
                }
            }
            // With the hour's weather on (committente, 20 ott 2026), the tall card's rows are
            // the next hours as «In words» prints them (laterHours, LaterTable: the same table,
            // so the hours read one way on every card), in place of the sky's moments; at the
            // forecast's end, with fewer than a table's worth, the moments that fit the band.
            val hours = if (plan.hourRows > 0) laterHours(content.strip, content.now).take(plan.hourRows) else emptyList()
            val momentRows = when {
                plan.hourRows > 0 && hours.size >= TextLaterMinRows -> 0
                plan.hourRows > 0 -> ((textLaterHeight(plan.hourRows, scale) + WindowRowsPadding) / windowRowHeight(scale)).toInt()
                else -> plan.momentRows
            }
            if (hours.size >= TextLaterMinRows && plan.hourRows > 0) {
                Box(
                    modifier = GlanceModifier.fillMaxWidth().padding(
                        start = WidgetCardPadding, end = WidgetCardPadding, top = WindowRowsPadding / 2
                    )
                ) {
                    LaterTable(hours, model, palette, scale, width = size.width - WidgetCardPadding * 2, edgeGive = 0.dp)
                }
            } else if (momentRows > 0) {
                // The Sky card's own rows, so the two lists say a verdict one way. The air
                // between rows is padding inside each row, for Glance's ten-children cap.
                val shown = model.moments.take(momentRows)
                val marks = shown.any { it.verdict != null }
                Column(
                    modifier = GlanceModifier.fillMaxWidth().padding(
                        start = WidgetCardPadding, end = WidgetCardPadding, top = WindowRowsPadding / 2
                    )
                ) {
                    shown.forEachIndexed { index, moment ->
                        CompactRow(model, moment, palette, marks, topGap = if (index > 0) SkyRowGap else 0.dp)
                    }
                }
            }
        }
    }
}

/** Whether the place keeps a different time from the phone at [at]: by offset, not by zone id,
 * so Paris does not get a clock on a phone in Rome. */
internal fun clockDiffers(zone: ZoneId, at: Instant): Boolean =
    zone.rules.getOffset(at) != ZoneId.systemDefault().rules.getOffset(at)
