package com.callbackdev.chiaro.ui.places

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.data.ActiveSource
import com.callbackdev.chiaro.data.LocationSettings
import com.callbackdev.chiaro.domain.model.City
import com.callbackdev.chiaro.ui.firstrun.gpsErrorText
import com.callbackdev.chiaro.ui.format.Formats
import com.callbackdev.chiaro.ui.theme.tabular
import java.util.Locale
import kotlinx.coroutines.launch

/**
 * Places (VISION §5.6): the device position pinned on top with its own treatment,
 * search-as-you-type with the recent searches, the saved list with a cached
 * temperature beside each place, long-press drag to reorder (with accessibility
 * actions doing the same job for TalkBack), and swipe-to-remove with undo.
 *
 * Deliberately no FAB: the sheet's first interactive element already IS "add a
 * place" — a button floating over the affordance it duplicates would be decoration
 * (deviation from §5.6's aside, recorded in PLANNING.md).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlacesSheet(
    viewModel: PlacesViewModel,
    onDismiss: () -> Unit
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val search by viewModel.search.collectAsStateWithLifecycle()
    val places by viewModel.places.collectAsStateWithLifecycle()
    val active by viewModel.active.collectAsStateWithLifecycle()
    val location by viewModel.location.collectAsStateWithLifecycle()
    val gpsState by viewModel.gpsState.collectAsStateWithLifecycle()
    val recents by viewModel.recentSearches.collectAsStateWithLifecycle()

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val removedMessage = stringResource(R.string.places_removed)
    val undoLabel = stringResource(R.string.action_undo)

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { viewModel.enableGps() }

    LaunchedEffect(Unit) {
        // A failure from the last time the sheet was open has been read by now: this
        // ViewModel outlives the sheet, so without this it would greet the next one.
        viewModel.dismissGpsError()
        viewModel.gpsSelected.collect { onDismiss() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Box(modifier = Modifier.fillMaxHeight(0.94f)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.places_title),
                    style = MaterialTheme.typography.titleMedium
                )

                GpsRow(
                    location = location,
                    gpsState = gpsState,
                    isActive = active is ActiveSource.Gps,
                    onEnable = {
                        permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                    },
                    onDisable = viewModel::disableGps,
                    // No dismissal here: the row goes to "looking for you" and the
                    // sheet closes when the position answers (or stays, with the
                    // reason, when it does not). The selection itself is instant —
                    // Today has already switched behind the sheet.
                    onSelect = viewModel::selectGps
                )
                (gpsState as? GpsState.Error)?.let { error ->
                    Text(
                        text = stringResource(gpsErrorText(error.kind)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                OutlinedTextField(
                    value = query,
                    onValueChange = viewModel::setQuery,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.places_search_hint)) },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    singleLine = true
                )

                when (val s = search) {
                    SearchState.Idle -> SavedAndRecents(
                        places = places,
                        recents = recents,
                        active = active,
                        onRecent = viewModel::setQuery,
                        onSelect = { viewModel.select(it); onDismiss() },
                        onMove = viewModel::move,
                        onRemove = { memo ->
                            viewModel.remove(memo)
                            scope.launch {
                                val result = snackbar.showSnackbar(
                                    message = removedMessage.format(memo.city.name),
                                    actionLabel = undoLabel
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    viewModel.undoRemove(memo)
                                }
                            }
                        }
                    )
                    SearchState.Searching -> Unit // the field is the busy signal;
                    // nothing false is drawn while the answer is on its way
                    is SearchState.Results -> LazyColumn {
                        items(s.cities.size, key = { s.cities[it].id }) { i ->
                            val city = s.cities[i]
                            ResultRow(city) {
                                viewModel.choose(city)
                                onDismiss()
                            }
                        }
                    }
                    is SearchState.NoResults -> SheetNote(
                        stringResource(R.string.places_no_results, s.query)
                    )
                    SearchState.Offline -> SheetNote(stringResource(R.string.places_offline))
                    SearchState.Failed -> SheetNote(stringResource(R.string.places_failed))
                }
            }
            SnackbarHost(
                hostState = snackbar,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

// -----------------------------------------------------------------------------------
// The pinned device-position row
// -----------------------------------------------------------------------------------

@Composable
private fun GpsRow(
    location: LocationSettings?,
    gpsState: GpsState,
    isActive: Boolean,
    onEnable: () -> Unit,
    onDisable: () -> Unit,
    onSelect: () -> Unit
) {
    val enabled = location?.useGps == true
    Column {
        ListItem(
            leadingContent = {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.mc_compass),
                    contentDescription = null,
                    tint = androidx.compose.ui.graphics.Color.Unspecified
                )
            },
            headlineContent = { Text(stringResource(R.string.places_gps_title)) },
            supportingContent = {
                Text(
                    when {
                        gpsState == GpsState.Acquiring ->
                            stringResource(R.string.places_gps_acquiring)
                        enabled && location?.gpsCity != null ->
                            listOfNotNull(
                                location.gpsCity?.name,
                                if (isActive) stringResource(R.string.places_active) else null
                            ).joinToString(" · ")
                        else -> stringResource(R.string.places_gps_subtitle_off)
                    }
                )
            },
            trailingContent = {
                Switch(
                    checked = enabled,
                    onCheckedChange = { wanted -> if (wanted) onEnable() else onDisable() }
                )
            },
            colors = ListItemDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            modifier = Modifier
                .clickable(enabled = enabled, onClick = onSelect)
        )
        if (gpsState == GpsState.Acquiring) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
    }
}

// -----------------------------------------------------------------------------------
// Saved places: recents, reorder, remove
// -----------------------------------------------------------------------------------

@Composable
private fun SavedAndRecents(
    places: List<SavedPlace>,
    recents: List<String>,
    active: ActiveSource?,
    onRecent: (String) -> Unit,
    onSelect: (City) -> Unit,
    onMove: (City, Int) -> Unit,
    onRemove: (RemovedPlace) -> Unit
) {
    // The order the reader sees while dragging. Store emissions are adopted only at
    // rest: mid-drag they would yank the row out from under the finger.
    var order by remember { mutableStateOf(places) }
    val listState = rememberLazyListState()
    val drag = remember(listState) { DragState(listState) }
    LaunchedEffect(places) {
        if (drag.draggingId == null) order = places
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset -> drag.start(offset) },
                    onDrag = { change, delta ->
                        change.consume()
                        drag.drag(delta.y, order) { from, to ->
                            order = order.toMutableList().apply { add(to, removeAt(from)) }
                        }
                    },
                    onDragEnd = {
                        drag.stop()?.let { (id, _) ->
                            val index = order.indexOfFirst { it.city.id == id }
                            if (index >= 0) onMove(order[index].city, index)
                        }
                    },
                    onDragCancel = { drag.stop() }
                )
            }
    ) {
        if (recents.isNotEmpty()) {
            item(key = "recents-header") { SectionLabel(stringResource(R.string.places_recent)) }
            recents.forEach { term ->
                item(key = "recent:$term") {
                    ListItem(
                        leadingContent = {
                            Icon(Icons.Outlined.Search, contentDescription = null)
                        },
                        headlineContent = { Text(term) },
                        modifier = Modifier.clickable { onRecent(term) }
                    )
                }
            }
        }
        if (order.isNotEmpty()) {
            item(key = "saved-header") { SectionLabel(stringResource(R.string.places_saved)) }
        }
        items(order.size, key = { order[it].city.id }) { index ->
            val place = order[index]
            val isActive = (active as? ActiveSource.Saved)?.city?.id == place.city.id
            val dragging = drag.draggingId == place.city.id
            SavedRow(
                place = place,
                isActive = isActive,
                index = index,
                lastIndex = order.lastIndex,
                onSelect = { onSelect(place.city) },
                onMove = { to -> onMove(place.city, to) },
                onRemove = {
                    onRemove(RemovedPlace(place.city, index, wasActive = isActive))
                },
                modifier = Modifier
                    .zIndex(if (dragging) 1f else 0f)
                    .graphicsLayer {
                        translationY = if (dragging) drag.offset else 0f
                    }
            )
        }
    }
}

@Composable
private fun SavedRow(
    place: SavedPlace,
    isActive: Boolean,
    index: Int,
    lastIndex: Int,
    onSelect: () -> Unit,
    onMove: (Int) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val moveUp = stringResource(R.string.places_move_up)
    val moveDown = stringResource(R.string.places_move_down)
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value != SwipeToDismissBoxValue.Settled) onRemove()
            true
        }
    )
    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = null, // the swipe already announced itself
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }
    ) {
        ListItem(
            headlineContent = { Text(place.city.name) },
            supportingContent = {
                val where = listOfNotNull(place.city.region, place.city.country)
                    .joinToString(", ")
                Text(
                    if (isActive) {
                        "$where · ${stringResource(R.string.places_active)}"
                    } else {
                        where
                    }
                )
            },
            trailingContent = {
                place.temperatureC?.let {
                    Text(
                        text = Formats.temperature(
                            it,
                            com.callbackdev.chiaro.domain.settings.TemperatureUnit.CELSIUS,
                            Locale.getDefault()
                        ),
                        style = MaterialTheme.typography.titleMedium.tabular()
                    )
                }
                // no cached report yet: no number, never a placeholder (§1.1)
            },
            modifier = Modifier
                .clickable(onClick = onSelect)
                .semantics {
                    customActions = buildList {
                        if (index > 0) {
                            add(CustomAccessibilityAction(moveUp) { onMove(index - 1); true })
                        }
                        if (index < lastIndex) {
                            add(CustomAccessibilityAction(moveDown) { onMove(index + 1); true })
                        }
                    }
                }
        )
    }
}

@Composable
private fun ResultRow(city: City, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(city.name) },
        supportingContent = {
            Text(listOfNotNull(city.region, city.country).joinToString(", "))
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
    )
}

@Composable
private fun SheetNote(text: String) {
    Row(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// -----------------------------------------------------------------------------------
// Long-press drag, on the saved rows only (they are the LazyColumn's Long keys)
// -----------------------------------------------------------------------------------

/**
 * The minimum drag machinery the sheet needs, keyed on city ids so the recents and
 * headers around the saved rows never take part. The gesture begins after a long
 * press, which is also what keeps it out of the way of the sheet's own swipe and the
 * rows' swipe-to-remove.
 */
