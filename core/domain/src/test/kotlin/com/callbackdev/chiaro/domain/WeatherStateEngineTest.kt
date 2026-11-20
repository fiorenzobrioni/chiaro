package com.callbackdev.chiaro.domain

import com.callbackdev.chiaro.domain.WeatherStateEngine.Hour
import com.callbackdev.chiaro.domain.model.CloudLayers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The state engine, rule by rule (25 set 2026). The cases named after a city are hours of
 * the measurement of that morning (`tools/measurements/states-2026-09-25.json`).
 */
class WeatherStateEngineTest {

    private fun state(
        code: Int = 3,
        sky: Int = code,
        mm: Double? = 0.0,
        snow: Double? = 0.0,
        showers: Double? = 0.0,
        pct: Int? = 50,
        t: Double? = 10.0,
        cloud: Int? = 90,
        likely: Boolean = true
    ) = WeatherStateEngine.state(
        Hour(code, sky, mm, snow, showers, pct, t, cloud, fogPersists = false),
        likely
    )

    // ---------------------------------------------------------------- 1. thunderstorm

    @Test
    fun `a probable storm keeps the provider's code when it rains`() {
        assertEquals(95, state(code = 95, mm = 2.0, pct = 60))
        assertEquals(96, state(code = 96, mm = 0.1, pct = 20))
    }

    @Test
    fun `a storm over a dry hour is a possible storm, still a hazard`() {
        assertEquals(WmoCode.THUNDERSTORM_POSSIBLE.code, state(code = 95, mm = 0.0, pct = 40))
        assertEquals(AlertEngine.SevereBucket.THUNDER, WmoCode.THUNDERSTORM_POSSIBLE.hazard)
    }

    @Test
    fun `a storm code is a storm at any chance of rain`() {
        // Until 17 nov 2026 a storm under 20% was the hour's sky. The chance is of rain, not
        // of lightning: against the airports' thunder such an hour is a storm as often as
        // one the ensemble backs, and in the Po valley more often.
        assertEquals(95, state(code = 95, mm = 1.0, pct = 10, cloud = 85))
        assertEquals(95, state(code = 95, mm = 1.0, pct = null))
        assertEquals(WmoCode.THUNDERSTORM_POSSIBLE.code, state(code = 95, mm = 0.0, pct = 5))
    }

    // ---------------------------------------------------------------- 2. freezing rain

    @Test
    fun `the provider's freezing rain stands at any chance`() {
        assertEquals(56, state(code = 56, mm = 0.1, pct = 5))
        assertEquals(67, state(code = 67, mm = 4.0, pct = 90))
    }

    @Test
    fun `liquid rain below zero is not deduced to be freezing, not yet`() {
        assertEquals(61, state(code = 61, mm = 1.0, t = -2.0, pct = 80))
    }

    // ------------------------------------------------------- 3. measurable and probable

    @Test
    fun `a trace under twenty percent is the sky — Longyearbyen's snowflake over a low chance`() {
        // 25 Sep, 02:00: code 71, 0.1 mm, 0.07 cm, 10%.
        assertEquals(3, state(code = 71, mm = 0.1, snow = 0.07, pct = 10, cloud = 100))
        // The same hour at 20% is snow.
        assertEquals(71, state(code = 71, mm = 0.1, snow = 0.07, pct = 20))
    }

    @Test
    fun `under the measurable floor nothing falls, whatever the code says`() {
        assertEquals(3, state(code = 71, mm = 0.0, snow = 0.0, pct = 40))
        assertEquals(2, state(code = 51, mm = 0.05, pct = 50, cloud = 60))
    }

    @Test
    fun `an amount under an overcast code is rain — Bari's case B`() {
        // Bari, 25 Sep 10:00: 0.5 mm at 48% under code 3.
        assertEquals(61, state(code = 3, mm = 0.5, showers = 0.0, pct = 48))
    }

    @Test
    fun `the rain ladder is the AMS one, not the provider's drizzle up to 1_3`() {
        assertEquals(51, state(code = 51, mm = 0.1))
        assertEquals(51, state(code = 51, mm = 0.4))
        assertEquals(61, state(code = 53, mm = 0.5))   // London, 0.9 mm under «dense drizzle»
        assertEquals(61, state(code = 55, mm = 2.4))
        assertEquals(63, state(code = 61, mm = 2.5))
        assertEquals(63, state(code = 63, mm = 7.5))
        assertEquals(65, state(code = 63, mm = 7.6))
    }

