package com.callbackdev.chiaro.domain.warnings

import java.time.LocalDate

/** One level that appeared, rose or fell between two bulletins, for one day and hazard. */
data class WarningChange(
    val day: LocalDate,
    val hazard: WarningHazard,
    val from: WarningLevel,
    val to: WarningLevel
)

/**
 * What changed for a place between two consecutive bulletins — the Journal's rows
 * («Allerta arancione per temporali, domani», «Allerta rientrata»). A day the new
 * bulletin covers and the old one did not starts from NONE, which is what the old
 * bulletin said about it by saying nothing; a day the old one covered and the new
 * one dropped is over, not changed. With no previous bulletin, or one about another
 * zone, there is nothing to be "between": the first bulletin a place sees is its
 * state, not its news.
 */
object WarningDiff {

    fun between(previous: PlaceWarnings?, next: PlaceWarnings): List<WarningChange> {
        val before = previous?.takeIf { it.zone.code == next.zone.code }
        return next.days.flatMap { day ->
            val old = before?.days?.firstOrNull { it.date == day.date }?.levels
            if (before == null) return@flatMap emptyList()
            WarningHazard.entries.mapNotNull { hazard ->
                val from = old?.getValue(hazard) ?: WarningLevel.NONE
                val to = day.levels.getValue(hazard)
                if (from == to) null else WarningChange(day.date, hazard, from, to)
            }
        }
    }
}
