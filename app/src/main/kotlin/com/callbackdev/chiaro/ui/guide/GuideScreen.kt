package com.callbackdev.chiaro.ui.guide

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.domain.settings.TemperatureUnit
import com.callbackdev.chiaro.ui.components.DayRow
import com.callbackdev.chiaro.ui.components.FreshnessChip
import com.callbackdev.chiaro.ui.components.LightPhase
import com.callbackdev.chiaro.ui.components.MetricTile
import com.callbackdev.chiaro.ui.components.SegmentGap
import com.callbackdev.chiaro.ui.components.gridSegmentShape
import com.callbackdev.chiaro.ui.components.RibbonHour
import com.callbackdev.chiaro.ui.components.humidityTrack
import com.callbackdev.chiaro.ui.components.uvTrack
import com.callbackdev.chiaro.ui.components.VerdictChip
import com.callbackdev.chiaro.ui.components.VerdictKind
import com.callbackdev.chiaro.ui.format.Formats
import com.callbackdev.chiaro.ui.format.LocalClock
import com.callbackdev.chiaro.ui.format.currentLocale
import com.callbackdev.chiaro.ui.icons.ChiaroIcons
import com.callbackdev.chiaro.ui.icons.ConditionGlyph
import com.callbackdev.chiaro.ui.sky.SkyRow
import com.callbackdev.chiaro.ui.sky.SkyText
import com.callbackdev.chiaro.ui.components.segmentShape
import com.callbackdev.chiaro.ui.theme.ChiaroTheme
import java.time.LocalDate
import java.time.LocalTime

/**
 * The guide (VISION §5.7): illustrated, and re-openable forever from Settings — a
 * definition offered before you have seen the thing it defines does not stick, and a
 * screen shown once cannot be consulted the day the question actually arrives.
 *
 * Rewritten in the 4th device pass, on the committente's call, into a **tour of the
 * screens** (three since the Journal left, 14 ott 2026): what each one answers, what it
 * can do, and the handful of things a screen cannot say out loud (that the sky is
 * computed rather than photographed, that a reminder is loose on purpose). The
 * old chapter explaining why there is no radar map is gone: a guide is where a
 * product says what it does, not where it defends what it is not, and the useful half
 * of that chapter — where the answer to "is it about to rain?" actually lives —
 * survives inside the Today tour.
 *
 * The rule that stays is the one that made it good: it never teaches a control. It
 * teaches what a screen is FOR, and it teaches by showing the real components —
 * [VerdictChip], [MetricTile], [FreshnessChip], the week's rows — each with a caption
 * saying it is an example, never a number posing as this reader's own.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuideRoute(
    onBack: () -> Unit,
    /** The events guide, the one chapter that is a document of its own: fifty-one pages
     * do not belong inside a tour, and a reader who opens it here has to be able to come
     * back to the paragraph they left — the shell opens it over this page, which keeps
     * its place under it. */
    onOpenSkyGuide: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.guide_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        GuideContent(
            onOpenSkyGuide = onOpenSkyGuide,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        )
    }
}

