package com.callbackdev.chiaro.widget

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.widgetLookDataStore by preferencesDataStore(name = "widget_look")

/** What a widget wears: the sky gradient (the app's own hero, the default), or a
 * plain card in light, dark, or whatever the phone says. */
enum class WidgetBackground { SKY, LIGHT, DARK, SYSTEM }

/** One widget's look: its background, how solid the card is (0 = see-through), and
 * what it puts on the card. */
data class WidgetLook(
    val background: WidgetBackground = WidgetBackground.SKY,
    val opacityPct: Int = DEFAULT_OPACITY,
    /**
     * The Now widget prints the sky's state beside the temperature (committente,
     * 4 set). **Off by default, and a choice rather than a measurement**: the widget
     * could tell from its granted width whether the words fit, but then it would
     * change what it says while the reader drags its handles, and a widget that
     * rewrites itself mid-resize is not one you can aim. The reader asks for it once;
     * on a narrow card the line simply clips, which is a thing they can see and undo.
     * Off by default so every widget already on a home screen keeps the layout it was
     * placed with.
     */
    val showCondition: Boolean = false
) {
    companion object {
        const val DEFAULT_OPACITY = 85
    }
}

/**
 * Per-widget looks, keyed by appWidgetId (Fase 8, device review): each placed widget
 * carries its own background and opacity, edited from the launcher's reconfigure
 * flow — a setting that lives next to the thing it changes. The city pin lives in
 * the inherited [com.callbackdev.chiaro.data.WidgetCityStore]; this store is
 * presentation only, which is why it lives in :app.
 */
class WidgetLookStore(private val dataStore: DataStore<Preferences>) {

    suspend fun lookFor(appWidgetId: Int): WidgetLook {
        val prefs = dataStore.data.first()
        val background = prefs[backgroundKey(appWidgetId)]
            ?.let { name -> WidgetBackground.entries.firstOrNull { it.name == name } }
            ?: WidgetBackground.SKY
        val opacity = (prefs[opacityKey(appWidgetId)] ?: WidgetLook.DEFAULT_OPACITY)
            .coerceIn(0, 100)
        return WidgetLook(background, opacity, prefs[conditionKey(appWidgetId)] ?: false)
    }

    suspend fun set(appWidgetId: Int, look: WidgetLook) {
        dataStore.edit { prefs ->
            prefs[backgroundKey(appWidgetId)] = look.background.name
            prefs[opacityKey(appWidgetId)] = look.opacityPct.coerceIn(0, 100)
            prefs[conditionKey(appWidgetId)] = look.showCondition
        }
    }

    /** Called from the receivers' onDeleted: removed widgets leave nothing behind. */
    suspend fun forget(appWidgetIds: IntArray) {
        dataStore.edit { prefs ->
            appWidgetIds.forEach {
                prefs.remove(backgroundKey(it))
                prefs.remove(opacityKey(it))
                prefs.remove(conditionKey(it))
            }
        }
    }

    private fun backgroundKey(id: Int) = stringPreferencesKey("bg_$id")
    private fun opacityKey(id: Int) = intPreferencesKey("opacity_$id")
    private fun conditionKey(id: Int) = booleanPreferencesKey("condition_$id")

    companion object {
        @Volatile
        private var instance: WidgetLookStore? = null

        fun get(context: Context): WidgetLookStore =
            instance ?: synchronized(this) {
                instance ?: WidgetLookStore(context.applicationContext.widgetLookDataStore)
                    .also { instance = it }
            }
    }
}
