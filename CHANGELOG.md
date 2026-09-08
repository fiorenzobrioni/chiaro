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
- The weather icons move. Meteocons draws its family with SMIL animations and the
  import had been dropping them; `tools/import_meteocons.py` now carries them across as
  AnimatedVectorDrawables, so the sun turns, the cloud banks drift, the rain falls out of
  step with itself and the lightning flickers — the illustrator's own motion, not a
  rewrite of it. The condition family only: a barometer that spins forever is decoration.
  On by default with a switch in Settings → Appearance, and always still when the phone
  asks for less animation, which is the same setting every other animation in the app
  reads. Not in the widgets, where `RemoteViews` cannot run one at all.
- New defaults for a fresh install: the Vivid palette, wallpaper colors off, outlined
  icons, animated icons on; and for a newly placed widget, a solid card with the day's
  range off. The theme keeps following the phone. The app now opens looking like itself
  rather than like the wallpaper, which also resolves the design document's open question
  about store screenshots.
- A second palette, chosen in Settings → Appearance: **Paper**, the warm identity, and
  **Vivid**, the same app at the brightest colors a screen holds — a cool white, an azure
  accent, saturated quantity ramps, a saturated sky and brighter weather icons on dark
  grounds. One choice picks the Material scheme, the semantic tokens, the sky bands and
  the icon set together, and it holds even under wallpaper colors, because the ramps and
  the canvas never followed the wallpaper. The vivid tokens are generated from the paper
  ones by one rule — same hue, held luminance, chroma to the sRGB gamut edge or ×1.8 —
  so every contrast ratio the design document prints is the same number in both, and the
  tests measure both rather than trusting that.
- The design system in Compose: a generated Material color scheme from three source hues,
  the semantic tokens Material has no slot for (verdicts, a rain ramp, a diverging
  temperature ramp anchored at 15 °C), the computed sky canvas and its scrim contract,
  the daylight ribbon, Inter as a bundled variable font, and the first components.
- Seven tests that hold the design document to the code: `PaletteContrastTest`,
  `PaletteDocTest`, `ScrimContractTest`, `SkyPaletteTest`, `NoRawColorTest`, `MotionTest`
  and `TextScaleTest`. Since the second palette they run over both of them, and
  `PaletteDocTest` reads the document section by section — two tables of the same shape
  in one file is one table and one lie waiting.
- Today ends with a line saying when its numbers arrived and where they came from:
  "Updated at 18:45 · Open-Meteo data". The freshness chip only speaks when the data is
  old enough to worry about, so until now a reader who simply wanted to know how recent
  the page was had nowhere to look. It sits at the foot because a timestamp is
  reference rather than headline, and because that is where a colophon goes.
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
  shapes), chosen in Settings → Appearance. Both sets are re-anchored to clear the
  measured 3:1 contrast floor on both surfaces, and `IconContrastTest` now sweeps both.
  Filled was the default until 6 set; see Changed.
- The launcher mark, replacing the Fase 0 placeholder: the icon family's starry-night
  crescent in Chiaro's own palette, low in the badge over two calm waves.
- **The Sky screen learns eclipses, and eighteen other moments** (asked by the
  committente, 4 set). Both eclipses, resolved for the place rather than for the
  planet: a lunar one only while the moon is up here, a solar one as these
  coordinates see it — clipped to daylight, and with its maximum re-measured inside
  the visible part, because a window that runs past sunset ends at sunset. The four
  moon quarters get rows of their own beside the generic next-quarter line, and so
  does the year's closest full moon. Two dark-sky windows go one step past the
  darkness window: the core of the Milky Way while it is high enough to see, and the
  zodiacal light on the nights the ecliptic stands steeply enough to show it. Four
  annual facts join the solstices — the earliest sunset and the latest sunrise, which
  are not the solstice; the earth at its closest to and farthest from the sun; and the
  start and end of the white nights, above the latitudes that have them. Three more
  meteor showers, from the same list the table already cited. Every one of them is
  computed on the phone and works offline, like the rest of the module.
