package com.callbackdev.chiaro.domain.pictures

import com.callbackdev.chiaro.domain.WeatherStateEngine
import java.time.Duration
import java.time.Instant

/**
 * **La striscia** (the strip) of the 4×1: twelve hours of the ribbon of light, 6 dp, with the
 * rain marked over it, the disc of «adesso» at its start and five hours under it — «ora»,
 * then every three (`docs/design/in-immagini/Forms.dc.html`, the 4×1 and `strip`).
 *
 * Its width is what the row leaves once the number and its gap are paid; the dp below are
 * from the strip's own top left corner.
 */
object StripMetrics {
    /** The hours the strip spans: twelve, the canvas's `ribbon(14, 12)`. */
    const val HOURS = 12L

    /** The whole strip's height, the ribbon at its top and the labels under it. */
    const val HEIGHT = 18f
    const val RIBBON_HEIGHT = 6f
    const val RIBBON_CORNER = 3f

    /** A rain mark: the hour's whole width over the ribbon, the rain's lightest blue at 85%. */
    const val MARK_ALPHA = 0.85f

    /** «Adesso»: 14 dp, a 2.5 dp ring of the veil at 40%, centred on the ribbon's rounded end
     * (the canvas's `left: -4px; top: -4px`) and never past either end. */
    const val DISC = 14f
    const val DISC_RING = 2.5f
    const val DISC_RING_ALPHA = 0.40f
    const val DISC_MIN_X = RIBBON_CORNER

    /** The hours: 11 sp quiet, 9 dp down, every three, spread edge to edge. */
    const val LABEL_TOP = 9f
    const val LABEL_SP = 11f
    const val LABEL_LINE = 12f
    const val LABEL_EVERY_HOURS = 3L
}

/** Where a strip label is held: the first against the strip's start, the last against its
 * end, the others centred on their hour — what the canvas's `justify-content:
 * space-between` does with five labels, written down so a bitmap can draw it. */
enum class StripAnchor { START, CENTER, END }

data class StripMark(val at: Instant, val left: Float, val width: Float)

data class StripTick(val at: Instant, val x: Float, val anchor: StripAnchor, val isNow: Boolean)

data class StripDrawing(
    val width: Float,
    val window: PictureWindow,
    val marks: List<StripMark>,
    /** The disc's centre on the ribbon's axis, y = [StripMetrics.RIBBON_HEIGHT] / 2. */
    val nowX: Float,
    val ticks: List<StripTick>
)

/**
 * The strip for the twelve hours from the hour we are in, at [width] dp.
 *
 * One mark per hour from 20%, the hour's own width over the ribbon: hours 0 to 11 of the
 * window, never the twelfth. The canvas loops to 12 inclusive and its thirteenth mark starts
 * where the ribbon ends, 18.6 dp out over the card's padding: the ribbon is twelve hours, and
 * the hour after them is not on it.
 */
fun stripDrawing(window: PictureWindow, hours: List<PictureHour>, width: Float): StripDrawing {
    val hourWidth = (width / window.hours).toFloat()
    fun x(at: Instant): Float = (window.fraction(at) * width).toFloat()
    val marks = hours
        .filter { !it.at.isBefore(window.start) && it.at.isBefore(window.end) }
        .sortedBy { it.at }
        .mapNotNull { hour ->
            val chance = hour.chancePct ?: return@mapNotNull null
            if (chance < WeatherStateEngine.PROBABLE_PCT) return@mapNotNull null
            val left = x(hour.at)
            StripMark(hour.at, left, minOf(hourWidth, width - left))
        }
    val count = (window.hours / StripMetrics.LABEL_EVERY_HOURS).toInt()
    val ticks = (0..count).map { k ->
        val at = window.start.plus(Duration.ofHours(k * StripMetrics.LABEL_EVERY_HOURS))
        StripTick(
            at = at,
            x = x(at),
            anchor = when (k) {
                0 -> StripAnchor.START
                count -> StripAnchor.END
                else -> StripAnchor.CENTER
            },
            isNow = at == window.nowHour
        )
    }
    return StripDrawing(
        width = width,
        window = window,
        marks = marks,
        nowX = x(window.now).coerceIn(StripMetrics.DISC_MIN_X, width - StripMetrics.DISC_MIN_X),
        ticks = ticks
    )
}
