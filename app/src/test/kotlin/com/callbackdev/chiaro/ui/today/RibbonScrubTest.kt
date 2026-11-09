package com.callbackdev.chiaro.ui.today

import com.callbackdev.chiaro.domain.model.City
import com.callbackdev.chiaro.domain.model.CloudLayers
import com.callbackdev.chiaro.domain.model.Coordinates
import com.callbackdev.chiaro.domain.model.DailyForecast
import com.callbackdev.chiaro.domain.model.HourlyForecast
import com.callbackdev.chiaro.domain.model.WeatherCondition
import com.callbackdev.chiaro.domain.sample.sampleWeatherReport
import com.callbackdev.chiaro.domain.sky.AstronomyEngine
import com.callbackdev.chiaro.ui.components.FogThin
import com.callbackdev.chiaro.ui.components.SkyFall
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * The ribbon dragged through the hours ahead (DESIGN §4, 9 nov 2026): which hour the finger
 * rests on, and the sky the canvas draws for it, from the forecast's own hour.
 */
class RibbonScrubTest {

    private val zone = ZoneId.of("Europe/Rome")
    private val milan = City(
        id = 1L, name = "Milano", region = "Lombardia", country = "Italia",
        coordinates = Coordinates(45.4643, 9.1895), timezone = "Europe/Rome"
    )
    private val date: LocalDate = LocalDate.of(2026, 9, 2)

    /** A day whose hours differ, so an hour read for another is seen: 14:00 is in fog, 16:00 a
     * thunderstorm, 18:00 snow, and every hour has its own cover. */
    private fun content(): TodayUiState.Content {
        val fetched = LocalDateTime.of(2026, 9, 2, 12, 0)
        val now = LocalDateTime.of(2026, 9, 2, 12, 30).atZone(zone).toInstant()
        val report = sampleWeatherReport().copy(
            location = sampleWeatherReport().location.copy(
                city = "Milano", coordinates = milan.coordinates, timezone = "Europe/Rome", localTime = fetched
            ),
            hourly = (0 until 48).map {
                val time = fetched.plusHours(it.toLong())
                HourlyForecast(
                    time = time,
                    at = time.atZone(zone).toInstant(),
                    tempC = 20.0 + it,
                    condition = WeatherCondition(
                        when (time.hour) { 14 -> 45; 16 -> 95; 18 -> 73; else -> 0 }
                    ),
                    precipChancePct = if (time.hour == 16) 90 else 10,
                    cloudCoverPct = (time.hour * 4).coerceAtMost(100),
                    windKph = 7.0 + it,
                    windDirectionDeg = 200,
                    visibilityKm = if (time.hour == 14) 0.2 else 20.0,
                    cloudLayers = CloudLayers(time.hour, 0, 0),
                    precipMm = if (time.hour == 16) 5.0 else 0.0
                )
            },
            daily = (0 until 7).map { DailyForecast(date.plusDays(it.toLong()), 24.0, 14.0, WeatherCondition(0), 10, 5) },
            systemInfo = sampleWeatherReport().systemInfo.copy(lastSync = fetched.atZone(zone).toInstant())
        )
        return TodayStateBuilder.build(milan, report, now, 60, false, null) as TodayUiState.Content
    }

    @Test
    fun `the hours ahead are today's full hours after now, in order`() {
        val ahead = TodayStateBuilder.hoursAhead(content())
        assertEquals((13..23).toList(), ahead.map { it.time.hour })
        assertTrue(ahead.all { it.time.toLocalDate() == date })
    }

    @Test
    fun `the finger rests on the nearest hour, and on now where now is nearer`() {
        val c = content()
        val ahead = TodayStateBuilder.hoursAhead(c)
        val now = c.sky.nowFraction
        fun at(hour: Int, minute: Int = 0) = (hour * 60 + minute) / 1440f
        assertEquals(18, scrubTarget(ahead, now, at(18))!!.time.hour)
        assertEquals(18, scrubTarget(ahead, now, at(18, 20))!!.time.hour)
        assertEquals(19, scrubTarget(ahead, now, at(18, 40))!!.time.hour)
        assertEquals(23, scrubTarget(ahead, now, 1f)!!.time.hour)
        // Now is at 12:30: at 12:40 the finger is on now, at 12:50 on 13:00.
        assertNull(scrubTarget(ahead, now, at(12, 40)))
        assertEquals(13, scrubTarget(ahead, now, at(12, 50))!!.time.hour)
        // The part of the day already spent is now, wherever in it the finger is.
        assertNull(scrubTarget(ahead, now, at(3)))
        assertNull(scrubTarget(ahead, now, 0f))
        // A day with no hour left has nowhere to go.
        assertNull(scrubTarget(emptyList(), now, at(18)))
    }

