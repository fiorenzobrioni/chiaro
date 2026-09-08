# DESIGN.md — Chiaro

The design system of Chiaro, written the way `obsidian_syntax/DESIGN.md` is written
for the terminal line: values first, reasons attached, nothing decorative left to
taste at implementation time.

It is Material 3, and the emphasis matters. Material is not a fallback for having no
design: it is a system with a color algorithm, a type scale, a shape scale and a
motion physics, and most apps that "use Material" use its defaults and stop. Chiaro
commits to it — generated color, the expressive scales, real motion — and adds the
three things Material has no opinion about because no other app needs them: a sky
computed from an ephemeris (§3), a ribbon of the day's light (§4), and the palettes
for weather quantities (§9).

**The one rule this document exists to enforce:** every visual decision here is either
a Material token, a value with a measured number beside it, or a rule with a test that
holds it. Nothing is "roughly amber".

---

## 1. Principles

### 1.1 The screen must not lie

The t-series' hard rule, in a UI with no text to say it. Its four visual forms:

| Situation | tweather said | Chiaro shows |
|---|---|---|
| data older than the interval | `# stale` | a freshness chip with the real age, in the warning role, tappable to retry |
| the provider has no data for here | the key is absent | the card is not drawn; the details sheet names what is unavailable and why |
| a computed value | a `//` disclaimer | the word "estimated" in the label, in `onSurfaceVariant` |
| a verdict | `~ unstable  cloud 61%` | the chip's word, plus the number that decided it, on the same line |

Two corollaries that cost layout work and are not negotiable. **A section with no data is
not drawn** — never a card with an em dash in it. And **no placeholder ever renders as a
value**: a skeleton must be visibly a skeleton, which in practice means it is a shimmer
of `surfaceContainerHigh`, never a grey "0°".

### 1.2 Every number says what to do with it

The mainstream inversion of the series' evidence rule. UV 7 is a number, "burns in about
25 minutes" is the answer. Every metric card is a pair: the value in `titleLarge` and its
consequence in `bodySmall` / `onSurfaceVariant`, from a lookup keyed on the value's band.
No card ships without its second line, and if a metric has no honest second line, that
metric belongs in the details sheet and not on the home screen.

### 1.3 Depth is optional, never mandatory

Three levels, and nothing important lives only at the third: **the sentence** (the canvas
headline), **the card** (the number and its meaning), **the sheet** (everything, including
the technical fields). A reader who never taps anything must still be correctly informed.

### 1.4 What Chiaro never does

The anti-pattern list, kept short so it is actually remembered:

- No photographic or 3D weather art. No glass droplets, no animated cartoon clouds.
- No full-screen spinner. Ever. Content first, freshness stated (§1.1).
- No number without its unit, and no unit that disagrees with the setting.
- No color as the only carrier of meaning (§10).
- No "beginner mode" toggle. One app, understandable by default.
- No rainbow ramp for a quantity (§9.1), and no dual-axis chart anywhere.
- No jargon. Not translated jargon — absent jargon.

---

## 2. Color

### 2.1 The scheme is generated, not painted

Chiaro's color comes from Material's tonal palettes, and there are two sources for them:

1. **The generated schemes** (default, and two of them since §2.5): built from the source
   colors of §2.2 and §2.5 by the same tonal algorithm. This is the app looking like
   itself, which is what it does out of the box since 7 set 2026 — §13's second open item,
   resolved.
2. **Dynamic color** (a switch in settings): the palettes are derived from the reader's
   wallpaper by the platform. Chiaro consumes `dynamicLightColorScheme` /
   `dynamicDarkColorScheme` and never hardcodes a role over them. It reaches the Material
   roles only: the semantic tokens of §2.3 and the canvas of §3 never followed the
   wallpaper, and still do not.

Consequence for implementation: **no composable ever names a hex.** It names a role
(`MaterialTheme.colorScheme.primaryContainer`) or a semantic token (§2.3). A hex in a
screen file is a bug, and the sweep test in §12 fails the build over it.

### 2.2 The Chiaro source colors

Three hues, and each one is a time of day the product is actually about:

| Source | Hex | What it is |
|---|---|---|
| Primary | `#F1A000` | the golden hour |
| Secondary | `#007DB6` | daylight sky |
| Tertiary | `#70569C` | the blue hour |
| Neutral | `#8C857A` | warm, so the surfaces read as paper and not as aluminium |
| Neutral variant | `#8A8578` | outlines and dividers |
| Error | `#BA1A1A` | Material's, unchanged — a convention worth borrowing |

Roles resolve as tones off those palettes, exactly per Material (primary = P40 light /
P80 dark, primaryContainer = P90 / P30, and so on). The two that get named here because
everything else is measured against them:

| Role | Light | Dark |
|---|---|---|
| `surface` | `#FCF9F3` | `#16130E` |
| `primary` | `#835500` | `#FFB957` |

The two surfaces sit 17.6:1 apart, which is the headroom every token in §2.3 is
measured inside. Both, and the other 34 roles, are **generated**: `tools/gen_scheme.py`
takes each source color's hue and chroma in CIELAB LCh, sets L\* to the tone Material
names for the role, and clamps the chroma until the result fits in sRGB. Material's own
tone IS L\*, so the tone is exact and only the chroma is an approximation of HCT — a fine
trade for a scheme nobody sees unless they turn dynamic color off, and the reason
`PaletteContrastTest` asserts the outcome instead of trusting the method. The neutral
palettes are pinned to chroma 3 and 7 respectively: that is the difference between a
surface that reads as warm paper and one that reads as beige.

Amber as the primary is a deliberate risk. It is the least-used hue in a category that is
overwhelmingly blue, it is the color of the thing this app knows about that others do not,
and Material's tone system keeps it legible where a hand-picked amber would not be: the
primary role at tone 40 is a deep bronze, not a highlighter.

The palette was retuned once (3 Sep 2026, the color pass): every hue source, sky
anchor and quantity ramp had its chroma raised in OKLCh — ×1.22 to ×1.65 depending
on how washed the original was, raised once more after the first on-device look — at
**held WCAG luminance**, which is why every ratio
printed in this document survived the retune with at most a 0.04 drift. The neutrals
did not move: the warm paper is the identity, the chroma was the complaint. (The
amber primary at tone 40 barely moved either — it was already at the sRGB gamut
edge, which is its own kind of measurement.)

### 2.3 Semantic tokens Material does not have

Material has no slot for "the sky at nautical twilight" or "70% chance of rain". These
live in a `ChiaroColors` object behind a `CompositionLocal`, they are theme-aware, and
their dark values are **selected for dark**, never flipped.

**Verdicts** (§8.7). Green / amber / red is the classic color-vision trap, so the numbers
are stated rather than assumed: measured with the palette validator, `unstable`↔`fail`
separate by ΔE 0.7 under deuteranopia at ink weight. That is not fixable by re-picking
hues, which is exactly why **a verdict is never a colored dot: it is a glyph and a word**,
and the color is the third carrier, not the first.

| Verdict | ink (light) | container (light) | ink (dark) | container (dark) |
|---|---|---|---|---|
| pass | `#005D2D` 7.7:1 | `#D1EDD9` | `#54DC88` 10.6:1 | `#003F23` |
| unstable | `#7A5200` 6.6:1 | `#FDE5AE` | `#FFBC27` 11.0:1 | `#3F2F00` |
| fail | `#950700` 8.7:1 | `#FFDCD7` | `#FFB4AB` 10.9:1 | `#560705` |
| unknown | `#4F5359` 7.4:1 | `#E7E7E4` | `#A8ADB6` 8.2:1 | `#2B2B2E` |

Ratios are against the surface of §2.2; ink-on-container is 5.6:1 or better in both
schemes. `unknown` is deliberately the one hueless entry: not knowing is not a state with
a color, and a gray chip reads as "no answer" without anyone having to be taught it.

**Rain** — a quantity, so a single hue, light to dark, monotonic in luminance (verified,
§12):

