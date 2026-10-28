package com.callbackdev.chiaro.data

import com.callbackdev.chiaro.domain.rules.NotificationRule
import com.callbackdev.chiaro.domain.rules.RuleCondition
import com.callbackdev.chiaro.domain.rules.RuleOp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * What a history commit says about a rule that fired (15.4, 28 Oct 2026): `id:nome`, read by
 * id; the rows written before, the name alone, read by name until retention takes them.
 */
class FiredRuleTest {

    private fun rule(id: Long, name: String) = NotificationRule(
        id = id, name = name, enabled = true,
        conditions = listOf(RuleCondition("current.temp_c", RuleOp.GT, 0.0)), message = "m"
    )

    @Test
    fun `a rule is written as its id and its name, and read back`() {
        val bici = rule(7, "Bici")
        assertEquals("7:Bici", FiredRule.of(bici).encode())
        assertEquals(FiredRule(7, "Bici"), FiredRule.decode("7:Bici"))
    }

    @Test
    fun `a name with a colon of its own survives the round trip`() {
        val encoded = FiredRule.of(rule(3, "Bici: mattina")).encode()
        assertEquals("3:Bici: mattina", encoded)
        assertEquals(FiredRule(3, "Bici: mattina"), FiredRule.decode(encoded))
    }

    @Test
    fun `a new row is the rule's by id, whatever it is called now`() {
        val row = FiredRule.decode("7:Bici")
        assertTrue(row.isFor(rule(7, "Pedalata")))
        assertFalse(row.isFor(rule(8, "Bici")))
    }

    @Test
    fun `a row written before carries the name alone and is read by it`() {
        val row = FiredRule.decode("Bici")
        assertEquals(FiredRule(null, "Bici"), row)
        assertTrue(row.isFor(rule(7, "Bici")))
        assertFalse(row.isFor(rule(7, "Pedalata")))
    }
}
