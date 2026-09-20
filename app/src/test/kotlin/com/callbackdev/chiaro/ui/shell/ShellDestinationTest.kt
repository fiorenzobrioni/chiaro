package com.callbackdev.chiaro.ui.shell

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.callbackdev.chiaro.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The widget-to-screen link (21 set 2026). Three things can break it quietly, and a
 * broken deep link looks exactly like a working one from the home screen: the app
 * opens, just on the wrong tab.
 *
 * 1. **The extra alone carries no weight in the `PendingIntent` cache**, which keys on
 *    `Intent.filterEquals` and ignores extras. Glance stamps a unique `data` URI on an
 *    intent that has none, so the cards are already told apart; the destination rides
 *    the ACTION as well so that it is still legible in an intent whose extras did not
 *    survive, which is what the second and third tests pin.
 * 2. **The flags.** Without `CLEAR_TOP or SINGLE_TOP` a tap on a running app brings
 *    the task forward and delivers nothing, so the tab never moves — the exact case a
 *    reader hits every time after the first.
 * 3. **An intent that names nothing** must stay null, not fall back to a tab: a plain
 *    launch lands where the reader left off.
 */
@RunWith(RobolectricTestRunner::class)
class ShellDestinationTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun intentFor(tab: ShellTab) =
        ShellDestination.intent(context, MainActivity::class.java, tab)

    @Test
    fun `every tab survives the trip through an intent`() {
        ShellTab.entries.forEach { tab ->
            assertEquals(tab, ShellDestination.of(intentFor(tab)))
        }
    }

    @Test
    fun `two destinations are two different intents for the PendingIntent cache`() {
        val sky = intentFor(ShellTab.SKY)
        val today = intentFor(ShellTab.TODAY)
        assertFalse(
            "filterEquals ignores extras: without the action these would share a PendingIntent",
            sky.filterEquals(today)
        )
    }

    @Test
    fun `the action alone is enough to read the destination back`() {
        // What a PendingIntent the system rebuilt from its cache can be left holding.
        val stripped = Intent(intentFor(ShellTab.SKY)).replaceExtras(null as android.os.Bundle?)
        assertEquals(ShellTab.SKY, ShellDestination.of(stripped))
    }

    @Test
    fun `the intent reaches a running activity instead of rebuilding it`() {
        val flags = intentFor(ShellTab.SKY).flags
        assertTrue("CLEAR_TOP", flags and Intent.FLAG_ACTIVITY_CLEAR_TOP != 0)
        assertTrue("SINGLE_TOP", flags and Intent.FLAG_ACTIVITY_SINGLE_TOP != 0)
        assertTrue("NEW_TASK", flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }

    @Test
    fun `an intent that names no tab asks for none`() {
        assertNull(ShellDestination.of(null))
        assertNull(ShellDestination.of(Intent(context, MainActivity::class.java)))
        assertNull(ShellDestination.of(Intent(Intent.ACTION_MAIN)))
    }

    @Test
    fun `an unknown destination is read as none rather than guessed`() {
        val unknown = Intent(context, MainActivity::class.java)
            .setAction("com.callbackdev.chiaro.action.OPEN_ATLANTIS")
        assertNull(ShellDestination.of(unknown))
    }

    /** The widget really does hand this to the system: a `PendingIntent` built from it
     * must be immutable-safe and land on our own activity, not on a component someone
     * else could name. */
    @Test
    fun `the destination intent is explicit`() {
        val intent = intentFor(ShellTab.SKY)
        assertEquals(MainActivity::class.java.name, intent.component?.className)
        assertEquals(context.packageName, intent.component?.packageName)
        // Building it is the other half of the contract: a PendingIntent over an
        // implicit intent is refused outright on this minSdk.
        PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
