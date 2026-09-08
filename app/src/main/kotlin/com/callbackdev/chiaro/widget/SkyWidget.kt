package com.callbackdev.chiaro.widget

import android.content.Context
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
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
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
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.data.AppPalette
import com.callbackdev.chiaro.data.WeatherIcons
import com.callbackdev.chiaro.domain.model.MoonPhase
import com.callbackdev.chiaro.domain.sky.SkyVerdict
import com.callbackdev.chiaro.ui.format.Formats
import com.callbackdev.chiaro.ui.icons.ChiaroIcons
import com.callbackdev.chiaro.ui.sky.SkyText
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The Sky widget (VISION §5.9): the subscribed moments in front of the reader and
 * their verdicts — the widget nobody else ships. A verdict is a word with its number,
 * in the same fixed colors the app uses; when the forecast cannot judge yet, the
 * widget says that instead of guessing.
 *
 * Three forms, one per kind of grant, laid out on the Now widget's grammar
 * ([SkyWidgetLayout.kt]): the moment's time as the hero number over its name, the
 * glyph filling the height, the verdict against the far edge where the card is wide
 * and as a mark before the name where it is not, and on a tall card the moments after
 * this one as a list — as many as the height honestly holds, never more than the
 * reader subscribed to. All of it off the same ordered list the Sky screen reads
 * ([com.callbackdev.chiaro.ui.sky.SkyUpcoming.allAt]), so home and screen cannot print
 * two different sunrises.
 */
class SkyWidget : GlanceAppWidget() {

