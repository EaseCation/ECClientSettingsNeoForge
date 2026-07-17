package net.easecation.clientsettings.profile.runtime;

import net.easecation.clientsettings.profile.model.ProfileDefinition;
import net.easecation.clientsettings.profile.store.ProfileStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfileManagerTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void supportsCrudStableOrderingAndActiveDeletionFallback() throws IOException {
        ProfileManager manager = manager();
        ProfileDefinition first = manager.create("Practice");
        ProfileDefinition second = manager.duplicate(first.id(), "Tournament");
        manager.rename(second.id(), "Ranked");
        manager.switchTo(second.id());

        assertEquals(List.of("Default", "Practice", "Ranked"), names(manager));
        assertEquals(second.id(), manager.activeSnapshot().id());
        assertThrows(IllegalArgumentException.class, () -> manager.create(" ranked "));

        manager.delete(second.id());

        assertEquals("default", manager.activeSnapshot().id());
        assertEquals(List.of("Default", "Practice"), names(manager));
        assertThrows(IllegalArgumentException.class, () -> manager.delete("default"));
    }

    @Test
    void updatesActiveProfileAndPersistsAcrossReload() throws IOException {
        ProfileManager manager = manager();
        manager.updateActiveFeatures(features -> features.withForceSprint(false));

        ProfileManager reloaded = ProfileManager.load(
                new ProfileStore(temporaryDirectory),
                ProfileDefinition.defaults(true)
        );

        assertFalse(reloaded.activeSnapshot().features().forceSprint().enabled());
    }

    @Test
    void appliesParticipantsInOrderAndResetsBeforeTransitions() throws IOException {
        ProfileManager manager = manager();
        ProfileDefinition target = manager.create("Target");
        List<String> calls = new ArrayList<>();
        manager.registerParticipant(participant("first", calls, false));
        manager.registerParticipant(participant("second", calls, false));
        calls.clear();

        manager.switchTo(target.id());

        assertEquals(List.of("first:reset", "second:reset", "first:apply", "second:apply"), calls);
        assertEquals(target.id(), manager.activeSnapshot().id());
    }

    @Test
    void participantFailureRollsBackSnapshotAndTransientState() throws IOException {
        ProfileManager manager = manager();
        ProfileDefinition target = manager.create("Target");
        List<String> calls = new ArrayList<>();
        manager.registerParticipant(participant("stable", calls, false));
        manager.registerParticipant(participant("failing", calls, true));
        calls.clear();

        assertThrows(IOException.class, () -> manager.switchTo(target.id()));

        assertEquals("default", manager.activeSnapshot().id());
        assertTrue(calls.stream().filter(call -> call.endsWith(":reset")).count() >= 4);
        ProfileManager reloaded = ProfileManager.load(
                new ProfileStore(temporaryDirectory),
                ProfileDefinition.defaults(true)
        );
        assertEquals("default", reloaded.activeSnapshot().id());
    }

    private ProfileManager manager() throws IOException {
        return ProfileManager.load(new ProfileStore(temporaryDirectory), ProfileDefinition.defaults(true));
    }

    private static List<String> names(ProfileManager manager) {
        return manager.profiles().stream().map(ProfileDefinition::name).toList();
    }

    private static ProfileParticipant participant(String name, List<String> calls, boolean failOnTarget) {
        return new ProfileParticipant() {
            @Override
            public void apply(ActiveProfileSnapshot previous, ActiveProfileSnapshot current) throws Exception {
                calls.add(name + ":apply");
                if (failOnTarget && !current.id().equals("default")) {
                    throw new Exception("simulated apply failure");
                }
            }

            @Override
            public void resetTransientState() {
                calls.add(name + ":reset");
            }
        };
    }
}
