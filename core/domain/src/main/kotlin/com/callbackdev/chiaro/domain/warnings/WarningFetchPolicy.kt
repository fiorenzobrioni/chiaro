package com.callbackdev.chiaro.domain.warnings

import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * When the job may spend network on the bulletin (PLANNING, Fase 11, «La cadenza del
 * passo»). The step has no clock of its own — it runs when the shared job runs — so
 * this answers "is it worth asking now", given what is already held. Pure; the
 * caller hands in the ISSUER's local time (Europe/Rome for the Dipartimento).
 *
 * Measured on 8–9 set 2026: the bulletin is out between 15:19 and 15:46 and on
 * GitHub within twenty-five minutes; an «Aggiornamento» can follow later the same
 * afternoon, so a held bulletin is re-checked once an hour until the evening.
 * Before the afternoon, yesterday's "domani" IS today and there is nothing new to
 * download.
 */
object WarningFetchPolicy {

    /** Earliest the day's bulletin is worth looking for. */
    val PublicationStart: LocalTime = LocalTime.of(15, 30)

    /** After this, today's bulletin is final for the purposes of a phone. */
    val RecheckUntil: LocalTime = LocalTime.of(21, 0)

    /** How often a held bulletin of today is re-checked for an update. */
    val RecheckEvery: Duration = Duration.ofHours(1)

    /**
     * @param now the issuer's local time
     * @param current the bulletin held, if any
     * @param lastAttempt when the network was last asked, if ever (success or failure)
     */
    fun shouldFetch(
        now: LocalDateTime,
        current: WarningBulletin?,
        lastAttempt: LocalDateTime?
    ): Boolean {
        // Nothing valid in hand: every run is a chance, the job's interval the pace.
        if (current == null || !now.isBefore(current.expiresAt)) return true
        val time = now.toLocalTime()
        if (time < PublicationStart) return false
        // Yesterday's bulletin past the hour: today's is out or about to be.
        if (current.issuedAt.toLocalDate() < now.toLocalDate()) return true
        // Today's bulletin held: an hourly look for an update, until the evening.
        if (time > RecheckUntil) return false
        return lastAttempt == null || Duration.between(lastAttempt, now) >= RecheckEvery
    }
}
