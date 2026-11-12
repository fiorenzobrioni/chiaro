package com.callbackdev.chiaro.ui.sky.explore

import com.callbackdev.chiaro.data.SkySubscription
import com.callbackdev.chiaro.domain.model.Coordinates
import com.callbackdev.chiaro.domain.sky.AstronomyEngine
import com.callbackdev.chiaro.domain.sky.MeteorShowerTable
import com.callbackdev.chiaro.domain.sky.Planet
import com.callbackdev.chiaro.domain.sky.SkyJobCatalog
import com.callbackdev.chiaro.domain.sky.SkyLead
import com.callbackdev.chiaro.domain.sky.SkyOccurrence
import com.callbackdev.chiaro.domain.sky.SkyTarget
import com.callbackdev.chiaro.widget.window.NamedStars
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A thing pointed at in the explorer (Fase 16, step 16.8): which thing a tap means, what
 * the sheet knows about it, and which of them have a bell and a page.
 */
class ExploreObjectTest {

    private val milan = Coordinates(45.4642, 9.19)
    private val zone = ZoneId.of("Europe/Rome")

    /** 11 Nov 2026, 23:00 in Milan: Saturn in the south-west, Jupiter and Mars rising, Orion up. */
    private val at = Instant.parse("2026-11-11T22:00:00Z")
    private val night = SkyOccurrence.At(
        SkyJobCatalog.DarknessWindow, Instant.parse("2026-11-11T17:40:00Z"), Instant.parse("2026-11-12T04:36:00Z")
    )
    private val dome = SkyDome.of(at, milan, zone, emptySet())

    // ------------------------------------------------------------------ which thing

    @Test
    fun `a tap takes the nearest thing within reach, and nothing beyond it`() {
        val vega = PickSpot(SkyPick.Star(4), 100f, 100f)
        val jupiter = PickSpot(SkyPick.Wanderer(Planet.JUPITER), 160f, 100f)
        val spots = listOf(jupiter, vega)
        assertEquals(SkyPick.Star(4), ExplorePick.nearest(110f, 100f, spots, 48f))
        assertEquals(SkyPick.Wanderer(Planet.JUPITER), ExplorePick.nearest(140f, 100f, spots, 48f))
        // Forty-eight px is the reach: at forty-nine from both there is nothing.
        assertNull(ExplorePick.nearest(100f, 149f, spots, 48f))
        assertEquals(SkyPick.Star(4), ExplorePick.nearest(100f, 148f, spots, 48f))
        assertNull(ExplorePick.nearest(100f, 100f, emptyList(), 48f))
    }

    @Test
    fun `the moon is reached from its limb and a name from its box`() {
        val moon = PickSpot(SkyPick.Moon, 200f, 200f, radiusPx = 30f)
        // Seventy px from the centre is forty from the limb.
        assertEquals(40f, moon.distance(270f, 200f), 1e-4f)
        assertEquals(0f, moon.distance(210f, 215f), 1e-4f)
        assertEquals(SkyPick.Moon, ExplorePick.nearest(270f, 200f, listOf(moon), 48f))

        val lyra = PickSpot.box(SkyPick.Figure(3), left = 300f, top = 90f, right = 380f, bottom = 110f)
        assertEquals(0f, lyra.distance(340f, 100f), 1e-4f)
        assertEquals(20f, lyra.distance(400f, 100f), 1e-4f)
        assertEquals(5f, lyra.distance(297f, 86f), 1e-4f)
    }

    /** Vega's dot and the word «LYRA» beside it are two things, and a finger says which. */
    @Test
    fun `a tap on a star is the star, and a tap on the name beside it is the constellation`() {
        val vega = PickSpot(SkyPick.Star(4), 100f, 100f)
        val lyra = PickSpot.box(SkyPick.Figure(3), left = 112f, top = 92f, right = 170f, bottom = 108f)
        val spots = listOf(vega, lyra)
        assertEquals(SkyPick.Star(4), ExplorePick.nearest(98f, 101f, spots, 48f))
        assertEquals(SkyPick.Figure(3), ExplorePick.nearest(140f, 100f, spots, 48f))
        // At the same distance from both, the one the page lists first: the light.
        assertEquals(SkyPick.Star(4), ExplorePick.nearest(106f, 100f, spots, 48f))
    }

