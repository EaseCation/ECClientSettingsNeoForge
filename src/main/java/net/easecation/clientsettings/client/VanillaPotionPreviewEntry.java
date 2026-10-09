package net.easecation.clientsettings.client;

import java.util.List;
import java.util.function.Supplier;
import me.shedaniel.clothconfig2.gui.entries.TextListEntry;
import net.easecation.clientsettings.feature.hud.potions.VanillaPotionOverlay;
import net.easecation.clientsettings.profile.model.PotionHudSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

@SuppressWarnings("deprecation")
final class VanillaPotionPreviewEntry extends TextListEntry {
    private final Supplier<PotionHudSettings> settings;
    VanillaPotionPreviewEntry(Supplier<PotionHudSettings> settings) {
        super(Component.empty(), Component.empty());
        this.settings = settings;
    }
    @Override public int getItemHeight() { return 74; }
    @Override public void render(GuiGraphics graphics, int index, int y, int x, int width, int height,
            int mouseX, int mouseY, boolean hovered, float partialTick) {
        var mc = Minecraft.getInstance();
        graphics.drawString(mc.font, Component.translatable("option.ecclientsettings.potions.vanillaPreview"),
                x + 4, y + 2, getPreferredTextColor());
        int canvasLeft = x + 4;
        int canvasRight = x + width - 4;
        int canvasTop = y + 16;
        int canvasBottom = y + 70;
        for (int tileY = canvasTop; tileY < canvasBottom; tileY += 8) {
            for (int tileX = canvasLeft; tileX < canvasRight; tileX += 8) {
                boolean alternate = ((tileX - canvasLeft) / 8 + (tileY - canvasTop) / 8) % 2 == 0;
                graphics.fill(tileX, tileY, Math.min(tileX + 8, canvasRight), Math.min(tileY + 8, canvasBottom),
                        alternate ? 0xFF242A31 : 0xFF343C45);
            }
        }
        PotionHudSettings current;
        try { current = settings.get(); }
        catch (IllegalArgumentException exception) { return; }
        if (current.hideVanilla()) {
            graphics.drawCenteredString(mc.font, Component.translatable("option.ecclientsettings.potions.vanillaHidden"),
                    x + width / 2, y + 36, 0xFFAAAAAA);
            return;
        }
        var effects = List.of(new MobEffectInstance(MobEffects.STRENGTH, 75 * 20, 0),
                new MobEffectInstance(MobEffects.SPEED, 8 * 20, 1),
                new MobEffectInstance(MobEffects.ABSORPTION, -1, 0));
        for (int i = 0; i < effects.size(); i++) {
            int tileX = x + width / 2 - 38 + i * 25;
            int tileY = y + 31;
            var effect = effects.get(i);
            float alpha = VanillaPotionOverlay.opacity(effect, current);
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED,
                    ResourceLocation.withDefaultNamespace("hud/effect_background"), tileX, tileY, 24, 24);
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, Gui.getMobEffectSprite(effect.getEffect()),
                    tileX + 3, tileY + 3, 18, 18, alpha);
            VanillaPotionOverlay.draw(graphics, effect, tileX, tileY, alpha, current);
        }
    }
}
