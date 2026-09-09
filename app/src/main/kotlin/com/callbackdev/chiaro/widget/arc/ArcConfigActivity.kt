package com.callbackdev.chiaro.widget.arc

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.data.AppPalette
import com.callbackdev.chiaro.data.ServiceLocator
import com.callbackdev.chiaro.data.ThemeMode
import com.callbackdev.chiaro.ui.theme.ChiaroTheme
import com.callbackdev.chiaro.widget.ChiaroWidgets
import com.callbackdev.chiaro.widget.ChoiceRow
import com.callbackdev.chiaro.widget.SectionLabel
import com.callbackdev.chiaro.widget.SwitchRow
import com.callbackdev.chiaro.widget.WidgetBackground
import com.callbackdev.chiaro.widget.WidgetData
import com.callbackdev.chiaro.widget.WidgetIcons
import com.callbackdev.chiaro.widget.WidgetLook
import com.callbackdev.chiaro.widget.WidgetLookStore
import com.callbackdev.chiaro.widget.WidgetModel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * The arc widget's own settings screen, from the launcher's reconfigure flow. It has
 * more to decide than the shared screen offers the other three — what the arc spans,
 * what it is drawn on, which layers, which words, which agenda, the week, the density —
 * and a card that reshapes itself at eight sizes is hard to picture from a list of
 * switches, so the screen opens on a preview of the card, at a size the reader picks,
 * drawn by the very code the launcher will run. Every choice persists as it is tapped
 * and repaints the one widget it belongs to; «Done» just closes the door.
 */
