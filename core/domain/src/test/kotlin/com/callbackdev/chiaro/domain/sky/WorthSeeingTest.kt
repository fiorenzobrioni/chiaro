package com.callbackdev.chiaro.domain.sky

import com.callbackdev.chiaro.domain.model.Coordinates
import com.callbackdev.chiaro.domain.model.MoonPhase
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * «Da vedere»: what is worth looking at on one night from one place, each thing at its
 * best hour (PLANNING, Fase 16, step 16.4).
 *
 * Two kinds of assertion. Where a row is a place in the sky at an hour, the numbers are
 * **JPL Horizons', read on 10 Nov 2026** (`ssd.jpl.nasa.gov/api/horizons.api`, observer
 * table at Milan, 45.4642 N 9.19 E, refracted, quantity 4, one-minute steps; targets 301,
 * 299, 499, 599, 699), the same reading as `SkyPassageTest`'s plus the rows of the two
 * twilights' edges: Horizons marks each minute with the twilight it is in, so the minute
 * the mark changes is the minute the sun crosses six or twelve degrees. Where a row is a
 * rule (what enters, and from when), the test re-derives the rule from the engine's
 * primitives instead of pinning this code's own output: a figure's window opens when its
 * lowest star reaches the floor, and the test asks the star.
 */
class WorthSeeingTest {

    private val milan = Coordinates(45.4642, 9.19)
    private val rome = ZoneId.of("Europe/Rome")
    private val sydney = Coordinates(-33.8688, 151.2093)
    private val sydneyZone = ZoneId.of("Australia/Sydney")

    /** The night of the phase's other engine tests: Saturn in the evening, Mars and
     * Jupiter from midnight, Venus before dawn, the moon a day past new. */
    private val reference = LocalDate.of(2026, 11, 10)

    private fun name(subject: SightSubject): String = when (subject) {
        is SightSubject.Moon -> "moon"
        is SightSubject.Wanderer -> subject.planet.name.lowercase()
        is SightSubject.Figure -> subject.figure.id
        SightSubject.MilkyWay -> "milky_way"
        is SightSubject.Radiant -> "radiant:${subject.shower.id}"
    }

    private fun List<Sight>.named(name: String): Sight? = firstOrNull { name(it.subject) == name }

    private fun altitude(target: SkyTarget, at: Instant, coords: Coordinates) =
        AstronomyEngine.horizontal(target, at, coords).altitudeDeg

    private fun assertPlace(what: String, sight: Sight, azimuthDeg: Double, altitudeDeg: Double, altitudeTolerance: Double = 0.3) {
        val turn = abs(AstronomyMath.norm180(sight.azimuthDeg - azimuthDeg))
        assertTrue("$what faces ${sight.azimuthDeg}, Horizons $azimuthDeg", turn < 0.6)
        assertEquals("$what altitude", altitudeDeg, sight.altitudeDeg, altitudeTolerance)
    }

    private fun assertBetween(what: String, from: String, to: String, actual: Instant) {
        assertTrue(
            "$what at $actual, expected $from .. $to",
            !actual.isBefore(Instant.parse(from)) && !actual.isAfter(Instant.parse(to))
        )
    }

    // ------------------------------------------------------- one night, whole

    @Test
    fun `the reference night from Milan, in the order of the hours`() {
        val list = WorthSeeing.of(reference, rome, milan)
        assertEquals(
            listOf(
                "summer_triangle", "great_square", "saturn", "cassiopeia", "pleiades", "hyades", "orion",
                "twins", "mars", "lion", "big_dipper", "venus", "jupiter"
            ),
            list.map { name(it.subject) }
        )
        // The moon set before the sun was six degrees down, the galaxy's core is under
        // the horizon all night in November, no shower peaks: three rows the night does
        // not have, and the list does not invent.
    }

