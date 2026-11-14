package com.callbackdev.chiaro.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * What the ring shows: the day's daylight as stretches of 0..1 from local midnight ([daylight]),
 * where the day is now, and whether the sun is up. At night the disc is the moon as it looks:
 * [moonLit] of its face lit, on the right while it waxes ([moonLitRight], the left in the southern
 * hemisphere), never a full moon nobody could see.
 */
internal data class DaylightRingSpec(
    val daylight: List<ClosedFloatingPointRange<Float>>,
    val now: Float,
    val sunUp: Boolean,
    val moonLit: Float = 1f,
    val moonLitRight: Boolean = true
)

/**
 * The ring's inks, from the card's own: [ink] the day gone by, [mid] the daylight still to come,
 * [track] the night; the sun's and the moon's ([com.callbackdev.chiaro.ui.theme.ArcPalette]): the
 * disc, its glow and the moon's shadowed part; and [halo] the card's own ground round the disc so it
 * stands off the arc (null on a see-through card or the sky, whose ground is a picture).
 */
internal data class DaylightRingInks(
    val ink: Color,
    val mid: Color,
    val track: Color,
    val sun: Color,
    val sunGlow: Color,
    val moon: Color,
    val moonShadow: Color,
    val halo: Color?
)

/**
 * The day's light as the family's ring (committente, 14 Nov 2026), painted into a bitmap, as the
 * family's other rings are: an arc with round caps at any minute of the day is the one drawing a
 * widget cannot build from boxes.
 *
 * **The disc stands centred on the ring's line and wider than it** (committente, the same day: «it
 * should be centred and stand out a little inside and a little outside»): the sun is a body on its
 * path, not a mark inside the band. So the bitmap is the family's circle plus [overflowOf] on every
 * side, and the ring inside it keeps the family's side exactly; the card gives the overflow back
 * out of its insets ([RingWordsContent]), so the ring lines up with its sisters' to the dp.
 *
 * The sun: a soft glow, a ring of the card's ground, and the disc lit from the upper left (the
 * family's light), warm at the centre and amber at its rim. The moon: its own pale glow, and its
 * phase drawn as it is, the lit part and the terminator over the shadowed face.
 *
 * Kept small for the platform's cap on a `RemoteViews`' bitmaps: [MAX_SIDE_PX] is a 104 dp ring
 * with its overflow at density 4, under 0.9 MB.
 */
internal object DaylightRingPainter {
    const val MAX_SIDE_PX: Int = 480

    /** The stroke, as a share of the ring's side: the family's ring, read from across a room. */
    const val STROKE_SHARE: Float = 0.11f

    fun strokeOf(side: Dp): Dp = maxOf(side * STROKE_SHARE, MIN_STROKE)

    /** How far the disc's glow reaches past the ring's side, on every side of the bitmap. */
    fun overflowOf(side: Dp): Dp = strokeOf(side) * (GLOW_SHARE - 0.5f)

