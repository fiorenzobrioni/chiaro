package com.callbackdev.chiaro.ui.shell

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.callbackdev.chiaro.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The one door into the app from outside it, and the promise it makes (21 set 2026).
 *
 * The promise is the first test and it is the whole fix: **this intent is the launcher's
 * intent**. Android identifies a task by the intent that created it, so a widget or a
 * notification that opens the app with an intent of its own opens a SECOND task — which
 * is what the reader saw, as two copies of the home screen one behind the other, and as
 * a closing animation the launcher did not own. `filterEquals` is the platform's own
 * comparison for that, which is why it is what is asserted rather than the fields.
 *
 * The first pass put the destination in the intent's ACTION, on the argument that extras
 * are not part of `filterEquals`. That argument was right and the conclusion was
 * backwards: not being part of `filterEquals` is exactly why the extra is the only place
 * the destination can ride without the task stopping to look like the launcher's.
 */
@RunWith(RobolectricTestRunner::class)
class ShellDestinationTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun intentFor(tab: ShellTab? = null) =
        ShellDestination.intent(context, MainActivity::class.java, tab)

    /** What the home screen's own icon sends. */
    private fun launcherIntent() = Intent(Intent.ACTION_MAIN)
        .addCategory(Intent.CATEGORY_LAUNCHER)
        .setComponent(android.content.ComponentName(context, MainActivity::class.java))

    @Test
    fun `every door asks for the same task the launcher icon asks for`() {
        assertTrue("a plain open", launcherIntent().filterEquals(intentFor()))
        ShellTab.entries.forEach { tab ->
            assertTrue(
                "$tab must not make a task of its own",
                launcherIntent().filterEquals(intentFor(tab))
            )
        }
    }

    @Test
    fun `every tab survives the trip through an intent`() {
        ShellTab.entries.forEach { tab ->
            assertEquals(tab, ShellDestination.of(intentFor(tab)))
        }
    }

    @Test
    fun `an intent that names no tab asks for none`() {
        assertNull(ShellDestination.of(null))
        assertNull(ShellDestination.of(intentFor(tab = null)))
        assertNull(ShellDestination.of(Intent(context, MainActivity::class.java)))
    }

    @Test
    fun `an unknown destination is read as none rather than guessed`() {
        val unknown = intentFor(ShellTab.SKY)
            .putExtra("com.callbackdev.chiaro.extra.TAB", "ATLANTIS")
        assertNull(ShellDestination.of(unknown))
    }

    /** A `PendingIntent` starts from outside an activity, and the platform refuses that
     * without `NEW_TASK`. Nothing else is set: `singleTask` in the manifest is what
     * routes the intent to the one live instance, and doing it here by hand as well was
     * the first pass papering over the missing half. */
    @Test
    fun `the only flag is the one the platform requires`() {
        val flags = intentFor(ShellTab.SKY).flags
        assertTrue("NEW_TASK", flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
        assertEquals(Intent.FLAG_ACTIVITY_NEW_TASK, flags)
    }

    @Test
    fun `the door is explicit, and a PendingIntent can be built over it`() {
        val intent = intentFor(ShellTab.SKY)
        assertEquals(MainActivity::class.java.name, intent.component?.className)
        assertEquals(context.packageName, intent.component?.packageName)
        // An implicit intent is refused outright on this minSdk, so building it is
        // half the contract.
        PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
