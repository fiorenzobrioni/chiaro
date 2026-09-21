# Changelog

All notable changes to Chiaro are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versions follow
[Semantic Versioning](https://semver.org/).

`release.yml` reads the section whose heading matches the tag being pushed and uses it
as the body of the GitHub Release, so a version's entry is written **before** its tag.

## [Unreleased]

Nothing yet.

## [1.0.0] - 2026-09-21

**The first release.** Chiaro answers the question people actually have: is it worth
going outside, and when. A computed sky as the hero, one sentence before any number, an
agenda of the day's sun and sky with a verdict on each moment, a journal of how the
forecast changed, and alerts the reader writes themselves. Free, no account, no ads, no
tracking, no API key.

What ships in it: **Today**, one scroll that starts with the sky and ends with the line
saying when its numbers arrived; **Sky**, sixty moments computed on the device with
their verdicts and their reminders; **Alerts**, four ready-made switches that say
exactly what they send plus a builder for your own rules; the **Journal**, the forecast's
own history read as prose with the drift strip under it; the **official Protezione
Civile warnings**, located by geometry rather than by how a place name is spelled; five
**home-screen widgets** in Glance; the **guide**; two palettes, three typefaces and two
weather-icon families; **Italian and English** through the system per-app language
picker; and an offline-first data layer that states the age of what it shows instead of
pretending.

Android 13 (API 33) or newer. The APK on this page is minified and signed with the
release key; `mapping.txt` beside it is what makes a stack trace from it readable.

Everything below is the work that got there, recorded as it happened.

### Added

- **The evening summary** (Alerts → Ready-made, off by default), the twin of the morning one.
  Once a day between 18:00 and 23:00, and its subject is **tomorrow**: by eight in the evening
  today is no longer a decision, and the alarm, the coat by the door and the umbrella are.
  Collapsed it is the same sentence its morning twin says, under a title that reads «Domani»
  instead of «Oggi» — the day is stated once, where it belongs. Opened, it carries what the
  collapsed line has no room for and what nothing else in the app tells you at that hour: how
  cold the night gets and when, with what that asks of you (ice on the glass, a coat, a window
  that can stay open); whether water falls on it; tomorrow's rain as a window with its peak, on
  the same 70% bar the rain warning fires at; tomorrow's sunrise and sunset **with the change in
  daylight against today**, which is the line this app exists to print; and tomorrow's peak UV
  once it asks for anything. Every one of those lines is drawn only when it has its data, and it
  does not fire at all when the report has no tomorrow left in it. It is deliberately one alert
  and not two: the night and tomorrow are one decision taken at one moment, and two notifications
  in the same hour is the noise every other rule in this app is written to avoid.
- **The typeface is a choice now** (Settings → Appearance → Typeface), with three answers.
  **Google Sans is the new default**: the open-licensed upstream font, bundled like Inter and
  so the same drawing on every phone, cut down to the letters this app prints, which is 307KB
  in the app against 5MB at the source. **Inter** is one tap away and stays the face the type
  scale was measured against. The third answer is the phone's own font, which is the closest
  the app gets to the home-screen widgets — closest, not identical: a widget is drawn by the
  launcher rather than by the app, so on a phone whose system interface runs a different face
  from the one apps get, the two still differ. The choice swaps the family under all seventeen
  type roles and moves nothing else, not one size, weight or line height, and the credits name
  both bundled fonts, always, then say which one is on the screen. Worth knowing before
  picking the phone's font: a weight the device does not carry is synthesised rather than
  drawn, and a font with no tabular figures quietly ignores the request for them, so columns
  of numbers can stop lining up. Both bundled fonts were checked for tabular figures on the
  files themselves.
- **The weather icon on the text widget, as a switch on the card** (off by default). It is
  not a section the card makes room for: all four of that widget's layout plans are computed
  without it, and the glyph is then drawn only into space they already leave empty — so
  turning it on costs no line of sentence, no warning's word, no high-and-low and no dp of
  number, and where there is no such space there is no drawing. Two slots, one per kind of
  air: beside the number, in the rest of its own line (which is air because a temperature is
  at most 2.1 ems wide, measured against «−12°» and never against the number being printed),
  and on the two-row four-cell panel over the words, in the band that form keeps empty by
  pinning its eyebrow to the top and its block to the bottom. That works out to 52 dp on the
  reference four-cell row, 58 on the taller row one launcher grants, 71 on the panel and 104
  on a three-row card; three cells on one row and two cells on two stay words, which is the
  width those forms already spend on the sentence. The glyph meets the card at a glyph's
  4 dp edge rather than the words' 14, and each text that reached that edge pays the 10 dp
  back, so every line still wraps against exactly the width it wrapped against before.
  Turning it on also brings back the icon-family choice for that card, and the drawing takes
  the condition's own word for a screen reader when the sentence is off and nothing else
  names the sky.
- **A card colour for every widget.** Beside the computed sky, light, dark and follow-the-
  system, a widget's card can now be one of six colours: blue, light blue, green, sea
  green, violet, terracotta. It is the one place in this product where colour is offered as
  colour rather than as meaning, and it ships with no new inks — each colour is picked dark
  enough to carry the white pair the sky card already writes with, measured at no worse
  than 7.9:1 for the ink, 5.2:1 for the quiet one and 6.2:1 for the freshness one, so even
  the 11 sp lines clear what small text needs. No two of the six are closer than 13 ΔE, so
  picking one over another is picking a colour and not a word. The two configuration
  screens now print the same background section from one place rather than from two copies
  of the same list, and the colour rows carry a swatch behind their name.
- **A fifth home widget, «In parole» / "In words"**: the Now widget's facts with nothing
  drawn on the card. No weather glyph, no position pin, no warning chip, no verdict mark —
  words and figures, and the whole hierarchy built out of type. It is not the Now widget
  with the icon deleted: a card that loses its drawing loses the thing that made it
  readable across a room, so the temperature becomes the drawing — Bold where the
  household writes Medium, and grown into the grant the way the other four grow their
  glyph, from 30 sp on a squeezed row to 56 on a tall card — with the place two ranks under
  it as the card's eyebrow, the day's sentence at its shoulder, and the official warning
  and the day's high and low as facts under that. Four ranks that differ by size AND weight
  AND ink, never by one of the three on its own.

  Four cells by one is the default placement and it resizes both ways from there. Three
  cells already carry the sentence, one cell better than the Now card manages, for an
  arithmetic reason: the 66 dp of glyph and its gap that card spends before its first
  letter, this one does not have. Two cells fall back to the number and its place. Two rows
  and up become one centred column with the next hours printed as figures where the height
  holds them, and the order the budget spends in is the hierarchy written down: the number
  is reserved first, the footnotes are bought last, and a section that does not fit is not
  drawn. The two things a drawing used to say now say themselves in words: the phone's own
  position, which was the pin, and the warning's level, which was a chip. The chip is the
  device that carries its own measured ground — a bare coloured word on a scrimmed sky or
  somebody's wallpaper was measured unreadable on the Sky card back on 4 set — and a card
  with nothing drawn on it cannot bring one, so it gives up the colour rather than the
  legibility and prints the level as a word in the card's own ink. DESIGN §2.3 makes the
  word the carrier anyway.

  Everything else is the household's: the same `WidgetData` model, the same brief sentence
  Today and Now print, the same `warningSlot` table, the same per-widget place, background,
  opacity and content switches (minus the icon family, which would change nothing here).
  Its budget is pure arithmetic on dp and sp with a table pinning it, like the other four.

  Redrawn twice the same day on a device. The second pass took the hourly temperatures off
  the tall card for good — they read as a second widget stapled under the first, and the
  card that exists for the hours is the Today widget — and gave the height back to the
  composition: the place is pinned to the top of the card as its eyebrow and everything else
  to the bottom, with the air between them. From four cells up a two-row card becomes a
  panel: the number on the leading side at 64 sp, which it can afford because it stands
  BESIDE the words rather than under them, and the sentence and the day's high and low
  right-aligned against the far edge, bottom-aligned with it. The one-row card's leading
  column stopped being a share of the row and became what the place name actually needs
  (168 dp against the sentence's 104 minimum), so «Cavenago di Brianza» is printed whole
  where it used to be «Cavenago di Bri…» beside a column with 174 dp of white space in it.
  Its picker description stopped promising the hourly temperatures, which went in the same
  pass.

  The first pass: the place and the facts moved up to the household's
  16 sp and the sentence to 18, so the ranks still sort by size as well as by weight and
  ink; the position pin came back in front of a place the phone is standing in, where the
  first pass spelled it out in words and the words ate the place name («Ornago · la mia
  posizi…»); and the day's high and low each got a mark drawn at the verdict marks' weight
  rather than sharing a slash. A mark at the size of its own line, in that line's ink, is
  punctuation, and the card still carries no picture. The hero's floor dropped from 30 sp
  to 26 to pay for the bigger place line, which is what keeps a stale one-row card from
  clipping instead of shrinking.
- **Official warnings from the Protezione Civile** (Fase 11), for places in Italy. The
  Dipartimento's criticality bulletin is discovered from its own files, read from its CAP,
  and matched to a place by geometry: the 187 warning zones ship inside the app as
  simplified polygons (284 KB at a 500 m tolerance), so the lookup works offline, works
  for the device position, and does not depend on how a municipality's name is spelled.
  On Today a banner sits between the freshness chip and the guide card when there is
  something to say — the level, what it is for, which day, and the hour of the bulletin —
  and opens a sheet with the arithmetic: one row per risk and one column per day, every
  cell a word inside its own colour, the Dipartimento's own definition of the level, the
  bulletin's note when it names the place's region, the CC BY 4.0 attribution and the way
  back to the bulletin. Orange and red also take the sentence at the top of the screen;
  yellow does not, because in an Italian autumn yellow is frequent and a sentence that
  repeats stops being read. Alerts leads with the whole answer, absence included ("no
  warning for this zone", "no bulletin for today yet", "official warnings are for places
  in Italy"), plus the notification switch and the level to be told from. The Journal
  writes a line whenever a level moves, and one a day at most when the bulletin could not
  be reached. Three new colour pairs in `ChiaroColors`, measured: the ink at 8.5:1 in
  light and 11.0:1 in dark, and a deuteranope simulation showing that one of the two
  carriers collapses in each scheme — which is why a level is always a word and a glyph
  before it is a colour.

  On the home screen the level reaches the Now, Today and day's-arc widgets as a chip,
  by one rule the four cards share: orange and red are already in the day's sentence, so
  the chip appears only where that sentence is not, and yellow, which the sentence never
  carries, takes a line of its own where the card has one to spare. Each widget can turn
  it off on its own, and it is on by default.
- **Two Glance containers that were losing children.** Glance draws at most ten children
  per container and drops the rest without a word. Counting them for the warning chip
  found two already over: the Today widget's hour strip (seven cells with six spacers
  between them is thirteen, on exactly the four-cell card the widget is designed around,
  so the last two hours were being dropped in silence) and the Sky widget's tall card
  (thirteen with five rows under the hero). Both now space with padding rather than with
  spacers, the cure the arc widget already used: the geometry does not move, and each row
  is one child instead of two.
- A fourth home widget, **The day's arc**: the sun's real path over the reader's place,
  computed by the same astronomy engine that paints the sky, drawn over the sky of every
  hour as bands; the moon's path and its disc in tonight's real phase; each hour's rain
  chance rising from the ground; the present marked and the past veiled; the hours under
  the plot with their forecast temperatures. In words, the next light moment with its time
  and countdown, the agenda of the next twenty-four hours with the Sky widget's verdict
  beside the rows the reader also follows, and on the tallest card the week. It resizes
  from one cell to sixteen and reshapes itself at every step (a dial, a strip, a card, a
  panel, a board), and it has a settings screen of its own with a live preview at eight
  sizes: what the arc spans, what it is drawn on, every layer, the words, the agenda's
  families, the week, the density. The Today screen's timeline rule became a windowed
  agenda so the widget and the screen keep reading one rule.
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
  accent, saturated quantity ramps, a saturated sky and, for the outlined icon set,
  brighter weather icons on dark grounds. One choice picks the Material scheme, the semantic tokens, the sky bands and
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

- **Four defaults moved, on a fresh install from the device.** The **morning summary** and the
  **evening summary** now ship **on**: they used to start silent on the argument that a digest
  nobody asked for is the one notification a weather app gets uninstalled over, which is the
  right argument about a digest and the wrong one about these two — a sentence before any
  number, said at the hour the day is still a decision, is what this app is for, and an install
  that never opened Alerts was getting the warnings and none of the reading. One notification
  each, at fixed hours, off in two taps. Every widget is now **a blue card** where it was the
  computed sky: the sky is still the app's own hero and one row away, but it is a photograph of
  the weather behind a card of facts, and on a busy wallpaper the two grounds argue — which is
  what the scrim and the opacity slider exist to manage, and a default should not need managing.
  And **"In parole" starts with the day's high and low**, alone among the five: it is the card
  with no drawing to protect, its hierarchy is built out of type in ranks of facts, and that
  rank is already designed.

- **A tapped notification opens the screen it is about**, by the same rule the widgets follow.
  A sky reminder lands on **Cielo**, where the moment's row, its bell and the verdict with the
  number that decided it are. A fired rule and an official warning land on **Avvisi**: the rule
  card with the hour it last fired, and the warning card that opens the sheet with the grid of
  hazards by day, what the level means and the attribution — Oggi carries a banner too, but only
  from yellow up and only while the bulletin is live, and says nothing at all about a zone that
  is green. The four built-in alerts — severe weather, rain in the next hours and both summaries
  — land on **Oggi**, because they are the weather itself: its hours, its rain, its sentence.
  Avvisi is where the switch that sent them lives, which is not what the reader who tapped
  "Pioggia alle 17" came for.

- **"Momenti del cielo" and "L'arco del giorno" open the Sky screen** (asked for from the
  device). Everything those two cards draw is the Sky screen's own material — the moments,
  their verdicts, the next light moment, the agenda that follows — and landing on Today
  asked the reader to go and find again what they had just read on the home screen. The
  other three cards open the app as they always did, wherever it was left. A tap on a
  running app moves the tab rather than restarting the app, and takes any open Settings or
  guide page down with it: landing on Sky underneath one would be answering the tap and
  hiding the answer.

- **The place name on the wide "Colpo d'occhio" card takes the room the sentence is not
  using.** The row split its slack in half between the number-and-place column and the
  sentence, because Glance cannot measure text — so a four-cell card printed «Cavenago di
  Bri…» in 118dp next to a column holding «Sereno», which is 47dp of ink in 118dp of room.
  Both blocks are measured now, with a `Paint` in the app's process at the size, weight and
  face the launcher will draw them in, and the boundary falls where they ask for it: the
  sentence keeps its measured width capped at the even share, so a long sentence is never
  squeezed and the card falls back to exactly the layout it has today; the words then take
  what the name needs, never past what the sentence kept and never below what the temperature
  needs. Nothing ever comes out narrower than before, so no card loses room it has — the only
  space that moves is space one column was holding empty. A warning chip turns the measuring
  off and restores the even share: a chip cannot wrap or ellipsise. The mirrored arrangement
  (glyph on the trailing side) is untouched, since there the words already have the whole row.
  **"Le prossime ore" shares the rule**, on the one arithmetic both cards now call: what its
  trailing column asks to keep is the widest of what it carries, because one of that column's
  tenants — the day's high and low — cannot wrap, and a range given less than it measures is a
  range with a digit cut off its end.

- **The hero temperature is Bold on every card whose hero it is.** The Now and Today widgets
  printed theirs at 34sp Medium, which is the weight everything else on those cards is set in —
  the sentence, the place, the day's high and low — so beside the text widget's Bold hero the
  number read as one more fact rather than as the thing the card exists for. It is a household
  rule now and it names the hero, not the quantity: the arc card keeps its Medium, because
  there the hero is the drawing and the number is one line of the strip beside it, and the
  Today strip's own hours stay Regular because seven bold figures under a bold hero is two
  heroes. The picker previews carry the same weight, since a preview that advertises a
  different one is advertising a product that does not exist. Re-measured where it mattered:
  Bold costs +2.3% of Medium's advance in Google Sans and +2.0% in Inter over «−12°», about
  1.5dp at 34sp, which the Now card's 66dp number column absorbs with 3dp to spare.

- **The day's low is the same size as the day's high, and now it is also drawn that way.**
  Two things were wrong and only one of them was visible as a decision. The low figure was
  set in Regular on the quiet ink while the high was Medium on the strong one, which on a
  home screen does not read as "this one is secondary" but as a smaller number; where the
  marks are drawn (the text widget, which has the column for them) both halves are now set
  alike, because ↑ and ↓ already say which is which and the dimming was saying it a second
  time. Where the slash is drawn instead (the Today widget, whose column will not take the
  marks) nothing changes: with no mark to carry the distinction, the ink stays the thing
  that sorts the pair.

  The other thing was **the mark itself, and it was a bug**: Glance's `padding` is
  `setViewPadding` on the same view the size lands on, and an `Image` scales its drawing to
  fit what the padding leaves. The mark asked for a 16dp box with 8dp of leading air and
  2 of trailing and got a **6dp arrow**, next to a high mark that had only the 2 to pay and
  drew at 14 — 43% of the ink at the same nominal size. The air is on a wrapper now, which
  is what `PlaceLine`'s pin and the warning chip's own gap already did, and what
  `WidgetGlyphBoxTest` holds for every drawing in the widgets from here on.

- **The severe and rain warnings are whole sentences again.** The storm alert was built as a
  stem plus an optional «, pioggia al 90%» fragment, which cost it the full stop every other
  built-in alert ends with and fixed the clause order in English for every language that
  translates it. Both are now one complete sentence per shape, and an alert with no hour or no
  forecast chance picks a sentence that needs neither: the old code printed «Temporale verso
  le» and «pioggia al 0%», a broken sentence and an invented zero, in the one place the reader
  cannot check either. Unreachable today, because the engine anchors both alerts on an hour it
  has actually read, and now unreachable in the text too.

- **Italian no longer puts an article where a number may force it to elide.** «al 70%» is
  right and «al 80%» is not, because eighty is *ottanta*: inside 0 to 100 the vowel-initial
  numbers are 1, 8, 11 and 80 to 89, so eighteen sentences across the notifications, the Sky
  screen and the Journal were right nine times out of ten and quietly wrong the tenth. They
  are now written so no article touches the value: «pioggia 80%», the form this app's own
  morning summary has used since the day it shipped; «fino a 80%» and «coperto per 80%», with
  the bare prepositions that never elide; and «bollettino del giorno 8 set 2026» for the dates,
  where the article was given a word to agree with instead of a number. No helper and no
  locale-specific branch: a rule about Italian belongs in the Italian file, and what remains of
  it lives in a test that fails the build if an article comes back.

- **The current temperature is bold**, and tracked in with it. At 64sp a hairline figure laid
  over a painted sky reads as ornament rather than as the reading the whole screen is for,
  and the home-screen card has printed the same number bold since the day it shipped. The
  tighter letter spacing is half of the change: at that size the default spacing is drawn for
  a paragraph, and without it bold reads as shouting. The smaller readings in the metric
  tiles are untouched — at their size the old argument still holds.

- **Today's place row no longer scrolls away.** The city these numbers belong to used to be
  the first thing to leave the screen: the row sat inside the sky canvas, which is the first
  item of the scroll. It is pinned now, the way Sky, Alerts and the Journal already kept
  their own header, and it is the one bar in the app with two grounds — white over the
  canvas' scrim while the page is at rest, the page's own surface and theme ink from the
  first scrolled pixel, with the status-bar icons following the same flip because that bar is
  what is behind them. The canvas keeps the row's seat with a spacer as tall as the bar
  really is, so the hero lands exactly where it did before and the skeleton still matches.

- **«Quasi sereno» has a drawing of its own, and the app composes it.** WMO code 1 is a
  quarter-covered sky — a median 25% of cloud against code 2's 64%, measured on 1 680
  hours — and it had been drawn twice wrong: first as «poco nuvoloso», then, since the
  drawing Meteocons offers for it carries 72% of that cloud, as the plain sun. The plain
  sun is honest where there are words and mute where there are none, and the hour strip
  and the week row have none: one hour in six showed a clear sky over a quarter-covered
  one. It now takes a drawing this repo composes from two the family already has —
  `clear-day` untouched, same size and same place, plus `cloudy`'s silhouette shrunk into
  the bottom-right corner and cut out of the sun by the mask «poco nuvoloso» already
  carries. Between «sereno» and «quasi sereno» exactly one thing changes now, and it is
  the thing that changes in the sky. Measured: the cloud is 43.1 units of the 128-unit box
  against «poco nuvoloso»'s 99.2, the air between cloud and rays is 1.80 to 2.45 against
  Meteocons' own 2.48, and the cloud is re-stroked at the family's 4 units rather than
  shrunk to a 2.0 wisp beside a 3.7 sun. The night keeps the same cloud in the same place,
  with `clear-night`'s moon in front of it. Nothing is hand-drawn: `tools/compose_sun_cloud.py`
  writes all sixteen files (four faces, day and night, and their animated twins, where the
  rays turn and the cloud bobs), and a test compares the result with its sources path by
  path so a later re-import cannot leave it behind.

- **The Vivid sky is now vivid at night too.** The vivid band table takes its chroma to
  ×1.8 of the paper one, which is the right rule for a token and the wrong one for a
  sky: paper draws the night with the least chroma of any band, so a multiplier handed
  the least to the bands where sRGB has the most left. Measured as a fraction of the
  chroma the gamut holds at each band's own luminance, the vivid day sky ran at 1.00 of
  it and vivid midnight at 0.44 — the dress was loudest on the one sky that is already
  bright and quietest on the sky an evening reader actually opens the app under. The
  generator now also holds every band at or above 0.65 of that gamut, which moves the
  nautical, astronomical and night rows and leaves every other band byte for byte as it
  was, scrim measurement included. Paper is untouched, and so are the vivid semantic
  tokens: the floor is the sky's clause alone.

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

- **The widgets have names.** In the launcher's picker they were «Chiaro · Now», «Chiaro
  · Today» and «Chiaro · Sky»; they are **At a glance**, **The hours ahead** and **Sky
  moments** now («Colpo d'occhio», «Le prossime ore», «Momenti del cielo»), which say what
  each one shows rather than repeating the app's name above them.

- **The day's sentence can be turned off, per widget.** A switch in the widget's own
  settings, for At a glance and for The hours ahead, on by default: a reader who wants the
  bare number has it. Whether there is room for the sentence stays the card's decision;
  the switch can only take it away.

- **At a glance can be laid the other way round.** Its settings offer a second
  arrangement for the one-row card: the glyph in the trailing corner, and on the leading
  side the temperature with the sentence at its shoulder — two lines at most, centred on
  the number — and the place under both. It is the tall card's composition pressed into
  a row, for a home screen that wants the two to match; the sentence appears at the same
  widths as the other way round, and a tall card is the same either way.

- **The hours ahead is laid out again on the same grammar.** Its head is At a glance's
  wide row — glyph filling the hero band, temperature over place, the day's sentence
  against the far edge, the day's high and low under the sentence when asked for — and
  the hour strip hangs under it. The line the sentence used to take under the hero goes to
  the glyph, which grows from ~45 to ~76 dp on a four-by-two card; the temperature drops
  from 36 to 34 sp to match its sibling. The strip keeps its rules and gains one: the rain
  row also has to fit, and yields to the hero's words when a stale marker takes their
  third line rather than being cut at the card's edge.

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

- **The Sky widget drew twenty-five moments as meteor showers.** «Momenti del cielo» and
  the Sky screen each carried their own table of which drawing a subscribed moment gets.
  The catalog grew twice — the planets and the pairs, the eclipses, the four quarter moons,
  the earthshine, the zodiacal light, the white nights, the Milky Way, the earliest sunset
  and latest sunrise, the perihelion and the aphelion, and the full moon rising at dusk —
  and only the screen's table was kept up. An id nobody listed is a perfectly legal `when`
  with an `else`, so nothing ever went red: the card drew a falling star over every one of
  them, and the same subscription showed two different pictures on the home screen and in
  the app. There is one table now, in the object that owns which drawing a thing gets, and
  a test walks the catalog so that a moment added without an icon fails the build instead
  of shipping a meteor shower over an eclipse.

- **And the moon's own moment drew a full moon every night.** «La luna oggi» is a different
  shape every evening, and the Sky screen has always drawn the real one; the widget printed
  a full disc whatever the sky was doing, which is a picture of a moon nobody could see. It
  carries the phase now, from the same classifier the screen reads.

- **The full moon at dusk lends its verdict to the day arc's moonrise row.** «Luna piena al
  crepuscolo» IS that evening's moonrise — the search that finds it returns the moonrise
  instant — but «L'arco del giorno» matched that row against `moon.rise` alone, so a reader
  who followed exactly that moment found the row bare on the one evening it was about.

- **«In parole» printed its place and its temperature against the wrong edge.** On the
  default 4×1 card the name and the number sat at the *trailing* edge of their own column
  instead of the card's leading inset, so «Manchester» floated in the middle of the card
  with the inset empty behind it. A name long enough to fill that column — the «Cavenago
  di Brianza» the column is measured for — hid it completely, which is why the same card
  looked right in one place and wrong in another. Glance's `Box` has no per-child
  alignment: the `BottomEnd` written for the weather glyph landed on the words' column
  too, and that column had no width of its own to resist it. It fills its column now, and
  the card draws what its own picker preview had been promising all along. The same fix
  takes a second fault with it, one no reader had met yet because the glyph starts off:
  the drawing is sized out of what the widest number leaves *measured from the leading
  edge*, so with a short name and the glyph switched on it landed on top of the
  temperature.

- **The same card gave its trailing edge to a drawing it was never going to make.** Where
  the glyph meets the card, «In parole» insets that edge at 4 dp instead of 14 and every
  line that reaches it pays the 10 dp back, so nothing a reader can read moves either way.
  The two halves were decided by two different conditions, though: the card handed the
  edge over on the reader's switch alone, while the words paid it back only where a glyph
  really came out — and none comes out on a two-cell stack at any font scale, on the
  reference panel at a 1.3 font scale, or on a narrow one-row card at 1.3. On exactly
  those cards, switching the glyph on moved every line 10 dp into a 24 dp corner and drew
  nothing there. The inset and the give-back are one function now, with a test holding
  them together, and the place name joins the lines that pay it: on the two tall forms it
  is the full-width eyebrow, which is precisely the line that reaches that edge.

- **Two copies of the app, and a settings screen that came back from under it.** Android
  identifies a task by the intent that created it, and this app was entered through three
  hand-rolled intents — the widgets', the notifications', and the launcher's. So opening it
  from a widget and then from the home-screen icon built a *second* task: two home screens,
  one behind the other, and back came out onto the first. Every door in is now one intent and
  it is the launcher's own, with the destination riding as an extra where it changes nothing
  about which task this is; the activity is `singleTask`, which is what the app already was —
  one screen, with Settings and the guide as state inside it. The widget settings screens had
  the same illness from the other side: with the app's own task affinity they sat *in* the
  app's task, so closing one with a swipe to home left it there and the next launch put the
  home screen on top of it. They now have no affinity at all and do not outlive being swiped
  away — nothing is lost, because every choice there is written the moment it is tapped. The
  same fix is expected to settle the closing animation drawing opaque rounded corners after a
  back gesture, which happened only when the app had been opened from a widget: that is the
  generic task-close animation, which the system plays for a task the launcher does not
  recognise as one of its icons.

- **The alerts that could never ring.** Four ready-made alerts ship switched on — severe
  weather, rain in the next hours, official warnings and your own rules — and Android's
  notification permission was only ever asked by the act of switching one on, which a
  fresh install never does. So the switches said yes, the phone said nothing, and the
  only road to the permission dialog was to turn an alert off and on again. Two repairs,
  and it takes both. **First run asks, once**, on a step of its own after the place has
  been answered: a notification is a promise about a place, and there is no place on the
  screen before it. It is a screen of words with an explicit "Allow" and a "Not now" that
  costs nothing, never the system dialog on arrival — Android shows that dialog at most
  twice per install and then does nothing at all, so it is spent on a tap that asked for
  it. Skipping the place skips this with it, and an upgrade is never stopped by it.
  **And Alerts says so in place**, in a card above the list, for as long as something is
  switched on that the phone will not deliver — with the one button that fixes it, which
  names the door it opens: the permission dialog where the system will still show one,
  this app's page in the system notification settings where it will not (the other dead
  end: permission held, notifications switched off in Settings, where asking again
  returns "granted" and draws nothing). Unlike the dialog the card can be offered again
  every time, and it goes away by itself the moment the permission arrives. The Sky
  screen carries the same card once a reminder is really armed. With every switch off
  neither screen draws anything: there is no promise to break, and a card that scolds you
  about a permission you need for nothing is inventing a problem.

- **The palette note promised icons it does not always change.** "Vivid is cool white and
  azure, with brighter weather icons" was shown to every reader, but the dress only
  reaches the weather icons through the outlined set's dark-ground siblings — the filled
  set is Meteocons' own palette and is the same drawing under both dresses. A reader on
  filled icons was being told a change would happen when they tapped, and none would.
  The note now says which of the two they are in.

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
