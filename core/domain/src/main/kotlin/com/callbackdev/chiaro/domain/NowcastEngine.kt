package com.callbackdev.chiaro.domain

import com.callbackdev.chiaro.domain.model.HourlyForecast
import com.callbackdev.chiaro.domain.model.QuarterHour
import com.callbackdev.chiaro.domain.model.WeatherReport
import java.time.Duration
import java.time.Instant

/**
 * What falls in the next two hours, a quarter of an hour at a time (17 ott 2026,
 * `PLANNING.md`, «La pioggia nelle prossime due ore»): the reading Today's section and its
 * sentence are drawn from.
 *
 * **Numbers are the model's; states are the app's**, here as everywhere. A quarter's state
 * is [WeatherStateEngine]'s, on the quarter as a rate — its amount times four, so the
 * engine's millimetres an hour, its 0.1 mm floor and its AMS ladder mean what they mean
 * for an hour — and on **its hour's chance**: a quarter whose hour the ensemble does not
 * back (under 20%) is the model's trace, exactly as the hour is. So the section cannot call
 * a quarter wet inside an hour the strip draws dry; the quarters of an hour sum to that
 * hour (measured, Milan, every hour of the response).
 *
 * The reading is null — no section, no sentence — when the report has no nowcast, when it
 * is older than [MAX_AGE] (a quarter-hour precision does not survive an old fetch), and when
 * the quarters still ahead reach less than [MIN_REACH].
 */
object NowcastEngine {

    /** How far the section looks: two hours, eight quarters. */
    val WINDOW: Duration = Duration.ofHours(2)

    /**
     * Past this age the reading is dropped. Today refetches past fifteen minutes while it is
     * open, so this is the phone that has been offline or the page left open on a cached
     * report: «in about 20 minutes» from a fetch two hours old is a sentence about the past.
     */
    val MAX_AGE: Duration = Duration.ofHours(1)

    /** Less than an hour of quarters ahead is not a window worth a section. */
    val MIN_REACH: Duration = Duration.ofHours(1)

    enum class Intensity { LIGHT, MODERATE, HEAVY }

    /** What falls in a quarter, by phase: rain, snow, or both. */
    enum class Falling { RAIN, SNOW, MIXED }

    /** One quarter of the window, with the app's reading of it. */
    data class Step(
        val start: Instant,
        val end: Instant,
        val precipMm: Double,
        val snowCm: Double?,
        /** [WeatherStateEngine]'s state of the quarter, a [WmoCode] number. */
        val state: Int
    ) {
        val wet: Boolean get() = WmoCode.isPrecipitation(state)

        /** The quarter as a rate, mm of water an hour: what the chart's height is. */
        val mmPerHour: Double get() = precipMm * QUARTERS_PER_HOUR

        /** The snow as a rate, cm an hour; null when the response does not carry snow. */
        val snowCmPerHour: Double? get() = snowCm?.times(QUARTERS_PER_HOUR)

        val intensity: Intensity? get() = if (wet) intensityOf(state) else null

        val falling: Falling? get() = if (wet) fallingOf(setOf(state)) else null
    }

    /**
     * A spell of consecutive wet quarters. [ongoing]: it is falling in the quarter we are
     * in. [lasting]: it is still falling when the window ends, so its end is not known.
     */
    data class Run(
        val start: Instant,
        val end: Instant,
        val ongoing: Boolean,
        val lasting: Boolean,
        /** Its heaviest quarter, by rate; the first of equals. */
        val peak: Step,
        val falling: Falling
    ) {
        val intensity: Intensity get() = peak.intensity ?: Intensity.LIGHT
    }

    data class Reading(
        val now: Instant,
        /** Where the window ends: the last quarter's edge within two hours, or the last
         * quarter the response has. */
        val until: Instant,
        /** Every quarter of the window, the one we are in first. */
        val steps: List<Step>,
        val runs: List<Run>,
        /**
         * Falling right now by the hero's own reading (the `current` block, the quarter just
         * measured), when the window's own quarters are dry: the rain the reader sees is
         * about to stop, and the sentence says so instead of saying nothing.
         */
        val endingNow: Boolean
    ) {
        val wet: Boolean get() = runs.isNotEmpty()

        /** What falls across the whole window, for the section's title. */
        val falling: Falling? get() = if (runs.isEmpty()) null else fallingOf(runs.map { it.falling })

        /** The heaviest quarter of the window. */
        val peak: Step? get() = runs.map { it.peak }.maxByOrNull { it.mmPerHour }
    }

    fun read(report: WeatherReport, now: Instant): Reading? {
        val quarters = report.nowcast?.quarters ?: return null
        if (Duration.between(report.systemInfo.lastSync, now) > MAX_AGE) return null
        val windowEnd = now.plus(WINDOW)
        // The quarter we are in, then every whole one after it with no gap, up to the
        // window's end: the window closes on a quarter's edge, never on a sliver of one.
        val ahead = mutableListOf<QuarterHour>()
        for (q in quarters) {
            if (!q.end.isAfter(now)) continue
            if (ahead.isNotEmpty() && (q.end.isAfter(windowEnd) || ahead.last().end != q.start)) break
            ahead += q
        }
        if (ahead.isEmpty()) return null
        val until = ahead.last().end
        if (Duration.between(now, until) < MIN_REACH) return null

        val steps = ahead.map { q ->
            Step(
                start = q.start,
                end = q.end,
                precipMm = q.precipMm,
                snowCm = q.snowCm,
                state = stateOf(q, hourOf(report.hourly, q.start))
            )
        }
        val runs = runs(steps, now)
        return Reading(
            now = now,
            until = until,
            steps = steps,
            runs = runs,
            endingNow = runs.none { it.ongoing } &&
                WmoCode.isPrecipitation(report.current.condition.wmoCode)
        )
    }

