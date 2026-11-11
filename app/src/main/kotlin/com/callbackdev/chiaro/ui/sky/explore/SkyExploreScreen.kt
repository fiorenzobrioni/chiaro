package com.callbackdev.chiaro.ui.sky.explore

import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.domain.model.Coordinates
import com.callbackdev.chiaro.domain.sky.BrightStars
import com.callbackdev.chiaro.domain.sky.MoonOfTheNight
import com.callbackdev.chiaro.domain.sky.SkyNotScheduled
import com.callbackdev.chiaro.domain.sky.SkyOccurrence
import com.callbackdev.chiaro.ui.components.BodyRadius
import com.callbackdev.chiaro.ui.components.FittingText
import com.callbackdev.chiaro.ui.components.StarField
import com.callbackdev.chiaro.ui.components.moonLitPath
import com.callbackdev.chiaro.ui.components.starRadiusDp
import com.callbackdev.chiaro.ui.format.Formats
import com.callbackdev.chiaro.ui.format.LocalClock
import com.callbackdev.chiaro.ui.format.currentLocale
import com.callbackdev.chiaro.ui.sky.ChipFill
import com.callbackdev.chiaro.ui.sky.ChipScrim
import com.callbackdev.chiaro.ui.sky.ConstellationNames
import com.callbackdev.chiaro.ui.sky.NightForecast
import com.callbackdev.chiaro.ui.sky.NightRibbon
import com.callbackdev.chiaro.ui.sky.SkyNights
import com.callbackdev.chiaro.ui.sky.SkyText
import com.callbackdev.chiaro.ui.sky.SkyUiState
import com.callbackdev.chiaro.ui.sky.SkyViewModel
import com.callbackdev.chiaro.ui.sky.Tonight
import com.callbackdev.chiaro.ui.sky.betweenHours
import com.callbackdev.chiaro.ui.sky.cloudAt
import com.callbackdev.chiaro.ui.sky.nightDate
import com.callbackdev.chiaro.ui.sky.nightDateForms
import com.callbackdev.chiaro.ui.sky.tonightInstant
import com.callbackdev.chiaro.ui.theme.ChiaroTheme
import com.callbackdev.chiaro.ui.theme.LocalChiaroColors
import com.callbackdev.chiaro.ui.theme.SkyGradient
import com.callbackdev.chiaro.ui.theme.SkyPalette
import com.callbackdev.chiaro.ui.theme.skyInkShadow
import com.callbackdev.chiaro.ui.theme.tabular
import com.callbackdev.chiaro.ui.today.StatusBarIcons
import com.callbackdev.chiaro.widget.window.WindowLabel
import com.callbackdev.chiaro.widget.window.WindowScene
import com.callbackdev.chiaro.widget.window.WindowText
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

/**
 * The explorer (PLANNING, Fase 16, steps 16.6 and 16.7; VISION §5.3): the sky of one hour of
 * one night, the whole screen of it, from the horizon to straight up, turned in every
 * direction by a finger and brought closer by two. Opened by the «Explore» pill on the Sky's
 * hero; it covers the tabs' bar, and back closes it.
 *
 * [atEpochSecond] is the hour the reader was holding on the hero's ribbon, [facingDeg]
 * where the hero was looking; null for either lets the explorer take the hero's own: now
 * while the night is in progress, the night's middle otherwise, facing where the window
 * faces on its own. From there the page has a time of its own (step 16.7): the panel at its
 * foot holds the hour, the night's ribbon and the night, and the reader moves all three.
 */
@Composable
fun SkyExploreRoute(
    atEpochSecond: Long?,
    facingDeg: Float?,
    onBack: () -> Unit,
    skyViewModel: SkyViewModel = viewModel(factory = SkyViewModel.Factory)
) {
    val state by skyViewModel.state.collectAsStateWithLifecycle()
    val content = state as? SkyUiState.Content
    val coordinates = content?.coordinates
    val nights = content?.nights
    val clock = LocalClock.current
    if (content == null || coordinates == null || nights == null) {
        // Nothing to draw a sky from yet (the state's first frame, or a place with no
        // coordinates, which has no pill to come through): the night's ground and the way back.
        ExploreGround {
            ExploreTop(looking = null, onBack = onBack, modifier = Modifier.statusBarsPadding().padding(4.dp))
        }
        return
    }
    SkyExploreScreen(
        coordinates = coordinates,
        zone = content.zone,
        nights = nights,
        tonightDate = nightDate(content.tonight, Instant.now(clock), content.zone),
        openAt = atEpochSecond?.let(Instant::ofEpochSecond),
        facingDeg = facingDeg?.toDouble(),
        followed = content.subscribedIds,
        bareSky = content.bareSky,
        onBareSky = skyViewModel::setBareSky,
        constellations = content.constellations,
        onConstellations = skyViewModel::setConstellations,
        onBack = onBack
    )
}