    @Test
    fun `convective rain is showers at any intensity — Reykjavik's drizzle`() {
        assertEquals(80, state(code = 51, mm = 0.4, showers = 0.4))
        assertEquals(80, state(code = 51, mm = 0.4, showers = 0.2))   // half
        assertEquals(51, state(code = 51, mm = 0.4, showers = 0.1))   // under half
        assertEquals(81, state(code = 80, mm = 3.0, showers = 3.0))
        assertEquals(82, state(code = 81, mm = 9.0, showers = 9.0))
    }

    @Test
    fun `London's rain that already holds its showers is still read right`() {
        // The UK model's `rain` includes the convective part: 1.5 total, 0.6 of it showers.
        assertEquals(61, state(code = 61, mm = 1.5, showers = 0.6))
    }

    // ------------------------------------- 19 nov 2026: a shower from the air and the shape

    /** A probable rain of [mm] in air of [cape], with the model's [showers]; judged alone
     * unless [episode] is passed. */
    private fun rain(cape: Double?, mm: Double = 1.0, showers: Double? = 0.0, episode: Int? = null) =
        WeatherStateEngine.state(
            Hour(61, 61, mm, 0.0, showers, 60, 15.0, 90, fogPersists = false, capeJkg = cape, episodeHours = episode)
        )

    /** A series: a rain of 1 mm at 60% in air of [cape] at the hours of [wet], dry at 10%
     * elsewhere (at 60% a dry hour would be «Pioggia possibile», rule 4). */
    private fun series(n: Int, wet: Set<Int>, cape: Double) =
        WeatherStateEngine.states(List(n) { i ->
            val isWet = i in wet
            Hour(if (isWet) 61 else 3, 3, if (isWet) 1.0 else 0.0, 0.0, 0.0, if (isWet) 60 else 10, 15.0, 90,
                fogPersists = false, capeJkg = cape)
        })

    @Test
    fun `unstable air makes a rain a shower at any intensity, whatever showers says`() {
        assertEquals(80, rain(cape = 100.0, showers = 0.0))
        assertEquals(81, rain(cape = 600.0, mm = 3.0, showers = 0.0))
        assertEquals(82, rain(cape = 1500.0, mm = 9.0, showers = 0.0))
        // `showers` is not read for rain any more: ICON-EU's all-convective hour in stable air
        // is the rain ICON-D2 calls it.
        assertEquals(61, rain(cape = 99.0, showers = 1.0))
    }

    @Test
    fun `a short episode in air with some energy is a shower, a long one is rain`() {
        // One or two wet hours at 20 J/kg: showers.
        assertEquals(listOf(3, 80, 80, 3, 3), series(5, setOf(1, 2), cape = 20.0))
        // Three: a spell of rain.
        assertEquals(listOf(3, 61, 61, 61, 3), series(5, setOf(1, 2, 3), cape = 20.0))
    }

    @Test
    fun `a short drizzle in stable air is not a shower — the Po valley's winter`() {
        assertEquals(listOf(3, 61, 3), series(3, setOf(1), cape = 5.0))
    }

    @Test
    fun `an improbable hour does not lengthen an episode`() {
        // Hours 1-3 have an amount, hour 2 at 10%: the episode is two runs of one hour each.
        assertEquals(1, WeatherStateEngine.episodeHours(
            List(5) { i -> Hour(61, 3, if (i in 1..3) 1.0 else 0.0, chancePct = if (i == 2) 10 else 60) }, 1
        ))
        // A null hour — one the provider gave nothing — ends a run too.
        val dry = Hour(3, 3, 0.0)
        val wet = Hour(61, 3, 1.0, chancePct = 60)
        assertEquals(1, WeatherStateEngine.episodeHours(listOf(dry, wet, null, wet, dry), 1))
        assertEquals(0, WeatherStateEngine.episodeHours(listOf(dry), 0))
    }

