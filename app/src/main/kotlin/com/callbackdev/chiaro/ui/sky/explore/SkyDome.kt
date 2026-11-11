package com.callbackdev.chiaro.ui.sky.explore

import com.callbackdev.chiaro.domain.model.Coordinates
import com.callbackdev.chiaro.domain.sky.AstronomyEngine
import com.callbackdev.chiaro.domain.sky.BrightStars
import com.callbackdev.chiaro.domain.sky.Constellation
import com.callbackdev.chiaro.domain.sky.Constellations
import com.callbackdev.chiaro.domain.sky.Planet
import com.callbackdev.chiaro.domain.sky.SkyTarget
import com.callbackdev.chiaro.ui.components.StarField
import com.callbackdev.chiaro.ui.components.milkyWayShown
import com.callbackdev.chiaro.ui.components.moonLightOf
import com.callbackdev.chiaro.ui.components.planetsShown
import com.callbackdev.chiaro.ui.components.starsShown
import com.callbackdev.chiaro.widget.window.NamedStar
import com.callbackdev.chiaro.widget.window.NamedStars
import com.callbackdev.chiaro.widget.window.Radiant
import com.callbackdev.chiaro.widget.window.WindowScene
import java.time.Instant
import java.time.ZoneId
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin

/** A place on the dome: its direction east, north and up, and the two angles it was made from. */
internal class DomePoint(val altitudeDeg: Double, val azimuthDeg: Double) {
    val east: Double
    val north: Double
    val up: Double

    init {
        val alt = Math.toRadians(altitudeDeg)
        val az = Math.toRadians(azimuthDeg)
        east = cos(alt) * sin(az)
        north = cos(alt) * cos(az)
        up = sin(alt)
    }
}

internal class DomePlanet(val planet: Planet, val place: DomePoint, val magnitude: Double)

internal class DomeStar(val star: NamedStar, val place: DomePoint)

/**
 * The whole sky of one instant from one place (PLANNING, Fase 16, step 16.6): every star of
 * the catalogue, the four planets, the moon, the sun, the Milky Way's band and a peak
 * night's radiant, each as a direction east, north and up. Worked out once for the instant;
 * the camera then turns over it, and turning costs a dot product a star ([SkyLens]).
 *
 * **Every place is the engine's.** The stars are turned from their catalogue places by one
 * rotation, and the rotation is read off [AstronomyEngine.starHorizontal] itself, at the
 * three axes of the catalogue's frame: the engine's altitude and bearing of a fixed point
 * are a turn of the sphere and nothing else (no refraction, no precession), so three
 * answers give the whole turn, and `SkyDomeTest` asks the engine for every star again and
 * finds each where the rotation put it. There is no second copy of the sidereal time here.
 *
 * **What shows is the hero's rule** (`starsShown`, `planetsShown`, `milkyWayShown`: the
 * dark, the moon and the hour's cloud). The dome is the sky itself and knows no weather:
 * [starsShown] and its two fellows are the clear sky's, and [starsUnder] and its fellows
 * the same rule under a cover, which the page reads off the forecast for the dome's
 * instant (step 16.7) or leaves at nothing where the forecast does not reach.
 */
