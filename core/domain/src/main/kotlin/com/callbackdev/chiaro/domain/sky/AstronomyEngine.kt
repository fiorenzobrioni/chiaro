package com.callbackdev.chiaro.domain.sky

import com.callbackdev.chiaro.domain.model.Coordinates
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs

/**
 * What the sky above a place is scheduled to do (Fase 16b): the single source of
 * truth for every solar and lunar instant in the app — `sky.crontab`'s lines, the
 * README's `## Astronomy`, `weather_data.json`'s `astronomical` block and
 * [com.callbackdev.chiaro.domain.model.MoonPhase] all read from here, so the app
 * can never show two different sunrises for one city.
 *
 * **Pure, and free of Android and of the clock**, like [com.callbackdev.chiaro.domain.AlertEngine]
 * and the rule engine: coordinates and a date go in, instants come out, and every
 * function is total — an event that does not happen returns `null` rather than a
 * fabricated time (see [SolarDay] and [LunarDay]).
 *
 * **One primitive, every event.** Rather than a closed-form formula per event, this
 * engine finds the instant at which the body's altitude crosses a threshold
 * ([crossing]). Sunrise is that crossing at −0.833°, civil dawn at −6°, the golden
 * hour between +6° and −0.833°, moonrise at a threshold that varies with the moon's
 * own parallax. That is why the event list can grow without the maths growing, and
 * why polar day, a moonless day and an equatorial ten-minute twilight are all the
 * same code path rather than three special cases.
 *
 * **Accuracy.** Sunrise and sunset land within ~30 s of Open-Meteo's own daily values
 * (there is a test); every other solar event is the same solver at a different angle,
 * and there is a second test asserting the sun really is at the requested altitude at
 * each returned instant. Lunar instants inherit the truncated series of
 * [AstronomyMath] — worth about ten seconds on a moonrise and three minutes on a
 * quarter.
 */
object AstronomyEngine {

    /**
     * Standard refraction-corrected altitude of the sun's UPPER LIMB at the horizon:
     * 34' of refraction plus the sun's 16' semidiameter. Every almanac's sunrise.
     */
    const val SUNRISE_ALTITUDE = -0.833

    const val CIVIL_TWILIGHT = -6.0
    const val NAUTICAL_TWILIGHT = -12.0
    const val ASTRONOMICAL_TWILIGHT = -18.0

    /** Golden hour runs from +6° down to the horizon; blue hour from −4° to −6°. */
    const val GOLDEN_HOUR_ALTITUDE = 6.0
    const val BLUE_HOUR_START = -4.0
    const val BLUE_HOUR_END = -6.0

    // ------------------------------------------------------------------- sun

    /** Every solar instant of one local day at one place. */
    fun solarDay(date: LocalDate, zone: ZoneId, coords: Coordinates): SolarDay {
        val window = DayWindow(date, zone)
        val extremes = extremes(window, coords, Body.Sun)
        fun rise(alt: Double) = crossing(window, coords, alt, rising = true, body = Body.Sun)
        fun set(alt: Double) = crossing(window, coords, alt, rising = false, body = Body.Sun)
        return SolarDay(
            date = date,
            sunrise = rise(SUNRISE_ALTITUDE),
            sunset = set(SUNRISE_ALTITUDE),
            solarNoon = transit(window, coords, Body.Sun),
            civilDawn = rise(CIVIL_TWILIGHT),
            civilDusk = set(CIVIL_TWILIGHT),
            nauticalDawn = rise(NAUTICAL_TWILIGHT),
            nauticalDusk = set(NAUTICAL_TWILIGHT),
            astronomicalDawn = rise(ASTRONOMICAL_TWILIGHT),
            astronomicalDusk = set(ASTRONOMICAL_TWILIGHT),
            goldenHourMorningEnd = rise(GOLDEN_HOUR_ALTITUDE),
            goldenHourEveningStart = set(GOLDEN_HOUR_ALTITUDE),
            blueHourMorningStart = rise(BLUE_HOUR_END),
            blueHourMorningEnd = rise(BLUE_HOUR_START),
            blueHourEveningStart = set(BLUE_HOUR_START),
            blueHourEveningEnd = set(BLUE_HOUR_END),
            // The two polar states, from ONE scan of the day rather than two: the
            // sun is up all day when its lowest sample is still above the horizon,
            // down all day when its highest is still below.
            sunUpAllDay = extremes.min > SUNRISE_ALTITUDE,
            sunDownAllDay = extremes.max < SUNRISE_ALTITUDE
        )
    }

    /**
     * ONE solar crossing of one local day — the same answer [solarDay] would give for
     * that field, without computing the other thirteen (Fase 19).
     *
     * [YearEvents] scans a hundred days looking for the earliest sunset of the year,
     * and a full [SolarDay] per day would be fourteen crossings and a scan of the day
     * thrown away each time.
     */
    fun sunCrossing(
        date: LocalDate,
        zone: ZoneId,
        coords: Coordinates,
        altitudeDeg: Double,
        rising: Boolean
    ): Instant? = crossing(DayWindow(date, zone), coords, altitudeDeg, rising, Body.Sun)

    /**
     * The lowest the sun gets on one local day, degrees. Below −18° somewhere in the
     * night is what makes a night astronomical, so this is the whole test for whether
     * a place is inside its white nights.
     */
    fun sunMinAltitude(date: LocalDate, zone: ZoneId, coords: Coordinates): Double =
        extremes(DayWindow(date, zone), coords, Body.Sun).min

    /** Altitude of the sun above the horizon at [at], degrees, refraction excluded. */
    fun sunAltitude(at: Instant, coords: Coordinates): Double =
        // Position from TT, hour angle from UT — see AstronomyMath's KDoc on why the
        // two arguments are not the same number.
        AstronomyMath.altitude(
            AstronomyMath.sunEquatorial(AstronomyMath.centuriesTT(at)),
            AstronomyMath.julianDay(at),
            coords.lat,
            coords.lon
        )

    /** Compass bearing of the sun at [at], degrees clockwise from north. */
    fun sunAzimuth(at: Instant, coords: Coordinates): Double =
        AstronomyMath.azimuth(
            AstronomyMath.sunEquatorial(AstronomyMath.centuriesTT(at)),
            AstronomyMath.julianDay(at),
            coords.lat,
            coords.lon
        )

