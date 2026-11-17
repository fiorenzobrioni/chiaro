# STATES.md — Chiaro

What an hour, a quarter of an hour and a day **are**: the icon and the word the app shows, and
the rules that decide them. A living reference: it describes the engine as the code has it
today, every threshold with its reason and its measurement. The history of how each rule was
reached stays in the planning archive (see the end).

**Numbers are the model's; states are the app's.** Nothing here changes an amount, a
probability or a temperature on screen. The engine reads them and returns one state, which may
differ from the provider's `weather_code`.

**Changing a rule** means changing it here, in the code and in its test, in the same change,
and writing the decision in `PLANNING.md` with the measurement behind it.

Where it lives:

| What | Code | Test |
|---|---|---|
| The hour | `core/domain/.../WeatherStateEngine.kt` | `WeatherStateEngineTest` |
| The states the app adds (68, 69, 1061, 1071, 1095) | `core/domain/.../WmoCode.kt` | `WmoCodeTest` |
| The day, the current block, the series | `core/data/.../mapper/WeatherReportMapper.kt` | `WeatherReportMapperTest` |
| The quarter of an hour | `core/domain/.../NowcastEngine.kt` | `NowcastEngineTest` |
| The measurement | `tools/measure_states.py`, `tools/analyze_states.py`, workflow `Measure states` | data in `tools/measurements/` |
| Against what was seen (§11) | `tools/measure_against_metar.py`, `tools/analyze_against_metar.py` | `tools/measurements/metar-*.json.gz` |
| The witnesses (§12) | `core/data/.../mapper/Witnesses.kt`, `OpenMeteoWitnessApi` | `WitnessesTest` |

---

## 1. Why the app decides

Verified on 25 Sep 2026 in Open-Meteo's source (`WeatherCode.calculate`) and documentation,
then measured on 26 cities, 7 days, 4 342 hours (§9).

- **The code starts ten times below the probability.** Open-Meteo writes a drizzle code from
  0.01 mm/h and a snow code from 0.01 cm/h, but `precipitation_probability` is the share of
  ensemble members seeing **more than 0.1 mm**, and hourly amounts are rounded to 0.1 mm. A
  snowflake over «0%» is the provider's arithmetic, not a forecast: 88 hours of the
  measurement carried a precipitation code under 20%.
- **For the ICON family the code is DWD's `ww`**, relayed as it is: 19 hours said «overcast»
  over a measurable amount of the same response, and in Italy, France and Germany the sky code
  disagreed with the cloud cover in 10-29% of the hours (Cagliari 29%, Trento 27%, Milan 17%;
  0-1% elsewhere, where Open-Meteo derives the code from that same cloud cover).
- **Every shower under 1.3 mm/h is «drizzle»**, and 1 mm/h is «dense drizzle».
- **A thunderstorm code comes from CAPE and lifted index**, before and independently of any
  precipitation, so it appears over dry hours.
- **`freezing_rain` is not a variable** (the request answers HTTP 400): freezing rain is only
  known from the codes 56/57/66/67.
- **`rain` and `showers` change meaning with the model.** In the UK `rain` already holds the
  showers; in Svalbard `showers` holds convective snow (the same water in `showers` and in
  `snowfall`, code 85). The only reliable liquid is **total − snowfall / 0.7**, which held in
  all 585 wet hours of the measurement. `WeatherReportMapper.rainOf` (the minimum of the two
  readings) gives the right answer in both cases.
- **The fog code contradicts the visibility of the same response 68% of the times it says
  fog** (6 Sep 2026, 23 cities, 3 864 hours).

What the app cannot do, said plainly: with no observations and no history it cannot
**calibrate**, that is make the forecast more accurate. It can make what it shows
**coherent and readable**, which is what this engine does.

## 2. The thresholds

| Constant | Value | Reason |
|---|---|---|
| `MEASURABLE_MM` | 0.1 mm | The smallest amount a response states, and the one the probability counts from. Under it is a trace. |
| `PROBABLE_PCT` | 20% | `AlertEngine`'s floor for «Maltempo»; under it bulletins do not name precipitation. Under it an amount is a trace of the single model that the ensemble does not back. Measured against the METARs of 24 airports (§11): the best floor, with 30%, of every one tried, and better than IMPROVER's probability alone. |
| `LIKELY_PCT` | 60% | Where the US National Weather Service starts saying «likely». |
| `FOG_VISIBILITY_M` | 1 000 m | The WMO definition of fog, and the provider's own: a witness whose visibility is this or less votes for fog (rule 5). |
| `FOG_SATURATED_PCT` / `FOG_CALM_KPH` | 98% / 12 km/h | The hour's own model votes for fog when its air is at least this humid in a wind this light or lighter: saturation in a calm, how fog is diagnosed where a model has no visibility (Zhou and Du 2010, NCEP's multi-model fog; 12 km/h is about the 7 knots the US National Weather Service puts between radiation fog and the wind that mixes it away). On a plateau, §11. |
| `FOG_VOTES` | 2 | How many models must see the fog: never one model's word (17 Nov 2026, §11). |
| `FOG_MEAN_HUMIDITY_PCT` | 93% | The air the models agree on, the mean of every humidity of the hour without the highest and the lowest, under which a fog does not stand however many vote: a filter, never a vote, never printed (17 Nov 2026, §11). |
| `SNOW_CM_PER_MM` | 0.7 | Open-Meteo's ratio: 7 cm of snow = 10 mm of water. |
| `RAIN_SNOW_C` | 1.0 °C | Mean 2 m temperature where rain and snow are equally likely over northern-hemisphere land (Jennings et al., Nature Communications, 2018). Used only when the model gives no phase. |
| `LIKELY_PHASE_REACH_HOURS` | 3 h | How far a likely hour looks for a measured one to borrow its phase. |
| Drizzle under | 0.5 mm/h | The «a few drops» end of light rain. |
| Rain light / heavy | 2.5 / 7.6 mm/h | The AMS Glossary's intensities, and Open-Meteo's own floors for 61/63/65 and 80/81/82. |
| Snow moderate / heavy | 0.2 / 0.8 cm/h | Open-Meteo's own snowfall ladder (71/73/75). |
| Sky | 20 / 50 / 80% | Open-Meteo's cloud cover buckets (clear, mainly clear, partly cloudy, overcast), applied since 26 Oct 2026 to the share of the sun's light the cloud takes, not to the share of sky it covers (rule 6). |
| Layer opacity | low 1, middle 0.8, high 0.3 | What each layer takes of the light, against a low one's whole (the WMO cloud atlas: a high veil never stops the sun from casting shadows). The canvas uses the same numbers (`CloudLayers.lightTaken`). |
| Wet day | 1 mm or 3 hours | The Met Office's «wet day»; three hours keeps a long soft drizzle (§6). |

