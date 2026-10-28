package com.callbackdev.chiaro.notifications

import android.content.Context
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
import com.callbackdev.chiaro.domain.model.Coordinates
import com.callbackdev.chiaro.domain.model.HourlyForecast
import com.callbackdev.chiaro.domain.settings.TemperatureUnit
import com.callbackdev.chiaro.domain.settings.WindSpeedUnit
import com.callbackdev.chiaro.domain.sky.AstronomyEngine
import com.callbackdev.chiaro.domain.warnings.PlaceWarnings
import com.callbackdev.chiaro.domain.warnings.WarningHazard
import com.callbackdev.chiaro.domain.warnings.WarningLevel
import com.callbackdev.chiaro.ui.components.ribbonColorStops
import com.callbackdev.chiaro.ui.format.Formats
import com.callbackdev.chiaro.ui.theme.ChiaroColors
import com.callbackdev.chiaro.ui.theme.ChiaroPalette
import com.callbackdev.chiaro.ui.theme.SkyPalette
import com.callbackdev.chiaro.ui.theme.paletteFor
import com.callbackdev.chiaro.ui.today.DaylightPhases
import com.callbackdev.chiaro.ui.warnings.WarningText
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
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
 * - **the gust's hours** under a gale alert (14.7, 27 Oct 2026) — the same twelve hours
 *   as bars of the strongest gust, the bar that makes them «Maltempo» drawn across them,
 *   the window lit, the strongest hour labelled in the reader's unit: a gale is when it
 *   rises and when it drops, which is the rain's own shape with another quantity;
 * - **the day** under the two summaries — the temperature as a curve coloured on the
 *   world scale, the rain under it, and under the hours the daylight ribbon of Oggi's hero
 *   (21 ott 2026), the same phases and the same forecast sky, where the night used to be
 *   boxes over the curve: the one picture of a day this app already draws;
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
 * The bitmaps are painted TWICE, once for each ground the shade can have, and the system
 * picks between them when it draws the notification (30 set 2026, device report):
 * [Themed], and the `-night` resources of `notification_expanded`. A picture painted for
 * the ground the app saw when it posted went on showing that ground whatever the shade
 * became: a morning summary was read on the pale day shade with the night's inks —
 * near-white figures and hours nobody could read, the night band a lavender box. The
 * ground at posting is not the ground at reading (a night schedule ending, a switch by
 * hand, a background process holding an old configuration), and a notification, unlike a
 * widget, is never repainted by the app; its layout IS re-inflated by the system on a
 * change of night mode, and that inflation is the one moment that knows which ground the
 * reader is looking at. The reader's dress (§2.5) is read once for both. A 1000 px wide image is laid out as ~340 dp at ~3×, so the type
 * sizes below are the dp of a 3× screen.
 */
internal object NotificationCharts {

    const val WIDTH = 1000

    /** The inks a chart is painted with: the reader's dress, on one of the two grounds
     * the system may give the notification. */
    data class Inks(
        val colors: ChiaroColors,
        /** The sky's own palette, for the ribbon under the day: the hero's colours of the light. */
        val sky: SkyPalette,
        val ink: Int,
        val quiet: Int,
        val hairline: Int,
        val night: Int,
        val band: Int,
        val severeBand: Int,
        val edge: Int,
        val dark: Boolean
    )

    /** One picture for each ground: the system shows [light] on a day shade and [dark]
     * on a night one, whenever it draws the notification. */
    class Themed(val light: Bitmap, val dark: Bitmap)

    /** Paints [draw] for both grounds; null when there is nothing to draw on either. */
    fun themed(context: Context, draw: (Inks) -> Bitmap?): Themed? {
        val palette = paletteFor(readerPalette(context))
        val light = draw(inks(palette, dark = false)) ?: return null
        val dark = draw(inks(palette, dark = true)) ?: return null
        return Themed(light, dark)
    }

    fun inks(context: Context, dark: Boolean): Inks = inks(paletteFor(readerPalette(context)), dark)

