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

- **Drag the day.** The ribbon under the temperature is a handle: drag it through the hours
  still ahead and the sky above becomes the one forecast for that hour, with its temperature
  and its word. Let go and it is now again.
- **Fog and thunderstorms in the sky.** An hour in fog draws a fog, thicker toward the ground,
  with the sun as a pale patch; a thunderstorm darkens its clouds and flashes inside them.
  Until now the sky above the word «Fog» drew whatever the cloud cover said.
- **A meteor now and then** on the peak night of a strong shower, when the sky is dark and
  open, running away from the shower's radiant.
- **Point the phone at the sky.** In the explorer, on a phone with a compass, «Point the
  phone» hands the sky to the phone: hold it up and the page shows the sky behind it, turn
  and it turns with you, lean back and it looks up. Drag the sky and it is yours again;
  pinching and the two buttons only come closer; tap a thing and its sheet opens while the
  sky keeps following. The compass is read only while the switch is on, and the page says
  when it wants calibrating or when something nearby is pulling it.
- **Worth seeing, and the night of an event.** In the explorer, «Worth seeing» lists what the
  night holds: the moon, the planets that are up, the figures of stars anybody can find, the
  Milky Way, a shower's radiant, each with the hour it stands highest, where it stands then
  and the hours it is worth looking in. The night is at the head of the list: tonight, the
  night after, the night of an event ahead, or any other. Tap a row and the sky goes to that
  hour and turns to the thing. And under «Coming up», an event that has a night of its own
  carries «See that night's sky», which opens the explorer on that night, turned to it.
- **Touch a thing in the sky.** In the explorer, tap the moon, a planet, a star with a name,
  a constellation's name or a shower's radiant: the sky comes closer round it and a sheet
  says what it is and where it stands, in words, the sky of that hour, when it rises, when
  it stands highest and when it sets, and until when it is worth looking. A planet has a
  bell, «Tell me the evenings it is worth looking», and a shower «Tell me on the night of
  the peak»; both have a link to their page, and so has the moon.
- **Mars tonight, Saturn tonight.** Two new moments, sixty-two in all: the hours of the night
  each planet is high enough to look at, with a reminder if you want one. Mars says so on the
  months it is too faint to tell from the stars.
- **Mars and Saturn in the sky.** The drawn sky had Venus and Jupiter; it now has the four
  planets the eye can see, each where it really stands and named: Mars orange, Saturn a pale
  yellow, both as bright as they are that night.
- **The wind on a dial** in its tile, an arrow that flies with the wind.
- **Explore the sky.** «Explore», on the Sky's sky, opens the night on the whole screen, from
  the horizon to straight overhead: drag it any way to look around, pinch it or press the two
  buttons to come closer. The directions are written along the horizon, and the moon, the
  planets and the brightest stars are named where they stand. At its foot are the hour, with
  that hour's cloud, the night's ribbon to drag through the night, and the night itself: step
  to the one before or after, or pick any from a calendar. The hour's cloud dims the stars as
  it would, «Behind the clouds» shows the sky behind it, and past the end of the forecast the
  sky is drawn without clouds and says so. «Constellations» traces the eighty-eight and names
  them.
- **A switch for the sky.** Settings → Appearance → «Animated sky» holds the drawn sky still on
  its own, apart from the icons: for an older phone, or for a quieter screen. Until now the sky
  moved under the icons' switch, which did not say so.

### Changed

- **The first run opens on what Chiaro is.** A welcome page comes before any question: the
  name, its sentence, and three promises (a sentence before the numbers, the moments of the
  sky with their verdicts, free with no account). Then the place and the notifications, as
  before, in one frame where only the words slide and the buttons stay put; back from the
  place returns to the welcome. The notifications' page now names what a fresh install has
  switched on (the morning and evening summaries, severe weather, rain on the way and, in
  Italy, the official warnings), where it promised "your own alerts" that did not exist yet.
  The quiet way past reads «Later». No line of it ends on a word alone.
