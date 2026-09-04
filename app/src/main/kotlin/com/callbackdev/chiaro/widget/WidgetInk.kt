package com.callbackdev.chiaro.widget

/**
 * Which set of inks a widget writes with. [OVER_SKY] is the white-over-scrim pair the
 * app's own canvas uses (DESIGN §3.6); the other two are the schemes' own pairs, named
 * after the GROUND they are written on, not after the mode they come from.
 */
enum class WidgetInk {
    OVER_SKY,
    ON_LIGHT,
    ON_DARK;

    /** What a §2.3 quantity ramp needs to know: ramps pick the set SELECTED for dark
     * rather than the light one flipped. */
    val darkGround: Boolean get() = this != ON_LIGHT
}

/** Below this solidity the card stops being the ink's ground. */
const val InkTrustFloorPct = 50

/**
 * The whole ink rule, kept pure so it can be pinned by a test rather than by a
 * screenshot (`WidgetInkTest`).
 *
 * Above [InkTrustFloorPct] the card is its own ground and decides on its own. Below it
 * the card is see-through and cannot, so the question passes to whoever can answer it:
 *
 * - **The reader, when they have already answered.** LIGHT and DARK are a choice of ink
 *   as much as of card, so they keep deciding at any solidity. This is also the way out
 *   for a see-through widget that came up unreadable — before it (device report, 4 set)
 *   picking "dark" at zero opacity still handed the decision to the wallpaper, so there
 *   was no way out at all.
 * - **The wallpaper, for SKY and SYSTEM**, which named no ink.
 *   [wallpaperCarriesDarkInk] is the system's own affirmative hint, and is read as one:
 *   dark ink only where the platform says the ground is bright, light ink everywhere
 *   else. The 3-set version fell back to the phone's THEME whenever the wallpaper gave
 *   no answer, and that is exactly the reported failure — a light theme over a black
 *   wallpaper wrote black on black, while the same phone in dark mode was fine. The
 *   fallback cannot fail the same way round, because a bright wallpaper is precisely
 *   the case the hint exists to announce.
 *
 * [night] is the phone's own mode, and only SYSTEM at full solidity still asks for it.
 */
fun widgetInk(
    background: WidgetBackground,
    opacityPct: Int,
    night: Boolean,
    wallpaperCarriesDarkInk: Boolean
): WidgetInk {
    if (opacityPct < InkTrustFloorPct) {
        return when (background) {
            WidgetBackground.LIGHT -> WidgetInk.ON_LIGHT
            WidgetBackground.DARK -> WidgetInk.ON_DARK
            WidgetBackground.SKY, WidgetBackground.SYSTEM ->
                if (wallpaperCarriesDarkInk) WidgetInk.ON_LIGHT else WidgetInk.ON_DARK
        }
    }
    return when (background) {
        // The scrim makes the ground dark whatever the sky above it is doing.
        WidgetBackground.SKY -> WidgetInk.OVER_SKY
        WidgetBackground.LIGHT -> WidgetInk.ON_LIGHT
        WidgetBackground.DARK -> WidgetInk.ON_DARK
        WidgetBackground.SYSTEM -> if (night) WidgetInk.ON_DARK else WidgetInk.ON_LIGHT
    }
}
