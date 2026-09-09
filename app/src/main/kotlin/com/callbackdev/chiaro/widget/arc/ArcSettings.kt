package com.callbackdev.chiaro.widget.arc

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.arcSettingsDataStore by preferencesDataStore(name = "widget_arc")

/**
 * What stretch of time the arc spans. [TODAY] is the civil day, midnight to midnight:
 * the classic arch, with what has already happened drawn faded and the present marked.
 * [AHEAD] is the next twenty-four hours from now: tonight and tomorrow morning, always
 * in front of the reader, at the cost of the arch's familiar shape.
 */
enum class ArcSpan { TODAY, AHEAD }

/**
 * What the arc is drawn on. [BANDS] paints every hour with the sky the canvas would
 * compute for it — dawn, noon, dusk and night side by side, under the same scrim the
 * card wears. [RIBBON] is the app's own daylight ribbon along the horizon line.
 * [NONE] leaves the card's ground bare and draws only the paths.
 */
enum class ArcGround { BANDS, RIBBON, NONE }

/** The sentence at the head of the card: the next light moment, the day's headline
 * (the Now widget's sentence), or nothing but the number. */
enum class ArcHero { NEXT_MOMENT, HEADLINE, NONE }

/** The one figure a one-cell card has room for. */
enum class ArcDialFigure { TEMPERATURE, NEXT_TIME }

/** How tightly the words are set: [COMPACT] trades a little size for one more row. */
enum class ArcDensity { COMFORTABLE, COMPACT }

/**
 * One arc widget's own settings — everything the card carries beyond the look every
 * widget shares (background, opacity, icon family: [com.callbackdev.chiaro.widget.WidgetLook])
 * and the place it watches. Every field has a default a fresh widget draws with, and
 * every field is per instance: two arcs on one home screen may honestly disagree.
 */
data class ArcSettings(
    val span: ArcSpan = ArcSpan.TODAY,
    val ground: ArcGround = ArcGround.BANDS,
    /** The sun's path across the window, and its disc where it stands now. */
    val sunPath: Boolean = true,
    /** The moon's path while it is up, and its disc drawn in its real phase. */
    val moon: Boolean = true,
    /** The rain chance of each hour, rising from the ground towards the horizon. */
    val rain: Boolean = true,
    /** Hour labels under the plot, where the card is tall enough for them. */
    val hourLabels: Boolean = true,
    /** The forecast temperature under each labelled hour, where there is room. */
    val temperatures: Boolean = true,
    /** The vertical mark at the present moment. */
    val nowMarker: Boolean = true,
    /** Whether the hours already behind the reader are drawn dimmed. */
    val fadePast: Boolean = true,
    val hero: ArcHero = ArcHero.NEXT_MOMENT,
    val dialFigure: ArcDialFigure = ArcDialFigure.TEMPERATURE,
    /** The agenda's three families: the sun's moments, the moon's, the rain's turns. */
    val agendaSun: Boolean = true,
    val agendaMoon: Boolean = true,
    val agendaRain: Boolean = true,
    /** The verdict mark beside an agenda row that is also a moment the reader follows. */
    val agendaVerdicts: Boolean = true,
    /** The week's seven days at the foot of a four-row card. */
    val week: Boolean = true,
    /**
     * The official warning's chip (Fase 11), on the two forms with a line to spare — the
     * card and the panel. On by default, for the reason
     * [com.callbackdev.chiaro.widget.WidgetLook.showWarning] gives: on a quiet day it
     * costs nothing, and on the other kind it is the line worth keeping.
     */
    val warning: Boolean = true,
    val density: ArcDensity = ArcDensity.COMFORTABLE
) {
    /** The factor every text size on the card is multiplied by. */
    val textScale: Float get() = if (density == ArcDensity.COMPACT) CompactTextScale else 1f

    companion object {
        /** Compact sets the words at 90%: 13 sp becomes 11.7, which is still a legible
         * label, and a 24 dp agenda row becomes 22 — one more row on a three-row card. */
        const val CompactTextScale = 0.9f
    }
}

/**
 * The settings as a flat string map, and back — pure, so a test can pin the round trip
 * without a DataStore ([ArcSettingsTest]). A value that fails to parse falls back to its
 * default rather than failing the whole card: a widget placed under an older version
 * keeps every choice it can still express.
 */
object ArcSettingsCodec {

    const val SPAN = "span"
    const val GROUND = "ground"
    const val SUN_PATH = "sun_path"
    const val MOON = "moon"
    const val RAIN = "rain"
    const val HOUR_LABELS = "hour_labels"
    const val TEMPERATURES = "temperatures"
    const val NOW_MARKER = "now_marker"
    const val FADE_PAST = "fade_past"
    const val HERO = "hero"
    const val DIAL_FIGURE = "dial_figure"
    const val AGENDA_SUN = "agenda_sun"
    const val AGENDA_MOON = "agenda_moon"
    const val AGENDA_RAIN = "agenda_rain"
    const val AGENDA_VERDICTS = "agenda_verdicts"
    const val WEEK = "week"
    const val WARNING = "warning"
    const val DENSITY = "density"

