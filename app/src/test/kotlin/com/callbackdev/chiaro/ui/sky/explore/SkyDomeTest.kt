package com.callbackdev.chiaro.ui.sky.explore

import com.callbackdev.chiaro.domain.model.Coordinates
import com.callbackdev.chiaro.domain.sky.AstronomyEngine
import com.callbackdev.chiaro.domain.sky.BrightStars
import com.callbackdev.chiaro.domain.sky.Constellation
import com.callbackdev.chiaro.domain.sky.Constellations
import com.callbackdev.chiaro.domain.sky.Planet
import com.callbackdev.chiaro.domain.sky.SkyBody
import com.callbackdev.chiaro.widget.window.NamedStars
import java.time.Instant
import java.time.ZoneId
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The explorer's dome (Fase 16, step 16.6): the whole sky of an instant as directions, and
 * every one of them where the engine puts it.
 */
class SkyDomeTest {

    private val milan = Coordinates(45.4642, 9.19)
    private val sydney = Coordinates(-33.8688, 151.2093)
    private val rome = ZoneId.of("Europe/Rome")

    // A moonless November night, the middle of the recorded Milan's full-moon night, and a noon.
    private val dark = Instant.parse("2026-11-11T22:00:00Z")
    private val moonlit = Instant.parse("2026-09-25T22:00:00Z")
    private val noon = Instant.parse("2026-11-11T11:00:00Z")

    private fun dome(at: Instant, coords: Coordinates = milan) = SkyDome.of(at, coords, rome, emptySet())

    /**
     * The angle between a direction of the dome and a height and a bearing, degrees: by
     * the chord between the two, because the arc cosine of a number next to one turns a
     * float's last digit into a fiftieth of a degree.
     */
    private fun off(east: Float, north: Float, up: Float, altitudeDeg: Double, azimuthDeg: Double): Double {
        val alt = Math.toRadians(altitudeDeg)
        val az = Math.toRadians(azimuthDeg)
        val chord = sqrt(
            (east - cos(alt) * sin(az)).pow(2) + (north - cos(alt) * cos(az)).pow(2) + (up - sin(alt)).pow(2)
        )
        return Math.toDegrees(2.0 * asin((chord / 2.0).coerceIn(0.0, 1.0)))
    }

    /**
     * The stars are turned by one rotation read off the engine at three points. Asked for
     * every star again, in both hemispheres and half a year apart, the engine puts each
     * where the rotation did: the two are the same turn, and there is one sidereal time.
     */
    @Test
    fun `every star of the catalogue is where the engine puts it`() {
        val cases = listOf(dark to milan, moonlit to milan, dark to sydney, Instant.parse("2027-05-03T14:20:00Z") to sydney)
        for ((at, coords) in cases) {
            val directions = dome(at, coords).starDirections
            assertEquals(BrightStars.count * 3, directions.size)
            var worst = 0.0
            for (i in 0 until BrightStars.count) {
                val place = AstronomyEngine.starHorizontal(BrightStars.raDeg[i].toDouble(), BrightStars.decDeg[i].toDouble(), at, coords)
                worst = maxOf(worst, off(directions[3 * i], directions[3 * i + 1], directions[3 * i + 2], place.altitudeDeg, place.azimuthDeg))
            }
            // A float's worth. A dp at the closest zoom is a twentieth of a degree.
            assertTrue("the worst star at $at from $coords is $worst° off", worst < 0.0001)
        }
    }

    @Test
    fun `the moon, the planets and the sun are the engine's`() {
        val dome = dome(dark)
        assertEquals(AstronomyEngine.moonAltitude(dark, milan), dome.moon.altitudeDeg, 1e-9)
        assertEquals(AstronomyEngine.bodyAzimuth(SkyBody.MOON, dark, milan), dome.moon.azimuthDeg, 1e-9)
        assertEquals(AstronomyEngine.sunAltitude(dark, milan), dome.sun.altitudeDeg, 1e-9)
        assertEquals(Planet.values().toList(), dome.planets.map { it.planet })
        for (p in dome.planets) {
            assertEquals(AstronomyEngine.planetAltitude(p.planet, dark, milan), p.place.altitudeDeg, 1e-9)
            assertEquals(AstronomyEngine.planetAzimuth(p.planet, dark, milan), p.place.azimuthDeg, 1e-9)
            assertEquals(AstronomyEngine.planetMagnitude(p.planet, dark), p.magnitude, 1e-9)
        }
        assertEquals(AstronomyEngine.moonIllumination(dark).illuminatedFraction, dome.moonIllumination, 1e-12)
    }

