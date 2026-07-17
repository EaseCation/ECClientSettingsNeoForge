# Time Changer Solution

## Goal

Allow a Profile to select a fixed client-visible time while continuously preserving enough server-time state to return accurately to server-following behavior.

## Non-Goals

- No outbound packet, server command, world rule change, weather change, or simulation-time change.
- No cross-server time carryover.
- No animation or gradual time sweep in this phase.

## AxolotlClient Reference Analysis

Reference commit: `b1d066585626e4a7adf9f4ddbeb31cbf1ec3245f`

Reference files:

- `common/.../AxolotlClientConfigCommon.java`
- `versions/1.21/.../mixin/ClientWorldMixin.java`

Useful behavior:

- Replace only the time stored in the client world.
- Provide common day/noon/night/midnight presets plus a custom value.

Rejected implementation details:

- Replacing incoming time without tracking the server baseline makes disable/restore dependent on a future packet.
- A single enabled boolean does not model explicit presets or server-following mode clearly.
- Fabric/Yarn method targets and upstream constants are not copied.

## NeoForge 1.21.8 Evidence

`ClientPacketListener.handleSetTime(ClientboundSetTimePacket)` receives `gameTime`, `dayTime`, and `tickDayTime`, then calls public `ClientLevel.setTimeFromServer(long, long, boolean)`. ClientLevel increments game time each client tick and day time only when `tickDayTime` is true.

NeoForge 21.8.52 has no client time-replacement event. One required argument-modification Mixin at the `setTimeFromServer` call is the narrowest point that preserves the original server packet as evidence before changing the client copy.

## Configuration

```json
"timeChanger": {
  "mode": "FOLLOW_SERVER",
  "customTime": 6000
}
```

Modes and fixed values:

| Mode | Time |
|---|---:|
| `FOLLOW_SERVER` | Server value |
| `DAY` | 1000 |
| `NOON` | 6000 |
| `SUNSET` | 12000 |
| `MIDNIGHT` | 18000 |
| `CUSTOM` | `0..23999` |

Custom time is validated even when another mode is active so every persisted Profile is switch-ready.

## Controller State

`TimeChangerController` holds transient state for the current world only:

- Last server `gameTime`.
- Last server `dayTime`.
- Last server `tickDayTime`.
- Client tick counter when the packet arrived.
- Current world identity/generation.

This state is never persisted in a Profile.

## Packet And Apply Flow

On each server time packet:

1. Capture all original packet values before modification.
2. Reset the baseline receipt tick.
3. In `FOLLOW_SERVER`, pass all values unchanged.
4. In a fixed mode, pass original game time, configured fixed day time, and `tickDayTime = false` to the client world.

While fixed, the controller advances its private expected server baseline using elapsed client ticks when the captured server flag permits day-time ticking. The visible client time remains fixed.

When switching to another fixed mode, call `ClientLevel.setTimeFromServer` with current client game time, the new fixed day time, and ticking disabled. This direct local call does not replace the captured server baseline.

When switching to `FOLLOW_SERVER`, calculate:

```text
restoredGameTime = capturedGameTime + elapsedClientTicks
restoredDayTime  = capturedDayTime + (tickDayTime ? elapsedClientTicks : 0)
```

Then call `setTimeFromServer` with the captured ticking flag. The values use `long` arithmetic; only the fixed visual setting is normalized to the day cycle.

## Lifecycle And Failure Behavior

- Login/world creation initializes a new generation and waits for/captures the first server time packet.
- Logout clears all baseline state.
- Player clone does not carry a baseline across a different client level.
- If fixed mode is enabled before a packet is available, seed the baseline from current client world values and record that restoration is approximate until a real packet arrives.
- A Profile switch resets visible time through the controller before the new mode is applied.
- A failed restore logs the condition and leaves the mode `FOLLOW_SERVER`; the next real packet repairs the client value.

No packet is sent and no server-owned object is mutated.

## Mixin Design

Add one `ClientPacketListener` Mixin around the `ClientLevel.setTimeFromServer(JJZ)V` arguments inside `handleSetTime`, with `require = 1`.

The adapter passes packet data to the controller and receives an immutable triple for the client call. It contains no preset or elapsed-time logic.

## Expected Code Areas

- `feature/timechanger/TimeChangerMode`
- `feature/timechanger/TimeChangerController`
- `feature/timechanger/ClientTimeValues`
- `mixin/client/ClientPacketListenerMixin`
- Client lifecycle event registration
- Settings entries, translations, and tests

## Automated Tests

- Every preset and custom-time validation.
- Packet capture with ticking enabled and disabled.
- Elapsed game/day restoration arithmetic.
- Fixed-to-fixed, fixed-to-follow, and Profile-switch transitions.
- Packet arrival while fixed replaces the baseline but not visible time.
- World-generation and logout state isolation.
- Long wrap behavior follows Java/Minecraft long semantics without narrowing.
- Structural test verifies the exact 1.21.8 `setTimeFromServer` call and `require = 1`.

## Manual Acceptance

- Exercise all presets and custom endpoints `0` and `23999`.
- Use a server with daylight cycle both enabled and disabled.
- Stay fixed for several minutes, return to server mode, and compare another client/server clock.
- Receive repeated server time updates while fixed.
- Switch Profiles, dimensions, worlds, multiplayer servers, and singleplayer.
- Confirm no command, packet, server time, crop behavior, mob behavior, or redstone timing changes.

## Completion Criteria

- Fixed mode changes only client-visible day time.
- Returning to server mode does not wait for another packet and uses a tracked baseline.
- Baseline state never leaks across client worlds.
- One required 1.21.8 Mixin is the only packet interception.
