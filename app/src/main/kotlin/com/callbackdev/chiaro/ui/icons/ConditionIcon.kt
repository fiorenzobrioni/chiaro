package com.callbackdev.chiaro.ui.icons

import android.content.Context
import android.graphics.drawable.AnimatedVectorDrawable
import android.view.View
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.viewinterop.AndroidView
import com.callbackdev.chiaro.ui.theme.LocalAppPalette
import com.callbackdev.chiaro.ui.theme.reducedMotion

/**
 * A weather condition as **data**, not as a drawing.
 *
 * The screens used to build an `ImageVector` where they built their row, which meant the
 * drawing was chosen before anything knew whether it was allowed to move. Keeping the
 * code and the hour of day instead lets [ConditionIcon] decide at the point of drawing,
 * where the style, the ground, the palette and the reader's motion setting are all in
 * scope at once.
 */
@Immutable
data class ConditionGlyph(val wmoCode: Int, val night: Boolean = false)

/**
 * Whether the weather is allowed to move (DESIGN.md §7.1), provided by `MainActivity`
 * from the reader's settings alongside the icon style.
 *
 * It is only half the answer: the system's own "remove animations" is the other half and
 * always wins ([reducedMotion]). Default false so a composable outside the app's theme —
 * a preview, a test — draws the still glyph.
 */
val LocalAnimatedIcons = staticCompositionLocalOf { false }

/**
 * The page is moving, so the weather holds still (DESIGN.md §7.1, 9 set 2026).
 *
 * Provided true by whatever is scrolling — Today's list, the hour strip's row, the pager
 * between places — for as long as the scroll is in progress. While it is true a moving
 * icon shows its still drawing and its animated twin is not drawn at all, which is the
 * one way to take the loops off the RenderThread while that thread is busy moving the
 * layers of the scroll (see [MovingIconView.setPaused] for why it cannot simply be
 * paused). At rest the loop reappears where the clock puts it. Default false.
 */
val LocalMotionPaused = staticCompositionLocalOf { false }

/**
 * The weather, drawn: the still glyph, or the illustrator's own motion where the family
 * has it and the reader has asked for it.
 *
 * The four conditions for moving, all of which must hold:
 *
 * 1. the reader turned it on ([LocalAnimatedIcons]);
 * 2. the system is not asking for less motion ([reducedMotion] — §7, and the same
 *    `ANIMATOR_DURATION_SCALE` every other animation in the app reads);
 * 3. this family HAS a moving sibling (`ChiaroIcons.movingRes`; the metric marks and
 *    "not available" do not, and never silently pretend to);
 * 4. this is not a preview, where an `AndroidView` renders as nothing at all.
 *
 * Nothing here gates information: the still and the moving drawings are the same
 * drawing, and a reader with motion off sees the same weather at the same moment (§7).
 *
 * A moving icon is drawn as two things in one box: the still drawing, and the moving
 * twin over it. Both are always composed — composing fourteen painters at the first
 * frame of a scroll would be the hitch this exists to remove — and [LocalMotionPaused]
 * says which one shows: the still one at alpha 1 with the twin hidden while the page
 * moves, the twin over a transparent still one at rest.
 */
@Composable
fun ConditionIcon(glyph: ConditionGlyph, modifier: Modifier = Modifier) {
    val style = LocalWeatherIcons.current
    val palette = LocalAppPalette.current
    // The ground the icon is about to sit on: the APPLIED theme's surface, read off the
    // scheme itself — the same question `ChiaroIcons` asks for the static sets.
    val darkGround = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val lineRes = ChiaroIcons.conditionLineRes(glyph.wmoCode, glyph.night)
    val moving = if (LocalAnimatedIcons.current && !reducedMotion() && !LocalInspectionMode.current) {
        ChiaroIcons.movingRes(lineRes, style, darkGround, palette)
    } else {
        null
    }
    val still = ImageVector.vectorResource(ChiaroIcons.styledRes(lineRes, style, darkGround, palette))
    if (moving != null) {
        val paused = LocalMotionPaused.current
        Box(modifier = modifier) {
            Icon(
                imageVector = still,
                contentDescription = null, // the row it sits in speaks once, via its semantics
                tint = Color.Unspecified, // Meteocons carry their own measured colors
                modifier = Modifier.matchParentSize().alpha(if (paused) 1f else 0f)
            )
            MovingIcon(moving, paused, Modifier.matchParentSize())
        }
    } else {
        Icon(
            imageVector = still,
            contentDescription = null, // the row it sits in speaks once, via its semantics
            tint = Color.Unspecified, // Meteocons carry their own measured colors
            modifier = modifier
        )
    }
}

