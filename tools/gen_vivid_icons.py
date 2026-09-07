#!/usr/bin/env python3
"""Generates the LINE icon set's dark-ground sibling, `mcn_*` (DESIGN.md §13.1).

The line set ships once and serves both surfaces, so its luminances are pinned into
[0.120, 0.283] — the band that clears the 3:1 non-text floor against a near-white page
AND a near-black one. That band is why the sun is a bronze: at Y ≤ 0.284 sRGB has no
bright yellow, and no amount of chroma invents one.

On a DARK ground the ceiling does not exist. This set takes it away, with the same rule
the rest of the vivid palette uses (`tools/gen_vivid.py`):

    keep the hue, take the chroma to the sRGB gamut edge or to BOOST times the shipped
    chroma — whichever comes first — and choose, among the luminances where that much
    chroma is available and the 3:1 floor still holds, the one closest to the shipped
    value.

The tie-break is what keeps it from being a caricature. A grey is a grey because its
chroma is small; BOOST of a small number is still a small number, so the cloud strokes
can reach their target chroma almost anywhere and stay where they are. The sun cannot:
amber runs out of gamut long before the cap, so it climbs to where the gamut is widest
and comes out at #FFA500, which is what anybody means by a yellow sun.

Only the vivid palette reads this set (`ChiaroIcons.styledRes`). Paper keeps the
both-grounds set, because the muted-but-consistent line family is part of what "paper"
means and moving it would be a redesign of the default nobody asked for.

    python3 tools/gen_vivid_icons.py          # writes app/src/main/res/drawable/mcn_*.xml
"""
from __future__ import annotations

import pathlib
import re
import sys

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))

from color_math import (  # noqa: E402
    contrast,
    hex_to_rgb,
    luminance,
    max_chroma,
    oklch_to_rgb,
    rgb_to_hex,
    rgb_to_oklch,
)

DRAWABLE = pathlib.Path("app/src/main/res/drawable")

#: The same ceiling as the rest of the vivid palette, and for the same reason.
BOOST = 1.8

#: The ground this set is picked for: `VividDarkScheme.surface`. Read from Scheme.kt so
#: a regenerated scheme cannot leave the icons measured against a surface that is gone.
DARK_SURFACE_ROLE = ("VividDarkScheme", "surface")

#: DESIGN §10's non-text floor, plus the margin 8-bit quantization can eat.
FLOOR = 3.05

COLOR_ATTR = re.compile(r'(android:(?:strokeColor|fillColor)=")#([0-9A-Fa-f]{6})(")')

HEADER_OLD = "Recolored for measured contrast on the\n     ground its set is picked for; the tables are in the tool. Do not edit\n     by hand."
HEADER_NEW = ("Recolored by tools/gen_vivid_icons.py for a DARK\n     ground and the vivid palette (DESIGN.md §13.1): the both-grounds\n"
              "     luminance ceiling lifted, chroma at the gamut edge. Do not edit by\n     hand.")


def dark_surface() -> str:
    text = (pathlib.Path("app/src/main/kotlin/com/callbackdev/chiaro/ui/theme/Scheme.kt")
            .read_text())
    block = text[text.index(f"internal val {DARK_SURFACE_ROLE[0]}"):]
    found = re.search(rf"\n    {DARK_SURFACE_ROLE[1]} = Color\(0xFF([0-9A-Fa-f]{{6}})\)", block)
    if not found:
        sys.exit("no vivid dark surface in Scheme.kt — did it change shape?")
    return "#" + found.group(1).upper()


def lifted(value: str, floor_y: float, steps: int = 500) -> str:
    rgb = hex_to_rgb(value)
    shipped_y = luminance(rgb)
    _, chroma, hue = rgb_to_oklch(rgb)
    if chroma < 1e-4:
        return value.upper()
    cap = chroma * BOOST
    best = None
    for i in range(steps + 1):
        lightness = i / steps
        available = min(max_chroma(lightness, hue), cap)
        out = tuple(min(1.0, max(0.0, v)) for v in oklch_to_rgb(lightness, available, hue))
        if luminance(out) < floor_y:
            continue
        key = (round(available, 4), -abs(luminance(out) - shipped_y))
        if best is None or key > best[0]:
            best = (key, out)
    return value.upper() if best is None else rgb_to_hex(best[1])


def main() -> None:
    if not DRAWABLE.is_dir():
        sys.exit(f"run me from the repo root: {DRAWABLE} not found")
    ground = dark_surface()
    floor_y = FLOOR * (luminance(hex_to_rgb(ground)) + 0.05) - 0.05

    sources = sorted(DRAWABLE.glob("mc_*.xml"))
    if not sources:
        sys.exit("no mc_*.xml to read — run tools/import_meteocons.py first")

    table: dict[str, str] = {}
    for path in sources:
        for _, value, _ in COLOR_ATTR.findall(path.read_text()):
            key = "#" + value.upper()
            table.setdefault(key, lifted(key, floor_y))

    for path in sources:
        text = path.read_text().replace(HEADER_OLD, HEADER_NEW)
        text = COLOR_ATTR.sub(lambda m: m.group(1) + table["#" + m.group(2).upper()] + m.group(3), text)
        (DRAWABLE / ("mcn_" + path.name[len("mc_"):])).write_text(text)

    print(f"{len(sources)} drawables written as mcn_*, on {ground}", file=sys.stderr)
    for before, after in sorted(table.items(), key=lambda kv: -contrast(hex_to_rgb(kv[1]), hex_to_rgb(ground))):
        print("  %s -> %s   %.2f:1 -> %.2f:1 on %s" % (
            before, after,
            contrast(hex_to_rgb(before), hex_to_rgb(ground)),
            contrast(hex_to_rgb(after), hex_to_rgb(ground)), ground), file=sys.stderr)


if __name__ == "__main__":
    main()
