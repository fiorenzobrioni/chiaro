package com.callbackdev.chiaro.widget.arc

import android.content.Context
import android.text.format.DateFormat
import androidx.annotation.DrawableRes
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.data.AppPalette
import com.callbackdev.chiaro.data.WeatherIcons
import com.callbackdev.chiaro.domain.model.MoonPhase
import com.callbackdev.chiaro.ui.format.Formats
import com.callbackdev.chiaro.ui.icons.ChiaroIcons
import com.callbackdev.chiaro.ui.sky.SkyText
import com.callbackdev.chiaro.ui.today.TimelineItem
import com.callbackdev.chiaro.ui.today.TimelineKind
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Every sentence the arc widget prints, built off a [Context] so the Glance card and
 * the configuration screen's preview say the same words. The agenda rows speak the
 * Today screen's own timeline strings; the hero adds «at 19:42» and the countdown.
 */
internal object ArcText {

    /** The agenda row's words: exactly the Today screen's timeline row. */
    fun rowLabel(context: Context, item: TimelineItem): String = when (item.kind) {
        TimelineKind.SUNRISE -> context.getString(R.string.tl_sunrise)
        TimelineKind.GOLDEN_MORNING_END -> context.getString(R.string.tl_golden_morning_end)
        TimelineKind.GOLDEN_EVENING -> context.getString(R.string.tl_golden_evening)
        TimelineKind.SUNSET -> context.getString(R.string.tl_sunset)
        TimelineKind.BLUE_EVENING -> context.getString(R.string.tl_blue_evening)
        TimelineKind.DARK -> context.getString(R.string.tl_dark)
        TimelineKind.MOONRISE -> context.getString(R.string.tl_moonrise)
        TimelineKind.MOONSET -> context.getString(R.string.tl_moonset)
        // The screen's sentence («Sole basso e pioggia (88%): un arcobaleno starebbe a
        // ovest») is a line of prose the card has no width for (device report, 9 set: cut
        // at «stareb…»); the row keeps its two facts, the chance and where to look.
        TimelineKind.RAINBOW -> context.getString(
            R.string.arc_rainbow_row,
            item.pct ?: 0,
            context.getString(SkyText.bearingRes(item.bearingDeg ?: 0.0))
        )
        TimelineKind.RAIN_START -> context.getString(R.string.tl_rain_start, item.pct ?: 0)
        TimelineKind.RAIN_STOP -> context.getString(R.string.tl_rain_stop)
    }

    /**
     * The hero's name for a moment: the row's words, except where the row carries a
     * figure or a whole clause — «Rain gets likely (60%) at 17:00» is not a sentence
     * anybody says, so the hero uses the short name and the row keeps the number.
     */
    fun heroLabel(context: Context, item: TimelineItem): String = when (item.kind) {
        TimelineKind.RAIN_START -> context.getString(R.string.arc_rain_likely)
        TimelineKind.RAINBOW -> context.getString(R.string.arc_rainbow_short)
        else -> rowLabel(context, item)
    }

    /** The moment's name with its day marker before it: «Tomorrow · Sunrise». */
    fun heroName(context: Context, event: ArcEvent): String {
        val name = heroLabel(context, event.item)
        return if (event.tomorrow) context.getString(R.string.arc_tomorrow_name, name) else name
    }

    /** «19:42 · in 8 h»: the clock and the countdown on one line, for the one-row card. */
    fun clockLine(context: Context, series: ArcSeries, event: ArcEvent, zone: ZoneId): String =
        context.getString(
            R.string.arc_next_short,
            clock(context, event.at, zone),
            countdown(context, series.now, event.at)
        )

    /** «Sunset at 19:42», or «Sunrise tomorrow at 06:50» for a moment past midnight. */
    fun heroSentence(context: Context, event: ArcEvent, zone: ZoneId): String = context.getString(
        if (event.tomorrow) R.string.arc_next_tomorrow_at else R.string.arc_next_at,
        heroLabel(context, event.item),
        clock(context, event.at, zone)
    )

