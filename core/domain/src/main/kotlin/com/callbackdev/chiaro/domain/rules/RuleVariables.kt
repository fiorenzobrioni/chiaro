package com.callbackdev.chiaro.domain.rules

import com.callbackdev.chiaro.domain.settings.PressureUnit
import com.callbackdev.chiaro.domain.settings.TemperatureUnit
import com.callbackdev.chiaro.domain.settings.UnitSettings
import com.callbackdev.chiaro.domain.settings.Units
import com.callbackdev.chiaro.domain.settings.WindSpeedUnit
import com.callbackdev.chiaro.domain.AlertEngine
import com.callbackdev.chiaro.domain.model.HourlyForecast
import com.callbackdev.chiaro.domain.model.WeatherReport
import java.time.LocalDateTime
import kotlin.math.pow

/** How a variable renders and converts: temperatures, speeds and pressures (14.9) follow
 * the units in settings.config (name AND value — a file doesn't lie about its units,
 * Fase 7); plain numbers are unit-fixed; booleans render `true`/`false`. */
enum class RuleVariableKind { TEMPERATURE, SPEED, PRESSURE, NUMBER, BOOLEAN }

/** A resolved variable: the value plus, for forecast aggregates, the hour that
 * produced it — what the `{trigger.time}` placeholder interpolates. */
data class ResolvedValue(val value: Double, val at: LocalDateTime? = null)

class RuleVariable(
    /** Canonical metric name, mirroring `weather_data.json`'s keys. */
    val id: String,
    val kind: RuleVariableKind,
    /** Pure resolution against a report; null when the data is unavailable
     * (air quality down, empty forecast window) — the rule then just skips. */
    val resolve: (WeatherReport, LocalDateTime) -> ResolvedValue?
)

/**
 * The curated variable registry of `alerts.rules` (Fase 11). No functions in the
 * language: forecast aggregates are precomputed variables, so the user picks the
 * time window by picking the name. Namespaces are explicit on purpose — a bare
 * `rain_probability` (now? tonight?) would make notifications ambiguous:
 *
 * - `current.*` mirrors `current_conditions` + `air_quality`
 * - `next_6h.*` / `next_12h.*` aggregate the hourly forecast from now
 * - `today.*` reads the daily row dated today (none when the report has no today)
 * - `tomorrow.*` reads the row dated tomorrow, the same seven facts (15.9, 29 Oct 2026)
 */
object RuleVariables {

