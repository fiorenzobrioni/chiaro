package com.callbackdev.chiaro.ui.today

import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The minute tick's wait after a failed fetch (19 ott 2026): a minute, then two, four,
 * eight, then the provider's quarter of an hour; a success starts over.
 */
class RefreshBackoffTest {

    private val t0 = Instant.parse("2026-10-19T09:00:00Z")
    private fun at(minutes: Long): Instant = t0.plus(Duration.ofMinutes(minutes))
    private fun minutes(n: Long): Duration = Duration.ofMinutes(n)

    @Test
    fun `nothing to wait for before any failure, or after a success`() {
        val backoff = RefreshBackoff()
        assertTrue(backoff.isDue(t0))
        backoff.attempting(t0)
        backoff.succeeded()
        assertTrue(backoff.isDue(t0))
        assertEquals(Duration.ZERO, backoff.waitAfter(0))
    }

    @Test
    fun `the wait doubles from a minute and stops at the provider's quarter of an hour`() {
        val backoff = RefreshBackoff()
        assertEquals(minutes(1), backoff.waitAfter(1))
        assertEquals(minutes(2), backoff.waitAfter(2))
        assertEquals(minutes(4), backoff.waitAfter(3))
        assertEquals(minutes(8), backoff.waitAfter(4))
        assertEquals(minutes(15), backoff.waitAfter(5))
        assertEquals(minutes(15), backoff.waitAfter(9))
        // Far past any doubling the cap still wins, and nothing overflows.
        assertEquals(minutes(15), backoff.waitAfter(200))
    }

    @Test
    fun `the tick asks again once the wait is over, not before`() {
        val backoff = RefreshBackoff()
        backoff.attempting(t0)
        backoff.failed()
        // One failure: the minute the tick always had.
        assertFalse(backoff.isDue(at(0)))
        assertTrue(backoff.isDue(at(1)))
        backoff.attempting(at(1))
        backoff.failed()
        assertFalse(backoff.isDue(at(2)))
        assertTrue(backoff.isDue(at(3)))
        backoff.attempting(at(3))
        backoff.failed()
        assertFalse(backoff.isDue(at(6)))
        assertTrue(backoff.isDue(at(7)))
        backoff.attempting(at(7))
        backoff.failed()
        assertFalse(backoff.isDue(at(14)))
        assertTrue(backoff.isDue(at(15)))
        assertEquals(4, backoff.failures)
    }

    @Test
    fun `a success in the middle starts the ladder over`() {
        val backoff = RefreshBackoff()
        repeat(3) { backoff.attempting(t0); backoff.failed() }
        assertEquals(minutes(4), backoff.waitAfter(backoff.failures))
        backoff.attempting(at(4))
        backoff.succeeded()
        assertEquals(0, backoff.failures)
        assertTrue(backoff.isDue(at(4)))
        // The next failure is a first failure again.
        backoff.attempting(at(20))
        backoff.failed()
        assertFalse(backoff.isDue(at(20)))
        assertTrue(backoff.isDue(at(21)))
    }

    @Test
    fun `a shorter cap bounds the ladder where it says`() {
        val backoff = RefreshBackoff(cap = minutes(3))
        assertEquals(minutes(1), backoff.waitAfter(1))
        assertEquals(minutes(2), backoff.waitAfter(2))
        assertEquals(minutes(3), backoff.waitAfter(3))
        assertEquals(minutes(3), backoff.waitAfter(8))
    }
}
