package com.callbackdev.chiaro.ui.components

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Every bottom sheet goes through [ChiaroSheet] (device, 17 ott 2026). A sheet built on
 * Material's own takes the top inset by the part of the status bar it is under, and one that
 * comes to rest inside that band bounces until it is touched: found on the Places sheet, fixed
 * there alone, and found again two days later on the next quarters' page. A sweep rather than
 * a review, because the next sheet will be written from Material's sample, not from this one.
 */
class OneSheetTest {

    private val direct = Regex("""\bModalBottomSheet\(""")

    @Test
    fun `no ModalBottomSheet outside ChiaroSheet`() {
        val root = File("src/main/kotlin/com/callbackdev/chiaro")
        assertTrue("the sweep found no sources to read at ${root.absolutePath}", root.isDirectory)

        val offenders = root.walkTopDown()
            .filter { it.isFile && it.extension == "kt" && it.name != "ChiaroSheet.kt" }
            .flatMap { file ->
                file.readLines().withIndex()
                    .filter { (_, line) -> direct.containsMatchIn(line) }
                    .map { (i, line) -> "${file.path}:${i + 1}  ${line.trim()}" }
            }
            .toList()

        assertTrue(
            "a bottom sheet is a ChiaroSheet, not Material's own:\n" + offenders.joinToString("\n"),
            offenders.isEmpty()
        )
    }
}
