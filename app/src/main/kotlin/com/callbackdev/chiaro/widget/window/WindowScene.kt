package com.callbackdev.chiaro.widget.window

import android.content.Context
import android.graphics.RectF
import com.callbackdev.chiaro.domain.model.CloudLayers
import com.callbackdev.chiaro.domain.model.Coordinates
import com.callbackdev.chiaro.domain.sky.AstronomyEngine
import com.callbackdev.chiaro.domain.sky.MeteorShowerTable
import com.callbackdev.chiaro.domain.sky.Planet
import com.callbackdev.chiaro.domain.sky.SkyBody
import com.callbackdev.chiaro.domain.sky.SkyJobCatalog
import com.callbackdev.chiaro.ui.components.BodyRadius
import com.callbackdev.chiaro.ui.components.EdgeInset
import com.callbackdev.chiaro.ui.components.SkyFall
import com.callbackdev.chiaro.ui.components.SkyScene
import com.callbackdev.chiaro.ui.components.StarCameraBottomDeg
import com.callbackdev.chiaro.ui.components.StarCameraTopDeg
import com.callbackdev.chiaro.ui.components.StarField
import com.callbackdev.chiaro.ui.components.StarMark
import com.callbackdev.chiaro.ui.components.skyScene
import com.callbackdev.chiaro.ui.components.starPlace
import com.callbackdev.chiaro.ui.sky.SkyUpcoming
import com.callbackdev.chiaro.ui.theme.SkyGradient
import com.callbackdev.chiaro.ui.theme.SkyPalette
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tan

/**
 * A name written on the window's sky where the thing stands: the moon with its phase,
 * a planet, a star, a shower's radiant. [x] and [y] are px in the sky's box; [gapDp] the
 * air between the spot and the first letter; [rank] the order among the stars (the
 * magnitude, lower first); [ring] a small ring at the spot, for a point of the sky that
 * is not a light.
 */
internal class WindowLabel(
    val text: String,
    val x: Float,
    val y: Float,
    val gapDp: Float = 7f,
    val rank: Double = 0.0,
    val ring: Boolean = false
)

/** A label the rules let through, with the corner its text starts at (px, baseline). */
internal data class PlacedLabel(val label: WindowLabel, val textX: Float, val baselineY: Float)

/** A shower's radiant on its peak night, where it stands: the point its meteors trace back to. */
internal class Radiant(val jobId: String, val altitudeDeg: Double, val azimuthDeg: Double)

/**
 * The window's sky for one instant, computed once for whoever draws it (22 ott 2026: the
 * window widget's painter, the sky reminder's picture, and the Tonight card of the Sky
 * screen, which draws it live and lets the reader turn it): where it looks, the scene the
 * shader draws, the moon placed in the stars' camera, and what the names can be written on.
 */
internal class WindowFrame(
    val drawAt: Instant,
    /** Where the window looks, degrees from north. */
    val facingDeg: Double,
    /** Whether it is night in it: the stars are drawn, the compass is said. */
    val night: Boolean,
    val moonUp: Boolean,
    val sunAltitudeDeg: Double,
    val moonIllumination: Double,
    val planetsOut: Boolean,
    val gradient: SkyGradient,
    /** The scene with the moon where the stars' camera puts it, or without a moon off the window. */
    val scene: SkyScene,
    /** The moon's spot in the sky's box, px, when it is drawn at night. */
    val moonSpot: Pair<Float, Float>?,
    val radiant: Radiant?
)

internal object WindowScene {

    /** The moon turns the window only when it is honestly up: under this it is in the houses. */
    const val MoonFacingFloorDeg = 5.0

    /** Under this cover the names are written; from it on, nothing is named. */
    const val LabelCloudCeilingPct = 60

    /** The named stars a picture writes, at most. */
    const val NamedStarsAtMost = 2