    /** The one-row card's version: «Sunset · 19:42», the day marker before the name. */
    fun heroShort(context: Context, event: ArcEvent, zone: ZoneId): String =
        context.getString(R.string.arc_next_short, heroName(context, event), clock(context, event.at, zone))

    /**
     * «in 8 h»: how far off the moment is, COARSELY. A widget repaints when a sync lands
     * — hourly by default, never by the minute — so «in 7 h 43 min» would still be on the
     * card an hour later, exact to the minute and wrong by an hour (the first version
     * printed exactly that, 9 set 2026). Hours are rounded to the nearest, under an hour
     * says only «within the hour», and a moment at hand says so; the clock time beside
     * it is the exact fact, and it never goes stale.
     */
    fun countdown(context: Context, now: Instant, at: Instant): String {
        val minutes = Duration.between(now, at).toMinutes()
        return when {
            minutes < 1 -> context.getString(R.string.arc_in_moments)
            minutes < 60 -> context.getString(R.string.arc_within_hour)
            else -> context.getString(R.string.arc_in_hours, ((minutes + 30) / 60).toInt())
        }
    }

    /** A clock time in the reader's 12/24-hour setting, at the place's zone. */
    fun clock(context: Context, at: Instant, zone: ZoneId): String =
        at.atZone(zone).format(timeFormatter(context))

    fun timeFormatter(context: Context): DateTimeFormatter =
        Formats.timeFormatter(DateFormat.is24HourFormat(context), Locale.getDefault())

    /** The week strip's day: «Today» for today, the locale's short weekday otherwise. */
    fun dayLabel(context: Context, date: LocalDate, today: LocalDate): String =
        if (date == today) context.getString(R.string.week_today) else Formats.dayLabel(date, Locale.getDefault())

    /**
     * The glyph before an agenda row: the Today screen's own choice per kind, in the
     * card's icon family. Meteocons has no rainbow, so the row that promises one shows
     * the weather a rainbow is made of — the screen's own reasoning.
     *
     * The moon's rows are the exception (device report, 9 set 2026: «the moon is cut off
     * at the bottom»). Meteocons draws moonrise and moonset as a disc clipped by the
     * horizon with a 2-unit line under it; the screen shows them at 34 dp, where the line
     * is a dp wide and the picture reads, but at a row's 20 dp the line is 0.6 dp and
     * vanishes, leaving a moon with its bottom missing. The row draws the moon in its real
     * phase at [at] instead — whole, and the same moon the arc itself paints — and the
     * words say whether it rises or sets.
     */
    @DrawableRes
    fun rowIconRes(
        kind: TimelineKind,
        at: Instant,
        style: WeatherIcons,
        darkGround: Boolean,
        palette: AppPalette
    ): Int {
        val line = when (kind) {
            TimelineKind.SUNRISE -> R.drawable.mc_sunrise
            TimelineKind.GOLDEN_MORNING_END, TimelineKind.GOLDEN_EVENING -> R.drawable.mc_horizon
            TimelineKind.SUNSET -> R.drawable.mc_sunset
            TimelineKind.BLUE_EVENING -> R.drawable.mc_star
            TimelineKind.DARK -> R.drawable.mc_starry_night
            TimelineKind.MOONRISE, TimelineKind.MOONSET ->
                return ChiaroIcons.moonPhaseRes(MoonPhase.at(at), style, darkGround, palette)
            TimelineKind.RAINBOW -> R.drawable.mc_partly_cloudy_day_rain
            TimelineKind.RAIN_START -> R.drawable.mc_raindrops
            TimelineKind.RAIN_STOP -> R.drawable.mc_cloudy
        }
        return ChiaroIcons.styledRes(line, style, darkGround, palette)
    }

    /**
     * What the arc says to a screen reader: the next moment and when, so the picture
     * has its text equivalent (DESIGN §9.3) even on the one-cell card that prints
     * nothing but a figure.
     */
    fun description(context: Context, series: ArcSeries, zone: ZoneId): String {
        val next = series.nextLight ?: return context.getString(R.string.arc_desc_no_moment)
        return context.getString(
            R.string.arc_desc_next,
            heroSentence(context, next, zone),
            countdown(context, series.now, next.at)
        )
    }
}
