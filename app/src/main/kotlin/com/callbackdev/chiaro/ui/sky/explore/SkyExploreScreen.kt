package com.callbackdev.chiaro.ui.sky.explore

import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.domain.model.Coordinates
import com.callbackdev.chiaro.domain.sky.BrightStars
import com.callbackdev.chiaro.ui.components.BodyRadius
import com.callbackdev.chiaro.ui.components.StarField
import com.callbackdev.chiaro.ui.components.moonLitPath
import com.callbackdev.chiaro.ui.components.starRadiusDp
import com.callbackdev.chiaro.ui.format.Formats
import com.callbackdev.chiaro.ui.format.LocalClock
import com.callbackdev.chiaro.ui.format.currentLocale
import com.callbackdev.chiaro.ui.sky.SkyText
import com.callbackdev.chiaro.ui.sky.SkyUiState
import com.callbackdev.chiaro.ui.sky.SkyViewModel
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
import java.time.ZoneId
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * The explorer (PLANNING, Fase 16, step 16.6; VISION §5.3): the sky of the hero's instant,
 * the whole screen of it, from the horizon to straight up, turned in every direction by a
 * finger and brought closer by two. Opened by the «Explore» pill on the Sky's hero; it
 * covers the tabs' bar, and back closes it.
 *
 * [atEpochSecond] is the hour the reader was holding on the hero's ribbon, [facingDeg]
 * where the hero was looking; null for either lets the explorer take the hero's own: now
 * while the night is in progress, the night's middle otherwise, facing where the window
 * faces on its own. The instant is taken once, when the page opens: this step has no
 * clock of its own (the hour, the night and «now» are step 16.7's panel), and the line at
 * the foot says which hour the sky is of.
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
    val clock = LocalClock.current
    // Taken when the place is first known and kept: a rotation, or a report that arrives
    // while the page is open, does not move the sky under the reader.
    val at = if (content != null) {
        rememberSaveable(atEpochSecond) {
            atEpochSecond ?: run {
                val now = Instant.now(clock)
                (content.tonight.night?.let { tonightInstant(now, it) } ?: now).epochSecond
            }
        }
    } else {
        null
    }
    if (content == null || coordinates == null || at == null) {
        // Nothing to draw a sky from yet (the state's first frame, or a place with no
        // coordinates, which has no pill to come through): the night's ground and the way back.
        ExploreShell(looking = null, footnote = null, onBack = onBack)
        return
    }
    val instant = Instant.ofEpochSecond(at)
    val facing = facingDeg?.toDouble() ?: remember(at, coordinates) {
        WindowScene.facing(instant, coordinates, content.zone, content.tonight.southern, content.subscribedIds)
    }
    SkyExploreScreen(
        coordinates = coordinates,
        zone = content.zone,
        at = instant,
        facingDeg = facing,
        followed = content.subscribedIds,
        onBack = onBack
    )
}

/**
 * The page itself, from a place and an instant: what [SkyExploreRoute] draws, and what a
 * test draws without a view model. [onFrame] is called each time the sky is drawn, for the
 * test that holds the page to its rule: **it draws when the camera changes, and only then**.
 * Nothing in this sky moves by itself, no twinkle and no drift, so a reader who is looking
 * and not touching costs the battery nothing.
 */
