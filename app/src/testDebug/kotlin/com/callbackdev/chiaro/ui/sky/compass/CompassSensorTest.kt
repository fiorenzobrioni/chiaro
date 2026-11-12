package com.callbackdev.chiaro.ui.sky.compass

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.activity.ComponentActivity
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowSensor
import org.robolectric.shadows.ShadowSensorManager

/**
 * The compass as the screens hold it (DESIGN §8.8c, and Fase 16, step 16.10): **when** it
 * listens, which is the battery's rule, and what it says to a page that asked it to aim.
 * The arithmetic is `CompassMathTest`'s; this is the wiring, on Robolectric's sensors.
 */
@RunWith(RobolectricTestRunner::class)
class CompassSensorTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    // The activity's own manager: a system service is one per context.
    private val manager get() = compose.activity.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensors get() = shadowOf(manager)

    private var enabled by mutableStateOf(false)
    private lateinit var compass: State<CompassState>

    private fun aPhoneWithACompass() {
        sensors.addSensor(ShadowSensor.newInstance(Sensor.TYPE_MAGNETIC_FIELD))
        sensors.addSensor(ShadowSensor.newInstance(Sensor.TYPE_ROTATION_VECTOR))
    }

    private fun show(aim: Boolean) {
        compose.setContent { compass = rememberCompass(coordinates = null, enabled = enabled, aim = aim) }
        compose.waitForIdle()
    }

    /** One reading of the fused sensor: the phone turned from lying flat, top to the north,
     * by [liftDeg] about its own X (90 is upright facing north, more is leaning back). */
    private fun lifted(liftDeg: Double) {
        val half = Math.toRadians(liftDeg) / 2.0
        val event = ShadowSensorManager.createSensorEvent(4, Sensor.TYPE_ROTATION_VECTOR)
        event.values[0] = kotlin.math.sin(half).toFloat()
        event.values[1] = 0f
        event.values[2] = 0f
        event.values[3] = kotlin.math.cos(half).toFloat()
        compose.runOnUiThread { sensors.sendSensorEventToListeners(event) }
        compose.waitForIdle()
    }

    @Test
    fun `a phone with no magnetometer has no compass`() {
        show(aim = true)
        assertFalse(compass.value.supported)
    }

    @Test
    fun `it listens while somebody reads it and the page is in front, and at no other time`() {
        aPhoneWithACompass()
        show(aim = true)
        assertTrue("the phone has one, read or not", compass.value.supported)
        assertTrue("nobody is reading: nothing is registered", sensors.listeners.isEmpty())

        enabled = true
        compose.waitForIdle()
        assertEquals("one listener, on the two sensors", 1, sensors.listeners.size)

        // The app goes behind another: the sensors are let go, and taken again on the way back.
        compose.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
        assertTrue(sensors.listeners.isEmpty())
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        assertEquals(1, sensors.listeners.size)

        enabled = false
        compose.waitForIdle()
        assertTrue("the switch went off", sensors.listeners.isEmpty())
    }

    @Test
    fun `asked to aim, it says where the back of the phone points and how high`() {
        aPhoneWithACompass()
        enabled = true
        show(aim = true)
        assertNull("nothing before the first reading", compass.value.heading)
        assertNull(compass.value.altitude)

        // Upright facing north, leaned back thirty degrees.
        lifted(120.0)
        val heading = compass.value.heading!!
        assertTrue("facing north, was $heading", heading < 0.1f || heading > 359.9f)
        assertEquals(30f, compass.value.altitude!!, 0.05f)

        // Let go and taken again: the old reading is not where the phone points now.
        enabled = false
        compose.waitForIdle()
        assertNull(compass.value.heading)
        assertNull(compass.value.altitude)
    }

    @Test
    fun `not asked, it is the compass it was, a heading and no height`() {
        aPhoneWithACompass()
        enabled = true
        show(aim = false)
        lifted(90.0)
        val heading = compass.value.heading!!
        assertTrue("facing north, was $heading", heading < 0.1f || heading > 359.9f)
        assertNull(compass.value.altitude)
    }
}
