#!/usr/bin/env python3
"""Converts the Meteocons the app uses into Android vector drawables.

The seam is `ui/icons/ChiaroIcons` (DESIGN.md §13.1): this tool fills the drawable side
of it, one `mc_*.xml` per icon, from a checkout of basmilius/meteocons at **v2.0.0** —
the release the Fase 1 decision described. The v3 rewrite on `main` is a different
drawing (128px, filled paths) and must not be mixed into this family.

    git clone --branch v2.0.0 --depth 1 https://github.com/basmilius/meteocons ../meteocons
    python tools/import_meteocons.py ../meteocons

Three deliberate departures from the source, each the kind that must be written down:

1. **The palette is re-anchored, not copied.** Meteocons' line set is drawn for a dark
   backdrop: its cloud stroke is #E5E7EB, which is 1.18:1 against Chiaro's light surface
   — invisible, and the icons are the only carrier of "what kind of weather" in the hour
   strip. Every hue is kept, every luminance is moved into the band that clears the 3:1
   non-text floor (DESIGN.md §10) against BOTH surfaces (#FCF9F3 / #16130E). The numbers
   live in REMAP below and `IconContrastTest` re-measures the emitted XML on every build.
2. **The SMIL animations are stripped.** Android vector drawables cannot carry them, and
   the static set is what Fase 2 needs. If animated states come later they come as AVDs,
   as their own decision.
3. **Dashes are re-drawn, not emulated.** VectorDrawable has no stroke-dasharray. Dashed
   circles (the moon's dark limb) and dashed straight segments (star trails, dust) are
   synthesized as real arcs and segments; the two dashed wind curls become solid — the
   dash there only existed to be animated.

The FILL set (the second icon theme, chosen in Settings) adds three departures of its own:

4. **Gradients are flattened to their face color.** Every fill is a two-stop gradient
   whose first stop covers 45% flat; at 24-32dp the ramp is invisible and the face stop
   IS the icon's color. VectorDrawable gradients exist but would buy nothing here.
5. **Hairline edge strokes (width 0.5) are dropped.** They exist to edge a near-white
   fill against a white page; after the re-anchor below the fills are deep and carry
   their own edge. Real drawing strokes (width >= 1, the sun's rays) stay and remap.
   The fill palette gets its own table (FILL_REMAP): same hues, luminances moved into
   the same [0.120, 0.283] band, order inside each hue family preserved so a shaded
   element stays darker than its lit neighbour.
6. **The fill set ships twice, picked by the ground it sits on** (icon pass, 3 set
   2026). One asset owing 3:1 on BOTH surfaces is forced into Y in [0.120, 0.283] --
   correct arithmetic, muted result. mcf_* keeps that band for LIGHT grounds, its
   chroma raised x1.25 at held WCAG luminance (the color pass' trick: same measured
   ratios, fuller color -- the line set gets the same treatment); mcfn_* is
   Meteocons' own fill palette, verbatim except the four near-black details lifted
   to clear 3:1 against the DARK surface. ChiaroIcons picks the set by ground (the
   app by its applied theme, the widgets by their card's darkGround), so each set
   only ever meets the surface its floor was measured against, and
   IconContrastTest measures each set against ITS surface.

Requires no third-party packages, runs from the repo root.
"""
from __future__ import annotations

import math
import pathlib
import re
import sys
import xml.etree.ElementTree as ET

OUT = pathlib.Path("app/src/main/res/drawable")
SRC_SUBDIR = pathlib.Path("production/line/all")
FILL_SRC_SUBDIR = pathlib.Path("production/fill/all")

