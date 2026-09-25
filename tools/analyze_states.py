#!/usr/bin/env python3
"""Analisi della misura per il motore degli stati (PLANNING.md, «Il motore degli stati»).

Legge un file di tools/measure_states.py e risponde, sulle ore come le mostra l'app, a:
quanto spesso codice, quantita' e probabilita' si contraddicono, e cosa cambierebbe ogni
regola del motore proposto rispetto a quello che l'app disegna oggi. Solo lettura, solo
libreria standard.

    python tools/analyze_states.py tools/measurements/states-2026-09-25.json

Le ore sono quelle dell'app (WeatherReportMapper.mapHourly): la riga i prende gli istanti
(temperatura, visibilita', nuvolosita') dal suo slot e i valori d'intervallo (quantita',
probabilita', il codice quando e' precipitazione) dallo slot i+1, che e' l'ora i..i+1.
"""

import collections
import json
import sys

SNOW_CM_PER_MM = 0.7
MEASURABLE_MM = 0.1
PROBABLE_PCT = 20
LIKELY_PCT = 60
FOG_M = 1000.0
WET = lambda c: c is not None and c >= 51
STORM = {95, 96, 99}
FREEZING = {56, 57, 66, 67}

WORD = {
    0: "sereno", 1: "quasi sereno", 2: "poco nuvoloso", 3: "coperto", 45: "nebbia", 48: "nebbia gelata",
    51: "pioviggine", 53: "pioviggine", 55: "pioviggine", 56: "pioviggine gelata", 57: "pioviggine gelata",
    61: "pioggia debole", 63: "pioggia", 65: "pioggia forte", 66: "pioggia gelata", 67: "pioggia gelata",
    68: "pioggia e neve", 69: "pioggia e neve", 71: "neve debole", 73: "neve", 75: "neve forte", 77: "neve",
    80: "rovesci", 81: "rovesci", 82: "rovesci forti", 85: "rovesci di neve", 86: "rovesci di neve",
    95: "temporale", 96: "temporale forte", 99: "temporale forte",
    "storm-dry": "temporali possibili", "likely-rain": "pioggia probabile", "likely-snow": "neve probabile",
}


def sky(cc):
    return 0 if cc < 20 else 1 if cc < 50 else 2 if cc < 80 else 3


def rows(h):
    n = len(h["time"])
    for i in range(n - 1):
        j = i + 1
        yield {
            "i": i, "time": h["time"][i],
            "code_now": h["weather_code"][i], "code": h["weather_code"][j],
            "mm": h["precipitation"][j], "rain": h["rain"][j], "showers": h["showers"][j],
            "snow": h["snowfall"][j], "pct": h["precipitation_probability"][j],
            "t": h["temperature_2m"][i], "t_end": h["temperature_2m"][j],
            "vis": h["visibility"][i], "cc": h["cloud_cover"][i],
            "vis_prev": h["visibility"][i - 1] if i > 0 else None, "vis_next": h["visibility"][j],
        }


def fog_persists(r):
    low = lambda v: v is not None and v <= FOG_M
    return low(r["vis"]) and (low(r["vis_prev"]) or low(r["vis_next"]))


def app_today(r):
    """What the app draws now: the next slot's code when it is precipitation, else the sky
    the hour starts with, with the fog repair's persistence rule (approximated)."""
    if WET(r["code"]):
        return r["code"]
    c = r["code_now"]
    if WET(c):
        return 45 if fog_persists(r) else sky(r["cc"])
    if c in (45, 48) and not (r["vis"] is not None and r["vis"] <= FOG_M):
        return sky(r["cc"])
    if c not in (45, 48) and fog_persists(r):
        return 45
    return c


def phase_of(r):
    sw = (r["snow"] or 0) / SNOW_CM_PER_MM
    liquid = max(0.0, (r["mm"] or 0) - sw)
    return sw, liquid


def engine(r, neighbours):
    """The proposed engine. Returns (state, rule)."""
    mm, pct, code, t = r["mm"], r["pct"], r["code"], r["t"]
    probable = pct is None or pct >= PROBABLE_PCT
    sw, liquid = phase_of(r)
    if code in STORM and probable:
        return (code if mm >= MEASURABLE_MM else "storm-dry"), "1 temporale"
    if code in FREEZING:
        return code, "2 gelicidio dal codice"
    if liquid >= MEASURABLE_MM and t is not None and t <= 0 and probable:
        return (56 if mm < 1.3 else 66), "2 gelicidio dedotto"
    if mm >= MEASURABLE_MM and probable:
        if liquid >= MEASURABLE_MM and sw >= MEASURABLE_MM:
            return (68 if mm < 2.5 else 69), "3 pioggia e neve"
        showers = r["showers"] or 0
        if sw > liquid:
            snow = r["snow"] or 0
            if showers >= sw / 2:
                return (86 if snow >= 0.8 else 85), "3 neve"
            return (71 if snow < 0.2 else 73 if snow < 0.8 else 75), "3 neve"
        if min(showers, liquid) >= liquid / 2:
            return (80 if liquid < 2.5 else 81 if liquid < 7.6 else 82), "3 rovesci"
        return (51 if liquid < 0.5 else 61 if liquid < 2.5 else 63 if liquid < 7.6 else 65), "3 pioggia"
    if mm < MEASURABLE_MM and pct is not None and pct >= LIKELY_PCT:
        snowy = None
        for k in (1, -1, 2, -2, 3, -3):
            nb = neighbours.get(r["i"] + k)
            if nb and (nb["mm"] or 0) >= MEASURABLE_MM:
                sw2, l2 = phase_of(nb)
                snowy = sw2 > l2
                break
        if snowy is None:
            snowy = t is not None and t <= 1.0
        return ("likely-snow" if snowy else "likely-rain"), "4 probabile"
    if fog_persists(r) or (r["code_now"] in (45, 48) and r["vis"] is not None and r["vis"] <= FOG_M):
        return (48 if t is not None and t <= 0 else 45), "5 nebbia"
    return sky(r["cc"]), "6 cielo"


