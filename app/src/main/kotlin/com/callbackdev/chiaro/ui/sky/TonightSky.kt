package com.callbackdev.chiaro.ui.sky

import android.graphics.RectF
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.data.PowerSaveState
import com.callbackdev.chiaro.domain.model.CloudLayers
import com.callbackdev.chiaro.domain.model.Coordinates
import com.callbackdev.chiaro.domain.sky.SkyNotScheduled
import com.callbackdev.chiaro.domain.sky.SkyOccurrence
import com.callbackdev.chiaro.domain.sky.SkyVerdictKind
import com.callbackdev.chiaro.ui.components.LocalAnimatedSky
import com.callbackdev.chiaro.ui.components.RibbonMarker
import com.callbackdev.chiaro.ui.components.RibbonPastAlpha
import com.callbackdev.chiaro.ui.components.SkyBand
import com.callbackdev.chiaro.ui.components.SkyCanvas
import com.callbackdev.chiaro.ui.components.SkyFall
import com.callbackdev.chiaro.ui.components.SkySheet
import com.callbackdev.chiaro.ui.components.VerdictKind
import com.callbackdev.chiaro.ui.components.moonLitPath
import com.callbackdev.chiaro.ui.components.nowDisc
import com.callbackdev.chiaro.ui.components.nowRing
import com.callbackdev.chiaro.ui.format.LocalClock
import com.callbackdev.chiaro.ui.format.currentLocale
import com.callbackdev.chiaro.ui.icons.ChiaroIcons
import com.callbackdev.chiaro.ui.icons.LocalMotionPaused
import com.callbackdev.chiaro.ui.shell.PageColumns
import com.callbackdev.chiaro.ui.theme.ChiaroMotion
import com.callbackdev.chiaro.ui.theme.ChiaroTheme
import com.callbackdev.chiaro.ui.theme.LocalChiaroColors
import com.callbackdev.chiaro.ui.theme.LocalChiaroType
import com.callbackdev.chiaro.ui.theme.SkyPalette
import com.callbackdev.chiaro.ui.theme.VerdictColors
import com.callbackdev.chiaro.ui.theme.reducedMotion
import com.callbackdev.chiaro.ui.theme.reflowForText
import com.callbackdev.chiaro.ui.theme.skyInkShadow
import com.callbackdev.chiaro.ui.theme.tabular
import com.callbackdev.chiaro.ui.today.HeroFigure
import com.callbackdev.chiaro.ui.today.HeroGroupGap
import com.callbackdev.chiaro.ui.today.HeroLineGap
import com.callbackdev.chiaro.ui.today.HeroRibbonGap
import com.callbackdev.chiaro.ui.today.heroScaled
import com.callbackdev.chiaro.widget.window.WindowFrame
import com.callbackdev.chiaro.widget.window.WindowLabel
import com.callbackdev.chiaro.widget.window.WindowScene
import com.callbackdev.chiaro.widget.window.WindowText
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/**
 * The hero of the Sky screen (VISION §5.3, DESIGN §8.8b): **the night it is about**, and since
 * Fase 16.1 (10 nov 2026) the canvas the page opens on, as Today opens on its sky. Until then
 * it was a card 224dp tall inset in the page, the smallest thing on a screen about the sky.
 *
 * Top to bottom, on one canvas that reaches the screen's top edge ([SkyCanvas], the one
 * Today's hero stands on, with the page's sheet laid on its foot):
 *
 * - **the stage**: the sky of the night's instant drawn through the window's rules
 *   ([WindowScene.frame], unchanged: the stars that are there, the moon where the stars'
 *   camera puts it, the planets, the Milky Way, the hour's clouds), from the top edge down
 *   to the words. The pinned place row stands on its top; at its foot, where it looks and the
 *   hour it is drawn for, and «Behind the clouds». A horizontal drag turns it.
 * - **the words**, where Today has the temperature: the night's verdict as the hero's figure
 *   (its mark and its word in the verdict's ink), the number that decided it, the dark hours,
 *   what the moon took, the clearest stretch.
 * - **the night's ribbon** ([NightRibbon]), in the form of Today's: dusk to dawn as an 8dp
 *   band, a handle on the night.
 *
 * The picture stops where the words start, and the words stand on the sky's own bottom
 * colour (`SkyCanvas`' stage): the camera's lowest degrees are where a rising moon and the
 * evening's planets are, and a picture as tall as the canvas had them behind the verdict.
 *
 * The canvas is the night's in both themes ([ChiaroTheme.nightColors] for the verdict's ink),
 * dark for the reason the sky is at 23:00. By day it shows the night that is coming, at the
 * night's middle, and the line at the stage's foot says which hour that is.
 */