```
light  #DFEFFC  #AFD9F6  #76BCEC  #2E97DE  #006FAC     Y .84 .65 .46 .28 .14
dark   #0E2E44  #004B6F  #006C98  #0092C8  #55BCEC     Y .02 .06 .13 .25 .44
```

That is the ramp of the **marks** — the sparkline, the drift cell, the swatch — and it
cannot carry text: on paper its light end is 1.12:1 and its middle 1.96:1. So a printed
probability has a **second ramp of the same quantity, selected as ink** (6 set 2026, after
a device report: the week's 0% was the heaviest figure in a column of pale blues, because
it alone fell back to `onSurfaceVariant` at 8.9:1 while 15% was printing at 1.3:1). Same
hue family, same five steps, every one of them measured against the surface of §2.2:

```
light  #5F7281  #426780  #1D5C81  #00507E  #004470     4.7 5.7 6.9 8.2 9.7 : 1
dark   #768996  #759BB3  #71ADD0  #6FBFEB  #76D1FF     5.1 6.3 7.6 9.1 10.9 : 1
```

Two rules come with it. **Zero is the quiet end of this ramp, not another color**: a
probability of nothing is still a probability, and the figure with the least to say must
not be the loudest one in the column. And **the fill ramp never paints a figure** — where
a number is printed it is `rainInkAt`, where a mark is drawn it is `rainAt`.

**Temperature** — a diverging quantity, because it has a meaningful middle. Two hues and a
neutral midpoint, never a rainbow; the midpoint is anchored at **15 °C / 59 °F**, a fixed
comfortable reference, and **never at the min/max of what happens to be on screen** (a
scale that re-anchors itself makes a mild week look like a heatwave):

```
light  #006FAC  #4CA5D8  #9CC9E7  #DCD7CC  #FABD72  #E67E00  #B85100
dark   #63B8EA  #1791D2  #0070AB  #4A4740  #985E00  #C87400  #F29300
```

Luminance peaks at the midpoint in light and troughs at it in dark, so in both schemes the
middle recedes and the extremes come forward.

**Freshness / warning**: the `unstable` pair above, reused deliberately — "this data is old"
and "the sky is iffy" are the same class of statement and should not learn two colors.

### 2.5 The second dress

Chiaro ships **two** generated palettes and the reader picks one in Settings. `PAPER` is
everything above and the default; `VIVID` is the same app at the brightest colors a
screen holds — asked for by the committente (7 set 2026), who wanted the saturated blue
of the launcher's other weather widget rather than warm paper.

It is a **choice of dress, not of accessibility**: every floor §10 sets is the same for
both, and every ratio §2.3 prints is the same number in both. That is not luck, it is
the rule the vivid tokens were derived by (`tools/gen_vivid.py`):

> keep the hue, hold the WCAG luminance, and take the chroma to the sRGB gamut edge — or
> to **×1.8** paper's chroma, whichever comes first.

Contrast is a function of luminance alone, so holding the luminance carries every
measured ratio across unchanged. The ×1.8 ceiling is what keeps a rule from becoming a
caricature: a token that is deliberately near-neutral — the diverging ramp's midpoint,
`unknown`, a night sky — would otherwise be dragged to the edge of the gamut and stop
being neutral, which is the one thing those tokens are for.

The Material roles are not derived that way; they are **generated by the same
`tools/gen_scheme.py` from their own sources**, because a tonal scheme is not a set of
tokens to nudge:

| Source | Hex | What it is |
|---|---|---|
| Primary | `#2C7BF2` | daylight sky, promoted |
| Secondary | `#FFB000` | the golden hour |
| Tertiary | `#7B57F0` | the blue hour |
| Neutral | `#7C8794` | cool, so the surfaces read as daylight and not as paper |
| Neutral variant | `#79859B` | outlines and dividers |
| Error | `#E01B24` | Material's, raised |

| Role | Light | Dark |
|---|---|---|
| `surface` | `#F6FAFF` | `#0D141B` |
| `primary` | `#015BBE` | `#B8C4FF` |

Those two surfaces sit 17.7:1 apart, the same headroom §2.2 measures its tokens inside.
The three hues are still the three times of day of §2.2 — this palette **promotes the
daylight sky to primary** rather than inventing a fourth. Amber stays, one role down: at
tone 40 an amber is a bronze whatever else changes around it, and a reader who asks for
brilliance is not asking for bronze. The neutrals are the other half of the difference:
chroma 6 and 14 instead of 3 and 7, which is what turns a warm white into a cool one.

The verdicts, measured against the surfaces above:

| Verdict | ink (light) | container (light) | ink (dark) | container (dark) |
|---|---|---|---|---|
| pass | `#005D2D` 7.7:1 | `#BFF2CE` | `#00E079` 10.5:1 | `#003F23` |
| unstable | `#7A5200` 6.6:1 | `#FFE5A8` | `#FFBC27` 11.0:1 | `#3F2F00` |
| fail | `#950700` 8.7:1 | `#FFDCD7` | `#FFB4AB` 10.9:1 | `#590001` |
| unknown | `#4C535E` 7.4:1 | `#E7E7E2` | `#A4ADBD` 8.2:1 | `#2B2B30` |

**Three of those inks are the paper values, unchanged, and that is the honest result
rather than an omission.** After the 3 set color pass the light inks already sat ON the
gamut edge at the luminance 4.5:1 allows them; sRGB has nothing left to give down there,
and no ceiling, boost or hand-picking invents it. Where the vivid palette is visibly
vivid is everywhere luminance was never the constraint: the fills, the ramps, the sky and
— on dark grounds — the icons (§13.1).

Rain, the marks:

```
light  #DCEFFF  #A4DAFF  #57BEFF  #0097E8  #006FAC     Y .84 .65 .46 .28 .14
dark   #002E4A  #004B6F  #006C98  #0092C8  #00BEFD     Y .02 .06 .13 .25 .44
```

Rain, the ink for a printed figure — its light end is 1.12:1 and its middle 1.96:1 on
this surface too, so the same two ramps for the same reason:

```
light  #50738F  #136997  #005C88  #00507E  #004470     4.8 5.7 6.9 8.2 9.7 : 1
dark   #688BA2  #539ECA  #22B1F1  #42C1FF  #76D1FF     5.1 6.3 7.6 9.1 10.9 : 1
```

Temperature, diverging, the midpoint still a neutral and still anchored at 15 °C:

```
light  #006FAC  #00A6EB  #7BCCFF  #E0D7C3  #FFBB66  #E67E00  #B85100
dark   #2FBAFF  #0091D5  #0070AB  #4C473A  #985E00  #C87400  #F29300
```

### 2.4 Rules for using color

- Roles, never hexes (§2.1). A role means the same thing in both dresses (§2.5); a
  composable never asks which one is on.
- Text wears text roles (`onSurface`, `onSurfaceVariant`) — **never a data color**. A
  colored mark may sit beside a label; the label itself stays ink.
- Any fill that carries meaning has a text or glyph companion (§10).
- On the canvas, text is white over the scrim contract of §3.6 and nowhere else.

---

## 3. The sky canvas

### 3.1 What it is

The hero of the Today screen is a gradient computed from the sky above the active city:
the sun's altitude from `AstronomyEngine`, the current hour's cloud cover and
precipitation probability, and at night the moon's illumination and altitude. It is not a
mood; it is the same data the screen below it is about, which is why it cannot contradict
that screen — one engine, one sunrise, and a test that says so.

### 3.2 The bands

Solar altitude picks the band; within a band the app interpolates on altitude so the
transition is continuous and a reader watching at sunset sees it move.

The rows are **anchors**, not buckets: a solar altitude between two of them renders as the
blend of the two, which is what makes a sunset move instead of snapping through seven
states. `SkyPaletteTest` holds that (no half-degree step may change the sky by more than
0.15, and the midpoint between two anchors must be neither of them).

