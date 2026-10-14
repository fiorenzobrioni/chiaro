package com.callbackdev.chiaro.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.callbackdev.chiaro.domain.rules.RuleEngineState
import com.callbackdev.chiaro.domain.rules.RuleTrigger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val Context.ruleStateDataStore by preferencesDataStore(name = "rule_state")

/**
 * RuleEngine bookkeeping (Fase 11), alongside [AlertStateStore] and separate from
 * [SettingsStore] for the same reason: it must not bump the user-visible
 * `// Last modified:` header. Latches persist as a joined set, fingerprints as a
 * newest-first bounded list.
 */
class RuleStateStore(private val dataStore: DataStore<Preferences>) {

    val state: Flow<RuleEngineState> = dataStore.data
        .map { prefs ->
            RuleEngineState(
                latched = prefs[Latched].toEntries(),
                firedFingerprints = prefs[Fingerprints].toEntries()
            )
        }
        .distinctUntilChanged()

    /** Called only after a successful notify — an unposted trigger can retry later. */
    suspend fun record(trigger: RuleTrigger) {
        dataStore.edit { prefs ->
            trigger.latchKey?.let { key ->
                prefs[Latched] = (prefs[Latched].toEntries() + key).joinToString(SEPARATOR)
            }
            trigger.fingerprint?.let { fingerprint ->
                prefs[Fingerprints] = (listOf(fingerprint) + prefs[Fingerprints].toEntries())
                    .distinct()
                    .take(MAX_FINGERPRINTS)
                    .joinToString(SEPARATOR)
            }
        }
    }

    /** Re-arms edge-triggered rules whose condition reads false again. */
    suspend fun unlatch(keys: Set<String>) {
        if (keys.isEmpty()) return
        dataStore.edit { prefs ->
            prefs[Latched] = (prefs[Latched].toEntries() - keys).joinToString(SEPARATOR)
        }
    }

    /**
     * Editing a rule's conditions or its window makes its state stale (a still-latched
     * rule with a new threshold would stay silent); Alerts clears it on every such edit.
     * A deletion is left to [retain], because «Annulla» brings the rule back as it was.
     * Latch keys are `cityKey:ruleId`, fingerprints embed `:rule:<id>:`.
     */
    suspend fun clearRule(ruleId: Long) {
        dataStore.edit { prefs ->
            prefs[Latched] = prefs[Latched].toEntries()
                .filterNot { it.endsWith(":$ruleId") }
                .joinToString(SEPARATOR)
            prefs[Fingerprints] = prefs[Fingerprints].toEntries()
                .filterNot { it.contains(":rule:$ruleId:") }
                .joinToString(SEPARATOR)
        }
    }

    /**
     * Keeps only the latches of a place the app still follows AND a rule that still
     * exists (14 ott 2026). A latch is `<place>:<rule id>` and is cleared only when its
     * rule, evaluated for its place, reads false again — so the latch of a removed place
     * or of a rule deleted before [clearRule] was wired is never evaluated and never
     * cleared. Fingerprints need no such care: they are a bounded ring.
     *
     * @param liveCities the places' notification keys (`"gps"` or the id)
     */
    suspend fun retain(liveCities: Set<String>, liveRules: Set<Long>) {
        dataStore.edit { prefs ->
            val latched = prefs[Latched].toEntries()
            val kept = latched.filter { key ->
                val place = key.substringBeforeLast(':')
                val rule = key.substringAfterLast(':').toLongOrNull()
                place in liveCities && rule in liveRules
            }
            if (kept.size != latched.size) prefs[Latched] = kept.joinToString(SEPARATOR)
        }
    }

    private fun String?.toEntries(): Set<String> =
        this?.split(SEPARATOR)?.filter { it.isNotBlank() }?.toSet() ?: emptySet()

    companion object {
        private val Latched = stringPreferencesKey("rule_latched")
        private val Fingerprints = stringPreferencesKey("rule_fingerprints")

        /** 10 rules × a few half-day buckets before the dated entries go stale. */
        private const val MAX_FINGERPRINTS = 32

        /** Never appears in latch keys or fingerprints. */
        private const val SEPARATOR = "\n"

        fun create(context: Context) = RuleStateStore(context.ruleStateDataStore)
    }
}