- **A rainbow window on Today's timeline.** The one sky event that is not astronomy: a
  bow is centred opposite the sun and rises 42°, so it can only clear the horizon while
  the sun is under 42 — and whether rain is falling into that light is two numbers the
  fetch already carries. The row says when, on what rain probability, and which way to
  turn, in words. A possibility, never a promise.
- **The values an alert's message can print are now a list you tap** (asked by the
  committente, 4 set). Every value an alert can watch can also be printed inside the
  message it sends, which nothing on the screen had ever said — the help line named
  the two the trigger carries and stopped there. "Add a value" under the message opens
  the same vocabulary the condition uses, in the same words, and the pick lands where
  the cursor is. Nothing has to be typed and nothing has to be remembered, which is the
  rule the whole builder follows; the name arrives spelled for the reader's own units,
  and the engine resolves either spelling of it.
- **A guide to the sky events** (asked by the committente, 5 set). Every entry in the
  catalog now has a page: what it is, when it happens, whether the clouds have a say and
  what to read next — fifty-one of them, in both languages, all offline. It has two
  doors. The info button beside a catalog entry opens the page inside the sheet, so the
  list you were halfway down survives and the button that adds the event travels with
  the answer; and the index, from the Sky screen and from the guide in Settings, is
  there for a reader who came to understand rather than to subscribe. "When it happens"
  is read off the event's own definition rather than written down a second time, so a
  page can never claim a cadence or a verdict the app does not have. The prose is
  ported from tweather's manual pages, rewritten wherever it spoke about a crontab line
  or a file: nothing in this edition has one, and no page prints a dotted name.
- **Recent searches can be forgotten, one at a time or all at once** (reported on device,
  7 Sep). They had no removal at all: no gesture, no button, nothing, so the only honest
  reading of the section was that it was permanent. Each row now carries a cross and the
  section a "Clear", both undoable through the snackbar the saved places already use. The
  saved rows' swipe was the obvious thing to copy and the wrong one: a gesture is exactly
  as invisible as the nothing that was there before, and the report came from not finding
  anything. Undo puts the list back as it stood, order included, rather than searching the
  terms again — that list is ordered by when each search happened, and re-adding one would
  date it today.
- **A cross in the "Search for a city" field**, while there is something in it to clear.
  It appears with the first character and goes away with the last: a cross over an empty
  field offers to undo nothing.
- **The widgets say when the place they show is your position** (reported on device,
  7 Sep): the position pin now sits before the name, the same mark Today's header has
  carried since the second device pass and for the same reason — a saved "Cavenago" and
  a fix standing in Cavenago were two identical cards. A widget pinned to a city never
  draws it: a pin is a saved city, and it stays that city while you travel.
- **Today says what day and hour it is in a place that is not the one you are in.** It
  appears under the name on exactly the pages the position pin does not: on the position
  page it would be the phone's own clock reprinted under the status bar that shows it,
  while on Palermo or Reykjavík it is the one thing you cannot look up. The hour is the
  place's, like every other hour on that screen, and it moves with the minute.
- **The app answers "remove animations".** Android has no reduced-motion flag of its own:
  the accessibility toggle and the developer-options slider both write the animator
  duration scale, and zero is the answer. Chiaro reads it, watches it while it is open,
  and collapses all three places it moves to a 100ms fade: the week row opens without
  sliding, the pager jumps between places instead of travelling, and a rule's dry-run
  answer appears instead of being scrolled to. The sky canvas needed nothing, having never
  animated.
- **Everything on screen keeps its whole value at 200% type.** Nothing was ever cut off,
  but a column measured in fixed units holding text measured in scaling ones comes apart
  by itself: the week row's four columns, the hour cell, the timeline's clock and the
  journal's dates all held text twice their width and wrapped it mid-value. Columns that
  hold text now grow with the reader's type, and past 150% the week row becomes two lines
  (which day and what kind of day, then how warm) and the details grid becomes one column.