    @Test
    fun `a pick is found again by its key`() {
        val picks = listOf(
            SkyPick.Moon,
            SkyPick.Wanderer(Planet.SATURN),
            SkyPick.Star(NamedStars.lastIndex),
            SkyPick.Figure(0),
            SkyPick.Radiant(MeteorShowerTable.jobId(MeteorShowerTable.all.first()))
        )
        for (pick in picks) assertEquals(pick, SkyPick.of(pick.key))
        assertNull(SkyPick.of("star:${NamedStars.size}"))
        assertNull(SkyPick.of("planet:PLUTO"))
        assertNull(SkyPick.of("radiant:sun.rise"))
        assertNull(SkyPick.of("comet"))
    }

    // ------------------------------------------------------- what can be pointed at

    @Test
    fun `the lights that show are the spots, where the camera puts them`() {
        val camera = SkyCamera(225.0, 30.0)
        val look = DomeLook(dome)
        val spots = pickSpots(dome, look, camera, 1000f, 2000f, emptyList(), 2f)
        val lens = camera.lens(1000f, 2000f)
        val out = FloatArray(2)
        val saturn = spots.single { it.pick == SkyPick.Wanderer(Planet.SATURN) }
        val place = dome.planets.single { it.planet == Planet.SATURN }.place
        assertTrue(lens.toScreen(place.east, place.north, place.up, out))
        assertEquals(out[0], saturn.x, 1e-3f)
        assertEquals(out[1], saturn.y, 1e-3f)
        // Every spot is on the screen, and every star among them is one of the named, up.
        assertTrue(spots.all { lens.onScreen(it.x, it.y) })
        assertTrue(spots.any { it.pick is SkyPick.Star })
        for (spot in spots) {
            val pick = spot.pick
            if (pick is SkyPick.Star) assertTrue(dome.named.any { it.star === NamedStars[pick.index] })
        }
        // The moon had set that evening: it is not a spot.
        assertFalse(dome.moonUp)
        assertTrue(spots.none { it.pick == SkyPick.Moon })
    }

    @Test
    fun `under a closed sky there is nothing to point at but the names the page wrote`() {
        val camera = SkyCamera(225.0, 30.0)
        val closed = DomeLook(dome, cover = 1f)
        assertTrue(pickSpots(dome, closed, camera, 1000f, 2000f, emptyList(), 2f).isEmpty())
        val written = listOf(PickSpot.box(SkyPick.Figure(1), 10f, 10f, 60f, 30f))
        assertEquals(written, pickSpots(dome, closed, camera, 1000f, 2000f, written, 2f))
    }

    // ------------------------------------------------------------ what the sheet knows

    @Test
    fun `a planet's sheet is its place, its passage and until when, and they are the engine's`() {
        val facts = ExploreObjects.facts(SkyPick.Wanderer(Planet.SATURN), dome, night, milan)!!
        val target = SkyTarget.Wanderer(Planet.SATURN)
        val now = AstronomyEngine.horizontal(target, at, milan)
        assertEquals(now.altitudeDeg, facts.place.altitudeDeg, 1e-9)
        assertEquals(now.azimuthDeg, facts.place.azimuthDeg, 1e-9)
        val dark = night.start..night.end!!
        assertEquals(AstronomyEngine.passage(target, dark, milan), facts.passage)
        assertEquals(AstronomyEngine.worthUntil(target, dark, milan, 10.0), facts.worthUntil)
        // Saturn sets in the small hours: worth it until then, not until dawn.
        assertNotNull(facts.worthUntil)
        assertFalse(facts.untilDawn)
        assertFalse(facts.lowNow)
        assertEquals(SkyJobCatalog.SaturnNight.id, facts.bellJobId)
        assertEquals(SkyJobCatalog.SaturnNight.id, facts.pageJobId)
    }

    @Test
    fun `Mars rising outlasts the night`() {
        val facts = ExploreObjects.facts(SkyPick.Wanderer(Planet.MARS), dome, night, milan)!!
        assertEquals(night.end, facts.worthUntil)
        assertTrue(facts.untilDawn)
        assertEquals(SkyJobCatalog.MarsNight.id, facts.bellJobId)
    }

