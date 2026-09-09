package com.callbackdev.chiaro.widget.arc

import com.callbackdev.chiaro.domain.model.City
import com.callbackdev.chiaro.domain.model.Coordinates
import com.callbackdev.chiaro.domain.model.DailyForecast
import com.callbackdev.chiaro.domain.model.HourlyForecast
import com.callbackdev.chiaro.domain.model.WeatherCondition
import com.callbackdev.chiaro.domain.sample.sampleWeatherReport
import com.callbackdev.chiaro.domain.sky.SkyJobCatalog
import com.callbackdev.chiaro.domain.sky.SkyVerdict
import com.callbackdev.chiaro.domain.sky.SkyVerdictKind
import com.callbackdev.chiaro.ui.today.TimelineKind
import com.callbackdev.chiaro.ui.today.TodayStateBuilder
import com.callbackdev.chiaro.ui.today.TodayUiState
import com.callbackdev.chiaro.widget.NextMoment
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * What the arc draws, derived from a report and a moment (9 set 2026): the window and
 * where the present falls in it, the hours the trimmed report leaves inside it, the
 * agenda's filters, and the verdict a followed moment lends to its row.
 */
class ArcSeriesTest {

    private val zone = ZoneId.of("Europe/Rome")
    private val milan = City(
        id = 1L, name = "Milano", region = "Lombardia", country = "Italia",
        coordinates = Coordinates(45.4643, 9.1895), timezone = "Europe/Rome"
    )
    private val date: LocalDate = LocalDate.of(2026, 9, 2)
    private val clear = WeatherCondition(0, "Clear", "☀️")

    private fun report(fetchedAt: LocalDateTime, hours: Int = 48) =
        sampleWeatherReport().copy(
            location = sampleWeatherReport().location.copy(
                city = "Milano",
                coordinates = milan.coordinates,
                timezone = "Europe/Rome",
                localTime = fetchedAt
            ),
            hourly = (0 until hours).map {
                HourlyForecast(
                    time = fetchedAt.withMinute(0).plusHours(it.toLong()),
                    tempC = 20.0,
                    condition = clear,
                    precipChancePct = 10,
                    cloudCoverPct = 20
                )
            },
            daily = (0 until 7).map {
                DailyForecast(date.plusDays(it.toLong()), 24.0, 14.0, clear, 10, 5, "Moderate")
            },
            systemInfo = sampleWeatherReport().systemInfo.copy(
                lastSync = fetchedAt.atZone(zone).toInstant()
            )
        )

    private fun content(now: Instant, fetched: LocalDateTime = LocalDateTime.of(2026, 9, 2, 12, 0)) =
        TodayStateBuilder.build(milan, report(fetched), now, 60, false, null) as TodayUiState.Content

    private fun at(hour: Int, minute: Int = 0, day: LocalDate = date): Instant =
        day.atTime(LocalTime.of(hour, minute)).atZone(zone).toInstant()

    private fun series(now: Instant, settings: ArcSettings = ArcSettings(), moments: List<NextMoment> = emptyList()) =
        ArcSeries.build(content(now), moments, milan.coordinates, now, settings)

    @Test
    fun `today's window is the civil day of the place`() {
        val window = ArcSeries.window(ArcSpan.TODAY, at(15), zone)
        assertEquals(at(0), window.start)
        assertEquals(at(0, day = date.plusDays(1)), window.end)
        assertEquals(0.625f, window.fraction(at(15)), 0.0001f)
        assertEquals(at(6), window.at(0.25f))
    }

    @Test
    fun `a clock-change day is honestly twenty-five hours`() {
        val fallBack = LocalDate.of(2026, 10, 25)
        val window = ArcSeries.window(ArcSpan.TODAY, at(12, day = fallBack), zone)
        assertEquals(Duration.ofHours(25), window.length)
    }

    @Test
    fun `the rolling window starts half an hour ago and runs a day`() {
        val now = at(14, 37)
        val window = ArcSeries.window(ArcSpan.AHEAD, now, zone)
        assertEquals(now.minus(Duration.ofMinutes(30)), window.start)
        assertEquals(Duration.ofHours(24), window.length)
        assertEquals(0.5f / 24f, window.fraction(now), 0.0001f)
    }

    @Test
    fun `ticks are the whole hours on the step's multiples, end exclusive`() {
        val s = series(at(15))
        val threes = s.ticks(zone, 3)
        assertEquals(8, threes.size)
        assertEquals(listOf(0, 3, 6, 9, 12, 15, 18, 21), threes.map { it.second.hour })
        assertEquals(at(0), threes.first().first)
        assertEquals(4, s.ticks(zone, 6).size)
        assertEquals(24, s.ticks(zone, 1).size)
        assertTrue(s.ticks(zone, 0).isEmpty())
    }

    @Test
    fun `in the rolling window the first tick is the next whole hour`() {
        val s = series(at(14, 37), ArcSettings(span = ArcSpan.AHEAD))
        val all = s.ticks(zone, 1)
        // Window 14:07 → 14:07 tomorrow: 15:00 today through 14:00 tomorrow.
        assertEquals(15, all.first().second.hour)
        assertEquals(24, all.size)
        assertEquals(14, all.last().second.hour)
    }

