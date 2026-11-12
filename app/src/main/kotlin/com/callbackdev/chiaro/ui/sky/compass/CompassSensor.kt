package com.callbackdev.chiaro.ui.sky.compass

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.callbackdev.chiaro.domain.model.Coordinates
import kotlin.math.sqrt

/**
 * What the compass knows at this instant.
 *
 * [heading] is degrees from TRUE north (the sky's bearings are true, a magnetometer's
 * are not: in Italy the two differ by 2 to 5 degrees, elsewhere by tens), null until the
 * first reading arrives.
 *
 * [altitude] is the height the back of the phone points at, degrees over the horizon
 * (Fase 16, step 16.10), for a compass asked to aim; null for one that only reads a
 * heading, and until the first reading arrives.
 */
@Immutable
internal data class CompassState(
    val supported: Boolean,
    val heading: Float? = null,
    val altitude: Float? = null,
    val needsCalibration: Boolean = false,
    val disturbed: Boolean = false
)

/**
 * The phone's compass, while the composable that asks for it is on screen and the app is
 * in front (DESIGN §8.8c): registered on resume, released on pause, so it costs nothing
 * the rest of the time — no background sensor, the battery rule of CLAUDE.md.
 *
 * The fused rotation vector where the phone has one (it is the steadier needle), the
 * accelerometer and the magnetometer combined where it does not; the magnetometer is
 * listened to either way, because its accuracy is the calibration flag and its strength
 * says whether something nearby is pulling the needle. A phone with no magnetometer has
 * no compass, and says so.
 *
 * [enabled] false is a compass nobody is reading (the explorer with «Point the phone» off):
 * nothing is registered, and the state still says whether the phone has one.
 *
 * [aim] true is the explorer's compass (step 16.10): [CompassState.heading] is then the
 * bearing of the back of the phone at any tilt and [CompassState.altitude] its height
 * ([CompassMath.aim]), both steadied for a sky drawn through them ([SteadyAim]).
 */
@Composable
internal fun rememberCompass(
    coordinates: Coordinates?,
    enabled: Boolean = true,
    aim: Boolean = false
): State<CompassState> {
    val context = LocalContext.current
    val view = LocalView.current
    val manager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val magnetometer = remember { manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) }
    val rotation = remember { manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) }
    val accelerometer = remember { manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }
    // A magnetometer alone gives a field, not a heading: it needs the fused rotation or
    // the accelerometer's gravity to know which way is down.
    val state = remember {
        mutableStateOf(CompassState(supported = magnetometer != null && (rotation != null || accelerometer != null)))
    }
    // Where magnetic north is from true north here, for the place the bearings are for.
    val declination = remember(coordinates) {
        coordinates?.let {
            GeomagneticField(it.lat.toFloat(), it.lon.toFloat(), 0f, System.currentTimeMillis()).declination
        } ?: 0f
    }

    LifecycleResumeEffect(manager, declination, enabled, aim) {
        val listener = object : SensorEventListener {
            val matrix = FloatArray(9)
            val gravity = FloatArray(3)
            val field = FloatArray(3)
            var haveGravity = false
            var haveField = false
            var posture = Posture.FLAT
            var smoothed: Pair<Float, Float>? = null
            val steady = SteadyAim()
            var accuracy = SensorManager.SENSOR_STATUS_ACCURACY_HIGH
            // A disturbance has to last before it is said: one spike as the phone passes
            // a zip is not a reason to send anybody walking.
            var disturbedSinceNs = 0L

            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        System.arraycopy(event.values, 0, field, 0, 3)
                        haveField = true
                        accuracy = event.accuracy
                        val strength = sqrt(field[0] * field[0] + field[1] * field[1] + field[2] * field[2])
                        disturbedSinceNs = when {
                            !CompassMath.disturbed(strength) -> 0L
                            disturbedSinceNs == 0L -> event.timestamp
                            else -> disturbedSinceNs
                        }
                        if (rotation == null) updateFromPair() else publish(null, null)
                    }
                    Sensor.TYPE_ACCELEROMETER -> {
                        System.arraycopy(event.values, 0, gravity, 0, 3)
                        haveGravity = true
                        updateFromPair()
                    }
                    Sensor.TYPE_ROTATION_VECTOR -> {
                        SensorManager.getRotationMatrixFromVector(matrix, event.values)
                        read(matrix)
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor, newAccuracy: Int) {
                if (sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                    accuracy = newAccuracy
                    publish(null, null)
                }
            }

            fun updateFromPair() {
                if (!haveGravity || !haveField) return
                if (SensorManager.getRotationMatrix(matrix, null, gravity, field)) read(matrix)
            }

            fun read(r: FloatArray) {
                if (!aim) return publish(headingOf(r), null)
                // No bearing to read (the two that make it cancel, past the zenith): the
                // sky stays where the last reading left it.
                val magnetic = CompassMath.aim(r, view.display?.rotation ?: 0) ?: return publish(null, null)
                val shown = steady.next(magnetic.copy(azimuthDeg = CompassMath.normalize(magnetic.azimuthDeg + declination)))
                publish(shown.azimuthDeg, shown.altitudeDeg)
            }

            fun headingOf(r: FloatArray): Float {
                posture = CompassMath.posture(posture, r)
                val magnetic = CompassMath.heading(r, view.display?.rotation ?: 0, posture)
                smoothed = CompassMath.smooth(smoothed, CompassMath.normalize(magnetic + declination))
                return CompassMath.angleOf(smoothed!!)
            }

            fun publish(heading: Float?, altitude: Float?) {
                val now = SystemClock.elapsedRealtimeNanos()
                val disturbed = disturbedSinceNs != 0L &&
                    now - disturbedSinceNs > DisturbanceLastsNs
                val next = state.value.copy(
                    heading = heading ?: state.value.heading,
                    altitude = altitude ?: state.value.altitude,
                    needsCalibration = CompassMath.needsCalibration(accuracy),
                    disturbed = disturbed
                )
                if (next != state.value) state.value = next
            }
        }
        if (enabled && state.value.supported) {
            manager.registerListener(listener, magnetometer!!, SensorManager.SENSOR_DELAY_GAME)
            if (rotation != null) {
                manager.registerListener(listener, rotation, SensorManager.SENSOR_DELAY_GAME)
            } else if (accelerometer != null) {
                manager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
            }
        }
        onPauseOrDispose {
            manager.unregisterListener(listener)
            // A reading from before is not where the phone points now: the next one to
            // listen starts from none, as the first did.
            state.value = state.value.copy(heading = null, altitude = null)
        }
    }
    return state
}

/** A second and a half. Measured against `elapsedRealtimeNanos`, the clock sensor
 * timestamps are on (not `System.nanoTime`, which stops while the phone sleeps). */
private const val DisturbanceLastsNs = 1_500_000_000L
