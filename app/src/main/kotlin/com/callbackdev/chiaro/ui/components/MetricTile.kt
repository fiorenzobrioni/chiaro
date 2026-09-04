package com.callbackdev.chiaro.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.callbackdev.chiaro.ui.icons.ChiaroIcons
import com.callbackdev.chiaro.ui.theme.tabular

/**
 * DESIGN.md §8.6 and §1.2: a number plus what to do about it.
 *
 * [meaning] is a required parameter, and that is the rule being enforced by the type
 * system rather than by a review: a metric with no honest second line does not belong on
 * the home screen, it belongs in the details sheet. UV 7 is not information; "burns in
 * about 25 minutes" is.
 */
@Composable
fun MetricTile(
    icon: ImageVector,
    label: String,
    value: String,
    meaning: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth().let { if (onClick != null) it.clickable(onClick = onClick) else it },
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // The header holds one line by construction, never by truncation: on a
            // 360dp screen the two columns leave the label 94dp beside the 24dp icon,
            // so the eight labels are written to fit that budget rather than trimmed
            // with an ellipsis. A label that outgrows it (a huge font scale) wraps and
            // keeps its words, which is the honest way to fail.
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null, // the label right beside it says the word
                    // DESIGN.md §13.1: the icons keep their own colors, here as
                    // everywhere else (HourStrip, DayRow, TimelineRow). A flat tint
                    // collapses a Meteocon into its silhouette, and the details grid is
                    // where that costs the most: the humidity drop loses the white %
                    // that makes it humidity and becomes any other drop, the barometer
                    // loses its needle and becomes a disc, the three particles of the
                    // air-quality mark merge into one blob. Reported from a device
                    // (4 set 2026): «due sembrano uguali».
                    tint = Color.Unspecified,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(text = value, style = MaterialTheme.typography.titleMedium.tabular())
            Text(
                text = meaning,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MetricTilePreview() {
    com.callbackdev.chiaro.ui.theme.ChiaroTheme(dynamicColor = false) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricTile(
                icon = ChiaroIcons.uv,
                label = "UV", value = "7", meaning = "Scotta in circa 25 minuti"
            )
            MetricTile(
                icon = ChiaroIcons.humidity,
                label = "Umidità", value = "44%", meaning = "Confortevole"
            )
            MetricTile(
                icon = ChiaroIcons.dewPoint,
                label = "Rugiada", value = "17°", meaning = "Leggermente appiccicosa"
            )
        }
    }
}
