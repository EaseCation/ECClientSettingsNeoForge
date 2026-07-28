# Independent And Combined CPS HUD

## User Need

Players who only want click speed should not need to enable the larger Keystrokes layout. Some players
need independent placement for each button, while others prefer a single compact line, so both layouts
are available.

## Interaction Model

`LEFT_CPS` and `RIGHT_CPS` are separate standard HUD widgets. `COMBINED_CPS` is an additional widget
that renders the same values as `CPS 5 | 0`. Each can be independently:

- enabled or disabled from the HUD category;
- selected, dragged, snapped, scaled, and reset in the visual editor;
- configured with its own background, border, padding, shadow, fixed color, or RGB text style;
- persisted per Profile without changing the Keystrokes content settings.

The default positions form a vertical stack below FPS at the upper-left safe inset. Both widgets are
disabled by default, avoiding an unexpected overlay while still giving the editor a predictable initial
layout. The combined widget follows them at `y = 0.24`. Preview values are stable (`L CPS 8`,
`R CPS 4`, and `CPS 8 | 4`), while live values use the same rolling
one-second physical-click tracker as Keystrokes. Values above 99 render as `99+` so text never truncates.

## Data Boundary

Profile schema v4 stores `left_cps`, `right_cps`, and `combined_cps` widget records. Readers recursively
fill any missing feature, widget, layout, style, or content fields from current defaults, so profiles
saved before `combined_cps` remain valid. There is no
separate CPS configuration object because all requested behavior is already represented by the common
widget layout and style fields. Counts remain transient: they are not saved, logged, or transmitted.

## Manual Acceptance

- Enable only left CPS and verify right CPS and Keystrokes remain hidden.
- Enable only right CPS and verify left CPS and Keystrokes remain hidden.
- Enable combined CPS and verify it renders `CPS left | right` using the same counts.
- Click both buttons at different rates and verify the two values update independently.
- Move, scale, recolor, and reset each widget without changing the other.
- Enable embedded Keystrokes CPS at the same time and verify all views share the same counts.
- Open a screen, lose focus, disconnect, and reconnect; UI clicks must not enter either count.
