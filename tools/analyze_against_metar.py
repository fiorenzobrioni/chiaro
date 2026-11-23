#!/usr/bin/env python3
"""Analisi della misura contro il cielo osservato (tools/measure_against_metar.py).

Risponde, ora per ora, a quanto le regole del motore e le loro varianti avrebbero detto il
vero, con la verita' presa da fuori dai modelli:

- **nebbia e foschia** contro la visibilita' e i fenomeni dei METAR;
- **il cielo** contro la radiazione solare vista dal satellite (il sole si vedeva o no);
- **la fase** (pioggia, neve, miste) contro i fenomeni dei METAR;
- **la nebbia senza la visibilita' di `best_match`** (14.2): le varianti della regola 5 per
  quando DWD toglie la visibilita' da ICON (30 nov 2026), che leggono solo il codice e
  l'umidita' propri e la visibilita' e l'umidita' dei testimoni; e **i doppioni**, le ore in
  cui la visibilita' di `best_match` e' quella di un testimone;
- **ICON-2I contro `best_match`, ICON-EU e ICON-D2** (14.4) sugli aeroporti italiani, per
  giorno di anticipo (il giorno 1 e' l'archivio, i giorni 2 e 3 le corse precedenti): errore
  medio della temperatura e CSI dell'ora bagnata contro il METAR, fase giusta, cielo giusto
  contro il satellite. Il criterio di decisione e' scritto in PLANNING.md, «14.4», prima dei
  numeri;
- **la concordanza dei modelli** (14.10): per aeroporto, giorno di calendario e giorno di
  anticipo 1-7, lo scarto fra `best_match` e i testimoni dell'app sulla massima e sul cielo,
  letto dalle corse intere per giorno di anticipo (Previous Runs API), contro l'errore che
  `best_match` ha poi fatto (METAR per la massima, satellite per il cielo): con quale soglia
  «i modelli non concordano» e' vera piu' di due volte su tre. Il criterio e' in PLANNING.md,
  «14.10», prima dei numeri;
- **le ore della frase del giorno aperto** (31 ott 2026): per giorno di anticipo 0-6, quanto
  spesso una corsa bagnata di `best_match` («dalle 17:00 alle 20:00») ha davvero qualcosa che
  cade nelle sue ore, contro le parti del giorno che tocca («nel pomeriggio»): fin dove la frase
  dice le ore. Il criterio e' in PLANNING.md, «La frase del giorno aperto», prima dei numeri;
- **la nebbia e la copertura** (16 nov 2026): sulle ore che il motore chiama nebbia (la regola
  5 com'e' nel codice, con la visibilita' propria e senza), quale copertura da' `best_match`,
  cioe' cosa ne direbbe il verdetto del Cielo, che legge la copertura e non lo stato; e, dove
  la copertura lascia il cielo aperto, cosa ha visto l'aeroporto. Nessun criterio di decisione
  prima dei numeri: la domanda era quanto spesso (PLANNING.md, «La misura: la nebbia contro
  la copertura»);
- **la nebbia dei due modelli** (17 nov 2026): la regola 5 com'e' nel codice da quel giorno,
  che non legge la visibilita' di `best_match`, contro quella che sostituisce (con la
  visibilita' di ICON e senza), le sue varianti, la griglia delle soglie, i giorni 4-7 senza
  KNMI e DMI, e l'errore della media dell'umidita' contro il METAR (PLANNING.md, «La nebbia:
  due modelli»);
- **il velato** (17 nov 2026): le ore che la regola 6 chiamava «Sereno» o «Quasi sereno» sotto
  una copertura dell'80% o piu', che dal 17 nov 2026 sono «Velato», contro il satellite: quante
  sono, e se il sole c'era (PLANNING.md, «Velato: il passo, pronto da eseguire»);
- **i pericoli del provider** (17 nov 2026): il temporale della regola 1 e il gelicidio della
  regola 2, che passano il codice del provider, contro i TS, i VCTS, gli FZRA e gli FZDZ dei
  METAR, ora per ora e, per il temporale, giorno per giorno; con le varianti che una regola
  diversa sarebbe (un'altra soglia di probabilita', la CAPE, il potenziale di fulmini di
  ICON-D2, il gelicidio dedotto dal liquido sotto zero). I file sono quelli di
  `tools/measure_hazards.py`, che hanno le grandezze convettive, e gli archivi `metar-*`, che
  non le hanno (PLANNING.md, «La review del motore, punto 4»). Dal 19 nov 2026 (Fase 17.4)
  anche **il temporale possibile del giorno** (`PossibleStorm` nell'app): il codice, e nei
  giorni in cui tace la CAPE, per giorno UTC e per zona (la tabella del punto 3 di PLANNING,
  «Fase 17»), per soglia, sul giorno del luogo come lo conta l'app, e quanto spesso il
  temporale cade nella parte del giorno detta (tzdata serve a `zoneinfo` su Windows);
- **i rovesci** (19 nov 2026, Fase 17.6): sulle ore che la regola 3 chiama pioggia e che
  l'aeroporto ha visto bagnate, quando la pioggia e' un rovescio contro gli SH e i TS dei METAR:
  la regola di `showers` (fino a quel giorno), la CAPE, la forma dell'episodio e le loro
  combinazioni, per fetta e per stagione, con e senza le ore di temporale; in fondo la tavola
  della decisione (PLANNING, «17.6»).

- **la media dei modelli nei numeri e nel cielo** (22 nov 2026, Fase 18): `--blend`, la
  temperatura di `best_match`, di IFS, di UKMO, della media dei tre (un testimone che e'
  `best_match` stesso, `same_model`, fuori) e delle tre rampe del peso, per giorno di anticipo
  1-7 e per gruppo, contro il METAR; massima e minima del giorno; quanto cambia la stessa ora
  da una corsa alla successiva; il criterio di 18.2. `--sky-blend`, il cielo dalla luce degli
  strati di ogni modello e degli strati medi contro il satellite al giorno 1, i giorni 1-7
  sulla copertura totale, la copertura contro le okta dei METAR, la quota di «Velato»; il
  criterio di 18.3. I criteri sono in PLANNING.md, «Fase 18», «I criteri, scritti prima
  dell'estate», e decidono sull'estate 2026: su ogni altro periodo il verdetto si stampa e
  non decide.

Dal 19 nov 2026 i rovesci e il temporale possibile del giorno stampano, accanto al CSI, l'HSS
(Heidke), il bias e il CSI di «sempre», cioe' la frequenza dell'evento: il CSI non conta i «no»
giusti, e dove l'evento e' frequente dirlo sempre ha gia' un CSI alto (PLANNING, «La misura
dei rovesci con l'HSS»).

Le ore sono quelle dell'app (WeatherReportMapper.rows): la riga i prende gli istanti
(visibilita', nuvole, umidita') dallo slot i e i valori d'intervallo (quantita', neve) dallo
slot i+1, che e' l'ora i..i+1. Il METAR della riga e' quello piu' vicino all'istante i (entro
30 minuti); il satellite e' lo slot i+1, la media dell'ora i..i+1.

Limiti, detti subito: l'archivio delle previsioni e' fatto delle prime ore di ogni corsa (e'
la previsione del giorno 1, non del giorno 5; i giorni 2 e 3 ci sono solo per l'Italia, da
14.4, e le corse dei giorni 1-7 di `best_match` e dei testimoni, da 14.10, hanno la temperatura
e la copertura totale soltanto); la probabilita' di precipitazione c'e' solo per `best_match`, e la regola del 20% non
si puo' rifare per gli altri modelli; un aeroporto non e' un centro citta'.

    python tools/analyze_against_metar.py tools/measurements/metar-*.json.gz   # uno o piu' periodi, sommati
    python tools/analyze_against_metar.py --wet-timing tools/measurements/metar-*.json.gz   # solo la frase del giorno
    python tools/analyze_against_metar.py --fog-cover tools/measurements/metar-*.json.gz    # solo la nebbia e la copertura
    python tools/analyze_against_metar.py --fog-votes tools/measurements/metar-*.json.gz    # solo la nebbia dei due modelli
    python tools/analyze_against_metar.py --veiled tools/measurements/metar-*.json.gz       # solo il velato
    python tools/analyze_against_metar.py --hazards tools/measurements/hazards-*.json.gz tools/measurements/metar-2025-*.json.gz   # solo temporale e gelicidio
    python tools/analyze_against_metar.py --showers tools/measurements/hazards-*.json.gz   # solo i rovesci
    python tools/analyze_against_metar.py --blend tools/measurements/metar-*.json.gz        # la temperatura della media (18.2)
    python tools/analyze_against_metar.py --sky-blend tools/measurements/metar-*.json.gz    # il cielo della media (18.3)
    python tools/analyze_against_metar.py --sky-ties tools/measurements/metar-*.json.gz     # il pareggio delle parti del giorno
"""

import bisect
import collections
import datetime as dt
import gzip
import json
import math
import sys

FOG_M = 1000.0
MIST_M = 5000.0
MEASURABLE_MM = 0.1
SNOW_CM_PER_MM = 0.7
WITNESSES = ["ecmwf_ifs", "ukmo_global_deterministic_10km", "gfs_global"]
PRECIP_TOKENS = ("DZ", "RA", "SN", "SG", "PL", "GR", "GS", "UP")


# ----------------------------------------------------------------------------- osservazioni

def parse_time(s):
    return dt.datetime.strptime(s, "%Y-%m-%d %H:%M")


def number(s):
    return None if s in (None, "", "null", "M") else float(s)


def observations(metar):
    """The reports as (time, record), sorted."""
    out = []
    for r in metar:
        wx = (r.get("wxcodes") or "")
        wx = "" if wx == "null" else wx
        tokens = [t for t in wx.split() if not t.startswith("VC") and not t.startswith("RE")]
        vis_mi = number(r.get("vsby"))
        temp_f = number(r.get("tmpf"))
        precip = any(p in t for t in tokens for p in PRECIP_TOKENS)
        snow = any(("SN" in t or "SG" in t) for t in tokens)
        rain = any(("RA" in t or "DZ" in t) for t in tokens)
        fog_code = any("FG" in t and not t.startswith(("MI", "BC", "PR")) for t in tokens)
        out.append((parse_time(r["valid"]), {
            "vis": None if vis_mi is None else vis_mi * 1609.344,
            "temp_c": None if temp_f is None else (temp_f - 32) * 5 / 9,
            "precip": precip, "snow": snow, "rain": rain, "fog_code": fog_code, "wx": wx,
        }))
    out.sort(key=lambda x: x[0])
    return out


def nearest(obs, at):
    """The report nearest to [at] within 30 minutes, or None."""
    lo, hi = 0, len(obs)
    while lo < hi:
        mid = (lo + hi) // 2
        if obs[mid][0] < at:
            lo = mid + 1
        else:
            hi = mid
    best = None
    for k in (lo - 1, lo):
        if 0 <= k < len(obs):
            gap = abs((obs[k][0] - at).total_seconds())
            if gap <= 1800 and (best is None or gap < best[0]):
                best = (gap, obs[k][1])
    return best[1] if best else None


def observed_class(o):
    """fog, mist, precip, clear-air or None (no usable report)."""
    if o is None:
        return None
    if o["precip"]:
        return "precip"
    if (o["vis"] is not None and o["vis"] < FOG_M) or o["fog_code"]:
        return "fog"
    if o["vis"] is not None and o["vis"] < MIST_M:
        return "mist"
    if o["vis"] is None:
        return None
    return "clear"


def observed_phase(o):
    if o is None or not o["precip"]:
        return None
    if o["snow"] and o["rain"]:
        return "mixed"
    if o["snow"]:
        return "snow"
    if o["rain"]:
        return "rain"
    return None


# ------------------------------------------------------------------------------ satellite

def cos_zenith(lat, lon, at):
    """NOAA's solar position, good to a fraction of a degree: enough for a clear-sky ratio."""
    doy = at.timetuple().tm_yday
    hour = at.hour + at.minute / 60
    g = 2 * math.pi / 365 * (doy - 1 + (hour - 12) / 24)
    decl = (0.006918 - 0.399912 * math.cos(g) + 0.070257 * math.sin(g) - 0.006758 * math.cos(2 * g)
            + 0.000907 * math.sin(2 * g) - 0.002697 * math.cos(3 * g) + 0.00148 * math.sin(3 * g))
    eqtime = 229.18 * (0.000075 + 0.001868 * math.cos(g) - 0.032077 * math.sin(g)
                       - 0.014615 * math.cos(2 * g) - 0.040849 * math.sin(2 * g))
    solar_min = hour * 60 + eqtime + 4 * lon
    ha = math.radians(solar_min / 4 - 180)
    phi = math.radians(lat)
    return math.sin(phi) * math.sin(decl) + math.cos(phi) * math.cos(decl) * math.cos(ha)


def clearness(lat, lon, slot_time, ghi):
    """Measured over clear-sky (Haurwitz) for the hour that ENDS at [slot_time]; None when the
    sun is under 15 degrees for that hour, where the ratio stops meaning anything."""
    if ghi is None:
        return None
    mid = slot_time - dt.timedelta(minutes=30)
    cz = cos_zenith(lat, lon, mid)
    if cz < math.sin(math.radians(15)):
        return None
    clear = 1098 * cz * math.exp(-0.057 / cz)
    return ghi / clear


def seen_sky(k):
    """The sky as a person saw it: sun, some sun, no sun. The thresholds are the usual ones of
    the clearness index (under 0.4 the sun's disc is gone, over 0.7 it dominates)."""
    if k is None:
        return None
    return "sunny" if k >= 0.7 else "partly" if k >= 0.4 else "cloudy"


# ----------------------------------------------------------------------------------- modelli

class Series:
    def __init__(self, hourly, model):
        self.h = hourly
        self.m = model

    def __call__(self, var, i):
        xs = self.h.get(f"{var}_{self.m}")
        if xs is None or not (0 <= i < len(xs)):
            return None
        return xs[i]

    def has(self, var):
        xs = self.h.get(f"{var}_{self.m}")
        return xs is not None and any(x is not None for x in xs)


def low(s, i, limit):
    v = s("visibility", i)
    return v is not None and v <= limit


def persists(s, i, limit):
    return low(s, i, limit) and (low(s, i - 1, limit) or low(s, i + 1, limit))


def wet(s, i):
    mm = s("precipitation", i + 1)
    return mm is not None and mm >= MEASURABLE_MM - 1e-9


def code_fog(s, i):
    return s("weather_code", i) in (45, 48)


def engine_fog(s, i):
    """Rule 5 as the engine has it today: kept where the code says fog and the visibility
    agrees, invented where it persists into a neighbouring hour."""
    return low(s, i, FOG_M) and (code_fog(s, i) or persists(s, i, FOG_M))


def humid(s, i, rh_min):
    rh = s("relative_humidity_2m", i)
    return rh is not None and rh >= rh_min


def confirmed(best, witnesses, i, witness_rh=97, own_rh=97, share=0.0, own_always=False):
    """The app's fog confirmation (`WeatherStateEngine.fogConfirmed`): with witnesses that
    have a visibility or a humidity, more than [share] of them (and at least one) see fog,
    under 1 km or at witness_rh; without them, the hour's own humidity at own_rh; with
    neither, nothing contradicts. [own_always]: the own humidity is asked in every case."""
    rh = best("relative_humidity_2m", i)
    if own_always and rh is not None and rh < own_rh:
        return False
    voters = [w for w in witnesses
              if w("visibility", i) is not None or w("relative_humidity_2m", i) is not None]
    if voters:
        yes = sum(low(w, i, FOG_M) or humid(w, i, witness_rh) for w in voters)
        return yes >= 1 and yes / len(voters) > share
    return True if rh is None else rh >= own_rh


def phase(s, i):
    mm = s("precipitation", i + 1)
    sn = s("snowfall", i + 1)
    if mm is None or mm < MEASURABLE_MM - 1e-9 or sn is None:
        return None
    snow_water = sn / SNOW_CM_PER_MM
    liquid = max(0.0, mm - snow_water)
    if liquid >= MEASURABLE_MM and snow_water >= MEASURABLE_MM:
        return "mixed"
    return "snow" if snow_water > liquid else "rain"


def same_model(a, b, i, reach=6, min_hours=6):
    """b is a copy of a around hour i: the temperature within 0.05 °C and the cloud within
    0.5% in every hour of reach either side that has both. The app's own test
    (`Witnesses.copies` in the mapper), so the measure scores what the app will do."""
    hours = 0
    for k in range(i - reach, i + reach + 1):
        t, c, bt, bc = b("temperature_2m", k), b("cloud_cover", k), a("temperature_2m", k), a("cloud_cover", k)
        if None in (t, c, bt, bc):
            continue
        if abs(t - bt) > 0.05 or abs(c - bc) > 0.5:
            return False
        hours += 1
    return hours >= min_hours


def sky_bucket(cc):
    """The engine's sky, folded to what a person sees: clear and mainly clear are sun, partly
    cloudy is some sun, overcast is none."""
    if cc is None:
        return None
    return "sunny" if cc < 50 else "partly" if cc < 80 else "cloudy"


def combined(l, m, h=None, high_weight=0.0):
    """Cover of layers seen together, random overlap: 1 − Π(1 − f)."""
    if l is None or m is None:
        return None
    clear = (1 - l / 100) * (1 - m / 100)
    if h is not None and high_weight:
        clear *= 1 - high_weight * h / 100
    return 100 * (1 - clear)


def opacity(l, m, h):
    """The share of the sun's light the hour's cloud takes, as the app's sky canvas already
    draws it (`cloudOpacity`, 16 ott 2026): a low layer in full, a middle one at 0.8 of its
    share, a high one at 0.3, combined like the cover."""
    if l is None or m is None or h is None:
        return None
    return 100 * (1 - (1 - l / 100) * (1 - 0.8 * m / 100) * (1 - 0.3 * h / 100))


# ------------------------------------------------------------------------------------ pioggia

def wet_seen(obs, start):
    """Precipitation in the hour that starts at [start]: any report inside it with a
    phenomenon falling at the station (not «in the vicinity», not «recent»)."""
    end = start + dt.timedelta(hours=1)
    reports = [o for t, o in obs if start <= t < end]
    if not reports:
        return None
    return any(o["precip"] for o in reports)


def liquid_rate(s, i):
    mm, sn = s("precipitation", i + 1), s("snowfall", i + 1)
    if mm is None:
        return None
    return max(0.0, mm - (sn or 0.0) / SNOW_CM_PER_MM)


def drizzle_like(s, i):
    """The engine's drizzle: measurable, liquid, under 0.5 mm/h, not convective."""
    mm = s("precipitation", i + 1)
    if mm is None or mm < MEASURABLE_MM - 1e-9:
        return False
    liquid = liquid_rate(s, i)
    showers = s("showers", i + 1) or 0.0
    return liquid >= (mm - liquid) and liquid < 0.5 and min(showers, liquid) < liquid / 2


def improver_drizzle(s, i):
    """IMPROVER's extra condition for drizzle: a visibility under 5 km or a low deck."""
    v, low_cc = s("visibility", i), s("cloud_cover_low", i)
    return (v is not None and v < MIST_M) or (low_cc is not None and low_cc >= 85)


def rain_report(data):
    """When does the app call an hour wet, against the METARs of that hour."""
    scores = collections.OrderedDict()
    likely = collections.Counter()
    drizzle = collections.Counter()
    stations = 0
    for st in data["stations"]:
        prob = (st.get("probability") or {}).get("hourly", {})
        pop_by_time = dict(zip(prob.get("time", []), prob.get("precipitation_probability", [])))
        if not pop_by_time:
            continue
        stations += 1
        h = st["forecast"]["hourly"]
        times = [dt.datetime.strptime(t, "%Y-%m-%dT%H:%M") for t in h["time"]]
        obs = observations(st["metar"])
        best = Series(h, "best_match")
        for i in range(len(times) - 1):
            seen = wet_seen(obs, times[i])
            mm = best("precipitation", i + 1)
            pop = pop_by_time.get(h["time"][i + 1])
            if seen is None or mm is None or pop is None:
                continue
            measurable = mm >= MEASURABLE_MM - 1e-9

            def add(name, said):
                scores.setdefault(name, Score()).add(said, seen)

            for floor in (0, 10, 20, 30, 40, 50):
                add(f"quantità ≥ 0,1 mm e probabilità ≥ {floor}%", measurable and pop >= floor)
            for floor in (30, 40, 50, 60):
                add(f"solo probabilità ≥ {floor}% (come IMPROVER)", pop >= floor)
            add("quantità ≥ 0,2 mm e probabilità ≥ 20%", mm >= 0.2 - 1e-9 and pop >= 20)
            add("motore attuale + pioviggine come IMPROVER",
                measurable and pop >= 20 and (not drizzle_like(best, i) or improver_drizzle(best, i)))
            if not measurable and pop >= 60:
                likely["ore"] += 1
                likely["bagnate"] += seen
            if measurable and pop >= 20 and drizzle_like(best, i):
                key = "con la condizione" if improver_drizzle(best, i) else "senza la condizione"
                drizzle[f"{key}: ore"] += 1
                drizzle[f"{key}: bagnate"] += seen
    if not stations:
        print("PIOGGIA: nessun aeroporto ha la probabilità (measure_against_metar.py --add-probability)\n")
        return
    print(f"PIOGGIA contro i METAR dell'ora, {stations} aeroporti")
    for name, sc in scores.items():
        print(sc.row(name).replace(" (di cui su aria limpida   0)", ""))
    if likely["ore"]:
        print(f"  «probabile» (niente di misurabile, ≥ 60%): {likely['ore']} ore, "
              f"bagnate {likely['bagnate'] / likely['ore']:.0%}")
    for key in ("con la condizione", "senza la condizione"):
        n = drizzle[f"{key}: ore"]
        if n:
            print(f"  pioviggine del motore {key} di IMPROVER: {n} ore, bagnate {drizzle[f'{key}: bagnate'] / n:.0%}")
    print()


