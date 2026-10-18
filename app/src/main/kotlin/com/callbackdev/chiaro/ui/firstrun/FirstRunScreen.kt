package com.callbackdev.chiaro.ui.firstrun

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.notifications.NotificationPermission
import com.callbackdev.chiaro.ui.components.rememberNotificationRequest
import com.callbackdev.chiaro.ui.places.GpsError
import com.callbackdev.chiaro.ui.places.GpsState
import com.callbackdev.chiaro.ui.places.PlacesSheet
import com.callbackdev.chiaro.ui.places.PlacesViewModel

/**
 * First run, step one (VISION §5.8): one screen, two answers — use my location, or
 * search for a place — and skipping is allowed, landing on the real "no place yet"
 * state. The location permission is asked only AFTER the sentence on this screen has
 * explained why; notifications are not mentioned here at all, because a notification
 * is a promise about a place and there is no place yet. They are step two
 * ([FirstRunNotificationsRoute]), which skipping skips along with this one.
 *
 * Answering happens in the stores, not here: choosing a city or acquiring a fix marks
 * the place question answered, and the shell moves on over the same flow every other
 * screen reads. This screen never navigates; it just stops being the answer.
 *
 * A refusal the system will not ask again (18 ott 2026) turns the button into the road
 * to the app's page in Settings: tapped again, the request would come back refused with
 * no dialog, and the button would do nothing at all.
 */
@Composable
fun FirstRunRoute(
    onSkip: () -> Unit,
    placesViewModel: PlacesViewModel = viewModel(factory = PlacesViewModel.Factory)
) {
    val context = LocalContext.current
    val gpsState by placesViewModel.gpsState.collectAsStateWithLifecycle()
    var searchOpen by remember { mutableStateOf(false) }
    var settingsOnly by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val activity = NotificationPermission.activityOf(context)
        settingsOnly = !granted && activity != null &&
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, LOCATION)
        // Granted or not, the flow is one: the provider checks the permission first
        // and answers with the honest error, which is the message the screen shows.
        placesViewModel.enableGps()
    }
    // Back from Settings with the permission given there: the fix is the next step.
    LifecycleResumeEffect(settingsOnly) {
        if (settingsOnly && ContextCompat.checkSelfPermission(context, LOCATION) == PackageManager.PERMISSION_GRANTED) {
            settingsOnly = false
            placesViewModel.enableGps()
        }
        onPauseOrDispose { }
    }

    FirstRunPage(
        step = 1,
        content = {
            Text(
                text = stringResource(R.string.first_run_title),
                style = MaterialTheme.typography.displaySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.stepHeading(1)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.first_run_tagline),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        },
        actions = {
            PrimaryAction(
                onClick = {
                    if (settingsOnly) {
                        openAppDetails(context)
                    } else {
                        permissionLauncher.launch(LOCATION)
                    }
                },
                enabled = gpsState != GpsState.Acquiring
            ) {
                if (gpsState == GpsState.Acquiring) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        stringResource(
                            if (settingsOnly) R.string.first_run_open_settings else R.string.first_run_use_location
                        )
                    )
                }
            }
            ActionNote(stringResource(R.string.first_run_location_why))
            (gpsState as? GpsState.Error)?.let { error ->
                // A refusal is the reader's answer, not a failure: it is said in the
                // page's quiet ink. A switched-off location or a fix that never came
                // is a fault, and keeps the error's.
                ActionNote(
                    text = stringResource(gpsErrorText(error.kind)),
                    color = if (error.kind == GpsError.PERMISSION) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            SecondaryAction(onClick = { searchOpen = true }) {
                Text(stringResource(R.string.first_run_search))
            }
            QuietAction(onClick = onSkip) {
                Text(stringResource(R.string.first_run_skip))
            }
        }
    )

    if (searchOpen) {
        PlacesSheet(viewModel = placesViewModel, onDismiss = { searchOpen = false })
    }
}