# svg name (in production/line/all) -> drawable name. The subset the app exposes
# through ChiaroIcons; growing it is: add a line, re-run, add the accessor.
ICONS = {
    # WMO condition buckets (day/night where the sky differs)
    "clear-day": "mc_clear_day",
    "clear-night": "mc_clear_night",
    "partly-cloudy-day": "mc_partly_cloudy_day",
    "partly-cloudy-night": "mc_partly_cloudy_night",
    "overcast": "mc_overcast",
    "cloudy": "mc_cloudy",
    "fog-day": "mc_fog_day",
    "fog-night": "mc_fog_night",
    "drizzle": "mc_drizzle",
    "rain": "mc_rain",
    "sleet": "mc_sleet",
    "snow": "mc_snow",
    "partly-cloudy-day-rain": "mc_partly_cloudy_day_rain",
    "partly-cloudy-night-rain": "mc_partly_cloudy_night_rain",
    "partly-cloudy-day-snow": "mc_partly_cloudy_day_snow",
    "partly-cloudy-night-snow": "mc_partly_cloudy_night_snow",
    "thunderstorms": "mc_thunderstorms",
    "thunderstorms-rain": "mc_thunderstorms_rain",
    "not-available": "mc_not_available",
    # the details grid
    "wind": "mc_wind",
    "humidity": "mc_humidity",
    "uv-index": "mc_uv_index",
    "thermometer": "mc_thermometer",
    "barometer": "mc_barometer",
    "raindrop": "mc_raindrop",
    "raindrops": "mc_raindrops",
    "mist": "mc_mist",
    "umbrella": "mc_umbrella",
    "snowflake": "mc_snowflake",
    "dust": "mc_dust",
    "smoke-particles": "mc_smoke_particles",
    "solar-eclipse": "mc_solar_eclipse",
    "compass": "mc_compass",
    # the day's timeline and the Sky screen
    "sunrise": "mc_sunrise",
    "sunset": "mc_sunset",
    "horizon": "mc_horizon",
    "star": "mc_star",
    "starry-night": "mc_starry_night",
    "falling-stars": "mc_falling_stars",
    "moonrise": "mc_moonrise",
    "moonset": "mc_moonset",
    "moon-new": "mc_moon_new",
    "moon-waxing-crescent": "mc_moon_waxing_crescent",
    "moon-first-quarter": "mc_moon_first_quarter",
    "moon-waxing-gibbous": "mc_moon_waxing_gibbous",
    "moon-full": "mc_moon_full",
    "moon-waning-gibbous": "mc_moon_waning_gibbous",
    "moon-last-quarter": "mc_moon_last_quarter",
    "moon-waning-crescent": "mc_moon_waning_crescent",
}

# Source hue -> shipped color. Hue kept, luminance moved into [0.120, 0.283], the band
# where a mark clears 3:1 against both surfaces of DESIGN.md §2.2 (light 0.9492, dark
# 0.0067). Measured with the same arithmetic PaletteContrastTest uses, and
# IconContrastTest holds the floor. Since the icon pass (3 set 2026) the shipped
# values also carry the color pass' trick -- chroma x1.25 at held luminance -- so the
# ratios kept their numbers while the color filled out.
REMAP = {
    "#f59e0b": "#C37D00",  # sun amber            3.20 / 5.52
    "#fcd34d": "#C37D00",  # star yellow -> the one amber; two ambers said nothing
    "#fde68a": "#C37D00",  # dust yellow -> same
    "#72b9d5": "#008AB6",  # moon blue            3.76 / 4.68
    "#72b8d4": "#008AB6",  # snow blue -> unified with the moon; they were 1 bit apart
    "#e5e7eb": "#7B87A0",  # cloud stroke         3.44 / 5.13
    "#9ca3af": "#636E82",  # back cloud           4.89 / 3.60
    "#d1d5db": "#6D7D94",  # fog bands            3.99 / 4.42
    "#374151": "#556A89",  # needles, arrows      5.25 / 3.36
    "#2885c7": "#0085D0",  # rain blue            3.79 / 4.65
    "#ef4444": "#FF2634",  # thermometer red      3.58 / 4.92
}

