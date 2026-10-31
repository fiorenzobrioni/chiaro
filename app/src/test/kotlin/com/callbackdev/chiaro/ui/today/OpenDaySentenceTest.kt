package com.callbackdev.chiaro.ui.today

import com.callbackdev.chiaro.domain.ConditionWord
import com.callbackdev.chiaro.domain.WmoCode
import com.callbackdev.chiaro.domain.model.HourlyForecast
import com.callbackdev.chiaro.domain.model.WeatherCondition
import com.callbackdev.chiaro.ui.today.OpenDaySentence.Fall
import com.callbackdev.chiaro.ui.today.OpenDaySentence.Part
import com.callbackdev.chiaro.ui.today.OpenDaySentence.Register
import com.callbackdev.chiaro.ui.today.OpenDaySentence.Sky
import com.callbackdev.chiaro.ui.today.OpenDaySentence.Span
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The open day's sentence, language-free (PLANNING, «La frase del giorno aperto», 31 ott
 * 2026): the sky by the part of the day, what falls by the hour near and by the part far out,
 * and never a word the row's icon would contradict.
 */
class OpenDaySentenceTest {

    private val zone = ZoneId.of("Europe/Rome")
    private val date = LocalDate.of(2026, 11, 3)

    private val clear = WmoCode.CLEAR.code
    private val mostlyClear = WmoCode.MAINLY_CLEAR.code
    private val cloudy = WmoCode.PARTLY_CLOUDY.code
    private val overcast = WmoCode.OVERCAST.code
    private val rain = WmoCode.RAIN_MODERATE.code
    private val drizzle = WmoCode.DRIZZLE_LIGHT.code

    private fun at(h: Int): LocalDateTime = date.atTime(h, 0)

    /** A sky code's own cloud, so a wet hour votes with the sky around it. */
    private fun cloudOf(code: Int) = when (code) {
        clear -> 10
        mostlyClear -> 30
        cloudy -> 65
        else -> 95
    }

    private fun hour(h: Int, code: Int, cloud: Int = cloudOf(code)) = HourlyForecast(
        time = at(h),
        at = at(h).atZone(zone).toInstant(),
        tempC = 12.0,
        condition = WeatherCondition(code),
        precipChancePct = 50,
        cloudCoverPct = cloud
    )

    /** A whole day: [sky] for every hour, [codes] where something else is the state. */
    private fun day(sky: (Int) -> Int = { overcast }, codes: Map<Int, Int> = emptyMap()) =
        (0..23).map { h -> codes[h]?.let { hour(h, it, cloudOf(sky(h))) } ?: hour(h, sky(h)) }

    /** The sun is down before 7 and from 19, as on an autumn day in Rome. */
    private fun of(label: Int, hours: List<HourlyForecast>, inHours: Boolean = true) =
        OpenDaySentence.of(label, hours.map { StripHour(it, night = it.time.hour !in 7..18) }, inHours)

    @Test
    fun `a steady sky is one stretch over the whole day`() {
        val s = of(clear, day({ clear }))!!
        assertEquals(listOf(Sky(clear, listOf(Part.MORNING, Part.AFTERNOON, Part.EVENING))), s.sky)
        assertTrue(s.falls.isEmpty())
    }

    @Test
    fun `the sky changes by the part of the day`() {
        val s = of(clear, day({ h -> if (h < 12) clear else overcast }))!!
        assertEquals(
            listOf(Sky(clear, listOf(Part.MORNING)), Sky(overcast, listOf(Part.AFTERNOON, Part.EVENING))),
            s.sky
        )
    }

    @Test
    fun `clear to mostly clear is not a change, and a steady sky says the label's word`() {
        val s = of(mostlyClear, day({ h -> if (h < 12) clear else mostlyClear }))!!
        assertEquals(listOf(Sky(mostlyClear, listOf(Part.MORNING, Part.AFTERNOON, Part.EVENING))), s.sky)
    }

    @Test
    fun `the night does not vote for the day's sky`() {
        val s = of(clear, day({ h -> if (h < 6) overcast else clear }))!!
        assertEquals(listOf(Sky(clear, listOf(Part.MORNING, Part.AFTERNOON, Part.EVENING))), s.sky)
    }