# --------------------------------------------------- la nebbia senza la visibilità propria

# The Po valley, the fog that opened the measurement: scored apart (14.2).
PO_VALLEY = {"LIML", "LIMC", "LIME", "LIMF", "LIPX", "LIPE", "LIPZ"}
IFS, UKMO, GFS = "ecmwf_ifs", "ukmo_global_deterministic_10km", "gfs_global"
KNMI, DMI = "knmi_harmonie_arome_europe", "dmi_harmonie_arome_europe"
SHORT = {IFS: "IFS", UKMO: "UKMO", GFS: "GFS", KNMI: "KNMI", DMI: "DMI", "icon_eu": "ICON-EU",
         "icon_global": "ICON", "best_match": "best_match", "icon_d2": "ICON-D2"}

# The rows of the per-airport table: the reference, what rule 4 would do, and the candidates.
KEY_VARIANTS = [
    "oggi (visibilità propria + um. ≥ 95 + 1 testimone IFS/UKMO/GFS)",
    "(i) solo il codice del provider",
    "(ii) um. propria ≥ 95 + 1 testimone IFS/UKMO/GFS",
    "(iii) IFS ≤ 1 km e (codice o persistente), um. ≥ 95, + 1 altro IFS/UKMO/KNMI",
    "(iii) min(IFS/UKMO/GFS) ≤ 1 km e (codice o persistente), um. ≥ 95, + 2 testimoni",
    "(iii) min(IFS/UKMO/KNMI/DMI) ≤ 1 km e (codice o persistente), um. ≥ 95, + 2 testimoni",
    "(iii) min(IFS/UKMO/KNMI/DMI) ≤ 1 km e (codice o persistente), um. ≥ 97, + 2 testimoni",
    "(iv) IFS al posto della propria, regola di oggi, IFS vota (si conferma da sé)",
    "(iv) IFS al posto della propria, regola di oggi, IFS non vota",
]


class MinSeries:
    """The lowest visibility the witnesses forecast for each hour: the vote's visibility
    when no single model lends it."""

    def __init__(self, series):
        self.s = series
        self.m = "min(" + "/".join(SHORT[x.m] for x in series) + ")"

    def __call__(self, var, i):
        if var != "visibility":
            return None
        xs = [s("visibility", i) for s in self.s]
        xs = [x for x in xs if x is not None]
        return min(xs) if xs else None


class Split:
    """A Score for the whole, and one per period, per group and per airport."""

    def __init__(self):
        self.by = collections.OrderedDict()

    def add(self, said, seen, gross, keys):
        for k in keys:
            self.by.setdefault(k, Score()).add(said, seen, gross)


def sees_fog(w, i, rh_min=97):
    """The witness' vote of today's rule: under 1 km, or its air at rh_min."""
    return low(w, i, FOG_M) or humid(w, i, rh_min)


def one_agrees(ws, i, rh_min=97):
    return any(sees_fog(w, i, rh_min) for w in ws
               if w("visibility", i) is not None or w("relative_humidity_2m", i) is not None)


def n_agree(ws, i, rh_min=97):
    return sum(sees_fog(w, i, rh_min) for w in ws)


def lent_fog(lender, best, i, with_code=True):
    """Rule 5's shape with a lent visibility: under 1 km on the lender's series, kept by the
    own code or by persistence on the same series."""
    if not low(lender, i, FOG_M):
        return False
    return (with_code and code_fog(best, i)) or persists(lender, i, FOG_M)


def fog_without_own_visibility(data):
    """Rule 5 for December (PLANNING, Fase 14, step 14.2): `best_match`'s visibility is not
    read at all, as it will not be there once DWD drops it from ICON, and every variant is
    scored on what remains, the own code and humidity and the witnesses' visibility and
    humidity. Reported whole, per period, with the Po valley apart, and per airport."""
    split = collections.OrderedDict()
    seen_hours = collections.Counter()
    observed = collections.Counter()

    def score(name):
        return split.setdefault(name, Split())

    for st in data["stations"]:
        h = st["forecast"]["hourly"]
        times = [dt.datetime.strptime(t, "%Y-%m-%dT%H:%M") for t in h["time"]]
        obs = observations(st["metar"])
        best = Series(h, "best_match")
        models = {m: Series(h, m) for m in (IFS, UKMO, GFS, KNMI, DMI)}
        has = {m for m, s in models.items() if s.has("visibility") or s.has("relative_humidity_2m")}
        group = "pianura padana" if st["icao"] in PO_VALLEY else "altrove"
        keys = [("tutto",), ("periodo", st["period"]), ("gruppo", group), ("aeroporto", st["icao"])]

        for i in range(1, len(times) - 1):
            o = nearest(obs, times[i])
            seen = observed_class(o)
            if seen is None or seen == "precip" or wet(best, i):
                continue
            is_fog = seen == "fog"
            clear_air = o["vis"] is not None and o["vis"] >= MIST_M
            seen_hours[("tutto",)] += 1
            seen_hours[("periodo", st["period"])] += 1
            seen_hours[("gruppo", group)] += 1
            observed[st["icao"]] += is_fog
            # The witnesses of the hour, as the mapper keeps them: not a copy of best_match.
            w = {m: s for m, s in models.items() if m in has and not same_model(best, s, i)}
            sets = {
                "IFS/UKMO/GFS": [w[m] for m in (IFS, UKMO, GFS) if m in w],
                "IFS/UKMO/GFS/KNMI": [w[m] for m in (IFS, UKMO, GFS, KNMI) if m in w],
                "IFS/UKMO/KNMI": [w[m] for m in (IFS, UKMO, KNMI) if m in w],
                "IFS/UKMO/KNMI/DMI": [w[m] for m in (IFS, UKMO, KNMI, DMI) if m in w],
                "IFS/UKMO/DMI": [w[m] for m in (IFS, UKMO, DMI) if m in w],
            }

            def add(name, said):
                score(name).add(said, is_fog, clear_air, keys)

            # The reference: today's engine, with its own visibility.
            add(KEY_VARIANTS[0], engine_fog(best, i)
                and confirmed(best, sets["IFS/UKMO/GFS"], i, 97, 95, 0.0, own_always=True))
            # (i) The code alone: rule 4 of STATES §4 with a null visibility.
            add(KEY_VARIANTS[1], code_fog(best, i))
            add("(i) il codice + um. propria ≥ 95", code_fog(best, i) and humid(best, i, 95))
            add("(i) il codice + um. propria ≥ 95 + 1 testimone IFS/UKMO/GFS",
                code_fog(best, i) and humid(best, i, 95) and one_agrees(sets["IFS/UKMO/GFS"], i))
            # (ii) Humidity and a witness, no visibility of any kind on the own side.
            for label, ws in sets.items():
                for rh in (95, 97):
                    add(f"(ii) um. propria ≥ {rh} + 1 testimone {label}", humid(best, i, rh) and one_agrees(ws, i))
                add(f"(ii) um. propria ≥ 95 + 2 testimoni {label}", humid(best, i, 95) and n_agree(ws, i) >= 2)
            # Each witness' visibility alone, persistent: which one lends best, and whether
            # GFS's visibility means anything.
            for m in (IFS, UKMO, GFS, KNMI, DMI):
                if m in w:
                    add(f"{SHORT[m]} ≤ 1 km persistente, da solo", persists(w[m], i, FOG_M))
            # (iii) Persistence on a lent visibility, with the own humidity, with and
            # without another witness.
            lenders = [(SHORT[m], w[m]) for m in (IFS, UKMO, KNMI) if m in w]
            for label in ("IFS/UKMO/GFS", "IFS/UKMO/KNMI", "IFS/UKMO/DMI", "IFS/UKMO/KNMI/DMI", "IFS/UKMO/GFS/KNMI"):
                lenders.append((f"min({label})", MinSeries(sets[label])))
            for label, lender in lenders:
                for rh in (95, 97):
                    for with_code, how in ((True, "codice o persistente"), (False, "persistente")):
                        fog = lent_fog(lender, best, i, with_code) and humid(best, i, rh)
                        add(f"(iii) {label} ≤ 1 km e ({how}), um. ≥ {rh}", fog)
                        if label.startswith("min"):
                            inner = sets[label[4:-1]]
                            add(f"(iii) {label} ≤ 1 km e ({how}), um. ≥ {rh}, + 2 testimoni",
                                fog and n_agree(inner, i) >= 2)
                        else:
                            for others_label in ("IFS/UKMO/GFS", "IFS/UKMO/KNMI"):
                                others = [x for x in sets[others_label] if x is not lender]
                                add(f"(iii) {label} ≤ 1 km e ({how}), um. ≥ {rh}, + 1 altro {others_label}",
                                    fog and one_agrees(others, i))
            # (iv) IFS's visibility in place of the own one, with today's rule as it is:
            # Open-Meteo's fill, which the mapper does not recognise, so IFS also votes.
            if IFS in w:
                ifs = w[IFS]
                base = lent_fog(ifs, best, i, True)
                add(KEY_VARIANTS[7], base and confirmed(best, sets["IFS/UKMO/GFS"], i, 97, 95, 0.0, own_always=True))
                add(KEY_VARIANTS[8], base and confirmed(best, [x for x in sets["IFS/UKMO/GFS"] if x is not ifs],
                                                        i, 97, 95, 0.0, own_always=True))
                add("(iv) IFS al posto della propria, regola di oggi, IFS non vota, + KNMI al posto di GFS",
                    base and confirmed(best, [x for x in sets["IFS/UKMO/KNMI"] if x is not ifs],
                                       i, 97, 95, 0.0, own_always=True))
                add("(iv) IFS al posto della propria, regola di oggi, nessun testimone (solo um. ≥ 95)",
                    base and humid(best, i, 95))

    def table(key, title):
        n = seen_hours[key]
        fog_n = split[KEY_VARIANTS[1]].by[key]
        print(f"\n{title}: {n} ore asciutte con METAR, {fog_n.hit + fog_n.miss} di nebbia")
        for name, sp in split.items():
            if key in sp.by:
                print(sp.by[key].row(name))

    print("\nNEBBIA SENZA LA VISIBILITÀ DI best_match (14.2): la regola 5 di dicembre")
    print("  (nessuna variante legge visibility_best_match, salvo «oggi», il riferimento)")
    table(("tutto",), "Tutto")
    for key in sorted(k for k in seen_hours if k[0] == "periodo"):
        table(key, f"Periodo {key[1]}")
    for key in (("gruppo", "pianura padana"), ("gruppo", "altrove")):
        table(key, key[1].capitalize())

    print("\n  per aeroporto: ore di nebbia osservate, poi «dette/prese» per variante")
    for k, name in enumerate(KEY_VARIANTS):
        print(f"    [{k}] {name}")
    print(f"  {'ICAO':<6}{'viste':>6}" + "".join(f"{'[' + str(k) + ']':>12}" for k in range(len(KEY_VARIANTS))))
    for icao in sorted(observed):
        key = ("aeroporto", icao)
        cells = []
        for name in KEY_VARIANTS:
            s = split[name].by.get(key) if name in split else None
            cells.append("—" if s is None else f"{s.hit + s.false}/{s.hit}")
        print(f"  {icao:<6}{observed[icao]:>6}" + "".join(f"{c:>12}" for c in cells))


def duplicate_report(data):
    """How often `best_match`'s visibility is a witness' own, hour by hour, against how often
    the mapper's test (temperature and cloud, `same_model`) recognises that witness as
    `best_match`: the hours in the first column and not in the second are the fill the vote
    would count twice (PLANNING, 14.1: Marseille's visibility is IFS's under AROME's
    temperature; Italy's will be from 30 Nov 2026)."""
    models = [IFS, UKMO, GFS, "icon_eu", KNMI, DMI]
    print("\nDOPPIONI: ore in cui la visibilità di best_match è identica a quella di un testimone")
    print("  (vis=: visibilità uguale; T&nubi: riconosciuto dal mapper; vis sola: uguale e non riconosciuto)")
    print(f"  {'ICAO':<6}{'periodo':<24}{'ore':>6}" + "".join(f"{SHORT[m]:>22}" for m in models))
    totals = collections.Counter()
    for st in data["stations"]:
        h = st["forecast"]["hourly"]
        best = Series(h, "best_match")
        n = len(h["time"])
        cells = []
        for m in models:
            w = Series(h, m)
            if not w.has("visibility"):
                cells.append("—")
                continue
            vis_eq = same = vis_only = 0
            for i in range(n):
                a, b = best("visibility", i), w("visibility", i)
                if a is None or b is None:
                    continue
                eq = abs(a - b) < 0.5
                sm = same_model(best, w, i)
                vis_eq += eq
                same += sm
                vis_only += eq and not sm
            totals[(m, "vis")] += vis_eq
            totals[(m, "same")] += same
            totals[(m, "only")] += vis_only
            cells.append(f"{vis_eq}/{same}/{vis_only}")
        print(f"  {st['icao']:<6}{st['period']:<24}{n:>6}" + "".join(f"{c:>22}" for c in cells))
    print(f"  {'tutti':<30}{'':>6}" + "".join(
        f"{totals[(m, 'vis')]}/{totals[(m, 'same')]}/{totals[(m, 'only')]:>0}".rjust(22) for m in models))
    both = equal = same = 0
    for st in data["stations"]:
        h = st["forecast"]["hourly"]
        k, d = Series(h, KNMI), Series(h, DMI)
        if not (k.has("visibility") and d.has("visibility")):
            continue
        for i in range(len(h["time"])):
            a, b = k("visibility", i), d("visibility", i)
            if a is None or b is None:
                continue
            both += 1
            equal += abs(a - b) < 0.5
            same += same_model(k, d, i)
    if both:
        print(f"  KNMI e DMI HARMONIE Europe: visibilità uguale in {equal} ore su {both} ({equal / both:.0%}), "
              f"stesso modello per il mapper in {same} ({same / both:.0%})")


# ------------------------------------------------------------------- ICON-2I in Italia (14.4)

ICON_2I = "italia_meteo_arpae_icon_2i"
LEAD_MODELS = ["best_match", "icon_eu", "icon_d2", ICON_2I]
LEAD_SHORT = {"best_match": "best_match", "icon_eu": "ICON-EU", "icon_d2": "ICON-D2", ICON_2I: "ICON-2I"}
# The south: the Italian airports where the probe of 27 Oct 2026 finds no ICON-D2 in
# best_match (Rome, Naples, Bari, Palermo, Cagliari) and Catania, south-east of Palermo. The
# north is the Po valley, where best_match is ICON-D2 on days 1-2 and ICON-EU from day 3.
ITALY_SOUTH = {"LIRF", "LIRN", "LIBD", "LICC", "LICJ", "LIEE"}
ITALY_NORTH = PO_VALLEY
# The decision's margins (PLANNING, 14.4, written before the numbers): ICON-2I enters where it
# beats best_match on the wet hour AND on the temperature, in both periods, by at least this.
CSI_MARGIN = 0.02
MAE_MARGIN_C = 0.2
MIN_WET_SEEN = 30


class Tally:
    """One cell of the comparison: the temperature's absolute error, the wet hour's score,
    the phase and the sky as right/total."""

    def __init__(self):
        self.t_err = 0.0
        self.t_n = 0
        self.wet = Score()
        self.phase_n = self.phase_ok = 0
        self.sky_n = self.sky_ok = 0

    @property
    def mae(self):
        return self.t_err / self.t_n if self.t_n else None

    @property
    def csi(self):
        w = self.wet
        return w.hit / (w.hit + w.miss + w.false) if w.hit + w.miss + w.false else None

    @property
    def wet_seen(self):
        return self.wet.hit + self.wet.miss

    def row(self, label):
        mae = "    —" if self.mae is None else f"{self.mae:5.2f}"
        csi = "   —" if self.csi is None else f"{self.csi:4.2f}"
        ph = "   —" if not self.phase_n else f"{self.phase_ok / self.phase_n:4.0%}"
        sky = "   —" if not self.sky_n else f"{self.sky_ok / self.sky_n:4.0%}"
        w = self.wet
        return (f"  {label:<22} T: ore {self.t_n:>5} MAE {mae} °C   bagnate: viste {self.wet_seen:>4} "
                f"dette {w.hit + w.false:>4} prese {w.hit:>4} false {w.false:>4} CSI {csi}   "
                f"fase {ph} su {self.phase_n:>3}   cielo {sky} su {self.sky_n:>4}")


def lead_series(st, day, model):
    """The model's series for the app's day [day]: the archive for day 1, the previous runs
    (24 and 48 hours before) for days 2 and 3. None when the station has no such column."""
    if day == 1:
        s = Series(st["forecast"]["hourly"], model)
    else:
        block = st.get("previous_runs")
        if not block:
            return None
        s = Series(block["hourly"], f"previous_day{day - 1}_{model}")
    return s if s.has("temperature_2m") or s.has("precipitation") else None


def icon_2i_report(data):
    """ICON-2I against best_match, ICON-EU and ICON-D2 on the Italian airports, per lead day,
    period, group and airport (PLANNING, 14.4). Every model is scored on the same hours: a
    row counts for the temperature when the METAR has one, for the wet hour when the METAR
    covers the hour, for the phase when both have precipitation, for the sky when the
    satellite sees the sun above 15° and the METAR saw neither precipitation nor fog (the
    hour's own forecast of rain is not a filter here, or the models would be scored on
    different hours)."""
    cells = collections.OrderedDict()
    stations = [st for st in data["stations"] if st["icao"].startswith("LI")]
    if not any(st.get("previous_runs") for st in stations) and not any(
            Series(st["forecast"]["hourly"], ICON_2I).has("temperature_2m") for st in stations):
        print("ICON-2I (14.4): nessun aeroporto ha le colonne di ICON-2I (measure_against_metar.py --add-models)\n")
        return

    def cell(*key):
        return cells.setdefault(key, Tally())

    for st in stations:
        group = "sud" if st["icao"] in ITALY_SOUTH else "nord" if st["icao"] in ITALY_NORTH else None
        if group is None:
            continue
        h = st["forecast"]["hourly"]
        times = [dt.datetime.strptime(t, "%Y-%m-%dT%H:%M") for t in h["time"]]
        obs = observations(st["metar"])
        sat = (st.get("satellite") or {}).get("hourly", {})
        sat_ghi = dict(zip(sat.get("time", []), sat.get("shortwave_radiation", [])))
        for day in (1, 2, 3):
            series = {m: lead_series(st, day, m) for m in LEAD_MODELS}
            series = {m: s for m, s in series.items() if s is not None}
            if ICON_2I not in series or "best_match" not in series:
                continue
            for i in range(1, len(times) - 1):
                o = nearest(obs, times[i])
                seen = observed_class(o)
                seen_wet = wet_seen(obs, times[i])
                ph_obs = observed_phase(o)
                slot = h["time"][i + 1]
                truth = seen_sky(clearness(st["lat"], st["lon"], times[i + 1], sat_ghi.get(slot)))
                sky_hour = truth is not None and seen not in ("precip", "fog")
                for m, s in series.items():
                    keys = [(m, day, "tutto", group), (m, day, st["period"], group),
                            (m, day, st["period"], st["icao"])]
                    t = s("temperature_2m", i)
                    mm = s("precipitation", i + 1)
                    said_phase = phase(s, i) if ph_obs is not None else None
                    if day == 1:
                        cc = opacity(*(s(v, i) for v in ("cloud_cover_low", "cloud_cover_mid", "cloud_cover_high")))
                    else:
                        cc = s("cloud_cover", i)
                    said_sky = sky_bucket(cc) if sky_hour else None
                    for key in keys:
                        c = cell(*key)
                        if o is not None and o["temp_c"] is not None and t is not None:
                            c.t_err += abs(t - o["temp_c"])
                            c.t_n += 1
                        if seen_wet is not None and mm is not None:
                            c.wet.add(mm >= MEASURABLE_MM - 1e-9, seen_wet)
                        if said_phase is not None:
                            c.phase_n += 1
                            c.phase_ok += said_phase == ph_obs
                        if said_sky is not None:
                            c.sky_n += 1
                            c.sky_ok += said_sky == truth

    periods = sorted({k[2] for k in cells if k[2] != "tutto"})
    print("ICON-2I CONTRO best_match, ICON-EU E ICON-D2 (14.4), aeroporti italiani")
    print("  giorno 1 = l'archivio; giorno 2 e 3 = le corse di 24 e 48 ore prima (Previous Runs API)")
    print("  T contro il METAR; ora bagnata ≥ 0,1 mm contro il METAR dell'ora, senza probabilità;")
    print("  fase dove METAR e modello hanno precipitazione; cielo contro il satellite (giorno 1 con gli")
    print("  strati, regola 6; giorni 2-3 con la copertura totale)")
    for group in ("sud", "nord"):
        for period in ["tutto"] + periods:
            title = "entrambi i periodi" if period == "tutto" else period
            print(f"\n  {group.upper()} — {title}")
            for day in (1, 2, 3):
                for m in LEAD_MODELS:
                    c = cells.get((m, day, period, group))
                    if c is not None:
                        print(c.row(f"giorno {day}  {LEAD_SHORT[m]}"))

    print("\n  per aeroporto (MAE °C / CSI), best_match → ICON-2I")
    header = f"  {'ICAO':<6}{'periodo':<24}" + "".join(f"{'giorno ' + str(d):>22}" for d in (1, 2, 3))
    print(header)
    for group in ("sud", "nord"):
        for icao in sorted(ITALY_SOUTH if group == "sud" else ITALY_NORTH):
            for period in periods:
                row = []
                for day in (1, 2, 3):
                    a, b = cells.get(("best_match", day, period, icao)), cells.get((ICON_2I, day, period, icao))
                    if a is None or b is None or a.mae is None or a.csi is None or b.csi is None:
                        row.append("—")
                        continue
                    row.append(f"{a.mae:.2f}/{a.csi:.2f} → {b.mae:.2f}/{b.csi:.2f}")
                if any(x != "—" for x in row):
                    print(f"  {icao:<6}{period:<24}" + "".join(f"{x:>22}" for x in row))

    # The decision, as written before the numbers: the south on days 1 and 2, the north on
    # day 3, against best_match, both margins, both periods, enough wet hours seen.
    print("\n  IL CRITERIO (PLANNING, 14.4): CSI + 0,02 e MAE − 0,2 °C contro best_match, in ogni periodo")
    verdicts = {}
    for group, days in (("sud", (1, 2)), ("nord", (3,))):
        for day in days:
            wins, thin = True, False
            for period in periods:
                a, b = cells.get(("best_match", day, period, group)), cells.get((ICON_2I, day, period, group))
                if a is None or b is None or a.csi is None or b.csi is None or a.mae is None or b.mae is None:
                    wins = False
                    print(f"    {group} giorno {day} {period}: cella vuota")
                    continue
                d_csi, d_mae = b.csi - a.csi, a.mae - b.mae
                ok = d_csi >= CSI_MARGIN and d_mae >= MAE_MARGIN_C
                thin = thin or min(a.wet_seen, b.wet_seen) < MIN_WET_SEEN
                wins = wins and ok
                print(f"    {group} giorno {day} {period}: ΔCSI {d_csi:+.3f} ΔMAE {d_mae:+.2f} °C "
                      f"(bagnate viste {b.wet_seen}) → {'vince' if ok else 'non vince'}")
            verdicts[(group, day)] = "non deciso (poche ore bagnate)" if wins and thin else "entra" if wins else "non entra"
    for (group, day), v in verdicts.items():
        print(f"  → ICON-2I al {group}, giorno {day}: {v}")
    print()


