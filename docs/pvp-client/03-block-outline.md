# Block Outline Solution

## Goal

Allow a Profile to replace the vanilla targeted-block outline color and opacity while retaining vanilla target acquisition, shape, depth, distance, and line width.

## Non-Goals

- No dynamic line width, filled highlight, entity outline, through-wall rendering, or extended reach.
- No Mixin when the supported NeoForge event can express the behavior.
- No custom texture, shader, or copied rendering utility.

## AxolotlClient Reference Analysis

Reference commit: `b1d066585626e4a7adf9f4ddbeb31cbf1ec3245f`

Reference files:

- `common/.../AxolotlClientConfigCommon.java`
- `versions/1.21/.../mixin/WorldRendererMixin.java`
- `versions/1.21/.../mixin/WorldRendererAccessor.java`

Useful behavior:

- The feature preserves the targeted block's actual `VoxelShape`.
- Color includes alpha rather than a separate opaque-only palette.
- The vanilla selection result remains authoritative.

Rejected implementation details:

- Fabric/Yarn Mixin targets are not applicable to NeoForge 1.21.8.
- Dynamic line-width mutation relies on internal render state and is outside the requested behavior.
- Filled outlines and upstream draw helpers add scope and are not reused.

## NeoForge 1.21.8 Evidence

NeoForge 21.8.52 exposes cancellable `RenderHighlightEvent.Block` with the `BlockHitResult`, camera, pose stack, buffer source, delta tracker, and translucent-block phase. Minecraft 1.21.8 vanilla renders the outline by obtaining `BlockState.getShape(..., CollisionContext.of(cameraEntity))` and passing it to `ShapeRenderer.renderShape` with one packed ARGB color.

This is a complete supported event surface; no Mixin is justified.

## Configuration

```json
"blockOutline": {
  "enabled": false,
  "color": "#CCFFFFFF"
}
```

- Default disabled preserves existing behavior for upgraded players.
- `#AARRGGBB` is validated and normalized by Profile Core.
- Zero alpha is valid and intentionally makes the outline invisible while the feature remains enabled.

## Runtime Flow

`BlockOutlineRenderer` subscribes to `RenderHighlightEvent.Block`:

1. Read the immutable active-Profile setting.
2. Return without touching the event when disabled.
3. Resolve the target position and current block state from the client level.
4. Obtain the exact vanilla shape with the current camera entity's collision context.
5. Translate by block position minus camera position.
6. Acquire the vanilla line render buffer and call `ShapeRenderer.renderShape` with configured ARGB.
7. Cancel the event only after the custom vertices are submitted.

If the client level, camera entity, state, or shape is unavailable, the handler returns without cancelling so vanilla remains the fallback.

The handler honors `isForTranslucentBlocks()` by processing only the event call selected by vanilla/NeoForge for that target. It must not submit a second pass.

## Lifecycle And Failure Behavior

There is no transient controller state. Profile switching changes the next render call. Invalid settings cannot enter the active snapshot. An unexpected rendering exception is logged and leaves the event uncancelled where possible.

## Expected Code Areas

- `feature/blockoutline/BlockOutlineRenderer`
- Registration from the client event bootstrap
- Rendering-category builder in the settings screen
- Profile model default/validation tests

No new Mixin or resource asset is expected.

## Automated Tests

- Disabled setting does not cancel or render.
- Enabled setting passes the exact packed ARGB value.
- Missing client context fails open to vanilla.
- Profile switch changes the setting source without cached stale color.
- Static structure check confirms no Block Outline Mixin is registered.

Rendering calls should be isolated behind a narrow adapter so control flow and color conversion can be unit-tested without a live renderer.

## Manual Acceptance

- Target full cubes, slabs, stairs, fences, fluids, and translucent blocks.
- Verify shape matches vanilla at multiple camera positions.
- Test opaque, semi-transparent, and zero-alpha colors.
- Verify walls and vanilla reach still prevent an outline.
- Toggle and switch Profiles while continuously targeting a block.
- Disable and compare directly with vanilla color and thickness.

## Completion Criteria

- Only color and opacity differ from vanilla.
- Event cancellation occurs only when a valid custom outline is submitted.
- No Mixin, custom asset, reach change, fill, or dynamic line width is introduced.
