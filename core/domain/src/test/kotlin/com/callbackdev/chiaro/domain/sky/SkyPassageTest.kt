package com.callbackdev.chiaro.domain.sky

import com.callbackdev.chiaro.domain.model.Coordinates
import java.time.Duration
import java.time.Instant
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * When a thing rises, stands highest and sets around a night, and until when it is worth
 * going out for (PLANNING, Fase 16, step 16.3).
 *
 * The planets' and the moon's rows are **JPL Horizons', read on 10 Nov 2026**
 * (`ssd.jpl.nasa.gov/api/horizons.api`, observer table at the place's own coordinates,
 * `R_T_S_ONLY='GEO'`, one-minute steps, refracted; quantity 4, the apparent azimuth and
 * elevation; targets 299, 499, 599, 699 and 301): Milan (45.4642 N, 9.19 E) over the night
 * of 10 November 2026, with Saturn in the evening and Mars and Jupiter rising before
 * midnight, and Sydney (33.8688 S, 151.2093 E) over the night of 19 February 2027, Mars'
 * opposition, where everything tops in the north. Horizons prints the minute; a row is its
 * minute at second zero. Measured: every row is within one minute, the moon's included;
 * the tolerances are two for a planet and three for the moon.
 *
 * A fixed point has no ephemeris to be read from and needs none: its passage is spherical
 * trigonometry, and the rows are that formula's.
 */
class SkyPassageTest {

    private val milan = Coordinates(45.4642, 9.19)
    private val sydney = Coordinates(-33.8688, 151.2093)

    /** Astronomical dusk to dawn, to the half hour: the middle is what the passage reads. */
    private val milanNight = Instant.parse("2026-11-10T17:30:00Z")..Instant.parse("2026-11-11T04:30:00Z")
    private val sydneyNight = Instant.parse("2027-02-19T10:15:00Z")..Instant.parse("2027-02-19T18:15:00Z")

    private class Expected(val at: String, val azimuthDeg: Double, val altitudeDeg: Double? = null)

    private fun assertPoint(
        name: String,
        expected: Expected,
        actual: SkyPassagePoint?,
        minutes: Long,
        altitudeTolerance: Double = 0.3
    ) {
        assertNotNull("$name is missing", actual)
        val gap = Duration.between(Instant.parse(expected.at), actual!!.at).abs()
        assertTrue("$name at ${actual.at}, Horizons ${expected.at}", gap <= Duration.ofMinutes(minutes))
        val turn = abs(AstronomyMath.norm180(actual.azimuthDeg - expected.azimuthDeg))
        assertTrue("$name faces ${actual.azimuthDeg}, Horizons ${expected.azimuthDeg}", turn < 0.6)
        expected.altitudeDeg?.let { assertEquals("$name altitude", it, actual.altitudeDeg, altitudeTolerance) }
    }

    // ------------------------------------------------------------ the planets, Milan

    /**
     * Saturn on an autumn evening: it rose in the afternoon, before the night began, and
     * that afternoon hour is the answer. Up at dusk, highest in the south, down after two.
     */
    @Test
    fun `Saturn over Milan rises in the afternoon, tops in the evening and sets in the night`() {
        val passage = AstronomyEngine.passage(SkyTarget.Wanderer(Planet.SATURN), milanNight, milan)
        assertPoint("rise", Expected("2026-11-10T14:34:00Z", 88.13), passage.rise, minutes = 2)
        assertPoint("highest", Expected("2026-11-10T20:40:00Z", 180.21, 45.57), passage.highest, minutes = 2)
        assertPoint("set", Expected("2026-11-11T02:46:00Z", 272.07), passage.set, minutes = 2)
        assertFalse(passage.neverUp)
        assertFalse(passage.alwaysUp)
    }

    /** Mars and Jupiter, a quarter of an hour apart that night: up before midnight, highest at dawn. */
    @Test
    fun `Mars and Jupiter over Milan rise before midnight and set the next afternoon`() {
        val mars = AstronomyEngine.passage(SkyTarget.Wanderer(Planet.MARS), milanNight, milan)
        assertPoint("Mars rise", Expected("2026-11-10T22:40:00Z", 67.13), mars.rise, minutes = 2)
        assertPoint("Mars highest", Expected("2026-11-11T05:48:00Z", 180.08, 59.92), mars.highest, minutes = 2)
        assertPoint("Mars set", Expected("2026-11-11T12:56:00Z", 292.88), mars.set, minutes = 2)

        val jupiter = AstronomyEngine.passage(SkyTarget.Wanderer(Planet.JUPITER), milanNight, milan)
        assertPoint("Jupiter rise", Expected("2026-11-10T22:54:00Z", 69.68), jupiter.rise, minutes = 2)
        assertPoint("Jupiter highest", Expected("2026-11-11T05:54:00Z", 180.24, 58.27), jupiter.highest, minutes = 2)
        assertPoint("Jupiter set", Expected("2026-11-11T12:54:00Z", 290.49), jupiter.set, minutes = 2)
    }

