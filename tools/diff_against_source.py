"""Quante icone convertite NON combaciano con la loro sorgente, e quali.

Elencare gli attributi che l'importatore legge dice quel che si e' pensato di leggere, non
quel che si e' disegnato. Questo invece guarda il **risultato**: Chrome disegna l'SVG
originale, `icon_filmstrip` ricostruisce un SVG dal drawable Android, i due si sovrappongono
con `mix-blend-mode: difference` in una cella di griglia a posizione nota. Dove combaciano
la cella e' nera. Poi si decodifica il PNG e si conta, perche' 519 celle non si guardano a
occhio.

Due accortezze, e senza di esse il conto non vuol dire niente:

- **`shape-rendering:crispEdges`**, perche' un pixel a meta' trasparenza non da' zero sotto
  `difference` (il blend e' `(1-a)Cb + a|Cb-Cs|`, non `|Cb-Cs|`): con l'antialiasing acceso
  tutte e 519 le celle brillano di un contorno che non significa niente.
- **la linea di base** (`--self`): si misura la stessa griglia con la sorgente contro se
  stessa e la si sottrae. Quel che resta e' rumore residuo, e va tolto prima di parlare.

Si confronta contro il set `mc3o_*` (i colori dell'illustratore, generati da
`import_meteocons_v3.py --original`): contro i set riancorati la differenza si accenderebbe
ovunque, per una ragione che e' voluta. Dall'11 set 2026 quel set e' anche l'unico **senza
la scala**: i set spediti portano il gruppo `mc3scale` che li riporta tutti a 0,75 della
scatola (DESIGN §13.1), e confrontare quelli con la sorgente misurerebbe la
normalizzazione invece della fedelta'. Il confronto delle taglie ha il suo strumento,
`tools/icon_ink.py --confronto`, che sa della scala e la mette nel conto.

    python tools/import_meteocons_v3.py <pacchetto> line --original
    python tools/diff_against_source.py <cartella di lavoro> <pacchetto> --self
    python tools/diff_against_source.py <cartella di lavoro> <pacchetto>

Le differenze attese, tutte dichiarate in DESIGN §13.1: le righe del vento, dove il
tratteggio spazzolato esce pieno nel disegno fermo perche' VectorDrawable non ha
`stroke-dasharray`. La famiglia dei pollini non e' piu' fra queste da quando il suo
ritaglio e' stato tolto. Tutto il resto e' da guardare.
"""
import pathlib
import re
import struct
import subprocess
import sys
import xml.etree.ElementTree as ET
import zlib
import importlib.util

SP = pathlib.Path(sys.argv[1])          # la scratchpad
#: Con "self" il secondo strato e' di nuovo la sorgente: e' la **linea di base** del
#: rumore, cioe' quanto inchiostro resta acceso anche quando i due disegni sono lo stesso
#: identico file. Senza sottrarla, i bordi fanno sembrare diverse 443 icone su 519.
SELF = "--self" in sys.argv
SRC = pathlib.Path(sys.argv[2]) / "line"
CHROME = r"C:\Program Files\Google\Chrome\Application\chrome.exe"
D = pathlib.Path("app/src/main/res/drawable")
CELL, GAP, COLS = 34, 2, 20
PITCH = CELL + GAP

spec = importlib.util.spec_from_file_location("fs", "tools/icon_filmstrip.py")
fs = importlib.util.module_from_spec(spec)
spec.loader.exec_module(fs)


def uniq(s, u):
    for m in set(re.findall(r'id="([^"]+)"', s)):
        s = s.replace(f'"{m}"', f'"{u}_{m}"').replace(f"url(#{m})", f"url(#{u}_{m})")
    return s


def source_svg(path, uid):
    s = path.read_text(encoding="utf8")
    s = re.sub(r"<animate(Transform|Motion)?\b[^>]*/>", "", s)
    s = re.sub(r"<animate(Transform|Motion)?\b.*?</animate\1?>", "", s, flags=re.S)
    return uniq(s, uid).replace("<svg ", "<svg class=a ", 1)


def rebuilt_svg(path, uid):
    root = ET.parse(path).getroot()
    body, clips = [], []
    for ch in root:
        fs.svg_shape(ch, {}, body, clips)
    vw = fs.attr(root, "viewportWidth", "64")
    vh = fs.attr(root, "viewportHeight", "64")
    return f'<svg class=b viewBox="0 0 {vw} {vh}">{uniq("".join(body), uid)}</svg>'


