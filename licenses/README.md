# Third-party licenses

Chiaro itself is GPL-3.0 (`LICENSE` at the repo root). What ships inside the APK and is
not ours:

| What | License | Where |
|---|---|---|
| [Inter](https://github.com/rsms/inter) (the app's typeface, bundled as `res/font/inter_variable.ttf`) | SIL Open Font License 1.1 | `Inter-OFL.txt` |
| [Meteocons](https://github.com/basmilius/meteocons) v2.0.0 (the weather icon family, converted to the `res/drawable/mc_*.xml` vectors by `tools/import_meteocons.py`, recolored for contrast) | MIT | `Meteocons-MIT.txt` |
| [Material Icons](https://github.com/google/material-design-icons) — one glyph, `place` in the outlined theme, copied into `res/drawable/ic_place_pin.xml` because a widget cannot draw the Compose `ImageVector` the app screens use | Apache 2.0 | `MaterialIcons-Apache-2.0.txt` |

Weather data comes from [Open-Meteo](https://open-meteo.com), which is free for
non-commercial use under CC BY 4.0 and needs no API key. The attribution belongs in the
app's guide, not only here.
