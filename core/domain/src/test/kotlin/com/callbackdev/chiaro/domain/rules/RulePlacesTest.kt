package com.callbackdev.chiaro.domain.rules

import com.callbackdev.chiaro.domain.model.City
import com.callbackdev.chiaro.domain.model.Coordinates
import com.callbackdev.chiaro.domain.model.GpsCityId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Where each rule is read (15.11, 29 Oct 2026): the rules without a place and those bound to
 * the active place on the active place's forecast, each other saved place's on its own, and a
 * rule whose place is gone nowhere.
 */
class RulePlacesTest {

    private fun city(id: Long, name: String) =
        City(id, name, null, "Italia", Coordinates(45.0 + id / 1000.0, 9.0), "Europe/Rome")

    private val milano = city(3173435, "Milano")
    private val bormio = city(3181554, "Bormio")
    private val torino = city(3165524, "Torino")
    private val position = city(GpsCityId, "Segrate")
    private val saved = listOf(milano, bormio, torino)

    private fun rule(id: Long, place: City? = null) = NotificationRule(
        id = id, name = "r$id",
        conditions = listOf(RuleCondition("current.temp_c", RuleOp.GT, 0.0)),
        message = "m",
        place = place?.let { RulePlace(it.id, it.name) }
    )

    @Test
    fun `a rule with no place, or bound to the active one, is read on the active place`() {
        assertTrue(RulePlaces.readsActive(rule(1), milano))
        assertTrue(RulePlaces.readsActive(rule(2, milano), milano))
        assertFalse(RulePlaces.readsActive(rule(3, bormio), milano))
        // The position is the active place by definition, and never a rule's own.
        assertTrue(RulePlaces.readsActive(rule(1), position))
        assertFalse(RulePlaces.readsActive(rule(2, milano), position))
    }

    @Test
    fun `the other places are the saved ones the rules are bound to, in the list's order`() {
        val rules = listOf(rule(1), rule(2, torino), rule(3, milano), rule(4, bormio), rule(5, torino))
        val elsewhere = RulePlaces.elsewhere(rules, active = milano, saved = saved)
        assertEquals(listOf(bormio, torino), elsewhere.map { it.first })
        assertEquals(listOf(listOf(4L), listOf(2L, 5L)), elsewhere.map { (_, r) -> r.map { it.id } })
    }

    @Test
    fun `with the position active, every bound rule is read on its own place`() {
        val rules = listOf(rule(1), rule(2, milano), rule(3, bormio))
        val elsewhere = RulePlaces.elsewhere(rules, active = position, saved = saved)
        assertEquals(listOf(milano, bormio), elsewhere.map { it.first })
    }

    /** No active place (15.13, 29 Oct 2026: the position with no fix yet, or nothing chosen):
     * every bound rule is read on its own place, and a rule with no place of its own nowhere. */
    @Test
    fun `with no active place, every bound rule is read on its own place and the rest nowhere`() {
        val rules = listOf(rule(1), rule(2, milano), rule(3, bormio))
        val elsewhere = RulePlaces.elsewhere(rules, active = null, saved = saved)
        assertEquals(listOf(milano, bormio), elsewhere.map { it.first })
        assertEquals(listOf(listOf(2L), listOf(3L)), elsewhere.map { (_, r) -> r.map { it.id } })
    }

    @Test
    fun `a rule whose place is gone is an orphan, and read nowhere`() {
        val gone = rule(6, city(3178229, "Courmayeur"))
        val savedIds = saved.map { it.id }.toSet()
        assertTrue(RulePlaces.isOrphan(gone, savedIds))
        assertFalse(RulePlaces.isOrphan(rule(1), savedIds))
        assertFalse(RulePlaces.isOrphan(rule(4, bormio), savedIds))
        assertFalse(RulePlaces.readsActive(gone, milano))
        assertTrue(RulePlaces.elsewhere(listOf(gone), milano, saved).isEmpty())
        // The last place removed: every bound rule is an orphan.
        assertTrue(RulePlaces.isOrphan(rule(4, bormio), emptySet()))
    }
}
