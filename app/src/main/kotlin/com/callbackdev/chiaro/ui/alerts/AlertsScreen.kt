package com.callbackdev.chiaro.ui.alerts

import com.callbackdev.chiaro.ui.components.ChiaroSheet
import com.callbackdev.chiaro.ui.components.TabHeader
import com.callbackdev.chiaro.ui.components.SegmentGap
import com.callbackdev.chiaro.ui.components.SegmentedGroup
import com.callbackdev.chiaro.ui.components.segmentShape
import com.callbackdev.chiaro.ui.theme.tabular
import com.callbackdev.chiaro.ui.warnings.WarningText.colors
import com.callbackdev.chiaro.domain.warnings.PlaceWarnings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import com.callbackdev.chiaro.ui.icons.ChiaroIcons
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.callbackdev.chiaro.domain.AlertKind
import com.callbackdev.chiaro.notifications.NotificationPermission
import com.callbackdev.chiaro.notifications.RuleNotifier
import com.callbackdev.chiaro.ui.components.rememberMutedChannels
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.callbackdev.chiaro.ui.format.currentLocale
import com.callbackdev.chiaro.ui.components.ListRow
import com.callbackdev.chiaro.ui.theme.GroupTop
import com.callbackdev.chiaro.ui.theme.SectionBottom
import com.callbackdev.chiaro.ui.shell.PageColumns
import com.callbackdev.chiaro.ui.shell.PaneBlocks
import com.callbackdev.chiaro.ui.shell.PaneTitleTop
import com.callbackdev.chiaro.ui.shell.pageColumns
import com.callbackdev.chiaro.ui.shell.paneItems
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.mutableIntStateOf
import com.callbackdev.chiaro.ui.theme.SectionTop
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.ui.components.NotificationsOffCard
import com.callbackdev.chiaro.ui.components.rememberNotificationRequest
import com.callbackdev.chiaro.ui.components.rememberNotificationsAllowed
import com.callbackdev.chiaro.data.warnings.PlaceWarningState
import com.callbackdev.chiaro.domain.rules.MaxConditions
import com.callbackdev.chiaro.domain.rules.MaxRules
import com.callbackdev.chiaro.domain.rules.NotificationRule
import com.callbackdev.chiaro.domain.rules.RuleBand
import com.callbackdev.chiaro.domain.rules.RuleCondition
import com.callbackdev.chiaro.domain.rules.RuleMessages
import com.callbackdev.chiaro.domain.rules.RuleOp
import com.callbackdev.chiaro.domain.rules.RuleVariableKind
import com.callbackdev.chiaro.domain.rules.RuleVariables
import com.callbackdev.chiaro.domain.rules.RuleWindow
import com.callbackdev.chiaro.domain.rules.RuleWindowKind
import com.callbackdev.chiaro.domain.settings.UnitSettings
import com.callbackdev.chiaro.domain.warnings.WarningLevel
import com.callbackdev.chiaro.ui.places.PlacesSheet
import com.callbackdev.chiaro.ui.places.PlacesViewModel
import com.callbackdev.chiaro.ui.warnings.WarningText
import com.callbackdev.chiaro.ui.warnings.WarningSheet
import com.callbackdev.chiaro.ui.format.Formats
import com.callbackdev.chiaro.ui.format.LocalClock
import com.callbackdev.chiaro.ui.today.NoPlaceState
import com.callbackdev.chiaro.ui.today.freshnessAge
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

/**
 * Alerts (VISION §5.4): two groups, one screen. The ready-made switches say exactly
 * what each will send and when; the reader's own rules are approached from the
 * answer — templates first, then a sentence of tappable chips, never a syntax.
 */
@Composable
fun AlertsRoute(
    onOpenSettings: () -> Unit,
    alertsViewModel: AlertsViewModel = viewModel(factory = AlertsViewModel.Factory),
    placesViewModel: PlacesViewModel = viewModel(factory = PlacesViewModel.Factory)
) {
    val state by alertsViewModel.state.collectAsStateWithLifecycle()
    var placesOpen by remember { mutableStateOf(false) }
    var editingRuleId by rememberSaveable { mutableStateOf<Long?>(null) }
    val snackbars = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val res = LocalContext.current.resources

    AlertsScreen(
        state = state,
        actions = AlertsActions(
            setOfficialWarnings = alertsViewModel::setOfficialWarnings,
            setOfficialWarningsFrom = alertsViewModel::setOfficialWarningsFrom,
            setSevereWeather = alertsViewModel::setSevereWeather,
            setPrecipitationWarning = alertsViewModel::setPrecipitationWarning,
            setDailySummary = alertsViewModel::setDailySummary,
            setEveningSummary = alertsViewModel::setEveningSummary,
            update = alertsViewModel::update,
            addFromTemplate = alertsViewModel::addFromTemplate
        ),
        onEdit = { editingRuleId = it },
        onOpenPlaces = { placesOpen = true },
        onOpenSettings = onOpenSettings,
        snackbars = snackbars
    )

    if (placesOpen) {
        PlacesSheet(viewModel = placesViewModel, onDismiss = { placesOpen = false })
    }

    val content = state as? AlertsUiState.Content
    val editing = content?.rules?.firstOrNull { it.rule.id == editingRuleId }?.rule
    if (editing != null && content != null) {
        RuleEditorSheet(
            rule = editing,
            takenNames = content.rules.filter { it.rule.id != editing.id }.map { it.rule.name },
            units = content.units,
            daylightToday = content.daylightToday,
            lastSync = content.lastSync,
            viewModel = alertsViewModel,
            onDismiss = { editingRuleId = null },
            onDelete = { deleted ->
                // «Annulla» in place of «Eliminare questo avviso?» (the Alerts review, point
                // 10, 14 ott 2026), as the Sky undoes a moment taken away: the sheet closes on
                // the act and the snackbar is the receipt and the way back. It puts the rule
                // back as it was, in its place, with its own id, so its «last fired» returns
                // with it. Long, for an action somebody may need a moment to reach.
                val index = content.rules.indexOfFirst { it.rule.id == deleted.id }
                alertsViewModel.remove(deleted.id)
                editingRuleId = null
                scope.launch {
                    snackbars.currentSnackbarData?.dismiss()
                    val result = snackbars.showSnackbar(
                        message = res.getString(R.string.alerts_rule_deleted, deleted.name),
                        actionLabel = res.getString(R.string.action_undo),
                        duration = SnackbarDuration.Long
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        alertsViewModel.restore(deleted, index)
                    }
                }
            }
        )
    }
}

/**
 * The page itself, from a state and not from the view model: what [AlertsRoute] draws,
 * and what the README's screenshots draw (25 set 2026). The editor sheet stays with the
 * route, because it is the one part of this screen that keeps the view model.
 */
@Composable
internal fun AlertsScreen(
    state: AlertsUiState,
    actions: AlertsActions,
    onEdit: (Long) -> Unit,
    onOpenPlaces: () -> Unit,
    onOpenSettings: () -> Unit,
    snackbars: SnackbarHostState = remember { SnackbarHostState() }
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        // Two panes on a wide page, as Today and the Sky stand (DESIGN §8.12c, 16 ott 2026).
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val columns = pageColumns(maxWidth)
            Column(modifier = Modifier.fillMaxSize()) {
                TabHeader(
                    title = stringResource(R.string.tab_alerts),
                    placeName = (state as? AlertsUiState.Content)?.placeName,
                    onOpenPlaces = onOpenPlaces,
                    onOpenSettings = onOpenSettings,
                    modifier = Modifier.padding(start = columns.start, end = columns.end)
                )
                when (state) {
                    AlertsUiState.Starting -> Unit // the stores answer within a frame; no skeleton flash
                    // The empty state Today draws, with this page's own sentence (15.2, 28 Oct
                    // 2026): with no place there is no zone to answer for and no forecast to
                    // read a rule against, and a page of switches here promised sounds about
                    // nowhere, «disponibili per i luoghi in Italia» at its head.
                    AlertsUiState.NoPlace -> NoPlaceState(
                        onOpenPlaces = onOpenPlaces,
                        body = stringResource(R.string.alerts_no_place_body)
                    )
                    is AlertsUiState.Content -> AlertsContent(
                        content = state,
                        actions = actions,
                        onEdit = onEdit,
                        snackbars = snackbars,
                        columns = columns
                    )
                }
            }
        }
    }
}


// ---------------------------------------------------------------------------------
// The two groups
// ---------------------------------------------------------------------------------

/** What the list can ask of its store, as functions: the content is then a plain
 * composable a test or a preview can draw (the editor keeps the view model). */
internal class AlertsActions(
    val setOfficialWarnings: (Boolean) -> Unit,
    val setOfficialWarningsFrom: (WarningLevel) -> Unit,
    val setSevereWeather: (Boolean) -> Unit,
    val setPrecipitationWarning: (Boolean) -> Unit,
    val setDailySummary: (Boolean) -> Unit,
    val setEveningSummary: (Boolean) -> Unit,
    val update: (NotificationRule) -> Unit,
    val addFromTemplate: (RuleText.Template, (NotificationRule) -> Unit) -> Unit
)

