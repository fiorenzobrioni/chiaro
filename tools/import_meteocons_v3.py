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
SVG_NS = "{http://www.w3.org/2000/svg}"
NL = chr(10)


def _load(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


paths = _load("svg_paths", HERE / "svg_paths.py")
#: La fase negativa e la rotazione dei keyframe NON sono novita' di v3: le ha risolte
#: l'importatore della v2 e le sue funzioni valgono qui identiche. Riusarle e' anche la
#: prova che il passaggio alla v3 e' un trasloco, non una riscrittura.
v2 = _load("v2", HERE / "import_meteocons.py")

fmt = paths.fmt

#: prefisso statico, prefisso animato — per stile.
PREFIXES = {
    "line": ("mc3_", "mc3a_"),
    "flat": ("mc3f_", "mc3fa_"),
    "fill": ("mc3p_", "mc3pa_"),
    "monochrome": ("mc3m_", "mc3ma_"),
}

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
    def __init__(self, clips=None):
        self.targets: list[str] = []
        self.interpolators: set = set()
        self.clips = clips or {}
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


def shape_data(el, gradients, notes):
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
        if el.get("stroke-dasharray"):
            # VectorDrawable non ha il tratteggio. v2 ridisegnava i tratteggi come
            # segmenti veri (dipartenza n. 3); qui si dichiara e basta, perche' un
            # tratto reso solido e' un disegno diverso e chi legge il rapporto deve
            # saperlo.
            notes.add("tratteggio reso solido")
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
        wrappers = transform_groups(el.get("transform")) if el.get("transform") else []
        clip_shape = canvas_clip(el, em.clips)
        if wrappers or clip_shape is not None:
            for w in wrappers:
                out.append("    " * depth + f"<group {w}>")
                depth += 1
            if clip_shape is not None:
                out.append("    " * depth + "<group>")
                out.append("    " * (depth + 1) + '<clip-path android:pathData="'
                           + shape_data(clip_shape, gradients, notes) + '"/>')
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
        for a in list(fades):
            if a.get("attributeName") == "stroke-dashoffset":
                # Le formiche in marcia di un tratto tratteggiato. VectorDrawable non ha
                # il tratteggio (vedi `paint`), quindi non c'e' niente da far marciare:
                # si perde l'animazione, non l'icona, e lo si dichiara.
                notes.add("animazione del tratteggio persa")
                fades.remove(a)
            elif a.get("attributeName") != "opacity":
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
        pname = em.gid("p") if alpha else None
        bits = [f'{ind}<path android:pathData="{d}"']
        if pname:
            bits.append(f'{ind}    android:name="{pname}"')
        bits += [f"{ind}    {a}" for a in attrs]
        out.append(NL.join(bits) + "/>")
        if pname:
            _fade(el, pname, em, alpha, attrs)
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


def _fade(el, pname, em, alpha, attrs):
    has_fill = any(a.startswith("android:fillColor") for a in attrs)
    has_stroke = any(a.startswith("android:strokeColor") for a in attrs)
    for a in alpha:
        controls = [float(v) for v in a.get("values").split(";")]
        dur = parse_time(a.get("dur"))
        interp = em.interpolator(a.get("keySplines"))
        for prop, present in (("fillAlpha", has_fill), ("strokeAlpha", has_stroke)):
            if present:
                em.animate(pname, prop, controls, dur, interp,
                           smil_phase(a, dur), smil_key_times(a))


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


# ------------------------------------------------------------------------ file

def convert(svg_path: pathlib.Path):
    """Un SVG -> (xml statico, xml animato o None, note, interpolatori)."""
    root = ET.parse(svg_path).getroot()
    vb = [float(v) for v in root.get("viewBox").split()]
    gradients = {g.get("id"): g for g in root.iter()
                 if g.tag in (SVG_NS + "linearGradient", SVG_NS + "radialGradient")}
    notes: set[str] = set()
    clip_paths = {c.get('id'): c for c in root.iter(SVG_NS + 'clipPath')}
    em = Emitter(clip_paths)
    body: list[str] = []
    for child in root:
        if child.tag == SVG_NS + "defs":
            continue
        walk(child, em, body, 3, gradients, notes)

    inner = NL.join(body)
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
    if em.targets:
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
            NL.join(em.targets),
            "</animated-vector>",
        ]) + NL
    return static, animated, notes, em.interpolators


def main() -> int:
    if len(sys.argv) < 2:
        print(__doc__)
        return 2
    src = pathlib.Path(sys.argv[1])
    styles = sys.argv[2:] or ["line", "flat"]
    OUT.mkdir(parents=True, exist_ok=True)
    INTERPOLATORS.mkdir(parents=True, exist_ok=True)

    all_interps: set = set()
    report: dict[str, list] = collections.defaultdict(list)
    counts = collections.Counter()

    for style in styles:
        folder = src / style
        if not folder.is_dir():
            sys.exit(f"manca {folder}")
        stat_p, anim_p = PREFIXES[style]
        for svg in sorted(folder.glob("*.svg")):
            name = svg.stem.replace("-", "_")
            try:
                static, animated, notes, interps = convert(svg)
            except Unsupported as e:
                report[str(e)].append(f"{style}/{svg.stem}")
                counts[style + ":saltate"] += 1
                continue
            except Exception as e:  # un errore vero, ma una sola icona non ferma le altre
                report[f"ERRORE {type(e).__name__}: {e}"].append(f"{style}/{svg.stem}")
                counts[style + ":saltate"] += 1
                continue
            (OUT / f"{stat_p}{name}.xml").write_text(static, encoding="utf8")
            counts[style + ":statiche"] += 1
            if animated:
                (OUT / f"{anim_p}{name}.xml").write_text(animated, encoding="utf8")
                counts[style + ":animate"] += 1
            all_interps |= interps
            for n in notes:
                report[n].append(f"{style}/{svg.stem}")

    for res, (x1, y1, x2, y2) in sorted(all_interps):
        (INTERPOLATORS / f"{res}.xml").write_text(NL.join([
            '<?xml version="1.0" encoding="utf-8"?>',
            '<pathInterpolator xmlns:android="http://schemas.android.com/apk/res/android"',
            f'    android:controlX1="{x1}" android:controlY1="{y1}"',
            f'    android:controlX2="{x2}" android:controlY2="{y2}"/>',
        ]) + NL, encoding="utf8")

    print("--- convertite")
    for k in sorted(counts):
        print(f"    {k:<22} {counts[k]}")
    print(f"    interpolatori          {len(all_interps)}")
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
