package com.callbackdev.chiaro.widget

import android.app.WallpaperColors
import android.app.WallpaperManager
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider as FixedColorProvider
import com.callbackdev.chiaro.MainActivity
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.data.AppPalette
import com.callbackdev.chiaro.domain.settings.UnitSettings
import com.callbackdev.chiaro.domain.sky.SkyVerdictKind
import com.callbackdev.chiaro.ui.format.Formats
import com.callbackdev.chiaro.ui.theme.ChiaroColors
import com.callbackdev.chiaro.ui.theme.ChiaroPalette
import com.callbackdev.chiaro.ui.theme.SkyPalette
import com.callbackdev.chiaro.ui.theme.paletteFor
import com.callbackdev.chiaro.ui.today.SkySnapshot
import java.time.Duration
import java.time.Instant
import java.util.Locale

/**
 * The widgets' side of the design system (Fase 8, redrawn on device review): the
 * default dress is the app's OWN hero — the computed sky gradient with the measured
 * scrim baked in and white ink over it, exactly the §3.6 contract the canvas keeps.
 * The alternatives are a plain card in the app's schemes (dynamic or Chiaro), fixed
 * light, fixed dark, or following the system — each widget chooses for itself.
 */
data class WidgetSchemes(
    val light: ColorScheme,
    val dark: ColorScheme,
    /**
     * The dress the reader picked (DESIGN §2.5). It rides along even when the two
     * schemes above came from the wallpaper, because the semantic tokens and the sky
     * never followed the wallpaper in the first place (§2.3, §3.7) — so a widget under
     * dynamic color still paints the reader's ramps, verdicts and sky.
     */
    val dress: ChiaroPalette
)

fun widgetSchemes(context: Context, dynamicColor: Boolean, palette: AppPalette): WidgetSchemes {
    val dress = paletteFor(palette)
    return if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        WidgetSchemes(dynamicLightColorScheme(context), dynamicDarkColorScheme(context), dress)
    } else {
        WidgetSchemes(dress.lightScheme, dress.darkScheme, dress)
    }
}

/**
 * The model this widget draws, re-read INSIDE the composition whenever [WidgetRefresh]
 * ticks. [initial] is what `provideGlance` already loaded and [loadedAt] the revision it
 * loaded at, so the first frame costs nothing extra and only a real change re-reads.
 *
 * This is the whole of the Fase 8 repaint fix (device report, 4 set): Glance keeps a
 * composition alive for about forty-five seconds and `update()` does not restart
 * `provideGlance`, so a widget that loads its data before `provideContent` and observes
 * nothing afterwards repaints its OWN old numbers. [WidgetRefresh] carries the reason.
 */
@Composable
fun rememberWidgetModel(
    context: Context,
    appWidgetId: Int,
    initial: WidgetModel,
    loadedAt: Long
): WidgetModel {
    val revision by WidgetRefresh.revision.collectAsState()
    val model by produceState(initial, revision) {
        if (revision != loadedAt) value = WidgetData.load(context, appWidgetId)
    }
    return model
}

/** The sky dress for [model], recomputed only when the sky or the look really moves —
 * it is a 320×320 bitmap, and a recomposition is not a reason to allocate another. */
@Composable
fun rememberSkyBitmap(model: WidgetModel): Bitmap? {
    val sky = model.content?.sky?.takeIf { model.look.background == WidgetBackground.SKY }
    val opacity = model.look.opacityPct
    val table = paletteFor(model.settings.palette).sky
    return remember(sky, opacity, table) { sky?.let { skyGradientBitmap(it, opacity, table) } }
}

/** The two schemes this widget writes in, rebuilt only when the choice behind them does. */
@Composable
fun rememberWidgetSchemes(
    context: Context,
    dynamicColor: Boolean,
    palette: AppPalette
): WidgetSchemes = remember(dynamicColor, palette) { widgetSchemes(context, dynamicColor, palette) }

/** The inks a widget writes with, resolved once per background choice, plus the one
 * fact a quantity ramp needs about the ground they all sit on. */
