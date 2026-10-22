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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
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
import com.callbackdev.chiaro.ui.theme.CardCorner
import com.callbackdev.chiaro.ui.theme.ChiaroTheme
import com.callbackdev.chiaro.ui.theme.LocalChiaroColors
import com.callbackdev.chiaro.ui.theme.SkyPalette
import com.callbackdev.chiaro.ui.theme.VerdictColors
import com.callbackdev.chiaro.ui.theme.tabular
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import android.graphics.RectF
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.LayoutCoordinates
import com.callbackdev.chiaro.data.PowerSaveState
import com.callbackdev.chiaro.domain.model.Coordinates
import com.callbackdev.chiaro.ui.components.SkyBand
import com.callbackdev.chiaro.ui.components.SkyFall
import com.callbackdev.chiaro.ui.components.SkyShaderLayer
import com.callbackdev.chiaro.ui.components.StarField
import com.callbackdev.chiaro.ui.icons.LocalAnimatedIcons
import com.callbackdev.chiaro.ui.icons.LocalMotionPaused
import com.callbackdev.chiaro.ui.theme.SkyGradient
import com.callbackdev.chiaro.ui.theme.skyInkShadow
import com.callbackdev.chiaro.widget.window.WindowFrame
import com.callbackdev.chiaro.widget.window.WindowLabel
import com.callbackdev.chiaro.widget.window.WindowScene
import com.callbackdev.chiaro.widget.window.WindowText

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
    modifier: Modifier = Modifier,
    /** The place, for the real sky in the card (22 ott 2026); without it the card keeps
     * the gradient and the moon badge it had. */
    coordinates: Coordinates? = null,
    /** The job ids the reader follows: a weak shower's radiant is drawn only for them. */
    followed: Set<String> = emptySet(),
    /** «Behind the clouds» (22 ott 2026): the picture without the hour's cover, veil and
     * rain, the reader's choice on the card, kept across launches; the words keep the forecast. */
    bareSky: Boolean = false,
    onBareSky: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val res = context.resources
    val window = tonight.window
    val verdict = tonight.verdict
    val night = ChiaroTheme.nightColors
    val moonUp = tonight.moonHeldTheStart || tonight.moonTookTheEnd ||
        tonight.reason == SkyNotScheduled.MOON_ALL_NIGHT
    val illumination = (tonight.moonIlluminationPct ?: 0) / 100.0
    // The instant a finger (or a screen reader) is asking about on the strip, null for
    // the night as a whole (14 ott 2026). While it is set the card paints that instant's
    // sky and lets go back to the night's own when the finger lifts.
    var scrub by remember(tonight.night) { mutableStateOf<Instant?>(null) }
    val reading = scrub?.let { nightReading(tonight, it) }
    // The instant the picture is of (22 ott 2026): the finger's, else now while the night
    // is in progress, else the night's middle, the hour the card's moon is read at.
    val now = Instant.now(LocalClock.current)
    val span = tonight.night
    val shown = scrub ?: span?.let { tonightInstant(now, it) } ?: now
    val shownReading = nightReading(tonight, shown)
    // The picture's cover between the hours (22 ott 2026): the forecast is hourly and the
    // scene jumped at every hour under the finger; the drawing slides between the two hours
    // that hold the instant, and a short spring eases a finger that jumps hours. The
    // numbers in the lines stay the hour's. «Behind the clouds» takes it all away.
    val reduced = reducedMotion()
    val cover by animateFloatAsState(
        targetValue = if (bareSky) 0f else betweenHours(tonight.hours, shown) { it.cloudPct.toFloat() } ?: 0f,
        animationSpec = ChiaroMotion.effects(reduced),
        label = "tonight-cover"
    )
    val wet by animateFloatAsState(
        targetValue = if (bareSky) 0f else betweenHours(tonight.hours, shown) { (it.precipPct ?: 0).toFloat() } ?: 0f,
        animationSpec = ChiaroMotion.effects(reduced),
        label = "tonight-wet"
    )
    val cloudNow = cover.roundToInt()
    val wetNow = wet.roundToInt()
    val kind = SkyText.chipKind(verdict?.kind ?: SkyVerdictKind.UNKNOWN)
    val ink = verdictColors(night, kind).ink
    val litRight = ((tonight.moonElongationDeg ?: 0.0) < 180.0) != tonight.southern
    // Where the window looks (22 ott 2026): the window's own choice at the night's instant,
    // made once a night so the sky moves under a finger on the strip and the camera does
    // not; or where the reader turned it, kept while the page is open.
    var turn by remember(tonight.night) { mutableStateOf<Double?>(null) }
    val autoFacing = remember(tonight.night, coordinates) {
        coordinates?.let { WindowScene.facing(shown, it, zone, tonight.southern, followed) }
    }
    val facing = turn ?: autoFacing
    val table = ChiaroTheme.sky
    val density = LocalDensity.current
    val saving = remember(context) { PowerSaveState.of(context) }
    val moving = LocalAnimatedIcons.current && !reducedMotion() && !LocalMotionPaused.current && !saving.isOn()
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(CardCorner))
    ) {
        val w = with(density) { maxWidth.roundToPx() }
        val skyH = with(density) { StageHeight.roundToPx() }
        val frame = if (coordinates != null && facing != null) {
            remember(shown, facing, coordinates, w, skyH, cloudNow, wetNow) {
                WindowScene.frame(
                    table = table, coords = coordinates, zone = zone, southern = tonight.southern,
                    drawAt = shown, facingDeg = facing, w = w, skyH = skyH, density = density.density,
                    cloudPct = cloudNow, layers = null, precipPct = wetNow,
                    fall = SkyFall.NONE, fallMmPerHour = 0.0, windKph = 0.0, windFromDeg = 0, followed = followed
                )
            }
        } else {
            null
        }
        // Without a place the card keeps the sky it had: the gradient of the night, or of
        // the instant under the finger, with the moon lifted when it is up.
        val fallback = frame?.gradient ?: table.gradient(
            sunAltitudeDeg = NightAltitude,
            cloudPct = reading?.cloudPct ?: 0,
            precipPct = reading?.precipPct ?: 0,
            moonIllumination = illumination,
            moonAltitudeDeg = if (reading?.moonUp ?: moonUp) MoonLiftAltitude else -10.0
        )
        val labels = if (frame != null && coordinates != null) {
            remember(frame) {
                WindowScene.labels(
                    context, frame, coordinates, w, skyH, cloudPct = cloudNow,
                    // One moon number per card: the night's, which the lines below print.
                    moonPct = tonight.moonIlluminationPct ?: WindowScene.moonPct(shown)
                )
            }
        } else {
            emptyList()
        }
        val looking = frame?.takeIf { it.night }?.let { WindowText.looking(context, it.facingDeg, it.drawAt, zone) }
        Column {
            CompositionLocalProvider(
                LocalChiaroColors provides night,
                LocalContentColor provides Color.White
            ) {
                TonightStage(
                    frame = frame,
                    fallback = fallback,
                    labels = labels,
                    moving = moving,
                    looking = looking,
                    // From the state, not from the composed value: a drag's events come
                    // faster than frames, and each step must start from the last one's answer.
                    onTurn = { dx -> turn = turnedFacing(turn ?: autoFacing ?: StarField.facing(tonight.southern), dx, skyH) },
                    bare = bareSky,
                    onBare = if (frame != null) onBareSky else null,
                    badge = if (frame == null) {
                        tonight.moonIlluminationPct?.let { pct ->
                            {
                                val badgeAlpha = if (reading != null && !reading.moonUp) MoonSetAlpha else 1f
                                MoonBadge(
                                    illumination = illumination.toFloat(),
                                    litRight = litRight,
                                    pct = pct,
                                    modifier = Modifier.graphicsLayer { alpha = badgeAlpha }
                                )
                            }
                        }
                    } else {
                        null
                    }
                ) {
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
                                style = MaterialTheme.typography.headlineMedium.copy(shadow = skyInkShadow()),
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
                                style = MaterialTheme.typography.bodyMedium.tabular().copy(shadow = skyInkShadow()),
                                color = Color.White.copy(alpha = SecondaryAlpha)
                            )
                        }
                    } else {
                        NoWindowHeadline(tonight)
                    }
                }
                // The ground under the stage: the sky's bottom stop, darkened, the tall
                // window's own rule for the rows under its picture.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(StageGround.compositeOver(fallback.bottom))
                        .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (window == null) {
                        NoWindowText(tonight, zone, timeFmt)
                    } else {
                        WindowLines(tonight, window, zone, timeFmt)
                    }
                    if (span?.end != null && (window != null || tonight.reason == SkyNotScheduled.MOON_ALL_NIGHT)) {
                        ScrubLine(
                            reading = reading, zone = zone, timeFmt = timeFmt,
                            moonKnown = tonight.moonIlluminationPct != null,
                            onClear = { scrub = null }
                        )
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
    }
}

