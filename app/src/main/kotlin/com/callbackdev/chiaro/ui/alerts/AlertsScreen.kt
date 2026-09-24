package com.callbackdev.chiaro.ui.alerts

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import com.callbackdev.chiaro.ui.icons.ChiaroIcons
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.callbackdev.chiaro.ui.format.currentLocale
import com.callbackdev.chiaro.ui.theme.GroupTop
import com.callbackdev.chiaro.ui.theme.SectionBottom
import com.callbackdev.chiaro.ui.theme.SectionTop
import com.callbackdev.chiaro.ui.theme.reducedMotion
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.ui.components.NotificationsOffCard
import com.callbackdev.chiaro.ui.components.rememberNotificationRequest
import com.callbackdev.chiaro.ui.components.rememberNotificationsAllowed
import com.callbackdev.chiaro.data.warnings.PlaceWarningState
import com.callbackdev.chiaro.domain.rules.MaxConditions
import com.callbackdev.chiaro.domain.rules.MaxRules
import com.callbackdev.chiaro.domain.rules.NotificationRule
import com.callbackdev.chiaro.domain.rules.RuleCondition
import com.callbackdev.chiaro.domain.rules.RuleMessages
import com.callbackdev.chiaro.domain.rules.RuleOp
import com.callbackdev.chiaro.domain.rules.RuleVariableKind
import com.callbackdev.chiaro.domain.rules.RuleVariables
import com.callbackdev.chiaro.domain.settings.UnitSettings
import com.callbackdev.chiaro.domain.warnings.WarningLevel
import com.callbackdev.chiaro.ui.places.PlacesSheet
import com.callbackdev.chiaro.ui.places.PlacesViewModel
import com.callbackdev.chiaro.ui.warnings.WarningText
import com.callbackdev.chiaro.ui.warnings.WarningSheet
import com.callbackdev.chiaro.ui.warnings.WarningBanner
import com.callbackdev.chiaro.ui.format.Formats
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

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            AlertsHeader(
                placeName = (state as? AlertsUiState.Content)?.placeName,
                onOpenPlaces = { placesOpen = true },
                onOpenSettings = onOpenSettings
            )
            (state as? AlertsUiState.Content)?.let { content ->
                AlertsContent(
                    content = content,
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
                    onEdit = { editingRuleId = it }
                )
            }
        }
    }

    if (placesOpen) {
        PlacesSheet(viewModel = placesViewModel, onDismiss = { placesOpen = false })
    }

    val content = state as? AlertsUiState.Content
    val editing = content?.rules?.firstOrNull { it.rule.id == editingRuleId }?.rule
    if (editing != null && content != null) {
        RuleEditorSheet(
            rule = editing,
            units = content.units,
            viewModel = alertsViewModel,
            onDismiss = { editingRuleId = null }
        )
    }
}

