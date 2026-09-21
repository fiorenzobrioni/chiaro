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
import com.callbackdev.chiaro.ui.theme.WidgetCardColor
import kotlinx.coroutines.flow.first

private val Context.widgetLookDataStore by preferencesDataStore(name = "widget_look")

/** What a widget wears: the sky gradient (the app's own hero, the default), a plain card
 * in light, dark, or whatever the phone says — or, since 19 set 2026, one of the card
 * colours of [com.callbackdev.chiaro.ui.theme.WidgetCardColor] (committente: «possibilità
 * di mettere uno sfondo colorato: blu, blu chiaro, verde…»). [COLOR] is one value and not
 * six, because which colour is a second question and only the reader who picked COLOR is
 * ever asked it: every `when` over this enum stays four lines long, and `WidgetLook` keeps
 * the answer in [WidgetLook.cardColor]. */
enum class WidgetBackground { SKY, LIGHT, DARK, SYSTEM, COLOR }

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

/**
 * Which way round the Now widget's one-row card is laid (committente, 8 set 2026,
 * afternoon: «un layout alternativo… l'icona a destra, e nella prima riga il testo che
 * lì è nella seconda»).
 *
 * [ICON_START] is the launcher's own grammar and the default: glyph first, the number
 * over the place beside it, the sentence against the far edge. [ICON_END] is the tall
 * card's composition pressed into one row — the glyph in the trailing corner, and on
 * the leading side the number with the sentence at its shoulder and the place under
 * both — so a reader who likes the square card can have its look on a strip too, and
 * a home screen can carry the two without them fighting. A tall card ignores the
 * choice: it already has its glyph on the trailing side.
 */
enum class WidgetArrangement { ICON_START, ICON_END }

/** One widget's look: its background, how solid the card is (0 = see-through), and
 * what it puts on the card. */
