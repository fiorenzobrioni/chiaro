package com.callbackdev.chiaro.domain.sky

import com.callbackdev.chiaro.domain.model.Coordinates
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The catalog's lines for Mars and Saturn (Fase 16, step 16.8), and Jupiter's, which reads
 * the same rule since the same step: the night's stretch in which each is worth going out
 * for, which is the «Da vedere» list's own row, and the reason there is none on a night
 * there is none.
 *
 * The two rows of the first test are **JPL Horizons', read on 12 Nov 2026**: observer table
 * for Milan (9.19 E, 45.4642 N), a minute a row, airless elevation, with Horizons' own mark
 * for the twilight each minute is in. Nautical dusk is the minute the mark goes from
 * nautical to astronomical (17:06 UTC on 10 Nov) and nautical dawn the minute it comes
 * back (05:10 UTC on the 11th); Saturn (699) is at 10.07 degrees at 01:45 and 9.90 at
 * 01:46, Mars (499) at 9.99 at 23:43 and 10.16 at 23:44.
 */
class SkyPlanetLinesTest {

    private val milan = Coordinates(45.4642, 9.19)
    private val rome = ZoneId.of("Europe/Rome")
    private val evening = LocalDate.of(2026, 11, 10)

    private val lines = listOf(
        SkyJobCatalog.JupiterNight to Planet.JUPITER,
        SkyJobCatalog.MarsNight to Planet.MARS,
        SkyJobCatalog.SaturnNight to Planet.SATURN
    )

    private fun assertNear(name: String, expected: String, actual: Instant?, minutes: Long = 2) {
        assertNotNull(name, actual)
        val gap = Duration.between(Instant.parse(expected), actual).abs()
        assertTrue("$name at $actual, Horizons $expected", gap <= Duration.ofMinutes(minutes))
    }

    @Test
    fun `Saturn's night runs from the stars coming out to the floor, and Mars' from the floor to the stars going`() {
        val saturn = SkyScheduler.resolve(SkyJobCatalog.SaturnNight, evening, rome, milan) as SkyOccurrence.At
        assertNear("Saturn's window opens", "2026-11-10T17:06:00Z", saturn.start)
        assertNear("Saturn sinks to ten degrees", "2026-11-11T01:45:30Z", saturn.end)

        val mars = SkyScheduler.resolve(SkyJobCatalog.MarsNight, evening, rome, milan) as SkyOccurrence.At
        assertNear("Mars clears ten degrees", "2026-11-10T23:43:30Z", mars.start)
        assertNear("Mars' window closes", "2026-11-11T05:10:00Z", mars.end)
    }

    /**
     * One rule for the list's row and the line's window: a reader told to look by a reminder
     * finds the thing on the list, the same hours.
     */
    @Test
    fun `the line is the list's row, the same window`() {
        val list = WorthSeeing.of(evening, rome, milan)
        for ((job, planet) in lines) {
            val row = list.single { it.subject == SightSubject.Wanderer(planet) }
            val line = SkyScheduler.resolve(job, evening, rome, milan) as SkyOccurrence.At
            assertEquals(row.window.start, line.start)
            assertEquals(row.window.endInclusive, line.end)
        }
    }

