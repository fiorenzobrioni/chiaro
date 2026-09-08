package com.callbackdev.chiaro.ui.today

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.domain.AlertEngine
import com.callbackdev.chiaro.domain.model.PollenLevel
import com.callbackdev.chiaro.domain.model.WeatherReport
import com.callbackdev.chiaro.domain.settings.UnitSettings
import com.callbackdev.chiaro.ui.components.DayRow
import com.callbackdev.chiaro.ui.components.DaylightRibbon
import com.callbackdev.chiaro.ui.components.FreshnessChip
import com.callbackdev.chiaro.ui.components.HourCell
import com.callbackdev.chiaro.ui.components.HourStrip
import com.callbackdev.chiaro.ui.components.MetricTile
import com.callbackdev.chiaro.ui.sky.SkyText
import com.callbackdev.chiaro.ui.components.RainChart
import com.callbackdev.chiaro.ui.components.RainHour
import com.callbackdev.chiaro.ui.components.SkyCanvas
import com.callbackdev.chiaro.ui.components.SkyCanvasTopScrimEnd
import com.callbackdev.chiaro.ui.components.WindArrow
import com.callbackdev.chiaro.ui.firstrun.gpsErrorText
import com.callbackdev.chiaro.ui.format.Formats
import com.callbackdev.chiaro.ui.icons.ChiaroIcons
import com.callbackdev.chiaro.ui.icons.ConditionGlyph
import com.callbackdev.chiaro.ui.icons.LocalMotionPaused
import com.callbackdev.chiaro.ui.places.PlacesSheet
import com.callbackdev.chiaro.ui.places.PlacesViewModel
import com.callbackdev.chiaro.ui.theme.ChiaroMotion
import com.callbackdev.chiaro.ui.theme.ChiaroTheme
import com.callbackdev.chiaro.ui.theme.reducedMotion
import com.callbackdev.chiaro.ui.theme.reflowForText
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.StateFlow

/**
 * Today (VISION §5.2): the canvas, the sentence, the hours, the day, the week, the
 * details — one vertical scroll, cached content first, and nothing on it the data did
 * not say.
 */
@Composable
fun TodayRoute(
    onOpenSettings: () -> Unit,
    onOpenGuide: () -> Unit,
    onOpenJournal: () -> Unit,
    todayViewModel: TodayViewModel = viewModel(factory = TodayViewModel.Factory),
    placesViewModel: PlacesViewModel = viewModel(factory = PlacesViewModel.Factory)
) {
    val pager by todayViewModel.pager.collectAsStateWithLifecycle()
    val units by todayViewModel.units.collectAsStateWithLifecycle()
    val locating by todayViewModel.locating.collectAsStateWithLifecycle()
    val locationError by todayViewModel.locationError.collectAsStateWithLifecycle()
    val guideCard by todayViewModel.guideCardVisible.collectAsStateWithLifecycle()
    // Resolved here, in composition, because a snackbar is shown from a coroutine and
    // a coroutine has no Resources: what travels down is the sentence, already said.
    val locationErrorMessage = locationError?.let { stringResource(gpsErrorText(it)) }
    var placesOpen by remember { mutableStateOf(false) }
    // Opening the guide is what the card was for: the two roads share the exit.
    val openGuideFromCard = {
        todayViewModel.dismissGuideCard()
        onOpenGuide()
    }

    when (val model = pager) {
        // The stores have not answered yet: a skeleton under a bare header, never a
        // wrong screen for one frame.
        null -> GlobalFrame(onOpenPlaces = { placesOpen = true }, onOpenSettings = onOpenSettings) {
            TodaySkeleton()
        }
        else -> if (model.pages.isEmpty()) {
            GlobalFrame(onOpenPlaces = { placesOpen = true }, onOpenSettings = onOpenSettings) {
                NoPlaceState(onOpenPlaces = { placesOpen = true })
            }
        } else {
            PagedToday(
                model = model,
                units = units,
                locating = locating,
                locationErrorMessage = locationErrorMessage,
                guideCardVisible = guideCard,
                stateFor = todayViewModel::stateFor,
                onRefresh = todayViewModel::refresh,
                onSettled = todayViewModel::setActive,
                onResumed = todayViewModel::resumed,
                onLocationErrorShown = todayViewModel::dismissLocationError,
                onOpenPlaces = { placesOpen = true },
                onOpenSettings = onOpenSettings,
                onOpenGuide = openGuideFromCard,
                onOpenJournal = onOpenJournal,
                onDismissGuideCard = todayViewModel::dismissGuideCard
            )
        }
    }
    if (placesOpen) {
        PlacesSheet(viewModel = placesViewModel, onDismiss = { placesOpen = false })
    }
}

/**
 * The pager between places (VISION §5.1): one page per saved place, plus the device
 * position while GPS is on. Settling on a page IS selecting it — the pager and the
 * sheet write the same store, so they can never disagree about what is active.
 *
 * There is deliberately no app bar (device decision, 2 set): the place row lives ON
 * the sky it labels, over the canvas' top scrim, and swiping to another place carries
 * its name with it. The §8.1 collapse — the row and the temperature condensing into a
 * persistent bar on scroll — remains the motion pass' work.
 */
