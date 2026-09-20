package com.callbackdev.chiaro.domain.sky

/**
 * The fixed set of jobs `sky.crontab` can carry (Fase 16b) — versioned in code, with
 * no runtime registration, exactly as the 22 variables of `alerts.rules` are. A user
 * picks lines from this list; a user cannot write one. That is the same discipline
 * that makes a syntax error unwritable in the rules file: the states the app has to
 * handle are the states the app defines.
 *
 * The order here is the order the FILE renders in ([all]) — a crontab is a file, not
 * a queue, so it does not re-sort itself by whatever fires next. The next job to fire
 * is called out in the header instead.
 */
object SkyJobCatalog {

    // Sun ---------------------------------------------------------------------
    val SunRise = SkyJob("sun.rise", SkyJobKind.DAILY, SkyJobShape.INSTANT)
    val SunSet = SkyJob("sun.set", SkyJobKind.DAILY, SkyJobShape.INSTANT)
    // A moment of geometry, not a sight: the sun is at its highest whether or not
    // anybody could tell by looking.
    val SolarNoon = SkyJob("solar.noon", SkyJobKind.DAILY, SkyJobShape.INSTANT, observable = false)

    // Twilight ----------------------------------------------------------------
    val CivilAm = twilight("twilight.civil.am")
    val CivilPm = twilight("twilight.civil.pm")
    val NauticalAm = twilight("twilight.nautical.am")
    val NauticalPm = twilight("twilight.nautical.pm")
    val AstronomicalAm = twilight("twilight.astronomical.am")
    val AstronomicalPm = twilight("twilight.astronomical.pm")

    // The photographer's hours ------------------------------------------------
    // These four are in the catalog BECAUSE of photography and have been since Fase 5;
    // `photographic` (Fase 28) only says out loud what the group's own name says.
    val GoldenAm = visibleRange("golden_hour.am", photographic = true)
    val GoldenPm = visibleRange("golden_hour.pm", photographic = true)
    val BlueAm = visibleRange("blue_hour.am", photographic = true)
    val BluePm = visibleRange("blue_hour.pm", photographic = true)

    /**
     * Astronomical dusk → dawn, with the moonless part of it named in the comment.
     *
     * The one derived line in the catalog, and the reason the module is worth
     * building: every other job is an hour at which the sun or the moon crosses an
     * angle, which any ephemeris site will tell you. This is the INTERSECTION — when
     * it is genuinely dark AND the moon is down — which is the thing an amateur
     * astronomer actually plans around and which no weather app prints.
     */
    val DarknessWindow = SkyJob(
        "darkness.window", SkyJobKind.DAILY, SkyJobShape.RANGE,
        visibilityDependent = true, needsDarkness = true
    )

    /**
     * The dark-sky pair the darkness window was the first of (Fase 19): what there is
     * to look AT once it is dark, rather than when the dark starts.
     *
     * The core of the Milky Way is above the horizon for part of the night from about
     * March to October at mid-northern latitudes and never at all far enough north;
     * the zodiacal light is dust along the ecliptic, so it stands up out of the
     * horizon only in the weeks when the ecliptic itself does. Both are intersections
     * — dark sky AND something in it — which is the same reason [DarknessWindow]
     * earns its place, one step further out.
     */
    val MilkyWayCore = SkyJob(
        "milky_way.core", SkyJobKind.DAILY, SkyJobShape.RANGE,
        visibilityDependent = true, needsDarkness = true, photographic = true
    )
    val ZodiacalAm = darkSky("zodiacal.am")
    val ZodiacalPm = darkSky("zodiacal.pm")

    // Moon --------------------------------------------------------------------
    val MoonRise = SkyJob("moon.rise", SkyJobKind.DAILY, SkyJobShape.INSTANT)
    val MoonSet = SkyJob("moon.set", SkyJobKind.DAILY, SkyJobShape.INSTANT)
    // The phase is a statement about the day and the quarter is an instant of
    // geometry: neither is a thing the clouds can spoil.
    val MoonToday = SkyJob("moon.today", SkyJobKind.DAILY, SkyJobShape.INSTANT, observable = false)
    val MoonPhase = SkyJob(
        "moon.phase", SkyJobKind.POLLING, SkyJobShape.INSTANT, observable = false
    )

    /**
     * The four quarters as four lines (Fase 19). [MoonPhase] answers "which is next",
     * which is the right answer to a question nobody asks: what a reader wants on the
     * calendar is the FULL moon, or the new moon they need for a dark sky. Each is the
     * same instant of geometry [MoonPhase] resolves, asked for by name.
     */
    val MoonNew = quarter("moon.new")
    val MoonFirstQuarter = quarter("moon.first_quarter")
    val MoonFull = quarter("moon.full")
    val MoonLastQuarter = quarter("moon.last_quarter")