    // ------------------------------------------------------------------ moon

    /** Rise, set and phase of the moon over one local day at one place. */
    fun lunarDay(date: LocalDate, zone: ZoneId, coords: Coordinates): LunarDay {
        val window = DayWindow(date, zone)
        val noon = window.start.plus(Duration.between(window.start, window.end).dividedBy(2))
        val illumination = AstronomyMath.moonIllumination(AstronomyMath.centuriesTT(noon))
        return LunarDay(
            date = date,
            // Null here is a fact, not a failure: the moon rises ~50 minutes later
            // each day, so roughly once a month a calendar day contains no moonrise
            // (or no moonset) at all. `sky.crontab` prints `∅ not scheduled` for it.
            moonrise = crossing(window, coords, altitude = null, rising = true, body = Body.Moon),
            moonset = crossing(window, coords, altitude = null, rising = false, body = Body.Moon),
            illuminatedFraction = illumination.fraction,
            elongation = illumination.elongation
        )
    }

    /** Altitude of the moon's centre at [at], degrees, refraction and parallax excluded. */
    fun moonAltitude(at: Instant, coords: Coordinates): Double =
        AstronomyMath.altitude(
            AstronomyMath.moonEquatorial(AstronomyMath.centuriesTT(at)),
            AstronomyMath.julianDay(at),
            coords.lat,
            coords.lon
        )

    /**
     * Distance from the earth's centre to the moon's, kilometres. The number behind
     * "the closest full moon of the year" and behind the size of the shadow in
     * [EclipseEngine].
     */
    fun moonDistanceKm(at: Instant): Double =
        AstronomyMath.moonEcliptic(AstronomyMath.centuriesTT(at)).distanceKm

    /** Illuminated fraction (0..1) and elongation of the moon at [at]. */
    fun moonIllumination(at: Instant): MoonLight {
        val light = AstronomyMath.moonIllumination(AstronomyMath.centuriesTT(at))
        return MoonLight(light.fraction, light.elongation)
    }

    /**
     * The first quarter boundary strictly after [after]: the instant the moon's
     * elongation from the sun reaches 0°, 90°, 180° or 270°.
     *
     * Defined by the same elongation the phase name is read from, rather than by a
     * separate polynomial for phase instants — one model, so the name in the README
     * and the instant in the crontab can never disagree about which day it is.
     */
    fun nextMoonQuarter(after: Instant): MoonQuarter {
        val quarter = quarterAfter(after)
        // STRICTLY after, and the degenerate case is the common one: asked for "the
        // quarter after this quarter" — which is what walking a series of them does —
        // the elongation at `after` sits a hair BELOW its own target, so the search
        // brackets the instant it started from and hands it straight back. Nudging
        // past it costs an hour of a 7.4-day cycle and turns "the next four quarters"
        // from four copies of one instant into four quarters.
        if (Duration.between(after, quarter.at) > MIN_QUARTER_ADVANCE) return quarter
        return quarterAfter(after.plus(MIN_QUARTER_ADVANCE))
    }

    /**
     * The next quarter of a NAMED kind after [after] — the next full moon, the next
     * new moon (Fase 19).
     *
     * Walks the series rather than solving for the kind directly: the elongation
     * targets are 90° apart and [nextMoonQuarter] already steps from one to the next,
     * so at most four steps land on any of them and the two answers are the same
     * model by construction.
     */
    fun nextMoonQuarter(after: Instant, kind: MoonQuarterKind): MoonQuarter {
        var at = after
        repeat(4) {
            val quarter = nextMoonQuarter(at)
            if (quarter.kind == kind) return quarter
            at = quarter.at
        }
        // Unreachable: four consecutive quarters are four different kinds.
        error("no ${'$'}kind within four quarters of ${'$'}after")
    }

    private fun quarterAfter(after: Instant): MoonQuarter {
        val current = moonIllumination(after).elongation
        val nextTarget = (Math.floor(current / 90.0).toInt() + 1) * 90.0
        return MoonQuarter(MoonQuarterKind.of(nextTarget), elongationCrossing(after, nextTarget))
    }

    // --------------------------------------------------------------- seasons

    /**
     * Solstice or equinox of [year].
     *
     * `VISION_SKY.md` planned this as the same root-find [solarLongitudeInstant] uses,
     * on the grounds that one solar model cannot disagree with itself. Measurement
     * overruled the plan: the low-accuracy solar series is good to ~0.01°, the sun
     * covers 0.01° in a quarter of an hour, and root-finding the 2026 March equinox
     * landed **eight minutes** from the published instant — inside the model's own
     * error bar and well outside what an `HH:mm` line may claim. So a season comes
     * from the series fitted to season instants (Meeus 27), which agrees with seven
     * published instants across three decades to within a minute.
     *
     * The two are not left to drift apart unwatched: a test measures the gap between
     * this and the root-find at all four longitudes and pins it, so the day one of
     * them changes, the other's disagreement is a failing test rather than a surprise.
     */
    fun season(year: Int, season: Season): Instant {
        val tt = AstronomyMath.seasonJulianDay(year, season.ordinal)
        return AstronomyMath.instantOf(tt - AstronomyMath.deltaTSeconds(tt) / 86_400.0)
    }

    /**
     * The instant in [year] at which the sun's apparent longitude reaches
     * [longitudeDeg]. Meteor shower peaks are defined this way (§6 of VISION_SKY),
     * which is why the table of showers never expires: a solar longitude is the same
     * point of the earth's orbit every year, whatever date the calendar puts on it.
     *
     * **Good to about a quarter of an hour**, which is the solar series' 0.01° turned
     * into time, and which is why [season] does not use it. That precision is ample
     * here and it is not a compromise: a shower peak is rendered as the NIGHT it falls
     * in, a window some nine hours wide, so an instant a few minutes either way picks
     * the same night. The one case it could not survive — a peak within minutes of
     * dusk or dawn — is bounded by the window itself.
     */
    fun solarLongitudeInstant(year: Int, longitudeDeg: Double): Instant {
        // The sun covers ~0.9856°/day, so the target date is within a couple of days
        // of this estimate — a ±10 day bracket is generous and still cheap.
        val estimate = LocalDate.of(year, 1, 1).atStartOfDay(ZoneId.of("UTC")).toInstant()
            .plus(Duration.ofSeconds((longitudeDeg / 360.0 * 365.2422 * 86_400).toLong()))
            .plus(Duration.ofDays(79))    // longitude 0 is the March equinox, ~day 79
        return bisect(
            low = estimate.minus(Duration.ofDays(10)),
            high = estimate.plus(Duration.ofDays(10))
        ) { at ->
            AstronomyMath.norm180(
                AstronomyMath.sunApparentLongitude(AstronomyMath.centuriesTT(at)) - longitudeDeg
            )
        }
    }

