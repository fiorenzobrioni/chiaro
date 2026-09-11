# UPSTREAM.md — where `:core` came from

`:core:domain` and `:core:data` are a **copy** of tweather's domain and data layers,
not a link to them (VISION.md §7.3). This file is the ledger that decision depends on:
without it, the first time the same bug has to be fixed in both apps, telling what
drifted from what is archaeology.

## The seed

| | |
|---|---|
| Source | [fiorenzobrioni/tweather](https://github.com/fiorenzobrioni/tweather) |
| Commit | `d7914ec838c38b1dc0757279cdf6e3772526b750` |
| Short | `d7914ec`, 2026-09-02 |
| Seeded | 2026-09-02 (Fase 0) |
| Files | 78 Kotlin files: 24 domain main, 16 domain test, 23 data main, 15 data test |

Reproduce it with a tweather checkout beside this repo:

```bash
python3 tools/seed_core.py ../tweather
python3 tools/seed_edits.py
```

`seed_core.py` is the mechanical half (package rename, four identifier renames, the
sample report repackaged). `seed_edits.py` is the half that is not mechanical, and it
is short on purpose — three edits, each with its reason in the file:

1. The four settings types the engines read (`TemperatureUnit`, `WindSpeedUnit`,
   `UnitSettings`, `NotificationSettings`) move from the store into
   `domain/settings/`, so `:core:domain` depends on nothing underneath it.
2. `ServiceLocator` stops importing the app: the User-Agent and the "new data landed"
   callback are handed in by `ServiceLocator.install` from `ChiaroApplication`, rather
   than reached for through `BuildConfig` and a widget class.
3. `sampleWeatherReport` becomes public: it crossed a module boundary, so `internal`
   no longer reaches its readers.

## What is NOT the same as upstream

- `sys@tweather.app` → `sys@chiaro.app` in the history rows. A value, not a comment.
- `TweatherDatabase` → `ChiaroDatabase`, `tweather.db` → `chiaro.db`.
- `CityStore` grew `move(city, toIndex)` and `insert(city, index)` (Fase 3): Chiaro's
  Places sheet is reorderable and its swipe-to-remove has an undo, two things
  tweather's Explorer never needed. Additive only — every inherited method and test
  is unchanged. If tweather ever grows the same needs, these belong upstream too.
- The position path diverged in three places (review della posizione, 4 set 2026), and
  the three are not the same kind of divergence:
  - **`LocationProvider` should NOT diverge.** `currentFix` takes a `maxAge` and a
    `timeout`, answers from the position the system already holds when one that young
    exists, and bounds the last-known fallback by age. All of it was a bug upstream too,
    on the byte-for-byte identical file — so the fix was carried back rather than kept
    here. Keep the two copies in step. The file was **corrected in both again on 5 set
    2026** (the place name, below): it is still identical modulo the package name, which
    is the property to preserve.
  - `CityStore.updateGpsCity(city)` became `adoptGpsFix(fix: GeoFix, at: Instant): City`
    and gained the `gps_fixed_at` preference. Chiaro-only for now: it exists because the
    place PAGE is keyed on the cacheKey and a 1.1 km grid made the page blank on a walk
    across town, which is a shape tweather's single editor does not have. The persisted
    instant, on the other hand, would help upstream too.
  - `CachedLocationProvider` is Chiaro-only and is about Chiaro's own shape: two
    ViewModels reach the position (Places owns the toggle, Today owns the page) where
    tweather has one, so the throttle had to move under both of them. Upstream has
    nothing to share it between.
- `SettingsStore` lost the editor's vocabulary and gained Chiaro's (Fase 4):
  `EditorSettings`, `showDetails`, `themeProfileName` and the `lastModified` stamp
  named surfaces that only exist in tweather (line numbers, the technical JSON view,
  the three theme profiles, the `// Last modified:` header of `settings.config`);
  in their place sit `themeMode` and `dynamicColor`, the two keys Chiaro's
  Appearance group actually edits. The engine-facing keys (units, notifications,
  sky, update frequency, widget opacity) are byte-for-byte the same preferences.
  `SettingsStoreTest` is new — upstream never had one.
- `WorkspaceStore` slimmed to the one concept that survives the reskin (Fase 4): the
  one-shot pointer to the help surface, renamed onto Chiaro's (`guideCardDismissed`).
  `MainEditorFile` and the active-tab state left with the editor they described.
- One piece of tweather's APP layer did survive after all (Fase 5): the sky reminder
  trio (`SkyAlarmScheduler`, `SkyAlarmReceiver`, `SkyNotifier`) is a near-verbatim
  port into Chiaro's `:app/notifications` — the alarm reasoning is product, not
  presentation — with the notification text rewritten as localized prose. A fix in
  the alarm logic almost certainly belongs in both apps.
- `:core:sync` exists here and not upstream (Fase 6): tweather keeps its worker and
  scheduler in `:app` because its notifiers live a file away; Chiaro's split is what
  makes the shared-core extraction cheap. The worker and `SyncScheduler` are
  near-verbatim ports of tweather's `WeatherSyncWorker`/`AlertScheduler` minus the
  widget legs (they return in Fase 8), with the notifiers moved behind
  `SyncNotifiers` — text is presentation, and a `:core:*` module must not own it.
- `RuleStore` grew a parameterized `add(name, conditions, message)` returning the
  created rule (Fase 6): Chiaro's templates are born in the reader's language, while
  the inherited `add()` seeds tweather's fixed English starter. Additive, tested;
  `WeatherRepository` likewise grew `firedRules(entry)` so the JSON the repository
  writes is decoded by the repository too — and, in Fase 7, `snapshot`, `forecast`
  and `skyRuns` for the Journal, on the same principle.
- `WeatherSnapshots.flattenForecast` stores SEVEN target dates here, two upstream
  (Fase 7): tweather's Logs only ever showed tomorrow and the day after, Chiaro's
  drift strip and "what changed" are about the week — "Saturday improved" needs
  Saturday on disk. `ForecastDiff` is per-date and unchanged (its `dayLabel` derived
  from date distance here for a while; upstream's Fase 28 removed the field — a hunk
  carries its date, not a relative word — and the removal was carried here on 9 set
  2026, so the two files are identical again). If tweather ever widens its Logs, the
  horizon belongs upstream too.
- `DailyForecast.precipPct` is **nullable** here, non-null upstream (Fase 7b): the
  seed maps `precipitation_probability_max` with `?: 0`, and a zero is a forecast of
  no rain put in the mouth of a model that never gave one. Chiaro's §1.1 already
  carried the hourly probability nullable to the screen; the daily one now travels the
  same way — the week row prints nothing, `flattenForecast` omits the key, the drift
  strip draws absence and the morning summary drops the clause. This is a bug upstream
  too and the fix belongs there. **Carried upstream on 9 set 2026**, together with
  `WeatherCodes.FIRST_PRECIP_CODE` / `isPrecipitation` (the WMO floor the mapper
  reads, moved out of the mapper here so `ForecastOutcome` could read it too):
  `WeatherModels`, `RuleVariables`, the mapper and their tests are byte-identical
  again. tweather's README prints `?` for a day with no probability and its JSON
  `null`, as both already did for the hour.
- `WeatherSnapshots.flatten` writes two keys the seed does not (Fase 7b):
  `current.wmo_code` and `current.precip_last_hour_mm`. `ForecastOutcome` needs to ask
  a past commit "was it raining when you looked", and the answer has to be a number the
  domain can read rather than the English label matched back by hand. The same pass
  fixed an inherited leak beside them: `current.precip_chance_pct` was written with
  `.toString()` on a nullable Int and had been storing the literal string `"null"`.
  **Here the two copies of `flatten` diverge on purpose (9 set 2026).** Upstream met
  the same word on the same day (its Fase 28, 6 set) and decided the opposite:
  `history.diff` is a diff OF `weather_data.json`, so a value the model did not fill
  is a LINE saying `null` — a named `NullValue`, a fixed key set, the widget and the
  Logs reading it as absence. Chiaro's Journal is prose: its shifts pair keys and a
  missing one is silence, so the key is left out here, for `precip_chance_pct`,
  `sunrise`, `sunset` and `daylight_duration` alike. Same fact, two registers, two
  files — the surfaces rule, not the identity rule. What did travel from that Fase 28
  is the rest of it: `location` falls back on the country like `City.label`, and the
  sky block carries `astronomical.daylight_duration` (via `Duration.hhMm()`, now a
  domain function in both).
- `ForecastOutcome` is new and Chiaro-only (Fase 7b): what the app predicted for a
  finished day, checked against what it then observed. Upstream's Logs render commits;
  reading two of them against each other to say "it rained" is a Chiaro surface.
- `WeatherHistoryDao.prune` was global and is now a **per-city** `pruneCity` plus a
  global backstop (Fase 7b): with one shared cap of a hundred rows, four saved places
  each got twenty-five commits, so a place's diary depth depended on how many other
  places you follow. The backstop stays because removing a place does not delete its
  commits. Upstream has one city at a time and never met this.
- `FetchLogStore` is new and Chiaro-only (Fase 7): a bounded ring of failed fetches
  (when, which place, why) so the Journal can say "an update didn't make it" —
  offline honesty is a Chiaro surface; upstream's Logs render commits, and a commit
  that never happened has nothing to render there.
- `SkyNotScheduled.DARK_ALL_DAY` and the branch in `SkyScheduler.darkness()` that
  returns it (8 set 2026): the darkness window reported `NO_DARKNESS` for two opposite
  skies — the sun that never sinks 18° under the horizon and the sun that never climbs
  back up to it — and the Sky screen said "never gets fully dark" over a sky that is
  dark at noon. It only happens above 84.6° of latitude at the solstice, so no town ever
  saw it; it is still the engine calling a polar night a white night. **This is a bug
  upstream too** — `sky.crontab` prints the same `∅` reason — and the fix belongs there
  as well: the new reason, the branch, and `SkySchedulerTest`'s near-pole case.
  **Carried upstream on 9 set 2026**: `SkyScheduler.kt` and `SkySchedulerTest.kt` are
  byte-identical again, and `sky.crontab`'s ∅ column gained its own sentence for it
  (`note_sky_dark_all_day`, EN/IT), because its `when` over the reasons is exhaustive
  and the new one had to be said somewhere.
- `WeatherSnapshots.flattenForecast` stores **today** as well as the seven days after it
  (8 set 2026): the Journal's drift strip gained a row for the day in progress, and a row
  needs its day on disk. Chiaro-only, like the seven-day horizon before it. (The seam
  this note used to describe — `ForecastDiff.dayLabel` calling the earliest stored
  date "tomorrow" — is gone: upstream removed the field in its Fase 28.)
- `ForecastOutcome` (Chiaro-only, above) changed what a reading vouches for (8 set
  2026): the time since the previous reading, capped at two hours — the app's own longest
  cadence — instead of a fixed hour. At the two-hour cadence a day watched end to end
  covered twelve hours and never reached the sixteen-hour floor, so a dry day at that
  setting was never called dry. The rain itself still counts only for the hour its
  millimetres describe. `ForecastOutcomeTest` has the three cases.
- `SearchHistoryStore.remove(term)` and `restore(terms)` (7 set 2026, recorded here
  on 9 set): Places' recent searches can be removed one by one and the removal
  undone. `restore` writes the list back in the order it is given rather than
  replaying `add`, which would have filed yesterday's search as the newest one.
  Additive, six tests; Chiaro-only, since upstream's `history -c` only ever clears.
- `WeatherHistoryDao.observeFor` and `WeatherRepository.historyFlowFor` (Fase 7b,
  recorded here on 9 set): the Journal follows one city's commits as a Flow instead
  of re-reading the table on a timer. Chiaro-only; the fake DAO in
  `SearchLanguageTest` grew the override, as it did for `pruneCity`.
- `WeatherFreshnessTest` lives in `:core:data` here and in the domain test tree
  upstream: it iterates `UpdateFrequencies`, which belongs to the store, and
  `:core:domain` cannot see the store. Same test, different tree — the seed diff
  will always list it as missing on one side and extra on the other.
- `PowerSaveState` was in a file called `PowerSaving.kt` upstream until 9 set 2026,
  when it was renamed there after its class like every other file, so the seed diff
  pairs the two copies. Code identical; the comment was given one wording.
- `ServiceLocator.overrideForTests` takes a `fetchLogStore` (9 set 2026), for
  `WeatherSyncWorkerTest`. Chiaro-only because the store is.
- `City.countryCode` and `City.admin3` (9 set 2026, the first PR of Fase 11): nullable
  with defaults, so every saved list written before them decodes unchanged, and filled
  on both roads. `GeoResultDto` gained `admin3` (`country_code` was already received and
  dropped) and `toCity` passes both; `GeoFix` grew the same two fields, `toGpsCity`
  carries them, `adoptGpsFix` treats them like the name (a fix that knows them updates
  them, one that does not leaves them alone), and `geocodedPlace` reads
  `Address.countryCode` and the first `locality` of the ladder. **This makes
  `LocationProvider.kt` differ from upstream for the first time on purpose**, by the
  two fields of `GeocodedPlace` and the two lines that fill them; everything else in the
  file is still byte-identical and must stay so. `country_code` would help upstream for
  the same reason (its `City.country` is a localized name too), the change is additive,
  and carrying it there restores the identity — recommended, not done. Four tests gained a
  case: `GpsLocationTest`, `CityStoreTest`, `LocationProviderTest`, `SearchLanguageTest`.
- The official warnings' second PR (9 set 2026) touched five shared files, each by a few
  lines and each additive: `NotificationSettings` gained `officialWarnings` and
  `officialWarningsFrom` (with `SettingsStore`'s two keys); `ChiaroDatabase` went 4 → 5 with
  a new table and `MIGRATIONS`, the one list the builder and the tests share
  (`weather_history` untouched, as promised above); `ServiceLocator` hoisted the OkHttp client
  and the database into fields so the warnings source and DAO could share them, and grew four
  accessors; `WeatherSyncWorker` gained one `runCatching` call to `OfficialWarningsStep`,
  placed BEFORE the alerts gate because the bulletin is content for Oggi and the widgets,
  not only a notification; `SyncNotifiers` gained `notifyOfficialWarning`, and so did the
  fake in `WeatherSyncWorkerTest`. `SyncScheduler.alertsWanted` counts the new switch.
  Everything else is new and Chiaro-only: `domain/warnings/{CapDocument, DpcBulletinReader,
  WarningFetchPolicy, WarningDiff}`, `data/warnings/{CapParser, WarningSource,
  DpcBulletinSource, OfficialWarningStore}`, `data/local/WarningRecords`,
  `sync/OfficialWarningsStep`, `notifications/OfficialWarningNotifier`.

## The known debt

**The inherited comments spoke tweather's vocabulary, and each phase rewrote the
ones naming a surface it built**: `CityStore`'s in Fase 3 (first run, the Places
sheet), `SettingsStore`'s and `WorkspaceStore`'s in Fase 4 (Settings, the guide
card), `RuleEngine`'s and `NotificationRule`'s in Fase 6 (the Alerts screen and its
"try it now"). The debt is paid; what remains English in `:core` comments is
engine vocabulary, not surface names.

They were deliberately left alone in Fase 0, and the reason is worth writing down: each
one names a tweather SURFACE, and the honest replacement is the name of the Chiaro
surface that does the same job — which for most of them has not been designed yet.
Rewriting them now would mean inventing vocabulary in a comment instead of in a phase.
**Every phase rewrites the comments in the code it touches**, and the count above is
what "done" is measured against.

## The copy that went the other way (Fase 19)

Everything above flows tweather → Chiaro. **Fase 19 is the first change that flowed
the other way**: nineteen sky jobs, an eclipse engine, the year-events searches and the
rainbow window were written here, in `:core:domain/sky`, and ported to tweather with a
package rename and nothing else — the two packages are byte-identical again, which is
the property the whole ledger exists to keep.

Two consequences worth writing down:

- **The direction is not the point; the identity is.** `UPSTREAM.md` tracks a copy, not
  a parent. A change made once and applied to both is exactly the cheap case; the
  expensive one is a change made twice, differently, which is what the extraction rule
  below is watching for.
- **The surfaces stayed apart, as designed.** The nineteen jobs are the same nineteen in
  both apps and the words for them are not: Chiaro's Cielo screen groups them in a
  catalog sheet and prints the eclipse evidence as a localized sentence, tweather's
  `sky.crontab` prints it as an English readout in the evidence column (its Fase 18
  register rule). One engine, two registers — which is the whole thesis of the fork.

## The prose that travelled, and the surface that did not (5 set 2026)

The guide to the sky events (`ui/sky/SkyGuide.kt` and its `sky_about_*` strings) is the
first thing copied out of tweather that is **not** in `:core`: fifty-one pages written
there as `man 7 <job>` (its Fase 23) and brought here as pages of a Material guide.

It is worth recording precisely because it is not the ledger's usual case. The engines
are copied byte-for-byte and the identity is the property being kept; this is prose,
and it arrived **rewritten**: thirteen of the fifty-one pages spoke about a crontab
line, a job, the file or `--notify`, and every one of those sentences says event, entry,
app and place here, in both languages. So the two copies are deliberately not identical
and never will be — what they share is the content, and a page corrected in one app is
worth carrying to the other by hand, sentence by sentence, not by diff.

What did not travel is the shape: no manual, no shouted section headers, no screen taken
whole. `SkyJob` still generates the "when it happens" section in both, which is the only
part of the page an engine change can invalidate — and the only part neither app writes
by hand.

## The same file, wrong a second time (5 set 2026)

The place name of the device position was wrong in both apps at once — "Provincia di
Monza e della Brianza" for a reader in Cavenago di Brianza, "Milano" for one in Segrate
— and for the same three reasons, because `LocationProvider.kt` was still byte-for-byte
identical. Two of the three were introduced by the position review of 4 set, and one had
been there since the file was written:

- the `Geocoder` was asked about the rounded pair (up to 679 m of displacement, for a
  privacy gain of zero: coarse permission already hands the app a position quantized
  onto a ~2 km grid, so both values name the same cell);
- one address was read out of the ladder the lookup returns, and `subAdminArea` sat in
  the *middle* of the name chain, so a rung with no `locality` printed the province;
- the last-known positions were ranked by recency alone, so a cell fix ten seconds old
  beat a good one from two minutes ago.

The fix is the same in both, and the two decisions were extracted into pure functions
(`geocodedPlace`, `expectedErrorMeters`) so `LocationProviderTest` — identical in both
repositories — can hold them without a device. `PLANNING.md` carries the reasoning in
each repo.

**This is the second time.** The rule below says `weather-core` gets extracted when the
same bug has to be fixed in both apps for the second time, and the position path is now
that file: fixed here on 4 set and carried upstream, wrong again in both on 5 set. The
trigger has fired; what it is waiting on is a decision, not another occurrence.

## The second pass (9 set 2026)

The first full re-read of both trees since the seed, done with `tools/seed_core.py`'s
own rewrite applied to tweather HEAD and the result diffed against `:core` file by
file. Three things came out of it.

**Two fixes the ledger already said belonged upstream were still missing there**,
and are now carried: the nullable day probability (with `FIRST_PRECIP_CODE` moving
into the domain) and `DARK_ALL_DAY`. Every file they touch in `:core` is byte-identical
again; the surfaces downstream of them in tweather (`WeatherReadme`, `WeatherJson`,
`SkyDocument` and its notes) were adapted in tweather's own register. A third one, the
`"null"` in the snapshot, was carried and then **withdrawn**: the comparison had been
made against a tweather `main` three days stale, and the rebase of the tweather PR
found its Fase 28 had already met that word and decided the other way (above).

**The stale base cut the other way too.** Four upstream changes of 6 set were not here
and are now: `Duration.hhMm()` in the domain, `ForecastDiff` without `dayLabel` (with
its test), and in `flatten` the `location` fallback and the daylight key — the parts
of Fase 28 that are facts rather than the diff's register. The lesson is procedural
and is written down where it will be read: fetch and compare with `origin/main` in
both repositories before measuring anything.

**The shared files had drifted in their comments only** — a phase number on one side
(`Fase 20`, `Fase 25`) and another on the other (`Fase 3b`), a surface name here
(`the hero`) and there (`the FAB`). `LocationProvider`, `WeatherFreshness`,
`PowerSaveState` and the shared paragraphs of `WeatherRepository` now carry one
wording in both repositories, dated rather than numbered: the two PLANNING files
number the same work differently and always will, the calendar does not. Comments
that name a surface that exists in one app only (`RuleEngine`'s "try it now",
`NotificationRule`'s card title) stay different on purpose.

**tweather's guards for the near-verbatim ports were not here.** `SkyAlarmScheduler`
and `SkyAlarmReceiver` had no test in Chiaro; `WeatherSyncWorker` had none either.
`SkyAlarmSchedulerTest` is now a straight port into `:app`; `WeatherSyncWorkerTest`
a port into `:core:sync`, with the notifiers and the widgets as fakes behind
`SyncDependencies` and two cases of its own for the failure log and the repaint;
`SkyNotifierTest` keeps upstream's structure and none of its words, since the words
are the one thing the two notifiers do not share — and adds the check that no dotted
id reaches the notification. Two spellings were unified while there: the timezone
fallback (`City.timezone` is nullable in both; tweather passed it to `ZoneId.of`
through the platform type) and the parameter order of `shouldRun`, both now as in
Chiaro.

What still differs after the pass is exactly the list above, each entry with its
reason: 66 of the 86 shared files are byte-identical (55 before the pass), and as of
9 set 2026 nothing is pending in either direction.

## What Chiaro has and tweather does not: the official warnings (planned 9 set 2026)

Fase 11 and 12 add the first capability that did not come from upstream: **official weather
warnings** — the Dipartimento della Protezione Civile's daily bulletins for places in Italy,
MeteoAlarm for the rest of Europe. Nothing in tweather reads an official feed, and it never did
on purpose (VISION §10 had "severe-weather government bulletins" out of scope until 9 set 2026;
the decision and its reasons are VISION §12.8). What the ledger has to say about it, written
before the code exists so the code has something to be measured against:

- **New, not diverged.** `domain/warnings/` (the model, the zone index, the level engine),
  `data/warnings/` (one source per issuer, the CAP pull-parser, the zip reader, the bundled
  zones asset and its importer of record `tools/build_warning_zones.py`), the `warnings`
  DataStore and the `warning_records` Room table. None of it has an upstream twin. The model is
  issuer-agnostic and every surface is in `:app`, so if tweather ever wants warnings the two
  directories move as they are — which is the same property `:core:*` was split for (below).
- **Shared files that will diverge, and how little.** `WeatherSyncWorker` gains one guarded call
  (the step itself lives in its own class); `SyncNotifiers` gains one method, and so does the
  fake in `WeatherSyncWorkerTest`; `ChiaroDatabase` goes 4 → 5 with an **additive** migration —
  a new table, the inherited `weather_history` untouched. The sky-runs argument for a column
  rather than a table (`WeatherHistory.kt`) is about what a *fetch observed*; a bulletin is a
  document with a validity window that outlives any fetch and must be shown on days nothing was
  fetched, which is `ReportDiskCache`'s kind of thing, not `sky_runs`'. `City` gains
  `countryCode` and `admin3`, nullable with defaults, filled on both roads: `GeoResultDto`,
  which already receives `country_code` and drops it (`toCity` keeps the localized name, and
  "Italia"/"Italy" is not a discriminator), and `GeoFix` for the position. Each divergence is a
  few lines; each is registered here the day it lands, with the same dated comment in both
  repositories if tweather takes the fields too — `country_code` would help upstream for the
  same reason.
- **Not shared and not meant to be: the words.** Levels, hazards and their meanings are string
  resources in `:app`; the issuer's own text is quoted in the language it was written
  (VISION §8), so no translation table for it exists in `:core` to keep in step.
- **A debt that travels with the data, measured on 9 set 2026.** The DPC files carry cp1252
  damage in a few names ("Citt� Sant'Angelo") and truncated municipality names in the vigilance
  layer ("Belv", "Bidon", "Buddus"). The zone index therefore matches a place by **geometry
  first** and by municipality name only as a fallback; the importer normalizes names and prints
  the count it could not match, so the debt is re-measured every time the asset is rebuilt.
- **Obligations that travel with the data.** CC BY 4.0 for both DPC repositories — attribution on
  the warning sheet, in the guide and in Informazioni. MeteoAlarm's terms are equivalent to
  CC BY 4.0 plus three requirements: the issuer's name, the time of issue (both of which the
  banner prints anyway) and no modification of the text, which is the quoting rule again. The
  two DPC READMEs still say "repository in fase di caricamento", as they have since 2020: the
  adapter treats a missing or malformed bulletin as "keep the last one and say its date", never
  as an error the reader has to see.

**Landed, first PR (9 set 2026):** `domain/warnings/` (the model, `WarningZoneIndex`,
`OfficialWarningEngine`, two test classes), `tools/build_warning_zones.py`, the asset
`warning_zones_it.json` (284 KB, 187 zones — the plan said 156: the bulletin's own DBF has
187 codes, 187 names and one polygon record each, and PLANNING records the correction),
`data/warnings/WarningZoneAssets` (the one Android line that opens the asset) and the `City`
fields listed above. Two of the debts measured on 9 set turned out smaller on the files this
PR reads: the criticality TopoJSON's 8 182 municipality entries carry **zero** U+FFFD (the
"Citt� Sant'Angelo" damage is in the vigilance layer, which Fase 11's second PR reads), and
the only codepage damage in the zone names is a lost apostrophe in two Valle d'Aosta names,
which the importer restores. One new debt: thirteen zones (Basilicata's seven, Marche's six)
have their code as their only name, so "no zone code reaches the screen" needs a rule for
them before the sheet exists.

**Landed, second PR (9 set 2026):** the source, the store, the table, the step and the
notifier (the shared-file bullet above lists the divergences). Three facts measured that
afternoon corrected the plan's own estimates and are recorded in PLANNING: GitHub's commit
Atom honours `If-None-Match` (a 304 with no body, so the hourly re-check is free); a commit's
`.diff` is ~200 B only for the preview PNGs — a mid-pipeline GeoJSON commit's weighs 5.5 MB,
so the source reads one line and hangs up; and `latest_all.zip` carries the CAP, so the
unmetered fallback works. The first non-Kotlin fixtures sit under
`core/data/src/test/resources/dpc`: the real CAP of 8 set, the real Atom of 9 set, one real
diff. The vigilance layer is deferred to the third PR (it needs a second zone asset and its
only surface is the sheet).

**Fase 12 was prototyped on 11 set 2026 and deliberately not landed**, so nothing in this
ledger changed for it. Two debts it measured are worth keeping here anyway, because they
belong to the data and will be found again by whoever reopens the phase:

- **MeteoAlarm publishes no geometry with its warnings.** Seven producers sampled, zero
  `<polygon>` and zero `<circle>` in any CAP; the areas are named and coded only. The shapes
  exist in a separate published file (`meteoalarm-pm-group/documents`, versioned by date), and
  its own outlines put **Helsinki inside "Western Gulf of Finland" and inside no Finnish land
  area at all** — so a marine block has to be excluded before anything is indexed.
- **`cap:event` is not a vocabulary**: sixty spellings across twenty-two producers, including
  one that prints its own template (`awareness_type=5, awareness_level=2`). Level and hazard
  can only come from the CAP's coded `awareness_level` / `awareness_type`.

The prototype also found a gap in **Fase 11's own shipped code**, which is in this repository:
`OfficialWarningsStep` runs only from `WeatherSyncWorker`, so nothing fetches a bulletin when
the app is opened or pulled to refresh — the behaviour the phase's plan specified and never
wired. It is recorded in `PLANNING.md` under Fase 11.

## When to extract

The rule from VISION.md §7.3: copy now, extract `weather-core` into its own repo when
the same bug has to be fixed in both apps for the second time. The `:core:*` split
exists from day one precisely so that extraction never requires moving code between
packages — only moving directories between repositories.
