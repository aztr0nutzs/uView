# Screen Authority Map

This file is the mandatory design authority map for the uView package. The
root HTML files are not optional inspiration, mockups, or loose visual
references. They are the screen contracts for the Compose implementation in
`uView/`.

## Authoritative Root HTML Files

| Root HTML authority | Required Compose surface | Primary implementation files |
| --- | --- | --- |
| `uview_screen1.html` | Dashboard / camera list / tactical hub | `uView/app/src/main/java/com/sentinel/app/features/dashboard/DashboardScreen.kt`, `uView/app/src/main/java/com/sentinel/app/features/cameras/CameraListScreen.kt` |
| `uview_screen2.html` | Multi-view tactical grid | `uView/app/src/main/java/com/sentinel/app/features/multiview/MultiViewScreen.kt` |
| `uview_screen3.html` | Camera detail / live feed | `uView/app/src/main/java/com/sentinel/app/features/cameras/detail/CameraDetailScreen.kt` |

The same files are mirrored into
`uView/app/src/main/assets/design-reference/` so the Android project carries
its own copy of the design contract while the root originals remain preserved.

## Required Shared Tactical Primitives

The shared Compose design system must preserve primitives derived from the
three HTML authority files:

- Tactical top bars with dark surface framing, orange active typography, dense
  status readouts, and hard bottom borders.
- Signal filter/search shells with black input wells, cyan labels, green focus
  accents, and compact tactical typography.
- Sector/room chips with low-radius edges, orange selected states, cyan inactive
  states, and dense horizontal rhythm.
- Camera/feed tiles with chamfered or hard-edged media frames, corner fasteners,
  live/offline overlays, status badges, UUID/unit labels, and bottom metadata
  scrims.
- HUD panels with left accent bars, fastener dots, dense key/value rows, and
  orange/cyan/green/red semantic color usage.
- Action strips with six dense controls, icon-led affordances, bottom accent
  bars, active/recording pulse states, and no generic card replacement.
- Tab bars with hard underline treatment, italic uppercase labels, and compact
  spacing.
- Bottom navigation and dock elements that preserve the dark framed tactical
  language from the authority HTML files.

## Review Rule

Any visual deviation from the mapped HTML authority files must be justified in
the change description and reviewed against this file before merge. Acceptable
reasons are limited to implementing missing states, binding real application
state, accessibility fixes, or adapting the same visual family to device size.
Do not replace, flatten, simplify, or modernize the tactical HUD identity.
