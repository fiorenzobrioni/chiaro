package com.callbackdev.chiaro.data.mapper

import com.callbackdev.chiaro.data.local.WeatherSnapshots
import com.callbackdev.chiaro.data.remote.dto.ForecastResponseDto
import com.callbackdev.chiaro.data.remote.dto.WitnessResponseDto
import com.callbackdev.chiaro.domain.correction.TemperatureBlend
import com.callbackdev.chiaro.domain.model.CacheStatus
import com.callbackdev.chiaro.domain.model.City
import com.callbackdev.chiaro.domain.model.Coordinates
import java.time.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The blend of days 6 and 7 laid on a response (Fase 18, PLANNING «18.4»), on the recorded
 * Milan of 24 Sep 2026 (147 m, the Po valley), with witnesses written around it.
 */
class TemperatureCorrectionTest {

    private val milan: ForecastResponseDto = Json { ignoreUnknownKeys = true }.decodeFromString(
        ForecastResponseDto.serializer(),
        javaClass.getResource("/openmeteo/milan-full-2026-09-24.json")!!.readText()
    )

    private val city = City(
        id = 3173435, name = "Milano", region = "Lombardia", country = "Italia",
        coordinates = Coordinates(45.4642, 9.19), timezone = "Europe/Rome", countryCode = "IT"
    )

    private val hourly = milan.hourly

    /** IFS and UKMO as [ifs] and [ukmo] of each hour's own temperature, cloud apart from it. */
    private fun witnesses(
        ifs: (Double) -> Double? = { it + 2.0 },
        ukmo: (Double) -> Double? = { it - 1.0 }
    ): WitnessResponseDto {
        fun column(f: (Double) -> Double?) = hourly.temperatureC.joinToString(",") { t -> t?.let(f)?.toString() ?: "null" }
        val n = hourly.time.size
        val body = buildString {
            append("""{"hourly":{"time":[""")
            append(hourly.time.joinToString(",") { "\"$it\"" })
            append("]")
            append(""","temperature_2m_ecmwf_ifs":[${column(ifs)}]""")
            append(""","cloud_cover_ecmwf_ifs":[${List(n) { 40 }.joinToString(",")}]""")
            append(""","temperature_2m_ukmo_global_deterministic_10km":[${column(ukmo)}]""")
            append(""","cloud_cover_ukmo_global_deterministic_10km":[${List(n) { 60 }.joinToString(",")}]""")
            append("}}")
        }
        return WitnessResponseDto(Json.parseToJsonElement(body).jsonObject["hourly"] as JsonObject)
    }

    private fun indicesOf(date: String) = hourly.time.indices.filter { hourly.time[it].startsWith(date) }

    @Test
    fun `the last two days are blended with the table's weights, the others are the model's`() {
        val result = TemperatureCorrection.apply(milan, witnesses(), city)
        val w6 = TemperatureBlend.weights(com.callbackdev.chiaro.domain.correction.BlendZone.PO_VALLEY, 6)!!
        val w7 = TemperatureBlend.weights(com.callbackdev.chiaro.domain.correction.BlendZone.PO_VALLEY, 7)!!
        val dates = milan.daily.time
        assertEquals(mapOf(dates[5] to 6, dates[6] to 7), result.leadDayOfDate)
        for ((d, w) in listOf(5 to w6, 6 to w7)) for (i in indicesOf(dates[d])) {
            val t = hourly.temperatureC[i]!!
            assertEquals(w.bestMatch * t + w.ifs * (t + 2) + w.ukmo * (t - 1), result.forecast.hourly.temperatureC[i]!!, 1e-9)
        }
        for (d in 0..4) for (i in indicesOf(dates[d])) {
            assertEquals(hourly.temperatureC[i], result.forecast.hourly.temperatureC[i])
            assertEquals(hourly.dewPointC[i], result.forecast.hourly.dewPointC[i])
        }
    }

