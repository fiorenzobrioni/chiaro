package com.callbackdev.chiaro.notifications

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import com.callbackdev.chiaro.R
import androidx.test.core.app.ApplicationProvider
import com.callbackdev.chiaro.domain.model.Coordinates
import com.callbackdev.chiaro.domain.model.HourlyForecast
import com.callbackdev.chiaro.domain.model.WeatherCondition
import com.callbackdev.chiaro.domain.settings.TemperatureUnit
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The pictures of the expanded notifications (23 set 2026). What can be held without a
 * screenshot is WHEN a picture is drawn — never a chart of nothing, never one the platform
 * would clip — and that is what this pins; how they look is in the review's renders.
 */
@RunWith(RobolectricTestRunner::class)
class NotificationChartsTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val zone = ZoneId.of("Europe/Rome")
    private val milan = Coordinates(45.4642, 9.19)
    private val start = LocalDate.of(2026, 9, 23).atTime(13, 0)
    private val date: LocalDate = start.toLocalDate()

    private fun hours(count: Int, pct: (Int) -> Int?): List<HourlyForecast> = (0 until count).map { i ->
        val t = start.plusHours(i.toLong())
        HourlyForecast(
            time = t, at = t.atZone(zone).toInstant(), tempC = 15.0 + i % 6,
            condition = WeatherCondition(2), precipChancePct = pct(i), cloudCoverPct = 50
        )
    }

    private val inks get() = NotificationCharts.inks(context, dark = false)

    @Test
    fun `the rain's hours need hours, and a chance in at least one of them`() {
        assertNull(NotificationCharts.rainHours(hours(3) { 80 }, start, null, false, inks, true, Locale.ITALY))
        // A model that forecast no chance at all: a row of empty bars is a chart of nothing.
        assertNull(NotificationCharts.rainHours(hours(12) { null }, start, null, false, inks, true, Locale.ITALY))
        val bmp = NotificationCharts.rainHours(hours(24) { if (it in 3..5) 80 else 5 }, start, null, true, inks, true, Locale.ITALY)
        assertNotNull(bmp)
        assertEquals(NotificationCharts.WIDTH, bmp!!.width)
    }

    @Test
    fun `hours already gone are not drawn`() {
        // From 18:30 the chart starts at 18:00: the five hours before it are the past.
        val from = start.plusHours(5).plusMinutes(30)
        val all = hours(20) { 40 }
        assertNotNull(NotificationCharts.rainHours(all, from, null, false, inks, true, Locale.ITALY))
        assertNull(NotificationCharts.rainHours(all, start.plusHours(17), null, false, inks, true, Locale.ITALY))
    }

    private fun day(hours: List<HourlyForecast>, inks: NotificationCharts.Inks = this.inks) =
        NotificationCharts.day(hours, date, zone, milan, TemperatureUnit.CELSIUS, inks, true, Locale.ITALY)

    @Test
    fun `a day is drawn with its rain row only when it rains`() {
        val wet = day(hours(11) { if (it == 4) 60 else 0 })
        val dry = day(hours(11) { 0 })
        assertEquals(NotificationCharts.DayChartHeight, wet!!.height)
        assertEquals(NotificationCharts.DayChartHeightDry, dry!!.height)
        assertNull(day(hours(2) { 0 }))
    }

    /** The day's light under the hours (21 ott 2026): the hero's ribbon, which is lighter
     * where the sun is up and darker where it is not — 13:00 against 23:00 in Milan in
     * September — on both grounds. A single tint could not say which end is the night. */
    @Test
    @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
    fun `the ribbon under the hours is the day's light, brighter by day than by night`() {
        fun luminance(argb: Int): Float {
            fun channel(shift: Int) = ((argb shr shift) and 0xFF) / 255f
            return 0.2126f * channel(16) + 0.7152f * channel(8) + 0.0722f * channel(0)
        }
        for (dark in listOf(false, true)) {
            val bmp = day(hours(11) { 0 }, NotificationCharts.inks(context, dark))!!
            val y = NotificationCharts.ribbonCenterY(bmp.height).toInt()
            val cell = bmp.width / 11
            val noon = bmp.getPixel(cell / 2, y)
            val night = bmp.getPixel(bmp.width - cell / 2, y)
            assertTrue("dark=$dark: the ribbon is not painted", (noon ushr 24) != 0 && (night ushr 24) != 0)
            assertTrue("dark=$dark: 13:00 ${luminance(noon)} against 23:00 ${luminance(night)}", luminance(noon) > luminance(night))
        }
    }

    /** The low's figure keeps off the rain's label (21 ott 2026, a low at 08 under a peak at
     * 09): to the right of its dot by default, to the left when the right would land on the
     * label or leave the picture, and inside the picture when neither side is clear. */
    @Test
    fun `the low's figure goes where the rain's label is not`() {
        val width = 60f
        assertTrue(NotificationCharts.lowLabelRight(100f, 100f, width, avoid = null))
        // The label sits just right of the dot: the figure goes left.
        val onTheRight = android.graphics.RectF(115f, 70f, 160f, 100f)
        assertFalse(NotificationCharts.lowLabelRight(100f, 100f, width, onTheRight))
        // Near the right edge the right side leaves the picture: left, label or not.
        assertFalse(NotificationCharts.lowLabelRight(NotificationCharts.WIDTH - 40f, 100f, width, avoid = null))
        // Near the left edge the left side leaves the picture: right, even on the label.
        assertTrue(NotificationCharts.lowLabelRight(20f, 100f, width, android.graphics.RectF(30f, 70f, 90f, 100f)))
    }

    /** Both grounds are painted, or neither: a notification never carries one picture
     * for a shade it may not be drawn on. */
    @Test
    fun `a picture is painted for each ground`() {
        val themed = NotificationCharts.themed(context) { inks -> day(hours(11) { 0 }, inks) }
        assertNotNull(themed)
        assertTrue(themed!!.light !== themed.dark)
        assertNull(NotificationCharts.themed(context) { null })
    }

    /** The shade picks the picture when it inflates the body (30 set 2026): by day the
     * day's, by night the night's, from the layout's own `-night` resources. */
    @Test
    @Config(qualifiers = "notnight")
    fun `the day shade shows the day picture`() {
        val body = LayoutInflater.from(context).inflate(R.layout.notification_expanded, null)
        assertEquals(View.VISIBLE, body.findViewById<View>(R.id.notif_chart_day).visibility)
        assertEquals(View.GONE, body.findViewById<View>(R.id.notif_chart_night).visibility)
    }

    @Test
    @Config(qualifiers = "night")
    fun `the night shade shows the night picture`() {
        val body = LayoutInflater.from(context).inflate(R.layout.notification_expanded, null)
        assertEquals(View.GONE, body.findViewById<View>(R.id.notif_chart_day).visibility)
        assertEquals(View.VISIBLE, body.findViewById<View>(R.id.notif_chart_night).visibility)
    }

    /** The platform clips an expanded custom view at 256 dp. At the narrowest width a
     * notification is laid out at (~300 dp), the tallest picture plus the body around it
     * — title, headline, five lines of details, margins — must still fit. */
    @Test
    fun `the tallest picture leaves room for the body under the platform's ceiling`() {
        val widthDp = 300f
        val tallest = maxOf(NotificationCharts.RainChartHeight, NotificationCharts.DayChartHeight)
        val pictureDp = tallest * widthDp / NotificationCharts.WIDTH
        val bodyDp = 24f + 20f * 2 + 8f + 6f + 5 * 19f
        assertTrue("${pictureDp + bodyDp} dp", pictureDp + bodyDp <= 256f)
    }
}

/** The quiet hours (23 set 2026): 22:00 up to, not including, 07:00, on the phone's clock. */
class QuietHoursTest {
    @Test
    fun `the night is quiet, the day is not`() {
        fun quiet(h: Int, m: Int = 0) = NotificationViews.isQuiet(java.time.LocalTime.of(h, m))
        assertTrue(quiet(22))
        assertTrue(quiet(3))
        assertTrue(quiet(6, 59))
        assertTrue(!quiet(7))
        assertTrue(!quiet(21, 59))
        assertTrue(!quiet(12))
    }
}
