package com.callbackdev.chiaro.ui.journal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.domain.settings.UnitSettings
import com.callbackdev.chiaro.ui.format.Formats
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * A forecast revision in words, shared between the Journal's entries and Today's
 * "what changed" (VISION §5.2.5 and §5.5 quote the same sentence): one vocabulary,
 * two screens, zero drift between them.
 */
object JournalText {

    /** "Sabato 5 è migliorato" / "è peggiorato" / neutral when rain did not decide. */
    @Composable
    fun shiftHeadline(shift: JournalEntry.ForecastShift, locale: Locale): String {
        val dayFmt = remember(locale) { DateTimeFormatter.ofPattern("EEEE d", locale) }
        val day = shift.date.format(dayFmt)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
        return when (shift.better) {
            true -> stringResource(R.string.journal_shift_better, day)
            false -> stringResource(R.string.journal_shift_worse, day)
            null -> stringResource(R.string.journal_shift_neutral, day)
        }
    }

    /**
     * "pioggia 70% → 30% · massima 24° → 27°" — the numbers behind the word.
     * The stored status field stays out: its value is the engine's English label,
     * and an English word must never reach this screen (VISION §8).
     */
    @Composable
    fun shiftDetails(
        shifts: List<FieldShift>,
        units: UnitSettings,
        locale: Locale
    ): String {
        fun temp(raw: String?): String? =
            raw?.toDoubleOrNull()?.let { Formats.temperature(it, units.temperature, locale) }
        return shifts.mapNotNull { shift ->
            when (shift.field) {
                "precip_pct" -> stringResource(
                    R.string.journal_field_rain,
                    shift.old?.toDoubleOrNull()?.toInt() ?: 0,
                    shift.new.toDoubleOrNull()?.toInt() ?: 0
                )
                "high_c" -> temp(shift.new)?.let { new ->
                    stringResource(R.string.journal_field_high, temp(shift.old) ?: "–", new)
                }
                "low_c" -> temp(shift.new)?.let { new ->
                    stringResource(R.string.journal_field_low, temp(shift.old) ?: "–", new)
                }
                else -> null
            }
        }.joinToString(" · ")
    }
}
