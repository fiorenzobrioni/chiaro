package com.callbackdev.chiaro.ui.today

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.ui.components.HourStrip
import com.callbackdev.chiaro.ui.components.SegmentGap
import com.callbackdev.chiaro.ui.components.segmentShape
import com.callbackdev.chiaro.ui.format.LocalClock
import com.callbackdev.chiaro.ui.theme.CardCorner
import com.callbackdev.chiaro.ui.theme.GroupCorner
import java.time.Duration
import java.time.Instant

// ---------------------------------------------------------------------------------
// The four states. §8.11: an empty state names its one action; a skeleton is visibly
// a skeleton; an error says what failed in words and offers the retry.
// ---------------------------------------------------------------------------------

/**
 * «Nessun luogo», the one action that fixes it, and [body], the page's own sentence about
 * what a place is for: Today's by default, and Alerts' since 15.2 (28 Oct 2026), which draws
 * this same state rather than a page of promises about nowhere.
 */
@Composable
internal fun NoPlaceState(
    onOpenPlaces: () -> Unit,
    body: String = stringResource(R.string.empty_no_place_body)
) {
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
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        TextButton(onClick = onOpenPlaces) {
            Text(stringResource(R.string.empty_no_place_action))
        }
    }
}

@Composable
internal fun EmptyState(state: TodayUiState.Empty, onRefresh: () -> Unit) {
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
internal val PlainHeaderHeight = 48.dp + HeaderVerticalPadding * 2

/** One hour cell at 100% type with its rain lane, quoted from `HourStrip`: 16 (hour) + 4 +
 * 32 (icon) + 4 + 42 (the temperature curve's band) + 4 + 14 (lane) + 16 (the chance), since
 * the strip was made compact (15 ott 2026). */
internal val SkeletonHourCellHeight = 132.dp

/** A section title's band on the paper: its 12dp, a `titleMedium` line, 12 under it. */
private val SkeletonTitleBand = ItemGap + 24.dp + ItemGap

/**
 * Blocks of `surfaceContainerHigh` in the shape of the real layout: unmistakably not
 * data (§1.1 — no placeholder may render as a value, so none of these carries text).
 *
 * In the shape of the real layout, re-measured whenever the layout moves: on 8 set 2026 the
 * sky block had kept corners and a height from before, and on 15 ott 2026 the hours went on
 * a card and the agenda into segments (DESIGN §8.0), so the skeleton draws a card as tall as
 * the hours' (16 + a cell + 16) and three of the agenda's segments. A skeleton that is the
 * wrong shape is a small lie told for one second; still a lie. The titles are bands of empty
 * paper: a block where a word will be would read as a word that failed to load.
 */
@Composable
internal fun TodaySkeleton() {
    val block = MaterialTheme.colorScheme.surfaceContainerHigh
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        // Straight edges, like the canvas: the sky is the ground, not a card.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(CanvasBaseHeight - PlainHeaderHeight)
                .background(block)
        )
        Box(
            modifier = Modifier
                .padding(start = 16.dp, top = SkeletonTitleBand, end = 16.dp)
                .fillMaxWidth()
                .height(16.dp + SkeletonHourCellHeight + 16.dp)
                .background(block, RoundedCornerShape(CardCorner))
        )
        Column(
            modifier = Modifier.padding(start = 16.dp, top = SectionGap + SkeletonTitleBand - ItemGap, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(SegmentGap)
        ) {
            repeat(3) { index ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .background(block, segmentShape(index, 3))
                )
            }
        }
    }
}

@Composable
internal fun ErrorBanner(error: TodayError, onRetry: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        // A banner of one statement: the groups' corner (DESIGN §6), as the warning's.
        shape = RoundedCornerShape(GroupCorner),
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
                // `bodyMedium`, the size of a sentence everywhere else on the page: an error is
                // not a footnote.
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onRetry) {
                Text(stringResource(R.string.error_retry))
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
internal fun GuideCard(onOpen: () -> Unit, onDismiss: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ),
        // The notice's card, as on Sky and Alerts (DESIGN §6): the app's card corner and the
        // 20dp of inner air it asks for.
        shape = RoundedCornerShape(CardCorner),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 20.dp, top = 8.dp, end = 8.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Alerts' scale (review of Today, 15 ott 2026): a card's title at
                // `titleMedium`, its sentence at `bodyMedium`.
                Text(
                    text = stringResource(R.string.guide_card_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(R.string.guide_card_body),
                    style = MaterialTheme.typography.bodyMedium
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
            modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
        ) {
            Text(stringResource(R.string.guide_card_open))
        }
    }
}

/** How old the data really is, in the largest unit that keeps a whole number. */
@Composable
internal fun freshnessAge(lastSync: Instant): String {
    val elapsed = Duration.between(lastSync, Instant.now(LocalClock.current))
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