## 3. The hour

The input is the hour as a row shows it: the amounts, the probability and the provider's code
of the **interval** (Open-Meteo's next slot, «sum of the preceding hour»), and the temperature,
cloud cover, humidity and wind of the **instant** it starts, with the witnesses' visibility and
humidity for the same instant (§12). `best_match`'s own visibility is not an input since 17 Nov
2026: ICON's goes on 30 Nov 2026, and the rules are the same before and after.

The rules, in order; **the first that answers decides**:

1. **Thunderstorm**: the provider's 95/96/99, when probable (≥ 20% or unknown). The app has no
   CAPE, so it neither invents a storm nor removes one. Over an hour with no measurable rain it
   is **1095 «Temporali possibili»**, still a hazard. 96/99 stay «heavy», without hail. Known
   limit: the probability is of precipitation, not of lightning (Open-Meteo gives none), so the
   20% filter is the best available, not an exact one.
2. **Freezing rain**: the provider's 56/57/66/67, **at any probability**, as `AlertEngine`
   always had it: ice is dangerous in any amount.
3. **Measurable and probable precipitation** (≥ 0.1 mm and ≥ 20% or unknown). The phase comes
   from the model's own split: snow's water = `snowfall / 0.7`, liquid = total − snow's water.
   - **Rain and snow** when both parts are measurable: **68** (under 2.5 mm/h in all) or **69**.
     A part that is heavy on its own names the hour and keeps its warning: snow ≥ 0.8 cm/h is
     heavy snow, liquid ≥ 7.6 mm/h is heavy rain.
   - **Snow** when the snow's water dominates: **snow showers** (85, heavy 86 from 0.8 cm/h)
     when `showers` ≥ half the snow's water, otherwise 71/73/75 at 0.2 and 0.8 cm/h.
   - **Rain** otherwise: **showers** (80, 81 from 2.5, 82 from 7.6 mm/h) when the convective
     liquid, `min(showers, liquid)`, is at least half, **at any intensity**; otherwise drizzle
     (51) under 0.5 mm/h, light rain (61) to 2.5, rain (63) to 7.6, heavy rain (65) beyond.
     53 and 55 are never produced: 1 mm/h is light rain, not «dense drizzle».
   - **An improbable amount** (under 20%) is a trace of the model, and the hour falls to its
     sky, **except heavy snow**, which `AlertEngine` warns about at any chance: the engine
     removes a distortion, never a warning.
4. **Likely, with nothing to measure** (< 0.1 mm and ≥ 60%): **1061 «Pioggia possibile»** or
   **1071 «Neve possibile»** («probabile» until 26 Oct 2026: such an hour was wet 49% of the time
   against the METARs, §11; the identifiers keep their name). The phase is the hour's own when the model gave it one (a trace
   of snow or rain, a precipitation code), else the nearest hour within 3 hours with a
   measurable amount, else snow at ≤ 1 °C. A state of **possibility**: it draws the light
   precipitation, never the full one; it is not a wet hour (`WmoCode.likely`, outside
   `isPrecipitation`), so the headline does not say «it is raining», it does not label the
   day, and it carries no millimetres.
5. **Fog where two models see it** (17 Nov 2026, §11, «Two models»). Each model votes, the
   way the ensembles of the Met Office (IMPROVER) and of NCEP decide fog from several models:
   - **each witness** (§12) whose **visibility is 1 000 m or less**: one vote;
   - **the hour's own model** when its **air is at 98% or more in a wind of 12 km/h or
     less**: one vote. Saturation in a calm is how fog is diagnosed where a model has no
     visibility, and from 30 Nov 2026 the ICON family serves none over Italy. Its own
     visibility is **not read**, before or after that day: the rule is one.

   **Two votes** make a fog. Kept where the provider's code says fog; **invented only where two
   votes persist into a neighbouring hour of the same series**, because fog is not one hour long
   (writing fog into an hour whose neighbours are clear nearly doubled the week's fog/non-fog
   transitions: 10 in the provider's series, 18 repaired hour by hour, 14 with this rule). And,
   kept or invented, only under **air the models agree is near saturation**: the mean of every
   humidity of the hour, its own model's and the witnesses', **without the highest and the
   lowest**, at **93% or more** (the committente's idea, 17 Nov 2026). The mean is a filter, not
   a vote, and never a number on screen: as an estimate of the air it is worse than the own
   model alone (§11). **One model's word never makes fog**: not its visibility (Lissone, 26 Oct
   2026: 140 m under three other models 24–28 km away; Cavenago, 17 Nov 2026: ICON-D2's 240 m
   under the witnesses' 1.4 to 18 km, the airports around in 2–7 km of haze, the next village in
   sight), not its saturated air, and not the provider's code (§11: it says fog 4 361 times for
   955 seen, 2 425 of them over air clear for five kilometres). **Freezing fog (48)** at ≤ 0 °C,
   the fog that deposits rime. Precipitation comes before fog: in rain, low visibility is the
   rain.
6. **Sky** from the **share of the sun's light the cloud takes** (`CloudLayers.lightTaken`:
   each layer by its opacity, low 1, middle 0.8, high 0.3, combined at random and never more
   than the total), on the 20/50/80 buckets, not from the code. Until 26 Oct 2026 it was the
   total cover, and 90% of high veil read «overcast» under a sun casting shadows: against the
   satellite, the total called 55% of 4 380 daylight hours right and said overcast under a
   visible sun in 17.5% of them; the light calls 66% right and does it in 5.8% (§11). The sky
   drawn at the top of the page reads the same formula for its clouds' light, and the cloud
   tile names the layer, so the word, the picture and the number say the same thing. With no
   layers it is the total.

## 4. Missing inputs

Every input may be missing: an older cache entry, a model that does not carry it. A missing
input **switches off only the rule that needs it**, and that part of the decision falls back
on the provider's code, as the app did before the engine:

- no amount: the provider's precipitation code stands;
- no `snowfall`: the phase comes from the provider's code (snow or rain), else from the
  temperature at 1 °C;