| Band | Altitude | top | mid | bottom |
|---|---|---|---|---|
| Day | ≥ 12° | `#0090DA` | `#55B7F0` | `#BADFF6` |
| Low sun | 8° | `#3483CA` | `#80B7DE` | `#E8CEA3` |
| Golden hour | 4° | `#4B7FBB` | `#F49C04` | `#FFD083` |
| Horizon | 0° | `#4573AF` | `#E58800` | `#FFC268` |
| Civil / blue hour | −6° | `#203D73` | `#425DA4` | `#8D7CB7` |
| Nautical | −12° | `#172449` | `#27396A` | `#425187` |
| Astronomical | −18° | `#101934` | `#152143` | `#202D53` |
| Night | ≤ −90° | `#0D1323` | `#111A2D` | `#182237` |

The golden hour has **two** anchors, and that is a fix rather than a flourish: with one
anchor at the horizon, 3° above it rendered as the midpoint between a cool low sun and the
amber — a washed-out tan, and not what anybody means by the golden hour. Rendering the
palette and looking at it is what found that. No test would have.

The bands are the same in the light and dark schemes. The sky is not a surface: it does
not follow the reader's theme, because at 23:00 it is dark outside whatever the phone is
set to. This is the single deliberate exception to §2.1, and its scrim contract (§3.6) is
what makes it safe.

### 3.3 Cloud and rain

Cloud cover **desaturates each stop toward its own brightness** by `0.7 × cloudPct` and
then dims it by `0.15 × cloudPct`, so a fully overcast day keeps 30% of its color and
stays recognizably morning or evening.

The first draft of this section mixed every stop toward one fixed grey, and implementing
it found the hole: a fixed grey is brighter than a night sky, so an overcast midnight came
out brighter than a clear dusk. Clouds take the color out of a sky, not a fixed amount of
light into it. `SkyPaletteTest` now asserts both halves of that.

Precipitation probability above 50% multiplies value by `1 − 0.25 × (p − 50)/50`. Both are
applied after the band interpolation and before the moon lift, in that order, and the moon
lift is itself **scaled by `(1 − cloud)`**: clouds hide the moon, and without that factor
an overcast full-moon night rendered brighter than a clear one.

### 3.4 The moon

At night only (altitude < −6°), and only while the moon is above the horizon, each stop is
lifted toward `#273458` by `illumination × clamp(moonAltitude/40°, 0, 1) × (1 − cloud)`. A full moon high in a clear sky makes a
visibly lighter canvas, which is both true and the reason the sky section says the dark
window is spoiled.

The lift target went up with the rest of the sky on the color pass (`#2A3550` → `#273458`),
and this line said the old value until the Fase 9 audit read the document against the
code. That is the drift `PaletteDocTest` now exists to prevent: a number in this file is
only worth printing if something fails when it stops being true.

### 3.5 Motion and cost

One shader, one animation, and a hard budget: **the canvas may not cost more than 2 ms per
frame on a mid-range device.** It animates only the interpolation between bands (a slow
crossfade on a 30-second tick, not a per-frame recomputation), and it becomes a static
gradient when the system is in battery saver, when the reader has reduced motion on, or
when the activity is not resumed. No particles, no parallax, no video.

### 3.6 The scrim contract

Text over the canvas is `#FFFFFF` (secondary text `#FFFFFF` at 70% alpha) over a scrim
of `rgba(16,18,22,0.55)` fading to transparent, covering **both text bands** — the
bottom one under the temperature and the headline sentence, and, since the canvas owns
the top edge of the screen (Fase 3), a symmetric top one under the place switcher and
the status bar icons. One color, one alpha, both bands. The rule that makes this safe
is testable and tested: **for the brightest possible canvas (Day band, 0% cloud, its
brightest stop `#BADFF6`), white on the scrimmed band is 5.27:1** — above the 4.5:1
floor, and the alpha was chosen for that reason: 0.50 gives 4.53:1 and leaves no
headroom for a future band, 0.45 gives 3.95:1 and fails. If a band is ever added that
breaks it, `ScrimContractTest` fails rather than the reader squinting.

Those three numbers are **SRC_OVER in sRGB values** — `scrim × α + sky × (1 − α)`, which
is what the brush does — held at the 8 bits per channel the framebuffer holds. They were
re-measured in the Fase 9 pass and two of them moved: the section had quoted 5.29 / 4.58
/ 3.95, three numbers from three arithmetics, one of which (4.58) no arithmetic reaches.
`ScrimContractTest` had a fourth, `Color.lerp`, which blends in Oklab — a perceptual mix,
not a composite. One model now, in both tests and in this line. The conclusion never
moved, and the correction sharpens it: 0.50 clears the floor by 0.03, which is not
headroom, it is luck.

### 3.7 The vivid bands

The reader's palette (§2.5) picks the band table too, because the canvas is the loudest
thing on the screen and a vivid app with a muted sky would be a mixed message. Same nine
anchors, same altitudes, same mixing rules — derived from §3.2 by §2.5's one rule **and
one clause the semantic tokens do not get**, so their luminances are §3.2's luminances:

> and never below **0.65** of the chroma sRGB holds at that band's own luminance.

| Band | Altitude | top | mid | bottom |
|---|---|---|---|---|
| Day | ≥ 12° | `#0090DA` | `#2BB8FF` | `#AFE0FF` |
| Low sun | 8° | `#0082DC` | `#4BBAFF` | `#FACA78` |
| Golden hour | 4° | `#007DE5` | `#F49C00` | `#FFD083` |
| Horizon | 0° | `#006FDC` | `#E58800` | `#FFC268` |
| Civil / blue hour | −6° | `#003698` | `#2C52DB` | `#9571DE` |
| Nautical | −12° | `#0D1D65` | `#172F95` | `#3445C0` |
| Astronomical | −18° | `#09144B` | `#0C1B5E` | `#142479` |
| Night | ≤ −90° | `#050E3C` | `#051645` | `#041B5F` |

That clause was added on 8 set 2026, and the reason is an accident in the shape of the
first rule rather than a change of mind about it. ×1.8 is a **multiple of paper's
chroma**, which is exactly right for a token: it is what keeps a deliberately
near-neutral one near-neutral. A sky is not a token. Paper draws the night with the
least chroma of any band, so the multiplier was handing the least to the bands where the
gamut has the most left over — measured as a fraction of what sRGB holds at each band's
luminance, the vivid day sky ran at **1.00** of it and vivid midnight at **0.44**. The
dress was at its loudest on the one sky that is already bright, and at its quietest on
the sky the reader opens the app under all evening. The floor is a fraction of the gamut
rather than a multiple of paper precisely so that it can only ever raise a band the
multiplier left flat.

It moves three rows and no others: day, low sun, both golden anchors, the horizon and
the blue hour were already above 0.65 and come out of the generator byte for byte as
before. So the scrim measurement below did not move, and neither did the brown dusk two
paragraphs down. 0.65 was picked the way 1.8 was, by rendering the sheet and looking at
it (`tools/palette_sheet.py`); at 0.75 midnight starts reading as a royal blue rather
than as a night.

Every claim §3 makes about the canvas is a claim about brightness — darker after sunset,
an overcast midnight is not a dusk, a clear full moon out-shines a cloudy one — so held
luminance carries all of them across. The scrim contract is the one that does **not**
come free: §3.6 composites per channel, and a per-channel composite of a more saturated
color is a different pixel. Measured rather than assumed: white over the scrimmed vivid
brightest stop `#AFE0FF` is **5.26:1**, against paper's 5.27:1, and the same alpha is the
smallest that clears the floor (0.50 gives 4.56:1, 0.45 gives 3.99:1). `ScrimContractTest`
sweeps both tables at every half-degree.

And then the sheet was rendered and looked at, which is the step no test does. It shows
one thing the numbers do not: between 0° and −6° the vivid canvas runs **browner** than
paper's, because the blend is perceptual and Oklab's midpoint between a saturated amber
and a saturated violet is a saturated brown. It was left. It reads as a real dusk, it
lasts six degrees of solar altitude, and if it ever stops reading that way the fix is the
one §3.2 already used for the golden hour — another anchor, not a duller table.

---

## 4. The daylight ribbon

A 6dp band (4dp in compact rows) showing one day of light: night, astronomical, nautical
and civil twilight, the golden hours, daylight — drawn with the §3.2 stops at fixed
saturation, with the current moment marked by a 2dp `onSurface` line and a 4dp dot.

