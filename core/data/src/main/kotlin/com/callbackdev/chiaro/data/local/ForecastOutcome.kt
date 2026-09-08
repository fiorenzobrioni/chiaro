package com.callbackdev.chiaro.data.local

import com.callbackdev.chiaro.domain.WeatherCodes
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Closes the loop the Journal had left open: `ForecastDiff` records what the app
 * *said*, this records whether it was *right*.
 *
 * Everything it needs is already on disk. What was forecast for a past day D is the
 * last commit written BEFORE D began that still carried D in its horizon — literally
 * "what the app told you the evening before". What actually happened is the commits
 * written DURING D, each carrying the sky it found (`current.wmo_code`) and the rain
 * of the hour behind it (`current.precip_last_hour_mm`, Open-Meteo's own definition:
 * the sum over the preceding hour).
 *
 * Two rules keep it from lying, and they are not symmetric on purpose:
 *
 * - **"It rained" needs one wet observation.** A positive is proof: the app saw rain,
 *   and no amount of missing hours can un-see it.
 * - **"It did not rain" needs [MIN_COVERAGE_HOURS] of the day actually covered.** Each
 *   observation covers the hour before it, the covered intervals are merged, and below
 *   the floor the day gets no verdict at all rather than a "stayed dry" resting on a
 *   phone that was switched off. The covered hours are carried out with the verdict so
 *   the sentence can state them, the way a sky run states its `obsMinutes`.
 *
 * A commit written before those two keys existed carries no observation and therefore
 * contributes nothing — not a dry hour, not a wet one. The screen fills in from the
 * update forward instead of judging days it has no evidence about.
 */
object ForecastOutcome {

    /** How much of a day must be observed before the app may say "it stayed dry". */
    const val MIN_COVERAGE_HOURS = 16

    private const val DAY_HOURS = 24

    /** The hour a reading's millimetres describe: Open-Meteo's own definition. */
    private val OBSERVATION_WINDOW = java.time.Duration.ofHours(1)

    /**
     * The most one reading may vouch for, backwards, when the previous reading is
     * further away than an hour (8 set 2026): the app's own longest cadence. Until then
     * every reading covered exactly the hour behind it, which made "covered" mean "how
     * many times the app fetched" rather than "how long it was watching": at the two-hour
     * cadence a day watched end to end covered twelve hours and never reached the floor,
     * so a dry day at that setting was never called dry, and at the hourly cadence one
     * night in Doze — where the periodic job runs at the system's convenience — dropped
     * a fully watched day under sixteen. A reading now covers the time since the one
     * before it, capped here, so a phone that watched all day at its own cadence covers
     * the day, and a phone that slept six hours still leaves four of them uncovered.
     * The rain itself stays on the hour the millimetres describe.
     */
    private val MAX_OBSERVATION_WINDOW = java.time.Duration.ofHours(2)

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
     * carrying when the day began; [observedHighC] is the warmest reading it actually
     * took, which is a maximum over samples and is named "seen" on screen for that
     * reason. Both temperatures are null when the day was not covered enough to make a
     * maximum mean anything.
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
        return days.mapNotNull { date -> outcomeFor(date, sorted, zone) }
    }

    private fun outcomeFor(date: LocalDate, sorted: List<Fetch>, zone: ZoneId): Outcome? {
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

        val observations = sorted.mapNotNull { it.observation() }
        var rained = false
        var high: Double? = null
        val covered = mutableListOf<Pair<Instant, Instant>>()
        var previous: Instant? = null
        observations.forEach { obs ->
            // Coverage reaches back to the previous reading, up to the cap; the very
            // first reading has nothing before it and vouches for its own hour.
            val window = previous
                ?.let { minOf(java.time.Duration.between(it, obs.at), MAX_OBSERVATION_WINDOW) }
                ?: OBSERVATION_WINDOW
            previous = obs.at
            val from = maxOf(obs.at.minus(window), start)
            val until = minOf(obs.at, end)
            if (from < until) covered += Pair(from, until)
            // The millimetres belong to the HOUR behind the reading — not to the
            // coverage window — so they count for the day that hour overlaps.
            val rainFrom = maxOf(obs.at.minus(OBSERVATION_WINDOW), start)
            if (rainFrom < until && obs.lastHourMm != null && obs.lastHourMm > 0.0) rained = true
            // The code is what the sky was doing AT the reading, so it counts only
            // for the day the reading itself falls in.
            if (obs.at >= start && obs.at < end) {
                if (obs.wmoCode != null && WeatherCodes.isPrecipitation(obs.wmoCode)) rained = true
                obs.tempC?.let { t -> high = high?.let { maxOf(it, t) } ?: t }
            }
        }
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

    private data class Observation(
        val at: Instant,
        val wmoCode: Int?,
        val lastHourMm: Double?,
        val tempC: Double?
    )

    /** A commit is an observation only if it carries one of the two rain readings —
     * a row written before they existed is silence, not a dry hour. */
    private fun Fetch.observation(): Observation? {
        val code = snapshot["current.wmo_code"]?.toIntOrNull()
        val mm = snapshot["current.precip_last_hour_mm"]?.toDoubleOrNull()
        if (code == null && mm == null) return null
        return Observation(
            at = Instant.ofEpochSecond(timestampEpochSeconds),
            wmoCode = code,
            lastHourMm = mm,
            tempC = snapshot["current.temp_c"]?.toDoubleOrNull()
        )
    }
}
