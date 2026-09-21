package com.callbackdev.chiaro.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * The card colours a home-screen widget can wear (committente, 19 set 2026: «possibilità
 * di mettere uno sfondo colorato: blu, blu chiaro, verde…»), DESIGN §2.6.
 *
 * They are **not** roles and they are not derived from the reader's dress: a widget's card
 * is furniture on somebody's wallpaper, and the reader is choosing what colour that piece
 * of furniture is, the way they already choose between the sky, light, dark and the
 * system. That is a different act from the app's own colour, which is generated and
 * semantic all the way down (§2.1). So this table is hexes, and it lives here because
 * `ui/theme/` is the one place hexes are allowed to live (`NoRawColorTest`).
 *
 * **Every one of the six is a dark ground, and that is the whole design.** Ink and ground
 * are a pair (§2.3, and the Sky card's verdicts measured it on a device on 4 set: a bare
 * colour on a card whose ground the app does not control was unreadable), so a card colour
 * that shipped without its ink would be half a decision. Rather than six new ink triples,
 * every colour is picked dark enough to carry the pair the app already has and has already
 * measured — **the §3.6 white over the scrimmed sky**, white at full strength, 75% for the
 * quiet ink and 85% for the freshness one. Measured on every colour, and asserted by
 * `PaletteContrastTest`: white is never under 7.8:1, the quiet ink never under 5.2:1 and
 * the freshness ink never under 6.1:1, so even the 11 sp lines clear the 4.5:1 that small
 * text needs with room to spare.
 *
 * Six and not twenty: a list of colours is a list somebody has to scroll, and past about
 * six the choice stops being a choice and becomes a swatch book. These are one blue, one
 * lighter blue (the committente named both), one green, one green-blue, one violet and one
 * warm earth, and «far enough apart» is measured too — no two are closer than 13 ΔE, so a
 * reader picking one over another is picking a colour and not a word. The blue is deeper
 * than the first draft's `#14477A` for exactly that reason: against the lighter blue beside
 * it, it came in at 9.4 and the two names were doing the work.
 */
enum class WidgetCardColor { BLUE, AZURE, GREEN, TEAL, PLUM, CLAY }

/**
 * The ground each colour paints, at full solidity. The reader's opacity thins it exactly
 * as it thins the sky, and the ink stays at full strength either way — which is also why
 * a see-through coloured card hands the ink question back to the wallpaper, like the sky
 * and the system card do and unlike light and dark, which are the reader naming an ink
 * (`widgetInk`).
 *
 * The numbers are printed in DESIGN §2.6 and `PaletteDocTest` reads them from there, so a
 * colour re-picked here without being re-measured there fails the build.
 */
fun widgetCardContainer(color: WidgetCardColor): Color = when (color) {
    WidgetCardColor.BLUE -> Color(0xFF0F3B6B)
    WidgetCardColor.AZURE -> Color(0xFF0F5580)
    WidgetCardColor.GREEN -> Color(0xFF17572E)
    WidgetCardColor.TEAL -> Color(0xFF0F5B5B)
    WidgetCardColor.PLUM -> Color(0xFF4A2C63)
    WidgetCardColor.CLAY -> Color(0xFF7A3320)
}
