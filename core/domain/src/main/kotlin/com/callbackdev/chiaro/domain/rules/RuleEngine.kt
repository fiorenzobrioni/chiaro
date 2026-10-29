package com.callbackdev.chiaro.domain.rules

import com.callbackdev.chiaro.domain.model.WeatherReport
import com.callbackdev.chiaro.domain.zone
import java.time.LocalDateTime
import java.time.LocalTime

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

    /**
     * Not all of it holds. [failed] is the first condition that does not, and [value] what
     * it read (the Alerts review, point 11, 14 ott 2026): «Adesso no · la pioggia è 35%, serve
     * al massimo 20%» needs both, and a bare «no» left the reader to guess which threshold to
     * move. Both null only for a rule with no conditions.
     */
    data class Passes(val failed: RuleCondition? = null, val value: Double? = null) : RuleCheck

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
 *   a fact about the day is one answer per date;
 * - conditions on `tomorrow.*` → the same, keyed on **tomorrow's** date and from 12:00 of the
 *   day before (15.9, 29 Oct 2026).
 *
 * A rule with hours of its own ([NotificationRule.window], 26 set 2026) is silent outside
 * them and speaks **once per occurrence** of its window inside them — once per band, once
 * per day of light — or once per date if it is day-shaped; see [RuleWindows].
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
                is RuleCheck.Passes -> if (instant && latchKey in state.latched) {
                    unlatch += latchKey
                }
                is RuleCheck.Fires -> {
                    // Outside its hours (26 set 2026): silent, and nothing recorded, so a
                    // rule still true when its window opens speaks then — «si pedala» at
                    // 7:00 about a mild night, never at 21:47 about a mild evening.
                    val occurrence = RuleWindows.occurrence(
                        rule.window, now, report.zone(), report.location.coordinates
                    )
                    fun once(fingerprint: String) =
                        RuleTrigger(rule, fingerprint, null, result.value, result.at)
                            .takeIf { fingerprint !in state.firedFingerprints }
                    // Keyed on the date the rule is about: tomorrow's, for a rule on tomorrow.
                    val about = now.toLocalDate().plusDays(daysAhead(rule))
                    val dayFingerprint = "$cityKey:rule:${rule.id}:$about:DAY"
                    val from = speaksFrom(rule)
                    val trigger = when {
                        occurrence == null -> null
                        // One voice per occurrence of the window: per band, per day of
                        // light. The half-day buckets would let a band across noon speak
                        // twice, and the latch would keep a condition true since yesterday
                        // silent through today's band. A fact about the day keeps its one
                        // voice per date, in the first band of the day that opens.
                        !rule.window.always ->
                            once(if (dayShaped(rule)) dayFingerprint else "$cityKey:rule:${rule.id}:$occurrence")
                        instant ->
                            RuleTrigger(rule, null, latchKey, result.value, result.at)
                                .takeIf { latchKey !in state.latched }
                        // A fact about the day (23 set 2026): once a day, and not before the
                        // day has begun — the half-day bucket was posting «Oggi UV fino a 8»
                        // at 00:05 and again at noon, the same sentence about the same day.
                        // A fact about tomorrow: once for its date, from noon (15.9).
                        from != null ->
                            if (now.toLocalTime() >= from) once(dayFingerprint) else null
                        else -> {
                            val half = if (now.hour < 12) "AM" else "PM"
                            once("$cityKey:rule:${rule.id}:${now.toLocalDate()}:$half")
                        }
                    }
                    trigger?.let { triggers += it }
                }
            }
        }
        return RuleEvaluation(triggers, unlatch)
    }

    /**
     * A rule whose forecast conditions are all about a DAY as a whole (`today.*` or
     * `tomorrow.*`, with any `current.*` beside them): its answer is one per date, so it
     * speaks once a date. A rule reading a sliding window (`next_Nh.*`) keeps the half-day
     * bucket, because the window it reads really is a different stretch of hours at nine and
     * at three.
     */
    private fun dayShaped(rule: NotificationRule): Boolean =
        rule.conditions.any { it.variable.isDay() } &&
            rule.conditions.all { it.variable.isDay() || RuleVariables.isInstant(it.variable) }

    private fun String.isDay() = startsWith(TodayPrefix) || startsWith(TomorrowPrefix)

    /** The date a day-shaped rule is about, in days after today: tomorrow's as soon as one
     * condition reads it (a rule on today and tomorrow is answered once, when tomorrow is). */
    private fun daysAhead(rule: NotificationRule): Long =
        if (rule.conditions.any { it.variable.startsWith(TomorrowPrefix) }) 1L else 0L

    private const val TodayPrefix = "today."
    private const val TomorrowPrefix = "tomorrow."

    /**
     * When a rule with no hours of its own may first speak on a date, if it is a fact about a
     * day: [DayRulesFrom] for today, [TomorrowRulesFrom] for tomorrow; null for any other rule,
     * which speaks when it holds, and for a rule with hours, whose first band decides — the
     * reader's own five o'clock wins over the six o'clock floor, and a morning band over noon,
     * or a «7–9» rule on tomorrow could never speak at all. The Alerts card reads it too, so
     * that «Adesso sì» at nine says the voice comes at noon.
     */
    fun speaksFrom(rule: NotificationRule): LocalTime? = when {
        !rule.window.always || !dayShaped(rule) -> null
        daysAhead(rule) > 0 -> TomorrowRulesFrom
        else -> DayRulesFrom
    }

    /** When a fact about today may first speak: the morning summary's own opening hour. */
    private val DayRulesFrom: LocalTime = LocalTime.of(6, 0)

    /**
     * When a fact about tomorrow may first speak, on the day before (15.9, 29 Oct 2026): noon.
     * In the morning the summary says today and the reader plans today; from the afternoon the
     * next day is the one being planned, and at 18 the evening summary closes it. Earlier, a
     * rule on tomorrow would talk over today's morning about a day thirty-six hours away, on a
     * run the afternoon's runs revise; later, it would leave no afternoon to act in (the tyres,
     * the car in the garage). One voice per date, so the evening summary at 18 is never
     * followed by the same rule a second time.
     */
    private val TomorrowRulesFrom: LocalTime = LocalTime.of(12, 0)

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
                return RuleCheck.Passes(condition, resolved.value)
            }
        }
        // The first condition is the rule's subject: its value/hour become trigger.*
        return first?.let { RuleCheck.Fires(it.value, it.at) } ?: RuleCheck.Passes()
    }

    fun latchKey(cityKey: String, ruleId: Long): String = "$cityKey:$ruleId"
}
