package com.callbackdev.chiaro.notifications

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.compose.ui.graphics.toArgb
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.data.AppPalette
import com.callbackdev.chiaro.data.ServiceLocator
import com.callbackdev.chiaro.domain.model.HourlyForecast
import com.callbackdev.chiaro.domain.settings.TemperatureUnit
import com.callbackdev.chiaro.domain.warnings.PlaceWarnings
import com.callbackdev.chiaro.domain.warnings.WarningHazard
import com.callbackdev.chiaro.domain.warnings.WarningLevel
import com.callbackdev.chiaro.ui.format.Formats
import com.callbackdev.chiaro.ui.theme.ChiaroColors
import com.callbackdev.chiaro.ui.theme.paletteFor
import com.callbackdev.chiaro.ui.warnings.WarningText
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Locale
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

/**
 * The pictures an expanded notification may carry (23 set 2026, notification review), and
 * only where a picture says something the lines under it cannot say as fast:
 *
 * - **the rain's hours** under a rain or storm alert — the next twelve hours of rain
 *   chance as bars, the alert's window lit behind them, the worst hour labelled: «when,
 *   and how long» is a shape, and the text needs three lines to say it;
 * - **the day** under the two summaries — the temperature as a curve coloured on the
 *   world scale, the rain under it, the night shaded off — the Today screen's strip, the
 *   one picture of a day this app already draws;
 * - **the levels** under an official warning — the Dipartimento's own colour grid, hazards
 *   by day, each cell a word on its level's measured container (a level is a word before
 *   it is a colour, DESIGN §2.3).
 *
 * Nothing else gets one: the sky reminder's verdict is a word and a number, and a fired
 * rule is the reader's own message — a drawing under either would be decoration.
 *
 * The same rules as every chart in the app (DESIGN §9): one hue for a quantity, never two
 * quantities on one axis (so the rain has its own row under the temperature, not a second
 * scale on the same one), rain on the world's 0–100% and colour on the world's
 * temperature scale. And every picture has a text equivalent — the lines under it, and a
 * content description on the image ([NotificationViews]).
 *
 * The bitmaps are painted for the ground the notification will have when it is posted:
 * the system's night mode, read once, and the reader's dress (§2.5). A 1000 px wide image
 * is laid out as ~340 dp at ~3×, so the type sizes below are the dp of a 3× screen.
 */
internal object NotificationCharts {

    const val WIDTH = 1000

    /** The inks a chart is painted with: the reader's dress, on the ground the system
     * will give the notification. */
    data class Inks(
        val colors: ChiaroColors,
        val ink: Int,
        val quiet: Int,
        val hairline: Int,
        val night: Int,
        val band: Int,
        val severeBand: Int,
        val dark: Boolean
    )

    fun inks(context: Context): Inks {
        val dark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        val palette = paletteFor(readerPalette(context))
        val scheme = palette.scheme(dark)
        val colors = palette.colors(dark)
        return Inks(
            colors = colors,
            ink = scheme.onSurface.toArgb(),
            quiet = scheme.onSurfaceVariant.toArgb(),
            hairline = scheme.outlineVariant.toArgb(),
            // The night as a cool tint of the primary, not a grey: the daylight ribbon's
            // night is blue, and a grey block reads as «no data».
            night = scheme.primary.copy(alpha = if (dark) 0.12f else 0.07f).toArgb(),
            band = scheme.primary.copy(alpha = if (dark) 0.16f else 0.10f).toArgb(),
            severeBand = colors.unstable.container.copy(alpha = if (dark) 0.55f else 0.65f).toArgb(),
            dark = dark
        )
    }

    /** The dress the reader picked, read from the store the worker already uses; the
     * default when the store does not answer at once (a notification is never late for
     * the sake of its colours). */
    private fun readerPalette(context: Context): AppPalette = runCatching {
        runBlocking {
            withTimeoutOrNull(PaletteReadMillis) {
                ServiceLocator.settingsStore(context).settings.first().palette
            }
        }
    }.getOrNull() ?: AppPalette.VIVID

    private const val PaletteReadMillis = 500L

    // ---------------------------------------------------------------- the rain's hours

