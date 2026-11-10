package com.callbackdev.chiaro.ui.sky

import com.callbackdev.chiaro.ui.theme.forText
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import com.callbackdev.chiaro.ui.components.TimelineRow
import com.callbackdev.chiaro.ui.components.SegmentGap
import com.callbackdev.chiaro.ui.components.Segment
import com.callbackdev.chiaro.ui.components.ChiaroSheet
import com.callbackdev.chiaro.ui.components.TabHeader
import com.callbackdev.chiaro.ui.shell.PageColumns
import com.callbackdev.chiaro.ui.shell.PaneBlocks
import com.callbackdev.chiaro.ui.shell.LocalRailInset
import com.callbackdev.chiaro.ui.shell.pageColumns
import com.callbackdev.chiaro.ui.shell.paneItems
import com.callbackdev.chiaro.ui.today.HeaderPadding
import com.callbackdev.chiaro.ui.today.HeroFigure
import com.callbackdev.chiaro.ui.today.HeroReach
import com.callbackdev.chiaro.ui.today.ItemGap
import com.callbackdev.chiaro.ui.today.NoPlaceState
import com.callbackdev.chiaro.ui.today.PlaceHeader
import com.callbackdev.chiaro.ui.today.PlainHeaderHeight
import com.callbackdev.chiaro.ui.today.StatusBarIcons
import com.callbackdev.chiaro.ui.today.barCrossfade
import com.callbackdev.chiaro.ui.today.canvasTop
import com.callbackdev.chiaro.ui.today.heroUnderBar
import com.callbackdev.chiaro.ui.icons.LocalMotionPaused
import com.callbackdev.chiaro.ui.theme.SkyPalette
import com.callbackdev.chiaro.ui.theme.reducedMotion
import com.callbackdev.chiaro.ui.theme.skyInkShadow
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.mutableIntStateOf
import com.callbackdev.chiaro.ui.components.segmentShape
import androidx.activity.compose.BackHandler
import com.callbackdev.chiaro.data.SkySubscription
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.derivedStateOf
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.notifications.NotificationPermission
import com.callbackdev.chiaro.notifications.SkyNotifier
import com.callbackdev.chiaro.ui.components.NotificationsOffCard
import com.callbackdev.chiaro.ui.components.rememberMutedChannels
import com.callbackdev.chiaro.ui.components.rememberNotificationRequest
import com.callbackdev.chiaro.ui.components.rememberNotificationsAllowed
import com.callbackdev.chiaro.domain.model.Coordinates
import com.callbackdev.chiaro.domain.model.MoonPhase
import com.callbackdev.chiaro.domain.sky.MoonQuarterKind
import com.callbackdev.chiaro.domain.sky.SkyJob
import com.callbackdev.chiaro.domain.sky.SkyJobCatalog
import com.callbackdev.chiaro.domain.sky.SkyLead
import com.callbackdev.chiaro.domain.sky.SkyNotScheduled
import com.callbackdev.chiaro.domain.sky.SkyOccurrence
import com.callbackdev.chiaro.domain.sky.SkyVerdictKind
import com.callbackdev.chiaro.domain.sky.SkyVerdictNote
import com.callbackdev.chiaro.ui.today.freshnessAge
import com.callbackdev.chiaro.ui.components.VerdictChip
import com.callbackdev.chiaro.ui.format.Formats
import com.callbackdev.chiaro.ui.format.LocalClock
import com.callbackdev.chiaro.ui.format.currentLocale
import com.callbackdev.chiaro.ui.icons.ChiaroIcons
import com.callbackdev.chiaro.ui.places.PlacesSheet
import com.callbackdev.chiaro.ui.sky.compass.CompassView
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
    /** The events guide. It takes the tab rather than opening beside it: it is a
     * document, and a reader who is in it is reading, not watching tonight's verdict.
     * The shell keeps the bottom bar under it, because they never left the Sky tab. */
    onOpenGuide: () -> Unit,
    skyViewModel: SkyViewModel = viewModel(factory = SkyViewModel.Factory),
    placesViewModel: PlacesViewModel = viewModel(factory = PlacesViewModel.Factory)
) {
    val state by skyViewModel.state.collectAsStateWithLifecycle()
    var placesOpen by remember { mutableStateOf(false) }

    SkyScreen(
        state = state,
        actions = SkyActions(
            addMoment = skyViewModel::addMoment,
            removeMoment = skyViewModel::removeMoment,
            setLead = skyViewModel::setLead,
            restoreMoment = skyViewModel::restoreMoment,
            setMoments = skyViewModel::setMoments,
            setBareSky = skyViewModel::setBareSky
        ),
        onOpenPlaces = { placesOpen = true },
        onOpenSettings = onOpenSettings,
        onOpenGuide = onOpenGuide
    )
    if (placesOpen) {
        PlacesSheet(viewModel = placesViewModel, onDismiss = { placesOpen = false })
    }
}

/**
 * The page itself, from a state and not from the view model: what [SkyRoute] draws, and
 * what the README's screenshots draw from a recorded forecast (25 set 2026).
 */
@Composable
internal fun SkyScreen(
    state: SkyUiState,
    actions: SkyActions,
    onOpenPlaces: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenGuide: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        // Two panes on a wide page, as Today stands (DESIGN §8.12c, 16 ott 2026), and since
        // Fase 16.1 (10 nov 2026) laid out the full window as Today is: the sky runs under
        // the status bar across the page, over the rail's band too, and [LocalRailInset]
        // keeps everything else clear of the rail.
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val railInset = LocalRailInset.current
            val columns = pageColumns(maxWidth, railInset)
            when (state) {
                is SkyUiState.Content -> SkyContent(
                    content = state,
                    actions = actions,
                    onOpenPlaces = onOpenPlaces,
                    onOpenSettings = onOpenSettings,
                    onOpenGuide = onOpenGuide,
                    columns = columns,
                    railInset = railInset,
                    pageHeight = maxHeight
                )
                // No place, no sky to stand a place row on: the tab's plain header, the one
                // Alerts draws, with nothing under its name.
                else -> Column(modifier = Modifier.fillMaxSize()) {
                    TabHeader(
                        title = stringResource(R.string.tab_sky),
                        placeName = null,
                        onOpenPlaces = onOpenPlaces,
                        onOpenSettings = onOpenSettings,
                        modifier = Modifier.padding(start = columns.start, end = columns.end)
                    )
                    // Starting: the tick answers within a frame; no skeleton flash.
                    // The empty state Today and Alerts draw (28 Oct 2026), with this page's
                    // sentence: the Sky had one of its own, the same words set at the start
                    // edge and the top at `titleMedium`, where the other two centre theirs at
                    // `titleLarge`, and the three tabs showed three alignments of one state.
                    if (state == SkyUiState.NoPlace) {
                        Box(modifier = Modifier.padding(start = railInset)) {
                            NoPlaceState(
                                onOpenPlaces = onOpenPlaces,
                                body = stringResource(R.string.sky_no_place_body)
                            )
                        }
                    }
                }
            }
        }
    }
}


