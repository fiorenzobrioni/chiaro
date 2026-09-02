package com.callbackdev.chiaro.ui.places

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.data.ActiveSource
import com.callbackdev.chiaro.domain.model.City

/**
 * The Fase-3 minimum (VISION §5.6, scoped down): search-as-you-type, tap to add and
 * select, the saved list to switch between. No GPS, no reorder, no swipe-to-remove —
 * those are the full Places surface and they arrive with it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlacesSheet(
    viewModel: PlacesViewModel,
    onDismiss: () -> Unit
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val search by viewModel.search.collectAsStateWithLifecycle()
    val cities by viewModel.cities.collectAsStateWithLifecycle()
    val active by viewModel.active.collectAsStateWithLifecycle()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .heightIn(min = 320.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.places_title),
                style = MaterialTheme.typography.titleMedium
            )
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::setQuery,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.places_search_hint)) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                singleLine = true
            )
            when (val s = search) {
                SearchState.Idle -> SavedList(
                    cities = cities,
                    active = active,
                    onSelect = { viewModel.select(it); onDismiss() }
                )
                SearchState.Searching -> Unit // the field itself is the busy signal;
                // nothing false is drawn while the answer is on its way
                is SearchState.Results -> LazyColumn {
                    items(s.cities, key = { it.id }) { city ->
                        CityRow(city = city, supporting = null) {
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
    }
}

@Composable
private fun SavedList(
    cities: List<City>,
    active: ActiveSource?,
    onSelect: (City) -> Unit
) {
    if (cities.isEmpty()) return // a fresh install: the search field IS the screen
    Column {
        Text(
            text = stringResource(R.string.places_saved),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        LazyColumn {
            items(cities, key = { it.id }) { city ->
                val isActive = (active as? ActiveSource.Saved)?.city?.id == city.id
                CityRow(
                    city = city,
                    supporting = if (isActive) stringResource(R.string.places_active) else null,
                    onClick = { onSelect(city) }
                )
            }
        }
    }
}

@Composable
private fun CityRow(city: City, supporting: String?, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(city.name) },
        supportingContent = {
            val where = listOfNotNull(city.region, city.country).joinToString(", ")
            Text(if (supporting != null) "$where · $supporting" else where)
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