/**
 * The page itself, from a place and its nights: what [SkyExploreRoute] draws, and what a
 * test draws without a view model.
 *
 * **Its time** (step 16.7). A night, [tonightDate] when it opens, and an hour of it: the one
 * a finger left on the ribbon, or, with none held, the night's own instant, which is now
 * while tonight is under way (and then it follows the clock, a minute at a time: «now» that
 * is twenty minutes old is a sky that has moved five degrees) and the night's middle
 * otherwise. [openAt] is the hour the hero handed over, held from the start.
 *
 * **Its clouds.** The forecast's cover for the hour, read between the hours as the hero
 * reads it, taken off the stars, the planets and the Milky Way by the hero's own rule and
 * laid on the sky's colours and on the moon as one even veil: the honest minimum the step
 * asked for. Where the forecast has no such hour the sky is drawn without, and the line
 * beside the hour says so and why. [bareSky] is the reader's «Behind the clouds», the hero's
 * own preference: the picture without the cover, the words unchanged.
 *
 * [onFrame] is called each time the sky is drawn, with the camera it is drawn through, for the test that holds the page to its
 * rule: **it draws when the camera or the hour changes, and only then**. Nothing in this
 * sky moves by itself, no twinkle and no drift, so a reader who is looking and not touching
 * costs the battery one frame a minute at most.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SkyExploreScreen(
    coordinates: Coordinates,
    zone: ZoneId,
    nights: SkyNights,
    tonightDate: LocalDate,
    openAt: Instant?,
    facingDeg: Double?,
    followed: Set<String>,
    bareSky: Boolean,
    onBareSky: (Boolean) -> Unit,
    constellations: Boolean,
    onConstellations: (Boolean) -> Unit,
    onBack: () -> Unit,
    onFrame: ((SkyCamera) -> Unit)? = null
) {
    val context = LocalContext.current
    val locale = currentLocale()
    val density = LocalDensity.current
    val clock = LocalClock.current

    // Which night, and which hour of it: the two things the panel moves. Kept across a
    // rotation and the process's death, so the reader comes back to the sky they left.
    var dateDay by rememberSaveable { mutableStateOf(tonightDate.toEpochDay()) }
    var heldSecond by rememberSaveable { mutableStateOf(openAt?.epochSecond) }
    var now by remember { mutableStateOf(Instant.now(clock)) }
    var picking by rememberSaveable { mutableStateOf(false) }
    val date = LocalDate.ofEpochDay(dateDay)
    val isTonight = date == tonightDate
    val tonight = remember(nights, date) { nights.of(date, Instant.now(clock)) }
    val outside = if (isTonight) tonight else remember(nights, tonightDate) { nights.of(tonightDate, Instant.now(clock)) }
    val held = heldSecond?.let(Instant::ofEpochSecond)
    // With no hour held on tonight the page follows the clock, and only then.
    val following = held == null && isTonight
    LaunchedEffect(following, clock) {
        while (following) {
            val tick = Instant.now(clock)
            now = tick
            delay(MinuteMs - Math.floorMod(tick.toEpochMilli(), MinuteMs) + 20L)
        }
    }
    val shown = exploreInstant(tonight, held, now, isTonight, date, zone, coordinates)
    val first = tonightDate.minusYears(YearsBack)
    val last = tonightDate.plusYears(YearsAhead)
    fun goTo(target: LocalDate) {
        val to = target.coerceIn(first, last)
        if (to == date) return
        // The hour goes with the reader to the other night, when that night has it.
        heldSecond = held?.let { carriedHour(it, date, to, zone, nights.of(to, Instant.now(clock)).night)?.epochSecond }
        dateDay = to.toEpochDay()
    }
    val back = exploreReturn(outside, Instant.now(clock), isTonight, held != null)

    // The hour's clouds: the number the line prints is the hour's, the cover the picture
    // wears slides between the hours, as on the hero.
    val cloudPct = cloudAt(tonight.hours, shown)
    val coverPct = if (bareSky) {
        0
    } else {
        (betweenHours(tonight.hours, shown) { it.cloudPct.toFloat() } ?: 0f).roundToInt().coerceIn(0, 100)
    }
    val dome = remember(shown, coordinates, zone, followed) { SkyDome.of(shown, coordinates, zone, followed) }
    val look = remember(dome, coverPct) { DomeLook(dome, coverPct / 100f) }
    val table = ChiaroTheme.sky
    val gradient = remember(dome, table, coverPct) {
        table.gradient(
            sunAltitudeDeg = dome.sun.altitudeDeg,
            cloudPct = coverPct,
            moonIllumination = dome.moonIllumination,
            moonAltitudeDeg = dome.moon.altitudeDeg
        )
    }
    // Where the page looks when it opens, chosen once: the hour moves the sky under the
    // camera, never the camera.
    val opened = rememberSaveable {
        facingDeg ?: WindowScene.facing(shown, coordinates, zone, coordinates.lat < 0.0, followed)
    }

    var viewport by remember { mutableStateOf(IntSize.Zero) }
    var chosen by rememberSaveable(stateSaver = CameraSaver) { mutableStateOf<SkyCamera?>(null) }
    // Where what stands on the ground starts, px from the top: the horizon is kept over it.
    // Null until the page has been laid out once, and the sky waits for it; infinite on a
    // wide window, where nothing stands across the foot.
    var floorPx by remember { mutableStateOf<Float?>(null) }
    val lipPx = with(density) { GroundLip.toPx() }
    val w = viewport.width.toFloat()
    val h = viewport.height.toFloat()
    val floor = floorPx
    val camera = if (viewport != IntSize.Zero && floor != null) {
        val horizonAt = horizonShare(floor, lipPx, h)
        (chosen ?: SkyCamera.resting(opened, w, h, horizonAt)).within(w, h, horizonAt)
    } else {
        null
    }
    // From the state and never from the composed value: a drag's events come faster than
    // frames, and each must start from where the last one left the camera.
    fun move(step: (from: SkyCamera, w: Float, h: Float, horizonAt: Float) -> SkyCamera) {
        val size = viewport
        val ground = floorPx ?: return
        if (size == IntSize.Zero) return
        val vw = size.width.toFloat()
        val vh = size.height.toFloat()
        val horizonAt = horizonShare(ground, lipPx, vh)
        val from = (chosen ?: SkyCamera.resting(opened, vw, vh, horizonAt)).within(vw, vh, horizonAt)
        chosen = step(from, vw, vh, horizonAt)
    }

    // The names, measured once for what they say: only their places change with the camera.
    val measurer = rememberTextMeasurer()
    val halo = skyInkShadow()
    val nameStyle = MaterialTheme.typography.labelMedium.copy(color = SkyPalette.Ink.copy(alpha = NameAlpha), shadow = halo)
    val names = remember(dome, locale) { ExploreNames.of(context, dome) }
    val nameLayouts = remember(names.all, nameStyle) {
        names.all.associateWith { measurer.measure(AnnotatedString(it), nameStyle) }
    }
    // A constellation's name is a region's and not a light's: small capitals, spaced and
    // quieter, the way a map writes a sea, so «LYRA» is not read as a star beside «Vega».
    val figureStyle = MaterialTheme.typography.labelSmall.copy(
        color = SkyPalette.Ink.copy(alpha = FigureNameAlpha),
        shadow = halo,
        letterSpacing = FigureNameSpacing
    )
    val figureLayouts = remember(locale, figureStyle) {
        ExploreNames.figures(context, locale).map { measurer.measure(AnnotatedString(it), figureStyle) }
    }
    val compassStyle = MaterialTheme.typography.labelLarge.copy(color = SkyPalette.Ink, shadow = halo)
    // The eight words through `stringResource`, so a change of language reaches them.
    val compassWords = CompassPoints.map { stringResource(SkyText.bearingRes(it)).replaceFirstChar { c -> c.titlecase(locale) } }
    val compass = remember(compassStyle, compassWords) {
        CompassPoints.mapIndexed { i, point -> point to measurer.measure(AnnotatedString(compassWords[i]), compassStyle) }
    }

    val looking = camera?.let { lookingLine(context, it) }
    val is24 = android.text.format.DateFormat.is24HourFormat(context)
    val timeFmt = remember(is24, locale) { Formats.timeFormatter(is24, locale) }
    val clockText = shown.atZone(zone).format(timeFmt)
    val cloudLine = when {
        cloudPct != null -> stringResource(R.string.sky_evidence_cloud, cloudPct).replaceFirstChar { it.titlecase(locale) }
        tonight.forecast == NightForecast.BEYOND -> stringResource(R.string.explore_no_clouds_beyond)
        tonight.forecast == NightForecast.PAST -> stringResource(R.string.explore_no_clouds_past)
        tonight.forecast == NightForecast.NONE -> stringResource(R.string.explore_no_clouds_none)
        else -> stringResource(R.string.explore_no_clouds)
    }
    val nightForms = if (isTonight) {
        nightDateForms(date, locale).map { stringResource(R.string.sky_hero_night, it) }
    } else {
        otherNightForms(date, tonightDate.year, locale).map { stringResource(R.string.explore_night_of, it) }
    }
    val turnLeft = stringResource(R.string.explore_turn_left)
    val turnRight = stringResource(R.string.explore_turn_right)
    val lookUp = stringResource(R.string.explore_look_up)
    val lookDown = stringResource(R.string.explore_look_down)
    val painter = remember { ExplorePainter() }

    ExploreGround {
        val wide = maxWidth >= WideFrom
        // What stands over the sky, in its own px: no name is written under it.
        var skyCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
        val boxes = remember(wide) { mutableStateMapOf<String, RectF>() }
        fun boxOf(c: LayoutCoordinates): RectF? = skyCoords?.takeIf { it.isAttached && c.isAttached }?.let { sky ->
            val r = sky.localBoundingBoxOf(c, clipBounds = false)
            RectF(r.left, r.top, r.right, r.bottom)
        }
        fun stands(key: String): (LayoutCoordinates) -> Unit = { c ->
            boxOf(c)?.let { box -> if (boxes[key] != box) boxes[key] = box }
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag(ExploreSkyTag)
                .onSizeChanged { viewport = it }
                .onGloballyPositioned { skyCoords = it }
                .semantics {
                    contentDescription = looking ?: ""
                    customActions = listOf(
                        CustomAccessibilityAction(turnLeft) { move { c, vw, vh, at -> c.turned(-TurnStepDeg, 0.0, vw, vh, at) }; true },
                        CustomAccessibilityAction(turnRight) { move { c, vw, vh, at -> c.turned(TurnStepDeg, 0.0, vw, vh, at) }; true },
                        CustomAccessibilityAction(lookUp) { move { c, vw, vh, at -> c.turned(0.0, LookStepDeg, vw, vh, at) }; true },
                        CustomAccessibilityAction(lookDown) { move { c, vw, vh, at -> c.turned(0.0, -LookStepDeg, vw, vh, at) }; true }
                    )
                }
                .pointerInput(Unit) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        move { from, vw, vh, at ->
                            from
                                .dragged(centroid.x - pan.x, centroid.y - pan.y, centroid.x, centroid.y, vw, vh, at)
                                .zoomed(zoom, centroid.x, centroid.y, vw, vh, at)
                        }
                    }
                }
        ) {
            val cam = camera ?: return@Canvas
            onFrame?.invoke(cam)
            with(painter) {
                draw(
                    dome = dome,
                    look = look,
                    camera = cam,
                    gradient = gradient,
                    names = names,
                    layouts = nameLayouts,
                    figures = constellations,
                    figureLayouts = figureLayouts,
                    compass = compass,
                    keepOut = boxes.values.toList(),
                    density = density.density
                )
            }
        }

        // The top: the way back and where the camera looks, then what is drawn. The two
        // switches stand up here and the time down at the foot: what the page shows is set
        // once and remembered, when it shows is under the thumb.
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(4.dp)
        ) {
            ExploreTop(looking = looking, onBack = onBack, modifier = Modifier.onGloballyPositioned(stands("top")))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 8.dp).onGloballyPositioned(stands("switches"))
            ) {
                SkySwitch(
                    label = stringResource(R.string.explore_constellations),
                    selected = constellations,
                    onClick = { onConstellations(!constellations) }
                )
                // «Behind the clouds» where there are clouds to look behind: a switch that
                // changes nothing, on a night the forecast does not reach, would be noise.
                if (cloudPct != null) {
                    SkySwitch(
                        label = stringResource(R.string.window_bare_note),
                        selected = bareSky,
                        onClick = { onBareSky(!bareSky) }
                    )
                }
            }
        }

        val zoom: @Composable () -> Unit = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                val canOut = camera != null && camera.fieldDeg < SkyCamera.MaxFieldDeg
                val canIn = camera != null && camera.fieldDeg > SkyCamera.MinFieldDeg
                ZoomButton(plus = false, enabled = canOut, label = stringResource(R.string.explore_zoom_out)) {
                    move { c, vw, vh, at -> c.zoomed(1f / ZoomStep, vw / 2, vh / 2, vw, vh, at) }
                }
                ZoomButton(plus = true, enabled = canIn, label = stringResource(R.string.explore_zoom_in)) {
                    move { c, vw, vh, at -> c.zoomed(ZoomStep, vw / 2, vh / 2, vw, vh, at) }
                }
            }
        }
        val wayBack: @Composable () -> Unit = {
            if (back != null) {
                ReturnChip(
                    label = stringResource(if (back == ExploreReturn.NOW) R.string.explore_now else R.string.explore_tonight),
                    onClick = {
                        dateDay = tonightDate.toEpochDay()
                        heldSecond = null
                        now = Instant.now(clock)
                    }
                )
            }
        }
        val panel: @Composable (Modifier, Boolean) -> Unit = { place, docked ->
            ExplorePanel(
                tonight = tonight,
                zone = zone,
                timeFmt = timeFmt,
                shown = shown,
                held = held,
                onHold = { heldSecond = it?.epochSecond },
                clockText = clockText,
                cloudLine = cloudLine,
                nightForms = nightForms,
                onPrevious = if (date > first) ({ goTo(date.minusDays(1)) }) else null,
                onNext = if (date < last) ({ goTo(date.plusDays(1)) }) else null,
                onPick = { picking = true },
                docked = docked,
                modifier = place
            )
        }
        if (wide) {
            // A wide window: the panel is a card in the corner and the sky keeps its floor.
            SideEffect { floorPx = Float.POSITIVE_INFINITY }
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(12.dp)
            ) {
                Column(modifier = Modifier.width(PanelWidth).onGloballyPositioned(stands("panel"))) {
                    wayBack()
                    panel(Modifier.fillMaxWidth(), false)
                }
                Spacer(modifier = Modifier.weight(1f))
                Box(modifier = Modifier.onGloballyPositioned(stands("zoom"))) { zoom() }
            }
        } else {
            // A phone held upright: the panel across the foot, the two buttons and the way
            // back standing on the ground over it, and the horizon over them all.
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .onGloballyPositioned { c ->
                        stands("foot")(c)
                        boxOf(c)?.let { if (floorPx != it.top) floorPx = it.top }
                    }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .padding(start = 12.dp, end = 12.dp, bottom = 8.dp)
                ) {
                    wayBack()
                    Spacer(modifier = Modifier.weight(1f))
                    zoom()
                }
                panel(Modifier.fillMaxWidth(), true)
            }
        }
    }
    if (picking) {
        NightPicker(
            date = date,
            first = first,
            last = last,
            onPick = {
                picking = false
                goTo(it)
            },
            onDismiss = { picking = false }
        )
    }
}

/**
 * The instant the explorer's sky is of: the hour [held] on the ribbon; with none, now while
 * the night is under way and its middle otherwise ([tonightInstant], the hero's own rule);
 * and on a date with no night to take a middle of (a white night, the polar night), now for
 * tonight and for any other the midnight that closes it, the instant its moon is read at.
 */
