package com.callbackdev.chiaro.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.callbackdev.chiaro.domain.model.CloudLayers
import com.callbackdev.chiaro.ui.theme.SkyPalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The canvas' scene (DESIGN §3.9): drawn from the data, and never brighter than what §3.6's
 * scrim is measured against.
 */
class SkySceneTest {

    private fun scene(
        palette: SkyPalette = SkyPalette.Paper,
        alt: Double = 40.0,
        cloud: Int = 40,
        precip: Int = 0,
        fall: SkyFall = SkyFall.NONE,
        moonAlt: Double = -20.0,
        moonIllum: Double = 0.0,
        layers: CloudLayers? = null,
        windKph: Double = 15.0,
        windFrom: Int = 270,
        southern: Boolean = false
    ) = skyScene(
        palette = palette,
        gradient = palette.gradient(alt, cloud, precip, moonIllum, moonAlt),
        sunAltitudeDeg = alt,
        sunAzimuthDeg = 180.0,
        moonAltitudeDeg = moonAlt,
        moonAzimuthDeg = 150.0,
        moonIllumination = moonIllum,
        southern = southern,
        cloudPct = cloud,
        layers = layers,
        precipPct = precip,
        fall = fall,
        fallMmPerHour = 2.0,
        windKph = windKph,
        windFromDeg = windFrom
    )

    /** The clouds are drawn under the scrim like the gradient, so they may never be brighter
     * than the stop §3.6 measured white against: at any altitude, any cover, any rain. */
    @Test
    fun `no cloud is brighter than the scrim's brightest stop`() {
        listOf(SkyPalette.Paper, SkyPalette.Vivid).forEach { palette ->
            val max = palette.brightestBottomStop().luminance()
            var alt = -90.0
            while (alt <= 90.0) {
                listOf(0, 30, 60, 100).forEach { cloud ->
                    listOf(0, 80).forEach { precip ->
                        val s = scene(palette, alt, cloud, precip, moonAlt = 40.0, moonIllum = 1.0)
                        assertTrue("lit at $alt° $cloud%", s.lit.luminance() <= max + 1e-4f)
                        assertTrue("shade at $alt° $cloud%", s.shade.luminance() <= max + 1e-4f)
                        assertEquals(max, s.maxLuminance, 1e-6f)
                    }
                }
                alt += 2.0
            }
        }
    }

    @Test
    fun `capping keeps a colour's hue and lowers only what is over`() {
        val dim = Color(0.2f, 0.3f, 0.4f)
        assertEquals(dim, capLuminance(dim, 0.9f))
        val capped = capLuminance(Color.White, 0.5f)
        assertEquals(0.5f, capped.luminance(), 0.01f)
        assertEquals(capped.red, capped.blue, 1e-3f)
    }

    /** Stars only where they are: a dark sky, a clear one, washed out by a bright moon. */
    @Test
    fun `stars need the dark, an open sky and no bright moon`() {
        assertEquals(0f, scene(alt = 30.0, cloud = 0).stars, 0f)
        assertEquals(0f, scene(alt = -30.0, cloud = 100).stars, 0f)
        val dark = scene(alt = -30.0, cloud = 0).stars
        assertTrue(dark > 0.9f)
        assertTrue(scene(alt = -30.0, cloud = 0, moonAlt = 40.0, moonIllum = 1.0).stars < dark * 0.5f)
        assertEquals(0f, scene(alt = -7.0, cloud = 0).stars, 0f)
    }

    /** Nothing falls on the canvas unless the hour's state says it does. */
    @Test
    fun `rain and snow only on a wet hour`() {
        assertEquals(0f, scene(precip = 90).rain, 0f)
        assertTrue(scene(fall = SkyFall.RAIN, precip = 90).rain > 0f)
        assertEquals(0f, scene(fall = SkyFall.RAIN).snow, 0f)
        assertTrue(scene(fall = SkyFall.SNOW).snow > 0f)
    }

    /** The clouds drift with the wind, as the reader facing the equator sees it: in the north
     * the west is on the right, in the south on the left; a stronger wind drifts faster. */
    @Test
    fun `the clouds drift where the wind blows`() {
        assertTrue("a wind from the east blows west, to the right", scene(windFrom = 90).drift > 0f)
        assertTrue("a wind from the west blows east, to the left", scene(windFrom = 270).drift < 0f)
        assertTrue(scene(windFrom = 90, southern = true).drift < 0f)
        assertTrue(scene(windFrom = 90, windKph = 40.0).drift > scene(windFrom = 90, windKph = 5.0).drift)
        assertTrue("a wind from the north still breathes", kotlin.math.abs(scene(windFrom = 0).drift) > 0f)
    }

    @Test
    fun `the high layer's share of the cover makes the veils`() {
        assertEquals(0f, scene(layers = null).cirrus, 0f)
        assertEquals(0.75f, scene(layers = CloudLayers(lowPct = 20, midPct = 0, highPct = 60)).cirrus, 1e-6f)
    }
}
