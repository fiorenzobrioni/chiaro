package com.callbackdev.chiaro.data.local

import com.callbackdev.chiaro.domain.model.WeatherReport

/**
 * What each history commit keeps of the forecast: an ordered key→value map, keys
 * `<ISO date>.<field>` and values strings, English and metric. The keys must stay stable: a
 * commit written today is read tomorrow.
 */
object WeatherSnapshots {

    /**
     * Today's high, under `<today's ISO date>.high_c` in the city's local date: the one key
     * anything reads back, Today's quiet sentence, which takes it the next morning as
     * yesterday's high (`TodayStateBuilder.yesterdayHigh`).
     *
     * Until 23 nov 2026 a commit kept the whole week, status, high, low and chance for today
     * and the seven days after it, which «What changed» compared commit against commit. The
     * section left the app that day (committente: measured, it reported mostly the model's
     * run-to-run noise), and the week with it: each commit is a few dozen bytes instead of a
     * kilobyte. A commit written before keeps its week on disk until the retention drops it,
     * and reads the same, the key being the same.
     *
     * Empty when the report has no row for today.
     */
    fun flattenForecast(report: WeatherReport): Map<String, String> {
        val today = report.location.localTime.toLocalDate()
        val day = report.daily.firstOrNull { it.date == today } ?: return emptyMap()
        return mapOf("$today.high_c" to day.highC.toString())
    }
}
