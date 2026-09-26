package com.callbackdev.chiaro.domain.rules

import com.callbackdev.chiaro.domain.model.WeatherReport
import com.callbackdev.chiaro.domain.sample.sampleWeatherReport
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleEngineTest {

    private val now: LocalDateTime = LocalDateTime.of(2023, 10, 27, 14, 30)
    private val report: WeatherReport = sampleWeatherReport() // temp 18.5, uv 4
    private val cityKey = "3173435"

    private fun rule(
        vararg conditions: RuleCondition,
        id: Long = 1,
        enabled: Boolean = true,
        window: RuleWindow = RuleWindow.Always
    ) = NotificationRule(
        id = id,
        name = "rule_$id",
        enabled = enabled,
        conditions = conditions.toList(),
        message = "msg",
        window = window
    )

    private fun evaluate(
        vararg rules: NotificationRule,
        state: RuleEngineState = RuleEngineState(),
        at: LocalDateTime = now
    ) = RuleEngine.evaluate(rules.toList(), report, state, at, cityKey)

    // --- check (the dry run's engine) ---

    @Test
    fun `every operator compares as written`() {
        fun fires(op: RuleOp, threshold: Double): Boolean =
            RuleEngine.check(
                rule(RuleCondition("current.temp_c", op, threshold)), report, now
            ) is RuleCheck.Fires
        assertTrue(fires(RuleOp.GT, 18.0))
        assertTrue(!fires(RuleOp.GT, 18.5))
        assertTrue(fires(RuleOp.GTE, 18.5))
        assertTrue(fires(RuleOp.LT, 19.0))
        assertTrue(!fires(RuleOp.LT, 18.5))
        assertTrue(fires(RuleOp.LTE, 18.5))
        assertTrue(fires(RuleOp.EQ, 18.5))
        assertTrue(fires(RuleOp.NEQ, 20.0))
    }

    @Test
    fun `and requires both conditions`() {
        val both = rule(
            RuleCondition("current.temp_c", RuleOp.GT, 10.0),
            RuleCondition("current.uv_index", RuleOp.GTE, 4.0)
        )
        assertTrue(RuleEngine.check(both, report, now) is RuleCheck.Fires)
        val secondFails = rule(
            RuleCondition("current.temp_c", RuleOp.GT, 10.0),
            RuleCondition("current.uv_index", RuleOp.GTE, 7.0)
        )
        assertEquals(RuleCheck.Passes, RuleEngine.check(secondFails, report, now))
    }

    @Test
    fun `trigger value and hour come from the first condition`() {
        val fires = RuleEngine.check(
            rule(
                RuleCondition("next_6h.temp_c_min", RuleOp.LT, 15.0),
                RuleCondition("current.temp_c", RuleOp.GT, 0.0)
            ),
            report, now
        ) as RuleCheck.Fires
        assertEquals(14.0, fires.value, 0.0) // the window minimum, not current temp
        assertEquals(LocalDateTime.of(2023, 10, 27, 19, 0), fires.at)
    }

    @Test
    fun `an unresolvable variable is unavailable, not false`() {
        val noAq = report.copy(airQuality = null)
        val check = RuleEngine.check(
            rule(RuleCondition("current.aqi_index", RuleOp.GT, 100.0)), noAq, now
        )
        assertEquals(RuleCheck.Unavailable("current.aqi_index"), check)
        // unknown id (e.g. from a future version) degrades the same way
        assertEquals(
            RuleCheck.Unavailable("current.made_up"),
            RuleEngine.check(rule(RuleCondition("current.made_up", RuleOp.GT, 0.0)), report, now)
        )
    }

    // --- evaluate: edge trigger for instant rules ---

    @Test
    fun `an instant rule fires once and latches until false`() {
        val warm = rule(RuleCondition("current.temp_c", RuleOp.GT, 15.0))
        val first = evaluate(warm)
        val trigger = first.triggers.single()
        assertEquals(RuleEngine.latchKey(cityKey, warm.id), trigger.latchKey)
        assertNull(trigger.fingerprint)
        // latched (recorded after the notify) → same truth, no re-fire
        val latched = RuleEngineState(latched = setOf(trigger.latchKey!!))
        assertTrue(evaluate(warm, state = latched).triggers.isEmpty())
    }

    @Test
    fun `a false instant rule re-arms its latch`() {
        val cold = rule(RuleCondition("current.temp_c", RuleOp.LT, 5.0)) // 18.5 → false
        val latchKey = RuleEngine.latchKey(cityKey, cold.id)
        val evaluation = evaluate(cold, state = RuleEngineState(latched = setOf(latchKey)))
        assertTrue(evaluation.triggers.isEmpty())
        assertEquals(setOf(latchKey), evaluation.unlatch)
        // an unlatched rule that reads false stays silent without churn
        assertTrue(evaluate(cold).unlatch.isEmpty())
    }

    @Test
    fun `unavailable data keeps the latch untouched`() {
        val aqi = rule(RuleCondition("current.aqi_index", RuleOp.GT, 100.0))
        val latchKey = RuleEngine.latchKey(cityKey, aqi.id)
        val noAqReport = report.copy(airQuality = null)
        val evaluation = RuleEngine.evaluate(
            listOf(aqi), noAqReport, RuleEngineState(latched = setOf(latchKey)), now, cityKey
        )
        assertTrue(evaluation.triggers.isEmpty())
        assertTrue(evaluation.unlatch.isEmpty())
    }

    // --- evaluate: fingerprints for windowed rules ---

    @Test
    fun `a windowed rule dedups per half-day and re-fires in the next bucket`() {
        // 19° peak at 15:00: true from a morning poll AND from an afternoon one
        val mild = rule(RuleCondition("next_6h.temp_c_max", RuleOp.GTE, 19.0))
        val trigger = evaluate(mild).triggers.single()
        assertNull(trigger.latchKey)
        assertEquals("$cityKey:rule:${mild.id}:2023-10-27:PM", trigger.fingerprint)
        val fired = RuleEngineState(firedFingerprints = setOf(trigger.fingerprint!!))
        assertTrue(evaluate(mild, state = fired).triggers.isEmpty())
        // a morning evaluation is a different half-day bucket → re-fires
        val morning = evaluate(mild, state = fired, at = now.withHour(9))
        assertEquals(
            "$cityKey:rule:${mild.id}:2023-10-27:AM",
            morning.triggers.single().fingerprint
        )
    }

    @Test
    fun `mixing a current condition with a windowed one uses fingerprints`() {
        val mixed = rule(
            RuleCondition("current.temp_c", RuleOp.GT, 0.0),
            RuleCondition("next_6h.precip_chance_max", RuleOp.GTE, 10.0)
        )
        val trigger = evaluate(mixed).triggers.single()
        assertNull(trigger.latchKey)
        assertTrue(trigger.fingerprint!!.contains(":rule:${mixed.id}:"))
    }

    // --- facts about the day (23 set 2026) ---

    @Test
    fun `a rule on today's facts speaks once a day, and not before six`() {
        val uv = rule(RuleCondition("today.uv_max", RuleOp.GTE, 1.0))
        val trigger = evaluate(uv).triggers.single()
        assertEquals("$cityKey:rule:${uv.id}:2023-10-27:DAY", trigger.fingerprint)
        val fired = RuleEngineState(firedFingerprints = setOf(trigger.fingerprint!!))
        // The morning half used to be a second bucket: «Oggi UV fino a 5» twice a day.
        assertTrue(evaluate(uv, state = fired, at = now.withHour(9)).triggers.isEmpty())
        // At 00:05 the day has not begun for anyone reading it.
        assertTrue(evaluate(uv, at = now.withHour(0).withMinute(5)).triggers.isEmpty())
        assertEquals(1, evaluate(uv, at = now.withHour(6)).triggers.size)
    }

    @Test
    fun `today's facts beside a window keep the half-day bucket`() {
        val mixed = rule(
            RuleCondition("today.uv_max", RuleOp.GTE, 1.0),
            RuleCondition("next_6h.precip_chance_max", RuleOp.GTE, 0.0)
        )
        assertTrue(evaluate(mixed).triggers.single().fingerprint!!.endsWith(":PM"))
    }

    // --- gating ---

    @Test
    fun `disabled rules are skipped entirely`() {
        val off = rule(RuleCondition("current.temp_c", RuleOp.GT, 0.0), enabled = false)
        val evaluation = evaluate(off, state = RuleEngineState(
            latched = setOf(RuleEngine.latchKey(cityKey, off.id))
        ))
        assertTrue(evaluation.triggers.isEmpty())
        assertTrue(evaluation.unlatch.isEmpty())
    }

    @Test
    fun `rules evaluate independently`() {
        val fires = rule(RuleCondition("current.temp_c", RuleOp.GT, 0.0), id = 1)
        val silent = rule(RuleCondition("current.temp_c", RuleOp.LT, 0.0), id = 2)
        val evaluation = evaluate(fires, silent)
        assertEquals(listOf(1L), evaluation.triggers.map { it.rule.id })
    }

    // --- a rule's own hours (26 set 2026) ---

    private fun band(from: Int, to: Int) =
        RuleWindow(RuleWindowKind.HOURS, listOf(RuleBand(from * 60, to * 60)))

    @Test
    fun `outside its hours a true rule is silent and records nothing`() {
        val mild = rule(RuleCondition("current.temp_c", RuleOp.GT, 15.0), window = band(7, 9))
        val evaluation = evaluate(mild) // 14:30
        assertTrue(evaluation.triggers.isEmpty())
        assertTrue(evaluation.unlatch.isEmpty())
        // The same truth inside the band speaks.
        assertEquals(1, evaluate(mild, at = now.withHour(8)).triggers.size)
    }

    @Test
    fun `a rule with hours speaks once per band, latch or no latch`() {
        val mild = rule(
            RuleCondition("current.temp_c", RuleOp.GT, 15.0),
            window = RuleWindow(RuleWindowKind.HOURS, listOf(RuleBand(7 * 60, 9 * 60), RuleBand(17 * 60, 19 * 60)))
        )
        val morning = evaluate(mild, at = now.withHour(8)).triggers.single()
        assertNull(morning.latchKey)
        assertEquals("$cityKey:rule:${mild.id}:2023-10-27:H420-540", morning.fingerprint)
        val fired = RuleEngineState(firedFingerprints = setOf(morning.fingerprint!!))
        assertTrue(evaluate(mild, state = fired, at = now.withHour(8).withMinute(45)).triggers.isEmpty())
        // The evening band is its own occurrence: the same truth speaks again there,
        // where the latch would have kept a condition true since the morning silent.
        val evening = evaluate(mild, state = fired, at = now.withHour(18)).triggers.single()
        assertEquals("$cityKey:rule:${mild.id}:2023-10-27:H1020-1140", evening.fingerprint)
    }

    @Test
    fun `a band across noon speaks once, not once per half-day`() {
        val mild = rule(RuleCondition("next_6h.temp_c_max", RuleOp.GTE, 19.0), window = band(11, 16))
        val first = evaluate(mild, at = now.withHour(11).withMinute(30)).triggers.single()
        val fired = RuleEngineState(firedFingerprints = setOf(first.fingerprint!!))
        assertTrue(evaluate(mild, state = fired).triggers.isEmpty()) // 14:30, PM
    }

    @Test
    fun `daylight closes the evening that started it all`() {
        // New York, 27 Oct 2023: the sun sets just before six.
        val bike = rule(
            RuleCondition("current.temp_c", RuleOp.GTE, 12.0),
            window = RuleWindow.Daylight
        )
        assertEquals(
            "$cityKey:rule:${bike.id}:2023-10-27:LIGHT",
            evaluate(bike).triggers.single().fingerprint
        )
        assertTrue(evaluate(bike, at = now.withHour(21).withMinute(47)).triggers.isEmpty())
        assertTrue(evaluate(bike, at = now.withHour(6)).triggers.isEmpty())
    }

    @Test
    fun `a fact about the day keeps one voice per date, in the reader's first band`() {
        val uv = rule(
            RuleCondition("today.uv_max", RuleOp.GTE, 1.0),
            window = RuleWindow(RuleWindowKind.HOURS, listOf(RuleBand(5 * 60, 6 * 60), RuleBand(17 * 60, 19 * 60)))
        )
        // The reader's own five o'clock wins over the six o'clock floor.
        val early = evaluate(uv, at = now.withHour(5).withMinute(15)).triggers.single()
        assertEquals("$cityKey:rule:${uv.id}:2023-10-27:DAY", early.fingerprint)
        val fired = RuleEngineState(firedFingerprints = setOf(early.fingerprint!!))
        assertTrue(evaluate(uv, state = fired, at = now.withHour(18)).triggers.isEmpty())
    }

    @Test
    fun `a false instant rule with hours still re-arms its old latch`() {
        val cold = rule(RuleCondition("current.temp_c", RuleOp.LT, 5.0), window = band(7, 9))
        val latchKey = RuleEngine.latchKey(cityKey, cold.id)
        assertEquals(
            setOf(latchKey),
            evaluate(cold, state = RuleEngineState(latched = setOf(latchKey))).unlatch
        )
    }
}
