"""Spike, Fase 13, domanda 1: una <mask> di Meteocons v3 si riscrive come <clip-path>?

La tesi da dimostrare: una maschera "tutta la tela MENO questa forma", che l'SVG esprime
con due sottopercorsi e `fill-rule="evenodd"`, dà la stessa identica area se si INVERTE
il verso del secondo sottopercorso e si valuta con la regola NON-ZERO -- che e' l'unica
regola che il `<clip-path>` di VectorDrawable sa applicare (non ha `fillType`).

Qui non si guarda un disegno: si rasterizza. Per ogni maschera, l'area evenodd
dell'originale e l'area nonzero della versione convertita vengono campionate su una
griglia da 128x128 e confrontate punto per punto. Zero pixel di differenza o e' falso.
"""
from __future__ import annotations

import math
import os
import re
import sys

# La cartella di @meteocons/svg scompattato (quella che contiene fill/ flat/ line/
# monochrome/). Primo argomento, o la variabile METEOCONS_V3.
BASE = os.environ.get("METEOCONS_V3", "")
FLAT_STEPS = 24          # segmenti per bezier/arco: sovracampiona, e' un test
GRID = 128               # la scatola di v3


# --------------------------------------------------------------------------- parsing

TOKEN = re.compile(r"[MmLlHhVvCcSsQqTtAaZz]|-?\d*\.?\d+(?:[eE][-+]?\d+)?")


def _bezier3(p0, p1, p2, p3, out):
    for i in range(1, FLAT_STEPS + 1):
        t = i / FLAT_STEPS
        u = 1 - t
        out.append((
            u*u*u*p0[0] + 3*u*u*t*p1[0] + 3*u*t*t*p2[0] + t*t*t*p3[0],
            u*u*u*p0[1] + 3*u*u*t*p1[1] + 3*u*t*t*p2[1] + t*t*t*p3[1],
        ))


def _bezier2(p0, p1, p2, out):
    for i in range(1, FLAT_STEPS + 1):
        t = i / FLAT_STEPS
        u = 1 - t
        out.append((
            u*u*p0[0] + 2*u*t*p1[0] + t*t*p2[0],
            u*u*p0[1] + 2*u*t*p1[1] + t*t*p2[1],
        ))


def _arc(p0, rx, ry, phi, large, sweep, p1, out):
    """SVG endpoint arc -> punti. Parametrizzazione centrale, W3C F.6.5."""
    if rx == 0 or ry == 0 or (abs(p0[0]-p1[0]) < 1e-12 and abs(p0[1]-p1[1]) < 1e-12):
        out.append(p1)
        return
    rx, ry = abs(rx), abs(ry)
    phi = math.radians(phi)
    cs, sn = math.cos(phi), math.sin(phi)
    dx2, dy2 = (p0[0]-p1[0])/2.0, (p0[1]-p1[1])/2.0
    x1p = cs*dx2 + sn*dy2
    y1p = -sn*dx2 + cs*dy2
    lam = (x1p*x1p)/(rx*rx) + (y1p*y1p)/(ry*ry)
    if lam > 1:
        s = math.sqrt(lam)
        rx, ry = rx*s, ry*s
    num = rx*rx*ry*ry - rx*rx*y1p*y1p - ry*ry*x1p*x1p
    den = rx*rx*y1p*y1p + ry*ry*x1p*x1p
    co = math.sqrt(max(0.0, num/den)) if den else 0.0
    if large == sweep:
        co = -co
    cxp = co * rx * y1p / ry
    cyp = -co * ry * x1p / rx
    cx = cs*cxp - sn*cyp + (p0[0]+p1[0])/2.0
    cy = sn*cxp + cs*cyp + (p0[1]+p1[1])/2.0

    def ang(ux, uy, vx, vy):
        d = (ux*vx + uy*vy) / (math.hypot(ux, uy) * math.hypot(vx, vy))
        a = math.acos(max(-1.0, min(1.0, d)))
        return -a if ux*vy - uy*vx < 0 else a

    th1 = ang(1, 0, (x1p-cxp)/rx, (y1p-cyp)/ry)
    dth = ang((x1p-cxp)/rx, (y1p-cyp)/ry, (-x1p-cxp)/rx, (-y1p-cyp)/ry)
    if not sweep and dth > 0:
        dth -= 2*math.pi
    elif sweep and dth < 0:
        dth += 2*math.pi
    n = max(FLAT_STEPS, int(abs(dth) / (math.pi/16)) + 1)
    for i in range(1, n+1):
        th = th1 + dth * i / n
        x = cs*rx*math.cos(th) - sn*ry*math.sin(th) + cx
        y = sn*rx*math.cos(th) + cs*ry*math.sin(th) + cy
        out.append((x, y))


