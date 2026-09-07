#!/usr/bin/env python3
"""Renders the palette to an HTML sheet, for the step no test can do: looking at it.

The dataviz rule the design system borrows — "render it and look at it" — is what found
that the golden hour was not golden at 3°, which every contrast and monotonicity test in
the suite had happily passed.

It reads the hexes **out of the Kotlin sources**, never from a copy of them: a preview
that has its own idea of the palette is a preview that lies the first time somebody
retunes a token.

Both dresses are rendered, one under the other (DESIGN.md §2.5): a second palette you
cannot see beside the first is a second palette nobody compared.

    python3 tools/palette_sheet.py > /tmp/palette.html
    chromium --headless --screenshot=/tmp/palette.png --window-size=1160,2400 file:///tmp/palette.html
"""
from __future__ import annotations

import pathlib
import re
import sys

THEME = pathlib.Path("app/src/main/kotlin/com/callbackdev/chiaro/ui/theme")

SKY = re.compile(
    r"(-?[\d.]+) to SkyGradient\(Color\(0xFF([0-9A-Fa-f]{6})\), "
    r"Color\(0xFF([0-9A-Fa-f]{6})\), Color\(0xFF([0-9A-Fa-f]{6})\)\)"
)
VERDICT = re.compile(r"(\w+) = VerdictColors\(Color\(0xFF([0-9A-Fa-f]{6})\), Color\(0xFF([0-9A-Fa-f]{6})\)\)")
RAMP = re.compile(r"(rainRamp|rainInkRamp|temperatureRamp) = listOf\((.*?)\)\s*\n", re.S)
HEX = re.compile(r"0xFF([0-9A-Fa-f]{6})")


def read(name: str) -> str:
    path = THEME / name
    if not path.is_file():
        sys.exit(f"run me from the repo root: {path} not found")
    return path.read_text()


#: The two dresses, as the names their values carry in the Kotlin. Adding a third means
#: adding a row here and nothing else in this file.
DRESSES = (("paper", "Chiaro"), ("vivid", "Vivid"))


def anchors(dress: str) -> list[tuple[float, tuple[str, str, str]]]:
    """One band table. The two tables live in the same file, so the block is cut out by
    the `val <Name> = SkyPalette(` that introduces it rather than by reading the lot."""
    text = read("SkyPalette.kt")
    start = text.index(f"val {'Paper' if dress == 'paper' else 'Vivid'} = SkyPalette(")
    end = text.index("\n        )", start)
    found = SKY.findall(text[start:end])
    if len(found) != 9:
        sys.exit(f"{dress}: parsed {len(found)} sky anchors, expected 9 — did SkyPalette.kt change shape?")
    return [(float(a), (f"#{t}", f"#{m}", f"#{b}")) for a, t, m, b in found]


def semantic(prefix: str) -> dict:
    text = read("ChiaroColors.kt")
    start = text.index(f"internal val {prefix}LightColors")
    light = text[start:text.index(f"internal val {prefix}DarkColors")]
    dark_start = text.index(f"internal val {prefix}DarkColors")
    dark = text[dark_start:text.index("\n)", text.index("temperatureRamp", dark_start)) + 2]
    out = {}
    for label, chunk in (("light", light), ("dark", dark)):
        verdicts = {n: (f"#{i}", f"#{c}") for n, i, c in VERDICT.findall(chunk)}
        ramps = {n: [f"#{h}" for h in HEX.findall(body)] for n, body in RAMP.findall(chunk)}
        if len(verdicts) != 4 or len(ramps) != 3:
            sys.exit(f"{label}: parsed {len(verdicts)} verdicts and {len(ramps)} ramps, expected 4 and 3")
        out[label] = {"verdicts": verdicts, **ramps}
    return out


def surfaces(prefix: str) -> dict:
    text = read("Scheme.kt")

    def roles(name: str) -> dict:
        start = text.index(f"internal val {name}")
        chunk = text[start:text.index("\n)", start)]
        return {
            m.group(1): "#" + m.group(2)
            for m in re.finditer(r"(\w+) = Color\(0xFF([0-9A-Fa-f]{6})\)", chunk)
        }

    return {"light": roles(f"{prefix}LightScheme"), "dark": roles(f"{prefix}DarkScheme")}


def mix(a: str, b: str, t: float) -> str:
    pa = [int(a.lstrip("#")[i:i + 2], 16) for i in (0, 2, 4)]
    pb = [int(b.lstrip("#")[i:i + 2], 16) for i in (0, 2, 4)]
    return "#{:02X}{:02X}{:02X}".format(*[round(x + (y - x) * t) for x, y in zip(pa, pb)])


