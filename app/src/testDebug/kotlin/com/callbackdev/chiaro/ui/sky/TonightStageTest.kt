package com.callbackdev.chiaro.ui.sky

import android.app.Application
import android.graphics.Bitmap
import android.graphics.HardwareRenderer
import android.graphics.PixelFormat
import android.graphics.RenderNode
import android.media.ImageReader
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.core.app.ApplicationProvider
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.domain.sky.AstronomyEngine
import com.callbackdev.chiaro.domain.sky.SkyBody
import com.callbackdev.chiaro.readme.MilanRecording
import com.callbackdev.chiaro.ui.components.StarMark
import com.callbackdev.chiaro.ui.components.starPlace
import com.callbackdev.chiaro.ui.format.LocalClock
import com.callbackdev.chiaro.ui.theme.ChiaroTheme
import com.callbackdev.chiaro.widget.window.WindowText
import java.io.File
import java.time.Clock
import java.time.Instant
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
 * The Tonight card's stage (22 ott 2026): the real sky of the night's instant, drawn by the
 * shader with the moon where the stars' camera puts it; the compass line with the hour; the
 * finger on the strip that moves the hour and not the camera; the drag that turns it.
 * Rendered through the GPU pipeline, as the widget's pictures and the README are.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w384dp-h832dp-xhdpi")
class TonightStageTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val app: Application = ApplicationProvider.getApplicationContext()
    private val at: Instant = MilanRecording.night // 21:00, inside the night: the picture is of now
    private val coords = MilanRecording.city.coordinates

    private val looking = SemanticsMatcher("looking") { node ->
        node.config.getOrNull(SemanticsProperties.ContentDescription)?.any { it.startsWith("Looking ") } == true
    }

    private fun show() {
        val content = MilanRecording.sky().let { base ->
            SkyStateBuilder.build(MilanRecording.city, MilanRecording.report, base.subscriptions, MilanRecording.settings, at)
        }
        compose.setContent {
            ChiaroTheme(darkTheme = true, dynamicColor = false) {
                CompositionLocalProvider(LocalClock provides Clock.fixed(at, content.zone)) {
                    TonightCard(
                        tonight = content.tonight,
                        zone = content.zone,
                        timeFmt = com.callbackdev.chiaro.ui.format.Formats.timeFormatter(true, java.util.Locale.UK),
                        coordinates = coords,
                        followed = content.subscribedIds
                    )
                }
            }
        }
        compose.waitForIdle()
    }

    private fun lookingText(): String =
        compose.onNode(looking).fetchSemanticsNode().config[SemanticsProperties.ContentDescription].first()

    @Test
    fun `the stage looks at the moon, says the hour, and draws the moon where the stars' camera puts it`() {
        show()
        val facing = AstronomyEngine.bodyAzimuth(SkyBody.MOON, at, coords)
        val zone = MilanRecording.sky().zone
        assertEquals(WindowText.looking(app, facing, at, zone), lookingText())
        // The moon badge is gone: the picture is the moon now.
        assertTrue(compose.onAllNodes(hasText(app.getString(R.string.sky_tonight_moon_pct, 99))).fetchSemanticsNodes().isEmpty())
        val stage = compose.onNode(looking).fetchSemanticsNode().boundsInRoot
        val bitmap = renderWindow()
        save("tonight-stage", bitmap)
        val w = stage.width.roundToInt()
        val h = stage.height.roundToInt()
        val altitude = AstronomyEngine.moonAltitude(at, coords)
        val spot = starPlace(StarMark(0f, altitude.toFloat(), 1f, 0f, 0f), w, h)!!
        val moon = luminanceAt(bitmap, (stage.left + spot.first).roundToInt(), (stage.top + spot.second).roundToInt())
        val corner = luminanceAt(bitmap, (stage.left + 12).roundToInt(), (stage.top + h - 12).roundToInt())
        assertTrue("the moon ($moon) outshines the sky's corner ($corner)", moon > corner * 3f && moon > 0.5f)
    }

    @Test
    fun `a finger on the strip moves the hour and leaves the camera where it was`() {
        show()
        val before = lookingText()
        val strip = compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
        strip.performSemanticsAction(SemanticsActions.SetProgress) { it(0.5f) }
        compose.waitForIdle()
        val after = lookingText()
        assertNotEquals(before, after)
        assertEquals(before.substringBefore(" at "), after.substringBefore(" at "))
        assertNotEquals(before.substringAfter(" at "), after.substringAfter(" at "))
    }

    @Test
    fun `a drag across the stage turns the window`() {
        show()
        // The first touch sequence after setContent is lost under Robolectric (seen 22 ott
        // 2026: no drag start for it, the next one received), so one drag warms the window
        // up and the measured one follows it.
        fun drag() = compose.onNode(looking).performTouchInput {
            down(center)
            repeat(10) { moveBy(androidx.compose.ui.geometry.Offset(-30f * density, 0f), delayMillis = 16) }
            up()
        }
        drag()
        compose.waitForIdle()
        val before = lookingText()
        drag()
        compose.waitForIdle()
        val after = lookingText()
        assertNotEquals("$before vs $after", before.substringBefore(" at "), after.substringBefore(" at "))
        assertEquals(before.substringAfter(" at "), after.substringAfter(" at "))
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
