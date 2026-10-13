package com.callbackdev.chiaro.ui.icons

import androidx.annotation.DrawableRes
import com.callbackdev.chiaro.R

/**
 * I disegni che **questo repo compone** dai pezzi di Meteocons, invece di importarli.
 *
 * **File generato da `tools/compose_sun_cloud.py` — non si modifica a mano.** Ha la
 * stessa forma di [MeteoconsSets], perche' `ChiaroIcons` lo interroga allo stesso modo:
 * la chiave e' l'id del set line su fondo chiaro, e le tabelle danno le altre tre facce
 * e i gemelli animati. Sta in un file suo e non dentro [MeteoconsSets] per la ragione
 * che rende sicura tutta la faccenda: quel file lo riscrive l'importatore a ogni giro,
 * questo no.
 *
 * Sono due, e sono una scala: il «quasi sereno» (il sereno di Meteocons intatto piu' una
 * nuvoletta nell'angolo, perche' il `mostly-clear` della libreria porta il 72% della
 * nuvola del «poco nuvoloso» per un cielo che ne ha il 39% di copertura) e dal 13 ott
 * 2026 il «poco nuvoloso» (lo stesso sole, la nuvola al 78%), che ha preso il posto di
 * `partly-cloudy` perche' fra i due disegni la scala saltava. Il perche' per esteso, con
 * le misure, sta in testa allo strumento.
 */
internal object ComposedIcons {

    /** line, fondo scuro. */
    val lineDarkOf: Map<Int, Int> = mapOf(
        R.drawable.mc3_sun_one_cloud_day to R.drawable.mc3n_sun_one_cloud_day,
        R.drawable.mc3_sun_one_cloud_night to R.drawable.mc3n_sun_one_cloud_night,
        R.drawable.mc3_sun_half_cloud_day to R.drawable.mc3n_sun_half_cloud_day,
        R.drawable.mc3_sun_half_cloud_night to R.drawable.mc3n_sun_half_cloud_night,
    )

    /** flat, fondo chiaro. */
    val flatOf: Map<Int, Int> = mapOf(
        R.drawable.mc3_sun_one_cloud_day to R.drawable.mc3f_sun_one_cloud_day,
        R.drawable.mc3_sun_one_cloud_night to R.drawable.mc3f_sun_one_cloud_night,
        R.drawable.mc3_sun_half_cloud_day to R.drawable.mc3f_sun_half_cloud_day,
        R.drawable.mc3_sun_half_cloud_night to R.drawable.mc3f_sun_half_cloud_night,
    )

    /** flat, fondo scuro. */
    val flatDarkOf: Map<Int, Int> = mapOf(
        R.drawable.mc3_sun_one_cloud_day to R.drawable.mc3fn_sun_one_cloud_day,
        R.drawable.mc3_sun_one_cloud_night to R.drawable.mc3fn_sun_one_cloud_night,
        R.drawable.mc3_sun_half_cloud_day to R.drawable.mc3fn_sun_half_cloud_day,
        R.drawable.mc3_sun_half_cloud_night to R.drawable.mc3fn_sun_half_cloud_night,
    )

    /** I quattro gemelli animati, nello stesso ordine dei set fermi. */
    val movingOf: Map<Int, MeteoconsSets.Moving> = mapOf(
        R.drawable.mc3_sun_one_cloud_day to MeteoconsSets.Moving(
            R.drawable.mc3a_sun_one_cloud_day, R.drawable.mc3an_sun_one_cloud_day,
            R.drawable.mc3fa_sun_one_cloud_day, R.drawable.mc3fan_sun_one_cloud_day
        ),
        R.drawable.mc3_sun_one_cloud_night to MeteoconsSets.Moving(
            R.drawable.mc3a_sun_one_cloud_night, R.drawable.mc3an_sun_one_cloud_night,
            R.drawable.mc3fa_sun_one_cloud_night, R.drawable.mc3fan_sun_one_cloud_night
        ),
        R.drawable.mc3_sun_half_cloud_day to MeteoconsSets.Moving(
            R.drawable.mc3a_sun_half_cloud_day, R.drawable.mc3an_sun_half_cloud_day,
            R.drawable.mc3fa_sun_half_cloud_day, R.drawable.mc3fan_sun_half_cloud_day
        ),
        R.drawable.mc3_sun_half_cloud_night to MeteoconsSets.Moving(
            R.drawable.mc3a_sun_half_cloud_night, R.drawable.mc3an_sun_half_cloud_night,
            R.drawable.mc3fa_sun_half_cloud_night, R.drawable.mc3fan_sun_half_cloud_night
        ),
    )

    /** Il disegno composto, per nome: la lista che i test camminano. */
    val byName: Map<String, Int> = mapOf(
        "sun-one-cloud-day" to R.drawable.mc3_sun_one_cloud_day,
        "sun-one-cloud-night" to R.drawable.mc3_sun_one_cloud_night,
        "sun-half-cloud-day" to R.drawable.mc3_sun_half_cloud_day,
        "sun-half-cloud-night" to R.drawable.mc3_sun_half_cloud_night,
    )

    /** Il disegno del «quasi sereno», di giorno e di notte. */
    @DrawableRes
    val mostlyClearDay: Int = R.drawable.mc3_sun_one_cloud_day

    @DrawableRes
    val mostlyClearNight: Int = R.drawable.mc3_sun_one_cloud_night

    /** Il disegno del «poco nuvoloso», di giorno e di notte. */
    @DrawableRes
    val partlyCloudyDay: Int = R.drawable.mc3_sun_half_cloud_day

    @DrawableRes
    val partlyCloudyNight: Int = R.drawable.mc3_sun_half_cloud_night
}
