package com.callbackdev.chiaro.ui.icons

import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.data.WeatherIcons
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Che disegno prende un codice meteo.
 *
 * **Questa tabella non aveva un test, ed è per questo che il difetto è vissuto tre
 * fasi.** `AnimatedIconTest` cammina i gemelli animati, `IconContrastTest` misura i
 * colori, `WidgetIconsTest` copre la scelta dello stile: nessuno dei tre ha mai chiesto
 * quale disegno esca da un codice, e così i codici 1 e 2 hanno potuto condividerne uno
 * senza che niente diventasse rosso. La segnalazione che ha aperto la Fase 13 è arrivata
 * da un lettore, non dalla suite.
 */
class ConditionIconsTest {

    /** Ogni codice che Open-Meteo può servire, e il disegno che gli tocca di giorno. */
    private val byDay = mapOf(
        0 to R.drawable.mc3_clear_day,
        1 to R.drawable.mc3_sun_one_cloud_day,
        2 to R.drawable.mc3_partly_cloudy_day,
        3 to R.drawable.mc3_overcast,
        45 to R.drawable.mc3_fog_day,
        48 to R.drawable.mc3_fog_day,
        51 to R.drawable.mc3_overcast_drizzle,
        53 to R.drawable.mc3_overcast_drizzle,
        55 to R.drawable.mc3_overcast_drizzle,
        56 to R.drawable.mc3_overcast_sleet,
        57 to R.drawable.mc3_overcast_sleet,
        61 to R.drawable.mc3_overcast_rain,
        63 to R.drawable.mc3_overcast_rain,
        65 to R.drawable.mc3_overcast_rain,
        66 to R.drawable.mc3_overcast_sleet,
        67 to R.drawable.mc3_overcast_sleet,
        71 to R.drawable.mc3_overcast_snow,
        73 to R.drawable.mc3_overcast_snow,
        75 to R.drawable.mc3_overcast_snow,
        77 to R.drawable.mc3_overcast_snow,
        80 to R.drawable.mc3_partly_cloudy_day_rain,
        81 to R.drawable.mc3_partly_cloudy_day_rain,
        82 to R.drawable.mc3_extreme_rain,
        85 to R.drawable.mc3_partly_cloudy_day_snow,
        86 to R.drawable.mc3_partly_cloudy_day_snow,
        95 to R.drawable.mc3_thunderstorms_day,
        96 to R.drawable.mc3_thunderstorms_day_hail,
        99 to R.drawable.mc3_thunderstorms_day_hail
    )

    /** I codici che di notte cambiano disegno, e in cosa. */
    private val byNight = mapOf(
        0 to R.drawable.mc3_clear_night,
        1 to R.drawable.mc3_sun_one_cloud_night,
        2 to R.drawable.mc3_partly_cloudy_night,
        45 to R.drawable.mc3_fog_night,
        48 to R.drawable.mc3_fog_night,
        80 to R.drawable.mc3_partly_cloudy_night_rain,
        81 to R.drawable.mc3_partly_cloudy_night_rain,
        85 to R.drawable.mc3_partly_cloudy_night_snow,
        86 to R.drawable.mc3_partly_cloudy_night_snow,
        95 to R.drawable.mc3_thunderstorms_night,
        96 to R.drawable.mc3_thunderstorms_night_hail,
        99 to R.drawable.mc3_thunderstorms_night_hail
    )

    @Test
    fun `every WMO code the provider can serve has its drawing`() {
        byDay.forEach { (code, expected) ->
            assertEquals(
                "il codice $code non prende il disegno che dovrebbe, di giorno",
                expected,
                ChiaroIcons.conditionLineRes(code, night = false)
            )
        }
    }

    @Test
    fun `night changes the drawing only where the sky differs`() {
        byDay.keys.forEach { code ->
            val expected = byNight[code] ?: byDay.getValue(code)
            assertEquals(
                "il codice $code non prende il disegno che dovrebbe, di notte",
                expected,
                ChiaroIcons.conditionLineRes(code, night = true)
            )
        }
    }

    /**
     * Il difetto che ha aperto la fase, in una riga, e le due decisioni che gli sono
     * seguite.
     *
     * Il codice 1 è «quasi sereno» — 25% di copertura mediana, misurata su 1 680 ore — e
     * il 2 è «poco nuvoloso», 64%. Fino alla Fase 13 disegnavano **la stessa cosa**, ed è
     * quella la confusione che non deve poter tornare: sono le due metà opposte del cielo
     * sereno. L'11 set 2026 il codice 1 ha preso il sole pieno, perché il disegno che
     * Meteocons chiama `mostly-clear` porta il 72% della nuvola del poco nuvoloso; e così
     * 0 e 1 si sono ritrovati uguali dove non ci sono parole, cioè nella striscia oraria.
     *
     * Dal 12 set 2026 sono **tre disegni diversi**: il codice 1 prende `sun-one-cloud`,
     * che questo repo compone (il sereno intatto più una nuvoletta nell'angolo,
     * `tools/compose_sun_cloud.py`). Il test fissa le tre distanze, così nessuna delle
     * due confusioni può tornare per caso.
     */
    @Test
    fun `mostly clear is its own drawing, neither clear nor partly cloudy`() {
        listOf(false, true).forEach { night ->
            val clear = ChiaroIcons.conditionLineRes(0, night)
            val mostly = ChiaroIcons.conditionLineRes(1, night)
            val partly = ChiaroIcons.conditionLineRes(2, night)
            assertNotEquals("quasi sereno e sereno sono lo stesso disegno", clear, mostly)
            assertNotEquals("quasi sereno e poco nuvoloso sono lo stesso disegno", mostly, partly)
            assertNotEquals("sereno e poco nuvoloso sono lo stesso disegno", clear, partly)
            assertTrue(
                "il quasi sereno non è il disegno composto",
                mostly in ComposedIcons.byName.values
            )
        }
    }

    /**
     * Un codice che non conosciamo non diventa una nuvola. La parola accanto dice già
     * «condizioni sconosciute», e disegnare del tempo che nessuno ha previsto è
     * esattamente la bugia che DESIGN §1.1 vieta.
     */
    @Test
    fun `an unknown code says it does not know`() {
        listOf(-1, 4, 19, 42, 100, 999).forEach { code ->
            assertEquals(
                "il codice $code inventa un tempo",
                R.drawable.mc3_not_available,
                ChiaroIcons.conditionLineRes(code)
            )
        }
    }

    /**
     * Ogni disegno che la mappatura può restituire esiste in tutti e quattro i set, e
     * `styledRes` lo trova. Usa `getValue`, quindi una tabella incompleta qui è
     * un'eccezione in faccia al lettore, non un'icona vuota.
     */
    @Test
    fun `every drawing the mapping can return exists in all four sets`() {
        val codes = byDay.keys + listOf(-1, 4, 999)
        codes.forEach { code ->
            listOf(false, true).forEach { night ->
                val line = ChiaroIcons.conditionLineRes(code, night)
                listOf(WeatherIcons.LINE, WeatherIcons.FILL).forEach { style ->
                    listOf(false, true).forEach { dark ->
                        assertTrue(
                            "nessun disegno per $code (notte=$night, $style, scuro=$dark)",
                            ChiaroIcons.styledRes(line, style, dark) != 0
                        )
                    }
                }
            }
        }
    }
}
