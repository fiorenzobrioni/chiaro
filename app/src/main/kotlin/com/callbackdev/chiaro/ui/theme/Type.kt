package com.callbackdev.chiaro.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.data.AppFont

/**
 * DESIGN.md §5. Inter, bundled as a variable font (OFL, `licenses/Inter-OFL.txt`)
 * rather than fetched from a font provider: a downloadable font is a runtime dependency
 * on Play Services, and an app that renders wrong on a de-Googled phone is an app that
 * renders wrong.
 *
 * Never a monospace. The terminal line owns that, and Chiaro must not read as its
 * sibling.
 */
// The variationSettings overload is still marked experimental. The opt-in is the
// whole point of bundling a VARIABLE font: without it Android synthesises the weights
// by smearing the outlines, which is exactly the look Inter was chosen to avoid.
@OptIn(ExperimentalTextApi::class)
private fun inter(weight: Int) = Font(
    resId = R.font.inter_variable,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight))
)

val InterFamily = FontFamily(
    inter(300), inter(400), inter(500), inter(600), inter(700)
)

/**
 * The other answer the reader may give ([AppFont.SYSTEM], 20 set 2026): whatever sans
 * the phone is wearing. It is `FontFamily.Default`, so it resolves to the device's own
 * typeface — Roboto on one phone, the OEM's on another, and on the phones with a font
 * picker the one the reader chose there.
 *
 * It is not the same KIND of value as [InterFamily] and the difference is the whole
 * argument of the setting: Inter is one font this app can measure, and this is a
 * different font on every device. Three things are therefore true of it and are not
 * true of the default, and each of them is a cost the reader is choosing to pay:
 *
 * - **The weights are the ones the device has.** Where a weight is missing Android
 *   synthesises it, which is the smearing the comment above says Inter was bundled to
 *   avoid; [HeroWeight] at 300 is the line most likely to meet it.
 * - **[Tabular] may do nothing.** `tnum` on a font that does not carry the feature is
 *   ignored in silence — no error, just columns of figures that stop being a column.
 *   Inter has it; a given system font may not.
 * - **The columns measured in dp were measured against Inter** (`TextScale.kt`: the
 *   week row's 44/36/34/34, the hour cell, the timeline's clock). A wider face wraps
 *   them sooner. Nothing clips — this app has no `maxLines` — but a value can reflow a
 *   step earlier than the measurement says.
 *
 * All three are why [AppFont.INTER] is the default and this is the choice.
 */
val SystemFamily = FontFamily.Default

internal fun familyFor(font: AppFont): FontFamily = when (font) {
    AppFont.INTER -> InterFamily
    AppFont.SYSTEM -> SystemFamily
}

/** Figures that sit in a column are tabular, always: proportional digits in a column are
 * the typographic equivalent of a wobbling table, and this app is mostly columns. */
private const val Tabular = "tnum"

/** The one weight the type scale states twice (the hero and the reading): light, because
 * a number in a body weight reads as a headline instead of as a reading. */
private val HeroWeight = FontWeight.Light

/**
 * The scale of DESIGN §5, in a family. Every role is named: a role left out would take
 * Material's own default family, which is the platform sans, and the app would be set in
 * two typefaces without anybody deciding that (`TypographyFamilyTest` counts them).
 */
internal fun typographyFor(family: FontFamily): Typography = Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontFamily = family),
        displayMedium = displayMedium.copy(fontFamily = family),
        displaySmall = displaySmall.copy(fontFamily = family, fontSize = 36.sp, lineHeight = 44.sp),
        headlineLarge = headlineLarge.copy(fontFamily = family),
        headlineMedium = headlineMedium.copy(fontFamily = family),
        headlineSmall = headlineSmall.copy(fontFamily = family),
        titleLarge = titleLarge.copy(
            fontFamily = family, fontSize = 22.sp, lineHeight = 28.sp,
            fontWeight = FontWeight.Medium
        ),
        titleMedium = titleMedium.copy(
            fontFamily = family, fontSize = 16.sp, lineHeight = 24.sp,
            fontWeight = FontWeight.SemiBold
        ),
        titleSmall = titleSmall.copy(fontFamily = family),
        bodyLarge = bodyLarge.copy(fontFamily = family),
        bodyMedium = bodyMedium.copy(fontFamily = family),
        bodySmall = bodySmall.copy(fontFamily = family),
        labelLarge = labelLarge.copy(fontFamily = family),
        labelMedium = labelMedium.copy(fontFamily = family),
        labelSmall = labelSmall.copy(fontFamily = family)
    )
}

/** The scale in Inter: the default, and what the design system is measured against. */
val ChiaroTypography: Typography = typographyFor(InterFamily)

private val SystemTypography: Typography = typographyFor(SystemFamily)

/** Both scales are built once and picked, never rebuilt per composition: a `Typography`
 * is fifteen `TextStyle`s and the reader changes this setting about once. */
fun chiaroTypography(font: AppFont): Typography = when (font) {
    AppFont.INTER -> ChiaroTypography
    AppFont.SYSTEM -> SystemTypography
}

/**
 * The two type roles Material does not have, in whichever family the reader chose.
 *
 * They are a [ChiaroType] rather than two top-level values because the family is now a
 * setting: a `val` built at class-init time could only ever hold one answer, and the two
 * places that use these styles ([LocalChiaroType] at Today's hero and the metric tile)
 * would have gone on printing Inter under a reader who asked for the system font. The
 * theme provides them the way it provides [LocalChiaroColors].
 */
@Immutable
data class ChiaroType(
    /**
     * The current temperature is the largest thing on the screen and it is a number, so
     * it is light, tight and tabular — `displayLarge` with a body weight would read as a
     * headline instead of as a reading.
     */
    val heroTemperature: TextStyle,
    /**
     * A reading in a tile (DESIGN §8.6, 8 set 2026): the same voice as [heroTemperature]
     * at a tile's scale — light, tabular, 24sp on a 32sp line. Until then the value stood
     * at `titleMedium`, 16sp against a 14sp label and a 34dp icon, and the eye went to
     * the icon; a reading has to be the first thing seen, and the argument for the hero's
     * weight holds here too: a number in a body weight reads as a headline, not as a
     * reading.
     */
    val readingValue: TextStyle
)

private fun chiaroTypeFor(family: FontFamily) = ChiaroType(
    heroTemperature = TextStyle(
        fontFamily = family,
        fontWeight = HeroWeight,
        fontSize = 64.sp,
        lineHeight = 68.sp,
        fontFeatureSettings = Tabular
    ),
    readingValue = TextStyle(
        fontFamily = family,
        fontWeight = HeroWeight,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        fontFeatureSettings = Tabular
    )
)

private val InterType = chiaroTypeFor(InterFamily)

private val SystemType = chiaroTypeFor(SystemFamily)

internal fun chiaroType(font: AppFont): ChiaroType = when (font) {
    AppFont.INTER -> InterType
    AppFont.SYSTEM -> SystemType
}

/** Inter by default, so a preview or any composable outside [ChiaroTheme] still reads the
 * app's own voice rather than the host's. */
val LocalChiaroType = staticCompositionLocalOf { InterType }

/** Any style, with the figures made tabular. */
fun TextStyle.tabular(): TextStyle = copy(fontFeatureSettings = Tabular)