@Composable
private fun PagedToday(
    model: PagerModel,
    units: UnitSettings,
    locating: Boolean,
    locationErrorMessage: String?,
    guideCardVisible: Boolean?,
    stateFor: (PlacePage) -> StateFlow<TodayUiState>,
    onRefresh: (PlacePage) -> Unit,
    onSettled: (PlacePage) -> Unit,
    onResumed: (PlacePage) -> Unit,
    onLocationErrorShown: () -> Unit,
    onOpenPlaces: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenGuide: () -> Unit,
    onOpenJournal: () -> Unit,
    onDismissGuideCard: () -> Unit
) {
    val pages by rememberUpdatedState(model.pages)
    val pagerState = rememberPagerState(
        initialPage = model.activeIndex.coerceIn(0, model.pages.lastIndex)
    ) { pages.size }

    // Selection made elsewhere (the sheet, a removal) → the pager follows.
    val reduced = reducedMotion()
    LaunchedEffect(model.activeIndex, pages.size, reduced) {
        val target = model.activeIndex
        if (target in pages.indices && target != pagerState.currentPage &&
            !pagerState.isScrollInProgress
        ) {
            // The page arrives either way; with motion off it arrives without the
            // sideways travel, which on a full-screen pager is the largest movement
            // in the app (§7).
            if (reduced) pagerState.scrollToPage(target) else pagerState.animateScrollToPage(target)
        }
    }
    // The pager settled → that is the selection now.
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { settled ->
            pages.getOrNull(settled)?.let(onSettled)
        }
    }
    // Coming back to the front is the moment the position is most likely to be wrong,
    // and the settle above does not fire again on a warm resume. Silent and gated:
    // usually it costs nothing.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        pages.getOrNull(pagerState.currentPage)?.let(onResumed)
    }

    // A fix the reader asked for and did not get, said once. Everything else about a
    // failed fix stays silent by design.
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(locationErrorMessage) {
        val message = locationErrorMessage ?: return@LaunchedEffect
        snackbar.showSnackbar(message = message, withDismissAction = true)
        onLocationErrorShown()
    }

    // The status bar icons follow what is under them: white while the canvas' top
    // scrim still backs the bar, theme ink over the plain states AND once the scroll
    // has carried the scrim past it — white icons over scrolled-up light content
    // were unreadable (device report, 3 set).
    val currentPage = pages.getOrNull(pagerState.currentPage.coerceIn(0, pages.lastIndex))
    val currentState = currentPage?.let { stateFor(it).collectAsStateWithLifecycle().value }
    var canvasBehindBar by remember { mutableStateOf(true) }
    StatusBarIcons(overCanvas = currentState is TodayUiState.Content && canvasBehindBar)

    Surface(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            // A swipe between places moves whole pages: the weather holds still on both
            // of them while it does (DESIGN §7.1); each page adds its own list's scroll.
            CompositionLocalProvider(LocalMotionPaused provides pagerState.isScrollInProgress) {
            HorizontalPager(
                state = pagerState,
                key = { pages[it].key },
                modifier = Modifier.fillMaxSize()
            ) { index ->
                val page = pages.getOrNull(index) ?: return@HorizontalPager
                val state by stateFor(page).collectAsStateWithLifecycle()
                TodayPage(
                    state = state,
                    title = pageTitle(page),
                    isGps = page is PlacePage.Gps,
                    dots = if (pages.size > 1) index to pages.size else null,
                    units = units,
                    // Taking the position again IS the refresh on that page, so a
                    // pull there keeps spinning until the fix lands. Only a pull:
                    // `locating` is never set by the automatic paths.
                    locating = locating && page is PlacePage.Gps,
                    guideCardVisible = guideCardVisible,
                    onRefresh = { onRefresh(page) },
                    onOpenPlaces = onOpenPlaces,
                    onOpenSettings = onOpenSettings,
                    onOpenGuide = onOpenGuide,
                    onOpenJournal = onOpenJournal,
                    onDismissGuideCard = onDismissGuideCard,
                    isCurrent = index == pagerState.currentPage,
                    onCanvasBehindBar = { canvasBehindBar = it }
                )
            }
            }
            SnackbarHost(
                hostState = snackbar,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
            )
        }
    }
}

@Composable
private fun pageTitle(page: PlacePage): String = when (page) {
    is PlacePage.Gps -> page.lastFix?.name ?: stringResource(R.string.places_gps_title)
    is PlacePage.Saved -> page.city.name
}

/** White over the canvas needs dark icons off; the plain states follow the theme —
 * the APPLIED theme, read off the surface itself, because since Fase 4 the reader can
 * force light or dark against the system and the icons must follow the choice. */
@Composable
private fun StatusBarIcons(overCanvas: Boolean) {
    val activity = LocalActivity.current
    val darkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    DisposableEffect(overCanvas, darkTheme, activity) {
        val controller = activity?.window?.let { window ->
            WindowCompat.getInsetsController(window, window.decorView)
        }
        controller?.isAppearanceLightStatusBars = !overCanvas && !darkTheme
        // Leaving Today — a tab switch, an overlay — hands the bar back to the theme.
        onDispose { controller?.isAppearanceLightStatusBars = !darkTheme }
    }
}

@Composable
private fun TodayPage(
    state: TodayUiState,
    title: String,
    isGps: Boolean,
    dots: Pair<Int, Int>?,
    units: UnitSettings,
    locating: Boolean,
    guideCardVisible: Boolean?,
    onRefresh: () -> Unit,
    onOpenPlaces: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenGuide: () -> Unit,
    onOpenJournal: () -> Unit,
    onDismissGuideCard: () -> Unit,
    isCurrent: Boolean,
    onCanvasBehindBar: (Boolean) -> Unit
) {
    when (state) {
        is TodayUiState.Content ->
            ContentState(
                state, title, isGps, dots, units, locating, guideCardVisible,
                onRefresh, onOpenPlaces, onOpenSettings, onOpenGuide, onOpenJournal,
                onDismissGuideCard, isCurrent, onCanvasBehindBar
            )
        else -> Column(modifier = Modifier.fillMaxSize()) {
            PlaceHeader(
                title = title,
                isGps = isGps,
                // No report, so no timezone and no honest hour: the phone's own would
                // be a guess wearing the place's name.
                localNow = null,
                dots = dots,
                onOpenPlaces = onOpenPlaces,
                onOpenSettings = onOpenSettings,
                contentColor = MaterialTheme.colorScheme.onSurface,
                dotInactive = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = HeaderVerticalPadding)
            )
            when (state) {
                TodayUiState.Starting -> TodaySkeleton()
                TodayUiState.NoPlace -> NoPlaceState(onOpenPlaces)
                is TodayUiState.Empty -> EmptyState(state, onRefresh)
                is TodayUiState.Content -> Unit // handled above
            }
        }
    }
}

/**
 * The place row's vertical padding, the same on the canvas and on a plain surface: it
 * was 8dp on the sky and 12dp everywhere else, so swiping from a place with a report to
 * one without moved the title 4dp (review, 8 set 2026).
 */
