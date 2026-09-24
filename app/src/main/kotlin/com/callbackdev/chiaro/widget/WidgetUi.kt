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
import android.graphics.Typeface
import android.os.Build
import android.util.TypedValue
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
import androidx.glance.action.clickable
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
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
import com.callbackdev.chiaro.ui.shell.ShellDestination
import com.callbackdev.chiaro.ui.shell.ShellTab
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.data.AppPalette
import com.callbackdev.chiaro.domain.settings.UnitSettings
import com.callbackdev.chiaro.domain.sky.SkyVerdictKind
import com.callbackdev.chiaro.domain.warnings.WarningLevel
import com.callbackdev.chiaro.ui.format.Formats
import com.callbackdev.chiaro.ui.icons.ChiaroIcons
import com.callbackdev.chiaro.ui.theme.ChiaroColors
import com.callbackdev.chiaro.ui.theme.ChiaroPalette
import com.callbackdev.chiaro.ui.theme.SkyPalette
import com.callbackdev.chiaro.ui.theme.WidgetCardColor
import com.callbackdev.chiaro.ui.theme.paletteFor
import com.callbackdev.chiaro.ui.theme.widgetCardContainer
import com.callbackdev.chiaro.ui.today.SkySnapshot
import com.callbackdev.chiaro.ui.warnings.WarningText
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
 * The Now widget's two kinds of edge, which are not each other (committente, 7 set: the
 * icon a little too close to the leading edge, the words decidedly close to the trailing
 * one). An edge carries either a glyph or words, and the two need different numbers.
 *
 * **A glyph edge takes 4 dp.** A Meteocons glyph brings a margin of its own — measured
 * by rasterising all 160 condition drawables, 6.5/64 of the box at the tightest
 * (`partly_cloudy_day`) and 9/64 at the median, which is 9 to 12.5 dp at the ~89 dp box
 * a one-cell grant leaves — so the edge only needs the difference. 4 dp since 8 set 2026,
 * halved from 8 with the home screen beside a second weather widget for scale: the
 * previous pass read the margin off the family's median, and the median is not what a
 * night home screen shows — `clear_night`, the crescent, keeps 22.5% of its box empty on
 * that side, so at 8 dp its ink stopped ~22 dp in while the neighbour's moon stopped at
 * 16. At 4 the crescent lands within a dp or two of it and the median glyph still clears
 * the edge by ~12. Since the three-form layout (8 set, evening) the same 4 dp is also
 * the tall card's trailing inset, because there it is the glyph that meets that edge.
 *
 * **A words edge takes [WidgetCardPadding], 14 dp** — the inset every widget's words
 * write at, since the same evening. It was 12 (the other one-cell widget's) while the
 * only thing on that edge was an optional high/low pair; a right-aligned sentence block
 * is a bigger thing to sit against a corner, and the reference widget insets its own
 * description by ~25 dp on a card with a much larger radius. Fourteen on a 24 dp corner
 * keeps the block clear of the curve, and the two dp came out of a column that no longer
 * has the pair to pay for.
 */
val WidgetCardPaddingLeading = 4.dp
val WidgetCardPaddingTrailing = WidgetCardPadding

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
    /**
     * The two vertical edges on their own for the same reason: the Now widget's tall
     * form has a glyph against its top edge and words against its bottom one.
     */
    contentPaddingTop: Dp = contentPadding,
    contentPaddingBottom: Dp = contentPadding,
    /**
     * The screen this card opens (21 set 2026). Null is "just open the app", which
     * lands wherever the reader left it; a card whose material belongs to one tab
     * names that tab instead, because arriving on Today from «Momenti del cielo» asks
     * the reader to go and find again what they had just read on the home screen.
     */
    destination: ShellTab? = null,
    content: @Composable (WidgetPalette) -> Unit
) {
    val look = model.look
    val effectiveBackground = effectiveWidgetBackground(look, hasSky = skyBitmap != null)
    val alpha = look.opacityPct / 100f
    val context = LocalContext.current
    val night = isNight(context)
    val resolved = resolveWidgetPalette(context, look, schemes, hasSky = skyBitmap != null)
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .cornerRadius(24.dp)
            // One door for all five cards, destination or not (21 set 2026): the intent
            // is what tells the platform which TASK this is, and a card that opened the
            // app with an intent of its own was opening a second one. The Intent
            // overload is the appwidget artifact's, not the base one's — qualified
            // rather than imported, because the two names would sit side by side and
            // the base one takes a ComponentName.
            .clickable(
                androidx.glance.appwidget.action.actionStartActivity(
                    ShellDestination.intent(context, MainActivity::class.java, destination)
                )
            )
    ) {
        if (effectiveBackground == WidgetBackground.SKY && skyBitmap != null) {
            Image(
                provider = ImageProvider(skyBitmap),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = GlanceModifier.fillMaxSize()
            )
        } else {
            val fill = FixedColorProvider(
                widgetCardFill(effectiveBackground, schemes, night, alpha, look.cardColor)
            )
            Box(modifier = GlanceModifier.fillMaxSize().background(fill)) {}
        }
        Box(
            modifier = GlanceModifier.fillMaxSize().padding(
                start = contentPaddingStart,
                top = contentPaddingTop,
                end = contentPaddingEnd,
                bottom = contentPaddingBottom
            )
        ) {
            content(resolved)
        }
    }
}

