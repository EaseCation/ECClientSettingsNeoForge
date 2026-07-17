package net.easecation.clientsettings.client;

import net.easecation.clientsettings.profile.model.ProfileFeatures;
import net.easecation.clientsettings.profile.model.ArgbColor;
import net.easecation.clientsettings.profile.model.BlockOutlineSettings;
import net.easecation.clientsettings.profile.model.LowFireSettings;
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

    public void setBlockOutlineEnabled(boolean enabled) {
        BlockOutlineSettings current = pendingFeatures.blockOutline();
        pendingFeatures = pendingFeatures.withBlockOutline(new BlockOutlineSettings(enabled, current.color()));
    }

    public void setBlockOutlineColor(int color) {
        BlockOutlineSettings current = pendingFeatures.blockOutline();
        pendingFeatures = pendingFeatures.withBlockOutline(new BlockOutlineSettings(
                current.enabled(),
                new ArgbColor(color)
        ));
    }

    public void setLowFireEnabled(boolean enabled) {
        LowFireSettings current = pendingFeatures.lowFire();
        pendingFeatures = pendingFeatures.withLowFire(new LowFireSettings(enabled, current.verticalOffset()));
    }

    public void setLowFireOffset(double offset) {
        LowFireSettings current = pendingFeatures.lowFire();
        pendingFeatures = pendingFeatures.withLowFire(new LowFireSettings(current.enabled(), offset));
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
