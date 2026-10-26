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
| `PROBABLE_PCT` | 20% | `AlertEngine`'s floor for «Maltempo»; under it bulletins do not name precipitation. Under it an amount is a trace of the single model that the ensemble does not back. |
| `LIKELY_PCT` | 60% | Where the US National Weather Service starts saying «likely». |
| `FOG_VISIBILITY_M` | 1 000 m | The WMO definition of fog, and the provider's own. |
| `SNOW_CM_PER_MM` | 0.7 | Open-Meteo's ratio: 7 cm of snow = 10 mm of water. |
| `RAIN_SNOW_C` | 1.0 °C | Mean 2 m temperature where rain and snow are equally likely over northern-hemisphere land (Jennings et al., Nature Communications, 2018). Used only when the model gives no phase. |
| `LIKELY_PHASE_REACH_HOURS` | 3 h | How far a likely hour looks for a measured one to borrow its phase. |
| Drizzle under | 0.5 mm/h | The «a few drops» end of light rain. |
| Rain light / heavy | 2.5 / 7.6 mm/h | The AMS Glossary's intensities, and Open-Meteo's own floors for 61/63/65 and 80/81/82. |
| Snow moderate / heavy | 0.2 / 0.8 cm/h | Open-Meteo's own snowfall ladder (71/73/75). |
| Sky | 20 / 50 / 80% | Open-Meteo's cloud cover buckets: clear, mainly clear, partly cloudy, overcast. |
| Wet day | 1 mm or 3 hours | The Met Office's «wet day»; three hours keeps a long soft drizzle (§6). |

## 3. The hour

The input is the hour as a row shows it: the amounts, the probability and the provider's code
of the **interval** (Open-Meteo's next slot, «sum of the preceding hour»), and the temperature,
cloud cover and visibility of the **instant** it starts.

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
4. **Likely, with nothing to measure** (< 0.1 mm and ≥ 60%): **1061 «Pioggia probabile»** or
   **1071 «Neve probabile»**. The phase is the hour's own when the model gave it one (a trace
   of snow or rain, a precipitation code), else the nearest hour within 3 hours with a
   measurable amount, else snow at ≤ 1 °C. A state of **possibility**: it draws the light
   precipitation, never the full one; it is not a wet hour (`WmoCode.likely`, outside
   `isPrecipitation`), so the headline does not say «it is raining», it does not label the
   day, and it carries no millimetres.
5. **Fog** at ≤ 1 000 m: kept where the provider says fog and the visibility agrees; **invented
   only where it persists into a neighbouring hour**, because fog is not one hour long (writing
   fog into an hour whose neighbours are clear nearly doubled the week's fog/non-fog
   transitions: 10 in the provider's series, 18 repaired hour by hour, 14 with this rule);
   dropped on the spot where the visibility contradicts it. **Freezing fog (48)** at ≤ 0 °C,
   the fog that deposits rime. Precipitation comes before fog: in rain, low visibility is the
   rain.
6. **Sky** from the cloud cover on the 20/50/80 buckets, not from the code: the sky drawn at the
   top of the page and the cloud tile already show that quantity, and the icon must say the
   same thing.

## 4. Missing inputs

Every input may be missing: an older cache entry, a model that does not carry it. A missing
input **switches off only the rule that needs it**, and that part of the decision falls back
on the provider's code, as the app did before the engine:

- no amount: the provider's precipitation code stands;
- no `snowfall`: the phase comes from the provider's code (snow or rain), else from the
  temperature at 1 °C;
- no `showers`: the provider's shower codes (80-82, 85-86) say whether the hour is convective;
- no visibility: the provider's own fog verdict stands;
- no cloud cover: the provider's sky stands, else overcast;
- no probability: the hour counts as probable (the probability is model-dependent and nullable
  all the way to the screen).

## 5. The present and the quarter of an hour

- **The current block** passes through the same engine. Its amounts cover 15 minutes
  (`interval` 900) and are brought to the hour before the thresholds. It never says «likely»
  (`likely = false`: it describes now). **Its fog follows its row** (26 Oct 2026): the block
  may invent fog only where the series does, in the row it falls in (`fogPersists` on the
  hourly visibility at that row and its neighbours), and its own visibility must agree too. Its
  visibility is the model's, interpolated like the rows', not an observation: until that day
  the block invented fog from it alone, and Lissone at 09:45 read «Nebbia» on 380 m under
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
- **Every reader inherits the state**: the headline, the alerts, the reader's rules, the
  widgets, the notifications. The engine lives in `:core:domain` and is called by the mapper
  precisely so that no screen reads the raw code and contradicts another.

## 8. The drawings

- **Rain and snow** takes `overcast-sleet`, which draws drops and flakes.
- **Freezing rain** takes the drawing of what falls (drizzle for 56/57, rain for 66/67):
  Meteocons has no honest drawing of rain that freezes. The danger is the word's and the
  «Maltempo» banner's. A composed drawing remains possible.
- **A thunderstorm that rains** takes `thunderstorms-*-rain`; «Temporali possibili» keeps the
  lightning without drops.
- **Likely rain** takes the drizzle; **likely snow** the single snow under a cloud.

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
- **Deduced freezing rain** (measurable liquid at ≤ 0 °C, with the 20% floor the provider's own
  freezing rain does not have) is decided and **off** until that winter measurement: a deduced
  state that fires a «Maltempo» alert would be the app's most expensive false alarm.
- **Out, for now**: mist and haze (visibility 1-5 km; a new state, to propose on its own), hail
  (no Open-Meteo model forecasts it), ground ice after rain (a sentence or an alert, not an
  icon), a «veiled» sky for high thin cloud only (the cloud layers are there; DWD's `ww` weighs
  them less than the total cover does).

## History

The reasoning, the alternatives refused (a probability floor and cap, rewriting numbers, a
filter in the ViewModel) and the decisions of 25 Sep 2026 are in the planning archive:
`docs/planning/04-verso-1.1.0.md`, «Il motore degli stati: dal codice del provider allo stato
dell'app», with its code review the same day. The day's rules were inherited from tweather's
phases 13b, 13c and 26 and refined on 24 Sep 2026 (`04-verso-1.1.0.md`, «La soglia del
maltempo», «Neve a 0%»). The index of every archived section is `docs/planning/README.md`.
