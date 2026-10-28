package com.callbackdev.chiaro.readme

import android.Manifest
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.HardwareRenderer
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.PixelFormat
import android.graphics.RenderNode
import android.media.ImageReader
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.ui.unit.DpSize
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import com.callbackdev.chiaro.widget.NowWidgetContent
import com.callbackdev.chiaro.widget.TextWidgetContent
import com.callbackdev.chiaro.widget.WidgetKind
import com.callbackdev.chiaro.widget.window.WindowWidgetContent
import com.callbackdev.chiaro.widget.rememberSkyBitmap
import com.callbackdev.chiaro.widget.widgetSchemes
import kotlinx.coroutines.runBlocking
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.domain.AlertEngine
import com.callbackdev.chiaro.domain.AlertKind
import com.callbackdev.chiaro.domain.AlertState
import com.callbackdev.chiaro.domain.placeZone
import com.callbackdev.chiaro.notifications.AlertNotifier
import com.callbackdev.chiaro.ui.alerts.AlertsActions
import com.callbackdev.chiaro.ui.alerts.AlertsScreen
import com.callbackdev.chiaro.ui.format.Formats
import com.callbackdev.chiaro.ui.format.LocalClock
import com.callbackdev.chiaro.ui.guide.GuideRoute
import com.callbackdev.chiaro.ui.icons.LocalAnimatedIcons
import com.callbackdev.chiaro.ui.icons.LocalWeatherIcons
import com.callbackdev.chiaro.ui.settings.SettingsActions
import com.callbackdev.chiaro.ui.settings.SettingsScreen
import com.callbackdev.chiaro.ui.shell.BottomBarHeight
import com.callbackdev.chiaro.ui.shell.ChiaroBottomBar
import com.callbackdev.chiaro.ui.shell.ChiaroNavRail
import com.callbackdev.chiaro.ui.shell.LocalRailInset
import com.callbackdev.chiaro.ui.shell.RailMinWidth
import com.callbackdev.chiaro.ui.shell.railInset
import com.callbackdev.chiaro.ui.shell.ShellTab
import com.callbackdev.chiaro.ui.sky.SkyActions
import com.callbackdev.chiaro.ui.sky.SkyScreen
import com.callbackdev.chiaro.ui.theme.ChiaroTheme
import com.callbackdev.chiaro.ui.today.TodayPage
import java.io.File
import java.time.Clock
import java.time.Instant
import java.util.TimeZone
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

    private companion object {
        /** The widget tests' reference cells: four by one, by two, by four (dp). */
        val OneRow = DpSize(340.dp, 82.dp)
        val TwoRows = DpSize(340.dp, 189.dp)
        val ThreeRows = DpSize(340.dp, 293.dp)
        val FourRows = DpSize(340.dp, 397.dp)

        /** The board the widgets are laid on: the phone's width, and a dark plain ground
         * standing in for a wallpaper nobody chose. */
        const val BoardWidth = 384f
        const val BoardMargin = 22f
        const val BoardGap = 16f
        const val BoardGround = 0xFF14171C.toInt()

        /** A notification in the shade: its side margin, its padding and its corners
         * (dp), stock Android's. */
        const val NotificationMargin = 12f
        const val NotificationPadding = 4f
        const val NotificationCorner = 24f
    }

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

    /** The rest of the day: the sky's moments with their verdicts, and the reader's rules
     * that hold today among them (15 ott 2026). */
    @Test
    fun todayDay() {
        showTab(ShellTab.TODAY) { today() }
        scrollUnderHeader(R.string.section_rest_of_day)
        save("today-day")
    }

    /** The week with tomorrow open: its hours and its facts, one tap on its row. */
    @Test
    fun todayWeek() {
        showTab(ShellTab.TODAY) { today() }
        scrollUnderHeader(R.string.section_week)
        val tomorrow = MilanRecording.today(app).week[1].forecast.date
        compose.onNode(hasText(Formats.dayLabel(tomorrow, app.resources.configuration.locales[0])))
            .performClick()
        compose.waitForIdle()
        scrollUnderHeader(R.string.section_week)
        save("today-week")
    }

    /** On an open foldable (DESIGN §8.12c): the rail at the start, the sky across the page,
     * the hours and the day under the hero, the week and the details beside them. */
    @Test
    @Config(qualifiers = "+w841dp-h701dp")
    fun todayFoldable() {
        showTab(ShellTab.TODAY) { today() }
        save("today-foldable")
    }

    @Test
    fun todayDetails() {
        showTab(ShellTab.TODAY) { today() }
        scrollToEnd(R.string.section_details)
        save("today-details")
    }

    /**
     * Today at night, in the dark theme (20 ott 2026): the stars that are there and the moon
     * as it looks, over the hero's numbers. The recording's own evening ([MilanRecording.night]),
     * so the sky is its forecast for that hour and the moon and the stars the engine's for it.
     * The picture is the hero alone, cut just above the paper's first item: the forecast was
     * fetched at noon, and by 21:00 the page honestly says «Updated 8 hours ago», which is the
     * recording's age and not the night's. The hero's own estimate line stays: it is true.
     */
    @Test
    fun todayNight() {
        val at = MilanRecording.night
        showTab(ShellTab.TODAY, dark = true, at = at) { today(at) }
        val hours = java.time.Duration.between(MilanRecording.fetchedAt, at).toHours().toInt()
        val chip = app.resources.getQuantityString(R.plurals.freshness_hours_ago, hours, hours)
        val chipTop = compose.onNode(hasText(chip)).fetchSemanticsNode().boundsInRoot.top
        val cut = (chipTop - with(compose.density) { 2.dp.toPx() }).toInt()
        val whole = renderWindow()
        saveBitmap("today-night", Bitmap.createBitmap(whole, 0, 0, whole.width, cut))
    }

    @Composable
    private fun today(at: Instant = MilanRecording.now) {
        val state = MilanRecording.today(app, at)
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
        // From «Coming up»: with the guide's row at the foot of the page (22 ott 2026) the
        // list is long enough for the section title to reach the header.
        scrollUnderHeader(R.string.sky_section_events)
        save("sky-ahead")
    }

    @Composable
    private fun sky() = SkyScreen(
        state = MilanRecording.sky(),
        actions = SkyActions({}, {}, { _, _ -> }, {}),
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

    /** Further down: the reader's own alerts, and the ideas they start from. */
    @Test
    fun alertsYours() {
        showTab(ShellTab.ALERTS) {
            AlertsScreen(
                state = MilanRecording.alerts(app, MilanRecording.yourRules(app)),
                actions = AlertsActions({}, {}, {}, {}, {}, {}, {}, { _, _ -> }),
                onEdit = {},
                onOpenPlaces = {},
                onOpenSettings = {}
            )
        }
        scrollUnderHeader(R.string.alerts_group_yours)
        save("alerts-yours")
    }

    @Test
    fun settings() {
        show {
            SettingsScreen(
                settings = settings,
                actions = SettingsActions({}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}),
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

    // ---------------------------------------------------------------------- widgets

    /**
     * The three widgets as a reader places them, one under the other (13 ott 2026, when
     * five became three; the window took the arc's place on 20 ott 2026): «At a glance» and
     * «In words» at four cells by one, the window at four by two, by day, and «At a glance»
     * again at four by two, where it carries the next hours. The launcher is not in the
     * picture: the cards are drawn on a plain dark ground instead of a wallpaper.
     */
    @Test
    fun widgets() {
        val now = widget(WidgetKind.NOW)
        val words = widget(WidgetKind.TEXT)
        val window = widget(WidgetKind.WINDOW)
        val schemes = schemes()
        val board = listOf(
            WidgetOne(OneRow) { NowWidgetContent(now, schemes, rememberSkyBitmap(now)) },
            WidgetOne(OneRow) { TextWidgetContent(words, schemes, rememberSkyBitmap(words)) },
            WidgetOne(TwoRows) { WindowWidgetContent(window, schemes, rememberSkyBitmap(window)) },
            WidgetOne(TwoRows) { NowWidgetContent(now, schemes, rememberSkyBitmap(now)) }
        )
        saveBitmap("widgets", drawBoard(board))
    }

    /**
     * The window at night and tall (20 ott 2026): at four cells by two on the recording's
     * own evening ([MilanRecording.night]), the moon and the stars where they stand and the
     * names of the brightest; and at four by three by day, where the moments the reader
     * follows sit under the picture with their verdicts.
     */
    @Test
    fun widgetWindow() {
        val night = widget(WidgetKind.WINDOW, MilanRecording.night)
        val day = widget(WidgetKind.WINDOW)
        val schemes = schemes()
        val board = listOf(
            WidgetOne(TwoRows, MilanRecording.night) { WindowWidgetContent(night, schemes, rememberSkyBitmap(night)) },
            WidgetOne(ThreeRows) { WindowWidgetContent(day, schemes, rememberSkyBitmap(day)) }
        )
        saveBitmap("widget-window", drawBoard(board))
    }

    /**
     * The morning summary as the shade shows it, opened, on the day shade and on the
     * night one: the alert the engine finds in the recording at the report's own moment
     * (12:00, the last minute of the summary's window), posted by the app's own notifier,
     * then decorated by the platform's own template, as the system decorates a custom
     * body. The shade itself is not the app's: the cards stand on stock Android's own
     * notification ground, not on any maker's.
     */
    @Test
    fun notificationSummary() {
        val report = MilanRecording.report
        val zone = placeZone(report, MilanRecording.city)
        val moment = report.location.localTime
        val alert = checkNotNull(
            AlertEngine.evaluate(report, settings.notifications, AlertState(), moment, "readme")
                .find { it.kind == AlertKind.DAILY_SUMMARY }
        ) { "the recording's moment is outside the morning summary's window" }
        val manager = app.getSystemService(NotificationManager::class.java)
        check(AlertNotifier.notify(app, alert, report, settings.units))
        val posted = shadowOf(manager).allNotifications.last()

        val density = app.resources.displayMetrics.density
        fun px(dp: Float) = (dp * density).toInt()
        val width = px(BoardWidth - NotificationMargin * 2)
        // The header as a reader sees it: the app's name, not the debug build's, and the
        // time the summary was posted, «now», not the distance from the test's 1970.
        val postedAt = moment.atZone(zone).toInstant().toEpochMilli()
        SystemClock.setCurrentTimeMillis(postedAt)
        val label = app.applicationInfo.nonLocalizedLabel
        app.applicationInfo.nonLocalizedLabel = "Chiaro"
        // The builder is recovered on the context given, not on a fresh one for the app:
        // that one would carry the phone's configuration, not the night the card is for.
        posted.extras.remove("android.appInfo")
        val defaultZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone(zone))
        val cards = try {
            listOf(false, true).map { night ->
                val context = app.createConfigurationContext(
                    Configuration(app.resources.configuration).apply {
                        uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                            (if (night) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO)
                    }
                )
                val views = Notification.Builder.recoverBuilder(context, posted)
                    .setWhen(postedAt)
                    .setShowWhen(true)
                    .createBigContentView()
                val body = views.apply(context, FrameLayout(context))
                body.measure(
                    View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                )
                val height = body.measuredHeight + px(NotificationPadding * 2)
                body.layout(0, 0, width, body.measuredHeight)
                val card = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(card)
                canvas.drawRoundRect(
                    RectF(0f, 0f, width.toFloat(), height.toFloat()),
                    px(NotificationCorner).toFloat(), px(NotificationCorner).toFloat(),
                    Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = context.getColor(
                            if (night) android.R.color.system_neutral1_800 else android.R.color.system_neutral1_50
                        )
                    }
                )
                canvas.drawBitmap(
                    hardwareDraw(body, width, body.measuredHeight), 0f, px(NotificationPadding).toFloat(), null
                )
                card
            }
        } finally {
            TimeZone.setDefault(defaultZone)
            app.applicationInfo.nonLocalizedLabel = label
        }
        val margin = px(BoardMargin)
        val gap = px(BoardGap)
        val board = Bitmap.createBitmap(
            px(BoardWidth), margin * 2 + cards.sumOf { it.height } + gap, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(board)
        canvas.drawColor(BoardGround)
        var top = margin.toFloat()
        cards.forEach { card ->
            canvas.drawBitmap(card, ((board.width - card.width) / 2).toFloat(), top, null)
            top += card.height + gap
        }
        saveBitmap("notification-summary", board)
    }

    private class WidgetOne(val size: DpSize, val at: Instant = MilanRecording.now, val content: @Composable () -> Unit)

    private fun widget(kind: WidgetKind, at: Instant = MilanRecording.now) = MilanRecording.widget(app, kind, at)

    private fun schemes() = widgetSchemes(app, settings.dynamicColor, settings.palette)

    /**
     * Each widget composed by Glance into the `RemoteViews` a launcher would receive,
     * applied to a real view as the launcher applies it, and drawn ([hardwareDraw]); then
     * the cards laid one under the other on the ground. The text is the system's face,
     * as on a phone: a widget is drawn in the launcher's process, not the app's.
     */
    @OptIn(ExperimentalGlanceRemoteViewsApi::class)
    private fun drawBoard(widgets: List<WidgetOne>): Bitmap {
        val density = app.resources.displayMetrics.density
        fun px(dp: Float) = (dp * density).toInt()
        val drawn = widgets.map { one ->
            val composed = runBlocking {
                GlanceRemoteViews().compose(app, one.size) {
                    CompositionLocalProvider(LocalClock provides clock(one.at)) { one.content() }
                }
            }
            val view = composed.remoteViews.apply(app, FrameLayout(app))
            val width = px(one.size.width.value)
            val height = px(one.size.height.value)
            view.measure(
                View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
            )
            view.layout(0, 0, width, height)
            hardwareDraw(view, width, height)
        }
        val margin = px(BoardMargin)
        val gap = px(BoardGap)
        val board = Bitmap.createBitmap(
            px(BoardWidth),
            margin * 2 + drawn.sumOf { it.height } + gap * (drawn.size - 1),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(board)
        canvas.drawColor(BoardGround)
        var top = margin.toFloat()
        drawn.forEach { card ->
            canvas.drawBitmap(card, ((board.width - card.width) / 2).toFloat(), top, null)
            top += card.height + gap
        }
        return board
    }

    /**
     * [view] drawn through the GPU pipeline rather than onto a software canvas. Glance
     * rounds its cards and chips with the view's outline (`clipToOutline`, API 31+),
     * and only a render node honours an outline: drawn in software every corner came
     * out square. The view is attached to the test's activity first, because a child
     * is recorded as its own render node only inside a hardware-accelerated window.
     */
    private fun hardwareDraw(view: View, width: Int, height: Int): Bitmap {
        compose.runOnUiThread {
            val host = FrameLayout(compose.activity)
            compose.activity.setContentView(host, ViewGroup.LayoutParams(width, height))
            host.addView(view, FrameLayout.LayoutParams(width, height))
        }
        compose.waitForIdle()
        val node = RenderNode("readme").apply { setPosition(0, 0, width, height) }
        view.draw(node.beginRecording())
        node.endRecording()
        val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 1)
        val renderer = HardwareRenderer()
        try {
            renderer.setSurface(reader.surface)
            renderer.setContentRoot(node)
            renderer.createRenderRequest().setWaitForPresent(true).syncAndDraw()
            reader.acquireNextImage().use { image ->
                val plane = image.planes[0]
                // A row may be padded past the width: copy the padded rows, then crop.
                val padded = Bitmap.createBitmap(plane.rowStride / plane.pixelStride, height, Bitmap.Config.ARGB_8888)
                padded.copyPixelsFromBuffer(plane.buffer)
                return Bitmap.createBitmap(padded, 0, 0, width, height)
            }
        } finally {
            renderer.destroy()
            reader.close()
        }
    }

    // ---------------------------------------------------------------------- drawing

    /** The app's frame as `MainActivity` sets it up, with the fresh install's settings
     * and the clock stopped at the recording's moment. */
    private fun show(dark: Boolean = false, at: Instant = MilanRecording.now, content: @Composable () -> Unit) {
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
                    LocalClock provides clock(at)
                ) {
                    Surface(modifier = Modifier.fillMaxSize()) { content() }
                }
            }
        }
        compose.waitForIdle()
    }

    /** A tab page with the bottom bar over it, or the rail beside it on a wide window, laid
     * out as `ChiaroRoot` lays them out. */
    private fun showTab(
        tab: ShellTab,
        dark: Boolean = false,
        at: Instant = MilanRecording.now,
        page: @Composable () -> Unit
    ) = show(dark, at) {
        val width = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() }
        if (width >= RailMinWidth) {
            val inset = railInset()
            val underRail = tab == ShellTab.TODAY
            Box(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.fillMaxSize().padding(start = if (underRail) 0.dp else inset)) {
                    CompositionLocalProvider(LocalRailInset provides if (underRail) inset else 0.dp) {
                        page()
                    }
                }
                ChiaroNavRail(selected = tab, onSelect = {})
            }
            return@show
        }
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
    private fun clock(at: Instant = MilanRecording.now): Clock = Clock.fixed(at, placeZone(MilanRecording.report, MilanRecording.city))

    private fun save(name: String) {
        compose.waitForIdle()
        saveBitmap(name, renderWindow())
    }

    /**
     * The activity's window drawn through the GPU pipeline, as the widgets are (15 ott 2026):
     * the sky's shader is a `RuntimeShader`, which a software canvas refuses to draw, so a
     * `View.draw` into a bitmap showed the gradient alone. Recorded into a `RenderNode` and
     * rendered by a `HardwareRenderer` into an `ImageReader`, the same road `hardwareDraw`
     * takes, without re-hosting the view.
     */
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

    private fun saveBitmap(name: String, bitmap: Bitmap) {
        val dir = File(checkNotNull(output)).apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
