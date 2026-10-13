#!/usr/bin/env python3
"""I disegni del «quasi sereno» e del «poco nuvoloso»: il sereno di Meteocons piu' una nuvola.

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

**Il «poco nuvoloso» e' la seconda composizione** (13 ott 2026, revisione dei widget). Il
codice 2 prendeva `partly-cloudy` di Meteocons: una nuvola piena con uno spicchio di sole
nell'angolo. Accanto al codice 1 la scala non andava a gradini: da 1 a 2 il sole passava da
intero a spicchio e cambiava angolo, la nuvola da piccola a piena, e 2 e 3 si confondevano
nella striscia oraria, dove lo spicchio e' di pochi pixel. Ora il codice 2 e' lo stesso sole
intero, nello stesso posto, con la nuvola al **78%** che ne copre circa un terzo: la scala
dice nuvola 0 → 49% → 78% → 100%, sole intero → intero → coperto per un terzo → nessuno.
Ogni composizione ha la sua taglia e i suoi due centri, uno per il giorno e uno per la notte
([RECIPES]), perche' la falce della luna ha le punte dove il sole non ha niente.

**Nessuna scheggia** (13 ott 2026). Oltre ai raggi tagliati per il lungo ([slivers]) lo
strumento rifiuta ora una punta di raggio rimasta nell'aria fra il buco e la nuvola ([tips]):
il «quasi sereno» ne lasciava una nello stile pieno, visibile in grande, e per questo la sua
nuvoletta e' salita di mezza unita'. E di notte rifiuta un pezzo di falce staccato dal resto
([moon_fragments]).

**Gli acquazzoni sono la terza composizione** (13 ott 2026): `sun-half-cloud-rain-*` e
`sun-half-cloud-snow-*`, il «poco nuvoloso» con le gocce o i fiocchi di
`partly-cloudy-*-rain/-snow` appesi al pavimento della nuvola ([FALLS], [fall_matrix]). Nello
stile `line` la nuvola perde il pavimento come nei disegni di pioggia di Meteocons ([floorless]),
e la caduta animata non esce dalla scatola piu' della pioggia che la famiglia gia' disegna
([fall_room]).

**I nomi sono suoi** (`sun-one-cloud-*`, `sun-half-cloud-*`, che in Meteocons non esistono), e i
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

#: Le composizioni. Per ognuna: il nome, il codice WMO che la disegna, quanto della sua
#: taglia tiene la nuvola di `cloudy`, e dove cade il centro della nuvola di giorno e di
#: notte. La silhouette di `cloudy` ha il proprio centro esattamente in mezzo alla scatola,
#: quindi il centro e' anche il `translate` del gruppo, meno 64.
#:
#: **«Quasi sereno», 0,4888.** A questa scala la nuvola e' larga 43,1 unita' con il
#: contorno, il **43%** di quella di `partly-cloudy` (99,2); `mostly-clear`, il disegno
#: scartato, ne vale il 72%. **Il centro non e' estetico**: piu' a sinistra di 91,5 il buco
#: taglierebbe il raggio di sud per il lungo (visto a 86,7, misurato da [ray_report]); a
#: 91,5 prende il raggio di sud-est per intero e non tocca gli altri sette. In Y era 94,5
#: fino al 13 ott 2026: li' la punta del raggio di sud-est restava nell'aria intorno alla
#: nuvola nello stile pieno, e 94 la nasconde ([tips]).
#:
#: **«Poco nuvoloso», 0,78** (13 ott 2026): la nuvola copre il 35% del disco, nasconde per
#: intero i raggi di sud e di sud-est e lascia interi gli altri sei, nessuno tagliato a
#: meta'. Di giorno in (77, 86); di notte in (84, 80), l'unico posto vicino in cui la punta
#: bassa della falce sparisce sotto la nuvola invece di restarne fuori come una scheggia
#: ([moon_fragments]). Scelti con una ricerca a griglia sui tre controlli, poi guardati.
#:
#: **Gli acquazzoni** (13 ott 2026) sono il «poco nuvoloso» con quel che cade: stessa taglia,
#: stessi centri, e sotto la nuvola le gocce o i fiocchi di `partly-cloudy-*-rain/-snow` di
#: Meteocons ([FALLS]). Prima prendevano quei disegni interi, cioe' lo spicchio di sole dietro
#: la nuvola piena che il codice 2 ha appena lasciato: accanto alla nuova scala l'acquazzone
#: si leggeva come un cielo diverso, non come lo stesso cielo con la pioggia.
HALF_CLOUD = {"scale": 0.78, "centre": {"day": (77.0, 86.0), "night": (84.0, 80.0)}}

RECIPES = [
    {"name": "sun-one-cloud", "wmo": 1, "scale": 0.4888,
     "centre": {"day": (91.5, 94.0), "night": (91.5, 94.0)}},
    {"name": "sun-half-cloud", "wmo": 2, **HALF_CLOUD},
    {"name": "sun-half-cloud-rain", "wmo": 80, "fall": "rain", **HALF_CLOUD},
    {"name": "sun-half-cloud-snow", "wmo": 85, "fall": "snow", **HALF_CLOUD},
]

#: Da dove vengono le gocce e i fiocchi: i gruppi che in `partly-cloudy-{parte}-{fall}`
#: seguono la nuvola, copiati con i loro percorsi e le loro animazioni **alla lettera**.
#: Stanno, in quel disegno, alla misura della sua nuvola, che e' la silhouette di `cloudy`
#: alle stesse coordinate (verificato da [check_sources]); quindi qui prendono la
#: trasformazione della nuvola, e cadono dove cadevano rispetto a lei. Le gocce sono un
#: tratto e vengono ri-tracciate come la nuvola, a [CLOUD_STROKE] a schermo; i fiocchi sono
#: pieni e si rimpiccioliscono con lei.
FALLS = {"rain": "partly_cloudy_{part}_rain", "snow": "partly_cloudy_{part}_snow"}

#: La misura delle gocce e dei fiocchi rispetto alla scatola ([fall_matrix]): il 95% di
#: quella della sorgente prima della sua `mc3scale`, cioe' una goccia lunga 11,4 con il
#: tratto a 4 come la nuvola. A 1,0 la caduta finiva a 133,5, oltre i 132,8 della pioggia
#: che la famiglia gia' disegna ([fall_room]); 0,95 e' il passo tondo piu' grande che ci sta.
FALL_SCALE = 0.95

#: Il pavimento della silhouette di `cloudy`, in coordinate della sorgente: il centro del
#: tratto dritto in basso, da (37,953, 89) a (90,977, 89). Le gocce ci sono appese.
FLOOR = ((37.953 + 90.977) / 2, 89.0)

#: La composizione in corso: [use] le imposta, e ogni funzione qui sotto le legge. Sono
#: globali perche' lo strumento e' nato con una composizione sola, e passarle a mano a
#: tutte le funzioni di geometria e di misura non le renderebbe piu' chiare.
CLOUD_SCALE = RECIPES[0]["scale"]
CLOUD_CENTRE = RECIPES[0]["centre"]["day"]
FALL = None


def use(recipe: dict, night: bool) -> None:
    """Prepara la geometria per [recipe], di giorno o di notte."""
    global CLOUD_SCALE, CLOUD_CENTRE, FALL
    CLOUD_SCALE = recipe["scale"]
    CLOUD_CENTRE = recipe["centre"]["night" if night else "day"]
    FALL = recipe.get("fall")


def file_stem(recipe: dict, night: bool) -> str:
    return recipe["name"].replace("-", "_") + ("_night" if night else "_day")

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
FALL_HEADER = (
    "<!-- {what} sono quelli di partly-cloudy-{part}-{fall}, alla misura della nuvola. -->"
)


def header(night: bool) -> str:
    part = "night" if night else "day"
    text = HEADER.format(part=part, pct=fmt(CLOUD_SCALE * 100))
    if FALL:
        what = "Le gocce" if FALL == "rain" else "I fiocchi"
        text += NL + FALL_HEADER.format(what=what, part=part, fall=FALL)
    return text


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


def fall_stem(prefix: str, night: bool) -> str:
    return prefix + FALLS[FALL].format(part="night" if night else "day")


def fall_of(prefix: str, night: bool) -> list[tuple[str, dict]]:
    """Le gocce o i fiocchi **come stanno nel file**: i gruppi che seguono la nuvola, ognuno
    con il suo nome e il suo percorso (che porta un nome anche lui, perche' l'animazione ne
    muove l'alfa)."""
    g = scale_group(fall_stem(prefix, night))
    kids = list(g)
    names = [k.get(A + "name") for k in kids]
    if "g4clouds" not in names:
        sys.exit(f"{fall_stem(prefix, night)}: nessun g4clouds — il disegno ha cambiato forma?")
    out = []
    for k in kids[names.index("g4clouds") + 1:]:
        if k.tag != "group" or len(k) != 1 or k[0].tag != "path" or set(attrs(k)) != {"name"}:
            sys.exit(f"{fall_stem(prefix, night)}: {k.get(A + 'name')} non e' un gruppo con "
                     f"un percorso solo — il disegno ha cambiato forma?")
        out.append((k.get(A + "name"), attrs(k[0])))
    return out


def fall_cloud(prefix: str, night: bool) -> str:
    """La nuvola del disegno da cui vengono le gocce: deve essere la silhouette di `cloudy`
    alle stesse coordinate, o le gocce non cadrebbero da questa nuvola."""
    g = scale_group(fall_stem(prefix, night))
    clouds = [k for k in g if k.get(A + "name") == "g4clouds"][0]
    return clouds.find("path").get(A + "pathData")


def renamed(name: str) -> str:
    """I nomi dei gruppi che cadono, con un prefisso: i numeri dell'importatore (g5, g6...)
    sono gli stessi che questo strumento usa per la nuvola."""
    return "fall_" + name


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


def ray_report(prefix="mc3_", step=0.25):
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
    if FALL:
        for _, p in fall_of(prefix, night):
            boxes.append(fall_box(p, fall_matrix(), CLOUD_STROKE))
    return (min(b[0] for b in boxes), min(b[1] for b in boxes),
            max(b[2] for b in boxes), max(b[3] for b in boxes))


def fall_matrix():
    """Dove cadono le gocce: la loro sorgente le appende al pavimento della nuvola, e qui
    il pavimento e' quello della nuvola composta. Alla misura [FALL_SCALE] della scatola e
    non a quella della nuvola: rimpicciolite con lei erano lunghe 9 unita' a schermo, e
    nella striscia oraria l'acquazzone non si distingueva piu' dal «poco nuvoloso»."""
    (fx, fy) = FLOOR
    m = cloud_matrix()
    ax, ay = m[0] * fx + m[4], m[3] * fy + m[5]
    return (FALL_SCALE, 0.0, 0.0, FALL_SCALE, ax - FALL_SCALE * fx, ay - FALL_SCALE * fy)


def fall_box(p: dict, m, stroke: float, drop: float = 0.0):
    """Il riquadro a schermo di una goccia o di un fiocco nella trasformazione `m`,
    contorno compreso (`stroke` a schermo), spostato in basso di `drop` unita' della
    sorgente: quanto l'animazione lo fa cadere."""
    d = p["pathData"]
    if "strokeWidth" in p:
        # una goccia e' un segmento: bbox() non ne vede l'area, quindi si prendono i punti
        xs = [float(v) for v in d.replace("M", " ").replace("L", " ").replace(",", " ").split()]
        pts = list(zip(xs[0::2], xs[1::2]))
        grow = stroke / 2
    else:
        pts = [pt for sub in raster.flatten(d) for pt in sub]
        grow = 0.0
    pts = [(m[0] * x + m[4], m[3] * (y + drop) + m[5]) for x, y in pts]
    return (min(x for x, _ in pts) - grow, min(y for _, y in pts) - grow,
            max(x for x, _ in pts) + grow, max(y for _, y in pts) + grow)


#: Quanto cadono le gocce e i fiocchi nelle animazioni di Meteocons, in unita' della
#: sorgente (il `translateY` piu' lungo dei gruppi che cadono).
FALL_TRAVEL = 20


def family_fall_end() -> float:
    """Fin dove cadono, a schermo, le gocce dei disegni di pioggia che l'app gia' usa.

    Escono tutte un poco dalla scatola alla fine della caduta, mentre svaniscono: la
    pioggia di `rain` arriva a 132,7. E' il limite che il composto rispetta, invece di uno
    piu' stretto che nessun altro disegno della famiglia rispetta."""
    worst = 0.0
    for stem in ("mc3_overcast_rain", "mc3_rain", "mc3_partly_cloudy_day_rain",
                 "mc3_overcast_snow", "mc3_partly_cloudy_day_snow"):
        g = scale_group(stem)
        k = float(g.get(A + "scaleX"))
        m = matrix(k, 0, 0)
        for el in g:
            if el.tag == "group" and el.get(A + "name") not in ("g4clouds", "g1maskmove"):
                for path in el.iter("path"):
                    if path.get(A + "name"):
                        p = attrs(path)
                        b = fall_box(p, m, 4 * k, drop=FALL_TRAVEL)
                        worst = max(worst, b[3])
    return worst


def fall_room(prefix="mc3_", night=False) -> list[str]:
    """Alla fine della caduta le gocce non escono dalla scatola piu' di quanto ne escano
    quelle che la famiglia gia' disegna ([family_fall_end])."""
    if not FALL:
        return []
    out = []
    limit = family_fall_end() + 0.05
    for name, p in fall_of(prefix, night):
        b = fall_box(p, fall_matrix(), CLOUD_STROKE, drop=FALL_TRAVEL)
        if b[3] > limit:
            out.append(f"{name} finisce la caduta a {b[3]:.1f}, oltre il {limit:.1f} della "
                       f"pioggia di Meteocons")
    return out


# --------------------------------------------------------------------- scrivere

def path_xml(p: dict, indent: str) -> str:
    order = ["pathData", "name", "fillColor", "fillType", "strokeColor", "strokeWidth",
             "strokeLineCap", "strokeLineJoin"]
    bits = [f'android:{k}="{p[k]}"' for k in order if k in p]
    return indent + "<path " + (NL + indent + "      ").join(bits) + "/>"


def group_open(name: str, indent: str, scale: float = None, pivot=(64, 64),
               translate=(0, 0)) -> str:
    """Un gruppo. `pivot=None` per un gruppo che non trasforma niente: e' il guscio su cui
    si appende un'animazione, e **deve restare vuoto di trasformazioni statiche** (vedi
    [drawing])."""
    bits = [f'android:name="{name}"']
    if scale is not None:
        bits.append(f'android:scaleX="{fmt(scale)}" android:scaleY="{fmt(scale)}"')
    if translate != (0, 0):
        bits.append(f'android:translateX="{fmt(translate[0])}" '
                    f'android:translateY="{fmt(translate[1])}"')
    if pivot is not None:
        bits.append(f'android:pivotX="{fmt(pivot[0])}" android:pivotY="{fmt(pivot[1])}"')
    return indent + "<group " + " ".join(bits) + ">"


def floor_segment(d: str):
    """Il tratto dritto in basso della silhouette: (sottopercorso, indice), o None."""
    for k, (start, segs, _) in enumerate(paths.parse_segments(d)):
        for i, seg in enumerate(segs):
            before = start if i == 0 else segs[i - 1][-1]
            if seg[0] == "L" and abs(before[1] - seg[1][1]) < 1e-6 and before[1] > 85:
                return k, i
    return None


def floorless(d: str) -> str:
    """La silhouette **senza pavimento**, come la nuvola dei disegni di pioggia di
    Meteocons nello stile `line`: il contorno parte dalla fine del tratto in basso, fa il
    giro e si ferma al suo inizio. Le gocce escono da li' invece di attraversare una riga."""
    found = floor_segment(d)
    if found is None:
        sys.exit("la silhouette di cloudy non ha piu' un pavimento dritto")
    k, i = found
    start, segs, _ = paths.parse_segments(d)[k]
    if segs[-1][-1] != start and segs[-1][-1] != tuple(start):
        sys.exit("la silhouette di cloudy non torna al punto da cui parte")
    opened = [segs[i][1], segs[i + 1:] + segs[:i], False]
    return paths.emit_path([opened])


def cloud_element(prefix: str, face: str) -> dict:
    silhouette, colour = cloud_of(prefix)
    if FALL and face == "line":
        silhouette = floorless(silhouette)
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
        # **Due gruppi per la nuvola, e non uno.** In un `<animated-vector>` un
        # `objectAnimator` **sostituisce** la proprieta' del gruppo, non la somma: un
        # `translateY` animato su un gruppo che ne porta gia' uno statico lo azzera, e la
        # nuvola parte per la tangente appena l'animazione comincia (visto sul telefono,
        # 12 set 2026). Quindi il guscio esterno e' vuoto di trasformazioni ed e' l'unico
        # che l'animazione tocca; quello interno porta la taglia e il posto e non si
        # muove. Il dondolio resta cosi' in unita' della scatola invece di finire diviso
        # per la scala della nuvola.
        group_open("g5cloud", " " * 4, pivot=None),
        group_open("g6cloudplace", " " * 8, scale=CLOUD_SCALE,
                   translate=(CLOUD_CENTRE[0] - 64, CLOUD_CENTRE[1] - 64)),
        path_xml(cloud_element(prefix, face), " " * 12),
        "        </group>",
        "    </group>",
    ]
    if FALL:
        # Fuori dal guscio che dondola, come nella sorgente: le gocce non salgono e
        # scendono con la nuvola, cadono e basta.
        m = fall_matrix()
        lines.append(group_open("g7fallplace", " " * 4, scale=FALL_SCALE, pivot=(0, 0),
                                translate=(m[4], m[5])))
        for name, p in fall_of(prefix, night):
            p = dict(p, name=renamed(p["name"]))
            if "strokeWidth" in p:
                p["strokeWidth"] = fmt(CLOUD_STROKE / FALL_SCALE)
            lines.append(group_open(renamed(name), " " * 8, pivot=None))
            lines.append(path_xml(p, " " * 12))
            lines.append(" " * 8 + "</group>")
        lines.append("    </group>")
    return lines


