#!/usr/bin/env python3
"""Misura dei pericoli che il motore non decide da solo: temporale e gelicidio (docs/STATES.md §11).

Le regole 1 e 2 del motore degli stati passano il codice del provider (95/96/99, 56/57/66/67):
erano le uniche due regole di pericolo mai confrontate con quel che si e' visto. Questo
strumento scarica, per gli aeroporti di `measure_against_metar.py` e per un periodo passato:

- **quel che `best_match` aveva previsto** per quelle ore, dall'archivio di Open-Meteo
  (`historical-forecast-api`): il codice, le quantita', la temperatura, la probabilita', e le
  grandezze convettive candidate a dire un temporale meglio del codice (CAPE, lifted index,
  inibizione convettiva, il potenziale di fulmini che solo ICON-D2 calcola);
- **i METAR** dello stesso aeroporto (Iowa Environmental Mesonet): l'ora, i fenomeni (TS, VCTS,
  FZRA, FZDZ...) e la temperatura, non il bollettino intero.

Una sola richiesta di dieci variabili per aeroporto: per tre mesi sono 6,6 chiamate l'uno, 256
in tutto con i tredici aeroporti fuori dall'Europa (`WORLD`), e si lancia da qui. Non usa il nome `metar-*.json.gz` di proposito: il workflow
`Measure against METAR` completa ogni file con quel nome con tutti i modelli, le corse
precedenti e il satellite, che a questa misura non servono.

L'analisi e' `python tools/analyze_against_metar.py --hazards <file...>`, e legge anche gli
archivi `metar-*` (che hanno il codice e la probabilita', non le grandezze convettive).

Solo libreria standard di Python 3.8+.

    python tools/measure_hazards.py --start 2026-06-01 --end 2026-08-31
"""

import argparse
import datetime as dt
import gzip
import json
import os
import sys
import time
import urllib.parse

import measure_against_metar as m

HOURLY = [
    "weather_code", "precipitation", "snowfall", "showers", "temperature_2m",
    "precipitation_probability", "cape", "lifted_index", "convective_inhibition",
    "lightning_potential",
]
METAR_KEPT = ("valid", "wxcodes", "tmpf")

# The storm outside Europe (17 nov 2026): `best_match` is the ICON family over the 26 airports
# of `measure_against_metar.py` and something else here, HRRR over the United States, GFS, IFS
# and JMA over the tropics and Asia, where Open-Meteo derives the thunderstorm code itself from
# CAPE and the lifted index. A rule about that code has to hold in both worlds. The first
# field is the station's name in the Iowa Mesonet's archive: the ICAO code, without the K in
# the United States.
WORLD = [
    ("MIA", "Miami", 25.793, -80.291),
    ("ATL", "Atlanta", 33.637, -84.428),
    ("IAH", "Houston", 29.984, -95.341),
    ("DEN", "Denver", 39.862, -104.673),
    ("ORD", "Chicago O'Hare", 41.979, -87.905),
    ("MMMX", "Mexico City", 19.436, -99.072),
    ("SKBO", "Bogota", 4.702, -74.147),
    ("WSSS", "Singapore Changi", 1.359, 103.989),
    ("WMKK", "Kuala Lumpur", 2.746, 101.710),
    ("VTBS", "Bangkok", 13.690, 100.750),
    ("VHHH", "Hong Kong", 22.309, 113.915),
    ("RJTT", "Tokyo Haneda", 35.553, 139.781),
    ("VIDP", "Delhi", 28.566, 77.103),
]


def forecast(lat, lon, start, end):
    """`best_match` for the period, its columns named as a many-model answer names them."""
    query = urllib.parse.urlencode({
        "latitude": lat, "longitude": lon, "hourly": ",".join(HOURLY),
        "start_date": start.isoformat(), "end_date": end.isoformat(), "timezone": "GMT",
    })
    answer = json.loads(m.get(f"{m.FORECAST_API}?{query}"))
    answer["hourly"] = m.suffixed(answer["hourly"], "best_match")
    return answer


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--start", type=dt.date.fromisoformat, required=True)
    parser.add_argument("--end", type=dt.date.fromisoformat, required=True)
    parser.add_argument("--out")
    args = parser.parse_args()
    here = os.path.dirname(os.path.abspath(__file__))
    out = args.out or os.path.join(here, "measurements", f"hazards-{args.start}-{args.end}.json.gz")
    results, failed = [], []
    stations = list(m.STATIONS) + WORLD
    print(f"{len(stations)} aeroporti, dal {args.start} al {args.end}")
    for icao, name, lat, lon in stations:
        try:
            predicted = forecast(lat, lon, args.start, args.end)
            time.sleep(1.5)
            observed = [{k: r.get(k) for k in METAR_KEPT} for r in m.metars(icao, args.start, args.end)]
            time.sleep(1.5)
        except Exception as error:  # un aeroporto perso non ferma gli altri
            print(f"   {icao} {name:<26} ERRORE: {error}")
            failed.append({"icao": icao, "error": str(error)})
            continue
        hourly = predicted["hourly"]
        # The analysis reads the chance from its own block, as in the `metar-*` archives.
        chance = {"hourly": {"time": hourly["time"],
                             "precipitation_probability": hourly.get("precipitation_probability_best_match")}}
        print(f"   {icao} {name:<26} ok  {len(hourly['time'])} ore, {len(observed)} METAR")
        results.append({"icao": icao, "name": name, "lat": lat, "lon": lon,
                        "forecast": predicted, "metar": observed, "probability": chance})
    os.makedirs(os.path.dirname(os.path.abspath(out)), exist_ok=True)
    m.write(out, {
        "fetched_at_utc": dt.datetime.now(dt.timezone.utc).isoformat(timespec="seconds"),
        "start": args.start.isoformat(), "end": args.end.isoformat(),
        "forecast_api": m.FORECAST_API, "metar_api": m.METAR_API,
        "hourly": HOURLY, "stations": results, "failed": failed,
    })
    print(f"\nFatto: {len(results)} aeroporti su {len(stations)}, {out} "
          f"({os.path.getsize(out) / 1024:.0f} KB)")
    return 0 if results else 1


if __name__ == "__main__":
    sys.exit(main())
