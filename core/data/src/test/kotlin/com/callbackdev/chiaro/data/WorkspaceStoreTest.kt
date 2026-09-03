package com.callbackdev.chiaro.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class WorkspaceStoreTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private fun store(file: File) = WorkspaceStore(
        PreferenceDataStoreFactory.create(scope = scope) { file }
    )

    @After
    fun tearDown() {
        scope.cancel()
    }

    /**
     * The guide card is session state on purpose — a settings reset must not bring
     * it back to someone who read the guide long ago (VISION §5.7).
     */
    @Test
    fun `the guide card shows until it is dismissed`() = runBlocking {
        val store = store(tmp.newFile("ws.preferences_pb"))
        assertEquals(false, store.guideCardDismissed.first())

        store.dismissGuideCard()

        assertEquals(true, store.guideCardDismissed.first())
    }

    @Test
    fun `the dismissal survives a restart (new store on the same file)`() = runBlocking {
        val file = tmp.newFile("ws.preferences_pb")
        // DataStore allows one active instance per file: the first scope must be
        // FULLY torn down (cancelAndJoin, cancel alone is async) to simulate
        // process death before the "restarted" instance opens the same file
        val firstRunJob = SupervisorJob()
        val firstRun = CoroutineScope(Dispatchers.IO + firstRunJob)
        WorkspaceStore(PreferenceDataStoreFactory.create(scope = firstRun) { file })
            .dismissGuideCard()
        firstRunJob.cancelAndJoin()
        assertEquals(true, store(file).guideCardDismissed.first())
    }
}
