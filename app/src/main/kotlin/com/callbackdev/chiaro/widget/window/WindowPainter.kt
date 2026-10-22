package com.callbackdev.chiaro.widget.window

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.HardwareRenderer
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.RenderNode
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.graphics.Typeface
import android.media.ImageReader
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.DpSize
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.data.AppPalette
import com.callbackdev.chiaro.domain.model.CloudLayers
import com.callbackdev.chiaro.domain.model.Coordinates
import com.callbackdev.chiaro.domain.sky.AstronomyEngine
import com.callbackdev.chiaro.domain.sky.MeteorShowerTable
import com.callbackdev.chiaro.domain.sky.Planet
import com.callbackdev.chiaro.domain.sky.SkyBody
import com.callbackdev.chiaro.domain.sky.SkyJobCatalog
import com.callbackdev.chiaro.ui.sky.SkyUpcoming
import com.callbackdev.chiaro.ui.components.BodyRadius
import com.callbackdev.chiaro.ui.components.EdgeInset
import com.callbackdev.chiaro.ui.theme.SkyGradient
import com.callbackdev.chiaro.ui.components.SkyFall
import com.callbackdev.chiaro.ui.components.SkyScene
import com.callbackdev.chiaro.ui.components.SkyShaderSource
import com.callbackdev.chiaro.ui.components.StarCameraBottomDeg
import com.callbackdev.chiaro.ui.components.StarCameraTopDeg
import com.callbackdev.chiaro.ui.components.StarField
import com.callbackdev.chiaro.ui.components.StarMap
import com.callbackdev.chiaro.ui.components.StarMark
import com.callbackdev.chiaro.ui.components.bindSky
import com.callbackdev.chiaro.ui.components.markRadiusDp
import com.callbackdev.chiaro.ui.components.skyScene
import com.callbackdev.chiaro.ui.components.starPlace
import com.callbackdev.chiaro.ui.theme.paletteFor
import com.callbackdev.chiaro.widget.WidgetModel
import com.callbackdev.chiaro.widget.fontScale
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import kotlin.math.atan
import kotlin.math.roundToInt

/** The window's picture, and two facts the caption needs from it. */
internal class WindowPicture(
    val bitmap: Bitmap,
    /** Where the window looks, degrees from north. */
    val facingDeg: Double,
    /** Whether it is night in it: the stars are drawn, the compass is said. */
    val night: Boolean,
    /** Whether the sky's shader drew it, or the plain fallback did. */
    val gpu: Boolean,
    /** The instant the sky in it is computed for: now, or the middle of the hour at full night. */
    val drawnAt: Instant,
    /** Whether the moon is drawn and named in it, with its phase: then the caption leaves the
     * moon's number to the disc (from the device, 20 ott 2026: «Moon 68%» on the disc and
     * «moon 69%» in the sentence, now and the night's middle, read as a mistake). */
    val moonNamed: Boolean,
    /** Whether it is the sky behind the clouds (the look's switch), with the note that says so. */
    val bare: Boolean
)

/**
 * What the window's sky is drawn from (21 ott 2026): a place, the hour's weather, the
 * reader's dress. The card makes one from its [WidgetModel] ([of]); a notification makes one
 * from a report for the instant it is about ([forHour]), which is how the sky reminder
 * carries the scene of its moment without a widget's model behind it.
 */
