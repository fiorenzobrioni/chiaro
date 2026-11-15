package com.callbackdev.chiaro.widget

import com.callbackdev.chiaro.ui.theme.WidgetCardColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * What a card wears the moment it is placed, before anybody opens its settings
 * (committente, 21 set 2026). These are five defaults on five cards that nothing else
 * in the suite touches, and a default is exactly the value that is changed in one of
 * the two places it is written and left alone in the other.
 */
class WidgetLookDefaultsTest {

    @Test
    fun `every card is a blue card, but «In immagini» wears the sky`() {
        (WidgetKind.entries.filter { it != WidgetKind.PICTURES } + null).forEach { kind ->
            val look = WidgetLook.defaultsFor(kind)
            assertEquals("$kind", WidgetBackground.COLOR, look.background)
            assertEquals("$kind", WidgetCardColor.BLUE, look.cardColor)
        }
        // The committente's first decision of Fase 17 (15 nov 2026): the drawn card's ground is
        // the app's colour, which for Chiaro is the sky of this hour; a colour picked later is
        // still the blue one.
        val pictures = WidgetLook.defaultsFor(WidgetKind.PICTURES)
        assertEquals(WidgetBackground.SKY, pictures.background)
        assertEquals(WidgetCardColor.BLUE, pictures.cardColor)
    }

    /** The canvas's settings board, every switch on and the next 24 hours. */
    @Test
    fun `«In immagini» starts as the canvas drew it`() {
        val look = WidgetLook.defaultsFor(WidgetKind.PICTURES)
        assertTrue("the hour's drawing", look.showIcon)
        assertTrue("the rain", look.picturesRain)
        assertTrue("the ribbon", look.picturesRibbon)
        assertTrue("the next days", look.showLater)
        assertTrue("the warnings", look.showWarning)
        assertEquals(com.callbackdev.chiaro.domain.pictures.PictureSpan.NEXT_24_HOURS, look.picturesSpan)
        assertEquals(WidgetLook.DEFAULT_OPACITY, look.opacityPct)
    }

    /** The one field that answers differently per card. */
    @Test
    fun `only «In parole» starts with the day's high and low`() {
        assertTrue(WidgetLook.defaultsFor(WidgetKind.TEXT).showDayRange)
        listOf(WidgetKind.NOW, WidgetKind.WINDOW, WidgetKind.PICTURES).forEach { kind ->
            assertFalse("$kind", WidgetLook.defaultsFor(kind).showDayRange)
        }
    }

    /** An id the host has not bound yet: the household's answer, never a guess. */
    @Test
    fun `an unbound widget takes the household's defaults`() {
        assertEquals(WidgetLook(), WidgetLook.defaultsFor(null))
    }

    @Test
    fun `the rest of the card is what it always was`() {
        val look = WidgetLook.defaultsFor(WidgetKind.NOW)
        assertEquals(WidgetLook.DEFAULT_OPACITY, look.opacityPct)
        assertTrue("the day's sentence", look.showSentence)
        assertTrue("the official warning", look.showWarning)
        assertFalse("the text card's glyph", look.showIcon)
        assertEquals(WidgetArrangement.ICON_START, look.arrangement)
        assertEquals(WidgetIcons.APP, look.icons)
    }
}
