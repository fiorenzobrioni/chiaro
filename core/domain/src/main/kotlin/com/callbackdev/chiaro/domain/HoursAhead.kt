package com.callbackdev.chiaro.domain

import com.callbackdev.chiaro.domain.model.HourlyForecast
import com.callbackdev.chiaro.domain.model.WeatherReport
import java.time.Duration
import java.time.LocalDateTime

/**
 * The next [hours] hours of this report at [now], a time on the place's clock (15.10, 29 Oct
 * 2026): the hour we are in and the ones that start after it, [hours] in all — the first
 * [hours] cells of Today's strip, «Ora» first. Read by their instants, so the night the clocks
 * go back six hours are six rows, where two of them carry the label 01:00.
 *
 * One definition for every reader that peaks, sums or counts over «the next N hours»: the
 * reader's rules (`next_Nh.*`) and the heat alert, which counts its hot hours. Until then each
 * read the rows whose label fell in `[now, now + N]`, which left the hour under way out — the
 * rain falling now, the heat of this hour — and held N + 1 hours at the top of the hour.
 *
 * Not the arrival alerts («Pioggia in arrivo», «Maltempo»): they look for the hour a run
 * STARTS, and leave the hour under way out on purpose ([AlertEngine]'s `firstArrival`).
 */
fun WeatherReport.hoursAhead(now: LocalDateTime, hours: Long): List<HourlyForecast> {
    val at = now.atZone(zone()).toInstant()
    val last = at.plus(Duration.ofHours(hours - 1))
    // A row is the hour that starts at it: the one under way has not ended yet.
    return hourly.filter { it.at.plus(Duration.ofHours(1)).isAfter(at) && !it.at.isAfter(last) }
}

/** Whether [hour], a row's label, is the hour [now] falls in: a value of it is said as now. */
fun isUnderWay(hour: LocalDateTime, now: LocalDateTime): Boolean =
    !now.isBefore(hour) && now.isBefore(hour.plusHours(1))
