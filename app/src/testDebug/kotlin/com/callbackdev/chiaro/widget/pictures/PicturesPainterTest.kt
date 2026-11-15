package com.callbackdev.chiaro.widget.pictures

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.test.core.app.ApplicationProvider
import com.callbackdev.chiaro.domain.pictures.DialSpec
import com.callbackdev.chiaro.domain.pictures.PictureHour
import com.callbackdev.chiaro.domain.pictures.PictureWindow
import com.callbackdev.chiaro.domain.pictures.PlateMetrics
import com.callbackdev.chiaro.domain.pictures.RainTone
import com.callbackdev.chiaro.domain.pictures.StripMetrics
import com.callbackdev.chiaro.domain.pictures.dialDrawing
import com.callbackdev.chiaro.domain.pictures.plateDrawing
import com.callbackdev.chiaro.domain.pictures.stripDrawing
import com.callbackdev.chiaro.domain.settings.TemperatureUnit
import com.callbackdev.chiaro.ui.theme.PicturesInks
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The painter puts the geometry where it says, in the inks it is given (Fase 17.2): the bitmap's
 * size with its overhang, a rain bar in its tone, the disc of «adesso» white on its point, the
 * dial's ring, the strip's marks; and the plate's words for TalkBack.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "it-rIT-xhdpi")
class PicturesPainterTest {

    private val app: Application = ApplicationProvider.getApplicationContext()
    private val density = 2f
    private val zone = ZoneId.of("Europe/Rome")
    private val now = LocalDateTime.parse("2026-11-15T14:00").atZone(zone).toInstant()

    /** The canvas's Milan: rain from 20:00 to 03:00, heaviest at 23:00. */
    private val hours = listOf(11.2, 11.8, 11.3, 10.2, 9.4, 8.9, 8.5, 8.1, 7.8, 7.5, 7.2, 6.9, 6.6, 6.3, 6.1, 6.0, 5.9, 6.0, 6.6, 7.9, 9.6, 10.9, 11.7, 12.1, 12.0)
        .mapIndexed { i, t ->
            val p = intArrayOf(5, 5, 5, 10, 10, 15, 30, 60, 75, 80, 70, 60, 45, 30, 20, 10, 5, 5, 5, 0, 0, 0, 0, 0, 0)[i]
            val a = doubleArrayOf(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.1, 0.6, 1.4, 1.8, 1.2, 0.7, 0.3, 0.1, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)[i]
            PictureHour(now.plus(Duration.ofHours(i.toLong())), t, p, a)
        }

    private fun pixel(bitmap: Bitmap, xDp: Float, yDp: Float): Int =
        bitmap.getPixel((xDp * density).roundToInt(), (yDp * density).roundToInt())

    private fun near(expected: Int, actual: Int, tolerance: Int = 3): Boolean =
        listOf(16, 8, 0).all { shift -> abs((expected shr shift and 0xFF) - (actual shr shift and 0xFF)) <= tolerance }

    @Test
    fun `the plate is painted at its size, with room around it`() {
        val window = PictureWindow.next(now, zone)
        val drawing = plateDrawing(window, hours, 312f, 3)
        val bitmap = PicturesPainter.plate(
            drawing, listOf(0f to Color.Blue, 1f to Color.Blue), PicturesInks.Sky,
            PicturesPainter.PlateText("12°", "6°", drawing.ticks.map { "x" }), density, 1f
        )
        val o = PlateMetrics.OVERHANG
        assertEquals(((312f + 2 * o) * density).roundToInt(), bitmap.width)
        assertEquals(((PlateMetrics.HEIGHT + 2 * o) * density).roundToInt(), bitmap.height)
        // A bar's middle wears its tone: 23:00 is 1.8 mm, light rain.
        val bar = drawing.bars.first { it.tone == RainTone.LIGHT }
        val tone = PicturesInks.Sky.rain[1].toArgb()
        assertTrue(near(tone, pixel(bitmap, o + bar.left + bar.width / 2, o + bar.top + bar.height / 2)))
        // «Adesso» is white on its point.
        val disc = drawing.now!!
        assertTrue(near(Color.White.toArgb(), pixel(bitmap, o + disc.x, o + disc.y)))
        // The ribbon is the stops it was given.
        assertTrue(near(Color.Blue.toArgb(), pixel(bitmap, o + 150f, o + PlateMetrics.RIBBON_TOP + 3f)))
    }

    @Test
    fun `the dial's spent hours are under their veil, the hours ahead are not`() {
        val drawing = dialDrawing(DialSpec.LARGE, now, zone, hours)
        val ring = listOf(0f to Color.Red, 1f to Color.Red)
        val bitmap = PicturesPainter.dial(drawing, ring, PicturesInks.Sky, density)
        val o = PicturesPainter.DialOverhang
        val r = DialSpec.LARGE.ringRadius
        // 06:00 is on the left, spent at 14:00; 18:00 is on the right, still ahead.
        val ahead = pixel(bitmap, o + 62f + r, o + 62f)
        val spent = pixel(bitmap, o + 62f - r, o + 62f)
        assertTrue(near(Color.Red.toArgb(), ahead))
        assertTrue("the veil darkens the spent ring", (spent shr 16 and 0xFF) < (ahead shr 16 and 0xFF) - 60)
    }

    @Test
    fun `the strip marks the rain over the ribbon`() {
        val window = PictureWindow.next(now, zone, StripMetrics.HOURS)
        val drawing = stripDrawing(window, hours, 240f)
        val bitmap = PicturesPainter.strip(
            drawing, listOf(0f to Color.Black, 1f to Color.Black), PicturesInks.Sky,
            drawing.ticks.map { "1" }, density, 1f
        )
        val o = PicturesPainter.StripOverhang
        val mark = drawing.marks.first()
        val wet = pixel(bitmap, o + mark.left + mark.width / 2, o + 3f)
        val dry = pixel(bitmap, o + 40f, o + 3f)
        assertTrue((wet shr 8 and 0xFF) > 150)
        assertTrue(near(Color.Black.toArgb(), dry))
    }

    @Test
    fun `the plate says in words what it draws`() {
        val window = PictureWindow.next(now, zone)
        val words = pictureDescription(app, window, hours, TemperatureUnit.CELSIUS, "11°", rain = true)
        assertEquals(
            "Prossime 24 ore: ora 11°, massima 12° alle 13:00, minima 6° alle 06:00. " +
                "Pioggia probabile dalle 20:00 alle 05:00, più probabile alle 23:00 (80%).",
            words
        )
        val dry = hours.map { it.copy(chancePct = 0) }
        assertTrue(pictureDescription(app, window, dry, TemperatureUnit.CELSIUS, "11°", rain = true).endsWith("Nessuna pioggia probabile."))
    }
}
