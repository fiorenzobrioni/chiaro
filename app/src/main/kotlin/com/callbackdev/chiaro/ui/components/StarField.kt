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
 * the north, north in the south, the frame the sun and the moon stand in), for the stars from
 * [StarCameraTopDeg] at the top row down to [StarCameraBottomDeg] at the bottom and about
 * ±29° of azimuth on a phone (`starPlace`); the window kept here is wider by a margin, so a
 * taller or wider canvas finds its stars in the list and the list does not change with the
 * layout. They are the sky that is there: on a January evening in Milan the window holds
 * Orion, and a reader may look up and check.
 *
 * The window was the deck's camera, 34° × 30°, and the catalogue stopped at magnitude 4.5
 * (committente, from the device, 20 ott 2026: «two or three stars»). Measured for Milan: on
 * an October evening, with Capricorn and Aquarius in the south and a gibbous moon over them,
 * that window held 15 stars, 3 of them under the third magnitude, which the moon's rule then
 * halved; the same evening the wider camera holds 46 to magnitude 4.5 and 245 to 6.0, a
 * January one 91 and 447. Random stars were refused: a star a reader can disprove by looking
 * up is the one thing this canvas must not draw.
 */
object StarField {
    /** The window's half-width in azimuth and its altitudes, with the margin over the camera. */
    const val WINDOW_ACROSS_DEG = 36f
    const val WINDOW_ALT_LOW_DEG = 5f
    const val WINDOW_ALT_HIGH_DEG = 70f

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
     * floor so a star at the limit is still a speck and not nothing. At magnitude 6.0 the
     * limit is the eye's under a dark sky, and the stars between 4.5 and 6 are the dust a
     * dark sky has: a tenth of Sirius at the limit, a fifth at 4.5, a third at the third
     * magnitude, three fifths at the first.
     */
    fun brightness(mag: Float): Float {
        val t = ((BrightStars.LIMIT_MAG + 0.5f - mag) / (BrightStars.LIMIT_MAG + 0.5f + 1.5f)).coerceIn(0f, 1f)
        return 0.08f + 0.92f * t.pow(1.5f)
    }

    /** The B-V colour as a warmth, 0..1: Rigel's −0.03 near 0, the Sun's 0.65 in the middle,
     * Betelgeuse's 1.85 at 1. A tint, never a saturated colour, which is what the eye sees. */
    fun warmth(bv: Float): Float = ((bv + 0.2f) / 1.6f).coerceIn(0f, 1f)

    /** A twinkle phase of the star's own, from its index: the golden ratio spreads them. */
    fun phase(index: Int): Float = ((index * 0.6180339887) % 1.0).toFloat()
}
