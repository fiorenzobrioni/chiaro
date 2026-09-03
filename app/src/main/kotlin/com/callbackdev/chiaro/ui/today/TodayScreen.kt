package com.callbackdev.chiaro.ui.today

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.core.view.WindowCompat
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import com.callbackdev.chiaro.ui.components.RainSparkline
import com.callbackdev.chiaro.ui.components.SkyCanvas
import com.callbackdev.chiaro.ui.format.Formats
import com.callbackdev.chiaro.ui.icons.ChiaroIcons
import com.callbackdev.chiaro.ui.places.PlacesSheet
import com.callbackdev.chiaro.ui.places.PlacesViewModel
import com.callbackdev.chiaro.ui.theme.SkyPalette
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
    val guideCard by todayViewModel.guideCardVisible.collectAsStateWithLifecycle()
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
                guideCardVisible = guideCard,
                stateFor = todayViewModel::stateFor,
                onRefresh = todayViewModel::refresh,
                onSettled = todayViewModel::setActive,
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
    guideCardVisible: Boolean?,
    stateFor: (PlacePage) -> StateFlow<TodayUiState>,
    onRefresh: (PlacePage) -> Unit,
    onSettled: (PlacePage) -> Unit,
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
    LaunchedEffect(model.activeIndex, pages.size) {
        val target = model.activeIndex
        if (target in pages.indices && target != pagerState.currentPage &&
            !pagerState.isScrollInProgress
        ) {
            pagerState.animateScrollToPage(target)
        }
    }
    // The pager settled → that is the selection now.
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { settled ->
            pages.getOrNull(settled)?.let(onSettled)
        }
    }

    // The status bar icons follow what is under them: white over the canvas' top
    // scrim, theme ink over the plain states.
    val currentPage = pages.getOrNull(pagerState.currentPage.coerceIn(0, pages.lastIndex))
    val currentState = currentPage?.let { stateFor(it).collectAsStateWithLifecycle().value }
    StatusBarIcons(overCanvas = currentState is TodayUiState.Content)

    Surface(modifier = Modifier.fillMaxSize()) {
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
                guideCardVisible = guideCardVisible,
                onRefresh = { onRefresh(page) },
                onOpenPlaces = onOpenPlaces,
                onOpenSettings = onOpenSettings,
                onOpenGuide = onOpenGuide,
                onOpenJournal = onOpenJournal,
                onDismissGuideCard = onDismissGuideCard
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
        activity?.window?.let { window ->
            WindowCompat.getInsetsController(window, window.decorView)
                .isAppearanceLightStatusBars = !overCanvas && !darkTheme
        }
        onDispose { }
    }
}

