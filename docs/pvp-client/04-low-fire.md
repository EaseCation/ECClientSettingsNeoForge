# Low Fire Solution

## Goal

Reduce first-person fire-overlay obstruction by moving the vanilla overlay downward. The feature must reuse vanilla fire rendering and leave all gameplay and third-person visuals unchanged.

## Non-Goals

- No replacement fire texture, opacity rewrite, animation change, fire immunity, or damage change.
- No third-person entity flame adjustment.
- No cancellation that would remove fire entirely.

## AxolotlClient Reference Analysis

Reference commit: `b1d066585626e4a7adf9f4ddbeb31cbf1ec3245f`

Reference files:

- `common/.../AxolotlClientConfigCommon.java`
- `versions/1.21/.../mixin/InGameOverlayRendererMixin.java`

Useful behavior:

- Move the existing first-person overlay rather than replace its texture.
- Do not touch entity fire state or server data.

Rejected implementation details:

- The upstream fixed offset is not configurable.
- Fabric/Yarn class and method targets do not match Minecraft 1.21.8.
- Upstream injection structure is not copied.

## NeoForge 1.21.8 Evidence

Minecraft 1.21.8 renders first-person fire through the private static `ScreenEffectRenderer.renderFire(PoseStack, MultiBufferSource)` call inside `renderScreenEffect`. NeoForge's `RenderBlockScreenEffectEvent.FIRE` fires before that call and provides the shared pose stack, but it has no corresponding post event. Mutating that shared stack in the event can leak the transform into the later item-activation animation.

A narrow Mixin around the one vanilla call is therefore safer than the public event for this behavior.

## Configuration

```json
"lowFire": {
  "enabled": false,
  "verticalOffset": 0.2
}
```

- Offset range: `0.0` through `0.5` screen-space units.
- `0.0` is visually vanilla even when enabled.
- Default disabled preserves upgrade behavior.

## Mixin Design

Add one `ScreenEffectRendererMixin` redirect for the `renderFire` invocation in `renderScreenEffect`, with `require = 1`.

The redirect calls the shadowed vanilla `renderFire` method:

1. If disabled, call vanilla directly.
2. If enabled, `pushPose()`.
3. Translate downward by the configured positive offset using a negative Y transform.
4. Call vanilla `renderFire` with the original buffer source.
5. Always `popPose()` in `finally`.

The redirect does not inspect textures or emit vertices. The push/pop pair guarantees that item-activation rendering receives the original stack.

## Lifecycle And Failure Behavior

Low Fire has no mutable transient state. It reads the immutable active Profile each render. Profile switching and toggling take effect on the next frame. A validated zero offset and a disabled setting both preserve vanilla geometry.

Mixin application failure must stop client startup through `require = 1`; silent loss of the feature is not accepted. Runtime configuration failure falls back to disabled before rendering begins.

## Expected Code Areas

- `feature/lowfire/LowFireSettings` access through the active snapshot
- `mixin/render/ScreenEffectRendererMixin`
- Mixin JSON registration
- Rendering settings category and translations
- Unit and structural tests

No resource asset is added.

## Automated Tests

- Offset validation accepts endpoints and rejects non-finite/out-of-range values.
- Disabled and zero-offset paths preserve the original transform.
- A small transform adapter verifies push/translate/call/pop order, including an exception from the delegated renderer.
- Structural test verifies one redirect target, the 1.21.8 descriptor, and `require = 1`.
- Resource scan confirms no fire texture is introduced.

## Manual Acceptance

- Compare enabled and disabled first-person fire at offset `0.0`, `0.2`, and `0.5`.
- Trigger a totem/item activation while burning and confirm its transform is unchanged.
- Switch to third-person and confirm entity flames remain vanilla.
- Open screens while burning and verify no stack corruption or misplaced UI.
- Switch Profiles while burning and confirm next-frame application.
- Confirm damage, fire duration, particles, sounds, and server state do not change.

## Completion Criteria

- Only the first-person fire overlay position changes.
- Vanilla textures, buffers, and animation remain authoritative.
- Pose-stack restoration is guaranteed even when rendering throws.
- The Mixin is one narrowly documented 1.21.8 call redirect.
