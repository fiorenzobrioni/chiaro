package com.callbackdev.chiaro.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
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
 * no corners, so neither does the block that draws it. What meets it since 23 set 2026 is
 * the page's own [SkySheet], rounded and laid over its last few dp — the page is the card
 * now, and the sky stays the ground under it.
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
    sheet: SkySheet? = null,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .drawBehind {
                drawRect(Brush.verticalGradient(gradient.stops()))
                val lip = sheet?.lip?.toPx() ?: 0f
                // The bottom band reaches its full scrim where the text ends, not at the
                // bottom edge: with a sheet over the canvas the last [SkySheet.lip] of
                // sky is under the page, and the band must not lose any of its alpha to
                // it (§3.6 is measured at full strength).
                val full = ((size.height - lip) / size.height).coerceIn(0.46f, 1f)
                drawRect(
                    Brush.verticalGradient(
                        0.00f to SkyPalette.ScrimColor.copy(alpha = SkyPalette.ScrimAlpha),
                        SkyCanvasTopScrimEnd to Color.Transparent,
                        0.45f to Color.Transparent,
                        full to SkyPalette.ScrimColor.copy(alpha = SkyPalette.ScrimAlpha),
                        1.00f to SkyPalette.ScrimColor.copy(alpha = SkyPalette.ScrimAlpha)
                    )
                )
                if (sheet != null) drawSheet(sheet, gradient)
            }
            .padding(bottom = sheet?.lip ?: 0.dp),
        content = content
    )
}

/**
 * The page's edge where it meets the sky (design review, 23 set 2026).
 *
 * Until then the canvas ended on a straight cut from its darkest band — the bottom scrim
 * at full strength — straight into the near-white page: the two most distant colors on
 * the screen, touching along a ruler line. The page now starts as a **sheet laid on the
 * sky**: its top [lip] overlaps the canvas with [Corner] rounded corners, so the sky shows
 * round them and the darkest strip of scrim is under the paper. This is not the rounded
 * canvas removed on 4 set — that made the sky a card floating over the page; here the sky
 * is still the ground and the page is what lies on it.
 *
 * And the sheet **catches the light** of the sky it lies on: its first [glowSpan] are
 * tinted with the canvas' own bottom stop at [GlowStrength], fading into [surface] — warm
 * at sunset, blue at noon, barely there at night. The glow reaches past the canvas' own
 * bounds on purpose (a lazy item is not clipped to itself): the items after it are
 * transparent and stand on it. It fades out well inside the height of the pinned place
 * row, so when the canvas item leaves the list the part that goes with it is already
 * hidden under that row's surface.
 */
@Immutable
data class SkySheet(
    val surface: Color,
    val lip: Dp = 24.dp,
    val glowSpan: Dp = 120.dp
)

private fun DrawScope.drawSheet(sheet: SkySheet, gradient: SkyGradient) {
    val lip = sheet.lip.toPx()
    val top = size.height - lip
    val glow = lerp(sheet.surface, gradient.bottom, GlowStrength)
    val span = sheet.glowSpan.toPx()
    // Eased rather than linear: a linear fade ends on a visible line where it meets
    // the plain surface.
    val brush = Brush.verticalGradient(
        0.0f to glow,
        0.35f to lerp(sheet.surface, glow, 0.55f),
        0.7f to lerp(sheet.surface, glow, 0.15f),
        1.0f to sheet.surface,
        startY = top,
        endY = top + span
    )
    val corner = CornerRadius(Corner.toPx())
    drawPath(
        Path().apply {
            addRoundRect(
                RoundRect(
                    left = 0f, top = top, right = size.width, bottom = size.height,
                    topLeftCornerRadius = corner, topRightCornerRadius = corner
                )
            )
        },
        brush = brush
    )
    // Below the canvas: the rest of the glow, over the list's own surface.
    drawRect(brush = brush, topLeft = Offset(0f, size.height), size = Size(size.width, span - lip))
}

private val Corner = 28.dp

/** How much of the sky's bottom stop the sheet's first pixel carries. */
private const val GlowStrength = 0.16f

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
