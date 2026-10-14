package com.callbackdev.chiaro.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.callbackdev.chiaro.R

/**
 * The header of the Sky, Alerts and Journal tabs: the tab's name, the place under it, and
 * Settings (review of the Sky screen, point 10, 14 ott 2026).
 *
 * The three screens carried a copy each, and each copy made only the place's line
 * clickable: a `titleSmall` row about **20 dp** tall, under the 48 dp DESIGN §6 asks of
 * every touch target. The target is now the whole block, name and place together —
 * 28 + 20 = **48 dp** tall, at least 48 wide — which is also where a finger aims for "this
 * place". It is one button to a screen reader («Cielo, Milano», with «Cambia luogo» as
 * the action), so the chevron says nothing of its own. The block keeps its text where it
 * was: the 8 dp it is inset by are the ripple's, taken from the row's 16.
 *
 * One composable rather than three copies, so the next fix lands on the three tabs at once.
 * Today has its own header, over the sky, and is not this.
 */
@Composable
fun TabHeader(
    title: String,
    placeName: String?,
    onOpenPlaces: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 16.dp - TargetInset, end = 16.dp, top = 8.dp, bottom = 8.dp)
    ) {
        Box(modifier = Modifier.weight(1f)) {
            val switcher = stringResource(R.string.place_switcher_action)
            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .then(
                        if (placeName != null) {
                            Modifier
                                .clip(TargetShape)
                                .clickable(onClickLabel = switcher, role = Role.Button, onClick = onOpenPlaces)
                        } else {
                            Modifier
                        }
                    )
                    .defaultMinSize(minWidth = MinTarget, minHeight = MinTarget)
                    .padding(horizontal = TargetInset)
            ) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                if (placeName != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = placeName,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(
                            imageVector = Icons.Outlined.KeyboardArrowDown,
                            contentDescription = null, // the block's action says it
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
        IconButton(onClick = onOpenSettings) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = stringResource(R.string.settings_title)
            )
        }
    }
}

private val MinTarget = 48.dp
private val TargetInset = 8.dp
private val TargetShape = RoundedCornerShape(12.dp)
