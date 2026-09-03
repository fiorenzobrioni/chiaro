package com.callbackdev.chiaro.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** New with Fase 7: the Journal reads failures off this, so the shape is pinned. */
class FetchLogStoreTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private fun store(file: File = tmp.newFile("log.preferences_pb")) = FetchLogStore(
        PreferenceDataStoreFactory.create(scope = scope) { file },
        Json
    )

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun `records land newest first with their reason`() = runBlocking {
        val store = store()
        store.record("milano", 1_000, FetchFailureReason.OFFLINE)
        store.record("milano", 2_000, FetchFailureReason.SERVICE)

        val failures = store.failures.first()
        assertEquals(listOf(2_000L, 1_000L), failures.map { it.atEpochSeconds })
        assertEquals(FetchFailureReason.SERVICE, failures.first().reason)
    }

    @Test
    fun `the log is bounded, oldest entries fall off`() = runBlocking {
        val store = store()
        repeat(40) { store.record("milano", it.toLong(), FetchFailureReason.UNKNOWN) }

        val failures = store.failures.first()
        assertEquals(30, failures.size)
        // Newest kept, oldest gone.
        assertEquals(39L, failures.first().atEpochSeconds)
        assertTrue(failures.none { it.atEpochSeconds < 10 })
    }
}
