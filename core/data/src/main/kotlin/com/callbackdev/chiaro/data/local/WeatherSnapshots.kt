package com.callbackdev.chiaro.data.local

import com.callbackdev.chiaro.domain.ConditionWord
import com.callbackdev.chiaro.domain.model.WeatherReport
import com.callbackdev.chiaro.domain.model.hhMm
import java.security.MessageDigest

/**
 * Flattens a [WeatherReport] into the ordered key→value map stored with each history
 * "commit". Fase 8 renders the Logs screen by diffing consecutive snapshots, so keys
 * must stay stable and values are the strings the UI would show.
 */
object WeatherSnapshots {

    fun flatten(report: WeatherReport): Map<String, String> = buildMap {
        // `region ?: country`, exactly like City.label — which is what the Journal's
        // entry prints above this one (upstream's Fase 28, 6 set 2026). A place with no
        // admin1 (Singapore, Monaco, the Vatican) had the two disagree.
        put("location", listOfNotNull(
            report.location.city,
            report.location.region ?: report.location.country
        ).joinToString(", "))
        // The word's stable id since 24 set 2026 (`partly_cloudy`), no longer the English
        // label with its emoji: see [conditionWord] for how the old values still read.
        put("current.status", report.current.condition.word.id)
        // The code, not only its English label: `ForecastOutcome` asks a past commit
        // "was it raining when you looked", and the answer has to be a number the
        // domain already knows how to read (WmoCode.isPrecipitation) rather than
        // a rendered string matched back by hand.
        put("current.wmo_code", report.current.condition.wmoCode.toString())
        // What `ForecastOutcome` needs to say whether a day stayed dry, as evidence with
        // its own time span (24 set 2026). Until then this was one value under
        // `current.precip_last_hour_mm`, documented as the preceding hour and in fact
        // Open-Meteo's preceding QUARTER of an hour — the engine was vouching for an hour
        // of dry weather on fifteen minutes of it. Now: the quarter, under its own name,
        // and the hours that have closed, each with the instant it ended.
        put("current.precip_quarter_mm", report.current.precipitation.lastQuarterHourMm.toString())
        report.current.precipitation.quarterEndedAt?.let { put(QUARTER_END, it.toString()) }
        report.current.precipitation.pastHours.takeIf { it.isNotEmpty() }?.let { hours ->
            put(PAST_HOURS_END, hours.joinToString(",") { it.endedAt.toString() })
            put(PAST_HOURS_MM, hours.joinToString(",") { it.precipMm.toString() })
            put(PAST_HOURS_TEMP_C, hours.joinToString(",") { it.tempC.toString() })
        }
        put("current.temp_c", report.current.tempC.toString())
        put("current.feels_like_c", report.current.feelsLikeC.toString())
        put("current.humidity_pct", report.current.humidityPct.toString())
        put("current.pressure_mb", report.current.pressureMb.toString())
        // Left out when the model carries none, like the chance below.
        report.current.uvIndex?.let { put("current.uv_index", it.toString()) }
        put("current.wind_kph", report.current.wind.speedKph.toString())
        put("current.wind_dir", report.current.wind.directionCompass)
        // Nullable since the 6 set 2026 review — `.toString()` on the null had been
        // writing the literal string "null" into the commit.
        report.current.precipitation.chancePct
            ?.let { put("current.precip_chance_pct", it.toString()) }
        report.airQuality?.let { put("air_quality.aqi", it.aqiIndex.toString()) }
        // Chiaro's rule for a value the model or the sky does not carry: the key is
        // left out, never the word "null" — the Journal draws absence and its shifts
        // pair keys, so a missing one is silence. Upstream decided the opposite for
        // its diff (a bare `null` line, fixed key set, its Fase 28); UPSTREAM.md says why
        // the two copies of this file differ here on purpose.
        report.astronomical.sunrise?.let { put("astronomical.sunrise", it.toString()) }
        report.astronomical.sunset?.let { put("astronomical.sunset", it.toString()) }
        put("astronomical.moon_phase", report.astronomical.moonPhase.text)
        // The fourth field of the sky block, and the one this snapshot was missing
        // (upstream's Fase 28): sunrise and sunset were kept and the span between them
        // was not. Formatted `10h 52m` like every surface, which also truncates the
        // engine's sub-second precision so it changes once a day, with its two ends.
        report.astronomical.daylightDuration?.let { put("astronomical.daylight_duration", it.hhMm()) }
    }

