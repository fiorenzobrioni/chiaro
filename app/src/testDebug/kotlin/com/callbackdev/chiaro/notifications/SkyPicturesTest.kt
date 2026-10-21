package com.callbackdev.chiaro.notifications

import android.app.Application
import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.callbackdev.chiaro.data.AppPalette
import com.callbackdev.chiaro.domain.placeZone
import com.callbackdev.chiaro.readme.MilanRecording
import java.io.File
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The sky reminder's picture on the recorded Milan (21 ott 2026): the scene of the moment,
 * through the window painter, for an hour the forecast has — and none for an hour it does
 * not. Saved under `CHIARO_RENDER_DIR` when that is set, to be looked at.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w384dp-h832dp-xhdpi")
class SkyPicturesTest {

    /** A window, as the painter's own tests have one: the hardware renderer draws only
     * inside the graphics environment an activity's window sets up. */
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val app: Application = ApplicationProvider.getApplicationContext()
    private val report = MilanRecording.report
    private val city = MilanRecording.city
    private val zone = placeZone(report, city)

    private fun scene(at: java.time.Instant, gpu: Boolean = true): Bitmap? =
        SkyPictures.scene(app, report, city.coordinates, zone, at, AppPalette.VIVID, followed = emptySet(), gpu = gpu)

    private fun save(name: String, bitmap: Bitmap) {
        System.getenv("CHIARO_RENDER_DIR")?.let { dir ->
            File(dir).apply { mkdirs() }.resolve("$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    private fun luminance(argb: Int): Float {
        fun channel(shift: Int) = ((argb shr shift) and 0xFF) / 255f
        return 0.2126f * channel(16) + 0.7152f * channel(8) + 0.0722f * channel(0)
    }

    @Test
    fun `the night of the recording is drawn at the picture's size, darker than its noon`() {
        val night = scene(MilanRecording.night)!!
        val noon = scene(MilanRecording.now)!!
        save("sky-reminder-night", night)
        save("sky-reminder-noon", noon)
        assertEquals(SkyPictures.WIDTH, night.width)
        assertEquals(SkyPictures.HEIGHT, night.height)
        val nightSky = luminance(night.getPixel(night.width / 4, night.height / 3))
        val noonSky = luminance(noon.getPixel(noon.width / 4, noon.height / 3))
        assertTrue("night $nightSky against noon $noonSky", nightSky < noonSky)
    }

    @Test
    fun `the plain fallback draws too, and the same size`() {
        val plain = scene(MilanRecording.night, gpu = false)!!
        save("sky-reminder-night-plain", plain)
        assertEquals(SkyPictures.WIDTH, plain.width)
        assertEquals(SkyPictures.HEIGHT, plain.height)
    }

    /** An hour the forecast does not cover has no picture: its clouds would be nobody's. */
    @Test
    fun `no forecast hour, no picture`() {
        assertNull(scene(report.hourly.last().at.plus(Duration.ofHours(2))))
        assertNull(scene(report.hourly.first().at.minus(Duration.ofHours(1))))
    }
}