    /**
     * The quarter's state: [WeatherStateEngine] on the quarter as an hour's rate, with its
     * hour's chance and temperature. Rule 4 (likely) is off — a quarter is what falls in
     * it — and the provider's code is not passed: the response has none per quarter, and an
     * hour's thunderstorm or freezing rain given to its dry quarters would make them wet.
     * Freezing rain is the one phase the amounts cannot show, so a measurable quarter of a
     * freezing hour takes the hour's state.
     */
    private fun stateOf(q: QuarterHour, hour: HourlyForecast?): Int {
        val hourState = hour?.condition?.wmoCode
        if (hourState != null && WmoCode.of(hourState)?.phase == WmoCode.Phase.FREEZING &&
            q.precipMm >= WeatherStateEngine.MEASURABLE_MM
        ) {
            return hourState
        }
        return WeatherStateEngine.state(
            WeatherStateEngine.Hour(
                providerCode = WmoCode.OVERCAST.code,
                precipitationMm = q.precipMm * QUARTERS_PER_HOUR,
                snowfallCm = q.snowCm?.times(QUARTERS_PER_HOUR),
                chancePct = hour?.precipChancePct,
                temperatureC = hour?.tempC,
                cloudCoverPct = hour?.cloudCoverPct
            ),
            likely = false
        )
    }

    /** The row of the hour a quarter starts in: rows are the hour that starts at them. */
    private fun hourOf(hours: List<HourlyForecast>, at: Instant): HourlyForecast? =
        hours.lastOrNull { !it.at.isAfter(at) }?.takeIf { Duration.between(it.at, at) < Duration.ofHours(1) }

    private fun runs(steps: List<Step>, now: Instant): List<Run> {
        val runs = mutableListOf<Run>()
        var i = 0
        while (i < steps.size) {
            if (!steps[i].wet) {
                i++
                continue
            }
            val first = i
            while (i < steps.size && steps[i].wet) i++
            val spell = steps.subList(first, i)
            runs += Run(
                start = maxOf(spell.first().start, now),
                end = spell.last().end,
                ongoing = first == 0,
                lasting = i == steps.size,
                // `maxBy` keeps the first of equals: the heaviest quarter, as early as it comes.
                peak = spell.maxBy { it.mmPerHour },
                falling = fallingOf(spell.map { it.falling!! })
            )
        }
        return runs
    }

    private fun fallingOf(states: Set<Int>): Falling {
        val phases = states.mapNotNull { WmoCode.of(it)?.phase }.toSet()
        return when {
            WmoCode.Phase.MIXED in phases -> Falling.MIXED
            WmoCode.Phase.FROZEN in phases &&
                (WmoCode.Phase.LIQUID in phases || WmoCode.Phase.FREEZING in phases) -> Falling.MIXED
            WmoCode.Phase.FROZEN in phases -> Falling.SNOW
            else -> Falling.RAIN
        }
    }

    private fun fallingOf(fallings: List<Falling>): Falling = when {
        Falling.MIXED in fallings -> Falling.MIXED
        Falling.SNOW in fallings && Falling.RAIN in fallings -> Falling.MIXED
        Falling.SNOW in fallings -> Falling.SNOW
        else -> Falling.RAIN
    }

    /**
     * The intensity of a state, on the ladders [WeatherStateEngine] already climbs: the
     * AMS's for rain (light under 2.5 mm/h, heavy from 7.6), Open-Meteo's for snow (light
     * under 0.2 cm/h, heavy from 0.8). One definition of «heavy» in the app.
     */
    fun intensityOf(state: Int): Intensity? = when (WmoCode.of(state)) {
        WmoCode.DRIZZLE_LIGHT, WmoCode.DRIZZLE_MODERATE, WmoCode.DRIZZLE_DENSE,
        WmoCode.FREEZING_DRIZZLE_LIGHT, WmoCode.FREEZING_DRIZZLE_DENSE,
        WmoCode.RAIN_SLIGHT, WmoCode.SHOWERS_SLIGHT, WmoCode.FREEZING_RAIN_LIGHT,
        WmoCode.SNOW_SLIGHT, WmoCode.SNOW_GRAINS, WmoCode.SNOW_SHOWERS_SLIGHT,
        WmoCode.RAIN_AND_SNOW_LIGHT -> Intensity.LIGHT
        WmoCode.RAIN_MODERATE, WmoCode.SHOWERS_MODERATE, WmoCode.SNOW_MODERATE,
        WmoCode.RAIN_AND_SNOW -> Intensity.MODERATE
        WmoCode.RAIN_HEAVY, WmoCode.SHOWERS_HEAVY, WmoCode.FREEZING_RAIN_HEAVY,
        WmoCode.SNOW_HEAVY, WmoCode.SNOW_SHOWERS_HEAVY,
        WmoCode.THUNDERSTORM, WmoCode.THUNDERSTORM_STRONG, WmoCode.THUNDERSTORM_SEVERE -> Intensity.HEAVY
        else -> null
    }

    private const val QUARTERS_PER_HOUR = 4
}
