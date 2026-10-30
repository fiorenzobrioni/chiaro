package com.callbackdev.chiaro.data

import com.callbackdev.chiaro.domain.WeatherException
import java.time.Clock
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The position re-taken when nobody asked out loud: the pager settling on the position
 * page, the app coming back to the front and, since 30 ott 2026, the tap on the position
 * row in Places.
 *
 * That tap used to be an ask out loud, a new fix whatever the age of the last one, with
 * the sheet held open for up to fifteen seconds over a page that had already switched
 * behind it (committente: «riparte la ricerca che può durare vari secondi»). The swipe
 * onto the same page asked for nothing when the last fix was recent and kept nobody
 * waiting when it was not. Both are this road now, so the two ways onto one page cannot
 * behave in two ways.
 *
 * Silent by design: a fix that fails leaves the last place with its real weather, which
 * beats an error over numbers that are still right. The asks out loud (the pull, the
 * toggle, a first fix) stay [LocationProvider.Now] and say why they failed.
 *
 * One run at a time for whoever holds this object, and the app holds one
 * ([ServiceLocator.gpsRefix]): a tap on the row selects the page and the pager then
 * settles on it, two callers for one gesture. The second waits for the first and finds
 * the fix fresh; without the lock both took the same fix and, when it moved the reader,
 * both fetched the new place's weather.
 */
class GpsRefix(
    private val cityStore: CityStore,
    private val locationProvider: LocationProvider,
    private val arrival: GpsArrival,
    private val clock: Clock = Clock.systemUTC()
) {

    private val mutex = Mutex()

    /**
     * Re-takes the position when one is due and hands it to [GpsArrival]. With none due,
     * asks again for the weather of a place reached and still waiting for it (20 ott
     * 2026): the position was taken already, only the forecast is missing.
     */
    suspend fun run() {
        mutex.withLock { refix() }
    }

    private suspend fun refix() {
        val settings = cityStore.locationSettings.first()
        // Turned off while a run waited for the lock: nobody is asking any more.
        if (!settings.useGps) return
        if (settings.gpsCity != null && !isDue(settings.fixedAt)) {
            if (settings.arrival != null) arrival.retry()
            return
        }
        // The interval is also the maxAge, so the layer that knows how to be cheap gets
        // to be cheap: below it the platform answers from a position it already holds
        // and powers nothing up at all.
        val fix = try {
            locationProvider.currentFix(maxAge = Interval, timeout = LocationProvider.SilentTimeout)
        } catch (e: WeatherException) {
            return
        }
        arrival.take(fix, clock.instant())
    }

    /** Whether the persisted fix has aged past [Interval]. No fix ever taken, or a clock
     * that moved backwards under us, counts as due. */
    private fun isDue(fixedAt: Instant?): Boolean {
        fixedAt ?: return true
        val age = Duration.between(fixedAt, clock.instant())
        return age.isNegative || age >= Interval
    }

    companion object {
        /**
         * How stale a fix has to be before this road takes another one, and the maxAge
         * handed to the provider. Battery is a feature (CLAUDE.md): a one-shot coarse fix
         * is cheap, a fix per swipe is not, and a fix per launch is the one that adds up.
         */
        val Interval: Duration = LocationProvider.SilentMaxAge
    }
}
