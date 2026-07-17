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
    public static final KeyMapping ZOOM = new KeyMapping(
            "key.ecclientsettings.zoom",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_C,
            "key.categories.ecclientsettings"
    );
    public static final KeyMapping CYCLE_PROFILE = new KeyMapping(
            "key.ecclientsettings.cycle_profile",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            "key.categories.ecclientsettings"
    );
    public static final KeyMapping TOGGLE_FULLBRIGHT = new KeyMapping(
            "key.ecclientsettings.toggle_fullbright",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            "key.categories.ecclientsettings"
    );

    private ClientSettingsKeyMappings() {
    }

    public static void register(RegisterKeyMappingsEvent event) {
        event.register(OPEN_SETTINGS);
        event.register(TOGGLE_FORCE_SPRINT);
        event.register(ZOOM);
        event.register(CYCLE_PROFILE);
        event.register(TOGGLE_FULLBRIGHT);
    }
}