    @Test
    fun `an hour's sky is the forecast's own hour, with the sun where it will be`() {
        val c = content()
        val hour = TodayStateBuilder.hoursAhead(c).first { it.time.hour == 19 }
        val sky = TodayStateBuilder.skyOf(c, hour)
        assertEquals(76, sky.cloudPct)
        assertEquals(CloudLayers(19, 0, 0), sky.layers)
        assertEquals(10, sky.precipPct)
        assertEquals(hour.windKph!!, sky.windKph, 1e-9)
        assertEquals(200, sky.windFromDeg)
        assertEquals(AstronomyEngine.sunAltitude(hour.at, milan.coordinates), sky.sunAltitudeDeg, 1e-9)
        assertTrue("at 19:00 in September the sun is low", sky.sunAltitudeDeg < 10.0)
        assertTrue("and lower than it is now", sky.sunAltitudeDeg < c.sky.sunAltitudeDeg)
        assertEquals(19 / 24f, sky.nowFraction, 1e-6f)
        // The day's own light and ribbon are the day's, whichever hour is asked.
        assertEquals(c.sky.phases, sky.phases)
        assertEquals(c.sky.hours, sky.hours)
    }

    @Test
    fun `fog, thunder and what falls are the hour's state`() {
        val c = content()
        val ahead = TodayStateBuilder.hoursAhead(c).associateBy { it.time.hour }
        val fog = TodayStateBuilder.skyOf(c, ahead.getValue(14))
        assertEquals(1f, fog.fog, 1e-6f)
        assertEquals(false, fog.thunder)
        assertEquals(SkyFall.NONE, fog.fall)
        val storm = TodayStateBuilder.skyOf(c, ahead.getValue(16))
        assertEquals(true, storm.thunder)
        assertEquals(SkyFall.RAIN, storm.fall)
        assertEquals(5.0, storm.fallMmPerHour, 1e-9)
        assertEquals(0f, storm.fog, 0f)
        assertEquals(SkyFall.SNOW, TodayStateBuilder.skyOf(c, ahead.getValue(18)).fall)
        // Now is a clear hour: no fog, no storm.
        assertEquals(0f, c.sky.fog, 0f)
        assertEquals(false, c.sky.thunder)
    }

    @Test
    fun `fog is drawn only for a state of fog, thunder only for a storm that is one`() {
        assertEquals(0f, TodayStateBuilder.fogOf(0, 0.1), 0f)
        assertEquals(0f, TodayStateBuilder.fogOf(3, 0.5), 0f)
        assertEquals(1f, TodayStateBuilder.fogOf(45, 0.1), 1e-6f)
        assertEquals(FogThin, TodayStateBuilder.fogOf(48, 1.0), 1e-6f)
        assertTrue(TodayStateBuilder.fogOf(45, null) > FogThin)
        listOf(95, 96, 99).forEach { assertTrue("$it", TodayStateBuilder.thunderOf(it)) }
        // A storm that is only possible is a dry hour under a sky that might: nothing flashes.
        listOf(0, 3, 63, 82, 1095).forEach { assertTrue("$it", !TodayStateBuilder.thunderOf(it)) }
    }

    /** The moon's north is the pole's direction at the moon: straight up when the moon is due
     * south for a northern place, tilted to the left as it rises in the east. */
    @Test
    fun `the moon's face is upright on the meridian and lies over as it rises`() {
        val south = AstronomyEngine.brightLimbAngle(45.46, 0.0, 40.0, 180.0)
        assertEquals(90.0, south, 1e-6)
        val east = AstronomyEngine.brightLimbAngle(45.46, 0.0, 15.0, 100.0)
        assertTrue("rising in the east the north is up and to the left: $east", east > 110.0 && east < 180.0)
        val west = AstronomyEngine.brightLimbAngle(45.46, 0.0, 15.0, 260.0)
        assertTrue("setting in the west, up and to the right: $west", west > 0.0 && west < 70.0)
    }
}
