# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in
this repository.

## What this repository is

**Chiaro** is an Android weather app (Kotlin 2.2 / Jetpack Compose Material 3) built for
a general audience: a computed sky as the hero, one sentence before any number, an agenda
of the day's sun and sky with a verdict, what changed in the forecast since the last look,
and alerts the reader writes themselves. Free, no account, no ads, no API key.

It was born as the **daylight edition of [tweather](https://github.com/fiorenzobrioni/tweather)**:
its data layer, alert engine, rules engine and astronomy engine, with the code-editor UI
replaced. **Since 23 set 2026 it is an independent product**: Chiaro no longer tracks
tweather, which follows its own road. The core is Chiaro's own — a fix is made here, for
this app, and is not carried back or kept in step.

Source of truth:

- `VISION.md` — the product: positioning against the store field (§2), identity (§3),
  design language (§4), every screen (§5), the parity map with tweather it started from
  (§6), the module split (§7), the roadmap (§10), the decisions still open (§12).
- `DESIGN.md` — the design system: color (generated, plus the semantic tokens Material
  has no slot for), the sky canvas, the daylight ribbon, type, shape, motion, the
  component kit, the chart rules, accessibility. Every value carries its measured number.
- `PLANNING.md` — the live plan: its rules, the state, what is still open, and the log of
  the current period. **Keep it updated as work progresses**, recording every decision and
  deviation with its reason (the series' rule). Since 21 ott 2026 it is split:
  - **Write only in `PLANNING.md`**: a new `##` section at the end of its log, dated, with who
    asked. A box ticked or opened goes in its «Aperto» list.
  - **`docs/planning/NN-<period>.md` is the archive**, the closed periods from Fase 0 on,
    **frozen**: never edit it. A decision that changes an archived one is written in
    `PLANNING.md`, naming the archived file and section it changes.
  - **To find a section** cited by name in the code («PLANNING, Fase 11», «I dati nuovi a
    schermo»), grep `PLANNING.md` first, then `docs/planning/README.md`, the index of every
    archived `##`/`###` heading with its file and line, and read from that line. Never load an
    archive file whole: they run to 4 000 lines.
  - **Archive** when the log passes ~1 000 lines or at a release, whichever first, as
    `PLANNING.md` «Come è fatto» says, then run `python tools/planning_index.py`
    (`--check` tells if the index is behind).
- `docs/STATES.md` — what an hour, a quarter and a day *are*: the state engine's rules in
  order, every threshold with its reason and measurement. Living: kept in step with
  `WeatherStateEngine` and the day's code.
- `UPSTREAM.md` — **history, frozen on 23 set 2026**: where `:core` came from and how it
  drifted until the split. Read it for context; do not keep it up to date.

## Build and commands

Stack: Kotlin 2.2 + Compose (Material 3), Gradle 9.1 / AGP 8.13, version catalog in
`gradle/libs.versions.toml`. Package/applicationId: `com.callbackdev.chiaro`. minSdk 33
(system per-app language picker, one runtime path for POST_NOTIFICATIONS, themed icons,
full java.time), compile/targetSdk 36.

- Build debug APK: `./gradlew :app:assembleDebug` (output: `app/build/outputs/apk/debug/app-debug.apk`)
- All unit tests: `./gradlew test :app:testDebugUnitTest`
- One module: `./gradlew :core:domain:test` — one class: `--tests "com.callbackdev.chiaro.domain.SomeTest"`
- Lint: `./gradlew :app:lintDebug`
- Installable minified build: `./gradlew :app:assembleRelease -PsignReleaseWithDebugKey`
- On a machine with no system JDK, prepend `JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"`.
- If Maven Central answers `429 Too Many Requests` (common on shared cloud runners and
  sandboxed containers; retrying does not help), route it through Google's mirror with the
  opt-in init script: `./gradlew --init-script tools/maven-mirror.init.gradle.kts
  --no-configuration-cache <tasks>`. It also points Robolectric's runtime download of
  `android-all` at the mirror, which a 429 otherwise fails as `LocationProviderTest` and
  friends. The build files are not changed: CI and local machines keep using Maven Central.

**Modules**: `:core:domain` is **pure Kotlin/JVM** and must stay that way. If a class in
it needs a `Context` or a `Resources`, it is in the wrong module. `:core:data` is the
Android library with the Open-Meteo client, the mapper, Room and DataStore. `:core:sync`
holds the single periodic WorkManager job (fetch, alerts, rules, sky observation); its
notifiers are text and live in `:app` behind the `SyncNotifiers` interface. `:app` holds
everything visible.

**Debug signing**: `keystore/debug.keystore` is intentionally committed (alias
`chiaro-debug`, store/key password `android`) so debug APKs from CI and any machine share
one signature. Do not regenerate it. Debug builds carry `applicationIdSuffix ".debug"`, so
they install side by side with the release-signed app.

**Release signing**: the real keystore lives OUTSIDE the repo; the `release` signingConfig
is created only when the four `CHIARO_KEYSTORE*` properties are all set (from
`~/.gradle/gradle.properties` locally, from `ORG_GRADLE_PROJECT_*` env vars in CI). On an
unconfigured checkout the release build is unsigned by default;
`-PsignReleaseWithDebugKey` signs it with the committed debug key so the minified build is
installable for testing (R8 breakage shows up nowhere else), and stays opt-in so an
unconfigured checkout can never produce an installable release by accident.

**CI**: `.github/workflows/android-ci.yml` runs on every push. Tests and lint run *before*
the APKs: a red suite must never produce an installable artifact.

## Design constraints (non-negotiable)

The full system is `DESIGN.md`; these are the rules that get broken by accident.

- **Roles, never hexes.** No composable outside `ui/theme/` names a color literal.
  `NoRawColorTest` fails the build over it.
- **The screen must not lie.** A section with no data is not drawn, never a card with a
  dash in it. Stale data states its real age. Estimates say so. No placeholder ever
  renders as a value: a skeleton must look like a skeleton, never like a grey zero.
- **Numbers are one model's per hour; states are the app's.** Chiaro is not a relay of the API.
  What an hour or a day *is* (its icon and its word: rain, showers, snow, fog, ice, sky)
  is the app's reading of the forecast, derived from the physical fields (amounts, phase,
  probability, temperature, visibility, cloud), and it may differ from `weather_code`: the
  fog repair and the day's code already do this, and the state engine (`docs/STATES.md`)
  extends it. What the app never does is change a number: a
  probability, an amount, a temperature on screen is the one the model forecast, or an
  estimate that says so. An hour may come from one model and the next from another (that
  is what `best_match` already is, and what Fase 14 allows for ICON-2I in Italy), but the
  app never averages two models into a number: which model an hour's numbers came from is
  always one answer (27 Oct 2026). Every rule that derives a state is written down with its
  reason and its measurement, and tested.
- **Every number says what to do with it.** A metric tile is a value plus its consequence.
  A metric with no honest second line belongs in the details sheet, not on the home screen.
  The visibility tile is the example (27 Oct 2026): drawn only when the state is fog, with
  «prudenza in strada» under the kilometres; out of fog the kilometres have no honest second
  line (the «haze» they printed was right 22% of the time) and the tile is not drawn.
- **A verdict ships with its arithmetic**, and is a glyph and a word before it is a color:
  green/amber/red do not separate under deuteranopia (measured, `DESIGN.md` §2.3).
- **No full-screen spinner, ever.** Cached content first, freshness stated, refresh silent.
- **One hue for a quantity, two plus a neutral for a diverging one, never a rainbow.** No
  dual-axis chart. Scales anchor to the world (15 °C, 0-100%), never to what is on screen.
- **No jargon.** Not translated jargon: absent jargon. The dot-notation identifiers
  (`golden_hour.pm`, `current.temp_c`) stay in the code and never reach a screen.
- **No emoji as iconography.** The weather icons are a vector family (`DESIGN.md` §13.1):
  Meteocons v3, imported by `tools/import_meteocons_v3.py` (+ `svg_paths.py`,
  `reanchor.py`, `shipped_icons.py`). **Nothing under `res/drawable/mc3*`,
  `ui/icons/MeteoconsSets.kt`, `ui/icons/ComposedIcons.kt` or `ui/icons/SkyMarks.kt` is
  edited by hand** — re-running the tool IS the drawing. Three tools write there and they
  do not overlap: the importer owns every name that exists in Meteocons, and
  `tools/compose_sun_cloud.py` owns
  `sun-one-cloud-*` and `sun-half-cloud-*`, the «quasi sereno» and the «poco nuvoloso» this
  repo composes from `clear-day` and `cloudy`, so that WMO codes 0 to 3 climb in steps (the
  family has no honest drawing for code 1, and its code 2 jumped), and the showers
  (`sun-half-cloud-rain-*`, `-snow-*`), code 2 with the drops or flakes of
  `partly-cloudy-*-rain/-snow`. `tools/compose_sky_marks.py` owns the sky catalog's marks
  Meteocons does not have (14 Oct 2026): `meteor` (the falling stars without the moon they
  cross), `moon-star` and `two-stars` (the conjunctions) and, since 25 Oct 2026,
  `horizon-star` (the blue hour: the sun under the line with the first star), still faces
  only. Run both
  composers again after any re-import: `ComposedIconsTest` and `SkyMarksTest` compare the
  composed drawings with their sources path by path and fail when they drift. The repo carries all 519 drawings; only what a screen
  names reaches the APK, so adding one to a screen means adding it to `shipped_icons.py`
  and re-running, and `MeteoconsSetsTest` fails the build if you forget.
- **The app icon is a script's output.** `tools/draw_launcher_icon.py` writes
  `ic_launcher_foreground.xml`, `ic_launcher_monochrome.xml` and `ic_stat_chiaro.xml`: change
  the script and re-run it, never the XML (`DESIGN.md` §3.8).
- **Localization**: everything on screen is prose or data, so **everything localizes**
  (IT/EN, system per-app language picker). There is no code register in this product to
  protect, which is the one rule of the terminal line that does not survive the reskin.
  The reader's own alert messages are user content and are never translated.

## README screenshots

The pictures of the app's screens in the root `README.md` (`docs/screenshots/*.png`) are
drawn by `ReadmeScreenshots` (`app/src/testDebug/.../readme/`), widgets included, from a recorded Milan
(`app/src/test/resources/readme/`, written by `tools/record_readme_data.py`), in English,
and only on request:
`./gradlew :app:testDebugUnitTest --tests "*ReadmeScreenshots" -PupdateScreenshots` (plus
the mirror init script in the sandbox). Without the property the tests are skipped, so no
ordinary run rewrites an image. Standing rules (committente, 25 set 2026):

- **Regenerate them** in the same change whenever it alters what an existing one shows, and
  look at every regenerated image before committing.
- **Add one** when a change brings something worth showing (a new screen, a new section),
  with its caption and alt text in the README; **drop one** whose screen is gone. Keep the
  set small: the meaningful views only.
- **Re-record the data** (`python3 tools/record_readme_data.py`) only when the recording no
  longer holds what a screen needs (a new field the old response does not have); a
  re-recording changes every picture, so it is a change of its own.
- The widgets are drawn too (`widgets.png`, `widget-day-arc.png`): Glance composes each
  one into the `RemoteViews` a launcher receives, applied to a real view and drawn through
  the GPU pipeline so the outline-rounded corners come out round. A widget change
  regenerates them like a screen change does.
- So is the morning summary (`notification-summary.png`): the alert the engine finds in the
  recording, posted by `AlertNotifier`, decorated by the platform's own template on a day
  and a night shade of stock Android. A change to the notifications' text or pictures
  regenerates it.
- **No invented data.** Every picture comes from the recording or from what a reader makes
  with the app's own words (the alerts are created from its ideas).

## Writing `README.md` (root file only)

**No em dashes (`—`) or en dashes (`–`) in the root `README.md`.** Rewrite the sentence
rather than swapping in a hyphen: use a colon when the clause explains, a full stop when
the thoughts are separate, parentheses for an aside. Same house style as tweather, tsteps
and thabit, deliberately scoped to that one file: every other file keeps normal
punctuation.

## The family

Chiaro belongs to a family of four apps by callbackdev, with Passo, Saldo and Tempo. The
sister apps are named **only** in the root `README.md` («The family») and in the project's
documents (VISION, PLANNING, DESIGN): never in a string the app shows, its guide included,
and never in the code, comments and tests included (committente, 10 ott 2026). A comment
says why a value is what it is, not which sister has it too.

## Domain notes

- **Provider**: Open-Meteo (forecast, air quality, geocoding), no API key. Astronomy is
  computed locally by `:core:domain` and works offline.
- **The state of an hour is the app's, not `weather_code`** (25 set 2026,
  `WeatherStateEngine` in `:core:domain`, measured on 26 cities and 4 342 hours): the icon
  and the word come from the amounts, the snowfall, the convective part, the probability,
  the temperature, the visibility and the cloud cover, and the provider's code is read
  only where those are missing or for what the app cannot see (thunderstorms, freezing
  rain). Precipitation needs ≥ 0.1 mm (the probability's own floor) and ≥ 20% (the
  «Maltempo» floor; heavy snow exempt); ≥ 60% with nothing measurable is «probabile», a
  possibility and never a wet hour; the sky is the cloud cover on the provider's 20/50/80.
  `rain` and `showers` change meaning with the model (the UK's rain holds its showers,
  Svalbard's showers hold snow): the liquid is always the total minus `snowfall` / 0.7.
  Every threshold and its reason is in `docs/STATES.md`, the living reference of the
  engine: a rule changes there, in the code and in its test in the same change.
- **Fog is never the code's alone** (decided 27 Oct 2026, Fase 14; in the code since step
  14.3, the same day). The served fog code contradicts the served visibility 68% of the
  times it says fog (6 Sep 2026, 23 cities, 3 864 hours), so the state engine reads fog
  from **the visibility the fetch has** — `best_match`'s own where it carries one of its
  own, otherwise the lowest of the witnesses' (ECMWF IFS 9 km, UKMO 10 km, KNMI and DMI
  HARMONIE Europe 5.5 km; GFS left in 14.2) — persisting into a neighbouring hour on the
  series read, confirmed by the hour's own humidity and by the witnesses: one that sees
  it when the visibility is the hour's own, two when it is lent, the lender among them
  (`docs/STATES.md` §3, rule 5, §11, §12). A visibility that is a witness's copy for the
  six hours around (Open-Meteo's fill: IFS's under MET Nordic's temperature at Oslo) is the
  witness's, and the hour borrows: a number counts once. **DWD removes the visibility from
  ICON, ICON-EU and ICON-D2 on 30 Nov 2026** (feed migration; Open-Meteo: «Visibility is
  only available until November 2026»), and `best_match` in Italy is the ICON family for
  the first five days: from December the app's own visibility there is null or a 9 km
  fill, and a rule that let the code decide when the visibility is missing would put the
  68%-wrong code back on screen; with no visibility of any kind there is no fog. The
  visibility tile follows the same state (since 14.3: drawn only in fog, never «haze» from
  a number the mist measurement found wrong 78% of the time, STATES §10).
- **Reading the provider's `weather_code`** (inherited from tweather's Fase 13b/13c,
  re-measured 6 Sep 2026 over 23 cities and 3 864 hours; now rules 1–2 of the engine and
  the day's label on its states): the day's code comes from the day's own hours, and
  precipitation must be material before it labels the day (≥ 1 mm or ≥ 3 hours, hazard
  codes exempt): one
  hour of 0.1 mm at 1% probability was printing "Drizzle" over a whole day. `visibility`
  and `precipitation_probability` are model-dependent and therefore **nullable all the
  way to the screen** — §1.1 in the data layer: the visibility tile is not drawn, the
  hour cell prints nothing, and the rain sparkline breaks rather than plotting a zero
  nobody forecast.
- **Battery is a feature**: one shared periodic job for sync, alerts, rules and sky
  observation; inexact alarms for reminders (hence the 15 minute floor and no
  `SCHEDULE_EXACT_ALARM`); no foreground service, no background location, no FCM.
  That is where the cost is paid, and **not** on a screen the reader just opened: the
  report cache TTL is `WeatherFreshness.ProviderResolution` (15 min, Open-Meteo's own
  `"interval": 900`), never `update_frequency_min`. Using the polling interval as the
  TTL let a battery setting decide how old the hero may be while somebody is looking
  at it — an hour by default, two at the top of the range, with the freshness chip
  silent because a cache hit is never stale. Today re-reads when the page comes back
  (`WhileSubscribed`) and, past those 15 minutes, on its minute tick; both silently,
  and neither under battery saver (`PowerSaveState`) — but `push()` still runs there,
  because the stated age and the recency trim cost nothing and freezing them would
  trade battery for a page that lies about the hour. A pull to refresh and a page with
  nothing to show yet are never postponed.
- **Offline**: the last successful report per place is kept with no TTL and carries a week
  of forecast, so the app is never blank. `WeatherRecency` drops the hours that have
  already happened; `WeatherFreshness` decides whether to trust what is left.
- **Storage is bounded by time and by owner** (14 ott 2026, when the Journal left with
  every table and column only it read). `weather_history` is a forecast and the rules
  that fired on it, one commit per 15-minute window (a fetch inside it rewrites the last
  commit), three days a place and never fewer than two rows; it feeds Today's "What
  changed", yesterday's high and Alerts' «last fired», nothing else. `report_cache`
  keeps a followed place up to the forecast horizon and at most 8 places no longer
  followed; `StoredDataSweep` empties a place no longer followed after a week, and the
  rules' latches of a removed place or a deleted rule go at the next commit. A new thing
  written to disk comes with its bound, or it is a leak.
- **The engines are tested, and they are Chiaro's.** They came from tweather and have been
  this app's own since 23 set 2026: change them here, with their tests, for what Chiaro
  needs. There is no upstream to keep in step with and no shared core to extract.
- **The provider is a moving target** (27 Oct 2026). Open-Meteo changes feeds and the
  composition of `best_match` without notice (ECMWF IFS HRES joined it everywhere in
  Oct 2025; DWD drops the visibility on 30 Nov 2026; which regional model serves a country
  is an order of rules in `ForecastapiController.swift`, not a table). So: every variable
  the engine reads has a named source and a stated road when it is missing (STATES §4), a
  claim about which model `best_match` is somewhere is a measurement with a date, never a
  recollection, and `tools/probe_provider.py` (workflow `Probe provider`, monthly and on
  request) records at 45 reference cities which model `best_match` is per variable and
  lead day, where the quarter of an hour is really computed, and which variables arrive
  null — and **fails when a variable full last month is empty now**. A red probe is the
  day the provider changed something: the work starts from its table, in
  `tools/measurements/probe-<date>.json`.
- **Free and without a key is a constraint, not a default** (committente, 27 Oct 2026).
  Open-Meteo's non-commercial tier counts calls per IP address and application — 600 a
  minute, 5 000 an hour, 10 000 a day, where a request weighs `variables / 10` beyond ten
  variables and `weeks / 2` beyond two — with no SLA, and its maintainer refused API keys
  for the free tier on 18 Oct 2026 (#2219) after the Central-Europe overload of #2220.
  Hence: every request the app sends carries its weight in calls in a comment beside its
  variable list (today: forecast 5.2, witnesses 1.6, air 1.4 — 8.2 a fetch, measured by
  the probe); a new request or variable writes the new sum in `PLANNING.md`; no scheduled
  fetch is pinned to the full hour (the provider drops requests at XX:00 under load); and
  nothing in the app calls an API of the paid tiers (Ensemble, Historical, Satellite,
  Previous Runs): those are the measurement tools', which run on a runner with an address
  of their own.
