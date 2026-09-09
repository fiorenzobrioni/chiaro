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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.Refresh
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.data.FetchFailureReason
import com.callbackdev.chiaro.domain.settings.UnitSettings
import com.callbackdev.chiaro.domain.warnings.WarningLevel
import com.callbackdev.chiaro.ui.format.Formats
import com.callbackdev.chiaro.ui.warnings.WarningText
import com.callbackdev.chiaro.ui.icons.ChiaroIcons
import com.callbackdev.chiaro.ui.places.PlacesSheet
import com.callbackdev.chiaro.ui.places.PlacesViewModel
import com.callbackdev.chiaro.ui.sky.SkyText
import com.callbackdev.chiaro.ui.theme.SectionBottom
import com.callbackdev.chiaro.ui.theme.SectionTop
import com.callbackdev.chiaro.ui.theme.ChiaroColors
import com.callbackdev.chiaro.ui.theme.ChiaroTheme
import com.callbackdev.chiaro.ui.theme.forText
import com.callbackdev.chiaro.ui.theme.tabular
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
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
            // One card for the whole drift (review, 8 set 2026): the chips, the strip,
            // the frost line and the sentence were five things stacked on the page and
            // read as five; on the same `surfaceContainer` as the details tiles they
            // read as one object with one job.
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        DriftStrip(
                            drift = content.drift,
                            metric = metric,
                            zone = content.zone,
                            units = units,
                            locale = locale,
                            timeFmt = timeFmt,
                            onOpenTable = { tableOpen = true }
                        )
                        FrostLine(content.drift, units, locale)
                        DriftSentence(content.drift, metric, units, locale)
                    }
                }
            }
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
            item { JournalSectionTitle(dayTitle(day.date, content.zone, dayFmt)) }
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
            zone = content.zone,
            locale = locale,
            timeFmt = timeFmt,
            onDismiss = { tableOpen = false }
        )
    }
}

@Composable
private fun JournalSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(start = 16.dp, top = SectionTop, bottom = SectionBottom)
    )
}

/** [zone] is the PLACE's, like the grouping above it: reading Tokyo's journal from
 * Italy, "today" is Tokyo's today or the two headings disagree by a day. */
