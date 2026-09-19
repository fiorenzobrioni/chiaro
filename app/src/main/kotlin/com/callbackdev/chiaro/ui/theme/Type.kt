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
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.data.AppFont

/**
 * DESIGN.md §5. The app's type, in the family the reader chose ([AppFont]).
 *
 * Both faces on offer are **bundled as variable fonts** (OFL, `licenses/`) rather than
 * fetched from a font provider: a downloadable font is a runtime dependency on Play
 * Services, and an app that renders wrong on a de-Googled phone is an app that renders
 * wrong. The third answer takes the phone's own sans, which is a different bargain and
 * says so at [SystemFamily].
 *
 * Never a monospace. The terminal line owns that, and Chiaro must not read as its
 * sibling.
 */
// The variationSettings overload is still marked experimental. The opt-in is the
// whole point of bundling a VARIABLE font: without it Android synthesises the weights
// by smearing the outlines, which is exactly the look Inter was chosen to avoid.
@OptIn(ExperimentalTextApi::class)
private fun variable(resId: Int, weight: Int) = Font(
    resId = resId,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight))
)

private fun inter(weight: Int) = variable(R.font.inter_variable, weight)

val InterFamily = FontFamily(
    inter(300), inter(400), inter(500), inter(600), inter(700)
)

private fun googleSans(weight: Int) = variable(R.font.google_sans_variable, weight)

/**
 * The second bundled family ([AppFont.GOOGLE_SANS], 20 set 2026): Google Sans, OFL 1.1,
 * imported and cut down to what this app prints by `tools/import_google_sans.py`. Like
 * the weather drawings, **the file under `res/font/` is not edited by hand — re-running
 * the tool IS the import**, and the tool's header holds the provenance, the hashes and
 * every value it pins.
 *
 * **Four weights, not five, and that is the font's own doing**: its `wght` axis starts
 * at 400. Inter's goes down to 100, this one does not go below Regular, so this family
 * declares exactly the faces the file can draw and a request for Light lands on 400 by
 * Compose's own nearest-weight rule. Declaring a 300 face here would have been asking
 * the rasteriser to invent one, which is the smearing the comment above exists to avoid.
 *
 * It is the answer to the thing the system font could not do (committente, 20 set 2026:
 * «invece di avere un font di sistema che cambia di marca in marca forse meglio provare
 * un font fisso oltre Inter»): a second face that is the same drawing on every phone,
 * and — since it is the type Google's own apps are set in — one that reads as if it
 * belonged to the phone without being hostage to who made the phone.
 */
val GoogleSansFamily = FontFamily(
    googleSans(400), googleSans(500), googleSans(600), googleSans(700)
)

/**
 * The third answer ([AppFont.SYSTEM], 20 set 2026): whatever sans the phone is wearing.
 * It is `FontFamily.Default`, so it resolves to the device's own typeface — Roboto on
 * one phone, the OEM's on another, and on the phones with a font picker the one the
 * reader chose there.
 *
 * It is not the same KIND of value as the two bundled families, and the difference is
 * the whole argument of the setting: those are fonts this app can measure, and this is a
 * different font on every device. It is kept because it is still the only answer that
 * matches the home-screen cards exactly, which is where this question started.
 *
 * Three things are true of it that are not true of a bundled family, and each is a
 * cost the reader is choosing to pay:
 *
 * - **The weights are the ones the device has.** Where a weight is missing Android
 *   synthesises it, which is the smearing the comment above says Inter was bundled to
 *   avoid; [ReadingWeight] at 300 is the line most likely to meet it.
 * - **[Tabular] may do nothing.** `tnum` on a font that does not carry the feature is
 *   ignored in silence — no error, just columns of figures that stop being a column.
 *   Both bundled families have it, checked on the files themselves; a given system
 *   font may not.
 * - **The columns measured in dp were measured against Inter** (`TextScale.kt`: the
 *   week row's 44/36/34/34, the hour cell, the timeline's clock). A wider face wraps
 *   them sooner. Nothing clips — this app has no `maxLines` — but a value can reflow a
 *   step earlier than the measurement says.
 *
 * All three are why a bundled family is the default and this is only ever a choice.
 */
