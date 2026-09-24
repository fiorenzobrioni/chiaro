package com.callbackdev.chiaro.ui.settings

import android.app.LocaleManager
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.sp
import com.callbackdev.chiaro.ui.components.VerdictChip
import com.callbackdev.chiaro.ui.components.VerdictKind
import com.callbackdev.chiaro.ui.format.Formats
import com.callbackdev.chiaro.ui.icons.ConditionGlyph
import com.callbackdev.chiaro.ui.icons.ConditionIcon
import com.callbackdev.chiaro.ui.theme.ChiaroTheme
import com.callbackdev.chiaro.ui.theme.LocalChiaroType
import com.callbackdev.chiaro.ui.theme.SkyPalette
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.callbackdev.chiaro.ui.format.currentLocale
import com.callbackdev.chiaro.ui.theme.SectionBottom
import com.callbackdev.chiaro.ui.theme.SectionTop
import com.callbackdev.chiaro.BuildConfig
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.data.AppFont
import com.callbackdev.chiaro.data.AppPalette
import com.callbackdev.chiaro.data.AppSettings
import com.callbackdev.chiaro.data.ThemeMode
import com.callbackdev.chiaro.data.UpdateFrequencies
import com.callbackdev.chiaro.data.WeatherIcons
import com.callbackdev.chiaro.domain.settings.TemperatureUnit
import com.callbackdev.chiaro.domain.settings.WindSpeedUnit

/**
 * Settings (VISION §5.7): standard M3 preferences, grouped, and the guide's front
 * door. Groups appear WITH the feature they control — notifications arrive with the
 * alert engine's surface (Fase 6), widgets with the widgets (Fase 8) — because a
 * switch that changes nothing yet would be the screen lying about what the app can
 * do (DESIGN §1.1).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    onOpenGuide: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        // Until the store's first answer the list is simply not there: half a screen
        // of defaults that might be about to change is a lie with good intentions.
        settings?.let { current ->
            SettingsList(
                settings = current,
                actions = SettingsActions(
                    setTemperatureUnit = { viewModel.setTemperatureUnit(it) },
                    setWindSpeedUnit = { viewModel.setWindSpeedUnit(it) },
                    setThemeMode = { viewModel.setThemeMode(it) },
                    setDynamicColor = { viewModel.setDynamicColor(it) },
                    setPalette = { viewModel.setPalette(it) },
                    setFont = { viewModel.setFont(it) },
                    setAnimatedIcons = { viewModel.setAnimatedIcons(it) },
                    setWeatherIcons = { viewModel.setWeatherIcons(it) },
                    setUpdateFrequency = { viewModel.setUpdateFrequency(it) },
                    resetToDefaults = { viewModel.resetToDefaults() }
                ),
                onOpenGuide = onOpenGuide,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
        }
    }
}

/** What the list can ask of the store, as functions: the list is then a plain composable
 * a test or a preview can draw. */
internal class SettingsActions(
    val setTemperatureUnit: (TemperatureUnit) -> Unit,
    val setWindSpeedUnit: (WindSpeedUnit) -> Unit,
    val setThemeMode: (ThemeMode) -> Unit,
    val setDynamicColor: (Boolean) -> Unit,
    val setPalette: (AppPalette) -> Unit,
    val setFont: (AppFont) -> Unit,
    val setAnimatedIcons: (Boolean) -> Unit,
    val setWeatherIcons: (WeatherIcons) -> Unit,
    val setUpdateFrequency: (Int) -> Unit,
    val resetToDefaults: () -> Unit
)

/**
 * The list, redrawn on the design review of 23 set 2026: the guide as a card of its own at
 * the top, every group on one rounded ground (the grouping the Alerts screen got the same
 * day, so the two settings-like screens of the app look like one app), a live preview of
 * the appearance above the choices that change it, the privacy note as a statement rather
 * than a paragraph among the credits, and the credits in a group of their own.
 */
