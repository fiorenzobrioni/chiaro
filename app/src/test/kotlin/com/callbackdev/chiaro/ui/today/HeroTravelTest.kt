package com.callbackdev.chiaro.ui.today

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The hero's number travelling into the bar (DESIGN §8.1e, 15 ott 2026): its pose is a pure
 * function of the scroll, so the page can be dragged either way and the number follows.
 */
class HeroTravelTest {

    private val rest = Offset(32f, 300f)
    private val dock = Offset(600f, 40f)
    private val trigger = 140f
    private val travel = 200f
    private val end = 0.25f

    private fun pose(itemTop: Float?) = heroPose(itemTop, rest, dock, trigger, travel, end)

    @Test
    fun `at rest the number stands where the canvas keeps it`() {
        val p = pose(0f)
        assertEquals(HeroPose(32f, 300f, 1f, 0f), p)
    }

    @Test
    fun `it rides the sky until it reaches the trigger`() {
        // 160px of scroll: the number's top at 140, exactly the trigger, not yet moving.
        val p = pose(-160f)
        assertEquals(0f, p.progress, 0f)
        assertEquals(140f, p.y, 0f)
        assertEquals(1f, p.scale, 0f)
    }

    @Test
    fun `halfway through the travel it is halfway to the bar, with no jump at either end`() {
        val p = pose(-260f)
        assertEquals(0.5f, p.progress, 1e-6f)
        assertEquals((trigger + dock.y) / 2f, p.y, 1e-4f)
        assertEquals((rest.x + dock.x) / 2f, p.x, 1e-4f)
        assertEquals((1f + end) / 2f, p.scale, 1e-6f)
        // A pixel either side of the trigger the figure is where the sky would have it.
        assertEquals(pose(-160f).y, pose(-160.5f).y, 1f)
    }

    @Test
    fun `past the travel, and with the canvas gone, it stays docked`() {
        assertEquals(HeroPose(dock.x, dock.y, end, 1f), pose(-400f))
        assertEquals(HeroPose(dock.x, dock.y, end, 1f), pose(null))
    }

    @Test
    fun `the slot stands before the gear and in the middle of the bar's row`() {
        val slot = dockSlot(
            pageWidth = 1080f, statusBottom = 72f, barBottom = 264f,
            numberWidth = 400f, numberHeight = 264f, endScale = 0.25f, endInset = 192f
        )
        assertEquals(1080f - 192f - 100f, slot.x, 1e-4f)
        assertEquals((72f + 264f) / 2f - 33f, slot.y, 1e-4f)
    }

    /** At 88sp a 200% type would set «−12°» wider than a 360dp phone: the figure grows to
     * 130% and the words beside it in full. */
    @Test
    fun `the figure grows with the reader's type, up to its ceiling`() {
        assertEquals(1f, heroFontScale(1f), 0f)
        assertEquals(1.15f, heroFontScale(1.15f), 0f)
        assertEquals(HeroScaleCeiling, heroFontScale(2f), 0f)
    }
}
