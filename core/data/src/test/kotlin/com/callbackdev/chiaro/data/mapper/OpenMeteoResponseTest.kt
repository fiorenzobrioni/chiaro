package com.callbackdev.chiaro.data.mapper

import com.callbackdev.chiaro.data.remote.dto.ForecastResponseDto
import com.callbackdev.chiaro.domain.model.CacheStatus
import com.callbackdev.chiaro.domain.model.City
import com.callbackdev.chiaro.domain.model.Coordinates
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Real responses, not fixtures built to agree with the code (24 set 2026, the engine
 * review's suggestion 5). Fetched that morning with exactly the variables
 * `OpenMeteoForecastApi` asks for, `timezone=auto` and `forecast_days=7`, and decoded
 * with the app's own `Json` settings — so a field the provider sends in a shape the DTO
 * does not accept fails here, not on a phone.
 *
 * Each place was chosen for what it had in it that morning: Reykjavík was raining, and
 * its quarter of an hour (0.4 mm) is not its hour (1.1 mm); Sydney had one hour of
 * 0.1 mm at 3% that the provider's own `daily.weather_code` turns into a day of drizzle,
 * and four hours of real fog at dawn served as cloud; Milan was dry and clouding over.
 */
class OpenMeteoResponseTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun load(name: String): ForecastResponseDto {
        val text = javaClass.getResource("/openmeteo/$name-2026-09-24.json")!!.readText()
        return json.decodeFromString(ForecastResponseDto.serializer(), text)
    }

    private fun map(forecast: ForecastResponseDto, lat: Double, lon: Double) = WeatherReportMapper.map(
        city = City(1, "X", null, null, Coordinates(lat, lon), forecast.timezone),
        forecast = forecast,
        airQuality = null,
        fetchedAt = Instant.parse("2026-09-24T09:50:00Z"),
        responseTimeMs = 100,
        cacheStatus = CacheStatus.MISS
    )

    @Test
    fun `reykjavik - the quarter is not the hour, and both are kept`() {
        val report = map(load("reykjavik"), 64.1466, -21.9426)
        val rain = report.current.precipitation
        assertEquals(0.4, rain.lastQuarterHourMm, 0.0)
        assertEquals(Instant.parse("2026-09-24T09:45:00Z"), rain.quarterEndedAt)
        // current.time 09:45; the hours closed by then end at 09:00, 08:00, 07:00.
        assertEquals(
            listOf("2026-09-24T09:00:00Z", "2026-09-24T08:00:00Z", "2026-09-24T07:00:00Z"),
            rain.pastHours.map { it.endedAt.toString() }
        )
        assertEquals(listOf(1.1, 1.0, 1.2), rain.pastHours.map { it.precipMm })
        assertEquals(listOf(6.9, 7.2, 7.2), rain.pastHours.map { it.tempC })
        // The hour under way is 09-10, the slot labelled 10:00.
        assertEquals(100, rain.chancePct)
        // A wet day of drizzle and light rain, ~10 mm: its heaviest code names it.
        assertEquals(61, report.daily.first().condition.wmoCode)
    }

    @Test
    fun `sydney - one hour of 0,1 mm at 3 percent does not make a day of drizzle`() {
        val forecast = load("sydney")
        assertEquals(51, forecast.daily.weatherCode.first()) // the provider's max()
        val report = map(forecast, -33.8688, 151.2093)
        assertEquals(0, report.daily.first().condition.wmoCode)
    }

    @Test
    fun `sydney - four hours under a kilometre at dawn are fog, whatever the code said`() {
        val forecast = load("sydney")
        // Served as 1, 3, 3, 3 with 940, 400, 280, 320 metres of visibility.
        assertEquals(listOf(1, 3, 3, 3), forecast.hourly.weatherCode.subList(3, 7))
        val report = map(forecast.copy(current = forecast.current.copy(time = "2026-09-24T00:15")), -33.8688, 151.2093)
        val dawn = report.hourly.filter { it.time.toLocalDate() == LocalDate.parse("2026-09-24") && it.time.hour in 3..6 }
        assertEquals(listOf(45, 45, 45, 45), dawn.map { it.condition.wmoCode })
        // 07:00, at 1 060 m, is not.
        assertEquals(1, report.hourly.first { it.time == LocalDateTime.parse("2026-09-24T07:00") }.condition.wmoCode)
    }

    @Test
    fun `milan - a dry day closing over reads as its daylight`() {
        val report = map(load("milan"), 45.4642, 9.19)
        assertEquals(3, report.daily.first().condition.wmoCode)
        assertTrue(report.current.precipitation.pastHours.all { it.precipMm == 0.0 })
        assertEquals(4, report.current.uvIndex) // 3.6
    }

    /** P6: the same response with the model's UV missing costs the tile, not the report. */
    @Test
    fun `a null current uv index parses, and maps to no index`() {
        val text = javaClass.getResource("/openmeteo/milan-2026-09-24.json")!!.readText()
        val withoutUv = text.replaceFirst(Regex("\"uv_index\":[0-9.]+"), "\"uv_index\":null")
        assertTrue(withoutUv != text)
        val report = map(json.decodeFromString(ForecastResponseDto.serializer(), withoutUv), 45.4642, 9.19)
        assertNull(report.current.uvIndex)
        assertEquals(7, report.daily.size)
    }

    /** Every code the three responses carry is one the table knows. */
    @Test
    fun `every served code is in the table`() {
        listOf("reykjavik", "milan", "sydney").map(::load).forEach { f ->
            (f.hourly.weatherCode + f.daily.weatherCode + f.current.weatherCode).forEach {
                assertTrue("code $it", com.callbackdev.chiaro.domain.WmoCode.of(it) != null)
            }
        }
    }

    // ---------------- 24 set 2026: the full request of the second pass, same morning

    private fun loadFull(name: String): ForecastResponseDto {
        val text = javaClass.getResource("/openmeteo/$name-full-2026-09-24.json")!!.readText()
        return json.decodeFromString(ForecastResponseDto.serializer(), text)
    }

    @Test
    fun `reykjavik full - the day's rain, its hours and a 112 km per hour gust`() {
        val report = map(loadFull("reykjavik"), 64.1466, -21.9426)
        val today = report.daily.first()
        assertEquals(10.5, today.precipMm!!, 0.0)
        assertEquals(16.0, today.precipHours!!, 0.0)
        assertEquals(0.0, today.snowCm!!, 0.0)
        assertEquals(112.3, today.gustMaxKph!!, 0.0)
        // current.time 10:45: the first row is 10:00, the hour 10-11, whose gust and code
        // the provider writes in its 11:00 slot.
        val row = report.hourly.first()
        assertEquals(LocalDateTime.parse("2026-09-24T10:00"), row.time)
        assertEquals(112.3, row.gustKph!!, 0.0)
        assertEquals(61, row.condition.wmoCode)
        assertEquals(-3.0, row.feelsLikeC!!, 0.0)
        assertEquals(58.0, row.windKph!!, 0.0)
        assertEquals(91, row.windDirectionDeg)
        // 19% low, 58% mid, 100% high: no one layer makes this sky.
        assertNull(report.current.cloudLayers!!.dominant())
    }

    @Test
    fun `milan full - a sky all high cloud is a veil`() {
        val report = map(loadFull("milan"), 45.4642, 9.19)
        assertEquals(100, report.current.cloudCoverPct)
        assertEquals(com.callbackdev.chiaro.domain.model.CloudLayers.Layer.HIGH, report.current.cloudLayers!!.dominant())
        assertEquals(0.0, report.daily.first().precipMm!!, 0.0)
    }

    @Test
    fun `everest full - a day of heavy snow is heavy snow, with its centimetres`() {
        val report = map(loadFull("everest"), 27.99, 86.93)
        val today = report.daily.first()
        assertEquals(75, today.condition.wmoCode)
        assertEquals(19.81, today.snowCm!!, 0.0)
        assertEquals(28.4, today.precipMm!!, 0.0)
    }

    /** Suggestion 3 on a real response: two hours offline, the hero is 12:45 + 2 h. */
    @Test
    fun `milan full - offline for two hours, now is the forecast for now`() {
        val forecast = loadFull("milan")
        val fetched = Instant.parse("2026-09-24T10:45:00Z") // 12:45 in Milan
        val report = WeatherReportMapper.map(
            city = City(1, "Milano", "Lombardia", "Italia", Coordinates(45.4642, 9.19), "Europe/Rome", countryCode = "IT"),
            forecast = forecast, airQuality = null, fetchedAt = fetched,
            responseTimeMs = 1, cacheStatus = CacheStatus.MISS
        )
        val now = Instant.parse("2026-09-24T12:45:00Z") // 14:45 in Milan
        val estimated = com.callbackdev.chiaro.domain.CurrentEstimate.apply(report, now).current
        assertTrue(estimated.estimated)
        val h = forecast.hourly
        val at14 = h.time.indexOf("2026-09-24T14:00")
        val expected = h.temperatureC[at14] + (h.temperatureC[at14 + 1] - h.temperatureC[at14]) * 0.75
        assertEquals(expected, estimated.tempC, 1e-9)
        // Nothing changes on a fresh report.
        assertEquals(false, com.callbackdev.chiaro.domain.CurrentEstimate.apply(report, fetched).current.estimated)
    }
}