# The fill set for LIGHT grounds (mcf_*), re-anchored exactly like REMAP above: hue
# kept, luminance moved into the [0.120, 0.283] band, chroma x1.25 at held luminance
# since the icon pass. Ratios measured with IconContrastTest's arithmetic
# (light / dark).
FILL_REMAP = {
    "#ffffff": "#8E8E8E",  # 3.12 / 5.65  compass needle half, drop shine
    "#f3f7fe": "#8B8F94",  # 3.10 / 5.69  cloud face
    "#e6effc": "#7D838C",  # 3.64 / 4.85  cloud mid
    "#deeafb": "#6F7883",  # 4.26 / 4.14  cloud shade
    "#e5e7eb": "#7B87A0",  # 3.44 / 5.13  (reused from the line table)
    "#d4d7dd": "#838489",  # 3.55 / 4.96  fog/mist bands
    "#bec1c6": "#7B7D82",  # 3.92 / 4.50
    "#b8bdc6": "#777B83",  # 4.04 / 4.36
    "#afb4bc": "#72767C",  # 4.35 / 4.06
    "#a5aab2": "#6D7179",  # 4.66 / 3.78
    "#9ca3af": "#636E82",  # 4.89 / 3.60  (reused from the line table)
    "#848b98": "#626976",  # 5.26 / 3.35
    "#6b7280": "#5D6675",  # 5.52 / 3.20
    "#515a69": "#596579",  # 5.61 / 3.14
    "#384354": "#506482",  # 5.73 / 3.08
    "#374151": "#556A89",  # 5.25 / 3.36  (reused from the line table)
    "#fcd966": "#AA8C1B",  # 3.09 / 5.71  star gold, lit
    "#fcd34d": "#A48400",  # 3.40 / 5.19
    "#fccd34": "#9F7E00",  # 3.66 / 4.82
    "#fde68a": "#A08D40",  # 3.13 / 5.63  dust
    "#fde171": "#9F8725",  # 3.35 / 5.26
    "#fbbf24": "#B28500",  # 3.20 / 5.51  sun face
    "#f7b23b": "#B67C00",  # 3.40 / 5.18
    "#f8af18": "#B17B00",  # 3.50 / 5.03  sun edge
    "#f6a823": "#B17500",  # 3.69 / 4.78
    "#f59e0b": "#B17000",  # 3.85 / 4.58  sun shade
    "#86c3db": "#5794AB",  # 3.21 / 5.50  moon lit
    "#72b9d5": "#008AB6",  # 3.76 / 4.68  (reused from the line table)
    "#5eafcf": "#2C809F",  # 4.26 / 4.14  moon shade
    "#4286ee": "#3184FF",  # 3.40 / 5.18
    "#0950bc": "#005EE3",  # 5.39 / 3.27  rain shade
    "#3392d6": "#0092E3",  # 3.21 / 5.49
    "#2885c7": "#0085D0",  # 3.79 / 4.65
    "#2477b2": "#0077BB",  # 4.59 / 3.84
    "#ef4444": "#FF2634",  # 3.58 / 4.92
    "#dc2626": "#E50017",  # 4.60 / 3.83
    "#f87171": "#E85256",  # 3.46 / 5.09  umbrella red, lit
}