internal fun exploreInstant(
    tonight: Tonight,
    held: Instant?,
    now: Instant,
    isTonight: Boolean,
    date: LocalDate,
    zone: ZoneId,
    coordinates: Coordinates
): Instant = held
    ?: tonight.night?.let { tonightInstant(now, it) }
    ?: if (isTonight) now else MoonOfTheNight.instant(date, zone, coordinates)

/**
 * The hour a reader was holding, carried to another night: the same time on the clock, [to]
 * instead of [from], when that night ([night], its dusk to its dawn) has it; null when it
 * does not, and the other night then opens on its own instant. Stepping a night at a time
 * with 22:00 held is how a reader watches the moon move from one evening to the next.
 */
internal fun carriedHour(held: Instant, from: LocalDate, to: LocalDate, zone: ZoneId, night: SkyOccurrence.At?): Instant? {
    val end = night?.end ?: return null
    val carried = held.atZone(zone).plusDays(to.toEpochDay() - from.toEpochDay()).toInstant()
    return carried.takeIf { !it.isBefore(night.start) && !it.isAfter(end) }
}

/** The way back the page offers, to where a reader standing outside would look. */
internal enum class ExploreReturn { NOW, TONIGHT }

/**
 * Which way back there is ([ExploreReturn]), null for none. «Now» while tonight is under way
 * (or has no dark hours at all, and now is all there is), from another night or another
 * hour; before dusk, «Tonight» from another night. Before dusk with an hour of tonight held
 * there is nothing to go back to: the ribbon under the finger is the whole of it.
 */
internal fun exploreReturn(outside: Tonight, now: Instant, isTonight: Boolean, hourHeld: Boolean): ExploreReturn? {
    val night = outside.night
    val underWay = if (night == null) true else night.end?.let { !now.isBefore(night.start) && now.isBefore(it) } ?: false
    return when {
        underWay && (!isTonight || hourHeld) -> ExploreReturn.NOW
        !underWay && !isTonight -> ExploreReturn.TONIGHT
        else -> null
    }
}

/**
 * The horizon's place for a foot that starts [floorPx] from the top of a viewport [heightPx]
 * tall: [lipPx] over it, as a share of the height, never lower than the camera's own floor
 * and never as high as the middle of the screen (`SkyCamera.HighestHorizon`).
 */
internal fun horizonShare(floorPx: Float, lipPx: Float, heightPx: Float): Float {
    if (heightPx <= 0f || floorPx.isInfinite()) return SkyCamera.HorizonLine
    return ((floorPx - lipPx) / heightPx).coerceIn(SkyCamera.HighestHorizon, SkyCamera.HorizonLine)
}

/** Another night's date, longest form first, with the year when it is not this one's. */
internal fun otherNightForms(date: LocalDate, thisYear: Int, locale: Locale): List<String> {
    val year = if (date.year == thisYear) "" else " yyyy"
    return listOf("EEEE d MMMM$year", "EEE d MMM$year", "d MMM$year").map { date.format(DateTimeFormatter.ofPattern(it, locale)) }
}

/**
 * The night's ground under everything, in the night's colours in both themes, as the hero
 * is (PLANNING, Fase 16, «Decisioni già prese»): white on the canvas, the status bar's
 * icons light.
 */
@Composable
private fun ExploreGround(content: @Composable BoxWithConstraintsScope.() -> Unit) {
    StatusBarIcons(overCanvas = true)
    CompositionLocalProvider(
        LocalChiaroColors provides ChiaroTheme.nightColors,
        LocalContentColor provides SkyPalette.Ink
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize().background(SkyPalette.ScrimColor), content = content)
    }
}

