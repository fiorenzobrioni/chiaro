#!/usr/bin/env python3
"""Quali delle 519 icone di Meteocons v3 l'app nomina davvero.

Il repo tiene la famiglia **intera** (PLANNING Fase 13). Questo file dice quale
sottoinsieme le tabelle Kotlin referenziano, e quindi l'unico che finisce nell'APK di
release: tutto il resto lo toglie `shrinkResources`, al prezzo misurato di 64 byte a
drawable in `resources.arsc`.

**SHIPPED e' cio' che una schermata disegna oggi, non cio' che vorremmo disegnare.**
La differenza e' stata misurata l'11 set 2026: spedire anche le 90 gia' scelte ma non
ancora cablate costava **570 KB** su un APK di 6,8 MB, cioe' l'8%, per disegni che nessun
lettore vedeva. Stanno in PLANNED, e ognuna si sposta di qui a li' nella stessa modifica
che le da' una schermata — una riga, senza reimportare niente e senza rete. E' questa la
ragione per cui il set completo vive nel repo.

ANIMATED e' piu' stretta apposta (DESIGN §7.1): **muove solo la famiglia delle
condizioni**, perche' il marchio di un tile etichetta una quantita' e un barometro che
gira per sempre e' decorazione.
"""

#: I cieli che `ChiaroIcons.conditionLineRes` puo' restituire, i marchi delle schede, del
#: Cielo e dell'agenda: tutto quello che oggi arriva su uno schermo.
SHIPPED = [
    "clear-day", "clear-night", "mostly-clear-day", "mostly-clear-night",
    "partly-cloudy-day", "partly-cloudy-night", "overcast", "cloudy", "fog-day",
    "fog-night", "overcast-drizzle", "overcast-rain", "overcast-sleet",
    "overcast-snow", "partly-cloudy-day-rain", "partly-cloudy-night-rain",
    "partly-cloudy-day-snow", "partly-cloudy-night-snow", "extreme-rain",
    "thunderstorms-day", "thunderstorms-night", "thunderstorms-day-hail",
    "thunderstorms-night-hail", "not-available", "wind", "humidity", "uv-index",
    "thermometer", "barometer", "raindrops", "mist", "snowflake", "smoke-particles",
    "compass", "pollen", "sunrise", "sunset", "moonrise", "moonset", "horizon",
    "star", "starry-night", "falling-stars", "solar-eclipse", "moon-new",
    "moon-waxing-crescent", "moon-first-quarter", "moon-waxing-gibbous", "moon-full",
    "moon-waning-gibbous", "moon-last-quarter", "moon-waning-crescent", "rainbow",
    "uv-index-1", "uv-index-2", "uv-index-3", "uv-index-4", "uv-index-5",
    "uv-index-6", "uv-index-7", "uv-index-8", "uv-index-9", "uv-index-10",
    "uv-index-11", "uv-index-11-plus", "barometer-moderate",
    "pollen-grass", "pollen-tree", "pollen-weed",
    "pollen-grass-low", "pollen-grass-moderate", "pollen-grass-high",
    "pollen-tree-low", "pollen-tree-moderate", "pollen-tree-high", "pollen-weed-low",
    "pollen-weed-moderate", "pollen-weed-high", "haze", "fog",
]

#: Scelte, verificate presenti a monte, e **non spedite**: nessuna schermata le disegna
#: ancora. I pollini nelle tre piante e quattro livelli, l'UV graduato, le bande del
#: barometro, la scala Beaufort, i momenti del giorno, i tipi di allerta della Fase 12.
#: Spostarne una in SHIPPED e' la meta' del lavoro che serve a usarla.
PLANNED = [
    "raindrop", "umbrella", "dust", "pollen-grass-very-high",
    "pollen-tree-very-high", "pollen-weed-very-high", "barometer-very-high",
    "barometer-extreme", "windsock", "windsock-calm", "windsock-weak",
    "windsock-moderate", "wind-beaufort-0", "wind-beaufort-1", "wind-beaufort-2",
    "wind-beaufort-3", "wind-beaufort-4", "wind-beaufort-5", "wind-beaufort-6",
    "wind-beaufort-7", "wind-beaufort-8", "wind-beaufort-9", "wind-beaufort-10",
    "wind-beaufort-11", "wind-beaufort-12", "umbrella-wind", "smoke",
    "thermometer-warmer", "thermometer-colder", "wind-direction-n",
    "wind-direction-ne", "wind-direction-e", "wind-direction-se", "wind-direction-s",
    "wind-direction-sw", "wind-direction-w", "wind-direction-nw", "rainbow-clear",
    "rainbow-cloud", "time-morning", "time-late-morning", "time-afternoon",
    "time-late-afternoon", "time-evening", "time-late-evening", "time-night",
    "time-late-night", "weather-alert", "weather-alert-day", "weather-alert-night",
    "wind-alert", "thermometer-alert", "uv-index-alert", "water-alert", "fire-alert",
    "avalanche-danger-alert", "tornado", "hurricane", "cyclone", "waterspout",
    "falling-rocks-alert",
]

#: Le condizioni si muovono, e solo loro. `not-available` no: Meteocons lo disegna fermo,
#: ed e' la quantita' di movimento giusta per «non lo sappiamo».
ANIMATED = {n for n in SHIPPED
            if n.startswith(("clear-", "mostly-clear-", "partly-cloudy-", "overcast",
                             "fog-", "thunderstorms-", "extreme-", "cloudy"))}
