#!/usr/bin/env python3
"""I dati veri da cui sono disegnate le schermate del README (25 set 2026).

Le immagini di `docs/screenshots/` le disegnano i test `ReadmeScreenshots` di `:app`,
e le disegnano da **risposte vere**, non da numeri inventati per stare bene in foto:
Milano, registrata una volta con questo script e poi tenuta ferma in
`app/src/test/resources/readme/`, cosi' ogni rigenerazione disegna la stessa giornata
e un'immagine cambia solo quando cambia l'app.

Cosa registra, e da dove:

- `geocoding.json`: il geocoder di Open-Meteo in inglese, come lo interroga l'app su un
  telefono in inglese (il README e' in inglese: «Milan», «Lombardy»).
- `forecast.json`: la previsione, con **le stesse variabili che chiede l'app**. Le liste
  non sono copiate qui: sono lette da `OpenMeteoApis.kt` a ogni esecuzione, cosi' una
  variabile aggiunta all'app finisce anche nella registrazione successiva.
- `air-quality.json`: la qualita' dell'aria, stesse regole.
- `bulletin.xml`: l'ultimo bollettino di criticita' della Protezione Civile (il CAP dentro
  `latest_all.zip`), per la scheda degli avvisi ufficiali.
- `recording.json`: quando e' stata fatta la registrazione. E' l'«adesso» dei test: le
  schermate sono disegnate cinque minuti dopo, come le vedrebbe chi apre l'app subito
  dopo l'aggiornamento.

Si rilancia quando la registrazione non basta piu' (un campo nuovo che nessuna risposta
vecchia contiene), poi
`./gradlew :app:testDebugUnitTest --tests "*ReadmeScreenshots" -PupdateScreenshots`.

    python3 tools/record_readme_data.py
"""

import io
import json
import re
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
import zipfile
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
APIS = ROOT / "core/data/src/main/kotlin/com/callbackdev/chiaro/data/remote/OpenMeteoApis.kt"
OUT = ROOT / "app/src/test/resources/readme"

PLACE = "Milan"
LANGUAGE = "en"

DPC_LATEST = (
    "https://raw.githubusercontent.com/pcm-dpc/"
    "DPC-Bollettini-Criticita-Idrogeologica-Idraulica/master/files/all/latest_all.zip"
)


def kotlin_constants(source: str, interface: str) -> dict:
    """Le `const val X = "a" + "b"` dentro il companion di [interface], concatenate."""
    start = source.index(f"interface {interface}")
    nxt = source.find("\ninterface ", start + 1)
    body = source[start: nxt if nxt != -1 else len(source)]
    values = {}
    for match in re.finditer(r"const val (\w+) =", body):
        name = match.group(1)
        literals = []
        # The rest of the `=` line first (often empty), then the continuation lines;
        # comment lines between the literals are skipped, and the first line that does
        # not end with `+` closes the expression.
        for line in body[match.end():].splitlines():
            code = line.split("//", 1)[0].strip()
            if not code:
                continue
            found = re.findall(r'"([^"]*)"', code)
            if not found:
                break
            literals.extend(found)
            if not code.endswith("+"):
                break
        if literals:
            values[name] = "".join(literals)
    return values


def get(url: str, params: dict | None = None, attempts: int = 3) -> bytes:
    """Una GET, ritentata sugli errori di rete: una registrazione a meta' e' peggio di niente."""
    if params:
        url = f"{url}?{urllib.parse.urlencode(params)}"
    request = urllib.request.Request(url, headers={"User-Agent": "chiaro-readme-recorder"})
    for attempt in range(1, attempts + 1):
        try:
            with urllib.request.urlopen(request, timeout=60) as response:
                return response.read()
        except urllib.error.URLError:
            if attempt == attempts:
                raise
            time.sleep(2 * attempt)
    raise AssertionError("unreachable")


def pretty(raw: bytes) -> str:
    return json.dumps(json.loads(raw), ensure_ascii=False, indent=1) + "\n"


def main() -> int:
    source = APIS.read_text(encoding="utf-8")
    forecast_vars = kotlin_constants(source, "OpenMeteoForecastApi")
    air_vars = kotlin_constants(source, "OpenMeteoAirQualityApi")
    for name in ("CURRENT_VARIABLES", "HOURLY_VARIABLES", "DAILY_VARIABLES"):
        if name not in forecast_vars:
            sys.exit(f"{name} not found in {APIS.name}: has the file changed shape?")
    if "CURRENT_VARIABLES" not in air_vars:
        sys.exit(f"the air quality CURRENT_VARIABLES not found in {APIS.name}")

    days = re.search(r"const val FORECAST_DAYS = (\d+)", source)
    if days is None:
        sys.exit(f"FORECAST_DAYS not found in {APIS.name}")

    fetched_at = datetime.now(timezone.utc).replace(microsecond=0)

    geocoding = get(
        "https://geocoding-api.open-meteo.com/v1/search",
        {"name": PLACE, "language": LANGUAGE, "count": 1, "format": "json"},
    )
    place = json.loads(geocoding)["results"][0]
    lat, lon = place["latitude"], place["longitude"]

    forecast = get(
        "https://api.open-meteo.com/v1/forecast",
        {
            "latitude": lat,
            "longitude": lon,
            "current": forecast_vars["CURRENT_VARIABLES"],
            "hourly": forecast_vars["HOURLY_VARIABLES"],
            "daily": forecast_vars["DAILY_VARIABLES"],
            "timezone": "auto",
            "forecast_days": int(days.group(1)),
        },
    )
    air = get(
        "https://air-quality-api.open-meteo.com/v1/air-quality",
        {"latitude": lat, "longitude": lon, "current": air_vars["CURRENT_VARIABLES"], "timezone": "auto"},
    )

    with zipfile.ZipFile(io.BytesIO(get(DPC_LATEST))) as archive:
        caps = [n for n in archive.namelist() if re.fullmatch(r"(.*/)?Cap_\d{8}_\d{4}\.xml", n)]
        if not caps:
            sys.exit("no Cap_*.xml in the DPC archive")
        bulletin = archive.read(caps[0])

    OUT.mkdir(parents=True, exist_ok=True)
    (OUT / "geocoding.json").write_text(pretty(geocoding), encoding="utf-8")
    (OUT / "forecast.json").write_text(pretty(forecast), encoding="utf-8")
    (OUT / "air-quality.json").write_text(pretty(air), encoding="utf-8")
    (OUT / "bulletin.xml").write_bytes(bulletin)
    (OUT / "recording.json").write_text(
        json.dumps(
            {
                "fetchedAt": fetched_at.isoformat().replace("+00:00", "Z"),
                "place": place["name"],
                "bulletin": Path(caps[0]).name,
            },
            indent=1,
        )
        + "\n",
        encoding="utf-8",
    )
    current = json.loads(forecast)["current"]
    print(f"{place['name']} at {current['time']} ({json.loads(forecast)['timezone']}): "
          f"{current['temperature_2m']} °C, code {current['weather_code']}, "
          f"cloud {current['cloud_cover']}%; bulletin {Path(caps[0]).name}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
