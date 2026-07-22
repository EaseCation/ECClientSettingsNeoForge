package net.easecation.clientsettings.client.input;

import net.easecation.clientsettings.profile.model.FullbrightMode;
import net.easecation.clientsettings.profile.model.FullbrightSettings;
import net.easecation.clientsettings.profile.model.ProfileDefinition;
import net.easecation.clientsettings.profile.runtime.ProfileManager;
import net.easecation.clientsettings.profile.store.ProfileStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientActionControllerTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void forceSprintTogglePersistsAndStopsOnlyWhenDisabled() throws IOException {
        ProfileManager profiles = manager();
        ClientActionController actions = new ClientActionController(profiles);
        AtomicInteger stopCalls = new AtomicInteger();

        assertFalse(actions.toggleForceSprint(stopCalls::incrementAndGet));
        assertEquals(1, stopCalls.get());
        assertTrue(actions.toggleForceSprint(stopCalls::incrementAndGet));
        assertEquals(1, stopCalls.get());

        ProfileManager reloaded = ProfileManager.load(
                new ProfileStore(temporaryDirectory),
                ProfileDefinition.defaults(false)
        );
        assertTrue(reloaded.activeSnapshot().features().forceSprint().enabled());
    }

    @Test
    void failedForceSprintSaveDoesNotStopPlayer() {
        ProfileManager unavailable = ProfileManager.unavailable(ProfileDefinition.defaults(true), "read only");
        ClientActionController actions = new ClientActionController(unavailable);
        AtomicInteger stopCalls = new AtomicInteger();

        assertThrows(IOException.class, () -> actions.toggleForceSprint(stopCalls::incrementAndGet));
        assertEquals(0, stopCalls.get());
    }

    @Test
    void profileCyclingFollowsStoredOrderAndWraps() throws IOException {
        ProfileManager profiles = manager();
        ProfileDefinition first = profiles.create("First");
        ProfileDefinition second = profiles.create("Second");
        ClientActionController actions = new ClientActionController(profiles);

        assertEquals(first.id(), actions.cycleProfile().id());
        assertEquals(second.id(), actions.cycleProfile().id());
        assertEquals("default", actions.cycleProfile().id());
    }

    @Test
    void fullbrightToggleRemembersLastNonOffModePerProfile() throws IOException {
        ProfileManager profiles = manager();
        ClientActionController actions = new ClientActionController(profiles);

        assertEquals(FullbrightMode.GAMMA, actions.toggleFullbright());
        profiles.updateActiveFeatures(features -> features.withFullbright(
                new FullbrightSettings(FullbrightMode.NIGHT_VISION, 0.5)
        ));
        assertEquals(FullbrightMode.OFF, actions.toggleFullbright());
        assertEquals(FullbrightMode.NIGHT_VISION, actions.toggleFullbright());
        assertEquals(0.5, profiles.activeSnapshot().features().fullbright().strength());
    }

    private ProfileManager manager() throws IOException {
        return ProfileManager.load(new ProfileStore(temporaryDirectory), ProfileDefinition.defaults(true));
    }
}
