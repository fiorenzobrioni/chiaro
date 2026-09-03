package com.callbackdev.chiaro.ui.guide

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DateRange
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
import com.callbackdev.chiaro.ui.components.FreshnessChip
import com.callbackdev.chiaro.ui.components.MetricTile
import com.callbackdev.chiaro.ui.components.VerdictChip
import com.callbackdev.chiaro.ui.components.VerdictKind
import com.callbackdev.chiaro.ui.format.Formats
import com.callbackdev.chiaro.ui.icons.ChiaroIcons
import com.callbackdev.chiaro.ui.sky.SkyText
import com.callbackdev.chiaro.ui.theme.ChiaroTheme
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale

/**
 * The guide (VISION §5.7): illustrated, and re-openable forever from Settings — a
 * definition offered before you have seen the thing it defines does not stick, and a
 * screen shown once cannot be consulted the day the question actually arrives.
 *
 * Rewritten in the 4th device pass, on the committente's call, into a **tour of the
 * four screens**: what each one answers, what it can do, and the handful of things a
 * screen cannot say out loud (that the sky is computed rather than photographed, that
 * a reminder is loose on purpose, that a failed update is a line in the Journal). The
 * old chapter explaining why there is no radar map is gone: a guide is where a
 * product says what it does, not where it defends what it is not, and the useful half
 * of that chapter — where the answer to "is it about to rain?" actually lives —
 * survives inside the Today tour.
 *
 * The rule that stays is the one that made it good: it never teaches a control. It
 * teaches what a screen is FOR, and it teaches by showing the real components —
 * [VerdictChip], [MetricTile], [FreshnessChip], the drift strip — each with a caption
 * saying it is an example, never a number posing as this reader's own.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuideRoute(onBack: () -> Unit) {
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
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        )
    }
}

@Composable
private fun GuideContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Paragraph(stringResource(R.string.guide_intro))

        // The map first: four glyphs a reader can recognize at the bottom of the app,
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

        Chapter(Icons.Outlined.DateRange, stringResource(R.string.guide_journal_title))
        Paragraph(stringResource(R.string.guide_journal_p1))
        Feature(
            stringResource(R.string.guide_journal_entries_title),
            stringResource(R.string.guide_journal_entries_body)
        )
        Feature(
            stringResource(R.string.guide_journal_drift_title),
            stringResource(R.string.guide_journal_drift_body)
        )
        DriftSample()
        Caption(stringResource(R.string.guide_journal_drift_caption))

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
            stringResource(R.string.guide_widgets_three_title),
            stringResource(R.string.guide_widgets_three_body)
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

/** The bottom bar, spelled out: the same four glyphs, in the same order. */
@Composable
private fun TabMap() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TabRow(ChiaroIcons.tabToday, R.string.tab_today, R.string.guide_map_today)
        TabRow(ChiaroIcons.tabSky, R.string.tab_sky, R.string.guide_map_sky)
        TabRow(Icons.Outlined.Notifications, R.string.tab_alerts, R.string.guide_map_alerts)
        TabRow(Icons.Outlined.DateRange, R.string.tab_journal, R.string.guide_map_journal)
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

/** A Sky row, built exactly as the Sky screen builds one: name, the day in a word,
 * the hour, and the verdict with its number underneath. */
@Composable
private fun MomentSample() {
    val locale = Locale.getDefault()
    val is24h = android.text.format.DateFormat.is24HourFormat(LocalContext.current)
    val time = LocalTime.of(6, 47).format(Formats.timeFormatter(is24h, locale))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(
            imageVector = ChiaroIcons.sunrise,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(26.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(SkyText.nameRes("sun.rise")),
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = stringResource(R.string.sky_day_tomorrow) + " · " + time,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            VerdictChip(
                kind = VerdictKind.PASS,
                label = stringResource(R.string.verdict_pass),
                evidence = stringResource(R.string.guide_verdict_evidence_pass)
            )
        }
    }
}

/** The age chip of Today, tappable there, inert here. */
@Composable
private fun FreshnessSample() {
    FreshnessChip(
        age = pluralStringResource(R.plurals.freshness_hours_ago, 2, 2),
        onRetry = {}
    )
}

/** Two tiles of the details grid: the value, and the line that says what to do. */
@Composable
private fun MetricSample() {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        MetricTile(
            icon = ChiaroIcons.uv,
            label = stringResource(R.string.metric_uv),
            value = "7",
            meaning = stringResource(R.string.uv_meaning_high),
            modifier = Modifier.weight(1f)
        )
        MetricTile(
            icon = ChiaroIcons.humidity,
            label = stringResource(R.string.metric_humidity),
            value = "62%",
            meaning = stringResource(R.string.humidity_meaning_comfortable),
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * The drift strip in miniature, drawn the way the Journal draws it: a row per day
 * ahead, a column per update, the rain ramp for color. The days are the real next
 * three — a strip labelled with invented weekdays would be the guide teaching a
 * calendar nobody has — and only the percentages are the example.
 */
@Composable
private fun DriftSample() {
    val locale = Locale.getDefault()
    val today = LocalDate.now()
    val rows = listOf(
        listOf(70, 60, 55, 40, 30, 20), // a day that kept improving
        listOf(20, 25, 20, 20, 15, 20), // a day that held
        listOf(10, 20, 35, 50, 60, 70)  // a day that turned
    )
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        rows.forEachIndexed { index, percentages ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = Formats.dayLabel(today.plusDays(index + 1L), locale),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(52.dp)
                )
                percentages.forEach { percent ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(16.dp)
                            .padding(1.dp)
                            .background(ChiaroTheme.colors.rainAt(percent))
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GuidePreview() {
    ChiaroTheme(dynamicColor = false) {
        GuideRoute(onBack = {})
    }
}
