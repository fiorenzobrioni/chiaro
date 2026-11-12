package com.callbackdev.chiaro.ui.firstrun

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.data.FirstRun
import com.callbackdev.chiaro.notifications.NotificationPermission
import com.callbackdev.chiaro.ui.components.rememberNotificationRequest
import com.callbackdev.chiaro.ui.places.GpsError
import com.callbackdev.chiaro.ui.places.GpsState
import com.callbackdev.chiaro.ui.places.PlacesSheet
import com.callbackdev.chiaro.ui.places.PlacesViewModel
import com.callbackdev.chiaro.ui.theme.ChiaroMotion
import com.callbackdev.chiaro.ui.theme.balanced
import com.callbackdev.chiaro.ui.theme.reducedMotion

/**
 * The first run's pages (VISION §5.8), three since 12 nov 2026: what Chiaro is, the place
 * the forecast is about, and the notifications the ready-made alerts need.
 *
 * The welcome is a page of its own because the run used to open on the question: the name,
 * its sentence and «Usa la mia posizione», with nothing between the icon and a permission
 * that said what the app does. It asks nothing, so it is not stored: a run left on the place
 * and reopened after the process died starts from it again, and costs one tap.
 */
enum class FirstRunPage { WELCOME, PLACE, NOTIFICATIONS }

/** Where the place's question stands, as the place's page draws it. */
data class PlaceAnswer(
    /** A fix is being looked for: the button waits, and says so to a screen reader. */
    val acquiring: Boolean = false,
    /** Refused for good: the button opens the app's page in Settings instead of asking. */
    val settingsOnly: Boolean = false,
    /** What came of the last try, said under the page's words. */
    val error: GpsError? = null
)

/** What the pages can ask for, as functions, so [FirstRunScreen] is a plain composable a test can draw. */
class FirstRunActions(
    val start: () -> Unit = {},
    val back: () -> Unit = {},
    val useLocation: () -> Unit = {},
    val search: () -> Unit = {},
    val skipPlace: () -> Unit = {},
    val allowNotifications: () -> Unit = {},
    val notificationsLater: () -> Unit = {}
)

/**
 * The first run, while the store says it owes an answer ([FirstRun.Pending] or
 * [FirstRun.Notifications]).
 *
 * The place (VISION §5.8): two answers, use my location or search for a place, and skipping
 * is allowed, landing on the real "no place yet" state. The location permission is asked only
 * AFTER the page has said why. Answering happens in the stores, not here: choosing a city or
 * acquiring a fix marks the place question answered, and the store's next value is the
 * notifications' page. A refusal the system will not ask again (18 ott 2026) turns the button
 * into the road to the app's page in Settings: tapped again, the request would come back
 * refused with no dialog, and the button would do nothing at all.
 *
 * The notifications (21 set 2026): a step of its own and not a line on the place's page,
 * because until there is a place there is nothing for an alert to be about. It exists because
 * five switches ship **on** (the four ready-made alerts and the official warnings) and the
 * permission was only ever asked by the act of switching one on, which a fresh install never does. The system dialog is shown at most twice
 * per install, so it is spent on purpose: the page explains first and asks only on «Consenti».
 * «Più tardi» costs nothing, and Avvisi carries the same offer for as long as it is true
 * (DESIGN §8.14). Skipping the place skips this too ([onSkip] answers both).
 */
@Composable
fun FirstRunRoute(
    stage: FirstRun,
    onSkip: () -> Unit,
    onNotificationsDone: () -> Unit,
    placesViewModel: PlacesViewModel = viewModel(factory = PlacesViewModel.Factory)
) {
    val context = LocalContext.current
    val gpsState by placesViewModel.gpsState.collectAsStateWithLifecycle()
    var welcomed by rememberSaveable { mutableStateOf(false) }
    var searchOpen by remember { mutableStateOf(false) }
    var settingsOnly by rememberSaveable { mutableStateOf(false) }
    val page = when {
        stage == FirstRun.Notifications -> FirstRunPage.NOTIFICATIONS
        welcomed -> FirstRunPage.PLACE
        else -> FirstRunPage.WELCOME
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val activity = NotificationPermission.activityOf(context)
        settingsOnly = !granted && activity != null &&
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, LOCATION)
        // Granted or not, the flow is one: the provider checks the permission first
        // and answers with the honest error, which is the message the page shows.
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

    // Allowed, refused or left for later, the question has been put.
    val notifications = rememberNotificationRequest { onNotificationsDone() }
    // Already allowed when the page comes up (a reinstall on a device that kept the grant,
    // or a launcher that asked for us): there is nothing to ask, so nothing is asked.
    LaunchedEffect(page) {
        if (page == FirstRunPage.NOTIFICATIONS && NotificationPermission.allowed(context)) onNotificationsDone()
    }

    FirstRunScreen(
        page = page,
        place = PlaceAnswer(
            acquiring = gpsState == GpsState.Acquiring,
            settingsOnly = settingsOnly,
            error = (gpsState as? GpsState.Error)?.kind
        ),
        notificationsSettingsOnly = notifications.settingsOnly,
        actions = FirstRunActions(
            start = { welcomed = true },
            back = { welcomed = false },
            useLocation = { if (settingsOnly) openAppDetails(context) else permissionLauncher.launch(LOCATION) },
            search = { searchOpen = true },
            skipPlace = onSkip,
            allowNotifications = notifications.ask,
            notificationsLater = onNotificationsDone
        )
    )

    if (searchOpen) {
        PlacesSheet(viewModel = placesViewModel, onDismiss = { searchOpen = false })
    }
}