    @Test
    fun `only the stars over the horizon carry a name, the brightest first`() {
        val north = dome(dark)
        assertTrue(north.named.isNotEmpty())
        assertTrue(north.named.all { it.place.altitudeDeg > 0.0 })
        assertEquals(north.named.map { it.star.mag }.sorted(), north.named.map { it.star.mag })
        // From Milan the Pole Star is always up and the Southern Cross never.
        assertTrue(north.named.any { it.star.decDeg > 89.0 })
        assertFalse(north.named.any { it.star.decDeg < -55.0 })
        // From Sydney the other way round.
        val south = dome(dark, sydney)
        assertFalse(south.named.any { it.star.decDeg > 89.0 })
    }

    /**
     * What a frame costs, counted (Fase 16.6, the battery's rule): the stars a camera
     * places and draws on a phone held upright, 384 by 832 dp. The whole catalogue is 5 080
     * and half of it is under the ground; the zoom's rule keeps a wider field from
     * drawing more of them than the resting one does.
     */
    @Test
    fun `a frame draws some hundreds of stars, and a wider field no more of them`() {
        val dome = dome(dark)
        fun drawn(camera: SkyCamera): Int {
            val lens = camera.lens(768f, 1664f)
            val out = FloatArray(2)
            val faintest = SkyCamera.faintestMagnitude(camera.fieldDeg)
            val d = dome.starDirections
            var n = 0
            for (i in 0 until BrightStars.count) {
                if (BrightStars.mag[i] > faintest) break
                if (d[3 * i + 2] < 0f) continue
                if (lens.toScreen(d[3 * i].toDouble(), d[3 * i + 1].toDouble(), d[3 * i + 2].toDouble(), out) && lens.onScreen(out[0], out[1])) n++
            }
            return n
        }
        val resting = drawn(SkyCamera.resting(135.0, 768f, 1664f))
        val widest = drawn(SkyCamera.resting(135.0, 768f, 1664f).zoomed(0.5f, 384f, 832f, 768f, 1664f))
        val closest = drawn(SkyCamera.resting(135.0, 768f, 1664f).zoomed(8f, 384f, 832f, 768f, 1664f))
        // Measured on this night (11 nov 2026): 812 at the resting field, 642 at the widest,
        // 66 at the closest. Never the catalogue's thousands, at any zoom.
        assertTrue("resting: $resting", resting in 600..1000)
        assertTrue("widest: $widest", widest in 450..resting)
        assertTrue("closest: $closest", closest in 30..120)
    }

    /** A wide field names the few everybody knows, a close one all thirty-one, and the Pole Star always. */
    @Test
    fun `the closer the field, the fainter the stars it names`() {
        val named = { field: Double -> NamedStars.count { ExploreNames.named(it.mag, it.decDeg, field) } }
        // The first magnitude at the widest: fourteen stars in the whole sky, and the Pole Star.
        assertEquals(1.0, ExploreNames.faintestNamed(90.0), 1e-9)
        assertEquals(15, named(90.0))
        assertTrue(ExploreNames.named(2.02, 89.264, 90.0))
        assertFalse(ExploreNames.named(1.64, 6.35, 90.0)) // Bellatrix waits for a closer look
        assertTrue(ExploreNames.named(1.64, 6.35, 60.0))
        assertEquals(NamedStars.size, named(45.0))
        assertEquals(NamedStars.size, named(15.0))
        assertTrue(named(75.0) in 17 until NamedStars.size)
    }

    /** What shows is the hero's rule: the dark and the moon. */
    @Test
    fun `a dark night shows it all, a full moon takes most of it, the day everything`() {
        val night = dome(dark)
        assertFalse(night.sunUp)
        assertTrue("a moonless night shows its stars: ${night.starsShown}", night.starsShown > 0.95f)
        assertTrue(night.milkyWayShown > 0.9f)
        assertEquals(1f, night.planetsShown, 0.05f)

        val moon = dome(moonlit)
        assertTrue(moon.moonUp)
        assertTrue(moon.moonIllumination > 0.9)
        assertTrue("a full moon takes the faint stars: ${moon.starsShown}", moon.starsShown in 0.3f..0.5f)
        assertTrue("and all but the Milky Way: ${moon.milkyWayShown}", moon.milkyWayShown < 0.05f)
        assertTrue(moon.planetsShown > 0.6f)

        val day = dome(noon)
        assertTrue(day.sunUp)
        assertEquals(0f, day.starsShown, 0f)
        assertEquals(0f, day.planetsShown, 0f)
        assertEquals(0f, day.milkyWayShown, 0f)
        assertNull(day.radiant)
    }