private val HeaderVerticalPadding = 8.dp

/** The frame for the two page-less situations: not started yet, and no place at all. */
@Composable
private fun GlobalFrame(
    onOpenPlaces: () -> Unit,
    onOpenSettings: () -> Unit,
    content: @Composable () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            PlaceHeader(
                title = stringResource(R.string.app_name),
                isGps = false,
                localNow = null,
                dots = null,
                onOpenPlaces = onOpenPlaces,
                onOpenSettings = onOpenSettings,
                contentColor = MaterialTheme.colorScheme.onSurface,
                dotInactive = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = HeaderVerticalPadding)
            )
            content()
        }
    }
}

/**
 * The place switcher: name, chevron, and the pager dots when there is more than one
 * page. One composable for both grounds — white over the canvas' scrim, theme ink on
 * a plain surface — so the two can never drift apart in shape.
 *
 * [isGps] draws the position pin before the name: a saved "Cavenago" and the GPS fix
 * standing in Cavenago would otherwise be two identical pages, and where a number
 * comes from is part of its truth (device request, 2 set). The pin carries its word
 * through the row's description, never alone.
 *
 * [localNow] is the other half of that same question, and appears on exactly the pages
 * the pin does not (device request, 7 set): the day and the hour IN this place. On the
 * position page it would be the phone's own clock printed under the status bar that
 * already shows it; on any other place it is the one thing the reader cannot look up —
 * Palermo and Reykjavík are not on the same hour, and some days not on the same date.
 * It is a fact about the place, not about the data: the page's last line still says
 * when the numbers arrived, in the same timezone as this one and in its own words.
 */
@Composable
private fun PlaceHeader(
    title: String,
    isGps: Boolean,
    localNow: String?,
    dots: Pair<Int, Int>?,
    onOpenPlaces: () -> Unit,
    onOpenSettings: () -> Unit,
    contentColor: Color,
    dotInactive: Color,
    modifier: Modifier = Modifier
) {
    val spoken = if (isGps) stringResource(R.string.header_gps_desc, title) else title
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .clickable(onClick = onOpenPlaces)
                    .semantics { contentDescription = spoken }
            ) {
                if (isGps) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = null, // the row speaks once, GPS included
                        tint = contentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(title, style = MaterialTheme.typography.titleLarge, color = contentColor)
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowDown,
                    contentDescription = stringResource(R.string.place_switcher_action),
                    tint = contentColor
                )
            }
            // Outside the tappable row: it is a fact, not a way in. Full strength
            // rather than a faded white — this sits over the sky, where §3.6's scrim
            // is measured against ink at full opacity, and the size is what makes it
            // secondary.
            localNow?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelLarge,
                    color = contentColor
                )
            }
            dots?.let { (selected, count) ->
                PageDots(
                    selected = selected,
                    count = count,
                    active = contentColor,
                    inactive = dotInactive
                )
            }
        }
        // The gear of VISION §5.1: settings are visited once a month, so they get an
        // icon by the place row, not a tab. Same two grounds as the row itself.
        IconButton(onClick = onOpenSettings) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = stringResource(R.string.settings_title),
                tint = contentColor
            )
        }
    }
}

/** The place dots of VISION §5.1: position among the pages, at a glance. */
@Composable
private fun PageDots(selected: Int, count: Int, active: Color, inactive: Color) {
    val description = stringResource(R.string.pager_dots_desc, selected + 1, count)
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .padding(top = 4.dp)
            .semantics { contentDescription = description }
    ) {
        repeat(count) { index ->
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (index == selected) active else inactive)
            )
        }
    }
}

// ---------------------------------------------------------------------------------
// The four states. §8.11: an empty state names its one action; a skeleton is visibly
// a skeleton; an error says what failed in words and offers the retry.
// ---------------------------------------------------------------------------------

@Composable
private fun NoPlaceState(onOpenPlaces: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.empty_no_place_title),
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = stringResource(R.string.empty_no_place_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        TextButton(onClick = onOpenPlaces) {
            Text(stringResource(R.string.empty_no_place_action))
        }
    }
}

@Composable
private fun EmptyState(state: TodayUiState.Empty, onRefresh: () -> Unit) {
    Column {
        state.error?.let { ErrorBanner(it, onRefresh) }
        if (state.error == null) {
            Text(
                text = stringResource(R.string.empty_no_data_body, state.city.name),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        }
        TodaySkeleton()
    }
}

/**
 * The plain place row that stands over the skeleton: [HeaderVerticalPadding] above and
 * below a row the 48dp settings button makes 48 tall. The skeleton's sky block is the
 * canvas' height minus this, so the row and the block together end exactly where the
 * real canvas ends and nothing jumps when the report lands.
 */
private val PlainHeaderHeight = 48.dp + HeaderVerticalPadding * 2

/** One hour cell at 100% type, quoted from `HourStrip`: 16 (hour) + 6 + 42 (icon) + 6 +
 * 20 (temperature) + 6 + 16 (rain). */
private val SkeletonHourCellHeight = 112.dp

/**
 * Blocks of `surfaceContainerHigh` in the shape of the real layout: unmistakably not
 * data (§1.1 — no placeholder may render as a value, so none of these carries text).
 *
 * In the shape of the real layout, and re-measured against it on 8 set 2026: the sky
 * block had kept the 28dp bottom corners the canvas lost on 4 set and a 240dp height
 * from before the canvas reached the status bar, and the hour cells were 16dp shorter
 * than the strip had grown. A skeleton that is the wrong shape is a small lie told for
 * one second; still a lie.
 */
@Composable
private fun TodaySkeleton() {
    val block = MaterialTheme.colorScheme.surfaceContainerHigh
    Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Straight edges, like the canvas: the sky is the ground, not a card.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(CanvasBaseHeight - PlainHeaderHeight)
                .background(block)
        )
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeat(6) {
                Box(
                    modifier = Modifier
                        .width(56.dp)
                        .height(SkeletonHourCellHeight)
                        .background(block, MaterialTheme.shapes.medium)
                )
            }
        }
        repeat(3) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(20.dp)
                    .background(block, MaterialTheme.shapes.small)
            )
        }
    }
}

