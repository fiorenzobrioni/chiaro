package com.callbackdev.chiaro.ui.alerts

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle

/**
 * The message field's placeholders as words (15.8, 28 Oct 2026; the Alerts review's point 9).
 *
 * The saved message keeps `{current.temp_c}`: it is what the engine interpolates and what the
 * notification prints from, and the reader's text is never rewritten. The field paints it as
 * its phrase, «la temperatura adesso», on a chip in the line — the one place the dotted ids
 * reached a screen, against the rule of CLAUDE.md. A name the registry does not know stays as
 * typed: a typo is not hidden, it is the one thing the reader must see.
 *
 * [phrase] answers a placeholder's name with its words, or null for a name it does not know.
 * The field hands the saved text in and gets the painted text and the map between the two
 * back: the cursor lives in the saved text, and the map puts it on the painted one. A cursor
 * inside a placeholder's id stands after its chip, so that a tap on the chip and a backspace
 * after it act on the chip whole ([wholeDeletion]), never on a letter of an id nobody sees.
 */
class PlaceholderTransformation(
    private val phrase: (String) -> String?,
    private val chip: SpanStyle
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val spans = PlaceholderWords.spans(text.text, phrase)
        val painted = buildAnnotatedString {
            var from = 0
            spans.forEach { span ->
                append(text.text, from, span.start)
                withStyle(chip) { append(PlaceholderWords.chipText(span.phrase)) }
                from = span.end
            }
            append(text.text, from, text.text.length)
        }
        return TransformedText(painted, PlaceholderWords.mapping(spans))
    }
}

/** A placeholder the field paints: its place in the saved text, `[start, end)`, and its words. */
class PlaceholderSpan(val start: Int, val end: Int, val phrase: String) {
    /** How many characters the chip's text has in place of the placeholder's. */
    val growth: Int get() = PlaceholderWords.chipText(phrase).length - (end - start)
}

/** The arithmetic of [PlaceholderTransformation], pure, so a test can walk the offsets. */
object PlaceholderWords {

    /** The same braces `RuleMessages` reads: a name is letters, digits, dots and underscores. */
    private val Pattern = Regex("""\{([A-Za-z0-9_.]+)\}""")

    /** A thin space each side of the words, so the chip's ground does not hug the glyphs. */
    const val Pad = " "

    fun chipText(phrase: String): String = Pad + phrase + Pad

    /** The placeholders of [text] that [phrase] knows, in order. */
    fun spans(text: String, phrase: (String) -> String?): List<PlaceholderSpan> =
        Pattern.findAll(text).mapNotNull { match ->
            phrase(match.groupValues[1])?.let { PlaceholderSpan(match.range.first, match.range.last + 1, it) }
        }.toList()

    /**
     * Saved offsets to painted ones and back. Before a chip the two texts differ by what the
     * chips before it grew; a saved offset inside a placeholder (after its `{`, up to its `}`)
     * is the chip's end, and a painted offset inside a chip is the saved offset after its `}`:
     * the id has no inside on screen.
     */
    fun mapping(spans: List<PlaceholderSpan>): OffsetMapping = object : OffsetMapping {
        override fun originalToTransformed(offset: Int): Int {
            var delta = 0
            spans.forEach { span ->
                if (offset <= span.start) return offset + delta
                if (offset <= span.end) return span.end + delta + span.growth
                delta += span.growth
            }
            return offset + delta
        }

        override fun transformedToOriginal(offset: Int): Int {
            var delta = 0
            spans.forEach { span ->
                val chipStart = span.start + delta
                val chipEnd = span.end + delta + span.growth
                if (offset <= chipStart) return offset - delta
                if (offset <= chipEnd) return span.end
                delta += span.growth
            }
            return offset - delta
        }
    }

    /**
     * A backspace on a chip takes the chip whole. The field deletes one saved character, the
     * `}` the cursor stood after; when that is the whole of the edit — [after] is [before] less
     * that one brace, the cursor at [cursor] where it was — and the brace closed a placeholder
     * [phrase] knows, the answer is the text without the placeholder and the cursor where it
     * began. Null for any other edit, which stands as the field made it.
     */
    fun wholeDeletion(before: String, after: String, cursor: Int, phrase: (String) -> String?): Pair<String, Int>? {
        if (after.length != before.length - 1 || cursor < 0 || cursor >= before.length) return null
        if (before[cursor] != '}' || before.removeRange(cursor, cursor + 1) != after) return null
        val span = spans(before, phrase).firstOrNull { it.end == cursor + 1 } ?: return null
        return before.removeRange(span.start, span.end) to span.start
    }
}