It is the app's signature element and the one component that makes a week of rows read as
a season rather than seven identical stripes. It is also, deliberately, **a depiction and
not an encoding**: nobody has to decode a color into a phase, because every phase it shows
is named in text on the Sky screen, and the ribbon's own content description reads the
phases with their times. That is why it is allowed to be a natural sky gradient where §9.1
forbids rainbows for data.

---

## 5. Typography

**Inter** (variable, OFL), with the platform sans as fallback. Never a monospace: the
terminal line owns that, and Chiaro must not read as its sibling. The one exception is
nothing — there is no exception.

| Role | Size / line | Weight | Where |
|---|---|---|---|
| `heroTemperature` (extended) | 64 / 68 | 300 | the canvas' current temperature |
| `displaySmall` | 36 / 44 | 400 | a day's high in the expanded day sheet |
| `titleLarge` | 22 / 28 | 500 | the headline sentence |
| `titleMedium` | 16 / 24 | 600 | section titles, metric values |
| `bodyLarge` | 16 / 24 | 400 | prose in the guide and the journal |
| `bodyMedium` | 14 / 20 | 400 | card body |
| `bodySmall` | 12 / 16 | 400 | the meaning line under a number, timestamps |
| `labelLarge` | 14 / 20 | 500 | buttons, chips, the hour strip's temperature |
| `labelSmall` | 11 / 16 | 500 | axis labels, ribbon legends |

**Every figure that sits in a column is tabular** (`FontFeatureSetting("tnum")`): the hour
strip, the week rows, the journal's deltas. Proportional digits in a column are the
typographic equivalent of a wobbling table, and this app has a lot of columns.

Rounding is a rule, not a call: temperatures to whole degrees everywhere except the
current one and the feels-like, which carry one decimal because the source does;
probabilities to whole percent; wind to whole units; distances to one decimal below 10.

---

## 6. Shape, elevation, spacing

**Shape** — Material's scale, assigned once:

| Component | Shape |
|---|---|
| canvas (bottom corners) | square: the sky has no corners |
| cards, sheets | large, 16dp |
| metric tiles, hour cells | medium, 12dp |
| chips, buttons, FAB | full |
| ribbon, bars, sparkline ends | 4dp round caps |

**Elevation**: tonal, not shadow. Hierarchy comes from `surface` →
`surfaceContainerLow` → `surfaceContainer` → `surfaceContainerHigh`. Shadow is permitted
at level 1 on scrolled app bars and at level 3 on the FAB, and nowhere else. The series'
distaste for fake depth survives the reskin as restraint rather than prohibition.

**Spacing**: an 8dp grid with a 4dp sub-unit. Screen margin 16dp; card padding 16dp; gap
between cards 12dp; gap between sections 24dp; touch targets never below 48dp.

A **section header** costs 16dp above and 4dp below (`SectionTop`/`SectionBottom` in
`ui/theme/Shape.kt`); a **group header** inside a section costs 12dp above. The numbers are
written down because they grew apart before anyone compared them: three different values
across five screens (device review, 4 set).

Those are what the header **spends**, not what the eye sees — the neighbour adds its own,
and the neighbour is not the same everywhere:

- next to one of the app's own rows (8dp of vertical padding), 16 + 8 is the 24dp above
  and 4 + 8 the 12dp below;
- Today's list spaces its own items by 12dp, so its header asks for 12 rather than 16 and
  arrives at the same 24 / 12;
- next to a Material `ListItem` — the Sky moments, the Alerts switches and rule cards, the
  Settings rows — the gap is Material's, not ours: `ListItem` brings its own padding and a
  minimum height that centres its text, so the air under a header there is at least 12dp
  and usually a little more.

That last case is **deliberate and stays**. A list built out of `ListItem` looks right
because it follows the platform, and shaving 2dp off a component to match a number in this
document would be the design system arguing with Material over something no reader can
see. What the document asks for is one header cost everywhere, which is what the three
constants give.

**Density**: on a 6.1" phone at default font size, the canvas, the headline sentence and
the first hours of the strip are above the fold. That is the layout's acceptance test.

---

## 7. Motion

Material 3 Expressive's spring physics, from `MaterialTheme.motionScheme`:

| Kind | Spec | Used for |
|---|---|---|
| spatial default | spring(damping 0.8, stiffness 380) | anything that moves or resizes |
| spatial fast | spring(damping 0.9, stiffness 800) | chips, toggles, small state |
| effects | spring(damping 1.0, stiffness 1600) | color, alpha, elevation |

Shared-element transition from a week row to its day sheet; predictive back everywhere;
the pager between places moves the canvas with it, so switching city looks like turning to
another sky. **Reduced motion collapses every one of these to a 100 ms fade**, and the
canvas freezes (§3.5) and the weather icons stop (§7.1). No animation ever gates
information: a reader who disables motion sees the same content at the same moment.

**How the app knows** (Fase 9): Android has no `prefers-reduced-motion` of its own.
Accessibility → Remove animations and Developer options → Animator duration scale both
write `Settings.Global.ANIMATOR_DURATION_SCALE`, and zero is the answer — the same number
the platform's own animators read, so this is the API and not a way around a missing one.
`ChiaroTheme` reads it, watches it (the toggle lives outside the app, so a value read once
at start-up would be wrong for exactly the reader it is for) and publishes
`LocalReducedMotion`.

