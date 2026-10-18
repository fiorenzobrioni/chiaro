package com.callbackdev.chiaro.ui.today

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize

/**
 * Where the hero's number stands at rest and how big it is (DESIGN §8.1e): the canvas measures
 * it once, and the bar reads it to know when the number has gone under it. The rest position is
 * the number's top-left inside the canvas item, which the scroll never changes; where that item
 * is now is the list's to say, so the question is arithmetic on the scroll and nothing is
 * measured while the page moves.
 */
@Stable
internal class HeroFigure {
    var rest by mutableStateOf(Offset.Unspecified)
    var size by mutableStateOf(IntSize.Zero)

    val measured: Boolean get() = rest != Offset.Unspecified && size != IntSize.Zero
}

/**
 * Whether the hero's number has gone under the bar (pure, `HeroInBarTest`): its middle has
 * climbed above the bar's bottom edge, or the canvas has left the list. [itemTop] is the
 * canvas item's top in the page, null once it is out of the list's window.
 *
 * One line and two states (committente, 18 ott 2026). Until that day the number travelled
 * into the bar a pixel at a time with the scroll, and a page left a little scrolled kept it
 * halfway, half-shrunk across the bar's edge: the state the reader saw most, since the 96dp of
 * the journey were exactly where one stops to read the range under the number. Now the number
 * rides the sky like everything else on it, and the bar's own icon and number come in together,
 * on a timed transition, the moment it crosses this line.
 */
internal fun heroUnderBar(itemTop: Float?, rest: Offset, size: IntSize, barBottom: Float): Boolean =
    itemTop == null || itemTop + rest.y + size.height / 2f < barBottom

/** The canvas item's top in the page, or null once it has scrolled out of the list. */
internal fun LazyListState.canvasTop(): Float? =
    if (firstVisibleItemIndex == 0) -firstVisibleItemScrollOffset.toFloat() else null
