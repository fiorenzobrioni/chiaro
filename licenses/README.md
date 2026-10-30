# Third-party licenses

Chiaro itself is GPL-3.0 (`LICENSE` at the repo root). What ships inside the APK and is
not ours:

| What | License | Where |
|---|---|---|
| [Inter](https://github.com/rsms/inter) (the app's typeface, bundled as `res/font/inter_variable.ttf`) | SIL Open Font License 1.1 | `Inter-OFL.txt` |
| [Google Sans](https://fonts.google.com/specimen/Google+Sans) (the app's second typeface, one Settings tap away, bundled as `res/font/google_sans_variable.ttf`: the upstream `ofl/googlesans` variable font with its unused axes pinned and its glyph set cut to what this app prints, by `tools/import_google_sans.py`) | SIL Open Font License 1.1 | `GoogleSans-OFL.txt` |
| [Meteocons](https://github.com/basmilius/meteocons) v3 (`@meteocons/svg` 3.0.0-next.10, the weather icon family: the 519 `res/drawable/mc3_*.xml` vectors written by `tools/import_meteocons_v3.py`, which re-anchors the colours to the app's two grounds, normalises the ink's size and turns the masks into clips; plus the drawings this repo composes out of its pieces and the family does not have: `sun-one-cloud-*`, `sun-half-cloud-*` and the showers (`tools/compose_sun_cloud.py`), `freezing-rain`, `freezing-drizzle`, `rain-possible`, `snow-possible` (`tools/compose_falls.py`), `meteor`, `moon-star`, `two-stars`, `horizon-star` (`tools/compose_sky_marks.py`). The in-app credit says «adapted for Chiaro» for this reason: MIT does not ask for it, honesty does. Only what a screen names is kept in the APK) | MIT | `Meteocons-MIT.txt` |
| [Warning zone geometry](https://github.com/pcm-dpc/DPC-Bollettini-Criticita-Idrogeologica-Idraulica) (the Dipartimento della Protezione Civile's 187 criticality zones, simplified to 500 m and bundled as `core/data/src/main/assets/warning_zones_it.json` so a place is located offline by where it is) | CC BY 4.0 | the source repo's own notice |
| [Material Icons](https://github.com/google/material-design-icons) — the outlined `place` glyph, copied into `res/drawable/ic_place_pin.xml` because a widget cannot draw the Compose `ImageVector` the app screens use; and ten outlined glyphs for the details grid's eyebrows (umbrella, snowflake, sun, air, cloud, drop, gauge, eye, particles, flower), their path data copied unchanged from `material-icons-extended` 1.7.8 into `ui/icons/MetricIcons.kt` | Apache 2.0 | `MaterialIcons-Apache-2.0.txt` |

Weather data comes from [Open-Meteo](https://open-meteo.com), which is free for
non-commercial use under CC BY 4.0 and needs no API key.

None of these attributions live only in this file. Every one of them is a row in
Settings → «Data and credits», each linking to its source, and the guide names Open-Meteo
where it explains where the numbers come from: a licence that asks for credit is not
satisfied by a file the reader never opens.
