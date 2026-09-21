#!/usr/bin/env python3
"""Il secondo carattere dell'app: Google Sans, ridotto a quel che l'app stampa.

    python3 tools/import_google_sans.py              # scarica, riduce, scrive
    python3 tools/import_google_sans.py --sorgente GoogleSans.ttf
    python3 tools/import_google_sans.py --controlla  # non scrive: rifa' e confronta

Serve `fonttools` (`pip install fonttools`). Come per i disegni del meteo, **il file
sotto `res/font/` non si tocca a mano: rifare girare questo strumento E' l'importazione.**

**Da dove viene.** `google/fonts`, cartella `ofl/googlesans`: dal 2025 Google Sans sta
nel ramo OFL del repository dei font di Google, quindi SIL Open Font License 1.1, e la
riga di copyright non dichiara nessun Reserved Font Name — si puo' impacchettare,
ridurre e ridistribuire tenendo la licenza accanto. Il testo va in
`licenses/GoogleSans-OFL.txt` e la riga in `licenses/README.md`, come per Inter.

    file     GoogleSans[GRAD,opsz,wght].ttf   4 974 940 byte
    sha256   d0a87d835a944b8b40d0e82a5651bb59ab97b936a2aeed5946eb57e7b2a3a90a
    versione Version 14.000

**Perche' si riduce.** Cinque mega dentro un APK per un carattere alternativo non si
possono giustificare: il font intero porta 8 311 glifi e una ventina di scritture che
questa app non stampa (bengalese, devanagari, etiope, cherokee...). Ridotto a quel che
serve sta in ~300 KB, cioe' **un terzo di Inter**, che ne pesa 880 perche' e' il file
come lo pubblica il suo autore.

**Cosa viene fissato, e perche' proprio questo.**

- `GRAD=0`. L'asse di grado compensa otticamente l'inchiostro chiaro su fondo scuro.
  Qui il fondo scuro c'e' (il tema scuro, la tela del cielo), ma il grado si imposta per
  contesto e l'app non ha un posto dove deciderlo: lasciarlo variabile sarebbe stato un
  asse che nessuno muove mai.
- `opsz=18`. L'asse ottico di questo file va da 17 a 18: un punto. Non c'e' niente da
  guadagnare a tenerlo variabile, e un asse in meno e' una tabella `gvar` piu' piccola.
- `wght` **resta variabile da 400 a 700**, perche' e' l'asse che il tema muove. Ed e'
  anche il vincolo da conoscere prima di disegnare: **in Google Sans il 300 non esiste.**
  Inter arriva a 100, questo parte da 400, quindi la famiglia dichiara quattro pesi
  (400, 500, 600, 700) e chi chiede Light si prende il 400. Non si sintetizza niente: un
  peso finto sotto il minimo dell'asse e' esattamente la sbavatura per cui Inter era
  stato impacchettato variabile.

**Le scritture tenute** sono quelle che Inter porta gia', perche' le due risposte del
lettore devono coprire le stesse parole: latino con le sue estensioni, greco e cirillico
— i nomi dei luoghi arrivano dal geocoder e non sono tutti italiani — piu' la
punteggiatura, i segni di valuta, gli apici e i pochi simboli matematici che una schermata
di meteo puo' stampare. Quel che resta fuori cade sul carattere di sistema, glifo per
glifo, che e' come Android gestisce da sempre un buco in un font.

**Le funzioni OpenType tenute** contengono `tnum`, ed e' la ragione per cui questo font
puo' entrare nell'app: DESIGN §5 dice che ogni cifra in colonna e' tabulare, e una
famiglia senza cifre tabulari farebbe ballare il Diario e la settimana senza dare un
errore. Google Sans ce l'ha, ed e' stato verificato sul file, non sulla scheda.
"""

from __future__ import annotations

import argparse
import hashlib
import io
import pathlib
import sys
import tempfile
import urllib.request

REPO = pathlib.Path(__file__).resolve().parent.parent

