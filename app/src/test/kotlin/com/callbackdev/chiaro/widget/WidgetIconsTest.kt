package com.callbackdev.chiaro.widget

import com.callbackdev.chiaro.data.WeatherIcons
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The per-widget icon family (committente, 8 set): a card may name a family of its own,
 * or keep following the app. The table is three lines of code and worth a test anyway —
 * the failure mode of getting it wrong is a widget that quietly stops answering to the
 * Settings screen, which nobody reports as a bug.
 */
class WidgetIconsTest {

    @Test
    fun `APP is whatever the app is set to`() {
        assertEquals(WeatherIcons.LINE, WidgetIcons.APP.resolve(WeatherIcons.LINE))
        assertEquals(WeatherIcons.FILL, WidgetIcons.APP.resolve(WeatherIcons.FILL))
    }

    @Test
    fun `a named family ignores the app`() {
        WeatherIcons.entries.forEach { app ->
            assertEquals(WeatherIcons.FILL, WidgetIcons.FILL.resolve(app))
            assertEquals(WeatherIcons.LINE, WidgetIcons.LINE.resolve(app))
        }
    }

    /** A widget placed before the option existed carries no choice, and must keep
     * drawing what it drew: the default is the one that changes nothing. */
    @Test
    fun `the default follows the app`() {
        assertEquals(WidgetIcons.APP, WidgetLook().icons)
    }
}
