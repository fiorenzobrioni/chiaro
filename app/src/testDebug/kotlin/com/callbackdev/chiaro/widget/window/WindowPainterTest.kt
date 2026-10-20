package com.callbackdev.chiaro.widget.window

import android.app.Application
import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.callbackdev.chiaro.domain.sky.AstronomyEngine
import com.callbackdev.chiaro.domain.sky.SkyBody
import com.callbackdev.chiaro.readme.MilanRecording
import com.callbackdev.chiaro.ui.components.StarMark
import com.callbackdev.chiaro.ui.components.starPlace
import com.callbackdev.chiaro.widget.WidgetKind
import java.io.File
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The window's picture on the recorded Milan, by night and by day, through the shader where
 * the environment lets it draw and through the plain fallback on request. Saved under
 * `CHIARO_RENDER_DIR` when that is set, so the card can be looked at on a machine with no
 * device (as the sky's pixel tests are).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w384dp-h832dp-xhdpi")
class WindowPainterTest {

    /** A window, as the sky's pixel tests have one: the hardware renderer draws into its
     * image only inside the graphics environment an activity's window sets up. */
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val app: Application = ApplicationProvider.getApplicationContext()
    private val size = DpSize(340.dp, 189.dp)

    private fun luminanceAt(bitmap: Bitmap, x: Int, y: Int): Float {
        val p = bitmap.getPixel(x.coerceIn(0, bitmap.width - 1), y.coerceIn(0, bitmap.height - 1))
        return Color(((p shr 16) and 0xFF) / 255f, ((p shr 8) and 0xFF) / 255f, (p and 0xFF) / 255f).luminance()
    }

    private fun save(name: String, bitmap: Bitmap) {
        System.getenv("CHIARO_RENDER_DIR")?.let { dir ->
            File(dir).apply { mkdirs() }.resolve("$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    @Test
    fun `at night the window turns to the moon and draws it where the stars' camera puts it`() {
        val at = MilanRecording.night
        val model = MilanRecording.widget(app, WidgetKind.WINDOW, at)
        val plan = windowPlan(size, 1f, model.moments.size, warning = false)
        val picture = checkNotNull(WindowPainter.paint(app, model, size, plan, at))
        save("widget-window-night", picture.bitmap)
        val density = app.resources.displayMetrics.density
        assertEquals((size.width.value * density).roundToInt(), picture.bitmap.width)
        assertEquals((size.height.value * density).roundToInt(), picture.bitmap.height)
        assertTrue(picture.night)
        // Full night: the picture is drawn for the hour's middle, thirty minutes on.
        val drawAt = at.plusSeconds(30 * 60)
        assertEquals(drawAt, picture.drawnAt)
        val coords = MilanRecording.city.coordinates
        val moonAltitude = AstronomyEngine.moonAltitude(drawAt, coords)
        assertTrue("the moon is up that evening", moonAltitude > WindowPainter.MoonFacingFloorDeg)
        assertEquals(AstronomyEngine.bodyAzimuth(SkyBody.MOON, drawAt, coords), picture.facingDeg, 1e-9)
        // The moon stands in the middle column, at its altitude in the stars' camera: a bright disc there.
        val spot = starPlace(StarMark(0f, moonAltitude.toFloat(), 1f, 0f, 0f), picture.bitmap.width, picture.bitmap.height)!!
        val moon = luminanceAt(picture.bitmap, spot.first.roundToInt(), spot.second.roundToInt())
        val corner = luminanceAt(picture.bitmap, 12, 12)
        assertTrue("the moon ($moon) outshines the sky's corner ($corner)", moon > corner * 3f && moon > 0.5f)
        // The moon is named on the disc, so the caption will leave its number to it.
        assertTrue(picture.moonNamed)
    }

    @Test
    fun `by day the window faces the equator and draws no stars`() {
        val at = MilanRecording.now
        val model = MilanRecording.widget(app, WidgetKind.WINDOW, at)
        val plan = windowPlan(size, 1f, model.moments.size, warning = false)
        val picture = checkNotNull(WindowPainter.paint(app, model, size, plan, at))
        save("widget-window-day", picture.bitmap)
        assertFalse(picture.night)
        assertEquals(180.0, picture.facingDeg, 1e-9)
        // By day the picture is drawn for now, and names no moon.
        assertEquals(at, picture.drawnAt)
        assertFalse(picture.moonNamed)
        // An overcast noon: a light sky, not a night.
        assertTrue(luminanceAt(picture.bitmap, picture.bitmap.width / 2, 20) > 0.2f)
    }

    @Test
    fun `the plain fallback draws the gradient, the stars and the moon with no shader`() {
        val at = MilanRecording.night
        val model = MilanRecording.widget(app, WidgetKind.WINDOW, at)
        val plan = windowPlan(size, 1f, model.moments.size, warning = false)
        val picture = checkNotNull(WindowPainter.paint(app, model, size, plan, at, gpu = false))
        save("widget-window-night-fallback", picture.bitmap)
        assertFalse(picture.gpu)
        assertTrue(picture.night)
        val moonAltitude = AstronomyEngine.moonAltitude(picture.drawnAt, MilanRecording.city.coordinates)
        val spot = starPlace(StarMark(0f, moonAltitude.toFloat(), 1f, 0f, 0f), picture.bitmap.width, picture.bitmap.height)!!
        assertTrue(luminanceAt(picture.bitmap, spot.first.roundToInt(), spot.second.roundToInt()) > 0.5f)
    }

    @Test
    fun `the tall card's picture keeps the sky over the rows' ground`() {
        val tall = DpSize(340.dp, 293.dp)
        val at = MilanRecording.now
        val model = MilanRecording.widget(app, WidgetKind.WINDOW, at)
        val plan = windowPlan(tall, 1f, model.moments.size, warning = false)
        assertTrue(plan.momentRows > 0)
        val picture = checkNotNull(WindowPainter.paint(app, model, tall, plan, at))
        val density = app.resources.displayMetrics.density
        assertEquals((tall.height.value * density).roundToInt(), picture.bitmap.height)
        val skyH = (plan.skyHeight.value * density).roundToInt()
        // Under the picture the ground is darker than the sky above it.
        assertTrue(luminanceAt(picture.bitmap, 40, skyH + 20) < luminanceAt(picture.bitmap, 40, 20))
    }

    /** In twilight the stars are out but the sky still moves in minutes: the picture is for now. */
    @Test
    fun `in twilight the window is drawn for now, not for the hour's middle`() {
        val at = java.time.Instant.parse("2026-09-25T18:00:00Z") // 20:00 in Milan, the sun under −6° and over −12°
        val coords = MilanRecording.city.coordinates
        val sun = AstronomyEngine.sunAltitude(at, coords)
        assertTrue("twilight: $sun", sun < -6.0 && sun > WindowPainter.FullNightSunDeg)
        val model = MilanRecording.widget(app, WidgetKind.WINDOW, at)
        val plan = windowPlan(size, 1f, model.moments.size, warning = false)
        val picture = checkNotNull(WindowPainter.paint(app, model, size, plan, at))
        assertTrue(picture.night)
        assertEquals(at, picture.drawnAt)
    }

    /** «Behind the clouds»: the overcast noon becomes a blue sky, and the picture says which it is. */
    @Test
    fun `behind the clouds the overcast noon is a blue sky, and the picture says so`() {
        val at = MilanRecording.now
        val model = MilanRecording.widget(app, WidgetKind.WINDOW, at)
        val plan = windowPlan(size, 1f, model.moments.size, warning = false)
        val clouded = checkNotNull(WindowPainter.paint(app, model, size, plan, at))
        val bare = checkNotNull(WindowPainter.paint(app, model.copy(look = model.look.copy(windowBareSky = true)), size, plan, at))
        save("widget-window-bare", bare.bitmap)
        assertFalse(clouded.bare)
        assertTrue(bare.bare)
        fun blueness(bitmap: Bitmap, x: Int, y: Int): Float {
            val p = bitmap.getPixel(x, y)
            return ((p and 0xFF) - ((p shr 16) and 0xFF)) / 255f
        }
        val x = bare.bitmap.width / 2
        val y = bare.bitmap.height / 3
        assertTrue(
            "a clear sky is bluer than an overcast one: ${blueness(bare.bitmap, x, y)} over ${blueness(clouded.bitmap, x, y)}",
            blueness(bare.bitmap, x, y) > blueness(clouded.bitmap, x, y) + 0.1f
        )
    }
}
