package com.callbackdev.chiaro.domain

import com.callbackdev.chiaro.domain.model.DailyForecast
import com.callbackdev.chiaro.domain.model.HourlyForecast
import java.time.LocalDateTime

/**
 * The day's possible thunderstorm (Fase 17.4, 19 nov 2026; `docs/STATES.md` §6, «The day's
 * possible storm»): the parts of a day in which the air could carry a storm the provider's code
 * does not name. **A possibility of the day, never a state**: it is no hour's icon or word, not
 * the day's label, not a hazard ([AlertEngine.SevereBucket]), no notification of its own, and
 * no reader's rule reads it. Where an hour of the day has the code (rule 1), the code speaks
 * and this says nothing.
 *
 * Why it exists: the provider's thunderstorm code is the ICON family's (STATES §10). Measured
 * against the thunder the airports report (summer and October 2026, 39 airports, 787 days with
 * a storm; `tools/analyze_against_metar.py --hazards`), the code alone names 32% of the storm
 * days, right 64% of the time (CSI 0.27); with this level on the days it is silent, 66%, right
 * 57% (0.44). In every slice, on the place's own day as the app counts it: the Po valley 0.46 →
 * 0.54, the rest of Italy 0.21 → 0.34, the rest of Europe 0.24 → 0.37, outside Europe 0.21 →
 * 0.42. A storm day is rare (13% of the days), so the CSI is not flattered here: the Heidke
 * score, 0 for «always», goes the same way in every slice, 0.35 → 0.50 over the three seasons
 * by the place's day (19 nov 2026, STATES §11). Right one day in two, as «Pioggia possibile»
 * is right one hour in two: a possibility, said as one. By the hour it names it is right one
 * time in ten (STATES §10), which is why it is a part of the day and never an hour; when the
 * day is right the storm falls in a part it names 91% of the time (88–95% in every slice).
 *
 * How the screens say it (Fase 17.5, `docs/STATES.md` §7): «Possibile qualche temporale nel
 * pomeriggio», never «Temporali possibili», which is the word of a storm-coded hour (1095).
 */
object PossibleStorm {

    /**
     * The convective energy from which an hour that rains may be a storm, J/kg: the higher of
     * the hour's two ends. On the plateau the measure found: 300 scores the same by the day (CSI
     * 0.44) and is right less often in Europe (51% against 61%), 1 000 scores lower (0.40) and
     * in Europe takes a third of the storm days instead of a half.
     */
    const val CAPE_JKG = 500.0

    /**
     * One hour of a day as a row shows it: the hour that starts at [start], on the place's
     * clock, with its interval values — the amount and the chance, from the slot after it — and
     * [capeJkg], the higher of the CAPE at its two ends (null where the model has none).
     * [state] is the engine's state of the hour, null where the provider gave it none.
     */
    data class Hour(
        val start: LocalDateTime,
        val state: Int?,
        val capeJkg: Double?,
        val precipitationMm: Double?,
        val chancePct: Int?
    )

    /**
     * The parts of the day, in order, in which an hour has CAPE of [CAPE_JKG] or more over a
     * measurable amount ([WeatherStateEngine.MEASURABLE_MM]) the ensemble backs (a chance of
     * [WeatherStateEngine.PROBABLE_PCT] or more, or none, as rule 3 reads it).
     *
     * Empty when no hour does, and when the code speaks: an hour of [day] is a storm already
     * (rule 1), or the day's own [label] is one. The label reads the provider's slots and the
     * hours here are the rows, an hour apart at the day's edge, so both are asked: a day never
     * says a storm is possible beside an icon or a strip that already says there is one.
     *
     * Null when no hour of [day] carries CAPE — a cache entry from before it was asked for, a
     * model without it — because then the rule cannot be read, and «no storm» would be a
     * forecast nobody made.
     */
    fun parts(day: List<Hour>, label: Int? = null): List<DayPart>? {
        if (day.none { it.capeJkg != null }) return null
        if (isStorm(label) || day.any { isStorm(it.state) }) return emptyList()
        return day.filter(::convective).map { DayPart.of(it.start) }.distinct().sorted()
    }

    private fun isStorm(state: Int?): Boolean =
        state?.let(WmoCode::of)?.hazard == AlertEngine.SevereBucket.THUNDER

    /**
     * What is still to say of [day]'s possible storm at [now] (Fase 17.5): the parts of its
     * hours that are not over yet, read on [rows] by the same rule the mapper read the whole day
     * with. Not the day's parts cut where the clock stands: at 17:00 an afternoon whose unstable
     * hour was 13:00 is an afternoon with nothing possible left in it, and «nel pomeriggio» would
     * be the sentence announcing its own past. Empty when the day has no CAPE to read
     * ([DailyForecast.possibleStorm] null), and when nothing is left of it.
     *
     * The rest of the day decides, not the whole of it (23 nov 2026, as Today's row,
     * [RestOfDay]): a storm that came at 3 in the morning made the day's field empty (the code
     * spoke), and kept the afternoon's possibility unsaid by the hero, the row and the morning's
     * summary alike. A storm hour still ahead silences it as before: it is among [rows].
     */
    fun ahead(day: DailyForecast, rows: List<HourlyForecast>, now: LocalDateTime): List<DayPart> {
        if (day.possibleStorm == null) return emptyList()
        val left = hoursOf(rows).filter { it.start.toLocalDate() == day.date && it.start.plusHours(1).isAfter(now) }
        return parts(left).orEmpty()
    }

    /**
     * [rows] as the rule reads them: each hour with its state, its interval values (the amount
     * and the chance of the hour it starts) and the higher of the CAPE at its two ends, its own
     * instant and the next row's — when the next row is the hour after it; at the end of the
     * list, or across a gap, its own alone.
     */
    fun hoursOf(rows: List<HourlyForecast>): List<Hour> = rows.mapIndexed { i, row ->
        val next = rows.getOrNull(i + 1)?.takeIf { it.at == row.at.plusSeconds(3_600) }
        Hour(
            start = row.time,
            state = row.condition.wmoCode,
            capeJkg = listOfNotNull(row.capeJkg, next?.capeJkg).maxOrNull(),
            precipitationMm = row.precipMm,
            chancePct = row.precipChancePct
        )
    }

    private fun convective(hour: Hour): Boolean {
        val cape = hour.capeJkg ?: return false
        val mm = hour.precipitationMm ?: return false
        val chance = hour.chancePct
        return cape >= CAPE_JKG && WeatherStateEngine.measurable(mm) &&
            (chance == null || chance >= WeatherStateEngine.PROBABLE_PCT)
    }
}