- **The drift strip marks the days it expects to freeze.** A day whose forecast
  minimum is at or below zero carries a small mark beside its name, and one line under
  the strip names those days with their numbers — "Frost forecast: Saturday 5 (28°),
  Sunday 6 (32°)". It is not a third metric: freezing is a threshold, not a drift, so it
  gets a mark and a sentence instead of a colour scale it would have nothing to say on.
  The latest forecast decides, so a day that warms back above zero loses its mark rather
  than keeping a warning nobody stands by; the line is absent entirely when nothing is
  freezing.

- **The Journal closes the loop: what the forecast said, against what happened.** When a
  day is over, one more line opens its section — "Rain was given at 70%: it rained", with
  the high that was forecast beside the warmest reading actually seen. Everything it
  needs was already on disk: the last prediction written before the day began, checked
  against the updates taken during it. Two rules, and they are deliberately not
  symmetric. "It rained" needs one wet observation, because a positive is proof and no
  amount of missing hours can un-see it. "It stayed dry" needs sixteen of the day's
  twenty-four hours genuinely covered — each update accounts for the hour behind it,
  overlapping windows are merged so a refresh spree buys nothing, and under the floor the
  day gets no line at all rather than a verdict resting on a phone that was switched off.
  The hours behind the claim are printed with it. Days from before this update carry no
  observation and are not judged: the screen fills in from here, it does not invent a
  past.

### Changed

- **The drift strip is drawn on time, not on updates.** It used to be one column per
  update, so at the default hour of polling it showed fourteen hours under a heading that
  says "how the week has been moving", printed "14 updates, Sat 5 to Sat 5" underneath,
  and spaced a night with the phone off exactly like an hour of it. A column is now a
  six-hour slot, fourteen of them are three and a half days, and a slot no update landed
  in is drawn as the gap it was. Empty slots at the start are trimmed, so the strip begins
  where the evidence begins; the caption states the slot width and both ends with their
  hour. The rows are the days still ahead: a horizon gone stale offline loses them rather
  than labelling days already gone "the week ahead".

- **The Journal follows its place instead of polling it.** It rebuilt forty commits of
  JSON and a diff engine once a minute, on unchanged data, and did it under battery saver
  too. Nothing on that screen ages with the clock, so the timer bought nothing: it now
  watches the table, which reports a new update and stays quiet otherwise. The history it
  reads is the whole of the place's own, which is also now what is kept — a hundred
  commits **per place** rather than a hundred shared between them, so a place's diary no
  longer gets shorter each time you follow another one.

- **A day with no rain probability says nothing instead of saying zero.** The provider's
  models do not all forecast one, and the day was being filled with a 0% — which is not
  "we were not told", it is a forecast of no rain. The week row now leaves its column
  empty and keeps the grid, the journal prints a dash where the old value never existed,
  the drift strip draws absence, the morning summary drops the rain clause rather than
  announcing 0%, and an alert of yours that reads the day's probability skips instead of
  firing "under 10%" on nothing.

- **The app icon wears the Vivid palette.** The mark was still painted in Paper, which
  stopped being the palette a fresh install sees. The crescent, the stars and the two
  waves are the same drawing: each ink keeps the lightness it was drawn at and takes its
  hue and chroma from the vivid source colors, so the badge is a cool white ground under
  an azure moon while every contrast inside it stays where it was measured.

- **The Now widget breathes on both sides.** The weather icon used to start exactly where
  the card starts, on the assumption that the glyph carried a quarter of its box as
  margin; measuring all 160 condition drawings put the real figure between 6.5 and 9
  parts in 64, so the icon sat closer to the edge than intended and the day's high and
  low, which have no margin at all, sat closer still. The card now insets its two
  horizontal edges separately — enough for the glyph on the leading side, the full inset
  for the words on the trailing one — and keeps the tight top and bottom that let the
  icon fill the height. The messages a widget shows when it has nothing to draw yet are
  centred on the card now instead of sitting in its top corner, where two lines read like
  content that had not finished loading.