/**
 * The stage (22 ott 2026): the top of the card, where the sky of the night's instant is
 * drawn by the hero's own shader through the window's rules ([WindowScene]: the stars that
 * are there, the moon where the stars' camera puts it, the planets, the Milky Way), with
 * the names of the brightest things written where they stand and, at the top right, where
 * the window looks and the hour it is drawn for. A horizontal drag turns it; the vertical
 * one stays the page's, the strip's own rule. The headline stands at the top left over a
 * scrim, with the canvas' halo (§3.6), and no name is written under it.
 */
@Composable
private fun TonightStage(
    frame: WindowFrame?,
    fallback: SkyGradient,
    labels: List<WindowLabel>,
    moving: Boolean,
    looking: String?,
    onTurn: (Float) -> Unit,
    /** «Behind the clouds», and its switch; null without a picture to take the clouds off. */
    bare: Boolean,
    onBare: ((Boolean) -> Unit)?,
    badge: (@Composable () -> Unit)?,
    headline: @Composable ColumnScope.() -> Unit
) {
    val density = LocalDensity.current
    var headlineBox by remember { mutableStateOf<RectF?>(null) }
    var lookingBox by remember { mutableStateOf<RectF?>(null) }
    var chipBox by remember { mutableStateOf<RectF?>(null) }
    var stageCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val latestOnTurn by rememberUpdatedState(onTurn)
    val stageDescription = looking ?: stringResource(R.string.sky_tonight_label)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(StageHeight)
            .onGloballyPositioned { stageCoords = it }
            .semantics { contentDescription = stageDescription }
            .pointerInput(frame != null) {
                if (frame == null) return@pointerInput
                detectHorizontalDragGestures { change, dx ->
                    change.consume()
                    latestOnTurn(dx)
                }
            }
    ) {
        if (frame != null) {
            SkyShaderLayer(
                gradient = frame.gradient,
                scene = frame.scene,
                moving = moving,
                modifier = Modifier.matchParentSize(),
                band = SkyBand(top = 0.dp, height = StageHeight)
            )
        } else {
            Spacer(
                modifier = Modifier
                    .matchParentSize()
                    .background(Brush.verticalGradient(fallback.stops()))
            )
        }
        // The headline's scrim, from the top: the canvas' own (§3.6), fading out where
        // the sky is the subject.
        Spacer(
            modifier = Modifier
                .matchParentSize()
                .drawBehind {
                    drawRect(
                        Brush.verticalGradient(
                            0f to SkyPalette.ScrimColor.copy(alpha = SkyPalette.ScrimAlpha),
                            StageScrimEnd to Color.Transparent
                        )
                    )
                }
        )
        if (frame != null && labels.isNotEmpty()) {
            val measurer = rememberTextMeasurer()
            val style = MaterialTheme.typography.labelMedium.copy(
                color = Color.White.copy(alpha = LabelAlpha),
                shadow = skyInkShadow()
            )
            val measured = remember(labels, style) { labels.associateWith { measurer.measure(AnnotatedString(it.text), style) } }
            val ringColor = Color.White.copy(alpha = LabelAlpha)
            Canvas(modifier = Modifier.matchParentSize().clearAndSetSemantics { }) {
                val textSize = style.fontSize.toPx()
                val placed = WindowScene.place(
                    labels, size.width.toInt(), size.height.toInt(), density.density,
                    keepOutBottomPx = StageLabelFloor.toPx(),
                    keepOut = listOfNotNull(headlineBox, lookingBox, chipBox),
                    textSizePx = textSize,
                    descentPx = textSize * 0.25f,
                    measure = { text -> measured.entries.first { it.key.text == text }.value.size.width.toFloat() }
                )
                placed.forEach { (label, x, baseline) ->
                    val layout = measured.getValue(label)
                    if (label.ring) {
                        drawCircle(ringColor, radius = RadiantRing.toPx(), center = Offset(label.x, label.y), style = Stroke(1.dp.toPx()))
                    }
                    drawText(layout, topLeft = Offset(x, baseline - layout.firstBaseline))
                }
            }
        }
        // «Tonight» and, at the end of its line, where the window looks: the headline keeps
        // the whole width under them (the compass beside the headline broke «A moonlit night»
        // in two, looked at on 22 ott 2026).
        fun boxOf(c: LayoutCoordinates): RectF? = stageCoords?.let { stage ->
            val r = stage.localBoundingBoxOf(c, clipBounds = false)
            RectF(r.left, r.top, r.right, r.bottom)
        }
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Text(
                    text = stringResource(R.string.sky_tonight_label),
                    style = MaterialTheme.typography.labelLarge.copy(shadow = skyInkShadow()),
                    color = Color.White.copy(alpha = SecondaryAlpha),
                    modifier = Modifier.weight(1f)
                )
                if (looking != null) {
                    Text(
                        text = looking,
                        style = MaterialTheme.typography.labelMedium.tabular().copy(shadow = skyInkShadow()),
                        color = Color.White.copy(alpha = SecondaryAlpha),
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .onGloballyPositioned { lookingBox = boxOf(it) }
                            .clearAndSetSemantics { }
                    )
                }
                badge?.invoke()
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp)
                    .onGloballyPositioned { headlineBox = boxOf(it) },
                verticalArrangement = Arrangement.spacedBy(2.dp),
                content = headline
            )
        }
        // «Behind the clouds» as a filter chip at the stage's foot (committente, 22 ott 2026):
        // the reader who looks at a covered stage wants to peek there, not in Settings; a
        // visible, labelled switch and not a hidden tap. Selected, the picture is the sky
        // without the hour's cover, veil and rain; the words keep the forecast.
        if (onBare != null) {
            FilterChip(
                selected = bare,
                onClick = { onBare(!bare) },
                label = { Text(stringResource(R.string.window_bare_note), style = MaterialTheme.typography.labelMedium) },
                leadingIcon = if (bare) {
                    {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = null, // the label beside it says it; the state is the chip's
                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                        )
                    }
                } else {
                    null
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = SkyPalette.ScrimColor.copy(alpha = ChipScrim),
                    labelColor = Color.White.copy(alpha = SecondaryAlpha),
                    selectedContainerColor = Color.White.copy(alpha = ChipFill),
                    selectedLabelColor = Color.White,
                    selectedLeadingIconColor = Color.White
                ),
                border = null,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 8.dp)
                    .onGloballyPositioned { chipBox = boxOf(it) }
            )
        }
    }
}

