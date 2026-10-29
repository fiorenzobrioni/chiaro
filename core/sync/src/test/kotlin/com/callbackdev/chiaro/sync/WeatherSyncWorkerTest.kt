package com.callbackdev.chiaro.sync

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import com.callbackdev.chiaro.data.AlertStateStore
import com.callbackdev.chiaro.data.CityStore
import com.callbackdev.chiaro.data.RuleStateStore
import com.callbackdev.chiaro.data.RuleStore
import com.callbackdev.chiaro.data.ServiceLocator
import com.callbackdev.chiaro.data.SettingsStore
import com.callbackdev.chiaro.data.WeatherRepository
import com.callbackdev.chiaro.data.local.ChiaroDatabase
import com.callbackdev.chiaro.data.remote.OpenMeteoAirQualityApi
import com.callbackdev.chiaro.data.remote.OpenMeteoForecastApi
import com.callbackdev.chiaro.data.remote.OpenMeteoGeocodingApi
import com.callbackdev.chiaro.data.remote.dto.AirQualityResponseDto
import com.callbackdev.chiaro.data.remote.dto.ForecastResponseDto
import com.callbackdev.chiaro.domain.Alert
import com.callbackdev.chiaro.domain.model.City
import com.callbackdev.chiaro.domain.model.Coordinates
import com.callbackdev.chiaro.domain.model.WeatherReport
import com.callbackdev.chiaro.domain.model.notificationKey
import com.callbackdev.chiaro.domain.rules.RuleCondition
import com.callbackdev.chiaro.domain.rules.RuleEngine
import com.callbackdev.chiaro.domain.rules.RuleOp
import com.callbackdev.chiaro.domain.rules.RulePlace
import com.callbackdev.chiaro.domain.rules.RuleTrigger
import com.callbackdev.chiaro.domain.settings.UnitSettings
import com.callbackdev.chiaro.domain.warnings.PlaceWarnings
import java.io.IOException
import java.time.LocalDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * The periodic job against an unreachable provider: what it does when there is
 * nothing to sync for, what keeps it alive, and what a failed fetch leaves behind.
 * Ported from tweather's `WeatherSyncWorkerTest` on 9 set 2026 (`UPSTREAM.md`); the
 * widget and the notifiers arrive through [SyncDependencies] here, so they are fakes
 * rather than a placed widget, and the last test is this app's own — the widget repaint
 * that lets the stale marker appear. Since 15.11 (29 Oct 2026) the job also reads the rules
 * bound to other saved places, on those places' recorded forecasts.
 */
