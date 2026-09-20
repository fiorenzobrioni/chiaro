package com.callbackdev.chiaro.notifications

import com.callbackdev.chiaro.domain.AlertEngine
import com.callbackdev.chiaro.domain.model.Coordinates
import com.callbackdev.chiaro.domain.model.HourlyForecast
import com.callbackdev.chiaro.domain.sky.AstronomyEngine
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * What the evening summary can say beyond its own sentence: the night between the
 * reader and tomorrow, and the parts of tomorrow that ask something of them tonight.
 *
 * The companion of [AlertDetails], and pure for the same reason — no clock, no
 * resources, no Android, so every number the expanded notification prints has a
 * table test behind it. The notifier does the words.
 *
 * Everything here is read off the very report the engine judged, except the sun,
 * which is computed by [AstronomyEngine]: the provider's daily block carries today's
 * sunrise, and by 20:00 today's sunrise is the one fact nobody needs.
 */
object EveningDetails {

    /**
     * Where the night ends when the sun does not say: the report is a week long, but
     * inside the polar night there is no sunrise to end it, and a "night" that ran to
     * the next evening would report a low nobody would call tonight's.
     */
    private val FALLBACK_NIGHT_END: LocalTime = LocalTime.of(6, 0)

    /**
     * The night between now and sunrise, as the two facts that decide anything about
     * it: how cold it gets and when, and whether water falls on it.
     */
    data class NightAhead(
        val lowC: Double,
        val lowAt: LocalDateTime,
        /**
         * Peak chance of precipitation over the night, **null when not one of its
         * hours carried one** — the provider's probability is model-dependent all the
         * way to the screen (CLAUDE.md §1.1), and a night reported at 0% is a forecast
         * of a dry night, which is not what "we were not told" means.
         */
        val peakPrecipPct: Int?,
        val peakPrecipAt: LocalDateTime?
    )

    /** A chance of rain and the hour it peaks at — tomorrow's, when it has no run. */
    data class RainPeak(val pct: Int, val at: LocalDateTime)

    /**
     * Tomorrow's two ends and what they add up to, with the change against today:
     * this is the daylight edition, and "four minutes less than today" is the one
     * number on the whole notification that no other weather app bothers to print.
     */
    data class TomorrowSun(
        val sunrise: LocalTime,
        val sunset: LocalTime,
        val daylight: Duration,
        /** Tomorrow minus today; null when today's own daylight is unknown. */
        val daylightDelta: Duration?
    )

    /**
     * The coldest hour between [from] and sunrise, and the wettest.
     *
     * [hours] is the report's own list, which in the worker is NOT trimmed — the
     * recency trim runs in the UI layer, which is why [from] is a parameter and not
     * `hours.first().time`: without it the "night" would start at this morning's
     * coldest hour and report a low that has already happened.
     *
     * Null when no hour falls inside the night: a cached report can outlive its own
     * hours, and an empty answer is the honest one — the notifier then draws no night
     * line rather than a line of dashes.
     */
    fun night(
        hours: List<HourlyForecast>,
        from: LocalDateTime,
        until: LocalDateTime
    ): NightAhead? {
        val run = hours.filter { !it.time.isBefore(from) && !it.time.isAfter(until) }
        if (run.isEmpty()) return null
        val coldest = run.minBy { it.tempC }
        val wettest = run.filter { it.precipChancePct != null }.maxByOrNull { it.precipChancePct!! }
        return NightAhead(
            lowC = coldest.tempC,
            lowAt = coldest.time,
            peakPrecipPct = wettest?.precipChancePct,
            peakPrecipAt = wettest?.time
        )
    }

    /**
     * When the night is over: tomorrow's sunrise at this place, or 06:00 where there
     * is none to have. Local to the city, like every other time in an alert.
     */
    fun nightEnd(tomorrow: LocalDate, zone: ZoneId, coords: Coordinates): LocalDateTime =
        AstronomyEngine
            .sunCrossing(tomorrow, zone, coords, AstronomyEngine.SUNRISE_ALTITUDE, rising = true)
            ?.atZone(zone)?.toLocalDateTime()
            ?: tomorrow.atTime(FALLBACK_NIGHT_END)

    /**
     * The stretch of TOMORROW at or over the umbrella threshold — the same bar the
     * rain warning uses, because the app must not hold two opinions on what counts as
     * rain coming.
     *
     * Deliberately computed over tomorrow's hours ALONE: a run that starts at 23:00
     * tonight is tonight's rain, and the line that prints this one says "tomorrow".
     * A run still going at midnight therefore comes back open-ended ("from 21:00 on"),
     * which is what the data says once the day is the unit.
     */
    fun tomorrowRain(
        hours: List<HourlyForecast>,
        tomorrow: LocalDate,
        thresholdPct: Int = AlertEngine.PRECIP_THRESHOLD_PCT
    ): AlertWindow? {
        val day = hours.filter { it.time.toLocalDate() == tomorrow }
        val first = day.firstOrNull { (it.precipChancePct ?: 0) >= thresholdPct } ?: return null
        return AlertDetails.rainWindow(day, first.time, thresholdPct)
    }

    /**
     * Tomorrow's worst hour for rain, for the days that never reach the umbrella bar
     * but are not dry either. Null when no hour of tomorrow carries a probability at
     * all — see [NightAhead.peakPrecipPct].
     */
    fun tomorrowRainPeak(hours: List<HourlyForecast>, tomorrow: LocalDate): RainPeak? =
        hours.filter { it.time.toLocalDate() == tomorrow && it.precipChancePct != null }
            .maxByOrNull { it.precipChancePct!! }
            ?.let { RainPeak(it.precipChancePct!!, it.time) }

    /**
     * Tomorrow's sunrise and sunset, and how much daylight that is against [today].
     * Null when either end does not happen — above the Arctic circle half a pair says
     * less than nothing, which is the rule [com.callbackdev.chiaro.domain.model
     * .Astronomical] was made nullable for.
     */
    fun sun(
        tomorrow: LocalDate,
        zone: ZoneId,
        coords: Coordinates,
        today: Duration?
    ): TomorrowSun? {
        fun cross(rising: Boolean) = AstronomyEngine.sunCrossing(
            tomorrow, zone, coords, AstronomyEngine.SUNRISE_ALTITUDE, rising
        )?.atZone(zone)?.toLocalDateTime()
        val sunrise = cross(rising = true) ?: return null
        val sunset = cross(rising = false) ?: return null
        val daylight = Duration.between(sunrise, sunset)
        if (daylight.isNegative || daylight.isZero) return null
        return TomorrowSun(
            sunrise = sunrise.toLocalTime(),
            sunset = sunset.toLocalTime(),
            daylight = daylight,
            daylightDelta = today?.let { daylight.minus(it) }
        )
    }
}
