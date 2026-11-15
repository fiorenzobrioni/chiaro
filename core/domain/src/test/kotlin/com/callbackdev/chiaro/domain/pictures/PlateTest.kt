package com.callbackdev.chiaro.domain.pictures

import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The plate against the canvas (PLANNING, «Fase 17», la regola della fedeltà, point 1): the
 * same example in, the canvas's own numbers out, to the tenth of a dp the canvas prints them
 * at — except where a deviation is written down, and then the test says which and by how much.
 */
class PlateTest {

    private fun <T : Any> nn(value: T?): T {
        assertNotNull(value)
        return value!!
    }

    private val window = PictureWindow.next(CanvasMilan.now, CanvasMilan.zone)
    private val panel = plateDrawing(window, CanvasMilan.hours, CanvasMilan.PANEL_PLATE, tickEveryHours = 3)
    private val compact = plateDrawing(window, CanvasMilan.hours, CanvasMilan.COMPACT_PLATE, tickEveryHours = 6)

    /** The canvas's `top + (12.4 − T) × 3.6`, drawn 6 dp down: where its points stand. */
    private fun canvasY(t: Double) = (6 + 14 + (12.4 - t) * 3.6).toFloat()

    @Test
    fun `every hour lands on the canvas's x, and on its y within a tenth of a dp`() {
        val points = panel.runs.single().points
        assertEquals(25, points.size)
        points.forEachIndexed { i, p ->
            assertEquals("x of hour $i", 6f + i * 12.5f, p.x, 0.01f)
            // CURVE_TOP is the canvas's 21.08 rounded to 21: every point stands 0.08 dp higher.
            assertEquals("y of hour $i", canvasY(CanvasMilan.T[i]), p.y, 0.1f)
        }
        assertEquals(PlateMetrics.DEGREE, panel.degree, 0f)
    }

    @Test
    fun `the curve is the canvas's smooth, cubic for cubic`() {
        // The canvas's path for the 4×2 opens «M6.0 18.3 C8.1 18.0 14.3 16.2 18.5 16.2 C22.7
        // 16.1 26.8 17.0 31.0 18.0» in its svg, which is 6 dp down the plate: to its tenth, plus
        // the 0.08 dp CURVE_TOP takes off.
        val cubics = panel.runs.single().cubics
        assertEquals(24, cubics.size)
        val first = cubics[0]
        assertEquals(8.1f, first.control1.x, 0.06f)
        assertEquals(18.0f + 6f, first.control1.y, 0.15f)
        assertEquals(14.3f, first.control2.x, 0.06f)
        assertEquals(16.2f + 6f, first.control2.y, 0.15f)
        assertEquals(18.5f, first.end.x, 0.06f)
        val second = cubics[1]
        assertEquals(22.7f, second.control1.x, 0.06f)
        assertEquals(26.8f, second.control2.x, 0.06f)
        assertEquals(17.0f + 6f, second.control2.y, 0.15f)
    }

    @Test
    fun `the high and the low are the canvas's, from the second hour on`() {
        val high = nn(panel.high)
        val low = nn(panel.low)
        assertEquals(12.1, high.temperatureC, 0.0)
        assertEquals(5.9, low.temperatureC, 0.0)
        // Canvas: hiX 279.5, hiY 2.1, loX 192.0, loY 48.4.
        assertEquals(279.5f, high.labelLeft, 0.05f)
        assertEquals(2.1f, high.labelTop, 0.1f)
        assertEquals(192.0f, low.labelLeft, 0.05f)
        assertEquals(48.4f, low.labelTop, 0.1f)
        val c = nn(compact.high)
        assertEquals(197.1f, c.labelLeft, 0.06f)
        assertEquals(134.7f, nn(compact.low).labelLeft, 0.06f)
    }

    @Test
    fun `the disc of now stands on the curve at the first hour`() {
        val now = nn(panel.now)
        // Canvas: a 12 dp box at (1.0, 18.3), so a centre at (7.0, 24.3). Its x is one dp right
        // of the point it stands for (`pts[0].x − 5` for a 12 dp disc); the settings board
        // centres it on the point (`− 6`), and so does this.
        assertEquals(6f, now.x, 0.01f)
        assertEquals(18.3f + PlateMetrics.NOW_DISC / 2f, now.y, 0.1f)
        assertEquals(6f, panel.nowX, 0.01f)
    }

