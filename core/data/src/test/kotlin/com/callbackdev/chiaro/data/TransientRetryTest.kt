package com.callbackdev.chiaro.data

import com.callbackdev.chiaro.data.local.WeatherHistoryDao
import com.callbackdev.chiaro.data.local.WeatherHistoryEntry
import com.callbackdev.chiaro.data.remote.OpenMeteoAirQualityApi
import com.callbackdev.chiaro.data.remote.OpenMeteoForecastApi
import com.callbackdev.chiaro.data.remote.OpenMeteoGeocodingApi
import com.callbackdev.chiaro.data.remote.dto.AirQualityResponseDto
import com.callbackdev.chiaro.data.remote.dto.ForecastResponseDto
import com.callbackdev.chiaro.data.remote.dto.GeocodingResponseDto
import com.callbackdev.chiaro.domain.WeatherException
import com.callbackdev.chiaro.domain.model.City
import com.callbackdev.chiaro.domain.model.Coordinates
import java.io.IOException
import java.time.Duration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

/**
 * The one retry of a transient failure (24 set 2026, the engine review's suggestion 4):
 * a 5xx gets a second try, and nothing else does — not a 4xx, which will fail the same
 * way; not a 429, whose limit a quick retry only spends further; not an offline phone.
 */
class TransientRetryTest {

    private val milan: ForecastResponseDto = Json { ignoreUnknownKeys = true }.decodeFromString(
        ForecastResponseDto.serializer(),
        javaClass.getResource("/openmeteo/milan-2026-09-24.json")!!.readText()
    )

    /** Fails with each of [failures] in turn, then answers with Milan. */
    private class ScriptedForecastApi(
        private val failures: List<Throwable>,
        private val answer: ForecastResponseDto
    ) : OpenMeteoForecastApi {
        var calls = 0
            private set

        override suspend fun forecast(
            latitude: Double,
            longitude: Double,
            current: String,
            hourly: String,
            daily: String,
            timezone: String,
            forecastDays: Int
        ): ForecastResponseDto {
            val failure = failures.getOrNull(calls)
            calls++
            if (failure != null) throw failure
            return answer
        }
    }

    private object NoAirQuality : OpenMeteoAirQualityApi {
        override suspend fun current(
            latitude: Double,
            longitude: Double,
            current: String,
            timezone: String
        ): AirQualityResponseDto = throw IOException("best effort")
    }

    private object NoGeocoding : OpenMeteoGeocodingApi {
        override suspend fun search(name: String, language: String, count: Int, format: String) =
            GeocodingResponseDto()
    }

    private class CountingHistoryDao : WeatherHistoryDao {
        var inserts = 0
        override suspend fun insert(entry: WeatherHistoryEntry): Long = (++inserts).toLong()
        override suspend fun historyFor(cityKey: String, limit: Int) = emptyList<WeatherHistoryEntry>()
        override fun observeLatest(limit: Int): Flow<List<WeatherHistoryEntry>> = emptyFlow()
        override fun observeFor(cityKey: String, limit: Int): Flow<List<WeatherHistoryEntry>> = emptyFlow()
        override suspend fun pruneCity(cityKey: String, keep: Int) = Unit
        override suspend fun prune(keep: Int) = Unit
        override suspend fun pruneForeign(liveKeys: List<String>, cutoffEpochSeconds: Long) = Unit
        override suspend fun setFiredRulesOnLatest(cityKey: String, firedRulesJson: String) = Unit
        override suspend fun setSkyRunsOnLatest(cityKey: String, skyRunsJson: String) = Unit
    }

    private val city = City(1, "Milano", "Lombardia", "Italia", Coordinates(45.4642, 9.19), "Europe/Rome")

    private fun http(code: Int) = HttpException(Response.error<Any>(code, "".toResponseBody()))

    private fun repository(api: OpenMeteoForecastApi, dao: WeatherHistoryDao = CountingHistoryDao()) =
        WeatherRepository(
            forecastApi = api,
            airQualityApi = NoAirQuality,
            geocodingApi = NoGeocoding,
            historyDao = dao,
            transientRetryDelay = Duration.ofSeconds(2)
        )

    @Test
    fun `a 5xx gets one more try, and the second answer is the report`() = runTest {
        TRANSIENT.forEach { code ->
            val api = ScriptedForecastApi(listOf(http(code)), milan)
            val dao = CountingHistoryDao()
            val report = repository(api, dao).getWeather(city, forceRefresh = true)
            assertEquals(2, api.calls)
            assertEquals(1, dao.inserts)
            assertEquals("Europe/Rome", report.location.timezone)
        }
    }

    @Test
    fun `two 5xx in a row are the service's answer`() = runTest {
        val api = ScriptedForecastApi(listOf(http(503), http(503)), milan)
        try {
            repository(api).getWeather(city, forceRefresh = true)
            fail("expected an ApiError")
        } catch (e: WeatherException.ApiError) {
            assertEquals(503, e.code)
        }
        assertEquals(2, api.calls)
    }

    @Test
    fun `a 4xx, a 429 and an offline phone are not retried`() = runTest {
        listOf(http(400), http(429), IOException("offline")).forEach { failure ->
            val api = ScriptedForecastApi(listOf(failure), milan)
            try {
                repository(api).getWeather(city, forceRefresh = true)
                fail("expected a failure for $failure")
            } catch (e: WeatherException) {
                assertTrue(e is WeatherException.ApiError || e is WeatherException.NoNetwork)
            }
            assertEquals("$failure", 1, api.calls)
        }
    }

    private companion object {
        val TRANSIENT = listOf(500, 502, 503, 504)
    }
}
