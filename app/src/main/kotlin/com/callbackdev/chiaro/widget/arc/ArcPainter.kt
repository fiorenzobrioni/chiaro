package com.callbackdev.chiaro.widget.arc

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withClip
import androidx.compose.ui.graphics.toArgb
import com.callbackdev.chiaro.domain.settings.TemperatureUnit
import com.callbackdev.chiaro.ui.format.Formats
import com.callbackdev.chiaro.ui.theme.ArcPalette
import com.callbackdev.chiaro.ui.theme.ChiaroColors
import com.callbackdev.chiaro.ui.theme.SkyPalette
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/** The inks the plot writes with, resolved by the card the way every widget color is. */
internal data class ArcInks(
    val primary: Int,
    val secondary: Int,
    val darkGround: Boolean,
    val sky: SkyPalette,
    val colors: ChiaroColors
)

/** The plot's box and the environment it is drawn for. */
internal data class ArcCanvasSpec(
    val widthPx: Int,
    val heightPx: Int,
    val density: Float,
    val fontScale: Float,
    val textScale: Float,
    val hourLabels: Boolean,
    val temperatures: Boolean,
    val tickStepHours: Int,
    val is24Hour: Boolean,
    val locale: Locale,
    val temperatureUnit: TemperatureUnit,
    val zone: ZoneId
)

/**
 * The arc itself, as pixels: the sky of every hour as bands, the sun's path over the
 * horizon and the moon's dotted beside it, the rain rising from the ground, the present
 * marked, the hours written under. Plain `android.graphics`, because a widget cannot
 * draw a curve any other way (RemoteViews knows no Canvas) and because it lets the
 * configuration screen show the very same picture the launcher will.
 *
 * Every measure is in dp through [ArcCanvasSpec.density]; every text follows the
 * reader's font scale; every color is a role or a sky table except the sun and the moon
 * ([ArcPalette]). Nothing here reads a clock: the series carries the present.
 */
internal object ArcPainter {

    /** The horizon sits below the middle: the sun's arch wants the height, the rain
     * below it needs enough room to read as a bar. */
    private const val HorizonFraction = 0.64f

    /** The altitude that reaches the top of the plot and the depth that reaches the
     * bottom. 70° is the summer noon of the mid latitudes; −40° keeps a midsummer night
     * (the sun bottoms out near −20° at 45° north) inside the lower half. */
    private const val AltitudeAtTop = 70f
    private const val AltitudeAtBottom = 40f

    /** How much of the past the veil takes away, per ground. */
    private const val PastVeilDark = 0.40f
    private const val PastVeilLight = 0.48f
    private const val PastPathAlpha = 0.45f

    /** Under this height (the one-cell dial at ~29 dp, the two-cell strip at ~32) the
     * plot keeps only what reads at that size: the bands, the sun's arch and its disc,
     * the present. The moon's path, the rain and the sun's course under the horizon are
     * detail, and detail at 30 dp is noise. */
    private const val CompactBelowDp = 40f

    /** The ribbon ground's band, centred on the horizon. */
    private const val RibbonDp = 6f

