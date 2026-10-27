#!/usr/bin/env python3
"""Le icone che mancano (Fase 14, step 14.13): la pioggia e la pioviggine gelata, la pioggia
e la neve possibile.

    python3 tools/compose_falls.py              # scrive i disegni e ComposedFalls.kt
    python3 tools/compose_falls.py --controlla  # non scrive: verifica e basta

**Perche' esiste** (committente, 27 ott 2026). Ogni stato del motore ha un disegno, ma
tre coppie di stati ne condividevano uno in modo che l'occhio non distingue, e DESIGN
vuole che un verdetto sia un glifo e una parola prima che un colore:

- **la pioggia gelata e la pioviggine gelata** (66/67, 56/57) disegnavano la pioggia e la
  pioviggine normali: l'unico pericolo della tabella senza un glifo suo, perche' Meteocons
  non ce l'ha. Qui sono **le gocce di `rain` e di `drizzle` con il cristallo di `snowflake`
  in basso a destra**, sotto la nuvola, dove il ghiaccio si forma: a terra.
- **la pioggia possibile e la neve possibile** (1061, 1071) disegnavano la pioviggine e la
  neve reali: una possibilita' al 60% e una pioviggine che cade erano lo stesso disegno
  nella striscia, dove non ci sono parole. Qui sono **la nuvola di `drizzle` e di `snow` con
  una goccia e un fiocco in meno** (resta la prima e la terza, simmetriche) **e piu'
  chiari**: la possibilita' disegnata come tale.

**Come.** Come `compose_sky_marks.py`, non si disegna niente: si compone. Ogni pezzo e' il
contenuto del gruppo di scala di un disegno gia' importato, **alla lettera** (percorsi e
colori, faccia per faccia), meno i gruppi che la ricetta toglie, dentro due gruppi: quello
della sua scala di famiglia e uno che lo porta dove la ricetta dice, alla taglia che dice.
Il pezzo di base (la pioggia, la pioviggine, la neve) resta **dov'e' e grande com'e'**: il
disegno che esce e' la pioggia di Meteocons con una cosa in piu' o una in meno, non una
pioggia somigliante. Le due cose che cambiano sono dichiarate:

- lo `strokeWidth` di un pezzo rimpicciolito e' diviso per la sua taglia, cosi' il
  cristallo resta disegnato con la linea della famiglia invece che con un filo;
- **la chiarezza** dei disegni «possibile» e' un `strokeAlpha` / `fillAlpha` sui percorsi
  che cadono (quelli con un nome: la nuvola non ne ha), al [LIGHTNESS] voluto **o a quanto
  il 3:1 del fondo permette, se e' di piu'** ([alpha_for]): un segno non testuale deve il
  3:1 al suo fondo (DESIGN §10) e una goccia sbiadita oltre quel limite non si legge. La
  misura, sui fondi delle due vesti (DESIGN §2.5): le gocce scendono al 60% (il limite e'
  al 55% sul chiaro, al 62% sullo scuro, quindi 65%), i fiocchi sul fondo scuro al 60% (il
  limite e' 43%) e **sul fondo chiaro solo all'85%** (il limite e' 81%: il blu-verde dei
  fiocchi sta a 4,2:1 gia' pieno). Sul chiaro la possibilita' della neve la dice il conto
  dei fiocchi, due invece di tre, piu' di quanto la dica il colore; e' scritto nel registro.

**Le animazioni sono della fonte** (DESIGN §7.1: le condizioni si muovono). I `<target>`
del gemello animato della sorgente (`mc3a_rain` e gli altri) sono ritagliati alla lettera
per i gruppi e i percorsi tenuti, con i nomi del composto; nei disegni «possibile» i
fotogrammi dell'alfa sono moltiplicati per la chiarezza, perche' un `objectAnimator`
**sostituisce** la proprieta' e non la somma: senza, la goccia che cade tornerebbe piena
appena si muove. Il cristallo dondola di 6 gradi in 3 secondi, come `snowflake` fa da solo
e con il passo della nuvola sopra di lui: la sorgente lo anima, e il composto se lo porta.

**I nomi sono suoi** (`freezing-rain`, `freezing-drizzle`, `rain-possible`, `snow-possible`,
che in Meteocons non esistono) e le facce sono le otto della famiglia (quattro ferme, quattro
animate). Il file Kotlin che li elenca e' `ui/icons/ComposedFalls.kt`, e lo scrive solo questo
strumento: ha la forma di `ComposedIcons`, con i gemelli animati.

Se l'importatore riscrive le sorgenti (un aggiornamento di Meteocons, un riancoraggio dei
colori), questo strumento va rifatto girare: `ComposedFallsTest` confronta i pezzi con le
loro sorgenti, percorso per percorso, e se ne accorge.
"""
from __future__ import annotations

