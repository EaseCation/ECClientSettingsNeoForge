# Keystrokes HUD

## User Scenario And Priority

The supplied planning document identifies Keystrokes as a streaming and recording HUD. The core task
is not competitive automation; it is making the player's current movement, jump, and mouse actions
legible to an audience. The adjacent CPS request shares the same mouse-input sampling concern, so CPS
remains available as an optional second line inside the two mouse buttons. Independent left/right CPS
widgets are also available for players who want a compact text-only layout without Keystrokes.

The implementation follows this priority:

1. Never consume, cancel, rewrite, or synthesize input.
2. Follow the player's current Minecraft movement and jump bindings instead of hard-coding WASD.
3. Make idle and pressed state distinguishable without relying on color alone.
4. Keep the common W/ASD, mouse, and space-bar geometry compact and directly movable.
5. Let streamers match overlays and branding without making high-salience animation the default.

## Reference Behavior

The supplied `QQ20260717-012913.mp4` shows the mature reference layout: one centered forward key, an
adjacent three-key movement row, two wider mouse buttons, and a full-width jump bar. It also exposes
separate idle/held text, background, and outline colors plus a short animation duration. Those visible
behaviors inform this design; no reference source, class structure, labels, or assets are copied.

The adopted default is smaller and quieter than the customized reference recording:

- 20 GUI px movement keys;
- 2 GUI px gap;
- square corners by default, with an optional 0-6 GUI px radius;
- 1 GUI px per-key border;
- translucent near-black idle background with an opaque white outline;
- opaque white pressed background with dark pressed text;
- 100 ms press/release transition;
- CPS disabled until the player requests it;
- no outer panel by default.
- vanilla text shadow enabled by default.

## Profile Model

`HudWidgetId.KEYSTROKES` owns the standard enabled, normalized position, scale, and shared
`HudWidgetStyle`. Its content-specific `KeystrokesSettings` stores:

- movement, jump, mouse, and CPS visibility;
- key size, gap, corner radius, and per-key border width;
- idle and pressed ARGB background colors;
- idle and pressed ARGB border colors;
- fixed pressed ARGB text color;
- press transition duration from 0 to 500 ms.

The shared style remains responsible for the optional outer panel, padding, text shadow, and idle text
mode. This means unpressed labels support the same fixed, rainbow wave, and low-frequency whole-text
switch modes as every other HUD. Pressed text deliberately uses one fixed accent so a press remains
immediately recognizable even when the idle labels are animated.

The project is still pre-release, so strict schema v4 requires the Keystrokes object, its content fields,
and the independent left/right CPS widget records. There is no v1/v2/v3 migration path.

## Input And Privacy Boundary

- Movement and jump use vanilla `KeyMapping.isDown()`, so custom bindings are authoritative.
- Mouse press state is read from the local GLFW window only while gameplay is focused.
- Left/right CPS counts physical press events in a rolling one-second window.
- Click events are observed through NeoForge `InputEvent.MouseButton.Pre` and are never cancelled.
- Screens, lost focus, missing world/player state, logout, and client stop cannot add clicks.
- Counts and transition state are transient memory only; they are not sent, logged, or persisted.

## Interaction Design

Keystrokes participates in the existing visual HUD editor, including selection, dragging, snapping,
scroll scaling, keyboard nudging, reset, and Profile draft semantics. The ordinary enable toggle stays
in the HUD category. Selecting Keystrokes and opening its style page exposes two categories:

- `HUD Style`: outer panel, idle text fixed/RGB mode, shadow, padding, and a live preview;
- `Keystrokes HUD`: visible groups, geometry, pressed/idle key colors, border width, and transition.

The preview renders both a pressed forward key and pressed left mouse button while the remaining keys
stay idle. Players can therefore compare both states and alpha values without repeatedly leaving the
screen. A checkerboard canvas makes transparency visible, and oversized layouts are scaled only inside
the preview; the saved in-game scale remains unchanged.

## Manual Acceptance

- Enable Keystrokes, save, restart, and verify Profile persistence.
- Hold and combine movement keys, jump, LMB, and RMB; verify no input is delayed or swallowed.
- Rebind forward and jump, reopen the world, and verify labels and state follow the new bindings.
- Enable CPS and compare slow single clicks, alternating buttons, held buttons, and clicks older than one second.
- Open inventory/chat, unfocus the window, disconnect, and reconnect; verify CPS does not count UI clicks or remain stuck.
- Compare 0, 100, and 500 ms transitions; rapid clicks must remain visible without long trailing state.
- Test every idle/pressed ARGB color, border width, corner radius, key size, gap, outer background, and idle RGB mode.
- Hide each content group independently, including hiding all groups, then verify the editor remains recoverable.
- Drag and scale the smallest and largest layouts at 320 x 240 and ultrawide GUI viewports.
