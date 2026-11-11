package com.callbackdev.chiaro.ui.sky.explore

import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * The explorer's camera (PLANNING, Fase 16, step 16.6): where it looks, how high, how wide.
 * Pure arithmetic, no screen and no clock, so every claim it makes is a test
 * (`SkyCameraTest`).
 *
 * **Why a camera of its own.** The sky the hero draws is three fixed frames wired into the
 * weather's shader (DESIGN §3.9): the deck from 45° to 15°, the stars from 65° to 10°, the
 * band the sun and the moon stand in. None of them can look up: the stars' camera is a
 * pinhole whose rows are altitudes, and a pinhole whose rows are altitudes has no row for
 * the zenith. The explorer does not go through that shader.
 *
 * **The projection is stereographic**, centred where the camera looks: a direction at the
 * angle θ from the centre lands `2·f·tan(θ/2)` from the middle of the screen. Three
 * reasons, in the order they decided it:
 *
 * - it **holds the zenith**: nothing in it is an altitude or an azimuth, only the angle from
 *   where the camera looks, so straight up is a direction like any other;
 * - it **keeps shapes**: it is conformal, so the Plough at the edge of a wide field is the
 *   Plough, larger, and not a Plough sheared; a pinhole at 45° off its axis doubles the
 *   lengths that point away from the centre and leaves the others;
 * - it **reaches past a right angle**: a phone held upright with sixty degrees across is
 *   over a hundred and twenty tall, horizon to zenith on one screen, and a pinhole stops
 *   before ninety from its axis.
 *
 * **No roll.** The horizon is level through the middle of the screen and the zenith is
 * straight up from it, always: a reader standing in a field does not tilt their head. That
 * is what makes «up» defined at the zenith too: [right] depends on the bearing alone, so
 * looking straight up after facing south, the south horizon is at the foot of the screen
 * and the north one at its top, as for someone who leaned back.
 *
 * [azimuthDeg] is the bearing from north, clockwise, in `[0, 360)`; [altitudeDeg] the
 * height over the horizon, never over 90 (past the zenith is the other bearing); [fieldDeg]
 * the angle the viewport's **shorter side** takes, so turning the phone on its side does
 * not zoom.
 */