    private fun inks(palette: ChiaroPalette, dark: Boolean): Inks {
        val scheme = palette.scheme(dark)
        val colors = palette.colors(dark)
        return Inks(
            colors = colors,
            sky = palette.sky,
            ink = scheme.onSurface.toArgb(),
            quiet = scheme.onSurfaceVariant.toArgb(),
            hairline = scheme.outlineVariant.toArgb(),
            // The night as a cool tint of the primary, not a grey: the daylight ribbon's
            // night is blue, and a grey block reads as «no data».
            night = scheme.primary.copy(alpha = if (dark) 0.12f else 0.07f).toArgb(),
            band = scheme.primary.copy(alpha = if (dark) 0.16f else 0.10f).toArgb(),
            severeBand = colors.unstable.container.copy(alpha = if (dark) 0.55f else 0.65f).toArgb(),
            edge = scheme.onSurface.copy(alpha = if (dark) 0.24f else 0.16f).toArgb(),
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

    // ----------------------------------------------------------- the gust's hours

    /**
     * The next [RainHours] hours of gust from [from] (14.7), the gale's [window] lit behind
     * them in the storm's amber, the place's own [barKph] drawn as the dashed guide where
     * the rain's chart draws its 50%, the strongest hour labelled in [unit]. The scale is
     * the world's, 0 to [GustScaleKph]: the top of MeteoSwiss' lowland ladder, so that the
     * bar sits at half height and a gust the table calls a hurricane fills the chart. One
     * ink for the quantity: the gale's verdict is the band, the guide and the label, never
     * a colour. Null under [MinHours] hours or when none of them carries a gust at all.
     */
    fun gustHours(
        hours: List<HourlyForecast>,
        from: LocalDateTime,
        window: WindWindow?,
        barKph: Double,
        unit: WindSpeedUnit,
        inks: Inks,
        is24h: Boolean,
        locale: Locale
    ): Bitmap? {
        val shown = hours.filter { !it.time.isBefore(from.withMinute(0)) }.take(RainHours)
        if (shown.size < MinHours || shown.all { it.gustKph == null }) return null
        val height = RainChartHeight
        val bmp = Bitmap.createBitmap(WIDTH, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val cell = WIDTH.toFloat() / shown.size
        val labelTop = height - LabelRow
        val barTop = PeakLabel.toFloat()
        val barBottom = labelTop - 8f
        fun y(kph: Double) = barBottom - (barBottom - barTop) * (kph / GustScaleKph).coerceIn(0.0, 1.0).toFloat()

        window?.let { w ->
            val first = shown.indexOfFirst { !it.time.isBefore(w.start) }
            val last = shown.indexOfLast { !it.time.isAfter(w.end) }
            if (first >= 0 && last >= first) {
                canvas.drawRoundRect(
                    RectF(first * cell + 2, 0f, (last + 1) * cell - 2, barBottom), 18f, 18f,
                    fill(inks.severeBand)
                )
            }
        }

        // The baseline, the world's zero; the guide is the bar itself, not a half-way mark.
        canvas.drawLine(0f, barBottom, WIDTH.toFloat(), barBottom, stroke(inks.hairline, 2f))
        val guide = y(barKph)
        canvas.drawLine(0f, guide, WIDTH.toFloat(), guide, stroke(inks.hairline, 1.5f).apply {
            pathEffect = android.graphics.DashPathEffect(floatArrayOf(8f, 10f), 0f)
        })

        val peak = shown.maxByOrNull { it.gustKph ?: -1.0 }
        shown.forEachIndexed { i, hour ->
            val gust = hour.gustKph ?: return@forEachIndexed
            val x0 = i * cell + cell * 0.2f
            val x1 = (i + 1) * cell - cell * 0.2f
            val top = y(gust)
            if (gust > 0) {
                canvas.drawRoundRect(
                    RectF(x0, top.coerceAtMost(barBottom - 6f), x1, barBottom), 10f, 10f,
                    fill(if (gust >= barKph) inks.ink else inks.quiet)
                )
            }
            if (hour == peak && gust > 0) {
                text(inks.ink, 30f, bold = true).let {
                    // The number alone over the bar («11 Bft»): the word is in the body.
                    canvas.drawText(Formats.wind(gust, unit, locale, short = true), (x0 + x1) / 2, top - 12f, it)
                }
            }
        }
        hourLabels(canvas, shown.map { it.time }, cell, height - 14f, inks, is24h, locale)
        return bmp
    }

    // ---------------------------------------------------------------------- the day

    /**
     * A day as the Today screen draws it: the temperature as a curve, each stretch coloured
     * by where it stands on the world's scale (§2.3, −5 to 35 °C), its high and its low
     * marked; the rain chance as a row of bars under it on its own 0–100%; and under the
     * hours the day's light, the daylight ribbon of the hero ([ribbon]), for the part of
     * [date] the hours cover. Null under [MinHours] hours.
     */
    fun day(
        hours: List<HourlyForecast>,
        date: LocalDate,
        zone: ZoneId,
        coords: Coordinates,
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
        val ribbonBottom = labelTop - RibbonGap
        val ribbonTop = ribbonBottom - RibbonRow
        val rainBottom = ribbonTop - 8f
        val rainTop = rainBottom - RainRow
        val curveTop = 46f
        // Room under the curve for the low's figure, which hangs below its dot: clear of
        // the rain row, and of the ribbon when there is no rain.
        val curveBottom = (if (rain) rainTop - 26f else ribbonTop - 34f)

        // The day's light under the hours (21 ott 2026): the hero's own ribbon, as the
        // chart's baseline. Until then the night was a rounded lavender box over the curve
        // at each end of the day, the one element of a notification with no sibling on any
        // screen; the ribbon is what every screen says a day's light looks like.
        ribbon(canvas, hours, date, zone, coords, inks, RectF(0f, ribbonTop, WIDTH.toFloat(), ribbonBottom))

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
        // A faint edge of the ground's ink under the line: the scale's mild end is a pale
        // peach by day and a dim brown by night, and on a shade the app does not paint —
        // grey and see-through on some phones — it thinned into the ground. The edge keeps
        // the line's shape without touching its colour, which stays the world's.
        canvas.drawPath(curve, stroke(inks.edge, 12f).apply { strokeCap = Paint.Cap.ROUND })
        // An opaque base colour under the shader: a paint's alpha is its colour's, and a
        // shader on a transparent paint draws nothing at all.
        canvas.drawPath(
            curve,
            stroke(android.graphics.Color.BLACK, 7f).apply {
                shader = gradient
                strokeCap = Paint.Cap.ROUND
            }
        )

        // The rain's peak and its label, decided before the curve's figures: the low's figure
        // must not land on it (21 ott 2026, a low at 08 under a peak at 09: «16°» over «80%»).
        val peak = if (rain) hours.maxByOrNull { it.precipChancePct ?: -1 } else null
        val peakPct = peak?.precipChancePct ?: 0
        val peakLabel: Pair<Float, Float>? = peak?.takeIf { peakPct >= RainLabelFloorPct }?.let { hour ->
            val i = hours.indexOf(hour)
            val x0 = i * cell + cell * 0.22f
            val x1 = (i + 1) * cell - cell * 0.22f
            val top = rainBottom - (RainRow - 8f) * peakPct / 100f
            ((x0 + x1) / 2).coerceIn(30f, WIDTH - 30f) to top - 8f
        }
        val peakLabelBox = peakLabel?.let { (cx, baseline) ->
            val width = text(0, RainLabelSize, bold = true).measureText(Formats.percent(peakPct, locale))
            RectF(cx - width / 2, baseline - RainLabelSize, cx + width / 2, baseline + 4f)
        }

        // The high and the low, each a dot on the curve and its figure beside it: the high
        // above its dot, where the curve cannot be; the low below and to the side of its
        // dot, where the curve cannot be either. Straight below, the low's figure landed on
        // the first hour's label whenever the day's low was its first hour — the morning
        // summary's usual shape (device report, 30 set 2026: «17°» over «09»). Which side,
        // [lowLabelRight] says: the right unless the figure would leave the picture or land
        // on the rain's label, then the left.
        val hiIndex = temps.indexOf(high)
        val loIndex = temps.indexOf(low)
        listOf(hiIndex to true, loIndex to false).distinctBy { it.first }.forEach { (i, isHigh) ->
            val (x, yy) = points[i]
            canvas.drawCircle(x, yy, 9f, fill(inks.colors.temperatureAt(temps[i]).toArgb()))
            canvas.drawCircle(x, yy, 9f, stroke(inks.ink, 2.5f))
            val label = Formats.temperature(temps[i], unit, locale)
            if (isHigh) {
                canvas.drawText(label, x.coerceIn(40f, WIDTH - 40f), yy - 20f, text(inks.ink, 34f, bold = true))
            } else {
                val width = text(inks.ink, 34f, bold = true).measureText(label)
                val right = lowLabelRight(x, yy + 30f, width, peakLabelBox)
                val paint = text(inks.ink, 34f, bold = true, align = if (right) Paint.Align.LEFT else Paint.Align.RIGHT)
                canvas.drawText(label, if (right) x + LowLabelGap else x - LowLabelGap, yy + 30f, paint)
            }
        }

        if (rain) {
            hours.forEachIndexed { i, hour ->
                val pct = hour.precipChancePct ?: return@forEachIndexed
                if (pct <= 0) return@forEachIndexed
                val x0 = i * cell + cell * 0.22f
                val x1 = (i + 1) * cell - cell * 0.22f
                val top = rainBottom - (RainRow - 8f) * pct / 100f
                canvas.drawRoundRect(RectF(x0, top.coerceAtMost(rainBottom - 5f), x1, rainBottom), 6f, 6f,
                    fill(inks.colors.rainAt(pct).toArgb()))
            }
            peakLabel?.let { (cx, baseline) ->
                val paint = text(inks.colors.rainInkAt(peakPct).toArgb(), RainLabelSize, bold = true)
                canvas.drawText(Formats.percent(peakPct, locale), cx, baseline, paint)
            }
        }
        hourLabels(canvas, hours.map { it.time }, cell, height - 14f, inks, is24h, locale)
        return bmp
    }

    /**
     * The low's figure goes to the right of its dot unless it would leave the picture or
     * land on the rain's label ([avoid], the peak's percent), then to the left; when neither
     * side is clear, the side that is at least inside the picture. Pure, for its test.
     * [x] and [baseline] are the figure's anchor; [width] its measured width.
     */
    internal fun lowLabelRight(x: Float, baseline: Float, width: Float, avoid: RectF?): Boolean {
        val top = baseline - 34f
        val bottom = baseline + 8f
        val right = RectF(x + LowLabelGap, top, x + LowLabelGap + width, bottom)
        val left = RectF(x - LowLabelGap - width, top, x - LowLabelGap, bottom)
        val rightFits = right.right <= WIDTH - 8f
        val leftFits = left.left >= 8f
        fun clear(box: RectF) = avoid == null || !RectF.intersects(box, avoid)
        return when {
            rightFits && clear(right) -> true
            leftFits && clear(left) -> false
            else -> rightFits
        }
    }

    /**
     * The day's light as the hero's ribbon paints it (`DaylightRibbon`, DESIGN §4): the
     * solar day's phases for [date] at this place, the forecast's cloud and chance tinting
     * them hour by hour, in the sky palette's own colours. The ribbon is a whole day; [rect]
     * shows the stretch the [hours] cover, so the gradient is laid out over the day's full
     * width and the picture shows its window of it.
     */
    private fun ribbon(
        canvas: Canvas,
        hours: List<HourlyForecast>,
        date: LocalDate,
        zone: ZoneId,
        coords: Coordinates,
        inks: Inks,
        rect: RectF
    ) {
        val phases = DaylightPhases.phases(AstronomyEngine.solarDay(date, zone, coords), zone)
        val stops = ribbonColorStops(phases, DaylightPhases.sky(hours, date), nightFade = null) { altitude, cloud, chance ->
            inks.sky.gradient(altitude, cloud, chance).mid
        }
        if (stops.size < 2) return
        val from = DaylightPhases.fraction(hours.first().time)
        val until = DaylightPhases.fraction(hours.last().time) + 1f / 24f
        val span = until - from
        if (span <= 0f) return
        fun x(t: Float) = (t - from) / span * WIDTH
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                x(0f), 0f, x(1f), 0f,
                stops.map { it.second.toArgb() }.toIntArray(),
                stops.map { it.first }.toFloatArray(),
                Shader.TileMode.CLAMP
            )
        }
        val radius = rect.height() / 2
        canvas.drawRoundRect(rect, radius, radius, paint)
    }

    /** Where the ribbon's middle runs in a day picture of [height] px, for the tests' eye. */
    internal fun ribbonCenterY(height: Int): Float = height - LabelRow - RibbonGap - RibbonRow / 2f

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

    /** The gust chart's top (14.7): 140 km/h, MeteoSwiss' level 5 in the lowlands. */
    const val GustScaleKph = 140.0
    private const val MaxWarningDays = 3
    private const val LabelRow = 44
    private const val PeakLabel = 48
    private const val RainRow = 60f

    /**
     * The pictures' heights at [WIDTH]: ~65 to ~80 dp at a notification's ~292 dp width.
     * The platform clips an expanded custom view at 256 dp, and the body around the picture
     * — title, headline, the lines of details — takes the rest; `NotificationFit` counts
     * how many lines a picture of this height leaves room for.
     */
    const val RainChartHeight = 220
    const val DayChartHeight = 272
    const val DayChartHeightDry = 222
    private const val AreaAlpha = 110
    /** The ribbon under the day's hours (6 dp at 3×) and its air from the labels. */
    private const val RibbonRow = 18f
    private const val RibbonGap = 6f
    /** The low's figure stands this far from its dot, to the right or to the left. */
    private const val LowLabelGap = 12f
    /** The rain's peak label, px: 26 at 3× is ~9 dp. */
    private const val RainLabelSize = 26f
    /** The day chart labels its rain only when it is worth a figure: the headline's own
     * «possible» floor. */
    private const val RainLabelFloorPct = 30
}
