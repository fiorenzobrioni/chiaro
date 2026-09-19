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
        // A card colour is a ground the card brings itself, like the scrimmed sky, and
        // every shipped one is dark enough to carry the same white pair (§2.6).
        assertEquals(WidgetInk.OVER_SKY, widgetInk(WidgetBackground.COLOR, 100, night = false, wallpaperCarriesDarkInk = true))
        assertEquals(WidgetInk.OVER_SKY, widgetInk(WidgetBackground.COLOR, 100, night = true, wallpaperCarriesDarkInk = false))
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
        // COLOR is in this list and not in the one below: picking a colour chooses a
        // GROUND, not an ink, and at 20% solidity that ground is mostly the wallpaper.
        listOf(
            WidgetBackground.SKY, WidgetBackground.SYSTEM, WidgetBackground.COLOR
        ).forEach { background ->
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

    /** A solid card is its own ground, whatever is behind it — so above the floor the
     * wallpaper's hint must change nothing, for every background there is. Asserted over
     * the enum rather than over a list, so a fifth kind of card (the colours, 19 set 2026)
     * cannot be added without meeting the rule. */
    @Test
    fun `above the floor no card asks the wallpaper`() {
        WidgetBackground.entries.forEach { background ->
            listOf(InkTrustFloorPct, 75, 100).forEach { opacity ->
                listOf(false, true).forEach { night ->
                    assertEquals(
                        "$background at $opacity% (night=$night) let the wallpaper decide",
                        widgetInk(background, opacity, night, wallpaperCarriesDarkInk = false),
                        widgetInk(background, opacity, night, wallpaperCarriesDarkInk = true)
                    )
                }
            }
        }
    }

    @Test
    fun `only the light ground is a light ground`() {
        assertEquals(false, WidgetInk.ON_LIGHT.darkGround)
        assertEquals(true, WidgetInk.ON_DARK.darkGround)
        assertEquals(true, WidgetInk.OVER_SKY.darkGround)
    }
}
