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

- **An alert for a place of its own.** Under its hours, an alert of your own can now say where it
  holds: the place you have open, which it follows as before, or one of your saved places, which
  it watches whatever place you are looking at. Snow at the mountain house, read from the city.
  Its card says where («In Bormio»), answers «True right now» with that place's forecast, says its
  age when it is old, and «Last fired» is that place's; the notification carries the place's name,
  and Today's day lists only the alerts read on the place on the page. Remove that place and its
  alerts switch off rather than move to another place in silence: the card says the place is gone,
  and its switch asks where before it turns back on. Each place watched this way adds its own
  forecast update to the background job, only while such an alert is on.

- **Heat.** When the feels-like is set to reach 32 °C, the sentence at the top of Today says so
  ahead of time («Heat: feels like 36° around 15:00») or, once it is hot, until when it lasts;
  in the evening it says tomorrow's. A perceived 35 °C for two hours or more in the next twelve
  brings an «Intense heat» notification, once per hot day and place, under the severe-weather
  switch, with the day's temperature drawn, its hours and what to do in them, the thermometer's
  range and the UV. The morning and evening summaries carry the heat line when their sentence
  does not already say it. Your own alerts can watch «the feels-like high of the next 6 / 12
  hours». The bars are the German weather service's heat-warning levels (perceived 32 and
  38 °C).

