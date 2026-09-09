package com.callbackdev.chiaro.widget.arc

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.callbackdev.chiaro.domain.warnings.WarningLevel
import com.callbackdev.chiaro.widget.StaleSp
import com.callbackdev.chiaro.widget.WarningChipGap
import com.callbackdev.chiaro.widget.WarningSlot
import com.callbackdev.chiaro.widget.textLineHeight
import com.callbackdev.chiaro.widget.warningChipHeight
import com.callbackdev.chiaro.widget.warningSlot

/**
 * The five forms of the day's arc, and the grant that picks one. The widget goes from
 * one cell to sixteen, and every step of the way it shows what fits rather than a
 * smaller copy of the same thing — the launcher's own grammar, which the other three
 * widgets adopted on 8 set 2026 and this one is born with.
 *
 * - [DIAL] is the one-cell card: the arc alone, and one figure under it.
 * - [STRIP] is one row at two cells or more: the words and the arc side by side — or,
 *   at two cells, the words over the arc, because 47 dp is not a width to draw a day in.
 * - [CARD] is two cells wide and two or more tall: temperature, place, the next moment,
 *   the arc, and under it as many agenda rows as the height honestly holds.
 * - [PANEL] is three or four cells wide and two tall: the hero row (the number, the
 *   sentence, the place and the countdown on one line), the arc with its hours, and a
 *   row or two of agenda.
 * - [BOARD] is three or four wide and three or four tall: the same, with a bigger arc,
 *   more agenda, and on four rows the week at the foot.
 *
 * Everything here is arithmetic on dp so it can be pinned by a table (`ArcLayoutTest`)
 * rather than by a screenshot of one launcher's idea of a cell.
 */
internal enum class ArcForm { DIAL, STRIP, CARD, PANEL, BOARD }

/**
 * How many launcher columns a width most likely is. One cell lands between ~70 and
 * ~101 dp depending on the grid; two between ~150 and ~180; three ~230-260; four
 * ~320-360. The thresholds sit in the gaps.
 */
internal fun arcColumns(width: Dp): Int = when {
    width < 120.dp -> 1
    width < 210.dp -> 2
    width < 300.dp -> 3
    else -> 4
}

/** The same for rows: one row is ~82-101 dp, two ~189, three ~290, four ~390. */
internal fun arcRows(height: Dp): Int = when {
    height < 150.dp -> 1
    height < 245.dp -> 2
    height < 340.dp -> 3
    else -> 4
}

internal fun arcForm(size: DpSize): ArcForm {
    val columns = arcColumns(size.width)
    val rows = arcRows(size.height)
    return when {
        columns == 1 -> ArcForm.DIAL
        rows == 1 -> ArcForm.STRIP
        columns == 2 -> ArcForm.CARD
        rows == 2 -> ArcForm.PANEL
        else -> ArcForm.BOARD
    }
}

/** The whole card, decided: what is drawn, how big, and how many of each. */
internal data class ArcPlan(
    val form: ArcForm,
    val columns: Int,
    val rows: Int,
    /** A [ArcForm.STRIP] at two cells stacks its words over the arc. */
    val stacked: Boolean,
    val paddingHorizontal: Dp,
    val paddingVertical: Dp,
    /** The arc's own box, hour labels included. */
    val graphic: DpSize,
    /** Whether the plot prints the hours under itself, and their temperatures. */
    val hourLabels: Boolean,
    val temperatures: Boolean,
    /** The hours between two labels, the plot's preferred value; the painter may widen
     * it when the labels it measures would collide. */
    val tickStepHours: Int,
    /** How many lines the hero sentence may take in the header; on a one-row card, how
     * many lines of words sit under the temperature (the name, then its clock). */
    val heroLines: Int,
    val agendaRows: Int,
    val week: Boolean,
    /**
     * Where the official warning's chip goes on this card (Fase 11). Only the card and
     * the panel ever carry one: the dial has one figure and the strip one line, and a
     * chip on either would be the card's only subject.
     */
    val warning: WarningSlot,
    /** [ArcSettings.textScale], carried so every text on the card reads it from one place. */
    val textScale: Float
)

/**
 * The plan for one grant. [agendaAvailable] is how many agenda rows there are to draw —
 * the plan never reserves for a row that does not exist, because a padded list is the
 * one thing this widget must not show. [weekAvailable] is whether the report has a week.
 */
