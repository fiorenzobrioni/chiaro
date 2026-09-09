package com.callbackdev.chiaro.data.warnings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.callbackdev.chiaro.domain.warnings.WarningBulletin
import com.callbackdev.chiaro.domain.warnings.WarningHazard
import com.callbackdev.chiaro.domain.warnings.WarningLevel
import com.callbackdev.chiaro.domain.warnings.ZoneWarning
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class OfficialWarningStoreTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private fun store() = OfficialWarningStore(
        PreferenceDataStoreFactory.create(scope = scope) {
            tmp.newFile("warnings-${System.nanoTime()}.preferences_pb")
        },
        Json { ignoreUnknownKeys = true }
    )

    @After
    fun tearDown() {
        scope.cancel()
    }

    private val sept8: LocalDate = LocalDate.of(2026, 9, 8)

    private val bulletin = WarningBulletin(
        id = "DPC_BULLETIN_2026_09_08_6471",
        issuedAt = sept8.atTime(15, 19),
        days = listOf(sept8, sept8.plusDays(1)),
        note = "Per la giornata di oggi: Regione Lombardia.",
        warnings = listOf(
            ZoneWarning("Cala-5", sept8, WarningHazard.HYDRAULIC, WarningLevel.YELLOW),
            ZoneWarning("Lomb-09", sept8.plusDays(1), WarningHazard.THUNDERSTORM, WarningLevel.ORANGE)
        )
    )

    @Test
    fun `a fresh store holds nothing for a source`() = runBlocking {
        val state = store().state("dpc").first()
        assertNull(state.current)
        assertNull(state.feedTag)
        assertNull(state.lastAttempt)
        assertNull(state.failureLoggedOn)
        assertEquals(emptySet<String>(), store().notified.first())
    }

    @Test
    fun `a bulletin round-trips whole, dates and rows included`() = runBlocking {
        val store = store()
        store.setBulletin(StoredBulletin("dpc", "20260908_1519", bulletin))
        val held = store.current("dpc").first()!!
        assertEquals("dpc", held.sourceId)
        assertEquals("20260908_1519", held.stamp)
        assertEquals(bulletin, held.bulletin)
        // Another source's slot is its own.
        assertNull(store.current("meteoalarm").first())
    }

    @Test
    fun `the feed tag is remembered and can be forgotten`() = runBlocking {
        val store = store()
        store.setFeedTag("dpc", "\"13c215c8\"")
        assertEquals("\"13c215c8\"", store.state("dpc").first().feedTag)
        store.setFeedTag("dpc", null)
        assertNull(store.state("dpc").first().feedTag)
    }

    @Test
    fun `attempts and the one failure row a day are dated`() = runBlocking {
        val store = store()
        val at = Instant.parse("2026-09-08T14:00:00Z")
        store.markAttempt("dpc", at)
        store.markFailureLogged("dpc", sept8)
        val state = store.state("dpc").first()
        assertEquals(at, state.lastAttempt)
        assertEquals(sept8, state.failureLoggedOn)
    }

    @Test
    fun `notified fingerprints are a bounded ring, newest first, without duplicates`() = runBlocking {
        val store = store()
        store.recordNotified("a:warn:B1:YELLOW")
        store.recordNotified("a:warn:B1:ORANGE")
        store.recordNotified("a:warn:B1:YELLOW")
        assertEquals(setOf("a:warn:B1:YELLOW", "a:warn:B1:ORANGE"), store.notified.first())

        repeat(45) { store.recordNotified("a:warn:B${it + 2}:YELLOW") }
        val ring = store.notified.first()
        assertEquals(40, ring.size)
        assertTrue("the newest survives", "a:warn:B46:YELLOW" in ring)
        assertTrue("the oldest fell off", "a:warn:B1:ORANGE" !in ring)
    }
}
