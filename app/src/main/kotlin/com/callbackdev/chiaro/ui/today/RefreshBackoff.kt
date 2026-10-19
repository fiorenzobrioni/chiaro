package com.callbackdev.chiaro.ui.today

import com.callbackdev.chiaro.domain.WeatherFreshness
import java.time.Duration
import java.time.Instant

/**
 * How long Today's minute tick waits before asking the service again after a fetch that
 * failed (19 ott 2026).
 *
 * The tick re-reads a report older than the provider's quarter of an hour, and a failed
 * fetch leaves the report exactly that old: so during an outage a page left open asked
 * again every sixty seconds — two requests and a retry each time — for as long as the
 * outage lasted, to a service that was already shedding load. It also meant the page
 * healed within a minute of the service coming back, which is worth keeping.
 *
 * So the wait doubles with each failure in a row — one minute, two, four, eight — and
 * stops at [cap], the provider's own resolution: a page that cannot be fifteen minutes
 * fresh has no reason to ask more often than the data moves. The first wait is the
 * minute the tick always had, so one failure changes nothing the reader can see. Any
 * success starts over. The tick is the only caller that asks [isDue]; a pull, the
 * landing fetch and a change of place never wait.
 */
class RefreshBackoff(private val cap: Duration = WeatherFreshness.ProviderResolution) {

    /** Fetches that failed since the last success, whoever asked for them. */
    var failures: Int = 0
        private set

    private var lastAttempt: Instant? = null

    /** A fetch is about to go out. */
    fun attempting(now: Instant) {
        lastAttempt = now
    }

    fun succeeded() {
        failures = 0
    }

    fun failed() {
        failures++
    }

    /** The wait after [failures] in a row: none after none, then 1, 2, 4, 8 minutes, then [cap]. */
    fun waitAfter(failures: Int): Duration {
        if (failures <= 0) return Duration.ZERO
        val doublings = (failures - 1).coerceAtMost(MAX_DOUBLINGS)
        return minOf(FIRST_WAIT.multipliedBy(1L shl doublings), cap)
    }

    /** Whether the tick may fetch at [now]: at once after a success, else once the wait is over. */
    fun isDue(now: Instant): Boolean {
        val last = lastAttempt ?: return true
        return Duration.between(last, now) >= waitAfter(failures)
    }

    private companion object {
        val FIRST_WAIT: Duration = Duration.ofMinutes(1)

        /** Past 2^5 minutes the cap wins whatever it is, and the shift never overflows. */
        const val MAX_DOUBLINGS = 5
    }
}
