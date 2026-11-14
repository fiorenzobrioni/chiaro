package com.callbackdev.chiaro.widget

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.unit.ColorProvider as FixedColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.domain.sky.AstronomyEngine
import com.callbackdev.chiaro.ui.format.Formats
import com.callbackdev.chiaro.ui.format.LocalClock
import com.callbackdev.chiaro.ui.icons.ChiaroIcons
import com.callbackdev.chiaro.ui.theme.ArcPalette
import com.callbackdev.chiaro.ui.today.TodayUiState
import java.time.LocalTime

/*
 * «In parole» with its ring: the forms [RingWordsLayout] decides, drawn. See that file for the
 * ranks and every number's reason; this one only draws them.
 */

/**
 * The inks the ring paints with, resolved once for the ground the card really has, by the same
 * rule as its words ([widgetInk]): white over the sky or a colour, the scheme's own on a plain
 * card. The disc is the sun's amber and the moon's pale ([ArcPalette]), depictions that keep one
 * value on every ground.
 */
internal fun daylightRingInks(
    context: Context,
    look: WidgetLook,
    schemes: WidgetSchemes,
    hasSky: Boolean
): DaylightRingInks {
    val background = effectiveWidgetBackground(look, hasSky)
    val night = isNight(context)
    val delegates = background == WidgetBackground.SKY || background == WidgetBackground.SYSTEM
    val bright = look.opacityPct < InkTrustFloorPct && delegates && wallpaperWantsDarkInk(context)
    val ink = when (widgetInk(background, look.opacityPct, night, bright)) {
        WidgetInk.OVER_SKY -> Color.White
        WidgetInk.ON_LIGHT -> schemes.light.onSurface
        WidgetInk.ON_DARK -> schemes.dark.onSurface
    }
    val solid = look.opacityPct >= InkTrustFloorPct
    val halo = when {
        !solid || background == WidgetBackground.SKY -> null
        else -> widgetCardFill(background, schemes, night, 1f, look.cardColor)
    }
    return DaylightRingInks(
        ink = ink,
        mid = ink.copy(alpha = MID_ALPHA),
        track = ink.copy(alpha = TRACK_ALPHA),
        sun = ArcPalette.Sun,
        moon = ArcPalette.MoonLit,
        halo = halo
    )
}

private const val MID_ALPHA = 0.5f
private const val TRACK_ALPHA = 0.2f

/** The card's insets for each form: the cell's own air, a row's snug top and bottom. */
internal fun ringCardPadding(form: RingForm?): Pair<Dp, Dp> = when (form) {
    RingForm.CELL -> RingCellInset to RingCellInset
    RingForm.ROW -> WidgetCardPadding to WidgetCardPaddingSnug
    else -> WidgetCardPadding to WidgetCardPadding
}

/** The ring card at [size], inside [WidgetCard]. */
@Composable
internal fun RingWordsContent(
    content: TodayUiState.Content,
    model: WidgetModel,
    palette: WidgetPalette,
    inks: DaylightRingInks,
    size: DpSize
) {
    val context = LocalContext.current
    val scale = fontScale(context)
    val parts = RingParts(context, content, model, palette, inks, scale)
    when (ringForm(size, scale)) {
        RingForm.CELL -> Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Ring(parts, ringCellCircle(size), cell = true)
        }
        RingForm.ROW -> RingRow(parts, size.width, ringRowCircle(size), GlanceModifier.fillMaxSize())
        RingForm.TALL -> RingTall(parts, size)
        RingForm.PANEL -> RingPanel(parts, size)
    }
}

/** What every form draws from, read once. */
private class RingParts(
    val context: Context,
    val content: TodayUiState.Content,
    val model: WidgetModel,
    val palette: WidgetPalette,
    val inks: DaylightRingInks,
    val scale: Float
) {
    private val locale = context.resources.configuration.locales[0]
    val temperature: String =
        Formats.temperature(content.report.current.tempC, model.settings.units.temperature, locale)
    val sentence: String? = sentence(context, content, model.settings.units).takeIf { model.look.showSentence }
    private val coords = content.report.location.coordinates
    private val today = content.now.toLocalDate()
    val solarDay = AstronomyEngine.solarDay(today, content.zone, coords)
    val spec = DaylightRingSpec(
        daylight = daylight(content.sky.phases),
        now = content.sky.nowFraction,
        sunUp = content.sky.sunAltitudeDeg > 0.0
    )
    val range = content.week.firstOrNull()?.forecast?.takeIf { model.look.showDayRange }

    /** The ring in words: when the sun rises and sets today, or that it does not. */
    val description: String = run {
        val time = Formats.timeFormatter(DateFormat.is24HourFormat(context), locale)
        val rise = solarDay.sunrise
        val set = solarDay.sunset
        when {
            solarDay.sunUpAllDay -> context.getString(R.string.widget_ring_desc_up)
            rise == null || set == null -> context.getString(R.string.widget_ring_desc_down)
            else -> context.getString(
                R.string.widget_ring_desc,
                time.format(rise.atZone(content.zone)),
                time.format(set.atZone(content.zone))
            )
        }
    }

    /** The sun's height at the middle of each hour of today, in degrees: the panel's bars. */
    fun sunHours(): List<Double> = (0 until HOURS).map { hour ->
        val at = today.atTime(LocalTime.of(hour, HALF_HOUR)).atZone(content.zone).toInstant()
        AstronomyEngine.sunAltitude(at, coords)
    }
}