def still_xml(prefix: str, face: str, night: bool) -> str:
    body = NL.join(drawing(prefix, face, night))
    return (header(night) + NL +
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


def fall_targets(prefix: str, night: bool) -> list[str]:
    """Le animazioni delle gocce o dei fiocchi, ritagliate dal gemello animato della
    sorgente come [sun_target] fa per il sole, con i nomi di [renamed]."""
    if not FALL:
        return []
    moving = {"mc3_": "mc3a_", "mc3n_": "mc3an_",
              "mc3f_": "mc3fa_", "mc3fn_": "mc3fan_"}[prefix]
    source = DRAWABLE / (moving + FALLS[FALL].format(part="night" if night else "day") + ".xml")
    text = source.read_text(encoding="utf8")
    out = []
    for name, p in fall_of(prefix, night):
        for wanted in (name, p["name"]):
            head = f'<target android:name="{wanted}">'
            if text.count(head) != 1:
                sys.exit(f"{source.name}: non anima {wanted} una volta sola — la pioggia ha "
                         f"cambiato forma")
            start = text.index(head)
            block = text[start:text.index("</target>", start) + len("</target>")]
            block = block.replace(head, f'<target android:name="{renamed(wanted)}">')
            lines = block.splitlines()
            out += ["    " + lines[0]] + lines[1:]
    return out


def animated_xml(prefix: str, face: str, night: bool) -> str:
    body = [" " * 8 + line for line in drawing(prefix, face, night)]
    lines = [
        header(night),
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
        *fall_targets(prefix, night),
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
 * Sono una scala: il «quasi sereno» (il sereno di Meteocons intatto piu' una nuvoletta
 * nell'angolo, perche' il `mostly-clear` della libreria porta il 72% della nuvola del «poco
 * nuvoloso» per un cielo che ne ha il 39% di copertura) e dal 13 ott 2026 il «poco
 * nuvoloso» (lo stesso sole, la nuvola al 78%), che ha preso il posto di `partly-cloudy`
 * perche' fra i due disegni la scala saltava, e con lui gli acquazzoni: lo stesso cielo
 * con le gocce o i fiocchi di Meteocons sotto la nuvola. Il perche' per esteso, con le
 * misure, sta in testa allo strumento.
 */
internal object ComposedIcons {
'''


def kotlin_file() -> str:
    def res(prefix, name):
        return f"R.drawable.{prefix}{name.replace('-', '_')}"

    names = [f"{r['name']}-{part}" for r in RECIPES for part in ("day", "night")]
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
    body.append("    val mostlyClearNight: Int = R.drawable.mc3_sun_one_cloud_night" + NL)
    body.append("    /** Il disegno del «poco nuvoloso», di giorno e di notte. */")
    body.append("    @DrawableRes")
    body.append("    val partlyCloudyDay: Int = R.drawable.mc3_sun_half_cloud_day" + NL)
    body.append("    @DrawableRes")
    body.append("    val partlyCloudyNight: Int = R.drawable.mc3_sun_half_cloud_night" + NL)
    body.append("    /** Gli acquazzoni di pioggia, di giorno e di notte. */")
    body.append("    @DrawableRes")
    body.append("    val showersDay: Int = R.drawable.mc3_sun_half_cloud_rain_day" + NL)
    body.append("    @DrawableRes")
    body.append("    val showersNight: Int = R.drawable.mc3_sun_half_cloud_rain_night" + NL)
    body.append("    /** Gli acquazzoni di neve, di giorno e di notte. */")
    body.append("    @DrawableRes")
    body.append("    val snowShowersDay: Int = R.drawable.mc3_sun_half_cloud_snow_day" + NL)
    body.append("    @DrawableRes")
    body.append("    val snowShowersNight: Int = R.drawable.mc3_sun_half_cloud_snow_night")
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
    # le gocce cadono dalla nuvola di `cloudy`: la nuvola della loro sorgente deve essere
    # quella, alle stesse coordinate, o qui cadrebbero da un'altra parte
    global FALL
    for fall in FALLS:
        FALL = fall
        for prefix in ("mc3f_", "mc3fn_"):
            for night in (False, True):
                if fall_cloud(prefix, night) != cloud_of(prefix)[0]:
                    problems.append(f"{fall_stem(prefix, night)}: la nuvola non e' piu' la "
                                    f"silhouette di cloudy")
    FALL = None
    floor = floor_segment(cloud_of("mc3_")[0])
    if floor is None:
        problems.append("la silhouette di cloudy non ha piu' un pavimento dritto")
    else:
        start, segs, _ = paths.parse_segments(cloud_of("mc3_")[0])[floor[0]]
        a = start if floor[1] == 0 else segs[floor[1] - 1][-1]
        b = segs[floor[1]][1]
        if abs((a[0] + b[0]) / 2 - FLOOR[0]) > 1e-3 or abs(a[1] - FLOOR[1]) > 1e-3:
            problems.append(f"il pavimento di cloudy e' da {a} a {b}, non intorno a {FLOOR}")
    return problems


def report() -> str:
    lines = []
    for recipe in RECIPES:
        use(recipe, night=False)
        lines.append(f"== {recipe['name']} (codice {recipe['wmo']})")
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
        silhouette, _ = cloud_of("mc3_")
        b = bbox(transformed(silhouette, cloud_matrix()))
        lines.append(f"nuvola: larga {b[2] - b[0] + CLOUD_STROKE:.1f} con il contorno, "
                     f"il {(b[2] - b[0] + CLOUD_STROKE) / 99.2 * 100:.0f}% di quella del "
                     f"poco nuvoloso di Meteocons")
        for night in (False, True):
            use(recipe, night)
            b = ink_box(night=night)
            w, h = b[2] - b[0], b[3] - b[1]
            lines.append(f"inchiostro {'notte ' if night else 'giorno'}: "
                         f"{w:.1f} x {h:.1f}, media geometrica "
                         f"{math.sqrt(w * h) / 128:.3f} della scatola (la famiglia sta a 0,690)")
    return NL.join(lines)


def slivers() -> list[str]:
    """I raggi che il buco taglia per il lungo: il difetto che sposta la nuvola."""
    out = []
    for i, ray in enumerate(ray_report(), start=1):
        if 0.02 < ray["kept"] < 0.99 and ray["width"] < 0.6 * 3.68:
            out.append(f"il raggio {i} resta largo {ray['width']:.2f} invece di 3,68: "
                       f"il buco lo taglia per il lungo, sposta il centro della nuvola")
    return out


def tips(prefix="mc3f_") -> list[str]:
    """Una punta di raggio rimasta nell'aria fra il buco e la nuvola (13 ott 2026).

    [slivers] guarda i raggi a passi di un quarto d'unita', e una punta piu' piccola gli
    sfugge: il «quasi sereno» ne lasciava una nello stile pieno, un puntino giallo accanto
    alla nuvoletta che a 160 dp si vedeva. Qui si guarda a un decimo d'unita', e un raggio
    di cui resta meno del 3% e' una punta, non un raggio: o si vede, o non c'e'.
    """
    out = []
    for i, ray in enumerate(ray_report(prefix, step=0.1), start=1):
        if 0 < ray["kept"] < 0.03:
            out.append(f"del raggio {i} resta una punta ({ray['kept'] * 100:.1f}%) nell'aria "
                       f"intorno alla nuvola: sposta il centro della nuvola")
    return out


def moon_fragments(step=0.5) -> list[str]:
    """Di notte, i pezzi della falce che restano fuori dalla nuvola: deve essere uno solo.

    Una punta della falce che spunta dall'altra parte della nuvola, staccata dal resto, e'
    la scheggia della notte (13 ott 2026: la punta bassa, accanto al «poco nuvoloso»). Si
    guarda il tratto dello stile `line`, il piu' sottile: dove resta intero lui, resta
    intero anche il disegno pieno.
    """
    sun = sun_of("mc3_", night=True)
    m = matrix(sun["scale"], 0, 0)
    outline = [p for sub in raster.flatten(transformed(sun["moon"]["pathData"], m)) for p in sub]
    half = float(sun["moon"].get("strokeWidth", 4)) * sun["scale"] / 2
    hole = raster.flatten(hole_path())
    segs = list(zip(outline, outline[1:]))

    def on_stroke(x, y):
        for (ax, ay), (bx, by) in segs:
            dx, dy = bx - ax, by - ay
            t = max(0.0, min(1.0, ((x - ax) * dx + (y - ay) * dy) / (dx * dx + dy * dy or 1)))
            if math.hypot(x - ax - t * dx, y - ay - t * dy) <= half:
                return True
        return False

    x0, y0, x1, y1 = bbox(transformed(sun["moon"]["pathData"], m))
    cells = set()
    i = 0
    while x0 - half + i * step <= x1 + half:
        j = 0
        while y0 - half + j * step <= y1 + half:
            x, y = x0 - half + i * step, y0 - half + j * step
            if on_stroke(x, y) and raster.winding(x, y, hole) == 0:
                cells.add((i, j))
            j += 1
        i += 1
    pieces = 0
    seen = set()
    for c in cells:
        if c in seen:
            continue
        pieces += 1
        stack = [c]
        seen.add(c)
        while stack:
            a, b = stack.pop()
            for n in ((a + 1, b), (a - 1, b), (a, b + 1), (a, b - 1)):
                if n in cells and n not in seen:
                    seen.add(n)
                    stack.append(n)
    if pieces > 1:
        return [f"di notte la falce resta in {pieces} pezzi fuori dalla nuvola: sposta il "
                f"centro di notte"]
    return []


def main() -> None:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--controlla", action="store_true",
                    help="verifica e misura senza scrivere niente")
    args = ap.parse_args()

    problems = check_sources()
    for recipe in RECIPES:
        use(recipe, night=False)
        problems += [f"{recipe['name']}: {p}" for p in slivers() + tips()]
        problems += [f"{recipe['name']}: {p}" for p in fall_room()]
        use(recipe, night=True)
        problems += [f"{recipe['name']}: {p}" for p in moon_fragments() + fall_room(night=True)]
    if problems:
        sys.exit("la composizione non si scrive:" + NL + NL.join(f"  - {p}" for p in problems))

    written = []
    for recipe in RECIPES:
        for still, moving, source, face, ground in SETS:
            for night in (False, True):
                use(recipe, night)
                name = file_stem(recipe, night)
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
