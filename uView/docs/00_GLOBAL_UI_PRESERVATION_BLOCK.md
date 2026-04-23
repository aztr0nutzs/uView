# Global UI Preservation Block

Use this block at the top of every future implementation prompt:

```text
UI PRESERVATION IS MANDATORY.

The root HTML files are mandatory screen authority assets, not optional
inspiration, and must be used:
- uview_screen1.html
- uview_screen2.html
- uview_screen3.html

Do not alter their exact colorway, tactical HUD styling, typography feel, spacing density, edge treatments, glow usage, framing language, or layout hierarchy except where necessary to complete missing states or improve usability without changing the visual identity.

Required mappings:
- uview_screen1.html -> dashboard / camera list
- uview_screen2.html -> multi-view tactical grid
- uview_screen3.html -> camera detail / live feed

Any visual deviation must be justified in the change description and reviewed against SCREEN_AUTHORITY_MAP.md.

Do not replace this design with generic Material UI.
Do not flatten the layout.
Do not remove density or industrial framing.
Do not introduce temporary simplifications.
Extend existing UI carefully and preserve the established visual contract.
```
