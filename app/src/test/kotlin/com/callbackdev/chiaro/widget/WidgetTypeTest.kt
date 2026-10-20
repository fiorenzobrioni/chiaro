package com.callbackdev.chiaro.widget

import com.callbackdev.chiaro.widget.window.WindowCaptionSp
import com.callbackdev.chiaro.widget.window.WindowLabelSp
import com.callbackdev.chiaro.widget.window.WindowPlaceSp
import com.callbackdev.chiaro.widget.window.WindowSentenceSp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The widgets' type scale (widget review, 13 ott 2026): every size a card prints in is a
 * step of [WidgetType], and one role is one size on every card.
 */
class WidgetTypeTest {

    private val everySize = mapOf(
        "Now: temperature" to TemperatureSp, "Now: sentence" to SentenceSp,
        "Now: place" to PlaceSp, "stale" to StaleSp,
        "Today: hour" to StripHourSp, "Today: figure" to StripTempSp, "Today: rain" to StripRainSp,
        "Sky: clock" to SkyTimeSp, "Sky: name" to SkyNameSp, "Sky: chip" to SkyChipSp,
        "Sky: evidence" to SkyEvidenceSp, "Sky: row" to SkyCompactSp,
        "text: facts" to TextFactSp, "text: stale" to TextStaleSp, "text: later" to TextLaterSp,
        "warning chip" to WarningChipSp,
        "window: place" to WindowPlaceSp, "window: sentence" to WindowSentenceSp,
        "window: aside" to WindowCaptionSp, "window: names" to WindowLabelSp
    )

    @Test
    fun `every widget size is a step of the scale`() {
        everySize.forEach { (role, sp) ->
            assertTrue("$role is $sp sp, not a step of the scale", sp in WidgetType.steps)
        }
    }

    /** The text card's sentence was the one exception until 13 ott 2026: no longer. */
    @Test
    fun `the text card's sentence is on the scale too`() {
        assertTrue(TextSentenceSp in WidgetType.steps)
        assertEquals(WidgetType.Body, TextSentenceSp)
    }

    @Test
    fun `one role is one size on every card`() {
        // A place under a hero, a strip's label, a list's row, a chip's word.
        assertEquals(PlaceSp, TextFactSp)
        assertEquals(SkyChipSp, WarningChipSp)
        assertEquals(StaleSp, TextStaleSp)
        assertEquals(PlaceSp, WindowPlaceSp)
        assertEquals(SkyCompactSp, WindowSentenceSp)
        assertEquals(WindowCaptionSp, WindowLabelSp)
    }
}