The app moves in four places and all four ask (the fourth is §7.1's icons): the week row's hour strip opens with
`ChiaroMotion.enter/exit`, the pager `scrollToPage`s instead of animating, and the rule
editor's dry-run answer jumps into view instead of scrolling to it. Until that pass
`ChiaroMotion.reducedMotionFadeMillis` was a constant nothing consulted, which is the
shape a design rule takes when it is only written down: true in this file, absent from
the APK. The canvas needed nothing — it is a `Brush`, it has never animated, and §3.5's
"becomes a static gradient" is a promise it keeps by construction.

### 7.1 The weather moves

The weather icons are Meteocons' **animated** drawings since 7 set 2026 (committente),
and the motion is the illustrator's own: every source SVG in the family carries SMIL, and
`tools/import_meteocons.py` now carries it across as an `AnimatedVectorDrawable` instead
of dropping it. The sun turns once in 45 seconds, the moon rocks, cloud banks drift, drops
fall in 0.7 seconds and out of step with each other, the bolt flickers. Nothing was
invented here; a rewrite would have been a second opinion about somebody else's drawing.

**Only the condition family moves.** A metric tile's mark labels a quantity — a barometer
that spins forever is decoration, and §1.4 is where decoration goes. `mc_not_available`
does not move either, because the family has no animation for "we do not know", which is
the right amount of motion for it. `ChiaroIcons.movingRes` returns **null** for those
rather than a still frame dressed as a moving one, and the caller falls back.

Four things must all be true before an icon moves, and they are checked in this order:

1. the reader left **Settings → Appearance → Animated icons** on (it ships on);
2. the system is not asking for less motion — the same `ANIMATOR_DURATION_SCALE` every
   other animation in the app reads (§7), so «Remove animations» stops the weather too;
3. this drawing HAS a moving sibling;
4. this is not a Compose preview, where the `AndroidView` that hosts it renders as nothing.

**Where it moves**: the hour strip and the week rows on Today — every condition icon the
app draws, which is the same list. About thirteen at once in the worst case, because the
strip is a `LazyRow` that composes what fits and the week is seven. **Not the widgets**,
and not by choice: `RemoteViews` cannot run an `AnimatedVectorDrawable` at all.

**What it costs**, and why the answer is the platform's rather than a promise made here:
a view that is not drawn is a render node hwui does not prepare, and the animators of a
node it does not prepare do not run. Scrolling a cell away or backgrounding the app
therefore stops the work without a lifecycle observer of our own. (Corrected 9 set 2026:
this paragraph used to credit `ImageView.onVisibilityAggregated` → `setVisible(false)`
with pausing the animator set. It does call it, but the RenderThread animator's `pause()`
and `resume()` are two TODOs in AOSP, so on that thread the call is a no-op — it is not
being drawn that stops the work, and that is enough.)

**The weather holds still while the page moves** (9 set 2026, after the device still
reported a slight hitch with the loops on). The remaining cost was the RenderThread's:
about fourteen vector loops re-rasterized at every vsync while the same thread moves the
layers of the scroll. For as long as a scroll is in progress — Today's list, the hour
strip's row, or the pager between places (`LocalMotionPaused`) — each moving icon hides
its animated twin and shows its still drawing, which is composed underneath it at all
times so that the first frame of a scroll composes nothing. Hidden, not paused, for the
reason above; and not stopped, because `stop()` jumps the drawing to the loop's end frame
(for the rain, the frame with no drops in it) and `start()` would replay from the cloud at
every rest. When the page stops the twin is drawn again and its loop is where the clock
puts it, because the animators run on frame time. The visible price is a change of pose
at the two ends of a scroll: the moving drawing snaps to its still pose as the finger
moves, and back when it stops.

**Why an `ImageView` and not a Compose painter** — corrected 8 set 2026. The first
version of this section said Compose's `AnimatedImageVector` could not play an endless
loop; it can (its parser reads `repeatCount="infinite"` and builds an infinite
`repeatable`), and the reason it is not used is a better one: it animates by recomposing
the vector's tree every frame and rasterizing it on the UI thread, and the UI thread is
what a scroll needs. The platform's `AnimatedVectorDrawable` in a hardware-accelerated
`ImageView` runs on the RenderThread (`VectorDrawableAnimatorRT`, verified in the AOSP
source), so while the icons move the UI thread pays nothing per frame.

**What the interop costs instead**, measured in the source after the strip and the page
were reported "slightly choppy" (8 set 2026), and what was done about each:

- Every `AndroidView` in a lazy list gets `View.layout()` called on every scroll frame —
  Compose's `AndroidViewHolder` does it from `onGloballyPositioned` — and lazy items are
  otherwise placed on layers and moved without redrawing. Inherent to the interop, small
  per icon, left alone.
- The drawable was `mutate()`d after `getDrawable`, which for an AVD is a second deep copy
  of the vector tree: the private constructor already copies it for every instance
  (`AnimatedVectorDrawableState(copy, …)` in AOSP). Removed.
- A recycled cell threw its drawable away in `onReset` and re-inflated on reuse even when
  the weather was the same. It now only stops the loop, and `ConditionIcon`'s
  `MovingIconView` restarts it when the resource matches; the drawable goes in `onRelease`.
- The strip's cells were keyed by position, so at the top of every hour all the visible
  cells changed content and re-inflated in one frame. `HourCell.key` is the hour now.
- The week was one lazy item, so its seven icons were inflated in the frame it entered.
  It is seven items now, one per row, on the same 12dp rhythm the list already had, so
  the prefetcher takes them one at a time.

What remains is the animation itself: about fourteen vector caches re-rasterized on the
RenderThread every vsync while they are on screen. That is the price of the feature, the
one lever on it is how many move at once, and it is measured on a device, not here.

**How the conversion works**, because it is the only place in the app where a file format
was translated rather than copied. Four SMIL forms appear in the family, all linear, all
endless: `rotate` and `translate` become a `<group>` with its pivot animating `rotation`
or `translateX`/`translateY`; `opacity` becomes the group's paths animating `fillAlpha`
and `strokeAlpha`; a `gradientTransform` is dropped with the gradient the importer had
already flattened. Two things SMIL has that AVD does not:

- **`additive="sum"`** stacks two transforms on one element. AVD gives a group exactly
  one, so two transforms become two nested groups, outermost first — the order SMIL
  multiplies them in.
- **A negative `begin`** is a phase, not a delay: it is what makes three raindrops fall
  out of step instead of in a chorus line. AVD's `startOffset` is the opposite, so the
  phase is baked into the keyframes instead. An instantaneous wrap is drawn as two
  keyframes one thousandth of a cycle apart, which under a millisecond at the family's
  shortest loop and inside a single frame.

---

## 8. The component kit

Each entry is the contract; the Compose signatures land in Fase 1.

**8.1 SkyCanvas** — the gradient (§3), the place name, `heroTemperature`, condition,
feels-like, the daylight ribbon, the headline sentence, the scrim (§3.6). Collapses on
scroll into the app bar, keeping place and temperature. **At least 280dp plus the status
bar, and taller when its text needs it** (8 set 2026): everything on it is measured in sp
and the block was measured in dp, so at 100% type a two-line sentence left 2dp before the
hero climbed into the place row, and at 115% they overlapped by 30dp. The row and the hero
are the two ends of one column now, `SpaceBetween` on a floor rather than two things
aligned to opposite edges of a fixed box. The bottom edge is straight (4 set, kept on
review 8 set): every other surface on the page is inset and rounded, and the one that is
not is the ground the page opens on, not a card floating over it.

**8.2 FreshnessChip** — appears only when the data is older than the update interval.
Warning role, the real age ("3 hours ago"), tappable to retry, with a progress state while
retrying. Never a toast: a toast is gone before it is read.

**8.3 HourStrip** — horizontal, 24 cells from the next full hour, each 56dp wide: hour,
icon, temperature (tabular), rain probability on the ink ramp (§2.3), zero included; an
hour the provider gave no probability for prints nothing at all. **Edge to edge** (8 set
2026): the page margin is the row's `contentPadding`, so the first cell starts on the 16dp
line and the rest slide under the screen's edge — cut on a line 16dp inside it, as they
were, the strip read as a box. A cell is 112dp tall at 100% type (16 + 6 + 42 + 6 + 20 + 6
+ 16), and the skeleton quotes that.

**8.3b RainChart** — under the strip, the same 24 hours as one series: 2px line on the
**ink** ramp (a mark has its own 3:1 floor, and the fill ramp's light end clears neither
floor), the area under it tinted with the fill ramp at 0.30 → 0.06, three recessive
gridlines at 0 / 50 / 100% with the two ends labelled in the right-hand gutter, a tick
and the hour under the axis every six hours with the first and last always named, and a
dot on every hour. No legend: one series, named by its own caption. It replaced a bare
sparkline on the second device review (6 set 2026) — over a day pinned at 100% a line
with no scale under it is a shape with nowhere to stand, and the flatter the day the
less it said. A dry run still draws nothing at all (§1.1).

**8.4 TimelineRow** — the merged day (VISION §5.2.4): time, icon or event glyph, one line
of prose, optional verdict chip. Sun events, weather turns and the reader's own alerts use
the same row; only the leading glyph differs.

**8.5 DayRow** — weekday, icon, rain probability on the ink ramp (§2.3, zero included),
the **temperature range bar** and the ribbon. The bar is one horizontal track per day, all seven **sharing one scale across the
week** so the week has a shape, filled with the diverging temperature ramp (§2.3) and
anchored at 15 °C; the low and high are printed at its ends in tabular figures, because a
colored bar is not a number.

**8.6 MetricTile** — icon and label; the value as a **reading** (`ReadingValue`: Inter
Light 24sp on a 32sp line, tabular — the hero's voice at a tile's scale, since the card
review of 8 set 2026; at `titleMedium` the value barely outranked its own 14sp label and
the eye went to the icon); where the metric has a scale the world uses, a **4dp track**
in `outlineVariant` filled in `primary` up to the value (UV on 0–11, humidity on 0–100,
air on 0–300 — one hue, anchored to the world, the number printed above it, §9; pressure
and visibility get none, one being a narrow band around 1013 and the other logarithmic);
then the **facts behind the value** in `bodyMedium` — where the wind comes from in words
with an arrow for where it goes, the gusts on the days they matter, the dew point under
the humidity, which pollen — and last the meaning line (`bodySmall`, `onSurfaceVariant`).
Never ships without the meaning line (§1.2). Tapping opens the details sheet at that
metric. Two tiles became one that day: the dew point said "pleasant" under the
humidity's "comfortable", the same fact in two cards, and the dew point is the better
predictor of how the air feels, so it writes the humidity tile's meaning and stays on it
as a note. The air index lost its acronym — "AQI" was the one piece of jargon on a
screen built to have none — and the wind lost its compass abbreviation for the words the
Sky screen already uses for a bearing. The icon is drawn **untinted** like every other
weather icon (§13.1): a flat tint turns the family into silhouettes, and two metrics
whose drawings differ only inside — humidity's drop and its %, the barometer's needle —
become one mark. The label is one line: beside a 34dp icon two columns of a 360dp screen
leave it 84dp, and a label is written to fit that rather than trimmed to it. (It was 94dp
beside a 24dp icon until the family's ladder went up on 6 set 2026 and 88dp beside 30
until the third step on 8 set; the widest label the app ships measures 76.7dp, so the ten
the icon took cost nothing and left 7.3dp of margin, which is where the ladder stops for
this rung. The contract is a 360dp contract: narrower than that the labels wrap and keep
their words, as they already did at 320dp beside the 24dp icon.)

**8.7 VerdictChip** — glyph + word + evidence, in that order: `✓ Great · 12% cloud`. The
container is the verdict container color, the text is the ink color. **Never the color
alone** (§2.3), never a bare dot, never a number without the word.

**8.8 MomentCard** — a sky event: name in plain words ("Golden hour, evening"), time,
verdict chip, the number behind it, a bell for a reminder. The dotted job id never appears.
The leading glyph is the weather family in its own colors at the timeline's rung (34dp),
on the moments, the calendar ahead and the guide's index alike (review, 8 set 2026): it
was a 26dp silhouette in `onSurfaceVariant`, the last place the family was tinted flat,
and tinted flat the full moon and the new moon are the same disc. The catalog's check
marks come from the subscription store, not from the rows on screen, so a subscribed job
with no row today still shows as subscribed.

**8.9 RuleSentence** — the alert builder as a sentence of tappable chips: *Notify me when*
`[rain, next 6 h]` *is* `[above]` `[70%]`. Every chip opens a picker; no free-text field
for a value with a range, which is how tweather's "a syntax error is not writable" property
survives into a UI with no syntax.

**8.10 JournalEntry** and **DriftStrip** — an entry is a line of prose with its numbers.
The drift strip is one row per target day — today included while it runs (8 set 2026) —
and one column per six-hour slot, colored on **the metric's own ramp** (rain on the rain
ramp, temperature on the diverging one) rather than on a good/bad scale: whether Saturday
got "better" is a judgement, and the judgement belongs in the sentence beside the strip,
not in the color. Legend always present, cells ≥ 8dp, and a table view behind a tap or a
long press for anyone who cannot read the colors at all — a grid with the slot's hour over
each column, not a line of arrows. The strip, its chips, the frost line and the sentence
sit in one `surfaceContainer` card. An entry's glyph names its **category** (a revision, a
sky moment observed, an alert fired, a day checked, an update missed) and is a Material
silhouette in `onSurfaceVariant` for all five: these are not weather icons, so §13.1's
"keep their colors" does not reach them, and a monochrome set is the consistent one. The
hour trails the row as a label. A journal day's revisions of one target day fold into one
line, first value to last, saying how many updates it took; a value that came back where
it started is not a change.

