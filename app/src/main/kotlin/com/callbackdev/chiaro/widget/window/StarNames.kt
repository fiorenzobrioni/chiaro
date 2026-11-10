package com.callbackdev.chiaro.widget.window

import androidx.annotation.StringRes
import com.callbackdev.chiaro.R

/**
 * The stars the app can name: J2000 right ascension and declination in degrees and visual
 * magnitude, at the places the catalogue the canvas draws has them (Yale BSC5;
 * `StarNamesTest` finds each one there). The names are strings, because Sirius is Sirio
 * and Procyon Procione; the positions are the engine's, not the catalogue's index, so the
 * list does not depend on how the binary is sorted. The magnitude is the one the eye
 * sees, both halves of a double together (Alpha Centauri, Acrux, Castor), and only sorts.
 *
 * **Thirty-one since Fase 16.5** (10 nov 2026), for the explorer: the thirty brightest of
 * the whole sky, which end at Alnair, and the Pole Star, which is about the fiftieth and
 * the one a reader looks for by name.
 *
 * [onWindow] marks the eighteen the window may write (20 ott 2026): the brightest with a
 * name a reader has heard, two at most on a card, the brightest in its window
 * ([WindowStars]). The thirteen that joined are the explorer's, where a reader asks «what
 * is that one»; on a card of two names, «Miaplacidus» would be one of them.
 */
internal data class NamedStar(
    @StringRes val nameRes: Int,
    val raDeg: Double,
    val decDeg: Double,
    val mag: Double,
    val onWindow: Boolean = true
)

internal val NamedStars: List<NamedStar> = listOf(
    NamedStar(R.string.star_sirius, 101.287, -16.716, -1.46),
    NamedStar(R.string.star_canopus, 95.988, -52.696, -0.74),
    NamedStar(R.string.star_alpha_centauri, 219.900, -60.835, -0.27, onWindow = false),
    NamedStar(R.string.star_arcturus, 213.915, 19.182, -0.05),
    NamedStar(R.string.star_vega, 279.235, 38.784, 0.03),
    NamedStar(R.string.star_capella, 79.172, 45.998, 0.08),
    NamedStar(R.string.star_rigel, 78.634, -8.202, 0.13),
    NamedStar(R.string.star_procyon, 114.826, 5.225, 0.34),
    NamedStar(R.string.star_achernar, 24.429, -57.237, 0.46),
    NamedStar(R.string.star_betelgeuse, 88.793, 7.407, 0.50),
    NamedStar(R.string.star_hadar, 210.956, -60.373, 0.61, onWindow = false),
    NamedStar(R.string.star_altair, 297.696, 8.868, 0.77),
    NamedStar(R.string.star_acrux, 186.650, -63.099, 0.77, onWindow = false),
    NamedStar(R.string.star_aldebaran, 68.980, 16.509, 0.85),
    NamedStar(R.string.star_spica, 201.298, -11.161, 1.04),
    NamedStar(R.string.star_antares, 247.352, -26.432, 1.09),
    NamedStar(R.string.star_pollux, 116.329, 28.026, 1.14),
    NamedStar(R.string.star_fomalhaut, 344.413, -29.622, 1.16),
    NamedStar(R.string.star_deneb, 310.358, 45.280, 1.25),
    NamedStar(R.string.star_mimosa, 191.930, -59.689, 1.25, onWindow = false),
    NamedStar(R.string.star_regulus, 152.093, 11.967, 1.35),
    NamedStar(R.string.star_adhara, 104.656, -28.972, 1.50, onWindow = false),
    NamedStar(R.string.star_castor, 113.650, 31.888, 1.58),
    NamedStar(R.string.star_shaula, 263.402, -37.104, 1.63, onWindow = false),
    NamedStar(R.string.star_gacrux, 187.791, -57.113, 1.63, onWindow = false),
    NamedStar(R.string.star_bellatrix, 81.283, 6.350, 1.64, onWindow = false),
    NamedStar(R.string.star_elnath, 81.573, 28.608, 1.65, onWindow = false),
    NamedStar(R.string.star_miaplacidus, 138.300, -69.717, 1.68, onWindow = false),
    NamedStar(R.string.star_alnilam, 84.053, -1.202, 1.70, onWindow = false),
    NamedStar(R.string.star_alnair, 332.058, -46.961, 1.74, onWindow = false),
    NamedStar(R.string.star_polaris, 37.953, 89.264, 2.02, onWindow = false)
)

/** The ones the window writes: the eighteen it has had since 20 ott 2026, brightest first. */
internal val WindowStars: List<NamedStar> = NamedStars.filter { it.onWindow }
