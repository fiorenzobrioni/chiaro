package com.callbackdev.chiaro.widget.pictures

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.graphics.Typeface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.callbackdev.chiaro.domain.pictures.DialDrawing
import com.callbackdev.chiaro.domain.pictures.PlateDrawing
import com.callbackdev.chiaro.domain.pictures.PlateMetrics
import com.callbackdev.chiaro.domain.pictures.PlateRun
import com.callbackdev.chiaro.domain.pictures.PictureSpan
import com.callbackdev.chiaro.domain.pictures.RainTone
import com.callbackdev.chiaro.domain.pictures.StripAnchor
import com.callbackdev.chiaro.domain.pictures.StripDrawing
import com.callbackdev.chiaro.domain.pictures.StripMetrics
import com.callbackdev.chiaro.domain.pictures.WeekRange
import com.callbackdev.chiaro.ui.theme.PicturesInks
import kotlin.math.roundToInt

/**
 * «In immagini»'s pictures (Fase 17.2): the plate, the dial, the strip and the week's bars,
 * painted at the size they are shown at and in the density of the screen, from the geometry of
 * `domain/pictures`, with the inks of [PicturesInks]. Glance cannot draw a path, a gradient
 * along a curve or a sweep, so these are bitmaps; everything around them is Glance, text
 * included, except the plate's own labels and hours, which are part of the drawing and are
 * painted here in the system face, as the launcher would set them (`Typeface.DEFAULT`).
 *
 * Every coordinate below is the geometry's dp; the canvas is scaled once by the density, and a
 * text size is `sp × fontScale` dp, which is what the platform turns an sp into.
 *
 * A picture is painted with room around it where the canvas's own drawing reaches past its
 * edge ([PlateMetrics.OVERHANG], [StripOverhang]): the card places the bitmap that much up and
 * to the left of the rect the layout gives.
 */
internal object PicturesPainter {

    /** What the plate writes: the high's and the low's labels, and one label per tick. */
    data class PlateText(val high: String?, val low: String?, val ticks: List<String>)

    /** The reader's switches that change the plate («Sulla card»). */
    data class PlateShow(val rain: Boolean = true, val ribbon: Boolean = true)

    /**
     * The plate, [drawing] over its veil, with [ribbon] as (fraction of the window, colour)
     * stops. In «La giornata» the hours before now are drawn at [SpentAlpha], the morning
     * already spent (the canvas's settings board).
     */
    fun plate(
        drawing: PlateDrawing,
        ribbon: List<Pair<Float, Color>>,
        inks: PicturesInks,
        text: PlateText,
        density: Float,
        fontScale: Float,
        show: PlateShow = PlateShow()
    ): Bitmap {
        val o = PlateMetrics.OVERHANG
        val w = drawing.width
        val h = PlateMetrics.HEIGHT
        val (bitmap, canvas) = canvasFor(w + 2 * o, h + 2 * o, density)
        canvas.translate(o, o)

        // The plate: the veil, and its inner hairline.
        val plate = RectF(0f, 0f, w, h)
        canvas.drawRoundRect(plate, PlateMetrics.CORNER, PlateMetrics.CORNER, fill(inks.veil))
        val hairline = RectF(0.5f, 0.5f, w - 0.5f, h - 0.5f)
        canvas.drawRoundRect(hairline, PlateMetrics.CORNER - 0.5f, PlateMetrics.CORNER - 0.5f, stroke(inks.hairline, 1f))

        val spentX = if (drawing.window.span == PictureSpan.DAY) drawing.nowX else null

        // The curve: each unbroken run, its area fading from 28% to nothing at the row's top.
        drawing.runs.filter { it.points.isNotEmpty() }.forEach { run ->
            withSpent(canvas, spentX, w, h) {
                val top = run.points.minOf { it.y }
                if (run.points.size > 1) {
                    val area = curvePath(run).apply {
                        lineTo(run.points.last().x, PlateMetrics.AREA_BOTTOM)
                        lineTo(run.points.first().x, PlateMetrics.AREA_BOTTOM)
                        close()
                    }
                    canvas.drawPath(area, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        shader = LinearGradient(
                            0f, top, 0f, PlateMetrics.AREA_BOTTOM,
                            inks.ink.copy(alpha = PlateMetrics.AREA_ALPHA_TOP).toArgb(),
                            inks.ink.copy(alpha = 0f).toArgb(),
                            Shader.TileMode.CLAMP
                        )
                    })
                    canvas.drawPath(curvePath(run), stroke(inks.ink, PlateMetrics.STROKE))
                } else {
                    val p = run.points.single()
                    canvas.drawCircle(p.x, p.y, PlateMetrics.STROKE / 2f, fill(inks.ink))
                }
            }
        }

