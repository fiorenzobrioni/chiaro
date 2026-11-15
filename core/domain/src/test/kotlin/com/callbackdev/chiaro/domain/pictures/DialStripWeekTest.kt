package com.callbackdev.chiaro.domain.pictures

import java.time.Duration
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The dial, the 4×1's strip, the 4×4's range bars and the rows under the plate, against
 * the canvas's own numbers for its Milan (`CanvasMilan`). */
class DialStripWeekTest {

    @Test
    fun `the large dial puts now and the rain where the canvas does`() {
        val d = dialDrawing(DialSpec.LARGE, CanvasMilan.now, CanvasMilan.zone, CanvasMilan.hours)
        // Canvas `dial(124, 9, 14, 16, 5)`: the disc's box at (83.7, 5.2), so its centre at
        // (90.7, 12.2); 14:00 is 210° round from midnight at the bottom.
        assertEquals(90.7f, d.now.x, 0.06f)
        assertEquals(12.2f, d.now.y, 0.06f)
        assertEquals(210f, d.spentSweep, 0.001f)
        // Nine dots, 20:00 to 04:00, the first box at (99.3, 82.5) and alpha 0.61.
        val boxes = listOf(
            99.3f to 82.5f, 92.0f to 92.0f, 82.5f to 99.3f, 71.4f to 103.9f, 59.5f to 105.5f,
            47.6f to 103.9f, 36.5f to 99.3f, 27.0f to 92.0f, 19.7f to 82.5f
        )
        val alphas = listOf(0.61f, 0.78f, 0.86f, 0.89f, 0.83f, 0.78f, 0.70f, 0.61f, 0.56f)
        assertEquals(9, d.dots.size)
        d.dots.forEachIndexed { i, dot ->
            assertEquals(boxes[i].first + 2.5f, dot.x, 0.06f)
            assertEquals(boxes[i].second + 2.5f, dot.y, 0.06f)
            assertEquals(alphas[i], dot.alpha, 0.006f)
        }
    }

    @Test
    fun `the small dial is the canvas's 52`() {
        val d = dialDrawing(DialSpec.SMALL, CanvasMilan.now, CanvasMilan.zone, CanvasMilan.hours)
        // Canvas `dial(52, 6, 10, 11, 3)`: disc box (32.5, 1.1), first dot box (37.5, 32.0).
        assertEquals(32.5f + 5f, d.now.x, 0.06f)
        assertEquals(1.1f + 5f, d.now.y, 0.06f)
        assertEquals(37.5f + 1.5f, d.dots.first().x, 0.06f)
        assertEquals(32.0f + 1.5f, d.dots.first().y, 0.06f)
    }

    @Test
    fun `midnight is at the bottom, noon at the top, the morning on the left`() {
        val s = DialSpec.LARGE
        val r = s.ringRadius
        assertEquals(62f, dialPoint(s, r, 0.0).x, 0.001f)
        assertEquals(62f + r, dialPoint(s, r, 0.0).y, 0.001f)
        assertEquals(62f - r, dialPoint(s, r, 6.0).x, 0.001f)
        assertEquals(62f - r, dialPoint(s, r, 12.0).y, 0.001f)
        assertEquals(62f + r, dialPoint(s, r, 18.0).x, 0.001f)
    }

    @Test
    fun `the dial's rain is 24 hours, never the 25th on top of the first`() {
        val wet = CanvasMilan.hours.map { it.copy(chancePct = 90) }
        val d = dialDrawing(DialSpec.LARGE, CanvasMilan.now, CanvasMilan.zone, wet)
        assertEquals(24, d.dots.size)
        assertEquals(CanvasMilan.now, d.dots.first().at)
        assertEquals(CanvasMilan.now.plus(Duration.ofHours(23)), d.dots.last().at)
    }

