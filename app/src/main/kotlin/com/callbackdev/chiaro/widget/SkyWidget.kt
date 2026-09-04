package com.callbackdev.chiaro.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
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
import androidx.glance.layout.width
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
 * The Sky widget (VISION §5.9): the subscribed moments in front of the reader and
 * their verdicts — the widget nobody else ships. A verdict is a word with its number,
 * in the same fixed colors the app uses; when the forecast cannot judge yet, the
 * widget says that instead of guessing.
 *
 * **As many moments as the grant has room for** (committente, 4 set). It used to
 * print exactly one at every size, and could not have done otherwise: it was the one
 * widget of the three left on the default [SizeMode.Single], so [LocalSize] reported
 * the MINIMUM size from the provider and the widget never learned it had been made
 * bigger. On `Exact` it is measured, and the height decides — one cell is the hero
 * alone, exactly as before, and every cell after that adds compact rows off the same
 * ordered list ([com.callbackdev.chiaro.ui.sky.SkyUpcoming.allAt]) the Sky screen
 * reads. The list is never padded: four subscriptions draw four rows on a widget with
 * room for six, because inventing a fifth moment is the one thing this widget must
 * not do.
 */
class SkyWidget : GlanceAppWidget() {

    /** Exact sizing so [LocalSize] is the height the launcher really granted: the
     * number of moments is read off it (committente, 4 set). */
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
            val schemes = rememberWidgetSchemes(context, model.settings.dynamicColor)
            val skyBitmap = rememberSkyBitmap(model)
            WidgetCard(
                model, schemes, skyBitmap,
                contentPadding = WidgetCardPaddingTight
            ) { palette ->
                when {
                    model.city == null -> NoPlaceContent(palette)
                    model.moments.isEmpty() -> NoMomentContent(palette)
                    else -> SkyContent(model, palette)
                }
            }
        }
    }
}

/**
 * The hero moment, then as many compact rows as the granted height honestly fits.
 *
 * The budget is arithmetic on the card's own numbers rather than a table of cell
 * sizes: launchers disagree about what a cell is, and the one thing they all report
 * truthfully is how many dp they handed over.
 */
@Composable
private fun SkyContent(model: WidgetModel, palette: WidgetPalette) {
    val moment = model.moments.first()
    val extra = skyExtraRows(
        granted = LocalSize.current.height,
        heroHasVerdict = moment.verdict != null,
        available = model.moments.size - 1
    )

    Column(modifier = GlanceModifier.fillMaxSize()) {
        HeroMoment(model, moment, palette)
        // With nothing under it the pill keeps the bottom edge it was tuned against
        // on device (3 set); with rows under it, it belongs to the moment above it.
        if (extra == 0) Spacer(modifier = GlanceModifier.defaultWeight())
        moment.verdict?.let { verdict ->
            if (extra > 0) Spacer(modifier = GlanceModifier.height(RowGap))
            VerdictPill(verdict)
        }
        model.moments.drop(1).take(extra).forEach { next ->
            Spacer(modifier = GlanceModifier.height(RowGap))
            CompactMoment(model, next, palette)
        }
        if (extra > 0) Spacer(modifier = GlanceModifier.defaultWeight())
    }
}

/** The moment in front of the reader: the big glyph, the name, the clock. */
@Composable
private fun HeroMoment(model: WidgetModel, moment: NextMoment, palette: WidgetPalette) {
    val context = LocalContext.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            provider = ImageProvider(
                skyJobIconRes(moment, model.settings.weatherIcons, palette.darkGround)
            ),
            contentDescription = null,
            modifier = GlanceModifier.size(HeroIconSize)
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
            Text(
                text = timeLine(context, model, moment, range = true),
                style = secondaryStyle(palette, 12.sp)
            )
        }
    }
}

/**
 * One further moment on a single line: small glyph, name, when, and the verdict's
 * WORD in the verdict's own color.
 *
 * The word and not the pill, and no evidence number: a row this size can hold one of
 * the three, and DESIGN §2.3 is explicit about which — a verdict is a glyph and a
 * word before it is a color. The arithmetic behind it stays one tap away, on the
 * screen that has room to print it.
 *
 * Only the name gives ground when the widget is narrow: the clock carries the day
 * marker, and dropping either would hand back the bug of 3 set, where a bare "06:47"
 * on a home screen read as this morning's.
 */
