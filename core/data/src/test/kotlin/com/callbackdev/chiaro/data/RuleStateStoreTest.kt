package com.callbackdev.chiaro.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.callbackdev.chiaro.domain.rules.NotificationRule
import com.callbackdev.chiaro.domain.rules.RuleCondition
import com.callbackdev.chiaro.domain.rules.RuleOp
import com.callbackdev.chiaro.domain.rules.RuleTrigger
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class RuleStateStoreTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private fun store(file: File = tmp.newFile("state.preferences_pb")) = RuleStateStore(
        PreferenceDataStoreFactory.create(scope = scope) { file }
    )

    @After
    fun tearDown() {
        scope.cancel()
    }

    private fun rule(id: Long) = NotificationRule(
        id = id,
        name = "rule_$id",
        enabled = true,
        conditions = listOf(RuleCondition("current.temp_c", RuleOp.GT, 0.0)),
        message = "msg"
    )

    private fun latchTrigger(ruleId: Long, key: String) =
        RuleTrigger(rule(ruleId), fingerprint = null, latchKey = key, value = 1.0, at = null)

    private fun fingerprintTrigger(ruleId: Long, fingerprint: String) =
        RuleTrigger(rule(ruleId), fingerprint = fingerprint, latchKey = null, value = 1.0, at = null)

    @Test
    fun `record stores latches and fingerprints, unlatch re-arms`() = runBlocking {
        val store = store()
        store.record(latchTrigger(1, "milan:1"))
        store.record(fingerprintTrigger(2, "milan:rule:2:2026-08-17:AM"))
        var state = store.state.first()
        assertEquals(setOf("milan:1"), state.latched)
        assertEquals(setOf("milan:rule:2:2026-08-17:AM"), state.firedFingerprints)

        store.unlatch(setOf("milan:1"))
        state = store.state.first()
        assertTrue(state.latched.isEmpty())
        // fingerprints are untouched by unlatching
        assertEquals(setOf("milan:rule:2:2026-08-17:AM"), state.firedFingerprints)
    }

    @Test
    fun `clearRule drops only that rule's latches and fingerprints`() = runBlocking {
        val store = store()
        store.record(latchTrigger(1, "milan:1"))
        store.record(latchTrigger(12, "milan:12"))
        store.record(fingerprintTrigger(1, "milan:rule:1:2026-08-17:AM"))
        store.record(fingerprintTrigger(12, "milan:rule:12:2026-08-17:AM"))

        store.clearRule(1)
        val state = store.state.first()
        // rule 12 must survive rule 1's cleanup (suffix/substring matching)
        assertEquals(setOf("milan:12"), state.latched)
        assertEquals(setOf("milan:rule:12:2026-08-17:AM"), state.firedFingerprints)
    }

    /** A latch is cleared only by its rule reading false for its place: a removed place
     * or a deleted rule would keep one for ever, so the sync's housekeeping retains. */
    @Test
    fun `retain keeps the latches of live places and live rules only`() = runBlocking {
        val store = store()
        store.record(latchTrigger(1, "3173435:1"))  // live place, live rule
        store.record(latchTrigger(1, "gps:1"))      // the position, live rule
        store.record(latchTrigger(1, "3165524:1"))  // a place removed
        store.record(latchTrigger(7, "3173435:7"))  // a rule deleted
        store.record(fingerprintTrigger(7, "3173435:rule:7:2026-10-14:DAY"))

        store.retain(liveCities = setOf("3173435", "gps"), liveRules = setOf(1L))

        val state = store.state.first()
        assertEquals(setOf("3173435:1", "gps:1"), state.latched)
        // The fingerprints are a bounded ring and are left to age out.
        assertEquals(setOf("3173435:rule:7:2026-10-14:DAY"), state.firedFingerprints)
    }

    @Test
    fun `fingerprints are bounded, oldest fall off`() = runBlocking {
        val store = store()
        repeat(40) { i ->
            store.record(fingerprintTrigger(1, "milan:rule:1:2026-08-${(i % 28) + 1}:$i"))
        }
        assertTrue(store.state.first().firedFingerprints.size <= 32)
    }
}