    /**
     * Venus as a morning star: of its two passages the night's is the one that begins before
     * dawn, not the one that ended the afternoon before, although that one's highest point
     * is nearly as near.
     */
    @Test
    fun `a morning star's passage is the one that begins at the end of the night`() {
        val venus = AstronomyEngine.passage(SkyTarget.Wanderer(Planet.VENUS), milanNight, milan)
        assertPoint("rise", Expected("2026-11-11T04:07:00Z", 105.47), venus.rise, minutes = 2)
        assertPoint("highest", Expected("2026-11-11T09:23:00Z", 180.07, 33.50), venus.highest, minutes = 2)
        assertPoint("set", Expected("2026-11-11T14:40:00Z", 254.87), venus.set, minutes = 2)
    }

    // ------------------------------------------------------------------- the moon

    /**
     * The moon a day after new: up and down with the sun, before the night began. Horizons'
     * elevation is from the surface and the engine's from the earth's centre, which at
     * eighteen degrees is nine tenths of a degree higher: the tolerance is that.
     */
    @Test
    fun `the moon over Milan rose and set in the day before the night`() {
        val moon = AstronomyEngine.passage(SkyTarget.Moon, milanNight, milan)
        assertPoint("rise", Expected("2026-11-10T07:44:00Z", 127.15), moon.rise, minutes = 3)
        assertPoint("highest", Expected("2026-11-10T11:57:00Z", 179.62, 18.36), moon.highest, minutes = 3, altitudeTolerance = 1.2)
        assertPoint("set", Expected("2026-11-10T16:09:00Z", 231.59), moon.set, minutes = 3)
    }

    // --------------------------------------------------------- the south: Sydney

    /** In the south a thing tops in the north: the highest point faces 0, not 180. */
    @Test
    fun `Mars at opposition over Sydney is up from dusk to dawn and tops in the north`() {
        val mars = AstronomyEngine.passage(SkyTarget.Wanderer(Planet.MARS), sydneyNight, sydney)
        assertPoint("rise", Expected("2027-02-19T08:57:00Z", 71.78), mars.rise, minutes = 2)
        assertPoint("highest", Expected("2027-02-19T14:16:00Z", 359.96, 40.79), mars.highest, minutes = 2)
        assertPoint("set", Expected("2027-02-19T19:35:00Z", 288.23), mars.set, minutes = 2)
    }

    @Test
    fun `Saturn and the moon over Sydney`() {
        val saturn = AstronomyEngine.passage(SkyTarget.Wanderer(Planet.SATURN), sydneyNight, sydney)
        assertPoint("Saturn rise", Expected("2027-02-18T22:53:00Z", 87.14), saturn.rise, minutes = 2)
        assertPoint("Saturn highest", Expected("2027-02-19T04:48:00Z", 359.68, 53.50), saturn.highest, minutes = 2)
        assertPoint("Saturn set", Expected("2027-02-19T10:42:00Z", 272.81), saturn.set, minutes = 2)

        val moon = AstronomyEngine.passage(SkyTarget.Moon, sydneyNight, sydney)
        assertPoint("moon rise", Expected("2027-02-19T07:36:00Z", 66.10), moon.rise, minutes = 3)
        assertPoint("moon highest", Expected("2027-02-19T12:56:00Z", 358.86, 36.95), moon.highest, minutes = 3, altitudeTolerance = 1.2)
        assertPoint("moon set", Expected("2027-02-19T18:16:00Z", 290.88), moon.set, minutes = 3)
    }

    // --------------------------------------------------------------- a fixed point

    private val sirius = SkyTarget.Fixed(101.2872, -16.7161)
    private val polaris = SkyTarget.Fixed(37.9546, 89.2641)
    private val canopus = SkyTarget.Fixed(95.9880, -52.6957)