@Composable
internal fun TonightHero(
    tonight: Tonight,
    zone: ZoneId,
    timeFmt: DateTimeFormatter,
    /** The canvas is at least this tall, the sheet's lip included ([skyHeroHeight]). */
    floor: Dp,
    modifier: Modifier = Modifier,
    /** The pinned place row's height, the status bar included: no name is written under it. */
    placeRowHeight: Dp = 0.dp,
    /** The place, for the real sky; without it the canvas keeps the night's gradient and
     * the moon is a badge. */
    coordinates: Coordinates? = null,
    /** The job ids the reader follows: a weak shower's radiant is drawn only for them. */
    followed: Set<String> = emptySet(),
    /** «Behind the clouds» (22 ott 2026): the picture without the hour's cover, veil and
     * rain, the reader's choice, kept across launches; the words keep the forecast. */
    bareSky: Boolean = false,
    onBareSky: (Boolean) -> Unit = {},
    /** The explorer (Fase 16.6): the pill under the place row, drawn where there is a real
     * sky to open (a place with coordinates) and somewhere to open it. It hands over the
     * hour held on the ribbon, null for the night as a whole, and where the stage looks. */
    onExplore: ((Instant?, Double?) -> Unit)? = null,
    /** Where the verdict rests, measured for the bar (DESIGN §8.1e); null measures nothing. */
    figure: HeroFigure? = null,
    /** How the paper under the canvas is laid out (§8.12c): the words stand over its first pane. */
    columns: PageColumns? = null,
    /** A navigation rail over the canvas' start (§8.12c): the sky runs under it, nothing else. */
    railInset: Dp = 0.dp
) {
    val context = LocalContext.current
    val window = tonight.window
    val verdict = tonight.verdict
    val night = ChiaroTheme.nightColors
    val moonUp = tonight.moonHeldTheStart || tonight.moonTookTheEnd ||
        tonight.reason == SkyNotScheduled.MOON_ALL_NIGHT
    val illumination = (tonight.moonIlluminationPct ?: 0) / 100.0
    // The instant a finger (or a screen reader) is asking about on the ribbon, null for
    // the night as a whole (14 ott 2026). While it is set the canvas paints that instant's
    // sky; it stays when the finger lifts, and the chip at the stage's foot gives the night back.
    var scrub by remember(tonight.night) { mutableStateOf<Instant?>(null) }
    val reading = scrub?.let { nightReading(tonight, it) }
    // The instant the picture is of (22 ott 2026): the finger's, else now while the night
    // is in progress, else the night's middle, the hour the night's moon is read at.
    val now = Instant.now(LocalClock.current)
    val span = tonight.night
    val shown = scrub ?: span?.let { tonightInstant(now, it) } ?: now
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
    // The cover's layers slide with it (28 ott 2026): the low and the middle are the deck
    // the shader draws, the high is the veil the stars show through, as Today's hero and the
    // window widget already read them (`cloudDecks`, `CloudLayers.lightTaken`). An hour the
    // model did not split is all deck, which is what the shader makes of no layers at all,
    // so the picture never dips between a split hour and an unsplit one ([layerShares]).
    val low by animateFloatAsState(
        targetValue = if (bareSky) 0f else betweenHours(tonight.hours, shown) { layerShares(it).low } ?: 0f,
        animationSpec = ChiaroMotion.effects(reduced),
        label = "tonight-low"
    )
    val mid by animateFloatAsState(
        targetValue = if (bareSky) 0f else betweenHours(tonight.hours, shown) { layerShares(it).mid } ?: 0f,
        animationSpec = ChiaroMotion.effects(reduced),
        label = "tonight-mid"
    )
    val high by animateFloatAsState(
        targetValue = if (bareSky) 0f else betweenHours(tonight.hours, shown) { layerShares(it).high } ?: 0f,
        animationSpec = ChiaroMotion.effects(reduced),
        label = "tonight-high"
    )
    val cloudNow = cover.roundToInt()
    val wetNow = wet.roundToInt()
    val layersNow = CloudLayers(low.roundToInt(), mid.roundToInt(), high.roundToInt())
    val kind = SkyText.chipKind(verdict?.kind ?: SkyVerdictKind.UNKNOWN)
    val ink = verdictColors(night, kind).ink
    val litRight = ((tonight.moonElongationDeg ?: 0.0) < 180.0) != tonight.southern
    // Where the stage looks (22 ott 2026): the window's own choice at the night's instant,
    // made once a night so the sky moves under a finger on the ribbon and the camera does
    // not; or where the reader turned it, kept while the page is open.
    // How far the reader turned it, degrees to the right, kept while the page is open and
    // never wrapped (25 ott 2026): the clouds move by it ([SkyScene.turn]), and the field they
    // are read from has no period, so a wrap at north would make them jump.
    var turned by remember(tonight.night) { mutableStateOf(0.0) }
    val autoFacing = remember(tonight.night, coordinates) {
        coordinates?.let { WindowScene.facing(shown, it, zone, tonight.southern, followed) }
    }
    val facing = autoFacing?.let { turnedFacing(it, turned) }
    val table = ChiaroTheme.sky
    val density = LocalDensity.current
    val saving = remember(context) { PowerSaveState.of(context) }
    val moving = LocalAnimatedSky.current && !reducedMotion() && !LocalMotionPaused.current && !saving.isOn()
    // The stage's box, as the column lays it out: the camera's picture is computed for it, so
    // the frame is one layout behind the first measure and the canvas draws the night's
    // gradient until it knows.
    var stage by remember { mutableStateOf(IntSize.Zero) }
    val w = stage.width
    val skyH = stage.height
    val frame = if (coordinates != null && facing != null && w > 0 && skyH > 0) {
        remember(shown, facing, turned, coordinates, w, skyH, cloudNow, layersNow, wetNow) {
            WindowScene.frame(
                table = table, coords = coordinates, zone = zone, southern = tonight.southern,
                drawAt = shown, facingDeg = facing, w = w, skyH = skyH, density = density.density,
                cloudPct = cloudNow, turnDeg = turned, layers = layersNow, precipPct = wetNow,
                fall = SkyFall.NONE, fallMmPerHour = 0.0, windKph = 0.0, windFromDeg = 0, followed = followed
            )
        }
    } else {
        null
    }
    // Without a place the canvas keeps the sky it had: the gradient of the night, or of
    // the instant under the finger, with the moon lifted when it is up.
    val gradient = frame?.gradient ?: table.gradient(
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
                // One moon number per hero: the night's, which the lines below print.
                moonPct = tonight.moonIlluminationPct ?: WindowScene.moonPct(shown)
            )
        }
    } else {
        emptyList()
    }
    val looking = frame?.takeIf { it.night }?.let { WindowText.looking(context, it.facingDeg, it.drawAt, zone) }
    val sheet = SkySheet(surface = MaterialTheme.colorScheme.surface, start = railInset)
    val stageHeight = with(density) { skyH.toDp() }
    // The canvas item's own coordinates, for where the verdict rests inside it.
    var canvasCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    CompositionLocalProvider(
        LocalChiaroColors provides night,
        LocalContentColor provides SkyPalette.Ink
    ) {
        SkyCanvas(
            gradient = gradient,
            minHeight = floor,
            sheet = sheet,
            scene = frame?.scene,
            moving = moving,
            // The band is the whole picture: the moon stands where the stars' camera puts it.
            band = frame?.let { SkyBand(top = 0.dp, height = stageHeight) },
            stage = frame?.let { stageHeight },
            modifier = modifier.onGloballyPositioned { canvasCoords = it }
        ) {
            NightColumn(seat = placeRowHeight, floor = floor - sheet.lip) {
                TonightStage(
                    frame = frame,
                    labels = labels,
                    looking = looking,
                    // The hour held, for the chip that gives the night back; with no picture
                    // to say where it looks, the chip is the hour alone.
                    held = scrub?.let { looking ?: it.atZone(zone).format(timeFmt) },
                    onWholeNight = { scrub = null },
                    // From the state, not from the composed value: a drag's events come
                    // faster than frames, and each step must start from the last one's answer.
                    onTurn = { dx -> turned += turnDegrees(dx, skyH) },
                    bare = bareSky,
                    onBare = if (frame != null) onBareSky else null,
                    onExplore = if (coordinates != null && onExplore != null) {
                        { onExplore(scrub, frame?.facingDeg) }
                    } else {
                        null
                    },
                    badge = if (coordinates == null) {
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
                    },
                    seat = placeRowHeight,
                    railInset = railInset,
                    modifier = Modifier.onSizeChanged { stage = it }
                )
                TonightWords(
                    tonight = tonight,
                    zone = zone,
                    timeFmt = timeFmt,
                    kind = kind,
                    ink = ink,
                    reading = reading,
                    shown = shown,
                    scrub = scrub,
                    onScrub = { scrub = it },
                    clear = night.pass.ink,
                    modifier = Modifier
                        .padding(start = columns?.start ?: railInset)
                        .widthIn(max = columns?.takeIf { it.twoPanes }?.pane ?: Dp.Infinity)
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = WordsTop, end = 16.dp, bottom = 16.dp),
                    figureModifier = if (figure == null) {
                        Modifier
                    } else {
                        Modifier.onGloballyPositioned { word ->
                            val item = canvasCoords ?: return@onGloballyPositioned
                            if (!item.isAttached || !word.isAttached) return@onGloballyPositioned
                            figure.rest = item.localPositionOf(word, Offset.Zero)
                            figure.size = word.size
                        }
                    }
                )
            }
        }
    }
}

