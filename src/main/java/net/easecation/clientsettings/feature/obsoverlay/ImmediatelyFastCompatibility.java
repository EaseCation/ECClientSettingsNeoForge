package net.easecation.clientsettings.feature.obsoverlay;

import net.easecation.clientsettings.ECClientSettings;
import net.neoforged.fml.ModList;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

final class ImmediatelyFastCompatibility {

    private static final String SIGN_TEXT_API = "net.raphimc.immediatelyfast.injection.interfaces.ISignText";
    private static volatile boolean initialized;
    private static volatile boolean unavailable;
    private static Method shouldCache;
    private static Method setShouldCache;

    private ImmediatelyFastCompatibility() {
    }

    static boolean suspendSignTextCache(Object signText) {
        if (!ModList.get().isLoaded("immediatelyfast")) {
            return false;
        }
        initialize();
        if (unavailable || !shouldCache.getDeclaringClass().isInstance(signText)) {
            return false;
        }
        try {
            boolean enabled = (Boolean) shouldCache.invoke(signText);
            if (enabled) {
                setShouldCache.invoke(signText, false);
            }
            return enabled;
        } catch (IllegalAccessException | InvocationTargetException | RuntimeException exception) {
            unavailable = true;
            ECClientSettings.LOGGER.warn(
                    "Could not suspend ImmediatelyFast sign-text caching for OBS protection",
                    exception
            );
            return false;
        }
    }

    static void restoreSignTextCache(Object signText, boolean restore) {
        if (!restore || unavailable) {
            return;
        }
        try {
            setShouldCache.invoke(signText, true);
        } catch (IllegalAccessException | InvocationTargetException | RuntimeException exception) {
            unavailable = true;
            ECClientSettings.LOGGER.warn(
                    "Could not restore ImmediatelyFast sign-text caching after OBS protection",
                    exception
            );
        }
    }

    private static synchronized void initialize() {
        if (initialized) {
            return;
        }
        try {
            Class<?> type = Class.forName(SIGN_TEXT_API);
            shouldCache = type.getMethod("immediatelyFast$shouldCache");
            setShouldCache = type.getMethod("immediatelyFast$setShouldCache", boolean.class);
        } catch (ClassNotFoundException | NoSuchMethodException | RuntimeException exception) {
            unavailable = true;
            ECClientSettings.LOGGER.warn(
                    "ImmediatelyFast sign-text compatibility API is unavailable",
                    exception
            );
        } finally {
            initialized = true;
        }
    }
}
