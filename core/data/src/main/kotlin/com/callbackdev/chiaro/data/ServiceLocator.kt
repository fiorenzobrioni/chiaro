package com.callbackdev.chiaro.data

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.room.Room
import com.callbackdev.chiaro.core.data.BuildConfig
import com.callbackdev.chiaro.data.local.ReportDiskCache
import com.callbackdev.chiaro.data.local.ChiaroDatabase
import com.callbackdev.chiaro.data.local.StoredDataSweep
import java.io.File
import java.time.Duration
import com.callbackdev.chiaro.data.remote.OpenMeteoAirQualityApi
import com.callbackdev.chiaro.data.remote.OpenMeteoForecastApi
import com.callbackdev.chiaro.data.remote.OpenMeteoWitnessApi
import com.callbackdev.chiaro.data.remote.OpenMeteoGeocodingApi
import com.callbackdev.chiaro.data.warnings.DpcBulletinSource
import com.callbackdev.chiaro.data.warnings.OfficialWarningReader
import com.callbackdev.chiaro.data.warnings.OfficialWarningStore
import com.callbackdev.chiaro.data.warnings.WarningSource
import com.callbackdev.chiaro.data.warnings.WarningZoneAssets
import com.callbackdev.chiaro.domain.model.City
import com.callbackdev.chiaro.domain.model.notificationKey
import com.callbackdev.chiaro.domain.warnings.WarningZoneIndex
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Hand-rolled DI: the app is small enough that a lazy singleton graph beats a Hilt
 * setup (decision recorded in PLANNING.md Fase 3). ViewModels grab the repository
 * via [weatherRepository] from Fase 4 on.
 */
object ServiceLocator {

    private val json = Json { ignoreUnknownKeys = true }

    @Volatile
    private var repository: WeatherRepository? = null

    @Volatile
    private var cityStore: CityStore? = null

    @Volatile
    private var searchHistoryStore: SearchHistoryStore? = null

    @Volatile
    private var settingsStore: SettingsStore? = null

    @Volatile
    private var locationProvider: LocationProvider? = null

    @Volatile
    private var gpsRefix: GpsRefix? = null

    @Volatile
    private var alertStateStore: AlertStateStore? = null

    @Volatile
    private var widgetCityStore: WidgetCityStore? = null

    @Volatile
    private var workspaceStore: WorkspaceStore? = null

    @Volatile
    private var ruleStore: RuleStore? = null

    @Volatile
    private var ruleStateStore: RuleStateStore? = null

    @Volatile
    private var skySubscriptionStore: SkySubscriptionStore? = null

    @Volatile
    private var skyAlertStateStore: SkyAlertStateStore? = null

    @Volatile
    private var warningStore: OfficialWarningStore? = null

    @Volatile
    private var warningZoneIndex: WarningZoneIndex? = null

    @Volatile
    private var warningSource: WarningSource? = null

    @Volatile
    private var warningReader: OfficialWarningReader? = null

    // The two things more than one graph node needs (Fase 11 hoisted them out of
    // build(): the warnings source shares the OkHttp client for its User-Agent, the
    // warnings DAO shares the database).
    @Volatile
    private var okHttp: OkHttpClient? = null

    @Volatile
    private var database: ChiaroDatabase? = null

    fun warningStore(context: Context): OfficialWarningStore =
        warningStore ?: synchronized(this) {
            warningStore ?: OfficialWarningStore.create(context.applicationContext, json)
                .also { warningStore = it }
        }

    /** Decodes ~290 KB of JSON the first time: call it off the main thread. */
    fun warningZoneIndex(context: Context): WarningZoneIndex =
        warningZoneIndex ?: synchronized(this) {
            warningZoneIndex ?: WarningZoneAssets.load(context.applicationContext)
                .also { warningZoneIndex = it }
        }

    fun warningSource(context: Context): WarningSource =
        warningSource ?: synchronized(this) {
            warningSource ?: DpcBulletinSource(okHttp()).also { warningSource = it }
        }