/**
 * The frame every page shares (18 ott 2026; the welcome since 12 nov 2026): the page's words
 * centred in the room above the buttons, and the buttons at the bottom, where a thumb reaches
 * them. Only the words move: a page slides a sixth of the width and fades, as the app's pages
 * do, the other way when it goes back, and a fade alone under reduced motion; the buttons
 * change in place. No picture and no progress bar: the run is three pages, and a screen reader
 * hears where it stands on each heading. The way back is the system's, from the place to the
 * welcome; the notifications have none, because the place they follow is already answered and
 * its road back is the places sheet. On an open foldable the column keeps a phone's width.
 */
@Composable
fun FirstRunScreen(
    page: FirstRunPage,
    actions: FirstRunActions,
    modifier: Modifier = Modifier,
    place: PlaceAnswer = PlaceAnswer(),
    notificationsSettingsOnly: Boolean = false
) {
    val reduced = reducedMotion()
    BackHandler(enabled = page == FirstRunPage.PLACE, onBack = actions.back)
    Surface(modifier = modifier.fillMaxSize().testTag(FirstRunTags.ROOT)) {
        Column(
            modifier = Modifier.fillMaxSize().safeDrawingPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedContent(
                targetState = page,
                transitionSpec = { pageTransition(forward = targetState.ordinal > initialState.ordinal, reduced) },
                label = "firstRun",
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) { shown ->
                PageColumn {
                    when (shown) {
                        FirstRunPage.WELCOME -> WelcomePage()
                        FirstRunPage.PLACE -> PlacePage(place)
                        FirstRunPage.NOTIFICATIONS -> NotificationsPage()
                    }
                }
            }
            BottomBar(page, place, notificationsSettingsOnly, actions)
        }
    }
}

private fun pageTransition(forward: Boolean, reduced: Boolean): ContentTransform {
    if (reduced) return fadeIn(ChiaroMotion.fade()) togetherWith fadeOut(ChiaroMotion.fade())
    val slide = tween<IntOffset>(PageMillis, easing = FastOutSlowInEasing)
    val shift = if (forward) SlideDivisor else -SlideDivisor
    return (fadeIn(tween(PageMillis)) + slideInHorizontally(slide) { it / shift }) togetherWith
        (fadeOut(tween(PageMillis)) + slideOutHorizontally(slide) { -it / shift })
}

/** The page's words, centred in the room above the buttons; a page taller than the room scrolls from its top. */
@Composable
private fun PageColumn(content: @Composable ColumnScope.() -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
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
                    .padding(horizontal = PageMargin, vertical = 24.dp)
                    .testTag(FirstRunTags.PAGE),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                content = content
            )
        }
    }
}

/**
 * The buttons under every page: the wide one that moves on, the line under it that says what
 * it does with what it asks for, a real alternative outlined, and the quiet way past.
 */
@Composable
private fun BottomBar(
    page: FirstRunPage,
    place: PlaceAnswer,
    notificationsSettingsOnly: Boolean,
    actions: FirstRunActions
) {
    Column(
        modifier = Modifier
            .widthIn(max = PageMaxWidth)
            .fillMaxWidth()
            .padding(start = PageMargin, end = PageMargin, top = 8.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (page) {
            FirstRunPage.WELCOME -> PrimaryAction(onClick = actions.start) {
                Text(stringResource(R.string.first_run_start))
            }

            FirstRunPage.PLACE -> {
                PrimaryAction(onClick = actions.useLocation, enabled = !place.acquiring) {
                    if (place.acquiring) {
                        val finding = stringResource(R.string.places_gps_acquiring)
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp).semantics { contentDescription = finding },
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            stringResource(
                                if (place.settingsOnly) R.string.first_run_open_settings else R.string.first_run_use_location
                            )
                        )
                    }
                }
                ActionNote(stringResource(R.string.first_run_location_why))
                OutlinedButton(
                    onClick = actions.search,
                    modifier = Modifier.fillMaxWidth().heightIn(min = ActionHeight).testTag(FirstRunTags.SECONDARY)
                ) {
                    Text(stringResource(R.string.first_run_search))
                }
                QuietAction(onClick = actions.skipPlace)
            }

            FirstRunPage.NOTIFICATIONS -> {
                PrimaryAction(onClick = actions.allowNotifications) {
                    Text(
                        stringResource(
                            if (notificationsSettingsOnly) R.string.notifications_off_open_settings else R.string.notifications_off_allow
                        )
                    )
                }
                ActionNote(stringResource(R.string.first_run_notify_where))
                QuietAction(onClick = actions.notificationsLater)
            }
        }
    }
}