        // The rain: its baseline, and a bar per hour from 20%.
        if (show.rain) {
            canvas.drawRect(
                PlateMetrics.INSET, PlateMetrics.RAIN_BASELINE,
                w - PlateMetrics.INSET, PlateMetrics.RAIN_BASELINE + 1f,
                fill(inks.ink.copy(alpha = PlateMetrics.RAIN_BASELINE_ALPHA))
            )
            drawing.bars.forEach { bar ->
                withSpent(canvas, spentX, w, h) {
                    val path = Path().apply {
                        addRoundRect(
                            RectF(bar.left, bar.top, bar.left + bar.width, bar.top + bar.height),
                            barRadii(bar.height),
                            Path.Direction.CW
                        )
                    }
                    canvas.drawPath(path, fill(inks.rain[toneIndex(bar.tone)]))
                }
            }
        }

        // The ribbon of light over the window.
        if (show.ribbon && ribbon.isNotEmpty()) {
            val left = drawing.ribbonLeft
            val right = drawing.ribbonRight
            canvas.drawRoundRect(
                RectF(left, PlateMetrics.RIBBON_TOP, right, PlateMetrics.RIBBON_TOP + PlateMetrics.RIBBON_HEIGHT),
                PlateMetrics.RIBBON_CORNER, PlateMetrics.RIBBON_CORNER,
                gradientPaint(ribbon, left, right)
            )
        }

        // The high and the low, on their points, and the hours.
        val labelPaint = textPaint(inks.ink, PlateMetrics.LABEL_SP * fontScale, Semibold)
        drawing.high?.let { e -> text.high?.let { drawCentred(canvas, it, e.labelLeft, e.labelTop, PlateMetrics.LABEL_WIDTH, PlateMetrics.LABEL_HEIGHT * fontScale, labelPaint) } }
        drawing.low?.let { e -> text.low?.let { drawCentred(canvas, it, e.labelLeft, e.labelTop, PlateMetrics.LABEL_WIDTH, PlateMetrics.LABEL_HEIGHT * fontScale, labelPaint) } }
        val tickPaint = textPaint(inks.quiet, PlateMetrics.TICK_SP * fontScale, Typeface.DEFAULT)
        drawing.ticks.zip(text.ticks).forEach { (tick, label) ->
            drawCentred(
                canvas, label, tick.centerX - PlateMetrics.TICK_WIDTH / 2f, PlateMetrics.TICK_TOP,
                PlateMetrics.TICK_WIDTH, PlateMetrics.TICK_LINE * fontScale, tickPaint
            )
        }