def png_pixels(path):
    data = path.read_bytes()
    assert data[:8] == b"\x89PNG\r\n\x1a\n"
    i, idat, w = 8, b"", None
    while i < len(data):
        ln, typ = struct.unpack(">I4s", data[i:i + 8])
        chunk = data[i + 8:i + 8 + ln]
        if typ == b"IHDR":
            w, h, depth, color = struct.unpack(">IIBB", chunk[:10])
        elif typ == b"IDAT":
            idat += chunk
        i += 12 + ln
    raw = zlib.decompress(idat)
    ch = {0: 1, 2: 3, 4: 2, 6: 4}[color]
    stride = w * ch
    out, prev, p = [], bytearray(stride), 0
    for _ in range(h):
        f = raw[p]; p += 1
        line = bytearray(raw[p:p + stride]); p += stride
        for x in range(stride):
            a = line[x - ch] if x >= ch else 0
            b = prev[x]
            c = prev[x - ch] if x >= ch else 0
            if f == 1: line[x] = (line[x] + a) & 255
            elif f == 2: line[x] = (line[x] + b) & 255
            elif f == 3: line[x] = (line[x] + (a + b) // 2) & 255
            elif f == 4:
                pa, pb, pc = abs(b - c), abs(a - c), abs(a + b - 2 * c)
                line[x] = (line[x] + (a if pa <= pb and pa <= pc else b if pb <= pc else c)) & 255
        out.append(bytes(line)); prev = line
    return w, h, ch, out


names = sorted(p.stem for p in SRC.glob("*.svg"))
rows = (len(names) + COLS - 1) // COLS
tag = "self" if SELF else "conv"
html = SP / f"diffgrid_{tag}.html"
png = SP / f"diffgrid_{tag}.png"
with html.open("w", encoding="utf8") as fh:
    fh.write("<html><head><meta charset=utf-8><style>body{margin:0;background:#000}"
             f"#g{{display:grid;grid-template-columns:repeat({COLS},{PITCH}px);"
             f"grid-auto-rows:{PITCH}px}}"
             f".c{{position:relative;width:{CELL}px;height:{CELL}px;background:#000;"
             "isolation:isolate}"
             f".c svg{{position:absolute;inset:0;width:{CELL}px;height:{CELL}px;"
             "shape-rendering:crispEdges}"
             ".c svg *{shape-rendering:crispEdges}"
             ".c svg.b{mix-blend-mode:difference}"
             "</style></head><body><div id=g>")
    for n in names:
        f = D / f"mc3o_{n.replace('-', '_')}.xml"
        uid = "u" + re.sub(r"[^a-z0-9]", "", n)
        if SELF:
            second = source_svg(SRC / (n + ".svg"), uid + "z").replace("class=a", "class=b", 1)
            cell = source_svg(SRC / (n + ".svg"), uid) + second
        else:
            cell = (source_svg(SRC / (n + ".svg"), uid) + rebuilt_svg(f, uid)) if f.exists() else ""
        fh.write(f'<span class=c>{cell}</span>')
    fh.write("</div></body></html>")

subprocess.run([CHROME, "--headless", "--disable-gpu", f"--screenshot={png}",
                f"--window-size={COLS * PITCH},{rows * PITCH}", "--hide-scrollbars",
                html.as_uri()], capture_output=True)

w, h, ch, px = png_pixels(png)
report = []
for idx, n in enumerate(names):
    r, c = divmod(idx, COLS)
    x0, y0 = c * PITCH, r * PITCH
    lit = peak = 0
    for y in range(y0, min(y0 + CELL, h)):
        line = px[y]
        for x in range(x0, min(x0 + CELL, w)):
            v = max(line[x * ch], line[x * ch + 1], line[x * ch + 2])
            if v > 24:
                lit += 1
                peak = max(peak, v)
    if lit:
        report.append((lit, peak, n))
NL, TAB = chr(10), chr(9)
out = SP / f"diff_{tag}.txt"
out.write_text(NL.join(f"{n}{TAB}{lit}{TAB}{peak}" for lit, peak, n in report),
               encoding="utf8")
print(f"{tag}: celle {len(names)}, con pixel accesi {len(report)} -> {out.name}")
