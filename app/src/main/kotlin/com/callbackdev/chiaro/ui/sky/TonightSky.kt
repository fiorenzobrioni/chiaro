package com.callbackdev.chiaro.ui.sky

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.domain.sky.SkyNotScheduled
import com.callbackdev.chiaro.domain.sky.SkyOccurrence
import com.callbackdev.chiaro.domain.sky.SkyVerdictKind
import com.callbackdev.chiaro.ui.components.VerdictKind
import com.callbackdev.chiaro.ui.components.moonLitPath
import com.callbackdev.chiaro.ui.icons.ChiaroIcons
import com.callbackdev.chiaro.ui.theme.ChiaroTheme
import com.callbackdev.chiaro.ui.theme.LocalChiaroColors
import com.callbackdev.chiaro.ui.theme.SkyPalette
import com.callbackdev.chiaro.ui.theme.VerdictColors
import com.callbackdev.chiaro.ui.theme.tabular
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * The hero of the Sky screen (VISION §5.3), redrawn on the design review of 23 set 2026 as
 * **the night it is about** rather than a colored card with sentences in it.
 *
 * The ground is the canvas' own night band (§3.2), lifted by the moon when the moon is up
 * tonight, so the card is dark for the same reason the sky is at 23:00 — whatever the
 * phone's theme. Its verdict therefore wears the DARK verdict pair ([ChiaroTheme.nightColors])
 * and is still a mark and a word before it is a color (§8.7). The moon sits in the corner
 * with tonight's phase drawn, not named.
 *
 * Under the words, the [NightStrip]: dusk to dawn as a band — the moon's hours silvered,
 * each hour's forecast cloud hanging from the top, the stars showing through where it is
 * clear and dark, the dark window framed in the verdict's ink with its two times under it,
 * and the clearest stretch underlined. It is a picture of the sentences above it, which
 * stay: they are the text equivalent (§9.3) and the strip itself is silent to a screen
 * reader.
 */
@Composable
internal fun TonightCard(
    tonight: Tonight,
    zone: ZoneId,
    timeFmt: DateTimeFormatter,
    modifier: Modifier = Modifier
) {
    val res = LocalContext.current.resources
    val window = tonight.window
    val verdict = tonight.verdict
    val night = ChiaroTheme.nightColors
    val moonUp = tonight.moonHeldTheStart || tonight.moonTookTheEnd ||
        tonight.reason == SkyNotScheduled.MOON_ALL_NIGHT
    val illumination = (tonight.moonIlluminationPct ?: 0) / 100.0
    val sky = ChiaroTheme.sky.gradient(
        sunAltitudeDeg = NightAltitude,
        moonIllumination = if (moonUp) illumination else 0.0,
        moonAltitudeDeg = if (moonUp) MoonLiftAltitude else -10.0
    )
    val kind = SkyText.chipKind(verdict?.kind ?: SkyVerdictKind.UNKNOWN)
    val ink = verdictColors(night, kind).ink
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(CardCorner))
            .background(Brush.verticalGradient(sky.stops()))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        CompositionLocalProvider(
            LocalChiaroColors provides night,
            LocalContentColor provides Color.White
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = stringResource(R.string.sky_tonight_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = SecondaryAlpha)
                    )
                    if (window != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                painter = painterResource(ChiaroIcons.verdictMarkRes(kind)),
                                contentDescription = null, // the word beside it says it
                                tint = ink,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = stringResource(
                                    SkyText.verdictWordRes(verdict?.kind ?: SkyVerdictKind.UNKNOWN)
                                ),
                                style = MaterialTheme.typography.headlineMedium,
                                color = ink
                            )
                        }
                        // The arithmetic, always (DESIGN §8.7), right under the word it
                        // decided — or the reason there is no number yet.
                        val evidence = verdict?.let { SkyText.chipEvidence(res, it) }
                        val reason = verdict?.let { SkyText.unknownReason(res, it) }
                        (evidence ?: reason)?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyMedium.tabular(),
                                color = Color.White.copy(alpha = SecondaryAlpha)
                            )
                        }
                    }
                }
                tonight.moonIlluminationPct?.let { pct ->
                    MoonBadge(
                        illumination = illumination.toFloat(),
                        litRight = ((tonight.moonElongationDeg ?: 0.0) < 180.0) != tonight.southern,
                        pct = pct
                    )
                }
            }
            if (window == null) {
                NoWindowText(tonight, zone, timeFmt)
            } else {
                WindowLines(tonight, window, zone, timeFmt)
            }
            val span = tonight.night
            if (span?.end != null && (window != null || tonight.reason == SkyNotScheduled.MOON_ALL_NIGHT)) {
                NightStrip(
                    tonight = tonight,
                    night = span,
                    zone = zone,
                    timeFmt = timeFmt,
                    frame = ink,
                    clear = night.pass.ink,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
        }
    }
}

