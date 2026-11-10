package com.callbackdev.chiaro.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.lerp
import com.callbackdev.chiaro.ui.theme.ChiaroMotion
import com.callbackdev.chiaro.ui.theme.SkyGradient

/** The sky a canvas draws: its gradient and the scene over it. */
typealias Sky = Pair<SkyGradient, SkyScene>

/**
 * The sky between two skies (review of the sky, 9 nov 2026). The hero's scene was computed
 * for the hour and handed to the shader as it was: at the turn of an hour, at a refresh, at a
 * place that gets its forecast, the cover, the light and the colours all changed in one frame.
 * A sky does not do that, and since the same day the ribbon is dragged through the hours of
 * the day ([com.callbackdev.chiaro.ui.today.CanvasHeader]), which would have been a slide show.
 *
 * So the canvas draws [shown]: the sky it came from, the sky it goes to and how far along it
 * is, read in the draw phase, so that a change costs frames of the shader and not
 * recompositions of the text over it. A new target starts from what is on screen at that
 * moment. Where the weather may not move (the reader's setting, the system's, battery saver,
 * a page being scrolled) the new sky is simply there, as every other motion in the app does.
 */
@Stable
class SkyTransition internal constructor(sky: Sky) {
    private var from by mutableStateOf(sky)
    private var to by mutableStateOf(sky)
    private val progress = Animatable(1f)

    /** The sky to draw now. Reads the animation's clock: call it while drawing. */
    fun shown(): Sky {
        val t = progress.value
        return if (t >= 1f) to else lerpSky(from, to, t)
    }

    internal suspend fun moveTo(sky: Sky, animate: Boolean) {
        if (sky == to) return
        if (!animate) {
            from = sky
            to = sky
            progress.snapTo(1f)
            return
        }
        from = shown()
        to = sky
        progress.snapTo(0f)
        progress.animateTo(1f, ChiaroMotion.sky())
    }
}

/** A [SkyTransition] that follows [gradient] and [scene], gliding while the weather may move. */
@Composable
fun rememberSkyTransition(gradient: SkyGradient, scene: SkyScene, moving: Boolean): SkyTransition {
    val transition = remember { SkyTransition(gradient to scene) }
    LaunchedEffect(gradient, scene) { transition.moveTo(gradient to scene, moving) }
    return transition
}

/**
 * The sky [t] of the way from [a] to [b] (pure, `SkyTransitionTest`). Every amount and every
 * colour is mixed; a body that is in one sky only keeps its place and fades, so the sun sets
 * by dimming where it stood rather than by leaping; the angles turn the short way round. What
 * cannot be mixed changes at the middle, where its amount is lowest: the stars' list (the
 * amount they show by is mixed), the radiant, the galactic frame.
 */
internal fun lerpSky(a: Sky, b: Sky, t: Float): Sky {
    if (t <= 0f) return a
    if (t >= 1f) return b
    val (ga, sa) = a
    val (gb, sb) = b
    fun f(x: Float, y: Float) = x + (y - x) * t
    val late = t >= 0.5f
    val gradient = SkyGradient(lerp(ga.top, gb.top, t), lerp(ga.mid, gb.mid, t), lerp(ga.bottom, gb.bottom, t))
    val scene = SkyScene(
        cover = f(sa.cover, sb.cover),
        deck = f(sa.deck, sb.deck),
        veil = f(sa.veil, sb.veil),
        opacity = f(sa.opacity, sb.opacity),
        lit = lerp(sa.lit, sb.lit, t),
        shade = lerp(sa.shade, sb.shade, t),
        light = Offset(f(sa.light.x, sb.light.x), f(sa.light.y, sb.light.y)),
        glow = f(sa.glow, sb.glow),
        stars = f(sa.stars, sb.stars),
        rain = f(sa.rain, sb.rain),
        snow = f(sa.snow, sb.snow),
        drift = f(sa.drift, sb.drift),
        maxLuminance = f(sa.maxLuminance, sb.maxLuminance),
        sun = lerpSun(sa.sun, sb.sun, t),
        moon = lerpMoon(sa.moon, sb.moon, t),
        lightAmount = f(sa.lightAmount, sb.lightAmount),
        starField = if (late) sb.starField else sa.starField,
        planets = f(sa.planets, sb.planets),
        milkyWay = f(sa.milkyWay, sb.milkyWay),
        galactic = if (late) sb.galactic ?: sa.galactic else sa.galactic ?: sb.galactic,
        turn = f(sa.turn, sb.turn),
        fog = f(sa.fog, sb.fog),
        // A fog that comes or goes wears the colour of the sky that has it.
        fogColor = when {
            sa.fog <= 0f -> sb.fogColor
            sb.fog <= 0f -> sa.fogColor
            else -> lerp(sa.fogColor, sb.fogColor, t)
        },
        storm = f(sa.storm, sb.storm),
        lowSun = f(sa.lowSun, sb.lowSun),
        sunAcross = f(sa.sunAcross, sb.sunAcross),
        rays = f(sa.rays, sb.rays),
        meteors = f(sa.meteors, sb.meteors),
        radiant = if (late) sb.radiant else sa.radiant,
        meteorSeed = if (late) sb.meteorSeed else sa.meteorSeed
    )
    return gradient to scene
}

private fun lerpSun(a: SunMark?, b: SunMark?, t: Float): SunMark? {
    if (a == null && b == null) return null
    fun f(x: Float, y: Float) = x + (y - x) * t
    // The one that is there lends its place to the one that is not, which shows nothing.
    val from = a ?: b!!.copy(disc = 0f, glowCore = 0f, glowMid = 0f)
    val to = b ?: a!!.copy(disc = 0f, glowCore = 0f, glowMid = 0f)
    return SunMark(
        across = f(from.across, to.across),
        up = f(from.up, to.up),
        color = lerp(from.color, to.color, t),
        warmth = f(from.warmth, to.warmth),
        disc = f(from.disc, to.disc),
        glowCore = f(from.glowCore, to.glowCore),
        glowMid = f(from.glowMid, to.glowMid),
        spread = f(from.spread, to.spread)
    )
}

private fun lerpMoon(a: MoonMark?, b: MoonMark?, t: Float): MoonMark? {
    if (a == null && b == null) return null
    fun f(x: Float, y: Float) = x + (y - x) * t
    val from = a ?: b!!.copy(disc = 0f, glow = 0f)
    val to = b ?: a!!.copy(disc = 0f, glow = 0f)
    return MoonMark(
        across = f(from.across, to.across),
        up = f(from.up, to.up),
        illumination = f(from.illumination, to.illumination),
        limbAngleDeg = turnTo(from.limbAngleDeg, to.limbAngleDeg, t),
        disc = f(from.disc, to.disc),
        glow = f(from.glow, to.glow),
        spread = f(from.spread, to.spread),
        northAngleDeg = turnTo(from.northAngleDeg, to.northAngleDeg, t)
    )
}

/** From one angle to another the short way round, in degrees. */
internal fun turnTo(from: Float, to: Float, t: Float): Float {
    val delta = ((to - from + 540f) % 360f) - 180f
    return from + delta * t
}
