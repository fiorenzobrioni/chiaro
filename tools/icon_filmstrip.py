#!/usr/bin/env python3
"""Renders the animated icons as filmstrips, for the step no test can do: looking.

`AnimatedIconTest` can say that a moving icon is the still icon plus valid animators
aimed at elements that exist. It cannot say that the rain falls DOWNWARD. This can: it
evaluates every `objectAnimator` at a series of instants, applies the result to the
drawing the way a VectorDrawable would, and writes the frames out as SVG.

It reads the shipped `<animated-vector>` XML, never a copy of it — the same rule
`tools/palette_sheet.py` follows, and for the same reason.

    python3 tools/icon_filmstrip.py > /tmp/icons.html
    python3 tools/icon_filmstrip.py mcafn_ > /tmp/icons.html      # one set
    chromium --headless --screenshot=/tmp/icons.png --window-size=780,1180 file:///tmp/icons.html

**Il foglio** (Fase 14, step 14.13): i disegni nominati, fermi, nelle quattro facce sul loro
fondo, alle taglie date in px (la striscia, la settimana, l'eroe del widget: 32, 38, 52), uno
accanto all'altro, e sotto la striscia animata di ognuno. E' la prova che nessun test fa:
guardare un disegno nuovo accanto ai vecchi alla misura in cui il lettore lo vede.

    python3 tools/icon_filmstrip.py --foglio 32,38,52 rain freezing-rain > /tmp/foglio.html
"""
from __future__ import annotations

import pathlib
import re
import sys
import xml.etree.ElementTree as ET

DRAWABLE = pathlib.Path("app/src/main/res/drawable")
ANDROID = "{http://schemas.android.com/apk/res/android}"
AAPT = "{http://schemas.android.com/aapt}"

#: How many frames a strip holds, and how much of a second they span. One second shows
#: a raindrop landing (0.7 s) and a whole flicker of lightning (2 s), and reduces the
#: 45-second sun to the one thing worth checking about it: that it turns at all.
FRAMES = 9
SPAN = 1.0

#: The ground each set is drawn on here, so a dark-ground set is looked at on one.
GROUNDS = {"mca_": "#F6FAFF", "mcaf_": "#F6FAFF", "mcafn_": "#0D141B", "mcan_": "#0D141B",
           # Fase 13: le icone v3 dello spike, guardate su entrambe le superfici vere
           # dell'app (DESIGN §2.2) perche' portano i colori ORIGINALI di Meteocons.
           "spike_": "#FCF9F3", "spike-dark_": "#16130E"}


def attr(el: ET.Element, name: str, default=None):
    return el.get(ANDROID + name, default)


def animators(target: ET.Element) -> list[ET.Element]:
    """Every objectAnimator under one <target>, set or not."""
    return [el for el in target.iter() if el.tag == "objectAnimator"]


def value_at(animator: ET.Element, seconds: float) -> tuple[str, float]:
    duration = int(attr(animator, "duration")) / 1000
    fraction = (seconds % duration) / duration
    holder = animator.find("propertyValuesHolder")
    if holder is None:
        return attr(animator, "propertyName"), (
            float(attr(animator, "valueFrom"))
            + (float(attr(animator, "valueTo")) - float(attr(animator, "valueFrom"))) * fraction
        )
    keys = [(float(attr(k, "fraction")), float(attr(k, "value"))) for k in holder]
    for (fa, va), (fb, vb) in zip(keys, keys[1:]):
        if fa <= fraction <= fb:
            span = fb - fa
            return attr(holder, "propertyName"), va + (vb - va) * ((fraction - fa) / span if span else 0)
    return attr(holder, "propertyName"), keys[-1][1]


def state(root: ET.Element, seconds: float) -> dict[str, dict[str, float]]:
    """Every named element's animated properties at this instant."""
    out: dict[str, dict[str, float]] = {}
    for target in root.findall("target"):
        name = attr(target, "name")
        for animator in animators(target):
            prop, value = value_at(animator, seconds)
            out.setdefault(name, {})[prop] = value
    return out


