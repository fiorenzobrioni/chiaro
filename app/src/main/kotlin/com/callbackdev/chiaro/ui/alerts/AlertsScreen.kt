package com.callbackdev.chiaro.ui.alerts

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.ListItem
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.callbackdev.chiaro.ui.theme.GroupTop
import com.callbackdev.chiaro.ui.theme.SectionBottom
import com.callbackdev.chiaro.ui.theme.SectionTop
import com.callbackdev.chiaro.ui.theme.reducedMotion
import com.callbackdev.chiaro.R
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
import java.util.Locale
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
                    viewModel = alertsViewModel,
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

@Composable
private fun AlertsContent(
    content: AlertsUiState.Content,
    viewModel: AlertsViewModel,
    onEdit: (Long) -> Unit
) {
    val context = LocalContext.current
    // Asked the first time something that needs it is switched on (VISION §5.8).
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    fun somethingTurnedOn() {
        if (!notificationsAllowed(context)) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val locale = Locale.getDefault()
    val is24h = android.text.format.DateFormat.is24HourFormat(context)
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
        item {
            ReadySwitch(
                title = stringResource(R.string.warning_switch_title),
                description = stringResource(R.string.warning_switch_desc),
                checked = content.notifications.officialWarnings,
                onChange = { viewModel.setOfficialWarnings(it); if (it) somethingTurnedOn() }
            )
        }
        // The other honest road to the same choice: the two channels let the system
        // silence the yellow, this lets the app never send it (DESIGN §8.13, Fase 11).
        if (content.notifications.officialWarnings) {
            item {
                WarningFromRow(
                    from = content.notifications.officialWarningsFrom,
                    onChange = viewModel::setOfficialWarningsFrom
                )
            }
        }

        item { GroupTitle(stringResource(R.string.alerts_group_ready)) }
        item {
            ReadySwitch(
                title = stringResource(R.string.alert_severe_title),
                description = stringResource(R.string.alert_severe_desc),
                checked = content.notifications.severeWeatherAlerts,
                onChange = { viewModel.setSevereWeather(it); if (it) somethingTurnedOn() }
            )
        }
        item {
            ReadySwitch(
                title = stringResource(R.string.alert_precip_title),
                description = stringResource(R.string.alert_precip_desc),
                checked = content.notifications.precipitationWarning,
                onChange = { viewModel.setPrecipitationWarning(it); if (it) somethingTurnedOn() }
            )
        }
        item {
            ReadySwitch(
                title = stringResource(R.string.alert_summary_title),
                description = stringResource(R.string.alert_summary_desc),
                checked = content.notifications.dailySummary,
                onChange = { viewModel.setDailySummary(it); if (it) somethingTurnedOn() }
            )
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
                    viewModel.update(card.rule.copy(enabled = enabled))
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
            val existing = content.rules.map { it.rule.conditions }.toSet()
            items(RuleText.templates.size) { index ->
                val template = RuleText.templates[index]
                val added = template.conditions in existing
                ListItem(
                    headlineContent = { Text(stringResource(template.titleRes)) },
                    supportingContent = { Text(stringResource(template.descriptionRes)) },
                    trailingContent = {
                        if (added) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = stringResource(R.string.tpl_already_added),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = null, // the row itself is the action
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    modifier = if (added) {
                        Modifier
                    } else {
                        Modifier.clickable {
                            viewModel.addFromTemplate(template) { created ->
                                somethingTurnedOn()
                                onEdit(created.id)
                            }
                        }
                    }
                )
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
 */
@Composable
private fun OfficialWarningCard(
    state: PlaceWarningState,
    today: LocalDate,
    timeFmt: DateTimeFormatter,
    onOpenSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val locale = Locale.getDefault()
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
                QuietState(
                    title = stringResource(
                        R.string.warning_card_none,
                        WarningText.zoneLabel(context, warnings.zone)
                    ),
                    detail = stringResource(
                        R.string.warning_card_none_detail,
                        warnings.issuedAt.toLocalTime().format(timeFmt)
                    ),
                    onClick = onOpenSheet,
                    modifier = modifier
                )
            }
        }
        is PlaceWarningState.Stale -> QuietState(
            title = stringResource(R.string.warning_card_stale),
            detail = stringResource(
                R.string.warning_card_stale_detail,
                state.issuedAt.toLocalDate().format(dateFmt)
            ),
            onClick = null,
            modifier = modifier
        )
        is PlaceWarningState.Waiting -> QuietState(
            title = stringResource(R.string.warning_card_waiting),
            detail = stringResource(R.string.warning_card_waiting_detail),
            onClick = null,
            modifier = modifier
        )
        PlaceWarningState.Unavailable -> QuietState(
            title = stringResource(R.string.warning_card_unavailable),
            detail = stringResource(R.string.warning_card_unavailable_detail),
            onClick = null,
            modifier = modifier
        )
    }
}

/** The three states the banner must never draw: a fact and its reason, on the neutral
 * container the details tiles use — not a warning colour for the absence of a warning. */
@Composable
private fun QuietState(
    title: String,
    detail: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** «Avvisami da: gialla / arancione» — two chips, because it is two values and a
 * dialog for two values is a screen nobody needs. */
@Composable
private fun WarningFromRow(from: WarningLevel, onChange: (WarningLevel) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp)
    ) {
        Text(
            text = stringResource(R.string.warning_from_title),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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

/** A ready-made alert: a switch with a plain description of what it sends and when. */
@Composable
private fun ReadySwitch(
    title: String,
    description: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(description) },
        trailingContent = { Switch(checked = checked, onCheckedChange = null) },
        modifier = Modifier.clickable(onClick = { onChange(!checked) }, role = Role.Switch)
    )
}

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
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth().clickable(onClick = onOpen)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 12.dp, bottom = 12.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(text = card.rule.name, style = MaterialTheme.typography.titleMedium)
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

private fun notificationsAllowed(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
