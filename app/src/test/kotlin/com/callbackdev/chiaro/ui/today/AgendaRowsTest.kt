package com.callbackdev.chiaro.ui.today

import androidx.compose.ui.geometry.Offset
import com.callbackdev.chiaro.domain.rules.NotificationRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

/**
 * The day's list as rows (review of Today, 9 nov 2026): the rules that hold now are one row,
 * everything else is its own; and where the wind goes on its dial.
 */
class AgendaRowsTest {

    private val noon: LocalDateTime = LocalDateTime.of(2026, 9, 25, 12, 15)

    private fun rule(name: String) = AgendaRule(
        rule = NotificationRule(id = name.hashCode().toLong(), name = name, conditions = emptyList(), message = "$name: go"),
        value = 0.0,
        at = null
    )

    private fun alert(name: String, now: Boolean, at: LocalDateTime = noon) =
        TimelineItem(at = at, kind = TimelineKind.ALERT, rule = rule(name), now = now)

    private fun moment(kind: TimelineKind, hour: Int) = TimelineItem(at = noon.withHour(hour), kind = kind)

    @Test
    fun `two rules that hold now are one row, and the moments keep theirs`() {
        val timeline = listOf(
            alert("Bike", now = true),
            alert("Run", now = true),
            moment(TimelineKind.GOLDEN_EVENING, 18),
            moment(TimelineKind.BLUE_EVENING, 19)
        )
        val rows = agendaRows(timeline)
        assertEquals(3, rows.size)
        assertSame(timeline[0], rows[0].item)
        assertEquals(listOf(timeline[1]), rows[0].alsoNow)
        assertEquals(0, rows[0].index)
        // The moments keep their place in the timeline, which is what «the next» is found by.
        assertSame(timeline[2], rows[1].item)
        assertEquals(2, rows[1].index)
        assertEquals(3, rows[2].index)
        assertTrue(rows[1].alsoNow.isEmpty() && rows[2].alsoNow.isEmpty())
    }

    @Test
    fun `one rule alone keeps its own row and its message`() {
        val timeline = listOf(alert("Bike", now = true), moment(TimelineKind.GOLDEN_EVENING, 18))
        val rows = agendaRows(timeline)
        assertEquals(2, rows.size)
        assertTrue(rows.all { it.alsoNow.isEmpty() })
        assertSame(timeline[0], rows[0].item)
    }

    @Test
    fun `a rule that holds at an hour of its own is a moment of the day`() {
        val timeline = listOf(
            alert("Bike", now = true),
            alert("Run", now = true),
            moment(TimelineKind.GOLDEN_EVENING, 18),
            alert("Laundry", now = false, at = noon.withHour(20))
        )
        val rows = agendaRows(timeline)
        assertEquals(3, rows.size)
        assertEquals(listOf("Run"), rows[0].alsoNow.map { it.rule!!.rule.name })
        assertSame(timeline[3], rows[2].item)
        assertTrue(rows[2].alsoNow.isEmpty())
        // No rule at all: the timeline, row for row.
        val plain = listOf(moment(TimelineKind.GOLDEN_EVENING, 18), moment(TimelineKind.DARK, 21))
        assertEquals(plain, agendaRows(plain).map { it.item })
    }

    /** A forecast carries where the wind comes FROM; the dial's arrow flies where it goes. */
    @Test
    fun `the wind's arrow flies downwind, with north up`() {
        fun near(expected: Offset, actual: Offset) {
            assertEquals(expected.x, actual.x, 1e-5f)
            assertEquals(expected.y, actual.y, 1e-5f)
        }
        near(Offset(0f, 1f), windVector(0))      // from the north: down the dial
        near(Offset(-1f, 0f), windVector(90))    // from the east: to the left
        near(Offset(0f, -1f), windVector(180))   // from the south: up
        near(Offset(1f, 0f), windVector(270))    // from the west: to the right
        near(windVector(0), windVector(360))
    }
}
