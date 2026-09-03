package com.callbackdev.chiaro.ui.journal

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.data.FetchFailureReason
import com.callbackdev.chiaro.domain.settings.UnitSettings
import com.callbackdev.chiaro.ui.format.Formats
import com.callbackdev.chiaro.ui.icons.ChiaroIcons
import com.callbackdev.chiaro.ui.places.PlacesSheet
import com.callbackdev.chiaro.ui.places.PlacesViewModel
import com.callbackdev.chiaro.ui.sky.SkyText
import com.callbackdev.chiaro.ui.theme.ChiaroTheme
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The Journal (VISION §5.5): the history table read as prose, newest first, grouped
 * by day — what changed, what fired, what the sky was seen to do, and what could not
 * be fetched. Above it, the drift strip: how the week ahead has been changing.
 */
@Composable
fun JournalRoute(
    onOpenSettings: () -> Unit,
    journalViewModel: JournalViewModel = viewModel(factory = JournalViewModel.Factory),
    placesViewModel: PlacesViewModel = viewModel(factory = PlacesViewModel.Factory)
) {
    val state by journalViewModel.state.collectAsStateWithLifecycle()
    val units by journalViewModel.units.collectAsStateWithLifecycle()
    var placesOpen by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            JournalHeader(
                placeName = (state as? JournalUiState.Ready)?.content?.placeName,
                onOpenPlaces = { placesOpen = true },
                onOpenSettings = onOpenSettings
            )
            when (val s = state) {
                JournalUiState.Starting -> Unit
                JournalUiState.NoPlace -> NoPlaceForJournal(onOpenPlaces = { placesOpen = true })
                is JournalUiState.Ready -> JournalBody(s.content, units)
            }
        }
    }
    if (placesOpen) {
        PlacesSheet(viewModel = placesViewModel, onDismiss = { placesOpen = false })
    }
}

@Composable
private fun JournalHeader(
    placeName: String?,
    onOpenPlaces: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.tab_journal), style = MaterialTheme.typography.titleLarge)
            if (placeName != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(onClick = onOpenPlaces)
                ) {
                    Text(
                        text = placeName,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Icon(
                        imageVector = Icons.Outlined.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.place_switcher_action),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
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

@Composable
private fun NoPlaceForJournal(onOpenPlaces: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(stringResource(R.string.empty_no_place_title), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.journal_no_place_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        TextButton(onClick = onOpenPlaces) { Text(stringResource(R.string.empty_no_place_action)) }
    }
}

// ---------------------------------------------------------------------------------
// Body
// ---------------------------------------------------------------------------------

private enum class DriftMetric { RAIN, HIGH }

@Composable
private fun JournalBody(content: JournalContent, units: UnitSettings) {
    val locale = Locale.getDefault()
    val is24h = android.text.format.DateFormat.is24HourFormat(LocalContext.current)
    val timeFmt = remember(locale, is24h) { Formats.timeFormatter(is24h, locale) }
    val dayFmt = remember(locale) { DateTimeFormatter.ofPattern("EEEE d MMMM", locale) }
    var metric by rememberSaveable { mutableStateOf(DriftMetric.RAIN) }
    var tableOpen by remember { mutableStateOf(false) }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        if (content.drift != null) {
            item { JournalSectionTitle(stringResource(R.string.journal_drift_title)) }
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    FilterChip(
                        selected = metric == DriftMetric.RAIN,
                        onClick = { metric = DriftMetric.RAIN },
                        label = { Text(stringResource(R.string.journal_metric_rain)) }
                    )
                    FilterChip(
                        selected = metric == DriftMetric.HIGH,
                        onClick = { metric = DriftMetric.HIGH },
                        label = { Text(stringResource(R.string.journal_metric_high)) }
                    )
                }
            }
            item {
                DriftStrip(
                    drift = content.drift,
                    metric = metric,
                    zone = content.zone,
                    units = units,
                    locale = locale,
                    onLongPress = { tableOpen = true }
                )
            }
            item { DriftSentence(content.drift, metric, units, locale) }
        } else if (content.days.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.journal_drift_waiting),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        if (content.days.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.journal_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(32.dp)
                )
            }
        }

        content.days.forEach { day ->
            item { JournalSectionTitle(dayTitle(day.date, dayFmt)) }
            items(day.entries.size) { index ->
                EntryRow(day.entries[index], content.zone, timeFmt, units, locale)
            }
        }
    }

    if (tableOpen && content.drift != null) {
        DriftTableDialog(
            drift = content.drift,
            metric = metric,
            units = units,
            locale = locale,
            onDismiss = { tableOpen = false }
        )
    }
}

