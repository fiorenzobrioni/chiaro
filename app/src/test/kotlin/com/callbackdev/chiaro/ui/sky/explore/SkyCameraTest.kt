package com.callbackdev.chiaro.ui.sky.explore

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.tan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The explorer's camera (Fase 16, step 16.6): from the sky to the screen and back, with a
 * projection that holds the zenith, and the three things a finger does to it.
 */
class SkyCameraTest {

    // A phone held upright, in px.
    private val w = 1080f
    private val h = 2340f
    private val out = FloatArray(2)

    private fun place(camera: SkyCamera, azimuthDeg: Double, altitudeDeg: Double, width: Float = w, height: Float = h): Pair<Float, Float> {
        assertTrue(
            "($azimuthDeg, $altitudeDeg) has a place for $camera",
            camera.lens(width, height).toScreen(azimuthDeg, altitudeDeg, out)
        )
        return out[0] to out[1]
    }

    private fun assertAngle(expected: Double, actual: Double, tolerance: Double = 1e-6) {
        val off = abs(SkyCamera.signed180(actual - expected))
        assertTrue("expected $expected°, was $actual°", off <= tolerance)
    }

    @Test
    fun `where the camera looks is the middle of the screen, the zenith included`() {
        for (az in listOf(0.0, 77.0, 180.0, 271.5)) {
            for (alt in listOf(10.0, 45.0, 89.0, 90.0)) {
                val (x, y) = place(SkyCamera(az, alt), az, alt)
                assertEquals(w / 2, x, 0.01f)
                assertEquals(h / 2, y, 0.01f)
            }
        }
    }

    @Test
    fun `the screen and the sky answer each other`() {
        for (camera in listOf(SkyCamera(200.0, 35.0), SkyCamera(10.0, 90.0, 90.0), SkyCamera(355.0, 72.0, 15.0))) {
            val lens = camera.lens(w, h)
            for (x in listOf(0f, 270f, 540f, 1080f)) {
                for (y in listOf(0f, 600f, 1170f, 2000f, 2340f)) {
                    val sky = lens.toSky(x, y)
                    assertTrue(lens.toScreen(sky.azimuthDeg, sky.altitudeDeg, out))
                    assertEquals(x, out[0], 0.05f)
                    assertEquals(y, out[1], 0.05f)
                }
            }
        }
    }

    /** No roll: straight up from the middle of the screen is the zenith, straight down the horizon in front. */
    @Test
    fun `the zenith is straight above the middle and the horizon ahead straight below`() {
        for (az in listOf(0.0, 133.0, 180.0, 300.0)) {
            for (alt in listOf(12.0, 40.0, 75.0)) {
                val camera = SkyCamera(az, alt)
                val (zx, zy) = place(camera, 0.0, 90.0)
                assertEquals(w / 2, zx, 0.01f)
                assertTrue(zy < h / 2)
                val (hx, hy) = place(camera, az, 0.0)
                assertEquals(w / 2, hx, 0.01f)
                assertTrue(hy > h / 2)
            }
        }
    }

    @Test
    fun `facing north the east is on the right, facing south on the left`() {
        val (north, _) = place(SkyCamera(0.0, 30.0), 20.0, 30.0)
        assertTrue(north > w / 2)
        val (south, _) = place(SkyCamera(180.0, 30.0), 160.0, 30.0)
        assertTrue(south < w / 2)
    }

    /**
     * The zenith is a direction like any other. Looking straight up after facing south,
     * every star at one height is on one ring round the middle; the south is at the foot
     * of the screen, the north at its top, the east on the left, as for a reader who
     * leaned back.
     */
    @Test
    fun `straight up, the sky is a ring round the middle with the horizon faced at the foot`() {
        val camera = SkyCamera(180.0, 90.0)
        val radius = (0 until 360 step 15).map { az ->
            val (x, y) = place(camera, az.toDouble(), 60.0)
            hypot(x - w / 2, y - h / 2)
        }
        assertEquals(radius.min(), radius.max(), 0.01f)

        val (sx, sy) = place(camera, 180.0, 60.0)
        assertEquals(w / 2, sx, 0.01f)
        assertTrue(sy > h / 2)
        val (nx, ny) = place(camera, 0.0, 60.0)
        assertEquals(w / 2, nx, 0.01f)
        assertTrue(ny < h / 2)
        val (ex, ey) = place(camera, 90.0, 60.0)
        assertTrue(ex < w / 2)
        assertEquals(h / 2, ey, 0.01f)
    }

