#!/usr/bin/env python3
"""Writes core/domain/src/main/resources/.../sky/bright_stars.bin from the Yale Bright Star
Catalogue, 5th edition (Hoffleit & Warren 1991, public domain,
http://tdc-www.harvard.edu/catalogs/bsc5.html): every star brighter than the limit, with its
J2000 right ascension and declination, its visual magnitude and its B-V colour, brightest first.
The canvas draws them where they stand (DESIGN §3.9, step 6 of the sky review, 19 ott 2026).

    python tools/gen_bright_stars.py [path/to/bsc5.dat.gz]

Without a path the catalogue is downloaded next to this script. The generated file is the
drawing: change this script and run it again, never the binary. A binary and not Kotlin
(20 ott 2026): at magnitude 6.0 the catalogue is 5 080 stars, and a Kotlin array of that many
literals is over the 64 KB a JVM static initialiser may hold.

Layout, big-endian: "CHST", a uint16 version (1), a uint16 count, then per star four 16-bit
fields: right ascension as uint16 over 0..360° (0.0055° a step), declination as int16 over
-90..90° (0.0027° a step), magnitude × 1000 as int16, B-V × 1000 as int16 (0 where the
catalogue has none). `BrightStars.kt` reads it back.
"""
import gzip
import os
import struct
import sys
import urllib.request

LIMIT_MAG = 6.0
URL = "http://tdc-www.harvard.edu/catalogs/bsc5.dat.gz"
OUT = os.path.join(os.path.dirname(__file__), "..", "core", "domain", "src", "main", "resources",
                   "com", "callbackdev", "chiaro", "domain", "sky", "bright_stars.bin")


def load(path):
    if not os.path.exists(path):
        print("downloading", URL)
        urllib.request.urlretrieve(URL, path)
    with gzip.open(path, "rb") as f:
        return f.read().decode("latin-1").splitlines()


def parse(lines):
    """The fixed columns of the catalogue's ReadMe (1-based): RA J2000 at 76-83 (h, m, s.s),
    Dec J2000 at 84-90 (sign, deg, arcmin, arcsec), Vmag at 103-107, B-V at 110-114."""
    stars = []
    for line in lines:
        ra, dec, vmag, bv = line[75:83], line[83:90], line[102:107].strip(), line[109:114].strip()
        if not ra.strip() or not vmag:
            continue  # a nova or a duplicate with no position
        mag = float(vmag)
        if mag > LIMIT_MAG:
            continue
        h, m, s = int(ra[0:2]), int(ra[2:4]), float(ra[4:8])
        ra_deg = 15.0 * (h + m / 60.0 + s / 3600.0)
        sign = -1.0 if dec[0] == "-" else 1.0
        d, dm, ds = int(dec[1:3]), int(dec[3:5]), int(dec[5:7])
        dec_deg = sign * (d + dm / 60.0 + ds / 3600.0)
        colour = float(bv) if bv else 0.0
        stars.append((ra_deg, dec_deg, mag, colour))
    stars.sort(key=lambda s: s[2])
    return stars


def main():
    path = sys.argv[1] if len(sys.argv) > 1 else os.path.join(os.path.dirname(__file__), "bsc5.dat.gz")
    stars = parse(load(path))
    out = os.path.normpath(OUT)
    os.makedirs(os.path.dirname(out), exist_ok=True)
    with open(out, "wb") as f:
        f.write(b"CHST")
        f.write(struct.pack(">HH", 1, len(stars)))
        for ra, dec, mag, bv in stars:
            f.write(struct.pack(
                ">Hhhh",
                min(65535, int(round(ra / 360.0 * 65535.0))),
                int(round(dec / 90.0 * 32767.0)),
                int(round(mag * 1000.0)),
                int(round(bv * 1000.0)),
            ))
    print("wrote", out, len(stars), "stars; brightest", stars[0], "; limit", LIMIT_MAG)


if __name__ == "__main__":
    main()
