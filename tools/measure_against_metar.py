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
  solo le nubi sotto i 1 500 m e non dice se il sole si vedeva;
- **le corse precedenti** per gli aeroporti italiani (`previous-runs-api` di Open-Meteo,
  Fase 14, step 14.4): quel che `best_match`, ICON-EU, ICON-D2 e ICON-2I dicevano per quelle
  ore 24 e 48 ore prima, cioe' il giorno 2 e il giorno 3 dell'app (l'archivio delle previsioni
  e' il giorno 1: a Roma il 10 dic 2025 coincide con `previous_day0` ora per ora). L'API porta
  per giorno di anticipo la temperatura, l'umidita', la rugiada, la copertura totale, la
  precipitazione, la neve, i rovesci, il vento e il codice, non la visibilita' ne' gli strati
  (verificato il 27 ott 2026).

Salva tutto **grezzo** in un solo file JSON compresso: l'analisi (tools/analyze_against_metar.py)
si fa dopo e si puo' rifare con soglie diverse senza riscaricare niente. Lo strumento serve
solo a misurare: nessuna osservazione entra nell'app.

Solo libreria standard di Python 3.8+.

    python tools/measure_against_metar.py                       # gli ultimi 30 giorni chiusi
    python tools/measure_against_metar.py --start 2026-09-26 --end 2026-10-25
    python tools/measure_against_metar.py --start 2026-09-26 --end 2026-10-25 --add-probability
    python tools/measure_against_metar.py --start 2026-09-26 --end 2026-10-25 --add-models
    python tools/measure_against_metar.py --start 2026-09-26 --end 2026-10-25 --add-lead-days

Il workflow `Measure against METAR` lo fa girare su un runner di GitHub, che ha un indirizzo suo:
Open-Meteo conta le chiamate per indirizzo, e una misura completa ne consuma molte.
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
PREVIOUS_RUNS_API = "https://previous-runs-api.open-meteo.com/v1/forecast"
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
    # Bari and Catania (14.4, 27 ott 2026): the south that ICON-2I would serve and ICON-D2 does
    # not reach, two more stations where the comparison is decided.
    ("LIBD", "Bari Palese", 41.138, 16.760),
    ("LICC", "Catania Fontanarossa", 37.467, 15.066),
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
# serve a riconoscere il doppione, non a votare). Dal 27 ott 2026 (Fase 14, step 14.2) le
# due HARMONIE-AROME Europe, KNMI e DMI: la stessa fisica a 5,5 km, due centri e due corse
# (nell'archivio a Linate coincidono in 29 ore su 72, nella sonda del 27 ott dal vivo erano
# uguali), con la visibilita' che ICON perde il 30 nov 2026. Un modello nuovo in questa lista
# non riscarica niente: `--add-models` aggiunge le sue colonne ai file che ci sono gia'
# (l'archivio di una previsione passata non cambia, e un file intero e' un giorno di chiamate).
MODELS = [
    "best_match",
    "ecmwf_ifs",
    "ukmo_global_deterministic_10km",
    "gfs_global",
    "icon_eu",
    "icon_global",
    "knmi_harmonie_arome_europe",
    "dmi_harmonie_arome_europe",
    # 14.4 (27 ott 2026): ICON-D2 (2 km, 48 h; best_match itself north of Florence, absent
    # south of it) and ARPAE's ICON-2I (2.2 km, 72 h, every 12 h; in the archive since 13 Apr
    # 2025, in best_match nowhere), the two the Italian comparison is between. ICON-2I has no
    # visibility, no probability and, in winter 2025-26, no weather code: its columns come
    # null, and the comparison reads none of them.
    "icon_d2",
    "italia_meteo_arpae_icon_2i",
]

# The previous runs (14.4): the models of the Italian comparison, the lead days the app shows
# as day 2 and day 3, and the hourly variables the Previous Runs API carries per lead day (the
# visibility and the cloud layers it does not). Weight: 4 models x 9 variables x 2 days = 72
# variables, 7.2 calls a week-pair: ~31 calls a station for the winter file, ~15 for October.
LEAD_MODELS = ["best_match", "icon_eu", "icon_d2", "italia_meteo_arpae_icon_2i"]
LEAD_DAYS = (1, 2)
LEAD_HOURLY = ["weather_code", "temperature_2m", "dew_point_2m", "relative_humidity_2m",
               "cloud_cover", "precipitation", "snowfall", "showers", "wind_speed_10m"]

HOURLY = [
    "weather_code", "temperature_2m", "dew_point_2m", "relative_humidity_2m", "visibility",
    "cloud_cover", "cloud_cover_low", "cloud_cover_mid", "cloud_cover_high",
    "precipitation", "snowfall", "showers", "wind_speed_10m",
]

METAR_FIELDS = ["vsby", "wxcodes", "skyc1", "skyl1", "skyc2", "skyl2", "skyc3", "skyl3",
                "skyc4", "skyl4", "tmpf", "dwpf", "metar"]


class NoData(Exception):
    """Open-Meteo's 400: the request is wrong for this point, typically because none of the
    models asked for covers it. Asking again does not change the answer."""


def get(url, attempts=10):
    """A GET with patience: Open-Meteo answers 429 when a minute's quota of weighted calls is
    spent (a request with many variables and models counts as many), so a 429 waits a whole
    minute; a network error, a growing pause. A 400 is not retried (27 ott 2026: ten tries
    at Longyearbyen for models that do not reach it cost four minutes a station)."""
    request = urllib.request.Request(url, headers={"User-Agent": "chiaro-measure/1"})
    for attempt in range(attempts):
        try:
            with urllib.request.urlopen(request, timeout=90) as response:
                return response.read()
        except (urllib.error.URLError, TimeoutError, ConnectionError) as error:
            if isinstance(error, urllib.error.HTTPError) and error.code == 400:
                raise NoData(error.read().decode("utf-8", "replace")[:200]) from None
            if attempt == attempts - 1:
                raise
            limited = isinstance(error, urllib.error.HTTPError) and error.code == 429
            wait = 60 if limited else min(2 ** (attempt + 1), 60)
            print(f"   riprovo tra {wait} s ({error})", file=sys.stderr)
            time.sleep(wait)


def forecasts(lat, lon, start, end, models=None):
    """The hourly series of [models] (every candidate by default). A model that does not
    cover the point is simply absent from the answer, not an error."""
    query = urllib.parse.urlencode({
        "latitude": lat, "longitude": lon,
        "hourly": ",".join(HOURLY), "models": ",".join(models or MODELS),
        "start_date": start.isoformat(), "end_date": end.isoformat(),
        "timezone": "GMT",
    })
    return json.loads(get(f"{FORECAST_API}?{query}"))


def previous_runs(lat, lon, start, end, models):
    """What [models] said for the period's hours one and two days earlier: the series of
    `<variable>_previous_day<n>_<model>`, on the same hours as the archive. A model that does
    not cover the point is absent from the answer; a lead day beyond a model's horizon
    (ICON-D2's day 3) comes null."""
    query = urllib.parse.urlencode({
        "latitude": lat, "longitude": lon,
        "hourly": ",".join(f"{v}_previous_day{d}" for d in LEAD_DAYS for v in LEAD_HOURLY),
        "models": ",".join(models),
        "start_date": start, "end_date": end, "timezone": "GMT",
    })
    return json.loads(get(f"{PREVIOUS_RUNS_API}?{query}"))


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


def probability(lat, lon, start, end):
    """`best_match`'s chance of more than 0.1 mm in each hour, as the archive kept it: the
    20% floor of the state engine is measured on it (STATES §11)."""
    query = urllib.parse.urlencode({
        "latitude": lat, "longitude": lon, "hourly": "precipitation_probability",
        "start_date": start, "end_date": end, "timezone": "GMT",
    })
    return json.loads(get(f"{FORECAST_API}?{query}"))


def suffixed(hourly, model):
    """The columns of one model's answer named as a many-model answer names them: Open-Meteo
    suffixes `_<model>` only when more than one model answers, so a completion asks one model
    a request and names the columns itself (27 ott 2026: ICON-D2 absent at Rome left ICON-2I's
    columns bare, and the file did not find them)."""
    return {k if k == "time" or k.endswith("_" + model) else f"{k}_{model}": v
            for k, v in hourly.items()}


def write(out, data):
    """Through a temporary file, so that a run cut short never leaves half an archive. The
    rename is retried: on Windows a scanner can hold the file just written for a moment
    (27 ott 2026, PermissionError on the October file after 18 columns)."""
    tmp = out + ".tmp"
    with gzip.open(tmp, "wt", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, separators=(",", ":"))
    for attempt in range(5):
        try:
            os.replace(tmp, out)
            return
        except PermissionError:
            if attempt == 4:
                raise
            time.sleep(1 + attempt)


def add_models(out):
    """The columns of the models in MODELS that an existing file does not have yet, station
    by station and written after each one (14.2: the two HARMONIE Europe). The archive of a
    past forecast does not change, so the columns added later are the ones a full download
    would have brought, for 2 models x 13 variables x 61 days ~ 11 calls a station instead of
    a day of them. A station remembers which models were asked for it (`models`): one the
    archive does not cover answers nothing, and is not asked again."""
    with gzip.open(out, "rt", encoding="utf-8") as f:
        data = json.load(f)
    start, end = dt.date.fromisoformat(data["start"]), dt.date.fromisoformat(data["end"])
    status = 0
    for st in data["stations"]:
        asked = list(st.get("models") or data["models"])
        for model in [m for m in MODELS if m not in asked]:
            try:
                part = forecasts(st["lat"], st["lon"], start, end, [model])
            except NoData as error:
                # The model does not reach this point (Palermo, Cagliari, Madrid, Athens and
                # Longyearbyen for the two HARMONIE Europe; ICON-D2 south of Florence):
                # asked, answered, nothing to add.
                asked.append(model)
                st["models"] = asked
                print(f"   {st['icao']} fuori dal dominio di {model}: {error}")
                write(out, data)
                continue
            except Exception as error:
                print(f"   {st['icao']} ERRORE: {error}")
                status = 1
                continue
            hourly = st["forecast"]["hourly"]
            if part["hourly"]["time"] != hourly["time"]:
                print(f"   {st['icao']} ERRORE: le ore dell'archivio non coincidono con quelle del file")
                status = 1
                continue
            columns = suffixed(part["hourly"], model)
            added = [k for k in columns if k != "time"]
            for k in added:
                hourly[k] = columns[k]
            for k, unit in suffixed(part.get("hourly_units", {}), model).items():
                st["forecast"].setdefault("hourly_units", {}).setdefault(k, unit)
            asked.append(model)
            st["models"] = asked
            print(f"   {st['icao']} ok  {len(added)} colonne di {model}")
            write(out, data)
            time.sleep(3.0)
    # The file's own list follows once every station has been asked; written only when it
    # changes, because a gzip rewritten with the same content is still a new blob for git.
    if data["models"] != list(MODELS) and all(
            set(MODELS) <= set(st.get("models") or data["models"]) for st in data["stations"]):
        data["models"] = list(MODELS)
        write(out, data)
    print(f"\nFatto: modelli del file {data['models']}")
    return status


def add_lead_days(out):
    """The previous runs of the Italian stations of an existing file (14.4), for the models
    of LEAD_MODELS a station does not have yet, written after each one, under
    `previous_runs` with the same hours as the archive. A station remembers which models were
    asked (`lead_models`), as `add_models` does."""
    with gzip.open(out, "rt", encoding="utf-8") as f:
        data = json.load(f)
    status = 0
    for st in data["stations"]:
        if not st["icao"].startswith("LI"):
            continue
        asked = list(st.get("lead_models") or [])
        for model in [m for m in LEAD_MODELS if m not in asked]:
            try:
                part = previous_runs(st["lat"], st["lon"], data["start"], data["end"], [model])
            except NoData as error:
                asked.append(model)
                st["lead_models"] = asked
                print(f"   {st['icao']} fuori dal dominio di {model}: {error}")
                write(out, data)
                continue
            except Exception as error:
                print(f"   {st['icao']} ERRORE: {error}")
                status = 1
                continue
            if part["hourly"]["time"] != st["forecast"]["hourly"]["time"]:
                print(f"   {st['icao']} ERRORE: le ore delle corse precedenti non coincidono con quelle del file")
                status = 1
                continue
            block = st.setdefault("previous_runs", {"hourly": {"time": part["hourly"]["time"]}, "hourly_units": {}})
            columns = suffixed(part["hourly"], model)
            added = [k for k in columns if k != "time"]
            for k in added:
                block["hourly"][k] = columns[k]
            for k, unit in suffixed(part.get("hourly_units", {}), model).items():
                block["hourly_units"].setdefault(k, unit)
            asked.append(model)
            st["lead_models"] = asked
            print(f"   {st['icao']} ok  {len(added)} colonne di corse precedenti di {model}")
            write(out, data)
            time.sleep(3.0)
    print("\nFatto: corse precedenti per",
          [st["icao"] for st in data["stations"] if st.get("previous_runs")])
    return status


def add_probability(out):
    """The probability for the stations of an existing file that do not have it yet, written
    after each one: one variable a station, the lightest request there is."""
    with gzip.open(out, "rt", encoding="utf-8") as f:
        data = json.load(f)
    for st in data["stations"]:
        if st.get("probability"):
            continue
        try:
            st["probability"] = probability(st["lat"], st["lon"], data["start"], data["end"])
        except Exception as error:
            print(f"   {st['icao']} ERRORE: {error}")
            continue
        values = st["probability"].get("hourly", {}).get("precipitation_probability", [])
        print(f"   {st['icao']} ok  {sum(v is not None for v in values)} ore con la probabilita'")
        write(out, data)
        time.sleep(3.0)
    missing = [st["icao"] for st in data["stations"] if not st.get("probability")]
    print(f"\nFatto: {len(data['stations']) - len(missing)} aeroporti con la probabilita', mancano {missing}")
    return 0 if not missing else 1


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    yesterday = dt.date.today() - dt.timedelta(days=1)
    parser.add_argument("--start", type=dt.date.fromisoformat, default=yesterday - dt.timedelta(days=29))
    parser.add_argument("--end", type=dt.date.fromisoformat, default=yesterday)
    here = os.path.dirname(os.path.abspath(__file__))
    parser.add_argument("--out")
    parser.add_argument("--resume", action="store_true",
                        help="tiene gli aeroporti gia' nel file e scarica solo quelli che mancano")
    parser.add_argument("--add-probability", action="store_true",
                        help="aggiunge a un file esistente la probabilita' di precipitazione di best_match")
    parser.add_argument("--add-models", action="store_true",
                        help="aggiunge a un file esistente le colonne dei modelli di MODELS che non ha")
    parser.add_argument("--add-lead-days", action="store_true",
                        help="aggiunge a un file esistente le corse precedenti (giorni 2 e 3) degli aeroporti italiani")
    args = parser.parse_args()
    out = args.out or os.path.join(here, "measurements", f"metar-{args.start}-{args.end}.json.gz")
    if args.add_models:
        return add_models(out)
    if args.add_lead_days:
        return add_lead_days(out)
    if args.add_probability:
        return add_probability(out)

    started = dt.datetime.now(dt.timezone.utc).isoformat(timespec="seconds")
    results, failed = [], []
    if args.resume and os.path.exists(out):
        with gzip.open(out, "rt", encoding="utf-8") as f:
            previous = json.load(f)
        results = previous["stations"]
        # The file keeps the date of its first download: a station added later (Bari and
        # Catania, 14.4) does not make the whole archive look fetched today.
        started = previous.get("fetched_at_utc", started)
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
        results.append({"icao": icao, "name": name, "lat": lat, "lon": lon, "models": list(MODELS),
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
