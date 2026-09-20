package com.callbackdev.chiaro.domain.sky

import com.callbackdev.chiaro.domain.model.Coordinates
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

/** One close approach of two naked-eye bodies, and the number that makes it one. */
data class Conjunction(
    val a: SkyBody,
    val b: SkyBody,
    /** The instant they are closest. */
    val at: Instant,
    /** How close they get, degrees — the whole content of the event. */
    val separationDeg: Double,
    /**
     * When the pair can actually be looked at from here: the stretch of the night or
     * twilight around [at] with both of them up. Null is not a possible value — a
     * conjunction with no such window is not returned at all.
     */
    val window: ClosedRange<Instant>
) {
    /** Degrees, rounded the way the row prints it: nobody reads a conjunction in arcminutes. */
    val separationRounded: Int get() = separationDeg.roundToInt()
}

/**
 * The sights of Fase 28: the events that are not an angle the sun crosses.
 *
 * Its own file for the reason [YearEvents] has one — these are **searches and
 * intersections**, where [AstronomyEngine] under them answers about one instant, and
 * [SkyScheduler] above them is a dispatcher that should stay one. Same rules as the
 * rest of the module: pure, no clock, no Android, and a `null` is a fact about the sky
 * rather than a failure.
 *
 * What they have in common is that each one is a thing a person can be *told to go and
 * look at*, in a sentence with a direction in it — which is the half of this app the
 * ephemeris sites do not write.
 */
object SkySights {

    // --------------------------------------------------------- the full moon at dusk

    /**
     * The evening the full moon comes up while the sky is still coloured.
     *
     * Once a month the moon rises within a few minutes of sunset, and for that half
     * hour it is a huge orange disc against a sky that is still blue — the one lunar
     * event that is genuinely worth carrying a camera for, and the one nobody is told
     * about because an almanac prints "full moon 03:14" and leaves it there.
     *
     * The condition is the intersection: **nearly full** (the disc has to read as
     * round) AND **rising inside the twilight**, which is a window rather than an
     * instant because the moon's rise slips about fifty minutes a day and the evening
     * either side of the full moon is often the better one.
     */
    fun nextFullMoonAtDusk(
        from: LocalDate,
        zone: ZoneId,
        coords: Coordinates,
        within: Int = FULL_MOON_SEARCH_DAYS
    ): Instant? {
        var date = from
        repeat(within) {
            val moonrise = SkyAlmanac.lunarDay(date, zone, coords).moonrise
            val sunset = SkyAlmanac.solarDay(date, zone, coords).sunset
            val civilDusk = SkyAlmanac.solarDay(date, zone, coords).civilDusk
            if (moonrise != null && sunset != null && civilDusk != null) {
                val illumination =
                    AstronomyEngine.moonIllumination(moonrise).illuminatedFraction
                val opens = sunset.minus(DUSK_SLACK)
                val closes = civilDusk.plus(DUSK_SLACK)
                if (illumination >= FULL_ENOUGH &&
                    !moonrise.isBefore(opens) && !moonrise.isAfter(closes)
                ) {
                    return moonrise
                }
            }
            date = date.plusDays(1)
        }
        return null
    }

    // ------------------------------------------------------------------ earthshine