    fun paint(series: ArcSeries, settings: ArcSettings, spec: ArcCanvasSpec, inks: ArcInks): Bitmap {
        val w = spec.widthPx.coerceAtLeast(1)
        val h = spec.heightPx.coerceAtLeast(1)
        val bitmap = createBitmap(w, h)
        val canvas = Canvas(bitmap)
        val d = spec.density
        fun dp(v: Float) = v * d

        val labelPaint = textPaint(spec, inks.secondary, medium = false)
        val tempPaint = textPaint(spec, inks.primary, medium = true)
        val labelLine = if (spec.hourLabels) lineHeight(labelPaint) else 0f
        val tempLine = if (spec.hourLabels && spec.temperatures) lineHeight(tempPaint) else 0f

        val plotTop = 0f
        val plotBottom = (h - labelLine - tempLine).coerceAtLeast(dp(12f))
        val plot = RectF(0f, plotTop, w.toFloat(), plotBottom)
        val horizonY = plotTop + (plotBottom - plotTop) * HorizonFraction
        val corner = dp(8f)
        val plotClip = Path().apply { addRoundRect(plot, corner, corner, Path.Direction.CW) }
        val nowX = series.nowFraction * w
        val compact = h < CompactBelowDp * d

        fun xOf(fraction: Float) = fraction * w
        fun yOf(altitude: Float): Float = if (altitude >= 0f) {
            horizonY - (altitude / AltitudeAtTop).coerceAtMost(1f) * (horizonY - plotTop - dp(6f))
        } else {
            horizonY + (-altitude / AltitudeAtBottom).coerceAtMost(1f) * (plotBottom - horizonY - dp(4f))
        }

        // ---- The ground: the sky of every hour, or the ribbon of it, or nothing. ----
        canvas.save()
        canvas.clipPath(plotClip)
        val ribbon = RectF(plot.left, horizonY - dp(RibbonDp) / 2, plot.right, horizonY + dp(RibbonDp) / 2)
        when (settings.ground) {
            ArcGround.BANDS -> paintBands(canvas, series, inks, plot)
            ArcGround.RIBBON -> paintRibbon(canvas, series, inks, ribbon)
            ArcGround.NONE -> Unit
        }
        // What is past is veiled, so the eye lands on what is still to come — the ground
        // only, whichever it is: over a bare card there is nothing to veil, and over the
        // ribbon the veil is the ribbon's own height, not a block on the card.
        val veiled = when (settings.ground) {
            ArcGround.BANDS -> plot
            ArcGround.RIBBON -> ribbon
            ArcGround.NONE -> null
        }
        if (settings.fadePast && veiled != null && nowX > 0f) {
            val veil = Paint().apply {
                color = (if (inks.darkGround) Color.Black else Color.White).toArgb()
                alpha = ((if (inks.darkGround) PastVeilDark else PastVeilLight) * 255).roundToInt()
            }
            canvas.drawRect(veiled.left, veiled.top, nowX, veiled.bottom, veil)
        }

        // ---- The rain: each hour's chance, rising from the ground towards the horizon. ----
        if (settings.rain && !compact) {
            val hourWidth = w / (series.window.length.toMinutes() / 60f)
            val gap = dp(1.5f)
            val room = plotBottom - horizonY - dp(2f)
            val bar = Paint(Paint.ANTI_ALIAS_FLAG)
            series.hours.forEach { hour ->
                val pct = hour.hour.precipChancePct ?: return@forEach
                if (pct <= 0) return@forEach
                val left = xOf(hour.fraction) + gap / 2
                val right = (xOf(hour.fraction) + hourWidth - gap / 2).coerceAtMost(plot.right)
                if (right <= left) return@forEach
                val top = plotBottom - room * (pct.coerceIn(0, 100) / 100f)
                bar.color = inks.colors.rainAt(pct).toArgb()
                bar.alpha = 224
                canvas.drawRoundRect(RectF(left, top, right, plotBottom), dp(2f), dp(2f), bar)
            }
        }
        canvas.restore()

        // ---- The horizon. ----
        if (settings.sunPath || settings.moon) {
            val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = inks.secondary
                alpha = 120
                strokeWidth = dp(1f)
            }
            canvas.drawLine(plot.left, horizonY, plot.right, horizonY, line)
        }

