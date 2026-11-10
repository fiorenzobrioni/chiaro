package com.callbackdev.chiaro.domain.sky

import java.time.Instant
import kotlin.math.abs
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The four planets against an ephemeris (PLANNING, Fase 16, step 16.3): where each one is and
 * how bright, at eleven instants from 2026 to 2039, read from **JPL Horizons on 10 Nov 2026**
 * (`ssd.jpl.nasa.gov/api/horizons.api`, observer table, geocentric, `CENTER='500@399'`,
 * every 500 days from 2026-01-01 00:00 UT; quantity 2, the apparent right ascension and
 * declination of date, and quantity 9, the visual magnitude; targets 299, 499, 599, 699).
 *
 * `PlanetMathTest` holds the element table to what a wrong table cannot satisfy, and refuses
 * remembered rows as another rumour. These are not remembered: they are the ephemeris'
 * own numbers with the day they were read, the rule Fase 16 gives every new rule of the
 * engine, and they say the one thing the invariants cannot, which is how far off the answer
 * is. The bounds below are the measured worst with a margin, not a wish.
 *
 * Horizons' place is apparent (aberration and nutation in, about a third of an arcminute
 * together) and this model's is of the mean equinox of date with neither: part of the
 * residual is that, and it is far under the bound.
 */
class PlanetEphemerisTest {

    private class Row(val at: String, val raDeg: Double, val decDeg: Double, val magnitude: Double)

    private val horizons: Map<Planet, List<Row>> = mapOf(
        Planet.VENUS to listOf(
            Row("2026-01-01T00:00:00Z", 280.0565, -23.6224, -3.911),
            Row("2027-05-16T00:00:00Z", 29.7775, 10.4615, -3.864),
            Row("2028-09-27T00:00:00Z", 146.0405, 13.6794, -4.085),
            Row("2030-02-09T00:00:00Z", 282.2597, -16.9010, -4.886),
            Row("2031-06-24T00:00:00Z", 138.2757, 17.3321, -4.618),
            Row("2032-11-05T00:00:00Z", 261.6438, -25.1934, -4.033),
            Row("2034-03-20T00:00:00Z", 16.4540, 6.0642, -3.899),
            Row("2035-08-02T00:00:00Z", 130.1132, 19.4113, -3.909),
            Row("2036-12-14T00:00:00Z", 237.2758, -18.7491, -3.923),
            Row("2038-04-28T00:00:00Z", 356.4015, -2.8898, -4.099),
            Row("2039-09-10T00:00:00Z", 130.7767, 12.3475, -4.772)
        ),
        Planet.MARS to listOf(
            Row("2026-01-01T00:00:00Z", 283.8796, -23.7200, 1.072),
            Row("2027-05-16T00:00:00Z", 153.1903, 12.8567, 0.495),
            Row("2028-09-27T00:00:00Z", 136.7656, 17.7860, 1.534),
            Row("2030-02-09T00:00:00Z", 346.8564, -6.5486, 1.193),
            Row("2031-06-24T00:00:00Z", 212.3922, -14.5446, -0.874),
            Row("2032-11-05T00:00:00Z", 183.9053, -0.3399, 1.743),
            Row("2034-03-20T00:00:00Z", 44.0275, 17.3897, 1.336),
            Row("2035-08-02T00:00:00Z", 359.5811, -5.9767, -1.875),
            Row("2036-12-14T00:00:00Z", 233.0690, -18.8674, 1.566),
            Row("2038-04-28T00:00:00Z", 102.8854, 24.5788, 1.390),
            Row("2039-09-10T00:00:00Z", 84.9849, 23.0521, 0.413)
        ),
        Planet.JUPITER to listOf(
            Row("2026-01-01T00:00:00Z", 113.1243, 21.9791, -2.670),
            Row("2027-05-16T00:00:00Z", 141.3520, 16.1749, -2.018),
            Row("2028-09-27T00:00:00Z", 186.9774, -1.8183, -1.664),
            Row("2030-02-09T00:00:00Z", 233.9631, -18.2180, -1.992),
            Row("2031-06-24T00:00:00Z", 262.4252, -22.8070, -2.628),
            Row("2032-11-05T00:00:00Z", 297.9430, -21.5048, -2.167),
            Row("2034-03-20T00:00:00Z", 353.4351, -3.9497, -2.036),
            Row("2035-08-02T00:00:00Z", 46.2450, 16.1772, -2.330),
            Row("2036-12-14T00:00:00Z", 80.2104, 22.5859, -2.792),
            Row("2038-04-28T00:00:00Z", 114.4117, 22.0236, -2.041),
            Row("2039-09-10T00:00:00Z", 164.9056, 7.4869, -1.694)
        ),
        Planet.SATURN to listOf(
            Row("2026-01-01T00:00:00Z", 357.3810, -3.5964, 1.007),
            Row("2027-05-16T00:00:00Z", 21.5029, 6.5812, 0.703),
            Row("2028-09-27T00:00:00Z", 38.7156, 12.3659, 0.057),
            Row("2030-02-09T00:00:00Z", 46.6512, 15.2685, 0.147),
            Row("2031-06-24T00:00:00Z", 73.9719, 21.1390, 0.107),
            Row("2032-11-05T00:00:00Z", 97.4721, 22.2469, -0.190),
            Row("2034-03-20T00:00:00Z", 105.5888, 22.5018, -0.026),
            Row("2035-08-02T00:00:00Z", 131.6186, 18.5531, 0.234),
            Row("2036-12-14T00:00:00Z", 155.3575, 11.7631, 0.461),
            Row("2038-04-28T00:00:00Z", 161.8853, 9.8900, 0.629),
            Row("2039-09-10T00:00:00Z", 181.3155, 1.7056, 1.012)
        )
    )