/**
 * The hero's column: the stage, then the words, top to bottom. The stage takes what the
 * [floor] leaves over the words and never less than [StageMin] under the place row's [seat]:
 * the sky is the hero of this screen, and at a large type the canvas grows rather than leave
 * the picture a strip. Today's `SkyColumn`, with the seat inside the band: the picture runs
 * under the place row to the screen's top edge.
 *
 * Children: the stage, then the words.
 */
@Composable
private fun NightColumn(seat: Dp, floor: Dp, content: @Composable () -> Unit) {
    Layout(content = content) { measurables, constraints ->
        val width = constraints.maxWidth
        // Up to the full width and not exactly it: the words fill it themselves, and a wide
        // page narrows them to its first pane (§8.12c).
        val words = measurables[1].measure(Constraints(maxWidth = width))
        val stage = maxOf((seat + StageMin).roundToPx(), floor.roundToPx() - words.height)
        val sky = measurables[0].measure(Constraints.fixed(width, stage))
        layout(width, stage + words.height) {
            sky.place(0, 0)
            words.place(0, stage)
        }
    }
}

/**
 * How tall the Sky's canvas is at least, the sheet's lip included, on a page [page] tall
 * whose status bar is [statusTop] (pure, `TonightNightTest`): the page less [PaperInSight],
 * so the first of the moments ahead shows under the sky and says that the page goes on;
 * never less than Today's own canvas, never more than [SkyHeroMax], past which a tall tablet
 * would open on a sky and nothing else.
 */
internal fun skyHeroHeight(page: Dp, statusTop: Dp): Dp {
    val least = com.callbackdev.chiaro.ui.today.CanvasBaseHeight + statusTop
    return (page - PaperInSight).coerceIn(least, maxOf(least, SkyHeroMax))
}

/**
 * The stage (22 ott 2026; the top of the canvas since Fase 16.1): the names of the brightest
 * things written where they stand, never under the place row's [seat] nor on the foot's two
 * controls, and at the foot the line that says where the stage looks and the hour it is drawn
 * for, with «Behind the clouds» at its end. A horizontal drag turns the sky; the vertical one
 * stays the page's. The sky itself is the canvas' (`SkyCanvas`' stage), drawn under this.
 *
 * With an hour [held] on the ribbon the line becomes a chip with the same words and a cross:
 * the hour stays when the finger lifts (22 ott 2026), and the cross is the way back to the
 * night. It stands here and not over the ribbon, where it stood on the card: a chip in the
 * words would make them a line taller on the first touch, and the stage would shrink and the
 * whole picture rescale under the finger.
 */