    fun paint(sideDp: Dp, density: Float, spec: DaylightRingSpec, inks: DaylightRingInks): Bitmap {
        val overflowDp = overflowOf(sideDp)
        val fullDp = sideDp + overflowDp * 2
        val full = (fullDp.value * density).roundToInt().coerceIn(1, MAX_SIDE_PX)
        val px = full / fullDp.value.coerceAtLeast(1f)
        val bitmap = createBitmap(full, full)
        val canvas = Canvas(bitmap)
        val stroke = strokeOf(sideDp).value * px
        val center = full / 2f
        val radius = (sideDp.value * px - stroke) / 2f
        val oval = RectF(center - radius, center - radius, center + radius, center + radius)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = stroke
        }
        // The night: the whole ring, faint.
        paint.color = inks.track.toArgb()
        canvas.drawArc(oval, 0f, FULL_TURN, false, paint)
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
        // Now: the disc, centred on the ring's line.
        val angle = (ringAngle(spec.now) - QUARTER_TURN) * RADIANS
        val x = center + radius * cos(angle)
        val y = center + radius * sin(angle)
        val disc = stroke * DISC_SHARE
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        // The glow, fading to nothing at the bitmap's edge.
        val glow = if (spec.sunUp) inks.sunGlow else inks.moon
        val glowRadius = stroke * GLOW_SHARE
        fill.shader = RadialGradient(
            x, y, glowRadius,
            intArrayOf(
                glow.copy(alpha = if (spec.sunUp) SUN_GLOW_ALPHA else MOON_GLOW_ALPHA).toArgb(),
                glow.copy(alpha = 0f).toArgb()
            ),
            floatArrayOf(disc / glowRadius, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(x, y, glowRadius, fill)
        fill.shader = null
        inks.halo?.let {
            fill.color = it.toArgb()
            canvas.drawCircle(x, y, disc + stroke * HALO_SHARE, fill)
        }
        if (spec.sunUp) {
            // Lit from the upper left: the warm centre of the glow, the sun's amber at the rim.
            fill.shader = RadialGradient(
                x - disc * HIGHLIGHT_SHIFT, y - disc * HIGHLIGHT_SHIFT, disc * HIGHLIGHT_REACH,
                intArrayOf(inks.sunGlow.toArgb(), inks.sun.toArgb()),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(x, y, disc, fill)
            fill.shader = null
        } else {
            moon(canvas, x, y, disc, spec, inks, fill)
        }
        return bitmap
    }

    /**
     * The moon as it looks tonight: the shadowed face, and over it the lit part, a half disc on
     * the lit side closed by the terminator, an ellipse as wide as the phase.
     */
    private fun moon(
        canvas: Canvas,
        x: Float,
        y: Float,
        r: Float,
        spec: DaylightRingSpec,
        inks: DaylightRingInks,
        fill: Paint
    ) {
        fill.color = inks.moonShadow.toArgb()
        canvas.drawCircle(x, y, r, fill)
        val lit = spec.moonLit.coerceIn(0f, 1f)
        if (lit <= 0.01f) return
        fill.color = inks.moon.toArgb()
        if (lit >= 0.99f) {
            canvas.drawCircle(x, y, r, fill)
            return
        }
        val k = 2f * lit - 1f // −1 new, 0 a quarter, +1 full
        val face = RectF(x - r, y - r, x + r, y + r)
        val terminator = RectF(x - r * abs(k), y - r, x + r * abs(k), y + r)
        val path = Path()
        if (spec.moonLitRight) {
            path.arcTo(face, -QUARTER_TURN, HALF_TURN) // top, right, bottom
            path.arcTo(terminator, QUARTER_TURN, if (k > 0f) HALF_TURN else -HALF_TURN)
        } else {
            path.arcTo(face, QUARTER_TURN, HALF_TURN) // bottom, left, top
            path.arcTo(terminator, -QUARTER_TURN, if (k > 0f) HALF_TURN else -HALF_TURN)
        }
        path.close()
        canvas.drawPath(path, fill)
    }

    /** A canvas arc starts at three o'clock; the ring's day starts at noon at the top. */
    private fun start(fraction: Float): Float = ringAngle(fraction) - QUARTER_TURN

    private fun sweep(from: Float, to: Float): Float = ((to - from) * FULL_TURN).coerceAtLeast(MIN_SWEEP)

    private val MIN_STROKE = 3.dp
    private const val FULL_TURN = 360f
    private const val HALF_TURN = 180f
    private const val QUARTER_TURN = 90f
    private const val MIN_SWEEP = 0.1f

    /** The disc's radius, in strokes: 1.64 strokes across, so it stands out of the line both ways. */
    private const val DISC_SHARE = 0.82f

    /** The ring of the card's ground round the disc, in strokes. */
    private const val HALO_SHARE = 0.14f

    /** How far the glow reaches from the disc's centre, in strokes. */
    private const val GLOW_SHARE = 1.3f
    private const val SUN_GLOW_ALPHA = 0.55f
    private const val MOON_GLOW_ALPHA = 0.3f
    private const val HIGHLIGHT_SHIFT = 0.35f
    private const val HIGHLIGHT_REACH = 1.45f
    private const val RADIANS = (PI / 180).toFloat()
}