    /** Saturn tops inside the dark: its best hour is its highest point, Horizons' transit. */
    @Test
    fun `a planet that tops in the dark is best when it tops`() {
        val saturn = WorthSeeing.of(reference, rome, milan).named("saturn")!!
        assertBetween("Saturn's best", "2026-11-10T20:38:00Z", "2026-11-10T20:42:00Z", saturn.best)
        assertPlace("Saturn", saturn, azimuthDeg = 180.21, altitudeDeg = 45.57)
        assertEquals(SkyHeight.HALFWAY, saturn.height)
        // A star among the stars: from the minute the sun is twelve degrees down, which
        // Horizons marks between 17:05 and 17:06 UTC, until it sinks to its floor.
        assertBetween("Saturn's window opens", "2026-11-10T17:04:00Z", "2026-11-10T17:07:00Z", saturn.window.start)
        assertEquals(
            SkySights.minAltitude(Planet.SATURN),
            altitude(saturn.target, saturn.window.endInclusive, milan), 0.05
        )
    }

    /**
     * Mars and Jupiter top after the dawn has begun: their best hour is the end of their
     * dark, and the two darks are not the same one. Mars leaves with the stars, when the
     * sun is back to twelve degrees (Horizons: between 05:09 and 05:10 UTC); Jupiter and
     * Venus stay until six (between 05:44 and 05:45).
     */
    @Test
    fun `a planet still climbing at dawn is best when its own dark ends`() {
        val list = WorthSeeing.of(reference, rome, milan)

        val mars = list.named("mars")!!
        assertBetween("Mars' best", "2026-11-11T05:08:00Z", "2026-11-11T05:11:00Z", mars.best)
        assertEquals(mars.window.endInclusive, mars.best)
        assertPlace("Mars", mars, azimuthDeg = 161.67, altitudeDeg = 58.83)

        // Horizons' rows of 05:44 and 05:45, the two minutes the sun crosses six degrees
        // between: Jupiter 175.61 and 176.08 at 58.21 and 58.22, Venus 123.84 and 124.04 at
        // 15.07 and 15.21.
        val jupiter = list.named("jupiter")!!
        assertBetween("Jupiter's best", "2026-11-11T05:43:00Z", "2026-11-11T05:46:00Z", jupiter.best)
        assertEquals(jupiter.window.endInclusive, jupiter.best)
        assertPlace("Jupiter", jupiter, azimuthDeg = 175.85, altitudeDeg = 58.21)

        val venus = list.named("venus")!!
        assertEquals(jupiter.best, venus.best)
        assertPlace("Venus", venus, azimuthDeg = 123.94, altitudeDeg = 15.14)
        assertEquals(SkyHeight.LOW, venus.height)
        // And Venus opens at its own floor, five degrees, an hour and a half after it rose.
        assertEquals(SkySights.minAltitude(Planet.VENUS), altitude(venus.target, venus.window.start, milan), 0.05)
    }

    /**
     * The moon nine days old, on 19 November: Horizons has it highest at 19:07 UTC, facing
     * 181.59 at 43.77 degrees from the ground (the engine's altitude is from the earth's
     * centre, six tenths higher there: the tolerance `SkyPassageTest` gives the moon).
     */
    @Test
    fun `the moon is on the list with the night's own phase, best when it is highest`() {
        val date = LocalDate.of(2026, 11, 19)
        val moon = WorthSeeing.of(date, rome, milan).named("moon")!!
        assertBetween("the moon's best", "2026-11-19T19:04:00Z", "2026-11-19T19:10:00Z", moon.best)
        assertPlace("the moon", moon, azimuthDeg = 181.59, altitudeDeg = 43.77, altitudeTolerance = 1.2)

        // The phase and the percentage are the night's, the ones the hero prints.
        val tonight = MoonOfTheNight.instant(date, rome, milan)
        val subject = moon.subject as SightSubject.Moon
        assertEquals(MoonPhase.WAXING_GIBBOUS, subject.phase)
        assertEquals((AstronomyEngine.moonIllumination(tonight).illuminatedFraction * 100).roundToInt(), subject.illuminationPct)

        // From the sun six degrees down until the moon sinks to five.
        assertEquals(SkyAlmanac.solarDay(date, rome, milan).civilDusk, moon.window.start)
        assertEquals(WorthSeeing.MOON_MIN_ALTITUDE, altitude(SkyTarget.Moon, moon.window.endInclusive, milan), 0.05)
    }

    @Test
    fun `a moon that is new, or down before the dusk, is not a row`() {
        // 12 Aug 2026: the day of the new moon. 10 Nov: a thin moon that set at 17:09.
        assertNull(WorthSeeing.of(LocalDate.of(2026, 8, 12), rome, milan).named("moon"))
        assertNull(WorthSeeing.of(reference, rome, milan).named("moon"))
    }