internal class SkyDome private constructor(
    val at: Instant,
    val coords: Coordinates,
    private val rotation: DoubleArray,
    val sun: DomePoint,
    val moon: DomePoint,
    /** The moon's lit share, 0 new to 1 full. */
    val moonIllumination: Double,
    val planets: List<DomePlanet>,
    /** The stars with a name that are over the horizon, brightest first. */
    val named: List<DomeStar>,
    /** The shower whose peak night this is, where its meteors come from; null on any other. */
    val radiant: Radiant?
) {
    /** The catalogue's stars, in its own order (brightest first): east, north and up, three a star. */
    val starDirections: FloatArray = FloatArray(BrightStars.count * 3).also { out ->
        val unit = StarUnits.equatorial
        for (i in 0 until BrightStars.count) {
            turn(unit[3 * i].toDouble(), unit[3 * i + 1].toDouble(), unit[3 * i + 2].toDouble(), out, 3 * i)
        }
    }

    /** The Milky Way's band as a grid of directions, [MilkyWay.LONGITUDES] by [MilkyWay.LATITUDES], three a vertex. */
    val milkyWayDirections: FloatArray = FloatArray(MilkyWay.vertexCount * 3).also { out ->
        val unit = MilkyWay.equatorial
        for (i in 0 until MilkyWay.vertexCount) {
            turn(unit[3 * i].toDouble(), unit[3 * i + 1].toDouble(), unit[3 * i + 2].toDouble(), out, 3 * i)
        }
    }

    private val moonLight: Float = moonLightOf(moonIllumination, moon.altitudeDeg)

    /** How much of the stars shows, 0..1: the dark, and the moon. */
    val starsShown: Float = starsShown(sun.altitudeDeg, moonLight)

    /** The planets come out before the stars, in the dusk. */
    val planetsShown: Float = planetsShown(sun.altitudeDeg, moonLight)

    val milkyWayShown: Float = milkyWayShown(sun.altitudeDeg, moonLight)

    /** [starsShown] under a sky [cover] of which is cloud, 0..1: the hero's own arithmetic. */
    fun starsUnder(cover: Float): Float = starsShown(sun.altitudeDeg, moonLight, cover)

    fun planetsUnder(cover: Float): Float = planetsShown(sun.altitudeDeg, moonLight, cover)

    fun milkyWayUnder(cover: Float): Float = milkyWayShown(sun.altitudeDeg, moonLight, cover)

    /**
     * The constellations' lines (step 16.7), every segment of the eighty-eight in
     * [ConstellationUnits]' order: east, north and up of one end, then of the other, six
     * floats a segment. Turned when first asked for: a reader with the lines off never
     * pays for them.
     */
    val constellationLines: FloatArray by lazy(LazyThreadSafetyMode.NONE) { turned(ConstellationUnits.lines) }

    /** Where each constellation's name is written, three floats a name, in [ConstellationUnits.names]' order. */
    val constellationNamePlaces: FloatArray by lazy(LazyThreadSafetyMode.NONE) { turned(ConstellationUnits.namePlaces) }

    private fun turned(units: FloatArray): FloatArray = FloatArray(units.size).also { out ->
        for (i in 0 until units.size / 3) {
            turn(units[3 * i].toDouble(), units[3 * i + 1].toDouble(), units[3 * i + 2].toDouble(), out, 3 * i)
        }
    }

    /** Whether the sun is over the horizon: then it is drawn, and little else shows. */
    val sunUp: Boolean get() = sun.altitudeDeg > AstronomyEngine.SUNRISE_ALTITUDE

    val moonUp: Boolean get() = moon.altitudeDeg > 0.0

    private fun turn(x: Double, y: Double, z: Double, out: FloatArray, at: Int) {
        val r = rotation
        out[at] = (r[0] * x + r[1] * y + r[2] * z).toFloat()
        out[at + 1] = (r[3] * x + r[4] * y + r[5] * z).toFloat()
        out[at + 2] = (r[6] * x + r[7] * y + r[8] * z).toFloat()
    }

    companion object {
        /**
         * The dome at [at] from [coords]. [zone] and [followed] are for the radiant: a
         * strong shower is named on its peak night, a weak one only for a reader who
         * follows it, as on the hero ([WindowScene.peakRadiant]).
         */
        fun of(at: Instant, coords: Coordinates, zone: ZoneId, followed: Set<String>): SkyDome {
            // The turn from the catalogue's frame to east, north and up: where the engine
            // puts the frame's three axes, a column each.
            val x = AstronomyEngine.starHorizontal(0.0, 0.0, at, coords).point()
            val y = AstronomyEngine.starHorizontal(90.0, 0.0, at, coords).point()
            val z = AstronomyEngine.starHorizontal(0.0, 90.0, at, coords).point()
            val rotation = doubleArrayOf(
                x.east, y.east, z.east,
                x.north, y.north, z.north,
                x.up, y.up, z.up
            )
            val sun = DomePoint(AstronomyEngine.sunAltitude(at, coords), AstronomyEngine.sunAzimuth(at, coords))
            val moon = AstronomyEngine.horizontal(SkyTarget.Moon, at, coords).point()
            val night = sun.altitudeDeg < AstronomyEngine.CIVIL_TWILIGHT
            return SkyDome(
                at = at,
                coords = coords,
                rotation = rotation,
                sun = sun,
                moon = moon,
                moonIllumination = AstronomyEngine.moonIllumination(at).illuminatedFraction,
                planets = Planet.values().map { planet ->
                    DomePlanet(
                        planet = planet,
                        place = AstronomyEngine.horizontal(SkyTarget.Wanderer(planet), at, coords).point(),
                        magnitude = AstronomyEngine.planetMagnitude(planet, at)
                    )
                },
                named = NamedStars
                    .map { DomeStar(it, AstronomyEngine.starHorizontal(it.raDeg, it.decDeg, at, coords).point()) }
                    .filter { it.place.altitudeDeg > 0.0 },
                radiant = if (night) {
                    WindowScene.peakRadiant(at, zone, coords, followed)?.takeIf { it.altitudeDeg > 0.0 }
                } else {
                    null
                }
            )
        }

        private fun AstronomyEngine.Horizontal.point() = DomePoint(altitudeDeg, azimuthDeg)
    }
}

