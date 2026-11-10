package com.callbackdev.chiaro.domain.sky

import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** What a [SkyFigure] is, for the two words a row says before where it stands. */
enum class SkyFigureKind {
    /** One of the eighty-eight, whole: Orion, Cassiopeia. */
    CONSTELLATION,

    /** A shape people know that is not one of the eighty-eight: a part of one (the Plough
     * is seven stars of the Great Bear) or stars of several (the Summer Triangle). */
    SHAPE,

    /** Stars that really are together, close enough to read as one thing. */
    CLUSTER
}

/**
 * A figure of stars a person finds without a chart (Fase 16, step 16.4): the stars that
 * draw it, and nothing a screen prints. Its name is a string where the screen is, like a
 * star's ([id] is what it is looked up by).
 *
 * [stars] are the ones that make the shape, J2000, **each one a star of [BrightStars]**:
 * `SkyFiguresTest` finds every one in the catalogue the canvas draws, so a figure the
 * list sends a reader out for is made of points the app itself puts in the sky.
 * [constellation] is the IAU's three letters for the constellation the figure is, or
 * lies in: what the lines and the names of step 16.5 are filed under.
 */
data class SkyFigure(
    val id: String,
    val kind: SkyFigureKind,
    val constellation: String,
    val stars: List<SkyTarget.Fixed>
) {
    /**
     * The middle of the figure: the direction its stars point to on average, which is
     * where a camera turns to hold all of them. The mean of the directions and not of the
     * coordinates, because right ascension wraps: the Great Square sits across 0°, and the
     * plain mean of 346° and 2° is a point on the other side of the sky.
     */
    val centre: SkyTarget.Fixed by lazy {
        var x = 0.0
        var y = 0.0
        var z = 0.0
        stars.forEach { star ->
            val ra = Math.toRadians(star.raDeg)
            val dec = Math.toRadians(star.decDeg)
            x += cos(dec) * cos(ra)
            y += cos(dec) * sin(ra)
            z += sin(dec)
        }
        val length = sqrt(x * x + y * y + z * z)
        SkyTarget.Fixed(
            raDeg = AstronomyMath.norm360(Math.toDegrees(atan2(y, x))),
            decDeg = Math.toDegrees(asin(z / length))
        )
    }
}

/**
 * The figures the «worth seeing» list may name ([WorthSeeing]), and **why these eleven**.
 *
 * A figure is here when a person who has never opened a star chart can be told «look
 * there» and find it: a shape with a name people have heard, drawn by stars brighter
 * than magnitude 3.5, which is what a town's sky still shows (the faintest are Megrez in
 * the Plough and Segin in the W, each the dim corner of a shape the others already draw),
 * or a cluster tight enough to read as one thing. That is a short list on purpose. The eighty-eight constellations are step 16.5's, for the
 * explorer to draw; most of them are an outline nobody finds without the lines, and a
 * list that sent a reader out for the Lynx would be the app filling rows because it can.
 *
 * Chosen so that every season of either hemisphere has something: the winter of the
 * north (Orion, the Twins, the Pleiades and the Hyades), its spring (the Lion, the Plough
 * high), its summer (the Summer Triangle, the Scorpion from where it clears the horizon),
 * its autumn (the Great Square, Cassiopeia high), and for the south the Cross and the
 * Scorpion overhead.
 *
 * **Left out, each for a reason.** The Swan and the Lyre, because the Summer Triangle is
 * their brightest stars and two rows for one patch of sky is one row too many. The Little
 * Bear, because only the Pole Star of it survives a town. The Teapot of the Archer,
 * because where it stands the list already has the Milky Way's core. Single stars
 * (Sirius, Arcturus): a star is a point the explorer names, not a figure. Anything that
 * wants binoculars: the phase's own rule keeps the faint catalogues out.
 *
 * The places are the Yale catalogue's, read from `bright_stars.bin` on 10 Nov 2026 and
 * held to it by the test.
 */
object SkyFigures {

    private fun star(raDeg: Double, decDeg: Double) = SkyTarget.Fixed(raDeg, decDeg)

    /** The seven of the Plough, bowl first: Dubhe, Merak, Phecda, Megrez, Alioth, Mizar, Alkaid. */
    val BigDipper = SkyFigure(
        "big_dipper", SkyFigureKind.SHAPE, "UMa",
        listOf(
            star(165.935, 61.751), star(165.462, 56.384), star(178.459, 53.695), star(183.859, 57.032),
            star(193.505, 55.961), star(200.981, 54.925), star(206.887, 49.314)
        )
    )