@Composable
private fun ErrorBanner(error: TodayError, onRetry: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(
                    when (error) {
                        TodayError.OFFLINE -> R.string.error_offline
                        TodayError.SERVICE -> R.string.error_service
                        TodayError.UNKNOWN -> R.string.error_unknown
                    }
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onRetry) {
                Text(stringResource(R.string.error_retry))
            }
        }
    }
}

// ---------------------------------------------------------------------------------
// Content
// ---------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContentState(
    content: TodayUiState.Content,
    title: String,
    isGps: Boolean,
    dots: Pair<Int, Int>?,
    units: UnitSettings,
    locating: Boolean,
    guideCardVisible: Boolean?,
    onRefresh: () -> Unit,
    onOpenPlaces: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenGuide: () -> Unit,
    onOpenJournal: () -> Unit,
    onDismissGuideCard: () -> Unit,
    isCurrent: Boolean,
    onCanvasBehindBar: (Boolean) -> Unit
) {
    val locale = Locale.getDefault()
    val is24h = android.text.format.DateFormat.is24HourFormat(LocalContext.current)
    val timeFmt = remember(locale, is24h) { Formats.timeFormatter(is24h, locale) }

    // White status-bar icons hold only while the canvas' top scrim band is still
    // behind the bar: past that offset the sky under the clock is unscrimmed, then
    // gone altogether, and the bar must return to theme ink. The band is a fraction
    // of the canvas, and the canvas grows with its text (8 set 2026), so the flip is
    // measured on the canvas item's real height and falls back to the floor only
    // before the first layout.
    val listState = rememberLazyListState()
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val density = LocalDensity.current
    val statusTopPx = with(density) { statusTop.toPx() }
    val canvasFloorPx = with(density) { (CanvasBaseHeight + statusTop).toPx() }
    val behindBar by remember(statusTopPx, canvasFloorPx) {
        derivedStateOf {
            val canvasPx = listState.layoutInfo.visibleItemsInfo.firstOrNull()
                ?.takeIf { it.index == 0 }?.size?.toFloat() ?: canvasFloorPx
            val flipAtPx = (canvasPx * SkyCanvasTopScrimEnd - statusTopPx).coerceAtLeast(0f)
            listState.firstVisibleItemIndex == 0 &&
                listState.firstVisibleItemScrollOffset <= flipAtPx
        }
    }
    LaunchedEffect(isCurrent, behindBar) { if (isCurrent) onCanvasBehindBar(behindBar) }

    // Which day of the week is open, hoisted out of the week because the week is no
    // longer one composable: each row is its own lazy item (8 set 2026), so the seven
    // moving icons arrive one row at a time instead of in the frame the section enters.
    // Reset when the week itself changes, as before.
    var expandedDay by remember(content.week.firstOrNull()?.forecast?.date) {
        mutableStateOf<java.time.LocalDate?>(null)
    }

    // "What changed", written out here because a `LazyListScope` cannot call a
    // composable and these sentences need the string table. A revision whose fields
    // this screen has no words for (a condition code, say) produces NO line: on device
    // it printed as "Wednesday 9's forecast changed:" with nothing after the colon,
    // which is §1.1's dash in a card by another route — and it spent one of the three
    // lines the section is allowed.
    val changed = content.whatChanged.mapNotNull { shift ->
        val details = com.callbackdev.chiaro.ui.journal.JournalText.shiftDetails(
            shift.shifts, units, locale
        )
        if (details.isBlank()) null
        else com.callbackdev.chiaro.ui.journal.JournalText.shiftHeadline(shift, locale) +
            ": " + details
    }

    // Both halves are the reader's own gesture and nothing else (Fase 3b): the pull
    // indicator means "doing what you just asked", so an automatic fetch and an
    // automatic re-fix leave it alone — VISION §5.2, refresh silent.
    // While the page moves the weather holds still (DESIGN §7.1, 9 set 2026): the
    // moving icons hide behind their still drawings for as long as the list is in
    // motion, so the RenderThread spends its frames on the scroll and not on fourteen
    // vector loops. The pager's swipe says the same thing one level up.
    val pageMoving = LocalMotionPaused.current || listState.isScrollInProgress
    CompositionLocalProvider(LocalMotionPaused provides pageMoving) {
    PullToRefreshBox(
        isRefreshing = content.userRefreshing || locating,
        onRefresh = onRefresh
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = WindowInsets.navigationBars.asPaddingValues()
        ) {
            item {
                CanvasHeader(
                    content, title, isGps, dots, units, timeFmt, locale,
                    onOpenPlaces, onOpenSettings
                )
            }

            if (content.error != null) {
                item { ErrorBanner(content.error, onRefresh) }
            }
            if (content.isStale) {
                item {
                    FreshnessChip(
                        age = freshnessAge(content.lastSync),
                        onRetry = onRefresh,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
            // The one-time pointer to the guide (VISION 5.7): true shows it, false
            // and null both draw nothing - a card that flashed while the store was
            // still answering would be shown to everyone and dismissed by nobody.
            if (guideCardVisible == true) {
                item { GuideCard(onOpen = onOpenGuide, onDismiss = onDismissGuideCard) }
            }

            item { SectionTitle(stringResource(R.string.section_next_hours)) }
            item { NextHours(content, units, is24h, timeFmt, locale) }

            if (content.timeline.isNotEmpty()) {
                item { SectionTitle(stringResource(R.string.section_rest_of_day)) }
                item { RestOfDay(content, timeFmt) }
            }

            if (content.week.isNotEmpty()) {
                item { SectionTitle(stringResource(R.string.section_week)) }
                weekRows(
                    content = content,
                    units = units,
                    is24h = is24h,
                    locale = locale,
                    expandedDay = expandedDay,
                    onToggle = { date -> expandedDay = if (expandedDay == date) null else date }
                )
            }

            // VISION §5.2.5 — when something did change: two or three sentences,
            // tapping opens the Journal where the whole story lives. AFTER the week
            // (asked on device, 6 set, and it is the right order): every one of these
            // sentences is about a day further out — "Wednesday's forecast changed" —
            // so it used to name days the reader had not been shown yet, and it split
            // the two sections about today from the one about the days ahead.
            if (changed.isNotEmpty()) {
                item { SectionTitle(stringResource(R.string.today_changed_title)) }
                item { WhatChanged(lines = changed, onOpenJournal = onOpenJournal) }
            }

            item { SectionTitle(stringResource(R.string.section_details)) }
            item { Details(content.report, units, locale) }
            item { DataFooter(content, timeFmt) }
        }
    }
    }
}

/**
 * The page's last line: when these numbers arrived, and where they came from.
 *
 * Until now the page said when only when it had bad news — the freshness chip appears
 * past twice the polling interval and is silent otherwise — so a reader who simply
 * wanted to know how recent the hero was had nowhere to look. This is the quiet half
 * of the same fact, and the two do different jobs: the chip is a warning and carries a
 * relative age ("7 hours ago") plus a way out; this is a statement and carries the
 * clock time, which is what you check against your own watch.
 *
 * At the FOOT of the page on purpose. A timestamp is reference, not headline, and the
 * top of this screen is spoken for by the sky, the temperature and the sentence of the
 * day — VISION §5.2's order, one thing before any number. It also gives the page an
 * ending and puts the attribution where a colophon goes.
 *
 * The hour is the PLACE's, like every other hour on this screen. It is a defensible
 * either way — the fetch happened on the reader's clock — but a footer in a different
 * timezone from the strip right above it is a line that has to be read twice, and for
 * the overwhelming case (the place you are in) the two are the same hour anyway.
 */
@Composable
private fun DataFooter(content: TodayUiState.Content, timeFmt: DateTimeFormatter) {
    Text(
        text = stringResource(
            R.string.today_updated_footer,
            LocalDateTime.ofInstant(content.lastSync, content.zone).format(timeFmt)
        ),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    )
}

/**
 * The same header rhythm every other screen keeps (DESIGN §6, `SectionTop`), reached
 * by a different road: this list spaces its items by 12dp, so 12 of the section's 24
 * are already paid and the header adds the other 12 itself. The 12dp under it is that
 * same list gap — which is exactly what a 4dp bottom plus a row's 8dp of padding comes
 * to on the screens that have no list spacing.
 */
@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(start = 16.dp, top = 12.dp)
    )
}

