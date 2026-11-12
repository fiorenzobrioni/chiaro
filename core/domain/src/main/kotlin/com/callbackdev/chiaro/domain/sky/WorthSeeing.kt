package com.callbackdev.chiaro.domain.sky

import com.callbackdev.chiaro.domain.model.Coordinates
import com.callbackdev.chiaro.domain.model.MoonPhase
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

/**
 * How high a thing stands, as the word a row says it with (Fase 16, step 16.4): the app
 * prints no degrees («a sud, a metà cielo»), so the bands are the vocabulary.
 *
 * Thirds of the way from the horizon to the top of the sky, because there is no finer
 * number a person holds up against a sky without an instrument; and a fourth band for
 * straight up, where a direction stops meaning anything (within ten degrees of the
 * zenith a thing «to the south» and one «to the north» are a tilt of the head apart).
 */
enum class SkyHeight {
    /** Under a third of the way up: over the roofs, in the haze. */
    LOW,

    /** The middle third. */
    HALFWAY,

    /** The top third. */
    HIGH,

    /** Overhead: the row leaves the direction out. */
    OVERHEAD;

    companion object {
        const val HALFWAY_FROM_DEG = 30.0
        const val HIGH_FROM_DEG = 60.0
        const val OVERHEAD_FROM_DEG = 80.0

        fun of(altitudeDeg: Double): SkyHeight = when {
            altitudeDeg >= OVERHEAD_FROM_DEG -> OVERHEAD
            altitudeDeg >= HIGH_FROM_DEG -> HIGH
            altitudeDeg >= HALFWAY_FROM_DEG -> HALFWAY
            else -> LOW
        }
    }
}

/** What a row of the list is about. The words for it are the screen's. */
sealed interface SightSubject {
    /** The moon, with the phase and the lit share the rest of the screen prints for this
     * night ([MoonOfTheNight]), so the list and the hero cannot say two numbers. */
    data class Moon(val phase: MoonPhase, val illuminationPct: Int) : SightSubject

    data class Wanderer(val planet: Planet) : SightSubject

    data class Figure(val figure: SkyFigure) : SightSubject

    /** The bright core of the Milky Way: the catalog's own line ([SkyJobCatalog.MilkyWayCore]). */
    data object MilkyWay : SightSubject

    /** Where a shower's meteors come from, on its peak night. */
    data class Radiant(val shower: MeteorShowerTable.MeteorShower) : SightSubject
}

/**
 * One thing worth looking at on one night: what, when it is best, and where it stands
 * then. Degrees here and words on the screen: [height] is the band, the direction is
 * [azimuthDeg] to the eighth of the compass, as every bearing of the Sky screen already is.
 */
data class Sight(
    val subject: SightSubject,
    /** What a camera turns to, and what [AstronomyEngine.passage] is asked about. */
    val target: SkyTarget,
    /** The instant of [window] at which it stands highest. */
    val best: Instant,
    val altitudeDeg: Double,
    val azimuthDeg: Double,
    /** The stretch around [best] in which it is worth it: dark enough for what it is, and
     * clear of the roofs. Its end is «until when». */
    val window: ClosedRange<Instant>
) {
    val height: SkyHeight get() = SkyHeight.of(altitudeDeg)
}

