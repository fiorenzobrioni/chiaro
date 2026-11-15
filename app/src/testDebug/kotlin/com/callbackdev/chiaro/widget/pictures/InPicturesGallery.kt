package com.callbackdev.chiaro.widget.pictures

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.HardwareRenderer
import android.graphics.PixelFormat
import android.graphics.RenderNode
import android.media.ImageReader
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import androidx.test.core.app.ApplicationProvider
import com.callbackdev.chiaro.domain.placeZone
import com.callbackdev.chiaro.readme.MilanRecording
import com.callbackdev.chiaro.ui.format.LocalClock
import com.callbackdev.chiaro.ui.theme.WidgetCardColor
import com.callbackdev.chiaro.widget.WidgetBackground
import com.callbackdev.chiaro.widget.WidgetKind
import com.callbackdev.chiaro.widget.WidgetModel
import com.callbackdev.chiaro.widget.rememberSkyBitmap
import com.callbackdev.chiaro.widget.widgetSchemes
import java.io.File
import java.time.Clock
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * «In immagini»'s gallery (Fase 17.2): the eight forms at their reference grants, laid out as
 * the canvas's «Le otto forme» board lays them, on the grounds of its «Gli sfondi» board, drawn
 * the way the README's widgets are (Glance into `RemoteViews`, applied to a view, through the GPU
 * pipeline). Only on request, into the directory the property names:
 * `./gradlew :app:testDebugUnitTest --tests "*InPicturesGallery" -PpicturesGallery=<dir>`.
 * It is the picture 17.4 compares with the canvas, form by form.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "it-rIT-w1760dp-h1200dp-xhdpi")
class InPicturesGallery {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val output = System.getProperty("chiaro.picturesGallery")
    private val app: Application = ApplicationProvider.getApplicationContext()

    @Before
    fun onlyOnRequest() = assumeTrue("Run with -PpicturesGallery=<dir>", output != null)

    @Test
    fun sky() = board("sky", WidgetBackground.SKY)

    @Test
    fun light() = board("light", WidgetBackground.LIGHT)

    @Test
    fun dark() = board("dark", WidgetBackground.DARK)

    @Test
    fun card() = board("card-blue", WidgetBackground.COLOR)

    @Test
    fun night() = board("sky-night", WidgetBackground.SKY, MilanRecording.night)

    @Test
    fun day() = board("sky-day", WidgetBackground.SKY, look = PicturesLook(span = com.callbackdev.chiaro.domain.pictures.PictureSpan.DAY))

    @Test
    @Config(fontScale = 1.3f)
    fun large() = board("sky-130", WidgetBackground.SKY)

    /** The eight forms on the canvas's own example (17.4): the board compared with the
     * canvas's «Le otto forme», form by form, at the same scale. */
    @Test
    fun canvas() = board("canvas", WidgetBackground.SKY, model = canvasModel(WidgetBackground.SKY))

