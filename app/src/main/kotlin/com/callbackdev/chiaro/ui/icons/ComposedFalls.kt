package com.callbackdev.chiaro.ui.icons

import androidx.annotation.DrawableRes
import com.callbackdev.chiaro.R

/**
 * Le icone che mancavano, che **questo repo compone** dai disegni di Meteocons (Fase 14,
 * step 14.13, 27 ott 2026): la pioggia e la pioviggine gelata (quel che cade piu' il
 * cristallo di `snowflake` in basso, dove il ghiaccio si forma), la pioggia e la neve
 * possibile (la nuvola di `drizzle` e di `snow` con una goccia e un fiocco in meno, e
 * piu' chiari). Il perche', con le misure, sta in testa allo strumento.
 *
 * **File generato da `tools/compose_falls.py` — non si modifica a mano.** Ha la
 * forma di [ComposedIcons] (la chiave e' l'id del set line su fondo chiaro), gemelli
 * animati compresi: le gocce e i fiocchi si muovono come nella loro sorgente.
 */
internal object ComposedFalls {

    /** line, fondo scuro. */
    val lineDarkOf: Map<Int, Int> = mapOf(
        R.drawable.mc3_freezing_rain to R.drawable.mc3n_freezing_rain,
        R.drawable.mc3_freezing_drizzle to R.drawable.mc3n_freezing_drizzle,
        R.drawable.mc3_rain_possible to R.drawable.mc3n_rain_possible,
        R.drawable.mc3_snow_possible to R.drawable.mc3n_snow_possible,
    )

    /** flat, fondo chiaro. */
    val flatOf: Map<Int, Int> = mapOf(
        R.drawable.mc3_freezing_rain to R.drawable.mc3f_freezing_rain,
        R.drawable.mc3_freezing_drizzle to R.drawable.mc3f_freezing_drizzle,
        R.drawable.mc3_rain_possible to R.drawable.mc3f_rain_possible,
        R.drawable.mc3_snow_possible to R.drawable.mc3f_snow_possible,
    )

    /** flat, fondo scuro. */
    val flatDarkOf: Map<Int, Int> = mapOf(
        R.drawable.mc3_freezing_rain to R.drawable.mc3fn_freezing_rain,
        R.drawable.mc3_freezing_drizzle to R.drawable.mc3fn_freezing_drizzle,
        R.drawable.mc3_rain_possible to R.drawable.mc3fn_rain_possible,
        R.drawable.mc3_snow_possible to R.drawable.mc3fn_snow_possible,
    )

    /** I quattro gemelli animati, nello stesso ordine dei set fermi. */
    val movingOf: Map<Int, MeteoconsSets.Moving> = mapOf(
        R.drawable.mc3_freezing_rain to MeteoconsSets.Moving(
            R.drawable.mc3a_freezing_rain, R.drawable.mc3an_freezing_rain,
            R.drawable.mc3fa_freezing_rain, R.drawable.mc3fan_freezing_rain
        ),
        R.drawable.mc3_freezing_drizzle to MeteoconsSets.Moving(
            R.drawable.mc3a_freezing_drizzle, R.drawable.mc3an_freezing_drizzle,
            R.drawable.mc3fa_freezing_drizzle, R.drawable.mc3fan_freezing_drizzle
        ),
        R.drawable.mc3_rain_possible to MeteoconsSets.Moving(
            R.drawable.mc3a_rain_possible, R.drawable.mc3an_rain_possible,
            R.drawable.mc3fa_rain_possible, R.drawable.mc3fan_rain_possible
        ),
        R.drawable.mc3_snow_possible to MeteoconsSets.Moving(
            R.drawable.mc3a_snow_possible, R.drawable.mc3an_snow_possible,
            R.drawable.mc3fa_snow_possible, R.drawable.mc3fan_snow_possible
        ),
    )

    /** Per nome, come [MeteoconsSets.byName]. */
    val byName: Map<String, Int> = mapOf(
        "freezing-rain" to R.drawable.mc3_freezing_rain,
        "freezing-drizzle" to R.drawable.mc3_freezing_drizzle,
        "rain-possible" to R.drawable.mc3_rain_possible,
        "snow-possible" to R.drawable.mc3_snow_possible,
    )

    /** La pioggia gelata (66, 67) e la pioviggine gelata (56, 57). */
    @DrawableRes
    val freezingRain: Int = R.drawable.mc3_freezing_rain

    @DrawableRes
    val freezingDrizzle: Int = R.drawable.mc3_freezing_drizzle

    /** La pioggia possibile (1061) e la neve possibile (1071). */
    @DrawableRes
    val rainPossible: Int = R.drawable.mc3_rain_possible

    @DrawableRes
    val snowPossible: Int = R.drawable.mc3_snow_possible
}
