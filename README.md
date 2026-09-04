<div align="center">

# 🌤️ Chiaro

**Weather that tells you what to do about it, and never invents anything to say it.**

An Android weather app for everybody, with a planner for the sky attached.
Free, no account, no ads, no tracking, no API key.

![Platform](https://img.shields.io/badge/platform-Android-2E6B3E?labelColor=FCFAF6)
![Status](https://img.shields.io/badge/status-Fase%209%20of%2010-8C857A?labelColor=FCFAF6)
![License](https://img.shields.io/badge/license-GPL--3.0-007DB6?labelColor=FCFAF6)
![minSdk](https://img.shields.io/badge/minSdk-33-70569C?labelColor=FCFAF6)
![Kotlin](https://img.shields.io/badge/Kotlin-2.2-F1A000?labelColor=FCFAF6)
![Compose](https://img.shields.io/badge/UI-Compose%20Material%203-007DB6?labelColor=FCFAF6)
![API key](https://img.shields.io/badge/API%20key-none%20needed-2E6B3E?labelColor=FCFAF6)

</div>

## What Chiaro is

Most weather apps answer "how many degrees". Chiaro answers the question people actually
have: is it worth going outside, and when. It opens on a computed sky and one sentence
("Umbrella around 17:00, clearing after 19:00"), and the numbers are there underneath for
whoever wants them.

Three things it does that a weather app normally does not. **The sky has an agenda**:
sunrise, the golden hour, the blue hour, the genuinely dark window, the moon, the meteor
peaks, each with the time it happens and whether the sky will let you see it, computed
from the same cloud forecast the app already downloaded. **It remembers the forecast**:
Saturday used to be 70% rain and is now 30%, and the movement is often more useful than
either number on its own. **You write the alerts**: start from an idea ("tell me when I
can ride") or build one out of real variables, and get told the thing you actually care
about.

It works offline with the last data it managed to fetch, and it says how old that data is
instead of pretending. Nothing on the screen is there because a layout needed filling.

## Features

- 🌤️ **Today**: one vertical scroll that starts with the sky. The **canvas** (a gradient
  computed from the real position of the sun, the cloud cover and the moon) carries the
  place, the temperature, the condition and the **headline sentence**, which is absent
  when there is nothing worth saying because quiet is an answer too. Under it: the next 24
  hours with a rain sparkline (not drawn on a dry day, because a chart of zeroes says
  nothing), the **rest of the day** as one merged timeline of sun, moon and weather turns,
  **what changed** when the last update moved the week, the seven days on one shared
  temperature scale with each day's ribbon of light, and a details grid where every number
  carries its meaning: UV 8 is "burns in about 15 minutes, cover up", not an 8
- 🌅 **Sky**: tonight's verdict on the dark window (**Great**, **So-so**, **No chance**,
  **Not sure yet**) with the numbers that decided it, and the moon named when the moon was
  the reason. The **moments ahead** are an agenda rather than a log: resolved in the
  place's own timezone, a moment that is over is replaced by its next occurrence and says
  "Tomorrow", a window in progress says "Now". Then the calendar ahead (meteor peaks, the
  next full moon, solstices and equinoxes) with an honest "too far out to say" past the
  forecast's horizon, and a catalog of 32 moments grouped by Sun, Night, Moon, Seasons and
  Meteor showers, each teaching what it is in one line. This is where a person finds out
  what a blue hour is, by adding one. All of it is computed on the device and works with
  no network at all
- 🔔 **Sky reminders**: a bell on any moment, plus a default lead. Delivered by a single
  deliberately inexact alarm (15 minute floor, no exact-alarm permission asked), re-armed
  on boot and after every edit, and suppressed when the sky is going to hide the event
  unless you ask for it anyway. The notification speaks your language and carries the
  verdict with the number behind it
- ⚠️ **Alerts**: three ready-made switches that say exactly what they send and when
  (severe weather in the next 12 hours, at most once per storm; rain past 70% likely
  within 6 hours, at most twice a day; the morning summary, once between 6 and 12). Then
  your own: five templates that create a real rule already switched on, and a builder that
  is a sentence of tappable chips (*when* **rain in the next 6 hours** *is* **above**
  **70%**), with an optional second condition and your own message. Values are picked and
  never typed, so an alert cannot be written wrong. **Try it now** runs the rule against
  the forecast already on the phone and says what it would have done, with nothing posted
  and nothing recorded. Your message is your content: it is never rewritten and never
  translated
- 📓 **Journal**: every update is already a row in the database, and the Journal is that
  table read as prose, newest first, grouped by day. Forecast revisions with their numbers
  ("Saturday improved: rain 70% → 30%"), alerts that fired, the sky moments the app
  observed (with the verdict, or an honest "no update came close enough"), and the updates
  that failed, each with its reason in plain language. Underneath, the **forecast drift
  strip**: one row per target day, one column per fetch, color on the metric's own ramp,
  the judgement written out beside it and the raw numbers behind a long press. Built
  entirely from data already on disk
- 📍 **Places**: a horizontal pager between saved places, where settling on a page is
  selecting it. The sheet has the GPS row pinned on top with its own state, the cached
  temperature beside each saved place, search as you type, recent searches, long-press
  reorder and swipe to remove with an undo that restores both position and selection.
  Location is coarse only and optional: the device position is re-taken when you pull to
  refresh or swipe onto its page (throttled to one fix every five minutes), never in the
  background, and a failed fix keeps the last position rather than replacing real numbers
  with an error
- 🏠 **Widgets**, in Glance: **Now** (icon, temperature, place, with the weather glyph
  growing into the height the launcher grants), **Today** (now plus the day's sentence and
  the next hours) and **Sky** (the moments in front of you and their verdicts, as many
  rows as honestly fit and never more than you subscribed to), which is the widget nobody
  else ships. Each one is configured on its own: which place, the background (the sky
  itself, light, dark or follow the system), its opacity, and what the card carries. They
  read the same builders the app reads, so the home screen and the app cannot print two
  different sunrises; they repaint on every data commit, state their age when stale, and
  say "no place yet" instead of showing a number they do not have
- 🚀 **First run**: one screen and two answers, use my position or search for a place. No
  carousel, no account, and no permission asked before the sentence explaining why. Skip
  is allowed and lands on a real "no place yet" state rather than a city you never chose
- 📖 **The guide**: a tour of the four screens in both languages. What each one answers,
  what it can do, and the things a screen cannot say out loud (that the sky is computed
  rather than photographed, that a reminder is loose on purpose, that a failed update is a
  line in the Journal), closing on where the numbers come from. It teaches with the app's
  own components shown as examples, each captioned as one, and it never teaches a control:
  a control that needs explaining is a bug in this edition. Reachable from Settings
  forever, pointed at once by a dismissable card on Today
- 🎨 **Appearance**: dynamic color from the wallpaper, or Chiaro's own generated scheme as
  the fallback, in light, dark or whatever the system is doing. Two weather-icon themes,
  fill and line, both re-anchored to clear a measured 3:1 against the surface they are
  drawn on
- ⚙️ **Settings**: units (temperature, wind), appearance, update frequency (15, 30, 60 or
  120 minutes, 60 by default), the system per-app language picker, the data source and the
  privacy position, and a reset that says exactly what it restores and what it leaves
  alone
- 📴 **Offline, honestly**: the last successful report per place is kept with no TTL and
  carries a week of forecast, so the app is never blank and yesterday's fetch still holds
  today. The hours that have already happened are dropped first, stale data states its
  real age, and a report past its horizon becomes an empty state instead of an old screen
  posing as current. There is no full-screen spinner in this product: cached content
  first, freshness stated, refresh silent
- 🔋 **One job for everything**: a single periodic WorkManager task carries the fetch, the
  built-in alerts, your rules and the sky observation, and cancels itself when there is
  nothing left to serve. Inexact alarms for reminders, no foreground service, no
  background location, no push service. A widget on the home screen keeps the job alive on
  its own
- 🇮🇹 🇬🇧 **Italian and English** through the system per-app language picker. Everything
  Chiaro renders is prose or data, so everything localizes, notifications and widgets
  included. The dotted identifiers behind the engines (`golden_hour.pm`, `current.temp_c`)
  stay in the code and never reach a screen

### Roadmap

Fase 0 to 8 are done and on device: the engines and their test suite, the design system in
code, Today, Places and first run, Settings and the guide, Sky with its reminders, Alerts
with `:core:sync`, the Journal with the drift strip, and the three widgets. **Fase 9** is
the accessibility and performance pass with numbers attached: the color pass and the
weather-icon pass are done, the contrast and 200% text and TalkBack sweep, cold start under
400 ms, and the full IT/EN reading are not. **Fase 10** is the store: final icon,
screenshots, listing, and v1.0.0.

Deliberately out of scope for v1: radar and satellite imagery (the provider has none, and
that is a stated position rather than a gap to hide), government severe-weather bulletins,
tides, aurora, air-quality forecasting beyond the current index, Wear OS, sharing, and a
second provider. The phased plan, with every decision and every deviation and its reason,
is in [PLANNING.md](./PLANNING.md).

## Install

There is no published release yet: the first tag will be `v1.0.0`, at the end of Fase 10.
Until then the debug APK of every green build is attached to its run under
[Actions](https://github.com/fiorenzobrioni/chiaro/actions), and building from source is
the three commands below. Android 13 (API 33) or newer.

Releases, when they start, are built by `.github/workflows/release.yml` on a `v*` tag:
tests and lint first, then the minified APK signed with the real key, published together
with its R8 mapping and with the matching [CHANGELOG.md](./CHANGELOG.md) section as the
body.

## Principles

| | |
|---|---|
| 🖥️ **The screen must not lie** | a section with no data is not drawn, never a card with a dash in it. Stale data states its age, estimates say so, and a skeleton looks like a skeleton and never like a grey zero |
| 💬 **One sentence before any number** | the top of Today is a computed line. Numbers follow, for the people who want them |
| 🔢 **Every number says what to do with it** | a metric is a value plus its consequence. A number with no honest second line lives one tap deeper, not on the home screen |
| 📴 **Offline-first** | the cached report renders before the network is asked; no core answer needs a connection, and the astronomy needs one at no point at all |
| 🔒 **Privacy-first** | no account, no analytics, no advertising id, no crash reporting that leaves the device. Coarse location only, optional, and never in the background |
| 🔋 **Battery is a feature** | one shared periodic job, inexact alarms, no foreground service and no push service. It is a constraint from the first commit, not an optimization phase |

## The data

[Open-Meteo](https://open-meteo.com/): free, **no API key, no account**.

| What | Source |
|---|---|
| Current conditions, hourly, daily | Forecast API |
| Air quality, pollutants, pollen *(Europe only)* | Air Quality API |
| Place search | Geocoding API |
| Sun, moon, twilight, meteor peaks, the verdicts | computed on the device by `:core:domain`, offline |

One fetch per active place per interval, behind a 15 minute cache and constrained to a
live connection. Nothing polls, and no request carries anything about you.

## Where it comes from

Chiaro is the daylight edition of
[tweather](https://github.com/fiorenzobrioni/tweather), a weather app whose entire
interface is a code editor: the forecast as syntax-highlighted JSON, the settings as a
config file you edit by tapping values, the update history as a git diff. tweather is
furniture for developers and was built that way on purpose.

Underneath that interface sits a layer with no opinion about looking like an editor: the
Open-Meteo client and mapper, a full solar and lunar ephemeris, the alert engine, the
rules engine, the Room history. Chiaro takes that layer as it stands, with its tests, and
puts a Material 3 product on top of it. The two apps ship side by side and neither
replaces the other. [UPSTREAM.md](./UPSTREAM.md) records the exact commit the core came
from, how to reproduce the copy with `tools/seed_core.py`, and the debt the seed
deliberately left behind.

## Design

Material 3 committed to rather than defaulted to: a generated color scheme (dynamic color
from the wallpaper, with the app's own three source hues as the fallback), light and dark,
the expressive type and shape scales, spring motion, and Inter bundled as a variable font.

Two elements are Chiaro's own, and both are computed rather than decorative. **The sky
canvas** is drawn from the real position of the sun, the cloud cover and the moon, so it
cannot disagree with the forecast below it: it comes off the same engine. **The daylight
ribbon** is a thin band of night, twilight, the golden hours and daylight, used on the
canvas and on every row of the week.

The rules that get broken by accident are enforced by tests rather than by good
intentions: no composable outside `ui/theme/` names a color literal (`NoRawColorTest`),
the palette and the canvas scrim hold their measured contrast ratios
(`PaletteContrastTest`, `ScrimContractTest`, `SkyPaletteTest`), and every weather icon in
either theme clears 3:1 against the ground it is drawn on (`IconContrastTest`). A verdict is a glyph and a
word before it is a color, because green, amber and red do not separate under
deuteranopia. Every value, with the number that was measured for it, is in
[DESIGN.md](./DESIGN.md).

## Tech stack

- **Kotlin** 2.2, **Jetpack Compose** with Material 3, Gradle 9.1 and AGP 8.13, minSdk
  **33** (Android 13), target and compile SDK **36**
- **Retrofit** + **OkHttp** + **kotlinx.serialization** (Open-Meteo), **Room** (the update
  history), **DataStore** (settings, places, rules, subscriptions and engine state),
  **WorkManager** (the one periodic job), **Glance** (the widgets)
- **Coroutines** and **Flow** throughout, a hand-rolled `ServiceLocator` instead of a DI
  framework (the app is small enough that Hilt would cost more than it saves), the four
  destinations as a saveable enum in the shell rather than a nav graph, and the charts
  drawn on a Compose canvas rather than by a charting library
- **Meteocons** v2.0.0 as vector drawables, imported and recolored by
  `tools/import_meteocons.py`; **Inter** as a bundled variable font
- 362 unit tests on the JVM (Robolectric where Android is unavoidable), across four
  modules

```text
Compose UI → ViewModel → :core:data (repository, stores) → Open-Meteo · Room · DataStore
                      ↘ :core:domain (pure engines: alerts, rules, astronomy)
```

## Build

Requires JDK 21. No signing setup, no API key, no local properties: clone and build.

```bash
./gradlew :app:assembleDebug          # app/build/outputs/apk/debug/app-debug.apk
./gradlew test :app:testDebugUnitTest # every module's tests
./gradlew :app:lintDebug              # lint
```

The release build is minified by R8 and **unsigned by default**. For an installable one to
test with:

```bash
./gradlew :app:assembleRelease -PsignReleaseWithDebugKey
```

That flag signs it with the debug keystore committed in `keystore/`, which is in the repo
on purpose so builds from CI and from any machine share one signature. It is not a release
key, and the flag stays opt-in precisely so an unconfigured checkout can never produce an
installable release by accident. Debug builds carry `applicationIdSuffix ".debug"` and
install side by side with the release-signed app.

CI runs the tests and lint **before** the builds, so a red suite never produces an
artifact anyone could install and trust. Every push uploads the debug APK, the release APK
and the R8 mapping.

## Project structure

```text
chiaro/
├── app/                              # everything visible
│   └── src/main/kotlin/com/callbackdev/chiaro/
│       ├── MainActivity.kt           # the single activity: edge to edge, theme, shell
│       ├── ChiaroApplication.kt      # process start: service locator, sync scheduling
│       ├── notifications/            # alert, rule and sky notifiers; the reminder alarms
│       ├── widget/                   # Glance: Now, Today, Sky, per-widget configuration
│       └── ui/
│           ├── theme/                # generated scheme, sky palette, type, shape, motion
│           ├── components/           # sky canvas, daylight ribbon, verdict chip, tiles, charts
│           ├── shell/                # scaffold and bottom navigation
│           ├── today/                # Today: canvas, headline engine, hours, week, details
│           ├── sky/                  # Sky: tonight, moments, catalog, reminders
│           ├── alerts/               # Alerts: ready-made switches, rule cards, chip builder
│           ├── journal/              # Journal: the history as prose, the drift strip
│           ├── places/               # places sheet: search, GPS row, saved list
│           ├── firstrun/             # one screen: position, search, skip
│           ├── guide/                # the guide
│           ├── settings/             # preferences and reset
│           ├── icons/                # the weather icon family, chosen by ground
│           └── format/               # units, times and numbers as the reader sees them
├── core/
│   ├── domain/                       # pure Kotlin/JVM, no Android at all
│   │   ├── AlertEngine.kt            # the three built-in alerts
│   │   ├── rules/                    # the rules engine: variables, evaluation, messages
│   │   ├── sky/                      # astronomy: Meeus series, catalog, verdicts, reminders
│   │   ├── model/                    # the weather report as the app reads it
│   │   └── settings/                 # the settings shape, with no Android in it
│   ├── data/                         # the Android library
│   │   ├── remote/                   # Open-Meteo APIs and their DTOs
│   │   ├── mapper/                   # DTO to domain report
│   │   ├── local/                    # Room history, snapshot and forecast diffs, disk cache
│   │   └── *Store.kt                 # DataStore: settings, places, rules, subscriptions, state
│   └── sync/                         # the one periodic job, and its desired-state scheduling
├── tools/                            # the seed script, the icon import, the palette generators
├── licenses/                         # what ships inside the APK and is not ours
└── keystore/                         # the shared debug key (deliberately committed)
```

`:core:domain` is pure Kotlin and stays that way: a class in it that needs a `Context` is
in the wrong module.

## Project documentation

| File | Contents |
|---|---|
| [VISION.md](./VISION.md) | the product: positioning, identity, design language, every screen, the parity map with tweather, the roadmap, the open decisions |
| [DESIGN.md](./DESIGN.md) | the design system: color, the sky canvas, the daylight ribbon, type, shape, motion, the component kit, the chart rules, accessibility, each value with its measured number |
| [PLANNING.md](./PLANNING.md) | the phased plan with checkable steps, and the honest account of where the work actually is |
| [UPSTREAM.md](./UPSTREAM.md) | where `:core` came from, how to reproduce the seed, and the debt it left behind |
| [CHANGELOG.md](./CHANGELOG.md) | what shipped, per version; a section is written before its tag |
| [CLAUDE.md](./CLAUDE.md) | the operating rules for AI-assisted development in this repo |

## License

[GPL-3.0](./LICENSE) © 2026 Fiorenzo Brioni

Weather data by [Open-Meteo](https://open-meteo.com/) (CC BY 4.0).
[Inter](https://github.com/rsms/inter) under the SIL Open Font License 1.1,
[Meteocons](https://github.com/basmilius/meteocons) under MIT. Full attributions in
[licenses/](./licenses/).
