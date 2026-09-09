package com.callbackdev.chiaro.data.warnings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.callbackdev.chiaro.domain.warnings.WarningBulletin
import com.callbackdev.chiaro.domain.warnings.WarningHazard
import com.callbackdev.chiaro.domain.warnings.WarningLevel
import com.callbackdev.chiaro.domain.warnings.ZoneWarning
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.warningsDataStore by preferencesDataStore(name = "warnings")

/** A bulletin as held: which source, under which stamp, and the document itself. */
data class StoredBulletin(
    val sourceId: String,
    val stamp: String,
    val bulletin: WarningBulletin
)

/** Everything the step needs to know about one source before asking the network. */
data class WarningSourceState(
    val current: StoredBulletin?,
    /** The discovery feed's ETag as last seen, for a free re-check. */
    val feedTag: String?,
    /** When the network was last asked, success or failure. */
    val lastAttempt: Instant?,
    /** The last day a «bollettino non raggiunto» row was written: one a day at most. */
    val failureLoggedOn: LocalDate?
)

/**
 * The official warnings' own DataStore (Fase 11): the current bulletin per source as
 * JSON, the fetch bookkeeping, and the ring of notified fingerprints. Its own file
 * and never `settings`, for the reason [com.callbackdev.chiaro.data.AlertStateStore]
 * gives: this is engine bookkeeping, not something the reader edited. A bulletin is
 * a document with a validity window that outlives any fetch and must be shown on
 * days nothing was fetched, which is why it lives here and not in a history row.
 */
class OfficialWarningStore(
    private val dataStore: DataStore<Preferences>,
    private val json: Json
) {

    fun state(sourceId: String): Flow<WarningSourceState> = dataStore.data
        .map { prefs ->
            WarningSourceState(
                current = prefs[bulletinKey(sourceId)]?.let { decode(it) },
                feedTag = prefs[feedTagKey(sourceId)],
                lastAttempt = prefs[lastAttemptKey(sourceId)]?.let(Instant::ofEpochMilli),
                failureLoggedOn = prefs[failureLoggedKey(sourceId)]?.let {
                    runCatching { LocalDate.parse(it) }.getOrNull()
                }
            )
        }
        .distinctUntilChanged()

    /** The bulletin in hand for [sourceId], for the surfaces that only need that. */
    fun current(sourceId: String): Flow<StoredBulletin?> = state(sourceId).map { it.current }

    val notified: Flow<Set<String>> = dataStore.data
        .map { it[Notified].toFingerprints() }
        .distinctUntilChanged()

    suspend fun setBulletin(stored: StoredBulletin) {
        dataStore.edit { prefs ->
            prefs[bulletinKey(stored.sourceId)] = json.encodeToString(stored.toDto())
        }
    }

    /** Remembers or forgets the feed's tag; null makes the next look a full one. */
    suspend fun setFeedTag(sourceId: String, tag: String?) {
        dataStore.edit { prefs ->
            if (tag == null) prefs.remove(feedTagKey(sourceId)) else prefs[feedTagKey(sourceId)] = tag
        }
    }

    suspend fun markAttempt(sourceId: String, at: Instant) {
        dataStore.edit { it[lastAttemptKey(sourceId)] = at.toEpochMilli() }
    }

    suspend fun markFailureLogged(sourceId: String, day: LocalDate) {
        dataStore.edit { it[failureLoggedKey(sourceId)] = day.toString() }
    }

    /** Called only after a successful post — an unposted warning can still fire. */
    suspend fun recordNotified(fingerprint: String) {
        dataStore.edit { prefs ->
            prefs[Notified] = (listOf(fingerprint) + prefs[Notified].toFingerprints())
                .distinct()
                .take(MAX_FINGERPRINTS)
                .joinToString(SEPARATOR)
        }
    }

    private fun decode(raw: String): StoredBulletin? =
        runCatching { json.decodeFromString<StoredBulletinDto>(raw).toStored() }.getOrNull()

    private fun String?.toFingerprints(): Set<String> =
        this?.split(SEPARATOR)?.filter { it.isNotBlank() }?.toSet() ?: emptySet()

    companion object {
        private fun bulletinKey(sourceId: String) = stringPreferencesKey("bulletin_$sourceId")
        private fun feedTagKey(sourceId: String) = stringPreferencesKey("feed_tag_$sourceId")
        private fun lastAttemptKey(sourceId: String) = longPreferencesKey("last_attempt_$sourceId")
        private fun failureLoggedKey(sourceId: String) = stringPreferencesKey("failure_logged_$sourceId")
        private val Notified = stringPreferencesKey("notified_fingerprints")

        /** Fingerprints carry the bulletin id and the level; a bulletin a day, a few
         * places, a few levels: forty covers weeks. */
        private const val MAX_FINGERPRINTS = 40

        /** Never appears in a fingerprint (city keys, bulletin ids, level names). */
        private const val SEPARATOR = "|"

        fun create(context: Context, json: Json) = OfficialWarningStore(context.warningsDataStore, json)
    }
}

// --- the wire shape: dates as ISO strings, nothing the domain has to know about -----

@Serializable
private data class StoredBulletinDto(
    val sourceId: String,
    val stamp: String,
    val id: String,
    val issuedAt: String,
    val days: List<String>,
    val note: String? = null,
    val warnings: List<StoredRowDto>
)

@Serializable
private data class StoredRowDto(
    val zone: String,
    val day: String,
    val hazard: String,
    val level: String
)

private fun StoredBulletin.toDto() = StoredBulletinDto(
    sourceId = sourceId,
    stamp = stamp,
    id = bulletin.id,
    issuedAt = bulletin.issuedAt.toString(),
    days = bulletin.days.map { it.toString() },
    note = bulletin.note,
    warnings = bulletin.warnings.map {
        StoredRowDto(it.zoneCode, it.day.toString(), it.hazard.name, it.level.name)
    }
)

private fun StoredBulletinDto.toStored() = StoredBulletin(
    sourceId = sourceId,
    stamp = stamp,
    bulletin = WarningBulletin(
        id = id,
        issuedAt = LocalDateTime.parse(issuedAt),
        days = days.map(LocalDate::parse),
        note = note,
        // A hazard or level this build does not know (a future issuer's) is dropped,
        // not guessed: the row is silence, which is what the model calls NONE.
        warnings = warnings.mapNotNull { row ->
            val hazard = WarningHazard.entries.firstOrNull { it.name == row.hazard } ?: return@mapNotNull null
            val level = WarningLevel.entries.firstOrNull { it.name == row.level } ?: return@mapNotNull null
            ZoneWarning(row.zone, LocalDate.parse(row.day), hazard, level)
        }
    )
)
