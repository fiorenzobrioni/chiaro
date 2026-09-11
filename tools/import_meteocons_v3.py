#!/usr/bin/env python3
"""Converte Meteocons **v3** nei vector drawable dell'app. Fase 13.

Sostituisce `import_meteocons.py`, che legge la v2.0.0 e che resta finche' la v2 e'
quella spedita. Le differenze che contano, e perche':

1. **La sorgente e' un pacchetto, non un clone.** v3 si pubblica su npm
   (`@meteocons/svg`, SVG con la SMIL dell'illustratore; `@meteocons/svg-static` e'
   lo stesso senza) e su `cdn.meteocons.com`. Si scompatta il tarball e si punta qui:

       npm pack @meteocons/svg@3.0.0-next.10
       tar -xzf meteocons-svg-3.0.0-next.10.tgz
       python tools/import_meteocons_v3.py package

2. **Le maschere.** v3 compone i cieli con `<mask>`, che VectorDrawable non ha. Ogni
   maschera diventa un `<clip-path>` con il sottopercorso interno invertito di verso
   ([svg_paths.mask_to_clip], che ri-rasterizza ogni conversione prima di restituirla).
   Quando la maschera si **muove** — la nuvola che sale e scende mentre il sole sotto
   resta fermo — servono due gruppi annidati, perche' in VectorDrawable la
   trasformazione di un gruppo si applica sia al clip sia ai figli: l'esterno muove il
   clip, l'interno rimette fermo il disegno con la traslazione opposta e lo stesso
   interpolatore.

3. **Gli stili sono `line` e `flat`**, non `line` e `fill`. Il `flat` di v3 e'
   esattamente cio' che questo repo gia' spedisce come set «pieno»: il fill con i
   gradienti appiattiti sul colore di faccia. Disegnato cosi' a monte invece che
   derivato qui, quindi si riusa invece di ricreare. `fill` e `monochrome` si
   convertono lo stesso — il costo e' zero finche' nessuna tabella Kotlin li nomina,
   perche' `shrinkResources` li toglie dalla release (misurato, PLANNING Fase 13).

4. **I colori restano quelli dell'illustratore.** Il riancoraggio al 3:1 delle due
   superfici (DESIGN §10) e' un passo successivo su questo stesso albero, non una
   scelta fatta qui: si generano entrambi e si decide guardando. Questo e' il motivo
   per cui i nomi portano `mc3`: non sono ancora i set spediti.

4b. **La taglia no** (11 set 2026, decisione del committente che rovescia la sua del
   mattino). Meteocons disegna ogni icona alla taglia che le serve — dal 33% della
   scatola all'81% — e incolonnate nel Cielo quelle differenze si leggono come un
   difetto dell'app. Ogni drawable spedito esce quindi avvolto in un gruppo che porta il
   lato del suo inchiostro a 0,75 della scatola: [with_scale], su misure di
   `tools/icon_ink.py`. La finestra resta quella dell'illustratore, il disegno dentro no.

5. **Gli `interpolator`.** v2 era tutta lineare; v3 usa `calcMode="spline"` con
   `keySplines`, che e' esattamente un `<pathInterpolator>`. Se ne genera uno per ogni
   coppia di controlli distinta, in `res/interpolator/`.

Quel che NON sa fare e' scritto nel rapporto finale, icona per icona, invece di essere
taciuto: il tratteggio (VectorDrawable non ha `stroke-dasharray`), i filtri (l'ombra
portata di `compass*`), e ogni elemento o animazione fuori dal vocabolario qui sotto.
"""
from __future__ import annotations

import collections
import importlib.util
import pathlib
import re
import sys
import xml.etree.ElementTree as ET

HERE = pathlib.Path(__file__).resolve().parent
OUT = HERE.parent / "app" / "src" / "main" / "res" / "drawable"
INTERPOLATORS = HERE.parent / "app" / "src" / "main" / "res" / "interpolator"
KOTLIN = (HERE.parent / "app" / "src" / "main" / "kotlin" / "com" / "callbackdev"
          / "chiaro" / "ui" / "icons" / "MeteoconsSets.kt")
SVG_NS = "{http://www.w3.org/2000/svg}"
NL = chr(10)