        // ---- The moon's path while it is up: dotted, quiet. ----
        if (settings.moon && !compact) {
            val moonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = inks.secondary
                alpha = 180
                style = Paint.Style.STROKE
                strokeWidth = dp(1.25f)
                strokeCap = Paint.Cap.ROUND
                pathEffect = DashPathEffect(floatArrayOf(dp(1.5f), dp(3.5f)), 0f)
            }
            val path = Path()
            var open = false
            val n = series.moon.size
            for (i in 0 until n) {
                val alt = series.moon[i]
                val x = xOf(i / (n - 1f))
                if (alt > 0f) {
                    val y = yOf(alt)
                    if (open) path.lineTo(x, y) else path.moveTo(x, y)
                    open = true
                } else {
                    open = false
                }
            }
            canvas.withClip(plot.left, plot.top, plot.right, horizonY) { drawPath(path, moonPaint) }
        }

        // ---- The sun's path: strong above the horizon, faint and dashed below. ----
        if (settings.sunPath) {
            val path = Path()
            val n = series.sun.size
            for (i in 0 until n) {
                val x = xOf(i / (n - 1f))
                val y = yOf(series.sun[i])
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            val above = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = inks.primary
                style = Paint.Style.STROKE
                strokeWidth = dp(2f)
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }
            val below = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = inks.secondary
                alpha = 130
                style = Paint.Style.STROKE
                strokeWidth = dp(1f)
                pathEffect = DashPathEffect(floatArrayOf(dp(3f), dp(3f)), 0f)
            }
            val fade = settings.fadePast && nowX > 0f
            // Above the horizon, then below; each drawn twice when the past is veiled —
            // the part behind the present at reduced alpha, the part ahead at full.
            fun stroke(paint: Paint, top: Float, bottom: Float) {
                if (fade) {
                    val dimmed = Paint(paint).apply { alpha = (paint.alpha * PastPathAlpha).roundToInt() }
                    canvas.withClip(plot.left, top, nowX, bottom) { drawPath(path, dimmed) }
                    canvas.withClip(nowX, top, plot.right, bottom) { drawPath(path, paint) }
                } else {
                    canvas.withClip(plot.left, top, plot.right, bottom) { drawPath(path, paint) }
                }
            }
            stroke(above, plot.top, horizonY)
            if (!compact) stroke(below, horizonY, plot.bottom)
        }

        // ---- The present. ----
        if (settings.nowMarker) {
            val marker = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = inks.primary
                alpha = 200
                strokeWidth = dp(1.5f)
                strokeCap = Paint.Cap.ROUND
            }
            canvas.drawLine(nowX, plot.top + dp(2f), nowX, plot.bottom - dp(2f), marker)
        }

        // ---- The discs: the moon in its phase, the sun where it stands. ----
        if (settings.moon && series.moonNowDeg > 0.0) {
            paintMoon(canvas, nowX, yOf(series.moonNowDeg.toFloat()), dp(4.5f), series, inks, d)
        }
        if (settings.sunPath) {
            paintSun(canvas, nowX, yOf(series.sunNowDeg.toFloat()), series.sunNowDeg >= 0.0, d)
        }

        // ---- The hours, and their temperatures, written under the plot. ----
        if (spec.hourLabels) {
            paintTicks(canvas, series, spec, labelPaint, tempPaint, plotBottom, labelLine, tempLine)
        }
        return bitmap
    }

    /**
     * Every sample's sky, top to bottom, as the canvas computes it for that moment: the
     * altitude's band, the forecast hour's cloud and rain where the report has the hour,
     * and the §3.6 scrim over all of it — the same one the card wears, so ink stays
     * legible on the brightest noon.
     */
    private fun paintBands(
        canvas: Canvas,
        series: ArcSeries,
        inks: ArcInks,
        plot: RectF
    ) {
        val n = series.sun.size
        val paint = Paint()
        val width = plot.width()
        for (i in 0 until n - 1) {
            val x0 = plot.left + width * (i / (n - 1f))
            val x1 = plot.left + width * ((i + 1) / (n - 1f)) + 1f
            val hour = series.hourCovering(series.window.at(i / (n - 1f)))
            val gradient = inks.sky.gradient(
                sunAltitudeDeg = series.sun[i].toDouble(),
                cloudPct = hour?.hour?.cloudCoverPct ?: 0,
                precipPct = hour?.hour?.precipChancePct ?: 0,
                moonIllumination = series.moonLight.illuminatedFraction,
                moonAltitudeDeg = series.moon[(i * (series.moon.size - 1)) / (n - 1)].toDouble()
            )
            paint.shader = LinearGradient(
                0f, plot.top, 0f, plot.bottom,
                intArrayOf(gradient.top.toArgb(), gradient.mid.toArgb(), gradient.bottom.toArgb()),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawRect(x0, plot.top, x1, plot.bottom, paint)
        }
        val scrim = Paint().apply {
            color = SkyPalette.ScrimColor.toArgb()
            alpha = (SkyPalette.ScrimAlpha * 255).roundToInt()
        }
        canvas.drawRect(plot, scrim)
    }

    /** The app's daylight ribbon (DESIGN §4), laid along the horizon: the mid stop of
     * each sample's band, at the ribbon's own fixed saturation, no scrim. */
    private fun paintRibbon(
        canvas: Canvas,
        series: ArcSeries,
        inks: ArcInks,
        ribbon: RectF
    ) {
        val n = series.sun.size
        val paint = Paint()
        val w = ribbon.width()
        for (i in 0 until n - 1) {
            paint.color = inks.sky.gradient(series.sun[i].toDouble()).mid.toArgb()
            canvas.drawRect(
                ribbon.left + w * (i / (n - 1f)), ribbon.top,
                ribbon.left + w * ((i + 1) / (n - 1f)) + 1f, ribbon.bottom,
                paint
            )
        }
    }

    /** The sun: a soft halo and a disc above the horizon; a thin ring of the same amber
     * below it — the sun is still there, and saying where is not a lie. */
    private fun paintSun(canvas: Canvas, x: Float, y: Float, up: Boolean, d: Float) {
        if (up) {
            val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = ArcPalette.SunGlow.toArgb()
                alpha = 90
            }
            canvas.drawCircle(x, y, 9f * d, glow)
            val disc = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ArcPalette.Sun.toArgb() }
            canvas.drawCircle(x, y, 5f * d, disc)
        } else {
            val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = ArcPalette.Sun.toArgb()
                alpha = 190
                style = Paint.Style.STROKE
                strokeWidth = 1.5f * d
            }
            canvas.drawCircle(x, y, 4f * d, ring)
        }
    }

    /**
     * The moon in its real phase: the shadowed disc, the lit half on the side the reader
     * would see it, and the terminator as a half-ellipse whose width is how far the
     * phase is from a quarter — lit past the half, shadowed before it. A ring of the
     * card's ink keeps a pale moon visible on a pale card.
     */
    private fun paintMoon(
        canvas: Canvas,
        x: Float,
        y: Float,
        r: Float,
        series: ArcSeries,
        inks: ArcInks,
        d: Float
    ) {
        val fraction = series.moonLight.illuminatedFraction.coerceIn(0.0, 1.0).toFloat()
        val waxing = series.moonLight.elongation < 180.0
        // Northern sky: a waxing moon is lit on its right. Southern: on its left.
        val litRight = waxing != series.southern
        val shadow = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ArcPalette.MoonShadow.toArgb() }
        val lit = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ArcPalette.MoonLit.toArgb() }
        canvas.drawCircle(x, y, r, shadow)
        val box = RectF(x - r, y - r, x + r, y + r)
        // The lit half.
        canvas.drawArc(box, if (litRight) -90f else 90f, 180f, true, lit)
        // The terminator: an ellipse of half-width |2f − 1| · r, lit or shadowed.
        val rx = abs(2f * fraction - 1f) * r
        if (rx > 0.5f) {
            val terminator = if (fraction >= 0.5f) lit else shadow
            canvas.drawOval(RectF(x - rx, y - r, x + rx, y + r), terminator)
        }
        val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inks.secondary
            alpha = 150
            style = Paint.Style.STROKE
            strokeWidth = 1f * d
        }
        canvas.drawCircle(x, y, r, ring)
    }

    /**
     * The hour labels, spaced so none collide: the plan's step is a preference, and the
     * widest label the locale writes decides whether it holds. Under each labelled hour
     * the forecast temperature, where the report has that hour — the past prints its
     * hour and no number, which is what the report knows about it.
     */
    private fun paintTicks(
        canvas: Canvas,
        series: ArcSeries,
        spec: ArcCanvasSpec,
        labelPaint: Paint,
        tempPaint: Paint,
        plotBottom: Float,
        labelLine: Float,
        tempLine: Float
    ) {
        val w = spec.widthPx.toFloat()
        val pxPerHour = w / (series.window.length.toMinutes() / 60f)
        var step = spec.tickStepHours.coerceAtLeast(1)
        val all = series.ticks(spec.zone, 1)
        if (all.isEmpty()) return
        val widest = all.maxOf { (_, local) ->
            maxOf(
                labelPaint.measureText(Formats.hourLabel(local, spec.is24Hour, spec.locale)),
                if (tempLine > 0f) {
                    series.hourAt(local.atZone(spec.zone).toInstant())?.let {
                        tempPaint.measureText(
                            Formats.temperature(it.hour.tempC, spec.temperatureUnit, spec.locale)
                        )
                    } ?: 0f
                } else {
                    0f
                }
            )
        }
        val minSpacing = widest + 6f * spec.density
        while (step < 24 && pxPerHour * step < minSpacing) step = widenStep(step)

        val labelBaseline = baseline(labelPaint, plotBottom, labelLine)
        val tempBaseline = baseline(tempPaint, plotBottom + labelLine, tempLine)
        series.ticks(spec.zone, step).forEach { (at, local) ->
            val x = series.window.fraction(at) * w
            val label = Formats.hourLabel(local, spec.is24Hour, spec.locale)
            val half = labelPaint.measureText(label) / 2
            if (x - half < 0f || x + half > w) return@forEach
            canvas.drawText(label, x, labelBaseline, labelPaint)
            if (tempLine > 0f) {
                val hour = series.hourAt(at) ?: return@forEach
                val temp = Formats.temperature(hour.hour.tempC, spec.temperatureUnit, spec.locale)
                canvas.drawText(temp, x, tempBaseline, tempPaint)
            }
        }
    }

    /** The next spacing that still lands on the clock's own multiples. */
    internal fun widenStep(step: Int): Int = when {
        step < 2 -> 2
        step < 3 -> 3
        step < 4 -> 4
        step < 6 -> 6
        step < 8 -> 8
        step < 12 -> 12
        else -> 24
    }

    private fun textPaint(spec: ArcCanvasSpec, color: Int, medium: Boolean): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = TickSp * spec.textScale * spec.fontScale * spec.density
            textAlign = Paint.Align.CENTER
            typeface = if (medium) {
                Typeface.create("sans-serif-medium", Typeface.NORMAL)
            } else {
                Typeface.DEFAULT
            }
        }

    /** A text line's box as the widget's TextViews keep it: the font's whole height. */
    private fun lineHeight(paint: Paint): Float {
        val fm = paint.fontMetrics
        return fm.bottom - fm.top
    }

    /** Where to put the baseline so the glyphs sit centred in a line of [line] height. */
    private fun baseline(paint: Paint, top: Float, line: Float): Float {
        val fm = paint.fontMetrics
        return top + (line - (fm.descent - fm.ascent)) / 2 - fm.ascent
    }
}
