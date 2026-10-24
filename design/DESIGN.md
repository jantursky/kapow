# Kapow design system

Used by the effect gallery (`samples/web`), its Compose widgets (`samples/design`) and the docs site
(`apps/docs`). Dusk-lit workspace look: a matte near-black canvas, graphite panels, frosted glass cards,
white pill buttons and medium-weight headlines. Color comes from the effects themselves; the chrome stays
monochrome, with one violet used only as light (glows, gradient washes, focus).

Dark is the primary theme. The light theme mirrors it: paper-white canvas, ink pills, the same violet light.

Code form: `design/tokens.css` (web, shared by the gallery page and the docs theme) and
`samples/design` (Compose `KapowTheme`). Every surface uses the tokens; no ad-hoc colors.

## Color

| Token | Dark | Light | Role |
| --- | --- | --- | --- |
| `canvas` | `#0a0a0a` | `#fafafa` | Page background (level 0) |
| `panel` | `#161616` | `#ffffff` | Nav bar, sidebars, code panels, modal bodies (level 1) |
| `glass` | `rgba(212,212,212,0.10)` | `rgba(22,22,22,0.04)` | Frosted cards, effect stages (level 2) |
| `stage` | `#111111` | `#f2f2f2` | Background behind a live effect, so particles read on both themes |
| `hairline` | `rgba(229,229,229,0.16)` | `rgba(22,22,22,0.12)` | 1px borders on controls and cards |
| `title` | `#ffffff` | `#0a0a0a` | Headlines |
| `ink` | `#ededed` | `#161616` | Body text |
| `ash` | `#c2c2c2` | `#3d3d3d` | Secondary text, helper copy |
| `slate` | `#8a8a8a` | `#6b6b6b` | Muted text, metadata, line numbers |
| `action` | `#ffffff` | `#161616` | The one filled button (white pill on dark, ink pill on light) |
| `on-action` | `#161616` | `#ffffff` | Text on `action` |
| `violet` | `#6b62f2` | `#6b62f2` | Glows, gradient washes, focus ring. Never a fill for text or buttons |
| `violet-wash` | 90° band, violet at 56 % in the middle 20 % | same | Highlight strip behind the active nav item |
| `success` | `#7ee2a8` | `#14804a` | Frame-time meter under budget |
| `warning` | `#ffcf5c` | `#a15c00` | Frame-time meter over budget |

The source system lists `#e5e5e5` for hairlines on dark. At full strength that reads as a bright outline on
`#0a0a0a`, so Kapow uses it at 16 % opacity, which matches how the reference renders.

Effect defaults in the gallery use their own palette (warm amber `#ffb547`, coral `#ff6b5a`, violet
`#6b62f2`, cobalt `#3d7bff`), the four stops of the hero horizon gradient. That keeps particles and glows
colorful while the chrome stays grey.

## Typography

- **DM Sans** for display and body (SIL Open Font License; npm `@fontsource-variable/dm-sans`, bundled as a
  font resource in the gallery). Weight 500 for headlines, 400 and 500 for body.
- **Geist** for section headings 24 px and up (SIL Open Font License; npm `@fontsource-variable/geist`).
- **Geist Mono** for code (SIL Open Font License; npm `@fontsource-variable/geist-mono`).
- Fallback stack: `Inter, system-ui, sans-serif`. Tabular numerals for timings and frame times.

| Role | Font | Size / line height | Weight | Tracking |
| --- | --- | --- | --- | --- |
| display | DM Sans | 72 / 72 | 500 | -2.52px |
| heading-lg | Geist | 48 / 48 | 500 | 0 |
| heading | Geist | 36 / 40 | 500 | 0 |
| heading-sm | Geist | 24 / 32 | 500 | 0 |
| subheading | DM Sans | 18 / 27 | 500 | 0 |
| body | DM Sans | 16 / 24 | 400 | 0 |
| caption | DM Sans | 13 / 20 | 500 | +0.33px |
| code | Geist Mono | 13 / 20 | 400 | 0 |

Headlines never go above weight 500. Geist is not used for body text.

## Spacing

Scale: 4, 6, 8, 10, 12, 14, 16, 20, 24, 28, 32, 40, 48, 56. Element gap 8 to 16. Card padding 28 (web) /
20 (compact). Section gap 72. Content column max 1200 px, centered.

## Radius

| Element | Radius |
| --- | --- |
| Icons, kbd | 4 |
| Inputs, segmented controls, code blocks | 10 |
| Floating nav | 19 |
| Cards, effect stages | 24 |
| Large panels | 40 |
| Buttons, pills, tags, nav links | 9999 |

## Borders and elevation

Depth comes from surface contrast, translucency and hairlines, not shadows. One exception: the floating
nav carries a faint layered shadow so it separates from scrolling content. Frosted cards have no border;
controls have a 1 px `hairline` border. Frosted surfaces use `backdrop-filter: blur(4px)`.

## Components

- **Primary pill**: `action` fill, `on-action` text, radius 9999, padding 8 × 14, DM Sans 14/500. One per view
  ("Fire", "Show").
- **Ghost pill**: transparent, `ink` text, 1 px `hairline`, radius 9999, padding 6 × 14.
- **Segmented control**: `glass` track, radius 10, selected segment `panel` with `hairline` border.
- **Slider**: 2 px `hairline` track, `action` filled part, 16 px `action` thumb with a violet focus glow.
- **Effect card**: `glass` background, radius 24, 28 padding; inside it a `stage` area (radius 16) for the
  live effect, controls below, source in a `panel` code block (radius 10, Geist Mono 13).
- **Floating nav**: `panel` at 80 % with 4 px backdrop blur, 1 px `hairline`, radius 19, 16 px from the
  viewport edge.
- **Focus**: 2 px `violet` ring at 60 % plus a 6 px violet glow at 25 %.

## Motion

The gallery chrome itself barely moves (150 ms fades on hover and theme change). Motion is the effects'
job. With reduced motion the chrome drops even those fades.

## Logo

Colors come from Kotlin, not from the system above: gradient `#7f52ff` → `#e44857`. Dark-mode variant
`#9b7bff` → `#ff6b7a`, so the mark keeps its contrast on `#0a0a0a`. Tokens `--logo-start` / `--logo-end`.
The mark is a comic burst with a block K cut out, tilted by 8 degrees. `design/brand/logo.py` draws it and
writes `mark.svg` (switches colors with the system theme), `icon.svg` (light) and `icon-dark.svg` (dark).

## Do

- White pill on dark, ink pill on light, and only one per view.
- Violet only as light: glows, washes, focus.
- Let the effect stage carry the color; keep chrome grey.
- Hairlines at low opacity; let translucency separate layers.

## Don't

- No box shadows for elevation (floating nav excepted).
- No solid violet buttons, text or backgrounds.
- No headline above weight 500.
- No radius below 4 or above 40 on cards and panels.
- No extra brand colors in the chrome.