    /** `2·f·tan(θ/2)`: the field is the angle the shorter side takes, whichever side that is. */
    @Test
    fun `the shorter side takes the field, upright and on its side`() {
        // Upright: thirty degrees to the right of the middle is the right edge.
        val (x, y) = place(SkyCamera(180.0, 0.0, 60.0), 210.0, 0.0)
        assertEquals(w, x, 0.01f)
        assertEquals(h / 2, y, 0.01f)
        // On its side: thirty degrees over the middle is the top edge.
        val (lx, ly) = place(SkyCamera(180.0, 30.0, 60.0), 180.0, 60.0, width = h, height = w)
        assertEquals(h / 2, lx, 0.01f)
        assertEquals(0f, ly, 0.01f)
        // And the law itself, at a right angle from the centre: two focal lengths out.
        val lens = SkyCamera(180.0, 0.0, 60.0).lens(w, h)
        assertEquals(w / (4.0 * tan(Math.toRadians(15.0))), lens.focalPx, 1e-6)
        assertTrue(lens.toScreen(270.0, 0.0, out))
        assertEquals(w / 2 + 2f * lens.focalPx.toFloat(), out[0], 0.01f)
    }

    /** Conformal: a small square of sky is a square on the screen, at the edge of a wide field too. */
    @Test
    fun `a shape at the edge of a wide field keeps its shape`() {
        val camera = SkyCamera(180.0, 45.0, 90.0)
        for ((az, alt) in listOf(180.0 to 45.0, 240.0 to 30.0, 120.0 to 70.0, 185.0 to 5.0)) {
            val (x0, y0) = place(camera, az, alt)
            val (x1, y1) = place(camera, az, alt + 0.1)
            // The same length of sky sideways: a tenth of a degree, which near a pole takes more bearing.
            // (A step along a circle of one height is not quite straight: hence the hundredth.)
            val (x2, y2) = place(camera, az + 0.1 / Math.cos(Math.toRadians(alt)), alt)
            val up = hypot(x1 - x0, y1 - y0)
            val across = hypot(x2 - x0, y2 - y0)
            assertEquals("at ($az, $alt)", 1.0, (up / across).toDouble(), 0.01)
            // And at a right angle to each other.
            val dot = (x1 - x0) * (x2 - x0) + (y1 - y0) * (y2 - y0)
            assertEquals(0.0, (dot / (up * across)).toDouble(), 0.01)
        }
    }

    @Test
    fun `what is behind the camera has no place`() {
        val lens = SkyCamera(180.0, 30.0).lens(w, h)
        assertFalse(lens.toScreen(0.0, -30.0, out))
        assertFalse(lens.toScreen(0.0, -10.0, out)) // 160° from where it looks
        assertTrue(lens.toScreen(0.0, 40.0, out)) // over the zenith and down the other side: far, but somewhere
    }

    @Test
    fun `the sky under the finger stays under the finger`() {
        // Over its floor, so that every one of these drags is one it can follow to the end.
        val start = SkyCamera(180.0, 55.0)
        val drags = listOf(
            floatArrayOf(300f, 900f, 420f, 700f),
            floatArrayOf(800f, 1400f, 200f, 1500f),
            floatArrayOf(540f, 1170f, 100f, 1500f),
            floatArrayOf(200f, 1000f, 900f, 1100f)
        )
        for (d in drags) {
            val sky = start.lens(w, h).toSky(d[0], d[1])
            val after = start.dragged(d[0], d[1], d[2], d[3], w, h)
            assertTrue(after.lens(w, h).toScreen(sky.azimuthDeg, sky.altitudeDeg, out))
            assertEquals(d[2], out[0], 0.5f)
            assertEquals(d[3], out[1], 0.5f)
            // A drag is not a zoom.
            assertEquals(start.fieldDeg, after.fieldDeg, 0.0)
        }
    }

