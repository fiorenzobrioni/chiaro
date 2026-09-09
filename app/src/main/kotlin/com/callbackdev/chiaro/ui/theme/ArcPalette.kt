package com.callbackdev.chiaro.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * The inks of the day's arc widget that no Material role covers: the sun's disc and the
 * moon's. Both are DEPICTIONS, not encodings (the ribbon's own licence, DESIGN §4) — the
 * sun is drawn amber because the sun is amber, and the phase of the moon is drawn as a
 * shape before it is a color — so they may keep one fixed value on every ground.
 *
 * Everything else the arc paints comes from roles: the path and the labels from the
 * card's own inks, the bands from the sky tables, the rain from the §2.3 ramp. This is
 * the one file of the feature allowed a hex, and it lives in `ui/theme/` for that reason
 * (`NoRawColorTest`).
 */
object ArcPalette {

    /** The sun: the golden anchor's amber lifted one step, so the disc stays a disc on
     * the scrimmed day sky (Y 0.55 against the band's 0.11) and still reads on paper. */
    val Sun = Color(0xFFF9B233)

    /** The disc's soft halo, drawn at low alpha behind it: the glow, never a second sun. */
    val SunGlow = Color(0xFFFFD27A)

    /** The lit part of the moon: the cool pale of the canvas' own moonlight, brightened
     * to sit on the night band. */
    val MoonLit = Color(0xFFE8EEF8)

    /** The moon's shadowed part: the canvas' moonlight itself — the color the sky turns
     * under it — so the dark side is drawn as sky rather than as black. */
    val MoonShadow = Color(0xFF3A4A78)
}