    // ------------------------------------------------------------ the figures

    @Test
    fun `a figure is on the list while every one of its stars is clear of the roofs`() {
        val list = WorthSeeing.of(reference, rome, milan)
        // The Plough from Milan in November: Alkaid, the end of the handle, goes under ten
        // degrees in the evening and the figure waits for it.
        val plough = list.named("big_dipper")!!
        val lowest = SkyFigures.BigDipper.stars.minOf { altitude(it, plough.window.start, milan) }
        assertEquals(WorthSeeing.FIGURE_MIN_ALTITUDE, lowest, 0.05)
        assertEquals(
            WorthSeeing.FIGURE_MIN_ALTITUDE,
            altitude(SkyFigures.BigDipper.stars.last(), plough.window.start, milan), 0.05
        )
        // Cassiopeia never goes that low from here: all the starry dark, best overhead-ish
        // in the north, where a thing sixty degrees from the equator tops at 45 N.
        val cassiopeia = list.named("cassiopeia")!!
        val day = SkyAlmanac.solarDay(reference, rome, milan)
        val next = SkyAlmanac.solarDay(reference.plusDays(1), rome, milan)
        assertEquals(day.nauticalDusk!!..next.nauticalDawn!!, cassiopeia.window)
        assertEquals(SkyHeight.HIGH, cassiopeia.height)
        assertEquals(90.0 - (SkyFigures.Cassiopeia.centre.decDeg - milan.lat), cassiopeia.altitudeDeg, 0.1)
    }

    @Test
    fun `a cluster waits for twenty degrees`() {
        val pleiades = WorthSeeing.of(reference, rome, milan).named("pleiades")!!
        assertEquals(
            WorthSeeing.CLUSTER_MIN_ALTITUDE,
            altitude(SkyFigures.Pleiades.centre, pleiades.window.start, milan), 0.05
        )
        // Best on the meridian, at ninety minus the latitude plus its declination.
        assertEquals(90.0 - milan.lat + SkyFigures.Pleiades.centre.decDeg, pleiades.altitudeDeg, 0.05)
        assertEquals(180.0, pleiades.azimuthDeg, 0.5)
    }

    /**
     * The Scorpion's sting is 37 degrees south of the equator: from Milan it tops at seven
     * and a half, under the floor, every night of the year; from Rome at eleven, for an
     * hour or two of a summer night; from Sydney the whole figure goes overhead.
     */
    @Test
    fun `the Scorpion is never on Milan's list, briefly on Rome's, overhead on Sydney's`() {
        var date = LocalDate.of(2026, 1, 1)
        while (date.year == 2026) {
            assertNull("the Scorpion from Milan on $date", WorthSeeing.of(date, rome, milan).named("scorpion"))
            date = date.plusDays(6)
        }
        val fromRome = WorthSeeing.of(LocalDate.of(2026, 7, 10), rome, Coordinates(41.9028, 12.4964)).named("scorpion")
        assertNotNull(fromRome)
        assertEquals(SkyHeight.LOW, fromRome!!.height)
        assertTrue(Duration.between(fromRome.window.start, fromRome.window.endInclusive) < Duration.ofHours(3))

        val fromSydney = WorthSeeing.of(LocalDate.of(2026, 6, 15), sydneyZone, sydney).named("scorpion")!!
        assertEquals(SkyHeight.OVERHEAD, fromSydney.height)
    }

    @Test
    fun `the south has the Cross and never the Plough or Cassiopeia`() {
        var date = LocalDate.of(2026, 1, 1)
        var crossNights = 0
        while (date.year == 2026) {
            val list = WorthSeeing.of(date, sydneyZone, sydney)
            assertNull(list.named("big_dipper"))
            assertNull(list.named("cassiopeia"))
            if (list.named("southern_cross") != null) crossNights++
            date = date.plusDays(9)
        }
        assertTrue("the Cross on $crossNights sampled nights", crossNights > 30)
        // And from Milan the Cross does not rise.
        assertNull(WorthSeeing.of(LocalDate.of(2026, 5, 1), rome, milan).named("southern_cross"))
    }

    // ------------------------------------------------- the planet nobody would find

