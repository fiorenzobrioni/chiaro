package com.callbackdev.chiaro.domain.rules

import com.callbackdev.chiaro.domain.settings.UnitSettings
import com.callbackdev.chiaro.domain.model.WeatherReport
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * `{placeholder}` interpolation for rule messages. Two namespaces:
 *
 * - any [RuleVariables] name, canonical or as displayed in the user's units
 *   (`{current.temp_c}` and `{current.temp_f}` both resolve);
 * - `{trigger.value}` / `{trigger.time}` — the first condition's resolved value
 *   and hour, what made the rule fire.
 *
 * Values render in the user's units, like every other surface. An unknown
 * placeholder stays literal: a typo must never eat part of the user's message.
 */
object RuleMessages {

    // Both braces escaped: Android's regex engine is ICU, stricter than the JVM's
    // about brace metacharacters — a pattern must compile on BOTH, and this object
    // initializes lazily exactly (and only) when a rule fires.
    private val Placeholder = Regex("""\{([A-Za-z0-9_.]+)\}""")
    private val ClockTime = DateTimeFormatter.ofPattern("HH:mm")

    /** The two names the trigger carries; every other name is a [RuleVariables] id.
     * Written down because a screen that offers them must spell them the same way. */
    const val TriggerValue = "trigger.value"
    const val TriggerTime = "trigger.time"

    /** A name as it is written inside a message: the one place that knows the braces. */
    fun placeholder(name: String): String = "{$name}"

    /**
     * How a message's placeholders are written out (23 set 2026). The domain knows what a
     * value IS — its variable, its kind, the number — and not how the reader writes it:
     * the decimal mark, the unit, the clock are the app's, which has a locale and the
     * reader's settings. [Canonical] is the locale-free answer, for tests and anything
     * without a screen.
     */
    interface Writer {
        /**
         * [variableId] is the canonical variable the value belongs to, or null when the
         * rule has no condition to name one; [following] is the message text right after
         * the placeholder, so a writer that adds a unit can see the author already wrote
         * one («{current.temp_c}°») and not print it twice.
         */
        fun value(variableId: String?, kind: RuleVariableKind, value: Double, following: String): String

        fun time(at: LocalDateTime): String
    }

    /** The canonical writing: [RuleVariables.formatValue], a bare number with a decimal
     * point, and a 24-hour clock. */
    class Canonical(private val units: UnitSettings) : Writer {
        override fun value(variableId: String?, kind: RuleVariableKind, value: Double, following: String): String =
            RuleVariables.formatValue(kind, value, units)

        override fun time(at: LocalDateTime): String = at.format(ClockTime)
    }

    fun interpolate(
        message: String,
        trigger: RuleTrigger,
        report: WeatherReport,
        now: LocalDateTime,
        units: UnitSettings,
        writer: Writer = Canonical(units)
    ): String = interpolate(message, trigger.rule, trigger.value, trigger.at, report, now, units, writer)

    /** Same substitution for the dry run, which has a [RuleCheck.Fires] instead. */
    fun interpolate(
        message: String,
        rule: NotificationRule,
        triggerValue: Double,
        triggerAt: LocalDateTime?,
        report: WeatherReport,
        now: LocalDateTime,
        units: UnitSettings,
        writer: Writer = Canonical(units)
    ): String = Placeholder.replace(message) { match ->
        val name = match.groupValues[1]
        val following = message.substring(match.range.last + 1)
        when (name) {
            TriggerValue -> {
                val variable = rule.conditions.firstOrNull()?.variable
                val kind = variable?.let { RuleVariables.byId(it)?.kind } ?: RuleVariableKind.NUMBER
                writer.value(variable, kind, triggerValue, following)
            }
            TriggerTime -> writer.time(triggerAt ?: now)
            else -> {
                val id = RuleVariables.canonicalId(name)
                val variable = id?.let { RuleVariables.byId(it) }
                val resolved = variable?.resolve?.invoke(report, now)
                if (variable != null && resolved != null) {
                    writer.value(id, variable.kind, resolved.value, following)
                } else {
                    match.value // unknown or unavailable: leave the text untouched
                }
            }
        }
    }
}