    @Test
    fun `a rain that reaches either end of the series has no known length`() {
        // 20 nov 2026, the review of PR #185: the series starts at today's midnight, and a rain
        // from 20:00 last night to 01:00 shows one hour of itself. Counted, it was a one-hour
        // episode and «Rovesci» in air of 20 J/kg; not known, the CAPE alone decides: rain.
        assertNull(WeatherStateEngine.episodeHours(List(5) { i -> Hour(61, 3, if (i == 0) 1.0 else 0.0, chancePct = 60) }, 0))
        assertEquals(listOf(61, 3, 3, 3, 3), series(5, setOf(0), cape = 20.0))
        // The same at the horizon: the last day's last hours.
        assertEquals(listOf(3, 3, 3, 61, 61), series(5, setOf(3, 4), cape = 20.0))
        // Away from both ends a short run is still a shower.
        assertEquals(listOf(3, 80, 3, 3, 3), series(5, setOf(1), cape = 20.0))
        // An hour given its episode keeps it: the edge is the series', not the hour's.
        assertEquals(80, rain(cape = 20.0, episode = 1))
    }

    @Test
    fun `an hour judged alone has no episode but the one it is given`() {
        assertEquals(61, rain(cape = 20.0))
        assertEquals(80, rain(cape = 20.0, episode = 1))
        assertEquals(61, rain(cape = 20.0, episode = 3))
        assertEquals(80, rain(cape = 150.0, episode = 5))
    }

    @Test
    fun `without CAPE the model's showers decide, as before`() {
        assertEquals(80, rain(cape = null, mm = 0.4, showers = 0.4))
        assertEquals(51, rain(cape = null, mm = 0.4, showers = 0.1))
    }

    @Test
    fun `snow showers are still the model's, in any air`() {
        val snow = Hour(71, 71, 0.4, 0.28, 0.0, 60, -2.0, 90, fogPersists = false, capeJkg = 500.0, episodeHours = 1)
        assertEquals(73, WeatherStateEngine.state(snow))
    }

    @Test
    fun `snow and its ladder come from the snowfall`() {
        assertEquals(71, state(code = 3, mm = 0.1, snow = 0.07))
        assertEquals(73, state(code = 71, mm = 0.4, snow = 0.28))
        assertEquals(75, state(code = 73, mm = 1.2, snow = 0.84))
    }

    @Test
    fun `convective snow is snow showers, even when showers count the same water`() {
        // Longyearbyen 05:00: showers 0.1 and 0.07 cm in a 0.1 mm hour, code 85.
        assertEquals(85, state(code = 85, mm = 0.1, snow = 0.07, showers = 0.1))
        assertEquals(86, state(code = 85, mm = 1.4, snow = 0.98, showers = 1.4))
    }

    @Test
    fun `rain and snow are both there only when both are measurable`() {
        // 0.3 mm of which 0.14 cm snow = 0.2 mm water, 0.1 liquid.
        assertEquals(68, state(code = 71, mm = 0.3, snow = 0.14))
        // 3 mm with 0.5 cm (0.71 mm of water, 2.29 liquid): neither part heavy on its own.
        assertEquals(69, state(code = 73, mm = 3.0, snow = 0.5))
        // 0.07 cm (0.1 mm water) in a 0.1 mm hour: no liquid left, snow.
        assertEquals(71, state(code = 71, mm = 0.1, snow = 0.07))
    }

    @Test
    fun `heavy snow is a warning at any chance, like the banner`() {
        assertEquals(75, state(code = 75, mm = 1.4, snow = 0.98, pct = 5))
        // …while an improbable heavy downpour is not, like the banner.
        assertEquals(3, state(code = 65, mm = 9.0, pct = 15))
    }

    @Test
    fun `rain and snow together never hide the warning of a heavy part`() {
        // 12 mm with 1 cm of snow (1.43 mm of water): the snow alone is heavy.
        assertEquals(75, state(code = 73, mm = 12.0, snow = 1.0))
        // …and still is at 5%, as heavy snow always is.
        assertEquals(75, state(code = 73, mm = 12.0, snow = 1.0, pct = 5))
        // 9 mm with 0.3 cm: the rain alone is heavy (8.6 mm).
        assertEquals(65, state(code = 63, mm = 9.0, snow = 0.3))
    }

    @Test
    fun `without the convective amount the provider's showers still name the hour`() {
        assertEquals(81, state(code = 81, mm = 3.0, showers = null))
        assertEquals(80, state(code = 80, mm = 0.4, showers = null))
        assertEquals(85, state(code = 85, mm = 0.1, snow = 0.07, showers = null))
        assertEquals(61, state(code = 61, mm = 1.0, showers = null))
    }

    // --------------------------------------------------------------------- 4. likely