    val all: List<RuleVariable> = buildList {
        current("current.temp_c", RuleVariableKind.TEMPERATURE) { it.current.tempC }
        current("current.feels_like_c", RuleVariableKind.TEMPERATURE) { it.current.feelsLikeC }
        current("current.humidity_pct", RuleVariableKind.NUMBER) { it.current.humidityPct.toDouble() }
        current("current.dew_point_c", RuleVariableKind.TEMPERATURE) { it.current.dewPointC }
        // Optional since 24 set 2026: a model may carry no UV at all, and a rule on it
        // then skips rather than firing `< 3` on an index nobody measured.
        optional("current.uv_index", RuleVariableKind.NUMBER) { it.current.uvIndex?.toDouble() }
        current("current.wind.speed_kph", RuleVariableKind.SPEED) { it.current.wind.speedKph }
        current("current.wind.gust_kph", RuleVariableKind.SPEED) { it.current.wind.gustKph }
        // Fase 26: both of these can be absent, and a variable that does not resolve
        // makes its rule skip rather than read as zero — which is what `optional` is.
        optional("current.precipitation.chance_pct", RuleVariableKind.NUMBER) {
            it.current.precipitation.chancePct?.toDouble()
        }
        current("current.pressure_mb", RuleVariableKind.PRESSURE) { it.current.pressureMb }
        optional("current.visibility_km", RuleVariableKind.NUMBER) { it.current.visibilityKm }
        // Air quality is best-effort upstream: null when the AQ API had failed
        add(
            RuleVariable("current.aqi_index", RuleVariableKind.NUMBER) { report, _ ->
                report.airQuality?.let { ResolvedValue(it.aqiIndex.toDouble()) }
            }
        )
        // The European index beside the US one (24 set 2026), not in its place: a rule
        // written as «aqi > 100» means the US scale, and changing what the id reads would
        // change what a saved rule means without its author knowing.
        optional("current.aqi_eu_index", RuleVariableKind.NUMBER) {
            it.airQuality?.europeanAqi?.toDouble()
        }

        for (hours in listOf(6L, 12L)) {
            window("next_${hours}h.precip_chance_max", RuleVariableKind.NUMBER, hours) { w ->
                w.mapNotNull { h -> h.precipChancePct?.let { h to it } }
                    .maxByOrNull { (_, pct) -> pct }
                    ?.let { (hour, pct) -> ResolvedValue(pct.toDouble(), hour.time) }
            }
            window("next_${hours}h.temp_c_min", RuleVariableKind.TEMPERATURE, hours) { w ->
                w.minByOrNull { it.tempC }?.let { ResolvedValue(it.tempC, it.time) }
            }
            window("next_${hours}h.temp_c_max", RuleVariableKind.TEMPERATURE, hours) { w ->
                w.maxByOrNull { it.tempC }?.let { ResolvedValue(it.tempC, it.time) }
            }
            // The strongest gust ahead (24 set 2026, when the rows gained the wind); an
            // hour with no gust is left out, never read as calm.
            window("next_${hours}h.gust_max_kph", RuleVariableKind.SPEED, hours) { w ->
                w.mapNotNull { h -> h.gustKph?.let { h to it } }
                    .maxByOrNull { (_, gust) -> gust }
                    ?.let { (hour, gust) -> ResolvedValue(gust, hour.time) }
            }
            // The feels-like high ahead (14.6, 27 Oct 2026): the heat is a hazard of the app's
            // own, and a rule on it wants the number a body feels, not the thermometer's. An
            // hour with no feels-like is left out, never read as the air temperature.
            window("next_${hours}h.feels_like_max_c", RuleVariableKind.TEMPERATURE, hours) { w ->
                w.mapNotNull { h -> h.feelsLikeC?.let { h to it } }
                    .maxByOrNull { (_, feels) -> feels }
                    ?.let { (hour, feels) -> ResolvedValue(feels, hour.time) }
            }
            // The builtin severe alert's own definition (AlertEngine.isSevere), chance
            // floor included: a rule must not call «Maltempo» a storm the app dropped.
            // The gale too (14.7), at the place's own bar: the elevation is the report's,
            // which is why this one does not go through [window].
            add(
                RuleVariable("next_${hours}h.wmo_severe", RuleVariableKind.BOOLEAN) { report, now ->
                    val end = now.plusHours(hours)
                    val w = report.hourly.filter { !it.time.isBefore(now) && !it.time.isAfter(end) }
                    if (w.isEmpty()) {
                        null
                    } else {
                        val hit = w.firstOrNull { AlertEngine.isSevere(it, report.location.elevationM) }
                        if (hit != null) ResolvedValue(1.0, hit.time) else ResolvedValue(0.0)
                    }
                }
            )
        }

        // Today's row, then tomorrow's (15.9, 29 Oct 2026): the same seven facts read off the
        // row dated the next day, so «neve domani almeno 5 cm» can be written at all — «Ghiaccio
        // domattina» was a stand-in on the next 12 hours. The two days list their facts in one
        // order, so the two groups of «Cosa guardare» read line for line.
        for ((day, offset) in listOf("today" to 0L, "tomorrow" to 1L)) {
            // Suffixed like the notification keys of Fase 9c (`high_c`/`high_f`), not
            // like daily_forecast's bare "high": the unit must live in the name here
            // too, or `today.high > 25` would silently change meaning with the setting.
            day("$day.high_c", RuleVariableKind.TEMPERATURE, offset) { it.highC }
            day("$day.low_c", RuleVariableKind.TEMPERATURE, offset) { it.lowC }
            // Nullable like `current.precipitation.chance_pct` above and for the same
            // reason: the day may simply not carry a probability, and a rule that reads
            // it then skips instead of firing `< 10` on a zero nobody forecast.
            day("$day.precip_pct", RuleVariableKind.NUMBER, offset) { it.precipPct?.toDouble() }
            // The day's peak, not `current.uv_index`'s instant reading: a "put sunscreen
            // on" rule wants to fire in the morning, when the current index is still low.
            day("$day.uv_max", RuleVariableKind.NUMBER, offset) { it.uvIndexMax?.toDouble() }
            // How much, not only how likely (24 set 2026): the day's rain in mm, its snow in
            // cm, its strongest gust. Nullable like the rest of the day: a model that does not
            // carry them makes the rule skip.
            // The id keeps its name for the rules already saved; the value is the rain alone
            // (24 set 2026), what the phrase «la pioggia di oggi» promises — the total also
            // carries the snow's water, and fired an umbrella rule on a day of snow.
            day("$day.precip_mm", RuleVariableKind.NUMBER, offset) { it.rainMm }
            day("$day.snow_cm", RuleVariableKind.NUMBER, offset) { it.snowCm }
            day("$day.gust_max_kph", RuleVariableKind.SPEED, offset) { it.gustMaxKph }
        }
    }

