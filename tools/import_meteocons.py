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
# 0.0067). Measured with the same arithmetic PaletteContrastTest uses; worst case after
# the move is 3.04:1, and IconContrastTest holds the floor.
REMAP = {
    "#f59e0b": "#C27D08",  # sun amber            3.21 / 5.50
    "#fcd34d": "#C27D08",  # star yellow -> the one amber; two ambers said nothing
    "#fde68a": "#C27D08",  # dust yellow -> same
    "#72b9d5": "#3589AC",  # moon blue            3.70 / 4.76
    "#72b8d4": "#3589AC",  # snow blue -> unified with the moon; they were 1 bit apart
    "#e5e7eb": "#7D879B",  # cloud stroke         3.44 / 5.13
    "#9ca3af": "#656E7E",  # back cloud           4.89 / 3.60
    "#d1d5db": "#707D8F",  # fog bands            3.98 / 4.43
    "#374151": "#596A83",  # needles, arrows      5.24 / 3.36
    "#2885c7": "#2885C7",  # rain blue, already in band (3.79 / 4.65)
    "#ef4444": "#EF4444",  # thermometer red, already in band (3.58 / 4.92)
}

SVG_NS = "{http://www.w3.org/2000/svg}"

LINECAP = {"butt": "butt", "round": "round", "square": "square"}
LINEJOIN = {"miter": "miter", "round": "round", "bevel": "bevel"}

HEADER = (
    "<!-- Generated by tools/import_meteocons.py from Meteocons v2.0.0\n"
    "     (github.com/basmilius/meteocons), MIT, (c) 2020-2021 Bas Milius —\n"
    "     licenses/Meteocons-MIT.txt. Recolored for contrast on both surfaces;\n"
    "     the table and its measurements are in the tool. Do not edit by hand. -->\n"
)


def remap(color: str) -> str:
    mapped = REMAP.get(color.lower())
    if mapped is None:
        sys.exit(f"unmapped color {color}: add it to REMAP with a measured value")
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


def stroke_attrs(el: ET.Element) -> str:
    """The paint of one SVG shape, as vector-drawable attributes."""
    a = []
    fill = el.get("fill", "")
    if fill and fill != "none":
        a.append(f'android:fillColor="{remap(fill)}"')
    stroke = el.get("stroke")
    if stroke and stroke != "none":
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
    src = pathlib.Path(sys.argv[1]) / SRC_SUBDIR
    if not src.is_dir():
        sys.exit(f"not a meteocons v2.0.0 checkout: {src} missing")
    OUT.mkdir(parents=True, exist_ok=True)
    for svg_name, drawable in sorted(ICONS.items()):
        svg = src / f"{svg_name}.svg"
        if not svg.is_file():
            sys.exit(f"missing upstream icon: {svg}")
        convert(svg, OUT / f"{drawable}.xml")
    print(f"{len(ICONS)} drawables written to {OUT}")


if __name__ == "__main__":
    main()
