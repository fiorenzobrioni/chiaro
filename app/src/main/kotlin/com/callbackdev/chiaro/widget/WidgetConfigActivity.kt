package com.callbackdev.chiaro.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.callbackdev.chiaro.data.AppSettings
import com.callbackdev.chiaro.ui.theme.paletteFor
import com.callbackdev.chiaro.ui.today.SkySnapshot
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
import com.callbackdev.chiaro.data.AppFont
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
        val kind = ChiaroWidgets.kindOf(applicationContext, appWidgetId)
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
                palette = settings?.palette ?: AppPalette.VIVID,
                font = settings?.font ?: AppFont.GOOGLE_SANS
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
                        onDone = { finish() },
                        kind = kind
                    )
                }
            }
        }
    }
}

@Composable
internal fun ConfigContent(
    appWidgetId: Int,
    modifier: Modifier,
    onDone: () -> Unit,
    // Content options are not the same for all of them: the Sky card has none, the text
    // card draws no icons, and a switch that changes nothing must not be offered.
    kind: WidgetKind?
) {
    val context = androidx.compose.ui.platform.LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    val cityStore = remember { ServiceLocator.cityStore(context) }
    val widgetCityStore = remember { ServiceLocator.widgetCityStore(context) }
    val lookStore = remember { WidgetLookStore.get(context) }
    val appSettings by remember { ServiceLocator.settingsStore(context).settings }
        .collectAsStateWithLifecycle(initialValue = null)
    val cities by cityStore.cities.collectAsStateWithLifecycle(initialValue = emptyList())
    val pinnedFlow = remember(appWidgetId) {
        widgetCityStore.pinned.map { it[appWidgetId] }
    }
    val pinnedId by pinnedFlow.collectAsStateWithLifecycle(initialValue = null)
    var look by remember { mutableStateOf<WidgetLook?>(null) }
    LaunchedEffect(appWidgetId) { look = lookStore.lookFor(appWidgetId, kind) }
    // The preview's model (23 set 2026): the widget's own loader, re-run when the place
    // changes, with this screen's look laid over it so a tap shows before the store has
    // finished writing it.
    var base by remember { mutableStateOf<WidgetModel?>(null) }
    LaunchedEffect(appWidgetId, pinnedId) {
        base = runCatching { WidgetData.load(context, appWidgetId) }.getOrNull()
    }

    fun repaint() = scope.launch { runCatching { ChiaroWidgets.updateOne(context, appWidgetId) } }
    fun save(next: WidgetLook) {
        look = next
        scope.launch {
            lookStore.set(appWidgetId, next)
            repaint()
        }
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp)
    ) {
        WidgetPreviewSection(
            appWidgetId = appWidgetId,
            kind = kind,
            model = base?.let { model -> look?.let { model.copy(look = it) } ?: model }
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

        look?.let { current ->
            ConfigHeader(stringResource(R.string.widget_config_background))
            ConfigGroup {
                BackgroundSection(current, base?.content?.sky, appSettings, onPick = ::save)
                ConfigDivider()
                OpacityRow(
                    pct = current.opacityPct,
                    onChange = { look = current.copy(opacityPct = it) },
                    onDone = { look?.let(::save) }
                )
            }

            // Now, Today and the text card carry the day's sentence and may hide it;
            // Today and the text card carry the day's range (the text card earns it the
            // same way Today does — it has a column of facts to put it in, where the Now
            // card had only the sentence's own edge to crowd); only Now has a one-row card
            // that can be laid two ways. The Sky widget's content is its subscriptions,
            // chosen on the Sky screen, so it has no content switch to offer here.
            if (kind == WidgetKind.NOW || kind == WidgetKind.TODAY || kind == WidgetKind.TEXT) {
                val text = kind == WidgetKind.TEXT
                ConfigHeader(stringResource(R.string.widget_config_content))
                ConfigGroup {
                    ConfigSwitchRow(
                        label = stringResource(R.string.widget_config_show_sentence),
                        note = stringResource(R.string.widget_config_show_sentence_note),
                        checked = current.showSentence,
                        onToggle = { save(current.copy(showSentence = it)) }
                    )
                    if (kind == WidgetKind.TODAY || text) {
                        ConfigDivider()
                        ConfigSwitchRow(
                            label = stringResource(R.string.widget_config_show_range),
                            note = stringResource(
                                if (text) {
                                    R.string.widget_config_show_range_note_text
                                } else {
                                    R.string.widget_config_show_range_note
                                }
                            ),
                            checked = current.showDayRange,
                            onToggle = { save(current.copy(showDayRange = it)) }
                        )
                    }
                    // Fase 11: on by default, and on a day with no warning it changes
                    // nothing at all — which is the whole argument for leaving it on. The
                    // text card prints the level as a word rather than a chip, so it says so.
                    ConfigDivider()
                    ConfigSwitchRow(
                        label = stringResource(R.string.widget_config_show_warning),
                        note = stringResource(
                            if (text) {
                                R.string.widget_config_show_warning_note_text
                            } else {
                                R.string.widget_config_show_warning_note
                            }
                        ),
                        checked = current.showWarning,
                        onToggle = { save(current.copy(showWarning = it)) }
                    )
                    // The Today card's extra rank on a tall card (23 set 2026): the same
                    // switch as the text card's «Più tardi», saying what it is on this card.
                    if (kind == WidgetKind.TODAY) {
                        ConfigDivider()
                        ConfigSwitchRow(
                            label = stringResource(R.string.widget_config_show_days),
                            note = stringResource(R.string.widget_config_show_days_note),
                            checked = current.showLater,
                            onToggle = { save(current.copy(showLater = it)) }
                        )
                    }
                    if (text) {
                        // «Più tardi» and the glyph close the list: both only ever take
                        // space the card leaves empty, so they are the two switches whose
                        // effect depends on the size — which the preview above shows.
                        ConfigDivider()
                        ConfigSwitchRow(
                            label = stringResource(R.string.widget_config_show_later),
                            note = stringResource(R.string.widget_config_show_later_note),
                            checked = current.showLater,
                            onToggle = { save(current.copy(showLater = it)) }
                        )
                        ConfigDivider()
                        ConfigSwitchRow(
                            label = stringResource(R.string.widget_config_show_icon),
                            note = stringResource(R.string.widget_config_show_icon_note),
                            checked = current.showIcon,
                            onToggle = { save(current.copy(showIcon = it)) }
                        )
                    }
                }
            }
            // Offered on every card that draws weather glyphs, after the content so that
            // on the text card it lands right under the switch that brings it. The
            // reason to pick a family here is the card's own — its size, its ground, the
            // wallpaper behind it (see [WidgetIcons]). On the text widget it appears only once the glyph
            // has been turned on below: a switch that changes nothing must not be offered,
            // and until then that card draws none.
            if (kind != WidgetKind.TEXT || current.showIcon) {
                ConfigHeader(stringResource(R.string.widget_config_icons))
                ConfigGroup {
                    listOf(
                        WidgetIcons.APP to stringResource(R.string.widget_icons_app),
                        WidgetIcons.FILL to stringResource(R.string.settings_icons_fill),
                        WidgetIcons.LINE to stringResource(R.string.settings_icons_line)
                    ).forEach { (icons, label) ->
                        ConfigChoiceRow(
                            label = label,
                            selected = current.icons == icons,
                            onPick = { save(current.copy(icons = icons)) }
                        )
                    }
                }
            }

            if (kind == WidgetKind.NOW) {
                ConfigHeader(stringResource(R.string.widget_config_arrangement))
                ConfigGroup {
                    listOf(
                        WidgetArrangement.ICON_START to
                            stringResource(R.string.widget_arrangement_icon_start),
                        WidgetArrangement.ICON_END to
                            stringResource(R.string.widget_arrangement_icon_end)
                    ).forEach { (arrangement, label) ->
                        ConfigChoiceRow(
                            label = label,
                            selected = current.arrangement == arrangement,
                            onPick = { save(current.copy(arrangement = arrangement)) }
                        )
                    }
                }
            }
        }

        ConfigDoneButton(onDone)
    }
}