@Composable
private fun TonightStage(
    frame: WindowFrame?,
    labels: List<WindowLabel>,
    looking: String?,
    held: String?,
    onWholeNight: () -> Unit,
    onTurn: (Float) -> Unit,
    /** «Behind the clouds», and its switch; null without a picture to take the clouds off. */
    bare: Boolean,
    onBare: ((Boolean) -> Unit)?,
    /** «Explore», the pill under the place row at the end; null where there is no sky to open. */
    onExplore: (() -> Unit)?,
    badge: (@Composable () -> Unit)?,
    seat: Dp,
    railInset: Dp,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    var lineBox by remember { mutableStateOf<RectF?>(null) }
    var chipBox by remember { mutableStateOf<RectF?>(null) }
    var pillBox by remember { mutableStateOf<RectF?>(null) }
    var stageCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val latestOnTurn by rememberUpdatedState(onTurn)
    val stageDescription = looking ?: stringResource(R.string.sky_tonight_label)
    val halo = skyInkShadow()
    // A control that left takes its box with it: a name may be written where it stood.
    SideEffect {
        if (held == null && looking == null) lineBox = null
        if (onBare == null) chipBox = null
        if (onExplore == null) pillBox = null
    }
    fun boxOf(c: LayoutCoordinates): RectF? = stageCoords?.let { stage ->
        val r = stage.localBoundingBoxOf(c, clipBounds = false)
        RectF(r.left, r.top, r.right, r.bottom)
    }
    Box(
        modifier = modifier
            .fillMaxSize()
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
        if (frame != null && labels.isNotEmpty()) {
            val measurer = rememberTextMeasurer()
            val style = MaterialTheme.typography.labelMedium.copy(color = SkyPalette.Ink.copy(alpha = LabelAlpha), shadow = halo)
            val measured = remember(labels, style) { labels.associateWith { measurer.measure(AnnotatedString(it.text), style) } }
            val ringColor = SkyPalette.Ink.copy(alpha = LabelAlpha)
            Canvas(modifier = Modifier.matchParentSize().clearAndSetSemantics { }) {
                val textSize = style.fontSize.toPx()
                val placed = WindowScene.place(
                    labels, size.width.toInt(), size.height.toInt(), density.density,
                    keepOutBottomPx = StageLabelFloor.toPx(),
                    keepOut = listOfNotNull(
                        // The place row, and the rail on a wide window: what stands over the sky.
                        RectF(0f, 0f, size.width, seat.toPx()),
                        RectF(0f, 0f, railInset.toPx(), size.height).takeIf { railInset > 0.dp },
                        lineBox,
                        chipBox,
                        pillBox
                    ),
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
        // Without a place the moon is a badge under the place row, at the end.
        badge?.let {
            Box(modifier = Modifier.align(Alignment.TopEnd).padding(top = seat + 4.dp, end = 16.dp)) { it() }
        }
        // «Explore» (Fase 16.6, board 1): under the place row, at the end, where the badge
        // stands on a page without a place; the two never meet, a sky to explore wants the
        // coordinates the badge is the lack of. The door to the whole sky, full screen: on
        // the canvas' scrim like the chip at the foot, and no name is written under it.
        if (onExplore != null) {
            val description = stringResource(R.string.sky_explore_description)
            AssistChip(
                onClick = onExplore,
                label = { Text(stringResource(R.string.sky_explore), style = MaterialTheme.typography.labelLarge) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = SkyPalette.ScrimColor.copy(alpha = ChipScrim),
                    labelColor = SkyPalette.Ink
                ),
                border = null,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = seat, end = 12.dp)
                    .onGloballyPositioned { pillBox = boxOf(it) }
                    .semantics { contentDescription = description }
            )
        }
        // The foot: where the stage looks and its hour at the start, «Behind the clouds» at the
        // end. 12 dp from the edges and, to the eye, 12 from the words: a chip's 48 dp touch
        // target adds 8 dp above and below its 32, so 4 here (from the device, 22 ott 2026).
        val line: @Composable () -> Unit = {
            if (held != null) {
                InputChip(
                    selected = true,
                    onClick = onWholeNight,
                    label = { Text(held, style = MaterialTheme.typography.labelLarge.tabular()) },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = stringResource(R.string.sky_scrub_whole_night),
                            modifier = Modifier.size(InputChipDefaults.IconSize)
                        )
                    },
                    colors = InputChipDefaults.inputChipColors(
                        selectedContainerColor = SkyPalette.Ink.copy(alpha = ChipFill),
                        selectedLabelColor = SkyPalette.Ink,
                        selectedTrailingIconColor = SkyPalette.Ink
                    ),
                    border = null,
                    modifier = Modifier.onGloballyPositioned { lineBox = boxOf(it) }
                )
            } else if (looking != null) {
                // Silent: the stage says the same words as its own description.
                Text(
                    text = looking,
                    style = MaterialTheme.typography.labelLarge.tabular().copy(shadow = halo),
                    color = SkyPalette.Ink,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .onGloballyPositioned { lineBox = boxOf(it) }
                        .clearAndSetSemantics { }
                )
            }
        }
        // «Behind the clouds» as a filter chip (committente, 22 ott 2026): the reader who
        // looks at a covered stage wants to peek there, not in Settings; a visible,
        // labelled switch and not a hidden tap. Selected, the picture is the sky without
        // the hour's cover, veil and rain; the words keep the forecast.
        val chip: @Composable (Modifier) -> Unit = { place ->
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
                        labelColor = SkyPalette.Ink,
                        selectedContainerColor = SkyPalette.Ink.copy(alpha = ChipFill),
                        selectedLabelColor = SkyPalette.Ink,
                        selectedLeadingIconColor = SkyPalette.Ink
                    ),
                    border = null,
                    modifier = place.onGloballyPositioned { chipBox = boxOf(it) }
                )
            }
        }
        val foot = Modifier
            .align(Alignment.BottomStart)
            .fillMaxWidth()
            .padding(start = railInset + 12.dp, end = 12.dp, bottom = 4.dp)
        if (reflowForText()) {
            // At a large type the two do not share a line (DESIGN §10): the chip took its
            // width and left the line three words wide, one to a row.
            Column(modifier = foot) {
                line()
                chip(Modifier.align(Alignment.End))
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = foot.heightIn(min = FootHeight)) {
                Box(modifier = Modifier.weight(1f)) { line() }
                chip(Modifier.padding(start = 8.dp))
            }
        }
    }
}

/**
 * The words under the stage, where Today has the temperature (Fase 16.1): the verdict as the
 * hero's figure, then what it rests on and when, then the night's ribbon. Every line is the
 * canvas' one white at full strength with its halo (DESIGN §3.6): the secondary ones are
 * told apart by their size, as on Today, never by a thinner white.
 *
 * A night with no dark window has **a word in the verdict's place** (review point 1, 14 ott
 * 2026) and it is not dressed as a verdict: no mark, no verdict colour. The four verdicts
 * judge a dark window, and there is none to judge; «Niente da fare» in red would be false on
 * the night the moon itself is the sight. So it is a statement of what the night is, in
 * white, with the night's mean cloud under it as a verdict carries its arithmetic.
 *
 * With an hour held on the ribbon the line under the verdict is that hour's reading (its
 * time, its cloud, its rain from the 20% the app prints a probability at, the moon): one line
 * for one line, as on Today, so nothing on the canvas moves when the finger lands.
 */