    // ------------------------------------------------- the sky behind the sky

    /**
     * Altitude of the GALACTIC CENTRE above the horizon, degrees (Fase 19).
     *
     * The bright core of the Milky Way, in Sagittarius, is the one deep-sky sight
     * with a season and a clock — and it is a fixed direction, so the same altitude
     * primitive that answers for the sun answers for it. J2000 coordinates, un-precessed:
     * a quarter of a degree by the end of this century, which moves the window it is
     * used for (the core above 10°) by about a minute.
     */
    fun galacticCoreAltitude(at: Instant, coords: Coordinates): Double =
        AstronomyMath.altitude(GALACTIC_CORE, AstronomyMath.julianDay(at), coords.lat, coords.lon)

    /** And which way it stands: the bearing a Milky Way row tells you to face. */
    fun galacticCoreAzimuth(at: Instant, coords: Coordinates): Double =
        AstronomyMath.azimuth(GALACTIC_CORE, AstronomyMath.julianDay(at), coords.lat, coords.lon)

    /**
     * The stretch of `[from, to]` during which the sun is above the horizon: what
     * clips a solar eclipse to the part of it that happens in this place's daylight.
     */
    fun sunAbove(from: Instant, to: Instant, coords: Coordinates): ClosedRange<Instant>? =
        aboveAltitude(from, to, coords, SUNRISE_ALTITUDE, Body.Sun)

    /**
     * The stretch of `[from, to]` during which the moon is above the horizon: what
     * turns a lunar eclipse, which the whole night side of the earth shares, into the
     * part of it a place can actually watch.
     */
    fun moonAbove(from: Instant, to: Instant, coords: Coordinates): ClosedRange<Instant>? =
        aboveAltitude(from, to, coords, 0.0, Body.Moon)

    /**
     * Every stretch of `[from, to]` during which the moon is **below the horizon** —
     * the other half of the dark window, and the one `darkness.window` is an
     * intersection with (Fase 27).
     *
     * A list rather than one range, because a night really can hold two of them: a
     * moon that rises at ten and sets at three leaves the sliver before it and the
     * hours after it, and returning only the first would hand a reader the shorter
     * one about half the time.
     *
     * The threshold is the moon's own rise threshold, parallax and all — the same one
     * [lunarDay] crosses for `moon.rise` and `moon.set`. It has to be: a window that
     * said the moon was down at an hour the row above it calls moonrise would be the
     * screen arguing with itself.
     */
    fun moonDownRuns(from: Instant, to: Instant, coords: Coordinates): List<ClosedRange<Instant>> =
        belowThresholdRuns(from, to, coords, Body.Moon)

    /**
     * The stretch of `[from, to]` during which the galactic core stands at least
     * [minAltitudeDeg] above the horizon, or null when it never does — which is a
     * fact about the latitude at high northern ones, where the core barely clears
     * the horizon at all.
     */
    fun galacticCoreAbove(
        from: Instant,
        to: Instant,
        coords: Coordinates,
        minAltitudeDeg: Double
    ): ClosedRange<Instant>? = aboveAltitude(from, to, coords, minAltitudeDeg, GalacticCore)

    // --------------------------------------------------------------- planets

    /** Altitude of [planet] above the horizon at [at], degrees (Fase 28). */
    fun planetAltitude(planet: Planet, at: Instant, coords: Coordinates): Double =
        AstronomyMath.altitude(
            PlanetMath.equatorial(planet, AstronomyMath.centuriesTT(at)).equatorial,
            AstronomyMath.julianDay(at),
            coords.lat,
            coords.lon
        )

    /** Compass bearing of [planet] at [at], degrees clockwise from north. */
    fun planetAzimuth(planet: Planet, at: Instant, coords: Coordinates): Double =
        AstronomyMath.azimuth(
            PlanetMath.equatorial(planet, AstronomyMath.centuriesTT(at)).equatorial,
            AstronomyMath.julianDay(at),
            coords.lat,
            coords.lon
        )

    /**
     * The stretch of `[from, to]` during which [planet] stands at least
     * [minAltitudeDeg] above the horizon, or null when it never does.
     *
     * The floor is not decoration: a planet at two degrees is behind the houses, and
     * an app that says "Venus is out" about a light nobody can see from a street would
     * be inventing a sight.
     */
    fun planetAbove(
        from: Instant,
        to: Instant,
        coords: Coordinates,
        planet: Planet,
        minAltitudeDeg: Double
    ): ClosedRange<Instant>? = aboveAltitude(from, to, coords, minAltitudeDeg, Body.Wanderer(planet))

    /**
     * How bright [planet] is at [at], as a visual magnitude (Fase 16, step 16.3): **for how
     * its dot is drawn, never for a screen**. Lower is brighter, on the scale the star
     * catalogue's own numbers are on ([BrightStars.mag]), so a planet and a star of one
     * brightness are drawn alike. See [PlanetMath.magnitude] for what it is made of.
     */
    fun planetMagnitude(planet: Planet, at: Instant): Double =
        PlanetMath.magnitude(planet, AstronomyMath.centuriesTT(at))

    /**
     * How far apart two of the naked-eye bodies are at [at], degrees — the number a
     * conjunction IS (Fase 28).
     *
     * Geocentric, and deliberately so for the moon: a topocentric separation is a few
     * tenths of a degree different because the moon is near enough for parallax to
     * matter, and a row that says "the moon passes two degrees from Jupiter" is not a
     * claim anybody checks with a protractor. What it must not do is disagree with
     * itself between two places, and geocentric is the answer that does not.
     */
    fun separation(a: SkyBody, b: SkyBody, at: Instant): Double {
        val t = AstronomyMath.centuriesTT(at)
        return AstronomyMath.separation(equatorialOf(a, t), equatorialOf(b, t))
    }