- **The widget picker shows the real widgets.** Glance has no picker preview of its own,
  so the launcher was advertising all three with the same generic loading card. Each one
  now carries a static preview drawn as the card really looks: the computed sky under its
  scrim, the outlined icons, and sample values — the hero glyph with a temperature and a
  place for Now, the day's sentence and five hours with their rain for Today, the moment
  and its verdict pill for Sky. A preview is a drawing of the product, never a reading:
  the moment the launcher binds a widget it draws only what it actually knows.

- **A widget can pick its own weather icons.** Its settings — reached by pressing and
  holding it — now offer the icon family beside the background and the opacity: follow
  the app, filled, or outlined. Filled reads across a room and outlined reads quietly on
  a page, and a card living on a wallpaper is not the app screen, so the two can honestly
  differ. Existing widgets keep following the app, which is what they did before.

- **The Now widget's icon moves a little closer to the edge.** The previous pass sized
  its inset off the icon family's median margin, and the family's median is not what a
  night home screen shows: the crescent keeps almost a quarter of its box empty on that
  side, so it ended up sitting noticeably further in than the neighbouring widget's. The
  inset is halved; the day's high and low keep the trailing edge they were given.

- **The Now widget has three forms, and the size you give it picks one.** Laid out
  against the launcher's own weather widget at four sizes: a one-row card at two or three
  cells is the glyph, the temperature and the place; at four cells the day's sentence
  joins them against the far edge, right-aligned and centred on the row; a two-row card
  puts the glyph alone in the top corner and stacks number, sentence and place under it.
  The sentence is the one Today opens with — the umbrella, the thunderstorm — in a
  register short enough for three lines of fourteen characters, and when there is nothing
  to warn about the slot says what the sky is doing now instead, which is not a filler
  but the thing the card is for. The two switches the widget used to offer are gone: the
  sky's state beside the number, because the grant now decides whether there is room for
  the sentence, and the day's high and low, which competed with it for the same edge and
  are a tap away in the app (the Today widget keeps its own). Every edge is inset for what
  sits against it — 4 dp where a glyph does, because a Meteocons drawing carries its own
  margin, 14 dp where words do — and the two-cell card gives the glyph a little less
  height so the place name is not cut to «Dergan…». The picker shows the four-cell form,
  which is also where a new widget now lands.

- **The Now widget's words grow a step, and so does the position pin.** On the device,
  beside the launcher's own widget, the sentence at 14 sp and the place at 15 read small
  against its 17: both are 16 now, told apart by weight and ink rather than size. The
  brief register gains its last two forms — «per il resto del giorno» for the rain and
  the snow that do not stop today — because at 16 sp «della giornata» was the one sentence
  that no longer fit two lines on a square card. The pin before a position's name grows
  from nine tenths of the text to its full size, which puts its ink where the
  neighbour's sits: a cap height and a descender, not a cap height alone.

- **The Sky widget is laid out again, on the Now widget's grammar.** The moment's time
  is the hero number now, its name under it with the day marker before it, the glyph
  filling the height beside them; on a four-cell card the verdict stands against the far
  edge as its word in the measured container with the number that decided it underneath,
  and on a three-cell card, where a word would push «Domani · Sorge la luna» off the card,
  it is the series' own mark — `✓ ~ ✗ ?` in the verdict's colors — before the name. A
  two-row card keeps that as its head and lists the moments after it, one per row with
  its small glyph, name, time and verdict, the word on a wide card and the mark on a
  narrow one, as many rows as the height holds and never more than you subscribed to.
  For a window that is open right now the number shown is when it ends, because that is
  the next thing that happens. The picker shows the four-cell form, where a new widget
  now lands; the message a widget shows when every subscription is off is centred like
  the other empty states.

- **About is complete.** The section now names the developer, the copyright and the
  licence, and credits everything the app is built out of: Open-Meteo's data under
  CC BY 4.0, the Meteocons weather icons, the Inter typeface and Google's interface
  icons, each with its licence and a tap through to the source.

