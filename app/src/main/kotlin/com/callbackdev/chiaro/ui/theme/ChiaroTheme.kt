package com.callbackdev.chiaro.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import com.callbackdev.chiaro.data.AppPalette

/**
 * The entry point of the design system (DESIGN.md §12).
 *
 * Dynamic color is ON by default (§4.1): a weather app that takes the reader's own
 * wallpaper is a weather app that belongs on their phone. [dynamicColor] false gives one
 * of the two generated schemes instead — the setting exists for readers who want the app
 * to look like itself, and it is what the store screenshots use, since a wallpaper-derived
 * scheme would make every screenshot a different app.
 *
 * [palette] picks WHICH of the two (§2.5). It is not the same question as dynamic color
 * and it is not subordinate to it: the semantic palette of §2.3 and the sky canvas of §3
 * never followed the wallpaper (see [ChiaroColors]), so the reader's choice of dress
 * still decides the verdicts, the quantity ramps and the sky even while Material's roles
 * are coming from their photo of a sunset.
 *
 * It also carries the reader's motion setting (§7, [LocalReducedMotion]): a theme is
 * where the app asks the system what it prefers, and motion is one of those answers.
 */
@Composable
fun ChiaroTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    palette: AppPalette = AppPalette.PAPER,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val dress = paletteFor(palette)
    val colorScheme = when {
        // minSdk is 33, so the S check is always true; it stays because lint reads it
        // as the contract it is, and because the day this app supports an older API
        // the compiler should not be the last to know.
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

        else -> dress.scheme(darkTheme)
    }

    CompositionLocalProvider(
        LocalAppPalette provides palette,
        LocalChiaroColors provides dress.colors(darkTheme),
        LocalSkyPalette provides dress.sky,
        // §7: the reader's answer to "less motion", read once here and asked at every
        // place the app moves. Live, because the toggle lives outside the app.
        LocalReducedMotion provides rememberReducedMotion(context)
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = ChiaroTypography,
            shapes = ChiaroShapes,
            content = content
        )
    }
}
