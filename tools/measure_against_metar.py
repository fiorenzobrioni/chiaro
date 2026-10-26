#!/usr/bin/env python3
"""Misura del motore degli stati contro il cielo osservato (docs/STATES.md, §9 e §11).

Per un campione di aeroporti scelto per i fenomeni (la nebbia della pianura padana prima di
tutto, poi foschia, cielo, pioggia e neve), scarica due cose per lo stesso periodo passato:

- **le previsioni** che ogni modello aveva fatto per quelle ore, dall'archivio di Open-Meteo
  (`historical-forecast-api`: le prime ore di ogni corsa, cucite insieme), per `best_match` e
  per i modelli candidati a testimoni;
- **le osservazioni** METAR dello stesso aeroporto, dall'archivio dell'Iowa Environmental
  Mesonet (gratuito, senza chiave): visibilita', fenomeni (FG, BR, RA, SN...), nubi per strato;
- **la radiazione solare vista dal satellite** sullo stesso punto (`satellite-api` di
  Open-Meteo, EUMETSAT sull'Europa): la verita' del cielo, perche' un METAR europeo riporta
  solo le nubi sotto i 1 500 m e non dice se il sole si vedeva.

Salva tutto **grezzo** in un solo file JSON compresso: l'analisi (tools/analyze_against_metar.py)
si fa dopo e si puo' rifare con soglie diverse senza riscaricare niente. Lo strumento serve
solo a misurare: nessuna osservazione entra nell'app.

Solo libreria standard di Python 3.8+.

    python tools/measure_against_metar.py                       # gli ultimi 30 giorni chiusi
    python tools/measure_against_metar.py --start 2026-09-26 --end 2026-10-25
"""

import argparse
import csv
import datetime as dt
import gzip
import io
import json
import os
import sys
import time
import urllib.error
import urllib.parse
import urllib.request

FORECAST_API = "https://historical-forecast-api.open-meteo.com/v1/forecast"
METAR_API = "https://mesonet.agron.iastate.edu/cgi-bin/request/asos.py"
SATELLITE_API = "https://satellite-api.open-meteo.com/v1/archive"
SATELLITE = ["shortwave_radiation", "direct_radiation", "diffuse_radiation"]

# ICAO, nome, latitudine, longitudine. La pianura padana pesa di piu' perche' e' la
# nebbia che ha aperto la misura (Lissone, 26 ott 2026); il resto copre gli altri modelli
# che best_match sceglie (UKMO, AROME, HARMONIE, MET Nordic, ECMWF) e la neve del nord.
STATIONS = [
    ("LIML", "Milano Linate", 45.445, 9.277),
    ("LIMC", "Milano Malpensa", 45.630, 8.723),
    ("LIME", "Bergamo Orio al Serio", 45.669, 9.700),
    ("LIMF", "Torino Caselle", 45.200, 7.650),
    ("LIPX", "Verona Villafranca", 45.396, 10.888),
    ("LIPE", "Bologna", 44.535, 11.289),
    ("LIPZ", "Venezia Tessera", 45.505, 12.352),
    ("LIRF", "Roma Fiumicino", 41.800, 12.239),
    ("LIRN", "Napoli Capodichino", 40.886, 14.291),
    ("LICJ", "Palermo Punta Raisi", 38.176, 13.091),
    ("LIEE", "Cagliari Elmas", 39.251, 9.054),
    ("LSZH", "Zurigo", 47.458, 8.548),
    ("EDDM", "Monaco di Baviera", 48.354, 11.786),
    ("LOWW", "Vienna", 48.110, 16.570),
    ("LFPG", "Parigi Charles de Gaulle", 49.010, 2.548),
    ("EHAM", "Amsterdam Schiphol", 52.308, 4.764),
    ("EGLL", "Londra Heathrow", 51.470, -0.454),
    ("LEMD", "Madrid Barajas", 40.472, -3.561),
    ("LGAV", "Atene", 37.936, 23.944),
    ("ENGM", "Oslo Gardermoen", 60.194, 11.100),
    ("ESSA", "Stoccolma Arlanda", 59.652, 17.919),
    ("EFHK", "Helsinki", 60.317, 24.963),
    ("BIKF", "Keflavik", 63.985, -22.605),
    ("ENSB", "Longyearbyen", 78.246, 15.466),
]

# best_match e' quello che l'app legge; gli altri sono i candidati a testimoni, una famiglia
# per riga: ECMWF, UKMO, NOAA, e ICON (che in Europa e' quasi sempre best_match stesso:
# serve a riconoscere il doppione, non a votare).
MODELS = [
    "best_match",
    "ecmwf_ifs",
    "ukmo_global_deterministic_10km",
    "gfs_global",
    "icon_eu",
    "icon_global",
]

HOURLY = [
    "weather_code", "temperature_2m", "dew_point_2m", "relative_humidity_2m", "visibility",
    "cloud_cover", "cloud_cover_low", "cloud_cover_mid", "cloud_cover_high",
    "precipitation", "snowfall", "showers", "wind_speed_10m",
]

METAR_FIELDS = ["vsby", "wxcodes", "skyc1", "skyl1", "skyc2", "skyl2", "skyc3", "skyl3",
                "skyc4", "skyl4", "tmpf", "dwpf", "metar"]


