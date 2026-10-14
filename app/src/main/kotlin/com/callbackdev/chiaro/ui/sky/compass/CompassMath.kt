package com.callbackdev.chiaro.ui.sky.compass

import kotlin.math.abs
import kotlin.math.atan2

/**
 * How the phone is being held, which decides what "where it points" means: lying flat
 * in front of the reader, the top edge of the screen points; held up as for a photo,
 * the back of the phone does — the camera's line of sight, which is where a person
 * standing up actually looks.
 */
internal enum class Posture { FLAT, UPRIGHT }

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
}
