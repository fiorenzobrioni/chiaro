#!/usr/bin/env python3
"""Il riancoraggio della tavolozza di Meteocons alle superfici di Chiaro.

Meteocons e' disegnato per un fondo neutro. Sulla carta dell'app (`#FCF9F3`) i corpi
delle nuvole stanno a 1,02-1,10:1, cioe' sono invisibili; sul fondo scuro (`#16130E`)
sono nitidi ma i dettagli quasi neri spariscono. DESIGN §10 chiede 3:1 ai segni non
testuali, e le icone sono l'unico portatore di «che tempo fa» nella striscia oraria.

L'importatore v2 risolveva la stessa cosa con una **tabella a mano** (`REMAP`,
`FILL_REMAP`, `FILL_NIGHT` in `import_meteocons.py`): una riga per ogni hex della v2.
La v3 ha piu' del doppio dei colori e cambiera' ancora, quindi qui la regola e' scritta
come funzione e la tabella e' il suo risultato.

**Due differenze dalla v2, e la seconda e' un guadagno.**

1. La regola e' la stessa: si tiene la tinta, si sposta la **luminanza**. Il contrasto
   WCAG dipende solo da quella, quindi muoverla e' l'unica leva che cambia il rapporto,
   ed e' anche quella che si vede di meno.

2. v2 chiedeva a UN set di reggere **entrambe** le superfici, il che lo inchiodava nella
   banda Y in [0,120, 0,283] — ed e' il motivo per cui il suo sole e' un bronzo: a
   quella luminanza in sRGB il giallo non esiste. Qui ogni set incontra **una sola**
   superficie, quindi il vincolo e' un tetto (chiaro) o un pavimento (scuro), mai
   tutti e due. Molta piu' tavolozza dell'illustratore sopravvive.

**L'ordine dentro una famiglia si conserva**, che e' la cosa che una regola per-colore
indipendente romperebbe: una nuvola e' tre grigi, e se il piu' scuro e il piu' chiaro si
scambiano il disegno diventa illeggibile. Quindi i colori si raggruppano per tinta e la
famiglia si sposta **intera**, mantenendo la sua spaziatura in L di Oklab (che e'
percettivamente uniforme) e schiacciandola solo quando non ci sta.
"""
from __future__ import annotations

import collections
import importlib.util
import pathlib

HERE = pathlib.Path(__file__).resolve().parent
_spec = importlib.util.spec_from_file_location("cm", HERE / "color_math.py")
cm = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(cm)

#: Le due superfici di DESIGN §2.2 e la soglia di §10.
LIGHT = "#FCF9F3"
DARK = "#16130E"
FLOOR = 3.0

#: Sotto questa croma un colore e' un neutro: la sua tinta non dice niente e metterlo in
#: famiglia con gli altri neutri e' cio' che tiene insieme i grigi di una nuvola.
NEUTRAL_CHROMA = 0.03
#: Larghezza di un settore di tinta, in gradi.
HUE_BUCKET = 40.0


def cap_for(background: str) -> tuple[float | None, float | None]:
    """(pavimento, tetto) di luminanza perche' un segno regga 3:1 su questo fondo.

    Su un fondo chiaro serve essere abbastanza scuri: c'e' solo un tetto. Su uno scuro
    serve essere abbastanza chiari: c'e' solo un pavimento. E' la meta' del vincolo che
    la v2 doveva rispettare.
    """
    yb = cm.luminance(cm.hex_to_rgb(background))
    if yb > 0.5:
        return None, (yb + 0.05) / FLOOR - 0.05
    return FLOOR * (yb + 0.05) - 0.05, None


def _family(rgb):
    L, C, h = cm.rgb_to_oklch(rgb)
    if C < NEUTRAL_CHROMA:
        return "neutro"
    return round(h / HUE_BUCKET)


