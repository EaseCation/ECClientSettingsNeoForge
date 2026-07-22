# Zoom Solution

## Goal

Provide smooth, configurable visual zoom using supported NeoForge camera/input events while never persisting temporary FOV, sensitivity, or cinematic-camera changes into Minecraft options.

## Non-Goals

- No target tracking, aim assistance, crosshair change, freelook, spyglass behavior, or server communication.
- No forced key unbinding.
- No Mixin when NeoForge exposes the required event surfaces.

## Current Repository State

The module registers no Zoom key, FOV event, mouse-scroll handler, temporary turn scaling, or camera state machine. Existing input dispatch handles settings and force sprint only.

## AxolotlClient Reference Analysis

Reference commit: `b1d066585626e4a7adf9f4ddbeb31cbf1ec3245f`

Reference files:

- `common/src/main/java/io/github/axolotlclient/modules/zoom/Zoom.java`
- `common/src/main/java/io/github/axolotlclient/bridge/key/AxoKeybinding.java`
- `versions/1.21/src/main/java/io/github/axolotlclient/mixin/GameRendererMixin.java`
- `versions/1.21/src/main/java/io/github/axolotlclient/mixin/MouseMixin.java`
- `versions/1.21/src/main/java/io/github/axolotlclient/mixin/TitleScreenMixin.java`

Useful behavior:

- Hold and toggle activation modes.
- Smooth FOV transition, optional scroll adjustment, sensitivity reduction, and cinematic camera.
- Conventional `C` default binding.

Rejected implementation details:

- Upstream mutates and later restores live option values, which can overwrite player changes or restore an unrelated smooth-camera value.
- It forcibly unbinds a conflicting vanilla action.
- Scroll maximum is insufficiently bounded.
- Fabric/Yarn Mixins are unnecessary because NeoForge 1.21.8 provides direct events.

## NeoForge 1.21.8 Evidence

- `ViewportEvent.ComputeFov` exposes the final configurable FOV and a setter.
- `CalculatePlayerTurnEvent` exposes temporary sensitivity and cinematic-camera values before turn calculation.
- `InputEvent.MouseScrollingEvent` is cancellable before vanilla hotbar scrolling.
- `ClientTickEvent.Post` provides deterministic state updates.
- `ClientPlayerNetworkEvent.LoggingOut` and `Clone` provide network/player lifecycle reset points.

The feature uses only these events plus registered `KeyMapping`; no Zoom Mixin is expected.

## Configuration

```json
"zoom": {
  "enabled": true,
  "activation": "HOLD",
  "divisor": 4.0,
  "maxDivisor": 16.0,
  "animationSpeed": 7.5,
  "scrollAdjustment": false,
  "reduceSensitivity": true,
  "smoothCamera": false
}
```

Validation:

- Activation: `HOLD` or `TOGGLE`.
- Divisor: finite `1.0..16.0`.
- Maximum divisor: finite, at least the base divisor, at most `32.0`.
- Animation speed: finite `1.0..10.0`.
- Default key: `C`, stored globally by Minecraft and freely rebindable.

## State Machine

`ZoomController` owns transient state only:

```text
INACTIVE -> ENTERING -> ACTIVE -> EXITING -> INACTIVE
```

It also tracks previous/current/target FOV multipliers, the active divisor, transition progress, and toggle latch.

- Target multiplier is `1 / activeDivisor` while zoomed and `1` while exiting.
- Animation speed maps deterministically to a transition duration from ten ticks at speed 1 to one tick at speed 10.
- Tick updates use smoothstep interpolation; `ComputeFov` interpolates previous/current tick values with event partial tick.
- A divisor change while active starts a new transition from the current multiplier without jumping.

## Input Behavior

- `HOLD`: desired active state follows `KeyMapping.isDown()`.
- `TOGGLE`: each consumed click flips desired active state once.
- Scroll changes active divisor by the sign of vertical delta, clamps it to `1..maxDivisor`, and cancels vanilla scrolling only when Zoom actually consumed the event.
- With a screen open, Zoom exits and scroll is never consumed.

## FOV And Turn Scaling

On `ViewportEvent.ComputeFov`, multiply the event's vanilla FOV by the interpolated multiplier only when Zoom is active/exiting and a player world is renderable. Panorama/menu or unavailable-player contexts are passed through.

On `CalculatePlayerTurnEvent`:

- If sensitivity reduction is enabled, supply `vanillaSensitivity / (activeDivisor * activeDivisor)`.
- If smooth camera is enabled, set only the event's temporary cinematic-camera value.

The controller never reads an option for later restoration and never calls an option setter.

## Mandatory Reset Conditions

Immediately begin/reset to unzoomed state on:

- Feature disable or Profile switch.
- Player death or clone/respawn.
- Logout, world unload, or server switch.
- Window focus loss.
- Opening any screen.
- Missing player, level, or window.

Disconnect and invalid-context resets snap to multiplier 1 to avoid carrying camera state into menus. Normal key release may animate through `EXITING`.

## Expected Code Areas

- `feature/zoom/ZoomActivation`
- `feature/zoom/ZoomState`
- `feature/zoom/ZoomController`
- `feature/zoom/ZoomEvents`
- Global key registration and input dispatch
- Settings entries, translations, and tests

No Mixin or asset is added.

## Automated Tests

- All valid state transitions and repeated input idempotence.
- HOLD and TOGGLE semantics.
- Speed-to-duration mapping and smoothstep endpoint/monotonic behavior.
- Divisor and scroll clamping, including large and horizontal-only deltas.
- Scroll cancellation only after consumption.
- Sensitivity scaling and cinematic-camera event values without option mutation.
- Every mandatory reset condition.
- Profile switch while entering, active, and exiting.
- Static check confirms no Zoom Mixin registration.

## Manual Acceptance

- Test HOLD and TOGGLE at animation speeds 1, 5, and 10.
- Scroll from minimum to maximum and confirm the hotbar does not move only while consumed.
- Compare mouse movement with sensitivity reduction and smooth camera independently.
- Open chat, inventory, pause screen, and settings while active.
- Die, respawn, alt-tab, change dimensions, switch servers, and disconnect while active.
- Rebind `C` to a conflicting action and confirm vanilla displays the conflict without automatic edits.
- Confirm vanilla FOV, sensitivity, and cinematic-camera options retain their original saved values.

## Completion Criteria

- Zoom is fully event-driven with no Mixin or persistent option mutation.
- Every exit path returns FOV multiplier to 1 and stops consuming scroll.
- Input conflicts remain under vanilla player control.
- No targeting or gameplay information is added.
