package com.callbackdev.chiaro.ui.alerts

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The place line of a bound rule's card (15.12, 29 Oct 2026): «A Bormio», and the euphonic d
 * Italian keeps before the same vowel, «Ad Aosta», whatever the case or the accent of the a;
 * any other vowel keeps the bare preposition («A Empoli», «A Imola»).
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "it")
class RulePlaceTextTest {

    private val res get() = ApplicationProvider.getApplicationContext<Context>().resources

    @Test
    fun `a place takes A, and Ad before an a`() {
        assertEquals("A Bormio", RuleText.placeAt(res, "Bormio"))
        assertEquals("Ad Aosta", RuleText.placeAt(res, "Aosta"))
        assertEquals("Ad Ancona", RuleText.placeAt(res, "Ancona"))
        assertEquals("Ad Àrbatax", RuleText.placeAt(res, "Àrbatax"))
        assertEquals("A Empoli", RuleText.placeAt(res, "Empoli"))
        assertEquals("A Imola", RuleText.placeAt(res, "Imola"))
    }

    @Test
    @Config(qualifiers = "en")
    fun `English says In either way`() {
        assertEquals("In Bormio", RuleText.placeAt(res, "Bormio"))
        assertEquals("In Aosta", RuleText.placeAt(res, "Aosta"))
    }
}
