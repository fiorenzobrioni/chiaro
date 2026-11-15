package com.callbackdev.chiaro.widget.pictures

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.data.AppSettings
import com.callbackdev.chiaro.domain.pictures.PictureSpan
import com.callbackdev.chiaro.ui.today.SkySnapshot
import com.callbackdev.chiaro.widget.BackgroundSection
import com.callbackdev.chiaro.widget.ConfigDivider
import com.callbackdev.chiaro.widget.ConfigGroup
import com.callbackdev.chiaro.widget.ConfigHeader
import com.callbackdev.chiaro.widget.ConfigSwitchRow
import com.callbackdev.chiaro.widget.OpacityRow
import com.callbackdev.chiaro.widget.WidgetLook

/**
 * «In immagini»'s own settings (Fase 17.3), in the canvas's order (`Config.dc.html`): what the
 * drawing spans, what is on the card, the ground, how solid the card is. The card itself at the
 * top and the place are the family's (`WidgetConfigActivity`), and so is the reset under them.
 *
 * «Il disegno» is two segments because it is one question with two answers, and the note under
 * them says what the chosen one draws; «Sulla card» is the five switches with a note each, the
 * canvas's words. The icon family is not offered: the canvas does not, and the card draws the
 * app's family as every reader of the app sees it.
 */
@Composable
internal fun InPicturesSettings(
    current: WidgetLook,
    sky: SkySnapshot?,
    app: AppSettings?,
    onSave: (WidgetLook) -> Unit,
    onOpacity: (Int) -> Unit,
    onOpacityDone: () -> Unit
) {
    ConfigHeader(stringResource(R.string.widget_config_pictures_span))
    ConfigGroup {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            val options = listOf(
                PictureSpan.NEXT_24_HOURS to stringResource(R.string.widget_config_pictures_next),
                PictureSpan.DAY to stringResource(R.string.widget_config_pictures_day)
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                options.forEachIndexed { index, (span, label) ->
                    SegmentedButton(
                        selected = current.picturesSpan == span,
                        onClick = { onSave(current.copy(picturesSpan = span)) },
                        shape = SegmentedButtonDefaults.itemShape(index, options.size)
                    ) { Text(label) }
                }
            }
            Text(
                text = stringResource(
                    if (current.picturesSpan == PictureSpan.DAY) {
                        R.string.widget_config_pictures_day_note
                    } else {
                        R.string.widget_config_pictures_next_note
                    }
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    }

    ConfigHeader(stringResource(R.string.widget_config_pictures_card))
    ConfigGroup {
        ConfigSwitchRow(
            label = stringResource(R.string.widget_config_pictures_glyph),
            note = stringResource(R.string.widget_config_pictures_glyph_note),
            checked = current.showIcon,
            onToggle = { onSave(current.copy(showIcon = it)) }
        )
        ConfigDivider()
        ConfigSwitchRow(
            label = stringResource(R.string.widget_config_pictures_rain),
            note = stringResource(R.string.widget_config_pictures_rain_note),
            checked = current.picturesRain,
            onToggle = { onSave(current.copy(picturesRain = it)) }
        )
        ConfigDivider()
        ConfigSwitchRow(
            label = stringResource(R.string.widget_config_pictures_ribbon),
            note = stringResource(R.string.widget_config_pictures_ribbon_note),
            checked = current.picturesRibbon,
            onToggle = { onSave(current.copy(picturesRibbon = it)) }
        )
        ConfigDivider()
        ConfigSwitchRow(
            label = stringResource(R.string.widget_config_pictures_days),
            note = stringResource(R.string.widget_config_pictures_days_note),
            checked = current.showLater,
            onToggle = { onSave(current.copy(showLater = it)) }
        )
        ConfigDivider()
        ConfigSwitchRow(
            label = stringResource(R.string.widget_config_pictures_warnings),
            note = stringResource(R.string.widget_config_pictures_warnings_note),
            checked = current.showWarning,
            onToggle = { onSave(current.copy(showWarning = it)) }
        )
    }

    ConfigHeader(stringResource(R.string.widget_config_background))
    ConfigGroup {
        BackgroundSection(current, sky, app, onPick = onSave)
    }

    ConfigHeader(stringResource(R.string.widget_config_transparency))
    ConfigGroup {
        OpacityRow(pct = current.opacityPct, onChange = onOpacity, onDone = onOpacityDone)
    }
}
