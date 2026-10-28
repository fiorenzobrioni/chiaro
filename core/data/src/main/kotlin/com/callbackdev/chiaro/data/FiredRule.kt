package com.callbackdev.chiaro.data

import com.callbackdev.chiaro.domain.rules.NotificationRule

/**
 * One rule a history commit lists as fired (15.4, 28 Oct 2026): its id and its name, written
 * `id:nome` ([encode]). Until 15.4 the commit carried the name alone and Alerts read «last
 * fired» by name, so two rules with one name shared a past and a rename started it over; the
 * id is what a rename leaves alone, and the name stays beside it for a reader of the row.
 *
 * The rows written before 15.4 carry the name only: [decode] reads them as a [FiredRule] with
 * no id, and [isFor] matches those by name, so no past is lost at the update. A commit lives
 * three days (`weather_history`'s retention), after which every row is of the new form. In
 * those three days a name of the old rows that itself begins with digits and a colon («12:00
 * corsa») reads as an id and is not found: the one ambiguity of the form, three days long.
 */
data class FiredRule(val id: Long?, val name: String) {

    /** Whether this row is [rule]'s: by id where the row has one, by name where it has not. */
    fun isFor(rule: NotificationRule): Boolean =
        if (id != null) id == rule.id else name == rule.name

    fun encode(): String = if (id != null) "$id:$name" else name

    companion object {
        fun of(rule: NotificationRule): FiredRule = FiredRule(rule.id, rule.name)

        /** The leading id and the colon; the name after them may itself hold a colon. */
        private val Form = Regex("""^(\d+):(.*)$""", RegexOption.DOT_MATCHES_ALL)

        fun decode(raw: String): FiredRule {
            val match = Form.matchEntire(raw) ?: return FiredRule(null, raw)
            val id = match.groupValues[1].toLongOrNull() ?: return FiredRule(null, raw)
            return FiredRule(id, match.groupValues[2])
        }
    }
}
