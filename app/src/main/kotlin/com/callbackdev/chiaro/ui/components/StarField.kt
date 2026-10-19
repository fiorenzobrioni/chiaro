package com.callbackdev.chiaro.ui.components

import androidx.compose.runtime.Immutable
import com.callbackdev.chiaro.domain.model.Coordinates
import com.callbackdev.chiaro.domain.sky.AstronomyEngine
import com.callbackdev.chiaro.domain.sky.BrightStars
import java.time.Instant
import kotlin.math.abs
import kotlin.math.pow

/**
 * A star on the canvas (step 6 of the sky review, 19 ott 2026): where it stands against the
 * direction the canvas faces, and how it is drawn.
 */
@Immutable
data class StarMark(
    /** Azimuth from the direction the canvas faces, degrees, positive toward the right. */
    val acrossDeg: Float,
    val altitudeDeg: Float,
    /** 0..1 from the visual magnitude, 1 for Sirius ([StarField.brightness]). */
    val brightness: Float,
    /** 0 blue-white, 1 orange, from the B-V colour ([StarField.warmth]). */
    val warmth: Float,
    /** The star's own twinkle phase, 0..1. */
    val phase: Float
)

/**
 * The stars the canvas draws: the ones of [BrightStars] that stand in its window at this
 * instant from this place (DESIGN §3.9). The canvas is a camera facing the equator (south in
 * the north, north in the south, the frame the sun and the moon stand in) at the deck's
 * elevations, 45° at the top down to 15° at the bottom, some 34° wide at the canvas' middle
 * row; the window kept here is wider by a margin, so a taller or wider canvas finds its stars
 * in the list and the list does not change with the layout. They are the sky that is there:
 * on a January evening in Milan the window holds Orion, and a reader may look up and check.
 */
object StarField {
    /** The window's half-width in azimuth and its altitudes, with the margin. */
    const val WINDOW_ACROSS_DEG = 30f
    const val WINDOW_ALT_LOW_DEG = 5f
    const val WINDOW_ALT_HIGH_DEG = 55f

    fun visible(at: Instant, coords: Coordinates, southern: Boolean): List<StarMark> {
        val facing = if (southern) 0.0 else 180.0
        val out = ArrayList<StarMark>(64)
        for (i in 0 until BrightStars.count) {
            val h = AstronomyEngine.starHorizontal(BrightStars.raDeg[i].toDouble(), BrightStars.decDeg[i].toDouble(), at, coords)
            if (h.altitudeDeg < WINDOW_ALT_LOW_DEG || h.altitudeDeg > WINDOW_ALT_HIGH_DEG) continue
            val across = ((h.azimuthDeg - facing + 540.0) % 360.0) - 180.0
            if (abs(across) > WINDOW_ACROSS_DEG) continue
            out += StarMark(
                acrossDeg = across.toFloat(),
                altitudeDeg = h.altitudeDeg.toFloat(),
                brightness = brightness(BrightStars.mag[i]),
                warmth = warmth(BrightStars.bv[i]),
                phase = phase(i)
            )
        }
        return out
    }

    /**
     * How bright a star is drawn, 0..1: the magnitude's span from the catalogue's limit to
     * Sirius, bent so the faint ones stay faint and the few bright ones stand out, with a
     * floor so a star at the limit is still a star and not nothing.
     */
    fun brightness(mag: Float): Float {
        val t = ((BrightStars.LIMIT_MAG + 0.5f - mag) / (BrightStars.LIMIT_MAG + 0.5f + 1.5f)).coerceIn(0f, 1f)
        return 0.2f + 0.8f * t.pow(1.3f)
    }

    /** The B-V colour as a warmth, 0..1: Rigel's −0.03 near 0, the Sun's 0.65 in the middle,
     * Betelgeuse's 1.85 at 1. A tint, never a saturated colour, which is what the eye sees. */
    fun warmth(bv: Float): Float = ((bv + 0.2f) / 1.6f).coerceIn(0f, 1f)

    /** A twinkle phase of the star's own, from its index: the golden ratio spreads them. */
    fun phase(index: Int): Float = ((index * 0.6180339887) % 1.0).toFloat()
}