/** The cross and the line that says where the camera looks. */
@Composable
private fun ExploreTop(looking: String?, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.fillMaxWidth()) {
        IconButton(
            onClick = onBack,
            colors = IconButtonDefaults.iconButtonColors(contentColor = SkyPalette.Ink)
        ) {
            Icon(imageVector = Icons.Outlined.Close, contentDescription = stringResource(R.string.explore_close))
        }
        if (looking != null) {
            // Silent: the sky says the same words as its own description.
            Text(
                text = looking,
                style = MaterialTheme.typography.titleMedium.copy(shadow = skyInkShadow()),
                color = SkyPalette.Ink,
                modifier = Modifier.padding(start = 4.dp).clearAndSetSemantics { }
            )
        }
    }
}

/**
 * One of the page's switches, the hero's «Behind the clouds» in form: a chip on the canvas'
 * scrim that fills and takes a check when it is on, a visible, labelled switch and not a
 * hidden tap.
 */
@Composable
private fun SkySwitch(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        leadingIcon = if (selected) {
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
        border = null
    )
}

/** «Now», or «Tonight»: the way back, on the ground beside the zoom. */
@Composable
private fun ReturnChip(label: String, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelLarge) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = SkyPalette.Ink.copy(alpha = ChipFill),
            labelColor = SkyPalette.Ink
        ),
        border = null
    )
}

/**
 * The panel at the page's foot (step 16.7, board 3): **when** this sky is. Top to bottom:
 * the hour, large, with the hour's cloud beside it, or the reason there is none to draw;
 * the night's ribbon, the hero's own ([NightRibbon]: the same band, the same finger, the
 * same reading to a screen reader), with the disc on the hour shown; and the night, between
 * the two arrows that step to the one before and the one after, itself the button that
 * opens the calendar.
 *
 * A white night's ribbon runs from sunset to sunrise (`SkyStateBuilder.exploredNight`), and
 * the panel says under it that the sky never gets fully dark. A date with no sunset in it,
 * and the polar night, have no ribbon, and the panel says which sky that is in its place.
 */
@Composable
private fun ExplorePanel(
    tonight: Tonight,
    zone: ZoneId,
    timeFmt: DateTimeFormatter,
    shown: Instant,
    held: Instant?,
    onHold: (Instant?) -> Unit,
    clockText: String,
    cloudLine: String,
    nightForms: List<String>,
    onPrevious: (() -> Unit)?,
    onNext: (() -> Unit)?,
    onPick: () -> Unit,
    docked: Boolean,
    modifier: Modifier = Modifier
) {
    val shape = if (docked) RoundedCornerShape(topStart = PanelCorner, topEnd = PanelCorner) else RoundedCornerShape(PanelCorner)
    Column(
        modifier = modifier
            .clip(shape)
            .background(SkyPalette.ScrimColor.copy(alpha = PanelScrim))
            .then(if (docked) Modifier.navigationBarsPadding() else Modifier)
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().testTag(ExploreHourTag)) {
            Text(
                text = clockText,
                style = MaterialTheme.typography.headlineMedium.tabular(),
                color = SkyPalette.Ink,
                modifier = Modifier.alignByBaseline()
            )
            Text(
                text = cloudLine,
                style = MaterialTheme.typography.bodyMedium.tabular(),
                color = SkyPalette.Ink,
                modifier = Modifier.padding(start = 12.dp).weight(1f).alignByBaseline()
            )
        }
        val span = tonight.night
        if (span?.end != null) {
            NightRibbon(
                tonight = tonight,
                night = span,
                zone = zone,
                timeFmt = timeFmt,
                clear = ChiaroTheme.nightColors.pass.ink,
                shown = shown,
                scrub = held,
                onScrub = onHold,
                stateDescription = "$clockText · $cloudLine"
            )
        }
        // A date with no dark hours says which sky it is, with or without a ribbon: on a
        // white night the ribbon runs from sunset to sunrise (`SkyStateBuilder.exploredNight`),
        // and a band of twilight with no word beside it would read as a night.
        if (tonight.reason == SkyNotScheduled.NO_DARKNESS || tonight.reason == SkyNotScheduled.DARK_ALL_DAY) {
            Text(
                text = stringResource(
                    if (tonight.reason == SkyNotScheduled.DARK_ALL_DAY) {
                        R.string.sky_tonight_headline_dark_all_day
                    } else {
                        R.string.sky_tonight_headline_no_darkness
                    }
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = SkyPalette.Ink,
                modifier = Modifier.padding(top = if (span?.end != null) 4.dp else 8.dp)
            )
        }
        val pick = stringResource(R.string.explore_pick_night)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            NightStep(forward = false, label = stringResource(R.string.explore_night_previous), onClick = onPrevious)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClickLabel = pick, role = Role.Button, onClick = onPick)
                    .testTag(ExploreNightTag)
                    .semantics { contentDescription = nightForms.first() }
            ) {
                FittingText(
                    candidates = nightForms,
                    style = MaterialTheme.typography.titleSmall.tabular(),
                    color = SkyPalette.Ink,
                    modifier = Modifier.clearAndSetSemantics { }
                )
            }
            NightStep(forward = true, label = stringResource(R.string.explore_night_next), onClick = onNext)
        }
    }
}

/** One of the two arrows beside the night; null [onClick] at the calendar's end, where it fades. */
@Composable
private fun NightStep(forward: Boolean, label: String, onClick: (() -> Unit)?) {
    IconButton(
        onClick = { onClick?.invoke() },
        enabled = onClick != null,
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = SkyPalette.Ink,
            disabledContentColor = SkyPalette.Ink.copy(alpha = DisabledInk)
        )
    ) {
        Icon(
            imageVector = if (forward) Icons.AutoMirrored.Outlined.KeyboardArrowRight else Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
            contentDescription = label
        )
    }
}

/**
 * The calendar a night is picked from: the system's own date picker, in the app's theme
 * and not the night's (it is a dialog over the page, not a part of the sky). A day of it is
 * the evening a night begins on. From a year back to five ahead: the planets' places are a
 * short series that holds its accuracy over these years and is not tested beyond them.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NightPicker(
    date: LocalDate,
    first: LocalDate,
    last: LocalDate,
    onPick: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = date.toEpochDay() * DayMs,
        yearRange = first.year..last.year,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                Math.floorDiv(utcTimeMillis, DayMs) in first.toEpochDay()..last.toEpochDay()

            override fun isSelectableYear(year: Int): Boolean = year in first.year..last.year
        }
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { state.selectedDateMillis?.let { onPick(LocalDate.ofEpochDay(Math.floorDiv(it, DayMs))) } },
                enabled = state.selectedDateMillis != null
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    ) {
        DatePicker(
            state = state,
            title = {
                Text(
                    text = stringResource(R.string.explore_pick_night),
                    modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp)
                )
            }
        )
    }
}

/**
 * One of the two zoom buttons: the pinch's other form, for one hand and for a reader who
 * cannot pinch. A plus or a minus drawn as strokes, on the canvas' scrim; at the end of
 * the zoom it is disabled and says so by fading.
 */
@Composable
private fun ZoomButton(plus: Boolean, enabled: Boolean, label: String, onClick: () -> Unit) {
    val ink = SkyPalette.Ink
    FilledIconButton(
        onClick = onClick,
        enabled = enabled,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = SkyPalette.ScrimColor.copy(alpha = ButtonScrim),
            contentColor = ink,
            disabledContainerColor = SkyPalette.ScrimColor.copy(alpha = ButtonScrim / 2f),
            disabledContentColor = ink.copy(alpha = DisabledInk)
        ),
        modifier = Modifier.size(48.dp).semantics { contentDescription = label }
    ) {
        val stroke = LocalContentColor.current
        Canvas(modifier = Modifier.size(18.dp)) {
            val width = 2.dp.toPx()
            drawLine(stroke, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), width, StrokeCap.Round)
            if (plus) drawLine(stroke, Offset(size.width / 2, 0f), Offset(size.width / 2, size.height), width, StrokeCap.Round)
        }
    }
}

