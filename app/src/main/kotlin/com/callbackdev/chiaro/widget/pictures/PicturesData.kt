package com.callbackdev.chiaro.widget.pictures

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.text.StaticLayout
import android.text.TextPaint
import android.util.TypedValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.domain.pictures.PictureHour
import com.callbackdev.chiaro.domain.pictures.PictureSpan
import com.callbackdev.chiaro.domain.pictures.PictureWindow
import com.callbackdev.chiaro.domain.WeatherStateEngine
import com.callbackdev.chiaro.domain.settings.TemperatureUnit
import com.callbackdev.chiaro.ui.components.ribbonColorStops
import com.callbackdev.chiaro.ui.format.Formats
import com.callbackdev.chiaro.ui.theme.PicturesInks
import com.callbackdev.chiaro.ui.theme.SkyPalette
import com.callbackdev.chiaro.ui.today.TodayUiState
import com.callbackdev.chiaro.ui.today.WeekDay
import com.callbackdev.chiaro.ui.warnings.WarningText
import com.callbackdev.chiaro.widget.PicturesMeasure
import com.callbackdev.chiaro.widget.PicturesPiece
import com.callbackdev.chiaro.widget.WidgetBackground
import com.callbackdev.chiaro.widget.WidgetInk
import com.callbackdev.chiaro.widget.WidgetLook
import com.callbackdev.chiaro.widget.WidgetModel
import com.callbackdev.chiaro.widget.WidgetSchemes
import com.callbackdev.chiaro.widget.effectiveWidgetBackground
import com.callbackdev.chiaro.widget.InkTrustFloorPct
import com.callbackdev.chiaro.widget.isNight
import com.callbackdev.chiaro.widget.markerText
import com.callbackdev.chiaro.widget.marked
import com.callbackdev.chiaro.widget.placeLineWidth
import com.callbackdev.chiaro.widget.sentence
import com.callbackdev.chiaro.widget.wallpaperWantsDarkInk
import com.callbackdev.chiaro.widget.widgetInk
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

/**
 * The reader's settings for «In immagini» (Fase 17, «Le impostazioni»): what the drawing spans
 * («Il disegno») and what is on the card («Sulla card»). The defaults are the canvas's; step
 * 17.3 stores them per widget.
 */
internal data class PicturesLook(
    val span: PictureSpan = PictureSpan.NEXT_24_HOURS,
    val glyph: Boolean = true,
    val rain: Boolean = true,
    val ribbon: Boolean = true,
    val days: Boolean = true,
    val warnings: Boolean = true
) {
    companion object {
        /** The reader's settings as the store keeps them ([WidgetLook]): three of their own,
         * and the family's switches for the glyph, the days and the warning. */
        fun of(look: WidgetLook) = PicturesLook(
            span = look.picturesSpan,
            glyph = look.showIcon,
            rain = look.picturesRain,
            ribbon = look.picturesRibbon,
            days = look.showLater,
            warnings = look.showWarning
        )
    }
}

/**
 * Everything the card prints, in words, built once off a [Context] so the card, its
 * description and its measurement say the same things. A piece the card has nothing for is
 * empty, and an empty piece is not drawn.
 */
internal class PicturesTexts(
    val pieces: Map<PicturesPiece, String>,
    /** The range in the reader's unit, «↑12° ↓6°», or empty. */
    val range: String
) {
    operator fun get(piece: PicturesPiece): String = pieces[piece].orEmpty()

    companion object {
        fun of(
            context: Context,
            model: WidgetModel,
            content: TodayUiState.Content,
            window: PictureWindow,
            hours: List<PictureHour>,
            look: PicturesLook,
            now: Instant
        ): PicturesTexts {
            val locale = Locale.getDefault()
            val unit = model.settings.units.temperature
            val full = fullSentence(context, content, model)
            val short = sentence(context, content, model.settings.units)
            val inside = hours.filter { it.at in window && it.temperatureC != null }
            val range = if (inside.isEmpty()) "" else context.getString(
                R.string.pictures_range,
                Formats.temperature(inside.maxOf { it.temperatureC!! }, unit, locale),
                Formats.temperature(inside.minOf { it.temperatureC!! }, unit, locale)
            )
            val place = model.city?.name.orEmpty()
            val warning = model.warning?.takeIf { look.warnings }?.let { context.getString(WarningText.phraseRes(it.maxLevel)) }
            val pieces = buildMap {
                put(PicturesPiece.Number, Formats.temperature(content.report.current.tempC, unit, locale))
                put(PicturesPiece.Sentence, full)
                put(PicturesPiece.ShortSentence, short)
                put(PicturesPiece.Place, place)
                if (range.isNotEmpty()) {
                    put(PicturesPiece.PlaceAndRange, context.getString(R.string.pictures_place_and_range, place, range))
                    put(PicturesPiece.Range, range)
                } else {
                    put(PicturesPiece.PlaceAndRange, place)
                }
                if (content.marked) put(PicturesPiece.Stale, markerText(context, content, now))
                if (warning != null) put(PicturesPiece.Warning, warning)
            }
            return PicturesTexts(pieces, range)
        }

        /** The day's sentence in Today's own register — «Pioggia dalle 21, smette all'alba» —
         * where the short one is «In parole»'s brief register: the canvas's two sentences. */
        private fun fullSentence(context: Context, content: TodayUiState.Content, model: WidgetModel): String {
            val timeFmt = Formats.timeFormatter(android.text.format.DateFormat.is24HourFormat(context), Locale.getDefault())
            return com.callbackdev.chiaro.ui.today.HeadlineText.of(
                context, content.headline, timeFmt, model.settings.units, brief = false
            ) ?: sentence(context, content, model.settings.units)
        }
    }
}

