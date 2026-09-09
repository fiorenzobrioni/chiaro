#!/usr/bin/env python3
"""Builds the bundled index of the Protezione Civile warning zones.

The seam is `domain/warnings/WarningZoneIndex` (PLANNING.md, Fase 11): this tool fills
the data side of it, one JSON asset the app ships and reads offline, from two files of
the Dipartimento's own bulletin repository (CC BY 4.0):

- the **shapefile** of the bulletin (`files/shp/<stamp>_shp.zip`): its DBF carries the
  zone CODE (`Zona_all`, e.g. `Lomb-01`) next to the zone NAME (`Nome_zona`), and its
  SHP the geometry — this is the only file where code, name and polygon sit side by side;
- the **TopoJSON** of the same bulletin (`files/topojson/<stamp>_today.json`): the only
  file that lists the municipalities (`Comuni`) of each zone, joined on the zone name.

    B=https://raw.githubusercontent.com/pcm-dpc/DPC-Bollettini-Criticita-Idrogeologica-Idraulica/master/files
    curl -sSLo /tmp/dpc_shp.zip  $B/shp/20260908_1519_shp.zip
    curl -sSLo /tmp/dpc_topo.json $B/topojson/20260908_1519_today.json
    python tools/build_warning_zones.py /tmp/dpc_shp.zip /tmp/dpc_topo.json

The zones change rarely (a Region redraws its zones once in years); the stamp used is
written into the asset so the next rebuild can say what it replaced.

What the tool decides, each the kind of thing that must be written down:

1. **Geometry first, names second.** The index answers "which zone is this point in"
   by point-in-polygon, and only falls back on the municipality name — the files carry
   cp1252 damage in a few names and a hamlet is not a comune, while a coordinate is a
   coordinate. The polygons are therefore the payload, and everything below is about
   making 137 195 vertices fit in an asset a phone can hold in memory.
2. **Simplification is Douglas-Peucker per ring, metric, with the tolerance recorded
   in the asset.** Rings are simplified independently, so two neighbouring zones can
   disagree by up to the tolerance along their shared border; the index knows the
   tolerance and treats a point within it of a border as "on the border" (where the
   comune decides), rather than pretending the simplified line is the real one.
3. **Coordinates are integers at 10^-4 degrees (11 m of latitude, 8 m of longitude
   at 45°N), delta-encoded along each ring.** A phone's decimal JSON parser reads them
   back exactly; the domain undoes the deltas once at load. Even-odd containment over
   all of a zone's rings makes ring orientation irrelevant, so holes need no marking.
4. **The region is derived from the code's prefix.** The DBF has no region column; the
   twenty prefixes (`Abru`, `Basi`, … `Vene`) are the Regions' own initials and the
   table below is the whole mapping. A prefix this table does not know stops the build.
5. **Names are repaired only where the damage has one possible reading.** The
   Valle d'Aosta zone names carry a `?` where an apostrophe was lost to a codepage
   (`Valle d?Aosta`): a `?` between two letters in a place name can only have been an
   apostrophe, and is restored. A U+FFFD in a municipality name is NOT repaired — the
   lost character could be anything — the name is dropped from the fallback list and
   counted, so the debt is re-measured every time the asset is rebuilt.
6. **Municipality names are normalized once, here, and the same way in the domain**
   (`WarningZoneIndex.normalizeComune`): NFD, combining marks dropped, lower case,
   curly apostrophes straightened, whitespace collapsed; bilingual names
   (`Bolzano/Bozen`) index under each half. The lookup side additionally drops a
   leading "Comune di", which is how Open-Meteo's `admin3` spells a municipality.

Requires no third-party packages, runs from the repo root.
"""
from __future__ import annotations

import argparse
import io
import json
import math
import pathlib
import re
import struct
import sys
import unicodedata
import zipfile

OUT = pathlib.Path("core/data/src/main/assets/warning_zones_it.json")

SOURCE_REPO = "pcm-dpc/DPC-Bollettini-Criticita-Idrogeologica-Idraulica"
LICENSE = "CC BY 4.0"

# Coordinates are stored as integers at this many decimal places.
PRECISION = 4
QUANTUM = 10 ** PRECISION

