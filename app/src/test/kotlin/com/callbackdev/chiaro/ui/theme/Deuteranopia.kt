package com.callbackdev.chiaro.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * The color-vision validator DESIGN.md §2.3 measures its separations with, in the test
 * source rather than in a tool, so the numbers the document prints are re-measured on
 * every build (`PaletteDocTest`) instead of on the day somebody remembers to look.
 *
 * Two steps, both textbook and both stated so the numbers can be reproduced:
 *
 * 1. **The simulation** is Viénot, Brettel and Mollon (1999) for a deuteranope, applied
 *    to LINEAR sRGB: to LMS, replace M with the plane the missing cone leaves
 *    (`M' = 0.494207·L + 1.24827·S`), and back.
 * 2. **The distance** is CIE ΔE*ab (CIE76) in L\*a\*b\* under D65 — the plain Euclidean
 *    one, because what is being asked here is "are these two the same color to this
 *    reader", not "which of two near-matches is closer". A just-noticeable difference is
 *    about **2.3** on that scale, which is the bar every separation below is read against.
 *
 * Kept apart from the palette so a second reader of the numbers (a widget, a later
 * phase) does not copy the matrices: one implementation, two test files.
 */
object Deuteranopia {

    /** How far apart two colors are to a deuteranope, in ΔE*ab. */
    fun separation(a: Color, b: Color): Double = deltaE(simulate(a), simulate(b))

    /** The same distance for normal color vision, for the "before" of a comparison. */
    fun deltaE(a: Color, b: Color): Double {
        val (l1, a1, b1) = lab(a)
        val (l2, a2, b2) = lab(b)
        return sqrt((l1 - l2).pow(2) + (a1 - a2).pow(2) + (b1 - b2).pow(2))
    }

    /** The threshold the eye starts to notice at: ΔE*ab 2.3 (CIE76's own JND). */
    const val JustNoticeable = 2.3

    fun simulate(color: Color): Color {
        val r = linear(color.red)
        val g = linear(color.green)
        val b = linear(color.blue)
        val l = 17.8824 * r + 43.5161 * g + 4.11935 * b
        val m = 3.45565 * r + 27.1554 * g + 3.86714 * b
        val s = 0.0299566 * r + 0.184309 * g + 1.46709 * b
        val m2 = 0.494207 * l + 1.24827 * s
        return Color(
            gamma(0.0809444479 * l - 0.130504409 * m2 + 0.116721066 * s),
            gamma(-0.0102485335 * l + 0.0540193266 * m2 - 0.113614708 * s),
            gamma(-0.000365296938 * l - 0.00412161469 * m2 + 0.693511405 * s)
        )
    }

    private fun linear(c: Float): Double =
        if (c <= 0.04045f) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)

    private fun gamma(c: Double): Float {
        val v = if (c <= 0.0031308) 12.92 * c else 1.055 * c.pow(1 / 2.4) - 0.055
        return v.coerceIn(0.0, 1.0).toFloat()
    }

    /** sRGB → XYZ (D65) → L\*a\*b\*. */
    private fun lab(color: Color): Triple<Double, Double, Double> {
        val r = linear(color.red)
        val g = linear(color.green)
        val b = linear(color.blue)
        val x = (0.4124564 * r + 0.3575761 * g + 0.1804375 * b) / 0.95047
        val y = 0.2126729 * r + 0.7151522 * g + 0.0721750 * b
        val z = (0.0193339 * r + 0.1191920 * g + 0.9503041 * b) / 1.08883
        fun f(t: Double) = if (t > 216.0 / 24389.0) t.pow(1.0 / 3.0) else (841.0 / 108.0) * t + 4.0 / 29.0
        val fx = f(x)
        val fy = f(y)
        val fz = f(z)
        return Triple(116 * fy - 16, 500 * (fx - fy), 200 * (fy - fz))
    }
}
