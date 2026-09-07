package com.callbackdev.chiaro.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** The `recent_searches` array behind the Search screen. */
class SearchHistoryStoreTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private fun store(): SearchHistoryStore = SearchHistoryStore(
        PreferenceDataStoreFactory.create(scope = scope) {
            tmp.newFile("search-history-${System.nanoTime()}.preferences_pb")
        },
        Json
    )

    @After
    fun tearDown() {
        scope.cancel()
    }

    private suspend fun SearchHistoryStore.entries() = recentSearches.first()

    @Test
    fun `the newest search comes first`() = runBlocking {
        val store = store()
        store.add("Milano")
        store.add("Torino")

        assertEquals(listOf("Torino", "Milano"), store.entries())
    }

    @Test
    fun `searching the same place again moves it up instead of duplicating it`() = runBlocking {
        val store = store()
        store.add("Milano")
        store.add("Torino")
        store.add("milano") // same place, different casing

        assertEquals(listOf("milano", "Torino"), store.entries())
    }

    @Test
    fun `the list stops at five and drops the oldest`() = runBlocking {
        val store = store()
        val searched = (1..SearchHistoryStore.MAX_ENTRIES + 2).map { "City $it" }
        searched.forEach { store.add(it) }

        assertEquals(SearchHistoryStore.MAX_ENTRIES, store.entries().size)
        assertEquals(searched.takeLast(SearchHistoryStore.MAX_ENTRIES).reversed(), store.entries())
    }

    @Test
    fun `blank terms are not history`() = runBlocking {
        val store = store()
        store.add("   ")

        assertEquals(emptyList<String>(), store.entries())
    }

    @Test
    fun `clear forgets every search`() = runBlocking {
        val store = store()
        store.add("Milano")
        store.add("Torino")

        store.clear()

        assertEquals(emptyList<String>(), store.entries())
    }

    @Test
    fun `remove forgets one search and leaves the others where they were`() = runBlocking {
        val store = store()
        store.add("Milano")
        store.add("Torino")
        store.add("Genova")

        store.remove("Torino")

        assertEquals(listOf("Genova", "Milano"), store.entries())
    }

    @Test
    fun `remove matches the way add deduplicates, ignoring case and spaces`() = runBlocking {
        val store = store()
        store.add("Milano")
        store.add("Torino")

        store.remove("  milano  ")

        assertEquals(listOf("Torino"), store.entries())
    }

    @Test
    fun `removing something that is not there changes nothing`() = runBlocking {
        val store = store()
        store.add("Milano")

        store.remove("Torino")
        store.remove("   ")

        assertEquals(listOf("Milano"), store.entries())
    }

    @Test
    fun `restore puts the list back in the order it was given`() = runBlocking {
        val store = store()
        store.add("Milano")
        store.add("Torino")
        store.add("Genova")
        val before = store.entries()

        store.clear()
        store.restore(before)

        // and not "Milano, Torino, Genova": replaying add would call the oldest
        // search the newest one
        assertEquals(before, store.entries())
    }

    @Test
    fun `restoring an empty list is the same as forgetting everything`() = runBlocking {
        val store = store()
        store.add("Milano")

        store.restore(emptyList())

        assertEquals(emptyList<String>(), store.entries())
    }

    @Test
    fun `restore keeps the cap`() = runBlocking {
        val store = store()
        val many = (1..SearchHistoryStore.MAX_ENTRIES + 3).map { "City $it" }

        store.restore(many)

        assertEquals(many.take(SearchHistoryStore.MAX_ENTRIES), store.entries())
    }
}
