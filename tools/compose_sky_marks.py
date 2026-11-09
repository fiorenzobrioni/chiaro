#!/usr/bin/env python3
"""I segni del cielo che Meteocons non ha: la meteora, la luna con la stella, le due stelle,
il sole sotto l'orizzonte con le prime stelle, le tre stelle, la luna piena che sorge,
l'eclissi di luna, la luce zodiacale.

    python3 tools/compose_sky_marks.py              # scrive i disegni e SkyMarks.kt
    python3 tools/compose_sky_marks.py --controlla  # non scrive: verifica e basta

**Perche' esiste** (revisione della schermata Cielo, punto 7, 14 ott 2026). Il catalogo del
cielo ha sessanta momenti e la famiglia di Meteocons pochi disegni per loro, e due cose si
leggevano male:

- **gli sciami** portavano `falling-stars`, che e' una falce di luna attraversata dalla
  scia: in riga, cinque sciami di fila, si leggeva come una luna barrata, cioe' come
  «niente luna», il contrario di quel che dice. La **meteora** e' lo stesso disegno senza
  la luna: le tre stelle e la scia, e nient'altro.
- **la stella** diceva cinque cose diverse: le ore blu, i crepuscoli nautici, Venere,
  Giove e le tre congiunzioni. Le congiunzioni sono le sole fra queste che sono **due
  cose in un punto**, e il disegno puo' dirlo: la **luna con la stella** (la luna accanto
  a Venere o a Giove) e le **due stelle** (Venere accanto a Giove). Venere e Giove soli
  restano la stella: «la stella della sera» e' il nome che hanno per chi li guarda, e un
  disco inventato sarebbe un pianeta che a occhio nudo nessuno vede. I crepuscoli
  nautici restano la stella anche loro: sono il cielo delle prime stelle nel blu.
- **le ore blu** (committente, 25 ott 2026) dicevano la stella anche loro, la stessa di
  Venere e del crepuscolo nautico, e non dicevano la cosa che le definisce: il sole
  appena sotto l'orizzonte (da −4° a −6°) con le prime stelle. Meteocons non ha quel
  disegno, ma ha i pezzi: il **sole sotto l'orizzonte con la prima stella** e' la cupola
  di `horizon` senza i raggi (il sole appena sceso sotto la linea) con sopra una stella
  di `star`. Cosi' la scala del giorno sale a gradini leggibili: il sole (ora d'oro), il
  sole sulla linea (tramonto), il sole sotto la linea con la stella (ora blu), la stella
  sola (crepuscolo nautico), le stelle e la luna (buio). Scelto fra cinque composizioni
  rese a 51, 32 e 20 px su entrambi i fondi accanto ai vicini della lista: la stella in
  alto a destra con la cupola in basso era sbilanciata, il sole con i raggi piu' la
  stella troppo vicino al tramonto, la cupola senza linea un arco sotto una stella, la
  sola linea con la stella una stella sottolineata.

**La revisione del 9 nov 2026** (committente: «alcune non mi convincono, tipo stelle cadenti e
ora blu. Verificale tutte»). Riletti i sessanta momenti contro i loro disegni, sei dicevano il
contrario dell'evento o non lo distinguevano da un altro, e si cambiano:

- **la meteora** era le stelle a meta' di una scia che attraversa tutta la scatola: in riga si
  leggeva come un barrato, a 20 px un trattino. Ora e' il segno che tutti riconoscono: una
  stella in testa e tre scie dietro, che cade verso il basso a sinistra;
- **l'ora blu** era la cupola sopra la linea con la stella in cima: si leggeva come una pedina
  o un trofeo, e la cupola sopra la linea e' il sole ancora sopra l'orizzonte, mentre l'ora
  blu e' il sole da −4° a −6° sotto. Ora il sole sta sotto la linea, tratteggiato come la luna
  nuova (c'e' ma non si vede), con le prime due stelle sopra. Lo prendono anche **le notti
  bianche**, che erano la notte stellata con la luna: sono le notti che non diventano mai buie,
  con il sole appena sotto l'orizzonte per tutta la notte;
- **le tre stelle** sono il buio pieno e il centro della Via Lattea, che erano la notte con la
  grande falce di luna: il buio pieno e' per definizione la notte astronomica con la luna
  sotto l'orizzonte, e la luna e' quel che il centro della Via Lattea non sopporta;
- **la luna piena che sorge** e' la luna piena al crepuscolo, che era la falce di `moonrise`:
  lo stesso sorgere, con il disco pieno al posto della falce;
- **l'eclissi di luna** era la luna piena, la stessa riga due volte nella lista: ora e' la
  luna piena con l'ombra della Terra in rosso, la luna «di sangue»;
- **la luce zodiacale** era `horizon`, un sole con i raggi per una luce che si vede solo nel
  buio pieno, e lo stesso disegno degli equinozi: ora e' un cono di luce che sale storto
  dall'orizzonte, sotto due stelle. E' la piu' debole delle sei, detto al committente.

Per questo lo strumento, dal 9 nov 2026, **disegna anche qualcosa** oltre a comporre: i tratti
che nessun disegno di Meteocons ha (le scie, la linea dell'orizzonte, il sole tratteggiato, il
cono, l'ombra). Con due regole: un tratto ha la linea della famiglia (4 unita' nella scatola di
128, quella di `horizon` e della scia di `falling-stars`), e **i colori non si inventano**: ogni
tratto prende il suo da un disegno della famiglia nella stessa faccia (la stella da `star`, la
linea e il sole da `horizon`, il rosso dagli avvisi di `code-red`), e `SkyMarksTest` lo
controlla faccia per faccia.

**Come.** Come `compose_sun_cloud.py`, i pezzi non si disegnano: si compongono. Ogni pezzo e' il
contenuto di un disegno gia' importato, **alla lettera** (percorsi e colori, faccia per
faccia), dentro due gruppi: quello della sua scala di famiglia (la stessa del file da cui
viene, sullo stesso perno) e uno che lo porta dove la ricetta dice, alla taglia che dice.
L'unica cosa che cambia e' lo `strokeWidth`, diviso per la taglia del pezzo: cosi' una
stella rimpicciolita resta disegnata con la linea della famiglia invece che con un filo.

**I nomi sono suoi** (`meteor`, `moon-star`, `two-stars`, che in Meteocons non esistono) e le
facce sono le quattro ferme della famiglia (`mc3_`, `mc3n_`, `mc3f_`, `mc3fn_`). Non ci sono
gemelli animati: nel catalogo del cielo non si muove niente, si muovono solo le condizioni.
Il file Kotlin che li elenca e' `ui/icons/SkyMarks.kt`, e lo scrive solo questo strumento.

Se l'importatore riscrive le sorgenti (un aggiornamento di Meteocons, un riancoraggio dei
colori), questo strumento va rifatto girare: `SkyMarksTest` confronta i pezzi con le loro
sorgenti e se ne accorge.
"""
from __future__ import annotations