/** «Looking south-east», or, from eighty degrees up, that it looks straight up: there the
 * bearing is the side of the screen's foot and not where anybody is looking. */
internal fun lookingLine(context: android.content.Context, camera: SkyCamera): String =
    if (camera.altitudeDeg >= OverheadFromDeg) {
        context.getString(R.string.explore_overhead)
    } else {
        context.getString(R.string.explore_looking, context.getString(SkyText.bearingRes(camera.azimuthDeg)))
    }

/**
 * The names the explorer can write, as texts: the moon with its phase of the instant, a
 * peak night's radiant, the four planets, the thirty-one stars. Which of them are written
 * in a frame is the painter's ([ExplorePainter]); this is what there is to measure.
 */
internal class ExploreNames(
    val moon: String,
    val radiant: String?,
    /** One a planet, in the dome's order. */
    val planets: List<String>,
    /** One a named star, in the dome's order. */
    val stars: List<String>
) {
    /** Every text there is to measure, once each. */
    val all: List<String> = (listOf(moon) + listOfNotNull(radiant) + planets + stars).distinct()

    companion object {
        /**
         * The faintest star a field names, as a magnitude: the first at the widest field,
         * where a dozen names would be a list and not a sky, every one of the thirty-one
         * from forty-five degrees in.
         */
        fun faintestNamed(fieldDeg: Double): Double {
            val t = ((SkyCamera.MaxFieldDeg - fieldDeg) / (SkyCamera.MaxFieldDeg - AllNamesFieldDeg)).coerceIn(0.0, 1.0)
            return WideNameLimit + (AllNameLimit - WideNameLimit) * t
        }

        /** Whether a star of this magnitude and declination is named at [fieldDeg]. The Pole
         * Star is named at any field: it is the one a reader looks for. */
        fun named(mag: Double, decDeg: Double, fieldDeg: Double): Boolean =
            decDeg >= PoleStarDec || mag <= faintestNamed(fieldDeg)

        /**
         * Whether a constellation of this [rank] is named at [fieldDeg] (step 16.7). The
         * source ranks them one to three (16.5): twenty-two a reader looks for first,
         * twenty-four more, and the forty-two faint ones. A field wider than the one the
         * page opens at writes the first only, where a whole sky of eighty-eight names
         * would be a list; the resting field adds the second; closer than forty-five
         * degrees, where a screen holds three or four figures, all of them.
         */
        fun figureNamed(rank: Int, fieldDeg: Double): Boolean = rank <= when {
            fieldDeg > SkyCamera.RestingFieldDeg -> 1
            fieldDeg > AllNamesFieldDeg -> 2
            else -> 3
        }

        /** The constellations' names as the sky writes them, in [ConstellationUnits.names]' order. */
        fun figures(context: android.content.Context, locale: Locale): List<String> =
            ConstellationUnits.names.map { name ->
                ConstellationNames.nameRes(name.id)?.let { context.getString(it).uppercase(locale) }.orEmpty()
            }

        private const val WideNameLimit = 1.0
        private const val AllNameLimit = 2.1
        private const val AllNamesFieldDeg = 45.0
        private const val PoleStarDec = 89.0

        fun of(context: android.content.Context, dome: SkyDome) = ExploreNames(
            moon = WindowText.moonLabel(context, (dome.moonIllumination * 100).roundToInt()),
            radiant = dome.radiant?.let { WindowText.radiantLabel(context, it.jobId) },
            planets = dome.planets.map { WindowText.planetLabel(context, it.planet) },
            stars = dome.named.map { context.getString(it.star.nameRes) }
        )
    }
}

/**
 * How the dome's lights are drawn, worked out once for the instant: each star's colour
 * with how much of it shows (its brightness, the night's own amount, the air near the
 * horizon) and its radius. The camera then only places them.
 */
internal class DomeLook(val dome: SkyDome, val cover: Float = 0f) {
    /** How much of the stars, the planets and the Milky Way shows under the cover: the hero's rule. */
    val stars: Float = dome.starsUnder(cover)
    val planets: Float = dome.planetsUnder(cover)
    val milkyWay: Float = dome.milkyWayUnder(cover)

    /**
     * How much of the constellations' lines and names shows, 0..1: nothing where the stars
     * they join are not there to join (by day, under a closed sky), and from half up with
     * them. The lines are the app's drawing and not a light, but a figure traced over a
     * sky with no stars in it would be a chart, and this page is the sky.
     */
    val figures: Float = if (stars <= NamedFrom) 0f else FigureFloor + (1f - FigureFloor) * stars

    /** What an even veil of this cover leaves of a disc behind it, the moon's or the sun's. */
    val discShown: Float = 1f - DiscVeil * cover
    val glowShown: Float = (1f - cover) * (1f - cover)

    val starColor = IntArray(BrightStars.count)
    val starRadiusDp = FloatArray(BrightStars.count)
    val starBrightness = FloatArray(BrightStars.count)
    val milkyWayColor = IntArray(MilkyWay.vertexCount)

    init {
        val directions = dome.starDirections
        for (i in 0 until BrightStars.count) {
            val brightness = StarField.brightness(BrightStars.mag[i])
            starBrightness[i] = brightness
            starRadiusDp[i] = starRadiusDp(brightness)
            starColor[i] = lightArgb(StarField.warmth(BrightStars.bv[i]), brightness * stars * lowAir(directions[3 * i + 2]))
        }
        val glow = SkyPalette.MilkyWayGlow
        val band = dome.milkyWayDirections
        for (i in 0 until MilkyWay.vertexCount) {
            val up = band[3 * i + 2]
            val alpha = if (up <= 0f) 0f else MilkyWayStrength * MilkyWay.strength[i] * milkyWay * lowAir(up)
            milkyWayColor[i] = glow.copy(alpha = alpha.coerceIn(0f, 1f)).toArgb()
        }
    }

    companion object {
        /** A light's colour by its warmth, blue-white to orange, at [alpha]. */
        fun lightArgb(warmth: Float, alpha: Float): Int =
            lerp(SkyPalette.StarCool, SkyPalette.StarWarm, warmth).copy(alpha = alpha.coerceIn(0f, 1f)).toArgb()

        /**
         * What the air takes of a light low in the sky, by the sine of its height ([up]):
         * all of it there from 35 degrees up, half at ten, a third on the horizon. The
         * hero's shader thins its stars the same way down its canvas.
         */
        fun lowAir(up: Float): Float {
            val t = ((HighAirSin - up) / HighAirSin).coerceIn(0f, 1f)
            return 1f - 0.67f * t * t * (3f - 2f * t)
        }

        private const val HighAirSin = 0.574f // sin 35°

        /** A name is written, and a figure traced, from this much of the stars' light: under
         * it the dot is not there to name. */
        const val NamedFrom = 0.2f
        private const val FigureFloor = 0.5f

        /** A full cover leaves a fifth of the moon's disc: the pale patch where it stands. */
        private const val DiscVeil = 0.8f
    }
}

/**
 * The explorer's drawing, on a plain canvas and **not through the weather's shader** (step
 * 16.6): that shader's sky is three fixed frames, and this one turns. Bottom to top: the
 * sky's own colours, lighter toward the horizon as the hero's are; the Milky Way; the
 * constellations' lines, where the reader wants them (step 16.7), under the stars they
 * join; the stars the zoom holds; the planets; the moon with its phase turned toward the sun; the
 * sun on a night that has one; then the ground, which hides whatever is under the horizon
 * exactly, the horizon's line with the eight directions written on it, and the names.
 *
 * It keeps its arrays between frames: a drag draws sixty times a second and should not
 * make sixty heaps of garbage.
 */
internal class ExplorePainter {
    private val spot = FloatArray(2)
    private val towards = FloatArray(2)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val meshPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val meshPlaces = FloatArray(MilkyWay.vertexCount * 2)
    private val meshSeen = BooleanArray(MilkyWay.vertexCount)
    private val meshIndices = ShortArray(MilkyWay.LONGITUDES * (MilkyWay.LATITUDES - 1) * 6)
    private val ground = Path()
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private var linePoints = FloatArray(0)
    private val first = ArrayList<WindowLabel>(8)
    private val labels = ArrayList<WindowLabel>(48)
    private val taken = ArrayList<RectF>(12)

