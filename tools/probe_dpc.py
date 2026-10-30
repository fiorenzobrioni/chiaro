#!/usr/bin/env python3
"""Il bollettino della Protezione Civile, visto dalla sonda (committente, 30 ott 2026).

L'app legge le allerte ufficiali dal repository GitHub del Dipartimento (`DpcBulletinSource`),
e un canale che si spegne non rompe niente: nessun bollettino trovato, niente tenuto, la corsa
dopo richiede. Quel silenzio e' identico a «nessuna allerta oggi», e un lettore lo scoprirebbe
il giorno di un'allerta vera non arrivata. Questo modulo lo scopre prima, una volta al mese
insieme alla sonda del provider (`probe_provider.py`, workflow `Probe provider`), rifacendo in
Python la scoperta dell'app alla lettera:

1. l'Atom dei commit (il sito, mai l'API REST: 60 richieste l'ora per IP, condiviso dagli
   operatori mobili), i primi tre commit, e sulla prima riga del `.diff` di ciascuno lo stamp
   `AAAAMMGG_HHMM` del bollettino: come `DpcBulletinSource.discover` e `stampOf`;
2. l'eta' dello stamp: il bollettino esce ogni giorno entro le 16 (Europe/Rome), quindi alle
   5:23 del mattino quello di ieri deve esserci. Oltre [MAX_AGE_HOURS] la sentinella scatta;
3. `files/xml/<stamp>.zip` e dentro `Cap_<stamp>.xml`: deve essere un CAP 1.2 con almeno un
   blocco `info` e aree con il geocodice «Zona di Allerta» (`DpcBulletinReader.ZONE_GEOCODE`),
   cioe' il formato che il lettore dell'app sa leggere;
4. `files/all/latest_all.zip`, il ripiego dell'app sulle reti non a consumo: una HEAD sola, per
   sapere che c'e' ancora e quanto pesa.

Quattro richieste piccole e una HEAD, da un indirizzo che non e' quello di nessun lettore. Quel
che NON vede: un guasto piu' breve del mese fra due sonde (la sonda si lancia a mano al dubbio),
e un cambio di contenuto che il lettore accetta ma legge male (un livello nuovo, una zona
rinominata), che e' un confronto con `warning_zones_it.json` e non sta qui.

Solo libreria standard di Python 3.9+. Da solo stampa il rapporto e esce con 1 se la sentinella
scatta; importato da `probe_provider.py` entra nel suo JSON e nella sua sentinella.

    python tools/probe_dpc.py
"""

import datetime as dt
import io
import re
import sys
import time
import urllib.error
import urllib.request
import xml.etree.ElementTree as ET
import zipfile

try:
    from zoneinfo import ZoneInfo
    ROME = ZoneInfo("Europe/Rome")
except Exception:  # un runner senza tzdata: l'ora solare, e l'eta' sbaglia al piu' di un'ora
    ROME = dt.timezone(dt.timedelta(hours=1))

#: Le stesse costanti di `DpcBulletinSource`: il repository, il sito, il raw.
REPO = "pcm-dpc/DPC-Bollettini-Criticita-Idrogeologica-Idraulica"
SITE_BASE = "https://github.com/%s" % REPO
RAW_BASE = "https://raw.githubusercontent.com/%s/master/files" % REPO
COMMITS_TO_TRY = 3
COMMIT_ID = re.compile(r"Grit::Commit/([0-9a-f]{40})")
STAMP = re.compile(r"\d{8}_\d{4}")

#: Il geocodice che porta il codice di zona in ogni area (`DpcBulletinReader.ZONE_GEOCODE`).
ZONE_GEOCODE = "Zona di Allerta"
CAP_NS = "{urn:oasis:names:tc:emergency:cap:1.2}"

#: Due giorni: il bollettino e' quotidiano entro le 16, la sonda gira alle 5:23 del giorno dopo,
#: e un giorno saltato dalla pipeline non e' ancora un canale spento.
MAX_AGE_HOURS = 48

USER_AGENT = "chiaro-probe (+https://github.com/fiorenzobrioni/chiaro)"


def fetch(url, attempts=3, method="GET", first_line_only=False):
    """Una richiesta con qualche ripetizione sui guasti passeggeri. Con `first_line_only` legge
    una riga e chiude, come l'app sul `.diff` (quello di un GeoJSON pesa 5,5 MB)."""
    req = urllib.request.Request(url, method=method, headers={"User-Agent": USER_AGENT})
    for attempt in range(attempts):
        try:
            with urllib.request.urlopen(req, timeout=60) as r:
                if method == "HEAD":
                    return r.status, r.headers, b""
                if first_line_only:
                    return r.status, r.headers, r.readline()
                return r.status, r.headers, r.read()
        except urllib.error.HTTPError as e:
            if e.code in (429, 502, 503) and attempt < attempts - 1:
                time.sleep(10 * (attempt + 1))
                continue
            return e.code, e.headers, b""
        except (urllib.error.URLError, TimeoutError):
            if attempt < attempts - 1:
                time.sleep(5 * (attempt + 1))
                continue
            raise
    return None, None, b""


def discover():
    """Lo stamp del bollettino piu' recente, dal feed e dalla prima riga di un diff."""
    status, headers, body = fetch("%s/commits/master.atom" % SITE_BASE)
    if status != 200:
        return {"error": "feed: HTTP %s" % status}
    shas = COMMIT_ID.findall(body.decode("utf-8", "replace"))[:COMMITS_TO_TRY]
    if not shas:
        return {"error": "feed: nessun commit nell'Atom (%d byte)" % len(body)}
    for sha in shas:
        st, _, line = fetch("%s/commit/%s.diff" % (SITE_BASE, sha), first_line_only=True)
        if st != 200:
            continue
        m = STAMP.search(line.decode("utf-8", "replace"))
        if m:
            return {"stamp": m.group(0), "commit": sha, "feed_etag": headers.get("ETag")}
    return {"error": "diff: nessuno stamp nelle prime righe di %d commit" % len(shas)}


