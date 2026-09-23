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

### Changed

- **Today: the daylight ribbon is drawn as light.** One continuous gradient with rounded ends
  instead of hard-edged blocks, "now" as a disc, and the part of the day already spent drawn
  quieter; in the week's rows the night recedes, so each day shows its band of light.
- **Today: the page lies on the sky.** The content starts as a rounded sheet over the bottom
  of the sky instead of a hard cut, and its top catches the sky's own color: warm at sunset,
  blue at noon.
- **Today: the rain chart follows the hour strip.** It marks the hours the strip has in view,
  and a tap or a drag on it scrolls the strip to that hour. The first time it appears the line
  draws itself in from left to right.
- **Today: the sun and the moon are in the sky**, where they really are, the moon in its real
  phase. The sky keeps a band of its own for them above the temperature.
- **Today: the temperature reads first.** Whole degrees large, tenths small; the sky's word is
  bigger; "feels like" appears only when it differs by a degree or more.
- **Today: the bar at the top stays sky** as the page scrolls, and carries the temperature once
  the big one has scrolled away.
- **Today: the next hours draw the temperature as a curve**, and midnight shows the new day's
  name. Today's row in the week marks the temperature right now on its bar.
- **Today: the rest of the day is one thread**, and its next moment says how soon.
- **Today: every detail with a scale shows it in its own color**, with a disc on the value and
  the bands marked; pressure and pollen get one too.
- **Sky: tonight is drawn as the night.** The card is the night sky with tonight's moon in its
  phase, and a strip from dusk to dawn shows the moon's hours, the clouds hour by hour, the
  stars where it is clear and the dark window framed.
- **Sky: the moments are grouped under Today and Tomorrow**, and the next one says how soon.
- **Sky: the calendar ahead counts the days**, and "too far out to say" is said once instead of
  on every row.
- **Alerts: a day of alerts at a glance.** A strip of the 24 hours shows when the timed alerts
  can arrive, painted with the sky of their hour.
- **Alerts: lighter to read.** Every alert has its drawing, one sentence on what it sends and a
  separate line on when and how often; the switches sit in rounded groups; "no warning" reads as
  an answer; ideas to start from are cards you browse sideways.
- **Journal: a diary with a face.** Entries sit on a thread with a coloured badge per kind (or the
  sky's own drawing), the new value of each change stands out, day headings stay pinned, and the
  week's drift card leads with its sentence and points at the day it names.
- **Journal: how the forecast did.** From the days already closed: how far the highs were from
  the real ones on average, and the rain the forecast gave on the days it rained against the rest.
- **Settings: the appearance previews itself.** A slice of sky, a temperature, an icon and a verdict
  drawn with the palette, typeface and icons you pick, above the choices that change them. Groups sit
  on rounded cards, and the guide is a card of its own.
- **Settings: a short, true privacy note.** No account, no ads, no tracking; Chiaro has no server of
  its own, and Open-Meteo only gets the place you ask about, rounded to about a kilometre when it is
  your position.
- **«In parole»: the next hours on a tall card.** From three rows up the widget adds «Più tardi»:
  every three hours, the time, the temperature, the rain when it is likely and the sky in a word.
  It only uses space the card was leaving empty, and can be turned off.
- **Widget settings show the widget.** «In parole» opens its settings on the card itself, at the
  size it has on the home screen, with chips to see how it lays itself out at other sizes. The
  settings are grouped like the app's own, and the backgrounds show their colour.
- **Widgets: tall cards use their height.** On «Adesso» the temperature grows with the card from
  three rows up; «Le prossime ore» keeps its sentence whole and adds the next days under the hours.
- **Every widget's settings show the widget.** All five open on the real card at its size, with
  chips for the other sizes and a line saying what each one carries, on the same grouped layout.
- **A new app icon: the day in a ring.** A day of 24 hours coloured like the app's sky, with the
  sun on it at mid-afternoon; the ring opens around the sun into a C. Themed icons get a drawing
  of their own.
- **Notifications with pictures, where they help.** Opened, a rain or storm alert shows the
  next twelve hours of rain with the alert's window marked; the morning and evening summaries
  show the day's temperature and rain; an official warning shows the levels by hazard and day.
- **Notifications: clearer words.** The morning summary says when it will rain; a fired alert
  of yours shows the value it read with its unit. The status bar icon is the new ring.
- **Notifications arrive once, and quietly at night.** Rain or a storm is announced when it
  starts, never again for the same spell and never while it is already falling; an official
  warning is not repeated when the next bulletin says the same thing; from 22:00 to 7:00
  notifications arrive without a sound, except red warnings and sky reminders.
- **Alerts: a «Serious heat» idea**, and «Twelve dry hours» in place of «A night without rain»,
  which could fire in the morning.

### Fixed

- **A rule about today no longer fires twice a day**, once of them just after midnight.
- **Widget «In parole», one-row card: the "updated N hours ago" line is no longer cut off.**
  The form's budget spends the card's whole height — the number takes everything the place's
  line and the stale marker leave — so on a phone whose system font boxes taller than the
  layout's estimate the last line was measured short and sliced at its baseline. The budget is
  unchanged, so every size and position on the card is the one it was; the card now lends its
  6 dp top and bottom inset back as headroom, which a one-row card can do for free because it
  centres what it holds.

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
