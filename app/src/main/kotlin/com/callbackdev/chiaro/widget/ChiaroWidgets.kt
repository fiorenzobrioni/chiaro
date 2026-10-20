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
import com.callbackdev.chiaro.widget.window.WindowWidget
import com.callbackdev.chiaro.widget.window.WindowWidgetReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * The widgets as one household (Fase 8): who is placed, and how to repaint
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
/** The five, as the reconfigure screen needs to tell them apart. WINDOW took ARC's place
 * on 20 ott 2026, and with it the arc's own configuration screen went: one screen for all. */
enum class WidgetKind { NOW, TODAY, SKY, WINDOW, TEXT }

object ChiaroWidgets {

    private val household: List<Pair<Class<*>, () -> GlanceAppWidget>> = listOf(
        NowWidgetReceiver::class.java to { NowWidget() },
        TodayWidgetReceiver::class.java to { TodayWidget() },
        SkyWidgetReceiver::class.java to { SkyWidget() },
        WindowWidgetReceiver::class.java to { WindowWidget() },
        TextWidgetReceiver::class.java to { TextWidget() }
    )

    fun hasWidgets(context: Context): Boolean {
        val manager = AppWidgetManager.getInstance(context)
        return household.any { (receiver, _) ->
            manager.getAppWidgetIds(ComponentName(context, receiver)).isNotEmpty()
        }
    }

    /**
     * Which of them [appWidgetId] is — the reconfigure screen is one activity for
     * all of them, and an option only some honour must not be offered to the rest. An
     * unbound id (asked before the host has bound the provider) answers null: better a
     * missing switch for one frame than one that changes nothing.
     */
    fun kindOf(context: Context, appWidgetId: Int): WidgetKind? =
        when (
            AppWidgetManager.getInstance(context).getAppWidgetInfo(appWidgetId)
                ?.provider?.className
        ) {
            NowWidgetReceiver::class.java.name -> WidgetKind.NOW
            TodayWidgetReceiver::class.java.name -> WidgetKind.TODAY
            SkyWidgetReceiver::class.java.name -> WidgetKind.SKY
            WindowWidgetReceiver::class.java.name -> WidgetKind.WINDOW
            TextWidgetReceiver::class.java.name -> WidgetKind.TEXT
            else -> null
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
 * a removed instance takes its pin and its look with it, and a restored one finds them
 * under its new id.
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

    /**
     * A backup restored onto a new phone (18 ott 2026): the launcher puts the widgets back
     * under new ids, and the pin and the look move with them. The
     * two stores are in the backup (`data_extraction_rules.xml`), and until this was
     * overridden they came back filed under ids nothing was bound to: every restored
     * widget lost its settings, and a new widget handed one of the old numbers inherited
     * somebody else's.
     */
    override fun onRestored(context: Context, oldWidgetIds: IntArray, newWidgetIds: IntArray) {
        super.onRestored(context, oldWidgetIds, newWidgetIds)
        val appContext = context.applicationContext
        scope.launch {
            runCatching { ServiceLocator.widgetCityStore(appContext).remap(oldWidgetIds, newWidgetIds) }
            runCatching { WidgetLookStore.get(appContext).remap(oldWidgetIds, newWidgetIds) }
            runCatching { ChiaroWidgets.updateAll(appContext) }
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

class TextWidgetReceiver : ChiaroWidgetReceiver() {
    override val glanceAppWidget = TextWidget()
}