    /**
     * How far off each planet may be on the sky, degrees. Measured on these rows: Venus
     * 0.015, Mars 0.019, Jupiter 0.042, Saturn 0.113; each bound is about twice its planet's
     * worst. Jupiter and Saturn are the two the mean elements serve least, because they pull
     * on each other over nine centuries and a table of rates per century cannot carry that.
     */
    private val placeBound = mapOf(
        Planet.VENUS to 0.03,
        Planet.MARS to 0.04,
        Planet.JUPITER to 0.08,
        Planet.SATURN to 0.2
    )

    @Test
    fun `every planet stands where the ephemeris puts it`() {
        horizons.forEach { (planet, rows) ->
            var worst = 0.0
            var worstAt = ""
            rows.forEach { row ->
                val t = AstronomyMath.centuriesTT(Instant.parse(row.at))
                val gap = AstronomyMath.separation(
                    PlanetMath.equatorial(planet, t).equatorial,
                    AstronomyMath.Equatorial(row.raDeg, row.decDeg)
                )
                if (gap > worst) {
                    worst = gap
                    worstAt = row.at
                }
            }
            assertTrue(
                "$planet was ${"%.4f".format(worst)}° from Horizons at $worstAt",
                worst < placeBound.getValue(planet)
            )
        }
    }

    /**
     * Within 0.3 of a magnitude. Measured worst: Venus 0.24 (a thin crescent, where the old
     * almanac's polynomial is weakest and where nothing depends on it: Venus is past the top
     * of the drawing's scale at any phase), Saturn 0.15, Mars 0.12, Jupiter 0.03. Saturn's
     * rows run from rings nearly edge-on (2026, 2039) to wide open (2032), so the two ring
     * terms are in the measure: without them the 2032 row would be off by about 0.9.
     */
    @Test
    fun `every planet is as bright as the ephemeris has it`() {
        horizons.forEach { (planet, rows) ->
            var worst = 0.0
            var worstAt = ""
            rows.forEach { row ->
                val gap = abs(AstronomyEngine.planetMagnitude(planet, Instant.parse(row.at)) - row.magnitude)
                if (gap > worst) {
                    worst = gap
                    worstAt = row.at
                }
            }
            assertTrue("$planet was ${"%.3f".format(worst)} magnitudes from Horizons at $worstAt", worst < 0.3)
        }
    }
}
