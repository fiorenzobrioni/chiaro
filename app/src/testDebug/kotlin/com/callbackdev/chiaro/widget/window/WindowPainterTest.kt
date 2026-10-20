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
        val sky = model.content!!.sky
        assertTrue("the moon is up that evening", sky.moonAltitudeDeg > WindowPainter.MoonFacingFloorDeg)
        assertEquals(sky.moonAzimuthDeg, picture.facingDeg, 1e-9)
        // The moon stands in the middle column, at its altitude in the stars' camera: a bright disc there.
        val spot = starPlace(StarMark(0f, sky.moonAltitudeDeg.toFloat(), 1f, 0f, 0f), picture.bitmap.width, picture.bitmap.height)!!
        val moon = luminanceAt(picture.bitmap, spot.first.roundToInt(), spot.second.roundToInt())
        val corner = luminanceAt(picture.bitmap, 12, 12)
        assertTrue("the moon ($moon) outshines the sky's corner ($corner)", moon > corner * 3f && moon > 0.5f)
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
        val sky = model.content!!.sky
        val spot = starPlace(StarMark(0f, sky.moonAltitudeDeg.toFloat(), 1f, 0f, 0f), picture.bitmap.width, picture.bitmap.height)!!
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
}
