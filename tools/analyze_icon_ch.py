#!/usr/bin/env python3
"""ICON-CH contro i METAR in tre ruoli (PLANNING.md, «18.2 — ICON-CH: il criterio»).

Legge le corse di `meteoswiss_icon_seamless` per i giorni 1-5 che `measure_against_metar.py
--add-icon-ch` aggiunge agli archivi, e risponde alle tre domande del criterio, scritto prima
dei dati:

- R1 al posto di `best_match`, giorni 1-2 e 3-5: MAE della temperatura dell'ora e CSI dell'ora
  bagnata (>= 0,1 mm, senza probabilita'), il criterio del 14.4;
- R2 membro della fusione della temperatura (la E di 18.1 con ICON-CH fra i membri), giorni 1-5,
  ogni aeroporto provato con pesi allenati senza di lui e senza i suoi giorni;
- R3 testimone della probabilita' del primo giorno: la regressione D di 18.5 con ICON-CH nella
  quota dei modelli bagnati, contro D com'e'.

E stampa, senza decidere, la fase di ICON-CH contro i METAR (il dato di 18.7). Decidono gli
aeroporti della pianura padana nel dominio; gli altri si stampano.

Usa numpy, come gli altri due strumenti della Fase 18.

    python tools/analyze_icon_ch.py tools/measurements/metar-*.json.gz
"""

import collections
import datetime as dt
import gzip
import itertools
import json
import math
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from analyze_against_metar import (  # noqa: E402
    MEASURABLE_MM, SNOW_CM_PER_MM, Series, day_copy, nearest, observations, observed_phase, run_series, wet_seen,
)
from analyze_correction import PO, period_name  # noqa: E402
from analyze_probability import (  # noqa: E402
    apply_logistic, brier, engine_csi, fit_logistic, reliability_error,
)

BEST = "best_match"
CH = "meteoswiss_icon_seamless"
IFS = "ecmwf_ifs"
UKMO = "ukmo_global_deterministic_10km"
KNMI = "knmi_harmonie_arome_europe"
DMI = "dmi_harmonie_arome_europe"
WITNESSES = [IFS, UKMO, KNMI, DMI]
LEADS = range(1, 6)
GROUPS = [("1-2", (1, 2)), ("3-5", (3, 4, 5))]

R1_MAE_C = 0.2
R1_CSI = 0.02
R2_MAE_C = 0.2
R2_STATION_C = 0.1
R3_SKILL = 0.02
CSI_DROP = 0.005
STEP = 0.1

# Jennings et al. 2018, Fig. 2: the 50% rain-snow air temperature by relative humidity bin.
def jennings_c(rh):
    return 0.7 if rh >= 90 else 1.4 if rh >= 80 else 2.2 if rh >= 70 else 2.8 if rh >= 60 else 3.7 if rh >= 50 else 4.5


def has_ch(st):
    block = (st.get("previous_runs") or {}).get("hourly", {})
    return any(v is not None for v in block.get(f"temperature_2m_{CH}", []))


