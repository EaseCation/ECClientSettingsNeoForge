package net.easecation.clientsettings.client.input;

import net.easecation.clientsettings.profile.model.FullbrightMode;
import net.easecation.clientsettings.profile.model.FullbrightSettings;
import net.easecation.clientsettings.profile.model.ProfileDefinition;
import net.easecation.clientsettings.profile.runtime.ProfileManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ClientActionController {

    private final ProfileManager profiles;
    private final Map<String, FullbrightMode> lastNonOffFullbrightModes = new HashMap<>();

    public ClientActionController(ProfileManager profiles) {
        this.profiles = profiles;
    }

    public boolean toggleForceSprint(Runnable stopSprinting) throws IOException {
        boolean enabled = !profiles.activeSnapshot().features().forceSprint().enabled();
        profiles.updateActiveFeatures(features -> features.withForceSprint(enabled));
        if (!enabled) {
            stopSprinting.run();
        }
        return enabled;
    }

    public ProfileDefinition cycleProfile() throws IOException {
        List<ProfileDefinition> available = profiles.profiles();
        String activeId = profiles.activeSnapshot().id();
        int activeIndex = 0;
        for (int index = 0; index < available.size(); index++) {
            if (available.get(index).id().equals(activeId)) {
                activeIndex = index;
                break;
            }
        }
        ProfileDefinition target = available.get((activeIndex + 1) % available.size());
        profiles.switchTo(target.id());
        return target;
    }

    public FullbrightMode toggleFullbright() throws IOException {
        String profileId = profiles.activeSnapshot().id();
        FullbrightSettings current = profiles.activeSnapshot().features().fullbright();
        FullbrightMode targetMode;
        if (current.mode() == FullbrightMode.OFF) {
            targetMode = lastNonOffFullbrightModes.getOrDefault(profileId, FullbrightMode.GAMMA);
        } else {
            lastNonOffFullbrightModes.put(profileId, current.mode());
            targetMode = FullbrightMode.OFF;
        }
        FullbrightSettings updated = new FullbrightSettings(targetMode, current.strength());
        profiles.updateActiveFeatures(features -> features.withFullbright(updated));
        return targetMode;
    }
}
