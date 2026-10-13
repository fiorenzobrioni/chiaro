package com.callbackdev.chiaro.ui.today

import com.callbackdev.chiaro.domain.model.HourlyForecast
import com.callbackdev.chiaro.domain.model.WeatherCondition
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The review of 12 ott 2026, point 5: where an open day's strip starts. (Point 6's
 * moon-skipping countdown went with the moon itself, 13 ott 2026: Today's agenda has none.)
 */
class RestOfDayTest {

    private val zone = ZoneId.of("Europe/Rome")

    private fun hours(nightUntil: Int, from: Int = 0) = (from..23).map { h ->
        val time = LocalDateTime.of(2026, 9, 26, h, 0)
        StripHour(
            HourlyForecast(
                time = time,
                at = time.atZone(zone).toInstant(),
                tempC = 18.0,
                condition = WeatherCondition(0),
                precipChancePct = 0,
                cloudCoverPct = 0
            ),
            night = h < nightUntil || h >= 20
        )
    }

    @Test
    fun `an open day starts the hour before its first daylight`() {
        // Daylight from 07:00: the strip opens on 06:00, the sunrise in its second cell.
        assertEquals(6, openingHour(hours(nightUntil = 7)))
        // Today at 12:00, already in daylight: it opens where it starts.
        assertEquals(0, openingHour(hours(nightUntil = 7, from = 12)))
        // Today at 21:00, all night: the same.
        assertEquals(0, openingHour(hours(nightUntil = 7, from = 21)))
    }
}
