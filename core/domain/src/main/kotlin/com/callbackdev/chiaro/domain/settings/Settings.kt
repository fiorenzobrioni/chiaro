package com.callbackdev.chiaro.domain.settings

/**
 * The preferences the ENGINES read, and only those.
 *
 * They lived in tweather's `SettingsStore` next to the DataStore keys, which made
 * the domain import the data layer to evaluate a rule ([com.callbackdev.chiaro.domain
 * .rules.RuleVariables] renders a threshold in the reader's units, so it needs to know
 * them). Chiaro splits the two: the values that decide an engine's answer live here,
 * the plumbing that persists them stays in the store, and `:core:domain` compiles
 * with nothing underneath it.
 *
 * Everything that only the UI cares about — the theme, the update interval, the
 * widget's opacity — deliberately did NOT move: it is not an input to any engine.
 */
enum class TemperatureUnit { CELSIUS, FAHRENHEIT }

/**
 * How a speed is printed (14.9, 28 Oct 2026): km/h and mph since Fase 2; m/s, knots and the
 * Beaufort scale for the readers outside Italy whose services print those (Yr and the
 * Nordic services m/s, the sea and the sky knots, the Met Office all five). Beaufort is a
 * force with the scale's word, never a speed: the number is the band the km/h fall in.
 * Stored by name, so the two older values keep reading.
 */
enum class WindSpeedUnit { KMH, MPH, M_S, KNOTS, BEAUFORT }

/**
 * How a pressure is printed (14.9): hectopascals, the model's own and the default; inches
 * of mercury, what the United States reads (29.92); millimetres of mercury, what Russia and
 * part of eastern Europe read (760). The engines keep hectopascals and the rules' canonical
 * id stays `_mb`.
 */
enum class PressureUnit { HPA, INHG, MMHG }

/**
 * The units the reader chose. The engines keep every value metric internally and
 * convert at the edge, so switching this never rewrites stored data — a threshold
 * saved as 20 °C is still 20 °C after a switch to Fahrenheit, and only its rendering
 * changes.
 */
data class UnitSettings(
    val temperature: TemperatureUnit = TemperatureUnit.CELSIUS,
    val windSpeed: WindSpeedUnit = WindSpeedUnit.KMH,
    val pressure: PressureUnit = PressureUnit.HPA
)

/**
 * Which built-in alerts are on. Each flag gates one rule in
 * [com.callbackdev.chiaro.domain.AlertEngine]. The reader's own rules have no master
 * switch here (15.6, 28 Oct 2026): each rule carries its own, and the flag that stood over
 * them all was never on any screen — always true, with dead branches in the page, the
 * worker and the scheduler. Its stored preference (`notif_user_rules`) is left unread.
 */
data class NotificationSettings(
    val severeWeatherAlerts: Boolean = true,
    /**
     * The morning summary: once a day between 06:00 and 12:00, today.
     *
     * **On by default since 21 set 2026** (committente), with its evening twin. Both
     * started silent on the argument that a digest nobody asked for is the one
     * notification a weather app gets uninstalled over — which is the right argument
     * about a digest, and the wrong one about these two. They are the app: a sentence
     * before any number, said at the hour the day is still a decision. An install that
     * never opens Avvisi was getting the warnings and none of the reading, and the pair
     * is capped at one notification each, at fixed hours, off in two taps.
     */
    val dailySummary: Boolean = true,
    /**
     * The evening summary (21 set 2026): once between 18:00 and 23:00, TOMORROW —
     * the night rides along in the expanded notification. **On by default** with its
     * morning twin, and for the same reason; by eight in the evening today is no longer
     * a decision and tomorrow is.
     */
    val eveningSummary: Boolean = true,
    val precipitationWarning: Boolean = true,
    /**
     * The official warnings (9 set 2026, Fase 11): whether the Protezione Civile's
     * bulletin is announced when it grades the active place's zone, and from which
     * level. On by default and from yellow — the level the Dipartimento itself calls
     * "prestare attenzione"; both are held or raised after the week of live use the
     * phase ends with, with real bulletins counted, not before.
     */
    val officialWarnings: Boolean = true,
    val officialWarningsFrom: com.callbackdev.chiaro.domain.warnings.WarningLevel =
        com.callbackdev.chiaro.domain.warnings.WarningLevel.YELLOW
)