def flatten(d: str):
    """Il `d` di un path SVG -> lista di sottopercorsi, ognuno una polilinea chiusa."""
    toks = TOKEN.findall(d)
    i = 0
    subs, cur = [], []
    x = y = sx = sy = 0.0
    prev_c = prev_q = None
    cmd = None
    while i < len(toks):
        t = toks[i]
        if re.match(r"[A-Za-z]", t):
            cmd = t
            i += 1
        elif cmd in ("M", "m"):
            cmd = "L" if cmd == "M" else "l"       # ripetizioni di M sono L
        num = lambda: float(toks[i])

        def take(n):
            nonlocal i
            vals = [float(toks[i+k]) for k in range(n)]
            i += n
            return vals

        if cmd in ("M", "m"):
            ax, ay = take(2)
            if cmd == "m":
                ax, ay = x + ax, y + ay
            if cur:
                subs.append(cur)
            cur = [(ax, ay)]
            x, y = sx, sy = ax, ay
            prev_c = prev_q = None
        elif cmd in ("L", "l"):
            ax, ay = take(2)
            if cmd == "l":
                ax, ay = x + ax, y + ay
            cur.append((ax, ay)); x, y = ax, ay; prev_c = prev_q = None
        elif cmd in ("H", "h"):
            (ax,) = take(1)
            if cmd == "h":
                ax = x + ax
            cur.append((ax, y)); x = ax; prev_c = prev_q = None
        elif cmd in ("V", "v"):
            (ay,) = take(1)
            if cmd == "v":
                ay = y + ay
            cur.append((x, ay)); y = ay; prev_c = prev_q = None
        elif cmd in ("C", "c"):
            x1, y1, x2, y2, ax, ay = take(6)
            if cmd == "c":
                x1, y1, x2, y2, ax, ay = x+x1, y+y1, x+x2, y+y2, x+ax, y+ay
            _bezier3((x, y), (x1, y1), (x2, y2), (ax, ay), cur)
            prev_c = (x2, y2); prev_q = None; x, y = ax, ay
        elif cmd in ("S", "s"):
            x2, y2, ax, ay = take(4)
            if cmd == "s":
                x2, y2, ax, ay = x+x2, y+y2, x+ax, y+ay
            x1, y1 = (2*x - prev_c[0], 2*y - prev_c[1]) if prev_c else (x, y)
            _bezier3((x, y), (x1, y1), (x2, y2), (ax, ay), cur)
            prev_c = (x2, y2); prev_q = None; x, y = ax, ay
        elif cmd in ("Q", "q"):
            x1, y1, ax, ay = take(4)
            if cmd == "q":
                x1, y1, ax, ay = x+x1, y+y1, x+ax, y+ay
            _bezier2((x, y), (x1, y1), (ax, ay), cur)
            prev_q = (x1, y1); prev_c = None; x, y = ax, ay
        elif cmd in ("T", "t"):
            ax, ay = take(2)
            if cmd == "t":
                ax, ay = x+ax, y+ay
            x1, y1 = (2*x - prev_q[0], 2*y - prev_q[1]) if prev_q else (x, y)
            _bezier2((x, y), (x1, y1), (ax, ay), cur)
            prev_q = (x1, y1); prev_c = None; x, y = ax, ay
        elif cmd in ("A", "a"):
            rx, ry, rot, la, sw, ax, ay = take(7)
            if cmd == "a":
                ax, ay = x+ax, y+ay
            _arc((x, y), rx, ry, rot, int(la), int(sw), (ax, ay), cur)
            prev_c = prev_q = None; x, y = ax, ay
        elif cmd in ("Z", "z"):
            if cur:
                cur.append((sx, sy))
                subs.append(cur)
            cur = []
            x, y = sx, sy
            prev_c = prev_q = None
        else:
            raise ValueError(f"comando non gestito: {cmd!r}")
    if cur:
        subs.append(cur)
    return [s for s in subs if len(s) > 2]


# ----------------------------------------------------------------- regole di riempimento

def signed_area(poly):
    a = 0.0
    for (x0, y0), (x1, y1) in zip(poly, poly[1:] + poly[:1]):
        a += x0*y1 - x1*y0
    return a / 2.0


