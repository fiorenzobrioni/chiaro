package com.callbackdev.chiaro.data

import com.callbackdev.chiaro.data.local.ReportDiskCache
import com.callbackdev.chiaro.data.local.WeatherHistoryDao
import com.callbackdev.chiaro.data.local.WeatherHistoryEntry
import com.callbackdev.chiaro.data.local.WeatherSnapshots
import com.callbackdev.chiaro.data.mapper.WeatherReportMapper
import com.callbackdev.chiaro.data.remote.OpenMeteoAirQualityApi
import com.callbackdev.chiaro.data.remote.OpenMeteoForecastApi
import com.callbackdev.chiaro.data.remote.OpenMeteoWitnessApi
import com.callbackdev.chiaro.data.remote.OpenMeteoGeocodingApi
import com.callbackdev.chiaro.data.remote.dto.ApiErrorDto
import com.callbackdev.chiaro.data.remote.dto.GeoResultDto
import com.callbackdev.chiaro.domain.WeatherException
import com.callbackdev.chiaro.domain.WeatherFreshness
import com.callbackdev.chiaro.domain.model.CacheStatus
import com.callbackdev.chiaro.domain.model.City
import com.callbackdev.chiaro.domain.model.Coordinates
import com.callbackdev.chiaro.domain.model.WeatherReport
import com.callbackdev.chiaro.domain.rules.NotificationRule
import androidx.annotation.VisibleForTesting
import java.io.IOException
import java.io.InterruptedIOException
import java.net.UnknownHostException
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.cancellation.CancellationException
import kotlin.random.Random
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import retrofit2.HttpException

/**
 * Single entry point of the data layer. In-memory cache of the last report per city
 * (TTL-based HIT/MISS, surfaced in `system_info`) backed by [ReportDiskCache] so a
 * process death doesn't cost a re-fetch inside the TTL, a short history of the
 * forecasts in Room (what Today's "what changed" and Alerts' «last fired» read), errors
 * normalized to [WeatherException].
 */
