package com.callbackdev.chiaro.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.ui.theme.ChiaroTheme
import com.callbackdev.chiaro.ui.theme.tabular

/**
 * One cell of the hour strip: everything already formatted, because prose and formats
 * are the caller's job (they carry locale and unit settings; this carries layout).
 * [description] is the spoken form of the whole cell — one announcement, not four.
 */
data class HourCell(
    val hourLabel: String,
    val icon: ImageVector,
    val temperature: String,
    val rainPct: Int,
    val description: String
)

/**
 * DESIGN.md §8.3. Horizontal, 24 cells from the next full hour, each 56dp: hour, icon,
 * temperature, rain probability. The rain column prints even a 0 — an absent number
 * under one hour would read as "no data", and it is data.
 */
@Composable
fun HourStrip(
    hours: List<HourCell>,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(hours) { cell ->
            Column(
                modifier = Modifier
                    .width(56.dp)
                    .semantics { contentDescription = cell.description },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = cell.hourLabel,
                    style = MaterialTheme.typography.labelSmall.tabular(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = cell.icon,
                    contentDescription = null, // the cell speaks once, via its semantics
                    tint = Color.Unspecified, // Meteocons carry their own measured colors
                    modifier = Modifier.padding(vertical = 2.dp)
                )
                Text(
                    text = cell.temperature,
                    style = MaterialTheme.typography.labelLarge.tabular()
                )
                Text(
                    text = "${cell.rainPct}%",
                    style = MaterialTheme.typography.labelSmall.tabular(),
                    color = if (cell.rainPct > 0) {
                        ChiaroTheme.colors.rainAt(cell.rainPct)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun HourStripPreview() {
    ChiaroTheme(dynamicColor = false) {
        val rain = listOf(0, 0, 10, 40, 70, 80, 30, 5)
        HourStrip(
            hours = (0 until 8).map { i ->
                HourCell(
                    hourLabel = "${14 + i}",
                    icon = ImageVector.vectorResource(
                        if (rain[i] >= 40) R.drawable.mc_rain else R.drawable.mc_partly_cloudy_day
                    ),
                    temperature = "${22 - i}°",
                    rainPct = rain[i],
                    description = "Alle ${14 + i}, ${22 - i} gradi, pioggia ${rain[i]}%"
                )
            },
            modifier = Modifier.padding(16.dp)
        )
    }
}