- no `showers`: the provider's shower codes (80-82, 85-86) say whether the hour is convective;
- fog (rule 5) has **no fallback**: whatever is missing, fewer than two votes is no fog. No
  witnesses (the request failed, a cache entry from before them, a place outside KNMI's and
  DMI's domain from the fourth day on, where two remain) leaves one vote at most: no fog, one
  model's word. A model with no visibility, no humidity or no wind does not vote; no humidity
  anywhere leaves the mean nothing to read, and no fog. The provider's code never makes fog
  alone (27 Oct 2026; until then its verdict stood where the visibility was missing, the rule §11
  found worst of every one tried). `best_match`'s visibility missing, or a witness's copy,
  changes nothing: it is not read;
- no cloud cover: the provider's sky stands, else overcast; no layers: the sky is the total's;
- no probability: the hour counts as probable (the probability is model-dependent and nullable
  all the way to the screen).

## 5. The present and the quarter of an hour

- **The current block** passes through the same engine. Its amounts cover 15 minutes
  (`interval` 900) and are brought to the hour before the thresholds. It never says «likely»
  (`likely = false`: it describes now). **Its fog follows its row** (26 Oct 2026): the block
  may invent fog only where the series does, in the row it falls in (`fogPersists` on the
  rows' votes at that row and its neighbours); its own votes are the row's witnesses and its
  own model's, from the block's own humidity and wind, and its mean humidity is the block's
  with the row's witnesses' (rule 5, 17 Nov 2026). Its numbers are the model's, interpolated
  like the rows', not an observation: until 26 Oct 2026 the block invented fog from its own
  visibility alone, and Lissone at 09:45 read «Nebbia» on 380 m under
  code 3, in a model fog one hour long (4 km at 08:00, 140 m at 09:00, 1.2 km at 10:00), with
  «Coperto» in the strip below and a part-sunny sky with excellent visibility on the spot. It
  can say «Temporali possibili»: the storm warning is the present's.
- **A quarter of an hour** (the next-two-hours section, `NowcastEngine`) is the engine's state
  on the quarter as an hourly rate (× 4), with **the probability of its hour**: a quarter in an
  hour under 20% is a trace, as the hour is, so the section and the hour icons say the same
  thing. Freezing rain, which the amounts do not show, takes its hour's state in the measurable
  quarters only.

## 6. The day

The day's state comes from its own hours' states, not from `daily.weather_code`, which is the
`max()` of 24 codes («the most severe condition») and picks the least representative hour
there is: one 3 am fog code labelled a clear August day «Foggy», one night CAPE spike a week of
thunderstorms. Three steps, in order:

1. **A hazard the ensemble backs claims the day** (freezing anything, the heavy grades, every
   thunderstorm), whatever else falls, at night too: scoping it to daylight turned 8 night
   thunderstorms out of 56 days into «Overcast». «Backs» is `AlertEngine.severeBucket`'s test:
   a storm or downpour under 20% is dropped entirely, millimetres included.
2. **Rain that is really there labels the day**: the wet hours have to reach **1 mm** over the
   day or last **3 hours**; one hour of 0.1 mm at 1% used to print «Drizzle» over a whole day.
   Measured on 161 days: the pair moves 5 days out of 73, all of them one or two hours of
   0.0-0.4 mm; the weakest day it keeps is six hours and 0.6 mm, a day you take a jacket for.
   The phase that brought more water names the day; the heaviest intensity inside it picks the
   word (by `WmoCode.severity`, never by the WMO number, which is not an order of severity).
3. **Otherwise the daylight's sky**: the most frequent sky of the daylight hours, ties to the
   heavier one. A precipitation hour that did not earn the day votes with the sky of its own
   cloud cover.

Dates the hourly run does not reach keep the provider's code; a day with nothing to read has no
row.

## 7. How the screens use the states

- **The probability is printed from 20%** (`PROBABLE_PCT`), the floor under which the app never
  calls an hour wet: in the hour strip, the open day's strip and the week. Under it the cell
  stays empty; the spoken description still says the figure.
- **Wet means the engine's state** wherever a surface asks whether an hour is wet (the
  daylight ribbon, the sky's rain and snow, the next-two-hours section): no surface keeps its
  own threshold.
- **The open day's sentence reads the same states** (31 Oct 2026, `OpenDaySentence`): the sky
  of a part of the day is step 3 of §6 on that part's hours (the daylight's in the morning and
  the afternoon, all of them in the evening), the stretches by look, and a sky that holds all
  day takes the label's word; what falls is the wet hours' own states, the ribbon's marks, and
  a wet hour under a label that is a sky (it did not earn the day) is said lighter, «qualche
  goccia», so the sentence never names rain under an icon that does not. A label that says
  something falls with no wet hour on the row (the label reads the provider's slots, the row its
  own hours) draws no sentence.
- **Every reader inherits the state**: the headline, the alerts, the reader's rules, the
  widgets, the notifications. The engine lives in `:core:domain` and is called by the mapper
  precisely so that no screen reads the raw code and contradicts another.
- **The reader's rules sum the amounts on the same split** (15.10, 29 Oct 2026): the rows carry
  each hour's total and snowfall (interval values, from the slot after the row, as the
  probability), and «the millimetres of rain in the next 6 hours» is the sum of total −
  snowfall / 0.7, never under zero, over the window's hours — the hour under way and the five
  after it, as every rule on the next hours reads them; the snow is the sum of the snowfall. No `snowfall` in one hour of the window, and neither sum is said: the rule's number
  is not a state, and the provider's code cannot stand in for an amount.

## 8. The drawings

- **Rain and snow** takes `overcast-sleet`, which draws drops and flakes.
- **Freezing rain** takes the drawing of what falls (drizzle for 56/57, rain for 66/67) **with
  the crystal of `snowflake` under the cloud, bottom right, where the ice forms** (Fase 14.13,
  27 Oct 2026; `freezing-drizzle`, `freezing-rain`, composed by `tools/compose_falls.py`):
  Meteocons has no drawing of rain that freezes, and until then the state drew plain rain, the
  one danger of the table without a glyph of its own. The danger is still the word's and the
  «Maltempo» banner's.
- **A thunderstorm that rains** takes `thunderstorms-*-rain`; «Temporali possibili» keeps the
  lightning without drops.
