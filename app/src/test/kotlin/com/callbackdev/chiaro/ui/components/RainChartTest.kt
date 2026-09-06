package com.callbackdev.chiaro.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The two rules of DESIGN §8.3b that are arithmetic rather than paint: where the line
 * breaks, and which hours the axis is allowed to name. Both were bugs waiting to
 * happen — a gap drawn as a zero is a forecast the app invented, and a last label that
 * lands on top of the one before it is worse than no last label at all.
 */
class RainChartTest {

    private fun hours(vararg pct: Int?) =
        pct.mapIndexed { i, p -> RainHour(label = "%02d".format(i), pct = p) }

    @Test
    fun `a full day is one run`() {
        assertEquals(listOf((0..23).toList()), rainRuns(hours(*Array(24) { 50 })))
    }

    @Test
    fun `an hour with no forecast ends the run it is in`() {
        assertEquals(
            listOf(listOf(0, 1), listOf(4, 5)),
            rainRuns(hours(10, 20, null, null, 30, 40))
        )
    }

    @Test
    fun `a lone hour between two gaps is a run of one, which draws a dot and no line`() {
        assertEquals(listOf(listOf(2)), rainRuns(hours(null, null, 70, null)))
    }

    @Test
    fun `nothing forecast at all draws nothing`() {
        assertEquals(emptyList<List<Int>>(), rainRuns(hours(null, null)))
    }

    @Test
    fun `the axis names every sixth hour and the last one`() {
        // 24 hours over a 300px plot: 13px an hour, labels 16px wide — room to spare.
        assertEquals(
            listOf(0, 6, 12, 18, 23),
            axisTicks(count = 24, every = 6, stepPx = 13f, labelWidthPx = { 16f }, gapPx = 6f)
        )
    }

    @Test
    fun `the last hour is dropped when its label would land on the one before it`() {
        // 20 hours: the last is two steps past 18, and two steps is not enough room.
        assertEquals(
            listOf(0, 6, 12, 18),
            axisTicks(count = 20, every = 6, stepPx = 8f, labelWidthPx = { 30f }, gapPx = 6f)
        )
    }

    @Test
    fun `the last hour is never named twice`() {
        assertEquals(
            listOf(0, 6, 12, 18),
            axisTicks(count = 19, every = 6, stepPx = 13f, labelWidthPx = { 16f }, gapPx = 6f)
        )
    }

    @Test
    fun `a strip of one hour names that hour, and a strip of none names nothing`() {
        assertEquals(
            listOf(0),
            axisTicks(count = 1, every = 6, stepPx = 0f, labelWidthPx = { 16f }, gapPx = 6f)
        )
        assertEquals(
            emptyList<Int>(),
            axisTicks(count = 0, every = 6, stepPx = 13f, labelWidthPx = { 16f }, gapPx = 6f)
        )
    }
}
