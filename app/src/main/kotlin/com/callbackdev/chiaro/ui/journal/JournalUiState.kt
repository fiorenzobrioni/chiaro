package com.callbackdev.chiaro.ui.journal

import com.callbackdev.chiaro.data.FetchFailure
import com.callbackdev.chiaro.data.FetchFailureReason
import com.callbackdev.chiaro.data.local.ForecastDiff
import com.callbackdev.chiaro.data.local.ForecastOutcome
import com.callbackdev.chiaro.data.local.SnapshotDiff
import com.callbackdev.chiaro.domain.model.City
import com.callbackdev.chiaro.domain.sky.SkyRun
import com.callbackdev.chiaro.domain.sky.SkyVerdictKind
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * One history commit, already decoded by the repository: what the Journal reads.
 * The ViewModel prepares these; the builder below never touches JSON.
 */
data class JournalRow(
    val at: Instant,
    val forecast: Map<String, String>,
    val firedRules: List<String>,
    val skyRuns: List<SkyRun>,
    /** The commit's `current.*` snapshot — what the app FOUND when it looked, which
     * is the evidence [JournalEntry.DayOutcome] judges a finished day against. */
    val snapshot: Map<String, String> = emptyMap()
)

/** One line of the Journal's prose, newest first inside its day (VISION §5.5). */
sealed interface JournalEntry {
    val at: Instant

    /**
     * A forecast revision for one target day. [better] is the judgement the sentence
     * carries (DESIGN §8.10: the judgement lives in words, never in the strip's
     * color): rain fell → better, rain rose → worse, anything else → null and the
     * sentence stays neutral.
     */
    data class ForecastShift(
        override val at: Instant,
        val date: LocalDate,
        val better: Boolean?,
        val shifts: List<FieldShift>,
        /** How many updates this line folds (the Journal's entries fold a journal day's
         * revisions of one target day into one line; Today's "what changed" never
         * folds, so there it is always 1). */
        val revisions: Int = 1
    ) : JournalEntry

    data class RuleFired(override val at: Instant, val name: String) : JournalEntry

    /** A sky moment the app observed as run; [verdict] null means no fetch came
     * near enough to have an opinion, and the entry says so instead of inventing. */
    data class SkyObserved(
        override val at: Instant,
        val jobId: String,
        val verdict: SkyVerdictKind?,
        val cloudPct: Int?
    ) : JournalEntry

    data class FetchFailed(
        override val at: Instant,
        val reason: FetchFailureReason
    ) : JournalEntry

    /**
     * A finished day, checked against what happened (`ForecastOutcome`). [at] is the
     * day's last instant, so newest-first ordering inside a day puts the verdict at
     * the top of the day it closes. Only days the app watched enough to speak about
     * become entries — see the engine for the two asymmetric rules.
     */
    data class DayOutcome(
        override val at: Instant,
        val date: LocalDate,
        val forecastPrecipPct: Int,
        val rained: Boolean,
        val forecastHighC: Double?,
        val observedHighC: Double?,
        val coveredHours: Int
    ) : JournalEntry
}

/** One changed field: old value (null when the day just entered the horizon) → new. */
data class FieldShift(val field: String, val old: String?, val new: String)

data class JournalDay(val date: LocalDate, val entries: List<JournalEntry>)

/**
 * The drift matrix (VISION §5.5): one row per target day, one column per **slot of
 * time**, values on the metric's own scale. Null cells are slots whose fetch did not
 * cover that day — drawn as absence, never as a zero.
 */
