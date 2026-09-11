# Block Selection Mask Implementation Plan

## Goal

Add a Profile-backed filled mask for the block selected by the crosshair. The mask
must follow the vanilla target and outline shape while remaining independent from
the existing custom outline color.

## Reference behavior

AxolotlClient commit `b1d066585626e4a7adf9f4ddbeb31cbf1ec3245f` adds an
`outlineFill` toggle and an `outlineFillColor`. Its renderer obtains the selected
block's outline shape, iterates every shape box, expands each box by a very small
epsilon, and emits six colored quads. The implementation is Fabric/Yarn-specific
and uses a GUI render layer. This project only reuses the observed behavior; it
does not copy upstream source, class structure, mixin signatures, assets, or text.

## Current NeoForge path

`BlockOutlineRenderer` already handles `RenderHighlightEvent.Block`. The event is
posted once for the opaque pass and once for the translucent pass. The handler
currently resolves `BlockState.getShape(level, pos, CollisionContext.of(camera))`,
uses `RenderType.lines()`, and cancels the event only after custom vertices are
submitted. This is sufficient for the mask; a Fabric `WorldRenderer` mixin is not
needed.

## Configuration contract

Extend `BlockOutlineSettings` with:

```json
{
  "enabled": false,
  "color": "#CCFFFFFF",
  "fillEnabled": false,
  "fillColor": "#4DFFFFFF"
}
```

`enabled` controls the outline and `fillEnabled` controls the filled mask. Either
feature may be used alone. Both colors use the existing `#AARRGGBB` parser and
zero alpha remains valid. The new fields are defaulted through the existing
recursive JSON merge so existing schema-4 Profiles remain readable and are
written back with explicit defaults.

## Rendering design

1. Resolve the client level, camera entity, target state, pass selection, shape,
   buffer source, and camera-relative offsets once.
2. If filling is enabled, iterate `shape.forAllBoxes` and emit six quads per box.
   Expand each min/max coordinate by `0.00005` before adding the camera offset to
   avoid z-fighting without changing the hit shape.
3. Submit fill vertices to the native `RenderType.debugStructureQuads()` layer.
   This keeps the normal `LEQUAL` world depth test, disables depth writes, and
   disables face culling, so the mask stays on the selected block surface and does
   not poison later translucent depth. This is an intentional NeoForge 1.21.8
   adaptation of AxolotlClient's GUI-layer fill, whose depth behavior is stronger.
4. Preserve the existing high-contrast secondary outline and custom line pass.
   Requesting the line buffer flushes the shared fill batch; `endLastBatch()` then
   flushes the final line batch. Cancel the event only if at least one enabled
   pass successfully submitted geometry.
5. Keep the opaque/translucent pass check so a target is never drawn twice.

## Code changes

- Add fill fields, defaults, and validation to `BlockOutlineSettings` while
  retaining the two-argument constructor used by existing callers.
- Add immutable draft setters and two Rendering-category controls.
- Encode/decode the two fields in `ProfileJsonCodec`; accept sparse old
  `blockOutline` objects through `withDefaults`.
- Synchronize the bundled `ECConfigRedirectNeoForge` default Profile so a fresh
  config install includes the same fields.
- Update `BlockOutlineController` to independently submit fill and outline passes.
- Add a focused fill renderer using Mojmap `VertexConsumer` and
  `RenderType.debugStructureQuads()`; do not import AxolotlClient classes.
- Update Chinese/English strings and the block-outline documentation.

## Verification matrix

- Unit tests: defaults, ARGB round-trip, sparse schema-4 JSON, independent
  controller submissions, disabled/failed submissions, and profile draft save.
- Static checks: no block-selection Mixin or `io.github.axolotlclient` reference;
  only the task worktree is changed.
- Composite build/test from `NeoForgeWorkspace`, not from the included build
  directory alone.
- Client acceptance when available: full cube, slab, stair/fence-like multi-box
  shapes, opaque and translucent targets, semi-transparent and zero-alpha fill,
  outline-only/fill-only/both, high-contrast outline, walls, and Profile switching.
  If no client session is available, report visual acceptance as unverified.

## Risks and non-goals

The filled layer is a client-only visual effect. It does not alter target
acquisition, block models, reach, textures, entities, line width, shaders,
post-processing, resource packs, or server state. The small epsilon and
no-depth-write layer need real-client checks around adjacent translucent geometry.