@Composable
private fun GuideContent(onOpenSkyGuide: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Paragraph(stringResource(R.string.guide_intro))

        // The map first: three glyphs a reader can recognize at the bottom of the app,
        // so every chapter below is already placed before it starts.
        Chapter(icon = null, text = stringResource(R.string.guide_map_title))
        TabMap()
        Paragraph(stringResource(R.string.guide_map_note))

        Chapter(ChiaroIcons.tabToday, stringResource(R.string.guide_today_title))
        Paragraph(stringResource(R.string.guide_today_p1))
        Feature(
            stringResource(R.string.guide_today_headline_title),
            stringResource(R.string.guide_today_headline_body)
        )
        Feature(
            stringResource(R.string.guide_today_rain_title),
            stringResource(R.string.guide_today_rain_body)
        )
        Feature(
            stringResource(R.string.guide_today_fresh_title),
            stringResource(R.string.guide_today_fresh_body)
        )
        FreshnessSample()
        Caption(stringResource(R.string.guide_today_fresh_caption))
        Feature(
            stringResource(R.string.guide_today_hours_title),
            stringResource(R.string.guide_today_hours_body)
        )
        Feature(
            stringResource(R.string.guide_today_rest_title),
            stringResource(R.string.guide_today_rest_body)
        )
        Feature(
            stringResource(R.string.guide_today_week_title),
            stringResource(R.string.guide_today_week_body)
        )
        WeekSample()
        Caption(stringResource(R.string.guide_today_week_caption))
        Feature(
            stringResource(R.string.guide_today_details_title),
            stringResource(R.string.guide_today_details_body)
        )
        MetricSample()
        Caption(stringResource(R.string.guide_today_details_caption))
        Feature(
            stringResource(R.string.guide_today_changed_title),
            stringResource(R.string.guide_today_changed_body)
        )
        Feature(
            stringResource(R.string.guide_today_places_title),
            stringResource(R.string.guide_today_places_body)
        )

        Chapter(ChiaroIcons.tabSky, stringResource(R.string.guide_sky_title))
        Paragraph(stringResource(R.string.guide_sky_p1))
        Feature(
            stringResource(R.string.guide_sky_tonight_title),
            stringResource(R.string.guide_sky_tonight_body)
        )
        Paragraph(stringResource(R.string.guide_verdicts_p1))
        VerdictSampler()
        Caption(stringResource(R.string.guide_verdicts_caption))
        Paragraph(stringResource(R.string.guide_verdicts_p2))
        Feature(
            stringResource(R.string.guide_sky_moments_title),
            stringResource(R.string.guide_sky_moments_body)
        )
        MomentSample()
        Caption(stringResource(R.string.guide_sky_moment_caption))
        // Fase 28 put a glyph on nine rows and a bearing under them, and a reader meets
        // the camera on a fresh install — the evening golden hour is a default line.
        // A mark nobody explained is a mark nobody trusts.
        Feature(
            stringResource(R.string.guide_sky_camera_title),
            stringResource(R.string.guide_sky_camera_body)
        )
        Feature(
            stringResource(R.string.guide_sky_bell_title),
            stringResource(R.string.guide_sky_bell_body)
        )
        Feature(
            stringResource(R.string.guide_sky_events_title),
            stringResource(R.string.guide_sky_events_body)
        )
        Feature(
            stringResource(R.string.guide_sky_catalog_title),
            stringResource(R.string.guide_sky_catalog_body)
        )
        TextButton(onClick = onOpenSkyGuide) {
            Text(stringResource(R.string.guide_sky_events_action))
        }

        Chapter(Icons.Outlined.Notifications, stringResource(R.string.guide_alerts_title))
        Paragraph(stringResource(R.string.guide_alerts_p1))
        Feature(
            stringResource(R.string.guide_alerts_ready_title),
            stringResource(R.string.guide_alerts_ready_body)
        )
        Feature(
            stringResource(R.string.guide_alerts_yours_title),
            stringResource(R.string.guide_alerts_yours_body)
        )
        Feature(
            stringResource(R.string.guide_alerts_preview_title),
            stringResource(R.string.guide_alerts_preview_body)
        )
        Feature(
            stringResource(R.string.guide_alerts_message_title),
            stringResource(R.string.guide_alerts_message_body)
        )
        Feature(
            stringResource(R.string.guide_alerts_battery_title),
            stringResource(R.string.guide_alerts_battery_body)
        )
        // Fase 11: what an official warning is, who issues it and when it arrives. The
        // attribution is not decoration — CC BY 4.0 requires it, and it is also the only
        // place a reader learns where the warning came from.
        Feature(
            stringResource(R.string.guide_alerts_official_title),
            stringResource(R.string.guide_alerts_official_body)
        )
        Caption(stringResource(R.string.guide_alerts_official_credit))

        Chapter(Icons.Outlined.LocationOn, stringResource(R.string.guide_places_title))
        Paragraph(stringResource(R.string.guide_places_p1))
        Feature(
            stringResource(R.string.guide_places_search_title),
            stringResource(R.string.guide_places_search_body)
        )
        Feature(
            stringResource(R.string.guide_places_gps_title),
            stringResource(R.string.guide_places_gps_body)
        )
        Feature(
            stringResource(R.string.guide_places_order_title),
            stringResource(R.string.guide_places_order_body)
        )

        Chapter(Icons.Outlined.Home, stringResource(R.string.guide_widgets_title))
        Feature(
            stringResource(R.string.guide_widgets_all_title),
            stringResource(R.string.guide_widgets_all_body)
        )
        Feature(
            stringResource(R.string.guide_widgets_config_title),
            stringResource(R.string.guide_widgets_config_body)
        )
        Feature(
            stringResource(R.string.guide_widgets_honest_title),
            stringResource(R.string.guide_widgets_honest_body)
        )

        Chapter(Icons.Outlined.Settings, stringResource(R.string.guide_settings_title))
        Paragraph(stringResource(R.string.guide_settings_body))

        Chapter(ChiaroIcons.cloud, stringResource(R.string.guide_data_title))
        Paragraph(stringResource(R.string.guide_data_p1))
        Paragraph(stringResource(R.string.guide_data_p2))
        Paragraph(stringResource(R.string.guide_data_p3))
    }
}

