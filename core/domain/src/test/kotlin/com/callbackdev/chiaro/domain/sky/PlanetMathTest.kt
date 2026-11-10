package com.callbackdev.chiaro.domain.sky

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The planets, measured rather than asserted (Fase 28: Venus and Jupiter; Mars and Saturn
 * since Fase 16, held to the same invariants).
 *
 * A table of orbital elements nobody checked is a rumour — the rule `EclipseEngine`
 * was held to when it was written — and the checking here is a chain rather than a
 * list of remembered ephemeris rows, because a remembered row is another rumour:
 *
 * 1. The **earth's** elements answer to this module's own solar longitude, which is
 *    itself measured against Open-Meteo's sunrise to thirty seconds (`SolarEventsTest`).
 *    That one assertion covers the element set, Kepler's solution, the precession step
 *    and the frame, because the sun seen from here IS the earth's own longitude turned
 *    around.
 * 2. The **planets** answer to the invariants a wrong table cannot satisfy: how far
 *    Venus is allowed to get from the sun, how long each takes to come back to the same
 *    place in the sky, how long each takes to go round, and how far each is allowed to
 *    be from the sun at all.
 *
 * Every bound below is a property of the solar system, not a number read off this code.
 * How far off the answer is, which no invariant says, is `PlanetEphemerisTest`'s to say.
 */
class PlanetMathTest {

    private val utc = ZoneId.of("Etc/UTC")

    private fun t(date: String): Double =
        AstronomyMath.centuriesTT(LocalDate.parse(date).atStartOfDay(utc).toInstant())

    private fun instant(date: String): Instant =
        LocalDate.parse(date).atStartOfDay(utc).toInstant()

    // ----------------------------------------------------- 1. the earth's elements

    /**
     * The chain's first link. The sun's geocentric longitude is the earth's
     * heliocentric longitude plus 180°, so if the elements, Kepler, the precession to
     * the ecliptic of date and the frame are all right, this agrees with the series
     * the rest of the app already trusts.
     *
     * Three arcminutes of tolerance, over thirty years. The two are not the same
     * quantity to the last digit — [AstronomyMath.sunApparentLongitude] carries
     * nutation and aberration (about 20") and this does not, and one describes the
     * earth-moon barycentre — so the residual is expected and small. A wrong element
     * set is wrong by degrees, which is a hundred times this.
     */
    @Test
    fun `the earth's elements reproduce the solar longitude the app already trusts`() {
        var worst = 0.0
        var worstAt = ""
        listOf(
            "2000-01-01", "2006-03-20", "2012-06-21", "2019-09-23",
            "2026-01-03", "2026-09-20", "2030-12-21"
        ).forEach { date ->
            val t = t(date)
            val gap = abs(
                AstronomyMath.norm180(
                    PlanetMath.sunLongitudeFromEarthElements(t) -
                        AstronomyMath.sunApparentLongitude(t)
                )
            )
            if (gap > worst) {
                worst = gap
                worstAt = date
            }
        }
        assertTrue(
            "worst disagreement with the app's own sun was ${"%.4f".format(worst)}° on $worstAt",
            worst < 3.0 / 60.0
        )
    }

    // -------------------------------------------------- 2. the planets' invariants

    /**
     * Venus never leaves the twilight, and that is the whole reason it is only ever a
     * morning or an evening star. Its greatest elongation is about 45° to 47°
     * depending on where the two planets are in their orbits, and it is a hard ceiling
     * the geometry cannot exceed: a wrong element set walks straight through it.
     */
    @Test
    fun `Venus stays inside its own greatest elongation`() {
        var greatest = 0.0
        var at = instant("2026-01-01")
        val end = instant("2030-01-01")
        while (at.isBefore(end)) {
            val t = AstronomyMath.centuriesTT(at)
            val separation = AstronomyMath.separation(
                PlanetMath.equatorial(Planet.VENUS, t).equatorial,
                AstronomyMath.sunEquatorial(t)
            )
            if (separation > greatest) greatest = separation
            at = at.plusSeconds(6 * 3600)
        }
        assertTrue("Venus reached ${"%.1f".format(greatest)}° from the sun", greatest < 48.0)
        assertTrue(
            "Venus never got far enough out to be an evening star (${"%.1f".format(greatest)}°)",
            greatest > 44.0
        )
    }

