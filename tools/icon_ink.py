#!/usr/bin/env python3
"""Quanto della propria scatola occupa il disegno di ogni icona — e se l'importazione
l'ha cambiato.

Nasce da una domanda del committente (11 set 2026), guardando «In arrivo» nel Cielo: la
luna piena e la luna con le stelle cadenti hanno taglie molto diverse, **e' l'importatore
che ridimensiona?** La risposta era no: le differenze erano dell'illustratore, e le
misurava questo strumento. Lo stesso giorno sono diventate un difetto da togliere, e
allora **e' questo strumento a decidere di quanto**: [scales] e' la tabella che
`import_meteocons_v3.py` cuoce nei drawable come gruppo `mc3scale`, e `--confronto` e'
diventato la prova che l'importazione non fa **nient'altro** che quella scala.

I modi in cui misura:

- `--sorgente` misura le SVG di Meteocons, cioe' il disegno dell'illustratore;
- `--confronto` misura le stesse icone **dopo** l'importazione e mette le due misure
  accanto. Se una riga non pareggia, l'importatore ha toccato la geometria;
- `--dentro <parola>` misura **un ramo solo** del disegno, per nome: e' cosi' che si
  confronta la luna dentro un'icona composta con la luna che sta da sola;
- `--copertura` conta l'inchiostro invece di misurarne il riquadro.

La misura principale e' il **lato dell'inchiostro in frazione della scatola**: si
appiattiscono tutte le forme (gli archi compresi, e i `transform` statici si compongono),
si allarga di mezzo tratto per lato dove c'e' un tratto, e si prende il riquadro che
contiene tutto. Non e' la copertura, e le due rispondono a due domande diverse: il lato
dice quanto il disegno **e' grande**, la copertura quanto **pesa**.

    python tools/icon_ink.py --sorgente path/al/package
    python tools/icon_ink.py --confronto path/al/package
    python tools/icon_ink.py --dentro moon path/al/package clear-night falling-stars
    python tools/icon_ink.py --copertura path/al/package moon-full falling-stars

Il pacchetto e' quello che scompatta `import_meteocons_v3.py` (`npm pack @meteocons/svg`).
"""
from __future__ import annotations

import importlib.util
import math
import pathlib
import sys
import xml.etree.ElementTree as ET

HERE = pathlib.Path(__file__).resolve().parent
DRAWABLE = HERE.parent / "app" / "src" / "main" / "res" / "drawable"
SVG_NS = "{http://www.w3.org/2000/svg}"
A = "{http://schemas.android.com/apk/res/android}"


