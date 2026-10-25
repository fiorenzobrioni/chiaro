package com.callbackdev.chiaro.widget.window

import com.callbackdev.chiaro.domain.model.Coordinates
import com.callbackdev.chiaro.domain.sky.AstronomyEngine
import com.callbackdev.chiaro.domain.sky.SkyBody
import com.callbackdev.chiaro.ui.components.SkyFall
import com.callbackdev.chiaro.ui.theme.SkyPalette
import java.time.Instant
import java.time.ZoneId
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The window turned by a finger (25 ott 2026): the turn reaches the scene unwrapped, the wind
 * is seen from where the window looked before the finger, the frame the clouds' field was laid
 * in, and the clouds' light stands where the moon is drawn.
 */
class WindowTurnTest {

    private val milan = Coordinates(45.4642, 9.19)
    private val rome: ZoneId = ZoneId.of("Europe/Rome")
    private val w = 1056
    private val skyH = 550
    private val density = 2.75f

    private fun frame(
        at: Instant,
        facing: Double?,
        turn: Double = 0.0,
        cloud: Int = 40,
        windKph: Double = 20.0,
        windFrom: Int = 180
    ) = WindowScene.frame(
        table = SkyPalette.Paper, coords = milan, zone = rome, southern = false, drawAt = at,
        facingDeg = facing, turnDeg = turn, w = w, skyH = skyH, density = density,
        cloudPct = cloud, layers = null, precipPct = 0, fall = SkyFall.NONE, fallMmPerHour = 0.0,
        windKph = windKph, windFromDeg = windFrom, followed = emptySet()
    )

    @Test
    fun `the turn reaches the scene as it is, past a full circle`() {
        val at = Instant.parse("2026-10-26T21:00:00Z")
        val turned = frame(at, facing = 190.0, turn = 400.0)
        assertEquals(190.0, turned.facingDeg, 0.0)
        assertEquals(400f, turned.scene.turn, 0f)
        assertEquals(0f, frame(at, facing = null).scene.turn, 0f)
    }

    /** A wind from the south blows north: facing west it crosses the view to the right, facing
     * east to the left, and facing north it goes away from the reader and only breathes. A
     * window turned by a finger keeps the drift of where it looked before (the second cut, 25
     * ott 2026): the shader turns the field, wind and all, so the drift is not turned twice,
     * and its sum (time by speed) does not jump under the finger. */
    @Test
    fun `the clouds drift as seen from where the window looked before the finger`() {
        val at = Instant.parse("2026-10-26T21:00:00Z")
        val west = frame(at, facing = 270.0).scene.drift
        assertTrue(west > 0.5f)
        assertTrue(frame(at, facing = 90.0).scene.drift < -0.5f)
        assertEquals(0.15f * west, abs(frame(at, facing = 0.0).scene.drift), 1e-4f)
        assertEquals("turned a right angle from west, the drift is west's", west, frame(at, facing = 0.0, turn = 90.0).scene.drift, 1e-5f)
        assertEquals("and past a circle", west, frame(at, facing = 290.0, turn = 380.0).scene.drift, 1e-5f)
    }

    /** The full moon of 26 Oct 2026, high in the south of Milan late in the evening, lights
     * the clouds from where the camera draws it; turned, the light turns with the moon; turned
     * away, the moon leaves the window and the light is held past its edge, not lost. */
    @Test
    fun `the clouds' light stands where the camera draws the moon, and turns with it`() {
        val at = Instant.parse("2026-10-26T22:00:00Z")
        val moonAz = AstronomyEngine.bodyAzimuth(SkyBody.MOON, at, milan)
        val facingMoon = frame(at, facing = moonAz)
        assertTrue("night with the moon up", facingMoon.night && facingMoon.moonUp)
        val moon = checkNotNull(facingMoon.scene.moon)
        assertEquals(moon.across, facingMoon.scene.light.x, 1e-4f)
        assertEquals(moon.up, facingMoon.scene.light.y, 1e-4f)
        val turned = frame(at, facing = moonAz + 20.0, turn = 20.0)
        val turnedMoon = checkNotNull(turned.scene.moon)
        assertTrue("turned right, the moon is left of where it was", turnedMoon.across < moon.across)
        assertEquals(turnedMoon.across, turned.scene.light.x, 1e-4f)
        assertEquals(turnedMoon.up, turned.scene.light.y, 1e-4f)
        val away = frame(at, facing = moonAz + 150.0, turn = 150.0)
        assertNull(away.scene.moon)
        assertTrue("held past the left edge: ${away.scene.light.x}", away.scene.light.x < 0f)
    }
}
