package com.callbackdev.chiaro.widget

import com.callbackdev.chiaro.widget.arc.AgendaSp
import com.callbackdev.chiaro.widget.arc.CardHeroSp
import com.callbackdev.chiaro.widget.arc.CardTempSp
import com.callbackdev.chiaro.widget.arc.DialFigureSp
import com.callbackdev.chiaro.widget.arc.HeroSp
import com.callbackdev.chiaro.widget.arc.HeroSubSp
import com.callbackdev.chiaro.widget.arc.PanelTempSp
import com.callbackdev.chiaro.widget.arc.StripLineSp
import com.callbackdev.chiaro.widget.arc.StripStackedTempSp
import com.callbackdev.chiaro.widget.arc.TickSp
import com.callbackdev.chiaro.widget.arc.WeekDaySp
import com.callbackdev.chiaro.widget.arc.WeekHighSp
import com.callbackdev.chiaro.widget.arc.WeekLowSp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import com.callbackdev.chiaro.widget.arc.PlaceSp as ArcPlaceSp
import com.callbackdev.chiaro.widget.arc.StripTempSp as ArcStripTempSp

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
        "arc: dial" to DialFigureSp, "arc: strip" to ArcStripTempSp, "arc: stacked" to StripStackedTempSp,
        "arc: strip line" to StripLineSp, "arc: card number" to CardTempSp, "arc: panel number" to PanelTempSp,
        "arc: place" to ArcPlaceSp, "arc: hero" to HeroSp, "arc: hero sub-line" to HeroSubSp,
        "arc: card hero" to CardHeroSp, "arc: agenda" to AgendaSp, "arc: ticks" to TickSp,
        "arc: week day" to WeekDaySp, "arc: week high" to WeekHighSp, "arc: week low" to WeekLowSp
    )

    @Test
    fun `every widget size is a step of the scale`() {
        everySize.forEach { (role, sp) ->
            assertTrue("$role is $sp sp, not a step of the scale", sp in WidgetType.steps)
        }
    }

    /** The one size off the scale, and the reason it is written down in [WidgetType]. */
    @Test
    fun `the text card's sentence is the one exception`() {
        assertFalse(TextSentenceSp in WidgetType.steps)
        assertEquals(18f, TextSentenceSp)
    }

    @Test
    fun `one role is one size on every card`() {
        // A place under a hero, a strip's label, a list's row, a chip's word.
        assertEquals(PlaceSp, TextFactSp)
        assertEquals(StripHourSp, WeekDaySp)
        assertEquals(StripTempSp, WeekHighSp)
        assertEquals(StripRainSp, WeekLowSp)
        assertEquals(SkyCompactSp, AgendaSp)
        assertEquals(SkyChipSp, WarningChipSp)
        assertEquals(SkyNameSp, HeroSp)
        assertEquals(StaleSp, TextStaleSp)
    }
}
