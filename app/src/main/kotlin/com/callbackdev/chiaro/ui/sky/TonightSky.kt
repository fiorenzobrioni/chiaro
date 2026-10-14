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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import com.callbackdev.chiaro.ui.format.LocalClock
import com.callbackdev.chiaro.ui.theme.ChiaroMotion
import com.callbackdev.chiaro.ui.theme.reducedMotion
import java.time.Duration
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt
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
 * stay: they are the text equivalent (§9.3). Since 14 ott 2026 it is also a control: a
 * finger on it asks for one instant of the night, and the card answers ([NightStrip]).
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
    // The instant a finger (or a screen reader) is asking about on the strip, null for
    // the night as a whole (14 ott 2026). While it is set the card paints that instant's
    // sky — the sun and the moon where they stand then, that hour's cloud and rain — and
    // lets go back to the whole night's sky when the finger lifts.
    var scrub by remember(tonight.night) { mutableStateOf<Instant?>(null) }
    val reading = scrub?.let { nightReading(tonight, it) }
    val sample = scrub?.let { sampleAt(tonight.samples, it) }
    val reduced = reducedMotion()
    val sunAltitude by animateFloatAsState(
        targetValue = (sample?.sunAltitudeDeg ?: NightAltitude).toFloat(),
        animationSpec = ChiaroMotion.effects(reduced),
        label = "tonight-sun"
    )
    // Pinned to the strip's own account of the moon: the sky never shows a moon the
    // readout under it calls set, even in the minute either side of a rising.
    val moonAltitude by animateFloatAsState(
        targetValue = when {
            reading == null -> if (moonUp) MoonLiftAltitude else -10.0
            reading.moonUp -> maxOf(sample?.moonAltitudeDeg ?: MoonLiftAltitude, 2.0)
            else -> minOf(sample?.moonAltitudeDeg ?: -10.0, -2.0)
        }.toFloat(),
        animationSpec = ChiaroMotion.effects(reduced),
        label = "tonight-moon"
    )
    val cloud by animateFloatAsState(
        targetValue = (reading?.cloudPct ?: 0).toFloat(),
        animationSpec = ChiaroMotion.effects(reduced),
        label = "tonight-cloud"
    )
    val precip by animateFloatAsState(
        targetValue = (reading?.precipPct ?: 0).toFloat(),
        animationSpec = ChiaroMotion.effects(reduced),
        label = "tonight-precip"
    )
    val sky = ChiaroTheme.sky.gradient(
        sunAltitudeDeg = sunAltitude.toDouble(),
        cloudPct = cloud.roundToInt(),
        precipPct = precip.roundToInt(),
        moonIllumination = illumination,
        moonAltitudeDeg = moonAltitude.toDouble()
    )
    val kind = SkyText.chipKind(verdict?.kind ?: SkyVerdictKind.UNKNOWN)
    val ink = verdictColors(night, kind).ink
    val litRight = ((tonight.moonElongationDeg ?: 0.0) < 180.0) != tonight.southern
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
                    // A moon below the horizon at the instant on the strip is dimmed,
                    // not removed: tonight's phase is still tonight's.
                    val badgeAlpha by animateFloatAsState(
                        targetValue = if (reading != null && !reading.moonUp) MoonSetAlpha else 1f,
                        animationSpec = ChiaroMotion.effects(reduced),
                        label = "tonight-moon-badge"
                    )
                    MoonBadge(
                        illumination = illumination.toFloat(),
                        litRight = litRight,
                        pct = pct,
                        modifier = Modifier.graphicsLayer { alpha = badgeAlpha }
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
                ScrubLine(reading = reading, zone = zone, timeFmt = timeFmt, moonKnown = tonight.moonIlluminationPct != null)
                NightStrip(
                    tonight = tonight,
                    night = span,
                    zone = zone,
                    timeFmt = timeFmt,
                    frame = ink,
                    clear = night.pass.ink,
                    moonIllumination = illumination.toFloat(),
                    moonLitRight = litRight,
                    scrub = scrub,
                    onScrub = { scrub = it },
                    stateDescription = reading?.let { scrubText(it, zone, timeFmt, tonight.moonIlluminationPct != null) },
                    modifier = Modifier.padding(top = 2.dp)
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
private fun MoonBadge(illumination: Float, litRight: Boolean, pct: Int, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
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
 * The line over the strip: what to do with it while nobody is touching it, and the
 * instant under the finger while somebody is — its time, that hour's cloud, its rain
 * from the 20% the app prints a probability at, and whether the moon is up. Silent to a
 * screen reader: the strip itself carries the same words as its state.
 */
@Composable
private fun ScrubLine(reading: NightReading?, zone: ZoneId, timeFmt: DateTimeFormatter, moonKnown: Boolean) {
    val text = reading?.let { scrubText(it, zone, timeFmt, moonKnown) }
        ?: stringResource(R.string.sky_scrub_hint)
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge.tabular(),
        color = if (reading != null) Color.White else Color.White.copy(alpha = HintAlpha),
        modifier = Modifier
            .padding(top = 10.dp)
            .clearAndSetSemantics { }
    )
}

/** «02:00 · nuvole 10% · pioggia 40% · senza luna» — the reading in the card's words. */
@Composable
private fun scrubText(reading: NightReading, zone: ZoneId, timeFmt: DateTimeFormatter, moonKnown: Boolean): String =
    listOfNotNull(
        reading.at.atZone(zone).format(timeFmt),
        reading.cloudPct?.let { stringResource(R.string.sky_evidence_cloud, it) },
        reading.precipPct?.takeIf { it >= PrecipPrintedFrom }?.let { stringResource(R.string.sky_evidence_rain, it) },
        if (!moonKnown) null
        else stringResource(if (reading.moonUp) R.string.sky_scrub_moon_up else R.string.sky_scrub_moon_down)
    ).joinToString(" · ")

/**
 * Dusk to dawn as a band (see [TonightCard]), and since 14 ott 2026 a control: a finger
 * dragged along it — or held on it — asks for one instant of the night, and the card
 * answers with that instant's sky and the line over the strip with its numbers. A
 * horizontal drag starts at once, because the page scrolls vertically and the two never
 * compete; holding still needs the long press, so a vertical scroll that happens to
 * begin on the band does not flash a reading. Every whole hour crossed is a light tick
 * under the finger, the edges of the dark window a firmer one.
 *
 * To a screen reader it is an adjustable control in whole hours, whose state is the
 * reading: the hour-by-hour cloud is information no sentence above it carries, so the
 * strip is no longer silent.
 *
 * The marks (14 ott 2026, review point 8): the moon's hours in their own straw wash with
 * the moon's disc in them, rather than the white of the cloud; a tick at every whole
 * hour; and "now", when the night is under way.
 */
@Composable
private fun NightStrip(
    tonight: Tonight,
    night: SkyOccurrence.At,
    zone: ZoneId,
    timeFmt: DateTimeFormatter,
    frame: Color,
    clear: Color,
    moonIllumination: Float,
    moonLitRight: Boolean,
    scrub: Instant?,
    onScrub: (Instant?) -> Unit,
    stateDescription: String?,
    modifier: Modifier = Modifier
) {
    val start = night.start
    val end = night.end ?: return
    val window = tonight.window
    val moonRanges = moonUpRanges(tonight)
    val stars = remember { starField(StarCount) }
    val measurer = rememberTextMeasurer()
    val now = Instant.now(LocalClock.current)
    val labelStyle = MaterialTheme.typography.labelMedium.tabular()
        .copy(color = Color.White.copy(alpha = SecondaryAlpha))
    val labels = listOfNotNull(
        (window?.start ?: start),
        (window?.end ?: end)
    ).map { it to measurer.measure(AnnotatedString(it.atZone(zone).format(timeFmt)), labelStyle) }
    val labelHeight = with(LocalDensity.current) { labels.maxOf { it.second.size.height }.toDp() }

    // Ticks under the finger, decided on the instant the strip settles on, so a drag
    // back and forth inside one hour stays quiet.
    val haptics = LocalHapticFeedback.current
    val latestOnScrub by rememberUpdatedState(onScrub)
    val latestScrub by rememberUpdatedState(scrub)
    fun scrubTo(fraction: Float?) {
        val next = fraction?.let { scrubInstant(it, start, end) }
        val previous = latestScrub
        if (next != null && previous != null) {
            when {
                window != null && inWindow(window, next) != inWindow(window, previous) ->
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                hourOf(next) != hourOf(previous) ->
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
            }
        }
        latestOnScrub(next)
    }

    // Whole hours for a screen reader: the step its swipe moves the control by.
    val hours = Duration.between(start, end).toMinutes() / 60f
    val a11ySteps = (hours.roundToInt() - 1).coerceAtLeast(0)
    val stripDescription = stringResource(R.string.sky_scrub_strip_desc)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(CursorHeadroom + BandHeight + LabelGap + labelHeight)
            .pointerInput(start, end, window) {
                detectHorizontalDragGestures(
                    onDragStart = { scrubTo(it.x / size.width) },
                    onDragEnd = { scrubTo(null) },
                    onDragCancel = { scrubTo(null) },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        scrubTo(change.position.x / size.width)
                    }
                )
            }
            .pointerInput(start, end, window) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        scrubTo(it.x / size.width)
                    },
                    onDragEnd = { scrubTo(null) },
                    onDragCancel = { scrubTo(null) },
                    onDrag = { change, _ ->
                        change.consume()
                        scrubTo(change.position.x / size.width)
                    }
                )
            }
            .semantics {
                contentDescription = stripDescription
                this.stateDescription = stateDescription.orEmpty()
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = scrub?.let { nightFraction(it, start, end) } ?: 0f,
                    range = 0f..1f,
                    steps = a11ySteps
                )
                setProgress { target ->
                    // Whole hours from dusk, so each swipe is the next hour on the clock
                    // of the night rather than an arbitrary slice of it.
                    val hour = (target * hours).roundToInt()
                    latestOnScrub(scrubInstant((hour / hours).coerceIn(0f, 1f), start, end))
                    true
                }
            }
    ) {
        val top = CursorHeadroom.toPx()
        val band = BandHeight.toPx()
        val w = size.width
        fun x(at: Instant) = w * nightFraction(at, start, end)
        val corner = CornerRadius(BandCorner.toPx())
        val bandShape = Path().apply {
            addRoundRect(RoundRect(0f, top, w, top + band, corner))
        }
        clipPath(bandShape) {
            drawRect(SkyPalette.ScrimColor.copy(alpha = 0.35f), topLeft = Offset(0f, top), size = Size(w, band))
            // The moon's hours: washed in moonlight's own straw, because moonlight is what
            // washes the faint things out — and not in the white the cloud hangs in.
            moonRanges.forEach { range ->
                val x0 = x(range.start)
                val x1 = x(range.endInclusive)
                if (x1 > x0) {
                    drawRect(
                        Brush.verticalGradient(
                            listOf(SkyPalette.MoonWash.copy(alpha = 0.10f), SkyPalette.MoonWash.copy(alpha = 0.30f)),
                            startY = top, endY = top + band
                        ),
                        topLeft = Offset(x0, top),
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
                    drawCircle(
                        Color.White.copy(alpha = alpha),
                        radius = fr.dp.toPx(),
                        center = Offset(w * fx, top + band * fy)
                    )
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
                            startY = top, endY = top + depth
                        ),
                        topLeft = Offset(x0, top),
                        size = Size(x1 - x0, depth)
                    )
                }
            }
            // The moon itself, in tonight's phase, low in each stretch it holds: the mark
            // that says "moon" in a shape and not only in a hue (§8.7's rule, kept here).
            val disc = MoonMarkRadius.toPx()
            moonRanges.forEach { range ->
                val x0 = x(range.start)
                val x1 = x(range.endInclusive)
                if (x1 - x0 >= disc * 4f) {
                    val c = Offset((x0 + x1) / 2f, top + band * 0.66f)
                    drawCircle(SkyPalette.MoonFace.copy(alpha = 0.18f), radius = disc, center = c)
                    drawPath(
                        moonLitPath(c, disc, moonIllumination, moonLitRight),
                        SkyPalette.MoonFace.copy(alpha = 0.9f)
                    )
                }
            }
            // A tick at every whole hour of the clock, standing on the band's floor.
            val tick = TickHeight.toPx()
            hourTicks(start, end).forEach { at ->
                val tx = x(at)
                drawLine(
                    color = Color.White.copy(alpha = 0.35f),
                    start = Offset(tx, top + band - tick),
                    end = Offset(tx, top + band),
                    strokeWidth = 1.dp.toPx()
                )
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
                    topLeft = Offset(x0 + stroke / 2f, top + stroke / 2f),
                    size = Size(x1 - x0 - stroke, band - stroke),
                    cornerRadius = corner,
                    style = Stroke(width = stroke)
                )
            }
        }
        // Now, when the night is under way: a thin line through the band and a dot under it.
        if (now >= start && now <= end) {
            val nx = x(now)
            drawLine(
                color = Color.White.copy(alpha = 0.85f),
                start = Offset(nx, top),
                end = Offset(nx, top + band),
                strokeWidth = 1.5.dp.toPx()
            )
            drawCircle(Color.White, radius = 2.5.dp.toPx(), center = Offset(nx, top + band + 3.dp.toPx()))
        }
        // The clearest stretch, underlined in the ink of a good sky.
        tonight.clearStretch?.let { stretch ->
            val x0 = x(stretch.start)
            val x1 = x(stretch.endInclusive)
            if (x1 > x0) {
                drawLine(
                    color = clear,
                    start = Offset(x0, top + band + 3.dp.toPx()),
                    end = Offset(x1, top + band + 3.dp.toPx()),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
        // The finger's instant: a line taller than the band, with a knob over it.
        scrub?.let { at ->
            val sx = x(at)
            drawLine(
                color = Color.White,
                start = Offset(sx, top - CursorOvershoot.toPx()),
                end = Offset(sx, top + band + CursorOvershoot.toPx()),
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawCircle(Color.White, radius = CursorKnob.toPx(), center = Offset(sx, top - CursorOvershoot.toPx()))
        }
        // The window's two times under its edges — or the night's, on a night the moon owns.
        val labelTop = top + band + LabelGap.toPx()
        val (first, last) = labels.first() to labels.last()
        val gap = 8.dp.toPx()
        if (x(last.first) - x(first.first) >= first.second.size.width + last.second.size.width + gap) {
            labels.forEachIndexed { i, (at, text) ->
                val cx = x(at)
                val left = if (i == 0) cx else cx - text.size.width
                drawText(text, topLeft = Offset(left.coerceIn(0f, (w - text.size.width).coerceAtLeast(0f)), labelTop))
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
                    labelTop
                )
            )
        }
    }
}

/** What the strip says about one instant of the night ([TonightCard]'s readout). */
internal data class NightReading(
    val at: Instant,
    val cloudPct: Int?,
    val precipPct: Int?,
    val moonUp: Boolean,
    val inWindow: Boolean
)

/**
 * The night at [at], read off the card's own facts so the line can never disagree with
 * the band under it: the forecast hour that holds it, and the moon from the same ranges
 * the strip washes — the window IS the moonless part of the night, so an instant on its
 * edge belongs to the window.
 */
internal fun nightReading(tonight: Tonight, at: Instant): NightReading {
    val hour = tonight.hours.lastOrNull { !it.at.isAfter(at) && at.isBefore(it.at.plusSeconds(3600)) }
    val window = tonight.window
    val dark = window != null && inWindow(window, at)
    val moonUp = !dark && moonUpRanges(tonight).any { at >= it.start && at <= it.endInclusive }
    return NightReading(at, hour?.cloudPct, hour?.precipPct, moonUp, dark)
}

private fun inWindow(window: SkyOccurrence.At, at: Instant): Boolean =
    at >= window.start && at <= (window.end ?: window.start)

/**
 * The instant a point on the strip stands for: its share of the night, settled on the
 * clock's five minutes (a reading of «02:07» is a precision the hourly forecast under it
 * does not have), and never outside dusk and dawn — whose own odd minutes are reachable
 * at the two ends.
 */
internal fun scrubInstant(fraction: Float, start: Instant, end: Instant): Instant {
    val span = end.toEpochMilli() - start.toEpochMilli()
    val raw = start.toEpochMilli() + (span * fraction.coerceIn(0f, 1f).toDouble()).toLong()
    // Within half a step of an end, the end itself: dusk at 20:54 is a time the card
    // prints, and a grid that could only offer 20:55 would never let a finger reach it.
    if (raw - start.toEpochMilli() < ScrubStepMs / 2) return start
    if (end.toEpochMilli() - raw < ScrubStepMs / 2) return end
    val snapped = Math.floorDiv(raw + ScrubStepMs / 2, ScrubStepMs) * ScrubStepMs
    return Instant.ofEpochMilli(snapped.coerceIn(start.toEpochMilli(), end.toEpochMilli()))
}

/** The whole hours strictly inside the night. */
internal fun hourTicks(start: Instant, end: Instant): List<Instant> {
    val first = start.truncatedTo(ChronoUnit.HOURS).plus(Duration.ofHours(1))
    return generateSequence(first) { it.plus(Duration.ofHours(1)) }.takeWhile { it.isBefore(end) }.toList()
}

private fun hourOf(at: Instant): Long = Math.floorDiv(at.epochSecond, 3600L)

/** The sun and the moon at [at], read between the two samples either side of it. */
internal fun sampleAt(samples: List<NightSample>, at: Instant): NightSample? {
    if (samples.isEmpty()) return null
    val after = samples.indexOfFirst { !it.at.isBefore(at) }
    if (after <= 0) return samples[if (after == 0) 0 else samples.lastIndex].copy(at = at)
    val a = samples[after - 1]
    val b = samples[after]
    val span = Duration.between(a.at, b.at).toMillis().toDouble()
    val t = if (span <= 0.0) 0.0 else Duration.between(a.at, at).toMillis() / span
    return NightSample(
        at = at,
        sunAltitudeDeg = a.sunAltitudeDeg + (b.sunAltitudeDeg - a.sunAltitudeDeg) * t,
        moonAltitudeDeg = a.moonAltitudeDeg + (b.moonAltitudeDeg - a.moonAltitudeDeg) * t
    )
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
private const val HintAlpha = 0.6f
private const val MoonSetAlpha = 0.35f
/** The app prints a precipitation probability from 20% up (the hour strip's rule). */
private const val PrecipPrintedFrom = 20
private const val ScrubStepMs = 5 * 60_000L
private const val StarCount = 46
private val CardCorner = 28.dp
private val BandHeight = 34.dp
private val BandCorner = 10.dp
private val LabelGap = 8.dp
private val MoonSize = 44.dp
private val MoonMarkRadius = 5.dp
private val TickHeight = 5.dp
/** Room over the band for the cursor's knob, so it is not clipped by the card's text. */
private val CursorHeadroom = 10.dp
private val CursorOvershoot = 4.dp
private val CursorKnob = 5.dp
