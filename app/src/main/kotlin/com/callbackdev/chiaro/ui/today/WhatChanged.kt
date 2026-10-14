package com.callbackdev.chiaro.ui.today

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.data.local.ForecastDiff
import com.callbackdev.chiaro.data.local.SnapshotDiff
import com.callbackdev.chiaro.domain.ConditionWord
import com.callbackdev.chiaro.domain.settings.UnitSettings
import com.callbackdev.chiaro.ui.format.Formats
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** One history commit as "what changed" reads it: when, and the week it forecast. */
data class ForecastCommit(val at: Instant, val forecast: Map<String, String>)

/** One changed field: old value (null when the day just entered the horizon) → new. */
data class FieldShift(val field: String, val old: String?, val new: String)

/**
 * A forecast revision for one target day. [better] is the judgement the sentence
 * carries (DESIGN §8.10: the judgement lives in words, never in a color): rain fell →
 * better, rain rose → worse, anything else → null and the sentence stays neutral.
 */
data class ForecastShift(
    val at: Instant,
    val date: LocalDate,
    val better: Boolean?,
    val shifts: List<FieldShift>
)

/**
 * Today's "what changed" (VISION §5.2.6), pure: the revisions the NEWEST fetch made,
 * read through the diff engine ([ForecastDiff], thresholds and baselines included), so
 * "Saturday improved" says exactly what the stored forecasts say.
 */
object WhatChangedBuilder {

    /** Biggest rain move first, at most [limit] lines: two or three sentences. */
    fun latestShifts(commits: List<ForecastCommit>, limit: Int = 3): List<ForecastShift> {
        val newest = commits.maxOfOrNull { it.at } ?: return emptyList()
        return revisions(commits)
            .filter { it.at == newest }
            .sortedByDescending { shift ->
                val rain = shift.shifts.firstOrNull { it.field == "precip_pct" }
                val old = rain?.old?.toDoubleOrNull() ?: 0.0
                val new = rain?.new?.toDoubleOrNull() ?: 0.0
                kotlin.math.abs(new - old)
            }
            .take(limit)
    }

    /**
     * Only hunks WITH a baseline become revisions: a day entering the horizon is a
     * fact about the calendar, not a revision worth a sentence.
     */
    private fun revisions(commits: List<ForecastCommit>): List<ForecastShift> {
        val fetches = commits
            .filter { it.forecast.isNotEmpty() }
            .sortedBy { it.at }
        val revisions = ForecastDiff.compute(
            fetches.map { ForecastDiff.Fetch(it.at.epochSecond, it.forecast) }
        )
        return revisions.flatMap { revision ->
            val at = fetches[revision.fetchIndex].at
            revision.hunks
                .filter { it.baselineEpochSeconds != null }
                .map { hunk ->
                    val shifts = fieldShifts(hunk.lines)
                    ForecastShift(
                        at = at,
                        date = LocalDate.parse(hunk.date),
                        better = judgement(shifts),
                        shifts = shifts
                    )
                }
        }
    }

    /** REMOVED+ADDED pairs become old → new; context lines are not a change. */
    private fun fieldShifts(lines: List<SnapshotDiff.Line>): List<FieldShift> {
        val removed = lines.filter { it.type == SnapshotDiff.Type.REMOVED }
            .associate { it.key to it.value }
        return lines
            .filter { it.type == SnapshotDiff.Type.ADDED }
            .map { FieldShift(it.key, removed[it.key], it.value) }
    }

    /**
     * Rain decides the word: it is the number people plan around. Temperature alone
     * stays neutral, because warmer is not universally better.
     *
     * When rain does not decide, the sky does (24 set 2026): a day that went from
     * «Sereno» to «Temporale» with the same chance of rain got worse. Ranked by
     * [ConditionWord.severity], the same ladder that picks the day's word.
     */
    private fun judgement(shifts: List<FieldShift>): Boolean? {
        compare(shifts, "precip_pct") { it.toDoubleOrNull() }?.let { return it }
        return compare(shifts, "status") { raw -> ConditionWord.fromId(raw)?.severity?.toDouble() }
    }

    /** true when [field] went down, false when it went up, null when it did not move or
     * either end cannot be read. */
    private fun compare(
        shifts: List<FieldShift>,
        field: String,
        measure: (String) -> Double?
    ): Boolean? {
        val shift = shifts.firstOrNull { it.field == field } ?: return null
        val old = shift.old?.let(measure) ?: return null
        val new = measure(shift.new) ?: return null
        return when {
            new < old -> true
            new > old -> false
            else -> null
        }
    }
}

/** A forecast revision in words: "Saturday 5 improved: rain 70% → 30%". */
object WhatChangedText {

    /** What a value that was never recorded prints as. Not a zero, not a blank. */
    const val MISSING = "–"

    /** "Sabato 5 è migliorato" / "è peggiorato" / neutral when rain did not decide. */
    @Composable
    fun shiftHeadline(shift: ForecastShift, locale: Locale): String {
        val dayFmt = remember(locale) { DateTimeFormatter.ofPattern("EEEE d", locale) }
        val day = shift.date.format(dayFmt)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
        return when (shift.better) {
            true -> stringResource(R.string.changed_shift_better, day)
            false -> stringResource(R.string.changed_shift_worse, day)
            null -> stringResource(R.string.changed_shift_neutral, day)
        }
    }

    /**
     * "sereno → temporale · pioggia 70% → 30% · massima 24° → 27°": the words and
     * numbers behind the verdict. The sky is a [ConditionWord] id printed in the
     * reader's words; one this build does not know is left out rather than guessed.
     */
    @Composable
    fun shiftDetails(
        shifts: List<FieldShift>,
        units: UnitSettings,
        locale: Locale
    ): String {
        fun temp(raw: String?): String? =
            raw?.toDoubleOrNull()?.let { Formats.temperature(it, units.temperature, locale) }
        @Composable
        fun sky(raw: String?): String? = raw?.let(ConditionWord::fromId)
            ?.let { stringResource(WeatherText.condition(it)).lowercase(locale) }
        return shifts.mapNotNull { shift ->
            when (shift.field) {
                "status" -> sky(shift.new)?.let { new ->
                    stringResource(R.string.changed_field_sky, sky(shift.old) ?: MISSING, new)
                }
                // The dash, never a "0%": the old value is absent when the previous
                // fetch carried no probability for that day, and a probability is the
                // one number that must not be invented as zero (§1.1).
                "precip_pct" -> shift.new.toDoubleOrNull()?.toInt()?.let { new ->
                    stringResource(
                        R.string.changed_field_rain,
                        shift.old?.toDoubleOrNull()?.toInt()?.let { Formats.percent(it, locale) }
                            ?: MISSING,
                        Formats.percent(new, locale)
                    )
                }
                "high_c" -> temp(shift.new)?.let { new ->
                    stringResource(R.string.changed_field_high, temp(shift.old) ?: MISSING, new)
                }
                "low_c" -> temp(shift.new)?.let { new ->
                    stringResource(R.string.changed_field_low, temp(shift.old) ?: MISSING, new)
                }
                else -> null
            }
        }.joinToString(" · ")
    }
}