    @Test
    fun `a dry hour at sixty percent is likely rain — Tokyo's case C`() {
        assertEquals(WmoCode.RAIN_LIKELY.code, state(code = 2, mm = 0.0, pct = 90, cloud = 60))
        assertEquals(WmoCode.RAIN_LIKELY.code, state(code = 3, mm = 0.0, pct = 60))
        assertEquals(3, state(code = 3, mm = 0.0, pct = 59))
    }

    @Test
    fun `likely is never a fact`() {
        assertEquals(false, WmoCode.RAIN_LIKELY.isPrecipitation)
        assertEquals(true, WmoCode.SNOW_LIKELY.isSnow)
    }

    @Test
    fun `an observation is never likely`() {
        assertEquals(3, state(code = 3, mm = 0.0, pct = 90, likely = false))
    }

    @Test
    fun `a likely hour takes the phase of the nearest measured one`() {
        fun hour(mm: Double, snow: Double, pct: Int, t: Double) =
            Hour(3, 3, mm, snow, 0.0, pct, t, 90, false)
        // Snow two hours later, rain three hours earlier: the nearer one wins.
        val series = listOf(
            hour(0.5, 0.0, 80, 3.0), hour(0.0, 0.0, 10, 3.0), hour(0.0, 0.0, 10, 3.0),
            hour(0.0, 0.0, 70, 3.0),
            hour(0.0, 0.0, 10, 3.0), hour(0.7, 0.49, 80, 3.0)
        )
        assertEquals(WmoCode.SNOW_LIKELY.code, WeatherStateEngine.states(series)[3])
        // Nothing measured within three hours: the temperature decides, 1 °C.
        assertEquals(WmoCode.SNOW_LIKELY.code, state(code = 3, mm = 0.0, pct = 63, t = 1.0))
        assertEquals(WmoCode.RAIN_LIKELY.code, state(code = 3, mm = 0.0, pct = 63, t = 1.1))
    }

    @Test
    fun `a likely hour's own phase comes first`() {
        // A trace of snow (0.03 cm) under a snow code at 1.5 °C: the model's own phase.
        assertEquals(WmoCode.SNOW_LIKELY.code, state(code = 71, mm = 0.05, snow = 0.03, pct = 70, t = 1.5))
        // A precipitation code with no amount at all.
        assertEquals(WmoCode.SNOW_LIKELY.code, state(code = 71, mm = 0.0, snow = 0.0, pct = 70, t = 3.0))
        assertEquals(WmoCode.RAIN_LIKELY.code, state(code = 51, mm = 0.0, snow = 0.0, pct = 70, t = 0.5))
    }

    @Test
    fun `a storm over a dry quarter is possible in the present too`() {
        assertEquals(WmoCode.THUNDERSTORM_POSSIBLE.code, state(code = 95, mm = 0.0, pct = 40, likely = false))
    }

    // ------------------------------------------------------------------------ 5. fog

    private fun w(vis: Double?, rh: Int? = 95) = WeatherStateEngine.Witness(visibilityM = vis, humidityPct = rh)

    /**
     * A dry hour of a series where fog persists (unless [persists] says not), its own model at
     * [rh] in a wind of [wind] km/h, the provider's code [sky] at its instant, with [witnesses].
     */
    private fun fog(
        rh: Int? = 99,
        wind: Double? = 5.0,
        vararg witnesses: WeatherStateEngine.Witness,
        sky: Int = 3,
        t: Double = 8.0,
        persists: Boolean = true
    ) = WeatherStateEngine.state(
        Hour(
            3, sky, 0.0, 0.0, 0.0, 10, t, 98, fogPersists = persists,
            humidityPct = rh, windKph = wind, witnesses = witnesses.toList()
        )
    )

    @Test
    fun `one model's word is not a fog — Cavenago, 17 Nov 2026`() {
        // 10:00: ICON-D2 240 m at 98% in a 3 km/h breeze, under its own fog code; IFS 1.4 km,
        // UKMO 18 km, KNMI 5 km, DMI 2.4 km. The airports around saw 2 to 7 km of haze, and the
        // committente, outside, the next village. ICON-D2's visibility is no input: its model
        // votes with its saturated, calm air, and nobody else sees fog. One vote: overcast.
        assertEquals(
            3, fog(98, 3.1, w(1_440.0, 92), w(17_780.0, 94), w(5_080.0, 95), w(2_440.0, 97), sky = 45)
        )
        // 07:00, the same morning: KNMI 160 m and DMI 220 m. Two models, and the air the five
        // agree on is 98% (100, 100, 99, 96, 94 without the 100 and the 94).
        assertEquals(
            45, fog(100, 3.0, w(5_280.0, 94), w(14_640.0, 96), w(160.0, 99), w(220.0, 100), sky = 45)
        )
    }

