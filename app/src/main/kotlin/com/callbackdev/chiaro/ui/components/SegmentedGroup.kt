package com.callbackdev.chiaro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.callbackdev.chiaro.ui.theme.GroupCorner
import com.callbackdev.chiaro.ui.theme.GroupInnerCorner

/**
 * Rows that belong together, as Material 3 Expressive groups them (DESIGN §8.8, review of
 * the Sky screen, 14 ott 2026): each row on its own `surfaceContainer` segment, [SegmentGap]
 * apart, the group's outer corners round and the inner ones tight. One language for every
 * group in the app (the Alerts review, point 1): until then Sky drew its rows this way while
 * Alerts and Settings put theirs on one ground with hairlines between them.
 *
 * [segments] rather than a content slot, because the shape of a segment depends on where it
 * stands, and a group whose rows come and go (a level that shows only while its switch is on)
 * must round the right ones.
 */
@Composable
fun SegmentedGroup(
    segments: List<@Composable () -> Unit>,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(SegmentGap),
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)
    ) {
        segments.forEachIndexed { index, segment ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(segmentShape(index, segments.size))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
            ) {
                segment()
            }
        }
    }
}

/** The segment of a group at [index] of [count]: round outside, tight inside. */
fun segmentShape(index: Int, count: Int): Shape {
    val top = if (index == 0) GroupCorner else GroupInnerCorner
    val bottom = if (index == count - 1) GroupCorner else GroupInnerCorner
    return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}

/** The air between two segments of a group. A lazy list that draws one segment per item
 * gives each half of it as vertical padding. */
val SegmentGap = 2.dp
