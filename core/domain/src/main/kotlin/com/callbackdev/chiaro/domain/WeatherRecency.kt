package com.callbackdev.chiaro.domain

import com.callbackdev.chiaro.domain.model.HourlyForecast
import com.callbackdev.chiaro.domain.model.WeatherReport
import java.time.Duration
import java.time.Instant

/**
 * Which part of a report is still about NOW (Fase 17).
 *
 * The companion to [WeatherFreshness], and a different question: freshness says
 * whether the numbers still count as current, recency says which ROWS of them have
 * not already happened. The two are needed together the moment the app is allowed to
 * show a report it could not refresh — a forecast fetched three hours ago opens with
 * three hours that are over, and printing them under `## Next hours` would not be
 * old data, it would be wrong data.
 *
 * Nothing here invents or shifts a value: it only drops what the clock has already
 * passed. What survives is what that fetch always said about the hours ahead.
 *
 * A report carries a **week** of hourly rows and seven daily ones (`HOURLY_WINDOW`),
 * so this is far from academic: yesterday's fetch still holds a full forecast for
 * today onward, and it is the only forecast an offline phone has.
 */
object WeatherRecency {

    /**
     * [report] without the hours and days that have already gone by, in the city's
     * own timezone. A no-op for a report fetched within the current hour, which is
     * why the caller can apply it unconditionally.
     *
     * The current hour is KEPT: it is the slot `current_conditions` and every engine
     * read at fetch time, and both renderers drop it themselves ([WeatherReport]'s
     * `hourly[0]` is "the hour we are in" by contract).
     *
     * An hour is over when its END has passed, read off [HourlyForecast.at] and not off
     * its label (18 ott 2026). The label is not an identity: the night a zone falls back
     * two rows say 02:00, and comparing labels kept the first of them — an hour already
     * gone — as `hourly[0]` for the whole of the second, so the hero's hour and the strip
     * were one row late for that hour. The days stay on the place's calendar, which is
     * what a date is.
     */
    fun trim(report: WeatherReport, now: Instant): WeatherReport {
        val today = now.atZone(report.zone()).toLocalDate()
        val hourly = report.hourly.filter { it.at.plus(HOUR).isAfter(now) }
        val daily = report.daily.filterNot { it.date.isBefore(today) }
        return if (hourly.size == report.hourly.size && daily.size == report.daily.size) {
            report
        } else {
            report.copy(hourly = hourly, daily = daily)
        }
    }

    /**
     * Whether [report] still says anything about the present: after [trim] it has at
     * least one hour and one day left.
     *
     * This is the honest expiry of a cached report, and it is DERIVED rather than
     * chosen — no "show it for up to N hours" constant. Past the forecast horizon a
     * report is not stale data, it is a record of a week that is over: `## Current`
     * would be an old observation, `## Today` would be somebody else's day, and the
     * two tables would be empty. There is nothing left to be honest about.
     */
    fun coversNow(report: WeatherReport, now: Instant): Boolean =
        trim(report, now).let { it.hourly.isNotEmpty() && it.daily.isNotEmpty() }

    /** A row is the hour that starts at it. */
    private val HOUR: Duration = Duration.ofHours(1)
}