    /** Altitude of one of the naked-eye bodies, for the code that takes them as a pair. */
    fun bodyAltitude(body: SkyBody, at: Instant, coords: Coordinates): Double =
        body.planet?.let { planetAltitude(it, at, coords) } ?: moonAltitude(at, coords)

    /** Bearing of one of the naked-eye bodies: which way a reader turns to look. */
    fun bodyAzimuth(body: SkyBody, at: Instant, coords: Coordinates): Double =
        body.planet?.let { planetAzimuth(it, at, coords) } ?: AstronomyMath.azimuth(
            AstronomyMath.moonEquatorial(AstronomyMath.centuriesTT(at)),
            AstronomyMath.julianDay(at), coords.lat, coords.lon
        )

    /** A place on the local sky: altitude above the horizon and bearing clockwise from
     * north, both in degrees. */
    data class Horizontal(val altitudeDeg: Double, val azimuthDeg: Double)

    /**
     * Where a star stands at [at] from [coords] (step 6 of the sky review, 19 ott 2026), from
     * its J2000 right ascension and declination in degrees ([BrightStars]). The catalogue's
     * place is taken as it is: precession moves it by a third of a degree in a generation,
     * under what a dp of the canvas holds, and a star has no parallax worth the name.
     */
    fun starHorizontal(raDeg: Double, decDeg: Double, at: Instant, coords: Coordinates): Horizontal {
        val equatorial = AstronomyMath.Equatorial(raDeg, decDeg)
        val julianDay = AstronomyMath.julianDay(at)
        return Horizontal(
            altitudeDeg = AstronomyMath.altitude(equatorial, julianDay, coords.lat, coords.lon),
            azimuthDeg = AstronomyMath.azimuth(equatorial, julianDay, coords.lat, coords.lon)
        )
    }

    /**
     * The angle of the moon's bright limb as seen from [coords] at [at], degrees (step 6 of the
     * sky review, 19 ott 2026): where the sun is from the moon on the sky, measured from the
     * direction of increasing azimuth toward the zenith, in (−180, 180]. 0 is a limb lit from
     * the right for a reader facing the equator in either hemisphere (facing south in the
     * north the azimuth grows toward the west, which is on the right; facing north in the
     * south it grows toward the east, on the right again), 90 a limb lit from above, −90 from
     * below: an evening crescent's bright limb points down toward the sun that has just set,
     * and the «boat» of a spring dusk and the «smile» of the tropics are this number.
     */
    fun moonBrightLimbAngle(at: Instant, coords: Coordinates): Double = brightLimbAngle(
        sunAltitude(at, coords), sunAzimuth(at, coords),
        moonAltitude(at, coords), bodyAzimuth(SkyBody.MOON, at, coords)
    )

    /**
     * The arithmetic of [moonBrightLimbAngle] on two places of the sky: the sun's direction
     * projected on the plane tangent to the sky at the moon, resolved on that plane's axes of
     * growing azimuth and growing altitude.
     */
    fun brightLimbAngle(sunAltDeg: Double, sunAzDeg: Double, moonAltDeg: Double, moonAzDeg: Double): Double {
        fun direction(altDeg: Double, azDeg: Double): DoubleArray {
            val a = Math.toRadians(altDeg)
            val z = Math.toRadians(azDeg)
            // East, north, up.
            return doubleArrayOf(Math.cos(a) * Math.sin(z), Math.cos(a) * Math.cos(z), Math.sin(a))
        }
        val moon = direction(moonAltDeg, moonAzDeg)
        val sun = direction(sunAltDeg, sunAzDeg)
        val am = Math.toRadians(moonAltDeg)
        val zm = Math.toRadians(moonAzDeg)
        val moreAzimuth = doubleArrayOf(Math.cos(zm), -Math.sin(zm), 0.0)
        val moreAltitude = doubleArrayOf(-Math.sin(am) * Math.sin(zm), -Math.sin(am) * Math.cos(zm), Math.cos(am))
        val along = sun[0] * moon[0] + sun[1] * moon[1] + sun[2] * moon[2]
        val d = doubleArrayOf(sun[0] - along * moon[0], sun[1] - along * moon[1], sun[2] - along * moon[2])
        val x = d[0] * moreAzimuth[0] + d[1] * moreAzimuth[1] + d[2] * moreAzimuth[2]
        val y = d[0] * moreAltitude[0] + d[1] * moreAltitude[1] + d[2] * moreAltitude[2]
        return Math.toDegrees(Math.atan2(y, x))
    }

    private fun equatorialOf(body: SkyBody, t: Double): AstronomyMath.Equatorial =
        body.planet?.let { PlanetMath.equatorial(it, t).equatorial } ?: AstronomyMath.moonEquatorial(t)

    // ------------------------------------------- one thing across one night's sky

    /** Where [target] stands at [at] from [coords]: how high, and which way to face. */
    fun horizontal(target: SkyTarget, at: Instant, coords: Coordinates): Horizontal {
        val body = bodyOf(target)
        return Horizontal(altitudeOf(body, at, coords), azimuthOf(body, at, coords))
    }

