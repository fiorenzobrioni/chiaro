package com.callbackdev.chiaro.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.callbackdev.chiaro.domain.rules.MaxRules
import com.callbackdev.chiaro.domain.rules.RuleOp
import com.callbackdev.chiaro.domain.rules.RulePlace
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class RuleStoreTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private fun store(file: File = tmp.newFile("rules.preferences_pb")) = RuleStore(
        PreferenceDataStoreFactory.create(scope = scope) { file },
        Json
    )

    @After
    fun tearDown() {
        scope.cancel()
    }

    /** The Chiaro-side add (Fase 6): content chosen by the caller, ids still
     * monotonic, ceiling still enforced. */
    @Test
    fun `parameterized add creates the given rule and returns it`() = runBlocking {
        val store = store()
        val created = store.add(
            name = "bici",
            conditions = listOf(
                com.callbackdev.chiaro.domain.rules.RuleCondition(
                    "current.temp_c", RuleOp.GTE, 12.0
                )
            ),
            message = "Si pedala"
        )
        assertEquals(created, store.rules.first().single())
        assertTrue(created!!.enabled)
        // The counter is shared with the plain add: no id is ever reused.
        store.add()
        assertEquals(listOf(1L, 2L), store.rules.first().map { it.id })
    }

    @Test
    fun `parameterized add refuses past the ceiling`() = runBlocking {
        val store = store()
        repeat(MaxRules) { store.add() }
        assertEquals(null, store.add(name = "extra", conditions = emptyList(), message = "no"))
        assertEquals(MaxRules, store.rules.first().size)
    }

    @Test
    fun `starts empty and add appends the umbrella template`() = runBlocking {
        val store = store()
        assertTrue(store.rules.first().isEmpty())
        store.add()
        val rule = store.rules.first().single()
        assertEquals("rule_1", rule.name)
        assertTrue(rule.enabled)
        val condition = rule.conditions.single()
        assertEquals("next_6h.precip_chance_max", condition.variable)
        assertEquals(RuleOp.GTE, condition.op)
        assertEquals(60.0, condition.threshold, 0.0)
    }

    @Test
    fun `ids are monotonic and never recycled`() = runBlocking {
        val store = store()
        store.add() // id 1
        store.add() // id 2
        store.remove(2)
        store.add() // must be id 3, not a recycled 2
        assertEquals(listOf(1L, 3L), store.rules.first().map { it.id })
    }

    @Test
    fun `update replaces only the matching rule`() = runBlocking {
        val store = store()
        store.add()
        store.add()
        val second = store.rules.first()[1]
        store.update(second.copy(name = "sunscreen", enabled = false))
        val rules = store.rules.first()
        assertEquals("rule_1", rules[0].name)
        assertEquals("sunscreen", rules[1].name)
        assertTrue(!rules[1].enabled)
    }

    /** «Annulla» after a deletion (the Alerts review, point 10): the rule comes back as it
     * was, in its place and with its own id, and the next new rule still gets a fresh one. */
    @Test
    fun `restore puts a removed rule back where it was, with its id`() = runBlocking {
        val store = store()
        repeat(3) { store.add() }
        val middle = store.rules.first()[1].copy(name = "bici", enabled = false)
        store.update(middle)
        store.remove(middle.id)
        store.restore(middle, index = 1)
        assertEquals(listOf(1L, 2L, 3L), store.rules.first().map { it.id })
        assertEquals(middle, store.rules.first()[1])
        store.add()
        assertEquals(4L, store.rules.first().last().id)
    }

    @Test
    fun `restore never doubles a rule nor passes the ceiling`() = runBlocking {
        val store = store()
        store.add()
        val rule = store.rules.first().single()
        store.restore(rule, index = 0)
        assertEquals(1, store.rules.first().size)
        store.remove(rule.id)
        repeat(MaxRules) { store.add() }
        store.restore(rule, index = 0)
        assertEquals(MaxRules, store.rules.first().size)
        assertTrue(store.rules.first().none { it.id == rule.id })
    }

    @Test
    fun `add is a no-op at the ceiling`() = runBlocking {
        val store = store()
        repeat(MaxRules + 2) { store.add() }
        assertEquals(MaxRules, store.rules.first().size)
    }

    @Test
    fun `rules survive a restart (new store on the same file)`() = runBlocking {
        val file = tmp.newFile("persist.preferences_pb")
        val firstRunJob = SupervisorJob()
        val firstRun = CoroutineScope(Dispatchers.IO + firstRunJob)
        RuleStore(PreferenceDataStoreFactory.create(scope = firstRun) { file }, Json).add()
        firstRunJob.cancelAndJoin()
        val reopened = store(file)
        assertEquals("rule_1", reopened.rules.first().single().name)
    }

    /** A rule saved before 15.11 has no place: it reads as null, the active place, which is
     * what it always watched. Written by hand, as an older build wrote it. */
    @Test
    fun `a rule saved without a place reads as bound to none`() = runBlocking {
        val file = tmp.newFile("old.preferences_pb")
        val job = SupervisorJob()
        val dataStore = PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO + job)) { file }
        dataStore.edit {
            it[stringPreferencesKey("rules_json")] =
                """[{"id":4,"name":"Bici","conditions":[{"variable":"current.temp_c","op":"GTE","threshold":12.0}],"message":"Si pedala"}]"""
        }
        job.cancelAndJoin()
        val rule = store(file).rules.first().single()
        assertEquals("Bici", rule.name)
        assertNull(rule.place)
        assertTrue(rule.enabled)
    }

    @Test
    fun `a rule's place survives a restart`() = runBlocking {
        val file = tmp.newFile("bound.preferences_pb")
        val job = SupervisorJob()
        val first = RuleStore(PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO + job)) { file }, Json)
        val created = checkNotNull(first.add("Neve", emptyList(), "m"))
        first.update(created.copy(place = RulePlace(3181554, "Bormio")))
        job.cancelAndJoin()
        assertEquals(RulePlace(3181554, "Bormio"), store(file).rules.first().single().place)
    }

    /** A place removed (15.11): its rules go off and keep the place, so the card can name it
     * and a place brought back finds them bound to it again; nothing else is touched. */
    @Test
    fun `switching off the orphans keeps their place and touches nothing else`() = runBlocking {
        val store = store()
        val free = checkNotNull(store.add("Ovunque", emptyList(), "m"))
        val milano = checkNotNull(store.add("Milano", emptyList(), "m"))
            .copy(place = RulePlace(3173435, "Milano"))
        val bormio = checkNotNull(store.add("Bormio", emptyList(), "m"))
            .copy(place = RulePlace(3181554, "Bormio"))
        val resting = checkNotNull(store.add("Bormio spenta", emptyList(), "m"))
            .copy(place = RulePlace(3181554, "Bormio"), enabled = false)
        listOf(milano, bormio, resting).forEach { store.update(it) }

        val switchedOff = store.switchOffOrphans(savedIds = setOf(3173435L))

        // The one that was on, and only it: a rule already off is not «switched off» again.
        assertEquals(listOf(bormio.id), switchedOff.map { it.id })
        val rules = store.rules.first()
        assertEquals(listOf(free, milano, bormio.copy(enabled = false), resting), rules)
        // Nothing left to do: no write, nothing returned.
        assertTrue(store.switchOffOrphans(savedIds = setOf(3173435L)).isEmpty())
        assertEquals(rules, store.rules.first())
    }
}