**8.11 States** — empty ("no place yet", with the one action that fixes it), error (what
failed, in plain language, and a retry), stale (§8.2), loading (a shimmer that cannot be
mistaken for a value, §1.1).

**8.12 Navigation** — a Material 3 `NavigationBar` with four destinations, a place switcher
in the app bar with a dots indicator, and a horizontal pager between saved places.

---

## 9. Charts and quantities

### 9.1 The three rules

1. **A quantity gets one hue, light to dark** (rain). A quantity with a meaningful middle
   gets two hues and a neutral midpoint (temperature). Never a rainbow, in either case.
2. **One axis.** No chart in this app ever carries two scales.
3. **The scale is anchored to the world, not to the data on screen** — 15 °C for
   temperature, 0–100% for probabilities. A self-scaling axis turns a quiet week into a
   dramatic one.

### 9.2 Marks

2px lines, 4px rounded ends anchored to the baseline, markers ≥ 8dp, a 2px surface gap
between adjacent fills, recessive gridlines (`outlineVariant` at 1dp, horizontal only).

The one deliberate exception to the 8dp marker: a **per-point dot on a dense series** —
the hour dots of §8.3b — is rhythm rather than a marker. It says where the hours are, no
value is ever read off it (the strip above prints all 24), and it is drawn only while the
points are at least 6dp apart. An axis tick is not a gridline and lives outside the plot:
1dp, 3dp long, under the baseline.
Direct labels on the extremes only — never a number on every point.

### 9.3 Every chart has a text equivalent

The rain chart prints the ends of its scale and the hours under it, and the strip above it
prints every one of its values; the range bar's ends are printed, the drift strip has a
table view. This is both the accessibility floor and §1.2: a picture of a number is not a
number.

---

## 10. Accessibility