    /**
     * When [target] rises, stands highest and sets around one night (Fase 16, step 16.3):
     * the three boxes a reader is shown about a thing they pointed at in the sky.
     *
     * **Which passage.** A thing in the sky goes over once a day, and a night is not a day:
     * an evening star rose that morning, a morning star will not set until tomorrow
     * afternoon. So the passage is the one whose **highest point is nearest the middle of
     * [night]**, the one the night sees most of, with the rising before that point and the
     * setting after it, wherever on the clock they fall, daylight included. Tonight's
     * Saturn in Milan (10 Nov 2026) rises at 15:34, in the afternoon, and that is the
     * true answer to «when did it rise».
     *
     * **Rising and setting are the almanac's**: a point of light is on the horizon when
     * refraction has lifted it there ([POINT_RISE_ALTITUDE]), the moon when its upper limb
     * is, at the threshold [lunarDay] crosses. The highest point is where the altitude
     * tops, which for the moon is a few minutes off its crossing of the meridian, because
     * the moon moves north or south while it goes over.
     *
     * **Total, like the rest of this engine.** A thing that does not rise or does not set
     * within half a day of its highest point has a null there: with both null it is up
     * all night and all day ([SkyPassage.alwaysUp]), or, when even its highest point is
     * under the horizon, never ([SkyPassage.neverUp]); the place of the highest point is
     * given either way, because it is computed either way, and a screen that has nothing
     * honest to say about it reads the flag.
     *
     * Within a minute of JPL Horizons on every row of `SkyPassageTest`, the moon's
     * included, and a minute is as fine as Horizons prints. The moon's altitude is the
     * engine's own, from the earth's centre, which is up to a degree over what a place
     * on the surface sees: the altitude every other moon in this app is drawn at.
     */
    fun passage(target: SkyTarget, night: ClosedRange<Instant>, coords: Coordinates): SkyPassage {
        val body = bodyOf(target)
        val middle = night.start.plus(Duration.between(night.start, night.endInclusive).dividedBy(2))
        val top = culmination(body, middle, coords)
        fun offset(at: Instant) = altitudeOf(body, at, coords) - riseThreshold(body, null, at)
        fun point(at: Instant) = SkyPassagePoint(at, altitudeOf(body, at, coords), azimuthOf(body, at, coords))
        if (offset(top) < 0) return SkyPassage(rise = null, highest = point(top), set = null, neverUp = true)

        // Away from the highest point on the grid, to the first cell that holds the horizon.
        fun edge(forward: Boolean): Instant? {
            val step = if (forward) GRID else GRID.negated()
            var near = top
            var far = top.plus(step)
            while (Duration.between(top, far).abs() <= PASSAGE_REACH) {
                if (offset(far) < 0) {
                    return if (forward) bisect(near, far) { -offset(it) } else bisect(far, near) { offset(it) }
                }
                near = far
                far = far.plus(step)
            }
            return null
        }
        return SkyPassage(
            rise = edge(forward = false)?.let(::point),
            highest = point(top),
            set = edge(forward = true)?.let(::point),
            neverUp = false
        )
    }

    /**
     * Every stretch of [dark] during which [target] stands at least [minAltitudeDeg] up, in
     * order (Fase 16, step 16.3): when a thing is worth going out for, which is when it is
     * dark and it has cleared the roofs. A list, because a long night can hold two: a thing
     * that is going down at dusk and is back up before dawn.
     *
     * [dark] is the caller's night, from the dusk to the dawn it means; the floor is the
     * caller's too, because how low is too low depends on how bright the thing is (the
     * same reason [SkySights] has one floor for Venus and another for Jupiter).
     */
    fun watchRuns(
        target: SkyTarget,
        dark: ClosedRange<Instant>,
        coords: Coordinates,
        minAltitudeDeg: Double
    ): List<ClosedRange<Instant>> {
        val body = bodyOf(target)
        return runsAtOrAbove(dark.start, dark.endInclusive) { altitudeOf(body, it, coords) - minAltitudeDeg }
    }

    /**
     * «Until when it is worth it»: the last instant of [dark] at which [target] still stands
     * [minAltitudeDeg] up, or null when it never does that night. The end of [dark] itself
     * when the thing outlasts the night, which a caller reads as «until dawn».
     */
    fun worthUntil(
        target: SkyTarget,
        dark: ClosedRange<Instant>,
        coords: Coordinates,
        minAltitudeDeg: Double
    ): Instant? = watchRuns(target, dark, coords, minAltitudeDeg).lastOrNull()?.endInclusive

    /**
     * How steeply the ecliptic stands out of the horizon, as the altitude of the
     * point of the ecliptic a quarter-turn from the sun (Fase 19).
     *
     * The zodiacal light is dust lying ALONG the ecliptic, so whether it is a cone
     * standing over the sunset or a smear hidden in the horizon haze is this one
     * number. Taking the altitude of a real point rather than the ecliptic's angle
     * with the horizon keeps it in a unit the screen can print: degrees above the
     * horizon, the same unit as everything else here.
     */
    fun eclipticStand(at: Instant, coords: Coordinates, evening: Boolean): Double {
        val t = AstronomyMath.centuriesTT(at)
        val longitude = AstronomyMath.sunApparentLongitude(t) + if (evening) 90.0 else -90.0
        val point = AstronomyMath.eclipticPoint(longitude, AstronomyMath.obliquity(t))
        return AstronomyMath.altitude(point, AstronomyMath.julianDay(at), coords.lat, coords.lon)
    }

    // ------------------------------------------------------------- internals

    /** Sagittarius A*, J2000: the direction of the galactic centre. */
    private val GALACTIC_CORE = AstronomyMath.Equatorial(266.41683, -29.00781)

    /**
     * The almanac's horizon for a point of light: 34' of refraction and no disk. A star or
     * a planet rises when its true altitude is this far under the horizon, because the
     * air has already lifted it into view.
     */
    const val POINT_RISE_ALTITUDE = -0.5667

    /**
     * What the solvers below can follow. A planet carries which one it is, so there is no
     * table from [Planet] to this to fall behind: until Fase 16 there was one, with a
     * branch per planet, and its last line sent every planet that was not Venus to
     * Jupiter.
     */
    private sealed interface Body {
        data object Sun : Body
        data object Moon : Body
        data class Wanderer(val planet: Planet) : Body
        data class Fixed(val equatorial: AstronomyMath.Equatorial) : Body
    }

    private val GalacticCore = Body.Fixed(GALACTIC_CORE)

    private fun bodyOf(target: SkyTarget): Body = when (target) {
        SkyTarget.Moon -> Body.Moon
        is SkyTarget.Wanderer -> Body.Wanderer(target.planet)
        is SkyTarget.Fixed -> Body.Fixed(AstronomyMath.Equatorial(target.raDeg, target.decDeg))
    }

    /** The half-open instant range of one local calendar day. */
    private class DayWindow(date: LocalDate, zone: ZoneId) {
        val start: Instant = date.atStartOfDay(zone).toInstant()

        /**
         * The NEXT day's start, not `start + 24h`: on a DST switch a local day is 23
         * or 25 hours long, and a window that assumed 24 would leave an hour of the
         * day unsearched (or search an hour of the next one).
         */
        val end: Instant = date.plusDays(1).atStartOfDay(zone).toInstant()
    }

    private fun altitudeOf(body: Body, at: Instant, coords: Coordinates): Double =
        when (body) {
            Body.Sun -> sunAltitude(at, coords)
            Body.Moon -> moonAltitude(at, coords)
            is Body.Wanderer -> planetAltitude(body.planet, at, coords)
            is Body.Fixed ->
                AstronomyMath.altitude(body.equatorial, AstronomyMath.julianDay(at), coords.lat, coords.lon)
        }

