package com.callbackdev.chiaro.data.local

import com.callbackdev.chiaro.domain.ConditionWord
import com.callbackdev.chiaro.domain.model.WeatherReport

/**
 * Flattens a [WeatherReport]'s week into the ordered key→value map stored with each
 * history commit, which `ForecastDiff` compares commit against commit for Today's
 * "what changed". Keys must stay stable and values are strings, English and metric.
 */
object WeatherSnapshots {

    /**
     * Flattens the daily forecast for today and the next seven target dates (in the
     * city's local time) into `<ISO date>.<field>` keys, e.g. `2026-08-18.high_c`.
     * Absolute dates keep consecutive snapshots aligned on the same target day, so the
     * comparisons run between two predictions of the same future moment.
     *
     * The week because Today's "what changed" is ABOUT the week — "Saturday improved"
     * needs Saturday on disk. **Today is in the horizon since 8 set 2026**, which is
     * also what gives Today's quiet sentence yesterday's high the next morning.
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
}