/**
 * The canvas' height before the status bar's own height is added on top — a FLOOR since
 * 8 set 2026, not a size. Everything on the canvas is measured in sp (the 64sp
 * temperature, a 22sp sentence that runs to two lines in Italian) and the block was
 * measured in dp: at 100% type a two-line sentence left 2dp before the hero climbed into
 * the place row, and at 115% the two overlapped by 30dp. The canvas is now at least this
 * tall and grows with what it holds — see [CanvasHeader].
 */
private val CanvasBaseHeight = 280.dp

@Composable
private fun CanvasHeader(
    content: TodayUiState.Content,
    title: String,
    isGps: Boolean,
    dots: Pair<Int, Int>?,
    units: UnitSettings,
    timeFmt: DateTimeFormatter,
    locale: Locale,
    onOpenPlaces: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val sky = content.sky
    val current = content.report.current
    // The canvas owns the top edge of the screen: its height grows by the status bar
    // so the sky sits behind the clock, over the top scrim that keeps both legible.
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val floor = CanvasBaseHeight + statusTop
    SkyCanvas(
        gradient = ChiaroTheme.sky.gradient(
            sunAltitudeDeg = sky.sunAltitudeDeg,
            cloudPct = sky.cloudPct,
            precipPct = sky.precipPct,
            moonIllumination = sky.moonIllumination,
            moonAltitudeDeg = sky.moonAltitudeDeg
        ),
        minHeight = floor
    ) {
        // One column with the place row at the top and the hero at the bottom, rather
        // than two children aligned to opposite edges of a fixed box: `SpaceBetween` on
        // a floor keeps them apart when there is room and stacks them when there is
        // not, and a fixed box let them overlap instead.
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(min = floor),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            PlaceHeader(
                title = title,
                isGps = isGps,
                localNow = if (isGps) {
                    null
                } else {
                    "${Formats.dayLong(content.now.toLocalDate(), locale)} · " +
                        content.now.format(timeFmt)
                },
                dots = dots,
                onOpenPlaces = onOpenPlaces,
                onOpenSettings = onOpenSettings,
                contentColor = Color.White,
                dotInactive = Color.White.copy(alpha = 0.4f),
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = HeaderVerticalPadding)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = Formats.temperature(current.tempC, units.temperature, locale, decimals = 1),
                    style = com.callbackdev.chiaro.ui.theme.HeroTemperature,
                    color = Color.White
                )
                // Two type sizes on one line align by BASELINE, not by top: top-aligned
                // they read as a mistake the moment the sizes differ (device check, 2 set).
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(WeatherText.condition(current.condition.wmoCode)),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        modifier = Modifier.alignByBaseline()
                    )
                    Text(
                        text = stringResource(
                            R.string.feels_like,
                            Formats.temperature(
                                current.feelsLikeC, units.temperature, locale, decimals = 1
                            )
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.alignByBaseline()
                    )
                }
                DaylightRibbon(
                    phases = sky.phases,
                    nowFraction = sky.nowFraction,
                    description = ribbonDescription(content, timeFmt)
                )
                headlineText(content.headline, timeFmt)?.let { sentence ->
                    Text(
                        text = sentence,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * The one-time card that points at the guide (VISION 5.7): a card in the scroll, not
 * a dialog over it - it waits its turn and never blocks the sky. Opening the guide
 * uses it up, the X waves it away, and either way it never comes back: the guide
 * itself stays one tap away in Settings forever.
 */
@Composable
private fun GuideCard(onOpen: () -> Unit, onDismiss: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 4.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.guide_card_title),
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = stringResource(R.string.guide_card_body),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.guide_card_dismiss)
                )
            }
        }
        TextButton(
            onClick = onOpen,
            modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
        ) {
            Text(stringResource(R.string.guide_card_open))
        }
    }
}

