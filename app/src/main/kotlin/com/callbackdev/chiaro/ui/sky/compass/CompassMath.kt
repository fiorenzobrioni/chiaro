package com.callbackdev.chiaro.ui.sky.compass

import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * How the phone is being held, which decides what "where it points" means: lying flat
 * in front of the reader, the top edge of the screen points; held up as for a photo,
 * the back of the phone does — the camera's line of sight, which is where a person
 * standing up actually looks.
 */
internal enum class Posture { FLAT, UPRIGHT }

/**
 * Where the back of the phone points, the camera's line of sight (Fase 16, step 16.10):
 * the bearing in degrees clockwise from north, `[0, 360)`, and the height over the horizon
 * in degrees, from -90 (at the ground) to 90 (straight up).
 */
internal data class Aim(val azimuthDeg: Float, val altitudeDeg: Float)

/** Which way to turn, in the four words the compass speaks. */
internal enum class Guidance { AHEAD, LEFT, RIGHT, BEHIND }

/**
 * The arithmetic of the compass (14 ott 2026), pure so it can be tested without a
 * sensor: Android hands over a rotation matrix, everything else is here.
 */
internal object CompassMath {

    /**
     * The magnetic heading, degrees clockwise from north in `[0, 360)`, of the direction
     * the phone points.
     *
     * [r] is Android's rotation matrix, row-major 3×3, taking the device's own axes to
     * the world's (east, north, up) — what `SensorManager.getRotationMatrixFromVector`
     * and `getRotationMatrix` both fill. The direction is a device vector carried into the
     * world and read as an angle: the screen's top edge when [posture] is flat (which
     * device axis that is depends on how the screen is turned, [displayRotation] being
     * `Surface.ROTATION_*`), the back of the phone (−Z) when upright. Reading the vector
     * directly rather than through `getOrientation` is what keeps an upright phone from
     * spinning: the Euler angles are singular exactly where a person holds a camera.
     */
    fun heading(r: FloatArray, displayRotation: Int, posture: Posture): Float {
        val (vx, vy, vz) = when (posture) {
            Posture.UPRIGHT -> Triple(0f, 0f, -1f)
            Posture.FLAT -> when (displayRotation) {
                ROTATION_0 -> Triple(0f, 1f, 0f)
                ROTATION_90 -> Triple(-1f, 0f, 0f)
                ROTATION_180 -> Triple(0f, -1f, 0f)
                else -> Triple(1f, 0f, 0f)
            }
        }
        val east = r[0] * vx + r[1] * vy + r[2] * vz
        val north = r[3] * vx + r[4] * vy + r[5] * vz
        return normalize(Math.toDegrees(atan2(east.toDouble(), north.toDouble())).toFloat())
    }

    /**
     * Where the back of the phone points ([Aim], the bearing magnetic), for a sky that
     * follows the phone (Fase 16, step 16.10). Null where there is no bearing to give, and
     * the caller keeps the one it had.
     *
     * **The height** is the back's own: the up component of the device's −Z, read as an
     * angle. Nothing else enters it, so it is exact at any tilt.
     *
     * **The bearing** is the back's too, wherever the back has one: its level part, as
     * [heading] reads an upright phone. Straight up it has none (the level part is nothing,
     * and a degree of tremor would spin it round the compass), and there the sky's picture
     * still has to be some way round. So from sixty degrees of height the bearing slides
     * from the back's to **the screen's own top edge, turned round**: a reader who faces
     * north and leans the phone back until it is over their head has its top edge pointing
     * south, behind them, and is still facing north. At the zenith that puts the sky on the
     * screen the way the screen lies under it. Under the horizon the same, mirrored: a
     * phone lying flat, screen up, points where its top edge points, as the compass reads
     * it. Up to sixty degrees a phone tilted sideways changes nothing: the back points
     * where it points.
     *
     * Past the zenith the two disagree, as they must: a reader who leans back past straight
     * up is looking at the sky behind them, and the bearing turns round. Where they cancel
     * there is no answer, and none is given.
     */
    fun aim(r: FloatArray, displayRotation: Int): Aim? {
        val up = (-r[8]).coerceIn(-1f, 1f)
        val (ux, uy) = when (displayRotation) {
            ROTATION_0 -> 0f to 1f
            ROTATION_90 -> -1f to 0f
            ROTATION_180 -> 0f to -1f
            else -> 1f to 0f
        }
        // The screen's top edge, its level part: turned round over the horizon, as it is under it.
        val side = if (up >= 0f) -1f else 1f
        val topEast = side * (r[0] * ux + r[1] * uy)
        val topNorth = side * (r[3] * ux + r[4] * uy)
        val t = ((abs(up) - STEEP_FROM) / (1f - STEEP_FROM)).coerceIn(0f, 1f)
        val steep = t * t * (3f - 2f * t)
        val east = (1f - steep) * -r[2] + steep * topEast
        val north = (1f - steep) * -r[5] + steep * topNorth
        if (hypot(east, north) < NO_BEARING) return null
        return Aim(
            azimuthDeg = normalize(Math.toDegrees(atan2(east.toDouble(), north.toDouble())).toFloat()),
            altitudeDeg = Math.toDegrees(asin(up.toDouble())).toFloat()
        )
    }

