package net.easecation.clientsettings.feature.performance;

import net.easecation.clientsettings.ECClientSettings;
import net.easecation.clientsettings.config.ClientSettingsConfig;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import org.oryxel.viabedrockutility.config.HardwareProfile;
import org.oryxel.viabedrockutility.config.LodConfig;

public final class InitialRenderDistanceController {
    private InitialRenderDistanceController() {
    }

    public static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        if (event.getConnection().isMemoryConnection() || !ClientSettingsConfig.renderDistanceAutoPending()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        int currentDistance = minecraft.options.renderDistance().get();
        HardwareProfile hardware = LodConfig.getInstance().getHardwareProfile();
        int recommendedDistance = hardware.recommendedInitialRenderDistance();
        ClientSettingsConfig.resolveInitialRenderDistance();
        if (currentDistance != recommendedDistance) {
            minecraft.options.renderDistance().set(recommendedDistance);
            minecraft.options.save();
        }
        ECClientSettings.LOGGER.info(
                "Initial render distance applied: previous={}, recommended={}, gpu={}",
                currentDistance,
                recommendedDistance,
                hardware.gpuName()
        );
    }
}
