package com.callbackdev.chiaro.ui.sky

import android.content.res.Resources
import androidx.annotation.StringRes
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.domain.sky.MeteorShowerTable
import com.callbackdev.chiaro.domain.sky.SkyJob
import com.callbackdev.chiaro.domain.sky.SkyJobCatalog
import com.callbackdev.chiaro.domain.sky.SkyJobKind
import com.callbackdev.chiaro.domain.sky.SkyJobShape

/**
 * The guide to the sky events: a page for every one of the fifty-one the app can
 * follow (VISION §5.3 — "this is where a person learns what a blue hour is").
 *
 * The catalog already carried a name and one line each, and one line is enough to
 * pick from a list and not nearly enough to understand what you picked. This is the
 * long half: what the thing IS, what decides whether you see it, and the handful of
 * facts that make it worth going outside for — the zodiacal light, why the earliest
 * sunset is not the shortest day, why a gibbous moon ruins a galaxy.
 *
 * Ported from tweather's `man 7 <job>` pages (its Fase 23), which is where the prose
 * was written; the register is not. Every sentence that spoke about a crontab line, a
 * job or a file says event, entry, app and place here, because this edition has none
 * of those words on screen and a page that mentions one would be the fork showing
 * through.
 *
 * Three parts, and only the first is written by hand:
 *
 * - the **description** ([pageRes]), two paragraphs a person wrote;
 * - **when it happens** ([whenLines]), read off [SkyJob] and never written down a
 *   second time — cadence, shape, whether the clouds get an opinion and whether the
 *   sky has to be dark are fields the engine already reads, and a hand-written
 *   sentence about them would be a copy free to drift the first time a job changes;
 * - **see also** ([seeAlso]), a hand-written adjacency, because the interesting
 *   neighbour is rarely the next entry in the list.
 */
object SkyGuide {

    /** The catalog as the reader meets it, and the order the guide's index follows.
     * Shared with the "Add a moment" sheet: two lists of the same fifty-one events in
     * two different orders would be the app disagreeing with itself. */
    val groups: List<SkyGuideGroup> = with(SkyJobCatalog) {
        listOf(
            SkyGuideGroup(
                R.string.sky_group_sun,
                listOf(
                    SunRise, SunSet, SolarNoon, GoldenAm, GoldenPm, BlueAm, BluePm,
                    CivilAm, CivilPm, EarliestSunset, LatestSunrise
                )
            ),
            SkyGuideGroup(
                R.string.sky_group_night,
                listOf(
                    NauticalAm, NauticalPm, AstronomicalAm, AstronomicalPm,
                    DarknessWindow, MilkyWayCore, ZodiacalPm, ZodiacalAm,
                    WhiteNightsStart, WhiteNightsEnd
                )
            ),
            SkyGuideGroup(
                R.string.sky_group_moon,
                listOf(
                    MoonRise, MoonSet, MoonToday, MoonPhase,
                    MoonNew, MoonFirstQuarter, MoonFull, MoonLastQuarter, MoonClosestFull
                )
            ),
            SkyGuideGroup(R.string.sky_group_eclipses, listOf(LunarEclipse, SolarEclipse)),
            SkyGuideGroup(
                R.string.sky_group_seasons,
                listOf(
                    EquinoxSpring, SolsticeSummer, EquinoxAutumn, SolsticeWinter,
                    Perihelion, Aphelion
                )
            ),
            SkyGuideGroup(R.string.sky_group_meteors, meteorShowers)
        )
    }

