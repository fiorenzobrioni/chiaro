package com.callbackdev.chiaro.domain.sky

import com.callbackdev.chiaro.domain.model.Coordinates
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The sights of Fase 28, each one checked against what it CLAIMS rather than against a
 * remembered date: a test that pins "the full moon rises at 18:35 on 25 September" pins
 * this code's own output and proves nothing about the sky.
 *
 * So every assertion here re-derives the definition — nearly full AND rising in the
 * twilight, a thin crescent AND inside twilight, close AND both of them up — from the
 * primitives the rest of the module already has tests for.
 */
class SkySightsTest {

    private val milan = Coordinates(45.4642, 9.19)
    private val rome = ZoneId.of("Europe/Rome")
    private val from = LocalDate.of(2026, 9, 20)

    // ------------------------------------------------- the full moon at dusk

    @Test
    fun `the full moon at dusk is nearly full, and really rises in the twilight`() {
        val moonrise = SkySights.nextFullMoonAtDusk(from, rome, milan)
        assertNotNull("a moonrise inside the twilight comes round every month", moonrise)

        val illumination = AstronomyEngine.moonIllumination(moonrise!!).illuminatedFraction
        assertTrue("the disc has to read as round, got $illumination", illumination >= 0.96)

        val date = moonrise.atZone(rome).toLocalDate()
        val day = SkyAlmanac.solarDay(date, rome, milan)
        // Inside the twilight, give or take the slack the search allows: after the sun
        // has gone and before the colour has.
        assertTrue(
            "the moon came up before the sun went down",
            moonrise.isAfter(day.sunset!!.minus(Duration.ofMinutes(45)))
        )
        assertTrue(
            "the moon came up after the twilight was over",
            moonrise.isBefore(day.civilDusk!!.plus(Duration.ofMinutes(45)))
        )
        // And it really is the moon's own rise, to the minute the moonrise row prints.
        val row = SkyAlmanac.lunarDay(date, rome, milan).moonrise
        assertEquals(row, moonrise)
    }

    // ------------------------------------------------------------ earthshine

    @Test
    fun `earthshine only happens on a thin crescent, inside the twilight`() {
        var found = 0
        (0..60).forEach { offset ->
            val date = from.plusDays(offset.toLong())
            val result = SkySights.earthshine(date, rome, milan, evening = true)
            if (result !is SkySights.SightResult.At) return@forEach
            found++
            val day = SkyAlmanac.solarDay(date, rome, milan)
            val illumination =
                AstronomyEngine.moonIllumination(result.start).illuminatedFraction
            assertTrue(
                "$date: the lit limb drowns the ashen light past a fifth ($illumination)",
                illumination in 0.01..0.18
            )
            assertEquals("the window opens at sunset", day.sunset, result.start)
            assertTrue("the window runs backwards", result.end.isAfter(result.start))
            assertTrue(
                "$date: the window has to end by nautical dusk or moonset",
                !result.end.isAfter(day.nauticalDusk) ||
                    !result.end.isAfter(SkyAlmanac.lunarDay(date, rome, milan).moonset)
            )
            // The moon has to still be up when the window opens, or there is nothing there.
            assertTrue(
                "$date: the moon was already down",
                AstronomyEngine.moonAltitude(result.start, milan) > -1.0
            )
        }
        assertTrue("two months should hold at least one earthshine evening", found >= 1)
    }

    @Test
    fun `a fat moon is not earthshine, and says so`() {
        // The night of the full moon: as far from a thin crescent as the month goes.
        val result = SkySights.earthshine(LocalDate.of(2026, 9, 26), rome, milan, evening = true)
        assertEquals(
            SkySights.SightResult.None(SkyNotScheduled.MOON_NOT_CRESCENT),
            result
        )
    }

    // --------------------------------------------------------------- planets

    /**
     * Venus is never both at once, and that is not a coincidence to be grateful for:
     * it is the geometry that makes it a morning OR an evening star and never a
     * midnight one. If the model ever let both windows open on one day, the elongation
     * ceiling in `PlanetMathTest` would have been passed too.
     */
    @Test
    fun `Venus is a morning star or an evening star, never both`() {
        var evenings = 0
        var mornings = 0
        (0..400 step 5).forEach { offset ->
            val date = from.plusDays(offset.toLong())
            val evening = SkySights.planetWindow(Planet.VENUS, date, rome, milan, evening = true)
            val morning = SkySights.planetWindow(Planet.VENUS, date, rome, milan, evening = false)
            if (evening is SkySights.SightResult.At) evenings++
            if (morning is SkySights.SightResult.At) mornings++
            assertTrue(
                "$date: Venus cannot be an evening AND a morning star",
                evening !is SkySights.SightResult.At || morning !is SkySights.SightResult.At
            )
        }
        // And over a synodic period it must be both at some point, or the windows are
        // not finding a planet that is certainly there.
        assertTrue("Venus was never an evening star in 400 days", evenings > 0)
        assertTrue("Venus was never a morning star in 400 days", mornings > 0)
    }

