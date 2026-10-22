package com.callbackdev.chiaro.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.callbackdev.chiaro.domain.WeatherException
import com.callbackdev.chiaro.domain.model.City
import com.callbackdev.chiaro.domain.model.Coordinates
import com.callbackdev.chiaro.domain.model.GeoFix
import com.callbackdev.chiaro.domain.model.isUnnamed
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

/**
 * A fix that moves the reader moves the position page only with the new place's weather
 * in hand (committente, 20 ott 2026): offline, the page stays on the place it has weather
 * for, and the reached place waits until its forecast can be had.
 */
class GpsArrivalTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val cavenago = GeoFix(Coordinates(45.58, 9.41), "Cavenago di Brianza", null, "Italy")
    // Milan's Lambrate, 17 km south-west: well past the two kilometres.
    private val lambrate = GeoFix(Coordinates(45.48, 9.24), null, null, null)
    // A walk across Cavenago: under them.
    private val nextStreet = GeoFix(Coordinates(45.585, 9.41), "Cavenago di Brianza", null, "Italy")
    private val at: Instant = Instant.parse("2026-10-20T08:00:00Z")

    private var online = false
    private val withWeather = mutableSetOf<String>()
    private val fetched = mutableListOf<String>()

    private val store = CityStore(
        PreferenceDataStoreFactory.create(scope = scope) { tmp.newFile("cities.preferences_pb") },
        Json
    )
    private val arrival = GpsArrival(
        store,
        hasWeather = { it.cacheKey in withWeather },
        fetchWeather = {
            fetched += it.cacheKey
            if (!online) throw WeatherException.NoNetwork()
            withWeather += it.cacheKey
        }
    )

    /** The same roads with a geocoder that answers: Lambrate is Milan's. */
    private val naming = GpsArrival(
        store,
        hasWeather = { it.cacheKey in withWeather },
        fetchWeather = {
            fetched += it.cacheKey
            if (!online) throw WeatherException.NoNetwork()
            withWeather += it.cacheKey
        },
        placeOf = { GeocodedPlace("Milano", "Lombardia", "Italy", "IT", "Milano") }
    )

    @After
    fun tearDown() {
        scope.cancel()
    }

    /** The reader at Cavenago, its weather on disk. */
    private suspend fun atCavenago(): City {
        val shown = store.adoptGpsFix(cavenago, at)
        withWeather += shown.cacheKey
        return shown
    }

    @Test
    fun `offline, a move keeps the page on the place it has weather for`() = runBlocking {
        val shown = atCavenago()

        val now = arrival.take(lambrate, at.plusSeconds(3_600))

        assertEquals(shown, now)
        val settings = store.locationSettings.first()
        assertEquals(shown, settings.gpsCity)
        assertEquals(lambrate.toGpsCity(), settings.arrival)
        // A fix was taken: the interval counts from it, held or not.
        assertEquals(at.plusSeconds(3_600), settings.fixedAt)
    }

    @Test
    fun `online, a move goes to the new place with its weather already fetched`() = runBlocking {
        atCavenago()
        online = true

        val now = arrival.take(lambrate, at.plusSeconds(3_600))

        assertEquals(lambrate.coordinates, now.coordinates)
        assertTrue(now.cacheKey in withWeather)
        assertNull(store.locationSettings.first().arrival)
    }

    @Test
    fun `a fix under the adoption distance asks for nothing`() = runBlocking {
        val shown = atCavenago()

        arrival.take(nextStreet, at.plusSeconds(600))

        assertEquals(emptyList<String>(), fetched)
        assertEquals(shown.cacheKey, store.locationSettings.first().gpsCity?.cacheKey)
    }

    @Test
    fun `with no weather to stay on, the new place is taken as before`() = runBlocking {
        store.adoptGpsFix(cavenago, at) // a fix, but nothing ever fetched for it

        val now = arrival.take(lambrate, at.plusSeconds(3_600))

        assertEquals(lambrate.coordinates, now.coordinates)
        assertEquals(emptyList<String>(), fetched)
    }

    @Test
    fun `the waiting place is adopted once its weather lands`() = runBlocking {
        atCavenago()
        arrival.take(lambrate, at.plusSeconds(3_600))

        assertFalse("still offline", arrival.retry())
        online = true
        assertTrue(arrival.retry())

        val settings = store.locationSettings.first()
        assertEquals(lambrate.coordinates, settings.gpsCity?.coordinates)
        assertNull(settings.arrival)
        assertFalse("nothing waits any more", arrival.retry())
    }

    @Test
    fun `coming back where the weather is answers the waiting place`() = runBlocking {
        val shown = atCavenago()
        arrival.take(lambrate, at.plusSeconds(3_600))

        arrival.take(nextStreet, at.plusSeconds(7_200))

        val settings = store.locationSettings.first()
        assertEquals(shown.cacheKey, settings.gpsCity?.cacheKey)
        assertNull(settings.arrival)
    }

    @Test
    fun `turning the position off forgets the waiting place`() = runBlocking {
        atCavenago()
        store.setUseGps(true)
        arrival.take(lambrate, at.plusSeconds(3_600))

        store.setUseGps(false)

        assertNull(store.locationSettings.first().arrival)
    }

    /** From the device, 22 ott 2026: a move made offline reached the page as «45.48N 9.30E»
     * and kept the coordinates until the next fix. The retry is the first moment online. */
    @Test
    fun `a place reached offline takes its name when the geocoder answers on the retry`() = runBlocking {
        atCavenago()
        arrival.take(lambrate, at.plusSeconds(3_600))
        val waiting = checkNotNull(store.locationSettings.first().arrival)
        assertTrue("held by its coordinates: ${waiting.name}", waiting.isUnnamed)

        online = true
        assertTrue(naming.retry())
        val shown = checkNotNull(store.locationSettings.first().gpsCity)
        assertEquals("Milano", shown.name)
        assertEquals("Lombardia", shown.region)
        assertEquals(waiting.cacheKey, shown.cacheKey)
        assertFalse(shown.isUnnamed)
        assertNull(store.locationSettings.first().arrival)
    }

    @Test
    fun `without a geocoder's answer the coordinates stay, honestly`() = runBlocking {
        atCavenago()
        arrival.take(lambrate, at.plusSeconds(3_600))
        online = true
        assertTrue(arrival.retry())
        assertTrue(checkNotNull(store.locationSettings.first().gpsCity).isUnnamed)
    }

    /** With no weather to stay on, the place was adopted at once by its coordinates: the
     * first fetch that lands is the sign of being online, and names it. */
    @Test
    fun `a page adopted with its coordinates is named at the first chance, once`() = runBlocking {
        store.adoptGpsFix(lambrate, at)
        assertTrue(checkNotNull(store.locationSettings.first().gpsCity).isUnnamed)
        assertTrue(naming.nameShown())
        val shown = checkNotNull(store.locationSettings.first().gpsCity)
        assertEquals("Milano", shown.name)
        assertEquals(lambrate.coordinates, shown.coordinates)
        assertFalse("already named", naming.nameShown())
        // A named page is left alone by a geocoder with another idea.
        atCavenago()
        assertFalse(naming.nameShown())
        assertEquals("Cavenago di Brianza", checkNotNull(store.locationSettings.first().gpsCity).name)
    }

    @Test
    fun `the distance said is in whole kilometres and never under two`() {
        val shown = cavenago.toGpsCity()
        assertEquals(17, GpsArrival.distanceKm(shown, lambrate.toGpsCity()))
        assertEquals(2, GpsArrival.distanceKm(shown, nextStreet.toGpsCity()))
    }
}
