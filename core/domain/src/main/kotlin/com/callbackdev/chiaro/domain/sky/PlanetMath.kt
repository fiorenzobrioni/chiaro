package com.callbackdev.chiaro.domain.sky

import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** The two planets this app knows, and nothing else — see [PlanetMath]. */
enum class Planet { VENUS, JUPITER }

/**
 * Where Venus and Jupiter are (Fase 28), and **only** those two.
 *
 * `PLANNING.md` recorded, at Fase 19, that planets were "a project of their own — a
 * truncated VSOP87 and its tests". That is true of *the planets* as a category and it
 * was the right call then. It is not true of these two, and the committente reopened
 * it on that argument: Venus and Jupiter are the only points of light a passer-by
 * picks out of the sky without being taught, they are the answer to "what is that
 * bright star", and getting them to a few arcminutes needs orbital elements rather
 * than a series per planet. The other six stay out, for the reason that has not
 * changed: Mars asks about its own colour, Mercury is never dark-sky, and Saturn,
 * Uranus and Neptune are a telescope's business, which §3.2 puts outside this app.
 *
 * **The model.** Keplerian elements with secular rates — the JPL/Caltech table of
 * approximate elements for the major planets, valid 1800–2050, solved through Kepler's
 * equation and referred to the mean ecliptic and equinox of J2000. Positions are then
 * precessed to the ecliptic of date, light-time corrected once, and handed to the same
 * obliquity, altitude and azimuth primitives every other body in this module uses.
 *
 * **What it is good for, and what it is not.** A few arcminutes, which is the right
 * size for the two questions this app asks: whether a planet is up and high enough to
 * be the bright thing somebody is pointing at, and how far it is from the moon when
 * they pass — a separation this app reports in whole degrees and never in arcminutes.
 * It is *not* an ephemeris: it does not do occultations, it does not do transits, and
 * nothing here should ever be rendered to the second.
 *
 * **How it is verified** (`PlanetMathTest`), and the chain matters because a table of
 * coefficients nobody checked is a rumour (the rule `EclipseEngine` was held to):
 *
 * 1. The **earth's** elements are checked against this module's own solar longitude,
 *    which is itself measured against Open-Meteo's sunrise to thirty seconds. The
 *    sun seen from the earth is the earth's heliocentric longitude plus half a turn,
 *    so agreeing with it validates the element set, Kepler's solution, the precession
 *    step and the ecliptic-to-equatorial hand-off in one assertion.
 * 2. The **planets'** own elements are then pinned by the invariants a wrong table
 *    cannot satisfy: Venus's greatest elongation from the sun (it never leaves the
 *    twilight, which is why it is only ever a morning or an evening star), both
 *    synodic periods, both sidereal periods, and the range each one's distance from
 *    the sun is allowed to take.
 */
internal object PlanetMath {

    private const val DEG = Math.PI / 180.0

    /** Light travels one AU in this many days: the light-time correction's constant. */
    private const val DAYS_PER_AU = 0.005_775_518_3

    /**
     * General precession in ecliptic longitude, degrees per Julian century.
     *
     * The elements below are referred to the ecliptic and equinox of **J2000** while
     * every other angle in this module is of date. A quarter of a century is 0.35° of
     * the difference, which is larger than the model's own error and larger than the
     * separations a conjunction row prints, so it is corrected rather than ignored.
     */
    private const val PRECESSION_PER_CENTURY = 1.396_971

    /** One planet's elements at an epoch, and the rate each one drifts per century. */
    private data class Elements(
        val semiMajorAu: Double, val semiMajorRate: Double,
        val eccentricity: Double, val eccentricityRate: Double,
        val inclinationDeg: Double, val inclinationRate: Double,
        val meanLongitudeDeg: Double, val meanLongitudeRate: Double,
        val perihelionLongitudeDeg: Double, val perihelionLongitudeRate: Double,
        val nodeLongitudeDeg: Double, val nodeLongitudeRate: Double
    )

    /**
     * The earth–moon barycentre, which is what every solution of this kind means by
     * "the earth" — the same body [AstronomyMath.earthRadiusVectorAu] describes, and
     * for the same reason: it is what an almanac means too.
     */
    private val Earth = Elements(
        1.00000261, 0.00000562,
        0.01671123, -0.00004392,
        -0.00001531, -0.01294668,
        100.46457166, 35999.37244981,
        102.93768193, 0.32327364,
        0.0, 0.0
    )

    private val Venus = Elements(
        0.72333566, 0.00000390,
        0.00677672, -0.00004107,
        3.39467605, -0.00078890,
        181.97909950, 58517.81538729,
        131.60246718, 0.00268329,
        76.67984255, -0.27769418
    )

    private val Jupiter = Elements(
        5.20288700, -0.00011607,
        0.04838624, -0.00013253,
        1.30439695, -0.00183714,
        34.39644051, 3034.74612775,
        14.72847983, 0.21252668,
        100.47390909, 0.20469106
    )

    private fun elementsOf(planet: Planet) = when (planet) {
        Planet.VENUS -> Venus
        Planet.JUPITER -> Jupiter
    }

    /** A heliocentric position in the J2000 ecliptic frame, in AU. */
    private data class Vector(val x: Double, val y: Double, val z: Double) {
        val length: Double get() = sqrt(x * x + y * y + z * z)
        operator fun minus(other: Vector) = Vector(x - other.x, y - other.y, z - other.z)
    }

