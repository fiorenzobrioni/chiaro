#!/usr/bin/env python3
"""Derives the vivid palette's semantic tokens and sky bands (DESIGN.md §2.5, §3.7).

The vivid palette is not the paper one with a saturation slider pushed, and it is not
a second set of colors picked by hand either. It is **one rule, applied to every token
paper already has**:

    keep the hue, hold the WCAG luminance, and take the chroma to the sRGB gamut edge
    — or to BOOST times paper's chroma, whichever comes first.

Holding luminance is what makes this safe rather than brave. WCAG contrast is a
function of luminance alone, so every ratio DESIGN.md prints, every monotonic ramp,
every "darker after sunset" ordering and the scrim contract of §3.6 hold for the
vivid palette **because they hold for the paper one** — the two are the same picture
at two saturations, not two pictures.

The BOOST ceiling is what keeps a rule from becoming a caricature. Without it, a token
that is deliberately near-neutral — the diverging ramp's midpoint, `unknown`, a night
sky — would be dragged to the edge of the gamut and stop being neutral, which is the
one thing those tokens are for. With it, a near-neutral stays near-neutral and only
the tokens that were already carrying a hue go to the edge.

The consequence worth stating: several tokens do not move at all. After the 3 set 2026
color pass most of the light inks already sit ON the gamut edge at the luminance their
contrast floor allows, so vivid prints the same hex. That is a fact about sRGB at those
luminances, not a shortcut.

    python3 tools/gen_vivid.py            # the Kotlin blocks, plus a report on stderr
"""
from __future__ import annotations

import pathlib
import re
import sys

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))

from color_math import (  # noqa: E402
    at_gamut_edge,
    contrast,
    hex_to_rgb,
    luminance,
    rgb_to_hex,
    rgb_to_oklch,
)

THEME = pathlib.Path("app/src/main/kotlin/com/callbackdev/chiaro/ui/theme")

#: How much more chroma a token may take than paper gives it. 1.8 was picked by
#: rendering the sheet and looking at it (tools/palette_sheet.py): at 1.4 the blue
#: hour is still a dusty indigo and the palette does not read as a second dress; past
#: 2.0 the near-neutrals start acquiring a hue and the diverging ramp's midpoint stops
#: being a midpoint.
BOOST = 1.8

VERDICT = re.compile(r"(\w+) = VerdictColors\(Color\(0xFF([0-9A-Fa-f]{6})\), Color\(0xFF([0-9A-Fa-f]{6})\)\)")
RAMP = re.compile(r"(rainRamp|rainInkRamp|temperatureRamp) = listOf\((.*?)\)\s*\n", re.S)
HEX = re.compile(r"0xFF([0-9A-Fa-f]{6})")
SKY = re.compile(
    r"(-?[\d.]+) to SkyGradient\(Color\(0xFF([0-9A-Fa-f]{6})\), "
    r"Color\(0xFF([0-9A-Fa-f]{6})\), Color\(0xFF([0-9A-Fa-f]{6})\)\)"
)


def read(name: str) -> str:
    path = THEME / name
    if not path.is_file():
        sys.exit(f"run me from the repo root: {path} not found")
    return path.read_text()


def vivid(paper_hex: str) -> str:
    """The one rule."""
    rgb = hex_to_rgb(paper_hex)
    _, chroma, hue = rgb_to_oklch(rgb)
    if chroma < 1e-4:
        return paper_hex.upper()
    got = at_gamut_edge(luminance(rgb), hue, chroma * BOOST)
    return paper_hex.upper() if got is None else rgb_to_hex(got)


def block(text: str, name: str) -> str:
    """One `internal val <name> = …( … )` declaration, and nothing after it.

    Slicing matters more than it looks: this tool writes its output into the very files
    it reads, so a loose split would feed the vivid values back in and boost them again.
    Running the generator twice must produce the same file, or it is not a generator.
    """
    start = text.index(f"internal val {name}")
    return text[start:text.index("\n)", start) + 2]


