package com.callbackdev.chiaro.widget.window

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.callbackdev.chiaro.domain.model.Coordinates
import com.callbackdev.chiaro.domain.sky.AstronomyEngine
import com.callbackdev.chiaro.domain.sky.MeteorShowerTable
import com.callbackdev.chiaro.ui.components.SkyFall
import com.callbackdev.chiaro.ui.theme.SkyPalette
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The window's sky draws the hour's state as Today's canvas does (16 nov 2026): the fog of an
 * hour in fog, the storm of a thunderstorm, and on a peak night the meteors, where the sky
 * moves. Until then the frame asked the shader for the cover alone, and a night in fog was a
 * field of stars with their names on.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rGB")
class WindowStateSkyTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val milan = Coordinates(45.4642, 9.19)
    private val rome: ZoneId = ZoneId.of("Europe/Rome")
    private val w = 1056
    private val skyH = 550
    private val density = 2.75f

    /** An evening with the stars out and no moon in it, two weeks after the full moon of
     * 26 Oct 2026. */
    private val darkEvening = Instant.parse("2026-11-10T19:00:00Z")

    private fun frame(
        at: Instant,
        cloud: Int = 0,
        fog: Float = 0f,
        thunder: Boolean = false,
        meteors: Boolean = false,
        facing: Double? = null
    ) = WindowScene.frame(
        table = SkyPalette.Paper, coords = milan, zone = rome, southern = false, drawAt = at,
        facingDeg = facing, w = w, skyH = skyH, density = density,
        cloudPct = cloud, layers = null, precipPct = 0, fall = SkyFall.NONE, fallMmPerHour = 0.0,
        windKph = 5.0, windFromDeg = 180, followed = emptySet(),
        fog = fog, thunder = thunder, meteors = meteors
    )

    @Test
    fun `an hour in fog draws its fog, and the fog takes the stars`() {
        val clear = frame(darkEvening).scene
        val foggy = frame(darkEvening, fog = 0.6f).scene
        assertEquals(0f, clear.fog, 0f)
        assertEquals(0.6f, foggy.fog, 0f)
        assertTrue("the stars are out on the clear evening: ${clear.stars}", clear.stars > 0.5f)
        assertEquals("and the thinnest fog leaves two fifths of them", clear.stars * 0.4f, foggy.stars, 1e-4f)
        assertEquals("a thick one none", 0f, frame(darkEvening, fog = 1f).scene.stars, 0f)
    }

    @Test
    fun `no name is written in fog`() {
        val clear = frame(darkEvening)
        val labels = WindowScene.labels(context, clear, milan, w, skyH, cloudPct = 0, moonPct = 2)
        assertTrue("the clear evening names something: ${labels.map { it.text }}", labels.isNotEmpty())
        val foggy = frame(darkEvening, fog = 0.6f)
        assertTrue(WindowScene.labels(context, foggy, milan, w, skyH, cloudPct = 0, moonPct = 2, fog = 0.6f).isEmpty())
    }

    @Test
    fun `a thunderstorm is a storm in the scene, under a darker deck`() {
        val noon = Instant.parse("2026-07-15T11:00:00Z")
        val rain = frame(noon, cloud = 100).scene
        val storm = frame(noon, cloud = 100, thunder = true).scene
        assertEquals(0f, rain.storm, 0f)
        assertEquals(1f, storm.storm, 0f)
        assertTrue("the deck of a cumulonimbus is darker", storm.lit.luminanceOf() < rain.lit.luminanceOf())
        assertEquals("and has no sun to draw", null, storm.sun)
    }

    @Test
    fun `on a peak night the meteors run from the radiant the window names, where it asks for them`() {
        val geminids = requireNotNull(MeteorShowerTable.byId("geminids"))
        val peak = AstronomyEngine.solarLongitudeInstant(2026, geminids.solarLongitudeDeg)
        val evening = peak.atZone(rome).toLocalDate().atTime(21, 0).atZone(rome).toInstant()
            .let { if (peak.atZone(rome).hour < 12) it.minus(Duration.ofDays(1)) else it }
        // Looking east, where Gemini rises: the moon of that night would turn the window away.
        val asked = frame(evening, meteors = true, facing = 90.0)
        val radiant = checkNotNull(asked.radiant)
        assertTrue("a meteor shows on the open, dark sky: ${asked.scene.meteors}", asked.scene.meteors > 0.2f)
        assertEquals(WindowScene.acrossOf(radiant.azimuthDeg, 90.0), asked.scene.radiant.x, 1e-4f)
        assertEquals(radiant.altitudeDeg.toFloat(), asked.scene.radiant.y, 1e-4f)
        // A picture drawn once asks for none: the name stays, the trails do not.
        val still = frame(evening, facing = 90.0)
        assertEquals(0f, still.scene.meteors, 0f)
        assertEquals("the radiant is still named", radiant.jobId, still.radiant?.jobId)
        // Under a closed sky, or in a thick fog, none shows.
        assertEquals(0f, frame(evening, cloud = 100, meteors = true, facing = 90.0).scene.meteors, 0f)
        assertEquals(0f, frame(evening, fog = 1f, meteors = true, facing = 90.0).scene.meteors, 0f)
        // And a night that is no shower's has none to draw.
        assertEquals(0f, frame(darkEvening, meteors = true).scene.meteors, 0f)
    }

    private fun androidx.compose.ui.graphics.Color.luminanceOf(): Float =
        0.2126f * red + 0.7152f * green + 0.0722f * blue
}