val SystemFamily = FontFamily.Default

internal fun familyFor(font: AppFont): FontFamily = when (font) {
    AppFont.INTER -> InterFamily
    AppFont.GOOGLE_SANS -> GoogleSansFamily
    AppFont.SYSTEM -> SystemFamily
}

/** Figures that sit in a column are tabular, always: proportional digits in a column are
 * the typographic equivalent of a wobbling table, and this app is mostly columns. */
private const val Tabular = "tnum"

/**
 * **The hero's weight, bold since 20 set 2026** (committente, on the device: «possiamo
 * anche provare a mettere in un bel bold la temperatura attuale»), from Light 300.
 *
 * The old argument was that a number in a body weight reads as a headline rather than as
 * a reading, and at a tile's 24sp it still holds — [ReadingWeight] below is unchanged.
 * At 64sp it does not: that number is not one reading among others, it is the thing the
 * screen is FOR, and a hairline 64sp figure reads as an ornament laid over the sky rather
 * than as the temperature. The card on the home screen has printed it Bold since the day
 * it shipped, and the app disagreeing with its own widget about the same number was the
 * observation that opened all of this.
 *
 * Bold is also the one weight both bundled families draw the same way round: Google Sans
 * starts at 400, so the old 300 was never going to survive a family switch intact.
 */
private val HeroWeight = FontWeight.Bold

/**
 * The hero's tracking: **−0.02 em, which is −1.28sp at 64**.
 *
 * Bold at display size needs negative tracking or the figures sit in their own way — the
 * default spacing is drawn for a paragraph, not for four glyphs that fill a quarter of
 * the screen. Inter's own tracking formula settles at about −0.022 em by this size;
 * this stops just short of it because the same number has to sit in Google Sans too,
 * whose rounder shapes close up sooner. Em and not sp so it stays proportional when the reader
 * scales their type.
 */
private val HeroTracking = (-0.02).em

/** The tile reading stays light (DESIGN §8.6): at 24sp the old argument is still the
 * right one. In Google Sans it lands on 400, the lightest that family has — see
 * [GoogleSansFamily]. */
private val ReadingWeight = FontWeight.Light

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

private val GoogleSansTypography: Typography = typographyFor(GoogleSansFamily)

private val SystemTypography: Typography = typographyFor(SystemFamily)

/** The three scales are built once and picked, never rebuilt per composition: a
 * `Typography` is fifteen `TextStyle`s and the reader changes this setting about once. */
fun chiaroTypography(font: AppFont): Typography = when (font) {
    AppFont.INTER -> ChiaroTypography
    AppFont.GOOGLE_SANS -> GoogleSansTypography
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
     * it is bold, tracked in and tabular ([HeroWeight], [HeroTracking]): `displayLarge`
     * would give it a paragraph's weight and a paragraph's spacing at four times a
     * paragraph's size.
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
        letterSpacing = HeroTracking,
        fontFeatureSettings = Tabular
    ),
    readingValue = TextStyle(
        fontFamily = family,
        fontWeight = ReadingWeight,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        fontFeatureSettings = Tabular
    )
)

private val InterType = chiaroTypeFor(InterFamily)

private val GoogleSansType = chiaroTypeFor(GoogleSansFamily)

private val SystemType = chiaroTypeFor(SystemFamily)

internal fun chiaroType(font: AppFont): ChiaroType = when (font) {
    AppFont.INTER -> InterType
    AppFont.GOOGLE_SANS -> GoogleSansType
    AppFont.SYSTEM -> SystemType
}

/** Inter by default, so a preview or any composable outside [ChiaroTheme] still reads the
 * app's own voice rather than the host's. */
val LocalChiaroType = staticCompositionLocalOf { InterType }

/** Any style, with the figures made tabular. */
fun TextStyle.tabular(): TextStyle = copy(fontFeatureSettings = Tabular)
