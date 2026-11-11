package com.callbackdev.chiaro.ui.sky.explore

import android.app.Application
import android.graphics.Bitmap
import android.graphics.HardwareRenderer
import android.graphics.PixelFormat
import android.graphics.RenderNode
import android.media.ImageReader
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.domain.model.Coordinates
import com.callbackdev.chiaro.domain.sky.AstronomyEngine
import com.callbackdev.chiaro.domain.sky.SkyBody
import com.callbackdev.chiaro.ui.format.LocalClock
import com.callbackdev.chiaro.ui.theme.ChiaroTheme
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The explorer's page (Fase 16, step 16.6): the sky of an instant on the whole screen, what
 * a finger and the two buttons do to it, and the rule it is held to: it draws when the
 * camera changes and at no other time. Rendered through the GPU pipeline, as the hero's
 * stage and the README's pictures are.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w384dp-h832dp-xhdpi")
class SkyExploreScreenTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val app: Application = ApplicationProvider.getApplicationContext()
    private val milan = Coordinates(45.4642, 9.19)
    private val zone = ZoneId.of("Europe/Rome")

    // A moonless night with Orion rising (11 Nov 2026, 23:00 in Milan), and the recorded
    // Milan's own night, a full moon over the south-east.
    private val dark = Instant.parse("2026-11-11T22:00:00Z")
    private val moonlit = Instant.parse("2026-09-25T21:00:00Z")

    private var frames = 0
    private var closed = 0

    private fun show(at: Instant = dark, facingDeg: Double = 135.0) {
        compose.setContent {
            ChiaroTheme(darkTheme = false, dynamicColor = false) {
                CompositionLocalProvider(LocalClock provides Clock.fixed(at, zone)) {
                    SkyExploreScreen(
                        coordinates = milan,
                        zone = zone,
                        at = at,
                        facingDeg = facingDeg,
                        followed = emptySet(),
                        onBack = { closed++ },
                        onFrame = { frames++ }
                    )
                }
            }
        }
        compose.waitForIdle()
    }

    private val sky get() = compose.onNode(hasTestTag(ExploreSkyTag))

    private fun looking(): String =
        sky.fetchSemanticsNode().config[SemanticsProperties.ContentDescription].first()

    private val closer get() = compose.onNode(hasContentDescription(app.getString(R.string.explore_zoom_in)))
    private val further get() = compose.onNode(hasContentDescription(app.getString(R.string.explore_zoom_out)))

    /** One drag of the sky by ([dx], [dy]) dp, in steps as a finger makes them. */
    private fun drag(dx: Float, dy: Float) {
        sky.performTouchInput {
            down(center)
            repeat(10) { moveBy(Offset(dx / 10f * density, dy / 10f * density), delayMillis = 16) }
            up()
        }
        compose.waitForIdle()
    }

    @Test
    fun `it opens facing where the hero looked, on the hour it was told, and says the clouds are not in it`() {
        show()
        assertEquals("Looking south-east", looking())
        compose.onNode(hasText("The sky at 23:00, without the clouds")).assertExists()
        save("explore-dark", renderWindow())
    }

    @Test
    fun `the moon is drawn where the camera puts it`() {
        val facing = AstronomyEngine.bodyAzimuth(SkyBody.MOON, moonlit, milan)
        show(moonlit, facing)
        val bitmap = renderWindow()
        save("explore-moon", bitmap)
        val out = FloatArray(2)
        val camera = SkyCamera.resting(facing, bitmap.width.toFloat(), bitmap.height.toFloat())
        assertTrue(camera.lens(bitmap.width.toFloat(), bitmap.height.toFloat()).toScreen(facing, AstronomyEngine.moonAltitude(moonlit, milan), out))
        // Facing the moon it is on the line down the middle of the screen.
        assertEquals(bitmap.width / 2f, out[0], 1f)
        val moon = luminanceAt(bitmap, out[0].roundToInt(), out[1].roundToInt())
        val beside = luminanceAt(bitmap, out[0].roundToInt() + 120, out[1].roundToInt())
        assertTrue("the moon ($moon) outshines the sky beside it ($beside)", moon > 0.6f && moon > beside * 4f)
    }

    /**
     * The phase is turned toward the sun as the place sees it: on the evening of 14 Nov
     * 2026 the moon is a crescent five days old, low in the south-west an hour after
     * sunset, and its lit side is the one toward where the sun went down.
     */
    @Test
    fun `an evening crescent is lit on the side of the sun that has set`() {
        val evening = Instant.parse("2026-11-14T17:00:00Z")
        val facing = AstronomyEngine.bodyAzimuth(SkyBody.MOON, evening, milan)
        val altitude = AstronomyEngine.moonAltitude(evening, milan)
        assertTrue("the moon is up: $altitude", altitude > 5.0)
        assertTrue(AstronomyEngine.moonIllumination(evening).illuminatedFraction in 0.15..0.45)
        show(evening, facing)
        val bitmap = renderWindow()
        save("explore-crescent", bitmap)
        val lens = SkyCamera.resting(facing, bitmap.width.toFloat(), bitmap.height.toFloat())
            .lens(bitmap.width.toFloat(), bitmap.height.toFloat())
        val moon = FloatArray(2)
        val sun = FloatArray(2)
        assertTrue(lens.toScreen(facing, altitude, moon))
        assertTrue(lens.toScreen(AstronomyEngine.sunAzimuth(evening, milan), AstronomyEngine.sunAltitude(evening, milan), sun))
        // The sun is down and to the right of an evening moon, from the north.
        assertTrue(sun[0] > moon[0] && sun[1] > moon[1])
        val d = kotlin.math.hypot(sun[0] - moon[0], sun[1] - moon[1])
        val reach = 11f * 2f * 0.7f // seven tenths of the disc's radius, in this picture's px
        val ux = (sun[0] - moon[0]) / d * reach
        val uy = (sun[1] - moon[1]) / d * reach
        val lit = luminanceAt(bitmap, (moon[0] + ux).roundToInt(), (moon[1] + uy).roundToInt())
        val shaded = luminanceAt(bitmap, (moon[0] - ux).roundToInt(), (moon[1] - uy).roundToInt())
        assertTrue("toward the sun ($lit) is the lit side, away from it ($shaded) the dark one", lit > 0.6f && shaded < 0.15f)
    }

    @Test
    fun `a drag turns the sky, and pulling it down looks straight up`() {
        show()
        drag(-40f, 0f) // the first sequence is lost under Robolectric
        val before = looking()
        // The sky pulled to the left: the camera turns to the right, from south-east on round.
        drag(-300f, 0f)
        val turned = looking()
        assertNotEquals(before, turned)
        assertTrue(turned, turned.startsWith("Looking south"))
        save("explore-turned", renderWindow())
        // Pulled down, twice the screen: the zenith, which the hero's camera never reached.
        drag(0f, 380f)
        drag(0f, 380f)
        assertEquals(app.getString(R.string.explore_overhead), looking())
        save("explore-zenith", renderWindow())
    }

    @Test
    fun `the two buttons zoom, and each stops at its end`() {
        show()
        closer.assertIsEnabled()
        further.assertIsEnabled()
        // Sixty degrees, then forty, twenty-seven, eighteen, and the fifteen it stops at.
        repeat(4) { closer.performClick() }
        compose.waitForIdle()
        closer.assertIsNotEnabled()
        save("explore-close", renderWindow())
        // And out again, all the way to the right angle.
        repeat(5) { further.performClick() }
        compose.waitForIdle()
        further.assertIsNotEnabled()
        closer.assertIsEnabled()
        save("explore-wide", renderWindow())
    }

    /**
     * The battery's rule (Fase 16, «Le regole comuni»): nothing in this sky moves by itself.
     * Read off the view: once it has been drawn, half a minute of looking asks for no other
     * drawing, and a zoom asks for one.
     */
    @Test
    fun `it draws when the camera changes and at no other time`() {
        show()
        renderWindow()
        assertEquals("it drew the sky it opened on", 1, frames)
        val view = composeView()
        compose.mainClock.autoAdvance = false
        compose.mainClock.advanceTimeBy(30_000)
        compose.runOnIdle { assertTrue("half a minute of looking asked for no drawing", !view.isDirty) }

        // The camera moved, by the screen reader's action: a button would add its own ripple.
        val right = sky.fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsActions.CustomActions]
            .first { it.label == app.getString(R.string.explore_turn_right) }
        compose.mainClock.autoAdvance = true
        compose.runOnUiThread { right.action() }
        compose.runOnIdle { assertTrue("a turn asks for one", view.isDirty) }
        renderWindow()
        assertEquals(2, frames)
        compose.mainClock.autoAdvance = false
        compose.mainClock.advanceTimeBy(30_000)
        compose.runOnIdle { assertTrue("and then no more", !view.isDirty) }
    }

    /** The view Compose draws into: the one that is asked to draw again when something changed. */
    private fun composeView(): android.view.View {
        fun find(view: android.view.View): android.view.View? {
            if (view.javaClass.name.endsWith("AndroidComposeView")) return view
            if (view is android.view.ViewGroup) {
                for (i in 0 until view.childCount) find(view.getChildAt(i))?.let { return it }
            }
            return null
        }
        return find(compose.activity.window.decorView)!!
    }

    @Test
    fun `the cross closes it`() {
        show()
        compose.onNode(hasContentDescription(app.getString(R.string.explore_close))).performClick()
        assertEquals(1, closed)
    }

    /** A screen reader cannot drag: the same four moves are actions on the sky. */
    @Test
    fun `the sky can be turned without a finger`() {
        show()
        val actions = sky.fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsActions.CustomActions]
        assertEquals(
            listOf(R.string.explore_turn_left, R.string.explore_turn_right, R.string.explore_look_up, R.string.explore_look_down)
                .map { app.getString(it) },
            actions.map { it.label }
        )
        // Three steps to the right of south-east is past south.
        repeat(3) { compose.runOnUiThread { actions.first { it.label == app.getString(R.string.explore_turn_right) }.action() } }
        compose.waitForIdle()
        assertEquals("Looking south", looking())
        repeat(4) { compose.runOnUiThread { actions.first { it.label == app.getString(R.string.explore_look_up) }.action() } }
        compose.waitForIdle()
        assertEquals(app.getString(R.string.explore_overhead), looking())
    }

    private fun luminanceAt(bitmap: Bitmap, x: Int, y: Int): Float {
        val p = bitmap.getPixel(x.coerceIn(0, bitmap.width - 1), y.coerceIn(0, bitmap.height - 1))
        return Color(((p shr 16) and 0xFF) / 255f, ((p shr 8) and 0xFF) / 255f, (p and 0xFF) / 255f).luminance()
    }

    private fun save(name: String, bitmap: Bitmap) {
        System.getenv("CHIARO_RENDER_DIR")?.let { dir ->
            File(dir).apply { mkdirs() }.resolve("$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    private fun renderWindow(): Bitmap {
        val view = compose.activity.window.decorView
        val width = view.width
        val height = view.height
        val node = RenderNode("window").apply { setPosition(0, 0, width, height) }
        compose.runOnUiThread { view.draw(node.beginRecording()); node.endRecording() }
        val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 1)
        val renderer = HardwareRenderer()
        try {
            renderer.setSurface(reader.surface)
            renderer.setContentRoot(node)
            renderer.createRenderRequest().setWaitForPresent(true).syncAndDraw()
            reader.acquireNextImage().use { image ->
                val plane = image.planes[0]
                val padded = Bitmap.createBitmap(plane.rowStride / plane.pixelStride, height, Bitmap.Config.ARGB_8888)
                padded.copyPixelsFromBuffer(plane.buffer)
                return Bitmap.createBitmap(padded, 0, 0, width, height)
            }
        } finally {
            renderer.destroy()
            reader.close()
        }
    }
}