/**
 * The instant the card's picture is of when no finger asks for one (22 ott 2026): now while
 * the night is in progress, the night's middle otherwise, which is the hour the card's
 * moon is read at (`MoonOfTheNight`), so the disc in the picture and the number in the
 * lines agree. A night with no dawn in it gives its start.
 */
internal fun tonightInstant(now: Instant, night: SkyOccurrence.At): Instant {
    val end = night.end ?: return night.start
    return if (!now.isBefore(night.start) && now.isBefore(end)) {
        now
    } else {
        night.start.plus(Duration.between(night.start, end).dividedBy(2))
    }
}

/**
 * Where the window looks after a finger moved [dxPx] across a stage [skyHeightPx] tall: a
 * drag to the right brings the sky to the left of the camera into view, so the bearing
 * falls, by the stars' camera's own focal length ([WindowScene.degreesPerPx]).
 */
internal fun turnedFacing(facingDeg: Double, dxPx: Float, skyHeightPx: Int): Double =
    ((facingDeg - dxPx * WindowScene.degreesPerPx(skyHeightPx)) % 360.0 + 360.0) % 360.0

/**
 * The answer at the top of a night with no dark window (review point 1, 14 ott 2026): a
 * word in the verdict's place, so the card answers before it explains — until then the
 * hero of a full-moon night was four lines of bold prose and no answer at all.
 *
 * It is **not a verdict**, and is not dressed as one: no mark, no verdict colour. The four
 * verdicts judge a dark window, and there is none to judge; «Niente da fare» in red would
 * be false on the night the moon itself is the sight. So it is a statement of what the
 * night is, in white, with its arithmetic under it as a verdict would carry it: the
 * night's mean cloud, beside the moon's own percentage on the badge — the two numbers
 * that decide whether it is worth looking up at all.
 */
