package com.callbackdev.chiaro.data

import com.callbackdev.chiaro.domain.WeatherException
import com.callbackdev.chiaro.domain.model.Coordinates
import com.callbackdev.chiaro.domain.model.GeoFix
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private val milan = GeoFix(Coordinates(45.46, 9.19), "Milano", "Lombardia", "Italy")

/**
 * The throttle that used to live in a ViewModel field, now where every caller shares
 * it. These are the tests the old arrangement could not have: the double acquisition
 * on enable was two objects each believing it was the first, which is exactly what
 * the third and fourth cases here pin down.
 */
class CachedLocationProviderTest {

    private class TestClock(var now: Instant) : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId): Clock = this
        override fun instant(): Instant = now
        fun advance(duration: Duration) {
            now = now.plus(duration)
        }
    }

    private class FakeProvider : LocationProvider {
        var calls = 0
        var failure: WeatherException? = null
        var gate: CompletableDeferred<Unit>? = null
        var lastMaxAge: Duration? = null
        var lastTimeout: Duration? = null

        override suspend fun currentFix(maxAge: Duration, timeout: Duration): GeoFix {
            calls++
            lastMaxAge = maxAge
            lastTimeout = timeout
            gate?.await()
            failure?.let { throw it }
            return milan
        }
    }

    private val clock = TestClock(Instant.parse("2026-09-04T10:00:00Z"))
    private val delegate = FakeProvider()
    private val provider = CachedLocationProvider(delegate, clock)

    @Test
    fun `the first ask reaches the device`() = runTest {
        assertEquals(milan, provider.currentFix(LocationProvider.SilentMaxAge))
        assertEquals(1, delegate.calls)
    }

    @Test
    fun `an ask inside maxAge is answered from the memo`() = runTest {
        provider.currentFix(LocationProvider.SilentMaxAge)
        clock.advance(Duration.ofMinutes(1))

        assertEquals(milan, provider.currentFix(LocationProvider.SilentMaxAge))
        assertEquals(1, delegate.calls)
    }

    @Test
    fun `an ask past maxAge acquires again`() = runTest {
        provider.currentFix(LocationProvider.SilentMaxAge)
        clock.advance(Duration.ofMinutes(6))

        provider.currentFix(LocationProvider.SilentMaxAge)
        assertEquals(2, delegate.calls)
    }

    /** The enable and the page settle it causes: one gesture, one acquisition. */
    @Test
    fun `an explicit ask and the automatic one behind it are one acquisition`() = runTest {
        provider.currentFix(LocationProvider.Now)
        clock.advance(Duration.ofMillis(400)) // a store write, a flow, a pager settle

        provider.currentFix(LocationProvider.SilentMaxAge)
        assertEquals(1, delegate.calls)
    }

    /** ...but a pull minutes later is a real ask and gets a real fix. */
    @Test
    fun `an explicit ask does not accept a memo the reader has outlived`() = runTest {
        provider.currentFix(LocationProvider.SilentMaxAge)
        clock.advance(Duration.ofMinutes(1))

        provider.currentFix(LocationProvider.Now)
        assertEquals(2, delegate.calls)
    }

    @Test
    fun `two callers at once wait for the one acquisition in flight`() = runTest {
        val gate = CompletableDeferred<Unit>()
        delegate.gate = gate

        val first = async { provider.currentFix(LocationProvider.Now) }
        val second = async { provider.currentFix(LocationProvider.Now) }
        testScheduler.runCurrent()
        gate.complete(Unit)

        assertEquals(milan, first.await())
        assertEquals(milan, second.await())
        assertEquals(1, delegate.calls)
    }

    /** Nothing was learned, so nothing is remembered: the next ask tries again rather
     * than inheriting a failure. */
    @Test
    fun `a failed acquisition is not memoized`() = runTest {
        delegate.failure = WeatherException.LocationTimeout()
        var thrown: WeatherException? = null
        try {
            provider.currentFix(LocationProvider.SilentMaxAge)
        } catch (e: WeatherException) {
            thrown = e
        }
        assertTrue("the failure reaches the caller", thrown is WeatherException.LocationTimeout)

        delegate.failure = null
        assertEquals(milan, provider.currentFix(LocationProvider.SilentMaxAge))
        assertEquals(2, delegate.calls)
    }

    @Test
    fun `the caller's own maxAge and timeout reach the device untouched`() = runTest {
        provider.currentFix(LocationProvider.Now, LocationProvider.SilentTimeout)

        assertEquals(LocationProvider.Now, delegate.lastMaxAge)
        assertEquals(LocationProvider.SilentTimeout, delegate.lastTimeout)
    }
}
