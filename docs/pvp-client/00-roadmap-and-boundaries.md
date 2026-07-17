# PvP Client Roadmap And Boundaries

## Goal

Extend `ECClientSettingsNeoForge` with local, non-HUD PvP quality-of-life features for Minecraft 1.21.8 on NeoForge. The deliverable includes a versioned Profile system, global input/settings integration, and six independently configurable visual features.

## Current Baseline

- `ECClientSettingsNeoForge` base: `origin/master@93c2a7305c0ae614c35634034783ed54f1cd2c66`.
- Parent `NeoForgeWorkspace` base: `origin/master@597dd04859637b755a80df70aef8d826b6386b74`.
- Existing module target: Minecraft 1.21.8, NeoForge 21.8.52, Java 21, Cloth Config 19.0.147.
- Existing behavior: Home opens EaseCation Settings, `I` toggles force sprint, and server-window title/frame permissions are global.

## Fixed Decisions

- Target only Minecraft 1.21.8, NeoForge 21.8.x, and Java 21.
- Extend the existing `ECClientSettingsNeoForge` repository; do not create another client mod.
- Keep server-window permissions and Minecraft `KeyMapping` values global.
- Store force sprint and the six visual feature settings in the active Profile.
- Use `feature/pvp-client` as the local integration branch and merge ordered task branches into it.
- Commit one solution document with each task branch before implementing that task.
- Do not push, merge `master`, update the parent Gitlink, publish a modpack, or operate a service without separate authorization.

## In-Scope Tasks

1. Profile storage, migration, validation, recovery, CRUD, and switching.
2. Settings center and global configurable inputs.
3. Block Outline color and opacity.
4. Low Fire vertical offset.
5. Fullbright with `OFF`, `GAMMA`, and `NIGHT_VISION` modes.
6. Time Changer with server-following, presets, and fixed custom time.
7. Zoom with hold/toggle activation, interpolation, scroll adjustment, and temporary turn scaling.
8. Hit Color driven by vanilla server-synchronized hurt state.
9. Cross-feature integration, build verification, and a manual acceptance matrix.

## Explicitly Deferred TODO List

The following work remains deferred until the HUD owner is chosen. No API or data model for it is added in this project phase.

- Armor, Potion, Crosshair, Ping, TPS, Keystrokes, FPS, CPS, and real-time HUDs.
- Force sprint status HUD.
- Bedwars, Skywars, or other game-mode HUDs.
- HUD dragging, scaling, snapping, previews, and layout persistence.
- Deciding whether future HUDs are server-driven NetEase ModUI or local JE client rendering.
- Defining a server-to-client TPS transport and trust contract.

## AxolotlClient Reference Policy

Behavior may be studied from AxolotlClient `dev@b1d066585626e4a7adf9f4ddbeb31cbf1ec3245f`. Implementation remains independent.

Repository-level references are `README.md` and `LICENSE`; exact feature source paths are pinned in documents 01 through 08.

The project must not copy AxolotlClient source, class structure, Mixin signatures, language text, textures, icons, videos, demonstrations, or other assets. Reference notes identify upstream files and observed behavior so reviewers can audit what was learned and what was deliberately rejected.

## Implementation Order

```text
roadmap
  -> profile core
  -> settings and input
  -> block outline
  -> low fire
  -> fullbright
  -> time changer
  -> zoom
  -> hit color
  -> integration acceptance
```

Block Outline is the first visual slice because it validates Profile application, settings binding, color validation, and NeoForge event registration without requiring a Mixin. Hit Color is last because it modifies a shared vanilla render resource and needs the strongest restoration checks.

## Global Quality Rules

- Prefer a supported NeoForge event over a Mixin.
- Use a narrowly targeted Mixin only when the event surface cannot preserve vanilla behavior safely.
- Keep business logic out of Mixins and event adapters.
- Read immutable active-Profile snapshots during render paths; never perform file I/O per frame.
- Fail closed: invalid or failed feature state restores vanilla behavior.
- Preserve current user changes in the original `NeoForgeWorkspace` worktree.
- Mark game behavior as unverified until it has direct in-client evidence.

## Completion Evidence

Completion requires focused branch history, approved design documents, automated tests, `ECClientSettingsNeoForge` test/build output, a root `NeoForgeWorkspace` build, final artifact hashes, the manual acceptance matrix, a forbidden-resource scan, and clean/aligned task-worktree status. Build success alone does not prove runtime behavior.