private class DragState(private val listState: LazyListState) {

    var draggingId by mutableStateOf<Long?>(null)
        private set
    var offset by mutableFloatStateOf(0f)
        private set

    private fun savedItems() =
        listState.layoutInfo.visibleItemsInfo.filter { it.key is Long }

    fun start(at: Offset) {
        draggingId = savedItems()
            .firstOrNull { at.y.toInt() in it.offset..(it.offset + it.size) }
            ?.key as? Long
        offset = 0f
    }

    /** Accumulates the finger's travel; when the dragged row's centre crosses a
     * neighbour, [swap] reorders the backing list and the offset is rebased so the
     * row stays under the finger. */
    fun drag(delta: Float, order: List<SavedPlace>, swap: (Int, Int) -> Unit) {
        val id = draggingId ?: return
        offset += delta
        val items = savedItems()
        val current = items.firstOrNull { it.key == id } ?: return
        val centre = current.offset + offset + current.size / 2f
        val target = items.firstOrNull {
            it.key != id && centre >= it.offset && centre < it.offset + it.size
        } ?: return
        val from = order.indexOfFirst { it.city.id == id }
        val to = order.indexOfFirst { it.city.id == target.key }
        if (from < 0 || to < 0) return
        swap(from, to)
        offset -= target.offset - current.offset
    }

    /** Ends the drag; returns what was being dragged, for the caller to persist. */
    fun stop(): Pair<Long, Float>? {
        val id = draggingId ?: return null
        val result = id to offset
        draggingId = null
        offset = 0f
        return result
    }
}
