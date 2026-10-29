package com.callbackdev.chiaro.ui.alerts

import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.domain.rules.RuleVariables
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * «Cosa guardare» in five groups (15.7, 28 Oct 2026; «Domani» since 15.9, 29 Oct 2026): every
 * variable of the registry in one of them and one only, now first and tomorrow last, each group
 * in the registry's own order. A variable the registry gains with a sixth prefix fails here
 * before it ships in no group at all.
 */
class VariableGroupsTest {

    @Test
    fun `the groups hold every variable once, in the registry's order`() {
        val grouped = RuleText.variableGroups.flatMap { it.ids }
        assertEquals(RuleVariables.all.map { it.id }.sorted(), grouped.sorted())
        assertEquals(grouped.size, grouped.toSet().size)
        RuleText.variableGroups.forEach { group ->
            val registry = RuleVariables.all.map { it.id }.filter { it in group.ids }
            assertEquals(registry, group.ids)
        }
    }

    @Test
    fun `now, the next six hours, the next twelve, today, tomorrow`() {
        assertEquals(
            listOf(
                R.string.rule_var_group_now, R.string.rule_var_group_next6,
                R.string.rule_var_group_next12, R.string.rule_var_group_today,
                R.string.rule_var_group_tomorrow
            ),
            RuleText.variableGroups.map { it.titleRes }
        )
        assertEquals("current.temp_c", RuleText.variableGroups[0].ids.first())
        assertEquals("today.high_c", RuleText.variableGroups[3].ids.first())
    }

    /** Today and tomorrow ask the same seven things, line for line (15.9). */
    @Test
    fun `tomorrow's group reads like today's`() {
        val today = RuleText.variableGroups[3].ids
        val tomorrow = RuleText.variableGroups[4].ids
        assertEquals(7, tomorrow.size)
        assertEquals(today.map { it.replace("today.", "tomorrow.") }, tomorrow)
    }
}