    /** The 4×2 on the grounds of the canvas's «Gli sfondi» board, on the canvas's example. */
    @Test
    fun grounds() {
        val grounds = listOf(WidgetBackground.SKY to WidgetCardColor.BLUE, WidgetBackground.LIGHT to WidgetCardColor.BLUE, WidgetBackground.DARK to WidgetCardColor.BLUE) +
            WidgetCardColor.entries.map { WidgetBackground.COLOR to it }
        val size = DpSize(340.dp, 189.dp)
        val drawn = grounds.mapIndexed { i, (ground, color) ->
            val model = canvasModel(ground, color)
            val schemes = widgetSchemes(app, model.settings.dynamicColor, model.settings.palette)
            draw(size, clock(MilanRecording.now)) { InPicturesContent(model, schemes, rememberSkyBitmap(model)) }
                .also { save("grounds-card-$i", it) }
        }
        val density = app.resources.displayMetrics.density
        val gap = (32 * density).toInt()
        val columns = 3
        val w = drawn.first().width
        val h = drawn.first().height
        val board = Bitmap.createBitmap(gap + columns * (w + gap), gap + ((drawn.size + columns - 1) / columns) * (h + gap), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(board)
        canvas.drawColor(0xFFE9E3F2.toInt())
        drawn.forEachIndexed { i, card ->
            canvas.drawBitmap(card, (gap + (i % columns) * (w + gap)).toFloat(), (gap + (i / columns) * (h + gap)).toFloat(), null)
        }
        save("grounds", board)
    }

    /**
     * The canvas's example (`docs/design/in-immagini/Forms.dc.html`, its `T`, `P`, `A` and its
     * week) laid over the recording's report, for the comparison only: the 25 hours from the
     * hour the recording was made carry the canvas's temperatures, chances and millimetres (an
     * hour with ≥ 0.1 mm at ≥ 20% is the engine's drizzle or light rain, as rule 3 names them),
     * the hour's number is the canvas's 11°, its sky the canvas's partly cloudy, and the five
     * days are the canvas's. The sentence, the sky dress and everything else is the app's own
     * reading of those numbers. Never the README's: its pictures stay the recording's.
     */
    private fun canvasModel(background: WidgetBackground, color: WidgetCardColor = WidgetCardColor.BLUE): WidgetModel {
        val base = model(background, MilanRecording.now).let { it.copy(look = it.look.copy(cardColor = color)) }
        val report = MilanRecording.report
        val t = doubleArrayOf(11.2, 11.8, 11.3, 10.2, 9.4, 8.9, 8.5, 8.1, 7.8, 7.5, 7.2, 6.9, 6.6, 6.3, 6.1, 6.0, 5.9, 6.0, 6.6, 7.9, 9.6, 10.9, 11.7, 12.1, 12.0)
        val p = intArrayOf(5, 5, 5, 10, 10, 15, 30, 60, 75, 80, 70, 60, 45, 30, 20, 10, 5, 5, 5, 0, 0, 0, 0, 0, 0)
        val a = doubleArrayOf(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.1, 0.6, 1.4, 1.8, 1.2, 0.7, 0.3, 0.1, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        val hourly = report.hourly.mapIndexed { i, h ->
            if (i >= t.size) return@mapIndexed h
            val wet = a[i] >= 0.1 && p[i] >= 20
            h.copy(
                tempC = t[i], precipChancePct = p[i], precipMm = a[i], snowCm = 0.0,
                condition = if (wet) {
                    com.callbackdev.chiaro.domain.model.WeatherCondition(if (a[i] < 0.5) 51 else 61)
                } else {
                    h.condition
                }
            )
        }
        val week = listOf(Triple(6.0, 12.0, 80 to 61), Triple(4.0, 11.0, 70 to 61), Triple(3.0, 10.0, 5 to 0), Triple(2.0, 9.0, 5 to 2), Triple(4.0, 12.0, 5 to 3))
        val daily = report.daily.mapIndexed { i, d ->
            week.getOrNull(i)?.let { (lo, hi, rain) ->
                d.copy(lowC = lo, highC = hi, precipPct = rain.first, condition = com.callbackdev.chiaro.domain.model.WeatherCondition(rain.second))
            } ?: d
        }
        val example = report.copy(
            current = report.current.copy(tempC = 11.2, condition = com.callbackdev.chiaro.domain.model.WeatherCondition(2)),
            hourly = hourly,
            daily = daily
        )
        val content = com.callbackdev.chiaro.ui.today.TodayStateBuilder.build(
            city = MilanRecording.city, report = example, now = MilanRecording.now,
            updateFrequencyMin = MilanRecording.settings.updateFrequencyMin,
            userRefreshing = false, error = null
        ) as com.callbackdev.chiaro.ui.today.TodayUiState.Content
        return base.copy(content = content, dayHours = example.earlierToday + example.hourly, warning = null)
    }

    private fun clock(at: Instant): Clock = Clock.fixed(at, placeZone(MilanRecording.report, MilanRecording.city))

    private fun save(name: String, bitmap: Bitmap) {
        val dir = File(checkNotNull(output)).apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    @Config(fontScale = 1.15f)
    fun medium() = board("sky-115", WidgetBackground.SKY)

    private fun model(background: WidgetBackground, at: Instant): WidgetModel {
        val base = MilanRecording.widget(app, WidgetKind.PICTURES, at)
        return base.copy(look = base.look.copy(background = background, cardColor = WidgetCardColor.BLUE, opacityPct = 100))
    }

    private fun board(
        name: String,
        background: WidgetBackground,
        at: Instant = MilanRecording.now,
        look: PicturesLook = PicturesLook(),
        model: WidgetModel = model(background, at)
    ) {
        val schemes = widgetSchemes(app, model.settings.dynamicColor, model.settings.palette)
        val clock = Clock.fixed(at, placeZone(MilanRecording.report, MilanRecording.city))
        val rows = listOf(
            listOf(DpSize(159.dp, 85.dp), DpSize(340.dp, 85.dp), DpSize(159.dp, 189.dp), DpSize(250.dp, 189.dp), DpSize(340.dp, 189.dp)),
            listOf(DpSize(159.dp, 293.dp), DpSize(340.dp, 293.dp), DpSize(340.dp, 397.dp))
        )
        val density = app.resources.displayMetrics.density
        fun px(dp: Float) = (dp * density).toInt()
        var index = 0
        val drawn = rows.map { row ->
            row.map { size ->
                draw(size, clock) { InPicturesContent(model, schemes, rememberSkyBitmap(model), look) }
                    .also { save("$name-card-${index++}", it) }
            }
        }
        val gap = px(48f)
        val margin = px(40f)
        val width = margin * 2 + drawn.maxOf { r -> r.sumOf { it.width } + gap * (r.size - 1) }
        val height = margin * 2 + drawn.sumOf { r -> r.maxOf { it.height } } + gap
        val board = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(board)
        canvas.drawColor(0xFFE9E3F2.toInt())
        var top = margin
        drawn.forEach { row ->
            var left = margin
            val rowHeight = row.maxOf { it.height }
            row.forEach { card ->
                canvas.drawBitmap(card, left.toFloat(), (top + rowHeight - card.height).toFloat(), null)
                left += card.width + gap
            }
            top += rowHeight + gap
        }
        val dir = File(checkNotNull(output)).apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { board.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @OptIn(ExperimentalGlanceRemoteViewsApi::class)
    private fun draw(size: DpSize, clock: Clock, content: @androidx.compose.runtime.Composable () -> Unit): Bitmap {
        val density = app.resources.displayMetrics.density
        val composed = runBlocking {
            GlanceRemoteViews().compose(app, size) { CompositionLocalProvider(LocalClock provides clock) { content() } }
        }
        val view = composed.remoteViews.apply(app, FrameLayout(app))
        val width = (size.width.value * density).toInt()
        val height = (size.height.value * density).toInt()
        view.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
        )
        view.layout(0, 0, width, height)
        compose.runOnUiThread {
            val host = FrameLayout(compose.activity)
            compose.activity.setContentView(host, ViewGroup.LayoutParams(width, height))
            host.addView(view, FrameLayout.LayoutParams(width, height))
        }
        compose.waitForIdle()
        val node = RenderNode("gallery").apply { setPosition(0, 0, width, height) }
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