    /**
     * Flattens the daily forecast for today and the next seven target dates (in the
     * city's local time) into `<ISO date>.<field>` keys, e.g. `2026-08-18.high_c`.
     * Absolute dates keep consecutive snapshots aligned on the same target day, so the
     * drift comparisons run between two predictions of the same future moment. Values
     * stay English and metric like [flatten].
     *
     * Upstream keeps two dates (its Logs only ever showed two); Chiaro stores the
     * week because the Journal's drift strip and Today's "what changed" (Fase 7)
     * are ABOUT the week — "Saturday improved" needs Saturday on disk. **Today is in
     * the horizon since 8 set 2026**: "has today's rain been going up?" is the
     * morning's question, and the drift strip could not answer it with today's own
     * forecast never written down. `ForecastDiff.dayLabel` still names the earliest
     * date "tomorrow"; that label is upstream's and nothing in Chiaro reads it.
     */
    fun flattenForecast(report: WeatherReport): Map<String, String> = buildMap {
        val today = report.location.localTime.toLocalDate()
        val horizon = (0L..7L).map { today.plusDays(it) }.toSet()
        report.daily.filter { it.date in horizon }.forEach { day ->
            val prefix = day.date.toString()
            put("$prefix.status", day.condition.word.id)
            put("$prefix.high_c", day.highC.toString())
            put("$prefix.low_c", day.lowC.toString())
            // Absent when the model carried no probability: a key that is not there
            // is a diff that says nothing and an absence a surface can draw. Never a "0".
            day.precipPct?.let { put("$prefix.precip_pct", it.toString()) }
        }
    }

    /** When the quarter under `current.precip_quarter_mm` ended, as an instant. */
    const val QUARTER_END = "current.precip_quarter_end"

    /** Keys of the closed hours, comma-separated lists aligned index by index, newest first. */
    const val PAST_HOURS_END = "current.past_hours_end"
    const val PAST_HOURS_MM = "current.past_hours_mm"
    const val PAST_HOURS_TEMP_C = "current.past_hours_temp_c"

    /**
     * The word a stored `status` value names, whichever shape it was written in: the
     * [ConditionWord.id] of today, or the English label with its emoji that every commit
     * before 24 set 2026 carries. Reading both is what keeps the first fetch after the
     * update from reporting every day of the week as changed — `"Partly Cloudy ⛅"` and
     * `partly_cloudy` are the same forecast. The old labels grouped the codes exactly as
     * the words do (51/53/55 one label, 96/99 one label), so the mapping loses nothing.
     */
    fun conditionWord(stored: String): ConditionWord? =
        ConditionWord.fromId(stored) ?: LegacyLabels[stored]

    /** tweather's `WeatherCodes.condition` vocabulary, frozen: these strings are on disk. */
    private val LegacyLabels: Map<String, ConditionWord> = mapOf(
        "Clear ☀️" to ConditionWord.CLEAR,
        "Clear 🌙" to ConditionWord.CLEAR,
        "Mainly Clear 🌤️" to ConditionWord.MOSTLY_CLEAR,
        "Mainly Clear 🌙" to ConditionWord.MOSTLY_CLEAR,
        "Partly Cloudy ⛅" to ConditionWord.PARTLY_CLOUDY,
        "Overcast ☁️" to ConditionWord.OVERCAST,
        "Foggy 🌫️" to ConditionWord.FOG,
        "Drizzle 🌦️" to ConditionWord.DRIZZLE,
        "Freezing Drizzle 🌧️" to ConditionWord.FREEZING_DRIZZLE,
        "Light Rain 🌧️" to ConditionWord.RAIN_LIGHT,
        "Rainy 🌧️" to ConditionWord.RAIN,
        "Heavy Rain 🌧️" to ConditionWord.RAIN_HEAVY,
        "Freezing Rain 🌧️" to ConditionWord.FREEZING_RAIN,
        "Light Snow 🌨️" to ConditionWord.SNOW_LIGHT,
        "Snowy 🌨️" to ConditionWord.SNOW,
        "Heavy Snow ❄️" to ConditionWord.SNOW_HEAVY,
        "Snow Grains ❄️" to ConditionWord.SNOW_GRAINS,
        "Rain Showers 🌦️" to ConditionWord.SHOWERS,
        "Violent Showers 🌧️" to ConditionWord.SHOWERS_HEAVY,
        "Snow Showers 🌨️" to ConditionWord.SNOW_SHOWERS,
        "Thunderstorm ⛈️" to ConditionWord.THUNDERSTORM,
        "Thunderstorm w/ Hail ⛈️" to ConditionWord.THUNDERSTORM_STRONG,
        "Unknown ❓" to ConditionWord.UNKNOWN
    )

    /** Short pseudo-git hash identifying a history entry. */
    fun commitHash(vararg parts: String): String =
        MessageDigest.getInstance("SHA-1")
            .digest(parts.joinToString("|").toByteArray())
            .joinToString("") { "%02x".format(it) }
            .take(7)
}
