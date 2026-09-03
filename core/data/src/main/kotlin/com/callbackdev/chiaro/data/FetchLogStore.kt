package com.callbackdev.chiaro.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val Context.fetchLogDataStore by preferencesDataStore(name = "fetch_log")

/** Why an update did not land — the same three words the Today screen already uses. */
@Serializable
enum class FetchFailureReason { OFFLINE, SERVICE, UNKNOWN }

/** One update that did not make it: when, for which place, and why. */
@Serializable
data class FetchFailure(
    val cityKey: String,
    val atEpochSeconds: Long,
    val reason: FetchFailureReason
)

/**
 * The updates that FAILED (Fase 7). The history table records what the app learned;
 * this small ring records what it could not learn — the Journal is where offline
 * honesty lives (VISION §5.5), and a fetch that silently vanished would be a gap
 * posing as a quiet day. Chiaro-only: upstream's Logs render commits, and a commit
 * that never happened has nothing to render there.
 *
 * Bounded and newest-first, like the fingerprint stores: a log that only grows is a
 * slow leak that never announces itself.
 */
class FetchLogStore(
    private val dataStore: DataStore<Preferences>,
    private val json: Json
) {

    val failures: Flow<List<FetchFailure>> = dataStore.data
        .map(::decode)
        .distinctUntilChanged()

    suspend fun record(cityKey: String, atEpochSeconds: Long, reason: FetchFailureReason) {
        dataStore.edit { prefs ->
            val next = listOf(FetchFailure(cityKey, atEpochSeconds, reason)) + decode(prefs)
            prefs[FailuresJson] = json.encodeToString(next.take(MAX_ENTRIES))
        }
    }

    private fun decode(prefs: Preferences): List<FetchFailure> =
        prefs[FailuresJson]
            ?.let { runCatching { json.decodeFromString<List<FetchFailure>>(it) }.getOrNull() }
            ?: emptyList()

    companion object {
        private val FailuresJson = stringPreferencesKey("failures_json")

        /** A few days of trouble at the worst polling rate; the Journal needs no more. */
        private const val MAX_ENTRIES = 30

        fun create(context: Context, json: Json) = FetchLogStore(context.fetchLogDataStore, json)
    }
}
