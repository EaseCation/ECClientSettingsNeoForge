package net.easecation.clientsettings.profile.store;

import net.easecation.clientsettings.profile.model.ProfileDefinition;
import net.easecation.clientsettings.profile.model.ProfileIndex;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public record ProfileCatalog(ProfileIndex index, Map<String, ProfileDefinition> profiles) {

    public ProfileCatalog {
        Objects.requireNonNull(index, "index");
        profiles = Map.copyOf(new LinkedHashMap<>(profiles));
        if (!profiles.keySet().equals(Set.copyOf(index.profileOrder()))) {
            throw new IllegalArgumentException("Profile catalog and order contain different IDs");
        }
        Set<String> names = profiles.values().stream()
                .map(profile -> profile.name().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        if (names.size() != profiles.size()) {
            throw new IllegalArgumentException("Profile names must be unique ignoring case");
        }
    }

    public ProfileDefinition activeProfile() {
        return profile(index.activeProfileId());
    }

    public ProfileDefinition profile(String profileId) {
        ProfileDefinition profile = profiles.get(profileId);
        if (profile == null) {
            throw new IllegalArgumentException("Unknown Profile: " + profileId);
        }
        return profile;
    }

    public List<ProfileDefinition> profilesInOrder() {
        List<ProfileDefinition> ordered = new ArrayList<>(index.profileOrder().size());
        for (String profileId : index.profileOrder()) {
            ordered.add(profile(profileId));
        }
        return List.copyOf(ordered);
    }
}
