package net.easecation.clientsettings.feature.hud;

import net.easecation.clientsettings.profile.model.HudWidgetStyle;
import net.easecation.clientsettings.profile.model.HudSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;

import java.util.Objects;

public record HudRenderContext(
        Minecraft minecraft,
        GuiGraphics graphics,
        HudRenderMode mode,
        float partialTick,
        HudSettings hudSettings,
        HudWidgetStyle style,
        long animationMillis
) {

    public HudRenderContext(Minecraft minecraft, GuiGraphics graphics, HudRenderMode mode, float partialTick) {
        this(minecraft, graphics, mode, partialTick, HudSettings.DEFAULT);
    }

    public HudRenderContext(
            Minecraft minecraft,
            GuiGraphics graphics,
            HudRenderMode mode,
            float partialTick,
            HudSettings hudSettings
    ) {
        this(
                minecraft,
                graphics,
                mode,
                partialTick,
                hudSettings,
                HudWidgetStyle.DEFAULT,
                System.currentTimeMillis()
        );
    }

    public HudRenderContext {
        minecraft = Objects.requireNonNull(minecraft, "minecraft");
        graphics = Objects.requireNonNull(graphics, "graphics");
        mode = Objects.requireNonNull(mode, "mode");
        hudSettings = Objects.requireNonNull(hudSettings, "HUD settings");
        style = Objects.requireNonNull(style, "style");
    }

    public Font font() {
        return minecraft.font;
    }

    public LocalPlayer player() {
        return minecraft.player;
    }

    public boolean preview() {
        return mode == HudRenderMode.PREVIEW;
    }

    public HudRenderContext withStyle(HudWidgetStyle widgetStyle) {
        return new HudRenderContext(
                minecraft,
                graphics,
                mode,
                partialTick,
                hudSettings,
                widgetStyle,
                animationMillis
        );
    }
}
