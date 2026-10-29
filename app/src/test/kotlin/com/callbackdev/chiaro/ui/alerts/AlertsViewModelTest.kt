package com.callbackdev.chiaro.ui.alerts

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.callbackdev.chiaro.data.CityStore
import com.callbackdev.chiaro.data.RuleStateStore
import com.callbackdev.chiaro.data.RuleStore
import com.callbackdev.chiaro.data.ServiceLocator
import com.callbackdev.chiaro.data.SettingsStore
import com.callbackdev.chiaro.data.WeatherRepository
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
import com.callbackdev.chiaro.domain.model.notificationKey
import com.callbackdev.chiaro.domain.rules.NotificationRule
import com.callbackdev.chiaro.domain.rules.RuleCondition
import com.callbackdev.chiaro.domain.rules.RuleEngine
import com.callbackdev.chiaro.domain.rules.RuleOp
import com.callbackdev.chiaro.domain.rules.RulePlace
import com.callbackdev.chiaro.domain.rules.RuleTrigger
import com.callbackdev.chiaro.domain.rules.RuleWindow
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The page's own bookkeeping of the rules (15.4, 28 Oct 2026): a rule switched back on is
 * re-armed, and «last fired» is read off the history by id, by name only off the rows written
 * before 15.4. Real stores on temp files and a real in-memory history; the network is a stub
 * that fails, because nothing here needs a forecast, but for the rules bound to a place (15.11),
 * whose cards are read on the README's recorded Milan.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class AlertsViewModelTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private fun prefs(name: String) = PreferenceDataStoreFactory.create(scope = scope) { tmp.newFile(name) }

    private val settingsStore = SettingsStore(prefs("settings.preferences_pb"))
    private val ruleStore = RuleStore(prefs("rules.preferences_pb"), Json)
    private val ruleStateStore = RuleStateStore(prefs("rule_state.preferences_pb"))
    private val cityStore = CityStore(prefs("cities.preferences_pb"), Json)
    private lateinit var database: ChiaroDatabase

    private val milan = City(
        id = 3173435, name = "Milano", region = "Lombardia", country = "Italia",
        coordinates = Coordinates(45.4642, 9.19), timezone = "Europe/Rome"
    )
    private val turin = City(
        id = 3165524, name = "Torino", region = "Piemonte", country = "Italia",
        coordinates = Coordinates(45.0703, 7.6869), timezone = "Europe/Rome"
    )
    private val now: Instant = Instant.parse("2026-10-28T08:00:00Z")
    private val warm = listOf(RuleCondition("current.temp_c", RuleOp.GTE, 20.0))

    private object NoForecast : OpenMeteoForecastApi {
        override suspend fun forecast(
            latitude: Double, longitude: Double, current: String, hourly: String, daily: String,
            timezone: String, forecastDays: Int, minutely15: String, forecastQuarters: Int
        ): ForecastResponseDto = throw IOException("no network in this test")
    }

    /** Milan as the README's pictures record it (`tools/record_readme_data.py`), fetched at
     * [Recorded.fetchedAt]. */
    private object Recorded : OpenMeteoForecastApi {
        val fetchedAt: Instant = Instant.parse("2026-09-25T10:10:22Z")
        override suspend fun forecast(
            latitude: Double, longitude: Double, current: String, hourly: String, daily: String,
            timezone: String, forecastDays: Int, minutely15: String, forecastQuarters: Int
        ): ForecastResponseDto = Json { ignoreUnknownKeys = true }.decodeFromString(
            ForecastResponseDto.serializer(),
            checkNotNull(javaClass.getResource("/readme/forecast.json")).readText()
        )
    }

    private object NoAirQuality : OpenMeteoAirQualityApi {
        override suspend fun current(
            latitude: Double, longitude: Double, current: String, timezone: String
        ): AirQualityResponseDto = throw IOException("best effort")
    }

    private object NoGeocoding : OpenMeteoGeocodingApi {
        override suspend fun search(name: String, language: String, count: Int, format: String) =
            GeocodingResponseDto()
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        database = Room.inMemoryDatabaseBuilder(context, ChiaroDatabase::class.java)
            .allowMainThreadQueries().build()
        // The scheduler's reconcile, run after every edit, reads the stores from here.
        ServiceLocator.overrideForTests(
            cityStore = cityStore, settingsStore = settingsStore,
            ruleStore = ruleStore, ruleStateStore = ruleStateStore
        )
    }

    @After
    fun tearDown() {
        ServiceLocator.overrideForTests()
        database.close()
        scope.cancel()
        Dispatchers.resetMain()
    }

    private fun viewModel(
        forecast: OpenMeteoForecastApi = NoForecast,
        at: Instant = now
    ) = AlertsViewModel(
        appContext = context,
        settingsStore = settingsStore,
        ruleStore = ruleStore,
        ruleStateStore = ruleStateStore,
        cityStore = cityStore,
        repository = WeatherRepository(
            forecastApi = forecast,
            airQualityApi = NoAirQuality,
            geocodingApi = NoGeocoding,
            historyDao = database.weatherHistoryDao(),
            clock = Clock.fixed(at, ZoneOffset.UTC)
        ),
        warningReader = null,
        clock = Clock.fixed(at, ZoneOffset.UTC)
    )

    private suspend fun rule(name: String) = checkNotNull(ruleStore.add(name, warm, "m"))

    private suspend fun latched(key: String) = key in ruleStateStore.state.first().latched

    /** The stores write on their own threads: the test waits for what it asked to land. */
    private suspend fun await(condition: suspend () -> Boolean) {
        withTimeout(10_000) {
            while (!condition()) kotlinx.coroutines.delay(20)
        }
    }

    // ------------------------------------------------------------------ the re-arm

    @Test
    fun `a rule switched back on is re-armed`() = runBlocking {
        cityStore.add(milan)
        val rule = rule("Caldo")
        val latch = RuleEngine.latchKey(milan.notificationKey, rule.id)
        ruleStateStore.record(RuleTrigger(rule, fingerprint = null, latchKey = latch, value = 21.0, at = null))
        await { latched(latch) }
        val viewModel = viewModel()

        // Off: nothing to re-arm at rest, the latch stays.
        viewModel.update(rule.copy(enabled = false))
        await { !ruleStore.rules.first().single().enabled }
        assertTrue(latched(latch))

        // On again: the worker never read it false while it was off, so the page unlatches it.
        viewModel.update(rule.copy(enabled = true))
        await { !latched(latch) }
        assertTrue(ruleStore.rules.first().single().enabled)
    }

    @Test
    fun `a rename leaves the latch alone`() = runBlocking {
        cityStore.add(milan)
        val rule = rule("Caldo")
        val latch = RuleEngine.latchKey(milan.notificationKey, rule.id)
        ruleStateStore.record(RuleTrigger(rule, fingerprint = null, latchKey = latch, value = 21.0, at = null))
        await { latched(latch) }

        viewModel().update(rule.copy(name = "Afa"))
        await { ruleStore.rules.first().single().name == "Afa" }
        assertTrue(latched(latch))
    }

    @Test
    fun `what re-arms, what does not`() {
        val on = NotificationRule(1, "Caldo", enabled = true, conditions = warm, message = "m")
        val off = on.copy(enabled = false)
        assertTrue(AlertsViewModel.rearms(before = off, after = on))
        assertFalse(AlertsViewModel.rearms(before = on, after = off))
        assertFalse(AlertsViewModel.rearms(before = on, after = on.copy(name = "Afa", message = "x")))
        assertTrue(AlertsViewModel.rearms(before = on, after = on.copy(conditions = listOf(warm[0].copy(threshold = 25.0)))))
        assertTrue(AlertsViewModel.rearms(before = on, after = on.copy(window = RuleWindow.Daylight)))
    }

    // ------------------------------------------------------------------ «last fired»

    @Test
    fun `last fired is read by id, and by name off the rows written before`() = runBlocking {
        cityStore.add(milan)
        val bici = rule("Bici")
        val corsa = rule("Corsa")
        val nuoto = rule("Nuoto")
        // Renamed after it fired: its past is its id's.
        ruleStore.update(corsa.copy(name = "Jogging"))
        val dao = database.weatherHistoryDao()
        val earlier = now.minusSeconds(7_200)
        val later = now.minusSeconds(3_600)
        // A row written before 15.4: names only. «Corsa» there is nobody's now, by design.
        dao.insert(WeatherHistoryEntry(
            cityKey = milan.cacheKey, timestampEpochSeconds = earlier.epochSecond,
            forecastJson = "{}", firedRulesJson = """["Bici","Corsa"]"""
        ))
        // A row of the new form, written under the old name: the id finds it.
        dao.insert(WeatherHistoryEntry(
            cityKey = milan.cacheKey, timestampEpochSeconds = later.epochSecond,
            forecastJson = "{}", firedRulesJson = """["${corsa.id}:Corsa"]"""
        ))

        val content = withTimeout(10_000) {
            viewModel().state.first { it is AlertsUiState.Content } as AlertsUiState.Content
        }
        val fired = content.rules.associate { it.rule.id to it.lastFired }
        assertEquals(earlier, fired[bici.id])
        assertEquals(later, fired[corsa.id])
        assertNull(fired[nuoto.id])
        assertEquals(listOf("Bici", "Jogging", "Nuoto"), content.rules.map { it.rule.name })
    }

    @Test
    fun `two rules never share a past, and the newest firing wins`() = runBlocking {
        cityStore.add(milan)
        val bici = rule("Bici")
        val other = rule("Bici 2")
        val dao = database.weatherHistoryDao()
        val earlier = now.minusSeconds(7_200)
        val later = now.minusSeconds(3_600)
        dao.insert(WeatherHistoryEntry(
            cityKey = milan.cacheKey, timestampEpochSeconds = earlier.epochSecond,
            forecastJson = "{}", firedRulesJson = """["${bici.id}:Bici","${other.id}:Bici 2"]"""
        ))
        dao.insert(WeatherHistoryEntry(
            cityKey = milan.cacheKey, timestampEpochSeconds = later.epochSecond,
            forecastJson = "{}", firedRulesJson = """["${bici.id}:Bici"]"""
        ))

        val content = withTimeout(10_000) {
            viewModel().state.first { it is AlertsUiState.Content } as AlertsUiState.Content
        }
        val fired = content.rules.associate { it.rule.id to it.lastFired }
        assertEquals(later, fired[bici.id])
        assertEquals(earlier, fired[other.id])
    }

    // ------------------------------------------------------------------ the place (15.11)

    /** A rule bound to another saved place is not read on the active place's forecast: its
     * answer is that place's (15.12 reads it). One whose place is gone is marked for its card
     * from the moment the place is, before the job has switched it off. */
    @Test
    fun `a rule bound elsewhere is not read here, and an orphan is marked`() = runBlocking {
        cityStore.add(turin)
        cityStore.add(milan) // the active place
        val here = rule("Qui")
        val there = rule("Torino").copy(place = RulePlace(turin.id, turin.name)).also { ruleStore.update(it) }
        val gone = rule("Bormio").copy(place = RulePlace(3181554, "Bormio")).also { ruleStore.update(it) }
        await { ruleStore.rules.first().count { it.place != null } == 2 }

        val content = withTimeout(10_000) {
            viewModel(Recorded, at = Recorded.fetchedAt.plusSeconds(300)).state.first {
                it is AlertsUiState.Content && it.rules.any { card -> card.now != null }
            } as AlertsUiState.Content
        }
        val cards = content.rules.associateBy { it.rule.id }
        assertNotNull(cards.getValue(here.id).now)
        assertNull(cards.getValue(there.id).now)
        assertNull(cards.getValue(gone.id).now)
        assertEquals(listOf(false, false, true), listOf(here, there, gone).map { cards.getValue(it.id).placeGone })
    }
}
