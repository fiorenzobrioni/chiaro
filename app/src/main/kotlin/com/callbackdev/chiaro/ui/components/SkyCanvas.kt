package com.callbackdev.chiaro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.callbackdev.chiaro.ui.theme.ChiaroTheme
import com.callbackdev.chiaro.ui.theme.SkyGradient
import com.callbackdev.chiaro.ui.theme.SkyPalette

/** Where the top scrim band fades out, as a fraction of the canvas height. Today's
 * screen reads it to know how far white status-bar icons stay backed by scrim before
 * the bar must return to theme ink. */
const val SkyCanvasTopScrimEnd = 0.25f

/**
 * DESIGN.md §3 and §8.1. The gradient is the sky above the active city, computed by
 * [SkyPalette]; this composable paints it and guarantees the scrim — on BOTH text
 * bands since Fase 3: the bottom one under the temperature and the sentence, and a
 * symmetric top one under the place row and the status bar icons, now that the canvas
 * reaches the top edge of the screen.
 *
 * The scrims are not decoration and not optional: the canvas is the one surface in the
 * app that does not follow the reader's theme, so white text over an unscrimmed noon
 * sky would be about 1.3:1. `ScrimContractTest` pins the alpha at the value that clears
 * 4.5:1 for every altitude the palette can produce, which is why it is a constant here
 * and not a parameter — one constant, both bands.
 *
 * The bottom edge is STRAIGHT (committente, 4 set; reconsidered and kept, 8 set): the
 * canvas carried a 28dp round on its two bottom corners until then, which read as a
 * card floating over the scroll rather than as the sky the screen opens on. The sky has
 * no corners, so neither does the block that draws it — every other surface on the page
 * is inset by 16dp and rounded, and the one that is not is the ground, not a card.
 *
 * [minHeight] is a floor, not a size (8 set 2026): the canvas holds text measured in sp
 * — the 64sp temperature, a 22sp sentence that runs to two lines in Italian — inside a
 * height that used to be fixed in dp, and at 100% type a two-line sentence left 2dp of
 * room before the hero climbed into the place row; at 115% they overlapped by 30dp. The
 * block is now at least this tall and grows with what it holds, and the scrim bands
 * scale with it because they are fractions.
 */
@Composable
fun SkyCanvas(
    gradient: SkyGradient,
    modifier: Modifier = Modifier,
    minHeight: Dp = 280.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .background(Brush.verticalGradient(gradient.stops()))
            .background(
                Brush.verticalGradient(
                    0.00f to SkyPalette.ScrimColor.copy(alpha = SkyPalette.ScrimAlpha),
                    SkyCanvasTopScrimEnd to Color.Transparent,
                    0.45f to Color.Transparent,
                    1.00f to SkyPalette.ScrimColor.copy(alpha = SkyPalette.ScrimAlpha)
                )
            ),
        content = content
    )
}

@Preview(showBackground = true, heightDp = 440)
@Composable
private fun SkyCanvasPreview() {
    ChiaroTheme(dynamicColor = false) {
        androidx.compose.foundation.layout.Column {
            listOf(50.0 to "mezzogiorno", 3.0 to "ora d'oro", -4.0 to "ora blu", -30.0 to "notte")
                .forEach { (altitude, label) ->
                    SkyCanvas(gradient = SkyPalette.Paper.gradient(altitude), minHeight = 100.dp) {
                        androidx.compose.material3.Text(
                            text = label,
                            color = Color.White,
                            modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)
                        )
                    }
                }
        }
    }
}
