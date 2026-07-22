package net.easecation.clientsettings.client;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ClientSettingsEventsTest {

    @Test
    void addsStandaloneButtonWhenModsButtonIsMissing() {
        PauseScreen pauseScreen = new PauseScreen(true);
        pauseScreen.width = 320;
        List<GuiEventListener> listeners = new ArrayList<>();

        ClientSettingsEvents.onScreenInit(event(pauseScreen, listeners));

        Button settingsButton = (Button) listeners.getFirst();
        assertEquals(Component.translatable("button.ecclientsettings.open"), settingsButton.getMessage());
        assertEquals(214, settingsButton.getX());
        assertEquals(ClientSettingsEvents.PAUSE_BUTTON_MARGIN, settingsButton.getY());
        assertEquals(ClientSettingsEvents.PAUSE_BUTTON_WIDTH, settingsButton.getWidth());
        assertEquals(ClientSettingsEvents.PAUSE_BUTTON_HEIGHT, settingsButton.getHeight());
    }

    @Test
    void leavesModifiedPauseMenuButtonsUntouched() {
        PauseScreen pauseScreen = new PauseScreen(true);
        pauseScreen.width = 320;
        Button customButton = Button.builder(Component.literal("NetEase Menu"), button -> {
        }).bounds(20, 40, 180, 20).build();
        List<GuiEventListener> listeners = new ArrayList<>(List.of(customButton));

        ClientSettingsEvents.onScreenInit(event(pauseScreen, listeners));

        assertEquals(2, listeners.size());
        assertSame(customButton, listeners.getFirst());
        assertEquals(180, customButton.getWidth());
    }

    @Test
    void doesNotAddDuplicateButton() {
        PauseScreen pauseScreen = new PauseScreen(true);
        pauseScreen.width = 320;
        Button existing = Button.builder(Component.translatable("button.ecclientsettings.open"), button -> {
        }).bounds(10, 10, 100, 20).build();
        List<GuiEventListener> listeners = new ArrayList<>(List.of(existing));

        ClientSettingsEvents.onScreenInit(event(pauseScreen, listeners));

        assertEquals(List.of(existing), listeners);
    }

    @Test
    void ignoresNonMenuPauseOverlay() {
        PauseScreen pauseScreen = new PauseScreen(false);
        pauseScreen.width = 320;
        List<GuiEventListener> listeners = new ArrayList<>();

        ClientSettingsEvents.onScreenInit(event(pauseScreen, listeners));

        assertEquals(List.of(), listeners);
    }

    private static ScreenEvent.Init.Post event(PauseScreen screen, List<GuiEventListener> listeners) {
        return new ScreenEvent.Init.Post(screen, listeners, listeners::add, listeners::remove);
    }
}
