package com.callbackdev.chiaro.ui.theme

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * DESIGN.md §7. Springs, not durations: a duration says how long, a spring says how it
 * feels, and Material 3 Expressive's whole motion argument is that the second is what a
 * reader notices.
 *
 * Every spec here collapses to a 100 ms fade when the reader has animations turned off,
 * and nothing here may ever gate information: a reader with motion off sees the same
 * content at the same moment. Until the Fase 9 pass that was a comment — the constant
 * existed, [LocalReducedMotion] did not, and no animation in the app had ever been
 * asked. It is asked now, at all three places the app moves.
 */
object ChiaroMotion {

    /** Anything that moves or resizes. */
    fun <T> spatial(reduced: Boolean = false): FiniteAnimationSpec<T> =
        if (reduced) fade() else spring(dampingRatio = 0.8f, stiffness = 380f)

    /** Chips, toggles, small state. */
    fun <T> spatialFast(reduced: Boolean = false): FiniteAnimationSpec<T> =
        if (reduced) fade() else spring(dampingRatio = 0.9f, stiffness = 800f)

    /** Color, alpha, elevation — things that change without moving. */
    fun <T> effects(reduced: Boolean = false): FiniteAnimationSpec<T> =
        if (reduced) fade() else spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 1600f)

    /** The escape hatch itself. */
    fun <T> fade(): FiniteAnimationSpec<T> = tween(reducedMotionFadeMillis)

    const val reducedMotionFadeMillis = 100

    /**
     * A section opening or closing. Reduced motion keeps the fade and drops the
     * measure: an expand is the one transition that moves everything under it, and it
     * is also the one whose content is fully readable the instant it is composed.
     */
    fun enter(reduced: Boolean): EnterTransition =
        if (reduced) fadeIn(fade()) else fadeIn(spatial()) + expandVertically(spatial())

    fun exit(reduced: Boolean): ExitTransition =
        if (reduced) fadeOut(fade()) else fadeOut(spatial()) + shrinkVertically(spatial())
}

/**
 * Whether the reader has asked the system for less motion.
 *
 * Android has no "prefers-reduced-motion" flag of its own: Accessibility → Remove
 * animations, and Developer options → Animator duration scale, both land on
 * [Settings.Global.ANIMATOR_DURATION_SCALE], and zero is the answer. Reading the global
 * setting is therefore not a hack around a missing API, it IS the API — the platform's
 * own animators consult the same number.
 *
 * Defaults to false, so a preview or a test renders the app as most readers see it.
 */
val LocalReducedMotion = staticCompositionLocalOf { false }

internal fun reducedMotionOf(context: Context): Boolean =
    Settings.Global.getFloat(
        context.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        1f
    ) == 0f

/**
 * The setting, live. It is changed from outside the app — that is the whole point of a
 * system accessibility toggle — so a value read once at start-up would be right until
 * the first reader who turns it on while the app is open, which is exactly the reader
 * it is for.
 */
@Composable
internal fun rememberReducedMotion(context: Context): Boolean {
    var reduced by remember(context) { mutableStateOf(reducedMotionOf(context)) }
    DisposableEffect(context) {
        val resolver = context.contentResolver
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                reduced = reducedMotionOf(context)
            }
        }
        resolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
            false,
            observer
        )
        reduced = reducedMotionOf(context)
        onDispose { resolver.unregisterContentObserver(observer) }
    }
    return reduced
}

/** Sugar for the call sites, which read better asking than looking one up. */
@Composable
@ReadOnlyComposable
fun reducedMotion(): Boolean = LocalReducedMotion.current