data class WidgetPalette(
    val primary: androidx.glance.unit.ColorProvider,
    val secondary: androidx.glance.unit.ColorProvider,
    val stale: androidx.glance.unit.ColorProvider,
    /** Whether the effective ground is dark — the scrimmed sky, a dark card, a dark
     * wallpaper behind a see-through card — so ramps pick the set that was SELECTED
     * for dark rather than the light one flipped (DESIGN §2.3). */
    val darkGround: Boolean,
    /** The reader's palette, carried so the ramps and verdicts on this card come from
     * the same dress as the app's (§2.5). */
    val dress: ChiaroPalette
) {
    /** The §2.3 set for the ground this card really has. */
    val colors: ChiaroColors get() = dress.colors(darkGround)
}

/**
 * The system's answer at render time: is the phone in night mode right now? Glance's
 * own day/night providers are resolved by the LAUNCHER, and a host that flips the
 * card without flipping the words leaves dark ink on a dark card (device report,
 * 3 set). Resolving every color here against one configuration makes that split
 * impossible; the Application repaints on every configuration change, so the answer
 * can only go stale while the process is dead, and the next sync corrects it.
 */
fun isNight(context: Context): Boolean =
    (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
        Configuration.UI_MODE_NIGHT_YES

/**
 * Whether the wallpaper behind a see-through card can carry DARK text, by the system's
 * own account: [WallpaperColors.HINT_SUPPORTS_DARK_TEXT], which the platform sets when
 * it has looked at the image and found it bright enough. A see-through card has no
 * ground of its own, and the phone's THEME is a bad proxy for what is behind it — a
 * light theme over a near-black wallpaper is common, and it made theme-following ink
 * invisible (device screenshot, 3 set).
 *
 * The hint is an AFFIRMATIVE signal and is read as one (device report, 4 set): dark ink
 * only when the system says the ground is bright, light ink in every other case — hint
 * absent, colors not extracted yet, a live wallpaper that answers nothing at all, or the
 * call failing outright. The 3-set version fell back to the theme when it got no answer,
 * and that is exactly the reported failure: a light theme over a black wallpaper wrote
 * black on black, while the same phone in dark mode was fine. Falling back the other way
 * cannot produce that, because a bright wallpaper is precisely the case the hint exists
 * to announce.
 *
 * The lock screen is asked as a second source: on the phones where the home wallpaper's
 * colors are unavailable the two are usually the same image, and one more answer is
 * better than none.
 */
fun wallpaperWantsDarkInk(context: Context): Boolean {
    val manager = runCatching { WallpaperManager.getInstance(context) }.getOrNull() ?: return false
    val colors = wallpaperColors(manager, WallpaperManager.FLAG_SYSTEM)
        ?: wallpaperColors(manager, WallpaperManager.FLAG_LOCK)
        ?: return false
    return colors.colorHints and WallpaperColors.HINT_SUPPORTS_DARK_TEXT != 0
}

private fun wallpaperColors(manager: WallpaperManager, which: Int): WallpaperColors? =
    runCatching { manager.getWallpaperColors(which) }.getOrNull()

/** [widgetInk]'s answer, dressed in the colors it names. The decision itself is pure
 * and lives next door precisely so a test can pin it (`WidgetInkTest`). */
private fun palette(ink: WidgetInk, schemes: WidgetSchemes): WidgetPalette = when (ink) {
    // White over the scrimmed gradient: the §3.6 numbers, reused as-is.
    WidgetInk.OVER_SKY -> WidgetPalette(
        primary = FixedColorProvider(Color.White),
        secondary = FixedColorProvider(Color.White.copy(alpha = 0.75f)),
        stale = FixedColorProvider(Color.White.copy(alpha = 0.85f)),
        darkGround = ink.darkGround,
        dress = schemes.dress
    )
    WidgetInk.ON_LIGHT -> WidgetPalette(
        primary = FixedColorProvider(schemes.light.onSurface),
        secondary = FixedColorProvider(schemes.light.onSurfaceVariant),
        stale = FixedColorProvider(schemes.dress.lightColors.freshness.ink),
        darkGround = ink.darkGround,
        dress = schemes.dress
    )
    WidgetInk.ON_DARK -> WidgetPalette(
        primary = FixedColorProvider(schemes.dark.onSurface),
        secondary = FixedColorProvider(schemes.dark.onSurfaceVariant),
        stale = FixedColorProvider(schemes.dress.darkColors.freshness.ink),
        darkGround = ink.darkGround,
        dress = schemes.dress
    )
}

/** The card's inner padding: the roomy default, and the tighter dress for the
 * one-cell widgets, whose bigger icon needs the air (device review, 3 set). */
val WidgetCardPadding = 14.dp
val WidgetCardPaddingTight = 12.dp

/** The air the Now widget's hero leaves above and below itself: it is one row, so
 * the glyph may own almost the whole height (4th device pass, measured). */
val WidgetCardPaddingSnug = 6.dp

/**
 * The Now widget's two horizontal insets, which are neither the vertical one nor each
 * other (committente, 7 set: the icon a little too close to the leading edge, the high
 * and the low decidedly close to the trailing one).
 *
 * The two edges carry different things, so one number was never going to be right for
 * both. Text has almost no side bearing, so at [WidgetCardPaddingSnug] the low's degree
 * sign really did stop 6 dp from the card; it takes [WidgetCardPaddingTight], the inset
 * the other one-cell widget already writes at. A Meteocons glyph brings a margin of its
 * own — measured by rasterising all 160 condition drawables, 6.5/64 of the box at the
 * tightest (`partly_cloudy_day`) and 9/64 at the median, which is 9 to 12.5 dp at the
 * ~89 dp box the launcher's one-cell grant leaves. So the leading edge does not need
 * the same number: it needs the difference.
 *
 * 8 dp is that difference, read off the vertical. The same glyph margin applies above
 * and below, so the hero's ink already sits 15 to 18.5 dp from the card's top and
 * bottom; 8 + 9…12.5 lands it 17 to 20.5 dp from the leading edge, the extra couple of
 * dp being what the 24 dp corner takes back on a side the vertical does not have. The
 * icon ends up inset by its own INK rather than by its bounding box, which is the only
 * inset the eye can see.
 *
 * The cost is 14 dp of the words' column, and the one thing that has to fit in it is
 * the optional high/low: "23°" and "27° / 16°" come to about 117 dp, against 123 dp of
 * column on a 240 dp grant. It was already too tight at 216 dp before this change and
 * it still is — which is what [R.string.widget_config_show_range] is for.
 */
val WidgetCardPaddingLeading = 8.dp
val WidgetCardPaddingTrailing = WidgetCardPaddingTight

/**
 * The card every widget lives in. The sky dress is a bitmap of the same gradient the
 * app's canvas computes for this exact moment, darkened by the §3.6 scrim so white
 * ink clears its measured floor; the reader's opacity scales gradient and scrim
 * together, down to fully see-through — the ink always stays full strength. With no
 * report yet there is no sky to show, so SKY falls back to the system card rather
 * than inventing a weather-less gradient.
 */
@Composable
fun WidgetCard(
    model: WidgetModel,
    schemes: WidgetSchemes,
    skyBitmap: Bitmap?,
    contentPadding: Dp = WidgetCardPadding,
    /**
     * The two horizontal edges on their own, because what sits against them is not
     * the same thing: a Meteocons glyph carries a margin of its own and text carries
     * none. The Now widget is the one card that needs the distinction — see
     * [WidgetCardPaddingLeading] for the measured numbers.
     */
    contentPaddingStart: Dp = contentPadding,
    contentPaddingEnd: Dp = contentPadding,
    content: @Composable (WidgetPalette) -> Unit
) {
    val look = model.look
    val effectiveBackground =
        if (look.background == WidgetBackground.SKY && skyBitmap == null) {
            WidgetBackground.SYSTEM
        } else {
            look.background
        }
    val alpha = look.opacityPct / 100f
    val context = LocalContext.current
    val night = isNight(context)
    // The wallpaper is asked only when the card is see-through enough to matter AND
    // the reader has not already named an ink by picking a light or a dark card: it is
    // a binder round-trip, and [widgetInk] would throw the answer away in every other
    // case anyway.
    val delegatesInk = effectiveBackground == WidgetBackground.SKY ||
        effectiveBackground == WidgetBackground.SYSTEM
    val wallpaperCarriesDarkInk = look.opacityPct < InkTrustFloorPct && delegatesInk &&
        wallpaperWantsDarkInk(context)
    val ink = widgetInk(effectiveBackground, look.opacityPct, night, wallpaperCarriesDarkInk)
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .cornerRadius(24.dp)
            .clickable(actionStartActivity<MainActivity>())
    ) {
        if (effectiveBackground == WidgetBackground.SKY && skyBitmap != null) {
            Image(
                provider = ImageProvider(skyBitmap),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = GlanceModifier.fillMaxSize()
            )
        } else {
            val fill = when (effectiveBackground) {
                WidgetBackground.LIGHT ->
                    FixedColorProvider(schemes.light.surface.copy(alpha = alpha))
                WidgetBackground.DARK ->
                    FixedColorProvider(schemes.dark.surface.copy(alpha = alpha))
                else -> FixedColorProvider(
                    (if (night) schemes.dark.surface else schemes.light.surface)
                        .copy(alpha = alpha)
                )
            }
            Box(modifier = GlanceModifier.fillMaxSize().background(fill)) {}
        }
        Box(
            modifier = GlanceModifier.fillMaxSize().padding(
                start = contentPaddingStart,
                top = contentPadding,
                end = contentPaddingEnd,
                bottom = contentPadding
            )
        ) {
            content(palette(ink, schemes))
        }
    }
}