import argparse
import importlib.util
import math
import pathlib
import re
import sys
import xml.etree.ElementTree as ET

HERE = pathlib.Path(__file__).resolve().parent
DRAWABLE = HERE.parent / "app" / "src" / "main" / "res" / "drawable"
KOTLIN = (HERE.parent / "app" / "src" / "main" / "kotlin" / "com" / "callbackdev"
          / "chiaro" / "ui" / "icons" / "SkyMarks.kt")
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

#: Le quattro facce ferme, con il loro ruolo nelle tabelle di `SkyMarks.kt`.
FACES = ("mc3_", "mc3n_", "mc3f_", "mc3fn_")


class Piece:
    """Un disegno importato, portato a [center] (nella scatola di 128) alla taglia [k]
    rispetto a come lo disegna il suo file, senza i gruppi o i percorsi in [drop]. Senza
    [center] resta dove lo disegna il suo file, alla sua taglia."""

    def __init__(self, source: str, k: float = 1.0, center: tuple[float, float] | None = None, drop=(),
                 swap: tuple[str, str, tuple[float, float]] | None = None):
        assert center is not None or k == 1.0, "un pezzo che resta al suo posto resta alla sua taglia"
        self.source = source
        self.k = k
        self.center = center
        #: Nomi di gruppi o percorsi da togliere, a qualunque profondita' (i raggi di
        #: `horizon` stanno dentro il gruppo del ritaglio, 25 ott 2026).
        self.drop = set(drop)
        #: (nome, sorgente, spostamento): il gruppo [nome] del pezzo prende il posto del
        #: gruppo omonimo di un'altra sorgente della stessa faccia, spostato di tanto nella
        #: scala di famiglia, che le due devono avere uguale (9 nov 2026: la falce di
        #: `moonrise` lascia il posto al disco di `moon-full`, dentro lo stesso ritaglio).
        self.swap = swap