# ------------------------------------------------- la concordanza dei modelli (14.10)

# The witnesses 14.11 would read, the app's four (`OpenMeteoWitnessApi.MODELS`); from day 3
# only IFS and UKMO have a run (the two HARMONIE Europe run 60 hours, in the app too).
AGREE_WITNESSES = [IFS, UKMO, KNMI, DMI]
AGREE_DAYS = range(1, 8)
# "A large error" (PLANNING, 14.10, written before the numbers): the day's high 2 °C off,
# twice the threshold at which «What changed» told a revision (`ForecastDiff`, 1 °C; the
# section left the app on 23 nov 2026); 3 °C is printed beside it to read, never to decide. The sky's: best_match's bucket
# wrong in more than half of the day's measurable hours.
HIGH_ERROR_C = 2.0
HIGH_ERROR_READ_C = 3.0
HIGH_THRESHOLDS_C = (1, 2, 3, 4, 5, 6)
SKY_THRESHOLDS_PCT = (20, 30, 40, 50, 60)
MIN_METAR_HOURS = 18     # hours of the day with a report within 30 min, or the high is not seen
MIN_SKY_HOURS = 3        # measurable daylight hours, or the day has no sky
COPY_MIN_HOURS = 12      # hours a witness must equal best_match on to be its copy for the day
# The decision's three conditions, on days 3-7 pooled over both periods.
PRECISION_MIN = 2 / 3
LIFT_MIN = 0.15
SAID_MIN = 0.05


def spearman(xs, ys):
    """Spearman's rank correlation, ties averaged; None under three pairs."""
    if len(xs) < 3:
        return None

    def ranks(vs):
        order = sorted(range(len(vs)), key=lambda k: vs[k])
        r = [0.0] * len(vs)
        i = 0
        while i < len(order):
            j = i
            while j + 1 < len(order) and vs[order[j + 1]] == vs[order[i]]:
                j += 1
            for k in range(i, j + 1):
                r[order[k]] = (i + j) / 2 + 1
            i = j + 1
        return r

    rx, ry = ranks(xs), ranks(ys)
    mx, my = sum(rx) / len(rx), sum(ry) / len(ry)
    sxy = sum((a - mx) * (b - my) for a, b in zip(rx, ry))
    sxx = sum((a - mx) ** 2 for a in rx)
    syy = sum((b - my) ** 2 for b in ry)
    return sxy / math.sqrt(sxx * syy) if sxx and syy else None


def day_copy(best, w, idx):
    """[w] is best_match's own model on these hours: temperature within 0.05 °C and cloud
    within 0.5% wherever both have them, on at least COPY_MIN_HOURS hours (the app's
    `Witnesses.copies`, on the day's hours instead of six either side)."""
    hours = 0
    for i in idx:
        t, c, bt, bc = w("temperature_2m", i), w("cloud_cover", i), best("temperature_2m", i), best("cloud_cover", i)
        if None in (t, c, bt, bc):
            continue
        if abs(t - bt) > 0.05 or abs(c - bc) > 0.5:
            return False
        hours += 1
    return hours >= COPY_MIN_HOURS


def run_series(block, day, model):
    """[model]'s run for the app's day [day] in the witnesses' runs block: the Previous Runs
    API names `previous_day0` as the bare variable, so day 1 is `<var>_<model>` and day N
    `<var>_previous_day<N-1>_<model>`."""
    return Series(block["hourly"], model if day == 1 else f"previous_day{day - 1}_{model}")


def agreement_rows(data):
    """One row a station, period, calendar day (UTC) and lead day: the spread of the models
    on the day's high and on its sky, and the error best_match then made (PLANNING, 14.10).
    Also returns, per station, the hours where best_match's `previous_day0` differs from the
    archive: they are the same forecast and must be the same numbers."""
    rows = []
    day0_check = {}
    for st in data["stations"]:
        block = st.get("previous_runs")
        if not block or not st.get("witness_models") or "temperature_2m_best_match" not in block["hourly"]:
            continue
        h = st["forecast"]["hourly"]
        times = [dt.datetime.strptime(t, "%Y-%m-%dT%H:%M") for t in h["time"]]
        obs = observations(st["metar"])
        sat = (st.get("satellite") or {}).get("hourly", {})
        sat_ghi = dict(zip(sat.get("time", []), sat.get("shortwave_radiation", [])))
        archive = Series(h, "best_match")
        run0 = run_series(block, 1, "best_match")
        day0_check[st["icao"]] = sum(
            1 for i in range(len(times))
            if archive("temperature_2m", i) is not None and run0("temperature_2m", i) is not None
            and abs(archive("temperature_2m", i) - run0("temperature_2m", i)) > 0.05)
        by_date = collections.defaultdict(list)
        for i, t in enumerate(times):
            by_date[t.date()].append(i)
        # The observations and the satellite's truth, once a day: the same for every lead.
        for date, idx in sorted(by_date.items()):
            if len(idx) != 24:
                continue
            reports = [nearest(obs, times[i]) for i in idx]
            temps = [o["temp_c"] for o in reports if o is not None and o["temp_c"] is not None]
            obs_high = max(temps) if len(temps) >= MIN_METAR_HOURS else None
            sky_hours = []
            for i in idx:
                if i + 1 >= len(times):
                    continue
                truth = seen_sky(clearness(st["lat"], st["lon"], times[i + 1], sat_ghi.get(h["time"][i + 1])))
                seen = observed_class(nearest(obs, times[i]))
                if truth is not None and seen not in ("precip", "fog"):
                    sky_hours.append((i, truth))
            for day in AGREE_DAYS:
                best = run_series(block, day, "best_match")
                witnesses = [run_series(block, day, m) for m in AGREE_WITNESSES]
                witnesses = [w for w in witnesses if w.has("temperature_2m") and not day_copy(best, w, idx)]
                models = [best] + witnesses
                highs = []
                for s in models:
                    ts = [s("temperature_2m", i) for i in idx]
                    highs.append(None if any(t is None for t in ts) else max(ts))
                have_high = highs[0] is not None and any(x is not None for x in highs[1:])
                row = {"period": st["period"], "icao": st["icao"], "date": date, "day": day,
                       "witnesses": sum(x is not None for x in highs[1:])}
                if have_high and obs_high is not None:
                    seen_highs = [x for x in highs if x is not None]
                    row["spread_high"] = max(seen_highs) - min(seen_highs)
                    row["err_high"] = abs(highs[0] - obs_high)
                if len(sky_hours) >= MIN_SKY_HOURS:
                    means = []
                    for s in models:
                        cs = [s("cloud_cover", i) for i, _ in sky_hours]
                        means.append(None if any(c is None for c in cs) else sum(cs) / len(cs))
                    if means[0] is not None and any(x is not None for x in means[1:]):
                        seen_means = [x for x in means if x is not None]
                        row["spread_sky"] = max(seen_means) - min(seen_means)
                        wrong = sum(sky_bucket(best("cloud_cover", i)) != truth for i, truth in sky_hours)
                        row["err_sky"] = wrong / len(sky_hours)
                if "err_high" in row or "err_sky" in row:
                    rows.append(row)
    return rows, day0_check


def agreement_table(rows, spread_key, err_key, big, thresholds, unit, title, groups, periods):
    """For every group of lead days and period: the days, the base rate of a large error,
    Spearman's rho between spread and error, and for every threshold S how often the word
    would be said, how often it would be true (precision), and how often the complementary
    «the models agree» would be true. Returns the cells the decision reads."""
    cells = {}
    print(f"\n{title}")
    print(f"  {'giorno':<7}{'periodo':<24}{'giorni':>7}{'base':>6}{'ρ':>7}  "
          + "".join(f"{'S≥' + str(s) + unit:>16}" for s in thresholds))
    print(f"  {'':<7}{'':<24}{'':>7}{'':>6}{'':>7}  " + "".join(f"{'dice/vera/conc.':>16}" for _ in thresholds))
    for label, days in groups:
        for period in ["tutto"] + periods:
            sel = [r for r in rows if r["day"] in days and spread_key in r and (period == "tutto" or r["period"] == period)]
            if not sel:
                continue
            n = len(sel)
            errs = [r[err_key] for r in sel]
            spreads = [r[spread_key] for r in sel]
            base = sum(big(e) for e in errs) / n
            rho = spearman(spreads, errs)
            line = f"  {label:<7}{('entrambi' if period == 'tutto' else period):<24}{n:>7}{base:>6.0%}{('   —' if rho is None else f'{rho:7.2f}'):>7}  "
            for s in thresholds:
                said = [r for r in sel if r[spread_key] >= s]
                silent = [r for r in sel if r[spread_key] < s]
                share = len(said) / n
                precision = sum(big(r[err_key]) for r in said) / len(said) if said else None
                concord = sum(not big(r[err_key]) for r in silent) / len(silent) if silent else None
                cells[(label, period, s)] = {"n": n, "base": base, "share": share, "precision": precision,
                                             "concord": concord, "rho": rho}
                p = "  —" if precision is None else f"{precision:3.0%}"
                c = "  —" if concord is None else f"{concord:3.0%}"
                line += f"{share:>5.0%}/{p}/{c}"
            print(line)
    return cells


def agreement_verdict(cells, thresholds, periods, unit, name):
    """The three conditions of PLANNING 14.10 on days 3-7: precision over two thirds in each
    period and pooled, at least LIFT_MIN over the base rate, said on at least SAID_MIN of the
    days. Prints every threshold and returns the ones that pass, with whether «the models
    agree» is true more than two thirds of the time as well."""
    passing = []
    print(f"\n  IL CRITERIO (PLANNING, 14.10) sulla {name}, giorni 3–7: vera > 2/3 in ogni periodo e nell'insieme, "
          f"+{LIFT_MIN:.0%} sul tasso di base, detta in ≥ {SAID_MIN:.0%} dei giorni")
    for s in thresholds:
        pooled = cells.get(("3–7", "tutto", s))
        if pooled is None or pooled["precision"] is None:
            print(f"    S ≥ {s}{unit}: cella vuota")
            continue
        per_period = [cells.get(("3–7", p, s)) for p in periods]
        precise = pooled["precision"] > PRECISION_MIN and all(
            c is not None and c["precision"] is not None and c["precision"] > PRECISION_MIN for c in per_period)
        lift = pooled["precision"] - pooled["base"] >= LIFT_MIN
        said = pooled["share"] >= SAID_MIN
        concord = pooled["concord"] is not None and pooled["concord"] > PRECISION_MIN and all(
            c is not None and c["concord"] is not None and c["concord"] > PRECISION_MIN for c in per_period)
        detail = ", ".join(f"{p[:7]} {c['precision']:.0%}" if c and c["precision"] is not None else f"{p[:7]} —"
                           for p, c in zip(periods, per_period))
        ok = precise and lift and said
        print(f"    S ≥ {s}{unit}: detta {pooled['share']:.0%}, vera {pooled['precision']:.0%} ({detail}), "
              f"base {pooled['base']:.0%} (+{pooled['precision'] - pooled['base']:.0%}), "
              f"«concordano» vera {pooled['concord']:.0%} → {'passa' if ok else 'non passa'}"
              f"{' (la frase «concordano» ' + ('sì' if concord else 'no') + ')' if ok else ''}")
        if ok:
            passing.append((s, concord))
    return passing


def agreement_report(data):
    """The models' agreement against best_match's error (PLANNING, 14.10): per station,
    calendar day and lead day 1-7, the spread between best_match and the witnesses on the
    day's high and on its sky, from the whole runs per lead day (the Previous Runs API), and
    the error best_match then made against the METAR's high and the satellite's sky. The
    decision's criterion is written in PLANNING before the numbers."""
    rows, day0_check = agreement_rows(data)
    if not rows:
        print("CONCORDANZA (14.10): nessun aeroporto ha le corse dei testimoni (measure_against_metar.py --add-witness-runs)\n")
        return
    periods = sorted({r["period"] for r in rows})
    stations = sorted({r["icao"] for r in rows})
    print("LA CONCORDANZA DEI MODELLI CONTRO L'ERRORE DI best_match (14.10)")
    print(f"  {len(stations)} aeroporti; giorno N = previous_day(N−1) della Previous Runs API, la corsa intera;")
    print(f"  testimoni: IFS, UKMO, KNMI, DMI dove hanno la corsa (dal giorno 3 IFS e UKMO), i doppioni tolti;")
    print(f"  massima: la più alta delle 24 ore contro il massimo dei METAR del giorno (≥ {MIN_METAR_HOURS} ore);")
    print(f"  cielo: media della copertura totale sulle ore di luce misurabili (≥ {MIN_SKY_HOURS}) contro il satellite")
    bad = {k: v for k, v in day0_check.items() if v}
    print(f"  previous_day0 di best_match contro l'archivio: {'uguale ora per ora in ogni aeroporto' if not bad else f'ore diverse {bad}'}")
    for day in AGREE_DAYS:
        sel = [r for r in rows if r["day"] == day]
        highs = [r for r in sel if "err_high" in r]
        skies = [r for r in sel if "err_sky" in r]
        w = collections.Counter(r["witnesses"] for r in highs)
        print(f"  giorno {day}: {len(highs):>5} giorni con la massima, {len(skies):>5} con il cielo; testimoni per giorno {dict(sorted(w.items()))}")

    groups = [(str(d), {d}) for d in AGREE_DAYS] + [("3–7", set(range(3, 8)))]
    high_cells = agreement_table(
        rows, "spread_high", "err_high", lambda e: e >= HIGH_ERROR_C, HIGH_THRESHOLDS_C, "°",
        f"MASSIMA — scarto fra i modelli (°C) contro l'errore ≥ {HIGH_ERROR_C:.0f} °C di best_match "
        f"(dice = giorni con la parola; vera = l'errore è grande quando è detta; conc. = è piccolo quando non lo è)",
        groups, periods)
    high_passing = agreement_verdict(high_cells, HIGH_THRESHOLDS_C, periods, "°", "massima")
    agreement_table(
        rows, "spread_high", "err_high", lambda e: e >= HIGH_ERROR_READ_C, HIGH_THRESHOLDS_C, "°",
        f"MASSIMA con l'errore ≥ {HIGH_ERROR_READ_C:.0f} °C (si legge, non decide)",
        [("3–7", set(range(3, 8)))], periods)
    sky_cells = agreement_table(
        rows, "spread_sky", "err_sky", lambda e: e > 0.5, SKY_THRESHOLDS_PCT, "pt",
        "CIELO — scarto fra le medie di copertura (punti) contro best_match sbagliato in più di metà delle ore",
        groups, periods)
    sky_passing = agreement_verdict(sky_cells, SKY_THRESHOLDS_PCT, periods, "pt", "cielo")

    print("\n  MAE della massima di best_match per giorno di anticipo (°C), e con la parola detta / non detta a S ≥ 3°")
    for day in AGREE_DAYS:
        sel = [r for r in rows if r["day"] == day and "err_high" in r]
        if not sel:
            continue
        said = [r["err_high"] for r in sel if r["spread_high"] >= 3]
        silent = [r["err_high"] for r in sel if r["spread_high"] < 3]
        mae = sum(r["err_high"] for r in sel) / len(sel)
        ms = sum(said) / len(said) if said else None
        mq = sum(silent) / len(silent) if silent else None
        print(f"    giorno {day}: MAE {mae:4.2f}  detta {('—' if ms is None else f'{ms:4.2f}')} su {len(said):>4}  "
              f"non detta {('—' if mq is None else f'{mq:4.2f}')} su {len(silent):>4}")

    print("\n  per aeroporto, giorni 3–7, entrambi i periodi: giorni, MAE della massima, ρ, base e vera a S ≥ 3°")
    for icao in stations:
        sel = [r for r in rows if r["icao"] == icao and r["day"] >= 3 and "err_high" in r]
        if not sel:
            continue
        said = [r for r in sel if r["spread_high"] >= 3]
        rho = spearman([r["spread_high"] for r in sel], [r["err_high"] for r in sel])
        base = sum(r["err_high"] >= HIGH_ERROR_C for r in sel) / len(sel)
        prec = sum(r["err_high"] >= HIGH_ERROR_C for r in said) / len(said) if said else None
        print(f"    {icao}  giorni {len(sel):>4}  MAE {sum(r['err_high'] for r in sel) / len(sel):4.2f}  "
              f"ρ {('  —' if rho is None else f'{rho:5.2f}')}  base {base:3.0%}  "
              f"detta {len(said) / len(sel):3.0%} vera {('  —' if prec is None else f'{prec:3.0%}')}")

    print()
    if high_passing:
        s, concord = high_passing[0]
        print(f"  → 14.11 esiste: sulla massima la parola è vera più di due volte su tre da S ≥ {s}°"
              f"{' (soglie che passano: ' + ', '.join(str(x) for x, _ in high_passing) + ')' if len(high_passing) > 1 else ''};"
              f" «I modelli concordano» {'entra' if concord else 'non entra'}")
    else:
        print("  → 14.11 non esiste: nessuna soglia sulla massima passa il criterio")
    if sky_passing:
        print(f"  → il cielo passa da S ≥ {sky_passing[0][0]} punti")
    else:
        print("  → il cielo non passa: nessuna soglia in punti regge il criterio")
    print()


# ---------------------------------------------------------------------------------- punteggi

class Score:
    """Hits, misses, false alarms of a yes/no forecast against a yes/no observation."""

    def __init__(self):
        self.hit = self.miss = self.false = self.none = 0
        self.gross = 0  # said fog over air clear to 5 km or more: the Lissone error

    def add(self, said, seen, gross=False):
        if said and seen:
            self.hit += 1
        elif seen:
            self.miss += 1
        elif said:
            self.false += 1
            self.gross += gross
        else:
            self.none += 1

    def row(self, name):
        said = self.hit + self.false
        pod = self.hit / (self.hit + self.miss) if self.hit + self.miss else 0
        far = self.false / said if said else 0
        csi = self.hit / (self.hit + self.miss + self.false) if self.hit + self.miss + self.false else 0
        return (f"  {name:<44} detto {said:>4}  presi {self.hit:>4}  persi {self.miss:>4}  "
                f"falsi {self.false:>4} (di cui su aria limpida {self.gross:>3})  "
                f"POD {pod:4.0%}  FAR {far:4.0%}  CSI {csi:4.2f}")


