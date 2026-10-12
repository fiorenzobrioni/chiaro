package com.callbackdev.chiaro.ui.today

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.callbackdev.chiaro.domain.settings.UnitSettings
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The quiet day's sentence in words (12 ott 2026): the dry spell and the warmth, each
 * alone or both, and nothing at all in the widgets' brief register.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "it")
class CalmTextTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val timeFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val sunday: LocalDate = LocalDate.of(2026, 9, 27)

    private fun text(calm: Headline.Calm, brief: Boolean = false) =
        HeadlineText.of(context, calm, timeFmt, UnitSettings(), brief = brief)

    private val peak = Headline.Calm.Warmth.Peak(LocalDateTime.of(2026, 9, 25, 16, 0), 22.7)

    @Test
    fun `the spell and the peak make one sentence`() {
        val calm = Headline.Calm(Headline.Calm.Dry.Until(sunday, 2, Headline.Falling.RAIN), peak)
        assertEquals("Prossima pioggia domenica, massima 23° verso le 16:00", text(calm))
    }

    @Test
    fun `tomorrow is tomorrow and snow is snow`() {
        val calm = Headline.Calm(Headline.Calm.Dry.Until(sunday, 1, Headline.Falling.SNOW), null)
        assertEquals("Prossima neve domani", text(calm))
    }

    @Test
    fun `a whole dry forecast and tomorrow's range`() {
        val calm = Headline.Calm(Headline.Calm.Dry.Days(7), Headline.Calm.Warmth.Tomorrow(14.1, 25.9))
        assertEquals("Asciutto per i prossimi 7 giorni, domani tra 14° e 26°", text(calm))
    }

    @Test
    fun `the warmth alone keeps its capital`() {
        assertEquals("Massima 23° verso le 16:00", text(Headline.Calm(null, peak)))
    }

    @Test
    fun `the widgets' brief register leaves a quiet day to its own fallback`() {
        assertNull(text(Headline.Calm(Headline.Calm.Dry.Days(7), peak), brief = true))
    }
}
