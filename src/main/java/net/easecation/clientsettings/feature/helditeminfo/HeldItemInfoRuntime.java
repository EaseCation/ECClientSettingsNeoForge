package net.easecation.clientsettings.feature.helditeminfo;

import java.util.List;
import net.easecation.clientsettings.feature.obsoverlay.ObsOverlayComponent;
import net.easecation.clientsettings.feature.obsoverlay.ObsOverlayRuntime;
import net.easecation.clientsettings.profile.model.HeldItemBackground;
import net.easecation.clientsettings.profile.model.HeldItemInfoSettings;
import net.easecation.clientsettings.profile.runtime.ProfileServices;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

public final class HeldItemInfoRuntime {
    private static final HeldItemInfoTimer TIMER = new HeldItemInfoTimer();
    private static ItemStack previous = ItemStack.EMPTY;
    private static int previousSlot = -1;
    private static Object previousPlayer;
    private static Object previousWorld;
    private static HeldItemInfoSettings previousSettings;
    private static List<HeldItemInfoContent.Line> content = List.of();
    private static HeldItemInfoLayout layout;
    private static int layoutWidth;
    private static int layoutHeight;
    private static Font layoutFont;
    private HeldItemInfoRuntime() {}

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        HeldItemInfoSettings settings = ProfileServices.active().features().heldItemInfo();
        if (!settings.enabled() || mc.player == null || mc.level == null) { clear(); return; }
        ItemStack selected = mc.player.getMainHandItem();
        int slot = mc.player.getInventory().getSelectedSlot();
        if (selected.isEmpty()) { clear(); return; }
        boolean changed = previousPlayer != mc.player || previousWorld != mc.level || previousSlot != slot
                || !settings.equals(previousSettings) || !ItemStack.isSameItemSameComponents(previous, selected);
        if (changed) {
            previous = selected.copy(); previousSlot = slot; previousPlayer = mc.player; previousWorld = mc.level;
            previousSettings = settings;
            content = HeldItemInfoContent.collect(selected, mc.player, Item.TooltipContext.of(mc.level), settings);
            layout = null;
            Font font = selectedFont(mc, selected);
            rebuild(settings, font, mc.getWindow().getGuiScaledWidth() - 32, mc.getWindow().getGuiScaledHeight() - 80);
            TIMER.restart(settings.baseSeconds(), settings.extraLineSeconds(), layout.rows().size());
        } else { TIMER.tick(); }
    }

    private static Font selectedFont(Minecraft mc, ItemStack stack) {
        Font font = IClientItemExtensions.of(stack).getFont(stack, IClientItemExtensions.FontContext.SELECTED_ITEM_NAME);
        return font == null ? mc.font : font;
    }

    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
    }

    private static void clear() {
        previous = ItemStack.EMPTY; previousSlot = -1; previousPlayer = null; previousWorld = null;
        previousSettings = null; content = List.of(); layout = null; TIMER.clear();
    }

    private static void rebuild(HeldItemInfoSettings settings, Font font, int width, int height) {
        width = Math.max(1, width); height = Math.max(0, height);
        if (layout == null || width != layoutWidth || height != layoutHeight || font != layoutFont) {
            layout = HeldItemInfoLayout.build(content, settings, font, width, height);
            layoutWidth = width; layoutHeight = height; layoutFont = font;
        }
    }

    /** Called in the existing selected-item GUI layer; its F1/spectator rules remain authoritative. */
    public static void render(GuiGraphics graphics, int yShift) {
        Minecraft mc = Minecraft.getInstance();
        HeldItemInfoSettings settings = ProfileServices.active().features().heldItemInfo();
        if (mc.player == null || mc.level == null || mc.options.hideGui || previous.isEmpty()
                || !settings.equals(previousSettings)) return;
        float alpha = TIMER.opacity(mc.getDeltaTracker().getGameTimeDeltaPartialTick(false));
        if (alpha <= 0) return;
        Font font = selectedFont(mc, previous);
        int anchor = graphics.guiHeight() - Math.max(yShift, 59) - settings.verticalOffset();
        if (mc.gameMode != null && !mc.gameMode.canHurtPlayer()) anchor += 14;
        anchor = Math.clamp(anchor, 16, Math.max(16, graphics.guiHeight() - 24));
        rebuild(settings, font, graphics.guiWidth() - 32, anchor - 16);
        if (layout.rows().isEmpty()) return;
        int x = (graphics.guiWidth() - layout.width()) / 2;
        int y = Math.max(16, anchor + font.lineHeight - layout.height());
        ObsOverlayRuntime.beginComponent(ObsOverlayComponent.EC_HUD);
        try {
            drawBackground(graphics, settings, x, y, layout.width(), layout.height(), alpha);
            int rowY = y;
            boolean nameEnded = false;
            boolean hasName = layout.rows().getFirst().kind() == HeldItemInfoContent.Kind.NAME;
            for (var row : layout.rows()) {
                if (hasName && !nameEnded && row.kind() != HeldItemInfoContent.Kind.NAME) {
                    rowY += settings.nameGap(); nameEnded = true;
                }
                graphics.drawString(font, row.text(), (graphics.guiWidth() - row.width()) / 2, rowY,
                        opacity(0xFFFFFFFF, alpha), true);
                rowY += settings.lineSpacing();
            }
        } finally { ObsOverlayRuntime.endComponent(); }
    }

    private static void drawBackground(GuiGraphics graphics, HeldItemInfoSettings settings,
            int x, int y, int width, int height, float alpha) {
        if (settings.background() == HeldItemBackground.NONE) return;
        if (settings.background() == HeldItemBackground.VANILLA) {
            ResourceLocation style = previous.get(DataComponents.TOOLTIP_STYLE);
            ResourceLocation background = style == null ? ResourceLocation.withDefaultNamespace("tooltip/background")
                    : style.withPath(path -> "tooltip/" + path + "_background");
            ResourceLocation frame = style == null ? ResourceLocation.withDefaultNamespace("tooltip/frame")
                    : style.withPath(path -> "tooltip/" + path + "_frame");
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, background, x - 12, y - 12, width + 24, height + 24, alpha);
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, frame, x - 12, y - 12, width + 24, height + 24, alpha);
        } else {
            int color = settings.backgroundColor().value();
            if (settings.chroma()) {
                // Animation also uses game time, avoiding jumps while the game is paused.
                float hue = (float) (((mcTicks() / 20.0) * settings.chromaSpeed()) % 1.0);
                color = ((int) Math.round(settings.chromaOpacity() * 255) << 24)
                        | Mth.hsvToRgb(hue, (float) settings.chromaSaturation(), (float) settings.chromaBrightness());
            }
            graphics.fill(x - 4, y - 4, x + width + 4, y + height + 4, opacity(color, alpha));
        }
    }

    private static double mcTicks() {
        Minecraft mc = Minecraft.getInstance();
        return mc.gui.getGuiTicks() + mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
    }

    private static int opacity(int color, float multiplier) {
        return ((int) Math.round((color >>> 24) * multiplier) << 24) | (color & 0xFFFFFF);
    }
}