def svg_shape(el: ET.Element, now: dict, out: list[str], clips: list[str]) -> None:
    tag = el.tag
    if tag == "clip-path":
        clips.append(attr(el, "pathData"))
        return
    name = attr(el, "name")
    animated = now.get(name, {})
    if tag == "group":
        px, py = float(attr(el, "pivotX", 0)), float(attr(el, "pivotY", 0))
        # Un gruppo puo' portare la trasformazione scritta nell'XML, quella animata, o
        # tutte e due: dalla Fase 13 l'importatore emette anche quelle statiche (i
        # `transform` di Figma, e il riquadro dei ritagli), e leggendo solo le animate
        # questo strumento disegnava il vuoto.
        #
        # **La animata SOSTITUISCE la statica, non ci si somma** (12 set 2026). Un
        # `objectAnimator` chiama il setter della proprieta' del gruppo, quindi il valore
        # scritto nell'XML e' solo il punto di partenza finche' l'animazione non parte.
        # Questo strumento le sommava, il che sui file dell'importatore non si vedeva —
        # nessun gruppo animato porta anche una trasformazione statica — e ha lasciato
        # passare una nuvola composta che sul telefono partiva per la tangente. Lo
        # strumento esiste per guardare: se mente, non serve a niente.
        tx = animated.get("translateX", float(attr(el, "translateX", 0)))
        ty = animated.get("translateY", float(attr(el, "translateY", 0)))
        rotation = animated.get("rotation", float(attr(el, "rotation", 0)))
        sx = animated.get("scaleX", float(attr(el, "scaleX", 1)))
        sy = animated.get("scaleY", float(attr(el, "scaleY", 1)))
        # The order a VectorDrawable composes a group in: about the pivot, then moved.
        out.append(
            f'<g transform="translate({px + tx} {py + ty}) rotate({rotation}) '
            f'scale({sx} {sy}) translate({-px} {-py})">'
        )
        inner_clips: list[str] = []
        body: list[str] = []
        for child in el:
            svg_shape(child, now, body, inner_clips)
        if inner_clips:
            clip_id = f"c{len(out)}{abs(hash(inner_clips[0])) % 9999}"
            out.append(f'<clipPath id="{clip_id}"><path d="{inner_clips[0]}"/></clipPath>')
            out.append(f'<g clip-path="url(#{clip_id})">')
            out += body
            out.append("</g>")
        else:
            out += body
        out.append("</g>")
        return
    if tag != "path":
        sys.exit(f"unhandled element <{tag}> in an animated vector")
    bits = [f'd="{attr(el, "pathData")}"']
    fill = attr(el, "fillColor")
    bits.append(f'fill="{fill}"' if fill else 'fill="none"')
    # `fillType="evenOdd"` non e' un dettaglio: nella famiglia `line` di Meteocons v3 un
    # contorno E' un anello disegnato con quella regola, e disegnarlo senza la riempie
    # piena. Questo strumento lo ignorava, e faceva sembrare diverse dalla loro sorgente
    # tutte le nuvole (trovato con la griglia in differenza, 11 set 2026).
    if attr(el, "fillType") == "evenOdd":
        bits.append('fill-rule="evenodd"')
    # L'alfa scritto nell'XML e' il punto di partenza, come una trasformazione statica
    # (14.13: i disegni «possibile» portano le gocce e i fiocchi piu' chiari); l'animato lo
    # sostituisce.
    if fill and ("fillAlpha" in animated or attr(el, "fillAlpha")):
        bits.append(f'fill-opacity="{animated.get("fillAlpha", attr(el, "fillAlpha"))}"')
    stroke = attr(el, "strokeColor")
    if stroke:
        bits.append(f'stroke="{stroke}"')
        bits.append(f'stroke-width="{attr(el, "strokeWidth", "1")}"')
        for svg_name, avd_name in (("stroke-linecap", "strokeLineCap"),
                                   ("stroke-linejoin", "strokeLineJoin"),
                                   ("stroke-miterlimit", "strokeMiterLimit")):
            if attr(el, avd_name):
                bits.append(f'{svg_name}="{attr(el, avd_name)}"')
        if "strokeAlpha" in animated or attr(el, "strokeAlpha"):
            bits.append(f'stroke-opacity="{animated.get("strokeAlpha", attr(el, "strokeAlpha"))}"')
        # `trimPath*` torna a essere il tratteggio da cui e' venuto (Fase 13). Con
        # `pathLength="1"` le frazioni di Android SONO le unita' del dasharray, quindi
        # la finestra si ridisegna senza misurare niente.
        start = float(attr(el, "trimPathStart", 0) or 0)
        end = float(attr(el, "trimPathEnd", 1) or 1)
        offset = animated.get("trimPathOffset",
                              float(attr(el, "trimPathOffset", 0) or 0))
        if (start, end) != (0.0, 1.0) or offset:
            span = max(0.0, end - start)
            bits.append('pathLength="1"')
            bits.append(f'stroke-dasharray="{span} {max(1e-6, 1 - span)}"')
            bits.append(f'stroke-dashoffset="{-(start + offset)}"')
    out.append("<path " + " ".join(bits) + "/>")


