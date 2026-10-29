package com.callbackdev.chiaro.domain.rules

import com.callbackdev.chiaro.domain.model.City

/**
 * Where each rule is read (15.11, 29 Oct 2026). A rule with no place of its own follows the
 * active place, whichever it is; a rule bound to a saved place ([NotificationRule.place]) is read
 * on that place's forecast and clock, whatever the active place is; a rule bound to a place the
 * reader has removed is read nowhere, and is switched off rather than moved to another place in
 * silence (the decisions of Fase 15, archive 08). The engine does not change: the place is
 * already in its keys (`cityKey`), so a bound rule's latch and fingerprints are its place's.
 */
object RulePlaces {

    /** Whether [rule] watches a saved place the reader no longer has. [savedIds]: the ids of
     * the saved list, never the position's. */
    fun isOrphan(rule: NotificationRule, savedIds: Set<Long>): Boolean =
        rule.place != null && rule.place.id !in savedIds

    /** Whether [rule] is read on [active]'s forecast: it has no place of its own, or its place
     * is the active one. */
    fun readsActive(rule: NotificationRule, active: City): Boolean =
        rule.place == null || rule.place.id == active.id

    /**
     * The saved places [rules] are bound to other than [active], each with its rules, in the
     * order of the saved list: the places a run reads after the active one. A rule whose place
     * is not in [saved] is in none of them.
     */
    fun elsewhere(
        rules: List<NotificationRule>,
        active: City,
        saved: List<City>
    ): List<Pair<City, List<NotificationRule>>> =
        saved.filter { it.id != active.id }
            .map { place -> place to rules.filter { it.place?.id == place.id } }
            .filter { (_, bound) -> bound.isNotEmpty() }
}