@Composable
private fun AlertsContent(
    content: AlertsUiState.Content,
    actions: AlertsActions,
    onEdit: (Long) -> Unit,
    snackbars: SnackbarHostState,
    columns: PageColumns
) {
    // Asked the first time something that needs it is switched on (VISION §5.8) — and
    // stated in a card above the list for as long as it is missing, because on this
    // screen the switches themselves are the promise (see [notificationsPromised]).
    val notificationsAllowed by rememberNotificationsAllowed()
    val request = rememberNotificationRequest()
    fun somethingTurnedOn() {
        if (!notificationsAllowed) request.ask()
    }

    val locale = currentLocale()
    val is24h = android.text.format.DateFormat.is24HourFormat(LocalContext.current)
    // The day of «scattato l'ultima volta» beyond yesterday (15.7, 28 Oct 2026): day and
    // month, as the history keeps three days and the year is never in doubt.
    val firedDateFmt = remember(locale) { DateTimeFormatter.ofPattern("d MMM", locale) }

    // The active place's own hour, like every other hour in the app.
    val issuedFmt = remember(locale, is24h) { Formats.timeFormatter(is24h, locale) }
    var warningSheetOpen by rememberSaveable { mutableStateOf(false) }
    var ideasOpen by rememberSaveable { mutableStateOf(false) }
    val current = content.warnings as? PlaceWarningState.Current
    // The place's own day, like the "last fired" hour above it.
    // Read here and not in the list below: a lazy list's builder is not a composable.
    val clock = LocalClock.current
    val placeToday = LocalDate.now(clock.withZone(content.zone))
    val placeNow = java.time.LocalTime.now(clock.withZone(content.zone))
    if (warningSheetOpen && current != null) {
        WarningSheet(
            warnings = current.warnings,
            today = placeToday,
            timeFmt = issuedFmt,
            onDismiss = { warningSheetOpen = false }
        )
    }
    fun add(template: RuleText.Template) {
        actions.addFromTemplate(template) { created ->
            somethingTurnedOn()
            onEdit(created.id)
        }
    }
    // A template whose rule already exists — same conditions, whatever the reader renamed
    // it — is marked as added rather than offered again: tapping it a second time made two
    // identical "Bike" rules (review, 8 set 2026).
    val existing = content.rules.map { it.rule.conditions }.toSet()
    if (ideasOpen) {
        IdeasSheet(
            existing = existing,
            onPick = { template ->
                ideasOpen = false
                add(template)
            },
            onDismiss = { ideasOpen = false }
        )
    }

    val listState = rememberLazyListState()
    // The page's blocks, said once for the phone's list and the wide page's two panes
    // (DESIGN §8.12c, 16 ott 2026). On the left the answer and the issuer's warnings,
    // which the card above them answers from: what is in force and whether the phone
    // will say so. On the right the alerts read from the forecast, the app's ready-made
    // ones and then the reader's own, which is where a reader who has made some keeps
    // coming back to. The order is the phone's, read pane by pane; the card that says the
    // phone cannot ring stands across both.
    val blocks = PaneBlocks(remember { mutableIntStateOf(0) })
    // Above everything, and only while the screen would otherwise be lying: the
    // switches below say an alert will arrive, and with notifications off none of
    // them can. With nothing switched on there is no promise to break and no card.
    if (!notificationsAllowed && notificationsPromised(content)) {
        blocks.across {
            NotificationsOffCard(
                body = stringResource(R.string.notifications_off_alerts),
                onAllow = request.ask,
                settingsOnly = request.settingsOnly,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
    // The card's second voice (15.3, 28 Oct 2026): the permission is there, and the phone
    // has silenced a channel one of the switches below promises on — set to «none» in the
    // system's settings, or by a long press on one notification. Every `notify` on it
    // returns false and the rule's card says «never fired» for life, which is the review
    // «it never warns me». Named by the channel's own name, the word the reader will look
    // for on the settings page; several muted, one card that lists them and opens the
    // first. Not drawn while notifications are off altogether: the card above already says
    // nothing can arrive, and two cards for one silence is noise.
    val channels = promisedChannels(content)
    val muted by rememberMutedChannels(channels.map { it.id })
    val mutedChannels = channels.filter { it.id in muted }
    if (notificationsAllowed && mutedChannels.isNotEmpty()) {
        blocks.across {
            val context = LocalContext.current
            val names = mutedChannels.map {
                stringResource(R.string.notifications_muted_name, stringResource(it.nameRes))
            }
            val listed = remember(names, locale) {
                android.icu.text.ListFormatter.getInstance(locale).format(names)
            }
            NotificationsOffCard(
                title = pluralStringResource(R.plurals.notifications_muted_title, names.size),
                body = pluralStringResource(R.plurals.notifications_muted_body, names.size, listed),
                // Only the channel's own page can un-mute it: no dialog to offer, so the
                // button says «Open settings» whatever the permission's state.
                settingsOnly = true,
                onAllow = { NotificationPermission.openChannelSettings(context, mutedChannels.first().id) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
    // VISION §5.4 and Fase 11: the authority's warnings lead, because they are the
    // only ones on this screen nobody chose — and because the reader who opens
    // Avvisi in an autumn afternoon is usually here to check exactly this. Since the
    // Alerts review (point 7, 14 ott 2026) they lead as the answer of one card, which
    // goes on to say when the phone may ring next and draws the day under it.
    blocks.left {
        AlertsHero(
            content = content,
            today = placeToday,
            now = placeNow,
            timeFmt = issuedFmt,
            onOpenSheet = { warningSheetOpen = true }
        )
    }
    blocks.left { GroupTitle(stringResource(R.string.alerts_group_warnings)) }
    // The switch is a setting, not a property of the place on screen: it stays put
    // when the active place is abroad, because hiding a setting behind today's
    // choice of city is how a setting becomes unfindable. The card above is what
    // says whether this place has a zone at all — and the hours over the switch
    // follow it (15.2, 28 Oct 2026): «15–17» over the switch of a place no bulletin
    // grades named a window the phone could not ring in.
    //
    // In a group of its own with the level it starts from (design review, 23 set
    // 2026): the level is a property of the switch, and as two loose rows under a
    // row they read as three settings.
    blocks.left {
        SegmentedGroup(
            segments = buildList<@Composable () -> Unit> {
                add {
                    AlertRow(
                        icon = ChiaroIcons.warning,
                        iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                        title = stringResource(R.string.warning_switch_title),
                        hours = stringResource(R.string.alerts_hours_range, BulletinFrom, BulletinTo)
                            .takeIf { bulletinPossible(content.warnings) },
                        description = stringResource(R.string.warning_switch_desc),
                        cadence = stringResource(R.string.warning_switch_when, BulletinFrom, BulletinTo),
                        checked = content.notifications.officialWarnings,
                        // Silenced when every channel it can post on is: the orange-and-red
                        // one, and the yellow one while the switch starts from yellow.
                        muted = warningsRowMuted(channels, muted),
                        onChange = { actions.setOfficialWarnings(it); if (it) somethingTurnedOn() }
                    )
                }
                // The other honest road to the same choice: the two channels let the
                // system silence the yellow, this lets the app never send it (§8.13).
                if (content.notifications.officialWarnings) {
                    add {
                        WarningFromRow(
                            from = content.notifications.officialWarningsFrom,
                            onChange = actions.setOfficialWarningsFrom
                        )
                    }
                }
            },
            modifier = Modifier.padding(vertical = 4.dp)
        )
    }

    blocks.right {
        GroupTitle(
            stringResource(R.string.alerts_group_ready),
            top = if (columns.twoPanes) PaneTitleTop else SectionTop
        )
    }
    // One group, four rows with their own drawing (design review, 23 set 2026): four
    // loose list items of three or four lines each were the densest block of text in
    // the app, and nothing on them said which was which before it was read.
    // Four segments (the Alerts review, point 1, 14 ott 2026): the grouped list the Sky
    // draws its moments with, where these rows sat on one ground with hairlines.
    blocks.right {
        SegmentedGroup(
            segments = listOf(
                {
                    AlertRow(
                        icon = ChiaroIcons.condition(ThunderstormCode),
                        title = stringResource(R.string.alert_severe_title),
                        description = stringResource(R.string.alert_severe_desc),
                        cadence = stringResource(R.string.alert_severe_when),
                        checked = content.notifications.severeWeatherAlerts,
                        muted = rowMuted(muted, AlertKind.SEVERE, AlertKind.HEAT),
                        onChange = { actions.setSevereWeather(it); if (it) somethingTurnedOn() }
                    )
                },
                {
                    AlertRow(
                        icon = ChiaroIcons.precipitation,
                        title = stringResource(R.string.alert_precip_title),
                        description = stringResource(R.string.alert_precip_desc),
                        cadence = stringResource(R.string.alert_precip_when),
                        checked = content.notifications.precipitationWarning,
                        muted = rowMuted(muted, AlertKind.PRECIPITATION),
                        onChange = { actions.setPrecipitationWarning(it); if (it) somethingTurnedOn() }
                    )
                },
                {
                    AlertRow(
                        icon = ChiaroIcons.sunrise,
                        title = stringResource(R.string.alert_summary_title),
                        hours = stringResource(R.string.alerts_hours_range, 6, 12),
                        description = stringResource(R.string.alert_summary_desc),
                        cadence = stringResource(R.string.alert_summary_when),
                        checked = content.notifications.dailySummary,
                        muted = rowMuted(muted, AlertKind.DAILY_SUMMARY),
                        onChange = { actions.setDailySummary(it); if (it) somethingTurnedOn() }
                    )
                },
                // Immediately under its twin, and never anywhere else: the pair is the
                // point, and a reader who has just read the morning's hours is exactly
                // the reader who wants to know there is an evening one.
                {
                    AlertRow(
                        icon = ChiaroIcons.starryNight,
                        title = stringResource(R.string.alert_evening_title),
                        hours = stringResource(R.string.alerts_hours_range, 18, 23),
                        description = stringResource(R.string.alert_evening_desc),
                        cadence = stringResource(R.string.alert_evening_when),
                        checked = content.notifications.eveningSummary,
                        muted = rowMuted(muted, AlertKind.EVENING_SUMMARY),
                        onChange = { actions.setEveningSummary(it); if (it) somethingTurnedOn() }
                    )
                }
            ),
            modifier = Modifier.padding(vertical = 4.dp)
        )
    }

    // The age of what the cards read, once it is past the stale bar (15.1, 28 Oct 2026): the
    // freshness chip's own words beside the title, and only over cards — an age over an
    // empty group qualifies nothing.
    val dataAge = content.lastSync
        ?.takeIf { content.isStale && content.rules.isNotEmpty() }
        ?.let { freshnessAge(it) }
    blocks.right { GroupTitle(stringResource(R.string.alerts_group_yours), hint = dataAge) }
    // The reader's rules are CARDS (VISION §5.4), since the review of 8 set 2026:
    // as list rows they were indistinguishable from the ready-made switches above
    // them, and a row with a switch does not say "I open". Keyed, so a toggle
    // animates in place instead of the list rebuilding the row.
    content.rules.forEachIndexed { index, card ->
        blocks.right(key = card.rule.id) {
            RuleCard(
                card = card,
                shape = segmentShape(index, content.rules.size),
                units = content.units,
                zone = content.zone,
                today = placeToday,
                dateFmt = firedDateFmt,
                clock = issuedFmt,
                muted = RuleNotifier.CHANNEL_ID in muted,
                onToggle = { enabled ->
                    actions.update(card.rule.copy(enabled = enabled))
                    if (enabled) somethingTurnedOn()
                },
                onOpen = { onEdit(card.rule.id) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = SegmentGap / 2)
            )
        }
    }

    // «I tuoi» empty: one line, not a row of ideas (committente, 14 ott 2026). The ideas
    // had stayed on the page while the section was empty, as its empty state, and the
    // page changed shape at the first alert: the row vanished and the FAB was left as
    // the one way in, which it had been all along beside the row. Now the FAB is the
    // only way in from the start, and the section says what it is for instead of
    // standing as a title over nothing (DESIGN §1.1).
    if (content.canAdd && content.rules.isEmpty()) {
        blocks.right {
            Text(
                text = stringResource(R.string.alerts_yours_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
    } else if (!content.canAdd) {
        // The templates used to vanish without a word at the cap (review, 8 set), and
        // the FAB goes with them now: this sentence is what says why.
        blocks.right {
            Text(
                text = stringResource(R.string.alerts_max_reached, MaxRules),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }
    }

    // Extended while the top of the page is in view, the icon alone once the reader has
    // scrolled into the list, as the Sky's: the label is read once, then it covers rows.
    val hasAcross = blocks.across.isNotEmpty()
    val fabExpanded by remember(columns.twoPanes, hasAcross) {
        derivedStateOf { blocks.firstInView(listState, columns.twoPanes) }
    }
    // A Scaffold for the two things that float over the list (the Alerts review, points 9
    // and 10, 14 ott 2026): the FAB, and the snackbar that offers «Annulla», which the
    // Scaffold lifts the FAB over.
    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbars) },
        floatingActionButton = {
            // «Nuovo avviso» (point 9): the screen's one action, where Material puts a
            // screen's one action, instead of a row of ideas at the foot of a page five
            // screens long. At the ceiling there is nothing to add, and the sentence at the
            // foot of «I tuoi» says why.
            if (content.canAdd) {
                ExtendedFloatingActionButton(
                    text = { Text(stringResource(R.string.alerts_new_rule)) },
                    icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                    expanded = fabExpanded,
                    onClick = { ideasOpen = true },
                    // On a wide page its end on the right pane's, not in the window's corner.
                    modifier = Modifier.padding(end = columns.end)
                )
            }
        }
    ) { inner ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(inner),
            // Room for the FAB under the last row, so nothing ends up beneath it.
            contentPadding = PaddingValues(bottom = FabClearance)
        ) {
            paneItems(blocks, columns)
        }
    }
}

/**
 * «Nuovo avviso» (the Alerts review, point 9, 14 ott 2026): the FAB's sheet, as the Sky's
 * opens its catalog. «Da zero» first, because building from nothing is the other way in;
 * then the ideas, each with its drawing, its promise and what it checks. An idea already
 * turned into a rule says so and does nothing, as its card on the page does. A pick closes
 * the sheet and opens the new rule's editor, where the rest of the choosing happens.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IdeasSheet(
    existing: Set<List<RuleCondition>>,
    onPick: (RuleText.Template) -> Unit,
    onDismiss: () -> Unit
) {
    val addedState = stringResource(R.string.tpl_already_added)
    ChiaroSheet(onDismissRequest = onDismiss) {
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                IdeaRow(
                    icon = Icons.Outlined.Edit,
                    iconTint = MaterialTheme.colorScheme.primary,
                    title = stringResource(RuleText.blank.titleRes),
                    description = stringResource(RuleText.blank.descriptionRes),
                    added = false,
                    addedState = addedState,
                    onPick = { onPick(RuleText.blank) }
                )
            }
            item {
                Text(
                    text = stringResource(R.string.alerts_templates_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, top = GroupTop, bottom = SectionBottom)
                )
            }
            items(RuleText.templates.size) { index ->
                val template = RuleText.templates[index]
                IdeaRow(
                    icon = templateIcon(template),
                    title = stringResource(template.titleRes),
                    description = stringResource(template.descriptionRes),
                    added = template.conditions in existing,
                    addedState = addedState,
                    onPick = { onPick(template) }
                )
            }
        }
    }
}

/** One idea in the sheet: the catalog's row, its drawing in front and the mark after. */
@Composable
private fun IdeaRow(
    icon: ImageVector,
    title: String,
    description: String,
    added: Boolean,
    addedState: String,
    onPick: () -> Unit,
    iconTint: Color = Color.Unspecified
) {
    ListItem(
        leadingContent = {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(RowIcon))
        },
        headlineContent = { Text(title) },
        supportingContent = { Text(description) },
        trailingContent = {
            Icon(
                imageVector = if (added) Icons.Outlined.Check else Icons.Outlined.Add,
                contentDescription = null, // the row says it, below
                tint = if (added) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        // On the sheet's own ground: the item's default container is `surface`, which on
        // the sheet's lighter one striped the list row by row (rendered).
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = if (added) {
            Modifier.semantics { stateDescription = addedState }
        } else {
            Modifier.clickable(onClick = onPick)
        }
    )
}

/**
 * The head of Alerts (the Alerts review, point 7, 14 ott 2026): one card that always
 * answers. On top, the official warnings' answer: the warning in force in its level's
 * colour (point 2), or «Nessuna allerta» with the zone and the bulletin, or the reason there
 * is none to give. Under it, on the card's own ground, the window the phone may ring in
 * next, as the Sky names its next moment, and the day of windows drawn under that.
 *
 * Until then the page opened with «Quando arrivano», a diagram to decipher, and the answer
 * somebody came for stood under it in a card of its own: two things where VISION §5.4 asks
 * for one, the warnings first. The lower half is drawn only while a timed alert is on; the
 * answer always is.
 *
 * The bulletin's window is the place's, not the switch's (15.2, 28 Oct 2026): drawn only
 * where a bulletin can come ([bulletinPossible]), and once today's is in hand its window is
 * spent for today and the next one named is the next that can still ring. «Il prossimo,
 * oggi · Il bollettino · 15–17» stood under «disponibili per i luoghi in Italia» abroad, and
 * «Adesso · Il bollettino» at four over a bulletin of 14:34: a sound promised that the
 * screen knew could not come.
 */
@Composable
private fun AlertsHero(
    content: AlertsUiState.Content,
    today: LocalDate,
    now: java.time.LocalTime,
    timeFmt: DateTimeFormatter,
    onOpenSheet: () -> Unit
) {
    val windows = alertWindows(content.notifications, bulletinPossible(content.warnings))
    val spent = windows.indices.filterTo(mutableSetOf()) { windows[it].spentOn(content.warnings, today) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        WarningAnswer(
            state = content.warnings,
            today = today,
            timeFmt = timeFmt,
            onOpenSheet = onOpenSheet
        )
        if (windows.isNotEmpty()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(start = 20.dp, top = 4.dp, end = 20.dp, bottom = 20.dp)
            ) {
                nextWindow(windows.map { it.from to it.to }, now, spent)?.let { next ->
                    NextWindowRow(windows[next.index], next.whenever)
                }
                AlertDayStrip(windows = windows, now = now)
            }
        }
    }
}

/**
 * The official warnings for the active place, as the hero's answer: the warning in force
 * when there is one, and otherwise the honest state (DESIGN §8.13). This is the screen where
 * an absence is a value — "nessuna allerta per Milano, bollettino delle 15:19" is the answer
 * somebody came for — which is exactly why Today draws nothing at all in the same case (§1.1).
 *
 * One skeleton for every state: a mark and a label on a line, the answer under them, then
 * what backs it. The answer is `headlineSmall` when it is one (a warning, or none), and
 * `titleMedium` when it is only the reason there is no answer yet: «Ancora nessun
 * bollettino» is not something to read from across the room.
 */
@Composable
private fun WarningAnswer(
    state: PlaceWarningState,
    today: LocalDate,
    timeFmt: DateTimeFormatter,
    onOpenSheet: () -> Unit
) {
    val locale = currentLocale()
    val dateFmt = remember(locale) {
        DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.LONG).withLocale(locale)
    }
    val context = LocalContext.current
    val quiet = MaterialTheme.colorScheme.onSurfaceVariant
    when (state) {
        is PlaceWarningState.Current -> {
            val warnings = state.warnings
            val issued = WarningText.issued(context, warnings.issuedAt, today, timeFmt)
            val zone = WarningText.zoneLabel(context, warnings.zone)
            if (warnings.maxLevel != WarningLevel.NONE) {
                val palette = warnings.maxLevel.colors
                val days = WarningText.days(context, warnings.peakDays.map { it.date }, today)
                    .replaceFirstChar { it.titlecase(locale) }
                AnswerBlock(
                    mark = { Icon(painterResource(ChiaroIcons.warningMarkRes()), null, tint = palette.ink, modifier = Modifier.size(24.dp)) },
                    label = zone,
                    answer = WarningText.sentence(context, warnings),
                    lines = listOf(
                        AnswerLine(days, MaterialTheme.typography.titleMedium),
                        AnswerLine(stringResource(R.string.warning_banner_source, issued), MaterialTheme.typography.bodyMedium)
                    ),
                    ink = palette.ink,
                    labelInk = palette.ink,
                    ground = palette.container,
                    onClick = onOpenSheet
                )
            } else {
                AnswerBlock(
                    mark = { Icon(Icons.Outlined.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp)) },
                    label = zone,
                    answer = stringResource(R.string.warning_card_all_clear),
                    lines = listOf(
                        AnswerLine(stringResource(R.string.warning_card_none_detail, issued), MaterialTheme.typography.bodyMedium)
                    ),
                    ink = MaterialTheme.colorScheme.onSurface,
                    labelInk = quiet,
                    onClick = onOpenSheet
                )
            }
        }
        is PlaceWarningState.Stale -> QuietAnswer(
            title = stringResource(R.string.warning_card_stale),
            detail = stringResource(
                R.string.warning_card_stale_detail,
                state.issuedAt.toLocalDate().format(dateFmt)
            )
        )
        is PlaceWarningState.Waiting -> QuietAnswer(
            title = stringResource(R.string.warning_card_waiting),
            detail = stringResource(R.string.warning_card_waiting_detail)
        )
        PlaceWarningState.Unavailable -> QuietAnswer(
            title = stringResource(R.string.warning_card_unavailable),
            detail = stringResource(R.string.warning_card_unavailable_detail)
        )
    }
}

private class AnswerLine(val text: String, val style: androidx.compose.ui.text.TextStyle)

/** The three states that are waiting on something, or have no zone: a fact and its
 * reason in the quiet ink, under the label of what is missing. Not a warning colour for the
 * absence of a warning, and no chevron: there is no bulletin behind it to open. */
@Composable
private fun QuietAnswer(title: String, detail: String) {
    AnswerBlock(
        mark = {
            Icon(Icons.Outlined.Info, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
        },
        label = stringResource(R.string.alerts_group_warnings),
        answer = title,
        answerStyle = MaterialTheme.typography.titleMedium,
        lines = listOf(AnswerLine(detail, MaterialTheme.typography.bodyMedium)),
        ink = MaterialTheme.colorScheme.onSurface,
        labelInk = MaterialTheme.colorScheme.onSurfaceVariant,
        onClick = null
    )
}

/**
 * The answer's block. On a warning in force it wears the level's own pair across the card's
 * full width (point 2), so the card leads in the colour of its news and keeps its ground
 * under it for the hours; otherwise it sits on the card's ground. One sentence to TalkBack,
 * as Today's banner is, and the whole block opens the bulletin when there is one.
 */
@Composable
private fun AnswerBlock(
    mark: @Composable () -> Unit,
    label: String,
    answer: String,
    lines: List<AnswerLine>,
    ink: Color,
    labelInk: Color,
    onClick: (() -> Unit)?,
    ground: Color = Color.Transparent,
    answerStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.headlineSmall
) {
    val spoken = (listOf(answer, label) + lines.map { it.text }).joinToString(". ")
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(ground)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .clearAndSetSemantics { contentDescription = spoken }
            .padding(start = 20.dp, top = 16.dp, end = 12.dp, bottom = if (ground == Color.Transparent) 12.dp else 20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            mark()
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = labelInk,
                modifier = Modifier.weight(1f).padding(start = 10.dp)
            )
            if (onClick != null) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null, // the block is the target; the sheet says the rest
                    tint = labelInk
                )
            }
        }
        Text(
            text = answer,
            style = answerStyle,
            color = ink,
            modifier = Modifier.padding(top = 4.dp, end = 8.dp)
        )
        lines.forEach { line ->
            Text(
                text = line.text,
                style = line.style,
                color = if (ground == Color.Transparent) MaterialTheme.colorScheme.onSurfaceVariant else ink,
                modifier = Modifier.padding(end = 8.dp)
            )
        }
    }
}

/**
 * The window the phone may ring in next, in the shape of the Sky's next moment: its drawing,
 * «Il prossimo, oggi» (or «Adesso», or «domani») in the accent over its name, and its hours in
 * the column the rows of «Pronti all'uso» put theirs in.
 */
@Composable
private fun NextWindowRow(window: AlertWindow, whenever: NextWhen) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ListRow.Gap)) {
        Icon(
            imageVector = window.icon,
            contentDescription = null, // the name beside it says it
            tint = window.tint,
            modifier = Modifier.size(RowIcon)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(
                    when (whenever) {
                        NextWhen.NOW -> R.string.alerts_next_now
                        NextWhen.TODAY -> R.string.alerts_next_today
                        NextWhen.TOMORROW -> R.string.alerts_next_tomorrow
                    }
                ),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(text = window.name, style = MaterialTheme.typography.bodyLarge)
        }
        Text(
            text = stringResource(R.string.alerts_hours_range, window.from, window.to),
            style = MaterialTheme.typography.titleMedium.tabular()
        )
    }
}

