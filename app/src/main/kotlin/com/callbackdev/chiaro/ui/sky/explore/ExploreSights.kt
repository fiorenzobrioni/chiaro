package com.callbackdev.chiaro.ui.sky.explore

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.domain.sky.MeteorShowerTable
import com.callbackdev.chiaro.domain.sky.Sight
import com.callbackdev.chiaro.domain.sky.SightSubject
import com.callbackdev.chiaro.domain.sky.SkyFigure
import com.callbackdev.chiaro.domain.sky.SkyFigureKind
import com.callbackdev.chiaro.domain.sky.SkyJobCatalog
import com.callbackdev.chiaro.ui.components.ChiaroSheet
import com.callbackdev.chiaro.ui.components.SegmentedGroup
import com.callbackdev.chiaro.ui.components.TimelineRow
import com.callbackdev.chiaro.ui.format.currentLocale
import com.callbackdev.chiaro.ui.icons.ChiaroIcons
import com.callbackdev.chiaro.ui.sky.ConstellationNames
import com.callbackdev.chiaro.ui.sky.NightForecast
import com.callbackdev.chiaro.ui.sky.SkyText
import com.callbackdev.chiaro.widget.window.WindowText
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The night of an event of the calendar ahead, as the list's sheet offers it (PLANNING,
 * Fase 16, step 16.9, board 5: «the night of an event»): the evening it begins on and the
 * moment it is the night of, by its line of the catalog. [fullMoon] is the calendar's own
 * full moon, which has the phase's name and not a line's.
 */
internal data class ExploreEventNight(val date: LocalDate, val jobId: String, val fullMoon: Boolean = false)

/**
 * «Worth seeing» (step 16.9, board 5): the things worth going out for on the night the
 * explorer is on, each at the hour it stands highest, and at the sheet's head the night
 * itself, to change it. The list is the engine's ([com.callbackdev.chiaro.domain.sky.WorthSeeing],
 * step 16.4): what it holds and why is written there, and this draws it.
 *
 * **The sheet's night is the page's.** Choosing another one here turns the sky behind the
 * sheet to it: there is one night on this page, and a list about a night the sky is not
 * showing would be a second one. The choices are the ones a reader asks for by name:
 * tonight, the night after, the nights of the events ahead ([eventNights]), and any other
 * from the calendar.
 *
 * **A row turns the sky to the thing, at its hour** ([onSight]): the sheet closes, the
 * page takes the row's hour, the camera goes to the thing and the sheet about it opens.
 *
 * A sheet over the page and in the app's theme, as the calendar is: it is a list to choose
 * from and not a part of the sky, and while it is open the sky under it has nothing to
 * answer. The sheet about a thing is the other kind, in the page, because there the sky
 * stays in the reader's hand (step 16.8, decision 1).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WorthSeeingSheet(
    sights: List<Sight>,
    date: LocalDate,
    tonightDate: LocalDate,
    /** The night's date as the page writes it: «Tonight, Thursday 12 November». */
    nightLine: String,
    /** The night after tonight is «tomorrow» until midnight; past it, with tonight under
     * way, tomorrow is the day the reader is already in. */
    afterMidnight: Boolean,
    eventNights: List<ExploreEventNight>,
    forecast: NightForecast,
    zone: ZoneId,
    timeFmt: DateTimeFormatter,
    onNight: (LocalDate) -> Unit,
    onCalendar: () -> Unit,
    onSight: (Sight) -> Unit,
    onDismiss: () -> Unit
) {
    ChiaroSheet(onDismissRequest = onDismiss) {
        WorthSeeingList(
            sights = sights,
            date = date,
            tonightDate = tonightDate,
            nightLine = nightLine,
            afterMidnight = afterMidnight,
            eventNights = eventNights,
            forecast = forecast,
            zone = zone,
            timeFmt = timeFmt,
            onNight = onNight,
            onCalendar = onCalendar,
            onSight = onSight
        )
    }
}