    fun DrawScope.draw(
        dome: SkyDome,
        look: DomeLook,
        camera: SkyCamera,
        gradient: SkyGradient,
        names: ExploreNames,
        layouts: Map<String, TextLayoutResult>,
        /** «Constellations»: the lines and the names, the reader's choice. */
        figures: Boolean,
        /** The constellations' names, measured, in [ConstellationUnits.names]' order. */
        figureLayouts: List<TextLayoutResult>,
        compass: List<Pair<Double, TextLayoutResult>>,
        keepOut: List<RectF>,
        density: Float
    ) {
        val lens = camera.lens(size.width, size.height)
        val scale = SkyCamera.markScale(camera.fieldDeg)
        val horizon = lens.horizon

        sky(lens, camera, gradient, horizon)
        val traced = figures && look.figures > 0f
        if (look.milkyWay > 0.003f) milkyWay(dome, look, lens)
        if (traced) figureLines(dome, look, lens, scale)
        stars(dome, look, lens, camera, scale, density)
        planets(dome, look, lens, scale, density)
        if (dome.sunUp) sun(dome, look, lens, scale)
        if (dome.moonUp) moon(dome, look, lens, scale)
        ground(gradient, horizon)
        compass(lens, horizon, compass, keepOut)
        names(dome, look, lens, camera.fieldDeg, scale, names, layouts, if (traced) figureLayouts else null, density)
    }

    /**
     * The constellations' lines (step 16.7): each segment between the two stars it joins,
     * stopped short of both, so the line leads to a star and does not run through it. One
     * call draws them all. A segment is drawn straight between its ends: on this projection
     * the true line is an arc of a circle, and over the twenty-five degrees of the longest
     * of them (16.5) the two differ by less than the star's own dot.
     */
    private fun DrawScope.figureLines(dome: SkyDome, look: DomeLook, lens: SkyLens, scale: Float) {
        val lines = dome.constellationLines
        val segments = lines.size / 6
        if (linePoints.size < segments * 4) linePoints = FloatArray(segments * 4)
        val margin = size.maxDimension * 2f
        val gap = FigureGap.toPx() * scale
        var n = 0
        for (i in 0 until segments) {
            val a = 6 * i
            val upA = lines[a + 2]
            val upB = lines[a + 5]
            // Both ends under the horizon: the ground has it.
            if (upA < 0f && upB < 0f) continue
            if (!lens.toScreen(lines[a].toDouble(), lines[a + 1].toDouble(), upA.toDouble(), spot)) continue
            if (!lens.toScreen(lines[a + 3].toDouble(), lines[a + 4].toDouble(), upB.toDouble(), towards)) continue
            val x1 = spot[0]
            val y1 = spot[1]
            val x2 = towards[0]
            val y2 = towards[1]
            // An end far off the screen is a line across it that is not there.
            if (!lens.onScreen(x1, y1, margin) || !lens.onScreen(x2, y2, margin)) continue
            // Both ends off the same side: no part of it is on the screen.
            if ((x1 < 0f && x2 < 0f) || (x1 > size.width && x2 > size.width) ||
                (y1 < 0f && y2 < 0f) || (y1 > size.height && y2 > size.height)
            ) {
                continue
            }
            val length = hypot(x2 - x1, y2 - y1)
            // Two stars closer than the air round them: the dots say it without a line.
            if (length <= gap * 2.5f) continue
            val ux = (x2 - x1) / length * gap
            val uy = (y2 - y1) / length * gap
            linePoints[n++] = x1 + ux
            linePoints[n++] = y1 + uy
            linePoints[n++] = x2 - ux
            linePoints[n++] = y2 - uy
        }
        if (n == 0) return
        linePaint.color = SkyPalette.Ink.copy(alpha = FigureLineAlpha * look.figures).toArgb()
        linePaint.strokeWidth = FigureLine.toPx()
        drawContext.canvas.nativeCanvas.drawLines(linePoints, 0, n, linePaint)
    }

    /** The sky's three colours from the zenith out to the horizon, as rings round the zenith. */
    private fun DrawScope.sky(lens: SkyLens, camera: SkyCamera, gradient: SkyGradient, horizon: HorizonCircle) {
        lens.toScreen(0.0, 0.0, 1.0, spot)
        val zenith = Offset(spot[0], spot[1])
        lens.toScreen(camera.azimuthDeg, 0.0, towards)
        val reach = hypot(towards[0] - zenith.x, towards[1] - zenith.y).coerceAtLeast(1f)
        drawRect(
            brush = Brush.radialGradient(
                0f to gradient.top, 0.55f to gradient.mid, 1f to gradient.bottom,
                center = zenith, radius = reach
            )
        )
    }

    /** The ground: everything outside the horizon's circle, in the night's own dark. */
    private fun DrawScope.ground(gradient: SkyGradient, horizon: HorizonCircle) {
        val centre = Offset(horizon.centreX, horizon.centreY)
        // The fill rule again each time: a path emptied goes back to the winding rule, under
        // which the square and the circle are one filled square, the sky and all.
        ground.reset()
        ground.fillType = PathFillType.EvenOdd
        ground.addRect(Rect(-1f, -1f, size.width + 1f, size.height + 1f))
        ground.addOval(Rect(centre, horizon.radius))
        drawPath(ground, lerp(gradient.bottom, SkyPalette.ScrimColor, GroundDepth))
        drawCircle(
            color = SkyPalette.Ink.copy(alpha = HorizonAlpha),
            radius = horizon.radius,
            center = centre,
            style = Stroke(HorizonLine.toPx())
        )
    }