@Composable
private fun SettingsList(
    settings: AppSettings,
    actions: SettingsActions,
    onOpenGuide: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var dialog by remember { mutableStateOf<SettingsDialog?>(null) }

    LazyColumn(modifier = modifier, contentPadding = PaddingValues(bottom = 24.dp)) {
        // The guide first: the row a new reader is here for — now a card, because it is
        // the one thing on this screen that is not a setting.
        item { GuideCard(onOpenGuide) }

        item { GroupHeader(stringResource(R.string.settings_group_units)) }
        item {
            SettingsGroup {
                ValueRow(
                    label = stringResource(R.string.settings_temperature),
                    value = temperatureLabel(settings.units.temperature),
                    onClick = { dialog = SettingsDialog.TEMPERATURE }
                )
                GroupDivider()
                ValueRow(
                    label = stringResource(R.string.settings_wind),
                    value = windLabel(settings.units.windSpeed),
                    onClick = { dialog = SettingsDialog.WIND }
                )
            }
        }

        item { GroupHeader(stringResource(R.string.settings_group_appearance)) }
        // What the five choices below look like together, drawn with them: the sky of
        // the reader's palette, their typeface, their icon set, a verdict in their colours.
        // A palette called «Carta» or «Brillante» is a word until it is seen.
        item { AppearancePreview(settings) }
        item {
            SettingsGroup {
                ValueRow(
                    label = stringResource(R.string.settings_theme),
                    value = themeLabel(settings.themeMode),
                    onClick = { dialog = SettingsDialog.THEME }
                )
                GroupDivider()
                ValueRow(
                    label = stringResource(R.string.settings_palette),
                    value = paletteLabel(settings.palette),
                    onClick = { dialog = SettingsDialog.PALETTE }
                )
                GroupDivider()
                ValueRow(
                    label = stringResource(R.string.settings_font),
                    value = fontLabel(settings.font),
                    onClick = { dialog = SettingsDialog.FONT }
                )
                GroupDivider()
                ValueRow(
                    label = stringResource(R.string.settings_weather_icons),
                    value = iconStyleLabel(settings.weatherIcons),
                    onClick = { dialog = SettingsDialog.ICONS }
                )
                GroupDivider()
                SwitchRow(
                    label = stringResource(R.string.settings_animated_icons),
                    note = stringResource(R.string.settings_animated_icons_note),
                    checked = settings.animatedIcons,
                    onChange = actions.setAnimatedIcons
                )
                GroupDivider()
                SwitchRow(
                    label = stringResource(R.string.settings_dynamic_color),
                    note = stringResource(R.string.settings_dynamic_color_note),
                    checked = settings.dynamicColor,
                    onChange = actions.setDynamicColor
                )
            }
        }

        item { GroupHeader(stringResource(R.string.settings_group_updates)) }
        item {
            SettingsGroup {
                ValueRow(
                    label = stringResource(R.string.settings_update_frequency),
                    value = frequencyLabel(settings.updateFrequencyMin),
                    onClick = { dialog = SettingsDialog.FREQUENCY }
                )
            }
        }

        item { GroupHeader(stringResource(R.string.settings_group_language)) }
        item {
            SettingsGroup {
                ValueRow(
                    label = stringResource(R.string.settings_language),
                    value = currentLanguageLabel(),
                    onClick = {
                        // The system per-app picker (minSdk 33): one place to change it,
                        // the same place every app has.
                        context.startActivity(
                            Intent(
                                Settings.ACTION_APP_LOCALE_SETTINGS,
                                Uri.fromParts("package", context.packageName, null)
                            )
                        )
                    }
                )
            }
        }

        // The privacy note, before the credits and on its own (committente, 23 set 2026:
        // «eccessivamente verbosa, vorrei una nota semplice e veritiera»).
        item { GroupHeader(stringResource(R.string.settings_privacy)) }
        item { PrivacyCard() }

        // The About block completed against tweather's own (committente, 8 set): who
        // wrote it, what it is licensed as, and what it is built out of. The series
        // ships the same facts on both apps, and the credits are exactly what
        // `licenses/README.md` says travels inside the APK — a bundled font and a
        // bundled icon family are somebody's work, and a screen that names the weather
        // provider and stops there is only two thirds honest.
        item { GroupHeader(stringResource(R.string.settings_group_about)) }
        item {
            SettingsGroup {
                InfoRow(stringResource(R.string.settings_version), BuildConfig.VERSION_NAME)
                GroupDivider()
                InfoRow(stringResource(R.string.settings_developer), stringResource(R.string.settings_developer_note))
                GroupDivider()
                InfoRow(stringResource(R.string.settings_copyright), stringResource(R.string.settings_copyright_note))
                GroupDivider()
                ValueRow(
                    label = stringResource(R.string.settings_license),
                    value = stringResource(R.string.settings_license_note),
                    onClick = { openUrl(context, "https://www.gnu.org/licenses/gpl-3.0.html") },
                    external = true
                )
                GroupDivider()
                ValueRow(
                    label = stringResource(R.string.settings_source_code),
                    value = stringResource(R.string.settings_source_code_note),
                    onClick = { openUrl(context, "https://github.com/fiorenzobrioni/chiaro") },
                    external = true
                )
            }
        }

        item { GroupHeader(stringResource(R.string.settings_group_credits)) }
        item {
            SettingsGroup {
                ValueRow(
                    label = stringResource(R.string.settings_data_source),
                    value = stringResource(R.string.settings_data_source_note),
                    onClick = { openUrl(context, "https://open-meteo.com") },
                    external = true
                )
                GroupDivider()
                // The official warnings' attribution (Fase 11): CC BY 4.0 asks for it, and
                // the sheet that shows a warning carries the same line where it is met.
                ValueRow(
                    label = stringResource(R.string.settings_credit_warnings),
                    value = stringResource(R.string.settings_credit_warnings_note),
                    onClick = {
                        openUrl(
                            context,
                            "https://mappe.protezionecivile.gov.it/it/mappe-rischi/bollettino-di-criticita/"
                        )
                    },
                    external = true
                )
                GroupDivider()
                ValueRow(
                    label = stringResource(R.string.settings_credit_icons),
                    value = stringResource(R.string.settings_credit_icons_note),
                    onClick = { openUrl(context, "https://github.com/basmilius/meteocons") },
                    external = true
                )
                GroupDivider()
                ValueRow(
                    label = stringResource(R.string.settings_credit_font),
                    value = fontCreditNote(settings.font),
                    onClick = { openUrl(context, fontCreditUrl(settings.font)) },
                    external = true
                )
                GroupDivider()
                ValueRow(
                    label = stringResource(R.string.settings_credit_ui_icons),
                    value = stringResource(R.string.settings_credit_ui_icons_note),
                    onClick = { openUrl(context, "https://github.com/google/material-design-icons") },
                    external = true
                )
            }
        }

        item {
            // Destructive styling, then a dialog that says exactly what resets and
            // what does not (VISION §5.7). A button at the foot rather than one more
            // row: it is the one thing on the screen that undoes the others.
            OutlinedButton(
                onClick = { dialog = SettingsDialog.RESET },
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp)
            ) {
                Text(stringResource(R.string.settings_reset))
            }
        }
    }

    when (dialog) {
        SettingsDialog.TEMPERATURE -> RadioDialog(
            title = stringResource(R.string.settings_temperature),
            options = TemperatureUnit.entries.map { it to temperatureLabel(it) },
            selected = settings.units.temperature,
            onSelect = { actions.setTemperatureUnit(it); dialog = null },
            onDismiss = { dialog = null }
        )
        SettingsDialog.WIND -> RadioDialog(
            title = stringResource(R.string.settings_wind),
            options = WindSpeedUnit.entries.map { it to windLabel(it) },
            selected = settings.units.windSpeed,
            onSelect = { actions.setWindSpeedUnit(it); dialog = null },
            onDismiss = { dialog = null }
        )
        SettingsDialog.THEME -> RadioDialog(
            title = stringResource(R.string.settings_theme),
            options = ThemeMode.entries.map { it to themeLabel(it) },
            selected = settings.themeMode,
            onSelect = { actions.setThemeMode(it); dialog = null },
            onDismiss = { dialog = null }
        )
        SettingsDialog.PALETTE -> RadioDialog(
            title = stringResource(R.string.settings_palette),
            explanation = paletteNote(settings.weatherIcons),
            options = AppPalette.entries.map { it to paletteLabel(it) },
            selected = settings.palette,
            onSelect = { actions.setPalette(it); dialog = null },
            onDismiss = { dialog = null }
        )
        SettingsDialog.FONT -> RadioDialog(
            title = stringResource(R.string.settings_font),
            explanation = stringResource(R.string.settings_font_note),
            options = AppFont.entries.map { it to fontLabel(it) },
            selected = settings.font,
            onSelect = { actions.setFont(it); dialog = null },
            onDismiss = { dialog = null }
        )
        SettingsDialog.ICONS -> RadioDialog(
            title = stringResource(R.string.settings_weather_icons),
            explanation = stringResource(R.string.settings_weather_icons_note),
            options = WeatherIcons.entries.map { it to iconStyleLabel(it) },
            selected = settings.weatherIcons,
            onSelect = { actions.setWeatherIcons(it); dialog = null },
            onDismiss = { dialog = null }
        )
        SettingsDialog.FREQUENCY -> RadioDialog(
            title = stringResource(R.string.settings_update_frequency),
            explanation = stringResource(R.string.settings_update_frequency_note),
            options = UpdateFrequencies.map { it to frequencyLabel(it) },
            selected = settings.updateFrequencyMin,
            onSelect = { actions.setUpdateFrequency(it); dialog = null },
            onDismiss = { dialog = null }
        )
        SettingsDialog.RESET -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text(stringResource(R.string.settings_reset_dialog_title)) },
            text = { Text(stringResource(R.string.settings_reset_dialog_body)) },
            confirmButton = {
                TextButton(onClick = { actions.resetToDefaults(); dialog = null }) {
                    Text(
                        text = stringResource(R.string.settings_reset_confirm),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { dialog = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
        null -> Unit
    }
}

/** The guide's front door, as the one card on the screen that is not a setting. */
@Composable
private fun GuideCard(onOpenGuide: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = GroupShape,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(GroupShape)
            .clickable(onClick = onOpenGuide)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(Icons.Outlined.Info, contentDescription = null, modifier = Modifier.size(28.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.guide_entry_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.guide_entry_subtitle), style = MaterialTheme.typography.bodyMedium)
            }
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null)
        }
    }
}

