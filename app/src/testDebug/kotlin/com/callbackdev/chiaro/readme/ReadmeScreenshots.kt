package com.callbackdev.chiaro.readme

import android.Manifest
import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.domain.placeZone
import com.callbackdev.chiaro.ui.alerts.AlertsActions
import com.callbackdev.chiaro.ui.alerts.AlertsScreen
import com.callbackdev.chiaro.ui.format.LocalClock
import com.callbackdev.chiaro.ui.guide.GuideRoute
import com.callbackdev.chiaro.ui.icons.LocalAnimatedIcons
import com.callbackdev.chiaro.ui.icons.LocalWeatherIcons
import com.callbackdev.chiaro.ui.settings.SettingsActions
import com.callbackdev.chiaro.ui.settings.SettingsScreen
import com.callbackdev.chiaro.ui.shell.BottomBarHeight
import com.callbackdev.chiaro.ui.shell.ChiaroBottomBar
import com.callbackdev.chiaro.ui.shell.ShellTab
import com.callbackdev.chiaro.ui.sky.SkyActions
import com.callbackdev.chiaro.ui.sky.SkyScreen
import com.callbackdev.chiaro.ui.theme.ChiaroTheme
import com.callbackdev.chiaro.ui.today.TodayPage
import java.io.File
import java.time.Clock
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The README's pictures (`docs/screenshots/`), drawn from the app's own screens and a
 * recorded Milan ([MilanRecording]). Run with
 * `./gradlew :app:testDebugUnitTest --tests "*ReadmeScreenshots" -PupdateScreenshots`;
 * skipped otherwise, so an ordinary run never rewrites a committed image.
 *
 * In English, like the README, on the reference phone of the widget measurements
 * (384 × 832 dp) at twice its density; British English, so the clock is the 24-hour one
 * an Italian city reads. Nothing is animated: the icons are the still drawings and the
 * test clock does not run the loops. The phone's status bar is not in the picture.
 *
 * Which views are here is a rule of the repository (CLAUDE.md): regenerate them when a
 * change alters what one of them shows, add one when a change brings something worth
 * showing, and drop one whose screen is gone.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w384dp-h832dp-xhdpi")
class ReadmeScreenshots {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val output = System.getProperty("chiaro.readmeScreenshots")
    private val app: Application = ApplicationProvider.getApplicationContext()
    private val settings get() = MilanRecording.settings

    @Before
    fun onlyOnRequest() {
        assumeTrue("Run with -PupdateScreenshots", output != null)
        // A phone that has answered the notification question: without it Alerts and Sky
        // open on the "notifications are off" card, which is the right screen for that
        // reader and the wrong picture of the app.
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
    }

    // ------------------------------------------------------------------------ Today

    @Test
    fun todayNow() {
        showTab(ShellTab.TODAY) { today() }
        save("today-now")
    }

    @Test
    fun todayWeek() {
        showTab(ShellTab.TODAY) { today() }
        scrollUnderHeader(R.string.section_week)
        save("today-week")
    }

    @Test
    fun todayDetails() {
        showTab(ShellTab.TODAY) { today() }
        scrollToEnd(R.string.section_details)
        save("today-details")
    }

    @Composable
    private fun today() {
        val state = MilanRecording.today(app)
        TodayPage(
            state = state,
            title = state.city.name,
            isGps = false,
            dots = null,
            units = settings.units,
            locating = false,
            guideCardVisible = false,
            onRefresh = {},
            onOpenPlaces = {},
            onOpenSettings = {},
            onOpenGuide = {},
            onOpenJournal = {},
            onDismissGuideCard = {},
            isCurrent = true,
            onCanvasBehindBar = {}
        )
    }

    // -------------------------------------------------------------------------- Sky

    /** In the dark theme: Sky is the tab people open at night, and the README should
     * show the second dress somewhere. */
    @Test
    fun skyTonight() {
        showTab(ShellTab.SKY, dark = true) { sky() }
        save("sky-tonight")
    }

    @Test
    fun skyAhead() {
        showTab(ShellTab.SKY) { sky() }
        scrollUnderHeader(R.string.sky_section_events)
        save("sky-ahead")
    }