data class SkyCamera(
    val azimuthDeg: Double,
    val altitudeDeg: Double,
    val fieldDeg: Double = RestingFieldDeg
) {

    /** The camera's arithmetic for one viewport, computed once a frame ([SkyLens]). */
    fun lens(widthPx: Float, heightPx: Float): SkyLens = SkyLens(this, widthPx, heightPx)

    /**
     * The camera after a finger went from one point of the screen to another: **the sky
     * under the finger stays under the finger**. The direction that was at
     * ([fromX], [fromY]) is at ([toX], [toY]) afterwards, exactly, wherever the camera's
     * limits allow it.
     *
     * They do not always: a camera that does not roll cannot carry a star near the zenith
     * sideways further than its own distance from the zenith, and it stops at the floor
     * ([minAltitudeDeg]) and at straight up. There the finger slides over the sky, as a
     * hand does on a globe that has reached its stop.
     */
    fun dragged(fromX: Float, fromY: Float, toX: Float, toY: Float, widthPx: Float, heightPx: Float): SkyCamera {
        val sky = lens(widthPx, heightPx).toSky(fromX, fromY)
        return holding(sky, toX, toY, widthPx, heightPx)
    }

    /**
     * The camera [factor] times closer (over 1) or further (under 1), with the sky at
     * ([aboutX], [aboutY]) held where it is: two fingers pinch a constellation and it grows
     * between them, it does not slide off. Stops at [MinFieldDeg] and [MaxFieldDeg].
     */
    fun zoomed(factor: Float, aboutX: Float, aboutY: Float, widthPx: Float, heightPx: Float): SkyCamera {
        if (factor <= 0f || factor == 1f) return this
        val field = (fieldDeg / factor).coerceIn(MinFieldDeg, MaxFieldDeg)
        if (field == fieldDeg) return this
        val sky = lens(widthPx, heightPx).toSky(aboutX, aboutY)
        return copy(fieldDeg = field).holding(sky, aboutX, aboutY, widthPx, heightPx)
    }

    /** The camera turned by [degrees] to the right and raised by [upDegrees], inside its limits. */
    fun turned(degrees: Double, upDegrees: Double, widthPx: Float, heightPx: Float): SkyCamera = copy(
        azimuthDeg = norm360(azimuthDeg + degrees),
        altitudeDeg = (altitudeDeg + upDegrees).coerceIn(minAltitudeDeg(fieldDeg, widthPx, heightPx), MaxAltitudeDeg)
    )

    /**
     * This camera with [sky] at ([x], [y]). The closed form of a camera with no roll:
     * across, a direction's place depends on the bearing alone (`cos h · sin(A − az)`), so
     * the bearing comes first; then the altitude is one turn in the plane the bearing left.
     */
    private fun holding(sky: SkyDirection, x: Float, y: Float, widthPx: Float, heightPx: Float): SkyCamera {
        val lens = lens(widthPx, heightPx)
        // Where (x, y) points in the camera's own frame: right, up, forward.
        val px = (x - lens.centreX) / (2.0 * lens.focalPx)
        val py = -(y - lens.centreY) / (2.0 * lens.focalPx)
        val rho2 = px * px + py * py
        val cz = (1.0 - rho2) / (1.0 + rho2)
        val cx = px * (1.0 + cz)
        val cy = py * (1.0 + cz)

        val h = Math.toRadians(sky.altitudeDeg)
        val cosH = cos(h)
        val a = Math.toRadians(sky.azimuthDeg)
        // The bearing. A direction at the zenith has none to give: the camera keeps its own.
        val off = if (cosH < 1e-9) {
            0.0
        } else {
            val reach = asin((cx / cosH).coerceIn(-1.0, 1.0))
            // Two bearings put a direction that far across: the one that has it in front,
            // and the one that has it past the zenith. The camera stays on the side it is on.
            val before = Math.toRadians(signed180(sky.azimuthDeg - azimuthDeg))
            if (cos(before) >= 0.0) reach else Math.PI - reach
        }
        val azimuth = a - off
        // The altitude: (forward, up) is (u, v) turned by it.
        val u = cosH * cos(off)
        val v = sin(h)
        val altitude = Math.toDegrees(atan2(v, u) - atan2(cy, cz))
        return copy(
            azimuthDeg = norm360(Math.toDegrees(azimuth)),
            altitudeDeg = signed180(altitude).coerceIn(minAltitudeDeg(fieldDeg, widthPx, heightPx), MaxAltitudeDeg)
        )
    }

    companion object {
        /** Straight up, and no further: past it is the other bearing. */
        const val MaxAltitudeDeg = 90.0

        /**
         * The narrowest field, across the viewport's shorter side. The catalogue stops at
         * the sixth magnitude, the eye's own limit, and there are no fainter things in this
         * app to zoom onto (PLANNING, Fase 16: no deep-sky catalogues): at fifteen degrees
         * a phone still holds some sixty stars, the Pleiades fill a thumb, and closer than
         * that the screen would be a dozen dots and a great deal of nothing.
         */
        const val MinFieldDeg = 15.0

        /**
         * The widest: a right angle across the shorter side, which on a phone held upright
         * is 170° top to bottom, the whole sky over one's head from one horizon to the
         * other. Past it the corners are over 90° from the centre and the projection
         * stretches them more than twice.
         */
        const val MaxFieldDeg = 90.0

        /** The field the explorer opens at: the hero's own scale, about the same stars a dp. */
        const val RestingFieldDeg = 60.0

        /**
         * How far up the screen the horizon may come, as a share of the height from the
         * top: the lowest the camera looks, and where the explorer opens. Lower than this
         * it would be looking at the ground, which is a dark shape with nothing on it: at
         * 0.82 the ground is never more than the foot of the screen, where the page's own
         * words and buttons stand, and whatever is on the horizon is still on the screen.
         * (Seven tenths at first, by the arithmetic alone: at the widest field the ground
         * then took a third of the picture, seen on the first render, 11 nov 2026.)
         */
        const val HorizonLine = 0.82f

        /**
         * The lowest the camera looks, for a field on a viewport: the altitude that puts
         * the horizon at [HorizonLine] of the height. It depends on the field: a close one
         * comes down to ten degrees and still holds the horizon in its lower part, a wide
         * one looks sixty degrees up and holds the horizon and the zenith together. So
         * widening the field never lets the ground in: the horizon stays where it is and
         * the sky opens over it.
         */
        fun minAltitudeDeg(fieldDeg: Double, widthPx: Float, heightPx: Float): Double =
            angleBelowCentre((HorizonLine - 0.5f) * heightPx, focalPx(fieldDeg, widthPx, heightPx))
                .coerceAtMost(MaxAltitudeDeg)

        /**
         * The camera the explorer opens with: facing [azimuthDeg], at the resting field,
         * as low as it may look, so the horizon's line and the directions written on it
         * are the first thing that says which way is up.
         */
        fun resting(azimuthDeg: Double, widthPx: Float, heightPx: Float): SkyCamera = SkyCamera(
            azimuthDeg = norm360(azimuthDeg),
            altitudeDeg = minAltitudeDeg(RestingFieldDeg, widthPx, heightPx),
            fieldDeg = RestingFieldDeg
        )

        /**
         * The faintest star a field draws, as a magnitude: all of the catalogue up to the
         * resting field, then fewer as the field widens, so that the stars on a dp of
         * screen stay about as many. The count of stars triples a magnitude (5 080 to the
         * sixth, about 1 600 to the fifth) and a field twice as wide holds four times the
         * sky: `4·log10` of the ratio, 1.2 magnitudes for a doubling. At the widest field
         * it is 5.3. A zoom that only made the dust denser would hide the figures in it.
         */
        fun faintestMagnitude(fieldDeg: Double): Float {
            if (fieldDeg <= RestingFieldDeg) return CatalogueLimit
            return (CatalogueLimit - 4.0 * log10(fieldDeg / RestingFieldDeg)).toFloat()
        }

        /**
         * How much larger a light is drawn than at the resting field: a star is a point at
         * any zoom, but a screen that came three times closer and drew every dot the same
         * would not look closer. The fourth root of the zoom: 1.4 at the narrowest field,
         * 0.9 at the widest.
         */
        fun markScale(fieldDeg: Double): Float = sqrt(sqrt(RestingFieldDeg / fieldDeg)).toFloat()

        private const val CatalogueLimit = 6.0f

        /** The focal length: the shorter side takes [fieldDeg], `2·f·tan(θ/2)` each way. */
        internal fun focalPx(fieldDeg: Double, widthPx: Float, heightPx: Float): Double =
            min(widthPx, heightPx) / (4.0 * tan(Math.toRadians(fieldDeg) / 4.0))

        /** The angle a point [px] under the middle of the screen is from the centre, degrees. */
        private fun angleBelowCentre(px: Float, focalPx: Double): Double =
            Math.toDegrees(2.0 * atan(px / (2.0 * focalPx)))

        internal fun norm360(deg: Double): Double = ((deg % 360.0) + 360.0) % 360.0

        internal fun signed180(deg: Double): Double = ((deg % 360.0) + 540.0) % 360.0 - 180.0
    }
}

