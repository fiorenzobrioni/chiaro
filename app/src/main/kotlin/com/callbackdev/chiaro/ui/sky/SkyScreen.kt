package com.callbackdev.chiaro.ui.sky

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.domain.model.MoonPhase
import com.callbackdev.chiaro.domain.sky.MoonQuarterKind
import com.callbackdev.chiaro.domain.sky.SkyJob
import com.callbackdev.chiaro.domain.sky.SkyJobCatalog
import com.callbackdev.chiaro.domain.sky.SkyLead
import com.callbackdev.chiaro.domain.sky.SkyOccurrence
import com.callbackdev.chiaro.domain.sky.SkyVerdictKind
import com.callbackdev.chiaro.ui.components.VerdictChip
import com.callbackdev.chiaro.ui.format.Formats
import com.callbackdev.chiaro.ui.icons.ChiaroIcons
import com.callbackdev.chiaro.ui.places.PlacesSheet
import com.callbackdev.chiaro.ui.places.PlacesViewModel
import com.callbackdev.chiaro.ui.theme.GroupTop
import com.callbackdev.chiaro.ui.theme.SectionBottom
import com.callbackdev.chiaro.ui.theme.SectionTop
import com.callbackdev.chiaro.ui.theme.ChiaroTheme
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Sky (VISION §5.3): tonight's verdict, the day's subscribed moments, the calendar
 * ahead, and the catalog a person learns the sky from — everything in words, the
 * dotted ids never on screen.
 */
@Composable
fun SkyRoute(
    onOpenSettings: () -> Unit,
    skyViewModel: SkyViewModel = viewModel(factory = SkyViewModel.Factory),
    placesViewModel: PlacesViewModel = viewModel(factory = PlacesViewModel.Factory)
) {
    val state by skyViewModel.state.collectAsStateWithLifecycle()
    var placesOpen by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            SkyHeader(
                placeName = (state as? SkyUiState.Content)?.placeName,
                onOpenPlaces = { placesOpen = true },
                onOpenSettings = onOpenSettings
            )
            when (val s = state) {
                SkyUiState.Starting -> Unit // the tick answers within a frame; no skeleton flash
                SkyUiState.NoPlace -> NoPlaceForSky(onOpenPlaces = { placesOpen = true })
                is SkyUiState.Content -> SkyContent(s, skyViewModel)
            }
        }
    }
    if (placesOpen) {
        PlacesSheet(viewModel = placesViewModel, onDismiss = { placesOpen = false })
    }
}

@Composable
private fun SkyHeader(placeName: String?, onOpenPlaces: () -> Unit, onOpenSettings: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.tab_sky), style = MaterialTheme.typography.titleLarge)
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
private fun NoPlaceForSky(onOpenPlaces: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(stringResource(R.string.empty_no_place_title), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.sky_no_place_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        TextButton(onClick = onOpenPlaces) { Text(stringResource(R.string.empty_no_place_action)) }
    }
}

// ---------------------------------------------------------------------------------
// Content
// ---------------------------------------------------------------------------------

/** Which lead picker is open: one moment's, or the default's. */
private sealed interface LeadDialog {
    data class ForMoment(val jobId: String, val lead: SkyLead, val followsDefault: Boolean) : LeadDialog
    data object ForDefault : LeadDialog
}

