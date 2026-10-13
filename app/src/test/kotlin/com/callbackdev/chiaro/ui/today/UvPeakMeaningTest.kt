package com.callbackdev.chiaro.ui.today

import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.domain.WmoCode
import com.callbackdev.chiaro.domain.model.WeatherCondition
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * A day's peak UV under the day its row draws (13 ott 2026): on a wet day the brightest
 * hour is the sunny spell, and the line says so; the bands and the number do not move.
 */
class UvPeakMeaningTest {

    private fun day(code: WmoCode) = WeatherCondition(code.code)

    @Test
    fun `a dry day keeps the plain line, overcast included`() {
        // Overcast can be a high veil, which burns as if it were not there.
        for (code in listOf(WmoCode.CLEAR, WmoCode.PARTLY_CLOUDY, WmoCode.OVERCAST, WmoCode.FOG)) {
            assertEquals(code.name, R.string.uv_meaning_moderate, WeatherText.uvPeakMeaning(4, day(code)))
            assertEquals(code.name, WeatherText.uvMeaning(8), WeatherText.uvPeakMeaning(8, day(code)))
        }
    }

    @Test
    fun `a wet day puts the burn time in the sunny spells, band by band`() {
        val rain = day(WmoCode.DRIZZLE_LIGHT)
        assertEquals(
            listOf(
                R.string.uv_meaning_moderate_spells, R.string.uv_meaning_moderate_spells,
                R.string.uv_meaning_high_spells, R.string.uv_meaning_very_high_spells,
                R.string.uv_meaning_extreme_spells
            ),
            listOf(3, 5, 6, 8, 11).map { WeatherText.uvPeakMeaning(it, rain) }
        )
    }

    @Test
    fun `a low peak needs no protection, wet or dry`() {
        assertEquals(R.string.uv_meaning_low, WeatherText.uvPeakMeaning(2, day(WmoCode.RAIN_MODERATE)))
    }

    @Test
    fun `wet is what falls and what threatens, never a code nobody knows`() {
        for (code in listOf(WmoCode.SHOWERS_SLIGHT, WmoCode.SNOW_SLIGHT, WmoCode.THUNDERSTORM_POSSIBLE)) {
            assertEquals(code.name, R.string.uv_meaning_moderate_spells, WeatherText.uvPeakMeaning(4, day(code)))
        }
        // A likely rain is a possibility the mapper never gives a whole day; were it to,
        // it is the sky it votes with, not a wet day.
        assertEquals(R.string.uv_meaning_moderate, WeatherText.uvPeakMeaning(4, day(WmoCode.RAIN_LIKELY)))
        assertEquals(R.string.uv_meaning_moderate, WeatherText.uvPeakMeaning(4, WeatherCondition(42)))
    }
}