SORGENTE = (
    "https://raw.githubusercontent.com/google/fonts/main/ofl/googlesans/"
    "GoogleSans%5BGRAD,opsz,wght%5D.ttf"
)
LICENZA = "https://raw.githubusercontent.com/google/fonts/main/ofl/googlesans/OFL.txt"

# Quel che questo strumento ha importato. Se il file a monte cambia, la differenza si
# vede qui e la si registra apposta: un carattere che cambia disegno sotto i piedi e'
# una misura che scade (le colonne in dp di `TextScale.kt` sono misurate su quel che
# l'APK porta, non su quel che il repo di Google ha oggi).
SHA_SORGENTE = "d0a87d835a944b8b40d0e82a5651bb59ab97b936a2aeed5946eb57e7b2a3a90a"
SHA_LICENZA = "2b75ef20f13d83a7514aee452c4782c20cdc9ff2dee17600f44d37a06d4fb958"

USCITA = REPO / "app/src/main/res/font/google_sans_variable.ttf"
USCITA_LICENZA = REPO / "licenses/GoogleSans-OFL.txt"

# Gli assi fissati e quello che resta: vedi il docstring.
ISTANZA = {"GRAD": 0, "opsz": 18}
PESI = (400, 700)

INTERVALLI = ",".join(
    (
        "U+0000-024F",  # latino, latino-1, estensioni A e B: le due lingue e i nomi
        "U+0259",       # schwa: sta fuori dagli intervalli sopra e sta in nomi veri
        "U+02B0-02FF",  # modificatori (apostrofi e accenti sciolti dei toponimi)
        "U+0300-036F",  # diacritici combinanti: un nome decomposto va composto lo stesso
        "U+0370-03FF",  # greco
        "U+0400-04FF",  # cirillico
        "U+2000-206F",  # punteggiatura: virgolette basse, lineette, puntini
        "U+2070-209F",  # apici e pedici
        "U+20A0-20BF",  # valute
        "U+2100-214F",  # segni tipo numero e gradi (il ° sta gia' in latino-1)
        "U+2190-2193",  # frecce: non le stampiamo, ma un messaggio del lettore puo'
        "U+2212",       # meno tipografico
        "U+2215,U+2248,U+2260,U+2264,U+2265",  # barra, circa, diverso, minore/maggiore uguale
        "U+2022,U+25CF",  # punto elenco e pallino pieno
    )
)

FUNZIONI = ",".join(
    (
        "kern", "liga", "clig", "calt", "ccmp", "mark", "mkmk", "locl",
        "tnum",  # la ragione per cui questo font e' ammissibile: DESIGN §5
        "lnum", "case", "ordn", "frac", "sups",
    )
)


def sha(dati: bytes) -> str:
    return hashlib.sha256(dati).hexdigest()


def scarica(url: str, atteso: str, nome: str) -> bytes:
    print(f"scarico {nome}...")
    with urllib.request.urlopen(url) as risposta:
        dati = risposta.read()
    trovato = sha(dati)
    if trovato != atteso:
        print(
            f"  ATTENZIONE: {nome} a monte e' cambiato.\n"
            f"    atteso  {atteso}\n    trovato {trovato}\n"
            f"  Aggiorna la costante in questo file e registra il cambio in PLANNING.md.",
            file=sys.stderr,
        )
    print(f"  {len(dati):,} byte, sha256 {trovato[:16]}...")
    return dati


