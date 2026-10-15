package com.callbackdev.chiaro.ui.today

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.callbackdev.chiaro.ui.components.SegmentedGroup
import com.callbackdev.chiaro.ui.icons.ConditionGlyph
import com.callbackdev.chiaro.ui.icons.WeatherIconSize

/** One revision as a row: the day and its verdict, then the figures behind it. */
internal class ChangedLine(val headline: String, val details: String, val glyph: ConditionGlyph?)

/**
 * VISION §5.2.6: each revision as one row (review of Today, 15 ott 2026). They were
 * sentences in `bodyMedium` — «Sabato è migliorato: pioggia 70% → 30%» — loose on the paper,
 * the one section of the page with nothing to anchor the eye. Now a group of the Sky's
 * segments: the day's sky as the week draws it now, the day and its verdict in words at
 * `bodyLarge` (the judgement is a word, never a colour, DESIGN §8.10), and the figures that
 * moved under it at `bodyMedium`. A revision with no printable numbers never becomes a row.
 */
@Composable
internal fun WhatChanged(lines: List<ChangedLine>) {
    SegmentedGroup(
        segments = lines.map { line ->
            {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 64.dp)
                        .semantics(mergeDescendants = true) {}
                        .padding(start = 12.dp, top = 8.dp, end = 16.dp, bottom = 8.dp)
                ) {
                    Box(modifier = Modifier.size(WeatherIconSize.Week)) {
                        line.glyph?.let { glyph ->
                            com.callbackdev.chiaro.ui.icons.ConditionIcon(
                                glyph = glyph,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(text = line.headline, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = line.details,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    )
}