    /**
     * Mars on the far side of its orbit: up in the dark for hours and a second-magnitude
     * point among others. The night is found, not remembered: the first from 2028 on which
     * Mars clears its floor in the starry dark for an hour and is fainter than the cut.
     */
    @Test
    fun `Mars fainter than the cut is up and is not on the list`() {
        val target = SkyTarget.Wanderer(Planet.MARS)
        fun longUpAndFaint(date: LocalDate): Boolean {
            val day = SkyAlmanac.solarDay(date, rome, milan)
            val next = SkyAlmanac.solarDay(date.plusDays(1), rome, milan)
            val run = AstronomyEngine
                .watchRuns(target, day.nauticalDusk!!..next.nauticalDawn!!, milan, SkySights.minAltitude(Planet.MARS))
                .firstOrNull { Duration.between(it.start, it.endInclusive) >= Duration.ofHours(1) }
                ?: return false
            return AstronomyEngine.planetMagnitude(Planet.MARS, run.start) > WorthSeeing.PLANET_FAINTEST_MAG &&
                AstronomyEngine.planetMagnitude(Planet.MARS, run.endInclusive) > WorthSeeing.PLANET_FAINTEST_MAG
        }
        val date = generateSequence(LocalDate.of(2028, 1, 1)) { it.plusDays(7) }
            .takeWhile { it.year < 2036 }
            .firstOrNull(::longUpAndFaint)
        assertNotNull("in eight years Mars is never up and that faint: the rule would be dead", date)

        assertNull("Mars on $date", WorthSeeing.of(date!!, rome, milan).named("mars"))
        // The same planet on the reference night, brighter than any star of the Lion, is a row.
        assertNotNull(WorthSeeing.of(reference, rome, milan).named("mars"))
        assertTrue(AstronomyEngine.planetMagnitude(Planet.MARS, Instant.parse("2026-11-11T05:09:00Z")) < 1.0)
        // And the two the rule is not about are on the list every night they are up.
        listOf(Planet.VENUS, Planet.JUPITER, Planet.SATURN).forEach { planet ->
            generateSequence(LocalDate.of(2026, 1, 1)) { it.plusDays(30) }.take(120).forEach { at ->
                val magnitude = AstronomyEngine.planetMagnitude(planet, at.atStartOfDay(rome).toInstant())
                assertTrue("$planet at $magnitude on $at", magnitude < WorthSeeing.PLANET_FAINTEST_MAG)
            }
        }
    }

    // ------------------------------------------------ the Milky Way and a radiant

    @Test
    fun `the Milky Way is the catalog's own line, and the Perseids' radiant is best at the end of the night`() {
        val date = LocalDate.of(2026, 8, 12)
        val list = WorthSeeing.of(date, rome, milan)

        val core = SkyScheduler.resolve(SkyJobCatalog.MilkyWayCore, date, rome, milan) as SkyOccurrence.At
        val milkyWay = list.named("milky_way")!!
        assertEquals(core.start..core.end!!, milkyWay.window)
        // From Milan the core never clears seventeen degrees: low, and in the south.
        assertEquals(SkyHeight.LOW, milkyWay.height)
        assertTrue("faces ${milkyWay.azimuthDeg}", milkyWay.azimuthDeg in 150.0..215.0)
        assertEquals(AstronomyEngine.galacticCoreAltitude(milkyWay.best, milan), milkyWay.altitudeDeg, 1e-6)

        // The radiant climbs all night in August: its best hour is the dawn that ends the
        // shower's window, high in the north-east.
        val perseids = SkyScheduler.resolve(SkyJobCatalog.byId("meteor.perseids.peak")!!, date, rome, milan) as SkyOccurrence.At
        val radiant = list.named("radiant:perseids")!!
        assertEquals(perseids.end, radiant.best)
        assertEquals(SkyHeight.HIGH, radiant.height)
        assertTrue("faces ${radiant.azimuthDeg}", radiant.azimuthDeg in 30.0..70.0)
    }

    @Test
    fun `no Milky Way under the moon, nor when the core is down`() {
        // 27 Aug 2026, the night before the full moon: the catalog's line has no window.
        val moonlit = LocalDate.of(2026, 8, 27)
        assertTrue(SkyScheduler.resolve(SkyJobCatalog.MilkyWayCore, moonlit, rome, milan) is SkyOccurrence.None)
        assertNull(WorthSeeing.of(moonlit, rome, milan).named("milky_way"))
        assertNull(WorthSeeing.of(reference, rome, milan).named("milky_way"))
    }