    /**
     * The page itself. `error(...)` for an id with no page, exactly as [SkyText] does
     * for a name: an event shipped without words is not a blank paragraph, it is a
     * crash on a screen somebody opened to learn something, and the test upstairs is
     * what keeps it unreachable.
     */
    @StringRes
    fun pageRes(jobId: String): Int = when (jobId) {
        "sun.rise" -> R.string.sky_about_sun_rise
        "sun.set" -> R.string.sky_about_sun_set
        "solar.noon" -> R.string.sky_about_solar_noon
        "twilight.civil.am" -> R.string.sky_about_civil_am
        "twilight.civil.pm" -> R.string.sky_about_civil_pm
        "twilight.nautical.am" -> R.string.sky_about_nautical_am
        "twilight.nautical.pm" -> R.string.sky_about_nautical_pm
        "twilight.astronomical.am" -> R.string.sky_about_astronomical_am
        "twilight.astronomical.pm" -> R.string.sky_about_astronomical_pm
        "golden_hour.am" -> R.string.sky_about_golden_am
        "golden_hour.pm" -> R.string.sky_about_golden_pm
        "blue_hour.am" -> R.string.sky_about_blue_am
        "blue_hour.pm" -> R.string.sky_about_blue_pm
        "darkness.window" -> R.string.sky_about_darkness
        "milky_way.core" -> R.string.sky_about_milky_way_core
        "zodiacal.pm" -> R.string.sky_about_zodiacal_pm
        "zodiacal.am" -> R.string.sky_about_zodiacal_am
        "moon.rise" -> R.string.sky_about_moon_rise
        "moon.set" -> R.string.sky_about_moon_set
        "moon.today" -> R.string.sky_about_moon_today
        "moon.phase" -> R.string.sky_about_moon_phase
        "moon.new" -> R.string.sky_about_moon_new
        "moon.first_quarter" -> R.string.sky_about_moon_first_quarter
        "moon.full" -> R.string.sky_about_moon_full
        "moon.last_quarter" -> R.string.sky_about_moon_last_quarter
        "moon.closest_full" -> R.string.sky_about_moon_closest_full
        "eclipse.lunar" -> R.string.sky_about_eclipse_lunar
        "eclipse.solar" -> R.string.sky_about_eclipse_solar
        "equinox.spring" -> R.string.sky_about_equinox_march
        "solstice.summer" -> R.string.sky_about_solstice_june
        "equinox.autumn" -> R.string.sky_about_equinox_september
        "solstice.winter" -> R.string.sky_about_solstice_december
        "earth.perihelion" -> R.string.sky_about_earth_perihelion
        "earth.aphelion" -> R.string.sky_about_earth_aphelion
        "sun.earliest_set" -> R.string.sky_about_sun_earliest_set
        "sun.latest_rise" -> R.string.sky_about_sun_latest_rise
        "night.white.start" -> R.string.sky_about_night_white_start
        "night.white.end" -> R.string.sky_about_night_white_end
        "meteor.quadrantids.peak" -> R.string.sky_about_quadrantids
        "meteor.lyrids.peak" -> R.string.sky_about_lyrids
        "meteor.eta_aquariids.peak" -> R.string.sky_about_eta_aquariids
        "meteor.delta_aquariids.peak" -> R.string.sky_about_delta_aquariids
        "meteor.alpha_capricornids.peak" -> R.string.sky_about_alpha_capricornids
        "meteor.perseids.peak" -> R.string.sky_about_perseids
        "meteor.draconids.peak" -> R.string.sky_about_draconids
        "meteor.southern_taurids.peak" -> R.string.sky_about_southern_taurids
        "meteor.orionids.peak" -> R.string.sky_about_orionids
        "meteor.northern_taurids.peak" -> R.string.sky_about_northern_taurids
        "meteor.leonids.peak" -> R.string.sky_about_leonids
        "meteor.geminids.peak" -> R.string.sky_about_geminids
        "meteor.ursids.peak" -> R.string.sky_about_ursids
        else -> error("no page for sky job $jobId")
    }

    /**
     * When it happens, said in words the engine cannot disagree with: the sentences
     * are chosen by [SkyJob]'s own fields, so a page can never claim a cadence or a
     * verdict the app does not actually have.
     */
    fun whenLines(resources: Resources, job: SkyJob): List<String> = buildList {
        add(
            resources.getString(
                when (job.kind) {
                    SkyJobKind.DAILY -> R.string.sky_guide_when_daily
                    SkyJobKind.ANNUAL -> R.string.sky_guide_when_annual
                    SkyJobKind.POLLING -> R.string.sky_guide_when_polling
                }
            )
        )
        add(
            resources.getString(
                when (job.shape) {
                    SkyJobShape.INSTANT -> R.string.sky_guide_when_instant
                    SkyJobShape.RANGE -> R.string.sky_guide_when_range
                }
            )
        )
        if (!job.observable) {
            add(resources.getString(R.string.sky_guide_when_geometry))
        } else if (job.visibilityDependent) {
            add(resources.getString(R.string.sky_guide_when_visibility))
        }
        if (job.needsDarkness) {
            add(resources.getString(R.string.sky_guide_when_darkness))
        }
    }

