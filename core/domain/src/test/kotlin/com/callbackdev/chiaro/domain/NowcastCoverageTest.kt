package com.callbackdev.chiaro.domain

import com.callbackdev.chiaro.domain.model.Coordinates
import com.callbackdev.chiaro.domain.model.QuarterHour
import java.time.LocalDateTime
import java.time.ZoneOffset
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Where the quarter is computed (17 ott 2026). The places and the series are the ones
 * measured that afternoon, `best_match` against ICON-D2, AROME and HRRR, 192 quarters each.
 */
class NowcastCoverageTest {

    @Test
    fun `northern Italy, the Alps and Germany are covered`() {
        assertTrue(NowcastCoverage.covers("IT", Coordinates(45.4642, 9.19))) // Milan
        assertTrue(NowcastCoverage.covers("IT", Coordinates(44.4056, 8.9463))) // Genoa
        assertTrue(NowcastCoverage.covers("IT", Coordinates(45.6495, 13.7768))) // Trieste
        assertTrue(NowcastCoverage.covers("IT", Coordinates(43.7696, 11.2558))) // Florence
        assertTrue(NowcastCoverage.covers("IT", Coordinates(43.6158, 13.5189))) // Ancona
        assertTrue(NowcastCoverage.covers("CH", Coordinates(47.3769, 8.5417))) // Zurich
        assertTrue(NowcastCoverage.covers("DE", Coordinates(48.1351, 11.582))) // Munich
        assertTrue(NowcastCoverage.covers("AT", Coordinates(48.2082, 16.3738))) // Vienna
        assertTrue(NowcastCoverage.covers("CZ", Coordinates(50.0755, 14.4378))) // Prague
        assertTrue(NowcastCoverage.covers("SI", Coordinates(46.0569, 14.5058))) // Ljubljana
        assertTrue(NowcastCoverage.covers("us", Coordinates(40.7128, -74.006))) // New York, any case
    }

    @Test
    fun `France, Croatia, Slovakia and western Poland are ICON-D2's too`() {
        // The provider's probe of 27 Oct 2026: best_match's quarters identical to ICON-D2's,
        // temperature and precipitation, at every one of these.
        assertTrue(NowcastCoverage.covers("FR", Coordinates(48.8566, 2.3522))) // Paris
        assertTrue(NowcastCoverage.covers("FR", Coordinates(45.764, 4.8357))) // Lyon
        assertTrue(NowcastCoverage.covers("FR", Coordinates(43.6047, 1.4442))) // Toulouse
        assertTrue(NowcastCoverage.covers("FR", Coordinates(44.8378, -0.5792))) // Bordeaux
        assertTrue(NowcastCoverage.covers("FR", Coordinates(43.7102, 7.262))) // Nice
        assertTrue(NowcastCoverage.covers("HR", Coordinates(45.815, 15.9819))) // Zagreb
        assertTrue(NowcastCoverage.covers("SK", Coordinates(48.1486, 17.1077))) // Bratislava
        assertTrue(NowcastCoverage.covers("PL", Coordinates(51.1079, 17.0385))) // Wrocław
    }

    @Test
    fun `inside the grid, a city the probe found on another model is not`() {
        // The same probe: Budapest and Warsaw are DMI HARMONIE's, Luxembourg KNMI's, Marseille
        // sits in the grid's relaxation band and matched nobody.
        assertFalse(NowcastCoverage.covers("HU", Coordinates(47.4979, 19.0402)))
        assertFalse(NowcastCoverage.covers("PL", Coordinates(52.2297, 21.0122)))
        assertFalse(NowcastCoverage.covers("LU", Coordinates(49.6116, 6.1319)))
        assertFalse(NowcastCoverage.covers("FR", Coordinates(43.2965, 5.3698)))
    }

    @Test
    fun `south of the grid, and its edge, are not`() {
        // ICON-D2's key is absent from the response at every one of these.
        assertFalse(NowcastCoverage.covers("IT", Coordinates(43.1107, 12.3908))) // Perugia
        assertFalse(NowcastCoverage.covers("IT", Coordinates(41.9028, 12.4964))) // Rome
        assertFalse(NowcastCoverage.covers("IT", Coordinates(40.8518, 14.2681))) // Naples
        assertFalse(NowcastCoverage.covers("IT", Coordinates(38.1157, 13.3615))) // Palermo
        // Inside the grid by 0.14°, inside its relaxation band: Siena.
        assertFalse(NowcastCoverage.covers("IT", Coordinates(43.3188, 11.3308)))
    }