#: La linea della famiglia nella scatola di 128: quella di `horizon` e della scia di
#: `falling-stars`. Un tratto disegnato qui ha questa, mai un filo.
LINE = 4.0


class Paint:
    """Un colore preso da un disegno della famiglia nella stessa faccia: il [index]-esimo
    `strokeColor` o `fillColor` di [source], nell'ordine del file. Mai un esadecimale scritto
    qui (9 nov 2026)."""

    def __init__(self, source: str, index: int = 0):
        self.source = source
        self.index = index

    def of(self, prefix: str) -> str:
        text = (DRAWABLE / f"{prefix}{self.source}.xml").read_text(encoding="utf-8")
        return re.findall(r'android:(?:strokeColor|fillColor)="(#[0-9a-fA-F]+)"', text)[self.index]


class Stroke:
    """Un tratto disegnato (9 nov 2026): una linea o un arco nella linea della famiglia, con
    le estremita' tonde, nel colore di [paint]."""

    def __init__(self, d: str, paint: Paint, what: str):
        self.d = d
        self.paint = paint
        self.what = what


class Field:
    """Una forma piena disegnata (9 nov 2026): il colore di [paint] alla trasparenza [alpha]
    (linea, piatta), con un contorno nelle facce a linea se [outline] (spessore e
    trasparenza), dentro il ritaglio [clip] se c'e'."""

    def __init__(self, d: str, paint: Paint, alpha: tuple[float, float], what: str,
                 outline: tuple[float, float] | None = None, clip: str | None = None):
        self.d = d
        self.paint = paint
        self.alpha = alpha
        self.what = what
        self.outline = outline
        self.clip = clip


def line_d(a: tuple[float, float], b: tuple[float, float]) -> str:
    return f"M{fmt(a[0])},{fmt(a[1])} L{fmt(b[0])},{fmt(b[1])}"


def trails(head: tuple[float, float], degrees: float, rows) -> list[Stroke]:
    """Le scie della meteora: per ciascuna (scarto di lato, distacco dalla stella, lunghezza),
    lungo la direzione [degrees] (in senso orario dalle tre)."""
    a = math.radians(degrees)
    d = (math.cos(a), math.sin(a))
    n = (-d[1], d[0])
    out = []
    for side, gap, length in rows:
        p = (head[0] + d[0] * gap + n[0] * side, head[1] + d[1] * gap + n[1] * side)
        q = (p[0] + d[0] * length, p[1] + d[1] * length)
        out.append(Stroke(line_d(p, q), Paint("star"), "scia"))
    return out


