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
        SkyPalette.Paper, gradient, sunAltitudeDeg, 180.0, moonAltitudeDeg, 150.0, moonIllumination, 0.0,
        false, cloudPct, layers, 0, SkyFall.NONE, 0.0, 15.0, 270
    )

    /** Drawn, and saved as a PNG under `CHIARO_RENDER_DIR` when that is set, so a sky the
     * shader draws can be looked at on a machine with no device (as `ArcPainterTest` does). */
    private val starMap = StarMap()

    private fun render(
        gradient: SkyGradient, scene: SkyScene, w: Int, h: Int, bandTop: Float, bandHeight: Float, name: String = "sky",
        time: Float = 0f
    ): Bitmap {
        val shader = RuntimeShader(SkyShaderSource)
        val stars = starMap.shaderFor(scene.starField, w, h, density)
        shader.bindSky(w.toFloat(), h.toFloat(), density, time, gradient, scene, bandTop, bandHeight, stars)
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
            Triple("rain-95", SkyPalette.Paper.gradient(40.0, 95, 90), skyScene(SkyPalette.Paper, SkyPalette.Paper.gradient(40.0, 95, 90), 40.0, 180.0, -20.0, 150.0, 0.0, 90.0, false, 95, CloudLayers(90, 40, 20), 90, SkyFall.RAIN, 3.0, 30.0, 270)),
            Triple("snow-100", SkyPalette.Paper.gradient(10.0, 100, 85), skyScene(SkyPalette.Paper, SkyPalette.Paper.gradient(10.0, 100, 85), 10.0, 180.0, -20.0, 150.0, 0.0, 90.0, false, 100, CloudLayers(100, 0, 0), 85, SkyFall.SNOW, 1.5, 10.0, 270)),
            Triple("rain-night-drizzle", SkyPalette.Paper.gradient(-30.0, 100, 70), skyScene(SkyPalette.Paper, SkyPalette.Paper.gradient(-30.0, 100, 70), -30.0, 180.0, -20.0, 150.0, 0.0, 90.0, false, 100, CloudLayers(100, 0, 0), 70, SkyFall.RAIN, 0.4, 5.0, 90)),
            // The sky that is there: Milan on a January night at 22:00, Orion in the south, and
            // a July night at 01:00, with the real moon's limb; clear, so every star shows.
            realNight("stars-milan-january", "2026-01-15T21:00:00Z"),
            realNight("stars-milan-july", "2026-07-15T23:00:00Z"),
            realNight("stars-milan-october", "2026-10-19T19:00:00Z"),
            // The review of 9 nov 2026: the fog, the shafts, the low sun's side, the storm's deck.
            Triple("fog-morning", SkyPalette.Paper.gradient(12.0, 20), skyScene(SkyPalette.Paper, SkyPalette.Paper.gradient(12.0, 20), 12.0, 120.0, -20.0, 150.0, 0.0, 0.0, false, 20, CloudLayers(10, 0, 12), 0, SkyFall.NONE, 0.0, 2.0, 90, fog = 0.9f)),
            Triple("fog-night", SkyPalette.Paper.gradient(-30.0, 0), skyScene(SkyPalette.Paper, SkyPalette.Paper.gradient(-30.0, 0), -30.0, 0.0, -20.0, 150.0, 0.0, 0.0, false, 0, null, 0, SkyFall.NONE, 0.0, 2.0, 90, fog = 0.8f)),
            Triple("rays-broken-55", SkyPalette.Paper.gradient(25.0, 55), skyScene(SkyPalette.Paper, SkyPalette.Paper.gradient(25.0, 55), 25.0, 200.0, -20.0, 150.0, 0.0, 0.0, false, 55, CloudLayers(55, 0, 0), 0, SkyFall.NONE, 0.0, 12.0, 270)),
            Triple("low-sun-west", SkyPalette.Paper.gradient(1.5, 15), skyScene(SkyPalette.Paper, SkyPalette.Paper.gradient(1.5, 15), 1.5, 262.0, -20.0, 150.0, 0.0, 0.0, false, 15, CloudLayers(15, 0, 0), 0, SkyFall.NONE, 0.0, 8.0, 270)),
            Triple("dusk-west", SkyPalette.Paper.gradient(-4.0, 30), skyScene(SkyPalette.Paper, SkyPalette.Paper.gradient(-4.0, 30), -4.0, 268.0, -20.0, 150.0, 0.0, 0.0, false, 30, CloudLayers(30, 0, 0), 0, SkyFall.NONE, 0.0, 8.0, 270)),
            Triple("storm-dark", SkyPalette.Paper.gradient(35.0, 100, 95), skyScene(SkyPalette.Paper, SkyPalette.Paper.gradient(35.0, 100, 95), 35.0, 180.0, -20.0, 150.0, 0.0, 0.0, false, 100, CloudLayers(100, 0, 0), 95, SkyFall.RAIN, 6.0, 28.0, 250, thunder = true)),
            Triple("full-moon-seas", SkyPalette.Paper.gradient(-30.0, 0, 0, 1.0, 45.0), skyScene(SkyPalette.Paper, SkyPalette.Paper.gradient(-30.0, 0, 0, 1.0, 45.0), -30.0, 0.0, 45.0, 170.0, 1.0, 0.0, false, 0, null, 0, SkyFall.NONE, 0.0, 5.0, 270))
        ).forEach { (name, gradient, scene) -> render(gradient, scene, canvasW, canvasH, bandTop, bandHeight, name) }
        // A closed, wet deck at sunset (the owner's tablet, Huaraz, 9 nov 2026 at 18:06, three
        // minutes before sunset, drizzle): what the clouds wear when the sun is on the horizon
        // and none of it gets through.
        listOf(
            "sunset-drizzle-100" to (100 to CloudLayers(100, 60, 40)),
            "sunset-drizzle-95" to (95 to CloudLayers(90, 50, 30)),
            "sunset-drizzle-85" to (85 to CloudLayers(80, 30, 20)),
            "sunset-dry-95" to (95 to CloudLayers(90, 50, 30))
        ).forEach { (name, sky) ->
            val (cloud, layers) = sky
            val at = java.time.Instant.parse("2026-11-09T23:06:00Z")
            val huaraz = com.callbackdev.chiaro.domain.model.Coordinates(-9.53, -77.53)
            val engine = com.callbackdev.chiaro.domain.sky.AstronomyEngine
            val sunAlt = engine.sunAltitude(at, huaraz)
            val wet = !name.contains("dry")
            val gradient = SkyPalette.Vivid.gradient(sunAlt, cloud, if (wet) 94 else 10)
            val scene = skyScene(
                SkyPalette.Vivid, gradient, sunAlt, engine.sunAzimuth(at, huaraz), -20.0, 150.0, 0.0, 0.0, true, cloud, layers,
                if (wet) 94 else 10, if (wet) SkyFall.RAIN else SkyFall.NONE, 0.4, 6.0, 90
            )
            if (System.getenv("CHIARO_RENDER_DIR") != null) println("LOOK $name sunAlt $sunAlt deck ${scene.deck} lit ${scene.lit} shade ${scene.shade} top ${gradient.top} mid ${gradient.mid} bottom ${gradient.bottom}")
            render(gradient, scene, canvasW, canvasH, bandTop, bandHeight, name)
        }
        // The same storm at the height of its flash, and a snowfall a few seconds on.
        val stormGradient = SkyPalette.Paper.gradient(35.0, 100, 95)
        val storm = skyScene(SkyPalette.Paper, stormGradient, 35.0, 180.0, -20.0, 150.0, 0.0, 0.0, false, 100, CloudLayers(100, 0, 0), 95, SkyFall.RAIN, 6.0, 28.0, 250, thunder = true)
        render(stormGradient, storm, canvasW, canvasH, bandTop, bandHeight, "storm-flash", time = 0.62f)
        val snowGradient = SkyPalette.Paper.gradient(-8.0, 100, 85)
        val snow = skyScene(SkyPalette.Paper, snowGradient, -8.0, 285.0, -20.0, 150.0, 0.0, 0.0, false, 100, CloudLayers(100, 0, 0), 85, SkyFall.SNOW, 2.5, 9.0, 60)
        render(snowGradient, snow, canvasW, canvasH, bandTop, bandHeight, "snow-blue-hour", time = 7.3f)
    }

    // ------------------------------------------------ the review of the sky, 9 nov 2026

    private fun meanLuminance(bitmap: Bitmap, fromY: Int, toY: Int): Float {
        var sum = 0.0
        var n = 0
        for (y in fromY until toY step 12) for (x in 6 until canvasW step 24) {
            sum += colorAt(bitmap, x, y).luminance()
            n++
        }
        return (sum / n).toFloat()
    }

    /** Under the word «Fog» the canvas draws a fog: the clear blue above is gone behind the
     * bank, most at the foot, and the bank is the scene's own colour. */
    @Test
    fun `a fog covers the sky, thicker toward the ground`() {
        val gradient = SkyPalette.Paper.gradient(40.0, 0)
        val clear = scene(gradient, 40.0).copy(sun = null)
        val foggy = skyScene(
            SkyPalette.Paper, gradient, 40.0, 180.0, -20.0, 150.0, 0.0, 0.0, false, 0, null, 0, SkyFall.NONE, 0.0, 2.0, 90, fog = 1f
        ).copy(sun = null)
        val open = render(gradient, clear, canvasW, canvasH, bandTop, bandHeight)
        val bank = render(gradient, foggy, canvasW, canvasH, bandTop, bandHeight, "fog-full")
        fun blueness(c: Color) = c.blue - c.red
        val x = canvasW / 2
        val top = canvasH / 10
        val foot = canvasH * 9 / 10
        assertTrue("the open sky is blue", blueness(colorAt(open, x, top)) > 0.3f)
        assertTrue("the fog took most of it at the top", blueness(colorAt(bank, x, top)) < blueness(colorAt(open, x, top)) * 0.75f)
        val low = colorAt(bank, x, foot)
        assertTrue("at the foot it is the fog's own colour, nearly a grey: $low", blueness(low) < 0.08f)
        assertEquals(foggy.fogColor.luminance(), low.luminance(), 0.06f)
        // And still under the cap.
        val max = SkyPalette.Paper.brightestBottomStop().luminance()
        for (y in 0 until canvasH step 16) assertTrue(colorAt(bank, x, y).luminance() <= max + 0.01f)
    }

    /** The flash lights the deck from inside for a moment and is gone; outside the bodies'
     * band no pixel of it is over the cap, and a still sky (time zero) has none. */
    @Test
    fun `a thunderstorm's deck flashes and goes dark again, under the cap`() {
        val gradient = SkyPalette.Paper.gradient(35.0, 100, 95)
        val storm = skyScene(
            SkyPalette.Paper, gradient, 35.0, 180.0, -20.0, 150.0, 0.0, 0.0, false, 100, CloudLayers(100, 0, 0), 95,
            SkyFall.RAIN, 6.0, 28.0, 250, thunder = true
        ).copy(rain = 0f, sun = null)
        val still = render(gradient, storm, canvasW, canvasH, bandTop, bandHeight)
        val lit = render(gradient, storm, canvasW, canvasH, bandTop, bandHeight, time = 0.62f)
        val after = render(gradient, storm, canvasW, canvasH, bandTop, bandHeight, time = 2.5f)
        val from = (bandTop + bandHeight).toInt() + 30
        val dark = meanLuminance(still, from, canvasH)
        val flash = meanLuminance(lit, from, canvasH)
        assertTrue("the deck in the flash ($flash) over the deck at rest ($dark)", flash > dark * 1.25f)
        assertEquals("two seconds on it is dark again", dark, meanLuminance(after, from, canvasH), 0.01f)
        val max = SkyPalette.Paper.brightestBottomStop().luminance()
        for (y in from until canvasH step 10) for (x in 0 until canvasW step 30) {
            assertTrue("row $y, column $x", colorAt(lit, x, y).luminance() <= max + 0.01f)
        }
        // The same deck with no storm in it does not move for the clock.
        val calm = storm.copy(storm = 0f)
        assertEquals(
            meanLuminance(render(gradient, calm, canvasW, canvasH, bandTop, bandHeight), from, canvasH),
            meanLuminance(render(gradient, calm, canvasW, canvasH, bandTop, bandHeight, time = 0.62f), from, canvasH),
            0.01f
        )
    }

    /** At sunset the horizon is brighter on the sun's side than on the other: the sky has two
     * dimensions. At noon the two sides are one. */
    @Test
    fun `toward a low sun the horizon is brighter than away from it`() {
        val gradient = SkyPalette.Paper.gradient(-2.0, 0)
        val dusk = skyScene(
            SkyPalette.Paper, gradient, -2.0, 300.0, -20.0, 150.0, 0.0, 0.0, false, 0, null, 0, SkyFall.NONE, 0.0, 5.0, 270
        )
        assertTrue(dusk.lowSun > 0.9f && dusk.sunAcross > 0.85f)
        val bitmap = render(gradient, dusk, canvasW, canvasH, bandTop, bandHeight, "low-sun-two-sides")
        val y = canvasH * 85 / 100
        val sunSide = colorAt(bitmap, canvasW * 9 / 10, y).luminance()
        val farSide = colorAt(bitmap, canvasW / 10, y).luminance()
        assertTrue("the sun's side ($sunSide) over the far side ($farSide)", sunSide > farSide * 1.2f)
        val day = SkyPalette.Paper.gradient(50.0, 0)
        val noon = render(day, scene(day, 50.0).copy(sun = null), canvasW, canvasH, bandTop, bandHeight)
        assertEquals(colorAt(noon, canvasW / 10, y).luminance(), colorAt(noon, canvasW * 9 / 10, y).luminance(), 0.01f)
    }

    /** The full moon has its seas: Imbrium is darker than the southern highlands, and the
     * face turns with its north. */
    @Test
    fun `the moon has its seas, and they turn with its north`() {
        val gradient = SkyPalette.Paper.gradient(-30.0, 0, 0, 1.0, 45.0)
        fun moon(north: Double) = skyScene(
            SkyPalette.Paper, gradient, -30.0, 0.0, 45.0, 170.0, 1.0, 0.0, false, 0, null, 0, SkyFall.NONE, 0.0, 5.0, 270,
            moonNorthAngleDeg = north
        ).copy(stars = 0f)
        val upright = moon(90.0)
        val bitmap = render(gradient, upright, canvasW, canvasH, bandTop, bandHeight, "moon-seas-upright")
        val (mx, my) = placed(upright.moon!!.across, upright.moon!!.up, canvasW, bandTop, bandHeight)
        val r = BodyRadius.value * density * 0.9f
        // Imbrium, up and to the left; the highlands, down and a little to the right.
        fun at(b: Bitmap, u: Float, v: Float) = colorAt(b, mx + (u * r).roundToInt(), my - (v * r).roundToInt()).luminance()
        val sea = at(bitmap, -0.25f, 0.50f)
        val highland = at(bitmap, 0.30f, -0.70f)
        assertTrue("Imbrium ($sea) is darker than the highlands ($highland)", sea < highland * 0.93f)
        assertTrue("and the moon is still the brightest thing in the sky", highland > SkyPalette.Paper.brightestBottomStop().luminance())
        // North to the left: the picture turned a quarter, Imbrium is now down and to the left.
        val turned = render(gradient, moon(180.0), canvasW, canvasH, bandTop, bandHeight, "moon-seas-turned")
        assertTrue(at(turned, -0.50f, -0.25f) < at(turned, 0.70f, 0.30f) * 0.93f)
    }

    /** On a peak night a trail crosses the sky once in a period, away from the radiant; on
     * any other night the same clock draws nothing. */
    @Test
    fun `a meteor crosses a peak night's sky, and no other`() {
        val gradient = SkyPalette.Paper.gradient(-30.0)
        val plain = scene(gradient, -30.0).copy(stars = 0f)
        val peak = plain.copy(meteors = 1f, radiant = androidx.compose.ui.geometry.Offset(10f, 50f), meteorSeed = 60f)
        fun brightest(b: Bitmap): Float {
            var max = 0f
            for (y in 0 until canvasH step 3) for (x in 0 until canvasW step 3) max = maxOf(max, colorAt(b, x, y).luminance())
            return max
        }
        val sky = brightest(render(gradient, plain, canvasW, canvasH, bandTop, bandHeight))
        var seen = 0
        var best = 0f
        var t = 2f
        while (t < MeteorPeriodSeconds) {
            val frame = brightest(render(gradient, peak, canvasW, canvasH, bandTop, bandHeight, time = t))
            if (frame > sky + 0.2f) seen++
            best = maxOf(best, frame)
            assertEquals("no trail on a night that is no shower's, at $t s", sky, brightest(render(gradient, plain, canvasW, canvasH, bandTop, bandHeight, time = t)), 0.01f)
            t += 0.35f
        }
        assertTrue("a trail was seen ($best over $sky)", seen >= 1)
        assertTrue("and it lasted a second, not the whole period: $seen frames of ${(MeteorPeriodSeconds / 0.35f).toInt()}", seen <= 4)
    }

    /**
     * The scrim is as strong as the sky needs it ([scrimNeed], 9 nov 2026), and the need is
     * read on what the sky is computed to draw; this reads it on what the shader did draw.
     * Over every pixel outside the bodies' band, white on the scrim at the strength this sky
     * was given clears 4.5:1: a moonlit night with clouds, a moonless one with its Milky Way,
     * a night of rain, a dusk, a day.
     */
    @Test
    fun `white clears the floor over every drawn pixel, at the scrim's strength for that sky`() {
        fun lin(c: Float) = toLinear(c)
        fun contrastOverScrim(pixel: Color, veil: Color, a: Float): Double {
            fun mix(f: Float, b: Float) = f * a + b * (1 - a)
            val l = 0.2126 * lin(mix(veil.red, pixel.red)) + 0.7152 * lin(mix(veil.green, pixel.green)) + 0.0722 * lin(mix(veil.blue, pixel.blue))
            return 1.05 / (l + 0.05)
        }
        val skies = listOf(
            "moonlit clouds" to skyScene(SkyPalette.Paper, SkyPalette.Paper.gradient(-30.0, 40, 0, 1.0, 50.0), -30.0, 0.0, 50.0, 170.0, 1.0, 0.0, false, 40, CloudLayers(40, 0, 0), 0, SkyFall.NONE, 0.0, 10.0, 270),
            "moonless clear" to skyScene(SkyPalette.Vivid, SkyPalette.Vivid.gradient(-30.0), -30.0, 0.0, -20.0, 170.0, 0.0, 0.0, false, 0, null, 0, SkyFall.NONE, 0.0, 10.0, 270, galactic = SkyFrame(1f, 0f, 0f, 0f, 0f, 1f)),
            "night rain" to skyScene(SkyPalette.Paper, SkyPalette.Paper.gradient(-30.0, 100, 90, 0.6, 30.0), -30.0, 0.0, 30.0, 170.0, 0.6, 0.0, false, 100, CloudLayers(100, 0, 0), 90, SkyFall.RAIN, 4.0, 20.0, 270),
            "nautical dusk" to skyScene(SkyPalette.Vivid, SkyPalette.Vivid.gradient(-10.0, 30), -10.0, 280.0, -20.0, 170.0, 0.0, 0.0, false, 30, CloudLayers(30, 0, 0), 0, SkyFall.NONE, 0.0, 10.0, 270),
            "noon cumulus" to skyScene(SkyPalette.Paper, SkyPalette.Paper.gradient(50.0, 50), 50.0, 180.0, -20.0, 170.0, 0.0, 0.0, false, 50, CloudLayers(50, 0, 0), 0, SkyFall.NONE, 0.0, 10.0, 270),
            "moonlit veil" to skyScene(SkyPalette.Vivid, SkyPalette.Vivid.gradient(-25.0, 60, 0, 1.0, 50.0), -25.0, 0.0, 50.0, 170.0, 1.0, 0.0, false, 60, CloudLayers(0, 0, 60), 0, SkyFall.NONE, 0.0, 10.0, 270),
            "moonlit closed veil" to skyScene(SkyPalette.Paper, SkyPalette.Paper.gradient(-25.0, 100, 0, 1.0, 50.0), -25.0, 0.0, 50.0, 170.0, 1.0, 0.0, false, 100, CloudLayers(0, 0, 100), 0, SkyFall.NONE, 0.0, 10.0, 270)
        )
        val gradients = listOf(
            SkyPalette.Paper.gradient(-30.0, 40, 0, 1.0, 50.0), SkyPalette.Vivid.gradient(-30.0),
            SkyPalette.Paper.gradient(-30.0, 100, 90, 0.6, 30.0), SkyPalette.Vivid.gradient(-10.0, 30), SkyPalette.Paper.gradient(50.0, 50),
            SkyPalette.Vivid.gradient(-25.0, 60, 0, 1.0, 50.0), SkyPalette.Paper.gradient(-25.0, 100, 0, 1.0, 50.0)
        )
        skies.forEachIndexed { i, (name, scene) ->
            val gradient = gradients[i]
            val need = scrimNeed(gradient, scene)
            val veil = SkyPalette.scrimFor(gradient)
            val bitmap = render(gradient, scene, canvasW, canvasH, bandTop, bandHeight, "need-${name.replace(' ', '-')}")
            var worst = Double.MAX_VALUE
            for (y in (bandTop + bandHeight).toInt() + 30 until canvasH step 7) for (x in 0 until canvasW step 11) {
                worst = minOf(worst, contrastOverScrim(colorAt(bitmap, x, y), veil, SkyPalette.ScrimAlpha * need))
            }
            assertTrue("$name: need $need, worst contrast %.2f:1".format(worst), worst >= 4.5)
        }
        // And the need is what it says: none at night, all of it by day.
        assertEquals(0f, scrimNeed(gradients[1], skies[1].second), 0f)
        assertEquals(1f, scrimNeed(gradients[4], skies[4].second), 0f)
    }

    /**
     * A closed deck (10 nov 2026, the owner's tablet: a drizzle at sunset drew clouds of one
     * tone with their gaps cut out of them, and an orange smudge where the sun stood). It has
     * its lights and darks, a sixth either way; a sun on the horizon leaves no patch on it,
     * where a high one does; and its gaps thin out instead of being cut.
     */
    @Test
    fun `a closed deck has lights and darks, soft gaps, and no sun on it at the horizon`() {
        fun deviation(bitmap: Bitmap): Float {
            val values = ArrayList<Float>()
            for (y in (bandTop + bandHeight).toInt() + 30 until canvasH * 3 / 4 step 9) for (x in 20 until canvasW - 20 step 13) {
                values += colorAt(bitmap, x, y).luminance()
            }
            val mean = values.average()
            return (kotlin.math.sqrt(values.sumOf { (it - mean) * (it - mean) } / values.size) / mean).toFloat()
        }
        // Noon, closed and dry, no sun drawn: only the deck's own tone varies.
        val noon = SkyPalette.Paper.gradient(50.0, 100)
        val closed = scene(noon, 50.0, cloudPct = 100, layers = CloudLayers(100, 0, 0)).copy(sun = null, glow = 0f)
        val drawn = render(noon, closed, canvasW, canvasH, bandTop, bandHeight, "closed-deck-tones")
        val tones = deviation(drawn)
        assertTrue("the deck's tone varies by ${tones * 100}% of its mean", tones > 0.035f && tones < 0.15f)

        // Sunset, closed: the sun's place is no brighter than the deck beside it.
        val dusk = SkyPalette.Paper.gradient(0.2, 100)
        val low = skyScene(
            SkyPalette.Paper, dusk, 0.2, 250.0, -20.0, 150.0, 0.0, 0.0, false, 100, CloudLayers(100, 0, 0), 0, SkyFall.NONE, 0.0, 6.0, 90
        )
        val sunset = render(dusk, low, canvasW, canvasH, bandTop, bandHeight, "closed-deck-sunset")
        val without = render(dusk, low.copy(sun = null), canvasW, canvasH, bandTop, bandHeight)
        val (sx, sy) = placed(low.sun!!.across, low.sun!!.up, canvasW, bandTop, bandHeight)
        assertEquals(
            "a sun on the horizon leaves no patch on a closed deck",
            colorAt(without, sx, sy).luminance(), colorAt(sunset, sx, sy).luminance(), 0.02f
        )

        // Nearly closed: across a gap's border the tone changes over tens of pixels, not two.
        val gappy = scene(noon, 50.0, cloudPct = 92, layers = CloudLayers(92, 0, 0)).copy(sun = null, glow = 0f)
        val soft = render(SkyPalette.Paper.gradient(50.0, 92), gappy, canvasW, canvasH, bandTop, bandHeight, "closed-deck-gaps")
        var steepest = 0f
        for (y in (bandTop + bandHeight).toInt() + 30 until canvasH * 3 / 4 step 5) for (x in 20 until canvasW - 24 step 3) {
            steepest = maxOf(steepest, abs(colorAt(soft, x + 3, y).luminance() - colorAt(soft, x, y).luminance()))
        }
        assertTrue("the steepest step in a dp is $steepest of luminance", steepest < 0.05f)
    }

    /** The clouds change shape in time, they do not only drift: with the wind held at nothing
     * the deck a minute on is another deck, and as much of it. */
    @Test
    fun `the clouds evolve where they stand`() {
        val gradient = SkyPalette.Paper.gradient(50.0, 50)
        val deck = scene(gradient, 50.0, cloudPct = 50, layers = CloudLayers(50, 0, 0)).copy(sun = null, glow = 0f, drift = 0f, rays = 0f)
        val now = render(gradient, deck, canvasW, canvasH, bandTop, bandHeight, "evolve-0")
        val later = render(gradient, deck, canvasW, canvasH, bandTop, bandHeight, "evolve-60", time = 60f)
        var moved = 0.0
        var n = 0
        for (y in 20 until canvasH / 2 step 10) for (x in 20 until canvasW step 20) {
            moved += abs(colorAt(now, x, y).luminance() - colorAt(later, x, y).luminance())
            n++
        }
        assertTrue("the deck changed: ${moved / n}", moved / n > 0.01)
        assertEquals("and there is as much of it", meanLuminance(now, 0, canvasH), meanLuminance(later, 0, canvasH), 0.03f)
    }

    /** Through a broken deck the shafts light the open ways and shade the others, and the sky
     * past the band stays under the cap. */
    @Test
    fun `the sun's shafts change the air under a broken deck, under the cap`() {
        val gradient = SkyPalette.Paper.gradient(25.0, 55)
        val broken = skyScene(
            SkyPalette.Paper, gradient, 25.0, 200.0, -20.0, 150.0, 0.0, 0.0, false, 55, CloudLayers(55, 0, 0), 0, SkyFall.NONE, 0.0, 12.0, 270
        )
        assertTrue(broken.rays > 0.4f)
        val with = render(gradient, broken, canvasW, canvasH, bandTop, bandHeight, "rays-on")
        val without = render(gradient, broken.copy(rays = 0f), canvasW, canvasH, bandTop, bandHeight, "rays-off")
        var changed = 0.0
        var n = 0
        val max = SkyPalette.Paper.brightestBottomStop().luminance()
        val from = (bandTop + bandHeight).toInt() + 30
        for (y in from until canvasH step 10) for (x in 0 until canvasW step 20) {
            val l = colorAt(with, x, y).luminance()
            assertTrue("row $y, column $x", l <= max + 0.01f)
            changed += abs(l - colorAt(without, x, y).luminance())
            n++
        }
        assertTrue("the shafts are there: ${changed / n}", changed / n > 0.004)
    }

    /** A star of the field is drawn where the camera puts it, as bright as it is, at night
     * only; and a turned moon is lit where its limb angle says (step 6, 19 ott 2026). */
    @Test
    fun `a star is drawn where the field puts it, and only at night`() {
        val field = listOf(
            StarMark(acrossDeg = 0f, altitudeDeg = 30f, brightness = 1f, warmth = 0.1f, phase = 0.3f),
            StarMark(acrossDeg = -8f, altitudeDeg = 40f, brightness = 0.09f, warmth = 0.9f, phase = 0.7f)
        )
        val night = SkyPalette.Paper.gradient(-30.0)
        val scene = scene(night, -30.0).copy(starField = field)
        val bitmap = render(night, scene, canvasW, canvasH, bandTop, bandHeight, "stars-two")
        val (sx, sy) = starPlace(field[0], canvasW, canvasH)!!
        val (fx, fy) = starPlace(field[1], canvasW, canvasH)!!
        val sky = colorAt(bitmap, sx.toInt() + 40, sy.toInt() + 40).luminance()
        val bright = colorAt(bitmap, sx.toInt(), sy.toInt()).luminance()
        val faint = colorAt(bitmap, fx.toInt(), fy.toInt()).luminance()
        assertTrue("Sirius-bright star $bright over the sky $sky", bright > sky + 0.3f)
        assertTrue("a speck of the dust $faint, still there over $sky", faint > sky + 0.005f && faint < bright)
        // By day the same field draws nothing: uStars is zero.
        val day = SkyPalette.Paper.gradient(50.0)
        val dayBitmap = render(day, scene(day, 50.0).copy(starField = field), canvasW, canvasH, bandTop, bandHeight)
        assertEquals(colorAt(dayBitmap, sx.toInt() + 40, sy.toInt()).luminance(), colorAt(dayBitmap, sx.toInt(), sy.toInt()).luminance(), 0.01f)
    }

    @Test
    fun `a moon lit from below has its bright limb at the bottom`() {
        val gradient = SkyPalette.Paper.gradient(-30.0, 0, 0, 0.3, 40.0)
        val scene = skyScene(
            SkyPalette.Paper, gradient, -30.0, 300.0, 40.0, 150.0, 0.3, -90.0, false, 0, null, 0, SkyFall.NONE, 0.0, 15.0, 270
        ).copy(stars = 0f)
        val bitmap = render(gradient, scene, canvasW, canvasH, bandTop, bandHeight, "moon-lit-below")
        val (mx, my) = placed(scene.moon!!.across, scene.moon!!.up, canvasW, bandTop, bandHeight)
        val r = BodyRadius.value * density * 0.9f
        val below = colorAt(bitmap, mx, my + (0.6f * r).roundToInt()).luminance()
        val above = colorAt(bitmap, mx, my - (0.6f * r).roundToInt()).luminance()
        assertTrue("lit below $below, dark above $above", below > above * 3f)
    }

    /** A clear night over Milan at [iso], with the engine's sun and moon and the catalogue's
     * stars, for the eye. */
    private fun realNight(name: String, iso: String): Triple<String, SkyGradient, SkyScene> {
        val at = java.time.Instant.parse(iso)
        val milan = com.callbackdev.chiaro.domain.model.Coordinates(45.4642, 9.19)
        val engine = com.callbackdev.chiaro.domain.sky.AstronomyEngine
        val sunAlt = engine.sunAltitude(at, milan)
        val moonAlt = engine.moonAltitude(at, milan)
        val illumination = engine.moonIllumination(at).illuminatedFraction
        val gradient = SkyPalette.Paper.gradient(sunAlt, 0, 0, illumination, moonAlt)
        val scene = skyScene(
            SkyPalette.Paper, gradient, sunAlt, engine.sunAzimuth(at, milan), moonAlt,
            engine.bodyAzimuth(com.callbackdev.chiaro.domain.sky.SkyBody.MOON, at, milan), illumination,
            engine.moonBrightLimbAngle(at, milan), false, 0, null, 0, SkyFall.NONE, 0.0, 10.0, 270,
            starMarks = StarField.visible(at, milan, southern = false),
            galactic = StarField.galacticFrame(at, milan, 180.0)
        )
        return Triple(name, gradient, scene)
    }

    /** The crescent is lit on the sun's side and brighter than any sky: the cap is open in
     * the band, and the dark part is only earthshine. Read on the highlands of the lit limb,
     * down and to the right, since the face has its seas (9 nov 2026): straight to the right
     * stand Tranquillitatis and Fecunditatis, a fifth darker by design. */
    @Test
    fun `a crescent is lit on the sun's side`() {
        val gradient = SkyPalette.Paper.gradient(-30.0, 0, 0, 0.25, 40.0)
        val scene = scene(gradient, -30.0, moonAltitudeDeg = 40.0, moonIllumination = 0.25).copy(stars = 0f)
        assertEquals(0f, scene.moon!!.limbAngleDeg, 1e-6f)
        val bitmap = render(gradient, scene, canvasW, canvasH, bandTop, bandHeight, "moon-crescent")
        val (mx, my) = placed(scene.moon!!.across, scene.moon!!.up, canvasW, bandTop, bandHeight)
        val r = BodyRadius.value * density * 0.9f
        val lit = colorAt(bitmap, mx + (0.5f * r).roundToInt(), my + (0.6f * r).roundToInt()).luminance()
        val dark = colorAt(bitmap, mx - (0.6f * r).roundToInt(), my).luminance()
        val max = SkyPalette.Paper.brightestBottomStop().luminance()
        assertTrue("lit limb $lit against earthshine $dark", lit > dark * 3f)
        assertTrue("the moon ($lit) is brighter than the brightest sky ($max)", lit > max)
    }

    /** The Milky Way is a band where the galactic plane crosses the window, and nothing by day:
     * a frame whose plane runs up the middle of the canvas against one whose plane misses it. */
    @Test
    fun `the Milky Way crosses where the plane is, and only in the dark`() {
        val gradient = SkyPalette.Paper.gradient(-30.0, 0, 0, 0.0, -20.0)
        // The pole to the right: the plane is the vertical plane through the camera's axis.
        val across = SkyFrame(1f, 0f, 0f, 0f, 0f, 1f)
        // The pole ahead: the plane is the ring around the camera, far from its middle.
        val away = SkyFrame(0f, 0f, 1f, 1f, 0f, 0f)
        val base = scene(gradient, -30.0).copy(stars = 0f)
        val with = render(gradient, skyScene(
            SkyPalette.Paper, gradient, -30.0, 180.0, -20.0, 150.0, 0.0, 0.0, false, 0, null, 0, SkyFall.NONE, 0.0, 15.0, 270,
            galactic = across
        ).copy(stars = 0f), canvasW, canvasH, bandTop, bandHeight, "milky-way-across")
        val without = render(gradient, skyScene(
            SkyPalette.Paper, gradient, -30.0, 180.0, -20.0, 150.0, 0.0, 0.0, false, 0, null, 0, SkyFall.NONE, 0.0, 15.0, 270,
            galactic = away
        ).copy(stars = 0f), canvasW, canvasH, bandTop, bandHeight, "milky-way-away")
        val x = canvasW / 2
        val y = canvasH / 2
        val band = colorAt(with, x, y).luminance()
        val plain = colorAt(without, x, y).luminance()
        assertTrue("the band ($band) over the plain sky ($plain)", band > plain + 0.01f)
        // Off the plane, the two agree: the band is a band, not a tint over the whole sky.
        assertEquals(colorAt(without, 40, y).luminance(), colorAt(with, 40, y).luminance(), 0.012f)
        assertTrue(base.milkyWay == 0f)
        // By day the frame is carried and the band is off.
        val day = SkyPalette.Paper.gradient(30.0, 0, 0, 0.0, -20.0)
        val noon = skyScene(SkyPalette.Paper, day, 30.0, 180.0, -20.0, 150.0, 0.0, 0.0, false, 0, null, 0, SkyFall.NONE, 0.0, 15.0, 270, galactic = across)
        assertEquals(0f, noon.milkyWay, 0f)
    }

    /** The degrees of a turn worth [shift] px at the stars' camera's focal length. */
    private fun turnWorth(shift: Int): Float {
        val focal = canvasH / Math.toRadians((StarCameraTopDeg - StarCameraBottomDeg).toDouble())
        return Math.toDegrees(shift / focal).toFloat()
    }

    /** A turned window moves the veil and the rain, flat sheets, by the pixels a star moves for
     * the same turn, the stars' camera's focal length (25 ott 2026): a camera turning has no
     * parallax, so the picture turns as one. By day, where the clouds have contrast, with the
     * sun's disc and the rim its place would draw taken off: the light's direction is one for
     * the whole canvas and turns nothing, its halo would (a moonless night, where no light
     * fixes anything, has clouds a hundredth of a step from its sky, and shows nothing). */
    @Test
    fun `a turned window moves the veil and the rain by the pixels a star moves`() {
        val gradient = SkyPalette.Paper.gradient(50.0, 30, 30)
        val base = skyScene(
            SkyPalette.Paper, gradient, 50.0, 180.0, -20.0, 150.0, 0.0, 0.0, false, 30, CloudLayers(0, 0, 30), 60,
            SkyFall.RAIN, 2.0, 0.0, 0
        ).copy(sun = null, glow = 0f, stars = 0f)
        assertEquals("no deck: the sheets alone", 0f, base.deck, 0f)
        assertTrue("a veil and rain", base.veil > 0.2f && base.rain > 0.3f)
        val shift = 60
        val still = render(gradient, base, canvasW, canvasH, bandTop, bandHeight, "turn-sheets-still")
        val turned = render(gradient, base.copy(turn = turnWorth(shift)), canvasW, canvasH, bandTop, bandHeight, "turn-sheets-right")
        var worst = 0f
        var moved = 0f
        for (y in listOf(canvasH / 5, canvasH / 2, canvasH * 4 / 5)) {
            for (x in 12 until canvasW - shift - 12 step 4) {
                val here = colorAt(turned, x, y).luminance()
                worst = maxOf(worst, abs(here - colorAt(still, x + shift, y).luminance()))
                moved = maxOf(moved, abs(here - colorAt(still, x, y).luminance()))
            }
        }
        assertTrue("turned right, the sheets read as the still ones $shift px over: off by $worst", worst < 0.01f)
        assertTrue("and they did move: $moved", moved > 0.05f)
    }

    /** The mean step of luminance across and down the canvas, 4 px apart, and the mean
     * luminance: the grain of the clouds. A deck read as lines has a step across near zero. */
    private fun grain(bitmap: Bitmap): Triple<Float, Float, Float> {
        var across = 0.0
        var down = 0.0
        var mean = 0.0
        var n = 0
        for (y in 8 until canvasH - 8 step 6) {
            for (x in 8 until canvasW - 8 step 6) {
                val here = colorAt(bitmap, x, y).luminance()
                across += abs(colorAt(bitmap, x + 4, y).luminance() - here)
                down += abs(colorAt(bitmap, x, y + 4).luminance() - here)
                mean += here
                n++
            }
        }
        return Triple((across / n).toFloat(), (down / n).toFloat(), (mean / n).toFloat())
    }

    /** The deck is a plane under a camera, and a turned window turns that camera (25 ott 2026,
     * the second cut): by the angle that moves its centre by the pixels a star moves, so at the
     * centre the clouds read as the still sky that many pixels over, within the pinhole's own
     * curvature a row and a half from the centre; and five circles on the clouds are still
     * clouds, their grain across the canvas the still deck's within a third, and as much of
     * them. The first cut read the still picture that many pixels further along, a plane seen
     * far off a pinhole's axis, and by the second circle the deck was horizontal lines
     * (committente, from the device, 25 ott 2026). */
    @Test
    fun `a turned window turns the deck's camera, and five circles on the clouds are still clouds`() {
        val gradient = SkyPalette.Paper.gradient(50.0, 60, 60)
        val base = skyScene(
            SkyPalette.Paper, gradient, 50.0, 180.0, -20.0, 150.0, 0.0, 0.0, false, 60, null, 0,
            SkyFall.NONE, 0.0, 0.0, 0
        ).copy(sun = null, glow = 0f, stars = 0f)
        assertEquals("the deck alone", 0f, base.veil, 0f)
        val shift = 60
        val still = render(gradient, base, canvasW, canvasH, bandTop, bandHeight, "turn-deck-still")
        val turned = render(gradient, base.copy(turn = turnWorth(shift)), canvasW, canvasH, bandTop, bandHeight, "turn-deck-right")
        val circles = render(gradient, base.copy(turn = 5 * 360f), canvasW, canvasH, bandTop, bandHeight, "turn-deck-circles")
        var worst = 0f
        var moved = 0f
        val middle = canvasW / 2
        for (y in listOf(canvasH / 5, canvasH / 2, canvasH * 4 / 5)) {
            for (x in middle - 60 until middle + 60 step 4) {
                val here = colorAt(turned, x, y).luminance()
                worst = maxOf(worst, abs(here - colorAt(still, x + shift, y).luminance()))
                moved = maxOf(moved, abs(here - colorAt(still, x, y).luminance()))
            }
        }
        assertTrue("turned right, the deck's centre reads as the still one $shift px over: off by $worst", worst < 0.03f)
        assertTrue("and it did move: $moved", moved > 0.05f)
        val (stillAcross, stillDown, stillMean) = grain(still)
        val (farAcross, farDown, farMean) = grain(circles)
        val stillGrain = stillAcross / stillDown
        val farGrain = farAcross / farDown
        if (System.getenv("CHIARO_RENDER_DIR") != null) {
            println("deck turn: centre off by $worst, moved $moved; grain still $stillGrain ($stillAcross / $stillDown), five circles $farGrain ($farAcross / $farDown); mean $stillMean vs $farMean")
        }
        assertTrue(
            "five circles on, the clouds' grain across ($farGrain) is the still deck's ($stillGrain) within a third",
            farGrain > stillGrain * 2f / 3f && farGrain < stillGrain * 3f / 2f
        )
        assertEquals("and as much cloud", stillMean, farMean, 0.03f)
    }

    /** In the dusk a planet is drawn and a star is not: Venus shows at the sun's −4°. */
    @Test
    fun `in the dusk the planets show and the stars wait`() {
        val gradient = SkyPalette.Paper.gradient(-4.0, 0, 0, 0.0, -20.0)
        val venus = StarMark(0f, 37.5f, 1f, StarField.planetWarmth(com.callbackdev.chiaro.domain.sky.Planet.VENUS), StarField.STEADY_PHASE, scale = 1.4f)
        val star = StarMark(0f, 37.5f, 1f, 0.3f, 0.5f)
        val base = scene(gradient, -4.0)
        assertTrue(base.planets > 0.1f && base.stars == 0f)
        val withVenus = render(gradient, base.copy(starField = listOf(venus)), canvasW, canvasH, bandTop, bandHeight, "dusk-venus")
        val withStar = render(gradient, base.copy(starField = listOf(star)), canvasW, canvasH, bandTop, bandHeight, "dusk-star")
        val plain = render(gradient, base, canvasW, canvasH, bandTop, bandHeight)
        val x = canvasW / 2
        val y = canvasH / 2
        val sky = colorAt(plain, x, y).luminance()
        assertTrue("Venus over the dusk", colorAt(withVenus, x, y).luminance() > sky + 0.02f)
        assertEquals("a star waits for the dark", sky, colorAt(withStar, x, y).luminance(), 0.004f)
    }

    /** What `moves()` promises (10 nov 2026): a sky it calls still is the same picture whatever
     * the clock says, so not running the clock for it loses nothing; one it calls moving is not. */
    @Test
    fun `a sky with nothing to move is the same picture at any instant`() {
        val noonGradient = SkyPalette.Paper.gradient(50.0, 0, 0, 0.0, -20.0)
        val noon = scene(noonGradient, 50.0)
        val duskGradient = SkyPalette.Paper.gradient(-4.0, 0, 0, 0.0, -20.0)
        val venus = StarMark(0f, 37.5f, 1f, StarField.planetWarmth(com.callbackdev.chiaro.domain.sky.Planet.VENUS), StarField.STEADY_PHASE, scale = 1.4f)
        val dusk = scene(duskGradient, -4.0).copy(starField = listOf(venus))
        listOf("noon" to (noonGradient to noon), "dusk" to (duskGradient to dusk)).forEach { (name, sky) ->
            val (gradient, scene) = sky
            assertTrue("$name is still", !scene.moves())
            val first = render(gradient, scene, canvasW, canvasH, bandTop, bandHeight)
            val later = render(gradient, scene, canvasW, canvasH, bandTop, bandHeight, time = 7.3f)
            assertTrue("a still $name is one picture", first.sameAs(later))
        }
        val cloudyGradient = SkyPalette.Paper.gradient(50.0, 50, 0, 0.0, -20.0)
        val cloudy = scene(cloudyGradient, 50.0, cloudPct = 50)
        assertTrue(cloudy.moves())
        assertTrue(
            "a deck drifts",
            !render(cloudyGradient, cloudy, canvasW, canvasH, bandTop, bandHeight)
                .sameAs(render(cloudyGradient, cloudy, canvasW, canvasH, bandTop, bandHeight, time = 7.3f))
        )
    }
}
