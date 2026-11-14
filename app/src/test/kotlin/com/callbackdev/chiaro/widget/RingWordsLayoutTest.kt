package com.callbackdev.chiaro.widget

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.callbackdev.chiaro.ui.components.LightPhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * «In parole» with its ring at the family's reference grants (committente, 14 Nov 2026): the
 * same circle the sister cards draw at each size, so they stand in line on one home screen.
 */
class RingWordsLayoutTest {
    @Test
    fun `the forms by size`() {
        assertEquals(RingForm.CELL, ringForm(DpSize(85.dp, 85.dp)))
        assertEquals(RingForm.ROW, ringForm(DpSize(159.dp, 85.dp)))
        assertEquals(RingForm.ROW, ringForm(DpSize(340.dp, 85.dp)))
        assertEquals(RingForm.TALL, ringForm(DpSize(159.dp, 189.dp)))
        assertEquals(RingForm.PANEL, ringForm(DpSize(250.dp, 189.dp)))
        assertEquals(RingForm.PANEL, ringForm(DpSize(340.dp, 189.dp)))
    }

    @Test
    fun `the family's circle at the reference grants`() {
        assertEquals(69.dp, ringCellCircle(DpSize(85.dp, 85.dp)))
        assertEquals(44.dp, ringRowCircle(DpSize(159.dp, 85.dp)))
        assertEquals(56.dp, ringRowCircle(DpSize(340.dp, 85.dp)))
        assertEquals(52.75.dp, ringTallCircle(DpSize(159.dp, 189.dp), 1f))
        assertEquals(73.75.dp, ringTallCircle(DpSize(340.dp, 189.dp), 1f))
        assertEquals(104.dp, ringTallCircle(DpSize(159.dp, 293.dp), 1f))
        assertEquals(119.25.dp, ringTallCircle(DpSize(340.dp, 293.dp), 1f))
        assertEquals(95.dp, ringTallCircle(DpSize(159.dp, 189.dp), 1f, sentence = false))
    }

    @Test
    fun `a panel's header and strip fit the card`() {
        val size = DpSize(340.dp, 189.dp)
        val header = ringPanelHeader(size, 1f)
        val strip = ringPanelStrip(size, 1f)
        assertEquals(73.75.dp, header)
        assertTrue(header + RingGap + strip <= 189.dp - 28.dp + 0.01.dp)
        assertTrue(strip >= RingStripMin)
    }

    @Test
    fun `noon stands at the top of the ring and midnight at the bottom`() {
        assertEquals(0f, ringAngle(0.5f), 0.001f)
        assertEquals(-180f, ringAngle(0f), 0.001f)
        assertEquals(90f, ringAngle(0.75f), 0.001f)
        assertEquals(-90f, ringAngle(0.25f), 0.001f)
    }

    @Test
    fun `the daylight is the phases the sun is up in, merged`() {
        val phases = listOf(
            LightPhase(0f, 0.28f, -30.0),
            LightPhase(0.28f, 0.31f, 2.0),
            LightPhase(0.31f, 0.70f, 30.0),
            LightPhase(0.70f, 0.78f, 2.0),
            LightPhase(0.78f, 1f, -4.0)
        )
        assertEquals(listOf(0.28f..0.78f), daylight(phases))
        assertEquals(emptyList<ClosedFloatingPointRange<Float>>(), daylight(listOf(LightPhase(0f, 1f, -30.0))))
        assertEquals(listOf(0f..1f), daylight(listOf(LightPhase(0f, 1f, 30.0))))
    }

    @Test
    fun `a temperature inside one cell's ring is as large as fits, never past the family's cap`() {
        assertEquals(RingInsideMaxSp, ringInsideSp(69.dp, 1.2f, 1f))
        assertNull(ringInsideSp(36.dp, 9f, 1f))
    }
}