def dashed_arc(centre: tuple[float, float], r: float, start: float, end: float, dashes: int, paint: Paint):
    """Un arco a trattini con le estremita' tonde, come la luna nuova disegna quel che non si
    vede: [dashes] trattini, ciascuno il 64% del suo passo."""
    out = []
    step = (end - start) / dashes
    for i in range(dashes):
        a0 = math.radians(start + step * (i + 0.18))
        a1 = math.radians(start + step * (i + 0.82))
        p = (centre[0] + r * math.cos(a0), centre[1] + r * math.sin(a0))
        q = (centre[0] + r * math.cos(a1), centre[1] + r * math.sin(a1))
        out.append(Stroke(f"M{fmt(p[0])},{fmt(p[1])} A{fmt(r)},{fmt(r)},0,0,1,{fmt(q[0])},{fmt(q[1])}",
                          paint, "sole sotto l'orizzonte"))
    return out


def circle_d(cx: float, cy: float, r: float) -> str:
    return f"M{fmt(cx - r)},{fmt(cy)} A{fmt(r)},{fmt(r)},0,1,1,{fmt(cx + r)},{fmt(cy)} A{fmt(r)},{fmt(r)},0,1,1,{fmt(cx - r)},{fmt(cy)} Z"


#: Le ricette. Le taglie e i centri sono stati scelti guardando i disegni a 51 dp, la
#: misura del Cielo, accanto ai loro vicini nella lista: l'inchiostro della composizione
#: occupa la scatola come quello di un disegno importato. Una stella di `star` e' larga 90
#: unita' alla sua taglia e ha il centro dell'inchiostro a (64, 60).
#:
#: - **meteor** (9 nov 2026): una stella di `star` al 45% in basso a sinistra, e dietro di lei
#:   tre scie a 45° verso l'alto a destra, la centrale piu' lunga: la stella che cade. Era
#:   `falling-stars` senza la luna (14 ott 2026), le stelle a meta' della scia.
#: - **moon-star**: la falce di `clear-night` all'80% in basso a sinistra e una stella di
#:   `star` al 42% in alto a destra, dove la luna lascia l'aria.
#: - **two-stars**: la stella al 62% e una al 40%, sulla stessa diagonale.
#: - **horizon-star** (9 nov 2026): la linea dell'orizzonte a 78, sotto di lei il sole
#:   tratteggiato (cinque trattini nel colore del sole di `horizon`, raggio 17), sopra due
#:   stelle al 41% e al 23%: il sole appena sotto l'orizzonte e le prime stelle, l'ora blu e
#:   le notti bianche. Era la cupola di `horizon` sopra la linea con la stella in cima (25 ott
#:   2026), che si leggeva come una pedina.
#: - **three-stars** (9 nov 2026): tre stelle al 57%, 34% e 23%, senza luna: il buio pieno e
#:   il centro della Via Lattea.
#: - **full-moonrise** (9 nov 2026): `moonrise` al suo posto, con il disco di `moon-full` al
#:   posto della falce, dentro lo stesso ritaglio sopra la linea: la luna piena al crepuscolo.
#: - **lunar-eclipse** (9 nov 2026): `moon-full` al suo posto, e l'ombra della Terra, un disco
#:   di raggio 55 da in basso a sinistra, ritagliata dentro la luna, nel rosso di `code-red` al
#:   45% (al 70% nelle facce piatte).
#: - **zodiacal-light** (9 nov 2026): la linea dell'orizzonte, un cono stretto che sale storto
#:   verso destra nel colore del sole di `horizon` (pieno al 25%, contorno al 70%; pieno al 60%
#:   nelle facce piatte), e due stelle al 25% e al 16%.
MOON_DISC = circle_d(64, 64, 30.5 * 1.38)  # il disco di `moon-full` nella scatola: il ritaglio dell'ombra
HORIZON_LINE = Stroke(line_d((26, 78), (102, 78)), Paint("horizon", 0), "orizzonte")
SUN = Paint("horizon", 1)

