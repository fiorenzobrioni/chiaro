package com.callbackdev.chiaro.ui.today

import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.domain.ConditionWord
import com.callbackdev.chiaro.domain.WmoCode
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * The words for the WMO codes (24 set 2026). The grouping lives in [WmoCode] now and
 * `WeatherText` only spells it; these pin the spelling, and the three words the engine
 * review changed because they promised more than the code can.
 */
class ConditionWordsTest {

    @Test
    fun `every word has its own string, and every code reaches one`() {
        val strings = ConditionWord.entries.map(WeatherText::condition)
        assertEquals(strings.size, strings.toSet().size)
        WmoCode.entries.forEach { code ->
            assertEquals(WeatherText.condition(code.word), WeatherText.condition(code.code))
        }
        assertEquals(R.string.cond_unknown, WeatherText.condition(42))
    }

    @Test
    fun `the codes the review reworded reach the new words`() {
        assertEquals(R.string.cond_thunderstorm_strong, WeatherText.condition(96))
        assertEquals(R.string.cond_thunderstorm_strong, WeatherText.condition(99))
        assertEquals(R.string.cond_showers_heavy, WeatherText.condition(82))
        assertEquals(R.string.cond_partly_cloudy, WeatherText.condition(2))
    }

    private fun strings(folder: String): Map<String, String> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(File("src/main/res/$folder/strings.xml"))
        val nodes = doc.getElementsByTagName("string")
        return (0 until nodes.length).associate { i ->
            val n = nodes.item(i)
            n.attributes.getNamedItem("name").nodeValue to n.textContent
        }
    }

    /**
     * Outside the ICON family Open-Meteo writes 96 for a strong thunderstorm and never
     * writes 99, so «con grandine» was hail nobody forecast; 82 starts at 7.6 mm/h, the
     * floor of «Pioggia forte», not a violent one; code 2 is 50-80% of the sky.
     */
    @Test
    fun `no word promises more than its code`() {
        val it = strings("values-it")
        val en = strings("values")
        assertEquals("Temporale forte", it["cond_thunderstorm_strong"])
        assertEquals("Strong thunderstorm", en["cond_thunderstorm_strong"])
        assertEquals("Rovesci forti", it["cond_showers_heavy"])
        assertEquals("Heavy showers", en["cond_showers_heavy"])
        assertEquals("Nuvoloso", it["cond_partly_cloudy"])
        listOf(it, en).forEach { table ->
            val conditions = table.filterKeys { k -> k.startsWith("cond_") }.values
            assertFalse(conditions.any { w -> "grandin" in w.lowercase() || "hail" in w.lowercase() })
            assertFalse(conditions.any { w -> "violent" in w.lowercase() })
        }
    }
}
