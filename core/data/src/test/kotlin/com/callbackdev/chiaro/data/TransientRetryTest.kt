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
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException
import java.time.Duration
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
 * And since 17 ott 2026 a connection the service dropped gets one too, and is told
 * apart from an offline phone.
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
    fun `a 4xx, a 429, an offline phone and a spent call budget are not retried`() = runTest {
        listOf(
            http(400),
            http(429),
            UnknownHostException("api.open-meteo.com"),
            InterruptedIOException("timeout")
        ).forEach { failure ->
            val api = ScriptedForecastApi(listOf(failure), milan)
            try {
                repository(api).getWeather(city, forceRefresh = true)
                fail("expected a failure for $failure")
            } catch (e: WeatherException) {
                assertTrue(
                    e is WeatherException.ApiError || e is WeatherException.NoNetwork ||
                        e is WeatherException.Unreachable
                )
            }
            assertEquals("$failure", 1, api.calls)
        }
    }

    @Test
    fun `a dropped connection gets one more try, and the second answer is the report`() = runTest {
        listOf(
            SSLHandshakeException("Connection reset"),
            ConnectException("refused"),
            SocketTimeoutException("read timed out"),
            IOException("unexpected end of stream")
        ).forEach { failure ->
            val api = ScriptedForecastApi(listOf(failure), milan)
            val report = repository(api).getWeather(city, forceRefresh = true)
            assertEquals("$failure", 2, api.calls)
            assertEquals("Europe/Rome", report.location.timezone)
        }
    }

    @Test
    fun `only a name that does not resolve says offline`() = runTest {
        suspend fun failureFor(e: Throwable): WeatherException = try {
            repository(ScriptedForecastApi(listOf(e, e), milan)).getWeather(city, forceRefresh = true)
            fail("expected a failure for $e")
            throw AssertionError()
        } catch (w: WeatherException) {
            w
        }
        assertTrue(failureFor(UnknownHostException("api.open-meteo.com")) is WeatherException.NoNetwork)
        assertTrue(failureFor(SSLHandshakeException("reset")) is WeatherException.Unreachable)
        assertTrue(failureFor(SocketTimeoutException("timeout")) is WeatherException.Unreachable)
        assertTrue(failureFor(InterruptedIOException("timeout")) is WeatherException.Unreachable)
    }

    private companion object {
        val TRANSIENT = listOf(500, 502, 503, 504)
    }
}