@Composable
private fun NoWindowHeadline(tonight: Tonight) {
    val res = LocalContext.current.resources
    val word = when (tonight.reason) {
        SkyNotScheduled.MOON_ALL_NIGHT -> R.string.sky_tonight_headline_moon
        SkyNotScheduled.DARK_ALL_DAY -> R.string.sky_tonight_headline_dark_all_day
        else -> R.string.sky_tonight_headline_no_darkness
    }
    Text(
        text = stringResource(word),
        style = MaterialTheme.typography.headlineMedium,
        color = Color.White
    )
    // The moon's percentage is the badge's, right beside this: said once.
    meanCloud(tonight.hours)?.let { res.getString(R.string.sky_evidence_cloud, it) }?.let { evidence ->
        Text(
            text = evidence,
            style = MaterialTheme.typography.bodyMedium.tabular(),
            color = Color.White.copy(alpha = SecondaryAlpha)
        )
    }
}

/**
 * The night's mean cloud cover, the number a whole-night statement rests on, or null
 * when the forecast has none of its hours (never a zero nobody forecast).
 */
internal fun meanCloud(hours: List<NightHour>): Int? =
    hours.takeIf { it.isNotEmpty() }?.map { it.cloudPct }?.average()?.roundToInt()

/**
 * The times of the night, big and tabular (review point 1): what a reader came for,
 * which used to be the middle of a sentence. Its label says which times they are; a
 * screen reader hears the whole sentence instead of two clock readings.
 */
