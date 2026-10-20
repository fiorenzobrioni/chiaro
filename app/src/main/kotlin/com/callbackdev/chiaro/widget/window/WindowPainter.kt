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
import com.callbackdev.chiaro.domain.model.Coordinates
import com.callbackdev.chiaro.domain.sky.AstronomyEngine
import com.callbackdev.chiaro.domain.sky.MeteorShowerTable
import com.callbackdev.chiaro.domain.sky.Planet
import com.callbackdev.chiaro.domain.sky.SkyJobCatalog
import com.callbackdev.chiaro.ui.sky.SkyUpcoming
import com.callbackdev.chiaro.ui.components.BodyRadius
import com.callbackdev.chiaro.ui.components.EdgeInset
import com.callbackdev.chiaro.ui.theme.SkyGradient
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
    val gpu: Boolean
)

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

    /** The moon turns the window only when it is honestly up: under this it is in the houses. */
    const val MoonFacingFloorDeg = 5.0

    /** Under this cover the names are written; from it on, nothing is named. */
    const val LabelCloudCeilingPct = 60

    /** The named stars a card writes, at most. */
    const val NamedStarsAtMost = 2

    /** The window's own altitudes: lower than the hero's floor, because a card has no
     * horizon line of its own to stop at, and the same ceiling. */
    const val WindowAltLowDeg = 3f
    const val WindowAltHighDeg = 70f

    /** The strip at the foot of the picture the caption owns: no name is written in it. */
    const val LabelKeepOutDp = 60f

    private class Label(
        val text: String, val x: Float, val y: Float, val gapDp: Float = 7f, val rank: Double = 0.0,
        /** A small ring at the spot: the thing named is a point of the sky, not a light. */
        val ring: Boolean = false
    )

    /** A shower's radiant on its peak night, where it stands: the point its meteors trace back to. */
    internal class Radiant(val jobId: String, val altitudeDeg: Double, val azimuthDeg: Double)

    /** How far ahead a peak window may start and still be tonight's: the evening before a
     * peak in the small hours is the peak's night, the stream being days wide. */
    private val PeakAhead: Duration = Duration.ofHours(14)

    /**
     * The shower whose peak night this is, with its radiant placed by the engine, or null:
     * the catalogue's showers resolved for [at] as the Sky tab resolves them, kept when the
     * peak's night window is open or opens within [PeakAhead]. A strong shower counts on every
     * peak night; a weak one only when the reader follows it ([followed], the job ids of the
     * moments on the card; committente, 20 ott 2026: a fresh install follows no shower and
     * must still see the Perseids, and «Taurids, from here» for five an hour is a promise the
     * sky will not keep). Two showers on one night (the delta Aquariids and the alpha
     * Capricornids share one) give the earlier window.
     */
    internal fun peakRadiant(at: Instant, zone: ZoneId, coords: Coordinates, followed: Set<String>): Radiant? {
        val jobs = SkyJobCatalog.meteorShowers.filter { job ->
            MeteorShowerTable.showerOf(job.id)?.strong == true || job.id in followed
        }
        val tonight = SkyUpcoming.allAt(jobs, at, zone, coords)
            .firstOrNull { resolved ->
                val window = resolved.at ?: return@firstOrNull false
                val end = window.end ?: return@firstOrNull false
                end.isAfter(at) && window.start.isBefore(at.plus(PeakAhead))
            } ?: return null
        val shower = MeteorShowerTable.showerOf(tonight.occurrence.job.id) ?: return null
        val place = AstronomyEngine.starHorizontal(shower.radiantRaDeg, shower.radiantDecDeg, at, coords)
        return Radiant(tonight.occurrence.job.id, place.altitudeDeg, place.azimuthDeg)
    }

    /** The picture for [model] in a card of [size] laid out as [plan], at [at]; null without a report. */
    fun paint(
        context: Context,
        model: WidgetModel,
        size: DpSize,
        plan: WindowPlan,
        at: Instant,
        gpu: Boolean = true
    ): WindowPicture? {
        val content = model.content ?: return null
        val city = model.city ?: return null
        val sky = content.sky
        val density = context.resources.displayMetrics.density
        val w = (size.width.value * density).roundToInt().coerceAtLeast(1)
        val h = (size.height.value * density).roundToInt().coerceAtLeast(1)
        val skyH = (plan.skyHeight.value * density).roundToInt().coerceIn(1, h)
        val coords = city.coordinates
        val night = sky.sunAltitudeDeg < -6.0
        val moonUp = night && sky.moonAltitudeDeg > MoonFacingFloorDeg
        // Where the window looks: at the moon when it is up; else, on a shower's peak night,
        // at its radiant when that is up (20 ott 2026); else at the equator.
        val radiant = if (night) {
            peakRadiant(at, model.zone, coords, followed = model.moments.map { it.job.id }.toSet())
                ?.takeIf { it.altitudeDeg >= WindowAltLowDeg }
        } else {
            null
        }
        val facing = when {
            moonUp -> sky.moonAzimuthDeg
            radiant != null -> radiant.azimuthDeg
            else -> StarField.facing(sky.southern)
        }
        val halfWidth = halfWidthDeg(w, skyH)
        val marks = if (night) {
            StarField.visible(at, coords, facing, halfWidth, WindowAltLowDeg, WindowAltHighDeg) +
                StarField.planets(at, coords, facing, halfWidth, WindowAltLowDeg, WindowAltHighDeg)
        } else {
            emptyList()
        }
        val table = paletteFor(model.settings.palette).sky
        val gradient = table.gradient(
            sky.sunAltitudeDeg, sky.cloudPct, sky.precipPct, sky.moonIllumination, sky.moonAltitudeDeg
        )
        var scene = skyScene(
            table, gradient, sky.sunAltitudeDeg, sky.sunAzimuthDeg, sky.moonAltitudeDeg, sky.moonAzimuthDeg,
            sky.moonIllumination, sky.moonLimbAngleDeg, sky.southern, sky.cloudPct, sky.layers, sky.precipPct,
            sky.fall, sky.fallMmPerHour, sky.windKph, sky.windFromDeg, starMarks = marks,
            galactic = if (night) StarField.galacticFrame(at, coords, facing) else null
        )
        val labels = ArrayList<Label>()
        var moonSpot: Pair<Float, Float>? = null
        if (night) {
            // The moon in the stars' camera, through the band's arithmetic with the band set to
            // the whole picture; off the window, it is not drawn, because it is not in it.
            val moon = scene.moon
            moonSpot = if (moonUp) {
                starPlace(StarMark(acrossOf(sky.moonAzimuthDeg, facing), sky.moonAltitudeDeg.toFloat(), 1f, 0f, 0f), w, skyH)
            } else {
                null
            }
            scene = if (moon != null && moonSpot != null) {
                scene.copy(moon = moon.copy(across = bandAcross(moonSpot.first, w), up = bandUp(moonSpot.second, skyH, density)))
            } else {
                scene.copy(moon = null)
            }
            if (plan.labels && sky.cloudPct < LabelCloudCeilingPct) {
                moonSpot?.let { (x, y) ->
                    labels += Label(
                        WindowText.moonLabel(context, (sky.moonIllumination * 100).roundToInt()),
                        x, y, gapDp = BodyRadius.value + 6f
                    )
                }
                radiant?.let { r ->
                    starPlace(StarMark(acrossOf(r.azimuthDeg, facing), r.altitudeDeg.toFloat(), 1f, 0f, 0f), w, skyH)?.let { (x, y) ->
                        labels += Label(WindowText.radiantLabel(context, r.jobId), x, y, gapDp = 10f, ring = true)
                    }
                }
                for (planet in Planet.values()) {
                    val altitude = AstronomyEngine.planetAltitude(planet, at, coords)
                    if (altitude < WindowAltLowDeg) continue
                    val azimuth = AstronomyEngine.planetAzimuth(planet, at, coords)
                    val spot = starPlace(StarMark(acrossOf(azimuth, facing), altitude.toFloat(), 1f, 0f, 0f), w, skyH) ?: continue
                    labels += Label(WindowText.planetLabel(context, planet), spot.first, spot.second)
                }
                labels += NamedStars.mapNotNull { star ->
                    val hz = AstronomyEngine.starHorizontal(star.raDeg, star.decDeg, at, coords)
                    if (hz.altitudeDeg < WindowAltLowDeg) return@mapNotNull null
                    val spot = starPlace(StarMark(acrossOf(hz.azimuthDeg, facing), hz.altitudeDeg.toFloat(), 1f, 0f, 0f), w, skyH)
                        ?: return@mapNotNull null
                    Label(context.getString(star.nameRes), spot.first, spot.second, rank = star.mag)
                }.sortedBy { it.rank }.take(NamedStarsAtMost)
            }
        }
        val drawn = if (gpu) runCatching { renderGpu(gradient, scene, w, skyH, density) }.getOrNull() else null
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
        finish(context, canvas, labels, plan, w, skyH, density)
        return WindowPicture(picture, facing, night, gpu = drawn != null)
    }

    /** The window's half-width in azimuth for a box: the stars' camera's focal length is set
     * by the height, the width takes what it takes, plus a margin so no star pops at the edge. */
    internal fun halfWidthDeg(widthPx: Int, skyHeightPx: Int): Float {
        val focal = skyHeightPx / Math.toRadians((StarCameraTopDeg - StarCameraBottomDeg).toDouble())
        return Math.toDegrees(atan((widthPx / 2.0) / focal)).toFloat() + 4f
    }

    internal fun acrossOf(azimuthDeg: Double, facingDeg: Double): Float =
        (((azimuthDeg - facingDeg + 540.0) % 360.0) - 180.0).toFloat()

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
    private fun renderGpu(gradient: SkyGradient, scene: SkyScene, w: Int, skyH: Int, density: Float): Bitmap {
        val shader = RuntimeShader(SkyShaderSource)
        val stars = StarMap().shaderFor(scene.starField, w, skyH, density)
        shader.bindSky(w.toFloat(), skyH.toFloat(), density, 0f, gradient, scene, 0f, skyH.toFloat(), stars)
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

    /** The caption's scrim and the names, over the picture. */
    private fun finish(
        context: Context,
        canvas: Canvas,
        labels: List<Label>,
        plan: WindowPlan,
        w: Int,
        skyH: Int,
        density: Float
    ) {
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
        if (labels.isEmpty()) return
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = LabelInk
            textSize = WindowLabelSp * fontScale(context) * density
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            setShadowLayer(3f * density, 0f, 1f * density, LabelHalo)
        }
        val keepOut = skyH - LabelKeepOutDp * density
        val warningBox = if (plan.warning) RectF(0f, 0f, WarningBoxWidthDp * density, WarningBoxHeightDp * density) else null
        val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = LabelInk
            style = Paint.Style.STROKE
            strokeWidth = 1f * density
            setShadowLayer(2f * density, 0f, 0f, LabelHalo)
        }
        labels.forEach { label ->
            if (label.y > keepOut || label.y < 8f * density) return@forEach
            if (warningBox?.contains(label.x, label.y) == true) return@forEach
            if (label.ring) canvas.drawCircle(label.x, label.y, RadiantRingDp * density, ring)
            val width = paint.measureText(label.text)
            val gap = label.gapDp * density
            val x = if (label.x + gap + width > w - 8f * density) label.x - gap - width else label.x + gap
            val y = (label.y + 4f * density).coerceAtLeast(14f * density)
            canvas.drawText(label.text, x, y, paint)
        }
    }

    /** The scrim the caption stands on: nothing to half black over the last 70 dp of the sky. */
    private const val CaptionScrimDp = 70f
    private const val CaptionScrim = 0x80000000.toInt()

    /** The rows' ground over the sky's bottom colour. */
    private const val RowsShade = 0x73000000

    /** The names' ink and its halo (§3.6's halo, in a bitmap). */
    private const val LabelInk = 0xD9FFFFFF.toInt()
    private const val LabelHalo = 0xB3000000.toInt()

    /** The radiant's ring: a point of the sky marked, not a light drawn. */
    private const val RadiantRingDp = 5f

    /** Where the warning's chip sits (top-left, inside the caption's padding): no name under it. */
    private const val WarningBoxWidthDp = 220f
    private const val WarningBoxHeightDp = 48f
}