    /**
     * The window for earthshine: the thin crescent with the rest of the disc faintly
     * lit — by the earth, which is why it has the name it has.
     *
     * Visible for a few evenings after new moon low in the west, and a few mornings
     * before it low in the east. The crescent has to be **thin**, because the lit limb
     * is what drowns it: past about a fifth of the disc the glare wins and the ashen
     * light is gone.
     *
     * The window is bounded by twilight at one end and by the moon at the other, and
     * the order of the two is the whole difference between the evening and the morning
     * case.
     */
    fun earthshine(
        date: LocalDate,
        zone: ZoneId,
        coords: Coordinates,
        evening: Boolean
    ): SightResult {
        val day = SkyAlmanac.solarDay(date, zone, coords)
        val lunar = SkyAlmanac.lunarDay(date, zone, coords)
        val edge = (if (evening) day.sunset else day.sunrise)
            ?: return SightResult.None(polarReasonOf(day))
        val dark = (if (evening) day.nauticalDusk else day.nauticalDawn)
            ?: return SightResult.None(SkyNotScheduled.NO_DARKNESS)

        val illumination = AstronomyEngine.moonIllumination(edge).illuminatedFraction
        if (illumination > EARTHSHINE_MAX || illumination < EARTHSHINE_MIN) {
            return SightResult.None(SkyNotScheduled.MOON_NOT_CRESCENT)
        }

        // Evening: from sunset until the moon goes down or the twilight does, whichever
        // comes first. Morning: the mirror, and the moon has to be up already.
        val window = if (evening) {
            val ends = listOfNotNull(lunar.moonset, dark).minOrNull()!!
            if (ends.isAfter(edge)) edge..ends else null
        } else {
            val opens = listOfNotNull(lunar.moonrise, dark).maxOrNull()!!
            if (edge.isAfter(opens)) opens..edge else null
        } ?: return SightResult.None(SkyNotScheduled.MOON_ABSENT)

        return SightResult.At(window.start, window.endInclusive)
    }

    // --------------------------------------------------------------- the two planets

    /**
     * When [planet] is up and high enough to be the bright thing somebody points at.
     *
     * [evening] picks the side of the day: after sunset for an evening apparition,
     * before sunrise for a morning one. Jupiter, which can be up all night, is asked
     * for with [wholeNight] instead.
     *
     * The floor on altitude is what makes this a sight rather than an ephemeris line.
     * Venus at three degrees is behind the houses however bright it is, and telling
     * somebody in a street that Venus is out would be inventing one.
     */
    fun planetWindow(
        planet: Planet,
        date: LocalDate,
        zone: ZoneId,
        coords: Coordinates,
        evening: Boolean,
        wholeNight: Boolean = false
    ): SightResult {
        val day = SkyAlmanac.solarDay(date, zone, coords)
        val minAltitude = if (planet == Planet.VENUS) VENUS_MIN_ALTITUDE else JUPITER_MIN_ALTITUDE
        val span = when {
            wholeNight -> {
                val dusk = day.civilDusk ?: return SightResult.None(polarReasonOf(day))
                val dawn = SkyAlmanac.solarDay(date.plusDays(1), zone, coords).civilDawn
                    ?: return SightResult.None(polarReasonOf(day))
                dusk..dawn
            }
            evening -> {
                val sunset = day.sunset ?: return SightResult.None(polarReasonOf(day))
                sunset..sunset.plus(APPARITION_SPAN)
            }
            else -> {
                val sunrise = day.sunrise ?: return SightResult.None(polarReasonOf(day))
                sunrise.minus(APPARITION_SPAN)..sunrise
            }
        }
        val window = AstronomyEngine
            .planetAbove(span.start, span.endInclusive, coords, planet, minAltitude)
            ?: return SightResult.None(SkyNotScheduled.PLANET_TOO_LOW)
        return SightResult.At(window.start, window.endInclusive)
    }

    // -------------------------------------------------------------- conjunctions

