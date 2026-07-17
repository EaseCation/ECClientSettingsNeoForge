# Fullbright Solution

## Goal

Provide local lightmap-only brightness modes without modifying the player's saved Gamma option, potion effects, world light data, or server state.

## Non-Goals

- No block/sky light-engine changes, x-ray behavior, fog removal, or server packets.
- No real Night Vision effect, status icon, particles, or effect duration.
- No persistent mutation of Minecraft `Options`.

## Current Repository State

The module does not currently intercept lightmap calculation, Gamma reads, Night Vision blending, or effect state. Existing global configuration contains no brightness setting.

## AxolotlClient Reference Analysis

Reference commit: `b1d066585626e4a7adf9f4ddbeb31cbf1ec3245f`

Reference files:

- `common/src/main/java/io/github/axolotlclient/AxolotlClientConfigCommon.java`
- `versions/1.21/src/main/java/io/github/axolotlclient/mixin/LightmapManagerMixin.java`

Useful behavior:

- Fullbright is applied at client lightmap calculation rather than world data.
- Disabling returns to the value supplied by vanilla.

Rejected implementation details:

- Upstream replaces the Gamma option object with a fixed synthetic value and supports only one boolean mode.
- Fabric/Yarn Mixin signatures are not reused.
- The project does not create a persistent fake option or copy upstream constants.

## NeoForge 1.21.8 Evidence

Minecraft 1.21.8 computes the client lightmap in `LightTexture.updateLightTexture(float)`. The method derives a vanilla Night Vision blend factor and separately reads `Options.gamma()`. NeoForge 21.8.52 does not expose an event that can safely replace either local value.

Two narrow `@ModifyVariable` injections are justified: one after the vanilla Night Vision factor is stored and one after the Gamma float is stored. They are pinned to the 1.21.8 method descriptor and bounded slices, each with `require = 1`. No other lightmap value is modified.

## Configuration

```json
"fullbright": {
  "mode": "OFF",
  "strength": 1.0
}
```

Modes:

- `OFF`: return all vanilla values unchanged.
- `GAMMA`: interpolate from the vanilla Gamma value to a synthetic lightmap Gamma ceiling.
- `NIGHT_VISION`: raise the vanilla Night Vision blend factor to at least the configured strength.

`strength` is finite and in `0.0..1.0`. Strength zero is visually vanilla. A real server-supplied Night Vision factor is never reduced.

The synthetic Gamma ceiling is an implementation constant documented in code and verified visually; it is not exposed as an unbounded setting.

## Runtime Flow

`FullbrightController` is a pure selector used by the Mixin adapters:

```text
effectiveGamma(vanilla)
effectiveNightVision(vanilla)
```

- `OFF` returns both inputs.
- `GAMMA` changes only Gamma.
- `NIGHT_VISION` changes only the Night Vision blend.

The controller reads the immutable active Profile. It never holds a reference to an `OptionInstance` and never calls `set`. LightTexture is recalculated by the normal client tick; switching a Profile is visible on the next lightmap update.

## Lifecycle And Failure Behavior

No original option needs to be captured or restored because it is never mutated. World changes and disconnects naturally create/use the new world's lightmap while the active Profile remains selected. Invalid mode or strength is rejected before snapshot publication.

If either Mixin target drifts, `require = 1` causes startup failure instead of silently claiming Fullbright works. If the feature controller cannot supply a valid value, the adapter returns vanilla input.

## Expected Code Areas

- `feature/fullbright/FullbrightMode`
- `feature/fullbright/FullbrightController`
- `mixin/render/LightTextureMixin`
- Rendering settings category and translations
- Mixin JSON registration and tests

No texture, shader, or potion resource is added.

## Automated Tests

- `OFF` is an identity function for arbitrary finite vanilla inputs.
- Gamma interpolation is monotonic, bounded, and strength-zero identity.
- Night Vision takes the maximum of vanilla and requested blend.
- Mode separation proves Gamma mode never changes Night Vision and vice versa.
- Non-finite and out-of-range strength is rejected by Profile validation.
- Structural test verifies exactly two 1.21.8 local-value injections with `require = 1`.
- Test confirms no call path mutates `Options.gamma()` or player effects.

## Manual Acceptance

- Compare `OFF`, `GAMMA`, and `NIGHT_VISION` in caves, night, Nether, and End.
- Test strengths `0.0`, `0.5`, and `1.0`.
- Apply real Night Vision and Darkness effects and confirm their status/effect data remains vanilla.
- Change the vanilla brightness slider before and after using Fullbright; confirm its saved value is unchanged.
- Switch Profiles in a dark area and confirm next-lightmap application.
- Disconnect/reconnect and enter singleplayer to check state isolation.

## Completion Criteria

- Only the client lightmap output changes.
- Vanilla options, effects, world lighting, and server state remain untouched.
- `OFF` and strength zero are provable identity paths.
- Both Mixin injections are narrow, required, and documented against 1.21.8.
