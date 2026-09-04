package com.callbackdev.chiaro.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.callbackdev.chiaro.data.ServiceLocator
import com.callbackdev.chiaro.domain.model.City
import com.callbackdev.chiaro.sync.SyncScheduler
import com.callbackdev.chiaro.sync.SyncWidgets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * The three widgets as one household (Fase 8): who is placed, and how to repaint
 * everyone. Repaints come from three directions — the repository's commit hook (new
 * data landed), the worker's failure path (the stale marker must appear), and the
 * Application's observer on place/settings changes — and all three arrive here.
 *
 * Updates are driven by the SYSTEM's provider mapping (`AppWidgetManager` ids per
 * `ComponentName`), never by Glance's own class-to-id bookkeeping: on the first
 * device pass that bookkeeping repainted every widget with the last-placed one's
 * content, and the system's answer is the one the launcher physically binds to.
 *
 * Every road out of here goes through [WidgetRefresh] first: `update()` alone wakes a
 * live composition without re-running `provideGlance`, so on its own it repaints the
 * OLD model (see [WidgetRefresh] for the whole of it).
 */
object ChiaroWidgets {

    private val household: List<Pair<Class<*>, () -> GlanceAppWidget>> = listOf(
        NowWidgetReceiver::class.java to { NowWidget() },
        TodayWidgetReceiver::class.java to { TodayWidget() },
        SkyWidgetReceiver::class.java to { SkyWidget() }
    )

    fun hasWidgets(context: Context): Boolean {
        val manager = AppWidgetManager.getInstance(context)
        return household.any { (receiver, _) ->
            manager.getAppWidgetIds(ComponentName(context, receiver)).isNotEmpty()
        }
    }

    suspend fun updateAll(context: Context) {
        WidgetRefresh.invalidate()
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val glanceManager = GlanceAppWidgetManager(context)
        household.forEach { (receiver, widget) ->
            appWidgetManager.getAppWidgetIds(ComponentName(context, receiver))
                .forEach { appWidgetId ->
                    runCatching {
                        widget().update(context, glanceManager.getGlanceIdBy(appWidgetId))
                    }
                }
        }
    }

    /** One instance, freshly loaded — the reconfigure flow's "apply now". */
    suspend fun updateOne(context: Context, appWidgetId: Int) {
        WidgetRefresh.invalidate()
        val manager = AppWidgetManager.getInstance(context)
        val glanceManager = GlanceAppWidgetManager(context)
        household.forEach { (receiver, widget) ->
            val ids = manager.getAppWidgetIds(ComponentName(context, receiver))
            if (appWidgetId in ids) {
                runCatching {
                    widget().update(context, glanceManager.getGlanceIdBy(appWidgetId))
                }
            }
        }
    }

    /** What `:core:sync` is handed at startup: the module must not know these classes. */
    fun bridge(context: Context): SyncWidgets = object : SyncWidgets {
        private val appContext = context.applicationContext
        override fun hasWidgets(): Boolean = hasWidgets(appContext)
        override suspend fun repaintAll() {
            runCatching { updateAll(appContext) }
        }

        override suspend fun pinnedCities(): List<City> {
            val pinnedIds = ServiceLocator.widgetCityStore(appContext).current()
                .values.toSet()
            if (pinnedIds.isEmpty()) return emptyList()
            return ServiceLocator.cityStore(appContext).cities.first()
                .filter { it.id in pinnedIds }
        }
    }
}

/**
 * Placing the first widget starts the periodic job (it is its own reason to fetch,
 * see `SyncScheduler.shouldRun`); removing the last one lets the job self-heal away;
 * a removed instance takes its pin and its look with it.
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

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        val appContext = context.applicationContext
        scope.launch {
            runCatching { ServiceLocator.widgetCityStore(appContext).forget(appWidgetIds) }
            runCatching { WidgetLookStore.get(appContext).forget(appWidgetIds) }
        }
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
