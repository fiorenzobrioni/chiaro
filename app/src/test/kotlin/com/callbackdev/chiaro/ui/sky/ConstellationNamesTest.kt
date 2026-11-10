package com.callbackdev.chiaro.ui.sky

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.domain.sky.Constellations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The names of the eighty-eight (PLANNING, Fase 16, step 16.5): one for every constellation
 * the engine draws and for nothing else, a name of its own for each, in both languages.
 */
@RunWith(RobolectricTestRunner::class)
class ConstellationNamesTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun names(): Map<String, String> =
        Constellations.all.associate { it.id to context.getString(ConstellationNames.nameRes(it)) }

    @Test
    fun `every constellation the engine draws has a name, and nothing else has`() {
        assertEquals(Constellations.all.map { it.id }.toSet(), ConstellationNames.ids)
        assertEquals(88, Constellations.all.map { ConstellationNames.nameRes(it) }.toSet().size)
        assertEquals(R.string.constellation_uma, ConstellationNames.nameRes("UMa"))
        // The capitals are the IAU's: «Cra» is not the Southern Crown, and a figure's id is not one.
        assertNull(ConstellationNames.nameRes("Cra"))
        assertNull(ConstellationNames.nameRes("orion"))
    }

    @Test
    @Config(qualifiers = "en-rGB")
    fun `in English the name is the Latin one, each its own`() {
        val names = names()
        assertEquals(88, names.values.toSet().size)
        assertTrue(names.values.none { it.isBlank() })
        assertEquals("Ursa Major", names["UMa"])
        assertEquals("Cygnus", names["Cyg"])
        assertEquals("Gemini", names["Gem"])
        assertEquals("Boötes", names["Boo"])
        assertEquals("Crux", names["Cru"])
        assertEquals("Serpens", names["Ser"])
    }

    @Test
    @Config(qualifiers = "it")
    fun `in Italian the name is the Italian one, each its own`() {
        val names = names()
        assertEquals(88, names.values.toSet().size)
        assertTrue(names.values.none { it.isBlank() })
        assertEquals("Orsa Maggiore", names["UMa"])
        assertEquals("Cigno", names["Cyg"])
        assertEquals("Gemelli", names["Gem"])
        assertEquals("Croce del Sud", names["Cru"])
        assertEquals("Idra", names["Hya"])
        assertEquals("Idra Maschio", names["Hyi"])
        // Norma has two names in Italian, and one of them is a star's in this sky.
        assertEquals("Squadra", names["Nor"])
        assertEquals("Regolo", context.getString(R.string.star_regulus))
        // The names the guide and the «worth seeing» list already say are the same words.
        assertEquals("Cassiopea", names["Cas"])
        assertEquals("Orione", names["Ori"])
        assertEquals("Leone", names["Leo"])
        assertEquals("Scorpione", names["Sco"])
    }
}
