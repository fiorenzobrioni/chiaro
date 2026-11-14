package com.callbackdev.chiaro.widget

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.callbackdev.chiaro.ui.components.LightPhase
import kotlin.math.floor

/*
 * «In parole» with its ring (committente, 14 Nov 2026): the family's ring card, drawn in
 * Chiaro's own material. The ring is the day's light: a clock of 24 hours with noon at the top
 * and midnight at the bottom, the arc of daylight from sunrise to sunset, full as far as the day
 * has gone, and the sun where it is now as an amber disc ([ArcPalette.Sun]; the moon's pale at
 * night). Inside it, the sky of the hour. It is the daylight ribbon (DESIGN §4) bent into the
 * family's circle: a depiction, not an encoding, every phase of it said in words on the Sky
 * screen and in the ring's own description.
 *
 * The card's arithmetic is the family's, constant for constant, so it stands in line with the
 * other ring cards of one size on a home screen:
 *
 * | rank | what | size | weight | ink |
 * |---|---|---|---|---|
 * | 1 | the ring, the sky inside it (one cell: the temperature) | the family's circle | – | strong, amber |
 * | 2 | the temperature | [RingHeroSp], smaller only where the width cannot hold it | Bold | strong |
 * | 3 | what it is: the place (an eyebrow on a tall card), the day's range | [RingFactSp] | Regular | quiet |
 * | 4 | the day's sentence | [RingFactSp], two lines on a tall card, three at a row's far edge | Medium | strong |
 * | 5 | under a wide card's header: the sun hour by hour | labels [RingCaptionSp] | – | – |
 *
 * Four forms, read off the grant: [RingForm.CELL] under 120 dp wide, the ring alone with the
 * temperature inside; [RingForm.ROW] one row; [RingForm.TALL] from two rows; [RingForm.PANEL] two
 * rows and from 230 dp wide, the row on top and the sun's hours under it.
 *
 * Off ([WidgetLook.showRing]), the card is the words alone it has been since 19 set 2026
 * ([TextForm]).
 */
internal enum class RingForm { CELL, ROW, TALL, PANEL }

internal fun ringForm(size: DpSize, fontScale: Float = 1f): RingForm = when {
    size.width < RingCellMaxWidth -> RingForm.CELL
    size.height < RingTallMinHeight -> RingForm.ROW
    size.width >= RingPanelMinWidth && ringPanelStrip(size, fontScale) >= RingStripMin -> RingForm.PANEL
    else -> RingForm.TALL
}

internal val RingCellMaxWidth = 120.dp
internal val RingTallMinHeight = 150.dp
internal val RingPanelMinWidth = 230.dp

// --- The family's circle ------------------------------------------------------------------------

/** The ring's side for [form]: the family's circle for the card's size. */
internal fun ringSide(form: RingForm, size: DpSize, fontScale: Float, sentence: Boolean = true): Dp = when (form) {
    RingForm.CELL -> ringCellCircle(size)
    RingForm.ROW -> ringRowCircle(size)
    RingForm.TALL -> ringTallCircle(size, fontScale, sentence)
    RingForm.PANEL -> ringTallCircle(size, fontScale)
}

/** One cell wide: the cell's shorter side, less [RingCellInset] on each side. */
internal fun ringCellCircle(size: DpSize): Dp =
    (minOf(size.width, size.height) - RingCellInset * 2).coerceIn(RingCellMin, RingTallCeiling)

/** One row: the height inside the snug insets, the words beside it keeping [RingRowWords]. */
internal fun ringRowCircle(size: DpSize): Dp = minOf(
    size.height - WidgetCardPaddingSnug * 2,
    size.width - WidgetCardPadding * 2 - RingGap - RingRowWords
).coerceIn(RingRowMin, RingRowMax)

/**
 * Two rows and up: the height inside the card over the family's budgeted band of words (a
 * figure's line and three lines of text under [RingPanelMinWidth], two from it; one where the
 * reader hid the sentence), between [RingRowMin] and the ceiling, rounded down to a quarter.
 */
internal fun ringTallCircle(size: DpSize, fontScale: Float, sentence: Boolean = true): Dp {
    val innerHeight = size.height - WidgetCardPadding * 2
    val innerWidth = size.width - WidgetCardPadding * 2
    val band = when {
        !sentence -> RingHeroSp + RingFactSp
        size.width < RingPanelMinWidth -> RingHeroSp + 3 * RingFactSp
        else -> RingHeroSp + 2 * RingFactSp
    }
    val ceiling = maxOf(RingTallCeiling, minOf(innerHeight, innerWidth) * RingTallShare)
    val side = (innerHeight - (band * RingLineBoxEm * fontScale).dp).coerceIn(RingRowMin, minOf(ceiling, innerWidth))
    return (floor(side.value * 4f) / 4f).dp
}

