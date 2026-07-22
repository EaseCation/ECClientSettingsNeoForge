# HUD Interaction And Style Baseline

## Product Scenarios

The HUD is used in two very different modes. During combat, the player must read a small amount of
information without moving attention far from the crosshair. During configuration, the player must be
able to manipulate the same objects directly and understand whether a change is saved.

The first interaction pass therefore optimizes these scenarios in order:

1. A new player enables a widget and gets a restrained, readable default without arranging it first.
2. A player notices a collision with a server scoreboard or another mod, presses Right Shift, opens the
   HUD category, and enters the clearly labeled visual editor.
3. A player fine-tunes a selected widget with snapping, Alt to bypass snapping, and keyboard nudging.
4. A player experiments with scale or color, can cancel the experiment, and can reset one widget or the
   whole layout without losing unrelated settings.
5. A player changes window size or GUI Scale and keeps every widget on screen.

These scenarios imply direct manipulation, visible selection state, immediate preview, explicit
Save/Cancel actions, and recovery controls. The editor follows an object-first model: select a widget,
then edit its layout or visual style. It does not expose a flat page containing every option for every
widget.

## Market Baseline

There is no single market-standard default position. Mature clients deliberately provide editors
because scoreboards, chat, subtitles, resource packs, and server-owned overlays compete for the same
space. There is, however, a strong size and styling baseline.

