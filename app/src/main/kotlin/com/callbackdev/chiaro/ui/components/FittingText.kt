package com.callbackdev.chiaro.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer

/**
 * The longest of [candidates] that fits its line, longest first (committente, 15 ott 2026):
 * each is measured at [style] against the width the line really has, so the choice follows the
 * phone's width, the language, the clock's format and the reader's type size, and the line
 * never wraps — a wrap would make the row a line taller. When not even the last fits (a huge
 * type size), the last is drawn anyway and wraps, which keeps its words (§10).
 *
 * Only the drawing is chosen: what is spoken is the caller's to say, and should be the longest.
 */
@Composable
fun FittingText(
    candidates: List<String>,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier
) {
    if (candidates.isEmpty()) return
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(modifier = modifier) {
        val max = constraints.maxWidth
        val chosen = remember(candidates, style, max, measurer) {
            candidates.firstOrNull { text ->
                measurer.measure(text = text, style = style, maxLines = 1, softWrap = false).size.width <= max
            } ?: candidates.last()
        }
        Text(text = chosen, style = style, color = color)
    }
}
