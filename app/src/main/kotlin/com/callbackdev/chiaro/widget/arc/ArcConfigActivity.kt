package com.callbackdev.chiaro.widget.arc

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.callbackdev.chiaro.data.AppFont
import com.callbackdev.chiaro.data.AppPalette
import com.callbackdev.chiaro.data.ServiceLocator
import com.callbackdev.chiaro.data.ThemeMode
import com.callbackdev.chiaro.ui.theme.ChiaroTheme
import com.callbackdev.chiaro.widget.BackgroundSection
import com.callbackdev.chiaro.widget.ConfigChoiceRow
import com.callbackdev.chiaro.widget.ConfigChoices
import com.callbackdev.chiaro.widget.ConfigDivider
import com.callbackdev.chiaro.widget.ConfigDoneButton
import com.callbackdev.chiaro.widget.ConfigGroup
import com.callbackdev.chiaro.widget.ConfigHeader
import com.callbackdev.chiaro.widget.ConfigPage
import com.callbackdev.chiaro.widget.ConfigResetButton
import com.callbackdev.chiaro.widget.ConfigSwitch
import com.callbackdev.chiaro.widget.ConfigSwitchRow
import com.callbackdev.chiaro.widget.ConfigSwitches
import com.callbackdev.chiaro.widget.OpacityRow
import com.callbackdev.chiaro.widget.WidgetPreviewSection
import com.callbackdev.chiaro.widget.ChiaroWidgets
import com.callbackdev.chiaro.widget.WidgetData
import com.callbackdev.chiaro.widget.WidgetIcons
import com.callbackdev.chiaro.widget.WidgetKind
import com.callbackdev.chiaro.widget.WidgetLook
import com.callbackdev.chiaro.widget.WidgetLookStore
import com.callbackdev.chiaro.widget.WidgetModel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

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
        enableEdgeToEdge()
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
                palette = settings?.palette ?: AppPalette.VIVID,
                font = settings?.font ?: AppFont.GOOGLE_SANS
            ) {
                Scaffold(
                    topBar = {
                        // «Questo widget», as on every widget's settings (16 Oct 2026): the
                        // preview under it already shows which widget this is.
                        TopAppBar(title = { Text(stringResource(R.string.widget_config_title)) })
                    }
                ) { padding ->
                    ConfigPage(padding) {
                        ArcConfigContent(
                            appWidgetId = appWidgetId,
                            modifier = Modifier.fillMaxSize(),
                            onDone = { finish() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun ArcConfigContent(appWidgetId: Int, modifier: Modifier, onDone: () -> Unit) {
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
        look = lookStore.lookFor(appWidgetId, WidgetKind.ARC)
        arc = arcStore.settingsFor(appWidgetId)
    }
    // The preview's model: the widget's own loader, re-run when the place changes. The
    // look is applied on top from this screen's state, so a tap shows before the store
    // has finished writing it.
    var base by remember { mutableStateOf<WidgetModel?>(null) }
    LaunchedEffect(appWidgetId, pinnedId) {
        base = runCatching { WidgetData.load(context, appWidgetId) }.getOrNull()
    }
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

    val appSettings by remember { ServiceLocator.settingsStore(context).settings }
        .collectAsStateWithLifecycle(initialValue = null)

    // The shared screen's grammar (23 set 2026): the card first, then every question on a
    // rounded group under its own heading. The arc has more questions than the other four
    // cards put together, so the groups are what keep a long page scannable.
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp)
    ) {
        WidgetPreviewSection(
            appWidgetId = appWidgetId,
            kind = WidgetKind.ARC,
            model = base?.let { model -> look?.let { model.copy(look = it) } ?: model },
            arc = arc
        )

        ConfigHeader(stringResource(R.string.widget_config_place))
        ConfigGroup {
            ConfigChoiceRow(
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
                ConfigChoiceRow(
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
        }

        // ---- The card: the look every widget shares. ----
        look?.let { current ->
            ConfigHeader(stringResource(R.string.widget_config_background))
            ConfigGroup {
                BackgroundSection(current, base?.content?.sky, appSettings) { next -> saveLook(next) }
                ConfigDivider()
                OpacityRow(
                    pct = current.opacityPct,
                    onChange = { look = current.copy(opacityPct = it) },
                    onDone = { look?.let { saveLook(it) } }
                )
            }

            ConfigHeader(stringResource(R.string.widget_config_icons))
            ConfigChoices(
                listOf(
                    WidgetIcons.APP to stringResource(R.string.widget_icons_app),
                    WidgetIcons.FILL to stringResource(R.string.settings_icons_fill),
                    WidgetIcons.LINE to stringResource(R.string.settings_icons_line)
                ),
                selected = current.icons,
                onPick = { saveLook(current.copy(icons = it)) }
            )
        }

        // ---- What the card is: the arc, or the followed moments (13 ott 2026). ----
        arc?.let { current ->
            ConfigHeader(stringResource(R.string.arc_config_drawing))
            ConfigChoices(
                listOf(
                    ArcDrawing.ARC to stringResource(R.string.arc_drawing_arc),
                    ArcDrawing.MOMENTS to stringResource(R.string.arc_drawing_moments)
                ),
                selected = current.drawing,
                onPick = { saveArc(current.copy(drawing = it)) }
            )
            if (current.drawing == ArcDrawing.MOMENTS) {
                Text(
                    text = stringResource(R.string.arc_drawing_moments_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp)
                )
            }
        }

        // ---- The arc itself: none of it applies to a card of moments, so it is not offered. ----
        arc?.takeIf { it.drawing == ArcDrawing.ARC }?.let { current ->
            ConfigHeader(stringResource(R.string.arc_config_span))
            ConfigChoices(
                listOf(
                    ArcSpan.TODAY to stringResource(R.string.arc_span_today),
                    ArcSpan.AHEAD to stringResource(R.string.arc_span_ahead)
                ),
                selected = current.span,
                onPick = { saveArc(current.copy(span = it)) }
            )

            ConfigHeader(stringResource(R.string.arc_config_ground))
            ConfigChoices(
                listOf(
                    ArcGround.BANDS to stringResource(R.string.arc_ground_bands),
                    ArcGround.RIBBON to stringResource(R.string.arc_ground_ribbon),
                    ArcGround.NONE to stringResource(R.string.arc_ground_none)
                ),
                selected = current.ground,
                onPick = { saveArc(current.copy(ground = it)) }
            )

            ConfigHeader(stringResource(R.string.arc_config_layers))
            ConfigSwitches(
                listOf(
                    ConfigSwitch(
                        stringResource(R.string.arc_layer_sun),
                        stringResource(R.string.arc_layer_sun_note),
                        current.sunPath
                    ) { saveArc(current.copy(sunPath = it)) },
                    ConfigSwitch(
                        stringResource(R.string.arc_layer_moon),
                        stringResource(R.string.arc_layer_moon_note),
                        current.moon
                    ) { saveArc(current.copy(moon = it)) },
                    ConfigSwitch(
                        stringResource(R.string.arc_layer_rain),
                        stringResource(R.string.arc_layer_rain_note),
                        current.rain
                    ) { saveArc(current.copy(rain = it)) },
                    ConfigSwitch(
                        stringResource(R.string.arc_layer_now),
                        stringResource(R.string.arc_layer_now_note),
                        current.nowMarker
                    ) { saveArc(current.copy(nowMarker = it)) },
                    ConfigSwitch(
                        stringResource(R.string.arc_layer_fade),
                        stringResource(R.string.arc_layer_fade_note),
                        current.fadePast
                    ) { saveArc(current.copy(fadePast = it)) },
                    ConfigSwitch(
                        stringResource(R.string.arc_layer_hours),
                        stringResource(R.string.arc_layer_hours_note),
                        current.hourLabels
                    ) { saveArc(current.copy(hourLabels = it)) },
                    ConfigSwitch(
                        stringResource(R.string.arc_layer_temperatures),
                        stringResource(R.string.arc_layer_temperatures_note),
                        current.temperatures
                    ) { saveArc(current.copy(temperatures = it)) }
                )
            )

            // ---- The words: which sentence heads the card, and the day's range. ----
            ConfigHeader(stringResource(R.string.arc_config_words))
            ConfigGroup {
                listOf(
                    ArcHero.NEXT_MOMENT to stringResource(R.string.arc_hero_next),
                    ArcHero.HEADLINE to stringResource(R.string.arc_hero_headline),
                    ArcHero.NONE to stringResource(R.string.arc_hero_none)
                ).forEach { (hero, label) ->
                    ConfigChoiceRow(
                        label = label,
                        selected = current.hero == hero,
                        onPick = { saveArc(current.copy(hero = hero)) }
                    )
                }
                look?.let { currentLook ->
                    ConfigDivider()
                    ConfigSwitchRow(
                        label = stringResource(R.string.widget_config_show_range),
                        note = stringResource(R.string.arc_range_note),
                        checked = currentLook.showDayRange,
                        onToggle = { saveLook(currentLook.copy(showDayRange = it)) }
                    )
                }
            }

            ConfigHeader(stringResource(R.string.arc_config_dial))
            ConfigChoices(
                listOf(
                    ArcDialFigure.TEMPERATURE to stringResource(R.string.arc_dial_temperature),
                    ArcDialFigure.NEXT_TIME to stringResource(R.string.arc_dial_next_time)
                ),
                selected = current.dialFigure,
                onPick = { saveArc(current.copy(dialFigure = it)) }
            )

            ConfigHeader(stringResource(R.string.arc_config_agenda))
            ConfigSwitches(
                listOf(
                    ConfigSwitch(
                        stringResource(R.string.arc_agenda_sun),
                        stringResource(R.string.arc_agenda_sun_note),
                        current.agendaSun
                    ) { saveArc(current.copy(agendaSun = it)) },
                    ConfigSwitch(
                        stringResource(R.string.arc_agenda_moon),
                        stringResource(R.string.arc_agenda_moon_note),
                        current.agendaMoon
                    ) { saveArc(current.copy(agendaMoon = it)) },
                    ConfigSwitch(
                        stringResource(R.string.arc_agenda_rain),
                        stringResource(R.string.arc_agenda_rain_note),
                        current.agendaRain
                    ) { saveArc(current.copy(agendaRain = it)) },
                    ConfigSwitch(
                        stringResource(R.string.arc_agenda_verdicts),
                        stringResource(R.string.arc_agenda_verdicts_note),
                        current.agendaVerdicts
                    ) { saveArc(current.copy(agendaVerdicts = it)) }
                )
            )

            // The official warning (Fase 11) and the week each had a heading of their own
            // over a single switch; on groups they are one group of two, «Altro sulla card».
            ConfigHeader(stringResource(R.string.arc_config_more))
            ConfigSwitches(
                listOf(
                    ConfigSwitch(
                        stringResource(R.string.arc_warning_switch),
                        stringResource(R.string.arc_config_warning_note),
                        current.warning
                    ) { saveArc(current.copy(warning = it)) },
                    ConfigSwitch(
                        stringResource(R.string.arc_week_switch),
                        stringResource(R.string.arc_week_note),
                        current.week
                    ) { saveArc(current.copy(week = it)) }
                )
            )

            ConfigHeader(stringResource(R.string.arc_config_density))
            ConfigChoices(
                listOf(
                    ArcDensity.COMFORTABLE to stringResource(R.string.arc_density_comfortable),
                    ArcDensity.COMPACT to stringResource(R.string.arc_density_compact)
                ),
                selected = current.density,
                onPick = { saveArc(current.copy(density = it)) }
            )
        }

        // The one control that undoes the others, on a card of moments as on an arc.
        if (arc != null) {
            ConfigResetButton {
                saveArc(ArcSettings())
                saveLook(WidgetLook.defaultsFor(WidgetKind.ARC))
            }
        }
        ConfigDoneButton(onDone)
    }
}
