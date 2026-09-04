package com.callbackdev.chiaro.ui.sky

import android.content.res.Resources
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.domain.model.MoonPhase
import com.callbackdev.chiaro.domain.sky.LunarEclipse
import com.callbackdev.chiaro.domain.sky.LunarEclipseKind
import com.callbackdev.chiaro.domain.sky.SolarEclipse
import com.callbackdev.chiaro.domain.sky.SolarEclipseKind
import com.callbackdev.chiaro.domain.sky.SkyNotScheduled
import com.callbackdev.chiaro.domain.sky.SkyVerdict
import com.callbackdev.chiaro.domain.sky.SkyVerdictKind
import com.callbackdev.chiaro.domain.sky.SkyVerdictNote
import com.callbackdev.chiaro.ui.components.VerdictKind
import kotlin.math.roundToInt

/**
 * Every dotted job id turned into words (VISION §5.3: "the dot notation never
 * appears"). The ids stay English in the code and the stores; what a screen or a
 * notification prints comes from here, so the two can never disagree.
 *
 * Resources-based rather than composable on purpose: [com.callbackdev.chiaro
 * .notifications.SkyNotifier] speaks the same vocabulary from outside composition.
 */
object SkyText {

    /** The moment's name. Unknown ids fail loudly in debug — a job the catalog
     * knows must have words before it ships. */
    fun nameRes(jobId: String): Int = when (jobId) {
        "sun.rise" -> R.string.sky_name_sun_rise
        "sun.set" -> R.string.sky_name_sun_set
        "solar.noon" -> R.string.sky_name_solar_noon
        "twilight.civil.am" -> R.string.sky_name_civil_am
        "twilight.civil.pm" -> R.string.sky_name_civil_pm
        "twilight.nautical.am" -> R.string.sky_name_nautical_am
        "twilight.nautical.pm" -> R.string.sky_name_nautical_pm
        "twilight.astronomical.am" -> R.string.sky_name_astronomical_am
        "twilight.astronomical.pm" -> R.string.sky_name_astronomical_pm
        "golden_hour.am" -> R.string.sky_name_golden_am
        "golden_hour.pm" -> R.string.sky_name_golden_pm
        "blue_hour.am" -> R.string.sky_name_blue_am
        "blue_hour.pm" -> R.string.sky_name_blue_pm
        "darkness.window" -> R.string.sky_name_darkness
        "moon.rise" -> R.string.sky_name_moon_rise
        "moon.set" -> R.string.sky_name_moon_set
        "moon.today" -> R.string.sky_name_moon_today
        "moon.phase" -> R.string.sky_name_moon_phase
        "equinox.spring" -> R.string.sky_name_equinox_march
        "solstice.summer" -> R.string.sky_name_solstice_june
        "equinox.autumn" -> R.string.sky_name_equinox_september
        "solstice.winter" -> R.string.sky_name_solstice_december
        "meteor.quadrantids.peak" -> R.string.sky_name_quadrantids
        "meteor.lyrids.peak" -> R.string.sky_name_lyrids
        "meteor.eta_aquariids.peak" -> R.string.sky_name_eta_aquariids
        "meteor.delta_aquariids.peak" -> R.string.sky_name_delta_aquariids
        "meteor.perseids.peak" -> R.string.sky_name_perseids
        "meteor.draconids.peak" -> R.string.sky_name_draconids
        "meteor.orionids.peak" -> R.string.sky_name_orionids
        "meteor.leonids.peak" -> R.string.sky_name_leonids
        "meteor.geminids.peak" -> R.string.sky_name_geminids
        "meteor.ursids.peak" -> R.string.sky_name_ursids
        "milky_way.core" -> R.string.sky_name_milky_way_core
        "zodiacal.pm" -> R.string.sky_name_zodiacal_pm
        "zodiacal.am" -> R.string.sky_name_zodiacal_am
        "moon.new" -> R.string.sky_name_moon_new
        "moon.first_quarter" -> R.string.sky_name_moon_first_quarter
        "moon.full" -> R.string.sky_name_moon_full
        "moon.last_quarter" -> R.string.sky_name_moon_last_quarter
        "moon.closest_full" -> R.string.sky_name_moon_closest_full
        "eclipse.lunar" -> R.string.sky_name_eclipse_lunar
        "eclipse.solar" -> R.string.sky_name_eclipse_solar
        "earth.perihelion" -> R.string.sky_name_earth_perihelion
        "earth.aphelion" -> R.string.sky_name_earth_aphelion
        "sun.earliest_set" -> R.string.sky_name_sun_earliest_set
        "sun.latest_rise" -> R.string.sky_name_sun_latest_rise
        "night.white.start" -> R.string.sky_name_night_white_start
        "night.white.end" -> R.string.sky_name_night_white_end
        "meteor.alpha_capricornids.peak" -> R.string.sky_name_alpha_capricornids
        "meteor.southern_taurids.peak" -> R.string.sky_name_southern_taurids
        "meteor.northern_taurids.peak" -> R.string.sky_name_northern_taurids
        else -> error("no words for sky job $jobId")
    }