/**
 * What the background question offers, in the order it asks it: the sky, light, dark, the
 * system, and a colour. Data rather than a list built inside the composable, so that
 * `WidgetConfigChoicesTest` can hold the one promise this section makes — **every kind of
 * card, on every widget** — without a screenshot. A [WidgetBackground] added without a row
 * here now fails the build rather than going missing from five settings screens at once.
 */
internal val WidgetBackgroundChoices: List<Pair<WidgetBackground, Int>> = listOf(
    WidgetBackground.SKY to R.string.widget_bg_sky,
    WidgetBackground.LIGHT to R.string.settings_theme_light,
    WidgetBackground.DARK to R.string.settings_theme_dark,
    WidgetBackground.SYSTEM to R.string.settings_theme_system,
    WidgetBackground.COLOR to R.string.widget_bg_color
)

/** The six colours a [WidgetBackground.COLOR] card can wear, in the order they are asked,
 * and pinned the same way: a [WidgetCardColor] with no row is a colour no reader can pick. */
internal val WidgetCardColorChoices: List<Pair<WidgetCardColor, Int>> = listOf(
    WidgetCardColor.BLUE to R.string.widget_color_blue,
    WidgetCardColor.AZURE to R.string.widget_color_azure,
    WidgetCardColor.GREEN to R.string.widget_color_green,
    WidgetCardColor.TEAL to R.string.widget_color_teal,
    WidgetCardColor.PLUM to R.string.widget_color_plum,
    WidgetCardColor.CLAY to R.string.widget_color_clay
)