// ---------------------------------------------------------------------------------
// The prose kit
// ---------------------------------------------------------------------------------

@Composable
private fun Chapter(icon: ImageVector?, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(top = 20.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null, // decoration: the title right here says it all
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        }
        Text(text, style = MaterialTheme.typography.titleLarge)
    }
}

/** Guide prose is bodyLarge (DESIGN §5): 16/24, made to be read, not scanned. */
@Composable
private fun Paragraph(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyLarge)
}

/**
 * One thing a screen can do: its name, then what it is for. No leading icon — the
 * chapter's own glyph already says which screen we are on, and a second column of
 * invented iconography would be decoration pretending to be a legend.
 */
@Composable
private fun Feature(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(text = body, style = MaterialTheme.typography.bodyMedium)
    }
}

/** What a sample above it stands for: always said, so no example reads as data. */
@Composable
private fun Caption(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

// ---------------------------------------------------------------------------------
// The samples: the app's own components, shown as themselves
// ---------------------------------------------------------------------------------

/** The bottom bar, spelled out: the same three glyphs, in the same order. */
@Composable
private fun TabMap() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TabRow(ChiaroIcons.tabToday, R.string.tab_today, R.string.guide_map_today)
        TabRow(ChiaroIcons.tabSky, R.string.tab_sky, R.string.guide_map_sky)
        TabRow(Icons.Outlined.Notifications, R.string.tab_alerts, R.string.guide_map_alerts)
    }
}

