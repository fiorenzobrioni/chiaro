package com.callbackdev.chiaro.domain.sky

import java.io.DataInputStream

/**
 * One of the eighty-eight constellations, as a drawing (Fase 16, step 16.5): the lines
 * that join its stars and where its name is written. Nothing a screen prints: the name is
 * a string where the screen is, looked up by [id], like a star's and a figure's.
 *
 * [id] is the IAU's abbreviation, three letters in its own capitals («UMa», «CrB»): what
 * [SkyFigure.constellation] already files a figure under.
 *
 * [rank] is the source's, one to three: 1 for the ones a reader looks for first (Orion,
 * the Great Bear, the Scorpion), 3 for the faint ones nobody finds without the lines. It
 * is there so that a sky a whole screen wide can write twenty-two names and not
 * eighty-eight; it sorts, and is never printed.
 *
 * [labels] is where the name goes, J2000: one point, or two for the Serpent, which is one
 * constellation in two pieces either side of the Serpent Bearer and is named on each.
 *
 * [lines] is the drawing, four numbers a segment: right ascension and declination of one
 * end, then of the other, J2000, in degrees. **Every end is a star of [BrightStars], to
 * the last bit**: the tool moves each vertex of its source onto the catalogue star it
 * stands on and writes it in the catalogue's own fields, so a line ends on a point the
 * canvas draws and not beside it (`ConstellationsTest` holds every end to the catalogue).
 * A segment is the short way round between its two stars.
 */
class Constellation(
    val id: String,
    val rank: Int,
    val labels: List<SkyTarget.Fixed>,
    val lines: FloatArray
) {
    val segmentCount: Int get() = lines.size / FLOATS_A_SEGMENT

    companion object {
        /** How many numbers of [lines] a segment takes: two ends, two coordinates each. */
        const val FLOATS_A_SEGMENT = 4
    }
}

/**
 * The lines and the name points of the eighty-eight (Fase 16, step 16.5, 10 nov 2026), for
 * the explorer to draw from step 16.7.
 *
 * **Where they come from.** d3-celestial by Olaf Frohn (BSD 3-Clause, read at its source on
 * 10 Nov 2026): the lines of the IAU's charts, with its author's own changes, and the
 * points its names are written at. Chosen over Stellarium's «modern» sky culture, read
 * the same day, for two reasons the tool's docstring gives in full: that one's licence
 * line names no version (only 4.0 of CC BY-SA may be taken into GPL-3), and its lines are
 * Hipparcos numbers, which need a second catalogue to become places. The credit is a row
 * of Settings and a line of the README, and the notice is in `licenses/`.
 *
 * **What is not taken.** The names: they are the app's strings, in its two languages. The
 * places of the stars: those are the Yale catalogue's, the ones the canvas already draws.
 * The mythological figures: decided out with the phase.
 *
 * The data is `constellations.bin` at [RESOURCE], written by `tools/gen_constellations.py`
 * and never by hand, read once, on first use; the layout is in the tool's docstring.
 * Looked up by its ABSOLUTE path for the reason [BrightStars] gives: R8 moves this class
 * and leaves the resource where the module put it.
 */
object Constellations {

    /** The file's place in the jar and in the APK, absolute (leading slash) on purpose. */
    const val RESOURCE = "/com/callbackdev/chiaro/domain/sky/constellations.bin"

    /** How many there are: the IAU's list of 1922, unchanged since. */
    const val COUNT = 88

    /** All of them, in the order of their Latin names: Andromeda first, Vulpecula last. */
    val all: List<Constellation> by lazy { read() }

    private val byId: Map<String, Constellation> by lazy { all.associateBy { it.id } }

    /** The one with the IAU abbreviation [id], capitals as the IAU writes them. */
    fun byId(id: String): Constellation? = byId[id]

    private fun read(): List<Constellation> {
        val stream = Constellations::class.java.getResourceAsStream(RESOURCE)
            ?: error("$RESOURCE is not on the classpath: run tools/gen_constellations.py")
        DataInputStream(stream.buffered()).use { input ->
            val magic = ByteArray(4).also { input.readFully(it) }
            require(String(magic, Charsets.US_ASCII) == "CHCN") { "constellations.bin: not the constellations" }
            val version = input.readUnsignedShort()
            require(version == 1) { "constellations.bin: version $version, this reader knows 1" }
            val n = input.readUnsignedShort()
            // The two fields of a point, decoded exactly as [BrightStars] decodes a star's:
            // the same expression on the same sixteen bits, so the same float.
            fun ra() = input.readUnsignedShort() / 65535f * 360f
            fun dec() = input.readShort() / 32767f * 90f
            return List(n) {
                val id = String(ByteArray(3).also { input.readFully(it) }, Charsets.US_ASCII)
                val rank = input.readUnsignedByte()
                val labels = List(input.readUnsignedByte()) {
                    val raDeg = ra()
                    SkyTarget.Fixed(raDeg.toDouble(), dec().toDouble())
                }
                val lines = FloatArray(input.readUnsignedShort() * Constellation.FLOATS_A_SEGMENT)
                for (i in lines.indices step 2) {
                    lines[i] = ra()
                    lines[i + 1] = dec()
                }
                Constellation(id, rank, labels, lines)
            }
        }
    }
}
