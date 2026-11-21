#!/usr/bin/env python3
"""La misura di partenza della Fase 18 (PLANNING.md, «18.1 — La misura di partenza»): se una
correzione della temperatura dei modelli batte `best_match` com'e', contro i METAR.

Legge gli archivi di tools/measure_against_metar.py (blocco `previous_runs`: la temperatura a
2 m di `best_match`, IFS, UKMO per i giorni 1-7 dell'app, KNMI e DMI per i giorni 1-2) e prova,
ora per ora, le varianti del criterio scritto prima dei numeri:

- A `best_match` com'e' (il riferimento);
- B IFS e UKMO da soli (per sapere);
- C la media semplice dei modelli presenti (il controllo, non si sceglie);
- D `best_match` meno il suo errore medio per zona, giorno di anticipo e fascia di sei ore UTC;
- E la fusione, pesi per zona e giorno di anticipo a passi di 0,1, non negativi, somma 1,
  per il minimo MAE (un membro che manca in un'ora: i pesi degli altri rinormalizzati);
- F la fusione E, poi la correzione dell'errore medio come in D.

Ogni aeroporto e' provato con una tabella allenata senza di lui e senza i suoi giorni (gli
altri aeroporti della zona, l'altra meta' contigua del periodo, e viceversa); poi, a parte, fra
i periodi. Stampa il MAE per zona, gruppo di anticipo e periodo, il verdetto del criterio, e la
massima e la minima del giorno, che non decidono.

Usa numpy (la ricerca dei pesi a passi di 0,1 su cinque modelli, ~1 000 combinazioni per
tabella, in Python puro durerebbe decine di minuti): l'unico strumento del repo che lo chiede.

    python tools/analyze_correction.py tools/measurements/metar-*.json.gz
"""

import collections
import datetime as dt
import gzip
import itertools
import json
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from analyze_against_metar import (  # noqa: E402
    MIN_METAR_HOURS, Series, day_copy, nearest, observations, run_series,
)

BEST = "best_match"
IFS = "ecmwf_ifs"
UKMO = "ukmo_global_deterministic_10km"
KNMI = "knmi_harmonie_arome_europe"
DMI = "dmi_harmonie_arome_europe"
MODELS = [BEST, IFS, UKMO, KNMI, DMI]
WITNESSES = MODELS[1:]
LEADS = range(1, 8)

PO = {"LIML", "LIMC", "LIME", "LIMF", "LIPX", "LIPE", "LIPZ"}
SOUTH = {"LIRF", "LIRN", "LIBD", "LICJ", "LICC", "LIEE"}
ZONES = ["Pianura padana", "Centro-sud e isole", "Resto d'Europa"]
GROUPS = [("1-2", (1, 2)), ("3-5", (3, 4, 5)), ("6-7", (6, 7))]

MARGIN_C = 0.2          # sotto A in entrambi i periodi
STATION_WORSE_C = 0.1   # nessun aeroporto italiano peggio di cosi'
SIMPLER_C = 0.05        # una variante piu' complessa vince solo con questo in piu'
STEP = 0.1


def zone_of(icao):
    return ZONES[0] if icao in PO else ZONES[1] if icao in SOUTH else ZONES[2]


def period_name(data):
    return "inverno" if data["start"].startswith("2025-12") else "ottobre" if data["start"].startswith("2026-09") else data["start"]


