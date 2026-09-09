package com.callbackdev.chiaro.widget.arc

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Which form the arc widget takes for a given grant, and how the height is shared
 * between the arc and the agenda (9 set 2026).
 *
 * The sizes are the reference device's, read off its screenshots at ~2.35 px/dp: one
 * cell is ~85 × 82 dp, two cells ~159, three ~250, four ~340 wide; two rows ~189, three
 * ~290, four ~390 tall. The layout is arithmetic on dp precisely so it can be pinned
 * here rather than by one launcher's idea of a cell.
 */
class ArcLayoutTest {

    private val defaults = ArcSettings()

    private fun plan(
        size: DpSize,
        settings: ArcSettings = defaults,
        fontScale: Float = 1f,
        stale: Boolean = false,
        agenda: Int = 6,
        week: Boolean = true
    ) = arcPlan(size, fontScale, settings, stale, agenda, week)

    @Test
    fun `the grant picks the form`() {
        assertEquals(ArcForm.DIAL, arcForm(DpSize(85.dp, 82.dp)))
        assertEquals(ArcForm.DIAL, arcForm(DpSize(85.dp, 189.dp)))
        assertEquals(ArcForm.STRIP, arcForm(DpSize(159.dp, 82.dp)))
        assertEquals(ArcForm.STRIP, arcForm(DpSize(250.dp, 82.dp)))
        assertEquals(ArcForm.STRIP, arcForm(DpSize(340.dp, 101.dp)))
        assertEquals(ArcForm.CARD, arcForm(DpSize(159.dp, 189.dp)))
        assertEquals(ArcForm.CARD, arcForm(DpSize(159.dp, 390.dp)))
        assertEquals(ArcForm.PANEL, arcForm(DpSize(250.dp, 189.dp)))
        assertEquals(ArcForm.PANEL, arcForm(DpSize(340.dp, 189.dp)))
        assertEquals(ArcForm.BOARD, arcForm(DpSize(340.dp, 290.dp)))
        assertEquals(ArcForm.BOARD, arcForm(DpSize(250.dp, 390.dp)))
        assertEquals(ArcForm.BOARD, arcForm(DpSize(340.dp, 390.dp)))
    }

    @Test
    fun `columns and rows sit in the gaps between grids`() {
        assertEquals(1, arcColumns(101.dp))
        assertEquals(2, arcColumns(159.dp))
        assertEquals(2, arcColumns(178.dp))
        assertEquals(3, arcColumns(250.dp))
        assertEquals(4, arcColumns(320.dp))
        assertEquals(4, arcColumns(360.dp))
        assertEquals(1, arcRows(101.dp))
        assertEquals(2, arcRows(189.dp))
        assertEquals(3, arcRows(290.dp))
        assertEquals(4, arcRows(390.dp))
    }

    @Test
    fun `the one-cell dial keeps its figure and gives the arc the rest`() {
        val plan = plan(DpSize(85.dp, 82.dp))
        assertEquals(ArcForm.DIAL, plan.form)
        // 85 − 20 of inset; 82 − 20 − 29.04 (a 22 sp line) − 4 = 28.96, under the 0.62
        // aspect's 40.3.
        assertEquals(65f, plan.graphic.width.value, 0.01f)
        assertEquals(28.96f, plan.graphic.height.value, 0.05f)
        assertFalse(plan.hourLabels)
        assertEquals(0, plan.agendaRows)
        // A stale marker takes its 11 sp line from the arc.
        assertEquals(28.96f - 14.52f, plan(DpSize(85.dp, 82.dp), stale = true).graphic.height.value, 0.05f)
    }

    @Test
    fun `two cells by one stacks the words over the arc`() {
        val plan = plan(DpSize(159.dp, 82.dp))
        assertTrue(plan.stacked)
        // 139 wide; 62 − 26.4 (a 20 sp line) − 4 = 31.6 tall: no room for hour labels.
        assertEquals(139f, plan.graphic.width.value, 0.01f)
        assertEquals(31.6f, plan.graphic.height.value, 0.05f)
        assertFalse(plan.hourLabels)
    }