/** A direction of the local sky: the bearing from north, clockwise, and the height over the horizon. */
data class SkyDirection(val azimuthDeg: Double, val altitudeDeg: Double)

/**
 * A [SkyCamera] on a viewport: the three axes it sees along and its focal length, worked
 * out once, so that placing five thousand stars is five thousand dot products.
 *
 * Directions are unit vectors **east, north, up**, the frame the engine's altitude and
 * bearing are read in. The camera's axes in it: [right] is level, a quarter turn clockwise
 * of the bearing; forward is where it looks; up is what is left, and at the zenith it is
 * level too, pointing back over the reader's head.
 */
class SkyLens internal constructor(camera: SkyCamera, val widthPx: Float, val heightPx: Float) {
    val centreX: Float = widthPx / 2f
    val centreY: Float = heightPx / 2f
    val focalPx: Double = SkyCamera.focalPx(camera.fieldDeg, widthPx, heightPx)

    private val rightE: Double
    private val rightN: Double
    private val upE: Double
    private val upN: Double
    private val upU: Double
    private val forwardE: Double
    private val forwardN: Double
    private val forwardU: Double

    init {
        val az = Math.toRadians(camera.azimuthDeg)
        val alt = Math.toRadians(camera.altitudeDeg)
        rightE = cos(az)
        rightN = -sin(az)
        forwardE = cos(alt) * sin(az)
        forwardN = cos(alt) * cos(az)
        forwardU = sin(alt)
        upE = -sin(alt) * sin(az)
        upN = -sin(alt) * cos(az)
        upU = cos(alt)
    }

