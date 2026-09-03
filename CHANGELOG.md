# Changelog

All notable changes to Chiaro are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versions follow
[Semantic Versioning](https://semver.org/).

`release.yml` reads the section whose heading matches the tag being pushed and uses it
as the body of the GitHub Release, so a version's entry is written **before** its tag.

## [Unreleased]

Nothing released yet. The engines are in and verified, the design system is code, and
the everyday surface — Today, places, first run, settings, the guide, the Sky screen
with its reminders, the alerts — is built. See `PLANNING.md` for where the work actually is.

### Added

- The project: Gradle skeleton with `:app`, `:core:domain` (pure Kotlin/JVM) and
  `:core:data`, CI that runs every module's tests and lint before any APK, the shared
  debug keystore and the release signing config behind the four `CHIARO_KEYSTORE*`
  properties.
- The engines, copied from tweather with their test suite: the Open-Meteo client and
  mapper, the Room history, the alert engine, the rules engine and the whole astronomy
  module. `UPSTREAM.md` records the commit they came from and how to reproduce the copy.
- The design system in Compose: a generated Material color scheme from three source hues,
  the semantic tokens Material has no slot for (verdicts, a rain ramp, a diverging
  temperature ramp anchored at 15 °C), the computed sky canvas and its scrim contract,
  the daylight ribbon, Inter as a bundled variable font, and the first components.
- Four tests that hold the design document to the code: `PaletteContrastTest`,
  `ScrimContractTest`, `SkyPaletteTest`, `NoRawColorTest`.
- The Today screen: the computed sky canvas over the active place, the headline sentence
  (built on the alert engine's own thresholds, and absent when there is nothing to say),
  the next 24 hours with a rain sparkline, the merged rest-of-day timeline (sun, moon and
  rain turns), the week on one shared temperature scale with each day's ribbon of light,
  and a details grid where every number carries its one-line meaning. Cached content
  renders before any network is asked; stale data states its age; a report past its
  horizon is an empty state, never an old screen posing as current.
- The weather icons: Meteocons v2.0.0 (MIT), imported as vector drawables by
  `tools/import_meteocons.py` with the palette re-anchored so every stroke clears 3:1 on
  both surfaces — measured in the tool, re-measured by `IconContrastTest` on every build.
  `material-icons-extended` is gone (the debug APK drops from 64 to 33 MB).
- The minimal Places sheet: search-as-you-type against Open-Meteo geocoding, tap to add
  and select, the saved list to switch — the piece of Fase 3 a cityless fresh install
  cannot exist without.
- Places in full (Fase 3): a horizontal pager between saved places where settling on a
  page is selecting it, the complete sheet (GPS row pinned on top with its own state,
  cached temperature beside each saved place, recent searches, long-press reorder,
  swipe-to-remove with an undo that restores position and selection), the GPS flow with
  its errors in words, and the one-screen first run that lands "Not now" on the real
  no-place state.
- Settings (Fase 4): units, appearance (theme and dynamic color), update frequency,
  the system per-app language picker, about, and a reset that says exactly what it
  restores and what it leaves alone. Groups arrive with the feature they control, so
  notifications and widgets join in their own phases.
- The guide (Fase 4): where the data is born, what the sky verdicts say, why there is
  no radar map — short prose in both languages, illustrated with the app's own
  components, reachable from Settings forever and pointed at once by a dismissable
  card on Today.
- The Sky screen (Fase 5): tonight's verdict on the dark window with the numbers that
  decided it (and the moon named when it was the moon), the day's subscribed moments
  resolved in the city's own timezone, the calendar ahead (meteor peaks, the next full
  moon, solstices and equinoxes, with an honest "too far out to say" past the
  forecast's horizon), and the grouped 32-moment catalog where each entry teaches what
  it is in one line.
- Sky reminders (Fase 5): a bell per moment plus a default lead, delivered by a single
  deliberately inexact alarm (15-minute floor, no exact-alarm permission), suppressed
  when the sky will hide the event unless asked otherwise, re-armed on boot and on
  every edit. The notification speaks the reader's language and carries the verdict
  with its number.
- The bottom navigation (Fase 5): Today and Sky; the remaining tabs arrive with their
  screens.
- The Alerts screen (Fase 6): the three ready-made alerts as switches that say exactly
  what they send and when; the reader's own rules as cards with their sentence in
  plain words, their state and when they last fired; five templates that create real
  rules already switched on; and a builder that is a sentence of tappable chips — what
  to watch, how to compare, the threshold on a slider — with an optional second
  condition, the reader's own message, and a "try it now" that says what the rule
  would do without posting anything.
- `:core:sync` (Fase 6): the single periodic background job shared by the fetch, the
  built-in alerts, the rules and the sky observation, with desired-state scheduling
  that cancels itself when nothing is left to serve. The notifiers stay in the app
  behind an interface, and they speak prose: a severe-weather heads-up reads
  "Temporale verso le 18:00", never a data dump.
- The guide's fourth chapter (Fase 6): how the alerts work, landed together with the
  screen it describes.

### Removed

- The last of the editor's vocabulary in the data layer (Fase 4): line numbers, word
  wrap, the technical-details toggle, the theme-profile name and the editor tab state.
  Chiaro's settings hold what Chiaro's screens actually edit.
