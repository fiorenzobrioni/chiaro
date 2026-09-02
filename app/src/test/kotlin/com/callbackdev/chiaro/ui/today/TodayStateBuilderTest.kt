package com.callbackdev.chiaro.ui.today

import com.callbackdev.chiaro.domain.model.City
import com.callbackdev.chiaro.domain.model.Coordinates
import com.callbackdev.chiaro.domain.model.DailyForecast
import com.callbackdev.chiaro.domain.model.HourlyForecast
import com.callbackdev.chiaro.domain.model.WeatherCondition
import com.callbackdev.chiaro.domain.sample.sampleWeatherReport
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TodayStateBuilderTest {

    private val zone = ZoneId.of("Europe/Rome")
    private val milan = City(
        id = 1L, name = "Milano", region = "Lombardia", country = "Italia",
        coordinates = Coordinates(45.4643, 9.1895), timezone = "Europe/Rome"
    )
    private val date: LocalDate = LocalDate.of(2026, 9, 2)
    private val clear = WeatherCondition(0, "Clear", "☀️")

    private fun report(fetchedAt: LocalDateTime, hours: Int = 48) =
        sampleWeatherReport().copy(
            location = sampleWeatherReport().location.copy(
                city = "Milano",
                coordinates = milan.coordinates,
                timezone = "Europe/Rome",
                localTime = fetchedAt
            ),
            hourly = (0 until hours).map {
                HourlyForecast(
                    time = fetchedAt.withMinute(0).plusHours(it.toLong()),
                    tempC = 20.0,
                    condition = clear,
                    precipChancePct = 10,
                    cloudCoverPct = 20
                )
            },
            daily = (0 until 7).map {
                DailyForecast(date.plusDays(it.toLong()), 24.0, 14.0, clear, 10, 5, "Moderate")
            },
            systemInfo = sampleWeatherReport().systemInfo.copy(
                lastSync = fetchedAt.atZone(zone).toInstant()
            )
        )

    @Test
    fun `a fresh report builds content with the strip starting at the next full hour`() {
        val fetched = LocalDateTime.of(2026, 9, 2, 12, 0)
        val now = LocalDateTime.of(2026, 9, 2, 12, 30).atZone(zone).toInstant()
        val state = TodayStateBuilder.build(milan, report(fetched), now, 60, false, null)
        val content = state as TodayUiState.Content
        assertEquals(TodayStateBuilder.STRIP_HOURS, content.strip.size)
        assertEquals(
            LocalDateTime.of(2026, 9, 2, 13, 0),
            content.strip.first().hour.time
        )
        assertTrue("noon is not night", !content.night)
        assertTrue("fresh data is not stale", !content.isStale)
        assertTrue("a clear day has sun events left at 12:30", content.timeline.isNotEmpty())
        assertEquals(7, content.week.size)
    }

    @Test
    fun `three quiet hours later the same report is content, aged and trimmed`() {
        val fetched = LocalDateTime.of(2026, 9, 2, 9, 0)
        val now = LocalDateTime.of(2026, 9, 2, 12, 10).atZone(zone).toInstant()
        val content =
            TodayStateBuilder.build(milan, report(fetched), now, 60, false, null) as TodayUiState.Content
        // hours 9, 10 and 11 have already happened and must not be in the report
        assertEquals(LocalDateTime.of(2026, 9, 2, 12, 0), content.report.hourly.first().time)
        assertTrue("3h > 2×60min: stale", content.isStale)
    }

    @Test
    fun `a report past its horizon is empty, not a lie about last week`() {
        val fetched = LocalDateTime.of(2026, 8, 24, 9, 0)
        val now = LocalDateTime.of(2026, 9, 2, 12, 0).atZone(zone).toInstant()
        val state = TodayStateBuilder.build(
            milan, report(fetched, hours = 24), now, 60, false, null
        )
        assertTrue(state is TodayUiState.Empty)
    }

    @Test
    fun `the timeline is only what is still ahead today`() {
        val fetched = LocalDateTime.of(2026, 9, 2, 12, 0)
        val now = LocalDateTime.of(2026, 9, 2, 23, 30).atZone(zone).toInstant()
        val content =
            TodayStateBuilder.build(milan, report(fetched), now, 60, false, null) as TodayUiState.Content
        val lateEvening = LocalDateTime.of(2026, 9, 2, 23, 30)
        content.timeline.forEach {
            assertTrue("${it.kind} at ${it.at} is not ahead", it.at.isAfter(lateEvening))
            assertEquals(date, it.at.toLocalDate())
        }
    }
}