/** VISION §5.2.5: each revision as one sentence, the same words the Journal uses
 * ([JournalText]), the whole block one door to it. The sentences arrive built (see
 * the list above): a revision with no printable numbers never becomes a line. */
@Composable
private fun WhatChanged(
    lines: List<String>,
    onOpenJournal: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenJournal)
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        lines.forEach { line ->
            Text(text = line, style = MaterialTheme.typography.bodyMedium)
        }
        Text(
            text = stringResource(R.string.today_changed_open),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/** The sentence, or nothing: [HeadlineText]'s mapping, shared with the Today widget. */
@Composable
private fun headlineText(headline: Headline?, timeFmt: DateTimeFormatter): String? =
    HeadlineText.of(LocalContext.current, headline, timeFmt)

@Composable
private fun ribbonDescription(
    content: TodayUiState.Content,
    timeFmt: DateTimeFormatter
): String {
    val astro = content.report.astronomical
    return when {
        astro.sunrise != null && astro.sunset != null -> stringResource(
            R.string.ribbon_desc, astro.sunrise!!.format(timeFmt), astro.sunset!!.format(timeFmt)
        )
        astro.daylightDuration == null && astro.sunrise == null && astro.sunset == null ->
            stringResource(R.string.ribbon_desc_all_night)
        else -> stringResource(R.string.ribbon_desc_all_day)
    }
}

/** How old the data really is, in the largest unit that keeps a whole number. */
@Composable
private fun freshnessAge(lastSync: Instant): String {
    val elapsed = Duration.between(lastSync, Instant.now())
    return when {
        elapsed.toHours() < 1 -> pluralStringResource(
            R.plurals.freshness_minutes_ago, elapsed.toMinutes().toInt(), elapsed.toMinutes().toInt()
        )
        elapsed.toDays() < 1 -> pluralStringResource(
            R.plurals.freshness_hours_ago, elapsed.toHours().toInt(), elapsed.toHours().toInt()
        )
        else -> pluralStringResource(
            R.plurals.freshness_days_ago, elapsed.toDays().toInt(), elapsed.toDays().toInt()
        )
    }
}

@Composable
private fun NextHours(
    content: TodayUiState.Content,
    units: UnitSettings,
    is24h: Boolean,
    timeFmt: DateTimeFormatter,
    locale: Locale
) {
    // The page margin travels differently for the two children: the strip takes it as
    // `contentPadding` so it scrolls edge to edge (DESIGN §8.3), the chart wears it.
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HourStrip(
            hours = content.strip.map { it.toCell(units, is24h, locale) },
            contentPadding = PagePadding
        )
        // A dry run draws NO chart (device review, 4 set). Every value at zero put
        // a flat line along the bottom of a 28dp box, which read on the screen as a
        // stray divider with a hole above it — and said nothing the row of "0%" right
        // over it had not already said. §1.1: a section with no data is not drawn, and
        // a chart of nothing but zeros is one.
        val peak = content.strip.maxByOrNull { it.hour.precipChancePct ?: -1 }
        val peakPct = peak?.hour?.precipChancePct
        if (peakPct != null && peakPct > 0) {
            RainChart(
                hours = content.strip.map {
                    RainHour(
                        label = Formats.hourLabel(it.hour.time, is24h, locale),
                        pct = it.hour.precipChancePct
                    )
                },
                caption = stringResource(R.string.rain_chart_caption),
                description = stringResource(
                    R.string.rain_chart_desc,
                    content.strip.first().hour.time.format(timeFmt),
                    content.strip.last().hour.time.format(timeFmt),
                    peakPct,
                    peak.hour.time.format(timeFmt)
                ),
                modifier = Modifier.padding(PagePadding)
            )
        }
    }
}

/** The page's side margin, as the strip's `contentPadding` and the chart's padding. */
private val PagePadding = PaddingValues(horizontal = 16.dp)

@Composable
private fun StripHour.toCell(units: UnitSettings, is24h: Boolean, locale: Locale): HourCell {
    val hourLabel = Formats.hourLabel(hour.time, is24h, locale)
    val temp = Formats.temperature(hour.tempC, units.temperature, locale)
    val word = stringResource(WeatherText.condition(hour.condition.wmoCode))
    return HourCell(
        // The hour itself, not its label: unique across the strip's 24 and a day's 24.
        key = hour.time.toString(),
        hourLabel = hourLabel,
        condition = ConditionGlyph(hour.condition.wmoCode, night),
        temperature = temp,
        rainPct = hour.precipChancePct,
        rainLabel = hour.precipChancePct?.let { Formats.percent(it, locale) },
        description = stringResource(
            // Spoken as 0 only when the forecast says 0; an hour with no chance at
            // all reads without the rain clause, like the cell itself.
            R.string.hour_cell_desc, hourLabel, word, temp, hour.precipChancePct ?: 0
        )
    )
}

@Composable
private fun RestOfDay(content: TodayUiState.Content, timeFmt: DateTimeFormatter) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        content.timeline.forEach { item ->
            com.callbackdev.chiaro.ui.components.TimelineRow(
                time = item.at.format(timeFmt),
                icon = timelineIcon(item.kind),
                text = timelineText(item)
            )
        }
    }
}

@Composable
private fun timelineIcon(kind: TimelineKind) = when (kind) {
    TimelineKind.SUNRISE -> ChiaroIcons.sunrise
    TimelineKind.GOLDEN_MORNING_END -> ChiaroIcons.horizon
    TimelineKind.GOLDEN_EVENING -> ChiaroIcons.horizon
    TimelineKind.SUNSET -> ChiaroIcons.sunset
    TimelineKind.BLUE_EVENING -> ChiaroIcons.star
    TimelineKind.DARK -> ChiaroIcons.starryNight
    TimelineKind.MOONRISE -> ChiaroIcons.moonrise
    TimelineKind.MOONSET -> ChiaroIcons.moonset
    TimelineKind.RAINBOW -> ChiaroIcons.rainbow
    TimelineKind.RAIN_START -> ChiaroIcons.precipitation
    TimelineKind.RAIN_STOP -> ChiaroIcons.cloud
}