# Douglas-Peucker tolerance in metres, measured on the 20260908_1519 bulletin (asset
# sizes include the 8 295 municipality keys, ~130 KB of the total):
#   100 m -> 73 067 points, 630 KB;   250 m -> 34 260 points, 389 KB;
#   500 m -> 18 723 points, 284 KB;  1000 m -> 10 130 points, 219 KB.
# 500 m: the budget is 400 KB and 250 m spends all of it; the position path already
# rounds a fix onto a ~1.1 km grid (`City.cacheKey`, up to 680 m of displacement) and a
# geocoded town sits kilometres from its zone border, so nothing the app locates is
# sharper than half a kilometre, and the border band the index hands to the municipality
# fallback is 500 m wide either way.
DEFAULT_TOLERANCE_M = 500.0

# Metres per degree, used only to make the tolerance metric. Latitude is constant;
# longitude shrinks with cos(lat) and 42°N is the middle of the peninsula.
M_PER_DEG_LAT = 111_320.0
M_PER_DEG_LON = 111_320.0 * math.cos(math.radians(42.0))

# The zone code prefix is the Region's initials, the Dipartimento's own convention.
REGION_BY_PREFIX = {
    "Abru": "Abruzzo",
    "Basi": "Basilicata",
    "Cala": "Calabria",
    "Camp": "Campania",
    "Emil": "Emilia-Romagna",
    "Friu": "Friuli-Venezia Giulia",
    "Lazi": "Lazio",
    "Ligu": "Liguria",
    "Lomb": "Lombardia",
    "Marc": "Marche",
    "Moli": "Molise",
    "Piem": "Piemonte",
    "Pugl": "Puglia",
    "Sard": "Sardegna",
    "Sici": "Sicilia",
    "Tosc": "Toscana",
    "Tren": "Trentino-Alto Adige",
    "Umbr": "Umbria",
    "VDAo": "Valle d'Aosta",
    "Vene": "Veneto",
}


# --- the shapefile ------------------------------------------------------------------


def read_dbf(data: bytes) -> list[dict[str, str]]:
    """dBASE III records as dicts of stripped strings (the DBF is cp1252, LDID 87)."""
    count = struct.unpack("<I", data[4:8])[0]
    header_len = struct.unpack("<H", data[8:10])[0]
    record_len = struct.unpack("<H", data[10:12])[0]
    fields: list[tuple[str, int]] = []
    pos = 32
    while data[pos] != 0x0D:
        name = data[pos:pos + 11].split(b"\0")[0].decode("ascii")
        fields.append((name, data[pos + 16]))
        pos += 32
    records = []
    for i in range(count):
        raw = data[header_len + i * record_len:header_len + (i + 1) * record_len]
        offset = 1  # deletion flag
        record = {}
        for name, length in fields:
            record[name] = raw[offset:offset + length].decode("cp1252", "replace").strip()
            offset += length
        records.append(record)
    return records


def read_shp_polygons(data: bytes) -> list[list[list[tuple[float, float]]]]:
    """Every record's rings, as (lon, lat) lists WITHOUT the repeated closing point.

    Shape type 5 (Polygon) only; a null shape yields no rings. Record order is the
    DBF's, which is the join.
    """
    shape_type = struct.unpack("<i", data[32:36])[0]
    if shape_type != 5:
        sys.exit(f"expected a Polygon shapefile (type 5), got type {shape_type}")
    shapes = []
    pos = 100
    while pos < len(data):
        _, content_len = struct.unpack(">ii", data[pos:pos + 8])
        pos += 8
        body = data[pos:pos + content_len * 2]
        pos += content_len * 2
        if struct.unpack("<i", body[:4])[0] == 0:
            shapes.append([])
            continue
        num_parts, num_points = struct.unpack("<ii", body[36:44])
        parts = struct.unpack(f"<{num_parts}i", body[44:44 + 4 * num_parts])
        points_at = 44 + 4 * num_parts
        points = struct.unpack(f"<{2 * num_points}d", body[points_at:points_at + 16 * num_points])
        rings = []
        for k, start in enumerate(parts):
            end = parts[k + 1] if k + 1 < num_parts else num_points
            ring = [(points[2 * i], points[2 * i + 1]) for i in range(start, end)]
            if len(ring) > 1 and ring[0] == ring[-1]:
                ring.pop()
            rings.append(ring)
        shapes.append(rings)
    return shapes


# --- simplification -----------------------------------------------------------------


def _to_metres(ring: list[tuple[float, float]]) -> list[tuple[float, float]]:
    return [(lon * M_PER_DEG_LON, lat * M_PER_DEG_LAT) for lon, lat in ring]