- **Settings reads like settings.** Every explanation is now one short sentence about
  what the option does. Gone are the lines that justified a design decision to the reader
  ("either way they stop moving when your phone asks for less animation") and the privacy
  claim that was repeated in three places with a longer clause each time; it is stated
  once, where it belongs. The palette description went from 210 characters to 90, the
  restore row is called the same length of thing in both languages, and the same pass was
  made over the first-run screen, the sky reminders and the widget configuration.

- **The weather icons are drawn in line by default, and every one of them is 6dp
  bigger.** The default moving is the consequence, not the cause: the drawings were sized
  for scanning, not for looking at, so the family's ladder went up (hour strip 32 to 38dp,
  week row 28 to 34, timeline row and detail tile 24 to 30) and nothing else moved with
  it, no padding and no column width. What the growth spends is the elastic measures
  beside the icons, each checked at 360dp: the fixed 56dp hour cell keeps 9dp of air per
  side, the week's temperature bar and the timeline's prose give up 6dp apiece, and the
  detail label's budget goes 94 to 88dp against a widest shipped label of 76.7dp. At those
  sizes the outlined set keeps one weight of ink on a screen whose hero is already a
  painted sky, where eight filled marks read as stickers laid over a painting. The four
  sizes are one object now (`ui/icons/WeatherIconSize`) instead of four numbers quoting
  each other in four components. The fill set is still one tap away in Settings →
  Appearance; an install that never opened that setting changes with the upgrade, which is
  what a default is.
- **The rain sparkline is a chart now.** A line with no scale under it is a shape with
  nowhere to stand: on a day pinned at 100% it drew a near-straight rule across an empty
  box, and the flatter the day the less it said. It now carries the two things a reader
  actually asks it — how high, and when: gridlines at 0, 50 and 100% with both ends
  printed, a dot on every hour, the hour named under the axis every six hours with the
  first and last always there, and a tinted area under the line, because a filled shape
  carries a level at a glance where a stroke only carries a direction. The line itself
  moved to the ink ramp: a day peaking at 10% used to draw its line in a colour two steps
  above the paper it was on. A day with no rain in it still draws nothing at all.
- **"What changed" comes after the week.** Every sentence in it is about a day further
  out, so ahead of the week it named days the reader had not been shown yet, and it cut
  between the two sections about today and the one about the days ahead.
- **Every printed number goes through the reader's language, percentages included.** Five
  of them were assembled in code instead — right in both shipped languages, which is
  exactly why they survived five readings — and the air-quality value had an English word
  welded into it where no translation could reach. The formatter itself is now tested in
  Italian and English at once, which is how a formatter really fails: by being correct in
  the language it was written in.

### Fixed

- **A temperature just below zero no longer prints "-0°".** Anything from −0.5 °C up to
  zero rounds away to a whole degree that kept its minus sign, so a frosty dawn at −0.4
  read as a value nobody writes by hand. At that precision the reading is zero and now
  says so; −0.5 still rounds to −1°, and every other negative keeps its sign. It ran
  through every temperature on every surface — the week rows, the journal, the
  notifications, the widgets — because they all print through one formatter.

- **The drift legend printed ends the scale does not have.** The temperature swatches
  were labelled −10 °C and 40 °C while the ramp itself stops at −5 and 35: measured, the
  two end swatches were exact duplicates of their neighbours and the two numbers under
  them named temperatures the strip cannot draw. The legend now samples evenly between
  the real anchors and prints those.

- **A tap on the drift strip does something.** It carried a ripple that led nowhere, and
  announced an action to screen readers that did not exist; the numbers were only ever
  behind a long press. Both gestures now open them, the hint says so, and the strip
  finally describes itself to a screen reader — its cells carry no text, so a reader used
  to hear seven dates and nothing about what was drawn beside them.

- **"Today" and "Yesterday" follow the place, not the phone.** Reading a journal from
  another time zone, the day headings were grouped in the place's day but labelled in the
  device's, so they could disagree by one.