    /**
     * The evening the full moon comes up while the sky is still coloured (Fase 28).
     *
     * Once a month the moon rises within minutes of sunset and hangs there, huge and
     * orange, against a sky that has not gone dark yet. An almanac answers this with
     * "full moon 03:14", which is the instant of the geometry and not the evening
     * anybody would go out for. This is the evening.
     *
     * Aperiodic by construction — it is an intersection detected by looking, not a
     * recurrence with a rule — so it is [SkyJobKind.POLLING] like the eclipses.
     */
    val MoonFullAtDusk = SkyJob(
        "moon.full_at_dusk", SkyJobKind.POLLING, SkyJobShape.INSTANT,
        visibilityDependent = true, photographic = true
    )

    /**
     * Earthshine: the thin crescent with the rest of the disc faintly lit — by the
     * earth, which is where the name comes from and why it is worth a line. A few
     * evenings after new moon low in the west, a few mornings before it in the east,
     * and most people have never been told it is a thing they can see.
     */
    val EarthshinePm = SkyJob(
        "earthshine.pm", SkyJobKind.DAILY, SkyJobShape.RANGE,
        visibilityDependent = true, photographic = true
    )
    val EarthshineAm = SkyJob(
        "earthshine.am", SkyJobKind.DAILY, SkyJobShape.RANGE,
        visibilityDependent = true, photographic = true
    )

    // Planets ------------------------------------------------------------------

    /**
     * Venus and Jupiter, and no others (Fase 28, reopening a Fase 19 decision — see
     * [PlanetMath] for the argument and for what is still out).
     *
     * These are the two points of light a passer-by picks out without being taught,
     * and the two the app can answer "what is that bright star" with. Venus is only
     * ever a morning or an evening star, so it gets one line each way; Jupiter can be
     * up all night, so it gets one line for the night.
     *
     * Deliberately NOT [SkyJob.needsDarkness]: Jupiter at magnitude −2 is obvious in a
     * suburban sky and Venus is obvious in daylight, so a full moon does not spoil
     * either — and saying it did would be the app borrowing a rule from the faint
     * things and applying it to the two brightest.
     */
    val VenusEvening = SkyJob(
        "venus.evening", SkyJobKind.DAILY, SkyJobShape.RANGE, visibilityDependent = true
    )
    val VenusMorning = SkyJob(
        "venus.morning", SkyJobKind.DAILY, SkyJobShape.RANGE, visibilityDependent = true
    )
    val JupiterNight = SkyJob(
        "jupiter.night", SkyJobKind.DAILY, SkyJobShape.RANGE, visibilityDependent = true
    )

    /**
     * The three pairs worth naming when they pass close (Fase 28): the moon beside
     * each planet, which happens about monthly and is the sight that makes somebody
     * look up and ask, and the two planets beside each other, which is rarer and
     * better.
     *
     * Aperiodic, and resolved only when the pair can actually be SEEN from here: the
     * moon meets Venus every month and half of those happen behind the sun.
     */
    val MoonVenus = conjunction("conjunction.moon_venus")
    val MoonJupiter = conjunction("conjunction.moon_jupiter")
    val VenusJupiter = conjunction("conjunction.venus_jupiter")

    /**
     * The full moon of the year that comes nearest to the earth — about 14 % wider and
     * 30 % brighter than the farthest one, and the only honest reading of a word the
     * internet hands out three or four times a year.
     */
    val MoonClosestFull = SkyJob(
        "moon.closest_full", SkyJobKind.ANNUAL, SkyJobShape.INSTANT, observable = false
    )

    // Eclipses ----------------------------------------------------------------

    /**
     * The two eclipses, each resolved **for this place**: a lunar one only when the
     * moon is up here, a solar one only when the moon takes a bite out of the sun as
     * seen from these coordinates — and clipped to the part of it that happens in
     * daylight, because the geometry does not stop at the horizon and the reader does.
     *
     * Deliberately NOT [SkyJob.visibilityDependent]: clouds decide whether a meteor
     * shower is worth setting an alarm for, and do not decide that about an eclipse.
     * People travel for these.
     */
    val LunarEclipse = SkyJob(
        "eclipse.lunar", SkyJobKind.POLLING, SkyJobShape.RANGE, photographic = true
    )

    /**
     * And the solar one is deliberately **not** [SkyJob.photographic], which is the one
     * place that flag is decided on safety rather than on what the event looks like.
     *
     * The app's own line about a solar eclipse is a warning — never look at the sun
     * without a proper filter — and "bring a camera" printed beside it reads as
     * permission. It is not: a lens pointed at the sun without a solar filter destroys
     * the sensor behind it, and through an optical viewfinder it destroys the eye
     * behind that. The flag exists to send somebody outside with a camera, so the one
     * event where that needs equipment this app cannot check for does not get it.
     */
    val SolarEclipse = SkyJob("eclipse.solar", SkyJobKind.POLLING, SkyJobShape.RANGE)

    // Seasons -----------------------------------------------------------------
    val EquinoxSpring = season("equinox.spring")
    val SolsticeSummer = season("solstice.summer")
    val EquinoxAutumn = season("equinox.autumn")
    val SolsticeWinter = season("solstice.winter")