import argparse
import importlib.util
import pathlib
import re
import sys
import xml.etree.ElementTree as ET

HERE = pathlib.Path(__file__).resolve().parent
DRAWABLE = HERE.parent / "app" / "src" / "main" / "res" / "drawable"
KOTLIN = (HERE.parent / "app" / "src" / "main" / "kotlin" / "com" / "callbackdev"
          / "chiaro" / "ui" / "icons" / "ComposedFalls.kt")
A_URI = "http://schemas.android.com/apk/res/android"
A = "{" + A_URI + "}"
NL = chr(10)


def _load(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


ink = _load("icon_ink", HERE / "icon_ink.py")
fmt = ink.paths.fmt

#: Le facce ferme e il prefisso del loro gemello animato.
FACES = {"mc3_": "mc3a_", "mc3n_": "mc3an_", "mc3f_": "mc3fa_", "mc3fn_": "mc3fan_"}

#: I fondi su cui ogni faccia e' scelta (`IconContrastTest`, DESIGN §2.5: due vesti per
#: fondo, e vale il piu' severo). Copiati da `ui/theme/Scheme.kt`; `ComposedFallsTest`
#: rimisura l'alfa scritto contro gli schemi veri, cosi' un fondo che cambia li' si vede.
GROUNDS = {
    "mc3_": ("#FCF9F3", "#F6FAFF"), "mc3f_": ("#FCF9F3", "#F6FAFF"),
    "mc3n_": ("#16130E", "#0D141B"), "mc3fn_": ("#16130E", "#0D141B"),
}

#: Quanto chiari sono i disegni «possibile»: il 60% dell'inchiostro di quel che cade, dove
#: il fondo lo permette. A 0,5 una goccia nella striscia era un filo grigio; a 0,7 non si
#: distingueva dalla pioviggine se non contando le gocce (guardato a 32, 38 e 52 px).
LIGHTNESS = 0.6

#: Il minimo che un segno non testuale deve al suo fondo (DESIGN §10).
FLOOR = 3.0


class Piece:
    """Un disegno importato, portato a [center] (nella scatola di 128; `None` lascia il
    pezzo dov'e') alla taglia [k] rispetto a come lo disegna il suo file, senza i gruppi o
    i percorsi in [drop]; con [light] i percorsi che cadono sono schiariti."""

    def __init__(self, source: str, k: float = 1.0, center=None, drop=(), light=False):
        self.source = source
        self.k = k
        self.center = center
        self.drop = set(drop)
        self.light = light


#: Le ricette. Il cristallo e' `snowflake` al 28% (largo 26,8 unita' su 128 con il
#: contorno: 7 px nella striscia a 32 dp, 8 nella settimana a 38, 11 nell'eroe del widget a
#: 52) con il centro dell'inchiostro
#: a (108, 113): sotto il lobo destro della nuvola (che a x 108 finisce a y 93,5) e a destra
#: della terza goccia (che finisce a x 82), senza toccare ne' l'una ne' l'altra e senza
#: uscire dalla scatola, contorno compreso. Al 32% toccava il lobo; a sinistra delle gocce
#: si leggeva come una quarta goccia; sotto le gocce ci cadevano dentro mentre si muovono.
ICE = Piece("snowflake", 0.28, (108, 113))

RECIPES = {
    "freezing-rain": [Piece("rain"), ICE],
    "freezing-drizzle": [Piece("drizzle"), ICE],
    "rain-possible": [Piece("drizzle", drop={"g4raindrop2"}, light=True)],
    "snow-possible": [Piece("snow", drop={"g4snowflake2"}, light=True)],
}


def resource(name: str) -> str:
    return name.replace("-", "_")


# ------------------------------------------------------------------- la chiarezza

def _lum(hex6: str) -> float:
    def ch(c):
        c /= 255
        return c / 12.92 if c <= 0.03928 else ((c + 0.055) / 1.055) ** 2.4
    r, g, b = (int(hex6[i:i + 2], 16) for i in (0, 2, 4))
    return 0.2126 * ch(r) + 0.7152 * ch(g) + 0.0722 * ch(b)


def _contrast(a: str, b: str) -> float:
    hi, lo = sorted((_lum(a), _lum(b)), reverse=True)
    return (hi + 0.05) / (lo + 0.05)


def _blend(color: str, ground: str, alpha: float) -> str:
    return "".join(f"{round(int(color[i:i + 2], 16) * alpha + int(ground[i:i + 2], 16) * (1 - alpha)):02x}"
                   for i in (0, 2, 4))


def contrast_at(color: str, prefix: str, alpha: float) -> float:
    """Il contrasto di [color] al [alpha] dato sul fondo peggiore della faccia [prefix]."""
    return min(_contrast(_blend(color.lstrip("#"), g.lstrip("#"), alpha), g.lstrip("#"))
               for g in GROUNDS[prefix])


def alpha_for(color: str, prefix: str) -> float:
    """La chiarezza di un colore che cade sulla faccia [prefix]: [LIGHTNESS], o il primo
    passo di 0,05 sopra che tiene il 3:1 sul fondo peggiore."""
    alpha = LIGHTNESS
    while contrast_at(color, prefix, alpha) < FLOOR:
        alpha = round(alpha + 0.05, 2)
    return alpha


# ------------------------------------------------------------------ leggere il repo

def read_piece(prefix: str, piece: Piece):
    """Il contenuto del gruppo di scala della sorgente, la sua scala, il riquadro del suo
    inchiostro nella scatola (senza contorno) e il mezzo contorno piu' largo."""
    path = DRAWABLE / f"{prefix}{piece.source}.xml"
    root = ET.parse(path).getroot()
    scale = next(g for g in root.iter("group") if g.get(A + "name") == "mc3scale")
    s = float(scale.get(A + "scaleX"))
    assert scale.get(A + "scaleY") == scale.get(A + "scaleX")
    assert float(scale.get(A + "pivotX")) == 64 and float(scale.get(A + "pivotY")) == 64
    scale = ET.fromstring(ET.tostring(scale))
    _prune(scale, piece.drop)
    kept = list(scale)
    box = ink.Box()
    pad = 0.0
    for el in kept:
        for g in el.iter("group"):
            for attr in ("scaleX", "scaleY", "translateX", "translateY", "rotation"):
                assert g.get(A + attr) in (None, "1", "0"), f"{path.name}: gruppo che trasforma"
        for p in el.iter("path"):
            # il contorno si misura a parte: un pezzo rimpicciolito lo tiene alla misura
            # della famiglia (strokeWidth / k), quindi non si rimpicciolisce con lui
            pad = max(pad, float(p.get(A + "strokeWidth", "0")) / 2 * s)
            for poly in ink.flatten(p.get(A + "pathData")):
                box.add([(s * (x - 64) + 64, s * (y - 64) + 64) for x, y in poly])
    return kept, s, box, pad


def _prune(el: ET.Element, drop: set) -> None:
    for child in list(el):
        if child.get(A + "name") in drop:
            el.remove(child)
        else:
            _prune(child, drop)


def falling(el: ET.Element):
    """I percorsi che cadono dentro [el]: quelli con un nome (la nuvola non ne ha, perche'
    l'animazione non ne muove l'alfa)."""
    return [p for p in el.iter("path") if p.get(A + "name")]


def fall_colour(prefix: str, piece: Piece) -> str:
    kept, _, _, _ = read_piece(prefix, piece)
    colours = {p.get(A + "strokeColor") or p.get(A + "fillColor") for el in kept for p in falling(el)}
    assert len(colours) == 1, f"{prefix}{piece.source}: quel che cade ha {len(colours)} colori"
    return colours.pop()


def serialize(el: ET.Element, k: float, suffix: str, indent: str, alpha: float | None) -> str:
    """Un elemento della sorgente, con i nomi resi unici, la linea riportata a misura e,
    se [alpha] e' dato, quel che cade schiarito."""
    el = ET.fromstring(ET.tostring(el))
    for e in el.iter():
        name = e.get(A + "name")
        if name:
            e.set(A + "name", name + suffix)
        sw = e.get(A + "strokeWidth")
        if sw is not None and k != 1.0:
            e.set(A + "strokeWidth", fmt(float(sw) / k))
        if alpha is not None and e.tag == "path" and name:
            e.set(A + ("strokeAlpha" if e.get(A + "strokeColor") else "fillAlpha"), fmt(alpha))
    ET.register_namespace("android", A_URI)
    text = ET.tostring(el, encoding="unicode")
    text = text.replace(' xmlns:android="' + A_URI + '"', "")
    text = re.sub(r"\s+", " ", text).replace(" />", "/>").replace("> <", ">" + NL + "<")
    return NL.join(indent + line for line in text.split(NL))


# --------------------------------------------------------------------- scrivere

HEADER = [
    "<!-- Composto da tools/compose_falls.py da disegni di Meteocons v3",
    "     (github.com/basmilius/meteocons), MIT, (c) Bas Milius —",
    "     licenses/Meteocons-MIT.txt. Non modificare a mano: ri-eseguire lo strumento. -->",
]


def body(name: str, prefix: str, indent: str) -> tuple[list[str], dict]:
    """Le righe dentro il `<vector>`, e per ogni pezzo i nomi tenuti e l'alfa scritto."""
    out = []
    pieces = {}
    for i, piece in enumerate(RECIPES[name], start=1):
        kept, s, box, pad = read_piece(prefix, piece)
        cx = (box.x0 + box.x1) / 2
        cy = (box.y0 + box.y1) / 2
        tx, ty = (0.0, 0.0) if piece.center is None else (piece.center[0] - cx, piece.center[1] - cy)
        alpha = alpha_for(fall_colour(prefix, piece), prefix) if piece.light else None
        out.append(
            f'{indent}<group android:name="piece{i}" android:pivotX="{fmt(cx)}" android:pivotY="{fmt(cy)}"'
            f' android:scaleX="{fmt(piece.k)}" android:scaleY="{fmt(piece.k)}"'
            f' android:translateX="{fmt(tx)}" android:translateY="{fmt(ty)}">'
        )
        out.append(
            f'{indent}    <group android:name="piece{i}scale" android:scaleX="{fmt(s)}" android:scaleY="{fmt(s)}"'
            f' android:pivotX="64" android:pivotY="64">'
        )
        for el in kept:
            out.append(serialize(el, piece.k, f"_{i}", indent + "        ", alpha))
        out.append(f"{indent}    </group>")
        out.append(f"{indent}</group>")
        names = [e.get(A + "name") for el in kept for e in el.iter() if e.get(A + "name")]
        pieces[i] = {"names": names, "alpha": alpha, "box": box, "pad": pad, "piece": piece}
    return out, pieces


def still_xml(name: str, prefix: str) -> str:
    lines, _ = body(name, prefix, "    ")
    return NL.join(HEADER + [
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"',
        '    android:width="24dp"',
        '    android:height="24dp"',
        '    android:viewportWidth="128"',
        '    android:viewportHeight="128">',
        *lines,
        "</vector>",
    ]) + NL


ALPHA_VALUE = re.compile(r'android:value="([-\d.]+)"')


def targets(name: str, prefix: str, pieces: dict) -> list[str]:
    """I `<target>` dei pezzi, ritagliati alla lettera dal gemello animato di ogni
    sorgente che ne ha uno, con i nomi del composto; l'alfa dei pezzi schiariti e'
    moltiplicato per la loro chiarezza."""
    out = []
    for i, info in pieces.items():
        source = DRAWABLE / f"{FACES[prefix]}{info['piece'].source}.xml"
        if not source.exists():
            continue  # un pezzo che Meteocons non anima resta fermo
        text = source.read_text(encoding="utf-8")
        for wanted in info["names"]:
            head = f'<target android:name="{wanted}">'
            if text.count(head) != 1:
                sys.exit(f"{source.name}: non anima {wanted} una volta sola — la sorgente ha cambiato forma")
            start = text.index(head)
            block = text[start:text.index("</target>", start) + len("</target>")]
            block = block.replace(head, f'<target android:name="{wanted}_{i}">')
            if info["alpha"] is not None and re.search(r'android:propertyName="(strokeAlpha|fillAlpha)"', block):
                block = ALPHA_VALUE.sub(
                    lambda m: f'android:value="{fmt(float(m.group(1)) * info["alpha"])}"', block)
            lines = block.splitlines()
            out += ["    " + lines[0]] + lines[1:]
    return out


def animated_xml(name: str, prefix: str) -> str | None:
    lines, pieces = body(name, prefix, "            ")
    moving = targets(name, prefix, pieces)
    if not moving:
        return None
    return NL.join(HEADER + [
        '<animated-vector xmlns:android="http://schemas.android.com/apk/res/android"',
        '    xmlns:aapt="http://schemas.android.com/aapt">',
        '    <aapt:attr name="android:drawable">',
        '        <vector',
        '            android:width="24dp"',
        '            android:height="24dp"',
        '            android:viewportWidth="128"',
        '            android:viewportHeight="128">',
        *lines,
        '        </vector>',
        '    </aapt:attr>',
        *moving,
        '</animated-vector>',
    ]) + NL


# ---------------------------------------------------------------------- le misure

def check(name: str) -> list[str]:
    """Quel che si da' per vero sulla composizione, misurato sulla faccia line chiara."""
    problems = []
    _, pieces = body(name, "mc3_", "")
    boxes = {}
    for i, info in pieces.items():
        b, k, piece, pad = info["box"], info["piece"].k, info["piece"], info["pad"]
        w, h = (b.x1 - b.x0) * k, (b.y1 - b.y0) * k
        if piece.center is None:
            boxes[i] = (b.x0 - pad, b.y0 - pad, b.x1 + pad, b.y1 + pad)
        else:
            boxes[i] = (piece.center[0] - w / 2 - pad, piece.center[1] - h / 2 - pad,
                        piece.center[0] + w / 2 + pad, piece.center[1] + h / 2 + pad)
        x0, y0, x1, y1 = boxes[i]
        if x0 < 0 or y0 < 0 or x1 > 128 or y1 > 128:
            problems.append(f"{name}: il pezzo {i} ({piece.source}) esce dalla scatola: "
                            f"{x0:.1f},{y0:.1f} - {x1:.1f},{y1:.1f}")
    # il cristallo non tocca quel che cade: il riquadro della goccia piu' a destra, con la
    # sua caduta animata di 20 unita' di sorgente, resta a sinistra del cristallo
    if len(pieces) == 2:
        kept, s, _, _ = read_piece("mc3_", pieces[1]["piece"])
        right = 0.0
        for el in kept:
            for p in falling(el):
                pad = float(p.get(A + "strokeWidth", "0")) / 2 * s
                for poly in ink.flatten(p.get(A + "pathData")):
                    right = max(right, max(s * (x - 64) + 64 for x, _ in poly) + pad)
        if right >= boxes[2][0]:
            problems.append(f"{name}: le gocce arrivano a x {right:.1f} e il cristallo comincia a "
                            f"{boxes[2][0]:.1f}: si toccano")
    return problems


def report(name: str) -> str:
    lines = [f"== {name}"]
    for prefix in FACES:
        _, pieces = body(name, prefix, "")
        for i, info in pieces.items():
            piece, b, pad = info["piece"], info["box"], info["pad"]
            w, h = (b.x1 - b.x0) * piece.k + 2 * pad, (b.y1 - b.y0) * piece.k + 2 * pad
            where = "dov'e'" if piece.center is None else f"in {piece.center}"
            line = f"  {prefix:7s} pezzo {i} {piece.source} x{piece.k} {where}: {w:.1f} x {h:.1f} con il contorno"
            if info["alpha"] is not None:
                colour = fall_colour(prefix, piece)
                line += (f"; chiarezza {info['alpha']:.2f} ({colour}: {contrast_at(colour, prefix, info['alpha']):.2f}:1 "
                         f"sul fondo peggiore, pieno {contrast_at(colour, prefix, 1.0):.2f}:1)")
            lines.append(line)
    return NL.join(lines)


# ----------------------------------------------------------------------- il Kotlin

def kotlin() -> str:
    names = list(RECIPES)

    def res(prefix, n):
        return f"R.drawable.{prefix}{resource(n)}"

    def table(doc, val, prefix):
        rows = NL.join(f"        {res('mc3_', n)} to {res(prefix, n)}," for n in names)
        return f"    /** {doc} */{NL}    val {val}: Map<Int, Int> = mapOf({NL}{rows}{NL}    ){NL}"

    moving = NL.join(
        f"        {res('mc3_', n)} to MeteoconsSets.Moving({NL}"
        f"            {res('mc3a_', n)}, {res('mc3an_', n)},{NL}"
        f"            {res('mc3fa_', n)}, {res('mc3fan_', n)}{NL}"
        f"        )," for n in names)
    by_name = NL.join(f'        "{n}" to {res("mc3_", n)},' for n in names)
    return (
        "package com.callbackdev.chiaro.ui.icons" + NL + NL
        + "import androidx.annotation.DrawableRes" + NL
        + "import com.callbackdev.chiaro.R" + NL + NL
        + "/**" + NL
        + " * Le icone che mancavano, che **questo repo compone** dai disegni di Meteocons (Fase 14," + NL
        + " * step 14.13, 27 ott 2026): la pioggia e la pioviggine gelata (quel che cade piu' il" + NL
        + " * cristallo di `snowflake` in basso, dove il ghiaccio si forma), la pioggia e la neve" + NL
        + " * possibile (la nuvola di `drizzle` e di `snow` con una goccia e un fiocco in meno, e" + NL
        + " * piu' chiari). Il perche', con le misure, sta in testa allo strumento." + NL
        + " *" + NL
        + " * **File generato da `tools/compose_falls.py` — non si modifica a mano.** Ha la" + NL
        + " * forma di [ComposedIcons] (la chiave e' l'id del set line su fondo chiaro), gemelli" + NL
        + " * animati compresi: le gocce e i fiocchi si muovono come nella loro sorgente." + NL
        + " */" + NL
        + "internal object ComposedFalls {" + NL + NL
        + table("line, fondo scuro.", "lineDarkOf", "mc3n_") + NL
        + table("flat, fondo chiaro.", "flatOf", "mc3f_") + NL
        + table("flat, fondo scuro.", "flatDarkOf", "mc3fn_") + NL
        + "    /** I quattro gemelli animati, nello stesso ordine dei set fermi. */" + NL
        + f"    val movingOf: Map<Int, MeteoconsSets.Moving> = mapOf({NL}{moving}{NL}    ){NL}{NL}"
        + "    /** Per nome, come [MeteoconsSets.byName]. */" + NL
        + f"    val byName: Map<String, Int> = mapOf({NL}{by_name}{NL}    ){NL}{NL}"
        + "    /** La pioggia gelata (66, 67) e la pioviggine gelata (56, 57). */" + NL
        + "    @DrawableRes" + NL
        + "    val freezingRain: Int = R.drawable.mc3_freezing_rain" + NL + NL
        + "    @DrawableRes" + NL
        + "    val freezingDrizzle: Int = R.drawable.mc3_freezing_drizzle" + NL + NL
        + "    /** La pioggia possibile (1061) e la neve possibile (1071). */" + NL
        + "    @DrawableRes" + NL
        + "    val rainPossible: Int = R.drawable.mc3_rain_possible" + NL + NL
        + "    @DrawableRes" + NL
        + "    val snowPossible: Int = R.drawable.mc3_snow_possible" + NL
        + "}" + NL
    )


# ------------------------------------------------------------------------ il giro

def main():
    parser = argparse.ArgumentParser(description=__doc__.split(NL)[0])
    parser.add_argument("--controlla", action="store_true", help="verifica senza scrivere")
    args = parser.parse_args()
    problems = [p for name in RECIPES for p in check(name)]
    if problems:
        sys.exit("la composizione non si scrive:" + NL + NL.join(f"  - {p}" for p in problems))
    outputs = {}
    for name in RECIPES:
        for prefix, moving in FACES.items():
            outputs[DRAWABLE / f"{prefix}{resource(name)}.xml"] = still_xml(name, prefix)
            twin = animated_xml(name, prefix)
            if twin is not None:
                outputs[DRAWABLE / f"{moving}{resource(name)}.xml"] = twin
    outputs[KOTLIN] = kotlin()
    # UTF-8 esplicito e fine riga indifferenti, come compose_sky_marks.py (25 ott 2026).
    stale = [p for p, text in outputs.items()
             if not p.exists() or p.read_bytes().replace(b"\r\n", b"\n") != text.encode("utf-8")]
    if args.controlla:
        for p in stale:
            print(f"da rigenerare: {p.relative_to(HERE.parent)}")
        sys.exit(1 if stale else 0)
    for p, text in outputs.items():
        p.write_text(text, encoding="utf-8")
    for name in RECIPES:
        print(report(name))
    print(f"scritti {len(outputs)} file")


if __name__ == "__main__":
    main()
