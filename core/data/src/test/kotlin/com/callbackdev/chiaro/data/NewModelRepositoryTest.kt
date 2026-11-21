package com.callbackdev.chiaro.data

import com.callbackdev.chiaro.data.local.WeatherHistoryDao
import com.callbackdev.chiaro.data.local.WeatherHistoryEntry
import com.callbackdev.chiaro.data.remote.OpenMeteoAirQualityApi
import com.callbackdev.chiaro.data.remote.OpenMeteoForecastApi
import com.callbackdev.chiaro.data.remote.OpenMeteoGeocodingApi
import com.callbackdev.chiaro.data.remote.OpenMeteoModelForecastApi
import com.callbackdev.chiaro.data.remote.dto.AirQualityResponseDto
import com.callbackdev.chiaro.data.remote.dto.ForecastResponseDto
import com.callbackdev.chiaro.data.remote.dto.GeocodingResponseDto
import com.callbackdev.chiaro.data.remote.dto.ModelCodesResponseDto
import com.callbackdev.chiaro.domain.WeatherException
import com.callbackdev.chiaro.domain.model.City
import com.callbackdev.chiaro.domain.model.Coordinates
import java.io.IOException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

/**
 * The new model in the repository (prova del nuovo modello, 21 nov 2026): its own roads only —
 * MeteoSwiss, else ItaliaMeteo, else `best_match` outside both, said as such — and kept apart
 * from the current source (committente, the same day): a failure of it is a failed fetch,
 * never a silent `best_match` in its place.
 */
class NewModelRepositoryTest {

    private val milan: ForecastResponseDto = Json { ignoreUnknownKeys = true }.decodeFromString(
        ForecastResponseDto.serializer(),
        javaClass.getResource("/openmeteo/milan-2026-09-24.json")!!.readText()
    )

    private class CountingForecastApi(private val answer: ForecastResponseDto) : OpenMeteoForecastApi {
        var calls = 0
            private set

        override suspend fun forecast(
            latitude: Double, longitude: Double, current: String, hourly: String, daily: String,
            timezone: String, forecastDays: Int, minutely15: String, forecastQuarters: Int, pastQuarters: Int
        ): ForecastResponseDto {
            calls++
            return answer
        }
    }

    /** Answers each model from [answers]: a response, or the exception to throw. */
    private class ScriptedModelApi(private val answers: Map<String, Any>) : OpenMeteoModelForecastApi {
        val asked = mutableListOf<String>()

        override suspend fun forecast(
            latitude: Double, longitude: Double, models: String, current: String, hourly: String,
            daily: String, timezone: String, forecastDays: Int, minutely15: String,
            forecastQuarters: Int, pastQuarters: Int
        ): ForecastResponseDto {
            asked += models
            return when (val answer = answers[models]) {
                is ForecastResponseDto -> answer
                is Throwable -> throw answer
                else -> throw http(400)
            }
        }

        override suspend fun codes(
            latitude: Double, longitude: Double, models: String, hourly: String, timezone: String, forecastDays: Int
        ): ModelCodesResponseDto {
            asked += "codes"
            throw http(400)
        }
    }

    private object NoAirQuality : OpenMeteoAirQualityApi {
        override suspend fun current(
            latitude: Double, longitude: Double, current: String, timezone: String, hourly: String, forecastDays: Int
        ): AirQualityResponseDto = throw IOException("best effort")
    }

    private object NoGeocoding : OpenMeteoGeocodingApi {
        override suspend fun search(name: String, language: String, count: Int, format: String) = GeocodingResponseDto()
    }

    private object NoHistory : WeatherHistoryDao {
        override suspend fun insert(entry: WeatherHistoryEntry): Long = 1
        override suspend fun update(entry: WeatherHistoryEntry) = Unit
        override suspend fun historyFor(cityKey: String, limit: Int) = emptyList<WeatherHistoryEntry>()
        override suspend fun latestFor(cityKey: String): WeatherHistoryEntry? = null
        override suspend fun any(): Boolean = false
        override suspend fun pruneCity(cityKey: String, cutoffEpochSeconds: Long, keepAtLeast: Int) = Unit
        override suspend fun prune(keep: Int) = Unit
        override suspend fun pruneForeign(liveKeys: List<String>, cutoffEpochSeconds: Long) = Unit
        override suspend fun setFiredRules(id: Long, firedRulesJson: String) = Unit
    }

    private val city = City(1, "Milano", "Lombardia", "Italia", Coordinates(45.4642, 9.19), "Europe/Rome")

    private fun repository(
        bestMatch: CountingForecastApi,
        models: ScriptedModelApi,
        source: ForecastSource = ForecastSource.NEW
    ) = WeatherRepository(
        forecastApi = bestMatch,
        airQualityApi = NoAirQuality,
        geocodingApi = NoGeocoding,
        historyDao = NoHistory,
        modelApi = models,
        forecastSource = { source }
    )