/** The three skies with no window, each in its own sentence (see [Tonight.reason]). */
@Composable
private fun NoWindowText(tonight: Tonight, zone: ZoneId, timeFmt: DateTimeFormatter) {
    // A fact about the latitude and the season, stated as such — and the RIGHT fact: an
    // empty window is also the deep polar night, where it is dark at noon and "never gets
    // fully dark" would be the reverse of the truth (8 set 2026). Since Fase 27 the common
    // one is a moon up from dusk to dawn, said with the night it did NOT cancel after it.
    val night = tonight.night
    val moonPct = tonight.moonIlluminationPct
    val text = when {
        tonight.reason == SkyNotScheduled.MOON_ALL_NIGHT && night?.end != null && moonPct != null ->
            stringResource(
                R.string.sky_tonight_moon_all_night,
                moonPct,
                night.start.atZone(zone).format(timeFmt),
                night.end!!.atZone(zone).format(timeFmt)
            )
        tonight.reason == SkyNotScheduled.DARK_ALL_DAY -> stringResource(R.string.sky_tonight_dark_all_day)
        else -> stringResource(R.string.sky_tonight_no_darkness)
    }
    Text(text = text, style = MaterialTheme.typography.titleMedium, color = Color.White)
}

/** The window, what the moon took from it, and when it is clearest. */
@Composable
private fun WindowLines(
    tonight: Tonight,
    window: SkyOccurrence.At,
    zone: ZoneId,
    timeFmt: DateTimeFormatter
) {
    Text(
        text = stringResource(
            R.string.sky_tonight_window,
            window.start.atZone(zone).format(timeFmt),
            (window.end ?: window.start).atZone(zone).format(timeFmt)
        ),
        style = MaterialTheme.typography.titleMedium,
        color = Color.White,
        modifier = Modifier.padding(top = 6.dp)
    )
    // What the moon took out of the night, read off the window's edges against the
    // night's (Fase 27): the window IS the moonless part.
    val moonPct = tonight.moonIlluminationPct
    val moonLine = when {
        moonPct == null -> null
        tonight.moonHeldTheStart -> stringResource(
            R.string.sky_tonight_moon_sets, window.start.atZone(zone).format(timeFmt), moonPct
        )
        tonight.moonTookTheEnd -> stringResource(
            R.string.sky_tonight_moon_rises,
            (window.end ?: window.start).atZone(zone).format(timeFmt),
            moonPct
        )
        else -> null
    }
    val secondary = Color.White.copy(alpha = SecondaryAlpha)
    moonLine?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = secondary) }
    // And WHEN, on a night the verdict had to average (Fase 28).
    tonight.clearStretch?.let { stretch ->
        Text(
            text = stringResource(
                R.string.sky_tonight_clear_between,
                stretch.start.atZone(zone).format(timeFmt),
                stretch.endInclusive.atZone(zone).format(timeFmt)
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = secondary
        )
    }
}

/** Tonight's moon, drawn in its phase, with its percentage under it. */
@Composable
private fun MoonBadge(illumination: Float, litRight: Boolean, pct: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(modifier = Modifier.size(MoonSize).clearAndSetSemantics { }) {
            val r = size.minDimension / 2f
            val c = Offset(size.width / 2f, size.height / 2f)
            drawCircle(
                Brush.radialGradient(
                    0f to SkyPalette.MoonFace.copy(alpha = 0.25f * illumination),
                    1f to Color.Transparent,
                    center = c,
                    radius = r
                ),
                radius = r,
                center = c
            )
            val disc = r * 0.62f
            drawCircle(SkyPalette.MoonFace.copy(alpha = 0.16f), radius = disc, center = c)
            drawPath(moonLitPath(c, disc, illumination, litRight), SkyPalette.MoonFace)
        }
        Text(
            text = stringResource(R.string.sky_tonight_moon_pct, pct),
            style = MaterialTheme.typography.labelMedium.tabular(),
            color = Color.White.copy(alpha = SecondaryAlpha)
        )
    }
}