    @Test
    fun `the rain bars are the canvas's boxes on the engine's tones`() {
        // Canvas, 4×2: nine bars from 77.1, 7.8 wide, the first 5.4 high at 62.6.
        assertEquals(9, panel.bars.size)
        val expectedLeft = listOf(77.1f, 89.6f, 102.1f, 114.6f, 127.1f, 139.6f, 152.1f, 164.6f, 177.1f)
        val expectedTop = listOf(62.6f, 57.2f, 54.5f, 53.6f, 55.4f, 57.2f, 59.9f, 62.6f, 64.4f)
        panel.bars.forEachIndexed { i, bar ->
            assertEquals(expectedLeft[i], bar.left, 0.06f)
            assertEquals(expectedTop[i], bar.top, 0.06f)
            assertEquals(7.75f, bar.width, 0.01f)
            assertEquals(PlateMetrics.RAIN_BASELINE, bar.top + bar.height, 0.01f)
        }
        // The canvas's placeholder ladder toned these 0,1,2,3,2,1,0,0,0; the engine's says
        // drizzle under 0.5 and light rain to 2.5 (PLANNING, «La lastra»).
        assertEquals(
            listOf(
                RainTone.DRIZZLE, RainTone.LIGHT, RainTone.LIGHT, RainTone.LIGHT, RainTone.LIGHT,
                RainTone.LIGHT, RainTone.DRIZZLE, RainTone.DRIZZLE, RainTone.DRIZZLE
            ),
            panel.bars.map { it.tone }
        )
        // 3×2: the same nine, 5.5 wide.
        assertEquals(9, compact.bars.size)
        assertEquals(56.7f, compact.bars.first().left, 0.06f)
        assertEquals(5.53f, compact.bars.first().width, 0.01f)
    }

    @Test
    fun `the hours are every three on the 4x2 and every six on the 3x2, now first`() {
        // Canvas: centres at x + 12 of −6.0, 31.5, 69.0 … 256.5; «ora», 17, 20 … 11.
        assertEquals(8, panel.ticks.size)
        panel.ticks.forEachIndexed { i, tick -> assertEquals(6f + i * 37.5f, tick.centerX, 0.01f) }
        assertTrue(panel.ticks.first().isNow)
        assertFalse(panel.ticks.drop(1).any { it.isNow })
        val clock = panel.ticks.drop(1).map { it.at.atZone(CanvasMilan.zone).hour }
        assertEquals(listOf(17, 20, 23, 2, 5, 8, 11), clock)
        assertEquals(listOf(6f, 59.5f, 113f, 166.5f), compact.ticks.map { it.centerX })
    }

    @Test
    fun `the ribbon is the axis`() {
        assertEquals(6f, panel.ribbonLeft, 0f)
        assertEquals(306f, panel.ribbonRight, 0f)
        assertEquals(panel.ribbonRight, panel.xAt(window.end), 0.01f)
    }

    @Test
    fun `the rain tone follows the engine's ladder`() {
        assertEquals(RainTone.DRIZZLE, RainTone.of(null))
        assertEquals(RainTone.DRIZZLE, RainTone.of(0.49))
        assertEquals(RainTone.LIGHT, RainTone.of(0.5))
        assertEquals(RainTone.LIGHT, RainTone.of(2.49))
        assertEquals(RainTone.MODERATE, RainTone.of(2.5))
        assertEquals(RainTone.MODERATE, RainTone.of(7.59))
        assertEquals(RainTone.HEAVY, RainTone.of(7.6))
    }

    @Test
    fun `a flat day stays flat, and a wide one narrows the scale just enough`() {
        val flat = CanvasMilan.hours.map { it.copy(temperatureC = 10.0 + (it.temperatureC!! - 9.0) / 10.0) }
        val f = plateDrawing(window, flat, 312f, 3)
        assertEquals(PlateMetrics.DEGREE, f.degree, 0f)
        val spread = f.runs.single().points.let { pts -> pts.maxOf { it.y } - pts.minOf { it.y } }
        assertTrue("a 0.6° day draws 2.2 dp, not a mountain: $spread", spread < 2.5f)

        // A 14° summer day: 28 dp of band, 2 dp per degree, the coldest on the floor.
        val summer = CanvasMilan.hours.mapIndexed { i, h -> h.copy(temperatureC = 18.0 + 14.0 * i / 24.0) }
        val s = plateDrawing(window, summer, 312f, 3)
        assertEquals(2f, s.degree, 0.001f)
        val ys = s.runs.single().points.map { it.y }
        assertEquals(PlateMetrics.CURVE_TOP, ys.min(), 0.001f)
        assertEquals(PlateMetrics.CURVE_FLOOR, ys.max(), 0.001f)
        assertEquals(7.78f, PLATE_RANGE_AT_FULL_SCALE_C, 0.01f)
    }

