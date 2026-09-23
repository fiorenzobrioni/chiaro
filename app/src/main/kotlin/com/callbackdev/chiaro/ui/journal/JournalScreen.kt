package com.callbackdev.chiaro.ui.journal

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.callbackdev.chiaro.ui.components.VerdictChip
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
import com.callbackdev.chiaro.ui.format.currentLocale
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
import kotlin.math.roundToInt

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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun JournalBody(content: JournalContent, units: UnitSettings) {
    val locale = currentLocale()
    val is24h = android.text.format.DateFormat.is24HourFormat(LocalContext.current)
    val timeFmt = remember(locale, is24h) { Formats.timeFormatter(is24h, locale) }
    val dayFmt = remember(locale) { DateTimeFormatter.ofPattern("EEEE d MMMM", locale) }
    var metric by rememberSaveable { mutableStateOf(DriftMetric.RAIN) }
    var tableOpen by remember { mutableStateOf(false) }

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
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
                    // The sentence FIRST since the design review of 23 set 2026: it is the
                    // answer the card exists for ("is the weekend getting better?"), and at
                    // the foot of the card it read as a caption to a chart nobody had yet
                    // learned to read. The strip under it then shows the row it names.
                    val headline = driftHeadline(content.drift, metric, units, locale)
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(text = headline.text, style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = metric == DriftMetric.RAIN,
                                onClick = { metric = DriftMetric.RAIN },
                                label = { Text(stringResource(R.string.journal_metric_rain)) },
                                leadingIcon = {
                                    Icon(
                                        ChiaroIcons.precipitation, contentDescription = null,
                                        tint = Color.Unspecified, modifier = Modifier.size(18.dp)
                                    )
                                }
                            )
                            FilterChip(
                                selected = metric == DriftMetric.HIGH,
                                onClick = { metric = DriftMetric.HIGH },
                                label = { Text(stringResource(R.string.journal_metric_high)) },
                                leadingIcon = {
                                    Icon(
                                        ChiaroIcons.dewPoint, contentDescription = null,
                                        tint = Color.Unspecified, modifier = Modifier.size(18.dp)
                                    )
                                }
                            )
                        }
                        DriftStrip(
                            drift = content.drift,
                            metric = metric,
                            zone = content.zone,
                            units = units,
                            locale = locale,
                            timeFmt = timeFmt,
                            highlight = headline.date,
                            onOpenTable = { tableOpen = true }
                        )
                        FrostLine(content.drift, units, locale)
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

        // How the forecast did, from the days the log has already closed (design review,
        // 23 set 2026). Only from three days on: two is an anecdote.
        val outcomes = content.days.flatMap { it.entries }
            .filterIsInstance<JournalEntry.DayOutcome>()
            .sortedBy { it.date }
            .takeLast(OutcomeDays)
        if (outcomes.size >= MinOutcomes) {
            item { JournalSectionTitle(stringResource(R.string.journal_skill_title)) }
            item {
                OutcomeCard(
                    outcomes = outcomes,
                    units = units,
                    locale = locale,
                    modifier = Modifier.padding(horizontal = 16.dp)
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

        // Each day's heading stays at the top while its entries scroll under it (design
        // review, 23 set 2026): a log read backwards through several days lost which day
        // it was in as soon as the heading scrolled away.
        content.days.forEach { day ->
            stickyHeader(key = "day-${day.date}") {
                DayHeading(day.date, content.zone, dayFmt, day.entries.size)
            }
            items(day.entries.size) { index ->
                EntryRow(
                    entry = day.entries[index],
                    zone = content.zone,
                    timeFmt = timeFmt,
                    units = units,
                    locale = locale,
                    threadAbove = index > 0,
                    threadBelow = index < day.entries.lastIndex
                )
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

/** A day of the log, pinned while its entries pass under it: «Oggi» or «Ieri» with the
 * full date beside it in the quiet ink (the relative word alone left the reader counting
 * back), or the full date alone further back. */
@Composable
private fun DayHeading(date: LocalDate, zone: ZoneId, dayFmt: DateTimeFormatter, count: Int) {
    val title = dayTitle(date, zone, dayFmt)
    val full = date.format(dayFmt)
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(currentLocale()) else it.toString() }
    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = SectionTop, bottom = SectionBottom)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            if (title != full) {
                Text(
                    text = full,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = pluralStringResource(R.plurals.journal_day_count, count, count),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }
    }
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
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(currentLocale()) else it.toString() }
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
    locale: Locale,
    threadAbove: Boolean,
    threadBelow: Boolean
) {
    // The hour trails the row as a label rather than ending the sentence with "· at
    // 21:54" (review, 8 set 2026): a log is scanned by the hour, and a column of hours
    // down the right edge is what makes it scannable. The outcome line has no hour —
    // it is about the day.
    val time = entry.at.atZone(zone).format(timeFmt)
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val quiet = EntryBadge.Silhouette(Icons.Outlined.Refresh, scheme.surfaceContainerHighest, scheme.onSurfaceVariant)
    @Composable
    fun item(badge: EntryBadge, headline: String, supporting: (@Composable () -> Unit)?, time: String?) =
        EntryItem(badge, headline, supporting, time, threadAbove, threadBelow)
    when (entry) {
        is JournalEntry.ForecastShift -> {
            val details = styledShift(entry.shifts, units, locale)
            // How many updates the folded line stands for, when more than one.
            val count = if (entry.revisions > 1) {
                pluralStringResource(
                    R.plurals.journal_shift_revisions, entry.revisions, entry.revisions
                )
            } else {
                null
            }
            item(
                badge = EntryBadge.Silhouette(Icons.Outlined.Edit, scheme.primaryContainer, scheme.onPrimaryContainer),
                headline = JournalText.shiftHeadline(entry, locale),
                // A revision this screen has no words for keeps its row — the Journal
                // is the log, and "something moved at 21:54" is still the truth.
                supporting = if (details.isEmpty() && count == null) null else {
                    {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            if (details.isNotEmpty()) Text(details, style = MaterialTheme.typography.bodyMedium)
                            count?.let {
                                Text(it, style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
                            }
                        }
                    }
                },
                time = time
            )
        }
        is JournalEntry.RuleFired -> item(
            badge = EntryBadge.Silhouette(Icons.Outlined.Notifications, scheme.secondaryContainer, scheme.onSecondaryContainer),
            headline = stringResource(R.string.journal_rule_fired, entry.name),
            supporting = null,
            time = time
        )
        // The sky moment in its own drawing, and its verdict as the chip the Sky screen
        // prints: the same answer in the same shape on both screens.
        is JournalEntry.SkyObserved -> item(
            badge = EntryBadge.Drawing(ChiaroIcons.skyJob(entry.jobId), scheme.surfaceContainerHigh),
            headline = stringResource(SkyText.nameRes(entry.jobId)),
            supporting = {
                val verdict = entry.verdict
                if (verdict != null) {
                    VerdictChip(
                        kind = SkyText.chipKind(verdict),
                        label = stringResource(SkyText.verdictWordRes(verdict)),
                        evidence = entry.cloudPct?.let { stringResource(R.string.sky_evidence_cloud).format(it) },
                        modifier = Modifier.padding(top = 4.dp)
                    )
                } else {
                    Text(stringResource(R.string.journal_sky_skipped), style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                }
            },
            time = time
        )
        // The loop closed: what the app said, against what it then saw. The drawing is
        // what the day DID — rain or no rain — a fact, not a judgement on the forecast
        // (DESIGN §8.10); the sentence carries the numbers.
        is JournalEntry.DayOutcome -> item(
            badge = EntryBadge.Drawing(
                if (entry.rained) ChiaroIcons.precipitation else ChiaroIcons.condition(0),
                scheme.surfaceContainerHigh
            ),
            headline = stringResource(
                if (entry.rained) R.string.journal_outcome_rained
                else R.string.journal_outcome_dry,
                Formats.percent(entry.forecastPrecipPct, locale)
            ),
            supporting = {
                val line = buildList {
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
                    add(pluralStringResource(R.plurals.journal_outcome_coverage, entry.coveredHours, entry.coveredHours))
                }.joinToString(" · ")
                Text(line, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
            },
            time = null
        )
        // The authority's own line (Fase 11), in the level's own pair: the level is a
        // word in the sentence first, the colour is its third carrier (§2.3).
        is JournalEntry.WarningChanged -> item(
            badge = if (entry.to == WarningLevel.NONE) {
                EntryBadge.Silhouette(Icons.Outlined.Check, scheme.surfaceContainerHighest, scheme.onSurfaceVariant)
            } else {
                with(WarningText) {
                    EntryBadge.Silhouette(Icons.Outlined.Warning, entry.to.colors.container, entry.to.colors.ink)
                }
            },
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
            // Il giorno del bollettino solo quando non e' quello della riga: una voce
            // del diario sta gia' sotto la sua data, quindi «delle 15:07» li' vuol dire
            // quel giorno — a meno che il bollettino non sia di prima, ed e' il caso che
            // l'ora da sola sbagliava (11 set 2026).
            supporting = entry.issuedAt?.let {
                val text = stringResource(
                    R.string.journal_warning_source,
                    WarningText.issued(context, it, entry.at.atZone(zone).toLocalDate(), timeFmt)
                )
                ({ Text(text, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant) })
            },
            time = time
        )
        is JournalEntry.BulletinMissed -> {
            val text = entry.heldFrom?.let {
                // The date without its year when it is this year's: «21 set 2026» under a
                // heading that is already in September 2026 was the year said twice.
                val held = it.toLocalDate()
                val fmt = if (held.year == LocalDate.now(zone).year) {
                    DateTimeFormatter.ofPattern("d MMMM", locale)
                } else {
                    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
                }
                stringResource(R.string.journal_warning_missed_held, held.format(fmt))
            } ?: stringResource(R.string.journal_warning_missed_empty)
            item(
                badge = quiet,
                headline = stringResource(R.string.journal_warning_missed),
                supporting = { Text(text, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant) },
                time = time
            )
        }
        is JournalEntry.FetchFailed -> {
            // The reason in the diary's own words: the app's error line («Aggiornamento
            // non riuscito: sei offline») under «Un aggiornamento non è arrivato» said
            // the headline twice before it said why.
            val reason = stringResource(
                when (entry.reason) {
                    FetchFailureReason.OFFLINE -> R.string.journal_fail_offline
                    FetchFailureReason.SERVICE -> R.string.journal_fail_service
                    FetchFailureReason.UNKNOWN -> R.string.journal_fail_unknown
                }
            )
            item(
                badge = quiet,
                headline = stringResource(R.string.journal_fetch_failed),
                supporting = { Text(reason, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant) },
                time = time
            )
        }
    }
}

/**
 * «pioggia 70% → **30%** · massima 24° → **27°**» (design review, 23 set 2026): the
 * value it came from in the quiet ink, the value it is now in full ink and weight — the
 * rain one on the rain's own ink ramp — so the eye lands on where the forecast IS.
 * Built on the same format strings [JournalText.shiftDetails] prints for Today, with
 * markers in the two slots, so the two screens keep one vocabulary.
 */
@Composable
private fun styledShift(shifts: List<FieldShift>, units: UnitSettings, locale: Locale): AnnotatedString {
    val quiet = MaterialTheme.colorScheme.onSurfaceVariant
    val strong = MaterialTheme.colorScheme.onSurface
    val colors = ChiaroTheme.colors
    fun temp(raw: String?): String? =
        raw?.toDoubleOrNull()?.let { Formats.temperature(it, units.temperature, locale) }
    val parts = shifts.mapNotNull { shift ->
        when (shift.field) {
            "precip_pct" -> shift.new.toDoubleOrNull()?.toInt()?.let { new ->
                Triple(
                    stringResource(R.string.journal_field_rain, OldMark, NewMark),
                    shift.old?.toDoubleOrNull()?.toInt()?.let { Formats.percent(it, locale) } ?: JournalText.MISSING,
                    Formats.percent(new, locale) to colors.rainInkAt(new)
                )
            }
            "high_c" -> temp(shift.new)?.let { new ->
                Triple(stringResource(R.string.journal_field_high, OldMark, NewMark), temp(shift.old) ?: JournalText.MISSING, new to strong)
            }
            "low_c" -> temp(shift.new)?.let { new ->
                Triple(stringResource(R.string.journal_field_low, OldMark, NewMark), temp(shift.old) ?: JournalText.MISSING, new to strong)
            }
            else -> null
        }
    }
    return buildAnnotatedString {
        parts.forEachIndexed { i, (template, old, new) ->
            if (i > 0) withStyle(SpanStyle(color = quiet)) { append(" · ") }
            val oldAt = template.indexOf(OldMark)
            val newAt = template.indexOf(NewMark)
            if (oldAt < 0 || newAt < 0 || newAt < oldAt) {
                append(template.replace(OldMark, old).replace(NewMark, new.first))
                return@forEachIndexed
            }
            withStyle(SpanStyle(color = quiet)) {
                append(template.substring(0, oldAt))
                append(old)
                append(template.substring(oldAt + OldMark.length, newAt))
            }
            withStyle(SpanStyle(color = new.second, fontWeight = FontWeight.SemiBold)) { append(new.first) }
            withStyle(SpanStyle(color = quiet)) { append(template.substring(newAt + NewMark.length)) }
        }
    }
}

private const val OldMark = "\u0001"
private const val NewMark = "\u0002"

/** What stands at the head of an entry: a category's silhouette on the category's own
 * tone, or — for the lines that are about the sky — the weather family's own drawing. */
private sealed interface EntryBadge {
    data class Silhouette(val icon: ImageVector, val container: Color, val ink: Color) : EntryBadge
    data class Drawing(val icon: ImageVector, val container: Color) : EntryBadge
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
            DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(currentLocale())
        )
    }
}

/**
 * One line of the log, since the design review of 23 set 2026 a stop on a **thread**: the
 * category's badge — a 40dp disc in the category's own tone (a revision in the primary
 * container, an alert of yours in the secondary, an official warning in its level's pair,
 * the sky and the day's outcome in their weather drawing, a missed update in the quiet
 * one) — joined to the badges above and below it by a hairline, the sentence, the facts
 * under it, and the hour trailing as before. It was a column of identical grey
 * silhouettes, which named the category and made the whole screen one colour.
 */
@Composable
private fun EntryItem(
    badge: EntryBadge,
    headline: String,
    supporting: (@Composable () -> Unit)?,
    time: String?,
    threadAbove: Boolean,
    threadBelow: Boolean
) {
    val thread = MaterialTheme.colorScheme.outlineVariant
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val x = (16.dp + BadgeSize / 2).toPx()
                val badgeTop = RowVertical.toPx()
                val badgeBottom = badgeTop + BadgeSize.toPx()
                val stroke = 2.dp.toPx()
                if (threadAbove) drawLine(thread, Offset(x, 0f), Offset(x, badgeTop - 4.dp.toPx()), stroke)
                if (threadBelow) drawLine(thread, Offset(x, badgeBottom + 4.dp.toPx()), Offset(x, size.height), stroke)
            }
            .padding(horizontal = 16.dp, vertical = RowVertical)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(BadgeSize)
                .clip(CircleShape)
                .background(
                    when (badge) {
                        is EntryBadge.Silhouette -> badge.container
                        is EntryBadge.Drawing -> badge.container
                    }
                )
        ) {
            when (badge) {
                is EntryBadge.Silhouette -> Icon(badge.icon, contentDescription = null, tint = badge.ink, modifier = Modifier.size(20.dp))
                is EntryBadge.Drawing -> Icon(badge.icon, contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(30.dp))
            }
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier
                .weight(1f)
                .padding(top = 2.dp)
        ) {
            Text(headline, style = MaterialTheme.typography.bodyLarge)
            supporting?.invoke()
        }
        time?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelMedium.tabular(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

private val BadgeSize = 40.dp
private val RowVertical = 10.dp

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
    highlight: LocalDate?,
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
        // The time axis, said at its two ends (design review, 23 set 2026): «oldest ← →
        // newest» was a sentence under the legend, and a grid read before its axis is a
        // grid read backwards.
        Row(modifier = Modifier.padding(bottom = 4.dp)) {
            Box(modifier = Modifier.width(labelWidth))
            Text(
                text = drift.oldest.atZone(zone).format(dayFmt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = stringResource(R.string.journal_drift_axis_latest),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        drift.dates.forEachIndexed { row, date ->
            val named = date == highlight
            Row(
                verticalAlignment = Alignment.CenterVertically,
                // The day the sentence above names, picked out on its own row.
                modifier = if (named) {
                    Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                } else {
                    Modifier
                }
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    // §10: «Sab 13» in the reader's type, not in 52 fixed dp.
                    modifier = Modifier.width(labelWidth)
                ) {
                    Text(
                        text = date.format(dayFmt),
                        style = if (named) {
                            MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        } else {
                            MaterialTheme.typography.labelSmall
                        },
                        color = if (named) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp)
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
                            .height(20.dp)
                            .padding(1.5.dp)
                            .clip(CellShape)
                            .let { base ->
                                if (color != null) {
                                    base.background(color)
                                } else {
                                    base.border(
                                        width = 0.5.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                        shape = CellShape
                                    )
                                }
                            }
                    )
                }
            }
        }
        DriftLegend(metric, units, locale)
        Text(
            text = pluralStringResource(
                R.plurals.journal_drift_every, drift.columnHours.toInt(), drift.columnHours.toInt()
            ) + " · " + tableHint,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

private val CellShape = RoundedCornerShape(3.dp)

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
                    .clip(CellShape)
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
private fun driftHeadline(
    drift: DriftModel,
    metric: DriftMetric,
    units: UnitSettings,
    locale: Locale
): DriftHeadline {
    val dayFmt = remember(locale) { DateTimeFormatter.ofPattern("EEEE d", locale) }
    /** A day at the head of a sentence takes a capital; in Italian the weekday is
     * lowercase on its own, so the format alone is not enough. */
    fun day(date: LocalDate): String = date.format(dayFmt)
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }

    return when (metric) {
        DriftMetric.RAIN -> {
            val moves = drift.dates.indices.mapNotNull { row -> move(drift.dates[row], drift.rain[row]) }
            val net = moves.filter { kotlin.math.abs(it.last - it.first) >= RAIN_MOVE_PCT }
                .maxByOrNull { kotlin.math.abs(it.last - it.first) }
            val swing = moves.filter { it.high - it.low >= RAIN_MOVE_PCT }
                .maxByOrNull { it.high - it.low }
            when {
                net != null && net.last < net.first -> DriftHeadline(stringResource(
                    R.string.journal_drift_rain_better, day(net.date),
                    Formats.percent(net.first, locale), Formats.percent(net.last, locale)
                ), net.date)
                net != null -> DriftHeadline(stringResource(
                    R.string.journal_drift_rain_worse, day(net.date),
                    Formats.percent(net.first, locale), Formats.percent(net.last, locale)
                ), net.date)
                swing != null -> DriftHeadline(stringResource(
                    R.string.journal_drift_rain_swing, day(swing.date),
                    Formats.percent(swing.low, locale), Formats.percent(swing.high, locale)
                ), swing.date)
                else -> DriftHeadline(stringResource(R.string.journal_drift_quiet), null)
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
                net != null -> DriftHeadline(stringResource(
                    R.string.journal_drift_high_moved,
                    net.date.format(dayFmt), temp(net.first), temp(net.last)
                ), net.date)
                swing != null -> DriftHeadline(stringResource(
                    R.string.journal_drift_high_swing,
                    day(swing.date), temp(swing.low), temp(swing.high)
                ), swing.date)
                else -> DriftHeadline(stringResource(R.string.journal_drift_quiet), null)
            }
        }
    }
}

/** The card's sentence, and the day it is about — which the strip then points at. */
private data class DriftHeadline(val text: String, val date: LocalDate?)

/**
 * How the forecast did (design review, 23 set 2026), from the log's own day-outcome lines:
 * the diary already closes each day with «rain given 40%: it did not rain» and the high it
 * gave against the one it saw, and read together those lines answer the question a reader
 * has about any forecast — can I trust it.
 *
 * Said as averages, never as a score. A 40% chance that stayed dry was not "wrong", and a
 * right/wrong tally of probabilities would be the screen inventing a verdict (§1.1). What
 * is honest is the separation — what the rain was given on the days it rained, against the
 * days it did not — and the high's average distance from the truth. Under the words, one
 * column per day: the rain it was given as a bar on the rain ramp, and what the day did
 * as the weather family's drawing.
 */
@Composable
private fun OutcomeCard(
    outcomes: List<JournalEntry.DayOutcome>,
    units: UnitSettings,
    locale: Locale,
    modifier: Modifier = Modifier
) {
    val stats = outcomeStats(outcomes)
    val narrow = remember(locale) { DateTimeFormatter.ofPattern("EEEEE", locale) }
    val lines = buildList {
        stats.highErrorC?.let { error ->
            val shown = if (units.temperature == com.callbackdev.chiaro.domain.settings.TemperatureUnit.FAHRENHEIT) {
                error * 9.0 / 5.0
            } else {
                error
            }
            add(stringResource(R.string.journal_skill_high, String.format(locale, "%.1f°", shown), stats.highDays))
        }
        when {
            stats.wetAvgPct != null && stats.dryAvgPct != null -> add(
                stringResource(
                    R.string.journal_skill_rain,
                    Formats.percent(stats.wetAvgPct, locale), Formats.percent(stats.dryAvgPct, locale)
                )
            )
            stats.dryAvgPct != null -> add(stringResource(R.string.journal_skill_rain_dry, Formats.percent(stats.dryAvgPct, locale)))
            stats.wetAvgPct != null -> add(stringResource(R.string.journal_skill_rain_wet, Formats.percent(stats.wetAvgPct, locale)))
        }
    }
    val colors = ChiaroTheme.colors
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            lines.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
            // The picture of the same numbers; the sentences above are its text (§9.3).
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) { contentDescription = lines.joinToString(" ") }
            ) {
                outcomes.forEach { day ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = Formats.percent(day.forecastPrecipPct, locale),
                            style = MaterialTheme.typography.labelSmall.tabular(),
                            color = colors.rainInkAt(day.forecastPrecipPct)
                        )
                        Box(
                            contentAlignment = Alignment.BottomCenter,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(OutcomeBar)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.7f)
                                    .height(OutcomeBar * (day.forecastPrecipPct.coerceIn(3, 100) / 100f))
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(colors.rainAt(day.forecastPrecipPct.coerceAtLeast(12)))
                            )
                        }
                        Icon(
                            imageVector = if (day.rained) ChiaroIcons.precipitation else ChiaroIcons.condition(0),
                            contentDescription = null, // the sentences say it
                            tint = Color.Unspecified,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = day.date.format(narrow).uppercase(locale),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/** The averages [OutcomeCard] prints. Pure, for the test. */
internal data class OutcomeStats(
    val highErrorC: Double?,
    val highDays: Int,
    val wetAvgPct: Int?,
    val dryAvgPct: Int?
)

internal fun outcomeStats(outcomes: List<JournalEntry.DayOutcome>): OutcomeStats {
    val pairs = outcomes.mapNotNull { o ->
        val f = o.forecastHighC ?: return@mapNotNull null
        val seen = o.observedHighC ?: return@mapNotNull null
        kotlin.math.abs(f - seen)
    }
    fun avg(list: List<Int>) = if (list.isEmpty()) null else list.average().roundToInt()
    return OutcomeStats(
        highErrorC = if (pairs.isEmpty()) null else pairs.average(),
        highDays = pairs.size,
        wetAvgPct = avg(outcomes.filter { it.rained }.map { it.forecastPrecipPct }),
        dryAvgPct = avg(outcomes.filter { !it.rained }.map { it.forecastPrecipPct })
    )
}

private const val OutcomeDays = 10
private const val MinOutcomes = 3
private val OutcomeBar = 44.dp

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