internal fun arcPlan(
    size: DpSize,
    fontScale: Float,
    settings: ArcSettings,
    stale: Boolean,
    agendaAvailable: Int,
    weekAvailable: Boolean,
    /**
     * Whether the header's sentence is the day's headline rather than the next light
     * moment. It is a parameter and not `settings.hero == HEADLINE` because the card
     * falls back to the headline when there is no moment left ([heroIsHeadline]), and
     * the warning's rule turns on what the slot is REALLY printing.
     */
    heroIsHeadline: Boolean = false,
    /** What the bulletin grades this place at, or null when there is nothing to draw. */
    warningLevel: WarningLevel? = null
): ArcPlan {
    val form = arcForm(size)
    val columns = arcColumns(size.width)
    val rows = arcRows(size.height)
    val scale = settings.textScale
    fun line(sp: Float): Dp = textLineHeight(sp * scale, fontScale)

    return when (form) {
        ArcForm.DIAL -> {
            val innerW = size.width - ArcPadTight * 2
            val innerH = size.height - ArcPadTight * 2
            var words = line(DialFigureSp)
            if (stale) words += line(StaleSp)
            if (rows >= 2) words += line(PlaceSp) + line(StripLineSp)
            val available = innerH - words - ArcGapTight
            val height = minOf(available, innerW * DialAspect).coerceAtLeast(DialGraphicMin)
            ArcPlan(
                form, columns, rows, stacked = false,
                paddingHorizontal = ArcPadTight, paddingVertical = ArcPadTight,
                graphic = DpSize(innerW, height),
                hourLabels = false, temperatures = false, tickStepHours = 0,
                heroLines = 0, agendaRows = 0, week = false,
                warning = WarningSlot.NONE, textScale = scale
            )
        }
        ArcForm.STRIP -> {
            val innerW = size.width - ArcPadTight * 2
            val innerH = size.height - ArcPadTight * 2
            val stacked = columns == 2
            val graphic = if (stacked) {
                DpSize(innerW, innerH - line(StripStackedTempSp) - ArcGapTight)
            } else {
                DpSize(innerW - StripTextColumn - StripGraphicGap, innerH)
            }
            val labels = settings.hourLabels && graphic.height >= HourLabelsMinHeight
            // Beside the arc the words get the moment's name and, when the height holds a
            // third line, its clock and countdown under it (62 − 29.04 = 32.96 ≥ 2 × 15.84
            // at the default font size; at 1.3 it does not, and the two join on one line).
            val wordLines = if (!stacked && innerH - line(StripTempSp) >= line(StripLineSp) * 2) 2 else 1
            ArcPlan(
                form, columns, rows, stacked,
                paddingHorizontal = ArcPadTight, paddingVertical = ArcPadTight,
                graphic = graphic,
                hourLabels = labels,
                temperatures = labels && settings.temperatures &&
                    graphic.height >= TemperaturesMinHeight,
                tickStepHours = arcTickStep(graphic.width),
                heroLines = wordLines, agendaRows = 0, week = false,
                warning = WarningSlot.NONE, textScale = scale
            )
        }
        ArcForm.CARD, ArcForm.PANEL, ArcForm.BOARD -> {
            val innerW = size.width - ArcPad * 2
            val innerH = size.height - ArcPad * 2
            val wantedHeroLines: Int
            if (form == ArcForm.CARD) {
                // Number over place over the sentence, all against the leading edge.
                wantedHeroLines = if (settings.hero == ArcHero.NONE) 0 else CardHeroMaxLines
            } else {
                // One row: the number, then the sentence with the place and the countdown
                // under it. The sentence takes two lines where its column is narrow.
                val heroColumn = innerW - TemperatureColumn - HeroGap
                wantedHeroLines = when {
                    settings.hero == ArcHero.NONE -> 0
                    heroColumn < HeroOneLineMin -> 2
                    else -> 1
                }
            }
            // The warning (Fase 11). The arc's sentence slot is not always the day's
            // headline — a reader who left the hero on the next light moment has a card
            // that says nothing about a warning — so `headlineShown` asks what the slot
            // is REALLY printing, not merely whether a slot exists.
            var warning = warningSlot(
                level = warningLevel,
                enabled = settings.warning,
                headlineShown = heroIsHeadline && wantedHeroLines > 0,
                sentenceSlot = wantedHeroLines > 0,
                ownRow = true
            )
            fun headerFor(slot: WarningSlot): Pair<Int, Dp> {
                val lines = if (slot == WarningSlot.SENTENCE) 0 else wantedHeroLines
                val chip = if (slot.drawn) warningChipHeight(fontScale) + WarningChipGap else 0.dp
                val height = if (form == ArcForm.CARD) {
                    line(CardTempSp) + line(PlaceSp) + line(CardHeroSp) * lines +
                        (if (stale) line(StaleSp) else 0.dp) + chip
                } else {
                    maxOf(line(PanelTempSp), line(HeroSp) * lines + line(HeroSubSp)) + chip
                }
                return lines to height
            }
            var (heroLines, header) = headerFor(warning)
            // The chip yields to the drawing, not the other way round: a card whose arc
            // would fall under its floor keeps the arc and drops the chip.
            if (warning.drawn && innerH - header - ArcGap < GraphicFloor) {
                warning = WarningSlot.NONE
                val fallback = headerFor(warning)
                heroLines = fallback.first
                header = fallback.second
            }
            val afterHeader = innerH - header - ArcGap
            val week = form == ArcForm.BOARD && rows >= 4 && settings.week && weekAvailable
            val room = afterHeader - (if (week) arcWeekHeight(fontScale, scale) + ArcGap else 0.dp)
            val preferred = when (form) {
                ArcForm.CARD -> GraphicPreferredCard
                ArcForm.PANEL -> GraphicPreferredPanel
                else -> GraphicPreferredBoard
            }
            val maxGraphic = when (form) {
                ArcForm.CARD -> GraphicMaxCard
                ArcForm.PANEL -> GraphicMaxPanel
                else -> GraphicMaxBoard
            }
            val rowHeight = arcAgendaRowHeight(fontScale, scale)
            val cap = if (form == ArcForm.PANEL) PanelAgendaMaxRows else AgendaMaxRows
            val fit = ((room - preferred - ArcGap + ArcRowGap) / (rowHeight + ArcRowGap)).toInt()
            val agendaRows = fit.coerceIn(0, minOf(cap, agendaAvailable.coerceAtLeast(0)))
            val agenda = if (agendaRows > 0) {
                ArcGap + rowHeight * agendaRows + ArcRowGap * (agendaRows - 1)
            } else {
                0.dp
            }
            val graphicHeight = (room - agenda).coerceIn(GraphicFloor, maxGraphic)
            val graphic = DpSize(innerW, graphicHeight)
            val labels = settings.hourLabels && graphicHeight >= HourLabelsMinHeight
            ArcPlan(
                form, columns, rows, stacked = false,
                paddingHorizontal = ArcPad, paddingVertical = ArcPad,
                graphic = graphic,
                hourLabels = labels,
                temperatures = labels && settings.temperatures &&
                    graphicHeight >= TemperaturesMinHeight,
                tickStepHours = arcTickStep(innerW),
                heroLines = heroLines,
                agendaRows = agendaRows,
                week = week,
                warning = warning,
                textScale = scale
            )
        }
    }
}