@Composable
private fun TonightWords(
    tonight: Tonight,
    zone: ZoneId,
    timeFmt: DateTimeFormatter,
    kind: VerdictKind,
    ink: Color,
    reading: NightReading?,
    shown: Instant,
    scrub: Instant?,
    onScrub: (Instant?) -> Unit,
    clear: Color,
    modifier: Modifier = Modifier,
    /** On the verdict, or the word in its place: what the bar takes over when it leaves. */
    figureModifier: Modifier = Modifier
) {
    val res = LocalContext.current.resources
    val locale = currentLocale()
    val window = tonight.window
    val verdict = tonight.verdict
    val span = tonight.night
    val halo = skyInkShadow()
    val figureStyle = heroScaled(LocalChiaroType.current.heroVerdict).copy(shadow = halo)
    val line = MaterialTheme.typography.bodyLarge.tabular().copy(shadow = halo)
    val note = MaterialTheme.typography.bodyMedium.tabular().copy(shadow = halo)
    val moonKnown = tonight.moonIlluminationPct != null
    val held = reading?.let { scrubText(it, zone, timeFmt, moonKnown) }
    fun clock(at: Instant) = at.atZone(zone).format(timeFmt)
    Column(modifier = modifier) {
        if (window != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = figureModifier
            ) {
                Icon(
                    painter = painterResource(ChiaroIcons.verdictMarkRes(kind)),
                    contentDescription = null, // the word beside it says it
                    tint = ink,
                    // On the word's own size, so the mark grows with it and stops where it does.
                    modifier = Modifier.size(with(LocalDensity.current) { figureStyle.fontSize.toDp() } * VerdictMarkShare)
                )
                Text(
                    text = stringResource(SkyText.verdictWordRes(verdict?.kind ?: SkyVerdictKind.UNKNOWN)),
                    style = figureStyle,
                    color = ink,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        } else {
            Text(
                text = stringResource(
                    when (tonight.reason) {
                        SkyNotScheduled.MOON_ALL_NIGHT -> R.string.sky_tonight_headline_moon
                        SkyNotScheduled.DARK_ALL_DAY -> R.string.sky_tonight_headline_dark_all_day
                        else -> R.string.sky_tonight_headline_no_darkness
                    }
                ),
                style = figureStyle,
                color = SkyPalette.Ink,
                modifier = figureModifier
            )
        }
        // The arithmetic, always (DESIGN §8.7), right under the word it decided, or the
        // reason there is no number yet; under a night's statement, the night's mean cloud.
        // The moon's percentage is in its own line, or on the picture: said once.
        val evidence = (
            if (window != null) {
                verdict?.let { SkyText.chipEvidence(res, it) ?: SkyText.unknownReason(res, it) }
            } else {
                meanCloud(tonight.hours)?.let { res.getString(R.string.sky_evidence_cloud, it) }
            }
            )?.sentence(locale)
        // The hour's reading takes the evidence's line and only that one: a night with no
        // line there gets none under a finger either, and the canvas keeps its height.
        if (evidence != null) {
            Text(text = held ?: evidence, style = line, color = SkyPalette.Ink)
        }
        if (window != null) {
            Text(
                text = stringResource(R.string.sky_tonight_window, clock(window.start), clock(window.end ?: window.start)),
                style = line,
                color = SkyPalette.Ink
            )
            // What the moon took out of the night, read off the window's edges against the
            // night's (Fase 27): the window IS the moonless part.
            val moonPct = tonight.moonIlluminationPct
            val moonLine = when {
                moonPct == null -> null
                tonight.moonHeldTheStart -> stringResource(R.string.sky_tonight_moon_sets, clock(window.start), moonPct)
                tonight.moonTookTheEnd ->
                    stringResource(R.string.sky_tonight_moon_rises, clock(window.end ?: window.start), moonPct)
                else -> null
            }
            moonLine?.let { Text(it, style = note, color = SkyPalette.Ink, modifier = Modifier.padding(top = HeroLineGap)) }
            // And WHEN, on a night the verdict had to average (Fase 28).
            tonight.clearStretch?.let { stretch ->
                Text(
                    text = stringResource(R.string.sky_tonight_clear_between, clock(stretch.start), clock(stretch.endInclusive)),
                    style = note,
                    color = SkyPalette.Ink,
                    modifier = Modifier.padding(top = HeroLineGap)
                )
            }
        } else {
            NoWindowLines(tonight, zone, timeFmt, line, note)
        }
        if (span?.end != null && (window != null || tonight.reason == SkyNotScheduled.MOON_ALL_NIGHT)) {
            NightRibbon(
                tonight = tonight,
                night = span,
                zone = zone,
                timeFmt = timeFmt,
                clear = clear,
                shown = shown,
                scrub = scrub,
                onScrub = onScrub,
                stateDescription = held
            )
        }
    }
}

/** A sentence's first letter, as the place's language writes one: the evidence is written
 * for a chip, after a word («Bello · nuvole 12%»), and here it opens its own line. */
private fun String.sentence(locale: Locale): String =
    replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }

/** The three skies with no window (see [Tonight.reason]): the moon's night says its dark
 * hours like a window would, the other two are a fact about latitude and season. */