# ------------------------------------------------------------------- la frase del giorno aperto

# The stations' own clocks, for the parts of the day (31 ott 2026): the archive is in GMT and a
# reader's afternoon is local. By the ICAO prefix, Longyearbyen first because it shares Norway's.
ZONES = [("ENSB", "Arctic/Longyearbyen"), ("LI", "Europe/Rome"), ("LS", "Europe/Zurich"),
         ("ED", "Europe/Berlin"), ("LO", "Europe/Vienna"), ("LF", "Europe/Paris"),
         ("EH", "Europe/Amsterdam"), ("EG", "Europe/London"), ("LE", "Europe/Madrid"),
         ("LG", "Europe/Athens"), ("EN", "Europe/Oslo"), ("ES", "Europe/Stockholm"),
         ("EF", "Europe/Helsinki"), ("BI", "Atlantic/Reykjavik"),
         # The airports outside Europe of `measure_hazards.py` (17.4, 19 nov 2026), by their
         # names in the Iowa Mesonet's archive: the day's possible storm is said by the part
         # of the place's own day.
         ("MIA", "America/New_York"), ("ATL", "America/New_York"), ("IAH", "America/Chicago"),
         ("ORD", "America/Chicago"), ("DEN", "America/Denver"), ("MMMX", "America/Mexico_City"),
         ("SKBO", "America/Bogota"), ("WSSS", "Asia/Singapore"), ("WMKK", "Asia/Kuala_Lumpur"),
         ("VTBS", "Asia/Bangkok"), ("VHHH", "Asia/Hong_Kong"), ("RJTT", "Asia/Tokyo"),
         ("VIDP", "Asia/Kolkata")]

# The parts of the day the sentence names: night, morning, afternoon, evening.
PARTS = ((0, 6), (6, 12), (12, 18), (18, 24))
WET_DAYS = range(0, 7)
COVERED = 0.75


def zone_of(icao):
    """The station's zone, None for an airport ZONES does not name. A missing tz database
    (Windows without the `tzdata` package) stops the run instead: caught as an unknown airport,
    it emptied the local-day tables of `--hazards` without a word (20 nov 2026, the review of
    PR #185)."""
    import zoneinfo
    for prefix, name in ZONES:
        if icao.startswith(prefix):
            try:
                return zoneinfo.ZoneInfo(name)
            except zoneinfo.ZoneInfoNotFoundError as e:
                raise SystemExit(f"{name}: no tz database here; pip install tzdata, or put it on PYTHONPATH") from e
    return None


def part_of(hour):
    return next(k for k, (a, b) in enumerate(PARTS) if a <= hour < b)


def wet_timing_report(data):
    """How far out «dalle 17:00 alle 20:00» is right (PLANNING, «La frase del giorno aperto»,
    31 ott 2026): per lead day, the share of `best_match`'s wet runs in which the METAR sees
    something fall inside the run's hours, against the same share inside the parts of the day
    the run touches. Day 0 is the archive, days 1-6 the previous runs (`--add-wet-runs`): the
    rows of the week from tomorrow to the seventh. Only days with one or two runs, the ones the
    sentence would say in hours; a window counts when the METAR covers 3/4 of its hours."""
    utc = dt.timezone.utc
    tally = collections.defaultdict(collections.Counter)
    asked = 0
    for st in data["stations"]:
        block = (st.get("previous_runs") or {}).get("hourly", {})
        if not st.get("wet_runs"):
            continue
        asked += 1
        zone = zone_of(st["icao"])
        if zone is None:
            raise KeyError(st["icao"])
        h = st["forecast"]["hourly"]
        times = [dt.datetime.strptime(t, "%Y-%m-%dT%H:%M") for t in h["time"]]
        local = [t.replace(tzinfo=utc).astimezone(zone) for t in times]
        obs = observations(st["metar"])
        seen = [wet_seen(obs, t) for t in times]
        prob = (st.get("probability") or {}).get("hourly", {})
        pop_by_time = dict(zip(prob.get("time", []), prob.get("precipitation_probability", [])))
        rows_by_day = collections.defaultdict(list)
        for i in range(len(times) - 1):
            rows_by_day[local[i].date()].append(i)

        def window(rows):
            known = [seen[r] for r in rows if 0 <= r < len(seen)]
            told = [s for s in known if s is not None]
            if not rows or len(told) < COVERED * len(rows):
                return None
            return any(told)

        for day in WET_DAYS:
            column = h.get("precipitation_best_match") if day == 0 else \
                block.get(f"precipitation_previous_day{day}_best_match")
            if not column:
                continue
            variants = [("", None)] + ([(" con il 20%", 20)] if day == 0 and pop_by_time else [])
            for suffix, floor in variants:
                key = (st["period"], day, suffix)
                for date, rows in rows_by_day.items():
                    if len(rows) < 23:
                        continue

                    def wet_row(i):
                        mm = column[i + 1] if i + 1 < len(column) else None
                        if mm is None or mm < MEASURABLE_MM - 1e-9:
                            return False
                        if floor is None:
                            return True
                        pop = pop_by_time.get(h["time"][i + 1])
                        return pop is None or pop >= floor

                    runs, current = [], []
                    for i in rows:
                        if wet_row(i):
                            current.append(i)
                        elif current:
                            runs.append(current)
                            current = []
                    if current:
                        runs.append(current)
                    if not runs:
                        continue
                    tally[key]["giorni bagnati"] += 1
                    if len(runs) > 2:
                        tally[key]["giorni con 3+ corse"] += 1
                        continue
                    for run in runs:
                        parts = {part_of(local[i].hour) for i in run}
                        in_parts = [i for i in rows if part_of(local[i].hour) in parts]
                        for name, rows_ in (("ore", run),
                                            ("ore ±1", list(range(run[0] - 1, run[-1] + 2))),
                                            ("parti", in_parts)):
                            verdict = window(rows_)
                            if verdict is None:
                                continue
                            tally[key][f"{name} n"] += 1
                            tally[key][f"{name} vere"] += verdict

    if not asked:
        print("FRASE DEL GIORNO: nessun aeroporto ha le corse dei giorni 1-6 (measure_against_metar.py --add-wet-runs)\n")
        return
    print(f"FRASE DEL GIORNO APERTO: le corse bagnate di best_match contro il METAR, {asked} aeroporti-periodo")
    print("  (giorno 0 l'archivio, 1-6 le corse precedenti: domani e' 1; solo giorni con una o due corse)")
    periods = sorted({k[0] for k in tally})
    verdicts = {}
    for period in periods + ["insieme"]:
        print(f"\n  {period}")
        print(f"    {'giorno':<16} {'g. bagnati':>10} {'3+ corse':>9} {'corse':>6}   "
              f"{'in ore':>7} {'ore ±1':>7} {'in parti':>8}")
        for day in WET_DAYS:
            for suffix in ("", " con il 20%"):
                keys = [k for k in tally if k[1] == day and k[2] == suffix and (period == "insieme" or k[0] == period)]
                if not keys:
                    continue
                c = sum((tally[k] for k in keys), collections.Counter())
                n = c["ore n"]
                if not n:
                    continue

                def share(name):
                    return c[f"{name} vere"] / c[f"{name} n"] if c[f"{name} n"] else float("nan")

                print(f"    {str(day) + suffix:<16} {c['giorni bagnati']:>10} {c['giorni con 3+ corse']:>9} "
                      f"{n:>6}   {share('ore'):>7.0%} {share('ore ±1'):>7.0%} {share('parti'):>8.0%}")
                if not suffix:
                    verdicts[(period, day)] = share("ore")
    hours_until = 0
    for day in range(1, 7):
        if all(verdicts.get((p, day), 0) >= 2 / 3 for p in periods + ["insieme"]):
            hours_until = day
        else:
            break
    if hours_until:
        print(f"\n  DECISIONE: le ore fino al giorno {hours_until}, le parti del giorno dal {hours_until + 1}")
    else:
        print("\n  DECISIONE: l'affermazione in ore non e' vera due volte su tre nemmeno domani: le parti del giorno ovunque")
    print()


# The Sky's verdict reads the cover (`SkyVerdictEngine.CLOUD_PASS_PCT`, `CLOUD_FAIL_PCT`).
VERDICT_PASS_PCT = 25
VERDICT_FAIL_PCT = 65
# Rule 5's confirmations as the engine had them from 14.3 (27 Oct 2026) to 17 Nov 2026: the own
# humidity, the witnesses' humidity, and how many witnesses see it with an own and with a lent
# visibility. Kept to re-run the measurements made on that rule.
ENGINE_OWN_RH = 95
ENGINE_WITNESS_RH = 97
ENGINE_WITNESSES_OWN = 1
ENGINE_WITNESSES_LENT = 2
# Rule 5 as the engine has it since 17 Nov 2026 (`WeatherStateEngine.fogVotes`, `fogOrSky`):
# two models see fog, each witness by its visibility under a kilometre, the own model by its
# humidity in a calm; kept by the code or persisting; under a mean humidity, without the
# highest and the lowest, of 93% or more. `best_match`'s visibility is not read.
VOTES_OWN_RH = 98
VOTES_CALM_KPH = 12.0
VOTES_NEEDED = 2
VOTES_MEAN_RH = 93.0
VOTES_WITNESSES = ("ecmwf_ifs", "ukmo_global_deterministic_10km",
                   "knmi_harmonie_arome_europe", "dmi_harmonie_arome_europe")


def trimmed_mean(xs):
    """The mean without the highest and the lowest, when there are three or more."""
    xs = sorted(xs)
    if len(xs) >= 3:
        xs = xs[1:-1]
    return sum(xs) / len(xs) if xs else None


def fog_votes(best, witnesses, i, own_rh=VOTES_OWN_RH, calm_kph=VOTES_CALM_KPH):
    """The models that see fog at hour i: each witness under a kilometre, and the own model
    with its air at [own_rh] or more in a wind of [calm_kph] or less."""
    if i < 0:
        return 0
    seen = sum(low(w, i, FOG_M) for w in witnesses)
    rh, wind = best("relative_humidity_2m", i), best("wind_speed_10m", i)
    return seen + (rh is not None and wind is not None and rh >= own_rh and wind <= calm_kph)


def votes_fog_now(best, witnesses_at, i, own_rh=VOTES_OWN_RH, calm_kph=VOTES_CALM_KPH,
                  mean_rh=VOTES_MEAN_RH, needed=VOTES_NEEDED, with_code=True):
    """Rule 5 since 17 Nov 2026. [witnesses_at] gives the hour's witnesses (not `best_match`)."""
    def seen(k):
        return fog_votes(best, witnesses_at(k), k, own_rh, calm_kph) >= needed
    if not seen(i):
        return False
    if not ((with_code and code_fog(best, i)) or seen(i - 1) or seen(i + 1)):
        return False
    rhs = [x for x in [best("relative_humidity_2m", i)] + [w("relative_humidity_2m", i) for w in witnesses_at(i)]
           if x is not None]
    mean = trimmed_mean(rhs)
    return mean is not None and mean >= mean_rh


def engine_fog_now(best, witnesses, i, own_visibility=True):
    """Rule 5 as `WeatherStateEngine.fogOrSky` and `fogConfirmed` have it since 14.3: the
    visibility the hour has (its own, else the lowest of the witnesses'), under 1 km, with the
    fog code or persisting on the series read; the own humidity at 95 where there is one; one
    witness that sees it when the visibility is the hour's own, two when it is lent.
    [own_visibility] false reads the hour as it will be where DWD drops the visibility."""
    own = best("visibility", i) if own_visibility else None
    lent = own is None
    if lent and not witnesses:
        return False
    series = MinSeries(witnesses) if lent else best
    if not low(series, i, FOG_M) or not (code_fog(best, i) or persists(series, i, FOG_M)):
        return False
    rh = best("relative_humidity_2m", i)
    if rh is not None and rh < ENGINE_OWN_RH:
        return False
    voters = [w for w in witnesses
              if w("visibility", i) is not None or w("relative_humidity_2m", i) is not None]
    if not voters:
        return not lent
    seeing = sum(low(w, i, FOG_M) or humid(w, i, ENGINE_WITNESS_RH) for w in voters)
    return seeing >= (ENGINE_WITNESSES_LENT if lent else ENGINE_WITNESSES_OWN)


def fog_cover_report(data):
    """The fog and the cover (PLANNING, «La misura: la nebbia contro la copertura», 16 nov
    2026): on the hours the engine calls fog, the cloud cover `best_match` gives, in the
    Sky's verdict's own buckets, and under 65% what the airport saw. Per hour, where the
    verdict reads a window's mean: an upper bound on how often a verdict could disagree."""
    names = ("fino al 25% (il verdetto direbbe «ottimo»)", "dal 26 al 65% («cosi' cosi'»)", "oltre il 65% («niente da fare»)")

    def bucket(cc):
        return 0 if cc <= VERDICT_PASS_PCT else 1 if cc <= VERDICT_FAIL_PCT else 2

    print("La nebbia e la copertura: sulle ore che il motore chiama nebbia, la copertura di best_match")
    for title, rule in (("la regola di oggi (17 nov 2026): due modelli che la vedono", "votes"),
                        ("la regola del 27 ott, con la visibilita' propria dove c'e'", "own"),
                        ("la regola del 27 ott, senza la visibilita' propria, prestata dai testimoni", "lent")):
        total = collections.Counter()
        night = collections.Counter()
        per_period = collections.OrderedDict()
        seen = collections.Counter()
        per_station = collections.defaultdict(collections.Counter)
        for st in data["stations"]:
            h = st["forecast"]["hourly"]
            times = [dt.datetime.strptime(t, "%Y-%m-%dT%H:%M") for t in h["time"]]
            obs = observations(st["metar"])
            best = Series(h, "best_match")
            models = {m: Series(h, m) for m in (IFS, UKMO, KNMI, DMI)}
            has = {m for m, s in models.items() if s.has("visibility") or s.has("relative_humidity_2m")}
            cache = {}

            def witnesses_at(k):
                if k not in cache:
                    cache[k] = [s for m, s in models.items() if m in has and not same_model(best, s, k)]
                return cache[k]

            for i in range(1, len(times) - 1):
                cc = best("cloud_cover", i)
                if cc is None or wet(best, i):
                    continue
                if rule == "votes":
                    fog = votes_fog_now(best, witnesses_at, i)
                else:
                    fog = engine_fog_now(best, witnesses_at(i), i, rule == "own")
                if not fog:
                    continue
                b = bucket(cc)
                total[b] += 1
                if cos_zenith(st["lat"], st["lon"], times[i]) < 0:
                    night[b] += 1
                per_period.setdefault(st["period"], collections.Counter())[b] += 1
                per_station[st["icao"]][b] += 1
                if b < 2:
                    seen[(b, observed_class(nearest(obs, times[i])))] += 1
        n = sum(total.values())
        print(f"\n  {title}: {n} ore di nebbia")
        if not n:
            continue
        nn = sum(night.values())
        for b, name in enumerate(names):
            print(f"    {name:<48} {total[b]:>5}  {100 * total[b] / n:5.1f}%   di notte {night[b]:>5} di {nn} ({100 * night[b] / max(1, nn):.1f}%)")
        for period, c in per_period.items():
            k = sum(c.values())
            print(f"    {period}: {k} ore, fino al 25% {c[0]} ({100 * c[0] / k:.1f}%), dal 26 al 65% {c[1]} ({100 * c[1] / k:.1f}%)")
        print("    sotto il 65%, cosa ha visto l'aeroporto:")
        words = {"fog": "nebbia", "mist": "foschia", "clear": "aria limpida (5 km o piu')", "precip": "qualcosa che cade", None: "nessun METAR"}
        for b in (0, 1):
            row = ", ".join(f"{words[k]} {v}" for (bb, k), v in sorted(seen.items(), key=lambda x: -x[1]) if bb == b)
            print(f"      {names[b]}: {row or 'nessuna ora'}")
        worst = sorted(per_station.items(), key=lambda kv: -(kv[1][0] + kv[1][1]))[:6]
        print("    gli aeroporti che ne hanno di piu' sotto il 65%: " + ", ".join(
            f"{icao} {c[0] + c[1]} di {sum(c.values())}" for icao, c in worst if c[0] + c[1]))


def fog_votes_report(data):
    """Rule 5 of 17 Nov 2026, two models (PLANNING, «La nebbia: due modelli», 17 nov 2026):
    against the rule it replaces, as it ran with ICON's visibility and as it would run from
    December without it; its variants; the plateau of its thresholds; the fourth to seventh
    day, when KNMI and DMI are gone; and how far the mean humidity it filters with is from the
    METAR's, against the own model's alone. `best_match`'s visibility is read by no row but
    the two references and the variant that says so."""
    split = collections.OrderedDict()
    err = collections.defaultdict(list)
    double = collections.Counter()

    def score(name):
        return split.setdefault(name, Split())

    grid = [(x, w, t) for x in (98, 99, 100) for w in (8.0, 12.0, 16.0) for t in (93.0, 94.0, 95.0)]
    for st in data["stations"]:
        h = st["forecast"]["hourly"]
        times = [dt.datetime.strptime(t, "%Y-%m-%dT%H:%M") for t in h["time"]]
        obs = observations(st["metar"])
        raw = []
        for r in st["metar"]:
            try:
                raw.append((parse_time(r["valid"]), float(r["tmpf"]), float(r["dwpf"])))
            except (TypeError, ValueError, KeyError):
                pass
        raw.sort()
        raw_t = [x[0] for x in raw]
        best = Series(h, "best_match")
        models = {m: Series(h, m) for m in VOTES_WITNESSES}
        has = {m for m, s in models.items() if s.has("visibility") or s.has("relative_humidity_2m")}
        group = "pianura padana" if st["icao"] in PO_VALLEY else "altrove"
        keys = [("tutto",), ("periodo", st["period"]), ("gruppo", group)]
        caches = {}

        def witnesses_of(which):
            cache = caches.setdefault(which, {})

            def at(k):
                if k not in cache:
                    cache[k] = [models[m] for m in which if m in has and not same_model(best, models[m], k)]
                return cache[k]
            return at

        everyone = witnesses_of(VOTES_WITNESSES)
        two_globals = witnesses_of((IFS, UKMO))

        def own_visibility(k):
            """`best_match`'s visibility where it is its own, not a witness's fill."""
            v = best("visibility", k)
            if v is None:
                return None
            for w in everyone(k):
                hours, same = 0, True
                for j in range(k - 6, k + 7):
                    a, b = w("visibility", j), best("visibility", j)
                    if a is None or b is None:
                        continue
                    if abs(a - b) >= 0.5:
                        same = False
                        break
                    hours += 1
                if same and hours >= 6:
                    return None
            return v

        def shipped_until_today(i):
            """The rule of 27 Oct as the app ran it until 17 Nov: the hour's own visibility where
            it is its own, else the witnesses' lowest; persistence on that series."""
            def vis_of(k):
                ov = own_visibility(k)
                if ov is not None:
                    return ov
                vs = [w("visibility", k) for w in everyone(k) if w("visibility", k) is not None]
                return min(vs) if vs else None
            lowk = lambda k: vis_of(k) is not None and vis_of(k) <= FOG_M
            if not (lowk(i) and (code_fog(best, i) or lowk(i - 1) or lowk(i + 1))):
                return False
            rh = best("relative_humidity_2m", i)
            if rh is not None and rh < ENGINE_OWN_RH:
                return False
            lent = own_visibility(i) is None
            voters = [w for w in everyone(i) if w("visibility", i) is not None or w("relative_humidity_2m", i) is not None]
            if not voters:
                return not lent
            seeing = sum(low(w, i, FOG_M) or humid(w, i, ENGINE_WITNESS_RH) for w in voters)
            return seeing >= (ENGINE_WITNESSES_LENT if lent else ENGINE_WITNESSES_OWN)

        def own_vis_rule(i):
            def votes(k):
                if k < 0:
                    return 0
                ov = own_visibility(k)
                own = (ov <= FOG_M) if ov is not None else fog_votes(best, [], k) == 1
                return sum(low(w, k, FOG_M) for w in everyone(k)) + own
            seen = lambda k: votes(k) >= VOTES_NEEDED
            if not (seen(i) and (code_fog(best, i) or seen(i - 1) or seen(i + 1))):
                return False
            m = trimmed_mean([x for x in [best("relative_humidity_2m", i)] + [w("relative_humidity_2m", i) for w in everyone(i)] if x is not None])
            return m is not None and m >= VOTES_MEAN_RH

        for i in range(1, len(times) - 1):
            # The mean humidity against the METAR's, on every hour with a report.
            j = bisect.bisect_left(raw_t, times[i])
            near = [raw[k] for k in (j - 1, j) if 0 <= k < len(raw) and abs((raw[k][0] - times[i]).total_seconds()) <= 1800]
            rb = best("relative_humidity_2m", i)
            rhs = [x for x in [rb] + [w("relative_humidity_2m", i) for w in everyone(i)] if x is not None]
            if near and rb is not None and len(rhs) >= 3:
                _, tf, df = min(near, key=lambda x: abs((x[0] - times[i]).total_seconds()))
                t_c, d_c = (tf - 32) * 5 / 9, (df - 32) * 5 / 9
                seen_rh = 100 * math.exp(17.62 * d_c / (243.12 + d_c)) / math.exp(17.62 * t_c / (243.12 + t_c))
                for key in ("tutto", group):
                    err[(key, "own")].append(abs(rb - seen_rh))
                    err[(key, "mean")].append(abs(trimmed_mean(rhs) - seen_rh))
                    if seen_rh >= 93:
                        err[(key, "own93")].append(abs(rb - seen_rh))
                        err[(key, "mean93")].append(abs(trimmed_mean(rhs) - seen_rh))

            o = nearest(obs, times[i])
            seen = observed_class(o)
            if seen is None or seen == "precip" or wet(best, i):
                continue
            is_fog = seen == "fog"
            clear_air = o["vis"] is not None and o["vis"] >= MIST_M

            def add(name, said):
                score(name).add(said, is_fog, clear_air, keys)

            add("la regola del 27 ott, come girava fino a oggi", shipped_until_today(i))
            add("la regola del 27 ott, da dicembre (senza ICON)", engine_fog_now(best, everyone(i), i, False))
            new = votes_fog_now(best, everyone, i)
            add("LA REGOLA DEL 17 NOV: due modelli", new)
            add("  il principale vota con la sua visibilita' dove ce l'ha", own_vis_rule(i))
            add("  senza il codice (solo la persistenza)", votes_fog_now(best, everyone, i, with_code=False))
            add("  senza il filtro della media", votes_fog_now(best, everyone, i, mean_rh=0.0))
            add("  99% / 12 km/h / 94% (la prima proposta)", votes_fog_now(best, everyone, i, 99, 12.0, 94.0))
            add("  giorni 4-7 (solo IFS e UKMO): la regola del 27 ott, senza", engine_fog_now(best, two_globals(i), i, False))
            add("  giorni 4-7 (solo IFS e UKMO): la regola del 17 nov", votes_fog_now(best, two_globals, i))
            for x, w, t in grid:
                add(f"griglia {x}/{int(w)}/{int(t)}", votes_fog_now(best, everyone, i, x, w, t))
            # The two votes that are one series twice: KNMI and DMI identical.
            if new:
                k_, d_ = models[KNMI], models[DMI]
                kv, dv = k_("visibility", i), d_("visibility", i)
                if (kv is not None and dv is not None and abs(kv - dv) < 0.5 and kv <= FOG_M
                        and k_("temperature_2m", i) == d_("temperature_2m", i)
                        and fog_votes(best, [w for w in everyone(i) if w is not d_], i) < VOTES_NEEDED):
                    double["dette"] += 1
                    double["giuste"] += is_fog

    def table(key, title):
        print(f"\n{title}")
        for name, sp in split.items():
            if name.startswith("griglia") or key not in sp.by:
                continue
            print(sp.by[key].row(name))

    print("LA NEBBIA: DUE MODELLI (17 nov 2026). La visibilita' di best_match non e' letta, salvo dove detto.")
    table(("tutto",), "Tutto")
    for key in sorted(k for k in next(iter(split.values())).by if k[0] == "periodo"):
        table(key, f"Periodo {key[1]}")
    for key in (("gruppo", "pianura padana"), ("gruppo", "altrove")):
        table(key, key[1].capitalize())

    print("\nLe soglie (umidita' del principale / vento / media senza estremi): CSI tutto, padana, altrove, per periodo")
    periods = sorted(k for k in next(iter(split.values())).by if k[0] == "periodo")
    for name, sp in split.items():
        if not name.startswith("griglia"):
            continue
        cells = []
        for key in [("tutto",), ("gruppo", "pianura padana"), ("gruppo", "altrove")] + periods:
            s = sp.by[key]
            cells.append(f"{s.hit / max(1, s.hit + s.miss + s.false):.3f}")
        print(f"  {name[8:]:<14}" + "  ".join(cells))

    print("\nL'umidita' contro il METAR, errore medio in punti: il principale da solo, la media senza estremi")
    for key in ("tutto", "pianura padana", "altrove"):
        f = lambda k: sum(err[(key, k)]) / max(1, len(err[(key, k)]))
        print(f"  {key:<15} tutte le ore: {f('own'):.2f} contro {f('mean'):.2f} ({len(err[(key, 'own')])} ore);"
              f" aria quasi satura (METAR >= 93%): {f('own93'):.2f} contro {f('mean93'):.2f} ({len(err[(key, 'own93')])} ore)")
    print(f"\nKNMI e DMI la stessa serie, unici due voti di una nebbia detta: {double['dette']} ore, {double['giuste']} giuste")


