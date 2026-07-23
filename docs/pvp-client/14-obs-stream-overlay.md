# OBS Stream Privacy Overlay

## Goal

Give a streamer a local-only view of selected private Minecraft content while an
OBS Game Capture source receives the same frame with that content removed or
anonymized. The design must remain understandable under time pressure:
protection is global, unsafe states are visible, and the streamer has a
repeatable pre-flight check before going live.

This is a privacy aid, not a general redaction or digital-rights-management
system. A successful build is not evidence that a particular OBS scene is safe.
The in-game test marker and a live OBS preview are the required evidence.

## Provenance And License

The capture-order technique is adapted from
[OBS Overlay](https://github.com/zziger/obs-overlay) by zziger / Artem
Dzhemesiuk under the MIT License.

- Upstream release: `2.0.0-beta.3` for Minecraft 1.21.4.
- Pinned upstream commit: `f0079170add6b5b4dcc3807c741ebad752314a19`.
- Port target: Minecraft 1.21.8, NeoForge 21.8.x, Java 21.
- Native dependency: official MinHook 1.3.3 x64/x86 binaries, BSD-2-Clause.
- MinHook includes the separately attributed HDE32 and HDE64 portions by
  Vyacheslav Patkov.

Repository and packaged notices are maintained in `THIRD_PARTY_NOTICES.md` and
`META-INF/licenses/ecclientsettings/`. Settings must show the upstream author,
project URL, and MIT license rather than presenting the feature as original
EaseCation work.

## User Mental Model

OBS Game Capture and the player's monitor do not have to receive pixels at the
same point in the frame. Selected content is removed from Minecraft's normal
frame and held in private overlay textures. In player-pseudonym mode, a safe
alias is placed in the public frame while the real name remains private. OBS
Game Capture takes the public frame first. Immediately afterward, the mod draws
the private textures for the physical game window and then allows the real
buffer swap to finish.

```text
Minecraft scene without selected private content
  + public frame + optional player aliases --> OBS Game Capture
  + real selected content -> private FBO
                                      |
                                      +------> local game window only
```

This model is valid only when the mod's native hook is installed before OBS
attaches its Game Capture hook. The test marker proves the ordering for the
current process, source, and session; it is not a permanent guarantee.

## Safety Defaults And Scope

OBS protection is global and stored in `ecclientsettings-obs-overlay.toml`. It is
deliberately outside PvP Profiles so switching a gameplay Profile cannot
silently disable a privacy control.

The first-run defaults are:

- master switch off;
- safety test marker on;
- fail-closed behavior on;
- Debug/F3 selected for protection;
- player-name mode `UNCHANGED`;
- player alias format `FRIENDLY`, color mode `DISTINCT`, and unified
  screen-open auto-hide on;
- all other HUD, world, and screen components off.

Fail closed means that when the hook is unavailable, has unsafe ordering, or the
compositor fails, selected private content never falls back into the OBS image.
Most selected content is omitted from both outputs; a player pseudonym may be
shown in both outputs when that safe fallback remains available. Losing local
convenience is safer than silently showing a secret on stream. A user may opt
out, but doing so converts a protection failure into a possible privacy leak and
must be described as unsafe in the UI.

Protection applies only to components the user explicitly selects:

- HUD: Debug/F3, chat, chat input, player list, subtitles, scoreboard, action
  bar, title/subtitle, status effects, vanilla main HUD, and EaseCation HUD.
- World: a single player-name policy covering both normal and sneaking players,
  plus sign text, chests, item-frame maps, banner patterns, and beacon beams.
- Screens: survival inventory, creative inventory, pause menu, command block,
  all in-game screens, or an explicit list of handled-menu IDs such as
  `minecraft:anvil`.

World components that depend on depth, especially player names, signs, chests,
item-frame maps, banners, and beacon beams, are experimental with Iris shader
packs until their matrix rows have direct game evidence.

## Player Name Policy

Player names use one mutually exclusive mode rather than separate normal and
sneaking checkboxes. A posture change must never change the privacy decision,
and ViaBedrockUtility may defer both postures through the same batching path.
The dedicated values are stored under `obsOverlay.playerNames` as `mode`,
`aliasFormat`, `aliasColorMode`, and `autoHideWhenScreenOpen`; the internal
`PLAYER_NAME_TAGS` render scope is not a second user-configurable component.

| Saved mode | Local window when the private path is ready | OBS Game Capture |
|---|---|---|
| `UNCHANGED` | Real player name | Real player name |
| `HIDE` | Real player name | No player name |
| `PSEUDONYMIZE` | Real player name | Stable pseudonym |

The master switch controls the effective mode. When it is off, the effective
mode is always `UNCHANGED`, but the saved selection is retained for the next
time the master switch is enabled. Normal and sneaking player names always
follow the same mode. Named mobs, armor stands, NPCs that do not use a player
render state, and every other non-player entity retain their original names in
both outputs.

Pseudonyms are deterministic and distinguishable for one server connection so
viewers can follow the same player during that connection. They are regenerated
after disconnecting or restarting and must never be derived from an unsalted
public hash of the real name. The two formats are `FRIENDLY` (for example,
`Amber Fox - ABCD2`) and `NUMBERED` (for example, `Player ABCD2`); the safe code
is part of the deterministic identity and collision handling. `DISTINCT` assigns
a stable safe color per alias, `TEAM` copies only the plain team color, and
`WHITE` uses one neutral color. Prefixes, suffixes, hover data, insertion text,
fonts, and other style
data from the real name are not copied into an alias.

The unified screen-open option temporarily hides normal names, sneaking names,
and public aliases in both outputs while a non-chat screen is open. It does not
change the saved mode.

Fail closed remains authoritative. If a player cannot be identified reliably,
ViaBedrockUtility cannot separate a deferred player entry, an Iris pass cannot
be redirected safely, or a world buffer cannot be closed safely, a real player
name must never be allowed into the public frame. The safe fallback may show the
alias in both outputs or hide the player name completely. With fail closed off,
the runtime may fall back to ordinary rendering and expose the real name; the UI
must report that state as unsafe.

## Status Contract

Only `READY` is eligible for the private overlay path, and even `READY` still
requires the marker check.

| Status | Meaning | User action |
|---|---|---|
| `NOT_INITIALIZED` | Render and native-hook startup has not completed. | Do not stream; wait for client startup or inspect the log. |
| `READY` | Hook order and renderer initialization are currently acceptable. | Run the marker and protected-content checks before going live. |
| `UNSAFE_CAPTURE_ORDER` | an OBS graphics-hook DLL was already loaded when the mod installed its hook. | Stop OBS and Minecraft, then start Minecraft before OBS. Fail closed hides selected content locally by default. |
| `UNSUPPORTED` | The platform is not supported. | Use Windows x64/x86 with the OpenGL Minecraft renderer; do not rely on this feature. |
| `FAILED` | Native setup or per-frame composition failed. | Stop streaming, preserve the log, restart in the required order, and revalidate. Fail closed remains active by default. |
| `STOPPED` | The client renderer is shutting down. | No protection is active. |

Seeing `READY` does not prove that OBS is using Game Capture, that the correct
source is visible, or that another capture application hooks at a compatible
point. The preview check remains mandatory.

## First-Use And Every-Stream Pre-Flight

Use this exact order for the first setup and repeat the abbreviated verification
before every stream:

1. Close OBS completely, including any tray/background instance. Also close
   other applications that inject capture overlays into Minecraft when
   practical.
2. Start Minecraft and wait until the title screen or world is stable.
3. Open EaseCation Client Settings, then open **OBS Stream Privacy Overlay**.
4. Confirm the status is `READY`. Leave **Fail closed** and **Test marker** on.
5. Turn on the master switch. Keep the default F3 protection selected and choose
   the required player-name mode for the first validation, click **Done** to
   save, and return to the game.
6. Confirm `OBS TEST` is visible near the top-right of the physical game window.
   `OBS UNSAFE` is a stop signal, not a successful test.
7. Only now start OBS. Add or select a **Game Capture** source, choose the
   Minecraft window, and disable **Capture third-party overlays**.
8. Inspect the live OBS preview. `OBS TEST` must be absent from the preview while
   remaining visible in the physical game window.
9. Open F3. Its text must remain visible locally and be absent from the OBS
   preview. If `PSEUDONYMIZE` is selected, observe at least two players: real
   names must remain local, OBS must show different stable aliases, and a named
   non-player entity must remain unchanged. Verify another real component before
   trusting a customized setup.
10. If either protected item appears in OBS, or disappears locally while status
    claims `READY`, do not go live. Stop both applications, correct the source or
    startup order, and repeat from step 1.
11. After changing scene, source, capture mode, fullscreen state, resolution,
    GUI scale, shader pack, graphics mod, or OBS version, repeat steps 6 through
    10. Keep the OBS preview visible during the check.
12. Once the stream is live, briefly verify the program output as well as the
    preview. A studio-mode preview can differ from the program scene.

The marker should normally remain enabled. Turning it off removes the fastest
way to detect a capture-order regression.

## Threat Boundary

The feature is designed to protect selected pixels from OBS **Game Capture** on
supported Windows x64/x86 OpenGL clients when the required startup order and
preview test pass.

It does not promise protection from:

- OBS Display Capture or Window Capture;
- Discord screen share, Xbox Game Bar, GPU-driver recording, remote desktop,
  screenshots, or other capture/injection software;
- OBS started before Minecraft, or an OBS source that attached before the mod's
  hook;
- a scene/source change made after validation;
- content outside the selected components;
- text already written to logs, chat history, crash reports, clipboard, network
  traffic, replay data, or another mod's storage;
- another mod that redraws the same information in an unprotected render path;
- unsupported render backends, operating systems, Windows on ARM, or future
  game/driver/OBS changes;
- malicious software or a hostile mod running in the same process.

Display Capture and Window Capture are useful negative controls: the marker may
appear there because those methods observe the final local result. That is
expected and demonstrates why they must not be used for privacy protection.

The mod does not inspect whether a string is actually sensitive. Users remain
responsible for selecting every relevant component and for validating the final
program output. Never enter a server address, coordinate, command, account
identifier, or private message on stream until the active setup has passed the
check.

## Minecraft 1.21.4 To 1.21.8 Renderer Port

The upstream 1.21.4 implementation brackets component render methods with direct
framebuffer changes. It also prevents `_glBindFramebuffer` calls globally while
the overlay owns the framebuffer. That approach cannot be copied unchanged.

Minecraft 1.21.8 records GUI work into `GuiRenderState` and performs the actual
GPU draw later in `GuiRenderer`. Switching an FBO at the head and return of a
HUD method therefore switches it before any relevant pixels are emitted. A
global framebuffer-bind cancellation would also interfere with Sodium and Iris
render passes.

The 1.21.8 design instead uses four narrow paths:

1. **Deferred GUI separation.** During a protected HUD or screen scope, element,
   text, item, picture-in-picture, and stratum submissions are diverted from the
   main `GuiRenderState` into a dedicated overlay `GuiRenderState`. A separate
   `GuiRenderer` draws that state into a transparent `TextureTarget`. Discard
   mode consumes the submissions without adding them to either visible target.
2. **World output overrides.** Protected world components temporarily set
   Mojang's public `RenderSystem.outputColorTextureOverride` and
   `outputDepthTextureOverride`. Existing values are saved and restored rather
   than assuming the vanilla main target. Compatible active depth is copied so
   local composition can preserve occlusion, and the final compositor compares
   the overlay depth with the completed scene depth to reject later occluders.
   Buffered vertices are flushed with `endBatch()` before and after each
   protected world element so they cannot be emitted after the target is
   restored. This boundary is necessary for ownership correctness, but it also
   reduces batching efficiency. Dense scenes containing many name tags, signs,
   chests, maps, banners, or beacons can therefore incur a per-element CPU/GPU
   cost and must be checked with a frame-time graph, not only an average-FPS
   reading.
3. **Public player-alias separation.** Pseudonym mode diverts the real player
   name into the private world target and renders only the generated alias into
   a public target. The public alias is present for OBS capture but must be
   removed from the final local result before the private real name is composed;
   drawing both labels on top of one another is not acceptable. Non-player names
   never enter this path.
4. **Post-capture local composition.** MinHook intercepts `wglSwapBuffers`. When
   OBS attached after Minecraft, OBS captures the public frame and then the mod
   composites world and GUI overlay textures to the real window immediately
   before the original swap. The compositor uses its own minimal OpenGL shader
   and full-screen triangle.

The compositor saves and restores the OpenGL program, vertex array, array-buffer
binding, draw/read framebuffer bindings, the default framebuffer draw buffer,
viewport, active texture, texture and sampler bindings, blend state and
equations, depth/cull/scissor/stencil/color-logic enables, depth mask, and color
mask. Indexed blend and color-mask changes are limited to draw buffer zero when
the driver exposes OpenGL 4.0 or `ARB_draw_buffers_blend`; this avoids flattening
Iris's MRT state. This is required because leaked state can corrupt the next
Sodium or Iris frame even when the current overlay looks correct.

Render targets are resized and cleared each frame. Unbalanced capture scopes and
world overrides are recovered at the next frame with a warning. Hook or shader
exceptions transition the runtime to `FAILED` so the next selected component
uses fail-closed behavior.

## Compatibility Strategy

### Sodium

- Do not mix into Sodium classes or assume its framebuffer IDs.
- Do not cancel global framebuffer binds.
- Keep the final compositor self-contained and restore all observed GL state.
- Validate local/OBS separation together with terrain, entities, translucent
  blocks, particles, menus, and dimension changes; a correct marker alone does
  not prove world-pass correctness.

### Iris

- Preserve and restore pre-existing output color/depth overrides; these can be
  Iris render targets rather than the vanilla main target.
- Size the overlay target from the active source and copy depth only when format
  and dimensions are compatible.
- Flush only verified buffer implementations at redirect boundaries. The known
  Iris unflushable wrapper is structurally unwrapped; an unknown `endBatch()`
  override fails closed instead of treating a possible no-op as success.
- Query Iris's optional API for both shadow-pass state and an active shader pack.
  The current Iris custom-pass path can bypass Mojang output overrides, so an
  active shader pack can disable the normal private/public world separation.
  With fail-closed enabled, a real player name must not fall back to a public
  pass: the safe result is an alias in both outputs when that path is verified,
  or no player name at all. Other selected world elements and their shadow-pass
  contribution are not drawn. HUD and screen protection remain available. With
  fail-closed disabled, the setting page reports that world elements and real
  player names cannot be guaranteed private.
- Treat depth-sensitive world components as experimental. Validate with Iris
  installed both without a shader pack and with at least one actively enabled
  pack. Include shadows, translucent surfaces, water, hand rendering, and shader
  reloads.
- If the log reports a depth-copy or buffer-flush fallback, do not mark world
  rows as passed merely because HUD rows work.

### ImmediatelyFast

- The port does not take a hard dependency on ImmediatelyFast internals.
- GUI content is separated at render-state submission and rendered by its own
  `GuiRenderer`, avoiding an FBO switch around a draw that ImmediatelyFast may
  defer or batch.
- World buffers are explicitly flushed at ownership boundaries. This prevents
  delayed vertices from escaping the private target, but the per-element
  `endBatch()` calls can reduce throughput in a dense protected world scene.
- ImmediatelyFast can cache sign text in its Sign Atlas and replay it through a
  path that conflicts with the OBS sign-text target. While protected sign text
  is rendering, the integration detects `ISignText`, remembers its existing
  cache flag, temporarily disables caching, and restores the flag in `finally`.
  This is a narrow, temporary bypass: it does not globally disable
  ImmediatelyFast's caches and does not modify the saved configuration.
- If the compatibility API cannot be reached, the warning in `latest.log` is a
  failed sign-text compatibility result, not a harmless startup warning.
- Validate text, items, picture-in-picture screen content, chat, and repeated
  screen open/close cycles for missing, duplicated, stale, or one-frame-late
  geometry. Test sign text with ImmediatelyFast's sign-text cache both enabled
  and disabled, and inspect dense groups of signs for frame-time regressions.

### ViaBedrockUtility

- In a ViaBedrock session, ViaBedrockUtility may queue name tags instead of
  drawing them during the original entity renderer call. EC intercepts the
  stable public `DeferredNameTag.enqueue` ABI and classifies each player draw
  before it enters VBU's queue; `DeferredNameTag.flush` remains the stable replay
  boundary. Both names are explicitly preserved by the VBU production
  obfuscation profile, while its private queue and helpers remain obfuscated.
- VBU can batch player labels together with armor-stand or other non-player
  labels. The integration must classify deferred entries individually: player
  real names use the private path, player aliases use the public path, and
  non-player entries remain on VBU's ordinary path. Wrapping or clearing the
  entire queue would violate the non-player preservation contract.
- If a deferred item cannot be identified or separated reliably, fail closed
  suppresses the affected player real name rather than replaying it publicly.
  It must not remove an unrelated non-player label. ABI or Mixin application
  failures are logged and invalidate the affected matrix row; no private VBU
  field name or obfuscated implementation detail is used as a production hook.
- In pseudonym mode, EC intentionally does not publish the player
  `RenderNameTagEvent.DoRender` event. A listener can otherwise reconstruct the
  real identity from `PlayerRenderState.name` and draw it into the public target.
  `CanRender` still controls visibility, `HIDE` routes `DoRender` drawing through
  the private target, and `UNCHANGED` retains normal NeoForge event behavior.
- Startup with ViaBedrockUtility only proves that the optional mixin can load.
  All three player-name modes, normal/sneaking transitions, multiple player
  aliases, and named non-player entities must still be exercised in a real
  Bedrock session and checked in the local window, OBS Preview, and OBS Program
  output.

### EaseCation And Other Mods

- The EaseCation HUD has its own selectable component so FPS, ping, armor,
  potion, Keystrokes, and independent CPS widgets can be protected together.
- OBS settings remain separate from Profile settings; Profile changes must not
  toggle privacy state.
- Unknown HUDs are not automatically protected. A mod drawing directly or in a
  different GUI layer needs an explicit integration point. Installing such a
  mod successfully, or selecting **Vanilla main HUD**, does not make its HUD
  private.
- Other capture, overlay, shader, replay, optimization, UI, and GPU diagnostic
  mods must be treated as unverified until they pass the same marker plus real
  component checks.
- Compatibility means both privacy and normal rendering: no leak in OBS, no
  missing local content, no render corruption, and no persistent GL-state error.

## Current Compatibility Evidence (2026-07-23)

The table below records only evidence already collected during development. A
startup smoke test is intentionally not promoted to a privacy or renderer
compatibility pass.

| Environment | Startup evidence | Live OBS privacy evidence | Current conclusion |
|---|---|---|---|
| NeoForge baseline without Sodium, Iris, or ImmediatelyFast | Reached the main menu. | `NOT RUN` | Startup smoke only. Run B01 and the selected D/E rows. |
| Sodium 0.7.3 + Iris 1.9.6 + ImmediatelyFast 1.12.5 | Entered the game; the log reported all three mods as detected and no EC OBS Mixin application failure was observed. | `NOT RUN` | Combined startup smoke only. No protected component, depth, performance, Preview, or Program claim yet. |
| Complete current EaseCation client set: BedrockLoader, BedrockCameraLib, BEParticle, ECCameraClient, ECClientLight, GeyserUtilsBridge, ModUIClient, ViaBedrockUtility, Sodium, Iris, ImmediatelyFast, YACL, and IMBlocker | Client startup succeeded; ViaBedrockUtility was detected without an EC OBS Mixin application or `InvalidInjection` error. | `PASS` for the user-accepted Game Capture flow without an effective Iris shader pack. | Core HUD and screen separation passed in OBS Preview and Program. Experimental world rows, active shader packs, real ViaBedrock sessions, and unintegrated third-party HUDs remain unverified. |
| Iris with an actively enabled, effective shader pack | `NOT RUN` | `NOT RUN` | Shader compatibility is unverified. The Iris startup above did not use an effective shader pack and must not be advertised as a shader-compatibility pass. |
| Real ViaBedrock server/session | `NOT RUN` | `NOT RUN` | Deferred item-level player/non-player separation, normal/sneaking names, aliases, depth, and fail-closed handling still require manual acceptance. |
| Player pseudonym mode | `NOT RUN` | `NOT RUN` | The earlier user acceptance predates player aliases. It does not prove alias stability, local real-name restoration, non-player preservation, Iris fallback, or VBU item-level separation. |

The complete current EaseCation client set passed user acceptance on 2026-07-23
with OBS 31.1.1 Game Capture and no effective Iris shader pack. The accepted
core flow covered local-versus-OBS separation for the configured HUD and screen
components. This does not promote the active-shader, experimental world, or real
ViaBedrock rows to passing evidence, and it provides no evidence for the newer
player-pseudonym mode.

Before release or streaming with a different installed combination, repeat the
comparison against both OBS Preview and OBS Program. Also confirm that every
Display Capture or Window Capture source capable of seeing Minecraft is hidden
or removed from both outputs; those capture methods observe the final local
frame and are outside the protection boundary. Third-party HUDs remain public
unless this mod has an explicit integration for their actual render path.

## Manual Acceptance Protocol

Automated unit tests are not the primary evidence for this feature. Record each
manual row as `PASS`, `FAIL`, or `NOT RUN` with:

- date, mod commit/build SHA-256, Minecraft and NeoForge versions;
- Windows version, CPU architecture, GPU, and driver;
- OBS version, source type, capture method, and third-party-overlay option;
- complete relevant mod list and versions;
- Iris shader-pack name/version and preset when applicable;
- window mode, game resolution, Windows DPI scale, and Minecraft GUI scale;
- status shown in settings, local observation, OBS preview/program observation;
- screenshot or recording reference and the relevant log excerpt on failure.

A row passes only when local rendering and OBS output both match the expected
result. Do not infer unrun shader or mod combinations from a vanilla pass.

### Gate A: Capture Order And Failure Behavior

| ID | Scenario | Expected result |
|---|---|---|
| A01 | Start Minecraft, enable marker, then start OBS Game Capture with third-party overlays off. | Status `READY`; marker and F3 visible locally and absent in OBS. |
| A02 | Start OBS Game Capture before Minecraft. | Status `UNSAFE_CAPTURE_ORDER`; `OBS UNSAFE` visible; with fail closed, selected content is absent locally or player names use the documented alias fallback instead of leaking. |
| A03 | Repeat A02 with fail closed deliberately off in a non-sensitive test world. | UI clearly reports unsafe; no privacy claim is made; restore fail closed immediately. |
| A04 | Use OBS Display Capture. | Marker is expected to be visible in OBS; source is rejected for privacy use. |
| A05 | Use OBS Window Capture. | Marker visibility is recorded; source is rejected regardless of result. |
| A06 | Toggle the OBS Game Capture source off/on and change scenes after a pass. | Re-run marker/F3 checks; no stream starts on an unverified source. |
| A07 | Exit Minecraft while OBS remains open, then relaunch Minecraft. | Treat the session as invalid regardless of any race in attachment timing; an already attached hook is reported unsafe and fails closed. Restart in the documented order and pass A01. |
| A08 | Disable master switch. | Local view and OBS both return to ordinary vanilla/mod rendering; no marker is shown. |
| A09 | Enable master switch with test marker disabled. | Selected protection still works, but UI warns that the safety signal is unavailable; marker is re-enabled afterward. |

### Gate B: Renderer And Mod Combinations

Run A01 plus F3, chat, EaseCation HUD, one protected screen, all three
player-name modes, one named non-player entity, sign text, chests, and item-frame
maps for every row.

| ID | Mod set | Additional checks |
|---|---|---|
| B01 | NeoForge plus required EaseCation dependencies only | Establish baseline in a clean test world. |
| B02 | Sodium | Terrain rebuild, translucent blocks, particles, entity outlines, dimension change. |
| B03 | Sodium + Iris, no active shader | All B02 checks plus shader pipeline present but disabled. |
| B04 | Sodium + Iris with an active shader pack | Shadows, water/translucency, Nether/End, night, hand render, shader reload. |
| B05 | ImmediatelyFast | Chat flood, inventory items, entity preview, subtitles, repeated menu open/close, and signs with its sign-text cache enabled and disabled. |
| B06 | Sodium + Iris + ImmediatelyFast, no shader | Combined batching and render-target ownership. |
| B07 | Sodium + Iris + ImmediatelyFast, active shader | Full combined case; every world-depth result remains explicitly experimental until passed. |
| B08 | Complete current EaseCation client set | BedrockLoader, BedrockCameraLib, BEParticle, ECCameraClient, ECClientLight, GeyserUtilsBridge, ModUIClient, ViaBedrockUtility, Sodium, Iris, ImmediatelyFast, YACL, IMBlocker, the existing FPS/ping/armor/potion/Keystrokes/CPS HUDs, and every existing visual feature. Do not carry forward the startup-smoke result as an OBS pass. |
| B09 | Each additional installed render/UI/overlay mod, one at a time | Identify the first incompatible mod rather than testing only the full pack. |
| B10 | ViaBedrockUtility in a real Bedrock session | All three modes and normal/sneaking transitions through deferred replay; multiple players and named non-player entities; rapid join/leave, entity churn, depth/occlusion, and fail-closed behavior. |
| B11 | Each third-party HUD or direct-render overlay | Determine its actual render path. Record it as unsupported unless an explicit component scope keeps it local while removing it from Preview and Program. |

### Gate C: Window And Lifecycle Changes

| ID | Scenario | Expected result |
|---|---|---|
| C01 | GUI scale Auto and every practical fixed scale | No clipping, offset, wrong-size target, or marker leak. |
| C02 | 1280x720, 1920x1080, native display resolution, and one non-16:9 size | Overlay aligns with the base frame in both outputs. |
| C03 | Drag-resize window repeatedly and maximize/restore | No stale frame, stretch, black target, upside-down image, or one-frame privacy leak. |
| C04 | Toggle windowed/borderless/fullscreen modes | Revalidate marker and F3 after every transition. |
| C05 | Alt-tab, minimize/restore, and lose/regain focus | No hook failure, persistent missing content, or OBS leak. |
| C06 | Windows DPI scales used by the streamer, including mixed-DPI monitors | Game-window and OBS alignment remain exact. |
| C07 | Resource reload and, with Iris, shader reload/change | Status stays valid or fails closed; no stale/private pixels appear in OBS. |
| C08 | Join/leave worlds, switch servers, die/respawn, and change dimensions | Targets clear between states; no previous frame survives. Aliases remain stable within one connection and are regenerated after logging out before another connection. |
| C09 | Change PvP Profiles while OBS protection is enabled | OBS settings and selected components remain unchanged. |
| C10 | Save settings, restart in correct order, and reopen settings | Global mode/format/color/auto-hide values persist, aliases are regenerated for the new process, and the pre-flight succeeds again. |

### Gate D: HUD And Screen Components

For each row, test the component alone and with two adjacent components enabled.
The normal expectation is **visible locally, absent from OBS**. When auto-hide is
enabled and another non-chat screen is open, the selected HUD component may be
intentionally absent locally; record that as the configured behavior.

| ID | Component | Required stimulus and checks |
|---|---|---|
| D01 | Debug/F3 | Reduced and full debug info; left/right columns and profiler chart. |
| D02 | Chat | History, fade, colored/styled messages, links, long wrapped lines; input remains independent. |
| D03 | Chat input | Typed commands, suggestions, selection/cursor; chat history remains independent. |
| D04 | Player list/TAB | Multiple players, headers/footers, scores, skins, latency icons. |
| D05 | Subtitles | Multiple simultaneous directional subtitles and expiry. |
| D06 | Scoreboard | Sidebar add/update/remove, long names, team colors. |
| D07 | Action bar | Rapid updates, formatted text, fade. |
| D08 | Title/subtitle | Both lines, timing/fade, repeated titles. |
| D09 | Status effects | Beneficial/harmful effects, timers, ambient icons. |
| D10 | Vanilla main HUD | Hotbar, health, armor, hunger, air, XP, selected item name, mount bars. |
| D11 | EaseCation HUD | FPS, ping, armor, potion, Keystrokes, embedded CPS, left CPS, right CPS; editor preview does not leak unexpectedly. |
| D12 | Survival inventory | Player/entity preview, armor, recipe book, carried item and tooltips. |
| D13 | Creative inventory | Tabs, search text, scrolling items, player preview and tooltips. |
| D14 | Pause menu | Single-player and multiplayer variants plus background blur. |
| D15 | Command block | Text field, mode buttons, previous output, done/cancel. |
| D16 | Custom handled menu ID | Exact namespaced ID and path-only ID; matching menu hidden, non-matching menu unaffected. |
| D17 | All in-game screens | Inventory, chat, settings, advancement, and one modded screen; title screen remains unaffected. |

### Gate E: World Components And Depth

Test in daylight and darkness, at near/far distances, while moving the camera,
and behind opaque and translucent occluders. Repeat the entire gate with the
active Iris shader row before claiming shader compatibility.

| ID | Component | Required stimulus and checks |
|---|---|---|
| E01 | Player mode `UNCHANGED` | Normal and sneaking players show their real names identically in the local window and OBS; the master switch off has the same effective result. |
| E02 | Player mode `HIDE` | Real names remain local and are absent from OBS for normal and sneaking players; posture transitions never expose one frame. |
| E03 | Player mode `PSEUDONYMIZE` | Local window shows only real names; OBS shows only aliases. No alias text, background, or edge remains locally, and no real-name pixel or formatting reaches OBS. |
| E04 | Alias identity and formats | At least 20 players/NPC players, leave/re-enter view, join/leave, nick changes, Unicode and long names. Aliases are stable and unique for the connection; reconnecting rotates them. Both `FRIENDLY` and `NUMBERED` fit and never contain real-name data. |
| E05 | Alias colors | Test `DISTINCT`, `TEAM`, and `WHITE`, including teams with prefixes/suffixes and custom formatting. Only the allowed plain color is copied; real text/style metadata is absent. |
| E06 | Non-player preservation | Named mobs, pets, villagers, armor stands, and non-player Bedrock entities retain their exact original labels in local and OBS under every player mode. |
| E07 | Player depth and posture | Players in front of and behind opaque/translucent blocks, near/far distance, `SEE_THROUGH` behavior, sneaking transition, team color, and score text. Alias and real label keep matching depth and placement. |
| E08 | Screen-open auto-hide | With each player mode selected, opening a non-chat screen removes normal/sneaking names and aliases in both outputs, then restores the configured result without a stale frame. |
| E09 | Player fail-closed fallback | Force unsupported hook/order, world-buffer failure, VBU classification failure, and Iris active-shader fallback. A real name never reaches OBS; local output is the documented alias-or-hidden fallback. |
| E10 | Sign text | Front/back text, hanging signs, glow ink, color, wax, multiple signs in view. |
| E11 | Chests | Single/double/trapped/Ender variants, lid animation, holiday texture if applicable. |
| E12 | Item-frame maps | Normal/glow frames, rotation, markers, near/far views; a map held in hand must remain unaffected. |
| E13 | Banner patterns | Standing/wall banners, shield/banner GUI previews remain unaffected unless their screen is protected. |
| E14 | Beacon beams | Colored glass segments, long beam, camera inside/around beam, fog and shader shadows. |
| E15 | Mixed world scene | Player alias, non-player name, sign, chest, map, banner, and beacon visible together; no target ownership or depth interaction. |

### Gate F: Visual Integrity, State Restoration, And Stability

| ID | Scenario | Expected result |
|---|---|---|
| F01 | Move camera quickly while several components are protected. | No flicker, ghosting, one-frame lag, stale private pixels, or OBS leak. |
| F02 | Observe transparency, particles, water, clouds, hand/items, damage overlay, and portal effect. | No blend/depth/cull/scissor/stencil corruption after composition. |
| F03 | Open and close many GUI layers, tooltips, and picture-in-picture previews. | No duplicated, missing, clipped, or delayed GUI states. |
| F04 | Run for at least 30 minutes with scene changes and active overlays. | No progressive texture corruption, native crash, warning flood, or material performance regression. |
| F05 | Inspect `latest.log` after every compatibility row. | No Mixin failure, unmatched scope, depth-copy failure, Iris flush fallback, GL error, or compositor failure is ignored. |
| F06 | Disable every protected component while master remains on. | Ordinary rendering is unchanged apart from the optional test marker. |
| F07 | Re-enable components one at a time after a failure. | Failure remains explicit; no automatic transition back to trusted without a fresh restart/pre-flight. |
| F08 | Observe a dense lobby in pseudonym mode for at least 15 minutes. | No alias collision, label flicker, local alias ghost, real-name leak, excessive target switching, or material frame-time regression. |

## Acceptance Decision

The feature is ready for a stream setup only when:

1. A01 passes for the exact OBS source and current session.
2. The row matching the installed Sodium/Iris/ImmediatelyFast combination passes.
3. Every component the streamer enabled has a passing D or E row in that same
   relevant render combination.
4. The current window mode, scale, and lifecycle transitions have direct C-row
   evidence.
5. No related warning or error from F05 remains unexplained.
6. The marker and at least one real protected component are absent from the OBS
   **program output**, not only a stale preview.
7. When player mode is not `UNCHANGED`, its matching E rows pass for both normal
   and sneaking players, and E06 proves that non-player labels remain intact.

Anything not run remains `NOT RUN`, not implicitly supported. If a shader pack
or another mod fails, keep the master switch and fail-closed behavior enabled,
disable the incompatible setup or protected component, and do not advertise
that combination as compatible until it is retested.
