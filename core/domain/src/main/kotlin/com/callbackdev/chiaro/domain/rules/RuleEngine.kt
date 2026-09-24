package com.callbackdev.chiaro.domain.rules

import com.callbackdev.chiaro.domain.model.WeatherReport
import java.time.LocalDateTime

/** One rule whose conditions all hold — ready to notify. Exactly one of
 * [fingerprint]/[latchKey] is set, matching the rule's anti-noise semantics. */
data class RuleTrigger(
    val rule: NotificationRule,
    /** Dedup fingerprint (windowed rules); recorded only after a successful post. */
    val fingerprint: String?,
    /** Latch key (instant rules); recorded after a successful post, cleared by the
     * engine once the condition reads false again. */
    val latchKey: String?,
    /** First condition's resolved value/hour — the `trigger.*` placeholders. */
    val value: Double,
    val at: LocalDateTime?
)

/** Persisted engine bookkeeping (RuleStateStore), never user-visible. */
data class RuleEngineState(
    /** `cityKey:ruleId` of instant rules currently true — the edge-trigger memory. */
    val latched: Set<String> = emptySet(),
    /** Recently fired fingerprints of windowed rules. */
    val firedFingerprints: Set<String> = emptySet()
)

data class RuleEvaluation(
    val triggers: List<RuleTrigger>,
    /** Latch keys whose instant rule reads false now — clear these regardless of
     * whether any notification posts, so the rule re-arms. */
    val unlatch: Set<String>
)

/** A single stateless rule check — what the dry run shows, one line per rule. */
sealed interface RuleCheck {
    data class Fires(val value: Double, val at: LocalDateTime?) : RuleCheck
    data object Passes : RuleCheck

    /** The variable can't be resolved right now (AQ down, empty window). */
    data class Unavailable(val variable: String) : RuleCheck
}

/**
 * Pure evaluation of the user's `alerts.rules` (Fase 11) — no clocks, no Android,
 * no I/O, like [com.callbackdev.chiaro.domain.AlertEngine]. Two anti-noise
 * semantics, chosen by what the rule reads:
 *
 * - all conditions on `current.*` → **edge-triggered**: fire on the false→true
 *   transition, re-arm when it reads false again (else `temp < 5` fires every
 *   poll all January);
 * - any forecast condition → **fingerprint per half-day** (`AM`/`PM`), the
 *   same bucket the builtin precipitation warning uses — an aggregate over a
 *   sliding window never cleanly reads "false again";
 * - conditions on `today.*` alone → **fingerprint per day**, from 06:00 (23 set 2026):
 *   a fact about the day is one answer per date.
 */
object RuleEngine {

    fun evaluate(
        rules: List<NotificationRule>,
        report: WeatherReport,
        state: RuleEngineState,
        now: LocalDateTime,
        cityKey: String
    ): RuleEvaluation {
        val triggers = mutableListOf<RuleTrigger>()
        val unlatch = mutableSetOf<String>()
        rules.filter { it.enabled }.forEach { rule ->
            val instant = rule.conditions.all { RuleVariables.isInstant(it.variable) }
            val latchKey = latchKey(cityKey, rule.id)
            when (val result = check(rule, report, now)) {
                // Missing data is not "false": keep the latch, the data may return
                is RuleCheck.Unavailable -> Unit
                RuleCheck.Passes -> if (instant && latchKey in state.latched) {
                    unlatch += latchKey
                }
                is RuleCheck.Fires -> if (instant) {
                    if (latchKey !in state.latched) {
                        triggers += RuleTrigger(rule, null, latchKey, result.value, result.at)
                    }
                } else if (dayShaped(rule)) {
                    // A fact about the day (23 set 2026): once a day, and not before the
                    // day has begun — the half-day bucket was posting «Oggi UV fino a 8»
                    // at 00:05 and again at noon, the same sentence about the same day.
                    if (now.toLocalTime() >= DayRulesFrom) {
                        val fingerprint = "$cityKey:rule:${rule.id}:${now.toLocalDate()}:DAY"
                        if (fingerprint !in state.firedFingerprints) {
                            triggers += RuleTrigger(rule, fingerprint, null, result.value, result.at)
                        }
                    }
                } else {
                    val half = if (now.hour < 12) "AM" else "PM"
                    val fingerprint = "$cityKey:rule:${rule.id}:${now.toLocalDate()}:$half"
                    if (fingerprint !in state.firedFingerprints) {
                        triggers += RuleTrigger(rule, fingerprint, null, result.value, result.at)
                    }
                }
            }
        }
        return RuleEvaluation(triggers, unlatch)
    }

    /**
     * A rule whose forecast conditions are all about TODAY as a whole (`today.*`, with any
     * `current.*` beside them): its answer is one per date, so it speaks once a date. A
     * rule reading a sliding window (`next_Nh.*`) keeps the half-day bucket, because the
     * window it reads really is a different stretch of hours at nine and at three.
     */
    private fun dayShaped(rule: NotificationRule): Boolean =
        rule.conditions.any { it.variable.startsWith(TodayPrefix) } &&
            rule.conditions.all { it.variable.startsWith(TodayPrefix) || RuleVariables.isInstant(it.variable) }

    private const val TodayPrefix = "today."

    /** When a day-shaped rule may first speak: the morning summary's own opening hour. */
    private val DayRulesFrom: java.time.LocalTime = java.time.LocalTime.of(6, 0)

    /**
     * Stateless check of one rule — the engine behind the Alerts screen's
     * "try it now": no dedup, no latching, exactly what IS true right now.
     */
    fun check(rule: NotificationRule, report: WeatherReport, now: LocalDateTime): RuleCheck {
        var first: ResolvedValue? = null
        rule.conditions.forEach { condition ->
            val variable = RuleVariables.byId(condition.variable)
                ?: return RuleCheck.Unavailable(condition.variable)
            val resolved = variable.resolve(report, now)
                ?: return RuleCheck.Unavailable(condition.variable)
            if (first == null) first = resolved
            if (!condition.op.compare(resolved.value, condition.threshold)) {
                return RuleCheck.Passes
            }
        }
        // The first condition is the rule's subject: its value/hour become trigger.*
        return first?.let { RuleCheck.Fires(it.value, it.at) } ?: RuleCheck.Passes
    }

    fun latchKey(cityKey: String, ruleId: Long): String = "$cityKey:$ruleId"
}
