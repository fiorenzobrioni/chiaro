package com.callbackdev.chiaro.domain

/**
 * Whether a place reads European scales (24 set 2026): today only the air index, which
 * the European Environment Agency publishes for Europe and Open-Meteo serves everywhere.
 *
 * By ISO country code and nothing else. A bounding box was the obvious shortcut and is
 * wrong at its first edge — Tunis and Algiers sit inside any box that holds Sicily — and
 * a place without a code (a hand-built City, an old saved one) keeps the scale it always
 * had rather than having one guessed for it. Turkey and Russia are left out: most of
 * their places are not in Europe, and the reader there has a national scale of its own.
 */
object PlaceRegion {

    private val Europe = setOf(
        "AD", "AL", "AT", "BA", "BE", "BG", "BY", "CH", "CY", "CZ", "DE", "DK", "EE", "ES",
        "FI", "FO", "FR", "GB", "GG", "GI", "GR", "HR", "HU", "IE", "IM", "IS", "IT", "JE",
        "LI", "LT", "LU", "LV", "MC", "MD", "ME", "MK", "MT", "NL", "NO", "PL", "PT", "RO",
        "RS", "SE", "SI", "SJ", "SK", "SM", "UA", "VA", "XK", "AX"
    )

    fun inEurope(countryCode: String?): Boolean =
        countryCode?.uppercase()?.let { it in Europe } == true
}
