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
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pinch
import androidx.test.core.app.ApplicationProvider
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.data.AppSettings
import com.callbackdev.chiaro.domain.model.City
import com.callbackdev.chiaro.domain.model.Coordinates
import com.callbackdev.chiaro.domain.model.HourlyForecast
import com.callbackdev.chiaro.domain.model.MoonPhase
import com.callbackdev.chiaro.domain.model.WeatherCondition
import com.callbackdev.chiaro.domain.model.WeatherReport
import com.callbackdev.chiaro.domain.sample.sampleWeatherReport
import com.callbackdev.chiaro.domain.sky.AstronomyEngine
import com.callbackdev.chiaro.domain.sky.Constellation
import com.callbackdev.chiaro.domain.sky.Constellations
import com.callbackdev.chiaro.domain.sky.Planet
import com.callbackdev.chiaro.domain.sky.SightSubject
import com.callbackdev.chiaro.domain.sky.SkyBody
import com.callbackdev.chiaro.domain.sky.SkyJobCatalog
import com.callbackdev.chiaro.domain.sky.SkyOccurrence
import com.callbackdev.chiaro.domain.sky.SkyScheduler
import com.callbackdev.chiaro.domain.sky.SkyTarget
import com.callbackdev.chiaro.domain.sky.WorthSeeing
import com.callbackdev.chiaro.ui.format.LocalClock
import com.callbackdev.chiaro.ui.sky.SkyNights
import com.callbackdev.chiaro.ui.sky.compass.CompassState
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

    /** The phone's compass as the page reads it (step 16.10), and whether it is being read.
     * A phone with none until a test gives it one, as a Robolectric phone is. */
    private val phone = mutableStateOf(CompassState(supported = false))
    private var listening = false

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
        figures: Boolean = false,
        openDate: LocalDate? = null,
        openPick: String? = null,
        eventNights: List<ExploreEventNight> = emptyList(),
        at: Coordinates = milan
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
                        coordinates = at,
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
                        onFrame = { frames++; drawn = it },
                        openDate = openDate,
                        openPick = openPick,
                        eventNights = eventNights,
                        phone = { on -> listening = on; phone }
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
        // And says why the sky behind it has no clouds (Fase 16.11): the panel's line is
        // under the sheet, and a clear sky nobody forecast is not left without a word.
        compose.onNode(text(R.string.explore_no_clouds_none)).assertExists()
        assertNull(pageAsked)
    }

    /**
     * A thing low over the horizon is not left behind its own sheet (Fase 16.11): the camera
     * was aimed over the panel's horizon, the sheet is taller than the panel, and Saturn an
     * hour before it set stood under the words about it.
     */
    @Test
    fun `a thing low over the horizon stands over its sheet, ring and all`() {
        val saturn = SkyTarget.Wanderer(Planet.SATURN)
        // The first ten-minute step of that night with Saturn under thirteen degrees, going down.
        val low = generateSequence(dark) { it.plusSeconds(600) }
            .first { AstronomyEngine.horizontal(saturn, it, milan).altitudeDeg < 13.0 }
        val place = AstronomyEngine.horizontal(saturn, low, milan)
        assertTrue("Saturn is still up (${place.altitudeDeg})", place.altitudeDeg > 10.0)
        show(held = low, now = dark, facingDeg = place.azimuthDeg)
        renderWindow()
        tapOn(saturn, at = low)
        sheet.assertExists()
        val bitmap = renderWindow()
        val out = FloatArray(2)
        assertTrue(lens(bitmap).toScreen(place.azimuthDeg, place.altitudeDeg, out))
        val sheetTop = sheet.fetchSemanticsNode().boundsInRoot.top
        val ring = 13f * compose.density.density
        assertTrue("the planet and its ring (${out[1] + ring}) are over the sheet ($sheetTop)", out[1] + ring < sheetTop)
        save("explore-object-low", bitmap)
    }

    /**
     * What stands over the sky is not the sky only where it has a ground of its own (the tail
     * of Fase 16): the row of the two buttons is as wide as the screen and bare between them,
     * and a tap there did nothing, not even close the sheet.
     */
    @Test
    fun `a tap on the bare row beside the two buttons is a tap on the sky`() {
        show(facingDeg = 225.0)
        renderWindow()
        tapOn(SkyTarget.Wanderer(Planet.SATURN))
        sheet.assertExists()
        compose.onNode(hasText("Saturn")).assertExists()
        val y = closer.fetchSemanticsNode().boundsInRoot.center.y
        sky.performTouchInput { click(Offset(width * 0.3f, y)) }
        compose.waitForIdle()
        // Empty sky closes the sheet; a light that happened to stand there would open its own.
        compose.onNode(hasText("Saturn")).assertDoesNotExist()
    }

    /** A finger on the time panel's ground is the panel's: it turned the sky under it (the
     * tail of Fase 16), and took it back from a phone that was pointing. */
    @Test
    fun `a finger that moves on the panel's ground does not turn the sky`() {
        show()
        drag(-40f, 0f) // the first sequence is lost under Robolectric
        drag(-40f, 0f)
        renderWindow()
        val before = drawn!!
        hour.performTouchInput {
            down(center)
            repeat(10) { moveBy(Offset(-12f * density, 0f), delayMillis = 16) }
            up()
        }
        compose.waitForIdle()
        renderWindow()
        assertEquals(before.azimuthDeg, drawn!!.azimuthDeg, 1e-9)
        assertEquals(before.altitudeDeg, drawn!!.altitudeDeg, 1e-9)
    }

    /** Venus before dawn rides the morning star's line, and the bell says mornings (Fase 16.11). */
    @Test
    fun `the morning star's bell says mornings`() {
        val venus = SkyTarget.Wanderer(Planet.VENUS)
        val dawn = Instant.parse("2026-12-10T05:00:00Z")
        val place = AstronomyEngine.horizontal(venus, dawn, milan)
        assertTrue("Venus is up before the sun (${place.altitudeDeg})", place.altitudeDeg > 10.0)
        show(held = dawn, now = dawn, facingDeg = place.azimuthDeg, forecast = null)
        renderWindow()
        tapOn(venus, at = dawn)
        sheet.assertExists()
        compose.onNode(text(R.string.explore_remind_morning)).assertExists()
        compose.onNode(text(R.string.explore_remind)).assertDoesNotExist()
        bell.performClick()
        compose.waitForIdle()
        assertEquals(setOf(SkyJobCatalog.VenusMorning.id), bells)
        compose.onNode(text(R.string.explore_reminded_morning)).assertExists()
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

    // ------------------------------------------------------------------ worth seeing (16.9)

    private val list get() = compose.onNode(hasTestTag(ExploreSightsTag))
    private val clockFmt = DateTimeFormatter.ofPattern("HH:mm")
    private fun clockOf(instant: Instant) = instant.atZone(zone).format(clockFmt)

    private fun openList() {
        compose.onNode(hasTestTag(ExploreListTag)).performClick()
        compose.waitForIdle()
    }

    /**
     * Step 16.9, board 5. The button at the top opens the list of the night the page is on:
     * every row the engine has for it, each with the hour it stands highest, where it
     * stands then and the stretch it is worth looking in.
     */
    @Test
    fun `worth seeing lists what the night holds, each at its hour`() {
        show(facingDeg = 225.0)
        list.assertDoesNotExist()
        openList()
        list.assertExists()
        compose.onNode(hasText("Tonight, Wednesday 11 November")).assertExists()
        val sights = WorthSeeing.of(LocalDate.of(2026, 11, 11), zone, milan)
        assertTrue("a November night from Milan has planets and figures: ${sights.size}", sights.size >= 6)
        sights.forEach { sight ->
            val name = SightWords.name(app, sight.subject)
            val line = SightWords.line(app, sight, zone, clockFmt, java.util.Locale.UK)
            assertTrue("a row for $name", name.isNotEmpty())
            compose.onNode(hasText(name) and hasText(line) and hasText(clockOf(sight.best))).assertExists()
        }
        // The words of one of them, read whole: Saturn tops in the south that evening.
        val saturn = sights.first { it.subject == SightSubject.Wanderer(Planet.SATURN) }
        assertEquals(
            "In the south, halfway up\nFrom ${clockOf(saturn.window.start)} to ${clockOf(saturn.window.endInclusive)}",
            SightWords.line(app, saturn, zone, clockFmt, java.util.Locale.UK)
        )
        // The forecast reaches this night: no line about a sky drawn without clouds.
        compose.onNode(text(R.string.sights_no_clouds_beyond)).assertDoesNotExist()
        compose.onNode(text(R.string.sights_legend)).assertExists()
    }

    /** A row takes the page to the thing's own hour and turns the camera to it, with its sheet. */
    @Test
    fun `a row takes the sky to the thing's hour, turns to it and opens its sheet`() {
        show(facingDeg = 45.0)
        renderWindow()
        openList()
        val saturn = WorthSeeing.of(LocalDate.of(2026, 11, 11), zone, milan)
            .first { it.subject == SightSubject.Wanderer(Planet.SATURN) }
        assertNotEquals("the page is on another hour", dark, saturn.best)
        compose.onNode(hasText("Saturn") and hasText(clockOf(saturn.best))).performClick()
        compose.waitForIdle()
        list.assertDoesNotExist()
        sheet.assertExists()
        compose.onNode(hasText("Planet · in the south, halfway up")).assertExists()
        // The sheet's sky is the row's hour: the verdict chip says which.
        compose.onNode(hasContentDescription("Great, cloud 12% at ${clockOf(saturn.best)}")).assertExists()
        // And the camera is on it, closer, over the sheet.
        val bitmap = renderWindow()
        assertEquals(35.0, drawn!!.fieldDeg, 1e-6)
        val out = FloatArray(2)
        assertTrue(lens(bitmap).toScreen(saturn.azimuthDeg, saturn.altitudeDeg, out))
        assertEquals(bitmap.width / 2f, out[0], 2f)
        assertTrue(out[1] < sheet.fetchSemanticsNode().boundsInRoot.top - 40f)
        save("explore-sight-row", bitmap)
        // Closing the sheet leaves the page on that hour, held.
        compose.onNode(hasContentDescription(app.getString(R.string.explore_object_close))).performClick()
        compose.waitForIdle()
        compose.onNode(hasText(clockOf(saturn.best))).assertExists()
    }

    /** A figure of the list is stars, not a written name: its sheet, and a ring round them all. */
    @Test
    fun `a figure's row opens the figure, with a ring round its stars and no bell`() {
        show(facingDeg = 225.0)
        renderWindow()
        openList()
        val orion = WorthSeeing.of(LocalDate.of(2026, 11, 11), zone, milan)
            .first { (it.subject as? SightSubject.Figure)?.figure?.id == "orion" }
        // Far down the list, under the fold of a sheet at rest: the row's own action, since a
        // finger would have to scroll to it first.
        compose.onNode(hasText("Orion") and hasText(clockOf(orion.best))).performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        sheet.assertExists()
        compose.onNode(hasText("Constellation · ", substring = true)).assertExists()
        bell.assertDoesNotExist()
        page.assertDoesNotExist()
        val bitmap = renderWindow()
        save("explore-sight-figure", bitmap)
        // A region: the field that holds it, and every star of it inside the ring.
        assertEquals(SkyCamera.RestingFieldDeg, drawn!!.fieldDeg, 1e-6)
        val lens = lens(bitmap)
        val middle = FloatArray(2)
        val star = FloatArray(2)
        assertTrue(lens.toScreen(orion.azimuthDeg, orion.altitudeDeg, middle))
        val facts = ExploreObjects.facts(
            SkyPick.Shape("orion"), SkyDome.of(orion.best, milan, zone, emptySet()), null, milan, orion
        )!!
        val ring = extentRingDp(facts.place, facts.extent, lens, compose.density.density) * compose.density.density
        assertEquals(7, facts.extent.size)
        facts.extent.forEach { point ->
            assertTrue(lens.toScreen(point.east, point.north, point.up, star))
            val away = kotlin.math.hypot(star[0] - middle[0], star[1] - middle[1])
            assertTrue("a star at $away px is inside the ring of $ring", away < ring - 10f)
        }
        assertTrue("the ring fits the sky over the sheet: $ring of ${bitmap.width}", ring < bitmap.width / 2f)
    }

    /** The night at the list's head is the page's night: choosing another turns the page to it. */
    @Test
    fun `the list's head changes the night, and the page goes with it`() {
        show(facingDeg = 225.0, eventNights = listOf(ExploreEventNight(LocalDate.of(2026, 12, 13), "meteor.geminids.peak")))
        openList()
        compose.onNode(hasText("Tomorrow")).performClick()
        compose.waitForIdle()
        compose.onNode(hasText("Night of Thursday 12 November")).assertExists()
        assertEquals("Night of Thursday 12 November", nightSays())
        // The night of an event ahead, by its name: past the forecast, and the list says so.
        compose.onNode(hasText("Geminids · 13 Dec")).performClick()
        compose.waitForIdle()
        assertEquals("Night of Sunday 13 December", nightSays())
        compose.onNode(text(R.string.sights_no_clouds_beyond)).assertExists()
        val geminids = WorthSeeing.of(LocalDate.of(2026, 12, 13), zone, milan).first { it.subject is SightSubject.Radiant }
        compose.onNode(hasText("Geminids") and hasText(clockOf(geminids.best))).assertExists()
        // Back to tonight from the same row of chips.
        compose.onNode(hasText("Tonight") and androidx.compose.ui.test.hasClickAction()).performClick()
        compose.waitForIdle()
        assertEquals("Tonight, Wednesday 11 November", nightSays())
        // And the calendar is one of the choices.
        compose.onNode(hasTestTag(ExploreSightsCalendarTag)).performClick()
        compose.waitForIdle()
        compose.onNode(hasText("20 November 2026", substring = true)).assertExists()
    }

    /** From «See that night's sky»: the page opens on the event's night, on its thing. */
    @Test
    fun `opened on an event's night the page is on that night, turned to the radiant, with its sheet`() {
        val date = LocalDate.of(2026, 12, 13)
        val radiant = WorthSeeing.of(date, zone, milan).first { it.subject is SightSubject.Radiant }
        show(
            held = radiant.best,
            facingDeg = radiant.azimuthDeg,
            now = dark,
            openDate = date,
            openPick = "radiant:meteor.geminids.peak"
        )
        val bitmap = renderWindow()
        save("explore-event-night", bitmap)
        sheet.assertExists()
        compose.onNode(hasText("Geminids")).assertExists()
        compose.onNode(hasText("Meteor shower · ", substring = true)).assertExists()
        // The peak's own bell, and no verdict: the forecast does not reach December.
        compose.onNode(text(R.string.explore_remind_peak)).assertExists()
        compose.onNode(hasContentDescription("cloud", substring = true)).assertDoesNotExist()
        // The sheet covers the panel's line, so the sheet says it: no clouds, and why.
        compose.onNode(text(R.string.explore_no_clouds_beyond)).assertExists()
        val out = FloatArray(2)
        assertTrue(lens(bitmap).toScreen(radiant.azimuthDeg, radiant.altitudeDeg, out))
        assertEquals(bitmap.width / 2f, out[0], 2f)
        // Closed, the panel is the event's night, with the way back to tonight.
        compose.onNode(hasContentDescription(app.getString(R.string.explore_object_close))).performClick()
        compose.waitForIdle()
        assertEquals("Night of Sunday 13 December", nightSays())
        compose.onNode(text(R.string.explore_now)).assertExists()
    }

    /**
     * A weak shower's radiant is drawn only for a reader who follows it. One who came
     * through that shower's own row asked for it by name: it is drawn, and on the list.
     */
    @Test
    fun `a weak shower nobody follows is drawn for the reader who came through its row`() {
        val date = LocalDate.of(2026, 12, 22)
        val ursids = "meteor.ursids.peak"
        assertTrue(WorthSeeing.of(date, zone, milan).none { it.subject is SightSubject.Radiant })
        val radiant = WorthSeeing.of(date, zone, milan, setOf(ursids)).first { it.subject is SightSubject.Radiant }
        show(held = radiant.best, facingDeg = radiant.azimuthDeg, now = dark, openDate = date, openPick = "radiant:$ursids")
        renderWindow()
        sheet.assertExists()
        compose.onNode(hasText("Ursids")).assertExists()
        openList()
        compose.onNode(hasText("Ursids") and hasText(clockOf(radiant.best))).assertExists()
    }

    /**
     * The phase's name is the day's, and «new moon» owns a day and three quarters either
     * side of the instant. The hair of a crescent before dawn on 13 July 2026 is on the
     * list, lit by three parts in a hundred: the row and the sheet call it what it is.
     */
    @Test
    fun `a thin crescent is on the list as a crescent, not as a new moon`() {
        val moon = WorthSeeing.of(LocalDate.of(2026, 7, 12), zone, milan).first { it.subject is SightSubject.Moon }
        val subject = moon.subject as SightSubject.Moon
        assertEquals(MoonPhase.NEW_MOON, subject.phase)
        assertEquals(3, subject.illuminationPct)
        val line = SightWords.line(app, moon, zone, clockFmt, java.util.Locale.UK)
        assertTrue(line, line.startsWith("Thin crescent, 3% lit · in the "))
        assertEquals("Thin crescent, 3% lit", ObjectWords.moonKind(app, MoonPhase.NEW_MOON, 3))
        // Before the new moon it is the waning one that is drawn; a moon with nothing lit stays new.
        assertEquals(MoonPhase.WANING_CRESCENT, ObjectWords.moonDrawn(subject.phase, subject.illuminationPct, moon.best))
        assertEquals("New moon, 0% lit", ObjectWords.moonKind(app, MoonPhase.NEW_MOON, 0))
        assertEquals("Waxing gibbous, 97% lit", ObjectWords.moonKind(app, MoonPhase.WAXING_GIBBOUS, 97))
    }

    /**
     * A row can take the page to an hour outside its night (Venus in the dusk, before the
     * astronomical night begins). The hour is printed; the ribbon has no such instant, and
     * a disc held at its nearer end would point at an hour the page is not showing.
     */
    @Test
    fun `an hour outside the night has no disc on the ribbon`() {
        // 17:45 in Milan on 11 Nov 2026: the astronomical night begins at 18:40.
        show(held = Instant.parse("2026-11-11T16:45:00Z"), now = dark)
        compose.onNode(hasText("17:45")).assertExists()
        fun brightestAtTheStart(bitmap: Bitmap): Float {
            val bounds = ribbon.fetchSemanticsNode().boundsInRoot
            val d = compose.density.density
            // Round the ribbon's first instant, over the two times written under it: the
            // band there is the dusk's dim blue, and a disc is the canvas' white.
            val xs = (bounds.left - 8f * d).roundToInt()..(bounds.left + 8f * d).roundToInt()
            val ys = bounds.top.roundToInt()..(bounds.top + 36f * d).roundToInt()
            return ys.maxOf { y -> xs.maxOf { x -> luminanceAt(bitmap, x, y) } }
        }
        val outside = renderWindow()
        save("explore-outside-night", outside)
        val without = brightestAtTheStart(outside)
        // The same ribbon with its first hour held: the disc is there.
        ribbon.performSemanticsAction(SemanticsActions.SetProgress) { it(0f) }
        compose.waitForIdle()
        val with = brightestAtTheStart(renderWindow())
        assertTrue("no disc at the ribbon's start ($without), and one when the hour is the night's first ($with)", without < 0.6f && with > 0.85f)
    }

    /** A date with nothing to send a reader out for says so, and why the list is empty. */
    @Test
    fun `a night with nothing on the list says so`() {
        // Longyearbyen under the midnight sun: no dark of any kind.
        show(held = null, facingDeg = 180.0, now = Instant.parse("2026-06-21T20:00:00Z"), forecast = null, at = Coordinates(78.22, 15.65))
        openList()
        compose.onNode(text(R.string.sights_none)).assertExists()
        compose.onNode(text(R.string.sights_legend)).assertDoesNotExist()
    }

    // «Point the phone» (step 16.10).

    private val point get() = compose.onNode(hasTestTag(ExplorePointTag))
    private val compassNote get() = compose.onNode(hasTestTag(ExploreCompassNoteTag))

    /** The page on a phone that has a compass, drawn once. */
    private fun showWithCompass(facingDeg: Double = 135.0) {
        phone.value = CompassState(supported = true)
        show(facingDeg = facingDeg)
        renderWindow()
    }

    /** The switch, by its own action: a finger's ripple would ask for drawings of its own. */
    private fun pointSwitch() {
        point.performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
    }

    /** The phone now points at [azimuthDeg], [altitudeDeg]: one reading of its compass. */
    private fun aimAt(azimuthDeg: Float, altitudeDeg: Float) {
        compose.runOnUiThread { phone.value = phone.value.copy(heading = azimuthDeg, altitude = altitudeDeg) }
        compose.waitForIdle()
    }

    @Test
    fun `a phone with no compass has no switch to point it`() {
        show()
        point.assertDoesNotExist()
        assertTrue("and nothing is read", !listening)
    }

    @Test
    fun `the switch is off when the page opens, and the compass is read only while it is on`() {
        showWithCompass()
        point.assertIsNotSelected()
        assertTrue("nothing is read before the switch is touched", !listening)
        pointSwitch()
        point.assertIsSelected()
        assertTrue(listening)
        pointSwitch()
        point.assertIsNotSelected()
        assertTrue("and not after it is turned off", !listening)
    }

    @Test
    fun `pointed, the sky is where the phone points and goes where it goes`() {
        showWithCompass(facingDeg = 135.0)
        pointSwitch()
        // No reading yet: the sky is where it was.
        renderWindow()
        assertEquals(135.0, drawn!!.azimuthDeg, 1e-6)
        // The first reading: the camera goes there, in one glide, and is there.
        aimAt(225f, 50f)
        renderWindow()
        assertEquals(225.0, drawn!!.azimuthDeg, 1e-3)
        assertEquals(50.0, drawn!!.altitudeDeg, 1e-3)
        assertEquals(60.0, drawn!!.fieldDeg, 1e-6)
        assertEquals("Looking south-west", looking())
        // And then it is the reading, each one.
        aimAt(300f, 72f)
        renderWindow()
        assertEquals(300.0, drawn!!.azimuthDeg, 1e-3)
        assertEquals(72.0, drawn!!.altitudeDeg, 1e-3)
        aimAt(10f, 90f)
        renderWindow()
        assertEquals(app.getString(R.string.explore_overhead), looking())
        save("explore-pointed", renderWindow())
    }

    @Test
    fun `a phone pointed at the ground leaves the sky on its horizon`() {
        showWithCompass(facingDeg = 135.0)
        val floor = drawn!!.altitudeDeg
        pointSwitch()
        aimAt(90f, -40f)
        renderWindow()
        assertEquals(90.0, drawn!!.azimuthDeg, 1e-3)
        assertEquals("the camera is on its floor, where the page opened", floor, drawn!!.altitudeDeg, 1e-6)
    }

    /** The battery's rule, with the sensors on: the page draws when the phone moves. */
    @Test
    fun `a phone held still asks for no drawing, and one that moves asks for one`() {
        showWithCompass()
        pointSwitch()
        aimAt(225f, 50f)
        renderWindow()
        val view = composeView()
        val before = frames
        compose.mainClock.autoAdvance = false
        compose.mainClock.advanceTimeBy(30_000)
        compose.runOnIdle { assertTrue("half a minute of holding still asked for no drawing", !view.isDirty) }
        compose.mainClock.autoAdvance = true
        aimAt(226f, 50f)
        compose.runOnIdle { assertTrue("a degree of turning asks for one", view.isDirty) }
        renderWindow()
        assertEquals(before + 1, frames)
    }

    @Test
    fun `a finger that drags the sky takes it back from the phone`() {
        showWithCompass()
        drag(-40f, 0f) // the first sequence is lost under Robolectric
        pointSwitch()
        aimAt(225f, 50f)
        renderWindow()
        drag(-120f, 0f)
        point.assertIsNotSelected()
        assertTrue("the compass is let go", !listening)
        renderWindow()
        val left = drawn!!
        assertNotEquals("the finger turned the sky", 225.0, left.azimuthDeg, 1.0)
        // The phone keeps moving: the sky is the finger's now.
        aimAt(10f, 30f)
        renderWindow()
        assertEquals(left, drawn)
    }

    @Test
    fun `the sky's own actions take it back too`() {
        showWithCompass()
        pointSwitch()
        aimAt(225f, 50f)
        val right = sky.fetchSemanticsNode().config[SemanticsActions.CustomActions]
            .first { it.label == app.getString(R.string.explore_turn_right) }
        compose.runOnUiThread { right.action() }
        compose.waitForIdle()
        point.assertIsNotSelected()
        renderWindow()
        assertEquals(245.0, drawn!!.azimuthDeg, 1e-3)
    }

    @Test
    fun `two fingers and the two buttons come closer and leave the sky to the phone`() {
        showWithCompass()
        drag(-40f, 0f) // the first sequence is lost under Robolectric
        pointSwitch()
        aimAt(225f, 50f)
        closer.performClick()
        compose.waitForIdle()
        point.assertIsSelected()
        renderWindow()
        assertEquals(40.0, drawn!!.fieldDeg, 1e-6)
        assertEquals(225.0, drawn!!.azimuthDeg, 1e-3)
        assertEquals(50.0, drawn!!.altitudeDeg, 1e-3)
        // A pinch that opens, off the middle of the sky: closer, and where the phone points.
        sky.performTouchInput {
            val y = height * 0.25f
            pinch(
                start0 = Offset(width * 0.40f, y), end0 = Offset(width * 0.15f, y),
                start1 = Offset(width * 0.60f, y), end1 = Offset(width * 0.85f, y)
            )
        }
        compose.waitForIdle()
        point.assertIsSelected()
        assertTrue(listening)
        renderWindow()
        assertTrue("the pinch came closer (${drawn!!.fieldDeg})", drawn!!.fieldDeg < 39.0)
        assertEquals(225.0, drawn!!.azimuthDeg, 1e-3)
        assertEquals(50.0, drawn!!.altitudeDeg, 1e-3)
        // And the phone still has it.
        aimAt(200f, 45f)
        renderWindow()
        assertEquals(200.0, drawn!!.azimuthDeg, 1e-3)
    }

    /**
     * Two fingers never leave a screen in one instant (Fase 16.11): the one still down when
     * the other lifts moved the sky as a drag does, and a pinch on a real phone switched
     * «Point the phone» off. The test above lifts both together and never saw it.
     */
    @Test
    fun `the finger left down at the end of a pinch does not take the sky from the phone`() {
        showWithCompass()
        drag(-40f, 0f) // the first sequence is lost under Robolectric
        pointSwitch()
        aimAt(225f, 50f)
        renderWindow()
        val before = drawn!!.fieldDeg
        sky.performTouchInput {
            val y = height * 0.25f
            down(0, Offset(width * 0.40f, y))
            down(1, Offset(width * 0.60f, y))
            repeat(8) {
                moveBy(0, Offset(-width * 0.03f, 0f), delayMillis = 16)
                moveBy(1, Offset(width * 0.03f, 0f), delayMillis = 16)
            }
            up(1)
            // The finger that is left wanders on before it lifts, as fingers do.
            repeat(6) { moveBy(0, Offset(width * 0.02f, height * 0.01f), delayMillis = 16) }
            up(0)
        }
        compose.waitForIdle()
        point.assertIsSelected()
        assertTrue(listening)
        renderWindow()
        assertTrue("the pinch came closer (${drawn!!.fieldDeg})", drawn!!.fieldDeg < before - 1.0)
        assertEquals(225.0, drawn!!.azimuthDeg, 1e-3)
        assertEquals(50.0, drawn!!.altitudeDeg, 1e-3)
    }

    @Test
    fun `a tap on a thing opens its sheet and the sky stays the phone's`() {
        showWithCompass(facingDeg = 225.0)
        val saturn = SkyTarget.Wanderer(Planet.SATURN)
        val place = AstronomyEngine.horizontal(saturn, dark, milan)
        pointSwitch()
        // The phone a little to the left of the planet, and over it.
        aimAt((place.azimuthDeg - 8.0).toFloat(), (place.altitudeDeg + 6.0).toFloat())
        renderWindow()
        val pointed = drawn!!
        tapOn(saturn)
        sheet.assertExists()
        compose.onNode(hasText("Saturn")).assertExists()
        point.assertIsSelected()
        renderWindow()
        // Not closer and not turned to it: the bearing is the phone's, and the field.
        assertEquals(pointed.azimuthDeg, drawn!!.azimuthDeg, 1e-6)
        assertEquals(60.0, drawn!!.fieldDeg, 1e-6)
        // The sheet is taller than the panel: the floor may have come up under the camera,
        // and the phone still moves it.
        aimAt((place.azimuthDeg + 20.0).toFloat(), 70f)
        renderWindow()
        assertEquals(SkyCamera.norm360(place.azimuthDeg + 20.0), drawn!!.azimuthDeg, 1e-3)
        assertEquals(70.0, drawn!!.altitudeDeg, 1e-3)
        sheet.assertExists()
    }

    @Test
    fun `the list rests the compass, and a row takes the sky from the phone`() {
        showWithCompass(facingDeg = 45.0)
        pointSwitch()
        aimAt(45f, 60f)
        openList()
        assertTrue("under the list the compass is not read", !listening)
        val saturn = WorthSeeing.of(LocalDate.of(2026, 11, 11), zone, milan)
            .first { it.subject == SightSubject.Wanderer(Planet.SATURN) }
        compose.onNode(hasText("Saturn") and hasText(clockOf(saturn.best))).performClick()
        compose.waitForIdle()
        sheet.assertExists()
        point.assertIsNotSelected()
        assertTrue(!listening)
        // The camera went to the row's thing, as it does with the switch off.
        val bitmap = renderWindow()
        assertEquals(35.0, drawn!!.fieldDeg, 1e-6)
        val out = FloatArray(2)
        assertTrue(lens(bitmap).toScreen(saturn.azimuthDeg, saturn.altitudeDeg, out))
        assertEquals(bitmap.width / 2f, out[0], 2f)
    }

    @Test
    fun `a compass that cannot be trusted says so while the sky hangs on it`() {
        showWithCompass()
        // Off, the compass is nobody's business.
        compose.runOnUiThread { phone.value = phone.value.copy(needsCalibration = true) }
        compose.waitForIdle()
        compassNote.assertDoesNotExist()
        pointSwitch()
        compassNote.assertExists()
        compose.onNode(text(R.string.compass_calibrate_title)).assertExists()
        compose.onNode(text(R.string.compass_calibrate_body)).assertExists()
        save("explore-compass-note", renderWindow())
        // Something nearby pulling it is the other sentence, the one no gesture fixes.
        compose.runOnUiThread { phone.value = phone.value.copy(needsCalibration = false, disturbed = true) }
        compose.waitForIdle()
        compose.onNode(text(R.string.compass_disturbed)).assertExists()
        compose.onNode(text(R.string.compass_calibrate_title)).assertDoesNotExist()
        // And the box goes when the reason does.
        compose.runOnUiThread { phone.value = phone.value.copy(disturbed = false) }
        compose.waitForIdle()
        compassNote.assertDoesNotExist()
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