@Composable
internal fun SkyExploreScreen(
    coordinates: Coordinates,
    zone: ZoneId,
    at: Instant,
    facingDeg: Double,
    followed: Set<String>,
    onBack: () -> Unit,
    onFrame: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val locale = currentLocale()
    val density = LocalDensity.current
    val dome = remember(at, coordinates, zone, followed) { SkyDome.of(at, coordinates, zone, followed) }
    val look = remember(dome) { DomeLook(dome) }
    val table = ChiaroTheme.sky
    val gradient = remember(dome, table) {
        table.gradient(
            sunAltitudeDeg = dome.sun.altitudeDeg,
            moonIllumination = dome.moonIllumination,
            moonAltitudeDeg = dome.moon.altitudeDeg
        )
    }

    var viewport by remember { mutableStateOf(IntSize.Zero) }
    var chosen by rememberSaveable(at, stateSaver = CameraSaver) { mutableStateOf<SkyCamera?>(null) }
    val w = viewport.width.toFloat()
    val h = viewport.height.toFloat()
    val camera = chosen ?: if (viewport != IntSize.Zero) SkyCamera.resting(facingDeg, w, h) else null
    // From the state and never from the composed value: a drag's events come faster than
    // frames, and each must start from where the last one left the camera.
    fun move(step: (SkyCamera) -> SkyCamera) {
        val size = viewport
        if (size == IntSize.Zero) return
        val from = chosen ?: SkyCamera.resting(facingDeg, size.width.toFloat(), size.height.toFloat())
        chosen = step(from)
    }

    // The names, measured once for the instant: only their places change with the camera.
    val measurer = rememberTextMeasurer()
    val halo = skyInkShadow()
    val nameStyle = MaterialTheme.typography.labelMedium.copy(color = SkyPalette.Ink.copy(alpha = NameAlpha), shadow = halo)
    val names = remember(dome, locale) { ExploreNames.of(context, dome) }
    val nameLayouts = remember(names, nameStyle) {
        names.all.associateWith { measurer.measure(AnnotatedString(it), nameStyle) }
    }
    val compassStyle = MaterialTheme.typography.labelLarge.copy(color = SkyPalette.Ink, shadow = halo)
    // The eight words through `stringResource`, so a change of language reaches them.
    val compassWords = CompassPoints.map { stringResource(SkyText.bearingRes(it)).replaceFirstChar { c -> c.titlecase(locale) } }
    val compass = remember(compassStyle, compassWords) {
        CompassPoints.mapIndexed { i, point -> point to measurer.measure(AnnotatedString(compassWords[i]), compassStyle) }
    }

    val looking = camera?.let { lookingLine(context, it) }
    val clockText = remember(at, zone, locale) {
        at.atZone(zone).format(Formats.timeFormatter(android.text.format.DateFormat.is24HourFormat(context), locale))
    }
    val turnLeft = stringResource(R.string.explore_turn_left)
    val turnRight = stringResource(R.string.explore_turn_right)
    val lookUp = stringResource(R.string.explore_look_up)
    val lookDown = stringResource(R.string.explore_look_down)
    val painter = remember { ExplorePainter() }
    // What stands over the sky, in its own px: no name is written under them.
    var skyCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var topBox by remember { mutableStateOf<RectF?>(null) }
    var footBox by remember { mutableStateOf<RectF?>(null) }
    var zoomBox by remember { mutableStateOf<RectF?>(null) }
    fun boxOf(c: LayoutCoordinates): RectF? = skyCoords?.takeIf { it.isAttached && c.isAttached }?.let { sky ->
        val r = sky.localBoundingBoxOf(c, clipBounds = false)
        RectF(r.left, r.top, r.right, r.bottom)
    }

    ExploreShell(
        looking = looking,
        footnote = stringResource(R.string.explore_sky_at, clockText),
        onBack = onBack,
        onTop = { topBox = boxOf(it) },
        onFoot = { footBox = boxOf(it) },
        zoom = camera?.let {
            ZoomButtons(
                canIn = it.fieldDeg > SkyCamera.MinFieldDeg,
                canOut = it.fieldDeg < SkyCamera.MaxFieldDeg,
                onIn = { move { c -> c.zoomed(ZoomStep, w / 2, h / 2, w, h) } },
                onOut = { move { c -> c.zoomed(1f / ZoomStep, w / 2, h / 2, w, h) } },
                onPlaced = { box -> zoomBox = boxOf(box) }
            )
        }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag(ExploreSkyTag)
                .onSizeChanged { viewport = it }
                .onGloballyPositioned { skyCoords = it }
                .semantics {
                    contentDescription = looking ?: ""
                    customActions = listOf(
                        CustomAccessibilityAction(turnLeft) { move { it.turned(-TurnStepDeg, 0.0, w, h) }; true },
                        CustomAccessibilityAction(turnRight) { move { it.turned(TurnStepDeg, 0.0, w, h) }; true },
                        CustomAccessibilityAction(lookUp) { move { it.turned(0.0, LookStepDeg, w, h) }; true },
                        CustomAccessibilityAction(lookDown) { move { it.turned(0.0, -LookStepDeg, w, h) }; true }
                    )
                }
                .pointerInput(Unit) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        move { from ->
                            val vw = size.width.toFloat()
                            val vh = size.height.toFloat()
                            from
                                .dragged(centroid.x - pan.x, centroid.y - pan.y, centroid.x, centroid.y, vw, vh)
                                .zoomed(zoom, centroid.x, centroid.y, vw, vh)
                        }
                    }
                }
        ) {
            val cam = camera ?: return@Canvas
            onFrame?.invoke()
            with(painter) {
                draw(
                    dome = dome,
                    look = look,
                    camera = cam,
                    gradient = gradient,
                    names = names,
                    layouts = nameLayouts,
                    compass = compass,
                    keepOut = listOfNotNull(topBox, footBox, zoomBox),
                    density = density.density
                )
            }
        }
    }
}

