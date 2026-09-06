package com.callbackdev.chiaro.data

import com.callbackdev.chiaro.domain.WeatherFreshness
import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The two intervals this app keeps, and the fact that they are two (device, 6 set 2026).
 *
 * It lives in `:core:data` rather than beside the object it tests because
 * [UpdateFrequencies] does: the point of the second case is that the invariant holds
 * for every interval the reader can actually pick, and a hand-copied list of them
 * would be exactly the thing that drifts.
 */
class WeatherFreshnessTest {

    private val now: Instant = Instant.parse("2026-09-06T10:00:00Z")

    @Test
    fun `the cache ttl is the provider's own resolution, not a setting`() {
        // Open-Meteo answers `"interval": 900` beside every `current` block. A held
        // report past that is a value the provider has already replaced.
        assertEquals(Duration.ofMinutes(15), WeatherFreshness.ProviderResolution)
    }

    @Test
    fun `a cache hit can never be stale, at any polling interval the reader can pick`() {
        UpdateFrequencies.forEach { minutes ->
            assertTrue(
                "$minutes min",
                WeatherFreshness.ProviderResolution < WeatherFreshness.staleAfter(minutes)
            )
            val oldestHit = now.minus(WeatherFreshness.ProviderResolution)
            assertFalse("$minutes min", WeatherFreshness.isStale(oldestHit, minutes, now))
        }
    }

    @Test
    fun `stale is still two missed syncs, and it moves with the setting`() {
        assertEquals(Duration.ofMinutes(120), WeatherFreshness.staleAfter(60))
        assertFalse(WeatherFreshness.isStale(now.minus(Duration.ofMinutes(120)), 60, now))
        assertTrue(WeatherFreshness.isStale(now.minus(Duration.ofMinutes(121)), 60, now))
    }
}
