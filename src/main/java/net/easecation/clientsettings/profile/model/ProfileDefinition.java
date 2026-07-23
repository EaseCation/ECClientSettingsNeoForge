package net.easecation.clientsettings.profile.model;

import java.util.Locale;
import java.util.UUID;

public record ProfileDefinition(int schemaVersion, String id, String name, ProfileFeatures features) {

    public static final int CURRENT_SCHEMA_VERSION = 4;
    public static final String DEFAULT_ID = "default";
    public static final int MAX_NAME_CODE_POINTS = 64;

    public ProfileDefinition {
        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException("unsupported Profile schema version: " + schemaVersion);
        }
        id = validateId(id);
        name = normalizeName(name);
        features = ProfileValidation.requireNonNull(features, "features");
    }

    public static ProfileDefinition defaults(boolean forceSprint) {
        return new ProfileDefinition(
                CURRENT_SCHEMA_VERSION,
                DEFAULT_ID,
                "Default",
                ProfileFeatures.DEFAULT.withForceSprint(forceSprint)
        );
    }

    public static ProfileDefinition create(String name, ProfileFeatures features) {
        return new ProfileDefinition(
                CURRENT_SCHEMA_VERSION,
                UUID.randomUUID().toString(),
                name,
                features
        );
    }

    public ProfileDefinition withName(String newName) {
        return new ProfileDefinition(schemaVersion, id, newName, features);
    }

    public ProfileDefinition withFeatures(ProfileFeatures newFeatures) {
        return new ProfileDefinition(schemaVersion, id, name, newFeatures);
    }

    public boolean isDefault() {
        return DEFAULT_ID.equals(id);
    }

    public static String normalizeName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Profile name must not be null");
        }
        String normalized = name.trim();
        int codePoints = normalized.codePointCount(0, normalized.length());
        if (normalized.isEmpty() || codePoints > MAX_NAME_CODE_POINTS) {
            throw new IllegalArgumentException("Profile name must contain 1..64 Unicode code points");
        }
        return normalized;
    }

    public static String validateId(String id) {
        if (DEFAULT_ID.equals(id)) {
            return id;
        }
        if (id == null || !id.equals(id.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Profile ID must be a lowercase canonical UUID");
        }
        try {
            UUID parsed = UUID.fromString(id);
            if (!parsed.toString().equals(id)) {
                throw new IllegalArgumentException("Profile ID must be a lowercase canonical UUID");
            }
            return id;
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Profile ID must be default or a lowercase canonical UUID", exception);
        }
    }
}