    /**
     * A star's passage is geometry. It tops on the meridian at 90° less the latitude plus its
     * declination; it rises and sets the same time either side of that; and the time is the
     * hour angle at which its altitude is the almanac's horizon, turned into clock time at
     * the stars' rate, 360.9856° a day.
     */
    @Test
    fun `a star's passage is the one spherical trigonometry gives`() {
        val night = Instant.parse("2027-01-15T18:00:00Z")..Instant.parse("2027-01-16T05:00:00Z")
        val passage = AstronomyEngine.passage(sirius, night, milan)
        val rise = passage.rise!!
        val set = passage.set!!

        assertEquals(90.0 - 45.4642 - 16.7161, passage.highest.altitudeDeg, 0.01)
        assertEquals(180.0, passage.highest.azimuthDeg, 0.05)
        assertTrue("it tops in the night it was asked about", passage.highest.at in night)

        val lat = Math.toRadians(45.4642)
        val dec = Math.toRadians(-16.7161)
        val horizon = Math.toRadians(AstronomyEngine.POINT_RISE_ALTITUDE)
        val hourAngle = Math.toDegrees(acos((sin(horizon) - sin(lat) * sin(dec)) / (cos(lat) * cos(dec))))
        val halfArcSeconds = hourAngle / 360.9856 * 86_400.0
        assertEquals(halfArcSeconds, Duration.between(rise.at, passage.highest.at).seconds.toDouble(), 5.0)
        assertEquals(halfArcSeconds, Duration.between(passage.highest.at, set.at).seconds.toDouble(), 5.0)
        assertEquals(AstronomyEngine.POINT_RISE_ALTITUDE, rise.altitudeDeg, 0.01)
        assertEquals(AstronomyEngine.POINT_RISE_ALTITUDE, set.altitudeDeg, 0.01)
        // It comes up south of east and goes down as far south of west.
        assertEquals(360.0, rise.azimuthDeg + set.azimuthDeg, 0.05)
        assertTrue(rise.azimuthDeg in 90.0..180.0)
    }

    @Test
    fun `a star round the pole never touches the horizon, and one too far south never clears it`() {
        val night = Instant.parse("2027-01-15T18:00:00Z")..Instant.parse("2027-01-16T05:00:00Z")

        val north = AstronomyEngine.passage(polaris, night, milan)
        assertTrue(north.alwaysUp)
        assertFalse(north.neverUp)
        assertNull(north.rise)
        assertNull(north.set)

        val south = AstronomyEngine.passage(canopus, night, milan)
        assertTrue(south.neverUp)
        assertFalse(south.alwaysUp)
        assertNull(south.rise)
        assertNull(south.set)
        assertEquals(90.0 - 45.4642 - 52.6957, south.highest.altitudeDeg, 0.01)

        // From Sydney it is the Southern Cross that never sets: Acrux, 63° south, clears the
        // southern horizon by seven degrees at its lowest.
        assertTrue(AstronomyEngine.passage(SkyTarget.Fixed(186.6496, -63.0991), sydneyNight, sydney).alwaysUp)
    }

    /** At the pole nothing rises, sets or tops: the answer is still an answer. */
    @Test
    fun `at the pole a star stands still and the passage is still total`() {
        val night = Instant.parse("2027-01-15T18:00:00Z")..Instant.parse("2027-01-16T05:00:00Z")
        val pole = Coordinates(90.0, 0.0)
        val up = AstronomyEngine.passage(SkyTarget.Fixed(101.2872, 20.0), night, pole)
        assertTrue(up.alwaysUp)
        assertEquals(20.0, up.highest.altitudeDeg, 0.01)
        assertTrue(AstronomyEngine.passage(sirius, night, pole).neverUp)
    }

    /** The passage and the engine's own altitude are one model: the highest point is the highest. */
    @Test
    fun `nothing in the day around it stands higher than the highest point`() {
        listOf(SkyTarget.Moon, SkyTarget.Wanderer(Planet.MARS), SkyTarget.Wanderer(Planet.SATURN), sirius).forEach { target ->
            val top = AstronomyEngine.passage(target, milanNight, milan).highest
            var at = top.at.minus(Duration.ofHours(6))
            val end = top.at.plus(Duration.ofHours(6))
            while (at.isBefore(end)) {
                assertTrue("$target at $at", AstronomyEngine.horizontal(target, at, milan).altitudeDeg <= top.altitudeDeg + 1e-6)
                at = at.plus(Duration.ofMinutes(7))
            }
        }
    }

    // ------------------------------------------------------- the old trap, pinned

    /**
     * `planetAbove` used to send every planet that was not Venus to Jupiter. On this night
     * Saturn is up at dusk and Jupiter is not up until midnight: Saturn's window has to
     * open with the night, and at its end it is Saturn, not Jupiter, that is on the floor.
     */
    @Test
    fun `a planet's window is that planet's`() {
        val saturn = AstronomyEngine.planetAbove(milanNight.start, milanNight.endInclusive, milan, Planet.SATURN, 10.0)!!
        assertEquals(milanNight.start, saturn.start)
        assertEquals(10.0, AstronomyEngine.planetAltitude(Planet.SATURN, saturn.endInclusive, milan), 0.01)
        assertTrue(AstronomyEngine.planetAltitude(Planet.JUPITER, saturn.start, milan) < 0)

        val mars = AstronomyEngine.planetAbove(milanNight.start, milanNight.endInclusive, milan, Planet.MARS, 10.0)!!
        assertEquals(10.0, AstronomyEngine.planetAltitude(Planet.MARS, mars.start, milan), 0.01)
        assertEquals(milanNight.endInclusive, mars.endInclusive)
        val jupiter = AstronomyEngine.planetAbove(milanNight.start, milanNight.endInclusive, milan, Planet.JUPITER, 10.0)!!
        assertTrue("Mars is up before Jupiter that night", mars.start.isBefore(jupiter.start))
    }