- **The drift sentence no longer contradicts the entries under it.** "The week has held
  steady" covered both the week that did not move and the week that moved and came back,
  above a list recording two revisions of the same day. A swing that returns now says so.
  And in Italian the sentence started with a lowercase weekday, where every other headline
  on the screen already capitalised it.

- **A forecast revision with nothing to say is no longer a line.** When the only thing
  that moved was a field this screen has no words for, Today printed "Wednesday 9's
  forecast changed:" and stopped — a colon with nothing behind it, and one of the three
  lines the section is allowed spent on it. The Journal keeps the row, because that is
  the log, but loses the separator that used to dangle in front of the hour.

- **The rain probability is one scale again, 0% included.** In the week rows the
  quietest number was printed in the heaviest ink: 0% fell back to the secondary text
  role at 8.9:1 while the 15% beside it printed at 1.29:1, because a figure was being
  painted with the ramp meant for marks (a sparkline, a drift cell, a swatch), whose
  light end is 1.12:1 on paper. Printed probabilities now have their own ink ramp in
  the same hue family, every step of it measured against the surface: 0% is the quiet
  end of that ramp rather than another color, and every value from 0 to 100 clears the
  4.5:1 floor. The hour strip and the widget follow the same rule; on the widget the
  low values used to disappear into the sky behind them.
- **A day is no longer called rainy because of one damp hour.** Any hour carrying a
  precipitation code used to label the whole day, so a single hour of 0.1 mm at 1%
  probability printed "Drizzle" across the week. Measured over 161 city-days: 47% of
  the days that came back wet were wet only from drizzle codes. Rain now has to be
  real before it names the day — a millimetre over the day, or three hours of it —
  while storms, freezing rain and the heavy grades still name it whatever falls.
- **Fog stops flickering on and off between hours.** An isolated hour is only turned
  into fog when the hour beside it is murky too; fog is not one hour long. Dropping a
  fog code the forecast's own visibility contradicts still happens on the spot, and it
  happens often: two thirds of the fog codes served are contradicted, some by sixteen
  kilometres.
- **Nothing renders a number the app was not given.** An hour with no forecast rain
  probability used to show "0%", which is a forecast of its own; the rain sparkline
  drew it as a point on the line. Now the cell prints nothing, the sparkline breaks
  where the data does, and the visibility tile is simply not drawn when the weather
  model does not carry visibility — which could previously sink the whole fetch.
- **The hero is this minute again, not the last hour.** The report was kept for as
  long as `update_frequency_min`, the background polling interval: with its default of
  60 minutes, landing on Today inside that hour showed the last background sync as if
  it were the present — temperature, sentence and all — and at 120 it could be two
  hours. The freshness chip said nothing, because staleness is counted from twice that
  interval and a cache hit is never that old. Open-Meteo publishes its current
  readings on a fifteen minute grid, and that is now how long one is kept.
  `update_frequency_min` is back to meaning one thing: how often the app wakes up in
  the background. Battery is still a feature, and the periodic job is still where it
  is paid; a screen you just opened is not.
- **Battery saver postpones the automatic re-reads.** The fetch on landing and the
  one the minute tick makes are conveniences nobody asked for out loud, so under the
  system's battery saver they do not run. A pull to refresh always does, and so does
  a page that has nothing to show yet. What keeps running either way is the clock: the
  stated age, the freshness verdict and the hours already over cost no radio, and
  freezing them would trade battery for a page that lies about the hour.
- **A page left open no longer freezes at the fetch that opened it.** The minute tick
  moved the stated age, the freshness verdict and the hours already over, but never
  the numbers themselves. Past those fifteen minutes it now re-reads them, silently,
  and it costs nothing while the page is not on screen.