@Composable
private fun AlertsHeader(
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
            Text(stringResource(R.string.tab_alerts), style = MaterialTheme.typography.titleLarge)
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
    onEdit: (Long) -> Unit
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
    val firedFmt = remember(locale, is24h) {
        DateTimeFormatter.ofPattern(if (is24h) "d MMM, HH:mm" else "d MMM, h:mm a", locale)
    }

    // The active place's own hour, like every other hour in the app.
    val issuedFmt = remember(locale, is24h) { Formats.timeFormatter(is24h, locale) }
    var warningSheetOpen by rememberSaveable { mutableStateOf(false) }
    val current = content.warnings as? PlaceWarningState.Current
    // The place's own day, like the "last fired" hour above it.
    val placeToday = LocalDate.now(content.zone)
    if (warningSheetOpen && current != null) {
        WarningSheet(
            warnings = current.warnings,
            today = placeToday,
            timeFmt = issuedFmt,
            onDismiss = { warningSheetOpen = false }
        )
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        // Above everything, and only while the screen would otherwise be lying: the
        // switches below say an alert will arrive, and with notifications off none of
        // them can. With nothing switched on there is no promise to break and no card.
        if (!notificationsAllowed && notificationsPromised(content)) {
            item {
                NotificationsOffCard(
                    body = stringResource(R.string.notifications_off_alerts),
                    onAllow = request.ask,
                    settingsOnly = request.settingsOnly,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
        // When the phone may ring (design review, 23 set 2026): the alerts that come in a
        // window of the day, drawn on the day. Only while at least one of them is on.
        if (content.notifications.dailySummary || content.notifications.eveningSummary ||
            content.notifications.officialWarnings
        ) {
            item {
                AlertDayStrip(
                    notifications = content.notifications,
                    ownRules = content.rules.any { it.rule.enabled },
                    now = java.time.LocalTime.now(content.zone),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
        // VISION §5.4 and Fase 11: the authority's warnings lead, because they are the
        // only ones on this screen nobody chose — and because the reader who opens
        // Avvisi in an autumn afternoon is usually here to check exactly this.
        item { GroupTitle(stringResource(R.string.alerts_group_warnings)) }
        item {
            OfficialWarningCard(
                state = content.warnings,
                today = placeToday,
                timeFmt = issuedFmt,
                onOpenSheet = { warningSheetOpen = true },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
        // The switch is a setting, not a property of the place on screen: it stays put
        // when the active place is abroad, because hiding a setting behind today's
        // choice of city is how a setting becomes unfindable. The card above is what
        // says whether this place has a zone at all.
        //
        // In a group of its own with the level it starts from (design review, 23 set
        // 2026): the level is a property of the switch, and as two loose rows under a
        // row they read as three settings.
        item {
            SwitchGroup(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                AlertRow(
                    icon = ChiaroIcons.warning,
                    iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                    title = stringResource(R.string.warning_switch_title),
                    description = stringResource(R.string.warning_switch_desc),
                    cadence = stringResource(R.string.warning_switch_when),
                    checked = content.notifications.officialWarnings,
                    onChange = { actions.setOfficialWarnings(it); if (it) somethingTurnedOn() }
                )
                // The other honest road to the same choice: the two channels let the
                // system silence the yellow, this lets the app never send it (§8.13).
                if (content.notifications.officialWarnings) {
                    WarningFromRow(
                        from = content.notifications.officialWarningsFrom,
                        onChange = actions.setOfficialWarningsFrom
                    )
                }
            }
        }

        item { GroupTitle(stringResource(R.string.alerts_group_ready)) }
        // One group, four rows with their own drawing (design review, 23 set 2026): four
        // loose list items of three or four lines each were the densest block of text in
        // the app, and nothing on them said which was which before it was read.
        item {
            SwitchGroup(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                AlertRow(
                    icon = ChiaroIcons.condition(ThunderstormCode),
                    title = stringResource(R.string.alert_severe_title),
                    description = stringResource(R.string.alert_severe_desc),
                    cadence = stringResource(R.string.alert_severe_when),
                    checked = content.notifications.severeWeatherAlerts,
                    onChange = { actions.setSevereWeather(it); if (it) somethingTurnedOn() }
                )
                GroupDivider()
                AlertRow(
                    icon = ChiaroIcons.precipitation,
                    title = stringResource(R.string.alert_precip_title),
                    description = stringResource(R.string.alert_precip_desc),
                    cadence = stringResource(R.string.alert_precip_when),
                    checked = content.notifications.precipitationWarning,
                    onChange = { actions.setPrecipitationWarning(it); if (it) somethingTurnedOn() }
                )
                GroupDivider()
                AlertRow(
                    icon = ChiaroIcons.sunrise,
                    title = stringResource(R.string.alert_summary_title),
                    description = stringResource(R.string.alert_summary_desc),
                    cadence = stringResource(R.string.alert_summary_when),
                    checked = content.notifications.dailySummary,
                    onChange = { actions.setDailySummary(it); if (it) somethingTurnedOn() }
                )
                GroupDivider()
                // Immediately under its twin, and never anywhere else: the pair is the
                // point, and a reader who has just read "tra le 6 e le 12" is exactly the
                // reader who wants to know there is an evening one.
                AlertRow(
                    icon = ChiaroIcons.starryNight,
                    title = stringResource(R.string.alert_evening_title),
                    description = stringResource(R.string.alert_evening_desc),
                    cadence = stringResource(R.string.alert_evening_when),
                    checked = content.notifications.eveningSummary,
                    onChange = { actions.setEveningSummary(it); if (it) somethingTurnedOn() }
                )
            }
        }

        item { GroupTitle(stringResource(R.string.alerts_group_yours)) }
        // The reader's rules are CARDS (VISION §5.4), since the review of 8 set 2026:
        // as list rows they were indistinguishable from the ready-made switches above
        // them, and a row with a switch does not say "I open". Keyed, so a toggle
        // animates in place instead of the list rebuilding the row.
        items(content.rules.size, key = { content.rules[it].rule.id }) { index ->
            val card = content.rules[index]
            RuleCard(
                card = card,
                units = content.units,
                zone = content.zone,
                firedFmt = firedFmt,
                onToggle = { enabled ->
                    actions.update(card.rule.copy(enabled = enabled))
                    if (enabled) somethingTurnedOn()
                },
                onOpen = { onEdit(card.rule.id) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        if (content.canAdd) {
            item {
                Text(
                    text = stringResource(R.string.alerts_templates_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(
                        start = 16.dp, top = GroupTop, bottom = SectionBottom
                    )
                )
            }
            // A template whose rule already exists — same conditions, whatever the
            // reader renamed it — is marked as added rather than offered again: tapping
            // it a second time made two identical "Bike" rules (review, 8 set 2026).
            //
            // A row of cards to browse sideways since the design review of 23 set 2026:
            // five more list rows at the foot of the page read as five more settings,
            // and an idea is something you pick up, not something you configure.
            val existing = content.rules.map { it.rule.conditions }.toSet()
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    items(RuleText.templates.size) { index ->
                        val template = RuleText.templates[index]
                        val added = template.conditions in existing
                        IdeaCard(
                            icon = templateIcon(template),
                            title = stringResource(template.titleRes),
                            description = stringResource(template.descriptionRes),
                            added = added,
                            onAdd = {
                                actions.addFromTemplate(template) { created ->
                                    somethingTurnedOn()
                                    onEdit(created.id)
                                }
                            }
                        )
                    }
                }
            }
        } else {
            // The templates used to vanish without a word at the cap (review, 8 set).
            item {
                Text(
                    text = stringResource(R.string.alerts_max_reached, MaxRules),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        }
    }
}

/**
 * The official warnings for the active place: the banner when there is one, and
 * otherwise the honest state (DESIGN §8.13). This is the screen where an absence is a
 * value — "nessuna allerta per Milano, bollettino delle 15:19" is the answer somebody
 * came for — which is exactly why Today draws nothing at all in the same case (§1.1).
 *
 * Since the design review of 23 set 2026 the absence is said like an answer: a mark, the
 * two words at `titleMedium`, the zone on its own line and the bulletin's hour under it,
 * where it used to be one `titleSmall` sentence with the zone's long name folded into it.
 */
@Composable
private fun OfficialWarningCard(
    state: PlaceWarningState,
    today: LocalDate,
    timeFmt: DateTimeFormatter,
    onOpenSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val locale = currentLocale()
    val dateFmt = remember(locale) {
        DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.LONG).withLocale(locale)
    }
    val context = LocalContext.current
    when (state) {
        is PlaceWarningState.Current -> {
            val warnings = state.warnings
            if (warnings.maxLevel != WarningLevel.NONE) {
                WarningBanner(
                    warnings = warnings,
                    today = today,
                    timeFmt = timeFmt,
                    onOpen = onOpenSheet,
                    modifier = modifier
                )
            } else {
                StatusCard(
                    icon = Icons.Outlined.CheckCircle,
                    iconTint = MaterialTheme.colorScheme.primary,
                    title = stringResource(R.string.warning_card_all_clear),
                    lines = listOf(
                        WarningText.zoneLabel(context, warnings.zone),
                        stringResource(
                            R.string.warning_card_none_detail,
                            WarningText.issued(context, warnings.issuedAt, today, timeFmt)
                        )
                    ),
                    onClick = onOpenSheet,
                    modifier = modifier
                )
            }
        }
        is PlaceWarningState.Stale -> StatusCard(
            icon = Icons.Outlined.Info,
            title = stringResource(R.string.warning_card_stale),
            lines = listOf(
                stringResource(
                    R.string.warning_card_stale_detail,
                    state.issuedAt.toLocalDate().format(dateFmt)
                )
            ),
            onClick = null,
            modifier = modifier
        )
        is PlaceWarningState.Waiting -> StatusCard(
            icon = Icons.Outlined.Info,
            title = stringResource(R.string.warning_card_waiting),
            lines = listOf(stringResource(R.string.warning_card_waiting_detail)),
            onClick = null,
            modifier = modifier
        )
        PlaceWarningState.Unavailable -> StatusCard(
            icon = Icons.Outlined.Info,
            title = stringResource(R.string.warning_card_unavailable),
            lines = listOf(stringResource(R.string.warning_card_unavailable_detail)),
            onClick = null,
            modifier = modifier
        )
    }
}

/** The states the banner must never draw: a fact and its reason, on the neutral ground the
 * details tiles use — not a warning colour for the absence of a warning. A mark in front,
 * in `primary` for the all-clear and in quiet ink for the three that are waiting on
 * something; a chevron when the card opens the bulletin. */
@Composable
private fun StatusCard(
    icon: ImageVector,
    title: String,
    lines: List<String>,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = GroupShape,
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(28.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                lines.forEachIndexed { i, line ->
                    Text(
                        text = line,
                        style = if (i == 0) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (onClick != null) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null, // the card is the target; the sheet says the rest
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** «Avvisami da: gialla / arancione» — two chips, because it is two values and a
 * dialog for two values is a screen nobody needs. Inside the switch's group, indented
 * to the row's text, so it reads as the switch's own setting. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WarningFromRow(from: WarningLevel, onChange: (WarningLevel) -> Unit) {
    // A flow, from the group's own inset: at the row's text indent «Arancione» broke in
    // two inside its chip on a 360dp screen (rendered and looked at).
    FlowRow(
        verticalArrangement = Arrangement.Center,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
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
private fun GroupTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(start = 16.dp, top = SectionTop, bottom = SectionBottom)
    )
}

/** Rows that belong together, on one rounded ground (design review, 23 set 2026). */
@Composable
private fun SwitchGroup(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = GroupShape,
        modifier = modifier.fillMaxWidth()
    ) {
        Column { content() }
    }
}

/** The hairline between two rows of a group, starting where their text starts. */
@Composable
private fun GroupDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
        modifier = Modifier.padding(start = RowTextIndent, end = 16.dp)
    )
}

/**
 * A ready-made alert (design review, 23 set 2026): its drawing, what it sends, and —
 * on a line of its own, in the accent — when and how often, which used to be the tail of
 * a four-line sentence. The drawing fades while the switch is off, so the group says
 * which of its rows are on before any switch is read.
 */
@Composable
private fun AlertRow(
    icon: ImageVector,
    title: String,
    description: String,
    cadence: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    iconTint: Color = Color.Unspecified
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RowGap),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = { onChange(!checked) }, role = Role.Switch)
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 14.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null, // the title beside it says the word
            tint = iconTint,
            modifier = Modifier
                .size(RowIcon)
                .alpha(if (checked) 1f else OffAlpha)
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = cadence,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

/**
 * One idea to start from, as a card in a sideways row (design review, 23 set 2026): its
 * drawing, its promise and what it checks, and the one action. A card already turned into
 * a rule says so in the accent and does nothing, as the row it replaced did.
 */
@Composable
private fun IdeaCard(
    icon: ImageVector,
    title: String,
    description: String,
    added: Boolean,
    onAdd: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = GroupShape,
        modifier = Modifier
            .width(IdeaWidth)
            .height(IdeaHeight)
            .then(if (added) Modifier else Modifier.clickable(onClick = onAdd))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(RowIcon))
            Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(
                    imageVector = if (added) Icons.Outlined.Check else Icons.Outlined.Add,
                    contentDescription = null, // the label beside it says it
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = stringResource(if (added) R.string.tpl_already_added else R.string.tpl_add),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * The reader's day of alerts (design review, 23 set 2026): twenty-four hours as a track,
 * and on it the windows the timed alerts arrive in — the morning summary 6–12, the
 * bulletin 15–17, the evening one 18–23 — each painted with the sky of its hour (§3.2,
 * the ribbon's own rule: a depiction, the words are under it) and marked with its drawing,
 * and "now" as a disc. The alerts that arrive whenever the weather does are named on a
 * line under it rather than drawn, because they have no window to draw.
 *
 * It answers the question a switch list cannot: when will this phone make a sound.
 */
@Composable
private fun AlertDayStrip(
    notifications: com.callbackdev.chiaro.domain.settings.NotificationSettings,
    ownRules: Boolean,
    now: java.time.LocalTime,
    modifier: Modifier = Modifier
) {
    val sky = com.callbackdev.chiaro.ui.theme.ChiaroTheme.sky
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val ink = MaterialTheme.colorScheme.onSurface
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val windows = buildList {
        if (notifications.dailySummary) {
            add(AlertWindow(6, 12, sky.gradient(35.0).mid, ChiaroIcons.sunrise, stringResource(R.string.alert_summary_title)))
        }
        if (notifications.officialWarnings) {
            add(
                AlertWindow(
                    // The yellow level's container on paper, its ink on a dark ground,
                    // where the container is an olive the track swallows (rendered).
                    15, 17, com.callbackdev.chiaro.ui.theme.ChiaroTheme.colors.warningYellow.let {
                        if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) it.ink else it.container
                    },
                    ChiaroIcons.warning, stringResource(R.string.warning_switch_title),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
        if (notifications.eveningSummary) {
            add(AlertWindow(18, 23, sky.gradient(-8.0).mid, ChiaroIcons.starryNight, stringResource(R.string.alert_evening_title)))
        }
    }
    val anytime = listOfNotNull(
        stringResource(R.string.alert_severe_title).takeIf { notifications.severeWeatherAlerts },
        stringResource(R.string.alert_precip_title).takeIf { notifications.precipitationWarning },
        stringResource(R.string.alerts_day_yours).takeIf { notifications.userRules && ownRules }
    )
    val res = LocalContext.current.resources
    val spoken = windows.joinToString(", ") { res.getString(R.string.alerts_day_window_desc, it.name, it.from, it.to) }
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    val hourStyle = MaterialTheme.typography.labelSmall.copy(color = labelColor)
    val hourLabels = remember(hourStyle) {
        listOf(0, 6, 12, 18, 24).map { it to measurer.measure(androidx.compose.ui.text.AnnotatedString("$it"), hourStyle) }
    }
    val iconPainters = windows.map { androidx.compose.ui.graphics.vector.rememberVectorPainter(it.icon) }
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = GroupShape,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.alerts_day_title),
                style = MaterialTheme.typography.titleSmall
            )
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
                // Now, as the disc the daylight ribbon uses.
                val nowX = x(now.hour + now.minute / 60f)
                val r = trackH * 0.9f
                drawCircle(ink, radius = r, center = Offset(nowX, top + trackH / 2f))
                drawCircle(track, radius = r - 2.dp.toPx(), center = Offset(nowX, top + trackH / 2f))
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
            if (anytime.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.alerts_day_anytime, anytime.joinToString(" · ")),
                    style = MaterialTheme.typography.bodySmall,
                    color = labelColor
                )
            }
            Text(
                text = stringResource(R.string.alerts_day_quiet),
                style = MaterialTheme.typography.bodySmall,
                color = labelColor
            )
        }
    }
}

private class AlertWindow(
    val from: Int,
    val to: Int,
    val color: Color,
    val icon: ImageVector,
    val name: String,
    val tint: Color = Color.Unspecified
)

private val DayIcon = 26.dp
private val DayTrack = 10.dp

/** A template's drawing: what it is about, from the weather family. */
@Composable
private fun templateIcon(template: RuleText.Template): ImageVector = when (template.titleRes) {
    R.string.tpl_bike_title -> ChiaroIcons.wind
    R.string.tpl_ice_title -> ChiaroIcons.frost
    R.string.tpl_run_title -> ChiaroIcons.condition(PartlyCloudyCode)
    R.string.tpl_uv_title -> ChiaroIcons.uv
    R.string.tpl_heat_title -> ChiaroIcons.dewPoint
    R.string.tpl_night_title -> ChiaroIcons.goldenHour
    else -> ChiaroIcons.cloud
}

/**
 * A rule's drawing, from the quantity its first condition watches — frost for a
 * temperature that has to fall to zero, the thermometer for any other.
 */
@Composable
private fun ruleIcon(rule: NotificationRule): ImageVector {
    val first = rule.conditions.firstOrNull() ?: return ChiaroIcons.cloud
    val id = first.variable
    return when {
        "temp" in id && (first.op == RuleOp.LT || first.op == RuleOp.LTE) && first.threshold <= 0.0 ->
            ChiaroIcons.frost
        "temp" in id || "feels" in id || "dew" in id -> ChiaroIcons.dewPoint
        "precip" in id || "rain" in id -> ChiaroIcons.precipitation
        "snow" in id -> ChiaroIcons.frost
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

private val GroupShape = RoundedCornerShape(24.dp)
private val RowIcon = 36.dp
private val RowGap = 14.dp
/** Where a group row's text starts: its inset, the drawing and the gap after it. */
private val RowTextIndent = 16.dp + RowIcon + RowGap
private const val OffAlpha = 0.4f
private val IdeaWidth = 176.dp
private val IdeaHeight = 204.dp

/**
 * A rule's card (VISION §5.4): its name, its sentence in words, when it last fired, and
 * its switch. A `Surface` on `surfaceContainer` like the details tiles, since the review
 * of 8 set 2026 — as a list row it was the ready-made switches' twin, and a row with a
 * switch does not say "I open". The whole card opens the editor; the switch is its own
 * target, as before. [zone] is the place's: "last fired" is a time on this place's
 * clock, like every other hour in the app.
 */
@Composable
private fun RuleCard(
    card: RuleCardModel,
    units: UnitSettings,
    zone: ZoneId,
    firedFmt: DateTimeFormatter,
    onToggle: (Boolean) -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val res = LocalContext.current.resources
    val sentence = card.rule.conditions.joinToString(
        separator = " " + stringResource(R.string.rule_and) + " "
    ) { RuleText.sentence(res, it, units) }
    val fired = card.lastFired?.let {
        stringResource(R.string.rule_last_fired, it.atZone(zone).format(firedFmt))
    } ?: stringResource(R.string.rule_never_fired)
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = GroupShape,
        modifier = modifier.fillMaxWidth().clickable(onClick = onOpen)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RowGap),
            modifier = Modifier.padding(start = 16.dp, top = 14.dp, end = 12.dp, bottom = 14.dp)
        ) {
            // The drawing of what it watches, faded while it is off — as the ready-made
            // rows above do (design review, 23 set 2026).
            Icon(
                imageVector = ruleIcon(card.rule),
                contentDescription = null, // the name beside it says what the rule is
                tint = Color.Unspecified,
                modifier = Modifier
                    .size(RowIcon)
                    .alpha(if (card.rule.enabled) 1f else OffAlpha)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(text = card.rule.name, style = MaterialTheme.typography.titleMedium)
                // The sentence: «Quando» in the ink of a label, the conditions in full.
                Text(
                    text = stringResource(R.string.rule_sentence_prefix) + " " + sentence,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = fired,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = card.rule.enabled, onCheckedChange = onToggle)
        }
    }
}

// ---------------------------------------------------------------------------------
// The editor
// ---------------------------------------------------------------------------------

/** Which picker is open, and for which of the (at most two) conditions. */
private sealed interface EditorDialog {
    data class Variable(val index: Int) : EditorDialog
    data class Operator(val index: Int) : EditorDialog
    data class Value(val index: Int) : EditorDialog
    data object Placeholder : EditorDialog
    data object ConfirmDelete : EditorDialog
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
 * threshold — an optional second condition, the reader's own message, and a dry run
 * that says what the rule would do right now without posting anything. Chip edits
 * persist immediately; the two text fields land when the sheet closes.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun RuleEditorSheet(
    rule: NotificationRule,
    units: UnitSettings,
    viewModel: AlertsViewModel,
    onDismiss: () -> Unit
) {
    val res = LocalContext.current.resources
    val scope = rememberCoroutineScope()
    var name by rememberSaveable(rule.id) { mutableStateOf(rule.name) }
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
    val reduced = reducedMotion()

    fun close() {
        val trimmedName = name.trim().ifEmpty { rule.name }
        if (trimmedName != rule.name || message.text != rule.message) {
            viewModel.update(rule.copy(name = trimmedName, message = message.text))
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

    // An answer about conditions that have since changed would be an answer to a
    // question nobody asked (DESIGN §1.1: the screen must not lie). Editing a chip
    // takes it away rather than letting it age in place; the message is not in the
    // key, because rewording what a fired alert would say does not change whether it
    // fires — the answer already quotes the wording it was given.
    LaunchedEffect(rule.conditions) { preview = null }

    ModalBottomSheet(
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
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.rule_name_label)) },
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

            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
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

            // The dry run (VISION §5.4): what it would have done, nothing posted.
            TextButton(
                onClick = {
                    scope.launch {
                        preview = viewModel.preview(rule.copy(message = message.text))
                        // The answer prints under the button, which on a short screen
                        // is under the fold: whoever just asked the question should not
                        // have to drag the sheet to read it. Two frames, because the
                        // line is composed on the first and measured on the second, and
                        // the scroll range only knows about it once it is measured.
                        withFrameNanos { }
                        withFrameNanos { }
                        // §7: with motion off the answer still has to come into view —
                        // the scroll is what carries the information here, the travel is
                        // only how it gets there.
                        if (reduced) scroll.scrollTo(scroll.maxValue)
                        else scroll.animateScrollTo(scroll.maxValue)
                    }
                },
                contentPadding = FlushTextButtonPadding
            ) {
                Text(stringResource(R.string.rule_preview_action))
            }
            preview?.let { result ->
                Text(
                    text = when (result) {
                        is RulePreview.WouldFire ->
                            stringResource(R.string.rule_preview_fires, result.message)
                        RulePreview.WouldPass -> stringResource(R.string.rule_preview_passes)
                        is RulePreview.Unavailable -> stringResource(
                            R.string.rule_preview_unavailable,
                            stringResource(RuleText.nameRes(result.variableId))
                        )
                        RulePreview.NoData -> stringResource(R.string.rule_preview_no_data)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    // "It would fire, and here is what it would say" is the answer the
                    // reader pressed the button for, in full ink; the three quiet
                    // answers stay quiet (review, 8 set 2026).
                    color = if (result is RulePreview.WouldFire) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            TextButton(
                onClick = { dialog = EditorDialog.ConfirmDelete },
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
        is EditorDialog.Variable -> VariablePickerDialog(
            selected = rule.conditions[d.index].variable,
            onPick = { variableId ->
                val kind = RuleVariables.byId(variableId)?.kind
                val spec = RuleText.valueSpec(variableId)
                val old = rule.conditions[d.index]
                val updated = if (kind == RuleVariableKind.BOOLEAN) {
                    // Booleans read "is yes": comparisons other than è/non è are nonsense.
                    old.copy(variable = variableId, op = RuleOp.EQ, threshold = 1.0)
                } else {
                    old.copy(
                        variable = variableId,
                        op = if (old.op == RuleOp.EQ || old.op == RuleOp.NEQ) RuleOp.GTE else old.op,
                        threshold = old.threshold.coerceIn(spec.min, spec.max)
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
        EditorDialog.ConfirmDelete -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text(stringResource(R.string.rule_delete_confirm_title)) },
            text = { Text(stringResource(R.string.rule_delete_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    dialog = null
                    viewModel.remove(rule.id)
                    onDismiss()
                }) {
                    Text(
                        text = stringResource(R.string.rule_delete),
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

private fun NotificationRule.withCondition(index: Int, condition: RuleCondition): NotificationRule =
    copy(conditions = conditions.mapIndexed { i, c -> if (i == index) condition else c })

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

@Composable
private fun VariablePickerDialog(
    selected: String,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rule_pick_variable)) },
        text = {
            LazyColumn {
                items(RuleVariables.all.size) { index ->
                    val variable = RuleVariables.all[index]
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(variable.id) }
                            .padding(vertical = 10.dp)
                    ) {
                        RadioButton(selected = variable.id == selected, onClick = null)
                        Text(
                            text = stringResource(RuleText.nameRes(variable.id)),
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
 * The threshold on a slider over the variable's honest range, stored canonical and
 * shown in the reader's units — the same value never rewrites itself when the unit
 * setting changes (the engine's own rule).
 */
@Composable
private fun ValuePickerDialog(
    condition: RuleCondition,
    units: UnitSettings,
    onPick: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    val res = LocalContext.current.resources
    val spec = RuleText.valueSpec(condition.variable)
    var value by remember {
        mutableStateOf(condition.threshold.coerceIn(spec.min, spec.max))
    }
    val steps = ((spec.max - spec.min) / spec.step).toInt() - 1
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rule_pick_value)) },
        text = {
            Column {
                Text(
                    text = RuleText.value(res, condition.copy(threshold = value), units),
                    style = MaterialTheme.typography.headlineSmall
                )
                Slider(
                    value = value.toFloat(),
                    onValueChange = {
                        // Snap to the step so the label never shows a value the
                        // slider cannot come back to.
                        value = (Math.round(it / spec.step) * spec.step)
                            .coerceIn(spec.min, spec.max)
                    },
                    valueRange = spec.min.toFloat()..spec.max.toFloat(),
                    steps = steps.coerceAtLeast(0)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onPick(value) }) {
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
 */
internal fun notificationsPromised(content: AlertsUiState.Content): Boolean =
    with(content.notifications) {
        severeWeatherAlerts || precipitationWarning || dailySummary || eveningSummary ||
            officialWarnings || (userRules && content.rules.any { it.rule.enabled })
    }
