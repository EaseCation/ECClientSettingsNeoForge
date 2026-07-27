package net.easecation.clientsettings.feature.performance;

import net.easecation.clientsettings.config.ClientSettingsConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;

import java.util.Objects;

public final class RenderDistancePreferenceTracker {
    private RenderDistancePreferenceTracker() {
    }

    public static void onOptionSet(OptionInstance<?> option, Object value) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options != null
                && option == minecraft.options.renderDistance()
                && !Objects.equals(option.get(), value)
                && ClientSettingsConfig.renderDistanceAutoPending()) {
            ClientSettingsConfig.resolveInitialRenderDistance();
        }
    }
}
