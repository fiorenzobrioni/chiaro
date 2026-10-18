#!/usr/bin/env python3
"""Draws Chiaro's mark: the launcher icon's two layers and the status-bar icon.

    python3 tools/draw_launcher_icon.py

The mark is THE DAY IN A RING (committente, 23 set 2026, DESIGN §3.8): one day of twenty-four
hours painted the way the app paints the sky, a sweep that starts at three o'clock (18:00, the
dusk's amber) and runs clockwise through the night's indigo to midnight at the bottom, the
dawn's amber at nine o'clock and the noon blue at the top. The stops are the vivid sky table's
(SkyPalette, DESIGN §3.7) read at its anchors, so the badge and the canvas are one sky.

The sun sits ON the ring at mid afternoon and a clip opens the ring around it: the gap turns the
ring into a C, the name's initial, and keeps the sun a sun on its path rather than a bead on a
bracelet. Since 18 ott 2026 (committente, the icons' review) the sun is drawn flat, with rays:
a disc and eight short, full rays in one amber lit from the upper left, where it was a shaded
sphere. A disc alone read as a dot; with its rays it is the sun the app draws for a clear sky,
and it carries the ring's weight, which thin rays did not.

The status-bar icon is not the ring (same review): the system shows it at 24 dp in one ink, and
the notifications are about the weather, so it is the sun with a cloud in front of it. The sun
alone is the system's own brightness icon. The cloud is a flat bank low under the sun and covers
only its lower left, so the sun stays the larger shape.

Re-running this script IS the drawing: ic_launcher_foreground.xml, ic_launcher_monochrome.xml
and ic_stat_chiaro.xml are its output and are not edited by hand. Only the standard library is
used, so any Python 3 runs it.
"""

import math
from pathlib import Path

RES = Path(__file__).resolve().parent.parent / "app/src/main/res/drawable"

CENTRE = 54.0
RING_RADIUS = 21.0
RING_WIDTH = 10.0
SUN_ANGLE = -40.0  # degrees clockwise from three o'clock: mid afternoon, the upper right
SUN_CORE = 5.6
RAYS = 8
RAY_FROM, RAY_TO, RAY_WIDTH = 7.6, 9.4, 2.6  # each ray a capsule, its ends round
GAP = 2.6  # the clear band past the rays' tips, where the sun cuts the ring
# Its reach (rays and caps, 10.7) and the band end 34 units from the centre: the sun itself stays
# inside the 33-unit safe circle of every launcher mask, only the cut reaches past it.
CUT_RADIUS = RAY_TO + RAY_WIDTH / 2 + GAP

# The ring's sweep: (position, colour), from three o'clock clockwise. The vivid sky table's
# anchors: horizon amber, civil-twilight violet, night navy, midday azure.
RING_STOPS = [
    (0.0, "F08A1A"), (0.07, "6B4FC8"), (0.16, "1B2A78"), (0.25, "0D1646"), (0.34, "1B2A78"),
    (0.43, "6B4FC8"), (0.5, "F7A21B"), (0.57, "7CC6FF"), (0.66, "2BA8FF"), (0.75, "168FF0"),
    (0.84, "2BA8FF"), (0.93, "7CC6FF"), (1.0, "F08A1A"),
]
AMBER = ("FFC658", "EF8618")  # the emblem's amber, light at the upper left
AMBER_REACH = 7.5  # how far from the sun's centre the amber runs, along the diagonal

# The status-bar icon, drawn on a 24-unit grid and then fitted into it (STAT_SCALE about the
# drawing's own middle), so the rays' tips keep a unit of air from the edge.
STAT_SUN = (13.6, 10.0)
STAT_SUN_CORE, STAT_RAY_FROM, STAT_RAY_TO, STAT_RAY_WIDTH = 4.4, 6.5, 8.5, 2.1
# The cloud: a long low base and two shallow bumps, the right one the taller: a flat bank the
# sun stands on, rather than a heap in front of it (committente's choice).
STAT_CLOUD_BASE = (2.0, 16.6, 16.0, 22.0)  # left, top, right, bottom
STAT_CLOUD_BUMPS = [(6.4, 16.8, 2.8), (10.6, 15.8, 3.5)]
STAT_CUT = 1.4  # the clear band between the cloud and the sun behind it
STAT_SCALE = 0.92

HEADER = """<?xml version="1.0" encoding="utf-8"?>
<!-- Written by tools/draw_launcher_icon.py: change the script and run it, never this file.
     {what} -->
"""


