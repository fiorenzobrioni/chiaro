package com.callbackdev.chiaro.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.data.AppPalette
import com.callbackdev.chiaro.data.ServiceLocator
import com.callbackdev.chiaro.data.ThemeMode
import com.callbackdev.chiaro.ui.theme.ChiaroTheme
import com.callbackdev.chiaro.ui.theme.WidgetCardColor
import com.callbackdev.chiaro.ui.theme.widgetCardContainer
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

        val settingsStore = ServiceLocator.settingsStore(applicationContext)
        setContent {
            // This screen is part of the app, so it wears what the app wears: the
            // reader's theme, their answer on wallpaper colors, and their dress (§2.5).
            // Until the store's first emission the defaults hold, which is also what a
            // fresh install chose.
            val settings by settingsStore.settings.collectAsStateWithLifecycle(initialValue = null)
            ChiaroTheme(
                darkTheme = when (settings?.themeMode) {
                    ThemeMode.LIGHT -> false
                    ThemeMode.DARK -> true
                    ThemeMode.SYSTEM, null -> isSystemInDarkTheme()
                },
                dynamicColor = settings?.dynamicColor ?: false,
                palette = settings?.palette ?: AppPalette.VIVID
            ) {
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
    // Content options are not the same for all of them: the Sky card has none, the text
    // card draws no icons, and a switch that changes nothing must not be offered.
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
            BackgroundSection(current) { next ->
                look = next
                scope.launch {
                    lookStore.set(appWidgetId, next)
                    repaint()
                }
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

            // Offered on every card that draws weather glyphs, and the reason to pick a
            // family here is the card's own — its size, its ground, the wallpaper behind
            // it (see [WidgetIcons]). On the text widget it appears only once the glyph
            // has been turned on below: a switch that changes nothing must not be offered,
            // and until then that card draws none.
            if (kind != WidgetKind.TEXT || current.showIcon) {
                SectionLabel(stringResource(R.string.widget_config_icons))
                val iconOptions = listOf(
                    WidgetIcons.APP to stringResource(R.string.widget_icons_app),
                    WidgetIcons.FILL to stringResource(R.string.settings_icons_fill),
                    WidgetIcons.LINE to stringResource(R.string.settings_icons_line)
                )
                iconOptions.forEach { (icons, label) ->
                    ChoiceRow(
                        label = label,
                        selected = current.icons == icons,
                        onPick = { save(current.copy(icons = icons)) }
                    )
                }
            }

            // Now, Today and the text card carry the day's sentence and may hide it;
            // Today and the text card carry the day's range (the text card earns it the
            // same way Today does — it has a column of facts to put it in, where the Now
            // card had only the sentence's own edge to crowd); only Now has a one-row card
            // that can be laid two ways. The Sky widget's content is its subscriptions,
            // chosen on the Sky screen, so it has no content switch to offer here.
            if (kind == WidgetKind.NOW || kind == WidgetKind.TODAY || kind == WidgetKind.TEXT) {
                SectionLabel(stringResource(R.string.widget_config_content))
                // The text card's one picture, and the first thing to decide about it —
                // above the sentence, because it is the switch that changes what KIND of
                // card this is rather than what the card says (committente, 20 set 2026).
                if (kind == WidgetKind.TEXT) {
                    SwitchRow(
                        label = stringResource(R.string.widget_config_show_icon),
                        note = stringResource(R.string.widget_config_show_icon_note),
                        checked = current.showIcon,
                        onToggle = { save(current.copy(showIcon = it)) }
                    )
                }
                SwitchRow(
                    label = stringResource(R.string.widget_config_show_sentence),
                    note = stringResource(R.string.widget_config_show_sentence_note),
                    checked = current.showSentence,
                    onToggle = { save(current.copy(showSentence = it)) }
                )
                if (kind == WidgetKind.TODAY || kind == WidgetKind.TEXT) {
                    SwitchRow(
                        label = stringResource(R.string.widget_config_show_range),
                        note = stringResource(R.string.widget_config_show_range_note),
                        checked = current.showDayRange,
                        onToggle = { save(current.copy(showDayRange = it)) }
                    )
                }
                // Fase 11: on by default, and on a day with no warning it changes
                // nothing at all — which is the whole argument for leaving it on.
                SwitchRow(
                    label = stringResource(R.string.widget_config_show_warning),
                    note = stringResource(R.string.widget_config_show_warning_note),
                    checked = current.showWarning,
                    onToggle = { save(current.copy(showWarning = it)) }
                )
            }
            if (kind == WidgetKind.NOW) {
                SectionLabel(stringResource(R.string.widget_config_arrangement))
                val arrangements = listOf(
                    WidgetArrangement.ICON_START to
                        stringResource(R.string.widget_arrangement_icon_start),
                    WidgetArrangement.ICON_END to
                        stringResource(R.string.widget_arrangement_icon_end)
                )
                arrangements.forEach { (arrangement, label) ->
                    ChoiceRow(
                        label = label,
                        selected = current.arrangement == arrangement,
                        onPick = { save(current.copy(arrangement = arrangement)) }
                    )
                }
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

/**
 * The background choices, shared by this screen and the arc widget's own (19 set 2026):
 * the sky, light, dark, the system — and a colour, whose six options only appear once the
 * reader has picked it. Nested rather than six more rows in the same list, because the
 * question is really two: what KIND of card, and then which colour, and a flat list of ten
 * makes the first question look like a colour picker with four odd entries in it.
 *
 * Shared because the two screens were already printing the same four rows from two copies
 * of the same list, and a fifth kind is exactly the change that makes one of the copies
 * quietly out of date.
 */
@Composable
internal fun BackgroundSection(look: WidgetLook, onPick: (WidgetLook) -> Unit) {
    SectionLabel(stringResource(R.string.widget_config_background))
    listOf(
        WidgetBackground.SKY to stringResource(R.string.widget_bg_sky),
        WidgetBackground.LIGHT to stringResource(R.string.settings_theme_light),
        WidgetBackground.DARK to stringResource(R.string.settings_theme_dark),
        WidgetBackground.SYSTEM to stringResource(R.string.settings_theme_system),
        WidgetBackground.COLOR to stringResource(R.string.widget_bg_color)
    ).forEach { (background, label) ->
        ChoiceRow(
            label = label,
            selected = look.background == background,
            onPick = { onPick(look.copy(background = background)) }
        )
    }
    if (look.background == WidgetBackground.COLOR) {
        listOf(
            WidgetCardColor.BLUE to stringResource(R.string.widget_color_blue),
            WidgetCardColor.AZURE to stringResource(R.string.widget_color_azure),
            WidgetCardColor.GREEN to stringResource(R.string.widget_color_green),
            WidgetCardColor.TEAL to stringResource(R.string.widget_color_teal),
            WidgetCardColor.PLUM to stringResource(R.string.widget_color_plum),
            WidgetCardColor.CLAY to stringResource(R.string.widget_color_clay)
        ).forEach { (color, label) ->
            ColorRow(
                label = label,
                color = widgetCardContainer(color),
                selected = look.cardColor == color,
                onPick = { onPick(look.copy(cardColor = color)) }
            )
        }
    }
}

/**
 * A colour's row: the radio, the name, and a swatch of the colour itself at the end. The
 * swatch is the one place in this app where a colour is offered as a colour, so it is also
 * the one place the name alone would not be enough — and the name is still there, in front
 * of it, because a swatch is not a label (DESIGN §10: a fill that carries meaning has a
 * word beside it).
 */
@Composable
private fun ColorRow(label: String, color: Color, selected: Boolean, onPick: () -> Unit) {
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
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp, end = 12.dp)
        )
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(color)
        )
    }
}

@Composable
internal fun SectionLabel(text: String) {
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
internal fun SwitchRow(
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
internal fun ChoiceRow(label: String, selected: Boolean, onPick: () -> Unit) {
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
