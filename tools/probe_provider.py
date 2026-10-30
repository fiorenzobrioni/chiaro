#!/usr/bin/env python3
"""La sonda del provider (PLANNING, Fase 14, step 14.1).

Open-Meteo cambia feed e composizione di `best_match` senza preavviso: il 30 nov 2026 DWD toglie
la visibilita' da ICON, ICON-EU e ICON-D2, e l'app lo saprebbe dai lettori. Questo strumento lo
sa prima. Per un elenco di citta' di riferimento chiede due cose:

- **la richiesta dell'app**, con le stesse variabili che `OpenMeteoForecastApi` elenca (lette dal
  sorgente Kotlin, cosi' la sonda non puo' restare indietro), e conta i valori nulli per blocco e
  per variabile;
- **i modelli candidati**, in una richiesta sola (`models=...`: Open-Meteo lascia fuori quelli
  che non coprono il punto), e riconosce **quale modello e' `best_match`** per variabile e giorno
  di anticipo dal criterio di `Witnesses`: serie identiche entro la tolleranza. Lo stesso sui
  quarti d'ora (`minutely_15`): calcolati quando coincidono con un modello che li calcola
  (ICON-D2, HRRR, AROME), altrimenti l'ora tagliata in quattro.

Scrive tutto in `tools/measurements/probe-<data>.json` e stampa la tabella per il registro. Esce
con codice 1 — la **sentinella** — quando una variabile che il motore legge arriva nulla in una
citta' dove la sonda precedente la trovava piena: e' il giorno in cui il provider ha cambiato
qualcosa, e il lavoro comincia da li'.

**Dal 30 ott 2026 guarda anche l'altro feed dell'app**, il bollettino delle allerte ufficiali
della Protezione Civile (`probe_dpc.py`, nel campo `bulletin` del JSON): lo stamp dell'ultimo, la
sua eta', che il CAP si legga ancora e che il ripiego ci sia. Anche quello fa scattare la
sentinella, perche' un canale spento e' muto come «nessuna allerta oggi».

Il peso in chiamate delle richieste dell'app sta nella tabella: Open-Meteo conta una chiamata ogni
dieci variabili e ogni due settimane per luogo (pagina dei prezzi), per indirizzo e per
applicazione, senza chiave: e' il vincolo scritto in CLAUDE.md.

Solo libreria standard di Python 3.8+. Una corsa intera vale circa 700 chiamate: sta sotto i
600 al minuto perche' le richieste sono distanziate, e sotto le 5 000 l'ora.

    python tools/probe_provider.py                 # oggi, confrontata con la sonda precedente
    python tools/probe_provider.py --no-sentinel   # solo la tabella
    python tools/probe_provider.py --cities Milano,Roma
"""

import argparse
import datetime as dt
import glob
import io
import json
import os
import re
import sys
import time
import urllib.error
import urllib.parse
import urllib.request

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import probe_dpc  # noqa: E402  (accanto a questo file: il bollettino della Protezione Civile)

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
API = "https://api.open-meteo.com/v1/forecast"
API_SOURCE = os.path.join(
    ROOT, "core", "data", "src", "main", "kotlin", "com", "callbackdev", "chiaro", "data",
    "remote", "OpenMeteoApis.kt"
)
OUT_DIR = os.path.join(ROOT, "tools", "measurements")