/** The catalogue's stars as unit vectors in its own frame, worked out once: they do not move. */
internal object StarUnits {
    val equatorial: FloatArray by lazy {
        FloatArray(BrightStars.count * 3).also { out ->
            for (i in 0 until BrightStars.count) {
                unit(BrightStars.raDeg[i].toDouble(), BrightStars.decDeg[i].toDouble(), out, 3 * i)
            }
        }
    }

    /** The unit vector of a right ascension and a declination: x toward 0h, y toward 6h, z north. */
    fun unit(raDeg: Double, decDeg: Double, out: FloatArray, at: Int) {
        val ra = Math.toRadians(raDeg)
        val dec = Math.toRadians(decDeg)
        out[at] = (cos(dec) * cos(ra)).toFloat()
        out[at + 1] = (cos(dec) * sin(ra)).toFloat()
        out[at + 2] = sin(dec).toFloat()
    }
}

/** A constellation's name on the sky: whose it is, and the source's rank, 1 for the ones a
 * reader looks for first (`Constellation.rank`). The Serpent has two, one on each piece. */
internal class ConstellationName(val id: String, val rank: Int)

/**
 * The eighty-eight constellations as unit vectors in the catalogue's frame (step 16.7),
 * worked out once from [Constellations]: like the stars, they do not move, and a dome
 * turns them with the one rotation it turns everything by. Every end of a line is a
 * catalogue star's place to the last bit (16.5), so a line drawn from here ends on a dot.
 */
internal object ConstellationUnits {
    /** Every segment, two ends, three floats an end. */
    val lines: FloatArray by lazy {
        val all = Constellations.all
        val out = FloatArray(all.sumOf { it.segmentCount } * 6)
        var at = 0
        for (c in all) {
            for (segment in 0 until c.segmentCount) {
                val from = segment * Constellation.FLOATS_A_SEGMENT
                StarUnits.unit(c.lines[from].toDouble(), c.lines[from + 1].toDouble(), out, at)
                StarUnits.unit(c.lines[from + 2].toDouble(), c.lines[from + 3].toDouble(), out, at + 3)
                at += 6
            }
        }
        out
    }

    /** One a name point, in the catalogue's order. */
    val names: List<ConstellationName> by lazy {
        Constellations.all.flatMap { c -> c.labels.map { ConstellationName(c.id, c.rank) } }
    }