    @Test
    fun `the radiant is there on a strong shower's peak night and on no other`() {
        // The Geminids' night, 13 to 14 December 2026.
        val peak = SkyDome.of(Instant.parse("2026-12-13T22:00:00Z"), milan, rome, emptySet())
        val radiant = assertNotNullAnd(peak.radiant)
        val place = AstronomyEngine.starHorizontal(112.0, 33.0, peak.at, milan) // Castor's corner of the Twins
        assertTrue(kotlin.math.abs(radiant.altitudeDeg - place.altitudeDeg) < 3.0)
        assertNull(dome(dark).radiant)
    }

    private fun <T> assertNotNullAnd(value: T?): T {
        assertNotNull(value)
        return value!!
    }

    /** The Milky Way's grid: on the galactic plane, brightest at the centre, closed, ending in the sky. */
    @Test
    fun `the band runs along the galactic plane and is brightest toward its centre`() {
        assertEquals(MilkyWay.COLUMNS * MilkyWay.LATITUDES, MilkyWay.vertexCount)
        assertEquals(MilkyWay.vertexCount * 3, MilkyWay.equatorial.size)
        val middle = (MilkyWay.LATITUDES - 1) / 2
        val dome = dome(dark)
        val directions = dome.milkyWayDirections

        // The first column's middle is the galactic centre, where the engine has it.
        val core = AstronomyEngine.horizontal(AstronomyEngine.galacticCore, dark, milan)
        val c = middle * 3
        assertTrue(off(directions[c], directions[c + 1], directions[c + 2], core.altitudeDeg, core.azimuthDeg) < 0.2)

        // The band closes: the last column is the first again.
        val last = ((MilkyWay.COLUMNS - 1) * MilkyWay.LATITUDES + middle)
        assertEquals(MilkyWay.equatorial[c], MilkyWay.equatorial[3 * last], 1e-6f)
        assertEquals(MilkyWay.strength[middle], MilkyWay.strength[last], 1e-6f)

        // Brightest toward the centre, a third of it toward the anticentre, nothing at the edges.
        val alongThePlane = (0 until MilkyWay.LONGITUDES).map { MilkyWay.strength[it * MilkyWay.LATITUDES + middle] }
        val opposite = MilkyWay.strength[(MilkyWay.LONGITUDES / 2) * MilkyWay.LATITUDES + middle]
        assertTrue(alongThePlane.max() <= 1f)
        assertTrue(alongThePlane.indexOf(alongThePlane.max()).let { it < 15 || it > MilkyWay.LONGITUDES - 15 })
        assertTrue(opposite < 0.4f * alongThePlane.max())
        for (column in 0 until MilkyWay.COLUMNS) {
            assertEquals(0f, MilkyWay.strength[column * MilkyWay.LATITUDES], 0f)
            assertEquals(0f, MilkyWay.strength[column * MilkyWay.LATITUDES + MilkyWay.LATITUDES - 1], 0f)
        }

        // Every vertex of the middle row is on the plane: a right angle from the galactic pole.
        val pole = AstronomyEngine.starHorizontal(192.85948, 27.12825, dark, milan)
        for (column in 0 until MilkyWay.COLUMNS) {
            val i = 3 * (column * MilkyWay.LATITUDES + middle)
            assertEquals(90.0, off(directions[i], directions[i + 1], directions[i + 2], pole.altitudeDeg, pole.azimuthDeg), 0.05)
        }
    }