RECIPES = {
    "meteor": [Piece("star", 0.455, (44, 81.53))] + trails((44, 82), -45, [(0, 23, 44), (-11, 20, 26), (11, 20, 18)]),
    "moon-star": [Piece("clear_night", 0.80, (55, 71)), Piece("star", 0.42, (97, 34))],
    "two-stars": [Piece("star", 0.62, (52, 72)), Piece("star", 0.40, (96, 35))],
    "horizon-star": [HORIZON_LINE] + dashed_arc((64, 102), 17, 180, 360, 5, SUN)
    + [Piece("star", 0.409, (50, 47.57)), Piece("star", 0.227, (84, 29.76))],
    "three-stars": [Piece("star", 0.568, (48, 69.41)), Piece("star", 0.341, (86, 41.65)),
                    Piece("star", 0.227, (90, 87.76))],
    "full-moonrise": [Piece("moonrise", swap=("g1moon", "moon_full", (0, 3)))],
    "lunar-eclipse": [Piece("moon_full"),
                      Field(circle_d(33.64, 97.12, 55.2), Paint("code_red"), (0.45, 0.7), "ombra della Terra",
                            clip=MOON_DISC)],
    "zodiacal-light": [Field("M50,76 Q60,52 80,24 Q76,54 72,76 Z", SUN, (0.25, 0.6), "cono di luce",
                             outline=(3.0, 0.7)),
                       HORIZON_LINE, Piece("star", 0.25, (38, 41.74)), Piece("star", 0.159, (98, 47.83))],
}


def resource(name: str) -> str:
    return name.replace("-", "_")


def read_piece(prefix: str, piece: Piece):
    """Il contenuto del gruppo di scala della sorgente, la sua scala, e il riquadro del
    suo inchiostro nella scatola (contorni compresi)."""
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
    for el in kept:
        for p in el.iter("path"):
            # I gruppi interni delle sorgenti non trasformano: hanno solo un perno.
            for g in el.iter("group"):
                for attr in ("scaleX", "scaleY", "translateX", "translateY", "rotation"):
                    assert g.get(A + attr) in (None, "1", "0"), f"{path.name}: gruppo che trasforma"
            pad = float(p.get(A + "strokeWidth", "0")) / 2
            for poly in ink.flatten(p.get(A + "pathData")):
                box.add([(s * (x - 64) + 64, s * (y - 64) + 64) for x, y in poly], pad=pad * s)
    return kept, s, box


def _prune(el: ET.Element, drop: set) -> None:
    """Toglie da [el] ogni discendente il cui nome sta in [drop]."""
    for child in list(el):
        if child.get(A + "name") in drop:
            el.remove(child)
        else:
            _prune(child, drop)


def serialize(el: ET.Element, k: float, suffix: str, indent: str) -> str:
    """Un elemento della sorgente, con i nomi resi unici e la linea riportata a misura."""
    el = ET.fromstring(ET.tostring(el))
    for e in el.iter():
        name = e.get(A + "name")
        if name:
            e.set(A + "name", name + suffix)
        sw = e.get(A + "strokeWidth")
        if sw is not None and k != 1.0:
            e.set(A + "strokeWidth", fmt(float(sw) / k))
    ET.register_namespace("android", A_URI)
    text = ET.tostring(el, encoding="unicode")
    text = text.replace(' xmlns:android="' + A_URI + '"', "")
    text = re.sub(r"\s+", " ", text).replace(" />", "/>").replace("> <", ">" + NL + "<")
    return NL.join(indent + line for line in text.split(NL))