    @Test
    fun `now on the dial is the clock, minutes included`() {
        val at = LocalDateTime.parse("2026-11-15T14:30").atZone(CanvasMilan.zone).toInstant()
        val d = dialDrawing(DialSpec.LARGE, at, CanvasMilan.zone, CanvasMilan.hours)
        assertEquals(217.5f, d.spentSweep, 0.001f)
        // 14:30 is still the hour of 14:00: its rain is on the dial.
        assertEquals(9, d.dots.size)
    }

    @Test
    fun `the strip marks twelve hours, not thirteen`() {
        val window = PictureWindow.next(CanvasMilan.now, CanvasMilan.zone, StripMetrics.HOURS)
        val s = stripDrawing(window, CanvasMilan.hours, 223f)
        // Canvas: marks at 111.5, 130.1 … 204.4 and a seventh at 223.0, which is past the
        // ribbon's end (the hour after the twelve); 18.6 wide.
        assertEquals(listOf(111.5f, 130.1f, 148.7f, 167.3f, 185.8f, 204.4f), s.marks.map { it.left }.map { r1(it) })
        s.marks.forEach { assertEquals(18.58f, it.width, 0.01f) }
        assertEquals(StripMetrics.DISC_MIN_X, s.nowX, 0f)
        assertEquals(listOf(0f, 55.75f, 111.5f, 167.25f, 223f), s.ticks.map { it.x })
        assertEquals(
            listOf(StripAnchor.START, StripAnchor.CENTER, StripAnchor.CENTER, StripAnchor.CENTER, StripAnchor.END),
            s.ticks.map { it.anchor }
        )
        assertTrue(s.ticks.first().isNow)
        assertEquals(listOf(17, 20, 23, 2), s.ticks.drop(1).map { it.at.atZone(CanvasMilan.zone).hour })
    }

    @Test
    fun `the week's bars are the canvas's on the fixed scale`() {
        // Canvas `day()`: Oggi 6–12 → left 31.4%, width 17.1%; Mar 3–10 → 22.9%, 20.0%.
        val today = weekRangeBar(6.0, 12.0)!!
        assertEquals(0.314f, today.start, 0.001f)
        assertEquals(0.171f, today.endInclusive - today.start, 0.001f)
        val tuesday = weekRangeBar(3.0, 10.0)!!
        assertEquals(0.229f, tuesday.start, 0.001f)
        assertEquals(0.2f, tuesday.endInclusive - tuesday.start, 0.001f)
    }

    @Test
    fun `a day past the scale is clipped to it, never stretched`() {
        assertEquals(0f..(25.0 / 35.0).toFloat(), weekRangeBar(-12.0, 20.0))
        assertEquals((40.0 / 35.0 - 1.0).let { 0.99f..1f }, weekRangeBar(33.0, 38.0))
        assertEquals(0f..0.01f, weekRangeBar(-14.0, -9.0))
        assertNull(weekRangeBar(null, 4.0))
    }

    @Test
    fun `later rows name the plate's own ticks`() {
        val window = PictureWindow.next(CanvasMilan.now, CanvasMilan.zone)
        val rows = strideHours(window, CanvasMilan.hours, 6)
        assertEquals(listOf(17, 20, 23, 2, 5, 8), rows.map { it.at.atZone(CanvasMilan.zone).hour })
        // «La giornata» starts at midnight; the rows still start after now.
        val day = PictureWindow.day(CanvasMilan.now, CanvasMilan.zone)
        assertEquals(listOf(17, 20, 23), strideHours(day, CanvasMilan.hours, 3).map { it.at.atZone(CanvasMilan.zone).hour })
        // A missing hour is a missing row.
        val holed = CanvasMilan.hours.filterIndexed { i, _ -> i != 6 }
        assertEquals(listOf(17, 23), strideHours(window, holed, 3).map { it.at.atZone(CanvasMilan.zone).hour })
    }

    private fun r1(v: Float) = Math.round(v * 10f) / 10f
}