    @Test
    fun `the hours before sunrise do not vote for the morning`() {
        // A winter morning: the sun up at 9, overcast until then and clear after. Counted
        // whole, three grey hours against three clear ones tied to the heavier, «Coperto al
        // mattino», the night's word over a morning of sun (the README's Milan, 26 Sep 2026).
        val hours = day({ h -> if (h < 9) overcast else clear })
            .map { StripHour(it, night = it.time.hour !in 9..16) }
        val s = OpenDaySentence.of(clear, hours, inHours = true)!!
        assertEquals(listOf(Sky(clear, listOf(Part.MORNING, Part.AFTERNOON, Part.EVENING))), s.sky)
    }

    @Test
    fun `the evening votes with all its hours, dark or light`() {
        val s = of(clear, day({ h -> if (h >= 19) overcast else clear }))!!
        assertEquals(
            listOf(Sky(clear, listOf(Part.MORNING, Part.AFTERNOON)), Sky(overcast, listOf(Part.EVENING))),
            s.sky
        )
    }

    @Test
    fun `a tie goes to the heavier look, then the heavier word`() {
        // The morning's daylight, 7 to 11: two hours of sun, two of sun and cloud, one grey.
        // The tie between the first two goes to the cloud.
        val codes = mapOf(7 to mostlyClear, 8 to mostlyClear, 9 to cloudy, 10 to cloudy, 11 to overcast)
        val s = of(clear, day({ h -> codes[h] ?: clear }))!!
        assertEquals(Sky(cloudy, listOf(Part.MORNING)), s.sky.first())
    }

    @Test
    fun `fog in the morning is a sky of its own`() {
        val s = of(clear, day({ h -> if (h < 12) WmoCode.FOG.code else clear }))!!
        assertEquals(
            listOf(Sky(WmoCode.FOG.code, listOf(Part.MORNING)), Sky(clear, listOf(Part.AFTERNOON, Part.EVENING))),
            s.sky
        )
    }

    @Test
    fun `a wet day near is said by the hour, to the end of its last hour`() {
        val s = of(rain, day(codes = mapOf(17 to rain, 18 to rain, 19 to rain)))!!
        assertEquals(
            listOf(Fall(ConditionWord.RAIN, WmoCode.Phase.LIQUID, Register.FULL, listOf(Span.Hours(at(17), at(20))))),
            s.falls
        )
        assertEquals(listOf(Sky(overcast, listOf(Part.MORNING, Part.AFTERNOON, Part.EVENING))), s.sky)
    }

    @Test
    fun `two runs of one word are one clause`() {
        val s = of(rain, day(codes = mapOf(9 to rain, 10 to rain, 15 to rain, 16 to rain)))!!
        assertEquals(listOf(Span.Hours(at(9), at(11)), Span.Hours(at(15), at(17))), s.falls.single().spans)
    }

    @Test
    fun `a wet hour that did not earn the day is light`() {
        val s = of(cloudy, day({ cloudy }, mapOf(15 to drizzle)))!!
        val fall = s.falls.single()
        assertEquals(Register.LIGHT, fall.register)
        assertEquals(listOf(Span.Hours(at(15), at(16))), fall.spans)
        assertEquals(listOf(Sky(cloudy, listOf(Part.MORNING, Part.AFTERNOON, Part.EVENING))), s.sky)
    }

    @Test
    fun `light snow keeps its flake`() {
        val s = of(overcast, day(codes = mapOf(8 to WmoCode.SNOW_SLIGHT.code)))!!
        assertEquals(WmoCode.Phase.FROZEN, s.falls.single().phase)
        assertEquals(Register.LIGHT, s.falls.single().register)
    }

    @Test
    fun `a hazard is never light`() {
        val s = of(overcast, day(codes = mapOf(7 to WmoCode.FREEZING_DRIZZLE_LIGHT.code)))!!
        assertEquals(Register.FULL, s.falls.single().register)
        assertEquals(ConditionWord.FREEZING_DRIZZLE, s.falls.single().word)
    }

