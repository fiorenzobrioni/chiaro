package com.callbackdev.chiaro.widget

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.callbackdev.chiaro.domain.warnings.WarningLevel

/**
 * Where an official warning goes on a home-screen card, and what it costs the card
 * (Fase 11, fourth step). Pure arithmetic and one decision table, so `WidgetWarningTest`
 * pins it rather than a screenshot of one launcher's idea of a cell — the same rule the
 * four widgets' layouts already live by.
 *
 * The widgets of the system Chiaro sits beside show a line when the national service
 * issues a warning; this is that habit, not an invention. What decides is what the card
 * is ALREADY saying:
 *
 * - **Orange and red** are already in the day's sentence ([HeadlineText]'s brief
 *   register: «Allerta arancione · temporali»). Where that sentence is on the card there
 *   is no chip — it would be the same thing said twice. Where it is not (the reader
 *   turned it off, or this card's sentence slot is showing something else, as the arc's
 *   can) the chip takes the sentence's place, so the warning reaches the card anyway and
 *   the card grows by nothing.
 * - **Yellow** is never in the sentence — in an Italian autumn yellow is frequent, and a
 *   line that repeats stops being read (the same reason Today's headline does not carry
 *   it) — so it needs a line of its own, and it only appears where the form has room for
 *   one more.
 */
enum class WarningSlot {
    /** Nothing is drawn: no warning, the switch is off, or the sentence already says it. */
    NONE,

    /** The chip stands where the sentence would have been: costs the card nothing. */
    SENTENCE,

    /** The chip has a line of its own: costs the card [warningChipHeight]. */
    OWN_ROW;

    val drawn: Boolean get() = this != NONE
}

/**
 * @param level what the bulletin grades this place at, null when there is none
 * @param enabled the per-instance switch ([WidgetLook.showWarning], `ArcSettings.warning`)
 * @param headlineShown whether a slot on this card is right now printing the day's
 *   headline — which for orange and red IS the warning. False on a card whose sentence
 *   slot carries something else (the arc's next moment) or is not drawn at all.
 * @param sentenceSlot whether this form HAS a sentence slot the chip could stand in
 * @param ownRow whether the form has room for one line more than it is already using
 */
fun warningSlot(
    level: WarningLevel?,
    enabled: Boolean,
    headlineShown: Boolean,
    sentenceSlot: Boolean,
    ownRow: Boolean
): WarningSlot {
    if (!enabled || level == null || level == WarningLevel.NONE) return WarningSlot.NONE
    if (level >= WarningLevel.ORANGE) {
        return when {
            headlineShown -> WarningSlot.NONE
            sentenceSlot -> WarningSlot.SENTENCE
            ownRow -> WarningSlot.OWN_ROW
            else -> WarningSlot.NONE
        }
    }
    // Yellow, which no sentence ever carries.
    return if (ownRow) WarningSlot.OWN_ROW else WarningSlot.NONE
}

/**
 * The chip's box: the mark or the word, whichever is taller, plus its own padding. The
 * one number every budget that makes room for a chip subtracts, so the four cards can
 * never disagree about what a warning costs them.
 */
fun warningChipHeight(fontScale: Float): Dp =
    maxOf(WarningChipGlyph, textLineHeight(WarningChipSp, fontScale)) + WarningChipPadV * 2

/** `ic_warning` at the size the Sky card's mark is drawn beside 11 sp words. */
val WarningChipGlyph = 12.dp

/** The word beside it, and the corner and padding of the Sky widget's own chip
 * (DESIGN §8.13: the `VerdictChip` grammar at the size a card can afford). */
const val WarningChipSp = 11f
val WarningChipCorner = 10.dp
val WarningChipPadH = 7.dp
val WarningChipPadV = 3.dp

/** The air between the chip and whatever it sits under. */
val WarningChipGap = 4.dp
