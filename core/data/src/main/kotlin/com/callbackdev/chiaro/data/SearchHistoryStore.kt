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
import kotlinx.serialization.json.Json

private val Context.searchHistoryDataStore by preferencesDataStore(name = "search_history")

/**
 * The `recent_searches` array of the Search screen: most recent first, deduplicated,
 * capped at [MAX_ENTRIES]. Stored as a JSON string array in DataStore.
 */
class SearchHistoryStore(
    private val dataStore: DataStore<Preferences>,
    private val json: Json
) {

    val recentSearches: Flow<List<String>> = dataStore.data
        .map { prefs -> prefs.recents() }
        .distinctUntilChanged()

    private fun Preferences.recents(): List<String> =
        this[RecentsJson]
            ?.let { runCatching { json.decodeFromString<List<String>>(it) }.getOrNull() }
            .orEmpty()

    suspend fun add(term: String) {
        val clean = term.trim()
        if (clean.isEmpty()) return
        dataStore.edit { prefs ->
            val kept = prefs.recents().filterNot { it.equals(clean, ignoreCase = true) }
            prefs[RecentsJson] = json.encodeToString((listOf(clean) + kept).take(MAX_ENTRIES))
        }
    }

    /**
     * Forgets one term, matched the way [add] deduplicates it — the reader who taps the
     * cross beside "milano" means the entry they are looking at, whatever casing it was
     * typed in. Removing something that is not there is not an error, it is a no-op.
     */
    suspend fun remove(term: String) {
        val clean = term.trim()
        if (clean.isEmpty()) return
        dataStore.edit { prefs ->
            val kept = prefs.recents().filterNot { it.equals(clean, ignoreCase = true) }
            prefs[RecentsJson] = json.encodeToString(kept)
        }
    }

    /**
     * Forgets what was searched for. Deliberately narrow: the saved cities are not
     * history, and live in [CityStore] where they are removed one by one.
     */
    suspend fun clear() {
        dataStore.edit { it.remove(RecentsJson) }
    }

    /**
     * Puts back a list that was just taken away — the undo behind [remove] and [clear].
     * It writes the order it is given instead of replaying [add], because this list is
     * ordered by *when* each term was searched: re-adding would file yesterday's search
     * as the newest one, which is a claim the store would have made up.
     */
    suspend fun restore(terms: List<String>) {
        val clean = terms.map { it.trim() }.filter { it.isNotEmpty() }.take(MAX_ENTRIES)
        dataStore.edit { prefs ->
            if (clean.isEmpty()) prefs.remove(RecentsJson)
            else prefs[RecentsJson] = json.encodeToString(clean)
        }
    }

    companion object {
        const val MAX_ENTRIES = 5
        private val RecentsJson = stringPreferencesKey("recent_searches_json")

        fun create(context: Context, json: Json) =
            SearchHistoryStore(context.searchHistoryDataStore, json)
    }
}