/**
 * The appearance, drawn with itself (design review, 23 set 2026): a slice of the sky canvas
 * in the reader's palette at the golden hour — where the two palettes differ most — with a
 * temperature in their typeface and a condition in their icon set on it, and under it a
 * verdict and a rain figure in their semantic colours. Every choice in the group below
 * changes something in it, the moment it is made. A picture, silent to a screen reader:
 * the rows under it say every choice in words.
 */
@Composable
private fun AppearancePreview(settings: AppSettings) {
    val locale = currentLocale()
    val sky = ChiaroTheme.sky.gradient(sunAltitudeDeg = PreviewAltitude, cloudPct = 20)
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = GroupShape,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
            .clearAndSetSemantics { }
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(PreviewSky)
                    .background(Brush.verticalGradient(sky.stops()))
                    .background(
                        Brush.verticalGradient(
                            0.4f to Color.Transparent,
                            1f to SkyPalette.ScrimColor.copy(alpha = SkyPalette.ScrimAlpha)
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Text(
                        text = Formats.temperature(PreviewTempC, settings.units.temperature, locale),
                        style = LocalChiaroType.current.heroTemperature.copy(fontSize = 44.sp, lineHeight = 48.sp),
                        color = Color.White
                    )
                    Text(
                        text = stringResource(com.callbackdev.chiaro.ui.today.WeatherText.condition(PreviewCode)),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                }
                ConditionIcon(
                    glyph = ConditionGlyph(PreviewCode),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(64.dp)
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                VerdictChip(
                    kind = VerdictKind.PASS,
                    label = stringResource(R.string.verdict_pass),
                    evidence = stringResource(R.string.sky_evidence_cloud).format(20)
                )
                Text(
                    text = Formats.percent(PreviewRain, locale),
                    style = MaterialTheme.typography.labelLarge,
                    color = ChiaroTheme.colors.rainInkAt(PreviewRain)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                listOf(ChiaroTheme.colors.temperatureAt(8.0), ChiaroTheme.colors.temperatureAt(24.0))
                            )
                        )
                )
            }
        }
    }
}

