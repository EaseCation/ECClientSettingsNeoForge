package net.easecation.clientsettings.client;

import net.easecation.clientsettings.profile.model.ProfileDefinition;
import net.easecation.clientsettings.profile.runtime.ProfileManager;

import java.io.IOException;
import java.util.List;

public final class ProfileManagementController {

    private final ProfileManager profiles;

    public ProfileManagementController(ProfileManager profiles) {
        this.profiles = profiles;
    }

    public List<ProfileDefinition> profiles() {
        return profiles.profiles();
    }

    public String activeProfileId() {
        return profiles.activeSnapshot().id();
    }

    public ProfileDefinition create(String name) throws IOException {
        return profiles.create(name);
    }

    public ProfileDefinition duplicate(String profileId, String name) throws IOException {
        return profiles.duplicate(profileId, name);
    }

    public ProfileDefinition rename(String profileId, String name) throws IOException {
        return profiles.rename(profileId, name);
    }

    public void delete(String profileId) throws IOException {
        profiles.delete(profileId);
    }

    public void select(String profileId) throws IOException {
        profiles.switchTo(profileId);
    }
}