# The fill set for DARK grounds (mcfn_*): Meteocons' own palette, verbatim -- the
# backdrop it was drawn for -- except the four near-black details, lifted at held hue
# and chroma to clear 3:1 against the dark surface (Y floor 0.1212). Ratios as above.
FILL_NIGHT = {
    "#ffffff": "#FFFFFF",  # 1.05 / 18.52
    "#f3f7fe": "#F3F7FE",  # 1.02 / 17.24  cloud face
    "#e6effc": "#E6EFFC",  # 1.10 / 15.98  cloud mid
    "#deeafb": "#DEEAFB",  # 1.16 / 15.23  cloud shade
    "#e5e7eb": "#E5E7EB",  # 1.18 / 14.96
    "#d4d7dd": "#D4D7DD",  # 1.37 / 12.85  fog/mist bands
    "#bec1c6": "#BEC1C6",  # 1.72 / 10.26
    "#b8bdc6": "#B8BDC6",  # 1.80 / 9.82
    "#afb4bc": "#AFB4BC",  # 1.98 / 8.89
    "#a5aab2": "#A5AAB2",  # 2.22 / 7.93
    "#9ca3af": "#9CA3AF",  # 2.42 / 7.30
    "#848b98": "#848B98",  # 3.26 / 5.41
    "#6b7280": "#6B7280",  # 4.60 / 3.83
    "#515a69": "#596271",  # 5.86 / 3.01  lifted (was 2.66:1 on dark)
    "#384354": "#576275",  # 5.86 / 3.01  lifted (was 1.85:1 on dark)
    "#374151": "#586274",  # 5.85 / 3.01  lifted (was 1.80:1 on dark)
    "#fcd966": "#FCD966",  # 1.31 / 13.46  star gold, lit
    "#fcd34d": "#FCD34D",  # 1.37 / 12.85
    "#fccd34": "#FCCD34",  # 1.44 / 12.28
    "#fde68a": "#FDE68A",  # 1.19 / 14.87  dust
    "#fde171": "#FDE171",  # 1.23 / 14.28
    "#fbbf24": "#FBBF24",  # 1.59 / 11.10  sun face
    "#f7b23b": "#F7B23B",  # 1.76 / 10.04
    "#f8af18": "#F8AF18",  # 1.79 / 9.82  sun edge
    "#f6a823": "#F6A823",  # 1.90 / 9.30
    "#f59e0b": "#F59E0B",  # 2.04 / 8.63  sun shade
    "#86c3db": "#86C3DB",  # 1.84 / 9.56  moon lit
    "#72b9d5": "#72B9D5",  # 2.08 / 8.48
    "#5eafcf": "#5EAFCF",  # 2.35 / 7.51  moon shade
    "#4286ee": "#4286EE",  # 3.40 / 5.18
    "#0950bc": "#195CC9",  # 5.85 / 3.01  lifted (was 2.55:1 on dark), rain shade
    "#3392d6": "#3392D6",  # 3.21 / 5.49
    "#2885c7": "#2885C7",  # 3.79 / 4.65
    "#2477b2": "#2477B2",  # 4.59 / 3.84
    "#ef4444": "#EF4444",  # 3.58 / 4.92
    "#dc2626": "#DC2626",  # 4.60 / 3.84
    "#f87171": "#F87171",  # 2.63 / 6.70  umbrella red, lit
}

# The set being converted right now; main() flips this between the two passes.
ACTIVE = {"palette": REMAP, "drop_hairline": False, "prefix": "mc_"}

# Gradient id -> face color of the current file (departure #4).
GRADIENTS = {}

SVG_NS = "{http://www.w3.org/2000/svg}"

LINECAP = {"butt": "butt", "round": "round", "square": "square"}
LINEJOIN = {"miter": "miter", "round": "round", "bevel": "bevel"}

HEADER = (
    "<!-- Generated by tools/import_meteocons.py from Meteocons v2.0.0\n"
    "     (github.com/basmilius/meteocons), MIT, (c) 2020-2021 Bas Milius —\n"
    "     licenses/Meteocons-MIT.txt. Recolored for measured contrast on the\n"
    "     ground its set is picked for; the tables are in the tool. Do not edit\n"
    "     by hand. -->\n"
)


def remap(color: str) -> str:
    c = color.lower()
    if c.startswith("#") and len(c) == 4:  # #fff -> #ffffff
        c = "#" + "".join(ch * 2 for ch in c[1:])
    if c.startswith("url("):
        m = re.match(r"url\(#(.+)\)", c)
        face = GRADIENTS.get(m.group(1)) if m else None
        if face is None:
            sys.exit(f"paint references unknown gradient: {color}")
        c = face.lower()
    mapped = ACTIVE["palette"].get(c)
    if mapped is None:
        sys.exit(f"unmapped color {c}: add it to the active table with a measured value")
    return mapped


