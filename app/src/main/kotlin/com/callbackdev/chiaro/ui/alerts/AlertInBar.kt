package com.callbackdev.chiaro.ui.alerts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.callbackdev.chiaro.data.warnings.PlaceWarningState
import com.callbackdev.chiaro.domain.warnings.WarningLevel
import com.callbackdev.chiaro.ui.icons.ChiaroIcons
import com.callbackdev.chiaro.ui.theme.forText
import com.callbackdev.chiaro.ui.warnings.WarningText
import com.callbackdev.chiaro.ui.warnings.WarningText.colors

/**
 * The level Alerts' bar carries once the hero's answer has scrolled away (DESIGN §8.9b,
 * 16 nov 2026), or null: the level of the bulletin in hand while it warns of something, the
 * same answer the hero gives in its level's colour. Null on a quiet day, and null while
 * there is no answer to give (no bulletin yet, an old one, a place with none): an all-clear
 * in the bar would be the green Today refuses to print (§1.1), and «not yet» is not a level.
 */
internal fun warningInBar(state: PlaceWarningState): WarningLevel? =
    (state as? PlaceWarningState.Current)?.warnings?.maxLevel?.takeIf { it != WarningLevel.NONE }

/**
 * Whether the hero's answer has gone under the bar (pure, `AlertInBarTest`): its middle has
 * climbed above the list's top edge, which on this page is the bar's bottom. Today's line
 * (`heroUnderBar`): one line and two states, no journey between them. [answerTop] and
 * [listTop] in the same frame, px.
 */
internal fun answerUnderBar(answerTop: Float, answerHeight: Int, listTop: Float): Boolean =
    answerTop + answerHeight / 2f < listTop

/**
 * The warning in the pinned bar: the level's mark and its phrase («Allerta gialla») on the
 * level's container, the chip the Sky's bar carries its verdict in (`VerdictChip`), at the
 * same size. A mark and a word before it is a colour (§2.3); one announcement.
 */
@Composable
internal fun WarningInBar(level: WarningLevel) {
    val palette = level.colors
    val phrase = stringResource(WarningText.phraseRes(level))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .padding(start = 8.dp)
            .background(palette.container, CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clearAndSetSemantics { contentDescription = phrase }
    ) {
        Icon(
            painter = painterResource(ChiaroIcons.warningMarkRes()),
            contentDescription = null, // the row's own semantics say the phrase
            tint = palette.ink,
            modifier = Modifier.size(MarkSize.forText())
        )
        Text(text = phrase, style = MaterialTheme.typography.labelLarge, color = palette.ink)
    }
}

/** The mark's box beside a `labelLarge` word, the verdict chip's. */
private val MarkSize = 14.dp
