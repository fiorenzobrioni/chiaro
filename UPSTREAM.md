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
  Saturday on disk. `ForecastDiff` is per-date and unchanged; its `dayLabel` now
  derives from date distance instead of list position (same output on two dates,
  correct on seven). If tweather ever widens its Logs, this belongs upstream too.
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
  That leak was still live upstream on 9 set 2026 — its widget reads the key and
  would have printed `Rain null%` — and was carried there the same day, with a test
  in both `WeatherSnapshotsTest`s. The two Chiaro-only keys stay here.
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
  needs its day on disk. Chiaro-only, like the seven-day horizon before it. One inherited
  seam to know about: `ForecastDiff.dayLabel` still calls the earliest stored date
  "tomorrow"; that label is upstream's Logs vocabulary and nothing in Chiaro reads it,
  so it was left as it is rather than re-taught for a screen that does not exist here.
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

**Three fixes the ledger already said belonged upstream were still missing there**,
and are now carried: the nullable day probability (with `FIRST_PRECIP_CODE` moving
into the domain), the `"null"` written into every commit's `current.precip_chance_pct`
(live in tweather's widget), and `DARK_ALL_DAY`. Every file they touch in `:core` is
byte-identical again; the surfaces downstream of them in tweather (`WeatherReadme`,
`WeatherJson`, `SkyDocument` and its notes) were adapted in tweather's own register.

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
reason. The one direction with nothing pending is tweather → Chiaro: every commit
upstream since the seed that touched its domain or data has its twin here.

## When to extract

The rule from VISION.md §7.3: copy now, extract `weather-core` into its own repo when
the same bug has to be fixed in both apps for the second time. The `:core:*` split
exists from day one precisely so that extraction never requires moving code between
packages — only moving directories between repositories.