    @Test
    fun `where MeteoSwiss covers, the week is the new model's and best_match is not asked`() = runTest {
        val bestMatch = CountingForecastApi(milan)
        val models = ScriptedModelApi(
            mapOf(OpenMeteoModelForecastApi.METEOSWISS to milan, OpenMeteoModelForecastApi.ECMWF to milan)
        )
        val report = repository(bestMatch, models).getWeather(city, forceRefresh = true)
        assertEquals(0, bestMatch.calls)
        assertEquals(OpenMeteoModelForecastApi.METEOSWISS, report.systemInfo.models.first())
        assertTrue("the composed week draws no quarters", report.nowcast == null)
    }

    @Test
    fun `outside MeteoSwiss, ItaliaMeteo`() = runTest {
        val bestMatch = CountingForecastApi(milan)
        val models = ScriptedModelApi(
            mapOf(OpenMeteoModelForecastApi.ITALIA_METEO to milan, OpenMeteoModelForecastApi.ECMWF to milan)
        )
        val report = repository(bestMatch, models).getWeather(city, forceRefresh = true)
        assertEquals(0, bestMatch.calls)
        assertEquals(OpenMeteoModelForecastApi.ITALIA_METEO, report.systemInfo.models.first())
    }

    @Test
    fun `outside every domain the new model is best_match by design, said as such, and the domains are not asked again`() = runTest {
        val bestMatch = CountingForecastApi(milan)
        val models = ScriptedModelApi(emptyMap())
        val repository = repository(bestMatch, models)
        val first = repository.getWeather(city, forceRefresh = true)
        assertEquals(1, bestMatch.calls)
        assertEquals(listOf(OpenMeteoModelForecastApi.BEST_MATCH), first.systemInfo.models)
        assertEquals(OpenMeteoModelForecastApi.REGIONAL, models.asked)
        repository.getWeather(city, forceRefresh = true)
        assertEquals(2, bestMatch.calls)
        assertEquals("remembered: no second round of 400s", OpenMeteoModelForecastApi.REGIONAL, models.asked)
    }

    @Test
    fun `a part of the new model that fails is a failed fetch, never best_match in its place`() = runTest {
        val bestMatch = CountingForecastApi(milan)
        val models = ScriptedModelApi(
            mapOf(OpenMeteoModelForecastApi.METEOSWISS to milan, OpenMeteoModelForecastApi.ECMWF to IOException("reset"))
        )
        val failure = runCatching { repository(bestMatch, models).getWeather(city, forceRefresh = true) }.exceptionOrNull()
        assertTrue(failure is WeatherException)
        assertEquals(0, bestMatch.calls)
    }

    @Test
    fun `a regional model that fails with anything but «no data here» is a failed fetch too`() = runTest {
        val bestMatch = CountingForecastApi(milan)
        val models = ScriptedModelApi(mapOf(OpenMeteoModelForecastApi.METEOSWISS to http(500)))
        val failure = runCatching { repository(bestMatch, models).getWeather(city, forceRefresh = true) }.exceptionOrNull()
        assertTrue(failure is WeatherException)
        assertEquals(0, bestMatch.calls)
        assertTrue("not taken for «outside its domain»", OpenMeteoModelForecastApi.ITALIA_METEO !in models.asked)
    }

    @Test
    fun `with the current source the new model is never asked`() = runTest {
        val bestMatch = CountingForecastApi(milan)
        val models = ScriptedModelApi(mapOf(OpenMeteoModelForecastApi.METEOSWISS to milan))
        repository(bestMatch, models, ForecastSource.CURRENT).getWeather(city, forceRefresh = true)
        assertEquals(1, bestMatch.calls)
        assertTrue(models.asked.isEmpty())
    }

    @Test
    fun `a report of one source is not a cache hit of the other`() = runTest {
        val bestMatch = CountingForecastApi(milan)
        val models = ScriptedModelApi(
            mapOf(OpenMeteoModelForecastApi.METEOSWISS to milan, OpenMeteoModelForecastApi.ECMWF to milan)
        )
        var source = ForecastSource.CURRENT
        val repository = WeatherRepository(
            forecastApi = bestMatch,
            airQualityApi = NoAirQuality,
            geocodingApi = NoGeocoding,
            historyDao = NoHistory,
            modelApi = models,
            forecastSource = { source }
        )
        repository.getWeather(city)
        assertEquals(1, bestMatch.calls)
        source = ForecastSource.NEW
        val switched = repository.getWeather(city)
        assertEquals(OpenMeteoModelForecastApi.METEOSWISS, switched.systemInfo.models.first())
        source = ForecastSource.CURRENT
        repository.getWeather(city)
        assertEquals("the in-memory entry of the new source is not this one's either", 2, bestMatch.calls)
    }

    private companion object {
        fun http(code: Int): HttpException = HttpException(
            Response.error<Any>(
                code,
                """{"error":true,"reason":"No data is available for this location"}""".toResponseBody("application/json".toMediaType())
            )
        )
    }
}
