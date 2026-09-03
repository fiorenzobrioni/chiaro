package com.callbackdev.chiaro.widget

import android.content.Context
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.material3.ColorProviders
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider as SingleColorProvider
import com.callbackdev.chiaro.MainActivity
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.data.AppSettings
import com.callbackdev.chiaro.ui.theme.ChiaroDarkColors
import com.callbackdev.chiaro.ui.theme.ChiaroDarkScheme
import com.callbackdev.chiaro.ui.theme.ChiaroLightColors
import com.callbackdev.chiaro.ui.theme.ChiaroLightScheme
import com.callbackdev.chiaro.domain.sky.SkyVerdictKind
import java.time.Duration
import java.time.Instant
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * The widgets' side of the design system (Fase 8): the same two schemes the app
 * runs on — the wallpaper-derived one, or Chiaro's own when dynamic color is off —
 * carried as day/night pairs so the launcher can switch them without asking us.
 * The widgets follow the SYSTEM's light/dark, not the app's forced theme: they live
 * on the launcher, and a dark widget on a light home screen would be the launcher's
 * lie, not ours.
 */
data class WidgetSchemes(val light: ColorScheme, val dark: ColorScheme)

fun widgetSchemes(context: Context, dynamicColor: Boolean): WidgetSchemes =
    if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        WidgetSchemes(dynamicLightColorScheme(context), dynamicDarkColorScheme(context))
    } else {
        WidgetSchemes(ChiaroLightScheme, ChiaroDarkScheme)
    }

/**
 * The card every widget lives in: the whole surface opens the app (VISION §5.9 — a
 * widget with no place says so "and opens the app"; the healthy ones open it too),
 * rounded like the launcher expects, with the reader's background opacity applied to
 * the fill only — the content keeps full ink.
 */
@Composable
fun WidgetCard(
    schemes: WidgetSchemes,
    settings: AppSettings,
    content: @Composable () -> Unit
) {
    val alpha = settings.widgetOpacityPct / 100f
    GlanceTheme(colors = ColorProviders(light = schemes.light, dark = schemes.dark)) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .appWidgetBackground()
                .background(
                    ColorProvider(
                        day = schemes.light.surface.copy(alpha = alpha),
                        night = schemes.dark.surface.copy(alpha = alpha)
                    )
                )
                .cornerRadius(24.dp)
                .clickable(actionStartActivity<MainActivity>())
                .padding(14.dp)
        ) {
            content()
        }
    }
}

/** The honest empty state: no place yet, and the tap that fixes it. */
@Composable
fun NoPlaceContent() {
    val context = LocalContext.current
    Column {
        Text(
            text = context.getString(R.string.empty_no_place_title),
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        )
        Text(
            text = context.getString(R.string.widget_no_place_hint),
            style = secondaryStyle(12.sp)
        )
    }
}

@Composable
fun secondaryStyle(size: TextUnit): TextStyle = TextStyle(
    color = GlanceTheme.colors.onSurfaceVariant,
    fontSize = size
)

/**
 * The stale marker (VISION §5.9: with stale data a widget says how old it is), in
 * the freshness amber the app already uses — "this data is old" keeps one color
 * everywhere it is said.
 */
@Composable
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

val FreshnessInk: SingleColorProvider = ColorProvider(
    day = ChiaroLightColors.freshness.ink,
    night = ChiaroDarkColors.freshness.ink
)

/** The verdict pair as day/night providers: same fixed semantics as in the app —
 * a verdict means the same thing whatever the wallpaper is (DESIGN §2.3). */
fun verdictInk(kind: SkyVerdictKind): SingleColorProvider = ColorProvider(
    day = verdictColorsLight(kind).ink,
    night = verdictColorsDark(kind).ink
)

fun verdictContainer(kind: SkyVerdictKind): SingleColorProvider = ColorProvider(
    day = verdictColorsLight(kind).container,
    night = verdictColorsDark(kind).container
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