    /**
     * The next time [a] and [b] pass close enough to read as a pair, **and can be seen
     * doing it from here**.
     *
     * The second half is not a nicety. The moon passes Venus every month and about
     * half of those happen with both of them behind the sun, where the event is real,
     * computable and invisible — and a row announcing one would be the screen sending
     * somebody outside to look at daylight. So a candidate is only returned once there
     * is a stretch of the surrounding night or twilight with both bodies genuinely up.
     *
     * Walks on a coarse step to a local minimum of the separation, then refines. The
     * step is six hours because the fastest thing here is the moon at 13° a day, so a
     * six-hour cell moves it about three degrees and cannot hide a close approach.
     */
    fun nextConjunction(
        a: SkyBody,
        b: SkyBody,
        from: Instant,
        zone: ZoneId,
        coords: Coordinates,
        within: Duration = CONJUNCTION_HORIZON
    ): Conjunction? {
        val limit = from.plus(within)
        var at = from
        var previous = AstronomyEngine.separation(a, b, at)
        var falling = false
        while (at.isBefore(limit)) {
            val next = at.plus(CONJUNCTION_STEP)
            val current = AstronomyEngine.separation(a, b, next)
            if (current < previous) {
                falling = true
            } else if (falling) {
                falling = false
                val closest = refine(a, b, at.minus(CONJUNCTION_STEP), next)
                val separation = AstronomyEngine.separation(a, b, closest)
                if (separation <= CONJUNCTION_MAX_DEG && closest.isAfter(from)) {
                    visibleWindow(a, b, closest, zone, coords)?.let { window ->
                        return Conjunction(a, b, closest, separation, window)
                    }
                }
            }
            previous = current
            at = next
        }
        return null
    }

    /** Ternary search on the separation curve, which is smooth and has one minimum here. */
    private fun refine(a: SkyBody, b: SkyBody, low: Instant, high: Instant): Instant {
        var lo = low
        var hi = high
        repeat(REFINE_STEPS) {
            val third = Duration.between(lo, hi).dividedBy(3)
            val x = lo.plus(third)
            val y = hi.minus(third)
            if (AstronomyEngine.separation(a, b, x) < AstronomyEngine.separation(a, b, y)) {
                hi = y
            } else {
                lo = x
            }
        }
        return lo.plus(Duration.between(lo, hi).dividedBy(2))
    }

    /**
     * The stretch of the night around [at] with both bodies up and the sun down — what
     * turns a geometric close approach into something a person can be sent out to see.
     *
     * Scans the local day of [at] and the one after it, because a conjunction that is
     * closest at noon is watched that evening.
     */
    private fun visibleWindow(
        a: SkyBody,
        b: SkyBody,
        at: Instant,
        zone: ZoneId,
        coords: Coordinates
    ): ClosedRange<Instant>? {
        val date = at.atZone(zone).toLocalDate()
        listOf(date.minusDays(1), date).forEach { day ->
            val evening = SkyAlmanac.solarDay(day, zone, coords).sunset ?: return@forEach
            val morning = SkyAlmanac.solarDay(day.plusDays(1), zone, coords).sunrise
                ?: return@forEach
            var start: Instant? = null
            var end: Instant? = null
            var cursor = evening
            while (!cursor.isAfter(morning)) {
                val up = AstronomyEngine.bodyAltitude(a, cursor, coords) >= CONJUNCTION_MIN_ALTITUDE &&
                    AstronomyEngine.bodyAltitude(b, cursor, coords) >= CONJUNCTION_MIN_ALTITUDE
                if (up) {
                    if (start == null) start = cursor
                    end = cursor
                }
                cursor = cursor.plus(VISIBILITY_STEP)
            }
            if (start != null && end != null && end!!.isAfter(start!!)) return start!!..end!!
        }
        return null
    }

    // ------------------------------------------------------------------ bearings

