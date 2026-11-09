#!/usr/bin/env python3
"""Draws Chiaro's mark: the launcher icon's three layers and the status-bar icon.

    python3 tools/draw_launcher_icon.py

The mark is THE BLUE HOUR (committente, 9 Nov 2026, DESIGN §3.8): a sky on the whole badge, the
sun half under the horizon with three rays up, the first star at the upper right and, under the
horizon, the night's dark ground. Day and night in one picture, three shapes and a ground: it
replaced the day in a ring (23 set 2026), when the family's marks were redrawn as one series.

The series' rules, the same in every app of the family:
- the badge's ground is the app's own colour, from its light tone at the upper left to its deep
  tone at the lower right; the symbol is white with one amber accent (here the sun);
- every shape is lit from the upper left: a linear ramp from its light tone at the upper left of
  its box to a darker one at the lower right (the amber to its base, the white to its deep tone,
  which is still nearly white), over the box's larger side; no shadow (the store and the
  launchers add their own);
- the symbol stays inside the keyline, a circle of KEYLINE around the centre: 56 dp of the 72
  that show, so a round mask leaves 8 dp of air all round and no mask of any launcher reaches
  it. Only the ground touches the edge: here the night under the horizon, which is ground, so it
  lives in the background layer and does not move with the symbol in a launcher's parallax.

The status-bar icon is the same picture in one ink on the 24-unit grid (the system tints it),
the horizon a bar, inside the 2 units of air Android's system icons keep.

Re-running this script IS the drawing: ic_launcher_background.xml, ic_launcher_foreground.xml,
ic_launcher_monochrome.xml and ic_stat_chiaro.xml are its output and are not edited by hand.
Only the standard library is used, so any Python 3 runs it.
"""

import math
from pathlib import Path

RES = Path(__file__).resolve().parent.parent / "app/src/main/res/drawable"

CENTRE = 54.0
KEYLINE = 28.0  # the symbol's reach from the centre: measured 27.7

# The family's palette, the tones this mark uses: (light, base, deep).
SKY = ("62AEFF", "2F7FE6", "1B5CC8")
NIGHT = ("2A55B8", "17398A", "0E2462")
SUN = ("FFD36A", "FDB528", "F28C17")
WHITE = ("FFFFFF", "F4F7FB", "E3EAF4")

HORIZON = 65.0
SUN_RADIUS = 12.5
RAY_FROM, RAY_TO, RAY_WIDTH = 16.6, 21.4, 3.6  # each ray a capsule, its ends round
RAYS = (-135.0, -90.0, -45.0)  # degrees clockwise from three o'clock: up left, up, up right
STAR = (68.5, 36.0, 5.6)  # middle and reach of a four-pointed star
STAR_WAIST = 0.32
GROUND_GAP = 2.6  # the band of sky between the sun and the ground
THEMED_BAR = (30.0, 78.0, 4.6)  # the themed icon's horizon: from, to, thickness

# The status-bar icon: the drawing above, a little heavier, fitted to the 24-unit grid.
STAT_SCALE = 0.38
STAT_LIFT = 4.5  # the drawing's middle is this far above the centre: it is moved down by it
STAT_SUN_GROW, STAT_RAY = 1.5, (18.2, 24.0, 5.4)
STAT_BAR = (28.0, 80.0, HORIZON + 3.2, 6.0)  # from, to, top, thickness
STAT_STAR = (70.5, 33.0, 8.0, 0.36)

HEADER = """<?xml version="1.0" encoding="utf-8"?>
<!-- Written by tools/draw_launcher_icon.py: change the script and run it, never this file.
     {what} -->
"""


def f(v):
    return f"{v:.2f}"


def lit(box):
    """A shape's light: from the upper-left corner of its box to the lower-right one, along the
    diagonal of a square on the box's larger side, so every shape is lit at the same angle."""
    x0, y0, x1, y1 = box
    cx, cy, r = (x0 + x1) / 2, (y0 + y1) / 2, max(x1 - x0, y1 - y0) / 2
    return (cx - r, cy - r), (cx + r, cy + r)


