package com.callbackdev.chiaro.ui.sky.compass

import kotlin.math.cos
import kotlin.math.sin
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
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

    // Where the back of the phone points (Fase 16, step 16.10).

    /**
     * A phone that started lying flat, top edge to the north, then: rolled by [rollDeg]
     * about its own line of sight, lifted by [liftDeg] about its own X (90 is upright,
     * 180 over the reader's head with the screen down), and turned as a whole to face
     * [faceDeg]. The matrix Android would hand over for it.
     */
    private fun held(faceDeg: Double, liftDeg: Double, rollDeg: Double = 0.0): FloatArray {
        val z = Math.toRadians(-faceDeg)
        val x = Math.toRadians(liftDeg)
        val r = Math.toRadians(rollDeg)
        val turn = arrayOf(
            doubleArrayOf(cos(z), -sin(z), 0.0),
            doubleArrayOf(sin(z), cos(z), 0.0),
            doubleArrayOf(0.0, 0.0, 1.0)
        )
        val lift = arrayOf(
            doubleArrayOf(1.0, 0.0, 0.0),
            doubleArrayOf(0.0, cos(x), -sin(x)),
            doubleArrayOf(0.0, sin(x), cos(x))
        )
        val roll = arrayOf(
            doubleArrayOf(cos(r), -sin(r), 0.0),
            doubleArrayOf(sin(r), cos(r), 0.0),
            doubleArrayOf(0.0, 0.0, 1.0)
        )
        fun times(a: Array<DoubleArray>, b: Array<DoubleArray>) =
            Array(3) { i -> DoubleArray(3) { j -> (0..2).sumOf { k -> a[i][k] * b[k][j] } } }
        val m = times(times(turn, lift), roll)
        return FloatArray(9) { m[it / 3][it % 3].toFloat() }
    }

    private fun assertBearing(expected: Float, actual: Float, within: Float = 0.01f) {
        assertTrue("bearing was $actual, not $expected", kotlin.math.abs(CompassMath.turn(expected, actual)) <= within)
    }

    @Test
    fun `the helper builds the matrices this file already trusts`() {
        assertArrayEquals(uprightNorth, held(faceDeg = 0.0, liftDeg = 90.0), 1e-6f)
        assertArrayEquals(flatEast, held(faceDeg = 90.0, liftDeg = 0.0), 1e-6f)
    }

    @Test
    fun `an upright phone aims at the horizon it faces`() {
        val north = CompassMath.aim(uprightNorth, CompassMath.ROTATION_0)!!
        assertBearing(0f, north.azimuthDeg)
        assertEquals(0f, north.altitudeDeg, 1e-3f)
        val southWest = CompassMath.aim(held(faceDeg = 225.0, liftDeg = 90.0), CompassMath.ROTATION_0)!!
        assertBearing(225f, southWest.azimuthDeg)
        assertEquals(0f, southWest.altitudeDeg, 1e-3f)
    }

    @Test
    fun `leaning the phone back raises the aim and leaves the bearing alone, all the way overhead`() {
        // From upright (90) to over the head, screen down (180), facing east the whole way.
        for (lift in 90..180 step 5) {
            val aim = CompassMath.aim(held(faceDeg = 90.0, liftDeg = lift.toDouble()), CompassMath.ROTATION_0)!!
            assertBearing(90f, aim.azimuthDeg)
            assertEquals("lifted $lift", lift - 90f, aim.altitudeDeg, 1e-2f)
        }
    }

    @Test
    fun `a phone tilted sideways aims where its back points, up to sixty degrees`() {
        for (alt in listOf(0, 20, 40, 60)) {
            for (roll in listOf(-30.0, 15.0, 90.0)) {
                val aim = CompassMath.aim(held(faceDeg = 200.0, liftDeg = 90.0 + alt, rollDeg = roll), CompassMath.ROTATION_0)!!
                assertBearing(200f, aim.azimuthDeg)
                assertEquals(alt.toFloat(), aim.altitudeDeg, 1e-2f)
            }
        }
    }

    @Test
    fun `straight up the bearing is the one that puts the sky on the screen the way the screen lies`() {
        // Over the head, screen down, the top edge to the south: the reader faces north, and
        // a camera that looks up facing north has the north horizon behind its top edge...
        val overhead = CompassMath.aim(held(faceDeg = 0.0, liftDeg = 180.0), CompassMath.ROTATION_0)!!
        assertBearing(0f, overhead.azimuthDeg)
        assertEquals(90f, overhead.altitudeDeg, 1e-3f)
        // ...and the same phone turned a quarter on itself, top edge to the west, faces east.
        val quarter = CompassMath.aim(held(faceDeg = 0.0, liftDeg = 180.0, rollDeg = 90.0), CompassMath.ROTATION_0)!!
        assertBearing(90f, quarter.azimuthDeg)
        // The screen turned on its side: its top is another edge of the device, the left one.
        val turned = CompassMath.aim(held(faceDeg = 0.0, liftDeg = 180.0), CompassMath.ROTATION_90)!!
        assertBearing(90f, turned.azimuthDeg)
    }

    @Test
    fun `a degree off the zenith does not spin the bearing round the compass`() {
        // The back a degree off straight up, towards each side in turn: the back's own
        // bearing goes round the compass, the aim's stays with the screen.
        val a = CompassMath.aim(held(faceDeg = 0.0, liftDeg = 179.0), CompassMath.ROTATION_0)!!
        val b = CompassMath.aim(held(faceDeg = 0.0, liftDeg = 181.0), CompassMath.ROTATION_0)!!
        assertBearing(0f, a.azimuthDeg)
        assertBearing(0f, b.azimuthDeg)
        assertEquals(89f, a.altitudeDeg, 1e-2f)
        assertEquals(89f, b.altitudeDeg, 1e-2f)
    }

    @Test
    fun `leaning back far past the zenith is looking at the sky behind`() {
        val behind = CompassMath.aim(held(faceDeg = 0.0, liftDeg = 240.0), CompassMath.ROTATION_0)!!
        assertBearing(180f, behind.azimuthDeg)
        assertEquals(30f, behind.altitudeDeg, 1e-2f)
    }

    @Test
    fun `a phone lying flat aims at the ground, where its top edge points`() {
        val flat = CompassMath.aim(flatEast, CompassMath.ROTATION_0)!!
        assertBearing(90f, flat.azimuthDeg)
        assertEquals(-90f, flat.altitudeDeg, 1e-3f)
        // Lifted a little off the table it still points there, and less far down.
        val lifted = CompassMath.aim(held(faceDeg = 90.0, liftDeg = 20.0), CompassMath.ROTATION_0)!!
        assertBearing(90f, lifted.azimuthDeg)
        assertEquals(-70f, lifted.altitudeDeg, 1e-2f)
    }

    @Test
    fun `where the two bearings cancel there is none`() {
        // Past the zenith the back's bearing and the screen's are opposite, and the slide
        // from one to the other goes through nothing, a degree or so of it.
        val none = (200..260).count { lift ->
            (0..9).any { tenth -> CompassMath.aim(held(0.0, lift + tenth / 10.0), CompassMath.ROTATION_0) == null }
        }
        assertTrue("no bearing in $none of the degrees past the zenith", none in 1..6)
    }

    @Test
    fun `how far apart two aims are counts the bearing by the height`() {
        assertEquals(10f, CompassMath.apart(Aim(350f, 0f), Aim(0f, 0f)), 1e-3f)
        assertEquals(5f, CompassMath.apart(Aim(100f, 60f), Aim(110f, 60f)), 1e-3f)
        assertEquals(5f, CompassMath.apart(Aim(100f, 20f), Aim(100f, 25f)), 1e-3f)
    }

    // The aim, steadied.

    /** A hand's tremor, the same every run: up to [reach] degrees each way on both axes. */
    private fun trembling(around: Aim, reach: Float, samples: Int): List<Aim> {
        val random = java.util.Random(16_10)
        return List(samples) {
            Aim(
                CompassMath.normalize(around.azimuthDeg + (random.nextFloat() * 2f - 1f) * reach),
                around.altitudeDeg + (random.nextFloat() * 2f - 1f) * reach
            )
        }
    }

    @Test
    fun `the first reading is the aim`() {
        assertEquals(Aim(123f, 45f), SteadyAim().next(Aim(123f, 45f)))
    }

    @Test
    fun `a still hand holds the sky still`() {
        val truth = Aim(120f, 40f)
        val steady = SteadyAim()
        steady.next(truth)
        // Ten seconds of a hand that trembles a third of a degree each way.
        val said = trembling(truth, reach = 0.33f, samples = 500).map(steady::next)
        val worst = said.maxOf { CompassMath.apart(truth, it) }
        assertTrue("the sky strayed $worst degrees", worst < 0.2f)
        val moves = said.zipWithNext().count { (a, b) -> a !== b }
        // The raw readings would have moved it five hundred times.
        assertTrue("the sky moved $moves times in ten seconds", moves < 50)
    }

    @Test
    fun `a phone on a table moves nothing`() {
        val steady = SteadyAim()
        val first = steady.next(Aim(10f, 5f))
        repeat(200) { assertSame(first, steady.next(Aim(10f, 5f))) }
    }

    @Test
    fun `a swinging arm is followed closely, and the sky arrives when the arm stops`() {
        val steady = SteadyAim()
        var said = steady.next(Aim(0f, 20f))
        // Sixty degrees a second for a second and a half, at fifty readings a second.
        var worst = 0f
        for (i in 1..75) {
            val raw = Aim(i * 1.2f, 20f + i * 0.2f)
            said = steady.next(raw)
            worst = maxOf(worst, CompassMath.apart(said, raw))
        }
        assertTrue("the sky was $worst degrees behind the phone", worst < 6f)
        val rest = Aim(90f, 35f)
        repeat(100) { said = steady.next(rest) }
        assertTrue("two seconds on it is ${CompassMath.apart(said, rest)} off", CompassMath.apart(said, rest) < 0.15f)
    }

    @Test
    fun `turning across north goes through north`() {
        val steady = SteadyAim()
        steady.next(Aim(350f, 30f))
        for (i in 1..40) {
            val said = steady.next(Aim(CompassMath.normalize(350f + i * 0.5f), 30f))
            assertTrue("was ${said.azimuthDeg}", said.azimuthDeg >= 349f || said.azimuthDeg <= 11f)
        }
    }
}
