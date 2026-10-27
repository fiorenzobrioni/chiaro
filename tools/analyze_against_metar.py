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
  cui la visibilita' di `best_match` e' quella di un testimone.

Le ore sono quelle dell'app (WeatherReportMapper.rows): la riga i prende gli istanti
(visibilita', nuvole, umidita') dallo slot i e i valori d'intervallo (quantita', neve) dallo
slot i+1, che e' l'ora i..i+1. Il METAR della riga e' quello piu' vicino all'istante i (entro
30 minuti); il satellite e' lo slot i+1, la media dell'ora i..i+1.

Limiti, detti subito: l'archivio delle previsioni e' fatto delle prime ore di ogni corsa (e'
la previsione del giorno 1, non del giorno 5); la probabilita' di precipitazione non c'e', e
la regola del 20% non si puo' rifare qui; un aeroporto non e' un centro citta'.

    python tools/analyze_against_metar.py tools/measurements/metar-*.json.gz   # uno o piu' periodi, sommati
"""

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
        precip = any(p in t for t in tokens for p in PRECIP_TOKENS)
        snow = any(("SN" in t or "SG" in t) for t in tokens)
        rain = any(("RA" in t or "DZ" in t) for t in tokens)
        fog_code = any("FG" in t and not t.startswith(("MI", "BC", "PR")) for t in tokens)
        out.append((parse_time(r["valid"]), {
            "vis": None if vis_mi is None else vis_mi * 1609.344,
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
         "icon_global": "ICON", "best_match": "best_match"}

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


def main(paths):
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
