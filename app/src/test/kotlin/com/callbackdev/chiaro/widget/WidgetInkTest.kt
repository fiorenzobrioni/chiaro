package com.callbackdev.chiaro.widget

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The see-through widget's ink, pinned as a table (device report, 4 set: a transparent
 * widget on a phone with a LIGHT theme and a BLACK wallpaper wrote dark on dark, and the
 * same phone in dark mode was fine — the tell that the rule was reading the theme when it
 * could not read the wallpaper).
 *
 * A screenshot cannot pin this: the failure only shows on a phone whose wallpaper refuses
 * to publish its colors, which is exactly the case nobody has in front of them.
 */
class WidgetInkTest {

    @Test
    fun `a solid card is its own ground`() {
        assertEquals(WidgetInk.OVER_SKY, widgetInk(WidgetBackground.SKY, 85, night = false, wallpaperCarriesDarkInk = false))
        assertEquals(WidgetInk.ON_LIGHT, widgetInk(WidgetBackground.LIGHT, 100, night = true, wallpaperCarriesDarkInk = false))
        assertEquals(WidgetInk.ON_DARK, widgetInk(WidgetBackground.DARK, 100, night = false, wallpaperCarriesDarkInk = true))
        assertEquals(WidgetInk.ON_DARK, widgetInk(WidgetBackground.SYSTEM, 100, night = true, wallpaperCarriesDarkInk = true))
        assertEquals(WidgetInk.ON_LIGHT, widgetInk(WidgetBackground.SYSTEM, 100, night = false, wallpaperCarriesDarkInk = false))
    }

    @Test
    fun `the floor is where the card stops being trusted`() {
        // Exactly at the floor the card still answers; one step under it, it does not.
        assertEquals(
            WidgetInk.OVER_SKY,
            widgetInk(WidgetBackground.SKY, InkTrustFloorPct, night = false, wallpaperCarriesDarkInk = false)
        )
        assertEquals(
            WidgetInk.ON_DARK,
            widgetInk(WidgetBackground.SKY, InkTrustFloorPct - 1, night = false, wallpaperCarriesDarkInk = false)
        )
    }

    @Test
    fun `a see-through card follows the wallpaper, and only an affirmed one carries dark ink`() {
        listOf(WidgetBackground.SKY, WidgetBackground.SYSTEM).forEach { background ->
            assertEquals(
                "$background over a wallpaper the system says is bright",
                WidgetInk.ON_LIGHT,
                widgetInk(background, 0, night = false, wallpaperCarriesDarkInk = true)
            )
            // The reported bug, both ways round: the THEME must not decide this.
            assertEquals(
                "$background, light theme, wallpaper unaffirmed",
                WidgetInk.ON_DARK,
                widgetInk(background, 0, night = false, wallpaperCarriesDarkInk = false)
            )
            assertEquals(
                "$background, dark theme, wallpaper unaffirmed",
                WidgetInk.ON_DARK,
                widgetInk(background, 0, night = true, wallpaperCarriesDarkInk = false)
            )
        }
    }

    @Test
    fun `an explicit light or dark card still names its own ink when see-through`() {
        // The way out: a reader whose transparent widget came up unreadable can say so.
        assertEquals(
            WidgetInk.ON_DARK,
            widgetInk(WidgetBackground.DARK, 0, night = false, wallpaperCarriesDarkInk = true)
        )
        assertEquals(
            WidgetInk.ON_LIGHT,
            widgetInk(WidgetBackground.LIGHT, 0, night = true, wallpaperCarriesDarkInk = false)
        )
    }

    @Test
    fun `only the light ground is a light ground`() {
        assertEquals(false, WidgetInk.ON_LIGHT.darkGround)
        assertEquals(true, WidgetInk.ON_DARK.darkGround)
        assertEquals(true, WidgetInk.OVER_SKY.darkGround)
    }
}