def rows_of(data):
    """Un dict per (aeroporto, giorno di anticipo, ora) con il METAR e i modelli presenti."""
    period = period_name(data)
    out = []
    for st in data["stations"]:
        block = st.get("previous_runs")
        if not block or f"temperature_2m_{BEST}" not in block["hourly"]:
            continue
        times = [dt.datetime.strptime(t, "%Y-%m-%dT%H:%M") for t in block["hourly"]["time"]]
        obs = observations(st["metar"])
        seen = [nearest(obs, t) for t in times]
        seen = [None if o is None else o["temp_c"] for o in seen]
        by_date = collections.defaultdict(list)
        for i, t in enumerate(times):
            by_date[t.date()].append(i)
        for lead in LEADS:
            best = run_series(block, lead, BEST)
            series = {m: run_series(block, lead, m) for m in MODELS}
            for date, idx in by_date.items():
                present = {m for m in WITNESSES if series[m].has("temperature_2m") and not day_copy(best, series[m], idx)}
                for i in idx:
                    b = best("temperature_2m", i)
                    if b is None:
                        continue
                    vals = {BEST: b}
                    for m in present:
                        v = series[m]("temperature_2m", i)
                        if v is not None:
                            vals[m] = v
                    out.append({
                        "period": period, "icao": st["icao"], "zone": zone_of(st["icao"]),
                        "lead": lead, "date": date, "hour": times[i].hour, "block": times[i].hour // 6,
                        "obs": seen[i], "vals": vals,
                    })
    return out


