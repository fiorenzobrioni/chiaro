package com.callbackdev.chiaro.ui.icons

import com.callbackdev.chiaro.R

/**
 * I segni del cielo che **questo repo compone** dai disegni di Meteocons (14 ott 2026,
 * rivisti il 9 nov 2026): la meteora (la stella con le scie), la luna con la stella e le
 * due stelle delle congiunzioni, il sole sotto l'orizzonte con le prime stelle (l'ora
 * blu, le notti bianche), le tre stelle (il buio pieno, la Via Lattea), la luna piena che
 * sorge, l'eclissi di luna e la luce zodiacale. Il perche', con le misure, sta in testa
 * allo strumento.
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
        R.drawable.mc3_three_stars to R.drawable.mc3n_three_stars,
        R.drawable.mc3_full_moonrise to R.drawable.mc3n_full_moonrise,
        R.drawable.mc3_lunar_eclipse to R.drawable.mc3n_lunar_eclipse,
        R.drawable.mc3_zodiacal_light to R.drawable.mc3n_zodiacal_light,
    )

    /** flat, fondo chiaro. */
    val flatOf: Map<Int, Int> = mapOf(
        R.drawable.mc3_meteor to R.drawable.mc3f_meteor,
        R.drawable.mc3_moon_star to R.drawable.mc3f_moon_star,
        R.drawable.mc3_two_stars to R.drawable.mc3f_two_stars,
        R.drawable.mc3_horizon_star to R.drawable.mc3f_horizon_star,
        R.drawable.mc3_three_stars to R.drawable.mc3f_three_stars,
        R.drawable.mc3_full_moonrise to R.drawable.mc3f_full_moonrise,
        R.drawable.mc3_lunar_eclipse to R.drawable.mc3f_lunar_eclipse,
        R.drawable.mc3_zodiacal_light to R.drawable.mc3f_zodiacal_light,
    )

    /** flat, fondo scuro. */
    val flatDarkOf: Map<Int, Int> = mapOf(
        R.drawable.mc3_meteor to R.drawable.mc3fn_meteor,
        R.drawable.mc3_moon_star to R.drawable.mc3fn_moon_star,
        R.drawable.mc3_two_stars to R.drawable.mc3fn_two_stars,
        R.drawable.mc3_horizon_star to R.drawable.mc3fn_horizon_star,
        R.drawable.mc3_three_stars to R.drawable.mc3fn_three_stars,
        R.drawable.mc3_full_moonrise to R.drawable.mc3fn_full_moonrise,
        R.drawable.mc3_lunar_eclipse to R.drawable.mc3fn_lunar_eclipse,
        R.drawable.mc3_zodiacal_light to R.drawable.mc3fn_zodiacal_light,
    )

    /** Per nome, come [MeteoconsSets.byName]. */
    val byName: Map<String, Int> = mapOf(
        "meteor" to R.drawable.mc3_meteor,
        "moon-star" to R.drawable.mc3_moon_star,
        "two-stars" to R.drawable.mc3_two_stars,
        "horizon-star" to R.drawable.mc3_horizon_star,
        "three-stars" to R.drawable.mc3_three_stars,
        "full-moonrise" to R.drawable.mc3_full_moonrise,
        "lunar-eclipse" to R.drawable.mc3_lunar_eclipse,
        "zodiacal-light" to R.drawable.mc3_zodiacal_light,
    )
}
