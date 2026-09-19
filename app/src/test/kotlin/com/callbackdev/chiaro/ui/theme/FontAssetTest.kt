package com.callbackdev.chiaro.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.ResourceFont
import com.callbackdev.chiaro.R
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The two bundled families, checked against the FILES they are drawn from.
 *
 * A `FontFamily` is a list of promises: this weight, from this resource. Nothing in the
 * toolchain checks that the file can keep them — ask for a weight outside the variable
 * font's axis and Android does not fail, it renders the nearest one it has (or, above
 * the maximum, smears an outline into a fake bold). So the promise and the file drift
 * apart in silence, which is the one way a typography bug reaches a reader.
 *
 * Three things are therefore read straight out of the `.ttf` here:
 *
 * 1. **The `wght` axis covers every weight its family declares.** Google Sans starts at
 *    400 and Inter at 100, so this is not a formality: it is exactly why the Google Sans
 *    family declares four faces where Inter declares five (`Type.kt`).
 * 2. **`tnum` is there.** DESIGN §5 says every figure in a column is tabular, and the
 *    request is ignored without a word by a face that has no tabular figures. It is the
 *    property that decided Google Sans was admissible at all.
 * 3. **The file is not bigger than it was meant to be**, so nobody drops a full 5 MB
 *    upstream build into `res/font/` by hand — `tools/import_google_sans.py` cuts it to
 *    what this app prints, and the cap is what the cut is worth.
 */
class FontAssetTest {

    private val fonts = File("src/main/res/font")
    private val licenses = File("../licenses")

    /** Every bundled family, with the file it is drawn from and what that file may weigh.
     * Inter is the upstream publication as its author ships it; Google Sans is the cut
     * this repo makes, and 400 KB leaves room for a script or two without leaving room
     * for the whole 5 MB original. */
    private val bundled = listOf(
        Bundle(InterFamily, R.font.inter_variable, "inter_variable.ttf", 1_000_000, "Inter-OFL.txt"),
        Bundle(
            GoogleSansFamily, R.font.google_sans_variable, "google_sans_variable.ttf",
            400_000, "GoogleSans-OFL.txt"
        )
    )

    private data class Bundle(
        val family: FontFamily,
        val resId: Int,
        val file: String,
        val maxBytes: Int,
        val license: String
    )

    @Test
    fun `a family is one file, and it is the file it says`() {
        bundled.forEach { bundle ->
            val ids = declared(bundle.family).map { (it as ResourceFont).resId }.toSet()
            assertEquals("${bundle.file} family", setOf(bundle.resId), ids)
        }
    }

    @Test
    fun `every declared weight is inside the file's own axis`() {
        bundled.forEach { bundle ->
            val axis = wghtAxis(File(fonts, bundle.file))
            declared(bundle.family).forEach { font ->
                val weight = font.weight.weight
                assertTrue(
                    "${bundle.file} declares $weight, axis is ${axis.first}..${axis.second}",
                    weight >= axis.first && weight <= axis.second
                )
            }
        }
    }

    /** DESIGN §5, and the reason this app can offer a second face at all. */
    @Test
    fun `both bundled faces carry tabular figures`() {
        bundled.forEach { bundle ->
            assertTrue(
                "${bundle.file} has no tnum",
                features(File(fonts, bundle.file)).contains("tnum")
            )
        }
    }

    @Test
    fun `the files are there, within their cap, with their licence beside them`() {
        bundled.forEach { bundle ->
            val file = File(fonts, bundle.file)
            assertTrue("missing ${file.path}", file.isFile)
            assertTrue(
                "${bundle.file} is ${file.length()} bytes, cap is ${bundle.maxBytes}",
                file.length() <= bundle.maxBytes
            )
            assertTrue(
                "missing licenses/${bundle.license}",
                File(licenses, bundle.license).isFile
            )
        }
    }

    private fun declared(family: FontFamily): List<Font> {
        @Suppress("UNCHECKED_CAST")
        return family as List<Font>
    }

    // --- just enough of the sfnt format to ask a file what it can draw ----------------
    //
    // A font is a table directory and then the tables: 12 bytes of header (the count of
    // tables is at offset 4), then one 16-byte record per table — tag, checksum, offset,
    // length. Everything below is two reads into that.

    private fun tables(file: File): Map<String, ByteArray> {
        val bytes = file.readBytes()
        val count = bytes.u16(4)
        return (0 until count).associate { i ->
            val record = 12 + i * 16
            val tag = String(bytes, record, 4, Charsets.US_ASCII)
            val offset = bytes.u32(record + 8)
            val length = bytes.u32(record + 12)
            tag to bytes.copyOfRange(offset, offset + length)
        }
    }

    /** `fvar`: a small header whose axes start at the offset it names, then 20 bytes per
     * axis — tag, min, default, max as 16.16 fixed, then flags and a name id. */
    private fun wghtAxis(file: File): Pair<Int, Int> {
        val fvar = requireNotNull(tables(file)["fvar"]) { "${file.name} is not a variable font" }
        val axesAt = fvar.u16(4)
        val axisCount = fvar.u16(8)
        val axisSize = fvar.u16(10)
        repeat(axisCount) { i ->
            val at = axesAt + i * axisSize
            if (String(fvar, at, 4, Charsets.US_ASCII) == "wght") {
                return fvar.fixed(at + 4) to fvar.fixed(at + 12)
            }
        }
        throw AssertionError("${file.name} has no wght axis")
    }

    /** The feature tags a file offers, read off the FeatureList of `GSUB` and `GPOS`:
     * count, then one record per feature of tag and offset. Only the tags are wanted. */
    private fun features(file: File): Set<String> {
        val found = mutableSetOf<String>()
        val tables = tables(file)
        listOf("GSUB", "GPOS").forEach { name ->
            val table = tables[name] ?: return@forEach
            val listAt = table.u16(6)
            val count = table.u16(listAt)
            repeat(count) { i ->
                found += String(table, listAt + 2 + i * 6, 4, Charsets.US_ASCII)
            }
        }
        return found
    }

    private fun ByteArray.u16(at: Int): Int =
        ((this[at].toInt() and 0xFF) shl 8) or (this[at + 1].toInt() and 0xFF)

    private fun ByteArray.u32(at: Int): Int =
        (u16(at) shl 16) or u16(at + 2)

    /** 16.16 fixed point; every axis value in a real font is a whole number of units. */
    private fun ByteArray.fixed(at: Int): Int = u16(at)
}