def _segment_distance(p, a, b) -> float:
    (px, py), (ax, ay), (bx, by) = p, a, b
    dx, dy = bx - ax, by - ay
    if dx == 0 and dy == 0:
        return math.hypot(px - ax, py - ay)
    t = max(0.0, min(1.0, ((px - ax) * dx + (py - ay) * dy) / (dx * dx + dy * dy)))
    return math.hypot(px - (ax + t * dx), py - (ay + t * dy))


def douglas_peucker(points: list[tuple[float, float]], tolerance: float) -> list[int]:
    """Indices of the points kept, for an OPEN polyline, iteratively (rings run to
    4 459 vertices and recursion is the wrong tool for that)."""
    keep = [False] * len(points)
    keep[0] = keep[-1] = True
    stack = [(0, len(points) - 1)]
    while stack:
        first, last = stack.pop()
        if last - first < 2:
            continue
        a, b = points[first], points[last]
        best, best_i = 0.0, -1
        for i in range(first + 1, last):
            d = _segment_distance(points[i], a, b)
            if d > best:
                best, best_i = d, i
        if best > tolerance:
            keep[best_i] = True
            stack.append((first, best_i))
            stack.append((best_i, last))
    return [i for i, k in enumerate(keep) if k]


def simplify_ring(ring: list[tuple[float, float]], tolerance_m: float) -> list[tuple[float, float]]:
    """A closed ring simplified as two open halves hinged on its two farthest points,
    so the anchors are not the arbitrary first vertex. Rings that collapse below a
    triangle are dropped by the caller."""
    if len(ring) < 4:
        return ring
    metric = _to_metres(ring)
    # The anchor pair: the first point and the one farthest from it.
    far = max(range(len(metric)), key=lambda i: math.dist(metric[0], metric[i]))
    first_half = metric[: far + 1]
    second_half = metric[far:] + [metric[0]]
    kept = douglas_peucker(first_half, tolerance_m)
    kept_second = douglas_peucker(second_half, tolerance_m)
    indices = kept + [(far + i) % len(ring) for i in kept_second[1:-1]]
    return [ring[i] for i in indices]


def quantize(ring: list[tuple[float, float]]) -> list[tuple[int, int]]:
    out: list[tuple[int, int]] = []
    for lon, lat in ring:
        q = (round(lon * QUANTUM), round(lat * QUANTUM))
        if not out or out[-1] != q:
            out.append(q)
    if len(out) > 1 and out[0] == out[-1]:
        out.pop()
    return out


def delta_encode(ring: list[tuple[int, int]]) -> list[int]:
    flat: list[int] = []
    px, py = 0, 0
    for x, y in ring:
        flat.extend((x - px, y - py))
        px, py = x, y
    return flat


# --- names --------------------------------------------------------------------------


def repair_zone_name(name: str) -> str:
    # A `?` between two letters in a place name can only be a lost apostrophe.
    return re.sub(r"(?<=[A-Za-z])\?(?=[A-Za-z])", "'", name)


def normalize_comune(name: str) -> str:
    """Mirror of `WarningZoneIndex.normalizeComune` in :core:domain — change both."""
    decomposed = unicodedata.normalize("NFD", name)
    stripped = "".join(ch for ch in decomposed if unicodedata.category(ch) != "Mn")
    straight = stripped.replace("’", "'").replace("`", "'").replace("´", "'")
    return " ".join(straight.lower().split())


def comune_keys(name: str) -> list[str]:
    """Every name a municipality is filed under: both halves of a bilingual one."""
    return [normalize_comune(part) for part in name.split("/") if part.strip()]


# --- the build ----------------------------------------------------------------------