def paper_semantics() -> dict:
    text = read("ChiaroColors.kt")
    out = {}
    for label, name in (("light", "ChiaroLightColors"), ("dark", "ChiaroDarkColors")):
        chunk = block(text, name)
        verdicts = {n: (f"#{i.upper()}", f"#{c.upper()}") for n, i, c in VERDICT.findall(chunk)}
        ramps = {n: [f"#{h.upper()}" for h in HEX.findall(body)] for n, body in RAMP.findall(chunk)}
        if len(verdicts) != 4 or len(ramps) != 3:
            sys.exit(f"{label}: parsed {len(verdicts)} verdicts and {len(ramps)} ramps, expected 4 and 3")
        out[label] = {"verdicts": verdicts, **ramps}
    return out


def paper_sky() -> list:
    """The PAPER band table only — see [block] on why the slice is not optional."""
    text = read("SkyPalette.kt")
    start = text.index("val Paper = SkyPalette(")
    found = SKY.findall(text[start:text.index("\n        )", start)])
    if len(found) != 9:
        sys.exit(f"parsed {len(found)} sky anchors, expected 9 — did SkyPalette.kt change shape?")
    return [(m[0], (f"#{m[1].upper()}", f"#{m[2].upper()}", f"#{m[3].upper()}")) for m in found]


def surface(scheme_name: str) -> str:
    text = read("Scheme.kt")
    block = text[text.index(f"internal val {scheme_name}"):]
    return "#" + re.search(r"\n    surface = Color\(0xFF([0-9A-Fa-f]{6})\)", block).group(1).upper()


def ramp_block(name: str, values: list, per_line: int, last: bool) -> str:
    cells = [f"Color(0xFF{v.lstrip('#')})" for v in values]
    rows = [", ".join(cells[i:i + per_line]) for i in range(0, len(cells), per_line)]
    body = ",\n".join(f"        {row}" for row in rows)
    return f"    {name} = listOf(\n{body}\n    )" + ("" if last else ",")


def main() -> None:
    paper = paper_semantics()
    report = []

    for label in ("light", "dark"):
        name = "VividLightColors" if label == "light" else "VividDarkColors"
        ground = surface("VividLightScheme" if label == "light" else "VividDarkScheme")
        print(f"internal val {name} = ChiaroColors(")
        for verdict in ("pass", "unstable", "fail", "unknown"):
            p_ink, p_container = paper[label]["verdicts"][verdict]
            ink, container = vivid(p_ink), vivid(p_container)
            print(f"    {verdict} = VerdictColors("
                  f"Color(0xFF{ink.lstrip('#')}), Color(0xFF{container.lstrip('#')})),")
            report.append(
                f"{label:5s} {verdict:9s} ink {p_ink}->{ink} "
                f"{contrast(hex_to_rgb(ink), hex_to_rgb(ground)):5.2f}:1   "
                f"container {p_container}->{container} "
                f"on-container {contrast(hex_to_rgb(ink), hex_to_rgb(container)):5.2f}:1"
            )
        for i, (ramp, per_line) in enumerate((("rainRamp", 3), ("rainInkRamp", 3), ("temperatureRamp", 4))):
            values = [vivid(v) for v in paper[label][ramp]]
            print(ramp_block(ramp, values, per_line, last=(i == 2)))
            for before, after in zip(paper[label][ramp], values):
                report.append(
                    f"{label:5s} {ramp:15s} {before}->{after}  "
                    f"{contrast(hex_to_rgb(after), hex_to_rgb(ground)):5.2f}:1"
                )
        print(")\n")

    print("// VIVID SKY ANCHORS")
    for altitude, stops in paper_sky():
        got = [vivid(s) for s in stops]
        print(f"        {altitude} to SkyGradient(Color(0xFF{got[0].lstrip('#')}), "
              f"Color(0xFF{got[1].lstrip('#')}), Color(0xFF{got[2].lstrip('#')})),")
        report.append(f"sky {altitude:>6s}  {' '.join(stops)}  ->  {' '.join(got)}")

    print("\n".join(report), file=sys.stderr)


if __name__ == "__main__":
    main()
