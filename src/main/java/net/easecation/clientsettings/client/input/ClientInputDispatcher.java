package net.easecation.clientsettings.client.input;

import net.easecation.clientsettings.ECClientSettings;
import net.easecation.clientsettings.client.ClientSettingsKeyMappings;
import net.easecation.clientsettings.client.ClientSettingsScreen;
import net.easecation.clientsettings.profile.model.FullbrightMode;
import net.easecation.clientsettings.profile.model.ProfileDefinition;
import net.easecation.clientsettings.profile.runtime.ProfileManager;
import net.easecation.clientsettings.profile.runtime.ProfileServices;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.io.IOException;
import java.util.Locale;

public final class ClientInputDispatcher {

    private static ClientActionController controller;
    private static ProfileManager controllerProfiles;

    private ClientInputDispatcher() {
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null) {
            return;
        }

        if (ClientSettingsKeyMappings.OPEN_SETTINGS.consumeClick()) {
            minecraft.setScreen(ClientSettingsScreen.create(null));
            return;
        }

        boolean toggleForceSprint = ClientSettingsKeyMappings.TOGGLE_FORCE_SPRINT.consumeClick();
        boolean cycleProfile = ClientSettingsKeyMappings.CYCLE_PROFILE.consumeClick();
        boolean toggleFullbright = ClientSettingsKeyMappings.TOGGLE_FULLBRIGHT.consumeClick();
        if (!toggleForceSprint && !cycleProfile && !toggleFullbright) {
            return;
        }

        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            return;
        }

        ClientActionController actions = controller();
        try {
            if (toggleForceSprint) {
                boolean enabled = actions.toggleForceSprint(() -> player.setSprinting(false));
                player.displayClientMessage(Component.translatable(enabled
                        ? "message.ecclientsettings.force_sprint.enabled"
                        : "message.ecclientsettings.force_sprint.disabled"), false);
            }
            if (cycleProfile) {
                ProfileDefinition selected = actions.cycleProfile();
                player.displayClientMessage(Component.translatable(
                        "message.ecclientsettings.profile.selected",
                        selected.name()
                ), false);
            }
            if (toggleFullbright) {
                FullbrightMode mode = actions.toggleFullbright();
                player.displayClientMessage(Component.translatable(
                        "message.ecclientsettings.fullbright.mode",
                        Component.translatable(
                                "option.ecclientsettings.fullbright.mode." + mode.name().toLowerCase(Locale.ROOT)
                        )
                ), false);
            }
        } catch (IOException exception) {
            ECClientSettings.LOGGER.error("Could not apply client input action", exception);
            player.displayClientMessage(Component.translatable("message.ecclientsettings.action_failed"), false);
        }
    }

    private static ClientActionController controller() {
        ProfileManager profiles = ProfileServices.manager();
        if (controller == null || controllerProfiles != profiles) {
            controllerProfiles = profiles;
            controller = new ClientActionController(profiles);
        }
        return controller;
    }
}