/** «Avvisami da: gialla / arancione» — two chips, because it is two values and a
 * dialog for two values is a screen nobody needs. The segment under the switch's, in its
 * group, so it reads as the switch's own setting. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WarningFromRow(from: WarningLevel, onChange: (WarningLevel) -> Unit) {
    // A flow, from the group's own inset: at the row's text indent «Arancione» broke in
    // two inside its chip on a 360dp screen (rendered and looked at).
    FlowRow(
        verticalArrangement = Arrangement.Center,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = stringResource(R.string.warning_from_title),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterVertically)
        )
        listOf(
            WarningLevel.YELLOW to R.string.warning_from_yellow,
            WarningLevel.ORANGE to R.string.warning_from_orange
        ).forEach { (level, labelRes) ->
            FilterChip(
                selected = from == level,
                onClick = { onChange(level) },
                label = { Text(stringResource(labelRes)) }
            )
        }
    }
}

@Composable
private fun GroupTitle(text: String, top: Dp = SectionTop, hint: String? = null) {
    // A note beside the title, as Today's section titles carry one (15.1, 28 Oct 2026):
    // quiet, at the end, on the title's baseline side — never a label of the group.
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = top, end = 16.dp, bottom = SectionBottom)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f)
        )
        hint?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.End
            )
        }
    }
}

/**
 * A ready-made alert (design review, 23 set 2026): its drawing, what it sends, and how often.
 * The drawing fades while the switch is off, so the group says which of its rows are on
 * before any switch is read.
 *
 * [hours] stand over the switch, `titleMedium` tabular (the Alerts review, point 3, 14 ott
 * 2026): the Sky's column of times, at the row's end edge. They were the head of an accent
 * line under the description, which in Italian broke over two and three lines, and they are
 * what a reader scans this group for. The alerts that come at any hour have none to show. How
 * often closes the description, in its ink: it qualifies the sentence, it is not a heading.
 *
 * The Sky row's measures since 15.7 (28 Oct 2026, [ListRow]): 12 at the start, 12 after the
 * drawing, 8 above and below, 72 the least a row is. The drawing stays at 36 (the Sky's 51 is
 * for a column of sunrises and moons; a thermometer at 51 is a poster). The switch keeps its
 * 16 at the end: unlike the Sky's bell it has no box of air of its own.
 */
