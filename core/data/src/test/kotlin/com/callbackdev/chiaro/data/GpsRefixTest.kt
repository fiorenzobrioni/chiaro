package com.callbackdev.chiaro.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.callbackdev.chiaro.domain.WeatherException
import com.callbackdev.chiaro.domain.model.City
import com.callbackdev.chiaro.domain.model.Coordinates
import com.callbackdev.chiaro.domain.model.GeoFix
import com.callbackdev.chiaro.domain.model.toGpsCity
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * The road to the position nobody asked for out loud, which the tap on the position row
 * in Places takes since 30 ott 2026 as the swipe onto its page always did: gated by the
 * age of the last fix, silent when it fails, and one fix for the two callers of one
 * gesture.
 */
class GpsRefixTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val cavenago = GeoFix(Coordinates(45.58, 9.41), "Cavenago di Brianza", null, "Italy")
    // Milan's Lambrate, 17 km south-west: well past the two kilometres of the adoption.
    private val lambrate = GeoFix(Coordinates(45.48, 9.24), "Milano", null, "Italy")
    private val now: Instant = Instant.parse("2026-10-30T08:00:00Z")

    private class FakeProvider(var answer: GeoFix) : LocationProvider {
        @Volatile
        var calls = 0
        var failure: WeatherException? = null
        var gate: CompletableDeferred<Unit>? = null
        var lastMaxAge: Duration? = null
        var lastTimeout: Duration? = null

        override suspend fun currentFix(maxAge: Duration, timeout: Duration): GeoFix {
            calls++
            lastMaxAge = maxAge
            lastTimeout = timeout
            gate?.await()
            failure?.let { throw it }
            return answer
        }
    }

    private var online = true
    private val withWeather = mutableSetOf<String>()
    private val fetched = mutableListOf<String>()

    private val store = CityStore(
        PreferenceDataStoreFactory.create(scope = scope) { tmp.newFile("cities.preferences_pb") },
        Json
    )
    private val provider = FakeProvider(cavenago)
    private val refix = GpsRefix(
        store,
        provider,
        GpsArrival(
            store,
            hasWeather = { it.cacheKey in withWeather },
            fetchWeather = {
                fetched += it.cacheKey
                if (!online) throw WeatherException.NoNetwork()
                withWeather += it.cacheKey
            }
        ),
        Clock.fixed(now, ZoneOffset.UTC)
    )

    @After
    fun tearDown() {
        scope.cancel()
    }

    /** The position on, the reader found at Cavenago [ago] before [now], its weather on disk. */
    private suspend fun atCavenago(ago: Duration): City {
        val shown = store.adoptGpsFix(cavenago, now.minus(ago))
        store.setUseGps(true)
        withWeather += shown.cacheKey
        return shown
    }

    @Test
    fun `a fix younger than five minutes asks for nothing`() = runBlocking {
        atCavenago(Duration.ofMinutes(2))

        refix.run()

        assertEquals(0, provider.calls)
    }

    @Test
    fun `an older fix is taken again, from what the phone already knows when it can`() = runBlocking {
        atCavenago(Duration.ofMinutes(6))

        refix.run()

        assertEquals(1, provider.calls)
        // Not Now: a position the platform holds from the last five minutes is the answer,
        // and nothing is powered up. And not the fifteen seconds of a reader watching.
        assertEquals(LocationProvider.SilentMaxAge, provider.lastMaxAge)
        assertEquals(LocationProvider.SilentTimeout, provider.lastTimeout)
        assertEquals(now, store.locationSettings.first().fixedAt)
    }

    @Test
    fun `a fix that fails leaves the place and its weather, and says nothing`() = runBlocking {
        val shown = atCavenago(Duration.ofHours(1))
        provider.failure = WeatherException.LocationPermissionDenied()

        refix.run() // no throw: nobody asked out loud

        val settings = store.locationSettings.first()
        assertEquals(shown, settings.gpsCity)
        assertEquals(now.minus(Duration.ofHours(1)), settings.fixedAt)
    }

    @Test
    fun `a move goes to the new place with its weather in hand`() = runBlocking {
        atCavenago(Duration.ofHours(1))
        provider.answer = lambrate

        refix.run()

        assertEquals(lambrate.coordinates, store.locationSettings.first().gpsCity?.coordinates)
        assertEquals(1, fetched.size)
    }

    @Test
    fun `with no fix due, a place still waiting for its weather is asked for again`() = runBlocking {
        atCavenago(Duration.ofHours(1))
        // Reached offline a minute ago: the page stayed on Cavenago and Lambrate waits.
        store.holdGpsFix(lambrate, now.minus(Duration.ofMinutes(1)))

        refix.run()

        assertEquals(0, provider.calls)
        val settings = store.locationSettings.first()
        assertEquals(lambrate.coordinates, settings.gpsCity?.coordinates)
        assertNull(settings.arrival)
        assertEquals(listOf(lambrate.toGpsCity().cacheKey), fetched)
    }

    @Test
    fun `the position turned off asks for nothing`() = runBlocking {
        atCavenago(Duration.ofHours(1))
        store.setUseGps(false)

        refix.run()

        assertEquals(0, provider.calls)
    }

    @Test
    fun `the two callers of one gesture take one fix and fetch the new place once`() = runBlocking {
        // The tap on the row selects the page, and the pager settling on it is the second
        // caller. Without the lock both took the fix and both fetched Lambrate.
        atCavenago(Duration.ofHours(1))
        provider.answer = lambrate
        val gate = CompletableDeferred<Unit>()
        provider.gate = gate

        val tap = async(Dispatchers.IO) { refix.run() }
        withTimeout(10_000) { while (provider.calls == 0) delay(10) }
        val settle = async(Dispatchers.IO) { refix.run() }
        delay(100) // the settle is waiting on the lock, not on the provider
        gate.complete(Unit)
        tap.await()
        settle.await()

        assertEquals(1, provider.calls)
        assertEquals(1, fetched.size)
        assertEquals(lambrate.coordinates, store.locationSettings.first().gpsCity?.coordinates)
    }
}
