package com.callbackdev.chiaro.widget

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The picker previews (8 set 2026), which no other test can reach: they are inflated by
 * the LAUNCHER, in a restricted context, on a screen the app never runs on. Nothing in
 * the toolchain checks them — a preview that fails to inflate simply shows nothing, and
 * the widget looks broken in the one place a reader meets it first.
 *
 * So the two things that can go wrong quietly are pinned here: a provider that forgot to
 * name its preview, and a preview that reaches for a view `RemoteViews` does not know.
 */
class WidgetPreviewTest {

    private val xml = File("src/main/res/xml")
    private val layout = File("src/main/res/layout")

    /** The set `RemoteViews` inflates, trimmed to what a static preview can want.
     * `Space` is deliberately absent: it is not on the platform's list, and it was the
     * first thing reached for when the Today strip needed to be pushed to the bottom. */
    private val remoteViewsTags = setOf(
        "FrameLayout", "LinearLayout", "RelativeLayout", "GridLayout",
        "TextView", "ImageView", "Button", "ImageButton", "ProgressBar",
        "AnalogClock", "Chronometer", "TextClock", "ViewFlipper", "ViewStub"
    )

    private val tag = Regex("""<([A-Za-z][A-Za-z0-9_.]*)[\s>]""")
    private val previewAttribute = Regex("""android:previewLayout="@layout/([a-z0-9_]+)"""")

    @Test
    fun `every widget provider names a preview layout that exists`() {
        val providers = xml.listFiles { file -> file.name.startsWith("widget_") }.orEmpty()
        assertTrue("no widget providers found at ${xml.absolutePath}", providers.size == 4)

        providers.forEach { provider ->
            val name = previewAttribute.find(provider.readText())?.groupValues?.get(1)
            assertTrue("${provider.name} declares no previewLayout", name != null)
            assertTrue(
                "${provider.name} points at a layout that is not there: $name",
                File(layout, "$name.xml").isFile
            )
        }
    }

    @Test
    fun `a preview layout only uses views RemoteViews knows`() {
        val previews = layout.listFiles { file -> file.name.endsWith("_preview.xml") }.orEmpty()
        assertEquals("one preview per widget", 4, previews.size)

        previews.forEach { preview ->
            val unsupported = tag.findAll(preview.readText())
                .map { it.groupValues[1] }
                .filterNot { it.startsWith("!") || it == "xml" }
                .filterNot { it in remoteViewsTags }
                .toSet()
            assertEquals("${preview.name} uses views RemoteViews cannot inflate", emptySet<String>(), unsupported)
        }
    }
}
