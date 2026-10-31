package com.callbackdev.chiaro.ui.today

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.callbackdev.chiaro.domain.ConditionWord
import com.callbackdev.chiaro.domain.WmoCode
import com.callbackdev.chiaro.ui.today.OpenDaySentence.Fall
import com.callbackdev.chiaro.ui.today.OpenDaySentence.Part
import com.callbackdev.chiaro.ui.today.OpenDaySentence.Register
import com.callbackdev.chiaro.ui.today.OpenDaySentence.Sentence
import com.callbackdev.chiaro.ui.today.OpenDaySentence.Sky
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The open day's sentence in words (31 ott 2026): the clauses, their joints, the parts of the
 * day as ranges, no full stop. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "it")
class OpenDaySentenceTextTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val allDay = listOf(Part.MORNING, Part.AFTERNOON, Part.EVENING)
    private val overcast = listOf(Sky(WmoCode.OVERCAST.code, allDay))

    private fun rain(vararg parts: Part, register: Register = Register.FULL, scattered: Boolean = false) =
        Fall(ConditionWord.RAIN, WmoCode.Phase.LIQUID, register, parts.toList(), scattered)

    private fun text(sentence: Sentence?) = OpenDaySentenceText.of(context, sentence)

    @Test
    fun `a steady dry sky holds all day`() {
        assertEquals("Sereno tutto il giorno", text(Sentence(listOf(Sky(WmoCode.CLEAR.code, allDay)), emptyList())))
    }

    @Test
    fun `the sky's stretches, each by its parts`() {
        val sky = listOf(Sky(WmoCode.CLEAR.code, listOf(Part.MORNING)), Sky(WmoCode.OVERCAST.code, listOf(Part.AFTERNOON, Part.EVENING)))
        assertEquals("Sereno al mattino, coperto dal pomeriggio", text(Sentence(sky, emptyList())))
        val later = listOf(Sky(WmoCode.PARTLY_CLOUDY.code, listOf(Part.MORNING, Part.AFTERNOON)), Sky(WmoCode.OVERCAST.code, listOf(Part.EVENING)))
        assertEquals("Nuvoloso fino al pomeriggio, coperto in serata", text(Sentence(later, emptyList())))
        val three = listOf(
            Sky(WmoCode.FOG.code, listOf(Part.MORNING)),
            Sky(WmoCode.MAINLY_CLEAR.code, listOf(Part.AFTERNOON)),
            Sky(WmoCode.OVERCAST.code, listOf(Part.EVENING))
        )
        assertEquals("Nebbia al mattino, quasi sereno nel pomeriggio, coperto in serata", text(Sentence(three, emptyList())))
    }

    @Test
    fun `a steady sky is its bare word in front of what falls`() {
        assertEquals("Coperto, pioggia nel pomeriggio", text(Sentence(overcast, listOf(rain(Part.AFTERNOON)))))
    }

    @Test
    fun `the parts of the day as ranges`() {
        assertEquals(
            "Coperto, pioggia dal pomeriggio alla sera",
            text(Sentence(overcast, listOf(rain(Part.AFTERNOON, Part.EVENING))))
        )
        assertEquals(
            "Coperto, pioggia dalla notte al pomeriggio",
            text(Sentence(overcast, listOf(rain(Part.NIGHT, Part.MORNING, Part.AFTERNOON))))
        )
        assertEquals("Coperto, pioggia nella notte e in serata", text(Sentence(overcast, listOf(rain(Part.NIGHT, Part.EVENING)))))
        assertEquals(
            "Coperto, pioggia a tratti dal mattino alla sera",
            text(Sentence(overcast, listOf(rain(*allDay.toTypedArray(), scattered = true))))
        )
        assertEquals("Pioggia tutto il giorno", text(Sentence(emptyList(), listOf(rain(*Part.entries.toTypedArray())))))
    }

    @Test
    fun `the light register is a few drops or a few flakes`() {
        val cloudy = listOf(Sky(WmoCode.PARTLY_CLOUDY.code, allDay))
        assertEquals(
            "Nuvoloso, qualche goccia nel pomeriggio",
            text(Sentence(cloudy, listOf(rain(Part.AFTERNOON, register = Register.LIGHT))))
        )
        val flakes = Fall(ConditionWord.SNOW_LIGHT, WmoCode.Phase.FROZEN, Register.LIGHT, listOf(Part.MORNING))
        assertEquals("Nuvoloso, qualche fiocco al mattino", text(Sentence(cloudy, listOf(flakes))))
    }

    @Test
    fun `the words are the states' own`() {
        val falls = listOf(
            Fall(ConditionWord.SNOW, WmoCode.Phase.FROZEN, Register.FULL, listOf(Part.MORNING)),
            Fall(ConditionWord.RAIN_LIKELY, WmoCode.Phase.LIQUID, Register.POSSIBLE, listOf(Part.EVENING))
        )
        assertEquals("Coperto, neve al mattino, pioggia possibile in serata", text(Sentence(overcast, falls)))
    }

    @Test
    fun `past the third day what falls has no when`() {
        val untimed = { f: Fall -> f.copy(timed = false) }
        assertEquals("Coperto, pioggia", text(Sentence(overcast, listOf(untimed(rain(Part.AFTERNOON, Part.EVENING))))))
        assertEquals(
            "Coperto, pioggia a tratti",
            text(Sentence(overcast, listOf(untimed(rain(*allDay.toTypedArray(), scattered = true)))))
        )
        val cloudy = listOf(Sky(WmoCode.PARTLY_CLOUDY.code, allDay))
        assertEquals(
            "Nuvoloso, qualche goccia",
            text(Sentence(cloudy, listOf(untimed(rain(Part.AFTERNOON, register = Register.LIGHT)))))
        )
        val possible = Fall(ConditionWord.THUNDERSTORM_POSSIBLE, WmoCode.Phase.NONE, Register.POSSIBLE, listOf(Part.EVENING), timed = false)
        val sky = listOf(Sky(WmoCode.CLEAR.code, listOf(Part.MORNING)), Sky(WmoCode.OVERCAST.code, listOf(Part.AFTERNOON, Part.EVENING)))
        assertEquals("Sereno al mattino, coperto dal pomeriggio, temporali possibili", text(Sentence(sky, listOf(possible))))
    }

    @Test
    fun `nothing to say is null`() {
        assertNull(text(null))
        assertNull(text(Sentence(emptyList(), emptyList())))
    }

    @Test
    @Config(qualifiers = "en")
    fun `in English`() {
        val sky = listOf(Sky(WmoCode.CLEAR.code, listOf(Part.MORNING)), Sky(WmoCode.OVERCAST.code, listOf(Part.AFTERNOON, Part.EVENING)))
        assertEquals(
            "Clear in the morning, overcast from the afternoon, rain in the evening",
            text(Sentence(sky, listOf(rain(Part.EVENING))))
        )
        assertEquals(
            "Overcast, a few drops in the afternoon",
            text(Sentence(overcast, listOf(rain(Part.AFTERNOON, register = Register.LIGHT))))
        )
        assertEquals(
            "Overcast, rain on and off from the morning into the evening",
            text(Sentence(overcast, listOf(rain(*allDay.toTypedArray(), scattered = true))))
        )
        assertEquals(
            "Partly cloudy through the afternoon, overcast in the evening",
            text(Sentence(listOf(Sky(WmoCode.PARTLY_CLOUDY.code, listOf(Part.MORNING, Part.AFTERNOON)), Sky(WmoCode.OVERCAST.code, listOf(Part.EVENING))), emptyList()))
        )
        assertEquals("Mostly clear all day", text(Sentence(listOf(Sky(WmoCode.MAINLY_CLEAR.code, allDay)), emptyList())))
        assertEquals(
            "Overcast, rain on and off",
            text(Sentence(overcast, listOf(rain(*allDay.toTypedArray(), scattered = true).copy(timed = false))))
        )
    }
}
