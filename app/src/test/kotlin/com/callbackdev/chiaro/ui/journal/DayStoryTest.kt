package com.callbackdev.chiaro.ui.journal

import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The story a revision opens (review of the Journal, A.6 and the first form of C.2): the
 * strip's row read down, one step for each column that moved a number, and nothing that
 * the strip did not record.
 */
class DayStoryTest {

    private val saturday = LocalDate.of(2026, 10, 17)
    private val sunday = saturday.plusDays(1)
    private fun at(hour: Int): Instant = Instant.parse("2026-10-14T00:00:00Z").plusSeconds(hour * 3600L)

    private fun drift(rain: List<Int?>, high: List<Double?>, columns: List<Instant?>) = DriftModel(
        dates = listOf(saturday, sunday),
        columns = columns,
        rain = listOf(rain, rain.map { 0 }),
        highC = listOf(high, high.map { 20.0 }),
        frostC = listOf(null, null),
        columnHours = 6
    )

    @Test
    fun `only the columns that moved a number are steps, the first always`() {
        val story = dayStory(
            drift(
                rain = listOf(52, 52, 30, 30),
                high = listOf(18.0, 18.0, 18.0, 21.0),
                columns = listOf(at(0), at(6), at(12), at(18))
            ),
            saturday
        )
        assertEquals(listOf(at(0), at(12), at(18)), story.map { it.at })
        assertEquals(StoryStep(at(0), 52, 18.0, rainMoved = false, highMoved = false), story[0])
        assertEquals(StoryStep(at(12), 30, 18.0, rainMoved = true, highMoved = false), story[1])
        assertEquals(StoryStep(at(18), 30, 21.0, rainMoved = false, highMoved = true), story[2])
    }

    @Test
    fun `a slot no fetch landed in is not a forecast that held`() {
        val story = dayStory(
            drift(
                rain = listOf(40, null, 40),
                high = listOf(19.0, null, 19.0),
                columns = listOf(at(0), null, at(12))
            ),
            saturday
        )
        assertEquals(listOf(at(0)), story.map { it.at })
    }

    @Test
    fun `a day the strip does not hold has no story`() {
        val story = dayStory(
            drift(rain = listOf(40, 50), high = listOf(19.0, 19.0), columns = listOf(at(0), at(6))),
            saturday.minusDays(3)
        )
        assertTrue(story.isEmpty())
    }
}