@Composable
private fun NoWindowLines(
    tonight: Tonight,
    zone: ZoneId,
    timeFmt: DateTimeFormatter,
    line: androidx.compose.ui.text.TextStyle,
    note: androidx.compose.ui.text.TextStyle
) {
    // The RIGHT fact: an empty window is also the deep polar night, where it is dark at
    // noon and "never gets fully dark" would be the reverse of the truth (8 set 2026).
    val night = tonight.night
    val moonPct = tonight.moonIlluminationPct
    if (tonight.reason == SkyNotScheduled.MOON_ALL_NIGHT && night?.end != null && moonPct != null) {
        Text(
            text = stringResource(
                R.string.sky_tonight_dark_between,
                night.start.atZone(zone).format(timeFmt),
                night.end!!.atZone(zone).format(timeFmt)
            ),
            style = line,
            color = SkyPalette.Ink
        )
        Text(
            text = stringResource(R.string.sky_tonight_moon_all_night, moonPct),
            style = note,
            color = SkyPalette.Ink,
            modifier = Modifier.padding(top = HeroLineGap)
        )
        return
    }
    Text(
        text = when (tonight.reason) {
            SkyNotScheduled.DARK_ALL_DAY -> stringResource(R.string.sky_tonight_dark_all_day)
            else -> stringResource(R.string.sky_tonight_no_darkness)
        },
        style = line,
        color = SkyPalette.Ink,
        modifier = Modifier.padding(top = HeroGroupGap)
    )
}

/**
 * The verdict in the pinned bar (DESIGN §8.1e, Fase 16.1): what the bar carries once the
 * hero's verdict has gone under it, as Today's carries the temperature. The verdict's own
 * chip, mark and word, in the page's theme (the bar is paper by then); on a night with no
 * dark window the word that stood in the verdict's place, plain. One announcement.
 */
@Composable
internal fun TonightInBar(tonight: Tonight, color: Color) {
    val kind = tonight.verdict?.kind ?: SkyVerdictKind.UNKNOWN
    val word = stringResource(
        when {
            tonight.window != null -> SkyText.verdictWordRes(kind)
            tonight.reason == SkyNotScheduled.MOON_ALL_NIGHT -> R.string.sky_tonight_headline_moon
            tonight.reason == SkyNotScheduled.DARK_ALL_DAY -> R.string.sky_tonight_headline_dark_all_day
            else -> R.string.sky_tonight_headline_no_darkness
        }
    )
    val spoken = stringResource(R.string.sky_bar_tonight, word)
    Box(
        modifier = Modifier
            .padding(start = 8.dp)
            .clearAndSetSemantics { contentDescription = spoken }
    ) {
        if (tonight.window != null) {
            com.callbackdev.chiaro.ui.components.VerdictChip(kind = SkyText.chipKind(kind), label = word, evidence = null)
        } else {
            Text(text = word, style = MaterialTheme.typography.labelLarge, color = color)
        }
    }
}

/**
 * The night's date as the place row prints it under the place, longest form first («Tonight,
 * Tuesday 10 November», «Tonight, Tue 10 Nov», «Tonight, Tue»: the row draws the longest that
 * fits). The date is the evening's: after midnight the night under way is still the one that
 * began yesterday. In the sentence's own case, which in Italian is a small letter.
 */
internal fun nightDateForms(date: LocalDate, locale: Locale): List<String> =
    listOf("EEEE d MMMM", "EEE d MMM", "EEE").map { date.format(DateTimeFormatter.ofPattern(it, locale)) }

/** The evening the night belongs to: the date the builder made it for, and for a value made
 * by hand its dusk's date in the place's zone, or today's on a night with no dusk in it. */
internal fun nightDate(tonight: Tonight, now: Instant, zone: ZoneId): LocalDate =
    tonight.date ?: (tonight.night?.start ?: now).atZone(zone).toLocalDate()

/**
 * The instant the hero's picture is of when no finger asks for one (22 ott 2026): now while
 * the night is in progress, the night's middle otherwise, which is the hour the night's
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
 * The degrees a finger moved [dxPx] across a stage [skyHeightPx] tall turns the sky through:
 * a drag to the right brings the sky to the left of the camera into view, so the bearing
 * falls, by the stars' camera's own focal length ([WindowScene.degreesPerPx]). Summed, never
 * wrapped: the clouds are read by the sum ([SkyScene.turn]).
 */
internal fun turnDegrees(dxPx: Float, skyHeightPx: Int): Double =
    -dxPx * WindowScene.degreesPerPx(skyHeightPx).toDouble()

/** Where the stage looks after a turn of [turnDeg] from [facingDeg]: a bearing, wrapped at north. */
internal fun turnedFacing(facingDeg: Double, turnDeg: Double): Double =
    ((facingDeg + turnDeg) % 360.0 + 360.0) % 360.0

/**
 * The night's mean cloud cover, the number a whole-night statement rests on, or null
 * when the forecast has none of its hours (never a zero nobody forecast).
 */
internal fun meanCloud(hours: List<NightHour>): Int? =
    hours.takeIf { it.isNotEmpty() }?.map { it.cloudPct }?.average()?.roundToInt()


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
 * The night's ribbon (Fase 16.1, 10 nov 2026): dusk to dawn in the form of Today's ribbon, an
 * 8dp band with round ends and a disc on it, where the card had a 34dp strip with marks of
 * its own. One handle for the day and one for the night, read the same way.
 *
 * What the band says, in colour ([nightRibbonStops]): the night's own light, washed in
 * moonlight's straw for the hours the moon is up, and greyed by each hour's forecast cloud.
 * It is a depiction, as Today's is (DESIGN §4): every one of those is a sentence above it or
 * the reading under a finger. The disc stands on **the instant the picture is of**: now while
 * the night is under way, the night's middle before it, the finger's while an hour is held,
 * with «now» left as a ring so the way back is in sight; the night already spent is quieter,
 * as the spent day is. The clearest stretch is underlined in the ink of a good sky, and the
 * dark window's two times stand under its edges (the night's, on a night the moon owns).
 *
 * A control since 14 ott 2026, unchanged: a finger dragged along it, or held on it, asks for
 * one instant of the night, and the canvas answers with that instant's sky. A horizontal drag
 * starts at once, because the page scrolls vertically and the two never compete; holding
 * still needs the long press, so a vertical scroll that begins on the band does not flash a
 * reading. Every whole hour crossed is a light tick under the finger, the edges of the dark
 * window a firmer one. The reach is the band, its times and the air over it, so the finger
 * does not have to find 8dp. To a screen reader it is an adjustable control in whole hours
 * whose state is the reading: the hour-by-hour cloud is information no sentence carries.
 */