    /** The same promise near the zenith, where a camera of rows and columns has no answer. */
    @Test
    fun `near the zenith a drag still carries the sky`() {
        val start = SkyCamera(200.0, 70.0)
        val lens = start.lens(w, h)
        // A star a little past the zenith, on the screen above it.
        val sky = lens.toSky(480f, 700f)
        assertTrue(sky.altitudeDeg > 80.0 && kotlin.math.abs(SkyCamera.signed180(sky.azimuthDeg - 200.0)) > 90.0)
        val after = start.dragged(480f, 700f, 560f, 900f, w, h)
        assertTrue(after.altitudeDeg <= 90.0)
        assertTrue(after.lens(w, h).toScreen(sky.azimuthDeg, sky.altitudeDeg, out))
        assertEquals(560f, out[0], 0.5f)
        assertEquals(900f, out[1], 0.5f)
    }

    @Test
    fun `a drag stops at straight up and at the floor`() {
        val start = SkyCamera.resting(90.0, w, h)
        // The sky pulled down and down: the camera looks up, and stops at the zenith.
        var camera = start
        repeat(30) { camera = camera.dragged(540f, 400f, 540f, 1900f, w, h) }
        assertEquals(90.0, camera.altitudeDeg, 1e-9)
        // Pushed up and up: it stops with the horizon at its ceiling.
        repeat(30) { camera = camera.dragged(540f, 1900f, 540f, 400f, w, h) }
        val floor = SkyCamera.minAltitudeDeg(camera.fieldDeg, w, h)
        assertEquals(floor, camera.altitudeDeg, 1e-9)
        val (_, horizonY) = place(camera, camera.azimuthDeg, 0.0)
        assertEquals(SkyCamera.HorizonLine * h, horizonY, 0.5f)
        // A drag straight up or down does not turn the camera.
        assertAngle(90.0, camera.azimuthDeg)
    }

    @Test
    fun `it opens with the horizon low on the screen, facing where it was told`() {
        val camera = SkyCamera.resting(540.0 + 135.0, w, h)
        assertAngle(315.0, camera.azimuthDeg)
        assertEquals(SkyCamera.RestingFieldDeg, camera.fieldDeg, 0.0)
        val (_, horizonY) = place(camera, camera.azimuthDeg, 0.0)
        assertEquals(SkyCamera.HorizonLine * h, horizonY, 0.5f)
        // As low as it may look: there is no ground to find by pushing the sky up.
        assertEquals(SkyCamera.minAltitudeDeg(camera.fieldDeg, w, h), camera.altitudeDeg, 0.0)
        // Upright at the resting field the top of the screen is past the zenith, on the far side.
        val top = camera.lens(w, h).toSky(w / 2, 0f)
        assertTrue(top.altitudeDeg > 60.0)
        assertAngle(135.0, top.azimuthDeg, 1e-6)
    }

    @Test
    fun `a pinch keeps the sky between the fingers where it is`() {
        val start = SkyCamera.resting(180.0, w, h)
        val sky = start.lens(w, h).toSky(700f, 800f)
        val closer = start.zoomed(2f, 700f, 800f, w, h)
        assertEquals(30.0, closer.fieldDeg, 1e-9)
        assertTrue(closer.lens(w, h).toScreen(sky.azimuthDeg, sky.altitudeDeg, out))
        assertEquals(700f, out[0], 0.5f)
        assertEquals(800f, out[1], 0.5f)
        // Twice as close is twice as large at the centre.
        assertEquals(2.0, closer.lens(w, h).focalPx / start.lens(w, h).focalPx, 0.04)
    }

