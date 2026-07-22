package net.easecation.clientsettings.profile.migration;

import net.easecation.clientsettings.config.ClientSettingsConfig;

import java.io.IOException;

public final class ClientConfigMigrationState implements ProfileMigrationState {

    @Override
    public int version() {
        return ClientSettingsConfig.profileMigrationVersion();
    }

    @Override
    public boolean legacyForceSprint() {
        return ClientSettingsConfig.legacyForceSprint();
    }

    @Override
    public void markComplete(int version) throws IOException {
        try {
            ClientSettingsConfig.setProfileMigrationVersion(version);
        } catch (RuntimeException exception) {
            throw new IOException("Could not save Profile migration marker", exception);
        }
    }
}
