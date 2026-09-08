package com.callbackdev.chiaro.widget

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.callbackdev.chiaro.data.WeatherIcons
import kotlinx.coroutines.flow.first

private val Context.widgetLookDataStore by preferencesDataStore(name = "widget_look")

/** What a widget wears: the sky gradient (the app's own hero, the default), or a
 * plain card in light, dark, or whatever the phone says. */
enum class WidgetBackground { SKY, LIGHT, DARK, SYSTEM }

/**
 * Which weather drawings a widget uses (committente, 8 set): the app's own choice, or
 * one of the two Meteocons families named outright.
 *
 * The app has one icon setting and it stays the app's; this is the per-widget override
 * beside the background and the opacity, and for the same reason those are per-widget —
 * a card on a home screen is not the app screen, it sits on a wallpaper the app never
 * sees, at a size the app never draws. The filled family reads at a glance across a
 * room where the line family reads quietly on a page, and a reader may honestly want
 * one in each place.
 *
 * [APP] is the default, so a widget already placed keeps drawing what it drew and every
 * later change to the Settings choice still reaches it.
 */
enum class WidgetIcons {
    APP, FILL, LINE;

    /** The family this choice really means, given what the app is set to. */
    fun resolve(app: WeatherIcons): WeatherIcons = when (this) {
        APP -> app
        FILL -> WeatherIcons.FILL
        LINE -> WeatherIcons.LINE
    }
}

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
    val showCondition: Boolean = false,
    /**
     * The day's high and low, anchored to the trailing edge (committente, 4 set). The
     * pair was on these two widgets until the third device pass took it off, and the
     * reason it went was WHERE it was: printed under a 34sp number it read as clutter.
     * Against the far edge, level with the state, it is the thing every weather widget
     * carries and the hero keeps its air.
     *
     * **Off by default since 7 set 2026** (committente), which is the third position
     * this pair has held and the one that matches the rest of the card: the bare number
     * is the hero, and the range is a tap away for whoever wants it. A widget already on
     * a home screen that never had this edited follows the new default — that is what a
     * default is, and moving it for those readers is the point of moving it.
     */
    val showDayRange: Boolean = false,
    /**
     * The icon family this card draws with, or [WidgetIcons.APP] to keep following the
     * Settings choice — which is the default, and what every widget placed before this
     * option existed keeps doing.
     */
    val icons: WidgetIcons = WidgetIcons.APP
) {
    companion object {
        /** 100 since 7 set 2026 (committente): a solid card. The reader can thin it
         * per widget, and below [InkTrustFloorPct] the ink starts asking the wallpaper
         * what color it should be, which is a different conversation. */
        const val DEFAULT_OPACITY = 100
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
        return WidgetLook(
            background = background,
            opacityPct = opacity,
            showCondition = prefs[conditionKey(appWidgetId)] ?: false,
            showDayRange = prefs[rangeKey(appWidgetId)] ?: false,
            icons = prefs[iconsKey(appWidgetId)]
                ?.let { name -> WidgetIcons.entries.firstOrNull { it.name == name } }
                ?: WidgetIcons.APP
        )
    }

    suspend fun set(appWidgetId: Int, look: WidgetLook) {
        dataStore.edit { prefs ->
            prefs[backgroundKey(appWidgetId)] = look.background.name
            prefs[opacityKey(appWidgetId)] = look.opacityPct.coerceIn(0, 100)
            prefs[conditionKey(appWidgetId)] = look.showCondition
            prefs[rangeKey(appWidgetId)] = look.showDayRange
            prefs[iconsKey(appWidgetId)] = look.icons.name
        }
    }

    /** Called from the receivers' onDeleted: removed widgets leave nothing behind. */
    suspend fun forget(appWidgetIds: IntArray) {
        dataStore.edit { prefs ->
            appWidgetIds.forEach {
                prefs.remove(backgroundKey(it))
                prefs.remove(opacityKey(it))
                prefs.remove(conditionKey(it))
                prefs.remove(rangeKey(it))
                prefs.remove(iconsKey(it))
            }
        }
    }

    private fun backgroundKey(id: Int) = stringPreferencesKey("bg_$id")
    private fun opacityKey(id: Int) = intPreferencesKey("opacity_$id")
    private fun conditionKey(id: Int) = booleanPreferencesKey("condition_$id")
    private fun rangeKey(id: Int) = booleanPreferencesKey("range_$id")
    private fun iconsKey(id: Int) = stringPreferencesKey("icons_$id")

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