# Nome, codice paese, latitudine, longitudine. L'Italia pesa di piu' (e' il pubblico primo e il
# bordo di ICON-D2 la taglia in due), poi ogni paese che Open-Meteo serve con un modello suo, e
# tre punti fuori dall'Europa per il resto del mondo.
CITIES = [
    ("Milano", "IT", 45.4642, 9.19),
    ("Torino", "IT", 45.0703, 7.6869),
    ("Trieste", "IT", 45.6495, 13.7768),
    ("Bologna", "IT", 44.4949, 11.3426),
    ("Firenze", "IT", 43.7696, 11.2558),
    ("Ancona", "IT", 43.6158, 13.5189),
    ("Perugia", "IT", 43.1107, 12.3908),
    ("Roma", "IT", 41.9028, 12.4964),
    ("Napoli", "IT", 40.8518, 14.2681),
    ("Bari", "IT", 41.1171, 16.8719),
    ("Palermo", "IT", 38.1157, 13.3615),
    ("Cagliari", "IT", 39.2238, 9.1217),
    ("Zurigo", "CH", 47.3769, 8.5417),
    ("Vienna", "AT", 48.2082, 16.3738),
    ("Monaco", "DE", 48.1351, 11.582),
    ("Berlino", "DE", 52.52, 13.405),
    ("Praga", "CZ", 50.0755, 14.4378),
    ("Lubiana", "SI", 46.0569, 14.5058),
    ("Lussemburgo", "LU", 49.6116, 6.1319),
    ("Parigi", "FR", 48.8566, 2.3522),
    ("Lione", "FR", 45.764, 4.8357),
    ("Marsiglia", "FR", 43.2965, 5.3698),
    ("Tolosa", "FR", 43.6047, 1.4442),
    ("Bordeaux", "FR", 44.8378, -0.5792),
    ("Nizza", "FR", 43.7102, 7.262),
    ("Amsterdam", "NL", 52.3676, 4.9041),
    ("Bruxelles", "BE", 50.8503, 4.3517),
    ("Londra", "GB", 51.5074, -0.1278),
    ("Dublino", "IE", 53.3498, -6.2603),
    ("Copenaghen", "DK", 55.6761, 12.5683),
    ("Odense", "DK", 55.4038, 10.4024),
    ("Oslo", "NO", 59.9139, 10.7522),
    ("Stoccolma", "SE", 59.3293, 18.0686),
    ("Helsinki", "FI", 60.1699, 24.9384),
    ("Varsavia", "PL", 52.2297, 21.0122),
    ("Breslavia", "PL", 51.1079, 17.0385),
    ("Budapest", "HU", 47.4979, 19.0402),
    ("Zagabria", "HR", 45.815, 15.9819),
    ("Bratislava", "SK", 48.1486, 17.1077),
    ("Madrid", "ES", 40.4168, -3.7038),
    ("Lisbona", "PT", 38.7223, -9.1393),
    ("Atene", "GR", 37.9838, 23.7275),
    ("New York", "US", 40.7128, -74.006),
    ("Sydney", "AU", -33.8688, 151.2093),
    ("Tokyo", "JP", 35.6762, 139.6503),
]

# I modelli che possono essere `best_match` o un testimone, come Open-Meteo li chiama. Quelli che
# non coprono il punto restano fuori dalla risposta, senza errore.
CANDIDATES = [
    "icon_d2", "icon_eu", "icon_global",
    "ecmwf_ifs", "ecmwf_ifs025",
    "gfs_global", "gfs_hrrr",
    "ukmo_uk_deterministic_2km", "ukmo_global_deterministic_10km",
    "meteofrance_arome_france_hd", "meteofrance_arome_france", "meteofrance_arpege_europe",
    "knmi_harmonie_arome_netherlands", "knmi_harmonie_arome_europe", "dmi_harmonie_arome_europe",
    "metno_nordic",
    "italia_meteo_arpae_icon_2i",
    "meteoswiss_icon_ch1", "meteoswiss_icon_ch2",
    "jma_msm",
]

# I modelli che calcolano il quarto d'ora (pagina di Open-Meteo: ICON-D2, HRRR, AROME).
QUARTER_NATIVE = {"icon_d2", "gfs_hrrr", "meteofrance_arome_france", "meteofrance_arome_france_hd"}

# Le variabili con cui si riconosce il modello: una per famiglia di numeri, piu' la visibilita' e
# l'umidita' che la nebbia legge (STATES, regola 5).
IDENTITY_HOURLY = ["temperature_2m", "cloud_cover", "precipitation", "visibility", "relative_humidity_2m"]
IDENTITY_MINUTELY = ["temperature_2m", "precipitation"]

# La tolleranza di `Witnesses`: temperatura entro 0,05 °C, nubi entro 0,5%, e per il resto
# l'arrotondamento della risposta.
TOLERANCE = {
    "temperature_2m": 0.05, "cloud_cover": 0.5, "precipitation": 0.005,
    "visibility": 1.0, "relative_humidity_2m": 0.5,
}

# Sotto questa quota di ore uguali il modello non e' `best_match`.
SAME_SHARE = 0.9

