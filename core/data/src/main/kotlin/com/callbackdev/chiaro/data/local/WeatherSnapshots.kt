package com.callbackdev.chiaro.data.local

import com.callbackdev.chiaro.domain.model.WeatherReport
import java.security.MessageDigest

/**
 * Flattens a [WeatherReport] into the ordered key→value map stored with each history
 * "commit". Fase 8 renders the Logs screen by diffing consecutive snapshots, so keys
 * must stay stable and values are the strings the UI would show.
 */
object WeatherSnapshots {

    fun flatten(report: WeatherReport): Map<String, String> = buildMap {
        put("location", listOfNotNull(report.location.city, report.location.region)
            .joinToString(", "))
        put("current.status", report.current.condition.label)
        // The code, not only its English label: `ForecastOutcome` asks a past commit
        // "was it raining when you looked", and the answer has to be a number the
        // domain already knows how to read (WeatherCodes.isPrecipitation) rather than
        // a rendered string matched back by hand.
        put("current.wmo_code", report.current.condition.wmoCode.toString())
        // Open-Meteo's `current.precipitation` is the sum of the PRECEDING hour, which
        // is why the outcome engine can cover a day with hourly fetches instead of
        // sampling instants and hoping it did not rain between two of them.
        put("current.precip_last_hour_mm", report.current.precipitation.lastHourMm.toString())
        put("current.temp_c", report.current.tempC.toString())
        put("current.feels_like_c", report.current.feelsLikeC.toString())
        put("current.humidity_pct", report.current.humidityPct.toString())
        put("current.pressure_mb", report.current.pressureMb.toString())
        put("current.uv_index", report.current.uvIndex.toString())
        put("current.wind_kph", report.current.wind.speedKph.toString())
        put("current.wind_dir", report.current.wind.directionCompass)
        // Nullable since Fase 26 — `.toString()` on the null had been writing the
        // literal string "null" into the commit.
        report.current.precipitation.chancePct
            ?.let { put("current.precip_chance_pct", it.toString()) }
        report.airQuality?.let { put("air_quality.aqi", it.aqiIndex.toString()) }
        put("astronomical.sunrise", report.astronomical.sunrise.toString())
        put("astronomical.sunset", report.astronomical.sunset.toString())
        put("astronomical.moon_phase", report.astronomical.moonPhase.text)
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
            put("$prefix.status", day.condition.label)
            put("$prefix.high_c", day.highC.toString())
            put("$prefix.low_c", day.lowC.toString())
            // Absent when the model carried no probability: a key that is not there
            // is the drift strip's absence and the diff's silence. Never a "0".
            day.precipPct?.let { put("$prefix.precip_pct", it.toString()) }
        }
    }

    /** Short pseudo-git hash identifying a history entry. */
    fun commitHash(vararg parts: String): String =
        MessageDigest.getInstance("SHA-1")
            .digest(parts.joinToString("|").toByteArray())
            .joinToString("") { "%02x".format(it) }
            .take(7)
}
