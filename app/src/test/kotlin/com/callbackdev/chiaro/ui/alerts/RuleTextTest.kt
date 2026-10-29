package com.callbackdev.chiaro.ui.alerts

import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.domain.rules.RuleMessages
import com.callbackdev.chiaro.domain.rules.RuleVariableKind
import com.callbackdev.chiaro.domain.rules.RuleVariables
import com.callbackdev.chiaro.domain.sample.sampleWeatherReport
import com.callbackdev.chiaro.domain.settings.PressureUnit
import com.callbackdev.chiaro.domain.settings.TemperatureUnit
import com.callbackdev.chiaro.domain.settings.UnitSettings
import com.callbackdev.chiaro.domain.settings.WindSpeedUnit
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The value list of a rule (the variable picker and the message's «+ Aggiungi un valore») is
 * built from the registry, not written by hand: every variable there must have words, or the
 * list throws when it is opened. Added on 24 set 2026 with the six new variables.
 */
class RuleTextTest {

    /**
     * The sample, its days re-dated so the first is the day the tests read at: a `today.*`
     * placeholder resolves on the row dated today since 15.1 (28 Oct 2026), and the sample's
     * own days start three days after its date.
     */
    private fun datedToday() = sampleWeatherReport().let { sample ->
        sample.copy(
            daily = sample.daily.mapIndexed { i, day -> day.copy(date = LocalDate.of(2023, 10, 27).plusDays(i.toLong())) }
        )
    }

    /** The name is unique (15.4, 28 Oct 2026): another rule's, typed, is refused, and the
     * sheet closes on the name the rule had. */
    @Test
    fun `a name another rule has is taken, whatever its case and blanks`() {
        val others = listOf("Bici", "Corsa al freddo")
        assertTrue(RuleText.isTaken("Bici", others))
        assertTrue(RuleText.isTaken(" bici ", others))
        assertTrue(RuleText.isTaken("CORSA AL FREDDO", others))
        assertFalse(RuleText.isTaken("Bici 2", others))
        assertFalse(RuleText.isTaken("Nuoto", others))
    }

    @Test
    fun `on closing a taken or empty name is the name the rule had`() {
        val others = listOf("Bici")
        assertEquals("Corsa", RuleText.keptName("Bici", "Corsa", others))
        assertEquals("Corsa", RuleText.keptName(" bici", "Corsa", others))
        assertEquals("Corsa", RuleText.keptName("   ", "Corsa", others))
        assertEquals("Nuoto", RuleText.keptName(" Nuoto ", "Corsa", others))
        // Its own name typed again, in another case, is not «taken»: the others are the others.
        assertEquals("corsa", RuleText.keptName("corsa", "Corsa", others))
    }

    @Test
    fun `an idea's name is numbered past a rule that has it in another case`() {
        assertEquals("Bici 2", RuleText.freeName("Bici", listOf("bici")))
    }

    @Test
    fun `every variable a rule can watch or print has words`() {
        RuleVariables.all.forEach { variable ->
            assertNotEquals(variable.id, 0, RuleText.nameRes(variable.id))
        }
    }

    @Test
    fun `the new quantities are offered, with their words and a range to pick from`() {
        mapOf(
            "today.precip_mm" to R.string.var_today_precip_mm,
            "today.snow_cm" to R.string.var_today_snow,
            "today.gust_max_kph" to R.string.var_today_gust,
            "next_6h.gust_max_kph" to R.string.var_next6_gust,
            "next_12h.gust_max_kph" to R.string.var_next12_gust,
            "current.aqi_eu_index" to R.string.var_current_aqi_eu
        ).forEach { (id, words) ->
            // In the message picker: every variable but the yes/no ones.
            assertTrue(id, RuleVariables.all.filter { it.kind != RuleVariableKind.BOOLEAN }.any { it.id == id })
            assertEquals(id, words, RuleText.nameRes(id))
        }
        val metric = UnitSettings()
        assertEquals(RuleText.ValueSpec(0.0, 100.0, 1.0), RuleText.valueSpec("today.precip_mm", metric))
        assertEquals(RuleText.ValueSpec(0.0, 150.0, 5.0), RuleText.valueSpec("today.gust_max_kph", metric))
        assertEquals(RuleText.ValueSpec(0.0, 150.0, 5.0), RuleText.valueSpec("current.aqi_eu_index", metric))
    }

    /** How much in the next hours (15.10, 29 Oct 2026): the words, and the ranges the step set. */
    @Test
    fun `the next hours' millimetres and centimetres have words and their own ranges`() {
        mapOf(
            "next_6h.precip_mm" to R.string.var_next6_precip_mm,
            "next_12h.precip_mm" to R.string.var_next12_precip_mm,
            "next_6h.snow_cm" to R.string.var_next6_snow,
            "next_12h.snow_cm" to R.string.var_next12_snow
        ).forEach { (id, words) ->
            assertTrue(id, RuleVariables.all.filter { it.kind == RuleVariableKind.NUMBER }.any { it.id == id })
            assertEquals(id, words, RuleText.nameRes(id))
        }
        val metric = UnitSettings()
        assertEquals(RuleText.ValueSpec(0.0, 50.0, 1.0), RuleText.valueSpec("next_6h.precip_mm", metric))
        assertEquals(RuleText.ValueSpec(0.0, 100.0, 1.0), RuleText.valueSpec("next_12h.precip_mm", metric))
        assertEquals(RuleText.ValueSpec(0.0, 50.0, 1.0), RuleText.valueSpec("next_6h.snow_cm", metric))
        assertEquals(RuleText.ValueSpec(0.0, 50.0, 1.0), RuleText.valueSpec("next_12h.snow_cm", metric))
        // A day's keep theirs, tomorrow's included.
        assertEquals(RuleText.ValueSpec(0.0, 100.0, 1.0), RuleText.valueSpec("tomorrow.snow_cm", metric))
        // Fifty and a hundred positions: past twenty, no comb of ticks under the thumb.
        assertEquals(0, RuleText.ticks(RuleText.valueSpec("next_6h.snow_cm", metric)))
    }

    /**
     * The slider's grid is the reader's unit's (15.5, 28 Oct 2026): every position, saved
     * canonical and read back, lands on a label that is whole where the unit is whole, one
     * decimal in inches of mercury, and no two positions share a label (in Beaufort, five
     * km/h a step gave several positions to one force). In metric the grids are the old ones.
     */
    @Test
    fun `the threshold slider walks the reader's unit, one label a position`() {
        val metric = UnitSettings()
        val f = UnitSettings(temperature = TemperatureUnit.FAHRENHEIT)
        val mph = UnitSettings(windSpeed = WindSpeedUnit.MPH)
        val ms = UnitSettings(windSpeed = WindSpeedUnit.M_S)
        val kn = UnitSettings(windSpeed = WindSpeedUnit.KNOTS)
        val bft = UnitSettings(windSpeed = WindSpeedUnit.BEAUFORT)
        val inHg = UnitSettings(pressure = PressureUnit.INHG)
        val mmHg = UnitSettings(pressure = PressureUnit.MMHG)
        val cases = listOf(
            Triple("current.temp_c", metric, RuleText.ValueSpec(-30.0, 45.0, 1.0)),
            Triple("current.temp_c", f, RuleText.ValueSpec(-22.0, 113.0, 1.0)),
            Triple("next_6h.feels_like_max_c", f, RuleText.ValueSpec(-22.0, 113.0, 1.0)),
            Triple("current.wind.speed_kph", metric, RuleText.ValueSpec(0.0, 120.0, 5.0)),
            Triple("current.wind.speed_kph", mph, RuleText.ValueSpec(0.0, 75.0, 5.0)),
            Triple("current.wind.speed_kph", ms, RuleText.ValueSpec(0.0, 35.0, 1.0)),
            Triple("current.wind.speed_kph", kn, RuleText.ValueSpec(0.0, 65.0, 5.0)),
            Triple("current.wind.speed_kph", bft, RuleText.ValueSpec(0.0, 12.0, 1.0)),
            Triple("today.gust_max_kph", metric, RuleText.ValueSpec(0.0, 150.0, 5.0)),
            Triple("today.gust_max_kph", mph, RuleText.ValueSpec(0.0, 95.0, 5.0)),
            Triple("today.gust_max_kph", ms, RuleText.ValueSpec(0.0, 40.0, 1.0)),
            Triple("today.gust_max_kph", kn, RuleText.ValueSpec(0.0, 80.0, 5.0)),
            Triple("today.gust_max_kph", bft, RuleText.ValueSpec(0.0, 12.0, 1.0)),
            Triple("current.pressure_mb", metric, RuleText.ValueSpec(950.0, 1050.0, 5.0)),
            Triple("current.pressure_mb", inHg, RuleText.ValueSpec(28.0, 31.0, 0.1)),
            Triple("current.pressure_mb", mmHg, RuleText.ValueSpec(710.0, 790.0, 5.0))
        )
        cases.forEach { (id, units, spec) ->
            val where = "$id in $units"
            assertEquals(where, spec, RuleText.valueSpec(id, units))
            val kind = RuleVariables.byId(id)!!.kind
            val labels = (0..RuleText.positions(spec)).map { i ->
                val shown = RuleText.snap(spec, spec.min + i * spec.step)
                RuleVariables.formatValue(kind, RuleVariables.canonicalValue(kind, shown, units), units)
            }
            assertEquals(where, labels.size, labels.distinct().size)
            val decimals = if (units.pressure == PressureUnit.INHG) 1 else 0
            labels.forEach { label ->
                assertTrue("$where: $label", label.substringAfter('.', "").length <= decimals)
            }
        }
    }

    /** The metric grids, the ones the reader who touched nothing sees, are unchanged. */
    @Test
    fun `the slider draws its ticks up to twenty steps and none past that`() {
        val metric = UnitSettings()
        assertEquals(19, RuleText.ticks(RuleText.valueSpec("current.humidity_pct", metric)))
        assertEquals(11, RuleText.ticks(RuleText.valueSpec("current.uv_index", metric)))
        assertEquals(
            11,
            RuleText.ticks(RuleText.valueSpec("current.wind.speed_kph", UnitSettings(windSpeed = WindSpeedUnit.BEAUFORT)))
        )
        // 75 positions for a temperature, 100 for the millimetres, 30 for the inches: no comb.
        assertEquals(0, RuleText.ticks(RuleText.valueSpec("current.temp_c", metric)))
        assertEquals(0, RuleText.ticks(RuleText.valueSpec("today.precip_mm", metric)))
        assertEquals(0, RuleText.ticks(RuleText.valueSpec("current.pressure_mb", UnitSettings(pressure = PressureUnit.INHG))))
    }

    @Test
    fun `a position snaps to the grid and stays inside the range`() {
        val inHg = RuleText.ValueSpec(28.0, 31.0, 0.1)
        assertEquals(29.9, RuleText.snap(inHg, 29.94), 1e-9)
        assertEquals(30.0, RuleText.snap(inHg, 29.96), 1e-9)
        assertEquals(31.0, RuleText.snap(inHg, 40.0), 1e-9)
        val f = RuleText.ValueSpec(-22.0, 113.0, 1.0)
        assertEquals(55.0, RuleText.snap(f, 54.6), 1e-9)
        assertEquals(-22.0, RuleText.snap(f, -40.0), 1e-9)
    }

    /** A threshold the reader's range holds is kept exactly, on the grid or off it; one
     * outside it is pulled to the edge (the chip that changes the variable). */
    @Test
    fun `a threshold inside the reader's range is kept as it is, outside it goes to the edge`() {
        val mph = UnitSettings(windSpeed = WindSpeedUnit.MPH)
        // 72 km/h is 44.7 mph: off the grid of five, inside the range, kept.
        assertEquals(72.0, RuleText.inRange("current.wind.speed_kph", 72.0, mph), 0.0)
        // 200 km/h is past 75 mph: the edge, back in km/h.
        assertEquals(75 * 1.609344, RuleText.inRange("current.wind.speed_kph", 200.0, mph), 1e-9)
        val bft = UnitSettings(windSpeed = WindSpeedUnit.BEAUFORT)
        assertEquals(40.0, RuleText.inRange("current.wind.speed_kph", 40.0, bft), 0.0)
        val f = UnitSettings(temperature = TemperatureUnit.FAHRENHEIT)
        assertEquals(45.0, RuleText.inRange("current.temp_c", 45.0, f), 0.0)
        assertEquals(-30.0, RuleText.inRange("current.temp_c", -40.0, f), 1e-9)
    }

    /** A gust placeholder is written in the reader's unit and still resolves. */
    @Test
    fun `a gust placeholder follows the wind unit and resolves in a message`() {
        val mph = UnitSettings(windSpeed = WindSpeedUnit.MPH)
        val gust = RuleVariables.byId("today.gust_max_kph")!!
        assertEquals("today.gust_max_mph", RuleVariables.displayId(gust, mph))
        val report = datedToday()
        val rule = com.callbackdev.chiaro.domain.rules.NotificationRule(
            id = 1L, name = "Vento", conditions = emptyList(), message = ""
        )
        val text = RuleMessages.interpolate(
            "Raffiche {today.gust_max_mph}, pioggia {today.precip_mm}",
            rule, 0.0, null, report, LocalDateTime.of(2023, 10, 27, 14, 30), mph
        )
        assertTrue(text, !text.contains("{"))
    }

    /**
     * The units of 14.9 in the rules' words: a threshold and a placeholder carry the
     * reader's unit, the Beaufort force its word, and a pressure its mercury; every
     * displayed spelling of a name resolves in a message.
     */
    @Test
    fun `thresholds and placeholders say the reader's speed and pressure unit`() {
        val report = datedToday()
        val rule = com.callbackdev.chiaro.domain.rules.NotificationRule(
            id = 1L, name = "Vento", conditions = emptyList(), message = ""
        )
        val now = LocalDateTime.of(2023, 10, 27, 14, 30)
        mapOf(
            UnitSettings(windSpeed = WindSpeedUnit.M_S) to "{current.wind.gust_ms}",
            UnitSettings(windSpeed = WindSpeedUnit.KNOTS) to "{today.gust_max_kn}",
            UnitSettings(windSpeed = WindSpeedUnit.BEAUFORT) to "{next_6h.gust_max_bft}",
            UnitSettings(pressure = PressureUnit.INHG) to "{current.pressure_inhg}",
            UnitSettings(pressure = PressureUnit.MMHG) to "{current.pressure_mmhg}"
        ).forEach { (units, placeholder) ->
            val variable = RuleVariables.byId(RuleVariables.canonicalId(placeholder.trim('{', '}'))!!)!!
            assertEquals(placeholder, "{" + RuleVariables.displayId(variable, units) + "}")
            val text = RuleMessages.interpolate("Ora $placeholder.", rule, 0.0, null, report, now, units)
            assertTrue(text, !text.contains("{"))
        }
    }

    /** A feels-like is a temperature (14.6): its picker runs −30…45, not the chance's 0…100. */
    @Test
    fun `a feels-like picks its value on the temperature's range`() {
        assertEquals(RuleText.ValueSpec(-30.0, 45.0, 1.0), RuleText.valueSpec("next_6h.feels_like_max_c", UnitSettings()))
        assertEquals(RuleText.ValueSpec(-30.0, 45.0, 1.0), RuleText.valueSpec("current.feels_like_c", UnitSettings()))
        assertEquals(R.string.var_next12_feels_like, RuleText.nameRes("next_12h.feels_like_max_c"))
    }
}