    @Test
    fun `a planet window really has the planet above its own floor`() {
        val window = (0..400 step 3).firstNotNullOf { offset ->
            SkySights.planetWindow(
                Planet.JUPITER, from.plusDays(offset.toLong()), rome, milan,
                evening = true, wholeNight = true
            ) as? SkySights.SightResult.At
        }
        listOf(0.1, 0.5, 0.9).forEach { fraction ->
            val span = Duration.between(window.start, window.end)
            val at = window.start.plus(Duration.ofMillis((span.toMillis() * fraction).toLong()))
            assertTrue(
                "Jupiter is under its floor at $fraction of its own window",
                AstronomyEngine.planetAltitude(Planet.JUPITER, at, milan) >= 9.9
            )
        }
    }

    // -------------------------------------------------------------- bearings

    /**
     * The bearing is the honest half of the camera mark: a flag saying an event is
     * worth photographing, with no direction to point in, is an opinion.
     *
     * What is asserted is the shape of the answer, not a remembered azimuth — null
     * where a direction would be noise, a real bearing where it is the content, and
     * the evening golden hour genuinely westerly at a northern latitude, which is the
     * one thing a wrong body or a wrong sign would break.
     */
    @Test
    fun `a bearing is given where it is the content, and withheld where it is noise`() {
        val date = LocalDate.of(2026, 9, 20)
        val goldenPm = SkyScheduler.resolve(SkyJobCatalog.GoldenPm, date, rome, milan)
                as SkyOccurrence.At
        val middle = goldenPm.start.plus(
            Duration.between(goldenPm.start, goldenPm.end!!).dividedBy(2)
        )
        val bearing = SkySights.bearing(SkyJobCatalog.GoldenPm, middle, milan)
        assertNotNull("the evening golden hour has a direction", bearing)
        assertTrue(
            "the evening sun must be in the west, got ${"%.0f".format(bearing)}\u00b0",
            bearing!! in 200.0..320.0
        )

        // And the rows that do not happen in a direction say nothing.
        listOf(
            SkyJobCatalog.EquinoxSpring, SkyJobCatalog.SolsticeWinter,
            SkyJobCatalog.Perihelion, SkyJobCatalog.MoonPhase, SkyJobCatalog.SolarNoon,
            SkyJobCatalog.DarknessWindow, SkyJobCatalog.SunRise
        ).forEach { job ->
            assertEquals(
                "${job.id} should have no bearing to print",
                null,
                SkySights.bearing(job, middle, milan)
            )
        }

        // Every photographic event does have one: the flag promises a camera, and a
        // camera needs a direction.
        SkyJobCatalog.all.filter { it.photographic }.forEach { job ->
            assertNotNull(
                "${job.id} is flagged photographic but has no bearing",
                SkySights.bearing(job, middle, milan)
            )
        }
    }

    // ----------------------------------------------------------- conjunctions

    /**
     * A conjunction is two claims — they are close, and you can see them being close —
     * and the second is the one that keeps the row honest. The moon passes Venus every
     * month and about half of those happen behind the sun, where the event is real,
     * computable and invisible.
     */
    @Test
    fun `every conjunction is close, and visible from here`() {
        listOf(
            SkyBody.MOON to SkyBody.VENUS,
            SkyBody.MOON to SkyBody.JUPITER,
            SkyBody.VENUS to SkyBody.JUPITER
        ).forEach { (a, b) ->
            val close = SkySights.nextConjunction(
                a, b, from.atStartOfDay(rome).toInstant(), rome, milan
            )
            assertNotNull("$a and $b never meet in three years?", close)
            assertTrue(
                "$a-$b came no closer than ${close!!.separationDeg}°",
                close.separationDeg <= 5.0
            )
            // The separation really is a minimum: a day either side is wider.
            listOf(-1L, 1L).forEach { days ->
                val away = close.at.plus(Duration.ofDays(days))
                assertTrue(
                    "$a-$b is not closest at the instant it claims",
                    AstronomyEngine.separation(a, b, away) > close.separationDeg
                )
            }
            // And both are genuinely up, with the sun down, somewhere in its window.
            val middle = close.window.start.plus(
                Duration.between(close.window.start, close.window.endInclusive).dividedBy(2)
            )
            assertTrue(
                "$a is not up in its own visible window",
                AstronomyEngine.bodyAltitude(a, middle, milan) >= 4.0
            )
            assertTrue(
                "$b is not up in its own visible window",
                AstronomyEngine.bodyAltitude(b, middle, milan) >= 4.0
            )
            assertTrue(
                "the sun is up in a conjunction's visible window",
                AstronomyEngine.sunAltitude(middle, milan) < 0.0
            )
        }
    }
}
