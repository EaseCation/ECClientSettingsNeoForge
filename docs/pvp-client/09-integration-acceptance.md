# Integration And Acceptance Solution

## Goal

Prove that the approved Profile, settings/input, and six visual-feature branches work together on Minecraft 1.21.8 without changing deferred HUD/server scope or losing vanilla restoration behavior.

## Non-Goals

- No production deployment, remote push, `master` merge, parent Gitlink commit, or modpack publication.
- No claim that a build proves visual runtime behavior.
- No opportunistic fixes in unrelated submodules.

## Current Repository State

`NeoForgeWorkspace` includes `ECClientSettingsNeoForge` as a composite build and packages its jar through the root modpack module registry. The original parent worktree already has unrelated user changes, so all integration work runs under `/home/ec/workspace/worktrees/ec-pvp-client/NeoForgeWorkspace` and never stages the original worktree.

## AxolotlClient Reference Analysis

Reference commit: `b1d066585626e4a7adf9f4ddbeb31cbf1ec3245f`.

Repository-level references: `README.md` and `LICENSE`. Feature source references and understood behaviors are enumerated in documents 01 through 08. Integration adopts no upstream build, packaging, resource, or test structure. The relevant rejected behavior is bundling upstream implementation/assets; the acceptance scan treats any such reuse outside these documents as a failure.

## Dependency Order

```text
profile core
  -> settings/input
  -> block outline
  -> low fire
  -> fullbright
  -> time changer
  -> zoom
  -> hit color
  -> integration acceptance
```

Each task branch synchronizes the latest local `feature/pvp-client`, adds implementation/tests matching its approved document, passes its focused checks, and is merged back with `--no-ff`. A later branch never starts from unverified code.

## Per-Branch Gate

Before a task branch can merge to the integration branch:

1. The branch solution document matches the implementation.
2. `git diff --check` passes.
3. Focused JUnit tests pass.
4. Full `./gradlew test` passes in `ECClientSettingsNeoForge`.
5. `./gradlew build` produces the plain mod jar.
6. Mixin/resource/static checks for that feature pass.
7. The branch has no unrelated file changes.
8. Any unperformed game check is explicitly carried into the final matrix.

Recommended command surface:

```bash
./gradlew test --no-daemon --console plain --rerun-tasks
./gradlew build --no-daemon --console plain --rerun-tasks
```

The exact task names are rechecked against the live Gradle project before execution.

## Automated Integration Coverage

### Profile And Global Separation

- Profile switch applies force sprint and every visual setting together.
- Key mappings and server-window permissions remain unchanged across switches.
- Invalid target Profile rolls back all participants.
- Default recovery yields a complete setting set for every feature.

### Lifecycle Composition

- Profile switch resets Zoom, restores Time Changer server baseline, and restores/reapplies Hit Color ownership in deterministic order.
- Logout/world change clears Time Changer and Zoom transient state.
- Disabling every feature produces identity behavior in adapters.
- Feature failures do not prevent later vanilla restoration.

### Mixin Integrity

- Mixin JSON contains only approved low-fire, fullbright, time-changer, and overlay-texture entries plus existing project Mixins.
- Every required injection targets the mapped Minecraft 1.21.8 descriptor and declares `require = 1`.
- Block Outline and Zoom remain event-only.

### Forbidden Reuse Scan

Scan `src/` and the built jar, excluding these reference documents, for:

- `io.github.axolotlclient` or AxolotlClient package/resource names.
- Image, icon, video, audio, model, or shader assets introduced by this feature set.
- Copied upstream copyright headers or language strings.

Any deliberate textual reference must exist only in `docs/pvp-client/` and name the pinned upstream commit.

## Manual Game Acceptance Matrix

Record `PASS`, `FAIL`, or `NOT RUN` with date, build hash, environment, and evidence notes.

| Area | Required scenarios |
|---|---|
| Profile | CRUD, restart persistence, per-Profile isolation, corrupt index/Profile recovery, failed-save rollback |
| Global settings | Key rebind/restart, deliberate conflict, server-window permissions unchanged across Profiles |
| Force sprint | Per-Profile enable/disable, shortcut, immediate sprint reset |
| Block Outline | Full/partial/translucent shapes, alpha values, wall/reach behavior, disable comparison |
| Low Fire | Offsets, third-person, item activation while burning, screen opening, Profile switch |
| Fullbright | Cave/night/Nether/End, real Night Vision, Darkness, vanilla brightness persistence |
| Time Changer | All presets, custom endpoints, daylight-cycle on/off, long fixed interval, dimensions/servers |
| Zoom | Hold/toggle, speeds, scroll bounds, sensitivity, smooth camera, death/focus/screens/logout |
| Hit Color | Player/mob/armor, damage by another player, empty/failed attack, wall, invisibility, restore |
| Combinations | Zoom+Fullbright, Low Fire+Zoom, Time Changer+Fullbright, rapid Profile switching |

Build-only or unit-only evidence must be marked `NOT RUN` for the corresponding visual row.

## Root Workspace Verification

After the submodule integration branch is clean:

1. Initialize the remaining submodules inside `/home/ec/workspace/worktrees/ec-pvp-client/NeoForgeWorkspace`.
2. Use Java 21.
3. Run the live root aggregate build after inspecting available tasks.
4. Confirm the root build consumes the task-worktree `ECClientSettingsNeoForge` source, not a stale jar.
5. Locate the final plain jar and calculate SHA-256.
6. Do not copy it into the original dirty worktree or commit the parent Gitlink.

Expected submodule artifact location:

```text
ECClientSettingsNeoForge/build/libs/ecclientsettings-1.0.0.jar
```

The actual artifact name is taken from Gradle output and reported if it differs.

## Git Evidence

Final reporting includes:

- `git log --graph --decorate --oneline` for `feature/pvp-client` and task branches.
- Each task's document, feature, and test commits.
- Ahead/behind state relative to `origin/master` without pushing.
- Clean `ECClientSettingsNeoForge` integration worktree.
- Parent task worktree status showing only the expected local submodule Gitlink difference, if any.
- Original `/home/ec/workspace/NeoForgeWorkspace` dirty state unchanged from the baseline captured before work.

## Rollback

Each feature is Profile-disabled by default except existing force sprint and the available Zoom capability. Runtime adapters must have identity paths. Git rollback is performed by reverting a task merge on the local integration branch; unrelated feature histories remain intact.

If a required 1.21.8 hook cannot pass startup and runtime validation, its task branch is not merged. The acceptance requirement is not weakened silently.

## Completion Report

The final report separates:

- Implemented and automated-test verified.
- Game-runtime verified with direct evidence.
- Built but not game-runtime verified.
- Deferred by the approved HUD/TPS boundary.
- Blocked with attempted approaches and evidence.

It also lists commands, test counts, artifact path/hash, branch/commit map, forbidden-resource scan, and remaining manual steps.

## Completion Criteria

- All approved task branches are merged locally in dependency order.
- Submodule tests/build and root workspace build pass against current source.
- No forbidden upstream resource/code reuse is present.
- Every manual acceptance row has an honest evidence state.
- Remote and production state remain unchanged.