def build(shp_zip: pathlib.Path, topojson: pathlib.Path, tolerance_m: float) -> tuple[dict, dict]:
    with zipfile.ZipFile(shp_zip) as z:
        names = z.namelist()
        dbf_name = next(n for n in names if n.endswith("_today.dbf"))
        shp_name = next(n for n in names if n.endswith("_today.shp"))
        prj_name = next(n for n in names if n.endswith("_today.prj"))
        prj = z.read(prj_name).decode("ascii", "replace")
        if "WGS_1984" not in prj:
            sys.exit(f"the shapefile is not WGS84: {prj[:60]}")
        records = read_dbf(z.read(dbf_name))
        shapes = read_shp_polygons(io.BytesIO(z.read(shp_name)).getvalue())
    stamp = re.search(r"(\d{8}_\d{4})", dbf_name).group(1)
    if len(records) != len(shapes):
        sys.exit(f"DBF has {len(records)} records, SHP has {len(shapes)} shapes")

    topo = json.loads(topojson.read_text(encoding="utf-8"))
    (collection,) = topo["objects"].values()
    comuni_by_name: dict[str, list[str]] = {}
    for geometry in collection["geometries"]:
        props = geometry["properties"]
        comuni_by_name[props["Nome zona"]] = list(props.get("Comuni") or [])

    stats = {
        "zones": 0, "rings_in": 0, "rings_out": 0, "points_in": 0, "points_out": 0,
        "comuni_raw": 0, "comuni_keys": 0, "comuni_damaged": [], "zones_without_comuni": [],
        "names_that_are_codes": [], "repaired_names": [],
    }
    zones = []
    for record, rings in zip(records, shapes):
        code = record["Zona_all"]
        name = repair_zone_name(record["Nome_zona"])
        if name != record["Nome_zona"]:
            stats["repaired_names"].append((record["Nome_zona"], name))
        prefix = code.split("-")[0]
        if prefix not in REGION_BY_PREFIX:
            sys.exit(f"unknown zone prefix {prefix!r} in {code}: extend REGION_BY_PREFIX")
        if name == code:
            stats["names_that_are_codes"].append(code)

        encoded_rings = []
        for ring in rings:
            stats["rings_in"] += 1
            stats["points_in"] += len(ring)
            simplified = quantize(simplify_ring(ring, tolerance_m))
            if len(simplified) < 3:
                continue
            stats["rings_out"] += 1
            stats["points_out"] += len(simplified)
            encoded_rings.append(delta_encode(simplified))

        raw_comuni = comuni_by_name.get(record["Nome_zona"])
        if raw_comuni is None:
            stats["zones_without_comuni"].append(code)
            raw_comuni = []
        keys: list[str] = []
        for comune in raw_comuni:
            stats["comuni_raw"] += 1
            if "�" in comune:
                stats["comuni_damaged"].append(comune)
                continue
            for key in comune_keys(comune):
                if key not in keys:
                    keys.append(key)
        stats["comuni_keys"] += len(keys)

        zones.append({
            "code": code,
            "name": name,
            "region": REGION_BY_PREFIX[prefix],
            "rings": encoded_rings,
            "comuni": sorted(keys),
        })
        stats["zones"] += 1

    zones.sort(key=lambda zone: zone["code"])
    asset = {
        "source": SOURCE_REPO,
        "stamp": stamp,
        "license": LICENSE,
        "precision": PRECISION,
        "toleranceM": int(tolerance_m),
        "zones": zones,
    }
    return asset, stats


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    parser.add_argument("shp_zip", type=pathlib.Path, help="files/shp/<stamp>_shp.zip")
    parser.add_argument("topojson", type=pathlib.Path, help="files/topojson/<stamp>_today.json")
    parser.add_argument("--tolerance-m", type=float, default=DEFAULT_TOLERANCE_M)
    parser.add_argument("--out", type=pathlib.Path, default=OUT)
    args = parser.parse_args()

    asset, stats = build(args.shp_zip, args.topojson, args.tolerance_m)
    text = json.dumps(asset, ensure_ascii=False, separators=(",", ":"))
    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text(text + "\n", encoding="utf-8", newline="\n")

    size = len(text.encode("utf-8"))
    print(f"{args.out}: {size:,} bytes ({size / 1024:.0f} KB), bulletin {asset['stamp']}, "
          f"tolerance {asset['toleranceM']} m")
    print(f"zones {stats['zones']}, rings {stats['rings_in']} -> {stats['rings_out']}, "
          f"points {stats['points_in']:,} -> {stats['points_out']:,}")
    print(f"municipalities {stats['comuni_raw']:,} listed -> {stats['comuni_keys']:,} keys; "
          f"{len(stats['comuni_damaged'])} dropped for U+FFFD: "
          f"{', '.join(stats['comuni_damaged']) or 'none'}")
    print(f"zones without a municipality list: {', '.join(stats['zones_without_comuni']) or 'none'}")
    print(f"zone names that are their own code ({len(stats['names_that_are_codes'])}): "
          f"{', '.join(stats['names_that_are_codes']) or 'none'}")
    for before, after in stats["repaired_names"]:
        print(f"repaired: {before!r} -> {after!r}")


if __name__ == "__main__":
    if sys.stdout.encoding and sys.stdout.encoding.lower() != "utf-8":
        sys.stdout.reconfigure(encoding="utf-8")
    main()