    @Test
    fun `a strong shower's radiant is on the list on its peak night and on no other`() {
        MeteorShowerTable.all.filter { it.strong }.forEach { shower ->
            val job = SkyJobCatalog.byId(MeteorShowerTable.jobId(shower))!!
            // The shower's 2026, which for the Quadrantids ends in January 2027.
            val peak = SkyScheduler.resolve(job, LocalDate.of(2026, 6, 1), rome, milan) as SkyOccurrence.At
            // The evening the peak's night begins on: the day before its dawn.
            val evening = peak.end!!.atZone(rome).toLocalDate().minusDays(1)
            val nights = (-3L..3L).map { evening.plusDays(it) }
                .filter { WorthSeeing.of(it, rome, milan).named("radiant:${shower.id}") != null }
            // On the peak night when the radiant gets ten degrees up inside the window for
            // a quarter of an hour, which the test asks the radiant; on no other, ever.
            val target = SkyTarget.Fixed(shower.radiantRaDeg, shower.radiantDecDeg)
            val night = SkyScheduler.darkNight(evening, rome, milan).night as SkyOccurrence.At
            val window = maxOf(peak.start, night.start)..peak.end!!
            val up = AstronomyEngine.watchRuns(target, window, milan, WorthSeeing.RADIANT_MIN_ALTITUDE)
                .any { Duration.between(it.start, it.endInclusive) >= WorthSeeing.MIN_WINDOW }
            assertEquals(shower.id, if (up) listOf(evening) else emptyList<LocalDate>(), nights)
        }
        // And the ones everybody waits for are really there.
        listOf(
            "perseids" to LocalDate.of(2026, 8, 12),
            "geminids" to LocalDate.of(2026, 12, 13),
            "quadrantids" to LocalDate.of(2027, 1, 3)
        ).forEach { (id, date) ->
            assertNotNull(id, WorthSeeing.of(date, rome, milan).named("radiant:$id"))
        }
    }

    @Test
    fun `a weak shower's radiant is only for a reader who follows it`() {
        // The Draconids, 8 Oct 2026: five or ten an hour.
        val date = LocalDate.of(2026, 10, 8)
        val job = "meteor.draconids.peak"
        assertFalse(MeteorShowerTable.showerOf(job)!!.strong)
        assertNull(WorthSeeing.of(date, rome, milan).named("radiant:draconids"))
        assertNotNull(WorthSeeing.of(date, rome, milan, followed = setOf(job)).named("radiant:draconids"))
        assertNull(WorthSeeing.of(date.plusDays(1), rome, milan, followed = setOf(job)).named("radiant:draconids"))
    }

    // ------------------------------------------------ nights that are not nights

    @Test
    fun `where the night is not dark enough the rows that need the dark are absent`() {
        val solstice = LocalDate.of(2026, 6, 21)
        // London: the sun gets twelve degrees down and not eighteen. The figures are out,
        // the Milky Way is not.
        val london = WorthSeeing.of(solstice, ZoneId.of("Europe/London"), Coordinates(51.5074, -0.1278))
        assertNotNull(london.named("summer_triangle"))
        assertNull(london.named("milky_way"))
        // Stockholm: six degrees and not twelve. Nothing made of stars.
        val stockholm = WorthSeeing.of(solstice, ZoneId.of("Europe/Stockholm"), Coordinates(59.3293, 18.0686))
        assertTrue(
            stockholm.map { name(it.subject) }.toString(),
            stockholm.all { name(it.subject) in setOf("moon", "venus", "jupiter") }
        )
        // Longyearbyen: the midnight sun. Nothing.
        val svalbard = Coordinates(78.2232, 15.6267)
        assertTrue(WorthSeeing.of(solstice, ZoneId.of("Arctic/Longyearbyen"), svalbard).isEmpty())
    }

