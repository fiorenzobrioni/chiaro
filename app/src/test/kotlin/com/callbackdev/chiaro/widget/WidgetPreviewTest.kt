package com.callbackdev.chiaro.widget

import androidx.compose.ui.graphics.toArgb
import com.callbackdev.chiaro.ui.theme.widgetCardContainer
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
 *
 * The counts are the household's: five providers and five previews since the text widget
 * landed (19 set 2026). They are asserted rather than derived so that a provider added
 * without a preview fails here rather than on somebody's home screen.
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
        assertTrue("no widget providers found at ${xml.absolutePath}", providers.size == 5)

        providers.forEach { provider ->
            val name = previewAttribute.find(provider.readText())?.groupValues?.get(1)
            assertTrue("${provider.name} declares no previewLayout", name != null)
            assertTrue(
                "${provider.name} points at a layout that is not there: $name",
                File(layout, "$name.xml").isFile
            )
        }
    }

    /**
     * Three in the picker (13 ott 2026, the widget review): «The hours ahead» joined «At a
     * glance» and «Sky moments» joined the day's arc, whose place the window took on 20 ott
     * 2026. The two retired providers stay
     * declared, so the cards already on home screens keep working, and are hidden from the
     * picker; a third hidden one, or one of the three hidden, fails here.
     */
    @Test
    fun `the picker offers three cards and hides the two that joined them`() {
        val hidden = xml.listFiles { file -> file.name.startsWith("widget_") }.orEmpty()
            .filter { "hide_from_picker" in it.readText() }
            .map { it.name }
            .toSet()
        assertEquals(setOf("widget_today_info.xml", "widget_sky_info.xml"), hidden)
    }

    @Test
    fun `a preview layout only uses views RemoteViews knows`() {
        val previews = layout.listFiles { file -> file.name.endsWith("_preview.xml") }.orEmpty()
        assertEquals("one preview per widget", 5, previews.size)

        previews.forEach { preview ->
            val unsupported = tag.findAll(preview.readText())
                .map { it.groupValues[1] }
                .filterNot { it.startsWith("!") || it == "xml" }
                .filterNot { it in remoteViewsTags }
                .toSet()
            assertEquals("${preview.name} uses views RemoteViews cannot inflate", emptySet<String>(), unsupported)
        }
    }

    /**
     * The picker shows the card a fresh widget wears (25 set 2026): the blue card since
     * 21 set. The hex is baked into `colors.xml` because a static layout cannot
     * call Kotlin, so this is where the two are kept in step: a default re-picked in
     * [WidgetLook] or a colour re-measured in `WidgetCardPalette` fails here, not on
     * somebody's picker. And no preview goes back to a ground of its own.
     */
    @Test
    fun `the previews draw the default card`() {
        val default = WidgetLook()
        assertEquals(WidgetBackground.COLOR, default.background)
        val expected = "#%08X".format(widgetCardContainer(default.cardColor).toArgb())
        val colors = File("src/main/res/values/colors.xml").readText()
        assertTrue(
            "widget_preview_card is not the default card, $expected",
            colors.contains("<color name=\"widget_preview_card\">$expected</color>")
        )

        layout.listFiles { file -> file.name.endsWith("_preview.xml") }.orEmpty().forEach { preview ->
            // The window's ground is its picture (20 ott 2026): its preview stands on a night
            // sky of its own, the one drawable this rule allows besides the card.
            val ground = if (preview.name == "widget_window_preview.xml") "widget_preview_window" else "widget_preview_card"
            assertTrue(
                "${preview.name} does not stand on its ground ($ground)",
                preview.readText().contains("""android:background="@drawable/$ground"""")
            )
        }
    }
}