/**
 * What stands over the sky and round it: the night's ground under everything, the cross
 * and the line that says where the camera looks at the top, the line that says which sky
 * this is at the foot, the two zoom buttons beside it. The page is the night's in both
 * themes, as the hero is (PLANNING, Fase 16, «Decisioni già prese»): white on the canvas,
 * the status bar's icons light.
 */
@Composable
private fun ExploreShell(
    looking: String?,
    footnote: String?,
    onBack: () -> Unit,
    onTop: (LayoutCoordinates) -> Unit = {},
    onFoot: (LayoutCoordinates) -> Unit = {},
    zoom: ZoomButtons? = null,
    sky: @Composable () -> Unit = {}
) {
    StatusBarIcons(overCanvas = true)
    val halo = skyInkShadow()
    CompositionLocalProvider(
        LocalChiaroColors provides ChiaroTheme.nightColors,
        LocalContentColor provides SkyPalette.Ink
    ) {
        Box(modifier = Modifier.fillMaxSize().background(SkyPalette.ScrimColor)) {
            sky()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 4.dp)
                    .onGloballyPositioned(onTop)
            ) {
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
                        style = MaterialTheme.typography.titleMedium.copy(shadow = halo),
                        color = SkyPalette.Ink,
                        modifier = Modifier.padding(start = 4.dp).clearAndSetSemantics { }
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 12.dp, bottom = 12.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    if (footnote != null) {
                        Text(
                            text = footnote,
                            style = MaterialTheme.typography.labelLarge.tabular().copy(shadow = halo),
                            color = SkyPalette.Ink,
                            modifier = Modifier.padding(bottom = 12.dp).onGloballyPositioned(onFoot)
                        )
                    }
                }
                if (zoom != null) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.onGloballyPositioned(zoom.onPlaced)
                    ) {
                        ZoomButton(plus = true, enabled = zoom.canIn, label = stringResource(R.string.explore_zoom_in), onClick = zoom.onIn)
                        ZoomButton(plus = false, enabled = zoom.canOut, label = stringResource(R.string.explore_zoom_out), onClick = zoom.onOut)
                    }
                }
            }
        }
    }
}

