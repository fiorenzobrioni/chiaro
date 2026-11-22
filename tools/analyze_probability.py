#!/usr/bin/env python3
"""La probabilita' della pioggia contro i METAR (PLANNING.md, «18.5 — La probabilita' della
pioggia»): se una calibrazione della probabilita' di `best_match` la rende piu' vera.

Le ore sono quelle di `rain_report` (analyze_against_metar.py, STATES §11): la riga i legge la
quantita' e la probabilita' dello slot i+1, l'ora i..i+1, ed e' bagnata se un METAR dentro
quell'ora riporta qualcosa che cade sulla stazione. Solo il giorno 1: la probabilita' di
`best_match` e' nell'archivio solo per le prime ore di ogni corsa.

Le varianti del criterio, scritto prima dei numeri:

- A la probabilita' di `best_match` com'e' (il riferimento);
- B la quota dei modelli bagnati (`best_match`, IFS, UKMO, KNMI, DMI presenti; >= 0,1 mm);
- C la probabilita' calibrata come IMPROVER: per fascia di dieci punti la frequenza osservata,
  resa monotona (pool adjacent violators), interpolata fra le fasce;
- D una regressione logistica (MOS) su logit(p), la quota, «>= 0,1 mm» e log(1 + mm);
- E la stessa senza p, per le ore senza probabilita' (si stampa, non decide).

Ogni aeroporto e' provato con una tabella allenata senza di lui e sull'altra meta' contigua del
periodo (e viceversa); poi, a parte, fra i periodi. Stampa Brier, Brier skill contro A, errore
d'affidabilita', il CSI della regola del motore al 20%, e il verdetto.

`--write-table` scrive i coefficienti di D che l'app applica (18.5: D ha passato il criterio nella
pianura padana soltanto), allenati su tutti gli aeroporti della zona e su tutti i periodi, in
`core/domain/.../correction/RainProbabilityTable.kt`; `--check` dice se il file e' indietro.

Usa numpy, come analyze_correction.py.

    python tools/analyze_probability.py tools/measurements/metar-*.json.gz
"""

import collections
import datetime as dt
import gzip
import json
import math
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from analyze_against_metar import (  # noqa: E402
    MEASURABLE_MM, Series, day_copy, observations, wet_seen,
)
from analyze_correction import ZONES, group, halves, period_name, zone_of  # noqa: E402

BEST = "best_match"
WITNESSES = ["ecmwf_ifs", "ukmo_global_deterministic_10km", "knmi_harmonie_arome_europe",
             "dmi_harmonie_arome_europe"]

FLOOR = 0.20            # la soglia del motore (STATES §2, PROBABLE_PCT)
MIN_SKILL = 0.05        # Brier skill contro A, in entrambi i periodi
CSI_DROP = 0.005        # quanto puo' scendere il CSI della regola del motore
STATION_WORSE = 1.05    # nessun aeroporto italiano con un Brier oltre il 5% sopra A
D_OVER_C = 0.02         # D vince su C solo con questo Brier skill in piu'
RIDGE = 1e-3

