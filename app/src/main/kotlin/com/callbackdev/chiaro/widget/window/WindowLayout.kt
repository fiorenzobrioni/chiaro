package com.callbackdev.chiaro.widget.window

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.callbackdev.chiaro.widget.SkyCompactSp
import com.callbackdev.chiaro.widget.SkyRowGap
import com.callbackdev.chiaro.widget.SkyRowHeight
import com.callbackdev.chiaro.widget.WidgetType
import com.callbackdev.chiaro.widget.textLineHeight

/**
 * The four forms of «The window» (20 ott 2026), and the grant that picks one. The card is
 * the sky of this hour, as the hero draws it, with the place and one sentence at its foot;
 * what the grant adds or takes away is around that picture, never a smaller copy of it.
 *
 * - [STRIP] is one row: the sky as a strip, the place and the sentence over it. No names
 *   on the sky, no compass: 82 dp holds the picture and two lines, nothing else.
 * - [NARROW] is two cells wide and two rows or more: the window with its caption; the
 *   names stay, the compass goes, because «Milan» and «Looking SE» do not share 159 dp.
 * - [WINDOW] is three or four cells wide and two rows tall: the card the picker places.
 *   The place and where the window looks on one line, the sentence under them, the
 *   brightest things named where they stand.
 * - [TALL] is three or four wide and three or four tall: the window, and under it the
 *   moments the reader follows with their verdicts, as many as the height honestly holds
 *   and never more than there are; the sky keeps at least two rows' worth of height.
 *
 * Arithmetic on dp, pinned by `WindowLayoutTest` rather than by a screenshot of one
 * launcher's idea of a cell.
 */
internal enum class WindowForm { STRIP, NARROW, WINDOW, TALL }

/** How many launcher columns a width most likely is: the thresholds sit in the gaps between
 * the grids' cells (one ~70-101 dp, two ~150-180, three ~230-260, four ~320-360). */
internal fun windowColumns(width: Dp): Int = when {
    width < 120.dp -> 1
    width < 210.dp -> 2
    width < 300.dp -> 3
    else -> 4
}

/** The same for rows: one row is ~82-101 dp, two ~189, three ~290, four ~390. */
internal fun windowRows(height: Dp): Int = when {
    height < 150.dp -> 1
    height < 245.dp -> 2
    height < 340.dp -> 3
    else -> 4
}

internal fun windowForm(size: DpSize): WindowForm {
    val columns = windowColumns(size.width)
    val rows = windowRows(size.height)
    return when {
        rows == 1 -> WindowForm.STRIP
        columns <= 2 -> WindowForm.NARROW
        rows == 2 -> WindowForm.WINDOW
        else -> WindowForm.TALL
    }
}

/** The whole card, decided: how tall the picture is, how many rows sit under it, what
 * the caption carries. */
internal data class WindowPlan(
    val form: WindowForm,
    /** The picture's height: the whole card, or what the rows under it leave. */
    val skyHeight: Dp,
    /** The moments the reader follows, drawn under the picture; 0 on every form but [WindowForm.TALL]. */
    val momentRows: Int,
    /** The names of the brightest things in the window, written where they stand. */
    val labels: Boolean,
    /** «Looking S» at the caption's right. */
    val compass: Boolean,
    /** The official warning's chip, over the top of the picture (Fase 11). */
    val warning: Boolean,
    /** The lines the sentence may take. */
    val captionLines: Int
)

/**
 * The plan for one grant. [momentsAvailable] is how many moments there are to draw: the
 * plan never reserves a row for one that does not exist, because a padded list is the one
 * thing a widget of this family must not show. [warning] is whether there is an official
 * warning to carry and the reader has not turned the chip off.
 */
internal fun windowPlan(size: DpSize, fontScale: Float, momentsAvailable: Int, warning: Boolean): WindowPlan {
    val form = windowForm(size)
    val rowHeight = windowRowHeight(fontScale)
    val rows = if (form == WindowForm.TALL) {
        ((size.height - WindowSkyMin - WindowRowsPadding) / rowHeight).toInt()
            .coerceIn(0, momentsAvailable.coerceAtLeast(0))
    } else {
        0
    }
    val skyHeight = if (rows > 0) size.height - rowHeight * rows - WindowRowsPadding else size.height
    val wide = form == WindowForm.WINDOW || form == WindowForm.TALL
    return WindowPlan(
        form = form,
        skyHeight = skyHeight,
        momentRows = rows,
        labels = form != WindowForm.STRIP,
        compass = wide,
        warning = warning && wide,
        captionLines = if (form == WindowForm.STRIP) 1 else 2
    )
}

/** A row of the moments and the air above it: the Sky card's own row, so the two lists agree. */
internal fun windowRowHeight(fontScale: Float): Dp =
    maxOf(SkyRowHeight, textLineHeight(SkyCompactSp, fontScale)) + SkyRowGap

/** The picture never shorter than two rows of launcher: under that the stars are a strip. */
internal val WindowSkyMin = 189.dp

/** The air between the picture and the first row of moments, and under the last. */
internal val WindowRowsPadding = 10.dp

/** The caption's inset from the card's edges: the hero's own 16 dp. */
internal val WindowCaptionPadding = 16.dp

/** The place: the second thing read, in the family's Body step. */
internal const val WindowPlaceSp = WidgetType.Body

/** The sentence: a row of a list, Label. */
internal const val WindowSentenceSp = WidgetType.Label

/** Where the window looks, and the stale line: read last, Caption. */
internal const val WindowCaptionSp = WidgetType.Caption

/** The names written on the sky, in the picture itself: Caption, scaled by the reader's font size. */
internal const val WindowLabelSp = WidgetType.Caption