/**
 * «Da vedere»: for one night and one place, the things worth going out for, each at its
 * best hour, in the order of those hours (Fase 16, step 16.4). Pure, like the rest of the
 * module: a date, a zone and coordinates in, a list out, no clock and no forecast. The
 * clouds are the verdict's business; this is what is up there behind them.
 *
 * **The rules of what enters**, each with its reason, as the weather's states have theirs:
 *
 * 1. **A thing is on the list while the sky is dark enough for what it is and it stands
 *    clear of the roofs**, and its best hour is the instant of that stretch at which it is
 *    highest. Highest, because that is where the air is thinnest and the houses are
 *    lowest; an hour picked any other way would need a reason this app does not have.
 * 2. **How dark is dark enough depends on how bright the thing is.** The moon, Venus and
 *    Jupiter are out as soon as the sun is six degrees down (civil dusk to civil dawn):
 *    each outshines every star, every night. Everything made of stars waits for twelve
 *    (nautical): Mars and Saturn, which are stars among the stars, the figures, a radiant.
 *    The Milky Way waits for eighteen and for the moon to be down.
 * 3. **The moon** at five degrees or more, and lit by one part in a hundred at least:
 *    under that it is inside the sun's glare (the floor earthshine has, [SkySights]).
 * 4. **A planet** at its own floor ([SkySights.minAltitude]: five degrees for Venus, ten
 *    for the others), and **no fainter than magnitude 1.5** ([PLANET_FAINTEST_MAG]). Only
 *    Mars ever is: on the far side of its orbit it is one more star of the Plough's
 *    brightness, and a row that says «Mars» about a point nobody can tell from its
 *    neighbours without a chart sends a reader out to fail. The number chooses, it is
 *    never printed.
 * 5. **A figure** ([SkyFigures]) while **every one of its stars** is ten degrees up: a
 *    shape with its feet behind the houses is not the shape. That one rule is why the
 *    Scorpion is on Sydney's list and never on Milan's, where its sting tops at seven.
 *    **A cluster** at twenty: it is faint stars close together, and lower than that the
 *    haze takes them first.
 * 6. **The Milky Way** when the catalog's own line has a window that night: the core ten
 *    degrees up in the moonless dark ([SkyScheduler]). The same line, so the list and a
 *    reader's «Milky Way» moment cannot disagree; and the core only, because the rest of
 *    the band is the faint half, the app does not know how dark the reader's sky is, and
 *    the explorer draws the band for whoever wants to look for it.
 * 7. **A radiant** on its shower's peak night only (the table's place for it holds on
 *    that night, [MeteorShowerTable]), ten degrees up inside the shower's own window. A
 *    strong shower always, a weak one only for a reader who follows it: the rule the
 *    window already has ([followed], the job ids).
 * 8. **A stretch under a quarter of an hour is not a row.** The floors are lines, and a
 *    thing can touch one for a second: on 15 April 2027 from Milan the Pleiades are at
 *    twenty degrees and sinking as the stars come out, one second above their floor, and
 *    a row for that is a row about the threshold. A quarter of an hour is the least the
 *    rest of the app plans in (the floor of a reminder, the forecast's own step), and
 *    less than it takes to go out and let the eyes settle. The dark window has no such
 *    floor and keeps none: that is a fact about the night, this is an invitation.
 *
 * **What is not here**: single stars (the explorer names them), anything that wants
 * binoculars, satellites, Mercury (the phase's decisions). And no rule about the moon
 * washing the others out: a figure of second-magnitude stars stands a full moon, and the
 * one sight that does not, the Milky Way, already waits for the moon to set.
 *
 * Where the night has no such darkness the rows that need it are simply absent: in
 * London in June there are the figures and no Milky Way, in Stockholm the moon and the
 * two bright planets at most, in the hour the sun is six degrees down, and under the
 * midnight sun the list is empty. Where the sun never comes back up to a threshold (the
 * polar night) the stretch runs from one noon to the next.
 */
object WorthSeeing {

    /** Under this a planet is one of the crowd. Castor, the faintest star the app names, is 1.58. */
    const val PLANET_FAINTEST_MAG = 1.5

    const val MOON_MIN_ALTITUDE = 5.0
    const val FIGURE_MIN_ALTITUDE = 10.0
    const val CLUSTER_MIN_ALTITUDE = 20.0
    const val RADIANT_MIN_ALTITUDE = 10.0

    /** One part in a hundred of the disc: the thinnest crescent that is out of the glare. */
    const val MOON_MIN_LIT = 0.01

    /** Rule 8: shorter than this, a stretch is the threshold talking. */
    val MIN_WINDOW: Duration = Duration.ofMinutes(15)

    /** The two that outshine every star on every night (step 16.3, decision 5). */
    private val Brilliant = setOf(Planet.VENUS, Planet.JUPITER)