- **The place name follows the reader's town again, not their province.** In Cavenago
  di Brianza the header read "Provincia di Monza e della Brianza"; in Segrate it read
  "Milano". Three things had to be wrong at once. The reverse geocoder was being
  handed the coordinates rounded to about a kilometre rather than the ones the phone
  gave — a displacement of up to 679 m at these latitudes, which is enough to leave a
  small comune and land in the fields beside it, where there is no town to name. Only
  the first of the several addresses the lookup returns was read, and the chain that
  turned it into a name put the province *ahead* of the quarter, so one address with
  no town on it was all it took for the province to win. Rounding is now what leaves
  the app, not what the lookup is asked about; five addresses are read; and the most
  specific name any of them knows wins, with the province kept as the answer only when
  it is the only one there is. A province printed where a town goes is not wrong data:
  it is data from another level standing where the reader reads a different one, which
  is the screen-must-not-lie rule caught from the side.
- **Between two positions the phone already holds, the better one wins rather than the
  more recent.** They were ranked by their timestamp alone, so a position derived from
  a cell tower ten seconds ago beat a good fix from two minutes before — which is the
  other half of "Milano" while standing in Segrate. They are now ranked by how far the
  reader may be from each one by now: the accuracy it declares plus the ground they
  could have covered since.

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
- **One header cost across all five screens.** A section header sat 20dp under the block
  above it on four screens and 12 on Today, where the list's own gap made up the
  difference; a group header sat at 16 in two places and 20 in a third. The three numbers
  now live in one place. What a header spends is uniform; what the eye sees still depends
  on the neighbour, and on the three screens whose rows are Material `ListItem`s the gap
  is Material's own — deliberately, because a list built out of the platform's component
  reads right by following it.

- **The sky canvas ends on a straight line.** Its two bottom corners carried a 28dp round
  that read as a card floating over the scroll rather than as the sky the screen opens
  on.

- **An alert's editor opens on the whole of itself.** The sheet behind a rule started
  half-open, which is the right state for a list — there is more under the fold, and the
  gesture that reveals it is the gesture that scrolls it — and the wrong one for a form,
  where the name, the condition, the message and the dry run are one sentence. Two
  consequences went with it. The dry run's answer printed just below the fold, so
  whoever had asked the question had to drag the sheet to read it; the sheet now opens
  on all of its content, and the answer is brought into view in any case. And that
  answer sat 12dp to the left of the button that asks for it: a text button carries
  Material's own content padding, so its label started inside the sheet's 16dp margin
  while everything else began on it. The sheet's three text buttons drop that padding
  horizontally and keep it vertically, so every line in the form now starts on one edge.
- **A dry run's answer no longer outlives its question.** It stayed on the screen while
  the conditions under it were edited, so a verdict about one alert sat under another.
  It goes when a chip moves.
- **A subscribed moment with no recurrence had no row.** The Sky screen took the daily
  moments and the annual calendar and dropped everything in between, so the next moon
  quarter — a line you could add from the catalog, that the widget showed and the
  reminders armed — never appeared on the screen that owns it. The calendar now carries
  those too, which is also where the new quarters and the eclipses land.
- **The details grid's icons are the drawings again, not their silhouettes.** The tile
  passed a flat tint to `Icon`, which recolors the whole vector — the one place in the
  app that did it to a weather icon, and against the rule that the family keeps its own
  colors (DESIGN §13.1). Flattened, the humidity drop lost the white % that makes it
  humidity, the barometer lost its needle, the air-quality particles merged into a blob.
  Two tiles that shared a drawing no longer do either: a dew point is a temperature, so
  it now carries the thermometer and the drop is the humidity mark alone.
- **Every label in the details grid holds one line.** Measured against the width the
  tile actually leaves it — 94dp beside the 24dp icon on a 360dp screen — the Italian
  "Punto di rugiada" (113dp) and "Qualità dell'aria" (107dp) did not fit, so they wrapped
  and pushed their own value down while the tile beside them stayed put. They are now
  "Rugiada" and "Qualità aria"; the full term keeps the places that have room for it.
- **A saved place can be removed with TalkBack.** Swipe-to-remove was the only way out
  of that list, and a swipe has neither a keyboard nor a screen reader — so for anybody
  using one, the list could be added to and reordered but never shortened. The row's
  custom actions now carry "Remove" beside "Move up" and "Move down", which have been
  there since the reorder gesture landed, for this same reason.

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