/**
 * First run, step two (21 set 2026): the notification question, put in words before
 * the system puts it in a dialog.
 *
 * It is a step of its own and not a line on the screen before it, because until there
 * is a place there is nothing for an alert to be about. And it exists at all because
 * the screen before it left a promise nobody could keep: four ready-made alerts ship
 * switched **on**, and the permission was only ever asked by the act of switching one
 * on — which a fresh install never does. The switches said yes, the phone said
 * nothing, and the only road to the dialog was to turn an alert off and on again.
 *
 * The system dialog is shown at most twice per install, so it is spent on purpose and
 * never on arrival: this screen explains first and asks only when the reader taps
 * «Consenti». «Non ora» is a real answer and costs nothing — it does not touch the
 * dialog, and Avvisi carries the same offer for as long as it is true (DESIGN §8.14).
 */
@Composable
fun FirstRunNotificationsRoute(onDone: () -> Unit) {
    val context = LocalContext.current
    val request = rememberNotificationRequest { onDone() }

    // Already allowed — a reinstall on a device that kept the grant, or a launcher
    // that asked for us. There is nothing to ask, so nothing is asked.
    LaunchedEffect(Unit) {
        if (NotificationPermission.allowed(context)) onDone()
    }

    FirstRunPage(
        step = 2,
        content = {
            Text(
                text = stringResource(R.string.first_run_notify_title),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.stepHeading(2)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.first_run_notify_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        },
        actions = {
            PrimaryAction(onClick = request.ask) {
                Text(
                    stringResource(
                        if (request.settingsOnly) R.string.notifications_off_open_settings else R.string.notifications_off_allow
                    )
                )
            }
            ActionNote(stringResource(R.string.first_run_notify_where))
            Spacer(modifier = Modifier.height(4.dp))
            QuietAction(onClick = onDone) {
                Text(stringResource(R.string.first_run_notify_later))
            }
        }
    )
}

// ------------------------------------------------------------------------- the frame

private const val LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION

/** How many steps the first run has: the place, then the notifications. */
private const val STEPS = 2

/**
 * The frame both steps share (18 ott 2026): what the page says, centered in the room above
 * the buttons, and the buttons at the bottom, in the same place on every step, where a
 * thumb reaches them. No picture and no progress bar: the page is its words, and the run
 * is two steps long; a screen reader hears the step on the heading instead. On an open
 * foldable the column keeps a phone's width.
 */
@Composable
private fun FirstRunPage(
    step: Int,
    content: @Composable ColumnScope.() -> Unit,
    actions: @Composable ColumnScope.() -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().safeDrawingPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
                val room = maxHeight
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(
                        modifier = Modifier
                            .heightIn(min = room)
                            .widthIn(max = PageMaxWidth)
                            .fillMaxWidth()
                            .padding(horizontal = PageMargin, vertical = 24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        content = content
                    )
                }
            }
            Column(
                modifier = Modifier
                    .widthIn(max = PageMaxWidth)
                    .fillMaxWidth()
                    .padding(start = PageMargin, end = PageMargin, top = 8.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                content = actions
            )
        }
    }
}

private val PageMargin = 24.dp
private val PageMaxWidth = 640.dp
private val ActionHeight = 52.dp

/** The heading of a step, which a screen reader announces with where the run stands. */
@Composable
private fun Modifier.stepHeading(step: Int): Modifier {
    val where = stringResource(R.string.first_run_step, step, STEPS)
    return semantics {
        heading()
        stateDescription = where
    }
}

@Composable
private fun PrimaryAction(onClick: () -> Unit, enabled: Boolean = true, label: @Composable () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().heightIn(min = ActionHeight)
    ) { label() }
}

@Composable
private fun SecondaryAction(onClick: () -> Unit, label: @Composable () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = ActionHeight)
    ) { label() }
}

@Composable
private fun QuietAction(onClick: () -> Unit, label: @Composable () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.heightIn(min = 48.dp)) { label() }
}

/** A line under a button: what it does with what it asks for, or what came of it. */
@Composable
private fun ActionNote(text: String, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = color,
        textAlign = TextAlign.Center
    )
}

private fun openAppDetails(context: android.content.Context) {
    val page = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
    runCatching { context.startActivity(page) }
}

fun gpsErrorText(kind: GpsError): Int = when (kind) {
    GpsError.PERMISSION -> R.string.gps_error_permission
    GpsError.DISABLED -> R.string.gps_error_disabled
    GpsError.TIMEOUT -> R.string.gps_error_timeout
    GpsError.UNAVAILABLE -> R.string.gps_error_unavailable
}
