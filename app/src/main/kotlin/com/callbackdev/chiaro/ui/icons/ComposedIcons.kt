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
 * Oggi ce n'e' uno solo, il «quasi sereno»: il sereno di Meteocons intatto piu' una
 * nuvoletta nell'angolo, perche' il disegno che la libreria chiama `mostly-clear`
 * porta il 72% della nuvola del «poco nuvoloso» per un cielo che ne ha il 39% di
 * copertura. Il perche' per esteso, con le misure, sta in testa allo strumento.
 */
internal object ComposedIcons {

    /** line, fondo scuro. */
    val lineDarkOf: Map<Int, Int> = mapOf(
        R.drawable.mc3_sun_one_cloud_day to R.drawable.mc3n_sun_one_cloud_day,
        R.drawable.mc3_sun_one_cloud_night to R.drawable.mc3n_sun_one_cloud_night,
    )

    /** flat, fondo chiaro. */
    val flatOf: Map<Int, Int> = mapOf(
        R.drawable.mc3_sun_one_cloud_day to R.drawable.mc3f_sun_one_cloud_day,
        R.drawable.mc3_sun_one_cloud_night to R.drawable.mc3f_sun_one_cloud_night,
    )

    /** flat, fondo scuro. */
    val flatDarkOf: Map<Int, Int> = mapOf(
        R.drawable.mc3_sun_one_cloud_day to R.drawable.mc3fn_sun_one_cloud_day,
        R.drawable.mc3_sun_one_cloud_night to R.drawable.mc3fn_sun_one_cloud_night,
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
    )

    /** Il disegno composto, per nome: la lista che i test camminano. */
    val byName: Map<String, Int> = mapOf(
        "sun-one-cloud-day" to R.drawable.mc3_sun_one_cloud_day,
        "sun-one-cloud-night" to R.drawable.mc3_sun_one_cloud_night,
    )

    /** Il disegno del «quasi sereno», di giorno e di notte. */
    @DrawableRes
    val mostlyClearDay: Int = R.drawable.mc3_sun_one_cloud_day

    @DrawableRes
    val mostlyClearNight: Int = R.drawable.mc3_sun_one_cloud_night
}