def norm_path_data(d: str) -> str:
    """Re-tokenizes SVG path data with explicit separators.

    Android's PathParser is stricter than a browser: packed arc flags ("1132" for
    large-arc=1 sweep=1 x=32) parse in Chrome and crash at inflate time on device.
    Tokenizing arc arguments flag-by-flag is the whole reason this function exists.
    """
    out: list[str] = []
    i, n = 0, len(d)
    NUM = re.compile(r"[-+]?(?:\d*\.\d+|\d+\.?)(?:[eE][-+]?\d+)?")
    FLAG = re.compile(r"[01]")
    cmd = ""

    def skip_sep(j: int) -> int:
        while j < n and (d[j].isspace() or d[j] == ","):
            j += 1
        return j

    def read_number(j: int):
        j = skip_sep(j)
        m = NUM.match(d, j)
        if not m:
            sys.exit(f"path parse error at {j} in: {d[:80]}...")
        return m.group(0), m.end()

    def read_flag(j: int):
        j = skip_sep(j)
        m = FLAG.match(d, j)
        if not m:
            sys.exit(f"arc flag expected at {j} in: {d[:80]}...")
        return m.group(0), m.end()

    ARITY = {"m": 2, "l": 2, "h": 1, "v": 1, "c": 6, "s": 4, "q": 4, "t": 2, "a": 7, "z": 0}
    while i < n:
        i = skip_sep(i)
        if i >= n:
            break
        if d[i].isalpha():
            cmd = d[i]
            out.append(cmd)
            i += 1
            if cmd.lower() == "z":
                continue
        elif not cmd:
            sys.exit(f"path data starts with a number: {d[:40]}")
        else:
            # implicit repeat of the previous command (an m repeats as l per SVG,
            # which PathParser also implements)
            if cmd == "M":
                cmd = "L"
                out.append(cmd)
            elif cmd == "m":
                cmd = "l"
                out.append(cmd)
        arity = ARITY[cmd.lower()]
        args: list[str] = []
        for k in range(arity):
            if cmd.lower() == "a" and k in (3, 4):
                tok, i = read_flag(i)
            else:
                tok, i = read_number(i)
            args.append(tok)
        out.append(" ".join(args))
    return " ".join(out)


def fmt(x: float) -> str:
    return f"{x:.2f}".rstrip("0").rstrip(".")


def dashed_circle_data(cx: float, cy: float, r: float, dash: float, gap: float,
                       start_deg: float) -> str:
    """A dashed circle as explicit arcs, evenly distributed so there is no seam."""
    circumference = 2 * math.pi * r
    periods = max(1, round(circumference / (dash + gap)))
    step = 2 * math.pi / periods
    dash_angle = dash / r
    parts = []
    for k in range(periods):
        a0 = math.radians(start_deg) + k * step
        a1 = a0 + dash_angle
        x0, y0 = cx + r * math.cos(a0), cy + r * math.sin(a0)
        x1, y1 = cx + r * math.cos(a1), cy + r * math.sin(a1)
        large = 1 if dash_angle > math.pi else 0
        parts.append(
            f"M {fmt(x0)} {fmt(y0)} A {fmt(r)} {fmt(r)} 0 {large} 1 {fmt(x1)} {fmt(y1)}"
        )
    return " ".join(parts)


LINE_D = re.compile(
    r"^M\s*([-\d.]+)[ ,]([-\d.]+)\s*([lL])\s*([-\d.]+)[ ,]([-\d.]+)$"
)


def dashed_line_data(d: str, dash: float, gap: float) -> str:
    """A dashed straight segment as explicit sub-segments. Only the M..l shape the
    source actually uses; anything else must be a conscious decision, so: error."""
    m = LINE_D.match(d.strip())
    if not m:
        sys.exit(f"dasharray on a non-straight path; decide by hand: {d}")
    x0, y0 = float(m.group(1)), float(m.group(2))
    dx, dy = float(m.group(4)), float(m.group(5))
    if m.group(3) == "L":
        dx, dy = dx - x0, dy - y0
    length = math.hypot(dx, dy)
    ux, uy = dx / length, dy / length
    parts, t = [], 0.0
    while t < length:
        end = min(t + dash, length)
        parts.append(
            f"M {fmt(x0 + ux * t)} {fmt(y0 + uy * t)} "
            f"L {fmt(x0 + ux * end)} {fmt(y0 + uy * end)}"
        )
        t += dash + gap
    return " ".join(parts)