    /**
     * Step 16.7: the constellations' lines are turned by the dome's one rotation, and each
     * end is where the engine puts the star it stands on. All 743 segments of the
     * eighty-eight, both ends, in both hemispheres.
     */
    @Test
    fun `every end of a constellation's line is where the engine puts its star`() {
        for ((at, coords) in listOf(dark to milan, moonlit to sydney)) {
            val dome = dome(at, coords)
            val lines = dome.constellationLines
            assertEquals(Constellations.all.sumOf { it.segmentCount } * 6, lines.size)
            assertEquals(743 * 6, lines.size)
            var segment = 0
            var worst = 0.0
            for (c in Constellations.all) {
                for (s in 0 until c.segmentCount) {
                    val from = s * Constellation.FLOATS_A_SEGMENT
                    for (end in 0..1) {
                        val hz = AstronomyEngine.starHorizontal(
                            c.lines[from + 2 * end].toDouble(), c.lines[from + 2 * end + 1].toDouble(), at, coords
                        )
                        val i = 6 * segment + 3 * end
                        worst = maxOf(worst, off(lines[i], lines[i + 1], lines[i + 2], hz.altitudeDeg, hz.azimuthDeg))
                    }
                    segment++
                }
            }
            assertTrue("the worst end is $worst degrees off the engine", worst < 1e-3)
        }
    }

    /** A line ends on a star the canvas draws: the dome's own direction for that star, to a float. */
    @Test
    fun `a line of the Plough ends on the dome's own stars`() {
        val dome = dome(dark)
        val plough = Constellations.byId("UMa")!!
        val before = Constellations.all.takeWhile { it.id != "UMa" }.sumOf { it.segmentCount }
        val stars = dome.starDirections
        for (s in 0 until plough.segmentCount) {
            for (end in 0..1) {
                val i = 6 * (before + s) + 3 * end
                val nearest = (0 until BrightStars.count).minOf { star ->
                    val de = stars[3 * star] - dome.constellationLines[i]
                    val dn = stars[3 * star + 1] - dome.constellationLines[i + 1]
                    val du = stars[3 * star + 2] - dome.constellationLines[i + 2]
                    de * de + dn * dn + du * du
                }
                assertTrue("an end of the Plough is $nearest from the nearest star", nearest < 1e-10f)
            }
        }
    }

    @Test
    fun `each constellation's name is where the engine puts its point, the Serpent's two included`() {
        val dome = dome(dark)
        val names = ConstellationUnits.names
        assertEquals(89, names.size)
        assertEquals(2, names.count { it.id == "Ser" })
        assertEquals(Constellations.all.map { it.id }, names.map { it.id }.distinct())
        val points = Constellations.all.flatMap { it.labels }
        val places = dome.constellationNamePlaces
        points.forEachIndexed { i, point ->
            val hz = AstronomyEngine.starHorizontal(point.raDeg, point.decDeg, dark, milan)
            assertTrue(off(places[3 * i], places[3 * i + 1], places[3 * i + 2], hz.altitudeDeg, hz.azimuthDeg) < 1e-3)
        }
        assertEquals(Constellations.all.flatMap { c -> c.labels.map { c.rank } }, names.map { it.rank })
    }

    /**
     * The hour's cloud takes the stars by the hero's rule: the open sky squared for a star
     * and a planet, cubed for the Milky Way. And the figures go with the stars they join.
     */
    @Test
    fun `a cover takes the stars as the hero's rule says, and the figures with them`() {
        val dome = dome(dark)
        assertEquals(dome.starsShown, dome.starsUnder(0f), 0f)
        assertEquals(dome.planetsShown, dome.planetsUnder(0f), 0f)
        assertEquals(dome.milkyWayShown, dome.milkyWayUnder(0f), 0f)
        assertEquals(dome.starsShown * 0.25f, dome.starsUnder(0.5f), 1e-6f)
        assertEquals(dome.planetsShown * 0.25f, dome.planetsUnder(0.5f), 1e-6f)
        assertEquals(dome.milkyWayShown * 0.125f, dome.milkyWayUnder(0.5f), 1e-6f)
        assertEquals(0f, dome.starsUnder(1f), 0f)

        val clear = DomeLook(dome)
        assertEquals(1f, clear.figures, 1e-3f)
        assertEquals(1f, clear.discShown, 0f)
        val closed = DomeLook(dome, 1f)
        assertEquals(0f, closed.stars, 0f)
        assertEquals("no figure is traced over a sky with no stars", 0f, closed.figures, 0f)
        assertTrue("a covered moon is a pale patch, not nothing", closed.discShown in 0.1f..0.3f)
        assertEquals(0f, closed.glowShown, 0f)
        // By day there are no stars to join, whatever the cloud.
        assertEquals(0f, DomeLook(dome(noon)).figures, 0f)
        // Under a full moon the figures are fainter with their stars, and still there.
        val moon = DomeLook(dome(moonlit))
        assertTrue("under a full moon: ${moon.figures}", moon.figures > 0.5f && moon.figures < 0.9f)
    }
}
