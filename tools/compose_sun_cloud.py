#!/usr/bin/env python3
"""Il disegno del «quasi sereno»: il sereno di Meteocons piu' una nuvoletta.

    python3 tools/compose_sun_cloud.py            # scrive, e stampa le misure
    python3 tools/compose_sun_cloud.py --controlla  # non scrive: verifica e basta

**Perche' esiste.** Il codice WMO 1 non ha un disegno in questa famiglia che si possa
usare. Quello che Meteocons chiama `mostly-clear` porta una nuvola larga 71,9 unita'
contro le 99,2 di `partly-cloudy`: il 72% della nuvola per un cielo che ha il 39% della
copertura (misurato su 1 680 ore, Fase 13). Per quello dall'11 set 2026 il codice 1
prendeva il sole pieno insieme allo 0 — onesto sulla parola, muto nella striscia oraria,
dove non ci sono parole e un'ora su sei mostrava un cielo sereno per un cielo coperto al
25%. Il committente ha chiesto la terza via (12 set 2026): il sereno **intatto**, piu'
una nuvoletta nell'angolo.

**Come.** Non si disegna niente di nuovo: si compone quel che l'importazione ha gia'
portato, e i percorsi dei pezzi restano **identici alla lettera** ai loro originali.

1. Il sole e' `clear-day` dentro il suo stesso gruppo di scala (0,92 sul perno della
   scatola): il disegno che esce e' il sereno, non un sereno somigliante. Di notte e'
   `clear-night`, anche lei alla sua scala (1,38).
2. La nuvola e' la silhouette di `cloudy` al **48,88%**, dentro un gruppo che la porta
   nell'angolo in basso a destra. Nello stile `line` non e' l'anello rimpicciolito ma la
   silhouette **ri-tracciata**: `strokeWidth` porta 4 / scala, cosi' il gruppo la
   riporta esattamente a 4, la linea della famiglia. Rimpicciolire l'anello avrebbe
   dimezzato anche il suo contorno (2,0 contro i 3,7 del sole) e la nuvoletta si sarebbe
   letta come uno sbaffo.
3. Il buco fra i due e' la maschera che `partly-cloudy-day` usa gia' — la stessa
   silhouette dilatata di 4 — scalata con la nuvola e poi scostata lungo le normali fino
   all'aria voluta. E' l'unico percorso che questo strumento calcola invece di copiare, e
   infatti e' l'unico che viene misurato riga per riga qui sotto.

**La taglia.** Le icone importate passano per `mc3scale`, che porta la media geometrica
dell'inchiostro a 0,69 della scatola. Questa no, e la deroga e' la scelta stessa: se la
composizione venisse normalizzata, il sole si rimpicciolirebbe e non sarebbe piu' quello
di `clear-day`. L'inchiostro esce a **0,711**, il 3% sopra la misura di famiglia e ben
sotto il tetto di 0,88 che `icon_ink.scale_of` impone agli altri. Per la stessa ragione
il file non porta nessun gruppo `mc3scale`: non c'e' niente da riportare a misura.

**Il nome e' suo** (`sun-one-cloud-day` / `-night`, che in Meteocons non esistono), e i
prefissi sono quelli della famiglia, cosi' `ChiaroIcons.styledRes` sceglie stile e fondo
come per tutti gli altri e i test che camminano i prefissi coprono anche questi. Una
ri-esecuzione di `import_meteocons_v3.py` non tocca questi file, ma se **cambia** i
disegni da cui vengono (un aggiornamento di Meteocons, un altro riancoraggio dei colori)
allora questo strumento va rifatto girare: `ComposedIconsTest` se ne accorge, perche'
confronta percorso per percorso il composto con le sue sorgenti.
"""
from __future__ import annotations

import argparse
import importlib.util
import math
import pathlib
import sys
import xml.etree.ElementTree as ET

HERE = pathlib.Path(__file__).resolve().parent
DRAWABLE = HERE.parent / "app" / "src" / "main" / "res" / "drawable"
KOTLIN = (HERE.parent / "app" / "src" / "main" / "kotlin" / "com" / "callbackdev"
          / "chiaro" / "ui" / "icons" / "ComposedIcons.kt")
A = "{http://schemas.android.com/apk/res/android}"
AAPT = "{http://schemas.android.com/aapt}"
NL = chr(10)