/** The ground a card really paints: SKY with no report yet has no sky to show, and
 * falls back to the system card rather than inventing a weather-less gradient. */
fun effectiveWidgetBackground(look: WidgetLook, hasSky: Boolean): WidgetBackground =
    if (look.background == WidgetBackground.SKY && !hasSky) WidgetBackground.SYSTEM else look.background

/**
 * The inks a card writes with, resolved exactly as [WidgetCard] resolves them — shared
 * with the arc widget's configuration preview (9 set 2026), so the preview cannot pick
 * an ink the launcher would not.
 *
 * The wallpaper is asked only when the card is see-through enough to matter AND the
 * reader has not already named an ink by picking a light or a dark card: it is a binder
 * round-trip, and [widgetInk] would throw the answer away in every other case anyway.
 */
fun resolveWidgetPalette(
    context: Context,
    look: WidgetLook,
    schemes: WidgetSchemes,
    hasSky: Boolean
): WidgetPalette {
    val effectiveBackground = effectiveWidgetBackground(look, hasSky)
    val night = isNight(context)
    val delegatesInk = effectiveBackground == WidgetBackground.SKY ||
        effectiveBackground == WidgetBackground.SYSTEM
    val wallpaperCarriesDarkInk = look.opacityPct < InkTrustFloorPct && delegatesInk &&
        wallpaperWantsDarkInk(context)
    val ink = widgetInk(effectiveBackground, look.opacityPct, night, wallpaperCarriesDarkInk)
    return palette(ink, schemes)
}

/**
 * A plain card's fill for a background that is not the sky, at the reader's opacity. A
 * coloured card ([WidgetBackground.COLOR], 19 set 2026) takes its ground from the table in
 * `ui/theme` rather than from either scheme: it is a colour the reader chose for this card,
 * not a surface the generator produced, and it does not follow the phone's mode for the
 * same reason the sky does not (DESIGN §2.6).
 */
