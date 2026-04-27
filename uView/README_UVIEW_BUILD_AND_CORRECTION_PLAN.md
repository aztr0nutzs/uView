# uView Camera Build and Correction Plan

## Purpose
This package contains a bulletproof multi-phase build and correction plan for the attached uView Camera project, with task-specific prompts for each phase.

This plan assumes the following non-negotiable visual contract:

- The three HTML files at the root of the project zip are authoritative visual references and must be used:
  - `uview_screen1.html`
  - `uview_screen2.html`
  - `uview_screen3.html`
- Their exact colorway, tactical HUD styling, typography feel, spacing density, edge treatments, glow usage, framing language, and layout hierarchy must be preserved.
- Redesign is allowed only where needed to complete missing states or improve usability without changing the visual identity.
- The project must not be flattened into generic Material UI.

## Required Screen Authority Map

- `uview_screen1.html` -> dashboard / camera list / tactical hub
- `uview_screen2.html` -> multi-view tactical grid
- `uview_screen3.html` -> camera detail / live feed

## Recommended Execution Order

1. Phase 1 - Establish the UI authority layer
2. Phase 2 - Build the shared tactical design system in Compose
3. Phase 3 - Rebuild the dashboard and camera list from `uview_screen1.html`
4. Phase 4 - Rebuild the multi-view tactical grid from `uview_screen2.html`
5. Phase 5 - Rebuild the camera detail screen from `uview_screen3.html`
6. Phase 6 - Correct architecture drift and wire screens to real state
7. Phase 7 - Complete the unfinished backend features honestly
8. Phase 8 - Security and production hardening
9. Phase 9 - Build integrity, packaging, and contributor handoff

## Global Prepend Block for Every Future Task

```text
UI PRESERVATION IS MANDATORY.

The root HTML files are authoritative visual references and must be used:
- uview_screen1.html
- uview_screen2.html
- uview_screen3.html

Do not alter their exact colorway, tactical HUD styling, typography feel, spacing density, edge treatments, glow usage, framing language, or layout hierarchy except where necessary to complete missing states or improve usability without changing the visual identity.

Do not replace this design with generic Material UI.
Do not flatten the layout.
Do not remove density or industrial framing.
Do not introduce temporary simplifications.
Extend existing UI carefully and preserve the established visual contract.
```

---

## Phase 1: Establish the UI authority layer

### Goal
Turn the three root HTML files into the formal UI spec and map them to actual Compose screens and reusable design primitives.

### What this phase must do
- Audit current Compose screens against the 3 HTML files
- Produce a screen-to-screen mapping table
- Identify reusable primitives versus one-off layouts
- Extract shared design language:
  - colors
  - typography roles
  - spacing rhythm
  - border treatments
  - corner/chamfer behavior
  - glow/shadow usage
  - icon treatment
  - panel/header/footer patterns
- Define a do-not-alter visual contract doc
- Confirm where existing `ui` components can stay and where they need extension

### Deliverables
- UI parity audit
- screen mapping table
- reusable component inventory
- token map from HTML to Compose/theme
- visual contract doc

---

## Phase 2: Build the shared tactical design system in Compose

### Goal
Create or correct the theme and shared components so all three screens can be implemented without style drift.

### What this phase must do
- Lock the app color palette to the HTML color family
- Define typography roles matching the HTML feel
- Build shared primitives for:
  - tactical top app bars
  - panel shells
  - chips
  - tab bars
  - stat blocks
  - feed cards
  - action strip buttons
  - section headers
  - overlay badges
- Add support for glow, bevel, inset, and chamfer styling in Compose
- Avoid hardcoding random per-screen styling where a reusable primitive belongs

### Deliverables
- corrected theme tokens
- reusable shared UI primitives
- preview coverage for all core components

---

## Phase 3: Rebuild the dashboard and camera list from `uview_screen1.html`

### Goal
Replace the current dashboard/list screen with a faithful Compose translation of the tactical hub screen.

### What this phase must do
- Recreate the header style and status area
- Build the signal filter/search shell
- Build room/sector chip row
- Recreate active recon feeds list styling
- Translate each list item into a real Compose card/tile with live status and data bindings
- Preserve dense layout and tactical feel

### Deliverables
- corrected dashboard screen
- corrected camera list rendering
- navigation intact to camera detail

---

## Phase 4: Rebuild the multi-view tactical grid from `uview_screen2.html`

### Goal
Make the multi-view screen visually and structurally match the tactical grid HTML while wiring real state.

### What this phase must do
- Recreate tactical grid shell
- Implement camera tiles in 2x2 grid first, scalable later
- Support online/offline indicators
- Add feed overlay labels and badges
- Implement deploy/monitor/grid mode controls as real UI actions or safely disabled actions
- Preserve aggressive grid framing and tactical composition

### Deliverables
- corrected multi-view grid screen
- screen state support for multiple feeds
- functional selection and navigation behavior

---

## Phase 5: Rebuild the camera detail screen from `uview_screen3.html`

### Goal
Make the detail/live-view screen match the HTML authority file while becoming the central screen for playback, actions, events, diagnostics, and settings handoff.

### What this phase must do
- Recreate the live feed shell and overlay treatment
- Implement action strip matching the HTML
- Implement tabs for:
  - Live
  - Details
  - Events
  - Diagnostics
- Rebuild metrics and diagnostics panels in the same style
- Keep playback/viewmodel behavior intact while fixing layout parity

### Deliverables
- corrected detail/live screen
- tabbed content sections
- consistent overlay/action strip styling

---

## Phase 6: Correct architecture drift and wire screens to real state

### Goal
Once the UI shell is correct, fix the data flow so the screens are not decorative lies.

### What this phase must do
- Map screen state to repositories/viewmodels cleanly
- Remove fake/manual seeded UI state where possible
- Ensure dashboard, grid, and detail all read from the same camera/event truth sources
- Fix dead buttons, placeholder actions, and state mismatch
- Ensure navigation and shared state are coherent

### Deliverables
- corrected state flow
- working viewmodel wiring
- removal of fake UI-only state

---

## Phase 7: Complete the unfinished backend features honestly

### Goal
Implement the missing functional areas that the earlier report overstated.

### What this phase must do
Priority order:
1. real local recording pipeline
2. snapshot persistence cleanup if needed
3. diagnostics completion
4. settings actions completion
5. multi-view action behavior completion

### Deliverables
- actual recording behavior or clearly bounded fallback
- completed detail screen actions
- diagnostics that reflect real capabilities

---

## Phase 8: Security and production hardening

### Goal
Finish the hardening work that exists partly in code and partly in wishful thinking.

### What this phase must do
- fix credential encryption edge cases
- remove insecure fallback behavior
- wire real biometric lock
- replace destructive migration shortcuts with real migrations
- wire crash reporting abstraction cleanly
- review export/import safety
- keep the UI intact

### Deliverables
- safer credential handling
- real app lock
- migration plan
- crash reporting integration point

---

## Phase 9: Build integrity, packaging, and contributor handoff

### Goal
Make the project actually handoff-ready.

### What this phase must do
- restore missing Gradle wrapper files
- confirm clean project structure
- verify manifest/service declarations
- verify icons/assets
- add contributor docs:
  - AGENTS.md
  - MASTER_INSPECTION.md
  - UI_PRESERVATION.md
  - BUILD_VERIFICATION_CHECKLIST.md
- add a screen authority doc referencing the 3 root HTML files

### Deliverables
- buildable project package
- contributor-safe documentation
- verification checklist
