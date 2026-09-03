package com.callbackdev.chiaro.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.updateAll
import com.callbackdev.chiaro.sync.SyncScheduler
import com.callbackdev.chiaro.sync.SyncWidgets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * The three widgets as one household (Fase 8): who is placed, and how to repaint
 * everyone. Repaints come from three directions — the repository's commit hook (new
 * data landed), the worker's failure path (the stale marker must appear), and the
 * Application's observer on place/settings changes — and all three arrive here.
 */
object ChiaroWidgets {

    private val receivers = listOf(
        NowWidgetReceiver::class.java,
        TodayWidgetReceiver::class.java,
        SkyWidgetReceiver::class.java
    )

    fun hasWidgets(context: Context): Boolean {
        val manager = AppWidgetManager.getInstance(context)
        return receivers.any { receiver ->
            manager.getAppWidgetIds(ComponentName(context, receiver)).isNotEmpty()
        }
    }

    suspend fun updateAll(context: Context) {
        NowWidget().updateAll(context)
        TodayWidget().updateAll(context)
        SkyWidget().updateAll(context)
    }

    /** What `:core:sync` is handed at startup: the module must not know these classes. */
    fun bridge(context: Context): SyncWidgets = object : SyncWidgets {
        private val appContext = context.applicationContext
        override fun hasWidgets(): Boolean = hasWidgets(appContext)
        override suspend fun repaintAll() {
            runCatching { updateAll(appContext) }
        }
    }
}

/**
 * Placing the first widget starts the periodic job (it is its own reason to fetch,
 * see `SyncScheduler.shouldRun`); removing the last one lets the job self-heal away.
 * Both edges reconcile immediately rather than waiting for the next app start.
 */
abstract class ChiaroWidgetReceiver : GlanceAppWidgetReceiver() {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        val appContext = context.applicationContext
        scope.launch {
            runCatching { SyncScheduler.reconcile(appContext) }
            runCatching { ChiaroWidgets.updateAll(appContext) }
        }
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        val appContext = context.applicationContext
        scope.launch { runCatching { SyncScheduler.reconcile(appContext) } }
    }
}

class NowWidgetReceiver : ChiaroWidgetReceiver() {
    override val glanceAppWidget = NowWidget()
}

class TodayWidgetReceiver : ChiaroWidgetReceiver() {
    override val glanceAppWidget = TodayWidget()
}

class SkyWidgetReceiver : ChiaroWidgetReceiver() {
    override val glanceAppWidget = SkyWidget()
}
