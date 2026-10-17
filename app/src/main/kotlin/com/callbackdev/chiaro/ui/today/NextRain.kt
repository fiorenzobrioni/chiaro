package com.callbackdev.chiaro.ui.today

import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.domain.NowcastEngine
import com.callbackdev.chiaro.domain.NowcastEngine.Falling
import com.callbackdev.chiaro.domain.NowcastEngine.Intensity
import com.callbackdev.chiaro.domain.WeatherStateEngine
import com.callbackdev.chiaro.ui.components.SegmentedGroup
import com.callbackdev.chiaro.ui.format.Formats
import com.callbackdev.chiaro.ui.theme.CardCorner
import com.callbackdev.chiaro.ui.theme.ChiaroColors
import com.callbackdev.chiaro.ui.theme.ChiaroTheme
import com.callbackdev.chiaro.ui.theme.forText
import com.callbackdev.chiaro.ui.theme.reducedMotion
import com.callbackdev.chiaro.ui.theme.tabular
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.sqrt

/**
 * «Pioggia nelle prossime 2 ore» (17 ott 2026, DESIGN §8.3c): the next two hours a quarter of
 * an hour at a time, on a card of its own above the hours, drawn only where a model computes
 * the quarter and only when something falls in them. The sentence at the top of the page says
 * it in words ([Headline.NextRain]); this is its arithmetic: when, how hard, a bar a quarter.
 *
 * The card opens its page ([NextRainSheet]): every quarter in words, and where the forecast
 * comes from, which a section built on a model's quarter hour owes the reader.
 */
