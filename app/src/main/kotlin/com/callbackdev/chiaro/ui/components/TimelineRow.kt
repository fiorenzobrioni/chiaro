package com.callbackdev.chiaro.ui.components

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.ui.theme.ChiaroTheme
import com.callbackdev.chiaro.ui.theme.tabular

/**
 * DESIGN.md §8.4. One entry of the merged day: time, glyph, one line of prose. Sun
 * events, weather turns and (from Fase 6) the reader's own alerts all use this same
 * row — only the leading glyph differs, which is what makes the list read as one
 * timeline instead of three.
 *
 * The verdict chip slot of §8.4 arrives with the Sky engine wiring (Fase 5); the row
 * takes an optional trailing composable so that lands here without a reshape.
 */
@Composable
fun TimelineRow(
    time: String,
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = time,
            style = MaterialTheme.typography.labelLarge.tabular(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(48.dp)
        )
        Icon(
            imageVector = icon,
            contentDescription = null, // the prose beside it says the word
            tint = Color.Unspecified,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        trailing?.invoke()
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun TimelineRowPreview() {
    ChiaroTheme(dynamicColor = false) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TimelineRow(
                time = "17:04",
                icon = ImageVector.vectorResource(R.drawable.mc_rain),
                text = "Pioggia probabile (72%)"
            )
            TimelineRow(
                time = "19:12",
                icon = ImageVector.vectorResource(R.drawable.mc_sunset),
                text = "Ora d'oro"
            )
            TimelineRow(
                time = "20:06",
                icon = ImageVector.vectorResource(R.drawable.mc_horizon),
                text = "Tramonto"
            )
            TimelineRow(
                time = "22:41",
                icon = ImageVector.vectorResource(R.drawable.mc_moonrise),
                text = "Sorge la luna"
            )
        }
    }
}