    @Test
    fun `two models make a fog, whoever they are`() {
        // A witness under a kilometre and the own model's saturated calm.
        assertEquals(45, fog(99, 5.0, w(800.0, 96), w(20_000.0, 94)))
        // Two witnesses, the own air short of saturation.
        assertEquals(45, fog(95, 5.0, w(600.0, 97), w(900.0, 98), w(20_000.0, 90)))
        // One witness alone, however low and however humid.
        assertEquals(3, fog(90, 5.0, w(100.0, 100), w(20_000.0, 90)))
        // A witness at exactly a kilometre votes; one a metre over does not.
        assertEquals(45, fog(99, 5.0, w(1_000.0, 96)))
        assertEquals(3, fog(99, 5.0, w(1_001.0, 96)))
    }

    @Test
    fun `the own model votes with saturated air in a calm`() {
        assertEquals(45, fog(98, 12.0, w(500.0, 97)))
        // Short of 98%, over 12 km/h, or either unknown: no vote.
        assertEquals(3, fog(97, 5.0, w(500.0, 97)))
        assertEquals(3, fog(99, 12.1, w(500.0, 97)))
        assertEquals(3, fog(null, 5.0, w(500.0, 97)))
        assertEquals(3, fog(99, null, w(500.0, 97)))
        assertEquals(1, WeatherStateEngine.fogVotes(Hour(3, humidityPct = 98, windKph = 0.0)))
        assertEquals(0, WeatherStateEngine.fogVotes(Hour(3, humidityPct = 100, windKph = 20.0)))
    }

    @Test
    fun `without witnesses there is no fog — one model's word`() {
        // A failed witness request, an old cache entry: the own model's vote is one.
        assertEquals(3, fog(100, 0.0, sky = 45))
    }

    @Test
    fun `the air the models agree on is their humidity without the highest and the lowest`() {
        fun mean(rh: Int?, vararg wrh: Int?) =
            WeatherStateEngine.fogMeanHumidityPct(Hour(3, humidityPct = rh, witnesses = wrh.map { w(9_000.0, it) }))
        // Cavenago at 10:00: 98, 92, 94, 95, 97 → 94, 95, 97.
        assertEquals(95.333, mean(98, 92, 94, 95, 97)!!, 0.001)
        // Three: the middle one. Two: their mean. One: itself. None: none.
        assertEquals(96.0, mean(99, 96, 94)!!, 0.0)
        assertEquals(97.5, mean(98, 97)!!, 0.0)
        assertEquals(96.0, mean(null, 96)!!, 0.0)
        assertEquals(null, mean(null, null))
    }

    @Test
    fun `a fog does not stand under air the models agree is short of saturation`() {
        // Three votes, but 85 and 88 once the 99 and the 80 are set aside.
        assertEquals(3, fog(99, 5.0, w(500.0, 85), w(700.0, 88), w(20_000.0, 80)))
        // At 93% it stands, under it it does not.
        assertEquals(45, fog(99, 5.0, w(500.0, 93), w(700.0, 93)))
        assertEquals(3, fog(99, 5.0, w(500.0, 92), w(700.0, 92)))
        // With no humidity anywhere there is nothing to filter with, and no fog.
        assertEquals(3, fog(null, 5.0, w(500.0, null), w(700.0, null)))
    }

    @Test
    fun `fog is kept by the provider's code or invented where it persists`() {
        assertEquals(3, fog(99, 5.0, w(500.0, 96), persists = false))
        assertEquals(45, fog(99, 5.0, w(500.0, 96), sky = 45, persists = false))
        assertEquals(45, fog(99, 5.0, w(500.0, 96), persists = true))
    }

    @Test
    fun `in a series two votes must persist into a neighbouring hour`() {
        fun hour(votes: Int) = Hour(
            3, 3, 0.0, 0.0, 0.0, 10, 8.0, 100, humidityPct = 96, windKph = 5.0,
            witnesses = List(4) { k -> w(if (k < votes) 400.0 else 20_000.0, 97) }
        )
        // Two votes in one hour alone, then in two hours in a row.
        assertEquals(listOf(3, 3, 3, 45, 45), WeatherStateEngine.states(listOf(hour(1), hour(2), hour(0), hour(2), hour(3))))
    }