    @Test
    fun `every named body is itself, in altitude, bearing and separation`() {
        val at = Instant.parse("2026-11-11T03:00:00Z")
        SkyBody.values().filter { it.planet != null }.forEach { body ->
            val planet = body.planet!!
            assertEquals(AstronomyEngine.planetAltitude(planet, at, milan), AstronomyEngine.bodyAltitude(body, at, milan), 0.0)
            assertEquals(AstronomyEngine.planetAzimuth(planet, at, milan), AstronomyEngine.bodyAzimuth(body, at, milan), 0.0)
            assertEquals(body.name, planet.name)
        }
        assertEquals(Planet.values().size + 1, SkyBody.values().size)
        // Horizons' geocentric places at that instant (read 10 Nov 2026): Mars 146.4898,
        // +15.3901; Jupiter 147.9482, +13.7272; Saturn 9.0485, +1.0102. Mars and Jupiter
        // stand 2.18° apart, a pair in one field, and Saturn 134.85° from Mars.
        assertEquals(2.18, AstronomyEngine.separation(SkyBody.MARS, SkyBody.JUPITER, at), 0.08)
        assertEquals(134.85, AstronomyEngine.separation(SkyBody.MARS, SkyBody.SATURN, at), 0.2)
    }

    // ---------------------------------------------------- until when it is worth it

    /** Saturn that evening: over the roofs from the start of the dark, until it sinks to the floor. */
    @Test
    fun `a planet that is up at dusk is worth it from dusk until it sinks to the floor`() {
        val saturn = SkyTarget.Wanderer(Planet.SATURN)
        val runs = AstronomyEngine.watchRuns(saturn, milanNight, milan, 10.0)
        assertEquals(1, runs.size)
        assertEquals(milanNight.start, runs.single().start)
        val until = AstronomyEngine.worthUntil(saturn, milanNight, milan, 10.0)!!
        assertEquals(runs.single().endInclusive, until)
        assertEquals(10.0, AstronomyEngine.horizontal(saturn, until, milan).altitudeDeg, 0.01)
        // After its highest point, and before it sets: an hour or so before, at that latitude.
        val passage = AstronomyEngine.passage(saturn, milanNight, milan)
        assertTrue(until.isAfter(passage.highest.at) && until.isBefore(passage.set!!.at))
        assertTrue(Duration.between(until, passage.set!!.at) in Duration.ofMinutes(45)..Duration.ofMinutes(75))
    }

    @Test
    fun `a planet that outlasts the night is worth it until the night ends`() {
        val mars = SkyTarget.Wanderer(Planet.MARS)
        val runs = AstronomyEngine.watchRuns(mars, milanNight, milan, 10.0)
        assertEquals(1, runs.size)
        assertEquals(10.0, AstronomyEngine.horizontal(mars, runs.single().start, milan).altitudeDeg, 0.01)
        assertEquals(milanNight.endInclusive, AstronomyEngine.worthUntil(mars, milanNight, milan, 10.0))
    }

    @Test
    fun `a thing that never clears the floor is never worth it`() {
        assertTrue(AstronomyEngine.watchRuns(canopus, milanNight, milan, 10.0).isEmpty())
        assertNull(AstronomyEngine.worthUntil(canopus, milanNight, milan, 10.0))
        // The moon that night had set before the dark began.
        assertNull(AstronomyEngine.worthUntil(SkyTarget.Moon, milanNight, milan, 5.0))
    }

    /**
     * A night long enough to see a star go down and come back: in the dark of a polar winter
     * Sirius, low at that latitude, clears two degrees twice in thirty hours. The last run's
     * end is the answer, and the first one's is not.
     */
    @Test
    fun `a long dark can hold two runs, and the last one's end is until when`() {
        val tromso = Coordinates(69.6492, 18.9553)
        val dark = Instant.parse("2026-12-20T16:00:00Z")..Instant.parse("2026-12-21T22:00:00Z")
        val runs = AstronomyEngine.watchRuns(sirius, dark, tromso, 2.0)
        assertEquals(2, runs.size)
        assertTrue(runs[0].endInclusive.isBefore(runs[1].start))
        assertEquals(runs[1].endInclusive, AstronomyEngine.worthUntil(sirius, dark, tromso, 2.0))
        runs.forEach { run ->
            val middle = run.start.plus(Duration.between(run.start, run.endInclusive).dividedBy(2))
            assertTrue(AstronomyEngine.horizontal(sirius, middle, tromso).altitudeDeg > 2.0)
        }
    }
}
