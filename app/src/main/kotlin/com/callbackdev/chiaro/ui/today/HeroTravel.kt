package com.callbackdev.chiaro.ui.today

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlin.math.roundToInt

/**
 * Where the hero's number stands at rest and how big it is (DESIGN §8.1e, 15 ott 2026): the
 * canvas measures it, the bar and the travelling number read it. The rest position is the
 * number's top-left inside the canvas item, which the scroll never changes; where that item
 * is now is the list's to say, so the number's place on screen is arithmetic on the scroll
 * and nothing is measured while the page moves.
 */
@Stable
internal class HeroDock {
    var rest by mutableStateOf(Offset.Unspecified)
    var size by mutableStateOf(IntSize.Zero)

    val measured: Boolean get() = rest != Offset.Unspecified && size != IntSize.Zero
}

/** Where the number is drawn, how large, and how far it has travelled (0 at rest, 1 docked). */
internal data class HeroPose(val x: Float, val y: Float, val scale: Float, val progress: Float)

/**
 * The number's pose for a scroll (pure, `HeroTravelTest`). [itemTop] is the canvas item's top
 * in the page, null once the item has left the list's window. The number rides the sky like
 * the rest of the canvas until its top reaches [trigger] (a little under the bar); over the
 * next [travel] pixels of scroll it shrinks to [endScale] and slides to [dock], the bar's slot
 * for it; past that it stays docked. Every step is a function of the scroll, so the page can
 * be dragged back and the number comes back down with it, at the finger's own speed.
 */
internal fun heroPose(
    itemTop: Float?,
    rest: Offset,
    dock: Offset,
    trigger: Float,
    travel: Float,
    endScale: Float
): HeroPose {
    if (itemTop == null) return HeroPose(dock.x, dock.y, endScale, 1f)
    val free = itemTop + rest.y
    val progress = ((trigger - free) / travel).coerceIn(0f, 1f)
    if (progress == 0f) return HeroPose(rest.x, free, 1f, 0f)
    return HeroPose(
        x = lerp(rest.x, dock.x, progress),
        y = lerp(trigger, dock.y, progress),
        scale = lerp(1f, endScale, progress),
        progress = progress
    )
}

/**
 * The bar's slot for the docked number, in the page's coordinates: its right edge just before
 * the gear's 48dp box and the bar's 16dp end padding, centred on the bar's row (which centres
 * every child, so its middle is halfway between the status bar's bottom and the bar's).
 */
internal fun dockSlot(
    pageWidth: Float,
    statusBottom: Float,
    barBottom: Float,
    numberWidth: Float,
    numberHeight: Float,
    endScale: Float,
    endInset: Float
): Offset = Offset(
    x = pageWidth - endInset - numberWidth * endScale,
    y = (statusBottom + barBottom) / 2f - numberHeight * endScale / 2f
)

/** The canvas item's top in the page, or null once it has scrolled out of the list. */
internal fun LazyListState.canvasTop(): Float? =
    if (firstVisibleItemIndex == 0) -firstVisibleItemScrollOffset.toFloat() else null

/**
 * The hero's number, drawn once for the whole page above the list and the bar (DESIGN §8.1e).
 * At rest it stands exactly where the canvas keeps an invisible copy of it, which is also the
 * one TalkBack reads; scrolled, it travels into the bar ([heroPose]). The part of it inside
 * the bar is drawn in the bar's ink and the part still over the sky in the sky's white, so the
 * figure crossing the bar's edge never stands a moment on a ground it does not read on.
 */
@Composable
internal fun TravellingNumber(
    text: String,
    style: TextStyle,
    pose: () -> HeroPose,
    /** Where the bar ends, in the page: above it the bar's ink, below it the sky's. */
    barBottom: () -> Float,
    barInk: Color,
    skyInk: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .offset {
                val p = pose()
                IntOffset(p.x.roundToInt(), p.y.roundToInt())
            }
            .graphicsLayer {
                val p = pose()
                scaleX = p.scale
                scaleY = p.scale
                transformOrigin = TransformOrigin(0f, 0f)
            }
            .clearAndSetSemantics { }
    ) {
        // The cut, in the number's own unscaled coordinates.
        fun cut(): Float {
            val p = pose()
            return (barBottom() - p.y) / p.scale
        }
        Text(
            text = text,
            style = style,
            color = barInk,
            modifier = Modifier.drawWithContent {
                clipRect(bottom = cut().coerceIn(0f, size.height)) { this@drawWithContent.drawContent() }
            }
        )
        Text(
            text = text,
            style = style,
            color = skyInk,
            modifier = Modifier.drawWithContent {
                clipRect(top = cut().coerceIn(0f, size.height)) { this@drawWithContent.drawContent() }
            }
        )
    }
}

/** Under the bar by this much the number starts to travel. */
internal val HeroTrigger: Dp = 8.dp

/** How much scroll the journey into the bar takes: about two lines of the page. */
internal val HeroTravelDistance: Dp = 96.dp
