package com.callbackdev.chiaro.domain

import com.callbackdev.chiaro.domain.NowcastEngine.Falling
import com.callbackdev.chiaro.domain.NowcastEngine.Intensity
import com.callbackdev.chiaro.domain.model.HourlyForecast
import com.callbackdev.chiaro.domain.model.Nowcast
import com.callbackdev.chiaro.domain.model.QuarterHour
import com.callbackdev.chiaro.domain.model.WeatherCondition
import com.callbackdev.chiaro.domain.model.WeatherReport
import com.callbackdev.chiaro.domain.sample.sampleWeatherReport
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The next two hours, a quarter at a time (17 ott 2026). */
class NowcastEngineTest {

    private val zone = ZoneId.of("Europe/Rome")
    private val base = sampleWeatherReport()

    private fun at(hour: Int, minute: Int = 0): Instant =
        LocalDateTime.of(2026, 10, 17, hour, minute).atZone(zone).toInstant()

    private fun row(hour: Int, chance: Int? = 70, temp: Double = 12.0, code: Int = WmoCode.OVERCAST.code) =
        HourlyForecast(
            time = LocalDateTime.of(2026, 10, 17, hour, 0),
            at = at(hour),
            tempC = temp,
            condition = WeatherCondition(code),
            precipChancePct = chance,
            cloudCoverPct = 90
        )

    /**
     * A report fetched at [fetched] whose quarters start at 14:00 and run on with [mm], one
     * value a quarter; [chance] is every hour's chance unless [hours] says otherwise.
     */
    private fun report(
        vararg mm: Double,
        fetched: Instant = at(14, 5),
        chance: Int? = 70,
        snow: Double? = 0.0,
        temp: Double = 12.0,
        hours: List<HourlyForecast> = (14..19).map { row(it, chance, temp) },
        currentCode: Int = WmoCode.OVERCAST.code
    ): WeatherReport = base.copy(
        current = base.current.copy(condition = WeatherCondition(currentCode)),
        hourly = hours,
        systemInfo = base.systemInfo.copy(lastSync = fetched),
        nowcast = Nowcast(
            mm.mapIndexed { i, v ->
                QuarterHour(at(14).plus(QuarterHour.LENGTH.multipliedBy(i.toLong())), v, snow?.let { v * 0.7 * it })
            }
        )
    )

    private fun dry(n: Int) = DoubleArray(n) { 0.0 }

    @Test
    fun `the window starts in the quarter we are in and ends on the last whole one`() {
        val reading = NowcastEngine.read(report(*dry(16)), at(14, 7))!!
        assertEquals(at(14, 7), reading.now)
        // 14:00–14:15 is the one we are in; 16:00–16:15 would end past 16:07 and is not in.
        assertEquals(at(16, 0), reading.until)
        assertEquals(8, reading.steps.size)
        assertEquals(at(14, 0), reading.steps.first().start)
        assertEquals(at(16, 0), reading.steps.last().end)
        assertFalse(reading.wet)
        assertFalse(reading.endingNow)
    }

    @Test
    fun `rain on the way is one run with its start, its end and its peak`() {
        // Dry until 14:30, then 0.2, 0.8, 0.3 mm a quarter: 0.8, 3.2, 1.2 mm/h.
        val reading = NowcastEngine.read(report(0.0, 0.0, 0.2, 0.8, 0.3, *dry(11)), at(14, 7))!!
        val run = reading.runs.single()
        assertEquals(at(14, 30), run.start)
        assertEquals(at(15, 15), run.end)
        assertFalse(run.ongoing)
        assertFalse(run.lasting)
        assertEquals(at(14, 45), run.peak.start)
        assertEquals(3.2, run.peak.mmPerHour, 1e-9)
        // 3.2 mm/h is past the AMS's 2.5: moderate, by the state engine's own ladder.
        assertEquals(Intensity.MODERATE, run.intensity)
        assertEquals(Falling.RAIN, run.falling)
    }

    @Test
    fun `wet now, the run is ongoing from now`() {
        val reading = NowcastEngine.read(report(0.5, 0.5, 0.1, *dry(13)), at(14, 7))!!
        val run = reading.runs.single()
        assertTrue(run.ongoing)
        assertEquals(at(14, 7), run.start)
        assertEquals(at(14, 45), run.end)
    }

    @Test
    fun `a run still falling at the window's end is lasting`() {
        val reading = NowcastEngine.read(report(*DoubleArray(16) { 0.4 }), at(14, 7))!!
        val run = reading.runs.single()
        assertTrue(run.ongoing)
        assertTrue(run.lasting)
        assertEquals(reading.until, run.end)
    }