def stamp_time(stamp):
    return dt.datetime.strptime(stamp, "%Y%m%d_%H%M").replace(tzinfo=ROME)


def read_cap(zip_bytes):
    """Il `Cap_<stamp>.xml` dentro lo zip, letto quanto basta a sapere che e' il formato atteso."""
    with zipfile.ZipFile(io.BytesIO(zip_bytes)) as z:
        names = [n for n in z.namelist() if n.rsplit("/", 1)[-1].startswith("Cap_") and n.endswith(".xml")]
        if not names:
            return {"error": "zip senza Cap_*.xml: %s" % ", ".join(z.namelist()[:5])}
        xml = z.read(names[0])
    root = ET.fromstring(xml)
    if root.tag != CAP_NS + "alert":
        return {"error": "radice %s, non un CAP 1.2" % root.tag}
    infos = root.findall(CAP_NS + "info")
    areas = [a for i in infos for a in i.findall(CAP_NS + "area")]
    zoned = 0
    for area in areas:
        for g in area.findall(CAP_NS + "geocode"):
            if (g.findtext(CAP_NS + "valueName") or "").strip() == ZONE_GEOCODE and (g.findtext(CAP_NS + "value") or "").strip():
                zoned += 1
                break
    return {
        "file": names[0].rsplit("/", 1)[-1],
        "identifier": root.findtext(CAP_NS + "identifier"),
        "sent": root.findtext(CAP_NS + "sent"),
        "infos": len(infos),
        "areas": len(areas),
        "zoned_areas": zoned,
        "cap_bytes": len(xml),
    }


def probe(now=None):
    """Il bollettino com'e' adesso: stamp, eta', il CAP e il ripiego. Mai un'eccezione fuori: un
    guasto e' un campo `error`, e la sentinella decide."""
    now = now or dt.datetime.now(dt.timezone.utc)
    out = {"repo": REPO, "checked_at": now.isoformat(timespec="seconds")}
    try:
        found = discover()
        out.update(found)
        if "stamp" in found:
            issued = stamp_time(found["stamp"])
            out["issued_at"] = issued.isoformat()
            out["age_hours"] = round((now - issued).total_seconds() / 3600, 1)
            status, headers, body = fetch("%s/xml/%s.zip" % (RAW_BASE, found["stamp"]))
            out["zip_status"] = status
            if status == 200:
                out["zip_bytes"] = len(body)
                try:
                    out["cap"] = read_cap(body)
                except Exception as e:
                    out["cap"] = {"error": "%s: %s" % (type(e).__name__, e)}
        status, headers, _ = fetch("%s/all/latest_all.zip" % RAW_BASE, method="HEAD")
        out["fallback"] = {"status": status, "bytes": int(headers.get("Content-Length") or 0) if headers else 0}
    except Exception as e:
        out["error"] = "%s: %s" % (type(e).__name__, e)
    return out


def trips(result):
    """Quel che fa scattare la sentinella: nessuno stamp, un bollettino vecchio, un CAP che
    manca o non si legge, il ripiego sparito. Frasi per il registro, come quelle del provider."""
    out = []
    if "error" in result:
        out.append("bollettino DPC: %s" % result["error"])
        return out
    if result.get("age_hours", 0) > MAX_AGE_HOURS:
        out.append("bollettino DPC: l'ultimo e' del %s, %.0f ore fa (oltre %d)"
                   % (result["stamp"], result["age_hours"], MAX_AGE_HOURS))
    if result.get("zip_status") != 200:
        out.append("bollettino DPC: files/xml/%s.zip risponde HTTP %s" % (result.get("stamp"), result.get("zip_status")))
    cap = result.get("cap") or {}
    if "error" in cap:
        out.append("bollettino DPC: il CAP non si legge (%s)" % cap["error"])
    elif cap:
        if cap["infos"] == 0:
            out.append("bollettino DPC: il CAP non ha blocchi info")
        if cap["zoned_areas"] == 0:
            out.append("bollettino DPC: nessuna area con il geocodice «%s»" % ZONE_GEOCODE)
    fb = result.get("fallback") or {}
    if fb.get("status") != 200:
        out.append("bollettino DPC: il ripiego files/all/latest_all.zip risponde HTTP %s" % fb.get("status"))
    return out


def line(result):
    """Una riga per la tabella della sonda."""
    if "error" in result:
        return "Bollettino DPC: %s" % result["error"]
    cap = result.get("cap") or {}
    parts = ["Bollettino DPC: %s, %.0f ore fa" % (result.get("stamp"), result.get("age_hours", 0))]
    if cap and "error" not in cap:
        parts.append("CAP %d KB, %d info, %d aree (%d con zona)"
                     % (cap["cap_bytes"] // 1024, cap["infos"], cap["areas"], cap["zoned_areas"]))
    elif cap:
        parts.append("CAP: %s" % cap["error"])
    else:
        parts.append("zip HTTP %s" % result.get("zip_status"))
    fb = result.get("fallback") or {}
    parts.append("ripiego HTTP %s, %.1f MB" % (fb.get("status"), fb.get("bytes", 0) / 1e6))
    return "; ".join(parts)


def main():
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")
    result = probe()
    print(line(result))
    tripped = trips(result)
    if tripped:
        print()
        print("SENTINELLA: il canale delle allerte ufficiali non e' piu' quello che l'app legge.")
        for t in tripped:
            print(" - " + t)
        sys.exit(1)


if __name__ == "__main__":
    main()
