package com.callbackdev.chiaro.domain.rules

import com.callbackdev.chiaro.domain.model.Coordinates
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleWindowTest {

    private val milan = Coordinates(45.4642, 9.19)
    private val rome: ZoneId = ZoneId.of("Europe/Rome")
    private val tromso = Coordinates(69.6492, 18.9553)
    private val oslo: ZoneId = ZoneId.of("Europe/Oslo")

    private fun hours(vararg bands: Pair<String, String>) = RuleWindow(
        RuleWindowKind.HOURS,
        bands.map { (from, to) -> RuleBand.of(LocalTime.parse(from), LocalTime.parse(to)) }
    )

    private fun at(text: String): LocalDateTime = LocalDateTime.parse(text)

    private fun occurrence(window: RuleWindow, now: String, zone: ZoneId = rome, coords: Coordinates = milan) =
        RuleWindows.occurrence(window, at(now), zone, coords)

    // --- the three kinds ---

    @Test
    fun `any hour is always open, with no key of its own`() {
        assertEquals("", occurrence(RuleWindow.Always, "2026-09-26T03:00"))
        assertEquals("", occurrence(RuleWindow.Always, "2026-09-26T21:47"))
    }

    @Test
    fun `a band is open from its start up to, not including, its end`() {
        val commute = hours("07:00" to "09:00", "17:00" to "19:30")
        assertNull(occurrence(commute, "2026-09-26T06:59"))
        assertEquals("2026-09-26:H420-540", occurrence(commute, "2026-09-26T07:00"))
        assertEquals("2026-09-26:H420-540", occurrence(commute, "2026-09-26T08:59"))
        assertNull(occurrence(commute, "2026-09-26T09:00"))
        assertEquals("2026-09-26:H1020-1170", occurrence(commute, "2026-09-26T19:29"))
        // The evening that started it all: 21:47 is in neither band.
        assertNull(occurrence(commute, "2026-09-26T21:47"))
    }

    @Test
    fun `a band across midnight is one occurrence on both sides of it`() {
        val night = hours("22:00" to "02:00")
        assertEquals("2026-09-26:H1320-120", occurrence(night, "2026-09-26T23:30"))
        assertEquals("2026-09-26:H1320-120", occurrence(night, "2026-09-27T01:30"))
        assertNull(occurrence(night, "2026-09-27T02:00"))
        assertNull(occurrence(night, "2026-09-26T12:00"))
    }

    @Test
    fun `no usable band reads as any hour, never as a rule silenced for ever`() {
        assertEquals("", occurrence(RuleWindow(RuleWindowKind.HOURS), "2026-09-26T12:00"))
        assertEquals("", occurrence(hours("08:00" to "08:00"), "2026-09-26T12:00"))
    }

    @Test
    fun `daylight follows the sun at the place, and the season with it`() {
        // Milan, 26 Sep 2026: sunrise about 07:13, sunset about 19:12.
        assertNull(occurrence(RuleWindow.Daylight, "2026-09-26T06:50"))
        assertEquals("2026-09-26:LIGHT", occurrence(RuleWindow.Daylight, "2026-09-26T07:30"))
        assertEquals("2026-09-26:LIGHT", occurrence(RuleWindow.Daylight, "2026-09-26T19:00"))
        assertNull(occurrence(RuleWindow.Daylight, "2026-09-26T21:47"))
        // December closes before five, June is still open at nine.
        assertNull(occurrence(RuleWindow.Daylight, "2026-12-21T17:00"))
        assertEquals("2026-06-21:LIGHT", occurrence(RuleWindow.Daylight, "2026-06-21T21:00"))
    }

    @Test
    fun `the polar day is open all night and the polar night closed all day`() {
        assertEquals(
            "2026-06-21:LIGHT",
            occurrence(RuleWindow.Daylight, "2026-06-21T01:00", oslo, tromso)
        )
        assertNull(occurrence(RuleWindow.Daylight, "2026-12-21T12:00", oslo, tromso))
    }

    // --- when it opens next («Prova adesso») ---

    @Test
    fun `the next opening of closed bands is the nearest start, tomorrow included`() {
        val commute = hours("07:00" to "09:00", "17:00" to "19:30")
        fun next(now: String) = RuleWindows.nextOpening(commute, at(now), rome, milan)
        assertEquals(at("2026-09-26T17:00"), next("2026-09-26T12:00"))
        assertEquals(at("2026-09-27T07:00"), next("2026-09-26T21:47"))
        assertNull(next("2026-09-26T08:00")) // open: nothing to wait for
    }

    @Test
    fun `the next opening of daylight is the next sunrise`() {
        val next = RuleWindows.nextOpening(RuleWindow.Daylight, at("2026-09-26T21:47"), rome, milan)!!
        assertEquals(at("2026-09-27T07:00").toLocalDate(), next.toLocalDate())
        assertEquals(7, next.hour)
        // Before dawn it is today's sunrise, not tomorrow's.
        val early = RuleWindows.nextOpening(RuleWindow.Daylight, at("2026-09-26T05:00"), rome, milan)!!
        assertEquals(at("2026-09-26T07:00").toLocalDate(), early.toLocalDate())
    }

    @Test
    fun `the polar night has no next sunrise to promise`() {
        assertNull(
            RuleWindows.nextOpening(RuleWindow.Daylight, at("2026-12-21T12:00"), oslo, tromso)
        )
    }

    // --- the saved file ---

    @Test
    fun `a rule saved before the window existed reads as any hour`() {
        val json = Json { ignoreUnknownKeys = true }
        val old = """[{"id":3,"name":"Bici","enabled":true,""" +
            """"conditions":[{"variable":"current.temp_c","op":"GTE","threshold":12.0}],""" +
            """"message":"Si pedala"}]"""
        val rule = json.decodeFromString<List<NotificationRule>>(old).single()
        assertTrue(rule.window.always)
        // And a window survives the round trip, bands kept under another kind.
        val kept = rule.copy(window = RuleWindow(RuleWindowKind.DAYLIGHT, listOf(RuleBand(420, 540))))
        val back = json.decodeFromString<List<NotificationRule>>(json.encodeToString(listOf(kept)))
        assertEquals(kept, back.single())
        assertFalse(back.single().window.always)
    }
}