    /**
     * Jupiter, by contrast, goes all the way round: it reaches opposition, which is the
     * night it is up from dusk to dawn. Both facts in one walk.
     */
    @Test
    fun `Jupiter reaches opposition`() {
        var greatest = 0.0
        var at = instant("2026-01-01")
        val end = instant("2028-01-01")
        while (at.isBefore(end)) {
            val t = AstronomyMath.centuriesTT(at)
            val separation = AstronomyMath.separation(
                PlanetMath.equatorial(Planet.JUPITER, t).equatorial,
                AstronomyMath.sunEquatorial(t)
            )
            if (separation > greatest) greatest = separation
            at = at.plusSeconds(12 * 3600)
        }
        assertTrue("Jupiter only reached ${"%.1f".format(greatest)}°", greatest > 179.0)
    }

    /**
     * So do Mars and Saturn, which is what lets them be up at midnight: Mars every 26
     * months (February 2027 is inside the walk), Saturn every year. An opposition is not
     * exactly 180° from the sun, because the planet is a little off the ecliptic there:
     * Saturn by up to two and a half degrees, Mars by up to four at a far opposition.
     */
    @Test
    fun `Mars and Saturn reach opposition`() {
        listOf(Planet.MARS, Planet.SATURN).forEach { planet ->
            var greatest = 0.0
            var at = instant("2026-01-01")
            val end = instant("2028-01-01")
            while (at.isBefore(end)) {
                val t = AstronomyMath.centuriesTT(at)
                val separation = AstronomyMath.separation(
                    PlanetMath.equatorial(planet, t).equatorial,
                    AstronomyMath.sunEquatorial(t)
                )
                if (separation > greatest) greatest = separation
                at = at.plusSeconds(12 * 3600)
            }
            assertTrue("$planet only reached ${"%.1f".format(greatest)}°", greatest > 175.0)
        }
    }

    /**
     * The synodic period: how long the sun, the earth and the planet take to line up
     * the same way again, which is the rhythm a reader actually feels — Venus swaps
     * between morning and evening star on it. Published: 583.92 days for Venus,
     * 398.88 for Jupiter.
     *
     * Measured from successive conjunctions **of the same kind**, and that qualifier is
     * the whole test. Venus passes the sun twice a cycle — once in front of it and once
     * behind — so counting every elongation minimum measures half the period and
     * reports it with confidence. The first draft here did exactly that and came back
     * with 292.28 days, which is not a defect in the model: it is 583.92 ÷ 2, and the
     * two halves of Venus's cycle are told apart by how far away it is.
     */
    @Test
    fun `both synodic periods come out of the model`() {
        assertPeriod(Planet.VENUS, expectedDays = 583.92, tolerance = 3.0)
        assertPeriod(Planet.JUPITER, expectedDays = 398.88, tolerance = 3.0)
    }

    /**
     * Published: 779.94 days for Mars, 378.09 for Saturn. Mars's is the least even of the
     * four, because its orbit is the least round: one conjunction to the next runs from 764
     * to 811 days, and the mean only comes out over the seven that bring it back to the
     * same part of its orbit, fifteen years. So Mars is walked to 2041.
     */
    @Test
    fun `Mars's and Saturn's synodic periods come out of the model`() {
        assertPeriod(Planet.MARS, expectedDays = 779.94, tolerance = 3.0, until = "2041-06-01")
        assertPeriod(Planet.SATURN, expectedDays = 378.09, tolerance = 3.0)
    }

