package net.easecation.clientsettings.profile.runtime;

import net.easecation.clientsettings.ECClientSettings;
import net.easecation.clientsettings.config.ClientSettingsConfig;
import net.easecation.clientsettings.profile.migration.ClientConfigMigrationState;
import net.easecation.clientsettings.profile.migration.ProfileBootstrap;
import net.easecation.clientsettings.profile.model.ProfileDefinition;
import net.easecation.clientsettings.profile.store.ProfileStore;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.fml.event.config.ModConfigEvent;

import java.nio.file.Path;

public final class ProfileServices {

    private static volatile ProfileManager manager;

    private ProfileServices() {
    }

    public static ProfileManager manager() {
        ProfileManager current = manager;
        if (current == null) {
            throw new IllegalStateException("Profile storage is not ready before client configuration loads");
        }
        return current;
    }

    public static boolean isReady() {
        return manager != null;
    }

    public static void onConfigLoading(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() != ClientSettingsConfig.SPEC) {
            return;
        }
        synchronized (ProfileServices.class) {
            if (manager == null) {
                manager = loadManager();
            }
        }
    }

    public static ActiveProfileSnapshot active() {
        return manager().activeSnapshot();
    }

    private static ProfileManager loadManager() {
        Path root = FMLPaths.CONFIGDIR.get().resolve("ecclientsettings");
        try {
            ProfileManager loaded = ProfileBootstrap.load(new ProfileStore(root), new ClientConfigMigrationState());
            for (String warning : loaded.warnings()) {
                ECClientSettings.LOGGER.warn("Profile recovery: {}", warning);
            }
            return loaded;
        } catch (Exception exception) {
            String error = "Could not initialize Profile storage at " + root + ": " + exception.getMessage();
            ECClientSettings.LOGGER.error(error, exception);
            return ProfileManager.unavailable(ProfileDefinition.defaults(true), error);
        }
    }
}