    @Test
    fun `the fields that belong together move together, the amounts do not move`() {
        val corrected = TemperatureCorrection.apply(milan, witnesses(), city).forecast
        val date = milan.daily.time[6]
        for (i in indicesOf(date)) {
            val t = corrected.hourly.temperatureC[i]!!
            val delta = t - hourly.temperatureC[i]!!
            val rh = hourly.humidityPct[i]!!.toDouble()
            // The humidity stays the model's: the dew point is the blended air's at it.
            assertEquals(rh, TemperatureBlend.humidityPct(t, corrected.hourly.dewPointC[i]!!), 1e-6)
            assertEquals(hourly.apparentTemperatureC[i]!! + delta, corrected.hourly.apparentTemperatureC[i]!!, 1e-9)
        }
        assertEquals(hourly.precipitationMm, corrected.hourly.precipitationMm)
        assertEquals(hourly.snowfallCm, corrected.hourly.snowfallCm)
        assertEquals(hourly.humidityPct, corrected.hourly.humidityPct)
        // The day's high and low are made again from its hours.
        val temps = indicesOf(date).map { corrected.hourly.temperatureC[it]!! }
        assertEquals(temps.max(), corrected.daily.temperatureMaxC[6]!!, 1e-9)
        assertEquals(temps.min(), corrected.daily.temperatureMinC[6]!!, 1e-9)
        assertEquals(milan.daily.temperatureMaxC[4], corrected.daily.temperatureMaxC[4])
    }

    @Test
    fun `a witness that is best_match is left out, and with none the hour is the model's`() {
        val date = milan.daily.time[6]
        val w = TemperatureBlend.weights(com.callbackdev.chiaro.domain.correction.BlendZone.PO_VALLEY, 7)!!
        // IFS equal to best_match hour by hour, and its cloud too: the copy test needs both.
        val copy = witnesses(ifs = { it }).let { r ->
            val obj = r.hourly.toMutableMap()
            obj["cloud_cover_ecmwf_ifs"] = Json.parseToJsonElement(hourly.cloudCoverPct.joinToString(",", "[", "]") { it?.toString() ?: "null" })
            WitnessResponseDto(JsonObject(obj))
        }
        val corrected = TemperatureCorrection.apply(milan, copy, city).forecast
        for (i in indicesOf(date)) {
            val t = hourly.temperatureC[i]!!
            assertEquals((w.bestMatch * t + w.ukmo * (t - 1)) / (w.bestMatch + w.ukmo), corrected.hourly.temperatureC[i]!!, 1e-9)
        }
        val none = TemperatureCorrection.apply(milan, witnesses(ifs = { null }, ukmo = { null }), city)
        assertEquals(milan, none.forecast)
        assertTrue(none.leadDayOfDate.isEmpty())
    }

    @Test
    fun `outside Italy, above 350 m or without witnesses nothing changes`() {
        assertEquals(milan, TemperatureCorrection.apply(milan, witnesses(), city.copy(countryCode = "CH")).forecast)
        val high = milan.copy(elevation = 500.0)
        assertEquals(high, TemperatureCorrection.apply(high, witnesses(), city).forecast)
        assertEquals(milan, TemperatureCorrection.apply(milan, null, city).forecast)
    }

    @Test
    fun `the report marks the blended days, and the main path marks none`() {
        val fetchedAt = Instant.parse("2026-09-24T10:00:00Z")
        fun report(corrected: Boolean) = WeatherReportMapper.map(
            city = city, forecast = milan, airQuality = null, fetchedAt = fetchedAt,
            cacheStatus = CacheStatus.MISS, witnesses = witnesses(), corrected = corrected
        )
        val main = report(corrected = false)
        assertFalse(main.systemInfo.corrected)
        assertTrue(main.daily.all { it.correction == null })
        val blended = report(corrected = true)
        assertTrue(blended.systemInfo.corrected)
        val byDate = blended.daily.associate { it.date.toString() to it.correction }
        assertEquals(6, byDate[milan.daily.time[5]])
        assertEquals(7, byDate[milan.daily.time[6]])
        assertNull(byDate[milan.daily.time[4]])
        // The history keeps the mark beside the day's fields.
        val flat = WeatherSnapshots.flattenForecast(blended)
        assertEquals("7", flat["${milan.daily.time[6]}.correction"])
        assertFalse(WeatherSnapshots.flattenForecast(main).keys.any { it.endsWith(".correction") })
    }
}