    /**
     * The list for the night that begins on the evening of [date] at [coords], soonest
     * best hour first. [followed] is the job ids of the moments the reader follows, for
     * the weak showers.
     */
    fun of(
        date: LocalDate,
        zone: ZoneId,
        coords: Coordinates,
        followed: Set<String> = emptySet()
    ): List<Sight> {
        val bright = span(date, zone, coords, AstronomyEngine.CIVIL_TWILIGHT, { it.civilDusk }, { it.civilDawn })
        val starry = span(date, zone, coords, AstronomyEngine.NAUTICAL_TWILIGHT, { it.nauticalDusk }, { it.nauticalDawn })

        val sights = buildList {
            bright?.let { moon(date, zone, coords, it) }?.let(::add)
            Planet.entries.forEach { planet ->
                val dark = if (planet in Brilliant) bright else starry
                (planetNight(planet, dark, coords) as? PlanetNight.Up)?.let { add(it.sight) }
            }
            if (starry != null) SkyFigures.all.forEach { figure(it, starry, coords)?.let(::add) }
            milkyWay(date, zone, coords)?.let(::add)
            addAll(radiants(date, zone, coords, followed))
        }
        // Built in the order a reader would name them, and a stable sort keeps that order
        // between two things that are best at the same instant (two that are highest when
        // the dawn takes them).
        return sights.sortedBy { it.best }
    }

    private fun moon(date: LocalDate, zone: ZoneId, coords: Coordinates, dark: ClosedRange<Instant>): Sight? {
        val tonight = MoonOfTheNight.instant(date, zone, coords)
        val light = AstronomyEngine.moonIllumination(tonight)
        if (light.illuminatedFraction < MOON_MIN_LIT) return null
        val subject = SightSubject.Moon(MoonPhase.at(tonight), (light.illuminatedFraction * 100).roundToInt())
        return sight(
            subject, SkyTarget.Moon,
            AstronomyEngine.watchRuns(SkyTarget.Moon, dark, coords, MOON_MIN_ALTITUDE), dark, coords
        )
    }

    /**
     * One planet on one night: its row, or which of the rules kept it off the list (Fase
     * 16, step 16.8). The catalog's lines for Mars and Saturn read this, so the reason a
     * line has no window tonight is the reason the list has no row.
     */
    sealed interface PlanetNight {
        data class Up(val sight: Sight) : PlanetNight

        /** Never at its floor for a quarter of an hour of the dark (rules 4 and 8). */
        data object TooLow : PlanetNight

        /** At its floor, and fainter than [PLANET_FAINTEST_MAG] (rule 4). */
        data object TooFaint : PlanetNight

        /** The sky does not get dark enough for it on this date (rule 2). */
        data object NoNight : PlanetNight
    }

    /** [planet] on the night that begins on the evening of [date]. */
    fun planetNight(planet: Planet, date: LocalDate, zone: ZoneId, coords: Coordinates): PlanetNight {
        val dark = if (planet in Brilliant) {
            span(date, zone, coords, AstronomyEngine.CIVIL_TWILIGHT, { it.civilDusk }, { it.civilDawn })
        } else {
            span(date, zone, coords, AstronomyEngine.NAUTICAL_TWILIGHT, { it.nauticalDusk }, { it.nauticalDawn })
        }
        return planetNight(planet, dark, coords)
    }

    private fun planetNight(planet: Planet, dark: ClosedRange<Instant>?, coords: Coordinates): PlanetNight {
        if (dark == null) return PlanetNight.NoNight
        val target = SkyTarget.Wanderer(planet)
        val runs = AstronomyEngine.watchRuns(target, dark, coords, SkySights.minAltitude(planet))
        val sight = sight(SightSubject.Wanderer(planet), target, runs, dark, coords) ?: return PlanetNight.TooLow
        return if (AstronomyEngine.planetMagnitude(planet, sight.best) <= PLANET_FAINTEST_MAG) {
            PlanetNight.Up(sight)
        } else {
            PlanetNight.TooFaint
        }
    }

    private fun figure(figure: SkyFigure, dark: ClosedRange<Instant>, coords: Coordinates): Sight? {
        val floor = if (figure.kind == SkyFigureKind.CLUSTER) CLUSTER_MIN_ALTITUDE else FIGURE_MIN_ALTITUDE
        val whole = figure.stars
            .map { AstronomyEngine.watchRuns(it, dark, coords, floor) }
            .reduce(::overlap)
        return sight(SightSubject.Figure(figure), figure.centre, whole, dark, coords)
    }

    private fun milkyWay(date: LocalDate, zone: ZoneId, coords: Coordinates): Sight? {
        val core = SkyScheduler.resolve(SkyJobCatalog.MilkyWayCore, date, zone, coords) as? SkyOccurrence.At
        val window = core?.end?.let { core.start..it } ?: return null
        return sight(SightSubject.MilkyWay, AstronomyEngine.galacticCore, listOf(window), window, coords)
    }

