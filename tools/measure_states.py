#!/usr/bin/env python3
"""Misura per il motore degli stati (PLANNING.md, «Il motore degli stati», passo 1).

Scarica da Open-Meteo, per un campione di citta' scelto per coprire i fenomeni (pioggia,
rovesci, neve, gelicidio, temporali, nebbia, cielo), la previsione di 7 giorni con tutti i
campi che il motore leggera', e la salva **grezza** in un solo file JSON: l'analisi si fa
dopo, sul file, e si puo' rifare con soglie diverse senza riscaricare niente.

Solo libreria standard di Python 3.8+: niente da installare. Gira uguale su Windows, macOS,
Linux e GitHub Actions.

    python tools/measure_states.py            # scrive tools/measurements/states-<data>.json
    python tools/measure_states.py --out x.json

Richieste: una per citta', una ogni mezzo secondo (Open-Meteo e' gratuito, restiamo
educati). Nessuna chiave, nessun dato personale: solo coordinate di citta'.
"""

import argparse
import datetime as dt
import json
import os
import sys
import time
import urllib.error
import urllib.parse
import urllib.request

API = "https://api.open-meteo.com/v1/forecast"

# Scelte per i fenomeni, non per la geografia: la pianura padana per nebbia e pioggia, le
# Alpi e l'Artico per la neve e il gelicidio, i tropici e l'estate australe per i rovesci
# e i temporali, le citta' dei casi gia' visti (Bari B, Longyearbyen A, Tokyo C).
CITIES = [
    ("Milano", 45.4642, 9.1900),
    ("Torino", 45.0703, 7.6869),
    ("Bologna", 44.4949, 11.3426),
    ("Venezia", 45.4408, 12.3155),
    ("Trento", 46.0748, 11.1217),
    ("Cortina d'Ampezzo", 46.5405, 12.1357),
    ("Roma", 41.9028, 12.4964),
    ("Napoli", 40.8518, 14.2681),
    ("Bari", 41.1171, 16.8719),
    ("Palermo", 38.1157, 13.3615),
    ("Cagliari", 39.2238, 9.1217),
    ("Londra", 51.5072, -0.1276),
    ("Parigi", 48.8566, 2.3522),
    ("Berlino", 52.5200, 13.4050),
    ("Oslo", 59.9139, 10.7522),
    ("Reykjavik", 64.1466, -21.9426),
    ("Longyearbyen", 78.2232, 15.6267),
    ("Montreal", 45.5019, -73.5674),
    ("New York", 40.7128, -74.0060),
    ("Denver", 39.7392, -104.9903),
    ("Miami", 25.7617, -80.1918),
    ("Tokyo", 35.6762, 139.6503),
    ("Singapore", 1.3521, 103.8198),
    ("Mumbai", 19.0760, 72.8777),
    ("Sydney", -33.8688, 151.2093),
    ("Buenos Aires", -34.6037, -58.3816),
]

HOURLY = [
    "precipitation", "rain", "showers", "snowfall", "precipitation_probability",
    "weather_code", "temperature_2m", "dew_point_2m", "relative_humidity_2m",
    "visibility", "cloud_cover", "is_day", "cape",
]
CURRENT = [
    "precipitation", "rain", "showers", "snowfall", "weather_code", "temperature_2m",
    "cloud_cover", "is_day",
]
DAILY = [
    "weather_code", "precipitation_sum", "rain_sum", "showers_sum", "snowfall_sum",
    "precipitation_hours", "precipitation_probability_max",
]


def fetch(lat, lon, attempts=4):
    query = urllib.parse.urlencode({
        "latitude": lat,
        "longitude": lon,
        "hourly": ",".join(HOURLY),
        "current": ",".join(CURRENT),
        "daily": ",".join(DAILY),
        "timezone": "auto",
        "forecast_days": 7,
    })
    request = urllib.request.Request(f"{API}?{query}", headers={"User-Agent": "chiaro-measure/1"})
    for attempt in range(attempts):
        try:
            with urllib.request.urlopen(request, timeout=30) as response:
                return json.load(response)
        except (urllib.error.URLError, TimeoutError) as error:
            if attempt == attempts - 1:
                raise
            wait = 2 ** (attempt + 1)
            print(f"   riprovo tra {wait} s ({error})", file=sys.stderr)
            time.sleep(wait)


def summary(response):
    """Tre numeri a colpo d'occhio, solo per chi lancia lo script: l'analisi vera e' dopo."""
    h = response["hourly"]
    wet_code = trace = sky_with_mm = likely_dry = 0
    for code, mm, pct in zip(h["weather_code"], h["precipitation"], h["precipitation_probability"]):
        if code is None or mm is None:
            continue
        if code >= 51:
            wet_code += 1
            if pct is not None and pct < 20:
                trace += 1
        elif mm >= 0.1:
            sky_with_mm += 1
        elif pct is not None and pct >= 60:
            likely_dry += 1
    return wet_code, trace, sky_with_mm, likely_dry


def main():
    today = dt.date.today().isoformat()
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    here = os.path.dirname(os.path.abspath(__file__))
    parser.add_argument("--out", default=os.path.join(here, "measurements", f"states-{today}.json"))
    args = parser.parse_args()

    started = dt.datetime.now(dt.timezone.utc).isoformat(timespec="seconds")
    results, failed = [], []
    print(f"Scarico {len(CITIES)} citta' da Open-Meteo...")
    for name, lat, lon in CITIES:
        try:
            response = fetch(lat, lon)
        except Exception as error:  # una citta' persa non ferma le altre
            print(f"   {name:<18} ERRORE: {error}")
            failed.append({"city": name, "error": str(error)})
            continue
        wet, trace, sky_mm, likely = summary(response)
        print(f"   {name:<18} ok  ore con codice di precipitazione {wet:>3}, "
              f"di cui sotto il 20% {trace:>3}; cielo con >=0,1 mm {sky_mm:>3}; asciutte al >=60% {likely:>3}")
        results.append({"city": name, "lat": lat, "lon": lon, "response": response})
        time.sleep(0.5)

    os.makedirs(os.path.dirname(os.path.abspath(args.out)), exist_ok=True)
    with open(args.out, "w", encoding="utf-8") as f:
        json.dump({
            "fetched_at_utc": started,
            "api": API,
            "hourly": HOURLY, "current": CURRENT, "daily": DAILY,
            "cities": results,
            "failed": failed,
        }, f, ensure_ascii=False, separators=(",", ":"))
    size_kb = os.path.getsize(args.out) / 1024
    print(f"\nFatto: {len(results)} citta' su {len(CITIES)}, file {args.out} ({size_kb:.0f} KB)")
    return 0 if results else 1


if __name__ == "__main__":
    sys.exit(main())