internal class WindowSky(
    val coords: Coordinates,
    val zone: ZoneId,
    val southern: Boolean,
    val cloudPct: Int,
    val layers: CloudLayers?,
    val precipPct: Int,
    val fall: SkyFall,
    val fallMmPerHour: Double,
    val windKph: Double,
    val windFromDeg: Int,
    val palette: AppPalette,
    /** The job ids of the moments the reader follows: a weak shower's radiant is drawn only for them. */
    val followed: Set<String>,
    /** «Behind the clouds»: the sky without this hour's cover, veil, rain and snow. */
    val bare: Boolean = false
) {
    companion object {
        /** The card's: this hour's weather as the hero has it, and the look's switch. */
        fun of(model: WidgetModel): WindowSky? {
            val content = model.content ?: return null
            val city = model.city ?: return null
            val sky = content.sky
            return WindowSky(
                coords = city.coordinates, zone = model.zone, southern = sky.southern,
                cloudPct = sky.cloudPct, layers = sky.layers, precipPct = sky.precipPct,
                fall = sky.fall, fallMmPerHour = sky.fallMmPerHour,
                windKph = sky.windKph, windFromDeg = sky.windFromDeg,
                palette = model.settings.palette,
                followed = model.moments.map { it.job.id }.toSet(),
                bare = model.look.windowBareSky
            )
        }

        /**
         * A report's forecast for the hour that holds [at], or null when the report has no
         * such hour: a sky drawn without its clouds would be a sky the forecast never said.
         * The hourly series carries no amount, so nothing falls in the drawing by the
         * millimetre; the state of the hour says whether rain or snow falls at all.
         */
        fun forHour(
            report: com.callbackdev.chiaro.domain.model.WeatherReport,
            coords: Coordinates,
            zone: ZoneId,
            at: Instant,
            palette: AppPalette,
            followed: Set<String>
        ): WindowSky? {
            val hour = report.hourly.firstOrNull { !at.isBefore(it.at) && at.isBefore(it.at.plusSeconds(3_600)) }
                ?: return null
            return WindowSky(
                coords = coords, zone = zone, southern = coords.lat < 0.0,
                cloudPct = hour.cloudCoverPct, layers = hour.cloudLayers,
                precipPct = hour.precipChancePct ?: 0,
                fall = com.callbackdev.chiaro.ui.today.TodayStateBuilder.fallOf(hour.condition.wmoCode),
                fallMmPerHour = 0.0,
                windKph = hour.windKph ?: 0.0, windFromDeg = hour.windDirectionDeg ?: 0,
                palette = palette, followed = followed
            )
        }
    }
}

/**
 * What a card writes over its picture, which the names must not stand on (21 ott 2026):
 * the caption's strip at the foot, the hour's weather at the top left, the warning's chip
 * under it. A notification's picture carries none of them: its words are the notification's.
 */
internal class WindowOverlay(
    val captionDp: Float = 0f,
    val weatherLine: String? = null,
    val warning: Boolean = false,
    val note: String? = null
) {
    val weather: Boolean get() = weatherLine != null
}

/**
 * The window's picture (20 ott 2026): the sky of this hour as the hero draws it, rendered by
 * the same shader into the card's box, with the moon and the stars in ONE camera, the stars'
 * (`starPlace`): on a card there is no hero text to keep a band for, so the moon stands
 * where the stars' camera puts it and its distance from a star is the sky's. The window
 * turns toward the moon when it is up at night, and faces the equator otherwise; the
 * caption says where it looks. The brightest things in it are named where they stand: the
 * moon with its phase, Venus and Jupiter, two stars at most, never under a cover of 60% or
 * more (a name on a star behind a cloud is a lie) and never over the words.
 *
 * The shader needs the GPU: `HardwareRenderer` draws the scene into an image off screen,
 * which is how the README's pictures are drawn too. Where that fails, the plain fallback
 * draws what a `Canvas` can — the hour's gradient, the stars as dots, the moon as a disc —
 * and says nothing it cannot draw: no cloud shapes, the cover said by the gradient's grey
 * and the caption's number. [WindowPicture.gpu] tells which one drew.
 */
internal object WindowPainter {

    /** The window's rules, shared with the Tonight card since 22 ott 2026 ([WindowScene]);
     * named here too for the widget's tests and its callers. */
    const val MoonFacingFloorDeg = WindowScene.MoonFacingFloorDeg
    const val WindowAltLowDeg = WindowScene.WindowAltLowDeg

    /** The strip at the foot of the picture the caption owns: no name is written in it. */
    const val LabelKeepOutDp = 60f

    /** Full night, where the picture is drawn for the hour's middle: the sun this far under. */
    const val FullNightSunDeg = -12.0

    /** Half an hour: the middle of the hour a card repainted hourly stands for. */
    val HalfHour: Duration = Duration.ofMinutes(30)

    internal fun peakRadiant(at: Instant, zone: ZoneId, coords: Coordinates, followed: Set<String>): Radiant? =
        WindowScene.peakRadiant(at, zone, coords, followed)

