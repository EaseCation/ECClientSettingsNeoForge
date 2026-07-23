package net.easecation.clientsettings.feature.obsoverlay;

import net.neoforged.fml.ModList;

final class ViaBedrockCompatibility {

    private static final String DEFERRED_NAME_TAG_CLASS =
            "org.oryxel.viabedrockutility.renderer.DeferredNameTag";
    private static volatile boolean deferredNameTagHookApplied;
    private static volatile boolean deferredNameTagLoadAttempted;

    private ViaBedrockCompatibility() {
    }

    static void markDeferredNameTagHookApplied() {
        deferredNameTagHookApplied = true;
    }

    static boolean deferredNameTagHookAvailable() {
        if (!ModList.get().isLoaded("viabedrockutility")) {
            return true;
        }
        ensureDeferredNameTagLoaded();
        return deferredNameTagHookApplied;
    }

    private static synchronized void ensureDeferredNameTagLoaded() {
        if (deferredNameTagHookApplied || deferredNameTagLoadAttempted) {
            return;
        }
        deferredNameTagLoadAttempted = true;
        try {
            Class.forName(DEFERRED_NAME_TAG_CLASS, false, ViaBedrockCompatibility.class.getClassLoader());
        } catch (ClassNotFoundException firstFailure) {
            ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
            if (contextLoader != null && contextLoader != ViaBedrockCompatibility.class.getClassLoader()) {
                try {
                    Class.forName(DEFERRED_NAME_TAG_CLASS, false, contextLoader);
                    return;
                } catch (ClassNotFoundException | LinkageError ignored) {
                    // The caller records the user-facing fail-closed runtime failure.
                }
            }
        } catch (LinkageError ignored) {
            // The caller records the user-facing fail-closed runtime failure.
        }
    }
}
