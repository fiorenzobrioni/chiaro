package com.callbackdev.chiaro.data.local

import com.callbackdev.chiaro.domain.model.DailyForecast
import com.callbackdev.chiaro.domain.model.WeatherCondition
import com.callbackdev.chiaro.domain.sample.sampleWeatherReport
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

/** What a history commit keeps of the forecast: today's high, the one key read back (23 nov 2026). */
class WeatherSnapshotsTest {

    private val sunny = WeatherCondition(0)
    private val rainy = WeatherCondition(63)

    // sampleWeatherReport's local time is 2023-10-27 14:30
    private val today: LocalDate = LocalDate.of(2023, 10, 27)

    private fun reportWithDaily(vararg days: DailyForecast) =
        sampleWeatherReport().copy(daily = days.toList())

    @Test
    fun `a commit keeps today's high, keyed by the city's local date, and nothing else`() {
        val report = reportWithDaily(
            DailyForecast(today, 21.0, 14.0, sunny, 0, 5),
            DailyForecast(today.plusDays(1), 20.0, 12.0, rainy, 85, 2),
            DailyForecast(today.plusDays(2), 16.0, 10.0, sunny, 20, 4)
        )
        assertEquals(mapOf("2023-10-27.high_c" to "21.0"), WeatherSnapshots.flattenForecast(report))
    }

    @Test
    fun `a report with no row for today keeps nothing`() {
        assertEquals(
            emptyMap<String, String>(),
            WeatherSnapshots.flattenForecast(reportWithDaily(DailyForecast(today.plusDays(1), 20.0, 12.0, sunny, 0, 5)))
        )
        assertEquals(emptyMap<String, String>(), WeatherSnapshots.flattenForecast(reportWithDaily()))
    }
}
