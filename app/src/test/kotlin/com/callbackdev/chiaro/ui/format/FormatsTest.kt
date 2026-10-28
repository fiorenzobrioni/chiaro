package com.callbackdev.chiaro.ui.format

import com.callbackdev.chiaro.domain.settings.PressureUnit
import com.callbackdev.chiaro.domain.settings.TemperatureUnit
import com.callbackdev.chiaro.domain.settings.WindSpeedUnit
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    /**
     * The sub-zero end, where `%f` used to keep the sign of a value it had just
     * rounded away: −0.4 °C printed "-0°". Both sides of the boundary and both
     * precisions, because the fix has to leave every other negative alone.
     */
    @Test
    fun `a temperature that rounds away to zero is zero, not minus zero`() {
        assertEquals("0°", Formats.temperature(-0.4, TemperatureUnit.CELSIUS, it))
        assertEquals("0°", Formats.temperature(-0.4, TemperatureUnit.CELSIUS, en))
        // The other side of HALF_UP still rounds, and still carries its sign.
        assertEquals("-1°", Formats.temperature(-0.5, TemperatureUnit.CELSIUS, en))
        assertEquals("-1°", Formats.temperature(-1.0, TemperatureUnit.CELSIUS, en))
        // Fahrenheit crosses its own zero somewhere else, and behaves the same there.
        assertEquals("0°", Formats.temperature(-17.75, TemperatureUnit.FAHRENHEIT, en))
        // One decimal: the rule follows the precision the caller asked for.
        assertEquals("0,0°", Formats.temperature(-0.04, TemperatureUnit.CELSIUS, it, decimals = 1))
        assertEquals("-0,4°", Formats.temperature(-0.4, TemperatureUnit.CELSIUS, it, decimals = 1))
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

    /**
     * The three units of 14.9, in both languages: the number converts and is whole, the
     * symbol is the unit's own (the ISO «kn» for the knot in both languages), and a
     * language changes nothing but the decimal mark, which a whole number never shows.
     */
    @Test
    fun `wind in metres a second and in knots, in both languages`() {
        assertEquals("5 m/s", Formats.wind(18.0, WindSpeedUnit.M_S, en))
        assertEquals("5 m/s", Formats.wind(18.0, WindSpeedUnit.M_S, it))
        assertEquals("31 m/s", Formats.wind(112.0, WindSpeedUnit.M_S, en))
        assertEquals("10 kn", Formats.wind(18.0, WindSpeedUnit.KNOTS, en))
        assertEquals("10 kn", Formats.wind(18.0, WindSpeedUnit.KNOTS, it))
        assertEquals("60 kn", Formats.wind(112.0, WindSpeedUnit.KNOTS, it))
        // A calm hour is zero in every unit, never a bare «-0».
        assertEquals("0 m/s", Formats.wind(0.0, WindSpeedUnit.M_S, en))
        assertEquals("0 kn", Formats.wind(0.0, WindSpeedUnit.KNOTS, it))
    }

    /**
     * Beaufort is a force and the scale's word (14.9): the band the km/h fall in, the Met
     * Office's descriptive term in English and the Italian scale's in Italian; the short
     * form is the force alone, for a tile's value and a chart's label.
     */
    @Test
    fun `Beaufort is the force with the scale's word, in each language`() {
        assertEquals("3 Bft (gentle breeze)", Formats.wind(18.0, WindSpeedUnit.BEAUFORT, en))
        assertEquals("3 Bft (brezza tesa)", Formats.wind(18.0, WindSpeedUnit.BEAUFORT, it))
        assertEquals("6 Bft (strong breeze)", Formats.wind(39.0, WindSpeedUnit.BEAUFORT, en))
        assertEquals("6 Bft (vento fresco)", Formats.wind(39.0, WindSpeedUnit.BEAUFORT, it))
        assertEquals("11 Bft (violent storm)", Formats.wind(112.0, WindSpeedUnit.BEAUFORT, en))
        assertEquals("11 Bft (fortunale)", Formats.wind(112.0, WindSpeedUnit.BEAUFORT, it))
        assertEquals("3 Bft", Formats.wind(18.0, WindSpeedUnit.BEAUFORT, en, short = true))
        assertEquals("3 Bft", Formats.wind(18.0, WindSpeedUnit.BEAUFORT, it, short = true))
    }

    /** The scale's two ends and its edges: calm under 1 km/h, hurricane from 118, and the
     * band changing exactly on the bound («tieni il cappello» at 39 is force 6). */
    @Test
    fun `the Beaufort bands change on their bounds`() {
        assertEquals("0 Bft (calm)", Formats.wind(0.0, WindSpeedUnit.BEAUFORT, en))
        assertEquals("0 Bft (calma)", Formats.wind(0.9, WindSpeedUnit.BEAUFORT, it))
        assertEquals("1 Bft (light air)", Formats.wind(1.0, WindSpeedUnit.BEAUFORT, en))
        assertEquals("5 Bft (fresh breeze)", Formats.wind(38.9, WindSpeedUnit.BEAUFORT, en))
        assertEquals("6 Bft (strong breeze)", Formats.wind(39.0, WindSpeedUnit.BEAUFORT, en))
        assertEquals("8 Bft (gale)", Formats.wind(62.0, WindSpeedUnit.BEAUFORT, en))
        assertEquals("9 Bft (strong gale)", Formats.wind(75.0, WindSpeedUnit.BEAUFORT, en))
        assertEquals("9 Bft (burrasca forte)", Formats.wind(88.9, WindSpeedUnit.BEAUFORT, it))
        assertEquals("12 Bft (hurricane)", Formats.wind(118.0, WindSpeedUnit.BEAUFORT, en))
        assertEquals("12 Bft (uragano)", Formats.wind(250.0, WindSpeedUnit.BEAUFORT, it))
        // A language the app does not ship reads the English words, like every string.
        assertEquals("8 Bft (gale)", Formats.wind(62.0, WindSpeedUnit.BEAUFORT, Locale.GERMAN))
    }

    @Test
    fun `pressure is whole hectopascals`() {
        assertEquals("1013 hPa", Formats.pressure(1013.4, PressureUnit.HPA, en))
        assertEquals("1013 hPa", Formats.pressure(1013.4, PressureUnit.HPA, it))
    }

    /**
     * The two mercury units of 14.9: the standard atmosphere is 29.92 inHg and 760 mmHg,
     * the inches to the hundredth with the language's mark, the millimetres whole.
     */
    @Test
    fun `pressure in inches and millimetres of mercury, in both languages`() {
        assertEquals("29.92 inHg", Formats.pressure(1013.25, PressureUnit.INHG, en))
        assertEquals("29,92 inHg", Formats.pressure(1013.25, PressureUnit.INHG, it))
        assertEquals("760 mmHg", Formats.pressure(1013.25, PressureUnit.MMHG, en))
        assertEquals("760 mmHg", Formats.pressure(1013.25, PressureUnit.MMHG, it))
        assertEquals("28.94 inHg", Formats.pressure(980.0, PressureUnit.INHG, en))
        assertEquals("735 mmHg", Formats.pressure(980.0, PressureUnit.MMHG, it))
    }

    /** The barometer's tendency carries its sign and its unit: a span, converted without
     * an offset, two decimals in inches where one would be the whole swing. */
    @Test
    fun `a change of pressure is signed, in the reader's unit and mark`() {
        assertEquals("+2.4 hPa", Formats.pressureChange(2.4, PressureUnit.HPA, en))
        assertEquals("-2,4 hPa", Formats.pressureChange(-2.4, PressureUnit.HPA, it))
        assertEquals("+0.07 inHg", Formats.pressureChange(2.4, PressureUnit.INHG, en))
        assertEquals("-0,07 inHg", Formats.pressureChange(-2.4, PressureUnit.INHG, it))
        assertEquals("+1.8 mmHg", Formats.pressureChange(2.4, PressureUnit.MMHG, en))
        assertEquals("-1,8 mmHg", Formats.pressureChange(-2.4, PressureUnit.MMHG, it))
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
        // Beside a row of days an hour says it is one: "17:00", never a bare "17".
        assertEquals("17:00", Formats.hourClockLabel(afternoon, is24Hour = true, en))
        assertEquals("5 PM", Formats.hourClockLabel(afternoon, is24Hour = false, en).uppercase(en))
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

    /** The bar's middle form of the place's day (15 ott 2026): the date kept, the words short. */
    @Test
    fun `the medium day keeps the date and abbreviates the words`() {
        val day = LocalDate.of(2026, 9, 30)
        // en-GB's short September is «Sep» or «Sept» depending on the JDK's CLDR: either way
        // the weekday, the date and the month, in that order.
        assertTrue(Formats.dayMedium(day, en).startsWith("Wed 30 Sep"))
        assertEquals("Mer 30 set", Formats.dayMedium(day, it))
    }
}

