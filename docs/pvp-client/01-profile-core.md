# Profile Core Solution

## Goal

Provide durable, immediately switchable Profiles for force sprint and the six non-HUD visual features. Profile storage must survive malformed files and interrupted writes without silently replacing valid player data.

## Non-Goals

- Key bindings and server-window permissions remain global.
- HUD layout or HUD feature fields are not modeled.
- Profile import/export and server-based automatic switching are deferred.
- The storage layer is not a general-purpose plugin configuration framework.

## Current Repository State

`ClientSettingsConfig` currently stores force sprint and server-window permissions in one NeoForge client `ModConfigSpec`. `ClientSettingsScreen` binds directly to those values. There is no profile identity, versioned JSON storage, transactional switch, or recovery path.

The existing window-title and frame permissions stay in the NeoForge TOML because they are trust decisions. The existing force-sprint key is retained as a deprecated migration input but is no longer the runtime source of truth after migration succeeds.

## AxolotlClient Reference Analysis

Reference commit: `b1d066585626e4a7adf9f4ddbeb31cbf1ec3245f`

Reference files:

- `common/.../config/profiles/Profiles.java`
- `common/.../config/profiles/ProfileAware.java`
- `common/.../AxolotlClientCommon.java`
- `versions/1.21/.../config/screen/ProfilesScreen.java`

Useful behavior:

- Stable Profile IDs are separate from editable display names.
- Per-Profile files isolate feature configuration.
- Switching a Profile triggers a coordinated configuration reload.
- Create, duplicate, rename, delete, and select are exposed as explicit operations.

Rejected implementation details:

- Direct synchronous writes to the final file are vulnerable to truncation.
- Loading assumes structurally valid JSON and a resolvable current Profile.
- Profile names have no uniqueness or blank-name validation.
- Directory duplication is not a robust deep-copy transaction.
- UI code mutates Profile objects before persistence succeeds.
- Fabric loader paths, native file dialogs, adapters, and class structure are not reused.

## Storage Layout

```text
config/ecclientsettings/
├── profiles.json
└── profiles/
    ├── default.json
    └── <uuid>.json
```

`profiles.json` contains only index-level state:

```json
{
  "schemaVersion": 1,
  "activeProfileId": "default",
  "profileOrder": ["default", "62bd28b8-35ee-4cf4-a0ea-bd6637fca074"]
}
```

Each Profile file is self-describing:

```json
{
  "schemaVersion": 1,
  "id": "default",
  "name": "Default",
  "features": {
    "forceSprint": {"enabled": true},
    "blockOutline": {"enabled": false, "color": "#CCFFFFFF"},
    "lowFire": {"enabled": false, "verticalOffset": 0.2},
    "fullbright": {"mode": "OFF", "strength": 1.0},
    "timeChanger": {"mode": "FOLLOW_SERVER", "customTime": 6000},
    "zoom": {
      "enabled": true,
      "activation": "HOLD",
      "divisor": 4.0,
      "maxDivisor": 16.0,
      "animationSpeed": 7.5,
      "scrollAdjustment": false,
      "reduceSensitivity": true,
      "smoothCamera": false
    },
    "hitColor": {"enabled": false, "color": "#80FF0000"}
  }
}
```

The exact records are owned by `profile.model`; feature packages may interpret their own record but must not perform file I/O.

## Identity And Validation

- `default` is a reserved stable ID. Its display name may change, but it cannot be deleted.
- New IDs are lowercase canonical UUID strings generated locally.
- Names are trimmed, non-empty, limited to 64 Unicode code points, and unique case-insensitively.
- Colors use `#AARRGGBB` and are normalized to uppercase on save.
- Numeric settings are finite and clamped only when loading an older compatible schema. Interactive edits reject out-of-range values rather than silently changing them.
- An unknown higher `schemaVersion` is never rewritten by an older build.

## Atomic Persistence

`ProfileStore` serializes to a sibling temporary file, flushes the writer, calls `FileChannel.force(true)`, and then uses `Files.move` with `ATOMIC_MOVE` and `REPLACE_EXISTING`. If the filesystem cannot provide an atomic replacement, the operation fails and leaves the existing file untouched. Directory metadata sync is attempted on supported platforms and logged if unavailable.

Multi-file operation order limits recoverable debris:

- Create/duplicate: write the Profile first, then update the index. A failed index update leaves a recoverable orphan.
- Rename/settings save: replace one Profile file atomically.
- Delete: update the index first, then move the removed Profile to a timestamped recovery file before cleanup.
- Switch: fully load and validate the target, apply its snapshot, then persist `activeProfileId`. If index persistence fails, the previous snapshot is reapplied.

All mutations run on the Minecraft client thread. Render paths only read the immutable snapshot.

## Migration

Add a global integer migration marker to the NeoForge client config. When the marker is below Profile schema migration 1:

1. Read the existing `forceSprint` value.
2. Build and atomically persist the Default Profile and index.
3. Activate the persisted Default snapshot.
4. Advance and save the migration marker only after both files succeed.

The deprecated TOML force-sprint key remains readable for downgrade safety but is removed from the settings UI and ignored after migration 1. A failed migration leaves the marker unchanged and retries next launch without overwriting a valid Profile store.

## Runtime Model

`ProfileManager` owns a volatile immutable `ActiveProfileSnapshot` and an explicit ordered list of `ProfileParticipant` instances.

```java
public interface ProfileParticipant<S> {
    void apply(S previous, S current);
    void resetTransientState();
}
```

Switching first validates the target, resets transient state, applies the new snapshot in deterministic order, and then updates the index. A participant failure triggers a best-effort reset and reapplication of the previous snapshot. Participants never call back into storage.

## Recovery

- Invalid index: preserve it as `.broken-<timestamp>`, scan valid Profile files, rebuild deterministic order, and activate Default.
- Invalid non-default Profile: preserve it, remove it from the usable set, and continue.
- Invalid or missing Default: preserve invalid data and generate a new safe Default.
- Duplicate recovered names: append ` (Recovered N)` without changing IDs.
- Missing active Profile: use Default and persist the repaired index.
- Newer schema: show a clear error and keep the file untouched; do not downgrade it.

Recovery events are logged with paths and surfaced in the settings screen. They do not use a custom HUD.

## Expected Code Areas

- `profile/model`: immutable records and enums.
- `profile/store`: JSON codec, atomic writer, validation, recovery.
- `profile/migration`: legacy force-sprint migration.
- `profile/runtime`: manager, snapshot, participant coordination.
- `ClientSettingsConfig`: global permissions plus migration marker and deprecated source value.
- Tests under matching `src/test/java` packages.

## Automated Tests

- Model defaults, validation, normalization, and equality.
- CRUD and stable ordering using JUnit `@TempDir`.
- Atomic replacement keeps the previous file when a write or move fails through an injectable file-operations seam.
- Invalid index, invalid Profile, missing Default, duplicate names, and missing active ID recovery.
- Higher schema rejection without file modification.
- Migration success, failed-write retry, and exactly-once marker advancement.
- Participant application order, rollback, and transient reset.

## Manual Acceptance

- Create, copy, rename, delete, and switch Profiles in the client.
- Restart and confirm active Profile and settings persist.
- Corrupt each storage surface separately and confirm preservation plus safe fallback.
- Switch while visual features are active and confirm no state leaks.

## Completion Criteria

- Profile operations are persisted atomically and never require a game restart.
- Default recovery always yields a usable configuration.
- Existing force-sprint preference migrates without changing security permissions or key bindings.
- No HUD field or future HUD ownership assumption exists in the schema.
