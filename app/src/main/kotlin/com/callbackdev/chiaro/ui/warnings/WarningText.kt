package com.callbackdev.chiaro.ui.warnings

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.domain.warnings.PlaceWarnings
import com.callbackdev.chiaro.domain.warnings.WarningHazard
import com.callbackdev.chiaro.domain.warnings.WarningLevel
import com.callbackdev.chiaro.domain.warnings.WarningZone
import com.callbackdev.chiaro.ui.theme.ChiaroTheme
import com.callbackdev.chiaro.ui.theme.VerdictColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * One vocabulary for the official warnings, shared by every surface that says one: the
 * notification (Fase 11's second step), the banner and the sheet on Today, the card in
 * Avvisi, the Journal's lines and the widgets' chip.
 *
 * It lives here and not in any of them for the reason [com.callbackdev.chiaro.ui.today.HeadlineText]
 * lives outside the two screens that print it: two copies of "Allerta arancione per
 * temporali" drift, and the drift shows up as a notification and a banner disagreeing
 * about the same bulletin.
 *
 * **The authority's own words are quoted, not translated** (VISION §8): the level
 * meanings and the bulletin's note stay in Italian for an English reader too, labelled
 * as the issuer's. Levels, hazards, days and zones localize, because those are ours.
 */
object WarningText {

    /** «Allerta gialla» — the level as a phrase that can open a sentence. */
    @StringRes
    fun phraseRes(level: WarningLevel): Int = when (level) {
        WarningLevel.RED -> R.string.warning_phrase_red
        WarningLevel.ORANGE -> R.string.warning_phrase_orange
        else -> R.string.warning_phrase_yellow
    }

    /** «giallo» — the level as an adjective after a hazard ("temporali giallo"). */
    @StringRes
    fun levelWordRes(level: WarningLevel): Int = when (level) {
        WarningLevel.RED -> R.string.warning_level_red
        WarningLevel.ORANGE -> R.string.warning_level_orange
        WarningLevel.YELLOW -> R.string.warning_level_yellow
        WarningLevel.NONE -> R.string.warning_level_none
    }

    /** «rischio idrogeologico» — the hazard as the bulletin names it. */
    @StringRes
    fun hazardRes(hazard: WarningHazard): Int = when (hazard) {
        WarningHazard.HYDRAULIC -> R.string.warning_hazard_hydraulic
        WarningHazard.HYDROGEOLOGICAL -> R.string.warning_hazard_hydrogeological
        WarningHazard.THUNDERSTORM -> R.string.warning_hazard_thunderstorm
    }

    /** «idrogeologico» — the same hazard where a grid cell or a notification line has
     * no room for the noun. */
    @StringRes
    fun shortHazardRes(hazard: WarningHazard): Int = when (hazard) {
        WarningHazard.HYDRAULIC -> R.string.warning_hazard_short_hydraulic
        WarningHazard.HYDROGEOLOGICAL -> R.string.warning_hazard_short_hydrogeological
        WarningHazard.THUNDERSTORM -> R.string.warning_hazard_short_thunderstorm
    }

    /** What the Dipartimento says a level means. In Italian in both languages. */
    @StringRes
    fun meaningRes(level: WarningLevel): Int = when (level) {
        WarningLevel.RED -> R.string.warning_meaning_red
        WarningLevel.ORANGE -> R.string.warning_meaning_orange
        else -> R.string.warning_meaning_yellow
    }

    /** «temporali e rischio idrogeologico», sentence-cased for the head of a line. */
    fun hazards(context: Context, hazards: List<WarningHazard>, capitalize: Boolean = false): String {
        val joined = hazards
            .map { context.getString(hazardRes(it)) }
            .joinToString(context.getString(R.string.warning_hazard_join))
        return if (capitalize) joined.replaceFirstChar { it.titlecase(Locale.getDefault()) } else joined
    }

    /**
     * «Allerta arancione per temporali, gialla per rischio idrogeologico» — every graded
     * hazard, highest level first, one clause per level. The leading clause carries the
     * phrase; the rest carry the level's adjective alone, because "Allerta arancione per
     * temporali, allerta gialla per…" is not how anybody says it.
     */
    fun sentence(context: Context, warnings: PlaceWarnings): String {
        val byLevel = warnings.ranked.groupBy({ it.second }, { it.first })
        return byLevel.entries
            .sortedByDescending { it.key }
            .mapIndexed { index, (level, hazards) ->
                val what = hazards(context, hazards)
                if (index == 0) {
                    context.getString(R.string.warning_clause_first, context.getString(phraseRes(level)), what)
                } else {
                    context.getString(R.string.warning_clause_more, context.getString(levelAdjectiveRes(level)), what)
                }
            }
            .joinToString(context.getString(R.string.warning_clause_join))
    }

    /** «gialla» — the level agreeing with «allerta», for a clause after the first. */
    @StringRes
    private fun levelAdjectiveRes(level: WarningLevel): Int = when (level) {
        WarningLevel.RED -> R.string.warning_adjective_red
        WarningLevel.ORANGE -> R.string.warning_adjective_orange
        else -> R.string.warning_adjective_yellow
    }

    /**
     * «Oggi fino a mezzanotte», «Domani», «Oggi e domani» — the days a set of grades
     * covers. [today] is the ISSUER's day, not the device's.
     */
    fun days(context: Context, days: List<LocalDate>, today: LocalDate): String {
        val hasToday = today in days
        val hasTomorrow = today.plusDays(1) in days
        return when {
            hasToday && hasTomorrow -> context.getString(R.string.notif_warning_today_and_tomorrow)
            hasTomorrow -> context.getString(R.string.notif_warning_tomorrow)
            hasToday -> context.getString(R.string.notif_warning_today)
            // A bulletin starting after tomorrow does not exist; the date is the honest fallback.
            else -> days.first().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
        }
    }

    /**
     * What a zone is called inside a sentence ("Allerta per …", "Nessuna allerta per …").
     * Thirteen zones have no name of their own — the Region never gave them one and the
     * file repeats the code — and a code is the one thing that must never reach a screen
     * (DESIGN §8.13), so those name their region instead.
     *
     * The word "regione" is in the string on purpose: the thirteen are Basilicata's seven
     * and the Marche's six, and Italian wants "in Basilicata" but "nelle Marche". With
     * the noun in front, one sentence works for both.
     */
    fun zoneLabel(context: Context, zone: WarningZone): String =
        if (zone.named) zone.name else context.getString(R.string.warning_zone_unnamed, zone.region)

    /** The pair the level wears (DESIGN §2.3). NONE takes `unknown`: no answer, no colour. */
    val WarningLevel.colors: VerdictColors
        @Composable @ReadOnlyComposable get() = when (this) {
            WarningLevel.RED -> ChiaroTheme.colors.warningRed
            WarningLevel.ORANGE -> ChiaroTheme.colors.warningOrange
            WarningLevel.YELLOW -> ChiaroTheme.colors.warningYellow
            WarningLevel.NONE -> ChiaroTheme.colors.unknown
        }
}