def f(v):
    return f"{v:.2f}"


def point(deg, r=RING_RADIUS):
    a = math.radians(deg)
    return CENTRE + r * math.cos(a), CENTRE + r * math.sin(a)


def circle(cx, cy, r):
    """A circle walked counter-clockwise (sweep flag 0): filled on its own, a hole in a clip."""
    return f"M {f(cx - r)},{f(cy)} a {f(r)},{f(r)} 0 1,0 {f(2 * r)},0 a {f(r)},{f(r)} 0 1,0 {f(-2 * r)},0 z"


def capsule(a, b, width):
    """A segment with round ends, as two straight sides and two half circles."""
    r = width / 2
    dx, dy = b[0] - a[0], b[1] - a[1]
    length = math.hypot(dx, dy)
    nx, ny = -dy / length * r, dx / length * r
    return (
        f"M {f(a[0] + nx)},{f(a[1] + ny)} L {f(b[0] + nx)},{f(b[1] + ny)} "
        f"A {f(r)},{f(r)} 0 0,0 {f(b[0] - nx)},{f(b[1] - ny)} L {f(a[0] - nx)},{f(a[1] - ny)} "
        f"A {f(r)},{f(r)} 0 0,0 {f(a[0] + nx)},{f(a[1] + ny)} Z"
    )


def sun(cx, cy, core, ray_from, ray_to, ray_width):
    """The disc and its rays, straight up and round by eighths, as one path."""
    parts = [circle(cx, cy, core)]
    for i in range(RAYS):
        a = math.radians(i * 360 / RAYS - 90)
        ux, uy = math.cos(a), math.sin(a)
        parts.append(capsule((cx + ux * ray_from, cy + uy * ray_from), (cx + ux * ray_to, cy + uy * ray_to), ray_width))
    return " ".join(parts)


def ring_clip():
    cx, cy = point(SUN_ANGLE)
    return f'<clip-path android:pathData="M 0,0 H 108 V 108 H 0 Z {circle(cx, cy, CUT_RADIUS)}"/>'


def sun_path(fill):
    cx, cy = point(SUN_ANGLE)
    return f'<path android:pathData="{sun(cx, cy, SUN_CORE, RAY_FROM, RAY_TO, RAY_WIDTH)}"{fill}'


def vector(body, size=108, aapt=True):
    ns = '\n    xmlns:aapt="http://schemas.android.com/aapt"' if aapt else ""
    return (
        f'<vector xmlns:android="http://schemas.android.com/apk/res/android"{ns}\n'
        f'    android:width="{size}dp"\n    android:height="{size}dp"\n'
        f'    android:viewportWidth="{size}"\n    android:viewportHeight="{size}">\n'
        f"{body}</vector>\n"
    )


def foreground():
    cx, cy = point(SUN_ANGLE)
    stops = "\n".join(
        f'                <item android:offset="{t:g}" android:color="#FF{c}"/>' for t, c in RING_STOPS
    )
    amber = f""">
    <aapt:attr name="android:fillColor">
        <gradient android:type="linear" android:startX="{f(cx - AMBER_REACH)}" android:startY="{f(cy - AMBER_REACH)}" android:endX="{f(cx + AMBER_REACH)}" android:endY="{f(cy + AMBER_REACH)}">
            <item android:offset="0" android:color="#FF{AMBER[0]}"/>
            <item android:offset="1" android:color="#FF{AMBER[1]}"/>
        </gradient>
    </aapt:attr>
</path>
"""
    body = f"""<group>
    {ring_clip()}
    <path android:pathData="{circle(CENTRE, CENTRE, RING_RADIUS)}" android:strokeWidth="{f(RING_WIDTH)}" android:fillColor="#00000000">
        <aapt:attr name="android:strokeColor">
            <gradient android:type="sweep" android:centerX="{f(CENTRE)}" android:centerY="{f(CENTRE)}">
{stops}
            </gradient>
        </aapt:attr>
    </path>
</group>
{sun_path(amber)}"""
    what = (
        "The mark: the day in a ring, painted like the sky (dusk at three o'clock, midnight at\n"
        "     the bottom, dawn at nine, noon at the top), opened by a flat amber sun with eight rays\n"
        f"     at mid afternoon. Ring radius {RING_RADIUS:g}, stroke {RING_WIDTH:g}; sun {SUN_CORE:g} with rays to\n"
        f"     {RAY_TO + RAY_WIDTH / 2:g}, at {SUN_ANGLE:g} degrees; gap {GAP:g} past the rays. The sun stays inside\n"
        "     the 33-unit safe circle of every launcher mask."
    )
    return HEADER.format(what=what) + vector(body)