@Composable
private fun timelineText(item: TimelineItem): String = when (item.kind) {
    TimelineKind.SUNRISE -> stringResource(R.string.tl_sunrise)
    TimelineKind.GOLDEN_MORNING_END -> stringResource(R.string.tl_golden_morning_end)
    TimelineKind.GOLDEN_EVENING -> stringResource(R.string.tl_golden_evening)
    TimelineKind.SUNSET -> stringResource(R.string.tl_sunset)
    TimelineKind.BLUE_EVENING -> stringResource(R.string.tl_blue_evening)
    TimelineKind.DARK -> stringResource(R.string.tl_dark)
    TimelineKind.MOONRISE -> stringResource(R.string.tl_moonrise)
    TimelineKind.MOONSET -> stringResource(R.string.tl_moonset)
    TimelineKind.RAINBOW -> stringResource(
        R.string.timeline_rainbow,
        item.pct ?: 0,
        stringResource(SkyText.bearingRes(item.bearingDeg ?: 0.0))
    )
    TimelineKind.RAIN_START -> stringResource(R.string.tl_rain_start, item.pct ?: 0)
    TimelineKind.RAIN_STOP -> stringResource(R.string.tl_rain_stop)
}

/**
 * The week, one lazy item per day (8 set 2026). It was one item holding a column of
 * seven rows, which composed seven moving icons in the frame the section scrolled into
 * view; as seven items the list's prefetcher takes them one row ahead of the scroll. The
 * rhythm is unchanged: the column spaced its rows by 12dp and the list spaces its items
 * by the same 12dp.
 *
 * The scale is the week's own extremes, computed once here and shared by all seven rows
 * so the week has a shape (DESIGN §8.5).
 */
private fun LazyListScope.weekRows(
    content: TodayUiState.Content,
    units: UnitSettings,
    is24h: Boolean,
    locale: Locale,
    expandedDay: java.time.LocalDate?,
    onToggle: (java.time.LocalDate) -> Unit
) {
    val scaleLow = content.week.minOf { it.forecast.lowC }
    val scaleHigh = content.week.maxOf { it.forecast.highC }
    val today = content.now.toLocalDate()
    items(content.week, key = { "week-${it.forecast.date}" }) { day ->
        WeekRow(
            day = day,
            isToday = day.forecast.date == today,
            scaleLowC = scaleLow,
            scaleHighC = scaleHigh,
            units = units,
            is24h = is24h,
            locale = locale,
            expanded = expandedDay == day.forecast.date,
            onToggle = { onToggle(day.forecast.date) }
        )
    }
}

/** One day of the week and, when it is open, its hours: the row wears the page margin,
 * the strip under it takes the same margin as `contentPadding` and scrolls edge to edge
 * like the strip at the top of the page. */
@Composable
private fun WeekRow(
    day: WeekDay,
    isToday: Boolean,
    scaleLowC: Double,
    scaleHighC: Double,
    units: UnitSettings,
    is24h: Boolean,
    locale: Locale,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    val f = day.forecast
    val label = if (isToday) stringResource(R.string.week_today) else Formats.dayLabel(f.date, locale)
    val low = Formats.temperature(f.lowC, units.temperature, locale)
    val high = Formats.temperature(f.highC, units.temperature, locale)
    val reduced = reducedMotion()
    Column {
        DayRow(
            dayLabel = label,
            condition = ConditionGlyph(f.condition.wmoCode, night = false),
            rainPct = f.precipPct,
            rainLabel = f.precipPct?.let { Formats.percent(it, locale) },
            lowC = f.lowC,
            highC = f.highC,
            lowLabel = low,
            highLabel = high,
            scaleLowC = scaleLowC,
            scaleHighC = scaleHighC,
            phases = day.phases,
            // Two forms, because a day with no probability must not announce one:
            // the row that prints nothing says nothing (§1.1).
            description = f.precipPct?.let { pct ->
                stringResource(
                    R.string.week_day_desc,
                    label,
                    stringResource(WeatherText.condition(f.condition.wmoCode)),
                    low, high, pct
                )
            } ?: stringResource(
                R.string.week_day_desc_no_rain,
                label,
                stringResource(WeatherText.condition(f.condition.wmoCode)),
                low, high
            ),
            onClick = if (day.hours.isNotEmpty()) onToggle else null,
            modifier = Modifier.padding(PagePadding)
        )
        AnimatedVisibility(
            visible = expanded,
            enter = ChiaroMotion.enter(reduced),
            exit = ChiaroMotion.exit(reduced)
        ) {
            HourStrip(
                hours = day.hours.map { it.toCell(units, is24h, locale) },
                modifier = Modifier.padding(top = 4.dp),
                contentPadding = PagePadding
            )
        }
    }
}

/**
 * The details grid (DESIGN §8.6), reviewed card by card on 8 set 2026. What each tile
 * carries beyond its value and its meaning, and why:
 *
 * - **UV, humidity, air**: a track on the scale the world uses (0–11, 0–100, 0–300), so
 *   "how much" arrives before the number is read. Pressure and visibility have no such
 *   scale — one is a narrow band around 1013, the other is logarithmic — and get none.
 * - **Wind**: an arrow for where the air goes and the words for where it comes from,
 *   and the gusts on the days they matter, when the wind a body feels is the gust.
 * - **Humidity**: one tile where there were two. The dew point tile said "pleasant"
 *   under the humidity tile's "comfortable", the same fact in two cards; the dew point
 *   is the better predictor of how the air feels, so it writes the meaning line and
 *   stays on the tile as a note, and "Rugiada" — the least understood word on the
 *   screen — stops being a headline.
 * - **Air**: the index without its acronym. "AQI" was the one piece of jargon on a
 *   screen built to have none; the label already says what the number is.
 * - **Pollen**: which pollen. "High" tells an allergic reader nothing to act on,
 *   "grass" does.
 */