    private fun azimuthOf(body: Body, at: Instant, coords: Coordinates): Double =
        when (body) {
            Body.Sun -> sunAzimuth(at, coords)
            Body.Moon -> bodyAzimuth(SkyBody.MOON, at, coords)
            is Body.Wanderer -> planetAzimuth(body.planet, at, coords)
            is Body.Fixed ->
                AstronomyMath.azimuth(body.equatorial, AstronomyMath.julianDay(at), coords.lat, coords.lon)
        }

    /**
     * Threshold the body has to cross to count as risen. Fixed for the sun; for the
     * moon it follows its parallax, which swings the horizon by nearly a tenth of a
     * degree over a month (Meeus 15.1: `0.7275·π − 34'`).
     */
    private fun riseThreshold(body: Body, fixed: Double?, at: Instant): Double =
        fixed ?: when (body) {
            Body.Sun -> SUNRISE_ALTITUDE
            // A star has no disk to speak of and no parallax worth the name, and a
            // planet is a point of light at this distance: the same goes for both.
            is Body.Fixed, is Body.Wanderer -> POINT_RISE_ALTITUDE
            Body.Moon -> {
                val parallax = AstronomyMath
                    .moonEcliptic(AstronomyMath.centuries(AstronomyMath.julianDay(at))).parallax
                0.7275 * parallax - 0.5667
            }
        }

    /**
     * The instant [body] stands highest, of the times it does within [PASSAGE_REACH] of
     * [around] the one nearest to it. A thing in the sky tops once a turn of the earth,
     * every 23 h 56 min for a star and up to 25 h 10 min for the moon, so thirteen hours
     * either side always holds one and sometimes two.
     *
     * On the grid first, for the samples higher than both neighbours, then a ternary
     * search inside the two cells around the chosen one, as [transit] does for the sun.
     * At the poles a star's altitude does not change and there is no such sample: the
     * highest sample stands in, so the answer is an instant even there.
     */
    private fun culmination(body: Body, around: Instant, coords: Coordinates): Instant {
        val from = around.minus(PASSAGE_REACH)
        val cells = (PASSAGE_REACH.multipliedBy(2).toMinutes() / GRID.toMinutes()).toInt()
        val altitudes = DoubleArray(cells + 1) { altitudeOf(body, from.plus(GRID.multipliedBy(it.toLong())), coords) }
        var best = -1
        for (i in 1 until cells) {
            if (altitudes[i] < altitudes[i - 1] || altitudes[i] <= altitudes[i + 1]) continue
            if (best < 0 || abs(i - cells / 2) < abs(best - cells / 2)) best = i
        }
        if (best < 0) best = altitudes.indices.maxByOrNull { altitudes[it] }!!
        val peak = from.plus(GRID.multipliedBy(best.toLong()))
        var low = peak.minus(GRID)
        var high = peak.plus(GRID)
        repeat(40) {
            val third = Duration.between(low, high).dividedBy(3)
            val a = low.plus(third)
            val b = high.minus(third)
            if (altitudeOf(body, a, coords) < altitudeOf(body, b, coords)) low = a else high = b
        }
        return low.plus(Duration.between(low, high).dividedBy(2))
    }

    /**
     * The instant inside [window] at which [body] crosses its threshold going up
     * ([rising]) or down. `null` when it does not: polar day, polar night, or a
     * calendar day the moon simply does not rise on.
     *
     * Scans on a coarse grid to bracket the sign change, then bisects to the second.
     * The grid is 10 minutes: the fastest thing here is the moon near the horizon at
     * a low latitude, which moves ~0.25°/min, so a 10-minute cell cannot hide a
     * crossing and an immediate re-crossing.
     *
     * The [rising] flag is folded into the SIGN of the tested function rather than
     * into the search, so bracketing and bisection are one direction-free `negative →
     * non-negative` hunt. A bisection that assumed "increasing" while chasing a
     * setting sun is not slightly wrong: it walks away from the root and returns the
     * edge of the bracket, which reads as a plausible time on a ten-minute boundary.
     */
    private fun crossing(
        window: DayWindow,
        coords: Coordinates,
        altitude: Double?,
        rising: Boolean,
        body: Body
    ): Instant? {
        val sign = if (rising) 1.0 else -1.0
        fun offset(at: Instant) =
            sign * (altitudeOf(body, at, coords) - riseThreshold(body, altitude, at))
        var previousAt = window.start
        var previous = offset(previousAt)
        var at = previousAt.plus(GRID)
        while (at <= window.end) {
            val current = offset(at)
            if (previous < 0 && current >= 0) return bisect(previousAt, at) { offset(it) }
            previousAt = at
            previous = current
            at = at.plus(GRID)
        }
        return null
    }

    /** The instant of the body's highest altitude inside [window] — solar noon. */
    private fun transit(window: DayWindow, coords: Coordinates, body: Body): Instant {
        var best = window.start
        var bestAltitude = Double.NEGATIVE_INFINITY
        var at = window.start
        while (at <= window.end) {
            val altitude = altitudeOf(body, at, coords)
            if (altitude > bestAltitude) {
                bestAltitude = altitude
                best = at
            }
            at = at.plus(GRID)
        }
        // Golden-section-free refinement: the altitude curve is smooth and symmetric
        // around the transit, so ternary search on the bracketing cells converges fast.
        var low = best.minus(GRID)
        var high = best.plus(GRID)
        repeat(40) {
            val third = Duration.between(low, high).dividedBy(3)
            val a = low.plus(third)
            val b = high.minus(third)
            if (altitudeOf(body, a, coords) < altitudeOf(body, b, coords)) low = a else high = b
        }
        return low.plus(Duration.between(low, high).dividedBy(2))
    }

    private class Extremes(val min: Double, val max: Double)

    /** Lowest and highest altitude of [body] over [window], on the same grid. */
    private fun extremes(window: DayWindow, coords: Coordinates, body: Body): Extremes {
        var min = Double.POSITIVE_INFINITY
        var max = Double.NEGATIVE_INFINITY
        var at = window.start
        while (at <= window.end) {
            val altitude = altitudeOf(body, at, coords)
            if (altitude < min) min = altitude
            if (altitude > max) max = altitude
            at = at.plus(GRID)
        }
        return Extremes(min, max)
    }