/**
 * The ring at [side]: the day's light, and inside it the sky of the hour (the glyph of the
 * hour's state, the app's own), or, on one cell, the temperature, as every ring card of the
 * family holds its figure there.
 */
@Composable
private fun Ring(parts: RingParts, side: Dp, cell: Boolean = false) {
    val density = parts.context.resources.displayMetrics.density
    val bitmap = remember(side, density, parts.spec, parts.inks) {
        DaylightRingPainter.paint(side, density, parts.spec, parts.inks)
    }
    Box(modifier = GlanceModifier.size(side), contentAlignment = Alignment.Center) {
        Image(
            provider = ImageProvider(bitmap),
            contentDescription = parts.description,
            modifier = GlanceModifier.size(side)
        )
        if (cell) {
            val em = measureWidgetText(parts.context, parts.temperature, 100f, medium = true).value / 100f
            ringInsideSp(side, em * BOLD_OVER_MEDIUM, parts.scale)?.let { sp ->
                Text(
                    text = parts.temperature,
                    style = TextStyle(color = parts.palette.primary, fontSize = sp.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1
                )
            }
        } else {
            val hole = side - DaylightRingPainter.strokeOf(side) * 2
            Image(
                provider = ImageProvider(
                    ChiaroIcons.conditionRes(
                        parts.content.report.current.condition.wmoCode,
                        parts.content.night,
                        parts.model.iconStyle,
                        parts.palette.darkGround
                    )
                ),
                contentDescription = null,
                modifier = GlanceModifier.size(hole * RingGlyphShare)
            )
        }
    }
}

@Composable
private fun Hero(parts: RingParts, sp: Float = RingHeroSp) {
    Text(
        text = parts.temperature,
        style = TextStyle(color = parts.palette.primary, fontSize = sp.sp, fontWeight = FontWeight.Bold),
        maxLines = 1
    )
}

@Composable
private fun Place(parts: RingParts, modifier: GlanceModifier = GlanceModifier) {
    PlaceLine(
        name = parts.content.city.name,
        fromGps = parts.model.fromGps,
        palette = parts.palette,
        size = RingFactSp.sp,
        modifier = modifier
    )
}

@Composable
private fun Range(parts: RingParts) {
    val day = parts.range ?: return
    DayRange(day.highC, day.lowC, parts.model.settings.units, parts.palette, size = RingFactSp.sp, marks = true)
}

@Composable
private fun Sentence(parts: RingParts, lines: Int, align: TextAlign, modifier: GlanceModifier = GlanceModifier) {
    val text = parts.sentence ?: return
    Text(
        text = text,
        style = TextStyle(
            color = parts.palette.primary,
            fontSize = RingFactSp.sp,
            fontWeight = FontWeight.Medium,
            textAlign = align
        ),
        maxLines = lines,
        modifier = modifier
    )
}

/** With old data the card says how old, on every form but one cell (VISION §5.9). */
@Composable
private fun Stale(parts: RingParts) {
    if (!parts.content.marked) return
    Text(
        text = markerText(parts.context, parts.content, java.time.Instant.now(LocalClock.current)),
        style = TextStyle(color = parts.palette.stale, fontSize = TextStaleSp.sp),
        maxLines = 1
    )
}

/**
 * One row (and the panel's header, at its height): the ring at the leading edge, the temperature
 * over the place, and the day's sentence at the far edge where the row has a column for it.
 */
@Composable
private fun RingRow(parts: RingParts, width: Dp, ring: Dp, modifier: GlanceModifier) {
    val column = if (parts.sentence != null) ringRowSentenceColumn(width, ring) else 0.dp
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Ring(parts, ring)
        Spacer(modifier = GlanceModifier.width(RingGap))
        Column(modifier = GlanceModifier.defaultWeight()) {
            Hero(parts)
            Place(parts)
            Stale(parts)
        }
        if (column > 0.dp) {
            Spacer(modifier = GlanceModifier.width(RingGap))
            Sentence(parts, RingRowSentenceLines, TextAlign.End, GlanceModifier.width(column))
        }
    }
}