    /** The step's own words: a star has no bell and no page, and the sheet does not feign them. */
    @Test
    fun `a star and a constellation have no bell and no page, and the moon has a page and no bell`() {
        val capella = NamedStars.indexOfFirst { it.raDeg == 79.172 }
        val star = ExploreObjects.facts(SkyPick.Star(capella), dome, night, milan)!!
        assertNull(star.bellJobId)
        assertNull(star.pageJobId)
        assertEquals(SkyTarget.Fixed(79.172, 45.998), star.target)

        val figure = ExploreObjects.facts(SkyPick.Figure(0), dome, night, milan)!!
        assertNull(figure.bellJobId)
        assertNull(figure.pageJobId)

        assertNull(ExploreObjects.bellJob(SkyPick.Moon, 300.0))
        assertEquals(SkyJobCatalog.MoonToday.id, ExploreObjects.pageJob(SkyPick.Moon, 300.0))
    }

    @Test
    fun `Venus is the evening star with the sun in the west and the morning star with it in the east`() {
        val venus = SkyPick.Wanderer(Planet.VENUS)
        assertEquals(SkyJobCatalog.VenusEvening.id, ExploreObjects.bellJob(venus, 290.0))
        assertEquals(SkyJobCatalog.VenusMorning.id, ExploreObjects.bellJob(venus, 70.0))
        assertEquals(SkyJobCatalog.JupiterNight.id, ExploreObjects.bellJob(SkyPick.Wanderer(Planet.JUPITER), 290.0))
    }

    /** Every bell and every page is a line the catalog has: none points at nothing. */
    @Test
    fun `every bell and every page is a line of the catalog`() {
        val picks = Planet.entries.map { SkyPick.Wanderer(it) } + SkyPick.Moon +
            MeteorShowerTable.all.map { SkyPick.Radiant(MeteorShowerTable.jobId(it)) }
        for (pick in picks) {
            for (sun in listOf(70.0, 290.0)) {
                listOfNotNull(ExploreObjects.bellJob(pick, sun), ExploreObjects.pageJob(pick, sun)).forEach {
                    assertNotNull("$pick: $it", SkyJobCatalog.byId(it))
                }
            }
        }
    }

    @Test
    fun `the Pole Star never sets from Milan, and the sheet is told`() {
        val polaris = NamedStars.lastIndex
        val facts = ExploreObjects.facts(SkyPick.Star(polaris), dome, night, milan)!!
        assertTrue(facts.passage.alwaysUp)
        assertNull(facts.passage.rise)
        assertNull(facts.passage.set)
        assertTrue(facts.untilDawn)
    }

    @Test
    fun `on a date with no night the passage is the one round the instant, and nothing is said of until when`() {
        val facts = ExploreObjects.facts(SkyPick.Wanderer(Planet.SATURN), dome, night = null, coords = milan)!!
        assertNull(facts.worthUntil)
        assertFalse(facts.untilDawn)
        assertEquals(AstronomyEngine.passage(SkyTarget.Wanderer(Planet.SATURN), at..at, milan), facts.passage)
    }

    @Test
    fun `a radiant is in the dome on its peak night only`() {
        val geminids = MeteorShowerTable.jobId(MeteorShowerTable.all.first { it.id == "geminids" })
        assertNull(ExploreObjects.facts(SkyPick.Radiant(geminids), dome, night, milan))
        assertEquals(geminids, ExploreObjects.bellJob(SkyPick.Radiant(geminids), 300.0))
    }

    // ------------------------------------------------------------------ the bell

    @Test
    fun `a bell is on for a moment followed with a reminder that is not never`() {
        val lines = listOf(
            SkySubscription("jupiter.night"),
            SkySubscription("mars.night", notifyLeadMinutes = 0),
            SkySubscription("saturn.night", notifyLeadMinutes = 60),
            SkySubscription("venus.evening", enabled = false, notifyLeadMinutes = 60)
        )
        assertEquals(setOf("jupiter.night", "saturn.night"), remindedJobs(lines, SkyLead.THIRTY))
        // With the usual reminder off, only a moment with a lead of its own rings.
        assertEquals(setOf("saturn.night"), remindedJobs(lines, SkyLead.OFF))
    }
}