    /** The window's own altitudes: lower than the hero's floor, because a card has no
     * horizon line of its own to stop at, and the same ceiling. */
    const val WindowAltLowDeg = 3f
    const val WindowAltHighDeg = 70f

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
    fun peakRadiant(at: Instant, zone: ZoneId, coords: Coordinates, followed: Set<String>): Radiant? {
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

    /**
     * Where the window looks on its own at [drawAt]: the moon when it is honestly up at
     * night, else a peak night's radiant when that is up, else the equator. The Tonight
     * card asks once a night and keeps the answer while the finger moves the hour.
     */
    fun facing(drawAt: Instant, coords: Coordinates, zone: ZoneId, southern: Boolean, followed: Set<String>): Double {
        val night = AstronomyEngine.sunAltitude(drawAt, coords) < -6.0
        if (night && AstronomyEngine.moonAltitude(drawAt, coords) > MoonFacingFloorDeg) {
            return AstronomyEngine.bodyAzimuth(SkyBody.MOON, drawAt, coords)
        }
        if (night) {
            peakRadiant(drawAt, zone, coords, followed)?.takeIf { it.altitudeDeg >= WindowAltLowDeg }?.let { return it.azimuthDeg }
        }
        return StarField.facing(southern)
    }

    /**
     * The frame for [drawAt] in a sky box [w] by [skyH] px. [facingDeg] null lets the window
     * choose: the moon when it is up at night, else a peak night's radiant when that is up,
     * else the equator; a reader who has turned the window passes where it looks now, and in
     * [turnDeg] how far it was turned, unwrapped, so the clouds turn with it ([SkyScene.turn]):
     * the wind is seen from where the window looked before the finger, the frame the clouds'
     * field was laid in, and the shader turns the field, wind and all. Seen from where it looks
     * now it was turned twice, and the sum of the drift (time by speed) jumped under the finger.
     */
    fun frame(
        table: SkyPalette,
        coords: Coordinates,
        zone: ZoneId,
        southern: Boolean,
        drawAt: Instant,
        facingDeg: Double?,
        w: Int,
        skyH: Int,
        density: Float,
        cloudPct: Int,
        turnDeg: Double = 0.0,
        layers: CloudLayers?,
        precipPct: Int,
        fall: SkyFall,
        fallMmPerHour: Double,
        windKph: Double,
        windFromDeg: Int,
        followed: Set<String>
    ): WindowFrame {
        val sunAltitude = AstronomyEngine.sunAltitude(drawAt, coords)
        val sunAzimuth = AstronomyEngine.sunAzimuth(drawAt, coords)
        val moonAltitude = AstronomyEngine.moonAltitude(drawAt, coords)
        val moonAzimuth = AstronomyEngine.bodyAzimuth(SkyBody.MOON, drawAt, coords)
        val moonIllumination = AstronomyEngine.moonIllumination(drawAt).illuminatedFraction
        val moonLimb = AstronomyEngine.moonBrightLimbAngle(drawAt, coords)
        val night = sunAltitude < -6.0
        val moonUp = night && moonAltitude > MoonFacingFloorDeg
        val radiant = if (night) {
            peakRadiant(drawAt, zone, coords, followed)?.takeIf { it.altitudeDeg >= WindowAltLowDeg }
        } else {
            null
        }
        val facing = facingDeg ?: when {
            moonUp -> moonAzimuth
            radiant != null -> radiant.azimuthDeg
            else -> StarField.facing(southern)
        }
        val halfWidth = halfWidthDeg(w, skyH)
        // The planets from the sun at −3°, before the stars (20 ott 2026): Venus in the dusk.
        val planetsOut = sunAltitude < StarField.PLANETS_SUN_DEG
        val marks = (
            if (night) StarField.visible(drawAt, coords, facing, halfWidth, WindowAltLowDeg, WindowAltHighDeg) else emptyList()
        ) + (
            if (planetsOut) StarField.planets(drawAt, coords, facing, halfWidth, WindowAltLowDeg, WindowAltHighDeg) else emptyList()
        )
        val gradient = table.gradient(sunAltitude, cloudPct, precipPct, moonIllumination, moonAltitude)
        var scene = skyScene(
            table, gradient, sunAltitude, sunAzimuth, moonAltitude, moonAzimuth,
            moonIllumination, moonLimb, southern, cloudPct, layers, precipPct,
            fall, fallMmPerHour, windKph, windFromDeg, starMarks = marks,
            galactic = if (night) StarField.galacticFrame(drawAt, coords, facing) else null,
            facingDeg = facing - turnDeg, turnDeg = turnDeg
        )
        var moonSpot: Pair<Float, Float>? = null
        if (night) {
            // The moon in the stars' camera, through the band's arithmetic with the band set to
            // the whole picture; off the window, it is not drawn, because it is not in it.
            val moon = scene.moon
            moonSpot = if (moonUp) {
                starPlace(StarMark(acrossOf(moonAzimuth, facing), moonAltitude.toFloat(), 1f, 0f, 0f), w, skyH)
            } else {
                null
            }
            // The clouds' light with it (25 ott 2026): while the moon is what lights them, the
            // light stands where the camera draws the moon, not where the bodies' band has it,
            // so the lit edges face the moon the reader sees and turn with the window.
            val light = if (moon != null && scene.light == Offset(moon.across, moon.up)) {
                cameraLight(moonAzimuth, moonAltitude, facing, w, skyH, density)
            } else {
                scene.light
            }
            scene = if (moon != null && moonSpot != null) {
                scene.copy(moon = moon.copy(across = bandAcross(moonSpot.first, w), up = bandUp(moonSpot.second, skyH, density)), light = light)
            } else {
                scene.copy(moon = null, light = light)
            }
        }
        return WindowFrame(
            drawAt = drawAt, facingDeg = facing, night = night, moonUp = moonUp,
            sunAltitudeDeg = sunAltitude, moonIllumination = moonIllumination, planetsOut = planetsOut,
            gradient = gradient, scene = scene, moonSpot = moonSpot, radiant = radiant
        )
    }

    /**
     * The names the frame can carry, unplaced, in the order of their priority: the moon with
     * [moonPct] (the widget's is the instant's phase, the Tonight card's is the night's, one
     * moon number per card), the radiant, the planets, the brightest stars. None under a
     * cover of [LabelCloudCeilingPct]: a name on a star behind a cloud is a lie.
     */
    fun labels(
        context: Context,
        frame: WindowFrame,
        coords: Coordinates,
        w: Int,
        skyH: Int,
        cloudPct: Int,
        moonPct: Int
    ): List<WindowLabel> {
        if (cloudPct >= LabelCloudCeilingPct) return emptyList()
        val labels = ArrayList<WindowLabel>()
        if (frame.night) {
            frame.moonSpot?.let { (x, y) ->
                labels += WindowLabel(WindowText.moonLabel(context, moonPct), x, y, gapDp = BodyRadius.value + 6f)
            }
            frame.radiant?.let { r ->
                starPlace(StarMark(acrossOf(r.azimuthDeg, frame.facingDeg), r.altitudeDeg.toFloat(), 1f, 0f, 0f), w, skyH)?.let { (x, y) ->
                    labels += WindowLabel(WindowText.radiantLabel(context, r.jobId), x, y, gapDp = 10f, ring = true)
                }
            }
        }
        if (frame.planetsOut) {
            for (planet in Planet.values()) {
                val altitude = AstronomyEngine.planetAltitude(planet, frame.drawAt, coords)
                if (altitude < WindowAltLowDeg) continue
                val azimuth = AstronomyEngine.planetAzimuth(planet, frame.drawAt, coords)
                val spot = starPlace(StarMark(acrossOf(azimuth, frame.facingDeg), altitude.toFloat(), 1f, 0f, 0f), w, skyH) ?: continue
                labels += WindowLabel(WindowText.planetLabel(context, planet), spot.first, spot.second)
            }
        }
        if (frame.night) {
            labels += NamedStars.mapNotNull { star ->
                val hz = AstronomyEngine.starHorizontal(star.raDeg, star.decDeg, frame.drawAt, coords)
                if (hz.altitudeDeg < WindowAltLowDeg) return@mapNotNull null
                val spot = starPlace(StarMark(acrossOf(hz.azimuthDeg, frame.facingDeg), hz.altitudeDeg.toFloat(), 1f, 0f, 0f), w, skyH)
                    ?: return@mapNotNull null
                WindowLabel(context.getString(star.nameRes), spot.first, spot.second, rank = star.mag)
            }.sortedBy { it.rank }.take(NamedStarsAtMost)
        }
        return labels
    }

    /**
     * Which of [labels] are written, and where their text starts: none in the bottom
     * [keepOutBottomPx] of the box (the caption's strip), none in the first 8 dp, none
     * whose spot falls in a [keepOut] box (the words at the top left), none whose text would
     * sit on another's (from the device, 20 ott 2026: two names on the moon); the text to the
     * right of the spot, or to its left when the right edge is near. The order is the
     * priority. [measure] gives a text's width in px, [textSizePx] and [descentPx] its box.
     */
    fun place(
        labels: List<WindowLabel>,
        w: Int,
        skyH: Int,
        density: Float,
        keepOutBottomPx: Float,
        keepOut: List<RectF>,
        textSizePx: Float,
        descentPx: Float,
        measure: (String) -> Float
    ): List<PlacedLabel> {
        val boxes = ArrayList<RectF>(keepOut)
        val written = ArrayList<PlacedLabel>(labels.size)
        val floor = skyH - keepOutBottomPx
        labels.forEach { label ->
            if (label.y > floor || label.y < 8f * density) return@forEach
            if (keepOut.any { it.contains(label.x, label.y) }) return@forEach
            val width = measure(label.text)
            val gap = label.gapDp * density
            val x = if (label.x + gap + width > w - 8f * density) label.x - gap - width else label.x + gap
            val y = (label.y + 4f * density).coerceAtLeast(14f * density)
            val box = RectF(x - 2f * density, y - textSizePx, x + width + 2f * density, y + descentPx)
            if (boxes.any { RectF.intersects(it, box) }) return@forEach
            boxes += box
            written += PlacedLabel(label, x, y)
        }
        return written
    }

    /** The window's half-width in azimuth for a box: the stars' camera's focal length is set
     * by the height, the width takes what it takes, plus a margin so no star pops at the edge. */
    internal fun halfWidthDeg(widthPx: Int, skyHeightPx: Int): Float {
        val focal = skyHeightPx / Math.toRadians((StarCameraTopDeg - StarCameraBottomDeg).toDouble())
        return Math.toDegrees(atan((widthPx / 2.0) / focal)).toFloat() + 4f
    }

    /** Degrees of azimuth one px of the box turns through at its centre: the camera's focal length. */
    internal fun degreesPerPx(skyHeightPx: Int): Float {
        val focal = skyHeightPx / Math.toRadians((StarCameraTopDeg - StarCameraBottomDeg).toDouble())
        return Math.toDegrees(1.0 / focal).toFloat()
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
     * Where the clouds' light stands when a body at [azimuthDeg], [altitudeDeg] gives it, in the
     * band's frame with the band the whole picture: the stars' camera's place for it
     * (`starPlace`) up to [LightEdgeDeg] off the axis, 3.7 focal lengths out, well past any
     * phone's edge. Past that a pinhole cannot place it, and the light **turns on round the
     * canvas** (committente, from the device, 25 ott 2026): from where it stood at the edge, at
     * the same distance, its direction from the canvas' middle turns toward straight down as the
     * bearing goes on to 180°, so a moon behind the reader lights the clouds from the near
     * side, the foot of the picture, which is the side that faces it. Both ways round meet
     * there, and the light never jumps. Held at the edge, as the first cut had it, the bearing
     * wrapped from +180° to −180° opposite the moon and the light leapt from one edge to the
     * other: every lit edge in the deck changed side at once, a picture redrawn with another
     * angle, at west whenever the moon was in the east. Off the picture is fine: the shader
     * extrapolates a light as it places a body.
     */
    private fun cameraLight(azimuthDeg: Double, altitudeDeg: Double, facingDeg: Double, w: Int, skyH: Int, density: Float): Offset {
        val across = acrossOf(azimuthDeg, facingDeg)
        val span = StarCameraTopDeg - StarCameraBottomDeg
        val focal = skyH / Math.toRadians(span.toDouble())
        val edge = across.coerceIn(-LightEdgeDeg, LightEdgeDeg)
        var x = w / 2.0 + tan(Math.toRadians(edge.toDouble())) * focal
        var y = skyH * ((StarCameraTopDeg - altitudeDeg) / span)
        if (abs(across) > LightEdgeDeg) {
            val cx = w / 2.0
            val cy = skyH / 2.0
            // The angle at the edge, screen-wise (y down): about 0 on the right, about π on the
            // left, kept on the left's side of the cut so the turn toward π/2 is the short way.
            val raw = atan2(y - cy, x - cx)
            val atEdge = if (across < 0 && raw < 0) raw + 2 * Math.PI else raw
            val reach = hypot(x - cx, y - cy)
            val behind = (abs(across) - LightEdgeDeg) / (180.0 - LightEdgeDeg)
            val angle = atEdge + (Math.PI / 2 - atEdge) * behind
            x = cx + cos(angle) * reach
            y = cy + sin(angle) * reach
        }
        return Offset(bandAcross(x.toFloat(), w), bandUp(y.toFloat(), skyH, density))
    }

    /** The whole-percent phase of the moon at [at], for a label. */
    fun moonPct(at: Instant): Int = (AstronomyEngine.moonIllumination(at).illuminatedFraction * 100).roundToInt()

    /** How far off the camera's axis the pinhole places the clouds' light, degrees: at 75° it
     * is 3.7 focal lengths out, well past any phone's edge and still a direction; further
     * round, the light turns on toward the foot of the picture ([cameraLight]). */
    private const val LightEdgeDeg = 75f
}
