package net.easecation.clientsettings.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.ScreenEvent;

public final class ClientSettingsEvents {

    private static final Component SETTINGS_BUTTON = Component.translatable("button.ecclientsettings.open");
    static final int PAUSE_BUTTON_WIDTH = 100;
    static final int PAUSE_BUTTON_HEIGHT = 20;
    static final int PAUSE_BUTTON_MARGIN = 6;

    private ClientSettingsEvents() {
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

        event.addListener(Button.builder(
                        SETTINGS_BUTTON,
                        button -> Minecraft.getInstance().setScreen(ClientSettingsScreen.create(pauseScreen))
                )
                .bounds(
                        pauseButtonX(pauseScreen.width),
                        PAUSE_BUTTON_MARGIN,
                        PAUSE_BUTTON_WIDTH,
                        PAUSE_BUTTON_HEIGHT
                )
                .build());
    }

    static int pauseButtonX(int screenWidth) {
        return Math.max(PAUSE_BUTTON_MARGIN, screenWidth - PAUSE_BUTTON_MARGIN - PAUSE_BUTTON_WIDTH);
    }
}