/**
 * The sky, rendered: the canvas' three stops top-to-bottom, the §3.6 scrim over the
 * whole of it (uniform here — every pixel of a widget can carry text), both scaled
 * by the widget's opacity so transparency thins the sky, never the words.
 */
fun skyGradientBitmap(sky: SkySnapshot, opacityPct: Int, table: SkyPalette): Bitmap {
    val gradient = table.gradient(
        sunAltitudeDeg = sky.sunAltitudeDeg,
        cloudPct = sky.cloudPct,
        precipPct = sky.precipPct,
        moonIllumination = sky.moonIllumination,
        moonAltitudeDeg = sky.moonAltitudeDeg
    )
    val size = 320
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val alpha = (opacityPct.coerceIn(0, 100) * 255) / 100
    val paint = Paint().apply {
        shader = LinearGradient(
            0f, 0f, 0f, size.toFloat(),
            intArrayOf(
                gradient.top.toArgb(), gradient.mid.toArgb(), gradient.bottom.toArgb()
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        this.alpha = alpha
    }
    canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
    val scrim = Paint().apply {
        color = SkyPalette.ScrimColor.toArgb()
        this.alpha = (SkyPalette.ScrimAlpha * alpha).toInt()
    }
    canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), scrim)
    return bitmap
}

/**
 * The day's high and low, as the trailing edge of a hero row (committente, 4 set).
 *
 * High first and in the strong ink, low after it and dimmed — which is the app's own
 * emphasis, not a borrowed convention: the week's rows print the low in
 * `onSurfaceVariant` and the high in the plain one for exactly this reason, so the pair
 * says which is which without a word for it. Nothing is drawn at all when the report
 * has no day left to describe (§1.1).
 *
 * Tabular figures are not available to Glance, so the pair is three Texts rather than
 * one: it is also the only way to give the two numbers two inks.
 */
