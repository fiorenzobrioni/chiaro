package com.callbackdev.chiaro.ui.sky.explore

import com.callbackdev.chiaro.domain.model.Coordinates
import com.callbackdev.chiaro.domain.sky.AstronomyEngine
import com.callbackdev.chiaro.domain.sky.BrightStars
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
}
