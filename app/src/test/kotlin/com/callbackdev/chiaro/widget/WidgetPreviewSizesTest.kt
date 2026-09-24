package com.callbackdev.chiaro.widget

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The size chips under every widget's preview (23 set 2026). A chip for a grant the
 * launcher will refuse would be teaching the reader a card nobody can have, so each list
 * is checked against its provider's own minimum in `res/xml` — the resize minimum where the
 * provider declares one, `minWidth`/`minHeight` otherwise, which is what a launcher falls
 * back to. And the card's default placement is always one of the chips, because it is the
 * size the preview opens on when the launcher has not said the real one.
 */
class WidgetPreviewSizesTest {

    private val providers = mapOf(
        WidgetKind.NOW to "widget_now_info.xml",
        WidgetKind.TODAY to "widget_today_info.xml",
        WidgetKind.SKY to "widget_sky_info.xml",
        WidgetKind.TEXT to "widget_text_info.xml",
        WidgetKind.ARC to "widget_arc_info.xml"
    )

    private fun dp(xml: String, attribute: String): Float? =
        Regex("""android:$attribute="(\d+)dp"""").find(xml)?.groupValues?.get(1)?.toFloat()

    @Test
    fun `every chip is a size the launcher can grant`() {
        providers.forEach { (kind, file) ->
            val xml = File("src/main/res/xml/$file").readText()
            val minWidth = dp(xml, "minResizeWidth") ?: dp(xml, "minWidth")!!
            val minHeight = dp(xml, "minResizeHeight") ?: dp(xml, "minHeight")!!
            previewSizes(kind).forEach { option ->
                assertTrue(
                    "$kind offers ${option.label}, under its provider's $minWidth × $minHeight",
                    option.size.width.value >= minWidth && option.size.height.value >= minHeight
                )
            }
        }
    }

    @Test
    fun `the default placement is always one of the chips`() {
        providers.keys.forEach { kind ->
            assertTrue("$kind", defaultPreviewSize(kind) in previewSizes(kind))
        }
    }
}