/**
 * Two rows and up: the ring in the top trailing corner, the place beside it as the eyebrow;
 * from the bottom leading corner, the temperature, the day's range (what it is, quiet) and the
 * day's sentence: the family's order.
 */
@Composable
private fun RingTall(parts: RingParts, size: DpSize) {
    val ring = ringTallCircle(size, parts.scale, parts.sentence != null)
    val inner = size.width - WidgetCardPadding * 2
    Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.TopEnd) {
        Ring(parts, ring)
        Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.TopStart) {
            Place(parts, GlanceModifier.width(inner - ring - EyebrowGap))
        }
        Column(modifier = GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.Bottom) {
            Hero(parts)
            Range(parts)
            Sentence(parts, RingTallSentenceLines, TextAlign.Start)
            Stale(parts)
        }
    }
}

/**
 * Two rows and three cells and up: the row on top at the header's height, its ring the tall
 * card's, and under it the sun hour by hour.
 */
@Composable
private fun RingPanel(parts: RingParts, size: DpSize) {
    val header = ringPanelHeader(size, parts.scale)
    val strip = ringPanelStrip(size, parts.scale)
    val inner = size.width - WidgetCardPadding * 2
    Column(modifier = GlanceModifier.fillMaxSize()) {
        RingRow(parts, size.width, ringTallCircle(size, parts.scale), GlanceModifier.fillMaxWidth().height(header))
        Spacer(modifier = GlanceModifier.defaultWeight())
        SunHours(parts, inner, strip)
    }
}

/**
 * Today's sun hour by hour (committente, 14 Nov 2026): 24 bars, each the sun's height at the
 * middle of its hour on the day's own scale, the night a flat line, as the family's ring cards
 * draw their day under the ring. The hours gone in the strong ink at half strength, the hour we
 * are in in the sun's amber (the moon's pale at night), the hours to come faint. A depiction of
 * the astronomy the app computes, never a forecast number.
 */
@Composable
private fun SunHours(parts: RingParts, width: Dp, height: Dp) {
    val hours = parts.sunHours()
    val highest = hours.maxOrNull()?.takeIf { it > 0.0 } ?: 1.0
    val labels = ringStripLabels(parts.scale)
    val bars = height - labels
    val bar = (width - BarGap * (HOURS - 1)) / HOURS
    val now = parts.content.now.hour
    val ink = parts.inks.ink
    Column(
        modifier = GlanceModifier.width(width).height(height).semantics { contentDescription = parts.description },
        verticalAlignment = Alignment.Bottom
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            (0 until HOURS).chunked(BAR_CHUNK).forEach { chunk ->
                Row(verticalAlignment = Alignment.Bottom) {
                    chunk.forEach { hour ->
                        val altitude = hours[hour]
                        val tall = if (altitude > 0.0) maxOf(MinBar, bars * (altitude / highest).toFloat()) else FlatBar
                        val color = when {
                            hour == now -> if (parts.spec.sunUp) parts.inks.sun else parts.inks.moon
                            altitude <= 0.0 -> parts.inks.track
                            hour < now -> parts.inks.mid
                            else -> ink.copy(alpha = FUTURE_ALPHA)
                        }
                        Box(
                            modifier = GlanceModifier.width(bar + if (hour > 0) BarGap else 0.dp)
                                .height(tall).padding(start = if (hour > 0) BarGap else 0.dp)
                        ) {
                            Box(
                                modifier = GlanceModifier.fillMaxSize().cornerRadius(BarCorner)
                                    .background(FixedColorProvider(color))
                            ) {}
                        }
                    }
                }
            }
        }
        Spacer(modifier = GlanceModifier.height(2.dp))
        Row {
            listOf(0, 6, 12, 18).forEach { hour ->
                Box(modifier = GlanceModifier.width((bar + BarGap) * 6)) {
                    Text(
                        text = axisHour(parts.context, hour),
                        style = TextStyle(color = parts.palette.secondary, fontSize = RingCaptionSp.sp),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/** An hour on the bars' axis, as the phone writes hours: «06» or «6 AM». */
private fun axisHour(context: Context, hour: Int): String {
    val locale = context.resources.configuration.locales[0]
    val skeleton = if (DateFormat.is24HourFormat(context)) "HH" else "ha"
    return LocalTime.of(hour, 0).format(
        java.time.format.DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale)
    )
}

private const val HOURS = 24
private const val HALF_HOUR = 30
private const val BAR_CHUNK = 8
private const val FUTURE_ALPHA = 0.3f

/** A figure in the bold face is about this much wider than in the medium one it is measured in. */
private const val BOLD_OVER_MEDIUM = 1.04f
private val EyebrowGap = 8.dp
private val BarGap = 3.dp
private val MinBar = 3.dp
private val FlatBar = 2.dp
private val BarCorner = 1.5.dp
