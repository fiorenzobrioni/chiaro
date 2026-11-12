package com.callbackdev.chiaro.ui.sky.explore

import android.app.Application
import android.graphics.Bitmap
import android.graphics.HardwareRenderer
import android.graphics.PixelFormat
import android.graphics.RenderNode
import android.media.ImageReader
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.data.AppSettings
import com.callbackdev.chiaro.domain.model.City
import com.callbackdev.chiaro.domain.model.Coordinates
import com.callbackdev.chiaro.domain.model.HourlyForecast
import com.callbackdev.chiaro.domain.model.WeatherCondition
import com.callbackdev.chiaro.domain.model.WeatherReport
import com.callbackdev.chiaro.domain.sample.sampleWeatherReport
import com.callbackdev.chiaro.domain.sky.AstronomyEngine
import com.callbackdev.chiaro.domain.sky.Constellation
import com.callbackdev.chiaro.domain.sky.Constellations
import com.callbackdev.chiaro.domain.sky.Planet
import com.callbackdev.chiaro.domain.sky.SkyBody
import com.callbackdev.chiaro.domain.sky.SkyOccurrence
import com.callbackdev.chiaro.domain.sky.SkyScheduler
import com.callbackdev.chiaro.domain.sky.SkyTarget
import com.callbackdev.chiaro.ui.format.LocalClock
import com.callbackdev.chiaro.ui.sky.SkyNights
import com.callbackdev.chiaro.ui.theme.ChiaroTheme
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.ZoneId
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The explorer's page (Fase 16, steps 16.6 and 16.7): the sky of an hour of a night on the
 * whole screen, what a finger and the two buttons do to it, what the panel at its foot
 * does to its time, the hour's cloud, the constellations, and the rule it is held to: it
 * draws when the camera or the hour changes and at no other time. Rendered through the GPU pipeline, as the hero's
 * stage and the README's pictures are.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w384dp-h832dp-xhdpi")
class SkyExploreScreenTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val app: Application = ApplicationProvider.getApplicationContext()
    private val milan = Coordinates(45.4642, 9.19)
    private val zone = ZoneId.of("Europe/Rome")

    // A moonless night with Orion rising (11 Nov 2026, 23:00 in Milan), and the recorded
    // Milan's own night, a full moon over the south-east.
    private val dark = Instant.parse("2026-11-11T22:00:00Z")
    private val moonlit = Instant.parse("2026-09-25T21:00:00Z")

    private var frames = 0
    private var closed = 0
    private var drawn: SkyCamera? = null
    private var bareAsked: Boolean? = null
    private var figuresAsked: Boolean? = null

    /** The bells the reader has set, as the store would hold them, and what the sheet asked for. */
    private var bells by mutableStateOf(emptySet<String>())
    private var pageAsked: String? = null

    private val city = City(
        id = 1L, name = "Milano", region = "Lombardia", country = "Italia",
        coordinates = milan, timezone = zone.id
    )

    /** The forecast the page reads: a test changes it under the page, as a fetch does. */
    private var forecast by mutableStateOf<WeatherReport?>(null)

    /** A forecast of [hours] hours from noon of [day] in Milan, [cloud] of the sky covered at each. */
    private fun report(
        hours: Int = 48,
        day: LocalDate = LocalDate.of(2026, 11, 11),
        cloud: (Int) -> Int = { 12 }
    ): WeatherReport {
        val fetched = day.atTime(12, 0)
        val sample = sampleWeatherReport()
        return sample.copy(
            location = sample.location.copy(
                city = city.name, region = city.region, country = city.country,
                coordinates = milan, timezone = zone.id, localTime = fetched
            ),
            hourly = (0 until hours).map {
                HourlyForecast(
                    time = fetched.plusHours(it.toLong()),
                    at = fetched.plusHours(it.toLong()).atZone(zone).toInstant(),
                    tempC = 8.0,
                    condition = WeatherCondition(0),
                    precipChancePct = 0,
                    cloudCoverPct = cloud(it)
                )
            },
            systemInfo = sample.systemInfo.copy(lastSync = fetched.atZone(zone).toInstant())
        )
    }

    /**
     * The page on the night that holds [now], with [held] on its ribbon (null: the night's
     * own instant). [forecast] null is a place with no report. The two switches are the
     * caller's, as they are the store's in the app.
     */
    private fun show(
        held: Instant? = dark,
        facingDeg: Double? = 135.0,
        now: Instant = held ?: dark,
        forecast: WeatherReport? = report(),
        bare: Boolean = false,
        figures: Boolean = false
    ) {
        this.forecast = forecast
        // The evening the night belongs to: before noon it is still yesterday's.
        val local = now.atZone(zone)
        val tonight = if (local.hour < 12) local.toLocalDate().minusDays(1) else local.toLocalDate()
        compose.setContent {
            ChiaroTheme(darkTheme = false, dynamicColor = false) {
                CompositionLocalProvider(LocalClock provides Clock.fixed(now, zone)) {
                    val nights = remember(this.forecast) { SkyNights(city, this.forecast, AppSettings()) }
                    var bareSky by remember { mutableStateOf(bare) }
                    var constellations by remember { mutableStateOf(figures) }
                    SkyExploreScreen(
                        coordinates = milan,
                        zone = zone,
                        nights = nights,
                        tonightDate = tonight,
                        openAt = held,
                        facingDeg = facingDeg,
                        followed = emptySet(),
                        bareSky = bareSky,
                        onBareSky = { bareAsked = it; bareSky = it },
                        constellations = constellations,
                        onConstellations = { figuresAsked = it; constellations = it },
                        onBack = { closed++ },
                        reminded = bells,
                        onRemind = { jobId, on -> bells = if (on) bells + jobId else bells - jobId },
                        onOpenPage = { pageAsked = it },
                        onFrame = { frames++; drawn = it }
                    )
                }
            }
        }
        compose.waitForIdle()
    }

    private val sky get() = compose.onNode(hasTestTag(ExploreSkyTag))

    private fun looking(): String =
        sky.fetchSemanticsNode().config[SemanticsProperties.ContentDescription].first()

    private val closer get() = compose.onNode(hasContentDescription(app.getString(R.string.explore_zoom_in)))
    private val further get() = compose.onNode(hasContentDescription(app.getString(R.string.explore_zoom_out)))
    private val night get() = compose.onNode(hasTestTag(ExploreNightTag))
    private val hour get() = compose.onNode(hasTestTag(ExploreHourTag))
    private val ribbon get() = compose.onNode(hasContentDescription(app.getString(R.string.sky_scrub_strip_desc)))

    private fun text(id: Int, vararg args: Any) = hasText(app.getString(id, *args))

    private val sheet get() = compose.onNode(hasTestTag(ExploreObjectTag))
    private val bell get() = compose.onNode(hasTestTag(ExploreBellTag))
    private val page get() = compose.onNode(hasTestTag(ExplorePageTag))

    /** A tap on the sky where the last frame's camera has [azimuthDeg], [altitudeDeg], [dxDp] to its right. */
    private fun tapAt(azimuthDeg: Double, altitudeDeg: Double, dxDp: Float = 0f) {
        val out = FloatArray(2)
        sky.performTouchInput {
            assertTrue(drawn!!.lens(width.toFloat(), height.toFloat()).toScreen(azimuthDeg, altitudeDeg, out))
            click(Offset(out[0] + dxDp * density, out[1]))
        }
        compose.waitForIdle()
    }

    private fun tapOn(target: SkyTarget, at: Instant = dark, dxDp: Float = 0f) {
        val place = AstronomyEngine.horizontal(target, at, milan)
        tapAt(place.azimuthDeg, place.altitudeDeg, dxDp)
    }

    private fun nightSays(): String = night.fetchSemanticsNode().config[SemanticsProperties.ContentDescription].first()

    /** The camera the last frame was drawn through, on the picture's own size. */
    private fun lens(bitmap: Bitmap): SkyLens = drawn!!.lens(bitmap.width.toFloat(), bitmap.height.toFloat())

    /** One drag of the sky by ([dx], [dy]) dp, in steps as a finger makes them. */
    private fun drag(dx: Float, dy: Float) {
        sky.performTouchInput {
            // Over the sky's upper half: the foot of the page is the panel's.
            val from = Offset(center.x, height * 0.3f)
            down(from)
            repeat(10) { moveBy(Offset(dx / 10f * density, dy / 10f * density), delayMillis = 16) }
            up()
        }
        compose.waitForIdle()
    }

    @Test
    fun `it opens facing where the hero looked, on the hour it was told, with the hour's cloud beside it`() {
        show(figures = true)
        assertEquals("Looking south-east", looking())
        compose.onNode(hasText("23:00")).assertExists()
        compose.onNode(hasText("Cloud 12%")).assertExists()
        assertEquals("Tonight, Wednesday 11 November", nightSays())
        save("explore-dark", renderWindow())
    }

    /**
     * The panel is taller than the line it replaced (step 16.7), and the horizon is kept
     * over it and over the buttons that stand on the ground: the camera's floor is the
     * page's own, read off where its foot starts.
     */
    @Test
    fun `the horizon stands over the panel and what stands on the ground`() {
        show()
        val bitmap = renderWindow()
        val out = FloatArray(2)
        assertTrue(lens(bitmap).toScreen(135.0, 0.0, out))
        val panelTop = hour.fetchSemanticsNode().boundsInRoot.top
        val zoomTop = further.fetchSemanticsNode().boundsInRoot.top
        assertTrue("the horizon (${out[1]}) is over the buttons ($zoomTop) and the panel ($panelTop)", out[1] < zoomTop && zoomTop < panelTop)
        assertTrue("and under the middle of the screen", out[1] > bitmap.height * 0.55f)
        // The camera rests on that floor: it opens as low as it may look.
        assertEquals(
            SkyCamera.minAltitudeDeg(drawn!!.fieldDeg, bitmap.width.toFloat(), bitmap.height.toFloat(), out[1] / bitmap.height),
            drawn!!.altitudeDeg,
            0.2
        )
    }

    @Test
    fun `the moon is drawn where the camera puts it`() {
        val facing = AstronomyEngine.bodyAzimuth(SkyBody.MOON, moonlit, milan)
        show(moonlit, facing, forecast = null)
        val bitmap = renderWindow()
        save("explore-moon", bitmap)
        val out = FloatArray(2)
        assertTrue(lens(bitmap).toScreen(facing, AstronomyEngine.moonAltitude(moonlit, milan), out))
        // Facing the moon it is on the line down the middle of the screen.
        assertEquals(bitmap.width / 2f, out[0], 1f)
        val moon = luminanceAt(bitmap, out[0].roundToInt(), out[1].roundToInt())
        val beside = luminanceAt(bitmap, out[0].roundToInt() + 120, out[1].roundToInt())
        assertTrue("the moon ($moon) outshines the sky beside it ($beside)", moon > 0.6f && moon > beside * 4f)
        // No forecast for this place: no cloud to print, none drawn, and no switch to take it off.
        compose.onNode(text(R.string.explore_no_clouds_none)).assertExists()
        compose.onNode(text(R.string.window_bare_note)).assertDoesNotExist()
    }

    /**
     * The phase is turned toward the sun as the place sees it: on the evening of 14 Nov
     * 2026 the moon is a crescent five days old, low in the south-west an hour after
     * sunset, and its lit side is the one toward where the sun went down.
     */
    @Test
    fun `an evening crescent is lit on the side of the sun that has set`() {
        val evening = Instant.parse("2026-11-14T17:00:00Z")
        val facing = AstronomyEngine.bodyAzimuth(SkyBody.MOON, evening, milan)
        val altitude = AstronomyEngine.moonAltitude(evening, milan)
        assertTrue("the moon is up: $altitude", altitude > 5.0)
        assertTrue(AstronomyEngine.moonIllumination(evening).illuminatedFraction in 0.15..0.45)
        show(evening, facing, forecast = null)
        val bitmap = renderWindow()
        save("explore-crescent", bitmap)
        val lens = lens(bitmap)
        val moon = FloatArray(2)
        val sun = FloatArray(2)
        assertTrue(lens.toScreen(facing, altitude, moon))
        assertTrue(lens.toScreen(AstronomyEngine.sunAzimuth(evening, milan), AstronomyEngine.sunAltitude(evening, milan), sun))
        // The sun is down and to the right of an evening moon, from the north.
        assertTrue(sun[0] > moon[0] && sun[1] > moon[1])
        val d = kotlin.math.hypot(sun[0] - moon[0], sun[1] - moon[1])
        val reach = 11f * 2f * 0.7f // seven tenths of the disc's radius, in this picture's px
        val ux = (sun[0] - moon[0]) / d * reach
        val uy = (sun[1] - moon[1]) / d * reach
        val lit = luminanceAt(bitmap, (moon[0] + ux).roundToInt(), (moon[1] + uy).roundToInt())
        val shaded = luminanceAt(bitmap, (moon[0] - ux).roundToInt(), (moon[1] - uy).roundToInt())
        assertTrue("toward the sun ($lit) is the lit side, away from it ($shaded) the dark one", lit > 0.6f && shaded < 0.15f)
    }

    @Test
    fun `a drag turns the sky, and pulling it down looks straight up`() {
        show()
        drag(-40f, 0f) // the first sequence is lost under Robolectric
        val before = looking()
        // The sky pulled to the left: the camera turns to the right, from south-east on round.
        drag(-300f, 0f)
        val turned = looking()
        assertNotEquals(before, turned)
        assertTrue(turned, turned.startsWith("Looking south"))
        save("explore-turned", renderWindow())
        // Pulled down, twice the screen: the zenith, which the hero's camera never reached.
        drag(0f, 380f)
        drag(0f, 380f)
        drag(0f, 380f)
        assertEquals(app.getString(R.string.explore_overhead), looking())
        save("explore-zenith", renderWindow())
    }

    @Test
    fun `the two buttons zoom, and each stops at its end`() {
        show(figures = true)
        closer.assertIsEnabled()
        further.assertIsEnabled()
        // Sixty degrees, then forty, twenty-seven, eighteen, and the fifteen it stops at.
        repeat(4) { closer.performClick() }
        compose.waitForIdle()
        closer.assertIsNotEnabled()
        save("explore-close", renderWindow())
        // And out again, all the way to the right angle.
        repeat(5) { further.performClick() }
        compose.waitForIdle()
        further.assertIsNotEnabled()
        closer.assertIsEnabled()
        save("explore-wide", renderWindow())
    }

    /**
     * The battery's rule (Fase 16, «Le regole comuni»): nothing in this sky moves by itself.
     * Read off the view: once it has been drawn, half a minute of looking asks for no other
     * drawing, and a zoom asks for one. With no hour held the page follows the clock, and
     * a clock that has not moved asks for nothing either.
     */
    @Test
    fun `it draws when the camera or the hour changes and at no other time`() {
        show(held = null, now = dark)
        renderWindow()
        assertEquals("it drew the sky it opened on", 1, frames)
        val view = composeView()
        compose.mainClock.autoAdvance = false
        compose.mainClock.advanceTimeBy(90_000)
        compose.runOnIdle { assertTrue("a minute and a half of looking asked for no drawing", !view.isDirty) }

        // The camera moved, by the screen reader's action: a button would add its own ripple.
        val right = sky.fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsActions.CustomActions]
            .first { it.label == app.getString(R.string.explore_turn_right) }
        compose.mainClock.autoAdvance = true
        compose.runOnUiThread { right.action() }
        compose.runOnIdle { assertTrue("a turn asks for one", view.isDirty) }
        renderWindow()
        assertEquals(2, frames)
        compose.mainClock.autoAdvance = false
        compose.mainClock.advanceTimeBy(30_000)
        compose.runOnIdle { assertTrue("and then no more", !view.isDirty) }

        // Another hour held on the ribbon, by the screen reader's own step: one drawing.
        compose.mainClock.autoAdvance = true
        ribbon.performSemanticsAction(SemanticsActions.SetProgress) { it(0.25f) }
        compose.runOnIdle { assertTrue("another hour asks for one", view.isDirty) }
        renderWindow()
        assertEquals(3, frames)
    }

    /** The view Compose draws into: the one that is asked to draw again when something changed. */
    private fun composeView(): android.view.View {
        fun find(view: android.view.View): android.view.View? {
            if (view.javaClass.name.endsWith("AndroidComposeView")) return view
            if (view is android.view.ViewGroup) {
                for (i in 0 until view.childCount) find(view.getChildAt(i))?.let { return it }
            }
            return null
        }
        return find(compose.activity.window.decorView)!!
    }

    @Test
    fun `the cross closes it`() {
        show()
        compose.onNode(hasContentDescription(app.getString(R.string.explore_close))).performClick()
        assertEquals(1, closed)
    }

    /** A screen reader cannot drag: the same four moves are actions on the sky. */
    @Test
    fun `the sky can be turned without a finger`() {
        show()
        val actions = sky.fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsActions.CustomActions]
        assertEquals(
            listOf(R.string.explore_turn_left, R.string.explore_turn_right, R.string.explore_look_up, R.string.explore_look_down)
                .map { app.getString(it) },
            actions.map { it.label }
        )
        // Three steps to the right of south-east is past south.
        repeat(3) { compose.runOnUiThread { actions.first { it.label == app.getString(R.string.explore_turn_right) }.action() } }
        compose.waitForIdle()
        assertEquals("Looking south", looking())
        repeat(5) { compose.runOnUiThread { actions.first { it.label == app.getString(R.string.explore_look_up) }.action() } }
        compose.waitForIdle()
        assertEquals(app.getString(R.string.explore_overhead), looking())
    }

    /**
     * The hour's cloud (step 16.7): a covered hour takes the stars by the hero's rule and
     * says its number; «Behind the clouds» gives the stars back and leaves the number.
     */
    @Test
    fun `a covered hour takes the stars, and behind the clouds gives them back`() {
        show(forecast = report { 100 })
        compose.onNode(hasText("Cloud 100%")).assertExists()
        val covered = renderWindow()
        save("explore-covered", covered)
        val under = lights(covered)
        compose.onNode(text(R.string.window_bare_note)).performClick()
        compose.waitForIdle()
        assertEquals(true, bareAsked)
        val bare = renderWindow()
        save("explore-bare", bare)
        val behind = lights(bare)
        assertTrue("a closed sky shows no stars ($under), the sky behind it does ($behind)", under == 0 && behind > 100)
        // The words keep the forecast.
        compose.onNode(hasText("Cloud 100%")).assertExists()
    }

    /**
     * A moon behind the hour's cloud is the pale patch a covered moon is: still where the
     * camera puts it, a good deal dimmer than the bare one, and not gone.
     */
    @Test
    fun `a veil pales the moon and leaves it where it stands`() {
        val facing = AstronomyEngine.bodyAzimuth(SkyBody.MOON, moonlit, milan)
        show(moonlit, facing, forecast = report(day = LocalDate.of(2026, 9, 25)) { 80 })
        val veiled = renderWindow()
        save("explore-moon-veiled", veiled)
        val out = FloatArray(2)
        assertTrue(lens(veiled).toScreen(facing, AstronomyEngine.moonAltitude(moonlit, milan), out))
        val under = luminanceAt(veiled, out[0].roundToInt(), out[1].roundToInt())
        val skyUnder = luminanceAt(veiled, out[0].roundToInt() + 160, out[1].roundToInt())
        compose.onNode(text(R.string.window_bare_note)).performClick()
        compose.waitForIdle()
        val bare = luminanceAt(renderWindow(), out[0].roundToInt(), out[1].roundToInt())
        assertTrue("the veiled moon ($under) is dimmer than the bare one ($bare) and brighter than its sky ($skyUnder)", under < bare * 0.7f && under > skyUnder * 1.5f)
    }

    /** Half a cover takes three quarters of the stars' light (the open sky squared). */
    @Test
    fun `half a cover dims the stars and does not take them`() {
        show(forecast = report { 0 })
        val clear = lights(renderWindow())
        forecast = report { 50 }
        compose.waitForIdle()
        compose.onNode(hasText("Cloud 50%")).assertExists()
        val half = renderWindow()
        save("explore-half", half)
        val dimmed = lights(half)
        assertTrue("some of the stars are gone under half a cover ($dimmed of $clear), not all", dimmed in 1 until clear)
    }

    /**
     * «Constellations» (step 16.7): the lines join the stars the canvas draws, and the
     * switch takes them away. Facing east at 23:00 on 11 Nov Orion is rising: the middle
     * of one of its lines is lit with the switch on and is plain sky with it off.
     */
    @Test
    fun `the constellations are lines between the stars, and a switch`() {
        show(facingDeg = 95.0, figures = true)
        val on = renderWindow()
        save("explore-orion", on)
        val lens = lens(on)
        val orion = Constellations.byId("Ori")!!
        val a = FloatArray(2)
        val b = FloatArray(2)
        var checked = 0
        for (segment in 0 until orion.segmentCount) {
            val at = segment * Constellation.FLOATS_A_SEGMENT
            val from = AstronomyEngine.starHorizontal(orion.lines[at].toDouble(), orion.lines[at + 1].toDouble(), dark, milan)
            val to = AstronomyEngine.starHorizontal(orion.lines[at + 2].toDouble(), orion.lines[at + 3].toDouble(), dark, milan)
            if (from.altitudeDeg < 8.0 || to.altitudeDeg < 8.0) continue
            if (!lens.toScreen(from.azimuthDeg, from.altitudeDeg, a) || !lens.toScreen(to.azimuthDeg, to.altitudeDeg, b)) continue
            if (!lens.onScreen(a[0], a[1], -40f) || !lens.onScreen(b[0], b[1], -40f)) continue
            if (kotlin.math.hypot(b[0] - a[0], b[1] - a[1]) < 80f) continue
            val x = ((a[0] + b[0]) / 2f).roundToInt()
            val y = ((a[1] + b[1]) / 2f).roundToInt()
            val line = (-1..1).maxOf { dx -> (-1..1).maxOf { dy -> luminanceAt(on, x + dx, y + dy) } }
            val beside = luminanceAt(on, x + 14, y + 14)
            assertTrue("a line of Orion is drawn between its two stars ($line over $beside)", line > beside + 0.03f)
            checked++
        }
        assertTrue("some of Orion's lines are on the screen: $checked", checked >= 3)

        compose.onNode(text(R.string.explore_constellations)).performClick()
        compose.waitForIdle()
        assertEquals(false, figuresAsked)
        val off = renderWindow()
        save("explore-orion-plain", off)
        assertTrue("without the lines the picture is another", differ(on, off) > 500)
    }

    /**
     * The ribbon holds another hour of the night, and «Now» gives the clock back (step
     * 16.7). At 23:00 with nothing held the page is on now and offers no way back to it.
     */
    @Test
    fun `the ribbon holds another hour, and now gives the clock back`() {
        show(held = null, now = dark)
        compose.onNode(hasText("23:00")).assertExists()
        compose.onNode(text(R.string.explore_now)).assertDoesNotExist()
        // A screen reader's step along the ribbon: whole hours from dusk.
        ribbon.performSemanticsAction(SemanticsActions.SetProgress) { it(0.75f) }
        compose.waitForIdle()
        compose.onNode(hasText("23:00")).assertDoesNotExist()
        save("explore-held", renderWindow())
        compose.onNode(text(R.string.explore_now)).performClick()
        compose.waitForIdle()
        compose.onNode(hasText("23:00")).assertExists()
        compose.onNode(text(R.string.explore_now)).assertDoesNotExist()
    }

    /**
     * The night before and the night after (step 16.7), with the hour carried along; a
     * night that is over and one the forecast does not reach say why they have no cloud.
     */
    @Test
    fun `the arrows step a night, and each night says what the forecast knows of it`() {
        show()
        compose.onNode(hasContentDescription(app.getString(R.string.explore_night_next))).performClick()
        compose.waitForIdle()
        assertEquals("Night of Thursday 12 November", nightSays())
        // The hour held goes along, and the forecast, two days long, has that night too.
        compose.onNode(hasText("23:00")).assertExists()
        compose.onNode(hasText("Cloud 12%")).assertExists()
        compose.onNode(text(R.string.explore_now)).assertExists()

        compose.onNode(hasContentDescription(app.getString(R.string.explore_night_next))).performClick()
        compose.waitForIdle()
        assertEquals("Night of Friday 13 November", nightSays())
        compose.onNode(text(R.string.explore_no_clouds_beyond)).assertExists()
        compose.onNode(text(R.string.window_bare_note)).assertDoesNotExist()
        save("explore-beyond", renderWindow())

        repeat(3) { compose.onNode(hasContentDescription(app.getString(R.string.explore_night_previous))).performClick() }
        compose.waitForIdle()
        assertEquals("Night of Tuesday 10 November", nightSays())
        compose.onNode(text(R.string.explore_no_clouds_past)).assertExists()

        compose.onNode(text(R.string.explore_now)).performClick()
        compose.waitForIdle()
        assertEquals("Tonight, Wednesday 11 November", nightSays())
    }

    /** Before dusk there is no «now» on a night's ribbon: from another night the way back is «Tonight». */
    @Test
    fun `by day the page opens on the night's middle, and tonight is the way back`() {
        val afternoon = Instant.parse("2026-11-11T14:00:00Z")
        show(held = null, now = afternoon)
        // The night's middle, the hour the hero draws by day: a few minutes past midnight.
        val span = SkyScheduler.darkNight(LocalDate.of(2026, 11, 11), zone, milan).night as SkyOccurrence.At
        val middle = span.start.plus(Duration.between(span.start, span.end).dividedBy(2)).atZone(zone)
        assertTrue("the middle of that night is round midnight: $middle", middle.hour == 0 || middle.hour == 23)
        compose.onNode(hasText(middle.format(DateTimeFormatter.ofPattern("HH:mm")))).assertExists()
        compose.onNode(text(R.string.explore_now)).assertDoesNotExist()
        compose.onNode(text(R.string.explore_tonight)).assertDoesNotExist()
        compose.onNode(hasContentDescription(app.getString(R.string.explore_night_next))).performClick()
        compose.waitForIdle()
        compose.onNode(text(R.string.explore_tonight)).performClick()
        compose.waitForIdle()
        assertEquals("Tonight, Wednesday 11 November", nightSays())
        compose.onNode(text(R.string.explore_tonight)).assertDoesNotExist()
    }

    /** The night's name is the button that opens the calendar. */
    @Test
    fun `the night opens a calendar, and a day of it is the evening of a night`() {
        show()
        night.performClick()
        compose.waitForIdle()
        compose.onNode(text(R.string.explore_pick_night)).assertExists()
        compose.onNode(hasText("20 November 2026", substring = true)).performClick()
        compose.onNode(hasText(app.getString(android.R.string.ok))).performClick()
        compose.waitForIdle()
        assertEquals("Night of Friday 20 November", nightSays())
        compose.onNode(text(R.string.explore_no_clouds_beyond)).assertExists()
    }

    // ------------------------------------------------------------ a thing pointed at

    /**
     * Step 16.8, board 4. Saturn in the south-west of the moonless night: a tap within a
     * finger of it opens its sheet in the panel's place, the camera comes closer with the
     * planet in the middle of the sky that is left, and the sheet says what the engine says.
     */
    @Test
    fun `a tap on a planet brings it closer and opens its sheet in the panel's place`() {
        show(facingDeg = 225.0)
        renderWindow()
        val saturn = SkyTarget.Wanderer(Planet.SATURN)
        // Twenty dp off the dot: inside a finger's reach.
        tapOn(saturn, dxDp = 20f)
        sheet.assertExists()
        hour.assertDoesNotExist()
        compose.onNode(hasText("Saturn")).assertExists()
        compose.onNode(hasText("Planet · in the south-west", substring = true)).assertExists()
        // The hour's sky, with its number and the hour it is the number of.
        compose.onNode(hasContentDescription("Great, cloud 12% at 23:00")).assertExists()
        // The three boxes, at the engine's own minutes, in the place's zone.
        val night = SkyScheduler.darkNight(LocalDate.of(2026, 11, 11), zone, milan).night as SkyOccurrence.At
        val passage = AstronomyEngine.passage(saturn, night.start..night.end!!, milan)
        val clock = DateTimeFormatter.ofPattern("HH:mm")
        fun at(instant: Instant) = instant.atZone(zone).format(clock)
        compose.onNode(hasContentDescription("Rises at ${at(passage.rise!!.at)}, in the east", substring = true)).assertExists()
        compose.onNode(hasContentDescription("Highest at ${at(passage.highest.at)}, in the south, halfway up")).assertExists()
        compose.onNode(hasContentDescription("Sets at ${at(passage.set!!.at)}, in the west", substring = true)).assertExists()
        val until = AstronomyEngine.worthUntil(saturn, night.start..night.end!!, milan, 10.0)!!
        compose.onNode(text(R.string.explore_object_until, at(until))).assertExists()
        compose.onNode(text(R.string.explore_eye_saturn)).assertExists()

        // The camera went to it: closer, and the planet in the middle of the sky over the sheet.
        val bitmap = renderWindow()
        assertEquals(35.0, drawn!!.fieldDeg, 1e-6)
        val place = AstronomyEngine.horizontal(saturn, dark, milan)
        val out = FloatArray(2)
        assertTrue(lens(bitmap).toScreen(place.azimuthDeg, place.altitudeDeg, out))
        assertEquals(bitmap.width / 2f, out[0], 2f)
        val sheetTop = sheet.fetchSemanticsNode().boundsInRoot.top
        assertTrue("the planet (${out[1]}) is over the sheet ($sheetTop)", out[1] < sheetTop - 40f)
        // And a ring round it: the canvas' ink a ring's radius from the dot, where the sky was.
        val ring = (13f * compose.density.density).roundToInt()
        val onRing = Color(bitmap.getPixel(out[0].roundToInt() + ring, out[1].roundToInt())).luminance()
        val beside = Color(bitmap.getPixel(out[0].roundToInt() + ring * 2, out[1].roundToInt())).luminance()
        assertTrue("the ring ($onRing) stands out of the sky beside it ($beside)", onRing > beside + 0.25f)
        save("explore-object", bitmap)
    }

    @Test
    fun `the bell sets a reminder on the planet's own line, and the link opens its page`() {
        show(facingDeg = 225.0)
        renderWindow()
        tapOn(SkyTarget.Wanderer(Planet.SATURN))
        compose.onNode(text(R.string.explore_remind)).assertExists()
        bell.performClick()
        compose.waitForIdle()
        assertEquals(setOf("saturn.night"), bells)
        compose.onNode(text(R.string.explore_reminded)).assertExists()
        // Again, and the bell is off.
        bell.performClick()
        compose.waitForIdle()
        assertEquals(emptySet<String>(), bells)
        compose.onNode(text(R.string.explore_remind)).assertExists()

        page.performClick()
        assertEquals("saturn.night", pageAsked)
    }

    /** The step's own words: a star has no bell and no page, and the sheet does not feign them. */
    @Test
    fun `a star's sheet has its name, its place and its passage, and no bell and no page`() {
        show(facingDeg = 45.0)
        renderWindow()
        // Capella, high in the north-east at that hour.
        tapOn(SkyTarget.Fixed(79.172, 45.998))
        sheet.assertExists()
        compose.onNode(hasText("Capella")).assertExists()
        compose.onNode(hasText("Star · ", substring = true)).assertExists()
        compose.onNode(hasContentDescription("Highest at ", substring = true)).assertExists()
        bell.assertDoesNotExist()
        page.assertDoesNotExist()
        save("explore-object-star", renderWindow())
    }

    @Test
    fun `a tap on a constellation's name opens the constellation`() {
        show(facingDeg = 135.0, figures = true)
        renderWindow()
        // Orion's name, where the page wrote it.
        val orion = Constellations.byId("Ori")!!.labels.first()
        tapOn(orion)
        sheet.assertExists()
        compose.onNode(hasText("Orion")).assertExists()
        compose.onNode(hasText("Constellation · ", substring = true)).assertExists()
        bell.assertDoesNotExist()
        page.assertDoesNotExist()
        // A region and not a light: the field stays the one that holds it.
        renderWindow()
        assertEquals(SkyCamera.RestingFieldDeg, drawn!!.fieldDeg, 1e-6)
    }

    @Test
    fun `the sheet closes with its cross, with a tap on empty sky and with back, and the panel returns`() {
        show(facingDeg = 225.0)
        renderWindow()
        val saturn = SkyTarget.Wanderer(Planet.SATURN)
        tapOn(saturn)
        sheet.assertExists()
        compose.onNode(hasContentDescription(app.getString(R.string.explore_object_close))).performClick()
        compose.waitForIdle()
        sheet.assertDoesNotExist()
        hour.assertExists()

        renderWindow()
        tapOn(saturn)
        sheet.assertExists()
        // Empty sky: forty degrees up to the north-west there is no name within a finger.
        renderWindow()
        tapAt(drawn!!.azimuthDeg + 12.0, drawn!!.altitudeDeg + 14.0)
        sheet.assertDoesNotExist()

        renderWindow()
        tapOn(saturn)
        sheet.assertExists()
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
        sheet.assertDoesNotExist()
        assertEquals("back closed the sheet, not the page", 0, closed)
    }

    @Test
    fun `a tap far from everything opens nothing`() {
        show(facingDeg = 225.0)
        renderWindow()
        tapAt(drawn!!.azimuthDeg + 12.0, drawn!!.altitudeDeg + 14.0)
        sheet.assertDoesNotExist()
        hour.assertExists()
    }

    /** For a reader who cannot point: the planets on the screen are actions of the sky's own. */
    @Test
    fun `a planet can be opened without a finger`() {
        show(facingDeg = 225.0)
        renderWindow()
        val actions = sky.fetchSemanticsNode().config[SemanticsActions.CustomActions]
        val open = actions.single { it.label == "Open: Saturn" }
        compose.runOnUiThread { open.action() }
        compose.waitForIdle()
        sheet.assertExists()
        compose.onNode(hasText("Saturn")).assertExists()
    }

    @Test
    fun `on a night the forecast does not reach the sheet has no verdict`() {
        show(facingDeg = 225.0, forecast = null)
        renderWindow()
        tapOn(SkyTarget.Wanderer(Planet.SATURN))
        sheet.assertExists()
        compose.onNode(hasContentDescription("cloud", substring = true)).assertDoesNotExist()
        assertNull(pageAsked)
    }

    /** A phone on its side: the sheet is the card in the corner, and the thing stands in the sky beside it. */
    @Test
    @Config(qualifiers = "en-rGB-w832dp-h384dp-land-xhdpi")
    fun `on a wide window the sheet is the card in the corner and the thing stands beside it`() {
        show(facingDeg = 225.0)
        renderWindow()
        val saturn = SkyTarget.Wanderer(Planet.SATURN)
        val place = AstronomyEngine.horizontal(saturn, dark, milan)
        // Saturn stands beside the line at the top of the page there: the sky beside the
        // words is sky, and a finger on it is a finger on the planet.
        tapOn(saturn)
        sheet.assertExists()
        hour.assertDoesNotExist()
        val bitmap = renderWindow()
        val out = FloatArray(2)
        assertTrue(lens(bitmap).toScreen(place.azimuthDeg, place.altitudeDeg, out))
        val card = sheet.fetchSemanticsNode().boundsInRoot
        assertTrue("the planet (${out[0]}) is to the right of the sheet (${card.right})", out[0] > card.right + 40f)
        assertTrue("the sheet keeps to its share of the height", card.height <= bitmap.height * 0.63f)
        save("explore-object-landscape", bitmap)
    }

    /**
     * A phone on its side, or a tablet: the panel is a card in the corner, nothing stands
     * across the foot, and the sky keeps the floor it had before there was a panel.
     */
    @Test
    @Config(qualifiers = "en-rGB-w832dp-h384dp-land-xhdpi")
    fun `on a wide window the panel is a card in the corner and the sky keeps its floor`() {
        show(figures = true)
        val bitmap = renderWindow()
        save("explore-landscape", bitmap)
        val out = FloatArray(2)
        assertTrue(lens(bitmap).toScreen(135.0, 0.0, out))
        assertEquals(SkyCamera.HorizonLine, out[1] / bitmap.height, 0.01f)
        val panel = hour.fetchSemanticsNode().boundsInRoot
        assertTrue("the panel is in the corner: ${panel.right} of ${bitmap.width}", panel.right < bitmap.width * 0.55f)
        compose.onNode(hasText("23:00")).assertExists()
        assertEquals("Tonight, Wednesday 11 November", nightSays())
    }

    /** How many px of the sky over the horizon are a light: a star, a planet, their glow. */
    private fun lights(bitmap: Bitmap): Int {
        val horizon = FloatArray(2)
        lens(bitmap).toScreen(drawn!!.azimuthDeg, 0.0, horizon)
        var n = 0
        // Under the top row and its switches, over the horizon's line and its words.
        for (y in (bitmap.height * 0.18f).toInt() until (horizon[1] - 80f).toInt()) {
            for (x in 0 until bitmap.width) if (luminanceAt(bitmap, x, y) > 0.35f) n++
        }
        return n
    }

    private fun differ(a: Bitmap, b: Bitmap): Int {
        var n = 0
        for (y in 0 until a.height step 2) for (x in 0 until a.width step 2) if (a.getPixel(x, y) != b.getPixel(x, y)) n++
        return n
    }

    private fun luminanceAt(bitmap: Bitmap, x: Int, y: Int): Float {
        val p = bitmap.getPixel(x.coerceIn(0, bitmap.width - 1), y.coerceIn(0, bitmap.height - 1))
        return Color(((p shr 16) and 0xFF) / 255f, ((p shr 8) and 0xFF) / 255f, (p and 0xFF) / 255f).luminance()
    }

    private fun save(name: String, bitmap: Bitmap) {
        System.getenv("CHIARO_RENDER_DIR")?.let { dir ->
            File(dir).apply { mkdirs() }.resolve("$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    private fun renderWindow(): Bitmap {
        val view = compose.activity.window.decorView
        val width = view.width
        val height = view.height
        val node = RenderNode("window").apply { setPosition(0, 0, width, height) }
        compose.runOnUiThread { view.draw(node.beginRecording()); node.endRecording() }
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
}