    /**
     * Where the direction ([east], [north], [up]) lands, written into [out] as x and y in
     * px; false, and [out] untouched, when it is too far behind the camera to have a place
     * ([BehindDeg]). On the screen or off it is the caller's to ask ([onScreen]).
     */
    fun toScreen(east: Double, north: Double, up: Double, out: FloatArray): Boolean {
        val cz = east * forwardE + north * forwardN + up * forwardU
        if (cz < BehindCos) return false
        val cx = east * rightE + north * rightN
        val cy = east * upE + north * upN + up * upU
        val k = 2.0 * focalPx / (1.0 + cz)
        out[0] = (centreX + k * cx).toFloat()
        out[1] = (centreY - k * cy).toFloat()
        return true
    }

    /** [toScreen] for a bearing and a height. */
    fun toScreen(azimuthDeg: Double, altitudeDeg: Double, out: FloatArray): Boolean {
        val az = Math.toRadians(azimuthDeg)
        val alt = Math.toRadians(altitudeDeg)
        return toScreen(cos(alt) * sin(az), cos(alt) * cos(az), sin(alt), out)
    }

    /** The direction of the sky at ([x], [y]): [toScreen] backwards. */
    fun toSky(x: Float, y: Float): SkyDirection {
        val px = (x - centreX) / (2.0 * focalPx)
        val py = -(y - centreY) / (2.0 * focalPx)
        val rho2 = px * px + py * py
        val cz = (1.0 - rho2) / (1.0 + rho2)
        val cx = px * (1.0 + cz)
        val cy = py * (1.0 + cz)
        val east = cx * rightE + cy * upE + cz * forwardE
        val north = cx * rightN + cy * upN + cz * forwardN
        val up = cy * upU + cz * forwardU
        return SkyDirection(
            azimuthDeg = SkyCamera.norm360(Math.toDegrees(atan2(east, north))),
            altitudeDeg = Math.toDegrees(asin(up.coerceIn(-1.0, 1.0)))
        )
    }

    /** Whether ([x], [y]) is on the viewport, or within [marginPx] of it. */
    fun onScreen(x: Float, y: Float, marginPx: Float = 0f): Boolean =
        x >= -marginPx && x <= widthPx + marginPx && y >= -marginPx && y <= heightPx + marginPx

    /**
     * The horizon on the screen. A stereographic projection draws every circle of the sky
     * as a circle, and the horizon is one: its centre straight above the middle of the
     * screen, `2·f·cot(alt)` up, its radius `2·f / sin(alt)`. The sky is inside it and the
     * ground outside, so with the camera low the line sags a little in the middle and
     * rises toward the sides, and with the camera straight up it is a ring round the
     * screen. Measured from the two places where the horizon crosses the line down the
     * middle of the screen: `alt` under the centre in front, `180° − alt` over it behind.
     */
    val horizon: HorizonCircle = run {
        val alt = Math.toRadians(camera.altitudeDeg.coerceIn(HorizonFloorDeg, SkyCamera.MaxAltitudeDeg))
        HorizonCircle(
            centreX = centreX,
            centreY = (centreY - 2.0 * focalPx * cos(alt) / sin(alt)).toFloat(),
            radius = (2.0 * focalPx / sin(alt)).toFloat()
        )
    }

    companion object {
        /** A direction this far from where the camera looks is behind it: its place runs off
         * to infinity at 180°, and nothing past 150° is ever near a screen. */
        const val BehindDeg = 150.0
        private val BehindCos = cos(Math.toRadians(BehindDeg))

        /** The horizon's circle is a straight line for a camera on the horizon; the camera
         * never is ([SkyCamera.minAltitudeDeg]), and this keeps the radius finite if it were. */
        private const val HorizonFloorDeg = 0.5
    }
}

/** The horizon as [SkyLens.horizon] draws it: a circle in px, the sky inside. */
data class HorizonCircle(val centreX: Float, val centreY: Float, val radius: Float)