    private fun radiants(
        date: LocalDate,
        zone: ZoneId,
        coords: Coordinates,
        followed: Set<String>
    ): List<Sight> {
        val night = SkyScheduler.darkNight(date, zone, coords).night as? SkyOccurrence.At ?: return emptyList()
        val dawn = night.end ?: return emptyList()
        return SkyJobCatalog.meteorShowers.mapNotNull { job ->
            val shower = MeteorShowerTable.showerOf(job.id) ?: return@mapNotNull null
            if (!shower.strong && job.id !in followed) return@mapNotNull null
            // The shower's own night, as its line of the catalog resolves it: the list and
            // a reader's reminder for the peak are then about the same hours. A shower's
            // «year» starts at the March equinox, where the sun's longitude is zero: the
            // Quadrantids of 2026 peak in January 2027. So the year before is asked too,
            // as [SkyScheduler.next] does.
            val peak = listOf(date.minusYears(1), date)
                .map { SkyScheduler.resolve(job, it, zone, coords) }
                .filterIsInstance<SkyOccurrence.At>()
                .firstOrNull { at -> at.end != null && at.start.isBefore(dawn) && at.end.isAfter(night.start) }
                ?: return@mapNotNull null
            val window = maxOf(peak.start, night.start)..minOf(peak.end!!, dawn)
            val target = SkyTarget.Fixed(shower.radiantRaDeg, shower.radiantDecDeg)
            sight(
                SightSubject.Radiant(shower), target,
                AstronomyEngine.watchRuns(target, window, coords, RADIANT_MIN_ALTITUDE), window, coords
            )
        }
    }

    /**
     * The row for [target] over [stretches], the parts of [dark] it is worth it in: null
     * with none long enough. The best instant is where it stands highest, and on an arc
     * that has one top that is the top itself when a stretch holds it, and otherwise an
     * end of a stretch, so those are the only instants weighed.
     */
    private fun sight(
        subject: SightSubject,
        target: SkyTarget,
        stretches: List<ClosedRange<Instant>>,
        dark: ClosedRange<Instant>,
        coords: Coordinates
    ): Sight? {
        val runs = stretches.filter { Duration.between(it.start, it.endInclusive) >= MIN_WINDOW }
        if (runs.isEmpty()) return null
        val top = AstronomyEngine.passage(target, dark, coords).highest.at
        val candidates = runs.flatMap { listOf(it.start, it.endInclusive) } +
            listOfNotNull(top.takeIf { at -> runs.any { at in it } })
        val best = candidates.maxBy { AstronomyEngine.horizontal(target, it, coords).altitudeDeg }
        val place = AstronomyEngine.horizontal(target, best, coords)
        return Sight(subject, target, best, place.altitudeDeg, place.azimuthDeg, runs.first { best in it })
    }

    /** The stretches two lists of stretches share, in order: when both of two stars are up. */
    private fun overlap(
        a: List<ClosedRange<Instant>>,
        b: List<ClosedRange<Instant>>
    ): List<ClosedRange<Instant>> = a.flatMap { first ->
        b.mapNotNull { second ->
            val start = maxOf(first.start, second.start)
            val end = minOf(first.endInclusive, second.endInclusive)
            if (start.isBefore(end)) start..end else null
        }
    }

    /**
     * From the evening of [date] the sun goes under [altitudeDeg] to the morning it comes
     * back over it; null when it does not get that far down. A day on which it never
     * comes back up to it has no such evening and no such morning, and the stretch runs
     * from that noon, or to the next one: the night that is the whole day.
     */
    private fun span(
        date: LocalDate,
        zone: ZoneId,
        coords: Coordinates,
        altitudeDeg: Double,
        dusk: (SolarDay) -> Instant?,
        dawn: (SolarDay) -> Instant?
    ): ClosedRange<Instant>? {
        val today = SkyAlmanac.solarDay(date, zone, coords)
        val tomorrow = SkyAlmanac.solarDay(date.plusDays(1), zone, coords)
        fun darkAtNoon(day: SolarDay) = AstronomyEngine.sunAltitude(day.solarNoon, coords) < altitudeDeg
        val start = dusk(today) ?: today.solarNoon.takeIf { darkAtNoon(today) } ?: return null
        val end = dawn(tomorrow) ?: tomorrow.solarNoon.takeIf { darkAtNoon(tomorrow) } ?: return null
        return if (start.isBefore(end)) start..end else null
    }
}
