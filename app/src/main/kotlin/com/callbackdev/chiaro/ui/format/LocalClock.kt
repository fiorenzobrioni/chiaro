package com.callbackdev.chiaro.ui.format

import androidx.compose.runtime.staticCompositionLocalOf
import java.time.Clock

/**
 * The clock a composable reads "now" from, for the lines that compute an age, a
 * countdown or "today" at draw time rather than taking it from their state: the
 * freshness chip, the next moment's «in 20 min» and the calendar's «in 13 days» on Sky,
 * the "now" disc of Alerts' day strip and its bulletin's day, the Journal's «today» and
 * «yesterday», the guide's sample days. A composable that needs the time reads it here,
 * never from `Instant.now()`.
 *
 * The system's, always, on a phone. It is a local so that a test can stop it (25 set
 * 2026): the README's screenshots are drawn from a recorded response, and with the
 * phone's clock the chip under a report fetched at noon printed its real age on the
 * day the pictures were regenerated. The zone is the device's, as `LocalDate.now()`
 * used, because a date read from this clock is the reader's date.
 */
val LocalClock = staticCompositionLocalOf<Clock> { Clock.systemDefaultZone() }
