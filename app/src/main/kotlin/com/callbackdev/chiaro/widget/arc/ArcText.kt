package com.callbackdev.chiaro.widget.arc

import android.content.Context
import android.text.format.DateFormat
import androidx.annotation.DrawableRes
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.data.AppPalette
import com.callbackdev.chiaro.data.WeatherIcons
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
        TimelineKind.RAINBOW -> context.getString(
            R.string.timeline_rainbow,
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

    /** «Sunset at 19:42», or «Sunrise tomorrow at 06:50» for a moment past midnight. */
    fun heroSentence(context: Context, event: ArcEvent, zone: ZoneId): String = context.getString(
        if (event.tomorrow) R.string.arc_next_tomorrow_at else R.string.arc_next_at,
        heroLabel(context, event.item),
        clock(context, event.at, zone)
    )

    /** The one-row card's version: «Sunset · 19:42», the day marker before the name. */
    fun heroShort(context: Context, event: ArcEvent, zone: ZoneId): String {
        val name = heroLabel(context, event.item)
        val label = if (event.tomorrow) {
            context.getString(R.string.arc_tomorrow_name, name)
        } else {
            name
        }
        return context.getString(R.string.arc_next_short, label, clock(context, event.at, zone))
    }

    /**
     * «in 2 h 10 min»: how far off the moment is. Whole hours print without the
     * minutes, under an hour without the hours, and a moment already at hand says so.
     * The count is against the clock, not the forecast, so it is right even when the
     * report is stale.
     */
    fun countdown(context: Context, now: Instant, at: Instant): String {
        val minutes = Duration.between(now, at).toMinutes()
        val hours = minutes / 60
        val rest = (minutes % 60).toInt()
        return when {
            minutes < 1 -> context.getString(R.string.arc_in_moments)
            minutes < 60 -> context.getString(R.string.arc_in_minutes, minutes.toInt())
            rest == 0 -> context.getString(R.string.arc_in_hours, hours.toInt())
            else -> context.getString(R.string.arc_in_hours_minutes, hours.toInt(), rest)
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
     */
    @DrawableRes
    fun rowIconRes(
        kind: TimelineKind,
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
            TimelineKind.MOONRISE -> R.drawable.mc_moonrise
            TimelineKind.MOONSET -> R.drawable.mc_moonset
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
        val next = series.next ?: return context.getString(R.string.arc_desc_no_moment)
        return context.getString(
            R.string.arc_desc_next,
            heroSentence(context, next, zone),
            countdown(context, series.now, next.at)
        )
    }
}