def get(url, attempts=10):
    """A GET with patience: Open-Meteo answers 429 when a minute's quota of weighted calls is
    spent (a request with many variables and models counts as many), so a 429 waits a whole
    minute; a network error, a growing pause."""
    request = urllib.request.Request(url, headers={"User-Agent": "chiaro-measure/1"})
    for attempt in range(attempts):
        try:
            with urllib.request.urlopen(request, timeout=90) as response:
                return response.read()
        except (urllib.error.URLError, TimeoutError, ConnectionError) as error:
            if attempt == attempts - 1:
                raise
            limited = isinstance(error, urllib.error.HTTPError) and error.code == 429
            wait = 60 if limited else min(2 ** (attempt + 1), 60)
            print(f"   riprovo tra {wait} s ({error})", file=sys.stderr)
            time.sleep(wait)


def forecasts(lat, lon, start, end):
    query = urllib.parse.urlencode({
        "latitude": lat, "longitude": lon,
        "hourly": ",".join(HOURLY), "models": ",".join(MODELS),
        "start_date": start.isoformat(), "end_date": end.isoformat(),
        "timezone": "GMT",
    })
    return json.loads(get(f"{FORECAST_API}?{query}"))


def satellite(lat, lon, start, end):
    query = urllib.parse.urlencode({
        "latitude": lat, "longitude": lon, "hourly": ",".join(SATELLITE),
        "models": "satellite_radiation_seamless",
        "start_date": start.isoformat(), "end_date": end.isoformat(), "timezone": "GMT",
    })
    return json.loads(get(f"{SATELLITE_API}?{query}"))


def metars(icao, start, end):
    """Every routine and special report of the period, as rows of the requested fields."""
    stop = end + dt.timedelta(days=1)
    params = [("station", icao)] + [("data", f) for f in METAR_FIELDS] + [
        ("year1", start.year), ("month1", start.month), ("day1", start.day),
        ("year2", stop.year), ("month2", stop.month), ("day2", stop.day),
        ("tz", "Etc/UTC"), ("format", "onlycomma"), ("latlon", "no"),
        ("missing", "null"), ("trace", "T"), ("direct", "no"),
        ("report_type", 3), ("report_type", 4),
    ]
    text = get(f"{METAR_API}?{urllib.parse.urlencode(params)}").decode("utf-8")
    return list(csv.DictReader(io.StringIO(text)))


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    yesterday = dt.date.today() - dt.timedelta(days=1)
    parser.add_argument("--start", type=dt.date.fromisoformat, default=yesterday - dt.timedelta(days=29))
    parser.add_argument("--end", type=dt.date.fromisoformat, default=yesterday)
    here = os.path.dirname(os.path.abspath(__file__))
    parser.add_argument("--out")
    parser.add_argument("--resume", action="store_true",
                        help="tiene gli aeroporti gia' nel file e scarica solo quelli che mancano")
    args = parser.parse_args()
    out = args.out or os.path.join(here, "measurements", f"metar-{args.start}-{args.end}.json.gz")

    started = dt.datetime.now(dt.timezone.utc).isoformat(timespec="seconds")
    results, failed = [], []
    if args.resume and os.path.exists(out):
        with gzip.open(out, "rt", encoding="utf-8") as f:
            results = json.load(f)["stations"]
        print(f"Riprendo: {len(results)} aeroporti gia' scaricati")
    done = {st["icao"] for st in results}
    print(f"{len(STATIONS)} aeroporti, dal {args.start} al {args.end}")
    for icao, name, lat, lon in STATIONS:
        if icao in done:
            continue
        try:
            forecast = forecasts(lat, lon, args.start, args.end)
            time.sleep(3.0)
            observed = metars(icao, args.start, args.end)
            time.sleep(3.0)
            # Best-effort: the satellites do not see the poles (Longyearbyen answers 400),
            # and a station without them still measures fog and phase.
            try:
                seen = satellite(lat, lon, args.start, args.end)
            except Exception as error:
                print(f"   {icao} senza satellite: {error}")
                seen = None
            time.sleep(3.0)
        except Exception as error:  # un aeroporto perso non ferma gli altri
            print(f"   {icao} {name:<26} ERRORE: {error}")
            failed.append({"icao": icao, "error": str(error)})
            continue
        print(f"   {icao} {name:<26} ok  {len(forecast['hourly']['time'])} ore, {len(observed)} METAR")
        results.append({"icao": icao, "name": name, "lat": lat, "lon": lon,
                        "forecast": forecast, "metar": observed, "satellite": seen})

    os.makedirs(os.path.dirname(os.path.abspath(out)), exist_ok=True)
    with gzip.open(out, "wt", encoding="utf-8") as f:
        json.dump({
            "fetched_at_utc": started,
            "start": args.start.isoformat(), "end": args.end.isoformat(),
            "forecast_api": FORECAST_API, "metar_api": METAR_API, "satellite_api": SATELLITE_API,
            "models": MODELS, "hourly": HOURLY,
            "stations": results, "failed": failed,
        }, f, ensure_ascii=False, separators=(",", ":"))
    print(f"\nFatto: {len(results)} aeroporti su {len(STATIONS)}, {out} "
          f"({os.path.getsize(out) / 1024:.0f} KB)")
    return 0 if results else 1


if __name__ == "__main__":
    sys.exit(main())