def sky_at(altitude: float, table) -> tuple[str, str, str]:
    altitude = max(-90.0, min(90.0, altitude))
    upper = [a for a in table if a[0] >= altitude][-1]
    lower = [a for a in table if a[0] <= altitude][0]
    if upper[0] == lower[0]:
        return upper[1]
    t = (upper[0] - altitude) / (upper[0] - lower[0])
    return tuple(mix(u, l, t) for u, l in zip(upper[1], lower[1]))


def main() -> None:
    # Windows consoles and redirects default to a legacy codepage that cannot
    # carry the verdict glyphs; the sheet declares utf-8, so stdout must be it.
    sys.stdout.reconfigure(encoding="utf-8")
    p = print
    p("<html><head><meta charset=utf-8><style>"
      "body{font-family:system-ui;margin:0}"
      "h2{font-size:12px;letter-spacing:.08em;text-transform:uppercase;color:#7C7768;margin:20px 16px 8px}"
      ".row{display:flex;gap:4px;padding:0 16px}.b{flex:1;height:120px;border-radius:8px;position:relative;overflow:hidden}"
      ".l{position:absolute;bottom:0;left:0;right:0;color:#fff;font-size:11px;padding:14px 5px 5px;"
      "background:linear-gradient(transparent,rgba(16,18,22,.55))}"
      ".ramp{display:flex;height:32px;border-radius:8px;overflow:hidden;margin:0 16px}.ramp div{flex:1}"
      ".chips{display:flex;gap:8px;padding:0 16px}"
      ".figures{display:flex;gap:16px;padding:0 16px;font-size:20px;font-weight:600}"
      ".chip{padding:6px 12px;border-radius:99px;font-size:13px;font-weight:500}"
      ".dark{padding:1px 0 24px;margin-top:24px}.dark h2{color:#969081}"
      "h1{font:600 15px system-ui;letter-spacing:.04em;margin:0;padding:20px 16px 0}"
      ".sheet{padding-bottom:24px}"
      "</style></head><body>")

    for dress, prefix in DRESSES:
        sheet(p, dress, anchors(dress), semantic(prefix), surfaces(prefix))
    p("</body></html>")


def sheet(p, dress: str, table, sem: dict, sur: dict) -> None:
    p("<div class=sheet style='background:%s;color:%s'>"
      % (sur["light"]["surface"], sur["light"]["onSurface"]))
    p(f"<h1>{dress}</h1>")
    p("<h2>The sky, degree by degree</h2><div class=row>")
    for altitude in (30, 12, 8, 6, 4, 2, 0, -2, -4, -6, -9, -12, -15, -18, -40):
        s = sky_at(altitude, table)
        p(f"<div class=b style='background:linear-gradient(180deg,{s[0]},{s[1]},{s[2]})'>"
          f"<span class=l>{altitude}°</span></div>")
    p("</div>")

    for mode in ("light", "dark"):
        if mode == "dark":
            p("<div class=dark style='background:%s;color:%s'>"
              % (sur["dark"]["surface"], sur["dark"]["onSurface"]))
        p(f"<h2>Rain ramp, the marks · {mode}</h2><div class=ramp>")
        for c in sem[mode]["rainRamp"]:
            p(f"<div style='background:{c}'></div>")
        # The ink ramp is drawn as INK, on the surface it was measured against: a
        # printed probability is the thing it has to survive, and a swatch of it would
        # show the one property nobody needs (its fill) and hide the one that matters.
        p(f"</div><h2>Rain ramp, the figures · {mode}</h2><div class=figures>")
        for i, c in enumerate(sem[mode]["rainInkRamp"]):
            p(f"<span style='color:{c}'>{i * 25}%</span>")
        p(f"</div><h2>Temperature ramp · {mode}</h2><div class=ramp>")
        for c in sem[mode]["temperatureRamp"]:
            p(f"<div style='background:{c}'></div>")
        p(f"</div><h2>Verdicts · {mode}</h2><div class=chips>")
        for name, glyph in (("pass", "✓"), ("unstable", "~"), ("fail", "✗"), ("unknown", "?")):
            ink, container = sem[mode]["verdicts"][name]
            p(f"<span class=chip style='background:{container};color:{ink}'>{glyph} {name}</span>")
        p("</div>")
        p(f"<h2>Surfaces · {mode}</h2><div class=ramp>")
        for role in ("surfaceContainerLowest", "surfaceContainerLow", "surfaceContainer",
                     "surfaceContainerHigh", "surfaceContainerHighest", "surfaceDim"):
            p(f"<div style='background:{sur[mode][role]}'></div>")
        p("</div>")
        if mode == "dark":
            p("</div>")
    p("</div>")


if __name__ == "__main__":
    main()