@Composable
private fun JournalSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp)
    )
}

@Composable
private fun dayTitle(date: LocalDate, dayFmt: DateTimeFormatter): String {
    val today = LocalDate.now()
    return when (date) {
        today -> stringResource(R.string.week_today)
        today.minusDays(1) -> stringResource(R.string.journal_yesterday)
        else -> date.format(dayFmt)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }
}

// ---------------------------------------------------------------------------------
// Entries as prose
// ---------------------------------------------------------------------------------

@Composable
private fun EntryRow(
    entry: JournalEntry,
    zone: ZoneId,
    timeFmt: DateTimeFormatter,
    units: UnitSettings,
    locale: Locale
) {
    val time = entry.at.atZone(zone).format(timeFmt)
    when (entry) {
        is JournalEntry.ForecastShift -> EntryItem(
            icon = ChiaroIcons.cloud,
            headline = JournalText.shiftHeadline(entry, locale),
            supporting = JournalText.shiftDetails(entry.shifts, units, locale) +
                " · " + stringResource(R.string.journal_at_time, time)
        )
        is JournalEntry.RuleFired -> EntryItem(
            icon = Icons.Outlined.Notifications,
            headline = stringResource(R.string.journal_rule_fired, entry.name),
            supporting = stringResource(R.string.journal_at_time, time)
        )
        is JournalEntry.SkyObserved -> EntryItem(
            icon = ChiaroIcons.star,
            headline = stringResource(SkyText.nameRes(entry.jobId)),
            supporting = when {
                entry.verdict != null -> buildString {
                    append(stringResource(SkyText.verdictWordRes(entry.verdict)))
                    entry.cloudPct?.let {
                        append(", ")
                        append(stringResource(R.string.sky_evidence_cloud).format(it))
                    }
                    append(" · ")
                    append(stringResource(R.string.journal_at_time, time))
                }
                else -> stringResource(R.string.journal_sky_skipped) +
                    " · " + stringResource(R.string.journal_at_time, time)
            }
        )
        is JournalEntry.FetchFailed -> EntryItem(
            icon = Icons.Outlined.Warning,
            headline = stringResource(R.string.journal_fetch_failed),
            supporting = stringResource(
                when (entry.reason) {
                    FetchFailureReason.OFFLINE -> R.string.error_offline
                    FetchFailureReason.SERVICE -> R.string.error_service
                    FetchFailureReason.UNKNOWN -> R.string.error_unknown
                }
            ) + " · " + stringResource(R.string.journal_at_time, time)
        )
    }
}

@Composable
private fun EntryItem(icon: ImageVector, headline: String, supporting: String) {
    ListItem(
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        },
        headlineContent = { Text(headline) },
        supportingContent = { Text(supporting) }
    )
}

// ---------------------------------------------------------------------------------
// The drift strip (DESIGN §8.10)
// ---------------------------------------------------------------------------------