def halves(rows):
    dates = sorted({r["date"] for r in rows})
    first = set(dates[: len(dates) // 2])
    for r in rows:
        r["half"] = 0 if r["date"] in first else 1


def matrix(rows, members):
    x = np.array([[r["vals"].get(m, np.nan) for m in members] for r in rows], dtype=float)
    mask = ~np.isnan(x)
    return np.nan_to_num(x), mask.astype(float)


_GRIDS = {}


def grid(n):
    """Ogni vettore di n pesi a passi di STEP, non negativi, somma 1."""
    if n not in _GRIDS:
        k = round(1 / STEP)
        ws = [c for c in itertools.product(range(k + 1), repeat=n) if sum(c) == k]
        _GRIDS[n] = np.array(ws, dtype=float) / k
    return _GRIDS[n]


def blend(x, mask, w):
    """Le previsioni della fusione con i pesi w (K x M) sulle righe x: (N x K)."""
    num = x @ w.T
    den = mask @ w.T
    best = x[:, [0]]
    return np.where(den > 1e-9, num / np.where(den > 1e-9, den, 1.0), best)


def fit_weights(rows, members):
    good = [r for r in rows if r["obs"] is not None]
    if not good:
        return None
    x, mask = matrix(good, members)
    y = np.array([r["obs"] for r in good])
    ws = grid(len(members))
    pred = blend(x, mask, ws)
    mae = np.abs(pred - y[:, None]).mean(axis=0)
    return ws[int(np.argmin(mae))]


def members_for(lead):
    return MODELS if lead <= 2 else [BEST, IFS, UKMO]


def fit(train):
    """La tabella: pesi per giorno di anticipo, errori medi per (anticipo, fascia) di A e di E."""
    table = {"w": {}, "biasA": {}, "biasE": {}}
    for lead in LEADS:
        rl = [r for r in train if r["lead"] == lead]
        if not rl:
            continue
        mem = members_for(lead)
        w = fit_weights(rl, mem)
        table["w"][lead] = w
        if w is None:
            continue
        x, mask = matrix(rl, mem)
        e = blend(x, mask, w[None, :])[:, 0]
        for b in range(4):
            sel = [k for k, r in enumerate(rl) if r["block"] == b and r["obs"] is not None]
            if sel:
                table["biasA"][(lead, b)] = float(np.mean([rl[k]["vals"][BEST] - rl[k]["obs"] for k in sel]))
                table["biasE"][(lead, b)] = float(np.mean([e[k] - rl[k]["obs"] for k in sel]))
    return table


def predict(table, test):
    """Le varianti per ogni riga di test, in r['pred']."""
    by_lead = collections.defaultdict(list)
    for r in test:
        by_lead[r["lead"]].append(r)
    for lead, rl in by_lead.items():
        mem = members_for(lead)
        w = table["w"].get(lead)
        x, mask = matrix(rl, mem)
        e = blend(x, mask, w[None, :])[:, 0] if w is not None else x[:, 0]
        for k, r in enumerate(rl):
            v = r["vals"]
            present = [v[m] for m in mem if m in v]
            p = r.setdefault("pred", {})
            p["A"] = v[BEST]
            p["IFS"] = v.get(IFS)
            p["UKMO"] = v.get(UKMO)
            p["C"] = sum(present) / len(present)
            p["D"] = v[BEST] - table["biasA"].get((lead, r["block"]), 0.0)
            p["E"] = float(e[k])
            p["F"] = float(e[k]) - table["biasE"].get((lead, r["block"]), 0.0)


def holdout(rows):
    """Ogni aeroporto, ogni meta': allenata sugli altri aeroporti della zona, sull'altra meta'."""
    out = []
    for (period, zone), zr in group(rows, lambda r: (r["period"], r["zone"])).items():
        for icao in sorted({r["icao"] for r in zr}):
            for half in (0, 1):
                train = [r for r in zr if r["icao"] != icao and r["half"] != half]
                test = [dict(r) for r in zr if r["icao"] == icao and r["half"] == half]
                if not train or not test:
                    continue
                predict(fit(train), test)
                out += test
    return out


def cross(rows):
    """Fra i periodi: allenata su tutta la zona in un periodo, provata sull'altro."""
    out = []
    periods = sorted({r["period"] for r in rows})
    for zone in ZONES:
        for p in periods:
            train = [r for r in rows if r["zone"] == zone and r["period"] != p]
            test = [dict(r) for r in rows if r["zone"] == zone and r["period"] == p]
            if train and test:
                predict(fit(train), test)
                out += test
    return out


def group(rows, key):
    g = collections.defaultdict(list)
    for r in rows:
        g[key(r)].append(r)
    return g


def mae(rows, variant):
    errs = [abs(r["pred"][variant] - r["obs"]) for r in rows if r["obs"] is not None and r["pred"].get(variant) is not None]
    return (sum(errs) / len(errs), len(errs)) if errs else (None, 0)


VARIANTS = ["A", "IFS", "UKMO", "C", "D", "E", "F"]
CANDIDATES = ["D", "E", "F"]


def cell_rows(rows, zone, leads, period):
    return [r for r in rows if r["zone"] == zone and r["lead"] in leads and r["period"] == period]


def fmt(x):
    return "   —  " if x is None else f"{x:6.2f}"


def report(tested, title, periods):
    print(f"\n## {title}\n")
    print("MAE della temperatura dell'ora (°C) contro il METAR; n = ore con un METAR.\n")
    print("| Zona | Anticipo | Periodo | n | " + " | ".join(VARIANTS) + " |")
    print("|---|---|---|---:|" + "---:|" * len(VARIANTS))
    for zone in ZONES:
        for gname, leads in GROUPS:
            for p in periods:
                cr = cell_rows(tested, zone, leads, p)
                vals = [mae(cr, v)[0] for v in VARIANTS]
                n = mae(cr, "A")[1]
                print(f"| {zone} | {gname} | {p} | {n} | " + " | ".join(fmt(v) for v in vals) + " |")


def verdict(tested, periods):
    print("\n## Il verdetto del criterio (sulla prova senza l'aeroporto e senza i suoi giorni)\n")
    winners = {}
    for zone in ZONES:
        for gname, leads in GROUPS:
            passed = {}
            for v in CANDIDATES:
                ok = True
                gains = []
                for p in periods:
                    cr = cell_rows(tested, zone, leads, p)
                    a, _ = mae(cr, "A")
                    x, _ = mae(cr, v)
                    if a is None or x is None or x > a - MARGIN_C:
                        ok = False
                    gains.append(None if a is None or x is None else a - x)
                    if zone != ZONES[2]:
                        for icao, sr in group(cr, lambda r: r["icao"]).items():
                            sa, _ = mae(sr, "A")
                            sx, _ = mae(sr, v)
                            if sa is not None and sx is not None and sx > sa + STATION_WORSE_C:
                                ok = False
                if ok:
                    passed[v] = gains
            chosen = None
            for v in CANDIDATES:
                if v not in passed:
                    continue
                if chosen is None:
                    chosen = v
                    continue
                if all(g is not None and c is not None and g >= c + SIMPLER_C for g, c in zip(passed[v], passed[chosen])):
                    chosen = v
            winners[(zone, gname)] = chosen
            detail = ", ".join(f"{v} −{min(g):.2f}" for v, g in passed.items()) or "nessuna"
            print(f"- **{zone}, giorni {gname}**: {'**' + chosen + '**' if chosen else 'nessuna passa'} (passano: {detail})")
    return winners


def cross_check(crossed, winners, periods):
    print("\n## Fra i periodi (non decide: dice se la correzione è stagionale)\n")
    for (zone, gname), v in winners.items():
        if v is None:
            continue
        leads = dict(GROUPS)[gname]
        parts = []
        for p in periods:
            cr = cell_rows(crossed, zone, leads, p)
            a, _ = mae(cr, "A")
            x, _ = mae(cr, v)
            parts.append(f"{p}: A {fmt(a).strip()}, {v} {fmt(x).strip()}")
        print(f"- {zone}, giorni {gname}, {v}: " + "; ".join(parts))


def extremes(tested, periods):
    print("\n## La massima e la minima del giorno (non decidono)\n")
    print("MAE (°C) della massima e della minima di 24 ore UTC, giorni con 24 previsioni e "
          f"almeno {MIN_METAR_HOURS} METAR.\n")
    print("| Zona | Anticipo | Periodo | giorni | max A | max D | max E | max F | min A | min D | min E | min F |")
    print("|---|---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|")
    days = group(tested, lambda r: (r["period"], r["zone"], r["icao"], r["lead"], r["date"]))
    acc = collections.defaultdict(list)
    for (p, zone, icao, lead, date), dr in days.items():
        if len(dr) != 24:
            continue
        obs = [r["obs"] for r in dr if r["obs"] is not None]
        if len(obs) < MIN_METAR_HOURS:
            continue
        g = next(n for n, ls in GROUPS if lead in ls)
        for v in ("A", "D", "E", "F"):
            pv = [r["pred"][v] for r in dr]
            acc[(zone, g, p, "max", v)].append(abs(max(pv) - max(obs)))
            acc[(zone, g, p, "min", v)].append(abs(min(pv) - min(obs)))
    for zone in ZONES:
        for gname, _ in GROUPS:
            for p in periods:
                n = len(acc[(zone, gname, p, "max", "A")])
                cells = [acc[(zone, gname, p, k, v)] for k in ("max", "min") for v in ("A", "D", "E", "F")]
                print(f"| {zone} | {gname} | {p} | {n} | " + " | ".join(fmt(sum(c) / len(c) if c else None) for c in cells) + " |")


def weights_report(rows, periods):
    print("\n## I pesi della fusione, allenata su tutta la zona e su tutto il periodo (per leggere)\n")
    for zone in ZONES:
        for p in periods:
            t = fit([r for r in rows if r["zone"] == zone and r["period"] == p])
            parts = []
            for lead in LEADS:
                w = t["w"].get(lead)
                if w is None:
                    continue
                mem = members_for(lead)
                parts.append(f"g{lead} " + "/".join(f"{x:.1f}" for x in w))
            print(f"- {zone}, {p} (best/IFS/UKMO[/KNMI/DMI]): " + ", ".join(parts))


def main(paths):
    rows = []
    periods = []
    for path in paths:
        with gzip.open(path, "rt", encoding="utf-8") as f:
            data = json.load(f)
        pr = rows_of(data)
        halves(pr)
        rows += pr
        periods.append(period_name(data))
    tested = holdout(rows)
    report(tested, "La prova senza l'aeroporto e senza i suoi giorni", periods)
    winners = verdict(tested, periods)
    crossed = cross(rows)
    report(crossed, "La prova fra i periodi", periods)
    cross_check(crossed, winners, periods)
    extremes(tested, periods)
    weights_report(rows, periods)


if __name__ == "__main__":
    if len(sys.argv) < 2:
        sys.exit(__doc__)
    main(sys.argv[1:])