@Composable
private fun SkyContent(content: SkyUiState.Content, viewModel: SkyViewModel) {
    val locale = Locale.getDefault()
    val is24h = android.text.format.DateFormat.is24HourFormat(LocalContext.current)
    val timeFmt = remember(locale, is24h) { Formats.timeFormatter(is24h, locale) }
    val dateFmt = remember(locale) { DateTimeFormatter.ofPattern("d MMMM", locale) }

    var catalogOpen by remember { mutableStateOf(false) }
    var leadDialog by remember { mutableStateOf<LeadDialog?>(null) }

    // POST_NOTIFICATIONS is asked the first time a reminder is switched on (VISION
    // §5.8), never at startup: the tap that needs it is the sentence that explains it.
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    fun leadChosen(minutes: Int?) {
        if (minutes != null && minutes > 0 && !notificationsAllowed(context)) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            TonightCard(
                tonight = content.tonight,
                zone = content.zone,
                timeFmt = timeFmt
            )
        }

        item { SkySectionTitle(stringResource(R.string.sky_section_moments)) }
        items(content.moments.size) { index ->
            val moment = content.moments[index]
            MomentRow(
                moment = moment,
                zone = content.zone,
                timeFmt = timeFmt,
                onBell = {
                    leadDialog = LeadDialog.ForMoment(
                        moment.job.id, moment.lead, moment.followsDefault
                    )
                }
            )
        }
        item {
            TextButton(
                onClick = { catalogOpen = true },
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null)
                Text(
                    text = stringResource(R.string.sky_add_moment),
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }

        item { SkySectionTitle(stringResource(R.string.sky_section_events)) }
        items(content.events.size) { index ->
            val event = content.events[index]
            EventRow(
                event = event,
                zone = content.zone,
                dateFmt = dateFmt,
                onBell = event.lead?.let { lead ->
                    { leadDialog = LeadDialog.ForMoment(event.job.id, lead, event.followsDefault) }
                }
            )
        }

        item { SkySectionTitle(stringResource(R.string.sky_section_reminders)) }
        item {
            ListItem(
                headlineContent = { Text(stringResource(R.string.sky_default_lead)) },
                supportingContent = { Text(leadLabel(content.defaultLead)) },
                modifier = Modifier.clickable { leadDialog = LeadDialog.ForDefault }
            )
        }
        item {
            ListItem(
                headlineContent = { Text(stringResource(R.string.sky_notify_on_fail)) },
                supportingContent = { Text(stringResource(R.string.sky_notify_on_fail_note)) },
                trailingContent = {
                    Switch(checked = content.notifyOnFail, onCheckedChange = null)
                },
                modifier = Modifier.clickable(
                    onClick = { viewModel.setNotifyOnFail(!content.notifyOnFail) },
                    role = Role.Switch
                )
            )
        }
        item {
            Text(
                text = stringResource(R.string.sky_reminders_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }

    if (catalogOpen) {
        CatalogSheet(
            subscribedIds = content.moments.map { it.job.id }.toSet() +
                content.events.mapNotNull { e -> e.lead?.let { e.job.id } }.toSet(),
            onAdd = viewModel::addMoment,
            onRemove = viewModel::removeMoment,
            onDismiss = { catalogOpen = false }
        )
    }

    when (val dialog = leadDialog) {
        is LeadDialog.ForMoment -> LeadPickerDialog(
            title = stringResource(R.string.sky_lead_title),
            defaultLead = content.defaultLead,
            current = if (dialog.followsDefault) null else (dialog.lead.minutes ?: 0),
            perMoment = true,
            onPick = { minutes ->
                viewModel.setLead(dialog.jobId, minutes)
                leadChosen(minutes ?: content.defaultLead.minutes)
                leadDialog = null
            },
            onDismiss = { leadDialog = null }
        )
        LeadDialog.ForDefault -> LeadPickerDialog(
            title = stringResource(R.string.sky_default_lead),
            defaultLead = content.defaultLead,
            current = content.defaultLead.minutes ?: 0,
            perMoment = false,
            onPick = { minutes ->
                viewModel.setDefaultLead(minutes?.takeIf { it > 0 })
                leadChosen(minutes)
                leadDialog = null
            },
            onDismiss = { leadDialog = null }
        )
        null -> Unit
    }
}

private fun notificationsAllowed(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

@Composable
private fun SkySectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(start = 16.dp, top = SectionTop, bottom = SectionBottom)
    )
}

// ---------------------------------------------------------------------------------
// Tonight
// ---------------------------------------------------------------------------------

/**
 * The hero (VISION §5.3): the verdict word first, the numbers that decided it, and
 * the reason when it was not the clouds. The card wears the verdict's own container
 * color — the same pair every chip uses, so the vocabulary has one look.
 */
@Composable
private fun TonightCard(tonight: Tonight, zone: ZoneId, timeFmt: DateTimeFormatter) {
    val res = LocalContext.current.resources
    val window = tonight.window
    val verdict = tonight.verdict
    val colors = when (verdict?.kind) {
        SkyVerdictKind.PASS -> ChiaroTheme.colors.pass
        SkyVerdictKind.UNSTABLE -> ChiaroTheme.colors.unstable
        SkyVerdictKind.FAIL -> ChiaroTheme.colors.fail
        else -> ChiaroTheme.colors.unknown
    }
    Card(
        colors = CardDefaults.cardColors(
            containerColor = colors.container,
            contentColor = colors.ink
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = stringResource(R.string.sky_tonight_label),
                style = MaterialTheme.typography.titleSmall
            )
            if (window == null) {
                // A fact about the latitude and the season, stated as such.
                Text(
                    text = stringResource(R.string.sky_tonight_no_darkness),
                    style = MaterialTheme.typography.titleMedium
                )
                return@Card
            }
            Text(
                text = stringResource(SkyText.verdictWordRes(verdict?.kind ?: SkyVerdictKind.UNKNOWN)),
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = stringResource(
                    R.string.sky_tonight_window,
                    window.start.atZone(zone).format(timeFmt),
                    (window.end ?: window.start).atZone(zone).format(timeFmt)
                ),
                style = MaterialTheme.typography.bodyMedium
            )
            // The arithmetic, always (DESIGN §8.7): the number that decided it, or
            // the reason there is no number yet.
            val evidence = verdict?.let { SkyText.chipEvidence(res, it) }
            val reason = verdict?.let { SkyText.unknownReason(res, it) }
            val moonLine = if (verdict?.moonPct != null) {
                stringResource(R.string.sky_tonight_moon, verdict.moonPct!!)
            } else null
            (moonLine ?: evidence ?: reason)?.let {
                Text(text = it, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

// ---------------------------------------------------------------------------------
// Moments and events
// ---------------------------------------------------------------------------------

/** DESIGN §8.8 MomentCard: plain name, time, verdict chip with its number, a bell. */
@Composable
private fun MomentRow(
    moment: Moment,
    zone: ZoneId,
    timeFmt: DateTimeFormatter,
    onBell: () -> Unit
) {
    val res = LocalContext.current.resources
    val name = stringResource(SkyText.nameRes(moment.job.id))
    val quiet = MaterialTheme.colorScheme.onSurfaceVariant
    val timeLine = when (val occ = moment.occurrence) {
        is SkyOccurrence.At -> when {
            moment.moonPhase != null -> moonLine(moment.moonPhase, moment.moonIlluminationPct)
            occ.end != null ->
                "${occ.start.atZone(zone).format(timeFmt)} – ${occ.end!!.atZone(zone).format(timeFmt)}"
            else -> occ.start.atZone(zone).format(timeFmt)
        }
        is SkyOccurrence.None -> stringResource(SkyText.notScheduledRes(occ.reason))
    }
    // Which day the time belongs to, in a word. Today's scheduled moments carry no
    // marker — a time with nothing in front of it means today, and saying it on
    // every row would be four "Today"s nobody reads; a `∅` says its day, because
    // "the moon skips it" without one is a sentence missing its subject.
    val dayMark = when (moment.timing) {
        MomentTiming.NOW -> stringResource(R.string.sky_moment_now)
        MomentTiming.TOMORROW -> stringResource(R.string.sky_day_tomorrow)
        MomentTiming.TODAY ->
            if (moment.occurrence is SkyOccurrence.None) {
                stringResource(R.string.sky_day_today)
            } else {
                null
            }
    }
    // The chip lives UNDER the name, never beside it: in a trailing slot a wide
    // verdict ("Niente da fare · nuvole 100%") squeezed the name to one letter per
    // line (device finding, 3 set). Only the fixed-width bell trails.
    ListItem(
        leadingContent = {
            Icon(
                imageVector = momentIcon(moment),
                contentDescription = null,
                tint = quiet,
                modifier = Modifier.size(26.dp)
            )
        },
        headlineContent = { Text(text = name) },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(listOfNotNull(dayMark, timeLine).joinToString(" · "))
                moment.verdict?.let { verdict ->
                    VerdictChip(
                        kind = SkyText.chipKind(verdict.kind),
                        label = stringResource(SkyText.verdictWordRes(verdict.kind)),
                        evidence = SkyText.chipEvidence(res, verdict)
                    )
                }
            }
        },
        trailingContent = {
            BellButton(lead = moment.lead, name = name, onClick = onBell)
        }
    )
}

@Composable
private fun EventRow(
    event: UpcomingEvent,
    zone: ZoneId,
    dateFmt: DateTimeFormatter,
    onBell: (() -> Unit)?
) {
    val res = LocalContext.current.resources
    val name = if (event.quarter == MoonQuarterKind.FULL_MOON) {
        stringResource(R.string.moon_phase_full)
    } else {
        stringResource(SkyText.nameRes(event.job.id))
    }
    val date = event.occurrence.start.atZone(zone).toLocalDate().format(dateFmt)
    val verdictLine = event.verdict?.let { verdict ->
        SkyText.unknownReason(res, verdict)
    }
    // Same rule as MomentRow: the chip goes under the text, only the bell trails.
    ListItem(
        leadingContent = {
            Icon(
                imageVector = eventIcon(event),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(26.dp)
            )
        },
        headlineContent = { Text(name) },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(listOfNotNull(date, verdictLine).joinToString(" · "))
                // An eclipse says which kind it is and the number that decides it,
                // the same way a verdict carries its own arithmetic.
                eclipseLine(res, event)?.let { Text(it) }
                event.verdict?.takeIf { it.kind != SkyVerdictKind.UNKNOWN }?.let { verdict ->
                    VerdictChip(
                        kind = SkyText.chipKind(verdict.kind),
                        label = stringResource(SkyText.verdictWordRes(verdict.kind)),
                        evidence = SkyText.chipEvidence(res, verdict)
                    )
                }
            }
        },
        trailingContent = {
            if (onBell != null && event.lead != null) {
                BellButton(lead = event.lead, name = name, onClick = onBell)
            }
        }
    )
}

/** The eclipse sentence of an event row, or null when the row is not an eclipse. */
private fun eclipseLine(res: android.content.res.Resources, event: UpcomingEvent): String? =
    event.lunarEclipse?.let { SkyText.lunarEclipseLine(res, it) }
        ?: event.solarEclipse?.let { SkyText.solarEclipseLine(res, it) }

@Composable
private fun BellButton(lead: SkyLead, name: String, onClick: () -> Unit) {
    val on = lead != SkyLead.OFF
    IconButton(onClick = onClick) {
        Icon(
            imageVector = if (on) Icons.Filled.Notifications else Icons.Outlined.Notifications,
            contentDescription = stringResource(
                if (on) R.string.sky_bell_on_desc else R.string.sky_bell_off_desc, name
            ),
            tint = if (on) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun moonLine(phase: MoonPhase, illuminationPct: Int?): String {
    val name = stringResource(SkyText.phaseRes(phase))
    return if (illuminationPct != null) {
        stringResource(R.string.sky_moon_line, name, illuminationPct)
    } else name
}

@Composable
private fun momentIcon(moment: Moment) = when {
    moment.moonPhase != null -> ChiaroIcons.moonPhase(moment.moonPhase)
    else -> jobIcon(moment.job)
}

@Composable
private fun eventIcon(event: UpcomingEvent) = when (event.quarter) {
    MoonQuarterKind.FULL_MOON -> ChiaroIcons.moonPhase(MoonPhase.FULL_MOON)
    else -> jobIcon(event.job)
}

@Composable
private fun jobIcon(job: SkyJob) = when (job.id) {
    "sun.rise", "twilight.civil.am", "sun.latest_rise" -> ChiaroIcons.sunrise
    "sun.set", "twilight.civil.pm", "sun.earliest_set" -> ChiaroIcons.sunset
    "solar.noon", "earth.perihelion", "earth.aphelion" -> ChiaroIcons.condition(0, night = false)
    "golden_hour.am", "golden_hour.pm" -> ChiaroIcons.horizon
    "blue_hour.am", "blue_hour.pm",
    "twilight.nautical.am", "twilight.nautical.pm" -> ChiaroIcons.star
    "twilight.astronomical.am", "twilight.astronomical.pm",
    "darkness.window", "milky_way.core",
    "night.white.start", "night.white.end" -> ChiaroIcons.starryNight
    // The zodiacal light is a glow standing out of the horizon, which is the one
    // drawing in the family that says exactly that.
    "zodiacal.am", "zodiacal.pm" -> ChiaroIcons.horizon
    "moon.rise" -> ChiaroIcons.moonrise
    "moon.set" -> ChiaroIcons.moonset
    "moon.new" -> ChiaroIcons.moonPhase(MoonPhase.NEW_MOON)
    "moon.first_quarter" -> ChiaroIcons.moonPhase(MoonPhase.FIRST_QUARTER)
    "moon.last_quarter" -> ChiaroIcons.moonPhase(MoonPhase.LAST_QUARTER)
    // A lunar eclipse happens at a full moon, so the full moon IS its picture.
    "moon.today", "moon.phase", "moon.full",
    "moon.closest_full", "eclipse.lunar" -> ChiaroIcons.moonPhase(MoonPhase.FULL_MOON)
    "eclipse.solar" -> ChiaroIcons.solarEclipse
    "equinox.spring", "solstice.summer",
    "equinox.autumn", "solstice.winter" -> ChiaroIcons.horizon
    else -> ChiaroIcons.fallingStars // the meteor showers
}

// ---------------------------------------------------------------------------------
// The catalog
// ---------------------------------------------------------------------------------

private data class CatalogGroup(val titleRes: Int, val jobs: List<SkyJob>)

private fun catalogGroups(): List<CatalogGroup> {
    val c = SkyJobCatalog
    return listOf(
        CatalogGroup(
            R.string.sky_group_sun,
            listOf(
                c.SunRise, c.SunSet, c.SolarNoon, c.GoldenAm, c.GoldenPm, c.BlueAm, c.BluePm,
                c.CivilAm, c.CivilPm, c.EarliestSunset, c.LatestSunrise
            )
        ),
        CatalogGroup(
            R.string.sky_group_night,
            listOf(
                c.NauticalAm, c.NauticalPm, c.AstronomicalAm, c.AstronomicalPm,
                c.DarknessWindow, c.MilkyWayCore, c.ZodiacalPm, c.ZodiacalAm,
                c.WhiteNightsStart, c.WhiteNightsEnd
            )
        ),
        CatalogGroup(
            R.string.sky_group_moon,
            listOf(
                c.MoonRise, c.MoonSet, c.MoonToday, c.MoonPhase,
                c.MoonNew, c.MoonFirstQuarter, c.MoonFull, c.MoonLastQuarter, c.MoonClosestFull
            )
        ),
        CatalogGroup(R.string.sky_group_eclipses, listOf(c.LunarEclipse, c.SolarEclipse)),
        CatalogGroup(
            R.string.sky_group_seasons,
            listOf(
                c.EquinoxSpring, c.SolsticeSummer, c.EquinoxAutumn, c.SolsticeWinter,
                c.Perihelion, c.Aphelion
            )
        ),
        CatalogGroup(R.string.sky_group_meteors, c.meteorShowers)
    )
}

/**
 * "Add a moment" (VISION §5.3): the whole catalog, grouped, each entry with the one
 * line that teaches what it is. This is where a person learns what a blue hour is —
 * by adding one. A subscribed row taps back out of the list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CatalogSheet(
    subscribedIds: Set<String>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn {
            catalogGroups().forEach { group ->
                item {
                    Text(
                        text = stringResource(group.titleRes),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(
                            start = 16.dp, top = GroupTop, bottom = SectionBottom
                        )
                    )
                }
                items(group.jobs.size) { index ->
                    val job = group.jobs[index]
                    val subscribed = job.id in subscribedIds
                    val name = stringResource(SkyText.nameRes(job.id))
                    ListItem(
                        headlineContent = { Text(name) },
                        supportingContent = { Text(stringResource(SkyText.explanationRes(job.id))) },
                        trailingContent = {
                            Icon(
                                imageVector = if (subscribed) Icons.Outlined.Check else Icons.Outlined.Add,
                                contentDescription = null, // the row itself announces the action
                                tint = if (subscribed) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        modifier = Modifier.clickable {
                            if (subscribed) onRemove(job.id) else onAdd(job.id)
                        }
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------
// The lead picker
// ---------------------------------------------------------------------------------

/**
 * One dialog for both bells. Per-moment it offers "follow the default" (null) and an
 * explicit "never" (0); the default's own picker offers plain off. Pickers, never a
 * free-text field — the same discipline every value with a range gets in this app.
 */
@Composable
private fun LeadPickerDialog(
    title: String,
    defaultLead: SkyLead,
    /** null = follows the default; 0 = explicitly off; else minutes. */
    current: Int?,
    perMoment: Boolean,
    onPick: (Int?) -> Unit,
    onDismiss: () -> Unit
) {
    val options = buildList {
        if (perMoment) {
            add(null to stringResource(R.string.sky_lead_follow_default, leadLabel(defaultLead)))
        }
        add(0 to stringResource(R.string.sky_lead_off))
        SkyLead.entries.filter { it.minutes != null }.forEach { lead ->
            add(lead.minutes to leadLabel(lead))
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.selectableGroup()) {
                options.forEach { (minutes, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = minutes == current,
                                onClick = { onPick(minutes) },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 10.dp)
                    ) {
                        RadioButton(selected = minutes == current, onClick = null)
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

/** "30 minuti prima", "1 ora prima", or the plain off. */
@Composable
private fun leadLabel(lead: SkyLead): String = when (lead) {
    SkyLead.OFF -> stringResource(R.string.sky_lead_off)
    SkyLead.FIFTEEN -> pluralStringResource(R.plurals.sky_lead_minutes, 15, 15)
    SkyLead.THIRTY -> pluralStringResource(R.plurals.sky_lead_minutes, 30, 30)
    SkyLead.ONE_HOUR -> pluralStringResource(R.plurals.sky_lead_hours, 1, 1)
    SkyLead.THREE_HOURS -> pluralStringResource(R.plurals.sky_lead_hours, 3, 3)
    SkyLead.ONE_DAY -> stringResource(R.string.sky_lead_one_day)
}
