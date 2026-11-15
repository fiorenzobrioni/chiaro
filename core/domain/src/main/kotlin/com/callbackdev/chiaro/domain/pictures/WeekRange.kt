package com.callbackdev.chiaro.domain.pictures

/**
 * The 4×4's five days: each day's excursion as a bar on a **fixed scale from −5° to 30°**
 * (PLANNING, «Le otto forme»), so November stays short and July long, and the week reads
 * against the year rather than against itself (CLAUDE.md: scales anchor to the world). The
 * canvas's `day()`: `left = (lo + 5) / 35`, `width = (hi − lo) / 35`.
 */
object WeekRange {
    const val SCALE_MIN_C = -5.0
    const val SCALE_MAX_C = 30.0

    /** The bar and its track: 6 dp, corners 3, the track white at 16%. */
    const val BAR_HEIGHT = 6f
    const val BAR_CORNER = 3f
    const val TRACK_ALPHA = 0.16f

    /** A day's row, with a 1 dp hairline at 10% over every row but the first. */
    const val ROW_HEIGHT = 38f
    const val HAIRLINE_ALPHA = 0.10f

    /** The rows the 4×4 shows: today first, five days. */
    const val DAYS = 5
}

/**
 * Where a day's bar runs, as fractions of the track, 0 at −5° and 1 at 30°.
 *
 * A day past either end is **clipped to the track**, never stretched: the scale is the point,
 * and the figures printed beside the bar say the real low and high. A day entirely outside
 * the scale still gets a sliver at the end it is past, so a −12° to −8° day reads as «off
 * the cold end» and not as a day with no range. Null when either figure is missing.
 */
fun weekRangeBar(lowC: Double?, highC: Double?): ClosedFloatingPointRange<Float>? {
    if (lowC == null || highC == null) return null
    val span = WeekRange.SCALE_MAX_C - WeekRange.SCALE_MIN_C
    fun f(c: Double) = ((c - WeekRange.SCALE_MIN_C) / span).coerceIn(0.0, 1.0).toFloat()
    val lo = minOf(lowC, highC)
    val hi = maxOf(lowC, highC)
    val start = f(lo)
    val end = f(hi)
    return when {
        end - start >= MIN_SLIVER -> start..end
        start >= 1f - MIN_SLIVER -> (1f - MIN_SLIVER)..1f
        else -> start..(start + MIN_SLIVER)
    }
}

/** The thinnest bar drawn: under 1% of the track (0.35°, 1.2 dp on the 4×4) a day still
 * shows that it has a range, and where it sits. */
private const val MIN_SLIVER = 0.01f
