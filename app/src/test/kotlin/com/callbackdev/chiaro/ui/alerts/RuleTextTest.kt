package com.callbackdev.chiaro.ui.alerts

import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.domain.rules.RuleMessages
import com.callbackdev.chiaro.domain.rules.RuleVariableKind
import com.callbackdev.chiaro.domain.rules.RuleVariables
import com.callbackdev.chiaro.domain.sample.sampleWeatherReport
import com.callbackdev.chiaro.domain.settings.UnitSettings
import com.callbackdev.chiaro.domain.settings.WindSpeedUnit
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The value list of a rule (the variable picker and the message's «+ Aggiungi un valore») is
 * built from the registry, not written by hand: every variable there must have words, or the
 * list throws when it is opened. Added on 24 set 2026 with the six new variables.
 */
class RuleTextTest {

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
        assertEquals(RuleText.ValueSpec(0.0, 100.0, 1.0), RuleText.valueSpec("today.precip_mm"))
        assertEquals(RuleText.ValueSpec(0.0, 150.0, 5.0), RuleText.valueSpec("today.gust_max_kph"))
        assertEquals(RuleText.ValueSpec(0.0, 150.0, 5.0), RuleText.valueSpec("current.aqi_eu_index"))
    }

    /** A gust placeholder is written in the reader's unit and still resolves. */
    @Test
    fun `a gust placeholder follows the wind unit and resolves in a message`() {
        val mph = UnitSettings(windSpeed = WindSpeedUnit.MPH)
        val gust = RuleVariables.byId("today.gust_max_kph")!!
        assertEquals("today.gust_max_mph", RuleVariables.displayId(gust, mph))
        val report = sampleWeatherReport()
        val rule = com.callbackdev.chiaro.domain.rules.NotificationRule(
            id = 1L, name = "Vento", conditions = emptyList(), message = ""
        )
        val text = RuleMessages.interpolate(
            "Raffiche {today.gust_max_mph}, pioggia {today.precip_mm}",
            rule, 0.0, null, report, LocalDateTime.of(2023, 10, 27, 14, 30), mph
        )
        assertTrue(text, !text.contains("{"))
    }
}