@Composable
private fun AlertRow(
    icon: ImageVector,
    title: String,
    description: String,
    cadence: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    hours: String? = null,
    iconTint: Color = Color.Unspecified,
    muted: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ListRow.Gap),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = { onChange(!checked) }, role = Role.Switch)
            .heightIn(min = ListRow.MinHeight)
            .padding(start = ListRow.Start, end = 16.dp, top = ListRow.Vertical, bottom = ListRow.Vertical)
    ) {
        // Faded while it is off — and while the phone has silenced its channel (15.3): a
        // row whose notification cannot arrive is off whatever its switch says, and the
        // card above names which channel.
        Icon(
            imageVector = icon,
            contentDescription = null, // the title beside it says the word
            tint = iconTint,
            modifier = Modifier
                .size(RowIcon)
                .alpha(if (checked && !muted) 1f else OffAlpha)
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = stringResource(R.string.alerts_row_supporting, description, cadence),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        // Over the switch and not beside the title: the text column is 180 dp at 360, and
        // «Il riepilogo del mattino» with «6–12» beside it broke in two (rendered). Over the
        // switch the hours of the group stand in one column at its end edge.
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            hours?.let {
                Text(text = it, style = MaterialTheme.typography.titleMedium.tabular())
            }
            Switch(checked = checked, onCheckedChange = null)
        }
    }
}

/**
 * Whether a bulletin can come for the place on screen: it is in a zone the issuer grades
 * (15.2, 28 Oct 2026). A zone waiting on its first bulletin, or holding yesterday's, can
 * still ring; a place outside every zone never will, whatever the switch says.
 */
private fun bulletinPossible(warnings: PlaceWarningState): Boolean =
    warnings !is PlaceWarningState.Unavailable

/**
 * The windows the timed alerts arrive in, for the ones switched on: the morning summary
 * 6–12, the bulletin 15–17, the evening one 18–23, each with the sky of their hour (§3.2, the
 * ribbon's own rule: a depiction, the words are under it) and its drawing. The bulletin's
 * only where one can come ([bulletinPossible], 15.2): the switch is a setting, the window
 * is a fact about the place.
 */
@Composable
private fun alertWindows(
    notifications: com.callbackdev.chiaro.domain.settings.NotificationSettings,
    bulletinPossible: Boolean
): List<AlertWindow> {
    val sky = com.callbackdev.chiaro.ui.theme.ChiaroTheme.sky
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val yellow = com.callbackdev.chiaro.ui.theme.ChiaroTheme.colors.warningYellow
    val quietTint = MaterialTheme.colorScheme.onSurfaceVariant
    val morning = stringResource(R.string.alert_summary_title)
    val bulletin = stringResource(R.string.alerts_window_bulletin)
    val evening = stringResource(R.string.alert_evening_title)
    return buildList {
        if (notifications.dailySummary) {
            add(AlertWindow(6, 12, sky.gradient(35.0).mid, ChiaroIcons.sunrise, morning))
        }
        if (notifications.officialWarnings && bulletinPossible) {
            // The yellow level's container on paper, its ink on a dark ground, where the
            // container is an olive the track swallows (rendered).
            add(
                AlertWindow(
                    BulletinFrom, BulletinTo, if (dark) yellow.ink else yellow.container,
                    ChiaroIcons.warning, bulletin, tint = quietTint, isBulletin = true
                )
            )
        }
        if (notifications.eveningSummary) {
            add(AlertWindow(18, 23, sky.gradient(-8.0).mid, ChiaroIcons.starryNight, evening))
        }
    }
}

/**
 * The reader's day of alerts (design review, 23 set 2026): twenty-four hours as a track, the
 * [windows] painted on it and marked with their drawings, and "now" as the Sky's line and dot.
 * It answers the question a switch list cannot: when will this phone make a sound.
 *
 * The lower half of the hero since the Alerts review (point 7, 14 ott 2026), under the next
 * window it pictures: its own card and its label «Quando arrivano» went, because the row
 * above it already says what the drawing is for. The line under it that named the alerts
 * with no window («In qualunque ora, se serve: Maltempo in arrivo · Pioggia nelle prossime
 * ore») went too (15.6, 28 Oct 2026): it repeated the titles of two switches a thumb's
 * length below, and «i tuoi avvisi» was true only of the rules with no hours of their own.
 * What stays under the track is the one sentence the drawing cannot say: the quiet hours.
 * It no longer names the dash that draws them — a legend explaining a drawing in words is
 * what the guide forbids itself (VISION §5.7); the dash is under the hours it means.
 */