    private val index = all.associateBy { it.id }

    fun byId(id: String): RuleVariable? = index[id]

    /** Instant variables read the present: their rules are edge-triggered instead
     * of fingerprint-deduplicated (see RuleEngine). */
    fun isInstant(id: String): Boolean = id.startsWith("current.")

    /**
     * The name as `alerts.rules` displays it, in the user's units: `current.temp_c` ↔
     * `current.temp_f`; `…_kph` ↔ `…_mph`, `…_ms`, `…_kn`, `…_bft`; `…_mb` ↔ `…_inhg`,
     * `…_mmhg` (14.9). Curated ids contain exactly one unit token, at the end for speeds
     * and pressures, so the replacement is unambiguous. The canonical ids stay `_kph` and
     * `_mb`: a rule saved in knots is the same rule in km/h.
     */
    fun displayId(variable: RuleVariable, units: UnitSettings): String = when (variable.kind) {
        RuleVariableKind.TEMPERATURE ->
            if (units.temperature == TemperatureUnit.FAHRENHEIT) {
                variable.id.replace("_c", "_f")
            } else {
                variable.id
            }
        RuleVariableKind.SPEED -> variable.id.replace("_kph", speedToken(units.windSpeed))
        RuleVariableKind.PRESSURE -> variable.id.replace("_mb", pressureToken(units.pressure))
        else -> variable.id
    }

    /** The canonical id for a displayed (possibly non-metric) name; null if unknown. */
    fun canonicalId(displayed: String): String? = when {
        index.containsKey(displayed) -> displayed
        else -> displayed.replace("_f", "_c")
            .replace(SpeedTokens, "_kph")
            .replace(PressureTokens, "_mb")
            .takeIf { index.containsKey(it) }
    }

    private fun speedToken(unit: WindSpeedUnit): String = when (unit) {
        WindSpeedUnit.KMH -> "_kph"
        WindSpeedUnit.MPH -> "_mph"
        WindSpeedUnit.M_S -> "_ms"
        WindSpeedUnit.KNOTS -> "_kn"
        WindSpeedUnit.BEAUFORT -> "_bft"
    }

    private fun pressureToken(unit: PressureUnit): String = when (unit) {
        PressureUnit.HPA -> "_mb"
        PressureUnit.INHG -> "_inhg"
        PressureUnit.MMHG -> "_mmhg"
    }

    private val SpeedTokens = Regex("_(mph|ms|kn|bft)$")
    private val PressureTokens = Regex("_(inhg|mmhg)$")

    /** Canonical metric value → the user's units, for display and interpolation. */
    fun displayValue(kind: RuleVariableKind, canonical: Double, units: UnitSettings): Double =
        when (kind) {
            RuleVariableKind.TEMPERATURE ->
                if (units.temperature == TemperatureUnit.FAHRENHEIT) {
                    canonical * 9 / 5 + 32
                } else {
                    canonical
                }
            RuleVariableKind.SPEED -> Units.speedFromKph(canonical, units.windSpeed)
            RuleVariableKind.PRESSURE -> Units.pressureFromHpa(canonical, units.pressure)
            else -> canonical
        }