@Composable
private fun NightTimes(label: String, start: String, end: String, spoken: String) {
    Column(
        modifier = Modifier
            .padding(top = 8.dp)
            .clearAndSetSemantics { contentDescription = spoken }
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = Color.White.copy(alpha = SecondaryAlpha)
        )
        Text(
            text = "$start – $end",
            style = MaterialTheme.typography.headlineSmall.tabular(),
            color = Color.White
        )
    }
}

/** The three skies with no window (see [Tonight.reason]): the moon's night shows its
 * dark hours like a window would, the other two are a fact about latitude and season. */
@Composable
private fun NoWindowText(tonight: Tonight, zone: ZoneId, timeFmt: DateTimeFormatter) {
    // The RIGHT fact: an empty window is also the deep polar night, where it is dark at
    // noon and "never gets fully dark" would be the reverse of the truth (8 set 2026).
    val night = tonight.night
    val moonPct = tonight.moonIlluminationPct
    if (tonight.reason == SkyNotScheduled.MOON_ALL_NIGHT && night?.end != null && moonPct != null) {
        val start = night.start.atZone(zone).format(timeFmt)
        val end = night.end!!.atZone(zone).format(timeFmt)
        NightTimes(
            label = stringResource(R.string.sky_tonight_dark_label),
            start = start,
            end = end,
            spoken = stringResource(R.string.sky_tonight_dark_between, start, end)
        )
        Text(
            text = stringResource(R.string.sky_tonight_moon_all_night, moonPct),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = SecondaryAlpha)
        )
        return
    }
    val text = when (tonight.reason) {
        SkyNotScheduled.DARK_ALL_DAY -> stringResource(R.string.sky_tonight_dark_all_day)
        else -> stringResource(R.string.sky_tonight_no_darkness)
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = Color.White,
        modifier = Modifier.padding(top = 8.dp)
    )
}

