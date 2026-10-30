package com.callbackdev.chiaro.ui.places

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.callbackdev.chiaro.data.ActiveSource
import com.callbackdev.chiaro.data.CityStore
import com.callbackdev.chiaro.data.LocationProvider
import com.callbackdev.chiaro.data.SearchHistoryStore
import com.callbackdev.chiaro.data.WeatherRepository
import com.callbackdev.chiaro.data.local.ChiaroDatabase
import com.callbackdev.chiaro.data.remote.OpenMeteoAirQualityApi
import com.callbackdev.chiaro.data.remote.OpenMeteoForecastApi
import com.callbackdev.chiaro.data.remote.OpenMeteoGeocodingApi
import com.callbackdev.chiaro.data.remote.dto.AirQualityResponseDto
import com.callbackdev.chiaro.data.remote.dto.ForecastResponseDto
import com.callbackdev.chiaro.data.remote.dto.GeocodingResponseDto
import com.callbackdev.chiaro.domain.WeatherException
import com.callbackdev.chiaro.domain.model.City
import com.callbackdev.chiaro.domain.model.Coordinates
import com.callbackdev.chiaro.domain.model.GeoFix
import java.io.IOException
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
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
import org.robolectric.annotation.Config

/**
 * The tap on the position row (committente, 30 ott 2026): with a place found it does what
 * the swipe onto its page does, the sheet closes at once and the position is re-taken
 * behind it, silently and only when due; the permission or the location switch off keeps
 * the sheet open with the reason; with no place found yet the tap looks out loud. Real
 * stores on temp files; the network is a stub that fails, because no case here needs a
 * forecast, and the position is a fake that says which age it was asked for.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class PlacesViewModelTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private fun prefs(name: String) = PreferenceDataStoreFactory.create(scope = scope) { tmp.newFile(name) }

    private val cityStore = CityStore(prefs("cities.preferences_pb"), Json)
    private val searchHistory = SearchHistoryStore(prefs("search.preferences_pb"), Json)
    private lateinit var database: ChiaroDatabase

    private val now: Instant = Instant.parse("2026-10-30T08:00:00Z")
    private val cavenago = GeoFix(Coordinates(45.58, 9.41), "Cavenago di Brianza", "Lombardia", "Italia")
    private val turin = City(
        id = 3165524, name = "Torino", region = "Piemonte", country = "Italia",
        coordinates = Coordinates(45.0703, 7.6869), timezone = "Europe/Rome"
    )

    private class FakeProvider(private val answer: GeoFix) : LocationProvider {
        @Volatile
        var calls = 0
        val maxAges = mutableListOf<Duration>()
        var obstacle: WeatherException? = null
        var gate: CompletableDeferred<Unit>? = null

        override fun obstacle(): WeatherException? = obstacle

        override suspend fun currentFix(maxAge: Duration, timeout: Duration): GeoFix {
            calls++
            maxAges += maxAge
            gate?.await()
            return answer
        }
    }

    private val provider = FakeProvider(cavenago)

    private object NoForecast : OpenMeteoForecastApi {
        override suspend fun forecast(
            latitude: Double, longitude: Double, current: String, hourly: String, daily: String,
            timezone: String, forecastDays: Int, minutely15: String, forecastQuarters: Int
        ): ForecastResponseDto = throw IOException("no network in this test")
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
    }

    @After
    fun tearDown() {
        database.close()
        scope.cancel()
        Dispatchers.resetMain()
    }

    private fun viewModel(): PlacesViewModel {
        val clock = Clock.fixed(now, ZoneOffset.UTC)
        val repository = WeatherRepository(
            forecastApi = NoForecast,
            airQualityApi = NoAirQuality,
            geocodingApi = NoGeocoding,
            historyDao = database.weatherHistoryDao(),
            clock = clock
        )
        return PlacesViewModel(repository, cityStore, searchHistory, provider, clock)
    }

    /** Completes when the view model tells the sheet to close. Subscribed before the tap:
     * the cue is not replayed to a sheet that starts listening late. */
    private fun closing(viewModel: PlacesViewModel): CompletableDeferred<Unit> {
        val closed = CompletableDeferred<Unit>()
        scope.launch(Dispatchers.Unconfined) {
            viewModel.gpsSelected.first()
            closed.complete(Unit)
        }
        return closed
    }

    /** The position on and found at Cavenago [ago] before now; Turin saved and on screen. */
    private suspend fun onTurinWithPositionFound(ago: Duration) {
        cityStore.adoptGpsFix(cavenago, now.minus(ago))
        cityStore.setUseGps(true)
        cityStore.add(turin)
    }

    /** The stores write on their own threads: the test waits for what it asked to land. */
    private suspend fun await(condition: suspend () -> Boolean) {
        withTimeout(10_000) {
            while (!condition()) delay(20)
        }
    }

    @Test
    fun `with a place found, the tap closes the sheet at once and asks for no position`() = runBlocking {
        onTurinWithPositionFound(Duration.ofMinutes(2))
        val viewModel = viewModel()
        val closed = closing(viewModel)

        viewModel.selectGps()

        withTimeout(10_000) { closed.await() }
        assertTrue(cityStore.activeSource.first() is ActiveSource.Gps)
        // Two minutes old: the swipe would have asked for nothing, and so does the tap.
        assertEquals(0, provider.calls)
        assertEquals(GpsState.Idle, viewModel.gpsState.value)
    }

    @Test
    fun `with the fix due, the sheet closes first and the position is taken behind it`() = runBlocking {
        onTurinWithPositionFound(Duration.ofHours(1))
        val gate = CompletableDeferred<Unit>()
        provider.gate = gate
        val viewModel = viewModel()
        val closed = closing(viewModel)

        viewModel.selectGps()

        // Closed while the position is still being looked for: nobody waits on it.
        withTimeout(10_000) { closed.await() }
        await { provider.calls == 1 }
        assertEquals(GpsState.Idle, viewModel.gpsState.value)
        gate.complete(Unit)
        await { cityStore.locationSettings.first().fixedAt == now }
        // The swipe's road: what the phone knows from the last five minutes will do.
        assertEquals(listOf(LocationProvider.SilentMaxAge), provider.maxAges)
    }

    @Test
    fun `a revoked permission keeps the sheet open and says why`() = runBlocking {
        onTurinWithPositionFound(Duration.ofHours(1))
        provider.obstacle = WeatherException.LocationPermissionDenied()
        val viewModel = viewModel()
        val closed = closing(viewModel)

        viewModel.selectGps()

        await { viewModel.gpsState.value is GpsState.Error }
        assertEquals(GpsState.Error(GpsError.PERMISSION), viewModel.gpsState.value)
        assertFalse(closed.isCompleted)
        assertEquals(0, provider.calls)
        // Selected all the same: the last place found, with its real weather.
        assertTrue(cityStore.activeSource.first() is ActiveSource.Gps)
    }

    @Test
    fun `the phone's location switched off keeps the sheet open and says so`() = runBlocking {
        onTurinWithPositionFound(Duration.ofMinutes(2))
        provider.obstacle = WeatherException.LocationDisabled()
        val viewModel = viewModel()
        val closed = closing(viewModel)

        viewModel.selectGps()

        await { viewModel.gpsState.value is GpsState.Error }
        assertEquals(GpsState.Error(GpsError.DISABLED), viewModel.gpsState.value)
        assertFalse(closed.isCompleted)
    }

    @Test
    fun `with no place found yet, the tap looks out loud and closes when it answers`() = runBlocking {
        cityStore.setUseGps(true) // on, and nothing found yet
        val gate = CompletableDeferred<Unit>()
        provider.gate = gate
        val viewModel = viewModel()
        val closed = closing(viewModel)

        viewModel.selectGps()

        await { viewModel.gpsState.value == GpsState.Acquiring }
        assertFalse(closed.isCompleted)
        gate.complete(Unit)
        withTimeout(10_000) { closed.await() }
        assertEquals(listOf(LocationProvider.Now), provider.maxAges)
        assertEquals(cavenago.coordinates, cityStore.locationSettings.first().gpsCity?.coordinates)
        assertEquals(GpsState.Idle, viewModel.gpsState.value)
    }
}
