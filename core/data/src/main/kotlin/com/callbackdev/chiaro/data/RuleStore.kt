package com.callbackdev.chiaro.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.callbackdev.chiaro.domain.rules.MaxRules
import com.callbackdev.chiaro.domain.rules.NotificationRule
import com.callbackdev.chiaro.domain.rules.RuleCondition
import com.callbackdev.chiaro.domain.rules.RuleOp
import com.callbackdev.chiaro.domain.rules.RulePlaces
import com.callbackdev.chiaro.domain.rules.RuleWindow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.rulesDataStore by preferencesDataStore(name = "rules")

/**
 * The user's `alerts.rules` file (Fase 11): the rule list as a JSON array plus a
 * monotonic id counter, same DataStore pattern as the saved cities. Ids never
 * recycle — they key notification ids, latch keys and fingerprints, and a new rule
 * must never inherit a removed rule's state.
 */
class RuleStore(
    private val dataStore: DataStore<Preferences>,
    private val json: Json
) {

    val rules: Flow<List<NotificationRule>> = dataStore.data.map(::decode).distinctUntilChanged()

    /** Appends a template rule the user then edits token by token; no-op at the
     * [MaxRules] ceiling (the UI hides `+ add rule` there too). */
    suspend fun add() {
        dataStore.edit { prefs ->
            val rules = decode(prefs)
            if (rules.size >= MaxRules) return@edit
            val id = prefs[NextRuleId] ?: 1L
            prefs[NextRuleId] = id + 1
            prefs[RulesJson] = json.encodeToString(rules + template(id))
        }
    }

    /**
     * Appends a rule with the given content — Chiaro's templates, whose name and
     * message are the reader's language at creation time (Fase 6). Additive next to
     * [add], which keeps upstream's fixed English starter for parity. Returns the
     * created rule, or null at the [MaxRules] ceiling.
     */
    suspend fun add(
        name: String,
        conditions: List<RuleCondition>,
        message: String,
        window: RuleWindow = RuleWindow.Always
    ): NotificationRule? {
        var created: NotificationRule? = null
        dataStore.edit { prefs ->
            val rules = decode(prefs)
            if (rules.size >= MaxRules) return@edit
            val id = prefs[NextRuleId] ?: 1L
            prefs[NextRuleId] = id + 1
            val rule = NotificationRule(
                id = id, name = name, enabled = true,
                conditions = conditions, message = message, window = window
            )
            created = rule
            prefs[RulesJson] = json.encodeToString(rules + rule)
        }
        return created
    }

    suspend fun update(rule: NotificationRule) {
        dataStore.edit { prefs ->
            val rules = decode(prefs)
            prefs[RulesJson] = json.encodeToString(rules.map { if (it.id == rule.id) rule else it })
        }
    }

    /**
     * Switches off every rule bound to a place no longer in [savedIds] (15.11, 29 Oct 2026;
     * [RulePlaces.isOrphan]) and returns them. The place stays on the rule: the card names the
     * place that is gone, and a place re-added, or brought back by the «Annulla» of its removal,
     * finds its rules bound to it again, off until the reader turns them on. One edit, read and
     * written in the same transaction, so a rule the reader saves meanwhile is not overwritten
     * with an older copy.
     */
    suspend fun switchOffOrphans(savedIds: Set<Long>): List<NotificationRule> {
        var switchedOff = emptyList<NotificationRule>()
        dataStore.edit { prefs ->
            val rules = decode(prefs)
            switchedOff = rules.filter { it.enabled && RulePlaces.isOrphan(it, savedIds) }
            if (switchedOff.isEmpty()) return@edit
            val ids = switchedOff.map { it.id }.toSet()
            prefs[RulesJson] = json.encodeToString(
                rules.map { if (it.id in ids) it.copy(enabled = false) else it }
            )
        }
        return switchedOff
    }

    suspend fun remove(id: Long) {
        dataStore.edit { prefs ->
            prefs[RulesJson] = json.encodeToString(decode(prefs).filterNot { it.id == id })
        }
    }

    /**
     * Puts back a rule [remove] took away, as it was and where it was ([index], clamped):
     * the «Annulla» of a deletion (the Alerts review, point 10, 14 ott 2026). The rule keeps
     * its own id. That is not a recycled id, because nothing else ever had it: it is the same
     * rule coming back, so its latch and the alerts it already sent today are its own again
     * and it does not fire a second time. A no-op when the id is already there or the list is
     * at [MaxRules] (a rule added while the snackbar was up).
     */
    suspend fun restore(rule: NotificationRule, index: Int) {
        dataStore.edit { prefs ->
            val rules = decode(prefs)
            if (rules.size >= MaxRules || rules.any { it.id == rule.id }) return@edit
            val at = index.coerceIn(0, rules.size)
            prefs[RulesJson] = json.encodeToString(rules.take(at) + rule + rules.drop(at))
        }
    }

    private fun decode(prefs: Preferences): List<NotificationRule> =
        prefs[RulesJson]
            ?.let { runCatching { json.decodeFromString<List<NotificationRule>>(it) }.getOrNull() }
            ?: emptyList()

    companion object {
        private val RulesJson = stringPreferencesKey("rules_json")
        private val NextRuleId = longPreferencesKey("next_rule_id")

        /** New rules start as the umbrella example — a working rule to edit beats
         * an empty skeleton to fill in. */
        fun template(id: Long) = NotificationRule(
            id = id,
            name = "rule_$id",
            enabled = true,
            conditions = listOf(
                RuleCondition("next_6h.precip_chance_max", RuleOp.GTE, 60.0)
            ),
            message = "Take an umbrella — {trigger.value}% rain at {trigger.time}"
        )

        fun create(context: Context, json: Json) = RuleStore(context.rulesDataStore, json)
    }
}