    /** The picture for [model] in a card of [size] laid out as [plan], at [at]; null without a report. */
    fun paint(
        context: Context,
        model: WidgetModel,
        size: DpSize,
        plan: WindowPlan,
        at: Instant,
        gpu: Boolean = true
    ): WindowPicture? {
        val sky = WindowSky.of(model) ?: return null
        val content = model.content ?: return null
        val density = context.resources.displayMetrics.density
        val w = (size.width.value * density).roundToInt().coerceAtLeast(1)
        val h = (size.height.value * density).roundToInt().coerceAtLeast(1)
        val skyH = (plan.skyHeight.value * density).roundToInt().coerceIn(1, h)
        return paintSky(
            context, sky, w, h, skyH, at, density,
            names = plan.labels,
            overlay = WindowOverlay(
                captionDp = LabelKeepOutDp,
                weatherLine = if (plan.weather) WindowText.weatherLine(context, content, model) else null,
                warning = plan.warning,
                note = if (sky.bare) context.getString(R.string.window_bare_note) else null
            ),
            gpu = gpu
        )
    }

    /**
     * The sky [sky] at [at] in a picture [w] by [h] px whose sky takes the top [skyH] of
     * them, with the names of the brightest things when [names], and [overlay] saying what
     * else is written over it. [density] is the px a dp takes where the picture is shown:
     * the card's screen, or the width a notification's picture is laid out at.
     */
    fun paintSky(
        context: Context,
        sky: WindowSky,
        w: Int,
        h: Int,
        skyH: Int,
        at: Instant,
        density: Float,
        names: Boolean,
        overlay: WindowOverlay = WindowOverlay(),
        gpu: Boolean = true
    ): WindowPicture {
        // «Behind the clouds» (committente, 20 ott 2026): the picture without this hour's
        // clouds, veil, rain and snow, the sky as it really is behind them, and a note in the
        // picture that says so. The words of the card keep the forecast; only the drawing moves.
        val bare = sky.bare
        val cloudPct = if (bare) 0 else sky.cloudPct
        val layers = if (bare) null else sky.layers
        val precipPct = if (bare) 0 else sky.precipPct
        val fall = if (bare) SkyFall.NONE else sky.fall
        val fallMm = if (bare) 0.0 else sky.fallMmPerHour
        val coords = sky.coords
        // The hour's middle (committente, 20 ott 2026): a card repainted once an hour stands for
        // that hour, and at full night (the sun under −12°, when the stars are out and the
        // gradient has stopped moving) it is drawn for the hour's middle, thirty minutes on,
        // so the stars and the moon are seven degrees ahead when it is fresh and seven behind
        // at the end, instead of fifteen behind: half the error at no cost. By day and in
        // twilight it is drawn for now, because there the sky changes in minutes and a sun
        // set a quarter of an hour early would be a lie. The clouds are the forecast hour's
        // either way; only the astronomy moves.
        val drawAt = if (AstronomyEngine.sunAltitude(at, coords) < FullNightSunDeg) at.plus(HalfHour) else at
        // The frame: where the window looks, the scene, the moon in the stars' camera
        // ([WindowScene], shared with the Tonight card since 22 ott 2026).
        val frame = WindowScene.frame(
            table = paletteFor(sky.palette).sky, coords = coords, zone = sky.zone, southern = sky.southern,
            drawAt = drawAt, facingDeg = null, w = w, skyH = skyH, density = density,
            cloudPct = cloudPct, layers = layers, precipPct = precipPct, fall = fall, fallMmPerHour = fallMm,
            windKph = sky.windKph, windFromDeg = sky.windFromDeg, followed = sky.followed
        )
        val night = frame.night
        val gradient = frame.gradient
        val scene = frame.scene
        val moonSpot = frame.moonSpot
        // The names, with the moon's phase of the instant: the widget has no other moon number.
        val labels = if (names) {
            WindowScene.labels(context, frame, coords, w, skyH, cloudPct, moonPct = WindowScene.moonPct(drawAt))
        } else {
            emptyList()
        }
        val moonLabel = if (night && moonSpot != null) labels.firstOrNull() else null

        // The bodies' band (committente, 22 ott 2026). At night it is the whole picture, because
        // the moon is put where the stars' camera puts it through the band's arithmetic above,
        // and a band that ends over the words would lift a setting moon off its place. By day
        // the sun stands on the band as the hero's does, a convention and not a camera, so the
        // band is the picture above the caption's strip and under the hour's words, as the
        // hero's is the strip above its text (§8.1c): with the band set to the whole picture a
        // sun under 25°, which is most of a late October day in Milan, stood in the caption.
        val scale = fontScale(context)
        val bandTop = if (night) 0f else dayBandTopDp(overlay, scale) * density
        val bandFoot = if (night || overlay.captionDp <= 0f) skyH.toFloat() else skyH - CaptionScrimDp * density
        val bandHeight = (bandFoot - bandTop).coerceAtLeast(2f * BodyRadius.value * density)
        val drawn = if (gpu) runCatching { renderGpu(gradient, scene, w, skyH, density, bandTop, bandHeight) }.getOrNull() else null
        val skyBitmap = drawn ?: renderSoftware(gradient, scene, w, skyH, density, moonSpot)
        val picture = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(picture)
        canvas.drawBitmap(skyBitmap, 0f, 0f, null)
        if (h > skyH) {
            // Under the rows the sky goes on, darkened: the bottom stop at a third.
            canvas.drawRect(0f, skyH.toFloat(), w.toFloat(), h.toFloat(), Paint().apply { color = gradient.bottom.toArgb() })
            canvas.drawRect(0f, skyH.toFloat(), w.toFloat(), h.toFloat(), Paint().apply { color = RowsShade })
        }
        skyBitmap.recycle()
        val written = finish(context, canvas, labels, overlay, w, skyH, density)
        return WindowPicture(
            picture, frame.facingDeg, night, gpu = drawn != null, drawnAt = drawAt,
            moonNamed = moonLabel != null && moonLabel in written,
            bare = bare
        )
    }