// ---------------------------------------------------------------------------------
// Content
// ---------------------------------------------------------------------------------

/** The lead picker that is open: one moment's. The default's lives in Settings since
 * 14 ott 2026 (review point 5). */
private sealed interface LeadDialog {
    data class ForMoment(val jobId: String, val lead: SkyLead, val followsDefault: Boolean) : LeadDialog
}

/** What the screen can ask of its store, as functions rather than the view model: the
 * content is then a plain composable a preview or a test can draw. */
internal class SkyActions(
    val addMoment: (String) -> Unit,
    val removeMoment: (String) -> Unit,
    val setLead: (String, Int?) -> Unit,
    /** Puts a removed line back as it was, lead included (review point 9). */
    val restoreMoment: (SkySubscription) -> Unit = { addMoment(it.jobId) },
    /** The file whole: the catalog's way back to the four defaults, and its undo (20 ott 2026). */
    val setMoments: (List<SkySubscription>) -> Unit = {},
    /** «Behind the clouds» on the Sky's hero, kept across launches (22 ott 2026). */
    val setBareSky: (Boolean) -> Unit = {}
)

@Composable
private fun SkyContent(
    content: SkyUiState.Content,
    actions: SkyActions,
    onOpenPlaces: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenGuide: () -> Unit,
    columns: PageColumns,
    railInset: Dp,
    /** The page's height, the status bar's band included: what the canvas is sized on. */
    pageHeight: Dp
) {
    val locale = currentLocale()
    val is24h = android.text.format.DateFormat.is24HourFormat(LocalContext.current)
    val timeFmt = remember(locale, is24h) { Formats.timeFormatter(is24h, locale) }
    val dateFmt = remember(locale) { DateTimeFormatter.ofPattern("d MMMM", locale) }
    // The same date with its year, for a row that is not in this one: the aperiodic
    // searches reach years out and "2 agosto" alone was a date the reader could not
    // place (Fase 27).
    val yearFmt = remember(locale) { DateTimeFormatter.ofPattern("d MMMM yyyy", locale) }

    var catalogOpen by remember { mutableStateOf(false) }
    var leadDialog by remember { mutableStateOf<LeadDialog?>(null) }
    // The page a row of the agenda opened, if any ([AgendaPageSheet]).
    var pageId by rememberSaveable { mutableStateOf<String?>(null) }
    // And where that row says to look, with its time line, for the page's compass
    // (14 ott 2026): a direction belongs to an occurrence, not to the job the page is for.
    var pageBearing by rememberSaveable { mutableStateOf<Double?>(null) }
    var pageWhen by rememberSaveable { mutableStateOf("") }

    // POST_NOTIFICATIONS is asked the first time a reminder is switched on (VISION
    // §5.8), never at startup: the tap that needs it is the sentence that explains it.
    // And once a bell IS set, the card above the list says so for as long as the phone
    // cannot ring it — the same repair Avvisi carries, for the same reason.
    val notificationsAllowed by rememberNotificationsAllowed()
    val request = rememberNotificationRequest()
    fun leadChosen(minutes: Int?) {
        if (minutes != null && minutes > 0 && !notificationsAllowed) request.ask()
    }

    // Read here and not in the list below: a lazy list's builder is not a composable.
    val clock = LocalClock.current
    val listState = rememberLazyListState()
    val snackbars = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val res = LocalContext.current.resources
    val undoLabel = stringResource(R.string.action_undo)

    // The place row is pinned over the list and stands on the sky, as Today's does (Fase
    // 16.1, 10 nov 2026; DESIGN §8.1b): until then the tab had its name in a header of paper
    // and the night in a card under it. At rest the row is the canvas' white on the night;
    // from the first scrolled pixel it is the page's own, ground and ink on one crossfade;
    // and once the verdict has gone under it, it carries the verdict, as Today's carries the
    // temperature. Its second line is the night the page is about.
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val density = LocalDensity.current
    val reduced = reducedMotion()
    // The canvas keeps the row's seat free of names: as tall as the bar really is, measured.
    var headerHeight by remember(statusTop) { mutableStateOf(statusTop + PlainHeaderHeight) }
    val atTop by remember {
        derivedStateOf { listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0 }
    }
    val crossfade = barCrossfade<Color>(reduced)
    // The transparent end is the same color at zero alpha, not `Color.Transparent`: fading in
    // from a transparent black would draw a veil of the wrong hue on the way.
    val paper = MaterialTheme.colorScheme.surface
    val headerGround by animateColorAsState(
        targetValue = if (atTop) paper.copy(alpha = 0f) else paper,
        animationSpec = crossfade,
        label = "sky place row ground"
    )
    val headerInk by animateColorAsState(
        targetValue = if (atTop) SkyPalette.Ink else MaterialTheme.colorScheme.onSurface,
        animationSpec = crossfade,
        label = "sky place row ink"
    )
    val headerHalo by animateFloatAsState(
        targetValue = if (atTop) SkyPalette.InkHaloAlpha else 0f,
        animationSpec = barCrossfade<Float>(reduced),
        label = "sky place row halo"
    )
    // The status bar shows what is behind it: the night at rest, white icons; the page once
    // scrolled, the theme's.
    StatusBarIcons(overCanvas = atTop)
    // The verdict is gone once its middle has gone under the bar (DESIGN §8.1e): the canvas
    // measures where it rests, and the rest is arithmetic on the scroll. Until it is
    // measured, the canvas' bottom climbing to within [HeroReach] of the bar stands in.
    val figure = remember { HeroFigure() }
    val heroGone by remember {
        derivedStateOf {
            val barBottom = with(density) { headerHeight.toPx() }
            if (figure.measured) {
                heroUnderBar(listState.canvasTop(), figure.rest, figure.size, barBottom)
            } else {
                val first = listState.layoutInfo.visibleItemsInfo.firstOrNull() ?: return@derivedStateOf false
                first.index > 0 ||
                    first.offset + first.size < with(density) { (headerHeight + HeroReach).roundToPx() }
            }
        }
    }
    val nightForms = nightDateForms(nightDate(content.tonight, java.time.Instant.now(clock), content.zone), locale)
        .map { stringResource(R.string.sky_hero_night, it) }

    /**
     * «Annulla» (review point 9, 14 ott 2026) after an add or a removal made from an
     * event's page: the sheet closes on the act, and the row that appeared or vanished
     * may well be off screen, so the snackbar is the receipt and the way back. A removal
     * is undone by putting the line back as it was, its own lead included ([restore]); an
     * add by removing it. Long, because it carries an action somebody may need a moment
     * to reach (and TalkBack users more than a moment).
     */
    fun announce(jobId: String, added: Boolean) {
        val name = res.getString(SkyText.nameRes(jobId))
        val line = content.subscriptions.firstOrNull { it.jobId == jobId }
        scope.launch {
            snackbars.currentSnackbarData?.dismiss()
            val result = snackbars.showSnackbar(
                message = res.getString(
                    if (added) R.string.sky_moment_added else R.string.sky_moment_removed, name
                ),
                actionLabel = undoLabel,
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) {
                if (added) actions.removeMoment(jobId)
                else actions.restoreMoment(line ?: SkySubscription(jobId))
            }
        }
    }
    // The page's blocks, said once for the phone's list and the wide page's two panes
    // (DESIGN §8.12c, 16 ott 2026): on the left tonight and the moments of today and
    // tomorrow, which are about the next two days as Today's left pane is about today; on
    // the right the calendar ahead, which is about the months, as Today's week is about
    // the days. The card that says the phone cannot ring stands across both.
    val blocks = PaneBlocks(remember { mutableIntStateOf(0) })
    val armed = remindersArmed(content)
    if (!notificationsAllowed && armed) {
        blocks.across {
            NotificationsOffCard(
                body = stringResource(R.string.notifications_off_sky),
                onAllow = request.ask,
                settingsOnly = request.settingsOnly,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
    // The card's second voice (15.3 on Alerts, here since 15.13, 29 Oct 2026): the permission
    // is there and a bell is set, but the phone has silenced the reminders' own channel — set
    // to «none» in the system's settings, or by a long press on one reminder — so every
    // `notify` on it returns false and the moment passes in silence. Named by the channel's
    // own name, the word the reader will look for on the settings page, with the button that
    // opens that page: no dialog can un-mute a channel. One channel, so the singular of the
    // card's words. Not drawn while notifications are off altogether: the card above already
    // says nothing can arrive. A channel never created — no reminder has fired yet — is not
    // muted, and the card is not drawn.
    val muted by rememberMutedChannels(listOf(SkyNotifier.CHANNEL_ID))
    if (notificationsAllowed && armed && SkyNotifier.CHANNEL_ID in muted) {
        blocks.across {
            val context = LocalContext.current
            val name = stringResource(R.string.notifications_muted_name, stringResource(R.string.sky_channel_name))
            NotificationsOffCard(
                title = pluralStringResource(R.plurals.notifications_muted_title, 1),
                body = pluralStringResource(R.plurals.notifications_muted_body, 1, name),
                settingsOnly = true,
                onAllow = { NotificationPermission.openChannelSettings(context, SkyNotifier.CHANNEL_ID) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
    // The first title on the paper stands close under the sheet's own lip, as Today's does;
    // with a card across the page before it, it keeps a section's air.
    val firstTitleTop = if (blocks.across.isEmpty()) ItemGap else SectionTop
    // «Aggiungi» stands in the section's title (Fase 16.2, 10 nov 2026): the floating button
    // that carried it since 14 ott 2026 stood over the time and the direction of the very
    // moment the reader was waiting for, and adding a moment is about this list.
    blocks.left {
        SkySectionTitle(
            stringResource(R.string.sky_section_moments),
            top = firstTitleTop,
            action = { AddMomentButton(onClick = { catalogOpen = true }) }
        )
    }
    // Grouped by day under a heading of their own (design review, 23 set 2026): four
    // rows that each began «Domani ·» said the same word four times and pushed the
    // time — the thing a reader scans for — to the middle of the line.
    // The moment a reader is waiting for is the first row with an hour, in its day's group
    // and in the row every other moment has ([momentsHead]): a card of its own (14 ott 2026)
    // was the second drawing of one moment on one page.
    val head = momentsHead(content.moments)
    fun open(moment: Moment, line: String) {
        pageId = moment.job.id
        pageBearing = moment.bearingDeg
        pageWhen = line
    }
    fun bell(moment: Moment) {
        leadDialog = LeadDialog.ForMoment(moment.job.id, moment.lead, moment.followsDefault)
    }
    val (todays, tomorrows) = content.moments.partition { it.timing != MomentTiming.TOMORROW }
    listOf(R.string.sky_day_today to todays, R.string.sky_day_tomorrow to tomorrows)
        .filter { it.second.isNotEmpty() }
        .forEach { (dayRes, dayMoments) ->
            blocks.left { SkyDayHeading(stringResource(dayRes)) }
            dayOrder(dayMoments).forEachIndexed { index, moment ->
                blocks.left {
                    MomentRow(
                        moment = moment,
                        zone = content.zone,
                        timeFmt = timeFmt,
                        shape = segmentShape(index, dayMoments.size),
                        head = moment === head,
                        onOpen = { open(moment, it) },
                        onBell = { bell(moment) }
                    )
                }
            }
        }
    // The verdicts refused for the data (22 ott 2026, review of the Sky screen): the rows
    // keep the moment and its time and carry no chip, and the reason is said once here,
    // with the data's age, the rule the widgets and the notifications follow since 21 ott.
    // Until then every row printed «? Not sure yet», which blames the hour for the age.
    if (content.moments.any { it.verdict?.isRefusedForData == true }) {
        blocks.left { DataNote(content) }
    }
    blocks.right {
        SkySectionTitle(
            stringResource(R.string.sky_section_events),
            // The second pane's first title on the first one's line.
            top = if (columns.twoPanes) firstTitleTop else SectionTop
        )
    }
    val today = java.time.LocalDate.now(clock.withZone(content.zone))
    content.events.forEachIndexed { index, event ->
        blocks.right {
            EventRow(
                shape = segmentShape(index, content.events.size),
                event = event,
                zone = content.zone,
                dateFmt = dateFmt,
                yearFmt = yearFmt,
                timeFmt = timeFmt,
                today = today,
                // A row that names two showers opens the first: it is the row's own job,
                // and the page's «see also» is one tap from the rest.
                onOpen = { line ->
                    pageId = event.job.id
                    pageBearing = event.bearingDeg
                    pageWhen = line
                },
                onBell = event.lead?.let { lead ->
                    { leadDialog = LeadDialog.ForMoment(event.job.id, lead, event.followsDefault) }
                }
            )
        }
    }

    // «Too far out to say» once for the section, not once a row (23 set 2026): five
    // rows in a row each ending «La previsione non arriva ancora così lontano» was the
    // same sentence five times, wrapped onto a second line each time. It is still said
    // — the rows simply carry no chip until the forecast reaches them.
    if (content.events.any { it.verdict?.note == SkyVerdictNote.BEYOND_HORIZON }) {
        blocks.right { SectionNote(stringResource(R.string.sky_events_beyond_note)) }
    }
    // And once for the data's age or absence, as under the moments (22 ott 2026).
    if (content.events.any { it.verdict?.isRefusedForData == true }) {
        blocks.right { DataNote(content) }
    }

    // The door for a reader who came to understand rather than to subscribe, at the end of
    // the page (review of the Sky screen, 22 ott 2026: between the agenda and the calendar
    // it cut the reading in two; at the foot it is where a reader who got there asks what
    // these are). Adding is the moments' title's since Fase 16.2.
    blocks.right {
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

    // A Scaffold for the one thing that floats over the list: the snackbar that offers
    // «Annulla» (review point 9). The floating button it also held went with Fase 16.2.
    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbars, modifier = Modifier.padding(start = railInset)) }
    ) { inner ->
        // Zero today (no bars, no insets: the bottom bar is the shell's), and right the day
        // the Scaffold is given one.
        Box(modifier = Modifier.fillMaxSize().padding(inner)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                // The page's own margin under its last row, now that nothing floats there.
                contentPadding = PaddingValues(bottom = PageBottom)
            ) {
                item(key = "tonight") {
                    // While the page moves the sky holds still (DESIGN §7.1), as Today's does.
                    CompositionLocalProvider(
                        LocalMotionPaused provides (LocalMotionPaused.current || listState.isScrollInProgress)
                    ) {
                        TonightHero(
                            tonight = content.tonight,
                            zone = content.zone,
                            timeFmt = timeFmt,
                            floor = skyHeroHeight(pageHeight, statusTop),
                            placeRowHeight = headerHeight,
                            coordinates = content.coordinates,
                            followed = content.subscribedIds,
                            bareSky = content.bareSky,
                            onBareSky = actions.setBareSky,
                            figure = figure,
                            columns = columns,
                            railInset = railInset
                        )
                    }
                }
                paneItems(blocks, columns)
            }
            // After the list so it stands over it. No position pin and no dots: the Sky has
            // one place at a time, the one Today is on.
            PlaceHeader(
                title = content.placeName,
                isGps = false,
                localNow = nightForms,
                dots = null,
                onOpenPlaces = onOpenPlaces,
                onOpenSettings = onOpenSettings,
                contentColor = headerInk,
                dotInactive = headerInk,
                shadow = if (headerHalo > 0.01f) skyInkShadow(headerHalo) else null,
                compactVisible = heroGone,
                compact = { TonightInBar(content.tonight, headerInk) },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    // First in the chain: what the canvas must keep clear is the whole bar,
                    // status-bar inset and padding included.
                    .onSizeChanged { size -> headerHeight = with(density) { size.height.toDp() } }
                    .background(headerGround)
                    .statusBarsPadding()
                    // Beside the rail, whose band of status bar is this bar's ground (§8.12c).
                    .padding(start = railInset)
                    .padding(HeaderPadding)
            )
        }
    }

    if (catalogOpen) {
        CatalogSheet(
            // The store's own set, not one rebuilt from the rows on screen: a subscribed
            // job with no row (an eclipse search that finds nothing ahead) used to show
            // as unsubscribed here and offer to be added twice (review, 8 set 2026).
            subscribedIds = content.subscribedIds,
            subscriptions = content.subscriptions,
            onAdd = actions.addMoment,
            onRemove = actions.removeMoment,
            onRestore = actions.restoreMoment,
            onSet = actions.setMoments,
            onDismiss = { catalogOpen = false }
        )
    }

    pageId?.let { id ->
        AgendaPageSheet(
            jobId = id,
            onOpenRelated = {
                pageId = it
                pageBearing = null
            },
            onDismiss = { pageId = null },
            subscription = AgendaSubscription(
                subscribed = id in content.subscribedIds,
                onAdd = {
                    actions.addMoment(it)
                    announce(it, added = true)
                },
                onRemove = {
                    actions.removeMoment(it)
                    announce(it, added = false)
                }
            ),
            look = pageBearing?.let { LookTarget(it, pageWhen) },
            coordinates = content.coordinates
        )
    }

    when (val dialog = leadDialog) {
        is LeadDialog.ForMoment -> LeadPickerDialog(
            title = stringResource(R.string.sky_lead_title),
            defaultLead = content.defaultLead,
            current = if (dialog.followsDefault) null else (dialog.lead.minutes ?: 0),
            perMoment = true,
            onPick = { minutes ->
                actions.setLead(dialog.jobId, minutes)
                leadChosen(minutes ?: content.defaultLead.minutes)
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

/** A section's footnote: one sentence in the quiet colour, under its rows. */
@Composable
private fun SectionNote(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

/**
 * Why the verdicts of a section are missing (22 ott 2026): the forecast is too old to
 * judge by, said with its real age in the words Today's chip uses («Updated 9 hours
 * ago: the verdicts wait for a fresh forecast»), or there is no forecast yet. Said once
 * per section, never on a row: the rows keep the moment and its time, which the phone
 * computes on its own and which are true whatever the forecast's age.
 */
@Composable
private fun DataNote(content: SkyUiState.Content) {
    val lastSync = content.lastSync
    SectionNote(
        if (lastSync != null) {
            stringResource(R.string.sky_verdicts_stale_note, freshnessAge(lastSync))
        } else {
            stringResource(R.string.sky_verdicts_no_data_note)
        }
    )
}

/** A day inside «I prossimi momenti»: the word the rows used to carry each, said once. */
@Composable
private fun SkyDayHeading(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 8.dp)
    )
}

/**
 * A section's title, and at its end the section's own [action] when it has one («Aggiungi»
 * on the moments, Fase 16.2).
 *
 * **A title with an action stands where a title without one does.** The button's box is
 * 48dp and the title's line 24: laid out as it measures, it would push the title 12dp down
 * and the list 12dp further, and on a wide page the two panes' first titles, one with the
 * button and one without, would stand on two lines. So the button is placed on the title's
 * line and counts for the line's height only ([onTheLine]): its box hangs 12dp over the air
 * above and below, which is air, and the finger still finds all 48.
 */
@Composable
private fun SkySectionTitle(text: String, top: Dp = SectionTop, action: (@Composable () -> Unit)? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 16.dp, top = top, bottom = SectionBottom, end = TitleActionEnd)
    ) {
        Text(text = text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        action?.let { Box(Modifier.onTheLine(TitleLine)) { it() } }
    }
}

/** Lays its content out whole and reports [line] for its height, centred on it. */
private fun Modifier.onTheLine(line: Dp): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.copy(minHeight = 0))
    val height = line.roundToPx().coerceAtMost(placeable.height)
    layout(placeable.width, height) { placeable.place(0, (height - placeable.height) / 2) }
}

/** `titleMedium`'s line, which a title's action may not make taller. Not scaled with the
 * reader's type: at a larger one the line is taller than this and the button hangs less. */
private val TitleLine = 24.dp

/** A text button's own 16dp of padding after its word, and 4: the word ends 20dp from the
 * edge, a little inside the segments' own. */
private val TitleActionEnd = 4.dp

// ---------------------------------------------------------------------------------
// Tonight
// ---------------------------------------------------------------------------------

// ---------------------------------------------------------------------------------
// Moments and events
// ---------------------------------------------------------------------------------

/**
 * The first moment with an hour of its own, in the order the list draws them: the one a
 * reader is waiting for, or the one happening now. Its row says how soon, or how much of it
 * is left ([MomentRow]). The moon's phase has no hour and is nobody's «next»; a moment the
 * sky skips has none either.
 */
internal fun momentsHead(moments: List<Moment>): Moment? = moments.firstOrNull { it.hasHour }

/** Whether the row has an hour to print: the moon's phase is the day's, and a moment the
 * sky skips has no occurrence. */
private val Moment.hasHour: Boolean
    get() = occurrence is SkyOccurrence.At && moonPhase == null

/**
 * A day's moments in the order its group draws them: by the hour, as the state gives them,
 * and after them the ones with no hour of their own. The state sorts the moon's phase by the
 * noon its reminder is set for, and at the row's end the hour was not what a reader read
 * the list by; in a clock column a row with no clock has no place among the hours, and at
 * the head of the list it stood where the next moment is looked for.
 */
internal fun dayOrder(moments: List<Moment>): List<Moment> = moments.sortedBy { !it.hasHour }

/**
 * DESIGN §8.8: a moment of the Sky's list, in the row Today's agenda draws the same moment
 * with ([TimelineRow], §8.4) since Fase 16.2 (10 nov 2026): the hour in the clock column, a
 * span's end under its start, the name with the camera when it is one to photograph, which
 * way to turn under it, the bell at the row's end and the verdict on its own line.
 *
 * Until then the time stood at the row's end (review point 3, 14 ott 2026) and the next
 * moment had a card of its own with the time large (point 2): the same golden hour was
 * three drawings, one on Today and two here.
 *
 * **The head of the list keeps its line** ([head]): «Tra 2 h 15 min» in the accent over the
 * name of the moment a reader is waiting for, within half a day. And a moment that is
 * happening says «Adesso» in the clock column, where its hour would be, with how much of it
 * is left over its name, «Ancora 4 h 54 min», and until when under it: its start is behind
 * the reader, and what they can still use is the end.
 */
@Composable
private fun MomentRow(
    moment: Moment,
    zone: ZoneId,
    timeFmt: DateTimeFormatter,
    shape: Shape,
    /** The list's first moment with an hour ([momentsHead]). */
    head: Boolean,
    /** Opens the row's page, handed the row's own time line for its compass. */
    onOpen: (String) -> Unit,
    onBell: () -> Unit
) {
    val res = LocalContext.current.resources
    val name = stringResource(SkyText.nameRes(moment.job.id))
    val occ = moment.occurrence
    val timed = (occ as? SkyOccurrence.At)?.takeIf { moment.moonPhase == null }
    val now = moment.timing == MomentTiming.NOW
    val nowWord = stringResource(R.string.sky_moment_now)
    val start = timed?.start?.atZone(zone)?.format(timeFmt)
    val end = timed?.end?.atZone(zone)?.format(timeFmt)
    val untilEnd = end?.let { stringResource(R.string.sky_until, it) }
    val detail = when {
        moment.moonPhase != null -> moonLine(moment, zone, timeFmt)
        occ is SkyOccurrence.None -> stringResource(SkyText.notScheduledRes(occ.reason))
        else -> null
    }
    // Which day the hour belongs to is the heading's to say since 23 set 2026; the row says
    // what no heading can: that it is happening, or how soon the first one comes.
    val label = when {
        now -> timed?.end?.let { leftLine(it) }
        head -> timed?.start?.let { soonLine(it) }?.replaceFirstChar { it.titlecase(currentLocale()) }
        else -> null
    }
    val line = timeLine(moment, zone, timeFmt)
    Segment(shape = shape, modifier = Modifier.padding(horizontal = 16.dp, vertical = SegmentGap / 2)) {
        TimelineRow(
            // A row with no hour (the moon's phase, a moment the sky skips) keeps the column
            // empty, so the drawings of its group stand in one column.
            time = if (now) nowWord else start.orEmpty(),
            icon = momentIcon(moment),
            text = name,
            label = label,
            supporting = listOfNotNull(detail, untilEnd.takeIf { now }, bearingLine(moment.bearingDeg))
                .joinToString(" · ")
                .ifEmpty { null },
            until = end.takeUnless { now },
            untilDescription = untilEnd.takeUnless { now },
            onClick = { onOpen(line) },
            onClickLabel = stringResource(R.string.sky_row_open_label),
            mark = if (moment.job.photographic) ({ CameraMark(rowMark()) }) else null,
            trailing = { BellButton(lead = moment.lead, name = name, onClick = onBell) },
            // A verdict refused for the data's age or absence is not a chip (22 ott 2026):
            // «?» alone blamed the hour, and the line under the section says the real reason.
            verdict = moment.verdict?.takeUnless { it.isRefusedForData }?.let { verdict ->
                {
                    VerdictChip(
                        kind = SkyText.chipKind(verdict.kind),
                        label = stringResource(SkyText.verdictWordRes(verdict.kind)),
                        evidence = SkyText.chipEvidence(res, verdict)
                    )
                }
            }
        )
    }
}

/** «18:36 – 19:15», «19:15», the moon's phase, or the reason the sky skips it: the whole
 * time of a moment in one line, for the compass's header. */
@Composable
private fun timeLine(moment: Moment, zone: ZoneId, timeFmt: DateTimeFormatter): String =
    when (val occ = moment.occurrence) {
        is SkyOccurrence.At -> when {
            moment.moonPhase != null -> moonLine(moment, zone, timeFmt)
            occ.end != null ->
                "${occ.start.atZone(zone).format(timeFmt)} – ${occ.end!!.atZone(zone).format(timeFmt)}"
            else -> occ.start.atZone(zone).format(timeFmt)
        }
        is SkyOccurrence.None -> stringResource(SkyText.notScheduledRes(occ.reason))
    }

/** The page's own margin under its last row. Until Fase 16.2 the list kept 88dp there for
 * the floating button. */
private val PageBottom = 16.dp

/**
 * «Aggiungi», in the title of «I prossimi momenti» (Fase 16.2, 10 nov 2026): the screen's
 * one action, where the list it adds to begins. It was an extended floating button from 14
 * ott 2026, and over a page that opens on the night it stood on the hour and the direction
 * of the next moment. The plus and one word; a screen reader hears the whole of it,
 * «Aggiungi un momento», because a title is not beside the word when it is read alone.
 */
@Composable
private fun AddMomentButton(onClick: () -> Unit) {
    val said = stringResource(R.string.sky_add_moment)
    TextButton(
        onClick = onClick,
        contentPadding = ButtonDefaults.TextButtonWithIconContentPadding,
        modifier = Modifier.semantics { contentDescription = said }
    ) {
        Icon(
            imageVector = Icons.Outlined.Add,
            contentDescription = null,
            modifier = Modifier.size(ButtonDefaults.IconSize)
        )
        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
        Text(stringResource(R.string.sky_add))
    }
}

/**
 * A row of the agenda opens its event's page ([AgendaPageSheet]).
 *
 * The whole row, verdict line included, and not an info button beside the bell: the
 * row has no 48 dp to spare, and a row is where a finger already goes to ask "what is
 * this". The bell stays its own [IconButton] and takes its own taps, the same two-targets
 * rule as the catalog's rows. No chevron either, for the same 24 dp; TalkBack is told what
 * the tap does (`sky_row_open_label`), because a ripple says it only to the eye.
 *
 * A row of the calendar ahead, in the moments' row without its clock column (Fase 16.2):
 * its «when» is a date, days out, and stays in the line under the name.
 */
@Composable
private fun EventRow(
    event: UpcomingEvent,
    zone: ZoneId,
    dateFmt: DateTimeFormatter,
    yearFmt: DateTimeFormatter,
    timeFmt: DateTimeFormatter,
    today: java.time.LocalDate,
    shape: Shape,
    onOpen: (String) -> Unit,
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
            val countdown = daysAway(today, occurrence.start.atZone(zone).toLocalDate())
            listOfNotNull(date, window, countdown).joinToString(" · ")
        }
        is SkyOccurrence.None -> stringResource(SkyText.notScheduledRes(occurrence.reason))
    }
    // «Too far out» is the section's footnote, and so are «no data» and «old data» since
    // 22 ott 2026 (six rows each ending «too old to judge by» was one sentence six times);
    // what stays on the row is a reason about the row itself, the hours the forecast
    // does not cover.
    val verdictLine = event.verdict
        ?.takeIf { it.note != SkyVerdictNote.BEYOND_HORIZON && !it.isRefusedForData }
        ?.let { verdict -> SkyText.unknownReason(res, verdict) }
    val supporting = listOfNotNull(whenLine, conjunctionLine(event), bearingLine(event.bearingDeg), verdictLine)
        .joinToString(" · ")
    Segment(shape = shape, modifier = Modifier.padding(horizontal = 16.dp, vertical = SegmentGap / 2)) {
        TimelineRow(
            time = null,
            icon = eventIcon(event),
            text = name,
            // An eclipse says which kind it is and the number that decides it, on a line of
            // its own, the same way a verdict carries its own arithmetic.
            supporting = listOfNotNull(supporting, eclipseLine(res, event)).joinToString("\n"),
            onClick = { onOpen(whenLine) },
            onClickLabel = stringResource(R.string.sky_row_open_label),
            mark = if (event.job.photographic) ({ CameraMark(rowMark()) }) else null,
            trailing = if (onBell != null && event.lead != null) {
                { BellButton(lead = event.lead, name = name, onClick = onBell) }
            } else null,
            verdict = event.verdict?.takeIf { it.kind != SkyVerdictKind.UNKNOWN }?.let { verdict ->
                {
                    VerdictChip(
                        kind = SkyText.chipKind(verdict.kind),
                        label = stringResource(SkyText.verdictWordRes(verdict.kind)),
                        evidence = SkyText.chipEvidence(res, verdict)
                    )
                }
            }
        )
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

/** «oggi», «domani», «tra 15 giorni» — up to two months out, where a count of days is
 * still a way of planning; past that the date alone says it. */
@Composable
private fun daysAway(today: java.time.LocalDate, date: java.time.LocalDate): String? {
    val days = java.time.temporal.ChronoUnit.DAYS.between(today, date)
    return when {
        days < 0 -> null
        days == 0L -> stringResource(R.string.sky_day_today).lowercase(currentLocale())
        days == 1L -> stringResource(R.string.sky_day_tomorrow).lowercase(currentLocale())
        days <= CountdownDays -> pluralStringResource(R.plurals.sky_event_in_days, days.toInt(), days.toInt())
        else -> null
    }
}

private const val CountdownDays = 60

/** «Ancora 20 min», «Ancora 4 h 54 min»: how much is left of a moment that is happening,
 * to its [end]. Nothing under a minute: «Adesso» in the clock column has said it. */
@Composable
private fun leftLine(end: java.time.Instant): String? {
    val minutes = java.time.Duration.between(java.time.Instant.now(LocalClock.current), end).toMinutes()
    return when {
        minutes < 1 -> null
        minutes < 60 -> stringResource(R.string.sky_left_minutes, minutes.toInt())
        minutes % 60 == 0L -> stringResource(R.string.sky_left_hours, (minutes / 60).toInt())
        else -> stringResource(R.string.sky_left_hours_minutes, (minutes / 60).toInt(), (minutes % 60).toInt())
    }
}

/** «tra 20 min», «tra 2 h», «tra 1 h 20 min» — the same words Today's agenda uses — for
 * the next moment of the list, and only within half a day. */
@Composable
private fun soonLine(at: java.time.Instant): String? {
    val minutes = java.time.Duration.between(java.time.Instant.now(LocalClock.current), at).toMinutes()
    return when {
        minutes < 1 || minutes > 12 * 60 -> null
        minutes < 60 -> stringResource(R.string.tl_in_minutes, minutes.toInt())
        minutes % 60 == 0L -> stringResource(R.string.tl_in_hours, (minutes / 60).toInt())
        else -> stringResource(R.string.tl_in_hours_minutes, (minutes / 60).toInt(), (minutes % 60).toInt())
    }
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
        CameraMark(Modifier.padding(start = 6.dp).size(18.dp))
    }
}

/** The camera after a name: beside it on the catalog's rows ([SkyHeadline]), and after
 * the name's last word on the page's ([TimelineRow]'s `mark`, [rowMark]). */
@Composable
private fun CameraMark(modifier: Modifier) {
    Icon(
        painter = painterResource(R.drawable.ic_photographic),
        contentDescription = stringResource(R.string.sky_photographic_desc),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

/** On a row of the page the mark grows with the reader's type, as the name it follows. */
@Composable
private fun rowMark(): Modifier = Modifier.padding(start = 6.dp).size(18.dp.forText())

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

/**
 * «Luna piena domani alle 18:49 · 99% illuminata» (review point 6, 14 ott 2026): the
 * phase, with the day and hour of its quarter when it is one of the four — a phase name
 * covers almost two days either side of its instant, and «Luna piena» on the evening
 * before the calendar's full moon read as the app disagreeing with itself — and the
 * percentage of the screen's one moon ([com.callbackdev.chiaro.domain.sky.MoonOfTheNight]).
 */
@Composable
private fun moonLine(moment: Moment, zone: ZoneId, timeFmt: DateTimeFormatter): String {
    val phase = moment.moonPhase ?: return ""
    val name = stringResource(SkyText.phaseRes(phase))
    val quarter = moment.moonQuarterAt
    val day = (moment.occurrence as? SkyOccurrence.At)?.start?.atZone(zone)?.toLocalDate()
    val named = if (quarter != null && day != null) {
        val local = quarter.atZone(zone)
        val locale = currentLocale()
        val whenDay = when (java.time.temporal.ChronoUnit.DAYS.between(day, local.toLocalDate())) {
            0L -> stringResource(R.string.sky_day_today).lowercase(locale)
            1L -> stringResource(R.string.sky_day_tomorrow).lowercase(locale)
            -1L -> stringResource(R.string.sky_day_yesterday).lowercase(locale)
            else -> stringResource(
                R.string.sky_moon_on_date,
                local.format(DateTimeFormatter.ofPattern("d MMMM", locale))
            )
        }
        stringResource(R.string.sky_moon_quarter_when, name, whenDay, local.format(timeFmt))
    } else name
    val pct = moment.moonIlluminationPct
    return if (pct != null) stringResource(R.string.sky_moon_line, named, pct) else named
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

/**
 * The drawing for a moment, by the job that names it. **The table is
 * [ChiaroIcons.skyJobLineRes] and it is shared with the Sky widget** (21 set 2026): this
 * screen and that card each carried one, the catalog grew twice, and only this one was
 * kept up — see there for the twenty-five jobs the card was drawing as meteor showers.
 */
@Composable
internal fun jobIcon(job: SkyJob) = ChiaroIcons.skyJob(job.id)

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
    subscriptions: List<SkySubscription>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
    onRestore: (SkySubscription) -> Unit,
    onSet: (List<SkySubscription>) -> Unit,
    onDismiss: () -> Unit
) {
    var openId by rememberSaveable { mutableStateOf<String?>(null) }
    // The way back to the four defaults (committente, 20 ott 2026): the list it replaced is
    // kept while the sheet is open, so the row that reset it undoes it in place.
    var resetFrom by remember { mutableStateOf<List<SkySubscription>?>(null) }
    // The catalog's undo is the same tap (review point 9, 14 ott 2026): the row's mark
    // turns in place, so it needs no snackbar — which a modal sheet would cover anyway.
    // What the tap could not do was bring back a line's own lead; the lines removed in
    // this sheet are kept here, and tapping one again restores it as it was.
    val removedHere = remember { mutableStateMapOf<String, SkySubscription>() }
    fun toggle(jobId: String) {
        if (jobId in subscribedIds) {
            subscriptions.firstOrNull { it.jobId == jobId }?.let { removedHere[jobId] = it }
            onRemove(jobId)
        } else {
            removedHere.remove(jobId)?.let(onRestore) ?: onAdd(jobId)
        }
    }
    var query by rememberSaveable { mutableStateOf("") }
    ChiaroSheet(onDismissRequest = onDismiss) {
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
                            toggle(open.id)
                            openId = null
                        }
                    )
                }
            )
            return@ChiaroSheet
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
                        // The mark goes here as well as on the agenda rows, and this is
                        // the place it earns most: the catalog is where somebody BROWSES
                        // for an event worth going out for, and a mark that only appears
                        // once the line is already subscribed is a mark that arrives after
                        // the decision it was meant to help with (committente, Fase 28b).
                        headlineContent = { SkyHeadline(name, job.photographic) },
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
                            toggle(job.id)
                        }
                    )
                }
            }
            // Back to the four defaults (committente, 20 ott 2026): a list touched by hand
            // has a way back, offered only while it differs from a fresh install's — a row
            // that changes nothing is not offered — and undone in place while the sheet is
            // open, the previous file put back whole, leads included.
            val previous = resetFrom
            if (query.isBlank() && (previous != null || differsFromDefaults(subscriptions))) {
                item {
                    ListItem(
                        headlineContent = {
                            Text(stringResource(if (previous != null) R.string.sky_catalog_reset_done else R.string.sky_catalog_reset))
                        },
                        supportingContent = if (previous == null) {
                            { Text(stringResource(R.string.sky_catalog_reset_note)) }
                        } else {
                            null
                        },
                        trailingContent = if (previous != null) {
                            {
                                TextButton(onClick = { onSet(previous); resetFrom = null }) {
                                    Text(stringResource(R.string.action_undo))
                                }
                            }
                        } else {
                            null
                        },
                        modifier = Modifier
                            .padding(top = GroupTop)
                            .then(
                                if (previous == null) {
                                    Modifier.clickable {
                                        resetFrom = subscriptions
                                        onSet(SkyJobCatalog.defaults.map { SkySubscription(it.id) })
                                    }
                                } else {
                                    Modifier
                                }
                            )
                    )
                }
            }
        }
    }
}

