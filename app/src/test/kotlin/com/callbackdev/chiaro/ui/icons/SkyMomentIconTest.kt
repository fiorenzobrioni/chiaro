package com.callbackdev.chiaro.ui.icons

import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.domain.sky.SkyJobCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Che disegno prende un momento del cielo — **la tabella che adesso è una sola**
 * (committente, 21 set 2026, da una schermata home: «l'icona dell'evento "Luna piena al
 * crepuscolo" non sembra corretta ed è diversa da quella che compare nell'app»).
 *
 * Era vero, e il modo in cui era successo è il motivo per cui questo test esiste. La
 * schermata Cielo e il widget «Momenti del cielo» avevano ciascuno il proprio `when` sugli
 * id dei job; il catalogo è cresciuto due volte (Fase 19 e Fase 28) e solo quello della
 * schermata è stato aggiornato. Un id non elencato è un `when` perfettamente legale con
 * un `else`, quindi niente è mai diventato rosso: venticinque momenti su quarantasette
 * uscivano dal widget col disegno dello sciame meteorico.
 *
 * Qui si cammina il catalogo. L'unico arm che può prendere il `falling-stars` sono gli
 * sciami, che è il disegno di cui è il nome; qualunque altro job che ci finisca è un job
 * aggiunto senza la sua icona, e fa fallire la build invece di disegnare una stella
 * cadente sopra un'eclissi.
 */
class SkyMomentIconTest {

    private val fallback = R.drawable.mc3_falling_stars

    @Test
    fun `ogni momento del catalogo ha il suo disegno, tranne gli sciami`() {
        val orphans = SkyJobCatalog.all
            .map { it.id }
            .filter { ChiaroIcons.skyJobLineRes(it) == fallback }
            .filterNot { it.startsWith("meteor.") }
        assertTrue("momenti senza icona: $orphans", orphans.isEmpty())
    }

    @Test
    fun `gli sciami tengono la stella cadente, che e' il loro disegno`() {
        val showers = SkyJobCatalog.meteorShowers.map { it.id }
        assertTrue("il catalogo ha perso gli sciami", showers.isNotEmpty())
        showers.forEach { id ->
            assertEquals(id, fallback, ChiaroIcons.skyJobLineRes(id))
        }
    }

    /**
     * I ventisette id che non sono sciami, uno per uno. La tabella è la politica — quale
     * disegno tocca a quale momento — ed è esattamente la parte che va discussa a mano,
     * quindi va scritta a mano anche qui: un test che la rileggesse dal codice non
     * direbbe niente.
     */
    @Test
    fun `la politica, id per id`() {
        val expected = mapOf(
            "sun.rise" to R.drawable.mc3_sunrise,
            "twilight.civil.am" to R.drawable.mc3_sunrise,
            "sun.latest_rise" to R.drawable.mc3_sunrise,
            "sun.set" to R.drawable.mc3_sunset,
            "twilight.civil.pm" to R.drawable.mc3_sunset,
            "sun.earliest_set" to R.drawable.mc3_sunset,
            "solar.noon" to R.drawable.mc3_clear_day,
            "earth.perihelion" to R.drawable.mc3_clear_day,
            "earth.aphelion" to R.drawable.mc3_clear_day,
            "golden_hour.am" to R.drawable.mc3_clear_day,
            "golden_hour.pm" to R.drawable.mc3_clear_day,
            "blue_hour.am" to R.drawable.mc3_star,
            "blue_hour.pm" to R.drawable.mc3_star,
            "twilight.nautical.am" to R.drawable.mc3_star,
            "twilight.nautical.pm" to R.drawable.mc3_star,
            "twilight.astronomical.am" to R.drawable.mc3_starry_night,
            "twilight.astronomical.pm" to R.drawable.mc3_starry_night,
            "darkness.window" to R.drawable.mc3_starry_night,
            "milky_way.core" to R.drawable.mc3_starry_night,
            "night.white.start" to R.drawable.mc3_starry_night,
            "night.white.end" to R.drawable.mc3_starry_night,
            "zodiacal.am" to R.drawable.mc3_horizon,
            "zodiacal.pm" to R.drawable.mc3_horizon,
            "moon.rise" to R.drawable.mc3_moonrise,
            "moon.set" to R.drawable.mc3_moonset,
            "moon.new" to R.drawable.mc3_moon_new,
            "moon.first_quarter" to R.drawable.mc3_moon_first_quarter,
            "moon.last_quarter" to R.drawable.mc3_moon_last_quarter,
            "moon.today" to R.drawable.mc3_moon_full,
            "moon.phase" to R.drawable.mc3_moon_full,
            "moon.full" to R.drawable.mc3_moon_full,
            "moon.closest_full" to R.drawable.mc3_moon_full,
            "eclipse.lunar" to R.drawable.mc3_moon_full,
            "eclipse.solar" to R.drawable.mc3_solar_eclipse,
            // La segnalazione: una luna piena che sorge nel crepuscolo È un sorgere di
            // luna, e prende in prestito quel disegno.
            "moon.full_at_dusk" to R.drawable.mc3_moonrise,
            "earthshine.pm" to R.drawable.mc3_moon_waxing_crescent,
            "earthshine.am" to R.drawable.mc3_moon_waning_crescent,
            "venus.evening" to R.drawable.mc3_star,
            "venus.morning" to R.drawable.mc3_star,
            "jupiter.night" to R.drawable.mc3_star,
            "conjunction.moon_venus" to R.drawable.mc3_star,
            "conjunction.moon_jupiter" to R.drawable.mc3_star,
            "conjunction.venus_jupiter" to R.drawable.mc3_star,
            "equinox.spring" to R.drawable.mc3_horizon,
            "solstice.summer" to R.drawable.mc3_horizon,
            "equinox.autumn" to R.drawable.mc3_horizon,
            "solstice.winter" to R.drawable.mc3_horizon
        )
        expected.forEach { (id, res) ->
            assertEquals(id, res, ChiaroIcons.skyJobLineRes(id))
        }
        // E la tabella qui sopra è il catalogo senza gli sciami: un momento nuovo va
        // aggiunto in due posti, e questo è il secondo.
        assertEquals(
            SkyJobCatalog.all.map { it.id }.filterNot { it.startsWith("meteor.") }.toSet(),
            expected.keys
        )
    }
}
