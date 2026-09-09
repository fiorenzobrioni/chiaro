package com.callbackdev.chiaro.data.warnings

import com.callbackdev.chiaro.domain.warnings.WarningBulletin
import com.callbackdev.chiaro.domain.warnings.WarningHazard
import com.callbackdev.chiaro.domain.warnings.WarningLevel
import com.callbackdev.chiaro.domain.warnings.WarningZone
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.callbackdev.chiaro.domain.warnings.ZoneWarning
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * The four answers a screen may give, as a table. They are the reason
 * [PlaceWarningState] exists at all: Avvisi prints every one of them and Today prints
 * only one, and neither should have to reproduce a day boundary to work out which.
 */
class OfficialWarningReaderTest {

    private val zone = WarningZone("Lomb-09", "Nodo Idraulico di Milano", "Lombardia")
    private val today = LocalDate.of(2026, 9, 9)

    @get:Rule
    val tmp = TemporaryFolder()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /** The store is real but never read here: every rule under test is pure, and the
     * index supplier throws to prove the table never needs the 290 KB asset. */
    private val reader by lazy {
        OfficialWarningReader(
            store = OfficialWarningStore(
                PreferenceDataStoreFactory.create(scope = scope) {
                    tmp.newFile("warnings-${System.nanoTime()}.preferences_pb")
                },
                Json
            ),
            index = { error("the table never asks for the index") }
        )
    }

    @After
    fun tearDown() {
        scope.cancel()
    }

    private fun stored(
        days: List<LocalDate> = listOf(today, today.plusDays(1)),
        rows: List<ZoneWarning> = emptyList()
    ) = StoredBulletin(
        sourceId = "dpc",
        stamp = "20260909_1546",
        bulletin = WarningBulletin(
            id = "DPC_BULLETIN_2026_09_09_6506",
            issuedAt = today.atTime(15, 46),
            days = days,
            note = null,
            warnings = rows
        )
    )

    @Test
    fun `a place in no zone is unavailable, whatever is in hand`() {
        assertEquals(PlaceWarningState.Unavailable, reader.state(null, stored(), today))
        assertEquals(PlaceWarningState.Unavailable, reader.state(null, null, today))
    }

    @Test
    fun `a zone with no bulletin yet is waiting, not empty`() {
        assertEquals(PlaceWarningState.Waiting(zone), reader.state(zone, null, today))
    }

    @Test
    fun `a bulletin whose days are over states its date rather than its grades`() {
        val old = stored(days = listOf(today.minusDays(2), today.minusDays(1)))
        val state = reader.state(zone, old, today)
        assertTrue(state is PlaceWarningState.Stale)
        assertEquals(today.atTime(15, 46), (state as PlaceWarningState.Stale).issuedAt)
    }

    /** Before the afternoon's bulletin, yesterday's "tomorrow" IS today and is current. */
    @Test
    fun `yesterday's bulletin still covering today is current`() {
        val yesterdays = stored(days = listOf(today.minusDays(1), today))
        val state = reader.state(zone, yesterdays, today)
        assertTrue(state is PlaceWarningState.Current)
        assertEquals(listOf(today), (state as PlaceWarningState.Current).warnings.days.map { it.date })
    }

    /** All green is an ANSWER, not an absence: Avvisi prints "Nessuna allerta" from it. */
    @Test
    fun `a green bulletin is current with nothing above NONE`() {
        val state = reader.state(zone, stored(), today) as PlaceWarningState.Current
        assertEquals(WarningLevel.NONE, state.warnings.maxLevel)
        assertEquals(2, state.warnings.days.size)
    }

    @Test
    fun `a graded bulletin comes back with its grades`() {
        val graded = stored(
            rows = listOf(
                ZoneWarning(zone.code, today, WarningHazard.THUNDERSTORM, WarningLevel.ORANGE),
                ZoneWarning("Lomb-01", today, WarningHazard.HYDRAULIC, WarningLevel.RED)
            )
        )
        val state = reader.state(zone, graded, today) as PlaceWarningState.Current
        // The other zone's red is not this place's business.
        assertEquals(WarningLevel.ORANGE, state.warnings.maxLevel)
    }
}
