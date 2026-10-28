package com.callbackdev.chiaro.ui.alerts

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.callbackdev.chiaro.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * «Scattato l'ultima volta» in the bulletin's grammar (15.7, 28 Oct 2026; the Alerts
 * review's point 11): the day only when it is not today, as `WarningText.issued` says a
 * bulletin's hour, so one card does not say a time two ways. Until then the line printed
 * «27 ott, 07:12» under a bulletin «di ieri alle 14:34».
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "it")
class RuleFiredTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val res get() = context.resources
    private val today: LocalDate = LocalDate.of(2026, 10, 28)
    private val timeFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("H:mm", Locale.ITALIAN)
    private val dateFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.ITALIAN)

    @Test
    fun `fired today says the hour and the day's own word`() {
        assertEquals(
            "oggi alle 7:12",
            RuleText.firedAt(res, today.atTime(7, 12), today, timeFmt, dateFmt)
        )
    }

    @Test
    fun `fired yesterday says so`() {
        assertEquals(
            "ieri alle 7:12",
            RuleText.firedAt(res, today.minusDays(1).atTime(7, 12), today, timeFmt, dateFmt)
        )
    }

    /** Beyond yesterday the day is named, day and month: the history keeps three days, so
     * the year is never in doubt. «Il giorno», not «il», so that the article never touches
     * the digit (`ItalianArticleTest`: «il 8 ott» would want «l'8»). */
    @Test
    fun `fired before yesterday carries its day`() {
        val phrase = RuleText.firedAt(res, today.minusDays(3).atTime(7, 12), today, timeFmt, dateFmt)
        assertTrue(phrase, phrase.startsWith("il giorno 25 ott"))
        assertTrue(phrase, phrase.endsWith(" alle 7:12"))
    }

    @Test
    fun `the card's line reads as one sentence`() {
        val fired = RuleText.firedAt(res, today.minusDays(1).atTime(7, 12), today, timeFmt, dateFmt)
        assertEquals(
            "Scattato l'ultima volta ieri alle 7:12",
            res.getString(R.string.rule_last_fired, fired)
        )
    }
}
