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
import com.callbackdev.chiaro.ui.today.OpenDaySentence.Span
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The open day's sentence in words (31 ott 2026): the clauses, their joints, no full stop. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "it")
class OpenDaySentenceTextTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val timeFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val date = LocalDate.of(2026, 11, 3)
    private fun at(h: Int): LocalDateTime = date.atTime(h, 0)

    private val allDay = listOf(Part.MORNING, Part.AFTERNOON, Part.EVENING)
    private val overcast = listOf(Sky(WmoCode.OVERCAST.code, allDay))

    private fun rain(vararg spans: Span, register: Register = Register.FULL, scattered: Boolean = false) =
        Fall(ConditionWord.RAIN, WmoCode.Phase.LIQUID, register, spans.toList(), scattered)

    private fun text(sentence: Sentence?) = OpenDaySentenceText.of(context, sentence, timeFmt)

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
        assertEquals(
            "Coperto, pioggia dalle 17:00 alle 20:00",
            text(Sentence(overcast, listOf(rain(Span.Hours(at(17), at(20))))))
        )
    }

    @Test
    fun `one hour is around, two runs are joined, the day's edges are open`() {
        assertEquals("Coperto, pioggia verso le 15:00", text(Sentence(overcast, listOf(rain(Span.Hours(at(15), at(16)))))))
        assertEquals(
            "Coperto, pioggia fino alle 03:00 e dalle 22:00",
            text(Sentence(overcast, listOf(rain(Span.Hours(null, at(3)), Span.Hours(at(22), null)))))
        )
        assertEquals("Pioggia tutto il giorno", text(Sentence(emptyList(), listOf(rain(Span.Hours(null, null))))))
    }

    @Test
    fun `the light register is a few drops or a few flakes`() {
        val cloudy = listOf(Sky(WmoCode.PARTLY_CLOUDY.code, allDay))
        assertEquals(
            "Nuvoloso, qualche goccia verso le 15:00",
            text(Sentence(cloudy, listOf(rain(Span.Hours(at(15), at(16)), register = Register.LIGHT))))
        )
        val flakes = Fall(ConditionWord.SNOW_LIGHT, WmoCode.Phase.FROZEN, Register.LIGHT, listOf(Span.Parts(listOf(Part.MORNING))))
        assertEquals("Nuvoloso, qualche fiocco al mattino", text(Sentence(cloudy, listOf(flakes))))
    }

    @Test
    fun `the parts of the day as ranges`() {
        assertEquals(
            "Coperto, pioggia dal pomeriggio alla sera",
            text(Sentence(overcast, listOf(rain(Span.Parts(listOf(Part.AFTERNOON, Part.EVENING))))))
        )
        assertEquals(
            "Coperto, pioggia nella notte e in serata",
            text(Sentence(overcast, listOf(rain(Span.Parts(listOf(Part.NIGHT, Part.EVENING))))))
        )
        assertEquals(
            "Coperto, pioggia a tratti dal mattino alla sera",
            text(Sentence(overcast, listOf(rain(Span.Parts(allDay), scattered = true))))
        )
        assertEquals(
            "Pioggia tutto il giorno",
            text(Sentence(emptyList(), listOf(rain(Span.Parts(Part.entries.toList())))))
        )
    }

    @Test
    fun `the words are the states' own`() {
        val falls = listOf(
            Fall(ConditionWord.SNOW, WmoCode.Phase.FROZEN, Register.FULL, listOf(Span.Parts(listOf(Part.MORNING)))),
            Fall(ConditionWord.RAIN_LIKELY, WmoCode.Phase.LIQUID, Register.POSSIBLE, listOf(Span.Parts(listOf(Part.EVENING))))
        )
        assertEquals("Coperto, neve al mattino, pioggia possibile in serata", text(Sentence(overcast, falls)))
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
            "Clear in the morning, overcast from the afternoon, rain from 17:00 to 20:00",
            text(Sentence(sky, listOf(rain(Span.Hours(at(17), at(20))))))
        )
        assertEquals(
            "Overcast, a few drops around 15:00",
            text(Sentence(overcast, listOf(rain(Span.Hours(at(15), at(16)), register = Register.LIGHT))))
        )
        assertEquals(
            "Overcast, rain on and off from the morning into the evening",
            text(Sentence(overcast, listOf(rain(Span.Parts(allDay), scattered = true))))
        )
        assertEquals("Mostly clear all day", text(Sentence(listOf(Sky(WmoCode.MAINLY_CLEAR.code, allDay)), emptyList())))
    }
}