/**
 * Dusk to dawn as a band (see [TonightCard]). Every mark here is a depiction of a number
 * the card prints in words above it; nothing on the strip is new information, and a
 * screen reader skips it.
 */
@Composable
private fun NightStrip(
    tonight: Tonight,
    night: SkyOccurrence.At,
    zone: ZoneId,
    timeFmt: DateTimeFormatter,
    frame: Color,
    clear: Color,
    modifier: Modifier = Modifier
) {
    val start = night.start
    val end = night.end ?: return
    val window = tonight.window
    val moonRanges = moonUpRanges(tonight)
    val stars = remember { starField(StarCount) }
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelMedium.tabular()
        .copy(color = Color.White.copy(alpha = SecondaryAlpha))
    val labels = listOfNotNull(
        (window?.start ?: start),
        (window?.end ?: end)
    ).map { it to measurer.measure(AnnotatedString(it.atZone(zone).format(timeFmt)), labelStyle) }
    val labelHeight = with(LocalDensity.current) { labels.maxOf { it.second.size.height }.toDp() }
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(BandHeight + LabelGap + labelHeight)
            .clearAndSetSemantics { }
    ) {
        val band = BandHeight.toPx()
        val w = size.width
        fun x(at: Instant) = w * nightFraction(at, start, end)
        val corner = CornerRadius(BandCorner.toPx())
        val bandShape = Path().apply {
            addRoundRect(RoundRect(0f, 0f, w, band, corner))
        }
        clipPath(bandShape) {
        drawRect(SkyPalette.ScrimColor.copy(alpha = 0.35f), size = Size(w, band))
        // The moon's hours: silvered, because moonlight is what washes the faint things out.
        moonRanges.forEach { range ->
            val x0 = x(range.start)
            val x1 = x(range.endInclusive)
            if (x1 > x0) {
                drawRect(
                    Brush.verticalGradient(
                        listOf(SkyPalette.MoonFace.copy(alpha = 0.30f), SkyPalette.MoonFace.copy(alpha = 0.10f)),
                        startY = 0f, endY = band
                    ),
                    topLeft = Offset(x0, 0f),
                    size = Size(x1 - x0, band)
                )
            }
        }
        // The stars show through where the hour is clear, and less where the moon is up.
        stars.forEach { (fx, fy, fr) ->
            val at = start.plusMillis(((end.toEpochMilli() - start.toEpochMilli()) * fx).toLong())
            val cloud = cloudAt(tonight.hours, at) ?: 0
            val moonlit = moonRanges.any { at >= it.start && at <= it.endInclusive }
            val alpha = (1f - cloud / 100f) * (if (moonlit) 0.35f else 0.9f)
            if (alpha > 0.05f) {
                drawCircle(Color.White.copy(alpha = alpha), radius = fr.dp.toPx(), center = Offset(w * fx, band * fy))
            }
        }
        // Each hour's cloud, hanging from the top of the band as deep as the sky is covered.
        tonight.hours.forEach { hour ->
            val x0 = x(hour.at).coerceIn(0f, w)
            val x1 = x(hour.at.plusSeconds(3600)).coerceIn(0f, w)
            if (x1 > x0 && hour.cloudPct > 0) {
                // Fading out at its lower edge, so neighbouring hours read as one bank of
                // cloud with a soft underside rather than as a bar chart.
                val depth = band * hour.cloudPct / 100f
                drawRect(
                    Brush.verticalGradient(
                        0f to Color.White.copy(alpha = 0.45f),
                        0.6f to Color.White.copy(alpha = 0.22f),
                        1f to Color.Transparent,
                        startY = 0f, endY = depth
                    ),
                    topLeft = Offset(x0, 0f),
                    size = Size(x1 - x0, depth)
                )
            }
        }
        }
        // The dark window, framed in the verdict's own ink.
        window?.let { win ->
            val x0 = x(win.start)
            val x1 = x(win.end ?: win.start)
            if (x1 > x0) {
                val stroke = 2.dp.toPx()
                drawRoundRect(
                    color = frame,
                    topLeft = Offset(x0 + stroke / 2f, stroke / 2f),
                    size = Size(x1 - x0 - stroke, band - stroke),
                    cornerRadius = corner,
                    style = Stroke(width = stroke)
                )
            }
        }
        // The clearest stretch, underlined in the ink of a good sky.
        tonight.clearStretch?.let { stretch ->
            val x0 = x(stretch.start)
            val x1 = x(stretch.endInclusive)
            if (x1 > x0) {
                drawLine(
                    color = clear,
                    start = Offset(x0, band + 3.dp.toPx()),
                    end = Offset(x1, band + 3.dp.toPx()),
                    strokeWidth = 3.dp.toPx(),
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            }
        }
        // The window's two times under its edges — or the night's, on a night the moon owns.
        val top = band + LabelGap.toPx()
        val (first, last) = labels.first() to labels.last()
        val gap = 8.dp.toPx()
        if (x(last.first) - x(first.first) >= first.second.size.width + last.second.size.width + gap) {
            labels.forEachIndexed { i, (at, text) ->
                val cx = x(at)
                val left = if (i == 0) cx else cx - text.size.width
                drawText(text, topLeft = Offset(left.coerceIn(0f, (w - text.size.width).coerceAtLeast(0f)), top))
            }
        } else {
            // A window too short for two labels gets one, centred under it.
            val both = measurer.measure(
                AnnotatedString(
                    "${first.first.atZone(zone).format(timeFmt)} – ${last.first.atZone(zone).format(timeFmt)}"
                ),
                labelStyle
            )
            val centre = (x(first.first) + x(last.first)) / 2f
            drawText(
                both,
                topLeft = Offset(
                    (centre - both.size.width / 2f).coerceIn(0f, (w - both.size.width).coerceAtLeast(0f)),
                    top
                )
            )
        }
    }
}