# --------------------------------------------- i pericoli del provider: temporale e gelicidio

# Rules 1 and 2 of the state engine relay the provider's code. They were the two hazard rules
# never scored against what was seen (17 nov 2026): the storm against the rain of ERA5 only,
# the freezing rain against nothing. The files are `tools/measure_hazards.py`'s, which carry
# the convective fields, and the `metar-*` archives, which do not.
THUNDER_CODES = (95, 96, 99)
FREEZING_CODES = (56, 57, 66, 67)
PROBABLE_PCT = 20
DAY_MIN_REPORTS = 12     # reports in a UTC day, or what the day saw is not known
CAPE_FLOORS = (100, 300, 500, 1000)
LIGHTNING_FLOORS = (1, 5, 20)

# The day's possible storm (Fase 17.4, 19 nov 2026; `PossibleStorm` in the app): on a day no
# hour of which has the provider's storm code, an hour with CAPE at this or more (the higher of
# its two ends), at least 0.1 mm and a chance of 20% or more, or none. 500 sits on the plateau:
# 300 scores the same per day and is right less often in Europe, 1000 scores lower.
STORM_CAPE_JKG = 500
CODE_ANY = "il codice, a qualunque probabilità"


def level_name(floor):
    return f"senza il codice: CAPE ≥ {floor}, ≥ 0,1 mm, ≥ 20%"


class Days:
    """Days said against days seen, for the storm by the day: a day is right when it saw a TS
    or a VCTS in any of its hours. The showers count hours in it, the same table.

    With the CSI, the Heidke skill score and the bias (19 nov 2026, after the external review of
    Fase 17): the CSI counts no correct «no», so where the event is frequent it rewards saying
    «yes» often, and saying it always scores the event's own frequency (the summer's showers at
    ICON-D2's airports: 0.47, more than the rule chosen). The HSS is the share of the hits beyond
    those chance would make with the same frequencies, 0 for «always» and for «never»; the bias
    is the said over the seen, 1 when the rule says the event as often as it happens."""

    def __init__(self):
        self.said = self.seen = self.hit = self.n = 0

    def add(self, said, seen):
        self.n += 1
        self.said += bool(said)
        self.seen += bool(seen)
        self.hit += bool(said and seen)

    @property
    def csi(self):
        miss = self.seen - self.hit
        return self.hit / (self.said + miss) if self.said + miss else 0

    @property
    def hss(self):
        """Heidke: (right − right by chance) / (all − right by chance), right counting the «no»s."""
        n = self.n
        if not n:
            return 0
        right = n - self.said - self.seen + 2 * self.hit
        chance = (self.said * self.seen + (n - self.said) * (n - self.seen)) / n
        return (right - chance) / (n - chance) if n != chance else 0

    @property
    def bias(self):
        return self.said / self.seen if self.seen else 0

    @property
    def always(self):
        """The CSI of saying the event every time: its frequency, the floor a CSI must clear."""
        return self.seen / self.n if self.n else 0

    def right(self):
        return f"{self.hit / self.said:4.0%}" if self.said else "   –"

    def taken(self):
        return f"{self.hit / self.seen:4.0%}" if self.seen else "   –"


class Hazard:
    """A hazard said for an hour against the reports of that hour and of the hours either
    side: a storm an hour early is not the false alarm a dry afternoon is."""

    def __init__(self):
        self.said = self.seen = self.hit = self.near = 0

    def add(self, said, seen, near):
        self.said += bool(said)
        self.seen += bool(seen)
        self.hit += bool(said and seen)
        self.near += bool(said and near)

    def row(self, name):
        miss = self.seen - self.hit
        csi = self.hit / (self.said + miss) if self.said + miss else 0
        share = (lambda n: f"{n / self.said:4.0%}") if self.said else (lambda n: "   –")
        return (f"  {name:<50} dette {self.said:>5}  giuste {self.hit:>4} ({share(self.hit)})  "
                f"entro un'ora {self.near:>4} ({share(self.near)})  viste {self.seen:>4}  "
                f"perse {miss:>4}  CSI {csi:4.2f}")


def hazard_hours(metar, times):
    """What the airport reported in each hour that starts at times[i]: how many reports,
    thunder at the station, thunder in the vicinity, freezing rain or drizzle, anything
    falling, the lowest temperature."""
    reports = sorted(((parse_time(r["valid"]), r) for r in metar), key=lambda x: x[0])
    stamps = [t for t, _ in reports]
    out = []
    for start in times:
        lo = bisect.bisect_left(stamps, start)
        hi = bisect.bisect_left(stamps, start + dt.timedelta(hours=1))
        ts = vcts = fz = falling = sh = False
        coldest = None
        for _, r in reports[lo:hi]:
            wx = r.get("wxcodes") or ""
            tokens = [] if wx == "null" else wx.split()
            here = [t for t in tokens if not t.startswith("VC") and not t.startswith("RE")]
            ts |= any("TS" in t for t in here)
            sh |= any("SH" in t for t in here)
            vcts |= any(t.startswith("VC") and "TS" in t for t in tokens)
            fz |= any("FZRA" in t or "FZDZ" in t for t in here)
            falling |= any(p in t for t in here for p in PRECIP_TOKENS)
            temp_f = number(r.get("tmpf"))
            if temp_f is not None:
                temp_c = (temp_f - 32) * 5 / 9
                coldest = temp_c if coldest is None else min(coldest, temp_c)
        out.append({"n": hi - lo, "ts": ts, "vcts": vcts, "fz": fz, "falling": falling, "cold": coldest,
                    "sh": sh})
    return out


def hazards_report(data):
    """Thunderstorm (rule 1) and freezing rain (rule 2) against the METARs, hour by hour and,
    for the storm, day by day; with the variants a different rule would be."""
    thunder = collections.defaultdict(Hazard)
    thunder_days = collections.defaultdict(Hazard)
    freezing = collections.defaultdict(Hazard)
    said_freezing = collections.Counter()
    seen_freezing = collections.Counter()
    per_station = collections.defaultdict(collections.Counter)
    order = {"temporale": [], "gelicidio": []}
    hours_scored = 0
    # The day's possible storm (17.4): the code alone, the code or the CAPE level where it is
    # silent, and the level alone on the days it is silent; by UTC day as the table above, and
    # by the place's own day with its parts, as the app says it.
    code_days = collections.defaultdict(Days)
    union_days = collections.defaultdict(Days)
    extra_days = collections.defaultdict(Days)
    local_days_score = collections.defaultdict(Days)
    part_tally = collections.defaultdict(collections.Counter)
    local_names = ("il codice", "il codice, o dove tace il livello", "il livello, dove il codice tace")

    def variant(kind, name):
        if name not in order[kind]:
            order[kind].append(name)
        return name

    for st in data["stations"]:
        h = st["forecast"]["hourly"]
        times = [dt.datetime.strptime(t, "%Y-%m-%dT%H:%M") for t in h["time"]]
        best = Series(h, "best_match")
        prob = (st.get("probability") or {}).get("hourly", {})
        pop_by_time = dict(zip(prob.get("time", []), prob.get("precipitation_probability") or []))
        truth = hazard_hours(st["metar"], times)
        # Europe by the ICAO prefix (E, L, and Iceland's B); the Mesonet's three-letter names
        # of the United States and every other prefix are the world beyond it.
        europe = len(st["icao"]) == 4 and st["icao"][0] in "ELB"
        place = ("pianura padana" if st["icao"] in PO_VALLEY
                 else "resto d'Italia" if st["icao"].startswith("LI")
                 else "resto d'Europa" if europe else "fuori dall'Europa")
        groups = ("tutti", place, st["period"])
        days = collections.defaultdict(lambda: {"n": 0, "seen": False, "said": collections.Counter()})
        has_cape = best.has("cape")
        zone = zone_of(st["icao"])
        local_days = collections.defaultdict(lambda: {"n": 0, "seen": set(), "coded": False, "level": set()})
        for i in range(1, len(times) - 2):
            now = truth[i]
            code = best("weather_code", i + 1)
            if not now["n"] or code is None:
                continue
            hours_scored += 1
            mm = best("precipitation", i + 1)
            pop = pop_by_time.get(h["time"][i + 1])
            probable = pop is None or pop >= PROBABLE_PCT
            wet = mm is not None and mm >= MEASURABLE_MM - 1e-9
            around = (truth[i - 1], now, truth[i + 1])

            # ---- the thunderstorm. The instants (CAPE, the lightning potential) are the
            # higher of the hour's two ends.
            def peak(var):
                ends = [v for v in (best(var, i), best(var, i + 1)) if v is not None]
                return max(ends) if ends else None

            cape, lightning = peak("cape"), peak("lightning_potential")
            coded = code in THUNDER_CODES
            rule = coded and probable
            said = [("il codice, a qualunque probabilità", coded),
                    ("regola 1: il codice e ≥ 20% (o ignota)", rule)]
            if pop is not None:
                said += [(f"il codice e ≥ {floor}%", coded and pop >= floor) for floor in (30, 50)]
                said.append(("regola 1 e almeno 0,1 mm", rule and wet))
                said.append(("regola 1 e niente di misurabile («possibili»)", rule and not wet))
            if cape is not None:
                said += [(f"regola 1 e CAPE ≥ {floor}", rule and cape >= floor) for floor in CAPE_FLOORS]
                said += [(f"senza il codice: CAPE ≥ {floor}, ≥ 0,1 mm, ≥ 20%",
                          cape >= floor and wet and probable) for floor in CAPE_FLOORS[1:]]
            if lightning is not None:
                said += [(f"regola 1 e potenziale di fulmini ≥ {floor}", rule and lightning >= floor)
                         for floor in LIGHTNING_FLOORS]
                said += [(f"senza il codice: potenziale di fulmini ≥ {floor}", lightning >= floor)
                         for floor in LIGHTNING_FLOORS]
                said += [(f"regola 1 o potenziale di fulmini ≥ {floor}", rule or lightning >= floor)
                         for floor in LIGHTNING_FLOORS[1:]]
            seen = now["ts"]
            near = any(x["ts"] or x["vcts"] for x in around)
            day = days[times[i].date()]
            day["n"] += now["n"]
            day["seen"] |= now["ts"] or now["vcts"]
            for name, yes in said:
                variant("temporale", name)
                for g in groups:
                    thunder[(g, name)].add(yes, seen, near)
                day["said"][name] += bool(yes)
            per_station[st["icao"]]["temporale detto"] += rule
            per_station[st["icao"]]["temporale giusto entro un'ora"] += rule and near
            per_station[st["icao"]]["temporale visto"] += seen
            if zone is not None and has_cape:
                # The hour on the place's clock, by the part of the day it starts in, as the
                # app's row is (`PossibleStorm`, `DayPart`).
                at = times[i].replace(tzinfo=dt.timezone.utc).astimezone(zone)
                local_day = local_days[at.date()]
                part = part_of(at.hour)
                local_day["n"] += now["n"]
                local_day["coded"] |= coded
                if now["ts"] or now["vcts"]:
                    local_day["seen"].add(part)
                if cape is not None and cape >= STORM_CAPE_JKG and wet and probable:
                    local_day["level"].add(part)

            # ---- freezing rain. «Deduced» is the rule STATES §10 kept switched off until a
            # winter was measured: liquid the model measures at 0 °C or under.
            temp = best("temperature_2m", i)
            liquid = liquid_rate(best, i)
            icy = code in FREEZING_CODES
            deduced = liquid is not None and liquid >= MEASURABLE_MM - 1e-9 and temp is not None and temp <= 0.0
            said = [("regola 2: il codice, a qualunque probabilità", icy)]
            if pop is not None:
                said.append(("il codice e ≥ 20%", icy and pop >= PROBABLE_PCT))
                said.append(("il codice e almeno 0,1 mm", icy and wet))
                said.append(("dedotto e ≥ 20%", deduced and pop >= PROBABLE_PCT))
            said.append(("dedotto: liquido ≥ 0,1 mm a 0 °C o meno", deduced))
            said.append(("il codice o il dedotto", icy or deduced))
            seen, near = now["fz"], any(x["fz"] for x in around)
            for name, yes in said:
                variant("gelicidio", name)
                for g in groups:
                    freezing[(g, name)].add(yes, seen, near)
            if icy:
                cold = now["cold"] is not None and now["cold"] <= 0
                what = ("gelicidio" if seen else "altro che cade" if now["falling"] else "niente che cade")
                said_freezing[f"{what}, {'a 0 °C o meno' if cold else 'sopra 0 °C'}"] += 1
            if seen:
                seen_freezing["gelicidio" if icy else "altra precipitazione" if code >= 51 else "asciutto"] += 1
            per_station[st["icao"]]["gelicidio detto"] += icy
            per_station[st["icao"]]["gelicidio giusto entro un'ora"] += icy and near
            per_station[st["icao"]]["gelicidio visto"] += seen
        for day in days.values():
            if day["n"] < DAY_MIN_REPORTS:
                continue
            for name in order["temporale"]:
                for g in groups:
                    thunder_days[(g, name)].add(day["said"][name] > 0, day["seen"], day["seen"])
            if not has_cape:
                continue
            coded_day = day["said"][CODE_ANY] > 0
            for g in groups:
                code_days[g].add(coded_day, day["seen"])
            for floor in CAPE_FLOORS[1:]:
                level = day["said"][level_name(floor)] > 0
                for g in groups:
                    union_days[(g, floor)].add(coded_day or level, day["seen"])
                    if not coded_day:
                        extra_days[(g, floor)].add(level, day["seen"])
        for local_day in local_days.values():
            if local_day["n"] < DAY_MIN_REPORTS:
                continue
            seen = bool(local_day["seen"])
            level = bool(local_day["level"]) and not local_day["coded"]
            for g in groups:
                local_days_score[(g, local_names[0])].add(local_day["coded"], seen)
                local_days_score[(g, local_names[1])].add(local_day["coded"] or level, seen)
                if not local_day["coded"]:
                    local_days_score[(g, local_names[2])].add(level, seen)
            if not level:
                continue
            named = local_day["level"]
            beside = named | {p + d for p in named for d in (-1, 1) if 0 <= p + d < len(PARTS)}
            for g in groups:
                t = part_tally[g]
                t["detti"] += 1
                t["giusti nel giorno"] += seen
                t["nella parte detta"] += bool(named & local_day["seen"])
                t["nella parte detta o accanto"] += bool(beside & local_day["seen"])
                t["il pomeriggio, detto sempre"] += 2 in local_day["seen"]
                t[f"parti dette: {len(named)}"] += 1

    periods = sorted({st["period"] for st in data["stations"]})
    groups = ["tutti", "pianura padana", "resto d'Italia", "resto d'Europa", "fuori dall'Europa"] + periods
    print(f"PERICOLI contro i METAR, {len(data['stations'])} aeroporti-periodo, {hours_scored} ore con un METAR")
    print("\nTEMPORALE, ora per ora (giusta: TS alla stazione nell'ora; entro un'ora: TS o VCTS "
          "nell'ora, in quella prima o in quella dopo)")
    for g in groups:
        rows = [thunder[(g, name)] for name in order["temporale"]]
        if not any(r.said or r.seen for r in rows):
            continue
        print(f" {g}")
        for name, r in zip(order["temporale"], rows):
            if r.said:
                print(r.row(name))
    print("\nGIORNI DI TEMPORALE (un giorno UTC con almeno un'ora detta, contro un giorno con almeno "
          f"un TS o VCTS; almeno {DAY_MIN_REPORTS} METAR nel giorno)")
    for g in groups:
        rows = [thunder_days[(g, name)] for name in order["temporale"]]
        if not any(r.said or r.seen for r in rows):
            continue
        print(f" {g}")
        for name, r in zip(order["temporale"], rows):
            if r.said:
                miss = r.seen - r.hit
                print(f"  {name:<50} detti {r.said:>4}  giusti {r.hit:>4} ({r.hit / r.said:4.0%})  "
                      f"visti {r.seen:>4}  persi {miss:>4}  CSI {r.hit / (r.said + miss):4.2f}")
    if code_days:
        print(f"\nIL TEMPORALE POSSIBILE DEL GIORNO (17.4): il codice, e nei giorni in cui nessuna ora "
              f"ha il codice un'ora con CAPE ≥ {STORM_CAPE_JKG} J/kg (il più alto dei due estremi), "
              "≥ 0,1 mm e ≥ 20% (o ignota). Giorni UTC come sopra, solo gli archivi con la CAPE")
        print(f"  {'zona (giorni con temporale, sui giorni)':<40}{'sempre':>7}  "
              f"{'il codice: detti, giusti, presi, CSI, HSS':<46}"
              f"{'il livello: in più, giusti':<30}insieme: giusti, presi, CSI, HSS, bias")
        for g in groups:
            c, x, u = code_days[g], extra_days[(g, STORM_CAPE_JKG)], union_days[(g, STORM_CAPE_JKG)]
            if not c.seen:
                continue
            print(f"  {f'{g} ({c.seen} su {c.n})':<40}{c.always:>7.2f}  "
                  f"{f'{c.said:>4}, {c.right()}, {c.taken()}, {c.csi:4.2f}, {c.hss:4.2f}':<46}"
                  f"{f'{x.said:>4}, {x.right()}':<30}{u.right()}, {u.taken()}, {u.csi:4.2f}, {u.hss:4.2f}, "
                  f"{u.bias:4.2f}")
        print("\n  la soglia della CAPE, insieme al codice: giusti, presi, CSI")
        for g in groups:
            if not code_days[g].seen:
                continue
            cells = "   ".join(f"CAPE ≥ {floor}: {union_days[(g, floor)].right()}, "
                               f"{union_days[(g, floor)].taken()}, {union_days[(g, floor)].csi:4.2f}"
                               for floor in CAPE_FLOORS[1:])
            print(f"  {g:<32}{cells}")
        print(f"\n  sul giorno del luogo (il suo orologio, come l'app), CAPE ≥ {STORM_CAPE_JKG}: "
              "detti, giusti, presi, CSI, HSS")
        for g in groups:
            rows = [local_days_score[(g, name)] for name in local_names]
            if not rows[0].seen:
                continue
            print(f"  {f'{g} ({rows[0].seen} su {rows[0].n}, sempre {rows[0].always:.2f})':<48}" + "   ".join(
                f"{name}: {r.said}, {r.right()}, {r.taken()}, {r.csi:4.2f}, HSS {r.hss:4.2f}"
                for name, r in zip(local_names, rows)))
        print("\n  la parte del giorno, sui giorni che il livello dice (il codice tace): quanto spesso il "
              "temporale (TS o VCTS) cade nella parte detta, sui giorni detti e su quelli giusti")
        for g in groups:
            t = part_tally[g]
            if not t["detti"]:
                continue
            n, ok = t["detti"], t["giusti nel giorno"]
            spread = ", ".join(f"{k} {t[f'parti dette: {k}']}" for k in range(1, len(PARTS) + 1))
            print(f"  {g:<32}detti {n:>4}  giusti nel giorno {ok / n:4.0%}  "
                  f"nella parte detta {t['nella parte detta'] / n:4.0%} "
                  f"({t['nella parte detta'] / ok if ok else 0:4.0%} dei giusti)  "
                  f"o accanto {t['nella parte detta o accanto'] / n:4.0%}  "
                  f"il pomeriggio, detto sempre {t['il pomeriggio, detto sempre'] / n:4.0%}  "
                  f"(parti dette: {spread})")
    print("\nGELICIDIO, ora per ora (FZRA o FZDZ alla stazione)")
    for g in groups:
        rows = [freezing[(g, name)] for name in order["gelicidio"]]
        if not any(r.said or r.seen for r in rows):
            continue
        print(f" {g}")
        for name, r in zip(order["gelicidio"], rows):
            if r.said or r.seen:
                print(r.row(name))
    print("\n  quando il codice dice gelicidio, l'aeroporto ha visto:", dict(said_freezing))
    print("  quando l'aeroporto ha visto gelicidio, il codice diceva:", dict(seen_freezing))
    print("\n  per aeroporto (dette dalla regola / giuste entro un'ora / viste):")
    for icao, c in sorted(per_station.items()):
        if any(c.values()):
            print(f"   {icao}  temporale {c['temporale detto']:>4} / {c['temporale giusto entro un\'ora']:>4} / "
                  f"{c['temporale visto']:>4}    gelicidio {c['gelicidio detto']:>3} / "
                  f"{c['gelicidio giusto entro un\'ora']:>3} / {c['gelicidio visto']:>3}")


