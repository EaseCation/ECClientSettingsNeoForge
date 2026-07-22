package net.easecation.clientsettings.profile.migration;

import net.easecation.clientsettings.profile.model.ProfileDefinition;
import net.easecation.clientsettings.profile.runtime.ProfileManager;
import net.easecation.clientsettings.profile.store.ProfileStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfileBootstrapTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void migratesLegacyPreferenceThenAdvancesMarker() throws IOException {
        FakeMigrationState migration = new FakeMigrationState(0, false);

        ProfileManager manager = ProfileBootstrap.load(new ProfileStore(temporaryDirectory), migration);

        assertFalse(manager.activeSnapshot().features().forceSprint().enabled());
        assertEquals(ProfileBootstrap.MIGRATION_VERSION, migration.version);
        assertEquals(1, migration.markAttempts);
    }

    @Test
    void failedMarkerRetriesWithoutOverwritingPersistedProfile() throws IOException {
        FakeMigrationState firstAttempt = new FakeMigrationState(0, false);
        firstAttempt.failMark = true;

        assertThrows(
                IOException.class,
                () -> ProfileBootstrap.load(new ProfileStore(temporaryDirectory), firstAttempt)
        );
        assertEquals(0, firstAttempt.version);

        FakeMigrationState retry = new FakeMigrationState(0, true);
        ProfileManager manager = ProfileBootstrap.load(new ProfileStore(temporaryDirectory), retry);

        assertFalse(manager.activeSnapshot().features().forceSprint().enabled());
        assertEquals(1, retry.version);
        assertEquals(1, retry.markAttempts);
    }

    @Test
    void completedMigrationNeverReadsLegacyValueAgain() throws IOException {
        FakeMigrationState migration = new FakeMigrationState(1, false);
        migration.failLegacyRead = true;

        ProfileManager manager = ProfileBootstrap.load(new ProfileStore(temporaryDirectory), migration);

        assertTrue(manager.activeSnapshot().features().forceSprint().enabled());
        assertEquals(0, migration.markAttempts);
    }

    private static final class FakeMigrationState implements ProfileMigrationState {
        private int version;
        private final boolean legacyForceSprint;
        private int markAttempts;
        private boolean failMark;
        private boolean failLegacyRead;

        private FakeMigrationState(int version, boolean legacyForceSprint) {
            this.version = version;
            this.legacyForceSprint = legacyForceSprint;
        }

        @Override
        public int version() {
            return version;
        }

        @Override
        public boolean legacyForceSprint() {
            if (failLegacyRead) {
                throw new AssertionError("legacy value should not be read");
            }
            return legacyForceSprint;
        }

        @Override
        public void markComplete(int version) throws IOException {
            markAttempts++;
            if (failMark) {
                throw new IOException("simulated marker failure");
            }
            this.version = version;
        }
    }
}
