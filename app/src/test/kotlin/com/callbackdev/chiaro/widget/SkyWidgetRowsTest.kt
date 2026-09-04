package com.callbackdev.chiaro.widget

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * How many moments the Sky widget draws for a given grant (committente, 4 set).
 *
 * The budget is arithmetic on the card's own heights precisely so it can be checked
 * here: launchers disagree about what a cell is, and a screenshot of one of them
 * proves nothing about the next. What the numbers below encode:
 *
 * - the card spends 24dp on its own padding (12 top, 12 bottom);
 * - a hero with a verdict costs 80 more (48dp glyph, 6dp gap, 26dp pill); without one
 *   it costs 48;
 * - the air between the moment and its list costs 10, charged once;
 * - every further moment costs 28 (a 22dp row and its 6dp gap).
 *
 * So the first row arrives at 114 + 28 = 142dp of grant, and at 110 when the hero
 * carries no verdict.
 */
class SkyWidgetRowsTest {

    @Test
    fun `the first row arrives at the height that actually fits it`() {
        assertEquals(0, skyExtraRows(141.dp, heroHasVerdict = true, available = 5))
        assertEquals(1, skyExtraRows(142.dp, heroHasVerdict = true, available = 5))
        assertEquals(1, skyExtraRows(169.dp, heroHasVerdict = true, available = 5))
        assertEquals(2, skyExtraRows(170.dp, heroHasVerdict = true, available = 5))
        assertEquals(3, skyExtraRows(198.dp, heroHasVerdict = true, available = 5))
    }

    @Test
    fun `a one-cell widget is the hero alone, exactly as it was before`() {
        // ~101dp is what the launcher granted the one-cell widget on the device this
        // was tuned on (measured, 4th device pass).
        assertEquals(0, skyExtraRows(101.dp, heroHasVerdict = true, available = 5))
    }

    @Test
    fun `an unjudged hero leaves the pill's room, and a row takes it sooner`() {
        // Solar noon and the moon's phase carry no verdict: nothing the clouds can
        // spoil has one (the catalog's `observable` rule). The 32dp the pill would
        // have used is real space, and leaving it empty would be the widget refusing
        // to say something true it has room for.
        assertEquals(0, skyExtraRows(109.dp, heroHasVerdict = false, available = 5))
        assertEquals(1, skyExtraRows(110.dp, heroHasVerdict = false, available = 5))
        assertEquals(2, skyExtraRows(138.dp, heroHasVerdict = false, available = 5))
    }

    @Test
    fun `the list is never padded - room beyond the subscriptions draws nothing`() {
        assertEquals(2, skyExtraRows(400.dp, heroHasVerdict = true, available = 2))
        assertEquals(0, skyExtraRows(400.dp, heroHasVerdict = true, available = 0))
    }

    @Test
    fun `a grant too short for the hero asks for no rows, never a negative one`() {
        assertEquals(0, skyExtraRows(40.dp, heroHasVerdict = true, available = 5))
        assertEquals(0, skyExtraRows(0.dp, heroHasVerdict = true, available = 5))
        assertEquals(0, skyExtraRows(400.dp, heroHasVerdict = true, available = -1))
    }
}