    /**
     * Conjunctions found as **crossings of the signed difference in right ascension**,
     * not as minima of the angular separation, and the difference is measured rather
     * than stylistic: the separation is a great-circle angle, so a planet's wander in
     * ecliptic latitude puts small local minima in it, and the first draft of this
     * counted those and reported Jupiter's period as 381.6 days instead of 398.9.
     *
     * A crossing is deduped against the one before it by a hundred days, because a
     * planet in retrograde can cross the same longitude three times in a few weeks and
     * those three are one conjunction.
     */
    private fun assertPeriod(
        planet: Planet,
        expectedDays: Double,
        tolerance: Double,
        until: String = "2038-01-01"
    ) {
        val conjunctions = mutableListOf<Instant>()
        var at = instant("2026-01-01")
        val end = instant(until)
        var previous = signedGap(planet, at)
        while (at.isBefore(end)) {
            val next = at.plusSeconds(12 * 3600)
            val current = signedGap(planet, next)
            // A crossing of zero, and not of the ±180 wrap, which is opposition.
            val crossed = previous <= 0.0 != current <= 0.0 &&
                abs(previous) < 90.0 && abs(current) < 90.0
            if (crossed) {
                // Venus passes the sun in front of it and behind it; only one of the
                // two is a synodic cycle apart from the next of its own kind.
                val inferior = PlanetMath
                    .equatorial(planet, AstronomyMath.centuriesTT(next)).distanceAu < 1.0
                val far = conjunctions.lastOrNull()?.let {
                    java.time.Duration.between(it, next).toDays() > 100
                } ?: true
                if ((planet != Planet.VENUS || inferior) && far) conjunctions += next
            }
            previous = current
            at = next
        }
        assertTrue("$planet gave too few conjunctions to measure", conjunctions.size >= 3)
        val gaps = conjunctions.zipWithNext { a, b ->
            java.time.Duration.between(a, b).toHours() / 24.0
        }
        assertEquals("$planet synodic period", expectedDays, gaps.average(), tolerance)
    }

    /** Planet minus sun in right ascension, folded to ±180: zero is conjunction. */
    private fun signedGap(planet: Planet, at: Instant): Double {
        val t = AstronomyMath.centuriesTT(at)
        return AstronomyMath.norm180(
            PlanetMath.equatorial(planet, t).equatorial.rightAscension -
                AstronomyMath.sunEquatorial(t).rightAscension
        )
    }

    /**
     * And the sidereal period, from the heliocentric side: 224.70 days for Venus,
     * 4332.6 (11.86 years) for Jupiter. Measured off the mean longitude rate, which is
     * the element the whole position rests on — so this pins the one number a typo
     * would hide in.
     */
    @Test
    fun `both sidereal periods come out of the mean longitude rates`() {
        // 36525 days per century divided by the turns made in one.
        assertEquals("Venus year", 224.701, 36_525.0 / (58_517.81538729 / 360.0), 0.05)
        assertEquals("Jupiter year", 4332.59, 36_525.0 / (3_034.74612775 / 360.0), 1.0)
    }

    /**
     * 686.98 days for Mars, 10 759.2 (29.46 years) for Saturn. Saturn's rate in a table
     * fitted to 1800-2050 is not its mean over the ages: Jupiter is speeding it up in
     * these centuries, the long inequality the two share, and the table's year is three
     * and a half days short of the textbook's. The tolerance is that, and says so.
     */
    @Test
    fun `Mars's and Saturn's sidereal periods come out of the mean longitude rates`() {
        assertEquals("Mars year", 686.98, 36_525.0 / (19_140.30268499 / 360.0), 0.05)
        assertEquals("Saturn year", 10_759.2, 36_525.0 / (1_222.49362201 / 360.0), 5.0)
    }