internal val RingCellInset = 8.dp
internal val RingCellMin = 36.dp
internal val RingRowMin = 44.dp
internal val RingRowMax = 56.dp
internal val RingTallCeiling = 104.dp
internal const val RingTallShare = 0.45f
internal val RingGap = 12.dp
internal val RingRowWords = 84.dp
internal const val RingLineBoxEm = 1.32f

// --- The words ----------------------------------------------------------------------------------

internal const val RingHeroSp = WidgetType.Hero
internal const val RingFactSp = WidgetType.Body
internal const val RingCaptionSp = WidgetType.Caption
internal const val RingTallSentenceLines = 2
internal const val RingRowSentenceLines = 3

/** A row's far edge: the sentence's column where the row is split evenly, worth a sentence from here. */
internal val RingSentenceColumnMin = 96.dp

/** The sentence's column on a row: what the words beside the ring leave, split evenly; 0 for none. */
internal fun ringRowSentenceColumn(width: Dp, ring: Dp): Dp {
    val words = width - WidgetCardPadding * 2 - ring - RingGap
    val column = (words - RingGap) / 2
    return if (column >= RingSentenceColumnMin) column else 0.dp
}

// --- The panel ----------------------------------------------------------------------------------

/** The panel's header: the tall card's circle, or the hero over its line where they are taller. */
internal fun ringPanelHeader(size: DpSize, fontScale: Float): Dp = maxOf(
    ringTallCircle(size, fontScale),
    ((RingHeroSp + RingFactSp) * RingLineBoxEm * fontScale).dp
)

/** What the sun's hours get under the header: the rest of the card, at most [RingStripMax]. */
internal fun ringPanelStrip(size: DpSize, fontScale: Float): Dp =
    (size.height - WidgetCardPadding * 2 - ringPanelHeader(size, fontScale) - RingGap).coerceAtMost(RingStripMax)

/** The hours' labels under the bars, and the air over them. */
internal fun ringStripLabels(fontScale: Float): Dp = (RingCaptionSp * RingLineBoxEm * fontScale).dp + 2.dp

internal val RingStripMin = 44.dp
internal val RingStripMax = 112.dp

// --- The day on the ring ------------------------------------------------------------------------

/**
 * Where a moment of the day stands on the ring, in degrees clockwise from the top: noon at the
 * top, midnight at the bottom, six in the morning on the left and six in the evening on the
 * right, as the sun goes round the sky. [fraction] is 0..1 from local midnight.
 */
internal fun ringAngle(fraction: Float): Float = (fraction - 0.5f) * 360f

/**
 * The stretches of the day the sun is up, as fractions from local midnight: the ribbon's
 * phases ([LightPhase]) whose light is the sun's own (an altitude over the horizon), merged where
 * they touch. Empty on a polar night; one stretch, the whole day, under the midnight sun.
 */
internal fun daylight(phases: List<LightPhase>): List<ClosedFloatingPointRange<Float>> {
    val up = phases.filter { it.sunAltitudeDeg > 0.0 }.sortedBy { it.start }
    val merged = mutableListOf<ClosedFloatingPointRange<Float>>()
    up.forEach { phase ->
        val last = merged.lastOrNull()
        if (last != null && phase.start <= last.endInclusive + 0.0001f) {
            merged[merged.lastIndex] = last.start..maxOf(last.endInclusive, phase.end)
        } else {
            merged += phase.start..phase.end
        }
    }
    return merged
}

/**
 * Where a temperature set inside a ring of side [ring] fits: as large as [RingInsideFill] of the
 * hole, never over [RingInsideMaxSp]; null under [RingInsideMinSp], which no one reads. [em] is the
 * figure's width in ems.
 */
internal fun ringInsideSp(ring: Dp, em: Float, fontScale: Float): Float? {
    val hole = ring.value * (1f - DaylightRingPainter.STROKE_SHARE * 2)
    val sp = floor(minOf(RingInsideMaxSp, hole * RingInsideFill / (em * fontScale.coerceAtLeast(0.1f))) * 4f) / 4f
    return sp.takeIf { it >= RingInsideMinSp }
}

internal const val RingInsideFill = 0.72f
internal const val RingInsideMaxSp = 28f
internal const val RingInsideMinSp = 10f

/** The sky's glyph inside a ring, as a share of its hole: a Meteocons drawing keeps a margin of its own. */
internal const val RingGlyphShare = 0.8f
