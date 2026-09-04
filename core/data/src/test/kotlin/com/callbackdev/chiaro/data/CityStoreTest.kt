package com.callbackdev.chiaro.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.callbackdev.chiaro.domain.model.City
import com.callbackdev.chiaro.domain.model.Coordinates
import com.callbackdev.chiaro.domain.model.GeoFix
import com.callbackdev.chiaro.domain.model.GpsCityId
import com.callbackdev.chiaro.domain.model.toGpsCity
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CityStoreTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Distinct from DefaultCity (Milan) so add/setActive really change the store
    private val turin = City(3_165_524, "Turin", "Piedmont", "Italy",
        Coordinates(45.0703, 7.6869), "Europe/Rome")
    private val gpsFix = GeoFix(Coordinates(45.46, 9.19), "Milano", null, "Italy")
    private val gpsCity = gpsFix.toGpsCity()
    private val fixedAt: Instant = Instant.parse("2026-09-04T10:15:00Z")
    private val milan = CityStore.DefaultCity

    private fun store(): CityStore = CityStore(
        PreferenceDataStoreFactory.create(scope = scope) {
            tmp.newFile("cities-${System.nanoTime()}.preferences_pb")
        },
        Json
    )

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun `move reorders without touching the selection`() = runBlocking {
        val store = store()
        store.add(milan)
        store.add(turin) // active is now Turin, list is [Milan, Turin]
        store.move(turin, 0)
        assertEquals(listOf(turin, milan), store.cities.first())
        assertEquals(ActiveSource.Saved(turin), store.activeSource.first())
    }

    @Test
    fun `move clamps an out-of-range index and ignores an unknown city`() = runBlocking {
        val store = store()
        store.add(milan)
        store.add(turin)
        store.move(milan, 99)
        assertEquals(listOf(turin, milan), store.cities.first())
        store.move(gpsCity, 0) // never in the saved list
        assertEquals(listOf(turin, milan), store.cities.first())
    }

    @Test
    fun `insert restores a removed city at its old index without selecting it`() = runBlocking {
        val store = store()
        store.add(milan)
        store.add(turin)
        store.setActive(milan)
        store.remove(milan) // active falls to Turin
        assertEquals(ActiveSource.Saved(turin), store.activeSource.first())
        store.insert(milan, 0) // the undo: back where it was, selection untouched
        assertEquals(listOf(milan, turin), store.cities.first())
        assertEquals(ActiveSource.Saved(turin), store.activeSource.first())
    }

    @Test
    fun `insert is a no-op for a city already saved`() = runBlocking {
        val store = store()
        store.add(milan)
        store.insert(milan, 0)
        assertEquals(listOf(milan), store.cities.first())
    }

    /** Fase 14b: no seeded city any more — "nothing configured" is a real state. */
    @Test
    fun `a fresh store has no location at all`() = runBlocking {
        val store = store()
        assertEquals(ActiveSource.None, store.activeSource.first())
        assertEquals(emptyList<City>(), store.cities.first())
        assertFalse(store.locationSettings.first().useGps)
    }

    @Test
    fun `enabling gps selects it immediately, before any fix exists`() = runBlocking {
        val store = store()
        store.setUseGps(true)
        assertEquals(ActiveSource.Gps(lastFix = null), store.activeSource.first())
        assertTrue(store.locationSettings.first().useGps)
    }

    @Test
    fun `adoptGpsFix persists the fix without touching the saved list`() = runBlocking {
        val store = store()
        store.setUseGps(true)
        store.adoptGpsFix(gpsFix, fixedAt)
        assertEquals(ActiveSource.Gps(gpsCity), store.activeSource.first())
        assertEquals(emptyList<City>(), store.cities.first())
    }

    @Test
    fun `adding a searched city switches away from gps but keeps it enabled`() = runBlocking {
        val store = store()
        store.setUseGps(true)
        store.add(turin)
        assertEquals(ActiveSource.Saved(turin), store.activeSource.first())
        assertTrue(store.locationSettings.first().useGps)
    }

    @Test
    fun `setActiveGps reselects gps only while enabled`() = runBlocking {
        val store = store()
        store.setActiveGps() // toggle off: must be a no-op
        assertEquals(ActiveSource.None, store.activeSource.first())

        store.setUseGps(true)
        store.adoptGpsFix(gpsFix, fixedAt)
        store.add(turin)
        store.setActiveGps()
        assertEquals(ActiveSource.Gps(gpsCity), store.activeSource.first())
    }

    @Test
    fun `disabling gps while active falls back to the first saved city`() = runBlocking {
        val store = store()
        store.add(turin)
        store.setUseGps(true)
        store.setUseGps(false)
        assertEquals(ActiveSource.Saved(turin), store.activeSource.first())
        assertFalse(store.locationSettings.first().useGps)
    }

    /** Nothing to fall back TO is no longer a crash, nor a city out of nowhere. */
    @Test
    fun `disabling gps with an empty list leaves no source at all`() = runBlocking {
        val store = store()
        store.setUseGps(true)
        store.setUseGps(false)
        assertEquals(ActiveSource.None, store.activeSource.first())
    }

    @Test
    fun `the last city can be removed and leaves cities json empty`() = runBlocking {
        val store = store()
        store.add(turin)

        store.remove(turin)

        assertEquals(emptyList<City>(), store.cities.first())
        assertEquals(ActiveSource.None, store.activeSource.first())
    }

    @Test
    fun `gps pseudo-city never appears in cities and cannot be added`() = runBlocking {
        val store = store()
        store.setUseGps(true)
        store.adoptGpsFix(gpsFix, fixedAt)
        assertNull(store.cities.first().find { it.id == GpsCityId })
    }

    /**
     * Fase 14a: same id, fresher record. The seeded Milan is the case that surfaced it
     * — "Milano" searched in Italian is GeoNames 3173435 like the English "Milan", so
     * the old add() skipped it and the file stayed milan.json.
     */
    @Test
    fun `re-adding a saved city refreshes its record in place`() = runBlocking {
        val store = store()
        store.add(milan)
        store.add(turin)
        val italian = milan.copy(name = "Milano", region = "Lombardia", country = "Italia")

        store.add(italian)

        // replaced where it was, not moved to the end: re-adding is not a reorder
        assertEquals(listOf(italian, turin), store.cities.first())
        assertEquals(ActiveSource.Saved(italian), store.activeSource.first())
    }

    // ---- Fase 14b: which installs inherit a city, and which get `tweather init` ----

    @Test
    fun `the shell draws nothing until the legacy check has run`() = runBlocking {
        assertEquals(FirstRun.Unknown, store().firstRun.first())
    }

    @Test
    fun `a fresh install is sent to init with no city`() = runBlocking {
        val store = store()

        store.migrateFirstRun(hasHistory = false)

        assertEquals(FirstRun.Pending, store.firstRun.first())
        assertEquals(emptyList<City>(), store.cities.first())
    }

    /**
     * The install that never touched cities.json but has been watching the seeded
     * Milan for months: it must keep it, and must not be asked to configure anything.
     */
    @Test
    fun `an install that has been fetching keeps the city it was watching`() = runBlocking {
        val store = store()

        store.migrateFirstRun(hasHistory = true)

        assertEquals(FirstRun.Done, store.firstRun.first())
        assertEquals(listOf(CityStore.DefaultCity), store.cities.first())
        assertEquals(ActiveSource.Saved(CityStore.DefaultCity), store.activeSource.first())
    }

    @Test
    fun `an install with its own list keeps it and skips init`() = runBlocking {
        val store = store()
        store.add(turin)

        store.migrateFirstRun(hasHistory = false)

        assertEquals(FirstRun.Done, store.firstRun.first())
        assertEquals(listOf(turin), store.cities.first())
    }

    /** Once decided, never revisited: a later fetch must not re-seed a skipped install. */
    @Test
    fun `the legacy check runs exactly once`() = runBlocking {
        val store = store()
        store.migrateFirstRun(hasHistory = false)

        store.migrateFirstRun(hasHistory = true)

        assertEquals(FirstRun.Pending, store.firstRun.first())
        assertEquals(emptyList<City>(), store.cities.first())
    }

    @Test
    fun `skipping init still counts as answering it`() = runBlocking {
        val store = store()
        store.migrateFirstRun(hasHistory = false)

        store.markInitDone()

        assertEquals(FirstRun.Done, store.firstRun.first())
        assertEquals(ActiveSource.None, store.activeSource.first())
    }

    /**
     * Fase 3b. The old guard here was a runtime `require` that the city handed in
     * carried the GPS id; the signature takes a [GeoFix] now, so a regular city is
     * not something a caller can express and the check has nothing left to catch.
     * What replaces it are the three tests below, which cover the rule that actually
     * decides what gets stored.
     */
    @Test
    fun `a fix under the adoption distance keeps the place and takes only its name`() =
        runBlocking {
            val store = store()
            store.adoptGpsFix(gpsFix, fixedAt)

            // ~800 m north: a different cacheKey cell, the same town.
            val nearby = GeoFix(Coordinates(45.467, 9.19), "Milano Centro", "Lombardia", "Italy")
            val adopted = store.adoptGpsFix(nearby, fixedAt.plusSeconds(600))

            assertEquals(gpsCity.coordinates, adopted.coordinates)
            assertEquals(gpsCity.cacheKey, adopted.cacheKey)
            assertEquals("Milano Centro", adopted.name)
            assertEquals("Lombardia", adopted.region)
        }

    @Test
    fun `a fix past the adoption distance is a new place`() = runBlocking {
        val store = store()
        store.adoptGpsFix(gpsFix, fixedAt)

        // Turin: unambiguously somewhere else.
        val far = GeoFix(Coordinates(45.07, 7.69), "Torino", "Piemonte", "Italy")
        val adopted = store.adoptGpsFix(far, fixedAt.plusSeconds(600))

        assertEquals(Coordinates(45.07, 7.69), adopted.coordinates)
        assertEquals("Torino", adopted.name)
    }

    /** A geocode that failed must not overwrite a name that worked: its own name is
     * the coordinate label of a position we are NOT adopting. */
    @Test
    fun `a nameless fix nearby leaves the stored name alone`() = runBlocking {
        val store = store()
        store.adoptGpsFix(gpsFix, fixedAt)

        val nameless = GeoFix(Coordinates(45.467, 9.19), null, null, null)
        val adopted = store.adoptGpsFix(nameless, fixedAt.plusSeconds(600))

        assertEquals("Milano", adopted.name)
        assertEquals("Italy", adopted.country)
    }

    /** The instant is what makes a cold start free, so it is written on every fix —
     * including the ones that changed nothing about the place. */
    @Test
    fun `every fix records when it was taken`() = runBlocking {
        val store = store()
        store.adoptGpsFix(gpsFix, fixedAt)
        assertEquals(fixedAt, store.locationSettings.first().fixedAt)

        val later = fixedAt.plusSeconds(3_600)
        store.adoptGpsFix(gpsFix, later)
        assertEquals(later, store.locationSettings.first().fixedAt)
    }

    @Test
    fun `no fix taken yet has no instant`() = runBlocking {
        assertNull(store().locationSettings.first().fixedAt)
    }
}