def frame(path: pathlib.Path, seconds: float, size: int = 52) -> str:
    root = ET.parse(path).getroot()
    vector = root if root.tag == "vector" else root.find(f"{AAPT}attr/vector")
    if vector is None:
        sys.exit(f"{path.name}: no inlined <vector> — did the emitter change shape?")
    now = state(root, seconds) if root.tag != "vector" else {}
    body: list[str] = []
    clips: list[str] = []
    for child in vector:
        svg_shape(child, now, body, clips)
    vw = attr(vector, "viewportWidth", "64")
    vh = attr(vector, "viewportHeight", "64")
    return (f'<svg viewBox="0 0 {vw} {vh}" width="{size}" height="{size}">'
            + "".join(body) + "</svg>")


#: Le quattro facce ferme sul fondo per cui sono scelte (DESIGN §2.5: la veste piu' severa
#: fra le due), e il gemello animato di ognuna.
FACES = {"mc3_": ("#FCF9F3", "mc3a_"), "mc3n_": ("#16130E", "mc3an_"),
         "mc3f_": ("#FCF9F3", "mc3fa_"), "mc3fn_": ("#16130E", "mc3fan_")}


def sheet(sizes: list[int], names: list[str]) -> None:
    """Il foglio: i disegni [names] fermi, nelle quattro facce, a ogni taglia di [sizes]; poi
    le strisce animate di ognuno a 52 px, dove c'e' un gemello."""
    stems = [n.replace("-", "_") for n in names]
    print("<html><head><meta charset=utf-8><style>"
          "body{font-family:system-ui;margin:0;background:#222;color:#ddd}"
          "h2{font-size:13px;margin:18px 12px 6px;letter-spacing:.06em;text-transform:uppercase}"
          "table{border-collapse:collapse;margin:0 12px}"
          "td,th{padding:3px 6px;font-size:11px;font-weight:normal;text-align:center;vertical-align:middle}"
          "th.n{text-align:left;opacity:.75;width:60px}"
          ".row{display:flex;align-items:center;gap:2px;padding:2px 12px}"
          ".n{width:190px;font-size:11px;opacity:.75}"
          "</style></head><body>")
    for size in sizes:
        print(f"<h2>fermi a {size} px</h2><table><tr><th></th>"
              + "".join(f"<th>{n}</th>" for n in names) + "</tr>")
        for prefix, (ground, _) in FACES.items():
            print(f'<tr><th class=n>{prefix}</th>')
            for stem in stems:
                path = DRAWABLE / f"{prefix}{stem}.xml"
                if not path.exists():
                    sys.exit(f"{path.name} manca — esegui lo strumento che lo compone")
                print(f'<td style="background:{ground}">{frame(path, 0.0, size)}</td>')
            print("</tr>")
        print("</table>")
    print("<h2>animati, un secondo in nove fotogrammi, a 52 px</h2>")
    for prefix, (ground, moving) in FACES.items():
        for stem in stems:
            path = DRAWABLE / f"{moving}{stem}.xml"
            if not path.exists():
                continue
            cells = "".join(
                f'<span style="background:{ground};display:inline-block;line-height:0">'
                + frame(path, i * SPAN / (FRAMES - 1)) + "</span>"
                for i in range(FRAMES)
            )
            print(f'<div class=row><span class=n>{path.stem}</span>{cells}</div>')
    print("</body></html>")


def main() -> None:
    sys.stdout.reconfigure(encoding="utf-8")
    if not DRAWABLE.is_dir():
        sys.exit(f"run me from the repo root: {DRAWABLE} not found")
    print("<html><head><meta charset=utf-8><style>"
          "body{font-family:system-ui;margin:0;background:#222;color:#ddd}"
          "h2{font-size:13px;margin:18px 12px 6px;letter-spacing:.06em;text-transform:uppercase}"
          ".row{display:flex;align-items:center;gap:2px;padding:2px 12px}"
          ".n{width:190px;font-size:11px;opacity:.75}"
          "</style></head><body>")
    if sys.argv[1:2] == ["--foglio"]:
        sheet([int(s) for s in sys.argv[2].split(",")], sys.argv[3:])
        return
    wanted = sys.argv[1:] or [p for p in GROUNDS if not p.startswith("spike")]
    unknown = [p for p in wanted if p not in GROUNDS]
    if unknown:
        sys.exit(f"no such set: {unknown} — pick from {list(GROUNDS)}")
    for prefix in wanted:
        ground = GROUNDS[prefix]
        glob = "spike_*_anim.xml" if prefix.startswith("spike") else f"{prefix}*.xml"
        files = sorted(DRAWABLE.glob(glob))
        if not files:
            sys.exit(f"no {glob} — run the importer first")
        print(f"<h2>{prefix}* on {ground}</h2>")
        for path in files:
            cells = "".join(
                f'<span style="background:{ground};display:inline-block;line-height:0">'
                + frame(path, i * SPAN / (FRAMES - 1)) + "</span>"
                for i in range(FRAMES)
            )
            print(f'<div class=row><span class=n>{path.stem}</span>{cells}</div>')
    print("</body></html>")


if __name__ == "__main__":
    main()
