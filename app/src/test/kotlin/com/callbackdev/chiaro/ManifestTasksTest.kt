package com.callbackdev.chiaro

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Three manifest attributes that are the whole of a fix and are invisible from the code
 * that they fix (21 set 2026, from the device). Nothing in Kotlin reads them, no test
 * that exercises an intent can see them, and the symptoms they cure appear only on a
 * real launcher — two copies of the home screen, and a settings screen coming back from
 * under the app after being swiped away. A sweep of the file is the only honest guard.
 *
 * `singleTask` on the activity: the app is ONE activity, and without this the platform
 * made a second task of it whenever the intent that opened it was not the launcher's.
 *
 * `taskAffinity=""` and `noHistory` on the two widget settings screens: with the app's
 * own affinity they joined the app's task, so a screen dismissed with a swipe to home
 * was still there under the next launch, and back came out onto it.
 */
class ManifestTasksTest {

    private val manifest = File("src/main/AndroidManifest.xml").readText()

    /** The activity block for [name], from its opening tag to the end of the tag. */
    private fun activity(name: String): String {
        val start = manifest.indexOf("android:name=\"$name\"")
        assertTrue("no <activity> for $name in the manifest", start > 0)
        val open = manifest.lastIndexOf("<activity", start)
        val close = manifest.indexOf(">", start)
        return manifest.substring(open, close)
    }

    @Test
    fun `the one activity is singleTask`() {
        assertTrue(
            "MainActivity must be singleTask, or a widget tap opens a second copy of the app",
            activity(".MainActivity").contains("android:launchMode=\"singleTask\"")
        )
    }

    @Test
    fun `neither widget settings screen can land in the app's task`() {
        listOf(".widget.WidgetConfigActivity", ".widget.arc.ArcConfigActivity").forEach { name ->
            val block = activity(name)
            assertTrue(
                "$name must have no task affinity, or it shares the app's task",
                block.contains("android:taskAffinity=\"\"")
            )
            assertTrue(
                "$name must not outlive being swiped away",
                block.contains("android:noHistory=\"true\"")
            )
            assertTrue(
                "$name must stay out of Recents",
                block.contains("android:excludeFromRecents=\"true\"")
            )
        }
    }

    /**
     * `noHistory` is only safe because no widget waits on this screen's result: all five
     * providers declare `configuration_optional`, so it runs from the reconfigure flow
     * and never at placement time. Drop that word from a provider and a placed widget
     * would be cancelled by a screen the system finished on its own.
     */
    @Test
    fun `no provider makes its settings screen part of placing the widget`() {
        val providers = File("src/main/res/xml")
            .listFiles { file -> file.name.startsWith("widget_") }.orEmpty()
        assertTrue("no widget providers found", providers.size == 5)
        providers.forEach { provider ->
            val xml = provider.readText()
            if (xml.contains("android:configure")) {
                assertTrue(
                    "${provider.name} declares a config screen without configuration_optional",
                    xml.contains("configuration_optional")
                )
            }
        }
    }
}
