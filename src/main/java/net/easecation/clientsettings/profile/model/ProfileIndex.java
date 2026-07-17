package net.easecation.clientsettings.profile.model;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record ProfileIndex(int schemaVersion, String activeProfileId, List<String> profileOrder) {

    public ProfileIndex {
        if (schemaVersion != ProfileDefinition.CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException("unsupported Profile index schema version: " + schemaVersion);
        }
        activeProfileId = ProfileDefinition.validateId(activeProfileId);
        profileOrder = List.copyOf(profileOrder);
        if (profileOrder.isEmpty()) {
            throw new IllegalArgumentException("Profile order must not be empty");
        }
        Set<String> unique = new HashSet<>();
        for (String profileId : profileOrder) {
            ProfileDefinition.validateId(profileId);
            if (!unique.add(profileId)) {
                throw new IllegalArgumentException("Profile order contains duplicate ID: " + profileId);
            }
        }
        if (!unique.contains(ProfileDefinition.DEFAULT_ID)) {
            throw new IllegalArgumentException("Profile order must contain default");
        }
        if (!unique.contains(activeProfileId)) {
            throw new IllegalArgumentException("Active Profile must be in Profile order");
        }
    }

    public static ProfileIndex defaults() {
        return new ProfileIndex(
                ProfileDefinition.CURRENT_SCHEMA_VERSION,
                ProfileDefinition.DEFAULT_ID,
                List.of(ProfileDefinition.DEFAULT_ID)
        );
    }

    public ProfileIndex withActiveProfile(String profileId) {
        return new ProfileIndex(schemaVersion, profileId, profileOrder);
    }
}