def parse_dasharray(value: str) -> tuple[float, float]:
    nums = [float(v) for v in re.split(r"[ ,]+", value.strip()) if v]
    if len(nums) == 1:
        return nums[0], nums[0]
    if len(nums) == 2:
        return nums[0], nums[1]
    sys.exit(f"unsupported dasharray: {value}")


def parse_rotate(transform: str) -> float:
    m = re.match(r"^rotate\(\s*([-\d.]+)(?:[ ,]+[-\d.]+[ ,]+[-\d.]+)?\s*\)$", transform)
    if not m:
        sys.exit(f"unsupported transform: {transform}")
    return float(m.group(1))


NL8 = chr(10) + " " * 8


def stroke_attrs(el: ET.Element) -> str:
    """The paint of one SVG shape, as vector-drawable attributes."""
    a = []
    fill = el.get("fill", "")
    if fill and fill != "none":
        a.append(f'android:fillColor="{remap(fill)}"')
    stroke = el.get("stroke")
    if stroke and stroke != "none":
        width = float(el.get("stroke-width", "1"))
        if ACTIVE["drop_hairline"] and width < 1.0:
            return NL8.join(a)  # departure #5: the edge hairline goes
        a.append(f'android:strokeColor="{remap(stroke)}"')
        a.append(f'android:strokeWidth="{el.get("stroke-width", "1")}"')
        cap = el.get("stroke-linecap")
        if cap:
            a.append(f'android:strokeLineCap="{LINECAP[cap]}"')
        join = el.get("stroke-linejoin")
        if join:
            a.append(f'android:strokeLineJoin="{LINEJOIN[join]}"')
        miter = el.get("stroke-miterlimit")
        if miter:
            a.append(f'android:strokeMiterLimit="{miter}"')
    return "\n        ".join(a)


def circle_data(cx: float, cy: float, r: float) -> str:
    return (
        f"M {fmt(cx - r)} {fmt(cy)} "
        f"a {fmt(r)} {fmt(r)} 0 1 0 {fmt(2 * r)} 0 "
        f"a {fmt(r)} {fmt(r)} 0 1 0 {fmt(-2 * r)} 0 Z"
    )


def convert_shape(el: ET.Element, lines: list[str], indent: str) -> None:
    tag = el.tag.removeprefix(SVG_NS)
    if tag in ("animate", "animateTransform", "animateMotion"):
        return  # SMIL does not travel; departure #2 in the module docstring
    if el.get("opacity") or el.get("fill-opacity") or el.get("stroke-opacity"):
        sys.exit(f"opacity on <{tag}>; decide by hand")
    if tag == "g":
        children = list(el)
        clip = el.get("clip-path")
        if el.get("transform"):
            sys.exit(f"static transform on a group; decide by hand: {el.get('transform')}")
        if clip:
            m = re.match(r"url\(#(.+)\)", clip)
            d = CLIPS.get(m.group(1)) if m else None
            if d is None:
                sys.exit(f"clip-path references unknown id: {clip}")
            lines.append(f"{indent}<group>")
            lines.append(f'{indent}    <clip-path android:pathData="{norm_path_data(d)}"/>')
            for child in children:
                convert_shape(child, lines, indent + "    ")
            lines.append(f"{indent}</group>")
        else:
            for child in children:
                convert_shape(child, lines, indent)
        return
    if tag == "circle":
        cx, cy, r = (float(el.get(k)) for k in ("cx", "cy", "r"))
        dasharray = el.get("stroke-dasharray")
        if dasharray:
            dash, gap = parse_dasharray(dasharray)
            start = parse_rotate(el.get("transform")) if el.get("transform") else 0.0
            d = dashed_circle_data(cx, cy, r, dash, gap, start)
        else:
            if el.get("transform"):
                sys.exit("transform on an undashed circle; decide by hand")
            d = circle_data(cx, cy, r)
        lines.append(f'{indent}<path\n{indent}    android:pathData="{d}"\n{indent}    {stroke_attrs(el)}/>')
        return
    if tag == "path":
        if el.get("transform"):
            sys.exit(f"static transform on a path; decide by hand: {el.get('transform')}")
        dasharray = el.get("stroke-dasharray")
        d = el.get("d")
        if dasharray:
            # The wind curls dash only to be animated; solid is the honest static form.
            if LINE_D.match(d.strip()):
                dash, gap = parse_dasharray(dasharray)
                d = dashed_line_data(d, dash, gap)
            else:
                pass  # keep the geometry, drop the dash
        lines.append(
            f'{indent}<path\n{indent}    android:pathData="{norm_path_data(d)}"\n{indent}    {stroke_attrs(el)}/>'
        )
        return
    if tag == "defs":
        return
    sys.exit(f"unhandled SVG element <{tag}>")