    /** How far apart two aims are on the sky, degrees: across by the height's cosine, so
     * ten degrees of bearing near the zenith are the little they are. */
    fun apart(a: Aim, b: Aim): Float {
        val level = cos(Math.toRadians(((a.altitudeDeg + b.altitudeDeg) / 2f).toDouble())).toFloat()
        return hypot(turn(a.azimuthDeg, b.azimuthDeg) * level, b.altitudeDeg - a.altitudeDeg)
    }

    /**
     * Flat or upright, from how far the screen faces the sky ([r]`[8]`, the up component
     * of the device's own Z), with a gap between the two thresholds so a phone held at
     * the boundary does not flick its heading back and forth between two directions.
     */
    fun posture(previous: Posture, r: FloatArray): Posture {
        val facingUp = abs(r[8])
        return when (previous) {
            Posture.FLAT -> if (facingUp < UPRIGHT_BELOW) Posture.UPRIGHT else Posture.FLAT
            Posture.UPRIGHT -> if (facingUp > FLAT_ABOVE) Posture.FLAT else Posture.UPRIGHT
        }
    }

    /** The turn from [heading] to [target], degrees in `(-180, 180]`: positive is right. */
    fun turn(heading: Float, target: Float): Float {
        val d = normalize(target - heading)
        return if (d > 180f) d - 360f else d
    }

    /**
     * The word for a turn, with hysteresis on «ahead»: the needle of a hand-held phone
     * wanders a few degrees, and a word that flickers between «there» and «turn right»
     * reads as a broken compass rather than a steady hand.
     */
    fun guidance(previous: Guidance?, turn: Float): Guidance {
        val off = abs(turn)
        val aheadWithin = if (previous == Guidance.AHEAD) AHEAD_EXIT else AHEAD_ENTER
        return when {
            off <= aheadWithin -> Guidance.AHEAD
            off > BEHIND_FROM -> Guidance.BEHIND
            turn > 0f -> Guidance.RIGHT
            else -> Guidance.LEFT
        }
    }

    /**
     * Whether the magnetometer is reading something other than the earth. The earth's
     * field is between about 25 and 65 µT everywhere on its surface; outside a margin of
     * that, a car, a railing, a magnetic case or a speaker is what the compass is
     * pointing at, and no calibration will fix it — the reader has to move.
     */
    fun disturbed(microtesla: Float): Boolean = microtesla < FIELD_MIN_UT || microtesla > FIELD_MAX_UT

    /**
     * Whether to ask for a calibration, from the sensor's own accuracy
     * (`SensorManager.SENSOR_STATUS_*`): unreliable and low ask, medium and high do not.
     * Honest about its limit: phones report this differently, and some never report
     * low at all, which is why the compass also offers the gesture on request.
     */
    fun needsCalibration(accuracy: Int): Boolean = accuracy in 0..1

    /** A low-pass over the direction as a unit vector, so 359° and 1° average to north
     * rather than to south. Returns the new (sin, cos). */
    fun smooth(previous: Pair<Float, Float>?, headingDeg: Float, alpha: Float = SMOOTHING): Pair<Float, Float> {
        val rad = Math.toRadians(headingDeg.toDouble())
        val s = kotlin.math.sin(rad).toFloat()
        val c = kotlin.math.cos(rad).toFloat()
        if (previous == null) return s to c
        return (previous.first + alpha * (s - previous.first)) to (previous.second + alpha * (c - previous.second))
    }