def monochrome():
    outer, inner = RING_RADIUS + RING_WIDTH / 2, RING_RADIUS - RING_WIDTH / 2
    body = f"""<group>
    {ring_clip()}
    <path android:pathData="{circle(CENTRE, CENTRE, outer)} {circle(CENTRE, CENTRE, inner)}" android:fillColor="#FFFFFFFF" android:fillType="evenOdd"/>
</group>
{sun_path(' android:fillColor="#FFFFFFFF"/>')}
"""
    what = (
        "The themed icon (Android 13+): the system reads only the alpha, so the mark is\n"
        "     restated as shapes, the ring as a filled annulus (evenOdd) with the same gap, and the sun."
    )
    return HEADER.format(what=what) + vector(body, aapt=False)


def stat_fit():
    """The scale about the drawing's middle that puts it in the middle of the 24-unit grid."""
    sx, sy = STAT_SUN
    reach = STAT_RAY_TO + STAT_RAY_WIDTH / 2
    left = min(STAT_CLOUD_BASE[0], sx - reach)
    right = max(STAT_CLOUD_BASE[2], sx + reach)
    top = min(sy - reach, *(cy - r for _, cy, r in STAT_CLOUD_BUMPS))
    bottom = STAT_CLOUD_BASE[3]
    mx, my = (left + right) / 2, (top + bottom) / 2

    def fit(x, y):
        return 12 + (x - mx) * STAT_SCALE, 12 + (y - my) * STAT_SCALE

    return fit


def rounded_bar(left, top, right, bottom):
    """A pill walked counter-clockwise, like circle(), so it is a hole in a clip as well."""
    r = (bottom - top) / 2
    return (
        f"M {f(left + r)},{f(top)} A {f(r)},{f(r)} 0 0,0 {f(left + r)},{f(bottom)} "
        f"L {f(right - r)},{f(bottom)} A {f(r)},{f(r)} 0 0,0 {f(right - r)},{f(top)} Z"
    )


def cloud_shapes(grow=0.0):
    """The cloud's three pieces on the grid, each grown by `grow` (for the band around it)."""
    fit = stat_fit()
    s = STAT_SCALE
    left, top, right, bottom = STAT_CLOUD_BASE
    (x0, y0), (x1, y1) = fit(left, top), fit(right, bottom)
    shapes = [rounded_bar(x0 - grow, y0 - grow, x1 + grow, y1 + grow)]
    for cx, cy, r in STAT_CLOUD_BUMPS:
        px, py = fit(cx, cy)
        shapes.append(circle(px, py, r * s + grow))
    return shapes


def status_bar():
    fit = stat_fit()
    s = STAT_SCALE
    sx, sy = fit(*STAT_SUN)
    cut = STAT_CUT * s
    # One hole per group: nested clips intersect, so the sun keeps what is outside all three
    # pieces of the grown cloud (in one clip, two overlapping holes would fill each other in).
    holes = [f'<group>\n    <clip-path android:pathData="M 0,0 H 24 V 24 H 0 Z {shape}"/>' for shape in cloud_shapes(cut)]
    sun_d = sun(sx, sy, STAT_SUN_CORE * s, STAT_RAY_FROM * s, STAT_RAY_TO * s, STAT_RAY_WIDTH * s)
    body = (
        "\n".join(holes)
        + f'\n<path android:pathData="{sun_d}" android:fillColor="#FFFFFFFF"/>\n'
        + "</group>\n" * len(holes)
        + "".join(f'<path android:pathData="{shape}" android:fillColor="#FFFFFFFF"/>\n' for shape in cloud_shapes())
    )
    what = (
        "Status-bar icon: the system renders only the alpha channel. The sun of the launcher\n"
        "     icon with a cloud low in front of it, which says weather where the sun alone is the\n"
        "     system's brightness icon; a band of air keeps the two apart."
    )
    return HEADER.format(what=what) + vector(body, size=24, aapt=False)


def main():
    for name, text in (
        ("ic_launcher_foreground.xml", foreground()),
        ("ic_launcher_monochrome.xml", monochrome()),
        ("ic_stat_chiaro.xml", status_bar()),
    ):
        (RES / name).write_text(text)
        print("wrote", RES / name)


if __name__ == "__main__":
    main()