/** What the sheet holds, without the sheet: what a test draws and reads. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun WorthSeeingList(
    sights: List<Sight>,
    date: LocalDate,
    tonightDate: LocalDate,
    nightLine: String,
    afterMidnight: Boolean,
    eventNights: List<ExploreEventNight>,
    forecast: NightForecast,
    zone: ZoneId,
    timeFmt: DateTimeFormatter,
    onNight: (LocalDate) -> Unit,
    onCalendar: () -> Unit,
    onSight: (Sight) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val locale = currentLocale()
    val tomorrow = tonightDate.plusDays(1)
    // An event's night that is tonight's or the next has its chip already.
    val events = eventNights
        .filter { it.date != tonightDate && it.date != tomorrow }
        .distinctBy { it.date }
        .take(EventChips)
    val shortDate = remember(locale) { DateTimeFormatter.ofPattern("d MMM", locale) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp)
            .testTag(ExploreSightsTag)
    ) {
        Text(
            text = stringResource(R.string.sights_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 16.dp).semantics { heading() }
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)
        ) {
            NightChip(stringResource(R.string.explore_tonight), selected = date == tonightDate) { onNight(tonightDate) }
            NightChip(
                label = stringResource(if (afterMidnight) R.string.explore_night_next else R.string.sky_day_tomorrow),
                selected = date == tomorrow
            ) { onNight(tomorrow) }
            events.forEach { night ->
                val name = stringResource(if (night.fullMoon) R.string.moon_phase_full else SkyText.shortNameRes(night.jobId))
                NightChip(
                    label = stringResource(R.string.sights_event_night, name, night.date.format(shortDate)),
                    selected = date == night.date
                ) { onNight(night.date) }
            }
            // A night that is none of the above is one the reader chose: its chip is the calendar's.
            val chosen = date != tonightDate && date != tomorrow && events.none { it.date == date }
            AssistChip(
                onClick = onCalendar,
                label = {
                    Text(
                        text = if (chosen) date.format(shortDate) else stringResource(R.string.sights_other_night),
                        style = MaterialTheme.typography.labelLarge
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.DateRange,
                        contentDescription = if (chosen) stringResource(R.string.explore_pick_night) else null,
                        modifier = Modifier.size(AssistChipDefaults.IconSize)
                    )
                },
                colors = if (chosen) {
                    AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        leadingIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                } else {
                    AssistChipDefaults.assistChipColors()
                },
                modifier = Modifier.testTag(ExploreSightsCalendarTag)
            )
        }
        Text(
            text = nightLine,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)
        )
        // Why the sky behind this list has no clouds on it, where it has none: without the
        // line a sky drawn clear reads as a clear night.
        val note = when (forecast) {
            NightForecast.BEYOND -> R.string.sights_no_clouds_beyond
            NightForecast.PAST -> R.string.sights_no_clouds_past
            NightForecast.NONE -> R.string.sights_no_clouds_none
            NightForecast.REACHES -> null
        }
        if (note != null) Quiet(stringResource(note))
        if (sights.isEmpty()) {
            Text(
                text = stringResource(R.string.sights_none),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)
            )
        } else {
            Quiet(stringResource(R.string.sights_legend))
            Spacer(modifier = Modifier.height(12.dp))
            val turn = stringResource(R.string.sights_row_label)
            SegmentedGroup(
                segments = sights.map { sight ->
                    {
                        TimelineRow(
                            time = sight.best.atZone(zone).format(timeFmt),
                            icon = sightIcon(sight),
                            text = remember(sight.subject, locale) { SightWords.name(context, sight.subject) },
                            supporting = remember(sight, locale, timeFmt) { SightWords.line(context, sight, zone, timeFmt, locale) },
                            onClick = { onSight(sight) },
                            onClickLabel = turn
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun NightChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelLarge) },
        leadingIcon = if (selected) {
            {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null, // the chip's own state says it
                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                )
            }
        } else {
            null
        }
    )
}

/** A sentence in the quiet colour, under the night. */
@Composable
private fun Quiet(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp)
    )
}