@Composable
fun DayRange(highC: Double, lowC: Double, units: UnitSettings, palette: WidgetPalette) {
    val locale = Locale.getDefault()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = Formats.temperature(highC, units.temperature, locale),
            style = TextStyle(
                color = palette.primary,
                fontSize = DayRangeSp,
                fontWeight = FontWeight.Medium
            ),
            maxLines = 1
        )
        // Punctuation, not prose: a slash between two temperatures reads the same in
        // every language this app speaks, so it stays in the code.
        Text(
            text = " / ",
            style = secondaryStyle(palette, DayRangeSp),
            maxLines = 1
        )
        Text(
            text = Formats.temperature(lowC, units.temperature, locale),
            style = secondaryStyle(palette, DayRangeSp),
            maxLines = 1
        )
    }
}

/** The pair sits at the place name's size: it is the same order of fact. */
private val DayRangeSp = 15.sp

/**
 * The empty states sit in the MIDDLE of the card, not at the top of it (committente,
 * 7 set — on the Now widget the "no forecast yet" line was jammed into the top corner
 * of a card that was otherwise empty).
 *
 * A card with content has a shape that decides where the words go: the hero row is
 * centred on the Now widget, the header is the top of a full column on Today and Sky.
 * A card with one or two lines in it has no such shape, and left at the top those lines
 * read like content that failed to finish loading rather than a message. Centred on the
 * card's own axis they read as the message they are — and it is the same treatment on
 * all three widgets, which is the other half of the report.
 */