    /**
     * How far down the picture the words at its top left reach, in dp from the top: the
     * hour's weather line (a Glance Text at 16 sp with the font's padding), then the
     * warning's chip under it, each where WindowWidgetContent lays it.
     */
    private fun wordsBottomDp(overlay: WindowOverlay, scale: Float): Float {
        val weatherBottom = WindowCaptionPadding.value + if (overlay.weather) WeatherLineDp * scale else 0f
        return if (overlay.warning) weatherBottom + (if (overlay.weather) WarningChipGapDp else 0f) + WarningChipDp * scale else weatherBottom
    }

    /** Where the day's band starts, dp from the top: under the words at the top left when
     * there are any, so the sun never stands behind them, as the hero's band is under its
     * place row; the top of the picture otherwise. */
    internal fun dayBandTopDp(overlay: WindowOverlay, scale: Float): Float =
        if (overlay.weather || overlay.warning) wordsBottomDp(overlay, scale) + NoteGapDp else 0f


    /** The band's `across` that lands a body at [x] when the band is the whole picture. */
    internal fun bandAcross(x: Float, widthPx: Int): Float {
        val inset = widthPx * EdgeInset
        return (x - inset) / (widthPx - 2 * inset)
    }

    /** The band's `up` that lands a body at [y] when the band is the whole picture. */
    internal fun bandUp(y: Float, skyHeightPx: Int, density: Float): Float {
        val r = BodyRadius.value * density
        return 1f - (y - r) / (skyHeightPx - 2 * r)
    }