fun widgetCardFill(
    background: WidgetBackground,
    schemes: WidgetSchemes,
    night: Boolean,
    alpha: Float,
    cardColor: WidgetCardColor
): Color = when (background) {
    WidgetBackground.LIGHT -> schemes.light.surface.copy(alpha = alpha)
    WidgetBackground.DARK -> schemes.dark.surface.copy(alpha = alpha)
    WidgetBackground.COLOR -> widgetCardContainer(cardColor).copy(alpha = alpha)
    else -> (if (night) schemes.dark.surface else schemes.light.surface).copy(alpha = alpha)
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
 * Tabular figures are not available to Glance, so the pair is several Texts rather than
 * one: it is also the only way to give the two numbers two dresses. Nothing is drawn at
 * all when the report has no day left to describe (§1.1).
 *
 * **[marks]** (committente, 19 set 2026: «con le frecce su e giù ad indicare massima e
 * minima») puts the up and down marks before the two figures instead of a slash between
 * them. One composable and two dresses rather than two composables, because it is one
 * statement at two budgets: the marks dress is ~87 dp at 16 sp against the slash's ~56
 * («27°» and «15°»; ~100 at the widest pair the scale prints), which the text widget's
 * 174 dp trailing column carries easily and the Today widget's 113 dp one does not — so
 * Today keeps the slash, and the card with the room says it outright. The two figures rose
 * by 12 dp on 20 set when each mark stopped paying its own air out of its own box, and the
 * column that carries them has 30 dp to spare at the widest.
 *
 * **Which dress it wears decides how the two figures are sorted**, and that is the whole
 * difference between them (committente, 20 set 2026, on the device: the low read as the
 * smaller number of the two, and it was never meant to be a smaller number).
 *
 * - **With the marks**, both halves are set alike — same size, same weight, same ink, and
 *   the two marks tinted with it. ↑ and ↓ already say which is which, so the dimming was
 *   saying it a second time and charging a figure for it: §2.3's rule about verdicts read
 *   at this scale — the mark carries the meaning, the ink only ever seconds it, and where
 *   the mark is there the ink is free to stop shouting.
 * - **With the slash** there is no mark to carry it, so the ink stays the thing that sorts
 *   the pair: the high first and strong, the low after it and dimmed. That is the app's own
 *   emphasis, not a borrowed convention — the week's rows print the low in
 *   `onSurfaceVariant` and the high in the plain one for exactly this reason.
 */
@Composable
fun DayRange(
    highC: Double,
    lowC: Double,
    units: UnitSettings,
    palette: WidgetPalette,
    /** The pair's size. The default is the place name's, which is the rank the pair sits
     * at on every card that draws a place beside it. */
    size: TextUnit = DayRangeSp,
    marks: Boolean = false
) {
    val locale = glanceLocale()
    val context = LocalContext.current
    // One dress for both figures in the marks form, and it is the high's: see the header
    // for why the low stops being dimmed the moment a ↓ stands in front of it.
    val strong = TextStyle(
        color = palette.primary,
        fontSize = size,
        fontWeight = FontWeight.Medium
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (marks) RangeMark(high = true, ink = palette.primary, size = size, context = context)
        Text(
            text = Formats.temperature(highC, units.temperature, locale),
            style = strong,
            maxLines = 1
        )
        if (marks) {
            // The air between the two halves is the mark's own leading padding, so the
            // row stays four children rather than five (Glance drops the eleventh child
            // of a container without a word, and every widget here counts them).
            RangeMark(
                high = false, ink = palette.primary, size = size, context = context,
                leading = RangeMarkGap
            )
        } else {
            // Punctuation, not prose: a slash between two temperatures reads the same in
            // every language this app speaks, so it stays in the code.
            Text(
                text = " / ",
                style = secondaryStyle(palette, size),
                maxLines = 1
            )
        }
        Text(
            text = Formats.temperature(lowC, units.temperature, locale),
            style = if (marks) strong else secondaryStyle(palette, size),
            maxLines = 1
        )
    }
}

/**
 * One of the two marks, sized on the figure's own text size and the reader's font scale —
 * like the position pin, and for the reason that one gives: a glyph that stays put while
 * the words grow stops being part of the same line. It carries the words that name it, so
 * a screen reader says «massima 25°» rather than reading a number with no subject.
 *
 * **The air is on a wrapper and never on the mark** (20 set 2026), and this is the whole of
 * the «la freccia della minima è più piccola» the committente reported twice. Glance's
 * `padding` is `RemoteViews.setViewPadding` on the SAME view the size lands on, and an
 * `Image` scales its drawing to Fit what is left: an `ImageView` asked for a 16 dp box with
 * 8 dp of leading air and 2 of trailing drew the arrow in **6 dp**, beside a high mark that
 * had only the 2 to pay and drew at 14. Less than half the ink, at the same nominal size,
 * from a modifier that reads like it adds space. It was never the ink, which is why
 * repainting the low in the strong colour did not fix it.
 *
 * It is the same trap [WarningChipRow] records for the chip — «a chip's own padding sits
 * INSIDE its background, and a tinted gap is not a gap» — and the reason [PlaceLine] spaces
 * its pin with a `Spacer` instead. A `Box` and not a `Spacer` here only because the row
 * counts its children: this keeps the pair at four.
 */
@Composable
private fun RangeMark(
    high: Boolean,
    ink: androidx.glance.unit.ColorProvider,
    size: TextUnit,
    context: Context,
    leading: Dp = 0.dp
) {
    val box = (size.value * context.resources.configuration.fontScale).dp
    Box(modifier = GlanceModifier.padding(start = leading, end = RangeMarkTextGap)) {
        Image(
            provider = ImageProvider(ChiaroIcons.rangeMarkRes(high)),
            contentDescription = context.getString(
                if (high) R.string.widget_range_high else R.string.widget_range_low
            ),
            colorFilter = ColorFilter.tint(ink),
            modifier = GlanceModifier.size(box)
        )
    }
}

/**
 * What a [DayRange] occupies on its line, so a layout that has to leave room for one can
 * ask rather than guess ([measureWidgetText]'s own argument). It mirrors the composable
 * above it piece by piece — change one and this has to change with it.
 */
fun dayRangeWidth(
    context: Context,
    highC: Double,
    lowC: Double,
    units: UnitSettings,
    size: TextUnit = DayRangeSp,
    marks: Boolean = false
): Dp {
    val locale = Locale.getDefault()
    val high = Formats.temperature(highC, units.temperature, locale)
    val low = Formats.temperature(lowC, units.temperature, locale)
    val figures = measureWidgetText(context, high, size.value, medium = true) +
        measureWidgetText(context, low, size.value, medium = marks)
    if (!marks) return figures + measureWidgetText(context, " / ", size.value, medium = false)
    val box = (size.value * context.resources.configuration.fontScale).dp
    return figures + (box + RangeMarkTextGap) * 2 + RangeMarkGap
}

/** The air between a mark and its figure, and between the high's figure and the low's
 * mark: the same two numbers the place pin uses, one step apart. */
private val RangeMarkTextGap = 2.dp
private val RangeMarkGap = 8.dp

/** The pair sits at the place name's size: it is the same order of fact. */
private val DayRangeSp = 16.sp

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

/**
 * The width one line of a widget's text really takes, **measured and not estimated**
 * (20 set 2026). Glance cannot measure text and a layout that has to share a row between
 * two blocks of words has to know how wide one of them is, so the measuring happens where
 * it can: a `Paint` in this process, at the size and weight the `Text` will be given.
 *
 * The face is [Typeface.DEFAULT] — the system font — because that is what a widget is
 * drawn in: a home-screen card is inflated by the launcher from `RemoteViews` and never
 * sees the app's own bundled face, which is the whole reason Settings' «the phone's font»
 * is the closest the app gets to its own widgets. So this is the same font, the same size
 * and the same weight the reader will see, not a guess about them.
 *
 * What it cannot promise: a launcher on a phone whose system interface runs a different
 * face from the one apps get measures a few percent off ours. That is why every caller
 * keeps [RowFitSlack] and why nothing here is a hard bound — a name that comes out wider
 * than we measured ellipsises exactly as it did before, which is the state we started from.
 *
 * The size is resolved through [TypedValue] rather than `scaledDensity` so that a reader's
 * non-linear font scale is the one the platform will really apply.
 */
fun measureWidgetText(context: Context, text: String, sizeSp: Float, medium: Boolean): Dp {
    val metrics = context.resources.displayMetrics
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = if (medium) WidgetMediumTypeface else Typeface.DEFAULT
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sizeSp, metrics)
    }
    return (paint.measureText(text) / metrics.density).dp
}

