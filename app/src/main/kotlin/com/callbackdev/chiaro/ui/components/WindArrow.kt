package com.callbackdev.chiaro.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.callbackdev.chiaro.ui.theme.ChiaroTheme

/**
 * Where the air is going, as an arrow (card review, 8 set 2026).
 *
 * Meteorology names a wind by where it comes FROM — "vento da nord-est" — and an arrow is
 * read as movement, so the glyph points the other way, where the air is heading, and the
 * words beside it say the source. The two agree the way a weather map's arrows agree with
 * its captions, and a reader who only glances gets the direction without decoding a
 * compass abbreviation. It is a drawn mark and not a Material icon because the one arrow
 * in Material's core set is auto-mirrored for RTL, and a compass direction must not be.
 *
 * No semantics of its own: the words right beside it carry the direction (§10, never a
 * glyph alone), and a second announcement would be worse than none.
 */
@Composable
fun WindArrow(
    fromDegrees: Int,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    Canvas(modifier = modifier.size(16.dp)) {
        val stroke = 2.dp.toPx()
        val centerX = size.width / 2
        val top = stroke
        val bottom = size.height - stroke
        val head = size.width * 0.32f
        // Zero is "from the north", which blows south: the arrow points down, and the
        // rotation follows the wind round the compass from there.
        rotate(degrees = ((fromDegrees % 360) + 180).toFloat()) {
            drawLine(color, Offset(centerX, bottom), Offset(centerX, top), stroke, StrokeCap.Round)
            drawLine(color, Offset(centerX, top), Offset(centerX - head, top + head), stroke, StrokeCap.Round)
            drawLine(color, Offset(centerX, top), Offset(centerX + head, top + head), stroke, StrokeCap.Round)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WindArrowPreview() {
    ChiaroTheme(dynamicColor = false) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(8.dp)
        ) {
            listOf(0, 45, 90, 180, 270, 315).forEach { WindArrow(fromDegrees = it) }
        }
    }
}