def halves_of(rows):
    by_period = collections.defaultdict(list)
    for r in rows:
        by_period[r["period"]].append(r)
    for pr in by_period.values():
        dates = sorted({r["date"] for r in pr})
        first = set(dates[: len(dates) // 2])
        for r in pr:
            r["half"] = 0 if r["date"] in first else 1


# ------------------------------------------------------------------------------------- R1

def r1_rows(data):
    period = period_name(data)
    out = []
    for st in data["stations"]:
        if not has_ch(st):
            continue
        block = st["previous_runs"]
        h = st["forecast"]["hourly"]
        times = [dt.datetime.strptime(t, "%Y-%m-%dT%H:%M") for t in block["hourly"]["time"]]
        obs = observations(st["metar"])
        archive = Series(h, BEST)
        for lead in LEADS:
            best = run_series(block, lead, BEST)
            ch = run_series(block, lead, CH)
            # best_match's amount on day 1 is the archive's: the block has it from day 2.
            best_wet = archive if lead == 1 else best
            for i in range(len(times) - 1):
                o = nearest(obs, times[i])
                temp = o["temp_c"] if o else None
                bt, ct = best("temperature_2m", i), ch("temperature_2m", i)
                seen = wet_seen(obs, times[i])
                bm, cm = best_wet("precipitation", i + 1), ch("precipitation", i + 1)
                out.append({
                    "period": period, "icao": st["icao"], "po": st["icao"] in PO, "lead": lead,
                    "date": times[i].date(), "obs_t": temp, "best_t": bt, "ch_t": ct,
                    "seen": seen, "best_mm": bm, "ch_mm": cm,
                })
    return out


def mae(rows, key):
    e = [abs(r[key] - r["obs_t"]) for r in rows if r["obs_t"] is not None and r["best_t"] is not None and r["ch_t"] is not None]
    return (sum(e) / len(e), len(e)) if e else (None, 0)


def csi(rows, key):
    hit = miss = false = 0
    for r in rows:
        if r["seen"] is None or r["best_mm"] is None or r["ch_mm"] is None:
            continue
        said = r[key] >= MEASURABLE_MM - 1e-9
        hit += said and r["seen"]
        miss += (not said) and r["seen"]
        false += said and not r["seen"]
    n = hit + miss + false
    return (hit / n if n else None), hit + miss


def fmt(x, d=2):
    return "—" if x is None else f"{x:.{d}f}"


def r1(rows, periods):
    print("\n## R1 — ICON-CH al posto di `best_match` (giorni 1-5)\n")
    print("MAE della temperatura dell'ora (°C) e CSI dell'ora bagnata (>= 0,1 mm), stesse ore per i due.\n")
    print("| Gruppo | Giorni | Periodo | ore T | MAE best | MAE ICON-CH | ore bagnate | CSI best | CSI ICON-CH |")
    print("|---|---|---|---:|---:|---:|---:|---:|---:|")
    verdict = {}
    for name, sel in (("Pianura padana", lambda r: r["po"]), ("Fuori dall'Italia", lambda r: not r["po"])):
        for gname, leads in GROUPS:
            ok = True
            for p in periods:
                cr = [r for r in rows if sel(r) and r["lead"] in leads and r["period"] == p]
                mb, n = mae(cr, "best_t")
                mc, _ = mae(cr, "ch_t")
                cb, wet = csi(cr, "best_mm")
                cc, _ = csi(cr, "ch_mm")
                print(f"| {name} | {gname} | {p} | {n} | {fmt(mb)} | {fmt(mc)} | {wet} | {fmt(cb, 3)} | {fmt(cc, 3)} |")
                if None in (mb, mc, cb, cc) or mc > mb - R1_MAE_C or cc < cb + R1_CSI:
                    ok = False
                if n == 0:
                    ok = None if ok is not False else False
            if name == "Pianura padana":
                verdict[gname] = ok
    print()
    for gname, ok in verdict.items():
        print(f"- **R1, pianura padana, giorni {gname}: {'entra' if ok else 'non entra'}**")
    print()
    print("Per aeroporto (pianura padana e fuori), tutti i giorni 1-5, i due periodi insieme:\n")
    print("| Aeroporto | ore | MAE best | MAE ICON-CH | CSI best | CSI ICON-CH |")
    print("|---|---:|---:|---:|---:|---:|")
    for icao in sorted({r["icao"] for r in rows}):
        sr = [r for r in rows if r["icao"] == icao]
        mb, n = mae(sr, "best_t")
        mc, _ = mae(sr, "ch_t")
        cb, _ = csi(sr, "best_mm")
        cc, _ = csi(sr, "ch_mm")
        print(f"| {icao} | {n} | {fmt(mb)} | {fmt(mc)} | {fmt(cb, 3)} | {fmt(cc, 3)} |")
    return verdict


# ------------------------------------------------------------------------------------- R2

def r2_rows(data):
    period = period_name(data)
    out = []
    for st in data["stations"]:
        if not has_ch(st) or st["icao"] not in PO:
            continue
        block = st["previous_runs"]
        times = [dt.datetime.strptime(t, "%Y-%m-%dT%H:%M") for t in block["hourly"]["time"]]
        obs = observations(st["metar"])
        by_date = collections.defaultdict(list)
        for i, t in enumerate(times):
            by_date[t.date()].append(i)
        for lead in LEADS:
            series = {m: run_series(block, lead, m) for m in [BEST, CH] + WITNESSES}
            best = series[BEST]
            for date, idx in by_date.items():
                present = {m for m in [CH] + WITNESSES
                           if series[m].has("temperature_2m") and not day_copy(best, series[m], idx)}
                for i in idx:
                    b = best("temperature_2m", i)
                    o = nearest(obs, times[i])
                    if b is None or o is None or o["temp_c"] is None:
                        continue
                    vals = {BEST: b}
                    for m in present:
                        v = series[m]("temperature_2m", i)
                        if v is not None:
                            vals[m] = v
                    if CH not in vals:
                        continue
                    out.append({"period": period, "icao": st["icao"], "lead": lead, "date": date,
                                "obs": o["temp_c"], "vals": vals})
    return out


_GRIDS = {}


def grid(n):
    if n not in _GRIDS:
        k = round(1 / STEP)
        _GRIDS[n] = np.array([c for c in itertools.product(range(k + 1), repeat=n) if sum(c) == k], dtype=float) / k
    return _GRIDS[n]


def members(lead, with_ch):
    base = [BEST] + ([CH] if with_ch else []) + [IFS, UKMO]
    return base + ([KNMI, DMI] if lead <= 2 else [])


def matrix(rows, mem):
    x = np.array([[r["vals"].get(m, np.nan) for m in mem] for r in rows], dtype=float)
    mask = ~np.isnan(x)
    return np.nan_to_num(x), mask.astype(float)


def blend(x, mask, w):
    num, den = x @ w.T, mask @ w.T
    return np.where(den > 1e-9, num / np.where(den > 1e-9, den, 1.0), x[:, [0]])


def fit_weights(rows, mem):
    x, mask = matrix(rows, mem)
    y = np.array([r["obs"] for r in rows])
    ws = grid(len(mem))
    return ws[int(np.argmin(np.abs(blend(x, mask, ws) - y[:, None]).mean(axis=0)))]


def r2(rows, periods):
    print("\n## R2 — ICON-CH membro della fusione della temperatura (pianura padana, giorni 1-5)\n")
    tested = []
    for p in periods:
        pr = [r for r in rows if r["period"] == p]
        for icao in sorted({r["icao"] for r in pr}):
            for half in (0, 1):
                for lead in LEADS:
                    train = [r for r in pr if r["icao"] != icao and r["half"] != half and r["lead"] == lead]
                    test = [dict(r) for r in pr if r["icao"] == icao and r["half"] == half and r["lead"] == lead]
                    if not train or not test:
                        continue
                    pred = {}
                    for tag, with_ch in (("E", False), ("E+CH", True)):
                        mem = members(lead, with_ch)
                        w = fit_weights(train, mem)
                        x, mask = matrix(test, mem)
                        pred[tag] = blend(x, mask, w[None, :])[:, 0]
                    for k, r in enumerate(test):
                        r["pred"] = {"A": r["vals"][BEST], "CH": r["vals"][CH], "E": float(pred["E"][k]),
                                     "E+CH": float(pred["E+CH"][k])}
                    tested += test
    print("| Giorni | Periodo | ore | A best_match | ICON-CH | E (18.1) | E con ICON-CH |")
    print("|---|---|---:|---:|---:|---:|---:|")
    verdict = {}
    for gname, leads in GROUPS:
        ok = True
        for p in periods:
            cr = [r for r in tested if r["lead"] in leads and r["period"] == p]
            if not cr:
                ok = False
                print(f"| {gname} | {p} | 0 | — | — | — | — |")
                continue
            m = {k: sum(abs(r["pred"][k] - r["obs"]) for r in cr) / len(cr) for k in ("A", "CH", "E", "E+CH")}
            print(f"| {gname} | {p} | {len(cr)} | {m['A']:.2f} | {m['CH']:.2f} | {m['E']:.2f} | {m['E+CH']:.2f} |")
            if m["E+CH"] > m["A"] - R2_MAE_C:
                ok = False
            for icao in {r["icao"] for r in cr}:
                sr = [r for r in cr if r["icao"] == icao]
                sa = sum(abs(r["pred"]["A"] - r["obs"]) for r in sr) / len(sr)
                sx = sum(abs(r["pred"]["E+CH"] - r["obs"]) for r in sr) / len(sr)
                if sx > sa + R2_STATION_C:
                    ok = False
        verdict[gname] = ok
    print()
    for gname, ok in verdict.items():
        print(f"- **R2, pianura padana, giorni {gname}: {'entra' if ok else 'non entra'}**")
    print("\nI pesi su tutta la zona e i due periodi (per leggere):\n")
    for lead in LEADS:
        lr = [r for r in rows if r["lead"] == lead]
        if lr:
            mem = members(lead, True)
            w = fit_weights(lr, mem)
            print(f"- giorno {lead}: " + ", ".join(f"{m.split('_')[0] if m != CH else 'ICON-CH'} {x:.1f}" for m, x in zip(mem, w)))
    return verdict


# ------------------------------------------------------------------------------------- R3

def r3_rows(data):
    period = period_name(data)
    out = []
    for st in data["stations"]:
        if not has_ch(st) or st["icao"] not in PO:
            continue
        prob = (st.get("probability") or {}).get("hourly", {})
        pop = dict(zip(prob.get("time", []), prob.get("precipitation_probability", [])))
        if not pop:
            continue
        h = st["forecast"]["hourly"]
        block = st["previous_runs"]
        times = [dt.datetime.strptime(t, "%Y-%m-%dT%H:%M") for t in h["time"]]
        obs = observations(st["metar"])
        best = Series(h, BEST)
        ch = run_series(block, 1, CH)
        wit = {m: Series(h, m) for m in WITNESSES if Series(h, m).has("precipitation")}
        by_date = collections.defaultdict(list)
        for i, t in enumerate(times):
            by_date[t.date()].append(i)
        copies = {d: {m for m, w in wit.items() if day_copy(best, w, idx)} for d, idx in by_date.items()}
        for i in range(len(times) - 1):
            seen = wet_seen(obs, times[i])
            mm = best("precipitation", i + 1)
            p = pop.get(h["time"][i + 1])
            cm = ch("precipitation", i + 1)
            if seen is None or mm is None or p is None or cm is None:
                continue
            wet = [mm >= MEASURABLE_MM - 1e-9]
            for m, w in wit.items():
                if m in copies[times[i + 1].date()]:
                    continue
                x = w("precipitation", i + 1)
                if x is not None:
                    wet.append(x >= MEASURABLE_MM - 1e-9)
            with_ch = wet + [cm >= MEASURABLE_MM - 1e-9]
            base = {"period": period, "icao": st["icao"], "date": times[i + 1].date(),
                    "seen": 1.0 if seen else 0.0, "p": p / 100.0, "mm": mm, "m": 1.0 if wet[0] else 0.0}
            out.append(dict(base, f=sum(wet) / len(wet), f_ch=sum(with_ch) / len(with_ch)))
    return out


def r3(rows, periods):
    print("\n## R3 — ICON-CH testimone della probabilità del primo giorno (pianura padana)\n")
    tested = []
    for p in periods:
        pr = [r for r in rows if r["period"] == p]
        for icao in sorted({r["icao"] for r in pr}):
            for half in (0, 1):
                train = [r for r in pr if r["icao"] != icao and r["half"] != half]
                test = [dict(r) for r in pr if r["icao"] == icao and r["half"] == half]
                if not train or not test:
                    continue
                beta_d = fit_logistic(train, True)
                beta_ch = fit_logistic([dict(r, f=r["f_ch"]) for r in train], True)
                for r in test:
                    r["pred"] = {"A": r["p"], "D": apply_logistic(beta_d, r, True),
                                 "D+CH": apply_logistic(beta_ch, dict(r, f=r["f_ch"]), True)}
                tested += test
    print("| Periodo | ore | bagnate | Brier D | Brier D+CH | skill contro D | affid. D | affid. D+CH | CSI D | CSI D+CH |")
    print("|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|")
    ok = True
    for p in periods:
        cr = [r for r in tested if r["period"] == p]
        if not cr:
            ok = False
            continue
        bd, bc = brier(cr, "D"), brier(cr, "D+CH")
        skill = 1 - bc / bd
        rd, rc = reliability_error(cr, "D"), reliability_error(cr, "D+CH")
        cd, cc = engine_csi(cr, "D"), engine_csi(cr, "D+CH")
        print(f"| {p} | {len(cr)} | {sum(r['seen'] for r in cr):.0f} | {bd:.4f} | {bc:.4f} | {skill:.3f} | {rd:.3f} | {rc:.3f} | {cd:.3f} | {cc:.3f} |")
        if skill < R3_SKILL or rc > rd or cc < cd - CSI_DROP:
            ok = False
    print(f"\n- **R3, pianura padana: {'entra' if ok else 'non entra'}**")
    return ok


# ---------------------------------------------------------------------------------- la fase

def phase_report(datas):
    print("\n## La fase di ICON-CH contro i METAR (non decide: il dato di 18.7)\n")
    acc = collections.Counter()
    for data in datas:
        for st in data["stations"]:
            if not has_ch(st):
                continue
            block = st["previous_runs"]
            times = [dt.datetime.strptime(t, "%Y-%m-%dT%H:%M") for t in block["hourly"]["time"]]
            obs = observations(st["metar"])
            for lead in LEADS:
                ch = run_series(block, lead, CH)
                for i in range(len(times) - 1):
                    seen = observed_phase(nearest(obs, times[i]))
                    mm, sn = ch("precipitation", i + 1), ch("snowfall", i + 1)
                    if seen is None or mm is None or sn is None or mm < MEASURABLE_MM - 1e-9:
                        continue
                    snow_water = sn / SNOW_CM_PER_MM
                    said = "snow" if snow_water > mm - snow_water else "rain"
                    acc[("ore", seen)] += 1
                    acc[("giuste", seen)] += (said == seen) or (seen == "mixed")
                    all_snow = abs(sn - SNOW_CM_PER_MM * mm) < 0.011 and sn > 0
                    t, rh = ch("temperature_2m", i + 1), ch("relative_humidity_2m", i + 1)
                    if all_snow and t is not None and rh is not None and t > jennings_c(rh):
                        acc[("calda", seen)] += 1
    for seen in ("rain", "snow", "mixed"):
        n = acc[("ore", seen)]
        if n:
            print(f"- METAR {seen}: {n} ore con ICON-CH >= 0,1 mm, fase giusta {acc[('giuste', seen)] / n:.0%}; "
                  f"«tutta neve» sopra la soglia di Jennings: {acc[('calda', seen)]}")


def main(paths):
    datas = []
    for path in paths:
        with gzip.open(path, "rt", encoding="utf-8") as f:
            datas.append(json.load(f))
    periods = [period_name(d) for d in datas]
    inside = sorted({st["icao"] for d in datas for st in d["stations"] if has_ch(st)})
    asked = sorted({st["icao"] for d in datas for st in d["stations"] if st.get("icon_ch")})
    print(f"Aeroporti con ICON-CH: {', '.join(inside) or 'nessuno'} (chiesto a {len(asked)})")
    for d in datas:
        n = [st["icao"] for st in d["stations"] if has_ch(st)]
        print(f"- {period_name(d)}: {len(n)} aeroporti con dei valori")
    if not inside:
        sys.exit("Nessun aeroporto ha ICON-CH: measure_against_metar.py --add-icon-ch")
    rows1 = [r for d in datas for r in r1_rows(d)]
    halves_of(rows1)
    r1(rows1, periods)
    rows2 = [r for d in datas for r in r2_rows(d)]
    halves_of(rows2)
    r2(rows2, periods)
    rows3 = [r for d in datas for r in r3_rows(d)]
    halves_of(rows3)
    r3(rows3, periods)
    phase_report(datas)


if __name__ == "__main__":
    if len(sys.argv) < 2:
        sys.exit(__doc__)
    main(sys.argv[1:])