@Composable
private fun Details(report: WeatherReport, units: UnitSettings, locale: Locale) {
    val current = report.current
    val today = report.daily.firstOrNull()
    val tiles = buildList {
        if (today != null) {
            add(
                Tile(
                    icon = { ChiaroIcons.uv },
                    label = R.string.metric_uv,
                    value = today.uvIndexMax.toString(),
                    meaning = WeatherText.uvMeaning(today.uvIndexMax),
                    scale = today.uvIndexMax / UvScaleTop
                )
            )
        }
        val wind = current.wind
        val gusty = WeatherText.gustsMaterial(wind.speedKph, wind.gustKph)
        add(
            Tile(
                icon = { ChiaroIcons.wind },
                label = R.string.metric_wind,
                value = Formats.wind(wind.speedKph, units.windSpeed, locale),
                // On a gusty day the wind a body feels is the gust, and the advice
                // follows it: "hold on to your hat" over a steady 20 km/h is the gust
                // talking, and it is right.
                meaning = WeatherText.windMeaning(if (gusty) wind.gustKph else wind.speedKph),
                detail = { WindDirection(fromDegrees = wind.degree) },
                note = if (gusty) {
                    stringResource(
                        R.string.wind_gusts, Formats.wind(wind.gustKph, units.windSpeed, locale)
                    )
                } else {
                    null
                }
            )
        )
        add(
            Tile(
                icon = { ChiaroIcons.humidity },
                label = R.string.metric_humidity,
                value = Formats.percent(current.humidityPct, locale),
                meaning = WeatherText.dewPointMeaning(current.dewPointC),
                scale = current.humidityPct / 100f,
                note = stringResource(
                    R.string.humidity_dew_note,
                    Formats.temperature(current.dewPointC, units.temperature, locale)
                )
            )
        )
        add(
            Tile(
                icon = { ChiaroIcons.pressure },
                label = R.string.metric_pressure,
                value = Formats.pressure(current.pressureMb, locale),
                meaning = WeatherText.pressureMeaning(current.pressureMb)
            )
        )
        // §1.1 again (Fase 26): `visibility` is a model-dependent field, and a model
        // that does not carry it gets no tile rather than a tile reading 0 km.
        current.visibilityKm?.let { km ->
            add(
                Tile(
                    icon = { ChiaroIcons.visibility },
                    label = R.string.metric_visibility,
                    value = Formats.kilometers(km, locale),
                    meaning = WeatherText.visibilityMeaning(km)
                )
            )
        }
        // §1.1: data the provider does not have for here is not drawn — no dashes.
        report.airQuality?.let { air ->
            add(
                Tile(
                    icon = { ChiaroIcons.airQuality },
                    label = R.string.metric_air,
                    value = air.aqiIndex.toString(),
                    meaning = WeatherText.aqiMeaning(air.aqiIndex),
                    scale = air.aqiIndex / AqiScaleTop
                )
            )
        }
        report.pollen?.let { pollen ->
            val worst = WeatherText.pollenWorst(pollen)
            val families = WeatherText.pollenFamiliesAtWorst(pollen).map { stringResource(it) }
            add(
                Tile(
                    icon = { ChiaroIcons.pollen },
                    label = R.string.metric_pollen,
                    value = stringResource(WeatherText.pollenLevel(worst)),
                    meaning = WeatherText.pollenMeaning(worst),
                    // The families are stored lowercase so they can be listed; the
                    // line then starts like a sentence.
                    note = when (families.size) {
                        0 -> null
                        1 -> families[0]
                        2 -> stringResource(R.string.list_two, families[0], families[1])
                        else -> stringResource(
                            R.string.list_three, families[0], families[1], families[2]
                        )
                    }?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
                )
            )
        }
    }
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // §10: two columns is a budget, not a layout. At 150% the label beside the
        // 34dp icon has 84dp for a word that wants 115, and the tile becomes three
        // wrapped lines of two words. One column keeps the pair rule (a row of one is
        // still a row) and gives the label the whole width it needed all along.
        val perRow = if (reflowForText()) 1 else 2
        tiles.chunked(perRow).forEach { rowTiles ->
            // The pair shares one height: two cards whose bottoms disagree read as a
            // misalignment, not as content of different lengths (device check, 2 set).
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.height(IntrinsicSize.Max)
            ) {
                rowTiles.forEach { tile ->
                    MetricTile(
                        icon = tile.icon(),
                        label = stringResource(tile.label),
                        value = tile.value,
                        meaning = stringResource(tile.meaning),
                        scale = tile.scale,
                        detail = tile.detail,
                        note = tile.note,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
                if (rowTiles.size < perRow) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/** The UV index's practical top: 11 is "extreme" and the WHO's scale is open-ended
 * above it, so a rarer 12 or 13 fills the track and the meaning line does the talking. */
private const val UvScaleTop = 11f

/** The US AQI's "hazardous" threshold: above 300 the track is full and the meaning line
 * says to stay indoors, which is all a reader needs from a number past that. */
private const val AqiScaleTop = 300f

/** Where the wind comes from, in words, and where it goes, as an arrow (DESIGN §8.6). The
 * eighth of the compass is `SkyText`'s vocabulary — the same "north-east" the Sky screen
 * says for a rainbow — because a general audience reads "from the north-east" and
 * decodes "NNE"; the 16-point label the model carries stays in the data. */
@Composable
private fun WindDirection(fromDegrees: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WindArrow(fromDegrees = fromDegrees)
        Text(
            text = stringResource(
                R.string.wind_from,
                stringResource(SkyText.bearingRes(fromDegrees.toDouble()))
            ),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private data class Tile(
    val icon: @Composable () -> androidx.compose.ui.graphics.vector.ImageVector,
    val label: Int,
    val value: String,
    val meaning: Int,
    /** 0..1 on a world-anchored scale, when the metric has one. */
    val scale: Float? = null,
    /** A composed fact about the value (the wind's arrow and source). */
    val detail: (@Composable () -> Unit)? = null,
    /** A printed fact about the value (gusts, dew point, which pollen). */
    val note: String? = null
)