- **Contrast**: every ink token ≥ 4.5:1 against its surface, measured in §2.3 and asserted
  in §12. Non-text marks ≥ 3:1, with one declared exception measured and argued in §13.1
  (the weather icon on the widget's Cielo card, where the ground is a mid-tone sky). A quantity that is both drawn and printed therefore owns
  two ramps, and the fill one never paints a figure (§2.3). The canvas is covered by the scrim contract (§3.6).
- **Never color alone**: verdicts carry a glyph and a word; the drift strip has a table;
  chart series are direct-labeled.
- **Type scale to 200%**: layouts wrap and reflow, they do not clip or ellipsize a value.
  The week rows and the metric grid are the two places this is tested, and both have a
  `fontScale = 2f` preview beside their normal one so the check is a thing you look at.
  Clipping was never the risk — there is not one `maxLines` in `ui/` — but **a column
  measured in dp holding text measured in sp comes apart on its own**: at 200% the week
  row's four columns, the hour cell, the timeline's clock and the drift strip's date all
  held text twice their width, and nothing was cut off because it wrapped mid-value into
  a line the row had no height for. Two rules, in `ui/theme/TextScale.kt`: a column that
  holds text is **measured in text** (`Dp.forText()`, capped at 2.0 where the system's own
  slider stops), and past **1.5** a row of columns becomes **two rows** — the week row
  splits into "which day, what kind of day" and "how warm", the metric grid drops to one
  column. 1.5 is measured, not round: above it the range bar has under 48dp left, which is
  a smudge and not a bar.
- **TalkBack**: reading order is canvas → sentence → freshness → content. Every icon has a
  description that says the word ("mostly cloudy"), never the glyph — or `null` where the
  words are right beside it and the row speaks once, which is the case for every one of the
  35 nulls in `ui/`. The ribbon reads its phases with times. Charts announce their extremes
  and their current value. Every row that can be removed can be removed without a gesture
  (`customActions`, Fase 9's predecessor pass).
- **Reduced motion**: §7. **Touch targets**: ≥ 48dp, always. Two things the app draws are
  smaller than that — the week row is 46dp (a 38dp icon, 4dp of gap, the 4dp ribbon) and
  the freshness chip 32dp — and the Fase 9 pass went to fix them and found nothing to fix:
  Compose expands a pointer node's bounds to the platform's minimum touch target, so a
  `clickable` of any size is already 48dp to a finger. Worth writing down because the
  expansion has one hole: it reserves no **space**, so two small targets sitting closer
  than their expanded bounds fight over the taps between them. Neither case is that — the
  week's rows are 12dp apart and hold one target each, the chip is alone in its row. If a
  layout ever puts two small targets side by side, `minimumInteractiveComponentSize()` is
  the answer, and it costs layout height; spending that height where nothing is ambiguous
  buys the reader nothing.
- **Color vision**: the status set's measured CVD separation is in §2.3, and the mitigation
  is structural, not hopeful.

---

## 11. Localization in the UI

Everything on screen is prose or data, so **everything localizes** (VISION §8) — there is
no code register in this product to protect. Two mechanical consequences for design:

- Italian runs 15–25% longer than English. Every label is laid out for the longer string;
  no single-line assumption survives without a wrap test.
- Numbers and dates go through the locale's formatter, always: decimal separator, day
  names, 12/24-hour clock. A hand-built `"$h:$m"` is a bug — and so is a hand-built
  `"$pct%"`, which is the same sentence with a different unit and which the Fase 9 IT/EN
  pass found in five places. `Formats.percent` now, and `FormatsTest` checks every
  function in the file against **both** shipped languages side by side, because the
  failure mode of a formatter is being right in the language it was written in.
- A printed value never has a word welded into it in Kotlin. The air-quality tile printed
  its index with the acronym written into the source, and Kotlin is the one place a
  language cannot reach; the whole value is `metric_air_value` now.
- `Formats.dayLong` writes `EEEE d MMMM` rather than asking for a localized skeleton, and
  that is a decision, not an oversight: java.time cannot build "weekday, day, month,
  no year" per locale (that is ICU's `DateTimePatternGenerator`, reachable on Android only
  through `getBestDateTimePattern`, which would cost the file its purity and its unit
  tests). The order is correct for both shipped languages. Revisit it with the third.
- RTL is not a target language today, but no layout may hardcode left/right — start/end
  only, so that decision stays cheap.

---

## 12. Implementation and the guards

```
ui/theme/
  Color.kt      raw tokens, private to the package
  Scheme.kt     the four generated ColorSchemes of §2.2 and §2.5
  ChiaroColors.kt   the semantic extras of §2.3 and §2.5, behind a CompositionLocal
  SkyPalette.kt     the canvas bands of §3.2 and §3.7, and the mixing rules of §3.3–3.4
  Palettes.kt       the two dresses, gathered: scheme + tokens + sky, picked by AppPalette
  Type.kt Shape.kt Motion.kt
  ChiaroTheme.kt    the entry point
```

Three generators sit behind those files and none of their output is hand-edited:
`tools/gen_scheme.py` (the four schemes), `tools/gen_vivid.py` (the vivid semantic tokens
under §2.5's ceiling, and the sky bands under that ceiling plus §3.7's floor) and `tools/gen_vivid_icons.py` (the `mcn_*` icon set), all three on the
shared color arithmetic in `tools/color_math.py`.

Seven tests keep this document from rotting, in the series' habit of turning a design rule
into something CI can fail:

- **`PaletteContrastTest`** asserts every ratio printed in §2.3 and the monotonicity of the
  three ramps, and walks the printed probability from 0 to 100 to hold every step of it
  above the §10 floor. If a token is re-picked, the numbers in this file must be re-measured.
  Since §2.5 it runs over **both dresses**, and adds the two claims that make a second
  dress cheap: the vivid tokens hold the paper luminances, and the diverging ramp's
  midpoint is still a neutral in both.
- **`PaletteDocTest`** asserts the other half of that, and the half that had quietly gone
  wrong: it **reads this file** and checks that §2.2's and §2.5's named roles, §2.3's and
  §2.5's verdict tables and ramps, §3.2's and §3.7's eight bands each, §3.4's moon target
  and §3.6's and §3.7's scrim numbers are the values the app actually holds — hexes *and*
  printed numbers. Every check is **scoped to its section** since the second dress: the
  document now prints two tables of every shape, and a sweep of the whole file would
  happily measure the vivid ramp against the paper Kotlin and pass. It was
  written in Fase 9 because the audit that went looking found the moon's lift target still
  printing its pre-color-pass value, six days after the pass. Everything else matched, so
  the color pass was done; but "was done" is only sayable after looking, and this is what
  looks now.
- **`ScrimContractTest`** asserts §3.6 against the brightest band — of every band table
  (§3.7), at every half-degree, because the scrim composites per channel and that is the
  one contract held luminance does not carry across on its own.
- **`NoRawColorTest`** sweeps the UI sources and fails on a hex literal outside
  `ui/theme/`. It caught its first violation the day it was written — the canvas' own
  scrim color, which now lives in `SkyPalette` where `ScrimContractTest` guards the value
  the canvas actually paints with instead of a copy of it.
- **`SkyPaletteTest`** holds the canvas' claims, for every band table: darker after
  sunset, continuous at every altitude, an anchor renders as itself and a midpoint as
  neither, an overcast midnight is not a dusk, and an overcast full moon does not
  out-shine a clear one. Plus §3.7's own: the two tables anchor at the same altitudes and
  the same luminances, and drift by at most 0.0112 between anchors — because the blend is
  perceptual and a midpoint between two saturated colors is not the midpoint between two
  dull ones.
- **`AnimatedIconTest`** holds §7.1: a moving icon is the still icon (same paths in
  the same order, same colors in the same order — the strongest thing a JVM test can say
  about a drawing it cannot render), every `<target>` names an element the vector really
  has, every animated property belongs to the kind of element it is aimed at, and every
  loop is endless, positive and linear with keyframes that go forwards. Each of those
  four fails silently on a device: an icon that simply does not move, discovered by a
  person. It found its first defect the day it was written — two keyframes of an
  instantaneous jump rounded onto the same instant, which is a jump that never happens.
- **`MotionTest`** holds §7's table and the one rule under it: the three springs are the
  three springs, and every one of them becomes the same 100 ms fade when motion is reduced.
- **`TextScaleTest`** holds §10's two rules and the measurement behind the 1.5 threshold,
  so moving the number means moving the arithmetic that justifies it.
- **`StringsParityTest`** holds §11: every translatable string in both languages, the same
  format arguments, the same plural quantities, no blanks, and no `%` that would throw at
  the moment a sentence is needed. **`FormatsTest`** holds the other side of it — the
  formatter itself, in Italian and English at once.

And two things that are not tests, because they cannot be. **`tools/palette_sheet.py`**
renders both palettes to an HTML sheet, reading the hexes out of the Kotlin sources so it
can never drift from them. Run it and look at the result whenever a color moves: it is
what found that the golden hour was not golden at 3°, which every contrast and
monotonicity test in the suite had passed without complaint.
**`tools/icon_filmstrip.py`** is the same idea for §7.1: it evaluates every animator at a
series of instants and writes the frames out as SVG, because a test can say the rain has
a valid animator and only a person can say the rain falls downward.

---

## 13. Open items

1. ~~The icon family~~ — **shipped in Fase 2: Meteocons v2.0.0**
   (github.com/basmilius/meteocons), MIT, converted to vector drawables by
   `tools/import_meteocons.py` (the importer of record — re-running it IS the import)
   behind `ui/icons/ChiaroIcons`. Two styles of the same drawings since 3 set 2026:
   **line** (the default since 6 set 2026 — one weight of ink on a screen whose hero is
   already a painted sky) and **fill**, chosen in Settings → Appearance; the whole family
   switches together through `LocalWeatherIcons`, and the navigation-bar silhouettes stay
   outside the choice (they are tinted to one color; the styles would be identical).
   Fill was the default from 3 set (solid shapes read faster at small sizes for an
   audience that scans) and the argument held while the sizes were small; the sizes
   moved, so the default did too.
   **The sizes are one ladder, `ui/icons/WeatherIconSize`**, and not four numbers spread
   over four components: hour strip **42dp**, week row **38dp**, timeline row and metric
   tile **34dp**. Each is 10dp above what it was until 6 set 2026 — 4dp, then 2 more on
   a second look that day, then 4 more on 8 set — when the drawings asked to be examined
   rather than recognised on a device; no step changed a padding, an arrangement or a
   column width, so every section kept the rhythm it was tuned to. What they spend
   instead are the elastic measures beside the icons, each measured at 360dp: the 56dp
   hour cell keeps 7dp of air per side and the same 112dp height (the third step took
   over the icon's 2dp of vertical padding), the week's temperature bar and the
   timeline's prose give up 10dp apiece (112→102dp, 232→222dp), and the tile's label
   budget goes 94→84dp against a widest label of 76.7dp. That last margin, 7.3dp, is the
   ladder's ceiling: a fourth step would put «Qualità aria» on two lines at the reference
   width. The third step was argued with a measurement first (8 set 2026): the obvious
   alternative was to let the drawings fill more of their own 64-unit box, and the ink of
   the 18 condition icons, measured over the whole animation loop, uses it — x 6.0–61.6,
   y 8.0–60.0, drops falling to y 60 and the drifting overcast reaching x 61.6 — so a
   uniform crop could take 2.4 units, under 4%, and would move the optical centres. The
   size moved and the viewport did not. What makes the family read small is the plain
   cloud: 30 units tall in a 64 box, 20dp of drawing in a 42dp icon. The order of the
   rungs is the reading order: the strip is scanned sideways and carries the most weight,
   the week is read down, a line of prose leads with the smallest glyph.
   Gradients are flattened to their face color (a two-stop ramp is invisible at
   30–38dp, and was at the 24–32 of the import) and hairline edge strokes are dropped.
   **The SMIL is no longer dropped** (7 set 2026): each of the four sets has an animated
   twin — `mca_*`, `mcan_*`, `mcaf_*`, `mcafn_*` — carrying the illustrator's own motion
   as an `AnimatedVectorDrawable`, for the eighteen drawings of the condition family that
   have any. The rule for which move and when is §7.1; the colors are the same table, so
   nothing in this section's arithmetic changes. On color, the departure worth
   this document's attention: **the palette is re-anchored, not copied — and since
   the icon pass (3 set 2026) the fill set ships twice, picked by its ground.**
   Meteocons draws for a dark backdrop: its cloud stroke is `#E5E7EB`, 1.18:1 against
   this app's light surface, and in the hour strip the icon is the only carrier of
   "what kind of weather", so the marks owe §10's 3:1 non-text floor. A single asset
   owing 3:1 on BOTH surfaces of §2.2 is forced into `Y ∈ [0.120, 0.283]` — correct
   arithmetic, muted result, and the reason the fill set is now two sets. The line
   set (`mc_*`) serves both themes and keeps that band, its chroma raised ×1.25 at
   held luminance (the color pass' trick: same measured ratios, fuller color); the
   fill set for light grounds (`mcf_*`) gets the same treatment. The fill set for
   dark grounds (`mcfn_*`) is **Meteocons' own fill palette, verbatim**, except the
   four near-black details lifted to clear 3:1 against the dark surface.
   `ChiaroIcons` picks by ground — the app by its applied theme, the widgets by
   their card's own `darkGround` — so each set only ever meets the surface its floor
   was measured against, and `IconContrastTest` re-measures the emitted XML per set
   on every build, in the same spirit as `PaletteContrastTest`: assert the outcome,
   not the method.

   **A fourth set arrived with the vivid palette** (7 set 2026): `mcn_*`, the LINE set
   for dark grounds, generated from `mc_*` by `tools/gen_vivid_icons.py` with §2.5's
   rule and the both-grounds ceiling taken off. That ceiling — `Y ≤ 0.283` — is the
   whole reason the app's sun is a bronze, and on a dark ground it does not apply: the
   sun goes `#C37D00` → **`#FFA500`**, the rain `#0085D0` → `#00A4FF`, the moon and snow
   `#008AB6` → `#02C3FF`, while the near-neutral cloud strokes stay near-neutral because
   ×1.8 of a small chroma is a small chroma. Only the vivid palette reads it, and only on
   a dark ground; paper keeps one line family on both, because a consistent muted set is
   part of what paper means. On a LIGHT ground the vivid palette gets the same files as
   paper and that is the gamut talking: at `Y ≤ 0.284` there is no bright yellow to pick,
   whatever the dress asks for. A reader who wants one there is asking to trade §10's
   floor, which is a decision and not a color. The icons keep their own colors under every theme (they depict
   the world, like the canvas — §2.1's other justified exception). Animated
   variants, if they ever come, come as AVDs and as their own decision.

   **The one ground no icon set clears, declared** (7 set 2026, measured from a device
   report that the app's sun looks darker than the widget's — which it is, and by
   design: `mc_`/`mcf_` sit at Y 0.26, `mcfn_` at Y 0.58). The widget's **Cielo** card
   is the scrimmed sky, and at its brightest that ground is `#5C6E7B`, **Y 0.149** — a
   mid-tone. An ink clears 3:1 there only at Y ≥ 0.546 or Y ≤ 0.016, with nothing in
   between, and neither set lives in those bands: **8 of the line set's 8 colors fall
   short (1.03–1.57:1), and 25 of the fill-night set's 37 (1.09–2.93:1)**. The sun is
   one of the twelve that pass, which is why the card looks right and is the reason
   this went unseen. The line sun drops under the floor from −7° of solar altitude
   upward: every daylight hour.

   Three ways out were measured and all three were turned down. Splitting the line set
   by ground the way the fill set was split does not help: the app's dark surface
   (Y 0.008) is already served at 5.52:1, so such a set would exist only for the sky
   and would have to put **every** color above Y 0.546 — a near-white family where a
   cloud, a raindrop and a sun stop being different things, in ~98 new drawables.
   Raising the scrim needs alpha ≈ 0.93 to bring the ground to Y 0.013, which is a black
   card with the memory of a sky behind it. Tinting the glyph to the card's own ink is
   the cheap and complete fix — white measures 5.29:1 there, `ColorFilter.tint` is
   already how the position pin is drawn — and **the committente turned it down on
   product grounds: the colored icons are much of what makes the widget worth looking
   at, and a monochrome silhouette buys a ratio at the cost of the thing itself.**

   The vivid palette's `mcn_*` narrows the shortfall without closing it, and the number
   is worth recording because it was measured on the way past: on that same `#5C6E7B`
   ground the line sun goes from **1.57:1 to 2.68:1** and the set's range from 1.03–1.57
   to 1.03–2.68. Still 8 of 8 below the floor, so the exception below stands unchanged
   for both dresses — but the icon a reader actually looks at on that card is now most of
   the way there, which is the closest thing to a free improvement this shortfall has had.

   So this is an accepted, bounded exception to §10's 3:1 for non-text marks, and it is
   bounded: it applies to the **Cielo** background only. The Chiaro, Scuro and Sistema
   cards are the app's own two surfaces, which `IconContrastTest` measures, and the app
   itself never puts a weather icon on the canvas. Nothing else on the Cielo card relies
   on it either — the condition is named in words beside the glyph, so the icon is not
   the sole carrier there the way it is in the hour strip, which is what §10's floor is
   protecting. If it is ever reopened, the option that keeps both is a small darkened
   plate under the glyph: contrast without giving up the color.
2. ~~Dynamic color default~~ — **resolved 7 set 2026, the other way**: dynamic color
   ships **off**, and the generated vivid scheme of §2.5 is what a fresh install wears.
   The reason the item existed was that a wallpaper-derived scheme makes every store
   screenshot a different app; the reason it resolved this way is that it also makes the
   palette choice invisible — with dynamic color on, a dress reaches only the ramps and
   the sky, which is exactly what the first device look reported as "barely a change".
   On device and in the store, the app now looks like itself. It stays one tap away for
   readers who want their wallpaper back.
3. ~~The brand mark~~ — **shipped, 3 set 2026**: the icon family's starry-night
   crescent, fill style, set low in the badge over two calm waves; everything inside the
   33-unit safe circle, and the `<monochrome>` layer reuses the same drawable.
   **Repainted in the vivid dress, 7 set 2026** (committente), that being the palette a
   fresh install wears since item 2 above resolved. It is hand-drawn, so it does not
   come out of a generator, but it moves by the same rule `tools/gen_scheme.py` uses:
   each ink keeps the CIELAB lightness it was drawn at and takes its hue and chroma from
   the vivid source of §2.5. Moon and near wave `#3589AC` → `#317DF5`, stars `#C27D08` →
   `#BB8000`, far wave `#6493A5` → `#6288E8`, ground `#F4F1EA` → `#E9F2FC`. Holding the
   lightness holds the badge's contrasts against its own ground — 2.99 → 3.00 stars,
   3.50 → 3.45 moon, 2.97 → 2.99 far wave — and the stars barely move because at L\* 58
   both palettes' ambers are already on the sRGB gamut edge, which is the same fact
   §2.5 records about the tokens that do not move.
4. **`heroTemperature`** is an extended type role, not a Material one. Confirm it survives
   contact with the expressive scale rather than becoming `displayLarge` with a tighter
   line height.