- **Rain possible** takes the drizzle's cloud with one drop fewer and the two left lighter;
  **snow possible** the snow's cloud with one flake fewer, lighter (`rain-possible`,
  `snow-possible`, same tool and date): a possibility drawn as such, where until then a 60%
  chance and a drizzle that falls were one drawing in the hour strip. Lighter is 60% of the
  ink, or as light as 3:1 on the ground allows (DESIGN §10): the flakes on a light ground go
  only to 80–85%, and there the count says it more than the colour.

## 9. The measurement (25 Sep 2026)

26 cities chosen by phenomenon, 7 days, 4 342 hours as the app shows them; 585 with ≥ 0.1 mm
(13.5%). Raw data: `tools/measurements/states-2026-09-25.json`. Disagreements in the raw data:

| | hours | % |
|---|---:|---:|
| A. precipitation code under 20% | 88 | 2.0 |
| B. sky or fog code over ≥ 0.1 mm of the same response | 19 | 0.4 |
| C. dry (< 0.1 mm) at ≥ 50 / 60 / 70% | 103 / 56 / 20 | 2.4 / 1.3 / 0.5 |

The engine changes the word of **795 hours out of 4 342 (18.3%)**:

- **Sky, 472 hours**, a European fact (the ICON `ww` above). Most frequent: mainly clear →
  clear 161, partly cloudy → mainly clear 104, partly cloudy ↔ overcast 64.
- **Rain and showers, 244**: drizzle → showers 120 (Reykjavik, 0.4 mm/h convective), drizzle →
  light rain 95 (London, 0.9 mm/h), drizzle → sky 71 and other wet → sky 13 (under 20%), sky →
  rain 9 (case B: Naples, 0.2 mm at 98% under «overcast»).
- **Likely, 56** (case C from 60%): Bari, Oslo, Tokyo, Singapore; two of snow in Longyearbyen.
- **Fog, 23**: 18 become freezing fog (Longyearbyen, −4 °C), 5 drizzle hours under 20% with
  visibility ≤ 1 km become fog (Singapore).
- **Thunderstorm (14) and snow (25)**: unchanged.

The Kotlin engine re-run on the same hours matches the Python prototype in 4 339 of them; the 3
differences are the intended rounding tolerance (`measurable` accepts 0.0999… as 0.1).

## 10. Limits and what is still open

- **Not measured, because September has no winter**: rain and snow together, a thunderstorm
  without rain, liquid rain at ≤ 0 °C. The rules for the first two are written and tested
  without a measurement behind them. Re-run `Measure states` between December and February.
  The phase has since been measured against a winter's METARs (§11): of 4 995 hours,
  `best_match` called 92 rain where rain and snow fell together and 45 «both» where only snow
  did; the rule for rain and snow together stands, the thunderstorm without rain is still
  unmeasured.
- **Deduced freezing rain** (measurable liquid at ≤ 0 °C, with the 20% floor the provider's own
  freezing rain does not have) is decided and **off** until that winter measurement: a deduced
  state that fires a «Maltempo» alert would be the app's most expensive false alarm.
- **Out, for now**: hail (no Open-Meteo model forecasts it), ground ice after rain (a sentence
  or an alert, not an icon).
- **Mist (1–5 km), measured and left out** (26 Oct 2026, §11): the model's visibility between 1
  and 5 km is right about mist in 22% of the hours it says it (741 of 3 400), and misses 69% of
  the mist seen; a humidity or a witness does not mend it. A state wrong in 78% of the hours it
  is drawn is the screen lying; it comes back if a model with a better visibility diagnostic does.
  The visibility tile followed on 27 Oct 2026: drawn only in fog, the engine's state, never a
  «haze» line from the kilometres alone. Since 17 Nov 2026 it prints the model's kilometres only
  where they are a fog's, 1 km or less: fog is two models' votes, the model's own visibility is
  not one of them, and «4 km — prudenza in strada» would be the tile contradicting itself;
  otherwise, and where the model has none (ICON from 30 Nov 2026), it prints the state's word.
- **The phase, measured for a vote and left to `best_match`** (§11): over 4 995 hours of
  precipitation seen and forecast, `best_match`'s rain, snow or both is right 94% of the time,
  the witnesses' majority 95%. The engine's split stays the model's.
- **The Sky's verdict reads the cover, not the fog: measured, and left so** (16 Nov
  2026, `tools/analyze_against_metar.py --fog-cover`, on the October and the winter archives of
  §11). A moment's verdict («great», «so-so», «no chance») is the mean cover of its window and
  the chance of rain; whether an hour is fog is not one of its inputs, and since that day the
  Sky's own picture draws the fog over it. Where the hour has a visibility of its own the two
  agree by themselves: of the 2 191 hours rule 5 calls fog, `best_match`'s cover is over 65%
  in 98.2% (the model counts its fog as low cloud), 26 to 65% in 1.5%, 25% or under in 0.3%.
  Where DWD drops the visibility and the fog is read from the witnesses, the fog and the cover
  come from two models: of 2 123 fog hours the cover is over 65% in 93.0%, 26 to 65% in 5.5%
  (116 hours), 25% or under in 1.5% (32). In those 148 hours the airport saw clear air, five
  kilometres or more, in 84, mist in 25 and fog in 39: a rule that closed the sky on every
  hour in fog would be wrong about more of the hours it changes than it mends, because under
  an open sky the fog is more often the engine's mistake than the cover's. Counted per hour,
  where the verdict reads a window's mean, so an upper bound. What leaving it costs: from
  December, in ICON's countries, in about 7% of the hours the app calls fog a page draws the
  fog and a moment's verdict does not say «no chance», and in a quarter of those the fog was
  there. No rule, the committente's decision on these numbers the same day. **Re-run on the
  rule of 17 Nov 2026** (two models, everywhere; the same tool, its first pass): of 1 898 fog
  hours the cover is over 65% in 91.8%, 26 to 65% in 5.8% (110), 25% or under in 2.4% (45);
  in those 155 the airport saw clear air in 83, mist in 26 and fog in 46. The same picture,
  8% of the fog hours instead of 7%, and the same decision stands.
- **The sky's measurement has no winter**: the satellite archive starts after January 2026.
  Re-run `tools/measure_against_metar.py` on a winter when it has one; the fog has both.

## 11. The measurement against what was seen (26 Oct 2026)

