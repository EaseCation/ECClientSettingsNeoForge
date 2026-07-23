package net.easecation.clientsettings.feature.obsoverlay;

import net.easecation.clientsettings.ECClientSettings;
import net.neoforged.fml.ModList;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

final class IrisCompatibility {

    private static final String IRIS_API = "net.irisshaders.iris.api.v0.IrisApi";
    private static volatile boolean initialized;
    private static volatile boolean unavailable;
    private static Object api;
    private static Method isRenderingShadowPass;
    private static Method isShaderPackInUse;

    private IrisCompatibility() {
    }

    static WorldRedirectDecision worldRedirectDecision() {
        if (!ModList.get().isLoaded("iris")) {
            return WorldRedirectDecision.ALLOW;
        }
        initialize();
        if (unavailable) {
            return WorldRedirectDecision.UNAVAILABLE;
        }
        try {
            if ((Boolean) isRenderingShadowPass.invoke(api)) {
                return WorldRedirectDecision.SHADOW_PASS;
            }
            return (Boolean) isShaderPackInUse.invoke(api)
                    ? WorldRedirectDecision.SHADER_PACK_ACTIVE
                    : WorldRedirectDecision.ALLOW;
        } catch (IllegalAccessException | InvocationTargetException | RuntimeException exception) {
            markUnavailable("Iris world-render state is unavailable", exception);
            return WorldRedirectDecision.UNAVAILABLE;
        }
    }

    static boolean shaderPackInUse() {
        if (!ModList.get().isLoaded("iris")) {
            return false;
        }
        initialize();
        if (unavailable) {
            return false;
        }
        try {
            return (Boolean) isShaderPackInUse.invoke(api);
        } catch (IllegalAccessException | InvocationTargetException | RuntimeException exception) {
            markUnavailable("Iris shader-pack state is unavailable", exception);
            return false;
        }
    }

    static boolean unavailable() {
        return ModList.get().isLoaded("iris") && initialized && unavailable;
    }

    private static synchronized void initialize() {
        if (initialized) {
            return;
        }
        try {
            Class<?> apiClass = Class.forName(IRIS_API);
            api = apiClass.getMethod("getInstance").invoke(null);
            isRenderingShadowPass = apiClass.getMethod("isRenderingShadowPass");
            isShaderPackInUse = apiClass.getMethod("isShaderPackInUse");
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException
                 | InvocationTargetException | RuntimeException exception) {
            unavailable = true;
            ECClientSettings.LOGGER.warn(
                    "Disabling experimental OBS world overlays because this Iris version is not compatible",
                    exception
            );
        } finally {
            initialized = true;
        }
    }

    private static void markUnavailable(String detail, Exception exception) {
        if (!unavailable) {
            unavailable = true;
            ECClientSettings.LOGGER.warn(
                    "Disabling experimental OBS world overlays because {}",
                    detail,
                    exception
            );
        }
    }

    enum WorldRedirectDecision {
        ALLOW,
        SHADOW_PASS,
        SHADER_PACK_ACTIVE,
        UNAVAILABLE
    }
}