@Composable
private fun dayTitle(date: LocalDate, zone: ZoneId, dayFmt: DateTimeFormatter): String {
    val today = LocalDate.now(zone)
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
    // The hour trails the row as a label rather than ending the sentence with "· at
    // 21:54" (review, 8 set 2026): a log is scanned by the hour, and a column of hours
    // down the right edge is what makes it scannable. The outcome line has no hour —
    // it is about the day.
    val time = entry.at.atZone(zone).format(timeFmt)
    when (entry) {
        is JournalEntry.ForecastShift -> {
            val details = JournalText.shiftDetails(entry.shifts, units, locale)
            // How many updates the folded line stands for, when more than one.
            val count = if (entry.revisions > 1) {
                pluralStringResource(
                    R.plurals.journal_shift_revisions, entry.revisions, entry.revisions
                )
            } else {
                null
            }
            EntryItem(
                icon = Icons.Outlined.Edit,
                headline = JournalText.shiftHeadline(entry, locale),
                // A revision this screen has no words for keeps its row — the Journal
                // is the log, and "something moved at 21:54" is still the truth.
                supporting = listOfNotNull(details.takeIf { it.isNotBlank() }, count)
                    .joinToString(" · ")
                    .ifBlank { null },
                time = time
            )
        }
        is JournalEntry.RuleFired -> EntryItem(
            icon = Icons.Outlined.Notifications,
            headline = stringResource(R.string.journal_rule_fired, entry.name),
            supporting = null,
            time = time
        )
        is JournalEntry.SkyObserved -> EntryItem(
            icon = Icons.Outlined.Star,
            headline = stringResource(SkyText.nameRes(entry.jobId)),
            supporting = when {
                entry.verdict != null -> buildString {
                    append(stringResource(SkyText.verdictWordRes(entry.verdict)))
                    entry.cloudPct?.let {
                        append(", ")
                        append(stringResource(R.string.sky_evidence_cloud).format(it))
                    }
                }
                else -> stringResource(R.string.journal_sky_skipped)
            },
            time = time
        )
        // The loop closed: what the app said, against what it then saw. The check is
        // "this day has been checked", not a verdict — green and red would be a
        // judgement on the weather, and the outcome is a fact (DESIGN §8.10).
        is JournalEntry.DayOutcome -> EntryItem(
            icon = Icons.Outlined.Check,
            headline = stringResource(
                if (entry.rained) R.string.journal_outcome_rained
                else R.string.journal_outcome_dry,
                Formats.percent(entry.forecastPrecipPct, locale)
            ),
            supporting = buildList {
                val forecast = entry.forecastHighC
                val observed = entry.observedHighC
                if (forecast != null && observed != null) {
                    add(
                        stringResource(
                            R.string.journal_outcome_high,
                            Formats.temperature(forecast, units.temperature, locale),
                            Formats.temperature(observed, units.temperature, locale)
                        )
                    )
                }
                // What the claim rests on, stated like a sky run states its distance:
                // a verdict that hides its coverage is a verdict you cannot weigh.
                add(
                    pluralStringResource(
                        R.plurals.journal_outcome_coverage,
                        entry.coveredHours,
                        entry.coveredHours
                    )
                )
            }.joinToString(" · "),
            time = null
        )
        // The authority's own line (Fase 11). The Material warning triangle is this
        // category's — an authority grading a day is the warning in this diary — which
        // is why the two "an update did not arrive" lines below moved to Refresh: the
        // glyph names the category, so two categories cannot share one.
        is JournalEntry.WarningChanged -> EntryItem(
            icon = Icons.Outlined.Warning,
            headline = if (entry.to == WarningLevel.NONE) {
                stringResource(R.string.journal_warning_cleared, warningDay(entry.day, zone))
            } else {
                stringResource(
                    R.string.journal_warning_raised,
                    stringResource(WarningText.phraseRes(entry.to)),
                    stringResource(WarningText.hazardRes(entry.hazard)),
                    warningDay(entry.day, zone)
                )
            },
            supporting = entry.issuedAt?.let {
                stringResource(R.string.journal_warning_source, it.toLocalTime().format(timeFmt))
            },
            time = time
        )
        is JournalEntry.BulletinMissed -> EntryItem(
            icon = Icons.Outlined.Refresh,
            headline = stringResource(R.string.journal_warning_missed),
            supporting = entry.heldFrom?.let {
                stringResource(
                    R.string.journal_warning_missed_held,
                    it.toLocalDate().format(
                        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
                    )
                )
            } ?: stringResource(R.string.journal_warning_missed_empty),
            time = time
        )
        is JournalEntry.FetchFailed -> EntryItem(
            icon = Icons.Outlined.Refresh,
            headline = stringResource(R.string.journal_fetch_failed),
            supporting = stringResource(
                when (entry.reason) {
                    FetchFailureReason.OFFLINE -> R.string.error_offline
                    FetchFailureReason.SERVICE -> R.string.error_service
                    FetchFailureReason.UNKNOWN -> R.string.error_unknown
                }
            ),
            time = time
        )
    }
}

/**
 * The day a level concerns, in the words the rest of the diary uses. [zone] is the
 * PLACE's, like every other date on this screen.
 */
@Composable
private fun warningDay(day: LocalDate, zone: ZoneId): String {
    val today = LocalDate.now(zone)
    return when (day) {
        today -> stringResource(R.string.warning_day_today_short)
        today.plusDays(1) -> stringResource(R.string.warning_day_tomorrow_short)
        else -> day.format(
            DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault())
        )
    }
}

/**
 * One line of the log. The glyph names the CATEGORY of the line — a revision, a sky
 * moment observed, an alert fired, a day checked, an official warning, an update that
 * did not arrive — and is a Material silhouette in `onSurfaceVariant` for all of them
 * (review, 8 set 2026): two used to be Meteocons tinted flat, the same silhouettes
 * taken off the Sky screen that day, and here they were not weather but categories, so
 * §13.1 does not reach them and a monochrome set is the consistent one.
 *
 * The triangle moved on 9 set 2026, when the official warnings arrived (Fase 11): it is
 * the mark of a warning everywhere else in the app, so it belongs to the line an
 * authority wrote. A fetch that failed and a bulletin that was not reached are one
 * category — something the app went for and did not get — and share `Refresh`.
 */
