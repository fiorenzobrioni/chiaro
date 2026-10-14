package com.callbackdev.chiaro.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.callbackdev.chiaro.data.local.ChiaroDatabase
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
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * What the history keeps and for how long (14 ott 2026): one commit per fetch unless the
 * fetch lands inside the previous commit's window, the rules a commit lists only ever
 * added to, three days a place, never fewer than two rows, and the in-memory cache
 * holding only the places the app still follows.
 */
@RunWith(RobolectricTestRunner::class)
class HistoryRetentionTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val json = Json { ignoreUnknownKeys = true }
    private val milanResponse: ForecastResponseDto = json.decodeFromString(
        ForecastResponseDto.serializer(),
        javaClass.getResource("/openmeteo/milan-2026-09-24.json")!!.readText()
    )
    private lateinit var database: ChiaroDatabase

    private val start: Instant = Instant.parse("2026-09-24T08:00:00Z")

    private class MovableClock(var now: Instant) : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId?): Clock = this
        override fun instant(): Instant = now
    }

    private inner class CountingForecastApi : OpenMeteoForecastApi {
        var calls = 0
        override suspend fun forecast(
            latitude: Double,
            longitude: Double,
            current: String,
            hourly: String,
            daily: String,
            timezone: String,
            forecastDays: Int
        ): ForecastResponseDto {
            calls++
            return milanResponse
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

    private val milan = City(1, "Milano", "Lombardia", "Italia", Coordinates(45.4642, 9.19), "Europe/Rome")
    private val turin = City(2, "Torino", "Piemonte", "Italia", Coordinates(45.0703, 7.6869), "Europe/Rome")

    private val clock = MovableClock(start)
    private val api = CountingForecastApi()
    private var housekept: Set<String>? = null

    private fun repository(liveKeys: Set<String>? = null) = WeatherRepository(
        forecastApi = api,
        airQualityApi = NoAirQuality,
        geocodingApi = NoGeocoding,
        historyDao = database.weatherHistoryDao(),
        json = json,
        clock = clock,
        liveKeys = liveKeys?.let { keys -> suspend { keys } },
        onHousekeeping = { housekept = it }
    )

    private fun rows(city: City = milan) =
        runBlocking { database.weatherHistoryDao().historyFor(city.cacheKey, 1_000) }

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, ChiaroDatabase::class.java)
            .allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `a pull inside the window rewrites the commit instead of standing beside it`() = runBlocking {
        val repository = repository()
        repository.getWeather(milan, forceRefresh = true)
        clock.now = start.plus(Duration.ofMinutes(5))
        repository.getWeather(milan, forceRefresh = true)
        clock.now = start.plus(Duration.ofMinutes(9))
        repository.getWeather(milan, forceRefresh = true)

        assertEquals(3, api.calls)
        assertEquals(listOf(start.plus(Duration.ofMinutes(9)).epochSecond), rows().map { it.timestampEpochSeconds })

        clock.now = start.plus(Duration.ofMinutes(25))
        repository.getWeather(milan, forceRefresh = true)
        assertEquals(2, rows().size)
    }

    /** A commit that stands for two fetches lists what fired on either. */
    @Test
    fun `the rules a commit lists are added to, never written over`() = runBlocking {
        val repository = repository()
        repository.getWeather(milan, forceRefresh = true)
        repository.recordFiredRules(milan, listOf("bici"))
        clock.now = start.plus(Duration.ofMinutes(5))
        repository.getWeather(milan, forceRefresh = true)
        repository.recordFiredRules(milan, listOf("ombrello", "bici"))

        val only = rows().single()
        assertEquals(listOf("bici", "ombrello"), repository.firedRules(only))
    }

    @Test
    fun `a place keeps three days of commits`() = runBlocking {
        val dao = database.weatherHistoryDao()
        listOf(5L, 4L, 2L, 1L).forEach { days ->
            dao.insert(
                WeatherHistoryEntry(
                    cityKey = milan.cacheKey,
                    timestampEpochSeconds = start.minus(Duration.ofDays(days)).epochSecond,
                    forecastJson = "{}"
                )
            )
        }

        repository().getWeather(milan, forceRefresh = true)

        assertEquals(
            listOf(start, start.minus(Duration.ofDays(1)), start.minus(Duration.ofDays(2))).map { it.epochSecond },
            rows().map { it.timestampEpochSeconds }
        )
    }

    /** A phone back from a week in a drawer still has a forecast to compare against. */
    @Test
    fun `never fewer than two commits, however old the last one is`() = runBlocking {
        val dao = database.weatherHistoryDao()
        listOf(10L, 9L).forEach { days ->
            dao.insert(
                WeatherHistoryEntry(
                    cityKey = milan.cacheKey,
                    timestampEpochSeconds = start.minus(Duration.ofDays(days)).epochSecond,
                    forecastJson = "{}"
                )
            )
        }

        repository().getWeather(milan, forceRefresh = true)

        assertEquals(
            listOf(start, start.minus(Duration.ofDays(9))).map { it.epochSecond },
            rows().map { it.timestampEpochSeconds }
        )
    }

    @Test
    fun `the in-memory cache keeps only the places the app follows`() = runBlocking {
        val repository = repository(liveKeys = setOf(milan.cacheKey))
        repository.getWeather(turin, forceRefresh = true)
        assertEquals(1, api.calls)

        // Turin is not followed: the next commit (Milan's) forgets it in memory, so asking
        // for it again inside the TTL goes back to the network (no disk cache here).
        repository.getWeather(milan, forceRefresh = true)
        assertEquals(setOf(milan.cacheKey), housekept)
        repository.getWeather(milan)
        assertEquals("Milan is still a hit", 2, api.calls)
        repository.getWeather(turin)
        assertEquals(3, api.calls)
    }
}
