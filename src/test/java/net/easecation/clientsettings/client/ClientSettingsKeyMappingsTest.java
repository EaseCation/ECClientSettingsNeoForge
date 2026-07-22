package net.easecation.clientsettings.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientSettingsKeyMappingsTest {

    @Test
    void usesApprovedRebindableDefaults() {
        assertEquals(GLFW.GLFW_KEY_RIGHT_SHIFT, ClientSettingsKeyMappings.OPEN_SETTINGS.getDefaultKey().getValue());
        assertEquals(GLFW.GLFW_KEY_I, ClientSettingsKeyMappings.TOGGLE_FORCE_SPRINT.getDefaultKey().getValue());
        assertEquals(GLFW.GLFW_KEY_C, ClientSettingsKeyMappings.ZOOM.getDefaultKey().getValue());
        assertTrue(ClientSettingsKeyMappings.CYCLE_PROFILE.isUnbound());
        assertTrue(ClientSettingsKeyMappings.TOGGLE_FULLBRIGHT.isUnbound());
    }

    @Test
    void vanillaConflictDetectionDoesNotRewriteEitherBinding() {
        KeyMapping other = new KeyMapping(
                "key.ecclientsettings.test.conflict",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_C,
                "key.categories.ecclientsettings"
        );

        assertTrue(ClientSettingsKeyMappings.ZOOM.same(other));
        assertEquals(GLFW.GLFW_KEY_C, ClientSettingsKeyMappings.ZOOM.getKey().getValue());
        assertEquals(GLFW.GLFW_KEY_C, other.getKey().getValue());
    }
}