# Le fasce di C e dell'affidabilita': 0-4, 5-14, ..., 85-94, 95-100.
def band(p100):
    return 0 if p100 < 5 else min(10, int((p100 + 5) // 10))


def rows_of(data):
    period = period_name(data)
    out = []
    for st in data["stations"]:
        prob = (st.get("probability") or {}).get("hourly", {})
        pop_by_time = dict(zip(prob.get("time", []), prob.get("precipitation_probability", [])))
        if not pop_by_time:
            continue
        h = st["forecast"]["hourly"]
        times = [dt.datetime.strptime(t, "%Y-%m-%dT%H:%M") for t in h["time"]]
        obs = observations(st["metar"])
        best = Series(h, BEST)
        witnesses = {m: Series(h, m) for m in WITNESSES if Series(h, m).has("precipitation")}
        by_date = collections.defaultdict(list)
        for i, t in enumerate(times):
            by_date[t.date()].append(i)
        copies = {date: {m for m, w in witnesses.items() if day_copy(best, w, idx)} for date, idx in by_date.items()}
        for i in range(len(times) - 1):
            seen = wet_seen(obs, times[i])
            mm = best("precipitation", i + 1)
            pop = pop_by_time.get(h["time"][i + 1])
            if seen is None or mm is None or pop is None:
                continue
            wet = [mm >= MEASURABLE_MM - 1e-9]
            for m, w in witnesses.items():
                if m in copies[times[i + 1].date()]:
                    continue
                x = w("precipitation", i + 1)
                if x is not None:
                    wet.append(x >= MEASURABLE_MM - 1e-9)
            out.append({
                "period": period, "icao": st["icao"], "zone": zone_of(st["icao"]),
                "date": times[i + 1].date(), "seen": 1.0 if seen else 0.0,
                "p": pop / 100.0, "mm": mm, "m": 1.0 if wet[0] else 0.0,
                "f": sum(wet) / len(wet),
            })
    return out


# ---------------------------------------------------------------------------- le varianti

def pav(xs, ys, ws):
    """Pool adjacent violators: ys non decrescente, pesata per ws, sugli xs ordinati."""
    blocks = [[x, y, w] for x, y, w in zip(xs, ys, ws)]
    merged = []
    for b in blocks:
        merged.append([b[0] * b[2], b[1] * b[2], b[2], [b[0]]])
        while len(merged) > 1 and merged[-2][1] / merged[-2][2] > merged[-1][1] / merged[-1][2]:
            a = merged.pop()
            c = merged.pop()
            merged.append([c[0] + a[0], c[1] + a[1], c[2] + a[2], c[3] + a[3]])
    out = []
    for sx, sy, sw, members in merged:
        for x in members:
            out.append((x, sy / sw))
    return out


def fit_reliability(rows):
    """C: la frequenza osservata per fascia, monotona, sulla probabilita' media della fascia."""
    acc = collections.defaultdict(lambda: [0.0, 0.0, 0])
    for r in rows:
        b = band(r["p"] * 100)
        acc[b][0] += r["p"]
        acc[b][1] += r["seen"]
        acc[b][2] += 1
    bands = sorted(acc)
    xs = [acc[b][0] / acc[b][2] for b in bands]
    ys = [acc[b][1] / acc[b][2] for b in bands]
    ws = [acc[b][2] for b in bands]
    pts = pav(xs, ys, ws)
    return {"bands": bands, "x": [x for x, _ in pts], "y": [y for _, y in pts], "n": ws}


def apply_reliability(table, p):
    xs, ys = table["x"], table["y"]
    if p <= xs[0]:
        return ys[0]
    if p >= xs[-1]:
        return ys[-1]
    for k in range(1, len(xs)):
        if p <= xs[k]:
            t = (p - xs[k - 1]) / (xs[k] - xs[k - 1]) if xs[k] > xs[k - 1] else 1.0
            return ys[k - 1] + t * (ys[k] - ys[k - 1])
    return ys[-1]


def logit(p):
    p = min(max(p, 0.01), 0.99)
    return math.log(p / (1 - p))


def features(r, with_p):
    base = [1.0, r["f"], r["m"], math.log1p(r["mm"])]
    return [base[0], logit(r["p"])] + base[1:] if with_p else base


def fit_logistic(rows, with_p):
    x = np.array([features(r, with_p) for r in rows])
    y = np.array([r["seen"] for r in rows])
    beta = np.zeros(x.shape[1])
    for _ in range(50):
        z = np.clip(x @ beta, -30, 30)
        mu = 1 / (1 + np.exp(-z))
        w = mu * (1 - mu) + 1e-9
        hess = x.T @ (x * w[:, None]) + RIDGE * np.eye(x.shape[1])
        grad = x.T @ (y - mu) - RIDGE * beta
        step = np.linalg.solve(hess, grad)
        beta += step
        if np.max(np.abs(step)) < 1e-8:
            break
    return beta


def apply_logistic(beta, r, with_p):
    z = float(np.dot(beta, features(r, with_p)))
    return 1 / (1 + math.exp(-max(min(z, 30), -30)))


def fit(train):
    return {
        "C": fit_reliability(train),
        "D": fit_logistic(train, True),
        "E": fit_logistic(train, False),
    }


def predict(table, test):
    for r in test:
        r["pred"] = {
            "A": r["p"],
            "B": r["f"],
            "C": apply_reliability(table["C"], r["p"]),
            "D": apply_logistic(table["D"], r, True),
            "E": apply_logistic(table["E"], r, False),
        }


def holdout(rows):
    out = []
    for (period, zone), zr in group(rows, lambda r: (r["period"], r["zone"])).items():
        for icao in sorted({r["icao"] for r in zr}):
            for half in (0, 1):
                train = [r for r in zr if r["icao"] != icao and r["half"] != half]
                test = [dict(r) for r in zr if r["icao"] == icao and r["half"] == half]
                if train and test:
                    predict(fit(train), test)
                    out += test
    return out


def cross(rows):
    out = []
    for zone in ZONES:
        for p in sorted({r["period"] for r in rows}):
            train = [r for r in rows if r["zone"] == zone and r["period"] != p]
            test = [dict(r) for r in rows if r["zone"] == zone and r["period"] == p]
            if train and test:
                predict(fit(train), test)
                out += test
    return out


# ------------------------------------------------------------------------------ le misure

VARIANTS = ["A", "B", "C", "D", "E"]


def brier(rows, v):
    return sum((r["pred"][v] - r["seen"]) ** 2 for r in rows) / len(rows) if rows else None


def reliability_error(rows, v):
    acc = collections.defaultdict(lambda: [0.0, 0.0, 0])
    for r in rows:
        q = r["pred"][v]
        b = band(q * 100)
        acc[b][0] += q
        acc[b][1] += r["seen"]
        acc[b][2] += 1
    n = len(rows)
    return sum(abs(s / c - o / c) * c for s, o, c in acc.values()) / n if n else None


def engine_csi(rows, v):
    hit = miss = false = 0
    for r in rows:
        said = r["m"] == 1.0 and r["pred"][v] >= FLOOR - 1e-9
        seen = r["seen"] == 1.0
        hit += said and seen
        miss += (not said) and seen
        false += said and not seen
    return hit / (hit + miss + false) if hit + miss + false else None


def fmt(x, d=3):
    return "  —  " if x is None else f"{x:.{d}f}"


def report(tested, title, periods):
    print(f"\n## {title}\n")
    print("Brier (più basso è meglio), Brier skill contro A, errore d'affidabilità, CSI della regola "
          "del motore (>= 0,1 mm e probabilità >= 20%).\n")
    print("| Zona | Periodo | ore | bagnate | " + " | ".join(f"Brier {v}" for v in VARIANTS)
          + " | skill C | skill D | affid. A | affid. C | affid. D | CSI A | CSI C | CSI D |")
    print("|---|---|---:|---:|" + "---:|" * (len(VARIANTS) + 8))
    for zone in ZONES:
        for p in periods:
            cr = [r for r in tested if r["zone"] == zone and r["period"] == p]
            if not cr:
                continue
            bs = {v: brier(cr, v) for v in VARIANTS}
            sk = {v: 1 - bs[v] / bs["A"] for v in ("C", "D")}
            rel = {v: reliability_error(cr, v) for v in ("A", "C", "D")}
            csi = {v: engine_csi(cr, v) for v in ("A", "C", "D")}
            wet = sum(r["seen"] for r in cr)
            print(f"| {zone} | {p} | {len(cr)} | {wet:.0f} | " + " | ".join(fmt(bs[v]) for v in VARIANTS)
                  + f" | {fmt(sk['C'])} | {fmt(sk['D'])} | {fmt(rel['A'])} | {fmt(rel['C'])} | {fmt(rel['D'])}"
                  + f" | {fmt(csi['A'])} | {fmt(csi['C'])} | {fmt(csi['D'])} |")


def verdict(tested, periods):
    print("\n## Il verdetto del criterio (sulla prova senza l'aeroporto e senza i suoi giorni)\n")
    winners = {}
    for zone in ZONES[:2]:
        passed = {}
        for v in ("C", "D"):
            ok, skills, why = True, [], []
            for p in periods:
                cr = [r for r in tested if r["zone"] == zone and r["period"] == p]
                a, x = brier(cr, "A"), brier(cr, v)
                skill = 1 - x / a
                skills.append(skill)
                if skill < MIN_SKILL:
                    ok = False
                    why.append(f"{p}: skill {skill:.3f}")
                if reliability_error(cr, v) > reliability_error(cr, "A"):
                    ok = False
                    why.append(f"{p}: affidabilità peggiore")
                if engine_csi(cr, v) < engine_csi(cr, "A") - CSI_DROP:
                    ok = False
                    why.append(f"{p}: CSI {engine_csi(cr, v):.3f} contro {engine_csi(cr, 'A'):.3f}")
                for icao, sr in group(cr, lambda r: r["icao"]).items():
                    if brier(sr, v) > STATION_WORSE * brier(sr, "A"):
                        ok = False
                        why.append(f"{p}: {icao} peggiore")
            if ok:
                passed[v] = skills
            print(f"- {zone}, {v}: {'passa' if ok else 'non passa'} (skill " +
                  ", ".join(f"{s:.3f}" for s in skills) + ")" + ("" if ok else "; " + "; ".join(why)))
        chosen = None
        if "C" in passed:
            chosen = "C"
        if "D" in passed and (chosen is None or all(d >= c + D_OVER_C for d, c in zip(passed["D"], passed["C"]))):
            chosen = "D"
        winners[zone] = chosen
        print(f"  **{zone}: {chosen or 'nessuna'}**")
    return winners


def cross_check(crossed, winners, periods):
    print("\n## Fra i periodi (non decide: dice se la calibrazione è stagionale)\n")
    for zone, v in winners.items():
        if v is None:
            continue
        parts = []
        for p in periods:
            cr = [r for r in crossed if r["zone"] == zone and r["period"] == p]
            parts.append(f"{p}: Brier A {fmt(brier(cr, 'A'))}, {v} {fmt(brier(cr, v))}")
        print(f"- {zone}, {v}: " + "; ".join(parts))


def reliability_table(tested, periods):
    print("\n## L'affidabilità fascia per fascia, zone italiane, i due periodi insieme\n")
    print("| Fascia di A | ore | A media | bagnate | C media |")
    print("|---|---:|---:|---:|---:|")
    cr = [r for r in tested if r["zone"] in ZONES[:2]]
    acc = collections.defaultdict(lambda: [0, 0.0, 0.0, 0.0])
    for r in cr:
        b = band(r["p"] * 100)
        acc[b][0] += 1
        acc[b][1] += r["p"]
        acc[b][2] += r["seen"]
        acc[b][3] += r["pred"]["C"]
    for b in sorted(acc):
        n, sp, so, sc = acc[b]
        lo = 0 if b == 0 else b * 10 - 5
        hi = 4 if b == 0 else min(100, b * 10 + 4)
        print(f"| {lo}–{hi}% | {n} | {sp / n:.0%} | {so / n:.0%} | {sc / n:.0%} |")


# ------------------------------------------------------------------------------ la tabella

TABLE_PATH = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "core", "domain", "src", "main",
                          "kotlin", "com", "callbackdev", "chiaro", "domain", "correction", "RainProbabilityTable.kt")
# The zones where a variant passed the criterion (18.5, 22 nov 2026): D in the Po valley alone.
TABLE_ZONES = [(ZONES[0], "PO_VALLEY")]


def table_source(rows, paths):
    """D's coefficients, trained on every airport of each zone that passed and every period."""
    archives = ", ".join(os.path.basename(p) for p in sorted(paths))
    lines = [
        "package com.callbackdev.chiaro.domain.correction",
        "",
        "// Written by `python tools/analyze_probability.py --write-table` (PLANNING.md, «18.5»). Do not",
        "// edit by hand: re-running the tool is the change, and `--check` says when this file is behind.",
        "",
        "/**",
        " * The calibration of the probability of precipitation of the first day (Fase 18, 18.5): a",
        " * logistic regression of «something falls at the station in that hour» on the logit of",
        " * `best_match`'s probability (clipped to 1-99%), the share of the models with 0.1 mm or more",
        " * (`best_match` and the witnesses present), whether `best_match` has 0.1 mm or more, and",
        " * ln(1 + its amount). Only the zones where it passed the criterion.",
        " *",
        f" * Trained on: {archives}.",
        " */",
        "internal object RainProbabilityTable {",
        "    /** Intercept, logit(p), share of models wet, best_match wet, ln(1 + mm). */",
        "    val COEFFICIENTS: Map<BlendZone, List<Double>> = mapOf(",
    ]
    for k, (zone, name) in enumerate(TABLE_ZONES):
        beta = fit_logistic([r for r in rows if r["zone"] == zone], True)
        comma = "," if k < len(TABLE_ZONES) - 1 else ""
        lines.append(f"        BlendZone.{name} to listOf(" + ", ".join(f"{b:.4f}" for b in beta) + f"){comma}")
    lines += ["    )", "}", ""]
    return "\n".join(lines)


def main(argv):
    mode = None
    if argv and argv[0] in ("--write-table", "--check"):
        mode, argv = argv[0], argv[1:]
    rows, periods = [], []
    for path in argv:
        with gzip.open(path, "rt", encoding="utf-8") as f:
            data = json.load(f)
        pr = rows_of(data)
        halves(pr)
        rows += pr
        periods.append(period_name(data))
    if mode:
        source = table_source(rows, argv)
        path = os.path.normpath(TABLE_PATH)
        if mode == "--write-table":
            with open(path, "w", encoding="utf-8", newline="\n") as f:
                f.write(source)
            print(f"scritto {path}\n{source}")
            return
        current = open(path, encoding="utf-8").read() if os.path.exists(path) else None
        if current != source:
            sys.exit(f"{path} e' indietro: python tools/analyze_probability.py --write-table ...")
        print(f"{path}: aggiornato")
        return
    tested = holdout(rows)
    report(tested, "La prova senza l'aeroporto e senza i suoi giorni", periods)
    winners = verdict(tested, periods)
    crossed = cross(rows)
    report(crossed, "La prova fra i periodi", periods)
    cross_check(crossed, winners, periods)
    reliability_table(tested, periods)


if __name__ == "__main__":
    if len(sys.argv) < 2:
        sys.exit(__doc__)
    main(sys.argv[1:])
