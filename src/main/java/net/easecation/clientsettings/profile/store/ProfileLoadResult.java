package net.easecation.clientsettings.profile.store;

import java.util.List;

public record ProfileLoadResult(ProfileCatalog catalog, List<String> warnings) {

    public ProfileLoadResult {
        warnings = List.copyOf(warnings);
    }
}