    /** The catalog's one-line explanation: what this moment IS. */
    fun explanationRes(jobId: String): Int = when (jobId) {
        "sun.rise" -> R.string.sky_expl_sun_rise
        "sun.set" -> R.string.sky_expl_sun_set
        "solar.noon" -> R.string.sky_expl_solar_noon
        "twilight.civil.am" -> R.string.sky_expl_civil_am
        "twilight.civil.pm" -> R.string.sky_expl_civil_pm
        "twilight.nautical.am" -> R.string.sky_expl_nautical_am
        "twilight.nautical.pm" -> R.string.sky_expl_nautical_pm
        "twilight.astronomical.am" -> R.string.sky_expl_astronomical_am
        "twilight.astronomical.pm" -> R.string.sky_expl_astronomical_pm
        "golden_hour.am" -> R.string.sky_expl_golden_am
        "golden_hour.pm" -> R.string.sky_expl_golden_pm
        "blue_hour.am" -> R.string.sky_expl_blue_am
        "blue_hour.pm" -> R.string.sky_expl_blue_pm
        "darkness.window" -> R.string.sky_expl_darkness
        "moon.rise" -> R.string.sky_expl_moon_rise
        "moon.set" -> R.string.sky_expl_moon_set
        "moon.today" -> R.string.sky_expl_moon_today
        "moon.phase" -> R.string.sky_expl_moon_phase
        "equinox.spring" -> R.string.sky_expl_equinox_march
        "solstice.summer" -> R.string.sky_expl_solstice_june
        "equinox.autumn" -> R.string.sky_expl_equinox_september
        "solstice.winter" -> R.string.sky_expl_solstice_december
        "meteor.quadrantids.peak" -> R.string.sky_expl_quadrantids
        "meteor.lyrids.peak" -> R.string.sky_expl_lyrids
        "meteor.eta_aquariids.peak" -> R.string.sky_expl_eta_aquariids
        "meteor.delta_aquariids.peak" -> R.string.sky_expl_delta_aquariids
        "meteor.perseids.peak" -> R.string.sky_expl_perseids
        "meteor.draconids.peak" -> R.string.sky_expl_draconids
        "meteor.orionids.peak" -> R.string.sky_expl_orionids
        "meteor.leonids.peak" -> R.string.sky_expl_leonids
        "meteor.geminids.peak" -> R.string.sky_expl_geminids
        "meteor.ursids.peak" -> R.string.sky_expl_ursids
        "milky_way.core" -> R.string.sky_expl_milky_way_core
        "zodiacal.pm" -> R.string.sky_expl_zodiacal_pm
        "zodiacal.am" -> R.string.sky_expl_zodiacal_am
        "moon.new" -> R.string.sky_expl_moon_new
        "moon.first_quarter" -> R.string.sky_expl_moon_first_quarter
        "moon.full" -> R.string.sky_expl_moon_full
        "moon.last_quarter" -> R.string.sky_expl_moon_last_quarter
        "moon.closest_full" -> R.string.sky_expl_moon_closest_full
        "eclipse.lunar" -> R.string.sky_expl_eclipse_lunar
        "eclipse.solar" -> R.string.sky_expl_eclipse_solar
        "earth.perihelion" -> R.string.sky_expl_earth_perihelion
        "earth.aphelion" -> R.string.sky_expl_earth_aphelion
        "sun.earliest_set" -> R.string.sky_expl_sun_earliest_set
        "sun.latest_rise" -> R.string.sky_expl_sun_latest_rise
        "night.white.start" -> R.string.sky_expl_night_white_start
        "night.white.end" -> R.string.sky_expl_night_white_end
        "meteor.alpha_capricornids.peak" -> R.string.sky_expl_alpha_capricornids
        "meteor.southern_taurids.peak" -> R.string.sky_expl_southern_taurids
        "meteor.northern_taurids.peak" -> R.string.sky_expl_northern_taurids
        else -> error("no words for sky job $jobId")
    }