def swapped(prefix: str, piece: Piece, kept: list) -> list:
    """I figli del pezzo con il gruppo [piece.swap] preso dall'altra sorgente, spostato."""
    name, other, (dx, dy) = piece.swap
    root = ET.parse(DRAWABLE / f"{prefix}{other}.xml").getroot()
    scale = next(g for g in root.iter("group") if g.get(A + "name") == "mc3scale")
    mine = next(g for g in ET.parse(DRAWABLE / f"{prefix}{piece.source}.xml").getroot().iter("group")
                if g.get(A + "name") == "mc3scale")
    assert scale.attrib == mine.attrib, f"{other} e {piece.source}: scale di famiglia diverse"
    new = next(g for g in scale.iter("group") if g.get(A + "name") == name)
    moved = ET.Element("group", {A + "name": name + "move", A + "translateX": fmt(dx), A + "translateY": fmt(dy)})
    moved.append(new)
    # Prima si trova, poi si sostituisce: il gruppo nuovo porta lo stesso nome, e una
    # sostituzione fatta camminando l'albero lo ritroverebbe all'infinito.
    found = [(parent, i) for el in kept for parent in el.iter()
             for i, child in enumerate(parent) if child.get(A + "name") == name]
    assert len(found) == 1, f"{piece.source}: {name} trovato {len(found)} volte"
    parent, i = found[0]
    parent.remove(parent[i])
    parent.insert(i, moved)
    return kept


def drawn(item, j: int, prefix: str) -> list[str]:
    flat = prefix in ("mc3f_", "mc3fn_")
    colour = item.paint.of(prefix)
    out = [f'    <group android:name="drawn{j}">']
    if isinstance(item, Field) and item.clip:
        out.append(f'        <clip-path android:pathData="{item.clip}"/>')
    if isinstance(item, Stroke):
        out.append(f'        <path android:name="drawn{j}p" android:pathData="{item.d}" android:strokeColor="{colour}"'
                   f' android:strokeWidth="{fmt(LINE)}" android:strokeLineCap="round"/>')
    else:
        alpha = item.alpha[1] if flat else item.alpha[0]
        attrs = f' android:fillColor="{colour}" android:fillAlpha="{fmt(alpha)}"'
        if item.outline and not flat:
            width, outline_alpha = item.outline
            attrs += (f' android:strokeColor="{colour}" android:strokeWidth="{fmt(width)}"'
                      f' android:strokeAlpha="{fmt(outline_alpha)}" android:strokeLineJoin="round"')
        out.append(f'        <path android:name="drawn{j}p" android:pathData="{item.d}"{attrs}/>')
    out.append("    </group>")
    return out


def compose(name: str, prefix: str) -> str:
    out = [
        "<!-- Composto da tools/compose_sky_marks.py da disegni di Meteocons v3",
        "     (github.com/basmilius/meteocons), MIT, (c) Bas Milius —",
        "     licenses/Meteocons-MIT.txt. Non modificare a mano: ri-eseguire lo strumento. -->",
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"',
        '    android:width="24dp"',
        '    android:height="24dp"',
        '    android:viewportWidth="128"',
        '    android:viewportHeight="128">',
    ]
    i = j = 0
    for item in RECIPES[name]:
        if not isinstance(item, Piece):
            j += 1
            out += drawn(item, j, prefix)
            continue
        i += 1
        piece = item
        kept, s, box = read_piece(prefix, piece)
        if piece.swap:
            kept = swapped(prefix, piece, kept)
        if piece.center is None:
            cx, cy, tx, ty = 64.0, 64.0, 0.0, 0.0
        else:
            cx = (box.x0 + box.x1) / 2
            cy = (box.y0 + box.y1) / 2
            tx = piece.center[0] - cx
            ty = piece.center[1] - cy
        out.append(
            f'    <group android:name="piece{i}" android:pivotX="{fmt(cx)}" android:pivotY="{fmt(cy)}"'
            f' android:scaleX="{fmt(piece.k)}" android:scaleY="{fmt(piece.k)}"'
            f' android:translateX="{fmt(tx)}" android:translateY="{fmt(ty)}">'
        )
        out.append(
            f'        <group android:name="piece{i}scale" android:scaleX="{fmt(s)}" android:scaleY="{fmt(s)}"'
            f' android:pivotX="64" android:pivotY="64">'
        )
        for el in kept:
            out.append(serialize(el, piece.k, f"_{i}", "            "))
        out.append("        </group>")
        out.append("    </group>")
    out.append("</vector>")
    return NL.join(out) + NL


