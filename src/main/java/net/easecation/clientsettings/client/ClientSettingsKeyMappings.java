package net.easecation.clientsettings.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

public final class ClientSettingsKeyMappings {

    public static final KeyMapping OPEN_SETTINGS = new KeyMapping(
            "key.ecclientsettings.open_settings",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_HOME,
            "key.categories.ecclientsettings"
    );
    public static final KeyMapping TOGGLE_FORCE_SPRINT = new KeyMapping(
            "key.ecclientsettings.toggle_force_sprint",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_I,
            "key.categories.ecclientsettings"
    );

    private ClientSettingsKeyMappings() {
    }

    public static void register(RegisterKeyMappingsEvent event) {
        event.register(OPEN_SETTINGS);
        event.register(TOGGLE_FORCE_SPRINT);
    }
}