    /**
     * The next [RainHours] hours of rain chance from [from], the alert's [window] lit
     * behind its bars ([severe] in the storm's amber, the rain's own in the primary), the
     * worst hour labelled. Null when the forecast has fewer than [MinHours] hours left or
     * none of them carries a chance at all — a row of empty bars is a chart of nothing.
     */
    fun rainHours(
        hours: List<HourlyForecast>,
        from: LocalDateTime,
        window: AlertWindow?,
        severe: Boolean,
        inks: Inks,
        is24h: Boolean,
        locale: Locale
    ): Bitmap? {
        val shown = hours.filter { !it.time.isBefore(from.withMinute(0)) }.take(RainHours)
        if (shown.size < MinHours || shown.all { it.precipChancePct == null }) return null
        val height = RainChartHeight
        val bmp = Bitmap.createBitmap(WIDTH, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val cell = WIDTH.toFloat() / shown.size
        val labelTop = height - LabelRow
        val barTop = PeakLabel.toFloat()
        val barBottom = labelTop - 8f

        // The window, behind everything: the hours the alert is about.
        window?.let { w ->
            val first = shown.indexOfFirst { !it.time.isBefore(w.start) }
            val last = shown.indexOfLast { !it.time.isAfter(w.end) }
            if (first >= 0 && last >= first) {
                canvas.drawRoundRect(
                    RectF(first * cell + 2, 0f, (last + 1) * cell - 2, barBottom), 18f, 18f,
                    fill(if (severe) inks.severeBand else inks.band)
                )
            }
        }

        // The baseline and the half-way guide: the world's 0 and 50%, never the data's.
        canvas.drawLine(0f, barBottom, WIDTH.toFloat(), barBottom, stroke(inks.hairline, 2f))
        val mid = barBottom - (barBottom - barTop) / 2
        canvas.drawLine(0f, mid, WIDTH.toFloat(), mid, stroke(inks.hairline, 1.5f).apply {
            pathEffect = android.graphics.DashPathEffect(floatArrayOf(8f, 10f), 0f)
        })

        val peak = shown.maxByOrNull { it.precipChancePct ?: -1 }
        shown.forEachIndexed { i, hour ->
            val pct = hour.precipChancePct ?: return@forEachIndexed
            val x0 = i * cell + cell * 0.2f
            val x1 = (i + 1) * cell - cell * 0.2f
            val top = barBottom - (barBottom - barTop) * pct / 100f
            if (pct > 0) {
                canvas.drawRoundRect(
                    RectF(x0, top.coerceAtMost(barBottom - 6f), x1, barBottom), 10f, 10f,
                    fill(inks.colors.rainAt(pct).toArgb())
                )
            }
            if (hour == peak && pct > 0) {
                text(inks.colors.rainInkAt(pct).toArgb(), 30f, bold = true).let {
                    canvas.drawText(Formats.percent(pct, locale), (x0 + x1) / 2, top - 12f, it)
                }
            }
        }
        hourLabels(canvas, shown.map { it.time }, cell, height - 14f, inks, is24h, locale)
        return bmp
    }

    // ---------------------------------------------------------------------- the day

    /**
     * A day as the Today screen's strip draws it: the temperature as a curve, each stretch
     * coloured by where it stands on the world's scale (§2.3, −5 to 35 °C), its high and
     * its low marked; the rain chance as a row of bars under it on its own 0–100%; the
     * hours outside [sunrise]–[sunset] shaded as night. Null under [MinHours] hours.
     */
    fun day(
        hours: List<HourlyForecast>,
        sunrise: LocalDateTime?,
        sunset: LocalDateTime?,
        unit: TemperatureUnit,
        inks: Inks,
        is24h: Boolean,
        locale: Locale
    ): Bitmap? {
        if (hours.size < MinHours) return null
        val rain = hours.any { (it.precipChancePct ?: 0) > 0 }
        val height = if (rain) DayChartHeight else DayChartHeightDry
        val bmp = Bitmap.createBitmap(WIDTH, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val cell = WIDTH.toFloat() / hours.size
        val labelTop = height - LabelRow
        val rainBottom = labelTop - 6f
        val rainTop = rainBottom - RainRow
        val curveTop = 52f
        val curveBottom = (if (rain) rainTop - 20f else labelTop - 26f)

        // The night, first and faint: the daylight ribbon's own reading of the hours.
        if (sunrise != null && sunset != null) {
            val nightPaint = fill(inks.night)
            hours.forEachIndexed { i, hour ->
                val mid = hour.time.plusMinutes(30)
                if (mid.isBefore(sunrise) || !mid.isBefore(sunset)) {
                    canvas.drawRect(i * cell, 0f, (i + 1) * cell, rainBottom, nightPaint)
                }
            }
        }

        // The curve. Its vertical scale is the day's own range (at least 4 °C, so a flat
        // day stays flat), and its COLOUR is the world's — which is the part that must
        // not re-anchor: a mild day must look mild.
        val temps = hours.map { it.tempC }
        val low = temps.min()
        val high = temps.max()
        val span = maxOf(high - low, 4.0)
        val base = (low + high) / 2 - span / 2
        fun y(t: Double) = (curveBottom - (t - base) / span * (curveBottom - curveTop)).toFloat()
        val points = hours.mapIndexed { i, h -> (i + 0.5f) * cell to y(h.tempC) }
        val curve = smooth(points)
        val gradient = LinearGradient(
            0f, 0f, WIDTH.toFloat(), 0f,
            temps.map { inks.colors.temperatureAt(it).toArgb() }.toIntArray(),
            points.map { it.first / WIDTH }.toFloatArray(),
            Shader.TileMode.CLAMP
        )
        val area = Path(curve).apply {
            lineTo(points.last().first, curveBottom)
            lineTo(points.first().first, curveBottom)
            close()
        }
        // The area under the curve in the same colours, fading to nothing at its foot: a
        // layer masked by a vertical ramp, so the day reads as a warm or cool shape and
        // not as a band with a hard floor.
        val layer = canvas.saveLayer(0f, 0f, WIDTH.toFloat(), curveBottom, null)
        canvas.drawPath(area, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = gradient; alpha = AreaAlpha })
        canvas.drawRect(
            0f, curveTop - 30f, WIDTH.toFloat(), curveBottom,
            Paint().apply {
                shader = LinearGradient(
                    0f, curveTop - 30f, 0f, curveBottom,
                    android.graphics.Color.BLACK, android.graphics.Color.TRANSPARENT,
                    Shader.TileMode.CLAMP
                )
                xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.DST_IN)
            }
        )
        canvas.restoreToCount(layer)
        // An opaque base colour under the shader: a paint's alpha is its colour's, and a
        // shader on a transparent paint draws nothing at all.
        canvas.drawPath(
            curve,
            stroke(android.graphics.Color.BLACK, 7f).apply {
                shader = gradient
                strokeCap = Paint.Cap.ROUND
            }
        )

        // The high and the low, each a dot on the curve and its figure beside it.
        val hiIndex = temps.indexOf(high)
        val loIndex = temps.indexOf(low)
        listOf(hiIndex to true, loIndex to false).distinctBy { it.first }.forEach { (i, isHigh) ->
            val (x, yy) = points[i]
            canvas.drawCircle(x, yy, 9f, fill(inks.colors.temperatureAt(temps[i]).toArgb()))
            canvas.drawCircle(x, yy, 9f, stroke(inks.ink, 2.5f))
            val label = Formats.temperature(temps[i], unit, locale)
            val paint = text(inks.ink, 34f, bold = true)
            val tx = x.coerceIn(40f, WIDTH - 40f)
            canvas.drawText(label, tx, if (isHigh) yy - 20f else (yy + 42f).coerceAtMost(curveBottom + 36f), paint)
        }

        if (rain) {
            canvas.drawLine(0f, rainBottom, WIDTH.toFloat(), rainBottom, stroke(inks.hairline, 2f))
            val peak = hours.maxByOrNull { it.precipChancePct ?: -1 }
            hours.forEachIndexed { i, hour ->
                val pct = hour.precipChancePct ?: return@forEachIndexed
                if (pct <= 0) return@forEachIndexed
                val x0 = i * cell + cell * 0.22f
                val x1 = (i + 1) * cell - cell * 0.22f
                val top = rainBottom - (RainRow - 8f) * pct / 100f
                canvas.drawRoundRect(RectF(x0, top.coerceAtMost(rainBottom - 5f), x1, rainBottom), 6f, 6f,
                    fill(inks.colors.rainAt(pct).toArgb()))
                if (hour == peak && pct >= RainLabelFloorPct) {
                    val paint = text(inks.colors.rainInkAt(pct).toArgb(), 26f, bold = true)
                    val cx = ((x0 + x1) / 2).coerceIn(30f, WIDTH - 30f)
                    canvas.drawText(Formats.percent(pct, locale), cx, top - 8f, paint)
                }
            }
        }
        hourLabels(canvas, hours.map { it.time }, cell, height - 14f, inks, is24h, locale)
        return bmp
    }

