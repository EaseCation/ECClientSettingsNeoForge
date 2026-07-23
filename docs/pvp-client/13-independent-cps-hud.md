# Independent CPS HUD

## User Need

Players who only want click speed should not need to enable the larger Keystrokes layout. Left and
right mouse activity also serve different purposes in PvP, so combining them into one card would make
placement and visual hierarchy less flexible.

## Interaction Model

`LEFT_CPS` and `RIGHT_CPS` are separate standard HUD widgets. Each can be independently:

- enabled or disabled from the HUD category;
- selected, dragged, snapped, scaled, and reset in the visual editor;
- configured with its own background, border, padding, shadow, fixed color, or RGB text style;
- persisted per Profile without changing the Keystrokes content settings.

The default positions form a vertical stack below FPS at the upper-left safe inset. Both widgets are
disabled by default, avoiding an unexpected overlay while still giving the editor a predictable initial
layout. Preview values are stable (`L CPS 8` and `R CPS 4`), while live values use the same rolling
one-second physical-click tracker as Keystrokes. Values above 99 render as `99+` so text never truncates.

## Data Boundary

Strict Profile schema v4 stores `left_cps` and `right_cps` as required widget records. There is no
separate CPS configuration object because all requested behavior is already represented by the common
widget layout and style fields. Counts remain transient: they are not saved, logged, or transmitted.

## Manual Acceptance

- Enable only left CPS and verify right CPS and Keystrokes remain hidden.
- Enable only right CPS and verify left CPS and Keystrokes remain hidden.
- Click both buttons at different rates and verify the two values update independently.
- Move, scale, recolor, and reset each widget without changing the other.
- Enable embedded Keystrokes CPS at the same time and verify all views share the same counts.
- Open a screen, lose focus, disconnect, and reconnect; UI clicks must not enter either count.
