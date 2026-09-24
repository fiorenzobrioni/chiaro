package com.callbackdev.chiaro.data.local

import com.callbackdev.chiaro.domain.WmoCode
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Closes the loop the Journal had left open: `ForecastDiff` records what the app
 * *said*, this records whether it was *right*.
 *
 * Everything it needs is already on disk. What was forecast for a past day D is the
 * last commit written BEFORE D began that still carried D in its horizon — literally
 * "what the app told you the evening before". What actually happened is what the
 * commits found: the sky at the moment of each (`current.wmo_code`), and the rain over
 * spans of time that each commit states for itself — the hours that had closed when it
 * was written, and the quarter of an hour behind it. "Found" is the model's own account
 * of the recent past, not a rain gauge: it is the best the app has without a station,
 * and the sentence on screen says "seen", not "measured".
 *
 * Two rules keep it from lying, and they are not symmetric on purpose:
 *
 * - **"It rained" needs one wet piece of evidence.** A positive is proof: the app saw
 *   rain, and no amount of missing hours can un-see it.
 * - **"It did not rain" needs [MIN_COVERAGE_HOURS] of the day actually covered**, and
 *   covered means exactly the spans some commit carried an amount for — merged, never
 *   stretched. Below the floor the day gets no verdict at all rather than a "stayed dry"
 *   resting on a phone that was switched off. The covered hours are carried out with
 *   the verdict so the sentence can state them, the way a sky run states its `obsMinutes`.
 *
 * **24 set 2026.** Until then a commit carried one amount, `current.precip_last_hour_mm`,
 * read as the hour behind it and in fact Open-Meteo's `current` quarter of an hour
 * (`"interval": 900`). The hourly cadence therefore covered a day with 15 minutes of
 * evidence per hour and called the rest dry, and the two-hour cadence had been given a
 * rule that let each reading vouch for up to two hours behind it to reach the floor at
 * all. Both went: each commit now carries the last [com.callbackdev.chiaro.domain.model.Precipitation.PAST_HOURS]
 * closed hours from the hourly series, so a phone that fetched every one or two hours
 * covers the day with real amounts, and one that slept six hours leaves them uncovered.
 * Commits from before keep what they really were: a quarter of an hour of evidence.
 */
object ForecastOutcome {

    /** How much of a day must be observed before the app may say "it stayed dry". */
    const val MIN_COVERAGE_HOURS = 16

    private const val DAY_HOURS = 24

    private val HOUR: Duration = Duration.ofHours(1)

    /** What `current.precipitation` sums over: Open-Meteo's 15-minutely block. */
    private val QUARTER: Duration = Duration.ofMinutes(15)

    /** One commit, as the Journal already decodes it. */
    data class Fetch(
        val timestampEpochSeconds: Long,
        /** `<ISO date>.<field>` — what this commit predicted. */
        val forecast: Map<String, String>,
        /** `current.*` — what this commit found. */
        val snapshot: Map<String, String>
    )

    /**
     * One finished day, judged. [forecastPrecipPct] is the probability the app was
     * carrying when the day began; [observedHighC] is the warmest temperature among the
     * instants the commits recorded — each closed hour's end and each reading's own —
     * which is a maximum over samples and is named "seen" on screen for that reason.
     * Both temperatures are null when the day was not covered enough to make a maximum
     * mean anything.
     */
    data class Outcome(
        val date: LocalDate,
        val forecastPrecipPct: Int,
        val rained: Boolean,
        val forecastHighC: Double?,
        val observedHighC: Double?,
        val coveredHours: Int
    )

    /**
     * Judges every finished day [fetches] can speak for, oldest first. Days on or after
     * [today] are not judged: a day still running has not turned out yet.
     */
    fun compute(fetches: List<Fetch>, zone: ZoneId, today: LocalDate): List<Outcome> {
        if (fetches.isEmpty()) return emptyList()
        val sorted = fetches.sortedBy { it.timestampEpochSeconds }
        val days = sorted
            .map { Instant.ofEpochSecond(it.timestampEpochSeconds).atZone(zone).toLocalDate() }
            .distinct()
            .filter { it.isBefore(today) }
        val observations = sorted.mapNotNull { it.observation() }
        return days.mapNotNull { date -> outcomeFor(date, sorted, observations, zone) }
    }