# La sentinella: una variabile piena ieri (sotto questa quota di nulli) e vuota oggi (sopra l'altra).
FULL_MAX_NULL = 0.1
EMPTY_MIN_NULL = 0.9

PAUSE_S = 1.2


def kotlin_constants():
    """Le liste di variabili dell'app, lette da OpenMeteoApis.kt: le stringhe concatenate di ogni
    `const val NOME = "..." + "..."`, saltando i commenti."""
    src = io.open(API_SOURCE, encoding="utf-8").read()
    lines = [l for l in src.splitlines() if not l.strip().startswith("//")]
    text = "\n".join(lines)
    out = {}
    for m in re.finditer(r"const val (\w+)(?::\s*\w+)?\s*=\s*((?:[^\n]*\n)+?)(?=\s*(?:/\*\*|const val|val |}|interface))", text):
        name, body = m.group(1), m.group(2)
        strings = re.findall(r'"([^"$]*)"', body)
        if name in out:
            continue  # il primo che il sorgente dichiara: le interfacce dopo riusano i nomi
        if strings:
            out[name] = "".join(strings)
        else:
            num = re.search(r"=\s*(\d+)", m.group(0))
            if num:
                out[name] = int(num.group(1))
    return out


def get(params, attempts=6):
    url = API + "?" + urllib.parse.urlencode(params)
    for attempt in range(attempts):
        try:
            with urllib.request.urlopen(url, timeout=60) as r:
                return json.load(r)
        except urllib.error.HTTPError as e:
            body = e.read().decode("utf-8", "replace")
            if e.code in (429, 503) and attempt < attempts - 1:
                time.sleep(10 * (attempt + 1))
                continue
            raise RuntimeError("HTTP %d: %s" % (e.code, body[:300]))
        except (urllib.error.URLError, TimeoutError):
            if attempt < attempts - 1:
                time.sleep(5 * (attempt + 1))
                continue
            raise
    return None


def split(csv):
    return [v for v in csv.split(",") if v]


def weight(n_vars, days):
    """Il peso in chiamate di una richiesta (pagina dei prezzi di Open-Meteo): una chiamata fino a
    dieci variabili e due settimane, poi in proporzione."""
    return max(1.0, n_vars / 10.0) * max(1.0, days / 14.0)


def app_request(lat, lon, k):
    """La richiesta di `OpenMeteoForecastApi.forecast`, tale e quale."""
    return get({
        "latitude": lat, "longitude": lon,
        "current": k["CURRENT_VARIABLES"], "hourly": k["HOURLY_VARIABLES"],
        "daily": k["DAILY_VARIABLES"], "minutely_15": k["MINUTELY_15_VARIABLES"],
        "forecast_days": k["FORECAST_DAYS"], "forecast_minutely_15": k["FORECAST_QUARTERS"],
        "timezone": "auto",
    })


def nulls_of(block, variables):
    out = {}
    for v in variables:
        values = block.get(v)
        if values is None:
            out[v] = 1.0
        elif isinstance(values, list):
            out[v] = round(sum(1 for x in values if x is None) / max(1, len(values)), 3)
        else:
            out[v] = 0.0
    return out


def identity_request(lat, lon, days):
    return get({
        "latitude": lat, "longitude": lon,
        "hourly": ",".join(IDENTITY_HOURLY), "minutely_15": ",".join(IDENTITY_MINUTELY),
        "forecast_days": days, "forecast_minutely_15": 24,
        "models": ",".join(["best_match"] + CANDIDATES), "timezone": "UTC",
    })


def same_share(a, b, tol):
    pairs = [(x, y) for x, y in zip(a, b) if x is not None]
    if not pairs:
        return None
    same = sum(1 for x, y in pairs if y is not None and abs(x - y) <= tol)
    return same / len(pairs)


def who_is(block, variable, start, end):
    """Il candidato le cui ore [start, end) coincidono con quelle di best_match, o None."""
    best = block.get("%s_best_match" % variable)
    if best is None:
        return None, 0.0
    best = best[start:end]
    known = [x for x in best if x is not None]
    if not known:
        return None, 0.0
    # Una serie piatta (sei ore secche, un cielo chiuso tutto il giorno) coincide con chiunque e
    # non dice niente: la temperatura, che non e' mai piatta, resta il riconoscimento di riserva.
    if max(known) - min(known) <= TOLERANCE.get(variable, 0.01):
        return None, None
    tol = TOLERANCE.get(variable, 0.01)
    top, top_share = None, 0.0
    for m in CANDIDATES:
        series = block.get("%s_%s" % (variable, m))
        if series is None:
            continue
        share = same_share(best, series[start:end], tol)
        if share is not None and share > top_share:
            top, top_share = m, share
    if top_share >= SAME_SHARE:
        return top, round(top_share, 3)
    return None, round(top_share, 3)