/** Whether the file is no longer a fresh install's: other lines, or a line with a lead or
 * commented out. The catalog's way back is offered only then. */
internal fun differsFromDefaults(subscriptions: List<SkySubscription>): Boolean =
    subscriptions.map { it.jobId }.toSet() != SkyJobCatalog.defaults.map { it.id }.toSet() ||
        subscriptions.any { !it.enabled || it.notifyLeadMinutes != null }

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
private fun matchesQuery(job: SkyJob, query: String): Boolean = matchesSearch(
    query = query,
    name = stringResource(SkyText.nameRes(job.id)),
    explanation = stringResource(SkyText.explanationRes(job.id)),
    photographic = job.photographic,
    photoTerms = stringResource(R.string.sky_catalog_search_photo_terms)
)

/**
 * The matching itself, pure so a test can reach it (Fase 28b).
 *
 * **Why the camera is matched off the FLAG and not off a word in the prose.** The
 * committente asked whether «da fotografare» should go into the one-line explanation
 * of the nine, so that searching «foto» would find them. The goal is right and the
 * mechanism would not have been: those one-liners exist to say what a thing IS — «la
 * sera in cui la luna piena sorge mentre il cielo è ancora colorato» is a definition,
 * and «, da fotografare» bolted onto it is worse prose that repeats the glyph sitting
 * on the very same row. Worse, the word would become a SECOND source of truth for
 * something [SkyJob.photographic] already knows, free to drift from it the first time
 * somebody edits one of nine strings — the same reason the guide's «when it happens»
 * lines are read off the job rather than written twice.
 *
 * So the flag answers the query directly. [photoTerms] is a space-separated list of
 * the words a reader might type for it, per language, and a term matches on its
 * PREFIX from three letters up: «fot» finds them, «a» does not find everything.
 */