    /**
     * The screens' side of the warnings (Fase 11, third step). The index is passed as a
     * supplier, not resolved here: a reader whose places are all abroad opens Today and
     * Avvisi without ever decoding the asset.
     */
    fun warningReader(context: Context): OfficialWarningReader =
        warningReader ?: synchronized(this) {
            warningReader ?: OfficialWarningReader(
                store = warningStore(context),
                index = { warningZoneIndex(context) }
            ).also { warningReader = it }
        }

    fun weatherRepository(context: Context): WeatherRepository =
        repository ?: synchronized(this) {
            repository ?: build(context.applicationContext).also { repository = it }
        }

    fun cityStore(context: Context): CityStore =
        cityStore ?: synchronized(this) {
            cityStore ?: CityStore.create(context.applicationContext, json)
                .also { cityStore = it }
        }

    fun searchHistoryStore(context: Context): SearchHistoryStore =
        searchHistoryStore ?: synchronized(this) {
            searchHistoryStore ?: SearchHistoryStore.create(context.applicationContext, json)
                .also { searchHistoryStore = it }
        }

    fun settingsStore(context: Context): SettingsStore =
        settingsStore ?: synchronized(this) {
            settingsStore ?: SettingsStore.create(context.applicationContext)
                .also { settingsStore = it }
        }

    /**
     * Wrapped in [CachedLocationProvider] since Fase 3b: the throttle has to be
     * shared by every caller, and this singleton is the one place both ViewModels
     * already meet.
     */
    fun locationProvider(context: Context): LocationProvider =
        locationProvider ?: synchronized(this) {
            locationProvider
                ?: CachedLocationProvider(AndroidLocationProvider(context.applicationContext))
                    .also { locationProvider = it }
        }

    /**
     * One for the app (30 ott 2026): the tap on the position row in Places and the pager
     * settling on that page are two callers for one gesture, and [GpsRefix]'s lock only
     * makes the second wait for the first if both hold the same object.
     */
    fun gpsRefix(context: Context): GpsRefix =
        gpsRefix ?: synchronized(this) {
            gpsRefix ?: GpsRefix(
                cityStore(context),
                locationProvider(context),
                GpsArrival(cityStore(context), weatherRepository(context), locationProvider(context))
            ).also { gpsRefix = it }
        }

    fun alertStateStore(context: Context): AlertStateStore =
        alertStateStore ?: synchronized(this) {
            alertStateStore ?: AlertStateStore.create(context.applicationContext)
                .also { alertStateStore = it }
        }

    fun widgetCityStore(context: Context): WidgetCityStore =
        widgetCityStore ?: synchronized(this) {
            widgetCityStore ?: WidgetCityStore.create(context.applicationContext)
                .also { widgetCityStore = it }
        }

    fun workspaceStore(context: Context): WorkspaceStore =
        workspaceStore ?: synchronized(this) {
            workspaceStore ?: WorkspaceStore.create(context.applicationContext)
                .also { workspaceStore = it }
        }

    fun ruleStore(context: Context): RuleStore =
        ruleStore ?: synchronized(this) {
            ruleStore ?: RuleStore.create(context.applicationContext, json)
                .also { ruleStore = it }
        }

    fun skySubscriptionStore(context: Context): SkySubscriptionStore =
        skySubscriptionStore ?: synchronized(this) {
            skySubscriptionStore ?: SkySubscriptionStore.create(context.applicationContext, json)
                .also { skySubscriptionStore = it }
        }

    fun skyAlertStateStore(context: Context): SkyAlertStateStore =
        skyAlertStateStore ?: synchronized(this) {
            skyAlertStateStore ?: SkyAlertStateStore.create(context.applicationContext)
                .also { skyAlertStateStore = it }
        }

    fun ruleStateStore(context: Context): RuleStateStore =
        ruleStateStore ?: synchronized(this) {
            ruleStateStore ?: RuleStateStore.create(context.applicationContext)
                .also { ruleStateStore = it }
        }

