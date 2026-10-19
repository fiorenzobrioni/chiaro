package com.callbackdev.chiaro.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.floor

/**
 * The cumulus deck takes the share of the canvas its cover says (DESIGN §3.9, 16 ott 2026).
 * The deck is drawn by [SkyShaderSource] on the GPU; this test runs the same noise on the CPU,
 * line for line, over 24 canvases of 400 × 300 dp along 100 000 dp of drift (a point every
 * 4 dp), and measures the mean alpha of the clouds against [deckThreshold]. The first test pins the shader lines the port copies, so
 * a change to the deck's noise fails here until the port and the thresholds are measured again.
 */
class SkyDeckTest {

    @Test
    fun `the port is the shader's deck`() {
        listOf(
            "float elev = mix(0.7854, 0.2618, uv.y);",
            "float dist = 1.0 / tan(elev);",
            "float focal = (uSize.y / uPx) / (0.7854 - 0.2618);",
            "float2 ground = float2((q.x - 0.5 * uSize.x / uPx) * dist / focal + drift * 1.732 / focal, dist);",
            "float2 p = ground * (focal / (1.732 * 95.0));",
            "float2 w = float2(fbm2(p * 0.7 + float2(3.1, 7.7)), fbm2(p * 0.7 + float2(9.4, 1.3)));",
            "float2 pw = p + (w - 0.5) * 0.6;",
            // fbmParts is fbm with its two- and three-octave sums handed back: the same n.
            "float n = fbmParts(pw, deck2, deck3);",
            "if (i == 1) two = v / 0.75;",
            "if (i == 2) three = v / 0.875;",
            "float a = smoothstep(uDeckTh, uDeckTh + 0.22, n);",
            "p = float2(0.8 * p.x - 0.6 * p.y, 0.6 * p.x + 0.8 * p.y) * 2.03 + float2(11.7, 5.3);",
            "for (int i = 0; i < 4; i++) {",
            "return v / 0.9375;",
            "return (0.5 * vnoise(p) + 0.25 * vnoise(float2(0.8 * p.x - 0.6 * p.y, 0.6 * p.x + 0.8 * p.y) * 2.03 + float2(11.7, 5.3))) / 0.75;",
            "p3 += dot(p3, p3.yzx + 33.33);"
        ).forEach { line -> assertTrue("the shader no longer has «$line»", SkyShaderSource.contains(line)) }
    }

    @Test
    fun `the clouds drawn take the deck's share of the sky`() {
        listOf(0.1f, 0.2f, 0.35f, 0.5f, 0.65f, 0.8f, 0.9f).forEach { deck ->
            val th = deckThreshold(deck)
            val mean = noise.map { smoothstep(th, th + 0.22f, it) }.average().toFloat()
            assertEquals("a deck of $deck", deck, mean, 0.03f)
        }
    }

    /** The blue hollows of an overcast (device, 16 ott 2026): a closed deck has no gap. */
    @Test
    fun `a closed deck covers every pixel`() {
        val th = deckThreshold(1f)
        // Under the noise's own floor: fbm is a sum of positive octaves of a value in 0..1.
        assertTrue(th + 0.22f <= 0f)
        assertTrue(noise.all { smoothstep(th, th + 0.22f, it) == 1f })
    }

    @Test
    fun `the threshold falls steadily as the deck grows`() {
        var last = Float.MAX_VALUE
        for (i in 0..100) {
            val th = deckThreshold(i / 100f)
            assertTrue("at ${i}%", th < last)
            last = th
        }
        assertEquals(deckThreshold(0f), deckThreshold(-1f), 0f)
        assertEquals(deckThreshold(1f), deckThreshold(2f), 0f)
    }

    private val noise by lazy { deckNoise() }

    private fun deckNoise(): List<Float> {
        val w = 400
        val h = 300
        val out = ArrayList<Float>(w * h * 24 / 16)
        // The plane projection, as the shader writes it (step 4, 19 ott 2026): elevations 45°
        // to 15° down the canvas, the field sampled where the row's ray meets the deck.
        val focal = h / (0.7854f - 0.2618f)
        for (k in 0 until 24) {
            val drift = k * 4100f
            for (y in 0 until h step 4) for (x in 0 until w step 4) {
                val elev = 0.7854f + (0.2618f - 0.7854f) * (y.toFloat() / h)
                val dist = 1f / kotlin.math.tan(elev)
                val scale = focal / (1.732f * 95f)
                val px = ((x - w / 2f) * dist / focal + drift * 1.732f / focal) * scale
                val py = dist * scale
                val wx = fbm2(px * 0.7f + 3.1f, py * 0.7f + 7.7f)
                val wy = fbm2(px * 0.7f + 9.4f, py * 0.7f + 1.3f)
                out += fbm(px + (wx - 0.5f) * 0.6f, py + (wy - 0.5f) * 0.6f)
            }
        }
        return out
    }

    private fun fract(x: Float) = x - floor(x)

    private fun hash12(x: Float, y: Float): Float {
        var a = fract(x * 0.1031f)
        var b = fract(y * 0.1031f)
        var c = fract(x * 0.1031f)
        val d = a * (b + 33.33f) + b * (c + 33.33f) + c * (a + 33.33f)
        a += d; b += d; c += d
        return fract((a + b) * c)
    }

    private fun vnoise(x: Float, y: Float): Float {
        val ix = floor(x)
        val iy = floor(y)
        val fx = x - ix
        val fy = y - iy
        val ux = fx * fx * (3f - 2f * fx)
        val uy = fy * fy * (3f - 2f * fy)
        val a = hash12(ix, iy)
        val b = hash12(ix + 1f, iy)
        val c = hash12(ix, iy + 1f)
        val d = hash12(ix + 1f, iy + 1f)
        val top = a + (b - a) * ux
        val bottom = c + (d - c) * ux
        return top + (bottom - top) * uy
    }

    private fun fbm(x0: Float, y0: Float): Float {
        var x = x0
        var y = y0
        var v = 0f
        var a = 0.5f
        repeat(4) {
            v += a * vnoise(x, y)
            val nx = (0.8f * x - 0.6f * y) * 2.03f + 11.7f
            val ny = (0.6f * x + 0.8f * y) * 2.03f + 5.3f
            x = nx; y = ny
            a *= 0.5f
        }
        return v / 0.9375f
    }

    private fun fbm2(x: Float, y: Float): Float {
        val x2 = (0.8f * x - 0.6f * y) * 2.03f + 11.7f
        val y2 = (0.6f * x + 0.8f * y) * 2.03f + 5.3f
        return (0.5f * vnoise(x, y) + 0.25f * vnoise(x2, y2)) / 0.75f
    }

    private fun smoothstep(e0: Float, e1: Float, x: Float): Float {
        val t = ((x - e0) / (e1 - e0)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }
}