internal fun matchesSearch(
    query: String,
    name: String,
    explanation: String,
    photographic: Boolean,
    photoTerms: String
): Boolean {
    // Trimmed, not just folded: a field a reader has typed a space into is a field
    // with no query in it, and «   » is not a substring anybody meant to look for.
    val needle = query.trim().foldForSearch()
    if (needle.isEmpty()) return true
    if (needle in name.foldForSearch() || needle in explanation.foldForSearch()) return true
    if (!photographic || needle.length < MIN_TERM_LENGTH) return false
    return photoTerms.foldForSearch().split(' ').any { it.isNotEmpty() && it.startsWith(needle) }
}

/** Below three letters a prefix matches half the dictionary and teaches nothing. */
private const val MIN_TERM_LENGTH = 3

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

/**
 * An event's page opened from a row of the agenda: the third door to the guide, and
 * the one where the question most often arrives — «Ora blu · 19:42», and what is that.
 * Most of what the agenda shows was never picked from the catalog (the default
 * moments, the calendar for everybody), so for those rows it was the only door that
 * did not mean knowing the name and going to look it up.
 *
 * A sheet over the agenda, like the catalog's page and for the same reasons: the list
 * under it stays where it was, and the page carries the one button that acts on what
 * you have just read. Here that is mostly «Togli dai miei momenti» — reading about a
 * moment you did not know you had, and deciding you do not want it, is the same
 * gesture, where before removal meant opening the catalog and finding the row again.
 * A row of the calendar for everybody (or a page reached through «see also») is not
 * subscribed, and the same button adds it.
 *
 * A related event takes this page's place instead of piling on top of it — the rule
 * the full-screen guide follows — so back always closes the sheet onto the agenda.
 * Acting closes it too: the agenda itself is the receipt, a row gone or a row added.
 */