    @Test
    fun `the persistence test reads a bare series of votes the same way`() {
        val votes = listOf(1, 2, 0, 2, 3, null)
        assertEquals(
            listOf(false, false, false, true, true, false),
            votes.indices.map { WeatherStateEngine.fogPersists(votes, it) }
        )
        // Lissone, 26 Oct 2026, 09:00: ICON-D2 140 m at 94%, ECMWF, the Met Office and GFS
        // 24–28 km away. No vote at all: the hour is its sky, and so are its neighbours.
        val lissone = Hour(3, 3, humidityPct = 94, windKph = 4.0, witnesses = listOf(w(28_280.0, 71), w(24_740.0, 77)))
        assertEquals(0, WeatherStateEngine.fogVotes(lissone))
    }

    @Test
    fun `the provider's fog code alone never makes fog`() {
        // Measured against the METARs of 24 airports (STATES §11): the code says fog 4 361
        // times for 955 seen, 2 425 of them over air clear for five kilometres.
        assertEquals(3, state(code = 45, cloud = 90))
        assertEquals(0, state(code = 45, cloud = 10))
    }

    @Test
    fun `fog at or below zero is freezing fog — Longyearbyen at minus four`() {
        assertEquals(48, fog(99, 5.0, w(200.0, 98), t = -4.2))
        assertEquals(48, fog(99, 5.0, w(200.0, 98), t = 0.0))
        assertEquals(45, fog(99, 5.0, w(200.0, 98), t = 0.1))
    }

    @Test
    fun `falling precipitation comes before fog`() {
        val wet = Hour(
            61, 61, 1.0, 0.0, 0.0, 50, 8.0, 98, fogPersists = true, humidityPct = 99, windKph = 5.0,
            witnesses = listOf(w(300.0, 99), w(400.0, 99))
        )
        assertEquals(61, WeatherStateEngine.state(wet))
    }

    @Test
    fun `a trace under fog is fog, not drizzle — Singapore`() {
        val trace = Hour(
            51, 45, 0.1, 0.0, 0.0, 18, 26.0, 98, fogPersists = true, humidityPct = 99, windKph = 5.0,
            witnesses = listOf(w(800.0, 97))
        )
        assertEquals(45, WeatherStateEngine.state(trace))
    }

    // ------------------------------------------------------------------------ 6. sky

    @Test
    fun `the sky is the cloud cover's, on the provider's buckets`() {
        assertEquals(0, state(code = 1, cloud = 19))
        assertEquals(1, state(code = 0, cloud = 20))
        assertEquals(1, state(code = 2, cloud = 49))
        assertEquals(2, state(code = 3, cloud = 50))
        assertEquals(2, state(code = 3, cloud = 79))
        assertEquals(3, state(code = 2, cloud = 80))
    }

    private fun sky(cloud: Int, low: Int?, mid: Int?, high: Int?) = WeatherStateEngine.state(
        Hour(3, 3, 0.0, 0.0, 0.0, 10, 10.0, cloud, cloudLayers = CloudLayers(low, mid, high))
    )

    @Test
    fun `the sky is the light the layers take, not the share of sky they cover`() {
        // A high veil casts shadows: 92% of it takes under a third of the light. Not
        // «overcast»; since 17 nov 2026 not «mostly clear» either, under a sky with no gap.
        assertEquals(WmoCode.VEILED.code, sky(cloud = 92, low = 0, mid = 0, high = 92))
        // The same light under a broken veil is the light's word.
        assertEquals(1, sky(cloud = 70, low = 0, mid = 0, high = 70))
        // A low deck is overcast whatever the total.
        assertEquals(3, sky(cloud = 95, low = 95, mid = 0, high = 0))
        // The README's Milan at noon, 25 Sep 2026: 86% in all, 66% low, 13% middle, 49% high;
        // the satellite saw the sun, and the total had said «overcast».
        assertEquals(2, sky(cloud = 86, low = 66, mid = 13, high = 49))
        // Never more than the total: layers that overshoot it are the total's.
        assertEquals(1, sky(cloud = 30, low = 90, mid = 0, high = 0))
    }

    // ------------------------------------------------- 6. sky: the veil (17 nov 2026)