@Composable
private fun AlertDayStrip(windows: List<AlertWindow>, now: java.time.LocalTime) {
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val ink = MaterialTheme.colorScheme.onSurface
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val res = LocalContext.current.resources
    val spoken = windows.joinToString(", ") { res.getString(R.string.alerts_day_window_desc, it.name, it.from, it.to) }
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    val hourStyle = MaterialTheme.typography.labelSmall.copy(color = labelColor)
    val hourLabels = remember(hourStyle) {
        listOf(0, 6, 12, 18, 24).map { it to measurer.measure(androidx.compose.ui.text.AnnotatedString("$it"), hourStyle) }
    }
    val iconPainters = windows.map { androidx.compose.ui.graphics.vector.rememberVectorPainter(it.icon) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(DayIcon + 6.dp + DayTrack + 8.dp + 14.dp)
                .semantics { contentDescription = spoken }
        ) {
            val w = size.width
            val iconPx = DayIcon.toPx()
            val top = iconPx + 6.dp.toPx()
            val trackH = DayTrack.toPx()
            fun x(hour: Float) = w * hour / 24f
            val corner = androidx.compose.ui.geometry.CornerRadius(trackH / 2f)
            drawRoundRect(track, topLeft = Offset(0f, top), size = Size(w, trackH), cornerRadius = corner)
            windows.forEachIndexed { i, win ->
                val x0 = x(win.from.toFloat())
                val x1 = x(win.to.toFloat())
                drawRoundRect(win.color, topLeft = Offset(x0, top), size = Size(x1 - x0, trackH), cornerRadius = corner)
                val cx = (x0 + x1) / 2f
                translate(left = cx - iconPx / 2f, top = 0f) {
                    with(iconPainters[i]) {
                        draw(
                            Size(iconPx, iconPx),
                            colorFilter = if (win.tint != Color.Unspecified) {
                                androidx.compose.ui.graphics.ColorFilter.tint(win.tint)
                            } else null
                        )
                    }
                }
            }
            // A tick at every whole hour, standing on the track's floor, and "now" as a thin
            // line through the track with a dot under it: the marks of the night strip on the
            // Sky (the Alerts review, point 8, 14 ott 2026), so the two strips read alike.
            val tick = DayTick.toPx()
            for (h in 1 until 24) {
                drawLine(
                    color = ink.copy(alpha = 0.3f),
                    start = Offset(x(h.toFloat()), top + trackH - tick),
                    end = Offset(x(h.toFloat()), top + trackH),
                    strokeWidth = 1.dp.toPx()
                )
            }
            val nowX = x(now.hour + now.minute / 60f)
            drawLine(
                color = ink.copy(alpha = 0.85f),
                start = Offset(nowX, top),
                end = Offset(nowX, top + trackH),
                strokeWidth = 1.5.dp.toPx()
            )
            drawCircle(ink, radius = 2.5.dp.toPx(), center = Offset(nowX, top + trackH + 3.dp.toPx()))
            // The quiet hours (23 set 2026): a hairline under the track from 22 to 7,
            // where everything above still arrives, only without a sound.
            val quietY = top + trackH + 3.dp.toPx()
            val quietStroke = 2.dp.toPx()
            listOf(0f to 7f, 22f to 24f).forEach { (from, to) ->
                drawLine(
                    labelColor.copy(alpha = 0.6f),
                    start = Offset(x(from) + quietStroke, quietY),
                    end = Offset(x(to) - quietStroke, quietY),
                    strokeWidth = quietStroke,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                        floatArrayOf(4.dp.toPx(), 4.dp.toPx())
                    )
                )
            }
            val labelTop = top + trackH + 8.dp.toPx()
            hourLabels.forEach { (h, text) ->
                val lx = (x(h.toFloat()) - text.size.width / 2f).coerceIn(0f, w - text.size.width)
                drawText(text, topLeft = Offset(lx, labelTop))
            }
        }
        Text(
            text = stringResource(R.string.alerts_day_quiet),
            style = MaterialTheme.typography.bodySmall,
            color = labelColor
        )
    }
}

private class AlertWindow(
    val from: Int,
    val to: Int,
    val color: Color,
    val icon: ImageVector,
    val name: String,
    val tint: Color = Color.Unspecified,
    val isBulletin: Boolean = false
) {
    /**
     * Whether this window has already rung for [today] (15.2, 28 Oct 2026): the bulletin's,
     * once the bulletin in hand is today's — the one alert whose arrival the screen can see.
     * A summary's window never is: the screen does not know whether the summary came.
     */
    fun spentOn(warnings: PlaceWarningState, today: LocalDate): Boolean =
        isBulletin && warnings is PlaceWarningState.Current &&
            warnings.warnings.issuedAt.toLocalDate() == today
}

/** The hours the issuer's bulletin usually comes out in (VISION §5.4): the window the strip
 * paints, the hours over the switch, the ones its sentence says. */
private const val BulletinFrom = 15
private const val BulletinTo = 17

private val DayIcon = 26.dp
private val DayTrack = 10.dp
private val DayTick = 5.dp
private val FabClearance = 88.dp

/** A template's drawing: what it is about, from the weather family. */
@Composable
private fun templateIcon(template: RuleText.Template): ImageVector = when (template.titleRes) {
    R.string.tpl_bike_title -> ChiaroIcons.wind
    R.string.tpl_ice_title -> ChiaroIcons.frost
    R.string.tpl_snow_title -> ChiaroIcons.snowfall
    R.string.tpl_run_title -> ChiaroIcons.condition(PartlyCloudyCode)
    R.string.tpl_cold_run_title -> ChiaroIcons.colder
    R.string.tpl_uv_title -> ChiaroIcons.uv
    R.string.tpl_heat_title -> ChiaroIcons.dewPoint
    else -> ChiaroIcons.cloud
}

/**
 * A rule's drawing. A rule that still checks what one of the ideas checks wears that idea's
 * drawing (the Alerts review, point 5, 14 ott 2026): «Bici», made from the idea with the
 * wind, was listed under a thermometer, and «Corsa», made from the sun among the clouds,
 * under raindrops, because the drawing came from the first condition alone. The match is
 * the one that marks an idea «già aggiunto», so the two can never disagree. Any other rule
 * takes the quantity its first condition watches — frost for a temperature that has to
 * fall to zero, the thermometer for any other.
 *
 * A temperature is the variable's kind, not a word in its id (15.9, 29 Oct 2026): a day's
 * high and low (`today.high_c`, `tomorrow.low_c`) have no «temp» in them, and fell through to
 * the cloud. The snow takes the «Neve domani» idea's drawing, so a rule made from it keeps its
 * drawing when its threshold moves.
 */
@Composable
internal fun ruleIcon(rule: NotificationRule): ImageVector {
    RuleText.templates.firstOrNull { it.conditions == rule.conditions }?.let { return templateIcon(it) }
    val first = rule.conditions.firstOrNull() ?: return ChiaroIcons.cloud
    val id = first.variable
    val temperature = RuleVariables.byId(id)?.kind == RuleVariableKind.TEMPERATURE
    return when {
        temperature && (first.op == RuleOp.LT || first.op == RuleOp.LTE) && first.threshold <= 0.0 ->
            ChiaroIcons.frost
        temperature -> ChiaroIcons.dewPoint
        "precip" in id || "rain" in id -> ChiaroIcons.precipitation
        "snow" in id -> ChiaroIcons.snowfall
        "uv" in id -> ChiaroIcons.uv
        "wind" in id || "gust" in id -> ChiaroIcons.wind
        "humidity" in id -> ChiaroIcons.humidity
        "pressure" in id -> ChiaroIcons.pressure
        "aqi" in id || "air" in id || "pm" in id -> ChiaroIcons.airQuality
        "pollen" in id -> ChiaroIcons.pollen
        else -> ChiaroIcons.cloud
    }
}

/** WMO codes for the two drawings the list borrows from the condition family. */
private const val ThunderstormCode = 95
private const val PartlyCloudyCode = 2

/** The drawing of a row of this page: 36, the family's rung under the Sky's 51 ([ListRow]). */
private val RowIcon = 36.dp
private const val OffAlpha = 0.4f

/**
 * A rule's card (VISION §5.4): its name, its sentence in words, when it last fired, and
 * its switch. The whole segment opens the editor; the switch is its own target. [zone] is
 * the place's: "last fired" is a time on this place's clock, like every other hour in the
 * app.
 *
 * A segment of the «I tuoi» group since the Alerts review (point 1, 14 ott 2026), where it
 * was a loose 24 dp card: the rows of one group look like one group, as the Sky's moments
 * do. Its type is the Sky row's (point 4): the name at `bodyLarge`, every line under it in
 * the quiet ink at `bodyMedium`, with the thresholds in full weight (point 6), so that the
 * sentence can be read down its numbers the way a day of moments is read down its times.
 * Its measures are the Sky row's too, since 15.7 (28 Oct 2026, [ListRow]), the drawing at 36.
 *
 * «Scattato l'ultima volta» says its day the way the bulletin's hour is said (15.7; the
 * review's point 11): «oggi alle 7:12», «ieri alle 7:12», the day beyond that — where the
 * card's head said «di ieri alle 14:34» and this line «27 ott, 07:12». [today] is the
 * place's, as every date on the page.
 */