    @Test
    fun `the hours inside the window are the trimmed report's, so the morning has none`() {
        val s = series(at(15, 30))
        // The report is trimmed to the present: the hour in progress (15:00) is the first.
        assertEquals(at(15), s.hours.first().at)
        assertEquals(at(23), s.hours.last().at)
        assertEquals(9, s.hours.size)
        assertEquals(0.625f, s.hours.first().fraction, 0.0001f)
        assertEquals(s.hours.first(), s.hourAt(at(15)))
        assertNull(s.hourAt(at(14)))
        assertEquals(s.hours.first(), s.hourCovering(at(15, 40)))
        assertNull(s.hourCovering(at(14, 40)))
    }

    @Test
    fun `the present sits where the window puts it`() {
        assertEquals(0.625f, series(at(15)).nowFraction, 0.0001f)
        assertEquals(0.5f / 24f, series(at(15), ArcSettings(span = ArcSpan.AHEAD)).nowFraction, 0.0001f)
        assertEquals(ArcSeries.SunSamples, series(at(15)).sun.size)
        assertEquals(ArcSeries.MoonSamples, series(at(15)).moon.size)
        assertFalse(series(at(15)).southern)
    }

    @Test
    fun `the agenda is the next twenty-four hours, soonest first`() {
        val now = at(15)
        val s = series(now)
        assertTrue(s.events.isNotEmpty())
        assertEquals(s.events.first(), s.next)
        assertTrue(s.events.all { it.at.isAfter(now) && !it.at.isAfter(now.plus(ArcSeries.AgendaReach)) })
        assertEquals(s.events.sortedBy { it.at }, s.events)
        // Tomorrow's sunrise is on the list, and knows it is tomorrow's.
        val sunrise = s.events.first { it.item.kind == TimelineKind.SUNRISE }
        assertTrue(sunrise.tomorrow)
        assertFalse(s.events.first { it.item.kind == TimelineKind.SUNSET }.tomorrow)
    }

    @Test
    fun `the agenda's families follow the settings`() {
        val sunOnly = series(at(15), ArcSettings(agendaMoon = false, agendaRain = false))
        assertTrue(sunOnly.events.all { ArcSeries.jobIdsFor(it.item.kind).isNotEmpty() })
        assertTrue(sunOnly.events.none { it.item.kind == TimelineKind.MOONRISE || it.item.kind == TimelineKind.MOONSET })
        val noSun = series(at(15), ArcSettings(agendaSun = false))
        assertTrue(noSun.events.none { it.item.kind == TimelineKind.SUNSET || it.item.kind == TimelineKind.SUNRISE })
        assertTrue(ArcSeries.allowed(TimelineKind.RAIN_START, ArcSettings(agendaRain = true)))
        assertFalse(ArcSeries.allowed(TimelineKind.RAINBOW, ArcSettings(agendaRain = false)))
    }

    @Test
    fun `a followed moment lends its verdict to its row`() {
        val now = at(15)
        val sunset = series(now).events.first { it.item.kind == TimelineKind.SUNSET }
        val pass = SkyVerdict(SkyVerdictKind.PASS, cloudPct = 10)
        val followed = listOf(NextMoment(SkyJobCatalog.SunSet, sunset.at, null, pass, inProgress = false))
        val withVerdict = series(now, moments = followed).events.first { it.item.kind == TimelineKind.SUNSET }
        assertEquals(pass, withVerdict.verdict)
        // Not when the reader turned the marks off.
        val silent = series(now, ArcSettings(agendaVerdicts = false), followed)
        assertNull(silent.events.first { it.item.kind == TimelineKind.SUNSET }.verdict)
        // Not from a moment of another kind, and not from one too far off.
        val moonrise = listOf(NextMoment(SkyJobCatalog.MoonRise, sunset.at, null, pass, inProgress = false))
        assertNull(series(now, moments = moonrise).events.first { it.item.kind == TimelineKind.SUNSET }.verdict)
        val late = listOf(NextMoment(SkyJobCatalog.SunSet, sunset.at.plus(Duration.ofMinutes(25)), null, pass, false))
        assertNull(series(now, moments = late).events.first { it.item.kind == TimelineKind.SUNSET }.verdict)
        val close = listOf(NextMoment(SkyJobCatalog.SunSet, sunset.at.plus(Duration.ofMinutes(10)), null, pass, false))
        assertNotNull(series(now, moments = close).events.first { it.item.kind == TimelineKind.SUNSET }.verdict)
    }

    @Test
    fun `a range lends its verdict by its end too`() {
        val now = at(6)
        val goldenEnd = series(now).events.first { it.item.kind == TimelineKind.GOLDEN_MORNING_END }
        val pass = SkyVerdict(SkyVerdictKind.UNSTABLE, cloudPct = 55)
        val golden = listOf(
            NextMoment(SkyJobCatalog.GoldenAm, goldenEnd.at.minus(Duration.ofMinutes(40)), goldenEnd.at, pass, false)
        )
        assertEquals(
            pass,
            series(now, moments = golden).events.first { it.item.kind == TimelineKind.GOLDEN_MORNING_END }.verdict
        )
    }

    @Test
    fun `the day's range comes from today's forecast`() {
        val s = series(at(15))
        assertEquals(24.0, s.highC!!, 0.0)
        assertEquals(14.0, s.lowC!!, 0.0)
    }
}