    /**
     * The scene through the sky's own shader, off screen: recorded into a `RenderNode`,
     * rendered by a `HardwareRenderer` into an `ImageReader`, read back. Throws, and the
     * caller falls back, when the environment has no GPU to draw with (a unit test without
     * a window draws nothing at all: a blank image is read as a failure too).
     */
    private fun renderGpu(
        gradient: SkyGradient,
        scene: SkyScene,
        w: Int,
        skyH: Int,
        density: Float,
        bandTop: Float,
        bandHeight: Float
    ): Bitmap {
        val shader = RuntimeShader(SkyShaderSource)
        val stars = StarMap().shaderFor(scene.starField, w, skyH, density)
        shader.bindSky(w.toFloat(), skyH.toFloat(), density, 0f, gradient, scene, bandTop, bandHeight, stars)
        val node = RenderNode("window").apply { setPosition(0, 0, w, skyH) }
        val recording = node.beginRecording()
        recording.drawRect(0f, 0f, w.toFloat(), skyH.toFloat(), Paint().apply { this.shader = shader })
        node.endRecording()
        val reader = ImageReader.newInstance(w, skyH, PixelFormat.RGBA_8888, 1)
        val renderer = HardwareRenderer()
        try {
            renderer.setSurface(reader.surface)
            renderer.setContentRoot(node)
            renderer.createRenderRequest().setWaitForPresent(true).syncAndDraw()
            val image = reader.acquireNextImage() ?: error("the renderer produced no image")
            image.use {
                val plane = it.planes[0]
                val padded = Bitmap.createBitmap(plane.rowStride / plane.pixelStride, skyH, Bitmap.Config.ARGB_8888)
                padded.copyPixelsFromBuffer(plane.buffer)
                val bitmap = Bitmap.createBitmap(padded, 0, 0, w, skyH)
                if (padded !== bitmap) padded.recycle()
                check(bitmap.getPixel(w / 2, skyH / 2) ushr 24 != 0) { "the renderer drew nothing" }
                return bitmap
            }
        } finally {
            renderer.destroy()
            reader.close()
        }
    }

    /** What a plain canvas can draw of the scene: the gradient, the stars as dots, the moon as a disc. */
    private fun renderSoftware(
        gradient: SkyGradient,
        scene: SkyScene,
        w: Int,
        skyH: Int,
        density: Float,
        moonSpot: Pair<Float, Float>?
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(w, skyH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawRect(
            0f, 0f, w.toFloat(), skyH.toFloat(),
            Paint().apply {
                shader = LinearGradient(
                    0f, 0f, 0f, skyH.toFloat(),
                    intArrayOf(gradient.top.toArgb(), gradient.mid.toArgb(), gradient.bottom.toArgb()),
                    floatArrayOf(0f, 0.5f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
        )
        if (scene.stars > 0.003f) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            scene.starField.forEach { star ->
                val (x, y) = starPlace(star, w, skyH) ?: return@forEach
                val alpha = (255f * star.brightness * scene.stars).roundToInt().coerceIn(0, 255)
                val warmth = star.warmth
                val red = (255f * (0.72f + 0.28f * warmth)).roundToInt()
                val green = (255f * (0.82f + 0.04f * warmth)).roundToInt()
                val blue = (255f * (1f - 0.38f * warmth)).roundToInt()
                paint.color = (alpha shl 24) or (red shl 16) or (green shl 8) or blue
                canvas.drawCircle(x, y, markRadiusDp(star) * density, paint)
            }
        }
        val moon = scene.moon
        if (moon != null && moonSpot != null) {
            val alpha = (255f * (0.55f + 0.45f * moon.illumination)).roundToInt().coerceIn(0, 255)
            canvas.drawCircle(
                moonSpot.first, moonSpot.second, BodyRadius.value * density,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = (alpha shl 24) or 0xF2F4F8 }
            )
        }
        return bitmap
    }

    /**
     * The caption's scrim and the names over the picture, and the picture's own words: a scrim
     * at the top when the hour's weather is written there, and «Behind the clouds» at the top
     * right when the sky is the one behind them. The names really written are returned.
     */
    private fun finish(
        context: Context,
        canvas: Canvas,
        labels: List<WindowLabel>,
        overlay: WindowOverlay,
        w: Int,
        skyH: Int,
        density: Float
    ): List<WindowLabel> {
        val note = overlay.note
        val weatherLine = overlay.weatherLine
        // The caption's scrim only where there is a caption: a notification's picture has
        // its words under it, not on it, and keeps its foot.
        if (overlay.captionDp > 0f) {
            val scrimTop = skyH - CaptionScrimDp * density
            canvas.drawRect(
                0f, scrimTop, w.toFloat(), skyH.toFloat(),
                Paint().apply {
                    shader = LinearGradient(
                        0f, scrimTop, 0f, skyH.toFloat(),
                        intArrayOf(0x00000000, CaptionScrim), floatArrayOf(0f, 1f), Shader.TileMode.CLAMP
                    )
                }
            )
        }
        if (overlay.weather) {
            // The words at the top left stand on a scrim of their own, as the caption's do.
            val scrimBottom = TopScrimDp * density
            canvas.drawRect(
                0f, 0f, w.toFloat(), scrimBottom,
                Paint().apply {
                    shader = LinearGradient(
                        0f, 0f, 0f, scrimBottom,
                        intArrayOf(TopScrim, 0x00000000), floatArrayOf(0f, 1f), Shader.TileMode.CLAMP
                    )
                }
            )
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = LabelInk
            textSize = WindowLabelSp * fontScale(context) * density
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            setShadowLayer(3f * density, 0f, 1f * density, LabelHalo)
        }
        val boxes = ArrayList<RectF>(labels.size + 1)
        val descent = paint.fontMetrics.descent
        val scale = fontScale(context)
        val chipBottom = wordsBottomDp(overlay, scale)
        if (note != null) {
            val width = paint.measureText(note)
            val x = w - NotePadDp * density - width
            // The note's line: the first, unless the hour's weather at the top left reaches
            // it (a narrow card, a large font); then the one under it, under the chip too if
            // there is one, so that nothing the card writes stands on anything else.
            val weatherEnd = weatherLine?.let {
                val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    textSize = WindowPlaceSp * scale * density
                    typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                }
                (WindowCaptionPadding.value + NoteGapDp) * density + line.measureText(it)
            }
            val top = if (weatherEnd != null && weatherEnd > x) chipBottom + NoteGapDp else NotePadDp
            val y = top * density + paint.textSize
            canvas.drawText(note, x, y, paint)
            boxes += RectF(x - 8f * density, 0f, w.toFloat(), y + descent + 4f * density)
        }
        if (labels.isEmpty()) return emptyList()
        val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = LabelInk
            style = Paint.Style.STROKE
            strokeWidth = 1f * density
            setShadowLayer(2f * density, 0f, 0f, LabelHalo)
        }
        // The words at the top left (the weather line, the chip) keep the names off them,
        // by spot and by text alike (stricter than before 22 ott 2026, when only the spot was
        // checked: a name's text could reach under the weather line).
        if (overlay.warning || overlay.weather) {
            boxes += RectF(0f, 0f, WarningBoxWidthDp * density, (chipBottom + NoteGapDp) * density)
        }
        val placed = WindowScene.place(
            labels, w, skyH, density,
            keepOutBottomPx = overlay.captionDp * density,
            keepOut = boxes,
            textSizePx = paint.textSize,
            descentPx = descent,
            measure = { paint.measureText(it) }
        )
        placed.forEach { (label, x, y) ->
            if (label.ring) canvas.drawCircle(label.x, label.y, RadiantRingDp * density, ring)
            canvas.drawText(label.text, x, y, paint)
        }
        val written = placed.map { it.label }

        return written
    }

