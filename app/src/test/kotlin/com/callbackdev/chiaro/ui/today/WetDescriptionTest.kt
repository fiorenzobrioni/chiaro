package com.callbackdev.chiaro.ui.today

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.callbackdev.chiaro.domain.WmoCode
import com.callbackdev.chiaro.domain.model.HourlyForecast
import com.callbackdev.chiaro.domain.model.WeatherCondition
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The words TalkBack says for a week row's wet marks (31 ott 2026): a run ends where its last
 * hour ends, not where it starts.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "it")
class WetDescriptionTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val timeFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val zone = ZoneId.of("Europe/Rome")
    private val date = LocalDate.of(2026, 11, 3)

    private fun day(codes: Map<Int, Int>) = (0..23).map { h ->
        val time = date.atTime(h, 0)
        HourlyForecast(
            time = time,
            at = time.atZone(zone).toInstant(),
            tempC = 8.0,
            condition = WeatherCondition(codes[h] ?: WmoCode.OVERCAST.code),
            precipChancePct = 60,
            cloudCoverPct = 95
        )
    }

    private val rain = WmoCode.RAIN_MODERATE.code

    @Test
    fun `a run ends at the end of its last hour, one hour is the hour it falls in`() {
        assertEquals(
            "Pioggia dalle 15:00 alle 18:00, alle 20:00",
            wetDescription(context, day(mapOf(15 to rain, 16 to rain, 17 to rain, 20 to rain)), timeFmt)
        )
    }

    @Test
    fun `snow and rain in the same day are both named`() {
        assertEquals(
            "Pioggia e neve dalle 07:00 alle 09:00",
            wetDescription(context, day(mapOf(7 to WmoCode.SNOW_SLIGHT.code, 8 to rain)), timeFmt)
        )
    }

    @Test
    fun `a dry day says nothing`() {
        assertNull(wetDescription(context, day(emptyMap()), timeFmt))
    }
}
