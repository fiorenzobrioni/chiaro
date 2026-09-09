package com.callbackdev.chiaro.domain.warnings

import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The cadence of «La cadenza del passo», as a table over the issuer's clock. */
class WarningFetchPolicyTest {

    private val sept8: LocalDate = LocalDate.of(2026, 9, 8)
    private val sept9: LocalDate = sept8.plusDays(1)

    private fun bulletin(issued: LocalDateTime) = WarningBulletin(
        id = "B", issuedAt = issued, days = listOf(issued.toLocalDate(), issued.toLocalDate().plusDays(1)),
        note = null, warnings = emptyList()
    )

    private val yesterdays = bulletin(sept8.atTime(15, 19))
    private val todays = bulletin(sept9.atTime(15, 46))

    private fun at(hour: Int, minute: Int) = sept9.atTime(hour, minute)

    @Test
    fun `nothing in hand - every run asks, at any hour`() {
        assertTrue(WarningFetchPolicy.shouldFetch(at(3, 0), current = null, lastAttempt = at(2, 50)))
        assertTrue(WarningFetchPolicy.shouldFetch(at(10, 0), current = null, lastAttempt = null))
    }

    @Test
    fun `an expired bulletin counts as nothing in hand`() {
        val stale = bulletin(sept8.minusDays(3).atTime(15, 0))
        assertTrue(WarningFetchPolicy.shouldFetch(at(9, 0), stale, lastAttempt = at(8, 55)))
    }

    @Test
    fun `before the afternoon, yesterday's tomorrow is today - nothing to download`() {
        assertFalse(WarningFetchPolicy.shouldFetch(at(9, 0), yesterdays, lastAttempt = null))
        assertFalse(WarningFetchPolicy.shouldFetch(at(15, 29), yesterdays, lastAttempt = null))
    }

    @Test
    fun `from half past three, every run looks for today's bulletin until it lands`() {
        assertTrue(WarningFetchPolicy.shouldFetch(at(15, 30), yesterdays, lastAttempt = null))
        assertTrue(WarningFetchPolicy.shouldFetch(at(16, 0), yesterdays, lastAttempt = at(15, 45)))
        assertTrue(WarningFetchPolicy.shouldFetch(at(22, 0), yesterdays, lastAttempt = at(21, 50)))
    }

    @Test
    fun `today's bulletin in hand - one look an hour for an update, until the evening`() {
        assertFalse(WarningFetchPolicy.shouldFetch(at(16, 0), todays, lastAttempt = at(15, 35)))
        assertTrue(WarningFetchPolicy.shouldFetch(at(16, 40), todays, lastAttempt = at(15, 35)))
        assertTrue(WarningFetchPolicy.shouldFetch(at(16, 40), todays, lastAttempt = null))
        assertTrue(WarningFetchPolicy.shouldFetch(at(20, 59), todays, lastAttempt = at(19, 0)))
        assertFalse(WarningFetchPolicy.shouldFetch(at(21, 1), todays, lastAttempt = at(19, 0)))
    }

    @Test
    fun `today's bulletin in hand before half past three is left alone too`() {
        // An early «Aggiornamento» held from the morning: nothing new before the afternoon.
        val early = bulletin(sept9.atTime(9, 0))
        assertFalse(WarningFetchPolicy.shouldFetch(at(11, 0), early, lastAttempt = null))
    }
}