    /** The scrim the caption stands on: nothing to half black over the last 70 dp of the sky. */
    private const val CaptionScrimDp = 70f
    private const val CaptionScrim = 0x80000000.toInt()

    /** The rows' ground over the sky's bottom colour. */
    private const val RowsShade = 0x73000000

    /** The names' ink and its halo (§3.6's halo, in a bitmap). */
    private const val LabelInk = 0xD9FFFFFF.toInt()
    private const val LabelHalo = 0xB3000000.toInt()

    /** The scrim under the hour's weather at the top: half the caption's, over 56 dp. */
    private const val TopScrimDp = 56f
    private const val TopScrim = 0x66000000

    /** The note's inset from the picture's top-right corner, and its air from the weather line. */
    private const val NotePadDp = 10f
    private const val NoteGapDp = 6f

    /** The height of the hour's weather line and of the warning's chip at font scale 1, and the
     * air between them, as WindowWidgetContent lays them: 16 sp and 13 sp with the font's
     * padding (1.33 em in the system face), the chip's 2 dp above and below. */
    private const val WeatherLineDp = 22f
    private const val WarningChipDp = 22f
    private const val WarningChipGapDp = 6f

    /** The radiant's ring: a point of the sky marked, not a light drawn. */
    private const val RadiantRingDp = 5f

    /** How far the warning's chip and the weather line reach from the left: no name under them. */
    private const val WarningBoxWidthDp = 220f
}
