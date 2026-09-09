package com.callbackdev.chiaro.data.warnings

import android.util.Xml
import com.callbackdev.chiaro.domain.warnings.CapAlert
import com.callbackdev.chiaro.domain.warnings.CapArea
import com.callbackdev.chiaro.domain.warnings.CapInfo
import java.io.InputStream
import org.xmlpull.v1.XmlPullParser

/**
 * A thin pull-parser for CAP 1.2: it reads the few elements a bulletin is made of
 * into the flat [CapAlert] and skips everything else, so a field the issuer adds
 * tomorrow costs nothing and a field it drops fails as an absence, not a crash. It
 * knows no Italian and no level: that reading is `DpcBulletinReader`'s, in the
 * domain, where it is tested on data.
 *
 * Tested on the real `Cap_20260908_1519.xml` (`src/test/resources/dpc`), the first
 * non-Kotlin file under the test tree and deliberately so: an invented CAP would
 * prove the parser agrees with its author, not with the Dipartimento.
 */
object CapParser {

    fun parse(input: InputStream): CapAlert {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
        parser.setInput(input, null)
        parser.nextTag()
        require(parser.name == "alert") { "not a CAP alert: <${parser.name}>" }

        var identifier = ""
        var sent = ""
        var note: String? = null
        val infos = mutableListOf<CapInfo>()
        parser.children {
            when (parser.name) {
                "identifier" -> identifier = parser.nextText()
                "sent" -> sent = parser.nextText()
                "note" -> note = parser.nextText()
                "info" -> infos += readInfo(parser)
                else -> parser.skip()
            }
        }
        require(identifier.isNotBlank()) { "CAP alert without <identifier>" }
        require(sent.isNotBlank()) { "CAP alert without <sent>" }
        return CapAlert(identifier = identifier, sent = sent, note = note, infos = infos)
    }

    private fun readInfo(parser: XmlPullParser): CapInfo {
        var event = ""
        var onset: String? = null
        var expires: String? = null
        var severity: String? = null
        val areas = mutableListOf<CapArea>()
        parser.children {
            when (parser.name) {
                "event" -> event = parser.nextText()
                "onset" -> onset = parser.nextText()
                "expires" -> expires = parser.nextText()
                "severity" -> severity = parser.nextText()
                "area" -> areas += readArea(parser)
                else -> parser.skip()
            }
        }
        return CapInfo(event = event, onset = onset, expires = expires, severity = severity, areas = areas)
    }

    private fun readArea(parser: XmlPullParser): CapArea {
        var description = ""
        val geocodes = mutableMapOf<String, String>()
        parser.children {
            when (parser.name) {
                "areaDesc" -> description = parser.nextText()
                "geocode" -> {
                    var name = ""
                    var value = ""
                    parser.children {
                        when (parser.name) {
                            "valueName" -> name = parser.nextText()
                            "value" -> value = parser.nextText()
                            else -> parser.skip()
                        }
                    }
                    if (name.isNotBlank()) geocodes[name.trim()] = value
                }
                else -> parser.skip()
            }
        }
        return CapArea(description = description, geocodes = geocodes)
    }

    /** Runs [onChild] on each child START_TAG of the element the parser is on. */
    private inline fun XmlPullParser.children(onChild: () -> Unit) {
        while (next() != XmlPullParser.END_TAG) {
            if (eventType == XmlPullParser.START_TAG) onChild()
        }
    }

    /** Skips the element the parser is on, whatever it contains. */
    private fun XmlPullParser.skip() {
        var depth = 1
        while (depth != 0) {
            when (next()) {
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> depth++
            }
        }
    }
}
