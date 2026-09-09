package com.callbackdev.chiaro.widget.arc

import android.content.Context
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.data.AppPalette
import com.callbackdev.chiaro.data.WeatherIcons
import com.callbackdev.chiaro.domain.model.MoonPhase
import com.callbackdev.chiaro.ui.icons.ChiaroIcons
import com.callbackdev.chiaro.ui.today.TimelineItem
import com.callbackdev.chiaro.ui.today.TimelineKind
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * The arc's sentences (9 set 2026, after the first device pass): the countdown is coarse
 * on purpose, the rainbow row is two facts rather than a line of prose, and the moon's
 * rows draw a whole moon.
 */
@RunWith(RobolectricTestRunner::class)
class ArcTextTest {

    private val context: Context get() = RuntimeEnvironment.getApplication()
    private val zone = ZoneId.of("Europe/Rome")
    private val now: Instant = LocalDateTime.of(2026, 9, 9, 9, 17).atZone(zone).toInstant()

    @Test
    fun `the countdown rounds to the hour, because the card repaints by the hour`() {
        // 7 h 43 min → «in 8 h»; 7 h 20 → «in 7 h»; exactly 1 h → «in 1 h».
        assertEquals(
            context.getString(R.string.arc_in_hours, 8),
            ArcText.countdown(context, now, now.plus(Duration.ofMinutes(7 * 60 + 43)))
        )
        assertEquals(
            context.getString(R.string.arc_in_hours, 7),
            ArcText.countdown(context, now, now.plus(Duration.ofMinutes(7 * 60 + 20)))
        )
        assertEquals(
            context.getString(R.string.arc_in_hours, 1),
            ArcText.countdown(context, now, now.plus(Duration.ofHours(1)))
        )
        // Under an hour it says only that; at hand, that.
        assertEquals(
            context.getString(R.string.arc_within_hour),
            ArcText.countdown(context, now, now.plus(Duration.ofMinutes(43)))
        )
        assertEquals(
            context.getString(R.string.arc_in_moments),
            ArcText.countdown(context, now, now.plus(Duration.ofSeconds(30)))
        )
    }

    @Test
    fun `the rainbow row is the chance and where to look, not the screen's sentence`() {
        val item = TimelineItem(LocalDateTime.of(2026, 9, 9, 17, 0), TimelineKind.RAINBOW, pct = 88, bearingDeg = 270.0)
        val row = ArcText.rowLabel(context, item)
        assertTrue(row, row.contains("88%"))
        assertTrue(row, row.contains(context.getString(R.string.compass_w)))
        assertFalse(row, row.contains(":"))
        // The hero keeps the short name and no figure.
        assertEquals(context.getString(R.string.arc_rainbow_short), ArcText.heroLabel(context, item))
    }

    @Test
    fun `the moon's rows draw the real phase, the sun's their own glyphs`() {
        // 9 Sep 2026, two days before the new moon of the 11th: the new moon's glyph, in
        // the card's family and for its ground — and the full disc a fortnight later.
        val nearNew = LocalDateTime.of(2026, 9, 9, 19, 0).atZone(zone).toInstant()
        assertEquals(MoonPhase.NEW_MOON, MoonPhase.at(nearNew))
        assertEquals(
            ChiaroIcons.moonPhaseRes(MoonPhase.NEW_MOON, WeatherIcons.LINE, true, AppPalette.VIVID),
            ArcText.rowIconRes(TimelineKind.MOONSET, nearNew, WeatherIcons.LINE, true, AppPalette.VIVID)
        )
        val nearFull = LocalDateTime.of(2026, 9, 26, 21, 0).atZone(zone).toInstant()
        assertEquals(MoonPhase.FULL_MOON, MoonPhase.at(nearFull))
        assertEquals(
            ChiaroIcons.moonPhaseRes(MoonPhase.FULL_MOON, WeatherIcons.FILL, true, AppPalette.VIVID),
            ArcText.rowIconRes(TimelineKind.MOONRISE, nearFull, WeatherIcons.FILL, true, AppPalette.VIVID)
        )
        assertEquals(
            R.drawable.mcn_sunset,
            ArcText.rowIconRes(TimelineKind.SUNSET, nearNew, WeatherIcons.LINE, true, AppPalette.VIVID)
        )
        assertEquals(
            R.drawable.mc_sunset,
            ArcText.rowIconRes(TimelineKind.SUNSET, nearNew, WeatherIcons.LINE, false, AppPalette.VIVID)
        )
    }

    /** Every glyph a row can show, in every family on every ground, and every phase of the
     * moon: a missing sibling in ChiaroIcons' tables would be a crash the moment a reader
     * picks filled icons. */
    @Test
    fun `every agenda glyph exists in every family and on both grounds`() {
        val start = LocalDateTime.of(2026, 9, 9, 12, 0).atZone(zone).toInstant()
        // Thirty days, one a day: every eighth of the moon's cycle is visited.
        val days = (0 until 30).map { start.plus(Duration.ofDays(it.toLong())) }
        TimelineKind.entries.forEach { kind ->
            WeatherIcons.entries.forEach { style ->
                listOf(true, false).forEach { dark ->
                    AppPalette.entries.forEach { palette ->
                        days.forEach { at ->
                            assertTrue(
                                "$kind $style dark=$dark $palette $at",
                                ArcText.rowIconRes(kind, at, style, dark, palette) != 0
                            )
                        }
                    }
                }
            }
        }
        assertEquals(MoonPhase.entries.toSet(), days.map { MoonPhase.at(it) }.toSet())
    }

    @Test
    fun `tomorrow's moments say so before their name`() {
        val sunrise = TimelineItem(LocalDateTime.of(2026, 9, 10, 6, 50), TimelineKind.SUNRISE)
        val event = ArcEvent(sunrise, sunrise.at.atZone(zone).toInstant(), tomorrow = true, verdict = null)
        assertEquals(
            context.getString(R.string.arc_tomorrow_name, context.getString(R.string.tl_sunrise)),
            ArcText.heroName(context, event)
        )
        assertEquals(
            context.getString(R.string.arc_next_tomorrow_at, context.getString(R.string.tl_sunrise), ArcText.clock(context, event.at, zone)),
            ArcText.heroSentence(context, event, zone)
        )
    }
}
