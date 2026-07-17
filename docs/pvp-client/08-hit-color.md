# Hit Color Solution

## Goal

Replace the color of Minecraft's existing server-synchronized hurt overlay while preserving vanilla decisions about which visible living entities are hurt, occluded, invisible, or no longer flashing.

## Non-Goals

- No client-side hit prediction, attacker attribution, extended flash duration, through-wall rendering, or invisible-entity reveal.
- No new texture asset or entity state.
- No custom color for an empty swing or unconfirmed local attack.

## AxolotlClient Reference Analysis

Reference commit: `b1d066585626e4a7adf9f4ddbeb31cbf1ec3245f`

Reference files:

- `common/.../AxolotlClientConfigCommon.java`
- `versions/1.21/.../mixin/OverlayTextureMixin.java`
- `versions/1.21/.../mixin/OverlayTextureAccessor.java`
- `versions/1.21/.../config/AxolotlClientConfig.java`
- `versions/1.21/.../mixin/ArmorFeatureRendererMixin.java`

Useful behavior:

- Customize the existing overlay resource instead of inventing a second entity-render pass.
- Let vanilla hurt state determine timing.
- Upload a changed dynamic texture only when the configured color changes.

Rejected implementation details:

- Upstream constructor-constant modification and Fabric/Yarn accessors are not copied.
- Chroma, custom duration, and a separate armor toggle are not in scope.
- The implementation does not infer that the local player caused a hurt event.
- Upstream texture-writing code and color conversion are not reused.

## Semantics

Minecraft 1.21.8 sets `LivingEntityRenderState.hasRedOverlay` when the server-synchronized entity has positive `hurtTime` or `deathTime`. `LivingEntityRenderer.getOverlayCoords` selects the hurt row of the global 16x16 `OverlayTexture` only for that state.

The feature changes the color of that vanilla row. Therefore:

- Empty swings do not trigger it.
- Hurt duration remains server/vanilla controlled.
- Vanilla depth testing still hides entities behind walls.
- Vanilla body visibility still controls fully invisible entities.
- Any visible entity that vanilla marks hurt uses the configured color, regardless of who caused the damage.

Reliable local-attacker attribution is unavailable in the standard client protocol and is deliberately not guessed.

## Configuration

```json
"hitColor": {
  "enabled": false,
  "color": "#80FF0000"
}
```

There is no duration setting. Color uses validated `#AARRGGBB`. Default disabled preserves the original overlay.

## NeoForge 1.21.8 Evidence

`OverlayTexture` owns a private final `DynamicTexture`. The upper eight pixel rows represent the hurt-color overlay, while the lower rows contain the vanilla white-overlay gradient. NeoForge has no event for replacing this texture or its pixels.

One read-only Mixin accessor for the `DynamicTexture` is justified. No renderer method is redirected and no entity render state is changed.

## Texture Ownership And Restore

`HitColorController` operates through a small `OverlayTextureAccess` adapter:

1. Resolve the current `OverlayTexture` instance from `GameRenderer`.
2. On first use of that instance, capture an immutable copy of its original upper eight rows.
3. When enabled or color changes, replace only those upper rows and upload once on the render thread.
4. When disabled, switching Profiles, or shutting down the feature, restore the captured original rows and upload once.
5. Never write the lower white-overlay rows.

Capturing actual original pixels is preferred to hard-coding vanilla red because it preserves another compatible mod's pre-existing texture customization. If the `OverlayTexture` instance changes, discard the old capture and capture the new instance before applying.

Repeated application of the same desired color is a no-op. GPU upload is scheduled through the render system when the caller is not already on the render thread.

## Lifecycle And Failure Behavior

- Profile switch restores the owned original before applying the new Profile color.
- Feature disable restores original pixels immediately.
- Resource/client renderer replacement causes instance recapture.
- A missing/closed texture or pixel image fails closed: log, stop ownership, and do not claim the custom color is active.
- A partial pixel write is prevented by preparing the complete upper-row array before mutation; an upload failure attempts original-row restoration.

## Expected Code Areas

- `feature/hitcolor/HitColorController`
- `feature/hitcolor/OverlayPixelGenerator`
- `feature/hitcolor/OverlayTextureAccess`
- `mixin/render/OverlayTextureAccessor`
- Settings entry, translation, Mixin JSON registration, and tests

No PNG, shader, model, or language text from AxolotlClient is added.

## Automated Tests

- Pixel generator writes exactly the upper 16x8 region with normalized ARGB.
- Lower rows remain byte-for-byte unchanged.
- Capture/apply/restore round trip preserves arbitrary original pixels.
- Same-color reapplication avoids upload.
- Profile color change performs one prepared replacement.
- Disable and failed-upload paths restore ownership safely.
- New texture instance gets a separate original capture.
- Structural test verifies only the private `DynamicTexture` accessor is mixed in.
- Static test confirms no attack tracker or hurt-duration mutation is present.

## Manual Acceptance

- Hurt players, armored players, and several mob renderers.
- Observe an entity hurt by another player to confirm documented global hurt semantics.
- Empty swing and attack an invulnerable/failed target.
- Place a hurt entity behind solid geometry and test a fully invisible entity.
- Toggle and switch Profiles during a hurt flash.
- Disable and compare the exact original red overlay.
- Reload resources and change dimensions to verify texture-instance handling.

## Completion Criteria

- Vanilla `hasRedOverlay` remains the only trigger and timer.
- Only the hurt-color rows of the current dynamic overlay texture are owned.
- Original pixels are captured and restored rather than assumed.
- No asset, attack inference, visibility change, or duration extension is introduced.
