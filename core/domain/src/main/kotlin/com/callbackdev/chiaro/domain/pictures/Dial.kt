package com.callbackdev.chiaro.domain.pictures

import com.callbackdev.chiaro.domain.WeatherStateEngine
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import kotlin.math.cos
import kotlin.math.sin

/**
 * **Il quadrante** (the dial) of the 2×1, 2×2 and 2×3 cards: the 24 hours of today round a
 * circle, midnight at the bottom and noon at the top, the ring painted with today's ribbon,
 * the hours already gone under a veil, «adesso» a disc on the ring and the rain of the next
 * 24 hours as dots inside it (`docs/design/in-immagini/Forms.dc.html`, `dial()` and
 * `conic()`).
 *
 * The clock runs clockwise from the bottom: 6:00 on the left, noon on top, 18:00 on the
 * right — the canvas's `conic-gradient(from 180deg, …)` and its `at(r, h)`, angle
 * `180° + h × 15°` from the top. Coordinates are dp from the dial's own top left corner.
 */
data class DialSpec(
    /** The dial's side: 124 dp at most, 52 on the 2×1. */
    val size: Float,
    /** The ring's thickness. */
    val ring: Float,
    /** «Adesso»: the disc's diameter and its ring of the veil. */
    val disc: Float,
    val discRing: Float,
    /** How far in from the dial's edge the rain's dots run, and their diameter. */
    val rainInset: Float,
    val rainDot: Float
) {
    val center: Float get() = size / 2f

    /** The radius the ring's middle runs at, where the disc of «adesso» stands. */
    val ringRadius: Float get() = center - ring / 2f

    /** The radius the rain's dots run at. */
    val rainRadius: Float get() = center - rainInset

    /** The inside of the ring, where the 2×2 prints its number. */
    val innerRadius: Float get() = center - ring

    /** The same proportions at [side] dp: what a 2×2 squeezed under 124 dp draws. The ring,
     * the disc and the dots keep their dp, only the circle gets smaller. */
    fun at(side: Float): DialSpec = copy(size = side)

    companion object {
        /** The 2×2 and the 2×3: 124 dp, ring 9, disc 14 with a 2.5 ring, dots of 5 at 16 dp in
         * (PLANNING, «Il quadrante»). 124 is a ceiling: «il quadrante si ferma a 124 dp». */
        val LARGE = DialSpec(size = 124f, ring = 9f, disc = 14f, discRing = 2.5f, rainInset = 16f, rainDot = 5f)

        /** The 2×1: 52 dp, ring 6 (the plan); the disc 10 with a 2 dp ring and dots of 3 at 11
         * in, which the plan does not write and the canvas does (`dial(52, 6, 10, 11, 3)`). */
        val SMALL = DialSpec(size = 52f, ring = 6f, disc = 10f, discRing = 2f, rainInset = 11f, rainDot = 3f)

        /** The veil over the hours already gone, and the disc's ring. */
        const val SPENT_ALPHA = 0.42f
        const val DISC_RING_ALPHA = 0.40f

        /** A dot's opacity: from this at 0% to 1 at 100% (`0.45 + p × 0.55`). */
        const val RAIN_ALPHA_FLOOR = 0.45f
    }
}

/** One hour of rain on the dial. */
data class DialDot(val at: Instant, val x: Float, val y: Float, val alpha: Float)

/** Everything the painter needs to draw one dial. */
data class DialDrawing(
    val spec: DialSpec,
    /** The hours already gone, as a sweep in degrees clockwise from the bottom (midnight):
     * the veil covers 0 to this. */
    val spentSweep: Float,
    /** The centre of «adesso»'s disc on the ring. */
    val now: PlatePoint,
    val dots: List<DialDot>
)

/** Degrees clockwise from the bottom for a time of day in hours (0 to 24): the angle the
 * painter's sweep gradient takes the day's ribbon at. */
fun dialSweep(hourOfDay: Double): Float = (hourOfDay * DEGREES_PER_HOUR).toFloat()

/** The point at [radius] from the centre of [spec] for a time of day in hours. */
fun dialPoint(spec: DialSpec, radius: Float, hourOfDay: Double): PlatePoint {
    val a = Math.toRadians(180.0 + hourOfDay * DEGREES_PER_HOUR)
    return PlatePoint(
        (spec.center + radius * sin(a)).toFloat(),
        (spec.center - radius * cos(a)).toFloat()
    )
}

/**
 * The dial at [now] on the place's clock, with the rain of the 24 hours from the hour we are
 * in: one dot per hour from 20% (`PROBABLE_PCT`), the hours of tonight landing in the
 * spent part of the ring, which is the canvas's picture («la pioggia di stanotte in punti»).
 *
 * The hours run from the hour we are in to 23 hours after it, never 24: the canvas loops
 * over 25 and its 25th lands on the first one's angle, where a second dot would draw
 * tomorrow's rain over today's.
 */
fun dialDrawing(spec: DialSpec, now: Instant, zone: ZoneId, hours: List<PictureHour>): DialDrawing {
    val local = now.atZone(zone)
    val hourOfDay = local.hour + local.minute / 60.0 + local.second / 3600.0
    val first = PictureWindow.next(now, zone).start
    val last = first.plus(Duration.ofHours(DIAL_RAIN_HOURS - 1))
    val dots = hours
        .filter { !it.at.isBefore(first) && !it.at.isAfter(last) }
        .sortedBy { it.at }
        .mapNotNull { hour ->
            val chance = hour.chancePct ?: return@mapNotNull null
            if (chance < WeatherStateEngine.PROBABLE_PCT) return@mapNotNull null
            val at = hour.at.atZone(zone)
            val p = dialPoint(spec, spec.rainRadius, at.hour + at.minute / 60.0)
            DialDot(
                at = hour.at,
                x = p.x,
                y = p.y,
                alpha = DialSpec.RAIN_ALPHA_FLOOR + chance.coerceIn(0, 100) / 100f * (1f - DialSpec.RAIN_ALPHA_FLOOR)
            )
        }
    return DialDrawing(
        spec = spec,
        spentSweep = dialSweep(hourOfDay),
        now = dialPoint(spec, spec.ringRadius, hourOfDay),
        dots = dots
    )
}

private const val DEGREES_PER_HOUR = 15.0

/** The rain the dial carries: the next 24 hours, the hour we are in first. */
const val DIAL_RAIN_HOURS = 24L