/** Where a row of the agenda says to look: its bearing, and its time line for the
 * compass's header. */
internal data class LookTarget(val bearingDeg: Double, val whenLine: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AgendaPageSheet(
    jobId: String,
    onOpenRelated: (String) -> Unit,
    onDismiss: () -> Unit,
    /**
     * Whether the moment is one of the reader's, and the two gestures that change it: the
     * page's one button, on the Sky. Null on Today (review of Today, 15 ott 2026), whose
     * rows open the same page to say what the moment is and where to look, and subscribe
     * to nothing: following a moment is the Sky's to do.
     */
    subscription: AgendaSubscription? = null,
    /**
     * The row's direction, when it has one (14 ott 2026): the page then opens with
     * «Dove guardare», whose button turns the sheet into the compass. A row with no
     * bearing — most of them, by design ([SkySights.bearing]) — has no compass to offer.
     */
    look: LookTarget? = null,
    coordinates: Coordinates? = null
) {
    // An id the catalog no longer carries (restored across an update) has no true
    // page to draw: close rather than open an empty sheet.
    val job = SkyJobCatalog.byId(jobId) ?: run {
        LaunchedEffect(jobId) { onDismiss() }
        return
    }
    val sheetState = rememberModalBottomSheetState()
    var compassOpen by rememberSaveable(jobId) { mutableStateOf(false) }
    ChiaroSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        val direction = look?.let { stringResource(SkyText.bearingRes(it.bearingDeg)) }
        if (compassOpen && look != null && direction != null) {
            // Back returns to the page, as the catalog's page returns to its list; the
            // sheet opens all the way, because a dial in half a sheet is half a dial.
            BackHandler { compassOpen = false }
            LaunchedEffect(Unit) { sheetState.expand() }
            CompassView(
                name = stringResource(SkyText.nameRes(job.id)),
                whenLine = look.whenLine,
                direction = direction,
                bearingDeg = look.bearingDeg,
                icon = jobIcon(job),
                coordinates = coordinates,
                onBack = { compassOpen = false }
            )
            return@ChiaroSheet
        }
        SkyEventPage(
            job = job,
            onOpenRelated = onOpenRelated,
            header = direction?.let { { LookBlock(direction = it, onOpen = { compassOpen = true }) } },
            action = subscription?.let { sub ->
                {
                    CatalogAction(
                        subscribed = sub.subscribed,
                        onClick = {
                            if (sub.subscribed) sub.onRemove(job.id) else sub.onAdd(job.id)
                            onDismiss()
                        }
                    )
                }
            }
        )
    }
}

