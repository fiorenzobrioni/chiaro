package com.callbackdev.chiaro.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * «In immagini»'s inks (Fase 17.2): what the plate, the dial and the strip are drawn with on
 * each ground the card can wear (`docs/design/in-immagini/Grounds.dc.html`). «Il disegno non
 * cambia colore con lo sfondo: cambia solo l'inchiostro. Il nastro resta il cielo vero, la
 * pioggia resta un tono solo.» Here rather than in the painter because a hex outside
 * `ui/theme/` is a hex outside `ui/theme/` (`NoRawColorTest`).
 *
 * The rain's four tones are the canvas's blue, in its order, **moved where they did not clear
 * 3:1 as marks** (WCAG's non-text contrast, DESIGN §10; `PicturesInksTest` measures them over
 * the plate's veil on the brightest scrimmed sky either table paints, on every card colour, on
 * paper and on the dark card). The canvas's darkest sky tone, #1E97CF, was 2.35:1 there; its two
 * lightest paper tones 1.78 and 2.75; its darkest dark one 2.81. Each moved only as far as the
 * floor asked, toward the next tone, so four tones remain and the steps between them stay
 * visible (PLANNING, 17.2).
 */
data class PicturesInks(
    /** The curve, the labels of the high and the low: the card's own strong ink. */
    val ink: Color,
    /** The hours under the ribbon: the card's quiet ink. */
    val quiet: Color,
    /** The plate's ground, over the card's. */
    val veil: Color,
    /** The plate's 1 dp inner line. */
    val hairline: Color,
    /** The disc of «adesso» (always white, as on every ground of the canvas) and its ring on
     * the plate and on the dial and strip. */
    val disc: Color,
    val plateDiscRing: Color,
    val markDiscRing: Color,
    /** The rain bars' four tones, drizzle first, on the plate's [veil]. */
    val rain: List<Color>,
    /** The dial's rain dots, at full opacity (they fade with the chance). */
    val dot: Color,
    /** The chance in words under the 4×3's hours and the 4×4's days: text on the card, at
     * 4.5:1. The canvas's #A9E2FF is 3.77:1 on the brightest scrimmed sky, so on the sky and
     * the colours it is that blue lightened halfway and a little more toward white, #D8F2FF;
     * on paper and on the dark card it is the dot's blue, which already passes. */
    val chance: Color,
    /** The veil over the dial's hours already gone. */
    val spent: Color,
    /** The 4×1's rain marks over the ribbon, which is the sky's on every ground. */
    val stripMark: Color
) {
    companion object {
        /** The canvas's night blue, `rgb(6, 24, 52)`: the veil's hue on the sky. */
        val Night = Color(0xFF061834)

        /** The canvas's lightest rain, #A9E2FF: the strongest tone on a dark ground, the dots,
         * the strip's marks. */
        val Rain = Color(0xFFA9E2FF)

        /** On the sky of this hour, with its §3.6 scrim. */
        val Sky = PicturesInks(
            ink = Color.White,
            // 75%, the family's quiet ink (§2.6), where the canvas wrote 78 on some lines: the
            // plan's first decided deviation.
            quiet = Color.White.copy(alpha = 0.75f),
            veil = Night.copy(alpha = 0.30f),
            hairline = Color.White.copy(alpha = 0.10f),
            disc = Color.White,
            plateDiscRing = Night.copy(alpha = 0.35f),
            markDiscRing = Night.copy(alpha = 0.40f),
            rain = listOf(Color(0xFF3FB0E6), Color(0xFF62C1EE), Color(0xFF86D1F7), Rain),
            dot = Rain,
            chance = Color(0xFFD8F2FF),
            spent = Night.copy(alpha = 0.42f),
            stripMark = Rain.copy(alpha = 0.85f)
        )

        /** On one of the six card colours: the sky's inks, the plate a black veil at 18%. */
        val Card = Sky.copy(veil = Color.Black.copy(alpha = 0.18f))

        /** On paper: the scheme's inks, the plate the ink at 5%, the rain darker as it is
         * stronger (on a light ground the strong end of a ramp is its dark end, §2.3). */
        fun light(onSurface: Color, onSurfaceVariant: Color, surface: Color) = PicturesInks(
            ink = onSurface,
            quiet = onSurfaceVariant,
            veil = onSurface.copy(alpha = 0.05f),
            hairline = onSurface.copy(alpha = 0.08f),
            disc = Color.White,
            plateDiscRing = onSurface.copy(alpha = 0.55f),
            markDiscRing = onSurface.copy(alpha = 0.55f),
            rain = listOf(Color(0xFF2988C8), Color(0xFF006FAC), Color(0xFF005A8E), Color(0xFF004470)),
            dot = Color(0xFF006FAC),
            chance = Color(0xFF006FAC),
            spent = surface.copy(alpha = 0.55f),
            stripMark = Rain.copy(alpha = 0.85f)
        )

        /** On the dark card: the scheme's inks, the plate white at 5% (the canvas's: a darker
         * veil does not show on the darkest ground), the rain lighter as it is stronger. */
        fun dark(onSurface: Color, onSurfaceVariant: Color, surface: Color) = PicturesInks(
            ink = onSurface,
            quiet = onSurfaceVariant,
            veil = Color.White.copy(alpha = 0.05f),
            hairline = Color.White.copy(alpha = 0.08f),
            disc = Color.White,
            plateDiscRing = Color.Black.copy(alpha = 0.55f),
            markDiscRing = Color.Black.copy(alpha = 0.55f),
            rain = listOf(Color(0xFF0078A8), Color(0xFF0092C8), Color(0xFF55BCEC), Rain),
            dot = Color(0xFF55BCEC),
            chance = Color(0xFF55BCEC),
            spent = surface.copy(alpha = 0.55f),
            stripMark = Rain.copy(alpha = 0.85f)
        )

    }
}
