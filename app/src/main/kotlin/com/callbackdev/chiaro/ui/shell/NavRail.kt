package com.callbackdev.chiaro.ui.shell

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.ui.icons.ChiaroIcons

/**
 * From this window width the tabs move from the bottom bar to a rail at the start (DESIGN
 * §8.12c, 15 ott 2026): Material's line between a compact window and a medium one, which is
 * where an unfolded foldable, a tablet and a phone on its side all land. Under it the bar;
 * from it the rail, because a bar 600dp wide puts three targets a hand-span apart and takes
 * 80dp of height from a window that, on its side, has little of it.
 */
internal val RailMinWidth: Dp = 600.dp

/** The rail's own width, without the system inset beside it. */
internal val NavRailWidth: Dp = 80.dp

/**
 * How much of the page's start a rail stands over, or zero where there is none. Today alone
 * reads it: its sky runs under the status bar across the whole window, so it is laid out the
 * full width and keeps its content clear of the rail by this much (DESIGN §8.12c); every other
 * tab is simply laid out beside the rail.
 */
internal val LocalRailInset = compositionLocalOf { 0.dp }

/** The rail's width with the system inset at the start (a side navigation bar, a cutout). */
@Composable
internal fun railInset(): Dp =
    NavRailWidth + WindowInsets.safeDrawing.only(WindowInsetsSides.Start).asPaddingValues()
        .calculateStartPadding(LocalLayoutDirection.current)

/**
 * The bottom bar's three tabs on a rail (DESIGN §8.12c). The rail starts UNDER the status bar,
 * not behind it: over Today the status bar's band is the sky's, white icons on it, and a rail
 * reaching up behind the clock would have put white digits on a pale container. The items
 * stand in the middle of the rail's height, where a hand holding a tablet or an open foldable
 * by its sides reaches them with a thumb.
 */
@Composable
internal fun ChiaroNavRail(selected: ShellTab, onSelect: (ShellTab) -> Unit, modifier: Modifier = Modifier) {
    NavigationRail(
        modifier = modifier.statusBarsPadding().fillMaxHeight(),
        // The bottom bar's container, so the two dresses of the same control are one color.
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Start + WindowInsetsSides.Bottom)
    ) {
        Spacer(Modifier.weight(1f))
        RailItem(ShellTab.TODAY, selected, onSelect, ChiaroIcons.tabToday, R.string.tab_today)
        RailItem(ShellTab.SKY, selected, onSelect, ChiaroIcons.tabSky, R.string.tab_sky)
        RailItem(ShellTab.ALERTS, selected, onSelect, Icons.Outlined.Notifications, R.string.tab_alerts)
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun ColumnScope.RailItem(
    tab: ShellTab,
    selected: ShellTab,
    onSelect: (ShellTab) -> Unit,
    icon: ImageVector,
    @StringRes label: Int
) {
    NavigationRailItem(
        selected = selected == tab,
        onClick = { onSelect(tab) },
        icon = { Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(24.dp)) },
        label = { Text(stringResource(label)) }
    )
}

/**
 * A tab page's column on a wide window (DESIGN §8.12c): Sky and Alerts are one list each, and
 * past [TabColumnMax] their rows would be wider than a line anyone reads in one look, so the
 * list stands centred at that width. Under it, the page as it always was.
 */
@Composable
internal fun TabColumn(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Box(Modifier.widthIn(max = TabColumnMax).fillMaxHeight()) { content() }
    }
}

/** The widest a one-list tab gets: Material's line between a medium window and an expanded one. */
internal val TabColumnMax: Dp = 840.dp
