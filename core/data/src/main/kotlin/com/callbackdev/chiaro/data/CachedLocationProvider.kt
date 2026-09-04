package com.callbackdev.chiaro.data

import com.callbackdev.chiaro.domain.model.GeoFix
import java.time.Clock
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * One acquisition per ask, however many callers ask (Fase 3b).
 *
 * The screens reach the position through two ViewModels — Places owns the toggle and
 * the row, Today owns the page — and each used to keep its own idea of when the last
 * fix was taken. Turning the source on therefore cost two acquisitions back to back:
 * the enable took one, the store emission moved the pager onto the position page, and
 * the settle took another from an object that had never heard of the first. The
 * throttle belongs under both of them, not inside either.
 *
 * Two mechanisms, and they are not the same thing. The **memo** answers from the last
 * fix while it is younger than the caller's `maxAge`, which is what makes an explicit
 * ask followed by the automatic one it triggers a single acquisition. The **lock**
 * makes concurrent callers wait for the one already in flight rather than starting a
 * second, and [CoalesceWindow] is what they then read: without it the caller that
 * waited would see a memo aged zero-or-one milliseconds and re-acquire on a rounding
 * accident.
 *
 * Deliberately in memory only: what has to survive the process is the fix itself,
 * and that is [CityStore]'s job — it persists the position with the instant it was
 * taken, so a cold start behind a recent one asks for nothing at all.
 */
class CachedLocationProvider(
    private val delegate: LocationProvider,
    private val clock: Clock = Clock.systemUTC()
) : LocationProvider {

    private val mutex = Mutex()

    @Volatile
    private var fix: GeoFix? = null

    @Volatile
    private var takenAt: Instant? = null

    override suspend fun currentFix(maxAge: Duration, timeout: Duration): GeoFix =
        mutex.withLock {
            memoized(maxAge)?.let { return@withLock it }
            delegate.currentFix(maxAge, timeout).also {
                fix = it
                takenAt = clock.instant()
            }
        }

    private fun memoized(maxAge: Duration): GeoFix? {
        val current = fix ?: return null
        val taken = takenAt ?: return null
        val age = Duration.between(taken, clock.instant())
        if (age.isNegative) return null // the clock moved backwards under us
        val ceiling = if (maxAge < CoalesceWindow) CoalesceWindow else maxAge
        return current.takeIf { age <= ceiling }
    }

    private companion object {
        /**
         * Two asks this close together are one ask. It is the width of a UI chain —
         * a toggle that writes a store, a flow that emits, a pager that animates to
         * the page and settles — and well under the interval at which anything the
         * reader would notice can change.
         */
        val CoalesceWindow: Duration = Duration.ofSeconds(10)
    }
}
