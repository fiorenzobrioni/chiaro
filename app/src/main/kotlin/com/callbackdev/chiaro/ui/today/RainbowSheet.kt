package com.callbackdev.chiaro.ui.today

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.domain.model.Coordinates
import com.callbackdev.chiaro.domain.sky.RainbowWindow
import com.callbackdev.chiaro.ui.icons.ChiaroIcons
import com.callbackdev.chiaro.ui.icons.WeatherIconSize
import com.callbackdev.chiaro.ui.sky.LookBlock
import com.callbackdev.chiaro.ui.sky.SkyText
import com.callbackdev.chiaro.ui.sky.compass.CompassView
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * The rainbow's page (15 ott 2026), opened from its row in the rest of the day: the one row
 * on Today whose whole use is a direction, and until then the one that opened nothing.
 *
 * What a rainbow is, in two sentences, because «turn your back on the sun» is the half nobody
 * knows; how high the bow will stand, in words; that it is a possibility and why; and «Dove
 * guardare», whose button turns the sheet into the Sky's compass.
 *
 * **The direction is the moment's.** Inside the window the bow is where it is now, opposite
 * the sun as it stands (it moves some 15° an hour, a compass point in two); ahead of it, the
 * window's own bearing, the one its row prints, and the height at the window's middle.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RainbowSheet(
    item: TimelineItem,
    now: LocalDateTime,
    zone: ZoneId,
    coordinates: Coordinates,
    timeFmt: DateTimeFormatter,
    onDismiss: () -> Unit
) {
    val start = item.at
    val end = item.until ?: item.at.plusHours(1)
    val inside = !now.isBefore(start) && now.isBefore(end)
    val moment = if (inside) now else start.plus(java.time.Duration.between(start, end).dividedBy(2))
    val sight = RainbowWindow.sight(moment.atZone(zone).toInstant(), coordinates)
    val bearing = (if (inside) sight?.lookTowardsDeg else null) ?: item.bearingDeg ?: sight?.lookTowardsDeg
    val from = start.format(timeFmt)
    val to = end.format(timeFmt)

    val sheetState = rememberModalBottomSheetState()
    var compassOpen by rememberSaveable(item.at) { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        val direction = bearing?.let { stringResource(SkyText.bearingRes(it)) }
        if (compassOpen && bearing != null && direction != null) {
            BackHandler { compassOpen = false }
            LaunchedEffect(Unit) { sheetState.expand() }
            CompassView(
                name = stringResource(R.string.rainbow_title),
                whenLine = stringResource(R.string.rainbow_compass_when, from, to),
                direction = direction,
                bearingDeg = bearing,
                icon = ChiaroIcons.rainbow,
                coordinates = coordinates,
                onBack = { compassOpen = false }
            )
            return@ModalBottomSheet
        }
        RainbowPage(
            from = from,
            to = to,
            rainPct = item.pct ?: 0,
            direction = direction,
            topAltitudeDeg = sight?.topAltitudeDeg,
            onOpenCompass = { compassOpen = true }
        )
    }
}

/** The page itself, apart from its sheet: what the reader reads before the compass. */
@Composable
internal fun RainbowPage(
    from: String,
    to: String,
    rainPct: Int,
    direction: String?,
    topAltitudeDeg: Double?,
    onOpenCompass: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Icon(
                imageVector = ChiaroIcons.rainbow,
                contentDescription = null, // the name is right beside it
                tint = Color.Unspecified,
                modifier = Modifier.size(WeatherIconSize.Page)
            )
            Column {
                Text(
                    text = stringResource(R.string.rainbow_title),
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = stringResource(R.string.rainbow_when, from, to, rainPct),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        direction?.let { LookBlock(direction = it, onOpen = onOpenCompass) }
        Text(stringResource(R.string.rainbow_about), style = MaterialTheme.typography.bodyLarge)
        topAltitudeDeg?.let {
            Text(stringResource(rainbowHeightRes(it)), style = MaterialTheme.typography.bodyLarge)
        }
        Text(
            text = stringResource(R.string.rainbow_honest),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * How high the bow's top stands, in words (pure, `RainbowSheetTest`): never in degrees (the
 * app prints none). Its top is 42° less the sun's altitude, so it runs from the horizon to 42°:
 * past [TallBowDeg] the arc is most of a half circle, under [LowBowDeg] a band on the horizon.
 */
@StringRes
internal fun rainbowHeightRes(topAltitudeDeg: Double): Int = when {
    topAltitudeDeg >= TallBowDeg -> R.string.rainbow_height_tall
    topAltitudeDeg >= LowBowDeg -> R.string.rainbow_height_mid
    else -> R.string.rainbow_height_low
}

/** A top this high or more: the sun under 14°, the arc most of a half circle. */
internal const val TallBowDeg = 28.0

/** A top under this: the sun past 28°, the arc a band just above the horizon. */
internal const val LowBowDeg = 14.0