    fun angleOf(sinCos: Pair<Float, Float>): Float =
        normalize(Math.toDegrees(atan2(sinCos.first.toDouble(), sinCos.second.toDouble())).toFloat())

    fun normalize(degrees: Float): Float = ((degrees % 360f) + 360f) % 360f

    // Surface.ROTATION_*, restated so this file needs no Android.
    const val ROTATION_0 = 0
    const val ROTATION_90 = 1
    const val ROTATION_180 = 2

    /** |up| of the screen's normal: under 0.4 (~66° from flat) is upright, over 0.6 (~53°) flat again. */
    private const val UPRIGHT_BELOW = 0.4f
    private const val FLAT_ABOVE = 0.6f

    /** Within 12° is «there» (a hand at arm's length covers about 20°); it takes 18 to leave. */
    private const val AHEAD_ENTER = 12f
    private const val AHEAD_EXIT = 18f
    private const val BEHIND_FROM = 150f

    private const val FIELD_MIN_UT = 20f
    private const val FIELD_MAX_UT = 70f

    /** At the sensor's ~50 Hz, a time constant of about a fifth of a second. */
    private const val SMOOTHING = 0.12f

    /** sin 60°: from there up (and from there down) the bearing slides to the top edge's. */
    private const val STEEP_FROM = 0.8660254f

    /** A level part shorter than this, of a unit vector, has no direction worth reading. */
    private const val NO_BEARING = 0.05f
}

/**
 * A hand-held phone's aim, made steady enough to hang a sky on (Fase 16, step 16.10).
 *
 * The compass's needle may wander a degree and still read; a sky drawn through the phone
 * may not: at the explorer's resting field a degree is some eighteen pixels on a phone, and
 * five thousand stars that shake by a few of them are a sky that trembles. Two things, both
 * on the readings as they arrive (about fifty a second):
 *
 * - **A low-pass that listens to how far the phone has moved.** Within half a degree of
 *   where the sky stands the new reading counts for a twenty-fifth (a time constant of half
 *   a second: the hand's tremor averages out); from five degrees off it counts for a third
 *   (a sixteenth of a second: the sky keeps up with an arm that swings). Between the two,
 *   in proportion. A fixed weight would have to choose between a sky that shakes and one
 *   that swims behind the phone.
 * - **A step under which nothing is said**: the aim handed back is the last one until the
 *   steadied one has moved a twentieth of a degree from it, a pixel at the resting field. A
 *   phone on a table, or in a still hand, gives the same aim again and again, and a page
 *   that draws when its camera changes draws nothing.
 *
 * The numbers are arithmetic, not yet a phone's verdict: PLANNING, «16.10».
 */
internal class SteadyAim {
    private var east = 0f
    private var north = 1f
    private var altitude = 0f
    private var said: Aim? = null

    /** The aim to show after the reading [raw]: the same instance as last time when the
     * phone has not moved enough to say so. */
    fun next(raw: Aim): Aim {
        val rad = Math.toRadians(raw.azimuthDeg.toDouble())
        val last = said
        if (last == null) {
            east = sin(rad).toFloat()
            north = cos(rad).toFloat()
            altitude = raw.altitudeDeg
            return raw.also { said = it }
        }
        val off = CompassMath.apart(steadied(), raw)
        val weight = STILL_WEIGHT +
            (MOVING_WEIGHT - STILL_WEIGHT) * ((off - STILL_DEG) / (MOVING_DEG - STILL_DEG)).coerceIn(0f, 1f)
        // The bearing as a unit vector, so 359° and 1° meet at north and not at south.
        east += weight * (sin(rad).toFloat() - east)
        north += weight * (cos(rad).toFloat() - north)
        altitude += weight * (raw.altitudeDeg - altitude)
        val now = steadied()
        return if (CompassMath.apart(last, now) >= STEP_DEG) now.also { said = it } else last
    }

    private fun steadied() = Aim(CompassMath.angleOf(east to north), altitude)

    private companion object {
        const val STILL_DEG = 0.5f
        const val MOVING_DEG = 5f
        const val STILL_WEIGHT = 0.04f
        const val MOVING_WEIGHT = 0.35f
        const val STEP_DEG = 0.05f
    }
}