def _load(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


raster = _load("raster", HERE / "spike_mask_clip.py")
paths = _load("svg_paths", HERE / "svg_paths.py")
shipped = _load("shipped_icons", HERE / "shipped_icons.py")


def flatten(d: str):
    """`d` -> polilinee, **tenendo i sottopercorsi di due punti**.

    `spike_mask_clip.flatten` li scarta, e li' e' giusto: serve a provare delle maschere,
    che sono forme chiuse. Qui no: la scia di `falling-stars` E' un segmento di due punti,
    e buttarlo via faceva sparire 0,25 di scatola — cioe' esattamente il tipo di errore
    che questo strumento esiste per escludere (misurato, 11 set 2026).
    """
    out = []
    for start, segs, _closed in paths.parse_segments(d):
        pts = [start]
        for seg in segs:
            p0 = pts[-1]
            if seg[0] == "L":
                pts.append(seg[1])
            elif seg[0] == "C":
                raster._bezier3(p0, seg[1], seg[2], seg[3], pts)
            elif seg[0] == "Q":
                raster._bezier2(p0, seg[1], seg[2], pts)
            elif seg[0] == "A":
                _, rx, ry, rot, laf, sf, p1 = seg
                raster._arc(p0, rx, ry, rot, laf, sf, p1, pts)
            else:
                pts.append(seg[-1])
        out.append(pts)
    return out

#: Gli stili che l'app puo' disegnare. La scatola non dipende dallo stile — misurato:
#: `line` e `flat` danno lo stesso lato a meno di un centesimo — ma il confronto con il
#: convertito si fa per stile, perche' i file sono diversi.
STYLES = ("line", "flat")
#: Prefisso del convertito per stile, fondo chiaro (il fondo cambia i colori, non la
#: geometria, quindi per misurare la taglia basta uno dei due).
PREFIX = {"line": "mc3_", "flat": "mc3f_"}
#: Un centesimo di unita' su 128: sotto questa soglia la differenza e' l'appiattimento
#: delle curve, non la geometria.
TOL = 0.01
NL = chr(10)


# --------------------------------------------------------------------------- matrici

def mat_mul(a, b):
    """Due affini 2x3 (a, b, c, d, e, f) come in SVG: la composizione a∘b."""
    a0, a1, a2, a3, a4, a5 = a
    b0, b1, b2, b3, b4, b5 = b
    return (a0*b0 + a2*b1, a1*b0 + a3*b1,
            a0*b2 + a2*b3, a1*b2 + a3*b3,
            a0*b4 + a2*b5 + a4, a1*b4 + a3*b5 + a5)


IDENT = (1.0, 0.0, 0.0, 1.0, 0.0, 0.0)


def apply(m, p):
    x, y = p
    return (m[0]*x + m[2]*y + m[4], m[1]*x + m[3]*y + m[5])


def group_matrix(el):
    """La trasformazione di un `<group>` di VectorDrawable, nel suo ordine: si porta il
    perno nell'origine, si ruota, si scala, si rimette il perno, poi si trasla."""
    g = lambda n, d: float(el.get(A + n, d))
    px, py = g("pivotX", "0"), g("pivotY", "0")
    rot = math.radians(g("rotation", "0"))
    sx, sy = g("scaleX", "1"), g("scaleY", "1")
    tx, ty = g("translateX", "0"), g("translateY", "0")
    cos, sin = math.cos(rot), math.sin(rot)
    m = (1.0, 0.0, 0.0, 1.0, tx + px, ty + py)
    m = mat_mul(m, (cos, sin, -sin, cos, 0.0, 0.0))
    m = mat_mul(m, (sx, 0.0, 0.0, sy, 0.0, 0.0))
    return mat_mul(m, (1.0, 0.0, 0.0, 1.0, -px, -py))


# ------------------------------------------------------------------------- le forme

def _circle(cx, cy, r, steps=180):
    return [(cx + r*math.cos(i*2*math.pi/steps), cy + r*math.sin(i*2*math.pi/steps))
            for i in range(steps)]


def svg_points(el):
    """Una forma SVG -> i suoi punti. Gli archi passano da `flatten`, che li parametrizza
    davvero: prenderne i soli estremi farebbe sparire ogni cerchio scritto con `A`."""
    tag = el.tag[len(SVG_NS):] if el.tag.startswith(SVG_NS) else el.tag
    if tag == "path":
        d = el.get("d")
        return [p for sub in flatten(d) for p in sub] if d else []
    if tag == "circle":
        return _circle(float(el.get("cx")), float(el.get("cy")), float(el.get("r")))
    if tag == "ellipse":
        cx, cy = float(el.get("cx")), float(el.get("cy"))
        rx, ry = float(el.get("rx")), float(el.get("ry"))
        return [(cx + rx*math.cos(a*2*math.pi/180), cy + ry*math.sin(a*2*math.pi/180))
                for a in range(180)]
    if tag == "rect":
        x, y = float(el.get("x", 0)), float(el.get("y", 0))
        w, h = float(el.get("width")), float(el.get("height"))
        return [(x, y), (x+w, y), (x+w, y+h), (x, y+h)]
    if tag == "line":
        return [(float(el.get("x1")), float(el.get("y1"))),
                (float(el.get("x2")), float(el.get("y2")))]
    return []


class Box:
    """Il riquadro che si allarga un punto alla volta."""

    def __init__(self):
        self.x0 = self.y0 = float("inf")
        self.x1 = self.y1 = float("-inf")

    def add(self, pts, pad=0.0):
        for x, y in pts:
            self.x0 = min(self.x0, x - pad)
            self.y0 = min(self.y0, y - pad)
            self.x1 = max(self.x1, x + pad)
            self.y1 = max(self.y1, y + pad)

    @property
    def empty(self):
        return self.x0 > self.x1

    def frac(self, vw, vh):
        return (self.x1 - self.x0) / vw, (self.y1 - self.y0) / vh


# ------------------------------------------------------------------- la sorgente SVG

def ink_svg(path: pathlib.Path, only: str | None = None):
    """(largo, alto) in frazione della scatola, dalla SVG dell'illustratore.

    Con [only] si misura solo il ramo il cui `id` contiene quella parola — e' cosi' che
    si confronta *la luna dentro* un disegno composto con la luna da sola.
    """
    root = ET.parse(path).getroot()
    vb = [float(v) for v in root.get("viewBox").split()]
    box = Box()

    def walk(el, inherited, picked, m):
        for ch in el:
            if ch.tag == SVG_NS + "defs":
                continue
            here = picked or (only is not None
                              and only in (ch.get("id") or "").lower())
            attrs = dict(inherited)
            for k in ("stroke", "stroke-width"):
                if ch.get(k) is not None:
                    attrs[k] = ch.get(k)
            # Il `transform` statico e' raro ma decisivo: l'ago del barometro e' un
            # rettangolo dritto ruotato di -135 gradi, e misurarlo fermo lo fa uscire
            # dalla scatola di nove unita' (misurato, 11 set 2026).
            here_m = m
            if ch.get("transform"):
                here_m = mat_mul(m, paths.parse_transform(ch.get("transform")))
            if ch.tag == SVG_NS + "g":
                walk(ch, attrs, here, here_m)
                continue
            pts = svg_points(ch)
            if not pts or (only is not None and not here):
                continue
            stroke, sw = attrs.get("stroke"), attrs.get("stroke-width")
            # `stroke-width` assente vuol dire 1, non zero (SVG 1.1 §11.4) — ed e' cosi'
            # che l'importatore lo scrive. Leggerlo come zero faceva risultare 18 icone
            # convertite piu' grandi della loro sorgente (misurato, 11 set 2026).
            pad = float(sw or 1) / 2 if (stroke and stroke != "none") else 0.0
            box.add([apply(here_m, p) for p in pts], pad)

    walk(root, {}, False, IDENT)
    return None if box.empty else box.frac(vb[2], vb[3])


# ----------------------------------------------------------------- il convertito VD

def ink_vd(path: pathlib.Path):
    """(largo, alto) in frazione della scatola, dal vector drawable generato.

    Le trasformazioni dei gruppi si compongono: quaranta disegni fermi ne portano una
    (l'ago del barometro, la freccia del vento), e ignorarla sposterebbe il riquadro.
    """
    root = ET.parse(path).getroot()
    vw = float(root.get(A + "viewportWidth"))
    vh = float(root.get(A + "viewportHeight"))
    box = Box()

    def walk(el, m):
        for ch in el:
            if ch.tag == "group":
                walk(ch, mat_mul(m, group_matrix(ch)))
            elif ch.tag == "path":
                d = ch.get(A + "pathData")
                if not d:
                    continue
                sw = ch.get(A + "strokeWidth")
                stroke = ch.get(A + "strokeColor")
                # Il tratto vive nello spazio del gruppo, quindi si scala con lui: il
                # mezzo tratto va misurato DOPO la trasformazione, o un'icona dentro
                # `mc3scale` risulta piu' stretta di quel che disegna.
                grow = math.sqrt(abs(m[0] * m[3] - m[1] * m[2])) or 1.0
                pad = (float(sw) / 2) * grow if (stroke and sw) else 0.0
                pts = [apply(m, p) for sub in flatten(d) for p in sub]
                box.add(pts, pad)

    walk(root, IDENT)
    return None if box.empty else box.frac(vw, vh)


# --------------------------------------------------------------- la normalizzazione

#: La frazione di scatola a cui ogni disegno viene portato (DESIGN §13.1). E' il valore
#: che la famiglia piu' grande gia' ha — `clear-day`, `sunrise`, la scala UV — quindi i
#: disegni che oggi stanno bene non si muovono e tutti gli altri salgono fino a loro.
TARGET = 0.75


def ink_box(pkg: pathlib.Path, name: str) -> tuple[Box, float, float] | None:
    """Il riquadro dell'inchiostro di un'icona **in unita' di viewport**, preso come
    unione di tutti gli stili.

    L'unione e non uno stile solo: `line` sborda di mezzo tratto dove `flat` no, e una
    scala diversa per stile farebbe cambiare taglia all'icona quando il lettore cambia
    stile in Impostazioni — che e' l'opposto di quel che la scala serve a fare.
    """
    box = Box()
    vw = vh = 0.0
    for style in STYLES:
        p = pkg / style / f"{name}.svg"
        if not p.exists():
            continue
        vb = [float(v) for v in ET.parse(p).getroot().get("viewBox").split()]
        vw, vh = vb[2], vb[3]
        b = _ink_box_one(p)
        if not b.empty:
            box.add([(b.x0, b.y0), (b.x1, b.y1)])
    return None if box.empty or not vw else (box, vw, vh)


def _ink_box_one(path: pathlib.Path) -> Box:
    """Lo stesso attraversamento di [ink_svg], ma il riquadro invece della frazione."""
    root = ET.parse(path).getroot()
    box = Box()

    def walk(el, inherited, m):
        for ch in el:
            if ch.tag == SVG_NS + "defs":
                continue
            attrs = dict(inherited)
            for k in ("stroke", "stroke-width"):
                if ch.get(k) is not None:
                    attrs[k] = ch.get(k)
            here_m = m
            if ch.get("transform"):
                here_m = mat_mul(m, paths.parse_transform(ch.get("transform")))
            if ch.tag == SVG_NS + "g":
                walk(ch, attrs, here_m)
                continue
            pts = svg_points(ch)
            if not pts:
                continue
            stroke, sw = attrs.get("stroke"), attrs.get("stroke-width")
            pad = float(sw or 1) / 2 if (stroke and stroke != "none") else 0.0
            box.add([apply(here_m, p) for p in pts], pad)

    walk(root, {}, IDENT)
    return box


def scale_of(box: Box, vw: float, vh: float, target: float = TARGET) -> float:
    """La scala che porta il lato dell'inchiostro a [target] della scatola.

    **Il perno e' il centro della scatola**, non il centro del disegno: ricentrare
    sposterebbe le composizioni che sono volutamente fuori centro (il sole di `sunrise`
    sta basso perche' sorge da una linea) e cambierebbe il centraggio ottico su cui la
    striscia oraria e' stata messa a punto. La conseguenza e' che un disegno fuori centro
    tocca il bordo prima di arrivare a [target], e li' la scala si ferma: meglio un'icona
    un po' sotto misura che una tagliata.
    """
    side = max(box.x1 - box.x0, box.y1 - box.y0) / max(vw, vh)
    if side <= 0:
        return 1.0
    cx, cy = vw / 2, vh / 2
    reach = max(cx - box.x0, box.x1 - cx, cy - box.y0, box.y1 - cy)
    ceiling = (min(cx, cy) / reach) if reach > 0 else 1.0
    return min(target / side, ceiling)


def scales(pkg: pathlib.Path, target: float = TARGET) -> dict[str, float]:
    """{nome icona: scala} per tutta la famiglia. E' quel che l'importatore cuoce nei
    drawable, ed e' la sola cosa che il repo aggiunge al disegno dell'illustratore."""
    out = {}
    for svg in sorted((pkg / "line").glob("*.svg")):
        measured = ink_box(pkg, svg.stem)
        if measured is None:
            continue
        box, vw, vh = measured
        out[svg.stem] = round(scale_of(box, vw, vh, target), 4)
    return out


# ------------------------------------------------------------------- la copertura

def coverage(path: pathlib.Path) -> float:
    """La frazione della scatola davvero coperta d'inchiostro, sulla sorgente.

    I pieni si rasterizzano con la scanline del modulo delle maschere; i tratti si
    contano come lunghezza x spessore (le sovrapposizioni sono qualche decimo di unita'
    su un totale di migliaia, e questo numero serve a confrontare, non a certificare).
    """
    root = ET.parse(path).getroot()
    on: set[tuple[int, int]] = set()
    extra = 0.0

    def walk(el, inherited):
        nonlocal extra
        for ch in el:
            if ch.tag == SVG_NS + "defs":
                continue
            attrs = dict(inherited)
            for k in ("fill", "stroke", "stroke-width", "fill-rule"):
                if ch.get(k) is not None:
                    attrs[k] = ch.get(k)
            if ch.tag == SVG_NS + "g":
                walk(ch, attrs)
                continue
            fill, stroke = attrs.get("fill"), attrs.get("stroke")
            sw = float(attrs.get("stroke-width") or 0)
            tag = ch.tag[len(SVG_NS):]
            if tag == "line":
                if stroke and stroke != "none":
                    p0 = (float(ch.get("x1")), float(ch.get("y1")))
                    p1 = (float(ch.get("x2")), float(ch.get("y2")))
                    extra += math.dist(p0, p1) * sw + math.pi * (sw / 2) ** 2
                continue
            if tag == "path":
                subs = flatten(ch.get("d"))
            elif tag == "circle":
                subs = [_circle(float(ch.get("cx")), float(ch.get("cy")),
                                float(ch.get("r")))]
            elif tag == "rect":
                x, y = float(ch.get("x", 0)), float(ch.get("y", 0))
                w, h = float(ch.get("width")), float(ch.get("height"))
                subs = [[(x, y), (x+w, y), (x+w, y+h), (x, y+h)]]
            else:
                continue
            if fill and fill != "none":
                rule = "evenodd" if attrs.get("fill-rule") == "evenodd" else "nonzero"
                edges = raster._edges(subs)
                step = 128 / raster.GRID
                for gy in range(raster.GRID):
                    for g in raster._row(edges, (gy + 0.5) * step, rule):
                        on.add((g[0], gy))
            elif stroke and stroke != "none":
                for poly in subs:
                    extra += sum(math.dist(p, q)
                                 for p, q in zip(poly, poly[1:])) * sw

    walk(root, {})
    cell = (128 / raster.GRID) ** 2
    return (len(on) * cell + extra) / (128 * 128)


# ------------------------------------------------------------------------ i rapporti

def _pkg(argv, i=0):
    if len(argv) <= i:
        sys.exit("serve il percorso del pacchetto Meteocons scompattato")
    return pathlib.Path(argv[i])


def report_source(pkg, names, style="line", box_dp=34.0, only=None):
    rows = []
    for n in names:
        p = pkg / style / f"{n}.svg"
        if not p.exists():
            print(f"{n:26s}  assente a monte")
            continue
        measured = ink_svg(p, only)
        if measured is None:
            print(f"{n:26s}  nessun ramo «{only}»")
            continue
        fw, fh = measured
        rows.append((n, fw, fh))
    rows.sort(key=lambda r: -max(r[1], r[2]))
    print(f"{'icona':26s}  largo  alto   lato   su {box_dp:g}dp")
    for n, fw, fh in rows:
        side = max(fw, fh)
        print(f"{n:26s}  {fw:5.2f}  {fh:5.2f}  {side:5.2f}  {side*box_dp:5.1f} dp")
    if rows:
        sides = [max(f, h) for _, f, h in rows]
        print(f"{NL}escursione {min(sides):.2f} -> {max(sides):.2f}"
              f"  ({max(sides)/min(sides):.2f}x)")
    return rows


def report_diff(pkg, names, box_dp=34.0):
    """Sorgente contro convertito, **al netto della scala dichiarata**.

    Il drawable spedito porta il gruppo `mc3scale` (l'importatore, §4b), quindi il
    confronto giusto non e' «sorgente uguale a convertito» ma «convertito uguale a
    sorgente per la sua scala»: se una riga non pareggia, l'importazione ha toccato la
    geometria oltre a quel fattore, che e' la sola cosa che ha il permesso di fare.
    """
    bad = 0
    ks = scales(pkg)
    print(f"{'icona':26s} {'stile':5s}  attesa        convertito    scarto")
    for n in names:
        k = ks.get(n, 1.0)
        for style in STYLES:
            src = pkg / style / f"{n}.svg"
            vd = DRAWABLE / f"{PREFIX[style]}{n.replace('-', '_')}.xml"
            if not src.exists() or not vd.exists():
                continue
            a, b = ink_svg(src), ink_vd(vd)
            if a is None or b is None:
                continue
            want = (a[0] * k, a[1] * k)
            d = max(abs(want[0] - b[0]), abs(want[1] - b[1]))
            flag = "  <-- DIVERSO" if d > TOL else ""
            if flag:
                bad += 1
            if flag or "-v" in sys.argv:
                print(f"{n:26s} {style:5s}  {want[0]:.3f} x {want[1]:.3f}  "
                      f"{b[0]:.3f} x {b[1]:.3f}  {d:.4f}{flag}")
    print(f"{NL}{bad} icone fuori tolleranza ({TOL})")
    return bad


def main():
    argv = sys.argv[1:]
    if not argv:
        sys.exit(__doc__)
    mode, rest = argv[0], argv[1:]
    if mode == "--sorgente":
        pkg = _pkg(rest)
        names = rest[1:] or sorted(set(shipped.SHIPPED))
        report_source(pkg, names)
    elif mode == "--confronto":
        pkg = _pkg(rest)
        names = rest[1:] or sorted(set(shipped.SHIPPED))
        sys.exit(1 if report_diff(pkg, names) else 0)
    elif mode == "--dentro":
        # «Quanto e' grande la luna DENTRO questo disegno»: la domanda che separa
        # un'icona semplice da una composta, e l'unica che spiega la segnalazione
        # dell'11 set 2026 sul Cielo.
        pkg = _pkg(rest, 1)
        names = rest[2:] or sorted(set(shipped.SHIPPED))
        report_source(pkg, names, only=rest[0])
    elif mode == "--copertura":
        pkg = _pkg(rest)
        names = rest[1:] or sorted(set(shipped.SHIPPED))
        for n in names:
            p = pkg / "flat" / f"{n}.svg"
            if p.exists():
                print(f"{n:26s}  {100*coverage(p):5.2f}% della scatola")
    else:
        sys.exit(__doc__)


if __name__ == "__main__":
    main()
