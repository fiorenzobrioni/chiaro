package com.callbackdev.chiaro.ui.today

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.onSizeChanged
import com.callbackdev.chiaro.ui.shell.LocalRailInset
import com.callbackdev.chiaro.ui.shell.pageColumns
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.domain.settings.UnitSettings
import com.callbackdev.chiaro.domain.sky.SkySights
import com.callbackdev.chiaro.ui.components.FreshnessChip
import com.callbackdev.chiaro.ui.firstrun.gpsErrorText
import com.callbackdev.chiaro.ui.format.Formats
import com.callbackdev.chiaro.ui.format.currentLocale
import com.callbackdev.chiaro.ui.icons.ConditionGlyph
import com.callbackdev.chiaro.ui.icons.LocalMotionPaused
import com.callbackdev.chiaro.ui.icons.LocalPageScrolls
import com.callbackdev.chiaro.ui.icons.PageScrolls
import com.callbackdev.chiaro.ui.places.PlacesSheet
import com.callbackdev.chiaro.ui.places.PlacesViewModel
import com.callbackdev.chiaro.ui.sky.AgendaPageSheet
import com.callbackdev.chiaro.ui.sky.LookTarget
import com.callbackdev.chiaro.ui.theme.ChiaroTheme
import com.callbackdev.chiaro.ui.theme.SkyPalette
import com.callbackdev.chiaro.ui.theme.reducedMotion
import com.callbackdev.chiaro.ui.warnings.WarningBanner
import com.callbackdev.chiaro.ui.warnings.WarningSheet
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
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
internal fun PagedToday(
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

    // The status bar icons follow what is under them, which since 18 set 2026 is the
    // pinned place row: white over a page with a report — the row stands on the canvas'
    // top scrim at rest, and since 23 set 2026 it keeps that same scrimmed sky as its
    // ground once the page scrolls (DESIGN §8.1b) — and theme ink over the plain states.
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
internal fun pageTitle(page: PlacePage): String = when (page) {
    is PlacePage.Gps -> page.lastFix?.name ?: stringResource(R.string.places_gps_title)
    is PlacePage.Saved -> page.city.name
}

/** White over the canvas needs dark icons off; the plain states follow the theme —
 * the APPLIED theme, read off the surface itself, because since Fase 4 the reader can
 * force light or dark against the system and the icons must follow the choice. */
@Composable
internal fun StatusBarIcons(overCanvas: Boolean) {
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

/**
 * One place's page: what the pager draws for each place, and, being a function of its
 * state alone, what the README's screenshots draw from a recorded forecast (25 set 2026).
 */
@Composable
internal fun TodayPage(
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
    onDismissGuideCard: () -> Unit,
    isCurrent: Boolean,
    onCanvasBehindBar: (Boolean) -> Unit
) {
    when (state) {
        is TodayUiState.Content ->
            ContentState(
                state, title, isGps, dots, units, locating, guideCardVisible,
                onRefresh, onOpenPlaces, onOpenSettings, onOpenGuide,
                onDismissGuideCard, isCurrent, onCanvasBehindBar
            )
        else -> Column(modifier = Modifier.fillMaxSize().padding(start = LocalRailInset.current)) {
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
                    .padding(HeaderPadding)
            )
            when (state) {
                TodayUiState.Starting -> TodaySkeleton()
                TodayUiState.NoPlace -> NoPlaceState(onOpenPlaces)
                is TodayUiState.Empty -> EmptyPage(state, locating, onRefresh)
                is TodayUiState.Content -> Unit // handled above
            }
        }
    }
}

/**
 * A page with no report yet takes the pull like a page with one (17 ott 2026). It had
 * none: the gesture that refreshes every other page did nothing here, and here is
 * exactly where a reader whose first fetch failed, or is still waiting on a service
 * slow to answer, tries it. The column scrolls so that the drag reaches the box.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmptyPage(state: TodayUiState.Empty, locating: Boolean, onRefresh: () -> Unit) {
    PullToRefreshBox(
        isRefreshing = state.userRefreshing || locating,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            EmptyState(state, onRefresh)
        }
    }
}

// ---------------------------------------------------------------------------------
// Content
// ---------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ContentState(
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
    onDismissGuideCard: () -> Unit,
    isCurrent: Boolean,
    onCanvasBehindBar: (Boolean) -> Unit
) {
    val locale = currentLocale()
    val is24h = android.text.format.DateFormat.is24HourFormat(LocalContext.current)
    val timeFmt = remember(locale, is24h) { Formats.timeFormatter(is24h, locale) }

    // The place row is PINNED over the list since 18 set 2026 (device request): the
    // city these numbers belong to must not scroll away, which is the rule the other
    // three tabs already kept by drawing their header above their list. Here it is an
    // overlay rather than a row above the list, because the canvas still owns the top
    // edge of the screen (§3.6) and the row still stands on the sky when the page
    // is at rest.
    //
    // At the very top the bar is transparent over the canvas' top scrim band, which is
    // exactly where §3.6 measures its white. From the first scrolled pixel it carries a
    // ground of its own, and that ground follows the scroll (committente, 15 ott 2026): the
    // scrimmed sky at first, fading into the page's `surface` as the page climbs, and the
    // paper exactly when the page's edge reaches the bar, which is then the header Sky and
    // Alerts draw. The ink follows the ground ([placeBarColors]), so no point of the scroll
    // sets the name on a ground it does not read on. When the hero has scrolled away the bar
    // also carries the temperature and the sky's icon: the reader who is down in the details
    // still has the one number the page is about.
    val listState = rememberLazyListState()
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val density = LocalDensity.current

    // The canvas keeps the row's seat empty: a spacer as tall as the bar really is,
    // measured rather than quoted because the row grows with the type scale, with the
    // pager dots and with the place's own hour. [PlainHeaderHeight] is only the floor it
    // stands on before the first layout.
    val reduced = reducedMotion()
    var headerHeight by remember(statusTop) { mutableStateOf(statusTop + PlainHeaderHeight) }
    val skyGradient = ChiaroTheme.sky.gradient(
        sunAltitudeDeg = content.sky.sunAltitudeDeg,
        cloudPct = content.sky.cloudPct,
        precipPct = content.sky.precipPct,
        moonIllumination = content.sky.moonIllumination,
        moonAltitudeDeg = content.sky.moonAltitudeDeg
    )
    // The bar at rest stands on the sky; from the first scrolled pixel it is the page's own,
    // as on Sky and Alerts (committente, 15 ott 2026). The change is a crossfade of
    // [BarCrossfadeMillis], ground, ink and dots together. Tried and dropped the same day: a
    // ground that followed the scroll from the sky to the paper, which on the device read as
    // the bar darkening with a jolt under the finger and then lightening again.
    val atTop by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
        }
    }
    val crossfade = barCrossfade<Color>(reduced)
    // The transparent end is the same color at zero alpha, not `Color.Transparent`:
    // colors interpolate perceptually here, and fading in from a transparent BLACK would
    // draw a veil of the wrong hue on the way.
    val paper = MaterialTheme.colorScheme.surface
    val headerGround by animateColorAsState(
        targetValue = if (atTop) paper.copy(alpha = 0f) else paper,
        animationSpec = crossfade,
        label = "place row ground"
    )
    val headerInk by animateColorAsState(
        targetValue = if (atTop) SkyPalette.Ink else MaterialTheme.colorScheme.onSurface,
        animationSpec = crossfade,
        label = "place row ink"
    )
    val headerDotInk by animateColorAsState(
        targetValue = if (atTop) SkyPalette.InkQuiet else MaterialTheme.colorScheme.outlineVariant,
        animationSpec = crossfade,
        label = "place row dots"
    )
    // The canvas' halo under the row's text while it stands on the sky (DESIGN §3.6, 19 ott
    // 2026), fading with the ink on the same crossfade so it never pops on or off.
    val headerHalo by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (atTop) SkyPalette.InkHaloAlpha else 0f,
        animationSpec = barCrossfade<Float>(reduced),
        label = "place row halo"
    )
    // The status bar shows what is behind it, and what is behind it is this bar: the sky at
    // rest, white icons; the page once scrolled, the theme's.
    LaunchedEffect(isCurrent, atTop) { if (isCurrent) onCanvasBehindBar(atTop) }
    // The hero is gone once its number's middle has gone under the bar (DESIGN §8.1e, 18 ott
    // 2026): the canvas measures where the number rests, and the rest is arithmetic on the
    // scroll. The bar's own icon and number then come in on a timed transition, the same with
    // or without reduced motion, so the bar holds the number or does not and never a half of
    // it. Until the figure is measured, the canvas' bottom climbing to within [HeroReach] of
    // the bar stands in for it.
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
    var pageWidth by remember { mutableStateOf(0) }

    // Which day of the week is open, hoisted out of the week because the week is no
    // longer one composable: each row is its own lazy item (8 set 2026), so the seven
    // moving icons arrive one row at a time instead of in the frame the section enters.
    // Reset when the week itself changes, as before.
    var expandedDay by remember(content.week.firstOrNull()?.forecast?.date) {
        mutableStateOf<java.time.LocalDate?>(null)
    }

    // The warning's own sheet, hoisted here for the same reason the week's expansion is:
    // the banner is a lazy item and its state must outlive being scrolled off.
    var warningSheetOpen by rememberSaveable(content.warnings?.bulletinId) {
        mutableStateOf(false)
    }
    content.warnings?.let { warning ->
        if (warningSheetOpen) {
            WarningSheet(
                warnings = warning,
                today = content.now.toLocalDate(),
                timeFmt = timeFmt,
                onDismiss = { warningSheetOpen = false }
            )
        }
    }

    // The Sky's page for a moment of the day (review of Today, 15 ott 2026), hoisted here
    // like the warning's sheet: the row that opened it is a lazy item.
    var agendaPage by rememberSaveable { mutableStateOf<String?>(null) }
    var agendaBearing by rememberSaveable { mutableStateOf<Double?>(null) }
    var agendaWhen by rememberSaveable { mutableStateOf("") }
    agendaPage?.let { id ->
        AgendaPageSheet(
            jobId = id,
            onOpenRelated = {
                agendaPage = it
                agendaBearing = null
            },
            onDismiss = { agendaPage = null },
            look = agendaBearing?.let { LookTarget(it, agendaWhen) },
            coordinates = content.report.location.coordinates
        )
    }

    // The next two hours' page (17 ott 2026), hoisted like the others: its card is a lazy item.
    var nextRainOpen by rememberSaveable { mutableStateOf(false) }
    val nowcast = content.nowcast
    // Closed for good, not hidden: a page left "open" under a dry window would come back by
    // itself the next time it rains.
    LaunchedEffect(nowcast == null) { if (nowcast == null) nextRainOpen = false }
    if (nextRainOpen && nowcast != null) {
        NextRainSheet(
            reading = nowcast,
            zone = content.zone,
            timeFmt = timeFmt,
            locale = locale,
            currentCode = content.report.current.condition.wmoCode,
            onDismiss = { nextRainOpen = false }
        )
    }

    // The rainbow's page (15 ott 2026), hoisted like the agenda's: its row is a lazy item.
    var rainbowItem by remember { mutableStateOf<TimelineItem?>(null) }
    rainbowItem?.let { item ->
        RainbowSheet(
            item = item,
            now = content.now,
            zone = content.zone,
            coordinates = content.report.location.coordinates,
            timeFmt = timeFmt,
            onDismiss = { rainbowItem = null }
        )
    }

    // "What changed", written out here because a `LazyListScope` cannot call a
    // composable and these sentences need the string table. A revision whose fields
    // this screen has no words for (a condition code, say) produces NO line: on device
    // it printed as "Wednesday 9's forecast changed:" with nothing after the colon,
    // which is §1.1's dash in a card by another route — and it spent one of the three
    // lines the section is allowed.
    val changed = content.whatChanged.mapNotNull { shift ->
        val details = WhatChangedText.shiftDetails(
            shift.shifts, units, locale
        )
        if (details.isBlank()) null
        else ChangedLine(
            headline = WhatChangedText.shiftHeadline(shift, locale),
            details = details,
            // The day's sky as the week draws it now: what the revision arrived at.
            glyph = content.week.firstOrNull { it.forecast.date == shift.date }
                ?.let { ConditionGlyph(it.forecast.condition.wmoCode, night = false) }
        )
    }

    // Both halves are the reader's own gesture and nothing else (Fase 3b): the pull
    // indicator means "doing what you just asked", so an automatic fetch and an
    // automatic re-fix leave it alone — VISION §5.2, refresh silent.
    // While the page moves the weather holds still (DESIGN §7.1, 9 set 2026): the
    // moving icons hide behind their still drawings for as long as the list is in
    // motion, so the RenderThread spends its frames on the scroll and not on fourteen
    // vector loops. The pager's swipe says the same thing one level up.
    // So does any strip on the page that is being dragged sideways: the week's open day, the
    // next hours (committente, 15 ott 2026). They report through [LocalPageScrolls].
    val scrolls = remember { PageScrolls() }
    val pageMoving = LocalMotionPaused.current || listState.isScrollInProgress || scrolls.any
    CompositionLocalProvider(LocalMotionPaused provides pageMoving, LocalPageScrolls provides scrolls) {
    PullToRefreshBox(
        isRefreshing = content.userRefreshing || locating,
        onRefresh = onRefresh
    ) {
        // The page's rhythm is the items' own since 15 ott 2026: a section is a title on the
        // paper and its content on a container under it (DESIGN §8.0), and a group's segments
        // stand 2dp apart where the list's even 12dp gap would have pulled the week's seven
        // segments apart into seven cards. So the list spaces nothing, and every item says
        // what air it needs: [ItemGap] over a notice, [SectionTitle]'s 24 over a section.
        // A wide page stands in two panes under the sky (DESIGN §8.12c, 15 ott 2026): the
        // hours and the rest of the day, which are about today, on the left under the hero;
        // the week, what changed and the details, which are about the days and the air, on
        // the right. The sky stays one, across the whole page.
        val railInset = LocalRailInset.current
        // The page's width once the list has measured it; the window's before, which is the
        // same width (the page is the window's, rail included) and keeps the first frame
        // from laying out a phone's column on a tablet.
        val windowWidth = LocalWindowInfo.current.containerSize.width
        val columns = with(density) {
            pageColumns((if (pageWidth > 0) pageWidth else windowWidth).toDp(), railInset)
        }
        val paper = Modifier.padding(start = columns.start, end = columns.end)
        // The first title on the paper (review of 12 ott 2026): the sheet's own 24dp lip
        // is already above it, and a section's whole 24 more made a band of empty paper
        // between the sky and the hours — the hours VISION §4.4 wants above the fold,
        // pushed down by nothing. With a chip, a banner or the guide card in between, it
        // keeps a section's air to stand apart from them.
        val opensThePaper = content.error == null && !content.isStale &&
            content.warnings == null && guideCardVisible != true
        val firstTitleTop = if (opensThePaper) ItemGap else SectionGap
        val restOfDay: @Composable () -> Unit = {
            RestOfDay(content, timeFmt, units, onOpenRainbow = { rainbowItem = it }) { item, job ->
                agendaPage = job.id
                // Where to look, for the rows that have a direction: the same rule
                // and the same instant the Sky's row asks (SkySights.bearing).
                agendaBearing = SkySights.bearing(
                    job,
                    item.at.atZone(content.zone).toInstant(),
                    content.report.location.coordinates
                )
                agendaWhen = listOfNotNull(item.at, item.until)
                    .joinToString(" – ") { it.format(timeFmt) }
            }
        }
        // The week says once that its days open (second review, 15 ott 2026), where
        // seven chevrons said it seven times.
        val weekTitle: @Composable (Dp) -> Unit = { top ->
            SectionTitle(
                stringResource(R.string.section_week),
                top = top,
                hint = stringResource(R.string.week_open_hint)
            )
        }
        val onToggleDay: (java.time.LocalDate) -> Unit = { date ->
            expandedDay = if (expandedDay == date) null else date
        }
        // The next two hours (17 ott 2026), first on the paper when something falls in them:
        // the most urgent thing the page knows, and the only section about minutes.
        val nextRainTitle: @Composable () -> Unit = {
            nowcast?.let {
                SectionTitle(
                    stringResource(nextRainTitleRes(it)),
                    top = firstTitleTop,
                    hint = stringResource(R.string.nowcast_hint)
                )
            }
        }
        val nextRainCard: @Composable () -> Unit = {
            nowcast?.let {
                NextRainCard(
                    reading = it,
                    zone = content.zone,
                    timeFmt = timeFmt,
                    locale = locale,
                    // Once per fetch: the window sliding by a quarter is not new rain.
                    animationKey = content.lastSync,
                    onOpen = { nextRainOpen = true }
                )
            }
        }
        val hoursTitleTop = if (nowcast != null) SectionGap else firstTitleTop
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().onSizeChanged { pageWidth = it.width },
            contentPadding = WindowInsets.navigationBars.asPaddingValues()
        ) {
            item {
                CanvasHeader(
                    content, skyGradient, units, timeFmt, locale, headerHeight, figure,
                    columns = columns,
                    railInset = railInset
                )
            }

            if (content.error != null) {
                item { Box(paper.padding(top = ItemGap)) { ErrorBanner(content.error, onRefresh) } }
            }
            if (content.isStale) {
                item {
                    Box(paper) {
                        FreshnessChip(
                            age = freshnessAge(content.lastSync),
                            onRetry = onRefresh,
                            modifier = Modifier.padding(start = 16.dp, top = ItemGap, end = 16.dp)
                        )
                    }
                }
            }
            // The official warning (Fase 11, DESIGN §8.13): after the chip, which
            // qualifies the hero, and before the guide card, because it opens the
            // content rather than annotating the sky. Never drawn for a green day —
            // `warnings` is null there — and never drawn as an error.
            content.warnings?.let { warning ->
                item {
                    Box(paper) {
                        WarningBanner(
                            warnings = warning,
                            // The PLACE's day, like every other date on this screen: the
                            // bulletin's own days are the issuer's, and for an Italian
                            // place the two are the same one.
                            today = content.now.toLocalDate(),
                            timeFmt = timeFmt,
                            onOpen = { warningSheetOpen = true },
                            modifier = Modifier.padding(start = 16.dp, top = ItemGap, end = 16.dp)
                        )
                    }
                }
            }
            // The one-time pointer to the guide (VISION 5.7): true shows it, false
            // and null both draw nothing - a card that flashed while the store was
            // still answering would be shown to everyone and dismissed by nobody.
            if (guideCardVisible == true) {
                item {
                    Box(paper.padding(top = ItemGap)) {
                        GuideCard(onOpen = onOpenGuide, onDismiss = onDismissGuideCard)
                    }
                }
            }

            if (columns.twoPanes) {
                // The panes are one item: two columns of a lazy list cannot scroll as one
                // page, and a wide page holds both of them in a screen or two anyway.
                item {
                    Row(paper) {
                        Column(Modifier.weight(1f)) {
                            nextRainTitle()
                            nextRainCard()
                            SectionTitle(stringResource(R.string.section_next_hours), top = hoursTitleTop)
                            NextHours(content, units, is24h, timeFmt, locale)
                            if (content.timeline.isNotEmpty()) {
                                SectionTitle(stringResource(R.string.section_rest_of_day))
                                restOfDay()
                            }
                        }
                        Column(Modifier.weight(1f)) {
                            // The second pane's first title on the first one's line.
                            var top = firstTitleTop
                            if (content.week.isNotEmpty()) {
                                weekTitle(top)
                                WeekColumn(content, units, is24h, locale, expandedDay, onToggleDay)
                                top = SectionGap
                            }
                            if (changed.isNotEmpty()) {
                                SectionTitle(stringResource(R.string.today_changed_title), top = top)
                                WhatChanged(lines = changed)
                            }
                        }
                    }
                }
                // The details under both panes, across the page (§8.12c): in one pane they
                // were the tallest thing on it and left the other a screen of empty paper;
                // across, the grid takes three or four tiles a row at a phone's tile width,
                // and the two panes above come out about as tall as each other.
                item { Box(paper) { SectionTitle(stringResource(R.string.section_details)) } }
                item { Box(paper) { Details(content, units, locale) } }
                item { Box(paper) { DataFooter(content, timeFmt) } }
                return@LazyColumn
            }

            if (nowcast != null) {
                item { Box(paper) { nextRainTitle() } }
                item { Box(paper) { nextRainCard() } }
            }
            item {
                Box(paper) { SectionTitle(stringResource(R.string.section_next_hours), top = hoursTitleTop) }
            }
            item { Box(paper) { NextHours(content, units, is24h, timeFmt, locale) } }

            if (content.timeline.isNotEmpty()) {
                item { Box(paper) { SectionTitle(stringResource(R.string.section_rest_of_day)) } }
                item { Box(paper) { restOfDay() } }
            }

            if (content.week.isNotEmpty()) {
                item { Box(paper) { weekTitle(SectionGap) } }
                weekRows(
                    content = content,
                    units = units,
                    is24h = is24h,
                    locale = locale,
                    expandedDay = expandedDay,
                    onToggle = onToggleDay,
                    modifier = paper
                )
            }

            // VISION §5.2.6 — when something did change: two or three sentences.
            // AFTER the week
            // (asked on device, 6 set, and it is the right order): every one of these
            // sentences is about a day further out — "Wednesday's forecast changed" —
            // so it used to name days the reader had not been shown yet, and it split
            // the two sections about today from the one about the days ahead.
            if (changed.isNotEmpty()) {
                item { Box(paper) { SectionTitle(stringResource(R.string.today_changed_title)) } }
                item { Box(paper) { WhatChanged(lines = changed) } }
            }

            item { Box(paper) { SectionTitle(stringResource(R.string.section_details)) } }
            item { Box(paper) { Details(content, units, locale) } }
            item { Box(paper) { DataFooter(content, timeFmt) } }
        }

        // After the list so it stands over it, inside the pull box so the refresh
        // indicator still comes down in front of it.
        PlaceHeader(
            title = title,
            isGps = isGps,
            localNow = if (isGps) {
                null
            } else {
                val day = content.now.toLocalDate()
                val time = content.now.format(timeFmt)
                listOf(
                    Formats.dayLong(day, locale),
                    Formats.dayMedium(day, locale),
                    Formats.dayLabel(day, locale)
                ).map { "$it · $time" }
            },
            dots = dots,
            onOpenPlaces = onOpenPlaces,
            onOpenSettings = onOpenSettings,
            contentColor = headerInk,
            dotInactive = headerDotInk,
            shadow = if (headerHalo > 0.01f) com.callbackdev.chiaro.ui.theme.skyInkShadow(headerHalo) else null,
            compactVisible = heroGone,
            compact = {
                // Whole degrees, like every other temperature below the hero: the bar is a
                // reminder of the number, and the tenths would cost the place's name its room.
                CompactNow(
                    temperature = Formats.temperature(content.report.current.tempC, units.temperature, locale),
                    glyph = ConditionGlyph(content.report.current.condition.wmoCode, content.night),
                    color = headerInk
                )
            },
            modifier = Modifier
                .align(Alignment.TopCenter)
                // First in the chain: what the canvas must leave empty is the whole bar,
                // status-bar inset and padding included.
                .onSizeChanged { size ->
                    headerHeight = with(density) { size.height.toDp() }
                }
                .background(headerGround)
                .statusBarsPadding()
                // Beside the rail, whose band of status bar is this bar's ground (§8.12c).
                .padding(start = LocalRailInset.current)
                .padding(HeaderPadding)
        )
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
internal fun DataFooter(content: TodayUiState.Content, timeFmt: DateTimeFormatter) {
    Text(
        text = stringResource(
            R.string.today_updated_footer,
            LocalDateTime.ofInstant(content.lastSync, content.zone).format(timeFmt)
        ),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        // On the page's margin, like the Sky's footnote (review of Today, 15 ott 2026): a
        // centred line was the one thing on the paper not standing on the 16dp column.
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp)
    )
}

/**
 * A section's title on the paper (DESIGN §6, §8.0): [SectionGap] over it, the 24 every
 * screen keeps between one section and the next, and 12 under it, down to its container.
 */
@Composable
internal fun SectionTitle(text: String, top: Dp = SectionGap, hint: String? = null) {
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = top, end = 16.dp, bottom = ItemGap)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f)
        )
        hint?.let {
            // A note on the section, not a label of it: quiet, on the title's baseline side.
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.End
            )
        }
    }
}

/** Between one section and the next. */
internal val SectionGap = 24.dp

/** Over a notice (the chip, a banner, the guide card) and under a section's title. */
internal val ItemGap = 12.dp
