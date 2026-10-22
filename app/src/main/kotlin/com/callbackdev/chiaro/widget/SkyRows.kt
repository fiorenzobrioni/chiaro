package com.callbackdev.chiaro.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.data.WeatherIcons
import com.callbackdev.chiaro.domain.sky.SkyVerdict
import com.callbackdev.chiaro.ui.format.Formats
import com.callbackdev.chiaro.ui.format.LocalClock
import com.callbackdev.chiaro.ui.icons.ChiaroIcons
import com.callbackdev.chiaro.ui.sky.SkyText
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/*
 * The rows of the sky's moments on a home-screen card (22 ott 2026): what survives of the
 * Sky moments widget, retired from the picker on 13 ott 2026 and removed on 22 ott, because
 * the window widget draws its list under the picture and a card kept only for the home
 * screens that still had it was an implementation kept for compatibility, which this
 * product does not do (committente). The rules below are the ones the Sky card wrote for
 * its list: one row per moment, the glyph, the short name, the clock with its day marker,
 * and the verdict as a bare mark in a slot every row keeps when any row has one.
 */

/**
 * One further moment on a single line: small glyph, name, when, verdict. Name and time
 * at one size, so their baselines meet under centre alignment (device finding, 4 set
 * 2026). The verdict is the note ([VerdictNote]) on every width: a row is a list's, and
 * a list says a verdict one way.
 */
@Composable
internal fun CompactRow(
    model: WidgetModel,
    moment: NextMoment,
    palette: WidgetPalette,
    /** Whether any row on the card has a verdict: then every row keeps the note's slot,
     * so the clocks stand in one column (the arc's agenda found it first). */
    marks: Boolean,
    /** The air above this row, taken as padding INSIDE a taller box (the arc widget's
     * own cure): the row still occupies its height plus the gap, and the column still
     * has one child per row rather than two. */
    topGap: Dp = 0.dp
) {
    val context = LocalContext.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(SkyRowHeight + topGap)
            .padding(top = topGap, start = SkyRowIndent)
    ) {
        MomentGlyph(model, moment, palette, SkyRowGlyph)
        Text(
            text = context.getString(SkyText.shortNameRes(moment.job.id)),
            style = TextStyle(color = palette.primary, fontSize = SkyCompactSp.sp),
            maxLines = 1,
            modifier = GlanceModifier.padding(start = 8.dp).defaultWeight()
        )
        Text(
            text = rowClock(context, model, moment, placeToday(model)),
            style = secondaryStyle(palette, SkyCompactSp.sp),
            maxLines = 1,
            modifier = GlanceModifier.padding(start = MarkGap)
        )
        if (marks) {
            Spacer(modifier = GlanceModifier.width(MarkGap))
            Box(contentAlignment = Alignment.Center, modifier = GlanceModifier.width(SkyNoteSlot)) {
                moment.verdict?.let { VerdictNote(it, palette) }
            }
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
            skyJobIconRes(moment, model.iconStyle, palette.darkGround, row = size <= SkyRowGlyph)
        ),
        contentDescription = null, // the name says it in words
        modifier = GlanceModifier.size(size)
    )
}

/** A row's verdict: the series' mark alone, in the ink measured for the card's ground. */
@Composable
internal fun VerdictNote(verdict: SkyVerdict, palette: WidgetPalette) {
    val context = LocalContext.current
    Image(
        provider = ImageProvider(ChiaroIcons.verdictMarkRes(verdict.kind)),
        contentDescription = context.getString(SkyText.verdictWordRes(verdict.kind)),
        colorFilter = ColorFilter.tint(verdictInk(verdict.kind, palette.darkGround, palette.dress)),
        modifier = GlanceModifier.size(SkyMarkGlyph)
    )
}

private val MarkGap = 6.dp

private fun rowClock(context: Context, model: WidgetModel, moment: NextMoment, today: LocalDate): String {
    val start = moment.start.atZone(model.zone).format(timeFormatter(context))
    return listOfNotNull(dayMark(context, model, moment, today), start).joinToString(" · ")
}

@Composable
private fun placeToday(model: WidgetModel): LocalDate =
    LocalDate.now(LocalClock.current.withZone(model.zone))

/** «Now», «Tomorrow», «3 Oct», or nothing for today: the day before the clock. */
private fun dayMark(context: Context, model: WidgetModel, moment: NextMoment, today: LocalDate): String? {
    val date = moment.start.atZone(model.zone).toLocalDate()
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
 * The drawing of a moment on a card: the job's own, or the moon's phase for the moon's
 * day-moment. A list row's 20 dp glyph: a phase there takes the filled family (13 ott
 * 2026), because a line-drawn full moon at that size is a bare ring.
 */
private fun skyJobIconRes(
    moment: NextMoment,
    style: WeatherIcons,
    darkGround: Boolean,
    row: Boolean = false
): Int = moment.moonPhase?.let { ChiaroIcons.moonPhaseRes(it, if (row) WeatherIcons.FILL else style, darkGround) }
    ?: ChiaroIcons.skyJobRes(moment.job.id, style, darkGround)

/** A row's verdict note sits in a slot this wide on every row of a card that has one,
 * so the clocks stand in a column: the arc's agenda's own 14 dp. */
internal val SkyNoteSlot = 14.dp

/** The mark's drawing: 12 dp, the size the glyph had as a character. */
internal val SkyMarkGlyph = 12.dp

/** A compact row's two texts share one size, so their baselines meet under centre
 * alignment (device finding, 4 set). */
internal const val SkyCompactSp = WidgetType.Label

internal val SkyRowGlyph = 20.dp

/** A row is its 22 dp mark and a dp of air either side; the 13 sp texts are shorter. */
internal val SkyRowHeight = 24.dp
internal val SkyRowGap = 6.dp

/**
 * The rows' small glyphs start this far in from where a hero's does, so their ink lines
 * up under its ink rather than under its box: a 60 dp Meteocons box keeps ~8 dp empty
 * on the leading side, a 20 dp one ~3, and 6 is the difference.
 */
internal val SkyRowIndent = 6.dp