/**
 * The text measured as the launcher will set it (`Typeface.DEFAULT`, the system face; Bold for
 * the number, Medium for the sentence and the warning's word, as Glance resolves them), at the
 * reader's scale through [TypedValue] so a non-linear scale is the one measured.
 */
internal class DevicePicturesMeasure(
    private val context: Context,
    private val texts: PicturesTexts,
    private val fromGps: Boolean
) : PicturesMeasure {

    override fun width(piece: PicturesPiece, sp: Float): Dp {
        val text = texts[piece]
        if (text.isEmpty()) return 0.dp
        if (piece == PicturesPiece.Place) return placeLineWidth(context, text, fromGps, sp)
        val paint = paint(piece, sp)
        return (paint.measureText(text) / context.resources.displayMetrics.density).dp
    }

    override fun lines(piece: PicturesPiece, sp: Float, width: Dp): Int {
        val text = texts[piece]
        if (text.isEmpty()) return 0
        val px = (width.value * context.resources.displayMetrics.density).toInt().coerceAtLeast(1)
        return StaticLayout.Builder.obtain(text, 0, text.length, paint(piece, sp), px)
            .setIncludePad(true)
            .build()
            .lineCount
    }

    private fun paint(piece: PicturesPiece, sp: Float) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = when (piece) {
            PicturesPiece.Number -> Typeface.DEFAULT_BOLD
            PicturesPiece.Sentence, PicturesPiece.ShortSentence, PicturesPiece.Warning -> MediumFace
            else -> Typeface.DEFAULT
        }
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, context.resources.displayMetrics)
    }

    private companion object {
        val MediumFace: Typeface by lazy { Typeface.create("sans-serif-medium", Typeface.NORMAL) }
    }
}

/**
 * Where a text's box must start for its first baseline to land where the canvas's does: the
 * canvas sets a line as CSS does (the face's ascent and descent centred in the line box), a
 * `TextView` with its font padding on puts the baseline under the face's `top`. The difference,
 * measured on the face the launcher draws in, is how much higher (negative) or lower the Glance
 * box goes than the canvas's line box. For 46 sp in a 48 dp line it is about −9 dp: the number's
 * box is 61 dp tall and starts above the canvas's.
 */
internal fun textShift(context: Context, sp: Float, line: Dp, bold: Boolean = false): Dp {
    val metrics = context.resources.displayMetrics
    val paint = Paint().apply {
        typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, metrics)
    }
    val m = paint.fontMetrics
    val css = (line.value * metrics.density - (m.descent - m.ascent)) / 2f - m.ascent
    val android = -m.top
    return ((css - android) / metrics.density).dp
}

/** The plate's inks for the ground this card really has: the same decision [widgetInk]
 * makes for its words, so the drawing and the text can never disagree about the ground. */
internal fun picturesInks(context: Context, model: WidgetModel, schemes: WidgetSchemes, hasSky: Boolean): PicturesInks {
    val look = model.look
    val effective = effectiveWidgetBackground(look, hasSky)
    val delegates = effective == WidgetBackground.SKY || effective == WidgetBackground.SYSTEM
    val wallpaperDark = look.opacityPct < InkTrustFloorPct && delegates && wallpaperWantsDarkInk(context)
    val l = schemes.light
    val d = schemes.dark
    return when (widgetInk(effective, look.opacityPct, isNight(context), wallpaperDark)) {
        WidgetInk.OVER_SKY -> if (effective == WidgetBackground.COLOR) PicturesInks.Card else PicturesInks.Sky
        WidgetInk.ON_LIGHT -> PicturesInks.light(l.onSurface, l.onSurfaceVariant, l.surface)
        WidgetInk.ON_DARK -> PicturesInks.dark(d.onSurface, d.onSurfaceVariant, d.surface)
    }
}

/** The report's hours, as the drawings read them. */
internal fun pictureHours(content: TodayUiState.Content): List<PictureHour> =
    content.report.hourly.map(PictureHour::of)

/**
 * The ribbon of light over [window], as (fraction of the window, colour) stops: each day the
 * window crosses with its own phases and its own forecast sky ([ribbonColorStops], the hero's
 * function and the canvas's colours: `SkyPalette.gradient(…).mid`), cut to the window.
 */
