package com.callbackdev.chiaro.widget.window

import androidx.annotation.StringRes
import com.callbackdev.chiaro.R

/**
 * The stars the window may name (20 ott 2026): the eighteen brightest with a name a reader
 * has heard, J2000 right ascension and declination in degrees and visual magnitude, from the
 * same catalogue the canvas draws (Yale BSC5). The names are strings, because Sirius is
 * Sirio and Procyon Procione; the positions are the engine's, not the catalogue's index,
 * so the list does not depend on how the binary is sorted. Two at most are written on a
 * card, the brightest in its window.
 */
internal data class NamedStar(
    @StringRes val nameRes: Int,
    val raDeg: Double,
    val decDeg: Double,
    val mag: Double
)

internal val NamedStars: List<NamedStar> = listOf(
    NamedStar(R.string.star_sirius, 101.287, -16.716, -1.46),
    NamedStar(R.string.star_canopus, 95.988, -52.696, -0.74),
    NamedStar(R.string.star_arcturus, 213.915, 19.182, -0.05),
    NamedStar(R.string.star_vega, 279.235, 38.784, 0.03),
    NamedStar(R.string.star_capella, 79.172, 45.998, 0.08),
    NamedStar(R.string.star_rigel, 78.634, -8.202, 0.13),
    NamedStar(R.string.star_procyon, 114.826, 5.225, 0.34),
    NamedStar(R.string.star_achernar, 24.429, -57.237, 0.46),
    NamedStar(R.string.star_betelgeuse, 88.793, 7.407, 0.50),
    NamedStar(R.string.star_altair, 297.696, 8.868, 0.77),
    NamedStar(R.string.star_aldebaran, 68.980, 16.509, 0.85),
    NamedStar(R.string.star_spica, 201.298, -11.161, 1.04),
    NamedStar(R.string.star_antares, 247.352, -26.432, 1.09),
    NamedStar(R.string.star_pollux, 116.329, 28.026, 1.14),
    NamedStar(R.string.star_fomalhaut, 344.413, -29.622, 1.16),
    NamedStar(R.string.star_deneb, 310.358, 45.280, 1.25),
    NamedStar(R.string.star_regulus, 152.093, 11.967, 1.35),
    NamedStar(R.string.star_castor, 113.650, 31.888, 1.58)
)