def quarters_verdict(block):
    """Chi calcola i quarti di best_match, e se sono calcolati: coincidono con un modello che li
    calcola, oppure ogni ora bagnata ha quattro quarti uguali (NowcastCoverage.looksInterpolated)."""
    who = {}
    for v in IDENTITY_MINUTELY:
        m, share = who_is(block, v, 0, 24)
        who[v] = {"model": m, "share": share}
    # La pioggia, quando c'e', e' il riconoscimento che conta (e' quel che la sezione disegna);
    # la temperatura, mai piatta, risponde nelle sei ore secche. Open-Meteo corregge la
    # temperatura di best_match per la quota del punto, quindi puo' non coincidere con nessun
    # modello dove i quarti sono comunque l'ora tagliata in quattro: allora decide la forma.
    precip_model = who["precipitation"]["model"]
    temp_model = who["temperature_2m"]["model"]
    precip = block.get("precipitation_best_match") or []
    wet_hours = [precip[i:i + 4] for i in range(0, len(precip) - 3, 4)
                 if all(x is not None for x in precip[i:i + 4]) and any(x > 0 for x in precip[i:i + 4])]
    flat = bool(wet_hours) and all(len(set(h)) == 1 for h in wet_hours)
    model = precip_model or temp_model
    computed = model in QUARTER_NATIVE and not flat
    return {"who": who, "model": model, "wet_hours": len(wet_hours), "flat": flat, "computed": computed}


def probe_city(name, country, lat, lon, k):
    days = int(k["FORECAST_DAYS"])
    app = app_request(lat, lon, k)
    time.sleep(PAUSE_S)
    ident = identity_request(lat, lon, days)
    time.sleep(PAUSE_S)
    nulls = {
        "current": nulls_of(app.get("current", {}), split(k["CURRENT_VARIABLES"])),
        "hourly": nulls_of(app.get("hourly", {}), split(k["HOURLY_VARIABLES"])),
        "daily": nulls_of(app.get("daily", {}), split(k["DAILY_VARIABLES"])),
        "minutely_15": nulls_of(app.get("minutely_15", {}), split(k["MINUTELY_15_VARIABLES"])),
    }
    hourly = ident.get("hourly", {})
    present = [m for m in CANDIDATES if any(key.endswith("_" + m) for key in hourly)]
    by_day = {}
    for v in IDENTITY_HOURLY:
        by_day[v] = {}
        for d in range(days):
            m, share = who_is(hourly, v, d * 24, (d + 1) * 24)
            by_day[v][str(d + 1)] = {"model": m, "share": share}
    witness_nulls = {}
    for m in CANDIDATES:
        key = "visibility_%s" % m
        if key in hourly:
            witness_nulls[m] = {
                "visibility": nulls_of(hourly, [key])[key],
                "relative_humidity_2m": nulls_of(hourly, ["relative_humidity_2m_%s" % m])["relative_humidity_2m_%s" % m],
            }
    return {
        "name": name, "country": country, "lat": lat, "lon": lon,
        "elevation": app.get("elevation"),
        "models_present": present,
        "best_match_is": by_day,
        "quarters": quarters_verdict(ident.get("minutely_15", {})),
        "nulls": nulls,
        "witness_nulls": witness_nulls,
    }


def previous_probe(out_path):
    files = sorted(glob.glob(os.path.join(OUT_DIR, "probe-*.json")))
    files = [f for f in files if os.path.abspath(f) != os.path.abspath(out_path)]
    if not files:
        return None
    return json.load(io.open(files[-1], encoding="utf-8"))