        // «Adesso»: the white disc with its ring, last, over everything.
        drawing.now?.let { p ->
            canvas.drawCircle(p.x, p.y, PlateMetrics.NOW_DISC / 2f + PlateMetrics.NOW_RING, fill(inks.plateDiscRing))
            canvas.drawCircle(p.x, p.y, PlateMetrics.NOW_DISC / 2f, fill(inks.disc))
        }
        return bitmap
    }

    /**
     * The dial: today's ribbon round the ring ([ring] as (fraction of the day, colour) stops,
     * midnight at the bottom), the spent hours under their veil, the rain's dots and «adesso».
     */
    fun dial(
        drawing: DialDrawing,
        ring: List<Pair<Float, Color>>,
        inks: PicturesInks,
        density: Float,
        rain: Boolean = true
    ): Bitmap {
        val spec = drawing.spec
        val o = DialOverhang
        val (bitmap, canvas) = canvasFor(spec.size + 2 * o, spec.size + 2 * o, density)
        canvas.translate(o, o)
        val c = spec.center
        val r = spec.ringRadius
        val oval = RectF(c - r, c - r, c + r, c + r)
        if (ring.isNotEmpty()) {
            // A sweep starts at three o'clock; the day starts at the bottom, a quarter on.
            val sweep = SweepGradient(c, c, ring.map { it.second.toArgb() }.toIntArray(), ring.map { it.first }.toFloatArray())
            sweep.setLocalMatrix(Matrix().apply { setRotate(90f, c, c) })
            canvas.drawOval(oval, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = spec.ring
                shader = sweep
            })
        }
        if (drawing.spentSweep > 0f) {
            canvas.drawArc(oval, 90f, drawing.spentSweep, false, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = spec.ring
                color = inks.spent.toArgb()
            })
        }
        if (rain) {
            drawing.dots.forEach { dot ->
                canvas.drawCircle(dot.x, dot.y, spec.rainDot / 2f, fill(inks.dot.copy(alpha = dot.alpha)))
            }
        }
        canvas.drawCircle(drawing.now.x, drawing.now.y, spec.disc / 2f + spec.discRing, fill(inks.markDiscRing))
        canvas.drawCircle(drawing.now.x, drawing.now.y, spec.disc / 2f, fill(inks.disc))
        return bitmap
    }

    /** The 4×1's strip: twelve hours of ribbon, the rain over it, «adesso», the five hours. */
    fun strip(
        drawing: StripDrawing,
        ribbon: List<Pair<Float, Color>>,
        inks: PicturesInks,
        ticks: List<String>,
        density: Float,
        fontScale: Float,
        rain: Boolean = true
    ): Bitmap {
        val o = StripOverhang
        val w = drawing.width
        val labelLine = StripMetrics.LABEL_LINE * fontScale
        val h = StripMetrics.LABEL_TOP + labelLine
        val (bitmap, canvas) = canvasFor(w + 2 * o, h + 2 * o, density)
        canvas.translate(o, o)
        val band = RectF(0f, 0f, w, StripMetrics.RIBBON_HEIGHT)
        val bandPath = Path().apply { addRoundRect(band, StripMetrics.RIBBON_CORNER, StripMetrics.RIBBON_CORNER, Path.Direction.CW) }
        if (ribbon.isNotEmpty()) canvas.drawPath(bandPath, gradientPaint(ribbon, 0f, w))
        if (rain) {
            canvas.save()
            canvas.clipPath(bandPath)
            drawing.marks.forEach { mark ->
                canvas.drawRect(mark.left, 0f, mark.left + mark.width, StripMetrics.RIBBON_HEIGHT, fill(inks.stripMark))
            }
            canvas.restore()
        }
        val paint = textPaint(inks.quiet, StripMetrics.LABEL_SP * fontScale, Typeface.DEFAULT)
        drawing.ticks.zip(ticks).forEach { (tick, label) ->
            val width = paint.measureText(label)
            val left = when (tick.anchor) {
                StripAnchor.START -> tick.x
                StripAnchor.CENTER -> tick.x - width / 2f
                StripAnchor.END -> tick.x - width
            }
            drawCentred(canvas, label, left, StripMetrics.LABEL_TOP, width, labelLine, paint)
        }
        val cy = StripMetrics.RIBBON_HEIGHT / 2f
        canvas.drawCircle(drawing.nowX, cy, StripMetrics.DISC / 2f + StripMetrics.DISC_RING, fill(inks.markDiscRing))
        canvas.drawCircle(drawing.nowX, cy, StripMetrics.DISC / 2f, fill(inks.disc))
        return bitmap
    }

    /** A day's range bar on its track: [range] as fractions of the −5° to 30° scale, painted
     * from the low's ramp step to the high's. */
    fun rangeBar(
        range: ClosedFloatingPointRange<Float>,
        width: Float,
        low: Color,
        high: Color,
        track: Color,
        density: Float
    ): Bitmap {
        val (bitmap, canvas) = canvasFor(width, WeekRange.BAR_HEIGHT, density)
        val r = WeekRange.BAR_CORNER
        canvas.drawRoundRect(RectF(0f, 0f, width, WeekRange.BAR_HEIGHT), r, r, fill(track))
        val left = range.start * width
        val right = (range.endInclusive * width).coerceAtLeast(left + 2f * r)
        canvas.drawRoundRect(
            RectF(left, 0f, right.coerceAtMost(width), WeekRange.BAR_HEIGHT), r, r,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(left, 0f, right, 0f, low.toArgb(), high.toArgb(), Shader.TileMode.CLAMP)
            }
        )
        return bitmap
    }

    /** A plate with nothing on it, [w] × [h] dp: «Più tardi»'s ground on the 2×3, the veil
     * and the hairline of the plate it is (`class="plate"` in the canvas). */
    fun panel(w: Float, h: Float, inks: PicturesInks, density: Float): Bitmap {
        val (bitmap, canvas) = canvasFor(w, h, density)
        canvas.drawRoundRect(RectF(0f, 0f, w, h), PlateMetrics.CORNER, PlateMetrics.CORNER, fill(inks.veil))
        canvas.drawRoundRect(
            RectF(0.5f, 0.5f, w - 0.5f, h - 0.5f), PlateMetrics.CORNER - 0.5f, PlateMetrics.CORNER - 0.5f,
            stroke(inks.hairline, 1f)
        )
        return bitmap
    }

    /** The dial's own room around it: its disc's ring at noon reaches past the box by
     * nothing (the ring is inside the dial), but a 1 dp margin keeps the antialiasing. */
    const val DialOverhang = 1f

    /** The strip's room: «adesso» is centred 3 dp into the ribbon and 3 dp down, and its
     * ring reaches 9.5 dp from there (`left: -4px; top: -4px` and its 2.5 dp shadow). */
    const val StripOverhang = 7f

    /** «La giornata»'s morning, already spent: the curve and the bars before now, lighter. */
    const val SpentAlpha = 0.45f

    private fun toneIndex(tone: RainTone): Int = when (tone) {
        RainTone.DRIZZLE -> 0
        RainTone.LIGHT -> 1
        RainTone.MODERATE -> 2
        RainTone.HEAVY -> 3
    }

    /** The bar's corners, 3 dp on top and 1 at the foot, never more than half its height. */
    private fun barRadii(height: Float): FloatArray {
        val top = minOf(PlateMetrics.BAR_CORNER_TOP, height / 2f)
        val bottom = minOf(PlateMetrics.BAR_CORNER_BOTTOM, height / 2f)
        return floatArrayOf(top, top, top, top, bottom, bottom, bottom, bottom)
    }

    private fun curvePath(run: PlateRun): Path = Path().apply {
        val first = run.points.first()
        moveTo(first.x, first.y)
        run.cubics.forEach { c ->
            cubicTo(c.control1.x, c.control1.y, c.control2.x, c.control2.y, c.end.x, c.end.y)
        }
    }

    /** Draws [block] twice where [spentX] splits the plate: lighter before it, full after. */
    private inline fun withSpent(canvas: Canvas, spentX: Float?, w: Float, h: Float, block: () -> Unit) {
        if (spentX == null) {
            block()
            return
        }
        canvas.save()
        canvas.clipRect(-PlateMetrics.OVERHANG, -PlateMetrics.OVERHANG, spentX, h + PlateMetrics.OVERHANG)
        canvas.saveLayerAlpha(null, (SpentAlpha * 255).roundToInt())
        block()
        canvas.restore()
        canvas.restore()
        canvas.save()
        canvas.clipRect(spentX, -PlateMetrics.OVERHANG, w + PlateMetrics.OVERHANG, h + PlateMetrics.OVERHANG)
        block()
        canvas.restore()
    }

    private fun canvasFor(widthDp: Float, heightDp: Float, density: Float): Pair<Bitmap, Canvas> {
        val bitmap = Bitmap.createBitmap(
            (widthDp * density).roundToInt().coerceAtLeast(1),
            (heightDp * density).roundToInt().coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        canvas.scale(density, density)
        return bitmap to canvas
    }

    private fun fill(color: Color) = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color.toArgb() }

    private fun stroke(color: Color, width: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color.toArgb()
        style = Paint.Style.STROKE
        strokeWidth = width
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private fun gradientPaint(stops: List<Pair<Float, Color>>, left: Float, right: Float) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                left, 0f, right, 0f,
                stops.map { it.second.toArgb() }.toIntArray(),
                stops.map { it.first.coerceIn(0f, 1f) }.toFloatArray(),
                Shader.TileMode.CLAMP
            )
        }

    private fun textPaint(color: Color, sizeDp: Float, face: Typeface) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color.toArgb()
        textSize = sizeDp
        typeface = face
        textAlign = Paint.Align.LEFT
    }

    /** [text] centred in a box, its line set as CSS sets one: the face's ascent and descent
     * centred in the line, the baseline where that puts it. */
    private fun drawCentred(canvas: Canvas, text: String, left: Float, top: Float, width: Float, line: Float, paint: Paint) {
        val m = paint.fontMetrics
        val baseline = top + (line - (m.descent - m.ascent)) / 2f - m.ascent
        val x = left + (width - paint.measureText(text)) / 2f
        canvas.drawText(text, x, baseline, paint)
    }

    /** The canvas's `font-weight: 600`, the high and the low. */
    private val Semibold: Typeface by lazy { Typeface.create(Typeface.DEFAULT, 600, false) }
}