    /**
     * Apparent equatorial coordinates of [planet] at [t] centuries TT, and how far away
     * it is in AU.
     *
     * The light-time step is one iteration and that is enough by a wide margin: Jupiter
     * is at most 50 light-minutes away and moves about 0.2 arcminutes in that time, so a
     * second pass would move the answer by less than the model's own noise.
     */
    fun equatorial(planet: Planet, t: Double): PlanetPosition {
        val earth = heliocentric(Earth, t)
        var geocentric = heliocentric(elementsOf(planet), t) - earth
        val lightTimeCenturies = geocentric.length * DAYS_PER_AU / 36_525.0
        geocentric = heliocentric(elementsOf(planet), t - lightTimeCenturies) - earth

        val distance = geocentric.length
        val longitude = AstronomyMath.norm360(
            atan2(geocentric.y, geocentric.x) / DEG + PRECESSION_PER_CENTURY * t
        )
        val latitude = asin((geocentric.z / distance).coerceIn(-1.0, 1.0)) / DEG
        return PlanetPosition(
            equatorial = eclipticToEquatorial(longitude, latitude, AstronomyMath.obliquity(t)),
            distanceAu = distance,
            /** The planet's own distance from the sun: what pins the element table. */
            sunDistanceAu = heliocentric(elementsOf(planet), t).length
        )
    }

    /** Heliocentric rectangular coordinates, J2000 ecliptic frame, AU. */
    private fun heliocentric(elements: Elements, t: Double): Vector {
        val a = elements.semiMajorAu + elements.semiMajorRate * t
        val e = elements.eccentricity + elements.eccentricityRate * t
        val inclination = elements.inclinationDeg + elements.inclinationRate * t
        val meanLongitude = elements.meanLongitudeDeg + elements.meanLongitudeRate * t
        val perihelion = elements.perihelionLongitudeDeg + elements.perihelionLongitudeRate * t
        val node = elements.nodeLongitudeDeg + elements.nodeLongitudeRate * t

        val argument = perihelion - node
        val meanAnomaly = AstronomyMath.norm180(meanLongitude - perihelion)
        val eccentric = solveKepler(meanAnomaly, e)

        // In the orbital plane, perifocal: the x axis points at perihelion.
        val xOrbit = a * (cos(eccentric * DEG) - e)
        val yOrbit = a * sqrt(1.0 - e * e) * sin(eccentric * DEG)

        val cosArg = cos(argument * DEG)
        val sinArg = sin(argument * DEG)
        val cosNode = cos(node * DEG)
        val sinNode = sin(node * DEG)
        val cosInc = cos(inclination * DEG)
        val sinInc = sin(inclination * DEG)

        return Vector(
            x = (cosArg * cosNode - sinArg * sinNode * cosInc) * xOrbit +
                (-sinArg * cosNode - cosArg * sinNode * cosInc) * yOrbit,
            y = (cosArg * sinNode + sinArg * cosNode * cosInc) * xOrbit +
                (-sinArg * sinNode + cosArg * cosNode * cosInc) * yOrbit,
            z = (sinArg * sinInc) * xOrbit + (cosArg * sinInc) * yOrbit
        )
    }

    /**
     * Kepler's equation `M = E − e*·sin E`, by Newton, in DEGREES (`e*` is the
     * eccentricity in degrees per radian, which is what keeps the units honest).
     *
     * Both orbits here are nearly circular — Venus is the roundest in the solar system
     * at e = 0.0068, Jupiter is 0.048 — so this converges in three or four passes. The
     * cap is there because a solver with no cap is a hang waiting for an input nobody
     * tested.
     */
    private fun solveKepler(meanAnomalyDeg: Double, eccentricity: Double): Double {
        val eStar = eccentricity / DEG
        var eccentric = meanAnomalyDeg + eStar * sin(meanAnomalyDeg * DEG)
        repeat(MAX_KEPLER_STEPS) {
            val delta = (meanAnomalyDeg - (eccentric - eStar * sin(eccentric * DEG))) /
                (1.0 - eccentricity * cos(eccentric * DEG))
            eccentric += delta
            if (abs(delta) < KEPLER_TOLERANCE_DEG) return eccentric
        }
        return eccentric
    }

    private fun eclipticToEquatorial(
        longitude: Double,
        latitude: Double,
        eps: Double
    ): AstronomyMath.Equatorial {
        val l = longitude * DEG
        val b = latitude * DEG
        val e = eps * DEG
        val ra = atan2(sin(l) * cos(e) - Math.tan(b) * sin(e), cos(l))
        val dec = asin((sin(b) * cos(e) + cos(b) * sin(e) * sin(l)).coerceIn(-1.0, 1.0))
        return AstronomyMath.Equatorial(AstronomyMath.norm360(ra / DEG), dec / DEG)
    }

    /**
     * The sun as the earth's own elements see it: the earth's heliocentric longitude
     * plus half a turn. Exists for the test that validates the element set against
     * this module's measured solar longitude, and for nothing else.
     */
    fun sunLongitudeFromEarthElements(t: Double): Double {
        val earth = heliocentric(Earth, t)
        return AstronomyMath.norm360(
            atan2(earth.y, earth.x) / DEG + 180.0 + PRECESSION_PER_CENTURY * t
        )
    }

    private const val KEPLER_TOLERANCE_DEG = 1e-8
    private const val MAX_KEPLER_STEPS = 12
}

/** Where a planet is, how far away, and how far from its own sun. */
internal data class PlanetPosition(
    val equatorial: AstronomyMath.Equatorial,
    val distanceAu: Double,
    val sunDistanceAu: Double
)
