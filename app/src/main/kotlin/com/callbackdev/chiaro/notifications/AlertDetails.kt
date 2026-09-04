package com.callbackdev.chiaro.notifications

import com.callbackdev.chiaro.domain.AlertEngine
import com.callbackdev.chiaro.domain.model.HourlyForecast
import java.time.LocalDateTime

/**
 * The stretch of hours an alert is actually about, read off the same forecast the
 * engine judged (Fase 6b, device request of 4 set: the expanded notification says
 * more than the collapsed one, and everything it says has to come from the data).
 *
 * The alert itself carries one hour — the first one that crossed the threshold —
 * because that is all its fingerprint needs. A reader deciding whether to move a
 * bike ride needs the rest of it: when the weather lets up, how bad it gets at its
 * worst, and what the temperature does meanwhile.
 */
data class AlertWindow(
    /** First hour of the run. */
    val start: LocalDateTime,
    /** Last hour of the run — equal to [start] when it is one hour long. */
    val end: LocalDateTime,
    val peakPrecipPct: Int,
    val peakPrecipAt: LocalDateTime,
    val lowC: Double,
    val highC: Double,
    /**
     * The run reached the end of the forecast instead of a calm hour, so [end] is
     * where the DATA stops, not where the weather does. The notification then says
     * "from 17:00" and nothing more: claiming an end the forecast never showed would
     * be the screen lying (DESIGN §1.1), in the one place the reader cannot check it.
     */
    val openEnded: Boolean
) {
    val singleHour: Boolean get() = start == end
}

/** Pure: no clock, no resources, no Android — the notifier does the words. */
object AlertDetails {

    /** The run of severe hours containing [at], by the engine's own bucket table. */
    fun severeWindow(hours: List<HourlyForecast>, at: LocalDateTime): AlertWindow? =
        window(hours, at) { it.condition.wmoCode in AlertEngine.SevereCodes }

    /** The run of hours at or above the warning threshold containing [at]. */
    fun rainWindow(
        hours: List<HourlyForecast>,
        at: LocalDateTime,
        thresholdPct: Int = AlertEngine.PRECIP_THRESHOLD_PCT
    ): AlertWindow? = window(hours, at) { it.precipChancePct >= thresholdPct }

    /**
     * The maximal run of consecutive hours around [at] for which [holds] is true.
     * Null when [at] is not in [hours] at all — a cached report whose hours have
     * already elapsed can outlive the alert that was built from it, and an empty
     * answer is the honest one.
     */
    private fun window(
        hours: List<HourlyForecast>,
        at: LocalDateTime,
        holds: (HourlyForecast) -> Boolean
    ): AlertWindow? {
        val index = hours.indexOfFirst { it.time == at }.takeIf { it >= 0 } ?: return null
        if (!holds(hours[index])) return null
        var first = index
        while (first > 0 && holds(hours[first - 1])) first--
        var last = index
        while (last < hours.lastIndex && holds(hours[last + 1])) last++
        val run = hours.subList(first, last + 1)
        val peak = run.maxBy { it.precipChancePct }
        return AlertWindow(
            start = run.first().time,
            end = run.last().time,
            peakPrecipPct = peak.precipChancePct,
            peakPrecipAt = peak.time,
            lowC = run.minOf { it.tempC },
            highC = run.maxOf { it.tempC },
            openEnded = last == hours.lastIndex
        )
    }
}