/**
 * The privacy note (committente, 23 set 2026): «eccessivamente verbosa, vorrei una nota
 * semplice e veritiera della filosofia di privacy dell'app». Three facts, each checked
 * against the code: there is no account, no advertising and no analytics SDK in the build;
 * the app has no server of its own, so everything it keeps is on the phone; and the one
 * thing it sends is the request for the weather — to Open-Meteo, with a position the
 * location provider has already rounded to two decimals (~1 km), and no identifier.
 */
@Composable
private fun PrivacyCard() {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = GroupShape,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(Icons.Outlined.Lock, contentDescription = null, modifier = Modifier.size(24.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.settings_privacy_headline), style = MaterialTheme.typography.titleSmall)
                Text(stringResource(R.string.settings_privacy_body), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private enum class SettingsDialog {
    TEMPERATURE, WIND, THEME, PALETTE, FONT, ICONS, FREQUENCY, RESET
}

@Composable
private fun GroupHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(
            start = 20.dp, end = 16.dp, top = SectionTop, bottom = SectionBottom
        )
    )
}

/** Rows that belong together, on one rounded ground (the Alerts screen's, 23 set 2026). */
@Composable
private fun SettingsGroup(content: @Composable () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = GroupShape,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(GroupShape)
    ) {
        Column { content() }
    }
}