    /**
     * The two ends of the earth's orbit. Unobservable by definition — nothing looks
     * different — and worth a line for what they correct: the earth is at its closest
     * to the sun in the first week of JANUARY, which is the northern winter.
     */
    val Perihelion = season("earth.perihelion")
    val Aphelion = season("earth.aphelion")

    /**
     * The earliest sunset and the latest sunrise of the winter, which are **not** the
     * solstice: the equation of time pulls them a fortnight either side of it at
     * Milan's latitude and seven weeks at the equator. Both are sunsets and sunrises
     * like any other, so the clouds get their say on them.
     */
    val EarliestSunset = SkyJob("sun.earliest_set", SkyJobKind.ANNUAL, SkyJobShape.INSTANT)
    val LatestSunrise = SkyJob("sun.latest_rise", SkyJobKind.ANNUAL, SkyJobShape.INSTANT)

    /**
     * The two evenings that open and close the white nights: above roughly 48.5° the
     * summer sun stops going 18° under the horizon and the astronomical night pauses
     * for weeks. Below that latitude both resolve to `∅` with the reason, which is a
     * fact about where you are and not a gap in the list.
     */
    val WhiteNightsStart = season("night.white.start")
    val WhiteNightsEnd = season("night.white.end")

    /**
     * One `meteor.<shower>.peak` per row of [MeteorShowerTable] — annual jobs whose
     * instant comes from a solar longitude, so the list never expires.
     */
    val meteorShowers: List<SkyJob> = MeteorShowerTable.all.map { shower ->
        SkyJob(
            MeteorShowerTable.jobId(shower), SkyJobKind.ANNUAL, SkyJobShape.RANGE,
            visibilityDependent = true, needsDarkness = true
        )
    }

    val all: List<SkyJob> = buildList {
        add(SunRise); add(SunSet); add(SolarNoon)
        add(CivilAm); add(CivilPm)
        add(NauticalAm); add(NauticalPm)
        add(AstronomicalAm); add(AstronomicalPm)
        add(GoldenAm); add(GoldenPm)
        add(BlueAm); add(BluePm)
        add(DarknessWindow); add(MilkyWayCore); add(ZodiacalPm); add(ZodiacalAm)
        add(MoonRise); add(MoonSet); add(MoonToday); add(MoonPhase)
        add(MoonNew); add(MoonFirstQuarter); add(MoonFull); add(MoonLastQuarter)
        add(MoonClosestFull); add(MoonFullAtDusk); add(EarthshinePm); add(EarthshineAm)
        add(VenusEvening); add(VenusMorning); add(JupiterNight)
        add(MoonVenus); add(MoonJupiter); add(VenusJupiter)
        add(LunarEclipse); add(SolarEclipse)
        add(EquinoxSpring); add(SolsticeSummer); add(EquinoxAutumn); add(SolsticeWinter)
        add(Perihelion); add(Aphelion)
        add(EarliestSunset); add(LatestSunrise)
        add(WhiteNightsStart); add(WhiteNightsEnd)
        addAll(meteorShowers)
    }

    /**
     * What a fresh install subscribes to: four lines. A user who opens the tab and
     * finds all fifty will close it — the catalog is what the file CAN hold, not what
     * it should greet anyone with.
     */
    val defaults: List<SkyJob> = listOf(SunRise, SunSet, GoldenPm, MoonToday)

    fun byId(id: String): SkyJob? = all.firstOrNull { it.id == id }

    /** Position in the file, used to keep the rendered order stable. */
    fun orderOf(job: SkyJob): Int = all.indexOfFirst { it.id == job.id }

    private fun twilight(id: String) =
        SkyJob(id, SkyJobKind.DAILY, SkyJobShape.INSTANT, visibilityDependent = false)

    private fun visibleRange(id: String, photographic: Boolean = false) = SkyJob(
        id, SkyJobKind.DAILY, SkyJobShape.RANGE,
        visibilityDependent = true, photographic = photographic
    )

    /** A close approach of two naked-eye bodies: aperiodic, and an instant. */
    private fun conjunction(id: String) = SkyJob(
        id, SkyJobKind.POLLING, SkyJobShape.INSTANT, visibilityDependent = true
    )

    /** A window that wants a clear sky AND a dark one. */
    private fun darkSky(id: String) = SkyJob(
        id, SkyJobKind.DAILY, SkyJobShape.RANGE,
        visibilityDependent = true, needsDarkness = true
    )

    /** An instant of geometry the clouds have no opinion about. */
    private fun quarter(id: String) =
        SkyJob(id, SkyJobKind.POLLING, SkyJobShape.INSTANT, observable = false)

    // A season is a date on the calendar, not an evening out.
    private fun season(id: String) =
        SkyJob(id, SkyJobKind.ANNUAL, SkyJobShape.INSTANT, observable = false)
}