def winding(px, py, subs):
    """Numero di giro (NON-ZERO)."""
    w = 0
    for poly in subs:
        for (x0, y0), (x1, y1) in zip(poly, poly[1:] + poly[:1]):
            if y0 <= py:
                if y1 > py and (x1-x0)*(py-y0) - (px-x0)*(y1-y0) > 0:
                    w += 1
            elif y1 <= py and (x1-x0)*(py-y0) - (px-x0)*(y1-y0) < 0:
                w -= 1
    return w


def crossings(px, py, subs):
    """Parità di attraversamenti (EVEN-ODD)."""
    c = 0
    for poly in subs:
        for (x0, y0), (x1, y1) in zip(poly, poly[1:] + poly[:1]):
            if (y0 > py) != (y1 > py):
                xin = x0 + (py-y0) * (x1-x0) / (y1-y0)
                if px < xin:
                    c ^= 1
    return c


def to_nonzero(subs):
    """La conversione: ogni sottopercorso oltre il primo prende il verso opposto."""
    if not subs:
        return subs
    outer = signed_area(subs[0])
    out = [subs[0]]
    for poly in subs[1:]:
        same = (signed_area(poly) > 0) == (outer > 0)
        out.append(list(reversed(poly)) if same else poly)
    return out


# ------------------------------------------------------------------------------ prova

def mask_paths(svg: str):
    for m in re.findall(r"<mask\b.*?</mask>", svg, re.S):
        for el in re.findall(r"<path\b[^>]*>", m):
            d = re.search(r'\bd="([^"]*)"', el)
            if d:
                yield d.group(1), ("evenodd" in el)


def _edges(subs):
    out = []
    for poly in subs:
        for (x0, y0), (x1, y1) in zip(poly, poly[1:] + poly[:1]):
            if y0 != y1:
                out.append((x0, y0, x1, y1))
    return out


def _row(edges, py, rule):
    """I pixel accesi su una scanline, con la regola chiesta. Scanline, non
    campionamento punto per punto: stessa risposta, due ordini di grandezza meno conti."""
    hits = []
    for x0, y0, x1, y1 in edges:
        if (y0 > py) != (y1 > py):
            hits.append((x0 + (py-y0) * (x1-x0) / (y1-y0), 1 if y1 > y0 else -1))
    hits.sort()
    on = set()
    acc = 0
    step = 128 / GRID
    for i, (xin, dirn) in enumerate(hits):
        acc = (acc + 1) % 2 if rule == "evenodd" else acc + dirn
        inside = acc != 0 if rule == "nonzero" else acc == 1
        if inside and i + 1 < len(hits):
            lo, hi = xin, hits[i+1][0]
            g0 = max(0, int(lo / step))
            g1 = min(GRID - 1, int(hi / step))
            for g in range(g0, g1 + 1):
                px = (g + 0.5) * step
                if lo <= px < hi:
                    on.add((g,))
        elif inside:
            pass
    return on


def prove(d: str, evenodd: bool = True):
    subs = flatten(d)
    conv = to_nonzero(subs)
    e_orig, e_conv = _edges(subs), _edges(conv)
    bad = 0
    step = 128 / GRID
    for gy in range(GRID):
        py = (gy + 0.5) * step
        a = _row(e_orig, py, "evenodd" if evenodd else "nonzero")
        b = _row(e_conv, py, "nonzero")
        if a != b:
            bad += len(a ^ b)
    return bad, len(subs)


def main():
    global BASE
    args = sys.argv[1:]
    if args and os.path.isdir(os.path.join(args[0], "line")):
        BASE = args[0]
        args = args[1:]
    if not BASE or not os.path.isdir(BASE):
        sys.exit("uso: spike_mask_clip.py <cartella di @meteocons/svg> [stili...]")
    styles = args or ["line", "flat", "fill", "monochrome"]
    total = ok = 0
    failures = []
    for style in styles:
        d = os.path.join(BASE, style)
        for f in sorted(os.listdir(d)):
            svg = open(os.path.join(d, f), encoding="utf8").read()
            for pd, evenodd in mask_paths(svg):
                total += 1
                bad, n = prove(pd, evenodd)
                if bad == 0:
                    ok += 1
                else:
                    failures.append((style, f[:-4], n, bad))
    print(f"maschere evenodd provate: {total}")
    print(f"identiche dopo la conversione: {ok}")
    print(f"diverse: {len(failures)}")
    for s, n, ns, bad in failures[:20]:
        print(f"   {s}/{n}: {ns} sottopercorsi, {bad} punti di differenza su {GRID*GRID}")
    return 0 if not failures else 1


if __name__ == "__main__":
    raise SystemExit(main())