    @Composable
    private fun sky() = SkyScreen(
        state = MilanRecording.sky(),
        actions = SkyActions({}, {}, { _, _ -> }, {}, {}),
        onOpenPlaces = {},
        onOpenSettings = {},
        onOpenGuide = {}
    )

    // ----------------------------------------------------------- Alerts, Settings, guide

    @Test
    fun alerts() {
        showTab(ShellTab.ALERTS) {
            AlertsScreen(
                state = MilanRecording.alerts(app),
                actions = AlertsActions({}, {}, {}, {}, {}, {}, {}, { _, _ -> }),
                onEdit = {},
                onOpenPlaces = {},
                onOpenSettings = {}
            )
        }
        save("alerts")
    }

    @Test
    fun settings() {
        show {
            SettingsScreen(
                settings = settings,
                actions = SettingsActions({}, {}, {}, {}, {}, {}, {}, {}, {}, {}),
                onBack = {},
                onOpenGuide = {}
            )
        }
        save("settings")
    }

    @Test
    fun guide() {
        show { GuideRoute(onBack = {}, onOpenSkyGuide = {}) }
        save("guide")
    }

    // ---------------------------------------------------------------------- drawing

    /** The app's frame as `MainActivity` sets it up, with the fresh install's settings
     * and the clock stopped at the recording's moment. */
    private fun show(dark: Boolean = false, content: @Composable () -> Unit) {
        compose.setContent {
            ChiaroTheme(
                darkTheme = dark,
                dynamicColor = settings.dynamicColor,
                palette = settings.palette,
                font = settings.font
            ) {
                CompositionLocalProvider(
                    LocalWeatherIcons provides settings.weatherIcons,
                    LocalAnimatedIcons provides false,
                    LocalClock provides Clock.fixed(MilanRecording.now, placeZone(MilanRecording.report, MilanRecording.city))
                ) {
                    Surface(modifier = Modifier.fillMaxSize()) { content() }
                }
            }
        }
        compose.waitForIdle()
    }

    /** A tab page with the bottom bar over it, laid out as `ChiaroRoot` lays them out. */
    private fun showTab(tab: ShellTab, dark: Boolean = false, page: @Composable () -> Unit) = show(dark) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize().padding(bottom = BottomBarHeight)) { page() }
            Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                ChiaroBottomBar(selected = tab, onSelect = {})
            }
        }
    }

    /** The page's own vertical list: the one scrollable that scrolls up and down. */
    private val pageList = hasScrollToNodeAction() and
        SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)

    /** Brings the section titled [title] to the top, just under the pinned header. */
    private fun scrollUnderHeader(@StringRes title: Int) {
        val text = app.getString(title)
        compose.onNode(pageList).performScrollToNode(hasText(text))
        compose.waitForIdle()
        val top = compose.onNode(hasText(text)).fetchSemanticsNode().boundsInRoot.top
        val headerBottom = compose.onNode(
            SemanticsMatcher.expectValue(
                SemanticsProperties.ContentDescription,
                listOf(app.getString(R.string.settings_title))
            )
        ).fetchSemanticsNode().boundsInRoot.bottom
        val gap = with(compose.density) { 16.dp.toPx() }
        compose.onNode(pageList).performSemanticsAction(SemanticsActions.ScrollBy) {
            it(0f, top - headerBottom - gap)
        }
        compose.waitForIdle()
    }

    /** Scrolls past [title] to the end of the page. */
    private fun scrollToEnd(@StringRes title: Int) {
        compose.onNode(pageList).performScrollToNode(hasText(app.getString(title)))
        compose.onNode(pageList).performSemanticsAction(SemanticsActions.ScrollBy) {
            it(0f, 100_000f)
        }
        compose.waitForIdle()
    }

    /**
     * The activity's window drawn into a bitmap. Not `captureToImage`: on this Compose and
     * Robolectric it waits for a redraw on the very thread it is blocking and times out
     * (25 set 2026), where a plain `View.draw` under native graphics draws the same pixels.
     */
    private fun save(name: String) {
        compose.waitForIdle()
        val view = compose.activity.window.decorView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        val dir = File(checkNotNull(output)).apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