data class WidgetLook(
    /**
     * **A blue card since 21 set 2026** (committente), where it was the sky gradient.
     * The sky is still the app's own hero and still one tap away; what it is not is
     * the right thing to meet on a home screen it has never seen. It is a photograph
     * of the weather behind a card of facts, and on a busy wallpaper the two grounds
     * argue — which is exactly what the scrim and the opacity slider exist to manage,
     * and a default should not need managing. A flat coloured card is the launcher's
     * own grammar, reads at arm's length on any wallpaper, and the colour is a row
     * away for anyone who wants another.
     */
    val background: WidgetBackground = WidgetBackground.COLOR,
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
     * **Except on «In parole», where it is on by default since 21 set 2026**
     * (committente, and see [defaultsFor]): that card has no drawing to protect. Its
     * hierarchy is built out of type, in ranks of facts, and the day's high and low are
     * a rank that is already designed — leaving it empty is the one card that loses
     * something by staying bare.
     */
    val showDayRange: Boolean = false,
    /**
     * The day's sentence on the Now and Today widgets — the headline Today opens with
     * in its brief register, or the sky's state when there is nothing to warn about.
     * **On by default**, because it is what the slot is for; a reader who wants the
     * bare number turns it off per widget (committente, 8 set 2026, afternoon: «la
     * possibilità di rendere visibile/invisibile la descrizione»). Whether there is
     * ROOM for it is still the grant's decision ([NowLayout]): this switch can only
     * take the sentence away, never force it onto a card too narrow to hold it.
     *
     * It replaces, a day later and the other way up, the «state beside the temperature»
     * switch that went on 8 set morning: that one was off by default and decided the
     * layout, this one is on by default and decides only the content.
     */
    val showSentence: Boolean = true,
    /**
     * The official warning's chip on the Now and Today widgets (Fase 11). **On by
     * default**: on a day without a warning it changes nothing at all, and on a day with
     * one it is the line a reader would least want their home screen to keep quiet about.
     * The launcher's own weather widgets show a line when the national service issues a
     * warning, so this follows a habit rather than inventing one.
     */
    val showWarning: Boolean = true,
    /** Which way round the Now widget's one-row card is laid; see [WidgetArrangement]. */
    val arrangement: WidgetArrangement = WidgetArrangement.ICON_START,
    /**
     * The icon family this card draws with, or [WidgetIcons.APP] to keep following the
     * Settings choice — which is the default, and what every widget placed before this
     * option existed keeps doing.
     */
    val icons: WidgetIcons = WidgetIcons.APP,
    /**
     * The weather glyph on the text widget (committente, 20 set 2026). **Off by default**,
     * and that is the card's name keeping its word: «In parole» has to be true the moment it
     * is placed, and a reader who wants the drawing turns it on. It is per widget rather
     * than an app setting for the reason every other content switch here is — so one home
     * screen can carry the same card twice, once with the glyph and once without, which is
     * half of why the option is worth having.
     *
     * It changes nothing on the other four cards, which have always drawn their glyph, and
     * it is only offered on the one it means something to (`WidgetConfigActivity`).
     */
    val showIcon: Boolean = false,
    /**
     * Which colour a [WidgetBackground.COLOR] card is painted (19 set 2026). It is kept
     * even while the background is something else, so a reader who tries the sky and comes
     * back finds the colour they picked rather than the default: a stored choice that
     * forgets itself on the way past is a choice the reader has to make twice.
     */
    val cardColor: WidgetCardColor = WidgetCardColor.BLUE
) {
    companion object {
        /** 100 since 7 set 2026 (committente): a solid card. The reader can thin it
         * per widget, and below [InkTrustFloorPct] the ink starts asking the wallpaper
         * what color it should be, which is a different conversation. */
        const val DEFAULT_OPACITY = 100

        /**
         * What a freshly placed widget of [kind] wears before anybody configures it.
         *
         * One field answers differently per card and it is [showDayRange], on «In
         * parole» alone: that card's whole premise is facts in words, and the day's
         * range is a rank its layout already draws. Everywhere else the field's own
         * KDoc holds — the bare number is the hero. A null [kind] is an id the host has
         * not bound yet, and takes the household's answer rather than guessing.
         *
         * It is a function and not five constants so that the choice has one home: a
         * default that differs per card is exactly the kind that otherwise ends up
         * written twice, in the store and in the settings screen, and drifts.
         */
        fun defaultsFor(kind: WidgetKind?): WidgetLook =
            WidgetLook(showDayRange = kind == WidgetKind.TEXT)
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

    /**
     * What this widget wears. [kind] decides only the defaults — the stored answers win
     * whatever it says — and it is passed in rather than looked up here because both
     * callers already know it and this class has no `Context` to ask with.
     */
    suspend fun lookFor(appWidgetId: Int, kind: WidgetKind?): WidgetLook {
        val prefs = dataStore.data.first()
        val default = WidgetLook.defaultsFor(kind)
        val background = prefs[backgroundKey(appWidgetId)]
            ?.let { name -> WidgetBackground.entries.firstOrNull { it.name == name } }
            ?: default.background
        val opacity = (prefs[opacityKey(appWidgetId)] ?: default.opacityPct)
            .coerceIn(0, 100)
        return WidgetLook(
            background = background,
            opacityPct = opacity,
            showDayRange = prefs[rangeKey(appWidgetId)] ?: default.showDayRange,
            showSentence = prefs[sentenceKey(appWidgetId)] ?: default.showSentence,
            showWarning = prefs[warningKey(appWidgetId)] ?: default.showWarning,
            arrangement = prefs[arrangementKey(appWidgetId)]
                ?.let { name -> WidgetArrangement.entries.firstOrNull { it.name == name } }
                ?: default.arrangement,
            icons = prefs[iconsKey(appWidgetId)]
                ?.let { name -> WidgetIcons.entries.firstOrNull { it.name == name } }
                ?: default.icons,
            showIcon = prefs[iconShownKey(appWidgetId)] ?: default.showIcon,
            cardColor = prefs[cardColorKey(appWidgetId)]
                ?.let { name -> WidgetCardColor.entries.firstOrNull { it.name == name } }
                ?: default.cardColor
        )
    }

    suspend fun set(appWidgetId: Int, look: WidgetLook) {
        dataStore.edit { prefs ->
            prefs[backgroundKey(appWidgetId)] = look.background.name
            prefs[opacityKey(appWidgetId)] = look.opacityPct.coerceIn(0, 100)
            prefs[rangeKey(appWidgetId)] = look.showDayRange
            prefs[sentenceKey(appWidgetId)] = look.showSentence
            prefs[warningKey(appWidgetId)] = look.showWarning
            prefs[arrangementKey(appWidgetId)] = look.arrangement.name
            prefs[iconsKey(appWidgetId)] = look.icons.name
            prefs[iconShownKey(appWidgetId)] = look.showIcon
            prefs[cardColorKey(appWidgetId)] = look.cardColor.name
        }
    }

    /** Called from the receivers' onDeleted: removed widgets leave nothing behind. */
    suspend fun forget(appWidgetIds: IntArray) {
        dataStore.edit { prefs ->
            appWidgetIds.forEach {
                prefs.remove(backgroundKey(it))
                prefs.remove(opacityKey(it))
                prefs.remove(rangeKey(it))
                prefs.remove(sentenceKey(it))
                prefs.remove(warningKey(it))
                prefs.remove(arrangementKey(it))
                prefs.remove(iconsKey(it))
                prefs.remove(iconShownKey(it))
                prefs.remove(cardColorKey(it))
                // The switch this key belonged to is gone (8 set 2026); a widget placed
                // while it existed still carries the key, and leaves with it.
                prefs.remove(legacyConditionKey(it))
            }
        }
    }

    private fun backgroundKey(id: Int) = stringPreferencesKey("bg_$id")
    private fun opacityKey(id: Int) = intPreferencesKey("opacity_$id")
    private fun rangeKey(id: Int) = booleanPreferencesKey("range_$id")
    private fun sentenceKey(id: Int) = booleanPreferencesKey("sentence_$id")
    private fun warningKey(id: Int) = booleanPreferencesKey("warning_$id")
    private fun arrangementKey(id: Int) = stringPreferencesKey("arrangement_$id")
    private fun iconsKey(id: Int) = stringPreferencesKey("icons_$id")
    private fun iconShownKey(id: Int) = booleanPreferencesKey("icon_shown_$id")
    private fun cardColorKey(id: Int) = stringPreferencesKey("card_color_$id")
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
