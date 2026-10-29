package com.callbackdev.chiaro.ui.alerts

import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.domain.rules.MaxConditions
import com.callbackdev.chiaro.domain.rules.NotificationRule
import com.callbackdev.chiaro.domain.rules.RuleCheck
import com.callbackdev.chiaro.domain.rules.RuleEngine
import com.callbackdev.chiaro.domain.rules.RuleWindow
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

    /**
     * The ideas, in the sheet's order (15.6, 28 Oct 2026): none on the next twelve hours' rain
     * alone — «Dodici ore asciutte» left, a clear night the registry could not verify, firing
     * at nine in the morning. Seven since «Neve domani» joined after «Ghiaccio domattina»
     * (15.9, 29 Oct 2026).
     */
    @Test
    fun `the ideas are seven, in the sheet's order, and none is the dry twelve hours`() {
        assertEquals(
            listOf(
                R.string.tpl_bike_title, R.string.tpl_ice_title, R.string.tpl_snow_title,
                R.string.tpl_run_title, R.string.tpl_cold_run_title, R.string.tpl_uv_title,
                R.string.tpl_heat_title
            ),
            RuleText.templates.map { it.titleRes }
        )
        assertTrue(
            RuleText.templates.none { template ->
                template.conditions.singleOrNull()?.variable == "next_12h.precip_chance_max"
            }
        )
    }

    @Test
    fun `every idea fits the builder`() {
        RuleText.templates.forEach { template ->
            assertTrue(template.conditions.size in 1..MaxConditions)
        }
    }

    @Test
    fun `the blank idea is a working rule at any hour, apart from the ideas`() {
        val blank = RuleText.blank
        assertEquals(1, blank.conditions.size)
        assertEquals(RuleWindow.Always, blank.window)
        assertTrue(blank !in RuleText.templates)
        val rule = NotificationRule(id = 1L, name = "", conditions = blank.conditions, message = "")
        assertTrue(RuleEngine.check(rule, sampleWeatherReport(), now) !is RuleCheck.Unavailable)
    }

    @Test
    fun `a second rule with a name already taken is numbered`() {
        assertEquals("Il mio avviso", RuleText.freeName("Il mio avviso", listOf("Bici")))
        assertEquals("Il mio avviso 2", RuleText.freeName("Il mio avviso", listOf("Il mio avviso")))
        assertEquals(
            "Bici 3",
            RuleText.freeName("Bici", listOf("Bici", "Bici 2", "Corsa"))
        )
    }

    @Test
    fun `the outdoor ideas are born in daylight, the others at any hour`() {
        val outdoor = setOf(R.string.tpl_bike_title, R.string.tpl_run_title, R.string.tpl_cold_run_title)
        RuleText.templates.forEach { template ->
            val expected = if (template.titleRes in outdoor) RuleWindow.Daylight else RuleWindow.Always
            assertEquals(expected, template.window)
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

    /**
     * «Neve domani» (15.9, 29 Oct 2026): five centimetres on tomorrow's row, and only on it —
     * today's snow is not tomorrow's, and the «Ghiaccio domattina» idea keeps the next 12 hours.
     * Through the engine, on the sample's days re-dated so that the first is today.
     */
    @Test
    fun `the snow idea reads tomorrow's row, from five centimetres`() {
        val snow = RuleText.templates.single { it.titleRes == R.string.tpl_snow_title }
        assertEquals(listOf("tomorrow.snow_cm"), snow.conditions.map { it.variable })
        assertEquals(RuleWindow.Always, snow.window)
        val ice = RuleText.templates.single { it.titleRes == R.string.tpl_ice_title }
        assertEquals(listOf("next_12h.temp_c_min"), ice.conditions.map { it.variable })
        val base = sampleWeatherReport()
        val rule = NotificationRule(id = 1L, name = "", conditions = snow.conditions, message = "")
        fun check(todayCm: Double, tomorrowCm: Double?): RuleCheck {
            val days = base.daily.mapIndexed { i, day -> day.copy(date = now.toLocalDate().plusDays(i.toLong())) }
            val report = base.copy(
                daily = listOf(days[0].copy(snowCm = todayCm), days[1].copy(snowCm = tomorrowCm)) + days.drop(2)
            )
            return RuleEngine.check(rule, report, now)
        }
        assertEquals(RuleCheck.Fires(5.0, null), check(todayCm = 0.0, tomorrowCm = 5.0))
        assertTrue(check(todayCm = 0.0, tomorrowCm = 4.9) is RuleCheck.Passes)
        assertTrue(check(todayCm = 12.0, tomorrowCm = 0.0) is RuleCheck.Passes)
        // A model that does not carry the snow makes it skip, never read zero.
        assertEquals(RuleCheck.Unavailable("tomorrow.snow_cm"), check(todayCm = 0.0, tomorrowCm = null))
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
            val passes = RuleEngine.check(rule, wet, now) as RuleCheck.Passes
            // Silenced by the rain, and the card can say so with the rain's own number.
            assertEquals(template.conditions.first { it.variable.contains("precip") }, passes.failed)
            assertEquals(60.0, passes.value!!, 0.0)
        }
    }
}
