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
import com.callbackdev.chiaro.domain.model.CloudLayers
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
        val scene = scene(gradient, sunAltitudeDeg)
            .copy(deck = 0f, veil = 0f, stars = 0f, rain = 0f, snow = 0f, sun = null, moon = null)
        return render(gradient, scene, width, height, 0f, 0f)
    }

    private fun scene(
        gradient: SkyGradient,
        sunAltitudeDeg: Double,
        moonAltitudeDeg: Double = -20.0,
        moonIllumination: Double = 0.0,
        cloudPct: Int = 0,
        layers: CloudLayers? = null
    ) = skyScene(
        SkyPalette.Paper, gradient, sunAltitudeDeg, 180.0, moonAltitudeDeg, 150.0, moonIllumination, 90.0,
        false, cloudPct, layers, 0, SkyFall.NONE, 0.0, 15.0, 270
    )

    /** Drawn, and saved as a PNG under `CHIARO_RENDER_DIR` when that is set, so a sky the
     * shader draws can be looked at on a machine with no device (as `ArcPainterTest` does). */
    private fun render(gradient: SkyGradient, scene: SkyScene, w: Int, h: Int, bandTop: Float, bandHeight: Float, name: String = "sky"): Bitmap {
        val shader = RuntimeShader(SkyShaderSource)
        shader.bindSky(w.toFloat(), h.toFloat(), density, 0f, gradient, scene, bandTop, bandHeight)
        val bitmap = draw(shader, w, h)
        System.getenv("CHIARO_RENDER_DIR")?.let { dir ->
            val file = java.io.File(dir).apply { mkdirs() }.resolve("$name.png")
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        return bitmap
    }

    /** Where the shader places a body, the arithmetic of `place()` in Kotlin. */
    private fun placed(across: Float, up: Float, w: Int, bandTop: Float, bandHeight: Float): Pair<Int, Int> {
        val inset = w * EdgeInset
        val r = BodyRadius.value * density
        val x = inset + (w - 2 * inset) * across
        val y = bandTop + r + (bandHeight - 2 * r) * (1 - up)
        return x.roundToInt() to y.roundToInt()
    }

    private fun colorAt(bitmap: Bitmap, x: Int, y: Int): Color {
        val p = bitmap.getPixel(x, y)
        return Color(((p shr 16) and 0xFF) / 255f, ((p shr 8) and 0xFF) / 255f, (p and 0xFF) / 255f)
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

    // ------------------------------------------------------------- the sun and the moon (§8.1c)

    private val canvasW = 400 * 3
    private val canvasH = 352 * 3
    private val bandTop = 80f * 3
    private val bandHeight = 72f * 3

    /** The sun burns white inside its band, where no text stands; its glow is a circle that
     * ends at the band's edge, so the sky past the band is under the cap of §3.6 as if there
     * were no sun, and the sky beside the sun, at the band's own height, is a sky and not a
     * stripe of light. */
    @Test
    fun `the sun burns white in its band and the sky past the band stays under the cap`() {
        val gradient = SkyPalette.Paper.gradient(50.0)
        val scene = scene(gradient, 50.0)
        val bitmap = render(gradient, scene, canvasW, canvasH, bandTop, bandHeight, "sun-clear-noon")
        val (sx, sy) = placed(scene.sun!!.across, scene.sun!!.up, canvasW, bandTop, bandHeight)
        val centre = colorAt(bitmap, sx, sy)
        assertTrue("the disc's centre is white, not $centre", centre.red >= 0.96f && centre.green >= 0.96f && centre.blue >= 0.96f)
        // A few discs away the sky is still a sky: lit by the sun, not white.
        assertTrue(colorAt(bitmap, sx + (6 * BodyRadius.value * density).roundToInt(), sy).luminance() < 0.9f)
        val max = SkyPalette.Paper.brightestBottomStop().luminance()
        for (y in 0 until bandTop.toInt() step 6) for (x in 0 until canvasW step 30) {
            assertTrue("row $y, column $x", colorAt(bitmap, x, y).luminance() <= max + 0.01f)
        }
        // At the sun's own height, far from it, the sky is what the gradient says: no stripe.
        val ideal = colorAt(bitmap, 30, (bandTop + bandHeight + 40f * density).toInt()).luminance()
        val beside = colorAt(bitmap, 30, sy).luminance()
        assertTrue("beside the sun $beside, under the band $ideal", beside < ideal + 0.03f)
    }

    /** Under a closed low deck the disc is gone pixel by pixel and only a broad, faint
     * brightening says where the sun is: nothing near the disc's white. */
    @Test
    fun `a closed low deck hides the sun's disc`() {
        val gradient = SkyPalette.Paper.gradient(50.0, 100)
        val scene = scene(gradient, 50.0, cloudPct = 100, layers = CloudLayers(100, 0, 0))
        val bitmap = render(gradient, scene, canvasW, canvasH, bandTop, bandHeight, "sun-closed-deck")
        val (sx, sy) = placed(scene.sun!!.across, scene.sun!!.up, canvasW, bandTop, bandHeight)
        val centre = colorAt(bitmap, sx, sy).luminance()
        val away = colorAt(bitmap, sx + (8 * BodyRadius.value * density).roundToInt(), sy + (3 * BodyRadius.value * density).roundToInt()).luminance()
        assertTrue("centre $centre is not white", centre < 0.85f)
        assertTrue("the sun's place ($centre) is a little brighter than the deck around it ($away)", centre > away)
    }

    /** More skies, for the eye only: drawn when `CHIARO_RENDER_DIR` is set, asserted on
     * nothing but the draw itself. */
    @Test
    fun `the skies a reader meets, drawn for a look`() {
        if (System.getenv("CHIARO_RENDER_DIR") == null) return
        listOf(
            Triple("golden-hour-35-low", SkyPalette.Paper.gradient(3.0, 35), scene(SkyPalette.Paper.gradient(3.0, 35), 3.0, cloudPct = 35, layers = CloudLayers(35, 0, 0))),
            Triple("high-veil-closed", SkyPalette.Paper.gradient(50.0, 100), scene(SkyPalette.Paper.gradient(50.0, 100), 50.0, cloudPct = 100, layers = CloudLayers(0, 0, 100))),
            Triple("cumulus-50-sun-behind", SkyPalette.Paper.gradient(35.0, 50), scene(SkyPalette.Paper.gradient(35.0, 50), 35.0, cloudPct = 50, layers = CloudLayers(50, 0, 0))),
            Triple("full-moon-20-low", SkyPalette.Paper.gradient(-30.0, 20, 0, 1.0, 40.0), scene(SkyPalette.Paper.gradient(-30.0, 20, 0, 1.0, 40.0), -30.0, moonAltitudeDeg = 40.0, moonIllumination = 1.0, cloudPct = 20, layers = CloudLayers(20, 0, 0))),
            Triple("day-moon-gibbous", SkyPalette.Paper.gradient(40.0, 10), scene(SkyPalette.Paper.gradient(40.0, 10), 40.0, moonAltitudeDeg = 25.0, moonIllumination = 0.8, cloudPct = 10, layers = CloudLayers(10, 0, 0))),
            Triple("sunset-40-low", SkyPalette.Paper.gradient(-3.0, 40), scene(SkyPalette.Paper.gradient(-3.0, 40), -3.0, cloudPct = 40, layers = CloudLayers(40, 0, 0))),
            Triple("night-no-moon-40-low", SkyPalette.Paper.gradient(-30.0, 40), scene(SkyPalette.Paper.gradient(-30.0, 40), -30.0, cloudPct = 40, layers = CloudLayers(40, 0, 0))),
            Triple("rain-95", SkyPalette.Paper.gradient(40.0, 95, 90), skyScene(SkyPalette.Paper, SkyPalette.Paper.gradient(40.0, 95, 90), 40.0, 180.0, -20.0, 150.0, 0.0, 90.0, false, 95, CloudLayers(90, 40, 20), 90, SkyFall.RAIN, 3.0, 30.0, 270))
        ).forEach { (name, gradient, scene) -> render(gradient, scene, canvasW, canvasH, bandTop, bandHeight, name) }
    }

    /** The crescent is lit on the sun's side and brighter than any sky: the cap is open in
     * the band, and the dark part is only earthshine. */
    @Test
    fun `a crescent is lit on the sun's side`() {
        val gradient = SkyPalette.Paper.gradient(-30.0, 0, 0, 0.25, 40.0)
        val scene = scene(gradient, -30.0, moonAltitudeDeg = 40.0, moonIllumination = 0.25).copy(stars = 0f)
        assertTrue(scene.moon!!.litRight)
        val bitmap = render(gradient, scene, canvasW, canvasH, bandTop, bandHeight, "moon-crescent")
        val (mx, my) = placed(scene.moon!!.across, scene.moon!!.up, canvasW, bandTop, bandHeight)
        val r = BodyRadius.value * density * 0.9f
        val lit = colorAt(bitmap, mx + (0.6f * r).roundToInt(), my).luminance()
        val dark = colorAt(bitmap, mx - (0.6f * r).roundToInt(), my).luminance()
        val max = SkyPalette.Paper.brightestBottomStop().luminance()
        assertTrue("lit limb $lit against earthshine $dark", lit > dark * 3f)
        assertTrue("the moon ($lit) is brighter than the brightest sky ($max)", lit > max)
    }
}