@Composable
private fun TodayPage(
    state: TodayUiState,
    title: String,
    isGps: Boolean,
    dots: Pair<Int, Int>?,
    units: UnitSettings,
    guideCardVisible: Boolean?,
    onRefresh: () -> Unit,
    onOpenPlaces: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenGuide: () -> Unit,
    onOpenJournal: () -> Unit,
    onDismissGuideCard: () -> Unit
) {
    when (state) {
        is TodayUiState.Content ->
            ContentState(
                state, title, isGps, dots, units, guideCardVisible,
                onRefresh, onOpenPlaces, onOpenSettings, onOpenGuide, onOpenJournal,
                onDismissGuideCard
            )
        else -> Column(modifier = Modifier.fillMaxSize()) {
            PlaceHeader(
                title = title,
                isGps = isGps,
                dots = dots,
                onOpenPlaces = onOpenPlaces,
                onOpenSettings = onOpenSettings,
                contentColor = MaterialTheme.colorScheme.onSurface,
                dotInactive = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
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
                dots = null,
                onOpenPlaces = onOpenPlaces,
                onOpenSettings = onOpenSettings,
                contentColor = MaterialTheme.colorScheme.onSurface,
                dotInactive = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
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
 */
@Composable
private fun PlaceHeader(
    title: String,
    isGps: Boolean,
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

/** Blocks of `surfaceContainerHigh` in the shape of the real layout: unmistakably not
 * data (§1.1 — no placeholder may render as a value, so none of these carries text). */
@Composable
private fun TodaySkeleton() {
    val block = MaterialTheme.colorScheme.surfaceContainerHigh
    Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .background(block, MaterialTheme.shapes.extraLarge)
        )
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(5) {
                Box(
                    modifier = Modifier
                        .width(56.dp)
                        .height(96.dp)
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
    guideCardVisible: Boolean?,
    onRefresh: () -> Unit,
    onOpenPlaces: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenGuide: () -> Unit,
    onOpenJournal: () -> Unit,
    onDismissGuideCard: () -> Unit
) {
    val locale = Locale.getDefault()
    val is24h = android.text.format.DateFormat.is24HourFormat(LocalContext.current)
    val timeFmt = remember(locale, is24h) { Formats.timeFormatter(is24h, locale) }

    PullToRefreshBox(isRefreshing = content.refreshing, onRefresh = onRefresh) {
        LazyColumn(
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

            // VISION §5.2.5 — when something did change: two or three sentences,
            // tapping opens the Journal where the whole story lives.
            if (content.whatChanged.isNotEmpty()) {
                item { SectionTitle(stringResource(R.string.today_changed_title)) }
                item {
                    WhatChanged(
                        shifts = content.whatChanged,
                        units = units,
                        locale = locale,
                        onOpenJournal = onOpenJournal
                    )
                }
            }

            if (content.week.isNotEmpty()) {
                item { SectionTitle(stringResource(R.string.section_week)) }
                item { Week(content, units, is24h, locale, timeFmt) }
            }

            item { SectionTitle(stringResource(R.string.section_details)) }
            item { Details(content.report, units, locale) }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(start = 16.dp, top = 12.dp)
    )
}

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
    SkyCanvas(
        gradient = SkyPalette.gradient(
            sunAltitudeDeg = sky.sunAltitudeDeg,
            cloudPct = sky.cloudPct,
            precipPct = sky.precipPct,
            moonIllumination = sky.moonIllumination,
            moonAltitudeDeg = sky.moonAltitudeDeg
        ),
        height = 280.dp + statusTop
    ) {
        PlaceHeader(
            title = title,
            isGps = isGps,
            dots = dots,
            onOpenPlaces = onOpenPlaces,
            onOpenSettings = onOpenSettings,
            contentColor = Color.White,
            dotInactive = Color.White.copy(alpha = 0.4f),
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
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
 * ([JournalText]), the whole block one door to it. */
@Composable
private fun WhatChanged(
    shifts: List<com.callbackdev.chiaro.ui.journal.JournalEntry.ForecastShift>,
    units: UnitSettings,
    locale: Locale,
    onOpenJournal: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenJournal)
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        shifts.forEach { shift ->
            Text(
                text = com.callbackdev.chiaro.ui.journal.JournalText.shiftHeadline(shift, locale) +
                    ": " +
                    com.callbackdev.chiaro.ui.journal.JournalText.shiftDetails(
                        shift.shifts, units, locale
                    ),
                style = MaterialTheme.typography.bodyMedium
            )
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
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HourStrip(hours = content.strip.map { it.toCell(units, is24h, locale) })
        val percentages = content.strip.map { it.hour.precipChancePct }
        val peak = content.strip.maxByOrNull { it.hour.precipChancePct }
        RainSparkline(
            percentages = percentages,
            description = if (peak != null && peak.hour.precipChancePct > 0) {
                stringResource(
                    R.string.sparkline_peak_desc,
                    peak.hour.precipChancePct,
                    peak.hour.time.format(timeFmt)
                )
            } else {
                stringResource(R.string.sparkline_dry_desc)
            }
        )
    }
}

@Composable
private fun StripHour.toCell(units: UnitSettings, is24h: Boolean, locale: Locale): HourCell {
    val hourLabel = Formats.hourLabel(hour.time, is24h, locale)
    val temp = Formats.temperature(hour.tempC, units.temperature, locale)
    val word = stringResource(WeatherText.condition(hour.condition.wmoCode))
    return HourCell(
        hourLabel = hourLabel,
        icon = ChiaroIcons.condition(hour.condition.wmoCode, night),
        temperature = temp,
        rainPct = hour.precipChancePct,
        description = stringResource(
            R.string.hour_cell_desc, hourLabel, word, temp, hour.precipChancePct
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
    TimelineKind.RAIN_START -> stringResource(R.string.tl_rain_start, item.pct ?: 0)
    TimelineKind.RAIN_STOP -> stringResource(R.string.tl_rain_stop)
}

@Composable
private fun Week(
    content: TodayUiState.Content,
    units: UnitSettings,
    is24h: Boolean,
    locale: Locale,
    timeFmt: DateTimeFormatter
) {
    val scaleLow = content.week.minOf { it.forecast.lowC }
    val scaleHigh = content.week.maxOf { it.forecast.highC }
    var expanded by remember(content.week.firstOrNull()?.forecast?.date) {
        mutableStateOf<java.time.LocalDate?>(null)
    }
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        content.week.forEach { day ->
            val f = day.forecast
            val today = f.date == content.now.toLocalDate()
            val label = if (today) stringResource(R.string.week_today) else Formats.dayLabel(f.date, locale)
            val low = Formats.temperature(f.lowC, units.temperature, locale)
            val high = Formats.temperature(f.highC, units.temperature, locale)
            DayRow(
                dayLabel = label,
                icon = ChiaroIcons.condition(f.condition.wmoCode, night = false),
                rainPct = f.precipPct,
                lowC = f.lowC,
                highC = f.highC,
                lowLabel = low,
                highLabel = high,
                scaleLowC = scaleLow,
                scaleHighC = scaleHigh,
                phases = day.phases,
                description = stringResource(
                    R.string.week_day_desc,
                    label,
                    stringResource(WeatherText.condition(f.condition.wmoCode)),
                    low, high, f.precipPct
                ),
                onClick = if (day.hours.isNotEmpty()) {
                    { expanded = if (expanded == f.date) null else f.date }
                } else {
                    null
                }
            )
            AnimatedVisibility(visible = expanded == f.date) {
                HourStrip(
                    hours = day.hours.map { it.toCell(units, is24h, locale) },
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

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
                    meaning = WeatherText.uvMeaning(today.uvIndexMax)
                )
            )
        }
        add(
            Tile(
                icon = { ChiaroIcons.wind },
                label = R.string.metric_wind,
                value = "${Formats.wind(current.wind.speedKph, units.windSpeed, locale)} " +
                    current.wind.directionCompass,
                meaning = WeatherText.windMeaning(current.wind.speedKph)
            )
        )
        add(
            Tile(
                icon = { ChiaroIcons.humidity },
                label = R.string.metric_humidity,
                value = "${current.humidityPct}%",
                meaning = WeatherText.humidityMeaning(current.humidityPct)
            )
        )
        add(
            Tile(
                icon = { ChiaroIcons.dewPoint },
                label = R.string.metric_dew,
                value = Formats.temperature(current.dewPointC, units.temperature, locale),
                meaning = WeatherText.dewPointMeaning(current.dewPointC)
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
        add(
            Tile(
                icon = { ChiaroIcons.visibility },
                label = R.string.metric_visibility,
                value = Formats.kilometers(current.visibilityKm, locale),
                meaning = WeatherText.visibilityMeaning(current.visibilityKm)
            )
        )
        // §1.1: data the provider does not have for here is not drawn — no dashes.
        report.airQuality?.let { air ->
            add(
                Tile(
                    icon = { ChiaroIcons.airQuality },
                    label = R.string.metric_air,
                    value = "${air.aqiIndex} AQI",
                    meaning = WeatherText.aqiMeaning(air.aqiIndex)
                )
            )
        }
        report.pollen?.let { pollen ->
            val worst = listOf(pollen.grass, pollen.tree, pollen.weed).maxBy { it.ordinal }
            add(
                Tile(
                    icon = { ChiaroIcons.pollen },
                    label = R.string.metric_pollen,
                    value = stringResource(WeatherText.pollenLevel(worst)),
                    meaning = WeatherText.pollenMeaning(worst)
                )
            )
        }
    }
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        tiles.chunked(2).forEach { rowTiles ->
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
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
                if (rowTiles.size == 1) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

private data class Tile(
    val icon: @Composable () -> androidx.compose.ui.graphics.vector.ImageVector,
    val label: Int,
    val value: String,
    val meaning: Int
)
