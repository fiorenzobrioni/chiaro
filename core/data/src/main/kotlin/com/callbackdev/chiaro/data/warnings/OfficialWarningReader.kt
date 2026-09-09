package com.callbackdev.chiaro.data.warnings

import com.callbackdev.chiaro.domain.model.City
import com.callbackdev.chiaro.domain.warnings.OfficialWarningEngine
import com.callbackdev.chiaro.domain.warnings.PlaceWarnings
import com.callbackdev.chiaro.domain.warnings.WarningLevel
import com.callbackdev.chiaro.domain.warnings.WarningZone
import com.callbackdev.chiaro.domain.warnings.WarningZoneIndex
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * What a SCREEN may say about the official warnings for one place (Fase 11, third step).
 * The step in the job writes the bulletin; this reads it, and the two never share a
 * code path beyond the store and the engine.
 *
 * The four states are the four honest answers, and Avvisi prints all four — which is why
 * absence is a value here and not a null: "there is no warning for Milano" is the answer
 * somebody came to that screen for, while Today draws nothing at all for it (§1.1).
 */
sealed interface PlaceWarningState {

    /** The place is in no zone the issuer grades — outside Italy, or at sea. */
    data object Unavailable : PlaceWarningState

    /** In a zone, but no bulletin has been read yet: a fresh install before the first job. */
    data class Waiting(val zone: WarningZone) : PlaceWarningState

    /**
     * In a zone, and the newest bulletin in hand no longer covers today — the afternoon's
     * has not landed, or the phone has been offline. Its date is stated rather than its
     * contents: yesterday's grades for a day that is over are not an answer.
     */
    data class Stale(val zone: WarningZone, val issuedAt: LocalDateTime) : PlaceWarningState

    /** A bulletin that still covers today. [warnings] may be green on every day. */
    data class Current(val warnings: PlaceWarnings) : PlaceWarningState
}

/**
 * The reader side of [OfficialWarningStore]. [index] is a supplier and not the index
 * itself because loading it decodes ~290 KB of JSON: a screen that never asks (a reader
 * abroad, a page never opened) never pays for it, and the caller keeps it off the main
 * thread.
 */
class OfficialWarningReader(
    private val store: OfficialWarningStore,
    private val index: () -> WarningZoneIndex,
    private val sourceId: String = DpcBulletinSource.ID,
    /** The issuer's clock: "today" for a bulletin is Rome's today, never the device's. */
    val issuerZone: ZoneId = DpcBulletinSource.ZONE
) {

    /** The bulletin in hand, re-emitted whenever the job replaces it. */
    val bulletin: Flow<StoredBulletin?> = store.current(sourceId)

    /** The zone a place falls in, or null. Blocking on first call: loads the index. */
    fun zoneOf(city: City): WarningZone? = index().locate(city.coordinates, city.admin3)

    /** Today in the issuer's zone — the date every rule below is written against. */
    fun today(): LocalDate = LocalDate.now(issuerZone)

    /**
     * The warning worth DRAWING for [city]: what the current bulletin grades it at when
     * something is above NONE, and null for everything else — no bulletin, no zone, or a
     * zone the bulletin leaves green. It is the widgets' whole question, and Today's.
     *
     * The store is asked FIRST and the index only after. The job's step is inert when no
     * saved place falls in a graded zone, so a reader whose places are all abroad has no
     * bulletin stored at all, and this never decodes the 290 KB asset on their behalf.
     */
    suspend fun graded(city: City): PlaceWarnings? {
        val stored = bulletin.first() ?: return null
        val zone = runCatching { zoneOf(city) }.getOrNull() ?: return null
        val current = state(zone, stored, today()) as? PlaceWarningState.Current ?: return null
        return current.warnings.takeIf { it.maxLevel != WarningLevel.NONE }
    }

    /**
     * The state of one place, pure: everything that varies is a parameter, so the four
     * answers are a table in `OfficialWarningReaderTest` rather than a screen somebody
     * has to reproduce a day boundary on.
     */
    fun state(zone: WarningZone?, stored: StoredBulletin?, today: LocalDate): PlaceWarningState {
        if (zone == null) return PlaceWarningState.Unavailable
        val bulletin = stored?.bulletin ?: return PlaceWarningState.Waiting(zone)
        val warnings = OfficialWarningEngine.forPlace(bulletin, zone, today)
            ?: return PlaceWarningState.Stale(zone, bulletin.issuedAt)
        return PlaceWarningState.Current(warnings)
    }
}