- **Wind.** A gust of 70 km/h or more in the next twelve hours (100 km/h from 1 600 m up, the
  Swiss weather service's bars) is severe weather like a storm: the sentence at the top of
  Today says «Gusts up to 112 km/h around 08:00», the severe-weather notification rings once
  per gale and place with the next twelve hours of gusts drawn and the bar across them, and
  your own alerts' «severe weather in the next 6 / 12 hours» sees it. The height of the place
  is the forecast's own. The day keeps its word: a windy day is not a weather, and its gust
  line was already there.

- **The snow line.** A rainy or snowy day, opened in the week, says where its precipitation
  turns to snow, «Snow above 1,200 m», with what that means where you are: rain here and snow
  on the roads higher up, or snow down to here and rain lower down. It is the forecast's own
  line where the model carries one, and otherwise the freezing level less 300 m, said as
  «roughly». Only when the line is near enough to matter: within 1,500 m above your place, or
  below it. The evening summary says tomorrow's.

- **Units.** The wind can be read in km/h, mph, metres per second, knots or on the Beaufort
  scale (the force with its word, «6 Bft (strong breeze)»), and the pressure in hectopascals,
  inches of mercury or millimetres of mercury, from Settings. Every number follows: the tiles,
  the sentence at the top, the week's days, the notifications and their charts, and your own
  alerts' thresholds, readings and placeholders. Nothing changes unless you choose: km/h and
  hPa stay the defaults, and an alert written in one unit is the same alert in another.

- **Rain in the next two hours.** In northern Italy (down to Florence and Ancona), France (down
  to Toulouse and Nice), Switzerland, Austria, Germany, Slovenia, the Czech Republic, Slovakia,
  Croatia, western Poland and the United States, Today shows the next two
  hours a quarter of an hour at a time when rain or snow is on the way: a bar for every quarter,
  as tall as it is heavy, and under it from when to when and the heaviest moment. The sentence at
  the top says it in minutes, «Light rain in about 20 minutes, for about 30 minutes», or when the
  rain you see will stop. Tap the card for every quarter in words and where the forecast comes
  from. It appears only where the forecast is really computed every 15 minutes, never where it
  would be the hourly one split in four.

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
- **A silenced alert says so.** If the phone has silenced one of Chiaro's notification
  channels (set to «none» in the system settings, or by a long press on a notification), the
  Alerts screen now says which one, by the name the phone shows for it («This phone has
  silenced “Your alerts”: it cannot arrive»), with a button that opens that channel's own
  settings page; the alert's row fades like one that is off. Until now a silenced channel
  stayed silent for good, and the alert's card kept saying «never fired». The Sky says the same
  of its reminders: with a bell set and «Sky reminders» silenced, the page says so and opens
  their settings.
- **Alerts about tomorrow.** Your own alerts can now watch tomorrow: its high and low, its
  chance of rain, its millimetres of rain, its snow, its strongest gusts and its UV, under a
  «Tomorrow» group in «What to watch». A new idea, «Snow tomorrow», tells you when tomorrow
  brings at least 5 cm. An alert about tomorrow rings once for that day, from noon the day
  before, so there is still an afternoon to get ready; before noon its card says «True right
  now, it may ring from 12:00».
- **How much rain in the next hours.** Your own alerts can now watch the millimetres of rain
  and the centimetres of snow expected over the next 6 or 12 hours, not only the chance of
  rain: the difference between an umbrella and boots. The rain leaves out the snow's water, so
  a snowy night does not set off a rain alert, and the message's time is the hour it falls
  hardest.

### Changed

- **«The next 6 hours» start with the hour you are in.** In your alerts, the next 6 or 12 hours
  are now the hour under way and the ones after it, as the first cells of the hour strip on
  Today: rain falling right now counts. Until now that hour was left out, and on the hour the
  window held one hour too many.
- **«Intense heat» counts the hour you are in.** Its two hours of heat out of the next twelve
  now include the hour under way, and when the hottest is this one the notification says
  «right now» instead of an hour already begun.
- **The values in your message are words.** In the alert editor, a value added to your message
  («what set the alert off», «the temperature now») now shows as those words on a chip, in the
  line, instead of a code in braces; a backspace removes the chip whole. The message itself is
  unchanged, and so is what the notification prints.
- **Alerts' rows, like the Sky's.** The ready-made alerts and your own now sit in their rows
  the way the Sky's moments do, with the same room around the drawing and the same least
  height, so the two tabs' lists read as one app. «Last fired» says its day the way the bulletin
  does: «today at 7:12», «yesterday at 7:12», the day when it is further back. And «What to
  watch» in the alert editor is a sheet in four groups, now, the next 6 hours, the next 12 hours
  and today, with your current choice marked, instead of one long list of thirty-one.
- **Tonight's clouds, by layer.** The picture on the Sky screen's Tonight card now reads the
  forecast's cloud layers like Today's sky does: a night of high cloud is a veil the stars show
  through, not a bank of low cloud, and the picture slides between the hours with them. The
  numbers, the strip and the verdict are unchanged.
- **«Rain possible» instead of «Rain likely».** An hour the forecast gives 60% or more with no
  rain to measure now says «Rain possible» (and «Snow possible»): checked against the reports of
  24 airports, such hours were wet about half the time, not most of it.
- **Four icons that were missing.** Freezing rain and freezing drizzle have an icon of their
  own, the rain with a small ice crystal under the cloud, instead of plain rain: the one danger
  in the list that looked like ordinary weather in the hour strip. «Rain possible» and «Snow
  possible» draw a possibility, two lighter drops or flakes under the cloud, instead of the same
  icon as a drizzle or a snowfall that is really coming. Widgets and notifications follow.
- **The sky says what you see.** The word and the icon of each hour now come from how much of
  the sun's light the clouds take, layer by layer, instead of how much of the sky they cover: a
  high veil of thin cloud no longer reads «Overcast» while the sun casts shadows. Checked against
  satellite pictures of 23 airports over a month, the sky is now right in 66% of the daylight
  hours instead of 55%, and says «Overcast» under a visible sun three times less often.
- **Fog needs a second opinion.** Fog is now shown only when the air is close to saturation and
  at least one other weather model (ECMWF, the Met Office or NOAA's) sees it too. Checked against
  the airport reports of 24 airports over October and last winter, the app now invents 41% fewer
  fogs, and half as many over air that was clear for kilometres.
- **Fog without the model's own visibility.** The German ICON models, which the forecast over
  Italy is built on, stop forecasting the visibility on 30 November 2026. Where an hour has no
  visibility of its own, the app now reads the lowest the other models forecast (ECMWF, the Met
  Office, and the Dutch and Danish HARMONIE models, which take NOAA's place among the models
  asked) and shows fog only when two of them see it; the provider's own fog code never decides
  alone, because checked against the same airports it was wrong more often than right. Measured
  on the same two periods with the model's visibility removed, the app finds the fogs it finds
  today. The Visibility tile in the details is drawn only when there is fog, with «take care on
  the road» under it: the «haze» it used to print from the kilometres alone was right one time
  in five.
- **The blue hour has a picture of its own.** The sun just under the horizon with the first
  star over it, in the day's list, on the Sky screen and in the window widget. It used to share
  the plain star with the nautical twilight and the evening star.
- **The window's band under the words is the colour of the sky.** Under a clear sky it was
  grey and looked cut off from the picture; it is now a deeper shade of the sky itself, blue on
  a clear day, grey under an overcast, navy at night, as dark as before.
- **The morning and evening summaries open with the day's sentence.** The same one Today and
  the widgets say: «Umbrella around 14:00, then dry from 18:00», «Frost around 04:00, low -1°»,
  «Dry for the next 7 days», or an official warning when there is one. The low and the high
  follow it, and the chance of rain is said only when it is worth a word. Opened, the chart's
  evening is no longer a box: under the hours runs the same ribbon of the day's light as on
  Today, and the morning says the sunset and how much light the day has gained or lost on
  yesterday.
- **Tonight's card shows the real sky.** The top of the card on the Sky screen is the night's
  sky as the window widget draws it: the stars that are there, the moon as it looks and where it
  really stands among them, Venus and Jupiter, the Milky Way on a dark night, the hour's clouds,
  with the names of the brightest things and, at the top right, where it looks and the hour it
  is drawn for. Drag the strip and the sky moves through the night, the clouds growing and
  thinning between the hours; the hour you stop on stays, with a × to bring the whole night
  back; drag the picture sideways to look another way, clouds and all. «Behind the clouds», at the foot of the
  picture, shows the sky without this hour's clouds, and remembers your choice. The calendar
  ahead no longer repeats a full moon the agenda already names, and «What these events are»
  moved to the foot of the page.
- **An old forecast no longer prints a question mark on every moment.** When the last
  forecast is too old to judge by, the Sky screen's rows keep the moment and its time and
  drop the verdict, and one line under the list says how old the forecast is: «Updated 9 hours
  ago: the verdicts wait for a fresh forecast». The calendar ahead says it once too, instead of
  on every row.
- **Every line under a notification's chart in the form that fits one line.** On a phone where
  «Sunset 18:25 · 3 minutes less light than yesterday» wrapped, the air's line fell off the
  bottom. Each line now has shorter forms («Sunset 18:25 · 3 min less light», «Wind 9 km/h»,
  «Air 2 · Good») and the picture's body takes the first that holds one line, measured on your
  screen and font; the long text keeps the long ones.
- **The window says the hour its sky is drawn for.** At night, beside where it looks: «Looking
  SE at 21:30». The card repaints on the hour and at each sync, and the moon and the stars move
  on in between; the hour says which sky you are comparing with. By day the sun now stands
  above the words at the foot of the card, never in them.
- **A sky reminder shows the sky it is about.** Opened, the reminder for a golden hour, a blue
  hour or a clear night carries the picture of that moment's sky, as the window widget draws it:
  the moon with its phase, the stars and planets that are out, the clouds the forecast gives the
  hour.
- **Today in the shape of Sky and Alerts.** The top of Today reads in three steps: the
  temperature and the sky's word, then the day's sentence set as a sentence, then the ribbon.
  Every day of the week carries an arrow that says it opens, and an open day stands on a card of
  its own with its facts and its hours; the closed days draw their skies more quietly, so the
  temperatures are read first. The details are one group of tiles and «What changed» is a row
  per day, as the Sky groups its moments. The bar at the top, once you scroll, is the same as on
  Sky and Alerts, with a short fade from the sky's colours, and it keeps the place's full date
  whenever the phone has room for it beside the temperature. The tiles, the banners and the notice
  cards share the rounder corners and the type of the other two tabs.
- **The rest of the day speaks the Sky's language.** Its moments carry the Sky's names («Golden
  hour, evening»), the next one is marked «Next, today», and tapping one opens its page from the
  Sky: what it is, and where to look.
- **Alerts in the Sky's shape.** The alerts are grouped rows like the Sky's moments, and so are
  the rows of Settings. A warning in force is now the big card at the top of the official
  warnings, in the warning's own color, instead of a small banner. The hours of the morning and
  evening summaries and of the bulletin stand over their switches, in a column. A rule made from
  an idea keeps that idea's drawing, and its sentence is shorter and easier to scan: «the
  temperature now is between 5° and 26°», with the thresholds in bold.
- **Alerts opens with an answer.** One card at the top says whether there is a warning for your
  place, which window your phone may ring in next («Next, today: the evening summary, 18–23»)
  and draws the day of alerts under it, with the Sky's hour ticks and its line for now. «New alert» is now a button that floats over the page and
  opens the ideas, with «From scratch» first. It is the only way to add one: with no alert of
  your own yet, «Yours» says in one line what it is for. Deleting an alert no longer asks first: it shows «Undo», which brings
  it back exactly as it was.
- **Every alert says whether it is true now.** Each of your alerts shows a line such as «Not
  right now · it is 19.7°, it needs below 5°» or «True right now, but outside its hours until
  20:00». In the editor the same answer stands at the top and changes with every choice, with the
  message that would arrive: there is no «Try it now» button to press any more.
- **The guide follows the new pages.** Its Alerts chapter starts from the card at the top,
  and says what the line «True right now» tells you and what the warnings switch sends.
- **Undo on the Sky screen.** Adding or removing a moment from its page now shows a message
  with «Undo», and undoing a removal brings the moment back with its own reminder.
- **An easier place switcher.** On Sky and Alerts the whole title is now the button
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
  «The hours ahead» and «Sky moments» are no longer offered, and have since gone for good
  (see Removed).
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

### Removed

- **Less under the day of alerts.** The line «At any hour, when needed: Severe weather · Rain
  in the next hours · your own alerts» is gone: it repeated two switches just below it. The
  sentence about the quiet hours stays, without «(dashed)» in it. And the «Twelve dry hours» idea
  is gone from «New alert»: it rang at nine in the morning, up to twice a day, about nothing
  anybody had asked. Alerts you made from it stay, as they are.

- **The Journal.** The fourth tab is gone, with everything only it kept: the log of each
  update, the week's trend strip, «How the forecast did» and the history of warnings and sky
  moments. Its one sentence everybody used stays where it always was, on Today: «What changed»
  still says when the latest update moves a day ahead («Saturday improved: rain 70% → 30%»).
  The app keeps less on the phone as a result: three days of forecasts a place instead of a
  hundred updates.

### Removed

- **The «Sky moments» and «The hours ahead» widgets.** Retired from the picker on 13 October and
  now gone: the window draws the moments you follow under its picture, with the same rows, and
  «At a glance» shows the hours from two rows up. A card of either still on a home screen
  disappears with this version; place a window or a taller «At a glance» in its stead.

### Fixed

- **An alert on a saved place rings even when the place you have open cannot be read.** The
  background job used to stop at the place open in the app: with your position switched on and
  no fix yet (just enabled, location off, the permission taken away), or with the provider
  refusing that place's request, it never got to the alerts bound to your other saved places,
  nor to the places of your widgets, and nothing said so. It now reads them anyway; only a
  failure that passes (no network, the service overloaded) still waits and tries the whole
  round again.
- **An alert whose place you removed goes off at once.** It used to go off only at the next
  run of the background job, which with notifications denied and no widget never comes: the
  alert stayed on, and would have rung again on its own had the place been saved again. Now it
  goes off as soon as the «Undo» of the removal has passed, and at any edit of Alerts or
  start of the app.
- **A widget on another place stays fresh with no alerts on.** A widget showing a place other
  than the one open in the app is kept up to date by the background job, which fetched that place
  only when it also had alerts to check: with notifications off or denied, or every alert
  switched off, the widget was never refreshed in the background and aged until it showed its
  out-of-date mark. It is now refreshed with every run of the job, whatever the alerts.
- **The Sky with no place looks like the other tabs.** With every place removed, the Sky set
  «No place yet» at the left edge and the top, in a smaller type than Today and Alerts, which
  centre it. The three tabs now draw the same empty state.
- **The Sky asks for a fresh forecast itself.** Opening the Sky without passing by Today
  showed the last forecast the background job had fetched, up to two hours old, and once it
  was older than the stale bar every verdict of the night read «?» for the age, when one
  request would have answered. The page now refreshes silently on its own, as Today and Alerts
  do: the cached forecast first, a request once it is older than a quarter of an hour, never
  under battery saver.
- **Your alerts' «True right now» reads what Today reads.** The cards on Alerts checked each
  rule against the forecast as it had been fetched, even hours before: «Not right now · it is
  19.7°» off a reading from the morning while Today's hero said 15°, and «the high of today» was
  yesterday's when the last fetch was the day before, so the card and the row in Today's agenda
  could say opposite things about the same rule. Both now read the same report, prepared for this
  minute: the forecast for now in place of a reading over an hour old, today's row for «today».
  The page refreshes silently when its data is older than a quarter of an hour, as Today does,
  and once it is older than the stale bar the «Yours» title says so («Updated 3 hours ago»). The
  answer at the head of the editor follows the clock and the data too, not only the chips.
- **An alert switched back on rings again.** One of your alerts that had rung, switched off and
  on again, stayed silent until its condition had been false in between: while it was off it was
  never checked, so it never learnt the condition had passed. Switching it back on now re-arms it,
  as changing its threshold or its hours already did.
- **Two alerts never share one name, nor one past.** The editor now says, under the name field,
  when the name typed is already another alert's, and keeps the old name if the sheet is closed
  on it; «Last fired» is now the alert's own: renaming an alert keeps its history, and two alerts
  that happened to share a name no longer share a «last fired». What was recorded before this
  change is still read as it was written.
- **Thresholds in your units.** The slider that sets an alert's threshold moved in °C under a
  Fahrenheit label («53.6°», «55.4°») and in km/h under a Beaufort one, several positions to one
  force. It now moves in the unit you read: a degree Fahrenheit, a mile an hour, a metre a
  second, a knot, a force, a tenth of an inch of mercury. The alert is saved as before, so
  changing a unit changes no alert, and a threshold set in another unit reads as it is until you
  move it. Long sliders no longer draw a tick mark at every step.
- **Alerts promises no sound that cannot come.** The card at the top of Alerts named the
  bulletin's window, «Next, today · The bulletin · 15–17», for a place outside Italy too, right
  under «Official warnings are available for places in Italy», and «15–17» stood over the
  switch; at four o'clock, with today's bulletin out at 14:34, it said «Now · The bulletin».
  Now the bulletin's window belongs to the place: it is drawn only where a bulletin can come,
  and once today's is in hand the card names the next window that can still ring. With no place
  at all the screen says «No place yet» with the button that fixes it, instead of a page of
  switches about nowhere. The switch's own line says the hours in words, «Usually between
  15:00 and 17:00».
- **No fog over a clear morning.** The weather now said «Fog» whenever the model put the
  visibility under 1 km at this very moment, even for a fog the hours below it did not show
  (they need it to last into a second hour). Now the present follows its own hour: fog on top
  only when the hour strip has it too.
- **A place reached offline gets its name once you are back online.** Moving while offline
  kept the position page on the place it had weather for, and when the connection came back
  the new place arrived titled by its coordinates («45.48N 9.30E») until the next fix. The
  geocoder is asked again at the first moment online: on the move itself, and on the first
  forecast that lands for a page still titled that way.

- **An opened notification no longer ends in «…».** The lines under a notification's chart
  used to be cut by the phone when they ran past the space, with the last visible line losing
  its tail and the lines after it vanishing: a rainy morning's air quality, the evening's UV,
  an official warning's source. What fits is now decided before the notification is sent,
  from the phone's width and your font size, line by line: a line that does not fit is left
  out whole, never cut, and the full text stays for watches and cars.
- **The temperature no longer stops halfway into the top bar.** Scrolling Today used to slide
  and shrink the big temperature into the bar a little at a time, so a page scrolled only a bit
  left it half-shrunk across the bar's edge. Now it scrolls away with the sky, and the moment it
  goes under the bar the bar shows the temperature and the sky's icon together, with a short
  fade.
- **Closing a day of the week no longer crashes the app**, and an open day's temperature bar
  shows its whole scale again instead of only the coloured part.
- **Smoother scrolling through a day's hours.** Dragging the hours of an open day now holds all
  the moving weather icons on the page still, not only that day's, as scrolling the page does.
- **Easier to read on a bright sky.** The feels-like, the estimate's note and the sunrise and
  sunset under the ribbon were a paler white than the rest of the sky's text, which on a clear
  noon sky dropped them under the contrast a line of text needs. They are now the same white,
  told apart by their size.
- **The place name on Today is easier to tap.** The whole name and hour are one button, as
  large as a finger needs, and TalkBack reads it as one; the week's days say whether they are
  open and what tapping does.
- **An alert you edit starts over.** Changing an alert's thresholds or hours now re-arms it,
  so it can speak under its new conditions the same day; until now an alert that had already
  fired kept quiet until its old condition happened to turn false.
- **«What changed» no longer vanishes on a pull to refresh.** A refresh within a quarter of an
  hour of the last update updates it instead of writing a copy, so the section keeps comparing
  against the forecast before.
- **No more leftovers on the phone.** The memory of alerts that fired for a place you removed,
  or for an alert you deleted, is cleaned up at the next update; the offline copy of a place
  you follow is never thrown away to make room, whatever the number of places; and the copies
  kept for places you passed through are capped.
- **Widgets keep their settings on a new phone.** After a backup is restored, each widget
  comes back with its own place, look and arc settings instead of the defaults, and nothing is
  left behind under the old phone's widget numbers.
- **The right hour the night the clocks go back.** During the repeated hour (02:00 to 03:00 on
  the last Sunday of October in Europe) Today's current hour and the hours strip were one hour
  behind: the first 02:00, already over, was still read as the present.

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