def _load(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


paths = _load("svg_paths", HERE / "svg_paths.py")
reanchor = _load("reanchor", HERE / "reanchor.py")
shipped = _load("shipped_icons", HERE / "shipped_icons.py")
ink = _load("icon_ink", HERE / "icon_ink.py")
#: La fase negativa e la rotazione dei keyframe NON sono novita' di v3: le ha risolte
#: l'importatore della v2 e le sue funzioni valgono qui identiche. Riusarle e' anche la
#: prova che il passaggio alla v3 e' un trasloco, non una riscrittura.
v2 = _load("v2", HERE / "import_meteocons.py")

fmt = paths.fmt

#: (statico, animato) per stile e per **fondo**, con lo stesso schema della v2: il set
#: senza suffisso e' quello dei fondi chiari, quello con la `n` e' per i fondi scuri.
#: `ChiaroIcons.styledRes` sceglie per fondo, e ogni set incontra solo la superficie
#: contro cui e' stato misurato.
PREFIXES = {
    ("line", "light"): ("mc3_", "mc3a_"),
    ("line", "dark"): ("mc3n_", "mc3an_"),
    ("flat", "light"): ("mc3f_", "mc3fa_"),
    ("flat", "dark"): ("mc3fn_", "mc3fan_"),
    ("fill", "light"): ("mc3p_", "mc3pa_"),
    ("fill", "dark"): ("mc3pn_", "mc3pan_"),
    ("monochrome", "light"): ("mc3m_", "mc3ma_"),
    ("monochrome", "dark"): ("mc3mn_", "mc3man_"),
}

#: I colori dell'illustratore, senza riancoraggio: non si spediscono (sulla carta sono
#: fantasmi), ma servono al filmstrip per il confronto che decide.
ORIGINAL = {"line": ("mc3o_", "mc3oa_"), "flat": ("mc3fo_", "mc3foa_")}

COLOR_ATTR = re.compile(r'(android:(?:fill|stroke)Color=")(#[0-9a-fA-F]{6})(")')

#: Icone che si importano a **meta'**: nome nuovo -> (sorgente, gruppo da tenere).
#:
#: Gruppi da **lasciare indietro**, per icona: si converte tutto tranne quel che si nomina.
#:
#: `compass` porta le lettere **N E S W disegnate come path**, e in italiano l'ovest e' O:
#: e' testo inglese dentro un'immagine, in un prodotto dove tutto quel che sta a schermo
#: si localizza. E' la stessa ragione per cui le otto `wind-direction-*` sono state
#: scartate. La bussola della v2 era cerchio piu' ago e basta, quindi toglierle e' anche
#: tornare al disegno con cui la riga dei luoghi era stata messa a punto.
DROP_GROUPS = {
    "compass": ("Letters",),
}



def recolor(xml: str, mapping: dict) -> str:
    """Lo stesso disegno, un'altra tavolozza. Si sostituiscono solo i due attributi di
    colore, mai il testo libero: il `pathData` puo' contenere qualunque cosa."""
    return COLOR_ATTR.sub(
        lambda m: m.group(1) + mapping.get(m.group(2).lower(), m.group(2)) + m.group(3),
        xml)


def palette_of(folder: pathlib.Path) -> set:
    """Ogni colore che uno stile mette a schermo, **maschere escluse**: li' `#fff` e
    `#000` non sono inchiostro, sono la maschera, e contarli falsava la misura."""
    out = set()
    for svg in folder.glob("*.svg"):
        text = re.sub(r"<mask.*?</mask>", "", svg.read_text(encoding="utf8"),
                      flags=re.S)
        out |= {c.lower() for c in
                re.findall(r'(?:stroke|fill|stop-color)="(#[0-9a-fA-F]{6})"', text)}
    return out

HEADER = (
    "<!-- Generato da tools/import_meteocons_v3.py da Meteocons v3\n"
    "     (github.com/basmilius/meteocons), MIT, (c) Bas Milius —\n"
    "     licenses/Meteocons-MIT.txt. Colori dell'illustratore, non ancora\n"
    "     riancorati alle superfici dell'app. Non modificare a mano. -->\n"
)


class Unsupported(Exception):
    """Qualcosa che VectorDrawable non sa dire. Si annota e si passa oltre: una sola
    icona zoppa non deve fermare l'importazione delle altre cinquecento."""


# --------------------------------------------------------------------------- SMIL

def parse_time(v: str) -> int:
    if not v.endswith("s"):
        raise Unsupported(f"tempo SMIL {v!r}")
    return round(float(v[:-1]) * 1000)


def smil_values(attr: str, arity: int):
    out = []
    for chunk in attr.split(";"):
        parts = chunk.replace(",", " ").split()
        if len(parts) != arity:
            raise Unsupported(f"{arity} numeri attesi per valore SMIL, trovato {chunk!r}")
        out.append([float(p) for p in parts])
    return out


def smil_phase(a, duration: int) -> float:
    """Un `begin` negativo non e' un ritardo: dice che il ciclo e' gia' partito, ed e'
    cio' che fa cadere le gocce sfasate invece che in coro. AVD ha solo il ritardo,
    quindi la fase si cuoce nei keyframe."""
    ms = parse_time(a.get("begin", "0s"))
    if ms == 0:
        return 0.0
    if ms > 0:
        raise Unsupported(f"begin positivo {a.get('begin')!r}")
    return ((-ms) % duration) / duration


def smil_key_times(a):
    kt = a.get("keyTimes")
    return [float(v) for v in kt.split(";")] if kt else None


def rotate_keyframes(times, values, phase):
    """`keyTimes` piu' fase: la rotazione di `v2.loop_keyframes` su un ciclo i cui punti
    NON sono equispaziati. v2 non ne aveva bisogno; v3 lo porta con le gocce."""
    def at(f):
        f %= 1.0
        for i in range(len(times) - 1):
            if times[i] <= f <= times[i + 1]:
                span = times[i + 1] - times[i]
                if span <= 0:
                    return values[i + 1]
                return values[i] + (values[i+1] - values[i]) * (f - times[i]) / span
        return values[-1]

    if not phase:
        return list(zip(times, values))
    if abs(values[0] - values[-1]) > 1e-9:
        raise Unsupported("ciclo a dente di sega con keyTimes e fase")
    breaks = sorted({0.0, 1.0} | {round((t - phase) % 1.0, 9) for t in times})
    return [(b, at(b + phase)) for b in breaks]


# ------------------------------------------------------------------------ emissione

class Emitter:
    def __init__(self, clips=None, animated=False, drop=()):
        self.targets: list[str] = []
        self.interpolators: set = set()
        self.clips = clips or {}
        self.drop = tuple(drop)
        #: Il disegno fermo e quello che si muove non dicono il tratteggio allo stesso
        #: modo: fermo lo si ridisegna a segmenti, in movimento diventa una finestra di
        #: `trimPath` che corre. Da cui due passate sullo stesso albero.
        self.animated = animated
        self.n = 0

    def gid(self, hint: str) -> str:
        self.n += 1
        return f"g{self.n}{re.sub(r'[^a-z0-9]+', '', hint.lower())[:10]}"

    def interpolator(self, key_splines) -> str:
        if not key_splines:
            return "@android:anim/linear_interpolator"
        first = key_splines.split(";")[0].replace(",", " ").split()
        if len(first) != 4:
            raise Unsupported(f"keySplines {key_splines!r}")
        vals = tuple(float(f) for f in first)
        res = "mc_ease_" + "_".join(
            str(v).replace(".", "").replace("-", "m") or "0" for v in vals)
        self.interpolators.add((res, vals))
        return f"@interpolator/{res}"

    def animate(self, target, prop, controls, duration, interp,
                phase=0.0, key_times=None):
        pad = " " * 16
        if len(controls) == 2 and not phase and not key_times:
            body = [pad + f'android:propertyName="{prop}"',
                    pad + f'android:valueFrom="{fmt(controls[0])}"',
                    pad + f'android:valueTo="{fmt(controls[1])}"/>']
        else:
            frames = (rotate_keyframes(key_times, controls, phase) if key_times
                      else v2.loop_keyframes(controls, phase))
            body = [pad + f'android:propertyName="{prop}">',
                    pad + f'<propertyValuesHolder android:propertyName="{prop}">']
            body += [pad + f'    <keyframe android:fraction="{fmt(f)}"'
                     f' android:value="{fmt(v)}"/>' for f, v in frames]
            body += [pad + "</propertyValuesHolder>", pad[:-4] + "</objectAnimator>"]
        self.targets.append(NL.join([
            f'    <target android:name="{target}">',
            '        <aapt:attr name="android:animation">',
            "            <objectAnimator",
            pad + f'android:duration="{duration}"',
            pad + 'android:repeatCount="infinite"',
            pad + 'android:valueType="floatType"',
            pad + f'android:interpolator="{interp}"',
        ] + body + ["        </aapt:attr>", "    </target>"]))


def transform_groups(t: str):
    """Un `transform` statico -> gli attributi dei `<group>` che lo dicono in Android.

    Non si cuoce nel `pathData`: un gruppo VectorDrawable ha esattamente le primitive
    che servono (`rotate(a cx cy)` e' `rotation` piu' il pivot, alla lettera) e cuocere
    una rotazione dentro un arco vorrebbe dire ricalcolarne raggi e inclinazione. Piu'
    primitive diventano gruppi annidati, la prima piu' esterna — l'ordine in cui SVG le
    moltiplica.
    """
    out = []
    for fn, args in re.findall(r"(\w+)\s*\(([^)]*)\)", t):
        a = [float(v) for v in args.replace(",", " ").split()]
        if fn == "translate":
            out.append(f'android:translateX="{fmt(a[0])}" '
                       f'android:translateY="{fmt(a[1] if len(a) > 1 else 0)}"')
        elif fn == "rotate" and len(a) == 3:
            out.append(f'android:rotation="{fmt(a[0])}" '
                       f'android:pivotX="{fmt(a[1])}" android:pivotY="{fmt(a[2])}"')
        elif fn == "rotate":
            out.append(f'android:rotation="{fmt(a[0])}"')
        elif fn == "scale":
            out.append(f'android:scaleX="{fmt(a[0])}" '
                       f'android:scaleY="{fmt(a[1] if len(a) > 1 else a[0])}"')
        else:
            raise Unsupported(f"transform {fn}()")
    return out


def canvas_clip(el, clip_paths):
    """Il `clip-path` di un gruppo, o None quando e' il rettangolo di tela.

    Figma avvolge quasi ogni icona in un clip grande quanto la scatola: e' un no-op, e
    portarselo dietro vorrebbe dire un gruppo in piu' per ogni disegno.
    """
    ref = el.get("clip-path")
    if not ref or not ref.startswith("url(#"):
        return None
    node = clip_paths.get(ref[5:-1])
    if node is None:
        raise Unsupported("clip-path verso un id ignoto")
    shapes = [c for c in node if c.tag.startswith(SVG_NS)]
    if len(shapes) == 1 and shapes[0].tag == SVG_NS + "rect":
        r = shapes[0]
        if (float(r.get("x", 0)) == 0 and float(r.get("y", 0)) == 0
                and float(r.get("width")) >= 128 and float(r.get("height")) >= 128):
            return None
    if len(shapes) != 1:
        raise Unsupported(f"clip-path con {len(shapes)} forme")
    return shapes[0]


def child_anims(el):
    return [c for c in el
            if c.tag in (SVG_NS + "animateTransform", SVG_NS + "animate",
                         SVG_NS + "animateMotion")]


def shape_data(el, gradients, notes, bake_transform: bool = False):
    """Una forma SVG -> il suo `pathData`. Cerchi, rettangoli e segmenti diventano
    percorsi, perche' VectorDrawable conosce solo quelli."""
    tag = el.tag
    if tag == SVG_NS + "circle":
        return paths.circle_to_path(float(el.get("cx")), float(el.get("cy")),
                                    float(el.get("r")))
    if tag == SVG_NS + "rect":
        x, y = float(el.get("x", 0)), float(el.get("y", 0))
        w, h = float(el.get("width")), float(el.get("height"))
        rx = float(el.get("rx") or el.get("ry") or 0)
        ry = float(el.get("ry") or el.get("rx") or 0)
        if not rx and not ry:
            return (f"M{fmt(x)},{fmt(y)} L{fmt(x+w)},{fmt(y)} "
                    f"L{fmt(x+w)},{fmt(y+h)} L{fmt(x)},{fmt(y+h)} Z")
        # Gli angoli arrotondati sono quattro archi veri: e' la forma dell'ago del
        # barometro e delle sue bande, cioe' 149 icone su 1 038.
        rx, ry = min(rx, w / 2), min(ry, h / 2)
        return " ".join([
            f"M{fmt(x + rx)},{fmt(y)}",
            f"L{fmt(x + w - rx)},{fmt(y)}",
            f"A{fmt(rx)},{fmt(ry)},0,0,1,{fmt(x + w)},{fmt(y + ry)}",
            f"L{fmt(x + w)},{fmt(y + h - ry)}",
            f"A{fmt(rx)},{fmt(ry)},0,0,1,{fmt(x + w - rx)},{fmt(y + h)}",
            f"L{fmt(x + rx)},{fmt(y + h)}",
            f"A{fmt(rx)},{fmt(ry)},0,0,1,{fmt(x)},{fmt(y + h - ry)}",
            f"L{fmt(x)},{fmt(y + ry)}",
            f"A{fmt(rx)},{fmt(ry)},0,0,1,{fmt(x + rx)},{fmt(y)}",
            "Z",
        ])
    if tag == SVG_NS + "line":
        return (f"M{fmt(float(el.get('x1')))},{fmt(float(el.get('y1')))} "
                f"L{fmt(float(el.get('x2')))},{fmt(float(el.get('y2')))}")
    if tag == SVG_NS + "path":
        return paths.emit_path(paths.parse_segments(el.get("d")))
    raise Unsupported(f"<{tag.replace(SVG_NS, '')}>")


def clip_data(el, gradients, notes) -> str:
    """La forma di un `<clipPath>`, col suo `transform` **cotto nelle coordinate**.

    `<clip-path>` in VectorDrawable non ha un `transform`, e appoggiarlo al gruppo che lo
    contiene lo applicherebbe anche ai figli — cioe' muoverebbe il disegno insieme al
    ritaglio. Ignorarlo era il difetto trovato sul telefono l'11 set 2026: il `clipPath`
    del barometro e' un rettangolo ruotato di 45°, e senza la rotazione finiva nell'angolo
    in alto a sinistra e **cancellava l'ago**. Venti icone, tutte `barometer*` e
    `compass*`, e due di queste si spediscono.
    """
    d = shape_data(el, gradients, notes)
    t = el.get("transform")
    if not t:
        return d
    try:
        m = paths.parse_transform(t)
        return paths.emit_path(paths.transform_path(paths.parse_segments(d), m))
    except ValueError as err:
        raise Unsupported(f"clip-path con {err}")


def paint(el, gradients, notes):
    """Riempimento e tratto di una forma, in attributi Android.

    Un `url(#gradiente)` viene **appiattito sul colore di faccia**: e' la dipartenza n. 4
    dell'importatore v2, qui per lo stesso motivo (la rampa non si vede alle dimensioni
    in cui l'icona e' disegnata) e per uno in piu': con `line` e `flat` i gradienti sono
    19 su 1 038, cioe' un caso di bordo, non lo stile.
    """
    out = []

    def color(value):
        if value.startswith("url(#"):
            grad = gradients.get(value[5:-1])
            if grad is None:
                raise Unsupported(f"riferimento a un riempimento ignoto {value!r}")
            stops = grad.findall(SVG_NS + "stop")
            if not stops:
                raise Unsupported("gradiente senza stop")
            notes.add("gradiente appiattito")
            return stops[0].get("stop-color", "#000")
        return value

    fill = el.get("fill")
    if fill and fill != "none":
        out.append(f'android:fillColor="{color(fill)}"')
        if el.get("fill-rule") == "evenodd":
            out.append('android:fillType="evenOdd"')
        if el.get("fill-opacity"):
            out.append(f'android:fillAlpha="{el.get("fill-opacity")}"')
    stroke = el.get("stroke")
    if stroke and stroke != "none":
        out.append(f'android:strokeColor="{color(stroke)}"')
        out.append(f'android:strokeWidth="{el.get("stroke-width", "1")}"')
        for svg_name, avd in (("stroke-linecap", "strokeLineCap"),
                              ("stroke-linejoin", "strokeLineJoin"),
                              ("stroke-miterlimit", "strokeMiterLimit"),
                              ("stroke-opacity", "strokeAlpha")):
            if el.get(svg_name):
                out.append(f'android:{avd}="{el.get(svg_name)}"')
    return out


def walk(el, em, out, depth, gradients, notes, alpha=()):
    ind = "    " * depth
    tag = el.tag

    if tag in (SVG_NS + "animateTransform", SVG_NS + "animate",
               SVG_NS + "animateMotion", SVG_NS + "defs", SVG_NS + "mask",
               SVG_NS + "title", SVG_NS + "desc"):
        return
    if tag in (SVG_NS + "filter", SVG_NS + "style"):
        raise Unsupported(f"<{tag.replace(SVG_NS, '')}>")

    if tag == SVG_NS + "g":
        if em.drop and (el.get("id") or "").split("__")[-1] in em.drop:
            return
        wrappers = transform_groups(el.get("transform")) if el.get("transform") else []
        clip_shape = canvas_clip(el, em.clips)
        if wrappers or clip_shape is not None:
            for w in wrappers:
                out.append("    " * depth + f"<group {w}>")
                depth += 1
            if clip_shape is not None:
                out.append("    " * depth + "<group>")
                out.append("    " * (depth + 1) + '<clip-path android:pathData="'
                           + clip_data(clip_shape, gradients, notes) + '"/>')
                depth += 1
                wrappers = wrappers + [None]
            bare = ET.Element(SVG_NS + "g", {k: v for k, v in el.attrib.items()
                                             if k not in ("transform", "clip-path")})
            for c in el:
                bare.append(c)
            walk(bare, em, out, depth, gradients, notes, alpha)
            for _ in wrappers:
                depth -= 1
                out.append("    " * depth + "</group>")
            return
        mask = el.find(SVG_NS + "mask")
        if mask is not None:
            _masked_group(el, mask, em, out, depth, gradients, notes, alpha)
            return

        anims = child_anims(el)
        if not anims:
            for c in el:
                walk(c, em, out, depth, gradients, notes, alpha)
            return

        # Due animazioni su un elemento sono due trasformazioni sommate, e AVD da' a un
        # gruppo esattamente una trasformazione: diventano due gruppi annidati,
        # l'esterno per prima — l'ordine in cui SMIL le moltiplica.
        transforms = [a for a in anims if a.tag == SVG_NS + "animateTransform"]
        fades = [a for a in anims if a not in transforms]
        for a in fades:
            if a.get("attributeName") not in ("opacity", "stroke-dashoffset"):
                raise Unsupported(f"animate {a.get('attributeName')!r}")

        opened = 0
        for a in transforms:
            _transform_group(a, el, em, out, depth + opened)
            opened += 1
        for c in el:
            walk(c, em, out, depth + opened, gradients, notes,
                 tuple(alpha) + tuple(fades))
        for k in range(opened - 1, -1, -1):
            out.append("    " * (depth + k) + "</group>")
        return

    if tag in (SVG_NS + "path", SVG_NS + "circle", SVG_NS + "rect", SVG_NS + "line"):
        anims = child_anims(el)
        if anims:
            # Una trasformazione su una forma e' un gruppo in AVD: la si avvolge.
            wrapper = ET.Element(SVG_NS + "g", {"id": el.get("id", "shape")})
            wrapper.append(ET.Element(tag, dict(el.attrib)))
            for a in anims:
                wrapper.append(a)
            walk(wrapper, em, out, depth, gradients, notes, alpha)
            return
        wrappers = transform_groups(el.get("transform")) if el.get("transform") else []
        for w in wrappers:
            out.append(ind + f"<group {w}>")
            ind += "    "
        d = shape_data(el, gradients, notes)
        attrs = paint(el, gradients, notes)
        trim = None
        if el.get("stroke-dasharray"):
            on, off = paths.dash_pattern(el.get("stroke-dasharray"))
            sweeping = [a for a in alpha
                        if a.get("attributeName") == "stroke-dashoffset"]
            if sweeping:
                # Un tratteggio il cui offset e' animato **non e' un tratteggio**: e' una
                # finestra che corre, e il disegno e' il tratto intero. Nel gemello
                # animato diventa `trimPath`; nel disegno FERMO resta **pieno**, ed e' la
                # stessa conclusione dell'importatore v2 («il tratteggio li' esisteva solo
                # per essere animato»).
                #
                # Spezzarlo anche da fermo era il primo tentativo, e sul dispositivo si e'
                # visto perche' e' sbagliato (11 set 2026): il marchio del vento nella
                # scheda non si anima mai — DESIGN §7.1, solo le condizioni si muovono —
                # quindi quel che il lettore vede per sempre e' il primo fotogramma di
                # un'animazione che non parte, cioe' meta' riga e un pezzo di ricciolo.
                # Sembrava rotto perche' lo era.
                if em.animated:
                    trim = paths.trim_window(d, on, off)
                if trim is not None:
                    attrs.append(f'android:trimPathStart="{fmt(trim[0])}"')
                    attrs.append(f'android:trimPathEnd="{fmt(trim[1])}"')
                    notes.add("tratteggio come finestra di trimPath")
                else:
                    notes.add("tratteggio spazzolato reso pieno (il fermo)")
            else:
                d = paths.dash_split(d, on, off)
                notes.add("tratteggio fermo ridisegnato a segmenti")
        pname = em.gid("p") if (alpha or trim) else None
        bits = [f'{ind}<path android:pathData="{d}"']
        if pname:
            bits.append(f'{ind}    android:name="{pname}"')
        bits += [f"{ind}    {a}" for a in attrs]
        out.append(NL.join(bits) + "/>")
        if pname:
            _fade(el, pname, em, alpha, attrs, trim)
        for _ in wrappers:
            ind = ind[:-4]
            out.append(ind + "</group>")
        return

    raise Unsupported(f"<{tag.replace(SVG_NS, '')}>")


def _transform_group(a, el, em, out, depth):
    """Un `animateTransform` -> un `<group>` con la sua proprieta' animata."""
    kind = a.get("type")
    name = em.gid(el.get("id", "g").split("__")[-1])
    dur = parse_time(a.get("dur"))
    phase = smil_phase(a, dur)
    interp = em.interpolator(a.get("keySplines"))
    times = smil_key_times(a)
    ind = "    " * depth
    if kind == "translate":
        vals = smil_values(a.get("values"), 2)
        out.append(f'{ind}<group android:name="{name}">')
        for axis, idx in (("translateX", 0), ("translateY", 1)):
            col = [v[idx] for v in vals]
            if any(abs(v) > 1e-9 for v in col):
                em.animate(name, axis, col, dur, interp, phase, times)
    elif kind == "rotate":
        vals = smil_values(a.get("values"), 3)
        out.append(f'{ind}<group android:name="{name}" '
                   f'android:pivotX="{fmt(vals[0][1])}" '
                   f'android:pivotY="{fmt(vals[0][2])}">')
        em.animate(name, "rotation", [v[0] for v in vals], dur, interp, phase, times)
    elif kind == "scale":
        vals = smil_values(a.get("values"), 2)
        out.append(f'{ind}<group android:name="{name}">')
        for axis, idx in (("scaleX", 0), ("scaleY", 1)):
            em.animate(name, axis, [v[idx] for v in vals], dur, interp, phase, times)
    else:
        raise Unsupported(f"animateTransform {kind!r}")


def _fade(el, pname, em, alpha, attrs, trim=None):
    """Le animazioni che in AVD sono proprieta' del **path** e non del gruppo."""
    has_fill = any(a.startswith("android:fillColor") for a in attrs)
    has_stroke = any(a.startswith("android:strokeColor") for a in attrs)
    for a in alpha:
        dur = parse_time(a.get("dur"))
        interp = em.interpolator(a.get("keySplines"))
        controls = [float(v) for v in a.get("values").split(";")]
        if a.get("attributeName") == "stroke-dashoffset":
            if trim is None:
                continue
            _sweep(a, pname, em, controls, dur, interp, trim)
            continue
        for prop, present in (("fillAlpha", has_fill), ("strokeAlpha", has_stroke)):
            if present:
                em.animate(pname, prop, controls, dur, interp,
                           smil_phase(a, dur), smil_key_times(a))


def _sweep(a, pname, em, controls, dur, interp, trim):
    """`stroke-dashoffset` che cresce -> `trimPathOffset` che scorre.

    Le due grandezze non hanno la stessa unita' ne' lo stesso verso. In SVG l'offset si
    misura in unita' del disegno e, crescendo, sposta il motivo **all'indietro** lungo il
    tratto; `trimPathOffset` e' una frazione della lunghezza totale e, crescendo, sposta
    la finestra in avanti. Quindi si converte la corsa in giri di percorso e si anima da
    1 a 0. La durata e' quella di UN giro, ripetuta: il ciclo SMIL ne fa piu' d'uno
    (mille unita' su un tratto lungo ottantacinque sono quasi dodici raffiche).
    """
    _, _, total = trim
    swept = abs(controls[-1] - controls[0])
    if swept <= 0 or total <= 0:
        return
    lap = round(dur * total / swept)          # quanto dura un giro intero
    begin = parse_time(a.get("begin", "0s"))
    phase = 0.0
    if begin < 0:
        # dove si trovava la finestra all'apertura della pagina, in giri
        phase = (((-begin) * swept / dur) % total) / total
    em.animate(pname, "trimPathOffset", [1.0, 0.0], lap, interp, phase)


def _masked_group(el, mask, em, out, depth, gradients, notes, alpha):
    """`<g><mask/><g mask="url(#…)">…</g></g>` -> clip, e se serve la coppia annidata."""
    inner = [c for c in el if c.tag == SVG_NS + "g" and c.get("mask")]
    if len(inner) != 1:
        raise Unsupported("gruppo con <mask> senza esattamente un figlio mascherato")
    holder = mask.find(SVG_NS + "g")
    if holder is None:
        holder = mask
    shapes = [c for c in holder
              if c.tag in (SVG_NS + "path", SVG_NS + "rect", SVG_NS + "circle")]
    if len(shapes) != 1:
        raise Unsupported(f"maschera con {len(shapes)} forme")
    clip = paths.mask_to_clip(shape_data(shapes[0], gradients, notes))
    anims = child_anims(holder)
    ind = "    " * depth
    if not anims:
        out.append(f"{ind}<group>")
        out.append(f'{ind}    <clip-path android:pathData="{clip}"/>')
        for c in inner[0]:
            walk(c, em, out, depth + 1, gradients, notes, alpha)
        out.append(f"{ind}</group>")
        return
    if len(anims) != 1 or anims[0].get("type") != "translate":
        raise Unsupported("maschera animata da qualcosa che non e' una traslazione")
    a = anims[0]
    vals = smil_values(a.get("values"), 2)
    dur = parse_time(a.get("dur"))
    interp = em.interpolator(a.get("keySplines"))
    phase = smil_phase(a, dur)
    times = smil_key_times(a)
    move, hold = em.gid("maskmove"), em.gid("maskhold")
    out.append(f'{ind}<group android:name="{move}">')
    out.append(f'{ind}    <clip-path android:pathData="{clip}"/>')
    out.append(f'{ind}    <group android:name="{hold}">')
    for c in inner[0]:
        walk(c, em, out, depth + 2, gradients, notes, alpha)
    out.append(f"{ind}    </group>")
    out.append(f"{ind}</group>")
    for axis, idx in (("translateX", 0), ("translateY", 1)):
        col = [v[idx] for v in vals]
        if any(abs(v) > 1e-9 for v in col):
            em.animate(move, axis, col, dur, interp, phase, times)
            em.animate(hold, axis, [-v for v in col], dur, interp, phase, times)


# ------------------------------------------------------------------- la scala

def with_scale(xml: str, k: float, vw: float, vh: float) -> str:
    """Lo stesso disegno, portato alla taglia comune: un `<group>` che scala tutto
    attorno al centro della scatola.

    **Questa e' la sola cosa che il repo aggiunge al disegno dell'illustratore**, ed e'
    una decisione del committente dell'11 set 2026 che rovescia quella del mattino («la
    scatola e' quella dell'illustratore, senza eccezioni»): Meteocons disegna ogni icona
    alla taglia che le serve, dal 33% della scatola (`smoke-particles`) all'81%
    (`uv-index-11-plus`), e incolonnate a 34 dp nel Cielo quelle differenze si leggono
    come un difetto dell'app. La scala e' `0,75 / lato dell'inchiostro`, misurata in
    [icon_ink.scales] sull'unione degli stili perche' cambiare stile non deve cambiare
    taglia.

    **Il tratto si scala col disegno**, e non e' una svista: meta' della famiglia `line`
    disegna i contorni come anelli riempiti `evenOdd`, non come tratti, e quelli non si
    possono compensare. Compensare gli uni e non gli altri renderebbe la famiglia
    disuniforme al posto delle icone; qui si zooma il disegno intero, pennello compreso.
    Il prezzo, misurato: alla scala massima della lista di spedizione (`smoke-particles`,
    2,29x) la linea e' piu' del doppio di quella di `clear-day`, che resta a 1,00.
    """
    if abs(k - 1.0) < 5e-4:
        return xml
    lines = xml.split(NL)
    open_at = next(i for i, l in enumerate(lines) if "android:viewportHeight=" in l)
    close_at = len(lines) - 1 - next(
        i for i, l in enumerate(reversed(lines)) if l.strip() == "</vector>")
    pad = " " * (len(lines[close_at]) - len(lines[close_at].lstrip()) + 4)
    body = [("    " + l if l.strip() else l) for l in lines[open_at + 1:close_at]]
    group = (f'{pad}<group android:name="mc3scale"'
             f' android:scaleX="{fmt(k)}" android:scaleY="{fmt(k)}"'
             f' android:pivotX="{fmt(vw / 2)}" android:pivotY="{fmt(vh / 2)}">')
    return NL.join(lines[:open_at + 1] + [group] + body + [f"{pad}</group>"]
                   + lines[close_at:])


# ------------------------------------------------------------------------ file

def convert(svg_path: pathlib.Path, drop=()):
    """Un SVG -> (xml statico, xml animato o None, note, interpolatori, viewBox).

    Con [drop] si lasciano indietro dei gruppi per nome. **La finestra non si tocca mai**:
    ogni icona esce nella scatola in cui l'illustratore l'ha disegnata, senza ritagli, e
    il `viewBox` di qui e' il `viewBox` di la'. Un ritaglio per una sola famiglia c'e'
    stato per mezza giornata e non c'e' piu': era un'eccezione da ricordare.

    Quel che **cambia** e' la taglia del disegno DENTRO quella finestra, e non succede
    qui: [with_scale] avvolge il risultato in un gruppo che porta ogni icona allo stesso
    lato d'inchiostro. E' un passo separato apposta, perche' cosi' il set `--original`
    (il riferimento con cui `diff_against_source.py` prova la fedelta') puo' uscire senza.
    """
    root = ET.parse(svg_path).getroot()
    vb = [float(v) for v in root.get("viewBox").split()]
    gradients = {g.get("id"): g for g in root.iter()
                 if g.tag in (SVG_NS + "linearGradient", SVG_NS + "radialGradient")}
    notes: set[str] = set()
    clip_paths = {c.get("id"): c for c in root.iter(SVG_NS + "clipPath")}

    # Due passate sullo stesso albero, e il motivo e' il tratteggio: fermo si ridisegna a
    # segmenti veri, in movimento diventa una finestra di `trimPath` che corre. Tutto il
    # resto esce identico, e per le 463 icone senza tratteggio la seconda passata produce
    # esattamente il disegno della prima.
    roots = [c for c in root if c.tag != SVG_NS + "defs"]

    def pass_(animated):
        em = Emitter(clip_paths, animated=animated, drop=drop)
        body: list[str] = []
        for child in roots:
            walk(child, em, body, 3, gradients, notes)
        return em, body

    em, body = pass_(False)
    em_anim, body_anim = pass_(True)

    inner = NL.join(body_anim)
    static = HEADER + NL.join([
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"',
        '    android:width="24dp"',
        '    android:height="24dp"',
        f'    android:viewportWidth="{fmt(vb[2])}"',
        f'    android:viewportHeight="{fmt(vb[3])}">',
        NL.join(l[8:] if l.startswith(" " * 8) else l for l in body),
        "</vector>",
    ]) + NL

    animated = None
    em = em_anim if em_anim.targets else em
    if em_anim.targets:
        animated = HEADER + NL.join([
            '<animated-vector xmlns:android="http://schemas.android.com/apk/res/android"',
            '    xmlns:aapt="http://schemas.android.com/aapt">',
            '    <aapt:attr name="android:drawable">',
            "        <vector",
            '            android:width="24dp"',
            '            android:height="24dp"',
            f'            android:viewportWidth="{fmt(vb[2])}"',
            f'            android:viewportHeight="{fmt(vb[3])}">',
            inner,
            "        </vector>",
            "    </aapt:attr>",
            NL.join(em_anim.targets),
            "</animated-vector>",
        ]) + NL
    return static, animated, notes, em.interpolators, (vb[2], vb[3])


def write_kotlin(emitted: set) -> int:
    """Le tabelle Kotlin, generate — e **solo per la lista di spedizione**.

    E' questo che tiene l'APK alla misura della lista mentre il repo tiene la famiglia
    intera: un drawable che nessuna tabella nomina non e' referenziato, e
    `shrinkResources` lo toglie dalla release. Crescere la lista e' una riga in
    `tools/shipped_icons.py`.

    Generarle invece di scriverle a mano non e' pigrizia: 143 icone per quattro set piu'
    i gemelli animati sono 595 righe in cui un refuso non si vede, e il tool sa gia' quali
    file ha scritto davvero.
    """
    names = [n for n in shipped.SHIPPED if n.replace("-", "_") in emitted]
    missing = [n for n in shipped.SHIPPED if n.replace("-", "_") not in emitted]
    if missing:
        sys.exit("la lista di spedizione nomina icone che non sono state convertite: "
                 + ", ".join(missing))

    def rows(pairs, indent=8):
        return NL.join(" " * indent + p + "," for p in pairs)

    def pair(prefix_a, prefix_b, n):
        k = n.replace("-", "_")
        return f"R.drawable.{prefix_a}{k} to R.drawable.{prefix_b}{k}"

    anim = [n for n in names if n in shipped.ANIMATED]
    body = f'''package com.callbackdev.chiaro.ui.icons

import androidx.annotation.DrawableRes
import com.callbackdev.chiaro.R

/**
 * Le quattro facce di ogni disegno di Meteocons, e i gemelli che si muovono.
 *
 * **File generato da `tools/import_meteocons_v3.py` — non si modifica a mano.** La lista
 * sta in `tools/shipped_icons.py`; questo e' il suo risultato, ed e' anche la ragione per
 * cui l'APK pesa quanto la lista mentre il repo tiene la famiglia intera: un drawable che
 * nessuna tabella qui nomina non e' referenziato, e `shrinkResources` lo toglie dalla
 * release.
 *
 * La chiave e' sempre l'id del set **line su fondo chiaro**: e' la cucitura su cui
 * `ChiaroIcons` fa girare stile e fondo, la stessa che aveva la v2.
 */
internal object MeteoconsSets {{

    /** line, fondo scuro. */
    val lineDarkOf: Map<Int, Int> = mapOf(
{rows(pair("mc3_", "mc3n_", n) for n in names)}
    )

    /** flat, fondo chiaro. */
    val flatOf: Map<Int, Int> = mapOf(
{rows(pair("mc3_", "mc3f_", n) for n in names)}
    )

    /** flat, fondo scuro. */
    val flatDarkOf: Map<Int, Int> = mapOf(
{rows(pair("mc3_", "mc3fn_", n) for n in names)}
    )

    /**
     * I quattro gemelli animati di un disegno che ne ha, nello stesso ordine in cui si
     * scelgono i set fermi: line chiaro, line scuro, flat chiaro, flat scuro.
     */
    class Moving(
        @DrawableRes val line: Int,
        @DrawableRes val lineDark: Int,
        @DrawableRes val flat: Int,
        @DrawableRes val flatDark: Int
    )

    /**
     * Quali disegni si muovono (DESIGN §7.1): la famiglia delle condizioni e solo quella,
     * perche' il marchio di un tile etichetta una quantita' e un barometro che gira per
     * sempre e' decorazione. `not-available` non c'e': Meteocons lo disegna fermo, ed e'
     * la quantita' di movimento giusta per «non lo sappiamo».
     */
    val movingOf: Map<Int, Moving> = mapOf(
{NL.join(f"        R.drawable.mc3_{n.replace('-', '_')} to Moving(" + NL +
         f"            R.drawable.mc3a_{n.replace('-', '_')}, R.drawable.mc3an_{n.replace('-', '_')}," + NL +
         f"            R.drawable.mc3fa_{n.replace('-', '_')}, R.drawable.mc3fan_{n.replace('-', '_')}" + NL +
         "        )," for n in anim)}
    )

    /**
     * Il nome che l'icona ha a monte, per i test: una tabella che dice «questo codice
     * WMO prende questo disegno» deve poter nominare il disegno come lo nomina
     * l'illustratore, non con un id numerico che non si legge.
     */
    val byName: Map<String, Int> = mapOf(
{rows('"' + n + '" to R.drawable.mc3_' + n.replace("-", "_") for n in names)}
    )
}}
'''
    KOTLIN.parent.mkdir(parents=True, exist_ok=True)
    KOTLIN.write_text(body, encoding="utf8")
    return len(names)


def main() -> int:
    if len(sys.argv) < 2:
        print(__doc__)
        return 2
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    keep_original = "--original" in sys.argv
    src = pathlib.Path(args[0])
    styles = args[1:] or ["line", "flat"]
    OUT.mkdir(parents=True, exist_ok=True)
    INTERPOLATORS.mkdir(parents=True, exist_ok=True)

    # Le scale si misurano **una volta**, sull'unione degli stili: un'icona ha una
    # taglia sola, e leggerla per stile la farebbe cambiare quando il lettore cambia
    # stile in Impostazioni.
    scale_of = ink.scales(src)

    all_interps: set = set()
    emitted: set = set()
    report: dict[str, list] = collections.defaultdict(list)
    counts = collections.Counter()

    for style in styles:
        folder = src / style
        if not folder.is_dir():
            sys.exit(f"manca {folder}")
        # La tavolozza si riancora UNA volta per stile, non per icona: una famiglia di
        # tinte si sposta intera, e l'ordine dentro una nuvola si conserva solo se la
        # regola vede tutti i suoi grigi insieme.
        palette = palette_of(folder)
        maps = {"light": reanchor.reanchor(palette, reanchor.LIGHT),
                "dark": reanchor.reanchor(palette, reanchor.DARK)}
        for ground, m in maps.items():
            kept, bad = reanchor.report(m, reanchor.LIGHT if ground == "light"
                                        else reanchor.DARK)
            counts[f"{style}:{ground}:identici"] = kept
            if bad:
                report[f"SOTTO 3:1 dopo il riancoraggio ({style}/{ground})"] += [
                    f"{a}->{b}" for a, b, _ in bad]

        for svg in sorted(folder.glob("*.svg")):
            stem = svg.stem
            name = stem.replace("-", "_")
            try:
                static, animated, notes, interps, vb = convert(
                    svg, DROP_GROUPS.get(stem, ()))
            except Unsupported as e:
                report[str(e)].append(f"{style}/{stem}")
                counts[style + ":saltate"] += 1
                continue
            except Exception as e:  # una sola icona non ferma le altre cinquecento
                report[f"ERRORE {type(e).__name__}: {e}"].append(f"{style}/{stem}")
                counts[style + ":saltate"] += 1
                continue
            k = scale_of.get(stem, 1.0)
            sized = with_scale(static, k, *vb)
            sized_anim = with_scale(animated, k, *vb) if animated else None
            for ground in ("light", "dark"):
                stat_p, anim_p = PREFIXES[(style, ground)]
                (OUT / f"{stat_p}{name}.xml").write_text(
                    recolor(sized, maps[ground]), encoding="utf8")
                if sized_anim:
                    (OUT / f"{anim_p}{name}.xml").write_text(
                        recolor(sized_anim, maps[ground]), encoding="utf8")
            if keep_original and style in ORIGINAL:
                stat_p, anim_p = ORIGINAL[style]
                (OUT / f"{stat_p}{name}.xml").write_text(static, encoding="utf8")
                if animated:
                    (OUT / f"{anim_p}{name}.xml").write_text(animated, encoding="utf8")
            counts[style + ":statiche"] += 1
            if abs(k - 1.0) >= 5e-4:
                counts[style + ":riportate a 0,75"] += 1
            if style == "line":
                emitted.add(name)
            if animated:
                counts[style + ":animate"] += 1
            all_interps |= interps
            for n in notes:
                report[n].append(f"{style}/{stem}")

    for res, (x1, y1, x2, y2) in sorted(all_interps):
        (INTERPOLATORS / f"{res}.xml").write_text(NL.join([
            '<?xml version="1.0" encoding="utf-8"?>',
            '<pathInterpolator xmlns:android="http://schemas.android.com/apk/res/android"',
            f'    android:controlX1="{x1}" android:controlY1="{y1}"',
            f'    android:controlX2="{x2}" android:controlY2="{y2}"/>',
        ]) + NL, encoding="utf8")

    if "line" in styles and "flat" in styles:
        counts["tabelle Kotlin (spedite)"] = write_kotlin(emitted)
    print("--- convertite")
    for k in sorted(counts):
        print(f"    {k:<26} {counts[k]}")
    print(f"    interpolatori              {len(all_interps)}")
    if report:
        print("--- da sapere")
        for reason, who in sorted(report.items(), key=lambda kv: -len(kv[1])):
            sample = ", ".join(w.split("/")[-1] for w in who[:6])
            more = f" (+{len(who) - 6})" if len(who) > 6 else ""
            print(f"    {len(who):>4}  {reason}")
            print(f"          {sample}{more}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