    /** The instant after [after] at which the moon's elongation reaches [targetDeg]. */
    private fun elongationCrossing(after: Instant, targetDeg: Double): Instant {
        // Elongation gains ~12.19°/day, so a target at most 90° away is at most ~8
        // days out; 12 days of bracket covers the moon's slowest stretch.
        var low = after
        var high = after.plus(Duration.ofDays(12))
        fun offset(at: Instant) =
            AstronomyMath.norm180(moonIllumination(at).elongation - targetDeg)
        // Walk forward in 6-hour steps to the first sign change: `norm180` makes the
        // offset sawtooth once per cycle, and bisecting across that jump would land
        // on the discontinuity instead of on the quarter.
        var previousAt = low
        var previous = offset(previousAt)
        var at = low.plus(Duration.ofHours(6))
        while (at <= high) {
            val current = offset(at)
            if (previous < 0 && current >= 0) {
                low = previousAt
                high = at
                break
            }
            previousAt = at
            previous = current
            at = at.plus(Duration.ofHours(6))
        }
        return bisect(low, high) { offset(it) }
    }

    /**
     * The instant in `[low, high]` where [offset] changes sign from negative to
     * non-negative, to the second. Plain bisection: 40 halvings of a 20-day bracket
     * land well under a millisecond, and it cannot diverge the way Newton can on a
     * curve that flattens out at the poles.
     */
    private fun bisect(low: Instant, high: Instant, offset: (Instant) -> Double): Instant {
        var lo = low
        var hi = high
        repeat(BISECTIONS) {
            val mid = lo.plus(Duration.between(lo, hi).dividedBy(2))
            if (offset(mid) < 0) lo = mid else hi = mid
            if (abs(Duration.between(lo, hi).seconds) <= 1) return mid
        }
        return lo.plus(Duration.between(lo, hi).dividedBy(2))
    }

    /**
     * The sub-interval of `[from, to]` where [body] is at or above [minAltitudeDeg]:
     * the first rise to the following set, each end clipped to the interval when the
     * body is already there. Null when it never gets that high.
     *
     * Written on the same grid-then-bisect pattern as [crossing] rather than through
     * it, because the window here is an arbitrary span — a night that crosses
     * midnight — and not a calendar day.
     */
    private fun aboveAltitude(
        from: Instant,
        to: Instant,
        coords: Coordinates,
        minAltitudeDeg: Double,
        body: Body
    ): ClosedRange<Instant>? =
        runsAtOrAbove(from, to) { altitudeOf(body, it, coords) - minAltitudeDeg }.firstOrNull()

    /**
     * Every sub-interval of `[from, to]` where [offset] is zero or more, in order, each
     * end clipped to the interval when it is already so there: the hunt [aboveAltitude]
     * had for its first answer, kept going to the end of the span (Fase 16, step 16.3).
     */
    private fun runsAtOrAbove(
        from: Instant,
        to: Instant,
        offset: (Instant) -> Double
    ): List<ClosedRange<Instant>> {
        if (!from.isBefore(to)) return emptyList()
        val runs = mutableListOf<ClosedRange<Instant>>()
        var start: Instant? = if (offset(from) >= 0) from else null
        var previousAt = from
        var previous = offset(previousAt)
        var at = from.plus(GRID)
        while (true) {
            val bounded = if (at.isAfter(to)) to else at
            val current = offset(bounded)
            if (start == null && previous < 0 && current >= 0) {
                start = bisect(previousAt, bounded) { offset(it) }
            } else if (start != null && previous >= 0 && current < 0) {
                // Falling edge: the same hunt with the sign flipped.
                runs += start..bisect(previousAt, bounded) { -offset(it) }
                start = null
            }
            if (bounded == to) break
            previousAt = bounded
            previous = current
            at = at.plus(GRID)
        }
        start?.let { runs += it..to }
        return runs
    }

    /**
     * Every sub-interval of `[from, to]` where [body] is under its own rise threshold,
     * in order, each end clipped to the interval when the body is already there.
     *
     * The complement of [aboveAltitude], and a list rather than its first answer: the
     * caller that needs this ([moonDownRuns]) needs the longest run, not the earliest.
     * Same grid-then-bisect pattern, and the threshold is re-read at every sample
     * because the moon's moves with its parallax.
     */
    private fun belowThresholdRuns(
        from: Instant,
        to: Instant,
        coords: Coordinates,
        body: Body
    ): List<ClosedRange<Instant>> {
        if (!from.isBefore(to)) return emptyList()
        // Negative while the body is DOWN, so the edges read the same way round as
        // every other hunt in this file: a sign change from negative is a rise.
        fun offset(at: Instant) = altitudeOf(body, at, coords) - riseThreshold(body, null, at)
        val runs = mutableListOf<ClosedRange<Instant>>()
        var start: Instant? = if (offset(from) < 0) from else null
        var previousAt = from
        var previous = offset(previousAt)
        var at = from.plus(GRID)
        while (true) {
            val bounded = if (at.isAfter(to)) to else at
            val current = offset(bounded)
            if (start != null && previous < 0 && current >= 0) {
                // Rising edge: the run ends where the body crosses back up.
                runs += start!!..bisect(previousAt, bounded) { offset(it) }
                start = null
            } else if (start == null && previous >= 0 && current < 0) {
                start = bisect(previousAt, bounded) { -offset(it) }
            }
            if (bounded == to) break
            previousAt = bounded
            previous = current
            at = at.plus(GRID)
        }
        start?.let { runs += it..to }
        return runs
    }

    private val GRID: Duration = Duration.ofMinutes(10)

    /**
     * How far either side of an instant [culmination] looks for a highest point, and how
     * far either side of that point [passage] looks for the horizon: over half of the
     * longest turn there is, the moon's 25 h 10 min, and over the longest half-arc a thing
     * that rises and sets can have.
     */
    private val PASSAGE_REACH: Duration = Duration.ofHours(13)

    /** How far past a quarter [nextMoonQuarter] jumps before looking for the next. */
    private val MIN_QUARTER_ADVANCE: Duration = Duration.ofHours(1)
    private const val BISECTIONS = 40
}