    /**
     * Which way to turn for [job] at [at], degrees clockwise from north — or null when
     * the row has no direction worth printing (Fase 28).
     *
     * **Null is the common answer and that is the point.** A bearing on every row would
     * be the app filling a line because it can: an equinox does not happen in a
     * direction, and "the sun rises towards the east" is a sentence that teaches
     * nobody anything. It is here for the rows where a person has to choose a window
     * to stand at or a horizon to clear — the photographer's hours, the two planets,
     * the pairs, and the sights this app added because nobody else prints them.
     *
     * It is also the honest half of [SkyJob.photographic]: the flag says bring a
     * camera, and without this the reader would still not know where to point it.
     */
    fun bearing(job: SkyJob, at: Instant, coords: Coordinates): Double? = when (job.id) {
        SkyJobCatalog.GoldenAm.id, SkyJobCatalog.GoldenPm.id,
        SkyJobCatalog.BlueAm.id, SkyJobCatalog.BluePm.id ->
            AstronomyEngine.sunAzimuth(at, coords)

        SkyJobCatalog.MoonFullAtDusk.id, SkyJobCatalog.EarthshinePm.id,
        SkyJobCatalog.EarthshineAm.id, SkyJobCatalog.LunarEclipse.id ->
            AstronomyEngine.bodyAzimuth(SkyBody.MOON, at, coords)

        SkyJobCatalog.MilkyWayCore.id -> AstronomyEngine.galacticCoreAzimuth(at, coords)

        SkyJobCatalog.VenusEvening.id, SkyJobCatalog.VenusMorning.id ->
            AstronomyEngine.bodyAzimuth(SkyBody.VENUS, at, coords)

        SkyJobCatalog.JupiterNight.id ->
            AstronomyEngine.bodyAzimuth(SkyBody.JUPITER, at, coords)

        // A pair is in one place, so either of the two answers; the first is the one
        // the row is named after.
        in SkyScheduler.ConjunctionJobs.keys ->
            AstronomyEngine.bodyAzimuth(SkyScheduler.ConjunctionJobs.getValue(job.id).first, at, coords)

        else -> null
    }

    // ------------------------------------------------------------------ internals

    /** What a sight resolved to: a window, or the reason the sky is not offering one. */
    sealed interface SightResult {
        data class At(val start: Instant, val end: Instant) : SightResult
        data class None(val reason: SkyNotScheduled) : SightResult
    }

    private fun polarReasonOf(day: SolarDay): SkyNotScheduled = when {
        day.sunUpAllDay -> SkyNotScheduled.POLAR_DAY
        day.sunDownAllDay -> SkyNotScheduled.POLAR_NIGHT
        else -> SkyNotScheduled.NO_DARKNESS
    }

    /** How round the disc has to be before "full moon" is an honest thing to call it. */
    private const val FULL_ENOUGH = 0.96

    /** How far either side of the twilight the moonrise may fall and still count. */
    private val DUSK_SLACK: Duration = Duration.ofMinutes(45)

    /** A moonrise inside the twilight comes round once a month; two are never needed. */
    private const val FULL_MOON_SEARCH_DAYS = 45

    /**
     * The crescent earthshine lives on. Below 1 % the moon is not out of the sun's
     * glare at all; past 18 % the lit limb drowns the ashen light, which is the whole
     * sight.
     */
    private const val EARTHSHINE_MIN = 0.01
    private const val EARTHSHINE_MAX = 0.18

    /**
     * How high a planet has to stand. Venus is the third-brightest thing in the sky and
     * survives a low, bright twilight; Jupiter wants a darker, higher sky to be the
     * obvious one.
     */
    private const val VENUS_MIN_ALTITUDE = 5.0
    private const val JUPITER_MIN_ALTITUDE = 10.0

    /** How far past sunset (or before sunrise) an apparition is worth looking for. */
    private val APPARITION_SPAN: Duration = Duration.ofHours(4)

    /** Close enough to read as a pair rather than as two things in the same sky. */
    private const val CONJUNCTION_MAX_DEG = 5.0

    /** And high enough that "next to each other" is something a street can see. */
    private const val CONJUNCTION_MIN_ALTITUDE = 5.0

    private val CONJUNCTION_STEP: Duration = Duration.ofHours(6)
    private val VISIBILITY_STEP: Duration = Duration.ofMinutes(15)

    /**
     * How far ahead to look. The moon meets each planet about monthly, but only some of
     * those are visible from a given place, so the horizon has to cover a planet's
     * whole turn behind the sun; Venus and Jupiter meet about every thirteen months and
     * can be behind the sun when they do.
     */
    private val CONJUNCTION_HORIZON: Duration = Duration.ofDays(3 * 366L)

    private const val REFINE_STEPS = 30
}
