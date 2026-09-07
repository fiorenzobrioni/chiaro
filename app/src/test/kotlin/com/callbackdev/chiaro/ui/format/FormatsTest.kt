package com.callbackdev.chiaro.ui.format

import com.callbackdev.chiaro.domain.settings.TemperatureUnit
import com.callbackdev.chiaro.domain.settings.WindSpeedUnit
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * DESIGN.md §11: every number and date the screen prints goes through the locale's
 * formatter. `Formats` was written for that rule in Fase 2 and had no test until the
 * Fase 9 IT/EN pass, which is how five hand-built percentages came to live in the UI
 * right next to it.
 *
 * Both shipped languages, side by side, because that is the failure this guards: a
 * format that is right in the language it was written in.
 */
class FormatsTest {

    private val en = Locale.forLanguageTag("en-GB")
    private val it = Locale.ITALIAN

    @Test
    fun `a temperature rounds to a whole degree and keeps its sign`() {
        assertEquals("21°", Formats.temperature(20.6, TemperatureUnit.CELSIUS, en))
        assertEquals("-4°", Formats.temperature(-3.5, TemperatureUnit.CELSIUS, en))
        assertEquals("0°", Formats.temperature(0.4, TemperatureUnit.CELSIUS, it))
    }

    @Test
    fun `Fahrenheit converts, it does not relabel`() {
        assertEquals("68°", Formats.temperature(20.0, TemperatureUnit.FAHRENHEIT, en))
        assertEquals("32°", Formats.temperature(0.0, TemperatureUnit.FAHRENHEIT, it))
    }

    @Test
    fun `the decimal separator is the language's, not the developer's`() {
        // The one place the two languages actually diverge, and the reason `locale` is a
        // parameter of every function in the file rather than a default read off the JVM.
        assertEquals("9.4 km", Formats.kilometers(9.44, en))
        assertEquals("9,4 km", Formats.kilometers(9.44, it))
        assertEquals("1.5°", Formats.temperature(1.5, TemperatureUnit.CELSIUS, en, decimals = 1))
        assertEquals("1,5°", Formats.temperature(1.5, TemperatureUnit.CELSIUS, it, decimals = 1))
    }

    @Test
    fun `distances lose their decimal above ten kilometres`() {
        assertEquals("9.9 km", Formats.kilometers(9.9, en))
        assertEquals("10 km", Formats.kilometers(10.0, en))
        assertEquals("24 km", Formats.kilometers(23.6, en))
    }

    @Test
    fun `wind converts and carries its unit`() {
        assertEquals("18 km/h", Formats.wind(18.0, WindSpeedUnit.KMH, en))
        assertEquals("11 mph", Formats.wind(18.0, WindSpeedUnit.MPH, en))
    }

    @Test
    fun `pressure is whole hectopascals`() {
        assertEquals("1013 hPa", Formats.pressure(1013.4, en))
    }

    @Test
    fun `a percentage is the locale's, in both languages`() {
        assertEquals("0%", Formats.percent(0, en))
        assertEquals("0%", Formats.percent(0, it))
        assertEquals("100%", Formats.percent(100, it))
    }

    @Test
    fun `the clock honours the reader's twelve or twenty-four hour setting`() {
        val afternoon = LocalDateTime.of(2026, 9, 7, 17, 5)
        assertEquals("17:05", afternoon.format(Formats.timeFormatter(is24Hour = true, en)))
        assertEquals("17", Formats.hourLabel(afternoon, is24Hour = true, en))
        assertEquals("5 PM", Formats.hourLabel(afternoon, is24Hour = false, en).uppercase(en))
    }

    @Test
    fun `a weekday is capitalized the way a label is, in both languages`() {
        // Italian writes its weekdays lower case inside a sentence; these are not inside
        // anything, so both languages get a capital.
        val monday = LocalDate.of(2026, 9, 7)
        assertEquals("Mon", Formats.dayLabel(monday, en))
        assertEquals("Lun", Formats.dayLabel(monday, it))
        assertEquals("Monday 7 September", Formats.dayLong(monday, en))
        assertEquals("Lunedì 7 settembre", Formats.dayLong(monday, it))
    }
}
