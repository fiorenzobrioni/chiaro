package com.callbackdev.chiaro.ui.settings

import android.app.LocaleManager
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
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
import com.callbackdev.chiaro.BuildConfig
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.data.AppSettings
import com.callbackdev.chiaro.data.ThemeMode
import com.callbackdev.chiaro.data.UpdateFrequencies
import com.callbackdev.chiaro.data.WeatherIcons
import com.callbackdev.chiaro.domain.settings.TemperatureUnit
import com.callbackdev.chiaro.domain.settings.WindSpeedUnit
import java.util.Locale

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
                viewModel = viewModel,
                onOpenGuide = onOpenGuide,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
        }
    }
}

@Composable
private fun SettingsList(
    settings: AppSettings,
    viewModel: SettingsViewModel,
    onOpenGuide: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var dialog by remember { mutableStateOf<SettingsDialog?>(null) }

    LazyColumn(modifier = modifier) {
        // The guide first: the row a new reader is here for.
        item {
            ListItem(
                headlineContent = { Text(stringResource(R.string.guide_entry_title)) },
                supportingContent = { Text(stringResource(R.string.guide_entry_subtitle)) },
                modifier = Modifier.clickable(onClick = onOpenGuide)
            )
            HorizontalDivider()
        }

        item { GroupHeader(stringResource(R.string.settings_group_units)) }
        item {
            ValueRow(
                label = stringResource(R.string.settings_temperature),
                value = temperatureLabel(settings.units.temperature),
                onClick = { dialog = SettingsDialog.TEMPERATURE }
            )
        }
        item {
            ValueRow(
                label = stringResource(R.string.settings_wind),
                value = windLabel(settings.units.windSpeed),
                onClick = { dialog = SettingsDialog.WIND }
            )
        }

        item { GroupHeader(stringResource(R.string.settings_group_appearance)) }
        item {
            ValueRow(
                label = stringResource(R.string.settings_theme),
                value = themeLabel(settings.themeMode),
                onClick = { dialog = SettingsDialog.THEME }
            )
        }
        item {
            ValueRow(
                label = stringResource(R.string.settings_weather_icons),
                value = iconStyleLabel(settings.weatherIcons),
                onClick = { dialog = SettingsDialog.ICONS }
            )
        }
        item {
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_dynamic_color)) },
                supportingContent = { Text(stringResource(R.string.settings_dynamic_color_note)) },
                trailingContent = {
                    Switch(
                        checked = settings.dynamicColor,
                        // The row is the touch target; the switch only shows the state.
                        onCheckedChange = null
                    )
                },
                modifier = Modifier.clickable(
                    onClick = { viewModel.setDynamicColor(!settings.dynamicColor) },
                    role = Role.Switch
                )
            )
        }

        item { GroupHeader(stringResource(R.string.settings_group_updates)) }
        item {
            ValueRow(
                label = stringResource(R.string.settings_update_frequency),
                value = frequencyLabel(settings.updateFrequencyMin),
                onClick = { dialog = SettingsDialog.FREQUENCY }
            )
        }

        item { GroupHeader(stringResource(R.string.settings_group_language)) }
        item {
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

        item { GroupHeader(stringResource(R.string.settings_group_about)) }
        item {
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_version)) },
                supportingContent = { Text(BuildConfig.VERSION_NAME) }
            )
        }
        item {
            ValueRow(
                label = stringResource(R.string.settings_data_source),
                value = stringResource(R.string.settings_data_source_note),
                onClick = { openUrl(context, "https://open-meteo.com") }
            )
        }
        item {
            ValueRow(
                label = stringResource(R.string.settings_source_code),
                value = stringResource(R.string.settings_source_code_note),
                onClick = { openUrl(context, "https://github.com/fiorenzobrioni/chiaro") }
            )
        }
        item {
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_privacy)) },
                supportingContent = { Text(stringResource(R.string.settings_privacy_note)) }
            )
        }

        item { HorizontalDivider(Modifier.padding(top = 8.dp)) }
        item {
            // Destructive styling, then a dialog that says exactly what resets and
            // what does not (VISION §5.7).
            ListItem(
                headlineContent = {
                    Text(
                        text = stringResource(R.string.settings_reset),
                        color = MaterialTheme.colorScheme.error
                    )
                },
                colors = ListItemDefaults.colors(),
                modifier = Modifier.clickable { dialog = SettingsDialog.RESET }
            )
        }
    }

    when (dialog) {
        SettingsDialog.TEMPERATURE -> RadioDialog(
            title = stringResource(R.string.settings_temperature),
            options = TemperatureUnit.entries.map { it to temperatureLabel(it) },
            selected = settings.units.temperature,
            onSelect = { viewModel.setTemperatureUnit(it); dialog = null },
            onDismiss = { dialog = null }
        )
        SettingsDialog.WIND -> RadioDialog(
            title = stringResource(R.string.settings_wind),
            options = WindSpeedUnit.entries.map { it to windLabel(it) },
            selected = settings.units.windSpeed,
            onSelect = { viewModel.setWindSpeedUnit(it); dialog = null },
            onDismiss = { dialog = null }
        )
        SettingsDialog.THEME -> RadioDialog(
            title = stringResource(R.string.settings_theme),
            options = ThemeMode.entries.map { it to themeLabel(it) },
            selected = settings.themeMode,
            onSelect = { viewModel.setThemeMode(it); dialog = null },
            onDismiss = { dialog = null }
        )
        SettingsDialog.ICONS -> RadioDialog(
            title = stringResource(R.string.settings_weather_icons),
            explanation = stringResource(R.string.settings_weather_icons_note),
            options = WeatherIcons.entries.map { it to iconStyleLabel(it) },
            selected = settings.weatherIcons,
            onSelect = { viewModel.setWeatherIcons(it); dialog = null },
            onDismiss = { dialog = null }
        )
        SettingsDialog.FREQUENCY -> RadioDialog(
            title = stringResource(R.string.settings_update_frequency),
            explanation = stringResource(R.string.settings_update_frequency_note),
            options = UpdateFrequencies.map { it to frequencyLabel(it) },
            selected = settings.updateFrequencyMin,
            onSelect = { viewModel.setUpdateFrequency(it); dialog = null },
            onDismiss = { dialog = null }
        )
        SettingsDialog.RESET -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text(stringResource(R.string.settings_reset_dialog_title)) },
            text = { Text(stringResource(R.string.settings_reset_dialog_body)) },
            confirmButton = {
                TextButton(onClick = { viewModel.resetToDefaults(); dialog = null }) {
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

private enum class SettingsDialog { TEMPERATURE, WIND, THEME, ICONS, FREQUENCY, RESET }

@Composable
private fun GroupHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 4.dp)
    )
}

/** A preference row: what it is, what it currently says, tap to change. */
@Composable
private fun ValueRow(label: String, value: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(label) },
        supportingContent = { Text(value) },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

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
private fun iconStyleLabel(style: WeatherIcons): String = when (style) {
    WeatherIcons.FILL -> stringResource(R.string.settings_icons_fill)
    WeatherIcons.LINE -> stringResource(R.string.settings_icons_line)
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
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
}

private fun openUrl(context: android.content.Context, url: String) {
    // A phone with no browser answers with nothing happening, which is better than
    // a crash and honest enough for a link that also states its destination.
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
}