data class DriftModel(
    val dates: List<LocalDate>,
    /**
     * One entry per column, oldest first; null where no fetch landed in that slot.
     * The axis is TIME, so a night with the phone off is a gap in the strip and not
     * a column that quietly disappears.
     */
    val columns: List<Instant?>,
    val rain: List<List<Int?>>,
    val highC: List<List<Double?>>,
    /**
     * One per row: the day's latest predicted minimum, present **only when it is at or
     * below freezing**. Not a third metric — the minimum has no drift worth a ramp, it
     * has a threshold — so it is a mark on the day and a clause in words, never a
     * column of its own.
     */
    val frostC: List<Double?>,
    /** Hours one column stands for — the caption states it. */
    val columnHours: Long
) {
    /**
     * Both ends of the strip are real fetches: the last slot is the one the newest
     * fetch defines, and leading empty slots are trimmed off before the model is
     * built. Only the interior can be a gap.
     */
    val newest: Instant get() = columns.last()!!
    val oldest: Instant get() = columns.first()!!
}

data class JournalContent(
    val placeName: String,
    val zone: ZoneId,
    val days: List<JournalDay>,
    /** Null until at least two slots carry a forecast: one column is not a drift. */
    val drift: DriftModel?
)

/**
 * rows + failures + the city → the whole screen, pure like every builder before it.
 * The diff engine is the inherited [ForecastDiff], thresholds and baselines included:
 * the Journal says "Saturday improved" on exactly the evidence the store recorded.
 */
object JournalStateBuilder {

    /** Columns the strip can hold before it stops being readable at 8dp cells. */
    private const val MAX_COLUMNS = 14

    /**
     * Hours one column stands for. 14 × 6 h is three and a half days, which is the
     * "over the last several days" the strip is supposed to answer for (VISION §5.5).
     *
     * The axis used to be one column per FETCH, which at the default hour of cadence
     * drew fourteen hours under a heading that says "the week", printed "from Sat 5 to
     * Sat 5" underneath, and spaced a phone that slept all night exactly like one that
     * fetched every hour. Time is the axis; a slot with no fetch in it is a gap.
     */
    private const val COLUMN_HOURS = 6L

    /**
     * Where the drift strip marks a day (committente, 8 set 2026). Zero and not two:
     * a marker that fires on a night the reader's own thermometer will read as +2 °C
     * is a marker they learn to distrust, and ground frost under a mild screen-level
     * minimum is a nuance an alert can carry and a glyph cannot.
     */
    private const val FREEZING_C = 0.0

    /** The place's own zone, falling back to the device's: the Journal groups, labels
     * and judges days in it, and the ViewModel wakes on its midnight. One resolution,
     * so those four can never disagree. */
    fun zoneOf(city: City): ZoneId =
        city.timezone?.let { runCatching { ZoneId.of(it) }.getOrNull() } ?: ZoneId.systemDefault()

    fun build(
        city: City,
        rows: List<JournalRow>,
        failures: List<FetchFailure>,
        now: Instant
    ): JournalContent {
        val zone = zoneOf(city)
        val entries = buildList {
            addAll(forecastShifts(rows, zone))
            addAll(outcomes(rows, zone, now))
            rows.forEach { row ->
                row.firedRules.forEach { add(JournalEntry.RuleFired(row.at, it)) }
                row.skyRuns.forEach { run ->
                    add(
                        JournalEntry.SkyObserved(
                            at = Instant.ofEpochSecond(run.atEpochSeconds),
                            jobId = run.jobId,
                            verdict = run.verdict?.takeIf { it != SkyVerdictKind.UNKNOWN },
                            cloudPct = run.cloudPct
                        )
                    )
                }
            }
            failures.forEach {
                add(JournalEntry.FetchFailed(Instant.ofEpochSecond(it.atEpochSeconds), it.reason))
            }
        }
        val days = entries
            .groupBy { it.at.atZone(zone).toLocalDate() }
            .map { (date, dayEntries) ->
                JournalDay(date, dayEntries.sortedByDescending { it.at })
            }
            .sortedByDescending { it.date }
        return JournalContent(
            placeName = city.name,
            zone = zone,
            days = days,
            drift = drift(rows, zone, now)
        )
    }

