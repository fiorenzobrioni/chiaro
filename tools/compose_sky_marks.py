#!/usr/bin/env python3
"""I segni del cielo che Meteocons non ha: la meteora, la luna con la stella, le due stelle.

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
  disco inventato sarebbe un pianeta che a occhio nudo nessuno vede. Le ore blu e i
  crepuscoli nautici restano la stella anche loro: sono lo stesso cielo, quello delle
  prime stelle nel blu, e Meteocons non ha un disegno per il sole appena sotto
  l'orizzonte.

**Come.** Come `compose_sun_cloud.py`, non si disegna niente: si compone. Ogni pezzo e' il
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
    rispetto a come lo disegna il suo file, senza i gruppi o i percorsi in [drop]."""

    def __init__(self, source: str, k: float, center: tuple[float, float], drop=()):
        self.source = source
        self.k = k
        self.center = center
        self.drop = set(drop)


#: Le ricette. Le taglie e i centri sono stati scelti guardando i disegni a 51 dp, la
#: misura del Cielo, accanto ai loro vicini nella lista: l'inchiostro della composizione
#: occupa la scatola come quello di un disegno importato.
#:
#: - **meteor**: `falling-stars` senza la luna (`g4moon`), al 110% e rimesso al centro: tolta
#:   la luna, le stelle e la scia stavano tutte nella meta' alta, e alla misura del Cielo la
#:   scia sola era un ramoscello. Di piu' non si puo': la scia arriva a 120 unita' su 128.
#: - **moon-star**: la falce di `clear-night` all'80% in basso a sinistra e una stella di
#:   `star` al 42% in alto a destra, dove la luna lascia l'aria.
#: - **two-stars**: la stella al 62% e una al 40%, sulla stessa diagonale.
RECIPES = {
    "meteor": [Piece("falling_stars", 1.1, (64, 64), drop={"g4moon"})],
    "moon-star": [Piece("clear_night", 0.80, (55, 71)), Piece("star", 0.42, (97, 34))],
    "two-stars": [Piece("star", 0.62, (52, 72)), Piece("star", 0.40, (96, 35))],
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
    kept = [el for el in list(scale) if el.get(A + "name") not in piece.drop]
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
    for i, piece in enumerate(RECIPES[name], start=1):
        kept, s, box = read_piece(prefix, piece)
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
        + " * I segni del cielo che **questo repo compone** dai disegni di Meteocons (14 ott 2026):" + NL
        + " * la meteora (le stelle cadenti senza la luna), la luna con la stella e le due stelle" + NL
        + " * delle congiunzioni. Il perche', con le misure, sta in testa allo strumento." + NL
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
    stale = [p for p, text in outputs.items() if not p.exists() or p.read_text() != text]
    if args.controlla:
        for p in stale:
            print(f"da rigenerare: {p.relative_to(HERE.parent)}")
        sys.exit(1 if stale else 0)
    for p, text in outputs.items():
        p.write_text(text)
    for name in RECIPES:
        print(f"{name}: " + ", ".join(f"{pc.source} x{pc.k} in {pc.center}" for pc in RECIPES[name]))
    print(f"scritti {len(outputs)} file")


if __name__ == "__main__":
    main()
