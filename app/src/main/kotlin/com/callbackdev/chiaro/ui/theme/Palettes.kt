package com.callbackdev.chiaro.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import com.callbackdev.chiaro.data.AppPalette

/**
 * One of the app's two dresses (DESIGN.md §2.5), gathered so that "which palette" is a
 * single value passed around rather than four parallel `if`s that can disagree.
 *
 * The pairing matters: a scheme, its semantic tokens and its sky were measured together,
 * and mixing the vivid sky with the paper ramps would be a third palette nobody
 * measured.
 */
@Immutable
data class ChiaroPalette(
    val lightScheme: ColorScheme,
    val darkScheme: ColorScheme,
    val lightColors: ChiaroColors,
    val darkColors: ChiaroColors,
    val sky: SkyPalette
) {
    fun scheme(dark: Boolean): ColorScheme = if (dark) darkScheme else lightScheme
    fun colors(dark: Boolean): ChiaroColors = if (dark) darkColors else lightColors
}

/** Warm paper and amber: the identity, and the default. */
internal val PaperPalette = ChiaroPalette(
    lightScheme = ChiaroLightScheme,
    darkScheme = ChiaroDarkScheme,
    lightColors = ChiaroLightColors,
    darkColors = ChiaroDarkColors,
    sky = SkyPalette.Paper
)

/** Daylight white and azure, every token at the gamut edge its luminance allows. */
internal val VividPalette = ChiaroPalette(
    lightScheme = VividLightScheme,
    darkScheme = VividDarkScheme,
    lightColors = VividLightColors,
    darkColors = VividDarkColors,
    sky = SkyPalette.Vivid
)

/** The reader's choice, resolved. Both palettes, so a sweep test can walk them. */
val ChiaroPalettes: Map<AppPalette, ChiaroPalette> = mapOf(
    AppPalette.PAPER to PaperPalette,
    AppPalette.VIVID to VividPalette
)

fun paletteFor(choice: AppPalette): ChiaroPalette = ChiaroPalettes.getValue(choice)

/**
 * The dress itself, not its colors: the one thing that cannot be read off
 * `colorScheme` or [LocalChiaroColors], because it decides which *assets* the app
 * reaches for — the icon sets of §13.1, which are files rather than values.
 */
val LocalAppPalette = staticCompositionLocalOf { AppPalette.PAPER }
