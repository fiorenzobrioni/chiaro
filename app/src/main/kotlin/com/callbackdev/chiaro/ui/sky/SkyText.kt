package com.callbackdev.chiaro.ui.sky

import android.content.res.Resources
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.domain.model.MoonPhase
import com.callbackdev.chiaro.domain.sky.SkyNotScheduled
import com.callbackdev.chiaro.domain.sky.SkyVerdict
import com.callbackdev.chiaro.domain.sky.SkyVerdictKind
import com.callbackdev.chiaro.domain.sky.SkyVerdictNote
import com.callbackdev.chiaro.ui.components.VerdictKind

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