@Composable
private fun EntryItem(icon: ImageVector, headline: String, supporting: String?, time: String?) {
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
        supportingContent = supporting?.let { { Text(it) } },
        trailingContent = time?.let {
            {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall.tabular(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

// ---------------------------------------------------------------------------------
// The drift strip (DESIGN §8.10)
// ---------------------------------------------------------------------------------

/**
 * One row per target day, one column per **slot of six hours**, color on the metric's
 * OWN ramp — whether Saturday got "better" is a judgement, and the judgement lives in
 * the sentence underneath, never in the color. A slot no fetch landed in, and a cell
 * whose fetch did not cover that day, are both drawn as absence (hairline outline),
 * never as a zero. A tap, or a long press, opens the numbers.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DriftStrip(
    drift: DriftModel,
    metric: DriftMetric,
    zone: ZoneId,
    units: UnitSettings,
    locale: Locale,
    timeFmt: DateTimeFormatter,
    onOpenTable: () -> Unit
) {
    val dayFmt = remember(locale) { DateTimeFormatter.ofPattern("EEE d", locale) }
    val tableHint = stringResource(R.string.journal_drift_desc)
    val window = pluralStringResource(
        R.plurals.journal_drift_columns,
        drift.columnHours.toInt(),
        drift.columnHours.toInt(),
        drift.oldest.atZone(zone).format(dayFmt) + " " + drift.oldest.atZone(zone).format(timeFmt),
        drift.newest.atZone(zone).format(dayFmt) + " " + drift.newest.atZone(zone).format(timeFmt)
    )
    val metricName = stringResource(
        when (metric) {
            DriftMetric.RAIN -> R.string.journal_metric_rain
            DriftMetric.HIGH -> R.string.journal_metric_high
        }
    )
    // One announcement for the whole strip: the cells carry no text, so without this
    // TalkBack would read seven dates and nothing about what is drawn beside them.
    val stripDescription = stringResource(R.string.journal_drift_a11y, metricName, window)
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier
            .fillMaxWidth()
            // Both gestures open the table. An empty onClick left a ripple that did
            // nothing and, worse, gave TalkBack a "double tap to activate" for an
            // action that was not there. The margins are the card's now.
            .combinedClickable(onClick = onOpenTable, onLongClick = onOpenTable)
            .semantics(mergeDescendants = true) { contentDescription = stripDescription }
    ) {
        // The mark's slot is added to every row or to none, so the cells stay in one
        // grid; a week with nothing freezing in it draws exactly the strip it drew
        // before this existed.
        val marked = drift.frostC.any { it != null }
        val labelWidth = (if (marked) 68.dp else 52.dp).forText()
        drift.dates.forEachIndexed { row, date ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    // §10: «Sab 13» in the reader's type, not in 52 fixed dp.
                    modifier = Modifier.width(labelWidth)
                ) {
                    Text(
                        text = date.format(dayFmt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (drift.frostC[row] != null) {
                        Icon(
                            imageVector = ChiaroIcons.frost,
                            // The strip speaks once, and the clause under it names
                            // these days in words: a glyph is never the only carrier.
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
                drift.columns.indices.forEach { col ->
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
            text = "$window · $tableHint",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

/**
 * Always present (DESIGN §8.10): sampled swatches with the scale's REAL ends.
 *
 * The temperature legend used to print −10 °C and 40 °C under swatches drawn from a
 * ramp that clamps at −5 and 35 — measured ΔE 0.00 between −10 and −5, and between 40
 * and 35, so the two end swatches were duplicates and the two numbers under them were
 * ends the scale does not have. The anchors are the only honest labels, and the
 * samples are spaced evenly along the ramp so the ramp looks like the ramp.
 */
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
                Formats.percent(0, locale),
                Formats.percent(100, locale)
            )
            DriftMetric.HIGH -> {
                val span = ChiaroColors.ANCHOR_HIGH - ChiaroColors.ANCHOR_LOW
                Triple(
                    (0..6).map {
                        ChiaroTheme.colors.temperatureAt(
                            ChiaroColors.ANCHOR_LOW + span * it / 6.0
                        )
                    },
                    Formats.temperature(ChiaroColors.ANCHOR_LOW, units.temperature, locale),
                    Formats.temperature(ChiaroColors.ANCHOR_HIGH, units.temperature, locale)
                )
            }
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

/**
 * The days the strip marked, in words and with their numbers — the legend for the
 * glyph and the information itself in one line (DESIGN §10: never a mark alone).
 *
 * It sits OUTSIDE the strip's merged description on purpose, so a screen reader
 * reaches it as its own sentence instead of it being folded into a paragraph about
 * columns. Absent entirely when nothing is freezing: a section with no data is not
 * drawn (§1.1), and a "no frost" line every week is the filler this screen refuses.
 */
@Composable
private fun FrostLine(drift: DriftModel, units: UnitSettings, locale: Locale) {
    val dayFmt = remember(locale) { DateTimeFormatter.ofPattern("EEEE d", locale) }
    val days = drift.dates.indices.mapNotNull { row ->
        drift.frostC[row]?.let { low ->
            stringResource(
                R.string.journal_drift_frost_day,
                drift.dates[row].format(dayFmt),
                Formats.temperature(low, units.temperature, locale)
            )
        }
    }
    if (days.isEmpty()) return
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = ChiaroIcons.frost,
            contentDescription = null, // the sentence beside it says it
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = stringResource(R.string.journal_drift_frost, days.joinToString(", ")),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/**
 * The judgement in words, beside the strip — never in its colors (DESIGN §8.10).
 *
 * Three states, not two. "Quiet" used to cover both the week that did not move and the
 * week that moved and came back, so the strip could print "the week held steady" over a
 * list of entries recording two revisions of the same day. A swing that returns is a
 * fact about the week, and saying it is cheaper than being contradicted by the prose
 * underneath.
 */
@Composable
private fun DriftSentence(
    drift: DriftModel,
    metric: DriftMetric,
    units: UnitSettings,
    locale: Locale
) {
    val dayFmt = remember(locale) { DateTimeFormatter.ofPattern("EEEE d", locale) }
    /** A day at the head of a sentence takes a capital; in Italian the weekday is
     * lowercase on its own, so the format alone is not enough. */
    fun day(date: LocalDate): String = date.format(dayFmt)
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }

    val sentence = when (metric) {
        DriftMetric.RAIN -> {
            val moves = drift.dates.indices.mapNotNull { row -> move(drift.dates[row], drift.rain[row]) }
            val net = moves.filter { kotlin.math.abs(it.last - it.first) >= RAIN_MOVE_PCT }
                .maxByOrNull { kotlin.math.abs(it.last - it.first) }
            val swing = moves.filter { it.high - it.low >= RAIN_MOVE_PCT }
                .maxByOrNull { it.high - it.low }
            when {
                net != null && net.last < net.first -> stringResource(
                    R.string.journal_drift_rain_better, day(net.date),
                    Formats.percent(net.first, locale), Formats.percent(net.last, locale)
                )
                net != null -> stringResource(
                    R.string.journal_drift_rain_worse, day(net.date),
                    Formats.percent(net.first, locale), Formats.percent(net.last, locale)
                )
                swing != null -> stringResource(
                    R.string.journal_drift_rain_swing, day(swing.date),
                    Formats.percent(swing.low, locale), Formats.percent(swing.high, locale)
                )
                else -> stringResource(R.string.journal_drift_quiet)
            }
        }
        DriftMetric.HIGH -> {
            fun temp(value: Double) = Formats.temperature(value, units.temperature, locale)
            val moves = drift.dates.indices.mapNotNull { row -> move(drift.dates[row], drift.highC[row]) }
            val net = moves.filter { kotlin.math.abs(it.last - it.first) >= HIGH_MOVE_C }
                .maxByOrNull { kotlin.math.abs(it.last - it.first) }
            val swing = moves.filter { it.high - it.low >= HIGH_MOVE_C }
                .maxByOrNull { it.high - it.low }
            when {
                net != null -> stringResource(
                    R.string.journal_drift_high_moved,
                    net.date.format(dayFmt), temp(net.first), temp(net.last)
                )
                swing != null -> stringResource(
                    R.string.journal_drift_high_swing,
                    day(swing.date), temp(swing.low), temp(swing.high)
                )
                else -> stringResource(R.string.journal_drift_quiet)
            }
        }
    }
    Text(text = sentence, style = MaterialTheme.typography.bodyMedium)
}

/** What one row of the strip did: where it started, where it ended, and how far it
 * wandered in between. Null when the row has fewer than two predictions to compare. */
private data class DriftMove<T : Comparable<T>>(
    val date: LocalDate,
    val first: T,
    val last: T,
    val low: T,
    val high: T
)

private fun <T : Comparable<T>> move(date: LocalDate, cells: List<T?>): DriftMove<T>? {
    val present = cells.filterNotNull()
    if (present.size < 2) return null
    return DriftMove(date, present.first(), present.last(), present.min(), present.max())
}

/** The strip's own thresholds are the diff engine's, so the sentence can never claim
 * a move the entries below it never recorded. */
private const val RAIN_MOVE_PCT = 10
private const val HIGH_MOVE_C = 1.0

/**
 * The numbers behind the colors (DESIGN §9.3): a picture of a number is not a number.
 *
 * A grid, not a line of arrows (review, 8 set 2026): fourteen values joined by "→"
 * wrapped three times inside a dialog and lost the one thing a table has, the column.
 * One row per target day, one column per slot with the slot's own hour over it — the
 * fetch's when there was one, the slot's nominal hour when the slot is empty — in
 * tabular figures, scrolling sideways when the strip is wider than the dialog. Absence
 * stays a dash, never a zero.
 */
@Composable
private fun DriftTableDialog(
    drift: DriftModel,
    metric: DriftMetric,
    units: UnitSettings,
    zone: ZoneId,
    locale: Locale,
    timeFmt: DateTimeFormatter,
    onDismiss: () -> Unit
) {
    val dayFmt = remember(locale) { DateTimeFormatter.ofPattern("EEE d", locale) }
    val slotDayFmt = remember(locale) { DateTimeFormatter.ofPattern("EEE", locale) }
    val cellStyle = MaterialTheme.typography.labelMedium.tabular()
    val quiet = MaterialTheme.colorScheme.onSurfaceVariant
    val labelWidth = 56.dp.forText()
    val cellWidth = 52.dp.forText()
    // Each column's moment: the fetch it holds, or where the empty slot would have
    // fallen — the strip's axis is time, so an empty column still has an hour.
    val slotTimes = drift.columns.indices.map { col ->
        drift.columns[col]
            ?: drift.newest.minusSeconds((drift.columns.lastIndex - col) * drift.columnHours * 3600)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.journal_drift_table_title)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .horizontalScroll(rememberScrollState())
            ) {
                // Two header lines: the slot's day, then its hour.
                Row {
                    Box(modifier = Modifier.width(labelWidth))
                    slotTimes.forEach { at ->
                        Text(
                            text = at.atZone(zone).format(slotDayFmt),
                            style = cellStyle,
                            color = quiet,
                            modifier = Modifier.width(cellWidth)
                        )
                    }
                }
                Row {
                    Box(modifier = Modifier.width(labelWidth))
                    slotTimes.forEach { at ->
                        Text(
                            text = at.atZone(zone).format(timeFmt),
                            style = cellStyle,
                            color = quiet,
                            modifier = Modifier.width(cellWidth)
                        )
                    }
                }
                drift.dates.forEachIndexed { row, date ->
                    val values = when (metric) {
                        DriftMetric.RAIN -> drift.rain[row].map { cell ->
                            cell?.let { Formats.percent(it, locale) } ?: JournalText.MISSING
                        }
                        DriftMetric.HIGH -> drift.highC[row].map { cell ->
                            cell?.let { Formats.temperature(it, units.temperature, locale) }
                                ?: JournalText.MISSING
                        }
                    }
                    Row {
                        Text(
                            text = date.format(dayFmt),
                            style = cellStyle,
                            color = quiet,
                            modifier = Modifier.width(labelWidth)
                        )
                        values.forEach { value ->
                            Text(text = value, style = cellStyle, modifier = Modifier.width(cellWidth))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_done)) }
        }
    )
}
