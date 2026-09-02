package com.callbackdev.chiaro.ui.today

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

/**
 * Today (VISION §5.2): the canvas, the sentence, the hours, the day, the week, the
 * details — one vertical scroll, cached content first, and nothing on it the data did
 * not say.
 */
@Composable
fun TodayRoute(
    todayViewModel: TodayViewModel = viewModel(factory = TodayViewModel.Factory),
    placesViewModel: PlacesViewModel = viewModel(factory = PlacesViewModel.Factory)
) {
    val state by todayViewModel.state.collectAsStateWithLifecycle()
    var placesOpen by remember { mutableStateOf(false) }

    TodayScreen(
        state = state,
        onRefresh = todayViewModel::refresh,
        onOpenPlaces = { placesOpen = true }
    )
    if (placesOpen) {
        PlacesSheet(viewModel = placesViewModel, onDismiss = { placesOpen = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    state: TodayUiState,
    onRefresh: () -> Unit,
    onOpenPlaces: () -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            val place = when (state) {
                is TodayUiState.Content -> state.city.name
                is TodayUiState.Empty -> state.city.name
                else -> stringResource(R.string.app_name)
            }
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable(onClick = onOpenPlaces)
                            .semantics {
                                contentDescription = place
                            }
                    ) {
                        Text(place, style = MaterialTheme.typography.titleMedium)
                        Icon(
                            imageVector = Icons.Outlined.KeyboardArrowDown,
                            contentDescription = stringResource(R.string.place_switcher_action)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (state) {
                TodayUiState.Starting -> TodaySkeleton()
                TodayUiState.NoPlace -> NoPlaceState(onOpenPlaces)
                is TodayUiState.Empty -> EmptyState(state, onRefresh)
                is TodayUiState.Content -> ContentState(state, onRefresh)
            }
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
private fun ContentState(content: TodayUiState.Content, onRefresh: () -> Unit) {
    val locale = Locale.getDefault()
    val is24h = android.text.format.DateFormat.is24HourFormat(LocalContext.current)
    val timeFmt = remember(locale, is24h) { Formats.timeFormatter(is24h, locale) }
    // Unit settings arrive with the Settings screen (Fase 4); until a switch exists,
    // the defaults are the truth, not a guess.
    val units = remember { UnitSettings() }

    PullToRefreshBox(isRefreshing = content.refreshing, onRefresh = onRefresh) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { CanvasHeader(content, units, timeFmt, locale) }

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

            item { SectionTitle(stringResource(R.string.section_next_hours)) }
            item { NextHours(content, units, is24h, timeFmt, locale) }

            if (content.timeline.isNotEmpty()) {
                item { SectionTitle(stringResource(R.string.section_rest_of_day)) }
                item { RestOfDay(content, timeFmt) }
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
    units: UnitSettings,
    timeFmt: DateTimeFormatter,
    locale: Locale
) {
    val sky = content.sky
    val current = content.report.current
    SkyCanvas(
        gradient = SkyPalette.gradient(
            sunAltitudeDeg = sky.sunAltitudeDeg,
            cloudPct = sky.cloudPct,
            precipPct = sky.precipPct,
            moonIllumination = sky.moonIllumination,
            moonAltitudeDeg = sky.moonAltitudeDeg
        ),
        height = 280.dp
    ) {
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
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(WeatherText.condition(current.condition.wmoCode)),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Text(
                    text = stringResource(
                        R.string.feels_like,
                        Formats.temperature(
                            current.feelsLikeC, units.temperature, locale, decimals = 1
                        )
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f)
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

/** The sentence, or nothing. Localization happens here, on top of [HeadlineEngine]'s
 * language-free answer. */
@Composable
private fun headlineText(headline: Headline?, timeFmt: DateTimeFormatter): String? {
    fun t(at: LocalDateTime): String = at.format(timeFmt)
    return when (headline) {
        null -> null
        is Headline.Severe -> stringResource(
            when (headline.bucket) {
                AlertEngine.SevereBucket.THUNDER -> R.string.headline_severe_thunder
                AlertEngine.SevereBucket.ICE -> R.string.headline_severe_ice
                AlertEngine.SevereBucket.RAIN -> R.string.headline_severe_rain
                AlertEngine.SevereBucket.SNOW -> R.string.headline_severe_snow
            },
            t(headline.at)
        )
        is Headline.WetSoon -> when {
            headline.snow && headline.clearsAt != null ->
                stringResource(R.string.headline_snow_soon_clearing, t(headline.at), t(headline.clearsAt))
            headline.snow -> stringResource(R.string.headline_snow_soon, t(headline.at))
            headline.clearsAt != null ->
                stringResource(R.string.headline_wet_soon_clearing, t(headline.at), t(headline.clearsAt))
            else -> stringResource(R.string.headline_wet_soon, t(headline.at))
        }
        is Headline.WetNow -> when {
            headline.snow && headline.stopsAt != null ->
                stringResource(R.string.headline_snow_now_stopping, t(headline.stopsAt))
            headline.snow -> stringResource(R.string.headline_snow_now)
            headline.stopsAt != null ->
                stringResource(R.string.headline_wet_now_stopping, t(headline.stopsAt))
            else -> stringResource(R.string.headline_wet_now)
        }
    }
}

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
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowTiles.forEach { tile ->
                    MetricTile(
                        icon = tile.icon(),
                        label = stringResource(tile.label),
                        value = tile.value,
                        meaning = stringResource(tile.meaning),
                        modifier = Modifier.weight(1f)
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
