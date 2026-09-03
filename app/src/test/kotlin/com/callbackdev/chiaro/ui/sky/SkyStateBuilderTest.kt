package com.callbackdev.chiaro.ui.sky

import com.callbackdev.chiaro.data.AppSettings
import com.callbackdev.chiaro.data.SkySubscription
import com.callbackdev.chiaro.domain.model.City
import com.callbackdev.chiaro.domain.model.Coordinates
import com.callbackdev.chiaro.domain.model.HourlyForecast
import com.callbackdev.chiaro.domain.model.WeatherCondition
import com.callbackdev.chiaro.domain.sample.sampleWeatherReport
import com.callbackdev.chiaro.domain.sky.MoonQuarterKind
import com.callbackdev.chiaro.domain.sky.SkyJobCatalog
import com.callbackdev.chiaro.domain.sky.SkyLead
import com.callbackdev.chiaro.domain.sky.SkyOccurrence
import com.callbackdev.chiaro.domain.sky.SkyVerdictKind
import com.callbackdev.chiaro.domain.sky.SkyVerdictNote
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SkyStateBuilderTest {

    private val zone = ZoneId.of("Europe/Rome")
    private val milan = City(
        id = 1L, name = "Milano", region = "Lombardia", country = "Italia",
        coordinates = Coordinates(45.4643, 9.1895), timezone = "Europe/Rome"
    )
    private val clear = WeatherCondition(0, "Clear", "☀️")

    /** Noon of a NEW-MOON day (2026-09-11), so the dark window cannot be washed by
     * moonlight and a clear forecast really is a PASS; every daily moment of the day
     * sits inside the 48 clear hours. */
    private val fetched: LocalDateTime = LocalDateTime.of(2026, 9, 11, 12, 0)
    private val noon = fetched.atZone(zone).toInstant()

    private fun report(cloudPct: Int = 10, precipPct: Int = 5) = sampleWeatherReport().copy(
        hourly = (0 until 48).map {
            HourlyForecast(
                time = fetched.plusHours(it.toLong()),
                tempC = 20.0,
                condition = clear,
                precipChancePct = precipPct,
                cloudCoverPct = cloudPct
            )
        },
        systemInfo = sampleWeatherReport().systemInfo.copy(
            lastSync = fetched.atZone(zone).toInstant()
        )
    )

    private fun defaults() = SkyJobCatalog.defaults.map { SkySubscription(it.id) }

    @Test
    fun `tonight has a window and a clear-sky verdict`() {
        val content = SkyStateBuilder.build(milan, report(), defaults(), AppSettings(), noon)

        val window = content.tonight.window
        assertNotNull(window)
        // Dusk tonight, dawn tomorrow: the window crosses midnight in the city zone.
        assertEquals(LocalDate.of(2026, 9, 11), window!!.start.atZone(zone).toLocalDate())
        assertEquals(LocalDate.of(2026, 9, 12), window.end!!.atZone(zone).toLocalDate())
        assertEquals(SkyVerdictKind.PASS, content.tonight.verdict?.kind)
    }

    @Test
    fun `in the small hours tonight means the night still in progress`() {
        val threeAm = LocalDateTime.of(2026, 9, 12, 3, 0).atZone(zone).toInstant()
        val content = SkyStateBuilder.build(milan, report(), defaults(), AppSettings(), threeAm)

        val window = content.tonight.window
        assertNotNull(window)
        assertTrue(window!!.start.isBefore(threeAm))
        assertTrue(window.end!!.isAfter(threeAm))
    }

    @Test
    fun `the default subscriptions become today's moments in time order`() {
        val content = SkyStateBuilder.build(milan, report(), defaults(), AppSettings(), noon)

        assertEquals(SkyJobCatalog.defaults.size, content.moments.size)
        val scheduled = content.moments.map { it.occurrence }.filterIsInstance<SkyOccurrence.At>()
        assertEquals(scheduled.sortedBy { it.start }, scheduled)
        // The morning is over by noon; the evening is not.
        assertTrue(content.moments.first { it.job.id == "sun.rise" }.past)
        assertTrue(!content.moments.first { it.job.id == "sun.set" }.past)
    }

    @Test
    fun `the moon moment carries its phase instead of a verdict`() {
        val content = SkyStateBuilder.build(milan, report(), defaults(), AppSettings(), noon)

        val moon = content.moments.first { it.job.id == "moon.today" }
        assertNotNull(moon.moonPhase)
        assertNotNull(moon.moonIlluminationPct)
        assertNull(moon.verdict) // a phase is a fact about the day, not a sight to judge
    }

    @Test
    fun `a moment's own lead wins and zero means off`() {
        val subscriptions = listOf(
            SkySubscription("sun.rise"),                         // follows the default
            SkySubscription("sun.set", notifyLeadMinutes = 60),  // its own hour
            SkySubscription("golden_hour.pm", notifyLeadMinutes = 0) // explicitly never
        )
        val settings = AppSettings(skyNotifyDefaultMin = 15)
        val content = SkyStateBuilder.build(milan, report(), subscriptions, settings, noon)

        assertEquals(SkyLead.FIFTEEN, content.moments.first { it.job.id == "sun.rise" }.lead)
        assertEquals(SkyLead.ONE_HOUR, content.moments.first { it.job.id == "sun.set" }.lead)
        assertEquals(SkyLead.OFF, content.moments.first { it.job.id == "golden_hour.pm" }.lead)
        assertTrue(content.moments.first { it.job.id == "sun.rise" }.followsDefault)
        assertTrue(!content.moments.first { it.job.id == "sun.set" }.followsDefault)
    }

    @Test
    fun `the calendar ahead is sorted, capped, and includes the next full moon`() {
        val content = SkyStateBuilder.build(milan, report(), defaults(), AppSettings(), noon)

        assertTrue(content.events.isNotEmpty())
        assertTrue(content.events.size <= 6)
        val starts = content.events.map { it.occurrence.start }
        assertEquals(starts.sorted(), starts)
        assertTrue(content.events.any { it.quarter == MoonQuarterKind.FULL_MOON })
        // Everything ahead is genuinely ahead.
        assertTrue(starts.all { it.isAfter(noon) })
    }

    @Test
    fun `an event past the forecast's horizon says so instead of guessing`() {
        val content = SkyStateBuilder.build(milan, report(), defaults(), AppSettings(), noon)

        // Every annual event is months out; a 48-hour forecast cannot judge it.
        val judged = content.events.mapNotNull { it.verdict }
        assertTrue(judged.isNotEmpty())
        assertTrue(judged.all { it.kind == SkyVerdictKind.UNKNOWN })
        assertTrue(judged.all { it.note == SkyVerdictNote.BEYOND_HORIZON })
    }

    @Test
    fun `no report means unknown with the no-data reason, never a guess`() {
        val content = SkyStateBuilder.build(milan, null, defaults(), AppSettings(), noon)

        assertEquals(SkyVerdictKind.UNKNOWN, content.tonight.verdict?.kind)
        assertEquals(SkyVerdictNote.NO_DATA, content.tonight.verdict?.note)
    }

    @Test
    fun `rain fails the night whatever the clouds say`() {
        val content = SkyStateBuilder.build(
            milan, report(cloudPct = 5, precipPct = 80), defaults(), AppSettings(), noon
        )

        assertEquals(SkyVerdictKind.FAIL, content.tonight.verdict?.kind)
        assertEquals(SkyVerdictNote.PRECIPITATION, content.tonight.verdict?.note)
    }

    @Test
    fun `every time on the screen is the city's, not the phone's`() {
        val content = SkyStateBuilder.build(milan, report(), defaults(), AppSettings(), noon)
        assertEquals(zone, content.zone)
    }
}
