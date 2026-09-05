# Smart EV Assistant visual system

## Direction

Cinematic automotive compositions inspired by the Nguyan reference: near-black
surfaces, ember illumination, oversized condensed headings, angular controls,
asymmetric marketing sections, and denser operational screens. Availability
always includes a text label and is explicitly described as simulated.

## Tokens

- Void `#050505`, canvas `#0A0909`, surface `#141110`, elevated `#1D1715`
- Border `#3A2D28`, primary text `#F7F2EE`, muted text `#A79E98`
- Ember `#FF4D1F`, hot ember `#FF7138`, deep ember `#B92B10`
- Focus `#FFB69A`; distinct green, amber, blue, and red semantic states
- Display/numerals: self-hosted Barlow Condensed; interface: self-hosted Manrope
- Spacing follows a 4/8/12/16/24/32/48/72 scale; controls are at least 44px
- Angular surfaces and restrained corners establish the cinematic visual language

## Layout sketches

```text
LANDING / DESKTOP
┌────────────────────────────────────────────────────────────┐
│ mark       Product  How it works               Sign in CTA │
├────────────────────────────────────────────────────────────┤
│ CHARGE.                     Full-bleed local hero video    │
│ MOVE.                       with poster and overlays      │
│ ARRIVE.                                                   │
│ [Find stations] [Learn]                                    │
├────────────────────────────────────────────────────────────┤
│ availability facts ┃ discovery ┃ community confidence      │
└────────────────────────────────────────────────────────────┘

APP / DESKTOP                         APP / MOBILE
┌──────────── top navigation ──────┐  ┌──── compact nav ────┐
│ purpose + primary action         │  │ page purpose         │
├──────────┬───────────────────────┤  ├──────────────────────┤
│ list 42% │ database map 58%       │  │ list / map switcher  │
│ filters  │ selected station      │  │ stacked results      │
└──────────┴───────────────────────┘  └──────────────────────┘
```

Left alignment is the default. Numbers use tabular alignment. Center alignment is
reserved for selected marketing sections and empty/error states. Navigation
collapses at 860px to accommodate the authenticated link set.

## Motion

GSAP 3.15 and ScrollTrigger are vendored locally for the hero sequence, occasional
parallax, and counters. Frequent interactions use CSS transitions. Motion primarily
changes transform and opacity. Fine-pointer detection gates magnetic interactions.
Reduced-motion rules remove spatial effects and pause the hero video.

The intro uses `sessionStorage.smartEvIntroSeen` and begins dismissal on the first
ready video frame, video failure, or a 1.2-second deadline. Video uses autoplay,
muted, loop, playsinline, no controls, and a local JPEG poster.

## Map states

Maps display only database stations. Without credentials, map panels show a useful
unavailable state and other application functions remain usable. See the README
for key restrictions and `verification.md` for the remaining live checks.