    /** Where each of [names] is written, three floats a name. */
    val namePlaces: FloatArray by lazy {
        val points = Constellations.all.flatMap { it.labels }
        FloatArray(points.size * 3).also { out ->
            points.forEachIndexed { i, point -> StarUnits.unit(point.raDeg, point.decDeg, out, 3 * i) }
        }
    }
}

/**
 * The Milky Way as the explorer draws it: the model the hero's shader draws (20 ott 2026,
 * `SkyShaderSource`), as a grid along the galactic plane instead of a pixel's arithmetic,
 * because this canvas is not a shader. A band along the plane, a Gaussian across it, wider
 * and brighter toward the centre in Sagittarius and narrow and faint toward the anticentre
 * in Auriga, a little uneven. **A drawing of a real thing at its real place, with a model's
 * profile**: the rift and the clouds of the real band are not in it.
 *
 * The grid is [LONGITUDES] steps round the plane by [LATITUDES] across it, the rows spread
 * over three widths of the band each side, which is where the Gaussian has gone to nothing.
 */
internal object MilkyWay {
    const val LONGITUDES = 120
    const val LATITUDES = 9

    /** A column more than the steps: the last is the first again, and the band closes. */
    const val COLUMNS = LONGITUDES + 1
    const val vertexCount = COLUMNS * LATITUDES

    /** How wide the grid is, in the band's own widths, each side of the plane. */
    private const val REACH = 3.0

    /** The grid's directions in the catalogue's frame, three a vertex, column by column. */
    val equatorial: FloatArray get() = built.first

    /** How bright each vertex is, 0..1 of the band's brightest, before the night's own amount. */
    val strength: FloatArray get() = built.second

    private val built: Pair<FloatArray, FloatArray> by lazy {
        val pole = FloatArray(3).also {
            StarUnits.unit(StarField.GALACTIC_POLE_RA_DEG, StarField.GALACTIC_POLE_DEC_DEG, it, 0)
        }
        val centre = FloatArray(3).also {
            StarUnits.unit(StarField.GALACTIC_CENTRE_RA_DEG, StarField.GALACTIC_CENTRE_DEC_DEG, it, 0)
        }
        // The third axis: a quarter turn along the plane from the centre.
        val side = floatArrayOf(
            pole[1] * centre[2] - pole[2] * centre[1],
            pole[2] * centre[0] - pole[0] * centre[2],
            pole[0] * centre[1] - pole[1] * centre[0]
        )
        val directions = FloatArray(vertexCount * 3)
        val strengths = FloatArray(vertexCount)
        for (column in 0 until COLUMNS) {
            val l = 2.0 * Math.PI * (column % LONGITUDES) / LONGITUDES
            val toward = 0.5 + 0.5 * cos(l)
            val sigma = 0.10 + 0.07 * toward
            val along = 0.3 + 0.7 * toward.pow(1.5)
            for (row in 0 until LATITUDES) {
                val b = (row - (LATITUDES - 1) / 2.0) / ((LATITUDES - 1) / 2.0) * REACH * sigma
                val i = column * LATITUDES + row
                val cb = cos(b)
                for (k in 0 until 3) {
                    directions[3 * i + k] = (cb * cos(l) * centre[k] + cb * sin(l) * side[k] + sin(b) * pole[k]).toFloat()
                }
                val band = exp(-b * b / (2.0 * sigma * sigma))
                // Uneven, and the same unevenness every night: it is fixed to the sky.
                val mottle = 0.8 + 0.4 * (0.5 + 0.25 * sin(9.0 * l + 4.0 * sin(23.0 * b)) + 0.25 * sin(17.0 * l + 31.0 * b - 2.0))
                // The outermost rows are the band's edge and carry nothing, so it ends in the sky.
                val edge = if (row == 0 || row == LATITUDES - 1) 0.0 else 1.0
                strengths[i] = (band * along * mottle * edge / 1.2).toFloat()
            }
        }
        directions to strengths
    }
}
