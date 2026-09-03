package com.callbackdev.chiaro.widget

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider as FixedColorProvider
import com.callbackdev.chiaro.MainActivity
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.domain.sky.SkyVerdictKind
import com.callbackdev.chiaro.ui.theme.ChiaroDarkColors
import com.callbackdev.chiaro.ui.theme.ChiaroDarkScheme
import com.callbackdev.chiaro.ui.theme.ChiaroLightColors
import com.callbackdev.chiaro.ui.theme.ChiaroLightScheme
import com.callbackdev.chiaro.ui.theme.SkyPalette
import com.callbackdev.chiaro.ui.today.SkySnapshot
import java.time.Duration
import java.time.Instant

/**
 * The widgets' side of the design system (Fase 8, redrawn on device review): the
 * default dress is the app's OWN hero — the computed sky gradient with the measured
 * scrim baked in and white ink over it, exactly the §3.6 contract the canvas keeps.
 * The alternatives are a plain card in the app's schemes (dynamic or Chiaro), fixed
 * light, fixed dark, or following the system — each widget chooses for itself.
 */
data class WidgetSchemes(val light: ColorScheme, val dark: ColorScheme)

fun widgetSchemes(context: Context, dynamicColor: Boolean): WidgetSchemes =
    if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        WidgetSchemes(dynamicLightColorScheme(context), dynamicDarkColorScheme(context))
    } else {
        WidgetSchemes(ChiaroLightScheme, ChiaroDarkScheme)
    }

/** The three inks a widget writes with, resolved once per background choice. */
data class WidgetPalette(
    val primary: androidx.glance.unit.ColorProvider,
    val secondary: androidx.glance.unit.ColorProvider,
    val stale: androidx.glance.unit.ColorProvider
)

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

/** Below this solidity the card stops being the ink's ground. */
private const val InkTrustFloorPct = 50

private fun palette(
    background: WidgetBackground,
    schemes: WidgetSchemes,
    night: Boolean,
    opacityPct: Int
): WidgetPalette {
    val lightInks = WidgetPalette(
        primary = FixedColorProvider(schemes.light.onSurface),
        secondary = FixedColorProvider(schemes.light.onSurfaceVariant),
        stale = FixedColorProvider(ChiaroLightColors.freshness.ink)
    )
    val darkInks = WidgetPalette(
        primary = FixedColorProvider(schemes.dark.onSurface),
        secondary = FixedColorProvider(schemes.dark.onSurfaceVariant),
        stale = FixedColorProvider(ChiaroDarkColors.freshness.ink)
    )
    // A card under half solidity no longer guarantees its own ground, so the ink
    // stops trusting it: whatever the dress, it follows the phone's theme — the
    // best proxy a widget has for the wallpaper showing through (device report,
    // 3 set: dark ink on a see-through card over a dark wallpaper).
    if (opacityPct < InkTrustFloorPct) return if (night) darkInks else lightInks
    return when (background) {
        // White over the scrimmed gradient: the §3.6 numbers, reused as-is.
        WidgetBackground.SKY -> WidgetPalette(
            primary = FixedColorProvider(Color.White),
            secondary = FixedColorProvider(Color.White.copy(alpha = 0.75f)),
            stale = FixedColorProvider(Color.White.copy(alpha = 0.85f))
        )
        WidgetBackground.LIGHT -> lightInks
        WidgetBackground.DARK -> darkInks
        WidgetBackground.SYSTEM -> if (night) darkInks else lightInks
    }
}

/** The card's inner padding: the roomy default, and the tighter dress for the
 * one-cell widgets, whose bigger icon needs the air (device review, 3 set). */
val WidgetCardPadding = 14.dp
val WidgetCardPaddingTight = 12.dp

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
    val night = isNight(LocalContext.current)
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
        Box(modifier = GlanceModifier.fillMaxSize().padding(contentPadding)) {
            content(palette(effectiveBackground, schemes, night, look.opacityPct))
        }
    }
}

/**
 * The sky, rendered: the canvas' three stops top-to-bottom, the §3.6 scrim over the
 * whole of it (uniform here — every pixel of a widget can carry text), both scaled
 * by the widget's opacity so transparency thins the sky, never the words.
 */
fun skyGradientBitmap(sky: SkySnapshot, opacityPct: Int): Bitmap {
    val gradient = SkyPalette.gradient(
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

/** The honest empty state: no place yet, and the tap that fixes it. */
@Composable
fun NoPlaceContent(palette: WidgetPalette) {
    val context = LocalContext.current
    Column {
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
    Column {
        Text(
            text = context.getString(R.string.widget_no_data),
            style = TextStyle(color = palette.secondary, fontSize = 12.sp)
        )
    }
}

@Composable
fun secondaryStyle(palette: WidgetPalette, size: TextUnit): TextStyle =
    TextStyle(color = palette.secondary, fontSize = size)

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

/** The verdict pair, resolved at render time like every other widget color: same
 * fixed semantics as in the app — a verdict means the same thing whatever the
 * wallpaper is (DESIGN §2.3) — and ink and container resolve together, so no
 * host can ever pair one mode's chip with the other mode's word. */
fun verdictInk(kind: SkyVerdictKind, night: Boolean): androidx.glance.unit.ColorProvider =
    FixedColorProvider(
        (if (night) verdictColorsDark(kind) else verdictColorsLight(kind)).ink
    )

fun verdictContainer(kind: SkyVerdictKind, night: Boolean): androidx.glance.unit.ColorProvider =
    FixedColorProvider(
        (if (night) verdictColorsDark(kind) else verdictColorsLight(kind)).container
    )

private fun verdictColorsLight(kind: SkyVerdictKind) = when (kind) {
    SkyVerdictKind.PASS -> ChiaroLightColors.pass
    SkyVerdictKind.UNSTABLE -> ChiaroLightColors.unstable
    SkyVerdictKind.FAIL -> ChiaroLightColors.fail
    SkyVerdictKind.UNKNOWN -> ChiaroLightColors.unknown
}

private fun verdictColorsDark(kind: SkyVerdictKind) = when (kind) {
    SkyVerdictKind.PASS -> ChiaroDarkColors.pass
    SkyVerdictKind.UNSTABLE -> ChiaroDarkColors.unstable
    SkyVerdictKind.FAIL -> ChiaroDarkColors.fail
    SkyVerdictKind.UNKNOWN -> ChiaroDarkColors.unknown
}