@Composable
private fun TabRow(icon: ImageVector, nameRes: Int, bodyRes: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null, // the name is right beside it
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(26.dp)
        )
        Column {
            Text(stringResource(nameRes), style = MaterialTheme.typography.titleSmall)
            Text(
                text = stringResource(bodyRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * The four answers, shown as themselves: real [VerdictChip]s, introduced by the
 * sentence above as an example — teaching by showing the actual thing, with the
 * caption saying what the sample numbers stand for.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VerdictSampler() {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        VerdictChip(
            kind = VerdictKind.PASS,
            label = stringResource(R.string.verdict_pass),
            evidence = stringResource(R.string.guide_verdict_evidence_pass)
        )
        VerdictChip(
            kind = VerdictKind.UNSTABLE,
            label = stringResource(R.string.verdict_unstable),
            evidence = stringResource(R.string.guide_verdict_evidence_unstable)
        )
        VerdictChip(
            kind = VerdictKind.FAIL,
            label = stringResource(R.string.verdict_fail),
            evidence = stringResource(R.string.guide_verdict_evidence_fail)
        )
        // The one chip with no number, exactly as in the app: not knowing has no
        // arithmetic to show.
        VerdictChip(
            kind = VerdictKind.UNKNOWN,
            label = stringResource(R.string.verdict_unknown),
            evidence = null
        )
    }
}

/** A Sky row, drawn by the Sky screen's own [SkyRow] (14 ott 2026): until then this was a
 * copy, and the copy had stayed on the row of early September (a small grey glyph, the
 * day and the hour in the line) while the screen moved on. The day is the list's heading
 * there, so the caption says it here. */
@Composable
private fun MomentSample() {
    val locale = currentLocale()
    val is24h = android.text.format.DateFormat.is24HourFormat(LocalContext.current)
    val time = LocalTime.of(6, 47).format(Formats.timeFormatter(is24h, locale))
    SkyRow(
        icon = ChiaroIcons.skyJob("sun.rise"),
        name = stringResource(SkyText.nameRes("sun.rise")),
        photographic = false,
        time = time,
        supporting = "",
        shape = segmentShape(0, 1),
        onOpen = null,
        bell = null,
        verdict = {
            VerdictChip(
                kind = VerdictKind.PASS,
                label = stringResource(R.string.verdict_pass),
                evidence = stringResource(R.string.guide_verdict_evidence_pass)
            )
        },
        inset = 0.dp
    )
}

/** The age chip of Today, tappable there, inert here. */
@Composable
private fun FreshnessSample() {
    FreshnessChip(
        age = pluralStringResource(R.plurals.freshness_hours_ago, 2, 2),
        onRetry = {}
    )
}

/**
 * Two rows of the week, drawn by the week's own [DayRow] (13 ott 2026): the two bars on
 * the screen that ask to be decoded rather than read, so the guide shows them and its
 * caption says what to look at. Today is clear, with the dot on the range bar and the
 * spent morning faded; tomorrow clouds over after lunch and rains from 15 to 17, with
 * the wet lane under its ribbon. The days are the real ones; the light is a generic
 * mid-latitude day and the numbers are the example.
 */
@Composable
private fun WeekSample() {
    val locale = currentLocale()
    val today = LocalDate.now(LocalClock.current)
    val phases = listOf(
        LightPhase(0f, 0.22f, -30.0),
        LightPhase(0.22f, 0.25f, -15.0),
        LightPhase(0.25f, 0.27f, -9.0),
        LightPhase(0.27f, 0.29f, -4.0),
        LightPhase(0.29f, 0.32f, 2.0),
        LightPhase(0.32f, 0.76f, 30.0),
        LightPhase(0.76f, 0.79f, 2.0),
        LightPhase(0.79f, 0.81f, -4.0),
        LightPhase(0.81f, 0.83f, -9.0),
        LightPhase(0.83f, 0.86f, -15.0),
        LightPhase(0.86f, 1f, -30.0)
    )
    fun hours(cloud: (Int) -> Int, chance: (Int) -> Int, wet: (Int) -> Boolean) =
        (0..24).map { h -> RibbonHour(h / 24f, cloud(h), chance(h), wet(h) && h < 24) }
    val clear = hours({ 10 }, { 0 }, { false })
    val turning = hours(
        cloud = { h -> when { h < 11 -> 15; h < 14 -> 15 + (h - 10) * 25; h < 21 -> 100; else -> 60 } },
        chance = { h -> if (h in 15..17) 70 else 5 },
        wet = { h -> h in 15..17 }
    )
    val days = listOf(
        Triple(stringResource(R.string.week_today), Triple(12.0, 21.0, 0), clear),
        Triple(Formats.dayLabel(today.plusDays(1), locale), Triple(10.0, 16.0, 70), turning)
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        days.forEachIndexed { i, (label, range, sky) ->
            val (low, high, pct) = range
            DayRow(
                dayLabel = label,
                condition = ConditionGlyph(if (i == 0) 0 else 61),
                rainPct = pct,
                rainLabel = pct.takeIf { it >= 20 }?.let { Formats.percent(it, locale) },
                lowC = low,
                highC = high,
                lowLabel = Formats.temperature(low, TemperatureUnit.CELSIUS, locale),
                highLabel = Formats.temperature(high, TemperatureUnit.CELSIUS, locale),
                scaleLowC = 10.0,
                scaleHighC = 21.0,
                phases = phases,
                description = stringResource(R.string.guide_today_week_caption),
                nowC = if (i == 0) 17.0 else null,
                // The chevron the week's rows carry, closed: the body above says to tap a
                // day, and the picture shows the mark that says so on Today.
                expanded = false,
                sky = sky,
                wetLane = true,
                skyFrom = if (i == 0) 0.42f else 0f
            )
        }
    }
}

/** Two tiles of the details grid: the value, and the line that says what to do. */
@Composable
private fun MetricSample() {
    // Two segments of the details grid, as Today draws them (15 ott 2026).
    Row(horizontalArrangement = Arrangement.spacedBy(SegmentGap)) {
        MetricTile(
            icon = ChiaroIcons.uv,
            label = stringResource(R.string.metric_uv),
            value = "7",
            meaning = stringResource(R.string.uv_meaning_high),
            track = uvTrack(7),
            shape = gridSegmentShape(0, 1, 0, 2),
            modifier = Modifier.weight(1f)
        )
        MetricTile(
            icon = ChiaroIcons.humidity,
            label = stringResource(R.string.metric_humidity),
            value = "62%",
            meaning = stringResource(R.string.dew_meaning_pleasant),
            track = humidityTrack(62),
            shape = gridSegmentShape(0, 1, 1, 2),
            modifier = Modifier.weight(1f)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun GuidePreview() {
    ChiaroTheme(dynamicColor = false) {
        GuideRoute(onBack = {}, onOpenSkyGuide = {})
    }
}