private class ZoomButtons(
    val canIn: Boolean,
    val canOut: Boolean,
    val onIn: () -> Unit,
    val onOut: () -> Unit,
    val onPlaced: (LayoutCoordinates) -> Unit
)

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
    val all: List<String> get() = (listOf(moon) + listOfNotNull(radiant) + planets + stars).distinct()

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
internal class DomeLook(val dome: SkyDome) {
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
            starColor[i] = lightArgb(StarField.warmth(BrightStars.bv[i]), brightness * dome.starsShown * lowAir(directions[3 * i + 2]))
        }
        val glow = SkyPalette.MilkyWayGlow
        val band = dome.milkyWayDirections
        for (i in 0 until MilkyWay.vertexCount) {
            val up = band[3 * i + 2]
            val alpha = if (up <= 0f) 0f else MilkyWayStrength * MilkyWay.strength[i] * dome.milkyWayShown * lowAir(up)
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
    }
}

/**
 * The explorer's drawing, on a plain canvas and **not through the weather's shader** (step
 * 16.6): that shader's sky is three fixed frames, and this one turns. Bottom to top: the
 * sky's own colours, lighter toward the horizon as the hero's are; the Milky Way; the
 * stars the zoom holds; the planets; the moon with its phase turned toward the sun; the
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
    private val labels = ArrayList<WindowLabel>(48)
    private val taken = ArrayList<RectF>(12)

    fun DrawScope.draw(
        dome: SkyDome,
        look: DomeLook,
        camera: SkyCamera,
        gradient: SkyGradient,
        names: ExploreNames,
        layouts: Map<String, TextLayoutResult>,
        compass: List<Pair<Double, TextLayoutResult>>,
        keepOut: List<RectF>,
        density: Float
    ) {
        val lens = camera.lens(size.width, size.height)
        val scale = SkyCamera.markScale(camera.fieldDeg)
        val horizon = lens.horizon

        sky(lens, camera, gradient, horizon)
        if (dome.milkyWayShown > 0.003f) milkyWay(dome, look, lens)
        stars(dome, look, lens, camera, scale, density)
        planets(dome, lens, scale, density)
        if (dome.sunUp) sun(dome, lens, scale)
        if (dome.moonUp) moon(dome, lens, scale)
        ground(gradient, horizon)
        compass(lens, horizon, compass, keepOut)
        names(dome, lens, camera.fieldDeg, scale, names, layouts, density)
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
        if (dome.starsShown <= 0.003f) return
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
    private fun DrawScope.planets(dome: SkyDome, lens: SkyLens, scale: Float, density: Float) {
        if (dome.planetsShown <= 0.003f) return
        val canvas = drawContext.canvas.nativeCanvas
        for (p in dome.planets) {
            if (p.place.up <= 0.0) continue
            if (!lens.toScreen(p.place.east, p.place.north, p.place.up, spot)) continue
            if (!lens.onScreen(spot[0], spot[1], 12f * density)) continue
            val brightness = StarField.planetBrightness(p.magnitude)
            val alpha = brightness * dome.planetsShown * DomeLook.lowAir(p.place.up.toFloat())
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
     * camera. The dark part at the earthshine's 14%, as on the hero.
     */
    private fun DrawScope.moon(dome: SkyDome, lens: SkyLens, scale: Float) {
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
                0f to face.copy(alpha = MoonGlow * dome.moonIllumination.toFloat()),
                1f to face.copy(alpha = 0f),
                center = centre, radius = reach
            ),
            radius = reach,
            center = centre
        )
        // The dark part is the sky with the earth's light on it, never darker than the sky.
        drawCircle(face.copy(alpha = Earthshine), radius, centre)
        rotate(angle, centre) {
            drawPath(moonLitPath(centre, radius, dome.moonIllumination.toFloat(), litRight = true), face)
        }
    }

    /** The sun, on the night of a place that has it up at this hour: a disc in its glow. */
    private fun DrawScope.sun(dome: SkyDome, lens: SkyLens, scale: Float) {
        val sun = dome.sun
        if (!lens.toScreen(sun.east, sun.north, sun.up, spot)) return
        val radius = BodyRadius.toPx() * scale
        if (!lens.onScreen(spot[0], spot[1], radius * 6f)) return
        val centre = Offset(spot[0], spot[1])
        val warmth = (1.0 - sun.altitudeDeg / 12.0).coerceIn(0.0, 1.0).toFloat()
        val color = lerp(SkyPalette.SunHigh, SkyPalette.SunLow, warmth)
        drawCircle(
            brush = Brush.radialGradient(listOf(color.copy(alpha = 0.45f), color.copy(alpha = 0f)), centre, radius * 6f),
            radius = radius * 6f,
            center = centre
        )
        drawCircle(color, radius, centre)
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
     * The names, by the window's own rules ([WindowScene.place]): the moon first, then the
     * radiant, the planets, the stars from the brightest; none on another, none under what
     * stands over the sky nor on a direction's word ([taken], which [compass] has filled). A star is named only while it is drawn bright enough to be the
     * thing the name points at, and the closer the field the fainter the stars it names.
     */
    private fun DrawScope.names(
        dome: SkyDome,
        lens: SkyLens,
        fieldDeg: Double,
        scale: Float,
        names: ExploreNames,
        layouts: Map<String, TextLayoutResult>,
        density: Float
    ) {
        labels.clear()
        if (dome.moonUp && lens.toScreen(dome.moon.east, dome.moon.north, dome.moon.up, spot) && lens.onScreen(spot[0], spot[1])) {
            labels += WindowLabel(names.moon, spot[0], spot[1], gapDp = MoonRadius.value * scale + 6f)
        }
        val radiant = dome.radiant
        if (radiant != null && names.radiant != null &&
            lens.toScreen(radiant.azimuthDeg, radiant.altitudeDeg, spot) && lens.onScreen(spot[0], spot[1])
        ) {
            labels += WindowLabel(names.radiant, spot[0], spot[1], gapDp = 10f, ring = true)
        }
        if (dome.planetsShown > NamedFrom) {
            dome.planets.forEachIndexed { i, p ->
                if (p.place.up <= 0.0) return@forEachIndexed
                if (!lens.toScreen(p.place.east, p.place.north, p.place.up, spot) || !lens.onScreen(spot[0], spot[1])) return@forEachIndexed
                labels += WindowLabel(names.planets[i], spot[0], spot[1])
            }
        }
        if (dome.starsShown > NamedFrom) {
            dome.named.forEachIndexed { i, s ->
                if (!ExploreNames.named(s.star.mag, s.star.decDeg, fieldDeg)) return@forEachIndexed
                if (!lens.toScreen(s.place.east, s.place.north, s.place.up, spot) || !lens.onScreen(spot[0], spot[1])) return@forEachIndexed
                labels += WindowLabel(names.stars[i], spot[0], spot[1], rank = s.star.mag)
            }
        }
        if (labels.isEmpty()) return
        val textSize = layouts.values.first().layoutInput.style.fontSize.toPx()
        val placed = WindowScene.place(
            labels, size.width.toInt(), size.height.toInt(), density,
            keepOutBottomPx = 0f,
            keepOut = taken,
            textSizePx = textSize,
            descentPx = textSize * 0.25f,
            measure = { text -> layouts.getValue(text).size.width.toFloat() }
        )
        val ringColor = SkyPalette.Ink.copy(alpha = NameAlpha)
        placed.forEach { (label, x, baseline) ->
            val layout = layouts.getValue(label.text)
            if (label.ring) {
                drawCircle(ringColor, radius = RadiantRing.toPx(), center = Offset(label.x, label.y), style = Stroke(1.dp.toPx()))
            }
            drawText(layout, topLeft = Offset(x, baseline - layout.firstBaseline))
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

        /** A name is written from this much of its light: under it the dot is not there to name. */
        const val NamedFrom = 0.2f
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
private const val ButtonScrim = 0.5f
private const val DisabledInk = 0.38f

/** The Milky Way's brightest point, as an opacity of its glow over a dark sky. */
private const val MilkyWayStrength = 0.26f