    // ---------------------------------------------------------------- the levels

    /**
     * The bulletin as the Dipartimento draws it: a row per hazard, a column per day, each
     * cell the level's word on the level's container — the verdict pairs of §2.3, whose
     * ink-on-container contrast is measured — and «nessuno» as a hairline pill in the quiet
     * ink, because green is the absence of a warning and not a fourth colour to decode.
     */
    fun levels(context: Context, warnings: PlaceWarnings, today: LocalDate, inks: Inks): Bitmap? {
        val days = warnings.days.take(MaxWarningDays)
        if (days.isEmpty()) return null
        val hazards = WarningHazard.displayOrder
        val rowH = 64f
        val head = 50f
        val height = (head + rowH * hazards.size).toInt() + 6
        val bmp = Bitmap.createBitmap(WIDTH, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val nameCol = 330f
        val col = (WIDTH - nameCol) / days.size
        days.forEachIndexed { d, day ->
            val label = when (day.date) {
                today -> context.getString(R.string.week_today)
                today.plusDays(1) -> context.getString(R.string.notif_chart_tomorrow)
                else -> Formats.dayLabel(day.date, Locale.getDefault())
            }
            canvas.drawText(label, nameCol + col * d + col / 2, 34f, text(inks.quiet, 28f, bold = true))
        }
        hazards.forEachIndexed { r, hazard ->
            val cy = head + rowH * r + rowH / 2
            val name = context.getString(WarningText.shortHazardRes(hazard))
                .replaceFirstChar { it.titlecase(Locale.getDefault()) }
            canvas.drawText(name, 6f, cy + 11f, text(inks.ink, 30f, bold = false, align = Paint.Align.LEFT))
            days.forEachIndexed { d, day ->
                val level = day.levels[hazard] ?: WarningLevel.NONE
                val rect = RectF(nameCol + col * d + 10f, cy - 24f, nameCol + col * (d + 1) - 10f, cy + 24f)
                val word = context.getString(WarningText.levelWordRes(level))
                val pair = when (level) {
                    WarningLevel.RED -> inks.colors.warningRed
                    WarningLevel.ORANGE -> inks.colors.warningOrange
                    WarningLevel.YELLOW -> inks.colors.warningYellow
                    WarningLevel.NONE -> null
                }
                if (pair == null) {
                    canvas.drawRoundRect(rect, 24f, 24f, stroke(inks.hairline, 2.5f))
                    canvas.drawText(word, rect.centerX(), cy + 10f, text(inks.quiet, 27f, bold = false))
                } else {
                    canvas.drawRoundRect(rect, 24f, 24f, fill(pair.container.toArgb()))
                    canvas.drawText(word, rect.centerX(), cy + 10f, text(pair.ink.toArgb(), 28f, bold = true))
                }
            }
        }
        return bmp
    }

    // ---------------------------------------------------------------- shared

    /** The hours' labels on the three-hour clock (00, 03 … 21), so the axis reads the same
     * whatever hour the chart happens to start at. */
    private fun hourLabels(
        canvas: Canvas,
        times: List<LocalDateTime>,
        cell: Float,
        baseline: Float,
        inks: Inks,
        is24h: Boolean,
        locale: Locale
    ) {
        val paint = text(inks.quiet, 28f, bold = false)
        times.forEachIndexed { i, t ->
            if (t.hour % 3 != 0) return@forEachIndexed
            val x = ((i + 0.5f) * cell).coerceIn(28f, WIDTH - 28f)
            canvas.drawText(Formats.hourLabel(t, is24h, locale), x, baseline, paint)
        }
    }

    /** A Catmull-Rom curve through [points]: the strip curve's own smoothing, so the
     * hours read as one line and not a row of kinks. */
    private fun smooth(points: List<Pair<Float, Float>>): Path = Path().apply {
        moveTo(points.first().first, points.first().second)
        for (i in 0 until points.lastIndex) {
            val p0 = points[(i - 1).coerceAtLeast(0)]
            val p1 = points[i]
            val p2 = points[i + 1]
            val p3 = points[(i + 2).coerceAtMost(points.lastIndex)]
            cubicTo(
                p1.first + (p2.first - p0.first) / 6f, p1.second + (p2.second - p0.second) / 6f,
                p2.first - (p3.first - p1.first) / 6f, p2.second - (p3.second - p1.second) / 6f,
                p2.first, p2.second
            )
        }
    }

    private fun fill(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.FILL
    }

    private fun stroke(color: Int, width: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.STROKE
        strokeWidth = width
    }

    private fun text(color: Int, size: Float, bold: Boolean, align: Paint.Align = Paint.Align.CENTER) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = size
            textAlign = align
            typeface = Typeface.create(Typeface.DEFAULT, if (bold) Typeface.BOLD else Typeface.NORMAL)
        }

    /** Twelve hours: from the afternoon into the night, which is the question a rain
     * alert raises — and the width at which each bar is still a bar at 340 dp. */
    const val RainHours = 12
    const val MinHours = 4
    private const val MaxWarningDays = 3
    private const val LabelRow = 44
    private const val PeakLabel = 48
    private const val RainRow = 60f

    /**
     * The pictures' heights at [WIDTH]: ~75 to ~90 dp at a notification's ~340 dp width.
     * The platform clips an expanded custom view at 256 dp, and the body around the picture
     * — title, headline, five lines of details — takes about 150 of them, so these are the
     * heights that leave the details whole with a margin to spare.
     */
    const val RainChartHeight = 220
    const val DayChartHeight = 264
    const val DayChartHeightDry = 200
    private const val AreaAlpha = 110
    /** The day chart labels its rain only when it is worth a figure: the headline's own
     * «possible» floor. */
    private const val RainLabelFloorPct = 30
}
