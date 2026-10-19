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
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
            forecastDays: Int,
            minutely15: String,
            forecastQuarters: Int
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

    /** An error status, with the body Open-Meteo would put under it when [reason] is given. */
    private fun http(code: Int, reason: String? = null): HttpException {
        val body = reason?.let { """{"error":true,"reason":"$it"}""" } ?: ""
        return HttpException(Response.error<Any>(code, body.toResponseBody("application/json".toMediaType())))
    }

    private fun repository(
        api: OpenMeteoForecastApi,
        dao: WeatherHistoryDao = CountingHistoryDao(),
        jitter: Duration = Duration.ZERO
    ) = WeatherRepository(
        forecastApi = api,
        airQualityApi = NoAirQuality,
        geocodingApi = NoGeocoding,
        historyDao = dao,
        transientRetryDelay = Duration.ofSeconds(2),
        transientRetryJitter = jitter
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
    fun `a 4xx, a 429 on a limit, an offline phone and any timeout are not retried`() = runTest {
        listOf(
            http(400, "Cannot initialize Float from invalid String value"),
            http(429),
            http(429, MINUTELY_LIMIT),
            http(429, "Daily API request limit exceeded. Please try again tomorrow."),
            UnknownHostException("api.open-meteo.com"),
            // Time already spent (19 ott 2026): a read's or connect's ten or twenty
            // seconds, the call's thirty — a second try after either never resolves.
            SocketTimeoutException("read timed out"),
            SocketTimeoutException("connect timed out"),
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
            IOException("unexpected end of stream")
        ).forEach { failure ->
            val api = ScriptedForecastApi(listOf(failure), milan)
            val report = repository(api).getWeather(city, forceRefresh = true)
            assertEquals("$failure", 2, api.calls)
            assertEquals("Europe/Rome", report.location.timezone)
        }
    }

    /** The one 429 that clears by itself: the address's queue, full for a few seconds. */
    @Test
    fun `a 429 for the address's queue gets one more try, a 429 for a limit does not`() = runTest {
        val queued = ScriptedForecastApi(listOf(http(429, WeatherRepository.CONCURRENCY_LIMIT_REASON)), milan)
        val report = repository(queued).getWeather(city, forceRefresh = true)
        assertEquals(2, queued.calls)
        assertEquals("Europe/Rome", report.location.timezone)

        val limited = ScriptedForecastApi(listOf(http(429, MINUTELY_LIMIT)), milan)
        try {
            repository(limited).getWeather(city, forceRefresh = true)
            fail("expected an ApiError")
        } catch (e: WeatherException.ApiError) {
            assertEquals(429, e.code)
            assertEquals(MINUTELY_LIMIT, e.reason)
        }
        assertEquals(1, limited.calls)
    }

    /** The service's own sentence rides on the error, read off the body once. */
    @Test
    fun `the reason the service gives is the error's, and says whether a later try is worth it`() = runTest {
        val overloaded = "The service is overloaded"
        val api = ScriptedForecastApi(listOf(http(503, overloaded), http(503, overloaded)), milan)
        try {
            repository(api).getWeather(city, forceRefresh = true)
            fail("expected an ApiError")
        } catch (e: WeatherException.ApiError) {
            assertEquals(503, e.code)
            assertEquals(overloaded, e.reason)
            assertTrue(e.message!!.contains(overloaded))
            assertTrue(e.isTransient)
        }
        // An empty body, or one that is not Open-Meteo's JSON, is no reason at all.
        assertNull(WeatherException.ApiError(502).reason)
        val html = ScriptedForecastApi(listOf(http(400, null)), milan)
        try {
            repository(html).getWeather(city, forceRefresh = true)
            fail("expected an ApiError")
        } catch (e: WeatherException.ApiError) {
            assertNull(e.reason)
            assertFalse(e.isTransient)
        }
        assertTrue(WeatherException.ApiError(429).isTransient)
        assertFalse(WeatherException.ApiError(404).isTransient)
    }

    /** Two seconds at least, six at most: the floor a restarting node needs, a spread
     * so that one 503 does not bring every phone back together. */
    @Test
    fun `the retry waits two to six seconds, not the same two for everyone`() {
        val repository = repository(ScriptedForecastApi(emptyList(), milan), jitter = Duration.ofSeconds(4))
        val waits = List(500) { repository.nextRetryDelay() }
        assertTrue(waits.all { it >= Duration.ofSeconds(2) && it <= Duration.ofSeconds(6) })
        assertTrue(waits.distinct().size > 1)
        val fixed = repository(ScriptedForecastApi(emptyList(), milan), jitter = Duration.ZERO)
        assertEquals(Duration.ofSeconds(2), fixed.nextRetryDelay())
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
        const val MINUTELY_LIMIT = "Minutely API request limit exceeded. Please try again in one minute."
    }
}
