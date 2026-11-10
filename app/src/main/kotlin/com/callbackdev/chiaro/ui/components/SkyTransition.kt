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
        // A sky that differs from the one on screen by less than the eye takes in is simply
        // there (10 nov 2026). Every minute the page hands over a new sky, the sun a quarter
        // of a degree on, and each one started a glide: a second of the shader at every frame
        // of the screen, sixty times an hour, for a move nobody sees. Measured from what is
        // shown, so a glide still under way is carried on to the new sky and not cut.
        if (!animate || skyDistance(shown(), sky) < GlideFrom) {
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
 * How far two skies are from each other, 0..1 (pure, `SkyTransitionTest`, 10 nov 2026): the
 * largest single difference between them, each in the unit it is drawn in. A colour by its
 * channel, an amount by itself, a body's place by its share of the canvas, an angle by its
 * share of a half turn; a body that is in one sky only counts for as much of it as shows. What
 * cannot be mixed (the stars' list, the galactic frame, the radiant) is not counted: it moves
 * by a fraction of a pixel a minute, and it changes whole at the middle of a glide anyway.
 */
internal fun skyDistance(a: Sky, b: Sky): Float {
    val (ga, sa) = a
    val (gb, sb) = b
    var far = 0f
    fun amount(x: Float, y: Float) {
        far = maxOf(far, kotlin.math.abs(x - y))
    }
    fun colour(x: androidx.compose.ui.graphics.Color, y: androidx.compose.ui.graphics.Color) {
        amount(x.red, y.red)
        amount(x.green, y.green)
        amount(x.blue, y.blue)
    }
    fun angle(x: Float, y: Float) = amount(kotlin.math.abs(turnTo(x, y, 1f) - x) / 180f, 0f)
    colour(ga.top, gb.top)
    colour(ga.mid, gb.mid)
    colour(ga.bottom, gb.bottom)
    amount(sa.cover, sb.cover)
    amount(sa.deck, sb.deck)
    amount(sa.veil, sb.veil)
    amount(sa.opacity, sb.opacity)
    colour(sa.lit, sb.lit)
    colour(sa.shade, sb.shade)
    amount(sa.light.x, sb.light.x)
    amount(sa.light.y, sb.light.y)
    amount(sa.glow, sb.glow)
    amount(sa.stars, sb.stars)
    amount(sa.planets, sb.planets)
    amount(sa.milkyWay, sb.milkyWay)
    amount(sa.rain, sb.rain)
    amount(sa.snow, sb.snow)
    amount(sa.maxLuminance, sb.maxLuminance)
    amount(sa.lightAmount, sb.lightAmount)
    amount(sa.fog, sb.fog)
    if (sa.fog > 0f && sb.fog > 0f) colour(sa.fogColor, sb.fogColor)
    amount(sa.storm, sb.storm)
    amount(sa.lowSun, sb.lowSun)
    // Where the low sun's lobe stands matters as much as there is of it.
    amount(sa.sunAcross * maxOf(sa.lowSun, sb.lowSun), sb.sunAcross * maxOf(sa.lowSun, sb.lowSun))
    amount(sa.rays, sb.rays)
    amount(sa.meteors, sb.meteors)
    val sunA = sa.sun
    val sunB = sb.sun
    if (sunA != null && sunB != null) {
        amount(sunA.across, sunB.across)
        amount(sunA.up, sunB.up)
        amount(sunA.warmth, sunB.warmth)
        amount(sunA.disc, sunB.disc)
        amount(sunA.glowCore, sunB.glowCore)
        amount(sunA.glowMid, sunB.glowMid)
        colour(sunA.color, sunB.color)
    } else {
        (sunA ?: sunB)?.let { amount(maxOf(it.disc, it.glowCore, it.glowMid), 0f) }
    }
    val moonA = sa.moon
    val moonB = sb.moon
    if (moonA != null && moonB != null) {
        amount(moonA.across, moonB.across)
        amount(moonA.up, moonB.up)
        amount(moonA.illumination, moonB.illumination)
        amount(moonA.disc, moonB.disc)
        amount(moonA.glow, moonB.glow)
        angle(moonA.limbAngleDeg, moonB.limbAngleDeg)
        angle(moonA.northAngleDeg, moonB.northAngleDeg)
    } else {
        (moonA ?: moonB)?.let { amount(maxOf(it.disc, it.glow), 0f) }
    }
    return far
}

/**
 * The distance from which a new sky is glided to rather than simply shown: a twentieth, of a
 * colour's channel, of an amount, of the canvas for a body's place. Under it a change is one
 * step nobody sees; from it up it is weather, or an hour dragged, and the canvas glides.
 * `SkyTransitionTest` holds both sides (a minute at noon and one at night are under it, ten
 * points of cloud are not) and counts a day: of the sun's four hundred quarter degrees from
 * 60° down to −40°, thirteen glide under a clear sky, five under a broken one, one under a
 * closed one, all between 6° and −5°, where the light turns golden, the sun sets and the
 * planets come out. Every one of the four hundred glided before.
 */
internal const val GlideFrom = 0.05f

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
