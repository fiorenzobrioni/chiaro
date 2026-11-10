#!/usr/bin/env python3
"""Writes core/domain/src/main/resources/.../sky/constellations.bin: the lines of the
eighty-eight constellations and the point where each one's name is written (Fase 16, step
16.5, 10 nov 2026). `Constellations.kt` reads it back.

    python tools/gen_constellations.py [lines.json names.json bsc5.dat.gz]

Without paths the three sources are downloaded next to this script. The generated file is
the drawing: change this script and run it again, never the binary.

**The source, and why this one.** d3-celestial by Olaf Frohn (https://github.com/ofrohn/d3-celestial,
BSD 3-Clause, read on 10 Nov 2026 at the commit pinned below): `data/constellations.lines.json`,
the lines of the IAU's charts «with some line modifications» of its author, as J2000
coordinates, and `data/constellations.json`, the point each name is written at and a rank
of one to three. BSD 3-Clause goes into a GPL-3 app with nothing to decide: the notice is
kept in `licenses/d3-celestial-BSD-3-Clause.txt` and the credit is a row of Settings.
The other candidate was Stellarium's «modern» sky culture (`stellarium-skycultures`,
`western/`), read the same day: its licence line is «Text and data: CC BY-SA», with no
version, and only version 4.0 of that licence may be taken into GPL-3; and its lines are
Hipparcos numbers, which need a second catalogue to become places.

**Only the lines and the name points are taken.** The names are the app's own strings
(d3-celestial has «Big Dipper» for Ursa Major and «Cavallo» for Equuleus), and no star's
place is: every end of a line is moved onto the star of the Yale catalogue it stands on,
the brightest within SNAP_DEG that `bright_stars.bin` holds, and written in that file's
own fields. A line therefore ends on a point the canvas draws, to the last bit. All 767
vertices of the source fall within 0.003 degrees of a Yale star; two of them sit on the
faint half of a double (kappa Bootis, iota Cancri) and move 0.004 and 0.008 degrees to
the half the eye sees. The script stops if a vertex finds no star.

Layout, big-endian: "CHCN", a uint16 version (1), a uint16 count (88), then per
constellation, in the source's order (alphabetical by Latin name): three ASCII bytes (the IAU abbreviation), a uint8 rank (1 the ones a reader looks for
first, 3 the faint ones), a uint8 count of name points and that many points, a uint16
count of segments and that many pairs of points. A point is `bright_stars.bin`'s: right
ascension as uint16 over 0..360 degrees, declination as int16 over -90..90. The Serpent is
one constellation in two pieces, the head and the tail, with a name point on each.
"""
import json
import math
import os
import struct
import sys
import urllib.request

from gen_bright_stars import URL as BSC_URL
from gen_bright_stars import dec_field, load, parse, ra_field

COMMIT = "7e720a3de062059d4c5400a379146a601d9010e0"
BASE = "https://raw.githubusercontent.com/ofrohn/d3-celestial/" + COMMIT + "/data/"
LINES = "constellations.lines.json"
NAMES = "constellations.json"

# How far a vertex may be from the star it is moved onto: a hundredth of a degree, just
# over the widest pair one of the source's vertices is moved across (iota Cancri, 0.008),
# and a tenth of what one dp holds on a sky a whole screen wide.
SNAP_DEG = 0.01

HERE = os.path.dirname(__file__)
OUT = os.path.join(HERE, "..", "core", "domain", "src", "main", "resources",
                   "com", "callbackdev", "chiaro", "domain", "sky", "constellations.bin")


def fetch(name, path):
    if not os.path.exists(path):
        print("downloading", BASE + name)
        urllib.request.urlretrieve(BASE + name, path)
    with open(path, encoding="utf-8") as f:
        return json.load(f)["features"]


def separation(ra1, dec1, ra2, dec2):
    """Degrees between two places, by the haversine: exact at the hundredths this asks."""
    r1, d1, r2, d2 = map(math.radians, (ra1, dec1, ra2, dec2))
    h = math.sin((d2 - d1) / 2) ** 2 + math.cos(d1) * math.cos(d2) * math.sin((r2 - r1) / 2) ** 2
    return math.degrees(2 * math.asin(min(1.0, math.sqrt(h))))


def snap(lon, lat, stars, where):
    """The catalogue star a vertex stands on, as the two fields of `bright_stars.bin`."""
    ra = lon % 360.0  # GeoJSON longitude, -180..180, back to right ascension
    near = [s for s in stars if abs(s[1] - lat) < SNAP_DEG and separation(ra, lat, s[0], s[1]) < SNAP_DEG]
    if not near:
        sys.exit("%s: no catalogue star within %s degrees of %s, %s" % (where, SNAP_DEG, lon, lat))
    star = min(near, key=lambda s: s[2])  # the brightest: the one the eye sees
    return ra_field(star[0]), dec_field(star[1])


def build(lines, names, stars):
    """One entry per constellation, in the source's order: (id, rank, name points, segments)."""
    order = []
    table = {}
    for feature in names:
        cid = feature["id"]
        if cid not in table:
            order.append(cid)
            table[cid] = {"rank": int(feature["properties"]["rank"]), "labels": [], "segments": []}
        lon, lat = feature["geometry"]["coordinates"]
        table[cid]["labels"].append((ra_field(lon % 360.0), dec_field(lat)))
    for feature in lines:
        entry = table[feature["id"]]
        for line in feature["geometry"]["coordinates"]:
            points = [snap(lon, lat, stars, feature["id"]) for lon, lat in line]
            for a, b in zip(points, points[1:]):
                # A stroke between the two halves of a double is no line, and a stroke the
                # source draws twice is one line.
                if a == b or (a, b) in entry["segments"] or (b, a) in entry["segments"]:
                    continue
                entry["segments"].append((a, b))
    for cid in order:
        if not table[cid]["segments"]:
            sys.exit(cid + ": no lines in the source")
    return [(cid, table[cid]["rank"], table[cid]["labels"], table[cid]["segments"]) for cid in order]


def main():
    if len(sys.argv) > 3:
        lines_path, names_path, bsc_path = sys.argv[1:4]
    else:
        lines_path, names_path = os.path.join(HERE, LINES), os.path.join(HERE, NAMES)
        bsc_path = os.path.join(HERE, "bsc5.dat.gz")
    if not os.path.exists(bsc_path):
        print("the Yale catalogue comes from", BSC_URL)
    stars = parse(load(bsc_path))
    entries = build(fetch(LINES, lines_path), fetch(NAMES, names_path), stars)
    out = os.path.normpath(OUT)
    os.makedirs(os.path.dirname(out), exist_ok=True)
    with open(out, "wb") as f:
        f.write(b"CHCN")
        f.write(struct.pack(">HH", 1, len(entries)))
        for cid, rank, labels, segments in entries:
            f.write(cid.encode("ascii"))
            f.write(struct.pack(">BB", rank, len(labels)))
            for ra, dec in labels:
                f.write(struct.pack(">Hh", ra, dec))
            f.write(struct.pack(">H", len(segments)))
            for (ra1, dec1), (ra2, dec2) in segments:
                f.write(struct.pack(">HhHh", ra1, dec1, ra2, dec2))
    print("wrote", out, len(entries), "constellations,",
          sum(len(e[3]) for e in entries), "segments,", os.path.getsize(out), "bytes")


if __name__ == "__main__":
    main()