    @Test
    fun `a missing temperature breaks the curve, a missing chance draws no bar`() {
        val holed = CanvasMilan.hours.mapIndexed { i, h ->
            when (i) {
                10 -> h.copy(temperatureC = null)
                8 -> h.copy(chancePct = null)
                else -> h
            }
        }
        val d = plateDrawing(window, holed, 312f, 3)
        assertEquals(listOf(10, 14), d.runs.map { it.points.size })
        assertEquals(8, d.bars.size)
        assertFalse(d.bars.any { it.at == CanvasMilan.hours[8].at })

        val gap = CanvasMilan.hours.filterIndexed { i, _ -> i != 5 }
        assertEquals(listOf(5, 19), plateDrawing(window, gap, 312f, 3).runs.map { it.points.size })
    }

    @Test
    fun `now is where the clock is, on the drawn curve`() {
        val later = PictureWindow.next(CanvasMilan.now.plus(Duration.ofMinutes(30)), CanvasMilan.zone)
        val d = plateDrawing(later, CanvasMilan.hours, 312f, 3)
        val now = nn(d.now)
        assertEquals(6f + 6.25f, now.x, 0.01f)
        val run = d.runs.single()
        assertEquals(run.yAt(now.x)!!, now.y, 0.001f)
        // Between 11.2° and 11.8°, so above the first point and under the second.
        assertTrue(now.y < run.points[0].y && now.y > run.points[1].y)
        assertTrue(d.ticks.first().isNow)
    }

    @Test
    fun `the day's drawing starts at midnight and labels the day's own extremes`() {
        val day = PictureWindow.day(CanvasMilan.now, CanvasMilan.zone)
        assertEquals(LocalDateTime.parse("2026-11-15T00:00"), day.start.atZone(CanvasMilan.zone).toLocalDateTime())
        val morning = (0 until 14).map { i ->
            PictureHour(day.start.plus(Duration.ofHours(i.toLong())), 7.0 + i * 0.1, 0, 0.0)
        }
        val d = plateDrawing(day, morning + CanvasMilan.hours, 312f, 3)
        // 25 points, midnight to midnight; now (14:00) is the fifteenth.
        assertEquals(25, d.runs.single().points.size)
        assertEquals(6f + 14 * 12.5f, d.nowX, 0.01f)
        // «La giornata» searches from the first hour: the low is midnight's 7.0, not tonight's.
        assertEquals(7.0, nn(d.low).temperatureC, 0.0)
        assertTrue(d.ticks.none { it.isNow })
        assertEquals(8, d.ticks.size)
    }

    @Test
    fun `a day with a clock change keeps time as its axis`() {
        val zone = CanvasMilan.zone
        val fallBack = LocalDateTime.parse("2026-10-25T12:00").atZone(zone).toInstant()
        val day = PictureWindow.day(fallBack, zone)
        assertEquals(25.0, day.hours, 0.0)
        val hours = (0..25).map { i -> PictureHour(day.start.plus(Duration.ofHours(i.toLong())), 10.0, 50, 1.0) }
        val d = plateDrawing(day, hours, 312f, 3)
        assertEquals(26, d.runs.single().points.size)
        assertEquals(306f, d.runs.single().points.last().x, 0.01f)
        assertEquals(300f / 25f * PlateMetrics.BAR_SHARE, d.bars.first().width, 0.01f)
    }

    @Test
    fun `labels stay inside the plate, and one point gets one label`() {
        val rising = CanvasMilan.hours.mapIndexed { i, h -> h.copy(temperatureC = 5.0 + i * 0.2) }
        val d = plateDrawing(window, rising, 312f, 3)
        assertEquals(312f - PlateMetrics.LABEL_WIDTH, nn(d.high).labelLeft, 0.01f)
        val flat = CanvasMilan.hours.map { it.copy(temperatureC = 9.0) }
        val f = plateDrawing(window, flat, 312f, 3)
        assertNotNull(f.high)
        assertNull(f.low)
    }

    @Test
    fun `hours outside the window are not drawn`() {
        val before = PictureHour(CanvasMilan.now.minus(Duration.ofHours(1)), 30.0, 90, 9.0)
        val after = PictureHour(window.end.plus(Duration.ofHours(1)), -10.0, 90, 9.0)
        val d = plateDrawing(window, listOf(before) + CanvasMilan.hours + after, 312f, 3)
        assertEquals(panel, d)
        assertEquals(Instant.parse("2026-11-16T13:00:00Z"), window.end)
    }
}