def kotlin() -> str:
    names = list(RECIPES)

    def table(doc: str, val: str, prefix: str) -> str:
        rows = NL.join(
            f"        R.drawable.mc3_{resource(n)} to R.drawable.{prefix}{resource(n)}," for n in names
        )
        return f"    /** {doc} */{NL}    val {val}: Map<Int, Int> = mapOf({NL}{rows}{NL}    ){NL}"

    by_name = NL.join(f'        "{n}" to R.drawable.mc3_{resource(n)},' for n in names)
    return (
        "package com.callbackdev.chiaro.ui.icons" + NL + NL
        + "import com.callbackdev.chiaro.R" + NL + NL
        + "/**" + NL
        + " * I segni del cielo che **questo repo compone** dai disegni di Meteocons (14 ott 2026," + NL
        + " * rivisti il 9 nov 2026): la meteora (la stella con le scie), la luna con la stella e le" + NL
        + " * due stelle delle congiunzioni, il sole sotto l'orizzonte con le prime stelle (l'ora" + NL
        + " * blu, le notti bianche), le tre stelle (il buio pieno, la Via Lattea), la luna piena che" + NL
        + " * sorge, l'eclissi di luna e la luce zodiacale. Il perche', con le misure, sta in testa" + NL
        + " * allo strumento." + NL
        + " *" + NL
        + " * **File generato da `tools/compose_sky_marks.py` — non si modifica a mano.** Ha la" + NL
        + " * forma di [ComposedIcons] (la chiave e' l'id del set line su fondo chiaro), senza" + NL
        + " * gemelli animati: nel catalogo del cielo non si muove niente." + NL
        + " */" + NL
        + "internal object SkyMarks {" + NL + NL
        + table("line, fondo scuro.", "lineDarkOf", "mc3n_") + NL
        + table("flat, fondo chiaro.", "flatOf", "mc3f_") + NL
        + table("flat, fondo scuro.", "flatDarkOf", "mc3fn_") + NL
        + "    /** Per nome, come [MeteoconsSets.byName]. */" + NL
        + f"    val byName: Map<String, Int> = mapOf({NL}{by_name}{NL}    ){NL}"
        + "}" + NL
    )


def main():
    parser = argparse.ArgumentParser(description=__doc__.split(NL)[0])
    parser.add_argument("--controlla", action="store_true", help="verifica senza scrivere")
    args = parser.parse_args()
    outputs = {}
    for name in RECIPES:
        for prefix in FACES:
            outputs[DRAWABLE / f"{prefix}{resource(name)}.xml"] = compose(name, prefix)
    outputs[KOTLIN] = kotlin()
    # UTF-8 esplicito e fine riga indifferenti (25 ott 2026): su Windows la codifica di sistema
    # scriveva il trattino lungo del commento come un byte che rende l'XML non valido, e il
    # CRLF della copia di lavoro diceva «da rigenerare» di ogni file uguale.
    stale = [p for p, text in outputs.items()
             if not p.exists() or p.read_bytes().replace(b"\r\n", b"\n") != text.encode("utf-8")]
    if args.controlla:
        for p in stale:
            print(f"da rigenerare: {p.relative_to(HERE.parent)}")
        sys.exit(1 if stale else 0)
    for p, text in outputs.items():
        p.write_text(text, encoding="utf-8")
    for name in RECIPES:
        print(f"{name}: " + ", ".join(
            f"{pc.source} x{pc.k} in {pc.center or 'posto'}" if isinstance(pc, Piece) else pc.what
            for pc in RECIPES[name]))
    print(f"scritti {len(outputs)} file")


if __name__ == "__main__":
    main()