CLIPS: dict[str, str] = {}


def convert(svg_path: pathlib.Path, out_path: pathlib.Path) -> None:
    root = ET.parse(svg_path).getroot()
    if root.get("viewBox") != "0 0 64 64":
        sys.exit(f"{svg_path.name}: unexpected viewBox {root.get('viewBox')}")
    CLIPS.clear()
    for clip in root.iter(f"{SVG_NS}clipPath"):
        CLIPS[clip.get("id")] = clip.find(f"{SVG_NS}path").get("d")
    GRADIENTS.clear()
    XLINK = "{http://www.w3.org/1999/xlink}href"
    for grad in root.iter(f"{SVG_NS}linearGradient"):
        stops = grad.findall(f"{SVG_NS}stop")
        if stops:
            GRADIENTS[grad.get("id")] = stops[0].get("stop-color")
        else:
            # xlink:href inherits another gradient's stops (the star clusters)
            ref = (grad.get(XLINK) or grad.get("href") or "").lstrip("#")
            if ref not in GRADIENTS:
                sys.exit(f"gradient {grad.get('id')} references unknown {ref}")
            GRADIENTS[grad.get("id")] = GRADIENTS[ref]
    # Upstream typo in fill/drizzle.svg (v2.0.0): the third drop strokes url(#e)
    # but the file defines a/b/c/d — the drops are a/c/d, so e can only mean d.
    # Fixed by name, never by falling back silently.
    if svg_path.name == "drizzle.svg" and "e" not in GRADIENTS and "d" in GRADIENTS:
        GRADIENTS["e"] = GRADIENTS["d"]
    lines: list[str] = []
    for child in root:
        convert_shape(child, lines, "    ")
    body = "\n".join(lines)
    out_path.write_text(
        HEADER
        + '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        + '    android:width="24dp"\n    android:height="24dp"\n'
        + '    android:viewportWidth="64"\n    android:viewportHeight="64">\n'
        + body
        + "\n</vector>\n",
        encoding="utf-8",
    )


def main() -> None:
    if len(sys.argv) != 2:
        sys.exit(__doc__.split("\n\n")[1].strip())
    checkout = pathlib.Path(sys.argv[1])
    OUT.mkdir(parents=True, exist_ok=True)
    passes = [
        (SRC_SUBDIR, REMAP, False, "mc_"),
        (FILL_SRC_SUBDIR, FILL_REMAP, True, "mcf_"),
        (FILL_SRC_SUBDIR, FILL_NIGHT, True, "mcfn_"),
    ]
    total = 0
    for subdir, palette, drop_hairline, prefix in passes:
        src = checkout / subdir
        if not src.is_dir():
            sys.exit(f"not a meteocons v2.0.0 checkout: {src} missing")
        ACTIVE.update(palette=palette, drop_hairline=drop_hairline, prefix=prefix)
        for svg_name, drawable in sorted(ICONS.items()):
            svg = src / f"{svg_name}.svg"
            if not svg.is_file():
                sys.exit(f"missing upstream icon: {svg}")
            out_name = drawable.replace("mc_", prefix, 1)
            convert(svg, OUT / f"{out_name}.xml")
            total += 1
    print(f"{total} drawables written to {OUT}")


if __name__ == "__main__":
    main()