@Composable
private fun RuleCard(
    card: RuleCardModel,
    shape: Shape,
    units: UnitSettings,
    zone: ZoneId,
    today: LocalDate,
    dateFmt: DateTimeFormatter,
    clock: DateTimeFormatter,
    onToggle: (Boolean) -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    muted: Boolean = false
) {
    val res = LocalContext.current.resources
    val ink = MaterialTheme.colorScheme.onSurface
    val sentence = remember(card.rule.conditions, units, res) {
        val parts = listOf(RuleText.Part(res.getString(R.string.rule_sentence_prefix) + " ", value = false)) +
            RuleText.sentenceParts(res, card.rule.conditions, units)
        buildAnnotatedString {
            parts.forEach { part ->
                if (part.value) {
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = ink)) { append(part.text) }
                } else {
                    append(part.text)
                }
            }
        }
    }
    val fired = card.lastFired?.let {
        stringResource(
            R.string.rule_last_fired,
            RuleText.firedAt(res, it.atZone(zone).toLocalDateTime(), today, clock, dateFmt)
        )
    } ?: stringResource(R.string.rule_never_fired)
    // Its hours, when it has any (26 set 2026): a rule that stays silent at night must say
    // so where it is listed, or its silence reads as a rule that does not work.
    val hours = RuleText.window(res, card.rule.window, clock)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ListRow.Gap),
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onOpen)
            .heightIn(min = ListRow.MinHeight)
            .padding(start = ListRow.Start, top = ListRow.Vertical, end = 12.dp, bottom = ListRow.Vertical)
    ) {
        // The drawing of what it watches, faded while it is off — as the ready-made
        // rows above do (design review, 23 set 2026) — and while the phone has silenced
        // the rules' channel (15.3), which is off one layer down.
        Icon(
            imageVector = ruleIcon(card.rule),
            contentDescription = null, // the name beside it says what the rule is
            tint = Color.Unspecified,
            modifier = Modifier
                .size(RowIcon)
                .alpha(if (card.rule.enabled && !muted) 1f else OffAlpha)
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(text = card.rule.name, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = sentence,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            hours?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // Whether it holds now, with the number that decides (the Alerts review, point
            // 11, 14 ott 2026): until then the card said only «Non è ancora mai scattato», and
            // whether a rule was true meant opening it and pressing «Prova adesso».
            card.now?.let { preview ->
                ruleNow(res, preview, units, clock, currentLocale(), watchesSeveral(card.rule))
                    ?.let { RuleNowLine(it) }
            }
            Text(
                text = fired,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = card.rule.enabled, onCheckedChange = onToggle)
    }
}

/** A rule's state now, as a line: whether it holds ([holds], null when it cannot be said),
 * the [word] that says it first, and the whole [text], which begins with that word. */
internal class RuleNow(val holds: Boolean?, val word: String, val text: String)

/**
 * The dry run as words (the Alerts review, points 11 and 12, 14 ott 2026): «Adesso sì»,
 * «Adesso sì, ma fuori orario fino alle 7:12», «Adesso sì, può suonare dalle 12:00» for a fact
 * about tomorrow in the morning (15.9), «Adesso no · la pioggia nelle prossime 6 ore è
 * 35%, serve al massimo 20%», or why it cannot be said. The number that decides is the one the
 * model forecast, in the reader's units, beside the threshold it missed: a «no» without them
 * left the reader to guess which threshold to move. Null with no forecast at all, where the
 * card says nothing (the editor says why, [EditorAnswer]).
 */
internal fun ruleNow(
    res: android.content.res.Resources,
    preview: RulePreview,
    units: UnitSettings,
    clock: DateTimeFormatter,
    locale: java.util.Locale,
    /** Whether to name the quantity: only when the rule watches more than one, because on
     * «la temperatura adesso sotto 5°» the line «è 19,7°, serve sotto 5°» needs no subject. */
    named: Boolean = true
): RuleNow? = when (preview) {
    is RulePreview.WouldFire -> res.getString(R.string.rule_now_yes).let { RuleNow(true, it, it) }
    is RulePreview.OutsideHours -> {
        val word = res.getString(R.string.rule_now_yes)
        val next = preview.next
        val text = when (next?.toLocalDate()) {
            preview.today -> res.getString(R.string.rule_now_closed_today, word, next.format(clock))
            preview.today.plusDays(1) -> res.getString(R.string.rule_now_closed_tomorrow, word, next.format(clock))
            else -> res.getString(R.string.rule_now_closed, word)
        }
        RuleNow(true, word, text)
    }
    is RulePreview.NotYet -> {
        val word = res.getString(R.string.rule_now_yes)
        RuleNow(true, word, res.getString(R.string.rule_now_from, word, preview.at.format(clock)))
    }
    is RulePreview.WouldPass -> {
        val word = res.getString(R.string.rule_now_no)
        val failed = preview.failed
        val value = preview.value
        val text = if (failed == null || value == null) {
            word
        } else {
            val name = res.getString(RuleText.nameRes(failed.variable))
            val reading = RuleText.reading(res, failed.variable, value, units, locale)
            if (RuleVariables.byId(failed.variable)?.kind == RuleVariableKind.BOOLEAN) {
                res.getString(R.string.rule_now_failed_flag, word, name, reading)
            } else {
                val need = res.getString(needRes(failed.op), RuleText.value(res, failed, units))
                if (named) {
                    res.getString(R.string.rule_now_failed, word, name, reading, need)
                } else {
                    res.getString(R.string.rule_now_failed_short, word, reading, need)
                }
            }
        }
        RuleNow(false, word, text)
    }
    is RulePreview.Unavailable -> {
        val word = res.getString(R.string.rule_now_unknown)
        RuleNow(null, word, res.getString(R.string.rule_now_missing, word, res.getString(RuleText.nameRes(preview.variableId))))
    }
    RulePreview.NoData -> null
}

/** What a threshold asks, as the words after «serve»: the pickers' «is at least» carries a
 * verb English cannot put after «it needs». */
private fun needRes(op: RuleOp): Int = when (op) {
    RuleOp.GT -> R.string.rule_need_above
    RuleOp.GTE -> R.string.rule_need_at_least
    RuleOp.LT -> R.string.rule_need_below
    RuleOp.LTE -> R.string.rule_need_at_most
    RuleOp.EQ -> R.string.rule_need_equal
    RuleOp.NEQ -> R.string.rule_need_not_equal
}

/** Whether a rule watches more than one quantity, so its «no» must say which one. */
internal fun watchesSeveral(rule: NotificationRule): Boolean =
    rule.conditions.map { it.variable }.distinct().size > 1

/**
 * A mark and a word before anything else (DESIGN §8.7): a full dot when the rule holds, a
 * ring when it does not or cannot be said, and the word in full ink. No green and no red:
 * «true» is not «good» here, and the Sky's verdict colours mean something this line does not.
 */
@Composable
private fun RuleNowLine(now: RuleNow, modifier: Modifier = Modifier) {
    val ink = MaterialTheme.colorScheme.onSurface
    val quiet = MaterialTheme.colorScheme.onSurfaceVariant
    Row(verticalAlignment = Alignment.Top, modifier = modifier) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(NowDot)
                .then(
                    if (now.holds == true) {
                        Modifier.background(ink, CircleShape)
                    } else {
                        Modifier.border(1.5.dp, quiet, CircleShape)
                    }
                )
        )
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = ink)) { append(now.word) }
                append(now.text.removePrefix(now.word))
            },
            style = MaterialTheme.typography.bodyMedium,
            color = quiet,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

/**
 * The editor's answer, at the head of the sheet (the Alerts review, point 12): the card's line,
 * and under it the message as it would arrive, when it would. With no forecast for the place
 * it says so, because here somebody is asking.
 */
@Composable
private fun EditorAnswer(preview: RulePreview, named: Boolean, units: UnitSettings, clock: DateTimeFormatter) {
    val res = LocalContext.current.resources
    val locale = currentLocale()
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(segmentShape(0, 1))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        val now = ruleNow(res, preview, units, clock, locale, named)
        if (now == null) {
            Text(
                text = stringResource(R.string.rule_preview_no_data),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@Column
        }
        RuleNowLine(now)
        val message = when (preview) {
            is RulePreview.WouldFire -> preview.message
            is RulePreview.OutsideHours -> preview.message
            is RulePreview.NotYet -> preview.message
            else -> null
        }
        message?.let {
            Text(
                text = stringResource(R.string.rule_preview_would_say, it),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = NowDot + 8.dp)
            )
        }
    }
}

private val NowDot = 8.dp

/** How long the answer waits for the next keystroke before it reads the rule again. */
private const val PreviewSettle = 300L

/** And how long it stands before it is read again with the clock (15.1): the cards' minute. */
private const val PreviewMinute = 60_000L

// ---------------------------------------------------------------------------------
// The editor
// ---------------------------------------------------------------------------------

/** Which picker is open, and for which of the (at most two) conditions. */
private sealed interface EditorDialog {
    data class Variable(val index: Int) : EditorDialog
    data class Operator(val index: Int) : EditorDialog
    data class Value(val index: Int) : EditorDialog
    data object Placeholder : EditorDialog

    /** The start ([end] false) or the end of the hours' band at [index]. */
    data class BandTime(val index: Int, val end: Boolean) : EditorDialog
}

/**
 * Material's text buttons carry 12dp of content padding, which would set a button's
 * label 12dp inside the sheet's own 16dp margin — and the dry run prints its answer
 * against that margin, so the question and the answer landed on two different left
 * edges (device review, 4 set). Dropped horizontally, kept vertically: the 8dp is the
 * button's own, and the 48dp touch target is Material's minimum, not this padding's.
 */
private val FlushTextButtonPadding = PaddingValues(horizontal = 0.dp, vertical = 8.dp)

