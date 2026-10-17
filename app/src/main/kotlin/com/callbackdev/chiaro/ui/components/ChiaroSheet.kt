package com.callbackdev.chiaro.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Every bottom sheet in the app: Material's, with its top always clear of the status bar and
 * its content never taking the top inset (device, 15 and 17 ott 2026).
 *
 * Material rests an open sheet at the window's height less the sheet's, and pads the content
 * by the part of the status bar the sheet is under at that moment. A sheet whose open position
 * fell inside the status bar's band (the Places sheet at 94% of the height, then the next
 * quarters' page once its notes became one line) chased itself: the spring's overshoot put it
 * under the bar, the padding grew the sheet, its open position moved up, the spring followed,
 * the padding went away and the position moved back, until a finger stopped it. Here the
 * content takes only the bottom inset, so the sheet's size never depends on where it is, and
 * its height is capped at the window less the status bar and [SheetTopGap], so it never needs
 * the top one. The content scrolls inside that cap, as every sheet's already does.
 *
 * [fillHeight] makes the sheet that tall whatever its content, for a page that grows while it
 * is open (a search answering under the field) and must not jump as it does.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChiaroSheet(
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(),
    fillHeight: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val reserve = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + SheetTopGap
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        contentWindowInsets = { WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom) }
    ) {
        Column(modifier = Modifier.sheetRoom(reserve, fillHeight), content = content)
    }
}

/** Between the status bar and the sheet's top at rest: the scrim says there is a page under it. */
private val SheetTopGap = 12.dp

/**
 * The content at most as tall as the room it is given less [reserve] (exactly that tall with
 * [fill]): what the window keeps above the sheet, independent of the sheet's own padding, so
 * the sheet's height is the same wherever it stands while it moves.
 */
private fun Modifier.sheetRoom(reserve: Dp, fill: Boolean): Modifier = layout { measurable, constraints ->
    val room = (constraints.maxHeight - reserve.roundToPx()).coerceAtLeast(0)
    val placeable = measurable.measure(
        constraints.copy(minHeight = if (fill) room else 0, maxHeight = room)
    )
    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
}