    /**
     * Test-only: workers resolve dependencies from here, so worker tests swap in
     * temp-file stores/fake repositories. Calling with no arguments resets to
     * lazy real instances (do it in @After — the object outlives the test).
     */
    @VisibleForTesting
    fun overrideForTests(
        repository: WeatherRepository? = null,
        cityStore: CityStore? = null,
        settingsStore: SettingsStore? = null,
        alertStateStore: AlertStateStore? = null,
        widgetCityStore: WidgetCityStore? = null,
        ruleStore: RuleStore? = null,
        ruleStateStore: RuleStateStore? = null,
        skySubscriptionStore: SkySubscriptionStore? = null,
        skyAlertStateStore: SkyAlertStateStore? = null,
        warningStore: OfficialWarningStore? = null,
        warningZoneIndex: WarningZoneIndex? = null,
        warningSource: WarningSource? = null
    ) {
        this.repository = repository
        this.cityStore = cityStore
        this.settingsStore = settingsStore
        this.alertStateStore = alertStateStore
        this.widgetCityStore = widgetCityStore
        this.ruleStore = ruleStore
        this.ruleStateStore = ruleStateStore
        this.skySubscriptionStore = skySubscriptionStore
        this.skyAlertStateStore = skyAlertStateStore
        this.warningStore = warningStore
        this.warningReader = null
        this.warningZoneIndex = warningZoneIndex
        this.warningSource = warningSource
        // Built over the stores above: one left from a previous test holds its stores.
        this.gpsRefix = null
    }

    /**
     * Sent on every API call. Open-Meteo doesn't require it, but rate-limits per IP
     * and reserves the right to block anonymous misbehaving traffic without notice:
     * a named agent with a contact URL turns "block" into "reach out".
     *
     * Handed in by [install] rather than read off BuildConfig: this module is a
     * library and does not have the app's version, which is exactly the point of it
     * being a library.
     */
    @Volatile
    private var userAgent = "chiaro (+https://github.com/fiorenzobrioni/chiaro)"

    @Volatile
    private var historyListener: suspend () -> Unit = {}

    /**
     * Called once from `Application.onCreate`, before anything resolves a
     * dependency. Two things the data layer cannot know about itself: who it says it
     * is upstream, and who wants to hear that new data landed (the widget, in
     * practice). Calling it later still works and only affects the graph built after
     * it, which is why it is called first.
     */
    fun install(userAgent: String, onHistoryCommitted: suspend () -> Unit = {}) {
        this.userAgent = userAgent
        this.historyListener = onHistoryCommitted
    }