def reanchor(colors, background: str) -> dict[str, str]:
    """{hex sorgente: hex spedito} per una superficie.

    La famiglia non viene **spostata**, viene **compressa**, e l'estremo che gia' sta
    bene resta fermo: contro un tetto si tiene il membro piu' scuro dov'e' e si tira giu'
    il piu' chiaro fin sotto la soglia, contro un pavimento si tiene il piu' chiaro e si
    alza il piu' scuro. Cosi' il nero di un disegno resta nero, il bianco diventa il
    grigio che si vede, e i passaggi in mezzo conservano ordine e spaziatura.
    """
    floor, ceiling = cap_for(background)
    groups = collections.defaultdict(list)
    for c in {c.lower() for c in colors}:
        try:
            rgb = cm.hex_to_rgb(c)
        except Exception:
            continue
        groups[_family(rgb)].append((c, rgb))

    out: dict[str, str] = {}
    for _, members in groups.items():
        lch = [(c, rgb) + cm.rgb_to_oklch(rgb) for c, rgb in members]
        hue = sum(h for *_, h in lch) / len(lch)
        Ls = [L for _, _, L, _, _ in lch]
        lo, hi = min(Ls), max(Ls)
        target_lo, target_hi = lo, hi
        if ceiling is not None and any(cm.luminance(rgb) > ceiling for _, rgb, *_ in lch):
            target_hi = _L_at(ceiling, hue)
            target_lo = min(lo, target_hi)
        if floor is not None and any(cm.luminance(rgb) < floor for _, rgb, *_ in lch):
            target_lo = _L_at(floor, hue)
            target_hi = max(hi, target_lo)
        span = hi - lo
        for c, rgb, L, C, h in lch:
            t = 0.0 if span <= 0 else (L - lo) / span
            newL = target_lo + (target_hi - target_lo) * t
            out[c] = _fit(newL, C, h, floor, ceiling, source=c, source_L=L)
    return out


def _fit(L: float, C: float, hue: float, floor, ceiling,
         source: str | None = None, source_L: float | None = None) -> str:
    """Il colore a questa L, con la croma che il gamut concede — e poi **verificato**.

    La luminanza si risolve sul neutro, ma la croma la sposta di un pelo, ed e' quanto
    basta a mancare il 3:1 per un centesimo. Invece di fidarsi del metodo si misura
    l'esito e si corregge di un passo alla volta finche' passa: e' la stessa regola con
    cui `PaletteContrastTest` asserisce il risultato e non la ricetta.
    """
    # Un colore che gia' reggeva e che la compressione lascia dov'era esce **identico**:
    # il giro per Oklch e ritorno sposterebbe l'ultima cifra, e cambiare un hex che non
    # aveva bisogno di cambiare e' rumore in un diff che qualcuno dovra' leggere.
    if source is not None and source_L is not None and abs(L - source_L) < 0.004:
        y = cm.luminance(cm.hex_to_rgb(source))
        if ((ceiling is None or y <= ceiling) and (floor is None or y >= floor)):
            return source
    for _ in range(64):
        L = min(1.0, max(0.0, L))
        rgb = cm.oklch_to_rgb(L, min(C, cm.max_chroma(L, hue)), hue)
        rgb = tuple(min(1.0, max(0.0, v)) for v in rgb)
        y = cm.luminance(rgb)
        if ceiling is not None and y > ceiling:
            L -= 0.005
            continue
        if floor is not None and y < floor:
            L += 0.005
            continue
        return cm.rgb_to_hex(rgb).lower()
    return cm.rgb_to_hex(rgb).lower()


def _L_at(target_y: float, hue: float) -> float:
    """La L di Oklab che, a questa tinta, da' questa luminanza.

    La luminanza si risolve sul neutro di quella L: la croma la sposta di poco e
    l'ultimo centesimo lo corregge `_fit`, misurando invece di fidarsi.
    """
    lo, hi = 0.0, 1.0
    for _ in range(48):
        mid = (lo + hi) / 2
        if cm.luminance(cm.oklch_to_rgb(mid, 0.0, hue)) < target_y:
            lo = mid
        else:
            hi = mid
    return (lo + hi) / 2


def report(mapping: dict[str, str], background: str):
    """Quante ne passano, dopo. Si misura l'esito, non il metodo."""
    bg = cm.hex_to_rgb(background)
    bad = [(a, b, cm.contrast(cm.hex_to_rgb(b), bg)) for a, b in mapping.items()
           if cm.contrast(cm.hex_to_rgb(b), bg) < FLOOR]
    kept = sum(1 for a, b in mapping.items() if a == b)
    return kept, bad
