# Settings And Input Solution

## Goal

Extend the existing settings center so players can manage Profiles and configure every in-scope feature without hard-coded input handling. Minecraft remains the sole owner of key persistence and conflict presentation.

## Non-Goals

- No HUD editor or right-Shift reservation.
- No custom key chord system or per-Profile key bindings.
- No automatic conflict resolution or forced unbinding.
- No replacement for Minecraft's Controls screen.

## Current Repository State

`ClientSettingsKeyMappings` registers Home to open settings and `I` to toggle force sprint. `ClientSettingsEvents` consumes those actions, adds an EaseCation Settings button to the pause menu, and opens a Cloth Config screen. `ClientSettingsScreen` directly edits one NeoForge `ModConfigSpec` containing force sprint and server-window permissions.

The existing entry points remain recognizable. The internal source for force sprint moves to the active Profile, while server-window permissions stay global.

## AxolotlClient Reference Analysis

Reference commit: `b1d066585626e4a7adf9f4ddbeb31cbf1ec3245f`

Reference files:

- `common/src/main/java/io/github/axolotlclient/bridge/key/AxoKeybinding.java`
- `common/src/main/java/io/github/axolotlclient/AxolotlClientConfigCommon.java`
- `common/src/main/java/io/github/axolotlclient/modules/zoom/Zoom.java`
- `versions/1.21/src/main/java/io/github/axolotlclient/config/screen/ProfilesScreen.java`
- `versions/1.21/src/main/java/io/github/axolotlclient/mixin/TitleScreenMixin.java`

Useful behavior:

- Feature actions are represented by registered, user-rebindable inputs.
- Profile management has a dedicated list rather than being hidden inside raw config files.
- Zoom supports a conventional default key plus optional unbound actions.

Rejected implementation details:

- AxolotlClient forcibly unbinds a vanilla key when it conflicts with Zoom; this project never mutates another binding.
- Fabric key abstractions and custom configuration UI classes are not reused.
- Profile objects are not edited directly before validation and persistence.
- Upstream labels, layout, styles, and assets are not copied.

## Global Key Mappings

Register actions through NeoForge's key-mapping event:

| Action | Default | Scope |
|---|---:|---|
| Open EaseCation Settings | Right Shift | Global setting |
| Toggle Force Sprint | I | Writes active Profile |
| Zoom | C | Global binding, Profile behavior |
| Cycle To Next Profile | Unbound | Global binding |
| Toggle Fullbright | Unbound | Global binding, Profile mode |

Right Shift opens the complete EaseCation client settings screen. The HUD editor remains an explicit entry inside the HUD category rather than replacing this global navigation shortcut. Every default can be changed in Minecraft Controls and is stored in `options.txt`.

The mod does not inspect other bindings to choose a different key at runtime. Minecraft's standard conflict display is the authoritative warning surface. A conflict does not delete, rewrite, or disable either mapping.

## Input Dispatch

`ClientInputDispatcher` consumes registered actions on `ClientTickEvent.Post` and calls narrow application services:

- Open settings only when it is safe to replace the current screen. The existing pause-screen shortcut remains supported.
- Toggle force sprint by creating and saving a modified active-Profile draft, then apply the new snapshot.
- Zoom delegates press state to `ZoomController`; it does not toggle a config value every tick.
- Cycle Profile selects the next valid ID in index order and reports failures through normal client messages.
- Toggle Fullbright switches between `OFF` and the last non-off mode without changing the configured mode value.

Feature adapters do not call Minecraft key APIs directly. This keeps action tests independent from rendering.

## Settings Screen Structure

Continue using Cloth Config for the primary settings screen:

```text
EaseCation Settings
├── Profile
│   ├── Current Profile (read-only summary)
│   └── Manage Profiles (opens ProfileManagementScreen)
├── Movement
│   └── Force Sprint
├── Rendering
│   ├── Block Outline
│   ├── Low Fire
│   ├── Fullbright
│   ├── Time Changer
│   ├── Zoom
│   └── Hit Color
└── Server Permissions
    ├── Allow Window Title
    └── Allow Window Frame
```

Feature branches contribute entries through explicit builder methods, not reflection. Until a feature branch lands, its category is absent rather than showing a nonfunctional toggle.

The screen edits an immutable Profile draft. `Done` validates and persists the draft through `ProfileManager`; `Cancel` discards it. Global server permissions use their existing NeoForge config save path. If Profile saving fails, the screen stays open and shows a clear error without applying partial runtime state.

## Profile Management Screen

Use a small vanilla list screen so CRUD behavior is explicit and testable:

- Rows show display name and current state.
- Actions: select, rename, duplicate, and delete.
- A footer action creates a Profile.
- Delete requires confirmation and is disabled for Default.
- Selecting another Profile closes and recreates the parent settings screen so no stale draft remains.
- Rename is committed only after validation succeeds.
- A failed operation leaves the list and active Profile unchanged.

Import/export controls are deliberately absent.

## Force Sprint Integration

`KeyboardInputMixin` reads `ActiveProfileSnapshot.forceSprint().enabled()` instead of the legacy TOML value. The toggle shortcut updates the Profile through the same validated persistence path as the screen. Disabling force sprint immediately calls `player.setSprinting(false)` as the current code does.

The on-screen force-sprint indicator remains in the deferred HUD list.

## Error Presentation

- Recoverable storage warnings appear as translated text in the settings/Profile screen.
- Action failures outside a screen use the vanilla client message area, not a custom HUD.
- Unknown Profile or save failures do not switch active state.
- Input dispatch ignores actions when no player/world is available unless the action is explicitly screen-only.

## Expected Code Areas

- Extend `client/ClientSettingsKeyMappings` with global mappings.
- Split `ClientSettingsEvents` into screen hooks and `client/input/ClientInputDispatcher`.
- Expand `client/ClientSettingsScreen` into explicit category builders.
- Add `client/ProfileManagementScreen` and a testable screen-model/controller layer.
- Route `KeyboardInputMixin` through the active Profile snapshot.
- Add translated labels written specifically for this project.

## Automated Tests

- Each action invokes exactly one application-service operation per consumed click.
- Force-sprint toggle persists the active Profile and resets sprint when disabled.
- Cycling follows stored order, skips invalid Profiles, and wraps once.
- Screen drafts save on Done and remain unchanged on Cancel or failure.
- CRUD controller validation matches Profile-core rules.
- Duplicate KeyMapping assignments are left untouched and detectable by vanilla; no unbind API is called.
- Input actions safely no-op without a player or world.

## Manual Acceptance

- Rebind Home, `I`, and `C`; restart and confirm Minecraft preserves them.
- Deliberately assign conflicting keys and confirm vanilla shows the conflict without either mapping changing.
- Open settings from gameplay and pause menu.
- Exercise every Profile operation and return to a correctly refreshed settings screen.
- Confirm global window permissions do not change when switching Profiles.
- Confirm force sprint changes immediately and persists per Profile.

## Completion Criteria

- No action depends on a hard-coded key check outside registered `KeyMapping` state.
- Global and Profile-scoped settings are visually and technically separated.
- Profile changes are validated before runtime application.
- No HUD entry point or right-Shift behavior is introduced.