    fun phaseRes(phase: MoonPhase): Int = when (phase) {
        MoonPhase.NEW_MOON -> R.string.moon_phase_new
        MoonPhase.WAXING_CRESCENT -> R.string.moon_phase_waxing_crescent
        MoonPhase.FIRST_QUARTER -> R.string.moon_phase_first_quarter
        MoonPhase.WAXING_GIBBOUS -> R.string.moon_phase_waxing_gibbous
        MoonPhase.FULL_MOON -> R.string.moon_phase_full
        MoonPhase.WANING_GIBBOUS -> R.string.moon_phase_waning_gibbous
        MoonPhase.LAST_QUARTER -> R.string.moon_phase_last_quarter
        MoonPhase.WANING_CRESCENT -> R.string.moon_phase_waning_crescent
    }

    /** Why the sky skipped a day: the `∅` of the engine, in words. */
    fun notScheduledRes(reason: SkyNotScheduled): Int = when (reason) {
        SkyNotScheduled.POLAR_DAY -> R.string.sky_none_polar_day
        SkyNotScheduled.POLAR_NIGHT -> R.string.sky_none_polar_night
        SkyNotScheduled.MOON_ABSENT -> R.string.sky_none_moon
        SkyNotScheduled.NO_DARKNESS -> R.string.sky_none_no_darkness
        SkyNotScheduled.DARKNESS_ALL_YEAR -> R.string.sky_none_darkness_all_year
        SkyNotScheduled.ECLIPTIC_TOO_FLAT -> R.string.sky_none_ecliptic_flat
        SkyNotScheduled.CORE_TOO_LOW -> R.string.sky_none_core_too_low
        SkyNotScheduled.NO_ECLIPSE_AHEAD -> R.string.sky_none_no_eclipse
    }

    /**
     * What an eclipse row says under its name: the kind, and the number the kind was
     * decided on. A verdict ships with its arithmetic (VISION §5.3) and an eclipse is
     * no different — "partial" alone is a word, "72 % of the moon in the shadow" is
     * something a reader can picture before going outside.
     */
    fun lunarEclipseLine(res: Resources, eclipse: LunarEclipse): String = when (eclipse.kind) {
        LunarEclipseKind.TOTAL -> res.getString(R.string.sky_eclipse_lunar_total)
        LunarEclipseKind.PARTIAL -> res.getString(
            R.string.sky_eclipse_lunar_partial,
            (eclipse.umbralMagnitude * 100).roundToInt().coerceIn(1, 99)
        )
        LunarEclipseKind.PENUMBRAL -> res.getString(R.string.sky_eclipse_lunar_penumbral)
    }