/**
 * The plot's preferred spacing of hour labels, by the width it has: every three hours
 * on a four-cell card (eight labels, ~39 dp apart on the reference grid), every four on
 * three cells (six, ~37 apart), every six below that. A preference, not a promise — the
 * painter measures the labels the locale really writes («12 PM» is twice «12») and
 * widens the step until none collide.
 */
internal fun arcTickStep(width: Dp): Int = when {
    width >= 280.dp -> 3
    width >= 180.dp -> 4
    else -> 6
}

/** An agenda row: its 13 sp line and 3 dp of air above and below; the 18 dp glyph
 * fits inside it at every scale the card offers. */
internal fun arcAgendaRowHeight(fontScale: Float, textScale: Float): Dp =
    textLineHeight(AgendaSp * textScale, fontScale) + AgendaRowAir * 2

/** The week's strip: the day's name, the glyph, the high and the low, stacked. */
internal fun arcWeekHeight(fontScale: Float, textScale: Float): Dp =
    textLineHeight(WeekDaySp * textScale, fontScale) + WeekIcon +
        textLineHeight(WeekHighSp * textScale, fontScale) +
        textLineHeight(WeekLowSp * textScale, fontScale) + WeekInnerGap * 2

/** The card's insets: the words' 14 on the four-cell forms, 10 on the small ones. */
internal val ArcPad = 14.dp
internal val ArcPadTight = 10.dp

/** The air between a card's sections, and between two agenda rows. */
internal val ArcGap = 6.dp
internal val ArcGapTight = 4.dp
internal val ArcRowGap = 4.dp