@RunWith(RobolectricTestRunner::class)
class WeatherSyncWorkerTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val json = Json { ignoreUnknownKeys = true }
    private lateinit var settingsStore: SettingsStore
    private lateinit var ruleStore: RuleStore
    private lateinit var cityStore: CityStore
    private lateinit var database: ChiaroDatabase
    private lateinit var retrofit: Retrofit
    private val notifiers = FakeNotifiers()
    private val widgets = FakeWidgets()

    /** A provider that answers every forecast with the status [failure] builds — built
     * per call, since an error body is read once (19 ott 2026). */
    private class StatusApi(private val failure: () -> Throwable) : OpenMeteoForecastApi {
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
        ): ForecastResponseDto = throw failure()
    }

    private fun status(code: Int, reason: String) = HttpException(
        Response.error<Any>(
            code,
            """{"error":true,"reason":"$reason"}""".toResponseBody("application/json".toMediaType())
        )
    )

    /** Recorded answers by hemisphere: Reykjavík's north of the equator, Sydney's south of it
     * ([fails] answers a place's own failure instead). Counts what each place cost. */
    private inner class RecordedApi(private val fails: Map<String, () -> Throwable> = emptyMap()) :
        OpenMeteoForecastApi {
        val calls = mutableMapOf<String, Int>()
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
            val place = if (latitude > 0) "reykjavik" else "sydney"
            calls[place] = (calls[place] ?: 0) + 1
            fails[place]?.let { throw it() }
            return json.decodeFromString(
                ForecastResponseDto.serializer(),
                checkNotNull(javaClass.getResource("/openmeteo/$place-2026-09-24.json")).readText()
            )
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

    // Two places no official bulletin grades, so the warnings' step stays inert.
    private val reykjavik = City(
        3413829, "Reykjavík", "Capital Region", "Iceland", Coordinates(64.1355, -21.8954), "Atlantic/Reykjavik"
    )
    private val sydney = City(
        2147714, "Sydney", "New South Wales", "Australia", Coordinates(-33.8679, 151.2073), "Australia/Sydney"
    )

    /** Holds right now anywhere on Earth: an instant rule, so it fires once and latches. */
    private val anyTemperature = listOf(RuleCondition("current.temp_c", RuleOp.GT, -80.0))

    private suspend fun rule(name: String, place: City? = null) =
        checkNotNull(ruleStore.add(name, anyTemperature, "m")).let { created ->
            created.copy(place = place?.let { RulePlace(it.id, it.name) }).also { ruleStore.update(it) }
        }

    /** The saved list is [places], the last one active. */
    private suspend fun savedPlaces(vararg places: City) {
        cityStore.remove(CityStore.DefaultCity)
        places.forEach { cityStore.add(it) }
    }

    private class FakeNotifiers : SyncNotifiers {
        /** Each rule that spoke, with the place its notification named. */
        val rules = mutableListOf<Pair<String, String>>()
        override fun notificationsEnabled() = true
        override fun notifyAlert(alert: Alert, report: WeatherReport, units: UnitSettings, warnings: PlaceWarnings?) = true
        override fun notifyRule(
            trigger: RuleTrigger,
            cityLabel: String,
            report: WeatherReport,
            now: LocalDateTime,
            units: UnitSettings
        ): Boolean {
            rules += trigger.rule.name to cityLabel
            return true
        }
        override suspend fun rearmSkyReminders() = Unit
        override fun notifyOfficialWarning(
            notification: com.callbackdev.chiaro.domain.warnings.WarningNotification,
            city: City,
            today: java.time.LocalDate
        ) = true
    }

    private class FakeWidgets : SyncWidgets {
        var placed = false
        var repaints = 0
        var pinned = emptyList<City>()
        override fun hasWidgets() = placed
        override suspend fun repaintAll() { repaints++ }
        override suspend fun pinnedCities(): List<City> = pinned
    }

    private fun <T> store(name: String, build: (androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>) -> T): T =
        build(
            PreferenceDataStoreFactory.create(scope = scope) {
                tmp.newFile("$name-${System.nanoTime()}.preferences_pb")
            }
        )

    @Before
    fun setUp() {
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
        SyncDependencies.install(notifiers, widgets)
        database = Room.inMemoryDatabaseBuilder(context, ChiaroDatabase::class.java)
            .allowMainThreadQueries().build()
        retrofit = Retrofit.Builder()
            .baseUrl("http://127.0.0.1:1/") // unreachable: getWeather → Unreachable
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        cityStore = store("cities") { CityStore(it, json) }
        // No seeded place: without one the worker has nothing to fetch.
        runBlocking { cityStore.add(CityStore.DefaultCity) }
        settingsStore = store("settings") { SettingsStore(it) }
        ruleStore = store("rules") { RuleStore(it, json) }
        install(retrofit.create(OpenMeteoForecastApi::class.java))
    }

    /** The graph under test with [forecastApi] as the provider: the refused port by
     * default, a scripted status where a test needs the service's own answer. */
    private fun install(
        forecastApi: OpenMeteoForecastApi,
        airQualityApi: OpenMeteoAirQualityApi = retrofit.create(OpenMeteoAirQualityApi::class.java)
    ) {
        ServiceLocator.overrideForTests(
            repository = WeatherRepository(
                forecastApi = forecastApi,
                airQualityApi = airQualityApi,
                geocodingApi = retrofit.create(OpenMeteoGeocodingApi::class.java),
                historyDao = database.weatherHistoryDao(),
                json = json,
                // The refused connection gets its one more try; no need to wait for it.
                transientRetryDelay = java.time.Duration.ZERO,
                transientRetryJitter = java.time.Duration.ZERO
            ),
            cityStore = cityStore,
            settingsStore = settingsStore,
            alertStateStore = store("alerts") { AlertStateStore(it) },
            ruleStore = ruleStore,
            ruleStateStore = store("rule-state") { RuleStateStore(it) }
        )
    }

    @After
    fun tearDown() {
        ServiceLocator.overrideForTests() // back to lazy real instances
        database.close()
        scope.cancel()
    }

    private fun runWorker(): ListenableWorker.Result = runBlocking {
        TestListenableWorkerBuilder<WeatherSyncWorker>(context).build().doWork()
    }

    /** Every one of them by name, which is what the helper claims to be: the two
     * summaries went ON by default on 21 set 2026 and the evening one, left out here,
     * quietly kept the job alive. */
    private fun allTogglesOff() = runBlocking {
        settingsStore.setSevereWeatherAlerts(false)
        settingsStore.setDailySummary(false)
        settingsStore.setEveningSummary(false)
        settingsStore.setPrecipitationWarning(false)
        settingsStore.setOfficialWarnings(false)
    }

    private fun periodicStates(): List<WorkInfo.State> = WorkManager.getInstance(context)
        .getWorkInfosForUniqueWork(SyncScheduler.UNIQUE_NAME).get().map { it.state }

    @Test
    fun `all toggles off - worker succeeds and cancels its own periodic work`() = runBlocking {
        allTogglesOff()
        // no widget placed either, so the self-heal has nothing left to sync for
        SyncScheduler.reconcile(context) // toggles off → this is already a cancel
        assertEquals(ListenableWorker.Result.success(), runWorker())
        assertEquals(emptyList<WorkInfo.State>(), periodicStates().filter { !it.isFinished })
    }

    @Test
    fun `all toggles off but a widget is placed - the job survives and still fetches`() =
        runBlocking {
            allTogglesOff()
            widgets.placed = true
            SyncScheduler.reconcile(context) // widget alone → enqueue, not cancel

            // retry means it walked past the self-heal branch and reached the fetch:
            // the widget must keep being fed even with every notification off
            assertEquals(ListenableWorker.Result.retry(), runWorker())

            val states = periodicStates()
            assertTrue("periodic work should still be alive, was $states", states.contains(WorkInfo.State.ENQUEUED))
            assertFalse("worker cancelled itself with a widget placed", states.contains(WorkInfo.State.CANCELLED))
        }

    @Test
    fun `all toggles off but a reader's alert exists - the job survives and still fetches`() =
        runBlocking {
            allTogglesOff()
            ruleStore.add() // one enabled rule
            SyncScheduler.reconcile(context) // rules alone → enqueue, not cancel

            // retry = it walked past the self-heal branch and reached the fetch
            assertEquals(ListenableWorker.Result.retry(), runWorker())
            val states = periodicStates()
            assertTrue("periodic work should still be alive, was $states", states.contains(WorkInfo.State.ENQUEUED))
        }

    @Test
    fun `network failure - worker asks for a retry`() {
        // defaults: severe+precip on, notifications enabled (the fake says so)
        assertEquals(ListenableWorker.Result.retry(), runWorker())
    }

    /** The service's own passing states (19 ott 2026): a 503 at the peak of the full hour
     * and a 429 on a shared address are retried on the backoff like a dropped
     * connection, instead of waiting out the whole period. */
    @Test
    fun `the service shedding load - worker asks for a retry, and the widgets repaint`() {
        install(StatusApi { status(503, "The service is overloaded") })
        widgets.placed = true
        assertEquals(ListenableWorker.Result.retry(), runWorker())
        assertEquals(1, widgets.repaints)
    }

    @Test
    fun `a limit on the address - worker asks for a retry`() {
        install(StatusApi { status(429, "Minutely API request limit exceeded. Please try again in one minute.") })
        assertEquals(ListenableWorker.Result.retry(), runWorker())
    }

    @Test
    fun `the request's own fault - worker waits for the next period`() {
        install(StatusApi { status(400, "Cannot initialize Float from invalid String value") })
        widgets.placed = true
        assertEquals(ListenableWorker.Result.success(), runWorker())
        assertEquals(1, widgets.repaints)
    }

    /** A successful fetch repaints through the repository's commit hook; a FAILED one
     * has no commit, so the worker repaints by hand — that is when the stale marker
     * has to appear (Fase 8). */
    @Test
    fun `a failed fetch repaints the widgets so the stale marker can appear`() {
        widgets.placed = true
        runWorker()
        assertEquals(1, widgets.repaints)
    }

    // ------------------------------------------------------------ rules bound to a place

    /** The heart of 15.11: the active place reads the rules with no place and those bound to
     * it, each other saved place its own, on its own forecast, under its own name and key;
     * and what fired is on that place's history, where Alerts reads «last fired». */
    @Test
    fun `a rule bound to another saved place is read on that place's forecast`() = runBlocking {
        val api = RecordedApi()
        install(api, NoAirQuality)
        savedPlaces(sydney, reykjavik)
        val anywhere = rule("Ovunque")
        val here = rule("Qui", place = reykjavik)
        val there = rule("Laggiù", place = sydney)

        assertEquals(ListenableWorker.Result.success(), runWorker())

        assertEquals(
            listOf("Ovunque" to "Reykjavík", "Qui" to "Reykjavík", "Laggiù" to "Sydney"),
            notifiers.rules
        )
        // One fetch a place: the bound place costs one, the active place no more than before.
        assertEquals(mapOf("reykjavik" to 1, "sydney" to 1), api.calls)
        val latched = ServiceLocator.ruleStateStore(context).state.first().latched
        assertEquals(
            setOf(
                RuleEngine.latchKey(reykjavik.notificationKey, anywhere.id),
                RuleEngine.latchKey(reykjavik.notificationKey, here.id),
                RuleEngine.latchKey(sydney.notificationKey, there.id)
            ),
            latched
        )
        val repository = ServiceLocator.weatherRepository(context)
        val firedThere = repository.firedRules(repository.historyFor(sydney).first())
        assertEquals(listOf(there.id), firedThere.map { it.id })
        val firedHere = repository.firedRules(repository.historyFor(reykjavik).first())
        assertEquals(listOf(anywhere.id, here.id), firedHere.map { it.id })
    }

    /** A place removed: its rule goes off at the first run, keeps its place, and is read on no
     * other place in the meantime; the rest runs as before. */
    @Test
    fun `a rule whose place is gone is switched off, not moved`() = runBlocking {
        val api = RecordedApi()
        install(api, NoAirQuality)
        savedPlaces(reykjavik)
        val anywhere = rule("Ovunque")
        val gone = rule("Laggiù", place = sydney)

        assertEquals(ListenableWorker.Result.success(), runWorker())

        assertEquals(listOf("Ovunque" to "Reykjavík"), notifiers.rules)
        assertEquals(mapOf("reykjavik" to 1), api.calls)
        val rules = ruleStore.rules.first()
        assertEquals(gone.copy(enabled = false), rules.single { it.id == gone.id })
        assertTrue(rules.single { it.id == anywhere.id }.enabled)
    }

    /** The only rule left on watches a place that is gone: switched off, it no longer keeps the
     * job alive, and the job lets itself go in the same run, with nothing fetched. */
    @Test
    fun `an orphan alone does not keep the job alive`() = runBlocking {
        val api = RecordedApi()
        install(api, NoAirQuality)
        allTogglesOff()
        savedPlaces(reykjavik)
        rule("Laggiù", place = sydney)
        SyncScheduler.reconcile(context) // one enabled rule → enqueue

        assertEquals(ListenableWorker.Result.success(), runWorker())

        assertTrue(ruleStore.rules.first().none { it.enabled })
        assertTrue(api.calls.isEmpty())
        assertEquals(emptyList<WorkInfo.State>(), periodicStates().filter { !it.isFinished })
    }

    /** A bound place whose service sheds load: the active place's run stands, and the job asks
     * for the retry the active place would ask for, so the bound rules are not a period late. */
    @Test
    fun `a passing failure at a bound place asks for a retry`() = runBlocking {
        val api = RecordedApi(fails = mapOf("sydney" to { status(503, "The service is overloaded") }))
        install(api, NoAirQuality)
        savedPlaces(sydney, reykjavik)
        rule("Ovunque")
        rule("Laggiù", place = sydney)

        assertEquals(ListenableWorker.Result.retry(), runWorker())
        assertEquals(listOf("Ovunque" to "Reykjavík"), notifiers.rules)
    }

    /** The request's own fault at a bound place waits for the next period, as the active
     * place's does: it would fail the same way again. */
    @Test
    fun `a bound place's own fault waits for the next period`() = runBlocking {
        val api = RecordedApi(fails = mapOf("sydney" to { status(400, "Cannot initialize Float") }))
        install(api, NoAirQuality)
        savedPlaces(sydney, reykjavik)
        rule("Laggiù", place = sydney)

        assertEquals(ListenableWorker.Result.success(), runWorker())
        assertTrue(notifiers.rules.isEmpty())
    }

    /** A widget pinned to another city is fed on a sync that posts nothing (15.11): the gate
     * of the alerts used to return before the pinned cities were fetched. */
    @Test
    fun `a widget-only sync fetches the pinned cities too`() = runBlocking {
        val api = RecordedApi()
        install(api, NoAirQuality)
        allTogglesOff()
        savedPlaces(sydney, reykjavik)
        widgets.placed = true
        widgets.pinned = listOf(sydney)

        assertEquals(ListenableWorker.Result.success(), runWorker())
        assertEquals(mapOf("reykjavik" to 1, "sydney" to 1), api.calls)
        assertTrue(notifiers.rules.isEmpty())
    }
}