private val EmptyStateModifier: GlanceModifier get() = GlanceModifier.fillMaxSize()

/** The honest empty state: no place yet, and the tap that fixes it. */
@Composable
fun NoPlaceContent(palette: WidgetPalette) {
    val context = LocalContext.current
    Column(
        modifier = EmptyStateModifier,
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        Text(
            text = context.getString(R.string.empty_no_place_title),
            style = TextStyle(
                color = palette.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        )
        Text(
            text = context.getString(R.string.widget_no_place_hint),
            style = TextStyle(color = palette.secondary, fontSize = 12.sp)
        )
    }
}

/** A place with no report yet: said plainly, never a grey zero (DESIGN §1.1). */
@Composable
fun NoDataContent(palette: WidgetPalette) {
    val context = LocalContext.current
    Column(
        modifier = EmptyStateModifier,
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        Text(
            text = context.getString(R.string.widget_no_data),
            style = TextStyle(color = palette.secondary, fontSize = 12.sp)
        )
    }
}

@Composable
fun secondaryStyle(palette: WidgetPalette, size: TextUnit): TextStyle =
    TextStyle(color = palette.secondary, fontSize = size)

/**
 * The place a card is showing, with the position pin before the name when that place
 * is the phone's own (device request, 7 set). It is Today's header rule (§5.1) carried
 * onto the home screen, for the reason that rule exists: a saved "Cavenago" and a fix
 * standing in Cavenago are two identical cards, and a card that is following the
 * reader around should say so. Same glyph as the app screen — see the drawable.
 *
 * The pin is sized on the name's own text size and the reader's font scale, like
 * [textInkBalance] and for the same reason: a glyph that stays put while the words
 * grow stops being part of the same line.
 */
@Composable
fun PlaceLine(
    name: String,
    fromGps: Boolean,
    palette: WidgetPalette,
    size: TextUnit,
    modifier: GlanceModifier = GlanceModifier
) {
    val context = LocalContext.current
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        if (fromGps) {
            Image(
                provider = ImageProvider(R.drawable.ic_place_pin),
                // The pin's own words, so the card reads "My position, Cavenago"
                // rather than showing a mark it never names.
                contentDescription = context.getString(R.string.places_gps_title),
                colorFilter = ColorFilter.tint(palette.secondary),
                modifier = GlanceModifier.size(placePinSize(context, size.value))
            )
            Spacer(modifier = GlanceModifier.width(PlacePinGap))
        }
        Text(text = name, style = secondaryStyle(palette, size), maxLines = 1)
    }
}

/** The app screen puts a 20dp pin before a 22sp title; the ratio travels, the numbers
 * do not — a widget's name is 15 or 16sp. */
private fun placePinSize(context: Context, fontSizeSp: Float): Dp =
    (fontSizeSp * PinToText * context.resources.configuration.fontScale).dp

private const val PinToText = 0.9f
private val PlacePinGap = 4.dp

/** The stale marker (VISION §5.9): with old data the widget says how old. */
fun staleText(context: Context, lastSync: Instant, now: Instant): String {
    val age = Duration.between(lastSync, now)
    return when {
        age.toDays() >= 1 -> context.resources.getQuantityString(
            R.plurals.freshness_days_ago, age.toDays().toInt(), age.toDays().toInt()
        )
        age.toHours() >= 1 -> context.resources.getQuantityString(
            R.plurals.freshness_hours_ago, age.toHours().toInt(), age.toHours().toInt()
        )
        else -> context.resources.getQuantityString(
            R.plurals.freshness_minutes_ago,
            age.toMinutes().coerceAtLeast(1).toInt(),
            age.toMinutes().coerceAtLeast(1).toInt()
        )
    }
}

