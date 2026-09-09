package com.callbackdev.chiaro.widget.arc

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.callbackdev.chiaro.domain.model.City
import com.callbackdev.chiaro.domain.model.Coordinates
import com.callbackdev.chiaro.domain.model.DailyForecast
import com.callbackdev.chiaro.domain.model.HourlyForecast
import com.callbackdev.chiaro.domain.model.WeatherCondition
import com.callbackdev.chiaro.domain.sample.sampleWeatherReport
import com.callbackdev.chiaro.domain.settings.TemperatureUnit
import com.callbackdev.chiaro.ui.theme.ArcPalette
import com.callbackdev.chiaro.ui.theme.SkyPalette
import com.callbackdev.chiaro.ui.theme.VividDarkColors
import com.callbackdev.chiaro.ui.today.TodayStateBuilder
import com.callbackdev.chiaro.ui.today.TodayUiState
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * The painter, run for real (Robolectric's native graphics): the bitmap has the box it
 * was asked for, the ground is painted where a ground was asked, the sun's disc is on the
 * plot where the sun is up, and nothing is drawn when every layer is off. Set
 * `CHIARO_RENDER_DIR` to a folder to also get the frames as PNGs for a look.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ArcPainterTest {

    private val zone = ZoneId.of("Europe/Rome")
    private val milan = City(
        id = 1L, name = "Milano", region = "Lombardia", country = "Italia",
        coordinates = Coordinates(45.4643, 9.1895), timezone = "Europe/Rome"
    )
    private val date: LocalDate = LocalDate.of(2026, 9, 2)
    private val clear = WeatherCondition(0, "Clear", "☀️")

    private fun report(fetchedAt: LocalDateTime) =
        sampleWeatherReport().copy(
            location = sampleWeatherReport().location.copy(
                city = "Milano",
                coordinates = milan.coordinates,
                timezone = "Europe/Rome",
                localTime = fetchedAt
            ),
            hourly = (0 until 48).map {
                HourlyForecast(
                    time = fetchedAt.withMinute(0).plusHours(it.toLong()),
                    tempC = 14.0 + 8.0 * kotlin.math.sin((it + 12) / 24.0 * Math.PI),
                    condition = clear,
                    precipChancePct = if (it in 4..7) 20 * (it - 3) else 5,
                    cloudCoverPct = if (it in 4..7) 80 else 20
                )
            },
            daily = (0 until 7).map {
                DailyForecast(date.plusDays(it.toLong()), 24.0, 14.0, clear, 10, 5, "Moderate")
            },
            systemInfo = sampleWeatherReport().systemInfo.copy(
                lastSync = fetchedAt.atZone(zone).toInstant()
            )
        )

    private fun at(hour: Int, minute: Int = 0): Instant =
        date.atTime(LocalTime.of(hour, minute)).atZone(zone).toInstant()

    private fun series(now: Instant, settings: ArcSettings): ArcSeries {
        val content = TodayStateBuilder.build(
            milan, report(LocalDateTime.of(2026, 9, 2, 12, 0)), now, 60, false, null
        ) as TodayUiState.Content
        return ArcSeries.build(content, emptyList(), milan.coordinates, now, settings)
    }

    private fun spec(width: Int, height: Int, labels: Boolean, temps: Boolean, step: Int = 3) = ArcCanvasSpec(
        widthPx = width, heightPx = height, density = 2.75f, fontScale = 1f, textScale = 1f,
        hourLabels = labels, temperatures = temps, tickStepHours = step,
        is24Hour = true, locale = Locale.ITALY, temperatureUnit = TemperatureUnit.CELSIUS, zone = zone
    )

    private val inks = ArcInks(
        primary = Color.WHITE,
        secondary = Color.argb(191, 255, 255, 255),
        darkGround = true,
        sky = SkyPalette.Vivid,
        colors = VividDarkColors
    )

    @Test
    fun `the bitmap has the box it was asked for and a painted ground`() {
        val settings = ArcSettings()
        val bitmap = ArcPainter.paint(series(at(15), settings), settings, spec(858, 162, labels = true, temps = false), inks)
        assertEquals(858, bitmap.width)
        assertEquals(162, bitmap.height)
        // Mid-plot, mid-afternoon: the band is opaque sky under the scrim.
        assertTrue(Color.alpha(bitmap.getPixel(600, 40)) > 200)
        // Under the label band nothing is painted but the labels themselves.
        assertEquals(0, Color.alpha(bitmap.getPixel(4, 158)))
        save(bitmap, "panel-15h")
    }

    @Test
    fun `the sun's disc is on the plot where the sun stands`() {
        val settings = ArcSettings(ground = ArcGround.NONE, rain = false, moon = false, nowMarker = false, hourLabels = false)
        val s = series(at(15), settings)
        val bitmap = ArcPainter.paint(s, settings, spec(400, 100, labels = false, temps = false), inks)
        val sun = ArcPalette.Sun.toArgb()
        val x = (s.nowFraction * 400).toInt()
        val column = (0 until 100).map { bitmap.getPixel(x, it) }
        assertTrue("an amber pixel in the present's column", column.any { near(it, sun) })
        save(bitmap, "sun-only")
    }

    @Test
    fun `with every layer off the plot is empty`() {
        val settings = ArcSettings(
            ground = ArcGround.NONE, sunPath = false, moon = false, rain = false,
            nowMarker = false, hourLabels = false, temperatures = false
        )
        val bitmap = ArcPainter.paint(series(at(15), settings), settings, spec(300, 80, labels = false, temps = false), inks)
        var painted = 0
        for (x in 0 until 300 step 3) for (y in 0 until 80 step 3) if (Color.alpha(bitmap.getPixel(x, y)) != 0) painted++
        assertEquals(0, painted)
    }

    @Test
    fun `the forms, for a look`() {
        val settings = ArcSettings()
        save(ArcPainter.paint(series(at(15), settings), settings, spec(179, 80, labels = false, temps = false), inks), "dial")
        save(ArcPainter.paint(series(at(15), settings), settings, spec(382, 87, labels = false, temps = false), inks), "strip-stacked")
        save(ArcPainter.paint(series(at(15), settings), settings, spec(583, 170, labels = true, temps = false, step = 4), inks), "strip-row")
        save(ArcPainter.paint(series(at(15), settings), settings, spec(858, 365, labels = true, temps = true), inks), "board")
        save(ArcPainter.paint(series(at(22, 30), settings), settings, spec(858, 365, labels = true, temps = true), inks), "board-night")
        val ahead = ArcSettings(span = ArcSpan.AHEAD)
        save(ArcPainter.paint(series(at(15), ahead), ahead, spec(858, 365, labels = true, temps = true), inks), "board-ahead")
        val ribbon = ArcSettings(ground = ArcGround.RIBBON)
        save(ArcPainter.paint(series(at(9), ribbon), ribbon, spec(858, 365, labels = true, temps = true), inks), "board-ribbon")
        val light = inks.copy(primary = Color.rgb(25, 28, 32), secondary = Color.rgb(68, 72, 80), darkGround = false)
        save(ArcPainter.paint(series(at(15), ArcSettings(ground = ArcGround.NONE)), ArcSettings(ground = ArcGround.NONE), spec(858, 365, labels = true, temps = true), light), "board-light-none")
    }

    @Test
    fun `the step widens until the labels stop colliding`() {
        assertEquals(3, ArcPainter.widenStep(2))
        assertEquals(4, ArcPainter.widenStep(3))
        assertEquals(6, ArcPainter.widenStep(4))
        assertEquals(12, ArcPainter.widenStep(8))
        assertEquals(24, ArcPainter.widenStep(12))
    }

    private fun near(a: Int, b: Int): Boolean =
        kotlin.math.abs(Color.red(a) - Color.red(b)) < 40 &&
            kotlin.math.abs(Color.green(a) - Color.green(b)) < 40 &&
            kotlin.math.abs(Color.blue(a) - Color.blue(b)) < 40 &&
            Color.alpha(a) > 200

    private fun save(bitmap: Bitmap, name: String) {
        val dir = System.getenv("CHIARO_RENDER_DIR")?.takeIf { it.isNotBlank() } ?: return
        File(dir).mkdirs()
        FileOutputStream(File(dir, "arc-$name.png")).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
