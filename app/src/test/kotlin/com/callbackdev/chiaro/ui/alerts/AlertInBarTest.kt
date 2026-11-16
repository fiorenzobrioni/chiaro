package com.callbackdev.chiaro.ui.alerts

import com.callbackdev.chiaro.data.warnings.PlaceWarningState
import com.callbackdev.chiaro.domain.warnings.PlaceWarnings
import com.callbackdev.chiaro.domain.warnings.WarningHazard
import com.callbackdev.chiaro.domain.warnings.WarningLevel
import com.callbackdev.chiaro.domain.warnings.WarningZone
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * What Alerts' bar carries once the hero's answer has scrolled away (16 nov 2026): the level
 * of a bulletin that warns of something, and nothing in every other state; and the line the
 * answer crosses to be «gone», its middle against the list's top edge.
 */
class AlertInBarTest {

    private val today: LocalDate = LocalDate.of(2026, 11, 16)
    private val milano = WarningZone("Lomb-09", "Nodo Idraulico di Milano", "Lombardia")

    private fun bulletin(today: WarningLevel, tomorrow: WarningLevel = WarningLevel.NONE) = PlaceWarnings(
        zone = milano,
        bulletinId = "DPC_BULLETIN_2026_11_16",
        issuedAt = this.today.atTime(15, 12),
        days = listOf(
            PlaceWarnings.DayWarnings(this.today, levels(WarningHazard.THUNDERSTORM, today)),
            PlaceWarnings.DayWarnings(this.today.plusDays(1), levels(WarningHazard.HYDRAULIC, tomorrow))
        ),
        note = null
    )

    /** Every hazard graded, as a bulletin grades them: [level] for [hazard], none for the rest. */
    private fun levels(hazard: WarningHazard, level: WarningLevel) =
        WarningHazard.entries.associateWith { if (it == hazard) level else WarningLevel.NONE }

    @Test
    fun `a bulletin that warns gives the bar its highest level`() {
        assertEquals(WarningLevel.YELLOW, warningInBar(PlaceWarningState.Current(bulletin(WarningLevel.YELLOW))))
        assertEquals(
            "tomorrow's orange is the answer the hero gives, and the bar's",
            WarningLevel.ORANGE,
            warningInBar(PlaceWarningState.Current(bulletin(WarningLevel.YELLOW, WarningLevel.ORANGE)))
        )
    }

    @Test
    fun `a quiet day puts nothing in the bar, and neither does a bulletin that is not there`() {
        assertNull("an all-clear is not printed", warningInBar(PlaceWarningState.Current(bulletin(WarningLevel.NONE))))
        assertNull(warningInBar(PlaceWarningState.Unavailable))
        assertNull(warningInBar(PlaceWarningState.Waiting(milano)))
        assertNull(
            "yesterday's grades are not an answer",
            warningInBar(PlaceWarningState.Stale(milano, today.minusDays(1).atTime(15, 0)))
        )
    }

    @Test
    fun `the answer is gone once its middle is over the list's top edge`() {
        // The list starts 200 px down the page; the answer is 300 px tall.
        assertFalse("at rest, 16 px under the bar", answerUnderBar(answerTop = 216f, answerHeight = 300, listTop = 200f))
        assertFalse("half of it still shows", answerUnderBar(answerTop = 51f, answerHeight = 300, listTop = 200f))
        assertTrue("its middle has crossed", answerUnderBar(answerTop = 49f, answerHeight = 300, listTop = 200f))
        assertTrue("scrolled well away", answerUnderBar(answerTop = -900f, answerHeight = 300, listTop = 200f))
    }
}