class ArcConfigActivity : ComponentActivity() {

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
                        TopAppBar(title = { Text(stringResource(R.string.widget_arc_label)) })
                    }
                ) { padding ->
                    ArcConfigContent(
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
private fun ArcConfigContent(appWidgetId: Int, modifier: Modifier, onDone: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    val cityStore = remember { ServiceLocator.cityStore(context) }
    val widgetCityStore = remember { ServiceLocator.widgetCityStore(context) }
    val lookStore = remember { WidgetLookStore.get(context) }
    val arcStore = remember { ArcSettingsStore.get(context) }

    val cities by cityStore.cities.collectAsStateWithLifecycle(initialValue = emptyList())
    val pinnedFlow = remember(appWidgetId) { widgetCityStore.pinned.map { it[appWidgetId] } }
    val pinnedId by pinnedFlow.collectAsStateWithLifecycle(initialValue = null)
    var look by remember { mutableStateOf<WidgetLook?>(null) }
    var arc by remember { mutableStateOf<ArcSettings?>(null) }
    LaunchedEffect(appWidgetId) {
        look = lookStore.lookFor(appWidgetId)
        arc = arcStore.settingsFor(appWidgetId)
    }
    // The preview's model: the widget's own loader, re-run when the place changes. The
    // look is applied on top from this screen's state, so a tap shows before the store
    // has finished writing it.
    var base by remember { mutableStateOf<WidgetModel?>(null) }
    LaunchedEffect(appWidgetId, pinnedId) {
        base = runCatching { WidgetData.load(context, appWidgetId) }.getOrNull()
    }
    var previewSize by remember { mutableStateOf(ArcPreviewSize.FOUR_BY_TWO) }

    fun repaint() = scope.launch { runCatching { ChiaroWidgets.updateOne(context, appWidgetId) } }
    fun saveLook(next: WidgetLook) {
        look = next
        scope.launch {
            lookStore.set(appWidgetId, next)
            repaint()
        }
    }
    fun saveArc(next: ArcSettings) {
        arc = next
        scope.launch {
            arcStore.set(appWidgetId, next)
            repaint()
        }
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // ---- The preview, and the sizes it can be seen at. ----
        SectionLabel(stringResource(R.string.arc_config_preview))
        ArcPreview(
            model = base?.let { model -> look?.let { model.copy(look = it) } ?: model },
            arc = arc,
            size = previewSize.size,
            modifier = Modifier.padding(vertical = 4.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ArcPreviewSize.entries.forEach { option ->
                FilterChip(
                    selected = previewSize == option,
                    onClick = { previewSize = option },
                    label = { Text(option.label) }
                )
            }
        }
        Text(
            text = stringResource(formNote(arcForm(previewSize.size))),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )

        // ---- The place. ----
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

        // ---- The card: the look every widget shares. ----
        look?.let { current ->
            SectionLabel(stringResource(R.string.widget_config_background))
            listOf(
                WidgetBackground.SKY to stringResource(R.string.widget_bg_sky),
                WidgetBackground.LIGHT to stringResource(R.string.settings_theme_light),
                WidgetBackground.DARK to stringResource(R.string.settings_theme_dark),
                WidgetBackground.SYSTEM to stringResource(R.string.settings_theme_system)
            ).forEach { (background, label) ->
                ChoiceRow(
                    label = label,
                    selected = current.background == background,
                    onPick = { saveLook(current.copy(background = background)) }
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
                onValueChange = { raw -> look = current.copy(opacityPct = (raw / 5f).roundToInt() * 5) },
                onValueChangeFinished = { look?.let { saveLook(it) } },
                valueRange = 0f..100f,
                steps = 19
            )

            SectionLabel(stringResource(R.string.widget_config_icons))
            listOf(
                WidgetIcons.APP to stringResource(R.string.widget_icons_app),
                WidgetIcons.FILL to stringResource(R.string.settings_icons_fill),
                WidgetIcons.LINE to stringResource(R.string.settings_icons_line)
            ).forEach { (icons, label) ->
                ChoiceRow(
                    label = label,
                    selected = current.icons == icons,
                    onPick = { saveLook(current.copy(icons = icons)) }
                )
            }
        }

        // ---- The arc itself. ----
        arc?.let { current ->
            SectionLabel(stringResource(R.string.arc_config_span))
            ChoiceRow(
                label = stringResource(R.string.arc_span_today),
                selected = current.span == ArcSpan.TODAY,
                onPick = { saveArc(current.copy(span = ArcSpan.TODAY)) }
            )
            ChoiceRow(
                label = stringResource(R.string.arc_span_ahead),
                selected = current.span == ArcSpan.AHEAD,
                onPick = { saveArc(current.copy(span = ArcSpan.AHEAD)) }
            )

            SectionLabel(stringResource(R.string.arc_config_ground))
            listOf(
                ArcGround.BANDS to stringResource(R.string.arc_ground_bands),
                ArcGround.RIBBON to stringResource(R.string.arc_ground_ribbon),
                ArcGround.NONE to stringResource(R.string.arc_ground_none)
            ).forEach { (ground, label) ->
                ChoiceRow(
                    label = label,
                    selected = current.ground == ground,
                    onPick = { saveArc(current.copy(ground = ground)) }
                )
            }

            SectionLabel(stringResource(R.string.arc_config_layers))
            SwitchRow(
                label = stringResource(R.string.arc_layer_sun),
                note = stringResource(R.string.arc_layer_sun_note),
                checked = current.sunPath,
                onToggle = { saveArc(current.copy(sunPath = it)) }
            )
            SwitchRow(
                label = stringResource(R.string.arc_layer_moon),
                note = stringResource(R.string.arc_layer_moon_note),
                checked = current.moon,
                onToggle = { saveArc(current.copy(moon = it)) }
            )
            SwitchRow(
                label = stringResource(R.string.arc_layer_rain),
                note = stringResource(R.string.arc_layer_rain_note),
                checked = current.rain,
                onToggle = { saveArc(current.copy(rain = it)) }
            )
            SwitchRow(
                label = stringResource(R.string.arc_layer_now),
                note = stringResource(R.string.arc_layer_now_note),
                checked = current.nowMarker,
                onToggle = { saveArc(current.copy(nowMarker = it)) }
            )
            SwitchRow(
                label = stringResource(R.string.arc_layer_fade),
                note = stringResource(R.string.arc_layer_fade_note),
                checked = current.fadePast,
                onToggle = { saveArc(current.copy(fadePast = it)) }
            )
            SwitchRow(
                label = stringResource(R.string.arc_layer_hours),
                note = stringResource(R.string.arc_layer_hours_note),
                checked = current.hourLabels,
                onToggle = { saveArc(current.copy(hourLabels = it)) }
            )
            SwitchRow(
                label = stringResource(R.string.arc_layer_temperatures),
                note = stringResource(R.string.arc_layer_temperatures_note),
                checked = current.temperatures,
                onToggle = { saveArc(current.copy(temperatures = it)) }
            )

            // ---- The words. ----
            SectionLabel(stringResource(R.string.arc_config_words))
            listOf(
                ArcHero.NEXT_MOMENT to stringResource(R.string.arc_hero_next),
                ArcHero.HEADLINE to stringResource(R.string.arc_hero_headline),
                ArcHero.NONE to stringResource(R.string.arc_hero_none)
            ).forEach { (hero, label) ->
                ChoiceRow(
                    label = label,
                    selected = current.hero == hero,
                    onPick = { saveArc(current.copy(hero = hero)) }
                )
            }
            look?.let { currentLook ->
                SwitchRow(
                    label = stringResource(R.string.widget_config_show_range),
                    note = stringResource(R.string.arc_range_note),
                    checked = currentLook.showDayRange,
                    onToggle = { saveLook(currentLook.copy(showDayRange = it)) }
                )
            }

            SectionLabel(stringResource(R.string.arc_config_dial))
            ChoiceRow(
                label = stringResource(R.string.arc_dial_temperature),
                selected = current.dialFigure == ArcDialFigure.TEMPERATURE,
                onPick = { saveArc(current.copy(dialFigure = ArcDialFigure.TEMPERATURE)) }
            )
            ChoiceRow(
                label = stringResource(R.string.arc_dial_next_time),
                selected = current.dialFigure == ArcDialFigure.NEXT_TIME,
                onPick = { saveArc(current.copy(dialFigure = ArcDialFigure.NEXT_TIME)) }
            )

            // ---- The agenda. ----
            SectionLabel(stringResource(R.string.arc_config_agenda))
            SwitchRow(
                label = stringResource(R.string.arc_agenda_sun),
                note = stringResource(R.string.arc_agenda_sun_note),
                checked = current.agendaSun,
                onToggle = { saveArc(current.copy(agendaSun = it)) }
            )
            SwitchRow(
                label = stringResource(R.string.arc_agenda_moon),
                note = stringResource(R.string.arc_agenda_moon_note),
                checked = current.agendaMoon,
                onToggle = { saveArc(current.copy(agendaMoon = it)) }
            )
            SwitchRow(
                label = stringResource(R.string.arc_agenda_rain),
                note = stringResource(R.string.arc_agenda_rain_note),
                checked = current.agendaRain,
                onToggle = { saveArc(current.copy(agendaRain = it)) }
            )
            SwitchRow(
                label = stringResource(R.string.arc_agenda_verdicts),
                note = stringResource(R.string.arc_agenda_verdicts_note),
                checked = current.agendaVerdicts,
                onToggle = { saveArc(current.copy(agendaVerdicts = it)) }
            )

            // ---- The week. ----
            SectionLabel(stringResource(R.string.arc_config_week))
            SwitchRow(
                label = stringResource(R.string.arc_week_switch),
                note = stringResource(R.string.arc_week_note),
                checked = current.week,
                onToggle = { saveArc(current.copy(week = it)) }
            )

            // ---- The density. ----
            SectionLabel(stringResource(R.string.arc_config_density))
            ChoiceRow(
                label = stringResource(R.string.arc_density_comfortable),
                selected = current.density == ArcDensity.COMFORTABLE,
                onPick = { saveArc(current.copy(density = ArcDensity.COMFORTABLE)) }
            )
            ChoiceRow(
                label = stringResource(R.string.arc_density_compact),
                selected = current.density == ArcDensity.COMPACT,
                onPick = { saveArc(current.copy(density = ArcDensity.COMPACT)) }
            )

            TextButton(
                onClick = {
                    saveArc(ArcSettings())
                    saveLook(WidgetLook())
                },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text(stringResource(R.string.arc_config_reset))
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

/** One sentence under the preview saying what the chosen size shows. */
private fun formNote(form: ArcForm): Int = when (form) {
    ArcForm.DIAL -> R.string.arc_form_dial
    ArcForm.STRIP -> R.string.arc_form_strip
    ArcForm.CARD -> R.string.arc_form_card
    ArcForm.PANEL -> R.string.arc_form_panel
    ArcForm.BOARD -> R.string.arc_form_board
}