    @Test
    fun `inside the grid, a country another model serves is not`() {
        // London is UKMO's and Amsterdam KNMI's: both measured interpolated (17 Oct 2026, and
        // again by the probe of 27 Oct, with Dublin, Brussels and Copenhagen).
        assertFalse(NowcastCoverage.covers("GB", Coordinates(51.5074, -0.1278)))
        assertFalse(NowcastCoverage.covers("NL", Coordinates(52.3676, 4.9041)))
        assertFalse(NowcastCoverage.covers("IE", Coordinates(53.3498, -6.2603)))
        assertFalse(NowcastCoverage.covers("BE", Coordinates(50.8503, 4.3517)))
        assertFalse(NowcastCoverage.covers("DK", Coordinates(55.6761, 12.5683)))
    }

    @Test
    fun `a place with no country code is never guessed into the table`() {
        assertFalse(NowcastCoverage.covers(null, Coordinates(45.4642, 9.19)))
    }

    @Test
    fun `computed series pass`() {
        // Milan, 18 Oct from the 05:00 slot: eight wet hours, none of them flat.
        assertFalse(NowcastCoverage.looksInterpolated(series(MILAN)))
        // Genoa, 17 Oct from 21:00: ten wet hours, one of them 0.1 0.1 0.1 0.1. One flat
        // hour is weather; all of them is arithmetic.
        assertFalse(NowcastCoverage.looksInterpolated(series(GENOA)))
        assertFalse(NowcastCoverage.looksInterpolated(series(NEW_YORK)))
    }

    @Test
    fun `the hourly series cut in four is caught`() {
        assertTrue(NowcastCoverage.looksInterpolated(series(LONDON)))
        assertTrue(NowcastCoverage.looksInterpolated(series(AMSTERDAM)))
    }

    @Test
    fun `a dry series is not suspected`() {
        assertFalse(NowcastCoverage.looksInterpolated(series(List(24) { 0.0 })))
        assertFalse(NowcastCoverage.looksInterpolated(emptyList()))
    }

    /**
     * Quarters as the mapper builds them: the first value is the slot labelled on the hour,
     * which is the quarter that ENDS there, so it starts fifteen minutes before.
     */
    private fun series(values: List<Double>): List<QuarterHour> {
        val firstSlot = LocalDateTime.of(2026, 10, 18, 5, 0).toInstant(ZoneOffset.UTC)
        return values.mapIndexed { i, mm ->
            QuarterHour(
                start = firstSlot.plus(QuarterHour.LENGTH.multipliedBy(i.toLong() - 1)),
                precipMm = mm,
                snowCm = 0.0
            )
        }
    }

    private fun parse(s: String) = s.trim().split(" ").map { it.toDouble() }

    private val MILAN = parse(
        "0 0 0.1 0.1 0.2 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0.1 0.1 0 0 0 0 " +
            "0.2 0.2 0.1 0 0 0 0 0 0 0 0 0 0 0.2 0.2 0 0 0.3 0.1 0.1 0.8 1.2 0.1 0 0 0 0 0 0 0.1 0 0"
    )
    private val GENOA = parse(
        "0 0 0 0.1 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0.9 6 1.4 0 0 0 0 " +
            "0.2 1.6 0.3 0 0 0 0 0.2 0.1 0.1 0.1 0.1 0.1 0.5 1.2 0.2 0.4 0 0 0 0 0 0 0 0.8 1.1 0 0 0 0 0 0 " +
            "0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0.3"
    )
    private val NEW_YORK = parse("0 0.1 0 0 0 0 0 0.1 0 0.2 0.1 0.1 0 0.2 0 0")
    private val LONDON = parse("0 0.2 0.2 0.2 0.2 0 0 0 0") + List(96) { 0.0 } +
        parse("0.2 0.2 0.2 0.2 0.2 0.2 0.2 0.2 0 0 0")
    private val AMSTERDAM = parse(
        "0 0.2 0.2 0.2 0.2 0.1 0.1 0.1 0.1 0 0 0 0 1.1 1.1 1.1 1.1 0 0 0 0 0.4 0.4 0.4 0.4 0 0 0 0 " +
            "0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0.1 0.1 0.1 0.1 0.2 0.2 0.2 0.2 0.2 0.2 0.2 0.2 " +
            "0.2 0.2 0.2 0.2 0.2 0.2 0.2 0.2"
    )
}