def sentinel(previous, current):
    """Le variabili piene nella sonda precedente e vuote in questa, citta' per citta'."""
    if previous is None:
        return []
    before = {c["name"]: c for c in previous.get("cities", [])}
    trips = []
    for city in current["cities"]:
        old = before.get(city["name"])
        if old is None:
            continue
        for block, variables in city["nulls"].items():
            for v, ratio in variables.items():
                was = old.get("nulls", {}).get(block, {}).get(v)
                if was is not None and was <= FULL_MAX_NULL and ratio >= EMPTY_MIN_NULL:
                    trips.append("%s: %s.%s era piena (%.0f%% nulli), ora vuota (%.0f%%)"
                                 % (city["name"], block, v, was * 100, ratio * 100))
        for m, variables in city.get("witness_nulls", {}).items():
            for v, ratio in variables.items():
                was = old.get("witness_nulls", {}).get(m, {}).get(v)
                if was is not None and was <= FULL_MAX_NULL and ratio >= EMPTY_MIN_NULL:
                    trips.append("%s: %s.%s (testimone) era piena, ora vuota" % (city["name"], m, v))
    return trips


def short(model):
    if model is None:
        return "—"
    return (model.replace("meteofrance_", "mf_").replace("knmi_harmonie_arome_", "knmi_")
            .replace("dmi_harmonie_arome_", "dmi_").replace("ukmo_uk_deterministic_2km", "ukmo_2km")
            .replace("ukmo_global_deterministic_10km", "ukmo_10km").replace("italia_meteo_arpae_", "")
            .replace("meteoswiss_", "").replace("_deterministic", ""))


def table(result):
    days = result["forecast_days"]
    picks = [1, 2, 3, 5, days]
    head = "| Città | " + " | ".join("G%d" % d for d in picks) + " | Quarti | Visibilità (nulli) |"
    rows = [head, "|---|" + "---|" * (len(picks) + 2)]
    for c in result["cities"]:
        if "best_match_is" not in c:
            rows.append("| %s (%s) | %s |" % (c["name"], c["country"], c.get("error", "?")))
            continue
        cells = [day_label(c["best_match_is"], d) for d in picks]
        q = c["quarters"]
        quarter = "%s, %s" % (short(q["model"]), "calcolati" if q["computed"] else "interpolati")
        vis = c["nulls"]["hourly"].get("visibility", 1.0)
        rows.append("| %s (%s) | %s | %s | %.0f%% |" % (c["name"], c["country"], " | ".join(cells), quarter, vis * 100))
    return "\n".join(rows)


def day_label(by_day, day):
    """Il modello del giorno: quello della pioggia quando la serie non e' piatta, con quello
    della temperatura accanto se e' un altro (`prec/temp`); altrimenti la temperatura."""
    p = by_day["precipitation"][str(day)]["model"]
    t = by_day["temperature_2m"][str(day)]["model"]
    if p and t and p != t:
        return "%s/%s" % (short(p), short(t))
    return short(p or t)