/** The one-cell arc is wider than tall — an arch, not a square. The floor is what a
 * stale one-cell card leaves it (62 − 29 − 14.5 − 4 = 14.4 dp): the marker that says
 * the data is old outranks the drawing, and a 14 dp arch is still an arch. */
internal const val DialAspect = 0.62f
internal val DialGraphicMin = 12.dp

/** The words' column of a one-row card beside its arc: «−12°» at 22 sp Medium is
 * ~47 dp, and the longest moment's name in Italian, «Tramonta la luna», ~100 dp at 12 sp
 * (device report, 9 set 2026: the first cut was 100, and «Tramonta la luna · 19:00» did
 * not fit it; the name now has a line of its own and the clock the next). */
internal val StripTextColumn = 110.dp
internal val StripGraphicGap = 8.dp

/** What the number needs beside the hero sentence on a panel: «−12°» at 30 sp Medium
 * is ~62 dp. */
internal val TemperatureColumn = 68.dp
internal val HeroGap = 8.dp

/** Below this width the hero sentence gets two lines: «Golden hour ends at 09:15» at
 * 15 sp Medium is ~165 dp, and a column that cannot hold that on one line holds it on
 * two rather than cutting it. */
internal val HeroOneLineMin = 190.dp

/** On a two-cell card the sentence wraps to two lines: at 13 sp its column is 131 dp. */
internal const val CardHeroMaxLines = 2

/** Under this height the plot has no honest room for a row of hour labels (the plot
 * itself would be under 38 dp); under the second, not for a row of temperatures too. */
internal val HourLabelsMinHeight = 52.dp
internal val TemperaturesMinHeight = 76.dp

/**
 * How tall the arc would like to be before it starts paying for agenda rows, per form,
 * and the most it will ever take. The panel prefers rows (a two-row card is the size
 * most readers place, and two moments under a 59 dp arc beat one under an 86 dp one);
 * the board prefers the arc (at three rows and up there is room for both).
 */
internal val GraphicPreferredCard = 72.dp
internal val GraphicPreferredPanel = 56.dp
internal val GraphicPreferredBoard = 110.dp
internal val GraphicMaxCard = 100.dp
internal val GraphicMaxPanel = 96.dp
internal val GraphicMaxBoard = 150.dp
internal val GraphicFloor = 24.dp

internal const val AgendaMaxRows = 6
internal const val PanelAgendaMaxRows = 2

// Text sizes. The panel's number is the Sky widget's 30, one step under the Now
// widget's 34: it shares its row with a sentence. The card's is 28, the strip's 24, the
// one-cell figure 22 — each the largest that leaves its words their column (measured
// with the system font, see ArcLayoutTest for the widths).
internal const val DialFigureSp = 22f
/** 22 since the device pass of 9 set 2026 (from 24): the number gives its two points to
 * the third line of words, the clock under the moment's name. */
internal const val StripTempSp = 22f
internal const val StripStackedTempSp = 20f
internal const val StripLineSp = 12f
internal const val CardTempSp = 28f
internal const val PanelTempSp = 30f
internal const val PlaceSp = 13f
internal const val HeroSp = 15f
internal const val HeroSubSp = 12f
internal const val CardHeroSp = 13f
internal const val AgendaSp = 13f
internal const val TickSp = 11f
internal const val WeekDaySp = 11f
internal const val WeekHighSp = 12f
internal const val WeekLowSp = 11f

/**
 * The verdict mark's column at the end of an agenda row: the glyph alone («✓ ~ ✗ ?») in the
 * verdict's ink, set at the row's own size. 14 dp holds the widest of the four at 13 sp
 * Medium with air either side. The Sky widget's filled pill was tried here first and came
 * back from the device as «a punch in the eye» (9 set 2026): the pill is the Sky card's
 * hero, and a list row wants a note, not a hero.
 */
internal val AgendaMarkSlot = 14.dp

/** The mark's drawing inside its slot: 12 dp is the x-height of the 13 sp words beside
 * it, so the cross stands in the line like a letter and not like an icon. */
internal val AgendaMarkGlyph = 12.dp
internal val AgendaRowAir = 3.dp
/** The Sky widget's row glyph, so the two cards' lists read as siblings; 20 since the
 * device pass of 9 set 2026 (from 18). */
internal val AgendaGlyph = 20.dp
internal val WeekIcon = 22.dp
internal val WeekInnerGap = 2.dp
