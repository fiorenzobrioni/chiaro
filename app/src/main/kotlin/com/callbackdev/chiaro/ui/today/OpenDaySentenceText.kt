package com.callbackdev.chiaro.ui.today

import android.content.Context
import androidx.annotation.StringRes
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.domain.WmoCode
import com.callbackdev.chiaro.ui.today.OpenDaySentence.Fall
import com.callbackdev.chiaro.ui.today.OpenDaySentence.Part
import com.callbackdev.chiaro.ui.today.OpenDaySentence.Register
import com.callbackdev.chiaro.ui.today.OpenDaySentence.Sky
import com.callbackdev.chiaro.ui.today.OpenDaySentence.Span
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * [OpenDaySentence] in words (31 ott 2026): the sky's stretches first, then what falls, one
 * clause each, joined by commas, with no full stop, like the hero's sentence. The words of the
 * sky and of what falls are the app's own condition words, the ones the row's icon and the
 * strip's cells speak, lowered after the first clause.
 */
object OpenDaySentenceText {

    fun of(context: Context, sentence: OpenDaySentence.Sentence?, timeFmt: DateTimeFormatter): String? {
        sentence ?: return null
        val locale = Locale.getDefault()
        val clauses = sky(context, sentence.sky, alone = sentence.falls.isEmpty()) +
            sentence.falls.map { fall(context, it, timeFmt) }
        if (clauses.isEmpty()) return null
        return clauses.mapIndexed { i, clause ->
            if (i == 0) clause.replaceFirstChar { it.titlecase(locale) } else clause
        }.joinToString(", ")
    }

    /** The sky's clauses. A sky that holds all day is «tutto il giorno» on its own, and its
     * bare word in front of what falls: «Coperto, pioggia dalle 15:00 alle 18:00». */
    private fun sky(context: Context, sky: List<Sky>, alone: Boolean): List<String> {
        if (sky.size == 1) {
            val word = word(context, sky.single().code)
            return listOf(if (alone) context.getString(R.string.day_sentence_all_day_of, word) else word)
        }
        return sky.mapIndexed { i, stretch ->
            val word = word(context, stretch.code)
            val parts = stretch.parts
            val time = when {
                parts.size == 1 -> context.getString(inPart(parts.single()))
                i == 0 -> context.getString(untilPart(parts.last()))
                else -> context.getString(fromPart(parts.first()))
            }
            context.getString(R.string.day_sentence_clause, word, time)
        }
    }

    private fun fall(context: Context, fall: Fall, timeFmt: DateTimeFormatter): String {
        val what = when (fall.register) {
            Register.LIGHT -> context.getString(
                if (fall.phase == WmoCode.Phase.FROZEN) R.string.day_sentence_light_flakes
                else R.string.day_sentence_light_drops
            )
            else -> context.getString(WeatherText.condition(fall.word)).lowered()
        }
        val time = and(context, fall.spans.flatMap { span ->
            when (span) {
                is Span.Hours -> listOf(hours(context, span, timeFmt))
                is Span.Parts -> parts(context, span.parts)
            }
        })
        return context.getString(
            if (fall.scattered) R.string.day_sentence_clause_scattered else R.string.day_sentence_clause,
            what, time
        )
    }

    private fun hours(context: Context, span: Span.Hours, timeFmt: DateTimeFormatter): String {
        val from = span.from
        val until = span.until
        return when {
            from == null && until == null -> context.getString(R.string.day_sentence_all_day)
            from == null -> context.getString(R.string.day_sentence_until, until!!.format(timeFmt))
            until == null -> context.getString(R.string.day_sentence_from, from.format(timeFmt))
            until == from.plusHours(1) -> context.getString(R.string.day_sentence_around, from.format(timeFmt))
            else -> context.getString(R.string.day_sentence_hours, from.format(timeFmt), until.format(timeFmt))
        }
    }

    /** The parts touched, as ranges: «nel pomeriggio», «dal mattino alla sera», «al mattino
     * e in serata»; all four are «tutto il giorno». */
    private fun parts(context: Context, parts: List<Part>): List<String> {
        if (parts.size == Part.entries.size) return listOf(context.getString(R.string.day_sentence_all_day))
        val ranges = mutableListOf<MutableList<Part>>()
        parts.sorted().forEach { part ->
            val last = ranges.lastOrNull()
            if (last != null && last.last().ordinal + 1 == part.ordinal) last += part else ranges += mutableListOf(part)
        }
        return ranges.map { range ->
            if (range.size == 1) context.getString(inPart(range.single()))
            else context.getString(
                R.string.day_sentence_range,
                context.getString(fromPart(range.first())),
                context.getString(toPart(range.last()))
            )
        }
    }

    private fun and(context: Context, items: List<String>): String = when (items.size) {
        0 -> ""
        1 -> items.single()
        else -> context.getString(R.string.day_sentence_and, items.dropLast(1).joinToString(", "), items.last())
    }

    private fun word(context: Context, code: Int): String = context.getString(WeatherText.condition(code)).lowered()

    private fun String.lowered(): String = replaceFirstChar { it.lowercase(Locale.getDefault()) }

    @StringRes
    private fun inPart(part: Part): Int = when (part) {
        Part.NIGHT -> R.string.day_sentence_in_night
        Part.MORNING -> R.string.day_sentence_in_morning
        Part.AFTERNOON -> R.string.day_sentence_in_afternoon
        Part.EVENING -> R.string.day_sentence_in_evening
    }

    @StringRes
    private fun fromPart(part: Part): Int = when (part) {
        Part.NIGHT -> R.string.day_sentence_from_night
        Part.MORNING -> R.string.day_sentence_from_morning
        Part.AFTERNOON -> R.string.day_sentence_from_afternoon
        Part.EVENING -> R.string.day_sentence_from_evening
    }

    @StringRes
    private fun toPart(part: Part): Int = when (part) {
        // A range never ends where the day begins; the night's «to» is its «in».
        Part.NIGHT -> R.string.day_sentence_in_night
        Part.MORNING -> R.string.day_sentence_to_morning
        Part.AFTERNOON -> R.string.day_sentence_to_afternoon
        Part.EVENING -> R.string.day_sentence_to_evening
    }

    @StringRes
    private fun untilPart(part: Part): Int = when (part) {
        Part.NIGHT, Part.MORNING -> R.string.day_sentence_until_morning
        Part.AFTERNOON -> R.string.day_sentence_until_afternoon
        Part.EVENING -> R.string.day_sentence_until_evening
    }
}
