package com.callbackdev.chiaro.ui.today

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import com.callbackdev.chiaro.ui.theme.SkyPalette

/**
 * What the pinned place row wears at a point of the scroll (DESIGN §8.1b, committente, 15 ott
 * 2026): a ground, an ink for the name and the hour, an ink for the pager's other dots, and
 * whether that ink is the light one (the status-bar icons follow it).
 */
internal data class PlaceBarColors(
    val ground: Color,
    val ink: Color,
    val dots: Color,
    val inkIsLight: Boolean
)

/**
 * The bar fades with the scroll, from the sky to the paper, instead of turning to paper at
 * once.
 *
 * [toPaper] is how far the page has travelled toward the bar: 0 with the sky under it, 1 when
 * the paper's edge reaches it. The ground runs from [skyTop] (the canvas' top stop under the
 * §3.6 scrim, the colour the bar stands on at rest) to [paper], **opaque**, so the sky never
 * shows through it and what the text stands on is this one colour. [opacity] only fades the
 * ground in over the first few dp, while it is still the scrimmed sky and white reads on it.
 *
 * The ink cannot simply blend the same way: halfway, a grey name on a grey ground. So the ink
 * blends while the blend still reads on the ground at [TextFloor] (in the dark theme, where
 * both ends are light on dark, that is the whole way), and otherwise it is whichever of the
 * two inks reads better — the sky's white or the theme's — which flips once, at the point
 * where the two are as good as each other. Swept over every sky top, both palettes, both themes
 * and the whole scroll (`PlaceBarColorsTest`), the worst is that flip in the light theme, 4.14:1
 * (sun at −10°, 47% of the way): past the 3:1 the 22sp name needs, a hair under the 4.5 of the
 * hour's line, for the instant the scroll passes it. Everywhere else it reads better.
 */
internal fun placeBarColors(
    skyTop: Color,
    paper: Color,
    paperInk: Color,
    paperDots: Color,
    toPaper: Float,
    opacity: Float
): PlaceBarColors {
    val f = toPaper.coerceIn(0f, 1f)
    val ground = lerp(skyTop, paper, f)
    val blended = lerp(SkyPalette.Ink, paperInk, f)
    val ink = when {
        f >= 1f -> paperInk
        contrast(blended, ground) >= TextFloor -> blended
        contrast(SkyPalette.Ink, ground) >= contrast(paperInk, ground) -> SkyPalette.Ink
        else -> paperInk
    }
    val inkIsLight = ink.luminance() > 0.5f
    return PlaceBarColors(
        ground = ground.copy(alpha = opacity.coerceIn(0f, 1f)),
        ink = ink,
        dots = if (inkIsLight && paperInk.luminance() <= 0.5f) SkyPalette.InkQuiet else paperDots,
        inkIsLight = inkIsLight
    )
}

/** WCAG's floor for a line of text. */
internal const val TextFloor = 4.5

/** The contrast ratio of two opaque colours. */
internal fun contrast(a: Color, b: Color): Double {
    val la = a.luminance().toDouble()
    val lb = b.luminance().toDouble()
    return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
}
