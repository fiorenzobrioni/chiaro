package com.callbackdev.chiaro.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * What the ring shows: the day's daylight as stretches of 0..1 from local midnight ([daylight]),
 * where the day is now, and whether the sun is up (the disc is the sun's amber, or the moon's
 * pale at night).
 */
internal data class DaylightRingSpec(
    val daylight: List<ClosedFloatingPointRange<Float>>,
    val now: Float,
    val sunUp: Boolean
)

/**
 * The ring's inks, from the card's own: [ink] the day gone by, [mid] the daylight still to come,
 * [track] the night, [sun] and [moon] the disc ([com.callbackdev.chiaro.ui.theme.ArcPalette]), and
 * [halo] the card's own ground under the disc so it reads across the arc (null on a see-through
 * card or the sky, whose ground is a picture).
 */
internal data class DaylightRingInks(
    val ink: Color,
    val mid: Color,
    val track: Color,
    val sun: Color,
    val moon: Color,
    val halo: Color?
)

/**
 * The day's light as the family's ring (committente, 14 Nov 2026), painted into a bitmap of the
 * size it is shown at, as the family's other rings are: an arc with round caps at any minute of
 * the day is the one drawing a widget cannot build from boxes. Kept small for the platform's cap
 * on a `RemoteViews`' bitmaps: [MAX_SIDE_PX] is a 104 dp ring at density 4, 0.7 MB.
 */
internal object DaylightRingPainter {
    const val MAX_SIDE_PX: Int = 416

    /** The stroke, as a share of the ring's side: the family's ring, read from across a room. */
    const val STROKE_SHARE: Float = 0.11f

    fun strokeOf(side: Dp): Dp = maxOf(side * STROKE_SHARE, MIN_STROKE)

    fun paint(sideDp: Dp, density: Float, spec: DaylightRingSpec, inks: DaylightRingInks): Bitmap {
        val side = (sideDp.value * density).roundToInt().coerceIn(1, MAX_SIDE_PX)
        val px = density * side / (sideDp.value * density).coerceAtLeast(1f)
        val bitmap = createBitmap(side, side)
        val canvas = Canvas(bitmap)
        val stroke = strokeOf(sideDp).value * px
        val inset = stroke / 2f
        val oval = RectF(inset, inset, side - inset, side - inset)
        val center = side / 2f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = stroke
        }
        // The night: the whole ring, faint.
        paint.color = inks.track.toArgb()
        canvas.drawArc(oval, 0f, FULL_TURN, false, paint)
        paint.strokeCap = Paint.Cap.ROUND
        spec.daylight.forEach { light ->
            val whole = light.endInclusive - light.start >= 0.999f
            paint.strokeCap = if (whole) Paint.Cap.BUTT else Paint.Cap.ROUND
            // The daylight still to come, at half strength; then the part already gone, full.
            paint.color = inks.mid.toArgb()
            canvas.drawArc(oval, start(light.start), sweep(light.start, light.endInclusive), false, paint)
            val gone = minOf(spec.now, light.endInclusive)
            if (gone > light.start) {
                paint.color = inks.ink.toArgb()
                canvas.drawArc(oval, start(light.start), sweep(light.start, gone), false, paint)
            }
        }
        // Now: the sun, or the moon at night, a disc a little wider than the band, over a ring of
        // the card's ground. Its outer edge meets the ring's, so the disc sits in the band and
        // bulges inward, never past the family's circle (centred on the band, it was cut by the
        // bitmap's edge at the top of the ring: committente, 14 Nov 2026).
        val outer = stroke * (DISC_SHARE + HALO_SHARE)
        val along = side / 2f - outer
        val angle = (ringAngle(spec.now) - QUARTER_TURN) * RADIANS
        val x = center + along * cos(angle)
        val y = center + along * sin(angle)
        val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        inks.halo?.let {
            dot.color = it.toArgb()
            canvas.drawCircle(x, y, outer, dot)
        }
        dot.color = (if (spec.sunUp) inks.sun else inks.moon).toArgb()
        canvas.drawCircle(x, y, stroke * DISC_SHARE, dot)
        return bitmap
    }

    /** A canvas arc starts at three o'clock; the ring's day starts at noon at the top. */
    private fun start(fraction: Float): Float = ringAngle(fraction) - QUARTER_TURN

    private fun sweep(from: Float, to: Float): Float = ((to - from) * FULL_TURN).coerceAtLeast(MIN_SWEEP)

    private val MIN_STROKE = 3.dp
    private const val FULL_TURN = 360f
    private const val QUARTER_TURN = 90f
    private const val MIN_SWEEP = 0.1f
    private const val DISC_SHARE = 0.62f

    /** The halo's width round the disc, as a share of the stroke. */
    private const val HALO_SHARE = 0.16f
    private const val RADIANS = (PI / 180).toFloat()
}
