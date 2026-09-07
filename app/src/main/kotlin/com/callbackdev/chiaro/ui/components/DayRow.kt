package com.callbackdev.chiaro.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.ui.icons.WeatherIconSize
import com.callbackdev.chiaro.ui.theme.ChiaroTheme
import com.callbackdev.chiaro.ui.theme.forText
import com.callbackdev.chiaro.ui.theme.reflowForText
import com.callbackdev.chiaro.ui.theme.tabular

/**
 * DESIGN.md §8.5. One day of the week: weekday, icon, rain probability, the range bar
 * on the scale the caller computed **across the whole week**, the printed low and high
 * (a colored bar is not a number), and the day's ribbon of light underneath.
 *
 * Everything arrives formatted; [scaleLowC]/[scaleHighC] are the week's own extremes,
 * shared by all seven rows so the week has a shape.
 *
 * **At 200% type the row is two rows** (§10, Fase 9). Its four text columns are measured
 * in dp and its content in sp, so they grew apart: at 1.5 the four of them plus the icon
 * and the gaps leave the range bar 40dp, and past that the day name wraps inside its own
 * 44dp. Splitting is the honest answer and not a compromise — the week reads as
 * "which day, what kind of day" and then "how warm", which is the order the row already
 * had, only turned through ninety degrees. The ribbon keeps its 52dp indent in one line
 * and loses it in two, because there is no longer a day column to line up under.
 */
@Composable
fun DayRow(
    dayLabel: String,
    icon: ImageVector,
    /** The quantity, which the ink ramp needs. */
    rainPct: Int,
    /** The same quantity printed, which the locale owns (§11). */
    rainLabel: String,
    lowC: Double,
    highC: Double,
    lowLabel: String,
    highLabel: String,
    scaleLowC: Double,
    scaleHighC: Double,
    phases: List<LightPhase>,
    description: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val reflow = reflowForText()
    Column(
        modifier = modifier
            .fillMaxWidth()
            // §10 asks for ≥ 48dp of target and the row draws 42 (a 34dp icon, 4dp of
            // gap, the 4dp ribbon). It clears the floor anyway and not by luck:
            // Compose expands a pointer node's bounds to the platform's minimum touch
            // target, so `clickable` is already 48. What that expansion does NOT do is
            // reserve space, so it fails when two small targets sit closer than their
            // expanded bounds — the week's rows are 12dp apart and each holds one
            // target, so there is nothing here to disambiguate. Measured in Fase 9;
            // `minimumInteractiveComponentSize()` would cost the week 6dp a row and buy
            // the reader nothing.
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .semantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (reflow) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) { DayAndSky(dayLabel, icon, rainPct, rainLabel) }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) { Range(lowLabel, highLabel, lowC, highC, scaleLowC, scaleHighC) }
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DayAndSky(dayLabel, icon, rainPct, rainLabel)
                Range(lowLabel, highLabel, lowC, highC, scaleLowC, scaleHighC)
            }
        }
        DaylightRibbon(
            phases = phases,
            nowFraction = null,
            description = "", // idem: one announcement per row
            height = 4.dp,
            modifier = Modifier.padding(start = if (reflow) 0.dp else 52.dp)
        )
    }
}

/** Which day, and what kind of day. */
@Composable
private fun RowScope.DayAndSky(
    dayLabel: String,
    icon: ImageVector,
    rainPct: Int,
    rainLabel: String
) {
    Text(
        text = dayLabel,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.width(44.dp.forText())
    )
    Icon(
        imageVector = icon,
        contentDescription = null, // the row speaks once, via its semantics
        tint = Color.Unspecified,
        // Between the strip and the timeline on the family's ladder, sized to
        // the row it sits in. The ribbon below still starts at 52dp (the day
        // label's 44 plus the 8 beside it), which the icon's size never touched.
        modifier = Modifier.size(WeatherIconSize.Week)
    )
    Text(
        text = rainLabel,
        style = MaterialTheme.typography.labelSmall.tabular(),
        // The ink ramp, zero included: 0% is the quiet end of the same scale and
        // not the secondary text role, which printed the emptiest day of the week
        // in the heaviest ink on it (DESIGN.md §2.3).
        color = ChiaroTheme.colors.rainInkAt(rainPct),
        textAlign = TextAlign.End,
        modifier = Modifier.width(36.dp.forText())
    )
}

/** How warm, printed at both ends and drawn between them. */
@Composable
private fun RowScope.Range(
    lowLabel: String,
    highLabel: String,
    lowC: Double,
    highC: Double,
    scaleLowC: Double,
    scaleHighC: Double
) {
    Text(
        text = lowLabel,
        style = MaterialTheme.typography.labelLarge.tabular(),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.End,
        modifier = Modifier.width(34.dp.forText())
    )
    TemperatureRangeBar(
        lowC = lowC,
        highC = highC,
        scaleLowC = scaleLowC,
        scaleHighC = scaleHighC,
        description = "", // the row's own description covers it
        modifier = Modifier.weight(1f)
    )
    Text(
        text = highLabel,
        style = MaterialTheme.typography.labelLarge.tabular(),
        modifier = Modifier.width(34.dp.forText())
    )
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun DayRowPreview() {
    ChiaroTheme(dynamicColor = false) {
        val phases = listOf(
            LightPhase(0f, 0.22f, -30.0),
            LightPhase(0.22f, 0.28f, -9.0),
            LightPhase(0.28f, 0.33f, 2.0),
            LightPhase(0.33f, 0.74f, 45.0),
            LightPhase(0.74f, 0.79f, 2.0),
            LightPhase(0.79f, 0.85f, -9.0),
            LightPhase(0.85f, 1f, -30.0)
        )
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(
                Triple("Oggi", R.drawable.mc_partly_cloudy_day, Triple(12.0, 22.0, 10)),
                Triple("Gio", R.drawable.mc_rain, Triple(14.0, 19.0, 80)),
                Triple("Ven", R.drawable.mc_clear_day, Triple(9.0, 24.0, 0))
            ).forEach { (day, iconRes, data) ->
                val (low, high, rain) = data
                DayRow(
                    dayLabel = day,
                    icon = ImageVector.vectorResource(iconRes),
                    rainPct = rain,
                    rainLabel = "$rain%",
                    lowC = low,
                    highC = high,
                    lowLabel = "${low.toInt()}°",
                    highLabel = "${high.toInt()}°",
                    scaleLowC = 9.0,
                    scaleHighC = 24.0,
                    phases = phases,
                    description = "$day, da ${low.toInt()} a ${high.toInt()} gradi, pioggia $rain%"
                )
            }
        }
    }
}

/**
 * §10 names the week row as one of the two places 200% type is checked, so the check is
 * a preview and not a promise: this is the row at the top of the system's own slider.
 */
@Preview(showBackground = true, widthDp = 360, fontScale = 2f)
@Composable
private fun DayRowLargeTextPreview() = DayRowPreview()