# --------------------------------------------------------------------------- i rovesci

# «Rovesci» (Fase 17.6, 19 nov 2026): rule 3 calls a rain a shower when `showers`, the model's
# convective part, is at least half of the liquid. ICON-D2 resolves convection and its
# `showers` is almost empty, ICON-EU parametrizes it: the same event is «Pioggia» on days 1-2 in
# the north of Italy and «Rovesci» from the third. The candidates read the air (CAPE) and the
# shape of the episode, which do not change with the model serving the hour. The truth: SH or
# TS at the station in the hour, on the hours the engine calls rain and the airport saw wet.
# Europe only: the stations of the United States do not write SH.
SHOWER_CAPE_FLOORS = (50, 100, 200)
ICON_D2 = PO_VALLEY | {"LSZH", "EDDM", "LOWW"}


def showers_report(data):
    """Rain or showers, against the SH and TS of the METARs (PLANNING, «Fase 17», point 2)."""
    scores = collections.defaultdict(Days)
    summary = collections.defaultdict(Days)   # (slice, period, rule), the storm hours out
    order = []
    hours_by_group = collections.Counter()
    seen_by_group = collections.Counter()

    def score(group, name, said, seen):
        if name not in order:
            order.append(name)
        scores[(group, name)].add(said, seen)

    for st in data["stations"]:
        icao = st["icao"]
        europe = len(icao) == 4 and icao[0] in "ELB"
        icao_world = len(icao) == 4 and not europe   # the tropics and Asia write SH too
        if not (europe or icao_world):
            continue
        h = st["forecast"]["hourly"]
        times = [dt.datetime.strptime(t, "%Y-%m-%dT%H:%M") for t in h["time"]]
        best = Series(h, "best_match")
        if not best.has("showers"):
            continue
        has_cape = best.has("cape")
        prob = (st.get("probability") or {}).get("hourly", {})
        pop_by_time = dict(zip(prob.get("time", []), prob.get("precipitation_probability") or []))
        truth = hazard_hours(st["metar"], times)
        place = ("pianura padana" if icao in PO_VALLEY
                 else "resto d'Italia" if icao.startswith("LI")
                 else "resto d'Europa" if europe else "fuori dall'Europa (METAR ICAO)")
        groups = [place, st["period"], f"{place}, {st['period']}"]
        if europe:
            groups += ["Europa", f"Europa, {st['period']}"]
        if icao in ICON_D2:
            groups += ["aeroporti di ICON-D2", f"aeroporti di ICON-D2, {st['period']}"]

        # The model's episodes: runs of consecutive hours with a measurable amount, and the
        # same on the hours the engine calls wet (measurable and probable).
        n = len(times)
        mm_at = [best("precipitation", i + 1) for i in range(n)]
        pop_at = [pop_by_time.get(h["time"][i + 1]) if i + 1 < n else None for i in range(n)]
        measurable = [m is not None and m >= MEASURABLE_MM - 1e-9 for m in mm_at]
        probable = [p is None or p >= PROBABLE_PCT for p in pop_at]
        engine_wet = [a and b for a, b in zip(measurable, probable)]

        def run_lengths(flags):
            out = [0] * len(flags)
            i = 0
            while i < len(flags):
                if not flags[i]:
                    i += 1
                    continue
                j = i
                while j < len(flags) and flags[j]:
                    j += 1
                for k in range(i, j):
                    out[k] = j - i
                i = j
            return out

        run_model = run_lengths(measurable)
        run_engine = run_lengths(engine_wet)

        for i in range(1, n - 2):
            now = truth[i]
            code = best("weather_code", i + 1)
            if not now["n"] or code is None or not engine_wet[i]:
                continue
            mm = mm_at[i]
            snow_water = (best("snowfall", i + 1) or 0.0) / SNOW_CM_PER_MM
            liquid = max(0.0, mm - snow_water)
            # Rule 3's rain branch only: not both phases measurable, the liquid dominant.
            if snow_water >= MEASURABLE_MM - 1e-9 and liquid >= MEASURABLE_MM - 1e-9:
                continue
            if snow_water > liquid:
                continue
            if code in FREEZING_CODES:
                continue
            if not now["falling"]:
                continue
            coded = code in THUNDER_CODES
            seen = now["sh"] or now["ts"]
            showers = best("showers", i + 1)
            today = showers is not None and min(showers, liquid) >= liquid / 2
            ends = [v for v in (best("cape", i), best("cape", i + 1)) if v is not None]
            cape = max(ends) if ends else None
            start_cape = best("cape", i)
            short_model = 1 <= run_model[i] <= 2
            short_engine = 1 <= run_engine[i] <= 2
            said = [("oggi: `showers` ≥ metà del liquido", today),
                    ("la forma sola: episodio di 1-2 ore (quantità del modello)", short_model),
                    ("la forma sola: episodio di 1-2 ore (ore bagnate del motore)", short_engine)]
            if has_cape and cape is not None:
                for floor in SHOWER_CAPE_FLOORS:
                    said.append((f"CAPE ≥ {floor} (il più alto dei due estremi)", cape >= floor))
                said.append(("CAPE ≥ 100 all'inizio dell'ora", start_cape is not None and start_cape >= 100))
                for floor in SHOWER_CAPE_FLOORS:
                    said.append((f"CAPE ≥ {floor} o episodio di 1-2 ore (modello)", cape >= floor or short_model))
                said.append(("CAPE ≥ 100 o episodio di 1-2 ore (motore)", cape >= 100 or short_engine))
                said.append(("CAPE ≥ 100 o `showers` ≥ metà", cape >= 100 or today))
                # The shape only in air with some energy: a short winter drizzle in stable air
                # is not a shower (the Po valley's winter: no SH or TS in two months).
                for floor in (10, 25, 50):
                    said.append((f"CAPE ≥ 100 o episodio di 1-2 ore con CAPE ≥ {floor} (motore)",
                                 cape >= 100 or (short_engine and cape >= floor)))
            for name, yes in said:
                summary[(place, st["period"], name)].add(yes, seen) if not coded else None
                if europe and not coded:
                    summary[("Europa", st["period"], name)].add(yes, seen)
                if icao in ICON_D2 and not coded:
                    summary[("aeroporti di ICON-D2", st["period"], name)].add(yes, seen)
            for with_storms in (True, False):
                if not with_storms and coded:
                    continue
                tag = "con le ore di temporale" if with_storms else "senza le ore di temporale (regola 1)"
                for g in groups:
                    key = f"{g} — {tag}"
                    hours_by_group[key] += 1
                    seen_by_group[key] += seen
                    for name, yes in said:
                        score(key, name, yes, seen)

    print("ROVESCI contro i METAR (SH o TS alla stazione nell'ora), sulle ore che il motore chiama "
          "pioggia e che l'aeroporto ha visto bagnate")
    for key in sorted(hours_by_group, key=lambda k: (k.split(" — ")[1], k)):
        print(f"\n {key}: {hours_by_group[key]} ore, SH/TS in {seen_by_group[key]} "
              f"({seen_by_group[key] / hours_by_group[key]:.0%}: «Rovesci» detto sempre farebbe CSI "
              f"{seen_by_group[key] / hours_by_group[key]:.2f}, HSS 0)")
        for name in order:
            r = scores.get((key, name))
            if r is None or not r.said:
                continue
            print(f"  {name:<58} dette {r.said:>5}  giuste {r.right()}  prese {r.taken()}  CSI {r.csi:4.2f}"
                  f"  HSS {r.hss:5.2f}  bias {r.bias:4.2f}")

    # The decision's table (17.6): the rules side by side, slice by slice and season by season,
    # the storm-coded hours out (rule 1's): CSI and, in brackets, the hours said.
    rules = ["oggi: `showers` ≥ metà del liquido",
             "CAPE ≥ 100 o episodio di 1-2 ore (motore)",
             "CAPE ≥ 100 o episodio di 1-2 ore con CAPE ≥ 10 (motore)",
             "CAPE ≥ 100 o episodio di 1-2 ore con CAPE ≥ 25 (motore)",
             "CAPE ≥ 100 (il più alto dei due estremi)",
             "CAPE ≥ 100 o `showers` ≥ metà"]
    short = ["oggi", "CAPE100|ep", "…ep≥10", "…ep≥25", "CAPE100", "CAPE100|sh"]
    slices = ["pianura padana", "resto d'Italia", "resto d'Europa", "aeroporti di ICON-D2", "Europa",
              "fuori dall'Europa (METAR ICAO)"]
    periods = sorted({p for (_, p, _) in summary})
    print("\nLA TAVOLA DELLA DECISIONE (senza le ore di temporale): CSI / HSS (ore dette); "
          "«sempre» è il CSI di «Rovesci» detto su ogni pioggia, la frequenza (HSS 0)")
    print(f"  {'fetta, periodo (viste)':<64}{'sempre':>8}" + "".join(f"{s:>19}" for s in short))
    for sl in slices:
        for p in periods:
            cells = [summary.get((sl, p, r)) for r in rules]
            if not any(c and (c.said or c.seen) for c in cells):
                continue
            first = next(c for c in cells if c)
            print(f"  {f'{sl}, {p} ({first.seen})':<64}{first.always:>8.2f}" + "".join(
                f"{f'{c.csi:.2f} / {c.hss:.2f} ({c.said})' if c else '–':>19}" for c in cells))


# --------------------------------------------------------------------------- il velato

#: Le soglie di «Velato» com'e' nell'app (`WeatherStateEngine.VEILED_COVER_PCT` e
#: `VEILED_LIGHT_PCT`, 17 nov 2026): la copertura da cui il cielo non ha buchi, e la luce
#: tolta sotto cui la regola 6 dice ancora parole di sole.
VEILED_COVER_PCT = 80
VEILED_LIGHT_PCT = 50


def light_taken(cc, l, m, h):
    """`CloudLayers.lightTaken` com'e' nell'app: la luce degli strati ([opacity]), mai piu'
    della copertura; senza strati, o con strati tutti a zero, la copertura."""
    if cc is None:
        return None
    if l is None or m is None or h is None or (l + m + h) <= 0:
        return cc
    return min(opacity(l, m, h), cc)


def sky_word(cc, light, cover_pct=VEILED_COVER_PCT, light_pct=VEILED_LIGHT_PCT):
    """La parola della regola 6 com'e' nell'app dal 17 nov 2026, con le soglie del velato date."""
    taken = round(light)
    if cc >= cover_pct and taken < light_pct:
        return "velato"
    return ("sereno" if taken < 20 else "quasi sereno" if taken < 50
            else "nuvoloso" if taken < 80 else "coperto")


def veiled_report(data):
    """«Velato», la misura ripetibile (17 nov 2026, PLANNING «Velato: il passo, pronto da
    eseguire»): sulle ore di luce asciutte con il satellite, il sole sopra 15° e niente nebbia
    detta (la regola 5 com'e' nel codice) o vista, il cielo del motore contro quello del
    satellite. Per ogni gruppo di ore: quante sono, quante volte il sole dominava (indice di
    serenita' da 0,7), c'era un po' (da 0,4) o non c'era, e l'indice mediano. A parte la
    pianura padana; per confronto le ore sotto il 50% di copertura e la soglia al 90%.

    Da rilanciare sull'inverno quando l'archivio del satellite lo avra': la misura di ottobre
    non dice cosa fa un velo con il sole basso di dicembre."""
    groups = collections.OrderedDict((name, collections.defaultdict(list)) for name in (
        "velato (copertura ≥ 80%, luce < 50%)",
        "  …di cui lo strato alto fa il cielo",
        "velato con la copertura ≥ 90%",
        "copertura ≥ 80%, «nuvoloso»",
        "copertura ≥ 80%, «coperto»",
        "per confronto, copertura < 50%",
    ))
    totals = collections.Counter()
    for st in data["stations"]:
        h = st["forecast"]["hourly"]
        times = [dt.datetime.strptime(t, "%Y-%m-%dT%H:%M") for t in h["time"]]
        obs = observations(st["metar"])
        sat = (st.get("satellite") or {}).get("hourly", {})
        sat_ghi = dict(zip(sat.get("time", []), sat.get("shortwave_radiation", [])))
        best = Series(h, "best_match")
        models = {m: Series(h, m) for m in (IFS, UKMO, KNMI, DMI)}
        has = {m for m, s in models.items() if s.has("visibility") or s.has("relative_humidity_2m")}
        cache = {}

        def witnesses_at(k):
            if k not in cache:
                cache[k] = [s for m, s in models.items() if m in has and not same_model(best, s, k)]
            return cache[k]

        zone = "pianura padana" if st["icao"] in PO_VALLEY else "altrove"
        for i in range(1, len(times) - 1):
            k = clearness(st["lat"], st["lon"], times[i + 1], sat_ghi.get(h["time"][i + 1]))
            truth = seen_sky(k)
            seen = observed_class(nearest(obs, times[i]))
            if truth is None or wet(best, i) or seen in ("precip", "fog"):
                continue
            cc = best("cloud_cover", i)
            if cc is None or votes_fog_now(best, witnesses_at, i):
                continue
            l, m, hi_ = (best(v, i) for v in ("cloud_cover_low", "cloud_cover_mid", "cloud_cover_high"))
            light = light_taken(cc, l, m, hi_)
            word = sky_word(cc, light)
            totals["tutto"] += 1
            totals[zone] += 1
            names = []
            if word == "velato":
                names.append("velato (copertura ≥ 80%, luce < 50%)")
                if hi_ is not None and hi_ >= max(l or 0, m or 0):
                    names.append("  …di cui lo strato alto fa il cielo")
            if sky_word(cc, light, cover_pct=90) == "velato":
                names.append("velato con la copertura ≥ 90%")
            if cc >= VEILED_COVER_PCT and word in ("nuvoloso", "coperto"):
                names.append(f"copertura ≥ 80%, «{word}»")
            if cc < 50:
                names.append("per confronto, copertura < 50%")
            for name in names:
                groups[name]["tutto"].append((truth, k))
                groups[name][zone].append((truth, k))

    print("«Velato» contro il satellite: ore di luce con il sole sopra 15°, asciutte, senza "
          "nebbia detta o vista")
    print(f"  ore: {totals['tutto']} in tutto, {totals['pianura padana']} in pianura padana\n")
    print(f"  {'':<40}{'dove':<16}{'ore':>6}{'quota':>8}{'sole':>7}{'un po’':>8}{'niente':>8}{'indice':>8}")
    for name, by in groups.items():
        for where in ("tutto", "pianura padana"):
            rows = by[where]
            if not rows:
                continue
            n = len(rows)
            c = collections.Counter(t for t, _ in rows)
            ks = sorted(k for _, k in rows)
            print(f"  {name if where == 'tutto' else '':<40}{where:<16}{n:>6}{n / totals[where]:>8.1%}"
                  f"{c['sunny'] / n:>7.0%}{c['partly'] / n:>8.0%}{c['cloudy'] / n:>8.0%}{ks[n // 2]:>8.2f}")


# ------------------------------------------------- più modelli nei numeri e nel cielo (Fase 18)

# The models of the mean (PLANNING, Fase 18, decision 3): best_match and the two global
# witnesses the app already downloads, at equal weight; KNMI and DMI only in the printed
# five-model mean. A witness that is best_match around the hour (`same_model`, the app's own
# test) or that has no number is left out, and the mean is of what remains.
BLEND_MODELS = (IFS, UKMO)
BLEND_FIVE = (IFS, UKMO, KNMI, DMI)
BLEND_DAYS = range(1, 8)
# The weight of the mean, from the hours counted from the report's moment: zero up to the
# first number, in a straight line to one at the second, full after. A is the criterion's
# (PLANNING, «I criteri, scritti prima dell'estate»); B and C are printed beside it.
BLEND_RAMPS = (("rampa A (24 → 72 h)", 24, 72), ("rampa B (48 h, a gradino)", 48, 48),
               ("rampa C (48 → 120 h)", 48, 120))
BLEND_CHANGE_C = (1, 2)
MEAN_OF_EXTREMES = "media degli estremi propri"


def blend_weight(day, start_h, full_h):
    """The ramp's weight on lead day [day], at the centre of its hours (12 + 24·(N − 1))."""
    h = 12 + 24 * (day - 1)
    if full_h <= start_h:
        return 0.0 if h < start_h else 1.0
    return min(1.0, max(0.0, (h - start_h) / (full_h - start_h)))


def lead_model(st, day, model):
    """[model]'s series for the app's day [day]: the archive (the first hours of every run)
    for day 1, the witnesses' runs (`previous_day<N-1>`) after; None without the column."""
    if day == 1:
        s = Series(st["forecast"]["hourly"], model)
    else:
        block = st.get("previous_runs")
        if not block:
            return None
        s = run_series(block, day, model)
    return s if s.has("temperature_2m") or s.has("cloud_cover") else None


def model_mean(best, others, var, i, need=None):
    """The mean of [var] at hour [i] of best_match and of the [others] that have it and are
    not best_match around the hour; None without best_match's own number. [need]: the
    variables a model must have all of to count (the sky's four)."""
    need = need or (var,)
    if any(best(v, i) is None for v in need):
        return None
    xs = [best(var, i)]
    for w in others:
        if w is None or any(w(v, i) is None for v in need) or same_model(best, w, i):
            continue
        xs.append(w(var, i))
    return sum(xs) / len(xs)


class Mae:
    def __init__(self):
        self.err = 0.0
        self.n = 0

    def add(self, said, seen):
        self.err += abs(said - seen)
        self.n += 1

    @property
    def value(self):
        return self.err / self.n if self.n else None