    /**
     * Three years of Milan, a night a week. Mars is on the list for part of them, too low
     * for part and too faint for part, and each night the line says which; Saturn, which
     * is never fainter than the stars the app names, is never refused for that.
     */
    @Test
    fun `over three years Mars is worth it, too low or too faint, and Saturn is never too faint`() {
        val seen = mutableMapOf<Planet, MutableSet<String>>()
        var date = LocalDate.of(2026, 1, 1)
        val faint = mutableListOf<LocalDate>()
        while (date.year < 2029) {
            for ((job, planet) in lines) {
                val line = SkyScheduler.resolve(job, date, rome, milan)
                val night = WorthSeeing.planetNight(planet, date, rome, milan)
                val outcome = when (line) {
                    is SkyOccurrence.At -> {
                        assertTrue("$planet on $date: a window with no row", night is WorthSeeing.PlanetNight.Up)
                        assertTrue(Duration.between(line.start, line.end) >= WorthSeeing.MIN_WINDOW)
                        "up"
                    }
                    is SkyOccurrence.None -> {
                        assertTrue("$planet on $date: a row with no window", night !is WorthSeeing.PlanetNight.Up)
                        if (planet == Planet.MARS && line.reason == SkyNotScheduled.PLANET_TOO_FAINT) faint += date
                        line.reason.name
                    }
                }
                seen.getOrPut(planet) { mutableSetOf() } += outcome
            }
            date = date.plusDays(7)
        }
        assertEquals(setOf("up", "PLANET_TOO_LOW", "PLANET_TOO_FAINT"), seen.getValue(Planet.MARS))
        assertEquals(setOf("up", "PLANET_TOO_LOW"), seen.getValue(Planet.SATURN))
        assertEquals(setOf("up", "PLANET_TOO_LOW"), seen.getValue(Planet.JUPITER))
        // Where the faint nights fall: the months before and after Mars goes behind the sun.
        assertTrue("faint on ${faint.first()}..${faint.last()}, ${faint.size} weeks", faint.size in 4..60)
    }

    /**
     * The end of Jupiter's evening apparition of 2026 over Milan, and the start of its
     * morning one (the measurement of 12 Nov 2026, PLANNING, «Giove: la riga contro
     * l'elenco, la misura»). At civil dusk on 17 June it stands over ten degrees for
     * thirteen minutes, on the 20th for three: until that day the line gave those as
     * windows, with a reminder for each. A stretch under a quarter of an hour is not a
     * row of the list (its rule 8), and is not a window of the line.
     */
    @Test
    fun `Jupiter's last evenings and first mornings of a few minutes are not a window`() {
        fun line(date: LocalDate) = SkyScheduler.resolve(SkyJobCatalog.JupiterNight, date, rome, milan)
        val lastEvening = line(LocalDate.of(2026, 6, 16)) as SkyOccurrence.At
        assertTrue(Duration.between(lastEvening.start, lastEvening.end) >= WorthSeeing.MIN_WINDOW)
        assertTrue(Duration.between(lastEvening.start, lastEvening.end) < Duration.ofMinutes(20))
        for (day in 17..20) {
            val gone = line(LocalDate.of(2026, 6, day)) as SkyOccurrence.None
            assertEquals(SkyNotScheduled.PLANET_TOO_LOW, gone.reason)
        }
        // Behind the sun for two months, then back before dawn: four mornings too short.
        assertEquals(SkyNotScheduled.PLANET_TOO_LOW, (line(LocalDate.of(2026, 7, 20)) as SkyOccurrence.None).reason)
        for (day in 22..25) {
            assertEquals(SkyNotScheduled.PLANET_TOO_LOW, (line(LocalDate.of(2026, 8, day)) as SkyOccurrence.None).reason)
        }
        val firstMorning = line(LocalDate.of(2026, 8, 26)) as SkyOccurrence.At
        assertTrue(Duration.between(firstMorning.start, firstMorning.end) >= WorthSeeing.MIN_WINDOW)
        // The short stretches are really there: it is the length that keeps them out.
        val dusk = SkyAlmanac.solarDay(LocalDate.of(2026, 6, 18), rome, milan).civilDusk!!
        val dawn = SkyAlmanac.solarDay(LocalDate.of(2026, 6, 19), rome, milan).civilDawn!!
        val stretch = AstronomyEngine.watchRuns(SkyTarget.Wanderer(Planet.JUPITER), dusk..dawn, milan, 10.0).single()
        assertEquals(dusk, stretch.start)
        assertTrue(Duration.between(stretch.start, stretch.endInclusive) in Duration.ofMinutes(8)..Duration.ofMinutes(12))
    }

