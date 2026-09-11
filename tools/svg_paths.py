#!/usr/bin/env python3
"""I percorsi SVG, esatti: leggerli, girarli, riscriverli, e la maschera che diventa clip.

Estratto dallo spike della Fase 13 perche' serve a due strumenti: `import_meteocons_v3.py`
lo usa per importare, `spike_mask_clip.py` per dimostrare che l'importazione e' lecita.

Il pezzo che conta e' [mask_to_clip]. Una `<mask>` di Meteocons dice "tutta la tela MENO
questa forma" con due sottopercorsi e `fill-rule="evenodd"`; VectorDrawable non ha le
maschere, ha `<clip-path>`, e il suo parser applica **NON-ZERO** senza offrire un
`fillType`. Le due regole coincidono se il sottopercorso interno viene percorso al
contrario, e l'inversione e' fatta **sui comandi** e non sulla polilinea: appiattire le
curve gonfierebbe il path di venti volte e perderebbe precisione. Ogni conversione viene
poi ri-rasterizzata e confrontata con l'originale prima di essere restituita: un pixel di
differenza e' un errore, non un avviso.
"""
from __future__ import annotations

import importlib.util
import pathlib
import re
import sys

HERE = pathlib.Path(__file__).resolve().parent


def _load(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


#: Il rasterizzatore a scanline che decide se una conversione e' identica.
raster = _load("raster", HERE / "spike_mask_clip.py")


TOKEN = re.compile(r"[MmLlHhVvCcSsQqTtAaZz]|-?\d*\.?\d+(?:[eE][-+]?\d+)?")


def parse_segments(d: str):
    """`d` -> sottopercorsi [(punto iniziale, [segmenti])], tutto in assoluto.

    Segmento: ('L', p) | ('C', c1, c2, p) | ('Q', c, p) | ('A', rx, ry, rot, laf, sf, p).
    Le abbreviazioni (H V S T) sono espanse, perche' un segmento va poi girato.
    """
    toks = TOKEN.findall(d)
    i = 0
    subs = []
    cur = None
    x = y = sx = sy = 0.0
    prev_c = prev_q = None
    cmd = None

    def take(n):
        nonlocal i
        vals = [float(toks[i + k]) for k in range(n)]
        i += n
        return vals

    while i < len(toks):
        if re.match(r"[A-Za-z]", toks[i]):
            cmd = toks[i]
            i += 1
            if cmd in ("Z", "z"):
                if cur:
                    subs.append(cur)
                cur = None
                x, y = sx, sy
                continue
        elif cmd in ("M", "m"):
            cmd = "L" if cmd == "M" else "l"

        if cmd in ("M", "m"):
            ax, ay = take(2)
            if cmd == "m":
                ax, ay = x + ax, y + ay
            if cur:
                subs.append(cur)
            cur = [(ax, ay), []]
            x, y = sx, sy = ax, ay
            prev_c = prev_q = None
            continue
        if cur is None:
            sys.exit("percorso che non inizia con M")
        if cmd in ("L", "l", "H", "h", "V", "v"):
            if cmd in ("L", "l"):
                ax, ay = take(2)
                if cmd == "l":
                    ax, ay = x + ax, y + ay
            elif cmd in ("H", "h"):
                (ax,) = take(1)
                ax, ay = (x + ax if cmd == "h" else ax), y
            else:
                (ay,) = take(1)
                ax, ay = x, (y + ay if cmd == "v" else ay)
            cur[1].append(("L", (ax, ay)))
            x, y = ax, ay
            prev_c = prev_q = None
        elif cmd in ("C", "c", "S", "s"):
            if cmd in ("C", "c"):
                x1, y1, x2, y2, ax, ay = take(6)
                if cmd == "c":
                    x1, y1, x2, y2, ax, ay = x+x1, y+y1, x+x2, y+y2, x+ax, y+ay
            else:
                x2, y2, ax, ay = take(4)
                if cmd == "s":
                    x2, y2, ax, ay = x+x2, y+y2, x+ax, y+ay
                x1, y1 = (2*x - prev_c[0], 2*y - prev_c[1]) if prev_c else (x, y)
            cur[1].append(("C", (x1, y1), (x2, y2), (ax, ay)))
            prev_c, prev_q = (x2, y2), None
            x, y = ax, ay
        elif cmd in ("Q", "q", "T", "t"):
            if cmd in ("Q", "q"):
                x1, y1, ax, ay = take(4)
                if cmd == "q":
                    x1, y1, ax, ay = x+x1, y+y1, x+ax, y+ay
            else:
                ax, ay = take(2)
                if cmd == "t":
                    ax, ay = x+ax, y+ay
                x1, y1 = (2*x - prev_q[0], 2*y - prev_q[1]) if prev_q else (x, y)
            cur[1].append(("Q", (x1, y1), (ax, ay)))
            prev_q, prev_c = (x1, y1), None
            x, y = ax, ay
        elif cmd in ("A", "a"):
            rx, ry, rot, laf, sf, ax, ay = take(7)
            if cmd == "a":
                ax, ay = x + ax, y + ay
            cur[1].append(("A", rx, ry, rot, int(laf), int(sf), (ax, ay)))
            prev_c = prev_q = None
            x, y = ax, ay
        else:
            sys.exit(f"comando non gestito: {cmd!r}")
    if cur:
        subs.append(cur)
    return subs


def reverse_subpath(sub):
    """Lo stesso contorno percorso al contrario. Esatto: nessuna curva viene appiattita.

    Una cubica P0,C1,C2,P1 girata e' P1,C2,C1,P0; una quadratica scambia solo gli
    estremi; un arco tiene raggi e rotazione, tiene `large-arc` e **inverte `sweep`**,
    perche' e' il verso di percorrenza a cambiare, non l'ellisse.
    """
    start, segs = sub
    pts = [start] + [s[-1] for s in segs]
    out_start = pts[-1]
    out = []
    for k in range(len(segs) - 1, -1, -1):
        seg, p_from = segs[k], pts[k]
        if seg[0] == "L":
            out.append(("L", p_from))
        elif seg[0] == "C":
            out.append(("C", seg[2], seg[1], p_from))
        elif seg[0] == "Q":
            out.append(("Q", seg[1], p_from))
        elif seg[0] == "A":
            _, rx, ry, rot, laf, sf, _ = seg
            out.append(("A", rx, ry, rot, laf, 1 - sf, p_from))
    return [out_start, out]


def fmt(v: float) -> str:
    s = f"{v:.4f}".rstrip("0").rstrip(".")
    return s if s not in ("", "-0") else "0"


def emit_path(subs) -> str:
    bits = []
    for start, segs in subs:
        bits.append(f"M{fmt(start[0])},{fmt(start[1])}")
        for seg in segs:
            if seg[0] == "L":
                bits.append(f"L{fmt(seg[1][0])},{fmt(seg[1][1])}")
            elif seg[0] == "C":
                bits.append("C" + ",".join(
                    fmt(c) for p in seg[1:] for c in p))
            elif seg[0] == "Q":
                bits.append("Q" + ",".join(fmt(c) for p in seg[1:] for c in p))
            elif seg[0] == "A":
                _, rx, ry, rot, laf, sf, p = seg
                bits.append(
                    f"A{fmt(rx)},{fmt(ry)},{fmt(rot)},{laf},{sf},{fmt(p[0])},{fmt(p[1])}")
        bits.append("Z")
    return " ".join(bits)


def mask_to_clip(d: str) -> str:
    """La maschera evenodd -> un pathData che NON-ZERO legge allo stesso modo.

    Ri-verificata qui, non data per buona altrove: si rasterizzano le due versioni e si
    confrontano. Un pixel di differenza e' un errore, non un avviso.
    """
    subs = parse_segments(d)
    if len(subs) == 1:
        out = subs
    else:
        outer = raster.signed_area(raster.flatten(emit_path([subs[0]]))[0])
        out = [subs[0]]
        for sub in subs[1:]:
            area = raster.signed_area(raster.flatten(emit_path([sub]))[0])
            out.append(reverse_subpath(sub) if (area > 0) == (outer > 0) else sub)
    result = emit_path(out)
    bad, _ = _verify(d, result)
    if bad:
        sys.exit(f"la conversione della maschera cambia {bad} pixel — non si spedisce")
    return result


def _verify(original: str, converted: str):
    a = raster.flatten(original)
    b = raster.flatten(converted)
    ea, eb = raster._edges(a), raster._edges(b)
    bad = 0
    step = 128 / raster.GRID
    for gy in range(raster.GRID):
        py = (gy + 0.5) * step
        ra = raster._row(ea, py, "evenodd")
        rb = raster._row(eb, py, "nonzero")
        if ra != rb:
            bad += len(ra ^ rb)
    return bad, len(a)


def circle_to_path(cx: float, cy: float, r: float) -> str:
    return (f"M{fmt(cx - r)},{fmt(cy)} "
            f"A{fmt(r)},{fmt(r)},0,1,1,{fmt(cx + r)},{fmt(cy)} "
            f"A{fmt(r)},{fmt(r)},0,1,1,{fmt(cx - r)},{fmt(cy)} Z")