§9 measured what the provider says against itself. This one measures the states against the
sky: what each rule, and each variant of it, would have said hour by hour, against what was
observed. `tools/measure_against_metar.py` downloads, for 26 airports chosen by phenomenon
(the Po valley first, for its fog, then the regions where `best_match` uses UKMO, AROME,
HARMONIE, MET Nordic and ECMWF, and the snow of the north; Bari and Catania since step 14.4,
for the south that ICON-2I would serve) and for two periods, **October 2026** (26 Sep –
25 Oct) and **winter** (1 Dec 2025 – 31 Jan 2026):

- what each model had forecast for those hours: Open-Meteo's archive of the first hours of every
  run, for `best_match` and the candidate witnesses (raw data, `tools/measurements/metar-*.json.gz`);
- the **METARs** of the same airports (the Iowa Environmental Mesonet's archive): visibility and
  phenomena, the truth for fog, mist and the phase;
- the **solar radiation seen by satellite** over the same point (Open-Meteo's satellite archive,
  EUMETSAT): the truth for the sky, as the share of the clear-sky light that reached the ground
  (over 0.7, the sun dominates; under 0.4, it is gone), because a European METAR reports only the
  clouds under 1 500 m and does not say whether the sun was out;
- for the Italian airports, **the previous runs** of `best_match`, ICON-EU, ICON-D2 and ICON-2I
  (Open-Meteo's Previous Runs API, step 14.4): what each said for the same hours 24 and 48 hours
  earlier, the app's day 2 and day 3. They carry the temperature, humidity, total cloud, amounts
  and code per lead day, not the visibility nor the cloud layers.

`tools/analyze_against_metar.py` scores the rules on the app's own rows (instants from the hour's
slot, amounts from the next), against the METAR within 30 minutes and the satellite's hour. Known
limits: the archive is the first day of each run, not the fifth (days 2 and 3 exist for Italy
only, from the previous runs); it has the precipitation probability for `best_match` only, so
the 20% floor is not in any comparison between models; an airport is not a city centre.

**Fog**, 41 873 dry hours with a METAR, 1 410 of them foggy (under 1 km):

| Rule | Said | Right | Missed | False (over air clear to 5 km) | CSI |
|---|---:|---:|---:|---:|---:|
| Until 26 Oct 2026 (persistence) | 3 144 | 887 (63%) | 523 | 2 257 (1 556) | 0.24 |
| + own humidity ≥ 97% | 2 420 | 824 (58%) | 586 | 1 596 (953) | 0.27 |
| + one witness (< 1 km or ≥ 97%) | 2 426 | 783 (56%) | 627 | 1 643 (1 074) | 0.26 |
| + most witnesses (≥ 95%) | 1 870 | 691 (49%) | 719 | 1 179 (700) | 0.27 |
| **+ own humidity ≥ 95% and one witness** | **2 076** | **753 (53%)** | **657** | **1 323 (764)** | **0.28** |
| Own humidity ≥ 95% alone (no witnesses) | 2 612 | 853 (60%) | 557 | 1 759 (1 078) | 0.27 |

(The tables of this section were decided on 24 airports. Since step 14.4, 27 Oct 2026, the files
carry Bari and Catania too, and a re-run reads 45 605 dry hours and 1 442 fogs: today's rule says
2 093 / 759 / 1 334, CSI 0.27, the rule chosen in 14.2 2 019 / 764 / 1 255, CSI 0.28. The order of
the variants does not change; the tables keep the numbers they were decided on.)

The rule chosen takes the false fogs down by 41% and the ones over clear air by half, and keeps
85% of the real fogs the old rule caught. Fog stays the hardest state there is (a CSI of 0.28 is
what it is; the Met Office calls a model's visibility «inherently poor»): the gain is in no
longer inventing it where the air is plainly clear. Where the old rule went most wrong:
Longyearbyen (527 hours said, 9 seen), Zürich (458 / 163), Munich (493 / 170), Milan Linate in
October (28 / 0).

**Fog without `best_match`'s visibility** (27 Oct 2026, Fase 14, step 14.2). DWD removes the
visibility from ICON, ICON-EU and ICON-D2 on 30 Nov 2026, and `best_match` over Italy is the
ICON family for the first five days (PLANNING, 14.1): from December the hour's own visibility
there is null, or filled by Open-Meteo with the largest model that carries one, IFS at 9 km.
The same 41 873 dry hours and 1 410 fogs, scored with `visibility_best_match` **not read at
all**, on what remains: the hour's own code and humidity, and the witnesses' visibility and
humidity. Two candidate witnesses join IFS, UKMO and GFS: the HARMONIE-AROME Europe of KNMI and
of DMI (5.5 km, with a visibility; the same series in 29% of the hours and two runs otherwise;
absent over Palermo, Cagliari, Madrid, Athens and Longyearbyen). A witness «sees fog» as in
rule 5: under 1 km, or its air at ≥ 97%. «Lent» is a visibility the hour does not have taken
from a witness's series, with rule 5's own shape on it: under 1 km, kept by the hour's own code
or persisting into a neighbouring hour of that series.

| Rule, with no visibility of the hour's own | Said | Right | Missed | False (over air clear to 5 km) | CSI |
|---|---:|---:|---:|---:|---:|
| Today's rule with its own visibility (the reference, above) | 2 076 | 753 (53%) | 657 | 1 323 (764) | 0.28 |
| (i) The provider's code alone (§4, «no visibility», until 14.3) | 4 361 | 955 (68%) | 455 | 3 406 (2 425) | 0.20 |
| (i) The code, own humidity ≥ 95%, one witness | 2 835 | 800 (57%) | 610 | 2 035 (1 284) | 0.23 |
| (ii) Own humidity ≥ 97% and one witness of the four, no visibility of any kind | 3 153 | 907 (64%) | 503 | 2 246 (1 411) | 0.25 |
| (ii) Own humidity ≥ 95% and two witnesses of the four | 2 951 | 863 (61%) | 547 | 2 088 (1 282) | 0.25 |
| One witness's visibility alone, persistent: IFS | 3 179 | 683 (49%) | 718 | 2 496 (1 918) | 0.18 |
| UKMO | 1 214 | 467 (33%) | 943 | 747 (450) | 0.22 |
| GFS | 707 | 70 (5%) | 1 340 | 637 (535) | 0.03 |
| KNMI | 620 | 265 (19%) | 1 112 | 355 (182) | 0.15 |
| DMI | 711 | 338 (25%) | 1 025 | 373 (179) | 0.19 |
| (iii) IFS's visibility lent, own humidity ≥ 95%, one other witness (UKMO or KNMI) | 1 481 | 570 (41%) | 831 | 911 (522) | 0.25 |
| (iii) The lowest of IFS, UKMO and GFS lent, own humidity ≥ 95%, two witnesses | 1 846 | 643 (46%) | 767 | 1 203 (724) | 0.25 |
| (iii) The lowest of IFS, UKMO, KNMI and DMI lent, own humidity ≥ 95% | 2 538 | 825 (59%) | 585 | 1 713 (1 053) | 0.26 |
| **(iii) The lowest of IFS, UKMO, KNMI and DMI lent, own humidity ≥ 95%, two witnesses** | **1 996** | **751 (53%)** | **659** | **1 245 (691)** | **0.28** |
| (iii) The same, persistence alone (the own code never kept) | 1 971 | 743 (53%) | 667 | 1 228 (684) | 0.28 |
| (iii) The same, own humidity ≥ 97% | 1 629 | 668 (47%) | 742 | 961 (510) | 0.28 |
| (iv) IFS's visibility in place of the hour's own, today's rule as it is, IFS voting (Open-Meteo's fill, unrecognised by the mapper) | 1 934 | 624 (45%) | 777 | 1 310 (841) | 0.23 |
| (iv) The same, IFS not voting | 1 438 | 551 (39%) | 850 | 887 (511) | 0.24 |

By period and by place, as said / right / false, with the CSI:

| | Today's rule | The code alone | **The rule chosen** | IFS's fill under today's rule |
|---|---|---|---|---|
| Winter, 27 646 h, 1 257 fogs | 1 824 / 671 / 1 153, 0.28 | 3 730 / 880 / 2 850, 0.21 | **1 830 / 689 / 1 141, 0.29** | 1 724 / 572 / 1 152, 0.24 |
| October, 14 227 h, 153 fogs | 252 / 82 / 170, 0.25 | 631 / 75 / 556, 0.11 | **166 / 62 / 104, 0.24** | 210 / 52 / 158, 0.17 |
| Po valley (7 airports), 13 145 h, 620 fogs | 734 / 265 / 469, 0.24 | 1 677 / 445 / 1 232, 0.24 | **806 / 283 / 523, 0.25** | 805 / 236 / 569, 0.20 |
| Elsewhere, 28 728 h, 790 fogs | 1 342 / 488 / 854, 0.30 | 2 684 / 510 / 2 174, 0.17 | **1 190 / 468 / 722, 0.31** | 1 129 / 388 / 741, 0.25 |

Per airport, the rule chosen against today's (said / right): Malpensa 62 / 21 against 105 / 40,
Bergamo 49 / 11 against 50 / 14, Munich 201 / 76 against 343 / 126, Zürich 362 / 121 against
403 / 146; Turin 40 / 20 against 26 / 15, Bologna 194 / 35 against 133 / 31, Verona 205 / 86
against 174 / 62, Venice 178 / 88 against 147 / 80, Vienna 171 / 109 against 151 / 96, Oslo
117 / 38 against 59 / 22. What goes with the hour's own visibility is ICON-D2's 2 km at the foot
of the Alps; what comes is the valley floor, and every fog the own model alone wrote over
clear air. The code alone would be the worst rule there is: it catches the most (68%) and says
fog 4 361 times for 955 seen, 2 425 of them over air clear for five kilometres, the Lissone error
at scale, so it never decides alone (Fase 14, decision of 27 Oct 2026).

**The rule chosen, in the engine from 14.3 (27 Oct 2026) to 17 Nov 2026**, when two models
replaced it (below, «Two models»). The hour's visibility is its own where
the response carries one of its own (not a witness's copy, below); otherwise the lowest the
witnesses forecast for that instant. Fog at ≤ 1 km on that series, kept by the own code or
persisting into a neighbouring hour of the same series; confirmed by the own humidity at
≥ 95% and by the witnesses that see it: **one** when the visibility is the hour's own (today's
rule, 0.28), **two** when it is lent, the lender being one of them (0.28 on the same hours,
with 78 fewer false fogs and 73 fewer over clear air). The witnesses become IFS, UKMO, KNMI and
DMI: GFS's visibility is right in 5% of the fogs seen (CSI 0.03) and adding it to the four costs
two points (0.26). KNMI and DMI are one run in 29% of their hours and count twice there; the
numbers above include it. Four models and four variables weigh 1.6 calls a fetch against 1.2.

**The copies, per variable.** How often `best_match`'s visibility is exactly a witness's, hour
by hour, against how often the mapper's test (temperature and cloud, §12) recognises that
witness as `best_match`: IFS 8 886 hours, 2 208 recognised (Longyearbyen, where IFS is
`best_match` whole) and **6 678 not: Oslo, Stockholm and Helsinki, every hour of both periods**,
where MET Nordic serves the temperature and has no visibility, so Open-Meteo's fill from IFS is
already there, and today's vote is IFS confirming itself (row (iv), which at Oslo is today's
rule: 59 said, 22 right); DMI 2 807 (Keflavik, whole, recognised); KNMI 1 518 (Amsterdam a third
of the hours, Keflavik where KNMI is DMI, none recognised); UKMO 131 and GFS 25, chance; ICON-EU
13 449 (the south of Italy, Madrid, Athens: recognised in every winter hour and in a third of
October's, the elevation correction of 14.1). So the recognition is **per variable** (14.3): a
visibility equal to a witness's for the six hours either side is that witness's, the hour has
none of its own, and the number counts once — as the witness's, among the lowest the hour
borrows, with two witnesses to see the fog and that one among them (the row chosen above, at
every hour). Needed today over the north of Europe and over Marseille, not only in Italy from
December. Since 17 Nov 2026 `best_match`'s visibility is not read at all, so a copy can no
longer count twice, and the mapper's test per variable is gone with it.

**The engine re-run on the same hours** (14.3, 27 Oct 2026), through the mapper's own
`Witnesses` and the two archives: with the hour's own visibility never read it matches the
prototype in every one of the 41 873 hours (1 996 said, 751 right, 1 245 false). As it ships —
the own visibility where it is really the hour's, the witnesses' lowest where it is a copy or
missing — 2 182 said, 801 right, 1 381 false, CSI 0.29, of which 217 said on a lent visibility
with 82 right: Oslo, Stockholm and Helsinki, where today's rule, IFS confirming its own fill,
said 113 for 48 right of 155 seen (CSI 0.22 against 0.28; 47 false over clear air against 93).
The same hours with the own visibility read everywhere and the four witnesses voting, the fill
unrecognised, score 2 078 / 767 / 1 311: KNMI and DMI in GFS's place catch 14 more fogs and
say 12 fewer false ones than the three of 26 Oct (2 076 / 753 / 1 323).

**Two models** (17 Nov 2026, the committente; `tools/analyze_against_metar.py --fog-votes`).
Cavenago di Brianza, that morning at 10:00: the hero said «Nebbia 7°» on ICON-D2's 240 m, with
the four witnesses at 1.4, 18, 5 and 2.4 km; the airports around saw 2 to 7 km of haze (Orio's
SYNOP 91% of humidity against ICON-D2's 98%), and the committente, outside, the next village.
The one vote that let it stand was DMI's humidity at exactly 97%, under a visibility of its own
of 2.4 km. Asked for a rule ready for 30 Nov 2026 and the same before it, measured on the 26
airports, 45 605 dry hours, 1 442 fogs, with `best_match`'s visibility read by no row but the
references:

| Rule | Said | Right | Missed | False (over air clear to 5 km) | CSI |
|---|---:|---:|---:|---:|---:|
| The rule of 27 Oct as it ran, ICON's visibility read | 2 205 | 808 (56%) | 634 | 1 397 (789) | 0.285 |
| The rule of 27 Oct from December, ICON's gone | 2 019 | 764 (53%) | 678 | 1 255 (701) | 0.283 |
| **Two models: witnesses ≤ 1 km, the own model at ≥ 98% in ≤ 12 km/h; mean ≥ 93%** | **1 807** | **747 (52%)** | **695** | **1 060 (579)** | **0.299** |
| The same, the own model voting with its visibility where it has one | 1 706 | 730 (51%) | 712 | 976 (505) | 0.302 |
| The same, persistence alone (the code never keeps) | 1 729 | 726 (50%) | 716 | 1 003 (548) | 0.297 |
| The same, without the mean | 2 097 | 790 (55%) | 652 | 1 307 (764) | 0.287 |
| 99% / 12 km/h / mean 94% (the first proposal) | 1 562 | 682 (47%) | 760 | 880 (461) | 0.294 |
| Days 4–7, only IFS and UKMO: the rule of 27 Oct from December | 1 646 | 624 (43%) | 818 | 1 022 (585) | 0.253 |
| Days 4–7, only IFS and UKMO: two models | 1 728 | 668 (46%) | 774 | 1 060 (634) | 0.267 |

By slice, CSI of the rule of 27 Oct as it ran / from December / two models: Po valley (7
airports, 620 fogs) 0.249 / 0.248 / **0.254**; elsewhere (822 fogs) 0.308 / 0.310 / **0.336**;
winter 0.285 / 0.288 / **0.303**; October 0.283 / 0.241 / **0.255**. Against the rule it replaces
as it would run from December: 16% fewer false fogs, 17% fewer over clear air, 2% fewer real
ones caught, and ahead in every slice. Against the rule as it ran with ICON's visibility, ahead
everywhere but October (0.255 against 0.283, on 153 fogs), and ICON's visibility goes anyway.

- **Why not the own visibility where there is one** (0.302): the gain is all in Italy before
  30 Nov 2026, where ICON's visibility goes; elsewhere the two score the same (0.336), and a rule
  that read it would change on that day. One rule, before and after, was the brief.
- **The thresholds sit on a plateau**: 98–100% for the own model, 8–16 km/h, 93–95% for the
  mean, CSI 0.285 to 0.299 everywhere, and 98 / 12 / 93 is ahead of the first proposal on every
  slice (all, Po valley, elsewhere, winter, October). 100% loses October (0.21): a model rarely
  writes it.
- **The mean is a filter, not a number**: as an estimate of the air it is worse than the own
  model alone, 5.58 points off the METAR's humidity against 5.40 (Po valley 6.06 against 5.33;
  with the METAR at 93% or more, 5.23 against 4.35, and in the Po valley 5.74 against 4.39),
  because the witnesses are coarser (9–10 km against ICON-D2's 2) and a mean can only pull
  saturated air down. Where two models already see the fog, it takes 247 false fogs off for 43
  real ones. It decides nothing else and reaches no screen.
- **The witnesses' humidity vote is gone**: in the rule of 27 Oct a witness «saw fog» at 97%
  whatever its visibility, and that vote bought 169 real fogs for 559 false ones; it is what
  kept Cavenago's.
- **Days 4–7**: KNMI and DMI run three days, then IFS, UKMO and the own model remain, and two
  of three must see it: 0.267 against 0.253, measured by dropping the two from day-1 hours (the
  archive has no later lead with a visibility).
- **One series counted twice**: KNMI and DMI are the same run in part of their hours; where they
  were the only two votes of a fog said, 5 hours of 1 807 (1 right). Left so.
- **The engine re-run on the same hours**: the Kotlin engine through the mapper's own
  `Witnesses`, every hour of the two archives, matches the prototype in all 45 605 (1 807 said,
  747 right, 1 060 false; 0 differences).

**Sky**, 4 380 daylight hours of October with the sun above 15°, dry and without fog, at the 23
airports the satellite sees:

| Sky from | Right | «Overcast» under a visible sun | «Sunny» under a closed sky |
|---|---:|---:|---:|
| The total cover (until 26 Oct 2026) | 55% | 17.5% | 0.1% |
| Low + middle layers | 66% | 6.9% | 0.6% |
| max(low, middle) | 68% | 5.6% | 0.8% |
| **The light the layers take (1, 0.8, 0.3)** | **66%** | **5.8%** | **0.5%** |

The light is chosen over max(low, middle), two points behind it, because it is the formula the
canvas already lights its clouds with: the word and the picture come from one arithmetic. The
ranking holds with the satellite's thresholds moved to 0.6/0.3 and 0.8/0.5.

**Mist** (1–5 km, where the engine says no fog), 2 377 hours seen: the model's visibility says it
3 400 times and is right 741 (CSI 0.15); with a humidity 0.15, with a witness 0.10. Left out (§10).

**Rain** (26 Oct 2026, measured with `best_match`'s probability as the archive kept it, downloaded by
the workflow `Measure against METAR`), 47 of the 48 airport-periods: an hour is wet when a METAR inside
it reports something falling at the station.

| An hour is wet when | Said | Right | Missed | False | CSI |
|---|---:|---:|---:|---:|---:|
| ≥ 0.1 mm, any probability | 8 033 | 5 649 (63%) | 3 249 | 2 384 | 0.50 |
| **≥ 0.1 mm and ≥ 20% (the engine)** | **6 756** | **5 110 (57%)** | **3 788** | **1 646** | **0.48** |
| ≥ 0.1 mm and ≥ 30% | 6 336 | 4 907 (55%) | 3 991 | 1 429 | 0.48 |
| ≥ 0.1 mm and ≥ 50% | 5 493 | 4 443 (50%) | 4 455 | 1 050 | 0.45 |
| Probability ≥ 40% alone | 7 933 | 5 523 (62%) | 3 375 | 2 410 | 0.49 |
| Probability ≥ 50% alone (IMPROVER's node) | 7 027 | 5 119 (58%) | 3 779 | 1 908 | 0.47 |
| The engine, with IMPROVER's drizzle (visibility < 5 km or low cloud ≥ 85%) | 5 949 | 4 620 (52%) | 4 278 | 1 329 | 0.45 |

The 20% floor stands: it sits on the plateau (0.48 from 20% to 30%), the hours it removes under it
are a trace more often than rain, and a floor of none would trade 738 more false hours for 539 more
right ones. Deciding by the probability alone, as IMPROVER does, is no better at any floor. IMPROVER's
drizzle condition is left out: the 807 drizzle hours it would remove were wet 61% of the time (the
ones it keeps, 76%). And «likely» (nothing measurable at ≥ 60%) was wet in 49% of 1 134 hours (37%
at 60–69%, 42% at 70–79%, 61% from 80%): a possibility, as rule 4 draws it, and not a forecast of
rain, so its word became «possibile» / «possible» the same day. Understating the hours from 80% is
the smaller fault than promising rain on the others.

**Phase**, 4 995 hours with precipitation in the METAR and in `best_match`: right 94%; the
witnesses' majority, on the 4 720 where they had a phase, 95%. Left to `best_match` (§10).

## 12. The witnesses

Since 26 Oct 2026 every fetch asks Open-Meteo, beside the forecast, for the same hours from
other model families (`OpenMeteoWitnessApi`): since 27 Oct 2026 ECMWF's IFS at 9 km, the Met
Office's global 10 km, and the HARMONIE-AROME Europe of KNMI and of DMI at 5.5 km (NOAA's GFS
until then: its visibility is right in 5% of the fogs seen, §11, and it left when the two
HARMONIE came in with 14.2). Temperature, cloud, visibility and humidity, nothing else. Since
17 Nov 2026 each votes on fog with its visibility, beside the own model's vote from its humidity
and wind, and adds its humidity to the mean that filters the fog (rule 5); they are never
printed: the numbers on screen stay `best_match`'s.

- **Not ICON-EU**: measured on 15 cities, `best_match` is the ICON family all week across Italy
  (ICON-D2 for two days where it reaches, then ICON-EU, then ICON global), and ICON-D2 runs inside
  ICON-EU: the same physics, the same mistakes.
- **Never `best_match` twice**: where `best_match` is one of the four (ECMWF over Sydney, the
  north of Europe from the fourth day, Longyearbyen; the Met Office over London; DMI over
  Keflavik), the mapper (`Witnesses`) recognises it from the numbers, temperature within 0.05 °C
  and cloud within 0.5% for the six hours either side, and leaves it out of that hour's vote.
  From the numbers and not from a table of Open-Meteo's choices, which can change tomorrow.
- **And never one number twice**: Open-Meteo fills a variable the chosen model lacks from the
  largest model that carries it, so `best_match` can serve MET Nordic's temperature with IFS's
  visibility (Oslo, Stockholm, Helsinki, every hour of both periods of §11; Marseille under
  AROME; Italy from 30 Nov 2026 under ICON). From 27 Oct 2026 the mapper recognised that one
  column (equal within half a metre for the six hours either side) so that IFS would not
  confirm itself; since 17 Nov 2026 `best_match`'s visibility is not read at all, and the fill
  cannot count twice by construction.
- **Where they are not**: KNMI and DMI answer over Europe (not Palermo, Cagliari, Madrid, Athens,
  Longyearbyen, nor Sydney) and for three days; outside, the response simply has no column of
  theirs, or nulls, and the two global ones remain (verified live on 27 Oct 2026 at Palermo,
  Sydney and Milan).
- **Best-effort**: a failed request costs the vote, never the report. The witnesses are written
  into the disk cache beside the forecast, so a report re-mapped from disk says the states the
  network's did, and an entry fetched with another list of witnesses is not a fresh hit
  (`ReportDiskCache.CURRENT_REQUEST`): the vote never changes its mind between a fetch and a
  re-read.
- **What it costs**: 3.3 KB gzipped beside the forecast's 4.5 KB with three models (26 Oct 2026,
  Lissone), 17.8 KB raw with four over Milan; for Open-Meteo's count, 16 variables from four
  models over 7 days weigh **1.6 calls** a fetch (1.2 with three), and a fetch 8.2: forecast
  5.2, witnesses 1.6, air 1.4.
- **What they may say on screen**: nothing. The vote ends in a state (rule 5). A word about
  their agreement on a far day was measured in 14.10 (28 Oct 2026, PLANNING «14.10»): on
  11 910 airport-days three to seven days ahead, from the whole runs per lead day, the spread
  between `best_match` and the witnesses on the day's high goes with the error later observed
  (Spearman 0.27; the high's mean error is 3.1 °C when the spread is 4 °C or more against
  1.4 °C below it), but «the models disagree» as a sentence about the day — the high then
  2 °C or more off — is true in 56% of the days at a 3 °C spread and 61% at 4 °C, and reaches
  two thirds only at 6 °C, said on one day in a hundred; the sky's spread, on the total cover,
  no better (51% of far days are wrong in silence). The spread says how wrong the forecast is
  on average, not whether this day is wrong, and the app has no honest sentence for an
  average: 14.11 does not exist.

## History

The reasoning, the alternatives refused (a probability floor and cap, rewriting numbers, a
filter in the ViewModel) and the decisions of 25 Sep 2026 are in the planning archive:
`docs/planning/04-verso-1.1.0.md`, «Il motore degli stati: dal codice del provider allo stato
dell'app», with its code review the same day. The day's rules were inherited from tweather's
phases 13b, 13c and 26 and refined on 24 Sep 2026 (`04-verso-1.1.0.md`, «La soglia del
maltempo», «Neve a 0%»). The index of every archived section is `docs/planning/README.md`.
