package com.callbackdev.chiaro.notifications

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.callbackdev.chiaro.domain.model.HourlyForecast
import com.callbackdev.chiaro.domain.model.WeatherCondition
import com.callbackdev.chiaro.domain.settings.TemperatureUnit
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The pictures of the expanded notifications (23 set 2026). What can be held without a
 * screenshot is WHEN a picture is drawn — never a chart of nothing, never one the platform
 * would clip — and that is what this pins; how they look is in the review's renders.
 */
@RunWith(RobolectricTestRunner::class)
class NotificationChartsTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val zone = ZoneId.of("Europe/Rome")
    private val start = LocalDate.of(2026, 9, 23).atTime(13, 0)

    private fun hours(count: Int, pct: (Int) -> Int?): List<HourlyForecast> = (0 until count).map { i ->
        val t = start.plusHours(i.toLong())
        HourlyForecast(
            time = t, at = t.atZone(zone).toInstant(), tempC = 15.0 + i % 6,
            condition = WeatherCondition(2, "", ""), precipChancePct = pct(i), cloudCoverPct = 50
        )
    }

    private val inks get() = NotificationCharts.inks(context)

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

    @Test
    fun `a day is drawn with its rain row only when it rains`() {
        val sunrise = start.withHour(7).withMinute(11)
        val sunset = start.withHour(19).withMinute(18)
        val wet = NotificationCharts.day(hours(11) { if (it == 4) 60 else 0 }, sunrise, sunset, TemperatureUnit.CELSIUS, inks, true, Locale.ITALY)
        val dry = NotificationCharts.day(hours(11) { 0 }, sunrise, sunset, TemperatureUnit.CELSIUS, inks, true, Locale.ITALY)
        assertEquals(NotificationCharts.DayChartHeight, wet!!.height)
        assertEquals(NotificationCharts.DayChartHeightDry, dry!!.height)
        assertNull(NotificationCharts.day(hours(2) { 0 }, sunrise, sunset, TemperatureUnit.CELSIUS, inks, true, Locale.ITALY))
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
