package net.easecation.clientsettings.profile.migration;

import java.io.IOException;

public interface ProfileMigrationState {

    int version();

    boolean legacyForceSprint();

    void markComplete(int version) throws IOException;
}
