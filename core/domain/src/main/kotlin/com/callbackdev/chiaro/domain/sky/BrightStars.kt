package com.callbackdev.chiaro.domain.sky

import java.io.DataInputStream

/**
 * The stars the canvas draws where they stand (DESIGN §3.9, step 6 of the sky review, 19 ott
 * 2026): every star of the Yale Bright Star Catalogue, 5th edition (Hoffleit & Warren 1991),
 * brighter than magnitude [LIMIT_MAG], sorted brightest first, J2000 right ascension and
 * declination in degrees, visual magnitude, B-V colour (0 where the catalogue has none).
 * Precession since J2000 moves a star by about a third of a degree, under what a dp of the
 * canvas holds, and is not applied.
 *
 * The data is `bright_stars.bin` at [RESOURCE] on the classpath, written by
 * `tools/gen_bright_stars.py` and never by hand (20 ott 2026: at magnitude 6.0 the catalogue
 * is 5 080 stars, over what a Kotlin array of literals may hold in one static initialiser), and
 * read once, on first use: four 16-bit fields a star after a four-byte magic, a version and a
 * count, the layout the tool's docstring gives.
 *
 * Looked up by its ABSOLUTE path, never relative to this class (the night crash of 20 ott
 * 2026, from the device): the minified build's R8 repackages this class (`j7.o` in that
 * build's mapping) while the resource stays where the module put it, so a name resolved
 * against the class' package pointed at `j7/bright_stars.bin`, found nothing, and the first
 * place where it was night brought the app down. The debug build, not minified, never showed
 * it, and no unit test runs under R8.
 */
object BrightStars {
    const val LIMIT_MAG = 6.0f

    /** The catalogue's place in the jar and in the APK, absolute (leading slash) on purpose. */
    const val RESOURCE = "/com/callbackdev/chiaro/domain/sky/bright_stars.bin"

    val count: Int get() = mag.size
    val raDeg: FloatArray get() = table.ra
    val decDeg: FloatArray get() = table.dec
    val mag: FloatArray get() = table.mag
    val bv: FloatArray get() = table.bv

    private class Table(val ra: FloatArray, val dec: FloatArray, val mag: FloatArray, val bv: FloatArray)

    private val table: Table by lazy { read() }

    private fun read(): Table {
        val stream = BrightStars::class.java.getResourceAsStream(RESOURCE)
            ?: error("$RESOURCE is not on the classpath: run tools/gen_bright_stars.py")
        DataInputStream(stream.buffered()).use { input ->
            val magic = ByteArray(4).also { input.readFully(it) }
            require(String(magic, Charsets.US_ASCII) == "CHST") { "bright_stars.bin: not a star catalogue" }
            val version = input.readUnsignedShort()
            require(version == 1) { "bright_stars.bin: version $version, this reader knows 1" }
            val n = input.readUnsignedShort()
            val ra = FloatArray(n)
            val dec = FloatArray(n)
            val mag = FloatArray(n)
            val bv = FloatArray(n)
            for (i in 0 until n) {
                ra[i] = input.readUnsignedShort() / 65535f * 360f
                dec[i] = input.readShort() / 32767f * 90f
                mag[i] = input.readShort() / 1000f
                bv[i] = input.readShort() / 1000f
            }
            return Table(ra, dec, mag, bv)
        }
    }
}