/**
 * How much of the moon is lit and where it is in its cycle.
 *
 * Its own type rather than [AstronomyMath]'s, which is internal: the arithmetic is
 * an implementation detail of this package, the answer is not.
 */
data class MoonLight(
    /** 0 at new moon, 1 at full. */
    val illuminatedFraction: Double,
    /** Moon longitude − sun longitude, `[0, 360)`: 0 new, 90 first quarter, 180 full. */
    val elongation: Double
)

/**
 * The naked-eye bodies this app will name (Fase 28): the moon, and the planets
 * [PlanetMath] knows, Mars and Saturn among them since Fase 16.
 *
 * Deliberately not "every body the engine can place". The sun is not here because a
 * conjunction with the sun is the one nobody may look at, and Mercury and the two
 * outer planets are not here because [PlanetMath] does not know them and the reason
 * it does not is written there. Which pairs get a row is not decided here either:
 * [SkyScheduler.ConjunctionJobs] names them, and it names three.
 */
enum class SkyBody(
    /** The planet this is, or null for the moon. */
    val planet: Planet?
) {
    MOON(null),
    VENUS(Planet.VENUS),
    JUPITER(Planet.JUPITER),
    MARS(Planet.MARS),
    SATURN(Planet.SATURN);

    /** This body as a thing to follow across a night ([AstronomyEngine.passage]). */
    val target: SkyTarget get() = planet?.let { SkyTarget.Wanderer(it) } ?: SkyTarget.Moon
}

/**
 * A thing the engine can follow across a night's sky (Fase 16, step 16.3): the moon, a
 * planet, or a fixed point, which is a star, a shower's radiant, the heart of a
 * constellation or the galaxy's centre, by its J2000 right ascension and declination in
 * degrees, as [AstronomyEngine.starHorizontal] takes them.
 */
sealed interface SkyTarget {
    data object Moon : SkyTarget
    data class Wanderer(val planet: Planet) : SkyTarget
    data class Fixed(val raDeg: Double, val decDeg: Double) : SkyTarget
}

/**
 * One passage of a thing over a place ([AstronomyEngine.passage]): where and when it
 * comes up, tops and goes down. A null [rise] or [set] is a thing that does not do that
 * within half a day of its highest point, never a failure and never a placeholder time.
 */
data class SkyPassage(
    val rise: SkyPassagePoint?,
    val highest: SkyPassagePoint,
    val set: SkyPassagePoint?,
    /** Even its highest point is under the horizon: there is nothing to see, and
     * [highest] is where it is not seen from. */
    val neverUp: Boolean
) {
    /** Up all night and all day: it circles the pole and never touches the horizon. */
    val alwaysUp: Boolean get() = !neverUp && rise == null && set == null
}

/** An instant of a passage and where the thing stands at it: how high, and which way. */
data class SkyPassagePoint(val at: Instant, val altitudeDeg: Double, val azimuthDeg: Double)

/** Which quarter boundary the moon is heading for. */
enum class MoonQuarterKind(val elongationDeg: Double) {
    NEW_MOON(0.0),
    FIRST_QUARTER(90.0),
    FULL_MOON(180.0),
    LAST_QUARTER(270.0);

    companion object {
        fun of(elongationDeg: Double): MoonQuarterKind {
            val target = AstronomyMath.norm360(elongationDeg)
            return entries.minByOrNull { abs(AstronomyMath.norm180(it.elongationDeg - target)) }!!
        }
    }
}

data class MoonQuarter(val kind: MoonQuarterKind, val at: Instant)

enum class Season(val longitudeDeg: Double) {
    /** March equinox — spring in the north, autumn in the south. */
    MARCH_EQUINOX(0.0),
    JUNE_SOLSTICE(90.0),
    SEPTEMBER_EQUINOX(180.0),
    DECEMBER_SOLSTICE(270.0)
}

/**
 * Every solar instant of one local day. A `null` field is an event that does not
 * happen that day at that latitude, never a failure and never a placeholder time —
 * `sky.crontab` renders it `∅ not scheduled` and says why.
 */
data class SolarDay(
    val date: LocalDate,
    val sunrise: Instant?,
    val sunset: Instant?,
    val solarNoon: Instant,
    val civilDawn: Instant?,
    val civilDusk: Instant?,
    val nauticalDawn: Instant?,
    val nauticalDusk: Instant?,
    val astronomicalDawn: Instant?,
    val astronomicalDusk: Instant?,
    val goldenHourMorningEnd: Instant?,
    val goldenHourEveningStart: Instant?,
    val blueHourMorningStart: Instant?,
    val blueHourMorningEnd: Instant?,
    val blueHourEveningStart: Instant?,
    val blueHourEveningEnd: Instant?,
    val sunUpAllDay: Boolean,
    val sunDownAllDay: Boolean
) {
    /** Morning golden hour: from sunrise up to +6°. */
    val goldenHourMorning: ClosedRange<Instant>?
        get() = range(sunrise, goldenHourMorningEnd)

    /** Evening golden hour: from +6° down to sunset. */
    val goldenHourEvening: ClosedRange<Instant>?
        get() = range(goldenHourEveningStart, sunset)

    val blueHourMorning: ClosedRange<Instant>?
        get() = range(blueHourMorningStart, blueHourMorningEnd)

    val blueHourEvening: ClosedRange<Instant>?
        get() = range(blueHourEveningStart, blueHourEveningEnd)

    /** Astronomical darkness: dusk to the next dawn is the stargazer's window. */
    val darkness: ClosedRange<Instant>?
        get() = range(astronomicalDusk, astronomicalDawn)

    val daylight: Duration?
        get() = if (sunrise != null && sunset != null) Duration.between(sunrise, sunset) else null

    private fun range(from: Instant?, to: Instant?): ClosedRange<Instant>? =
        if (from != null && to != null && !to.isBefore(from)) from..to else null
}

/**
 * The moon over one local day. [moonrise] and [moonset] are null on the days the
 * moon does not do that — about one day a month for each, because it rises roughly
 * 50 minutes later every day and eventually skips a calendar box.
 */
data class LunarDay(
    val date: LocalDate,
    val moonrise: Instant?,
    val moonset: Instant?,
    /** 0 at new moon, 1 at full, measured at local noon. */
    val illuminatedFraction: Double,
    /** Moon longitude − sun longitude, `[0, 360)`: 0 new, 90 first quarter, 180 full. */
    val elongation: Double
)
