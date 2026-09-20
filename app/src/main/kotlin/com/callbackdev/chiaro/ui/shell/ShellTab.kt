package com.callbackdev.chiaro.ui.shell

import android.content.Context
import android.content.Intent

/** The four destinations of the bottom bar (VISION §5.1). */
enum class ShellTab { TODAY, SKY, ALERTS, JOURNAL }

/**
 * A tap from outside the app that names the screen it should land on — today only the
 * home widgets (21 set 2026), which open the tab whose information they were showing:
 * «Momenti del cielo» and «L'arco del giorno» are the Sky screen's own material, and
 * landing on Today from them asked the reader to find their way back to what they had
 * just been reading.
 *
 * The destination travels as the intent's **action** as well as an extra. Extras are
 * not part of `Intent.filterEquals`, which is what the `PendingIntent` cache keys on,
 * so an extra on its own is the piece of this that a cache hit could carry over from
 * another card. Glance already guards that by stamping a unique `data` URI on an
 * intent that has none, so the two are belt and braces — but the belt is the one that
 * survives being read back from an intent nobody reassembled, and it costs a string.
 *
 * The flags are the ones a deep link needs into a single-task app: `CLEAR_TOP` plus
 * `SINGLE_TOP` hand the intent to the instance that is already running (through
 * `onNewIntent`) instead of finishing and rebuilding it, so tapping a widget while the
 * app is open moves the tab rather than restarting the app.
 */
object ShellDestination {

    private const val ACTION_PREFIX = "com.callbackdev.chiaro.action.OPEN_"
    private const val EXTRA_TAB = "com.callbackdev.chiaro.extra.TAB"

    /** The intent that opens [tab], for a widget's `actionStartActivity`. */
    fun intent(context: Context, activity: Class<*>, tab: ShellTab): Intent =
        Intent(context, activity)
            .setAction(ACTION_PREFIX + tab.name)
            .putExtra(EXTRA_TAB, tab.name)
            .addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            )

    /** The tab [intent] asks for, or null when it names none — a plain launch. */
    fun of(intent: Intent?): ShellTab? {
        val name = intent?.getStringExtra(EXTRA_TAB)
            ?: intent?.action?.takeIf { it.startsWith(ACTION_PREFIX) }?.removePrefix(ACTION_PREFIX)
            ?: return null
        return ShellTab.entries.firstOrNull { it.name == name }
    }
}
