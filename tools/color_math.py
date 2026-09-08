#!/usr/bin/env python3
"""sRGB, WCAG luminance and OKLCh, shared by the palette generators.

Two facts the generators lean on and the tests re-measure independently:

- **WCAG contrast is a function of luminance alone.** Hold a token's luminance and
  every ratio printed in DESIGN.md survives whatever else is done to the color.
- **sRGB has a chroma ceiling that depends on luminance.** That is why the vivid
  palette is not the paper palette with the saturation slider pushed: at the
  luminance the contrast floor allows, some hues have nothing left to give, and the
  only way to more chroma is to move the luminance to where the gamut is wider.
"""
from __future__ import annotations

import math


def srgb_to_linear(c: float) -> float:
    return c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4


def linear_to_srgb(c: float) -> float:
    return 12.92 * c if c <= 0.0031308 else 1.055 * (c ** (1 / 2.4)) - 0.055


def hex_to_rgb(value: str) -> tuple[float, float, float]:
    h = value.lstrip("#")
    return tuple(int(h[i:i + 2], 16) / 255 for i in (0, 2, 4))


def rgb_to_hex(rgb) -> str:
    return "#{:02X}{:02X}{:02X}".format(*(max(0, min(255, round(v * 255))) for v in rgb))


def luminance(rgb) -> float:
    r, g, b = (srgb_to_linear(v) for v in rgb)
    return 0.2126 * r + 0.7152 * g + 0.0722 * b


def contrast(a, b) -> float:
    ya, yb = luminance(a), luminance(b)
    hi, lo = max(ya, yb), min(ya, yb)
    return (hi + 0.05) / (lo + 0.05)


def rgb_to_oklab(rgb):
    r, g, b = (srgb_to_linear(v) for v in rgb)
    l = 0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b
    m = 0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b
    s = 0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b
    l_, m_, s_ = (v ** (1 / 3) if v >= 0 else -((-v) ** (1 / 3)) for v in (l, m, s))
    return (
        0.2104542553 * l_ + 0.7936177850 * m_ - 0.0040720468 * s_,
        1.9779984951 * l_ - 2.4285922050 * m_ + 0.4505937099 * s_,
        0.0259040371 * l_ + 0.7827717662 * m_ - 0.8086757660 * s_,
    )


def oklab_to_rgb(lab):
    L, a, b = lab
    l_ = L + 0.3963377774 * a + 0.2158037573 * b
    m_ = L - 0.1055613458 * a - 0.0638541728 * b
    s_ = L - 0.0894841775 * a - 1.2914855480 * b
    l, m, s = (v ** 3 for v in (l_, m_, s_))
    return tuple(
        linear_to_srgb(v)
        for v in (
            +4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s,
            -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s,
            -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s,
        )
    )


def rgb_to_oklch(rgb):
    L, a, b = rgb_to_oklab(rgb)
    return L, math.hypot(a, b), math.atan2(b, a)


def oklch_to_rgb(L: float, C: float, hue: float):
    return oklab_to_rgb((L, C * math.cos(hue), C * math.sin(hue)))


def in_gamut(rgb, eps: float = 1e-4) -> bool:
    return all(-eps <= v <= 1 + eps for v in rgb)


def max_chroma(L: float, hue: float, ceiling: float = 0.5) -> float:
    """The most chroma sRGB holds at this Oklab lightness and hue."""
    lo, hi = 0.0, ceiling
    for _ in range(40):
        mid = (lo + hi) / 2
        if in_gamut(oklch_to_rgb(L, mid, hue)):
            lo = mid
        else:
            hi = mid
    return lo


def at_gamut_edge(target_y: float, hue: float, chroma_cap: float | None = None):
    """The most saturated sRGB color of this hue whose WCAG luminance is `target_y`.

    Lightness is what gets solved for: chroma is taken to the gamut edge (or to
    `chroma_cap`, for the tokens that are deliberately quiet), and then Oklab's L is
    moved until the luminance is the one asked for. Returns an (r, g, b) triple, or
    None when no color of that hue reaches that luminance.
    """
    lo, hi = 0.0, 1.0
    for _ in range(48):
        mid = (lo + hi) / 2
        c = max_chroma(mid, hue)
        if chroma_cap is not None:
            c = min(c, chroma_cap)
        y = luminance(tuple(min(1.0, max(0.0, v)) for v in oklch_to_rgb(mid, c, hue)))
        if y < target_y:
            lo = mid
        else:
            hi = mid
    L = (lo + hi) / 2
    c = max_chroma(L, hue)
    if chroma_cap is not None:
        c = min(c, chroma_cap)
    rgb = tuple(min(1.0, max(0.0, v)) for v in oklch_to_rgb(L, c, hue))
    return rgb if abs(luminance(rgb) - target_y) < 5e-3 else None


def chroma_ceiling(target_y: float, hue: float) -> float | None:
    """How much chroma sRGB holds at this hue once the luminance is fixed at `target_y`.

    The denominator the sky's floor is a fraction of. It is NOT `max_chroma`: that one
    is asked at an Oklab lightness, and holding a WCAG luminance means solving for the
    lightness first, which is what [at_gamut_edge] does. Returns None where no color of
    this hue reaches that luminance at all.
    """
    rgb = at_gamut_edge(target_y, hue)
    return None if rgb is None else rgb_to_oklch(rgb)[1]
