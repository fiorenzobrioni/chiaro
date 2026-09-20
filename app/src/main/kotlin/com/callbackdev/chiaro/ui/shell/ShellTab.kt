package com.callbackdev.chiaro.ui.shell

import android.content.Context
import android.content.Intent

/** The four destinations of the bottom bar (VISION §5.1). */
enum class ShellTab { TODAY, SKY, ALERTS, JOURNAL }

/**
 * **The one door into the app from outside it** — every widget and every notification —
 * and the tab it asks for, when it asks for one.
 *
 * It is one door because of what the alternative cost, found on a device on 21 set
 * 2026. Android identifies a task by the intent that created it, and an app that is
 * entered through three hand-rolled intents is an app with three kinds of task. The
 * reader saw both halves of that: two copies of the home screen, one behind the other,
 * after opening the app from a widget and then from the launcher icon; and a closing
 * animation with opaque rounded corners instead of the launcher's own, which is the
 * animation the system plays for a task the launcher does not recognise as one of its
 * icons.
 *
 * So this intent **is** the launcher's intent — `ACTION_MAIN`, `CATEGORY_LAUNCHER`, our
 * own component — plus, where there is one, the destination as an extra. A tap on a
 * widget asks for the same task the icon asks for, and there is only ever one.
 *
 * The extra is safe to be the only carrier: Glance builds these with
 * `PendingIntent.FLAG_UPDATE_CURRENT`, which replaces the extras of a cached intent
 * rather than keeping the old ones, and stamps a `data` URI of its own per widget so
 * two cards never share an entry. Putting the destination in the ACTION instead — which
 * is what the first pass did — is precisely what stopped the task looking like the
 * launcher's.
 *
 * **One intent has one consequence worth knowing**: a `PendingIntent` built over it is
 * told apart from another only by its REQUEST CODE, because `filterEquals` — which the
 * cache keys on — cannot see the extra the destination rides in. With
 * `FLAG_UPDATE_CURRENT`, two callers sharing a request code would have the second
 * rewrite the first's destination, under a notification already sitting on the shade.
 * The four notifiers pass their own notification id, and those four id ranges are
 * disjoint and documented where each one is computed (1001-1004 the built-in alerts,
 * 2000-2999 the reader's rules, 3000-3999 the official warnings, 7000+ the sky
 * reminders); `NotificationDestinationTest` posts all four and reads the destinations
 * back, which is what a collision would break. The widgets are out of this: Glance
 * stamps a `data` URI of its own per card, and `data` IS part of `filterEquals`.
 *
 * `FLAG_ACTIVITY_NEW_TASK` is here because a `PendingIntent` starts from outside an
 * activity and the platform requires it. Nothing else is needed: [ShellTab] arrives at
 * a `singleTask` activity (`AndroidManifest.xml`), so the platform routes every one of
 * these to the single live instance through `onNewIntent` and clears whatever sits
 * above it. `CLEAR_TOP` and `SINGLE_TOP` were doing that job by hand in the first pass
 * and could not do the other half of it, which is guaranteeing there is one instance to
 * route to.
 */
object ShellDestination {

    private const val EXTRA_TAB = "com.callbackdev.chiaro.extra.TAB"

    /**
     * The intent that opens the app — on [tab], or wherever the reader left it when
     * [tab] is null, which is what a card showing today's weather honestly asks for.
     */
    fun intent(context: Context, activity: Class<*>, tab: ShellTab? = null): Intent =
        Intent(context, activity)
            .setAction(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .apply { if (tab != null) putExtra(EXTRA_TAB, tab.name) }

    /** The tab [intent] asks for, or null when it names none — a plain launch. */
    fun of(intent: Intent?): ShellTab? {
        val name = intent?.getStringExtra(EXTRA_TAB) ?: return null
        return ShellTab.entries.firstOrNull { it.name == name }
    }
}
