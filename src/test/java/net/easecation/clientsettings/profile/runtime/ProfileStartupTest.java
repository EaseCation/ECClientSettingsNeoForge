package net.easecation.clientsettings.profile.runtime;

import net.easecation.clientsettings.client.input.ClientInputDispatcher;
import net.easecation.clientsettings.config.ClientSettingsConfig;
import net.easecation.clientsettings.feature.helditeminfo.HeldItemInfoRuntime;
import net.easecation.clientsettings.feature.hitcolor.HitColorRuntime;
import net.easecation.clientsettings.feature.hud.speed.SpeedHudRuntime;
import net.easecation.clientsettings.feature.timechanger.TimeChangerRuntime;
import net.easecation.clientsettings.feature.zoom.ZoomEvents;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProfileStartupTest {
    @Test
    void earlyClientCallbacksDoNotReadConfigOrCreateAnUnavailableProfile() {
        assertFalse(ClientSettingsConfig.SPEC.isLoaded());
        assertFalse(ProfileServices.isReady());

        for (int tick = 0; tick < 3; tick++) {
            assertDoesNotThrow(HeldItemInfoRuntime::tick);
            assertDoesNotThrow(() -> HeldItemInfoRuntime.render(null, 0));
            assertDoesNotThrow(() -> SpeedHudRuntime.onClientTick(null));
            assertDoesNotThrow(() -> ZoomEvents.onClientTick(null));
            assertDoesNotThrow(() -> ZoomEvents.onMouseScroll(null));
            assertDoesNotThrow(() -> TimeChangerRuntime.onClientTick(null));
            assertDoesNotThrow(() -> HitColorRuntime.onClientTick(null));
            assertDoesNotThrow(() -> ClientInputDispatcher.onClientTick(null));
        }

        assertFalse(ProfileServices.isReady());
        assertThrows(IllegalStateException.class, ProfileServices::manager);
        assertFalse(ProfileServices.isReady());
    }
}
