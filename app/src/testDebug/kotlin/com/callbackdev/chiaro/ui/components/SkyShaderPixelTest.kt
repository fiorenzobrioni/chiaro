package com.callbackdev.chiaro.ui.components

import android.graphics.Bitmap
import android.graphics.HardwareRenderer
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RenderNode
import android.graphics.RuntimeShader
import android.media.ImageReader
import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.callbackdev.chiaro.ui.theme.SkyGradient
import com.callbackdev.chiaro.ui.theme.SkyPalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The sky's shader, drawn by Skia and read back pixel by pixel (review of the sky, 19 ott
 * 2026): the shader composes in linear light and encodes the pixel itself, so what it prints
 * for the palette's own stops, for the gradient between them and for the dither is checked
 * here against the arithmetic in Kotlin, not trusted. Through a `RenderNode` and a
 * `HardwareRenderer`, the road the README's pictures take, because a software canvas refuses
 * a `RuntimeShader`.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w384dp-h832dp-xhdpi")
class SkyShaderPixelTest {

    /** A window, as the README's pictures have one: the hardware renderer draws into its
     * image only inside the graphics environment an activity's window sets up. */
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val width = 8
    private val height = 1056
    private val density = 3f

    /** The gradient alone, as a column of pixels: a scene with nothing drawn over it. */
    private fun column(gradient: SkyGradient, sunAltitudeDeg: Double): Bitmap {
        val palette = SkyPalette.Paper
        val scene = skyScene(
            palette, gradient, sunAltitudeDeg, 180.0, -20.0, 150.0, 0.0, false, 0,
            null, 0, SkyFall.NONE, 0.0, 15.0, 270
        ).copy(deck = 0f, veil = 0f, stars = 0f, rain = 0f, snow = 0f)
        val shader = RuntimeShader(SkyShaderSource)
        shader.bindSky(width.toFloat(), height.toFloat(), density, 0f, gradient, scene)
        return draw(shader, width, height)
    }

    /** [width] and [height] are parameters on purpose: inside `RenderNode.apply` the node's own
     * `width` and `height` (still zero) would shadow the class' and position a node of no size. */
    private fun draw(shader: RuntimeShader, width: Int, height: Int): Bitmap {
        val node = RenderNode("sky").apply { setPosition(0, 0, width, height) }
        compose.runOnUiThread {
            val canvas = node.beginRecording()
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), Paint().apply { this.shader = shader })
            node.endRecording()
        }
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

    /** The stop the shader would print at [y], by Kotlin's own arithmetic: the stops mixed as
     * linear light, encoded by the exact curve, in levels of 255 and not yet rounded. */
    private fun ideal(gradient: SkyGradient, y: Int, channel: (Color) -> Float): Float {
        val v = (y + 0.5f) / height
        val (a, b, t) = if (v < 0.5f) Triple(gradient.top, gradient.mid, v * 2f) else Triple(gradient.mid, gradient.bottom, v * 2f - 1f)
        val lin = toLinear(channel(a)) * (1f - t) + toLinear(channel(b)) * t
        return toSrgb(lin) * 255f
    }

    private fun red(bitmap: Bitmap, y: Int) = (bitmap.getPixel(width / 2, y) shr 16) and 0xFF
    private fun green(bitmap: Bitmap, y: Int) = (bitmap.getPixel(width / 2, y) shr 8) and 0xFF
    private fun blue(bitmap: Bitmap, y: Int) = bitmap.getPixel(width / 2, y) and 0xFF

    /** The palette's stops come back as themselves: linear in, the exact curve out, within
     * the one level the dither may add. */
    @Test
    fun `the stops print as the palette wrote them`() {
        val gradient = SkyPalette.Paper.gradient(50.0)
        val bitmap = column(gradient, 50.0)
        fun check(y: Int, stop: Color) {
            assertEquals("red at $y", (stop.red * 255).roundToInt().toDouble(), red(bitmap, y).toDouble(), 1.0)
            assertEquals("green at $y", (stop.green * 255).roundToInt().toDouble(), green(bitmap, y).toDouble(), 1.0)
            assertEquals("blue at $y", (stop.blue * 255).roundToInt().toDouble(), blue(bitmap, y).toDouble(), 1.0)
        }
        check(0, gradient.top)
        check(height / 2, gradient.mid)
        check(height - 1, gradient.bottom)
    }

    /** Between the stops the shader prints the linear-light mix, within a level, over the
     * whole height of a day and of a night: the curve in the shader is the one in Kotlin. */
    @Test
    fun `between the stops the pixel is the linear mix, encoded by the exact curve`() {
        listOf(50.0, -30.0).forEach { alt ->
            val gradient = SkyPalette.Paper.gradient(alt)
            val bitmap = column(gradient, alt)
            var worst = 0f
            for (y in 0 until height) {
                worst = maxOf(worst, abs(red(bitmap, y) - ideal(gradient, y) { it.red }))
                worst = maxOf(worst, abs(green(bitmap, y) - ideal(gradient, y) { it.green }))
                worst = maxOf(worst, abs(blue(bitmap, y) - ideal(gradient, y) { it.blue }))
            }
            assertTrue("worst error $worst levels at $alt°", worst <= 1.01f)
        }
    }

    /**
     * The dither: where the ideal value sits between two levels, the shader prints both, in
     * the share the value says, so a slow night gradient is a grain and not a staircase.
     * Measured on the rows whose ideal red lies within a tenth of a level of a midpoint.
     */
    @Test
    fun `a value between two levels prints as both, not as a step`() {
        val gradient = SkyPalette.Paper.gradient(-30.0)
        val bitmap = column(gradient, -30.0)
        var rows = 0
        var up = 0
        for (y in 0 until height) {
            val v = ideal(gradient, y) { it.blue }
            val frac = v - kotlin.math.floor(v)
            if (abs(frac - 0.5f) > 0.1f) continue
            for (x in 0 until width) {
                rows++
                if ((bitmap.getPixel(x, y) and 0xFF) > v) up++
            }
        }
        assertTrue("too few midpoint rows to measure: $rows", rows >= 200)
        val share = up.toFloat() / rows
        assertTrue("the upper level took $share of the midpoint pixels", share in 0.3f..0.7f)
    }

    /** The cap holds on the drawn pixel too: no pixel of the brightest day is over the
     * luminance of the palette's brightest stop, the dither's level aside. */
    @Test
    fun `no pixel is brighter than the brightest stop`() {
        val gradient = SkyPalette.Paper.gradient(50.0)
        val bitmap = column(gradient, 50.0)
        val max = SkyPalette.Paper.brightestBottomStop().luminance()
        for (y in 0 until height step 8) {
            val c = Color(red(bitmap, y) / 255f, green(bitmap, y) / 255f, blue(bitmap, y) / 255f)
            assertTrue("row $y", c.luminance() <= max + 0.01f)
        }
    }
}
