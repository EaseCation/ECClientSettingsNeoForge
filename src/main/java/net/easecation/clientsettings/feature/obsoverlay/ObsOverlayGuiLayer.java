package net.easecation.clientsettings.feature.obsoverlay;

import net.easecation.clientsettings.ECClientSettings;
import net.easecation.clientsettings.config.ObsOverlayConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

public final class ObsOverlayGuiLayer {

    private static final ResourceLocation LAYER_ID =
            ResourceLocation.fromNamespaceAndPath(ECClientSettings.MOD_ID, "obs_safety_marker");

    private ObsOverlayGuiLayer() {
    }

    public static void register(RegisterGuiLayersEvent event) {
        event.registerAboveAll(LAYER_ID, ObsOverlayGuiLayer::render);
    }

    private static void render(GuiGraphics graphics, DeltaTracker ignored) {
        ObsOverlaySettings settings = ObsOverlayConfig.current();
        if (!settings.enabled() || !settings.showTestMarker()) {
            return;
        }

        boolean ready = ObsOverlayRuntime.protectionReady();
        if (ready) {
            ObsOverlayRuntime.beginTestMarker();
        }
        try {
            Component label = Component.literal(ready ? "OBS TEST" : "OBS UNSAFE");
            int width = Minecraft.getInstance().font.width(label) + 10;
            int x = Math.max(4, graphics.guiWidth() - width - 6);
            int y = 6;
            int border = ready ? 0xFF38F29B : 0xFFFF4D4D;
            graphics.fill(x, y, x + width, y + 17, 0xD0101418);
            graphics.fill(x, y, x + width, y + 1, border);
            graphics.fill(x, y + 16, x + width, y + 17, border);
            graphics.fill(x, y, x + 1, y + 17, border);
            graphics.fill(x + width - 1, y, x + width, y + 17, border);
            graphics.drawString(Minecraft.getInstance().font, label, x + 5, y + 4, 0xFFFFFFFF, true);
        } finally {
            if (ready) {
                ObsOverlayRuntime.endTestMarker();
            }
        }
    }
}