    @Test
    fun `in the polar night the night is the whole day, from one noon to the next`() {
        val date = LocalDate.of(2026, 12, 21)
        val zone = ZoneId.of("Arctic/Longyearbyen")
        val svalbard = Coordinates(78.2232, 15.6267)
        val noon = SkyAlmanac.solarDay(date, zone, svalbard).solarNoon
        val nextNoon = SkyAlmanac.solarDay(date.plusDays(1), zone, svalbard).solarNoon
        // The sun does not come back up to six degrees under the horizon that day.
        assertTrue(AstronomyEngine.sunAltitude(noon, svalbard) < AstronomyEngine.CIVIL_TWILIGHT)

        val list = WorthSeeing.of(date, zone, svalbard)
        // The moon, six days from full and far north of the equator, does not set: its
        // stretch is the whole of that day.
        val moon = list.named("moon")!!
        assertEquals(noon..nextNoon, moon.window)
        assertNotNull(list.named("cassiopeia"))
        assertNotNull(list.named("big_dipper"))
        list.forEach { assertTrue(name(it.subject), !it.best.isBefore(noon) && !it.best.isAfter(nextNoon)) }
    }

    // ------------------------------------------------------------ what always holds

    @Test
    fun `every row is in order, inside its window, clear of its floor and at the top of it`() {
        val places = listOf(
            milan to rome,
            sydney to sydneyZone,
            Coordinates(-0.1807, -78.4678) to ZoneId.of("America/Guayaquil"),
            Coordinates(69.6492, 18.9553) to ZoneId.of("Europe/Oslo")
        )
        var rows = 0
        places.forEach { (coords, zone) ->
            var date = LocalDate.of(2026, 1, 4)
            while (date.year == 2026) {
                val list = WorthSeeing.of(date, zone, coords)
                assertEquals("in the order of the hours", list.sortedBy { it.best }, list)
                assertEquals("one row a thing", list.size, list.map { name(it.subject) }.toSet().size)
                list.forEach { sight ->
                    val what = "${name(sight.subject)} on $date at ${coords.lat}"
                    rows++
                    assertTrue(what, sight.best in sight.window)
                    assertTrue(what, Duration.between(sight.window.start, sight.window.endInclusive) >= WorthSeeing.MIN_WINDOW)
                    assertEquals(what, SkyHeight.of(sight.altitudeDeg), sight.height)
                    assertEquals(what, altitude(sight.target, sight.best, coords), sight.altitudeDeg, 1e-9)
                    assertTrue("$what stands ${sight.altitudeDeg}", sight.altitudeDeg >= floorOf(sight.subject) - 0.05)
                    // The best hour is the highest of the window: no instant of it stands higher.
                    val length = Duration.between(sight.window.start, sight.window.endInclusive)
                    (0..12).forEach { step ->
                        val at = sight.window.start.plus(length.multipliedBy(step.toLong()).dividedBy(12))
                        assertTrue("$what is higher at $at", altitude(sight.target, at, coords) <= sight.altitudeDeg + 0.02)
                    }
                }
                date = date.plusDays(13)
            }
        }
        assertTrue("$rows rows looked at", rows > 400)
    }

    /** The floor of the thing's own middle. A figure's is its lowest star's, so its middle is over it. */
    private fun floorOf(subject: SightSubject): Double = when (subject) {
        is SightSubject.Moon -> WorthSeeing.MOON_MIN_ALTITUDE
        is SightSubject.Wanderer -> SkySights.minAltitude(subject.planet)
        is SightSubject.Figure ->
            if (subject.figure.kind == SkyFigureKind.CLUSTER) WorthSeeing.CLUSTER_MIN_ALTITUDE else WorthSeeing.FIGURE_MIN_ALTITUDE
        SightSubject.MilkyWay -> 10.0
        is SightSubject.Radiant -> WorthSeeing.RADIANT_MIN_ALTITUDE
    }

    // ------------------------------------------------------------- the words

    @Test
    fun `how high is said in thirds of the sky, and overhead is a band of its own`() {
        assertEquals(SkyHeight.LOW, SkyHeight.of(5.0))
        assertEquals(SkyHeight.LOW, SkyHeight.of(29.9))
        assertEquals(SkyHeight.HALFWAY, SkyHeight.of(30.0))
        assertEquals(SkyHeight.HALFWAY, SkyHeight.of(59.9))
        assertEquals(SkyHeight.HIGH, SkyHeight.of(60.0))
        assertEquals(SkyHeight.HIGH, SkyHeight.of(79.9))
        assertEquals(SkyHeight.OVERHEAD, SkyHeight.of(80.0))
        assertEquals(SkyHeight.OVERHEAD, SkyHeight.of(90.0))
    }
}