    private fun DrawScope.milkyWay(dome: SkyDome, look: DomeLook, lens: SkyLens) {
        val directions = dome.milkyWayDirections
        val margin = size.maxDimension
        for (i in 0 until MilkyWay.vertexCount) {
            val seen = lens.toScreen(directions[3 * i].toDouble(), directions[3 * i + 1].toDouble(), directions[3 * i + 2].toDouble(), spot)
            // A vertex far off the screen stretches its triangles across it: left out.
            meshSeen[i] = seen && lens.onScreen(spot[0], spot[1], margin)
            meshPlaces[2 * i] = spot[0]
            meshPlaces[2 * i + 1] = spot[1]
        }
        var n = 0
        val rows = MilkyWay.LATITUDES
        for (column in 0 until MilkyWay.LONGITUDES) {
            for (row in 0 until rows - 1) {
                val a = column * rows + row
                val b = a + 1
                val c = a + rows
                val d = c + 1
                if (!(meshSeen[a] && meshSeen[b] && meshSeen[c] && meshSeen[d])) continue
                meshIndices[n++] = a.toShort(); meshIndices[n++] = b.toShort(); meshIndices[n++] = c.toShort()
                meshIndices[n++] = b.toShort(); meshIndices[n++] = d.toShort(); meshIndices[n++] = c.toShort()
            }
        }
        if (n == 0) return
        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawVertices(
                android.graphics.Canvas.VertexMode.TRIANGLES,
                meshPlaces.size, meshPlaces, 0, null, 0, look.milkyWayColor, 0, meshIndices, 0, n, meshPaint
            )
        }
    }

    private fun DrawScope.stars(dome: SkyDome, look: DomeLook, lens: SkyLens, camera: SkyCamera, scale: Float, density: Float) {
        if (look.stars <= 0.003f) return
        val faintest = SkyCamera.faintestMagnitude(camera.fieldDeg)
        val directions = dome.starDirections
        val mag = BrightStars.mag
        val canvas = drawContext.canvas.nativeCanvas
        for (i in 0 until BrightStars.count) {
            // The catalogue is sorted brightest first: past the faintest, all the rest are fainter.
            if (mag[i] > faintest) break
            val up = directions[3 * i + 2]
            if (up < 0f) continue
            if (!lens.toScreen(directions[3 * i].toDouble(), directions[3 * i + 1].toDouble(), up.toDouble(), spot)) continue
            if (!lens.onScreen(spot[0], spot[1], 6f * density)) continue
            val radius = look.starRadiusDp[i] * scale * density
            val color = look.starColor[i]
            if (look.starBrightness[i] > GlowFrom) glow(canvas, spot[0], spot[1], radius, color)
            paint.color = color
            canvas.drawCircle(spot[0], spot[1], radius, paint)
        }
    }

    /** The planets as the hero draws them: by their brightness that night, warm, larger than a star. */
    private fun DrawScope.planets(dome: SkyDome, look: DomeLook, lens: SkyLens, scale: Float, density: Float) {
        if (look.planets <= 0.003f) return
        val canvas = drawContext.canvas.nativeCanvas
        for (p in dome.planets) {
            if (p.place.up <= 0.0) continue
            if (!lens.toScreen(p.place.east, p.place.north, p.place.up, spot)) continue
            if (!lens.onScreen(spot[0], spot[1], 12f * density)) continue
            val brightness = StarField.planetBrightness(p.magnitude)
            val alpha = brightness * look.planets * DomeLook.lowAir(p.place.up.toFloat())
            val color = DomeLook.lightArgb(StarField.planetWarmth(p.planet), alpha)
            val radius = starRadiusDp(brightness) * StarField.planetScale(p.planet) * scale * density
            glow(canvas, spot[0], spot[1], radius, color)
            paint.color = color
            canvas.drawCircle(spot[0], spot[1], radius, paint)
        }
    }

    /**
     * The light round a bright point, as three faint discs one in the other: a hard disc
     * of three dp reads as a button, and the hero's stars are soft because a shader draws
     * them. Three steps, each under a tenth of the light apart, are a slope to the eye.
     */
    private fun glow(canvas: android.graphics.Canvas, x: Float, y: Float, radius: Float, color: Int) {
        val alpha = color ushr 24
        val rgb = color and 0x00FFFFFF
        for (step in GlowSteps.indices) {
            paint.color = rgb or ((alpha * GlowAlphas[step]).toInt() shl 24)
            canvas.drawCircle(x, y, radius * GlowSteps[step], paint)
        }
    }

    /**
     * The moon with its phase, the lit side toward the sun as the place sees it: the
     * direction on the screen from the moon to a point a little nearer the sun along the
     * sky, so an evening crescent lies the way it does over the roofs, at any turn of the
     * camera. The dark part at the earthshine's 14%, as on the hero. Under the hour's cloud
     * the disc pales and its glow goes first ([DomeLook.discShown]): an even veil, the pale
     * patch a covered moon is.
     */
    private fun DrawScope.moon(dome: SkyDome, look: DomeLook, lens: SkyLens, scale: Float) {
        val moon = dome.moon
        if (!lens.toScreen(moon.east, moon.north, moon.up, spot)) return
        val radius = MoonRadius.toPx() * scale
        if (!lens.onScreen(spot[0], spot[1], radius * 3f)) return
        val centre = Offset(spot[0], spot[1])
        // A step from the moon toward the sun, on the sphere.
        val sun = dome.sun
        val along = sun.east * moon.east + sun.north * moon.north + sun.up * moon.up
        val e = moon.east + SunwardStep * (sun.east - along * moon.east)
        val n = moon.north + SunwardStep * (sun.north - along * moon.north)
        val u = moon.up + SunwardStep * (sun.up - along * moon.up)
        val length = Math.sqrt(e * e + n * n + u * u)
        val angle = if (lens.toScreen(e / length, n / length, u / length, towards)) {
            Math.toDegrees(atan2((towards[1] - spot[1]).toDouble(), (towards[0] - spot[0]).toDouble())).toFloat()
        } else {
            0f
        }
        val face = SkyPalette.MoonFace
        val reach = radius * MoonGlowReach
        drawCircle(
            brush = Brush.radialGradient(
                0f to face.copy(alpha = MoonGlow * dome.moonIllumination.toFloat() * look.glowShown),
                1f to face.copy(alpha = 0f),
                center = centre, radius = reach
            ),
            radius = reach,
            center = centre
        )
        // The dark part is the sky with the earth's light on it, never darker than the sky.
        drawCircle(face.copy(alpha = Earthshine * look.discShown), radius, centre)
        rotate(angle, centre) {
            drawPath(moonLitPath(centre, radius, dome.moonIllumination.toFloat(), litRight = true), face.copy(alpha = look.discShown))
        }
    }

    /** The sun, on the night of a place that has it up at this hour: a disc in its glow. */
    private fun DrawScope.sun(dome: SkyDome, look: DomeLook, lens: SkyLens, scale: Float) {
        val sun = dome.sun
        if (!lens.toScreen(sun.east, sun.north, sun.up, spot)) return
        val radius = BodyRadius.toPx() * scale
        if (!lens.onScreen(spot[0], spot[1], radius * 6f)) return
        val centre = Offset(spot[0], spot[1])
        val warmth = (1.0 - sun.altitudeDeg / 12.0).coerceIn(0.0, 1.0).toFloat()
        val color = lerp(SkyPalette.SunHigh, SkyPalette.SunLow, warmth)
        drawCircle(
            brush = Brush.radialGradient(listOf(color.copy(alpha = 0.45f * look.glowShown), color.copy(alpha = 0f)), centre, radius * 6f),
            radius = radius * 6f,
            center = centre
        )
        drawCircle(color.copy(alpha = look.discShown), radius, centre)
    }

    /**
     * The eight directions, written on the horizon where they are: a tick on the line and
     * the word over it, toward the sky. They are how a reader who has turned the sky twice
     * knows where they are again.
     */
    private fun DrawScope.compass(lens: SkyLens, horizon: HorizonCircle, compass: List<Pair<Double, TextLayoutResult>>, keepOut: List<RectF>) {
        val tick = CompassTick.toPx()
        val gap = CompassGap.toPx()
        taken.clear()
        taken.addAll(keepOut)
        for ((bearing, word) in compass) {
            if (!lens.toScreen(bearing, 0.0, spot)) continue
            if (!lens.onScreen(spot[0], spot[1])) continue
            // Toward the sky: toward the middle of the horizon's circle.
            val dx = horizon.centreX - spot[0]
            val dy = horizon.centreY - spot[1]
            val d = hypot(dx, dy).coerceAtLeast(1f)
            val ux = dx / d
            val uy = dy / d
            drawLine(
                color = SkyPalette.Ink.copy(alpha = HorizonAlpha * 1.6f),
                start = Offset(spot[0], spot[1]),
                end = Offset(spot[0] + ux * tick, spot[1] + uy * tick),
                strokeWidth = HorizonLine.toPx()
            )
            val reach = tick + gap + word.size.height / 2f
            val cx = spot[0] + ux * reach
            val cy = spot[1] + uy * reach
            // A word whose tick is near the side of the screen is moved in beside it: the
            // tick says where the direction is, the word which one.
            val inset = CompassInset.toPx()
            val left = (cx - word.size.width / 2f).coerceIn(inset, (size.width - word.size.width - inset).coerceAtLeast(inset))
            val top = cy - word.size.height / 2f
            val box = RectF(left, top, left + word.size.width, top + word.size.height)
            if (keepOut.any { RectF.intersects(it, box) }) continue
            drawText(word, topLeft = Offset(left, top))
            // A star's name does not stand on a direction: the word was there first.
            taken += box
        }
    }

    /**
     * The names, by the window's own rules ([WindowScene.place]), in three turns: the moon,
     * the radiant and the planets first; then the constellations', each centred on the
     * point its source writes it at; then the stars from the brightest. None on another,
     * none under what stands over the sky nor on a direction's word ([taken], which
     * [compass] has filled). A star is named only while it is drawn bright enough to be the
     * thing the name points at, and the closer the field the fainter the stars it names.
     *
     * A constellation comes before a star: a reader who turned the figures on asked for
     * their names, and «Lyra» dropped for «Vega» would leave the figure traced and unnamed.
     */
    private fun DrawScope.names(
        dome: SkyDome,
        look: DomeLook,
        lens: SkyLens,
        fieldDeg: Double,
        scale: Float,
        names: ExploreNames,
        layouts: Map<String, TextLayoutResult>,
        figureLayouts: List<TextLayoutResult>?,
        density: Float
    ) {
        first.clear()
        if (dome.moonUp && lens.toScreen(dome.moon.east, dome.moon.north, dome.moon.up, spot) && lens.onScreen(spot[0], spot[1])) {
            first += WindowLabel(names.moon, spot[0], spot[1], gapDp = MoonRadius.value * scale + 6f)
        }
        val radiant = dome.radiant
        if (radiant != null && names.radiant != null &&
            lens.toScreen(radiant.azimuthDeg, radiant.altitudeDeg, spot) && lens.onScreen(spot[0], spot[1])
        ) {
            first += WindowLabel(names.radiant, spot[0], spot[1], gapDp = 10f, ring = true)
        }
        if (look.planets > DomeLook.NamedFrom) {
            dome.planets.forEachIndexed { i, p ->
                if (p.place.up <= 0.0) return@forEachIndexed
                if (!lens.toScreen(p.place.east, p.place.north, p.place.up, spot) || !lens.onScreen(spot[0], spot[1])) return@forEachIndexed
                first += WindowLabel(names.planets[i], spot[0], spot[1])
            }
        }
        write(first, layouts, density)
        if (figureLayouts != null) figureNames(dome, look, lens, fieldDeg, figureLayouts, density)
        labels.clear()
        if (look.stars > DomeLook.NamedFrom) {
            dome.named.forEachIndexed { i, s ->
                if (!ExploreNames.named(s.star.mag, s.star.decDeg, fieldDeg)) return@forEachIndexed
                if (!lens.toScreen(s.place.east, s.place.north, s.place.up, spot) || !lens.onScreen(spot[0], spot[1])) return@forEachIndexed
                labels += WindowLabel(names.stars[i], spot[0], spot[1], rank = s.star.mag)
            }
        }
        write(labels, layouts, density)
    }

    /** Places [labels] round what is already [taken], draws the ones let through, and takes their boxes. */
    private fun DrawScope.write(labels: List<WindowLabel>, layouts: Map<String, TextLayoutResult>, density: Float) {
        if (labels.isEmpty() || layouts.isEmpty()) return
        val textSize = layouts.values.first().layoutInput.style.fontSize.toPx()
        val descent = textSize * 0.25f
        val placed = WindowScene.place(
            labels, size.width.toInt(), size.height.toInt(), density,
            keepOutBottomPx = 0f,
            keepOut = taken,
            textSizePx = textSize,
            descentPx = descent,
            measure = { text -> layouts.getValue(text).size.width.toFloat() }
        )
        val ringColor = SkyPalette.Ink.copy(alpha = NameAlpha)
        placed.forEach { (label, x, baseline) ->
            val layout = layouts.getValue(label.text)
            if (label.ring) {
                drawCircle(ringColor, radius = RadiantRing.toPx(), center = Offset(label.x, label.y), style = Stroke(1.dp.toPx()))
            }
            drawText(layout, topLeft = Offset(x, baseline - layout.firstBaseline))
            taken += RectF(x - 2f * density, baseline - textSize, x + layout.size.width + 2f * density, baseline + descent)
        }
    }

    /**
     * The constellations' names (step 16.7): the ones the field names ([ExploreNames.figureNamed]),
     * the first rank before the second, each centred on its point if the point is clear of
     * the horizon and the whole name is on the screen and on nothing already written.
     */
    private fun DrawScope.figureNames(
        dome: SkyDome,
        look: DomeLook,
        lens: SkyLens,
        fieldDeg: Double,
        layouts: List<TextLayoutResult>,
        density: Float
    ) {
        val places = dome.constellationNamePlaces
        val all = ConstellationUnits.names
        val inset = 4f * density
        for (rank in 1..3) {
            if (!ExploreNames.figureNamed(rank, fieldDeg)) break
            for (i in all.indices) {
                if (all[i].rank != rank) continue
                val up = places[3 * i + 2]
                if (up < FigureNameUp) continue
                if (!lens.toScreen(places[3 * i].toDouble(), places[3 * i + 1].toDouble(), up.toDouble(), spot)) continue
                val layout = layouts[i]
                val left = spot[0] - layout.size.width / 2f
                val top = spot[1] - layout.size.height / 2f
                val box = RectF(left - inset, top, left + layout.size.width + inset, top + layout.size.height)
                if (box.left < 0f || box.top < 0f || box.right > size.width || box.bottom > size.height) continue
                if (taken.any { RectF.intersects(it, box) }) continue
                drawText(layout, topLeft = Offset(left, top), alpha = look.figures)
                taken += box
            }
        }
    }

    private companion object {
        val MoonRadius = 11.dp
        val HorizonLine = 1.dp
        val CompassTick = 6.dp
        val CompassGap = 4.dp
        val CompassInset = 8.dp
        val RadiantRing = 5.dp

        /** The ground: the sky's own bottom colour, this far toward the canvas' ink. */
        const val GroundDepth = 0.78f
        const val HorizonAlpha = 0.35f

        /** A light this bright (the second magnitude, about) wears its glow ([glow]). */
        const val GlowFrom = 0.42f
        val GlowSteps = floatArrayOf(3.4f, 2.4f, 1.6f)
        val GlowAlphas = floatArrayOf(0.05f, 0.09f, 0.16f)

        const val MoonGlow = 0.3f
        const val MoonGlowReach = 4.5f
        const val Earthshine = 0.14f
        const val SunwardStep = 0.02

        /** The constellations' lines: a hairline, a third of the canvas' white at its
         * strongest, and the air left round each star they lead to. */
        val FigureLine = 1.dp
        val FigureGap = 5.dp
        const val FigureLineAlpha = 0.3f

        /** A constellation is named from five degrees up: lower, its name stands in the
         * directions' words and half its figure is under the ground. */
        const val FigureNameUp = 0.087f
    }
}

