package com.callbackdev.chiaro.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.notifications.NotificationPermission

/**
 * The card a screen draws when it is promising something the phone cannot deliver
 * (DESIGN §8.14): every alert is switched on, every bell is set, and notifications
 * are off at the system level, so none of it can arrive.
 *
 * It exists because four ready-made alerts ship switched **on** and the permission
 * used to be asked only by the act of switching one on — which a fresh install never
 * does, so the promise was never tested and never kept. The card is the standing
 * repair: unlike the system dialog it can be offered again every time, it says what
 * is wrong in words before it offers the fix, and it goes away by itself the moment
 * the permission is granted.
 *
 * It is drawn **only** when something is really on. With every switch off there is no
 * promise to break, and a card scolding a reader about a permission they need for
 * nothing would be the screen inventing a problem (CLAUDE.md: a section with no data
 * is not drawn).
 */
@Composable
fun NotificationsOffCard(
    body: String,
    onAllow: () -> Unit,
    settingsOnly: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp)) {
            Text(
                text = stringResource(R.string.notifications_off_title),
                style = MaterialTheme.typography.titleSmall
            )
            Text(text = body, style = MaterialTheme.typography.bodySmall)
        }
        TextButton(
            onClick = onAllow,
            modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
        ) {
            // The button says which door it opens. "Allow" where the system will ask,
            // "Open settings" where it will not: a button that promises a dialog and
            // silently hands over a settings screen is the same broken promise one
            // layer down.
            Text(
                stringResource(
                    if (settingsOnly) {
                        R.string.notifications_off_open_settings
                    } else {
                        R.string.notifications_off_allow
                    }
                )
            )
        }
    }
}

/**
 * Whether a notification posted right now would arrive, re-read every time the screen
 * comes back to the front.
 *
 * The re-read is the whole point: the repair often happens OUTSIDE the app — in the
 * system settings this card sends people to — and a value captured once would leave
 * the card sitting there after the permission was granted, which is the same lie in
 * the other direction.
 */
@Composable
fun rememberNotificationsAllowed(): State<Boolean> {
    val context = LocalContext.current
    val state = remember { mutableStateOf(NotificationPermission.allowed(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        state.value = NotificationPermission.allowed(context)
    }
    return state
}

/** What a screen needs to ask for notifications: the one action, and whether it will
 * be the system dialog or the settings screen. */
class NotificationRequest(val settingsOnly: Boolean, val ask: () -> Unit)

/**
 * The one road to "let the notifications through", with both dead ends closed.
 *
 * The runtime dialog is shown at most twice per install and after that `launch()`
 * returns refused without drawing anything; and where the permission is already held
 * but notifications were switched off in Settings, `launch()` returns granted without
 * drawing anything either. Both are a button that does nothing, which is exactly the
 * state this whole card exists to end. So: where only Settings can help, go straight
 * there; otherwise ask the system, and if the answer comes back refused with no
 * dialog left to show ([NotificationPermission.deniedForGood]), hand over the settings
 * screen in the same tap rather than leaving the reader with a button that shrugs.
 *
 * One refusal is taken as a refusal — the system still has a dialog left, so nothing
 * is forced — and [onResult] lets the caller record that the question was put.
 */
@Composable
fun rememberNotificationRequest(onResult: (Boolean) -> Unit = {}): NotificationRequest {
    val context = LocalContext.current
    val activity = remember(context) { NotificationPermission.activityOf(context) }
    var settingsOnly by remember { mutableStateOf(NotificationPermission.onlySettingsCanHelp(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        settingsOnly = NotificationPermission.onlySettingsCanHelp(context)
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (NotificationPermission.deniedForGood(activity, granted)) {
            NotificationPermission.openSettings(context)
        }
        settingsOnly = NotificationPermission.onlySettingsCanHelp(context)
        onResult(granted)
    }
    val onlySettings = settingsOnly
    return remember(onlySettings, launcher, context) {
        NotificationRequest(settingsOnly = onlySettings) {
            if (onlySettings) {
                NotificationPermission.openSettings(context)
                onResult(false)
            } else {
                launcher.launch(NotificationPermission.PERMISSION)
            }
        }
    }
}