@Composable
private fun CompactMoment(model: WidgetModel, moment: NextMoment, palette: WidgetPalette) {
    val context = LocalContext.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            provider = ImageProvider(
                skyJobIconRes(moment, model.settings.weatherIcons, palette.darkGround)
            ),
            contentDescription = null,
            modifier = GlanceModifier.size(CompactIconSize)
        )
        Text(
            text = context.getString(SkyText.nameRes(moment.job.id)),
            style = TextStyle(color = palette.primary, fontSize = 13.sp),
            maxLines = 1,
            modifier = GlanceModifier.padding(start = 8.dp).defaultWeight()
        )
        Text(
            text = timeLine(context, model, moment, range = false),
            style = secondaryStyle(palette, 12.sp),
            maxLines = 1
        )
        moment.verdict?.let { verdict ->
            Spacer(modifier = GlanceModifier.width(6.dp))
            Text(
                text = context.getString(SkyText.verdictWordRes(verdict.kind)),
                style = TextStyle(
                    color = verdictInk(verdict.kind, isNight(context)),
                    fontSize = 12.sp
                ),
                maxLines = 1
            )
        }
    }
}

/**
 * When the moment happens, in the Sky screen's own words: at nine in the evening the
 * next sunrise is TOMORROW's, and a bare "06:47" on a home screen reads as this
 * morning's (committente, 3 set). Today's moments carry no marker; anything past
 * tomorrow carries its date, because a word for it would be a guess at how the reader
 * counts days. [range] prints a window's closing time too — the hero has the width
 * for it, a compact row does not.
 */
private fun timeLine(
    context: Context,
    model: WidgetModel,
    moment: NextMoment,
    range: Boolean
): String {
    val locale = Locale.getDefault()
    val timeFmt = Formats.timeFormatter(
        android.text.format.DateFormat.is24HourFormat(context), locale
    )
    val start = moment.start.atZone(model.zone).format(timeFmt)
    val clock = moment.end
        ?.takeIf { range }
        ?.let { "$start – ${it.atZone(model.zone).format(timeFmt)}" }
        ?: start
    val date = moment.start.atZone(model.zone).toLocalDate()
    val today = LocalDate.now(model.zone)
    val dayMark = when {
        moment.inProgress -> context.getString(R.string.sky_moment_now)
        date == today -> null
        date == today.plusDays(1) -> context.getString(R.string.sky_day_tomorrow)
        else -> date.format(DateTimeFormatter.ofPattern("d MMM", locale))
    }
    return listOfNotNull(dayMark, clock).joinToString(" · ")
}

/**
 * How many further moments fit under the hero, given the height the launcher really
 * granted. Pure, so the budget can be pinned by a table rather than by a screenshot
 * of one phone's idea of a cell (`SkyWidgetRowsTest`).
 *
 * [available] is how many moments there ARE beyond the first: the widget never draws
 * more rows than the reader subscribed to, whatever the room.
 */
internal fun skyExtraRows(granted: Dp, heroHasVerdict: Boolean, available: Int): Int {
    val content = granted - WidgetCardPaddingTight * 2
    val hero = HeroRowHeight + if (heroHasVerdict) RowGap + VerdictPillHeight else 0.dp
    return ((content - hero) / (CompactRowHeight + RowGap))
        .toInt()
        .coerceIn(0, available.coerceAtLeast(0))
}

// The heights the row budget is arithmetic on. Each is the box its content really
// occupies, not a guess: the hero row is its icon, the pill is its 12sp text plus the
// 4dp it is padded by top and bottom, a compact row is its 20dp glyph.
private val HeroIconSize = 48.dp
private val HeroRowHeight = HeroIconSize
private val VerdictPillHeight = 26.dp
private val CompactIconSize = 20.dp
private val CompactRowHeight = CompactIconSize
private val RowGap = 6.dp


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