/** A moment's page's one button, on the Sky: whether the moment is the reader's, and how to
 * add it or take it away. */
internal class AgendaSubscription(
    val subscribed: Boolean,
    val onAdd: (String) -> Unit,
    val onRemove: (String) -> Unit
)

/**
 * «Dove guardare» at the top of a row's page: the direction the row prints in its
 * supporting line, said once more where there is room for the button that turns it into
 * a needle. The compass is behind a tap rather than running on the page because the page
 * is mostly read, and a sensor listening while somebody reads about earthshine is a
 * battery spent on nothing.
 */
@Composable
internal fun LookBlock(direction: String, onOpen: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(
                    painter = painterResource(R.drawable.ic_compass),
                    contentDescription = null, // the title beside it says it
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(stringResource(R.string.sky_look_title), style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = stringResource(R.string.sky_look_towards, direction),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            FilledTonalButton(onClick = onOpen, modifier = Modifier.align(Alignment.End)) {
                Text(stringResource(R.string.sky_look_open))
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
internal fun LeadPickerDialog(
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
                // What used to be the footnote of the page's «Promemoria» section, said
                // where the reader is choosing a time (review point 5, 14 ott 2026); and,
                // on a moment's bell, where the default it can follow is kept now.
                Text(
                    text = listOfNotNull(
                        stringResource(R.string.sky_reminders_note),
                        if (perMoment) stringResource(R.string.sky_lead_default_where) else null
                    ).joinToString(" "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

/** "30 minuti prima", "1 ora prima", or the plain off. */
@Composable
internal fun leadLabel(lead: SkyLead): String = when (lead) {
    SkyLead.OFF -> stringResource(R.string.sky_lead_off)
    SkyLead.FIFTEEN -> pluralStringResource(R.plurals.sky_lead_minutes, 15, 15)
    SkyLead.THIRTY -> pluralStringResource(R.plurals.sky_lead_minutes, 30, 30)
    SkyLead.ONE_HOUR -> pluralStringResource(R.plurals.sky_lead_hours, 1, 1)
    SkyLead.THREE_HOURS -> pluralStringResource(R.plurals.sky_lead_hours, 3, 3)
    SkyLead.ONE_DAY -> stringResource(R.string.sky_lead_one_day)
}
