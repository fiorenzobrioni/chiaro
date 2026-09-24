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
    private val clear = WeatherCondition(0)

    /** Noon of a NEW-MOON day (2026-09-11), so the dark window cannot be washed by
     * moonlight and a clear forecast really is a PASS; every daily moment of the day
     * sits inside the 48 clear hours. */
    private val fetched: LocalDateTime = LocalDateTime.of(2026, 9, 11, 12, 0)
    private val noon = fetched.atZone(zone).toInstant()

    /** [syncedAt] is when the fetch landed: a report older than twice the update
     * interval is too old to judge by, and the evening cases here are about the
     * moment on the row, not about staleness. */
    private fun report(
        cloudPct: Int = 10,
        precipPct: Int = 5,
        syncedAt: LocalDateTime = fetched
    ) = sampleWeatherReport().copy(
        // The sample is a New York report, and until 20 set 2026 this fixture handed it
        // to a Milan city and left the mismatch standing: the builder resolved its zone
        // from the CITY, so nothing noticed. It resolves it from the report now — the
        // better source, and the only one the position has — and a report whose location
        // is not its city's is a pairing the repository cannot produce, since `map()`
        // writes the location from the very city it was asked about.
        location = sampleWeatherReport().location.copy(
            city = milan.name,
            region = milan.region,
            country = milan.country,
            coordinates = milan.coordinates,
            timezone = zone.id,
            localTime = fetched
        ),
        hourly = (0 until 48).map {
            HourlyForecast(
                time = fetched.plusHours(it.toLong()),
                at = fetched.plusHours(it.toLong()).atZone(zone).toInstant(),
                tempC = 20.0,
                condition = clear,
                precipChancePct = precipPct,
                cloudCoverPct = cloudPct
            )
        },
        systemInfo = sampleWeatherReport().systemInfo.copy(
            lastSync = syncedAt.atZone(zone).toInstant()
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
    fun `the default subscriptions become the moments ahead, in time order`() {
        val content = SkyStateBuilder.build(milan, report(), defaults(), AppSettings(), noon)

        assertEquals(SkyJobCatalog.defaults.size, content.moments.size)
        val scheduled = content.moments.map { it.occurrence }.filterIsInstance<SkyOccurrence.At>()
        assertEquals(scheduled.sortedBy { it.start }, scheduled)
        // The morning is over by noon, so its row is tomorrow's; the evening is not.
        assertEquals(
            MomentTiming.TOMORROW,
            content.moments.first { it.job.id == "sun.rise" }.timing
        )
        assertEquals(
            MomentTiming.TODAY,
            content.moments.first { it.job.id == "sun.set" }.timing
        )
    }

    /**
     * The device report this rule was written for (3 set): at 21:19 the screen showed
     * this morning's sunrise, greyed out and unjudgeable, while the widget already
     * showed tomorrow's, judged. Now the screen shows the one the widget shows.
     */
    @Test
    fun `in the evening the sunrise row is tomorrow's, and it can be judged`() {
        val eveningAt = LocalDateTime.of(2026, 9, 11, 21, 19)
        val evening = eveningAt.atZone(zone).toInstant()
        val content = SkyStateBuilder.build(
            milan, report(syncedAt = eveningAt), defaults(), AppSettings(), evening
        )

        val sunrise = content.moments.first { it.job.id == "sun.rise" }
        assertEquals(MomentTiming.TOMORROW, sunrise.timing)
        val at = sunrise.occurrence as SkyOccurrence.At
        assertTrue(at.start.isAfter(evening))
        assertEquals(LocalDate.of(2026, 9, 12), at.start.atZone(zone).toLocalDate())
        // The hours it lands in are in the forecast, so there is a real verdict.
        assertEquals(SkyVerdictKind.PASS, sunrise.verdict?.kind)
    }

    @Test
    fun `the screen and the Sky widget name the same moment`() {
        val evening = LocalDateTime.of(2026, 9, 11, 21, 19).atZone(zone).toInstant()
        val content = SkyStateBuilder.build(milan, report(), defaults(), AppSettings(), evening)

        val widget = SkyUpcoming.firstAt(
            SkyJobCatalog.defaults, evening, zone, milan.coordinates
        )
        // The widget's moment is the screen's first row that actually fires (the
        // moon's day-moment is a standing fact about today, not an appointment).
        val firstToFire = content.moments
            .filter { it.job.id != SkyJobCatalog.MoonToday.id }
            .mapNotNull { it.occurrence as? SkyOccurrence.At }
            .minByOrNull { it.start }
        assertEquals(firstToFire?.start, widget?.at?.start)
        assertEquals(firstToFire?.job?.id, widget?.at?.job?.id)
    }

    @Test
    fun `the moon's day-moment never rolls to tomorrow`() {
        val evening = LocalDateTime.of(2026, 9, 11, 21, 19).atZone(zone).toInstant()
        val content = SkyStateBuilder.build(milan, report(), defaults(), AppSettings(), evening)

        val moon = content.moments.first { it.job.id == "moon.today" }
        assertEquals(MomentTiming.TODAY, moon.timing)
        val at = moon.occurrence as SkyOccurrence.At
        assertEquals(LocalDate.of(2026, 9, 11), at.start.atZone(zone).toLocalDate())
    }

    @Test
    fun `a window that has opened and not closed is the one shown, marked now`() {
        val threeAm = LocalDateTime.of(2026, 9, 12, 3, 0).atZone(zone).toInstant()
        val subscriptions = listOf(SkySubscription("darkness.window"))
        val content = SkyStateBuilder.build(milan, report(), subscriptions, AppSettings(), threeAm)

        val window = content.moments.single()
        assertEquals(MomentTiming.NOW, window.timing)
        val at = window.occurrence as SkyOccurrence.At
        assertTrue(at.start.isBefore(threeAm))
        assertTrue(at.end!!.isAfter(threeAm))
        // The hero and the row are looking at the same night.
        assertEquals(content.tonight.window?.start, at.start)
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
        val starts = content.events.map { it.at!!.start }
        assertEquals(starts.sorted(), starts)
        assertTrue(content.events.any { it.quarter == MoonQuarterKind.FULL_MOON })
        // Everything ahead is genuinely ahead.
        assertTrue(starts.all { it.isAfter(noon) })
    }

    /**
     * Fase 27, tier one. The six rows used to be the six nearest events in the
     * catalog, so a subscribed line appeared only if it happened to win a race it
     * never knew it was in: measured at Milan on 20 set 2026 the six covered seven
     * weeks, and the next solar eclipse visible from there (2 ago 2027) could not
     * reach the screen in a year of trying — while its reminder fired all the same.
     */
    @Test
    fun `a subscribed event always has a row, however far out it is`() {
        val content = SkyStateBuilder.build(
            milan,
            report(),
            defaults() + SkySubscription(SkyJobCatalog.SolarEclipse.id),
            AppSettings(),
            noon
        )
        val eclipse = content.events.firstOrNull { it.job.id == SkyJobCatalog.SolarEclipse.id }
        assertNotNull("the subscribed eclipse must have a row", eclipse)
        assertNotNull("and a date on it", eclipse!!.at)
        // Years out, so the row has to say which year (the list printed `d MMMM`).
        assertTrue("an eclipse this far out must print its year", eclipse.showYear)
        // And the calendar everyone gets is still there under it.
        assertTrue(content.events.size > 1)
    }

    /**
     * Fase 27, and the other half of tier one: a `∅` on a subscribed line is the
     * answer, not a row to drop. The Perseids peak over Stockholm on a night that
     * never gets astronomically dark, and the list used to delete the most famous
     * shower of the year rather than say so — while the moments list beside it
     * printed exactly this kind of fact with its reason.
     */
    @Test
    fun `a subscribed event the sky skips keeps its row and its reason`() {
        val stockholm = City(
            id = 2L, name = "Stoccolma", region = "Stockholms lan", country = "Svezia",
            coordinates = Coordinates(59.3293, 18.0686), timezone = "Europe/Stockholm"
        )
        val perseids = SkyJobCatalog.meteorShowers.first { it.id.contains("perseids") }
        val content = SkyStateBuilder.build(
            stockholm, null, listOf(SkySubscription(perseids.id)), AppSettings(), noon
        )
        val row = content.events.firstOrNull { it.job.id == perseids.id }
        assertNotNull("the subscribed shower must keep its row", row)
        assertEquals(
            com.callbackdev.chiaro.domain.sky.SkyNotScheduled.NO_DARKNESS,
            (row!!.occurrence as SkyOccurrence.None).reason
        )
        // The undated rows sort after the dated ones, as they do in the moments list.
        assertEquals(content.events.last().job.id, perseids.id)
    }

    /**
     * Fase 27. The delta Aquariids and the alpha Capricornids are both pinned to solar
     * longitude 127.0° — the IMO working list really does put them there — so they
     * resolve to the same instant, the same night and the same icon. Two rows a reader
     * could only read as a bug; one row, both names.
     */
    @Test
    fun `two showers that peak on one night share one row`() {
        val delta = SkyJobCatalog.meteorShowers.first { it.id.contains("delta_aquariids") }
        val capricornids =
            SkyJobCatalog.meteorShowers.first { it.id.contains("alpha_capricornids") }
        val content = SkyStateBuilder.build(
            milan,
            report(),
            listOf(SkySubscription(delta.id), SkySubscription(capricornids.id)),
            AppSettings(),
            noon
        )
        val rows = content.events.filter {
            it.job.id == delta.id || it.job.id == capricornids.id
        }
        assertEquals("one night, one row", 1, rows.size)
        assertEquals(1, rows.single().sharesNightWith.size)
        assertTrue(
            "and the row names the other shower too",
            rows.single().sharesNightWith.single().id in setOf(delta.id, capricornids.id)
        )
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
