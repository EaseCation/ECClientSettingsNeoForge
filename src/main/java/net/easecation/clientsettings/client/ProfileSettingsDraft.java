package net.easecation.clientsettings.client;

import net.easecation.clientsettings.profile.model.ProfileFeatures;
import net.easecation.clientsettings.profile.runtime.ProfileManager;

import java.io.IOException;
import java.util.Objects;

public final class ProfileSettingsDraft {

    private final String profileId;
    private final ProfileFeatures initialFeatures;
    private ProfileFeatures pendingFeatures;

    public ProfileSettingsDraft(String profileId, ProfileFeatures features) {
        this.profileId = profileId;
        this.initialFeatures = Objects.requireNonNull(features, "features");
        this.pendingFeatures = features;
    }

    public static ProfileSettingsDraft active(ProfileManager profiles) {
        return new ProfileSettingsDraft(
                profiles.activeSnapshot().id(),
                profiles.activeSnapshot().features()
        );
    }

    public String profileId() {
        return profileId;
    }

    public ProfileFeatures features() {
        return pendingFeatures;
    }

    public void setForceSprint(boolean enabled) {
        pendingFeatures = pendingFeatures.withForceSprint(enabled);
    }

    public boolean edited() {
        return !pendingFeatures.equals(initialFeatures);
    }

    public void save(ProfileManager profiles) throws IOException {
        if (!profiles.activeSnapshot().id().equals(profileId)) {
            throw new IOException("Active Profile changed while its settings screen was open");
        }
        if (edited()) {
            profiles.updateActiveFeatures(ignored -> pendingFeatures);
        }
    }
}