    /** The W: Caph, Schedar, gamma, Ruchbah, Segin. */
    val Cassiopeia = SkyFigure(
        "cassiopeia", SkyFigureKind.CONSTELLATION, "Cas",
        listOf(
            star(2.296, 59.149), star(10.130, 56.537), star(14.178, 60.718), star(21.457, 60.234),
            star(28.598, 63.670)
        )
    )

    /** The Great Square of Pegasus: Markab, Scheat, Algenib, and Alpheratz, which is Andromeda's. */
    val GreatSquare = SkyFigure(
        "great_square", SkyFigureKind.SHAPE, "Peg",
        listOf(star(346.190, 15.206), star(345.943, 28.082), star(3.307, 15.184), star(2.098, 29.090))
    )

    /** The Pleiades, by their brightest: Alcyone. Six or seven points in a thumb's width. */
    val Pleiades = SkyFigure("pleiades", SkyFigureKind.CLUSTER, "Tau", listOf(star(56.872, 24.105)))

    /** The V of the Bull: Aldebaran (in front of the cluster, not of it), Ain, gamma, delta and theta Tauri. */
    val Hyades = SkyFigure(
        "hyades", SkyFigureKind.CLUSTER, "Tau",
        listOf(
            star(68.979, 16.510), star(67.155, 19.180), star(64.947, 15.629), star(65.732, 17.543),
            star(67.166, 15.870)
        )
    )

    /** Shoulders, belt and feet: Betelgeuse, Bellatrix, Alnitak, Alnilam, Mintaka, Saiph, Rigel. */
    val Orion = SkyFigure(
        "orion", SkyFigureKind.CONSTELLATION, "Ori",
        listOf(
            star(88.793, 7.408), star(81.284, 6.350), star(85.189, -1.942), star(84.052, -1.203),
            star(83.003, -0.299), star(86.942, -9.671), star(78.636, -8.202)
        )
    )

    /** The two heads and a foot: Castor, Pollux, Alhena. */
    val Twins = SkyFigure(
        "twins", SkyFigureKind.CONSTELLATION, "Gem",
        listOf(star(113.650, 31.889), star(116.331, 28.027), star(99.428, 16.400))
    )

    /** The sickle's foot and neck, the back and the tail: Regulus, Algieba, Zosma, Denebola. */
    val Lion = SkyFigure(
        "lion", SkyFigureKind.CONSTELLATION, "Leo",
        listOf(star(152.092, 11.967), star(154.992, 19.842), star(168.527, 20.523), star(177.267, 14.571))
    )

    /** Three first-magnitude stars of three constellations: Vega, Deneb, Altair. Filed
     * under the Swan, whose Deneb is its far corner. */
    val SummerTriangle = SkyFigure(
        "summer_triangle", SkyFigureKind.SHAPE, "Cyg",
        listOf(star(279.233, 38.783), star(310.358, 45.282), star(297.696, 8.869))
    )

    /** Claws, heart and sting: Acrab, Dschubba, Antares, epsilon, Shaula. The sting is what
     * keeps it off the list north of about 43 degrees: there it never clears the roofs. */
    val Scorpion = SkyFigure(
        "scorpion", SkyFigureKind.CONSTELLATION, "Sco",
        listOf(
            star(241.357, -19.806), star(240.082, -22.622), star(247.350, -26.431), star(252.541, -34.292),
            star(263.401, -37.105)
        )
    )

    /** Acrux, Mimosa, Gacrux, Imai. */
    val SouthernCross = SkyFigure(
        "southern_cross", SkyFigureKind.CONSTELLATION, "Cru",
        listOf(star(186.650, -63.099), star(191.929, -59.688), star(187.792, -57.114), star(183.788, -58.748))
    )

    /** In the order the sky turns them past, by the right ascension of their middle. */
    val all: List<SkyFigure> = listOf(
        Cassiopeia, Pleiades, Hyades, Orion, Twins, Lion, BigDipper, SouthernCross, Scorpion,
        SummerTriangle, GreatSquare
    )

    fun byId(id: String): SkyFigure? = all.firstOrNull { it.id == id }
}
