package com.callbackdev.chiaro.widget.arc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The arc widget's settings on their way to disk and back (9 set 2026). The codec is
 * pure so this can run without a DataStore; what it pins is that every field survives
 * the round trip, that an empty store is the defaults, and that a value the store holds
 * from another version falls back to its default rather than failing the card.
 */
class ArcSettingsTest {

    private val everythingFlipped = ArcSettings(
        span = ArcSpan.AHEAD,
        ground = ArcGround.NONE,
        sunPath = false,
        moon = false,
        rain = false,
        hourLabels = false,
        temperatures = false,
        nowMarker = false,
        fadePast = false,
        hero = ArcHero.HEADLINE,
        dialFigure = ArcDialFigure.NEXT_TIME,
        agendaSun = false,
        agendaMoon = false,
        agendaRain = false,
        agendaVerdicts = false,
        week = false,
        warning = false,
        density = ArcDensity.COMPACT
    )

    @Test
    fun `a fresh widget draws the whole picture`() {
        val defaults = ArcSettings()
        assertEquals(ArcSpan.TODAY, defaults.span)
        assertEquals(ArcGround.BANDS, defaults.ground)
        assertTrue(defaults.sunPath && defaults.moon && defaults.rain)
        assertTrue(defaults.hourLabels && defaults.temperatures && defaults.nowMarker && defaults.fadePast)
        assertEquals(ArcHero.NEXT_MOMENT, defaults.hero)
        assertEquals(ArcDialFigure.TEMPERATURE, defaults.dialFigure)
        assertTrue(defaults.agendaSun && defaults.agendaMoon && defaults.agendaRain && defaults.agendaVerdicts)
        assertTrue(defaults.week)
        // The official warning's chip is on by default (Fase 11): on a quiet day it
        // draws nothing, and on the other kind it is the line worth keeping.
        assertTrue(defaults.warning)
        assertEquals(ArcDensity.COMFORTABLE, defaults.density)
        assertEquals(1f, defaults.textScale)
    }

    @Test
    fun `every field survives the round trip`() {
        assertEquals(everythingFlipped, ArcSettingsCodec.decode(ArcSettingsCodec.encode(everythingFlipped)))
        assertEquals(ArcSettings(), ArcSettingsCodec.decode(ArcSettingsCodec.encode(ArcSettings())))
    }

    @Test
    fun `the codec writes exactly the keys it reads`() {
        assertEquals(ArcSettingsCodec.keys.toSet(), ArcSettingsCodec.encode(ArcSettings()).keys)
        assertEquals(18, ArcSettingsCodec.keys.size)
    }

    @Test
    fun `an empty store is the defaults`() {
        assertEquals(ArcSettings(), ArcSettingsCodec.decode(emptyMap()))
    }

    @Test
    fun `a value from another version falls back on its own, not the whole card`() {
        val decoded = ArcSettingsCodec.decode(
            mapOf(
                ArcSettingsCodec.SPAN to "NEXT_WEEK",
                ArcSettingsCodec.GROUND to "NONE",
                ArcSettingsCodec.RAIN to "maybe",
                ArcSettingsCodec.MOON to "false"
            )
        )
        assertEquals(ArcSpan.TODAY, decoded.span)
        assertEquals(ArcGround.NONE, decoded.ground)
        assertTrue(decoded.rain)
        assertEquals(false, decoded.moon)
    }

    @Test
    fun `compact sets the words at ninety percent`() {
        assertEquals(0.9f, ArcSettings(density = ArcDensity.COMPACT).textScale)
    }
}