def ramp(attr, box, light, deep, indent="    "):
    (sx, sy), (ex, ey) = lit(box)
    return (
        f'{indent}<aapt:attr name="android:{attr}">\n'
        f'{indent}    <gradient android:type="linear" android:startX="{f(sx)}" android:startY="{f(sy)}" '
        f'android:endX="{f(ex)}" android:endY="{f(ey)}">\n'
        f'{indent}        <item android:offset="0" android:color="#FF{light}"/>\n'
        f'{indent}        <item android:offset="1" android:color="#FF{deep}"/>\n'
        f"{indent}    </gradient>\n{indent}</aapt:attr>\n"
    )


def shaded(d, box, light, deep):
    return f'<path android:pathData="{d}">\n{ramp("fillColor", box, light, deep)}</path>\n'


def ink(d):
    return f'<path android:pathData="{d}" android:fillColor="#FFFFFFFF"/>\n'


def vector(body, size=108, aapt=True):
    ns = '\n    xmlns:aapt="http://schemas.android.com/aapt"' if aapt else ""
    return (
        f'<vector xmlns:android="http://schemas.android.com/apk/res/android"{ns}\n'
        f'    android:width="{size}dp"\n    android:height="{size}dp"\n'
        f'    android:viewportWidth="{size}"\n    android:viewportHeight="{size}">\n'
        f"{body}</vector>\n"
    )


def capsule(a, b, w):
    """A segment with round ends, walked counter-clockwise (its arcs' sweep flag 0)."""
    r = w / 2
    dx, dy = b[0] - a[0], b[1] - a[1]
    length = math.hypot(dx, dy)
    ux, uy = dx / length, dy / length
    nx, ny = -uy * r, ux * r
    p1, p2, p3, p4 = (a[0] + nx, a[1] + ny), (b[0] + nx, b[1] + ny), (b[0] - nx, b[1] - ny), (a[0] - nx, a[1] - ny)
    tips = (b[0] + ux * r, b[1] + uy * r), (a[0] - ux * r, a[1] - uy * r)
    poly = [p1, p2, tips[0], p3, p4, tips[1]]
    if sum(p[0] * q[1] - q[0] * p[1] for p, q in zip(poly, poly[1:] + poly[:1])) > 0:
        p1, p2, p3, p4 = p4, p3, p2, p1  # the other way round
    return (
        f"M {f(p1[0])},{f(p1[1])} L {f(p2[0])},{f(p2[1])} A {f(r)},{f(r)} 0 0,0 {f(p3[0])},{f(p3[1])} "
        f"L {f(p4[0])},{f(p4[1])} A {f(r)},{f(r)} 0 0,0 {f(p1[0])},{f(p1[1])} Z"
    )


def rounded_bar(x0, x1, top, thickness):
    r = thickness / 2
    return (
        f"M {f(x0 + r)},{f(top)} A {f(r)},{f(r)} 0 0,0 {f(x0 + r)},{f(top + thickness)} "
        f"L {f(x1 - r)},{f(top + thickness)} A {f(r)},{f(r)} 0 0,0 {f(x1 - r)},{f(top)} Z"
    )


def half_sun(r):
    return f"M {f(CENTRE - r)},{f(HORIZON)} A {f(r)},{f(r)} 0 0,1 {f(CENTRE + r)},{f(HORIZON)} Z"


def rays(r_from, r_to, width):
    out = []
    for deg in RAYS:
        a = math.radians(deg)
        ux, uy = math.cos(a), math.sin(a)
        out.append(capsule((CENTRE + ux * r_from, HORIZON + uy * r_from), (CENTRE + ux * r_to, HORIZON + uy * r_to), width))
    return " ".join(out)


