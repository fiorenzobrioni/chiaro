package com.callbackdev.chiaro.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.callbackdev.chiaro.ui.theme.ChiaroTheme
import com.callbackdev.chiaro.ui.theme.tabular

/**
 * DESIGN.md §8.5. One day of the week: weekday, icon, rain probability, the range bar
 * on the scale the caller computed **across the whole week**, the printed low and high
 * (a colored bar is not a number), and the day's ribbon of light underneath.
 *
 * Everything arrives formatted; [scaleLowC]/[scaleHighC] are the week's own extremes,
 * shared by all seven rows so the week has a shape.
 */
@Composable
fun DayRow(
    dayLabel: String,
    icon: ImageVector,
    rainPct: Int,
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
    Column(
        modifier = modifier
            .fillMaxWidth()
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .semantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = dayLabel,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.width(44.dp)
            )
            Icon(
                imageVector = icon,
                contentDescription = null, // the row speaks once, via its semantics
                tint = Color.Unspecified,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = "$rainPct%",
                style = MaterialTheme.typography.labelSmall.tabular(),
                color = if (rainPct > 0) {
                    ChiaroTheme.colors.rainAt(rainPct)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                textAlign = TextAlign.End,
                modifier = Modifier.width(36.dp)
            )
            Text(
                text = lowLabel,
                style = MaterialTheme.typography.labelLarge.tabular(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                modifier = Modifier.width(34.dp)
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
                modifier = Modifier.width(34.dp)
            )
        }
        DaylightRibbon(
            phases = phases,
            nowFraction = null,
            description = "", // idem: one announcement per row
            height = 4.dp,
            modifier = Modifier.padding(start = 52.dp)
        )
    }
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
