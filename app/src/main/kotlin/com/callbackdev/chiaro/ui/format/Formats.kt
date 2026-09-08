package com.callbackdev.chiaro.ui.format

import com.callbackdev.chiaro.domain.settings.TemperatureUnit
import com.callbackdev.chiaro.domain.settings.WindSpeedUnit
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.pow

/**
 * Every number the screen prints goes through here, because DESIGN.md §5 makes
 * rounding a rule and §11 makes the locale's formatter non-negotiable. Pure
 * functions: locale and the 12/24-hour preference are parameters, so the whole
 * file is testable without a device.
 */
object Formats {

    fun temperature(
        celsius: Double,
        unit: TemperatureUnit,
        locale: Locale,
        decimals: Int = 0
    ): String {
        val value = when (unit) {
            TemperatureUnit.CELSIUS -> celsius
            TemperatureUnit.FAHRENHEIT -> celsius * 9.0 / 5.0 + 32.0
        }
        // A value that rounds away to zero keeps its sign through `%f`, so −0.4 °C
        // printed **"-0°"** — a reading nobody writes by hand, and the one thing the
        // sub-zero end of the scale got wrong. At this precision that value IS zero,
        // so it is formatted as zero. The threshold mirrors `%f`'s own HALF_UP: −0.5
        // still rounds to −1°, and every other negative keeps its sign.
        val roundsToZero = abs(value) * 10.0.pow(decimals) < 0.5
        return String.format(locale, "%.${decimals}f°", if (roundsToZero) 0.0 else value)
    }

    fun wind(kph: Double, unit: WindSpeedUnit, locale: Locale): String = when (unit) {
        WindSpeedUnit.KMH -> String.format(locale, "%.0f km/h", kph)
        WindSpeedUnit.MPH -> String.format(locale, "%.0f mph", kph / 1.609344)
    }

    /** Distances to one decimal below ten, whole above (§5). */
    fun kilometers(km: Double, locale: Locale): String =
        if (km < 10) String.format(locale, "%.1f km", km) else String.format(locale, "%.0f km", km)

    fun pressure(mb: Double, locale: Locale): String = String.format(locale, "%.0f hPa", mb)

    /**
     * A probability or a proportion. It went through here on the Fase 9 IT/EN pass,
     * having been `"$pct%"` in five places until then.
     *
     * In Italian and English that template is right, which is exactly why it survived
     * five readings: §11's rule is not "the output must differ", it is that a number the
     * screen prints is the locale's to shape — digits included. A hand-built `"$h:$m"`
     * is called out in the document by name; a hand-built percentage is the same
     * sentence with a different unit.
     */
    fun percent(value: Int, locale: Locale): String = String.format(locale, "%d%%", value)

    /** Clock times honor the reader's 12/24-hour system setting, always. */
    fun timeFormatter(is24Hour: Boolean, locale: Locale): DateTimeFormatter =
        DateTimeFormatter.ofPattern(if (is24Hour) "HH:mm" else "h:mm a", locale)

    /** The hour strip's compact label: "17" or "5 PM". */
    fun hourLabel(time: LocalDateTime, is24Hour: Boolean, locale: Locale): String =
        time.format(DateTimeFormatter.ofPattern(if (is24Hour) "HH" else "h a", locale))

    /** A week row's day name: short, capitalized the locale's way. */
    fun dayLabel(date: java.time.LocalDate, locale: Locale): String =
        date.format(DateTimeFormatter.ofPattern("EEE", locale))
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }

    /** The whole day written out — "Sunday 7 September", "Domenica 7 settembre".
     * Capitalized like [dayLabel]: Italian writes its weekdays in lower case inside a
     * sentence, and this one is not inside anything. */
    fun dayLong(date: java.time.LocalDate, locale: Locale): String =
        date.format(DateTimeFormatter.ofPattern("EEEE d MMMM", locale))
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
}