| Reference | Observed default or design signal | Adopted conclusion |
| --- | --- | --- |
| [uku's Armor HUD](https://github.com/uku3lig/armor-hud) | Defaults to a hotbar anchor, horizontal orientation, vanilla-hotbar style, 22 GUI-pixel slots with a 20-pixel step, 16-pixel item icons, and durability bars | Keep item icons at vanilla 16 px; use roughly 20-22 px per armor row/slot rather than oversized cards |
| [Simple Armor Hud](https://github.com/legoraft/simple-armor-hud) | Defaults above the food bar and uses about 15 px per armor item; alternate positions are above health or beside the hotbar | Armor belongs near combat status information, not in an arbitrary screen-center location |
| [Immersive Armor HUD](https://github.com/txnimc/ImmersiveArmorHUD) | Extends the vanilla status area and uses compact 6 px durability bars | Durability bars should be glanceable and compact; they do not need a large card |
| [Inventory HUD+](https://www.curseforge.com/minecraft/mc-mods/inventory-hud-forge) | Provides horizontal/vertical modes, background transparency, scale, effect gaps, and an in-game drag-and-drop screen | Position, scale, spacing, and background are user decisions; direct manipulation is an expected feature |
| [Status Effect Timer](https://github.com/magicus/statuseffecttimer) | Adds timer and amplifier text directly to vanilla 18 px effect icons | An effect row can stay near 20-22 px high; a 26+ px row is unnecessarily loose for the default |
| [FPS - Display](https://github.com/Grayray75/FPS-Display) | Defaults to scale 1, a 4 px top/left offset, vanilla font sizing, and optional text styling | FPS/Ping should use the vanilla 9 px font with about 2-4 px internal padding |
| [AxolotlClient](https://codeberg.org/AxolotlClient/AxolotlClient-mod) | Text widgets use a compact 53 x 13 baseline, 2 px text insets, approximately 39% black background, no outline by default, and per-widget color/background options | Reduce the current 78 x 18 text cards, lower visual weight, and keep outlines off by default |
| [Lunar Client HUD guidance](https://www.lunarclient.com/news/the-best-hud-setup-for-lunar-client) | Recommends minimal information, spacing between HUDs, and a consistent primary/secondary color hierarchy | Defaults must be quiet; high-salience RGB animation is opt-in and configured per widget |

The Axolotl/KronHUD files that expose these measurements identify GPL-3.0-derived HUD code. They are
used only as behavioral evidence. This project does not copy their implementation, class structure,
text, or assets.

## Default Visual Tokens

Minecraft GUI pixels, not physical monitor pixels, are the design unit.

- Vanilla font: 9 px high at scale 1.
- Item icon: 16 x 16 px.
- Potion icon: 18 x 18 px.
- Compact row: 20-22 px.
- Text inset: 2-4 px.
- Widget-to-screen inset: a fixed 4 GUI px safe area at every edge; normalized storage controls travel inside that safe area.
- Default background: neutral near-black at about 44% opacity.
- Default border: disabled. When enabled, 1 px with user-selected ARGB color.
- Default text: opaque white with vanilla shadow.
- Default RGB animation: disabled.

The first compact implementation uses 64 x 14 outer text cards, 22 px live potion rows, and a vertical
armor widget that collapses empty slots. The editor uses three representative potion rows and all four
armor slots so growth direction and maximum ordinary armor height are visible before saving. Vertical
armor matches the supplied PvP reference and keeps the 16 px icon, percentage, and durability bar
readable without the previous 120 x 36 horizontal card.

Default positions use the safe-area edges instead of approximate normalized margins: FPS is top-left
to avoid chat, Ping is top-right, Armor is bottom-right near combat status information, and Potion is
on the left quarter. These are starting points only; every widget remains directly movable.

## Style Architecture

Every `HudWidgetSettings` owns a `HudWidgetStyle`. Layout and style are intentionally separate so future
widgets can reuse the renderer without adding widget-specific persistence fields.

The shared style includes:

- background enabled and ARGB color;
- border enabled, ARGB color, and 1-3 px width;
- 0-12 px content padding;
- text shadow;
- fixed ARGB text color;
- text color mode;
- RGB animation speed;
- rainbow spatial spread.

Text color modes are:

- `FIXED`: one selected ARGB color;
- `RAINBOW_WAVE`: an animated hue gradient across Unicode code points;
- `RAINBOW_SWITCH`: the complete string changes between hues at a low, bounded frequency.

Animation speed is restricted to 0.1-2.0 cycles or switches per second. The upper bound stays below the
3 Hz flashing threshold commonly used for photosensitive-content safety. Critical semantic states such
as an expiring potion may still use warning red instead of decorative RGB.

The renderer owns panel background, border, padding, and text animation. Widgets own only semantic
content and measurement. This prevents each future widget from implementing colors and padding in a
different way.

The style page includes a checkerboard-backed live preview. It reads the current Cloth controls before
they are saved, so background/text alpha, border width, padding, shadow, RGB mode, speed, and spread are
visible while the player adjusts them. Invalid numeric input suspends the preview instead of crashing
the screen.

## Editor Information Architecture

Right Shift and the pause-menu button open the same complete client settings screen. The visual HUD
editor is an explicit action in the HUD category; the global shortcut does not unexpectedly switch to
a specialized editing mode.

The editor exposes a compact command palette while the game remains visible:

- All Settings;
- Edit Selected Style;
- Snapping on/off;
- Reset Selected;
- Reset All;
- Cancel;
- Save and Exit, or Apply when entered from the parent settings screen.

The command palette is visually and interactively above previews: its buttons and gaps consume input,
so a hidden widget can never steal Save or Cancel clicks. The first enabled widget is selected on entry
(FPS when all are disabled), so keyboard adjustment works immediately. The selected widget reports GUI
X/Y and effective scale; when the viewport limits a requested scale, both actual and configured values
are shown. Arrow keys nudge by 1 px, Shift+Arrow by 10 px, `+`/`-` adjust scale, and holding Alt
temporarily bypasses snapping.

Standalone Save writes the active Profile. Cancel or Escape discards the standalone draft. When the
editor is opened from Cloth Config, Apply returns the layout/style draft to the parent and the parent
Done action remains the only persistence point. HUD toggles changed on the Cloth page are synchronized
into the editor draft before it opens. Entering from a paused single-player screen preserves pause;
opening directly during play remains non-pausing so the player can judge the live scene.

## Manual Acceptance

- At GUI viewports 320 x 240, 427 x 240, 640 x 360, 16:9, 4:3, and ultrawide, all previews remain
  selectable and on screen.
- Compare GUI Scale 1-4 and window resize behavior at every corner.
- Check Armor with four slots, one slot, damaged/repaired items, and no armor.
- Check Potion with one, four, and long localized/modded effect names.
- Check FPS/Ping on bright sky, dark cave, chat open, and scoreboard visible.
- Verify fixed text color, animated gradient, low-frequency whole-text switching, alpha, shadow, padding,
  border width/color, and background color for every widget.
- Confirm Escape cancels, Save persists after restart, Reset Selected leaves other widgets untouched,
  and profile switching immediately changes layout and style.