/** `sans-serif-medium`, the family Glance's [FontWeight.Medium] resolves to on the
 * launcher's side: measuring Medium text with the Regular face reads ~2% narrow.
 *
 * `by lazy` and not a plain initializer: this file's top-level properties are read by the
 * pure layout tests, and a `Typeface` built while the class loads would drag the whole of
 * `WidgetUi` onto a device. Nothing constructs it until something really measures. */
private val WidgetMediumTypeface: Typeface by lazy {
    Typeface.create("sans-serif-medium", Typeface.NORMAL)
}

/**
 * The whole place line, pin included: what [PlaceLine] occupies on one line. The pin is
 * sized on the name's own text size, exactly as [placePinSize] sizes it, so a reader's
 * font scale moves the mark and the measurement together.
 */
fun placeLineWidth(context: Context, name: String, fromGps: Boolean, sizeSp: Float): Dp {
    val text = measureWidgetText(context, name, sizeSp, medium = false)
    return if (fromGps) text + placePinSize(context, sizeSp) + PlacePinGap else text
}

/**
 * The system font's line box under the baseline, in ems, **measured** for the reason
 * [measureWidgetText] gives: the card is drawn in the phone's face, not the app's. A
 * `TextView` with its font padding on (a widget's always is) ends its last line at the
 * face's `bottom`, so that is the number. A face that answers something implausible gets
 * Roboto's own ([TextDescentEm]).
 */