    /**
     * The solar line, in OBSCURATION rather than magnitude: the fraction of the disk
     * covered is what the light outside does, and the fraction of the diameter — the
     * number an almanac prints — reads a good deal more dramatic than the afternoon
     * looks. The warning travels with it, always, on every kind.
     */
    fun solarEclipseLine(res: Resources, eclipse: SolarEclipse): String {
        val head = when (eclipse.kind) {
            SolarEclipseKind.TOTAL -> res.getString(R.string.sky_eclipse_solar_total)
            SolarEclipseKind.ANNULAR -> res.getString(R.string.sky_eclipse_solar_annular)
            SolarEclipseKind.PARTIAL -> res.getString(
                R.string.sky_eclipse_solar_partial,
                (eclipse.obscuration * 100).roundToInt().coerceIn(1, 99)
            )
        }
        return head + " \u00b7 " + res.getString(R.string.sky_eclipse_solar_warning)
    }

    /**
     * A bearing in words, to the eighth of the compass. Prose, so it localizes: the
     * app says "look east", never "look 92°" — a number nobody can act on without
     * turning their phone into a compass first.
     */
    fun bearingRes(degrees: Double): Int {
        val point = (((degrees % 360.0) + 360.0) % 360.0 + 22.5).toInt() / 45 % 8
        return when (point) {
            0 -> R.string.compass_n
            1 -> R.string.compass_ne
            2 -> R.string.compass_e
            3 -> R.string.compass_se
            4 -> R.string.compass_s
            5 -> R.string.compass_sw
            6 -> R.string.compass_w
            else -> R.string.compass_nw
        }
    }

    /** The domain's four answers on the UI's four chips. */
    fun chipKind(kind: SkyVerdictKind): VerdictKind = when (kind) {
        SkyVerdictKind.PASS -> VerdictKind.PASS
        SkyVerdictKind.UNSTABLE -> VerdictKind.UNSTABLE
        SkyVerdictKind.FAIL -> VerdictKind.FAIL
        SkyVerdictKind.UNKNOWN -> VerdictKind.UNKNOWN
    }

    fun verdictWordRes(kind: SkyVerdictKind): Int = when (kind) {
        SkyVerdictKind.PASS -> R.string.verdict_pass
        SkyVerdictKind.UNSTABLE -> R.string.verdict_unstable
        SkyVerdictKind.FAIL -> R.string.verdict_fail
        SkyVerdictKind.UNKNOWN -> R.string.verdict_unknown
    }

    /**
     * The number behind the chip's word: the one that decided the verdict. Null for
     * UNKNOWN — not knowing has no arithmetic to show; its reason is a sentence
     * ([unknownReason]), not an evidence figure.
     */
    fun chipEvidence(res: Resources, verdict: SkyVerdict): String? = when {
        verdict.kind == SkyVerdictKind.UNKNOWN -> null
        verdict.note == SkyVerdictNote.MOONLIGHT && verdict.moonPct != null ->
            res.getString(R.string.sky_evidence_moon, verdict.moonPct)
        verdict.note == SkyVerdictNote.PRECIPITATION && verdict.precipPct != null ->
            res.getString(R.string.sky_evidence_rain, verdict.precipPct)
        verdict.cloudPct != null -> res.getString(R.string.sky_evidence_cloud, verdict.cloudPct)
        else -> null
    }

    /** Why the app does not know — always stated, never a bare question mark. */
    fun unknownReason(res: Resources, verdict: SkyVerdict): String? {
        if (verdict.kind != SkyVerdictKind.UNKNOWN) return null
        return when (verdict.note) {
            SkyVerdictNote.BEYOND_HORIZON -> res.getString(R.string.sky_unknown_beyond)
            SkyVerdictNote.NO_DATA -> res.getString(R.string.sky_unknown_no_data)
            SkyVerdictNote.STALE_DATA -> res.getString(R.string.sky_unknown_stale)
            SkyVerdictNote.NO_COVERAGE -> res.getString(R.string.sky_unknown_no_coverage)
            else -> null
        }
    }
}
