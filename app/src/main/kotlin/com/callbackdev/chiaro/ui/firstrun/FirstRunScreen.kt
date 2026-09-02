package com.callbackdev.chiaro.ui.firstrun

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.ui.places.GpsError
import com.callbackdev.chiaro.ui.places.GpsState
import com.callbackdev.chiaro.ui.places.PlacesSheet
import com.callbackdev.chiaro.ui.places.PlacesViewModel

/**
 * First run (VISION §5.8): one screen, two answers — use my location, or search for
 * a place — and skipping is allowed, landing on the real "no place yet" state. The
 * location permission is asked only AFTER the sentence on this screen has explained
 * why; notifications are not mentioned at all, because nothing here needs them.
 *
 * Answering happens in the stores, not here: choosing a city or acquiring a fix marks
 * first-run done, and the shell switches to Today on the same flow every other screen
 * reads. This screen never navigates; it just stops being the answer.
 */
@Composable
fun FirstRunRoute(
    onSkip: () -> Unit,
    placesViewModel: PlacesViewModel = viewModel(factory = PlacesViewModel.Factory)
) {
    val gpsState by placesViewModel.gpsState.collectAsStateWithLifecycle()
    var searchOpen by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        // Granted or not, the flow is one: the provider checks the permission first
        // and answers with the honest error, which is the message the screen shows.
        placesViewModel.enableGps()
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 48.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displaySmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.first_run_tagline),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = { permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION) },
                enabled = gpsState != GpsState.Acquiring,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (gpsState == GpsState.Acquiring) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(stringResource(R.string.first_run_use_location))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.first_run_location_why),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            (gpsState as? GpsState.Error)?.let { error ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(gpsErrorText(error.kind)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            OutlinedButton(
                onClick = { searchOpen = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.first_run_search))
            }

            Spacer(modifier = Modifier.height(32.dp))
            TextButton(onClick = onSkip) {
                Text(stringResource(R.string.first_run_skip))
            }
        }
    }

    if (searchOpen) {
        PlacesSheet(viewModel = placesViewModel, onDismiss = { searchOpen = false })
    }
}

fun gpsErrorText(kind: GpsError): Int = when (kind) {
    GpsError.PERMISSION -> R.string.gps_error_permission
    GpsError.DISABLED -> R.string.gps_error_disabled
    GpsError.TIMEOUT -> R.string.gps_error_timeout
    GpsError.UNAVAILABLE -> R.string.gps_error_unavailable
}