/**
 * One row per target day, one column per fetch, color on the metric's OWN ramp —
 * whether Saturday got "better" is a judgement, and the judgement lives in the
 * sentence underneath, never in the color. Cells the fetch did not cover are drawn
 * as absence (hairline outline), not as a zero. Long press opens the numbers.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DriftStrip(
    drift: DriftModel,
    metric: DriftMetric,
    zone: ZoneId,
    units: UnitSettings,
    locale: Locale,
    onLongPress: () -> Unit
) {
    val dayFmt = remember(locale) { DateTimeFormatter.ofPattern("EEE d", locale) }
    val tableHint = stringResource(R.string.journal_drift_desc)
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .combinedClickable(onClick = {}, onLongClick = onLongPress)
            .semantics { contentDescription = tableHint }
    ) {
        drift.dates.forEachIndexed { row, date ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = date.format(dayFmt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(52.dp)
                )
                drift.fetches.indices.forEach { col ->
                    val color = when (metric) {
                        DriftMetric.RAIN -> drift.rain[row][col]
                            ?.let { ChiaroTheme.colors.rainAt(it) }
                        DriftMetric.HIGH -> drift.highC[row][col]
                            ?.let { ChiaroTheme.colors.temperatureAt(it) }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(16.dp)
                            .padding(1.dp)
                            .let { base ->
                                if (color != null) {
                                    base.background(color)
                                } else {
                                    base.border(
                                        width = 0.5.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant
                                    )
                                }
                            }
                    )
                }
            }
        }
        DriftLegend(metric, units, locale)
        Text(
            text = stringResource(
                R.string.journal_drift_columns,
                drift.fetches.size,
                drift.fetches.first().atZone(zone).format(dayFmt),
                drift.fetches.last().atZone(zone).format(dayFmt)
            ) + " · " + tableHint,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

/** Always present (DESIGN §8.10): sampled swatches with the scale's real ends. */
@Composable
private fun DriftLegend(metric: DriftMetric, units: UnitSettings, locale: Locale) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(top = 6.dp)
    ) {
        val (samples, low, high) = when (metric) {
            DriftMetric.RAIN -> Triple(
                (0..100 step 20).map { ChiaroTheme.colors.rainAt(it) },
                "0%", "100%"
            )
            DriftMetric.HIGH -> Triple(
                listOf(-10.0, 0.0, 10.0, 15.0, 20.0, 30.0, 40.0)
                    .map { ChiaroTheme.colors.temperatureAt(it) },
                Formats.temperature(-10.0, units.temperature, locale),
                Formats.temperature(40.0, units.temperature, locale)
            )
        }
        Text(low, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        samples.forEach { color ->
            Box(
                modifier = Modifier
                    .size(width = 16.dp, height = 8.dp)
                    .background(color)
            )
        }
        Text(high, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** The judgement in words, beside the strip — never in its colors (DESIGN §8.10). */
@Composable
private fun DriftSentence(
    drift: DriftModel,
    metric: DriftMetric,
    units: UnitSettings,
    locale: Locale
) {
    val dayFmt = remember(locale) { DateTimeFormatter.ofPattern("EEEE d", locale) }
    val sentence = when (metric) {
        DriftMetric.RAIN -> {
            val moved = drift.dates.indices.mapNotNull { row ->
                val cells = drift.rain[row].filterNotNull()
                if (cells.size < 2) return@mapNotNull null
                Triple(drift.dates[row], cells.first(), cells.last())
            }.maxByOrNull { (_, first, last) -> kotlin.math.abs(last - first) }
            when {
                moved == null || kotlin.math.abs(moved.third - moved.second) < 10 ->
                    stringResource(R.string.journal_drift_quiet)
                moved.third < moved.second -> stringResource(
                    R.string.journal_drift_rain_better,
                    moved.first.format(dayFmt), moved.second, moved.third
                )
                else -> stringResource(
                    R.string.journal_drift_rain_worse,
                    moved.first.format(dayFmt), moved.second, moved.third
                )
            }
        }
        DriftMetric.HIGH -> {
            val moved = drift.dates.indices.mapNotNull { row ->
                val cells = drift.highC[row].filterNotNull()
                if (cells.size < 2) return@mapNotNull null
                Triple(drift.dates[row], cells.first(), cells.last())
            }.maxByOrNull { (_, first, last) -> kotlin.math.abs(last - first) }
            if (moved == null || kotlin.math.abs(moved.third - moved.second) < 1.0) {
                stringResource(R.string.journal_drift_quiet)
            } else {
                stringResource(
                    R.string.journal_drift_high_moved,
                    moved.first.format(dayFmt),
                    Formats.temperature(moved.second, units.temperature, locale),
                    Formats.temperature(moved.third, units.temperature, locale)
                )
            }
        }
    }
    Text(
        text = sentence,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    )
}

/** The numbers behind the colors (DESIGN §9.3): a picture of a number is not a
 * number. One line per target day, values in fetch order, absence as a dot. */
@Composable
private fun DriftTableDialog(
    drift: DriftModel,
    metric: DriftMetric,
    units: UnitSettings,
    locale: Locale,
    onDismiss: () -> Unit
) {
    val dayFmt = remember(locale) { DateTimeFormatter.ofPattern("EEE d", locale) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.journal_drift_table_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                drift.dates.forEachIndexed { row, date ->
                    val values = when (metric) {
                        DriftMetric.RAIN -> drift.rain[row].map { cell ->
                            cell?.let { "$it%" } ?: "·"
                        }
                        DriftMetric.HIGH -> drift.highC[row].map { cell ->
                            cell?.let { Formats.temperature(it, units.temperature, locale) } ?: "·"
                        }
                    }
                    Text(
                        text = date.format(dayFmt) + "  " + values.joinToString(" → "),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_done)) }
        }
    )
}