def star(cx, cy, r, waist=STAR_WAIST):
    """A four-pointed star: its points joined by curves drawn in towards the middle."""
    pts = [(cx + r * math.cos(math.radians(i * 90 - 90)), cy + r * math.sin(math.radians(i * 90 - 90))) for i in range(4)]
    d = f"M {f(pts[0][0])},{f(pts[0][1])} "
    for i in range(4):
        p, q = pts[i], pts[(i + 1) % 4]
        c = (cx + (p[0] + q[0] - 2 * cx) * waist, cy + (p[1] + q[1] - 2 * cy) * waist)
        d += f"Q {f(c[0])},{f(c[1])} {f(q[0])},{f(q[1])} "
    return d + "Z"


def box_around(cx, cy, r):
    return (cx - r, cy - r, cx + r, cy + r)


def background():
    ground_top = HORIZON + GROUND_GAP
    body = shaded("M 0,0 H 108 V 108 H 0 Z", (18, 18, 90, 90), SKY[0], SKY[2]) + (
        f'<path android:pathData="M 0,{f(ground_top)} H 108 V 108 H 0 Z">\n'
        f'{ramp("fillColor", (0, ground_top, 108, 108), NIGHT[1], NIGHT[2])}</path>\n'
    )
    what = (
        "The badge's ground: the sky, lit from the upper left, and the night's ground under the\n"
        f"     horizon from {ground_top:g} down, which runs to the edge as ground does."
    )
    return HEADER.format(what=what) + vector(body)


def foreground():
    body = (
        shaded(half_sun(SUN_RADIUS), box_around(CENTRE, HORIZON, SUN_RADIUS), SUN[0], SUN[1])
        + shaded(rays(RAY_FROM, RAY_TO, RAY_WIDTH), box_around(CENTRE, HORIZON, RAY_TO), SUN[0], SUN[1])
        + shaded(star(*STAR), box_around(*STAR), WHITE[0], WHITE[2])
    )
    what = (
        "The mark: the blue hour. The sun half under the horizon, three rays up, the first star\n"
        f"     at the upper right; every shape inside {KEYLINE:g} units of the centre, the keyline."
    )
    return HEADER.format(what=what) + vector(body)


def monochrome():
    x0, x1, thickness = THEMED_BAR
    body = (
        ink(half_sun(SUN_RADIUS))
        + ink(rays(RAY_FROM, RAY_TO, RAY_WIDTH))
        + ink(rounded_bar(x0, x1, HORIZON + GROUND_GAP, thickness))
        + ink(star(*STAR))
    )
    what = (
        "The themed icon (Android 13+): the system reads only the alpha, so the ground under the\n"
        "     horizon becomes a bar inside the keyline, and the rest is the mark's shapes."
    )
    return HEADER.format(what=what) + vector(body, aapt=False)


def status_bar():
    s = STAT_SCALE
    x0, x1, top, thickness = STAT_BAR
    sx, sy, sr, sw = STAT_STAR
    body = (
        f'<group android:pivotX="{f(CENTRE)}" android:pivotY="{f(CENTRE)}" android:scaleX="{s:g}" android:scaleY="{s:g}" '
        f'android:translateX="{f(12 - CENTRE)}" android:translateY="{f(12 - CENTRE + STAT_LIFT * s)}">\n'
        + ink(half_sun(SUN_RADIUS + STAT_SUN_GROW))
        + ink(rays(*STAT_RAY))
        + ink(rounded_bar(x0, x1, top, thickness))
        + ink(star(sx, sy, sr, sw))
        + "</group>\n"
    )
    what = (
        "Status-bar icon: the system renders only the alpha channel. The launcher mark in one ink,\n"
        "     the horizon a bar, inside 2 units of air on the 24-unit grid."
    )
    return HEADER.format(what=what) + vector(body, size=24, aapt=False)


def main():
    for name, text in (
        ("ic_launcher_background.xml", background()),
        ("ic_launcher_foreground.xml", foreground()),
        ("ic_launcher_monochrome.xml", monochrome()),
        ("ic_stat_chiaro.xml", status_bar()),
    ):
        (RES / name).write_text(text)
        print("wrote", RES / name)


if __name__ == "__main__":
    main()
