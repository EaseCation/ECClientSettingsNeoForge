package net.easecation.clientsettings.profile.store;

import java.io.IOException;

public final class UnsupportedProfileSchemaException extends IOException {

    private final int schemaVersion;

    public UnsupportedProfileSchemaException(int schemaVersion) {
        super("Profile data uses newer schema version " + schemaVersion);
        this.schemaVersion = schemaVersion;
    }

    public int schemaVersion() {
        return schemaVersion;
    }
}