@Composable
private fun GroupDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

/** A preference row: what it is, what it currently says, tap to change — or, with
 * [external], a credit whose tap leaves the app, which the trailing mark says. */
@Composable
private fun ValueRow(label: String, value: String, onClick: () -> Unit, external: Boolean = false) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (external) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null, // the row's label says where it goes
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

/** A fact with nothing to change. */
@Composable
private fun InfoRow(label: String, value: String) {
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** A switch row: the row is the touch target, the switch only shows the state. */
@Composable
private fun SwitchRow(label: String, note: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = { onChange(!checked) }, role = Role.Switch)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(note, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

private val GroupShape = RoundedCornerShape(24.dp)
private val PreviewSky = 132.dp
/** The golden hour: where the two palettes' skies differ most. */
private const val PreviewAltitude = 3.0
private const val PreviewTempC = 21.0
private const val PreviewCode = 2
private const val PreviewRain = 30

/**
 * One dialog shape for every multiple-choice preference: pickers, never a free-text
 * field for a value with a range (the same property the alert builder keeps, §5.4).
 */
@Composable
private fun <T> RadioDialog(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
    explanation: String? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.selectableGroup()) {
                if (explanation != null) {
                    Text(
                        text = explanation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
                options.forEach { (value, label) ->
                    androidx.compose.foundation.layout.Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = value == selected,
                                onClick = { onSelect(value) },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 10.dp)
                    ) {
                        RadioButton(selected = value == selected, onClick = null)
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

@Composable
private fun temperatureLabel(unit: TemperatureUnit): String = when (unit) {
    TemperatureUnit.CELSIUS -> stringResource(R.string.settings_temp_celsius)
    TemperatureUnit.FAHRENHEIT -> stringResource(R.string.settings_temp_fahrenheit)
}

@Composable
private fun windLabel(unit: WindSpeedUnit): String = when (unit) {
    WindSpeedUnit.KMH -> stringResource(R.string.settings_wind_kmh)
    WindSpeedUnit.MPH -> stringResource(R.string.settings_wind_mph)
}

@Composable
private fun fontLabel(font: AppFont): String = when (font) {
    AppFont.INTER -> stringResource(R.string.settings_font_inter)
    AppFont.GOOGLE_SANS -> stringResource(R.string.settings_font_google_sans)
    AppFont.SYSTEM -> stringResource(R.string.settings_font_system)
}

/**
 * The typefaces credit, and which of them the reader is actually reading.
 *
 * Both bundled faces travel in the APK whatever the setting says — one is the default,
 * the other is one tap away — so the OFL attribution names both, always. What the choice
 * changes is which one is on the screen, and a credits row that said "Inter" to somebody
 * reading the app in Google Sans would be the screen telling them something that is not
 * true (DESIGN §1.1, the same rule the palette note learned).
 */
@Composable
private fun fontCreditNote(font: AppFont): String {
    val credit = stringResource(R.string.settings_credit_font_note)
    val inUse = when (font) {
        AppFont.SYSTEM -> stringResource(R.string.settings_credit_font_inuse_system)
        else -> stringResource(R.string.settings_credit_font_inuse, fontLabel(font))
    }
    return "$credit — $inUse"
}

/** The tap goes where the credit points: to the face being read, or — when that face is
 * the phone's and belongs to nobody this app can credit — to the licence the two bundled
 * ones share. */
private fun fontCreditUrl(font: AppFont): String = when (font) {
    AppFont.INTER -> "https://rsms.me/inter/"
    AppFont.GOOGLE_SANS -> "https://fonts.google.com/specimen/Google+Sans"
    AppFont.SYSTEM -> "https://openfontlicense.org"
}

@Composable
private fun iconStyleLabel(style: WeatherIcons): String = when (style) {
    WeatherIcons.FILL -> stringResource(R.string.settings_icons_fill)
    WeatherIcons.LINE -> stringResource(R.string.settings_icons_line)
}

/**
 * What the palette actually does, told to the reader in front of it.
 *
 * The dress reaches the weather icons only through the LINE set's dark-ground sibling
 * (`mcn_*`, DESIGN.md §13.1); the fill set is Meteocons' own palette and the same files
 * under both dresses. The note used to promise "brighter weather icons" to everybody,
 * which is a screen telling a reader on the fill set something that will not happen
 * when they tap — so it now says which of the two they are in.
 */
@Composable
private fun paletteNote(icons: WeatherIcons): String = stringResource(R.string.settings_palette_note) +
    " " + stringResource(
        when (icons) {
            WeatherIcons.LINE -> R.string.settings_palette_note_icons_line
            WeatherIcons.FILL -> R.string.settings_palette_note_icons_fill
        }
    )

@Composable
private fun paletteLabel(palette: AppPalette): String = when (palette) {
    AppPalette.PAPER -> stringResource(R.string.settings_palette_paper)
    AppPalette.VIVID -> stringResource(R.string.settings_palette_vivid)
}

@Composable
private fun themeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.SYSTEM -> stringResource(R.string.settings_theme_system)
    ThemeMode.LIGHT -> stringResource(R.string.settings_theme_light)
    ThemeMode.DARK -> stringResource(R.string.settings_theme_dark)
}

@Composable
private fun frequencyLabel(minutes: Int): String = when (minutes) {
    15 -> stringResource(R.string.settings_freq_15)
    30 -> stringResource(R.string.settings_freq_30)
    60 -> stringResource(R.string.settings_freq_60)
    else -> stringResource(R.string.settings_freq_120)
}

/** What the app is speaking right now: the reader's pick, or the phone's language. */
@Composable
private fun currentLanguageLabel(): String {
    val context = LocalContext.current
    val appLocales = context.getSystemService(LocaleManager::class.java).applicationLocales
    if (appLocales.isEmpty) return stringResource(R.string.settings_language_system)
    val locale = appLocales[0]
    return locale.getDisplayLanguage(locale)
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(currentLocale()) else it.toString() }
}

private fun openUrl(context: android.content.Context, url: String) {
    // A phone with no browser answers with nothing happening, which is better than
    // a crash and honest enough for a link that also states its destination.
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
}