def weights(k):
    n_forecast = (len(split(k["CURRENT_VARIABLES"])) + len(split(k["HOURLY_VARIABLES"]))
                  + len(split(k["DAILY_VARIABLES"])) + len(split(k["MINUTELY_15_VARIABLES"])))
    n_witness = len(split(k["WITNESS_HOURLY_VARIABLES"])) * len(split(k["WITNESS_MODELS"]))
    n_air = len(split(k["AIR_CURRENT_VARIABLES"]))
    days = int(k["FORECAST_DAYS"])
    out = {
        "forecast": {"variables": n_forecast, "days": days, "calls": round(weight(n_forecast, days), 2)},
        "witnesses": {"variables": n_witness, "days": days, "calls": round(weight(n_witness, days), 2)},
        "air_quality": {"variables": n_air, "days": 1, "calls": round(weight(n_air, 1), 2)},
    }
    out["per_fetch"] = round(sum(v["calls"] for v in out.values()), 2)
    return out


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--out", help="file di uscita (default tools/measurements/probe-<oggi>.json)")
    ap.add_argument("--cities", help="solo queste citta', separate da virgola")
    ap.add_argument("--no-sentinel", action="store_true", help="non confrontare con la sonda precedente")
    args = ap.parse_args()
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")

    k = kotlin_constants()
    # Le liste dei testimoni e dell'aria hanno nomi uguali nelle tre interfacce: si distinguono
    # dall'ordine in cui il sorgente le dichiara (forecast, air, geocoding, witness).
    src = io.open(API_SOURCE, encoding="utf-8").read()
    def block(name):
        start = src.index("interface %s" % name)
        return src[start:]
    def const(name, text):
        return kotlin_constants_from(text).get(name)
    k["AIR_CURRENT_VARIABLES"] = const("CURRENT_VARIABLES", block("OpenMeteoAirQualityApi"))
    k["WITNESS_MODELS"] = const("MODELS", block("OpenMeteoWitnessApi"))
    k["WITNESS_HOURLY_VARIABLES"] = const("HOURLY_VARIABLES", block("OpenMeteoWitnessApi"))
    for needed in ("CURRENT_VARIABLES", "HOURLY_VARIABLES", "DAILY_VARIABLES", "MINUTELY_15_VARIABLES",
                   "FORECAST_DAYS", "FORECAST_QUARTERS", "AIR_CURRENT_VARIABLES", "WITNESS_MODELS",
                   "WITNESS_HOURLY_VARIABLES"):
        if not k.get(needed):
            sys.exit("OpenMeteoApis.kt: non trovo %s" % needed)

    today = dt.date.today().isoformat()
    out_path = args.out or os.path.join(OUT_DIR, "probe-%s.json" % today)
    wanted = set(args.cities.split(",")) if args.cities else None
    cities = []
    for name, country, lat, lon in CITIES:
        if wanted and name not in wanted:
            continue
        print("%s…" % name, end=" ", flush=True)
        try:
            cities.append(probe_city(name, country, lat, lon, k))
            print("ok")
        except Exception as e:  # una citta' fallita non ferma la sonda
            print("fallita: %s" % e)
            cities.append({"name": name, "country": country, "lat": lat, "lon": lon, "error": str(e), "nulls": {}})
    print("Bollettino DPC…", end=" ", flush=True)
    bulletin = probe_dpc.probe()
    print("ok" if "error" not in bulletin else "fallito")
    result = {
        "date": today,
        "forecast_days": int(k["FORECAST_DAYS"]),
        "request": {key: k[key] for key in ("CURRENT_VARIABLES", "HOURLY_VARIABLES", "DAILY_VARIABLES",
                                             "MINUTELY_15_VARIABLES", "WITNESS_MODELS",
                                             "WITNESS_HOURLY_VARIABLES", "AIR_CURRENT_VARIABLES")},
        "weights": weights(k),
        "cities": cities,
        "bulletin": bulletin,
    }
    # Il bollettino non si confronta con la sonda precedente: la sua regola e' assoluta (c'e',
    # e' di oggi o di ieri, si legge), quindi scatta anche con --no-sentinel.
    trips = [] if args.no_sentinel else sentinel(previous_probe(out_path), result)
    trips += probe_dpc.trips(bulletin)
    result["sentinel"] = trips
    os.makedirs(OUT_DIR, exist_ok=True)
    with io.open(out_path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(result, f, ensure_ascii=False, indent=1)
        f.write("\n")

    print()
    print(table(result))
    print()
    w = result["weights"]
    print("Peso in chiamate per fetch: previsione %.1f (%d variabili, %d giorni), testimoni %.1f, aria %.1f: %.1f"
          % (w["forecast"]["calls"], w["forecast"]["variables"], w["forecast"]["days"],
             w["witnesses"]["calls"], w["air_quality"]["calls"], w["per_fetch"]))
    print(probe_dpc.line(bulletin))
    print("Scritto %s" % os.path.relpath(out_path, ROOT))
    if trips:
        print()
        print("SENTINELLA: il provider o il canale delle allerte hanno cambiato qualcosa.")
        for t in trips:
            print(" - " + t)
        sys.exit(1)


def kotlin_constants_from(text):
    lines = [l for l in text.splitlines() if not l.strip().startswith("//")]
    text = "\n".join(lines)
    out = {}
    for m in re.finditer(r"const val (\w+)(?::\s*\w+)?\s*=\s*((?:[^\n]*\n)+?)(?=\s*(?:/\*\*|const val|val |}|interface))", text):
        name, body = m.group(1), m.group(2)
        strings = re.findall(r'"([^"$]*)"', body)
        if strings and name not in out:
            out[name] = "".join(strings)
    return out


if __name__ == "__main__":
    main()