/** The stretches of tonight's night the moon is up for, read off the card's own facts:
 * the window is the night minus the moon (Fase 27). */
internal fun moonUpRanges(tonight: Tonight): List<ClosedRange<Instant>> {
    val night = tonight.night ?: return emptyList()
    val end = night.end ?: return emptyList()
    val window = tonight.window
    if (window == null) {
        return if (tonight.reason == SkyNotScheduled.MOON_ALL_NIGHT) listOf(night.start..end) else emptyList()
    }
    return buildList {
        if (tonight.moonHeldTheStart) add(night.start..window.start)
        if (tonight.moonTookTheEnd) add((window.end ?: window.start)..end)
    }
}

/** Where [at] falls between [start] and [end], 0..1. */
internal fun nightFraction(at: Instant, start: Instant, end: Instant): Float {
    val span = (end.toEpochMilli() - start.toEpochMilli()).toFloat()
    if (span <= 0f) return 0f
    return ((at.toEpochMilli() - start.toEpochMilli()) / span).coerceIn(0f, 1f)
}

/** The cloud of the hour that holds [at], or null when the forecast has no such hour. */
internal fun cloudAt(hours: List<NightHour>, at: Instant): Int? =
    hours.lastOrNull { !it.at.isAfter(at) && at.isBefore(it.at.plusSeconds(3600)) }?.cloudPct

/** A fixed scatter of stars — x, y as fractions of the band, radius in dp. Deterministic,
 * so the same night does not reshuffle its sky at every recomposition. */
private fun starField(count: Int): List<Triple<Float, Float, Float>> {
    var seed = 0x2545F491L
    fun next(): Float {
        seed = (seed * 6364136223846793005L + 1442695040888963407L)
        return ((seed ushr 33) and 0xFFFFFF).toFloat() / 0xFFFFFF
    }
    return List(count) { Triple(next(), 0.12f + 0.76f * next(), 0.6f + 0.7f * next()) }
}

private fun verdictColors(colors: com.callbackdev.chiaro.ui.theme.ChiaroColors, kind: VerdictKind): VerdictColors =
    when (kind) {
        VerdictKind.PASS -> colors.pass
        VerdictKind.UNSTABLE -> colors.unstable
        VerdictKind.FAIL -> colors.fail
        VerdictKind.UNKNOWN -> colors.unknown
    }

/** The night band the card is painted with: astronomical night, with the moon's lift. */
private const val NightAltitude = -24.0
private const val MoonLiftAltitude = 30.0
private const val SecondaryAlpha = 0.72f
private const val StarCount = 46
private val CardCorner = 28.dp
private val BandHeight = 34.dp
private val BandCorner = 10.dp
private val LabelGap = 8.dp
private val MoonSize = 44.dp