    @Test
    fun `a covered sky whose cloud takes less than half the light is veiled`() {
        // A sheet of cirrostratus: the whole sky, 30% of the light.
        assertEquals(WmoCode.VEILED.code, sky(cloud = 100, low = 0, mid = 0, high = 100))
        // At the edges: cover 80, and 49% of the light taken (80% of high veil takes 24%,
        // a middle layer of 41% under it brings the two to 48.9%).
        assertEquals(WmoCode.VEILED.code, sky(cloud = 80, low = 0, mid = 41, high = 80))
        // Thin layers under the veil, 46% of the light: still a sun to see through it.
        assertEquals(WmoCode.VEILED.code, sky(cloud = 95, low = 10, mid = 20, high = 95))
    }

    @Test
    fun `a gap, a low deck, or half the light gone is not a veil`() {
        // Under 80% the sky has gaps: the light's word stands.
        assertEquals(1, sky(cloud = 79, low = 0, mid = 0, high = 79))
        // A low deck under the veil takes the light: 57% is «Nuvoloso», 90% «Coperto».
        assertEquals(WmoCode.PARTLY_CLOUDY.code, sky(cloud = 95, low = 40, mid = 0, high = 95))
        assertEquals(WmoCode.OVERCAST.code, sky(cloud = 100, low = 85, mid = 0, high = 100))
        // Exactly half the light taken is not «less than half».
        assertEquals(WmoCode.PARTLY_CLOUDY.code, sky(cloud = 100, low = 50, mid = 0, high = 0))
    }

    @Test
    fun `no veil is read without the layers, and nothing before the sky is moved by it`() {
        // Without layers the light taken is the cover: at 80% it is never under half.
        assertEquals(WmoCode.OVERCAST.code, sky(cloud = 95, low = null, mid = null, high = null))
        assertEquals(WmoCode.OVERCAST.code, state(cloud = 100))
        // Rain and the possible storm come first, under the same veil.
        val veil = CloudLayers(0, 0, 100)
        assertEquals(
            61,
            WeatherStateEngine.state(Hour(61, 61, 1.0, 0.0, 0.0, 80, 10.0, 100, cloudLayers = veil))
        )
        assertEquals(
            WmoCode.THUNDERSTORM_POSSIBLE.code,
            WeatherStateEngine.state(Hour(95, 95, 0.0, 0.0, 0.0, 40, 20.0, 100, cloudLayers = veil))
        )
        // Fog too: two models in it, its code, saturated air.
        assertEquals(
            45,
            WeatherStateEngine.state(
                Hour(
                    45, 45, 0.0, 0.0, 0.0, 5, 6.0, 100, fogPersists = true, cloudLayers = veil,
                    humidityPct = 99, windKph = 4.0, witnesses = listOf(w(600.0, 98))
                )
            )
        )
    }

    @Test
    fun `without layers, or with layers that say nothing, the sky is the total's`() {
        assertEquals(3, sky(cloud = 90, low = null, mid = null, high = null))
        assertEquals(3, sky(cloud = 90, low = 0, mid = 0, high = 0))
        assertEquals(3, state(cloud = 90))
    }

    // -------------------------------------------------------------------- fallbacks

    @Test
    fun `without amounts the provider's precipitation code stands, as before the engine`() {
        assertEquals(63, state(code = 63, mm = null, snow = null, showers = null))
        assertEquals(71, state(code = 71, mm = null, snow = null, showers = null, pct = 5))
    }

    @Test
    fun `without snowfall the phase comes from the code, then the temperature`() {
        assertEquals(71, state(code = 73, mm = 0.1, snow = null))
        assertEquals(61, state(code = 61, mm = 1.0, snow = null, showers = null))
        assertEquals(71, state(code = 3, mm = 0.1, snow = null, t = 0.5))
        assertEquals(51, state(code = 3, mm = 0.1, snow = null, t = 5.0))
    }

    @Test
    fun `without cloud cover the provider's sky stands, without votes its fog does not`() {
        assertEquals(1, state(code = 1, cloud = null))
        assertEquals(3, state(code = 45, cloud = null))
        assertEquals(3, state(code = 61, sky = 61, mm = 0.0, cloud = null))
    }

    @Test
    fun `a number outside the table is not a sky, and not wet`() {
        assertEquals(2, state(code = 42, cloud = 60))
        assertEquals(3, state(code = 42, sky = 42, mm = null, cloud = null))
    }
}
