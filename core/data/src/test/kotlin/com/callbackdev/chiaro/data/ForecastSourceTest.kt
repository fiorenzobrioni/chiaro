package com.callbackdev.chiaro.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.callbackdev.chiaro.data.local.WeatherHistoryDao
import com.callbackdev.chiaro.data.local.WeatherHistoryEntry
import com.callbackdev.chiaro.data.remote.OpenMeteoAirQualityApi
import com.callbackdev.chiaro.data.remote.OpenMeteoForecastApi
import com.callbackdev.chiaro.data.remote.OpenMeteoGeocodingApi
import com.callbackdev.chiaro.data.remote.dto.AirQualityResponseDto
import com.callbackdev.chiaro.data.remote.dto.ForecastResponseDto
import com.callbackdev.chiaro.data.remote.dto.GeocodingResponseDto
import com.callbackdev.chiaro.domain.model.City
import com.callbackdev.chiaro.domain.model.Coordinates
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * The switch between the corrected forecast and main's (Fase 18, 21 nov 2026, the
 * committente: one install, the two models side by side through Settings). Both read the
 * same download, so a switch re-maps and never fetches; an in-memory report of the other
 * source is never served; and the store remembers when the source changed, which is where
 * «Cosa è cambiato» starts again.
 */
class ForecastSourceTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @After
    fun tearDown() {
        scope.cancel()
    }

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

    private object NoAirQuality : OpenMeteoAirQualityApi {
        override suspend fun current(
            latitude: Double, longitude: Double, current: String, timezone: String, hourly: String, forecastDays: Int
        ): AirQualityResponseDto = throw IOException("best effort")
    }

    private object NoGeocoding : OpenMeteoGeocodingApi {
        override suspend fun search(name: String, language: String, count: Int, format: String) =
            GeocodingResponseDto()
    }

    private object NoHistory : WeatherHistoryDao {
        override suspend fun insert(entry: WeatherHistoryEntry): Long = 1L
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

    @Test
    fun `the report says which source mapped it, and a switch never serves the other's`() = runBlocking {
        val api = CountingForecastApi(milan)
        var source = ForecastSource.CORRECTED
        val repository = WeatherRepository(
            forecastApi = api, airQualityApi = NoAirQuality, geocodingApi = NoGeocoding,
            historyDao = NoHistory, forecastSource = { source }
        )

        assertTrue(repository.getWeather(city).systemInfo.corrected)
        assertTrue(repository.getWeather(city).systemInfo.corrected)
        assertEquals(1, api.calls)

        source = ForecastSource.CURRENT
        // No disk cache in this test: the other source is a fetch here, never the cached report.
        assertFalse(repository.getWeather(city).systemInfo.corrected)
        assertEquals(2, api.calls)
        assertFalse(repository.cachedReport(city)!!.systemInfo.corrected)
    }

    @Test
    fun `with the disk cache a switch re-maps what is there and fetches nothing`() = runBlocking {
        val api = CountingForecastApi(milan)
        var source = ForecastSource.CURRENT
        val repository = WeatherRepository(
            forecastApi = api, airQualityApi = NoAirQuality, geocodingApi = NoGeocoding,
            historyDao = NoHistory, forecastSource = { source },
            diskCache = com.callbackdev.chiaro.data.local.ReportDiskCache(tmp.newFolder("cache"), Json)
        )

        assertFalse(repository.getWeather(city).systemInfo.corrected)
        source = ForecastSource.CORRECTED
        assertTrue(repository.getWeather(city).systemInfo.corrected)
        assertTrue(repository.cachedReport(city)!!.systemInfo.corrected)
        assertEquals(1, api.calls)
    }

    @Test
    fun `the store remembers when the source changed, and choosing the same one is no change`() = runBlocking {
        val store = SettingsStore(PreferenceDataStoreFactory.create(scope = scope) { tmp.newFile("s.preferences_pb") })

        assertEquals(ForecastSource.CORRECTED, store.settings.first().forecastSource)
        assertEquals(0L, store.settings.first().forecastSourceSinceEpochS)

        // The first fetch on a phone that had main's build records the start of the corrected one.
        assertEquals(ForecastSource.CORRECTED, store.forecastSourceForFetch(nowEpochS = 100))
        assertEquals(100L, store.settings.first().forecastSourceSinceEpochS)
        store.forecastSourceForFetch(nowEpochS = 200)
        assertEquals(100L, store.settings.first().forecastSourceSinceEpochS)

        store.setForecastSource(ForecastSource.CORRECTED, nowEpochS = 300)
        assertEquals(100L, store.settings.first().forecastSourceSinceEpochS)
        store.setForecastSource(ForecastSource.CURRENT, nowEpochS = 400)
        assertEquals(ForecastSource.CURRENT, store.settings.first().forecastSource)
        assertEquals(400L, store.settings.first().forecastSourceSinceEpochS)
        assertEquals(ForecastSource.CURRENT, store.forecastSourceForFetch(nowEpochS = 500))
    }
}
