package com.callbackdev.chiaro.ui.journal

import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The week's hero (review of the Journal, 14 ott 2026) says one day and one change: the
 * day that moved most by the diff engine's own thresholds, else the day that wandered and
 * came back, else a week that held. The words are the screen's; this is what they say.
 */
class DriftStoryTest {

    private val sat = LocalDate.of(2026, 10, 17)
    private val sun = LocalDate.of(2026, 10, 18)

    private fun drift(rain: List<List<Int?>>, high: List<List<Double?>> = rain.map { row -> row.map { 20.0 } }) =
        DriftModel(
            dates = listOf(sat, sun),
            columns = List(rain.first().size) { Instant.ofEpochSecond(it * 21_600L) },
            rain = rain,
            highC = high,
            frostC = listOf(null, null),
            columnHours = 6
        )

    @Test
    fun `less rain is the better day, and the larger move wins`() {
        val story = driftStory(drift(listOf(listOf(70, 50, 30), listOf(20, 25, 35))), DriftMetric.RAIN)
        assertEquals(DriftStory(DriftStory.Kind.BETTER, sat, 70.0, 30.0), story)
    }

    @Test
    fun `more rain is the worse day`() {
        val story = driftStory(drift(listOf(listOf(10, 10, 12), listOf(20, null, 45))), DriftMetric.RAIN)
        assertEquals(DriftStory(DriftStory.Kind.WORSE, sun, 20.0, 45.0), story)
    }

    @Test
    fun `a day that moved and came back is a swing, said with its two ends`() {
        val story = driftStory(drift(listOf(listOf(20, 60, 22), listOf(10, 10, 10))), DriftMetric.RAIN)
        assertEquals(DriftStory(DriftStory.Kind.SWING, sat, 20.0, 60.0), story)
    }

    @Test
    fun `under the engine's threshold the week held, and names no day`() {
        val story = driftStory(drift(listOf(listOf(20, 25, 29), listOf(10, 11, 12))), DriftMetric.RAIN)
        assertEquals(DriftStory.Kind.QUIET, story.kind)
        assertNull(story.date)
    }

    @Test
    fun `the high is warmer or cooler, never better or worse`() {
        val rain = listOf(listOf(10, 10, 10), listOf(10, 10, 10))
        val warmer = driftStory(drift(rain, listOf(listOf(19.0, 20.0, 22.0), listOf(18.0, 18.0, 18.0))), DriftMetric.HIGH)
        assertEquals(DriftStory(DriftStory.Kind.WARMER, sat, 19.0, 22.0), warmer)
        val cooler = driftStory(drift(rain, listOf(listOf(19.0, 19.0, 19.0), listOf(21.0, 20.0, 18.5))), DriftMetric.HIGH)
        assertEquals(DriftStory(DriftStory.Kind.COOLER, sun, 21.0, 18.5), cooler)
    }

    @Test
    fun `a row with one prediction has nothing to compare`() {
        val story = driftStory(drift(listOf(listOf(null, null, 90), listOf(null, 5, null))), DriftMetric.RAIN)
        assertEquals(DriftStory.Kind.QUIET, story.kind)
    }
}