    @Test
    fun `three and four cells by one put the arc beside a 100 dp column of words`() {
        val three = plan(DpSize(250.dp, 82.dp))
        assertFalse(three.stacked)
        assertEquals(122f, three.graphic.width.value, 0.01f)
        assertEquals(62f, three.graphic.height.value, 0.01f)
        assertTrue(three.hourLabels)
        assertFalse(three.temperatures)
        assertEquals(6, three.tickStepHours)

        val four = plan(DpSize(340.dp, 82.dp))
        assertEquals(212f, four.graphic.width.value, 0.01f)
        assertEquals(4, four.tickStepHours)
    }

    @Test
    fun `the two-by-two card is header and arc, the taller card adds agenda`() {
        val small = plan(DpSize(159.dp, 189.dp))
        assertEquals(ArcForm.CARD, small.form)
        // Header: 36.96 + 17.16 + 2 × 17.16 = 88.44; 161 − 88.44 − 6 = 66.56 for the arc.
        assertEquals(2, small.heroLines)
        assertEquals(0, small.agendaRows)
        assertEquals(66.56f, small.graphic.height.value, 0.05f)
        assertTrue(small.hourLabels)
        assertFalse(small.temperatures)

        val tall = plan(DpSize(159.dp, 290.dp))
        // 262 − 88.44 − 6 = 167.56; (167.56 − 72 − 6 + 4) / 27.16 = 3.4 → 3 rows;
        // the arc keeps 167.56 − (6 + 69.48 + 8) = 84.08, enough for temperatures.
        assertEquals(3, tall.agendaRows)
        assertEquals(84.08f, tall.graphic.height.value, 0.05f)
        assertTrue(tall.temperatures)
    }

    @Test
    fun `the four-by-two panel prefers two agenda rows under a 59 dp arc`() {
        val plan = plan(DpSize(340.dp, 189.dp))
        assertEquals(ArcForm.PANEL, plan.form)
        // Hero column 312 − 68 − 8 = 236: one line. Header 39.6 (the 30 sp number).
        assertEquals(1, plan.heroLines)
        // 161 − 39.6 − 6 = 115.4; (115.4 − 56 − 6 + 4) / 27.16 = 2.1 → 2 rows;
        // arc = 115.4 − (6 + 46.32 + 4) = 59.08.
        assertEquals(2, plan.agendaRows)
        assertEquals(59.08f, plan.graphic.height.value, 0.05f)
        assertTrue(plan.hourLabels)
        assertFalse(plan.temperatures)
        assertEquals(3, plan.tickStepHours)
        assertFalse(plan.week)
    }

    @Test
    fun `the three-by-two panel wraps its sentence and keeps one row`() {
        val plan = plan(DpSize(250.dp, 189.dp))
        // Hero column 222 − 76 = 146 < 190: two lines, header 2 × 19.8 + 15.84 = 55.44.
        assertEquals(2, plan.heroLines)
        // 161 − 55.44 − 6 = 99.56; (99.56 − 56 − 6 + 4) / 27.16 = 1.5 → 1 row; arc 70.4.
        assertEquals(1, plan.agendaRows)
        assertEquals(70.4f, plan.graphic.height.value, 0.05f)
        assertEquals(4, plan.tickStepHours)
    }

    @Test
    fun `the board grows the arc and the agenda, and on four rows adds the week`() {
        val three = plan(DpSize(340.dp, 290.dp))
        assertEquals(ArcForm.BOARD, three.form)
        // 262 − 39.6 − 6 = 216.4; (216.4 − 110 − 6 + 4) / 27.16 = 3.8 → 3 rows;
        // arc = 216.4 − (6 + 69.48 + 8) = 132.92.
        assertEquals(3, three.agendaRows)
        assertEquals(132.92f, three.graphic.height.value, 0.05f)
        assertTrue(three.temperatures)
        assertFalse(three.week)

        val four = plan(DpSize(340.dp, 390.dp))
        // 362 − 45.6 = 316.4; the week takes 70.88 + 6; (239.52 − 110 − 6 + 4) / 27.16 =
        // 4.7 → 4 rows; arc = 239.52 − (6 + 92.64 + 12) = 128.88.
        assertTrue(four.week)
        assertEquals(4, four.agendaRows)
        assertEquals(128.88f, four.graphic.height.value, 0.05f)
    }

