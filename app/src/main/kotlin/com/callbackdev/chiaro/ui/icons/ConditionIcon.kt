package com.callbackdev.chiaro.ui.icons

import android.graphics.drawable.AnimatedVectorDrawable
import android.view.View
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
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
    if (moving != null) {
        MovingIcon(moving, modifier)
    } else {
        Icon(
            imageVector = ImageVector.vectorResource(
                ChiaroIcons.styledRes(lineRes, style, darkGround, palette)
            ),
            contentDescription = null, // the row it sits in speaks once, via its semantics
            tint = Color.Unspecified, // Meteocons carry their own measured colors
            modifier = modifier
        )
    }
}

/**
 * One `AnimatedVectorDrawable`, playing.
 *
 * It is an `AndroidView` and not a Compose painter because the loops here are endless,
 * and Compose's own `AnimatedImageVector` is built for the other thing — a transition
 * from one state to another, played once when a boolean flips. An AVD with
 * `repeatCount="infinite"` needs a host that lets the drawable drive its own
 * invalidations, which is exactly what an `ImageView` is.
 *
 * The drawable is [android.graphics.drawable.Drawable.mutate]d because resources hand
 * out a shared constant state, and thirteen hour cells sharing one animator would be
 * thirteen icons keeping each other's time.
 *
 * Nothing here starts a timer that outlives the view: the platform stops an AVD when its
 * host stops being visible (`ImageView.onVisibilityAggregated` → `setVisible(false)`), so
 * scrolling a cell away or backgrounding the app pauses it without a lifecycle observer
 * of our own. That is the battery answer, and it is the platform's, not a promise made
 * here.
 */
@Composable
private fun MovingIcon(@DrawableRes res: Int, modifier: Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            ImageView(context).apply {
                scaleType = ImageView.ScaleType.FIT_CENTER
                // The cell around it already carries the words; a second announcement
                // of the same thing is worse than none.
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }
        },
        update = { view ->
            // `update` runs again on any recomposition that touches this node, and
            // rebuilding the drawable there would restart the loop — a raindrop that
            // jumps back to the cloud every time the temperature beside it changes.
            // The tag is the cheapest honest answer to "is this already the right one".
            if (view.tag != res) {
                view.tag = res
                val drawable = view.context.getDrawable(res)?.mutate() as? AnimatedVectorDrawable
                view.setImageDrawable(drawable)
                drawable?.start()
            }
        },
        onReset = { view ->
            // Lazy lists recycle these; a view handed back with the previous hour's
            // weather still running is the bug this exists to prevent.
            (view.drawable as? AnimatedVectorDrawable)?.stop()
            view.setImageDrawable(null)
            view.tag = null
        }
    )
}
