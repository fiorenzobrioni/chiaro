package com.callbackdev.chiaro.ui.format

import com.callbackdev.chiaro.domain.settings.TemperatureUnit
import com.callbackdev.chiaro.domain.settings.WindSpeedUnit
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

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
        return String.format(locale, "%.${decimals}f°", value)
    }

    fun wind(kph: Double, unit: WindSpeedUnit, locale: Locale): String = when (unit) {
        WindSpeedUnit.KMH -> String.format(locale, "%.0f km/h", kph)
        WindSpeedUnit.MPH -> String.format(locale, "%.0f mph", kph / 1.609344)
    }

    /** Distances to one decimal below ten, whole above (§5). */
    fun kilometers(km: Double, locale: Locale): String =
        if (km < 10) String.format(locale, "%.1f km", km) else String.format(locale, "%.0f km", km)

    fun pressure(mb: Double, locale: Locale): String = String.format(locale, "%.0f hPa", mb)

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
}