/**
 * The background question, shared by this screen and the arc widget's own (19 set 2026):
 * the sky, light, dark, the system — and a colour, whose six options only appear once the
 * reader has picked it. Nested rather than six more rows in the same list, because the
 * question is really two: what KIND of card, and then which colour, and a flat list of ten
 * makes the first question look like a colour picker with four odd entries in it.
 *
 * Shared because the two screens were already printing the same four rows from two copies
 * of the same list, and a fifth kind is exactly the change that makes one of the copies
 * quietly out of date. It is ONE composable for all five widgets on purpose: the card is
 * furniture on somebody's wallpaper whatever is printed on it, so the question is the same
 * question on every one of them, and «Un colore» has been on all five since the day it
 * landed (committente, 20 set 2026, asking for exactly that — it was already true).
 *
 * **Rows that show their answer since 23 set 2026**: each kind of card carries a swatch of
 * the ground it paints — the sky as it is right now when there is a report to draw it from,
 * the two fixed cards, the phone's half-and-half, the colour picked — and the six colours
 * are a strip of swatches rather than six more rows, with the chosen one's name printed
 * under the strip (a swatch is never the only label, DESIGN §10). The rows only: the
 * caller puts them on a [ConfigGroup] under its own heading, with the opacity after them.
 */
@Composable
internal fun BackgroundSection(
    look: WidgetLook,
    sky: SkySnapshot? = null,
    app: AppSettings? = null,
    onPick: (WidgetLook) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val dynamic = app?.dynamicColor ?: false
    val dress = app?.palette ?: AppPalette.VIVID
    val schemes = remember(dynamic, dress) { widgetSchemes(context, dynamic, dress) }
    val skyImage = remember(sky, dress) {
        sky?.let { skyGradientBitmap(it, 100, paletteFor(dress).sky).asImageBitmap() }
    }
    val colors = MaterialTheme.colorScheme
    val fallback = Brush.linearGradient(listOf(colors.primaryContainer, colors.tertiaryContainer))
    val night = isNight(context)
    val inset = 16.dp
    WidgetBackgroundChoices.forEach { (background, labelRes) ->
        ConfigChoiceRow(
            label = stringResource(labelRes),
            selected = look.background == background,
            onPick = { onPick(look.copy(background = background)) },
            inset = inset,
            trailing = {
                GroundSwatch(background, look, schemes, skyImage, night, fallback)
            }
        )
    }
    if (look.background == WidgetBackground.COLOR) {
        ColorSwatches(look.cardColor, start = inset + 40.dp, end = inset) {
            onPick(look.copy(cardColor = it))
        }
    }
}

/** One ground, the size of a card corner: a rounded rectangle, not a dot, because what is
 * being chosen is the card's whole face. */
@Composable
private fun GroundSwatch(
    background: WidgetBackground,
    look: WidgetLook,
    schemes: WidgetSchemes,
    skyImage: androidx.compose.ui.graphics.ImageBitmap?,
    night: Boolean,
    fallback: Brush
) {
    val app = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(10.dp)
    val modifier = Modifier
        .size(width = 44.dp, height = 30.dp)
        .clip(shape)
        .border(1.dp, app.outlineVariant, shape)
    when (background) {
        WidgetBackground.SKY -> if (skyImage != null) {
            androidx.compose.foundation.Image(
                bitmap = skyImage,
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = modifier
            )
        } else {
            Box(modifier.background(fallback))
        }
        // The phone's choice, drawn as both of its answers on a diagonal.
        WidgetBackground.SYSTEM -> androidx.compose.foundation.Canvas(modifier) {
            drawRect(schemes.light.surface)
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(size.width, 0f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(path, schemes.dark.surface)
        }
        else -> Box(
            modifier.background(widgetCardFill(background, schemes, night, 1f, look.cardColor))
        )
    }
}

/**
 * The six colours as swatches across the group, the chosen one ringed and ticked, and its
 * name under the strip — the swatch shows the colour, the word says which one it is.
 */
@Composable
private fun ColorSwatches(
    selected: WidgetCardColor,
    start: androidx.compose.ui.unit.Dp,
    end: androidx.compose.ui.unit.Dp,
    onPick: (WidgetCardColor) -> Unit
) {
    Column(modifier = Modifier.padding(start = start, end = end, bottom = 12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            WidgetCardColorChoices.forEach { (color, labelRes) ->
                val chosen = color == selected
                val name = stringResource(labelRes)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .border(
                            width = if (chosen) 2.dp else 0.dp,
                            color = if (chosen) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = CircleShape
                        )
                        .padding(if (chosen) 4.dp else 0.dp)
                        .clip(CircleShape)
                        .background(widgetCardContainer(color))
                        .selectable(selected = chosen, onClick = { onPick(color) }, role = Role.RadioButton)
                        .semantics { contentDescription = name }
                ) {
                    if (chosen) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            // Every card colour is a dark ground under white ink (§2.6).
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
        Text(
            text = stringResource(WidgetCardColorChoices.first { it.first == selected }.second),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}