/**
 * One `AnimatedVectorDrawable`, playing.
 *
 * It is an `AndroidView` and not a Compose painter, and the reason is the per-frame
 * bill, not the loop. The note that stood here until 8 set 2026 said Compose's
 * `AnimatedImageVector` could not play an endless loop; that was wrong — its parser
 * reads `repeatCount="infinite"` and builds an infinite `repeatable` from it. What it
 * does with it is the problem: it animates by recomposing the vector's tree every frame
 * and rasterizing the result on the UI thread, which is the thread the scroll needs.
 * The platform AVD in an `ImageView` hands its animators to the RenderThread (the
 * `VectorDrawableAnimatorRT` every hardware-accelerated `ImageView` gets), so the UI
 * thread pays nothing per frame while the icons move.
 *
 * What it does pay is on ENTRY, and that bill was trimmed on 8 set 2026 after the
 * strip and the page were reported "slightly choppy" (see [MovingIconView]):
 *
 * - **no `mutate()`**: the AVD's own constructor already deep-copies the vector tree for
 *   every instance a resource hands out (`AnimatedVectorDrawableState(copy, …)` calls
 *   `newDrawable` and then `mutate` on the inner `VectorDrawable`), so thirteen cells
 *   never shared an animator in the first place, and the explicit `mutate()` copied the
 *   tree a second time and threw the first away;
 * - **a recycled cell that shows the same weather keeps its drawing**: `onReset` only
 *   stops the loop, and `update` restarts it when the resource is the one already
 *   inflated — a stretch of sunny hours costs one inflation, not one per cell;
 * - the drawable is let go in `onRelease`, when the View itself is discarded.
 *
 * What it pays PER FRAME is the RenderThread re-rasterizing every visible loop at every
 * vsync, and that is what [paused] takes away while the page moves (9 set 2026).
 *
 * Nothing here starts a timer that outlives the view. A view that is not drawn is a
 * render node hwui does not prepare, and the animators of a node it does not prepare do
 * not run: scrolling a cell away or backgrounding the app stops the work without a
 * lifecycle observer of our own. (Not, as this note used to say, because the platform
 * pauses the animator set: `ImageView.onVisibilityAggregated` does call
 * `setVisible(false)`, but on the RenderThread animator that is a no-op — see
 * [MovingIconView.setPaused].)
 */
@Composable
private fun MovingIcon(@DrawableRes res: Int, paused: Boolean, modifier: Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context -> MovingIconView(context) },
        // `update` runs again on any recomposition that touches this node; `show` is
        // written so that a repeat with the same resource is a no-op — rebuilding the
        // drawable there would restart the loop, a raindrop that jumps back to the
        // cloud every time the temperature beside it changes. `setPaused` is a no-op
        // when nothing changed too.
        update = { view ->
            view.show(res)
            view.setPaused(paused)
        },
        // Lazy lists recycle these; a view handed back with the previous hour's weather
        // still running is the bug this exists to prevent. The drawing stays for the
        // next `show`, which may well be the same weather.
        onReset = { view -> view.rest() },
        onRelease = { view -> view.release() }
    )
}

/**
 * The `ImageView` behind [MovingIcon], carrying the two facts `update` needs and a tag
 * could not hold at once: which drawable is inflated, and whether its loop was stopped
 * by a reset. The second is kept here rather than asked of the drawable because the
 * RenderThread reports the end of a loop back to the UI thread a frame or two after
 * `stop()`, and a reuse that arrived in that window would have found `isRunning` true
 * and left the icon frozen.
 */
private class MovingIconView(context: Context) : ImageView(context) {

    @DrawableRes
    private var shownRes = 0
    private var resting = false

    init {
        scaleType = ScaleType.FIT_CENTER
        // The cell around it already carries the words; a second announcement of the
        // same thing is worse than none.
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    private val moving: AnimatedVectorDrawable?
        get() = drawable as? AnimatedVectorDrawable

    fun show(@DrawableRes res: Int) {
        val current = moving
        when {
            res != shownRes || current == null -> {
                shownRes = res
                resting = false
                val fresh = context.getDrawable(res) as? AnimatedVectorDrawable
                setImageDrawable(fresh)
                fresh?.start()
            }
            resting -> {
                // Back from the reuse pool with the same weather: same drawing, no
                // inflation, only the loop to restart.
                resting = false
                current.start()
            }
        }
    }

    /**
     * Hidden, not paused, while the page moves (9 set 2026) — because the RenderThread
     * animator cannot be paused: in AOSP `VectorDrawableAnimatorRT.pause()` and
     * `resume()` are two TODOs, so the `setVisible(false)` the platform sends an
     * off-screen `ImageView` stops nothing on that thread. `stop()` would, but it jumps
     * the drawing to the loop's end frame — for the rain that is the frame with no drops
     * in it — and `start()` would replay from the cloud at every rest.
     *
     * What does stop the work is not being drawn: an INVISIBLE child is skipped by its
     * parent, its render node leaves the display list, and hwui does not tick the
     * animators of a node it is not preparing. The still drawing under this view shows
     * meanwhile ([ConditionIcon]), and when the view is drawn again the loop is where
     * the clock says it should be — the animators run on frame time — not where it was
     * left, so nothing replays.
     */
    fun setPaused(paused: Boolean) {
        val target = if (paused) INVISIBLE else VISIBLE
        if (visibility != target) visibility = target
    }

    fun rest() {
        moving?.stop()
        resting = true
    }

    fun release() {
        moving?.stop()
        setImageDrawable(null)
        shownRes = 0
        resting = false
    }
}