    @Test
    fun `far out the runs are said by the part of the day`() {
        val s = of(rain, day(codes = mapOf(17 to rain, 18 to rain, 19 to rain)), inHours = false)!!
        assertEquals(listOf(Span.Parts(listOf(Part.AFTERNOON, Part.EVENING))), s.falls.single().spans)
        assertTrue(!s.falls.single().scattered)
    }

    @Test
    fun `three runs are a broken day, by the part of the day even near`() {
        val s = of(rain, day(codes = mapOf(8 to rain, 13 to rain, 19 to rain)))!!
        val fall = s.falls.single()
        assertTrue(fall.scattered)
        assertEquals(listOf(Span.Parts(listOf(Part.MORNING, Part.AFTERNOON, Part.EVENING))), fall.spans)
    }

    @Test
    fun `parts of different words are separate clauses`() {
        val s = of(
            WmoCode.SNOW_MODERATE.code,
            day(codes = mapOf(7 to WmoCode.SNOW_MODERATE.code, 8 to WmoCode.SNOW_MODERATE.code, 15 to rain)),
            inHours = false
        )!!
        assertEquals(listOf(ConditionWord.SNOW, ConditionWord.RAIN), s.falls.map { it.word })
        assertEquals(listOf(Span.Parts(listOf(Part.MORNING))), s.falls[0].spans)
        assertEquals(listOf(Span.Parts(listOf(Part.AFTERNOON))), s.falls[1].spans)
    }

    @Test
    fun `rain and snow in one run is rain and snow`() {
        val s = of(rain, day(codes = mapOf(10 to WmoCode.SNOW_SLIGHT.code, 11 to rain)))!!
        assertEquals(ConditionWord.RAIN_AND_SNOW, s.falls.single().word)
    }

    @Test
    fun `the day's edges are open ends`() {
        val s = of(rain, day(codes = mapOf(0 to rain, 1 to rain, 2 to rain, 22 to rain, 23 to rain)))!!
        assertEquals(listOf(Span.Hours(null, at(3)), Span.Hours(at(22), null)), s.falls.single().spans)
    }

    @Test
    fun `rain all day leaves the sky unsaid`() {
        val s = of(rain, day(codes = (0..23).associateWith { rain }))!!
        assertTrue(s.sky.isEmpty())
        assertEquals(listOf(Span.Hours(null, null)), s.falls.single().spans)
        assertTrue(s.falls.single().allDay)
    }

    @Test
    fun `a label that says rain with no wet hour on the row draws nothing`() {
        assertNull(of(rain, day()))
    }

    @Test
    fun `a possibility is said by the part of the day`() {
        val s = of(overcast, day(codes = mapOf(14 to WmoCode.RAIN_LIKELY.code, 15 to WmoCode.RAIN_LIKELY.code)))!!
        val fall = s.falls.single()
        assertEquals(Register.POSSIBLE, fall.register)
        assertEquals(ConditionWord.RAIN_LIKELY, fall.word)
        assertEquals(listOf(Span.Parts(listOf(Part.AFTERNOON))), fall.spans)
    }

    @Test
    fun `a possibility beside rain that falls is not said, a possible storm is`() {
        val codes = mapOf(9 to rain, 10 to rain, 15 to WmoCode.RAIN_LIKELY.code, 18 to WmoCode.THUNDERSTORM_POSSIBLE.code)
        val s = of(WmoCode.THUNDERSTORM_POSSIBLE.code, day(codes = codes))!!
        assertEquals(listOf(ConditionWord.RAIN, ConditionWord.THUNDERSTORM_POSSIBLE), s.falls.map { it.word })
        assertEquals(Register.FULL, s.falls[0].register)
        assertEquals(Register.POSSIBLE, s.falls[1].register)
        assertEquals(listOf(Span.Parts(listOf(Part.EVENING))), s.falls[1].spans)
    }

    @Test
    fun `a wet hour votes with its own cloud`() {
        val hours = day({ clear }, mapOf(13 to drizzle, 14 to drizzle)).map {
            if (it.time.hour in 13..14) it.copy(cloudCoverPct = 95) else it
        }
        // Two overcast hours out of six do not make the afternoon grey.
        assertEquals(1, of(clear, hours)!!.sky.size)
    }

    @Test
    fun `no hours, no sentence`() {
        assertNull(of(clear, emptyList()))
    }
}