    /** Exact sizing so [LocalSize] is the size the launcher really granted: the form,
     * the glyph and the row count are all read off it. */
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
            val size = LocalSize.current
            val hasContent = model.city != null && model.moments.isNotEmpty()
            val tall = hasContent && skyIsTall(size)
            // The same edge rule as the Now widget: a glyph edge takes the glyph's inset,
            // a words edge the words'. The glyph leads every form and owns the top; the
            // far edge always carries words or a chip; the bottom is the glyph's on one
            // row and the last row's words on a tall card. Empty states are words only.
            WidgetCard(
                model, schemes, skyBitmap,
                contentPaddingStart =
                    if (hasContent) WidgetCardPaddingLeading else WidgetCardPadding,
                contentPaddingEnd = WidgetCardPaddingTrailing,
                contentPaddingTop = if (hasContent) WidgetCardPaddingSnug else WidgetCardPadding,
                contentPaddingBottom =
                    if (hasContent && !tall) WidgetCardPaddingSnug else WidgetCardPadding
            ) { palette ->
                when {
                    model.city == null -> NoPlaceContent(palette)
                    model.moments.isEmpty() -> NoMomentContent(palette)
                    tall -> TallContent(model, palette, size)
                    else -> HeroRow(
                        model, model.moments.first(), palette,
                        glyph = skyHeroIconSize(size),
                        wide = skyIsWide(size),
                        modifier = GlanceModifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

/**
 * The moment in front of the reader, as one row: the glyph; the clock over the name,
 * with the day marker before the name when the moment is not today's; and the verdict.
 *
 * On a wide card the verdict is the word chip over the number that decided it, in a
 * column of its own against the far edge, centred on the row like the Now widget's
 * sentence. On a narrow card it is the mark — the series' `✓ ~ ✗ ?` in the verdict's
 * own container — before the name, where a word chip would push «Domani · Sorge la
 * luna» off the card. A verdict is a glyph and a word before it is a color (DESIGN
 * §2.3); the narrow card keeps the glyph, the wide one adds the word and the number,
 * and the color rides along on both.
 */
@Composable
private fun HeroRow(
    model: WidgetModel,
    moment: NextMoment,
    palette: WidgetPalette,
    glyph: Dp,
    wide: Boolean,
    modifier: GlanceModifier
) {
    val context = LocalContext.current
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        MomentGlyph(model, moment, palette, glyph)
        // The bottom padding balances the leading above the clock's digits, so the
        // words' ink and the glyph's ink share a centre line (the Now widget's finding,
        // 5th device pass).
        Column(
            modifier = GlanceModifier
                .padding(start = IconTextGap, bottom = textInkBalance(context, SkyTimeSp))
                .defaultWeight()
        ) {
            Text(
                text = heroClock(context, model, moment),
                style = TextStyle(
                    color = palette.primary,
                    fontSize = SkyTimeSp.sp,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!wide) {
                    moment.verdict?.let { verdict ->
                        VerdictMark(verdict, palette)
                        Spacer(modifier = GlanceModifier.width(MarkGap))
                    }
                }
                Text(
                    text = heroLabel(context, model, moment),
                    style = secondaryStyle(palette, SkyNameSp.sp),
                    maxLines = 1
                )
            }
        }
        if (wide) {
            moment.verdict?.let { verdict ->
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = GlanceModifier
                        .padding(start = SentenceGap)
                        .width(SkyVerdictColumn)
                ) {
                    VerdictChip(verdict, palette)
                    evidence(context, verdict)?.let { number ->
                        Text(
                            text = number,
                            style = TextStyle(
                                color = palette.secondary,
                                fontSize = SkyEvidenceSp.sp,
                                textAlign = TextAlign.End
                            ),
                            maxLines = 1,
                            modifier = GlanceModifier.fillMaxWidth().padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * The tall card: the hero row at the head, and under it the moments behind it, one per
 * row, as many as [skyRows] says the height holds. The block sits in the middle of the
 * room it does not fill (committente, 4 set): four subscriptions on a card with room
 * for six should look composed, not interrupted.
 */
@Composable
private fun TallContent(model: WidgetModel, palette: WidgetPalette, size: DpSize) {
    val context = LocalContext.current
    val wide = skyIsWide(size)
    val rows = skyRows(size, fontScale(context), available = model.moments.size - 1)
    Column(modifier = GlanceModifier.fillMaxSize()) {
        Spacer(modifier = GlanceModifier.defaultWeight())
        HeroRow(
            model, model.moments.first(), palette,
            glyph = SkyTallHeroIcon,
            wide = wide,
            modifier = GlanceModifier.fillMaxWidth()
        )
        if (rows > 0) {
            Spacer(modifier = GlanceModifier.height(SkyListGap))
            model.moments.drop(1).take(rows).forEachIndexed { index, next ->
                if (index > 0) Spacer(modifier = GlanceModifier.height(SkyRowGap))
                CompactRow(model, next, palette, wide)
            }
        }
        Spacer(modifier = GlanceModifier.defaultWeight())
    }
}

/**
 * One further moment on a single line: small glyph, name, when, verdict. Name and time
 * share a size so their baselines meet under centre alignment; only the name gives
 * ground on a narrow card, because the clock carries the day marker and a bare «06:47»
 * on a home screen reads as this morning's (committente, 3 set). The verdict is the
 * word chip where the card is wide and the mark where it is not — the same rule as
 * the hero above it, so a card speaks one register top to bottom.
 */
@Composable
private fun CompactRow(
    model: WidgetModel,
    moment: NextMoment,
    palette: WidgetPalette,
    wide: Boolean
) {
    val context = LocalContext.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(SkyRowHeight)
            .padding(start = SkyRowIndent)
    ) {
        MomentGlyph(model, moment, palette, SkyRowGlyph)
        Text(
            text = context.getString(SkyText.nameRes(moment.job.id)),
            style = TextStyle(color = palette.primary, fontSize = SkyCompactSp.sp),
            maxLines = 1,
            modifier = GlanceModifier.padding(start = 8.dp).defaultWeight()
        )
        Text(
            text = rowClock(context, model, moment),
            style = secondaryStyle(palette, SkyCompactSp.sp),
            maxLines = 1,
            modifier = GlanceModifier.padding(start = MarkGap)
        )
        moment.verdict?.let { verdict ->
            Spacer(modifier = GlanceModifier.width(MarkGap))
            if (wide) VerdictChip(verdict, palette) else VerdictMark(verdict, palette)
        }
    }
}

@Composable
private fun MomentGlyph(
    model: WidgetModel,
    moment: NextMoment,
    palette: WidgetPalette,
    size: Dp
) {
    Image(
        provider = ImageProvider(
            skyJobIconRes(moment, model.iconStyle, palette.darkGround, model.settings.palette)
        ),
        contentDescription = null, // the name says it in words
        modifier = GlanceModifier.size(size)
    )
}

/**
 * The word, in its measured container (committente, 4 set — a bare green was hard to
 * read on a dark card). The app's verdict inks are measured against the app's SURFACE,
 * and a widget's ground is a scrimmed sky, a wallpaper, or whatever the reader chose;
 * ink and container are a PAIR (`PaletteContrastTest`), so a chip carries its own
 * ground and is legible on any card. The word and nothing else: the number sits under
 * it on the hero, and one tap away in a row.
 */
@Composable
private fun VerdictChip(verdict: SkyVerdict, palette: WidgetPalette) {
    val context = LocalContext.current
    val night = isNight(context)
    Row(
        modifier = GlanceModifier
            .background(verdictContainer(verdict.kind, night, palette.dress))
            .cornerRadius(10.dp)
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(
            text = context.getString(SkyText.verdictWordRes(verdict.kind)),
            style = TextStyle(
                color = verdictInk(verdict.kind, night, palette.dress),
                fontSize = SkyChipSp.sp
            ),
            maxLines = 1
        )
    }
}

/**
 * The mark: the verdict's own glyph — `✓ ~ ✗ ?`, the series' vocabulary and the same
 * characters the app's chip opens with — alone in a round container of the verdict's
 * colors. It is the verdict at the size a narrow card can afford: still a shape before
 * it is a color, which is what keeps it readable under deuteranopia (DESIGN §2.3), and
 * the word is one form up or one tap away. A fixed box, so a fallback font's taller
 * check mark cannot make one row bounce against the next.
 */
@Composable
private fun VerdictMark(verdict: SkyVerdict, palette: WidgetPalette) {
    val context = LocalContext.current
    val night = isNight(context)
    Box(
        contentAlignment = Alignment.Center,
        modifier = GlanceModifier
            .size(SkyMarkChip)
            .background(verdictContainer(verdict.kind, night, palette.dress))
            .cornerRadius(SkyMarkChip / 2)
    ) {
        Text(
            text = verdict.kind.glyph,
            style = TextStyle(
                color = verdictInk(verdict.kind, night, palette.dress),
                fontSize = SkyMarkSp.sp,
                fontWeight = FontWeight.Medium
            ),
            maxLines = 1
        )
    }
}

/** The air between a mark and the words beside it, and before a row's clock. */
private val MarkGap = 6.dp

/**
 * The hero number. The moment's start, as a rule; for a window that is open right
 * now, its END — the next thing that happens, and the one a reader standing in the
 * golden hour wants to know. The label under it says «Adesso», so the two read as
 * «now, until 20:20». Windows print their start alone otherwise: «19:55 – 20:20» at
 * 30 sp is 200 dp, and the closing time is on the screen one tap away.
 */
private fun heroClock(context: Context, model: WidgetModel, moment: NextMoment): String {
    val at: Instant = moment.end?.takeIf { moment.inProgress } ?: moment.start
    return at.atZone(model.zone).format(timeFormatter(context))
}

/**
 * Under the clock: the day marker, then the name — in that order, so when the column
 * runs out it is the name's tail that goes, never the word that says WHICH day. Today's
 * moments carry no marker; a time with nothing before it means today, as on the Sky
 * screen. Anything past tomorrow carries its date, because a word for it would be a
 * guess at how the reader counts days.
 */
private fun heroLabel(context: Context, model: WidgetModel, moment: NextMoment): String {
    val name = context.getString(SkyText.nameRes(moment.job.id))
    return listOfNotNull(dayMark(context, model, moment), name).joinToString(" · ")
}

/** A row's clock: the day marker and the start, the Sky screen's own phrase. */
private fun rowClock(context: Context, model: WidgetModel, moment: NextMoment): String {
    val start = moment.start.atZone(model.zone).format(timeFormatter(context))
    return listOfNotNull(dayMark(context, model, moment), start).joinToString(" · ")
}

private fun dayMark(context: Context, model: WidgetModel, moment: NextMoment): String? {
    val date = moment.start.atZone(model.zone).toLocalDate()
    val today = LocalDate.now(model.zone)
    return when {
        moment.inProgress -> context.getString(R.string.sky_moment_now)
        date == today -> null
        date == today.plusDays(1) -> context.getString(R.string.sky_day_tomorrow)
        else -> date.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))
    }
}

private fun timeFormatter(context: Context): DateTimeFormatter = Formats.timeFormatter(
    android.text.format.DateFormat.is24HourFormat(context), Locale.getDefault()
)

/**
 * The number under the word chip: the one that decided the verdict, in the Sky
 * screen's own words («nuvole 10%»). Null for an UNKNOWN — not knowing has no
 * arithmetic, and its reason is a sentence the screen has room for and this card has
 * not; the word «Presto per dirlo» is the honest whole of what the card can say.
 */
private fun evidence(context: Context, verdict: SkyVerdict): String? =
    SkyText.chipEvidence(context.resources, verdict)

/** The moon's day-moment gets its real phase; everything else its family glyph. */
private fun skyJobIconRes(
    moment: NextMoment,
    style: WeatherIcons,
    darkGround: Boolean,
    appPalette: AppPalette
): Int = when (moment.job.id) {
    "sun.rise", "twilight.civil.am" ->
        ChiaroIcons.styledRes(R.drawable.mc_sunrise, style, darkGround, appPalette)
    "sun.set", "twilight.civil.pm" ->
        ChiaroIcons.styledRes(R.drawable.mc_sunset, style, darkGround, appPalette)
    "solar.noon" ->
        ChiaroIcons.conditionRes(
            0, night = false, style = style, darkGround = darkGround,
            palette = appPalette
        )
    "golden_hour.am", "golden_hour.pm" ->
        ChiaroIcons.styledRes(R.drawable.mc_horizon, style, darkGround, appPalette)
    "blue_hour.am", "blue_hour.pm",
    "twilight.nautical.am", "twilight.nautical.pm" ->
        ChiaroIcons.styledRes(R.drawable.mc_star, style, darkGround, appPalette)
    "twilight.astronomical.am", "twilight.astronomical.pm", "darkness.window" ->
        ChiaroIcons.styledRes(R.drawable.mc_starry_night, style, darkGround, appPalette)
    "moon.rise" -> ChiaroIcons.styledRes(R.drawable.mc_moonrise, style, darkGround, appPalette)
    "moon.set" -> ChiaroIcons.styledRes(R.drawable.mc_moonset, style, darkGround, appPalette)
    "moon.today", "moon.phase" ->
        ChiaroIcons.moonPhaseRes(MoonPhase.FULL_MOON, style, darkGround, appPalette)
    "equinox.spring", "solstice.summer", "equinox.autumn", "solstice.winter" ->
        ChiaroIcons.styledRes(R.drawable.mc_horizon, style, darkGround, appPalette)
    else -> ChiaroIcons.styledRes(R.drawable.mc_falling_stars, style, darkGround, appPalette)
}

/** Subscriptions emptied by hand: the widget says why it is quiet, never blanks —
 * centred on the card like every other empty state (committente, 7 set). */
@Composable
private fun NoMomentContent(palette: WidgetPalette) {
    val context = LocalContext.current
    Column(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        Text(
            text = context.getString(R.string.widget_sky_empty),
            style = secondaryStyle(palette, 12.sp)
        )
    }
}