fun widgetTextDescentEm(): Float {
    val paint = Paint().apply {
        typeface = Typeface.DEFAULT
        textSize = 100f
    }
    return (paint.fontMetrics.bottom / 100f).takeIf { it in 0.15f..0.45f } ?: TextDescentEm
}

/** The air a measured width is given before it is used as a width: the launcher's font
 * is not this process's font, and a block that asks for exactly what it measured wraps
 * or ellipsises on the first phone that rounds the other way. */
val RowFitSlack = 4.dp

/** The app screen puts a 20dp pin before a 22sp title; the ratio travels, the numbers
 * do not — a widget's name is 16sp. */
private fun placePinSize(context: Context, fontSizeSp: Float): Dp =
    (fontSizeSp * PinToText * context.resources.configuration.fontScale).dp

/**
 * The pin's box is the text's size. **1.0 since 8 set 2026** (committente, on the
 * screenshot beside the launcher's own widget: «ours is smaller, and it does not look
 * good»), from 0.9. The drawing fills 20 of its 24 units top to bottom, so at 0.9 a
 * 16sp name got 12 dp of pin — the cap height and no more — where the reference's pin
 * stands ~13.5 dp, a cap height and a descender. At 1.0 the ink is 13.3 dp: the same
 * mark at the same size, on every widget that prints a place.
 */
private const val PinToText = 1.0f
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
    textInkBalance(fontSizeSp, context.resources.configuration.fontScale)

/** The same band as pure arithmetic, so a layout budget can be pinned by a test. */
fun textInkBalance(fontSizeSp: Float, fontScale: Float): Dp =
    (fontSizeSp * LeadingAboveCaps * fontScale).dp

/** Ascender minus cap height, as a fraction of the font size. The device's own font
 * measures about 0.30 em; 0.24 is the value taken, because a last line that ends
 * without a descender gives part of the band back at the bottom. */
private const val LeadingAboveCaps = 0.24f

/**
 * The height one line of text really occupies in a Glance `Text`: the TextView keeps
 * `includeFontPadding`, so a line is the font's top-to-bottom box rather than its
 * ascent plus descent — about 1.32 em for the system font (Roboto: 2146 + 555 units
 * of 2048) — scaled by the reader's font size. Used where a layout has to reserve for
 * words it cannot measure (the Now widget's tall form) and where it counts how many
 * lines a height holds. An estimate, and named as one: the budgets that rest on it
 * keep a band of slack ([textInkBalance]) that absorbs a font whose box is a few
 * hundredths taller.
 */
fun textLineHeight(fontSizeSp: Float, fontScale: Float): Dp =
    (fontSizeSp * LineBoxEm * fontScale).dp

/**
 * [textLineHeight] the other way round: the biggest text size whose line box still fits
 * [room]. The four cards with a drawing on them size that drawing to the grant
 * ([heroIconSize]); the text widget has no drawing and sizes its NUMBER to the grant
 * instead, which is the same question asked about a figure — and it needs the same
 * arithmetic read backwards. Never negative: a budget that has run out asks for 0 sp and
 * gets it, and the caller's own floor decides what to do about that.
 */