def word(state):
    return WORD.get(state, str(state))


def main(path):
    data = json.load(open(path, encoding="utf-8"))
    total = 0
    c = collections.Counter()
    transitions = collections.Counter()
    examples = collections.defaultdict(list)
    by_rule = collections.Counter()
    changed_by_rule = collections.Counter()
    identity = collections.Counter()
    for city in data["cities"]:
        h = city["response"]["hourly"]
        rs = list(rows(h))
        nb = {r["i"]: r for r in rs}
        for r in rs:
            if r["mm"] is None or r["code"] is None:
                c["senza dati"] += 1
                continue
            total += 1
            wet_code, pct, mm = WET(r["code"]), r["pct"], r["mm"]
            if mm >= MEASURABLE_MM:
                c["ore con >= 0,1 mm"] += 1
            if wet_code:
                c["codice di precipitazione (slot dell'ora)"] += 1
                if pct is not None and pct < PROBABLE_PCT:
                    c["A codice di precipitazione, probabilita' < 20%"] += 1
                if pct == 0:
                    c["A0 codice di precipitazione, probabilita' 0%"] += 1
                if mm < MEASURABLE_MM:
                    c["A' codice di precipitazione con < 0,1 mm"] += 1
            elif mm >= MEASURABLE_MM:
                c["B codice di cielo o nebbia con >= 0,1 mm"] += 1
                examples["B"].append((city["city"], r["time"], mm, pct, r["code"]))
            if not wet_code and mm < MEASURABLE_MM and pct is not None:
                for p in (50, 60, 70):
                    if pct >= p:
                        c[f"C asciutta con probabilita' >= {p}%"] += 1
            # the identity: total = rain + showers + snow / 0.7, to the response's 0.1 mm
            if mm >= MEASURABLE_MM and None not in (r["rain"], r["showers"], r["snow"]):
                excess = (r["rain"] + r["showers"] + r["snow"] / SNOW_CM_PER_MM) - mm
                key = "vale" if abs(excess) <= 0.1 + 1e-9 else (
                    "rovesci contano la neve" if r["showers"] > 0 and r["snow"] > 0 and excess > 0 else "altro")
                identity[key] += 1

            before = app_today(r)
            after, rule = engine(r, nb)
            by_rule[rule] += 1
            if word(before) != word(after):
                changed_by_rule[rule] += 1
                transitions[(word(before), word(after))] += 1
                if len(examples[(word(before), word(after))]) < 3:
                    examples[(word(before), word(after))].append(
                        (city["city"], r["time"], mm, pct, r["code"], r["t"]))
            if rule == "2 gelicidio dedotto":
                examples["gelicidio dedotto"].append((city["city"], r["time"], mm, pct, r["code"], r["t"]))

    print(f"File: {path}\nScaricato: {data['fetched_at_utc']}, citta' {len(data['cities'])}, ore {total}\n")
    print("== Disaccordi fra codice, quantita' e probabilita'")
    for k in sorted(c):
        print(f"  {k:<48} {c[k]:>5}  ({100 * c[k] / total:.1f}%)")
    print("\n== L'identita' totale = pioggia + rovesci + neve/0,7 (ore con >= 0,1 mm)")
    for k, v in identity.most_common():
        print(f"  {k:<30} {v:>5}")
    print("\n== Il motore proposto: ore decise da ogni regola, e quante cambiano parola")
    for rule in sorted(by_rule):
        print(f"  {rule:<28} {by_rule[rule]:>5} ore, cambiano {changed_by_rule[rule]:>5}")
    print(f"  {'totale cambiate':<28} {sum(changed_by_rule.values()):>5} su {total} "
          f"({100 * sum(changed_by_rule.values()) / total:.1f}%)")
    print("\n== I passaggi (oggi -> motore), dal piu' frequente")
    for (a, b), v in transitions.most_common():
        ex = "; ".join(f"{e[0]} {e[1][5:13]} {e[2]} mm {e[3]}% cod {e[4]} {e[5]}°" for e in examples[(a, b)])
        print(f"  {v:>5}  {a:>18} -> {b:<20} es. {ex}")
    print("\n== Gelicidio dedotto (tutte le ore)")
    for e in examples["gelicidio dedotto"]:
        print(f"  {e[0]} {e[1]} {e[2]} mm {e[3]}% codice {e[4]} {e[5]}°C")


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else "tools/measurements/states-2026-09-25.json")
