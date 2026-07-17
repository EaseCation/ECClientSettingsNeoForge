package net.easecation.clientsettings.profile.runtime;

import net.easecation.clientsettings.profile.model.ProfileDefinition;
import net.easecation.clientsettings.profile.model.ProfileFeatures;

import java.util.Objects;

public record ActiveProfileSnapshot(String id, String name, ProfileFeatures features) {

    public ActiveProfileSnapshot {
        ProfileDefinition.validateId(id);
        name = ProfileDefinition.normalizeName(name);
        features = Objects.requireNonNull(features, "features");
    }

    public static ActiveProfileSnapshot from(ProfileDefinition profile) {
        return new ActiveProfileSnapshot(profile.id(), profile.name(), profile.features());
    }
}
