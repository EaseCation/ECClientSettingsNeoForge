package net.easecation.clientsettings.profile.migration;

import net.easecation.clientsettings.profile.model.ProfileDefinition;
import net.easecation.clientsettings.profile.runtime.ProfileManager;
import net.easecation.clientsettings.profile.store.ProfileStore;

import java.io.IOException;

public final class ProfileBootstrap {

    public static final int MIGRATION_VERSION = 1;

    private ProfileBootstrap() {
    }

    public static ProfileManager load(ProfileStore store, ProfileMigrationState migrationState) throws IOException {
        boolean indexExisted = store.indexExists();
        boolean migrationPending = migrationState.version() < MIGRATION_VERSION;
        boolean initialForceSprint = migrationPending && !indexExisted
                ? migrationState.legacyForceSprint()
                : ProfileDefinition.defaults(true).features().forceSprint().enabled();

        ProfileManager manager = ProfileManager.load(store, ProfileDefinition.defaults(initialForceSprint));
        if (migrationPending) {
            migrationState.markComplete(MIGRATION_VERSION);
        }
        return manager;
    }
}