    private fun outcomeFor(
        date: LocalDate,
        sorted: List<Fetch>,
        observations: List<Observation>,
        zone: ZoneId
    ): Outcome? {
        val start = date.atStartOfDay(zone).toInstant()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant()

        // What the app was carrying when the day began: the newest commit from before
        // it that still had the day in its horizon. Without one there is nothing to
        // verify, and a day the app never predicted is not a miss.
        val baseline = sorted.lastOrNull {
            Instant.ofEpochSecond(it.timestampEpochSeconds).isBefore(start) &&
                it.forecast.containsKey("$date.precip_pct")
        } ?: return null
        val forecastPct = baseline.forecast["$date.precip_pct"]?.toDoubleOrNull()?.toInt()
            ?: return null
        val forecastHigh = baseline.forecast["$date.high_c"]?.toDoubleOrNull()

        var rained = false
        val covered = mutableListOf<Pair<Instant, Instant>>()
        observations.forEach { obs ->
            // A span of rain counts for the day it overlaps — a closed hour never
            // straddles midnight, the quarter behind a reading at 00:05 barely does.
            obs.spans.forEach { span ->
                val from = maxOf(span.from, start)
                val until = minOf(span.until, end)
                if (from < until) {
                    covered += Pair(from, until)
                    if (span.wet) rained = true
                }
            }
            // The code is what the sky was doing AT the reading, so it counts only
            // for the day the reading itself falls in.
            if (obs.at >= start && obs.at < end && obs.wmoCode != null &&
                WmoCode.isPrecipitation(obs.wmoCode)
            ) {
                rained = true
            }
        }
        val high = observations.flatMap { it.temps }
            .filter { (at, _) -> at >= start && at < end }
            .maxOfOrNull { (_, tempC) -> tempC }
        val coveredHours = (mergedSeconds(covered) / 3600).toInt().coerceAtMost(DAY_HOURS)
        val judged = coveredHours >= MIN_COVERAGE_HOURS
        if (!rained && !judged) return null
        return Outcome(
            date = date,
            forecastPrecipPct = forecastPct,
            rained = rained,
            forecastHighC = forecastHigh.takeIf { judged },
            observedHighC = high.takeIf { judged },
            coveredHours = coveredHours
        )
    }

    /** Union of the covered intervals: two fetches inside the same hour cover one hour
     * between them, not two, or a pull-to-refresh spree would buy a verdict. */
    private fun mergedSeconds(intervals: List<Pair<Instant, Instant>>): Long {
        if (intervals.isEmpty()) return 0
        val sorted = intervals.sortedBy { it.first }
        var total = 0L
        var from = sorted.first().first
        var to = sorted.first().second
        sorted.drop(1).forEach { (start, end) ->
            if (start.isAfter(to)) {
                total += to.epochSecond - from.epochSecond
                from = start
                to = end
            } else if (end.isAfter(to)) {
                to = end
            }
        }
        return total + (to.epochSecond - from.epochSecond)
    }

    /** A stretch of time some commit carried an amount of rain for. */
    private data class Span(val from: Instant, val until: Instant, val wet: Boolean)

    private data class Observation(
        val at: Instant,
        val wmoCode: Int?,
        val spans: List<Span>,
        /** Instants with the temperature the commit recorded for them. */
        val temps: List<Pair<Instant, Double>>
    )

    /**
     * A commit is an observation only if it carries some rain reading — a row written
     * before they existed is silence, not a dry hour.
     *
     * The quarter is read under both its names: `current.precip_quarter_mm` since 24 set
     * 2026, and `current.precip_last_hour_mm` before, which held the same 15-minute value
     * under the wrong name. The closed hours are three aligned lists; lists that do not
     * line up are dropped whole rather than paired by guess.
     */
    private fun Fetch.observation(): Observation? {
        val at = Instant.ofEpochSecond(timestampEpochSeconds)
        val code = snapshot["current.wmo_code"]?.toIntOrNull()
        val quarter = (snapshot["current.precip_quarter_mm"] ?: snapshot["current.precip_last_hour_mm"])
            ?.toDoubleOrNull()
        val ends = snapshot[WeatherSnapshots.PAST_HOURS_END]?.split(',')
            ?.map { runCatching { Instant.parse(it) }.getOrNull() }
        val amounts = snapshot[WeatherSnapshots.PAST_HOURS_MM]?.split(',')?.map { it.toDoubleOrNull() }
        val hourTemps = snapshot[WeatherSnapshots.PAST_HOURS_TEMP_C]?.split(',')?.map { it.toDoubleOrNull() }

        // The quarter ends at the provider's `current.time`, up to fifteen minutes before
        // the commit; a commit that did not record it (all before 24 set 2026) ends at its own.
        val quarterEnd = snapshot[WeatherSnapshots.QUARTER_END]
            ?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: at
        val spans = buildList {
            quarter?.let { add(Span(quarterEnd.minus(QUARTER), quarterEnd, wet = it > 0.0)) }
            if (ends != null && amounts != null && ends.size == amounts.size) {
                ends.zip(amounts).forEach { (end, mm) ->
                    if (end != null && mm != null) add(Span(end.minus(HOUR), end, wet = mm > 0.0))
                }
            }
        }
        if (code == null && spans.isEmpty()) return null
        val temps = buildList {
            snapshot["current.temp_c"]?.toDoubleOrNull()?.let { add(at to it) }
            if (ends != null && hourTemps != null && ends.size == hourTemps.size) {
                ends.zip(hourTemps).forEach { (end, t) -> if (end != null && t != null) add(end to t) }
            }
        }
        return Observation(at = at, wmoCode = code, spans = spans, temps = temps)
    }
}