@Composable
private fun NightRibbon(
    tonight: Tonight,
    night: SkyOccurrence.At,
    zone: ZoneId,
    timeFmt: DateTimeFormatter,
    clear: Color,
    shown: Instant,
    scrub: Instant?,
    onScrub: (Instant?) -> Unit,
    stateDescription: String?,
    modifier: Modifier = Modifier
) {
    val start = night.start
    val end = night.end ?: return
    val window = tonight.window
    val table = ChiaroTheme.sky
    val stops = remember(tonight, table) {
        nightRibbonStops(
            tonight = tonight,
            // A shade over the night it lies on: a dark, clear hour is the ground's own
            // colour, and the band would have no shape exactly where the night is best.
            light = { sample -> lerp(table.gradient(sample.sunAltitudeDeg).mid, SkyPalette.Ink, TrackShare) },
            moon = SkyPalette.MoonWash,
            cloud = SkyPalette.Ink
        )
    }
    val measurer = rememberTextMeasurer()
    val now = Instant.now(LocalClock.current)
    val labelStyle = MaterialTheme.typography.labelSmall.tabular().copy(color = SkyPalette.Ink, shadow = skyInkShadow())
    val labels = listOfNotNull(
        (window?.start ?: start),
        (window?.end ?: end)
    ).map { it to measurer.measure(AnnotatedString(it.atZone(zone).format(timeFmt)), labelStyle) }
    val labelHeight = with(LocalDensity.current) { labels.maxOf { it.second.size.height }.toDp() }
    val lane = if (tonight.clearStretch != null) StretchLane else 0.dp

    // Ticks under the finger, decided on the instant the ribbon settles on, so a drag
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
    val ribbonDescription = stringResource(R.string.sky_scrub_strip_desc)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(HeroRibbonGap + RibbonMarker + lane + HeroLineGap + labelHeight)
            .pointerInput(start, end, window) {
                detectHorizontalDragGestures(
                    onDragStart = { scrubTo(it.x / size.width) },
                    // The hour stays on the lift (22 ott 2026): the chip on the stage is the way back.
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
                    // The hour stays on the lift (22 ott 2026): the chip on the stage is the way back.
                    onDragEnd = {},
                    onDragCancel = {},
                    onDrag = { change, _ ->
                        change.consume()
                        scrubTo(change.position.x / size.width)
                    }
                )
            }
            .semantics {
                contentDescription = ribbonDescription
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
        val w = size.width
        fun x(at: Instant) = w * nightFraction(at, start, end)
        // The band in the middle of the disc's box, under the air the finger reaches through.
        val box = RibbonMarker.toPx()
        val band = BandHeight.toPx()
        val boxTop = HeroRibbonGap.toPx()
        val top = boxTop + (box - band) / 2f
        val centre = boxTop + box / 2f
        val brush = Brush.horizontalGradient(colorStops = stops.toTypedArray(), startX = 0f, endX = w.coerceAtLeast(1f))
        fun band(alpha: Float) = drawRoundRect(
            brush = brush,
            topLeft = Offset(0f, top),
            size = Size(w, band),
            cornerRadius = CornerRadius(band / 2f),
            alpha = alpha
        )
        val underWay = now >= start && now <= end
        if (underWay) {
            clipRect(right = x(now)) { band(RibbonPastAlpha) }
            clipRect(left = x(now)) { band(1f) }
        } else {
            band(1f)
        }
        // The clearest stretch, underlined in the ink of a good sky.
        tonight.clearStretch?.let { stretch ->
            val x0 = x(stretch.start)
            val x1 = x(stretch.endInclusive)
            if (x1 > x0) {
                val y = boxTop + box + lane.toPx() / 2f
                drawLine(color = clear, start = Offset(x0, y), end = Offset(x1, y), strokeWidth = StretchLine.toPx(), cap = StrokeCap.Round)
            }
        }
        // The disc on the instant the picture is of; «now» a ring while a finger holds another.
        if (scrub != null && underWay) nowRing(Offset(x(now), centre), SkyPalette.Ink)
        nowDisc(Offset(x(shown), centre), SkyPalette.Ink)
        // The window's two times under its edges, or the night's on a night the moon owns.
        val labelTop = boxTop + box + lane.toPx() + HeroLineGap.toPx()
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
            val centreX = (x(first.first) + x(last.first)) / 2f
            drawText(
                both,
                topLeft = Offset(
                    (centreX - both.size.width / 2f).coerceIn(0f, (w - both.size.width).coerceAtLeast(0f)),
                    labelTop
                )
            )
        }
    }
}

/**
 * The ribbon's colours across the night, as (share of the night, colour), in order (pure,
 * `TonightNightTest`). One stop for each of the night's samples, every ten minutes:
 * [light] is the night's own colour for the sun's place then; where the moon is up it
 * leans toward [moon] by [MoonShare], and by the reading's own account of the moon
 * ([nightReading]: the window IS the moonless part, its edges included), so the band can
 * never wash an instant the reading calls moonless; then the hour's forecast cloud leans it toward [cloud], all the way
 * to [CloudShare] at a full cover, read between the hours as the picture reads it. An hour
 * the forecast does not reach is drawn with no cloud: the band does not invent a cover it
 * was not told about, and the reading under a finger says nothing of cloud there either.
 */
