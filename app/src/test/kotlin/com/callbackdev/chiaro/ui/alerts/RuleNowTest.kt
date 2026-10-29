package com.callbackdev.chiaro.ui.alerts

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.domain.rules.RuleCondition
import com.callbackdev.chiaro.domain.rules.RuleOp
import com.callbackdev.chiaro.domain.settings.UnitSettings
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A rule's state now, in words (the Alerts review, points 11 and 12, 14 ott 2026): the word
 * first, then the number that decides beside the threshold it missed, or when it may speak.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "it")
class RuleNowTest {

    private val res = ApplicationProvider.getApplicationContext<Context>().resources
    private val units = UnitSettings()
    private val clock = DateTimeFormatter.ofPattern("H:mm")
    private val today = LocalDate.of(2026, 10, 14)

    private fun text(preview: RulePreview, named: Boolean = true) =
        ruleNow(res, preview, units, clock, Locale.ITALY, named)

    @Test
    fun `a rule that holds says so in two words`() {
        val now = text(RulePreview.WouldFire("Si pedala"))!!
        assertEquals(true, now.holds)
        assertEquals("Adesso sì", now.text)
    }

    @Test
    fun `a no names the condition, what it read and what it needs`() {
        val rain = RuleCondition("next_6h.precip_chance_max", RuleOp.LTE, 20.0)
        val now = text(RulePreview.WouldPass(rain, 35.0))!!
        assertEquals(false, now.holds)
        assertEquals("Adesso no · la pioggia nelle prossime 6 ore è 35%, serve al massimo 20%", now.text)
        assertEquals("Adesso no", now.word)
    }

    @Test
    fun `a reading keeps the reader's decimal mark`() {
        val cold = RuleCondition("current.temp_c", RuleOp.GTE, 5.0)
        assertEquals(
            "Adesso no · la temperatura adesso è 3,4°, serve almeno 5°",
            text(RulePreview.WouldPass(cold, 3.4))!!.text
        )
    }

    @Test
    fun `a rule on one quantity does not name it twice`() {
        val cold = RuleCondition("current.temp_c", RuleOp.LT, 5.0)
        assertEquals(
            "Adesso no · è 19,7°, serve sotto 5°",
            text(RulePreview.WouldPass(cold, 19.7), named = false)!!.text
        )
        val single = com.callbackdev.chiaro.domain.rules.NotificationRule(1, "", conditions = listOf(cold), message = "")
        val range = single.copy(conditions = listOf(cold, cold.copy(op = RuleOp.GT, threshold = 0.0)))
        val two = RuleText.templates.single { it.titleRes == R.string.tpl_run_title }.let { single.copy(conditions = it.conditions) }
        assertEquals(false, watchesSeveral(single))
        assertEquals(false, watchesSeveral(range))
        assertEquals(true, watchesSeveral(two))
    }

    @Test
    fun `outside its hours it says until when`() {
        val tomorrow = RulePreview.OutsideHours("m", LocalDateTime.of(today.plusDays(1), java.time.LocalTime.of(7, 12)), today)
        assertEquals("Adesso sì, ma fuori orario fino a domani alle 7:12", text(tomorrow)!!.text)
        val later = RulePreview.OutsideHours("m", LocalDateTime.of(today, java.time.LocalTime.of(17, 0)), today)
        assertEquals("Adesso sì, ma fuori orario fino alle 17:00", text(later)!!.text)
        assertEquals("Adesso sì, ma fuori dai suoi orari", text(RulePreview.OutsideHours("m", null, today))!!.text)
    }

    /** A fact about tomorrow in the morning (15.9, 29 Oct 2026): it holds, and its voice comes
     * at noon; «Adesso sì» alone promised a sound at nine. */
    @Test
    fun `a fact about a day before its hour says when it may ring`() {
        val noon = RulePreview.NotYet("m", LocalDateTime.of(today, java.time.LocalTime.NOON))
        val now = text(noon)!!
        assertEquals(true, now.holds)
        assertEquals("Adesso sì", now.word)
        assertEquals("Adesso sì, può suonare dalle 12:00", now.text)
    }

    @Test
    fun `what cannot be read is said, and no forecast says nothing`() {
        val now = text(RulePreview.Unavailable("current.aqi_eu_index"))!!
        assertNull(now.holds)
        assertEquals("Adesso non si può dire · l'indice europeo dell'aria adesso non ha un valore", now.text)
        assertNull(text(RulePreview.NoData))
    }
}