- **«Behind the clouds» is a look, not a setting.** The switch on the Sky's sky used to be
  remembered: left on, the page opened every covered evening on a starry sky under the
  verdict that said otherwise. It now lasts for the night you asked it on, while the app is
  open, and the page opens on the forecast's sky again. In the explorer the switch is that
  page's own: it opens as the picture was and changes nothing outside it.
- **Jupiter tonight keeps to evenings worth going out for.** At the very end of its season
  in the evening sky, and the first mornings of the next, Jupiter clears the roofs for a few
  minutes in a sky still light: about ten times a year the moment gave that as its window,
  with a reminder. It now says Jupiter is too low, as it does the weeks it is behind the sun.
- **A sky with more in it.** Clouds have a finer edge and change shape as they drift; at
  sunset the horizon is brighter toward the sun than away from it; the sun sends shafts
  through a broken sky; the moon has its seas and is turned as it really is; rain comes in
  gusts and snow has depth. A sky that closes over is grey with lights and darks of its own,
  at sunset too, where it used to turn one brown tone. The sky now glides from one hour's
  weather to the next instead of switching.
- **More sky behind the numbers.** The dark veil under the text wears the sky's own colour
  and is only as strong as the sky needs: at night there is none. The temperature is set
  lighter.
- **Sky opens on the night.** The page starts with tonight's sky from edge to edge, as Today
  starts with the day's: the place and the night's date on it, the verdict in large type where
  Today has the temperature, and the night as a ribbon to drag, like the day's. Scroll, and the
  place stays at the top with the verdict beside it. Until now the night was a card in the
  page, and its sky the smallest thing on a screen about the sky.
- **The Sky's moments are the rows of Today's agenda.** The hour stands at the start of the
  row, a moment's end under its start, the bell at the end and the verdict below. The first
  moment says how soon it comes; one that is happening says «Now» and how much of it is left.
  The next moment no longer has a card of its own, and the calendar ahead wears the same row.
- **«Add» is in the moments' title.** The floating button is gone: it stood over the hour and
  the direction of the moment you were waiting for.
- **Large type keeps a moment readable.** Past 150% of type the hour moves to a line of its
  own above the name, on Today's agenda and on the Sky, instead of squeezing the name into a
  few letters a line.
- **How close two bodies pass is said in full moons.** A close pass under «Coming up» read
  «3° apart»; it reads «about 6 full moons apart», a width in the sky everybody has seen.
- **A shorter page.** Your alerts that hold now share one row; a moment's start and end stand
  together beside it; each day of the week is one bar with a mark that says it opens, and its
  ribbon of sky comes with the day opened; the detail tiles are as tall as what they say.
- **The cloud tile agrees with the sky above it.** «Closed sky» is now read on the light the
  clouds take, as the word under the temperature is: a high veil over a broken deck no longer
  reads «Partly cloudy» at the top and «Closed sky» below.

- **A new icon**: the blue hour, the sun on the horizon under the first star, on the sky. The
  notifications' small icon is the same picture.
- **Clearer sky icons.** The meteor showers are a shooting star; the blue hour and the white
  nights show the sun just under the horizon with the first stars; full darkness and the Milky
  Way are stars without the moon; the full moon at dusk rises as a full moon; a lunar eclipse
  has the Earth's red shadow, so it no longer looks like any full moon; the zodiacal light is a
  cone of light over the horizon instead of a sun.
- **The icons hold still while you drag the day**, as they do while the page scrolls, so the
  screen no longer flickers under the finger.
- **A lighter sky on the phone.** A clear sky, where nothing moves, is no longer redrawn
  fifteen times a second, and the sky no longer glides at every minute for a move of the sun
  nobody sees: it glides when the weather changes, or when you drag the day.

- **A meteor shower's night is the whole night.** A shower that peaks in the small hours was
  a window from its peak to dawn, sixteen minutes for the Geminids of 2026, dated the morning
  after. It is now the night the peak falls in, from dark to dawn, dated the evening it
  begins on: the row, which stays under «Coming up» until that dawn, the reminder (which
  comes at nightfall and no longer before dawn), the radiant in the drawn sky.
- **Alerts opens on the place, like Today and Sky.** The page's header says the place first
  and «Alerts» under it, as the other two tabs say the place and then the day or the night.