/** The window, what the moon took from it, and when it is clearest. */
@Composable
private fun WindowLines(
    tonight: Tonight,
    window: SkyOccurrence.At,
    zone: ZoneId,
    timeFmt: DateTimeFormatter
) {
    val start = window.start.atZone(zone).format(timeFmt)
    val end = (window.end ?: window.start).atZone(zone).format(timeFmt)
    NightTimes(
        label = stringResource(R.string.sky_tonight_window_label),
        start = start,
        end = end,
        spoken = stringResource(R.string.sky_tonight_window, start, end)
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
/**
 * Over the strip: the hint until a finger asks for an hour; then the hour held, as an input
 * chip (22 ott 2026: the hour stays when the finger lifts, so the reader can set it and then
 * turn the sky; until then it fell back to the whole night on the lift, the 14 ott rule, which
 * made «set the hour, then look around» impossible). The chip's × is the way back to the
 * whole night, and so is a tap on the chip.
 */
@Composable
private fun ScrubLine(
    reading: NightReading?,
    zone: ZoneId,
    timeFmt: DateTimeFormatter,
    moonKnown: Boolean,
    onClear: () -> Unit
) {
    if (reading == null) {
        Text(
            text = stringResource(R.string.sky_scrub_hint),
            style = MaterialTheme.typography.labelLarge.tabular(),
            color = Color.White.copy(alpha = HintAlpha),
            modifier = Modifier
                .padding(top = 6.dp)
                .clearAndSetSemantics { }
        )
        return
    }
    InputChip(
        selected = true,
        onClick = onClear,
        label = { Text(scrubText(reading, zone, timeFmt, moonKnown), style = MaterialTheme.typography.labelLarge.tabular()) },
        trailingIcon = {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = stringResource(R.string.sky_scrub_whole_night),
                modifier = Modifier.size(InputChipDefaults.IconSize)
            )
        },
        colors = InputChipDefaults.inputChipColors(
            selectedContainerColor = Color.White.copy(alpha = ChipFill),
            selectedLabelColor = Color.White,
            selectedTrailingIconColor = Color.White
        ),
        border = null,
        modifier = Modifier.padding(top = 2.dp)
    )
}

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
                    // The hour stays on the lift (22 ott 2026): the chip over the strip is the way back.
                    onDragEnd = {},
                    onDragCancel = {},
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
                    // The hour stays on the lift (22 ott 2026): the chip over the strip is the way back.
                    onDragEnd = {},
                    onDragCancel = {},
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

/**
 * A value of the night's hours read between them (22 ott 2026): the hour that holds [at]
 * and the next, by the minutes into the hour, so the picture's cover slides where the
 * forecast steps; the hour's own value where there is no next hour, and null with no
 * hour holding the instant. For the drawing only: a printed number stays the hour's.
 */
internal fun betweenHours(hours: List<NightHour>, at: Instant, value: (NightHour) -> Float): Float? {
    val hour = hours.lastOrNull { !it.at.isAfter(at) && at.isBefore(it.at.plusSeconds(3600)) } ?: return null
    val next = hours.firstOrNull { it.at == hour.at.plusSeconds(3600) } ?: return value(hour)
    val fraction = Duration.between(hour.at, at).toMinutes() / 60f
    return value(hour) + (value(next) - value(hour)) * fraction
}

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
/** The stage: the sky at the top of the card, as tall as a window on a home screen (22 ott 2026). */
private val StageHeight = 224.dp
/** Where the headline's scrim has faded to nothing, as a fraction of the stage. */
private const val StageScrimEnd = 0.6f
/** The ground under the stage: this much black over the sky's bottom stop (the tall window's shade). */
private val StageGround = Color.Black.copy(alpha = 0.45f)
/** No name in the last of the stage: the ground's edge is where the lines start. */
private val StageLabelFloor = 10.dp
private const val LabelAlpha = 0.85f
/** The chips on the sky: a selected one's white fill, an idle one's scrim. */
private const val ChipFill = 0.18f
private const val ChipScrim = 0.3f
private val RadiantRing = 5.dp
private const val NightAltitude = -24.0
private const val MoonLiftAltitude = 30.0
private const val SecondaryAlpha = 0.72f
private const val HintAlpha = 0.6f
private const val MoonSetAlpha = 0.35f
/** The app prints a precipitation probability from 20% up (the hour strip's rule). */
private const val PrecipPrintedFrom = 20
private const val ScrubStepMs = 5 * 60_000L
private const val StarCount = 46
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