def blend_group(icao):
    return "pianura padana" if icao in PO_VALLEY else "altrove"


def by_period(data):
    out = collections.OrderedDict()
    for st in data["stations"]:
        out.setdefault(st["period"], []).append(st)
    return out


def blend_report(data):
    """The temperature of the mean of three models (PLANNING, Fase 18, «Cosa ho trovato»,
    point 1, and the criterion of 18.2): per lead day 1-7, the hourly error against the METAR
    within 30 minutes of best_match, of each witness, of the mean and of the ramps; the day's
    high and low (the UTC day with at least 18 hours of METAR, the extremes of the hours, as
    14.10); how often a run changes the same hour by 1 and 2 °C against the next day's; by
    group, the Po valley and the rest. The verdict of the criterion is printed per period: it
    decides on the summer 2026 only."""
    for period, stations in by_period(data).items():
        mae = collections.defaultdict(Mae)          # (group, day, variant)
        ext = collections.defaultdict(Mae)          # (group, day, variant, "max"/"min")
        changes = collections.defaultdict(collections.Counter)   # (day, variant)
        variants = ["best_match", "IFS", "UKMO", "media a tre"] + [r[0] for r in BLEND_RAMPS] + [
            "media a cinque", "rampa A, a cinque"]
        for st in stations:
            h = st["forecast"]["hourly"]
            times = [dt.datetime.strptime(t, "%Y-%m-%dT%H:%M") for t in h["time"]]
            obs = observations(st["metar"])
            temps = [None] * len(times)
            for i, t in enumerate(times):
                o = nearest(obs, t)
                temps[i] = None if o is None else o["temp_c"]
            group = blend_group(st["icao"])
            said = {}                                # (day, variant) -> list of values per hour
            for day in BLEND_DAYS:
                best = lead_model(st, day, "best_match")
                if best is None or not best.has("temperature_2m"):
                    continue
                ws = {m: lead_model(st, day, m) for m in BLEND_FIVE}
                three = [ws[m] for m in BLEND_MODELS]
                five = [ws[m] for m in BLEND_FIVE]
                cols = {v: [None] * len(times) for v in variants}
                for i in range(len(times)):
                    b = best("temperature_2m", i)
                    if b is None or any(w is None or w("temperature_2m", i) is None for w in three):
                        continue
                    m3 = model_mean(best, three, "temperature_2m", i)
                    m5 = model_mean(best, five, "temperature_2m", i)
                    cols["best_match"][i] = b
                    cols["IFS"][i] = ws[IFS]("temperature_2m", i)
                    cols["UKMO"][i] = ws[UKMO]("temperature_2m", i)
                    cols["media a tre"][i] = m3
                    for name, start_h, full_h in BLEND_RAMPS:
                        cols[name][i] = b + blend_weight(day, start_h, full_h) * (m3 - b)
                    cols["media a cinque"][i] = m5
                    cols["rampa A, a cinque"][i] = b + blend_weight(day, 24, 72) * (m5 - b)
                for v in variants:
                    said[(day, v)] = cols[v]
                    for i, x in enumerate(cols[v]):
                        if x is not None and temps[i] is not None:
                            mae[(group, day, v)].add(x, temps[i])
                # The day's high and low, UTC days whole in the series.
                by_date = collections.defaultdict(list)
                for i, t in enumerate(times):
                    by_date[t.date()].append(i)
                for idx in by_date.values():
                    seen = [temps[i] for i in idx if temps[i] is not None]
                    if len(idx) != 24 or len(seen) < MIN_METAR_HOURS:
                        continue
                    for v in variants:
                        xs = [cols[v][i] for i in idx]
                        if any(x is None for x in xs):
                            continue
                        ext[(group, day, v, "max")].add(max(xs), max(seen))
                        ext[(group, day, v, "min")].add(min(xs), min(seen))
                    # Printed, not the criterion's: the mean of the models' own extremes, the
                    # figure «Cosa ho trovato» gave (computed there without the copy test).
                    if any(cols["best_match"][i] is None for i in idx):
                        continue
                    highs, lows = [], []
                    for s in [best] + three:
                        xs = [s("temperature_2m", i) for i in idx]
                        if any(x is None for x in xs) or (s is not best and day_copy(best, s, idx)):
                            continue
                        highs.append(max(xs))
                        lows.append(min(xs))
                    ext[(group, day, MEAN_OF_EXTREMES, "max")].add(sum(highs) / len(highs), max(seen))
                    ext[(group, day, MEAN_OF_EXTREMES, "min")].add(sum(lows) / len(lows), min(seen))
            for day in range(1, 7):
                for v in ("best_match", "media a tre", "rampa A (24 → 72 h)"):
                    a, b = said.get((day, v)), said.get((day + 1, v))
                    if a is None or b is None:
                        continue
                    for x, y in zip(a, b):
                        if x is None or y is None:
                            continue
                        c = changes[(day, v)]
                        c["n"] += 1
                        for limit in BLEND_CHANGE_C:
                            c[limit] += abs(x - y) >= limit - 1e-9

        groups = ("tutti", "pianura padana", "altrove")

        def pooled(table, group, days, v, *rest):
            m = Mae()
            for g in (("pianura padana", "altrove") if group == "tutti" else (group,)):
                for d in days:
                    cell = table.get((g, d, v) + rest)
                    if cell:
                        m.err += cell.err
                        m.n += cell.n
            return m

        print(f"\nTEMPERATURA, la media dei modelli contro il METAR: {period}")
        print("  errore medio assoluto dell'ora, °C, per giorno di anticipo (1: l'archivio; N: previous_day(N−1))")
        for group in groups:
            print(f"\n  {group}")
            print(f"    {'':<28}" + "".join(f"{d:>7}" for d in BLEND_DAYS))
            print(f"    {'ore':<28}" + "".join(f"{pooled(mae, group, [d], 'best_match').n:>7}" for d in BLEND_DAYS))
            for v in variants:
                cells = [pooled(mae, group, [d], v).value for d in BLEND_DAYS]
                print(f"    {v:<28}" + "".join("      —" if c is None else f"{c:7.2f}" for c in cells))
        print("\n  massima e minima del giorno (giorno UTC con almeno 18 ore di METAR), errore medio, °C: gli estremi"
              " delle ore della serie, il criterio; sotto, la media degli estremi propri dei modelli")
        for group in groups:
            for kind in ("max", "min"):
                print(f"    {group + ', ' + ('massima' if kind == 'max' else 'minima'):<28}"
                      + "".join(f"{d:>7}" for d in BLEND_DAYS) + f"{'giorni':>9}")
                for v in ("best_match", "media a tre", "rampa A (24 → 72 h)", MEAN_OF_EXTREMES):
                    cells = [pooled(ext, group, [d], v, kind).value for d in BLEND_DAYS]
                    n = pooled(ext, group, [1], v, kind).n
                    print(f"      {v:<26}" + "".join("      —" if c is None else f"{c:7.2f}" for c in cells)
                          + f"{n:>9}")
        print("\n  le corse che cambiano: ore in cui la stessa ora cambia di 1 o 2 °C o più fra il giorno N e il N+1")
        print(f"    {'':<28}" + "".join(f"{f'{d}→{d + 1}':>14}" for d in range(1, 7)))
        for v in ("best_match", "media a tre", "rampa A (24 → 72 h)"):
            cells = []
            for d in range(1, 7):
                c = changes[(d, v)]
                cells.append("—" if not c["n"] else
                             f"{c[1] / c['n']:.1%} {c[2] / c['n']:.1%}")
            print(f"    {v:<28}" + "".join(f"{x:>14}" for x in cells))

        # The criterion of 18.2, as written before the summer's numbers.
        ramp = "rampa A (24 → 72 h)"
        base, new = pooled(mae, "tutti", range(5, 8), "best_match"), pooled(mae, "tutti", range(5, 8), ramp)
        gain = 1 - new.value / base.value if base.n and new.n else None
        a = gain is not None and gain >= 0.05
        worst = []
        for group in ("pianura padana", "altrove"):
            for d in BLEND_DAYS:
                b, r = pooled(mae, group, [d], "best_match").value, pooled(mae, group, [d], ramp).value
                if b is not None and r is not None:
                    worst.append((r - b, group, d))
        worst.sort(reverse=True)
        b_ok = bool(worst) and worst[0][0] <= 0.02 + 1e-9
        c_cells = []
        for group in ("pianura padana", "altrove"):
            for kind in ("max", "min"):
                b = pooled(ext, group, range(4, 8), "best_match", kind).value
                r = pooled(ext, group, range(4, 8), ramp, kind).value
                c_cells.append((group, kind, b, r))
        c_ok = all(b is not None and r is not None and r <= b + 1e-9 for _, _, b, r in c_cells)
        print(f"\n  CRITERIO 18.2 su questo periodo (decide solo l'estate 2026), con la rampa A:")
        print(f"    (a) giorni 5–7: {base.value:.3f} → {new.value:.3f} °C, {gain:+.1%} di errore in meno "
              f"(serve ≥ 5%): {'sì' if a else 'no'}" if gain is not None else "    (a) senza ore")
        if worst:
            print(f"    (b) il giorno e il gruppo peggiore: {worst[0][1]}, giorno {worst[0][2]}, "
                  f"{worst[0][0]:+.3f} °C (al più +0,02): {'sì' if b_ok else 'no'}")
        print("    (c) massima e minima dei giorni 4–7: " + "; ".join(
            f"{g} {('massima' if k == 'max' else 'minima')} {b:.2f} → {r:.2f}" for g, k, b, r in c_cells
            if b is not None and r is not None) + f": {'sì' if c_ok else 'no'}")
        print("        stampata, non decide: la media degli estremi propri dei modelli, giorni 4–7: " + "; ".join(
            f"{g} {('massima' if k == 'max' else 'minima')} {pooled(ext, g, range(4, 8), 'best_match', k).value:.2f} → "
            f"{pooled(ext, g, range(4, 8), MEAN_OF_EXTREMES, k).value:.2f}"
            for g in ("pianura padana", "altrove") for k in ("max", "min")
            if pooled(ext, g, range(4, 8), MEAN_OF_EXTREMES, k).n))
        print(f"    → la media della temperatura {'ENTRA' if a and b_ok and c_ok else 'NON ENTRA'} su questo periodo")


def hss(confusion, classes=("sunny", "partly", "cloudy")):
    """Heidke's skill over the three skies: the right share against the share chance
    would get with the same proportions said and seen."""
    n = sum(confusion.values())
    if not n:
        return None
    right = sum(confusion[(c, c)] for c in classes) / n
    chance = sum(sum(confusion[(c, t)] for t in classes) * sum(confusion[(s, c)] for s in classes)
                 for c in classes) / n / n
    return (right - chance) / (1 - chance) if chance < 1 else None


def metar_okta(o_raw):
    """The cover a METAR reports, in okta, from its most covering layer: FEW 1-2, SCT 3-4,
    BKN 5-7, OVC 8, as the middle of each; NSC, NCD, CLR and SKC are none, a vertical
    visibility is a sky not seen, and a report without a layer says nothing."""
    covers = [(o_raw.get(f"skyc{k}") or "").strip() for k in range(1, 5)]
    covers = [c for c in covers if c and c not in ("null", "///", "M")]
    if not covers or "VV" in covers:
        return None
    okta = {"FEW": 1.5, "SCT": 3.5, "BKN": 6.0, "OVC": 8.0, "NSC": 0.0, "NCD": 0.0, "CLR": 0.0,
            "SKC": 0.0, "CAVOK": 0.0}
    xs = [okta[c] for c in covers if c in okta]
    return max(xs) if xs else None


def okta_class(okta):
    return ("0" if okta < 0.5 else "FEW" if okta < 2.5 else "SCT" if okta < 4.5
            else "BKN" if okta < 7.5 else "OVC")


SKY_MODELS = ["best_match", "icon_eu", "icon_d2", IFS, UKMO, KNMI, DMI]


def sky_blend_report(data):
    """The sky of the mean of three models (PLANNING, Fase 18, «Cosa ho trovato», point 2,
    and the criterion of 18.3): on the daylight hours of day 1 (the archive, which has the
    layers) with the sun above 15° and no precipitation or fog seen, the sky from the light
    the layers take (`opacity`) for each model and for the mean of best_match, IFS and UKMO,
    against the satellite (§11 of STATES); days 2-7 on the total cover alone; the cover
    against the METAR's okta by day and by night; the share of «Velato»."""
    names = SKY_MODELS + ["media a tre", "media a cinque"]
    for period, stations in by_period(data).items():
        conf = collections.defaultdict(collections.Counter)    # (group, name) -> (said, truth)
        lead_conf = collections.defaultdict(collections.Counter)  # (group, day, name)
        okta = collections.defaultdict(Mae)
        okta_hit = collections.defaultdict(collections.Counter)
        veiled = collections.defaultdict(collections.Counter)
        for st in stations:
            h = st["forecast"]["hourly"]
            times = [dt.datetime.strptime(t, "%Y-%m-%dT%H:%M") for t in h["time"]]
            obs = observations(st["metar"])
            raw = sorted(((parse_time(r["valid"]), r) for r in st["metar"]), key=lambda x: x[0])
            sat = (st.get("satellite") or {}).get("hourly", {})
            sat_ghi = dict(zip(sat.get("time", []), sat.get("shortwave_radiation", [])))
            group = blend_group(st["icao"])
            series = {m: Series(h, m) for m in SKY_MODELS}
            best = series["best_match"]
            three = [series[IFS], series[UKMO]]
            five = [series[m] for m in BLEND_FIVE]
            layers = ("cloud_cover", "cloud_cover_low", "cloud_cover_mid", "cloud_cover_high")

            def mean_sky(others, i):
                return [model_mean(best, others, v, i, need=layers) for v in layers]

            for i in range(1, len(times) - 1):
                k = clearness(st["lat"], st["lon"], times[i + 1], sat_ghi.get(h["time"][i + 1]))
                truth = seen_sky(k)
                o = nearest(obs, times[i])
                seen = observed_class(o)
                if seen in ("precip", "fog"):
                    continue
                m3 = mean_sky(three, i)
                # The cover against the METAR, day and night: the number of the tile.
                r = nearest(raw, times[i])
                ok = None if r is None else metar_okta(r)
                if ok is not None and m3[0] is not None:
                    when = "giorno" if cos_zenith(st["lat"], st["lon"], times[i]) > 0 else "notte"
                    for name, cc in (("best_match", best("cloud_cover", i)), ("media a tre", m3[0])):
                        okta[(when, name)].add(cc / 12.5, ok)
                        okta_hit[(when, name)]["n"] += 1
                        okta_hit[(when, name)]["giusto"] += okta_class(cc / 12.5) == okta_class(ok)
                if truth is None:
                    continue
                for name in SKY_MODELS:
                    s = series[name]
                    l, m, hi_ = (s(v, i) for v in layers[1:])
                    said = sky_bucket(opacity(l, m, hi_))
                    if said is not None:
                        conf[(group, name)][(said, truth)] += 1
                if None not in m3:
                    conf[(group, "media a tre")][(sky_bucket(opacity(*m3[1:])), truth)] += 1
                    for name, (cc, l, m, hi_) in (("best_match", [best(v, i) for v in layers]),
                                                  ("media a tre", m3)):
                        word = sky_word(cc, light_taken(cc, l, m, hi_))
                        veiled[(group, name)]["n"] += 1
                        if word == "velato":
                            veiled[(group, name)]["velato"] += 1
                            veiled[(group, name)][truth] += 1
                m5 = mean_sky(five, i)
                if None not in m5:
                    conf[(group, "media a cinque")][(sky_bucket(opacity(*m5[1:])), truth)] += 1
            # Days 1-7 on the total cover alone: the runs have no layers (14.4).
            for day in BLEND_DAYS:
                b = lead_model(st, day, "best_match")
                if b is None or not b.has("cloud_cover"):
                    continue
                ws = [lead_model(st, day, m) for m in BLEND_MODELS]
                for i in range(1, len(times) - 1):
                    k = clearness(st["lat"], st["lon"], times[i + 1], sat_ghi.get(h["time"][i + 1]))
                    truth = seen_sky(k)
                    if truth is None or observed_class(nearest(obs, times[i])) in ("precip", "fog"):
                        continue
                    if b("cloud_cover", i) is None or any(w is None or w("cloud_cover", i) is None for w in ws):
                        continue
                    lead_conf[(group, day, "best_match")][(sky_bucket(b("cloud_cover", i)), truth)] += 1
                    mc = model_mean(b, ws, "cloud_cover", i)
                    lead_conf[(group, day, "media a tre")][(sky_bucket(mc), truth)] += 1

        def merged(table, group, *key):
            out = collections.Counter()
            for g in (("pianura padana", "altrove") if group == "tutti" else (group,)):
                out.update(table.get((g,) + key, collections.Counter()))
            return out

        def line(c):
            n = sum(c.values())
            if not n:
                return "—"
            right = sum(c[(x, x)] for x in ("sunny", "partly", "cloudy")) / n
            said = [sum(c[(x, t)] for t in ("sunny", "partly", "cloudy")) / n for x in ("sunny", "partly", "cloudy")]
            seen = [sum(c[(s, x)] for s in ("sunny", "partly", "cloudy")) / n for x in ("sunny", "partly", "cloudy")]
            wrong_sun = c[("cloudy", "sunny")] / n
            return (f"ore {n:>6}  giuste {right:5.1%}  HSS {hss(c):5.3f}  detti {said[0]:3.0%}/{said[1]:3.0%}/{said[2]:3.0%}"
                    f"  visti {seen[0]:3.0%}/{seen[1]:3.0%}/{seen[2]:3.0%}  «niente sole» col sole {wrong_sun:5.1%}")

        print(f"\nCIELO, la media dei modelli contro il satellite: {period}")
        print("  giorno 1, ore di luce col sole sopra 15°, senza precipitazione né nebbia viste; "
              "il cielo dalla luce degli strati (opacity): sole / un po' / niente")
        for group in ("tutti", "pianura padana", "altrove"):
            print(f"\n  {group}")
            for name in names:
                print(f"    {SHORT.get(name, name):<16}{line(merged(conf, group, name))}")
        print("\n  giorni 1–7 sulla sola copertura totale (1: l'archivio; N: previous_day(N−1))")
        for group in ("tutti", "pianura padana", "altrove"):
            for name in ("best_match", "media a tre"):
                cells = []
                for day in BLEND_DAYS:
                    c = merged(lead_conf, group, day, name)
                    n = sum(c.values())
                    cells.append("—" if not n else
                                 f"{sum(c[(x, x)] for x in ('sunny', 'partly', 'cloudy')) / n:.1%} {hss(c):.2f}")
                print(f"    {group + ', ' + name:<30}" + "".join(f"{x:>13}" for x in cells))
        print("\n  la copertura contro le okta del METAR (lo strato più coperto; FEW 1–2, SCT 3–4, BKN 5–7, OVC 8)")
        for when in ("giorno", "notte"):
            for name in ("best_match", "media a tre"):
                m, c = okta[(when, name)], okta_hit[(when, name)]
                if m.n:
                    print(f"    {when:<8}{name:<14} ore {m.n:>6}  errore {m.value:4.2f} okta  "
                          f"classe giusta {c['giusto'] / c['n']:5.1%}")
        print("\n  «Velato» (copertura ≥ 80%, luce < 50%): quota delle ore e che cosa ha visto il satellite")
        for group in ("tutti", "pianura padana", "altrove"):
            for name in ("best_match", "media a tre"):
                c = merged(veiled, group, name)
                if c["n"]:
                    v = c["velato"]
                    seen = "" if not v else (f"  sole {c['sunny'] / v:3.0%} un po' {c['partly'] / v:3.0%} "
                                             f"niente {c['cloudy'] / v:3.0%}")
                    print(f"    {group + ', ' + name:<30} {v:>5} ore su {c['n']:>6} ({v / c['n']:4.1%}){seen}")

        # The criterion of 18.3, as written before the summer's numbers.
        def score(group, name):
            c = merged(conf, group, name)
            n = sum(c.values())
            return (sum(c[(x, x)] for x in ("sunny", "partly", "cloudy")) / n, hss(c), c[("cloudy", "sunny")], n) if n else None

        allb, allm = score("tutti", "best_match"), score("tutti", "media a tre")
        print("\n  CRITERIO 18.3 su questo periodo (decide solo l'estate 2026):")
        if not allb or not allm:
            print("    senza ore")
            continue
        a = allm[0] - allb[0] >= 0.05 - 1e-9 and allm[1] - allb[1] >= 0.05 - 1e-9
        print(f"    (a) ore giuste {allb[0]:.1%} → {allm[0]:.1%} (≥ +5 punti), HSS {allb[1]:.3f} → {allm[1]:.3f} "
              f"(≥ +0,05): {'sì' if a else 'no'}")
        b_ok = True
        parts = []
        for group in ("pianura padana", "altrove"):
            gb, gm = score(group, "best_match"), score(group, "media a tre")
            if gb and gm:
                ok = gm[0] >= gb[0] - 1e-9 and gm[1] >= gb[1] - 1e-9
                b_ok &= ok
                parts.append(f"{group} {gb[0]:.1%} → {gm[0]:.1%}, HSS {gb[1]:.3f} → {gm[1]:.3f}")
        print(f"    (b) {'; '.join(parts)}: {'sì' if b_ok else 'no'}")
        c_ok = allm[2] / allm[3] <= allb[2] / allb[3] + 1e-12
        print(f"    (c) «niente sole» col sole (k ≥ 0,7): {allb[2]} ore ({allb[2] / allb[3]:.1%}) → {allm[2]} "
              f"({allm[2] / allm[3]:.1%}): {'sì' if c_ok else 'no'}")
        print(f"    → la media del cielo {'ENTRA' if a and b_ok and c_ok else 'NON ENTRA'} su questo periodo")