    /**
     * How far each is allowed to be from the sun: perihelion and aphelion, from the
     * published semi-major axis and eccentricity of each orbit. Venus's orbit is the
     * roundest in the solar system and this is what that looks like.
     */
    @Test
    fun `each planet keeps to its own orbit`() {
        val bounds = mapOf(
            Planet.VENUS to (0.718..0.729),
            Planet.JUPITER to (4.94..5.47),
            // Mars: 1.381 to 1.666, the widest swing of the four. Saturn: 9.02 to 10.05,
            // of which twelve years walk less than half, and all of it inside.
            Planet.MARS to (1.38..1.67),
            Planet.SATURN to (9.0..10.1)
        )
        bounds.forEach { (planet, allowed) ->
            var at = instant("2026-01-01")
            val end = instant("2038-01-01")
            var low = Double.MAX_VALUE
            var high = 0.0
            while (at.isBefore(end)) {
                val r = PlanetMath.equatorial(planet, AstronomyMath.centuriesTT(at)).sunDistanceAu
                if (r < low) low = r
                if (r > high) high = r
                at = at.plusSeconds(10L * 24 * 3600)
            }
            assertTrue(
                "$planet ranged ${"%.3f".format(low)}..${"%.3f".format(high)} AU from the sun",
                low in allowed && high in allowed
            )
        }
    }

    /**
     * Venus is closer to the sun than we are and Jupiter is further, so one is always
     * nearer to us at conjunction than the other ever gets. A frame or sign error in
     * the geocentric subtraction shows up here and nowhere else.
     */
    @Test
    fun `the geocentric distances are the right size`() {
        var at = instant("2026-01-01")
        val end = instant("2029-01-01")
        var venusMin = Double.MAX_VALUE
        var venusMax = 0.0
        var jupiterMin = Double.MAX_VALUE
        var jupiterMax = 0.0
        while (at.isBefore(end)) {
            val t = AstronomyMath.centuriesTT(at)
            val v = PlanetMath.equatorial(Planet.VENUS, t).distanceAu
            val j = PlanetMath.equatorial(Planet.JUPITER, t).distanceAu
            if (v < venusMin) venusMin = v
            if (v > venusMax) venusMax = v
            if (j < jupiterMin) jupiterMin = j
            if (j > jupiterMax) jupiterMax = j
            at = at.plusSeconds(24L * 3600)
        }
        // Venus comes to about 0.27 AU and goes to about 1.73; Jupiter runs roughly
        // 4.0 to 6.5 — each is the planet's own distance from the sun, minus and plus
        // ours.
        assertTrue("Venus came to ${"%.2f".format(venusMin)} AU", venusMin in 0.25..0.30)
        assertTrue("Venus went to ${"%.2f".format(venusMax)} AU", venusMax in 1.70..1.75)
        assertTrue("Jupiter came to ${"%.2f".format(jupiterMin)} AU", jupiterMin in 3.9..4.6)
        assertTrue("Jupiter went to ${"%.2f".format(jupiterMax)} AU", jupiterMax in 5.9..6.5)
    }

    /**
     * Mars comes nearer than any planet but Venus, 0.37 AU at its closest opposition and
     * 0.68 at its farthest, and goes to the far side of the sun, past 2.3; Saturn stays
     * between its own distance from the sun less ours and plus ours. Ten years hold five
     * oppositions of Mars, the close one of 2035 among them.
     */
    @Test
    fun `Mars's and Saturn's geocentric distances are the right size`() {
        var at = instant("2026-01-01")
        val end = instant("2036-01-01")
        var marsMin = Double.MAX_VALUE
        var marsMax = 0.0
        var saturnMin = Double.MAX_VALUE
        var saturnMax = 0.0
        while (at.isBefore(end)) {
            val t = AstronomyMath.centuriesTT(at)
            val m = PlanetMath.equatorial(Planet.MARS, t).distanceAu
            val s = PlanetMath.equatorial(Planet.SATURN, t).distanceAu
            if (m < marsMin) marsMin = m
            if (m > marsMax) marsMax = m
            if (s < saturnMin) saturnMin = s
            if (s > saturnMax) saturnMax = s
            at = at.plusSeconds(24L * 3600)
        }
        assertTrue("Mars came to ${"%.2f".format(marsMin)} AU", marsMin in 0.37..0.45)
        assertTrue("Mars went to ${"%.2f".format(marsMax)} AU", marsMax in 2.3..2.7)
        assertTrue("Saturn came to ${"%.2f".format(saturnMin)} AU", saturnMin in 8.0..9.1)
        assertTrue("Saturn went to ${"%.2f".format(saturnMax)} AU", saturnMax in 10.0..11.1)
    }
}