class WeatherRepository(
    private val forecastApi: OpenMeteoForecastApi,
    private val airQualityApi: OpenMeteoAirQualityApi,
    private val geocodingApi: OpenMeteoGeocodingApi,
    private val historyDao: WeatherHistoryDao,
    private val diskCache: ReportDiskCache? = null,
    private val json: Json = Json,
    private val clock: Clock = Clock.systemUTC(),
    private val cacheTtl: Duration = WeatherFreshness.ProviderResolution,
    /**
     * Fired after every history commit — the single choke point where new data
     * lands, whichever caller fetched it (FAB, cold start, background worker).
     * Wired to the home-widget re-render in [ServiceLocator]; a cache HIT rightly
     * skips it, as nothing changed.
     */
    private val onHistoryCommitted: suspend () -> Unit = {},
    /**
     * The places the app still follows (`City.cacheKey` of the saved list and the GPS
     * fix), read at the commit, which is where the storage tidies up after itself:
     * [onHousekeeping] gets them (the `StoredDataSweep` of the disk entries and the
     * history, the rules' latches) and the in-memory cache forgets everything else.
     * Injected because the saved-places list is not this class's to know —
     * [ServiceLocator] wires it. Null means a test that does not care about retention
     * gets none.
     */
    private val liveKeys: (suspend () -> Set<String>)? = null,
    private val onHousekeeping: suspend (liveKeys: Set<String>) -> Unit = {},
    /** How long [fetch] waits at least before its one retry of a transient failure. */
    private val transientRetryDelay: Duration = TRANSIENT_RETRY_DELAY,
    /** The random spread on top of it, so that one failure does not become one herd. */
    private val transientRetryJitter: Duration = TRANSIENT_RETRY_JITTER,
    /**
     * The other model families that vote on the states (26 ott 2026). Null — a test, or a
     * build without them — and the engine reads `best_match` alone.
     */
    private val witnessApi: OpenMeteoWitnessApi? = null
) {

    private data class CacheEntry(val report: WeatherReport, val fetchedAt: Instant)

    private val cache = ConcurrentHashMap<String, CacheEntry>()

    /**
     * The geocoding index is searched in the DEVICE's language, not in the app's
     * fallback English (Fase 13f): on Open-Meteo that parameter decides what the query
     * can match, so an Italian phone had to ask for "Florence" to find Firenze. Read
     * per call — the system per-app language picker can change it under a live process.
     */
    suspend fun searchCities(query: String): List<City> = wrapErrors {
        val language = OpenMeteoGeocodingApi.languageOf(Locale.getDefault())
        val results = geocodingApi.search(query, language = language).results
        if (results.isEmpty()) throw WeatherException.CityNotFound(query)
        results.map { it.toCity() }
    }

    /**
     * Report for [city]: fresh cache entry unless expired or [forceRefresh] (the
     * refresh the reader asked for). Cache hits keep the original `last_sync` and
     * flip `cache_status` to HIT.
     *
     * [ttl] defaults to [WeatherFreshness.ProviderResolution] and every caller takes
     * that default since the 6 set 2026 fix. It used to be handed the reader's
     * `update_frequency_min`, which turned a battery setting into the answer to "how
     * old may the numbers be while somebody is looking at them" — an hour by default,
     * two at the top of the range. The parameter stays because a test wants to name
     * its own window, not because a caller should.
     */
    suspend fun getWeather(
        city: City,
        forceRefresh: Boolean = false,
        ttl: Duration = cacheTtl
    ): WeatherReport {
        val now = clock.instant()
        if (!forceRefresh) {
            cache[city.cacheKey]
                ?.takeIf { Duration.between(it.fetchedAt, now) < ttl }
                ?.let {
                    return it.report.copy(
                        systemInfo = it.report.systemInfo.copy(cacheStatus = CacheStatus.HIT)
                    )
                }
            // Process death wipes the map above: an unexpired disk entry (last fetch
            // by any process — app or worker) re-maps as a HIT instead of re-spending
            // two GETs. No history commit: its fetch already committed one. An entry an
            // older app fetched with a shorter request is not one: it lacks the new fields.
            diskCache?.read(city.cacheKey)
                ?.takeIf { it.isCurrentRequest && Duration.between(Instant.ofEpochMilli(it.fetchedAtEpochMs), now) < ttl }
                ?.let { entry ->
                    val fetchedAt = Instant.ofEpochMilli(entry.fetchedAtEpochMs)
                    runCatching {
                        WeatherReportMapper.map(
                            city = city,
                            forecast = entry.forecast,
                            airQuality = entry.airQuality,
                            witnesses = entry.witnesses,
                            fetchedAt = fetchedAt,
                            responseTimeMs = entry.responseTimeMs,
                            cacheStatus = CacheStatus.MISS
                        )
                    }.getOrNull()?.let { report ->
                        cache[city.cacheKey] = CacheEntry(report, fetchedAt)
                        return report.copy(
                            systemInfo = report.systemInfo.copy(cacheStatus = CacheStatus.HIT)
                        )
                    }
                }
        }
        return fetch(city, now)
    }

    /**
     * The freshest report the app ALREADY has for [city], or null — never a fetch
     * (Fase 16d).
     *
     * `sky.crontab` reads the sky from whatever the last sync brought back and must
     * not go to the network on its own: opening a tab is not a reason to spend two
     * HTTP GETs, and the schedule it renders needs no network at all. No TTL gate
     * either, unlike [getWeather]: this returns what exists and lets the caller judge
     * its age from `systemInfo.lastSync`, which is exactly the judgement the verdict
     * has to make out loud.
     */
    suspend fun cachedReport(city: City): WeatherReport? {
        cache[city.cacheKey]?.let { return it.report }
        val entry = diskCache?.read(city.cacheKey) ?: return null
        val fetchedAt = Instant.ofEpochMilli(entry.fetchedAtEpochMs)
        return runCatching {
            WeatherReportMapper.map(
                city = city,
                forecast = entry.forecast,
                airQuality = entry.airQuality,
                witnesses = entry.witnesses,
                fetchedAt = fetchedAt,
                responseTimeMs = entry.responseTimeMs,
                cacheStatus = CacheStatus.HIT
            )
        }.getOrNull()?.also { cache[city.cacheKey] = CacheEntry(it, fetchedAt) }
    }

    /**
     * Fase 11: marks the user rules that fired on [city]'s newest commit — the data
     * the worker just evaluated (that commit even on a cache HIT: a hit means the
     * latest commit is still the current data). Alerts reads «last fired» off these.
     *
     * Merged with what the commit already lists, never written over it (14 ott 2026):
     * a commit can now stand for several fetches ([recordHistory]), and a rule that
     * fired on the first of them must not vanish because another fired on the second.
     *
     * Each rule is written as `id:nome` ([FiredRule], 15.4, 28 Oct 2026): the id is what
     * «last fired» is read by, so a rename keeps its past and two rules never share one.
     */
    suspend fun recordFiredRules(city: City, rules: List<NotificationRule>) {
        if (rules.isEmpty()) return
        val latest = historyDao.latestFor(city.cacheKey) ?: return
        val merged = (firedRules(latest) + rules.map(FiredRule::of)).distinct()
        historyDao.setFiredRules(latest.id, json.encodeToString(merged.map { it.encode() }))
    }

    /** [city]'s commits, newest first; the default reaches the whole retention. */
    suspend fun historyFor(city: City, limit: Int = HISTORY_MAX_ROWS_PER_PLACE) =
        historyDao.historyFor(city.cacheKey, limit)

    /** The rules a commit recorded, decoded where they were encoded (Fase 6): the Alerts
     * screen reads "last fired" off these without owning the JSON. The rows written before
     * 15.4 carry the name alone and come back with no id ([FiredRule.decode]). */
    fun firedRules(entry: WeatherHistoryEntry): List<FiredRule> =
        entry.firedRulesJson
            ?.let { runCatching { json.decodeFromString<List<String>>(it) }.getOrNull() }
            ?.map(FiredRule::decode)
            ?: emptyList()

    /** A commit's flattened week of forecast (`<date>.<field>` keys). */
    fun forecast(entry: WeatherHistoryEntry): Map<String, String> =
        runCatching { json.decodeFromString<Map<String, String>>(entry.forecastJson) }
            .getOrDefault(emptyMap())

    private suspend fun fetch(city: City, now: Instant): WeatherReport = wrapErrors {
        val startNanos = System.nanoTime()
        val (forecast, air, witnesses) = coroutineScope {
            val forecastDeferred = async {
                retryingTransient { forecastApi.forecast(city.coordinates.lat, city.coordinates.lon) }
            }
            // Air quality is best-effort: its failure must not sink the whole report
            val airDeferred = async {
                runCatching { airQualityApi.current(city.coordinates.lat, city.coordinates.lon) }
                    .getOrNull()
            }
            // So are the witnesses: without them the engine reads best_match alone.
            val witnessDeferred = async {
                witnessApi?.let { api ->
                    runCatching { api.witnesses(city.coordinates.lat, city.coordinates.lon) }.getOrNull()
                }
            }
            Triple(forecastDeferred.await(), airDeferred.await(), witnessDeferred.await())
        }
        val responseTimeMs = (System.nanoTime() - startNanos) / 1_000_000
        val report = WeatherReportMapper.map(
            city = city,
            forecast = forecast,
            airQuality = air?.current,
            witnesses = witnesses,
            fetchedAt = now,
            responseTimeMs = responseTimeMs,
            cacheStatus = CacheStatus.MISS
        )
        cache[city.cacheKey] = CacheEntry(report, now)
        diskCache?.write(
            city.cacheKey,
            ReportDiskCache.Entry(
                fetchedAtEpochMs = now.toEpochMilli(),
                responseTimeMs = responseTimeMs,
                forecast = forecast,
                airQuality = air?.current,
                request = ReportDiskCache.CURRENT_REQUEST,
                witnesses = witnesses
            )
        )
        recordHistory(city, report)
        report
    }

    /**
     * One commit per fetch, with two rules on top (14 ott 2026).
     *
     * **A fetch inside the previous commit's window replaces it** ([COMMIT_WINDOW], the
     * provider's own resolution): Open-Meteo moves its numbers every fifteen minutes at
     * most, so a second commit inside them is the same forecast written twice. It used
     * to be written twice, and it cost twice. Every pull to refresh stood a row beside
     * the last one, which made the depth of the history a function of how often the
     * reader pulled, and it emptied "what changed": the newest commit, identical to the
     * one a minute older, had no revision of its own, so the section vanished on the
     * pull. Replaced in place, the newest commit still compares against the forecast
     * before it. The rules it already lists stay listed.
     *
     * **Retention is by time** ([HISTORY_RETENTION]), never fewer than
     * [HISTORY_MIN_ROWS] a place, under a global ceiling that is only a backstop.
     */
    private suspend fun recordHistory(city: City, report: WeatherReport) {
        val forecast = json.encodeToString(WeatherSnapshots.flattenForecast(report))
        val timestamp = report.systemInfo.lastSync.epochSecond
        val latest = historyDao.latestFor(city.cacheKey)
        if (latest != null && timestamp - latest.timestampEpochSeconds in 0 until COMMIT_WINDOW.seconds) {
            historyDao.update(latest.copy(timestampEpochSeconds = timestamp, forecastJson = forecast))
        } else {
            historyDao.insert(
                WeatherHistoryEntry(
                    cityKey = city.cacheKey,
                    timestampEpochSeconds = timestamp,
                    forecastJson = forecast
                )
            )
        }
        historyDao.pruneCity(
            city.cacheKey,
            cutoffEpochSeconds = timestamp - HISTORY_RETENTION.seconds,
            keepAtLeast = HISTORY_MIN_ROWS
        )
        historyDao.prune(HISTORY_MAX_ROWS)
        // Its own best-effort call, not folded into the block below: housekeeping that
        // fails must still leave the widget its repaint.
        bestEffort { housekeeping() }
        // A failing observer must never sink a successful fetch: the report is already
        // cached and committed, and at worst the widget misses a repaint.
        bestEffort { onHistoryCommitted() }
    }

    /** What belongs to places the app no longer follows leaves, on disk and in memory. */
    private suspend fun housekeeping() {
        val live = liveKeys?.invoke() ?: return
        // The in-memory map had no bound but the process's life: every GPS cell a
        // long-lived process drove through kept a week of forecast in RAM.
        cache.keys.retainAll(live)
        onHousekeeping(live)
    }

    /**
     * Runs [block] for its effect and lets it fail — but a cancelled caller still has
     * to unwind, and a bare `runCatching` would eat the cancellation too, letting a
     * superseded load publish its stale report.
     */
    private suspend fun bestEffort(block: suspend () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // deliberately swallowed: see the call sites for what is being given up
        }
    }

    /**
     * [block] once more, after [nextRetryDelay], when the failure is one the service will
     * have passed by then and the first try cost no time to wait through:
     *
     * - one of [TRANSIENT_HTTP_CODES] (24 set 2026): a 5xx is the service's own passing
     *   state — a deploy, a node restarting, and since 19 ott 2026 a node shedding load
     *   with a 503 «The service is overloaded» at the peak of every full hour — and not a
     *   verdict on the request, so the reader who pulled to refresh should not be told
     *   the service is down because of one of them;
     * - a 429 whose reason is [CONCURRENCY_LIMIT_REASON] (19 ott 2026): Open-Meteo runs
     *   one request per address at a time and queues five, and the sixth is refused with
     *   that sentence; the queue clears in seconds. Every other 429 is a limit on the
     *   minute, the hour or the day and is left alone as before — a retry seconds later
     *   can only fail again and count against the same limit, which on a carrier's shared
     *   address is not ours alone;
     * - a connection the service dropped (17 ott 2026): on that day Open-Meteo's forecast
     *   host failed one TLS handshake in two, each failure a stalled connection and the
     *   next one fine, and a report that needed one lucky try out of two showed a
     *   skeleton for hours.
     *
     * **Once**, and never after time already spent. A 4xx is the request's fault and will
     * fail the same way. Not for an offline phone ([isOffline]): the cached report is
     * already on screen and nothing changes in a few seconds. Not after a timeout of any
     * kind either ([isTimeout], 19 ott 2026): the read timeout is twenty seconds and the
     * call's budget thirty (`ServiceLocator`), and a second try after either is a page that
     * never resolves and a worker held open on somebody's battery. The minute tick and the
     * job's own backoff are the second try, later.
     */
    private suspend fun <T> retryingTransient(block: suspend () -> T): T = try {
        block()
    } catch (e: HttpException) {
        // The body is read here, once: it is what tells the address's queue from a limit.
        val reason = e.reason()
        val passing = e.code() in TRANSIENT_HTTP_CODES ||
            (e.code() == 429 && reason == CONCURRENCY_LIMIT_REASON)
        if (!passing) throw WeatherException.ApiError(e.code(), e, reason)
        delay(nextRetryDelay().toMillis())
        block()
    } catch (e: IOException) {
        if (e.isOffline() || e.isTimeout()) throw e
        delay(nextRetryDelay().toMillis())
        block()
    }

    private inline fun <T> wrapErrors(block: () -> T): T = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: WeatherException) {
        throw e
    } catch (e: HttpException) {
        throw WeatherException.ApiError(e.code(), e, e.reason())
    } catch (e: IOException) {
        throw if (e.isOffline()) WeatherException.NoNetwork(e) else WeatherException.Unreachable(e)
    } catch (e: Exception) {
        throw WeatherException.Unknown(e)
    }

    /**
     * A name that did not resolve is the phone with no way out: with no network Android
     * has nowhere to ask, and that is the one failure that says so. Everything else — a
     * refused or reset connection, a handshake that stalled, a read that timed out — got
     * as far as the service and is the service's ([WeatherException.Unreachable]).
     */
    private fun IOException.isOffline(): Boolean = this is UnknownHostException

    /** A timeout of any kind — a single connect's or read's (`SocketTimeoutException`)
     * and OkHttp's `callTimeout`, both an [InterruptedIOException]: time already spent. */
    private fun IOException.isTimeout(): Boolean = this is InterruptedIOException

    /**
     * Open-Meteo's own sentence for an error status, from the body's `reason`
     * ([ApiErrorDto]); null for an empty body or one that is not its JSON (an
     * intermediary's HTML page). Read once and closed: a body left unread holds its
     * connection until the garbage collector finds it.
     */
    private fun HttpException.reason(): String? = runCatching {
        response()?.errorBody()?.use { body -> json.decodeFromString<ApiErrorDto>(body.string()).reason }
    }.getOrNull()

    /**
     * [transientRetryDelay] plus a random share of [transientRetryJitter] (19 ott 2026):
     * every phone that met the same 503 at the same second used to come back two seconds
     * later, together, to the same node. The floor stays the two seconds a restarting
     * node needs; the spread is paid to the service, not by the reader — a pull resolves
     * within seven seconds either way.
     */
    @VisibleForTesting
    internal fun nextRetryDelay(): Duration {
        val jitter = transientRetryJitter.toMillis()
        val share = if (jitter > 0) Random.nextLong(jitter + 1) else 0L
        return transientRetryDelay.plusMillis(share)
    }

    /**
     * Has this install ever fetched weather? The one durable trace of an app that has
     * been used, whatever the user did or did not configure — which is exactly what
     * [CityStore.migrateFirstRun] needs to tell an upgrade from a fresh install.
     */
    suspend fun hasAnyHistory(): Boolean = historyDao.any()

    companion object {
        /** The server-side answers worth one more try; see [retryingTransient]. */
        val TRANSIENT_HTTP_CODES = setOf(500, 502, 503, 504)

        /** Long enough for a restarting node, short enough for a pull to refresh. */
        val TRANSIENT_RETRY_DELAY: Duration = Duration.ofSeconds(2)

        /** The random spread on top of [TRANSIENT_RETRY_DELAY]: two to six seconds in all. */
        val TRANSIENT_RETRY_JITTER: Duration = Duration.ofSeconds(4)

        /**
         * What Open-Meteo answers a request that finds five others from the same address
         * already waiting (`RateLimiter.swift`, `ConcurrencyGroupLimiter.swift` in its
         * repository: one runs, five queue, the sixth is refused with HTTP 429 and this
         * sentence). The only 429 that clears by itself in seconds; the forecast host has
         * no IPv6, so on a carrier's shared IPv4 the address is every phone behind it.
         */
        const val CONCURRENCY_LIMIT_REASON = "Too many concurrent requests"

        /**
         * How far back a place's history reaches. Three days: the deepest reader is
         * yesterday's high, which in the place's own zone can lie up to 48 hours back,
         * and the third day is the margin a phone that slept through a morning needs.
         */
        val HISTORY_RETENTION: Duration = Duration.ofDays(3)

        /** The commits a place keeps past [HISTORY_RETENTION]: one to compare against,
         * one to be compared, for a phone that comes back after a week off. */
        const val HISTORY_MIN_ROWS = 2

        /** Two fetches closer than this are one commit: the provider's own resolution. */
        val COMMIT_WINDOW: Duration = WeatherFreshness.ProviderResolution

        /** The most commits [HISTORY_RETENTION] can hold for one place: one per window. */
        val HISTORY_MAX_ROWS_PER_PLACE: Int =
            (HISTORY_RETENTION.seconds / COMMIT_WINDOW.seconds).toInt()

        /**
         * The global backstop: eight places at their ceiling. Each commit is a week of
         * daily forecast, about a kilobyte, so the table cannot pass a couple of
         * megabytes, and the ceiling is far above what any place reaches at the hourly
         * cadence (some seventy rows), so it never truncates a place still followed.
         */
        private val HISTORY_MAX_ROWS: Int = HISTORY_MAX_ROWS_PER_PLACE * 8
    }
}

private fun GeoResultDto.toCity() = City(
    id = id,
    name = name,
    region = admin1,
    country = country ?: countryCode,
    coordinates = Coordinates(latitude, longitude),
    timezone = timezone,
    // Both received all along and dropped here until 9 set 2026: the official warnings
    // need the country as a code and the municipality as the zone index's fallback.
    countryCode = countryCode,
    admin3 = admin3
)