internal fun windowRibbon(window: PictureWindow, days: List<WeekDay>, table: SkyPalette, zone: ZoneId): List<Pair<Float, Color>> {
    val stops = mutableListOf<Pair<Float, Color>>()
    days.forEach { day ->
        val start = day.forecast.date.atStartOfDay(zone).toInstant()
        val end = day.forecast.date.plusDays(1).atStartOfDay(zone).toInstant()
        if (!end.isAfter(window.start) || !start.isBefore(window.end)) return@forEach
        val length = Duration.between(start, end).toMillis().toDouble()
        ribbonColorStops(day.phases, day.sky, nightFade = null) { alt, cloud, chance ->
            table.gradient(alt, cloud, chance).mid
        }.forEach { (f, color) ->
            val at = start.plusMillis((f * length).toLong())
            stops += window.fraction(at).toFloat() to color
        }
    }
    if (stops.isEmpty()) return emptyList()
    val sorted = stops.sortedBy { it.first }
    // Cut to the window: the stop at each edge is the colour there, interpolated.
    fun colorAt(t: Float): Color {
        val after = sorted.indexOfFirst { it.first >= t }
        if (after < 0) return sorted.last().second
        if (after == 0) return sorted.first().second
        val (a, ca) = sorted[after - 1]
        val (b, cb) = sorted[after]
        val u = if (b > a) (t - a) / (b - a) else 0f
        return Color(
            ca.red + (cb.red - ca.red) * u, ca.green + (cb.green - ca.green) * u,
            ca.blue + (cb.blue - ca.blue) * u, ca.alpha + (cb.alpha - ca.alpha) * u
        )
    }
    val inside = sorted.filter { it.first > 0f && it.first < 1f }
    return listOf(0f to colorAt(0f)) + inside + (1f to colorAt(1f))
}

/** Today's ribbon round the dial: its stops as fractions of the day. */
internal fun dayRing(today: WeekDay?, table: SkyPalette): List<Pair<Float, Color>> =
    today?.let { day ->
        ribbonColorStops(day.phases, day.sky, nightFade = null) { alt, cloud, chance -> table.gradient(alt, cloud, chance).mid }
    }.orEmpty()

/** A tick's label: «ora» for the hour we are in, the clock's hour otherwise — «17», or «5 PM». */
internal fun tickLabel(context: Context, at: Instant, zone: ZoneId, isNow: Boolean): String {
    if (isNow) return context.getString(R.string.pictures_tick_now)
    val time = at.atZone(zone).toLocalDateTime()
    return if (android.text.format.DateFormat.is24HourFormat(context)) {
        time.hour.toString()
    } else {
        Formats.hourLabel(time, false, Locale.getDefault())
    }
}

/** A chance worth printing: from 20%, as everywhere (STATES §7). */
internal fun chanceLabel(chance: Int?): String? =
    chance?.takeIf { it >= WeatherStateEngine.PROBABLE_PCT }?.let { Formats.percent(it, Locale.getDefault()) }

/** A temperature in the reader's unit. */
internal fun degrees(celsius: Double, unit: TemperatureUnit): String =
    Formats.temperature(celsius, unit, Locale.getDefault())

/**
 * The drawing in words, for TalkBack (DESIGN §9.3: an image with its equivalent in text): what
 * the curve says — now, the high and the low with their hours — and what the rain says — from
 * when to when it is likely, and its most likely hour. The same facts the picture draws, in
 * the order it is read.
 */
internal fun pictureDescription(
    context: Context,
    window: PictureWindow,
    hours: List<PictureHour>,
    unit: TemperatureUnit,
    now: String,
    rain: Boolean
): String {
    val inside = hours.filter { it.at in window }
    val known = inside.filter { it.temperatureC != null }
    val clock = Formats.timeFormatter(android.text.format.DateFormat.is24HourFormat(context), Locale.getDefault())
    fun time(at: Instant) = at.atZone(window.zone).toLocalTime().format(clock)
    val curve = if (known.isEmpty()) null else {
        val high = known.maxBy { it.temperatureC!! }
        val low = known.minBy { it.temperatureC!! }
        context.getString(
            if (window.span == PictureSpan.DAY) R.string.pictures_desc_day else R.string.pictures_desc_next,
            now,
            degrees(high.temperatureC!!, unit), time(high.at),
            degrees(low.temperatureC!!, unit), time(low.at)
        )
    }
    val wet = inside.filter { (it.chancePct ?: 0) >= WeatherStateEngine.PROBABLE_PCT && !it.at.isBefore(window.nowHour) }
    val rainLine = when {
        !rain -> null
        wet.isEmpty() -> context.getString(R.string.pictures_desc_dry)
        else -> {
            val peak = wet.maxBy { it.chancePct ?: 0 }
            context.getString(
                R.string.pictures_desc_rain,
                time(wet.first().at), time(wet.last().at.plus(Duration.ofHours(1))),
                Formats.percent(peak.chancePct ?: 0, Locale.getDefault()), time(peak.at)
            )
        }
    }
    return listOfNotNull(curve, rainLine).joinToString(" ")
}
