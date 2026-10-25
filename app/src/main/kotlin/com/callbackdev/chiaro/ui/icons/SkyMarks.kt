package com.callbackdev.chiaro.ui.icons

import com.callbackdev.chiaro.R

/**
 * I segni del cielo che **questo repo compone** dai disegni di Meteocons (14 ott 2026):
 * la meteora (le stelle cadenti senza la luna), la luna con la stella e le due stelle
 * delle congiunzioni, e il sole sotto l'orizzonte con la prima stella dell'ora blu
 * (25 ott 2026). Il perche', con le misure, sta in testa allo strumento.
 *
 * **File generato da `tools/compose_sky_marks.py` — non si modifica a mano.** Ha la
 * forma di [ComposedIcons] (la chiave e' l'id del set line su fondo chiaro), senza
 * gemelli animati: nel catalogo del cielo non si muove niente.
 */
internal object SkyMarks {

    /** line, fondo scuro. */
    val lineDarkOf: Map<Int, Int> = mapOf(
        R.drawable.mc3_meteor to R.drawable.mc3n_meteor,
        R.drawable.mc3_moon_star to R.drawable.mc3n_moon_star,
        R.drawable.mc3_two_stars to R.drawable.mc3n_two_stars,
        R.drawable.mc3_horizon_star to R.drawable.mc3n_horizon_star,
    )

    /** flat, fondo chiaro. */
    val flatOf: Map<Int, Int> = mapOf(
        R.drawable.mc3_meteor to R.drawable.mc3f_meteor,
        R.drawable.mc3_moon_star to R.drawable.mc3f_moon_star,
        R.drawable.mc3_two_stars to R.drawable.mc3f_two_stars,
        R.drawable.mc3_horizon_star to R.drawable.mc3f_horizon_star,
    )

    /** flat, fondo scuro. */
    val flatDarkOf: Map<Int, Int> = mapOf(
        R.drawable.mc3_meteor to R.drawable.mc3fn_meteor,
        R.drawable.mc3_moon_star to R.drawable.mc3fn_moon_star,
        R.drawable.mc3_two_stars to R.drawable.mc3fn_two_stars,
        R.drawable.mc3_horizon_star to R.drawable.mc3fn_horizon_star,
    )

    /** Per nome, come [MeteoconsSets.byName]. */
    val byName: Map<String, Int> = mapOf(
        "meteor" to R.drawable.mc3_meteor,
        "moon-star" to R.drawable.mc3_moon_star,
        "two-stars" to R.drawable.mc3_two_stars,
        "horizon-star" to R.drawable.mc3_horizon_star,
    )
}
