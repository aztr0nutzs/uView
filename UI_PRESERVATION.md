# UI Preservation Guidelines

Sentinel Home’s appeal lies in its clean, futuristic heads‑up display
style. To preserve this experience, follow the principles below when
modifying or extending the user interface.

## Core Principles

1. **Consistency over novelty.** When adding new screens or controls,
   mirror the spacing, typography and colour palette of existing
   components. The Compose components defined in
   `com.sentinel.app.ui.components` should be your first choice.
2. **Neon palette fidelity.** Use the established colours from
   `SentinelTheme`—deep backgrounds (`BackgroundDeep`), primary text
   (`TextPrimary`), neon accents (`NeonOrange`, `CyanPrimary`) and
   status indicators (`StatusOnline`, `WarningAmber`). Do not introduce
   arbitrary colours that break the theme.
3. **Adapt, don’t overwrite.** If a new feature requires a variation of
   an existing component, extend it rather than replacing the original.
   For example, create a `SettingsToggleRow` variant rather than
   changing the existing one. This minimises regression risk for
   existing screens.
4. **Respect layout proportions.** The top bars, section cards and
   camera tiles have been carefully sized. Ensure that new elements
   align visually and do not crowd the screen.
5. **Iconography matters.** The adaptive launcher icon is part of the
   brand. When adding icons for actions or statuses, choose ones from
   Material Icons that fit the futuristic, tactical theme.

## Process

- **Design review:** Before merging UI changes, conduct a visual review
  comparing new screens to the mandatory root HTML authority files
  (`uview_screen1.html`, `uview_screen2.html`, `uview_screen3.html`).
  These files are not optional inspiration. `uview_screen1.html` governs
  dashboard / camera list surfaces, `uview_screen2.html` governs the
  multi-view tactical grid, and `uview_screen3.html` governs camera detail /
  live feed surfaces. Deviations must be justified and reviewed against
  `SCREEN_AUTHORITY_MAP.md`.
- **Dark mode first:** Sentinel uses a dark theme by default. Test all
  new UI elements under dark theme conditions. If light mode is
  introduced later, define equivalent colours in `Theme.kt` before
  switching.
- **Testing with real data:** Use realistic camera names, rooms and
  stream states to ensure that dynamic content fits within the
  component constraints and does not overflow.

By adhering to these guidelines, you will preserve the integrity of the
Sentinel Home UI and provide a cohesive user experience.