def _load(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


paths = _load("svg_paths", HERE / "svg_paths.py")
raster = _load("raster", HERE / "spike_mask_clip.py")
shipped = _load("shipped_icons", HERE / "shipped_icons.py")

fmt = paths.fmt

# --------------------------------------------------------------------- la ricetta

#: I due nomi composti, e da quale disegno del sereno viene ognuno.
COMPOSED = {"sun-one-cloud-day": "clear-day", "sun-one-cloud-night": "clear-night"}

#: Quanto della sua taglia tiene la nuvola di `cloudy`. Scelta guardando: a questa scala
#: e' larga 43,1 unita' con il contorno, cioe' il **43%** della nuvola di `partly-cloudy`
#: (99,2). Quella di `mostly-clear`, che e' il disegno scartato, ne vale il 72%.
CLOUD_SCALE = 0.4888

#: Dove cade il centro della nuvola. La silhouette di `cloudy` ha il proprio centro
#: esattamente in mezzo alla scatola, quindi questo e' anche il `translate` del gruppo,
#: meno 64. **Il valore in X non e' estetico**: piu' a sinistra il buco taglierebbe il
#: raggio di sud per il lungo e lascerebbe una scheggia di raggio accanto alla nuvola
#: (visto a 86,7, misurato da [ray_report]). A 91,5 il buco prende il raggio di sud-est
#: per intero e non tocca gli altri sette.
CLOUD_CENTRE = (91.5, 94.5)

#: Il contorno della nuvola **a schermo**, uguale a quello del sole di `clear-day`
#: (4 x 0,92 = 3,68): la nuvola e' un filo piu' marcata, com'e' gia' in tutta la famiglia
#: (in `partly-cloudy` la nuvola sta a 4,96 contro i 3,25 dei raggi).
CLOUD_STROKE = 4.0

#: L'aria fra l'inchiostro della nuvola e il bordo del buco. Meteocons ne lascia 2 alla
#: sorgente, cioe' 2,48 a schermo in `partly-cloudy`; qui il valore e' voluto e misurato,
#: non ereditato, perche' la nuvola e' scalata e la maschera con lei.
GAP = 2.45

#: Il dondolio della nuvola, copiato da `partly-cloudy`: stessa durata, stesso
#: interpolatore, stessa ampiezza in unita' di scatola. La maschera dondola con lei e il
#: disegno sotto resta fermo, che e' il trucco a due gruppi dell'importatore.
BOB = {"duration": 3000, "interpolator": "@interpolator/mc_ease_042_00_058_10",
       "amplitude": 3}

#: I set: prefisso fermo, prefisso animato, da quale set leggere i disegni del sole e
#: della nuvola, e come si chiama la faccia.
SETS = [
    ("mc3_", "mc3a_", "mc3_", "line", "chiaro"),
    ("mc3n_", "mc3an_", "mc3n_", "line", "scuro"),
    ("mc3f_", "mc3fa_", "mc3f_", "flat", "chiaro"),
    ("mc3fn_", "mc3fan_", "mc3fn_", "flat", "scuro"),
]

#: Da dove viene la **geometria** della nuvola: lo stile pieno la disegna come silhouette
#: piena, ed e' quella che serve sia da riempire (flat) sia da tracciare (line). Il
#: colore invece lo da' il set stesso, perche' ogni fondo ha il suo.
SILHOUETTE_OF = {"chiaro": "mc3f_cloudy", "scuro": "mc3fn_cloudy"}
GROUND_OF = {"mc3_": "chiaro", "mc3n_": "scuro", "mc3f_": "chiaro", "mc3fn_": "scuro"}

HEADER = (
    "<!-- Composto da tools/compose_sun_cloud.py dai disegni gia' importati di" + NL +
    "     Meteocons v3 (github.com/basmilius/meteocons), MIT, (c) Bas Milius —" + NL +
    "     licenses/Meteocons-MIT.txt. Il sole e' clear-{part} alla sua scala, la nuvola" + NL +
    "     e' cloudy al {pct}% ri-tracciata. Non modificare a mano: ri-eseguire lo" + NL +
    "     strumento E' la composizione. -->"
)


# ------------------------------------------------------------------ leggere il repo

def vector_of(stem: str) -> ET.Element:
    """Il `<vector>` di un drawable, anche quando e' dentro un `<animated-vector>`."""
    root = ET.parse(DRAWABLE / f"{stem}.xml").getroot()
    return root if root.tag == "vector" else root.find(f"{AAPT}attr/vector")


def scale_group(stem: str) -> ET.Element:
    g = vector_of(stem).find("group")
    if g is None or g.get(A + "name") != "mc3scale":
        sys.exit(f"{stem}: nessun gruppo mc3scale — l'importatore ha cambiato forma?")
    return g


def attrs(el: ET.Element) -> dict:
    return {k.split("}")[-1]: v for k, v in el.attrib.items()}


def sun_of(prefix: str, night: bool) -> dict:
    """I pezzi del sereno **come stanno nel file**, mai riscritti: la scala del suo
    gruppo, il nome e il perno del gruppo interno, i percorsi.

    Di giorno sono il disco e il gruppo dei raggi; di notte il gruppo della luna, che ha
    il perno dell'illustratore e non quello della scatola.
    """
    g = scale_group(f"{prefix}clear_{'night' if night else 'day'}")
    inner = g.find("group")
    out = {"scale": float(g.get(A + "scaleX", 1)),
           "group": inner.get(A + "name"),
           "pivot": (float(inner.get(A + "pivotX", 64)),
                     float(inner.get(A + "pivotY", 64)))}
    if night:
        out["moon"] = attrs(inner.find("path"))
    else:
        out["disc"] = attrs(g.find("path"))
        out["rays"] = attrs(inner.find("path"))
    return out


def cloud_of(prefix: str):
    """(percorso della silhouette, colore dell'inchiostro della nuvola in questo set)."""
    silhouette = attrs(
        scale_group(SILHOUETTE_OF[GROUND_OF[prefix]]).find("group").find("path"))
    colour = attrs(scale_group(f"{prefix}cloudy").find("group").find("path"))["fillColor"]
    return silhouette["pathData"], colour


def mask_of_partly_cloudy() -> str:
    """Il sottopercorso «nuvola» della maschera di `partly-cloudy-day`: la silhouette
    dilatata di 4 (verificato qui sotto, non dato per buono)."""
    g = scale_group("mc3_partly_cloudy_day")
    d = g.find("group").find("clip-path").get(A + "pathData")
    return paths.emit_path(paths.parse_segments(d)[1:])


# ------------------------------------------------------------------- la geometria

def matrix(scale: float, tx: float, ty: float):
    """La stessa trasformazione che VectorDrawable applica a un gruppo con questo
    `scale` sul perno della scatola e questo `translate`."""
    return (scale, 0.0, 0.0, scale, 64 - scale * 64 + tx, 64 - scale * 64 + ty)


def transformed(d: str, m) -> str:
    return paths.emit_path(paths.transform_path(paths.parse_segments(d), m))


def bbox(d: str):
    xs = [p[0] for sub in raster.flatten(d) for p in sub]
    ys = [p[1] for sub in raster.flatten(d) for p in sub]
    return min(xs), min(ys), max(xs), max(ys)


def _unit(v):
    n = math.hypot(*v)
    return (v[0] / n, v[1] / n) if n > 1e-9 else (0.0, 0.0)


def offset_path(d: str, by: float) -> str:
    """Lo stesso contorno spostato di `by` verso l'esterno.

    Ogni punto di ancoraggio va lungo la propria normale e i punti di controllo lo
    seguono. Su una curva liscia e per uno scostamento piccolo rispetto al raggio di
    curvatura sbaglia di frazioni di unita', e **quanto** sbaglia lo dice [gap_report]
    invece di lasciarlo credere: sul contorno gia' dilatato di Meteocons, per 2,5 unita'
    chieste, il minimo misurato e' 1,8 e sta nell'angolo in basso a destra della nuvola,
    dove il sole non arriva.
    """
    out = []
    for start, segs, closed in paths.parse_segments(d):
        anchors = [start] + [s[-1] for s in segs]

        def before(i):
            if i > 0:
                s = segs[i - 1]
                return s[2] if s[0] == "C" else (s[1] if s[0] == "Q" else anchors[i - 1])
            return anchors[-2] if closed else anchors[0]

        def after(i):
            if i < len(segs):
                s = segs[i]
                return s[1] if s[0] in ("C", "Q") else s[-1]
            return anchors[0] if closed else anchors[-1]

        normals = []
        for i, p in enumerate(anchors):
            back = _unit((p[0] - before(i)[0], p[1] - before(i)[1]))
            fwd = _unit((after(i)[0] - p[0], after(i)[1] - p[1]))
            t = _unit((back[0] + fwd[0], back[1] + fwd[1])) or fwd
            normals.append((t[1], -t[0]))
        area = raster.signed_area(
            raster.flatten(paths.emit_path([[start, segs, closed]]))[0])
        sign = 1.0 if area > 0 else -1.0

        def moved(p, k):
            return (p[0] + sign * by * normals[k][0], p[1] + sign * by * normals[k][1])

        new = []
        for i, s in enumerate(segs):
            if s[0] == "L":
                new.append(("L", moved(s[1], i + 1)))
            elif s[0] == "C":
                new.append(("C", moved(s[1], i), moved(s[2], i + 1), moved(s[3], i + 1)))
            elif s[0] == "Q":
                new.append(("Q", moved(s[1], i), moved(s[2], i + 1)))
            else:
                sys.exit("un arco nel contorno della nuvola: lo scostamento non lo fa")

        out.append([moved(start, 0), new, closed])
    grown = paths.emit_path(out)
    if abs(raster.signed_area(raster.flatten(grown)[0])) < abs(
            raster.signed_area(raster.flatten(d)[0])):
        sys.exit("lo scostamento ha rimpicciolito il contorno: normale girata")
    return grown


def cloud_matrix():
    return matrix(CLOUD_SCALE, CLOUD_CENTRE[0] - 64, CLOUD_CENTRE[1] - 64)


def hole_path() -> str:
    """Il buco, in coordinate della scatola: la maschera di `partly-cloudy` portata dove
    sta la nuvola e gonfiata fino a lasciare [GAP] oltre il contorno."""
    scaled = transformed(mask_of_partly_cloudy(), cloud_matrix())
    already = 4 * CLOUD_SCALE          # la dilatazione della sorgente, scalata
    return offset_path(scaled, CLOUD_STROKE / 2 + GAP - already)


def clip_path() -> str:
    """Tutta la tela **meno** il buco, con il verso che NON-ZERO legge come la maschera
    evenodd da cui viene: la conversione e' quella dell'importatore, che ri-rasterizza e
    si rifiuta di restituire un pixel di differenza."""
    return paths.mask_to_clip("M128,0 L0,0 L0,128 L128,128 Z " + hole_path())


# -------------------------------------------------------------------- le misure

def _inside(point, subs, rule: str) -> bool:
    x, y = point
    if rule == "evenodd":
        return raster.crossings(x, y, subs) % 2 == 1
    return raster.winding(x, y, subs) != 0


def gap_report(prefix="mc3_"):
    """L'aria misurata fra l'inchiostro della nuvola e il bordo del buco, **dove il sole
    arriva davvero**: il resto del giro non lo incontra nessuno."""
    silhouette, _ = cloud_of(prefix)
    edge = [p for sub in raster.flatten(transformed(silhouette, cloud_matrix()))
            for p in sub]
    half = CLOUD_STROKE / 2
    sun = sun_of(prefix, night=False)
    scale = sun["scale"]
    reach = (bbox(sun["rays"]["pathData"])[2] - 64) * scale + 1
    near = [(x, y) for sub in raster.flatten(hole_path()) for x, y in sub
            if math.hypot(x - 64, y - 64) <= reach]
    d = sorted(min(math.hypot(x - ex, y - ey) for ex, ey in edge) - half
               for x, y in near)
    return d[0], d[len(d) // 2], d[-1], len(near)


def ray_report(prefix="mc3_"):
    """Per ogni raggio del sole: quanto ne resta fuori dal buco, e **quanto e' largo**
    quel che resta.

    E' il controllo che ha spostato la nuvola di 3,2 unita' (12 set 2026). Un raggio
    tagliato di traverso e' il disegno giusto — la nuvola gli passa davanti. Un raggio
    tagliato **per il lungo** lascia una scheggia larga mezzo tratto, che a schermo si
    legge come un difetto del disegno e non come una nuvola.
    """
    sun = sun_of(prefix, night=False)
    scale = sun["scale"]
    m = matrix(scale, 0, 0)
    hole = raster.flatten(hole_path())
    out = []
    for sub in paths.parse_segments(transformed(sun["rays"]["pathData"], m)):
        d = paths.emit_path([sub])
        poly = raster.flatten(d)
        x0, y0, x1, y1 = bbox(d)
        step = 0.25
        inside, kept = 0, []
        y = y0
        while y <= y1:
            x = x0
            while x <= x1:
                if _inside((x, y), poly, "evenodd"):
                    inside += 1
                    if not _inside((x, y), hole, "nonzero"):
                        kept.append((x, y))
                x += step
            y += step
        if not inside:
            continue
        # la larghezza di quel che resta, misurata di traverso al raggio
        angle = math.atan2((y0 + y1) / 2 - 64, (x0 + x1) / 2 - 64)
        across = [-math.sin(angle) * x + math.cos(angle) * y for x, y in kept]
        width = (max(across) - min(across)) if kept else 0.0
        out.append({"kept": len(kept) / inside, "width": width,
                    "centre": ((x0 + x1) / 2, (y0 + y1) / 2)})
    return out


def ink_box(prefix="mc3_", night=False):
    """Il riquadro dell'inchiostro composto, contorni compresi."""
    sun = sun_of(prefix, night)
    scale = sun["scale"]
    m = matrix(scale, 0, 0)
    boxes = []
    for key in ("disc", "rays", "moon"):
        if key not in sun:
            continue
        p = sun[key]
        grow = float(p.get("strokeWidth", 0)) / 2 * scale
        b = bbox(transformed(p["pathData"], m))
        boxes.append((b[0] - grow, b[1] - grow, b[2] + grow, b[3] + grow))
    silhouette, _ = cloud_of(prefix)
    b = bbox(transformed(silhouette, cloud_matrix()))
    half = CLOUD_STROKE / 2
    boxes.append((b[0] - half, b[1] - half, b[2] + half, b[3] + half))
    return (min(b[0] for b in boxes), min(b[1] for b in boxes),
            max(b[2] for b in boxes), max(b[3] for b in boxes))


# --------------------------------------------------------------------- scrivere

def path_xml(p: dict, indent: str) -> str:
    order = ["pathData", "fillColor", "fillType", "strokeColor", "strokeWidth",
             "strokeLineCap", "strokeLineJoin"]
    bits = [f'android:{k}="{p[k]}"' for k in order if k in p]
    return indent + "<path " + (NL + indent + "      ").join(bits) + "/>"


def group_open(name: str, indent: str, scale: float = None, pivot=(64, 64),
               translate=(0, 0)) -> str:
    bits = [f'android:name="{name}"']
    if scale is not None:
        bits.append(f'android:scaleX="{fmt(scale)}" android:scaleY="{fmt(scale)}"')
    if translate != (0, 0):
        bits.append(f'android:translateX="{fmt(translate[0])}" '
                    f'android:translateY="{fmt(translate[1])}"')
    bits.append(f'android:pivotX="{fmt(pivot[0])}" android:pivotY="{fmt(pivot[1])}"')
    return indent + "<group " + " ".join(bits) + ">"


def cloud_element(prefix: str, face: str) -> dict:
    silhouette, colour = cloud_of(prefix)
    if face == "flat":
        return {"pathData": silhouette, "fillColor": colour}
    return {"pathData": silhouette, "strokeColor": colour,
            # il gruppo la scala, quindi il tratto qui dentro e' 4 diviso quella scala
            "strokeWidth": fmt(CLOUD_STROKE / CLOUD_SCALE),
            "strokeLineCap": "round", "strokeLineJoin": "round"}


def drawing(prefix: str, face: str, night: bool) -> list[str]:
    """Il corpo del vettore: il sole ritagliato dal buco, poi la nuvola."""
    sun = sun_of(prefix, night)
    scale = sun["scale"]
    lines = [
        '    <group android:name="g1maskmove">',
        f'        <clip-path android:pathData="{clip_path()}"/>',
        '        <group android:name="g2maskhold">',
        group_open("g3sun", " " * 12, scale=scale),
    ]
    if night:
        lines.append(group_open("g4moon", " " * 16, pivot=sun["pivot"]))
        lines.append(path_xml(sun["moon"], " " * 20))
        lines.append(" " * 16 + "</group>")
    else:
        lines.append(path_xml(sun["disc"], " " * 16))
        lines.append(group_open("g4rays", " " * 16, pivot=sun["pivot"]))
        lines.append(path_xml(sun["rays"], " " * 20))
        lines.append(" " * 16 + "</group>")
    lines += [
        " " * 12 + "</group>",
        "        </group>",
        "    </group>",
        group_open("g5cloud", " " * 4, scale=CLOUD_SCALE,
                   translate=(CLOUD_CENTRE[0] - 64, CLOUD_CENTRE[1] - 64)),
        path_xml(cloud_element(prefix, face), " " * 8),
        "    </group>",
    ]
    return lines


def still_xml(prefix: str, face: str, night: bool) -> str:
    body = NL.join(drawing(prefix, face, night))
    return (HEADER.format(part="night" if night else "day",
                          pct=fmt(CLOUD_SCALE * 100)) + NL +
            '<vector xmlns:android="http://schemas.android.com/apk/res/android"' + NL +
            '    android:width="24dp"' + NL +
            '    android:height="24dp"' + NL +
            '    android:viewportWidth="128"' + NL +
            '    android:viewportHeight="128">' + NL +
            body + NL + "</vector>" + NL)


def bob_target(name: str, sign: int, indent: str) -> list[str]:
    """Il dondolio su un gruppo, con il segno che serve: la maschera e la nuvola salgono,
    il disegno sotto la maschera scende della stessa quantita' e resta fermo."""
    a = BOB["amplitude"] * sign
    return [
        f'{indent}<target android:name="{name}">',
        f'{indent}    <aapt:attr name="android:animation">',
        f'{indent}        <objectAnimator',
        f'{indent}            android:duration="{BOB["duration"]}"',
        f'{indent}            android:repeatCount="infinite"',
        f'{indent}            android:valueType="floatType"',
        f'{indent}            android:interpolator="{BOB["interpolator"]}"',
        f'{indent}            android:propertyName="translateY">',
        f'{indent}            <propertyValuesHolder android:propertyName="translateY">',
        f'{indent}                <keyframe android:fraction="0" android:value="0"/>',
        f'{indent}                <keyframe android:fraction="0.5" android:value="{-a}"/>',
        f'{indent}                <keyframe android:fraction="1" android:value="0"/>',
        f'{indent}            </propertyValuesHolder>',
        f'{indent}        </objectAnimator>',
        f'{indent}    </aapt:attr>',
        f'{indent}</target>',
    ]


def sun_target(prefix: str, night: bool) -> list[str]:
    """L'animazione del sereno, copiata **alla lettera** dal suo gemello animato: i raggi
    girano, la luna oscilla.

    Si ritaglia il blocco dal file invece di riscriverlo, per la stessa ragione per cui i
    percorsi si copiano: se l'importatore cambia la durata o l'interpolatore, il composto
    se lo porta dietro al primo giro di questo strumento, e nel frattempo i due file non
    si contraddicono a meta'.
    """
    part = "night" if night else "day"
    moving = {"mc3_": "mc3a_", "mc3n_": "mc3an_",
              "mc3f_": "mc3fa_", "mc3fn_": "mc3fan_"}[prefix]
    source = DRAWABLE / f"{moving}clear_{part}.xml"
    text = source.read_text(encoding="utf8")
    wanted = sun_of(prefix, night)["group"]
    head = f'<target android:name="{wanted}">'
    if head not in text:
        sys.exit(f"{source.name}: non anima {wanted} — il sereno ha cambiato forma")
    start = text.index(head)
    block = text[start:text.index("</target>", start) + len("</target>")]
    block = block.replace(head, f'<target android:name="{"g4moon" if night else "g4rays"}">')
    # il ritaglio parte dopo il rientro della riga <target>: lo rimette solo li', le
    # righe seguenti portano gia' quello della sorgente.
    lines = block.splitlines()
    return ["    " + lines[0]] + lines[1:]


def animated_xml(prefix: str, face: str, night: bool) -> str:
    body = [" " * 8 + line for line in drawing(prefix, face, night)]
    lines = [
        HEADER.format(part="night" if night else "day", pct=fmt(CLOUD_SCALE * 100)),
        '<animated-vector xmlns:android="http://schemas.android.com/apk/res/android"',
        '    xmlns:aapt="http://schemas.android.com/aapt">',
        '    <aapt:attr name="android:drawable">',
        '        <vector',
        '            android:width="24dp"',
        '            android:height="24dp"',
        '            android:viewportWidth="128"',
        '            android:viewportHeight="128">',
        *body,
        '        </vector>',
        '    </aapt:attr>',
        *sun_target(prefix, night),
        *bob_target("g1maskmove", +1, "    "),
        *bob_target("g2maskhold", -1, "    "),
        *bob_target("g5cloud", +1, "    "),
        '</animated-vector>',
    ]
    return NL.join(lines) + NL


# ---------------------------------------------------------------------- il Kotlin

KOTLIN_HEAD = '''package com.callbackdev.chiaro.ui.icons

import androidx.annotation.DrawableRes
import com.callbackdev.chiaro.R

/**
 * I disegni che **questo repo compone** dai pezzi di Meteocons, invece di importarli.
 *
 * **File generato da `tools/compose_sun_cloud.py` — non si modifica a mano.** Ha la
 * stessa forma di [MeteoconsSets], perche' `ChiaroIcons` lo interroga allo stesso modo:
 * la chiave e' l'id del set line su fondo chiaro, e le tabelle danno le altre tre facce
 * e i gemelli animati. Sta in un file suo e non dentro [MeteoconsSets] per la ragione
 * che rende sicura tutta la faccenda: quel file lo riscrive l'importatore a ogni giro,
 * questo no.
 *
 * Oggi ce n'e' uno solo, il «quasi sereno»: il sereno di Meteocons intatto piu' una
 * nuvoletta nell'angolo, perche' il disegno che la libreria chiama `mostly-clear`
 * porta il 72% della nuvola del «poco nuvoloso» per un cielo che ne ha il 39% di
 * copertura. Il perche' per esteso, con le misure, sta in testa allo strumento.
 */
internal object ComposedIcons {
'''


def kotlin_file() -> str:
    def res(prefix, name):
        return f"R.drawable.{prefix}{name.replace('-', '_')}"

    names = list(COMPOSED)
    body = [KOTLIN_HEAD]
    for title, comment, prefix in (
        ("lineDarkOf", "line, fondo scuro.", "mc3n_"),
        ("flatOf", "flat, fondo chiaro.", "mc3f_"),
        ("flatDarkOf", "flat, fondo scuro.", "mc3fn_"),
    ):
        body.append(f"    /** {comment} */")
        body.append(f"    val {title}: Map<Int, Int> = mapOf(")
        for n in names:
            body.append(f"        {res('mc3_', n)} to {res(prefix, n)},")
        body.append("    )" + NL)
    body.append("    /** I quattro gemelli animati, nello stesso ordine dei set fermi. */")
    body.append("    val movingOf: Map<Int, MeteoconsSets.Moving> = mapOf(")
    for n in names:
        body.append(f"        {res('mc3_', n)} to MeteoconsSets.Moving(")
        body.append(f"            {res('mc3a_', n)}, {res('mc3an_', n)},")
        body.append(f"            {res('mc3fa_', n)}, {res('mc3fan_', n)}")
        body.append("        ),")
    body.append("    )" + NL)
    body.append("    /** Il disegno composto, per nome: la lista che i test camminano. */")
    body.append("    val byName: Map<String, Int> = mapOf(")
    for n in names:
        body.append(f'        "{n}" to {res("mc3_", n)},')
    body.append("    )" + NL)
    body.append("    /** Il disegno del «quasi sereno», di giorno e di notte. */")
    body.append("    @DrawableRes")
    body.append("    val mostlyClearDay: Int = R.drawable.mc3_sun_one_cloud_day" + NL)
    body.append("    @DrawableRes")
    body.append("    val mostlyClearNight: Int = R.drawable.mc3_sun_one_cloud_night")
    body.append("}")
    return NL.join(body) + NL


# ------------------------------------------------------------------------ il giro

def check_sources():
    """Le due cose che questo strumento da' per vere sulla sorgente, verificate."""
    problems = []
    silhouette = cloud_of("mc3_")[0]
    dilated = mask_of_partly_cloudy()
    edge = [p for sub in raster.flatten(silhouette) for p in sub]
    d = sorted(min(math.hypot(x - ex, y - ey) for ex, ey in edge)
               for sub in raster.flatten(dilated) for x, y in sub)
    if not 3.9 <= d[0] <= 4.1 or not 3.9 <= d[-1] <= 4.2:
        problems.append(f"la maschera di partly-cloudy non e' piu' la silhouette + 4: "
                        f"misurato da {d[0]:.2f} a {d[-1]:.2f}")
    if cloud_of("mc3_")[0] != cloud_of("mc3n_")[0]:
        problems.append("le due silhouette della nuvola (chiara e scura) non coincidono")
    for prefix, _, _, _, _ in SETS:
        for night in (False, True):
            if abs(sun_of(prefix, night)["scale"] - sun_of("mc3_", night)["scale"]) > 1e-6:
                problems.append(f"{prefix}clear_* porta una scala diversa da mc3_clear_*")
    return problems


def report() -> str:
    lines = []
    gmin, gmed, gmax, points = gap_report()
    lines.append(f"aria sole-nuvola, dove il sole arriva: {gmin:.2f} / {gmed:.2f} / "
                 f"{gmax:.2f} (min, mediana, max su {points} punti); "
                 f"Meteocons ne lascia 2,48 in partly-cloudy")
    for i, ray in enumerate(ray_report(), start=1):
        if ray["kept"] > 0.99:
            state = "intero"
        elif ray["kept"] < 0.01:
            state = "dietro la nuvola"
        else:
            state = f"tagliato, ne resta il {ray['kept'] * 100:.0f}% largo {ray['width']:.2f}"
        lines.append(f"raggio {i} ({ray['centre'][0]:.0f},{ray['centre'][1]:.0f}): {state}")
    for night in (False, True):
        b = ink_box(night=night)
        w, h = b[2] - b[0], b[3] - b[1]
        lines.append(f"inchiostro {'notte ' if night else 'giorno'}: "
                     f"{w:.1f} x {h:.1f}, media geometrica "
                     f"{math.sqrt(w * h) / 128:.3f} della scatola (la famiglia sta a 0,690)")
    silhouette, _ = cloud_of("mc3_")
    b = bbox(transformed(silhouette, cloud_matrix()))
    lines.append(f"nuvola: larga {b[2] - b[0] + CLOUD_STROKE:.1f} con il contorno, "
                 f"il {(b[2] - b[0] + CLOUD_STROKE) / 99.2 * 100:.0f}% di quella del "
                 f"poco nuvoloso")
    return NL.join(lines)


def slivers() -> list[str]:
    """I raggi che il buco taglia per il lungo: il difetto che sposta la nuvola."""
    out = []
    for i, ray in enumerate(ray_report(), start=1):
        if 0.02 < ray["kept"] < 0.99 and ray["width"] < 0.6 * 3.68:
            out.append(f"il raggio {i} resta largo {ray['width']:.2f} invece di 3,68: "
                       f"il buco lo taglia per il lungo, sposta CLOUD_CENTRE")
    return out


def main() -> None:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--controlla", action="store_true",
                    help="verifica e misura senza scrivere niente")
    args = ap.parse_args()

    problems = check_sources() + slivers()
    if problems:
        sys.exit("la composizione non si scrive:" + NL + NL.join(f"  - {p}" for p in problems))

    written = []
    for still, moving, source, face, ground in SETS:
        for night, name in ((False, "sun_one_cloud_day"), (True, "sun_one_cloud_night")):
            files = [(f"{still}{name}.xml", still_xml(source, face, night)),
                     (f"{moving}{name}.xml", animated_xml(source, face, night))]
            for filename, text in files:
                if not args.controlla:
                    (DRAWABLE / filename).write_text(text, encoding="utf8")
                written.append(filename)
    if not args.controlla:
        KOTLIN.write_text(kotlin_file(), encoding="utf8")

    verb = "verificati" if args.controlla else "scritti"
    print(f"{verb} {len(written)} drawable"
          + ("" if args.controlla else f" e {KOTLIN.name}"))
    print(report())


if __name__ == "__main__":
    main()