- **«Add» is in the title of «Yours».** The floating «New alert» button is gone: it stood
  over the switches. «Add» sits beside the title of your own alerts, as it does beside the
  Sky's moments, and opens the same ideas.
- **Alerts breathes like Today.** The same air between its sections and inside its rows as
  on Today, and the same size for its drawings.
- **«A value in the message» is grouped.** The values an alert's message can quote are in a
  sheet under «Now», «Next 6 hours», «Next 12 hours», «Today» and «Tomorrow», as the list of
  things to watch is; it was one long list in a dialog.

### Fixed

- **«Not sure yet» where the forecast stops.** A moment that runs past the forecast's last
  hour, the night a week from now, was judged on the hours that were there and printed as if
  it were the whole of it. It now says the forecast does not reach that far yet.
- **A moment the forecast does not reach says so.** Under «The moments ahead» it showed
  «Not sure yet» and nothing else; the row now says why, as the calendar's rows do.
- **A reminder agrees with its row.** The reminder of a moment that lasts (the dark window,
  a shower's night) was judged on its first hour, and could say «Great» of a night the Sky
  called «So-so». It is judged on the whole of it, as the row is.
- **The Quadrantids in January.** They peak on the third or fourth of January, and from the
  first the app showed next year's, on their own night too. The row, the reminder and the
  radiant in the drawn sky are this January's.
- **«Adesso» on one line.** In Italian the word broke in two in the hour's column of a row
  that holds now, on Today's agenda.
- **A place chosen from Sky or Alerts stays chosen.** Coming back to Today put the place you
  had left back in its stead, sometimes after sliding halfway to the new one.
- **TalkBack says whether a switch is on.** The switches of Alerts and of Settings were read
  as switches with no state; and each of your own alerts' switches now says whose it is.
- **The day of alerts at a large type.** The hours under the strip at the head of Alerts were
  drawn over the sentence below them once the type was large.
- **Alerts on a twelve-hour clock.** The hours the alerts arrive in read «15–17» and «18–23»
  whatever the phone's clock, beside a bulletin «at 2:34 PM». They follow the phone now:
  «3–5 PM», «6–11 PM», and so do the quiet hours, the day strip and the guide.
- **Picking an hour on a phone held sideways.** The clock for an alert's hours did not fit a
  low window and was cut. It now comes with a button to type the hour instead, and a window
  too low for the clock shows the two fields alone.

## [2.0.0] - 2026-11-02

**Heat, gales and the snow line, alerts for any of your places, and three tabs instead of
four.** The forecast now says more of what changes a day: heat ahead of time, strong gusts,
where the rain turns to snow, the rain of the next two hours. Your alerts can watch tomorrow and
your other places, Today, Sky and Alerts read the same way, and a new widget, the window, shows
the sky of this hour. The Journal is gone, and three older widgets with it: that is why this is
2.0.

Android 13 (API 33) or newer. Installs over 1.1.0 and keeps your places, alerts and settings.
**What does not come with you**: the Journal and its history, and the «Sky moments», «The
hours ahead» and «The day's arc» widgets, which disappear from the home screen; place a window
or a taller «At a glance» in their stead (see Removed). Check the download with the `.sha256`
file beside the APK, and the signing certificate against the fingerprint in the
[README](https://github.com/fiorenzobrioni/chiaro#install).

### New

- **The window**, a new widget: the sky of this hour over your place, with the clouds, the sun,
  the moon as it looks and the stars and planets that are out, the brightest named, and
  tonight's verdict or the next moment of light. Taller, the moments you follow.
- **Heat.** A feels-like of 32 °C or more is said ahead of time at the top of Today, and an
  «Intense heat» notification rings when a feels-like of 35 °C lasts two hours or more.
- **Gales.** Gusts of 70 km/h or more (100 km/h from 1,600 m up) are severe weather, in the
  sentence and in the notification.
- **The snow line.** A rainy or snowy day, opened, says where the rain turns to snow, when that
  is near enough to matter where you are.
- **The open day's sentence.** An opened day of the week starts with its shape in one sentence:
  «Partly cloudy in the morning, clear from the afternoon».
- **Rain in the next two hours**, a quarter of an hour at a time, where the forecast is really
  computed every 15 minutes: northern Italy, France, central Europe and the United States.
- **Alerts for a place of their own.** An alert can watch one of your saved places, whatever
  place you have open.
- **Alerts about tomorrow, and about how much.** Tomorrow's high, low, rain, snow, gusts and UV;
  the millimetres of rain and centimetres of snow of the next 6 or 12 hours; a «Snow tomorrow»
  idea.
- **Units.** Wind in km/h, mph, m/s, knots or Beaufort; pressure in hPa, inHg or mmHg.
- **Tonight, hour by hour.** Drag along the night strip on the Sky screen and the card paints
  that hour's sky, clouds and moon.
- **A compass for where to look**, on the sky moments that have a direction.
- **A silenced alert says so.** When the phone has silenced one of Chiaro's notification
  channels, Alerts and Sky say which, with a button to its settings.
- **Split screen on BYD cars.** Chiaro carries the mark BYD's DiLink system looks for before it
  offers an app in split screen (#143, contributed by zuosus; not yet confirmed on a car). It
  needs a head unit on Android 13 or newer, such as DiLink 5.1 or DiLink 100: older DiLink units
  cannot install Chiaro.

### Changed

- **Today in the shape of Sky and Alerts.** The temperature and the sky's word, the day's
  sentence, then the ribbon; every day of the week opens on a card of its own.
- **The ribbon shows the sky**: the forecast's clouds hour by hour, the wet hours marked,
  sunrise and sunset under it.
- **A sentence on quiet days too**: when the next rain is due, when it is warmest, whether it is
  warmer or cooler than yesterday.
- **The sky says what you see.** An hour's word and icon come from how much light the clouds
  take, layer by layer: against satellite pictures, right in 66% of daylight hours instead of
  55%.
- **Fog needs a second opinion**, and keeps working after the German models stop forecasting
  the visibility on 30 November: 41% fewer invented fogs against airport reports. The
  Visibility tile shows only in fog.
- **«Rain possible» instead of «Rain likely»**, and four new icons: freezing rain, freezing
  drizzle, rain possible, snow possible.
- **Sky.** Tonight's card shows the real sky (stars, moon, planets, the hour's clouds), the next
  moment stands out, and adding or removing a moment can be undone.
- **Alerts opens with an answer**: whether there is a warning, and when your phone may ring
  next. Every alert says whether it is true right now; «New alert» is a floating button; the
  values in a message are words on a chip; thresholds move in your units.
- **The morning and evening summaries open with the day's sentence**, and a notification's
  lines always fit the screen.
- **Widgets**: one type scale, your launcher's corners, more on tall cards.
- **Settings, reviewed**, with a «Widgets» group; «My position» in Places opens at once.
- **Pressure says which way it is going**, and the chance of rain is printed from 20%.

### Removed

- **The Journal.** The fourth tab, with its log of updates, its trend strip, «How the forecast
  did» and the history of warnings and sky moments. «What changed» stays on Today.
- **The «Sky moments», «The hours ahead» and «The day's arc» widgets.** A card still on a home
  screen disappears; place a window or a taller «At a glance» in its stead.
- **The «Twelve dry hours» idea.** Alerts you made from it stay.

### Fixed

- **Alerts on saved places ring** even when the place you have open cannot be read, and an
  alert whose place you removed goes off at once.
- **Your alerts' «True right now» reads what Today reads**, and an alert edited or switched
  back on rings again.
- **Widgets on another place stay fresh** with no alerts on, and keep their settings on a new
  phone.
- **The Sky refreshes its forecast itself**, and an old forecast no longer prints «?» on every
  moment.
- **No fog over a clear morning**, and the right hour the night the clocks go back.
- **Closing a day of the week no longer crashes the app**, and «What changed» survives a pull
  to refresh.
- **A place reached offline gets its name** once you are back online.
- **Easier to read and to reach**: the sky's small text on a bright sky, the place name as one
  button, TalkBack reading the week as the screen shows it.

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
