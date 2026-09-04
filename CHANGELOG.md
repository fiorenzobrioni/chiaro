# Changelog

All notable changes to Chiaro are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versions follow
[Semantic Versioning](https://semver.org/).

`release.yml` reads the section whose heading matches the tag being pushed and uses it
as the body of the GitHub Release, so a version's entry is written **before** its tag.

## [Unreleased]

Nothing released yet. The engines are in and verified, the design system is code, and
the everyday surface — Today, places, first run, settings, the guide, the Sky screen
with its reminders, the alerts, the Journal, the home widgets — is built. See `PLANNING.md` for where the work actually is.

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
- The guide (Fase 4, rewritten in the widget-and-guide pass): a tour of the four
  screens in both languages — what each one answers, what it can do, and the things a
  screen cannot say out loud (the sky is computed rather than photographed, a reminder
  is loose on purpose, a failed update is a line in the Journal), followed by where the
  numbers come from. It teaches with the app's own components shown as examples —
  verdict chips, a details tile, the freshness chip, a miniature drift strip, each with
  a caption saying it is an example. Reachable from Settings forever and pointed at once
  by a dismissable card on Today.
- The Sky screen (Fase 5): tonight's verdict on the dark window with the numbers that
  decided it (and the moon named when it was the moon), the subscribed moments ahead
  resolved in the city's own timezone — a moment that is over is replaced by its next
  occurrence, marked "Tomorrow", and a window in progress says "Now" — the calendar
  ahead (meteor peaks, the next full moon, solstices and equinoxes, with an honest
  "too far out to say" past the forecast's horizon), and the grouped 32-moment catalog
  where each entry teaches what it is in one line.
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
- The Journal (Fase 7): the history table read as prose, newest first, grouped by
  day — forecast revisions with their numbers ("Saturday improved: rain 70% → 30%"),
  fired alerts, the sky moments the app observed (with the verdict, or an honest "no
  update came close enough"), and the updates that failed, each with its reason. The
  bottom bar reaches its four destinations.
- The forecast drift strip (Fase 7): one row per target day, one column per fetch,
  color on the metric's own ramp (rain, or the highs on the diverging temperature
  ramp), legend always present, the judgement in a sentence beside it and the raw
  numbers behind a long press. Each commit now stores the week ahead, so the strip
  is built entirely from data already on disk.
- "What changed" on Today (Fase 7): up to three sentences after the day's timeline
  when the latest update moved the week, tapping opens the Journal.
- The home widgets (Fase 8), in Glance: Now (icon, temperature, place), Today (now
  plus the day's sentence and the next hours) and Sky (the followed moment in front of
  you and its verdict, with the day named when it is not today — the widget nobody
  else ships). The weather icon grows to fill the height the launcher grants. They draw
  from the same builders the app reads — the Sky widget and the Sky screen resolve
  their moment through the same rule, so the two cannot print two different sunrises —
  repaint on every data commit, follow the system's light/dark with the app's own color
  scheme (dynamic or Chiaro), state their age when stale, and say "no place yet"
  instead of ever showing a number they do not have. A placed widget keeps the shared
  periodic job alive on its own.
- The Widgets group in Settings (Fase 8): the card's background opacity, applied to
  the fill only — the text always keeps full ink.
- Two weather-icon themes: Meteocons' fill set joins the line set (same glyphs, solid
  shapes), chosen in Settings → Appearance, filled by default. Both sets are
  re-anchored to clear the measured 3:1 contrast floor on both surfaces, and
  `IconContrastTest` now sweeps both.
- The launcher mark, replacing the Fase 0 placeholder: the icon family's starry-night
  crescent in Chiaro's own palette, low in the badge over two calm waves.

### Fixed

Four things the device found (committente, 4 set), all of them behaviour rather than
polish:

- **"My position" now follows the reader.** The fix behind the device-position page was
  only ever re-taken when the source was enabled or its row tapped in the Places sheet,
  so someone who drove to the next town and pulled to refresh got fresh numbers for the
  town they had left. A pull on that page now takes the position first and the weather
  after, and swiping onto the page re-takes it too (throttled to one fix every five
  minutes, so a swipe back and forth is not a stream of them). A failed fix stays silent
  and keeps the last position: old place with real weather beats an error over numbers
  that are still true. Background location remains off the table.
- **The widgets repaint when the data does.** Glance runs `provideGlance` once per
  session and keeps the composition alive for about forty-five seconds afterwards;
  inside that window `update()` wakes the composition without running the function
  again, so a widget that loaded its model before `provideContent` repainted its own old
  numbers. A manual refresh, the morning sync and a just-confirmed widget setting all
  landed in that window — which is also why the setting case looked intermittent. The
  model is now read from inside the composition and re-read whenever anything the widget
  draws changes.
- **A see-through widget over a dark wallpaper is legible again.** Under half solidity
  the card is not the ink's ground, so the ink asked the wallpaper — and fell back to the
  phone's THEME whenever the wallpaper gave no answer. A light theme over a black
  wallpaper then wrote black on black (the same phone in dark mode was fine). The
  wallpaper hint is an affirmative signal and is read as one: dark ink only where the
  system says the ground is bright. Picking a light or a dark card now also names the ink
  at any solidity, so there is a way out; `WidgetInkTest` holds the whole table.
- **A pulled-open notification now says more than the collapsed one.** Every
  notification used the same text for both states, so expanding one gave back exactly
  what it already said. Collapsed stays the sentence — the system gives it one line and
  cuts the rest — and expanded keeps that sentence as the headline with the rest of the
  story under it, one fact per line, each with what to do about it: for a storm or a
  rain warning the window the weather really covers, its worst hour and by how much, the
  temperature across it, and the current reading; for the morning summary the day's
  sunrise and sunset, its peak UV, the wind and the air, each with its own consequence
  line; for one of the reader's own alerts the arithmetic that fired it, condition by
  condition with the value read; for a sky reminder the moment's own explanation under
  the lead and the verdict. Nothing in the block is invented: a window that runs past the
  end of the forecast says "from 17:00" and stops, a line whose data is missing is not
  drawn, and the wind says "right now" in words because the hourly forecast carries none.

- **The Sky widget shows as many moments as it has room for.** It printed exactly one at
  every size, and could not have done otherwise: it was the only one of the three left on
  Glance's default sizing, so it was told the provider's minimum size and never learned it
  had been made bigger. Measured now: one cell is the moment and its verdict exactly as
  before, and every cell after that adds compact rows — glyph, name, when, and the
  verdict's word in the verdict's own color — off the same ordered list the Sky screen
  reads. Four subscriptions draw four rows on a widget with room for six: the list is
  never padded out, and the block sits in the middle of the space it does not fill
  rather than clinging to the top edge. Every verdict on a row wears the same chip the
  hero's pill is made of — a bare colored word was hard to read on a dark card, because
  the app's verdict inks are measured against the app's SURFACE and a widget's ground is
  a scrimmed sky or somebody's wallpaper; ink and container are a measured pair, so a
  chip carries its own ground with it.
- **The Now widget can show the sky's state beside the temperature**, off by default and
  switched on per widget in its own settings rather than appearing and vanishing as the
  widget is resized. The standard layout is untouched. The words are set at three fifths
  of the hero number and given the card's own 12dp of air, optically centred against it:
  small and close, they read as something stuck to the degree sign rather than said with
  it.
- **The five starting-point alerts are named with a capital** ("Bike", not "bike"). The
  lowercase came from tweather, where a rule lives in a configuration file and a lowercase
  identifier is the code register — the one register this product deliberately does not
  have. Only the seed changed: a rule already saved keeps the name its reader gave it.

- **The day's high and low are back on the Now and Today widgets**, against the trailing
  edge and level with the temperature rather than under it: the position was what made
  the pair read as clutter in the first place, not the pair. High first and in the strong
  ink, low after it and dimmed — the same emphasis the week's own rows use, so it says
  which is which without a word for it. On by default, switchable per widget.
- **A dry run draws no rain sparkline.** With every hour at 0% the chart was a flat line
  along the bottom of a 28dp box: on the screen it read as a stray divider with a hole
  above it, and it said nothing the row of "0%" over it had not already said.
- **One header rhythm across all five screens.** A section header sat 20dp under the block
  above it on four screens and 12 on Today, where the list's own gap made up the
  difference; a group header sat at 16 in two places and 20 in a third. The three numbers
  now live in one place with the arithmetic written down, and every screen lands on the
  24dp between sections the design document always asked for.

- **The sky canvas ends on a straight line.** Its two bottom corners carried a 28dp round
  that read as a card floating over the scroll rather than as the sky the screen opens
  on.

### Removed

- The last of the editor's vocabulary in the data layer (Fase 4): line numbers, word
  wrap, the technical-details toggle, the theme-profile name and the editor tab state.
  Chiaro's settings hold what Chiaro's screens actually edit.
- The guide's chapter on why there is no radar map: a guide is where a product says
  what it does, not where it defends what it is not. The useful half of it survives
  inside the tour of Today, where the answer to "is it about to rain?" actually lives.
- The day's low and high from the Now and Today widgets: beside a 34sp number the pair
  read as clutter on a home screen, and VISION §5.9 asks those cards for icon,
  temperature and place.
