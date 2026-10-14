package com.callbackdev.chiaro.ui.sky.compass

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The compass's arithmetic (14 ott 2026). The matrices are Android's convention: row-major,
 * device axes to world (east, north, up); a matrix's COLUMNS are where the device's X, Y
 * and Z end up.
 */
class CompassMathTest {

    /** Lying flat, screen up, top edge to the north. */
    private val flatNorth = floatArrayOf(
        1f, 0f, 0f,
        0f, 1f, 0f,
        0f, 0f, 1f
    )

    /** Lying flat, top edge to the east: device Y is world east, device X world south. */
    private val flatEast = floatArrayOf(
        0f, 1f, 0f,
        -1f, 0f, 0f,
        0f, 0f, 1f
    )

    /** Held up as for a photo, facing north: device Y up, the screen (Z) toward the reader
     * standing south of it, so the camera (−Z) looks north. */
    private val uprightNorth = floatArrayOf(
        1f, 0f, 0f,
        0f, 0f, -1f,
        0f, 1f, 0f
    )

    @Test
    fun `a flat phone points where the top of its screen points`() {
        assertEquals(0f, CompassMath.heading(flatNorth, CompassMath.ROTATION_0, Posture.FLAT), 1e-4f)
        assertEquals(90f, CompassMath.heading(flatEast, CompassMath.ROTATION_0, Posture.FLAT), 1e-4f)
    }

    @Test
    fun `a turned screen moves the top edge to another side of the device`() {
        // ROTATION_90: the screen's top is the device's left edge, −X: west here.
        assertEquals(270f, CompassMath.heading(flatNorth, CompassMath.ROTATION_90, Posture.FLAT), 1e-4f)
        assertEquals(180f, CompassMath.heading(flatNorth, CompassMath.ROTATION_180, Posture.FLAT), 1e-4f)
        assertEquals(90f, CompassMath.heading(flatNorth, 3, Posture.FLAT), 1e-4f)
    }

    @Test
    fun `an upright phone points where its camera looks, whichever way the screen is turned`() {
        assertEquals(0f, CompassMath.heading(uprightNorth, CompassMath.ROTATION_0, Posture.UPRIGHT), 1e-4f)
        assertEquals(0f, CompassMath.heading(uprightNorth, CompassMath.ROTATION_90, Posture.UPRIGHT), 1e-4f)
    }

    @Test
    fun `the posture changes with a gap between the two thresholds`() {
        assertEquals(Posture.UPRIGHT, CompassMath.posture(Posture.FLAT, uprightNorth))
        assertEquals(Posture.FLAT, CompassMath.posture(Posture.UPRIGHT, flatNorth))
        // Halfway (|up| 0.5): each posture keeps itself.
        val half = floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0.5f)
        assertEquals(Posture.FLAT, CompassMath.posture(Posture.FLAT, half))
        assertEquals(Posture.UPRIGHT, CompassMath.posture(Posture.UPRIGHT, half))
    }

    @Test
    fun `a turn is the short way round, right positive`() {
        assertEquals(20f, CompassMath.turn(350f, 10f), 1e-4f)
        assertEquals(-20f, CompassMath.turn(10f, 350f), 1e-4f)
        assertEquals(180f, CompassMath.turn(0f, 180f), 1e-4f)
    }

    @Test
    fun `ahead is easier to stay in than to enter, and far round is behind`() {
        assertEquals(Guidance.RIGHT, CompassMath.guidance(null, 15f))
        assertEquals(Guidance.AHEAD, CompassMath.guidance(Guidance.AHEAD, 15f))
        assertEquals(Guidance.LEFT, CompassMath.guidance(Guidance.AHEAD, -25f))
        assertEquals(Guidance.AHEAD, CompassMath.guidance(Guidance.RIGHT, 10f))
        assertEquals(Guidance.BEHIND, CompassMath.guidance(null, -160f))
    }

    @Test
    fun `only a field the earth cannot make counts as a disturbance`() {
        assertFalse(CompassMath.disturbed(25f))
        assertFalse(CompassMath.disturbed(65f))
        assertTrue(CompassMath.disturbed(8f))
        assertTrue(CompassMath.disturbed(140f))
    }

    @Test
    fun `unreliable and low ask for the gesture, medium and high and no-contact do not`() {
        assertTrue(CompassMath.needsCalibration(0))
        assertTrue(CompassMath.needsCalibration(1))
        assertFalse(CompassMath.needsCalibration(2))
        assertFalse(CompassMath.needsCalibration(3))
        assertFalse(CompassMath.needsCalibration(-1))
    }

    @Test
    fun `smoothing across north stays at north`() {
        var s = CompassMath.smooth(null, 359f)
        repeat(20) { s = CompassMath.smooth(s, 1f) }
        val angle = CompassMath.angleOf(s)
        assertTrue("was $angle", angle < 2f || angle > 358f)
    }
}