    @Test
    fun `the agenda never reserves for rows that do not exist`() {
        val one = plan(DpSize(340.dp, 290.dp), agenda = 1)
        assertEquals(1, one.agendaRows)
        // The arc takes what the rows leave, up to its ceiling.
        assertEquals(150f, one.graphic.height.value, 0.01f)
        assertEquals(0, plan(DpSize(340.dp, 290.dp), agenda = 0).agendaRows)
    }

    @Test
    fun `the week can be turned off and gives its height to the agenda`() {
        val plan = plan(DpSize(340.dp, 390.dp), settings = defaults.copy(week = false))
        assertFalse(plan.week)
        // (316.4 − 110 − 6 + 4) / 27.16 = 7.5 → the cap of six; arc 151.44 → ceiling 150.
        assertEquals(AgendaMaxRows, plan.agendaRows)
        assertEquals(150f, plan.graphic.height.value, 0.01f)
    }

    @Test
    fun `without a hero the header is the number alone`() {
        val plan = plan(DpSize(340.dp, 189.dp), settings = defaults.copy(hero = ArcHero.NONE))
        assertEquals(0, plan.heroLines)
        // 161 − 39.6 − 6 = 115.4 as before: the number's line was already the taller.
        assertEquals(59.08f, plan.graphic.height.value, 0.05f)
    }

    @Test
    fun `compact density buys a row where comfortable does not`() {
        // 13 sp at 90% is 11.7 sp: a row of 11.7 × 1.32 + 6 = 21.44 dp.
        assertEquals(23.16f, arcAgendaRowHeight(1f, 1f).value, 0.01f)
        assertEquals(21.44f, arcAgendaRowHeight(1f, ArcSettings.CompactTextScale).value, 0.01f)
        val compact = plan(DpSize(340.dp, 290.dp), settings = defaults.copy(density = ArcDensity.COMPACT))
        // Header 35.64 (30 sp × 0.9 = 27 sp → 35.64); 262 − 35.64 − 6 = 220.36;
        // (220.36 − 110 − 6 + 4) / 25.44 = 4.26 → 4 rows against comfortable's 3.
        assertEquals(4, compact.agendaRows)
    }

    @Test
    fun `a larger system font lowers the row count rather than overflowing`() {
        val plan = plan(DpSize(340.dp, 189.dp), fontScale = 1.3f)
        // Header 51.48; 161 − 51.48 − 6 = 103.52; rows of 28.31 + 4: (103.52 − 56 − 6 + 4)
        // / 32.31 = 1.4 → 1 row; the arc keeps 103.52 − 34.31 = 69.21.
        assertEquals(1, plan.agendaRows)
        assertEquals(69.21f, plan.graphic.height.value, 0.05f)
    }

    @Test
    fun `hour labels need 52 dp of arc and temperatures 76`() {
        assertEquals(6, arcTickStep(179.dp))
        assertEquals(4, arcTickStep(180.dp))
        assertEquals(4, arcTickStep(279.dp))
        assertEquals(3, arcTickStep(280.dp))
        val labelsOff = plan(DpSize(340.dp, 189.dp), settings = defaults.copy(hourLabels = false))
        assertFalse(labelsOff.hourLabels)
        assertFalse(labelsOff.temperatures)
        val tempsOff = plan(DpSize(340.dp, 290.dp), settings = defaults.copy(temperatures = false))
        assertTrue(tempsOff.hourLabels)
        assertFalse(tempsOff.temperatures)
    }

    @Test
    fun `the week strip is four lines and a glyph`() {
        // 14.52 + 22 + 15.84 + 14.52 + 2 × 2 = 70.88.
        assertEquals(70.88f, arcWeekHeight(1f, 1f).value, 0.01f)
    }
}
