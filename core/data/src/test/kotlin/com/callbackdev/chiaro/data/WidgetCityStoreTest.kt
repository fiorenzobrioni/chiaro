package com.callbackdev.chiaro.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.callbackdev.chiaro.domain.model.City
import com.callbackdev.chiaro.domain.model.Coordinates
import com.callbackdev.chiaro.domain.model.GpsCityId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class WidgetCityStoreTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Arbitrary Open-Meteo geocoding ids; only their identity matters here
    private val turinId = 3_165_524L
    private val romeId = 3_169_070L

    private fun store(): WidgetCityStore = WidgetCityStore(
        PreferenceDataStoreFactory.create(scope = scope) {
            tmp.newFile("widget-cities-${System.nanoTime()}.preferences_pb")
        }
    )

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun `an empty store pins nothing, so every widget follows the app`() = runBlocking {
        assertTrue(store().current().isEmpty())
    }

    @Test
    fun `pin maps the widget to the city and pinning again overwrites`() = runBlocking {
        val store = store()
        store.pin(appWidgetId = 42, cityId = turinId)
        assertEquals(mapOf(42 to turinId), store.current())

        // Reconfiguring a placed widget must replace the pin, not add a second one
        store.pin(appWidgetId = 42, cityId = romeId)
        assertEquals(mapOf(42 to romeId), store.current())
    }

    @Test
    fun `widgets are pinned independently, gps included`() = runBlocking {
        val store = store()
        store.pin(1, turinId)
        store.pin(2, romeId)
        store.pin(3, GpsCityId) // the GPS source is a legal pin, not a sentinel for "unpinned"
        assertEquals(mapOf(1 to turinId, 2 to romeId, 3 to GpsCityId), store.current())
    }

    private fun city(id: Long, name: String) = City(
        id = id, name = name, region = null, country = null,
        coordinates = Coordinates(45.0, 9.0), timezone = null
    )

    /**
     * The position is a place a widget can watch while the app looks at another (14 nov
     * 2026): the settings offered the saved list only, and the sentinel the store has always
     * accepted resolved to nothing, so the first place of the list was out of reach.
     */
    @Test
    fun `a pin on the position resolves to the fix while the position is on`() {
        val turin = city(turinId, "Torino")
        val here = city(GpsCityId, "Cavenago di Brianza")
        val on = LocationSettings(useGps = true, gpsCity = here, fixedAt = null)
        assertEquals(here, WidgetCityStore.resolve(GpsCityId, listOf(turin), on))
        assertEquals(turin, WidgetCityStore.resolve(turinId, listOf(turin), on))
        // Off, or with no fix yet, the pin names nothing and the card follows the app.
        assertNull(WidgetCityStore.resolve(GpsCityId, listOf(turin), on.copy(useGps = false)))
        assertNull(WidgetCityStore.resolve(GpsCityId, listOf(turin), on.copy(gpsCity = null)))
        // A removed city is the same answer.
        assertNull(WidgetCityStore.resolve(romeId, listOf(turin), on))
    }

    @Test
    fun `unpin only clears the widget it names`() = runBlocking {
        val store = store()
        store.pin(1, turinId)
        store.pin(2, romeId)
        store.unpin(1)
        assertEquals(mapOf(2 to romeId), store.current())
    }

    @Test
    fun `forget clears every deleted widget in one edit and spares the rest`() = runBlocking {
        val store = store()
        store.pin(1, turinId)
        store.pin(2, romeId)
        store.pin(3, GpsCityId)
        store.forget(intArrayOf(1, 3))
        assertEquals(mapOf(2 to romeId), store.current())
    }

    @Test
    fun `forget ignores ids that were never pinned`() = runBlocking {
        // onDeleted also fires for widgets added without configuring, which have no entry
        val store = store()
        store.pin(1, turinId)
        store.forget(intArrayOf(7, 8, 9))
        assertEquals(mapOf(1 to turinId), store.current())
    }

    @Test
    fun `forget takes the sky line with the widget`() = runBlocking {
        // The sky flag arrived after `forget` was written (upstream Fase 16e) and was not added
        // to it, so every removed widget left one behind — and a new widget handed that
        // id inherited a line nobody asked for.
        val store = store()
        store.pin(1, turinId)
        store.setSkyLine(1, enabled = true)
        store.setSkyLine(2, enabled = true)

        store.forget(intArrayOf(1))

        assertEquals(setOf(2), store.currentSkyLine())
    }

    @Test
    fun `a restore moves the pin and the sky line to the new ids, overlaps included`() = runBlocking {
        // 1→2 while 2→3: moving key by key would let the first move overwrite the second's
        // value before it was read.
        val store = store()
        store.pin(1, turinId)
        store.pin(2, romeId)
        store.setSkyLine(1, enabled = true)

        store.remap(oldIds = intArrayOf(1, 2), newIds = intArrayOf(2, 3))

        assertEquals(mapOf(2 to turinId, 3 to romeId), store.current())
        assertEquals(setOf(2), store.currentSkyLine())
    }

    @Test
    fun `a restore leaves nothing under an old id`() = runBlocking {
        val store = store()
        store.pin(5, turinId)
        store.setSkyLine(5, enabled = true)

        store.remap(oldIds = intArrayOf(5), newIds = intArrayOf(9))

        assertEquals(mapOf(9 to turinId), store.current())
        assertEquals(setOf(9), store.currentSkyLine())
    }

    @Test
    fun `the pinned flow reports the map after a pin`() = runBlocking {
        // The updater observes `pinned` rather than polling, so the edit must reach the flow
        val store = store()
        store.pin(11, turinId)
        assertEquals(mapOf(11 to turinId), store.pinned.first())
    }
}
