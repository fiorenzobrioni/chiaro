package com.callbackdev.chiaro.ui.today

import com.callbackdev.chiaro.data.PowerSaveState
import com.callbackdev.chiaro.domain.WeatherException
import com.callbackdev.chiaro.domain.model.City
import com.callbackdev.chiaro.domain.model.Coordinates
import com.callbackdev.chiaro.domain.model.WeatherReport
import com.callbackdev.chiaro.domain.sample.sampleWeatherReport
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * Today's road to a fresh report, shared by Alerts and the Sky (28 Oct 2026): the cache first,
 * a silent fetch past the provider's quarter of an hour, the backoff after a failure, the
 * battery saver's exceptions, and a change of place.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReportKeeperTest {

    private val base: Instant = Instant.parse("2026-10-28T10:00:00Z")
    private val milan = City(
        id = 1L, name = "Milano", region = "Lombardia", country = "Italia",
        coordinates = Coordinates(45.4643, 9.1895), timezone = "Europe/Rome"
    )
    private val rome = milan.copy(id = 2L, name = "Roma")

    /** The scheduler's virtual time, as a clock: a minute of `delay` is a minute of age. */
    private class VirtualClock(private val scheduler: TestCoroutineScheduler, private val base: Instant) : Clock() {
        override fun instant(): Instant = base.plusMillis(scheduler.currentTime)
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId?): Clock = this
    }

    private fun report(fetchedAt: Instant): WeatherReport =
        sampleWeatherReport().let { it.copy(systemInfo = it.systemInfo.copy(lastSync = fetchedAt)) }

    private class Service(var cached: WeatherReport?, var answer: (Instant) -> WeatherReport) {
        var fetches = 0
    }

    private fun TestScope.keeper(
        service: Service,
        powerSave: PowerSaveState = PowerSaveState.Off
    ): Pair<ReportKeeper, VirtualClock> {
        val clock = VirtualClock(testScheduler, base)
        val keeper = ReportKeeper(
            cached = { service.cached },
            fetch = { service.fetches++; service.answer(clock.instant()) },
            clock = clock,
            powerSave = powerSave
        )
        return keeper to clock
    }

    @Test
    fun `the cache goes out first, then a fresh report when it is older than the quarter hour`() = runTest {
        val old = report(base.minus(Duration.ofMinutes(20)))
        val service = Service(old) { report(it) }
        val (keeper, _) = keeper(service)
        val seen = mutableListOf<KeptReport>()
        backgroundScope.launch { keeper.keep(milan).collect { seen += it } }
        runCurrent()
        assertEquals(2, seen.size)
        assertSame(old, seen[0].report)
        assertEquals(base, seen[1].report!!.systemInfo.lastSync)
        assertEquals(1, service.fetches)
    }

    @Test
    fun `a young report is not asked again until the quarter hour has passed`() = runTest {
        val service = Service(report(base.minus(Duration.ofMinutes(5)))) { report(it) }
        val (keeper, _) = keeper(service)
        val seen = mutableListOf<KeptReport>()
        backgroundScope.launch { keeper.keep(milan).collect { seen += it } }
        runCurrent()
        assertEquals(0, service.fetches)
        // Nine ticks later it is fourteen minutes old: still a hit.
        advanceTimeBy(Duration.ofMinutes(9).toMillis() + 1)
        assertEquals(0, service.fetches)
        assertEquals(10, seen.size) // the cache, then one a minute: the clock moves on screen
        // At the tenth it is fifteen: asked, once.
        advanceTimeBy(Duration.ofMinutes(1).toMillis())
        assertEquals(1, service.fetches)
        assertEquals(base.plus(Duration.ofMinutes(10)), seen.last().report!!.systemInfo.lastSync)
        advanceTimeBy(Duration.ofMinutes(5).toMillis())
        assertEquals(1, service.fetches)
    }

    @Test
    fun `under battery saver a report is never asked again, and no report is asked once`() = runTest {
        val saving = PowerSaveState { true }
        val withReport = Service(report(base.minus(Duration.ofHours(3)))) { report(it) }
        val (keeper, _) = keeper(withReport, saving)
        backgroundScope.launch { keeper.keep(milan).collect { } }
        advanceTimeBy(Duration.ofMinutes(30).toMillis() + 1)
        assertEquals(0, withReport.fetches)

        val empty = Service(null) { report(it) }
        val (keeper2, clock2) = keeper(empty, saving)
        val seen = mutableListOf<KeptReport>()
        backgroundScope.launch { keeper2.keep(milan).collect { seen += it } }
        runCurrent()
        assertEquals(1, empty.fetches)
        assertNull(seen[0].report)
        // Fetched at this minute of the test's clock, half an hour in.
        assertEquals(clock2.instant(), seen[1].report!!.systemInfo.lastSync)
    }

    @Test
    fun `a failure is silent and waits for the backoff before asking again`() = runTest {
        val service = Service(report(base.minus(Duration.ofHours(1)))) { throw WeatherException.NoNetwork() }
        val (keeper, _) = keeper(service)
        val seen = mutableListOf<KeptReport>()
        backgroundScope.launch { keeper.keep(milan).collect { seen += it } }
        runCurrent()
        assertEquals(1, service.fetches)
        assertEquals(1, seen.size)
        // One minute after one failure, two after two, four after three (RefreshBackoff).
        advanceTimeBy(Duration.ofMinutes(1).toMillis() + 1)
        assertEquals(2, service.fetches)
        advanceTimeBy(Duration.ofMinutes(1).toMillis())
        assertEquals(2, service.fetches)
        advanceTimeBy(Duration.ofMinutes(1).toMillis())
        assertEquals(3, service.fetches)
        advanceTimeBy(Duration.ofMinutes(3).toMillis())
        assertEquals(3, service.fetches)
        advanceTimeBy(Duration.ofMinutes(1).toMillis())
        assertEquals(4, service.fetches)
        // The page kept the report it had all along, and the ticks went on.
        assertSame(service.cached, seen.last().report)
        assertEquals(8, seen.size)
    }

    @Test
    fun `follow moves with the place and answers null for none`() = runTest {
        val service = Service(report(base)) { report(it) }
        val (keeper, _) = keeper(service)
        val cities = MutableStateFlow<City?>(milan)
        val seen = mutableListOf<KeptReport?>()
        backgroundScope.launch { keeper.follow(cities).collect { seen += it } }
        runCurrent()
        assertEquals(milan, seen.last()!!.city)
        cities.value = null
        runCurrent()
        assertNull(seen.last())
        cities.value = rome
        runCurrent()
        assertEquals(rome, seen.last()!!.city)
        assertEquals(0, service.fetches) // a report of this minute: hits only
    }
}