/**
 * The hero glyph's size: it fills the height the launcher really granted, between a
 * floor and a ceiling (committente, 3 set — "as big as the Samsung one"). A fixed
 * number could only ever be right on one cell size, and the Meteocons art draws
 * inside about half its box (the crescent of `mcf_clear_night` is 33 units of 64),
 * so the box has to be generous before the glyph reads at arm's length. The floor
 * keeps a squeezed widget legible; the ceiling stops a tall grant from turning the
 * icon into a poster.
 *
 * Measured on the device's own screenshot (4th pass): the launcher grants the Now
 * widget about 101 dp of height, so 12 dp of card padding on each side left a 77 dp
 * box against the neighbouring widget's 87 dp — the reason the Now widget now keeps
 * only 6 dp above and below.
 */
/**
 * The band of empty leading a block of words carries above its capitals, so the row
 * beside it can balance the same band underneath.
 *
 * A text block is taller than the ink you can see: the system font leaves roughly a
 * quarter of an em above the capitals of "23°", while the last line's descenders (the
 * g of "Cavenago") reach the very bottom of its box. Centre such a block against an
 * icon and the ICON reads high — measured at 5 dp on the device's own screenshot (5th
 * device pass), which is exactly half that band, the half the block's own asymmetry
 * is worth. Padding the block by the band at the bottom makes it symmetric around its
 * own ink, and then plain vertical centring lands the two inks on one line.
 *
 * It follows the reader's font scale, because the band is made of text — read off a
 * Context rather than a composition local for the same reason [isNight] is: a widget
 * is recomposed by the Application on every configuration change, and there is no
 * `LocalConfiguration` on the launcher's side of the fence. What it does
 * not chase is the drawing: measured over the icon family, most Meteocons sit dead
 * centre in their box, a cloudy night sits 4% high and a thunderstorm 10% low — the
 * bolt hangs down on purpose. That is the illustrator's composition, not a defect,
 * and a per-icon table to "fix" it would be the app arguing with its own artwork.
 */
fun textInkBalance(context: Context, fontSizeSp: Float): Dp =
    (fontSizeSp * LeadingAboveCaps * context.resources.configuration.fontScale).dp

/** Ascender minus cap height, as a fraction of the font size. The device's own font
 * measures about 0.30 em; 0.24 is the value taken, because a last line that ends
 * without a descender gives part of the band back at the bottom. */
private const val LeadingAboveCaps = 0.24f

fun heroIconSize(
    available: Dp,
    min: Dp = HeroIconMin,
    max: Dp = HeroIconMax
): Dp = available.coerceIn(min, max)

private val HeroIconMin = 52.dp
private val HeroIconMax = 104.dp

/** The rain figure's ink: the §2.3 INK ramp — the one selected for figures rather
 * than for marks — resolved on the ground the card really has, zero included. The
 * app strip's own rule: 0% is the quiet end of the scale, not a second color, and the
 * fill ramp never carries text (its light end is 1.3:1 on paper). */
fun rainInk(pct: Int, palette: WidgetPalette): androidx.glance.unit.ColorProvider =
    FixedColorProvider(palette.colors.rainInkAt(pct))

/** The verdict pair, resolved at render time like every other widget color: same
 * fixed semantics as in the app — a verdict means the same thing whatever the
 * wallpaper is (DESIGN §2.3) — and ink and container resolve together, so no
 * host can ever pair one mode's chip with the other mode's word. */
fun verdictInk(
    kind: SkyVerdictKind,
    night: Boolean,
    dress: ChiaroPalette
): androidx.glance.unit.ColorProvider = FixedColorProvider(verdictColors(kind, night, dress).ink)

fun verdictContainer(
    kind: SkyVerdictKind,
    night: Boolean,
    dress: ChiaroPalette
): androidx.glance.unit.ColorProvider =
    FixedColorProvider(verdictColors(kind, night, dress).container)

private fun verdictColors(kind: SkyVerdictKind, night: Boolean, dress: ChiaroPalette) =
    dress.colors(night).let {
        when (kind) {
            SkyVerdictKind.PASS -> it.pass
            SkyVerdictKind.UNSTABLE -> it.unstable
            SkyVerdictKind.FAIL -> it.fail
            SkyVerdictKind.UNKNOWN -> it.unknown
        }
    }