    val keys: List<String> = listOf(
        SPAN, GROUND, SUN_PATH, MOON, RAIN, HOUR_LABELS, TEMPERATURES, NOW_MARKER, FADE_PAST,
        HERO, DIAL_FIGURE, AGENDA_SUN, AGENDA_MOON, AGENDA_RAIN, AGENDA_VERDICTS, WEEK,
        WARNING, DENSITY
    )

    fun encode(settings: ArcSettings): Map<String, String> = mapOf(
        SPAN to settings.span.name,
        GROUND to settings.ground.name,
        SUN_PATH to settings.sunPath.toString(),
        MOON to settings.moon.toString(),
        RAIN to settings.rain.toString(),
        HOUR_LABELS to settings.hourLabels.toString(),
        TEMPERATURES to settings.temperatures.toString(),
        NOW_MARKER to settings.nowMarker.toString(),
        FADE_PAST to settings.fadePast.toString(),
        HERO to settings.hero.name,
        DIAL_FIGURE to settings.dialFigure.name,
        AGENDA_SUN to settings.agendaSun.toString(),
        AGENDA_MOON to settings.agendaMoon.toString(),
        AGENDA_RAIN to settings.agendaRain.toString(),
        AGENDA_VERDICTS to settings.agendaVerdicts.toString(),
        WEEK to settings.week.toString(),
        WARNING to settings.warning.toString(),
        DENSITY to settings.density.name
    )

    fun decode(values: Map<String, String>): ArcSettings {
        val defaults = ArcSettings()
        fun flag(key: String, default: Boolean): Boolean =
            values[key]?.toBooleanStrictOrNull() ?: default
        fun <E : Enum<E>> named(key: String, default: E, entries: List<E>): E =
            values[key]?.let { name -> entries.firstOrNull { it.name == name } } ?: default
        return ArcSettings(
            span = named(SPAN, defaults.span, ArcSpan.entries),
            ground = named(GROUND, defaults.ground, ArcGround.entries),
            sunPath = flag(SUN_PATH, defaults.sunPath),
            moon = flag(MOON, defaults.moon),
            rain = flag(RAIN, defaults.rain),
            hourLabels = flag(HOUR_LABELS, defaults.hourLabels),
            temperatures = flag(TEMPERATURES, defaults.temperatures),
            nowMarker = flag(NOW_MARKER, defaults.nowMarker),
            fadePast = flag(FADE_PAST, defaults.fadePast),
            hero = named(HERO, defaults.hero, ArcHero.entries),
            dialFigure = named(DIAL_FIGURE, defaults.dialFigure, ArcDialFigure.entries),
            agendaSun = flag(AGENDA_SUN, defaults.agendaSun),
            agendaMoon = flag(AGENDA_MOON, defaults.agendaMoon),
            agendaRain = flag(AGENDA_RAIN, defaults.agendaRain),
            agendaVerdicts = flag(AGENDA_VERDICTS, defaults.agendaVerdicts),
            week = flag(WEEK, defaults.week),
            warning = flag(WARNING, defaults.warning),
            density = named(DENSITY, defaults.density, ArcDensity.entries)
        )
    }
}

/**
 * Per-widget arc settings, keyed by appWidgetId, in a DataStore of their own: they are
 * presentation and belong to `:app`, like the look store next door, and they leave with
 * the widget ([forget], from the receiver's onDeleted).
 */
class ArcSettingsStore(private val dataStore: DataStore<Preferences>) {

    /** The settings as they change — the configuration screen's preview reads this. */
    fun settings(appWidgetId: Int): Flow<ArcSettings> =
        dataStore.data.map { prefs -> decode(prefs, appWidgetId) }

    suspend fun settingsFor(appWidgetId: Int): ArcSettings =
        decode(dataStore.data.first(), appWidgetId)

    suspend fun set(appWidgetId: Int, settings: ArcSettings) {
        dataStore.edit { prefs ->
            ArcSettingsCodec.encode(settings).forEach { (key, value) ->
                prefs[key(appWidgetId, key)] = value
            }
        }
    }

    suspend fun forget(appWidgetIds: IntArray) {
        dataStore.edit { prefs ->
            appWidgetIds.forEach { id ->
                ArcSettingsCodec.keys.forEach { key -> prefs.remove(key(id, key)) }
            }
        }
    }

    private fun decode(prefs: Preferences, appWidgetId: Int): ArcSettings =
        ArcSettingsCodec.decode(
            ArcSettingsCodec.keys.mapNotNull { key ->
                prefs[key(appWidgetId, key)]?.let { key to it }
            }.toMap()
        )

    private fun key(appWidgetId: Int, field: String) = stringPreferencesKey("arc_${field}_$appWidgetId")

    companion object {
        @Volatile
        private var instance: ArcSettingsStore? = null

        fun get(context: Context): ArcSettingsStore =
            instance ?: synchronized(this) {
                instance ?: ArcSettingsStore(context.applicationContext.arcSettingsDataStore)
                    .also { instance = it }
            }
    }
}