@Composable
internal fun NextRainCard(
    reading: NowcastEngine.Reading,
    zone: ZoneId,
    timeFmt: DateTimeFormatter,
    locale: Locale,
    animationKey: Any,
    onOpen: () -> Unit
) {
    val caption = nextRainCaption(reading, zone, timeFmt, locale)
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(CardCorner))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClickLabel = stringResource(R.string.nowcast_open), onClick = onOpen)
            .padding(start = 16.dp, top = 20.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        NowcastChart(reading, zone, timeFmt, animationKey, Modifier.fillMaxWidth())
        // The chart's text equivalent (§9.3): what TalkBack reads for the whole card.
        Text(
            text = caption,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** The section's title: what falls, named by the window's own quarters. */
@StringRes
internal fun nextRainTitleRes(reading: NowcastEngine.Reading): Int = when (reading.falling) {
    Falling.SNOW -> R.string.nowcast_title_snow
    Falling.MIXED -> R.string.nowcast_title_mixed
    else -> R.string.nowcast_title_rain
}

/**
 * «Dalle 14:30 alle 15:15 · più intensa verso le 14:45: 3,2 mm/h, moderata». The spells
 * first, at most two (a third is in the chart and on the page), then the heaviest quarter
 * with its rate and the word that says what the rate means.
 */
@Composable
internal fun nextRainCaption(
    reading: NowcastEngine.Reading,
    zone: ZoneId,
    timeFmt: DateTimeFormatter,
    locale: Locale
): String {
    fun t(at: Instant) = LocalDateTime.ofInstant(at, zone).format(timeFmt)
    val phrases = reading.runs.take(2).map { run ->
        when {
            run.ongoing && run.lasting -> stringResource(R.string.nowcast_run_all)
            run.ongoing -> stringResource(R.string.nowcast_run_until, t(run.end))
            run.lasting -> stringResource(R.string.nowcast_run_from, t(run.start))
            else -> stringResource(R.string.nowcast_run_from_to, t(run.start), t(run.end))
        }
    }
    val spells = if (phrases.size == 2) {
        stringResource(R.string.nowcast_runs_then, phrases[0], phrases[1])
    } else {
        phrases.firstOrNull().orEmpty()
    }
    val peak = reading.peak ?: return spells.replaceFirstChar { it.titlecase(locale) }
    val rate = rateText(peak, reading.falling, locale)
    val word = stringResource(bandRes(peak.intensity ?: Intensity.LIGHT))
    val peakText = if (!peak.start.isAfter(reading.now)) {
        stringResource(R.string.nowcast_peak_now, rate, word)
    } else {
        stringResource(R.string.nowcast_peak, t(peak.start), rate, word)
    }
    return stringResource(R.string.nowcast_caption, spells.replaceFirstChar { it.titlecase(locale) }, peakText)
}

/** A quarter's rate as printed: millimetres of water an hour, or centimetres of snow. */
@Composable
private fun rateText(step: NowcastEngine.Step, falling: Falling?, locale: Locale): String =
    if (falling == Falling.SNOW) {
        stringResource(R.string.nowcast_rate_cm, String.format(locale, "%.1f", snowRate(step)))
    } else {
        stringResource(R.string.nowcast_rate_mm, String.format(locale, "%.1f", step.mmPerHour))
    }

/** The snow of a quarter, cm/h: the model's own when it gives one, else its water as snow. */
private fun snowRate(step: NowcastEngine.Step): Double =
    step.snowCmPerHour ?: (step.mmPerHour * WeatherStateEngine.SNOW_CM_PER_MM)

@StringRes
internal fun bandRes(intensity: Intensity): Int = when (intensity) {
    Intensity.LIGHT -> R.string.nowcast_band_light
    Intensity.MODERATE -> R.string.nowcast_band_moderate
    Intensity.HEAVY -> R.string.nowcast_band_heavy
}

/**
 * The chart's height scale (DESIGN §9.1, anchored to the world, never to the data): the
 * square root of the rate over a fixed top, so a light 0.4 mm/h stands a sixth of the way up
 * and is seen, and a cloudburst is not allowed to flatten everything else. The edges are the
 * state engine's own ladder, drawn as the chart's only gridlines and named in words: rain on
 * the AMS's 2.5 and 7.6 mm/h, snow on Open-Meteo's 0.2 and 0.8 cm/h. Past [top] a bar stops at
 * the top and the caption carries its number.
 */
internal enum class NowcastScale(val top: Double, val edges: List<Double>) {
    RAIN(top = 16.0, edges = listOf(2.5, 7.6)),
    SNOW(top = 2.0, edges = listOf(0.2, 0.8));

    fun fraction(value: Double): Float = sqrt((value / top).coerceIn(0.0, 1.0)).toFloat()

    /** What the bar of [step] stands for on this scale. */
    fun valueOf(step: NowcastEngine.Step): Double = if (this == SNOW) snowRate(step) else step.mmPerHour

    companion object {
        fun of(falling: Falling?): NowcastScale = if (falling == Falling.SNOW) SNOW else RAIN
    }
}

/**
 * A bar's colour: the rain's fill ramp (§2.3, the ramp of the marks), one step per band —
 * light, moderate, heavy, darker as it gets heavier on paper and brighter in the dark — and
 * only from its 80% stop up, the first where the lightest bar clears the 3:1 a mark owes the
 * card (measured on `surfaceContainer`: 3.02, 3.73 and 4.66:1 on paper, 5.14 to 7.65 in the
 * dark; the 70% stop was 2.48). `PaletteContrastTest` holds both.
 */
internal object NextRainColors {
    fun ChiaroColors.bar(intensity: Intensity?): Color = rainAt(
        when (intensity) {
            Intensity.HEAVY -> 100
            Intensity.MODERATE -> 90
            else -> 80
        }
    )
}

internal fun ChiaroColors.nowcastBar(intensity: Intensity?): Color = with(NextRainColors) { bar(intensity) }

/**
 * The window drawn: a bar a quarter on a time axis that starts where the reader stands
 * («Ora») and ends two hours on, the quarter we are in cut at now; the bands' words at the
 * right, the half hours under the axis. The bars rise once per fetch, left to right, a beat
 * apart — the rain arriving the way it will — and stand still under reduced motion.
 */
@Composable
internal fun NowcastChart(
    reading: NowcastEngine.Reading,
    zone: ZoneId,
    timeFmt: DateTimeFormatter,
    animationKey: Any,
    modifier: Modifier = Modifier,
    plotHeight: Dp = PlotHeight
) {
    val colors = ChiaroTheme.colors
    val grid = MaterialTheme.colorScheme.outlineVariant
    val ink = MaterialTheme.colorScheme.onSurfaceVariant
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.tabular().copy(color = ink)
    val nowLabel = stringResource(R.string.strip_now)
    val bandLabels = listOf(
        stringResource(R.string.nowcast_band_light),
        stringResource(R.string.nowcast_band_moderate),
        stringResource(R.string.nowcast_band_heavy)
    )
    val scale = NowcastScale.of(reading.falling)
    val ticks = remember(reading.now, reading.until) { halfHours(reading.now, reading.until) }
    val tickLabels = ticks.map { LocalDateTime.ofInstant(it, zone).format(timeFmt) }

    val reduced = reducedMotion()
    val rise = remember(animationKey) { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(animationKey, reduced) {
        if (reduced) rise.snapTo(1f) else rise.animateTo(1f, tween(RiseMillis, easing = LinearEasing))
    }

    val density = LocalDensity.current
    val labelHeight = with(density) { measurer.measure(nowLabel, labelStyle).size.height.toDp() }
    // Measured in text (§10): at 200% the bands' words are twice as tall, and a plot held at
    // its dp height would stack them on top of each other.
    val plot = plotHeight.forText()
    Canvas(modifier = modifier.height(plot + AxisGap + labelHeight).clearAndSetSemantics { }) {
        val hairline = 1.dp.toPx()
        val base = plot.toPx()
        val measuredBands = bandLabels.map { measurer.measure(it, labelStyle) }
        val gutter = (measuredBands.maxOf { it.size.width } + GutterGap.toPx())
        val plotW = (size.width - gutter).coerceAtLeast(1f)
        val windowMs = Duration.between(reading.now, reading.until).toMillis().toFloat().coerceAtLeast(1f)
        fun x(at: Instant): Float =
            (Duration.between(reading.now, at).toMillis() / windowMs).coerceIn(0f, 1f) * plotW
        fun y(value: Double): Float = base - scale.fraction(value) * base

        // The bands: two gridlines on the ladder's edges, each band's word in the gutter, on
        // the middle of its band.
        val edgesY = scale.edges.map { y(it) }
        edgesY.forEach { drawLine(grid, Offset(0f, it), Offset(plotW, it), strokeWidth = hairline) }
        val bounds = listOf(base) + edgesY + listOf(0f)
        measuredBands.forEachIndexed { i, label ->
            val mid = (bounds[i] + bounds[i + 1]) / 2f
            drawText(label, topLeft = Offset(size.width - label.size.width, mid - label.size.height / 2f))
        }

        // The bars, a beat apart.
        val steps = reading.steps
        val inset = BarGap.toPx() / 2f
        val corner = BarCorner.toPx()
        val minBar = MinBar.toPx()
        steps.forEachIndexed { i, step ->
            if (!step.wet) return@forEachIndexed
            val left = x(maxOf(step.start, reading.now)) + inset
            val right = x(step.end) - inset
            if (right - left < hairline) return@forEachIndexed
            val delay = if (steps.size > 1) Stagger * i / (steps.size - 1) else 0f
            val local = ((rise.value - delay) / (1f - Stagger)).coerceIn(0f, 1f)
            val grown = FastOutSlowInEasing.transform(local)
            val h = maxOf(minBar, base - y(scale.valueOf(step))) * grown
            if (h <= 0f) return@forEachIndexed
            val r = minOf(corner, (right - left) / 2f, h)
            val bar = Path().apply {
                addRoundRect(
                    RoundRect(
                        left = left, top = base - h, right = right, bottom = base,
                        topLeftCornerRadius = CornerRadius(r), topRightCornerRadius = CornerRadius(r),
                        bottomLeftCornerRadius = CornerRadius.Zero, bottomRightCornerRadius = CornerRadius.Zero
                    )
                )
            }
            drawPath(bar, colors.nowcastBar(step.intensity))
        }

        // The floor, then the axis: «Ora» where the window starts, and the half hours.
        drawLine(grid, Offset(0f, base), Offset(plotW, base), strokeWidth = hairline)
        val labelTop = base + AxisGap.toPx()
        val nowText = measurer.measure(nowLabel, labelStyle)
        drawText(nowText, topLeft = Offset(0f, labelTop))
        var lastRight = nowText.size.width + LabelGap.toPx()
        ticks.forEachIndexed { i, tick ->
            val tx = x(tick)
            drawLine(grid, Offset(tx, base), Offset(tx, base + TickLength.toPx()), strokeWidth = hairline)
            val text = measurer.measure(tickLabels[i], labelStyle)
            val left = (tx - text.size.width / 2f).coerceAtMost(size.width - text.size.width)
            if (left >= lastRight) {
                drawText(text, topLeft = Offset(left, labelTop))
                lastRight = left + text.size.width + LabelGap.toPx()
            }
        }
    }
}

/** Every half hour strictly after [from] and up to [until], on the UTC grid (every zone the
 * section covers keeps whole-hour offsets, so its half hours are the clock's). */
internal fun halfHours(from: Instant, until: Instant): List<Instant> {
    val step = Duration.ofMinutes(30)
    val start = from.atOffset(ZoneOffset.UTC).let { t ->
        t.withMinute(if (t.minute < 30) 30 else 0).withSecond(0).withNano(0)
            .let { if (t.minute >= 30) it.plusHours(1) else it }
    }.toInstant()
    return generateSequence(start) { it.plus(step) }.takeWhile { !it.isAfter(until) }.toList()
}

/** The section's page: the window again, every quarter in words, and where it comes from. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NextRainSheet(
    reading: NowcastEngine.Reading,
    zone: ZoneId,
    timeFmt: DateTimeFormatter,
    locale: Locale,
    currentCode: Int,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        NextRainPage(reading, zone, timeFmt, locale, currentCode)
    }
}

@Composable
internal fun NextRainPage(
    reading: NowcastEngine.Reading,
    zone: ZoneId,
    timeFmt: DateTimeFormatter,
    locale: Locale,
    currentCode: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sentence = HeadlineEngine.nextRain(reading, zone, currentCode)
        ?.let { HeadlineText.nextRain(context, it) { at -> at.format(timeFmt) } }
    val text = Modifier.padding(horizontal = 16.dp)
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(nextRainTitleRes(reading)),
            style = MaterialTheme.typography.headlineSmall,
            modifier = text.padding(top = 8.dp)
        )
        sentence?.let { Text(it, style = MaterialTheme.typography.bodyLarge, modifier = text) }
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(CardCorner))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(start = 16.dp, top = 20.dp, end = 16.dp, bottom = 16.dp)
        ) {
            NowcastChart(reading, zone, timeFmt, animationKey = Unit, modifier = Modifier.fillMaxWidth())
        }
        SegmentedGroup(
            segments = quarterRows(reading.steps).map { (from, to, step) ->
                { QuarterRow(from, to, step, first = from == 0, steps = reading.steps, zone = zone, timeFmt = timeFmt, locale = locale) }
            }
        )
        Text(
            text = stringResource(R.string.nowcast_sheet_about_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = text.padding(top = 12.dp)
        )
        Text(stringResource(R.string.nowcast_sheet_about), style = MaterialTheme.typography.bodyMedium, modifier = text)
        Text(stringResource(R.string.nowcast_sheet_rule), style = MaterialTheme.typography.bodyMedium, modifier = text)
        Text(
            text = stringResource(R.string.nowcast_sheet_where),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = text
        )
    }
}

/**
 * The page's rows: a wet quarter each, and a dry spell as one row — eight «niente» in a column
 * would be the loudest thing on the page and say the least. Each is the first and last index
 * of its steps, and the step whose reading it carries.
 */
internal fun quarterRows(steps: List<NowcastEngine.Step>): List<Triple<Int, Int, NowcastEngine.Step>> {
    val rows = mutableListOf<Triple<Int, Int, NowcastEngine.Step>>()
    var i = 0
    while (i < steps.size) {
        val start = i
        if (steps[i].wet) {
            i++
        } else {
            while (i < steps.size && !steps[i].wet) i++
        }
        rows += Triple(start, i - 1, steps[start])
    }
    return rows
}

/**
 * One row in words: its stretch of the clock («Ora» for the one we are in), a swatch of its
 * bar's colour, the band's word and the amount; a dry spell says so, with no amount — not
 * «0,0 mm», which is a figure with nothing to plan by.
 */
@Composable
private fun QuarterRow(
    fromIndex: Int,
    toIndex: Int,
    step: NowcastEngine.Step,
    first: Boolean,
    steps: List<NowcastEngine.Step>,
    zone: ZoneId,
    timeFmt: DateTimeFormatter,
    locale: Locale
) {
    fun t(at: Instant) = LocalDateTime.ofInstant(at, zone).format(timeFmt)
    val from = if (first) stringResource(R.string.strip_now) else t(steps[fromIndex].start)
    val to = t(steps[toIndex].end)
    val word = step.intensity?.let { stringResource(bandRes(it)) }
    val amount = if (step.wet) {
        if (step.falling == Falling.SNOW) {
            Formats.centimetres(step.snowCm ?: (step.precipMm * WeatherStateEngine.SNOW_CM_PER_MM), locale)
        } else {
            Formats.millimetres(step.precipMm, locale)
        }
    } else {
        null
    }
    val description = if (word != null && amount != null) {
        stringResource(R.string.nowcast_sheet_row_desc, from, to, word, amount)
    } else {
        stringResource(R.string.nowcast_sheet_row_desc_dry, from, to)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = description }
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Box(
            modifier = Modifier
                .width(SwatchWidth)
                .height(SwatchHeight)
                .clip(RoundedCornerShape(SwatchWidth / 2))
                .background(
                    if (step.wet) ChiaroTheme.colors.nowcastBar(step.intensity)
                    else MaterialTheme.colorScheme.outlineVariant
                )
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = stringResource(R.string.nowcast_sheet_range, from, to),
            style = MaterialTheme.typography.bodyLarge.tabular(),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = word ?: stringResource(R.string.nowcast_sheet_dry),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        amount?.let {
            Spacer(Modifier.width(12.dp))
            Text(text = it, style = MaterialTheme.typography.bodyMedium.tabular())
        }
    }
}

/** The plot's height: a third more than the strip's lane, so the bands have room for words. */
private val PlotHeight = 88.dp
private val AxisGap = 6.dp
private val TickLength = 3.dp
private val GutterGap = 8.dp
private val LabelGap = 8.dp
/** 2dp between neighbouring quarters (§9.2's surface gap), so a spell reads as one shape. */
private val BarGap = 2.dp
private val BarCorner = 4.dp
/** A wet quarter is never thinner than this, however light: wet is not a hairline. */
private val MinBar = 3.dp
private val SwatchWidth = 4.dp
private val SwatchHeight = 20.dp

/** The rise: one pass of [RiseMillis], the last bar starting [Stagger] of the way in. */
private const val RiseMillis = 700
private const val Stagger = 0.35f
