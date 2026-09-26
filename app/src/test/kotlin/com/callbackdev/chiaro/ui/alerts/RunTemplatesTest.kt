package com.callbackdev.chiaro.ui.alerts

import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.domain.rules.MaxConditions
import com.callbackdev.chiaro.domain.rules.NotificationRule
import com.callbackdev.chiaro.domain.rules.RuleCheck
import com.callbackdev.chiaro.domain.rules.RuleEngine
import com.callbackdev.chiaro.domain.sample.sampleWeatherReport
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The run in two bands (26 set 2026): the plain run from 5 to 26°, the cold run above 0
 * and under 5, nothing at 0 and below. Checked through the engine itself, on the sample
 * report (rain at most 10% in the next six hours), so a band is what a reader receives.
 */
class RunTemplatesTest {

    private val now = LocalDateTime.of(2023, 10, 27, 14, 30)
    private val run = RuleText.templates.single { it.titleRes == R.string.tpl_run_title }
    private val coldRun = RuleText.templates.single { it.titleRes == R.string.tpl_cold_run_title }

    private fun fires(template: RuleText.Template, tempC: Double): Boolean {
        val base = sampleWeatherReport()
        val report = base.copy(current = base.current.copy(tempC = tempC))
        val rule = NotificationRule(id = 1L, name = "", conditions = template.conditions, message = "")
        return RuleEngine.check(rule, report, now) is RuleCheck.Fires
    }

    @Test
    fun `every idea fits the builder`() {
        RuleText.templates.forEach { template ->
            assertTrue(template.conditions.size in 1..MaxConditions)
        }
    }

    @Test
    fun `the run bands meet at 5 and 0, with no gap and no overlap`() {
        // temperature to (plain run, cold run)
        mapOf(
            -3.0 to (false to false),
            0.0 to (false to false),
            0.5 to (false to true),
            4.9 to (false to true),
            5.0 to (true to false),
            18.0 to (true to false),
            26.0 to (true to false),
            26.5 to (false to false),
        ).forEach { (tempC, expected) ->
            assertEquals("run at $tempC °C", expected.first, fires(run, tempC))
            assertEquals("cold run at $tempC °C", expected.second, fires(coldRun, tempC))
        }
    }

    @Test
    fun `a wet window silences both`() {
        val base = sampleWeatherReport()
        val wet = base.copy(
            current = base.current.copy(tempC = 3.0),
            hourly = base.hourly.map { it.copy(precipChancePct = 60) }
        )
        listOf(run, coldRun).forEach { template ->
            val rule = NotificationRule(id = 1L, name = "", conditions = template.conditions, message = "")
            assertEquals(RuleCheck.Passes, RuleEngine.check(rule, wet, now))
        }
    }
}
