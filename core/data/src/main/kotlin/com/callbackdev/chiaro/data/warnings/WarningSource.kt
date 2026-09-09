package com.callbackdev.chiaro.data.warnings

import com.callbackdev.chiaro.domain.warnings.WarningBulletin
import java.time.ZoneId

/**
 * One issuer of official warnings (Fase 11: the Dipartimento della Protezione
 * Civile; Fase 12 adds MeteoAlarm behind the same seam). A source knows how to find
 * and read ITS bulletin; what to do with one — which place it concerns, whether it is
 * news, whether to speak — is the step's and the domain's.
 */
interface WarningSource {

    /** Stable key the store files this source's bulletin under (`"dpc"`). */
    val id: String

    /** The issuer's clock: the bulletin's hours and the fetch cadence live in it. */
    val zone: ZoneId

    /**
     * Looks for a bulletin newer than [known]. [allowLargeDownload] is whether the
     * network is unmetered right now: a source with a heavy fallback (the DPC's
     * 4.7 MB `latest_all.zip`) may take it only then.
     */
    suspend fun fetch(known: WarningFetchState, allowLargeDownload: Boolean): WarningFetchResult
}

/**
 * What the caller already holds, so the source can stop early: the [stamp] of the
 * bulletin in hand and the [feedTag] (an HTTP ETag) of the discovery feed as last
 * seen — measured 9 set 2026, GitHub's commit Atom answers `If-None-Match` with a
 * 304 and no body, so an hourly re-check of a held bulletin costs nothing.
 */
data class WarningFetchState(
    val stamp: String? = null,
    val feedTag: String? = null
)

sealed interface WarningFetchResult {

    /** A bulletin the caller did not have. */
    data class Fresh(
        val bulletin: WarningBulletin,
        val stamp: String,
        val feedTag: String?
    ) : WarningFetchResult

    /**
     * Nothing newer than what the caller holds. [feedTag] is what to remember for the
     * next look; null when the source could not vouch for the feed being unchanged
     * (a newer stamp was announced but its files are not up yet), so the next run
     * asks again in full.
     */
    data class Unchanged(val feedTag: String?) : WarningFetchResult

    /** The network or the issuer let us down; the caller keeps what it has. */
    data class Failed(
        val reason: WarningFetchFailure,
        val cause: Throwable? = null
    ) : WarningFetchResult
}

enum class WarningFetchFailure {
    /** No connection, DNS, timeout. */
    OFFLINE,
    /** The issuer's host answered with an error. */
    SERVICE,
    /** The bulletin came back and could not be read. */
    MALFORMED
}
