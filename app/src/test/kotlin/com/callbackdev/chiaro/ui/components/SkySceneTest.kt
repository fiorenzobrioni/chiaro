package com.callbackdev.chiaro.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.callbackdev.chiaro.domain.model.CloudLayers
import com.callbackdev.chiaro.ui.theme.SkyPalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
        moonLimb: Double = 0.0,
        layers: CloudLayers? = null,
        windKph: Double = 15.0,
        windFrom: Int = 270,
        southern: Boolean = false,
        galactic: SkyFrame? = null
    ) = skyScene(
        palette = palette,
        gradient = palette.gradient(alt, cloud, precip, moonIllum, moonAlt),
        sunAltitudeDeg = alt,
        sunAzimuthDeg = 180.0,
        moonAltitudeDeg = moonAlt,
        moonAzimuthDeg = 150.0,
        moonIllumination = moonIllum,
        moonLimbAngleDeg = moonLimb,
        southern = southern,
        cloudPct = cloud,
        layers = layers,
        precipPct = precip,
        fall = fall,
        fallMmPerHour = 2.0,
        windKph = windKph,
        windFromDeg = windFrom,
        galactic = galactic
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

    /** The Milky Way needs more dark than the stars: an open, moonless sky, and the frame to draw it in. */
    @Test
    fun `the Milky Way needs a darker sky than the stars`() {
        val frame = SkyFrame(1f, 0f, 0f, 0f, 0f, 1f)
        assertEquals(0f, scene(alt = -30.0, cloud = 0).milkyWay, 0f)
        assertEquals(0f, scene(alt = 30.0, cloud = 0, galactic = frame).milkyWay, 0f)
        assertEquals(0f, scene(alt = -30.0, cloud = 100, galactic = frame).milkyWay, 0f)
        val dark = scene(alt = -30.0, cloud = 0, galactic = frame)
        assertTrue(dark.milkyWay > 0.9f)
        assertEquals(frame, dark.galactic)
        val half = scene(alt = -30.0, cloud = 50, galactic = frame)
        assertTrue("half a sky of cloud leaves an eighth of the band", half.milkyWay < 0.15f)
        assertTrue(half.milkyWay < half.stars)
        val moon = scene(alt = -30.0, cloud = 0, moonAlt = 40.0, moonIllum = 1.0, galactic = frame)
        assertTrue(moon.milkyWay < 0.05f)
        assertTrue(moon.milkyWay < moon.stars)
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

    /**
     * 16 ott 2026: the low and middle layers make the deck, the high one the veil behind it,
     * overlapping at random as the provider's total does; neither more than the total.
     */
    @Test
    fun `the layers split the cover into a deck and a veil`() {
        // No layers: the whole cover is the deck, as before the layers were read.
        assertEquals(0.4f to 0f, cloudDecks(40, null))
        // Layers that explain nothing of a cloudy total do not empty the sky.
        assertEquals(0.6f to 0f, cloudDecks(60, CloudLayers(0, 0, 0)))
        assertEquals(1f to 0f, cloudDecks(100, CloudLayers(100, 0, 0)))
        assertEquals(0f to 1f, cloudDecks(100, CloudLayers(0, 0, 100)))
        // Low and middle overlap at random: 50% and 50% cover 75% of the sky.
        val (deck, veil) = cloudDecks(90, CloudLayers(50, 50, 60))
        assertEquals(0.75f, deck, 1e-6f)
        assertEquals(0.6f, veil, 1e-6f)
        // Never more than the total, which is the number the hero's word comes from.
        assertEquals(0.2f to 0.2f, cloudDecks(20, CloudLayers(80, null, 90)))
        assertEquals(0.75f, scene(cloud = 90, layers = CloudLayers(50, 50, 60)).deck, 1e-6f)
    }

    /**
     * 16 ott 2026: a low deck takes the light, a middle one most of it, a high veil little;
     * a wet hour takes all its cover's; no layers is the cover itself.
     */
    @Test
    fun `the cloud's opacity is its layer's`() {
        assertEquals(0.7f, cloudOpacity(70, null, SkyFall.NONE), 1e-6f)
        assertEquals(1f, cloudOpacity(100, CloudLayers(100, 0, 0), SkyFall.NONE), 1e-6f)
        assertEquals(0.8f, cloudOpacity(100, CloudLayers(0, 100, 0), SkyFall.NONE), 1e-6f)
        assertEquals(0.3f, cloudOpacity(100, CloudLayers(0, 0, 100), SkyFall.NONE), 1e-6f)
        assertEquals(1f, cloudOpacity(100, CloudLayers(0, 0, 100), SkyFall.RAIN), 1e-6f)
        // The README's Milan, 29 Sep at 13:00: 92% of cloud, almost all of it high.
        assertTrue(cloudOpacity(92, CloudLayers(2, 0, 92), SkyFall.NONE) < 0.35f)
        // Never more than the cover.
        assertEquals(0.3f, cloudOpacity(30, CloudLayers(90, 0, 0), SkyFall.NONE), 1e-6f)
        assertEquals(0.3f, scene(cloud = 100, layers = CloudLayers(0, 0, 100)).opacity, 1e-6f)
    }

    /** The light catches the clouds' rims under a high veil, hardly under a low deck. */
    @Test
    fun `the sun's rim light follows the opacity, not the cover`() {
        val veiled = scene(cloud = 100, layers = CloudLayers(0, 0, 100)).glow
        val decked = scene(cloud = 100, layers = CloudLayers(100, 0, 0)).glow
        assertTrue(veiled > decked + 0.3f)
    }

    /**
     * 16 ott 2026: a closed deck is grey, its shade no longer drawn toward the blue of the
     * sky's top stop (the blue hollows of an overcast on a device), and no darker for it.
     */
    @Test
    fun `a closed deck's shade loses its hue and keeps its brightness`() {
        fun chroma(c: Color) = maxOf(c.red, c.green, c.blue) - minOf(c.red, c.green, c.blue)
        val open = scene(cloud = 30, layers = CloudLayers(30, 0, 0)).shade
        val closed = scene(cloud = 100, layers = CloudLayers(100, 0, 0)).shade
        val veiled = scene(cloud = 100, layers = CloudLayers(0, 0, 100)).shade
        assertTrue(chroma(closed) < chroma(open) * 0.5f)
        // Under a high veil the deck is a few puffs, shaded as puffs are.
        assertTrue(chroma(veiled) > chroma(closed) * 2f)
        val toned = grey(closed)
        assertEquals(closed.luminance(), toned.luminance(), 0.01f)
    }

    /**
     * 19 ott 2026: a rain cloud is darker than the sky in its gaps, not lighter. Before, the
     * lit colour ignored the rain while the gradient dimmed under it (§3.3), and a 94% deck at
     * 90% of rain drew light grey clouds over dark blue holes. Now the sky's mid and bottom
     * stops sit between the clouds' two colours (the top stop stays darker, §3.3's own number).
     */
    @Test
    fun `a likely rain darkens the clouds, and the sky's mid stop sits between their colours`() {
        val layers = CloudLayers(90, 40, 20)
        val dry = scene(cloud = 95, layers = layers)
        val wet = scene(cloud = 95, layers = layers, precip = 90, fall = SkyFall.RAIN)
        assertTrue(wet.lit.luminance() < dry.lit.luminance() * 0.75f)
        val sky = SkyPalette.Paper.gradient(40.0, 95, 90)
        assertTrue(wet.shade.luminance() < sky.mid.luminance())
        assertTrue(sky.mid.luminance() < wet.lit.luminance())
        assertTrue(sky.bottom.luminance() < wet.lit.luminance())
        // A dry hour is as it was.
        assertEquals(dry.lit, scene(cloud = 95, layers = layers, precip = 0).lit)
    }

    /**
     * 19 ott 2026: the sun and the moon are the scene's, placed by the bodies' rules in the
     * same frame the clouds' light is, and only while they are up.
     */
    @Test
    fun `the sun's mark stands where the bodies' rules put it, and only while it is up`() {
        val s = scene(alt = 30.0)
        val sun = requireNotNull(s.sun)
        assertEquals(bodyAcross(180.0, southern = false), sun.across, 1e-6f)
        assertEquals(bodyUp(30.0), sun.up, 1e-6f)
        assertEquals(1f, sun.disc, 1e-6f)
        assertEquals(0f, sun.warmth, 1e-6f)
        assertTrue(scene(alt = 3.0).sun!!.warmth > 0.7f)
        assertNull(scene(alt = -1.0).sun)
        // The light the clouds read is the sun's own place, the same two numbers.
        assertEquals(Offset(sun.across, sun.up), s.light)
    }

    /** The deck hides a disc pixel by pixel in the shader, so only the high veil dims it here;
     * the glow in front of the clouds reads the whole opacity, and a wet hour takes it all. */
    @Test
    fun `a deck leaves the sun's disc to the shader, a veil dims it, a wet hour takes it`() {
        val decked = scene(cloud = 100, layers = CloudLayers(100, 0, 0)).sun!!
        val veiled = scene(cloud = 100, layers = CloudLayers(0, 0, 100)).sun!!
        assertEquals(1f, decked.disc, 1e-6f)
        assertEquals(0.7f, veiled.disc, 1e-6f)
        assertTrue(decked.spread > veiled.spread)
        assertTrue(decked.glowMid > veiled.glowMid)
        val wet = scene(cloud = 100, layers = CloudLayers(100, 0, 0), precip = 90, fall = SkyFall.RAIN).sun!!
        assertEquals(0f, wet.disc, 1e-6f)
    }

    @Test
    fun `the moon's mark wears its phase and its veil`() {
        val night = scene(alt = -30.0, moonAlt = 40.0, moonIllum = 0.3, moonLimb = -35.0)
        val moon = requireNotNull(night.moon)
        // The bright limb's angle is the place's, carried as it is (step 6).
        assertEquals(-35f, moon.limbAngleDeg, 1e-6f)
        assertEquals(150f, scene(alt = -30.0, moonAlt = 40.0, moonIllum = 0.3, moonLimb = 150.0).moon!!.limbAngleDeg, 1e-6f)
        assertEquals(0.95f, moon.disc, 1e-6f)
        assertEquals(0.3f, moon.illumination, 1e-6f)
        assertEquals(Offset(moon.across, moon.up), night.light)
        assertNull(scene(moonAlt = -5.0).moon)
        // By day the moon is the pale thing it is; behind a closed low deck, at night, its disc
        // is the shader's to hide and its glow stays, dimmer and wider.
        assertEquals(0.55f, scene(alt = 30.0, moonAlt = 20.0, moonIllum = 0.8).moon!!.disc, 1e-6f)
        val decked = scene(alt = -30.0, moonAlt = 40.0, moonIllum = 1.0, cloud = 100, layers = CloudLayers(100, 0, 0)).moon!!
        assertEquals(0.95f, decked.disc, 1e-6f)
        assertTrue(decked.glow in 0.3f..0.5f)
        assertTrue(decked.spread > 1.2f)
    }

    /**
     * Step 3 (19 ott 2026): the clouds' light is the sun's or the moon's, whichever gives more;
     * at dusk the sun lights them from under the horizon, and without either they are flat.
     */
    @Test
    fun `at dusk the sun lights the clouds from below the band, and without a light they are flat`() {
        val dusk = scene(alt = -3.0, cloud = 40)
        assertTrue("the light is under the band at −3°", dusk.light.y < 0f)
        assertEquals(-0.25f, dusk.light.y, 1e-6f)
        assertTrue("the dusk still lights the clouds", dusk.lightAmount > 0.5f)
        assertTrue("and they are warm", dusk.lit.red > dusk.lit.blue)
        assertEquals(0f, scene(alt = -30.0, cloud = 40).lightAmount, 1e-6f)
        assertEquals(1f, scene(alt = 30.0, cloud = 40).lightAmount, 1e-6f)
        // The rim light follows the light there is: some at dusk, none on a moonless night.
        assertTrue(dusk.glow > 0.3f)
        assertEquals(0f, scene(alt = -30.0, cloud = 40).glow, 0f)
    }

    @Test
    fun `the light is the body that gives more of it, not the sun by default`() {
        // A thin crescent at dusk: the sun, still, from under the horizon.
        val dusk = scene(alt = -3.0, moonAlt = 30.0, moonIllum = 0.2)
        assertTrue(dusk.light.y < 0f)
        assertEquals(bodyAcross(180.0, southern = false), dusk.light.x, 1e-6f)
        // The same crescent at −12°: the moon.
        val night = scene(alt = -12.0, moonAlt = 30.0, moonIllum = 0.2)
        assertEquals(bodyAcross(150.0, southern = false), night.light.x, 1e-6f)
        assertEquals(bodyUp(30.0), night.light.y, 1e-6f)
        assertEquals(0.2f, night.lightAmount, 0.01f)
    }

    @Test
    fun `a grey keeps its colour's luminance`() {
        val c = Color(0.35f, 0.55f, 0.85f)
        val g = grey(c)
        assertEquals(c.luminance(), g.luminance(), 1e-3f)
        assertEquals(g.red, g.blue, 1e-6f)
        assertEquals(g.red, g.green, 1e-6f)
    }
}
