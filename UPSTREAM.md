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
    exists, bounds the last-known fallback by age, and rounds the coordinates before
    handing them to the `Geocoder`. All three were bugs upstream too, on the byte-for-byte
    identical file — so the fix is being carried back rather than kept here. Keep the two
    copies in step.
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
- `FetchLogStore` is new and Chiaro-only (Fase 7): a bounded ring of failed fetches
  (when, which place, why) so the Journal can say "an update didn't make it" —
  offline honesty is a Chiaro surface; upstream's Logs render commits, and a commit
  that never happened has nothing to render there.

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

## When to extract

The rule from VISION.md §7.3: copy now, extract `weather-core` into its own repo when
the same bug has to be fixed in both apps for the second time. The `:core:*` split
exists from day one precisely so that extraction never requires moving code between
packages — only moving directories between repositories.