/** The name and its sentence, then three promises in words only: no picture (the reader has just touched the icon) and no badges. */
@Composable
private fun WelcomePage() {
    Text(
        text = stringResource(R.string.first_run_title),
        style = MaterialTheme.typography.displaySmall.balanced(),
        textAlign = TextAlign.Center,
        modifier = Modifier.stepHeading(FirstRunPage.WELCOME)
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = stringResource(R.string.first_run_tagline),
        style = MaterialTheme.typography.titleMedium.balanced(),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(36.dp))
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Promise(R.string.first_run_reading_title, R.string.first_run_reading_body)
        Promise(R.string.first_run_sky_title, R.string.first_run_sky_body)
        Promise(R.string.first_run_free_title, R.string.first_run_free_body)
    }
}

@Composable
private fun Promise(@StringRes title: Int, @StringRes body: Int) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.titleMedium.balanced(),
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(body),
            style = MaterialTheme.typography.bodyMedium.balanced(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PlacePage(place: PlaceAnswer) {
    PageTitle(R.string.first_run_place_title, FirstRunPage.PLACE)
    PageBody(R.string.first_run_place_body)
    place.error?.let { error ->
        // A refusal is the reader's answer, not a failure: it is said in the page's quiet
        // ink, with the way on. A switched-off location or a fix that never came is a
        // fault, and keeps the error's.
        val refused = error == GpsError.PERMISSION
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(if (refused) R.string.first_run_location_refused else gpsErrorText(error)),
            style = MaterialTheme.typography.bodyMedium.balanced(),
            color = if (refused) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag(FirstRunTags.LOCATION_ERROR)
        )
    }
}

@Composable
private fun NotificationsPage() {
    PageTitle(R.string.first_run_notify_title, FirstRunPage.NOTIFICATIONS)
    PageBody(R.string.first_run_notify_body)
}

@Composable
private fun PageTitle(@StringRes text: Int, page: FirstRunPage) {
    Text(
        text = stringResource(text),
        style = MaterialTheme.typography.headlineMedium.balanced(),
        textAlign = TextAlign.Center,
        modifier = Modifier.stepHeading(page)
    )
    Spacer(modifier = Modifier.height(12.dp))
}

@Composable
private fun PageBody(@StringRes text: Int) {
    Text(
        text = stringResource(text),
        style = MaterialTheme.typography.bodyLarge.balanced(),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
}

// ------------------------------------------------------------------------- the frame

private const val LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION

private val PageMargin = 24.dp
private val PageMaxWidth = 640.dp
private val ActionHeight = 52.dp

/** The page transition's length, the app's own pages' (`ChiaroRoot`). */
private const val PageMillis = 300

/** How far a page slides, as a fraction (1/N) of its width, the app's own pages' too. */
private const val SlideDivisor = 6

/** The heading of a page, which a screen reader announces with where the run stands. */
@Composable
private fun Modifier.stepHeading(page: FirstRunPage): Modifier {
    val where = stringResource(R.string.first_run_step, page.ordinal + 1, FirstRunPage.entries.size)
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
        modifier = Modifier.fillMaxWidth().heightIn(min = ActionHeight).testTag(FirstRunTags.PRIMARY)
    ) { label() }
}

/** «Più tardi»: the quiet way past a question, which costs nothing. */
@Composable
private fun QuietAction(onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.heightIn(min = 48.dp).testTag(FirstRunTags.LATER)) {
        Text(stringResource(R.string.first_run_later))
    }
}

/** A line under a button: what it does with what it asks for. */
@Composable
private fun ActionNote(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall.balanced(),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
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

/** Hooks for the UI tests. */
object FirstRunTags {
    const val ROOT = "first_run"
    const val PAGE = "first_run_page"
    const val PRIMARY = "first_run_primary"
    const val SECONDARY = "first_run_secondary"
    const val LATER = "first_run_later"
    const val LOCATION_ERROR = "first_run_location_error"
}
