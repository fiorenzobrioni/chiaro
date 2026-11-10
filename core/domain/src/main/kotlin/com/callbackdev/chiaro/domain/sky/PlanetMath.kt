package com.callbackdev.chiaro.domain.sky

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The four planets this app knows, and nothing else — see [PlanetMath]. In the order the
 * eye finds them: the two nobody has to be taught, then the two a reader looks for.
 */
enum class Planet { VENUS, JUPITER, MARS, SATURN }

/**
 * Where Venus, Jupiter, Mars and Saturn are, and **only** those four.
 *
 * `PLANNING.md` recorded, at Fase 19, that planets were "a project of their own — a
 * truncated VSOP87 and its tests". That is true of *the planets* as a category and it
 * was the right call then. It was not true of Venus and Jupiter, and the committente
 * reopened it on that argument (Fase 28): they are the only points of light a passer-by
 * picks out of the sky without being taught, they are the answer to "what is that
 * bright star", and getting them to a few arcminutes needs orbital elements rather
 * than a series per planet.
 *
 * **Mars and Saturn came in on 10 Nov 2026** (PLANNING, Fase 16, step 16.3), for the sky a
 * reader explores: a sky that can be turned and asked "what is that" has to answer for
 * the steady light that is not a star, and on most nights that light is one of these
 * four. What kept them out was true of a list of moments and is not true of a sky: Mars
 * asks about its own colour, and now has one; Saturn's rings are a telescope's
 * business, and the point of light is not. Mercury stays out: it is up for a few days a
 * year, low in a bright twilight, and a name that says "not visible" on almost every
 * night is noise. Uranus and Neptune are not naked-eye sights at all.
 *
 * **The model.** Keplerian elements with secular rates — the JPL/Caltech table of
 * approximate elements for the major planets, valid 1800–2050, solved through Kepler's
 * equation and referred to the mean ecliptic and equinox of J2000. Positions are then
 * precessed to the ecliptic of date, light-time corrected once, and handed to the same
 * obliquity, altitude and azimuth primitives every other body in this module uses.
 *
 * **What it is good for, and what it is not.** About an arcminute for Venus and Mars,
 * two and a half for Jupiter and seven for Saturn (the worst of eleven instants from
 * 2026 to 2039 against JPL Horizons, `PlanetEphemerisTest`; the two giants pull on each
 * other in a way mean elements do not carry), which is the right size for the questions
 * this app asks: whether a planet is up and high enough to be
 * the bright thing somebody is pointing at, where on a drawn sky its dot stands, the
 * minute it rises, and how far it is from the moon when they pass — a separation this
 * app reports in whole degrees and never in arcminutes.
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
 * 3. Since Fase 16, **all four against an ephemeris** (`PlanetEphemerisTest`): eleven
 *    instants from 2026 to 2039 read from JPL Horizons, place and brightness, with the
 *    day they were read. The rule of that phase, and what Mars and Saturn came in on.
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

    private val Mars = Elements(
        1.52371034, 0.00001847,
        0.09339410, 0.00007882,
        1.84969142, -0.00813131,
        -4.55343205, 19140.30268499,
        -23.94362959, 0.44441088,
        49.55953891, -0.29257343
    )

    private val Saturn = Elements(
        9.53667594, -0.00125060,
        0.05386179, -0.00050991,
        2.48599187, 0.00193609,
        49.95424423, 1222.49362201,
        92.59887831, -0.41897216,
        113.66242448, -0.28867794
    )

    private fun elementsOf(planet: Planet) = when (planet) {
        Planet.VENUS -> Venus
        Planet.JUPITER -> Jupiter
        Planet.MARS -> Mars
        Planet.SATURN -> Saturn
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
     * The light-time step is one iteration and that is enough by a wide margin: the
     * first pass is off by the minute or two the planet's distance changed over the light's
     * own journey (ninety minutes at most, Saturn's), and in a minute or two the fastest
     * of the four moves a few hundredths of an arcminute, so a second pass would move the
     * answer by less than the model's own noise.
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
     * Every orbit here is nearly circular — Venus is the roundest in the solar system
     * at e = 0.0068, Mars the least round of the four at 0.093 — so this converges in
     * three or four passes. The cap is there because a solver with no cap is a hang
     * waiting for an input nobody tested.
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
     * How bright [planet] is at [t] centuries TT, as a visual magnitude: **for how a dot
     * is drawn, never for a screen** (no jargon: the app says bright or faint in words).
     *
     * The Astronomical Almanac's formulas as Meeus prints them (Astronomical Algorithms,
     * ch. 41): a constant per planet, the two distances, and the phase angle, the angle
     * at the planet between the sun and the earth. Saturn has two terms more, for the
     * rings: open, they more than double its light, and edge-on (2025, 2039) they give
     * nothing. Within a quarter of a magnitude of JPL Horizons on every row of
     * `PlanetEphemerisTest` (Venus the worst at 0.24, Saturn 0.15, Mars 0.12, Jupiter
     * 0.03); the eye does not tell a quarter of a magnitude, and the drawing bends it
     * further.
     *
     * Why it exists: Venus and Jupiter outshine every star on every night, so they were
     * drawn at the top of the scale and no number was needed. Mars runs from brighter
     * than Jupiter to an ordinary second-magnitude star, and Saturn from Arcturus to
     * Spica: drawn like Jupiter they would be a lie on most nights of most years.
     */
    fun magnitude(planet: Planet, t: Double): Double {
        val earth = heliocentric(Earth, t)
        val body = heliocentric(elementsOf(planet), t)
        val geocentric = body - earth
        val r = body.length
        val delta = geocentric.length
        val sun = earth.length
        val phase = acos(((r * r + delta * delta - sun * sun) / (2 * r * delta)).coerceIn(-1.0, 1.0)) / DEG
        val distances = 5.0 * log10(r * delta)
        return when (planet) {
            Planet.VENUS -> -4.40 + distances + 0.0009 * phase + 0.000239 * phase * phase -
                0.00000065 * phase * phase * phase
            Planet.MARS -> -1.52 + distances + 0.016 * phase
            Planet.JUPITER -> -9.40 + distances + 0.005 * phase
            Planet.SATURN -> {
                val sinTilt = abs(sin(ringTilt(geocentric, t) * DEG))
                -8.88 + distances + 0.044 * phase - 2.60 * sinTilt + 1.25 * sinTilt * sinTilt
            }
        }
    }

    /**
     * How far Saturn's rings are opened toward the earth, degrees: the earth's latitude
     * over the ring plane, positive when their north face shows (Meeus, ch. 45). The
     * plane's tilt and node are given on the ecliptic of date, so the planet's
     * longitude is precessed as [equatorial] precesses it.
     */
    private fun ringTilt(geocentric: Vector, t: Double): Double {
        val inclination = (28.075216 - 0.012998 * t) * DEG
        val node = 169.508470 + 1.394681 * t
        val longitude = atan2(geocentric.y, geocentric.x) / DEG + PRECESSION_PER_CENTURY * t
        val latitude = asin((geocentric.z / geocentric.length).coerceIn(-1.0, 1.0))
        val sinTilt = sin(inclination) * cos(latitude) * sin((longitude - node) * DEG) -
            cos(inclination) * sin(latitude)
        return asin(sinTilt.coerceIn(-1.0, 1.0)) / DEG
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
