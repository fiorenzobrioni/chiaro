package com.callbackdev.chiaro.data.mapper

import com.callbackdev.chiaro.data.local.WeatherSnapshots
import com.callbackdev.chiaro.data.remote.dto.ForecastResponseDto
import com.callbackdev.chiaro.data.remote.dto.WitnessResponseDto
import com.callbackdev.chiaro.domain.correction.BlendZone
import com.callbackdev.chiaro.domain.correction.RainProbability
import com.callbackdev.chiaro.domain.model.CacheStatus
import com.callbackdev.chiaro.domain.model.City
import com.callbackdev.chiaro.domain.model.Coordinates
import java.time.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The first day's calibrated probability laid on a response (Fase 18, PLANNING «18.5»), on the
 * recorded Milan of 24 Sep 2026 with a rainy first day written in and witnesses around it.
 */
class RainProbabilityCorrectionTest {

    private val recorded: ForecastResponseDto = Json { ignoreUnknownKeys = true }.decodeFromString(
        ForecastResponseDto.serializer(),
        javaClass.getResource("/openmeteo/milan-full-2026-09-24.json")!!.readText()
    )

    private val first = recorded.daily.time[0]

    /** The first day's afternoon wet in `best_match`: 1.2 mm at 70% from 14 to 18. */
    private val milan: ForecastResponseDto = recorded.copy(
        hourly = recorded.hourly.copy(
            precipitationMm = recorded.hourly.time.mapIndexed { i, t ->
                if (t.startsWith(first) && t.substring(11, 13).toInt() in 14..18) 1.2 else recorded.hourly.precipitationMm[i]
            },
            precipitationProbabilityPct = recorded.hourly.time.mapIndexed { i, t ->
                if (t.startsWith(first) && t.substring(11, 13).toInt() in 14..18) 70 else recorded.hourly.precipitationProbabilityPct[i]
            }
        )
    )

    private val city = City(
        id = 3173435, name = "Milano", region = "Lombardia", country = "Italia",
        coordinates = Coordinates(45.4642, 9.19), timezone = "Europe/Rome", countryCode = "IT"
    )

    private val ifs = "ecmwf_ifs"
    private val ukmo = "ukmo_global_deterministic_10km"

    /** Two witnesses: IFS wet in the same hours, UKMO dry; cloud apart from best_match's. */
    private fun witnesses(precipitation: Boolean = true): WitnessResponseDto {
        val h = milan.hourly
        val n = h.time.size
        fun col(f: (Int) -> Any?) = (0 until n).joinToString(",") { f(it)?.toString() ?: "null" }
        val body = buildString {
            append("""{"hourly":{"time":[""" + h.time.joinToString(",") { "\"$it\"" } + "]")
            append(""","temperature_2m_$ifs":[${col { h.temperatureC[it]?.plus(1.5) }}]""")
            append(""","cloud_cover_$ifs":[${col { 40 }}]""")
            append(""","temperature_2m_$ukmo":[${col { h.temperatureC[it]?.minus(1.5) }}]""")
            append(""","cloud_cover_$ukmo":[${col { 60 }}]""")
            if (precipitation) {
                append(""","precipitation_$ifs":[${col { h.precipitationMm[it] }}]""")
                append(""","precipitation_$ukmo":[${col { 0.0 }}]""")
            }
            append("}}")
        }
        return WitnessResponseDto(Json.parseToJsonElement(body).jsonObject["hourly"] as JsonObject)
    }

    private fun indicesOf(date: String) = milan.hourly.time.indices.filter { milan.hourly.time[it].startsWith(date) }

    @Test
    fun `the first day takes the regression's probability, the other days stay the model's`() {
        val result = RainProbabilityCorrection.apply(milan, witnesses(), city)
        assertEquals(mapOf(first to 1), result.leadDayOfDate)
        val h = milan.hourly
        for (i in indicesOf(first)) {
            val expected = RainProbability.calibrated(
                BlendZone.PO_VALLEY, h.precipitationProbabilityPct[i]!!, h.precipitationMm[i]!!,
                listOf(h.precipitationMm[i]!!, 0.0)
            )
            assertEquals(expected, result.forecast.hourly.precipitationProbabilityPct[i])
        }
        for (d in 1 until milan.daily.time.size) for (i in indicesOf(milan.daily.time[d])) {
            assertEquals(h.precipitationProbabilityPct[i], result.forecast.hourly.precipitationProbabilityPct[i])
        }
        // The day's highest from its hours; the amounts untouched.
        val calibrated = indicesOf(first).map { result.forecast.hourly.precipitationProbabilityPct[it]!! }
        assertEquals(calibrated.max(), result.forecast.daily.precipitationProbabilityMaxPct[0])
        assertEquals(milan.daily.precipitationProbabilityMaxPct.drop(1), result.forecast.daily.precipitationProbabilityMaxPct.drop(1))
        assertEquals(h.precipitationMm, result.forecast.hourly.precipitationMm)
    }

    @Test
    fun `without the witnesses' rain, outside the Po valley or above 350 m nothing changes`() {
        assertEquals(milan, RainProbabilityCorrection.apply(milan, witnesses(precipitation = false), city).forecast)
        assertEquals(milan, RainProbabilityCorrection.apply(milan, null, city).forecast)
        val rome = milan.copy(latitude = 41.9)
        assertEquals(rome, RainProbabilityCorrection.apply(rome, witnesses(), city).forecast)
        val high = milan.copy(elevation = 500.0)
        assertEquals(high, RainProbabilityCorrection.apply(high, witnesses(), city).forecast)
    }

    @Test
    fun `the report marks the first day, and the history keeps the mark`() {
        val report = WeatherReportMapper.map(
            city = city, forecast = milan, airQuality = null, fetchedAt = Instant.parse("2026-09-24T10:00:00Z"),
            cacheStatus = CacheStatus.MISS, witnesses = witnesses(), corrected = true
        )
        assertEquals(1, report.daily.first { it.date.toString() == first }.correction)
        assertEquals("1", WeatherSnapshots.flattenForecast(report)["$first.correction"])
        val main = WeatherReportMapper.map(
            city = city, forecast = milan, airQuality = null, fetchedAt = Instant.parse("2026-09-24T10:00:00Z"),
            cacheStatus = CacheStatus.MISS, witnesses = witnesses(), corrected = false
        )
        assertTrue(main.daily.all { it.correction == null })
    }
}
