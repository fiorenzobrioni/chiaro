package com.callbackdev.chiaro.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.data.ServiceLocator
import com.callbackdev.chiaro.ui.theme.ChiaroTheme
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * The per-widget settings, reached from the launcher's own long-press reconfigure
 * flow (device review, 3 set): the place this instance shows — the app's active one,
 * or a pinned saved city, so two widgets can watch two cities — its background, and
 * how solid the card is. Every choice persists as it is tapped and repaints the one
 * widget it belongs to; "Done" just closes the door.
 */
class WidgetConfigActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        // The host expects this result whether or not anything changes.
        setResult(
            RESULT_OK,
            Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        )

        setContent {
            ChiaroTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(title = { Text(stringResource(R.string.widget_config_title)) })
                    }
                ) { padding ->
                    ConfigContent(
                        appWidgetId = appWidgetId,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        onDone = { finish() }
                    )
                }
            }
        }
    }
}

@Composable
private fun ConfigContent(appWidgetId: Int, modifier: Modifier, onDone: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    val cityStore = remember { ServiceLocator.cityStore(context) }
    val widgetCityStore = remember { ServiceLocator.widgetCityStore(context) }
    val lookStore = remember { WidgetLookStore.get(context) }
    // Content options are not the same for all three: only the Now widget can put the
    // state beside its number, and only Now and Today carry a day at all. A switch that
    // changes nothing must not be offered.
    val kind = remember(appWidgetId) { ChiaroWidgets.kindOf(context, appWidgetId) }

    val cities by cityStore.cities.collectAsStateWithLifecycle(initialValue = emptyList())
    val pinnedFlow = remember(appWidgetId) {
        widgetCityStore.pinned.map { it[appWidgetId] }
    }
    val pinnedId by pinnedFlow.collectAsStateWithLifecycle(initialValue = null)
    var look by remember { mutableStateOf<WidgetLook?>(null) }
    LaunchedEffect(appWidgetId) { look = lookStore.lookFor(appWidgetId) }

    fun repaint() = scope.launch { runCatching { ChiaroWidgets.updateOne(context, appWidgetId) } }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)
    ) {
        SectionLabel(stringResource(R.string.widget_config_place))
        ChoiceRow(
            label = stringResource(R.string.widget_config_active_place),
            selected = pinnedId == null,
            onPick = {
                scope.launch {
                    widgetCityStore.unpin(appWidgetId)
                    repaint()
                }
            }
        )
        cities.forEach { city ->
            ChoiceRow(
                label = city.name,
                selected = pinnedId == city.id,
                onPick = {
                    scope.launch {
                        widgetCityStore.pin(appWidgetId, city.id)
                        repaint()
                    }
                }
            )
        }

        look?.let { current ->
            SectionLabel(stringResource(R.string.widget_config_background))
            val options = listOf(
                WidgetBackground.SKY to stringResource(R.string.widget_bg_sky),
                WidgetBackground.LIGHT to stringResource(R.string.settings_theme_light),
                WidgetBackground.DARK to stringResource(R.string.settings_theme_dark),
                WidgetBackground.SYSTEM to stringResource(R.string.settings_theme_system)
            )
            options.forEach { (background, label) ->
                ChoiceRow(
                    label = label,
                    selected = current.background == background,
                    onPick = {
                        val next = current.copy(background = background)
                        look = next
                        scope.launch {
                            lookStore.set(appWidgetId, next)
                            repaint()
                        }
                    }
                )
            }

            SectionLabel(stringResource(R.string.widget_config_opacity))
            Text(
                text = when (current.opacityPct) {
                    100 -> stringResource(R.string.settings_opacity_full)
                    0 -> stringResource(R.string.widget_opacity_transparent)
                    else -> "${current.opacityPct}%"
                },
                style = MaterialTheme.typography.titleMedium
            )
            Slider(
                value = current.opacityPct.toFloat(),
                onValueChange = { raw ->
                    look = current.copy(opacityPct = ((raw / 5f).roundToInt() * 5))
                },
                onValueChangeFinished = {
                    look?.let { final ->
                        scope.launch {
                            lookStore.set(appWidgetId, final)
                            repaint()
                        }
                    }
                },
                valueRange = 0f..100f,
                steps = 19
            )

            fun save(next: WidgetLook) {
                look = next
                scope.launch {
                    lookStore.set(appWidgetId, next)
                    repaint()
                }
            }

            if (kind == WidgetKind.NOW || kind == WidgetKind.TODAY) {
                SectionLabel(stringResource(R.string.widget_config_content))
                if (kind == WidgetKind.NOW) {
                    SwitchRow(
                        label = stringResource(R.string.widget_config_show_condition),
                        note = stringResource(R.string.widget_config_show_condition_note),
                        checked = current.showCondition,
                        onToggle = { save(current.copy(showCondition = it)) }
                    )
                }
                SwitchRow(
                    label = stringResource(R.string.widget_config_show_range),
                    note = stringResource(R.string.widget_config_show_range_note),
                    checked = current.showDayRange,
                    onToggle = { save(current.copy(showDayRange = it)) }
                )
            }
        }

        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Text(stringResource(R.string.action_done))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
    )
}

/** A toggle with the sentence that says what it costs — the same shape the Settings
 * screen gives every switch, so a reader meets one control, not two. */
@Composable
private fun SwitchRow(
    label: String,
    note: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, onValueChange = onToggle, role = Role.Switch)
            .padding(vertical = 10.dp)
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = note,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun ChoiceRow(label: String, selected: Boolean, onPick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onPick, role = Role.RadioButton)
            .padding(vertical = 10.dp)
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}
