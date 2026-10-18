package com.callbackdev.chiaro.ui.today

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * When the hero's number counts as gone under the bar (DESIGN §8.1e, 18 ott 2026): one line,
 * the number's middle crossing the bar's bottom edge, so the bar's own number is either there
 * or not and never halfway.
 */
class HeroInBarTest {

    private val rest = Offset(32f, 300f)
    private val size = IntSize(400, 200)
    private val barBottom = 264f

    private fun under(itemTop: Float?) = heroUnderBar(itemTop, rest, size, barBottom)

    @Test
    fun `at rest the number is on the sky`() {
        assertFalse(under(0f))
    }

    @Test
    fun `it goes under the bar when its middle crosses the bar's edge, not before`() {
        // The middle is at itemTop + 300 + 100: on the edge at itemTop = -136.
        assertFalse(under(-136f))
        assertTrue(under(-137f))
    }

    @Test
    fun `scrolled back, it is on the sky again`() {
        assertTrue(under(-300f))
        assertFalse(under(-100f))
    }

    @Test
    fun `with the canvas out of the list it is under the bar`() {
        assertTrue(under(null))
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