/** The camera across a rotation or the process's death: its three numbers. */
private val CameraSaver = Saver<SkyCamera?, List<Double>>(
    save = { it?.let { c -> listOf(c.azimuthDeg, c.altitudeDeg, c.fieldDeg) } ?: emptyList() },
    restore = { if (it.size == 3) SkyCamera(it[0], it[1], it[2]) else null }
)

/** The sky's canvas, for a test to put a finger on. */
internal const val ExploreSkyTag = "explore-sky"

/** The eight directions written on the horizon, degrees from north. */
private val CompassPoints = listOf(0.0, 45.0, 90.0, 135.0, 180.0, 225.0, 270.0, 315.0)

/** One press of a zoom button: half as much sky again, or two thirds of it. */
private const val ZoomStep = 1.5f

/** One step of a screen reader's action: a third of the resting field across, a quarter up. */
private const val TurnStepDeg = 20.0
private const val LookStepDeg = 15.0

/** From here up the line says «straight up» (the height `SkyHeight.OVERHEAD` starts at). */
private const val OverheadFromDeg = 80.0

private const val NameAlpha = 0.85f
private const val FigureNameAlpha = 0.62f
private val FigureNameSpacing = 1.2.sp
/** The panel over what is behind it: nearly all of the canvas' dark. Closer than the
 * horizon reaches, the sky runs behind the panel, and at six tenths its stars and lines
 * stood in the hour's digits (seen on the first render, 11 nov 2026). */
private const val PanelScrim = 0.9f
private val PanelCorner = 20.dp

/** A window this wide has the panel as a card in its corner, this wide; narrower, across its foot. */
private val WideFrom = 600.dp
private val PanelWidth = 400.dp

/** The horizon's line stands this far over what stands on the ground. */
private val GroundLip = 12.dp

private const val MinuteMs = 60_000L
private const val DayMs = 86_400_000L

/** The calendar: a year back, five ahead ([NightPicker]). */
private const val YearsBack = 1L
private const val YearsAhead = 5L

/** The hour and its cloud, and the night, for a test to read. */
internal const val ExploreHourTag = "explore-hour"
internal const val ExploreNightTag = "explore-night"
private const val ButtonScrim = 0.5f
private const val DisabledInk = 0.38f

/** The Milky Way's brightest point, as an opacity of its glow over a dark sky. */
private const val MilkyWayStrength = 0.26f