    @Test
    fun `a pause makes two runs`() {
        val reading = NowcastEngine.read(report(0.3, 0.3, 0.0, 0.0, 0.0, 0.6, 0.6, *dry(9)), at(14, 7))!!
        assertEquals(2, reading.runs.size)
        assertEquals(at(14, 30), reading.runs[0].end)
        assertEquals(at(15, 15), reading.runs[1].start)
        assertEquals(reading.runs[1].peak, reading.peak)
    }

    @Test
    fun `a quarter in an hour the ensemble does not back is a trace, as the hour is`() {
        // 0.3 mm in the quarter, but the hour's chance is 10%: under the 20% floor.
        val reading = NowcastEngine.read(report(0.0, 0.3, 0.0, *dry(13), chance = 10), at(14, 7))!!
        assertFalse(reading.wet)
    }

    @Test
    fun `an hour with no chance at all lets the amount speak`() {
        val reading = NowcastEngine.read(report(0.0, 0.3, *dry(14), chance = null), at(14, 7))!!
        assertTrue(reading.wet)
    }

    @Test
    fun `under 0_1 mm is not rain`() {
        // 0.02 mm a quarter is 0.08 mm/h: under the measurable floor.
        assertFalse(NowcastEngine.read(report(0.02, 0.02, *dry(14)), at(14, 7))!!.wet)
    }

    @Test
    fun `heavy is the AMS's 7_6 mm an hour`() {
        val reading = NowcastEngine.read(report(0.0, 2.0, *dry(14)), at(14, 7))!!
        assertEquals(Intensity.HEAVY, reading.runs.single().intensity)
    }

    @Test
    fun `snow is named by the model's own snowfall`() {
        val reading = NowcastEngine.read(report(0.0, 0.4, 0.4, *dry(13), snow = 1.0, temp = -1.0), at(14, 7))!!
        assertEquals(Falling.SNOW, reading.falling)
    }

    @Test
    fun `rain then snow is both`() {
        val hours = (14..19).map { row(it) }
        val r = report(0.4, *dry(15), hours = hours).let { rep ->
            val q = rep.nowcast!!.quarters
            rep.copy(nowcast = Nowcast(q.mapIndexed { i, x -> if (i == 1) x.copy(precipMm = 0.4, snowCm = 0.4 * 0.7) else x }))
        }
        assertEquals(Falling.MIXED, NowcastEngine.read(r, at(14, 7))!!.falling)
    }

    @Test
    fun `the hero falling over a dry window is the rain about to stop`() {
        val reading = NowcastEngine.read(
            report(*dry(16), currentCode = WmoCode.RAIN_SLIGHT.code),
            at(14, 7)
        )!!
        assertFalse(reading.wet)
        assertTrue(reading.endingNow)
    }

    @Test
    fun `an old fetch has no reading`() {
        assertNull(NowcastEngine.read(report(*dry(24), fetched = at(13, 0)), at(14, 7)))
        // Exactly an hour is still young.
        assertTrue(NowcastEngine.read(report(*dry(24), fetched = at(13, 7)), at(14, 7)) != null)
    }

    @Test
    fun `less than an hour of quarters ahead has no reading`() {
        // Quarters to 15:00 only, and it is 14:07.
        assertNull(NowcastEngine.read(report(*dry(4)), at(14, 7)))
    }

    @Test
    fun `a gap ends the window where it is`() {
        val r = report(*dry(16)).let { rep ->
            rep.copy(nowcast = Nowcast(rep.nowcast!!.quarters.filterIndexed { i, _ -> i != 6 }))
        }
        // 14:00–15:30 is all there is before the hole: under two hours but over one.
        val reading = NowcastEngine.read(r, at(14, 7))!!
        assertEquals(at(15, 30), reading.until)
    }

    @Test
    fun `no nowcast, no reading`() {
        assertNull(NowcastEngine.read(base.copy(systemInfo = base.systemInfo.copy(lastSync = at(14))), at(14, 7)))
    }

    @Test
    fun `freezing rain is the one phase the amounts cannot show`() {
        val hours = (14..19).map { row(it, code = WmoCode.FREEZING_RAIN_LIGHT.code, temp = -1.0) }
        val reading = NowcastEngine.read(report(0.0, 0.3, *dry(14), hours = hours), at(14, 7))!!
        assertEquals(WmoCode.FREEZING_RAIN_LIGHT.code, reading.runs.single().peak.state)
        // Its dry quarters stay dry.
        assertEquals(1, reading.steps.count { it.wet })
    }

    @Test
    fun `the intensity ladder is the state engine's`() {
        assertEquals(Intensity.LIGHT, NowcastEngine.intensityOf(WmoCode.DRIZZLE_LIGHT.code))
        assertEquals(Intensity.MODERATE, NowcastEngine.intensityOf(WmoCode.SNOW_MODERATE.code))
        assertEquals(Intensity.HEAVY, NowcastEngine.intensityOf(WmoCode.RAIN_HEAVY.code))
        assertNull(NowcastEngine.intensityOf(WmoCode.OVERCAST.code))
    }
}