/**
 * The builder (VISION §5.4): a sentence of tappable chips — variable, operator,
 * threshold — up to two more conditions, the reader's own message, and a dry run
 * that says what the rule would do right now without posting anything. Chip edits
 * persist immediately; the two text fields land when the sheet closes.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun RuleEditorSheet(
    rule: NotificationRule,
    /** The other rules' names: one of them typed here is refused (15.4, 28 Oct 2026). */
    takenNames: Collection<String>,
    units: UnitSettings,
    daylightToday: Pair<java.time.LocalTime, java.time.LocalTime>?,
    /** When the report the page keeps was fetched: a new one re-reads the answer. */
    lastSync: java.time.Instant?,
    viewModel: AlertsViewModel,
    onDismiss: () -> Unit,
    /** Deletes [NotificationRule] — the rule as the sheet shows it, its unsaved name and
     * message included, so that «Annulla» brings back what the reader was looking at. */
    onDelete: (NotificationRule) -> Unit
) {
    val res = LocalContext.current.resources
    val locale = currentLocale()
    val is24h = android.text.format.DateFormat.is24HourFormat(LocalContext.current)
    val clock = remember(locale, is24h) { Formats.timeFormatter(is24h, locale) }
    var name by rememberSaveable(rule.id) { mutableStateOf(rule.name) }
    // A name that is already another rule's is said under the field as it is typed, and on
    // closing the rule keeps the name it had (15.4, 28 Oct 2026): the name is the card's title
    // and the notification's, and two «Bici» confuse before their history does.
    val nameTaken = RuleText.isTaken(name, takenNames)
    // A TextFieldValue rather than a String: a value picked from the list lands where
    // the cursor is, and a cursor is something only the field itself knows about. It
    // starts at the end of the message, so a value picked before the field is ever
    // touched appends instead of jumping to the front.
    var message by rememberSaveable(rule.id, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(rule.message, TextRange(rule.message.length)))
    }
    var dialog by remember { mutableStateOf<EditorDialog?>(null) }
    var preview by remember { mutableStateOf<RulePreview?>(null) }
    val scroll = rememberScrollState()

    fun close() {
        val keptName = RuleText.keptName(name, rule.name, takenNames)
        if (keptName != rule.name || message.text != rule.message) {
            viewModel.update(rule.copy(name = keptName, message = message.text))
        }
        onDismiss()
    }

    /** A picked value replaces the selection, and the cursor lands after it. */
    fun insert(placeholder: String) {
        val start = message.selection.min
        val end = message.selection.max
        message = TextFieldValue(
            text = message.text.replaceRange(start, end, placeholder),
            selection = TextRange(start + placeholder.length)
        )
    }

    // The answer answers the rule on screen, every time it changes (the Alerts review, point
    // 12, 14 ott 2026): a chip, the hours, the message. It used to wait for «Prova adesso» at
    // the foot of the sheet and vanish at the next chip, so trying a threshold meant pressing
    // again and scrolling down to read it. The message changes what it would say, never
    // whether it fires; it is in the key so the quoted message is the one being typed, after a
    // breath so a word being typed is not a reading a letter.
    //
    // And again every minute, and at every report that lands (15.1, 28 Oct 2026): the card
    // under the sheet was read again on both, and the answer at its head stood still.
    LaunchedEffect(rule.conditions, rule.window, message.text, lastSync) {
        if (preview != null) kotlinx.coroutines.delay(PreviewSettle)
        while (true) {
            preview = viewModel.preview(rule.copy(message = message.text))
            kotlinx.coroutines.delay(PreviewMinute)
        }
    }

    ChiaroSheet(
        onDismissRequest = ::close,
        // A form is not a list: it opens on all of itself. Material's half-open state
        // is for content that continues past the fold, and this content does not —
        // stopping at half made the reader drag the sheet before reading it
        // (device review, 4 set).
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(scroll)
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // The answer before the controls, as the Sky's answer comes before anything to
            // change (point 12): what the rule would do now, and what it would say.
            preview?.let { EditorAnswer(it, watchesSeveral(rule), units, clock) }
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.rule_name_label)) },
                isError = nameTaken,
                supportingText = if (nameTaken) {
                    { Text(stringResource(R.string.rule_name_taken)) }
                } else {
                    null
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = stringResource(R.string.rule_sentence_prefix),
                style = MaterialTheme.typography.titleMedium
            )
            rule.conditions.forEachIndexed { index, condition ->
                ConditionChips(
                    condition = condition,
                    units = units,
                    prefixAnd = index > 0,
                    removable = index > 0,
                    onVariable = { dialog = EditorDialog.Variable(index) },
                    onOperator = { dialog = EditorDialog.Operator(index) },
                    onValue = {
                        val kind = RuleVariables.byId(condition.variable)?.kind
                        if (kind == RuleVariableKind.BOOLEAN) {
                            // Yes/no has exactly two values: the tap IS the picker.
                            viewModel.update(
                                rule.withCondition(
                                    index,
                                    condition.copy(
                                        threshold = if (condition.threshold != 0.0) 0.0 else 1.0
                                    )
                                )
                            )
                        } else {
                            dialog = EditorDialog.Value(index)
                        }
                    },
                    onRemove = {
                        viewModel.update(
                            rule.copy(conditions = rule.conditions.filterIndexed { i, _ -> i != index })
                        )
                    }
                )
            }
            if (rule.conditions.size < MaxConditions) {
                TextButton(
                    onClick = {
                        viewModel.update(
                            rule.copy(
                                conditions = rule.conditions +
                                    RuleCondition("current.temp_c", RuleOp.GTE, 20.0)
                            )
                        )
                    },
                    contentPadding = FlushTextButtonPadding
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null)
                    Text(
                        text = stringResource(R.string.rule_add_condition),
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }

            RuleHoursEditor(
                window = rule.window,
                daylightToday = daylightToday,
                clock = clock,
                onChange = { viewModel.update(rule.copy(window = it)) },
                onPickTime = { index, end -> dialog = EditorDialog.BandTime(index, end) }
            )

            // The placeholders as words on chips (15.8, 28 Oct 2026; the review's point 9):
            // the saved text keeps the ids, the field paints their phrases on a chip of
            // `secondaryContainer`, a name the registry does not know stays as typed, and a
            // backspace after a chip takes the chip whole (`PlaceholderWords.wholeDeletion`),
            // never a letter of an id nobody sees. The field used to show «Si pedala:
            // {current.temp_c} e pioggia al {next_6h.precip_chance_max}», the one place the
            // dotted ids reached a screen.
            val fieldRes = LocalContext.current.resources
            val placeholderPhrase: (String) -> String? = remember(fieldRes) {
                { name -> RuleText.placeholderPhrase(fieldRes, name) }
            }
            val chipStyle = SpanStyle(
                background = MaterialTheme.colorScheme.secondaryContainer,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            val placeholderChips = remember(placeholderPhrase, chipStyle) {
                PlaceholderTransformation(placeholderPhrase, chipStyle)
            }
            OutlinedTextField(
                value = message,
                onValueChange = { typed ->
                    message = PlaceholderWords.wholeDeletion(
                        message.text, typed.text, typed.selection.start, placeholderPhrase
                    )
                        ?.takeIf { typed.selection.collapsed }
                        ?.let { (text, cursor) -> TextFieldValue(text, TextRange(cursor)) }
                        ?: typed
                },
                visualTransformation = placeholderChips,
                label = { Text(stringResource(R.string.rule_message_label)) },
                supportingText = { Text(stringResource(R.string.rule_message_help)) },
                modifier = Modifier.fillMaxWidth()
            )
            // Every variable of the registry interpolates into the message, not only
            // the trigger's two, and until now nothing on the screen said so (asked by
            // the committente, 4 set). A list you tap is the only way this product can
            // say it: VISION §5.4 rules out a syntax to remember, and the dotted names
            // are not words it speaks out loud — so the reader picks words, and what
            // lands in the message is their own text.
            TextButton(
                onClick = { dialog = EditorDialog.Placeholder },
                contentPadding = FlushTextButtonPadding
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null)
                Text(
                    text = stringResource(R.string.rule_message_add_value),
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            TextButton(
                // No «Eliminare questo avviso?» since the Alerts review (point 10, 14 ott
                // 2026): the snackbar's «Annulla» puts it back, as the Sky's does a moment.
                onClick = {
                    onDelete(rule.copy(name = RuleText.keptName(name, rule.name, takenNames), message = message.text))
                },
                contentPadding = FlushTextButtonPadding
            ) {
                Text(
                    text = stringResource(R.string.rule_delete),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }

    when (val d = dialog) {
        is EditorDialog.Variable -> VariablePickerSheet(
            selected = rule.conditions[d.index].variable,
            onPick = { variableId ->
                val kind = RuleVariables.byId(variableId)?.kind
                val old = rule.conditions[d.index]
                val updated = if (kind == RuleVariableKind.BOOLEAN) {
                    // Booleans read "is yes": comparisons other than è/non è are nonsense.
                    old.copy(variable = variableId, op = RuleOp.EQ, threshold = 1.0)
                } else {
                    old.copy(
                        variable = variableId,
                        op = if (old.op == RuleOp.EQ || old.op == RuleOp.NEQ) RuleOp.GTE else old.op,
                        threshold = RuleText.inRange(variableId, old.threshold, units)
                    )
                }
                viewModel.update(rule.withCondition(d.index, updated))
                dialog = null
            },
            onDismiss = { dialog = null }
        )
        is EditorDialog.Operator -> OperatorPickerDialog(
            boolean = RuleVariables.byId(rule.conditions[d.index].variable)?.kind ==
                RuleVariableKind.BOOLEAN,
            selected = rule.conditions[d.index].op,
            onPick = { op ->
                viewModel.update(rule.withCondition(d.index, rule.conditions[d.index].copy(op = op)))
                dialog = null
            },
            onDismiss = { dialog = null }
        )
        is EditorDialog.Value -> ValuePickerDialog(
            condition = rule.conditions[d.index],
            units = units,
            onPick = { threshold ->
                viewModel.update(
                    rule.withCondition(d.index, rule.conditions[d.index].copy(threshold = threshold))
                )
                dialog = null
            },
            onDismiss = { dialog = null }
        )
        EditorDialog.Placeholder -> PlaceholderPickerDialog(
            units = units,
            onPick = { placeholder ->
                insert(placeholder)
                dialog = null
            },
            onDismiss = { dialog = null }
        )
        is EditorDialog.BandTime -> {
            val band = rule.window.bands[d.index]
            BandTimeDialog(
                title = stringResource(if (d.end) R.string.rule_band_pick_to else R.string.rule_band_pick_from),
                initial = if (d.end) band.toTime else band.fromTime,
                other = if (d.end) band.fromTime else band.toTime,
                is24h = is24h,
                onPick = { time ->
                    val picked = if (d.end) {
                        RuleBand.of(band.fromTime, time)
                    } else {
                        RuleBand.of(time, band.toTime)
                    }
                    viewModel.update(
                        rule.copy(
                            window = rule.window.copy(
                                bands = rule.window.bands.mapIndexed { i, b -> if (i == d.index) picked else b }
                            )
                        )
                    )
                    dialog = null
                },
                onDismiss = { dialog = null }
            )
        }
        null -> Unit
    }
}

private fun NotificationRule.withCondition(index: Int, condition: RuleCondition): NotificationRule =
    copy(conditions = conditions.mapIndexed { i, c -> if (i == index) condition else c })

/**
 * «In che orari» (26 set 2026, committente): when the rule may speak. Three chips — any
 * time, in daylight, set hours — and, for the last, one or two bands of two time chips
 * each, pickers like every other value here. The line under the chips says what the
 * choice does; for daylight it says today's sunrise and sunset at the place, because «con
 * la luce» is a promise about an hour and the reader should see which one.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RuleHoursEditor(
    window: RuleWindow,
    daylightToday: Pair<java.time.LocalTime, java.time.LocalTime>?,
    clock: DateTimeFormatter,
    onChange: (RuleWindow) -> Unit,
    onPickTime: (index: Int, end: Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(R.string.rule_window_title),
            style = MaterialTheme.typography.titleMedium
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                RuleWindowKind.ALWAYS to R.string.rule_window_always,
                RuleWindowKind.DAYLIGHT to R.string.rule_window_daylight,
                RuleWindowKind.HOURS to R.string.rule_window_hours
            ).forEach { (kind, labelRes) ->
                FilterChip(
                    selected = window.kind == kind,
                    onClick = {
                        if (window.kind != kind) {
                            // The bands survive a trip to another kind; a first visit to
                            // «Fasce orarie» starts from the waking day, to be narrowed.
                            val bands = if (kind == RuleWindowKind.HOURS && window.bands.isEmpty()) {
                                listOf(RuleWindow.FirstBand)
                            } else {
                                window.bands
                            }
                            onChange(RuleWindow(kind, bands))
                        }
                    },
                    label = { Text(stringResource(labelRes)) }
                )
            }
        }
        if (window.kind == RuleWindowKind.HOURS) {
            window.bands.forEachIndexed { index, band ->
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (index > 0) {
                        Text(
                            text = stringResource(R.string.rule_and),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                    Text(
                        text = stringResource(R.string.rule_band_from),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    AssistChip(
                        onClick = { onPickTime(index, false) },
                        label = { Text(band.fromTime.format(clock)) }
                    )
                    Text(
                        text = stringResource(R.string.rule_band_to),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    AssistChip(
                        onClick = { onPickTime(index, true) },
                        label = { Text(band.toTime.format(clock)) }
                    )
                    if (window.bands.size > 1) {
                        IconButton(
                            onClick = {
                                onChange(window.copy(bands = window.bands.filterIndexed { i, _ -> i != index }))
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = stringResource(R.string.rule_band_remove)
                            )
                        }
                    }
                }
            }
            if (window.bands.size < RuleWindow.MaxBands) {
                TextButton(
                    onClick = {
                        // The second band starts where a commute's evening does.
                        onChange(window.copy(bands = window.bands + SecondBand))
                    },
                    contentPadding = FlushTextButtonPadding
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null)
                    Text(
                        text = stringResource(R.string.rule_band_add),
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }
        Text(
            text = when (window.kind) {
                RuleWindowKind.ALWAYS -> stringResource(R.string.rule_window_always_help)
                RuleWindowKind.DAYLIGHT -> daylightToday?.let { (rise, set) ->
                    stringResource(R.string.rule_window_daylight_today, rise.format(clock), set.format(clock))
                } ?: stringResource(R.string.rule_window_daylight_help)
                RuleWindowKind.HOURS -> stringResource(R.string.rule_window_hours_help)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** What «e un'altra fascia» adds: the evening of a commute, 17:00-19:00. */
private val SecondBand = RuleBand(17 * 60, 19 * 60)

/**
 * One end of a band, on Material's clock (26 set 2026). The confirm button refuses the
 * other end's own time: a band that starts where it ends is no hours at all, and a rule
 * silenced for ever by a slip of the finger is not writable here (VISION §5.4).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BandTimeDialog(
    title: String,
    initial: java.time.LocalTime,
    other: java.time.LocalTime,
    is24h: Boolean,
    onPick: (java.time.LocalTime) -> Unit,
    onDismiss: () -> Unit
) {
    val state = rememberTimePickerState(
        initialHour = initial.hour,
        initialMinute = initial.minute,
        is24Hour = is24h
    )
    val picked = java.time.LocalTime.of(state.hour, state.minute)
    val same = picked == other
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TimePicker(state = state)
                if (same) {
                    Text(
                        text = stringResource(R.string.rule_band_same),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onPick(picked) }, enabled = !same) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

/** One condition as its three chips, each a door to a picker — never a text field. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ConditionChips(
    condition: RuleCondition,
    units: UnitSettings,
    prefixAnd: Boolean,
    removable: Boolean,
    onVariable: () -> Unit,
    onOperator: () -> Unit,
    onValue: () -> Unit,
    onRemove: () -> Unit
) {
    val res = LocalContext.current.resources
    val boolean = RuleVariables.byId(condition.variable)?.kind == RuleVariableKind.BOOLEAN
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        if (prefixAnd) {
            Text(
                text = stringResource(R.string.rule_and),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
        AssistChip(
            onClick = onVariable,
            label = { Text(stringResource(RuleText.nameRes(condition.variable))) }
        )
        AssistChip(
            onClick = onOperator,
            label = { Text(stringResource(RuleText.opRes(condition.op, boolean))) }
        )
        AssistChip(
            onClick = onValue,
            label = { Text(RuleText.value(res, condition, units)) }
        )
        if (removable) {
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.rule_remove_condition)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------
// The pickers
// ---------------------------------------------------------------------------------

/**
 * The values a message can print. `RuleMessages` resolves every name the registry
 * knows, not only the two the trigger carries, and this is where the reader finds
 * that out: the list is words, and the tap writes the name into their own message.
 * Picked, never typed — VISION §5.4 rules out a syntax to remember, and a screen of
 * dotted ids would be the jargon this product does not speak.
 */
@Composable
private fun PlaceholderPickerDialog(
    units: UnitSettings,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    // Every variable but the yes/no ones: those interpolate as `true`/`false`, which
    // is neither a number nor a word this product says — offering them would drop a
    // code word into the reader's own sentence. A rule can still watch them.
    val printable = remember {
        RuleVariables.all.filter { it.kind != RuleVariableKind.BOOLEAN }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rule_pick_placeholder)) },
        text = {
            LazyColumn {
                item {
                    Text(
                        text = stringResource(R.string.rule_placeholder_intro),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                item {
                    PlaceholderRow(stringResource(R.string.var_trigger_value)) {
                        onPick(RuleMessages.placeholder(RuleMessages.TriggerValue))
                    }
                }
                item {
                    PlaceholderRow(stringResource(R.string.var_trigger_time)) {
                        onPick(RuleMessages.placeholder(RuleMessages.TriggerTime))
                    }
                }
                items(printable.size) { index ->
                    val variable = printable[index]
                    // The displayed name, so the message says the unit the number will
                    // arrive in; the engine resolves either spelling of it.
                    val name = RuleVariables.displayId(variable, units)
                    PlaceholderRow(stringResource(RuleText.nameRes(variable.id))) {
                        onPick(RuleMessages.placeholder(name))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

/** One offered value: a whole row is the target, 48dp of it. */
@Composable
private fun PlaceholderRow(name: String, onPick: () -> Unit) {
    Text(
        text = name,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPick)
            .padding(vertical = 12.dp)
    )
}

/**
 * «Cosa guardare» as a sheet (15.7, 28 Oct 2026): the registry's variables in the four
 * groups a reader thinks in — now, the next six hours, the next twelve, today
 * ([RuleText.variableGroups]) — in the grammar of the ideas' sheet: a heading per group
 * in the accent, the catalog's rows on the sheet's own ground, the one chosen marked with
 * the check the catalog marks a subscribed moment with. It was a dialog of thirty-one radio
 * buttons in the registry's order, a list with no landmark to scroll by. The question stays
 * at its head: the chip that opens it says the variable, not what the list is for.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VariablePickerSheet(
    selected: String,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ChiaroSheet(onDismissRequest = onDismiss) {
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                Text(
                    text = stringResource(R.string.rule_pick_variable),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp)
                )
            }
            RuleText.variableGroups.forEach { group ->
                item {
                    Text(
                        text = stringResource(group.titleRes),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 16.dp, top = GroupTop, bottom = SectionBottom)
                    )
                }
                items(group.ids.size) { index ->
                    val id = group.ids[index]
                    val chosen = id == selected
                    ListItem(
                        headlineContent = { Text(stringResource(RuleText.nameRes(id))) },
                        trailingContent = if (chosen) {
                            {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = null, // the row's selected state says it
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        } else {
                            null
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier
                            .clickable { onPick(id) }
                            .semantics { this.selected = chosen }
                    )
                }
            }
        }
    }
}

@Composable
private fun OperatorPickerDialog(
    boolean: Boolean,
    selected: RuleOp,
    onPick: (RuleOp) -> Unit,
    onDismiss: () -> Unit
) {
    // A yes/no reads "is" or "is not"; a continuous quantity never "equals" a threshold
    // — "temperature equal to 20°" is a rule that all but never fires, the nonsense
    // threshold the pickers exist to make unwritable (review, 8 set 2026). A rule that
    // already carries one keeps it; the picker just stops offering it.
    val options = if (boolean) {
        listOf(RuleOp.EQ, RuleOp.NEQ)
    } else {
        RuleOp.entries.filter { it != RuleOp.EQ && it != RuleOp.NEQ }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rule_pick_operator)) },
        text = {
            Column {
                options.forEach { op ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(op) }
                            .padding(vertical = 10.dp)
                    ) {
                        RadioButton(selected = op == selected, onClick = null)
                        Text(
                            text = stringResource(RuleText.opRes(op, boolean)),
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

/**
 * The threshold on a slider over the variable's honest range in the reader's units
 * (15.5, 28 Oct 2026), stored canonical: the rule keeps its °C, km/h and hPa whatever the
 * setting says, so the same value never rewrites itself when the unit changes (the
 * engine's own rule), and the slider's grid is the unit's own ([RuleText.valueSpec]).
 */
@Composable
private fun ValuePickerDialog(
    condition: RuleCondition,
    units: UnitSettings,
    onPick: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    val res = LocalContext.current.resources
    val kind = RuleVariables.byId(condition.variable)?.kind ?: RuleVariableKind.NUMBER
    val spec = RuleText.valueSpec(condition.variable, units)
    // The position in the reader's units, null until the thumb moves: a threshold the
    // grid does not hold (saved in another unit, or before the grid was this one) reads
    // as it is, «44,7 mph», and leaves as it came unless the reader moves it. A look
    // rewrites nothing.
    var moved by remember { mutableStateOf<Double?>(null) }
    val threshold = moved?.let { RuleVariables.canonicalValue(kind, it, units) } ?: condition.threshold
    val shown = moved ?: RuleVariables.displayValue(kind, condition.threshold, units)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rule_pick_value)) },
        text = {
            Column {
                Text(
                    text = RuleText.value(res, condition.copy(threshold = threshold), units),
                    style = MaterialTheme.typography.headlineSmall
                )
                Slider(
                    value = shown.coerceIn(spec.min, spec.max).toFloat(),
                    // Snapped to the step so the label never shows a value the thumb
                    // cannot come back to, with the ticks drawn or not.
                    onValueChange = { moved = RuleText.snap(spec, it.toDouble()) },
                    valueRange = spec.min.toFloat()..spec.max.toFloat(),
                    steps = RuleText.ticks(spec)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onPick(threshold) }) {
                Text(stringResource(R.string.action_done))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

/**
 * Whether anything on this screen has promised the reader a notification: one of the
 * ready-made switches, the official warnings, or a rule of their own that is enabled.
 *
 * This is the question the card is drawn on, and it is the whole reason the card
 * exists: four of these ship switched **on**, so a fresh install carries four promises
 * before anybody has touched anything — and the permission used to be asked only by
 * the act of switching one on, which that install never does.
 *
 * A rule's own switch is the whole of its promise (15.6, 28 Oct 2026): the master switch
 * over the reader's rules went, having never had a control on any screen. A rule whose
 * place is gone promises nothing (15.11): it is read nowhere, and the job switches it off.
 */
internal fun notificationsPromised(content: AlertsUiState.Content): Boolean =
    with(content.notifications) {
        severeWeatherAlerts || precipitationWarning || dailySummary || eveningSummary ||
            officialWarnings || content.rules.any { it.canRing }
    }
