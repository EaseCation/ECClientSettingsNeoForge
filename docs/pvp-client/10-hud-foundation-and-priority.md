# HUD Foundation And Priority

## Goal

Start the deferred HUD phase with a reusable local HUD foundation and the four widgets with the strongest combination of player value and implementation confidence:

1. FPS
2. Ping
3. Armor durability
4. Active potion effects
5. Keystrokes with optional per-button CPS

This phase also adds Profile-backed normalized layout data and an editor for moving, scaling, and styling widgets. It does not add TPS, Crosshair, a standalone CPS card, real-time clock, or game-mode-specific overlays.

## Why This Slice Comes First

Public download counts are imperfect because they are cumulative, cross-version, and not unique-player counts. They are still useful as directional evidence when combined with community discussion and implementation risk.

| Need | Public signal observed on 2026-07-22 | Decision |
| --- | --- | --- |
| Inventory, armor, and potion status | [Inventory HUD+](https://www.curseforge.com/minecraft/mc-mods/inventory-hud-forge) had about 84.49 million CurseForge downloads | Build Armor and Potion on the first HUD foundation |
| Armor durability | [uku's Armor HUD](https://modrinth.com/mod/ukus-armor-hud) had about 6.86 million Modrinth downloads | Show per-slot durability percentage and a readable durability bar |
| Potion duration | [Status Effect Timer](https://modrinth.com/mod/statuseffecttimer) had about 2.90 million Modrinth downloads | Show name, amplifier, duration, icon, and expiry warning |
| Ping | [Better Ping Display](https://modrinth.com/mod/better-ping-display-fabric) had about 12.16 million Modrinth downloads | Use the server-synchronized player latency, with `--` when unavailable |
| FPS | [FPS Display](https://modrinth.com/mod/fpsdisplay) had about 3.70 million Modrinth downloads | Use the client's measured FPS as the simplest end-to-end widget |

Competitive Minecraft discussion also calls out armor durability, FPS, and ping as useful client information, while emphasizing the value of one settings menu:

- <https://www.reddit.com/r/CompetitiveMinecraft/comments/1k3xu9x/do_pvp_clients_have_an_advantage_over_installing/mo7z6su/>
- <https://www.reddit.com/r/CompetitiveMinecraft/comments/1k3xu9x/do_pvp_clients_have_an_advantage_over_installing/mo811bt/>
- <https://www.reddit.com/r/CompetitiveMinecraft/comments/1f000rs/mods_for_pvp_120/ljqblaj/>

The resulting development order is:

```text
Profile schema v3
  -> normalized HUD layout and renderer
  -> editor and preview
  -> FPS and Ping
  -> Armor and Potion
  -> Keystrokes and optional embedded CPS
  -> Crosshair
  -> server-authoritative TPS and mode HUDs
```

## Reference And License Boundary

The matching AxolotlClient reference is tag `3.1.5`, commit `3180a519cdff83855cf82d34cac22eca766d0e3c`. Its source directory is named `1.21.7`, but its build declares Minecraft 1.21.8.

The repository root uses LGPL-3.0-or-later, but the HUD framework and widget files explicitly state that they are based on KronHUD and identify GPL-3.0. The current EaseCation project declares MIT. Therefore this implementation may study visible behavior, configuration ideas, and known defects, but it must not copy the AxolotlClient or KronHUD HUD source, class structure, assets, or text.

The implementation uses the existing Profile system, Cloth Config, Mojang APIs, and NeoForge GUI-layer events. No third-party HUD source or asset is included.

## Known Reference Pitfalls

The new implementation must explicitly avoid issues already reported against the reference client:

- HUD overlap at different resolutions: <https://codeberg.org/AxolotlClient/AxolotlClient-mod/issues/162>
- Scale changes shifting widgets or leaving stale content: <https://codeberg.org/AxolotlClient/AxolotlClient-mod/issues/194>
- Armor layouts needing compact spacing: <https://codeberg.org/AxolotlClient/AxolotlClient-mod/issues/160>
- Crosshair state affecting unrelated HUD backgrounds: <https://codeberg.org/AxolotlClient/AxolotlClient-mod/issues/74>
- Crosshair background flicker: <https://codeberg.org/AxolotlClient/AxolotlClient-mod/issues/82>
- Ping implementations presenting missing data as `0 ms`: <https://github.com/vladmarica/better-ping-display-fabric/issues/51>

Crosshair is deliberately deferred until the ordinary GUI-layer renderer is stable. Ping must never substitute zero for missing data.

## Profile Schema V3

HUD state belongs to a Profile because the planning document requires different layouts for different PvP modes. The current pre-release schema v3 stores both HUD layout and reusable per-widget visual style below `features.hud`.

Each implemented widget stores:

- `enabled`
- `normalizedX`
- `normalizedY`
- `scale`
- background, border, padding, and text/RGB style

Normalized coordinates are in `[0, 1]`. Scale is in `[0.5, 3.0]`. The renderer maps normalized coordinates into the currently available screen area after accounting for the scaled widget size and a fixed 4 GUI px safe inset. This keeps a widget visible with consistent edge spacing when resolution or GUI scale changes.

Schema rules:

- Writers always produce strict schema v3.
- Readers accept only strict schema v3; there is no v1/v2 migration because the feature has not shipped.
- Every HUD widget and every style field is required.
- Unknown fields and unsupported schema versions fail closed.

## Rendering Architecture

The HUD is registered through `RegisterGuiLayersEvent`, without a HUD Mixin. It renders immediately below the vanilla chat layer so chat remains readable. A small widget registry owns the four implementations and exposes the same measurement and rendering path to the game overlay and editor preview.

Render-path rules:

- Read the immutable active Profile snapshot; do not perform file I/O per frame.
- Do not render while vanilla `hideGui` is active.
- Do not render world-dependent widgets without a player and level.
- Push and pop the GUI pose for every scaled widget.
- Clamp every calculated rectangle to the current GUI dimensions.
- Use stable placeholder data only in the editor, never in live gameplay.
- Suppress the live Profile HUD while the layout editor draws its draft previews.

## Editor Behavior

Right Shift and the pause-menu button both open the complete settings center. The HUD category contains the visual editor entry, preserving one predictable global navigation target while keeping layout editing discoverable.

The editor supports:

- left-button drag;
- wheel scaling for the hovered widget;
- snapping to screen edges, horizontal and vertical center lines, and other widget edges;
- visible snap guides;
- Alt to temporarily bypass snapping;
- arrow-key movement and `+`/`-` scaling for the initially selected widget;
- reset for one widget or the complete layout;
- enable/disable and per-widget style editing from the standalone editor;
- preview content for all four widgets;
- live style preview for color, transparency, border, padding, shadow, and RGB animation;
- draft-only edits until the parent Cloth Config screen is saved.

Closing or cancelling the parent settings screen must discard HUD layout edits. Clicking Done on the parent screen saves them atomically with the rest of the active Profile.

## Widget Requirements

### FPS

- Display the current client FPS.
- Use a stable preview value in the editor.
- Do not sample or write any server state.

### Ping

- Read the local player's current `PlayerInfo` latency.
- Display `--` when no connection, player entry, or multiplayer latency exists.
- Never turn unavailable data into `0 ms`.
- Use a stable preview value in the editor.

### Armor

- Show helmet, chestplate, leggings, and boots in a compact order.
- Update directly from the current player equipment every frame.
- Show item icon, durability percentage, and a durability bar.
- Use green, yellow, and red states so low durability is visible without reading text.
- Hide the live widget when every armor slot is empty.
- Use representative armor only in editor preview.

Low-durability notifications, main-hand/off-hand slots, orientation, and numeric display modes are follow-up settings. Notifications require a deduplicated state machine and must not be added as a per-frame chat message.

### Potion

- Show effect icon, localized name, amplifier, and remaining time.
- Use the player's effective active-effect collection, so removal is visible on the next frame.
- Display an infinity marker for infinite duration.
- Use a warning color near expiry.
- Hide the live widget when there are no active effects.
- Use stable examples only in editor preview.

Horizontal layout, icon-only mode, ordering, and custom colors remain follow-up settings.

## Deferred Work

- A standalone CPS card remains deferred; Keystrokes already provides optional left/right CPS inside the mouse buttons.
- Crosshair needs isolated render-state and compatibility testing before it can replace or augment vanilla rendering.
- TPS requires an EaseCation server payload or an explicitly labeled estimate. Until then it would be misleading.
- Bedwars and other mode HUDs require a server-owned state contract, not Hypixel chat or scoreboard parsing.
- AppleSkin-scale interest suggests saturation information is worth a separate player vote, but it is outside the supplied plan.

## Automated Acceptance

- Schema v3 HUD layout and style data round-trip exactly.
- Invalid normalized coordinates and scale values are rejected.
- Layout conversion keeps scaled bounds on-screen at small and large GUI dimensions.
- Snap calculations are deterministic at edges and center lines.
- Missing Ping data formats as `--`, not `0 ms`.
- Existing Profile, Zoom, rendering, input, and window tests remain green.
- The composite workspace build resolves NeteaseBridge substitutions and produces the mod jar.

## Manual Acceptance

- Enable each widget, save, reconnect, and restart the client; visibility and layout persist.
- Move a widget to every corner, change GUI scale and window size, and verify it remains visible.
- Scale each widget at its minimum and maximum and verify the cursor stays aligned with the selected widget.
- Switch Profiles and verify enabled state, position, and scale change immediately.
- Disconnect and open a single-player world; Ping shows unavailable rather than a plausible fake value.
- Equip, damage, repair, and remove every armor slot; the HUD updates without reopening settings.
- Add, upgrade, expire, and clear potion effects; the HUD updates without duplicate rows.
- Toggle F1 and verify all custom HUD widgets follow vanilla HUD visibility.
- Check the overlay together with ModUI, chat, scoreboard, subtitles, and all existing visual features.
- Rebind movement/jump controls and verify Keystrokes labels and press state follow the active mappings.
- Toggle movement, jump, mouse, and CPS groups independently; verify key size, gap, corner radius, pressed colors, and transition duration in the live style preview.