/**
 * The drawing for a row, from the family the Sky's rows already use: the moon as its phase
 * of that night, a planet and a radiant as the catalog draws their lines (the star, the
 * falling star), and what is made of stars, a figure or the Milky Way, as the three stars
 * of the dark window.
 */
@Composable
private fun sightIcon(sight: Sight): ImageVector = when (val subject = sight.subject) {
    // A thin crescent as a crescent, not as the new moon's empty circle.
    is SightSubject.Moon -> ChiaroIcons.moonPhase(ObjectWords.moonDrawn(subject.phase, subject.illuminationPct, sight.best))
    is SightSubject.Wanderer -> ChiaroIcons.skyJob(SkyJobCatalog.JupiterNight.id)
    is SightSubject.Radiant -> ChiaroIcons.skyJob(MeteorShowerTable.jobId(subject.shower))
    is SightSubject.Figure, SightSubject.MilkyWay -> ChiaroIcons.skyJob(SkyJobCatalog.MilkyWayCore.id)
}

/** The list's words. From a [Context], as the sheet's are ([ObjectWords]). */
internal object SightWords {

    /** A figure's name: a constellation's own where it is one, its common name where it is
     * a part of one or stars of several. */
    fun figureName(context: Context, figure: SkyFigure): String {
        val res = when (figure.id) {
            "big_dipper" -> R.string.figure_big_dipper
            "great_square" -> R.string.figure_great_square
            "summer_triangle" -> R.string.figure_summer_triangle
            "pleiades" -> R.string.figure_pleiades
            "hyades" -> R.string.figure_hyades
            else -> ConstellationNames.nameRes(figure.constellation)
        }
        return res?.let(context::getString).orEmpty()
    }

    /** What the row is about, as the sky and the sheet name it. */
    fun name(context: Context, subject: SightSubject): String = when (subject) {
        is SightSubject.Moon -> context.getString(R.string.explore_object_moon)
        is SightSubject.Wanderer -> WindowText.planetLabel(context, subject.planet)
        is SightSubject.Figure -> figureName(context, subject.figure)
        SightSubject.MilkyWay -> context.getString(SkyText.nameRes(SkyJobCatalog.MilkyWayCore.id))
        is SightSubject.Radiant -> ObjectWords.title(context, SkyPick.of(subject))
    }

    /**
     * The two lines under the name: where it stands at its hour, in words, and under that
     * the stretch it is worth looking in; before the place, for the moon, its phase of that
     * night, and for a cluster that it is one (a reader who has never heard «Hyades» is
     * told what to look for).
     */
    fun line(context: Context, sight: Sight, zone: ZoneId, timeFmt: DateTimeFormatter, locale: Locale): String {
        val what = when (val subject = sight.subject) {
            is SightSubject.Moon -> ObjectWords.moonKind(context, subject.phase, subject.illuminationPct)
            is SightSubject.Figure -> context.getString(R.string.explore_kind_cluster)
                .takeIf { subject.figure.kind == SkyFigureKind.CLUSTER }
            else -> null
        }
        val where = ObjectWords.place(context, sight.azimuthDeg, sight.altitudeDeg)
        val window = context.getString(
            R.string.sights_window,
            sight.window.start.atZone(zone).format(timeFmt),
            sight.window.endInclusive.atZone(zone).format(timeFmt)
        )
        // Two lines: where, then when. On one, a phone broke the hours in the middle of the pair.
        return listOf(listOfNotNull(what, where).joinToString(" · "), window)
            .joinToString("\n") { it.replaceFirstChar { c -> c.titlecase(locale) } }
    }
}

/** The sheet's content, and the chip that opens the calendar, for a test to find. */
internal const val ExploreSightsTag = "explore-sights"
internal const val ExploreSightsCalendarTag = "explore-sights-calendar"

/** The events' nights the sheet offers by name: the soonest three, so the chips stay two lines. */
private const val EventChips = 3