    /**
     * Today's "what changed" (VISION §5.2.5): the revisions the NEWEST fetch made,
     * biggest rain move first, at most [limit] lines — two or three sentences, not
     * a second journal on the home screen.
     */
    fun latestShifts(rows: List<JournalRow>, limit: Int = 3): List<JournalEntry.ForecastShift> {
        val newest = rows.maxOfOrNull { it.at } ?: return emptyList()
        return revisions(rows)
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
     * The inherited diff, read as prose: only hunks WITH a baseline become entries —
     * a day entering the horizon is a fact about the calendar, not a revision worth
     * a sentence. One entry per fetch and target day: Today's "what changed" reads
     * these as they are, the Journal folds them ([forecastShifts]).
     */
    private fun revisions(rows: List<JournalRow>): List<JournalEntry.ForecastShift> {
        val fetches = rows
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
                    JournalEntry.ForecastShift(
                        at = at,
                        date = LocalDate.parse(hunk.date),
                        better = judgement(shifts),
                        shifts = shifts
                    )
                }
        }
    }

    /**
     * The Journal's entries: the revisions of one target day inside one journal day,
     * folded into one line (review, 8 set 2026). At the hourly cadence and over seven
     * target days, every fetch that moved a high by a degree or the rain by ten points
     * was a line of its own — a dozen or more a day of the same tenor, "Friday's high
     * went from 24° to 25°", and the diary stopped being readable. Each field now runs
     * from the first value the day heard to the last, the line says how many updates
     * that took, and a field that came back to where it started is not a change at
     * all — the drift sentence already reasoned that way ("went up and down"), the
     * prose did not. A day whose every field came back leaves no line.
     */
    private fun forecastShifts(rows: List<JournalRow>, zone: ZoneId): List<JournalEntry.ForecastShift> =
        revisions(rows)
            .groupBy { it.at.atZone(zone).toLocalDate() to it.date }
            .values
            .mapNotNull { group -> fold(group.sortedBy { it.at }) }

    private fun fold(group: List<JournalEntry.ForecastShift>): JournalEntry.ForecastShift? {
        val last = group.last()
        val fields = group.flatMap { it.shifts }.map { it.field }.distinct()
        val shifts = fields.mapNotNull { field ->
            val first = group.firstNotNullOfOrNull { it.shifts.firstOrNull { s -> s.field == field } }
            val newest = group.asReversed()
                .firstNotNullOfOrNull { it.shifts.firstOrNull { s -> s.field == field } }
            if (first == null || newest == null || sameValue(first.old, newest.new)) null
            else FieldShift(field, first.old, newest.new)
        }
        if (shifts.isEmpty()) return null
        return JournalEntry.ForecastShift(
            at = last.at,
            date = last.date,
            better = judgement(shifts),
            shifts = shifts,
            revisions = group.size
        )
    }

    /** Numbers compare as numbers ("24.0" is "24"), anything else as text; an absent
     * old value is never the same as a present new one. */
    private fun sameValue(old: String?, new: String): Boolean {
        if (old == null) return false
        val a = old.toDoubleOrNull()
        val b = new.toDoubleOrNull()
        return if (a != null && b != null) a == b else old == new
    }

    /** The other half of the loop: what the app said, checked against what it then
     * saw. The engine decides what may be claimed; this only dates the entry. */
    private fun outcomes(
        rows: List<JournalRow>,
        zone: ZoneId,
        now: Instant
    ): List<JournalEntry.DayOutcome> = ForecastOutcome
        .compute(
            fetches = rows.map {
                ForecastOutcome.Fetch(it.at.epochSecond, it.forecast, it.snapshot)
            },
            zone = zone,
            today = now.atZone(zone).toLocalDate()
        )
        .map { outcome ->
            JournalEntry.DayOutcome(
                at = outcome.date.plusDays(1).atStartOfDay(zone).toInstant().minusSeconds(1),
                date = outcome.date,
                forecastPrecipPct = outcome.forecastPrecipPct,
                rained = outcome.rained,
                forecastHighC = outcome.forecastHighC,
                observedHighC = outcome.observedHighC,
                coveredHours = outcome.coveredHours
            )
        }

    /** REMOVED+ADDED pairs become old → new; context lines are not a change. */
    private fun fieldShifts(lines: List<SnapshotDiff.Line>): List<FieldShift> {
        val removed = lines.filter { it.type == SnapshotDiff.Type.REMOVED }
            .associate { it.key to it.value }
        return lines
            .filter { it.type == SnapshotDiff.Type.ADDED }
            .map { FieldShift(it.key, removed[it.key], it.value) }
    }

    /** Rain decides the word: it is the number people plan around (VISION §5.5's own
     * example). Temperature alone stays neutral — warmer is not universally better. */
    private fun judgement(shifts: List<FieldShift>): Boolean? {
        val rain = shifts.firstOrNull { it.field == "precip_pct" } ?: return null
        val old = rain.old?.toDoubleOrNull() ?: return null
        val new = rain.new.toDoubleOrNull() ?: return null
        return when {
            new < old -> true
            new > old -> false
            else -> null
        }
    }

    /**
     * Rows are the days still AHEAD — the ones a person is planning, which is why a
     * horizon gone stale offline loses its rows instead of showing a "week ahead"
     * that already happened. Columns are slots of [COLUMN_HOURS], newest last, each
     * holding the last fetch that landed in it.
     */
    private fun drift(rows: List<JournalRow>, zone: ZoneId, now: Instant): DriftModel? {
        val fetches = rows
            .filter { it.forecast.isNotEmpty() }
            .sortedBy { it.at }
        val newest = fetches.lastOrNull()?.at ?: return null
        val slots = arrayOfNulls<JournalRow>(MAX_COLUMNS)
        fetches.forEach { row ->
            val slotsBack = Duration.between(row.at, newest).seconds / (COLUMN_HOURS * 3600)
            if (slotsBack in 0 until MAX_COLUMNS.toLong()) {
                val index = (MAX_COLUMNS - 1 - slotsBack).toInt()
                val held = slots[index]
                if (held == null || row.at.isAfter(held.at)) slots[index] = row
            }
        }
        // Leading empty slots are not a gap in the record, they are a record that does
        // not reach that far back yet: the strip starts where the evidence does.
        val columns = slots.dropWhile { it == null }
        if (columns.count { it != null } < 2) return null
        val today = now.atZone(zone).toLocalDate()
        val dates = fetches.last().forecast.keys
            .map { it.substringBefore('.') }
            .distinct()
            .sorted()
            .mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }
            // Today included while it runs (review, 8 set 2026): "has today's rain
            // been going up?" is the morning's question, and the row was the one the
            // strip did not have. Days gone by still fall off — a strip of the past
            // is not the week ahead.
            .filter { !it.isBefore(today) }
        if (dates.isEmpty()) return null
        fun cell(date: LocalDate, field: String): List<String?> =
            columns.map { it?.forecast?.get("$date.$field") }
        return DriftModel(
            dates = dates,
            columns = columns.map { it?.at },
            rain = dates.map { date -> cell(date, "precip_pct").map { it?.toDoubleOrNull()?.toInt() } },
            highC = dates.map { date -> cell(date, "high_c").map { it?.toDoubleOrNull() } },
            // The LATEST prediction decides, not the coldest one ever made: the mark
            // says what the app is forecasting now, so a day that has warmed back
            // above zero loses it instead of keeping a warning nobody still stands by.
            frostC = dates.map { date ->
                cell(date, "low_c").mapNotNull { it?.toDoubleOrNull() }
                    .lastOrNull()
                    ?.takeIf { it <= FREEZING_C }
            },
            columnHours = COLUMN_HOURS
        )
    }
}