fun textSizeForLine(room: Dp, fontScale: Float): Float =
    (room.value / (LineBoxEm * fontScale.coerceAtLeast(0.1f))).coerceAtLeast(0f)

private const val LineBoxEm = 1.32f

fun heroIconSize(
    available: Dp,
    min: Dp = HeroIconMin,
    max: Dp = HeroIconMax
): Dp = available.coerceIn(min, max)

private val HeroIconMin = 52.dp
internal val HeroIconMax = 104.dp

/** The rain figure's ink: the §2.3 INK ramp — the one selected for figures rather
 * than for marks — resolved on the ground the card really has, zero included. The
 * app strip's own rule: 0% is the quiet end of the scale, not a second color, and the
 * fill ramp never carries text (its light end is 1.3:1 on paper). */
fun rainInk(pct: Int, palette: WidgetPalette): androidx.glance.unit.ColorProvider =
    FixedColorProvider(palette.colors.rainInkAt(pct))

/**
 * The official warning's chip (Fase 11, DESIGN §8.13): `ic_warning` in the level's ink
 * and the level's word, on the level's container — the Sky card's `VerdictChip` grammar
 * at the size a home-screen card can afford. One TalkBack string for the pair, because a
 * screen reader should say «Allerta gialla», never «triangle, Allerta gialla».
 *
 * The colours come from [WidgetPalette.colors], which is the dress read against the
 * ground THIS CARD really has — not against the phone's theme. A light card under a dark
 * system theme is the trap: theme-resolved colours put a dark-mode ink on a light-mode
 * container, and the pair stops being the measured pair.
 */
@Composable
fun WarningChip(level: WarningLevel, palette: WidgetPalette) {
    val context = LocalContext.current
    val colors = palette.colors
    val pair = when (level) {
        WarningLevel.RED -> colors.warningRed
        WarningLevel.ORANGE -> colors.warningOrange
        else -> colors.warningYellow
    }
    val word = context.getString(WarningText.phraseRes(level))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = GlanceModifier
            .background(FixedColorProvider(pair.container))
            .cornerRadius(WarningChipCorner)
            .padding(horizontal = WarningChipPadH, vertical = WarningChipPadV)
            .semantics { contentDescription = word }
    ) {
        Image(
            provider = ImageProvider(ChiaroIcons.warningMarkRes()),
            contentDescription = null, // the row's own semantics say the word
            colorFilter = ColorFilter.tint(FixedColorProvider(pair.ink)),
            modifier = GlanceModifier.size(WarningChipGlyph)
        )
        Spacer(modifier = GlanceModifier.width(WarningChipTextGap))
        Text(
            text = word,
            style = TextStyle(
                color = FixedColorProvider(pair.ink),
                fontSize = WarningChipSp.sp,
                fontWeight = FontWeight.Medium
            ),
            maxLines = 1
        )
    }
}

/**
 * The chip with the air above it, as ONE child of whatever column it joins: the gap is
 * the wrapper's padding rather than a `Spacer`, for the reason the arc's agenda gives —
 * Glance draws at most ten children per container and drops the rest without a word, so
 * a line that costs two children costs twice what it looks like. The padding is on the
 * box and not on the chip because a chip's own padding sits INSIDE its background, and
 * a tinted gap is not a gap.
 */
@Composable
fun WarningChipRow(level: WarningLevel, palette: WidgetPalette, topGap: Dp = 0.dp) {
    Box(modifier = GlanceModifier.padding(top = topGap)) {
        WarningChip(level, palette)
    }
}

/** The air between the mark and its word, the Sky chip's own. */
private val WarningChipTextGap = 4.dp

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

/**
 * The locale a card formats in, read from the context Glance composes with rather than
 * from `Locale.getDefault()`: the same answer, but Compose 1.10's lint refuses the default
 * inside any composable (`NonObservableLocale`, 22 set 2026), and Glance has no
 * `LocalConfiguration` to take it from. A card is recomposed on every update, and a change
 * of language restarts the process, so there is nothing to observe beyond this read.
 */
@Composable
fun glanceLocale(): Locale = LocalContext.current.resources.configuration.locales[0]