    /** A value typed in the user's units → canonical metric, for storage. A Beaufort
     * force becomes its band's lower bound in km/h ([Units.speedToKph]). */
    fun canonicalValue(kind: RuleVariableKind, displayed: Double, units: UnitSettings): Double =
        when (kind) {
            RuleVariableKind.TEMPERATURE ->
                if (units.temperature == TemperatureUnit.FAHRENHEIT) {
                    (displayed - 32) * 5 / 9
                } else {
                    displayed
                }
            RuleVariableKind.SPEED -> Units.speedToKph(displayed, units.windSpeed)
            RuleVariableKind.PRESSURE -> Units.pressureToHpa(displayed, units.pressure)
            else -> displayed
        }

    /**
     * `19`, `19.5`, `true` — one decimal at most, integers bare, like the JSON. The unit
     * decides the decimals where one is not its precision (14.9): a Beaufort force is
     * whole, inches of mercury keep two (`29.92`).
     */
    fun formatValue(kind: RuleVariableKind, canonical: Double, units: UnitSettings): String {
        if (kind == RuleVariableKind.BOOLEAN) return if (canonical != 0.0) "true" else "false"
        val decimals = when (kind) {
            RuleVariableKind.SPEED -> Units.speedDecimals(units.windSpeed)
            RuleVariableKind.PRESSURE -> Units.pressureDecimals(units.pressure)
            else -> 1
        }
        val scale = 10.0.pow(decimals)
        val display = Math.round(displayValue(kind, canonical, units) * scale) / scale
        return if (display == display.toLong().toDouble()) {
            display.toLong().toString()
        } else {
            display.toString()
        }
    }

    private fun MutableList<RuleVariable>.current(
        id: String,
        kind: RuleVariableKind,
        value: (WeatherReport) -> Double
    ) = add(RuleVariable(id, kind) { report, _ -> ResolvedValue(value(report)) })

    /**
     * Like [current], for a reading the report may simply not carry (Fase 26): a null
     * makes the variable unresolvable, and an unresolvable variable makes its rule
     * skip — the engine's existing contract, and the only honest reading of "we were
     * not told". Filling it with a zero would let `< 10` fire on missing data.
     */
    private fun MutableList<RuleVariable>.optional(
        id: String,
        kind: RuleVariableKind,
        value: (WeatherReport) -> Double?
    ) = add(RuleVariable(id, kind) { report, _ -> value(report)?.let { ResolvedValue(it) } })

    private fun MutableList<RuleVariable>.window(
        id: String,
        kind: RuleVariableKind,
        hours: Long,
        aggregate: (List<HourlyForecast>) -> ResolvedValue?
    ) = add(
        RuleVariable(id, kind) { report, now ->
            val end = now.plusHours(hours)
            aggregate(
                report.hourly.filter { !it.time.isBefore(now) && !it.time.isAfter(end) }
            )
        }
    )

    /**
     * [value] is nullable for the same reason [optional] exists: a reading the day
     * does not carry leaves the variable unresolved, and its rule skips.
     *
     * The row DATED today, not the first (15.1, 28 Oct 2026), as [AlertEngine] has read the
     * day since 23 set 2026: the first row of a report is today only while the report is
     * today's, and the Alerts cards read a cache that can be yesterday's — «Adesso sì» on
     * yesterday's high. A report with no row for today leaves the variable unresolved.
     * [offset] is the days after today: 1 is tomorrow's row, by its date as well (15.9).
     */
    private fun MutableList<RuleVariable>.day(
        id: String,
        kind: RuleVariableKind,
        offset: Long,
        value: (com.callbackdev.chiaro.domain.model.DailyForecast) -> Double?
    ) = add(
        RuleVariable(id, kind) { report, now ->
            report.daily.firstOrNull { it.date == now.toLocalDate().plusDays(offset) }
                ?.let { day -> value(day)?.let { ResolvedValue(it) } }
        }
    )
}