internal fun nightRibbonStops(
    tonight: Tonight,
    light: (NightSample) -> Color,
    moon: Color,
    cloud: Color
): List<Pair<Float, Color>> {
    val night = tonight.night ?: return emptyList()
    val end = night.end ?: return emptyList()
    val stops = tonight.samples.map { sample ->
        val lit = if (nightReading(tonight, sample.at).moonUp) lerp(light(sample), moon, MoonShare) else light(sample)
        val cover = (betweenHours(tonight.hours, sample.at) { it.cloudPct.toFloat() } ?: 0f) / 100f
        nightFraction(sample.at, night.start, end) to lerp(lit, cloud, CloudShare * cover.coerceIn(0f, 1f))
    }
    // A gradient needs two stops: a night with one sample, or none, is one colour across.
    return when (stops.size) {
        0 -> emptyList()
        1 -> listOf(0f to stops[0].second, 1f to stops[0].second)
        else -> stops
    }
}

/** What the ribbon says about one instant of the night ([TonightHero]'s readout). */
internal data class NightReading(
    val at: Instant,
    val cloudPct: Int?,
    val precipPct: Int?,
    val moonUp: Boolean,
    val inWindow: Boolean
)

/**
 * The night at [at], read off the hero's own facts so the line can never disagree with
 * the band under it: the forecast hour that holds it, and the moon from the same ranges
 * the ribbon washes — the window IS the moonless part of the night, so an instant on its
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
 * The instant a point on the ribbon stands for: its share of the night, settled on the
 * clock's five minutes (a reading of «02:07» is a precision the hourly forecast under it
 * does not have), and never outside dusk and dawn — whose own odd minutes are reachable
 * at the two ends.
 */
internal fun scrubInstant(fraction: Float, start: Instant, end: Instant): Instant {
    val span = end.toEpochMilli() - start.toEpochMilli()
    val raw = start.toEpochMilli() + (span * fraction.coerceIn(0f, 1f).toDouble()).toLong()
    // Within half a step of an end, the end itself: dusk at 20:54 is a time the hero
    // prints, and a grid that could only offer 20:55 would never let a finger reach it.
    if (raw - start.toEpochMilli() < ScrubStepMs / 2) return start
    if (end.toEpochMilli() - raw < ScrubStepMs / 2) return end
    val snapped = Math.floorDiv(raw + ScrubStepMs / 2, ScrubStepMs) * ScrubStepMs
    return Instant.ofEpochMilli(snapped.coerceIn(start.toEpochMilli(), end.toEpochMilli()))
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

/** The stretches of tonight's night the moon is up for, read off the hero's own facts:
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

/**
 * An hour's cloud by layer for the picture, 0..100 each (28 ott 2026): the model's own three
 * where it split the sky, else the whole cover as low cloud. That is exactly what the shader
 * makes of an hour with no layers (`cloudDecks`: the deck is the cover, the veil nothing;
 * `CloudLayers.lightTaken`: the cover), so [betweenHours] can slide from a split hour to an
 * unsplit one and the deck never dips on the way. Layers that explain nothing of the cover
 * (all zero, the model's own «unsplit») read the same.
 */
internal data class LayerShares(val low: Float, val mid: Float, val high: Float)

internal fun layerShares(hour: NightHour): LayerShares {
    val l = hour.layers?.takeIf { (it.lowPct ?: 0) + (it.midPct ?: 0) + (it.highPct ?: 0) > 0 }
        ?: return LayerShares(hour.cloudPct.toFloat(), 0f, 0f)
    return LayerShares((l.lowPct ?: 0).toFloat(), (l.midPct ?: 0).toFloat(), (l.highPct ?: 0).toFloat())
}

private fun verdictColors(colors: com.callbackdev.chiaro.ui.theme.ChiaroColors, kind: VerdictKind): VerdictColors =
    when (kind) {
        VerdictKind.PASS -> colors.pass
        VerdictKind.UNSTABLE -> colors.unstable
        VerdictKind.FAIL -> colors.fail
        VerdictKind.UNKNOWN -> colors.unknown
    }

/** The least sky the canvas keeps under the place row: room for the foot's line and chip
 * and a band of stars over them. */
private val StageMin = 160.dp
/** What the page keeps in sight under the canvas: the section's title and the top of the
 * first of the moments ahead. */
internal val PaperInSight = 144.dp
/** The canvas' height at most ([skyHeroHeight]). */
internal val SkyHeroMax = 640.dp
/** Over the verdict: the foot's controls already carry 8 dp of their own. */
private val WordsTop = 4.dp
/** The foot of the stage: a chip's whole touch target. */
private val FootHeight = 48.dp
/** No name in the last of the stage, where the picture fades into the horizon's colour. */
private val StageLabelFloor = 24.dp
/** The verdict's mark beside its word, as a share of the word's size: the drawing fills
 * ~15/24 of its box, the cap height of the letters next to it. */
private const val VerdictMarkShare = 0.75f
private const val LabelAlpha = 0.85f
/** The chips on the sky: a selected one's white fill, an idle one's scrim. */
private const val ChipFill = 0.18f
private const val ChipScrim = 0.3f
private val RadiantRing = 5.dp
/** The night band the canvas is painted with when it has no place: astronomical night,
 * with the moon's lift. */
private const val NightAltitude = -24.0
private const val MoonLiftAltitude = 30.0
private const val SecondaryAlpha = 0.72f
private const val MoonSetAlpha = 0.35f
/** The app prints a precipitation probability from 20% up (the hour strip's rule). */
private const val PrecipPrintedFrom = 20
private const val ScrubStepMs = 5 * 60_000L
/** The ribbon's band, Today's own. */
private val BandHeight = 8.dp
/** How far the moon's hours lean toward moonlight's straw, and a full cover toward the
 * cloud's grey: enough to tell the three apart on 8 dp over a night sky (DESIGN §8.8b). */
internal const val MoonShare = 0.28f
internal const val CloudShare = 0.38f
/** How far the band's own night leans toward the canvas' white, so that it reads as a band
 * on a night sky of its own colour. */
private const val TrackShare = 0.12f
/** Under the band, the clearest stretch's line and the lane it runs in. */
private val StretchLine = 3.dp
private val StretchLane = 5.dp
private val MoonSize = 44.dp
