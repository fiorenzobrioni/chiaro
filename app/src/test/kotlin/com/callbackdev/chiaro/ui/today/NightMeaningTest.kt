package com.callbackdev.chiaro.ui.today

import com.callbackdev.chiaro.R
import org.junit.Assert.assertEquals
import org.junit.Test

/** The night bands of the evening summary: each one is a decision taken tonight. */
class NightMeaningTest {

    @Test
    fun `night bands switch at 0 - 5 - 13 - 16 - 21 degrees`() {
        val bands = listOf(
            -3.0 to R.string.night_meaning_freezing,
            0.0 to R.string.night_meaning_freezing,
            0.1 to R.string.night_meaning_cold,
            4.9 to R.string.night_meaning_cold,
            5.0 to R.string.night_meaning_cool,
            12.9 to R.string.night_meaning_cool,
            13.0 to R.string.night_meaning_ajar,
            15.9 to R.string.night_meaning_ajar,
            16.0 to R.string.night_meaning_mild,
            20.9 to R.string.night_meaning_mild,
            21.0 to R.string.night_meaning_warm,
        )
        bands.forEach { (lowC, expected) ->
            assertEquals("low $lowC °C", expected, WeatherText.nightMeaning(lowC))
        }
    }

    @Test
    fun `a 14 degree low leaves the window ajar, not open`() {
        // The night of 26 set 2026 in Cavenago di Brianza: 16° at midnight, 14° from
        // three to seven. The summary said to sleep with the window open.
        assertEquals(R.string.night_meaning_ajar, WeatherText.nightMeaning(14.0))
    }
}