    /**
     * The one HTTP client, shared by the four hosts the app talks to (three of
     * Open-Meteo's, GitHub's for the official warnings): the User-Agent interceptor is
     * what makes it one — on `raw.githubusercontent.com` it is the only host where a
     * named agent matters (Fase 11).
     */
    private fun okHttp(): OkHttpClient =
        okHttp ?: synchronized(this) {
            okHttp ?: OkHttpClient.Builder()
                // Three clocks, and two of them add up to the third. OkHttp's defaults
                // bound each PHASE — 10 s to connect, 10 s between bytes — and nothing
                // bounds the call: a connection that dribbles a byte before every read
                // timeout never trips one, which on a screen the reader just opened is a
                // refresh that never resolves and in `WeatherSyncWorker` a worker held
                // open on somebody's battery. The call budget of thirty seconds is there
                // for that, and every caller falls back to the cached report under it.
                //
                // The read timeout is twenty seconds, not the default ten (19 ott 2026).
                // Open-Meteo sends nothing until it has computed the answer, so this
                // clock is the time the service gets to think — and at the peak of every
                // full hour, measured that morning on the app's own request from Italy,
                // the first byte took 8.6 to 11.7 s (`generationtime_ms` 7–8 s, against
                // 5 ms outside it) for three minutes, with every request a 200. Under ten
                // seconds half of them died as a timeout, the one retry fell inside the
                // same three minutes, and the reader read «il servizio non risponde»
                // about a service that was answering. Twenty is well past the worst
                // first byte measured; ten to connect plus twenty to think is exactly
                // the thirty the call may take; and a timeout is no longer retried
                // (`WeatherRepository`), so the worst case of a silent service went from
                // 10 + 2 + 10 seconds of radio to 20.
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(20))
                .callTimeout(Duration.ofSeconds(30))
                .addInterceptor { chain ->
                    chain.proceed(
                        chain.request().newBuilder()
                            .header("User-Agent", userAgent)
                            .build()
                    )
                }
                .apply {
                    if (BuildConfig.DEBUG) {
                        addInterceptor(
                            HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC)
                        )
                    }
                }
                .build()
                .also { okHttp = it }
        }

    private fun database(appContext: Context): ChiaroDatabase =
        database ?: synchronized(this) {
            database ?: Room.databaseBuilder(
                appContext,
                ChiaroDatabase::class.java,
                "chiaro.db"
            )
                .addMigrations(*ChiaroDatabase.MIGRATIONS)
                .build()
                .also { database = it }
        }

    private fun build(appContext: Context): WeatherRepository {
        val okHttp = okHttp()

        fun retrofit(baseUrl: String): Retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttp)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

        val database = database(appContext)
        // Survives process death so cold starts inside the TTL cost zero GETs
        val diskCache = ReportDiskCache(File(appContext.filesDir, "report_cache"), json)
        forgetRetiredFiles(appContext)
        val sweep = StoredDataSweep(historyDao = database.weatherHistoryDao(), diskCache = diskCache)

        return WeatherRepository(
            forecastApi = retrofit(OpenMeteoForecastApi.BASE_URL)
                .create(OpenMeteoForecastApi::class.java),
            witnessApi = retrofit(OpenMeteoForecastApi.BASE_URL)
                .create(OpenMeteoWitnessApi::class.java),
            airQualityApi = retrofit(OpenMeteoAirQualityApi.BASE_URL)
                .create(OpenMeteoAirQualityApi::class.java),
            geocodingApi = retrofit(OpenMeteoGeocodingApi.BASE_URL)
                .create(OpenMeteoGeocodingApi::class.java),
            historyDao = database.weatherHistoryDao(),
            diskCache = diskCache,
            json = json,
            // Every fetch that commits new data notifies whoever installed us — the
            // widget repaint, in the app. The data layer does not know that.
            onHistoryCommitted = { historyListener() },
            // ...and the same commit is where the storage tidies up after itself. The
            // live set is read here and not inside the sweep because the sweep is in
            // `local/` and has no business knowing about the saved-places store.
            liveKeys = { liveCityKeys(appContext) },
            onHousekeeping = { live ->
                sweep.run(live)
                // The rules' latches are filed by place and by rule: a place removed or
                // a rule deleted leaves latches nothing would ever re-arm.
                ruleStateStore(appContext).retain(
                    liveCities = liveNotificationKeys(appContext),
                    liveRules = ruleStore(appContext).rules.first().map { it.id }.toSet()
                )
            }
        )
    }

    /**
     * Files only the Journal wrote, left behind by builds before 14 ott 2026: the log of
     * failed updates. Deleting a file that is not there is free, so this runs once per
     * process with no flag to remember it by. (The Journal's table goes in the Room
     * migration to v6.)
     */
    private fun forgetRetiredFiles(appContext: Context) {
        runCatching { File(appContext.filesDir, "datastore/fetch_log.preferences_pb").delete() }
    }

    /**
     * Every `City.cacheKey` the app still has a use for: the saved places and, when
     * there is one, the current GPS fix. Widget pins resolve city ids against that
     * same saved list (or the GPS sentinel), so they add nothing here — and the GPS
     * cells the reader drove through yesterday are deliberately NOT in it, which is
     * the whole point of [StoredDataSweep].
     */
    private suspend fun liveCityKeys(appContext: Context): Set<String> =
        livePlaces(appContext).map { it.cacheKey }.toSet()

    /** The same places, by the key the notification engines file them under. */
    private suspend fun liveNotificationKeys(appContext: Context): Set<String> =
        livePlaces(appContext).map { it.notificationKey }.toSet()

    private suspend fun livePlaces(appContext: Context): List<City> {
        val store = cityStore(appContext)
        return store.cities.first() + listOfNotNull(store.locationSettings.first().gpsCity)
    }
}
