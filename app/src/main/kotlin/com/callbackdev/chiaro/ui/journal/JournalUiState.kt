package com.callbackdev.chiaro.ui.journal

import com.callbackdev.chiaro.data.FetchFailure
import com.callbackdev.chiaro.data.FetchFailureReason
import com.callbackdev.chiaro.data.local.ForecastDiff
import com.callbackdev.chiaro.data.local.SnapshotDiff
import com.callbackdev.chiaro.domain.model.City
import com.callbackdev.chiaro.domain.sky.SkyRun
import com.callbackdev.chiaro.domain.sky.SkyVerdictKind
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
    val skyRuns: List<SkyRun>
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
        val shifts: List<FieldShift>
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
}

/** One changed field: old value (null when the day just entered the horizon) → new. */
data class FieldShift(val field: String, val old: String?, val new: String)

data class JournalDay(val date: LocalDate, val entries: List<JournalEntry>)

/**
 * The drift matrix (VISION §5.5): one row per target day, one column per fetch,
 * values on the metric's own scale. Null cells are fetches whose horizon did not
 * cover that day — drawn as absence, never as a zero.
 */
data class DriftModel(
    val dates: List<LocalDate>,
    val fetches: List<Instant>,
    val rain: List<List<Int?>>,
    val highC: List<List<Double?>>
)

data class JournalContent(
    val placeName: String,
    val zone: ZoneId,
    val days: List<JournalDay>,
    /** Null until at least two fetches carry a forecast: one column is not a drift. */
    val drift: DriftModel?
)

/**
 * rows + failures + the city → the whole screen, pure like every builder before it.
 * The diff engine is the inherited [ForecastDiff], thresholds and baselines included:
 * the Journal says "Saturday improved" on exactly the evidence the store recorded.
 */
object JournalStateBuilder {

    /** Columns the strip can hold before it stops being readable at 8dp cells. */
    private const val MAX_FETCH_COLUMNS = 14

    fun build(
        city: City,
        rows: List<JournalRow>,
        failures: List<FetchFailure>
    ): JournalContent {
        val zone = city.timezone?.let { runCatching { ZoneId.of(it) }.getOrNull() }
            ?: ZoneId.systemDefault()
        val entries = buildList {
            addAll(forecastShifts(rows))
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
            drift = drift(rows)
        )
    }

    /**
     * Today's "what changed" (VISION §5.2.5): the revisions the NEWEST fetch made,
     * biggest rain move first, at most [limit] lines — two or three sentences, not
     * a second journal on the home screen.
     */
    fun latestShifts(rows: List<JournalRow>, limit: Int = 3): List<JournalEntry.ForecastShift> {
        val newest = rows.maxOfOrNull { it.at } ?: return emptyList()
        return forecastShifts(rows)
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
     * a sentence.
     */
    private fun forecastShifts(rows: List<JournalRow>): List<JournalEntry.ForecastShift> {
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
     * Rows are the NEWEST fetch's week — the days still ahead are the ones a person
     * is planning; columns are the fetches that said anything about them.
     */
    private fun drift(rows: List<JournalRow>): DriftModel? {
        val fetches = rows
            .filter { it.forecast.isNotEmpty() }
            .sortedBy { it.at }
            .takeLast(MAX_FETCH_COLUMNS)
        if (fetches.size < 2) return null
        val dates = fetches.last().forecast.keys
            .map { it.substringBefore('.') }
            .distinct()
            .sorted()
            .map(LocalDate::parse)
        if (dates.isEmpty()) return null
        fun cell(fetch: JournalRow, date: LocalDate, field: String): String? =
            fetch.forecast["$date.$field"]
        return DriftModel(
            dates = dates,
            fetches = fetches.map { it.at },
            rain = dates.map { date ->
                fetches.map { cell(it, date, "precip_pct")?.toDoubleOrNull()?.toInt() }
            },
            highC = dates.map { date ->
                fetches.map { cell(it, date, "high_c")?.toDoubleOrNull() }
            }
        )
    }
}
