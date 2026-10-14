# Changelog

All notable changes to Chiaro are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versions follow
[Semantic Versioning](https://semver.org/).

`release.yml` reads the section whose heading matches the tag being pushed and uses it
as the body of the GitHub Release, so a version's entry is written **before** its tag,
and kept to what somebody arriving at that page wants to read.

1.0.0 had no release before it, so its entry-by-entry development record is its own
document: [docs/CHANGELOG-1.0.0.md](./docs/CHANGELOG-1.0.0.md). From 1.1.0 on, version
sections are a normal size and live here alone.

## [Unreleased]

### Added

- **Explore tonight hour by hour.** On the Sky screen, drag a finger along the night strip, or
  hold it, and the card paints the sky of that moment, with the hour's cloud, the rain when it
  is likely and whether the moon is up. Lift the finger and the whole night comes back. The
  strip now also marks every hour and the present moment, and draws the moon's hours in their
  own color with the moon in them.
- **A compass for where to look.** A sky row that says which way to look (golden and blue
  hours, the moon at dusk, the Milky Way, Venus, Jupiter) now opens with «Where to look» and a
  compass that turns you the right way. It tells you when the phone's compass needs
  calibrating, shows the figure-of-eight gesture that fixes it, and warns when something
  metallic nearby is pulling the needle. The sensors run only while the compass is open.

### Changed

- **The Journal answers first.** The week's card is now the head of the Journal, shaped like
  the Sky's tonight card: the answer in large type, such as «Saturday 17 is improving», with
  its numbers on a line of their own («rain 70% → 30%»), then the strip. Before the first
  trend can be drawn it says «Too early to say» and when the trend will arrive. «How the
  forecast did» takes the same rounded shape.
- **A Journal easier to scan.** The hours of the log stand in a column in full ink, as on the
  Sky screen; the days read «Today», «Yesterday» under a «What happened» title, without the
  count of entries. Each line's badge now draws what it is about: the rain or the thermometer
  for a revision, your alert's own drawing for an alert that fired, a crossed-out cloud for an
  update that did not arrive. Under «How the forecast did» each day is named by weekday and
  date, where a lone «T» could be Tuesday or Thursday.
- **Undo on the Sky screen.** Adding or removing a moment from its page now shows a message
  with «Undo», and undoing a removal brings the moment back with its own reminder.
- **An easier place switcher.** On Sky, Alerts and Journal the whole title is now the button
  that changes place, not just the small line under it.
- **Adding a moment is one button.** «Add a moment» is now a floating button at the bottom
  of the Sky screen. The usual reminder and «Even under a covered sky» moved to Settings,
  under «Sky reminders».
- **One moon on the Sky screen.** The tonight card, today's moon and the widget now read the
  moon at the same moment of the night, so they print the same percentage, and a phase like
  the full moon says its day and hour: «Full moon tomorrow at 18:49».
- **Clearer sky icons.** Meteor showers have their own drawing, a shooting star, instead of a
  moon with a streak across it; the conjunctions show the moon beside a star, or two stars.
- **Tonight always answers.** On a night with no dark window (a moon up from dusk to dawn,
  a summer night that never gets dark, a polar night) the Sky card now opens with a word, such
  as «A moonlit night», and the night's cloud, instead of a paragraph. The hours of the night
  are printed large.
- **The next moment stands out.** At the top of the moments, the next one is a card of its
  own with its time large, how soon, which way to look and its verdict. The others sit in
  grouped rows, with their times in a column on the right.
- **The ribbon shows the sky.** Under the temperature and on every day of the week, the band
  of the day's light now takes the forecast's clouds hour by hour, with the same colors as the
  sky at the top, and marks the hours with rain or snow: each day says *when* it is clear,
  grey or wet. What is already over today is drawn faded.
- **A sentence on quiet days too.** When there is nothing to warn about, the line under the
  temperature now says when the next rain is due and when it will be warmest, from the
  forecast's own numbers. It sits right under the sky's word, before the ribbon.
- **Sunrise and sunset on the ribbon**, printed under the points where they fall, and the
  week's ribbons a little thicker so the sky in them reads at a glance.
- **Less «0%».** The chance of rain is printed from 20%, where the app starts calling an hour
  wet; under it the column stays empty, and a dry strip of hours has no rain row at all.
- **Overcast is one cloud.** The two-cloud drawing read as a moon at small sizes.
- **An open day starts at dawn**, not at midnight.
- **The rest of the day**: the golden and blue hours say when they end, and the moon's rising
  and setting stay on the Sky tab.
- **The guide explains the week's two bars** with a drawn example, and no longer describes the
  ribbon as it was before.
- **Warmer or cooler than yesterday.** On a quiet day the sentence says when today's high is
  three degrees or more from yesterday's, from what Chiaro already kept: no extra download.
- **Pressure says which way it is going**: rising, falling or falling fast over the next three
  hours, with the change in hPa; a steady barometer keeps its reading's meaning.
- **Details**: an odd last tile takes the whole row, and the empty band between the sky and
  the next hours is gone.
- **Widgets, one type scale.** The five home-screen widgets now print in the same six sizes,
  so a place, a list row or a chip is the same size on every card. The verdict is larger, and
  the Sky widget's chip carries its mark before the word, as in the app.
- **The day's arc on a narrow card** prints its agenda on two lines, name over time, instead
  of cutting the names short; its header names the next moment you follow before a moonrise.
- **The hours ahead say "13:00"**, not "13", so they no longer read as dates over the row of
  days.
- **One name for the golden hour** in Italian («ora d'oro»), and the Sky widget prints the
  moment's short name: its clock already says morning or evening.
- **Three widgets instead of five.** «At a glance» shows the next hours and days when you make
  it two rows tall, and «The day's arc» can show the moments you follow instead of the arc.
  «The hours ahead» and «Sky moments» are no longer offered, but the ones already on your
  home screen keep working.
- **«Partly cloudy» looks like a step up from «mostly clear»**: the same sun with a bigger
  cloud, instead of a different drawing; the four skies from clear to overcast now climb in
  steps, and a stray speck next to the «mostly clear» cloud is gone.
- **The full moon on the day's arc is a disc**, not an empty ring.
- **«In words» on the same scale**: its sentence is the size of the facts under it, set apart by
  weight, which gives the temperature a little more room.
- **Widgets wear your launcher's corner**, and on the day's arc the moon's path is dotted, so
  it no longer looks like the sun's course under the horizon.
- **Showers look like the partly cloudy sky with rain**: the same sun and cloud, with drops or
  flakes under it, instead of a different drawing.
- **Rain, snow and storms fall from one cloud**, the overcast one: their drawings had a second,
  darker cloud that read as a moon at small sizes, and the storms a sliver of sun.
- **The day's arc paints one continuous sky**, without a step at every hour or stripes at dawn
  and dusk; the hours already gone are faded like the rest of the past.
- **Tall widgets carry more instead of empty space**: «At a glance» lists the next hours under
  its temperature when it is narrow and tall, and shows twice the hours at four rows; the
  moments you follow have the day's arc under them when the card has room.

## [1.1.0] - 2026-10-01

**A clearer page, and an app that reads the forecast itself.** Each hour's icon and word now
come from Chiaro's own reading of the forecast's numbers, every screen has been redrawn, and
alerts, widgets and notifications do more with less noise.

Android 13 (API 33) or newer. Installs over 1.0.0 and keeps your places, alerts and settings.
Check the download with the `.sha256` file beside the APK, and the signing certificate against
the fingerprint in the
[README](https://github.com/fiorenzobrioni/chiaro#install).

### New

- **The forecast, read by Chiaro.** An hour's icon and word come from the rain, snow, chance,
  temperature, visibility and clouds; the numbers on screen stay the model's.
- **How much, not only how likely.** Today's rain and snow in millimetres, the cloud layer,
  and each day's rain, gusts and UV when you open it.
- **Your alerts' hours.** Any time, only in daylight, or one or two bands of your own.
- **A «New alert» card** to start from scratch, a «Serious heat» idea, and new things to watch
  (today's rain and snow, gusts, the European air index).
- **Journal: how the forecast did**, on the days already over.
- **Notifications with pictures**: the next hours of rain, the day's temperature, the warning
  levels. Quiet from 22:00 to 7:00, except red warnings and sky reminders.
- **«In words» shows the next hours** on a tall card, and every widget's settings show the
  card itself at its real size.
- **A new app icon**: the day in a ring.

### Changed

- **Today, redrawn.** The ribbon as light, the sun and moon in the sky, the next hours as a
  curve, the temperature in whole degrees, the forecast for now when the data is old.
- **Sky, redrawn.** Tonight as a night sky, the moments grouped by day with a countdown.
- **Alerts and Journal, lighter to read**, and Settings with a live preview of the appearance.
- **The hours mean the hour that starts.** «15 · 60%» is the chance from 15 to 16.
- **Air quality on the European scale in Europe**, and pollen on each species' own scale.
- **Fewer false alarms.** A storm with under 20% chance no longer raises «Bad weather»; rain
  is announced once per spell.
- **Back follows your finger**, with a preview of the page behind it.

### Fixed

- «In words» no longer drops its temperature on days with a warning, or cuts its last line.
- An overcast sun no longer looks like the moon.
- The week's word follows the day, not its single worst hour.
- A forecast without UV no longer empties the app.
- A rule about today no longer fires twice a day.

## [1.0.0] - 2026-09-21

**The first release.** Chiaro answers the question people actually have: is it worth
going outside, and when. A computed sky as the hero, one sentence before any number, an
agenda of the day's sun and sky with a verdict on each moment, a journal of how the
forecast changed, and alerts the reader writes themselves. Free, no account, no ads, no
tracking, no API key.

Android 13 (API 33) or newer. The APK on this page is minified and signed with the
release key; `mapping.txt` beside it is what makes a stack trace from it readable.

### What is in it

- **Today** — one scroll that starts with a **computed sky**: a gradient built from the real
  position of the sun, the cloud cover and the moon, carrying the place, the temperature and
  the **headline sentence** ("umbrella around 17:00, clearing after 19:00"), which is absent
  when there is nothing worth saying. Under it the next 24 hours with a rain sparkline, the
  rest of the day as one merged timeline of sun, moon and weather turns, what changed when
  the last update moved the week, the seven days on one shared temperature scale, and a
  details grid where every number carries its consequence: UV 8 is "burns in about 15
  minutes, cover up", not an 8.
- **Sky** — tonight's verdict on the dark window with the numbers that decided it, the
  moments ahead as an agenda rather than a log, the calendar of what is coming (meteor peaks,
  the next full moon, solstices and equinoxes) with an honest "too far out to say" past the
  forecast's horizon, and a catalog of **60 moments** in seven groups, each teaching what it
  is in one line. Nine carry a camera and a direction to look in. All computed on the device,
  with no network at all.
- **Sky reminders** — a bell on any moment, delivered by a single deliberately inexact alarm
  (15 minute floor, no exact-alarm permission asked), re-armed on boot, and suppressed when
  the sky is going to hide the event unless you ask for it anyway.
- **Official warnings** — in Italy, the Protezione Civile's criticality bulletin. The 187
  zones are bundled as geometry, so a place is located by where it is rather than by how its
  name is spelled, offline and for the device position too. A level is always a word and a
  glyph before it is a colour.
- **Alerts** — four ready-made switches that say exactly what they send and when, then your
  own: five templates, and a builder that is a sentence of tappable chips. Values are picked
  and never typed, so an alert cannot be written wrong. **Try it now** runs a rule against the
  forecast already on the phone and says what it would have done, posting nothing. Your
  message is your content: never rewritten, never translated.
- **Journal** — every update is already a row in the database, read back as prose and grouped
  by day: forecast revisions with their numbers, alerts that fired, sky moments observed, and
  updates that failed with their reason in plain language. Under it the forecast drift strip,
  built entirely from data already on disk.
- **Places** — a horizontal pager between saved places, with search as you type, recent
  searches, reorder, and swipe to remove with an undo. Location is coarse only and optional,
  never taken in the background.
- **Five home-screen widgets** in Glance — Now, Today, In words, Sky and The day's arc, the
  last resizing from one cell to sixteen. Each configured on its own, reading the same
  builders the app reads, so the home screen and the app cannot print two different sunrises.
- **First run** — one screen and two answers, and the location permission asked only after
  the sentence explaining why. Then the notification question, put in words before the system
  puts it in a dialog. Skipping is allowed and lands on a real "no place yet" state.
- **The guide** — a tour of the four screens in both languages, teaching with the app's own
  components. It never explains a control: a control that needs explaining is a bug here.
- **Appearance** — two palettes (Paper and Vivid), three typefaces (Google Sans, Inter, the
  phone's own), two weather-icon families drawn from Meteocons v3 by a tool rather than by
  hand, animated unless the phone asks for less motion, in light, dark or whatever the system
  is doing.
- **Offline, honestly** — the last successful report per place is kept with no TTL, so the app
  is never blank. Hours that have already happened are dropped, stale data states its real
  age, and there is no full-screen spinner in this product.
- **Battery as a feature** — one shared periodic job carries the fetch, the alerts, the rules
  and the sky observation. No foreground service, no background location, no push service.
- **Italian and English** through the system per-app language picker. Everything Chiaro
  renders is prose or data, so everything localizes, notifications and widgets included.

### Deliberately not in 1.0.0

Radar and satellite imagery (the provider has none, and that is a stated position rather than
a gap to hide), tides, aurora, air-quality forecasting beyond the current index, Wear OS,
sharing, and a second provider.

### The full record

Chiaro reached 1.0.0 with no release before it, so the entry-by-entry development history is
long enough to be its own document: it is in
[docs/CHANGELOG-1.0.0.md](https://github.com/fiorenzobrioni/chiaro/blob/main/docs/CHANGELOG-1.0.0.md).
