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

    @Test
    @Config(fontScale = 1.15f)
    fun medium() = board("sky-115", WidgetBackground.SKY)

    private fun model(background: WidgetBackground, at: Instant): WidgetModel {
        val base = MilanRecording.widget(app, WidgetKind.PICTURES, at)
        return base.copy(look = base.look.copy(background = background, cardColor = WidgetCardColor.BLUE, opacityPct = 100))
    }

    private fun board(name: String, background: WidgetBackground, at: Instant = MilanRecording.now, look: PicturesLook = PicturesLook()) {
        val model = model(background, at)
        val schemes = widgetSchemes(app, model.settings.dynamicColor, model.settings.palette)
        val clock = Clock.fixed(at, placeZone(MilanRecording.report, MilanRecording.city))
        val rows = listOf(
            listOf(DpSize(159.dp, 85.dp), DpSize(340.dp, 85.dp), DpSize(159.dp, 189.dp), DpSize(250.dp, 189.dp), DpSize(340.dp, 189.dp)),
            listOf(DpSize(159.dp, 293.dp), DpSize(340.dp, 293.dp), DpSize(340.dp, 397.dp))
        )
        val density = app.resources.displayMetrics.density
        fun px(dp: Float) = (dp * density).toInt()
        val drawn = rows.map { row -> row.map { size -> draw(size, clock) { InPicturesContent(model, schemes, rememberSkyBitmap(model), look) } } }
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
