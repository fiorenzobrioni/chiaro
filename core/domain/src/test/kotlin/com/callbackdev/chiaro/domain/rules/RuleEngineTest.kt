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
    // temp 18.5, uv 4. The sample's days start three days after its date: the first is
    // re-dated to today's, the row a `today.*` rule reads since 15.1 (28 Oct 2026).
    private val report: WeatherReport = sampleWeatherReport().let { sample ->
        sample.copy(
            daily = sample.daily.mapIndexed { i, day -> day.copy(date = now.toLocalDate().plusDays(i.toLong())) }
        )
    }
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
        val passes = RuleEngine.check(secondFails, report, now) as RuleCheck.Passes
        // The condition that does not hold, and what it read (the Alerts review, point 11).
        assertEquals(secondFails.conditions[1], passes.failed)
        assertTrue(passes.value != null && passes.value!! < 7.0)
    }

    @Test
    fun `the first condition that fails is the one reported`() {
        val bothFail = rule(
            RuleCondition("current.temp_c", RuleOp.LT, -40.0),
            RuleCondition("current.uv_index", RuleOp.GTE, 99.0)
        )
        val passes = RuleEngine.check(bothFail, report, now) as RuleCheck.Passes
        assertEquals(bothFail.conditions[0], passes.failed)
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
        // a morning evaluation is a different half-day bucket → re-fires (at ten, whose six
        // hours reach the 15:00 row, since 15.10 the sixth)
        val morning = evaluate(mild, state = fired, at = now.withHour(10))
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

    // --- facts about tomorrow (15.9, 29 Oct 2026) ---

    @Test
    fun `a rule on tomorrow speaks once for tomorrow's date, and not before noon`() {
        val mild = rule(RuleCondition("tomorrow.high_c", RuleOp.GTE, 15.0)) // tomorrow 18°
        // The morning is today's: the summary says it, and tomorrow is a day and a half away.
        assertTrue(evaluate(mild, at = now.withHour(9)).triggers.isEmpty())
        assertTrue(evaluate(mild, at = now.withHour(11).withMinute(59)).triggers.isEmpty())
        val noon = evaluate(mild, at = now.withHour(12).withMinute(0)).triggers.single()
        // Keyed on the date it is about, not the date it is said on.
        assertEquals("$cityKey:rule:${mild.id}:2023-10-28:DAY", noon.fingerprint)
        assertNull(noon.latchKey)
        assertEquals(18.0, noon.value, 0.0)
        assertNull(noon.at)
        val fired = RuleEngineState(firedFingerprints = setOf(noon.fingerprint!!))
        // At 18 the evening summary speaks of tomorrow; the rule has, and does not again.
        assertTrue(evaluate(mild, state = fired, at = now.withHour(18)).triggers.isEmpty())
        assertTrue(evaluate(mild, state = fired, at = now.withHour(23).withMinute(30)).triggers.isEmpty())
        // Past midnight «tomorrow» is the next date, and it waits for noon again.
        val nextDay = now.plusDays(1)
        assertTrue(evaluate(mild, state = fired, at = nextDay.withHour(0).withMinute(30)).triggers.isEmpty())
        val next = evaluate(mild, state = fired, at = nextDay.withHour(12).withMinute(15)).triggers.single()
        assertEquals("$cityKey:rule:${mild.id}:2023-10-29:DAY", next.fingerprint)
    }

    @Test
    fun `a rule on tomorrow that first holds in the evening still speaks, once`() {
        val wet = rule(RuleCondition("tomorrow.precip_pct", RuleOp.GTE, 80.0)) // tomorrow 85%
        val evening = evaluate(wet, at = now.withHour(19)).triggers.single()
        val fired = RuleEngineState(firedFingerprints = setOf(evening.fingerprint!!))
        assertTrue(evaluate(wet, state = fired, at = now.withHour(21)).triggers.isEmpty())
    }

    @Test
    fun `today and tomorrow in one rule are answered once, when tomorrow is`() {
        val both = rule(
            RuleCondition("today.high_c", RuleOp.GTE, 15.0),
            RuleCondition("tomorrow.high_c", RuleOp.GTE, 15.0)
        )
        assertTrue(evaluate(both, at = now.withHour(10)).triggers.isEmpty())
        assertEquals(
            "$cityKey:rule:${both.id}:2023-10-28:DAY",
            evaluate(both).triggers.single().fingerprint
        )
    }

    @Test
    fun `tomorrow beside a window keeps the half-day bucket`() {
        val mixed = rule(
            RuleCondition("tomorrow.high_c", RuleOp.GTE, 15.0),
            RuleCondition("next_6h.precip_chance_max", RuleOp.GTE, 0.0)
        )
        assertEquals(
            "$cityKey:rule:${mixed.id}:2023-10-27:AM",
            evaluate(mixed, at = now.withHour(10)).triggers.single().fingerprint
        )
    }

    @Test
    fun `a rule on tomorrow with hours speaks in its first band, before noon too`() {
        // The reader's own morning wins over noon, as their five o'clock wins over six for
        // today: a 7–9 band under a noon floor could never speak at all.
        val mild = rule(RuleCondition("tomorrow.high_c", RuleOp.GTE, 15.0), window = band(7, 9))
        val morning = evaluate(mild, at = now.withHour(8)).triggers.single()
        assertEquals("$cityKey:rule:${mild.id}:2023-10-28:DAY", morning.fingerprint)
        assertTrue(evaluate(mild).triggers.isEmpty()) // 14:30, outside the band
    }

    @Test
    fun `the hour a fact about a day may first speak`() {
        assertEquals(
            java.time.LocalTime.of(6, 0),
            RuleEngine.speaksFrom(rule(RuleCondition("today.uv_max", RuleOp.GTE, 1.0)))
        )
        assertEquals(
            java.time.LocalTime.NOON,
            RuleEngine.speaksFrom(rule(RuleCondition("tomorrow.snow_cm", RuleOp.GTE, 5.0)))
        )
        assertEquals(
            java.time.LocalTime.NOON,
            RuleEngine.speaksFrom(
                rule(RuleCondition("current.temp_c", RuleOp.LT, 3.0), RuleCondition("tomorrow.snow_cm", RuleOp.GTE, 5.0))
            )
        )
        // Any other rule speaks when it holds; a rule with hours, when its band opens.
        assertNull(RuleEngine.speaksFrom(rule(RuleCondition("current.temp_c", RuleOp.GT, 0.0))))
        assertNull(RuleEngine.speaksFrom(rule(RuleCondition("next_6h.temp_c_max", RuleOp.GT, 0.0))))
        assertNull(
            RuleEngine.speaksFrom(rule(RuleCondition("tomorrow.snow_cm", RuleOp.GTE, 5.0), window = band(7, 9)))
        )
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