    @Test
    fun `the zoom stops at its two ends, and one is no zoom`() {
        val start = SkyCamera.resting(180.0, w, h)
        assertEquals(start, start.zoomed(1f, 540f, 1170f, w, h))
        var camera = start
        repeat(20) { camera = camera.zoomed(1.5f, 540f, 1170f, w, h) }
        assertEquals(SkyCamera.MinFieldDeg, camera.fieldDeg, 0.0)
        assertEquals(camera, camera.zoomed(1.5f, 540f, 1170f, w, h))
        repeat(20) { camera = camera.zoomed(1 / 1.5f, 540f, 1170f, w, h) }
        assertEquals(SkyCamera.MaxFieldDeg, camera.fieldDeg, 0.0)
        // Zooming out lifts a camera that was looking lower than the wide field may:
        // the horizon stays on its line and the ground does not come in.
        assertEquals(SkyCamera.minAltitudeDeg(SkyCamera.MaxFieldDeg, w, h), camera.altitudeDeg, 1e-9)
        val (_, horizonY) = place(camera, camera.azimuthDeg, 0.0)
        assertEquals(SkyCamera.HorizonLine * h, horizonY, 0.5f)
    }

    @Test
    fun `a step turns it, wraps at north and stops at the ends`() {
        val camera = SkyCamera(350.0, 40.0)
        assertAngle(5.0, camera.turned(15.0, 0.0, w, h).azimuthDeg)
        assertAngle(335.0, camera.turned(-15.0, 0.0, w, h).azimuthDeg)
        assertEquals(55.0, camera.turned(0.0, 15.0, w, h).altitudeDeg, 1e-9)
        assertEquals(90.0, camera.turned(0.0, 80.0, w, h).altitudeDeg, 1e-9)
        assertEquals(
            SkyCamera.minAltitudeDeg(camera.fieldDeg, w, h),
            camera.turned(0.0, -80.0, w, h).altitudeDeg,
            1e-9
        )
    }

    /** A circle of the sky is a circle on the screen, and the horizon is one. */
    @Test
    fun `the horizon is a circle with the sky inside it`() {
        for (camera in listOf(SkyCamera(180.0, 20.0), SkyCamera(40.0, 60.0, 90.0), SkyCamera(300.0, 90.0, 90.0))) {
            val lens = camera.lens(w, h)
            val circle = lens.horizon
            assertEquals(w / 2, circle.centreX, 0f)
            for (az in 0 until 360 step 10) {
                if (!lens.toScreen(az.toDouble(), 0.0, out)) continue
                val d = hypot(out[0] - circle.centreX, out[1] - circle.centreY)
                assertEquals("$camera at $az", 1.0, (d / circle.radius).toDouble(), 1e-4)
            }
            assertTrue(lens.toScreen(0.0, 90.0, out))
            assertTrue(hypot(out[0] - circle.centreX, out[1] - circle.centreY) < circle.radius)
            assertTrue(lens.toScreen(camera.azimuthDeg, -10.0, out))
            assertTrue(hypot(out[0] - circle.centreX, out[1] - circle.centreY) > circle.radius)
        }
    }

    @Test
    fun `a wider field draws fewer of the faint stars, and a closer one larger lights`() {
        assertEquals(6.0f, SkyCamera.faintestMagnitude(15.0), 0f)
        assertEquals(6.0f, SkyCamera.faintestMagnitude(60.0), 0f)
        assertEquals(5.3f, SkyCamera.faintestMagnitude(90.0), 0.01f)
        assertTrue(SkyCamera.faintestMagnitude(75.0) < 6.0f)
        assertTrue(SkyCamera.faintestMagnitude(75.0) > SkyCamera.faintestMagnitude(90.0))

        assertEquals(1f, SkyCamera.markScale(60.0), 0f)
        assertEquals(1.41f, SkyCamera.markScale(15.0), 0.01f)
        assertEquals(0.90f, SkyCamera.markScale(90.0), 0.01f)
    }
}