# ------------------------------------------------- il pareggio delle parti del giorno (23 nov 2026)

TIE_PARTS = ((6, 12, "mattina"), (12, 18, "pomeriggio"))
TIE_ORDER = ("sun", "sun_and_cloud", "grey")   # dal più leggero al più pesante, come `OpenDaySentence.Look`
# I codici di cielo dal più leggero al più pesante, come `WmoCode.severity` (il velato fra quasi
# sereno e nuvoloso, STATES §3 regola 6).
TIE_SEVERITY = ("sereno", "quasi sereno", "velato", "nuvoloso", "coperto")


def tie_look(word):
    """Il «look» della frase del giorno (`OpenDaySentence.look`): sereno, quasi sereno e velato
    sono il sole, nuvoloso il sole e le nuvole, coperto il grigio."""
    return {"sereno": "sun", "quasi sereno": "sun", "velato": "sun", "nuvoloso": "sun_and_cloud",
            "coperto": "grey"}[word]


def seen_look(truth):
    return {"sunny": "sun", "partly": "sun_and_cloud", "cloudy": "grey"}[truth]


def sky_ties_report(data):
    """Il pareggio fra i cieli di una parte del giorno (PLANNING, «Le tre cose del 23 nov 2026»,
    il criterio scritto prima dei numeri): per aeroporto, giorno e parte (mattina 6–12,
    pomeriggio 12–18 locali), le ore di luce del giorno 1 con il sole sopra 15° e senza
    precipitazione né nebbia viste votano il loro cielo, com'è nell'app dal 23 nov 2026 (la
    media di `best_match`, IFS e UKMO, la luce presa, il velato); dove due cieli pareggiano, il
    cielo detto con il pareggio al più pesante (oggi) e al più leggero, contro il satellite. Per
    ogni regola: le ore delle parti in pareggio in cui il satellite vede il cielo detto, e le
    parti in cui il cielo detto è quello che il satellite vede più spesso."""
    for period, stations in by_period(data).items():
        tally = collections.defaultdict(collections.Counter)
        for st in stations:
            zone = zone_of(st["icao"])
            if zone is None:
                continue
            h = st["forecast"]["hourly"]
            times = [dt.datetime.strptime(t, "%Y-%m-%dT%H:%M") for t in h["time"]]
            obs = observations(st["metar"])
            sat = (st.get("satellite") or {}).get("hourly", {})
            sat_ghi = dict(zip(sat.get("time", []), sat.get("shortwave_radiation", [])))
            best = Series(h, "best_match")
            three = [Series(h, IFS), Series(h, UKMO)]
            layers = ("cloud_cover", "cloud_cover_low", "cloud_cover_mid", "cloud_cover_high")
            group = blend_group(st["icao"])
            parts = collections.defaultdict(list)
            days = collections.defaultdict(list)
            for i in range(1, len(times) - 1):
                k = clearness(st["lat"], st["lon"], times[i + 1], sat_ghi.get(h["time"][i + 1]))
                truth = seen_sky(k)
                if truth is None or observed_class(nearest(obs, times[i])) in ("precip", "fog"):
                    continue
                sky = [model_mean(best, three, v, i, need=layers) for v in layers]
                if None in sky:
                    continue
                cc, l, m, hi_ = sky
                word = sky_word(cc, light_taken(cc, l, m, hi_))
                said = tie_look(word)
                local = times[i].replace(tzinfo=dt.timezone.utc).astimezone(zone)
                for start, end, name in TIE_PARTS:
                    if start <= local.hour < end:
                        parts[(local.date(), name)].append((said, seen_look(truth)))
                days[local.date()].append((word, seen_look(truth)))
            # The day's label (`DayLabel`, step 3): the codes vote, a tie by severity.
            for votes in days.values():
                if len(votes) < 2:
                    continue
                count = collections.Counter(word for word, _ in votes)
                top = max(count.values())
                tied = sorted((w for w in count if count[w] == top), key=TIE_SEVERITY.index)
                seen = collections.Counter(truth for _, truth in votes)
                t = tally[("tutti", "giorno (etichetta)")]
                t["parti"] += 1
                if len(tied) < 2:
                    continue
                t["pareggi"] += 1
                t["ore"] += len(votes)
                seen_top = max(seen.values())
                for rule, word in (("pesante", tied[-1]), ("leggero", tied[0])):
                    look = tie_look(word)
                    t[rule + " ore"] += seen[look]
                    t[rule + " parti"] += seen[look] == seen_top
            for (_, name), votes in parts.items():
                if len(votes) < 2:
                    continue
                count = collections.Counter(said for said, _ in votes)
                top = max(count.values())
                tied = [look for look in TIE_ORDER if count[look] == top]
                seen = collections.Counter(truth for _, truth in votes)
                seen_top = max(seen.values())
                for key in (("tutti", "tutte"), (group, "tutte"), ("tutti", name)):
                    t = tally[key]
                    t["parti"] += 1
                    if len(tied) < 2:
                        continue
                    t["pareggi"] += 1
                    t["ore"] += len(votes)
                    for rule, look in (("pesante", tied[-1]), ("leggero", tied[0])):
                        t[rule + " ore"] += seen[look]
                        t[rule + " parti"] += seen[look] == seen_top
                        t[rule + " grigio col sole"] += look == "grey" and seen["sun"] == seen_top
                        t[rule + " sole col grigio"] += look == "sun" and seen["grey"] == seen_top
        print(f"\nIL PAREGGIO DELLE PARTI DEL GIORNO: {period}")
        print("  le parti (mattina, pomeriggio) del giorno 1 con almeno due ore di luce misurabili; "
              "il cielo della media dei tre modelli contro il satellite")
        print(f"  {'':<26}{'parti':>7}{'pareggi':>9}{'ore':>7}   {'al più pesante (oggi)':<34}{'al più leggero':<34}")
        for key in sorted(tally, key=lambda k: (k[0] != "tutti", k)):
            t = tally[key]
            if not t["pareggi"]:
                continue
            cells = []
            for rule in ("pesante", "leggero"):
                cells.append(f"ore {t[rule + ' ore'] / t['ore']:5.1%} parti {t[rule + ' parti'] / t['pareggi']:5.1%}")
            print(f"  {key[0] + ', ' + key[1]:<26}{t['parti']:>7}{t['pareggi']:>9}{t['ore']:>7}   {cells[0]:<34}{cells[1]:<34}")
        d = tally[("tutti", "giorno (etichetta)")]
        if d["pareggi"]:
            better = d["leggero ore"] > d["pesante ore"]
            print(f"  → l'etichetta del giorno: il pareggio al più leggero {'VINCE' if better else 'NON VINCE'} sulle ore "
                  f"({d['pesante ore'] / d['ore']:.1%} → {d['leggero ore'] / d['ore']:.1%}, {d['pareggi']} giorni in pareggio)")
        t = tally[("tutti", "tutte")]
        if t["pareggi"]:
            print(f"  «grigio» detto dove il satellite vede soprattutto sole: pesante {t['pesante grigio col sole']}, "
                  f"leggero {t['leggero grigio col sole']}; «sole» detto dove vede soprattutto grigio: "
                  f"pesante {t['pesante sole col grigio']}, leggero {t['leggero sole col grigio']}")
            better = t["leggero ore"] > t["pesante ore"]
            print(f"  → criterio, su questo periodo: il pareggio al più leggero {'VINCE' if better else 'NON VINCE'} sulle ore "
                  f"({t['pesante ore'] / t['ore']:.1%} → {t['leggero ore'] / t['ore']:.1%})")


def main(paths):
    if paths and paths[0] in ("--blend", "--sky-blend", "--sky-ties"):
        data = {"stations": []}
        for path in paths[1:]:
            with gzip.open(path, "rt", encoding="utf-8") as f:
                part = json.load(f)
            for st in part["stations"]:
                st["period"] = f"{part['start']} – {part['end']}"
            data["stations"] += part["stations"]
        {"--blend": blend_report, "--sky-blend": sky_blend_report, "--sky-ties": sky_ties_report}[paths[0]](data)
        return
    if paths and paths[0] == "--fog-votes":
        data = {"stations": []}
        for path in paths[1:]:
            with gzip.open(path, "rt", encoding="utf-8") as f:
                part = json.load(f)
            for st in part["stations"]:
                st["period"] = f"{part['start']} – {part['end']}"
            data["stations"] += part["stations"]
        fog_votes_report(data)
        return
    if paths and paths[0] == "--veiled":
        data = {"stations": []}
        for path in paths[1:]:
            with gzip.open(path, "rt", encoding="utf-8") as f:
                part = json.load(f)
            for st in part["stations"]:
                st["period"] = f"{part['start']} – {part['end']}"
            data["stations"] += part["stations"]
        veiled_report(data)
        return
    if paths and paths[0] == "--showers":
        data = {"stations": []}
        for path in paths[1:]:
            with gzip.open(path, "rt", encoding="utf-8") as f:
                part = json.load(f)
            for st in part["stations"]:
                st["period"] = f"{part['start']} – {part['end']}"
            data["stations"] += part["stations"]
        showers_report(data)
        return
    if paths and paths[0] == "--hazards":
        data = {"stations": []}
        for path in paths[1:]:
            with gzip.open(path, "rt", encoding="utf-8") as f:
                part = json.load(f)
            for st in part["stations"]:
                st["period"] = f"{part['start']} – {part['end']}"
            data["stations"] += part["stations"]
        hazards_report(data)
        return
    if paths and paths[0] == "--fog-cover":
        data = {"stations": []}
        for path in paths[1:]:
            with gzip.open(path, "rt", encoding="utf-8") as f:
                part = json.load(f)
            for st in part["stations"]:
                st["period"] = f"{part['start']} – {part['end']}"
            data["stations"] += part["stations"]
        fog_cover_report(data)
        return
    if paths and paths[0] == "--wet-timing":
        data = {"stations": []}
        for path in paths[1:]:
            with gzip.open(path, "rt", encoding="utf-8") as f:
                part = json.load(f)
            for st in part["stations"]:
                st["period"] = f"{part['start']} – {part['end']}"
            data["stations"] += part["stations"]
        wet_timing_report(data)
        return
    data = {"stations": []}
    for path in paths:
        with gzip.open(path, "rt", encoding="utf-8") as f:
            part = json.load(f)
        print(f"Misura {part['start']} – {part['end']}, {len(part['stations'])} aeroporti")
        for st in part["stations"]:
            st["period"] = f"{part['start']} – {part['end']}"
        data["stations"] += part["stations"]
    print()

    fog_variants = collections.OrderedDict()
    mist_variants = collections.OrderedDict()
    sky_variants = collections.OrderedDict()
    sky_confusion = collections.defaultdict(collections.Counter)
    phase_variants = collections.OrderedDict()
    duplicates = collections.Counter()
    hours_seen = collections.Counter()
    per_station_fog = collections.defaultdict(lambda: collections.Counter())

    def fog_score(name):
        return fog_variants.setdefault(name, Score())

    def mist_score(name):
        return mist_variants.setdefault(name, Score())

    for st in data["stations"]:
        h = st["forecast"]["hourly"]
        times = [dt.datetime.strptime(t, "%Y-%m-%dT%H:%M") for t in h["time"]]
        obs = observations(st["metar"])
        sat = (st.get("satellite") or {}).get("hourly", {})
        sat_ghi = dict(zip(sat.get("time", []), sat.get("shortwave_radiation", [])))
        best = Series(h, "best_match")
        witnesses_all = [Series(h, m) for m in WITNESSES]

        for i in range(1, len(times) - 1):
            o = nearest(obs, times[i])
            seen = observed_class(o)
            # Witnesses for this hour: the families that are not best_match itself.
            witnesses = []
            for w in witnesses_all:
                if same_model(best, w, i):
                    duplicates[w.m] += 1
                else:
                    witnesses.append(w)
            hours_seen["ore"] += 1

            if seen is not None and seen != "precip" and not wet(best, i):
                hours_seen["ore asciutte con METAR"] += 1
                is_fog = seen == "fog"
                clear_air = o["vis"] is not None and o["vis"] >= MIST_M
                per_station_fog[st["icao"]]["osservata"] += is_fog
                base = engine_fog(best, i)
                per_station_fog[st["icao"]]["motore"] += base
                fog_score("oggi (motore attuale)").add(base, is_fog, clear_air)
                fog_score("solo il codice del provider").add(code_fog(best, i) and low(best, i, FOG_M), is_fog, clear_air)
                for rh in (90, 93, 95, 97):
                    fog_score(f"oggi + umidità ≥ {rh}%").add(base and humid(best, i, rh), is_fog, clear_air)
                vis_w = [w for w in witnesses if w.has("visibility")]
                votes = sum(low(w, i, FOG_M) for w in vis_w)
                fog_score("oggi + almeno 1 testimone < 1 km").add(base and votes >= 1, is_fog, clear_air)
                fog_score("oggi + almeno 2 testimoni < 1 km").add(base and votes >= 2, is_fog, clear_air)
                share = (1 + votes) / (1 + len(vis_w)) if vis_w else 1
                fog_score("oggi + metà dei modelli < 1 km").add(base and share >= 0.5, is_fog, clear_air)
                for rh in (93, 95):
                    fog_score(f"oggi + 1 testimone + umidità ≥ {rh}%").add(
                        base and votes >= 1 and humid(best, i, rh), is_fog, clear_air)
                    fog_score(f"oggi + (1 testimone o umidità ≥ {rh}%)").add(
                        base and (votes >= 1 or humid(best, i, rh)), is_fog, clear_air)
                agree = sum(low(w, i, FOG_M) or humid(w, i, 97) for w in witnesses)
                fog_score("oggi + 1 testimone (< 1 km o umidità ≥ 97%)").add(base and agree >= 1, is_fog, clear_air)
                fog_score("oggi + 2 testimoni (< 1 km o umidità ≥ 97%)").add(base and agree >= 2, is_fog, clear_air)
                for rh in (93, 95):
                    fog_score(f"oggi + 1 testim. (< 1 km o um. ≥ 97%) + um. ≥ {rh}%").add(
                        base and agree >= 1 and humid(best, i, rh), is_fog, clear_air)
                for wrh, own, share, label in ((97, 97, 0.0, "uno"), (97, 97, 0.49, "metà"),
                                               (97, 97, 0.5, "maggioranza"), (95, 97, 0.5, "maggioranza"),
                                               (98, 97, 0.5, "maggioranza"), (97, 97, 0.99, "tutti")):
                    fog_score(f"REGOLA APP {label}, testim. um.≥{wrh}, da solo um.≥{own}").add(
                        base and confirmed(best, witnesses, i, wrh, own, share), is_fog, clear_air)
                for wrh, own in ((97, 95), (97, 93), (97, 97), (95, 95), (98, 95)):
                    fog_score(f"REGOLA APP uno + sempre um. propria ≥{own}, testim. ≥{wrh}").add(
                        base and confirmed(best, witnesses, i, wrh, own, 0.0, own_always=True), is_fog, clear_air)
                fog_score("REGOLA APP senza testimoni: um. propria ≥95").add(
                    base and confirmed(best, [], i, 97, 95, 0.0, own_always=True), is_fog, clear_air)
                fog_score("un testimone qualsiasi < 1 km (persistente)").add(
                    any(persists(w, i, FOG_M) for w in vis_w), is_fog, clear_air)

                # Mist: 1–5 km. Everything under 5 km that is not fog.
                is_mist = seen == "mist"
                said_mist_base = (not base) and persists(best, i, MIST_M)
                mist_score("best_match < 5 km, persistente").add(said_mist_base, is_mist, clear_air)
                for rh in (85, 90, 93):
                    mist_score(f"best_match < 5 km, persistente, umidità ≥ {rh}%").add(
                        said_mist_base and humid(best, i, rh), is_mist, clear_air)
                mvotes = sum(low(w, i, MIST_M) for w in vis_w)
                mist_score("best_match < 5 km, persistente, + 1 testimone").add(
                    said_mist_base and mvotes >= 1, is_mist, clear_air)
                mist_score("best_match < 5 km, persistente, + 1 testimone, um. ≥ 90%").add(
                    said_mist_base and mvotes >= 1 and humid(best, i, 90), is_mist, clear_air)

            # The sky, against the satellite, in dry hours with no fog said or seen.
            slot = h["time"][i + 1]
            k = clearness(st["lat"], st["lon"], times[i + 1], sat_ghi.get(slot))
            truth = seen_sky(k)
            if truth is not None and not wet(best, i) and seen not in ("precip", "fog"):
                l, m, hi_ = (best(v, i) for v in ("cloud_cover_low", "cloud_cover_mid", "cloud_cover_high"))
                variants = {
                    "totale (motore attuale)": best("cloud_cover", i),
                    "basse + medie": combined(l, m),
                    "basse + medie + alte × 0,5": combined(l, m, hi_, 0.5),
                    "basse + medie + alte × 0,3": combined(l, m, hi_, 0.3),
                    "max(basse, medie)": None if l is None or m is None else max(l, m),
                    "luce del disegno (1; 0,8; 0,3)": opacity(l, m, hi_),
                }
                for name, cc in variants.items():
                    said = sky_bucket(cc)
                    if said is None:
                        continue
                    c = sky_variants.setdefault(name, collections.Counter())
                    c["n"] += 1
                    c["giusto"] += said == truth
                    c["coperto, c'era il sole"] += said == "cloudy" and truth == "sunny"
                    c["sole, era coperto"] += said == "sunny" and truth == "cloudy"
                    sky_confusion[name][(said, truth)] += 1

            # The phase, where the METAR saw precipitation and the model had some.
            ph_obs = observed_phase(o)
            if ph_obs is not None:
                said = phase(best, i)
                if said is not None:
                    c = phase_variants.setdefault("best_match", collections.Counter())
                    c["n"] += 1
                    c["giusto"] += said == ph_obs
                    c[f"{said}→{ph_obs}"] += said != ph_obs
                    others = [phase(w, i) for w in witnesses]
                    others = [p for p in others if p is not None]
                    if others:
                        tally = collections.Counter([said] + others)
                        top, n_top = tally.most_common(1)[0]
                        voted = top if n_top * 2 > len(others) + 1 else said
                        c = phase_variants.setdefault("voto (maggioranza, altrimenti best_match)", collections.Counter())
                        c["n"] += 1
                        c["giusto"] += voted == ph_obs
                        c[f"{voted}→{ph_obs}"] += voted != ph_obs

    rain_report(data)
    fog_without_own_visibility(data)
    duplicate_report(data)
    icon_2i_report(data)
    agreement_report(data)
    wet_timing_report(data)
    print("\nOre:", dict(hours_seen))
    print("Testimoni scartati perché identici a best_match (ore):", dict(duplicates))

    print("\nNEBBIA (< 1 km) nelle ore asciutte con un METAR")
    for name, s in fog_variants.items():
        print(s.row(name))
    print("\n  per aeroporto (ore di nebbia osservate / dette dal motore attuale):")
    print("  " + "  ".join(f"{k} {v['osservata']}/{v['motore']}" for k, v in sorted(per_station_fog.items())))

    print("\nFOSCHIA (1–5 km) nelle ore asciutte con un METAR, dove il motore non dice nebbia")
    for name, s in mist_variants.items():
        print(s.row(name))

    print("\nCIELO contro il satellite (ore di luce con il sole sopra 15°, asciutte, senza nebbia)")
    for name, c in sky_variants.items():
        n = c["n"]
        print(f"  {name:<30} ore {n:>5}  giusto {c['giusto'] / n:4.0%}  "
              f"«coperto» col sole {c['coperto, c\'era il sole'] / n:4.1%}  "
              f"«sole» col coperto {c['sole, era coperto'] / n:4.1%}")
    for name in ("totale (motore attuale)", "basse + medie", "luce del disegno (1; 0,8; 0,3)"):
        conf = sky_confusion[name]
        print(f"\n  {name}: detto (righe) contro visto (colonne)")
        print("            " + "".join(f"{t:>9}" for t in ("sunny", "partly", "cloudy")))
        for said in ("sunny", "partly", "cloudy"):
            print(f"    {said:<8}" + "".join(f"{conf[(said, t)]:>9}" for t in ("sunny", "partly", "cloudy")))

    print("\nFASE nelle ore con precipitazione nel METAR e nel modello")
    for name, c in phase_variants.items():
        n = c["n"]
        errors = {k: v for k, v in c.items() if "→" in k and v}
        print(f"  {name:<44} ore {n:>4}  giusto {c['giusto'] / n:4.0%}  errori {errors}")


if __name__ == "__main__":
    main(sys.argv[1:])
