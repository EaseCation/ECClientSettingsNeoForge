package net.easecation.clientsettings.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.easecation.clientsettings.ECClientSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

public final class ClientSettingsEvents {

    private static final Component MODS_BUTTON = Component.translatable("fml.menu.mods");
    private static final Component SETTINGS_BUTTON = Component.translatable("button.ecclientsettings.open");

    private ClientSettingsEvents() {
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen == null && ClientSettingsKeyMappings.OPEN_SETTINGS.consumeClick()) {
            minecraft.setScreen(ClientSettingsScreen.create(null));
        }
    }

    public static void onScreenKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (!(event.getScreen() instanceof PauseScreen pauseScreen)
                || !pauseScreen.showsPauseMenu()
                || !ClientSettingsKeyMappings.OPEN_SETTINGS.isActiveAndMatches(
                        InputConstants.getKey(event.getKeyCode(), event.getScanCode())
                )) {
            return;
        }

        Minecraft.getInstance().setScreen(ClientSettingsScreen.create(pauseScreen));
        event.setCanceled(true);
    }

    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof PauseScreen pauseScreen) || !pauseScreen.showsPauseMenu()) {
            return;
        }

        boolean settingsButtonExists = event.getListenersList().stream()
                .filter(Button.class::isInstance)
                .map(Button.class::cast)
                .anyMatch(button -> SETTINGS_BUTTON.equals(button.getMessage()));
        if (settingsButtonExists) {
            return;
        }

        Button modsButton = event.getListenersList().stream()
                .filter(Button.class::isInstance)
                .map(Button.class::cast)
                .filter(button -> MODS_BUTTON.equals(button.getMessage()))
                .findFirst()
                .orElse(null);
        if (modsButton == null) {
            ECClientSettings.LOGGER.warn("Could not find the pause-menu Mods button; Home remains available");
            return;
        }

        modsButton.setWidth(98);
        event.addListener(Button.builder(
                        SETTINGS_BUTTON,
                        button -> Minecraft.getInstance().setScreen(ClientSettingsScreen.create(pauseScreen))
                )
                .bounds(modsButton.getX() + 102, modsButton.getY(), 98, 20)
                .build());
    }
}
