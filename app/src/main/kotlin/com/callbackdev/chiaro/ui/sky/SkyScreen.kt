package com.callbackdev.chiaro.ui.sky

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.ui.components.NotificationsOffCard
import com.callbackdev.chiaro.ui.components.rememberNotificationRequest
import com.callbackdev.chiaro.ui.components.rememberNotificationsAllowed
import com.callbackdev.chiaro.domain.model.MoonPhase
import com.callbackdev.chiaro.domain.sky.MoonQuarterKind
import com.callbackdev.chiaro.domain.sky.SkyJob
import com.callbackdev.chiaro.domain.sky.SkyJobCatalog
import com.callbackdev.chiaro.domain.sky.SkyLead
import com.callbackdev.chiaro.domain.sky.SkyNotScheduled
import com.callbackdev.chiaro.domain.sky.SkyOccurrence
import com.callbackdev.chiaro.domain.sky.SkyVerdictKind
import com.callbackdev.chiaro.ui.components.VerdictChip
import com.callbackdev.chiaro.ui.format.Formats
import com.callbackdev.chiaro.ui.icons.ChiaroIcons
import com.callbackdev.chiaro.ui.icons.WeatherIconSize
import com.callbackdev.chiaro.ui.places.PlacesSheet
import com.callbackdev.chiaro.ui.places.PlacesViewModel
import com.callbackdev.chiaro.ui.theme.GroupTop
import com.callbackdev.chiaro.ui.theme.SectionBottom
import com.callbackdev.chiaro.ui.theme.SectionTop
import com.callbackdev.chiaro.ui.theme.ChiaroTheme
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

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
    // The guide takes the tab rather than opening beside it: it is a document, and a
    // reader who is in it is reading, not watching tonight's verdict. The bottom bar
    // stays, because they never left the Sky tab.
    var guideOpen by rememberSaveable { mutableStateOf(false) }
    if (guideOpen) {
        SkyGuideRoute(onClose = { guideOpen = false })
        return
    }

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
                is SkyUiState.Content -> SkyContent(
                    content = s,
                    viewModel = skyViewModel,
                    onOpenGuide = { guideOpen = true }
                )
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
private fun SkyContent(
    content: SkyUiState.Content,
    viewModel: SkyViewModel,
    onOpenGuide: () -> Unit
) {
    val locale = Locale.getDefault()
    val is24h = android.text.format.DateFormat.is24HourFormat(LocalContext.current)
    val timeFmt = remember(locale, is24h) { Formats.timeFormatter(is24h, locale) }
    val dateFmt = remember(locale) { DateTimeFormatter.ofPattern("d MMMM", locale) }
    // The same date with its year, for a row that is not in this one: the aperiodic
    // searches reach years out and "2 agosto" alone was a date the reader could not
    // place (Fase 27).
    val yearFmt = remember(locale) { DateTimeFormatter.ofPattern("d MMMM yyyy", locale) }

    var catalogOpen by remember { mutableStateOf(false) }
    var leadDialog by remember { mutableStateOf<LeadDialog?>(null) }

    // POST_NOTIFICATIONS is asked the first time a reminder is switched on (VISION
    // §5.8), never at startup: the tap that needs it is the sentence that explains it.
    // And once a bell IS set, the card above the list says so for as long as the phone
    // cannot ring it — the same repair Avvisi carries, for the same reason.
    val notificationsAllowed by rememberNotificationsAllowed()
    val request = rememberNotificationRequest()
    fun leadChosen(minutes: Int?) {
        if (minutes != null && minutes > 0 && !notificationsAllowed) request.ask()
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        if (!notificationsAllowed && remindersArmed(content)) {
            item {
                NotificationsOffCard(
                    body = stringResource(R.string.notifications_off_sky),
                    onAllow = request.ask,
                    settingsOnly = request.settingsOnly,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
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
        // A row and not a second button beside "Add a moment": the two labels are a
        // line and a half on a 360dp screen in Italian, and this one is the door for a
        // reader who came to understand rather than to subscribe.
        item {
            ListItem(
                leadingContent = {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null, // the headline right beside it says it
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                headlineContent = { Text(stringResource(R.string.sky_guide_entry_title)) },
                supportingContent = {
                    Text(stringResource(R.string.sky_guide_entry_subtitle))
                },
                modifier = Modifier.clickable(onClick = onOpenGuide)
            )
        }

        item { SkySectionTitle(stringResource(R.string.sky_section_events)) }
        items(content.events.size) { index ->
            val event = content.events[index]
            EventRow(
                event = event,
                zone = content.zone,
                dateFmt = dateFmt,
                yearFmt = yearFmt,
                timeFmt = timeFmt,
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
            // The store's own set, not one rebuilt from the rows on screen: a subscribed
            // job with no row (an eclipse search that finds nothing ahead) used to show
            // as unsubscribed here and offer to be added twice (review, 8 set 2026).
            subscribedIds = content.subscribedIds,
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

/**
 * Whether any bell on this screen is really set: a subscribed moment, or a row of the
 * calendar ahead, with a lead other than «mai». The default lead on its own is not a
 * promise — it is what a moment adopts when it is subscribed to — so a screen with no
 * armed row has nothing to warn about.
 */
internal fun remindersArmed(content: SkyUiState.Content): Boolean =
    content.moments.any { it.lead != SkyLead.OFF } ||
        content.events.any { it.lead != null && it.lead != SkyLead.OFF }

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
                // A fact about the latitude and the season, stated as such — and the
                // RIGHT fact: an empty window is also the deep polar night, where it is
                // dark at noon and "never gets fully dark" would be the reverse of the
                // truth (8 set 2026). The engine tells the two apart; the card repeats it.
                //
                // Since Fase 27 there is a third empty sky and it is the common one: a
                // moon up from dusk to dawn. It gets its own sentence, with the night it
                // did NOT cancel stated after it — the sky is still dark, it is just not
                // dark enough to plan a faint thing around, and a card that said only
                // "no dark window" would have the reader believe the sun is misbehaving.
                val night = tonight.night
                val moonPct = tonight.moonIlluminationPct
                val text = when {
                    tonight.reason == SkyNotScheduled.MOON_ALL_NIGHT &&
                        night?.end != null && moonPct != null -> stringResource(
                        R.string.sky_tonight_moon_all_night,
                        moonPct,
                        night.start.atZone(zone).format(timeFmt),
                        night.end!!.atZone(zone).format(timeFmt)
                    )
                    tonight.reason == SkyNotScheduled.DARK_ALL_DAY ->
                        stringResource(R.string.sky_tonight_dark_all_day)
                    else -> stringResource(R.string.sky_tonight_no_darkness)
                }
                Text(text = text, style = MaterialTheme.typography.titleMedium)
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
            // What the moon took out of the night, when it took something (Fase 27).
            // It is read off the window's edges against the night's, not off the
            // verdict: the window IS the moonless part now, so the moon is a fact
            // about where it opens rather than a downgrade applied afterwards.
            val moonPct = tonight.moonIlluminationPct
            val moonLine = when {
                moonPct == null -> null
                tonight.moonHeldTheStart -> stringResource(
                    R.string.sky_tonight_moon_sets,
                    window.start.atZone(zone).format(timeFmt),
                    moonPct
                )
                tonight.moonTookTheEnd -> stringResource(
                    R.string.sky_tonight_moon_rises,
                    (window.end ?: window.start).atZone(zone).format(timeFmt),
                    moonPct
                )
                else -> null
            }
            moonLine?.let { Text(text = it, style = MaterialTheme.typography.bodyMedium) }
            // And WHEN, on a night the verdict had to average (Fase 28): the clearest
            // run of hours inside the window, which is the half of "so-so" a reader can
            // actually act on. Absent on most nights, by design.
            tonight.clearStretch?.let { stretch ->
                Text(
                    text = stringResource(
                        R.string.sky_tonight_clear_between,
                        stretch.start.atZone(zone).format(timeFmt),
                        stretch.endInclusive.atZone(zone).format(timeFmt)
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            // The arithmetic, always (DESIGN §8.7): the number that decided it, or
            // the reason there is no number yet — and on its own line, beside the
            // moon rather than instead of it. The two used to share one slot with the
            // moon winning, so a card that blamed the moon printed no cloud figure at
            // all, which is half an answer to the one question this card exists for.
            val evidence = verdict?.let { SkyText.chipEvidence(res, it) }
            val reason = verdict?.let { SkyText.unknownReason(res, it) }
            (evidence ?: reason)?.let {
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
    // line (device finding, 3 set). Only the fixed-width bell trails. Since 12 set it
    // lives under the whole list item too — see [SkyVerdictLine].
    Column {
        ListItem(
            leadingContent = {
                // Its own colors and the Sky's own rung (review, 8 set 2026): these were
                // 26dp silhouettes in `onSurfaceVariant`, the one place left where the
                // family was tinted flat — and tinted flat the full moon and the new moon
                // are the same disc, a sunrise and a sunset the same horizon. §13.1 holds
                // here as on Today: the icons depict the world and keep their palette.
                Icon(
                    imageVector = momentIcon(moment),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(WeatherIconSize.Sky)
                )
            },
            headlineContent = { SkyHeadline(name, moment.job.photographic) },
            supportingContent = {
                Text(
                    text = listOfNotNull(dayMark, timeLine, bearingLine(moment.bearingDeg))
                        .joinToString(" · "),
                    color = quiet
                )
            },
            trailingContent = {
                BellButton(lead = moment.lead, name = name, onClick = onBell)
            }
        )
        moment.verdict?.let { verdict ->
            SkyVerdictLine {
                VerdictChip(
                    kind = SkyText.chipKind(verdict.kind),
                    label = stringResource(SkyText.verdictWordRes(verdict.kind)),
                    evidence = SkyText.chipEvidence(res, verdict)
                )
            }
        }
    }
}

/**
 * The verdict's own line under a Sky row: it starts where the row's text starts and runs
 * to the card's far inset, the bell's column included.
 *
 * **Why it is not in the list item's `supportingContent` any more** (committente, 12 set
 * 2026, from a device: "can the chip have more room, so the text does not wrap so
 * easily?"). That slot is the item's text column, and a text column stops where the
 * trailing slot begins. Material spends **163 dp** of a row with a bell on insets and
 * fixed slots — `16 (edge) + 51 (the glyph) + 16 + 16 + 48 (the bell) + 16`, and the
 * fourth 16 is real: measured on the device's own screenshot, the chip ends at 303.6 dp
 * and the bell's 48 dp box is centred at 344, so there are 16.4 dp between them. (The
 * note on [WeatherIconSize.Sky] counted one 16 too few until this pass; a row without a
 * bell, which is where its other figure comes from, has no such gap and was right.)
 *
 * That leaves **221 dp** for the column on this 384 dp screen and 197 at 360. The chip
 * measured on the same screenshot — «✗ Niente da fare  nuvole 66%» — wants **223**:
 * 24 of padding, 14 of mark, two 6 dp gaps, 92.4 of word and 77 of number, with 3.8 of
 * side bearings read off the «✓ Bello  nuvole 0%» chip beside it, which fits on one
 * line at 154. So it missed by **two dp**, and what wrapped was the number under the
 * word — the one pair this app may not break apart (§8.7: a verdict ships with its
 * arithmetic). Two dp is not a margin, which is what "does not wrap so easily" means:
 * the widest pair the app can print, «Niente da fare» with «pioggia 100%», wants ~236.
 *
 * Nothing of the bell reaches this line, so the line may have the bell's 48 dp and the
 * gap before it: the chip's room becomes `screen − 83 − 16`, which is **285 dp** here
 * and **261 at 360** — clear of the worst 236 on both. What it costs is height and air:
 * the item is now a two-line item, so Material centres its 44 dp of text in its own
 * 72 dp minimum and the gap over the chip goes from 6 dp to ~14, and the row grows about
 * 6 dp. The alternatives were measured before being dropped, and none of them buys a
 * margin: a 40 dp bell is worth 8 dp (the 48 dp touch target survives, `IconButton`
 * extends it past its own bounds), a tighter chip 4, and the Sky's 51 dp glyph is a
 * decision of 11 set, not slack.
 *
 * [SkyChipIndent] is Material's own list-item arithmetic — 16 to the leading edge, the
 * glyph, 16 more to the text — so the chip's mark lines up under the name above it.
 */
@Composable
private fun SkyVerdictLine(chip: @Composable () -> Unit) {
    Box(
        modifier = Modifier.padding(
            start = SkyChipIndent, end = SkyRowInset, bottom = SkyChipBottom
        )
    ) {
        chip()
    }
}

private val SkyRowInset = 16.dp
private val SkyChipIndent = SkyRowInset + WeatherIconSize.Sky + SkyRowInset
private val SkyChipBottom = 8.dp

@Composable
private fun EventRow(
    event: UpcomingEvent,
    zone: ZoneId,
    dateFmt: DateTimeFormatter,
    yearFmt: DateTimeFormatter,
    timeFmt: DateTimeFormatter,
    onBell: (() -> Unit)?
) {
    val res = LocalContext.current.resources
    // The name, and the names of anything sharing this instant: the delta Aquariids
    // and the alpha Capricornids peak on one night, so they get one row (Fase 27).
    val name = eventName(event)
    // A `∅` says its reason where the date would go. The row is kept rather than
    // dropped because the reader asked for this line, and "the sky never gets fully
    // dark here in August" is the answer to it — the same rule the moments list above
    // has always followed.
    val whenLine = when (val occurrence = event.occurrence) {
        is SkyOccurrence.At -> {
            val date = occurrence.start.atZone(zone).toLocalDate()
                .format(if (event.showYear) yearFmt else dateFmt)
            // The hour, for the one kind of event whose hour is the whole content. A
            // shower's row is a night nine hours wide and a solstice is a date; an
            // eclipse is ninety minutes you either step outside for or miss.
            val window = eclipseWindow(event, zone, timeFmt)
            listOfNotNull(date, window).joinToString(" · ")
        }
        is SkyOccurrence.None -> stringResource(SkyText.notScheduledRes(occurrence.reason))
    }
    val verdictLine = event.verdict?.let { verdict -> SkyText.unknownReason(res, verdict) }
    // Same rule as MomentRow: the chip goes under the text on a line of its own
    // ([SkyVerdictLine]), only the bell trails.
    Column {
        ListItem(
            leadingContent = {
                Icon(
                    imageVector = eventIcon(event),
                    contentDescription = null, // as in MomentRow: the family's own colors
                    tint = Color.Unspecified,
                    modifier = Modifier.size(WeatherIconSize.Sky)
                )
            },
            headlineContent = { SkyHeadline(name, event.job.photographic) },
            supportingContent = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        listOfNotNull(
                            whenLine,
                            conjunctionLine(event),
                            bearingLine(event.bearingDeg),
                            verdictLine
                        ).joinToString(" · ")
                    )
                    // An eclipse says which kind it is and the number that decides it,
                    // the same way a verdict carries its own arithmetic.
                    eclipseLine(res, event)?.let { Text(it) }
                }
            },
            trailingContent = {
                if (onBell != null && event.lead != null) {
                    BellButton(lead = event.lead, name = name, onClick = onBell)
                }
            }
        )
        event.verdict?.takeIf { it.kind != SkyVerdictKind.UNKNOWN }?.let { verdict ->
            SkyVerdictLine {
                VerdictChip(
                    kind = SkyText.chipKind(verdict.kind),
                    label = stringResource(SkyText.verdictWordRes(verdict.kind)),
                    evidence = SkyText.chipEvidence(res, verdict)
                )
            }
        }
    }
}

/**
 * The row's headline: its own name, plus anything that peaks on the same night.
 *
 * Two names are joined with the locale's own conjunction; three or more — which the
 * shower table cannot currently produce, but the collapse rule does not know that —
 * fall back to a comma list with the conjunction on the last, which is how both
 * languages write one.
 */
@Composable
private fun eventName(event: UpcomingEvent): String {
    val own = if (event.quarter == MoonQuarterKind.FULL_MOON) {
        stringResource(R.string.moon_phase_full)
    } else {
        stringResource(SkyText.nameRes(event.job.id))
    }
    if (event.sharesNightWith.isEmpty()) return own
    val others = event.sharesNightWith.map { stringResource(SkyText.nameRes(it.id)) }
    val head = (listOf(own) + others.dropLast(1)).joinToString(", ")
    return stringResource(R.string.sky_event_and, head, others.last())
}

/** The contact window of an eclipse row, or null for every other kind of event. */
@Composable
private fun eclipseWindow(
    event: UpcomingEvent,
    zone: ZoneId,
    timeFmt: DateTimeFormatter
): String? {
    val at = event.at ?: return null
    if (event.lunarEclipse == null && event.solarEclipse == null) return null
    val end = at.end ?: return at.start.atZone(zone).format(timeFmt)
    return "${at.start.atZone(zone).format(timeFmt)} – ${end.atZone(zone).format(timeFmt)}"
}

/** The eclipse sentence of an event row, or null when the row is not an eclipse. */
private fun eclipseLine(res: android.content.res.Resources, event: UpcomingEvent): String? =
    event.lunarEclipse?.let { SkyText.lunarEclipseLine(res, it) }
        ?: event.solarEclipse?.let { SkyText.solarEclipseLine(res, it) }

/**
 * A row's name, with the camera mark after it when the catalog says the event is one
 * somebody would bring a camera to (Fase 28).
 *
 * A Material glyph rather than one of the weather family, and that is on purpose: the
 * weather icons depict the sky and keep their own palette (§13.1), while this is
 * chrome — it says something about the ROW, the way the bell beside it does, and it is
 * tinted like chrome. Eighteen density-independent pixels, quiet ink, after the name
 * and never before it: the name is what the reader is scanning for.
 */
@Composable
private fun SkyHeadline(name: String, photographic: Boolean) {
    if (!photographic) {
        Text(name)
        return
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(name, modifier = Modifier.weight(1f, fill = false))
        Icon(
            painter = painterResource(R.drawable.ic_photographic),
            contentDescription = stringResource(R.string.sky_photographic_desc),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(start = 6.dp)
                .size(18.dp)
        )
    }
}

/**
 * Which way to turn, in words — «verso ovest-nordovest», never «verso 292°».
 *
 * The half of the camera mark that is actually useful: a flag saying an event is worth
 * photographing and no direction to point in is an opinion, and this app does not print
 * those. Null on most rows by design ([SkySights.bearing]): an equinox does not happen
 * in a direction.
 */
@Composable
private fun bearingLine(bearingDeg: Double?): String? = bearingDeg?.let {
    stringResource(R.string.sky_bearing_towards, stringResource(SkyText.bearingRes(it)))
}

/**
 * How close a pair gets, which is the whole content of a conjunction row. Under a
 * degree it is said in words: «0°» would read as a collision, and the app would be
 * printing a rounding as if it were the fact.
 */
@Composable
private fun conjunctionLine(event: UpcomingEvent): String? =
    event.conjunctionSeparationDeg?.let { degrees ->
        if (degrees < 1.0) {
            stringResource(R.string.sky_conjunction_very_close)
        } else {
            stringResource(R.string.sky_conjunction_apart, degrees.roundToInt())
        }
    }

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
internal fun jobIcon(job: SkyJob) = when (job.id) {
    "sun.rise", "twilight.civil.am", "sun.latest_rise" -> ChiaroIcons.sunrise
    "sun.set", "twilight.civil.pm", "sun.earliest_set" -> ChiaroIcons.sunset
    "solar.noon", "earth.perihelion", "earth.aphelion" -> ChiaroIcons.condition(0, night = false)
    "golden_hour.am", "golden_hour.pm" -> ChiaroIcons.goldenHour
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
    // A moonrise is what the full moon at dusk IS, so it borrows the drawing.
    "moon.full_at_dusk" -> ChiaroIcons.moonrise
    "earthshine.pm" -> ChiaroIcons.moonPhase(MoonPhase.WAXING_CRESCENT)
    "earthshine.am" -> ChiaroIcons.moonPhase(MoonPhase.WANING_CRESCENT)
    // A planet to the naked eye IS a bright point of light, so the family's own star
    // is the honest drawing for it — Meteocons has no planets and inventing one would
    // be a disk nobody sees. The pairs take it too: two points, one of them this.
    "venus.evening", "venus.morning", "jupiter.night",
    "conjunction.moon_venus", "conjunction.moon_jupiter",
    "conjunction.venus_jupiter" -> ChiaroIcons.star
    "equinox.spring", "solstice.summer",
    "equinox.autumn", "solstice.winter" -> ChiaroIcons.horizon
    else -> ChiaroIcons.fallingStars // the meteor showers
}

// ---------------------------------------------------------------------------------
// The catalog
// ---------------------------------------------------------------------------------

/**
 * "Add a moment" (VISION §5.3): the whole catalog, grouped, each entry with the one
 * line that teaches what it is. This is where a person learns what a blue hour is —
 * by adding one. A subscribed row taps back out of the list.
 *
 * The one line was never quite enough, though, and the info button is the other half:
 * it opens the event's page INSIDE the sheet, so reading about the zodiacal light
 * does not throw away the list you were halfway down, and the page carries the button
 * that adds it. Two targets on one row, which is why the icon is a real
 * [IconButton] and not a second clickable modifier: 48dp, or it is decoration that
 * happens to be tappable.
 *
 * The grouping itself lives in [SkyGuide], because the guide's index shows the same
 * fifty-one events and two orders would be the app disagreeing with itself.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CatalogSheet(
    subscribedIds: Set<String>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var openId by rememberSaveable { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        val open = openId?.let { SkyJobCatalog.byId(it) }
        if (open != null) {
            // Back closes the page, not the sheet: the reader came here from the list
            // and that is where the gesture should put them back.
            BackHandler { openId = null }
            val subscribed = open.id in subscribedIds
            SkyEventPage(
                job = open,
                onOpenRelated = { openId = it },
                onBack = { openId = null },
                action = {
                    // Adding returns to the list, where the check mark is the receipt.
                    CatalogAction(
                        subscribed = subscribed,
                        onClick = {
                            if (subscribed) onRemove(open.id) else onAdd(open.id)
                            openId = null
                        }
                    )
                }
            )
            return@ModalBottomSheet
        }
        // Sixty entries in six groups is a long scroll for a reader who came looking for
        // «Perseidi» by name (review, Fase 28). The field filters on the words the rows
        // already print — the name and the one line under it — and never on the dotted
        // id, which does not appear on this screen and never will (VISION §5.3).
        val filtered = SkyGuide.groups.map { group ->
            group to group.jobs.filter { job ->
                query.isBlank() || matchesQuery(job, query)
            }
        }
        CatalogSearchField(query = query, onQuery = { query = it })
        LazyColumn {
            if (filtered.all { it.second.isEmpty() }) {
                item {
                    // The honest empty state: absence stated, never an empty list that
                    // reads as a broken screen.
                    Text(
                        text = stringResource(R.string.sky_catalog_no_match, query),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp)
                    )
                }
            }
            filtered.forEach { (group, jobs) ->
                if (jobs.isEmpty()) return@forEach
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
                items(jobs.size) { index ->
                    val job = jobs[index]
                    val subscribed = job.id in subscribedIds
                    val name = stringResource(SkyText.nameRes(job.id))
                    ListItem(
                        headlineContent = { Text(name) },
                        supportingContent = { Text(stringResource(SkyText.explanationRes(job.id))) },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { openId = job.id }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Info,
                                        contentDescription = stringResource(
                                            R.string.sky_guide_open_desc, name
                                        ),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = if (subscribed) Icons.Outlined.Check else Icons.Outlined.Add,
                                    contentDescription = null, // the row itself announces the action
                                    tint = if (subscribed) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
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

/**
 * The catalog's search field. Its own composable so the sheet's list stays a list.
 *
 * Not a `SearchBar`: that component brings its own expanding surface and its own
 * results pane, and inside a bottom sheet already holding a list it would be a second
 * scrolling surface over the first. A plain field over the list is what this is.
 */
@Composable
private fun CatalogSearchField(query: String, onQuery: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQuery,
        singleLine = true,
        label = { Text(stringResource(R.string.sky_catalog_search)) },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQuery("") }) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.sky_catalog_search_clear)
                    )
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

/**
 * Whether a catalog row answers the query, matched on the two strings the row itself
 * prints — accent- and case-insensitively, because a reader typing «perseidi» on a
 * phone keyboard is not going to reach for the right diacritic and should not have to.
 */
@Composable
private fun matchesQuery(job: SkyJob, query: String): Boolean {
    val needle = query.foldForSearch()
    val name = stringResource(SkyText.nameRes(job.id)).foldForSearch()
    val explanation = stringResource(SkyText.explanationRes(job.id)).foldForSearch()
    return needle in name || needle in explanation
}

/** Lower case, accents stripped: «Luce cinerea» and «luce cinerea» are one word here. */
private fun String.foldForSearch(): String =
    java.text.Normalizer.normalize(this, java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .lowercase(Locale.getDefault())

/** The one button of an event's page inside the sheet: filled to add, outlined to
 * undo — the same weight order every destructive-ish action in the app uses. */
@Composable
private fun CatalogAction(subscribed: Boolean, onClick: () -> Unit) {
    if (subscribed) {
        OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.sky_guide_remove_action))
        }
    } else {
        Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.sky_guide_add_action))
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
