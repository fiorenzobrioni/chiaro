package com.callbackdev.chiaro.ui.alerts

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.domain.rules.RuleCondition
import com.callbackdev.chiaro.domain.rules.RuleOp
import com.callbackdev.chiaro.domain.settings.UnitSettings
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A rule's sentence on its card (the Alerts review, point 6, 14 ott 2026): two bounds on one
 * quantity are one phrase, and the thresholds are the pieces the card sets in full weight.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "it")
class RuleSentenceTest {

    private val res = ApplicationProvider.getApplicationContext<Context>().resources
    private val units = UnitSettings()

    /** An idea's conditions, by its title: the ideas' order is the sheet's, not this test's. */
    private fun idea(titleRes: Int) = RuleText.templates.single { it.titleRes == titleRes }.conditions

    private fun text(conditions: List<RuleCondition>) =
        RuleText.sentenceParts(res, conditions, units).joinToString("") { it.text }

    private fun values(conditions: List<RuleCondition>) =
        RuleText.sentenceParts(res, conditions, units).filter { it.value }.map { it.text }

    @Test
    fun `the run's two bounds are one range, said once`() {
        val run = idea(R.string.tpl_run_title)
        assertEquals(
            "la pioggia nelle prossime 6 ore al massimo 20% e la temperatura adesso fra 5° e 26°",
            text(run)
        )
        assertEquals(listOf("20%", "5°", "26°"), values(run))
    }

    @Test
    fun `two strict bounds keep their words, with the quantity said once`() {
        val coldRun = idea(R.string.tpl_cold_run_title)
        assertEquals(
            "la pioggia nelle prossime 6 ore al massimo 20% e la temperatura adesso sopra 0° e sotto 5°",
            text(coldRun)
        )
    }

    @Test
    fun `bounds that leave nothing between them are not called a range`() {
        val never = listOf(
            RuleCondition("current.temp_c", RuleOp.GTE, 26.0),
            RuleCondition("current.temp_c", RuleOp.LTE, 5.0)
        )
        assertEquals("la temperatura adesso almeno 26° e al massimo 5°", text(never))
    }

    @Test
    fun `a rule with one condition per quantity reads as before`() {
        val bike = idea(R.string.tpl_bike_title)
        val before = bike.joinToString(" e ") { RuleText.sentence(res, it, units) }
        assertEquals(before, text(bike))
        assertEquals(listOf("12°", "20%"), values(bike))
    }
}
