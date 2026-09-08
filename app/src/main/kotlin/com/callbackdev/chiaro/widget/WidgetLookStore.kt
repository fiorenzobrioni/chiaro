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
     * The day's high and low on the Today widget, anchored to the trailing edge
     * (committente, 4 set). The pair was on both one-row widgets until the third device
     * pass took it off, and the reason it went was WHERE it was: printed under a 34sp
     * number it read as clutter. Against the far edge, level with the temperature, it
     * is the thing every weather widget carries and the hero keeps its air.
     *
     * **Off by default since 7 set 2026** (committente): the bare number is the hero,
     * and the range is a tap away for whoever wants it. **Today only since 8 set**: the
     * Now widget is a glance at what the sky is doing now, and on it the pair competed
     * with the sentence for the same edge; the reader who wants the day's range on the
     * home screen has the widget that carries the day.
     *
     * There used to be a second switch here, the sky's state beside the Now widget's
     * temperature. It went the same day: the state's slot is now the day's sentence,
     * and whether there is room for it is decided by the grant ([NowLayout]), not by a
     * setting — see that file for the reversal and its reason.
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
                prefs.remove(rangeKey(it))
                prefs.remove(iconsKey(it))
                // The switch this key belonged to is gone (8 set 2026); a widget placed
                // while it existed still carries the key, and leaves with it.
                prefs.remove(legacyConditionKey(it))
            }
        }
    }

    private fun backgroundKey(id: Int) = stringPreferencesKey("bg_$id")
    private fun opacityKey(id: Int) = intPreferencesKey("opacity_$id")
    private fun rangeKey(id: Int) = booleanPreferencesKey("range_$id")
    private fun iconsKey(id: Int) = stringPreferencesKey("icons_$id")
    private fun legacyConditionKey(id: Int) = booleanPreferencesKey("condition_$id")

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
