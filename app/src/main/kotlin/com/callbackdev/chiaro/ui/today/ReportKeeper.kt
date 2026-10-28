package com.callbackdev.chiaro.ui.today

import com.callbackdev.chiaro.data.PowerSaveState
import com.callbackdev.chiaro.data.WeatherRepository
import com.callbackdev.chiaro.domain.WeatherException
import com.callbackdev.chiaro.domain.WeatherFreshness
import com.callbackdev.chiaro.domain.model.City
import com.callbackdev.chiaro.domain.model.WeatherReport
import java.time.Clock
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

/**
 * What a page keeps for the place it shows: the raw report as fetched (null when the app has
 * none for the place yet), and the minute it was handed over at — a new [at] every tick, so a
 * screen that reads the clock off it moves with it.
 */
class KeptReport(val city: City, val report: WeatherReport?, val at: Instant)

/**
 * Today's road to a fresh report, for the pages that are not Today (15.1 for Alerts, and the
 * Sky the same day, 28 Oct 2026). One flow per place, alive only while the page is watched:
 *
 * - the cache goes out first, before any network is asked — no full-screen spinner, ever;
 * - then the repository is asked again, silently, at the opening and at every minute once
 *   the report is older than the provider's own quarter of an hour
 *   ([WeatherFreshness.ProviderResolution], which is also the repository's TTL: a younger
 *   report is a cache hit and costs nothing), waiting what [RefreshBackoff] says after a
 *   failure, as Today's tick does;
 * - never under battery saver unless there is no report at all (Today's own exceptions: a
 *   request nobody made out loud is postponed, an empty page is not a cheaper one);
 * - a fetch that fails is silent: the page keeps what it has and says its age, and the next
 *   tick asks again when the backoff allows.
 *
 * No request of its own: it is the same cache Today fills, so a reader coming from Today
 * finds it fresh, and the fetch made here is the one Today would have made at the same
 * minute. Until it existed, Alerts and the Sky read a cache as old as the last visit to Today
 * or the last run of the job — up to two hours, with the Sky's verdicts all «?» for the age
 * when one fetch would have let them speak.
 *
 * [cached] and [fetch] are functions and not the repository so that a test can hand in a
 * recorder and a clock ([ReportKeeperTest]); the secondary constructor binds the repository.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReportKeeper(
    private val cached: suspend (City) -> WeatherReport?,
    /** Throws [WeatherException] when the service cannot answer. */
    private val fetch: suspend (City) -> WeatherReport,
    private val clock: Clock = Clock.systemUTC(),
    private val powerSave: PowerSaveState = PowerSaveState.Off,
    /** The tick: a minute, as every page's. */
    private val tick: Duration = Duration.ofMinutes(1)
) {

    constructor(
        repository: WeatherRepository,
        clock: Clock = Clock.systemUTC(),
        powerSave: PowerSaveState = PowerSaveState.Off
    ) : this(
        cached = { repository.cachedReport(it) },
        fetch = { repository.getWeather(it) },
        clock = clock,
        powerSave = powerSave
    )

    /** [keep] of whichever place [cities] names, null between places and with none. */
    fun follow(cities: Flow<City?>): Flow<KeptReport?> = cities
        .distinctUntilChanged { a, b -> a?.cacheKey == b?.cacheKey }
        .flatMapLatest { city -> city?.let(::keep) ?: flowOf(null) }

    /** The report of [city], kept current while collected: the cache at once, then every tick. */
    fun keep(city: City): Flow<KeptReport> = flow {
        var report = runCatching { cached(city) }.getOrNull()
        val backoff = RefreshBackoff()

        /** True when a fresh report landed. */
        suspend fun refresh(): Boolean {
            if (report != null && powerSave.isOn()) return false
            val now = clock.instant()
            val age = report?.let { Duration.between(it.systemInfo.lastSync, now) }
            if (age != null && age < WeatherFreshness.ProviderResolution) return false
            if (!backoff.isDue(now)) return false
            backoff.attempting(now)
            return try {
                report = fetch(city)
                backoff.succeeded()
                true
            } catch (_: WeatherException) {
                backoff.failed()
                false
            }
        }

        emit(KeptReport(city, report, clock.instant()))
        if (refresh()) emit(KeptReport(city, report, clock.instant()))
        while (true) {
            delay(tick.toMillis())
            refresh()
            emit(KeptReport(city, report, clock.instant()))
        }
    }
}