def riduci(originale: bytes) -> bytes:
    """Fissa gli assi che non servono, poi tiene solo i glifi e le funzioni che l'app usa."""
    from fontTools import subset
    from fontTools.ttLib import TTFont
    from fontTools.varLib import instancer

    # `recalcTimestamp=False`: fonttools, lasciato fare, scrive l'ora di adesso in
    # `head.modified`, e due esecuzioni dello stesso strumento sullo stesso file
    # darebbero due byte diversi. Cosi' invece l'uscita dipende solo dall'entrata
    # (e dalla versione di fonttools), che e' cio' che rende `--controlla` una
    # verifica e non un rituale.
    font = TTFont(io.BytesIO(originale), recalcTimestamp=False)
    limiti = dict(ISTANZA)
    limiti["wght"] = PESI
    instancer.instantiateVariableFont(font, limiti, inplace=True, updateFontNames=False)

    # Il font istanziato si riapre da zero prima di ridurlo. Non e' pignoleria: dopo
    # `instantiateVariableFont` la `gvar` in memoria conosce ancora glifi che il resto
    # del font non ha piu' (`e.logo` e compagnia), e il riduttore va a sbattere su uno
    # di quelli. Un giro per i byte e le tabelle tornano a raccontare la stessa cosa.
    mezzo = io.BytesIO()
    font.save(mezzo)
    mezzo.seek(0)
    font = TTFont(mezzo, recalcTimestamp=False)

    opzioni = subset.Options()
    opzioni.set(layout_features=FUNZIONI.split(","))
    opzioni.name_IDs = ["*"]
    opzioni.name_legacy = True
    opzioni.notdef_outline = True
    opzioni.hinting = False
    opzioni.drop_tables += ["DSIG"]
    riduttore = subset.Subsetter(options=opzioni)
    riduttore.populate(unicodes=subset.parse_unicodes(INTERVALLI))
    riduttore.subset(font)

    fuori = io.BytesIO()
    font.save(fuori)
    return fuori.getvalue()


def misura(dati: bytes) -> None:
    from fontTools.ttLib import TTFont

    font = TTFont(io.BytesIO(dati))
    assi = [(a.axisTag, a.minValue, a.maxValue) for a in font["fvar"].axes]
    funzioni = {
        f.FeatureTag
        for t in ("GSUB", "GPOS")
        if t in font
        for f in font[t].table.FeatureList.FeatureRecord
    }
    print(f"  {len(dati):,} byte")
    print(f"  assi: {assi}")
    print(f"  glifi: {font['maxp'].numGlyphs}, caratteri: {len(font.getBestCmap())}")
    print(f"  tnum: {'tnum' in funzioni}")
    if "tnum" not in funzioni:
        raise SystemExit("il font ridotto ha perso tnum: DESIGN §5 non lo ammette")


def main() -> None:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--sorgente", help="un GoogleSans[GRAD,opsz,wght].ttf gia' scaricato")
    ap.add_argument(
        "--controlla",
        action="store_true",
        help="non scrive: rifa' la riduzione e la confronta con il file in repo",
    )
    args = ap.parse_args()

    if args.sorgente:
        originale = pathlib.Path(args.sorgente).read_bytes()
        print(f"sorgente locale: {args.sorgente}, sha256 {sha(originale)[:16]}...")
    else:
        originale = scarica(SORGENTE, SHA_SORGENTE, "GoogleSans[GRAD,opsz,wght].ttf")

    ridotto = riduci(originale)
    misura(ridotto)

    if args.controlla:
        if not USCITA.exists():
            raise SystemExit(f"manca {USCITA.relative_to(REPO)}")
        in_repo = USCITA.read_bytes()
        if in_repo == ridotto:
            print("il file in repo e' esattamente questo.")
            return
        raise SystemExit(
            "il file in repo NON coincide.\n"
            f"  repo     {len(in_repo):,} byte, sha256 {sha(in_repo)[:16]}...\n"
            f"  rifatto  {len(ridotto):,} byte, sha256 {sha(ridotto)[:16]}...\n"
            "  Puo' essere una versione diversa di fonttools: confronta le misure sopra "
            "prima di riscrivere."
        )

    USCITA.parent.mkdir(parents=True, exist_ok=True)
    USCITA.write_bytes(ridotto)
    print(f"scritto {USCITA.relative_to(REPO)}")

    if not args.sorgente:
        testo = scarica(LICENZA, SHA_LICENZA, "OFL.txt")
        USCITA_LICENZA.write_bytes(testo)
        print(f"scritto {USCITA_LICENZA.relative_to(REPO)}")


if __name__ == "__main__":
    main()