    @Test
    fun `a faint Mars is refused at the brightness the list refuses it at`() {
        var date = LocalDate.of(2026, 1, 1)
        while (date.year < 2029) {
            val night = WorthSeeing.planetNight(Planet.MARS, date, rome, milan)
            if (night == WorthSeeing.PlanetNight.TooFaint) {
                // High enough that night, and then it is the brightness that keeps it off.
                val dusk = SkyAlmanac.solarDay(date, rome, milan).nauticalDusk!!
                val dawn = SkyAlmanac.solarDay(date.plusDays(1), rome, milan).nauticalDawn!!
                val runs = AstronomyEngine.watchRuns(SkyTarget.Wanderer(Planet.MARS), dusk..dawn, milan, 10.0)
                assertTrue(runs.any { Duration.between(it.start, it.endInclusive) >= WorthSeeing.MIN_WINDOW })
                assertTrue(runs.any { AstronomyEngine.planetMagnitude(Planet.MARS, it.start) > WorthSeeing.PLANET_FAINTEST_MAG - 0.2 })
            }
            date = date.plusDays(7)
        }
    }

    @Test
    fun `each line says which way to turn`() {
        val at = Instant.parse("2026-11-11T00:30:00Z")
        assertEquals(
            AstronomyEngine.horizontal(SkyTarget.Wanderer(Planet.MARS), at, milan).azimuthDeg,
            SkySights.bearing(SkyJobCatalog.MarsNight, at, milan)!!,
            1e-9
        )
        assertEquals(
            AstronomyEngine.horizontal(SkyTarget.Wanderer(Planet.SATURN), at, milan).azimuthDeg,
            SkySights.bearing(SkyJobCatalog.SaturnNight, at, milan)!!,
            1e-9
        )
    }

    @Test
    fun `the two lines want a clear sky and not a dark one`() {
        for (job in listOf(SkyJobCatalog.MarsNight, SkyJobCatalog.SaturnNight)) {
            assertTrue(job.visibilityDependent)
            assertTrue(job.observable)
            assertTrue(!job.needsDarkness && !job.photographic)
            assertEquals(SkyJobKind.DAILY, job.kind)
            assertEquals(SkyJobShape.RANGE, job.shape)
        }
    }

    /** One hour's sky is judged by the thresholds a window's is: the explorer's chip. */
    @Test
    fun `an hour's weather is a verdict by the window's own thresholds`() {
        assertEquals(SkyVerdictKind.PASS, SkyVerdictEngine.ofWeather(SkyVerdictEngine.CLOUD_PASS_PCT, null).kind)
        assertEquals(SkyVerdictKind.UNSTABLE, SkyVerdictEngine.ofWeather(SkyVerdictEngine.CLOUD_PASS_PCT + 1, 0).kind)
        assertEquals(SkyVerdictKind.UNSTABLE, SkyVerdictEngine.ofWeather(SkyVerdictEngine.CLOUD_FAIL_PCT, 0).kind)
        assertEquals(SkyVerdictKind.FAIL, SkyVerdictEngine.ofWeather(SkyVerdictEngine.CLOUD_FAIL_PCT + 1, 0).kind)
        val wet = SkyVerdictEngine.ofWeather(10, SkyVerdictEngine.PRECIP_FAIL_PCT)
        assertEquals(SkyVerdictKind.FAIL, wet.kind)
        assertEquals(SkyVerdictNote.PRECIPITATION, wet.note)
        val showery = SkyVerdictEngine.ofWeather(10, SkyVerdictEngine.PRECIP_UNSTABLE_PCT)
        assertEquals(SkyVerdictKind.UNSTABLE, showery.kind)
        assertEquals(SkyVerdictNote.PRECIPITATION, showery.note)
        assertEquals(12, SkyVerdictEngine.ofWeather(12, null).cloudPct)
    }
}