    /**
     * What to read next. Every pair points both ways ([symmetric]), so no page is a
     * dead end, and the one deliberate exception is added in [seeAlso] rather than
     * here: every meteor shower points at full darkness, because that is what decides
     * whether you see any of them, and full darkness does not point back at thirteen
     * showers, because that is a page nobody finishes.
     */
    private val related: Map<String, List<String>> = with(SkyJobCatalog) {
        symmetric(
            SunRise.id to listOf(SunSet.id, GoldenAm.id, CivilAm.id),
            SunSet.id to listOf(GoldenPm.id, CivilPm.id, BluePm.id),
            SolarNoon.id to listOf(EarliestSunset.id, LatestSunrise.id),
            CivilAm.id to listOf(NauticalAm.id, BlueAm.id),
            CivilPm.id to listOf(NauticalPm.id),
            NauticalAm.id to listOf(AstronomicalAm.id),
            NauticalPm.id to listOf(AstronomicalPm.id),
            AstronomicalAm.id to listOf(DarknessWindow.id),
            AstronomicalPm.id to listOf(DarknessWindow.id, WhiteNightsStart.id),
            GoldenAm.id to listOf(GoldenPm.id, BlueAm.id),
            GoldenPm.id to listOf(BluePm.id),
            BlueAm.id to listOf(BluePm.id),
            DarknessWindow.id to listOf(MoonSet.id, MilkyWayCore.id, MoonNew.id),
            MilkyWayCore.id to listOf(ZodiacalPm.id),
            ZodiacalPm.id to listOf(ZodiacalAm.id),
            ZodiacalAm.id to listOf(EquinoxAutumn.id),
            MoonRise.id to listOf(MoonSet.id, MoonToday.id),
            MoonToday.id to listOf(MoonPhase.id),
            MoonPhase.id to listOf(
                MoonNew.id, MoonFirstQuarter.id, MoonFull.id, MoonLastQuarter.id
            ),
            MoonNew.id to listOf(SolarEclipse.id),
            MoonFull.id to listOf(LunarEclipse.id, MoonClosestFull.id),
            MoonFirstQuarter.id to listOf(MoonLastQuarter.id),
            LunarEclipse.id to listOf(SolarEclipse.id),
            EquinoxSpring.id to listOf(SolsticeSummer.id, EquinoxAutumn.id),
            SolsticeSummer.id to listOf(SolsticeWinter.id, Aphelion.id, WhiteNightsStart.id),
            EquinoxAutumn.id to listOf(SolsticeWinter.id),
            SolsticeWinter.id to listOf(EarliestSunset.id, LatestSunrise.id, Perihelion.id),
            Perihelion.id to listOf(Aphelion.id),
            EarliestSunset.id to listOf(LatestSunrise.id),
            WhiteNightsStart.id to listOf(WhiteNightsEnd.id),
            WhiteNightsEnd.id to listOf(DarknessWindow.id),
            // The two pairs of showers that share a parent body point at each other.
            shower("eta_aquariids") to listOf(shower("orionids")),
            shower("southern_taurids") to listOf(shower("northern_taurids"))
        )
    }

    /** The list under a page, with the showers' shared link folded in and never the
     * page itself. */
    fun seeAlso(jobId: String): List<String> {
        val extra = if (MeteorShowerTable.showerOf(jobId) != null) {
            listOf(SkyJobCatalog.DarknessWindow.id)
        } else {
            emptyList()
        }
        return (related[jobId].orEmpty() + extra)
            .distinct()
            .filter { it != jobId && SkyJobCatalog.byId(it) != null }
    }

    private fun shower(id: String): String =
        MeteorShowerTable.jobId(MeteorShowerTable.all.first { it.id == id })

    /** Both directions of every pair, so a reader can always walk back out. */
    private fun symmetric(vararg pairs: Pair<String, List<String>>): Map<String, List<String>> {
        val out = mutableMapOf<String, MutableList<String>>()
        pairs.forEach { (from, targets) ->
            targets.forEach { to ->
                out.getOrPut(from) { mutableListOf() }.add(to)
                out.getOrPut(to) { mutableListOf() }.add(from)
            }
        }
        return out.mapValues { (_, ids) -> ids.distinct() }
    }
}

/** One heading of the guide's index, and of the "Add a moment" sheet. */
data class SkyGuideGroup(@param:StringRes val titleRes: Int, val jobs: List<SkyJob>)
